/*
   GASH 2

   DBWriteLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 1999/01/22 18:05:39 $
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
                                                                           class
                                                                     DBWriteLock

------------------------------------------------------------------------------*/

/**
 *
 * <p>A DBWriteLock is established on one or more DBObjectBases to prevent any
 * other threads from reading or writing to the database.  When a DBWriteLock
 * is established on a DBObjectBase, the establishing thread suspends until
 * all readers currently working in the specified DBObjectBases complete.  The
 * write lock is then established, and the thread possessing the DBWriteLock
 * is free to replace objects in the DBStore with modified copies.</p>
 *
 * <p>DBWriteLocks are typically created and managed by the code in the editSet
 * class.  It is very important that any thread that obtains a DBWriteLock be
 * scrupulous about releasing the lock in a timely fashion once the
 * appropriate changes are made in the database. </p>
 *
 * @see arlut.csd.ganymede.DBEditSet
 * @see arlut.csd.ganymede.DBObjectBase
 *
 */

public class DBWriteLock extends DBLock {

  static final boolean debug = false;

  private Object key;
  private DBStore lockManager;
  private Vector baseSet;
  private boolean 
    locked = false,
    abort = false;

  boolean inEstablish = false;

  /* -- */

  /**
   *
   * constructor to get a write lock on all the object bases
   *
   */

  public DBWriteLock(DBStore lockManager)
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    this.key = null;
    this.lockManager = lockManager;
    baseSet = new Vector();

    synchronized (lockManager)
      {
	enum = lockManager.objectBases.elements();
	
	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	    baseSet.addElement(base);
	  }

	lockManager.notifyAll();
      }
  }

  /**
   *
   * constructor to get a write lock on a subset of the
   * object bases.
   *
   */

  public DBWriteLock(DBStore lockManager, Vector baseSet)
  {
    this.key = null;
    this.lockManager = lockManager;
    this.baseSet = baseSet;
  }

  /**
   *
   * Establish a dump lock on bases specified in this DBDumpLock's
   * constructor.  Can throw InterruptedException if another thread
   * orders us to abort() while we're waiting for permission to
   * proceed with reads on the specified baseset.
   *
   */

  public void establish(Object key) throws InterruptedException
  {
    boolean done, okay;
    DBObjectBase base;

    /* -- */

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): enter");
	System.err.println(key + ": DBWriteLock.establish(): baseSet vector size " + baseSet.size());
      }

    synchronized (lockManager)
      {
	while (lockManager.lockHash.containsKey(key))
	  {
	    if (debug)
	      {
		System.err.println("DBWriteLock: Spinning waiting for existing lock with same key to be released");
	      }

	    if (abort)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.removeWriter(this);
		  }

		inEstablish = false;
		lockManager.notifyAll();
		throw new InterruptedException();
	      }
	    else
	      {
		lockManager.wait(500);	// we might have to wait for multiple readers on our key to release
	      }
	  }

	lockManager.lockHash.put(key, this);

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): added myself to the DBStore lockHash.");
	  }

	this.key = key;
	inEstablish = true;

	done = false;

	// wait until there are no dumpers 

	do
	  {
	    if (abort)
	      {
		lockManager.lockHash.remove(key);
		key = null;
		inEstablish = false;
		lockManager.notifyAll();
		throw new InterruptedException();
	      }

	    okay = true;

	    if (lockManager.schemaEditInProgress)
	      {
		okay = false;
	      }
	    else
	      {
		for (int i = 0; okay && (i < baseSet.size()); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);

		    if (!base.isDumperEmpty())
		      {
			if (debug)
			  {
			    System.err.println(key + ": DBWriteLock.establish(): waiting for dumpers on base " + 
					       base.object_name);
			    System.err.println(key + ": DBWriteLock.establish(): dumperList size: " + 
					       base.getDumperSize());
			  }
			okay = false;
		      }
		  }
	      }

	    if (!okay)
	      {
		try
		  {
		    lockManager.wait(500);
		  }
		catch (InterruptedException ex)
		  {
		    lockManager.lockHash.remove(key);
		    inEstablish = false;
		    lockManager.notifyAll();
		    throw ex;
		  }
	      }

	  } while (!okay);	// waiting for dumpers / schema editing to clear out

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): no dumpers queued.");
	  }

	// add our selves to the ObjectBase write queues

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
 	    base.addWriter(this);
	  }

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): added ourself to the writerList.");
	  }

	// spinwait until we can get into all of the ObjectBases
	// note that since we added ourselves to the writer
	// queues, we know the dumpers are waiting until we
	// finish. 

	while (!done)
	  {
	    if (debug)
	      {
		System.err.println(key + ": DBWriteLock.establish(): spinning.");
	      }

	    if (abort)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.removeWriter(this);
		  }

		lockManager.lockHash.remove(key);
		key = null;
		inEstablish = false;
		lockManager.notifyAll();
		throw new InterruptedException();
	      }

	    okay = true;

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		if (base.writeInProgress || !base.isReaderEmpty())
		  {
		    if (debug)
		      {
			if (!base.isReaderEmpty())
			  {
			    System.err.println(key +
					       ": DBWriteLock.establish(): " +
					       "waiting for readers to release.");
			  }
			else if (base.writeInProgress)
			  {
			    System.err.println(key +
					       ": DBWriteLock.establish(): " + 
					       "waiting for writer to release.");
			  }
		      }
		    okay = false;
		  }
	      }

	    // at this point, okay == true only if we were able to
	    // verify that no bases have writeInProgress to be true.
	    // Note that we don't try to insure that writers write in
	    // the order they were put into the writerList, since this
	    // may vary from base to base

	    if (okay)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.writeInProgress = true;
		    base.currentLock = this;
		  }
		
		done = true;
	      }
	    else
	      {
		try
		  {
		    lockManager.wait(500);
		  }
		catch (InterruptedException ex)
		  {
		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);
			base.removeWriter(this);
		      }
	
		    lockManager.lockHash.remove(key);
		    key = null;
		    inEstablish = false;
		    lockManager.notifyAll();
		    throw ex;
		  }
	      }
	  } // while (!done)

	locked = true;
	inEstablish = false;
	lockManager.addLock();	// notify consoles
	lockManager.notifyAll();

      } // synchronized(lockManager)

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): got the lock.");
      }
  }

  /**
   *
   * Release this lock on all bases locked
   *
   */

  public void release()
  {
    DBObjectBase base;

    /* -- */

    synchronized (lockManager)
      {
	while (inEstablish)
	  {
	    try
	      {
		lockManager.wait(500);
	      } 
	    catch (InterruptedException ex)
	      {
	      }
	  }

	// note that we have to check locked here or else we might accidentally
	// release somebody else's lock below

	if (!locked)
	  {
	    return;
	  }

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
	    base.removeWriter(this);
	    base.writeInProgress = false;
	    base.currentLock = null;
	  }

	locked = false;
	lockManager.lockHash.remove(key);
	key = null;
	lockManager.removeLock();	// notify consoles
	lockManager.notifyAll();	// many readers may want in
      }
  }

  /**
   *
   * Withdraw this lock.  This method can be called by a thread to
   * interrupt a lock establish that is blocked waiting to get
   * access to the appropriate set of DBObjectBase objects.  If
   * this method is called while another thread is blocked in
   * establish(), establish() will throw an InterruptedException.
   *
   * Once abort() is processed, this lock may never be established.
   * Any subsequent calls to estabish() will always throw
   * InterruptedException.
   *
   */
  
  public void abort()
  {
    synchronized (lockManager)
      {
	abort = true;
	lockManager.notifyAll();
	release();
      }
  }

  /**
   *
   * Returns true if the lock has been established and not
   * yet aborted / released.
   *
   */

  boolean isLocked()
  {
    return locked;
  }

  /**
   *
   * Returns true if <base> is locked by this lock.
   *
   */

  boolean isLocked(DBObjectBase base)
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

    return false;
  }

  /**
   *
   * Returns the key that this lock is established with,
   * or null if the lock has not been established.
   *
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
  
}
