/*
   GASH 2

   DBDumpLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 2000/12/06 09:59:39 $
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
                                                                      DBDumpLock

------------------------------------------------------------------------------*/

/**
 * <P>DBDumpLock is a {@link arlut.csd.ganymede.DBLock DBLock} object used to lock the
 * {@link arlut.csd.ganymede.DBStore DBStore} for the purpose of
 * dumping the database.  A DBDumpLock establish request has lower
 * priority than {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}
 * requests, but once a DBDumpLock establish
 * request is submitted, no new DBWriteLock can be established until
 * the dumping thread has completed the dump and released the lock.</P>
 *
 * <P>{@link arlut.csd.ganymede.DBReadLock DBReadLock}'s can be established
 * while a DBDumpLock is active.</P>
 */

class DBDumpLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   *
   * constructor to get a dump lock on all the object bases
   *
   */

  public DBDumpLock(DBStore store)
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    this.lockSync = store.lockSync;
    baseSet = store.getBases();

    locked = false;
  }

  /**
   *
   * constructor to get a dump lock on a subset of the
   * object bases.
   *
   */

  public DBDumpLock(DBStore store, Vector baseSet)
  {
    this.lockSync = store.lockSync;
    this.baseSet = baseSet;

    locked = false;
  }

  /**
   * <P>Establish a dump lock on bases specified in this DBDumpLock's
   * constructor.  Can throw InterruptedException if another thread
   * orders us to abort() while we're waiting for permission to
   * proceed with reads on the specified baseset.</P>
   */

  public void establish(Object key) throws InterruptedException
  {
    boolean added = false;
    boolean done, okay;
    DBObjectBase base;

    /* -- */

    synchronized (lockSync)
      {
	try
	  {
	    done = false;

	    if (lockSync.isLockHeld(key))
	      {
		throw new RuntimeException("Error: dump lock sought by owner of existing lockset.");
	      }

	    lockSync.setDumpLockHeld(key, this);
	    this.key = key;
	    inEstablish = true;

	    // add ourselves to the ObjectBase dump queues..  we don't
	    // have to wait for anything to do this.. it's up to the
	    // writers to hold off on adding themselves to the
	    // writerlists until the dumperlists are empty.

	    for (int i = 0; i < baseSet.size(); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		base.addDumper(this);
	      }

	    added = true;

	    while (!done)
	      {
		okay = true;

		if (abort)
		  {
		    throw new InterruptedException("DBDumpLock (" + key + "):  establish aborting before permission granted");
		  }

		// see if we can establish a dump lock on all the bases.. if
		// isWriterEmpty() is not true, that base needs to wait for
		// its slate of writers to release and drain

		for (int i = 0; okay && (i < baseSet.size()); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);

		    // note that the writer locks are polite enough to
		    // wait for us if we are queued in the dump wait
		    // queue, which we are at this point.  so we just
		    // have to wait for the writer that has this base
		    // locked and any of his buddies on the write wait
		    // list to finish up

		    if (!base.isWriterEmpty() || base.writeInProgress)
		      {
			okay = false;
		      }
		  }

		// if okay, we know that none of the bases we're
		// concerned with have writers queued or locked.. we
		// can go ahead and lock the bases.

		if (okay)
		  {
		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);

			// base.addDumpLock() actually records this
			// DBDumpLock as being established, and not
			// just in the dumper wait queue

			base.addDumpLock(this);
		      }

		    done = true;
		  }
		else
		  {
		    try
		      {
			lockSync.wait(2500); // or until notify'ed
		      }
		    catch (InterruptedException ex)
		      {
			throw ex; // finally will clean up
		      } 
		  }
	      }

	    locked = true;
	    lockSync.addLock();	// notify consoles
	  }
	finally
	  {
	    inEstablish = false;

	    if (added)
	      {
		// either we're locked or we're not going to lock,
		// in either case we don't need to be on the dumper
		// wait queues any more

		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.removeDumper(this);
		  }
	      }

	    if (!locked)
	      {
		lockSync.clearLockHeld(key);
		this.key = null;
	      }

	    lockSync.notifyAll(); // let a thread trying to release this lock proceed
	  }
      } // synchronized (lockSync)
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
	while (inEstablish)
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
	
	if (!locked)
	  {
	    return;
	  }
	
	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
	    base.removeDumpLock(this);
	  }
	
	locked = false;
	lockSync.clearLockHeld(key);
	
	key = null;		// gc
	
	lockSync.removeLock();	// notify consoles
	lockSync.notifyAll();	// many threads might want to check to see what we freed
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
	abort = true;
	release();
      }
  }
}
