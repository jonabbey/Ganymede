/*
   GASH 2

   DBReadLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.3 $ %D%
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

  Enumeration enum;
  DBStore lockManager;
  DBObjectBase base;
  Vector baseSet;
  Object key;
  
  private boolean locked = false, abort = false, inEstablish = false;

  /* -- */

  public DBReadLock(DBStore lockManager)
  {
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

  // constructor to get a read lock on a subset of the
  // object bases.

  public DBReadLock(DBStore lockManager, Vector baseSet)
  {
    key = null;
    this.lockManager = lockManager;
    this.baseSet = baseSet;
  }

  public void establish(Object key) throws InterruptedException
  {
    boolean done, okay;

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
		    
		if (base.writerList.size() > 0)
		  {
		    okay = false;
		  }
	      }

	    if (okay)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.readerList.addElement(this);
		  }

		done = true;
		inEstablish = false;
	      }
	    else
	      {
		lockManager.wait(); // an InterruptedException here gets propagated up
	      }
	  } // while (!done)

	locked = true;
	lockManager.notifyAll(); // let a thread trying to release this lock proceed

      }	// synchronized (lockManager)
  }

  public void release()
  {
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
	    base.readerList.removeElement(this);
	  }

	locked = false;
	lockManager.lockHash.remove(key);
	key = null;

	lockManager.notifyAll();
      }
  }

  public void abort()
  {
    synchronized (lockManager)
      {
	abort = true;
	lockManager.notifyAll();
      }

    release();
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
