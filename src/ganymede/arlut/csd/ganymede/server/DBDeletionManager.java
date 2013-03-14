/*
   GASH 2

   DBDeletionManager.java

   The GANYMEDE object storage system.

   Created: 23 June 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBDeletionManager

------------------------------------------------------------------------------*/

/**
 * <p>The DBDeletionManager class is used to handle deletion locking
 * in the Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
 *
 * <p>This class was designed to remove the necessity of having all
 * {@link arlut.csd.ganymede.common.Invid Invid} link relationships be
 * symmetrical.  Previously, establishing a one-way link from object
 * 'A' to object 'B' involved editing both A and B, and placing a
 * reverse link in B that pointed to A.</p>
 *
 * <p>This worked, but it meant that it was impossible for multiple
 * users to be concurrently editing objects to establish or remove
 * links to object B.  This was especially troublesome for Owner Group
 * objects, as it meant that only one user at a time could be creating
 * or deleting objects in a given Owner Group.</p>
 *
 * <p>With the introduction of the DBDeletionManager, we eliminate the
 * need to be constantly editing targets of asymmetric links.</p>
 *
 * <p>Instead, when an object 'A' is checked out for editing that
 * contains asymmetric link to other objects, those objects are locked
 * (using the DBDeletionManager addSessionInvids() method) so that
 * they cannot be deleted.  The session editing object A can remove
 * the links to any of the linked objects if it wants, but if it
 * doesn't, it can be confident that the targeted objects will still
 * be there at the time the transaction commits.</p>
 *
 * <p>Similarly, if object 'A' does not initially contain a link to
 * 'B', the act of creating an asymmetric link from A to B will cause
 * object B to be deletion locked at that time.  See the source of the
 * arlut.csd.ganymede.server.InvidDBField bind() method for
 * details.</p>
 *
 * <p>In either of the above scenarios, deletion locks established by
 * a given {@link arlut.csd.ganymede.server.DBSession} are released
 * when the session's transactions are committed or aborted in the
 * session's {@link arlut.csd.ganymede.server.DBEditSet}.</p>
 */

public final class DBDeletionManager {

  /**
   * <p>DBSession objects will appear as keys in this Map when those
   * sessions have locked Invids in the database against deletion.</p>
   *
   * <p>The Set of Invids mapped from the DBSession is the set of Invids
   * that that DBSession has locked.</p>
   */

  private static Map<DBSession, Set<Invid>> sessions = new HashMap<DBSession, Set<Invid>>();

  /**
   * Invids will appear as keys in this Map when the DBObjects
   * corresponding to those Invids have been delete locked by
   * DBSessions in the server.  The Set of DBSession objects is the
   * list of DBSessions that have locked the Invid.
   */

  private static Map<Invid, Set<DBSession>> invids = new HashMap<Invid, Set<DBSession>>();

  /* -- */

  /**
   * This method is used by the Invid binding logic to attempt to
   * block an object from being deleted.  If this method returns true,
   * the object will be forbidden from being deleted until such time
   * as the DBSession that asserted the lock clears it.
   */

  public static synchronized boolean deleteLockObject(DBObject obj, DBSession session)
  {
    DBEditObject eObj;

    /* -- */

    eObj = obj.shadowObject;

    // N.B. the obj that we get as a parameter may well be a
    // DBEditObject already if this session has checked it out for
    // editing, but in that case eObj will be null, as DBEditObjects
    // always have no shadowObject, and the following check won't
    // complain, which is appropriate, since we are only interested in
    // blocking out other sessions

    if (eObj != null &&
        (eObj.getStatus() == DBEditObject.DROPPING ||
         eObj.getStatus() == DBEditObject.DELETING))
      {
        // just in case someone went to some effort to get around
        // DBSession.viewDBObject()'s normal retrieval of an
        // in-session DBEditObject, double check the session here,
        // only reject if it's another session that has it marked for
        // deletion

        if (eObj.getDBSession() != session)
          {
            return false;
          }
      }

    // if this session already marked the object in question for
    // deletion, all the rest of this method will wind up not actually
    // doing anything except wasting a bit of time

    Invid objInvid = obj.getInvid();

    // invidList is a list of invid's that this session
    // has locked

    Set<Invid> invidSet = sessions.get(session);

    if (invidSet == null)
      {
        invidSet = new HashSet<Invid>();
        sessions.put(session, invidSet);
      }

    invidSet.add(objInvid);

    // sessionSet is a Set of DBSession objects that have locked this
    // invid

    Set<DBSession> sessionSet = invids.get(objInvid);

    if (sessionSet == null)
      {
        sessionSet = new HashSet<DBSession>();
        invids.put(objInvid, sessionSet);
      }

    sessionSet.add(session);

    return true;
  }

  /**
   * This method is used by the DBSession deleteDBObject() method to
   * safely convert a DBEditObject's status to a deletion state.  This
   * method returns true if the object was able to be converted to a
   * deletion state, or false if the object has been deletion locked
   * by another DBSession.
   */

  public static synchronized boolean setDeleteStatus(DBEditObject obj, DBSession session)
  {
    Invid objInvid = obj.getInvid();

    // we need to see if a session has expressed a desire to establish
    // an asymmetrical link to obj in another transaction, using
    // deleteLockObject().. if they have, we can't delete.  Otherwise,
    // go ahead and set the deletion status, which will prevent
    // deleteLockObject() from messing with us

    Set<DBSession> sessionSet = invids.get(objInvid);

    if (sessionSet != null)
      {
        // if more than one session is associated with this invid, we
        // can't allow deletion

        if (sessionSet.size() > 1)
          {
            return false;
          }

        // if that one session isn't the one requesting deletion privs,
        // we can't allow it either

        if (!sessionSet.contains(session))
          {
            return false;
          }
      }

    // okay, go ahead and flag the object for deletion

    if (obj.getStatus() == DBEditObject.CREATING)
      {
        obj.setStatus(DBEditObject.DROPPING);
      }
    else if (obj.getStatus() == DBEditObject.EDITING)
      {
        obj.setStatus(DBEditObject.DELETING);
      }

    return true;
  }

  /**
   * When a DBSession releases or commits a transaction, this method
   * is called to clear any deletion locks asserted by it.
   */

  public static synchronized void releaseSession(DBSession session)
  {
    Set<Invid> invidSet = sessions.get(session);

    if (invidSet == null)
      {
        return;
      }

    for (Invid invid: invidSet)
      {
        Set<DBSession> sessionSet = invids.get(invid);

        if (sessionSet.size() == 1)
          {
            invids.remove(invid);
          }
        else
          {
            sessionSet.remove(session);
          }
      }

    sessions.remove(session);
  }

  /**
   * This method deletion-locks a vector of invids, returning false
   * without changes if the deletion-locks could not all be performed.
   */

  public static synchronized boolean addSessionInvids(DBSession session, Set<Invid> invidSet)
  {
    DBObject obj;
    DBEditObject eObj;

    /* -- */

    if (invidSet == null || invidSet.size() == 0)
      {
        return true;
      }

    Set<Invid> currentSet = sessions.get(session);
    Set<Invid> toAdd = new HashSet<Invid>(invidSet);

    if (currentSet != null)
      {
        toAdd.removeAll(currentSet);
      }

    for (Invid invid: toAdd)
      {
        obj = session.viewDBObject(invid);
        eObj = obj.shadowObject;

        // N.B. the obj that we get from session.viewDBObject() may
        // well be a DBEditObject already if this session has checked
        // it out for editing, but in that case eObj will be null, as
        // DBEditObjects always have no shadowObject, and the
        // following check won't complain, which is appropriate, since
        // we are only interested in blocking out other sessions

        if (eObj != null &&
            (eObj.getStatus() == DBEditObject.DROPPING ||
             eObj.getStatus() == DBEditObject.DELETING))
          {
            return false;
          }
      }

    // Okay, we know that we can safely delete lock all of the invids
    // in toAdd.  Let's proceed.

    for (Invid invid: toAdd)
      {
        obj = session.viewDBObject(invid);

        if (!deleteLockObject(obj, session))
          {
            // eek, no way.  scream and whine!

            throw new Error("Error, addSessionInvids encountered an internal inconsistency");
          }
      }

    return true;
  }

  /**
   * This method returns a snapshot of what Invids are locked by a
   * given session at the time the method is called.  It is used by
   * the DBEditSet checkpoint code to allow a transaction to
   * checkpoint the objects that the transaction had delete-locked at
   * a moment in time.  With revertSessionCheckpoint(), below, a
   * transaction can provisionally carry out some object linkages
   * which delete-locks objects, then undo those additional locks if
   * the transaction is rolled back.
   */

  public static synchronized Set<Invid> getSessionCheckpoint(DBSession session)
  {
    Set<Invid> invidSet = sessions.get(session);

    if (invidSet == null)
      {
        return new HashSet<Invid>();
      }

    return new HashSet<Invid>(invidSet);
  }

  /**
   * This method rolls back the invids delete-locked by a session to
   * an earlier state.  revertSessionCheckpoint() cannot cause more
   * invids to be delete-locked.. the only effect it can have is to
   * undelete-lock invids that were provisionally delete-locked by
   * session between a checkpoint and rollback.
   */

  public static synchronized void revertSessionCheckpoint(DBSession session, Set<Invid> invidSet)
  {
    Set<Invid> currentSet = sessions.get(session);

    if (currentSet == null)
      {
        // we won't necessarily have a Set<Invid> for the session
        // unless an object has been delete-locked in that session

        return;
      }

    Set<Invid> badItems = new HashSet<Invid>(invidSet); // copy
    badItems.removeAll(currentSet);

    Set<Invid> toRemove = new HashSet<Invid>(currentSet); // copy
    toRemove.removeAll(invidSet);

    if (badItems.size() != 0)
      {
        throw new IllegalArgumentException("Error, DBDeletionManager.revertSessionCheckpoint() " +
                                           "fed invids that weren't deletion-locked");
      }

    for (Invid invid: toRemove)
      {
        Set<DBSession> sessionSet = invids.get(invid);

        if (sessionSet.size() == 1)
          {
            invids.remove(invid);
          }
        else
          {
            sessionSet.remove(session);
          }
      }

    sessions.put(session, invidSet);
  }
}
