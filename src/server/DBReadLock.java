/*
   GASH 2

   DBReadLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.2 $ %D%
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
  boolean okay, done;
  DBStore lockManager;
  DBObjectBase base;
  Vector baseSet;
  Object key;
  
  private boolean locked;

  /* -- */

  public DBReadLock(DBStore lockManager)
  {
    key = null;
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

  // constructor to get a read lock on a subset of the
  // object bases.

  public DBReadLock(DBStore lockManager, Vector baseSet)
  {
    key = null;
    this.lockManager = lockManager;
    this.baseSet = baseSet;
    locked = false;
  }

  public void establish(Object key)
  {
    done = false;

    if (lockManager.lockHash.containsKey(key))
      {
	throw new RuntimeException("Error: lock sought by owner of existing lockset.");
      }

    lockManager.lockHash.put(key, this);
    this.key = key;

    synchronized (lockManager)
      {
	// wait until there are no writers blocking our access

	while (!done)
	  {
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
		    
		    if (base.writerList.size() > 0)
		      {
			okay = false;
		      }
		  }
	      }

	    if (okay)
	      {
		for (int i = 0; i < baseSet.size(); i++)
		  {
		    base = (DBObjectBase) baseSet.elementAt(i);
		    base.readerList.addElement(this);
		    // we don't need to set currentLock
		    // since readers are shared
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
	    base.readerList.removeElement(this);
	    // we don't need to clear currentLock
	    // since readers are shared
	  }

	locked = false;
	lockManager.lockHash.remove(key);
	key = null;

	lockManager.notify();
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
