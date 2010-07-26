/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
   The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import arlut.csd.Util.NamedStack;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.scheduleHandle;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBEditSet

------------------------------------------------------------------------------*/

/**
 * <p>DBEditSet is the basic transactional unit.  All changes to the
 * database during normal operations are made in the context of a
 * DBEditSet, which may then be committed or rolled back as an atomic
 * operation.  Each {@link arlut.csd.ganymede.server.DBSession DBSession} will
 * have at most one DBEditSet transaction object active at any time.</p>
 *
 * <p>A DBEditSet tracks several things for the server, including instances of
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s that were created
 * or checked-out from the {@link arlut.csd.ganymede.server.DBStore DBStore},
 * {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace} values that were reserved
 * during the course of the transaction, and {@link arlut.csd.ganymede.server.DBLogEvent DBLogEvent}
 * objects to be recorded in the {@link arlut.csd.ganymede.server.DBLog DBLog} and/or
 * mailed out to various interested parties when the transaction is committed.</p>
 *
 * <p>DBEditSet's transaction logic is based on a two-phase commit
 * protocol, where all DBEditObject's involved in the transaction are
 * given an initial opportunity to approve or reject the transaction's
 * commit before the DBEditSet commit method goes back and 'locks-in'
 * the changes.  DBEditObjects are able to initiate changes external
 * to the Ganymede database in their
 * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase2() commitPhase2()}
 * methods, if needed.</p>
 *
 * <p>When a DBEditSet is committed, a {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}
 * is established on all {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}'s
 * involved in the transaction.  All objects checked out by that transaction
 * are then updated in the DBStore, and a summary of changes is recorded to the
 * DBStore {@link arlut.csd.ganymede.server.DBJournal DBJournal}.  The database as
 * a whole will not be dumped to disk unless and until the
 * {@link arlut.csd.ganymede.server.dumpTask dumpTask} is run, or until the server
 * undergoes a formal shutdown.</p>
 *
 * <p>Typically, the {@link arlut.csd.ganymede.server.DBEditSet#commit() commit()}
 * method is called by the
 * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction(boolean) GanymedeSession.commitTransaction()}
 * method, which will induce the server to schedule any commit-time build
 * tasks registered with the {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.</p>
 *
 * <p>If a DBEditSet commit() operation fails catastrophically, or if
 * {@link arlut.csd.ganymede.server.DBEditSet#abort() abort()} is called,
 * all DBEditObjects created or checked out during the course of the
 * transaction will be discarded, all DBNameSpace values allocated will
 * be relinquished, and any logging information for the abandoned transaction
 * will be forgotten.</p>
 *
 * <p>As if all that wasn't enough, the DBEditSet class also maintains a stack
 * of {@link arlut.csd.ganymede.server.DBCheckPoint DBCheckPoint} objects to enable
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

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBEditSet");

  /**
   * <p>Maps Invids to {@link arlut.csd.ganymede.server.DBEditObject
   * DBEditObject}s checked out in care of this transaction.</p>
   */

  private Map<Invid, DBEditObject> objects = null;

  /**
   * <p>A List of {@link arlut.csd.ganymede.server.DBLogEvent DBLogEvent}'s
   * to be written to the Ganymede logfile and/or mailed out when
   * this transaction commits.</p>
   */

  private List<DBLogEvent> logEvents = null;

  /**
   * <p>A record of the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}'s
   * touched by this transaction.  These DBObjectBase's will be locked
   * when this transaction is committed.</p>
   */

  private Set<DBObjectBase> basesModified = null;

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
   * Will be true if we are running this transaction on behalf of an
   * XML client and the server was started with the -historyOverride
   * command line parameter.
   */

  private boolean allowXMLHistoryOverride = false;

  /**
   * A stack of {@link arlut.csd.ganymede.server.DBCheckPoint DBCheckPoint} objects
   * to keep track of check points performed during the course of this transaction.
   */

  private NamedStack checkpoints = new NamedStack();

  /**
   * <p>The writelock acquired during the course of a commit attempt.  We keep
   * this around as a DBEditSet field so that we can use the handy
   * {@link arlut.csd.ganymede.server.DBEditSet#releaseWriteLock() releaseWriteLock()}
   * method, but wLock should really never be non-null outside of the
   * context of the commit() call.</p>
   *
   * <p>As long as the writeLock is held, no other transactions can
   * proceed into commit on the same object bases.</p>
   */

  private DBWriteLock wLock = null;

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

  /**
   * <p>This DBJournalTransaction is used to remember information
   * about a transaction that we have persisted to the on-disk
   * journal, but which we have not yet finalized.</p>
   */

  private DBJournalTransaction persistedTransaction = null;

  /**
   * A comment to attach to logging and email generated in response to
   * this transaction.
   */

  private String comment = null;

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
    objects = Collections.synchronizedMap(new HashMap<Invid, DBEditObject>());
    logEvents = Collections.synchronizedList(new ArrayList<DBLogEvent>());
    basesModified = new HashSet(dbStore.objectBases.size());

    if (session.GSession != null && session.GSession.xSession != null && Ganymede.allowMagicImport)
      {
	this.allowXMLHistoryOverride = true;
      }
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
   * <p>Returns the user-level GanymedeSession object associated with this
   * transaction, or null if there is no GanymedeSession associated.</p>
   */

  public final GanymedeSession getGSession()
  {
    if (this.session == null)
      {
	return null;
      }

    return session.getGSession();
  }

  /**
   * <p>Returns the descriptive string passed to us when this
   * transaction was opened.</p>
   */

  public final String getDescription()
  {
    return this.description;
  }

  /**
   * <p>Returns the name of the GanymedeSession user who created this
   * transaction.</p>
   *
   * <p>If this transaction was created by an admin persona, we will
   * return that persona name, otherwise we'll return the user
   * name.</p>
   */

  public final String getUsername()
  {
    GanymedeSession gSession = this.getGSession();

    if (gSession == null)
      {
	return null;
      }

    return gSession.getPersonaLabel();
  }

  /**
   * <p>Returns true if the GanymedeSession associated with this
   * transaction has oversight turned on.</p>
   */

  public final boolean isOversightOn()
  {
    return (getGSession() != null && getGSession().enableOversight);
  }

  /**
   * <p>This method returns true if this transaction is being carried
   * out by an interactive client.</p>
   */

  public boolean isInteractive()
  {
    return interactive;
  }

  /**
   * <p>To allow the GanymedeSession to get a copy of our object hash.</p>
   */

  public Map<Invid, DBEditObject> getObjectHashClone()
  {
    synchronized (objects)
      {
	return new HashMap<Invid,DBEditObject>(objects);
      }
  }

  /**
   * <p>Return a list of objects that we are currently working on.</p>
   */

  public DBEditObject[] getObjectList()
  {
    return objects.values().toArray(new DBEditObject[0]);
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
    return objects.get(invid);
  }

  /**
   * <p>This method returns true if the invid parameter
   * has been checked out for editing by this transaction.</p>
   */

  public boolean isEditingObject(Invid invid)
  {
    return objects.containsKey(invid);
  }

  /**
   * <p>Method to associate a DBEditObject with this transaction.</p>
   *
   * <p>This method is called by the createDBObject and editDBObject
   * methods in {@link arlut.csd.ganymede.server.DBSession DBSession}.</p>
   *
   * @param object The newly created DBEditObject.
   */

  public synchronized boolean addObject(DBEditObject object)
  {
    // if this transaction is in the middle of commit(), don't let the
    // programmer try to corrupt the transaction by adding new objects
    // to this transaction.  Gaurav, this means you. ;-)

    if (wLock != null)
      {
	throw new RuntimeException(ts.l("addObject.cant_add"));
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

	basesModified.add(object.objectBase);
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
   * @param objects A List of invids of objects involved in this event.
   * @param notifyList A List of Strings listing email addresses to send notification
   * of this event to.
   */

  public void logEvent(String eventClassToken, String description,
		       Invid admin, String adminName,
		       List<Invid> objects, List<String> notifyList)
  {
    DBLogEvent event = new DBLogEvent(eventClassToken, description,
				      admin, adminName,
				      objects, notifyList);
    logEvents.add(event);
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
    logEvents.add(event);
  }

  /**
   * <p>This method is used to record a message to be sent out when
   * the transaction is committed.</p>
   *
   * @param addresses Vector of Strings, the address list
   * @param subject The subject line of the message
   * @param message The body of the message
   * @param admin The invid of the admin whose action resulted in the mail
   * @param adminName The name of the admin whose actin resulted in the mail
   * @param objects A vector of invids of objects involved in the mail
   */

  public void logMail(Collection<String> addresses, String subject, String message,
		      Invid admin, String adminName, Vector objects)
  {
    logEvents.add(new DBLogEvent(addresses, subject, message, admin, adminName, objects));
  }

  /**
   * <p>This method is used to record a message to be sent out when
   * the transaction is committed.</p>
   *
   * @param addresses Vector of Strings, the address list
   * @param subject The subject line of the message
   * @param message The body of the message
   */

  public void logMail(Collection<String> addresses, String subject, String message)
  {
    logEvents.add(new DBLogEvent(addresses, subject, message, null, null, null));
  }

  /**
   * This method is used to record a message to be sent out when
   * the transaction is committed.
   *
   * @param toAddress The email address to send this message to.
   * @param subject The subject line of the message
   * @param message The body of the message
   */

  public void logMail(String toAddress, String subject, String message)
  {
    Vector addresses = new Vector();

    addresses.addElement(toAddress);

    logEvents.add(new DBLogEvent(addresses, subject, message, null, null, null));
  }

  /**
   * <p>This method is used to transmit a log event during successful
   * transaction commit.  The provided log event is recorded in the
   * Ganymede log file and mail notification is sent out if
   * appropriate.</p>
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

  private void streamLogEvent(String eventClassToken, String description,
			      Invid admin, String adminName,
			      List<Invid> objects, List<String> notifyList)
  {
    DBLogEvent event = new DBLogEvent(eventClassToken, description,
				      admin, adminName,
				      objects, notifyList);

    Ganymede.log.streamEvent(event, this);
  }

  /**
   * <p>This method is used to transmit a log event during successful
   * transaction commit.  The provided log event is recorded in the
   * Ganymede log file and mail notification is sent out if
   * appropriate.</p>
   *
   * @param event A pre-formed log event to register with this transaction.
   */

  private void streamLogEvent(DBLogEvent event)
  {
    Ganymede.log.streamEvent(event, this);
  }

  /**
   * <p>This method is used to transmit a message during transaction commit.</p>
   *
   * @param addresses Vector of Strings, the address list
   * @param subject The subject line of the message
   * @param message The body of the message
   * @param admin The invid of the admin whose action resulted in the mail
   * @param adminName The name of the admin whose actin resulted in the mail
   * @param objects A vector of invids of objects involved in the mail
   */

  private void streamLogMail(Vector addresses, String subject, String message,
			     Invid admin, String adminName, Vector objects)
  {
    Ganymede.log.streamEvent(new DBLogEvent(addresses, subject, message, admin, adminName, objects), this);
  }

  /**
   * <p>This method is used to transmit a message during transaction commit.</p>
   *
   * @param addresses Vector of Strings, the address list
   * @param subject The subject line of the message
   * @param message The body of the message
   */

  private void streamLogMail(Vector addresses, String subject, String message)
  {
    Ganymede.log.streamEvent(new DBLogEvent(addresses, subject, message, null, null, null), this);
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
    // if we're not interactive, we'll disregard any checkpointing, in
    // favor of just failing the transaction at commit time if a
    // rollback is later attempted

    if (!interactive)
      {
	return;
      }

    // checkpoint our objects, logEvents, and deletion locks

    checkpoints.push(name, new DBCheckPoint(logEvents, getObjectList(), session));

    // and our namespaces

    // we don't synchronize on dbStore, the odds are zip that a
    // namespace will be created or deleted while we are in the middle
    // of a transaction, since that is only done during schema editing

    for (DBNameSpace space: dbStore.nameSpaces)
      {
	space.checkpoint(this, name);
      }

    Ganymede.db.aSymLinkTracker.checkpoint(session, name);
  }

  /**
   * <p>This method is used to pop a checkpoint off the checkpoint
   * stack without making any other changes to the edit set.  This
   * method is equivalent to a rollback where the checkpoint
   * information is taken off the stack, but this DBEditSet's state is
   * not reverted.</p>
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
   * stack without making any other changes to the edit set.  This
   * method is equivalent to a rollback where the checkpoint
   * information is taken off the stack, but this DBEditSet's state is
   * not reverted.</p>
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

    // if we're not an interactive transaction, we disregard all checkpoints

    if (!interactive)
      {
	return null;
      }

    // see if we can find the checkpoint

    point = (DBCheckPoint) checkpoints.pop(name);

    if (point == null)
      {
	System.err.println(ts.l("popCheckpoint.no_checkpoint", name));
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

	for (DBNameSpace space: dbStore.nameSpaces)
	  {
	    space.popCheckpoint(this, name);
	  }
      }

    Ganymede.db.aSymLinkTracker.popCheckpoint(session, name);

    // if we've cleared the last checkpoint stacked, wake up any
    // threads that are blocking to create new checkpoints

    if (checkpoints.empty())
      {
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

    /* -- */

    if (!interactive)
      {
	// oops, we're non-interactive and we didn't actually do the
	// checkpoint we're being asked to go back to.. set the
	// mustAbort flag so the transaction will never commit

	this.mustAbort = true;

	try
	  {
	    // "rollback() called in non-interactive transaction"
	    throw new RuntimeException(ts.l("rollback.non_interactive"));
	  }
	catch (RuntimeException ex)
	  {
            Ganymede.logError(ex);
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

    for (DBCheckPointObj objck: point.objects)
      {
	DBEditObject obj = findObject(objck.invid);

	if (obj != null)
	  {
	    obj.rollback(objck.fields);
	    obj.status = objck.status;
	  }
	else
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

    // calculate what DBEditObjects we have in the transaction at the
    // present time that we didn't have in the checkpoint

    HashSet<Invid> oldvalues = new HashSet<Invid>();

    for (DBCheckPointObj obj: point.objects)
      {
	oldvalues.add(obj.invid);
      }

    ArrayList<DBEditObject> drop = new ArrayList<DBEditObject>();

    for (DBEditObject eobjRef: objects.values())
      {
	Invid tmpvid = eobjRef.getInvid();

	if (!oldvalues.contains(tmpvid))
	  {
	    drop.add(eobjRef);
	  }
      }

    // and now we get rid of DBEditObjects we need to drop

    for (DBEditObject obj: drop)
      {
	obj.release(true);

	switch (obj.getStatus())
	  {
	  case ObjectStatus.CREATING:
	  case ObjectStatus.DROPPING:
	    obj.getBase().releaseId(obj.getID()); // relinquish the unused invid

	    session.GSession.checkIn();	// XXX *synchronized* on GanymedeSession
	    obj.getBase().getStore().checkIn(); // update checked out count
	    break;

	  case ObjectStatus.EDITING:
	  case ObjectStatus.DELETING:

	    // note that clearShadow updates the checked out count for us.

	    if (!obj.original.clearShadow(this))
	      {
		throw new RuntimeException("editset ownership synchronization error");
	      }

	    break;
	  }
      }

    // now go ahead and clean out the dropped objects

    for (DBEditObject obj: drop)
      {
	objects.remove(obj.getInvid());
      }

    // and our namespaces

    boolean success = true;

    // we don't synchronize on dbStore, the odds are zip that a
    // namespace will be created or deleted while we are in the middle
    // of a transaction, since that is only done during schema editing

    for (DBNameSpace space: dbStore.nameSpaces)
      {
	if (!space.rollback(this, name))
	  {
	    success = false;
	  }
      }

    Ganymede.db.aSymLinkTracker.rollback(session, name);

    return success;
  }

  /**
   * <p>commit is used to cause all changes in association with this
   * DBEditSet to be performed.  If commit() cannot make the changes
   * for any reason, commit() will return a ReturnVal indicating
   * failure and the cause of the commit failure.  Depending on the
   * source of the failure, the transaction may be left open for a
   * subsequent transaction commit or release.</p>
   *
   * <p>The returned {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * will have doNormalProcessing set to false if the transaction was
   * completely aborted.  Both {@link arlut.csd.ganymede.server.DBSession
   * DBSession} and the client should take a false doNormalProcessing
   * boolean as an indicator that the transaction was simply wiped out
   * and a new transaction should be opened for subsequent activity.
   * A true doNormalProcessing value indicates that the client can try
   * the commit again at a later time, perhaps after making changes to
   * fix the commit problem.</p>
   *
   * <p>This method is synchronized and calls a synchronized method on
   * the DBSession which contains this DBEditSet.  Because of this,
   * this method should really only be called by way of the
   * DBSession.commitTransaction() method, to avoid the possibility of
   * nested monitor deadlock.</p>
   *
   * @param comment If not null, a comment to attach to logging and
   * email generated in response to this transaction.
   *
   * @return a ReturnVal indicating success or failure, or null on success
   * without comment.
   */

  public synchronized ReturnVal commit(String comment)
  {
    if (objects == null)
      {
	throw new RuntimeException(ts.l("global.already"));
      }

    if (mustAbort)
      {
	release();

	// "Forced Transaction Abort"
	// "The server ran into a non-reversible error while processing this transaction and forced an abort."
	return Ganymede.createErrorDialog(ts.l("commit.forced_abort"),
					  ts.l("commit.forced_abort_text"));
      }

    this.comment = comment;

    try
      {
	commit_run_precommit_hooks();
	commit_lockBases(); // may block
	commit_verifyNamespaces();
	commit_handlePhase1();
	commit_recordModificationDates();
	commit_integrateChanges();
	releaseWriteLock();

	return null;
      }
    catch (CommitNonFatalException ex)
      {
	return ex.getReturnVal();
      }
    catch (CommitFatalException ex)
      {
	releaseWriteLock();
	release();
	return ex.getReturnVal();
      }
    catch (Throwable ex)
      {
	Ganymede.debug(Ganymede.stackTrace(ex));

	releaseWriteLock();
	release();

	// "Transaction commit failure"
	// "Couldn''t commit transaction, exception caught: {0}"
	return Ganymede.createErrorDialog(ts.l("commit.commit_failure"),
					  ts.l("commit.commit_failure_text", Ganymede.stackTrace(ex)));
      }
    finally
      {
	// just to be sure we don't leave a write lock hanging somehow.
	// if we successfully released before, this is a no-op

	releaseWriteLock();
      }
  }

  /**
   * This hook is run before we lock the bases, so we're still able to
   * make changes to our objects in transaction.
   *
   * The main use of this hook will be to allow DBEditObject
   * subclasses to refresh their hidden unique label fields, if any.
   * If such a hook has a problem, it should return a ReturnVal
   * indicating the problem.
   */

  private final void commit_run_precommit_hooks() throws CommitException
  {
    ReturnVal retVal;
    String checkpointKey = description + " precommit hook";

    /* -- */

    // make a copy of the object references currently in the
    // transaction.
    //
    // XXX right now, if an object's preCommitHook() causes
    // another object to be pulled into the transaction (as by doing
    // invid linkage operations or discrete remote object editing),
    // those new objects will not have their preCommitHook() run.
    //
    // this shouldn't be a big concern since the main purpose of
    // preCommitHook() is to update hidden label fields. /XXX

    DBEditObject[] myObjects = getObjectList();

    checkpoint(checkpointKey);

    try
      {
	for (DBEditObject eObj: myObjects)
          {
            try
              {
                retVal = eObj.preCommitHook();

                if (!ReturnVal.didSucceed(retVal))
                  {
                    throw new CommitNonFatalException(retVal);
                  }
              }
            catch (CommitNonFatalException ex)
              {
                throw ex;
              }
            catch (Throwable ex)
              {
                retVal = Ganymede.createErrorDialog(Ganymede.stackTrace(ex));
                throw new CommitNonFatalException(retVal);
              }
          }
      }
    catch (CommitNonFatalException ex)
      {
	if (!rollback(checkpointKey))
	  {
	    throw new CommitFatalException(ex.getReturnVal());
	  }

	throw ex;
      }

    popCheckpoint(checkpointKey);
  }

  /**
   * <p>Obtain a write lock on all bases modified by this transaction.
   * This method may block indefinitely, waiting on other transactions
   * which are in the process of modifying the DBStore hashes.</p>
   *
   * <p>Returns a Vector of DBObjectBases that we have locked if we
   * succeed.</p>
   *
   * <p>Throws a CommitNonFatalException if we can't get the lock.</p>
   */

  private final Vector<DBObjectBase> commit_lockBases() throws CommitNonFatalException
  {
    Vector<DBObjectBase> baseSet = new Vector();

    /* -- */

    for (DBObjectBase base: basesModified)
      {
	baseSet.addElement(base);
      }

    // and try to lock the bases down.
    // There should be NO WAY we can have a non-null wLock at this point.

    if (wLock != null)
      {
	throw new Error(ts.l("commit_lockBases.wLock", description));
      }

    // Create the lock on the bases changed and establish.  This
    // openWriteLock() call will cause our thread to block until we
    // can get all the bases we need locked.

    try
      {
	wLock = session.openWriteLock(baseSet);	// wait for write lock *synchronized*
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug(ts.l("commit_lockBases.interrupted", String.valueOf(session.key)));

	releaseWriteLock();

	ReturnVal retVal = Ganymede.createErrorDialog(ts.l("commit.commit_failure"),
						      ts.l("commit_lockBases.wLock_refused"));
	throw new CommitNonFatalException(retVal);
      }

    return baseSet;
  }

  /**
   * <p>If this transaction was carried out by an xml session, we will
   * have allowed namespace operations (in particular, moving values
   * from one field in a namespace to another) to be done
   * out-of-order.  In such cases, we have to verify that the xml
   * transaction ultimately put things back right.  This method does
   * that for us, throwing a CommitNonFatalException if we were not
   * successful.</p>
   *
   * <p>Of course, since the xmlclient has no way of interactively
   * fixing a problem, the CommitNonFatalException winds up causing
   * the transaction to be aborted by the xmlclient after all.</p>
   */

  private final void commit_verifyNamespaces() throws CommitNonFatalException
  {
    // interactive transactions aren't allowed to get out of sync in
    // namespace

    if (isInteractive())
      {
	return;
      }

    // we don't synchronize on dbStore.nameSpaces, the nameSpaces
    // vector should never have elements added or deleted while we are
    // in the middle of a transaction, since that is only done during
    // schema editing

    Set<String> totalConflicts = new TreeSet<String>();

    for (DBNameSpace space: dbStore.nameSpaces)
      {
	List<String> conflicts = space.verify_noninteractive(this);

	if (conflicts != null)
	  {
	    totalConflicts.addAll(conflicts);
	  }
      }

    if (totalConflicts.size() > 0)
      {
	// "Error, namespace conflicts remaining at transaction commit time.
	// The following values are in namespace conflict:\n\t{0}"
	ReturnVal retVal = Ganymede.createErrorDialog("",
						      ts.l("commit_verifyNamespaces.conflicts",
							   VectorUtils.vectorString(totalConflicts, ",\n\t")));
	throw new CommitNonFatalException(retVal);
      }
  }

  /**
   * <p>This private helper method for the commit() method handles
   * phase 1 of transaction commit.</p>
   *
   * <p>If an object refuses transaction commit, we'll throw a
   * CommitNonFatalException with ReturnVal information encoded.</p>
   */

  private final void commit_handlePhase1() throws CommitNonFatalException
  {
    ReturnVal retVal;
    List<DBEditObject> committedObjects = new ArrayList<DBEditObject>();

    /* -- */

    for (DBEditObject eObj: this.objects.values())
      {
	try
	  {
	    retVal = eObj.commitPhase1();
	  }
	catch (Throwable ex)
	  {
	    retVal = Ganymede.createErrorDialog(ts.l("commit_handlePhase1.exception"),
						Ganymede.stackTrace(ex));

	    eObj.release(false);

	    for (DBEditObject eObj2: committedObjects)
	      {
		eObj2.release(false); // unlock commit mode
	      }

	    // let DBSession/the client know they can retry
	    // things.. but if we've got an error in an object's
	    // commitPhase1, committing again will probably just
	    // repeat the problem.

	    throw new CommitNonFatalException(retVal);
	  }

	// the object has now been locked to commit mode, and will not
	// allow further modifications from the client

	if (ReturnVal.didSucceed(retVal))
	  {
	    try
	      {
		commit_checkObjectMissingFields(eObj);
	      }
	    catch (CommitNonFatalException ex)
	      {
		retVal = ex.getReturnVal();
	      }
	  }

	// retVal could be set by either eObj.commitPhase1() or
	// by commit_checkObjectMissingFields()

	if (!ReturnVal.didSucceed(retVal))
	  {
	    eObj.release(false);

	    for (DBEditObject eObj2: committedObjects)
	      {
		eObj2.release(false); // unlock commit mode
	      }

	    // let DBSession/the client know they can retry things.

	    throw new CommitNonFatalException(retVal);
	  }
	else
	  {
	    committedObjects.add(eObj);
	  }
      }
  }

  /**
   * <p>This private helper method for the commit() method runs a
   * check looking for missing mandatory fields on an object
   * involved with this transaction.</p>
   *
   * <p>If the object is missing fields, a CommitNonFatalException
   * will be thrown.</p>
   */

  private final void commit_checkObjectMissingFields(DBEditObject eObj) throws CommitNonFatalException
  {
    ReturnVal retVal;
    Set<String> missingFields = new TreeSet<String>();

    /* -- */

    // if we're deleting or dropping an object, we won't ever require
    // fields to be set

    if (eObj.getStatus() == ObjectStatus.DELETING ||
	eObj.getStatus() == ObjectStatus.DROPPING)
      {
	return;
      }

    // otherwise, we always insist on the label field being present.
    // We'll check that up front.

    DBField labelField = eObj.retrieveField(eObj.getLabelFieldID());

    if (labelField == null || !labelField.isDefined())
      {
	// the label field is missing.  look it up.

	DBObjectBaseField fieldDef = (DBObjectBaseField) eObj.getBase().getField(eObj.getLabelFieldID());

	missingFields.add(fieldDef.getName());
      }

    if (isOversightOn())
      {
	Vector<String> missingRequiredFields = eObj.checkRequiredFields();

	if (missingRequiredFields != null)
	  {
	    missingFields.addAll(missingRequiredFields);
	  }
      }

    if (missingFields.size() > 0)
      {
	StringBuilder errorBuf = new StringBuilder();

	errorBuf.append(ts.l("commit_checkObjectMissingFields.missing_fields_text",
			     eObj.getTypeName(),
			     eObj.getLabel()));

	for (String fieldName: missingFields)
	  {
	    errorBuf.append(fieldName);
	    errorBuf.append("\n");
	  }

	retVal = Ganymede.createErrorDialog(ts.l("commit_checkObjectMissingFields.missing_fields"),
					    errorBuf.toString());

	// let DBSession/the client know they can retry things.

	throw new CommitNonFatalException(retVal);
      }
  }

  /**
   * <p>This private helper method for the commit() method records
   * the creation/modification timestamp for the vector of
   * committed objects.</p>
   */

  private final void commit_recordModificationDates()
  {
    DateDBField df;
    StringDBField sf;
    String result = this.session.getID() + " [" + this.description + "]";
    Date modDate = new Date();

    /* -- */

    // intern the result string so that we don't have multiple
    // copies of common strings in our heap

    result = result.intern();

    for (DBEditObject eObj: objects.values())
      {
	// force a change of date and modifier information
	// into the object without using the normal field
	// modification methods.. this lets us set field
	// values at a time when the object would reject
	// changes from the user because the committing flag
	// is set.

	switch (eObj.getStatus())
	  {
	  case ObjectStatus.CREATING:

	    if (!eObj.isEmbedded())
	      {
		df = (DateDBField) eObj.getField(SchemaConstants.CreationDateField);

		// If we're processing an XML transaction and the XML
		// transaction already specified a creation date (the
		// df field is not undefined), we'll go ahead and
		// leave it alone.

		// this behavior is intended to allow data to be
		// dumped from a Ganymede 1.0 server, manually
		// massaged to bring it into compliance with a
		// Ganymede 2.0 server's schema, and then reloaded
		// without losing the original creation information.

		if (!allowXMLHistoryOverride || !df.isDefined())
		  {
		    df.value = modDate;
		  }

		// ditto for the creator info field.

		sf = (StringDBField) eObj.getField(SchemaConstants.CreatorField);

		if (!allowXMLHistoryOverride || !sf.isDefined())
		  {
		    sf.value = result;
		  }
	      }

	    // * fall-through *

	  case ObjectStatus.EDITING:

	    if (!eObj.isEmbedded())
	      {
		df = (DateDBField) eObj.getField(SchemaConstants.ModificationDateField);
		if (!allowXMLHistoryOverride || !df.isDefined())
		  {
		    df.value = modDate;
		  }

		sf = (StringDBField) eObj.getField(SchemaConstants.ModifierField);
		if (!allowXMLHistoryOverride || !sf.isDefined())
		  {
		    sf.value = result;
		  }
	      }
	  }
      }
  }

  /**
   * <p>This private helper method for commit() integrates all
   * committed objects back into the DBStore, handling on-disk change
   * journaling, transaction logging, namespaces, and more.</p>
   */

  private final void commit_integrateChanges() throws CommitFatalException
  {
    Set<DBObjectBaseField> fieldsTouched = new HashSet<DBObjectBaseField>();

    /* -- */

    // we serialize transaction commits here

    synchronized (dbStore.journal)
      {
	commit_persistTransaction();
	commit_writeSyncChannels();
	commit_finalizeTransaction();
      }

    // we've successfully persisted the transaction, written the
    // transaction to the sync channels, and finalized the
    // transaction.. we can proceed
    //
    // note that we still have our write lock established, even though
    // we are giving up synchronization on the journal.

    try
      {
	// we sync on Ganymede global objects in the following, but we
	// don't keep external sync for more than one step, so
	// multiple transactions on non-overlapping DBObjectBases can
	// proceed through this section concurrently

	commit_handlePhase2();
	commit_logTransaction(fieldsTouched); // *sync* Ganymede.log
	commit_replace_objects();
	commit_updateNamespaces(); // *sync* over each namespace in Ganymede.db.nameSpaces
	DBDeletionManager.releaseSession(session);   // *sync* static DBDeletionManager
	Ganymede.db.aSymLinkTracker.commit(session); // *sync* Ganymede.db.aSymLinkTracker
	commit_updateBases(fieldsTouched);
      }
    catch (Throwable ex)
      {
	// If we throw up here, we've got real problems

	throw new CommitError("Critical error: Intolerable exception during commit_integrateChanges().", ex);
      }
  }

  /**
   * <p>This private helper method for commit() writes the transaction
   * to the on-disk transactions journal, which will persist our
   * transaction's changes.</p>
   *
   * <p>Will throw a CommitException if a failure was detected.</p>
   */

  private final void commit_persistTransaction() throws CommitFatalException
  {
    try
      {
	persistedTransaction = dbStore.journal.writeTransaction(this);

	if (persistedTransaction == null)
	  {
	    // "Couldn''t commit transaction, couldn''t write transaction to disk"
	    // "Couldn''t commit transaction, the server may have run out of disk space.  Couldn''t write transaction to disk."
	    throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_persistTransaction.error"),
								      ts.l("commit_persistTransaction.error_text")));
	  }
      }
    catch (Throwable ex)
      {
	if (ex instanceof IOException)
	  {
	    // "Couldn''t commit transaction, Exception caught writing journal"
	    // "Couldn''t commit transaction, the server may have run out of disk space.\n\n{0}"
	    throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_persistTransaction.exception"),
								      ts.l("commit_persistTransaction.ioexception_text",
									   Ganymede.stackTrace(ex))));
	  }
	else
	  {
	    // "Couldn''t commit transaction, Exception caught writing journal"
	    // "Couldn''t commit transaction, an exception was caught persisting to the journal.\n\n{0}"
	    throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_persistTransaction.exception"),
								      ts.l("commit_persistTransaction.exception_text",
									   Ganymede.stackTrace(ex))));
	  }
      }
  }

  /**
   * <p>This private helper method for commit() writes the transaction
   * to all of the builder queue sync channels.</p>
   */

  private final void commit_writeSyncChannels() throws CommitFatalException
  {
    DBEditObject[] objectList = getObjectList();

    try
      {
	for (scheduleHandle handle: Ganymede.scheduler.getTasksByClass(SyncRunner.class))
	  {
	    SyncRunner sync = (SyncRunner) handle.task;

	    try
	      {
		if (sync.isIncremental())
		  {
		    sync.writeIncrementalSync(persistedTransaction, objectList, this);
		  }
		else if (sync.isFullState())
		  {
		    sync.checkBuildNeeded(persistedTransaction, objectList, this);
		  }
	      }
	    catch (java.io.FileNotFoundException in_ex)
	      {
		// "Couldn''t write transaction to sync channel.  Exception caught writing to sync channel."
		// "Couldn''t write transaction to sync channel {0} due to a FileNotFoundException.
		//
		// This sync channel is configured to write to {1}, but this directory does not exist or is not writable.
		//
		// Transaction Cancelled."
		
		throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_writeSyncChannels.exception"),
									  ts.l("commit_writeSyncChannels.no_sync_found", sync.getName(), sync.getDirectory())));
	      }
	  }
      }
    catch (Throwable ex)
      {
	undoSyncChannels();

	try
	  {
	    dbStore.journal.undoTransaction(persistedTransaction);
	  }
	catch (IOException inex)
	  {
	    // This *really* shouldn't happen, since there's no writes involved
	    // in truncating the journal.  If it did, we're kind of screwed, though.

	    // ***
	    // *** Error in commit_writeSyncChannels()!  Couldn''t undo a transaction in the
	    // *** journal file after catching an exception!
	    // ***
	    // *** The journal may not be completely recoverable!
	    // ***
	    //
	    // {0}

	    Ganymede.debug(ts.l("commit_writeSyncChannels.badundo", Ganymede.stackTrace(ex)));
	  }

	if (ex instanceof CommitFatalException)
	  {
	    throw (CommitFatalException) ex;
	  }
	else if (ex instanceof IOException)
	  {
	    // "Couldn''t write transaction to sync channel.  Exception caught writing to sync channel."
	    // "Couldn''t write transaction to sync channels due to an IOException.   The server may have run out of disk space.\n\n{0}"
	    throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_writeSyncChannels.exception"),
								      ts.l("commit_writeSyncChannels.ioexception_text",
									   Ganymede.stackTrace(ex))));
	  }
	else
	  {
	    // "Couldn''t write transaction to sync channel.  Exception caught writing to sync channel."
	    // "Exception caught while writing to sync channels.  Sync channels write aborted.\n\n{0}"
	    throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_writeSyncChannels.exception"),
								      ts.l("commit_writeSyncChannels.exception_text",
									   Ganymede.stackTrace(ex))));
	  }
      }
  }

  /**
   * <p>This private helper method scrubs the sync channels of the
   * persistedTransaction, so that we can avoid having bits of the
   * transaction sync'ed to the channels when we ultimately wind up
   * undoing the transaction.</p>
   */

  private final void undoSyncChannels()
  {
    for (scheduleHandle handle: Ganymede.scheduler.getTasksByClass(SyncRunner.class))
      {
	SyncRunner sync = (SyncRunner) handle.task;

	if (sync.isIncremental())
	  {
	    try
	      {
		sync.unSync(persistedTransaction);
	      }
	    catch (Throwable inex)
	      {
		// what can we do?  keep clearing them out as best we
		// can

		Ganymede.logError(inex);
	      }
	  }
      }
  }

  /**
   * <p>This private helper method for commit() writes a finalized
   * token to the on-disk transactions journal, so that we'll know
   * upon restart that we don't need to scrub the transaction from the
   * sync channels.</p>
   *
   * <p>Will throw a CommitException if a failure was detected.</p>
   */

  private final void commit_finalizeTransaction() throws CommitFatalException
  {
    try
      {
	dbStore.journal.finalizeTransaction(persistedTransaction);
      }
    catch (IOException ex)
      {
	try
	  {
	    undoSyncChannels();
	    dbStore.journal.undoTransaction(persistedTransaction);
	  }
	catch (IOException inex)
	  {
	    // This *really* shouldn't happen, since there's no writes involved
	    // in truncating the journal.  If it did, we're kind of screwed, though.

	    // ***
	    // *** Error in commit_finalizeTransaction()!  Couldn''t undo a transaction in the
	    // *** journal file after catching an exception!
	    // ***
	    // *** The journal may not be completely recoverable!
	    // ***
	    //
	    // {0}

	    Ganymede.debug(ts.l("commit_finalizeTransaction.badundo", Ganymede.stackTrace(ex)));
	  }

	// "Couldn''t finalize transaction to journal.  IOException caught writing to journal."
	// "Couldn''t finalize transaction to journal, the server may have run out of disk space.\n\n{0}"
	throw new CommitFatalException(Ganymede.createErrorDialog(ts.l("commit_finalizeTransaction.exception"),
								  ts.l("commit_finalizeTransaction.exception_text",
								       Ganymede.stackTrace(ex))));
      }
    finally
      {
	persistedTransaction = null;
      }
  }

  /**
   * <p>This private helper method calls {@link
   * arlut.csd.ganymede.server.DBEditObject#commitPhase2()} on the
   * DBEditObjects in this transaction, after we have successfully
   * finalized the transaction to disk.</p>
   */

  private final void commit_handlePhase2()
  {
    for (DBEditObject eObj: objects.values())
      {
	// tell the object to go ahead and do any external
	// commit actions.

	try
	  {
	    eObj.commitPhase2();
	  }
	catch (Throwable ex)
	  {
	    // if we get a runtime exception here, there's nothing to
	    // do for it.. we're locked on course.  log the trace, but
	    // stay on target.. stay on target..

	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }
      }
  }

  /**
   * This private helper method is executed in the middle of the
   * commit() method, and handles logging for any changes made to
   * objects during the committed transaction.
   *
   * While this method is examining each object in the transaction to
   * determine the diffs, we'll also take the opportunity to track the
   * identity of all fields in all object bases that have themselves
   * been touched.  We use the fieldsTouched Set for this purpose,
   * storing the DBObjectBaseFields that were touched.
   */

  private final void commit_log_events(Set<DBObjectBaseField> fieldsTouched)
  {
    for (DBEditObject eObj: objects.values())
      {
	try
	  {
	    commit_log_event(eObj, fieldsTouched);
	  }
	catch (Throwable ex)
	  {
	    // we're already committed, so we just warn about the log
	    // failure and move on

	    // "Error!  Problem occured while writing log entry, continuing with transaction commit.\n{0}"
	    Ganymede.debug(ts.l("commit_log_events.log_failure", Ganymede.stackTrace(ex)));
	  }
      }
  }

  /**
   * <p>This private helper method is executed in the middle of the
   * commit() method, and handles logging for any changes made to a
   * DBEditObject during the committed transaction.</p>
   *
   * <p>While this method is examining each object in the transaction
   * to determine the diffs, we'll also take the opportunity to track
   * the identity of all fields in all object bases that have
   * themselves been touched.  We use the fieldsTouched hashtable for
   * this purpose, storing identity maps for the DBObjectBaseFields
   * that were touched.</p>
   */

  private final void commit_log_event(DBEditObject eObj, Set<DBObjectBaseField> fieldsTouched)
  {
    if (Ganymede.log == null)
      {
	return;
      }

    Vector<Invid> invids;
    String diff;
    Invid responsibleInvid = null;
    String responsibleName = null;

    /* -- */

    if (getGSession() != null)
      {
	responsibleInvid = getGSession().personaInvid;

	if (responsibleInvid == null)
	  {
	    responsibleInvid = getGSession().userInvid;
	  }

	responsibleName = getGSession().getPersonaLabel();
      }
    else
      {
	responsibleInvid = null;
	responsibleName = "system";
      }

    switch (eObj.getStatus())
      {
      case ObjectStatus.EDITING:

	invids = new Vector<Invid>();
	invids.add(eObj.getInvid());

	// if we changed an embedded object, make a
	// note that the containing object was
	// involved so that we show changes to
	// embedded objects in a log search for the
	// containing object

	if (eObj.isEmbedded())
	  {
	    DBObject container = session.getContainingObj(eObj);

	    if (container != null)
	      {
		invids.add(container.getInvid());
	      }
	  }

	diff = eObj.diff(fieldsTouched);

	if (diff != null)
	  {
	    boolean logNormal = true;

	    if (eObj.isEmbedded())
	      {
		try
		  {
		    DBObject parentObj = session.getContainingObj(eObj);
		    
		    // "{0} {1}''s {2} ''{3}'', <{4}> was modified.\n\n{5}"

		    streamLogEvent("objectchanged",
				   ts.l("commit_createLogEvent.embedded_modified",
					parentObj.getTypeName(),
					parentObj.getLabel(),
					eObj.getTypeName(),
					eObj.getLabel(),
					eObj.getInvid(),
					diff),
				   responsibleInvid, responsibleName,
				   invids, (List<String>) VectorUtils.union(eObj.getEmailTargets(), parentObj.getEmailTargets()));

		    logNormal = false;
		  }
		catch (IntegrityConstraintException ex)
		  {
		    // We might catch this from
		    // getContainingObj if the
		    // embedded object doesn't have a
		    // proper container.  Won't be
		    // fatal, as we'll just leave
		    // logNormal true and handle it below

		    Ganymede.debug(Ganymede.stackTrace(ex));
		  }
	      }

	    if (logNormal)
	      {
		streamLogEvent("objectchanged",
			       ts.l("commit_createLogEvent.modified",
				    eObj.getTypeName(),
				    eObj.getLabel(),
				    String.valueOf(eObj.getInvid()),
				    diff),
			       responsibleInvid, responsibleName,
			       invids, (List<String>) eObj.getEmailTargets());
	      }
	  }

	break;

      case ObjectStatus.CREATING:

	invids = new Vector<Invid>();
	invids.add(eObj.getInvid());

	// if we created an embedded object, make a
	// note that the containing object was
	// involved so that we show changes to
	// embedded objects in a log search for the
	// containing object

	if (eObj.isEmbedded())
	  {
	    DBObject container = session.getContainingObj(eObj);

	    if (container != null)
	      {
		invids.add(container.getInvid());
	      }
	  }

	// We'll call diff() to update the fieldsTouched hashtable,
	// but we won't use the string generated, since
	// getPrintString() does a better job of describing the
	// contents of embedded objects.  This forced use of diff()
	// isn't elegant, but as DBField was originally defined, it's
	// only through the use of the diff strings that we have a
	// unified way to determine change, and we don't want to have
	// to re-do that work in all the DBField subclasses.

	eObj.diff(fieldsTouched);

	diff = eObj.getPrintString();

	if (diff != null)
	  {
	    boolean logNormal = true;

	    if (eObj.isEmbedded())
	      {
		try
		  {
		    DBObject parentObj = session.getContainingObj(eObj);

                    // "{0} {1}''s {2} ''{3}'', <{4}> was created.\n\n{5}\n"
		    streamLogEvent("objectcreated",
				   ts.l("commit_createLogEvent.embedded_created",
					parentObj.getTypeName(),
					parentObj.getLabel(),
					eObj.getTypeName(),
					eObj.getLabel(),
					eObj.getInvid(),
					diff),
				   responsibleInvid, responsibleName,
				   invids, (List<String>) VectorUtils.union(eObj.getEmailTargets(), parentObj.getEmailTargets()));

		    logNormal = false;
		  }
		catch (IntegrityConstraintException ex)
		  {
		    // We might catch this from
		    // getContainingObj if the
		    // embedded object doesn't have a
		    // proper container.  Won't be
		    // fatal, as we'll just leave
		    // logNormal true and handle it below

		    Ganymede.debug(Ganymede.stackTrace(ex));
		  }
	      }

	    if (logNormal)
	      {
                // "{0} {1}, <{2}> was created.\n\n{3}\n"
		streamLogEvent("objectcreated",
			       ts.l("commit_createLogEvent.created",
				    eObj.getTypeName(),
				    eObj.getLabel(),
				    String.valueOf(eObj.getInvid()),
				    diff),
			       responsibleInvid, responsibleName,
			       invids, eObj.getEmailTargets());
	      }
	  }

	break;

      case ObjectStatus.DELETING:

	invids = new Vector<Invid>();
	invids.add(eObj.getInvid());

	// if we deleted an embedded object, make a
	// note that the containing object was
	// involved so that we show changes to
	// embedded objects in a log search for the
	// containing object

	if (eObj.isEmbedded())
	  {
	    try
	      {
		DBObject container = session.getContainingObj(eObj);

		if (container != null)
		  {
		    invids.add(container.getInvid());
		  }
	      }
	    catch (IntegrityConstraintException ex)
	      {
		// GanymedeServer.sweepEmbeddedObjects() may need to
		// delete an embedded object with no container
		// registered if an error condition elsewhere in the
		// Ganymede server left a dangling embedded object.

		// So we'll ignore this here for the sake of getting
		// the dangling embedded object properly flushed.
	      }
	  }

	String oldVals = null;

	try
	  {
	    oldVals = eObj.getOriginal().getPrintString();
	  }
	catch (NullPointerException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }

	if (oldVals != null)
	  {
	    boolean logNormal = true;

	    if (eObj.isEmbedded())
	      {
		try
		  {
		    DBObject parentObj = session.getContainingObj(eObj);

		    streamLogEvent("deleteobject",
				   ts.l("commit_createLogEvent.embedded_deleted",
					parentObj.getTypeName(),
					parentObj.getLabel(),
					eObj.getTypeName(),
					eObj.getLabel(),
					eObj.getInvid(),
					oldVals),
				   responsibleInvid, responsibleName,
				   invids, (List<String>) VectorUtils.union(eObj.getEmailTargets(), parentObj.getEmailTargets()));

		    logNormal = false;
		  }
		catch (IntegrityConstraintException ex)
		  {
		    // We might catch this from
		    // getContainingObj if the
		    // embedded object doesn't have a
		    // proper container.  Won't be
		    // fatal, as we'll just leave
		    // logNormal true and handle it below

		    Ganymede.debug(Ganymede.stackTrace(ex));
		  }
	      }

	    if (logNormal)
	      {
		streamLogEvent("deleteobject",
			       ts.l("commit_createLogEvent.deleted",
				    eObj.getTypeName(),
				    eObj.getLabel(),
				    String.valueOf(eObj.getInvid()),
				    oldVals),
			       responsibleInvid, responsibleName,
			       invids, eObj.getEmailTargets());
	      }

	    // and calculate the fields that we touched by losing them

	    DBObject origObj = eObj.getOriginal();

	    for (DBObjectBaseField fieldDef: origObj.objectBase.getFieldsInFieldOrder())
	      {
		// we don't care if certain fields change

		if (fieldDef.getID() == SchemaConstants.CreationDateField ||
		    fieldDef.getID() == SchemaConstants.CreatorField ||
		    fieldDef.getID() == SchemaConstants.ModificationDateField ||
		    fieldDef.getID() == SchemaConstants.ModifierField)
		  {
		    continue;
		  }

		DBField origField = (DBField) origObj.getField(fieldDef.getID());

		if (origField != null && origField.isDefined())
		  {
		    fieldsTouched.add(fieldDef);
		  }
	      }
	  }
	else
	  {
	    boolean logNormal = true;

	    if (eObj.isEmbedded())
	      {
		try
		  {
		    DBObject parentObj = session.getContainingObj(eObj);

		    streamLogEvent("deleteobject",
				   ts.l("commit_createLogEvent.embedded_deleted_nodiff",
					parentObj.getTypeName(),
					parentObj.getLabel(),
					eObj.getTypeName(),
					eObj.getLabel(),
					eObj.getInvid()),
				   responsibleInvid, responsibleName,
				   invids, (List<String>) VectorUtils.union(eObj.getEmailTargets(), parentObj.getEmailTargets()));

		    logNormal = false;
		  }
		catch (IntegrityConstraintException ex)
		  {
		    // We might catch this from
		    // getContainingObj if the
		    // embedded object doesn't have a
		    // proper container.  Won't be
		    // fatal, as we'll just leave
		    // logNormal true and handle it below

		    Ganymede.debug(Ganymede.stackTrace(ex));
		  }
	      }

	    if (logNormal)
	      {
		streamLogEvent("deleteobject",
			       ts.l("commit_createLogEvent.deleted_nodiff",
				    eObj.getTypeName(),
				    eObj.getLabel(),
				    String.valueOf(eObj.getInvid())),
			       responsibleInvid, responsibleName,
			       invids, eObj.getEmailTargets());
	      }
	  }

	break;
      }
  }

  /**
   * <p>This method handles the on-disk and email logging for events
   * that have built up over the course of this transaction.</p>
   */

  private final void commit_logTransaction(Set<DBObjectBaseField> fieldsTouched)
  {
    Invid responsibleInvid;
    String responsibleName;

    /* -- */

    if (Ganymede.log == null)
      {
	// if our log is null, as it is with DBStore bootstrap, then
	// we don't need to do any logging..  we'll just return.
	//
	// note that this means that when we have a log, we also won't
	// be updating the per-field-type timestamps in
	// DBObjectBaseField.
	//
	// This is acceptable, since the DBStore bootstrap is only for
	// creating the mandatory Ganymede objects, which we shouldn't
	// care about in a GanymedeBuilderTask context (the only thing
	// that cares about the DBObjectBase/DBObjectBaseField
	// timestamps).

	return;
      }

    if (getGSession() != null)
      {
	responsibleName = getGSession().getPersonaLabel();
	responsibleInvid = getGSession().getPersonaInvid();

	if (responsibleInvid == null)
	  {
	    responsibleInvid = getGSession().getUserInvid();
	  }
      }
    else
      {
	responsibleName = "system";
	responsibleInvid = null;
      }

    // collect the list of invids that we know were touched in this
    // transaction for the start transaction log record

    Vector<Invid> invids = new Vector<Invid>(objects.keySet());

    synchronized (Ganymede.log)
      {
	try
	  {
	    Ganymede.log.startTransactionLog(invids, responsibleName, responsibleInvid, comment, this);

	    // then transmit/log any pre-recorded log events that we
	    // have accumulated during the user's session/transaction

	    for (DBLogEvent event: logEvents)
	      {
		streamLogEvent(event);
	      }

	    // for garbage collection

	    logEvents.clear();

	    // then create and stream log events describing the
	    // objects that are in this transaction at the time of
	    // commit

	    commit_log_events(fieldsTouched);

	    // finish the transaction to disk and send out any email
	    // that we need to send

	    Ganymede.log.endTransactionLog(invids, responsibleName, responsibleInvid, this);
	  }
	catch (Throwable ex)
	  {
	    // exceptions during logging aren't important enough to break a
	    // transaction commit in progress, but we do want to record any
	    // such

	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }
	finally
	  {
	    logEvents = null;
	    Ganymede.log.cleanupTransaction();
	  }
      }
  }

  /**
   * This method integrates commiteted objects back into our in-memory
   * DBStore data structures, and clears the transaction of objects as
   * it does so.
   */

  private final void commit_replace_objects()
  {
    for (DBEditObject eObj: objects.values())
      {
	commit_replace_object(eObj);
      }

    objects.clear();
  }

  /**
   * <p>Private helper method for commit() that integrates committed
   * objects back into the DBStore hashes.</p>
   *
   * <p>If this method throws an exception, we will be pretty screwed,
   * as it means we're not able to replace an object that we've
   * already committed to our logs.  This essentially boils down to
   * meaning that we're screwed if we can't create a new DBObject with
   * a copy constructor from a DBEditObject.</p>
   */

  private final void commit_replace_object(DBEditObject eObj)
  {
    DBObjectBase base;

    /* -- */

    base = eObj.getBase();

    // Create a new DBObject from our DBEditObject and insert
    // into the object hash

    switch (eObj.getStatus())
      {
      case ObjectStatus.CREATING:
      case ObjectStatus.EDITING:

	// Create a read-only version of eObj, with all fields
	// reset to checked-in status, put it into our object hash

	// note that this new DBObject will not include any
	// transient fields which self-identify as undefined

	base.put(new DBObject(eObj));

	// (note that we can't use a no-sync put above, since
	// we don't prevent asynchronous viewDBObject().

	if (getGSession() != null)
	  {
	    getGSession().checkIn();
	  }

	base.getStore().checkIn(); // update checkout count

	break;

      case ObjectStatus.DELETING:

	// Deleted objects had their deletion finalization done before
	// we ever got to this point.

	// Note that we don't try to release the id for previously
	// registered objects.. the base.releaseId() method really
	// can only handle popping object id's off of a stack, and
	// can't do anything for object id's unless the id was the
	// last one allocated in that base, which is unlikely
	// enough that we don't worry about it here.

	base.remove(eObj.getID());

	// (note that we can't use a no-sync remove above, since
	// we don't prevent asynchronous viewDBObject().

	session.GSession.checkIn(); // *synchronized*
	base.getStore().checkIn(); // count it as checked in once it's deleted
	break;

      case ObjectStatus.DROPPING:

	// dropped objects had their deletion finalization done before
	// we ever got to this point..

	base.releaseId(eObj.getID()); // relinquish the unused invid

	if (getGSession() != null)
	  {
	    getGSession().checkIn();
	  }

	base.getStore().checkIn(); // count it as checked in once it's deleted
	break;
      }
  }

  /**
   * <p>Private helper method for commit() which causes all namespaces to update
   * themselves in conjunction with a commit.</p>
   */

  private final void commit_updateNamespaces()
  {
    // we don't synchronize on dbStore.nameSpaces, the nameSpaces
    // vector should never have elements added or deleted while we are
    // in the middle of a transaction, since that is only done during
    // schema editing

    for (DBNameSpace space: dbStore.nameSpaces)
      {
	space.commit(this);
      }
  }

  /**
   * Private helper method for commit() which causes all bases that
   * were touched by this transaction to be updated.
   *
   * This should be run as late as possible in the commit()
   * sequence to minimize the chance that a previously scheduled
   * builder task completes and updates its lastRunTime field after we
   * have touched the timestamps on the changed bases.
   *
   * @param fieldsTouched hash of DBObjectBases that contain objects
   * created, changed, or deleted during this transaction
   */

  private final void commit_updateBases(Set<DBObjectBaseField> fieldsTouched)
  {
    for (DBObjectBase base: basesModified)
      {
	base.updateTimeStamp();

	// and, very important, update the base's snapshot vector
	// so that any new queries that are issued will proceed
	// against the new state of objects in this base

	base.updateIterationSet();
      }

    // And in addition to updating the time stamps on the object
    // bases, update the time stamps on each field.

    for (DBObjectBaseField fieldDef: fieldsTouched)
      {
	fieldDef.updateTimeStamp();
      }
  }

  /**
   * <p>This method is intended for use by DBSession's
   * abortTransaction() method, and returns true if the transaction
   * could be aborted, false otherwise.</p>
   */

  public synchronized boolean abort()
  {
    // if we are called while we are waiting on a write lock in order
    // to commit() on another thread, try to kill it off.  We
    // synchronize on Ganymede.db.lockSync here because we are using
    // that as a monitor for all lock operations, and we need the
    // wLock.inEstablish check to be sync'ed so that we don't force an
    // abort after we have gotten our lock established and are busy
    // mucking with the server's DBObjectTables.

    synchronized (Ganymede.db.lockSync)
      {
	if (wLock != null)
	  {
	    if (wLock.inEstablish)
	      {
		return false;
	      }

	    wLock.abort();
	  }
      }

    release();

    return true;
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

  private final void release()
  {
    if (objects == null)
      {
	throw new RuntimeException(ts.l("global.already"));
      }

    for (DBEditObject eObj: objects.values())
      {
	eObj.release(true);

	switch (eObj.getStatus())
	  {
	  case ObjectStatus.CREATING:
	  case ObjectStatus.DROPPING:
	    eObj.getBase().releaseId(eObj.getID()); // relinquish the unused invid

	    if (getGSession() != null)
	      {
		getGSession().checkIn();
	      }

	    eObj.getBase().getStore().checkIn(); // update checked out count
	    break;

	  case ObjectStatus.EDITING:
	  case ObjectStatus.DELETING:

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

    for (DBNameSpace space: dbStore.nameSpaces)
      {
	space.abort(this);
      }

    // release any deletion locks we have asserted

    DBDeletionManager.releaseSession(session);

    // and scrub any link tracking data for the session

    Ganymede.db.aSymLinkTracker.abort(session);

    // make sure that we haven't somehow left a write lock
    // hanging.. and let's do it before we deconstruct.  This is a
    // no-op if we don't have a write lock open.

    releaseWriteLock();

    // and help out garbage collection some

    this.deconstruct();
  }

  /**
   * <p>Private helper method for commit() and release(), which breaks apart
   * and nulls references to data structures maintained for this transaction
   * to aid GC.</p>
   *
   * <p>This method also does a notifyAll() to wake up any checkpoint threads
   * that are blocking waiting for the ability to checkpoint.</p>
   */

  private void deconstruct()
  {
    if (objects != null)
      {
	objects.clear();
	objects = null;
      }

    if (logEvents != null)
      {
	logEvents.clear();
	logEvents = null;
      }

    if (basesModified != null)
      {
	basesModified.clear();
	basesModified = null;
      }

    dbStore = null;
    session = null;

    description = null;

    if (checkpoints != null)
      {
	checkpoints.removeAllElements();
	checkpoints = null;
      }

    this.notifyAll();		// wake up any late checkpointers
  }

  /**
   * <p>This is a dinky little private helper method to keep things
   * clean.  It's essential that wLock be released if things go
   * wrong, else next time this session tries to commit a transaction,
   * it'll wind up waiting forever for the old lock to be released.</p>
   *
   * <p>Note that this method will fail if called after deconstruct().</p>
   */

  private void releaseWriteLock()
  {
    if (wLock != null)
      {
	session.releaseLock(wLock);
	wLock = null;
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 CommitException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific Exception that can be thrown by code in
 * the server during a transactional commit.</p>
 */

class CommitException extends Exception {

  public CommitException()
  {
    super();
  }

  public CommitException(String s)
  {
    super(s);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                         CommitNonFatalException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific Exception that can be thrown by code in
 * the server during a transactional commit.</p>
 *
 * <p>The ReturnVal encapsulated by this exception class will be coded so
 * that upstream code can re-try the transaction commit once the problems
 * that caused the CommitNonFatalException to be thrown are fixed.</p>
 */

class CommitNonFatalException extends CommitException {

  private ReturnVal retVal;

  /* -- */

  public CommitNonFatalException(ReturnVal retVal)
  {
    super();
    this.retVal = retVal;
    retVal.doNormalProcessing = true;
  }

  public CommitNonFatalException(String s, ReturnVal retVal)
  {
    super(s);
    this.retVal = retVal;
    retVal.doNormalProcessing = true;
  }

  public ReturnVal getReturnVal()
  {
    return retVal;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                            CommitFatalException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific Exception that can be thrown by code in
 * the server during a transactional commit.</p>
 *
 * <p>The ReturnVal encapsulated by a CommitFatalException will cause all
 * upstream code to treat the transaction as fatally compromised, and a
 * transaction cancel will be triggered.</p>
 */

class CommitFatalException extends CommitException {

  private ReturnVal retVal;

  /* -- */

  public CommitFatalException(ReturnVal retVal)
  {
    super();
    this.retVal = retVal;
    retVal.doNormalProcessing = false;
  }

  public CommitFatalException(String s, ReturnVal retVal)
  {
    super(s);
    this.retVal = retVal;
    retVal.doNormalProcessing = false;
  }

  public ReturnVal getReturnVal()
  {
    return retVal;
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                                     CommitError

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific Error that can be thrown by code in
 * the server during a transactional commit, signifying that a commit
 * failed in a possibly non-recoverable way.</p>
 */

class CommitError extends Error {

  public CommitError()
  {
    super();
  }

  public CommitError(String s)
  {
    super(s);
  }

  public CommitError(String s, Throwable t)
  {
    super(s, t);
  }
}
