/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

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

  static final boolean debug = true;

  Vector
    objects = null;

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
    objects = new Vector();
    basesModified = new Hashtable(dbStore.objectBases.size());
  }

  /**
   *
   * Method to find a DBObject / DBEditObject if it has
   * previously been checked out to this EditSet in some
   * fashion.
   *
   */

  public synchronized DBObject findObject(DBObject object)
  {
    return findObject(object.getInvid());
  }

  /**
   *
   * Method to find a DBObject / DBEditObject if it has
   * previously been checked out to this EditSet in some
   * fashion.  This method is used to allow consistency
   * check code in the DBEditObjects to get a transaction
   * consistent view of the system as it stands with the
   * transaction's changes made.
   *
   */

  public synchronized DBObject findObject(Invid invid)
  {
    Enumeration enum;
    DBObject obj;

    /* -- */

    enum = object.elements();

    while (enum.hasMoreElements())
      {
	obj = (DBObject) enum.nextElement();
	if (obj.getInvid().equals(invid))
	  {
	    return obj;
	  }
      }

    return null;
  }

  /**
   *
   * Method to associate a DBEditObject with this transaction.
   *
   * @param object The newly created DBEditObject.
   *
   */

  public synchronized void addObject(DBEditObject object)
  {
    if (!objects.contains(object))
      {
	objects.addElement(object);

	// just need something to mark the slot in the hash table,
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

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): entering");
      }

    if (objects == null)
      {
	throw new RuntimeException("already committed or released");
      }

    baseSet = new Vector();
    enum = basesModified.keys();

    while (enum.hasMoreElements())
      {
	baseSet.addElement(enum.nextElement());
      }

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): acquiring write lock");
      }

    wLock = new DBWriteLock(dbStore, baseSet);
    
    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): created write lock");
      }

    wLock.establish(session.key);	// wait for write lock

    // This yield is here to allow me to verify that the locking logic
    // works properly.

    Thread.yield();

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): established write lock");
      }

    // verify phase one of our commit protocol

    for (int i = 0; i < objects.size(); i++)
      {
	eObj = (DBEditObject) objects.elementAt(i);

	if (!eObj.commitPhase1())
	  {
	    release();
	    return false;
	  }
      }

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

	// log this condition somehow

	release();
	return false;
      }

    // phase one complete, go ahead and have all our
    // objects do their 2nd level commit processes

    for (int i = 0; i < objects.size(); i++)
      {
	eObj = (DBEditObject) objects.elementAt(i);

	eObj.commitPhase2();
      }

    // and make the changes in our in-memory hashes..

    // create new, non-editable versions of our newly created objects
    // and insert them into the appropriate slots in our DBStore

    for (int i = 0; i < objects.size(); i++)
      {
	eObj = (DBEditObject) objects.elementAt(i);

	base = eObj.objectBase;

	// Create a new DBObject from our DBEditObject and insert
	// into the object hash

	switch (eObj.getStatus())
	  {
	  case DBEditObject.CREATING:
	  case DBEditObject.EDITING:
	    base.objectHash.put(new Integer(eObj.id), new DBObject(eObj));
	    break;

	  case DBEditObject.DELETING:
	    base.objectHash.remove(new Integer(obj.id));	    
	    break;

	  case DBEditObject.DROPPING:
	    // just forget about it
	    break;
	  }
      }

    // confirm all namespace modifications associated with this editset
    // and release namespace values that correspond with old
    // object / field values.

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).commit(this);
      }

    wLock.release();

    // null our vector to speed GC

    objects = null;

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
    DBEditObject eObj;

    /* -- */

    if (objects == null)
      {
	throw new RuntimeException("already committed or released");
      }

    while (objects.size() > 0)
      {
	eObj = (DBEditObject) objects.firstElement();
	eObj.release();
	
	switch (eObj.getStatus())
	  {
	  case DBEditObject.CREATING:
	  case DBEditObject.DROPPING:
	    break;

	  case DBEditObject.EDITING:
	  case DBEditObject.DELETING:
	    if (!eObj.original.clearShadow(this))
	      {
		throw new RuntimeException("editset ownership synchronization error");
	      }
	    break;
	  }
	objects.removeElement(eObj);
      }

    basesModified.clear();

    // undo all namespace modifications associated with this editset

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).abort(this);
      }

    objects = null;
  }
}
