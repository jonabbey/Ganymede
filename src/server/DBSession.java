 /*

   DBSession.java

   The GANYMEDE object storage system.

   Created: 26 August 1996
   Release: $Name:  $
   Version: $Revision: 1.65 $
   Last Mod Date: $Date: 1999/05/26 23:17:26 $
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

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBSession

------------------------------------------------------------------------------*/

/** 
 * <p>DBSession is the {@link arlut.csd.ganymede.DBStore DBStore} session
 * class.  All normal database interactions are performed through a
 * DBSession object.  The DBSession object provides a handle for
 * monitoring the operations on the database.. who holds what lock,
 * what actions are performed during a lock / transaction /
 * session, etc..</p>
 *
 * <p>Most particularly, DBSession handles transactions and namespace
 * logic for the Ganymede server, as well as providing the actual
 * check-out/create/ check-in methods that 
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} calls.
 * GanymedeSession tends to have the more high-level
 * application/permissions logic, while DBSession is more concerned
 * with internal database issues.  As well, GanymedeSession is
 * designed to be directly accessed and manipulated by the client,
 * while DBSession is accessed only by (presumably trusted)
 * server-side code.</p>
 *
 * @version $Revision: 1.65 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

final public class DBSession {

  static boolean debug = false;

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  GanymedeSession GSession;
  DBStore store;

  Vector lockVect = new Vector();

  DBEditSet editSet;
  String lastError;
  String id = null;
  Object key;

  /* -- */

  /**   
   * <p>Constructor for DBSession.</p>
   * 
   * <p>The key passed to the DBSession constructor is intended to be used
   * to allow code to save an identifier in the DBSession.. this might be
   * a thread object or a higher level session object or whatever.  Eventually
   * I expect I'll replace this generic key with some sort of reporting 
   * Interface object.</p>
   *
   * <p>This constructor is intended to be called by the DBStore login() method.</p>
   *
   * @param store The DBStore database this session belongs to.
   * @param GSession The Ganymede session associated with this DBSession
   * @param key An identifying key with meaning to whatever code is using arlut.csd.ganymede
   *
   */
   
  DBSession(DBStore store, GanymedeSession GSession, Object key)
  {
    this.store = store;
    this.key = key;
    this.GSession = GSession;

    editSet = null;
    lastError = null;
  }

  /**
   *
   * Close out this DBSession, aborting any open transaction, and releasing
   * held read locks.
   *
   */

  public synchronized void logout()
  {
    releaseAllLocks();

    if (editSet != null)
      {
	abortTransaction();
      }

    this.store = null;
  }

  /**
   *
   * This method is provided so that custom DBEditObject subclasses
   * can get access to methods on our DBStore.
   *
   */

  public DBStore getStore()
  {
    return store;
  }


  /**
   * Create a new object in the database.<br><br>
   *
   * This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.<br><br>
   *
   * The created object will be given an object id.
   * The DBEditObject can be queried to determine its
   * invid.
   *
   * This method will return null if the object could
   * not be constructed and initialized for some reason.
   *
   * @param object_type Type of the object to be created
   * @param chosenSlot Invid to create the new object with.
   * normally only used in internal Ganymede code in conjunction with
   * the addition of new kinds of built-in objects during development
   *
   * @see arlut.csd.ganymede.DBStore
   *
   */

  public synchronized DBEditObject createDBObject(short object_type, Invid chosenSlot, Vector owners)
  {
    DBObjectBase base;
    DBEditObject e_object;
    DateDBField df;
    StringDBField sf;
    Date modDate = new Date();
    String result;
    Invid tmpInvid;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("createDBObject called outside of a transaction");
      }

    base = (DBObjectBase) store.objectBases.get(new Short(object_type));

    e_object = base.createNewObject(editSet, chosenSlot);

    if (e_object == null)
      {
	return null;
      }

    // set ownership for this new object if it is not an embedded object

    if (!base.isEmbedded() && (owners != null))
      {
	InvidDBField inf = (InvidDBField) e_object.getField(SchemaConstants.OwnerListField);
	
	for (int i = 0; i < owners.size(); i++)
	  {
	    tmpInvid = (Invid) owners.elementAt(i);

	    if (tmpInvid.getType() != SchemaConstants.OwnerBase)
	      {
		throw new RuntimeException("bad ownership invid");
	      }

	    // we don't want to explicitly record supergash ownership
	    
	    if (tmpInvid.getNum() == SchemaConstants.OwnerSupergash)
	      {
		continue;
	      }

	    inf.addElementLocal(owners.elementAt(i));
	  }
      }

    // add this object to the transaction

    String ckp_label = "create" + e_object.toString();

    // we're only going to do the checkpoint here if
    // oversight is enabled.. otherwise it would be far, far
    // too expensive a burden during bulk loading.

    if (GSession.enableOversight)
      {
	checkpoint(ckp_label);
      }

    // register the object as created	

    editSet.addObject(e_object);

    // set any inital fields

    if (!e_object.initializeNewObject())
      {
	if (GSession.enableOversight)
	  {
	    rollback(ckp_label);
	  }

	return null;
      }

    if (GSession.enableOversight)
      {
	popCheckpoint(ckp_label);
      }

    // update admin consoles

    // update the session's checkout count first, then
    // update the database's overall checkout, which
    // will trigger a console update

    GSession.checkOut();

    store.checkOut();

    // set the following false to true to view the initial state of the object
    
    if (false)
      {
	try
	  {
	    Ganymede.debug("Created new object : " + e_object.getLabel() + ", invid = " + e_object.getInvid());
	    db_field[] fields = e_object.listFields(false);
	    
	    for (int i = 0; i < fields.length; i++)
	      {
		Ganymede.debug("field: " + i + " is " + fields[i].getID() + ":" + fields[i].getName());
	      }
	  }
	catch (java.rmi.RemoteException ex)
	  {
	    Ganymede.debug("Whoah!" + ex);
	  }
      }

    // finish initialization of the object

    if (!base.isEmbedded())
      {
	// set creator info to something non-null

	df = (DateDBField) e_object.getField(SchemaConstants.CreationDateField);
	df.setValueLocal(modDate);

	sf = (StringDBField) e_object.getField(SchemaConstants.CreatorField);

	result = getID();

	if (editSet.description != null)
	  {
	    result += ": " + editSet.description;
	  }

	sf.setValueLocal(result);

	// set modifier info to something non-null

	df = (DateDBField) e_object.getField(SchemaConstants.ModificationDateField);
	df.setValueLocal(modDate);

	sf = (StringDBField) e_object.getField(SchemaConstants.ModifierField);

	result = getID();

	if (editSet.description != null)
	  {
	    result += ": " + editSet.description;
	  }

	sf.setValueLocal(result);
      }

    return e_object;
  }

  /**
   * Create a new object in the database.<br><br>
   *
   * This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.<br><br>
   *
   * The created object will be given an object id.
   * The DBEditObject can be queried to determine its
   * invid.
   *
   * This method will return null if the object could
   * not be constructed and initialized for some reason.
   *
   * @param object_type Type of the object to be created
   *
   * @see arlut.csd.ganymede.DBStore
   *
   */

  public DBEditObject createDBObject(short object_type, Vector owners)
  {
    return createDBObject(object_type, null, owners);
  }

  /**
   *
   * Pull an object out of the database for editing.<br><br>
   *
   * This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.<br><br>
   *
   * The session has to have a transaction opened before it can pull
   * an object out for editing.
   *
   * @param invid The invariant id of the object to be modified.
   *
   * @see arlut.csd.ganymede.DBObjectBase 
   */

  public DBEditObject editDBObject(Invid invid)
  {
    return editDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Pull an object out of the database for editing.<br><br>
   *
   * This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.<br><br>
   *
   * The session has to have a transaction opened before it can pull a
   * new object out for editing.  If the object specified by <baseID,
   * objectID> is part of the current transaction, the transactional
   * copy will be returned, and no readLock is strictly necessary in
   * that case.
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be edited.
   *
   * @param objectID The int id number of the object to be edited within the specified
   *                 object base.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   * 
   */

  public synchronized DBEditObject editDBObject(short baseID, int objectID)
  {
    DBObject obj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("editDBObject called outside of a transaction");
      }

    obj = viewDBObject(baseID, objectID);

    if (obj == null)
      {
	System.err.println("*** couldn't find object, base = " + baseID + ", obj = " + objectID);
	return null;
      }

    if (obj instanceof DBEditObject)
      {
	// we already have a copy checked out.. go ahead and
	// return a reference to our copy

	return (DBEditObject) obj;
      }
    else
      {
	DBEditObject eObj = obj.createShadow(editSet); // *sync* DBObject

	if (eObj == null)
	  {
	    setLastError("Couldn't edit " + obj.getLabel() + 
			 ", someone else is working with the object.");
	  }

	return eObj;
      }
  }

  /**
   *
   * Get a reference to a read-only copy of an object in the DBStore.<br><br>
   *
   * If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.
   *
   * Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use GanymedeSession.view_db_object to
   * get the object.
   *
   * @param invid The invariant id of the object to be viewed.
   *
   * @see arlut.csd.ganymede.DBObjectBase
   *
   */

  public DBObject viewDBObject(Invid invid)
  {
    return viewDBObject(invid.getType(), invid.getNum());
  }

  /**
   *
   * Get a reference to a read-only copy of an object in the DBStore.<br><br>
   *
   * If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.
   *
   * Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use GanymedeSession.view_db_object to
   * get the object.
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be viewed.
   *
   * @param objectID The int id number of the object to be viewed within the specified
   *                 object base.
   *
   *
   * @see arlut.csd.ganymede.DBObjectBase
   *
   */

  public DBObject viewDBObject(short baseID, int objectID)
  {
    DBObjectBase base;
    DBObject     obj = null;
    Short      baseKey;
    Integer      objKey;

    /* -- */
    
    if (isTransactionOpen())
      {
	try
	  {
	    obj = editSet.findObject(new Invid(baseID, objectID)); // *sync* DBEditSet
	  }
	catch (NullPointerException ex)
	  {
	    // maybe the transaction got closed?  We're not synchronized here, after all
	  }

	if (obj != null)
	  {
	    return obj;
	  }
      }

    baseKey = new Short(baseID);

    base = Ganymede.db.getObjectBase(baseKey); // store may not be set at this point if this object is not checked out

    if (base == null)
      {
	return null;
      }

    // depend on the objectTable's thread synchronization here

    obj = base.objectTable.get(objectID);

    // do we want to do any kind of logging here?

    return obj;
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction.  This method will mark an object for deletion.  When
   * the transaction is committed, the object is removed from the
   * database.  If the transaction is aborted, the object remains in
   * the database unchanged.
   *
   * @param invid Invid of the object to be deleted
   * 
   */

  public ReturnVal deleteDBObject(Invid invid)
  {
    return deleteDBObject(invid.getType(), invid.getNum()); // *sync*
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction.  This method will check an object out of the DBStore
   * and add it to the editset's deletion list.  When the transaction
   * is committed, the object has its remove() method called to do
   * cleanup, and the editSet nulls the object's slot in the DBStore.
   * If the transaction is aborted, the object remains in the database
   * unchanged.
   *
   * @param baseID id of the object base containing the object to be deleted
   * @param objectID id of the object to be deleted
   *  
   */

  public ReturnVal deleteDBObject(short baseID, int objectID)
  {
    DBObject obj;
    DBEditObject eObj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("deleteDBObject called outside of a transaction");
      }

    obj = viewDBObject(baseID, objectID);

    if (obj instanceof DBEditObject)
      {
	eObj = (DBEditObject) obj;
      }
    else
      {
	eObj = obj.createShadow(editSet);
      }

    if (eObj == null)
      {
	return Ganymede.createErrorDialog("Can't delete " + obj.getLabel(),
					  "Couldn't delete " + obj.getLabel() + 
					  ", someone else is working with the object.");
      }

    return deleteDBObject(eObj);
  }

  /**
   *
   * Remove an object from the database<br><br>
   *
   * This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the
   * only way to obtain a DBEditObject), no other locking is
   * required. This method will take an object out of the DBStore, do
   * whatever immediate removal logic is required, and mark it as
   * deleted in the transaction.  When the transaction is committed,
   * the object will be expunged from the database.
   *
   * Note that this method does not check to see whether permission
   * has been obtained to delete the object.. that's done in
   * GanymedeSession.delete_db_object().
   *
   * @param eObj An object checked out in the current transaction to be deleted
   *   
   */

  public synchronized ReturnVal deleteDBObject(DBEditObject eObj)
  {
    ReturnVal retVal, retVal2;
    String key;

    /* -- */

    key = "del" + eObj.getLabel();

    switch (eObj.getStatus())
      {
      case DBEditObject.CREATING:
	checkpoint(key);
	eObj.setStatus(DBEditObject.DROPPING);
	break;

      case DBEditObject.EDITING:
	checkpoint(key);
	eObj.setStatus(DBEditObject.DELETING);
	break;

      case DBEditObject.DELETING:
      case DBEditObject.DROPPING:

	// already to be deleted

	return null;
      }

    retVal = eObj.remove();

    // the remove logic can entirely bypass our normal finalize logic

    if (retVal != null && !retVal.didSucceed())
      {
	if (retVal.getCallback() == null)
	  {
	    // oops, irredeemable failure.  rollback.

	    rollback(key);
	    return retVal;
	  }
	else
	  {
	    // the remove() logic is presenting a wizard
	    // to the user.. turn the client over to
	    // the wizard

	    return retVal;
	  }
      }
    else
      {
	// ok, go ahead and finalize

	retVal2 = eObj.finalizeRemove(true);

	if (retVal2 != null && !retVal2.didSucceed())
	  {
	    // oops, irredeemable failure.  rollback.
		
	    rollback(key);
	  }

	return retVal2;
      }
  }

  /**
   * <p>Inactivate an object in the database</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the only
   * way to obtain a DBEditObject), no other locking is required. This method
   * will take an object out of the DBStore and proceed to do whatever is
   * necessary to cause that object to be 'inactivated'.</p>
   *
   * <p>Note that this method does not check to see whether permission
   * has been obtained to inactivate the object.. that's done in
   * {@link arlut.csd.ganymede.GanymedeSession#inactivate_db_object(arlut.csd.ganymede.Invid) 
   * GanymedeSession.inactivate_db_object()}.</p>
   *
   * @param eObj An object checked out in the current transaction to be inactivated
   */

  public synchronized ReturnVal inactivateDBObject(DBEditObject eObj)
  {
    ReturnVal retVal;
    String key;

    /* -- */

    key = "inactivate" + eObj.getLabel();

    switch (eObj.getStatus())
      {
      case DBEditObject.EDITING:
      case DBEditObject.CREATING:
	break;

      default:
	return Ganymede.createErrorDialog("Server: Error in DBSession.inactivateDBObject()",
					  "Error.. can't inactivate an object that has " +
					  "already been inactivated or deleted");
      }

    checkpoint(key);

    if (debug)
      {
	System.err.println("DBSession.inactivateDBObject(): Calling eObj.inactivate()");
      }

    retVal = eObj.inactivate();

    if (debug)
      {
	System.err.println("DBSession.inactivateDBObject(): Got back from eObj.inactivate()");
      }

    if (retVal != null && !retVal.didSucceed())
      {
	if (retVal.getCallback() == null)
	  {
	    // oops, irredeemable failure.  rollback.

	    System.err.println("DBSession.inactivateDBObject(): object refused inactivation, rolling back");

	    eObj.finalizeInactivate(false);
	  }

	// otherwise, we've got a wizard that the client will deal with.
      }
    else
      {
	// immediate success!

	eObj.finalizeInactivate(true);
      }

    return retVal;
  }

  /**
   * <p>Reactivates an object in the database.</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the only
   * way to obtain a {@link arlut.csd.ganymede.DBEditObject DBEditObject}),
   * no other locking is required. This method
   * will take an object out of the {@link arlut.csd.ganymede.DBStore DBStore}
   * and proceed to do whatever is
   * necessary to cause that object to be 'inactivated'.</p>
   *
   * <p>Note that this method does not specifically check to see whether permission
   * has been obtained to reactivate the object.. that's done in
   * {@link arlut.csd.GanymedeSession#reactivate_db_object(arlut.csd.ganymede.Invid) 
   * GanymedeSession.reactivate_db_object()}.</p>
   *
   * @param eObj An object checked out in the current transaction to be reactivated
   */

  public synchronized ReturnVal reactivateDBObject(DBEditObject eObj)
  {
    ReturnVal retVal;
    String key;

    /* -- */

    key = "reactivate" + eObj.getLabel();

    switch (eObj.getStatus())
      {
      case DBEditObject.DELETING:
      case DBEditObject.DROPPING:
	return Ganymede.createErrorDialog("Server: Error in DBSession.reactivateDBObject()",
					  "Error.. can't reactivate an object that is being deleted\n" +
					  "If you need to undo an object deletion, cancel your transaction.");
      }

    if (!eObj.isInactivated())
      {
	return Ganymede.createErrorDialog("Server: Error in DBSession.reactivateDBObject()",
					  "Error.. can't reactivate an object that is not inactive.");
      }

    checkpoint(key);

    System.err.println("DBSession.reactivateDBObject(): Calling eObj.reactivate()");

    retVal = eObj.reactivate();

    System.err.println("DBSession.reactivateDBObject(): Got back from eObj.reactivate()");

    if (retVal != null && !retVal.didSucceed())
      {
	if (retVal.getCallback() == null)
	  {
	    // oops, irredeemable failure.  rollback.

	    System.err.println("DBSession.reactivateDBObject(): object refused reactivation, rolling back");

	    rollback(key);
	  }
      }
    else
      {
	// immediate success!

	eObj.finalizeReactivate(true);
	popCheckpoint(key);
      }

    return retVal;
  }

  /**
   * <p>Convenience pass-through method</p>
   * 
   * @see arlut.csd.ganymede.DBEditSet#checkpoint(java.lang.String)
   */

  public final void checkpoint(String name)
  {
    if (editSet != null)
      {
	editSet.checkpoint(name);
      }
  }

  /**
   * <p>Convenience pass-through method</p>
   * 
   * @see arlut.csd.ganymede.DBEditSet#popCheckpoint(java.lang.String)
   */

  public final boolean popCheckpoint(String name)
  {
    DBCheckPoint point = null;

    /* -- */

    if (editSet != null)
      {
	point = editSet.popCheckpoint(name);
      }

    return (point != null);
  }

  /**
   * <p>Convenience pass-through method</p>
   * 
   * @see arlut.csd.ganymede.DBEditSet#rollback(java.lang.String)
   */

  public final boolean rollback(String name)
  {
    if (editSet != null)
      {
	return editSet.rollback(name);
      }

    return false;
  }

  /**
   * <p>Returns true if the session's lock is currently locked, false
   * otherwise.</p>
   */

  public boolean isLocked(DBLock lockParam)
  {
    if (lockParam == null)
      {
	throw new IllegalArgumentException("bad param to isLocked()");
      }

    if (!lockVect.contains(lockParam))
      {
	return false;
      }
    else
      {
	return (lockParam.isLocked());
      }
  }

  /**
   * <p>Establishes a read lock for the DBObjectBase's in bases.</p>
   *
   * <p>The thread calling this method will block until the read lock 
   * can be established.  If any of the {@link arlut.csd.ganymede.DBObjectBase DBObjectBases}
   * in the bases vector have transactions
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.</p>
   *
   * <p>All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.</p>
   */

  public synchronized DBReadLock openReadLock(Vector bases) throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    lock = new DBReadLock(store, bases);

    lockVect.addElement(lock);

    lock.establish(this);

    return lock;
  }

  /**
   *
   * openReadLock establishes a read lock for the entire DBStore.<br><br>
   *
   * The thread calling this method will block until the read lock 
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.<br><br>
   *
   * All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.
   */

  public synchronized DBReadLock openReadLock() throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    lock = new DBReadLock(store);
    lockVect.addElement(lock);

    lock.establish(this);

    return lock;
  }

  /**
   *
   * This method establishes a dump lock on all object bases in this Ganymede
   * server.
   *
   */

  public synchronized DBDumpLock openDumpLock() throws InterruptedException
  {
    DBDumpLock lock;

    /* -- */

    lock = new DBDumpLock(store);
    lockVect.addElement(lock);

    lock.establish(this);

    return lock;
  }

  /**
   *
   * releaseLock releases a particular lock held by this session.<br><br>
   *
   * This method must be synchronized.
   *
   */

  public synchronized void releaseLock(DBLock lock)
  {
    lock.release();		// *sync* DBStore
    lockVect.removeElement(lock);
    notifyAll();
  }

  /**
   *
   * releaseAllLocks() releases all locks held by this
   * session.<br><br>
   *
   * This method is *not* synchronized.  This method must
   * only be called by code synchronized on this DBSession
   * instance.
   *
   */

  public void releaseAllLocks()
  {
    DBLock lock;
    Enumeration enum = lockVect.elements();

    /* -- */
    
    while (enum.hasMoreElements())
      {
	lock = (DBLock) enum.nextElement();
	lock.abort();
      }

    lockVect.removeAllElements();

    notifyAll();
  }

  /**
   * openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject and 
   * createDBObject to obtain DBEditObjects.  Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction is called.
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @see arlut.csd.ganymede.DBEditObject
   *
   */ 

  public synchronized void openTransaction(String describe)
  {
    if (editSet != null)
      {
	throw new IllegalArgumentException("transaction already open.");
      }

    editSet = new DBEditSet(store, this, describe);
  }

  /**
   * commitTransaction causes any changes made during the context of
   * a current transaction to be performed.  Appropriate portions of the
   * database are locked, changes are made to the in-memory image of
   * the database, and a description of the changes is placed in the
   * DBStore journal file.  Event logging / mail notification may
   * take place.<br><br>
   *
   * The session must not hold any locks when commitTransaction is
   * called.  The symmetrical invid references between related objects
   * and the atomic namespace management code should guarantee that no
   * incompatible change is made with respect to any checked out objects
   * while the Bases are unlocked.
   *
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.DBEditObject
   *
   */

  public synchronized ReturnVal commitTransaction()
  {
    ReturnVal retVal = null;
    boolean result;

    /* -- */

    // we need to release our readlock, if we have one,
    // so that the commit can establish a writelock..

    // should i make it so that a writelock can be established
    // if the possessor of a readlock doesn't give it up? 

    if (debug)
      {
	System.err.println(key + ": entering commitTransaction");
      }

    if (editSet == null)
      {
	throw new RuntimeException(key + ": commitTransaction called outside of a transaction");
      }

    releaseAllLocks();

    while (lockVect.size() != 0)
      {
	Ganymede.debug("DBSession: commitTransaction waiting for read/dump locks to be released");

	try
	  {
	    wait();
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("DBSession: commitTransaction got an interrupted exception " + 
			   "waiting for read locks to be released." + ex);
	  }

	//	throw new IllegalArgumentException(key + ": commitTransaction(): holding a lock");
      }

    retVal = editSet.commit();

    if (retVal == null || retVal.didSucceed())
      {
	Ganymede.debug(key + ": commitTransaction(): editset committed");
	editSet = null;
      }
    else
      {
	// The DBEditSet.commit() method will set retVal.doNormalProcessing true
	// if the problem that prevented commit was transient.. i.e., missing
	// fields, lock not available, etc.

	// If we had an IO error or some unexpected exception or the
	// like, doNormalProcessing will be false, and the transaction
	// will have been wiped out by the commit logic.  In this case,
	// there's nothing that can be done, the transaction is dead
	// and gone.

	if (!retVal.doNormalProcessing)
	  {
	    editSet = null;
	  }
      }

    return retVal;		// later on we'll figure out how to do this right
  }

  /**
   * abortTransaction causes all DBEditObjects that were pulled during
   * the course of the session's transaction to be released without affecting
   * the state of the database.  Any changes made to DBObjects pulled for editing
   * by this session during this transaction are abandoned.  Any objects created
   * or destroyed by this session during this transaction are abandoned / unaffected
   * by the actions during the transaction.<br><br>
   *
   * Calling abortTransaction has no affect on any locks held by this session, but
   * generally no locks should be held here.
   *
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.DBEditObject
   */

  public synchronized ReturnVal abortTransaction()
  {
    if (editSet == null)
      {
	throw new RuntimeException("abortTransaction called outside of a transaction");
      }

    if (editSet.wLock != null)
      {
	if (editSet.wLock.inEstablish)
	  {
	    try
	      {
		editSet.wLock.abort();
	      }
	    catch (NullPointerException ex)
	      {
	      }
	  }
	else
	  {
	    Ganymede.debug("abortTransaction() for " + key + ", can't safely dump writeLock.. can't kill it off");

	    return Ganymede.createErrorDialog("Server: Error in DBSession.abortTransaction()",
					      "Error.. transaction could not abort: can't safely dump writeLock");
	  }
      }

    releaseAllLocks();

    while (lockVect.size() != 0)
      {
	Ganymede.debug("DBSession: abortTransaction waiting for read/dump locks to be released");

	try
	  {
	    wait(500);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("DBSession: abortTransaction got an interrupted exception " +
			   "waiting for read locks to be released." + ex);
	  }
      }

    editSet.release();
    editSet = null;

    return null;
  }

  /**
   *
   * Returns true if this session has an transaction open
   *
   */

  public boolean isTransactionOpen()
  {
    return (editSet != null);
  }

  /**
   *
   * internal method for setting error messages resulting from session activities.
   *
   * this method may eventually be hooked up to a more general logging
   * mechanism.
   *
   */

  public void setLastError(String error)
  {
    this.lastError = error;

    if (debug)
      {
	Ganymede.debug(key + ": DBSession.setLastError(): " + error);
      }
  }

  public Object getKey()
  {
    return key;
  }

  /**
   *
   * This method is responsible for providing an identifier string
   * for the user who this session belongs to, and is used for
   * logging and what-not.
   *
   */

  public String getID()
  {
    String result = "";
    DBObject obj;

    /* -- */

    if (id != null)
      {
	return id;
      }

    obj = GSession.getUser();

    if (obj != null)
      {
	result = obj.getLabel();
      }

    obj = GSession.getPersona();

    if (obj != null)
      {
	result += ":" + obj.getLabel();
      }

    return result;
  }

  /**
   *
   * This method returns a handle to the Ganymede Session
   * that owns this DBSession.
   *
   */

  public GanymedeSession getGSession()
  {
    return GSession;
  }

  /**
   *
   * This method returns a handle to the objectHook for
   * a particular Invid.
   *
   */

  public DBEditObject getObjectHook(Invid invid)
  {
    DBObjectBase base;

    base = Ganymede.db.getObjectBase(invid.getType());
    return base.objectHook;
  }
  
}
