/*
   GASH 2

   DBLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                          DBLock

------------------------------------------------------------------------------*/

/**
 * <p>DBLocks arbitrate access to a DBStore object.  Threads wishing to read from,
 * dump, or update the DBStore must be in possession of an established DBLock.
 * The general scheme is that any number of readers and/or dumpers can read
 * from the database simultaneously.  If a number of readers are processing when
 * a thread attempts to establish a write lock, those readers are allowed to
 * complete their reading, but no new read lock may be established until the
 * writer has a chance to get in and make its update.</p>
 * 
 * <p>If there are a number of writer locks queued up for update access to the
 * DBStore when a thread attempts to establish a dump lock, those writers are
 * allowed to complete their updates, but no new writer is queued until the
 * dump thread finishes dumping the database.</p>
 * 
 * <p>There is currently no support for handling timeouts, and locks can persist
 * indefinitely.</p>
 *
 */

public abstract class DBLock {

  // type parent


  /**
   *
   * Returns true if the lock has been established and not
   * yet aborted / released.
   *
   *
   */

  abstract boolean isLocked();

  /**
   *
   * Returns true if the lock has the given base
   * locked.
   *
   */

  abstract boolean isLocked(DBObjectBase base);

  /**
   *
   * This method waits until the lock can be established.
   * 
   */

  abstract void establish(Object key) throws InterruptedException;

  /**
   *
   * Unlock the bases held by this lock.
   *
   */ 

  abstract void release();

  /**
   *
   * Abort this lock;  if any thread is waiting in establish() on this
   * lock when abort() is called, that thread's call to establish() will
   * fail with an InterruptedException.
   *
   */

  abstract void abort();

  /**
   *
   * Returns the key that this lock is established with,
   * or null if the lock has not been established.
   *
   */

  abstract Object getKey();

}
