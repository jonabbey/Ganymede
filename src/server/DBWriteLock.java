/*
   GASH 2

   DBWriteLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

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
 * @see csd.DBStore.DBEditSet
 * @see csd.DBStore.DBObjectBase
 *
 */

public class DBWriteLock extends DBLock {

  static final boolean debug = true;

  Enumeration enum;
  boolean done, okay;
  DBStore lockManager;
  DBObjectBase base;
  Vector baseSet;
  Object key;
  
  private boolean locked;

  /* -- */

  // constructor to get a write lock on all the object
  // bases.

  public DBWriteLock(DBStore lockManager)
  {
    this.key = null;
    this.lockManager = lockManager;
    baseSet = new Vector();

    enum = lockManager.objectBases.elements();
	    
    while (enum.hasMoreElements())
      {
	base = (DBObjectBase) enum.nextElement();
	baseSet.addElement(base);
      }

    locked = false;
  }

  // constructor to get a write lock on a subset of the
  // object bases.

  public DBWriteLock(DBStore lockManager, Vector baseSet)
  {
    this.key = null;
    this.lockManager = lockManager;
    this.baseSet = baseSet;
    locked = false;
  }

  // establish the lock

  public void establish(Object key)
  {
    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): enter");
	System.err.println(key + ": DBWriteLock.establish(): baseSet vector size " + baseSet.size());
      }

    done = false;

    if (lockManager.lockHash.containsKey(key))
      {
	throw new RuntimeException("Error: lock sought by owner of existing lockset.");
      }

    lockManager.lockHash.put(key, this);

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): added myself to the DBStore lockHash.");
      }

    this.key = key;

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): entering DBStore synchronized block.");
      }

    synchronized (lockManager)
      {
	// wait until there are no dumpers 

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): entered DBStore synchronized block.");
	  }

	do
	  {
	    okay = true;

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		if (base.dumperList.size() > 0)
		  {
		    if (debug)
		      {
			System.err.println(key + ": DBWriteLock.establish(): waiting for dumpers on base " + base.object_name);
			System.err.println(key + ": DBWriteLock.establish(): dumperList size: " + base.dumperList.size());
		      }
 		    okay = false;
		  }
	      }

	    if (!okay)
	      {
		try
		  {
		    lockManager.wait();
		  }
		catch (InterruptedException ex)
		  {

		  }
	      }

	  } while (!okay);

	if (debug)
	  {
	    System.err.println(key + ": DBWriteLock.establish(): no dumpers queued.");
	  }

	// add our selves to the ObjectBase write queues

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
 	    base.writerList.addElement(this);
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

	    okay = true;
	    enum = lockManager.objectBases.elements();

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		if (base.writeInProgress || base.readerList.size() > 0)
		  {
		    if (debug)
		      {
			if (base.readerList.size() > 0)
			  {
			    System.err.println(key + ": DBWriteLock.establish(): waiting for readers to release.");
			  }
			else if (base.writeInProgress)
			  {
			    System.err.println(key + ": DBWriteLock.establish(): waiting for writer to release.");
			  }
		      }
		    okay = false;
		  }
	      }

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
		    lockManager.wait();
		  }
		catch (InterruptedException ex)
		  {
		  }
	      }
	  }
      }

    locked = true;

    if (debug)
      {
	System.err.println(key + ": DBWriteLock.establish(): got the lock.");
      }

  }

  public void release()
  {
    if (!locked)
      {
	return;
      }

    synchronized (lockManager)
      {
	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
	    base.writerList.removeElement(this);
	    base.writeInProgress = false;
	    base.currentLock = null;
	  }

	locked = false;
	lockManager.lockHash.remove(key);
	key = null;

	lockManager.notifyAll();	// many readers may want in
      }
  }

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
  
}
