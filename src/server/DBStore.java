/*
   GASH 2

   DBStore.java

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
                                                                         DBStore

------------------------------------------------------------------------------*/

public class DBStore {

  // type identifiers used in the object store

  static final short BOOLEAN = 0;
  static final short NUMERIC = 1;
  static final short DATE = 2;
  static final short STRING = 3;
  static final short INVID = 4;
  static final short BOOLEANARRAY = 5;
  static final short NUMERICARRAY = 6;
  static final short DATEARRAY = 7;
  static final short STRINGARRAY = 8;
  static final short INVIDARRAY = 9;

  static final String id_string = "Gstore";
  static final byte major_version = 0;
  static final byte minor_version = 0;

  /* - */

  Hashtable objectBases;	// hash mapping object type to DBObjectBase's
  Hashtable lockHash;		// identifier keys for current locks
  Vector nameSpaces;		// unique valued hashes

  /* -- */

  public DBStore()
  {
    objectBases = null;
    lockHash = null;
    nameSpaces = new Vector();
  }

  public load(String filename)
  {
    FileInputStream inStream;
    DataInputStream in;

    DBObjectBase tempBase;
    short baseCount;
    String file_id;
    byte file_major, file_minor;

    /* -- */

    try
      {
	inStream = new FileInputStream(filename);
	in = new DataInputStream(inStream);

	try
	  {
	    file_id = in.readUTF();
	    
	    if (!file_id.equals(id_string))
	      {
		System.err.println("DBStore initialization error: DBStore id mismatch for " + filename);
		throw new RuntimeException("DBStore initialization error (" + filename + ")");
	      }
	  }
	catch (IOException ex)
	  {
 	    System.err.println("DBStore initialization error: DBStore id read failure for " + filename);
	    System.err.println("IOException: " + ex);
	    throw new RuntimeException("DBStore initialization error (" + filename + ")");
	  }

	file_major = in.readByte();
	file_minor = in.readByte();

	if (file_major != major_version)
	  {
	    System.err.println("DBStore initialization error: major version mismatch");
	    throw new Error("DBStore initialization error (" + filename + ")");
	  }
	
	baseCount = in.readShort();
	
	objectBases = new Hashtable(baseCount * 1.5);
	
	for (short i = 0; i < baseCount; i++)
	  {
	    tempBase = new DBObjectBase(in, this);
	    
	    objectBases.put(new Integer(tempBase.type_code), tempBase);
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore initialization error: couldn't properly process " + filename);
	System.err.println("IOException: " + ex);
	throw new RuntimeException("DBStore initialization error (" + filename + ")");
      }
    finally
      {
	try
	  {
	    inStream.close();
	  }
	catch (IOException ex)
	  {
	  }
      }

    lockHash = new Hashtable(baseCount * 1.5);

  }

  public void dump(String filename) throws IOException
  {
    FileOutputStream outStream;
    DataOutputStream out;
    Enumeration basesEnum;
    short baseCount;
    DBDumpLock lock;

    /* -- */

    try
      {
	lock = new DBDumpLock(this);
	lock.establish();	// wait until we get our lock 

	outStream = new FileOutputStream(filename);
	out = new DataOutputStream(outStream);

	out.writeUTF("GStore");
	out.writeByte(major_version);
	out.writeByte(minor_version);

	baseCount = (short) objectBases.size();

	out.writeShort(baseCount);
	
	basesEnum = objectBases.elements();

	while (basesEnum.hasMoreElements())
	  {
	    ((DBObjectBase) basesEnum.nextElement()).emit(out);
	  } 
      }
    catch (IOException ex)
      {
	System.err.println("DBStore error dumping to " + filename);
	throw ex;
      }
    finally
      {
	lock.release();
	if (outStream != null)
	  {
	    outStream.close();
	  }
      }
  }

  public DBEditObject editDBObject(DBLock lock, DBEditSet editSet, Invid invid)
  {
    return editDBObject(lock, editSet, invid.getType(), invid.getNum());
  }

  public DBEditObject editDBObject(DBLock lock, DBEditSet editSet, short baseID, int objectID)
  {
    DBObject obj;

    /* -- */

    obj = viewDBObject(lock, baseID, objectID);

    return obj.createShadow(editSet);
  }

  public DBEditObject editDBObject(DBEditSet editSet, DBObject obj)
  {
    return obj.createShadow(editSet);
  }

  public DBObject viewDBObject(DBLock lock, Invid invid)
  {
    return viewDBObject(lock, invid.getType(), invid.getNum());
  }

  public DBObject viewDBObject(DBLock lock, short baseID, int objectID)
  {
    DBObjectBase base;
    DBObject     obj;
    Integer      baseKey;
    Integer      objKey;

    /* -- */

    baseKey = new Integer(baseID);
    objKey = new Integer(objectID);

    base = objectBases.get(baseKey);

    if (base == null)
      {
	return null;
      }

    // we don't check to see if the lock is a
    // DBReadLock.. viewDBOBject is acceptably atomic relative to any
    // possible write activity that might be occurring if we are
    // trying to read on a DBWriteLock, we won't worry about the
    // DBWriteLock possessor changing things on us.

    // we couldn't be so trusting on an enum establish request, though.

    if (!lock.isLocked(base))
      {
	throw new RuntimeException("viewDBObject: viewDBObject called without lock");
      }

    obj = base.objectHash.get(objKey);

    // do we want to do any kind of logging here?

    return obj;
  }
}


/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                          DBLock

DBLocks arbitrate access to a DBStore object.  Threads wishing to read from,
dump, or update the DBStore must be in possession of an established DBLock.

The general scheme is that any number of readers and/or dumpers can read
from the database simultaneously.  If a number of readers are processing when
a thread attempts to establish a write lock, those readers are allowed to
complete their reading, but no new read lock may be established until the
writer has a chance to get in and make its update.

If there are a number of writer locks queued up for update access to the
DBStore when a thread attempts to establish a dump lock, those writers are
allowed to complete their updates, but no new writer is queued until the
dump thread finishes dumping the database.

There is currently no support for handling timeouts, and locks can persist
indefinitely.

------------------------------------------------------------------------------*/

abstract class DBLock {

  // type parent

}


/*------------------------------------------------------------------------------
                                                                           class
                                                                     DBWriteLock

------------------------------------------------------------------------------*/

class DBWriteLock extends DBLock {

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
    done = false;

    if (lockManager.lockHash.containsKey(key))
      {
	throw new RuntimeException("Error: lock sought by owner of existing lockset.");
      }

    lockManager.lockHash.put(key, this);
    this.key = key;

    synchronized (lockManager)
      {
	// wait until there are no dumpers 

	do
	  {
	    okay = true;

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		if (base.dumperList.size() > 0)
		  {
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

	// add our selves to the ObjectBase write queues

	for (int i = 0; i < baseSet.size(); i++)
	  {
	    base = (DBObjectBase) baseSet.elementAt(i);
 	    base.writerList.addElement(this);
	  }

	// spinwait until we can get into all of the ObjectBases
	// note that since we added ourselves to the writer
	// queues, we know the dumpers are waiting until we
	// finish. 

	while (!done)
	  {
	    okay = true;
	    enum = lockManager.objectBases.elements();

	    for (int i = 0; okay && (i < baseSet.size()); i++)
	      {
		base = (DBObjectBase) baseSet.elementAt(i);
		if (base.writeInProgress || base.readerList.size() > 0)
		  {
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
      }

    locked = false;
    lockManager.lockHash.remove(key);
    key = null;

    lockManager.notifyAll();	// many readers may want in
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBReadLock

------------------------------------------------------------------------------*/

class DBReadLock extends DBLock {

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
      }

    locked = false;
    lockManager.lockHash.remove(key);
    key = null;

    lockManager.notify();
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBDumpLock

------------------------------------------------------------------------------*/

class DBDumpLock extends DBLock {

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
	    base.dumperList.addElement(this);
	    base.dumpInProgress = false;
	    base.currentLock = null;
	  }
      }

    locked = false;
    lockManager.lockHash.remove(key);
    key = null;

    lockManager.notify();
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
