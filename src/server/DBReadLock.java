/*
   GASH 2

   DBReadLock.java

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
                                                                           class
                                                                      DBReadLock

------------------------------------------------------------------------------*/

public class DBReadLock extends DBLock {

  private Object key;
  private DBStore lockManager;
  private Vector baseSet;
  private boolean locked = false, abort = false, inEstablish = false;

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
	enum = lockManager.objectBases.elements();
	    
	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	    baseSet.addElement(base);
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
   *
   * Establish a read lock on bases specified in this DBReadLock's
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
	if (lockManager.lockHash.containsKey(key))
	  {
	    throw new RuntimeException("Error: lock sought by owner of existing lockset.");
	  }
	
	lockManager.lockHash.put(key, this);
	this.key = key;

	inEstablish = true;

	done = false;

	while (!done)
	  {
	    // if we've received an abort notification, bail.

	    if (abort)
	      {
		lockManager.lockHash.remove(key);
		inEstablish = false;
		lockManager.notifyAll();
		throw new InterruptedException();
	      }

	    // assume we can proceed to get our lock until we find out
	    // otherwise

	    okay = true;

	    // if the schema is being edited, we can't proceed

	    if (lockManager.schemaEditInProgress)
	      {
		okay = false;
	      }

	    // if there are any writers queued, we have to wait
	    // for them to finish before we can proceed

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		    
		if (!base.isWriterEmpty())
		  {
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
		try
		  {
		    lockManager.wait(); // an InterruptedException here gets propagated up
		  }
		catch (InterruptedException ex)
		  {
		    lockManager.lockHash.remove(key);
		    inEstablish = false;
		    lockManager.notifyAll();
		    throw ex;
		  }
	      }
	  } // while (!done)

	locked = true;
	inEstablish = false;
	lockManager.notifyAll(); // let a thread trying to release this lock proceed

      }	// synchronized (lockManager)
  }

  /**
   *
   * Relinquish the lock on bases held by this lock object
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
		lockManager.wait();
	      } 
	    catch (InterruptedException ex)
	      {
	      }
	  }

	if (!locked)
	  {
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

	lockManager.notifyAll();
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
