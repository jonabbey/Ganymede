/*
   GASH 2

   DBDeletionManager.java

   The GANYMEDE object storage system.

   Created: 23 June 2000
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2000/10/03 06:30:59 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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

import java.util.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBDeletionManager

------------------------------------------------------------------------------*/

/**
 * <p>The DBDeletionManager class is used to handle deletion locking
 * in the Ganymede {@link arlut.csd.ganymede.DBStore DBStore}.</p>
 */

public class DBDeletionManager {

  /**
   * <p>A hashtable mapping {@link arlut.csd.ganymede.DBSession
   * DBSession} objects to a Vector of {@link arlut.csd.ganymede.Invid
   * Invid} objects.</p>
   *
   * <p>DBSession will appear as keys in this hash when those
   * sessions have locked Invids in the database against deletion.
   * The Vector of Invids mapped from the DBSession is the list of
   * Invids that that DBSession has locked.</p>
   */

  private static Hashtable sessions = new Hashtable();

  /**
   * <p>A hashtable mapping {@link arlut.csd.ganymede.Invid Invid}
   * objects to a Vector of {@link arlut.csd.ganymede.DBSession
   * DBSession} objects.</p>
   *
   * <p>Invids will appear as keys in this hash when the DBObjects
   * corresponding to those Invids have been delete locked by
   * DBSessions in the server.  The Vector of DBSession objects
   * is the list of DBSessions that have locked the Invid.</p>
   */

  private static Hashtable invids = new Hashtable();

  /* -- */

  /**
   * <p>This method is used by the Invid binding logic to attempt to block
   * an object from being deleted.  If this method returns true, the object
   * will be forbidden from being deleted until such time as the DBSession
   * that asserted the lock clears it.</p>
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

	if (eObj.getSession() != session)
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

    Vector invidList = (Vector) sessions.get(session);

    if (invidList == null)
      {
	invidList = new Vector();
	sessions.put(session, invidList);
      }

    VectorUtils.unionAdd(invidList, objInvid);

    // sessionList is a list of DBSession objects that
    // have locked this invid

    Vector sessionList = (Vector) invids.get(objInvid);

    if (sessionList == null)
      {
	sessionList = new Vector();
	invids.put(objInvid, sessionList);
      }

    VectorUtils.unionAdd(sessionList, session);

    return true;
  }

  /**
   * <p>This method is used by the DBSession deleteDBObject() method to
   * safely convert a DBEditObject's status to a deletion state.  This
   * method returns true if the object was able to be converted to a
   * deletion state, or false if the object has been deletion locked
   * by another DBSession.</p>
   */

  public static synchronized boolean setDeleteStatus(DBEditObject obj, DBSession session)
  {
    Invid objInvid = obj.getInvid();

    // we need to see if a session has expressed a desire to establish
    // an asymmetrical link to obj in another transaction, using
    // deleteLockObject().. if they have, we can't delete.  Otherwise,
    // go ahead and set the deletion status, which will prevent
    // deleteLockObject() from messing with us

    Vector sessionList = (Vector) invids.get(objInvid);

    if (sessionList != null)
      {
	// if more than one session is associated with this invid, we
	// can't allow deletion

	if (sessionList.size() > 1)
	  {
	    return false;
	  }

	// if that one session isn't the one requesting deletion privs,
	// we can't allow it either

	if (sessionList.elementAt(0) != session)
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
   * <p>When a DBSession releases or commits a transaction, this method
   * is called to clear any deletion locks asserted by it.</p>
   */

  public static synchronized void releaseSession(DBSession session)
  {
    Vector invidList = (Vector) sessions.get(session);

    if (invidList == null)
      {
	return;
      }

    for (int i = 0; i < invidList.size(); i++)
      {
	Invid invid = (Invid) invidList.elementAt(i);

	Vector sessionList = (Vector) invids.get(invid);
	
	if (sessionList.size() == 1)
	  {
	    invids.remove(invid);
	  }
	else
	  {
	    sessionList.removeElement(session);
	  }
      }

    sessions.remove(session);
  }

  /**
   * <p>This method deletion-locks a vector of invids, returning false
   * without changes if the deletion-locks could not all be
   * performed.</p>
   */

  public static synchronized boolean addSessionInvids(DBSession session, Vector invidList)
  {
    Invid invid;
    DBObject obj;
    DBEditObject eObj;

    /* -- */

    if (invidList == null || invidList.size() == 0)
      {
	return true;
      }

    Vector currentList = (Vector) sessions.get(session);
    Vector toAdd = VectorUtils.difference(invidList, currentList);

    for (int i=0; i < toAdd.size(); i++)
      {
	invid = (Invid) toAdd.elementAt(i);
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

    // okay, we know that all off the objects in toAdd are not yet
    // deletion locked.. go ahead and lock them all

    for (int i=0; i < toAdd.size(); i++)
      {
	invid = (Invid) toAdd.elementAt(i);
	obj = session.viewDBObject(invid);

	if (!deleteLockObject(session.viewDBObject(invid), session))
	  {
	    // eek, no way.  scream and whine!

	    throw new Error("Error, addSessionInvids encountered an internal inconsistency");
	  }
      }

    return true;
  }

  /**
   * <p>This method returns a snapshot of what Invids are locked by a
   * given session at the time the method is called.  It is used by
   * the DBEditSet checkpoint code to allow a transaction to
   * checkpoint the objects that the transaction had delete-locked at
   * a moment in time.  With revertSessionCheckpoint(), below, a
   * transaction can provisionally carry out some object linkages which
   * delete-locks objects, then undo those additional locks if the
   * transaction is rolled back.</p>
   */

  public static synchronized Vector getSessionCheckpoint(DBSession session)
  {
    Vector invidList = (Vector) sessions.get(session);

    if (invidList == null)
      {
	return new Vector();
      }

    return (Vector) invidList.clone();
  }

  /**
   * <p>This method rolls back the invids delete-locked by a session to
   * an earlier state.  revertSessionCheckpoint() cannot cause more
   * invids to be delete-locked.. the only effect it can have is to
   * undelete-lock invids that were provisionally delete-locked by
   * session between a checkpoint and rollback.</p>
   */

  public static synchronized void revertSessionCheckpoint(DBSession session, Vector invidList)
  {
    Vector currentList = (Vector) sessions.get(session);
    Vector toRemove = VectorUtils.difference(currentList, invidList);
    Vector badItems = VectorUtils.difference(invidList, currentList);

    if (badItems.size() != 0)
      {
	throw new IllegalArgumentException("Error, DBDeletionManager.revertSessionCheckpoint() " +
					   "fed invids that weren't deletion-locked");
      }

    for (int i = 0; i < toRemove.size(); i++)
      {
	Invid invid = (Invid) toRemove.elementAt(i);

	Vector sessionList = (Vector) invids.get(invid);
	
	if (sessionList.size() == 1)
	  {
	    invids.remove(invid);
	  }
	else
	  {
	    sessionList.removeElement(session);
	  }
      }

    sessions.put(session, invidList);
  }
}

