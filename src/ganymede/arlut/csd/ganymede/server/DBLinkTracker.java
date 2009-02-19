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

  static final boolean debug = false;

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

  private DBLinkTrackerContext persistentLinks;

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
    persistentLinks = new DBLinkTrackerContext();
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

    persistentLinks.transferFrom(tracker.getCurrentContext());

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

    return getContext(session).linkObject(target, source);
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

    return getContext(session).unlinkObject(target, source);
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

    getContext(session).registerObject(targets, source);
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

    getContext(session).unregisterObject(targets, source);
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

    return getContext(session).getForwardLinkSources(target);
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

    return getContext(session).forwardAsymmetricLinksToString(target);
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

    return getContext(session).linkExists(source, target);
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
    boolean ok = true;
    DBLinkTrackerContext realLinks = new DBLinkTrackerContext();

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

	    ok = false;

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

		ok = false;
	      }
	  }

	if (extraTracked.size() > 0)
	  {
	    for (Invid missingSource: extraTracked)
	      {
		// "** DBLinkTracker.checkInvids(): DBObject {0} is lacking a forward asymmetric link to invid {1} that the DBLinkTracker thinks should be there!"
		Ganymede.debug(ts.l("checkInvids.missingLink", missingSource, target));

		ok = false;
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
	DBLinkTrackerSession sessionObj = new DBLinkTrackerSession(session);

	sessionOverlays.put(session, sessionObj);

	return sessionObj;
      }
  }

  /**
   * This method returns the appropriate, active DBLinkTrackerContext
   * for the session being requested.
   *
   * The session parameter may be null, in which case the
   * persistentLinks context is returned.
   *
   * If the session has not been seen before, a new
   * DBLinkTrackerSession and DBLinkTrackerContext will be created to
   * service the session.
   */

  private DBLinkTrackerContext getContext(DBSession session)
  {
    if (session == null)
      {
	return persistentLinks;
      }

    if (sessionOverlays.containsKey(session))
      {
	DBLinkTrackerSession sessionObj = sessionOverlays.get(session);

	return sessionObj.getCurrentContext();
      }
    else
      {
	DBLinkTrackerSession sessionObj = new DBLinkTrackerSession(session);

	sessionOverlays.put(session, sessionObj);

	return sessionObj.getCurrentContext();
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
   * arlut.csd.ganymede.server.DBLinkTrackerContext
   * DBLinkTrackerContexts} to allow checkpoint, popCheckpoint, and
   * rollback operations to properly interact with the DBLinkTracker
   * system.
   */

  class DBLinkTrackerSession
  {
    /**
     * Named stack of DBLinkTrackerContext objects, tracking the
     * overlays on the asymmetric link structures of DBLinkTracker as
     * checkpoints are established and popped/rolledback for the
     * DBSession.
     */

    private NamedStack<DBLinkTrackerContext> contexts;

    private DBSession session = null;

    /* -- */

    public DBLinkTrackerSession(DBSession session)
    {
      this.session = session;

      contexts = new NamedStack<DBLinkTrackerContext>();

      DBLinkTrackerContext defaultContext = new DBLinkTrackerContext(this, persistentLinks);

      contexts.push(DBLinkTrackerSession.class.getName(), defaultContext);
    }

    public DBLinkTrackerContext getCurrentContext()
    {
      return contexts.getTopObject();
    }

    public void checkpoint(String ckp_key)
    {
      contexts.push(ckp_key, new DBLinkTrackerContext(this, getCurrentContext()));
    }

    public void rollback(String ckp_key)
    {
      contexts.pop(ckp_key);
    }

    public void consolidate(String ckp_key)
    {
      DBLinkTrackerContext consolidationContext = getCurrentContext();

      if (contexts.pop(ckp_key) != null)
	{
	  getCurrentContext().transferFrom(consolidationContext);
	}
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                            DBLinkTrackerContext

  ----------------------------------------------------------------------------*/

  /**
   * Helper class associated with DBLinkTracker.
   *
   * DBLinkTrackerContext is responsible for recording per-DBSession
   * overlays onto the DBLinkTracker's main overlay structure,
   * representing additions or subtractions from the overlay
   * that affect a particular DBSession.
   *
   * DBLinkTrackerSession supports maintaining a checkpoint NamedStack
   * to allow checkpoint, popCheckpoint, and rollback operations to
   * properly interact with the DBLinkTracker system.
   */

  class DBLinkTrackerContext
  {
    /**
     * If we are representing changes made by a DBSession, we'll need
     * to point to our parent context.  Checkpoints made in a
     * DBEditSet will stack DBLinkTrackerContexts in the
     * DBLinkTrackerSession class, with each checkpoint pointing to
     * the DBLinkTrackerContext that preceded it in the checkpoint
     * stack.
     */

    DBLinkTrackerContext parent;

    /**
     * If we are tracking changes made by a specific DBSession, we'll
     * record a reference to the DBLinkTrackerSession here.
     *
     * Note that sessionTracker will only be null if parent is null,
     * as we must then be representing the persistent, checked-in
     * state of the linkages.
     */

    DBLinkTrackerSession sessionTracker;

    /**
     * Map of target Invids to Sets of Invids that have forward
     * asymmetric links to the target in this context.
     */

    Map<Invid, Set<Invid>> targetToSourcesMap;

    /**
     * These are source invids that our session has modified.  We know
     * that only one session at a time can have an object checked out
     * for editing, so we know that sourcesTouched by one
     * DBLinkTrackerContext cannot overlap with sourcesTouched from a
     * DBLinkTrackerContext associated with a different DBSession.
     *
     * Thus, if we have an Invid our sourcesTouched Set, we know that
     * we can add or remove it from another context when we are merged
     * down into it.
     */

    Set<Invid> sourcesTouched;

    /* -- */

    /**
     * Constructor for the root context.
     */

    public DBLinkTrackerContext()
    {
      parent = null;
      sessionTracker = null;
      sourcesTouched = null;
      targetToSourcesMap = new HashMap<Invid, Set<Invid>>();
    }

    /**
     * Constructor for a session context.
     */

    public DBLinkTrackerContext(DBLinkTrackerSession sessionTracker, DBLinkTrackerContext parent)
    {
      this();

      this.parent = parent;
      this.sessionTracker = sessionTracker;

      if (parent.sourcesTouched == null)
	{
	  this.sourcesTouched = new HashSet<Invid>();
	}
      else
	{
	  this.sourcesTouched = new HashSet<Invid>(parent.sourcesTouched);
	}
    }

    /**
     * Returns true if this context is the root node, with no
     * associated DBSessionTracker, sourcesTouched Set, or parent.
     */

    public boolean isRootNode()
    {
      return parent == null;
    }

    /**
     * This method is used to fold changes made in another context
     * into our own.
     *
     * If we are associated with a DBLinkTrackerSession, we must be
     * receiving a fold-in from a checkpoint context that is being
     * coalesced into our own in response to a
     * DBEditSet.popCheckpoint().  In this case, we want to replace
     * our own data with the one from the newer checkpoint.
     *
     * If do not have an associated DBLinkTrackerSession, we are the
     * context for the persisted checked-in objects in the DBStore,
     * and we have to take care to only transfer relating to
     * asymmetric links from Invids that were checked out by the
     * otherContext's associated DBSession.
     */

    public void transferFrom(DBLinkTrackerContext otherContext)
    {
      if (otherContext.sessionTracker == null)
	{
	  throw new RuntimeException("Can't transfer changes from a context that is not associated with a session tracker.");
	}

      if (!isRootNode())
	{
	  if (sessionTracker != otherContext.sessionTracker)
	    {
	      throw new RuntimeException("We can't transfer from one session to another.");
	    }

	  this.targetToSourcesMap.putAll(otherContext.targetToSourcesMap);
	  this.sourcesTouched.addAll(otherContext.sourcesTouched);

	  return;
	}

      // okay, we're the root node, which means we have to take care
      // only to fold in changes relating to source invids that the
      // otherContext has checked out for its session, otherwise we
      // might get errors from concurrent sessions working.

      for (Invid target: otherContext.targetToSourcesMap.keySet())
	{
	  Set<Invid> otherSources = otherContext.targetToSourcesMap.get(target);
	  Set<Invid> localSources = targetToSourcesMap.get(target);

	  if (localSources == null)
	    {
	      localSources = new HashSet<Invid>();
	      targetToSourcesMap.put(target, localSources);
	    }

	  for (Invid touchedSource: otherContext.sourcesTouched)
	    {
	      if (otherSources.contains(touchedSource))
		{
		  localSources.add(touchedSource);
		}
	      else
		{
		  localSources.remove(touchedSource);
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

    public boolean linkObject(Invid target, Invid source)
    {
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      if (!isRootNode())
	{
	  sourcesTouched.add(source);
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

    public boolean unlinkObject(Invid target, Invid source)
    {
      if (target == null || source == null)
	{
	  throw new NullPointerException();
	}

      if (!isRootNode())
	{
	  sourcesTouched.add(source);
	}

      Set<Invid> sources = getForwardLinkSources(target);

      boolean result = sources.remove(source);

      if (isRootNode() && sources.size() == 0)
	{
	  // we can only remove a target from the targetToSourcesMap
	  // if we are the root node, else transferFrom() will not
	  // know that we made any changes to links pointing to target

	  targetToSourcesMap.remove(target);
	}

      return result;
    }

    /**
     * This method records that object source has forward asymmetric
     * links to all object invids in the targets set.
     */

    public void registerObject(Set<Invid> targets, Invid source)
    {
      if (targets == null || source == null)
	{
	  throw new NullPointerException();
	}

      if (!isRootNode())
	{
	  sourcesTouched.add(source);
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

    public void unregisterObject(Set<Invid> targets, Invid source)
    {
      if (targets == null || source == null)
	{
	  throw new NullPointerException();
	}

      if (!isRootNode())
	{
	  sourcesTouched.add(source);
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

    public boolean linkExists(Invid source, Invid target)
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

    public String forwardAsymmetricLinksToString(Invid target)
    {
      if (target == null)
	{
	  throw new NullPointerException();
	}

      StringBuilder builder = new StringBuilder();

      builder.append("-> Tracked forward links pointing at ");
      builder.append(describe(sessionTracker.session, target));
      builder.append("\n");

      Set<Invid> sources = targetToSourcesMap.get(target);

      if (sources == null && parent != null)
	{
	  DBLinkTrackerContext p = parent;

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
	      builder.append(describe(sessionTracker.session, source));
	      builder.append("\n");
	    }
	}

      return builder.toString();
    }

    /**
     * This method returns a Set of all Invids that point to the
     * target object through forward asymmetric links in this
     * DBLinkTrackerContext's context.
     *
     * If we don't have a set in our own context, we'll either copy
     * the set for the target from our nearest parent that has a set
     * for the target, or else we'll create a new empty set.. in this
     * way, we provide a Set of forward pointer Invids that is
     * specific to this context in our session/checkpoint stack.
     */

    private Set<Invid> getForwardLinkSources(Invid target)
    {
      Set<Invid> sources = targetToSourcesMap.get(target);

      if (sources == null)
	{
	  DBLinkTrackerContext p = parent;

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
