/*
   GASH 2

   DBReadLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.18 $
   Last Mod Date: $Date: 1999/12/14 23:44:14 $
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
                                                                      DBReadLock

------------------------------------------------------------------------------*/

/**
 * <P>DBReadLock is a class used in the Ganymede server to represent a read lock on
 * one or more {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} objects.  A
 * DBReadLock is used in the
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} class to guarantee
 * that all query operations go from start to finish without any changes being made
 * along the way.</P>
 *
 * <P>While a DBReadLock is established on a DBObjectBase, no changes may be made
 * to that base.  The {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}'s
 * {@link arlut.csd.ganymede.DBWriteLock#establish(java.lang.Object) establish()}
 * method will suspend until all read locks on a base are cleared.  As soon as
 * a thread attempts to establish a DBWriteLock on a base, no more DBReadLocks
 * will be established on that base until the DBWriteLock is cleared, but any
 * DBReadLocks already established will persist until released, whereupon the
 * DBWriteLock will establish.</P>
 *
 * <P>See {@link arlut.csd.ganymede.DBLock DBLock},
 * {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}, and
 * {@link arlut.csd.ganymede.DBDumpLock DBDumpLock} for details.</P>
 */

public class DBReadLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   *
   * constructor to get a read lock on all the object bases
   *
   */

  public DBReadLock(DBStore lockManager)
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    key = null;
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
  }

  /**
   *
   * constructor to get a read lock on a subset of the
   * object bases.
   *
   */

  public DBReadLock(DBStore lockManager, Vector baseSet)
  {
    key = null;
    this.lockManager = lockManager;
    this.baseSet = baseSet;
  }

  /**
   * <P>Establish a read lock on bases specified in this DBReadLock's
   * constructor.  Can throw InterruptedException if another thread
   * orders us to abort() while we're waiting for permission to
   * proceed with reads on the specified baseset.</P>
   */

  public void establish(Object key) throws InterruptedException
  {
    boolean done, okay;
    DBObjectBase base;
    Vector vect;
    Object obj;

    /* -- */

    synchronized (lockManager)
      {
	try
	  {
	    if (lockManager.lockHash.containsKey(key))
	      {
		obj = lockManager.lockHash.get(key);

		if (obj instanceof Vector)
		  {
		    // we've got a vector, indicating that we have another read lock set
		    // open.  It's okay to add another item to this vector, since we
		    // only do read locks in the GanymedeSession query() and dump() methods.
		    // These methods have no possibility of deadlock, so we don't care.

		    vect = (Vector) obj;

		    vect.addElement(this);

		    if (debug)
		      {
			// Ganymede.debug("DBReadLock (" + key + "):  added this to lockHash vector");

			System.err.println("DBReadLock (" + key + "):  added this to lockHash vector");
		      }
		  }
		else
		  {
		    // we've got a write lock or a dump lock.. it's ok to hold off here.

		    if (debug)
		      {
			// Ganymede.debug("DBReadLock (" + key + "):  dump or write lock blocking us");

			System.err.println("DBReadLock (" + key + "):  dump or write lock blocking us");
		      }
		
		    throw new RuntimeException("Error: read lock sought by owner of existing write or dump lockset.");
		  }
	      }
	    else
	      {
		vect = new Vector();

		vect.addElement(this);
		lockManager.lockHash.put(key, vect);

		if (debug)
		  {
		    // Ganymede.debug("DBReadLock (" + key + "):  initialized lockHash vector");

		    System.err.println("DBReadLock (" + key + "):  initialized lockHash vector");
		  }
	      }

	    this.key = key;

	    inEstablish = true;

	    done = false;

	    while (!done)
	      {
		// if we've received an abort notification, bail.

		if (debug)
		  {
		    // Ganymede.debug("DBReadLock (" + key + "):  looping to get establish permission");

		    System.err.println("DBReadLock (" + key + "):  looping to get establish permission");
		  }

		if (abort)
		  {
		    vect = (Vector) lockManager.lockHash.get(key);
		    vect.removeElement(this);

		    if (vect.size() == 0)
		      {
			lockManager.lockHash.remove(key);
		      }

		    if (debug)
		      {
			// Ganymede.debug("DBReadLock (" + key + "):  aborting before permission granted");

			System.err.println("DBReadLock (" + key + "):  aborting before permission granted");
		      }

		    inEstablish = false;
		    throw new InterruptedException();
		  }

		// assume we can proceed to get our lock until we find out
		// otherwise

		okay = true;

		// if the schema is being edited, we can't proceed

		if (lockManager.schemaEditInProgress)
		  {
		    if (debug)
		      {
			System.err.println("DBReadLock (" + key + "):  schema editor is in progress");
		      }

		    okay = false;
		  }

		// if there are any writers queued, we have to wait
		// for them to finish before we can proceed

		for (int i = 0; okay && (i < baseSet.size()); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		
		    // check for writers.  we don't care about dumpers, since
		    // we can read without problems while a dump lock is held
		    
		    if (!base.isWriterEmpty())
		      {
			if (debug)
			  {
			    System.err.println("DBReadLock (" + key + "):  base " + base.getName() + " has writers queued");
			  }

			okay = false;
		      }
		  }

		if (okay)
		  {
		    for (int i = 0; i < baseSet.size(); i++)
		      {
			base = (DBObjectBase) baseSet.elementAt(i);
			base.addReader(this);
		      }

		    done = true;
		  }
		else
		  {
		    if (debug)
		      {
			// Ganymede.debug("DBReadLock (" + key + "):  waiting on lockManager");

			System.err.println("DBReadLock (" + key + "):  waiting on lockManager");
		      }
		 
		    try
		      {
			lockManager.wait(2500); // an InterruptedException here gets propagated up

			if (debug)
			  {
			    // Ganymede.debug("DBReadLock (" + key + "):  done waiting on lockManager");

			    System.err.println("DBReadLock (" + key + "):  done waiting on lockManager");
			  }
		      }
		    catch (InterruptedException ex)
		      {
			if (debug)
			  {
			    // Ganymede.debug("DBReadLock (" + key + "):  interrupted exception");

			    System.err.println("DBReadLock (" + key + "):  interrupted exception");
			  }

			vect = (Vector) lockManager.lockHash.get(key);
			vect.removeElement(this);

			if (vect.size() == 0)
			  {
			    lockManager.lockHash.remove(key);
			  }

			inEstablish = false;
			throw ex;
		      }
		  }
	      } // while (!done)

	    locked = true;
	    inEstablish = false;
	    lockManager.addLock();	// notify consoles

	    if (debug)
	      {
		System.err.println("DBReadLock (" + key + "):  read lock established");
	      }
	  }
	finally
	  {
	    lockManager.notifyAll(); // let a thread trying to release this lock proceed
	  }
      }	// synchronized (lockManager)
  }

  /**
   * <P>Relinquish the lock on bases held by this lock object.</P>
   *
   * <P>Should be called by {@link arlut.csd.ganymede.DBSession DBSession}'s
   * {@link arlut.csd.ganymede.DBSession#releaseLock(arlut.csd.ganymede.DBLock) releaseLock()}
   * method.</P>
   */

  public void release()
  {
    DBObjectBase base;

    /* -- */

    if (debug)
      {
	// Ganymede.debug("DBReadLock (" + key + "):  attempting release");

	System.err.println("DBReadLock (" + key + "):  attempting release");
      }

    synchronized (lockManager)
      {
	try
	  {
	    while (inEstablish)
	      {
		if (debug)
		  {
		    // Ganymede.debug("DBReadLock (" + key + "):  release() looping waiting on inEstablish");

		    System.err.println("DBReadLock (" + key + "):  release() looping waiting on inEstablish");
		  }

		try
		  {
		    lockManager.wait(2500);
		  } 
		catch (InterruptedException ex)
		  {
		  }
	      }

	    if (!locked)
	      {
		if (debug)
		  {
		    // Ganymede.debug("DBReadLock (" + key + "):  release() not locked, returning");

		    System.err.println("DBReadLock (" + key + "):  release() not locked, returning");
		  }

		return;
	      }

	    for (int i = 0; i < baseSet.size(); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		base.removeReader(this);
	      }

	    locked = false;
	    lockManager.lockHash.remove(key);
	    key = null;

	    if (debug)
	      {
		// Ganymede.debug("DBReadLock (" + key + "):  release() released");

		System.err.println("DBReadLock (" + key + "):  release() released");
	      }

	    lockManager.removeLock();	// notify consoles
	  }
	finally
	  {
	    lockManager.notifyAll();
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
   * Any subsequent calls to establish() will always throw
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
