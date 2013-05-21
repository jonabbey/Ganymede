/*
   GASH 2

   DBDumpLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

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

import java.util.Vector;

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
 * <p>{@link arlut.csd.ganymede.server.DBReadLock DBReadLock}'s can be
 * established while a DBDumpLock is active, though.</p>
 *
 * <p>A DBDumpLock acts as a highest priority DBReadLock.</p>
 */

final class DBDumpLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   * <p>Constructor to get a dump lock on all the object bases.</p>
   */

  public DBDumpLock(DBStore store)
  {
    this.lockSync = store.lockSync;
    this.baseSet = store.getBases();

    this.locked = false;
  }

  /**
   * <p>Constructor to get a dump lock on a subset of the object
   * bases.</p>
   */

  public DBDumpLock(DBStore store, Vector<DBObjectBase> baseSet)
  {
    this.lockSync = store.lockSync;
    this.baseSet = baseSet;
    this.locked = false;
  }

  /**
   * <p>A thread that calls establish() will be suspended (waiting on
   * the server's {@link arlut.csd.ganymede.server.DBStore DBStore}
   * until all DBObjectBases listed in this DBDumpLock's constructor
   * are available to be locked.  At that point, the thread blocking
   * on establish() will wake up possessing a dump lock on the
   * requested DBObjectBases.</p>
   *
   * <p>It is possible for the establish() to fail completely.. the
   * admin console may reject a client whose thread is blocking on
   * establish(), for instance, or the server may be shut down.  In
   * those cases, another thread may call this DBDumpLock's {@link
   * arlut.csd.ganymede.server.DBLock#abort() abort()} method, in
   * which case establish() will throw an InterruptedException, and
   * the lock will not be established.</p>
   *
   * @param key An object used in the server to uniquely identify the
   * entity internal to Ganymede that is attempting to obtain the
   * lock, typically a {@link
   * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} or a
   * {@link arlut.csd.ganymede.server.GanymedeBuilderTask
   * GanymedeBuilderTask}.
   */

  @Override public final void establish(Object key) throws InterruptedException
  {
    boolean waiting = false;
    boolean okay = false;

    /* -- */

    synchronized (lockSync)
      {
        if (!lockSync.claimLockKey(key, this))
          {
            throw new RuntimeException("Error: dump lock sought by owner of existing lockset.");
          }

        try
          {
            lockSync.incLocksWaitingCount();

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
                if (abort)
                  {
                    throw new InterruptedException("DBDumpLock (" + key + "):  establish aborting before permission granted");
                  }

                okay = true;

                for (DBObjectBase base: baseSet)
                  {
                    if (base.hasWriter())
                      {
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
  }

  /**
   * <p>Release this lock on all bases locked</p>
   */

  @Override public final void release()
  {
    synchronized (lockSync)
      {
        while (this.inEstablish)
          {
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
            return;
          }

        for (DBObjectBase base: baseSet)
          {
            base.removeDumpLock(this);
          }

        this.locked = false;
        lockSync.unclaimLockKey(key, this);

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
        this.abort = true;
        release();
      }
  }
}
