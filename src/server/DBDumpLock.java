/*
   GASH 2

   DBDumpLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBDumpLock

------------------------------------------------------------------------------*/

class DBDumpLock extends DBLock {

  static final boolean debug = true;

  DBStore lockManager;
  Enumeration enum;
  boolean done, okay;
  DBObjectBase base;
  Vector baseSet;
  Object key;
  
  private boolean locked;

  /* -- */

  public DBDumpLock(DBStore lockManager)
  {
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

  // constructor to get a dump lock on a subset of the
  // object bases.

  public DBDumpLock(DBStore lockManager, Vector baseSet)
  {
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
	// add our selves to the ObjectBase dump queues

	enum = lockManager.objectBases.elements();

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
	    base.dumperList.addElement(this);
	  }

	while (!done)
	  {
	    okay = true;
	    enum = lockManager.objectBases.elements();

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);

		if (base.writerList.size() > 0 || base.dumpInProgress)
		  {
		    okay = false;
		  }
	      }

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
	    base.dumperList.removeElement(this);
	    base.dumpInProgress = false;
	    base.currentLock = null;
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
