/*
   GASH 2

   DBNameSpace.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;


/*------------------------------------------------------------------------------
                                                                           class
                                                                     DBNameSpace
DBNameSpaces are the objects used to coordinate unique valued fields across
various EditSets that would possibly want to modify fields constrained
to have a unique value.

Several different fields in different DBObjectBase's can point to the
same DBNameSpace.  All such fields thus share a common name space.

DBNameSpace is designed to coordinate transactional access in conjunction with
DBEditSet's and DBEditObject's.

When an object is pulled out from editing, it can't affect any other object,
except through the acquisition of values in unique contraint fields.  Such
an acquisition must be atomic and immediate, unlike normal DBEditObject
processing where nothing in the database is actually changed until the
DBEditSet is committed.

The actual acquisition logic is in the DBEditObject's setValue method.

------------------------------------------------------------------------------*/

class DBNameSpace {

  String    name;		// the name of this namespace
  Hashtable uniqueHash;		// index of values used in the current namespace
  Hashtable reserved;		// index of editSet's currently actively modifying
				// values in this namespace
  /* -- */

  // constructors

  public DBNameSpace(String name)
  {
    this.name = name;
    uniqueHash = new Hashtable(); // size?
    reserved = new Hashtable();	// size?
  }

  public String name()
  {
    return name;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                        lookup()
  
  This method allows the namespace to be used as a unique valued search index.

  Note that this lookup only works for precise equality lookup.. i.e., strings
  must be the same capitalization and the whole works.

  As well, this method is really probably useful in the context of a DBReadLock,
  but we're not doing anything to enforce this requirement at this point.

  ----------------------------------------------------------------------------*/
  public synchronized DBField lookup(Object value)
  {
    DBNameSpaceHandle handle;

    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.field != null)
	  {
	    return handle.field;
	  }
      }

    return null;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                      testmark()

  This method tests to see whether a value in the namespace can be marked as
  in use.  Such a marking is done to allow an editset to have the ability
  to juggle values in namespace associated fields without allowing another
  editset to acquire a value needed if the editset transaction is aborted.

  For array db fields, all elements in the array should be testmark'ed
  in the context of a synchronized block on the namespace before going back
  and marking each value (while still in the synchronized block on the
  namespace).. this ensures that we won't mark several values in an array
  before discovering that one of the values in a DBArrayField is already
  taken

  ----------------------------------------------------------------------------*/
  public synchronized boolean testmark(DBEditSet editSet, Object value)
  {
    DBNameSpaceHandle handle;

    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner == null)
	  {
	    return true;
	  }
	else
	  {
	    if (handle.owner != editSet)
	      {
		return false;	// somebody else owns it
	      }

	    return !handle.inuse;
	  }
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                          mark()

  This method marks a value as being used in this namespace.  Marked
  values are held in editSet's, which are free to shuffle these
  reserved values around during processing.  The inuse and original
  fields in the namespace object are used during unique value searches
  to do direct hash lookups without getting confused by any shuffling
  being performed by a current thread.

  ----------------------------------------------------------------------------*/

  synchronized public boolean mark(DBEditSet editSet, Object value, DBField field)
  {
    DBNameSpaceHandle handle;
    
    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner == null)
	  {
	    // no one has claimed this, give it to the editSet

	    // note that since this handle has no owner,
	    // we know that the original state should be true,
	    // else it wouldn't have been in the hash to begin
	    // with

	    handle.owner = editSet;
	    handle.original = true;
	    handle.inuse = true;
	    handle.shadowField = field;

	    remember(editSet, value);
	  }
	else
	  {
	    if (handle.owner != editSet)
	      {
		return false;	// somebody else owns it
	      }
	    else
	      {
		// we own it

		// if this namespace value is still being used in
		// the namespace, give it up.  they need to 
		// unmark the value in one place before they
		// can mark it in another

		if (handle.inuse)
		  {
		    return false;
		  }

		handle.inuse = true;
		handle.shadowField = field;

		// we don't have to make a note in reserved since
		// we already have this value noted in the editset?
	      }
	  }
      }
    else
      {
	// we're creating a new value.. previous value
	// is false

	handle = new DBNameSpaceHandle(editSet, false, null);
	handle.inuse = true;
	handle.shadowField = field;
	uniqueHash.put(value, handle);

	remember(editSet, value);
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                    testunmark()

  This method tests to see whether a value in the namespace can be marked as
  not in use.  Such a marking is done to allow an editset to have the ability
  to juggle values in namespace associated fields without allowing another
  editset to acquire a value needed if the editset transaction is aborted.

  For array db fields, all elements in the array should be testunmark'ed
  in the context of a synchronized block on the namespace before actually
  unmarking all the values.  See the comments in testmark() for the logic
  here.  Note that testunmark() is less useful than testmark() because
  we really aren't expecting anything to prevent us from unmarking()
  something.

  ----------------------------------------------------------------------------*/
  public synchronized boolean testunmark(DBEditSet editSet, Object value)
  {
    DBNameSpaceHandle handle;

    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner == null)
	  {
	    return true;
	  }
	else
	  {
	    if (handle.owner != editSet)
	      {
		return false;	// somebody else owns it
	      }
	    else
	      {
		return handle.inuse;
	      }
	  }
      }

    return false;		// if the value isn't already in the name
				// space, it doesn't make sense for us to
				// be unmarking it
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                        unmark()

  Used to mark a value as not used in the namespace.  Unmarked values are not
  available for other threads / editset's until commit is called on this
  namespace on behalf of this editset.

  ----------------------------------------------------------------------------*/

  public synchronized boolean unmark(DBEditSet editSet, Object value)
  {
    DBNameSpaceHandle handle;
    
    /* -- */

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (handle.owner == null)
	  {
	    // no one has claimed this, give it to the editSet

	    // note that since this handle has no owner,
	    // we know that the original state should be true,
	    // else it wouldn't have been in the hash to begin
	    // with

	    handle.owner = editSet;
	    handle.original = true;
	    handle.inuse = false;
	    handle.shadowField = null;

	    remember(editSet, value);
	  }
	else
	  {
	    if (handle.owner != editSet)
	      {
		return false;	// somebody else owns it
	      }
	    else
	      {
		// we own it, but we don't want to change the original
		// value, which we will need if we abort this editset

		handle.inuse = false;
		handle.shadowField = null;
	      }
	  }
      }
    else
      {
	// we're creating a new value.. previous value
	// is false

	handle = new DBNameSpaceHandle(editSet, false, null);
	handle.inuse = true;
	handle.shadowField = null;
	uniqueHash.put(value, handle);

	remember(editSet, value);
      }

    return true;
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                         abort()
  Method to revert an editSet's namespace modifications to its
  original state.  Used when a transaction is rolled back.

  ----------------------------------------------------------------------------*/

  public synchronized void abort(DBEditSet editSet)
  {
    Vector valueVect;
    DBNameSpaceHandle handle;

    /* -- */

    // if we don't have any namespace values reserved for this
    // editSet, just return

    if (!reserved.containsKey(editSet))
      {
	return;
      }

    // get the vector of namespace values used by this editset

    valueVect = (Vector) reserved.get(editSet);

    // loop over the values in the namespace that were changed or affected
    // by this editset

    for (int i = 0; i < valueVect.size(); i++)
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(valueVect.elementAt(i));

	// if the handle was originally in the namespace,
	// clear the shadowfield. if the handle wasn't in the
	// namespace, take it out.

	if (handle.original)
	  {
	    handle.owner = null;
	    handle.shadowField = null;
	    handle.inuse = true;
	  }
	else
	  {
	    uniqueHash.remove(valueVect.elementAt(i));
	  }
      }

    // we're done with this editSet

    reserved.remove(editSet);
  }

  /*----------------------------------------------------------------------------
                                                                          method
                                                                        commit()

  Method to put the editSet's current namespace modifications into
  final effect and to make any abandoned values available for other
  namespaces.

  ----------------------------------------------------------------------------*/

  public synchronized void commit(DBEditSet editSet)
  {
    Vector valueVect;
    DBNameSpaceHandle handle;

    /* -- */

    // if we don't have any namespace values reserved for this
    // editSet, just return

    if (!reserved.containsKey(editSet))
      {
	return;
      }

    // get the vector of namespace values used by this editset

    valueVect = (Vector) reserved.get(editSet);

    // loop over the values in the namespace that were changes or affected
    // by this editset

    for (int i = 0; i < valueVect.size(); i++)
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(valueVect.elementAt(i));

	if (handle.inuse)
	  {
	    // if the transaction wishes to retain this value, update
	    // our handle to go along with the new order

	    handle.owner = null;
	    handle.field = handle.shadowField;
	    handle.shadowField = null;
	  }
	else
	  {
	    // this value will be floating free and easy, unused and
	    // unloved.  forget about it.

	    uniqueHash.remove(valueVect.elementAt(i));
	  }
      }

    // we're done with this editSet

    reserved.remove(editSet);
  }  

  /*----------------------------------------------------------------------------
                                                                          method
                                                                      remember()

  Remember that this editSet has changed the location/status of this value.

  This is a private convenience method.

  ----------------------------------------------------------------------------*/
  private void remember(DBEditSet editSet, Object value)
  {
    Vector tmpvect;

    /* -- */

    if (!reserved.containsKey(editSet))
      {
	tmpvect = new Vector();
	tmpvect.addElement(value);
	reserved.put(editSet, tmpvect);
      }
    else
      {
	tmpvect = (Vector) reserved.get(editSet);
	tmpvect.addElement(value);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBNameSpaceHandle

This class is intended to be the targets of elements of a name space's unique
value hash.  The fields in this class are used to keep track of who currently
'owns' a given value, and whether or not there is actually any field in the
namespace that really contains that value.

This class will be manipulated by the DBNameSpace class and by the
DBEditObject class.

------------------------------------------------------------------------------*/

class DBNameSpaceHandle {

  DBEditSet owner;		// if this value is currently being shuffled
				// by a transaction, this is the transaction

  boolean original;		// remember if the value was in use at the
				// start of the transaction

  boolean inuse;		// is the value currently in use?

  DBField field;		// so the namespace hash can be used as an index
				// field always points to the field that contained
				// this value at the time this field was last
				// committed in a transaction

  DBField shadowField;		// if this handle is currently being edited
				// by an editset, shadowField points to the
				// new field

  /* -- */

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;
  }

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue, DBField field)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;
    this.field = field;
  }

  public boolean matches(DBEditSet set)
  {
    return (this.owner == set);
  }
}
