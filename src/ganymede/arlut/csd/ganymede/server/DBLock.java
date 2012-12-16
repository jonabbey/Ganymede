/*
   GASH 2

   DBLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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
                                                                  abstract class
                                                                          DBLock

------------------------------------------------------------------------------*/

/**
 * <p>DBLocks arbitrate access to {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects in the
 * Ganymede server's {@link arlut.csd.ganymede.server.DBStore
 * DBStore}.</p>
 *
 * <p>Threads wishing to read from, dump, or update object bases in the
 * DBStore must be in possession of an established DBLock.</p>
 *
 * <p>The general scheme is that any number of readers and/or dumpers
 * can read from an object base simultaneously.  Once a DBWriteLock
 * calls {@link
 * arlut.csd.ganymede.server.DBLock#establish(java.lang.Object)} on an
 * object base, all active readers are allowed to complete their
 * reading, but no new read lock may be established until the writer
 * has a chance to get in and make its update and then signals
 * completion by calling release().  Writers are given priority in the
 * DBLock queue over readers.</p>
 *
 * <p>Similarly, if there are a number of writer locks queued up for
 * update access to a DBObjectBase in the DBStore when a thread
 * attempts to establish a DBDumpLock, those writers are allowed to
 * complete their updates, but no new writer is queued until the dump
 * thread finishes dumping the locked bases.</p>
 *
 * <p>All of this priority logic is implemented in the establish()
 * methods of the concrete DBLock subclasses.</p>
 *
 * <p>As mentioned above, all DBLock's are issued in the context of one
 * or more {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
 * objects.  The DBObjectBases are actually the things being locked.
 * To maintain multi-threaded safety of the lock system across
 * multiple DBObjectBases, the DBLock {@link
 * arlut.csd.ganymede.server.DBLock#establish(java.lang.Object)
 * establish()} and {@link arlut.csd.ganymede.server.DBLock#release()
 * release()} methods (as implemented in {@link
 * arlut.csd.ganymede.server.DBReadLock DBReadLock}, {@link
 * arlut.csd.ganymede.server.DBReadLock DBWriteLock}, and {@link
 * arlut.csd.ganymede.server.DBReadLock DBDumpLock}) are all
 * synchronized on the Ganymede's DBStore object.  This
 * synchronization is critical for the proper functioning of the
 * DBLock system.</p>
 *
 * <p>There is currently no intrinsic support for handling timeouts
 * within the DBLock class hierarchy, and locks can persist
 * indefinitely.</p>
 *
 * <p>However, the {@link arlut.csd.ganymede.server.GanymedeSession
 * GanymedeSession} will be notified by RMI via the {@link
 * arlut.csd.ganymede.server.GanymedeSession#unreferenced()} method if
 * the client dies and by the scheduled {@link
 * arlut.csd.ganymede.server.timeOutTask} in conjunction with the
 * server's {@link
 * arlut.csd.ganymede.server.GanymedeServer#clearIdleSessions()}
 * method if the client goes idle for too long.</p>
 *
 * <p>In either case, the client's GanymedeSession will force all
 * locks held by the client to {@link
 * arlut.csd.ganymede.server.DBLock#abort()}, thus releasing the
 * locks.</p>
 */

public abstract class DBLock {

  /**
   * <p>All DBLock's have an identifier key, which is used to identify
   * the lock in the {@link arlut.csd.ganymede.server.DBStore
   * DBStore}'s {@link arlut.csd.ganymede.server.DBLockSync
   * DBLockSync} object.  The establish() methods in the DBLock
   * subclasses consult the DBStore.lockSync to make sure that no
   * {@link arlut.csd.ganymede.server.DBSession DBSession} ever
   * possesses more than one write lock, to prevent deadlocks from
   * occuring in the server.</p>
   */

  Object key;

  /**
   * <p>All DBLock's establish() and release() methods synchronize
   * their critical sections on a singleton DBLockSync object held in
   * the Ganymede server's DBStore object in order to guarantee that
   * all lock negotiations are thread-safe.</p>
   */

  DBLockSync lockSync;

  /**
   * <p>In order to prevent deadlocks, each individual lock must be
   * established on all applicable {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} at the time
   * the lock is initially established.  baseSet is the Vector of
   * DBObjectBases that this DBLock is/will be locked on.</p>
   */

  Vector<DBObjectBase> baseSet;

  /**
   * <p>Will be true if a DBLock is successfully locked.</p>
   *
   * <p>Should not be directly accessed outside of the DBLock class
   * hierarchy (unfortunately Java has no support for 'accessible to
   * subclasses only').</p>
   */

  boolean locked = false;

  /**
   * <p>Will be true if a DBLock has had its abort() method called.
   * Once aborted, a lock may never be re-established; the locking
   * code must create a new lock.</p>
   */

  boolean abort = false;

  /**
   * <p>Will be true while a DBLock is in the process of being
   * established.</p>
   */

  boolean inEstablish = false;

  /* -- */

  /**
   * <p>Returns true if the lock has been established and not yet
   * aborted / released.</p>
   */

  boolean isLocked()
  {
    synchronized (lockSync)
      {
        return locked;
      }
  }

  /**
   * <p>Returns true if the lock has the given {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} locked.</p>
   */

  boolean isLocked(DBObjectBase base)
  {
    synchronized (lockSync)
      {
        if (!locked)
          {
            return false;
          }

        for (int i=0; i < baseSet.size(); i++)
          {
            if (baseSet.elementAt(i) == base)
              {
                return true;
              }
          }
      }

    return false;
  }

  /**
   * <p>Returns true if the lock has all of the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects in
   * the provided Vector locked.</p>
   */

  boolean isLocked(Vector<DBObjectBase> bases)
  {
    synchronized (lockSync)
      {
        return arlut.csd.Util.VectorUtils.difference(bases, baseSet).size() == 0;
      }
  }

  /**
   * <p>Returns true if the lock has any of the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects in
   * the provided Vector locked.</p>
   */

  boolean overlaps(Vector<DBObjectBase> bases)
  {
    synchronized (lockSync)
      {
        return arlut.csd.Util.VectorUtils.overlaps(bases, baseSet);
      }
  }

  /**
   * <p>This method waits until the lock can be established.  The
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBases} locked
   * are specified in the constructor of the implementation subclass
   * ({@link arlut.csd.ganymede.server.DBReadLock DBReadLock},
   * {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}, or
   * {@link arlut.csd.ganymede.server.DBDumpLock DBDumpLock}).</p>
   *
   * <p>A thread that calls establish() will be suspended (waiting
   * on the server's {@link arlut.csd.ganymede.server.DBStore DBStore} until
   * all DBObjectBases listed in the DBLock's constructor are available
   * to be locked.  At that point, the thread blocking on establish()
   * will wake up possessing a lock on the requested DBObjectBases.</p>
   *
   * <p>It is possible for the establish() to fail completely.. the
   * admin console may reject a client whose thread is blocking on
   * establish(), for instance, or the server may be shut down.  In
   * those cases, another thread may call the DBLock's
   * {@link arlut.csd.ganymede.server.DBLock#abort() abort()} method, in
   * which case
   * establish() will throw an InterruptedException, and the lock will
   * not be established.</p>
   *
   * @param key An object used in the server to uniquely identify the
   * entity internal to Ganymede that is attempting to obtain the
   * lock, typically a {@link
   * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} or a
   * {@link arlut.csd.ganymede.server.GanymedeBuilderTask
   * GanymedeBuilderTask}.
   */

  abstract void establish(Object key) throws InterruptedException;

  /**
   * <p>Unlock the bases held by this lock.</p>
   */

  abstract void release();

  /**
   * <p>Abort this lock; if any thread is waiting in establish() on
   * this lock when abort() is called, that thread's call to
   * establish() will fail with an InterruptedException.</p>
   */

  abstract void abort();

  /**
   * <p>Returns the key that this lock is established with, or null if
   * the lock has not been established.</p>
   */

  Object getKey()
  {
    if (locked)
      {
        return key;
      }
    else
      {
        return null;
      }
  }

  /**
   * <p>Returns a string describing this lock for use in debug
   * messages</p>
   */

  public String toString()
  {
    StringBuilder returnString = new StringBuilder();

    // get the object's type and ID

    returnString.append(super.toString());

    returnString.append(", key = ");
    returnString.append(key.toString());

    if (inEstablish)
      {
        returnString.append(", establishing");
      }

    if (abort)
      {
        returnString.append(", aborted");
      }

    if (locked)
      {
        returnString.append(", locked on: ");
      }
    else
      {
        returnString.append(", currently unlocked on: ");
      }

    for (int i = 0; i < baseSet.size(); i++)
      {
        if (i>0)
          {
            returnString.append(", ");
          }

        returnString.append(baseSet.elementAt(i).toString());
      }

    return returnString.toString();
  }
}
