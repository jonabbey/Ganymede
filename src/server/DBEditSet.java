/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.18 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

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
  String description;

  /* -- */

  /**
   * Constructor for DBEditSet
   *
   * @param dbStore The owning DBStore object.
   * @param session The DBStore session owning this transaction.
   *
   */

  public DBEditSet(DBStore dbStore, DBSession session, String description)
  {
    this.session = session;
    this.dbStore = dbStore;
    this.description = description;
    objects = new Vector();
    basesModified = new Hashtable(dbStore.objectBases.size());
  }

  /**
   *
   * Method to return the DBSession handle owning this
   * transaction.
   *
   */

  public DBSession getSession()
  {
    return session;
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

    enum = objects.elements();

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

    Date modDate = new Date();

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

    try
      {
	wLock.establish(session.key);	// wait for write lock
      }
    catch (InterruptedException ex)
      {
	release();
	return false;
      }

    // This yield is here to allow me to verify that the locking logic
    // works properly on a non-preemptive VM.

    Thread.yield();

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): established write lock");
      }

    // write the creation and / or modification info into the object and 
    // verify phase one of our commit protocol

    DateDBField df;
    StringDBField sf;
    String result;

    for (int i = 0; i < objects.size(); i++)
      {
	eObj = (DBEditObject) objects.elementAt(i);

	switch (eObj.getStatus())
	  {
	  case DBEditObject.CREATING:
	    
	    df = (DateDBField) eObj.getField(SchemaConstants.CreationDateField);
	    df.setValue(modDate);

	    sf = (StringDBField) eObj.getField(SchemaConstants.CreatorField);

	    result = session.getID();

	    if (description != null)
	      {
		result += ": " + description;
	      }

	    sf.setValue(result);

	  case DBEditObject.EDITING:

	    df = (DateDBField) eObj.getField(SchemaConstants.ModificationDateField);
	    df.setValue(modDate);

	    sf = (StringDBField) eObj.getField(SchemaConstants.ModifierField);

	    result = session.getID();

	    if (description != null)
	      {
		result += ": " + description;
	      }

	    sf.setValue(result);

	    break;

	  case DBEditObject.DELETING:
	    break;

	  case DBEditObject.DROPPING:
	    break;
	  }

	if (!eObj.commitPhase1())
	  {
	    release();
	    Ganymede.debug("Transaction commit rejected in phase 1");
	    return false;
	  }
      }

    // need to clear out any transients before
    // we write the transaction out to disk

    for (int i = 0; i < objects.size(); i++)
      {
	eObj = (DBEditObject) objects.elementAt(i);
	eObj.clearTransientFields();
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
	Ganymede.debug("IO exception in transaction commit");
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

	base = eObj.getBase();
	base.updateTimeStamp();

	// Create a new DBObject from our DBEditObject and insert
	// into the object hash

	switch (eObj.getStatus())
	  {
	  case DBEditObject.CREATING:
	  case DBEditObject.EDITING:
	    try
	      {
		base.objectHash.put(new Integer(eObj.id), new DBObject(eObj));
		base.store.checkIn(); // update checkout count
	      }
	    catch (RemoteException ex)
	      {
		throw new Error("couldn't save edited object " + eObj + ", we're down in flames.");
	      }
	    break;

	  case DBEditObject.DELETING:
	    base.objectHash.remove(new Integer(eObj.id));
	    base.store.checkIn(); // count it as checked out until it's deleted
	    break;

	  case DBEditObject.DROPPING:
	    base.releaseId(eObj.getID()); // relinquish the unused invid
	    base.store.checkIn(); // count it as checked out until it's deleted
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
	    eObj.getBase().releaseId(eObj.getID()); // relinquish the unused invid
	    eObj.getBase().store.checkIn(); // update checked out count
	    break;

	  case DBEditObject.EDITING:
	  case DBEditObject.DELETING:

	    // note that clearShadow updates the checked out count for us.

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
