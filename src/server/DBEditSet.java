/*
   GASH 2

   DBEditSet.java

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
                                                                       DBEditSet

------------------------------------------------------------------------------*/

/*
 * DBEditSet is the basic transactional unit.  All changes to the database during
 * normal operations are made in the context of a DBEditSet, which may then be
 * committed or rolledback as an atomic operation.
 * 
 * When DBEditSet commits, the datastore is changed, and information is written
 * to the DBStore journal.
 *
 */

class DBEditSet {

  Vector 
    objectsChanged = null, 
    objectsCreated = null, 
    objectsDeleted = null;

  Hashtable basesModified;

  DBStore dbStore;
  DBSession session;

  /* -- */

  /*
   * Constructor for DBEditSet
   *
   */

  DBEditSet(DBStore dbStore, DBSession session)
  {
    this.session = session;
    this.dbStore = dbStore;
    objectsChanged = new Vector();
    objectsCreated = new Vector();
    objectsDeleted = new Vector();
    basesModified = new Hashtable(dbStore.objectBases.size());
  }

  synchronized void addCreatedObject(DBEditObject object)
  {
    if (!objectsCreated.contains(object))
      {
	objectsCreated.addElement(object);

	// just need something to mark the slot in the hash table,
	basesModified.put(object.objectBase, this);	
      }
  }

  synchronized void addChangedObject(DBEditObject object)
  {
    if (!objectsChanged.contains(object))
      {
	objectsChanged.addElement(object);

	// just need something to mark the slot in the hash table,
	basesModified.put(object.objectBase, this);	
      }
  }

  synchronized void addDeletedObject(DBObject object)
  {
    if (!objectsDeleted.contains(object))
      {
	objectsDeleted.addElement(object);

	// just need something to mark the slot in the  hash table,
	basesModified.put(object.objectBase, this);	
      }
  }

  /* note that in the release methods, we don't clear anything out of
     the basesModified hash.  It's not worth keeping track of whether
     or not a given object was the only object in the editset that was
     part of the DBObjectBase in question. */

  /* we need to make sure these release methods properly manage
     nameset issues. */

  synchronized void releaseCreatedObject(DBEditObject object)
  {
    if (objectsCreated.contains(object))
      {
	objectsCreated.removeElement(object);
      }
  }

  synchronized void releaseChangedObject(DBEditObject object)
  {
    if (objectsChanged.contains(object))
      {
	objectsChanged.removeElement(object);
	if (!object.original.clearShadow(this))
	  {
	    throw new RuntimeException("editset ownership synchronization error");
	  }
      }

    object.original.shadowObject = null;
  }

  synchronized void releaseDeletedObject(DBObject object)
  {
    if (objectsDeleted.contains(object))
      {
	objectsDeleted.removeElement(object);
	if (!object.clearDeletionMark(this))
	  {
	    throw new RuntimeException("editset ownership synchronization error");
	  }
      }
  }

  /*
   * commit is used to cause all changes in association with
   * this DBEditSet to be performed.  If commit() cannot make
   * the changes for any reason, commit() will return false and
   * the database will be unchanged.
   * */

  synchronized boolean commit()
  {
    DBWriteLock wLock;
    Vector baseSet;
    Enumeration enum;
    DBObjectBase base;
    Object key;
    DBEditObject eObj;
    DBObject obj;

    /* -- */

    baseSet = new Vector();
    enum = basesModified.keys();

    while (enum.hasMoreElements())
      {
	baseSet.addElement(enum.nextElement());
      }

    wLock = new DBWriteLock(dbStore, baseSet);
    wLock.establish(session);	// wait for write lock

    // write this transaction out to the Journal

    try
      {
	if (!dbStore.journal.writeTransaction(this))
	  {
	    release();
	    return false;
	  }
      }
    catch (IOException ex)
      {
	// we probably want to be more extreme here.. if we couldn't write out
	// the transaction, we are probably out of space on the filesystem
	// with the journal/dbstore file.

	release();
	return false;
      }

    // and make the changes in our in-memory hashes.

    for (int i = 0; i < objectsCreated.size(); i++)
      {
	eObj = (DBEditObject) objectsCreated.elementAt(i);

	base = eObj.objectBase;

	base.objectHash.put(new Integer(eObj.id), new DBObject(eObj));
      }

    for (int i = 0; i < objectsChanged.size(); i++)
      {
	eObj = (DBEditObject) objectsChanged.elementAt(i);

	base = eObj.objectBase;

	base.objectHash.put(new Integer(eObj.id), new DBObject(eObj));
      }

    for (int i = 0; i < objectsDeleted.size(); i++)
      {
	obj = (DBObject) objectsDeleted.elementAt(i);

	base = obj.objectBase;

	base.objectHash.remove(new Integer(obj.id));
      }

    // confirm all namespace modifications associated with this editset
    // and release namespace values that correspond with old
    // object / field values.

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).commit(this);
      }

    wLock.release();

    objectsCreated = null;
    objectsChanged = null;
    objectsDeleted = null;

    return true;
  }

  /*
   * release is used to abandon all changes made to DBEditObjects
   * in association with this DBEditSet.
   *
   */

  synchronized void release()
  {
    while (objectsCreated.size() > 0)
      {
	releaseCreatedObject((DBEditObject) objectsCreated.firstElement());
      }

    while (objectsChanged.size() > 0)
      {
	releaseChangedObject((DBEditObject) objectsChanged.firstElement());
      }

    while (objectsDeleted.size() > 0)
      {
	releaseDeletedObject((DBEditObject) objectsDeleted.firstElement());
      }

    basesModified.clear();

    // undo all namespace modifications associated with this editset

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).abort(this);
      }
  }
}
