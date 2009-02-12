/*
   GASH 2

   DBLinkTracker.java

   The GANYMEDE object storage system.

   Created: 9 February 2009
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2009
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.Invid;

import arlut.csd.Util.NamedStack;
import arlut.csd.Util.TranslationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   DBLinkTracker

------------------------------------------------------------------------------*/

/**
 * This class is responsible for tracking the objects that point
 * asymmetrically to DBObjects in the Ganymede persistent datastore.
 *
 * That is, this class is reponsible for remembering all objects that
 * point to each object in the server's datastore, with the exception
 * of objects which maintain forward and reverse pointers in paired
 * data fields.
 *
 * This class makes it possible to efficiently delete objects from the
 * Ganymede datastore without having to scan the entire datastore in
 * search of objects that point to the to-be-deleted object.
 *
 * Once the Invids for objects which link to a to-be-deleted object
 * are retrieved, the objects containing these Invids can (and will
 * be) edited to remove these links.
 */

public class DBLinkTracker {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBLinkTracker");

  /* --- */

  /**
   * persistentLinks stores sets of object invids that point to the
   * invid keys.  When looking up an invid in the persistentLinks map,
   * a set of invids with asymmetric links pointing to the invid key
   * is returned.
   */

  private Map<Invid, Set<Invid>> persistentLinks;

  /**
   * sessionOverlays tracks modifications that are accruing in active
   * sessions on the DBStore.  These modifications are private to the
   * session unless/until the session is committed, at which time the
   * overlays are folded into the persistentLinks structure.
   *
   * The DBLinkTrackerSession objects kept in this Map for each active
   * DBSession include support for handling checkpoints and rollbacks
   * across the duration of the DBSessions.
   */

  private Map<DBSession, DBLinkTrackerSession> sessionOverlays;

  /* -- */

  public DBLinkTracker()
  {
    persistentLinks = new HashMap<Invid, Set<Invid>>(1000);
    sessionOverlays = new HashMap<DBSession, DBLinkTrackerSession>(23);
  }

  /**
   * This method records the fact that the source Invid is pointing to
   * the target Invid, such that if the object pointed to by the
   * target Invid is deleted, the object pointed to by the source
   * Invid will need to be edited to remove the link.
   *
   * @return true if the source previously was not registered as
   * pointing to target, false if it was already registered.
   */

  public synchronized boolean linkObject(Invid target, Invid source)
  {
    Set<Invid> linkSources = persistentLinks.get(target);

    if (linkSources == null)
      {
	linkSources = new HashSet<Invid>();
	persistentLinks.put(target, linkSources);
      }

    return linkSources.add(source);
  }

  /**
   * This method makes a note that the source object is no longer
   * pointing to the target object.
   *
   * @return true if the source previously was registered as pointing
   * to target, false otherwise.
   */

  public synchronized boolean unlinkObject(Invid target, Invid source)
  {
    Set<Invid> linkSources = persistentLinks.get(target);

    if (linkSources == null)
      {
	return false;
      }

    boolean result = linkSources.remove(source);

    if (linkSources.size() == 0)
      {
	persistentLinks.remove(target);
      }

    return result;
  }

  /**
   * This method records that object source points to all object
   * invids in the targets set.
   */

  public synchronized void registerObject(Set<Invid> targets, Invid source)
  {
    for (Invid target: targets)
      {
	linkObject(target, source);
      }
  }

  /**
   * This method removes all links registered from source to the
   * various targets.
   */

  public synchronized void unregisterObject(Set<Invid> targets, Invid source)
  {
    for (Set<Invid> innerSet: persistentLinks.values())
      {
	innerSet.remove(source);
      }
  }

  /**
   * This method removes all links registered from all sources to
   * target.
   */

  public synchronized void unlinkTarget(Invid target)
  {
    persistentLinks.remove(target);
  }

  /**
   * This method returns a List of all Invids pointing to the target
   * object in the server's persistent data store.
   */

  public synchronized List<Invid> getLinkSources(Invid target)
  {
    List<Invid> sources = new ArrayList<Invid>();
    Set<Invid> linkSources = persistentLinks.get(target);

    if (linkSources != null)
      {
	sources.addAll(linkSources);
      }

    return sources;
  }

  public synchronized String linkSourcesToString(Invid target)
  {
    StringBuilder builder = new StringBuilder();

    builder.append("-> Asymmetric links to ");
    builder.append(describe(target));
    builder.append("\n");

    Set<Invid> linkSources = persistentLinks.get(target);

    if (linkSources == null)
      {
	builder.append("-> ** empty ** \n");
      }
    else
      {
	for (Invid source: linkSources)
	  {
	    builder.append("<--- ");
	    builder.append(describe(source));
	    builder.append("\n");
	  }
      }

    return builder.toString();
  }

  /**
   * This method updates the data structures held by this
   * DBLinkTracker object in keeping with the changes being made by
   * the DBEditObject obj.
   *
   * <p>This method is executed towards the end of a transaction
   * commit, and compares the current state of this object with its
   * original state, and makes the appropriate changes to the internal
   * state of this DBLinkTracker.</p>
   *
   * <p>The purpose of this is to support the decoupling of an object
   * from its backlinks, so that objects can be asymmetrically linked
   * to an object without having to check that object out for editing.</p>
   */

  public synchronized void syncObjBackPointers(DBEditObject obj)
  {
    Set<Invid> addedBackPointers;
    Set<Invid> removedBackPointers;

    Invid ourInvid = obj.getInvid();
    Invid testInvid;

    Set<Invid> linkSources;

    DBObject original;

    /* -- */

    original = obj.getOriginal();

    addedBackPointers = obj.getASymmetricTargets();

    if (original == null)
      {
	removedBackPointers = new HashSet<Invid>();
      }
    else
      {
	Set<Invid> oldBackPointers = original.getASymmetricTargets();

	removedBackPointers = new HashSet<Invid>(oldBackPointers);
	removedBackPointers.removeAll(addedBackPointers);

	addedBackPointers.removeAll(oldBackPointers);
      }

    for (Invid target: removedBackPointers)
      {
	if (!unlinkObject(target, ourInvid))
	  {
	    System.err.println("DBEditObject.syncObjBackPointers(): couldn't find and remove proper backlink for: " +
			       target);
	  }
      }

    for (Invid target: addedBackPointers)
      {
	linkObject(target, ourInvid);
      }
  }

  /**
   * Returns true if an asymmetric link is registered from source to
   * target.
   */

  public synchronized boolean linkExists(Invid source, Invid target)
  {
    Set<Invid> linkSources;

    linkSources = persistentLinks.get(target);

    if (linkSources == null)
      {
	return false;
      }

    return linkSources.contains(source);
  }

  /**
   * This method is called from GanymedeServer.checkInvids() to
   * perform a validation of the DBLinkTracker structures for the sake
   * of the 'check invid integrity' debug option in the Ganymede Admin
   * console.
   */

  public synchronized boolean checkInvids(DBSession session)
  {
    boolean ok = true;

    // "Testing Ganymede persistentLinks hash structure for validity"

    Ganymede.debug(ts.l("checkInvids.backpointers"));

    // "Ganymede persistentLinks hash structure tracking {0} invid''s."

    Ganymede.debug(ts.l("checkInvids.backpointers2", Integer.valueOf(persistentLinks.size())));

    for (Invid key: persistentLinks.keySet())
      {
	for (Invid backTarget: persistentLinks.get(key))
	  {
	    if (session.viewDBObject(backTarget) == null)
	      {
		ok = false;

		// "***Backpointers hash for object {0} has an invid pointing to a non-existent object: {1}"

		Ganymede.debug(ts.l("checkInvids.aha", 
				    session.getGSession().describe(key),
				    backTarget.toString()));
	      }
	  }
      }

    return ok;
  }

  public synchronized void debugDump()
  {
    for (Invid objInvid: persistentLinks.keySet())
      {
	System.err.println("Object: " + describe(objInvid));

	for (Invid pointedToBy: persistentLinks.get(objInvid))
	  {
	    System.err.println("\t" + describe(pointedToBy));
	  }

	System.err.println("\n");
      }
  }

  private String describe(Invid invid)
  {
    // very little synchronization involved here aside from
    // backpointers in the debugDump() method above, so we're not very
    // worried about deadlocks here

    return Ganymede.internalSession.describe(invid);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                            DBLinkTrackerSession

------------------------------------------------------------------------------*/

/**
 * Helper class associated with {@link arlut.csd.ganymede.server.DBLinkTracker}.
 *
 * DBLinkTrackerSession is responsible for tracking the view a
 * particular DBSession has of the Ganymede DBStore's asymmetric
 * links.
 *
 * In the Ganymede DBStore, each DBSession has its own view of the
 * underlying data store, with private visibility of changes that have
 * been made in the session's transaction, but not yet committed into
 * the underlying object store.
 *
 * DBLinkTrackerSession maintains the per-session overlay on top of
 * the persisted back link structure, and supports a {@link
 * arlut.csd.Util.NamedStack} of {@link
 * arlut.csd.ganymede.server.DBLinkTrackerElement
 * DBLinkTrackerElements} to allow checkpoint, popCheckpoint, and
 * rollback operations to properly interact with the DBLinkTracker
 * system.
 */

class DBLinkTrackerSession
{
  /**
   * The DBSession object that this DBLinkTrackerSession is tracking overlays for.
   */

  private DBSession session;

  /**
   * Named stack of DBLinkTrackerElement objects, tracking the
   * overlays on the asymmetric link structures of DBLinkTracker as
   * checkpoints are established and popped/rolledback for the
   * DBSession.
   */

  private NamedStack<DBLinkTrackerElement> elements;

  /* -- */

  public DBLinkTrackerSession(DBSession session)
  {
    this.session = session;
    elements = new NamedStack<DBLinkTrackerElement>();

    DBLinkTrackerElement defaultElement = new DBLinkTrackerElement();

    elements.push(DBLinkTrackerSession.class.getName(), defaultElement);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                            DBLinkTrackerElement

------------------------------------------------------------------------------*/

/**
 * Helper class associated with DBLinkTracker.
 *
 * DBLinkTrackerElement is responsible for recording per-DBSession
 * overlays onto the DBLinkTracker's main persistentLinks structure,
 * representing additions or subtractions from the persistentLinks
 * that affect a particular DBSession.
 *
 * DBLinkTrackerSession supports maintaining a checkpoint NamedStack
 * to allow checkpoint, popCheckpoint, and rollback operations to
 * properly interact with the DBLinkTracker system.
 */

class DBLinkTrackerElement
{
  public DBLinkTrackerElement()
  {
  }
}