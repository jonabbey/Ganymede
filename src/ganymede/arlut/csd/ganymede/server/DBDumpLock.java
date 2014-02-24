/*
   GASH 2

   DBDumpLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
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

import java.util.List;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBDumpLock

------------------------------------------------------------------------------*/

/**
 * <p>DBDumpLock is a {@link arlut.csd.ganymede.server.DBLock DBLock}
 * object used to lock the {@link arlut.csd.ganymede.server.DBStore
 * DBStore} either for the purpose of dumping the database or for
 * handling a GanymedeBuilderTask build.  A DBDumpLock establish
 * request has lower priority than {@link
 * arlut.csd.ganymede.server.DBWriteLock DBWriteLock} requests, but
 * once a DBDumpLock establish request is submitted, no new
 * DBWriteLock can be established until the dumping thread has
 * completed the dump and released the lock.</p>
 *
 * <p>{@link arlut.csd.ganymede.server.DBReadLock DBReadLocks} can be
 * established while a DBDumpLock is active and vice-versa,
 * though.</p>
 *
 * <p>Essentially, DBDumpLock acts as a DBReadLock that has priority
 * over incoming {@link arlut.csd.ganymede.server.DBWriteLock
 * DBWriteLocks}.</p>
 */

final class DBDumpLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   * All DBLock's have an identifier key, which is used to identify
   * the lock in the {@link arlut.csd.ganymede.server.DBStore
   * DBStore}'s {@link arlut.csd.ganymede.server.DBLockSync
   * DBLockSync} object.  The establish() methods in the DBLock
   * subclasses consult the DBStore.lockSync to make sure that no
   * {@link arlut.csd.ganymede.server.DBSession DBSession} ever
   * possesses more than one write lock, to prevent deadlocks from
   * occuring in the server.
   */

  private Object key;

  private boolean locked = false;
  private boolean inEstablish = false;
  private boolean abort = false;

  /**
   * In order to prevent deadlocks, each individual lock must be
   * established on all applicable {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} at the time
   * the lock is initially established.  baseSet is the List of
   * DBObjectBases that this DBLock is/will be locked on.
   */

  private List<DBObjectBase> baseSet;

  /**
   * Constructor to get a dump lock on all the object bases.
   */

  public DBDumpLock(DBStore store)
  {
    super(store.lockSync);

    this.baseSet = store.getBases();
  }

  /**
   * Constructor to get a dump lock on a subset of the object bases.
   */

  public DBDumpLock(DBStore store, List<DBObjectBase> baseSet)
  {
    super(store.lockSync);

    this.baseSet = baseSet;
  }

  /**
   * Returns true if this lock is locked.
   */

  @Override public final boolean isLocked()
  {
    return this.locked;
  }

  /**
   * Returns true if this lock is waiting in establish()
   */

  @Override public final boolean isEstablishing()
  {
    return this.inEstablish;
  }

  /**
   * Returns true if this lock is aborting
   */

  @Override public final boolean isAborting()
  {
    return this.abort;
  }

  /**
   * Returns list of DBObjectBases that this lock is meant to cover.
   */

  @Override final List<DBObjectBase> getBases()
  {
    return this.baseSet;
  }

  @Override final Object getKey()
  {
    if (isLocked())
      {
        return key;
      }
    else
      {
        return null;
      }
  }

  /**
   * <p>A thread that calls establish() will be suspended (waiting on
   * the server's {@link arlut.csd.ganymede.server.DBLockSync
   * DBLockSync} until all DBObjectBases listed in this DBDumpLock's
   * constructor are available to be locked.  At that point, the
   * thread blocking on establish() will wake up possessing a shared
   * dump lock on the requested DBObjectBases.</p>
   *
   * <p>It is possible for the establish() to fail completely.. the
   * admin console may reject a client whose thread is blocking on
   * establish(), for instance, or the server may be shut down.  In
   * those cases, another thread may call this DBDumpLock's {@link
   * arlut.csd.ganymede.server.DBLock#abort() abort()} method, in
   * which case establish() will throw an InterruptedException, and
   * the lock will not be established.</p>
   *
   * <p>The possessors of DBLocks are identified by a key Object that
   * is provided on the call to {@link
   * arlut.csd.ganymede.server.DBLock#establish(java.lang.Object)}.  A
   * given key may only have one DBWriteLock established at a time,
   * but it may have multiple concurrent DBDumpLocks and DBReadLocks
   * established if there are no DBWriteLocks held by that key or
   * locked on DBObjectBases that overlap this lock request.</p>
   *
   * @param key An object used in the server to uniquely identify the
   * entity internal to Ganymede that is attempting to obtain the
   * lock, typically a unique String.
   */

  @Override public final void establish(Object key) throws InterruptedException
  {
    boolean waiting = false;
    boolean okay = false;

    /* -- */

    if (debug)
      {
        debug(key, "establish() entering");

        Ganymede.printCallStack();
      }

    synchronized (lockSync)
      {
        if (!lockSync.claimLockKey(key, this))
          {
            throw new RuntimeException("Error: dump lock sought by owner of existing lockset.");
          }

        try
          {
            lockSync.incLocksWaitingCount();

            if (debug) debug(key, "added myself to the DBLockSync lockHash.");

            this.key = key;
            this.inEstablish = true;

            // add ourselves to the ObjectBase dump queues..  we don't
            // have to wait for anything to do this.. it's up to the
            // writers to hold off on adding themselves to the
            // writerlists until the dumperlists are empty.

            for (DBObjectBase base: baseSet)
              {
                base.addWaitingDumper(this);
              }

            waiting = true;

            while (!okay)
              {
                if (debug) debug("establish() spinning to get establish permission for " + getBaseNames(baseSet));

                if (abort)
                  {
                    throw new InterruptedException("DBDumpLock (" + key + "):  establish aborting before permission granted");
                  }

                okay = true;

                for (DBObjectBase base: baseSet)
                  {
                    if (base.hasWriter())
                      {
                        if (debug) debug("establish() blocked on base with writer: " + base.getName());

                        okay = false;
                        break;
                      }
                  }

                if (!okay)
                  {
                    lockSync.wait(2500);
                    continue;
                  }

                // nothing can stop us now

                for (DBObjectBase base: baseSet)
                  {
                    base.addDumpLock(this);
                  }

                this.locked = true;
                lockSync.incLockCount();
              }
          }
        finally
          {
            lockSync.decLocksWaitingCount();
            this.inEstablish = false;

            if (waiting)
              {
                for (DBObjectBase base: baseSet)
                  {
                    base.removeWaitingDumper(this);
                  }
              }

            if (!this.locked)
              {
                lockSync.unclaimLockKey(key, this);
                this.key = null;
              }

            lockSync.notifyAll();
          }
      }

    if (debug) debug("establish() got the lock.");
  }

  /**
   * Release this lock on all bases locked
   */

  @Override public final void release()
  {
    if (debug)
      {
        debug("release() entering");
        Ganymede.printCallStack();
      }

    synchronized (lockSync)
      {
        while (this.inEstablish)
          {
            if (debug) debug("release() waiting for inEstablish");

            try
              {
                lockSync.wait(2500); // or until notify'ed
              }
            catch (InterruptedException ex)
              {
              }
          }

        // note that we have to check locked here or else we might accidentally
        // release somebody else's lock below

        if (!this.locked)
          {
            if (debug) debug("release() not locked, returning");
            return;
          }

        for (DBObjectBase base: baseSet)
          {
            base.removeDumpLock(this);
          }

        this.locked = false;
        lockSync.unclaimLockKey(key, this);

        if (debug) debug("release() released");

        this.key = null;             // gc

        lockSync.decLockCount();  // notify consoles
        lockSync.notifyAll();   // many threads might want to check to see what we freed
      }
  }

  /**
   * <p>Withdraw this lock.  This method can be called by a thread to
   * interrupt a lock establish that is blocked waiting to get
   * access to the appropriate set of
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects.  If
   * this method is called while another thread is blocked in
   * establish(), establish() will throw an InterruptedException.</p>
   *
   * <p>Once abort() is processed, this lock may never be established.
   * Any subsequent calls to estabish() will always throw
   * InterruptedException.</p>
   */

  @Override public final void abort()
  {
    synchronized (lockSync)
      {
        if (debug) debug("abort() aborting");
        this.abort = true;
        release();
      }
  }

  private void debug(Object key, String message)
  {
    System.err.println("DBDumpLock(" + key + "): " + message);
  }

  private void debug(String message)
  {
    System.err.println("DBDumpLock(" + this.key + "): " + message);
  }
}
