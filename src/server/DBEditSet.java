/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.32 $ %D%
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
    objects = null,
    logEvents = null;

  Hashtable basesModified;

  DBStore dbStore;
  DBSession session;
  String description;

  Stack checkpoints = new Stack();

  DBWriteLock wLock = null;

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
    logEvents = new Vector();
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

  public synchronized DBEditObject findObject(DBObject object)
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

  public synchronized DBEditObject findObject(Invid invid)
  {
    Enumeration enum;
    DBEditObject obj;

    /* -- */

    enum = objects.elements();

    while (enum.hasMoreElements())
      {
	obj = (DBEditObject) enum.nextElement();

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
   * This method checkpoints the current transaction at its current
   * state.  If need be, this transaction can later be rolled back
   * to this point by calling the rollback() method.
   *
   * @param name An identifier for this checkpoint
   *
   */

  public synchronized void checkpoint(String name)
  {
    // check point our objects

    System.err.println("DBEditSet.checkpoint(): checkpointing key " + name);

    checkpoints.push(new DBCheckPoint(name, this));

    // and our namespaces

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).checkpoint(this, name);
      }
  }

  /**
   *
   * This brings this transaction back to the state it was
   * at at the time of the matching checkPoint() call.  Any
   * objects that were checked out in care of this transaction
   * since the checkPoint() will be checked back into the
   * database and made available for other transactions to
   * access.  All namespace changes made by this transaction
   * will likewise be rolled back to their state at the
   * checkpoint.
   *
   * @param name An identifier for the checkpoint to be rolled back to.
   *
   */

  public synchronized boolean rollback(String name)
  {
    DBCheckPoint point = null;
    DBCheckPointObj objck;
    DBEditObject obj;
    boolean found;

    /* -- */

    System.err.println("DBEditSet.rollback(): rollback key " + name);

    found = false;

    for (int i = 0; i < checkpoints.size(); i++)
      {
	point = (DBCheckPoint) checkpoints.elementAt(i);

	if (point.name.equals(name))
	  {
	    found = true;
	  }
      }

    if (!found)
      {
	System.err.println("DBEditSet.rollback: couldn't find rollback for " + name);
	return false;
      }

    // ok, we know it's in there.. now pop down the stack til we find it.

    found = false;

    while (!found && !checkpoints.empty())
      {
	point = (DBCheckPoint) checkpoints.pop();

	if (point.name.equals(name))
	  {
	    found = true;
	  }
	else
	  {
	    System.err.println("DBEditSet.rollback(): popping checkpoint " + point.name);
	  }
      }

    // this really never will happen, but it costs us little to check

    if (!found)
      {
	System.err.println("DBEditSet.rollback: couldn't find rollback for " + name);
	return false;
      }

    // rollback our mail/log events

    logEvents = point.logEvents;

    // and our objects

    // first, take care of all the objects that were in the transaction at
    // the time of this checkpoint.. we want to revert these objects to
    // their checkpoint-time status

    for (int i = 0; i < point.objects.size(); i++)
      {
	objck = (DBCheckPointObj) point.objects.elementAt(i);

	System.err.println("Object in transaction at checkpoint time: " + objck.invid.toString());

	found = false;

	for (int j = 0; !found && j < objects.size(); j++)
	  {
	    obj = (DBEditObject) objects.elementAt(j);
	    
	    if (obj.getInvid().equals(objck.invid))
	      {
		obj.rollback(objck.fields);
		obj.status = objck.status;
		found = true;
	      }
	  }

	if (!found)
	  {
	    // huh?  this shouldn't ever happen, unless maybe we have a rollback order
	    // error or something.  Complain.

	    throw new RuntimeException("DBEditSet.rollback error.. we lost checked out objects in midstream?");
	  }
      }

    // now, we have to sweep out any objects that are in the transaction now
    // that weren't in the transaction at the checkpoint.

    // note that we need a drop temp vector because it confuses things
    // if we remove elements from objects while we are iterating over it

    Vector drop = new Vector();

    for (int i = 0; i < objects.size(); i++)
      {
	obj = (DBEditObject) objects.elementAt(i);

	System.err.println("DBEditSet.rollback(): object in transaction at rollback time: " + obj.getLabel() +
			   " (" + obj.getInvid().toString() + ")");

	found = false;

	for (int j = 0; !found && j < point.objects.size(); j++)
	  {
	    objck = (DBCheckPointObj) point.objects.elementAt(j);

	    if (obj.getInvid().equals(objck.invid))
	      {
		found = true;
	      }
	  }

	if (!found)
	  {
	    // ok, we've got a new object.  Ditch it.

	    obj.release();
	
	    switch (obj.getStatus())
	      {
	      case DBEditObject.CREATING:
	      case DBEditObject.DROPPING:
		obj.getBase().releaseId(obj.getID()); // relinquish the unused invid
		obj.getBase().store.checkIn(); // update checked out count
		break;
		
	      case DBEditObject.EDITING:
	      case DBEditObject.DELETING:
		
		// note that clearShadow updates the checked out count for us.
		
		if (!obj.original.clearShadow(this))
		  {
		    throw new RuntimeException("editset ownership synchronization error");
		  }
		break;
	      }

	    System.err.println("DBEditSet.rollback(): dropping object " + obj.getLabel() + " (" 
			       + obj.getInvid().toString() + ")");

	    drop.addElement(obj);
	  }
	else
	  {
	    System.err.println("DBEditSet.rollback(): keeping object " + obj.getLabel() + " (" 
			       + obj.getInvid().toString() + ")");
	  }
      }

    // now go ahead and clean out objects

    for (int i = 0; i < drop.size(); i++)
      {
	objects.removeElement(drop.elementAt(i));
      }

    // and our namespaces

    boolean success = true;

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	if (!((DBNameSpace) dbStore.nameSpaces.elementAt(i)).rollback(this, name))
	  {
	    success = false;
	  }
      }

    return success;
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

    if (wLock != null)
      {
	if (wLock.inEstablish)
	  {
	    wLock.abort();	// we've got an old lock.. force it to die.. will cause the old lock
                        	// thread's establish method to have an interrupted exception thrown
	    wLock = null;

	    System.err.println(session.key + ": DBEditSet.commit(): aborting dead write lock");
	  }
	else
	  {
	    System.err.println(session.key + ": DBEditSet.commit(): dead (?) write lock already established.. can't commit");
	    release();
	    return false;
	  }
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
	Ganymede.debug("DBEditSet.commit(): lock aborted, commit failed, releasing transaction for " + session.key);
	release();
	wLock = null;
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

    // this is the mother of all try clauses.. its purpose is to help us recover from
    // any unforeseen exceptions in this area.  The code that writes transactions
    // to the journal could cause the journal to be slightly corrupted, but otherwise
    // nothing in this try does anything that aborting the transaction can't undo.
    //
    // well, that's not quite true.  the phase 2 commit code by definition may do
    // something that would cause exterior databases to be updated, but if we
    // get an exception thrown in that part of the code, we still have to try
    // to do something.

    try
      {
	boolean done = false;

	// we need to loop over the objects in our transaction until
	// we don't have any objects in the transaction that need to
	// be processed.  The act of handling the remove logic for an
	// object may cause other objects to be pulled into this transaction's
	// list of touched objects, in which case we'll need to keep looping
	// until things settle out.

	// clone the object list so that we don't loop
	// over a list of objects that is going to change on us

	Vector objList = (Vector) objects.clone();

	while (!done)
	  {
	    System.err.println("DBEditSet.commit(): looping the loop");

	    // assume we're done, until we find out otherwise

	    done = true;

	    for (int i = 0; i < objList.size(); i++)
	      {
		eObj = (DBEditObject) objList.elementAt(i);

		if (!eObj.finalized)
		  {
		    done = false; // we'll need to loop through again
		  }
		else
		  {
		    continue;	// skip this object, we've already processed it
		  }

		switch (eObj.getStatus())
		  {
		  case DBEditObject.CREATING:
		    
		    if (!eObj.getBase().isEmbedded())
		      {
			df = (DateDBField) eObj.getField(SchemaConstants.CreationDateField);
			df.setValueLocal(modDate);
			
			sf = (StringDBField) eObj.getField(SchemaConstants.CreatorField);
			
			result = session.getID();
			
			if (description != null)
			  {
			    result += ": " + description;
			  }

			sf.setValueLocal(result);
		      }

		  case DBEditObject.EDITING:
		    
		    if (!eObj.getBase().isEmbedded())
		      {
			df = (DateDBField) eObj.getField(SchemaConstants.ModificationDateField);
			df.setValueLocal(modDate);

			sf = (StringDBField) eObj.getField(SchemaConstants.ModifierField);

			result = session.getID();

			if (description != null)
			  {
			    result += ": " + description;
			  }

			sf.setValueLocal(result);
		      }

		    eObj.finalized = true;
		    break;

		  case DBEditObject.DELETING:
		  case DBEditObject.DROPPING:

		    // Deletion activities for this object were done at the time of the
		    // client's request.. the commit logic may have something to do,
		    // however.

		    eObj.finalized = true;
		    break;
		  }
	      }
	  }

	// now do the commit logic for the objects

	for (int i = 0; i < objects.size(); i++)
	  {
	    eObj = (DBEditObject) objects.elementAt(i);

	    if (!eObj.commitPhase1())
	      {
		releaseWriteLock("transaction commit rejected in phase 1");
		release();
		Ganymede.debug("Transaction commit rejected in phase 1");
		return false;
	      }
	  }

	// need to clear out any transients before
	// we write the transaction out to disk

	GanymedeSession gSession = session.getGSession();
	Vector invids;

	for (int i = 0; i < objects.size(); i++)
	  {
	    eObj = (DBEditObject) objects.elementAt(i);

	    // anything that's going back into the DBStore needs
	    // to have the transient fields cleared away

	    if (eObj.getStatus() == DBEditObject.EDITING ||
		eObj.getStatus() == DBEditObject.CREATING)
	      {
		eObj.clearTransientFields();
	      }

	    // we'll want to log the before/after state of any objects
	    // edited by this transaction

	    if (Ganymede.log != null)
	      {
		if (eObj.getStatus() == DBEditObject.EDITING)
		  {
		    invids = new Vector();
		    invids.addElement(eObj.getInvid());
		   
		    if (debug)
		      {
			System.err.println("Logging event for " + eObj.getLabel());
		      }
		    
		    String diff = eObj.diff();

		    if (diff != null)
		      {
			
			if (debug)
			  {
			    System.err.println("**** DIFF (" + eObj.getLabel() + "):" + diff + " : ENDDIFF****");
			  }
			
			logEvents.addElement(new DBLogEvent("objectchanged",
							    eObj.getTypeDesc() + ":" + eObj.getLabel() + " --\n" + diff,
							    (gSession.personaInvid == null ?
							     gSession.userInvid : gSession.personaInvid),
							    gSession.username,
							    invids,
							    null));
		      }
		  }
	      }
	  }

	// write this transaction out to the Journal

	try
	  {
	    if (!dbStore.journal.writeTransaction(this))
	      {
		releaseWriteLock("couldn't write transaction to disk");
		release();
		Ganymede.debug("DBEditSet.commit(): Couldn't write transaction to disk");
		return false;
	      }
	  }
	catch (IOException ex)
	  {
	    // we probably want to be more extreme here.. if we couldn't write out
	    // the transaction, we are probably out of space on the filesystem
	    // with the journal/dbstore file.

	    // log this condition somehow

	    releaseWriteLock("IO Exception in transaction commit");
	    release();
	    Ganymede.debug("IO exception in transaction commit");
	    return false;
	  }

	// log it

	if (Ganymede.log != null)
	  {
	    Ganymede.log.logTransaction(logEvents, gSession.username, 
					(gSession.personaInvid == null ?
					 gSession.userInvid : gSession.personaInvid));
	  }

	if (debug)
	  {
	    System.err.println(session.key + ": DBEditSet.commit(): transaction written to disk");
	  }

	// phase one complete, go ahead and have all our
	// objects do their 2nd level commit processes

	for (int i = 0; i < objects.size(); i++)
	  {
	    eObj = (DBEditObject) objects.elementAt(i);

	    eObj.commitPhase2();
	  }
      }
    catch (Exception ex)
      {
	Ganymede.debug("** Caught exception while preparing transaction for commit: " + ex);
	Ganymede.debug("** aborting transaction");
	ex.printStackTrace();
	Ganymede.debug(ex.getMessage());
	releaseWriteLock("exception caught while preparing trans.");
	release();
	return false;
      }

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): phase 2 committed");
      }

    // and make the changes in our in-memory hashes..

    // create new, non-editable versions of our newly created objects
    // and insert them into the appropriate slots in our DBStore

    // note that once we get to this point, we're not taking no
    // for an answer.. we don't check within this loop to see
    // if our write lock has been pulled.  we just have to hope
    // it wasn't.  Any exceptions thrown at this point will
    // propagate up, and let the chips fall where they may.

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

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): transaction objects integrated into in-memory hashes");
      }

    // confirm all namespace modifications associated with this editset
    // and release namespace values that correspond with old
    // object / field values.

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).commit(this);
      }

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): namespace changes committed");
      }

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): releasing write lock");
      }

    releaseWriteLock("successful commit");

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): released write lock");
      }

    // null our vector to speed GC

    objects = null;
    session = null;

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
    session = null;
  }

  /**
   *
   * This is a dinky little private helper method to keep things
   * clean.  It's essential that wLock be released if things go
   * wrong, else next time this session tries to commit a transaction,
   * it'll wind up waiting forever for the old lock to be released.
   *
   */

  private void releaseWriteLock(String reason)
  {
    if (wLock != null)
      {
	wLock.release();
	wLock = null;
	
	if (debug)
	  {
	    System.err.println(session.key + ": DBEditSet.commit(): released write lock: " + reason);
	  }
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBCheckPoint

------------------------------------------------------------------------------*/

/**
 * DBCheckPoint is a class designed to allow server-side code that
 * needs to attempt a multi-step operation that might not successfully
 * complete to be able to undo all changes made without having to
 * abort the entire transaction.
 * 
 * In other words, a DBCheckPoint is basically a transaction within a transaction.
 *
 */

class DBCheckPoint {

  String 
    name;

  Vector
    objects = null,
    logEvents = null;

  /* -- */

  DBCheckPoint(String name, DBEditSet transaction)
  {
    DBEditObject obj;

    /* -- */

    this.name = name;

    // assume that log events are not going to change once recorded,
    // so we can make do with a shallow copy.

    logEvents = (Vector) transaction.logEvents.clone();

    objects = new Vector();

    for (int i = 0; i < transaction.objects.size(); i++)
      {
	obj = (DBEditObject) transaction.objects.elementAt(i);

	System.err.println("DBCheckPoint: add " + obj.getLabel() + 
			   " (" + obj.getInvid().toString() + ")");

	objects.addElement(new DBCheckPointObj(obj));
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 DBCheckPointObj

------------------------------------------------------------------------------*/

/**
 * DBCheckPoint is a class designed to allow server-side code that
 * needs to attempt a multi-step operation that might not successfully
 * complete to be able to undo all changes made without having to
 * abort the entire transaction.
 * 
 * In other words, a DBCheckPoint is basically a transaction within a transaction.
 *
 */

class DBCheckPointObj {

  Invid invid;
  Hashtable fields;
  byte status;

  /* -- */

  DBCheckPointObj(DBEditObject obj)
  {
    this.invid = obj.getInvid();
    this.status = obj.status;
    this.fields = obj.checkpoint();
  }
}
