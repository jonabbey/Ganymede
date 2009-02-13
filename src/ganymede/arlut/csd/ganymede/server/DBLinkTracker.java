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

  private DBLinkTrackerElement persistentLinks;

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
    persistentLinks = new DBLinkTrackerElement();
    sessionOverlays = new HashMap<DBSession, DBLinkTrackerSession>(23);
  }

  /**
   * Adds a new checkpoint to session's link tracker data.
   */

  public synchronized void checkpoint(DBSession session, String ckp_label)
  {
    if (session == null)
      {
	throw new NullPointerException();
      }

    DBLinkTrackerSession tracker = getSession(session);

    tracker.checkpoint(ckp_label);
  } 

  /**
   * Removes a checkpoint from session's link tracker data, reverting
   * the link tracker data for the session to the state it was in
   * prior to the checkpoint being established.
   */

  public synchronized void rollback(DBSession session, String ckp_label)
  {
    if (session == null)
      {
	throw new NullPointerException();
      }

    DBLinkTrackerSession tracker = getSession(session);

    tracker.rollback(ckp_label);
  }

  /**
   * Removes a checkpoint from session's link tracker data, without
   * reverting the link tracker data for the session.
   */

  public synchronized void popCheckpoint(DBSession session, String ckp_label)
  {
    if (session == null)
      {
	throw new NullPointerException();
      }

    DBLinkTrackerSession tracker = getSession(session);

    tracker.consolidate(ckp_label);
  }

  /**
   * Integrates changes to link tracker data made by the session into
   * the link tracker data for the persistent store.
   */

  public synchronized void commit(DBSession session)
  {
    if (session == null)
      {
	throw new NullPointerException();
      }

    DBLinkTrackerSession tracker = getSession(session);

    persistentLinks.transferFrom(tracker.getCurrentElement());

    sessionOverlays.remove(session);
  }

  /**
   * Erases any memory of link tracker data changes made by the
   * session, as if they had never happened.
   */

  public synchronized void abort(DBSession session)
  {
    if (session == null)
      {
	throw new NullPointerException();
      }

    sessionOverlays.remove(session);
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

  public synchronized boolean linkObject(DBSession session, Invid target, Invid source)
  {
    if (target == null || source == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).linkObject(target, source);
  }

  /**
   * This method makes a note that the source object is no longer
   * pointing to the target object.
   *
   * @return true if the source previously was registered as pointing
   * to target, false otherwise.
   */

  public synchronized boolean unlinkObject(DBSession session, Invid target, Invid source)
  {
    if (target == null || source == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).unlinkObject(target, source);
  }

  /**
   * This method records that object source points to all object
   * invids in the targets set.
   */

  public synchronized void registerObject(DBSession session, Set<Invid> targets, Invid source)
  {
    if (targets == null || source == null)
      {
	throw new NullPointerException();
      }

    getElement(session).registerObject(targets, source);
  }

  /**
   * This method removes all links registered from source to the
   * various targets.
   */

  public synchronized void unregisterObject(DBSession session, Set<Invid> targets, Invid source)
  {
    if (targets == null || source == null)
      {
	throw new NullPointerException();
      }

    getElement(session).unregisterObject(targets, source);
  }

  /**
   * This method removes all links registered from all sources to
   * target.
   */

  public synchronized void unlinkTarget(DBSession session, Invid target)
  {
    if (target == null)
      {
	throw new NullPointerException();
      }

    getElement(session).unlinkTarget(target);
  }

  /**
   * This method returns a List of all Invids pointing to the target
   * object in the server's persistent data store.
   */

  public synchronized Set<Invid> getLinkSources(DBSession session, Invid target)
  {
    if (target == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).getLinkSources(target);
  }

  /**
   * Returns a string describing the objects that asymmetrically point
   * to target.
   */

  public synchronized String linkSourcesToString(DBSession session, Invid target)
  {
    if (target == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).linkSourcesToString(session, target);
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
	if (!unlinkObject(obj.getSession(), target, ourInvid))
	  {
	    System.err.println("DBEditObject.syncObjBackPointers(): couldn't find and remove proper backlink for: " +
			       target);
	  }
      }

    for (Invid target: addedBackPointers)
      {
	linkObject(obj.getSession(), target, ourInvid);
      }
  }

  /**
   * Returns true if an asymmetric link is registered from source to
   * target.
   */

  public synchronized boolean linkExists(DBSession session, Invid source, Invid target)
  {
    if (target == null || source == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).linkExists(source, target);
  }

  /**
   * This method is called from GanymedeServer.checkInvids() to
   * perform a validation of the persistent DBLinkTracker structures
   * for the sake of the 'check invid integrity' debug option in the
   * Ganymede Admin console.
   */

  public synchronized boolean checkInvids(DBSession session)
  {
    boolean ok = true;

    // "Testing Ganymede.db.backPointers structure for validity"

    Ganymede.debug(ts.l("checkInvids.backpointers"));

    // "Ganymede persistentLinks hash structure tracking {0} invid''s."

    Ganymede.debug(ts.l("checkInvids.backpointers2", Integer.valueOf(persistentLinks.overlay.size())));

    for (Invid key: persistentLinks.overlay.keySet())
      {
	for (Invid backTarget: persistentLinks.overlay.get(key))
	  {
	    if (session.viewDBObject(backTarget) == null)
	      {
		ok = false;

		// "***Backpointers hash for object {0} has an invid pointing to a non-existent object: {1}"

		if (session != null)
		  {
		    Ganymede.debug(ts.l("checkInvids.aha", 
					describe(session, key),
					backTarget.toString()));
		  }
	      }
	  }
      }

    return ok;
  }

  private String describe(DBSession session, Invid invid)
  {
    // very little synchronization involved here aside from
    // backpointers in the debugDump() method above, so we're not very
    // worried about deadlocks here

    if (session != null)
      {
	return session.describe(invid);
      }
    else
      {
	return Ganymede.internalSession.describe(invid);
      }
  }

  /**
   * Returns (after creating and registering , if necessary) a
   * DBLinkTrackerSession for the given session.
   */

  private DBLinkTrackerSession getSession(DBSession session)
  {
    if (sessionOverlays.containsKey(session))
      {
	return sessionOverlays.get(session);
      }
    else
      {
	DBLinkTrackerSession sessionObj = new DBLinkTrackerSession();

	sessionOverlays.put(session, sessionObj);

	return sessionObj;
      }
  }

  /**
   * This method returns the appropriate, active DBLinkTrackerElement
   * for the session being requested.
   *
   * The session parameter may be null, in which case the
   * persistentLinks element is returned.
   *
   * If the session has not been seen before, a new
   * DBLinkTrackerSession and DBLinkTrackerElement will be created to
   * service the session.
   */

  private DBLinkTrackerElement getElement(DBSession session)
  {
    if (session == null)
      {
	return persistentLinks;
      }

    if (sessionOverlays.containsKey(session))
      {
	DBLinkTrackerSession sessionObj = sessionOverlays.get(session);

	return sessionObj.getCurrentElement();
      }
    else
      {
	DBLinkTrackerSession sessionObj = new DBLinkTrackerSession();

	sessionOverlays.put(session, sessionObj);

	return sessionObj.getCurrentElement();
      }
  }


  /*----------------------------------------------------------------------------
                                                                     inner class
                                                            DBLinkTrackerSession

  ----------------------------------------------------------------------------*/

  /**
   * Helper class associated with {@link
   * arlut.csd.ganymede.server.DBLinkTracker}.
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
     * Named stack of DBLinkTrackerElement objects, tracking the
     * overlays on the asymmetric link structures of DBLinkTracker as
     * checkpoints are established and popped/rolledback for the
     * DBSession.
     */

    private NamedStack<DBLinkTrackerElement> elements;

    /* -- */

    public DBLinkTrackerSession()
    {
      elements = new NamedStack<DBLinkTrackerElement>();

      DBLinkTrackerElement defaultElement = new DBLinkTrackerElement(persistentLinks);

      elements.push(DBLinkTrackerSession.class.getName(), defaultElement);
    }

    public synchronized DBLinkTrackerElement getCurrentElement()
    {
      return elements.getTopObject();
    }

    public synchronized void checkpoint(String ckp_key)
    {
      elements.push(ckp_key, new DBLinkTrackerElement(getCurrentElement()));
    }

    public synchronized void rollback(String ckp_key)
    {
      elements.pop(ckp_key);
    }

    public synchronized void consolidate(String ckp_key)
    {
      DBLinkTrackerElement consolidationElement = getCurrentElement();

      if (elements.pop(ckp_key) != null)
	{
	  getCurrentElement().transferFrom(consolidationElement);
	}
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                            DBLinkTrackerElement

  ----------------------------------------------------------------------------*/

  /**
   * Helper class associated with DBLinkTracker.
   *
   * DBLinkTrackerElement is responsible for recording per-DBSession
   * overlays onto the DBLinkTracker's main overlay structure,
   * representing additions or subtractions from the overlay
   * that affect a particular DBSession.
   *
   * DBLinkTrackerSession supports maintaining a checkpoint NamedStack
   * to allow checkpoint, popCheckpoint, and rollback operations to
   * properly interact with the DBLinkTracker system.
   */

  class DBLinkTrackerElement
  {
    private DBLinkTrackerElement parent;
    private Map<Invid, Set<Invid>> overlay;

    /* -- */

    public DBLinkTrackerElement()
    {
      this(null);
    }

    public DBLinkTrackerElement(DBLinkTrackerElement parent)
    {
      this.parent = parent;
      overlay = new HashMap<Invid, Set<Invid>>();
    }

    public synchronized void transferFrom(DBLinkTrackerElement otherElement)
    {
      // We're copying all sets of link sources from otherElement.
      // Some of these sets might be empty, but if we have a parent
      // element, we still need to copy these, as they signify that
      // the otherElement's frame deleted all of the links to that
      // invid.
      //
      // If we are the root node of the DBLinkTrackerElement chain, we
      // can go ahead and just delete those empty sources, because
      // we'll never need to represent the deletion of those links to
      // an underlying element.

      if (parent != null)
	{
	  overlay.putAll(otherElement.overlay);
	}
      else
	{
	  for (Invid key: otherElement.overlay.keySet())
	    {
	      Set<Invid> linkSources = otherElement.overlay.get(key);

	      if (linkSources.size() == 0)
		{
		  overlay.remove(key);
		}
	      else
		{
		  overlay.put(key, linkSources);
		}
	    }
	}
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
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      return getLinkSources(target).add(source);
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
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      Set<Invid> linkSources = getLinkSources(target);

      boolean result = linkSources.remove(source);

      if (parent == null && linkSources.size() == 0)
	{
	  overlay.remove(target);
	}

      return result;
    }

    /**
     * This method records that object source points to all object
     * invids in the targets set.
     */

    public synchronized void registerObject(Set<Invid> targets, Invid source)
    {
      if (targets == null || source == null)
	{
	  throw new NullPointerException();
	}

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
      if (targets == null || source == null)
	{
	  throw new NullPointerException();
	}

      for (Invid target: targets)
	{
	  unlinkObject(target, source);
	}
    }

    /**
     * This method removes all links registered from all sources to
     * target.
     */

    public synchronized void unlinkTarget(Invid target)
    {
      if (target == null)
	{
	  throw new NullPointerException();
	}

      if (parent != null)
	{
	  Set<Invid> linkSources = getLinkSources(target);

	  linkSources.clear();
	}
      else
	{
	  overlay.remove(target);
	}
    }

    /**
     * Returns true if an asymmetric link is registered from source to
     * target.
     */

    public synchronized boolean linkExists(Invid source, Invid target)
    {
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      return getLinkSources(target).contains(source);
    }

    /**
     * Returns a string describing the objects that asymmetrically point
     * to target.
     */

    public synchronized String linkSourcesToString(DBSession session, Invid target)
    {
      if (target == null)
	{
	  throw new NullPointerException();
	}

      StringBuilder builder = new StringBuilder();

      builder.append("-> Asymmetric links to ");
      builder.append(describe(session, target));
      builder.append("\n");

      Set<Invid> linkSources = overlay.get(target);

      if (linkSources == null && parent != null)
	{
	  DBLinkTrackerElement p = parent;

	  while (linkSources == null && p != null)
	    {
	      linkSources = p.overlay.get(target);

	      p = p.parent;
	    }
	}

      if (linkSources == null)
	{
	  builder.append("-> ** empty ** \n");
	}
      else
	{
	  for (Invid source: linkSources)
	    {
	      builder.append("<--- ");
	      builder.append(describe(session, source));
	      builder.append("\n");
	    }
	}

      return builder.toString();
    }

    /**
     * Get a set of invids sources for the given target.
     *
     * If we don't have a set in our own element, we'll either copy
     * the set for the target from our nearest parent that has a set
     * for the target, or else we'll create a new empty set.
     */

    private Set<Invid> getLinkSources(Invid target)
    {
      Set<Invid> linkSources = overlay.get(target);

      if (linkSources == null)
	{
	  DBLinkTrackerElement p = parent;

	  while (p != null)
	    {
	      if (p.overlay.containsKey(target))
		{
		  linkSources = new HashSet<Invid>(p.overlay.get(target));

		  break;
		}

	      p = p.parent;
	    }

	  if (p == null)
	    {
	      linkSources = new HashSet<Invid>();
	    }

	  overlay.put(target, linkSources);
	}

      return linkSources;
    }
  }
}
