/*
   GASH 2

   DBReadLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.11 $ %D%
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

  static final boolean debug = true;

  // --

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

	lockManager.notifyAll();
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
    Vector vect;
    Object obj;

    /* -- */

    synchronized (lockManager)
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
		    Ganymede.debug("DBReadLock (" + key + "):  added this to lockHash vector");
		  }
	      }
	    else
	      {
		// we've got a write lock or a dump lock.. it's ok to hold off here.

		if (debug)
		  {
		    Ganymede.debug("DBReadLock (" + key + "):  dump or write lock blocking us");
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
		Ganymede.debug("DBReadLock (" + key + "):  initialized lockHash vector");
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
		Ganymede.debug("DBReadLock (" + key + "):  looping to get establish permission");
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
		    Ganymede.debug("DBReadLock (" + key + "):  aborting before permission granted");
		  }

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
		Ganymede.debug("DBReadLock (" + key + "):  waiting on lockManager");

		try
		  {
		    lockManager.wait(500); // an InterruptedException here gets propagated up

		    Ganymede.debug("DBReadLock (" + key + "):  done waiting on lockManager");
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("DBReadLock (" + key + "):  interrupted exception");

		    vect = (Vector) lockManager.lockHash.get(key);
		    vect.removeElement(this);

		    if (vect.size() == 0)
		      {
			lockManager.lockHash.remove(key);
		      }

		    inEstablish = false;
		    lockManager.notifyAll();
		    throw ex;
		  }
	      }
	  } // while (!done)

	locked = true;
	inEstablish = false;
	lockManager.addLock();	// notify consoles
	lockManager.notifyAll(); // let a thread trying to release this lock proceed

	if (debug)
	  {
	    Ganymede.debug("DBReadLock (" + key + "):  read lock established");
	  }
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

    if (debug)
      {
	Ganymede.debug("DBReadLock (" + key + "):  attempting release");
      }

    synchronized (lockManager)
      {
	while (inEstablish)
	  {
	    if (debug)
	      {
		Ganymede.debug("DBReadLock (" + key + "):  release() looping waiting on inEstablish");
	      }

	    try
	      {
		lockManager.wait(500);
	      } 
	    catch (InterruptedException ex)
	      {
	      }
	  }

	if (!locked)
	  {
	    if (debug)
	      {
		Ganymede.debug("DBReadLock (" + key + "):  release() not locked, returning");
	      }

	    lockManager.notifyAll();

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
	    Ganymede.debug("DBReadLock (" + key + "):  release() released");
	  }

	lockManager.removeLock();	// notify consoles
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
