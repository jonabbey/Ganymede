/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.43 $ %D%
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

  static final boolean debug = false;

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

  public DBEditObject findObject(DBObject object)
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

    if (debug)
      {
	System.err.println("DBEditSet.checkpoint(): checkpointing key " + name);
      }

    checkpoints.push(new DBCheckPoint(name, this));

    // and our namespaces

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).checkpoint(this, name);
      }
  }

  /**
   *
   * This method is used to pop a checkpoint off the checkpoint
   * stack.  This method is equivalent to a rollback where the
   * checkpoint information is taken off the stack, but this
   * DBEditSet's state is not reverted.<br><br>
   *
   * Any checkpoints that were placed on the stack after
   * the checkpoint matching &lt;name&gt; will also be
   * removed from the checkpoint stack.
   *
   * @param name An identifier for the checkpoint to take off
   * the checkpoint stack.

   * @return null if the checkpoint could not be found on the
   * stack, or the DBCheckPoint object representing the state
   * of the transaction at the checkpoint time if the checkpoint
   * could be found.
   *
   */

  public DBCheckPoint popCheckpoint(String name)
  {
    return popCheckpoint(name, false);
  }

  /**
   *
   * This method is used to pop a checkpoint off the checkpoint
   * stack.  This method is equivalent to a rollback where the
   * checkpoint information is taken off the stack, but this
   * DBEditSet's state is not reverted.<br><br>
   *
   * Any checkpoints that were placed on the stack after
   * the checkpoint matching &lt;name&gt; will also be
   * removed from the checkpoint stack.
   *
   * @param name An identifier for the checkpoint to take off
   * the checkpoint stack.
   *
   * @param inRollback If true, popCheckpoint will not actually
   * pop the checkpoint states out of the DBStore namespaces,
   * leaving that for rollback() to finish up.
   *
   * @return null if the checkpoint could not be found on the
   * stack, or the DBCheckPoint object representing the state
   * of the transaction at the checkpoint time if the checkpoint
   * could be found.
   *
   */

  public synchronized DBCheckPoint popCheckpoint(String name, boolean inRollback)
  {
    DBCheckPoint point = null;
    boolean found;

    /* -- */

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
	System.err.println("DBEditSet.popCheckpoint: couldn't find checkpoint for " + name);
	return null;
      }

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
	    System.err.println("DBEditSet.popCheckpoint(): popping checkpoint " + point.name);
	  }
      }

    if (!inRollback)
      {
	// we don't synchronize on dbStore, the odds are zip that a
	// namespace will be created or deleted while we are in the middle
	// of a transaction.  Go ahead and clear out the namespace checkpoint.
	
	for (int i = 0; i < dbStore.nameSpaces.size(); i++)
	  {
	    ((DBNameSpace) dbStore.nameSpaces.elementAt(i)).popCheckpoint(this, name);
	  }
      }

    if (found)
      {
	return point;
      }
    else
      {
	return null;
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

    if (debug)
      {
	System.err.println("DBEditSet.rollback(): rollback key " + name);
      }

    point = popCheckpoint(name, true);

    if (point == null)
      {
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

	if (debug)
	  {
	    System.err.println("Object in transaction at checkpoint time: " + objck.invid.toString());
	  }

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

	if (debug)
	  {
	    System.err.println("DBEditSet.rollback(): object in transaction at rollback time: " + obj.getLabel() +
			       " (" + obj.getInvid().toString() + ")");
	  }

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

	    if (debug)
	      {
		System.err.println("DBEditSet.rollback(): dropping object " + obj.getLabel() + " (" 
				   + obj.getInvid().toString() + ")");
	      }

	    drop.addElement(obj);
	  }
	else if (debug)
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
   * the changes for any reason, commit() will return a ReturnVal
   * indicating failure and the cause of the commit failure, and
   * will leave the transaction open for a subsequent commit() attempt,
   * or an abort().<br><br>
   *
   * The returned ReturnVal will have doNormalProcessing set to false if
   * the transaction was completely aborted.  Both DBSession and the client
   * should take a false doNormalProcessing boolean as an indicator that
   * the transaction was simply wiped out and a new transaction should
   * be opened for subsequent activity.  A true doNormalProcessing value
   * indicates that the client can try the commit again at a later time,
   * or manually cancel.
   *
   * @return a ReturnVal indicating success or failure, or null on success
   * without comment.
   * 
   */

  public synchronized ReturnVal commit()
  {
    Vector baseSet;
    Enumeration enum;
    DBObjectBase base;
    Object key;
    DBEditObject eObj, eObj2;
    ReturnVal retVal = null;

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

    // determine what bases we need to lock to do this commit

    baseSet = new Vector();
    enum = basesModified.keys();

    while (enum.hasMoreElements())
      {
	baseSet.addElement(enum.nextElement());
      }

    // and try to lock the bases down.

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): acquiring write lock");
      }

    // We'll first check to see if we already have a lock open from a
    // prior failed commit() attempt.

    if (wLock != null)
      {
	if (wLock.inEstablish)
	  {
	    wLock.abort();	// we've got an old lock.. force it to die.. will cause the old lock
                        	// thread's establish method to have an interrupted exception thrown
	    wLock = null;

	    Ganymede.debug(session.key + ": DBEditSet.commit(): killing dead write lock");
	  }
	else
	  {
	    System.err.println(session.key +
			       ": DBEditSet.commit(): dead (?) write lock already established.. can't commit");

	    retVal =  Ganymede.createErrorDialog("Commit failure",
						 "Couldn't commit transaction, there is an existing write " +
						 "lock interfering with commit.");

	    // we didn't kill off this transaction, we just couldn't commit it at
	    // this time.. let the DBSession and client code know that.

	    retVal.doNormalProcessing = true;

	    return retVal;
	  }
      }

    // Create the lock on the bases changed

    wLock = new DBWriteLock(dbStore, baseSet);
    
    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): created write lock");
      }

    // and establish.  This establish() call will cause our thread to block
    // until we can get all the bases we need locked.

    try
      {
	wLock.establish(session.key);	// wait for write lock
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug("DBEditSet.commit(): lock aborted, commit failed, releasing transaction for " + 
		       session.key);
	release();
	wLock = null;

	return Ganymede.createErrorDialog("Commit failure",
					  "Couldn't commit transaction, our write lock was denied.. server going down?");
      }

    // This yield is here to allow me to verify that the locking logic
    // works properly on a non-preemptive VM.

    Thread.yield();

    if (debug)
      {
	System.err.println(session.key +
			   ": DBEditSet.commit(): established write lock");
      }

    // write the creation and / or modification info into the objects,
    // run the objects' commit routines.

    DateDBField df;
    StringDBField sf;
    String result;

    // this is the mother of all try clauses.. its purpose is to help
    // us recover from any unforeseen exceptions in this area.  The
    // code that writes transactions to the journal could cause the
    // journal to be slightly corrupted, but otherwise nothing in this
    // try does anything that aborting the transaction can't undo.
    //
    // well, that's not quite true.  the phase 2 commit code by
    // definition may do something that would cause exterior databases
    // to be updated, but if we get an exception thrown in that part
    // of the code, we still have to try to do something.

    try
      {
	// we're going to checkpoint the transaction here so that we
	// can abort cleanly if something rejects go-ahead in
	// commitPhase1().  commitPhase1() may not make any changes
	// that we need to worry about, but once we call
	// commitPhase1() on an object, we can't make _any_ changes to
	// it.  So, we need to set the modification times and what-not
	// before we call commitPhase1() and find out whether the object
	// considers itself good to go.  In theory, we could avoid the
	// checkpointing altogether and just assume that our timestamp
	// changes to the object won't ever have any bearing on the
	// object's commitPhase1() go/no go, but that would require
	// special-case coding, whereas we can just use the checkpoint
	// system to achieve the same ends generically.
	
	String checkpointLabel = "commit" + (new Date()).toString();

	checkpoint(checkpointLabel);

	// Ok, we now need to go through the objects that are being
	// changed by this transaction and update their informational
	// fields.
    
	// Note that we are assuming here that none of the changes we
	// make at this point (setting creation dates, modification
	// dates, etc.) will alter the working set of this transaction
	// (i.e., no new objects will be brought into the transaction
	// by anything we do here.)

	for (int i = 0; i < objects.size(); i++)
	  {
	    eObj = (DBEditObject) objects.elementAt(i);

	    switch (eObj.getStatus())
	      {
	      case DBEditObject.CREATING:
		    
		if (!eObj.isEmbedded())
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

		// * fall-through *

	      case DBEditObject.EDITING:
		    
		if (!eObj.isEmbedded())
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
		else
		  {
		    InvidDBField invf = (InvidDBField) eObj.getField(SchemaConstants.ContainerField);
			
		    if (invf == null || invf.getValueLocal() == null)
		      {
			Ganymede.debug("DBEditSet.commit(): WARNING, an embedded object's " +
				       "container link is undefined or null-valued.(?!)");
		      }
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

	// Check all the objects that we have checked out to make sure their
	// commit logic approves the transaction.

	for (int i = 0; i < objects.size(); i++)
	  {
	    eObj = (DBEditObject) objects.elementAt(i);

	    if (eObj.getGSession().enableOversight)
	      {
		if ((eObj.getStatus() != DBEditObject.DELETING) &&
		    (eObj.getStatus() != DBEditObject.DROPPING))
		  {
		    retVal = eObj.checkRequiredFields();
		  }

		if (retVal != null && !retVal.didSucceed())
		  {
		    // we're not going to clear out the transaction, 
		    // as there is a chance the user can fix things
		    // up.  We need to clear the committing flag on
		    // all objects that we've called commitPhase1()
		    // on so that they will accept future changes.
		
		    for (int j = 0; j < i; j++)
		      {
			eObj2 = (DBEditObject) objects.elementAt(j);
			eObj2.release(); // undo committing flag
		      }
		
		    releaseWriteLock("transaction commit rejected in phase 1 for missing fields");
		
		    // undo the time stamp changes and what-not.
		
		    rollback(checkpointLabel);
		
		    // let DBSession/the client know they can retry things.
		
		    retVal.doNormalProcessing = true;
		    return retVal;
		  }
	      }

	    retVal = eObj.commitPhase1();
	
	    if (retVal != null && !retVal.didSucceed())
	      {
		// we're not going to clear out the transaction, 
		// as there is a chance the user can fix things
		// up.  We need to clear the committing flag on
		// all objects that we've called commitPhase1()
		// on so that they will accept future changes.

		for (int j = 0; j <= i; j++)
		  {
		    eObj2 = (DBEditObject) objects.elementAt(j);
		    eObj2.release(); // undo committing flag
		  }
	    
		releaseWriteLock("transaction commit rejected in phase 1");

		// undo the time stamp changes and what-not.

		rollback(checkpointLabel);

		// let DBSession/the client know they can retry things.

		retVal.doNormalProcessing = true;
		return retVal;
	      }
	  }

	// we've gotten this far, phase one must have completed ok.

	popCheckpoint(checkpointLabel);

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

		// Ganymede.createErrorDialog() sets doNormalProcessing to false,
		// so DBSession and the client should know we're doing a total
		// scrub.
		
		return Ganymede.createErrorDialog("Couldn't commit transaction, couldn't write transaction to disk",
						  "Couldn't commit transaction, the server may have run out of" +
						  " disk space.  Couldn't write transaction to disk.");
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

	    // Ganymede.createErrorDialog() sets doNormalProcessing to false,
	    // so DBSession and the client should know we're doing a total
	    // scrub.

	    return Ganymede.createErrorDialog("Couldn't commit transaction, IOException caught writing journal",
					      "Couldn't commit transaction, the server may have run out of" +
					      " disk space.");
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
	// objects do their 2nd level (external) commit processes

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
	releaseWriteLock("exception caught while preparing trans.");
	release();

	// Ganymede.createErrorDialog() sets doNormalProcessing to false,
	// so DBSession and the client should know we're doing a total
	// scrub.

	retVal = Ganymede.createErrorDialog("Couldn't commit transaction, exception caught in DBEditSet.commit()",
					    ex.getMessage());
	return retVal;
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
		// Create a read-only version of eObj, with all fields
		// reset to checked-in status, put it into our object hash

		base.objectTable.putNoSync(new DBObject(eObj)); // noSync ok, since we are write-locked
		base.store.checkIn(); // update checkout count
	      }
	    catch (RemoteException ex)
	      {
		throw new Error("couldn't save edited object " + eObj + ", we're down in flames.");
	      }
	    break;

	  case DBEditObject.DELETING:

	    // Deleted objects had their deletion finalization done before
	    // we ever got to this point.  

	    // Note that we don't try to release the id for previously
	    // registered objects.. the base.releaseId() method really
	    // can only handle popping object id's off of a stack, and
	    // can't do anything for object id's unless the id was the
	    // last one allocated in that base, which is unlikely
	    // enough that we don't worry about it here.

	    base.objectTable.removeNoSync(eObj.id); // noSync ok, since we are write-locked
	    base.store.checkIn(); // count it as checked in once it's deleted
	    break;

	  case DBEditObject.DROPPING:

	    // dropped objects had their deletion finalization done before
	    // we ever got to this point.. 

	    base.releaseId(eObj.getID()); // relinquish the unused invid
	    base.store.checkIn(); // count it as checked in once it's deleted
	    break;
	  }
      }

    if (debug)
      {
	System.err.println(session.key +
			   ": DBEditSet.commit(): transaction objects integrated into in-memory hashes");
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

    // clear out any checkpoints that may be lingering

    checkpoints = new Stack();

    // we're going to return a ReturnVal with doNormalProcessing set to
    // false to let everyone above us know that we've totally cleared
    // out this transaction, being as we were successful and all.

    retVal = new ReturnVal(true, false);

    return retVal;
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
    checkpoints = new Stack();
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

  static final boolean debug = false;

  // ---

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

	if (debug)
	  {
	    System.err.println("DBCheckPoint: add " + obj.getLabel() + 
			       " (" + obj.getInvid().toString() + ")");
	  }

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
