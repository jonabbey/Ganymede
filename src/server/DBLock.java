/*
   GASH 2

   DBLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/01/22 18:05:33 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
