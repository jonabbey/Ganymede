/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.6 $ %D%
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

/**
 *
 * <p>DBEditSet is the basic transactional unit.  All changes to the
 * database during normal operations are made in the context of a
 * DBEditSet, which may then be committed or rolledback as an atomic
 * operation.</p>
 *
 * 
 * <p>When DBEditSet commits, the datastore is changed, and
 * information is written to the DBStore journal.</p>
 *
 */

public class DBEditSet {

  Vector 
    objectsChanged = null, 
    objectsCreated = null, 
    objectsDeleted = null;

  Hashtable basesModified;

  DBStore dbStore;
  DBSession session;

  /* -- */

  /**
   * Constructor for DBEditSet
   *
   * @param dbStore The owning DBStore object.
   * @param session The DBStore session owning this transaction.
   *
   */

  public DBEditSet(DBStore dbStore, DBSession session)
  {
    this.session = session;
    this.dbStore = dbStore;
    objectsChanged = new Vector();
    objectsCreated = new Vector();
    objectsDeleted = new Vector();
    basesModified = new Hashtable(dbStore.objectBases.size());
  }

  /**
   *
   * Method to associate a newly created object with this
   * transaction.  If a created object is not associated with
   * a transaction, it cannot be integrated into the DBStore
   * object hash.
   *
   * @param object The newly created DBEditObject.
   *
   */

  public synchronized void addCreatedObject(DBEditObject object)
  {
    if (!objectsCreated.contains(object))
      {
	objectsCreated.addElement(object);

	// just need something to mark the slot in the hash table,
	basesModified.put(object.objectBase, this);	
      }
  }

  /**
   *
   * Method to associate a shadow object pulled from the DBStore with
   * this transaction.  If a shadow object is not associated with a
   * transaction, it cannot be re-integrated into the DBStore object
   * hash.
   *
   * @param object A shadow created from a DBObject.
   *
   * @see csd.DBStore.DBObject
   * 
   */

  public synchronized void addChangedObject(DBEditObject object)
  {
    if (!objectsChanged.contains(object))
      {
	objectsChanged.addElement(object);

	// just need something to mark the slot in the hash table,
	basesModified.put(object.objectBase, this);	
      }
  }

  /**
   *
   * <p>Method to associate an object marked for deletion (via
   * DBObject.  mark()) with this transaction.  It is an error to mark
   * an object for deletion and not then record that fact in a
   * transaction with addDeletedObject().</p>
   *
   * <p>The object recorded as deleted in care of this transaction
   * will only be deleted if this transaction is committed.</p>
   *
   * @param object The DBObject marked for death by this transaction.
   *
   * @see csd.DBStore.DBObject#markAsDeleted()
   * @see csd.DBStore.DBObject
   *  
   */

  public synchronized void addDeletedObject(DBObject object)
  {
    if (!objectsDeleted.contains(object))
      {
	objectsDeleted.addElement(object);

	// just need something to mark the slot in the  hash table,
	basesModified.put(object.objectBase, this);	
      }
  }

  /**
   *
   * commit is used to cause all changes in association with
   * this DBEditSet to be performed.  If commit() cannot make
   * the changes for any reason, commit() will return false and
   * the database will be unchanged.
   *
   * @return Whether the transaction was committed (true) or released (false).
   * 
   */

  public synchronized boolean commit()
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

	// Create a new DBObject from our DBEditObject and insert
	// into the object hash

	base.objectHash.put(new Integer(eObj.id), new DBObject(eObj));
      }

    for (int i = 0; i < objectsChanged.size(); i++)
      {
	eObj = (DBEditObject) objectsChanged.elementAt(i);

	base = eObj.objectBase;

	// Create a new DBObject from our DBEditObject and insert
	// into the object hash.  This unlinks our old DBObject
	// from the hash, making it available for garbage collection
	// and freeing us from having to worry about it's shadow
	// object field.

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

  /**
   *
   * <p>release is used to abandon all changes made in association
   * with this DBEditSet.  All DBObjects created, deleted, or
   * modified, and all unique values allocated and freed during the
   * course of actions on this transaction will be reverted to their
   * state when this transaction was created. </p>
   *
   * <p>Note that this does not mean that the entire DBStore will revert
   * to its state at the beginning of this transaction;  any changes not
   * relating to objects and namespace values connected to this transaction
   * will not be affected by this transaction's release. </p>
   *
   */

  public synchronized void release()
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

  /*--------------------------------------------------------------------
    
    private convenience methods

    note that in the release methods, we don't clear anything out of
    the basesModified hash.  It's not worth keeping track of whether
    or not a given object was the only object in the editset that was
    part of the DBObjectBase in question.

    These are private because they don't handle the namespace
    implications of releasing a modified object from the transaction.

    For the time being, we don't allow an object to be taken out of
    a transaction without aborting the whole transaction.

  --------------------------------------------------------------------*/

  private synchronized void releaseCreatedObject(DBEditObject object)
  {
    objectsCreated.removeElement(object);
  }

  private synchronized void releaseChangedObject(DBEditObject object)
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

  private synchronized void releaseDeletedObject(DBObject object)
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

}
