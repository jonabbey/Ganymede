/*
   GASH 2

   DBLockSync.java

   The GANYMEDE object storage system.

   Created: 9 February 2000

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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBLockSync

------------------------------------------------------------------------------*/

/**
 * <p>This class acts to provide a singleton object for interlock
 * coordination.  All global data required for coordinating DBLock
 * lock activity is stored in the singleton object.</p>
 *
 * <p>Note that much code in the various {@link
 * arlut.csd.ganymede.server.DBLock} subclasses, and in the rest of
 * the Ganymede server, establishes external synchronization on the
 * DBLockSync object referenced in {@link
 * arlut.csd.ganymede.server.DBStore#lockSync DBStore.lockSync}, so
 * certain methods in this class which do not appear synchronized may
 * in fact be dependent on external synchronization.</p>
 */

public final class DBLockSync {

  /**
   * <p>Identifier keys for current {@link arlut.csd.ganymede.server.DBLock
   * DBLocks}.</p>
   *
   * <p>This hash is used by the establish() method in various DBLock
   * subclasses to guarantee that only one lock will established by
   * a client at a time, to prevent any possibility of DBLock deadlock.</p>
   *
   * <p>The values in this hash may either be scalar DBLock objects, or
   * in the case of readers (where it is permissible for a single client
   * to have several distinct reader locks), a List of DBReadLocks.</p>
   */

  private HashMap lockHash;

  /**
   * <p>A count of how many {@link arlut.csd.ganymede.server.DBLock
   * DBLocks} are established on {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} in this
   * DBStore.</p>
   */

  private int locksHeld = 0;

  /**
   * <p>A count of how many {@link arlut.csd.ganymede.server.DBLock
   * DBLocks} are waiting to be established on {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} in this
   * DBStore.</p>
   */

  private int locksWaiting = 0;

  /* -- */

  public DBLockSync()
  {
    resetLockHash(0);
  }

  /**
   * <p>This method causes the DBLockSync object's lock owner HashMap
   * to be reset.  If count is not zero, that value will be used to
   * set the initial capacity for the HashMap.</p>
   */

  public synchronized void resetLockHash(int count)
  {
    if (count <= 0)
      {
        lockHash = new HashMap(20); // default value
      }
    else
      {
        lockHash = new HashMap(count);
      }

    locksHeld = 0;
    locksWaiting = 0;
  }

  /**
   * <p>This method associates a DBLock with the given key, making
   * sure that there is no conflicting lock request for the key</p>
   *
   * @return True if the lock could be associated with key, False if
   * there was a pre-existing conflicting association.
   */

  public synchronized boolean claimLockKey(Object key, DBLock lock)
  {
    if (lock instanceof DBReadLock)
      {
        Object obj = lockHash.get(key);

        if (obj != null && !(obj instanceof List))
          {
            return false;
          }

        List<DBLock> lockList = (List<DBLock>) obj;

        if (lockList == null)
          {
            lockList = new ArrayList<DBLock>();
            lockHash.put(key, lockList);
          }

        lockList.add(lock);
      }
    else
      {
        if (lockHash.containsKey(key))
          {
            return false;
          }

        lockHash.put(key, lock);
      }

    return true;
  }

  /**
   * <p>This method disassociates a DBLock from the given key</p>
   *
   * <p>If the key was not previously claimed for the given lock, an
   * IllegalStateException will be thrown.</p>
   */

  public synchronized void unclaimLockKey(Object key, DBLock lock)
  {
    Object obj = lockHash.get(key);

    if (obj == null)
      {
        throw new IllegalStateException("No such key");
      }

    if (lock instanceof DBReadLock)
      {
        if (!(obj instanceof List))
          {
            throw new IllegalStateException("Error, can't remove a read lock while there is a " +
                                            obj +
                                            " associated with key " + key +
                                            ".. there are no readlocks here.");
          }

        List<DBLock> lockList = (List<DBLock>) obj;

        if (!lockList.contains(lock))
          {
            throw new IllegalStateException("Mismatched lock claim");
          }

        lockList.remove(lock);

        if (lockList.size() == 0)
          {
            lockHash.remove(key);   // that was the last read lock on this key
          }
      }
    else
      {
        if (obj != lock)
          {
            throw new IllegalStateException("Mismatched lock claim");
          }

        lockHash.remove(key);
      }
  }

  /**
   * <p>This method returns a List of DBReadLock objects associated
   * with key, if any.  If there is no DBReadLock vector associated
   * with the key, an IllegalStateException will be thrown.</p>
   *
   * <p>The List returned is part of DBLockSync's internal data
   * structures, and should only be browsed in a block synchronized on
   * this DBLockSync object.</p>
   *
   * <p>The List returned should not be modified by external code.</p>
   */

  public synchronized List<DBReadLock> getReadLockList(Object key)
  {
    Object o = lockHash.get(key);

    if (o == null)
      {
        return null;
      }

    if (o instanceof List)
      {
        return (List<DBReadLock>) o;
      }

    throw new IllegalStateException("DBLockSync does not contain a readlock list for key " + key);
  }

  /**
   * <p>This method returns a DBLock associated with the given key, if
   * any.</p>
   *
   * <p>This method will only ever return a DBWriteLock or a
   * DBDumpLock.  If the key is associated with a List of DBReadLocks,
   * null will be returned.</p>
   */

  public synchronized DBLock getLockHeld(Object key)
  {
    Object obj = lockHash.get(key);

    if (obj == null || (obj instanceof DBLock))
      {
        return (DBLock) obj;
      }

    return null; // Can't return a single lock, this key has a readlock vector.
  }

  /**
   * <p>Increments the count of locks waiting to be established.</p>
   */

  public synchronized void incLocksWaitingCount()
  {
    locksWaiting++;
    GanymedeAdmin.updateLocksHeld();
  }

  /**
   * <p>Decrements the count of locks waiting to be established.</p>
   */

  public synchronized void decLocksWaitingCount()
  {
    locksWaiting--;
    GanymedeAdmin.updateLocksHeld();
  }

  /**
   * <p>Increments the count of held locks for the admin consoles.</p>
   */

  public synchronized void incLockCount()
  {
    locksHeld++;
    GanymedeAdmin.updateLocksHeld();
  }

  /**
   * <p>Decrements the count of held locks for the admin consoles.</p>
   */

  public synchronized void decLockCount()
  {
    locksHeld--;
    GanymedeAdmin.updateLocksHeld();

    if (locksHeld < 0)
      {
        throw new RuntimeException("Locks held has gone negative");
      }
  }

  /**
   * <p>Returns the number of locks currently waiting to be established.</p>
   */

  public int getLocksWaitingCount()
  {
    return locksWaiting;
  }

  /**
   * <p>Returns the number of locks presently held in the database.</p>
   */

  public int getLockCount()
  {
    return locksHeld;
  }
}
