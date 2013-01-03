/*
   GASH 2

   DBSessionLockManager.java

   The GANYMEDE object storage system.

   Created: 15 March 2002

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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                            DBSessionLockManager

------------------------------------------------------------------------------*/

/**
 * <p>This class coordinates lock activity for a server-side {@link
 * arlut.csd.ganymede.server.DBSession DBSession} object.  This class
 * handles the logic to make sure that a session does not grant a new
 * lock that would conflict with a lock already held by the same
 * session.</p>
 */

public class DBSessionLockManager {

  private HashSet<DBLock> lockSet = new HashSet<DBLock>(31);
  private DBSession session;

  /* -- */

  public DBSessionLockManager(DBSession session)
  {
    this.session = session;
  }

  /**
   * <p>Returns true if the session's lock is currently locked, false
   * otherwise.</p>
   */

  public synchronized boolean isLocked(DBLock lock)
  {
    if (lock == null)
      {
        throw new IllegalArgumentException("bad param to isLocked()");
      }

    if (!lockSet.contains(lock))
      {
        return false;
      }
    else
      {
        return lock.isLocked();
      }
  }

  /**
   * <p>Establishes a read lock for the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} in
   * bases.</p>
   *
   * <p>The thread calling this method will block until the read lock
   * can be established.  If any of the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} in the
   * bases vector have transactions currently committing, the
   * establishment of the read lock will be suspended until all such
   * transactions are committed.</p>
   *
   * <p>All viewDBObject calls done within the context of an open read
   * lock will be transaction consistent.  Other sessions may pull
   * objects out for editing during the course of the session's read
   * lock, but no visible changes will be made to those ObjectBases
   * until the read lock is released.</p>
   */

  public synchronized DBReadLock openReadLock(Vector<DBObjectBase> bases) throws InterruptedException
  {
    // we'll never be able to establish a read lock if we have to
    // wait for this thread to release an existing write lock..

    for (DBLock oldLock: lockSet)
      {
        if (oldLock instanceof DBWriteLock)
          {
            if (oldLock.overlaps(bases))
              {
                throw new InterruptedException("Can't establish read lock, session " + session.getID() +
                                               " already has overlapping write lock:\n" +
                                               oldLock.toString());
              }
          }
      }

    DBReadLock lock = new DBReadLock(session.getStore(), bases);
    lockSet.add(lock);
    lock.establish(session.getKey()); // block

    return lock;
  }

  /**
   * <p>openReadLock establishes a read lock for the entire
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>The thread calling this method will block until the read lock
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be
   * suspended until all such transactions are committed.</p>
   *
   * <p>All viewDBObject calls done within the context of an open read
   * lock will be transaction consistent.  Other sessions may pull
   * objects out for editing during the course of the session's read
   * lock, but no visible changes will be made to those ObjectBases
   * until the read lock is released.</p>
   */

  public synchronized DBReadLock openReadLock() throws InterruptedException
  {
    // we'll never be able to establish a read lock if we have to
    // wait for this thread to release an existing write lock..

    for (DBLock oldLock: lockSet)
      {
        if (oldLock instanceof DBWriteLock)
          {
            throw new InterruptedException("Can't establish global read lock, session " + session.getID() +
                                           " already has write lock:\n" +
                                           oldLock.toString());
          }
      }

    DBReadLock lock = new DBReadLock(session.getStore());
    lockSet.add(lock);
    lock.establish(session.getKey());

    return lock;
  }

  /**
   * <p>Establishes a write lock for the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} in
   * bases.</p>
   *
   * <p>The thread calling this method will block until the write lock
   * can be established.  If this DBSession already possesses a write lock,
   * read lock, or dump lock, the openWriteLock() call will fail with
   * an InterruptedException.</p>
   *
   * <p>If one or more different DBSessions (besides this) have locks in
   * place that would block acquisition of the write lock, this method
   * will block until the lock can be acquired.</p>
   */

  public synchronized DBWriteLock openWriteLock(Vector<DBObjectBase> bases) throws InterruptedException
  {
    // we'll never be able to establish a write lock if we have to
    // wait for this thread to release read, write, or dump locks..
    // and we must not have pre-existing locks on bases not
    // overlapping with our bases parameter either, or else we risk
    // dead-lock later on..

    if (lockSet.size() != 0)
      {
        StringBuilder resultBuffer = new StringBuilder();

        for (DBLock lock: lockSet)
          {
            resultBuffer.append(lock.toString());
            resultBuffer.append("\n");
          }

        throw new InterruptedException("Can't establish write lock, session " + session.getID() +
                                       " already has locks:\n" +
                                       resultBuffer.toString());
      }

    DBWriteLock lock = new DBWriteLock(session.getStore(), bases);
    lockSet.add(lock);
    lock.establish(session.getKey());

    return lock;
  }

  /**
   * <p>This method establishes a dump lock on all object bases in
   * this Ganymede server.</p>
   */

  public synchronized DBDumpLock openDumpLock() throws InterruptedException
  {
    // we'll never be able to establish a dump lock if we have to
    // wait for this thread to release an existing write lock..

    for (DBLock oldLock: lockSet)
      {
        if (oldLock instanceof DBWriteLock)
          {
            throw new InterruptedException("Can't establish global dump lock, session " + session.getID() +
                                           " already has write lock:\n" +
                                           oldLock.toString());
          }
      }

    DBDumpLock lock = new DBDumpLock(session.getStore());
    lockSet.add(lock);
    lock.establish(session.getKey());

    return lock;
  }

  /**
   * <p>releaseLock releases a particular lock held by this session.
   * This method will not force a lock being held by another thread to
   * drop out of its establish method.. it is intended to be called by
   * the same thread that established the lock.</p>
   */

  public synchronized void releaseLock(DBLock lock)
  {
    if (!lockSet.contains(lock))
      {
        throw new IllegalArgumentException("lock " + lock.toString() + " not held by this session");
      }

    lock.release();
    lockSet.remove(lock);
  }

  /**
   * <p>releaseAllLocks() releases all locks held by this
   * session.</p>
   */

  public synchronized void releaseAllLocks()
  {
    for (DBLock lock: lockSet)
      {
        lock.abort();           // blocks until the lock can be cleared
      }

    lockSet.clear();
  }
}
