/*
   GASH 2

   DBDumpLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.12 $
   Last Mod Date: $Date: 1999/12/14 23:44:13 $
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

  public DBDumpLock(DBStore lockManager)
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    this.lockManager = lockManager;
    baseSet = new Vector();

    synchronized (lockManager)
      {
	try
	  {
	    enum = lockManager.objectBases.elements();
	    
	    while (enum.hasMoreElements())
	      {
		base = (DBObjectBase) enum.nextElement();
		baseSet.addElement(base);
	      }    
	  }
	finally
	  {
	    lockManager.notifyAll();
	  }
      }

    locked = false;
  }

  /**
   *
   * constructor to get a dump lock on a subset of the
   * object bases.
   *
   */

  public DBDumpLock(DBStore lockManager, Vector baseSet)
  {
    this.lockManager = lockManager;
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
    boolean done, okay;
    DBObjectBase base;

    /* -- */

    synchronized (lockManager)
      {
	try
	  {
	    done = false;

	    if (lockManager.lockHash.containsKey(key))
	      {
		throw new RuntimeException("Error: lock sought by owner of existing lockset.");
	      }

	    lockManager.lockHash.put(key, this);
	    this.key = key;
	    inEstablish = true;

	    // add ourselves to the ObjectBase dump queues..
	    // we don't have to wait for anything to do this.. it's
	    // up to the writers to hold off on adding themselves to
	    // the writerlists until the dumperlist is empty.

	    for (int i = 0; i < baseSet.size(); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		base.addDumper(this);
	      }

	    while (!done)
	      {
		okay = true;

		if (abort)
		  {
		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);
			base.removeDumper(this);
		      }
		
		    inEstablish = false;
		    key = null;
		    lockManager.lockHash.remove(key);

		    throw new InterruptedException();
		  }

		if (lockManager.schemaEditInProgress)
		  {
		    okay = false;
		  }
		else
		  {
		    for (int i = 0; okay && (i < baseSet.size()); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);

			if (!base.isWriterEmpty() || base.dumpInProgress)
			  {
			    okay = false;
			  }
		      }
		  }

		// if okay, we know that none of the bases we're concerned
		// with have writers queued or dumps in progress.. we can
		// go ahead and lock the bases.

		if (okay)
		  {
		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);
			base.dumpInProgress = true;
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
			    base.removeDumper(this);
			  }
			lockManager.lockHash.remove(key);

			throw ex;
		      } 
		  }
	      }

	    inEstablish = false;
	    locked = true;
	    lockManager.addLock();	// notify consoles
	  }
	finally
	  {
	    lockManager.notifyAll(); // let a thread trying to release this lock proceed
	  }

      } // synchronized (lockManager)
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
	try
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
		base.removeDumper(this);
		base.dumpInProgress = false;
		base.currentLock = null;
	      }

	    locked = false;
	    lockManager.lockHash.remove(key);
	    key = null;

	    lockManager.removeLock();	// notify consoles
	  }
	finally
	  {
	    lockManager.notify();
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
    synchronized (lockManager)
      {
	try
	  {
	    abort = true;
	    release();
	  }
	finally
	  {
	    lockManager.notifyAll();
	  }
      }
  }
}
