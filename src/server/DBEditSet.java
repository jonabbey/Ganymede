/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.55 $
   Last Mod Date: $Date: 1999/04/28 06:46:51 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
 * <p>DBEditSet is the basic transactional unit.  All changes to the
 * database during normal operations are made in the context of a
 * DBEditSet, which may then be committed or rolled back as an atomic
 * operation.  Each {@link arlut.csd.ganymede.DBSession DBSession} will
 * have at most one DBEditSet transaction object active at any time.</p>
 *
 * <p>A DBEditSet tracks several things for the server, including instances of
 * {@link arlut.csd.ganymede.DBEditObject DBEditObject}'s that were created
 * or checked-out from the {@link arlut.csd.ganymede.DBStore DBStore},
 * {@link arlut.csd.ganymede.DBNameSpace DBNameSpace} values that were reserved
 * during the course of the transaction, and {@link arlut.csd.ganymede.DBLogEvent DBLogEvent}
 * objects to be recorded in the {@link arlut.csd.ganymede.DBLog DBLog} and/or
 * mailed out to various interested parties when the transaction is committed.</p>
 *
 * <p>DBEditSet's transaction logic is based on a two-phase commit
 * protocol, where all DBEditObject's involved in the transaction are
 * given an initial opportunity to approve or reject the transaction's
 * commit before the DBEditSet commit method goes back and 'locks-in'
 * the changes.  DBEditObjects are able to initiate changes external
 * to the Ganymede database in their
 * {@link arlut.csd.ganymede.DBEditObject#commitPhase2() commitPhase2()}
 * methods, if needed.</p>
 *
 * <p>When a DBEditSet is committed, a {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}
 * is established on all {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}'s
 * involved in the transaction.  All objects checked out by that transaction
 * are then updated in the DBStore, and a summary of changes is recorded to the
 * DBStore {@link arlut.csd.ganymede.DBJournal DBJournal}.  The database as
 * a whole will not be dumped to disk unless and until the
 * {@link arlut.csd.ganymede.dumpTask dumpTask} is run, or until the server
 * undergoes a formal shutdown.</p>
 *
 * <p>Typically, the {@link arlut.csd.ganymede.DBEditSet#commit() commit()}
 * method is called by the
 * {@link arlut.csd.ganymede.GanymedeSession#commitTransaction(boolean) GanymedeSession.commitTransaction()}
 * method, which will induce the server to schedule any commit-time build
 * tasks registered with the {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}.</p>
 *
 * <p>If a DBEditSet commit() operation fails catastrophically, or if
 * {@link arlut.csd.ganymede.DBEditSet#release() release()} is called,
 * all DBEditObjects created or checked out during the course of the
 * transaction will be discarded, all DBNameSpace values allocated will
 * be relinquished, and any logging information for the abandoned transaction
 * will be forgotten.</p>
 *
 * <p>As if all that wasn't enough, the DBEditSet class also maintains a stack
 * of {@link arlut.csd.ganymede.DBCheckPoint DBCheckPoint} objects to enable
 * users to set checkpoints during the course of a transaction.  These objects
 * are basically a snapshot of the transaction's state at the moment of the
 * checkpoint, and are used to rollback the transaction to a known state if
 * a series of linked operations within a transaction cannot all be completed.</p>
 *
 * <p>Finally, note that the DBEditSet class does not actually track namespace
 * value allocations.. instead, the DBNameSpace class is responsible for recording
 * a list of values allocated by each active DBEditSet.  When a DBEditSet commits
 * or releases, all DBNameSpace objects in the server are informed of this, whereupon
 * they do their own cleanup.</p>
 */

public class DBEditSet {

  static final boolean debug = false;

  /**
   * A list of {@link arlut.csd.ganymede.DBEditObject DBEditObject}'s
   * that were checked out in care of this transaction.
   */

  Vector objects = null;

  /**
   * A list of {@link arlut.csd.ganymede.DBLogEvent DBLogEvent}'s
   * to be written to the Ganymede logfile and/or mailed out when
   * this transaction commits.
   */

  Vector logEvents = null;

  /**
   * A record of the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}'s
   * touched by this transaction.  These DBObjectBase's will be locked
   * when this transaction is committed.
   */

  Hashtable basesModified;

  /**
   * Who's our daddy?
   */

  DBStore dbStore;

  /**
   * A reference to the DBSession that this transaction is attached to.
   */

  DBSession session;

  /** 
   * A brief description of the client associated with this
   * transaction, used in logging to identify what was done by the
   * main client, what by a password-changing utility, etc.
   */

  String description;

  /**
   * A stack of {@link arlut.csd.ganymede.DBCheckPoint DBCheckPoint} objects
   * to keep track of check points performed during the course of this transaction.
   */

  Stack checkpoints = new Stack();

  /**
   * The writelock acquired during the course of a commit attempt.  We keep
   * this around as a DBEditSet field so that we can use the handy
   * {@link arlut.csd.ganymede.DBEditSet#releaseWriteLock() releaseWriteLock()}
   * method, but wLock should really never be non-null outside of the
   * context of the commit() call.
   */

  DBWriteLock wLock = null;

  /* -- */

  /**
   * Constructor for DBEditSet
   *
   * @param dbStore The owning DBStore object.
   * @param session The DBStore session owning this transaction.
   * @param description An optional string to identify this transaction
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
   * <p>Method to return the DBSession handle owning this
   * transaction.</p>
   */

  public DBSession getSession()
  {
    return session;
  }

  /**
   * <p>Method to find a DBObject / DBEditObject if it has
   * previously been checked out to this EditSet in some
   * fashion.</p>
   */

  public DBEditObject findObject(DBObject object)
  {
    return findObject(object.getInvid());
  }

  /**
   * <p>Method to find a DBObject / DBEditObject if it has
   * previously been checked out to this EditSet in some
   * fashion.  This method is used to allow consistency
   * check code in the DBEditObjects to get a transaction
   * consistent view of the system as it stands with the
   * transaction's changes made.</p>
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
   * <p>Method to associate a DBEditObject with this transaction.</p>
   *
   * <p>This method is called by the createDBObject and editDBObject
   * methods in {@link arlut.csd.ganymede.DBSession DBSession}.</p>
   *
   * @param object The newly created DBEditObject.
   */

  public synchronized void addObject(DBEditObject object)
  {
    if (!objects.contains(object))
      {
	objects.addElement(object);

	// just need something to mark the slot in the hash table, to
	// indicate that this object's base is involved in the
	// transaction.

	basesModified.put(object.objectBase, this);	
      }
  }

  /**
   * <p>This method is used to register a log event with this transaction.
   * If the transaction successfully commits, the provided log event
   * will be recorded in the Ganymede log file and mail notification will
   * be sent out if appropriate.</p>
   *
   * @param eventClassToken a short string specifying a DBObject record describing
   * the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A vector of invids of objects involved in this event.
   * @param notifyList A vector of Strings listing email addresses to send notification
   * of this event to.
   */

  public void logEvent(String eventClassToken, String description,
		       Invid admin, String adminName,
		       Vector objects, Vector notifyList)
  {
    DBLogEvent event = new DBLogEvent(eventClassToken, description,
				      admin, adminName,
				      objects, notifyList);
    logEvents.addElement(event);
  }

  /**
   * <p>This method is used to register a log event with this transaction.
   * If the transaction successfully commits, the provided log event
   * will be recorded in the Ganymede log file and mail notification will
   * be sent out if appropriate.</p>
   *
   * @param event A pre-formed log event to register with this transaction.
   */

  public void logEvent(DBLogEvent event)
  {
    logEvents.addElement(event);
  }

  /**
   *
   * This method is used to record a message to be sent out when
   * the transaction is committed.
   *
   * @param addressList Vector of Strings, the address list
   * @param subject The subject line of the message
   * @param message The body of the message
   * @param admin The invid of the admin whose action resulted in the mail
   * @param adminName The name of the admin whose actin resulted in the mail
   * @param objects A vector of invids of objects involved in the mail
   *
   */

  public void logMail(Vector addresses, String subject, String message, 
		      Invid admin, String adminName, Vector objects)
  {
    logEvents.addElement(new DBLogEvent(addresses, subject, message, admin, adminName, objects));
  }

  /**
   *
   * This method is used to record a message to be sent out when
   * the transaction is committed.
   *
   * @param addressList Vector of Strings, the address list
   * @param subject The subject line of the message
   * @param message The body of the message
   *
   */

  public void logMail(Vector addresses, String subject, String message)
  {
    logEvents.addElement(new DBLogEvent(addresses, subject, message, null, null, null));
  }

  /**
   * <p>This method checkpoints the current transaction at its current
   * state. If need be, this transaction can later be rolled back
   * to this point by calling the rollback() method.</p>
   *
   * @param name An identifier for this checkpoint
   */

  public synchronized void checkpoint(String name)
  {
    if (debug)
      {
	System.err.println("DBEditSet.checkpoint(): checkpointing key " + name);
      }

    // checkpoint our objects

    checkpoints.push(new DBCheckPoint(name, this));

    // and our namespaces

    // we don't synchronize on dbStore, the odds are zip that a
    // namespace will be created or deleted while we are in the middle
    // of a transaction, since that is only done during schema editing
    
    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).checkpoint(this, name);
      }
  }

  /**
   * <p>This method is used to pop a checkpoint off the checkpoint
   * stack.  This method is equivalent to a rollback where the
   * checkpoint information is taken off the stack, but this
   * DBEditSet's state is not reverted.</p>
   *
   * <p>Any checkpoints that were placed on the stack after
   * the checkpoint matching &lt;name&gt; will also be
   * removed from the checkpoint stack.</p>
   *
   * @param name An identifier for the checkpoint to take off
   * the checkpoint stack.

   * @return null if the checkpoint could not be found on the
   * stack, or the DBCheckPoint object representing the state
   * of the transaction at the checkpoint time if the checkpoint
   * could be found.
   */

  public DBCheckPoint popCheckpoint(String name)
  {
    return popCheckpoint(name, false);
  }

  /**
   * <p>This method is used to pop a checkpoint off the checkpoint
   * stack.  This method is equivalent to a rollback where the
   * checkpoint information is taken off the stack, but this
   * DBEditSet's state is not reverted.</p>
   *
   * <p>Any checkpoints that were placed on the stack after
   * the checkpoint matching &lt;name&gt; will also be
   * removed from the checkpoint stack.</p>
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
   */

  public synchronized DBCheckPoint popCheckpoint(String name, boolean inRollback)
  {
    DBCheckPoint point = null;
    boolean found;

    /* -- */

    if (debug)
      {
	System.err.println("DBEditSet.popCheckpoint(): seeking to pop " + name);
      }

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
	System.err.println("\nCurrently registered checkpoints:");

	for (int i = 0; i < checkpoints.size(); i++)
	  {
	    point = (DBCheckPoint) checkpoints.elementAt(i);
	    
	    System.err.println(i + ": " + point.name);
	  }

	System.err.println();

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
	else if (debug)
	  {
	    System.err.println("DBEditSet.popCheckpoint(): popping overlaid checkpoint " + point.name);
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
   * <p>This brings this transaction back to the state it was
   * at at the time of the matching checkPoint() call.  Any
   * objects that were checked out in care of this transaction
   * since the checkPoint() will be checked back into the
   * database and made available for other transactions to
   * access.  All namespace changes made by this transaction
   * will likewise be rolled back to their state at the
   * checkpoint.</p>
   *
   * @param name An identifier for the checkpoint to be rolled back to.
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
	    // ok, we've got a new object since the checkpoint.  Ditch it.

	    obj.release(true);
	
	    switch (obj.getStatus())
	      {
	      case DBEditObject.CREATING:
	      case DBEditObject.DROPPING:
		obj.getBase().releaseId(obj.getID()); // relinquish the unused invid

		session.GSession.checkIn();
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

    // we don't synchronize on dbStore, the odds are zip that a
    // namespace will be created or deleted while we are in the middle
    // of a transaction, since that is only done during schema editing

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
   * <p>commit is used to cause all changes in association with
   * this DBEditSet to be performed.  If commit() cannot make
   * the changes for any reason, commit() will return a ReturnVal
   * indicating failure and the cause of the commit failure, and
   * will leave the transaction open for a subsequent commit() attempt,
   * or an abort().</p>
   *
   * <p>The returned ReturnVal will have doNormalProcessing set to false if
   * the transaction was completely aborted.  Both DBSession and the client
   * should take a false doNormalProcessing boolean as an indicator that
   * the transaction was simply wiped out and a new transaction should
   * be opened for subsequent activity.  A true doNormalProcessing value
   * indicates that the client can try the commit again at a later time,
   * or manually cancel.</p>
   *
   * @return a ReturnVal indicating success or failure, or null on success
   * without comment.
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

    // There should be NO WAY we can have a non-null wLock at this point.

    if (wLock != null)
      {
	throw new Error("Error! DBEditSet " + description + 
			" commit already has writeLock established!");
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

    // we don't want to have any chance of leaving commit with the
    // write lock still established.

    try
      {
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
			Vector missingFields = eObj.checkRequiredFields();
		    
			if (missingFields != null)
			  {
			    StringBuffer errorBuf = new StringBuffer();

			    errorBuf.append("Error, ");
			    errorBuf.append(eObj.getTypeName());
			    errorBuf.append(" object ");
			    errorBuf.append(eObj.getLabel());
			    errorBuf.append(" has not been completely filled out.  The following fields need ");
			    errorBuf.append("to be filled in before this transaction can be committed:\n\n");
			
			    for (int j = 0; j < missingFields.size(); j++)
			      {
				errorBuf.append((String) missingFields.elementAt(j));
				errorBuf.append("\n");
			      }

			    retVal = Ganymede.createErrorDialog("Error, required fields not filled in",
								errorBuf.toString());
			  }
			else
			  {
			    retVal = null;
			  }
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
			    eObj2.release(false); // undo committing flag
			  }

			// we are going to relinquish the write lock,
			// however.

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
			eObj2.release(false); // undo committing flag
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
		    switch (eObj.getStatus())
		      {
		      case DBEditObject.EDITING:
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
				System.err.println("**** DIFF (" + eObj.getLabel() +
						   "):" + diff + " : ENDDIFF****");
			      }
			
			    logEvent("objectchanged",
				     eObj.getTypeDesc() + ":" + eObj.getLabel() +
				     " --\n" + diff,
				     (gSession.personaInvid == null ?
				      gSession.userInvid : gSession.personaInvid),
				     gSession.username,
				     invids, null);
			  }
		    
			break;

		      case DBEditObject.CREATING:

			invids = new Vector();
			invids.addElement(eObj.getInvid());
		   
			if (debug)
			  {
			    System.err.println("Logging event for " + eObj.getLabel());
			  }

			// DBEditObject.diff() also works with newly created objects

			diff = eObj.diff();

			if (diff != null)
			  {
			    if (debug)
			      {
				System.err.println("**** DIFF (" + eObj.getLabel() +
						   "):" + diff + " : ENDDIFF****");
			      }
			
			    logEvent("objectcreated",
				     eObj.getTypeDesc() + ":" + eObj.getLabel() +
				     " --\n" + diff,
				     (gSession.personaInvid == null ?
				      gSession.userInvid : gSession.personaInvid),
				     gSession.username,
				     invids, null);
			  }

			break;

		      case DBEditObject.DELETING:

			invids = new Vector();
			invids.addElement(eObj.getInvid());
		   
			if (debug)
			  {
			    System.err.println("Logging event for " + eObj.getLabel());
			  }

			// DBEditObject.diff() does not work for deleted objects.

			logEvent("deleteobject",
				 eObj.getTypeDesc() + ":" + eObj.getLabel(),
				 (gSession.personaInvid == null ?
				  gSession.userInvid : gSession.personaInvid),
				 gSession.username,
				 invids, null);

			break;
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
					     gSession.userInvid : gSession.personaInvid),
					    this);
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
	    // so DBSession and the client should know we've done a total
	    // scrub.

	    retVal = Ganymede.createErrorDialog("Couldn't commit transaction, exception caught in DBEditSet.commit()",
						ex.getMessage());
	    return retVal;
	  }
	catch (Error ex)
	  {
	    Ganymede.debug("** Caught error while preparing transaction for commit: " + ex);
	    Ganymede.debug("** aborting transaction");
	    ex.printStackTrace();
	    releaseWriteLock("error caught while preparing trans.");
	    release();

	    // Ganymede.createErrorDialog() sets doNormalProcessing to false,
	    // so DBSession and the client should know we've done a total
	    // scrub.

	    retVal = Ganymede.createErrorDialog("Couldn't commit transaction, error caught in DBEditSet.commit()",
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

		// Create a read-only version of eObj, with all fields
		// reset to checked-in status, put it into our object hash

		base.objectTable.put(new DBObject(eObj)); 

		// (note that we can't use a no-sync put above, since
		// we don't prevent asynchronous viewDBObject().

		session.GSession.checkIn();
		base.store.checkIn(); // update checkout count

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

		base.objectTable.remove(eObj.id);

		// (note that we can't use a no-sync remove above, since
		// we don't prevent asynchronous viewDBObject().

		session.GSession.checkIn();
		base.store.checkIn(); // count it as checked in once it's deleted
		break;

	      case DBEditObject.DROPPING:

		// dropped objects had their deletion finalization done before
		// we ever got to this point.. 

		base.releaseId(eObj.getID()); // relinquish the unused invid
		session.GSession.checkIn();
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

	// we don't synchronize on dbStore, the odds are zip that a
	// namespace will be created or deleted while we are in the middle
	// of a transaction, since that is only done during schema editing
	
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
    finally
      {
	// just to be sure we don't leave a write lock hanging somehow
	// if we successfully released before, this is a no-op

	releaseWriteLock("problem in commit");
      }
  }

  /**
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
	eObj.release(true);
	
	switch (eObj.getStatus())
	  {
	  case DBEditObject.CREATING:
	  case DBEditObject.DROPPING:
	    eObj.getBase().releaseId(eObj.getID()); // relinquish the unused invid
	    session.GSession.checkIn();
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

    // we don't synchronize on dbStore, the odds are zip that a
    // namespace will be created or deleted while we are in the middle
    // of a transaction, since that is only done during schema editing

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).abort(this);
      }

    objects = null;
    session = null;
    checkpoints = new Stack();
  }

  /**
   * This is a dinky little private helper method to keep things
   * clean.  It's essential that wLock be released if things go
   * wrong, else next time this session tries to commit a transaction,
   * it'll wind up waiting forever for the old lock to be released.
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

