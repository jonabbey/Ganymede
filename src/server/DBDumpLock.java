/*
   GASH 2

   DBDumpLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBDumpLock

------------------------------------------------------------------------------*/

/**
 *
 * DBDumpLock is an object used to lock the DBStore for the purpose of
 * dumping the database.  A DBDumpLock establish request has lower
 * priority than DBWriteLock requests, but once a DBDumpLock establish
 * request is submitted, no new DBWriteLock can be established until
 * the dumping thread has completed the dump and released the lock.
 *
 * DBReadLock's can be established while a DBDumpLock is active.
 *
 */

class DBDumpLock extends DBLock {

  static final boolean debug = false;

  private Object key;
  private DBStore lockManager;
  private Vector baseSet;
  private boolean 
    locked = false,
    abort = false, 
    inEstablish = false;

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
	enum = lockManager.objectBases.elements();
	    
	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	    baseSet.addElement(base);
	  }    

	lockManager.notifyAll();
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

    synchronized (lockManager)
      {
	done = false;

	if (lockManager.lockHash.containsKey(key))
	  {
	    lockManager.notifyAll();
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
		lockManager.notifyAll();
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
		    lockManager.notifyAll();
		    throw ex;
		  } 
	      }
	  }

	inEstablish = false;
	locked = true;
	lockManager.addLock();	// notify consoles
	lockManager.notifyAll(); // let a thread trying to release this lock proceed

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
	    lockManager.notifyAll();
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
	lockManager.notify();
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
