/*
   GASH 2

   DBEditSet.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import csd.DBStore.*;
import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBEditSet

DBEditSet is the basic transactional unit.  All changes to the database during
normal operations are made in the context of a DBEditSet, which may then be
committed or rolledback as an atomic operation.

When DBEditSet commits, the datastore is changed, and information is written
to the DBStore journal. 

------------------------------------------------------------------------------*/

public class DBEditSet {

  Vector 
    objectsChanged, 
    objectsCreated, 
    objectsDeleted;

  Hashtable basesModified;

  DBStore dbStore;

  Object key;

  /* -- */

  public DBEditSet(DBStore dbStore, Object key)
  {
    this.key = key;
    this.dbStore = dbStore;
    objectsChanged = new Vector();
    objectsCreated = new Vector();
    objectsDeleted = new Vector();
    basesModified = new Hashtable(dbStore.basecount);
  }

  synchronized void addChangedObject(DBEditObject object)
  {
    objectsChanged.addElement(object);
    
    // just need something to mark the slot in the hash table,
    // this is as convenient as anything

    basesModified.put(object.objectBase, this);	
  }

  synchronized void releaseChangedObject(DBEditObject object)
  {
    objectsChanged.removeElement(object);

    // we'll leave the basesModified hash alone, since we
    // don't know if any other objects in the base were
    // touched.
  }

  synchronized void addCreatedObject(DBEditObject object)
  {
    objectsCreated.addElement(object);

    // just need something to mark the slot in the hash table,
    // this is as convenient as anything

    basesModified.put(object.objectBase, this);	
  }

  synchronized releaseCreatedObject(DBEditObject object)
  {
    objectsCreated.removeElement(object);

    // we'll leave the basesModified hash alone, since we
    // don't know if any other objects in the base were
    // touched.
  }

  synchronized void addDeletedObject(DBObject object)
  {
    objectsDeleted.addElement(object);

    // just need something to mark the slot in the  hash table,
    // this is as convenient as anything

    basesModified.put(object.objectBase, this);	
  }

  synchronized releaseDeletedObject(DBObject object)
  {
    objectsDeleted.removeElement(object);

    // we'll leave the basesModified hash alone, since we
    // don't know if any other objects in the base were
    // touched.
  }

  synchronized public void commit()
  {
    DBWriteLock wLock;
    Vector baseSet;
    Enumeration enum;

    /* -- */

    baseSet = new Vector();
    enum = basesModified.keys();

    while (enum.hasMoreElements())
      {
	baseSet.addElement(enum.nextElement);
      }

    wLock = new DBWriteLock(dbStore, baseSet);
    wLock.establish(key);	// wait for write lock

    // need to iterate through vectors, emit dumplings
    // and clear DBEditObject's from DBObjectBases

    wLock.release();
  }

  synchronized public void release()
  {
    // need to iterate through vectors, clear DBObjectBase
    // references
  }
}
