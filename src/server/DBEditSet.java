/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.93 $
   Last Mod Date: $Date: 2001/02/14 06:55:44 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.NamedStack;
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
   * A hashtable mapping Invids to {@link
   * arlut.csd.ganymede.DBEditObject DBEditObject}s checked out in
   * care of this transaction.  
   */

  Hashtable objects = null;

  /**
   * A list of {@link arlut.csd.ganymede.DBLogEvent DBLogEvent}'s
   * to be written to the Ganymede logfile and/or mailed out when
   * this transaction commits.
   */

  Vector logEvents = null;

  /**
   * <p>A record of the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}'s
   * touched by this transaction.  These DBObjectBase's will be locked
   * when this transaction is committed.</p>
   */

  private Hashtable basesModified;

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

  NamedStack checkpoints = new NamedStack();

  /**
   * <p>We keep track of the thread that is doing checkpointing.. once
   * a thread starts a checkpoint on this transaction, we don't allow
   * any other threads to checkpoint until the first thread releases
   * its checkpoint.  This is to prevent problems resulting from
   * interleaved checkpoint/popCheckpoint/rollback activities across
   * multiple threads.</p>
   */

  Thread currentCheckpointThread = null;

  /**
   * The writelock acquired during the course of a commit attempt.  We keep
   * this around as a DBEditSet field so that we can use the handy
   * {@link arlut.csd.ganymede.DBEditSet#releaseWriteLock(java.lang.String) releaseWriteLock()}
   * method, but wLock should really never be non-null outside of the
   * context of the commit() call.  */

  DBWriteLock wLock = null;

  /**
   * <p>True if this DBEditSet is operating in interactive mode.</p>
   */

  private boolean interactive;

  /**
   * <p>True if this DBEditSet is operating in non-interactive mode
   * and a rollback was ordered.  In such cases, the server skipped
   * doing the checkpoint and so has no choice but to condemn the
   * whole transaction.</p>
   */

  private boolean mustAbort = false;

  /* -- */

  /**
   * Constructor for DBEditSet
   *
   * @param dbStore The owning DBStore object.
   * @param session The DBStore session owning this transaction.
   * @param description An optional string to identify this transaction
   * @param interactive If false, this transaction will operate in
   * non-interactive mode.  Certain Invid operations will be optimized
   * to avoid doing choice list queries and bind checkpoint
   * operations.  When a transaction is operating in non-interactive mode,
   * any failure that cannot be handled cleanly due to the optimizations will
   * result in the transaction refusing to commit when commitTransaction()
   * is attempted.  This mode is intended for batch operations.
   *
   */

  public DBEditSet(DBStore dbStore, DBSession session, String description,
		   boolean interactive)
  {
    this.session = session;
    this.dbStore = dbStore;
    this.description = description;
    this.interactive = interactive;
    objects = new Hashtable();
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

  public boolean isInteractive()
  {
    return interactive;
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

  public DBEditObject findObject(Invid invid)
  {
    return (DBEditObject) objects.get(invid);
  }

  /**
   * <p>Method to associate a DBEditObject with this transaction.</p>
   *
   * <p>This method is called by the createDBObject and editDBObject
   * methods in {@link arlut.csd.ganymede.DBSession DBSession}.</p>
   *
   * @param object The newly created DBEditObject.
   */

  public synchronized boolean addObject(DBEditObject object)
  {
    if (false)
      {
	System.err.println("DBEditSet adding " + object.getTypeName() + " " + object.getLabel());
      }

    // remember that we are not allowing objects that this object is
    // pointing to via an asymmetric link to be deleted.. make sure
    // that no one has already deleted an object we're pointing to, or
    // else we can't check this object out

    if (!DBDeletionManager.addSessionInvids(session, object.getASymmetricTargets()))
      {
	return false;
      }

    if (!objects.containsKey(object.getInvid()))
      {
	objects.put(object.getInvid(), object);

	// just need something to mark the slot in the hash table, to
	// indicate that this object's base is involved in the
	// transaction.

	basesModified.put(object.objectBase, this);
      }

    return true;
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
   * <p>Once a thread checkpoints a transaction, no other thread
   * can checkpoint a transaction until some thread clears the
   * checkpoint, either by doing a rollback() or a popCheckpoint().
   * checkpoint() will block any threads that try to establish a checkpoint()
   * until the prior thread's checkpoint is resolved.</p>
   *
   * <p>See DBSession.deleteDBObject() and DBSesssion.createDBObject()
   * for instances of this.</p>
   *
   * @param name An identifier for this checkpoint
   */

  public synchronized void checkpoint(String name)
  {
    if (debug)
      {
	System.err.println("DBEditSet.checkpoint(): checkpointing key " + name);
      }

    if (!interactive)
      {
	return;
      }

    Thread thisThread = java.lang.Thread.currentThread();

    int waitcount = 0;

    while (currentCheckpointThread != null && currentCheckpointThread != thisThread)
      {
	System.err.println("DBEditSet.checkpoint(\"" + name + "\") waiting for prior thread " +
			   currentCheckpointThread.toString() +
			   " to finish with prior checkpoint");
	try
	  {
	    wait(1000);		// only wait a second at a time, so we'll get lots of printlns if we get stuck
	  }
	catch (InterruptedException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }

	waitcount++;

	if (waitcount > 60)
	  {
	    System.err.println("DBEditSet.checkpoint(\"" + name + "\") has waited to checkpoint for 60 seconds");
	    System.err.println("DBEditSet.checkpoint(\"" + name + "\") giving up to avoid deadlock");

	    System.err.println("DBEditSet.checkpoint(\"" + name + "\") stack trace:");
	    thisThread.dumpStack();
	    
	    if (currentCheckpointThread.isAlive())
	      {
		System.err.println("DBEditSet.checkpoint(\"" + name + "\") printing blocking thread stack trace:");
		currentCheckpointThread.dumpStack();
	      }

	    throw new RuntimeException("DBEditSet.checkpoint(\"" + name + "\") timed out");
	  }
      }

    if (waitcount > 0)
      {
	System.err.println("DBEditSet.checkpoint(\"" + name + "\") proceeding");
      }

    // if we slept until the transaction was committed or aborted, oops

    if (session == null)
      {
	throw new RuntimeException("DBEditSet.checkpoint(" + name + ") slept until transaction committed/cleared");
      }

    currentCheckpointThread = thisThread;

    // checkpoint our objects, logEvents, and deletion locks

    checkpoints.push(name, new DBCheckPoint(this));

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
   *
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

    /* -- */

    if (debug)
      {
	System.err.println("DBEditSet.popCheckpoint(): seeking to pop " + name);
      }

    // if we're not an interactive transaction, we disregard all checkpoints

    if (!interactive)
      {
	return null;
      }

    // see if we can find the checkpoint

    point = (DBCheckPoint) checkpoints.pop(name);

    if (point == null)
      {
	System.err.println("DBEditSet.popCheckpoint: couldn't find checkpoint for " + name);
	System.err.println("\nCurrently registered checkpoints:");

	System.err.println(checkpoints.toString());

	return null;
      }

    // DBEditSet.rollback() calls us to take care of getting our
    // transactional checkpoint in the rollback case, but it doesn't
    // want us to pop the namespace checkpoint for it, since it will
    // want to do a rollback on the namespace checkpoint instead.

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

    // if we've cleared the last checkpoint stacked, wake up any
    // threads that are blocking to create new checkpoints

    if (checkpoints.empty())
      {
	currentCheckpointThread = null;
	this.notifyAll();
      }

    return point;
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

    /* -- */

    if (debug)
      {
	System.err.println("DBEditSet.rollback(): rollback key " + name);
      }

    if (!interactive)
      {
	// oops, we're non-interactive and we didn't actually do the
	// checkpoint we're being asked to go back to.. set the
	// mustAbort flag so the transaction will never commit

	this.mustAbort = true;

	try
	  {
	    throw new RuntimeException("rollback called in non-interactive transaction");
	  }
	catch (RuntimeException ex)
	  {
	    ex.printStackTrace();
	  }

	return false;
      }

    point = popCheckpoint(name, true); // this may wake up blocking checkpointers

    if (point == null)
      {
	return false;
      }

    // rollback our mail/log events

    logEvents = point.logEvents;

    // restore the noDeleteLocks we had

    DBDeletionManager.revertSessionCheckpoint(session, point.invidDeleteLocks);

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

	    System.err.println("Looking for object " + objck.invid.toString() + " in database");
	  }
	
	obj = findObject(objck.invid);

	if (obj != null)
	  {
	    if (debug)
	      {
		System.err.println("Found object " + obj.toString() + ", rolling back fields");
	      }

	    obj.rollback(objck.fields);

	    if (debug)
	      {
		System.err.println("Found object " + obj.toString() + ", rolling back status ");
		System.err.println(obj.status + " to old status " + objck.status);
	      }

	    obj.status = objck.status;
	  }
	else
	  {
	    // huh?  this shouldn't ever happen, unless maybe we have a rollback order
	    // error or something.  Complain.

	    throw new RuntimeException("DBEditSet.rollback error.. we lost checked out objects in midstream?");
	  }
      }

    if (debug)
      {
	System.err.println("DBEditSet.rollback() At checkpoint:");

	for (int i = 0; i < point.objects.size(); i++)
	  {
	    System.err.println(point.objects.elementAt(i));
	  }

	System.err.println("\nDBEditSet.rollback() Now:");

	Enumeration enum = objects.elements();

	while (enum.hasMoreElements())
	  {
	    System.err.println(enum.nextElement());
	  }
      }

    // now, we have to sweep out any objects that are in the transaction now
    // that weren't in the transaction at the checkpoint.

    // note that we need a drop temp vector because it confuses things
    // if we remove elements from objects while we are iterating over it

    // calculate what DBEditObjects we have in the transaction at the
    // present time that we didn't have in the checkpoint

    Vector drop = new Vector();

    Hashtable oldvalues = new Hashtable();

    for (int i = 0; i < point.objects.size(); i++)
      {
	Invid chkinvid = ((DBCheckPointObj) point.objects.elementAt(i)).invid;

	oldvalues.put(chkinvid, chkinvid);
      }

    Enumeration enum = objects.elements();

    while (enum.hasMoreElements())
      {
	DBEditObject eobjRef = (DBEditObject) enum.nextElement();

	Invid tmpvid = eobjRef.getInvid();
	
	if (!oldvalues.containsKey(tmpvid))
	  {
	    drop.addElement(eobjRef);
	  }
      }

    // and now we get rid of DBEditObjects we need to drop

    for (int i = 0; i < drop.size(); i++)
      {
	// ok, we've got a new object since the checkpoint.  Ditch it.

	obj = (DBEditObject) drop.elementAt(i);

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
	    System.err.println("DBEditSet.rollback(): dropping object " + obj.getLabel() + " (" +
			       obj.getInvid().toString() + ")");
	  }
      }

    // now go ahead and clean out the dropped objects

    for (int i = 0; i < drop.size(); i++)
      {
	objects.remove(((DBEditObject) drop.elementAt(i)).getInvid());
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
   * <p>The returned {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * will have doNormalProcessing set to false if the transaction was
   * completely aborted.  Both {@link arlut.csd.ganymede.DBSession
   * DBSession} and the client should take a false doNormalProcessing
   * boolean as an indicator that the transaction was simply wiped out
   * and a new transaction should be opened for subsequent activity.
   * A true doNormalProcessing value indicates that the client can try
   * the commit again at a later time, or manually cancel.</p>
   *
   * @return a ReturnVal indicating success or failure, or null on success
   * without comment.  
   */

  public synchronized ReturnVal commit()
  {
    Vector baseSet = new Vector();
    Vector committedObjects = new Vector();
    Enumeration enum;
    DBObjectBase base;
    DBEditObject eObj, eObj2;
    ReturnVal retVal = null;

    Date modDate = new Date();

    /* -- */

    if (debug)
      {
	System.err.println(session.key + ": DBEditSet.commit(): entering");
      }

    if (mustAbort)
      {
	release();
	return Ganymede.createErrorDialog("Forced Transaction Abort",
					  "The server ran into a non-reversible error while processing this " +
					  "transaction and forced an abort.");
      }

    if (objects == null)
      {
	throw new RuntimeException("already committed or released");
      }

    // determine what bases we need to lock to do this commit

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

    // Create the lock on the bases changed and establish.  This
    // openWriteLock() call will cause our thread to block until we
    // can get all the bases we need locked.

    try
      {
	wLock = session.openWriteLock(baseSet);	// wait for write lock
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug("DBEditSet.commit(): lock aborted, commit failed, releasing transaction for " + 
		       session.key);

	// the following test shouldn't be necessary, but just in case

	if (wLock != null)
	  {
	    session.releaseLock(wLock);
	    wLock = null;
	  }

	return Ganymede.createErrorDialog("Commit failure",
					  "Couldn't commit transaction, our write lock was " +
					  "denied.. server going down?");
      }

    // we don't want to have any chance of leaving commit with the
    // write lock still established.

    try
      {
	if (debug)
	  {
	    System.err.println(session.key +
			       ": DBEditSet.commit(): established write lock");
	  }

	if (session.getGSession() != null && session.getGSession().enableOversight)
	  {
	    // Check all the objects that we have checked out to make
	    // sure their commit logic approves the transaction.

	    enum = objects.elements();

	    while (enum.hasMoreElements())
	      {
		eObj = (DBEditObject) enum.nextElement();

		if (debug)
		  {
		    System.err.println("DBEditSet.commit(): checking object " + eObj.toString());
		  }

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

			// we're not going to clear out the transaction, 
			// as there is a chance the user can fix things
			// up.

			// we are going to relinquish the write lock,
			// however.

			releaseWriteLock("transaction commit rejected in phase 1 for missing fields");

			// let DBSession/the client know they can retry things.
			
			retVal.doNormalProcessing = true;
			return retVal;
		      }
		  }
	      }
	  }

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
	    // Ok, we now need to go through the objects that are being
	    // changed by this transaction and check to see if we
	    // have an ok to commit them.
	    
	    enum = objects.elements();

	    while (enum.hasMoreElements())
	      {
		eObj = (DBEditObject) enum.nextElement();

		// and try to commit this object

		retVal = eObj.commitPhase1();

		if (retVal != null && !retVal.didSucceed())
		  {
		    // we're not going to clear out the transaction, 
		    // as there is a chance the user can fix things
		    // up.  We need to clear the committing flag on
		    // all objects that we've called commitPhase1()
		    // on so that they will accept future changes.

		    // Note.. we really do want to give the user a
		    // chance to fix things up here.. if the end-user
		    // requested a transaction commit without the
		    // possibility of retry, that is handled in
		    // GanymedeSession.commitTransaction().

		    // We must allow retry here.

		    for (int i = 0; i < committedObjects.size(); i++)
		      {
			eObj2 = (DBEditObject) committedObjects.elementAt(i);
			eObj2.release(false); // undo committing flag
		      }

		    releaseWriteLock("transaction commit rejected in phase 1");

		    // let DBSession/the client know they can retry things.

		    retVal.doNormalProcessing = true;
		    return retVal;
		  }
		else
		  {
		    committedObjects.addElement(eObj);
		  }
	      }

	    // phase one complete, go ahead and have all our objects
	    // do their 2nd level (external) commit processes

	    // before we call commitPhase2() on the objects in our
	    // working set, we'll set the historical fields, which
	    // specify the time of creation/last modification and the
	    // identity of the user who created/modified the object
	    
	    DateDBField df;
	    StringDBField sf;
	    String result = session.getID() + " [" + description + "]";

	    // intern the result string so that we don't have multiple
	    // copies of common strings in our heap

	    result = result.intern();

	    for (int i = 0; i < committedObjects.size(); i++)
	      {
		eObj = (DBEditObject) committedObjects.elementAt(i);

		// force a change of date and modifier information
		// into the object without using the normal field
		// modification methods.. this lets us set field
		// values at a time when the object would reject
		// changes from the user because the committing flag
		// is set.

		switch (eObj.getStatus())
		  {
		  case DBEditObject.CREATING:
		    
		    if (!eObj.isEmbedded())
		      {
			df = (DateDBField) eObj.getField(SchemaConstants.CreationDateField);
			df.value = modDate;
			
			sf = (StringDBField) eObj.getField(SchemaConstants.CreatorField);
			sf.value = result;
		      }

		    // * fall-through *

		  case DBEditObject.EDITING:
		    
		    if (!eObj.isEmbedded())
		      {
			df = (DateDBField) eObj.getField(SchemaConstants.ModificationDateField);
			df.value = modDate;

			sf = (StringDBField) eObj.getField(SchemaConstants.ModifierField);
			sf.value = result;
		      }
		    else
		      {
			// not sure why this test is here.. was I
			// going to do something more with this?

			InvidDBField invf = (InvidDBField) eObj.getField(SchemaConstants.ContainerField);

			if (invf == null || !invf.isDefined())
			  {
			    Ganymede.debug("DBEditSet.commit(): WARNING, an embedded object's " +
					   "container link is undefined or null-valued.(?!)");
			  }
		      }
		  }

		// tell the object to go ahead and do any external
		// commit actions.

		try
		  {
		    eObj.commitPhase2();
		  }
		catch (RuntimeException ex)
		  {
		    // if we get a runtime exception here, we want to
		    // go ahead and commit the rest of the objects,
		    // since we may already have done external actions
		    // through other objects that have already had
		    // their commitPhase2() methods called

		    // just show the trace, don't throw it up

		    ex.printStackTrace();
		  }
	      }

	    // need to clear out any transients before we write the
	    // transaction out to disk and do logging and such.. we do
	    // this in a separate loop from the above so that we won't
	    // chance throwing an exception that would prevent all
	    // objects in the transaction from having commitPhase2()
	    // called

	    GanymedeSession gSession = session.getGSession();
	    Vector invids;

	    for (int i = 0; i < committedObjects.size(); i++)
	      {
		eObj = (DBEditObject) committedObjects.elementAt(i);

		// this is where we used to call clearTransientFields
		// on eObj, but we don't do that anymore since everything
		// here and below should check the defined() method itself
		// to skip fields that have no useful information in them

		// we'll want to log the before/after state of any objects
		// edited by this transaction

		if (Ganymede.log != null)
		  {
		    switch (eObj.getStatus())
		      {
		      case DBEditObject.EDITING:

			invids = new Vector();
			invids.addElement(eObj.getInvid());

			// if we changed an embedded object, make a
			// note that the containing object was
			// involved so that we show changes to
			// embedded objects in a log search for the
			// containing object

			if (eObj.isEmbedded())
			  {
			    DBObject container = gSession.getContainingObj(eObj);
			    
			    if (container != null)
			      {
				invids.addElement(container.getInvid());
			      }
			  }
		   
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
				     eObj.getTypeDesc() + " " + eObj.getLabel() +
				     ", <" +  eObj.getInvid() + "> was modified.\n\n" +
				     diff,
				     (gSession.personaInvid == null ?
				      gSession.userInvid : gSession.personaInvid),
				     gSession.username,
				     invids, eObj.getEmailTargets());
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
				     eObj.getTypeDesc() + " " + eObj.getLabel() +
				     ", <" +  eObj.getInvid() + "> was created.\n\n" +
				     diff,
				     (gSession.personaInvid == null ?
				      gSession.userInvid : gSession.personaInvid),
				     gSession.username,
				     invids, eObj.getEmailTargets());
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
				 eObj.getTypeDesc() + " " + eObj.getLabel() + ", <" + 
				 eObj.getInvid() + "> was deleted.\n\n",
				 (gSession.personaInvid == null ?
				  gSession.userInvid : gSession.personaInvid),
				 gSession.username,
				 invids, eObj.getEmailTargets());

			break;
		      }
		  }
	      }

	    // the logging was successful, now write this transaction
	    // out to the Journal

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
		
		    return Ganymede.createErrorDialog("Couldn't commit transaction, couldn't write " +
						      "transaction to disk",
						      "Couldn't commit transaction, the server may " +
						      "have run out of" +
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

		return Ganymede.createErrorDialog("Couldn't commit transaction, IOException caught " + 
						  "writing journal",
						  "Couldn't commit transaction, the server may have " + 
						  "run out of" +
						  " disk space.");
	      }

	    if (debug)
	      {
		System.err.println(session.key + ": DBEditSet.commit(): transaction written to disk");
	      }

	    // We've written the journal for this transaction out, go
	    // ahead and log it.. note that we may mail out events
	    // created by the commitPhase2() methods called above.

	    if (Ganymede.log != null)
	      {
		Ganymede.log.logTransaction(logEvents, gSession.username, 
					    (gSession.personaInvid == null ?
					     gSession.userInvid : gSession.personaInvid),
					    this);
	      }

	    // for garbage collection

	    logEvents.removeAllElements();
	    logEvents = null;
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

	    return Ganymede.createErrorDialog("Couldn't commit transaction, exception " +
					      "caught in DBEditSet.commit()",
					      Ganymede.stackTrace(ex));
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

	    return Ganymede.createErrorDialog("Couldn't commit transaction, error " +
					      "caught in DBEditSet.commit()",
					      Ganymede.stackTrace(ex));
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

	for (int i = 0; i < committedObjects.size(); i++)
	  {
	    eObj = (DBEditObject) committedObjects.elementAt(i);

	    base = eObj.getBase();

	    // Create a new DBObject from our DBEditObject and insert
	    // into the object hash

	    switch (eObj.getStatus())
	      {
	      case DBEditObject.CREATING:
	      case DBEditObject.EDITING:

		// we need to update DBStore.backPointers to take into account
		// the changes made to this object.

		syncObjBackPointers(eObj);

		// Create a read-only version of eObj, with all fields
		// reset to checked-in status, put it into our object hash

		// note that this new DBObject will not include any
		// transient fields which self-identify as undefined

		base.objectTable.put(new DBObject(eObj));

		// (note that we can't use a no-sync put above, since
		// we don't prevent asynchronous viewDBObject().

		session.GSession.checkIn();
		base.store.checkIn(); // update checkout count

		break;

	      case DBEditObject.DELETING:

		// we need to update DBStore.backPointers to take into account
		// the changes made to this object.

		syncObjBackPointers(eObj);

		// Deleted objects had their deletion finalization done before
		// we ever got to this point.  

		// Note that we don't try to release the id for previously
		// registered objects.. the base.releaseId() method really
		// can only handle popping object id's off of a stack, and
		// can't do anything for object id's unless the id was the
		// last one allocated in that base, which is unlikely
		// enough that we don't worry about it here.

		base.objectTable.remove(eObj.getID());

		// (note that we can't use a no-sync remove above, since
		// we don't prevent asynchronous viewDBObject().

		session.GSession.checkIn();
		base.store.checkIn(); // count it as checked in once it's deleted
		break;

	      case DBEditObject.DROPPING:

		// don't need to update backpointers, since this object was
		// created and destroyed within this transaction

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

	// very last thing before we release our write lock.. touch
	// all the bases' time stamps.. we do this as late as possible
	// to minimize the chance that a builder task run in response
	// to a previous commit records its lastRunTime after we, the
	// next transaction committed, gets our timestamps updated.

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);

	    if (debug)
	      {
		Ganymede.debug("DBEditSet.commit(): Touching " + base + "'s timestamp");
	      }

	    base.updateTimeStamp();

	    // and, very important, update the base's snapshot vector
	    // so that any new queries that are issued will proceed
	    // against the new state of objects in this base

	    if (debug)
	      {
		Ganymede.debug("DBEditSet.commit(): Updating " + base + "'s iteration set");
	      }

	    base.updateIterationSet();
	  }

	releaseWriteLock("successful commit");

	if (debug)
	  {
	    System.err.println(session.key + ": DBEditSet.commit(): released write lock");
	  }

	// And now it's just clean up time.  First we remove any
	// delete locks that we had asserted during this
	// transaction.. this will allow other sessions/threads to try
	// to delete objects that we had pointed to asymmetrically

	DBDeletionManager.releaseSession(session);

	// null stuff out to speed GC

	objects.clear();
	objects = null;

	committedObjects.removeAllElements();

	session = null;

	// clear out any checkpoints that may be lingering

	checkpoints.removeAllElements();

	// and wake up any threads sleeping to checkpoint

	this.currentCheckpointThread = null;
	this.notifyAll();

	// reset the basesModified hash

	basesModified.clear();
	basesModified = null;

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
   * <p>This method is executed towards the end of a transaction commit,
   * and compares the current state of this object with its original state,
   * and makes the appropriate changes to the
   * {@link arlut.csd.ganymede.DBStore#backPointers backPointers} hash in
   * the server's {@link arlut.csd.ganymede.DBStore DBStore} object.</p>
   *
   * <p>The purpose of this is to support the decoupling of an object
   * from its backlinks, so that objects can be asymmetrically linked
   * to an object without having to check that object out for editing.</p>
   */

  private void syncObjBackPointers(DBEditObject obj)
  {
    Vector oldBackPointers;
    Vector newBackPointers;
    Vector removedBackPointers;
    Vector addedBackPointers;

    Invid target;
    Invid ourInvid = obj.getInvid();
    Invid testInvid;

    Hashtable reverseLinks;

    DBObject original;

    /* -- */

    synchronized (Ganymede.db.backPointers)
      {
	original = obj.getOriginal();

	if (original == null)
	  {
	    oldBackPointers = null;
	  }
	else
	  {
	    oldBackPointers = original.getASymmetricTargets();
	  }

	newBackPointers = obj.getASymmetricTargets();

	removedBackPointers = VectorUtils.difference(oldBackPointers, newBackPointers);
	addedBackPointers = VectorUtils.difference(newBackPointers, oldBackPointers);
	
	for (int i = 0; i < removedBackPointers.size(); i++)
	  {
	    target = (Invid) removedBackPointers.elementAt(i);

	    reverseLinks = (Hashtable) Ganymede.db.backPointers.get(target);

	    if (reverseLinks == null)
	      {
		// error.. it should be there so we can remove it

		System.err.println("DBEditObject.syncObjBackPointers(): missing reverseLinks found removing a backlink: " +
				   target);
		continue;
	      }

	    testInvid = (Invid) reverseLinks.remove(ourInvid);

	    if (testInvid == null || !testInvid.equals(ourInvid))
	      {
		System.err.println("DBEditObject.syncObjBackPointers(): couldn't find and remove proper backlink for: " +
				   target);
	      }

	    // if that was the last back-link, pull the second level hash out

	    // this is safe only because we are using synchronization
	    // on Ganymede.db.backPointers to restrict modifications
	    // to the backPointers structure to a thread at a time

	    if (reverseLinks.size() == 0)
	      {
		Ganymede.db.backPointers.remove(target);
	      }
	  }

	for (int i = 0; i < addedBackPointers.size(); i++)
	  {
	    target = (Invid) addedBackPointers.elementAt(i);

	    reverseLinks = (Hashtable) Ganymede.db.backPointers.get(target);

	    if (reverseLinks == null)
	      {
		reverseLinks = new Hashtable();
		Ganymede.db.backPointers.put(target, reverseLinks);
	      }

	    reverseLinks.put(ourInvid, ourInvid);
	  }
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

    if (debug)
      {
	System.err.println("DBEditSet.release()");
      }

    if (objects == null)
      {
	throw new RuntimeException("already committed or released");
      }

    Enumeration enum = objects.elements();

    while (enum.hasMoreElements())
      {
	eObj = (DBEditObject) enum.nextElement();
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
      }

    // undo all namespace modifications associated with this editset

    // we don't synchronize on dbStore, the odds are zip that a
    // namespace will be created or deleted while we are in the middle
    // of a transaction, since that is only done during schema editing

    for (int i = 0; i < dbStore.nameSpaces.size(); i++)
      {
	((DBNameSpace) dbStore.nameSpaces.elementAt(i)).abort(this);
      }

    // release any deletion locks we have asserted

    DBDeletionManager.releaseSession(session);

    // and help out garbage collection some

    objects.clear();
    objects = null;

    logEvents.removeAllElements();
    logEvents = null;

    basesModified.clear();
    basesModified = null;

    dbStore = null;
    session = null;

    description = null;

    checkpoints.removeAllElements();
    checkpoints = null;

    currentCheckpointThread = null;

    // wake up any sleepy heads

    this.notifyAll();
  }

  /**
   * <p>This is a dinky little private helper method to keep things
   * clean.  It's essential that wLock be released if things go
   * wrong, else next time this session tries to commit a transaction,
   * it'll wind up waiting forever for the old lock to be released.</p>
   */

  private void releaseWriteLock(String reason)
  {
    if (wLock != null)
      {
	session.releaseLock(wLock);
	wLock = null;
	
	if (debug)
	  {
	    System.err.println(session.key + ": DBEditSet.commit(): released write lock: " + reason);
	  }
      }
  }
}

