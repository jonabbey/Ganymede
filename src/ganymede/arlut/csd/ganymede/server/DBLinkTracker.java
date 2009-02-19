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
import java.util.Enumeration;
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
 * This class is responsible for tracking forward asymmetric links
 * from sources to targets in the Ganymede data store.
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

  static final boolean debug = true;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBLinkTracker");

  /* --- */

  /**
   * persistentLinks stores sets of source object invids that have
   * forward asymmetric links to the target invid keys.  When looking
   * up an invid in the persistentLinks map, a set of invids with
   * forward asymmetric links pointing to the invid key is returned.
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
   * This method adds tracking for a virtual back pointer to source
   * from target, in keeping with a forward asymmetric link from
   * source to target.
   *
   * @return true if we were not previously tracking a virtual back
   * pointer from target to source, false if we previously were.
   */

  public synchronized boolean linkObject(DBSession session, Invid target, Invid source)
  {
    if (target == null || source == null)
      {
	throw new NullPointerException();
      }

    if (debug)
      {
	System.err.println("DBLinkTracker.linkObject(" + session + ", " + target + ", " + source + ")");
      }

    return getElement(session).linkObject(target, source);
  }

  /**
   * This method makes a note that the source object is no longer
   * pointing to the target object with a forward asymmetric link.
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

    if (debug)
      {
	System.err.println("DBLinkTracker.unlinkObject(" + session + ", " + target + ", " + source + ")");
      }

    return getElement(session).unlinkObject(target, source);
  }

  /**
   * This method records that object source has forward asymmetric
   * links to all object invids in the targets set.
   */

  public synchronized void registerObject(DBSession session, Set<Invid> targets, Invid source)
  {
    if (targets == null || source == null)
      {
	throw new NullPointerException();
      }

    if (debug)
      {
	System.err.println("DBLinkTracker.registerObject(" + session + ", " + targets + ", " + source + ")");
      }

    getElement(session).registerObject(targets, source);
  }

  /**
   * This method removes the tracking for all forward asymmetric links
   * registered from source to the Invids in the targets Set.
   */

  public synchronized void unregisterObject(DBSession session, Set<Invid> targets, Invid source)
  {
    if (targets == null || source == null)
      {
	throw new NullPointerException();
      }

    if (debug)
      {
	System.err.println("DBLinkTracker.unregisterObject(" + session + ", " + targets + ", " + source + ")");
      }

    getElement(session).unregisterObject(targets, source);
  }

  /**
   * This method returns a Set of all Invids that we are tracking as
   * having forward asymmetric links to the target Invid in the
   * current data we are tracking for session, or in the persistent
   * data store if session is null.
   */

  public synchronized Set<Invid> getForwardLinkSources(DBSession session, Invid target)
  {
    if (target == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).getForwardLinkSources(target);
  }

  /**
   * Returns a string describing the objects that we are tracking as
   * having forward asymmetric links to the target Invid in the
   * current data we are tracking for session, or in the persistent
   * data store if session is null.
   */

  public synchronized String forwardAsymmetricLinksToString(DBSession session, Invid target)
  {
    if (target == null)
      {
	throw new NullPointerException();
      }

    return getElement(session).forwardAsymmetricLinksToString(target);
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

  public synchronized void syncObjTargets(DBEditObject obj)
  {
    Set<Invid> addedTargets;
    Set<Invid> removedTargets;

    Invid ourInvid = obj.getInvid();
    Invid testInvid;

    Set<Invid> linkSources;

    DBObject original;

    /* -- */

    original = obj.getOriginal();

    addedTargets = obj.getASymmetricTargets();

    if (original == null)
      {
	removedTargets = new HashSet<Invid>();
      }
    else
      {
	Set<Invid> oldTargets = original.getASymmetricTargets();

	removedTargets = new HashSet<Invid>(oldTargets);
	removedTargets.removeAll(addedTargets);

	addedTargets.removeAll(oldTargets);
      }

    for (Invid target: removedTargets)
      {
	if (!unlinkObject(obj.getSession(), target, ourInvid))
	  {
	    System.err.println("DBEditObject.syncObjTargets(): couldn't find and remove proper backlink for: " +
			       target);
	  }
      }

    for (Invid target: addedTargets)
      {
	linkObject(obj.getSession(), target, ourInvid);
      }
  }

  /**
   * Returns true if a forward asymmetric link is registered from
   * source to target.  That is, if we have registered a virtual
   * back pointer from target to source.
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
   *
   * This method should always be called from a context that has a
   * DBDumpLock established on the entirety of the server.
   *
   * Note that this method will be *extremely* expensive to run, and
   * will effectively shut down the server due to its synchronization
   * and dump lock!
   */

  public synchronized boolean checkInvids(DBSession session)
  {
    DBLinkTrackerElement realLinks = new DBLinkTrackerElement();

    /* -- */

    // "Testing Ganymede.db.backPointers structure for validity"
    Ganymede.debug(ts.l("checkInvids.backpointers"));

    // "Ganymede persistentLinks hash structure tracking {0} invid''s."
    Ganymede.debug(ts.l("checkInvids.backpointers2", Integer.valueOf(persistentLinks.targetToSourcesMap.size())));

    for (DBObjectBase base: Ganymede.db.objectBases.values())
      {
	for (DBObject object: base.getObjectTable())
	  {
	    realLinks.registerObject(object.getASymmetricTargets(), object.getInvid());
	  }
      }

    if (realLinks.targetToSourcesMap.equals(persistentLinks.targetToSourcesMap))
      {
	return true;
      }

    for (Invid target: realLinks.targetToSourcesMap.keySet())
      {
	Set<Invid> realSources = realLinks.targetToSourcesMap.get(target);

	if (!persistentLinks.targetToSourcesMap.containsKey(target))
	  {
	    // "** DBLinkTracker.checkInvids() target object {0} is not listed in the DBLinkTracker structures."
	    Ganymede.debug(ts.l("checkInvids.missingTarget", describe(null, target)));

	    continue;
	  }

	Set<Invid> trackedSources = persistentLinks.targetToSourcesMap.get(target);

	Set<Invid> extraReal = new HashSet<Invid>(realSources);
	extraReal.removeAll(trackedSources);

	Set<Invid> extraTracked = new HashSet<Invid>(trackedSources);
	extraTracked.removeAll(realSources);

	if (extraReal.size() > 0)
	  {
	    for (Invid extraSource: extraReal)
	      {
		// "** DBLinkTracker.checkInvids(): DBObject {0} has a forward asymmetric link to invid {1} that is not present in the DBLinkTracker structures!"
		Ganymede.debug(ts.l("checkInvids.extraLink", extraSource, target));
	      }
	  }

	if (extraTracked.size() > 0)
	  {
	    for (Invid missingSource: extraTracked)
	      {
		// "** DBLinkTracker.checkInvids(): DBObject {0} is lacking a forward asymmetric link to invid {1} that the DBLinkTracker thinks should be there!"
		Ganymede.debug(ts.l("checkInvids.missingLink", missingSource, target));
	      }
	  }
      }

    return false;
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
    DBLinkTrackerElement parent;

    Map<Invid, Set<Invid>> targetToSourcesMap;

    DBSession session;

    /* -- */

    public DBLinkTrackerElement()
    {
      parent = null;
      session = null;
      targetToSourcesMap = new HashMap<Invid, Set<Invid>>();
    }

    public DBLinkTrackerElement(DBSession session)
    {
      this();

      this.session = session;
    }

    public DBLinkTrackerElement(DBLinkTrackerElement parent)
    {
      this();

      this.parent = parent;
      this.session = parent.session;
    }

    public synchronized void transferFrom(DBLinkTrackerElement otherElement)
    {
      // We're copying all sets of link targets from otherElement.
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
	  this.targetToSourcesMap.putAll(otherElement.targetToSourcesMap);
	}
      else
	{
	  // source has forward asymmetric links, we're tracking the
	  // reverse pointers from the targets

	  for (Invid target: otherElement.targetToSourcesMap.keySet())
	    {
	      Set<Invid> sources = otherElement.targetToSourcesMap.get(target);

	      if (sources.size() == 0)
		{
		  this.targetToSourcesMap.remove(target);
		}
	      else
		{
		  this.targetToSourcesMap.put(target, sources);
		}
	    }
	}
    }

    /**
     * This method adds tracking for a virtual back pointer to source
     * from target, in keeping with a forward asymmetric link from
     * source to target.
     *
     * @return true if we were not previously tracking a virtual back
     * pointer from target to source, false if we previously were.
     */

    public synchronized boolean linkObject(Invid target, Invid source)
    {
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      return getForwardLinkSources(target).add(source);
    }

    /**
     * This method makes a note that the source object is no longer
     * pointing to the target object with a forward asymmetric link.
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

      Set<Invid> sources = getForwardLinkSources(target);

      boolean result = sources.remove(source);

      if (parent == null && sources.size() == 0)
	{
	  targetToSourcesMap.remove(target);
	}

      return result;
    }

    /**
     * This method records that object source has forward asymmetric
     * links to all object invids in the targets set.
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
     * This method removes the virtual reverse pointers (target->source)
     * for all forward asymmetric links registered from source to the
     * Invids in the targets Set.
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
     * Returns true if a forward asymmetric link is registered from
     * source to target.  That is, if we have registered a virtual
     * back pointer from target to source.
     */

    public synchronized boolean linkExists(Invid source, Invid target)
    {
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      return getForwardLinkSources(target).contains(source);
    }

    /**
     * Returns a string describing the objects that have virtual reverse
     * pointers corresponding to forward asymmetric links from source.
     */

    public synchronized String forwardAsymmetricLinksToString(Invid target)
    {
      if (target == null)
	{
	  throw new NullPointerException();
	}

      StringBuilder builder = new StringBuilder();

      builder.append("-> Tracked forward links pointing at ");
      builder.append(describe(session, target));
      builder.append("\n");

      Set<Invid> sources = targetToSourcesMap.get(target);

      if (sources == null && parent != null)
	{
	  DBLinkTrackerElement p = parent;

	  while (sources == null && p != null)
	    {
	      sources = p.targetToSourcesMap.get(target);

	      p = p.parent;
	    }
	}

      if (sources == null)
	{
	  builder.append("-> ** empty ** \n");
	}
      else
	{
	  for (Invid source: sources)
	    {
	      builder.append("<--- ");
	      builder.append(describe(session, source));
	      builder.append("\n");
	    }
	}

      return builder.toString();
    }

    /**
     * This method returns a Set of all Invids that point to the
     * target object through forward asymmetric links in this
     * DBLinkTrackerElement's context.
     *
     * If we don't have a set in our own element, we'll either copy
     * the set for the target from our nearest parent that has a set
     * for the target, or else we'll create a new empty set.. in this
     * way, we provide a Set of forward pointer Invids that is
     * specific to this element in our session/checkpoint stack.
     */

    private Set<Invid> getForwardLinkSources(Invid target)
    {
      Set<Invid> sources = targetToSourcesMap.get(target);

      if (sources == null)
	{
	  DBLinkTrackerElement p = parent;

	  while (p != null)
	    {
	      if (p.targetToSourcesMap.containsKey(target))
		{
		  sources = new HashSet<Invid>(p.targetToSourcesMap.get(target));

		  break;
		}

	      p = p.parent;
	    }

	  if (p == null)
	    {
	      sources = new HashSet<Invid>();
	    }

	  targetToSourcesMap.put(target, sources);
	}

      return sources;
    }
  }
}
