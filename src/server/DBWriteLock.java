/*
   GASH 2

   DBWriteLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.20 $
   Last Mod Date: $Date: 2000/02/10 04:35:39 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
 * <p>A DBWriteLock is a {@link arlut.csd.ganymede.DBLock DBLock} subclass
 * used to lock one or more
 * {@link arlut.csd.ganymede.DBObjectBase DBObjectBases} for the purposes
 * of committing changes into those bases, preventing any
 * other threads from reading or writing to the database while the update
 * is being performed.  When a DBWriteLock
 * is established on a DBObjectBase, the establishing thread suspends until
 * all readers currently working in the specified DBObjectBases complete.  The
 * write lock is then established, and the thread possessing the DBWriteLock
 * is free to replace objects in the {@link arlut.csd.ganymede.DBStore DBStore}
 * with modified copies.</p>
 *
 * <p>DBWriteLocks are typically created and managed by the code in the 
 * {@link arlut.csd.ganymede.DBEditSet DBEditSet}
 * class.  It is very important that any thread that obtains a DBWriteLock be
 * scrupulous about releasing the lock in a timely fashion once the
 * appropriate changes are made in the database. </p>
 *
 * @see arlut.csd.ganymede.DBEditSet
 * @see arlut.csd.ganymede.DBObjectBase
 */

public class DBWriteLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   *
   * constructor to get a write lock on all the object bases
   *
   */

  public DBWriteLock(DBStore store)
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    this.key = null;
    this.lockSync = store.lockSync;
    baseSet = store.getBases();
  }

  /**
   *
   * constructor to get a write lock on a subset of the
   * object bases.
   *
   */

  public DBWriteLock(DBStore store, Vector baseSet)
  {
    this.key = null;
    this.lockSync = store.lockSync;
    this.baseSet = baseSet;
  }

  /**
   * <P>Establish a dump lock on bases specified in this DBWriteLock's
   * constructor.  Can throw InterruptedException if another thread
   * orders us to abort() while we're waiting for permission to
   * proceed with writes on the specified baseset.</P>
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

    synchronized (lockSync)
      {
	try
	  {
	    // okay, we're wanting to establish a write lock.. first
	    // we check to see if we already have a lock associated
	    // with the same lock key.. if so, we need to abort to
	    // avoid deadlock.  we don't support upgrading read locks
	    // to write locks.

	    if (lockSync.isLockHeld(key))
	      {
		throw new InterruptedException("DBWriteLock.establish(): error, lock already held for key: " + key);
	      }

	    lockSync.setWriteLockHeld(key, this);

	    if (debug)
	      {
		System.err.println(key + ": DBWriteLock.establish(): added myself to the DBLockSync lockHash.");
	      }

	    // okay, now we've got ourselves recorded as the only lock
	    // that can be in the process of establishing for this
	    // key.  record that fact so that we can interact with
	    // another thread trying to release us.

	    this.key = key;
	    inEstablish = true;

	    done = false;

	    // wait until there are no dumpers queued for establish,
	    // then queue up for own own turn at our bases

	    do
	      {
		if (abort)
		  {
		    lockSync.clearLockHeld(this.key);
		    this.key = null;

		    // the finally clause at bottom will clean up

		    throw new InterruptedException("aborted on command");
		  }

		okay = true;

		// if the server is in shutdown or in schema edit, we
		// can't proceed.  in theory, this check would be
		// taken care of above here, but internal sessions
		// don't use the login semaphore so that a delayed
		// shutdown won't wait around forever for
		// Ganymede.internalSession

		String disabledMessage = GanymedeServer.lSemaphore.checkEnabled();

		if (disabledMessage != null)
		  {
		    throw new InterruptedException(disabledMessage); // finally will clean up
		  }
		else
		  {
		    for (int i = 0; okay && (i < baseSet.size()); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);

			// if this base has any dumpers queued on it
			// (even if they don't yet hold an actual
			// lock), we have to wait.  once a write lock
			// is queued up, no new dumpers will queue up,
			// but all dumpers presently queued get in
			// before we do.

			if (!base.isDumperEmpty())
			  {
			    if (debug)
			      {
				System.err.println(this.key + ": DBWriteLock.establish(): waiting for dumpers on base " + 
						   base.object_name);
				System.err.println(this.key + ": DBWriteLock.establish(): dumperList size: " + 
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
			lockSync.wait(2500);
		      }
		    catch (InterruptedException ex)
		      {
			lockSync.clearLockHeld(this.key);

			throw ex; // finally at bottom will clean up
		      }
		  }

	      } while (!okay);	// waiting for dumpers / schema editing to clear out

	    if (debug)
	      {
		System.err.println(this.key + ": DBWriteLock.establish(): no dumpers queued.");
	      }

	    // okay, there are no dump locks waiting to establish.
	    // Add ourselves to the ObjectBase write queues.  This
	    // will prevent any read locks from establishing, but any
	    // readlocks that have already been granted will block us
	    // later.  The idea is for the granted read locks to drain
	    // away while we block out any new readers until we're
	    // through

	    for (int i = 0; i < baseSet.size(); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		base.addWriter(this);
	      }

	    if (debug)
	      {
		System.err.println(this.key + ": DBWriteLock.establish(): added ourself to the writerList.");
	      }

	    // spinwait until we can get into all of the ObjectBases
	    // note that since we added ourselves to the writer
	    // queues, we know that any new dump or read locks will
	    // wait until we finish.

	    while (!done)
	      {
		if (debug)
		  {
		    System.err.println(this.key + ": DBWriteLock.establish(): spinning.");
		  }

		if (abort)
		  {
		    // we've been told to abort by another thread's
		    // abort() call.  remove ourselves from all writer
		    // queues, dissociate from the key, get out of
		    // dodge.

		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);
			base.removeWriter(this);
		      }

		    lockSync.clearLockHeld(this.key);
		    this.key = null;

		    throw new InterruptedException("aborted on command");
		  }

		// we know no dumpers have anything locked because
		// we checked above, and once we added ourselves
		// to a base's writer queue, no dumper would ever
		// get the lock.
		
		// so, we just wait for readers and/or writers to drain
		// out and let us have the lock on all the bases we want.

		okay = true;

		for (int i = 0; okay && (i < baseSet.size()); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);

		    if (!base.isReaderEmpty() || base.writeInProgress)
		      {
			if (debug)
			  {
			    if (!base.isReaderEmpty())
			      {
				System.err.println(this.key +
						   ": DBWriteLock.establish(): " +
						   "waiting for readers to release.");
			      }
			    else if (base.writeInProgress)
			      {
				System.err.println(this.key +
						   ": DBWriteLock.establish(): " + 
						   "waiting for writer to release.");
			      }
			  }
			okay = false;
		      }
		  }

		// at this point, okay == true only if we were able to
		// verify that no bases we need have any locks on them.

		if (okay)
		  {
		    // Okay, we've got permission to lock all of the bases,
		    // let's do that.

		    // Note that we don't ever try to insure that
		    // writers are granted locks on bases in the order
		    // they originally queued up in any base writer
		    // list.  We can't do that, since different write
		    // locks pending will specify different sets of
		    // bases.  We just depend on the JVM's thread
		    // scheduler eventually hitting upon a write lock
		    // in establish() that is okay to proceed on all
		    // requested bases.

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
		    // Okay, something's locked us out.  Wait a few
		    // seconds and try again.

		    try
		      {
			lockSync.wait(2500);
		      }
		    catch (InterruptedException ex)
		      {
			// take us off all base lists, dissociate the key,
			// get out of dodge.

			for (int i = 0; i < baseSet.size(); i++)
			  {
			    base = (DBObjectBase) baseSet.elementAt(i);
			    base.removeWriter(this);
			  }
	
			lockSync.clearLockHeld(this.key);
			this.key = null;

			throw ex; // finally will clean up
		      }
		  }
	      } // while (!done)

	    locked = true;
	    lockSync.addLock();	// notify consoles
	  }
	finally
	  {
	    inEstablish = false; // in case we threw an exception establishing
	    lockSync.notifyAll(); // wake up threads waiting to establish or release
	  }

      } // synchronized(lockSync)

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

    synchronized (lockSync)
      {
	try
	  {
	    while (inEstablish)
	      {
		try
		  {
		    lockSync.wait(2500);
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
	    lockSync.clearLockHeld(key);

	    key = null;		// gc

	    lockSync.removeLock();	// notify consoles
	  }
	finally
	  {
	    lockSync.notifyAll();	// many readers may want in
	  }
      }
  }

  /**
   * <P>Withdraw this lock.  This method can be called by a thread to
   * interrupt a lock establish that is blocked waiting to get
   * access to the appropriate set of
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} objects.  If
   * this method is called while another thread is blocked in
   * establish(), establish() will throw an InterruptedException.</P>
   *
   * <P>Once abort() is processed, this lock may never be established.
   * Any subsequent calls to estabish() will always throw
   * InterruptedException.</P>
   */
  
  public void abort()
  {
    synchronized (lockSync)
      {
	try
	  {
	    abort = true;
	    release();		// blocks
	  }
	finally
	  {
	    lockSync.notifyAll();
	  }
      }
  }
}
