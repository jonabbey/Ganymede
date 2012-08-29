/*
   GASH 2

   DBSessionLockManager.java

   The GANYMEDE object storage system.

   Created: 15 March 2002

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                            DBSessionLockManager

------------------------------------------------------------------------------*/

/**
 * <P>This class coordinates lock activity for a server-side
 * {@link arlut.csd.ganymede.server.DBSession DBSession} object.  This class handles
 * the logic to make sure that a session does not grant a new lock that would
 * conflict with a lock already held by the same session.</P>
 */

public class DBSessionLockManager {

  private Hashtable lockHash = new Hashtable(31);
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

    if (!lockHash.containsKey(lock))
      {
        return false;
      }
    else
      {
        return lock.isLocked();
      }
  }

  /**
   * <p>Establishes a read lock for the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBases}
   * in bases.</p>
   *
   * <p>The thread calling this method will block until the read lock 
   * can be established.  If any of the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBases}
   * in the bases vector have transactions
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.</p>
   *
   * <p>All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.</p>
   */

  public synchronized DBReadLock openReadLock(Vector<DBObjectBase> bases) throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    // we'll never be able to establish a read lock if we have to
    // wait for this thread to release an existing write lock..

    if (lockHash.size() != 0)
      {
        Enumeration en = lockHash.keys();

        while (en.hasMoreElements())
          {
            DBLock oldLock = (DBLock) en.nextElement();

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
      }
    
    lock = new DBReadLock(session.getStore(), bases);
    
    lockHash.put(lock, Boolean.TRUE);   // use like a set
    
    lock.establish(session.getKey()); // block

    return lock;
  }

  /**
   * <P>openReadLock establishes a read lock for the entire
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</P>
   *
   * <P>The thread calling this method will block until the read lock 
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.</P>
   *
   * <P>All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.</P>
   */

  public synchronized DBReadLock openReadLock() throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    // we'll never be able to establish a read lock if we have to
    // wait for this thread to release an existing write lock..

    if (lockHash.size() != 0)
      {
        Enumeration en = lockHash.keys();

        while (en.hasMoreElements())
          {
            DBLock oldLock = (DBLock) en.nextElement();

            if (oldLock instanceof DBWriteLock)
              {
                throw new InterruptedException("Can't establish global read lock, session " + session.getID() +
                                               " already has write lock:\n" +
                                               oldLock.toString());
              }
          }
      }

    lock = new DBReadLock(session.getStore());

    lockHash.put(lock, Boolean.TRUE);

    lock.establish(session.getKey());

    return lock;
  }

  /**
   * <p>Establishes a write lock for the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBases}
   * in bases.</p>
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
    DBWriteLock lock;

    /* -- */

    // we'll never be able to establish a write lock if we have to
    // wait for this thread to release read, write, or dump locks..
    // and we must not have pre-existing locks on bases not
    // overlapping with our bases parameter either, or else we risk
    // dead-lock later on..

    if (lockHash.size() != 0)
      {
        StringBuilder resultBuffer = new StringBuilder();

        Enumeration en = lockHash.keys();

        while (en.hasMoreElements())
          {
            resultBuffer.append(en.nextElement().toString());
            resultBuffer.append("\n");
          }

        throw new InterruptedException("Can't establish write lock, session " + session.getID() + 
                                       " already has locks:\n" +
                                       resultBuffer.toString());
      }

    lock = new DBWriteLock(session.getStore(), bases);

    lockHash.put(lock, Boolean.TRUE);

    lock.establish(session.getKey());

    return lock;
  }

  /**
   * <P>This method establishes a dump lock on all object bases in this Ganymede
   * server.</P>
   */

  public synchronized DBDumpLock openDumpLock() throws InterruptedException
  {
    DBDumpLock lock;

    /* -- */

    // we'll never be able to establish a dump lock if we have to
    // wait for this thread to release an existing write lock..

    if (lockHash.size() != 0)
      {
        Enumeration en = lockHash.keys();

        while (en.hasMoreElements())
          {
            DBLock oldLock = (DBLock) en.nextElement();

            if (oldLock instanceof DBWriteLock)
              {
                throw new InterruptedException("Can't establish global dump lock, session " + session.getID() +
                                               " already has write lock:\n" +
                                               oldLock.toString());
              }
          }
      }

    lock = new DBDumpLock(session.getStore());
    lockHash.put(lock, Boolean.TRUE);

    lock.establish(session.getKey());

    return lock;
  }

  /**
   * <P>releaseLock releases a particular lock held by this session.
   * This method will not force a lock being held by another thread to
   * drop out of its establish method.. it is intended to be called by
   * the same thread that established the lock.</P>
   */

  public synchronized void releaseLock(DBLock lock)
  {
    if (!lockHash.containsKey(lock))
      {
        throw new IllegalArgumentException("lock " + lock.toString() + " not held by this session");
      }

    lock.release();
    lockHash.remove(lock);
  }

  /**
   * <P>releaseAllLocks() releases all locks held by this
   * session.</P>
   */

  public synchronized void releaseAllLocks()
  {
    Enumeration en = lockHash.keys();

    while (en.hasMoreElements())
      {
        DBLock lock = (DBLock) en.nextElement();
        lock.abort();           // blocks until the lock can be cleared
      }

    lockHash.clear();
  }
}
