/*
   GASH 2

   DBLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.1 $ %D%
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

  abstract boolean isLocked(DBObjectBase base);
  abstract void establish(Object key);
  abstract void release();
}
