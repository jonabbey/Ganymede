/*

   DBSession.java

   The GANYMEDE object storage system.

   Created: 26 August 1996
   Release: $Name:  $
   Version: $Revision: 1.105 $
   Last Mod Date: $Date: 2002/03/13 05:29:50 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
 * <p>DBSession is the Ganymede server's
 * {@link arlut.csd.ganymede.DBStore DBStore}-level session class.  Each
 * client or server process that interacts with the Ganymede database
 * must eventually do so through a DBSession object.  Clients and
 * server processes generally interact directly with a
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}, by way of
 * the {@link arlut.csd.ganymede.Session Session} interface on the
 * part of the client.  The GanymedeSession talks to the DBSession class
 * to actually interact with the database.</P>
 *
 * <p>Most particularly, DBSession handles transactions and namespace
 * logic for the Ganymede server, as well as providing the actual
 * check-out/create/ check-in methods that GanymedeSession calls. 
 * GanymedeSession tends to have the more high-level
 * application/permissions logic, while DBSession is more concerned
 * with internal database issues.  As well, GanymedeSession is
 * designed to be directly accessed and manipulated by the client,
 * while DBSession is accessed only by (presumably trusted)
 * server-side code, that needs to bypass the security logic in
 * GanymedeSession.</p>
 *
 * <P>The DBSession contains code and logic to actually manipulate the
 * Ganymede database (the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase},
 * {@link arlut.csd.ganymede.DBObject DBObject}, and
 * {@link arlut.csd.ganymede.DBEditObject DBEditObject} objects held
 * in the DBStore).  The DBSession class connects to the extensive 
 * transaction logic implemented in the {@link arlut.csd.ganymede.DBEditSet DBEditSet}
 * class, as well as the database locking handled by the
 * {@link arlut.csd.ganymede.DBLock DBLock} class.</P>
 * 
 * @version $Revision: 1.105 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

final public class DBSession {

  static boolean debug = false;

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  /**
   * <P>User-level session reference.  As mentioned above, 
   * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} has the
   * user-level permissions handling, while DBSession has the database
   * handling.</P>
   */

  GanymedeSession GSession;

  /**
   * <P>Root object of the Ganymede database system.</P>
   */

  DBStore store;

  /**
   * <P>Vector of {@link arlut.csd.ganymede.DBLock DBLock} objects held
   * by this session.  This needs to be a Vector because a DBSession
   * can legitimately hold multiple {@link arlut.csd.ganymede.DBReadLock DBReadLock}
   * objects at a time.  Only one writer lock is allowed at a time, however,
   * to avoid deadlock.</P>
   *
   * <P>The DBStore's {@link arlut.csd.ganymede.DBLockSync
   * DBLockSync} object is used, in conjunction with the {@link
   * arlut.csd.ganymede.DBLock#establish(java.lang.Object)
   * establish()} methods in the DBReadLock, {@link
   * arlut.csd.ganymede.DBWriteLock DBWriteLock}, and {@link
   * arlut.csd.ganymede.DBDumpLock DBDumpLock} classes.</P> 
   */

  Vector lockVect = new Vector();

  /**
   * <P>Transaction handle for this session.</P>
   */

  DBEditSet editSet;

  /** 
   * <P>Deprecated error reporting string.. like errno in the C
   * standard library, this sort of interface isn't thread-friendly,
   * so we've largely moved away from using this String to the
   * per-method {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * result object.</P>
   */

  String lastError;

  /**
   * <P>Optional string identifying this session in logging, etc.</P>
   *
   * <P>Used by {@link arlut.csd.ganymede.DBSession#getID() getID()}.
   */

  String id = null;

  /**
   * <P>Identifying key used in the lock system to identify owner of
   * locks.</P>
   */

  Object key;

  /**
   * <p>This flag will be set to true if this session is busy
   * trying to commit or abandon a transaction, and no new objects
   * should be editable by this transaction.</p>
   */

  private boolean terminating = false;


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

    store.login(this);

    editSet = null;
    lastError = null;
  }

  /**
   * <P>Close out this DBSession, aborting any open transaction, and releasing
   * any held read/write/dump locks.</P>
   */

  public synchronized void logout()
  {
    releaseAllLocks();

    if (editSet != null)
      {
	abortTransaction();
      }

    store.logout(this);

    // help GC

    store = null;
    GSession = null;
    lockVect = null;
    key = null;
  }

  /**
   * <P>This method is provided so that custom
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject} subclasses
   * can get access to methods on our DBStore.</P>
   */

  public DBStore getStore()
  {
    return store;
  }

  /**
   * <P>Create a new object in the database.</P>
   *
   * <P>This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.</P>
   *
   * <P>The created object will be given an object id.
   * The {@link arlut.csd.ganymede.DBEditObject DBEditObject} can 
   * be queried to determine its invid.</P>
   *
   * <P>The created DBEditObject will have its fields initialized
   * by the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * {@link arlut.csd.ganymede.DBObjectBase#objectHook objectHook}
   * custom DBEditObject's
   * {@link arlut.csd.ganymede.DBEditObject#initializeNewObject() initializeNewObject()}
   * method.</P>
   *
   * <P>This method will return null if the object could
   * not be constructed and initialized for some reason.</P>
   *
   * @param object_type Type of the object to be created
   * @param chosenSlot Invid to create the new object with.
   * normally only used in internal Ganymede code in conjunction with
   * the addition of new kinds of built-in objects during development
   * @param owners Vector of invids for owner group objects to make initial
   * owners for the newly created object
   *
   * @see arlut.csd.ganymede.DBStore
   */

  public synchronized ReturnVal createDBObject(short object_type, Invid chosenSlot, Vector owners)
  {
    DBObjectBase base;
    DBEditObject e_object;
    ReturnVal retVal = null;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("createDBObject called outside of a transaction");
      }

    if (isTerminating())
      {
	throw new RuntimeException("createDBObject called during transaction commit/abort");
      }

    base = store.getObjectBase(object_type);

    // we create the object.. this just gets the DBEditObject
    // created.. all of its fields will be created, but it won't be
    // linked into the database or editset or anything else yet.

    e_object = base.createNewObject(editSet, chosenSlot);

    if (e_object == null)
      {
	// failure?  Report it, but we don't have to do any clean up at this
	// point.

	return Ganymede.createErrorDialog("Object Creation Failure",
					  "Couldn't create the new object in the database.");
      }

    // Checkpoint the transaction at this point so that we can
    // recover if we can't get the object into the owner groups
    // it needs to go into

    String ckp_label = "createX" + base.getName();

    checkpoint(ckp_label);
    boolean checkpointed = true;
    
    try
      {
	// set ownership for this new object if it is not an embedded object

	if (!base.isEmbedded() && (owners != null))
	  {
	    InvidDBField inf = (InvidDBField) e_object.getField(SchemaConstants.OwnerListField);
	    Invid tmpInvid;

	    /* -- */
	
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

		retVal = inf.addElementLocal(owners.elementAt(i));

		if (retVal != null && !retVal.didSucceed())
		  {
		    try
		      {
			DBObject owner = viewDBObject((Invid) owners.elementAt(i));
			String name = owner.getLabel();

			String checkedOutBy = owner.shadowObject.editset.description;

			retVal.getDialog().appendText("\nOwner group " + name + 
						      " is currently checked out by:\n" + checkedOutBy);
		      }
		    catch (NullPointerException ex)
		      {
		      }

		    return retVal;
		  }
	      }
	  }

	// register the object as created

	// this can fail if the e_object comes to us already pointing
	// to an object that is being deleted by another transaction
	// by way of an asymmetric InvidDBField.  This should never
	// happen, as it would require a custom object's constructor
	// to have set an InvidDBField value instead of putting that
	// logic in its initializeNewObject() method, but we should
	// check just in case.

	if (!editSet.addObject(e_object))
	  {
	    return Ganymede.createErrorDialog("Object creation failure",
					      "Couldn't create the object, because it came pre-linked " +
					      "to a deleted object.\nDon't worry, this wasn't your fault.\n" +
					      "Talk to whoever customized Ganymede for you, or try again later.");
	  }

	// update admin consoles
	//
	// Now that we've added our new object to our transaction, we need
	// to update objects checked-out counts.  After this point, doing a
	// rollback will cause the session and server check-out counts to
	// be decremented for our new object, and we have to increment it
	// before that happens.
	//
	// we need to do the session's checkout count first, then
	// update the database's overall checkout, which
	// will trigger a console update
	
	GSession.checkOut();
	
	store.checkOut();

	if (!base.isEmbedded())
	  {
	    // do any work that the custom code for this object wants
	    // to have done

	    // note that we're not doing this for embedded objects,
	    // because we want to defer the initializeNewObject() call
	    // until the embedded object has been linked to its
	    // parent, which is done by
	    // InvidDBField.createNewEmbedded().

	    retVal = e_object.initializeNewObject();

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	// okay, we're good, and we won't need to revert to the checkpoint.  
	// Clear out the checkpoint and continue

	popCheckpoint(ckp_label);
	checkpointed = false;
      }
    finally
      {
	// just in case we had an exception thrown.. all standard
	// returns from the above try clause should have taken care of
	// the checkpoint
  
	if (checkpointed)
	  {
	    rollback(ckp_label);
	  }
      }

    // set the following false to true to view the initial state of the object
    
    if (false)
      {
	try
	  {
	    Ganymede.debug("Created new object : " + e_object.getLabel() + ", invid = " + e_object.getInvid());
	    db_field[] fields = e_object.listFields();
	    
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

    // finish initialization of the object.. none of this should fail
    // since we are just setting text and date fields
    
    if (!base.isEmbedded())
      {
	DateDBField df;
	StringDBField sf;
	Date modDate = new Date();
	String result;

	/* -- */

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

    retVal = new ReturnVal(true);
    retVal.setObject(e_object);
    retVal.setInvid(e_object.getInvid());
    
    return retVal;
  }

  /**
   * <P>Create a new object in the database.</P>
   *
   * <P>This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction 
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.</P>
   *
   * <P>The created object will be given an object id.
   * The {@link arlut.csd.ganymede.DBEditObject DBEditObject}
   * can be queried to determine its invid.</P>
   *
   * <P>The created DBEditObject will have its fields initialized
   * by the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * {@link arlut.csd.ganymede.DBObjectBase#objectHook objectHook}
   * custom DBEditObject's
   * {@link arlut.csd.ganymede.DBEditObject#initializeNewObject() initializeNewObject()}
   * method.</P>
   *
   * <P>This method returns a ReturnVal object to convey the
   * result of the creation.  Call the 
   * {@link arlut.csd.ganymede.ReturnVal#getObject() getObject()} method on
   * the returned ReturnVal in order to get the created DBEditObject.  Note
   * that the ReturnVal.getObject() method is intended to support passing
   * a remote db_object reference to the client, so on the server, it is
   * necessary to cast the db_object reference to a DBEditObject reference
   * for use on the server.</P>
   *
   * @param object_type Type of the object to be created
   *
   * @see arlut.csd.ganymede.DBStore
   */

  public ReturnVal createDBObject(short object_type, Vector owners)
  {
    return createDBObject(object_type, null, owners);
  }

  /**
   * <P>Pull an object out of the database for editing.</P>
   *
   * <P>This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.</P>
   *
   * <P>The session has to have a transaction opened before it can pull
   * an object out for editing.</P>
   *
   * @param invid The invariant id of the object to be modified.
   *
   * @return null if the object could not be found for editing
   *
   * @see arlut.csd.ganymede.DBObjectBase 
   */

  public DBEditObject editDBObject(Invid invid)
  {
    return editDBObject(invid.getType(), invid.getNum());
  }

  /**
   * <P>Pull an object out of the database for editing.</P>
   *
   * <P>This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.</P>
   *
   * <P>The session has to have a transaction opened before it can pull a
   * new object out for editing.  If the object specified by &lt;baseID,
   * objectID&gt; is part of the current transaction, the transactional
   * copy will be returned, and no readLock is strictly necessary in
   * that case.</P>
   *
   * <P>This method doesn't do permission checking.. that is performed at the
   * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} level.</P>
   *
   * @param baseID The short id number of the
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} containing the object to
   * be edited.
   *
   * @param objectID The int id number of the object to be edited within the specified
   *                 object base.
   *
   * @return null if the object could not be found for editing
   */

  public synchronized DBEditObject editDBObject(short baseID, int objectID)
  {
    DBObject obj;

    /* -- */

    if (editSet == null)
      {
	throw new RuntimeException("editDBObject called outside of a transaction");
      }

    if (isTerminating())
      {
	throw new RuntimeException("editDBObject called during transaction commit/abort");
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
	// the createShadow call will update the check-out counts

	DBEditObject eObj = obj.createShadow(editSet); // *sync* DBObject

	return eObj; // if null, GanymedeSession.edit_db_object() will handle the error
      }
  }

  /**
   * <P>Get a reference to a read-only copy of an object in the 
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</P>
   *
   * <P>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.GanymedeSession#view_db_object(arlut.csd.ganymede.Invid) 
   * view_db_object()} method to get the object.</P>
   *
   * @param invid The invariant id of the object to be viewed.
   * @param getOriginal if true, viewDBObject will return the original
   * version of a DBEditObject in this session if the specified object
   * is in the middle of being deleted 
   */

  public DBObject viewDBObject(Invid invid, boolean getOriginal)
  {
    return viewDBObject(invid.getType(), invid.getNum(), getOriginal);
  }

  /**
   * <P>Get a reference to a read-only copy of an object in the 
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</P>
   *
   * <P>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.GanymedeSession#view_db_object(arlut.csd.ganymede.Invid) 
   * view_db_object()} method to get the object.</P>
   *
   * @param invid The invariant id of the object to be viewed.
   */

  public DBObject viewDBObject(Invid invid)
  {
    return viewDBObject(invid.getType(), invid.getNum());
  }

  /**
   * <P>Get a reference to a read-only copy of an object in the 
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</P>
   *
   * <P>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.GanymedeSession#view_db_object(arlut.csd.ganymede.Invid) 
   * view_db_object()} method to get the object.</P>
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be viewed.
   *
   * @param objectID The int id number of the object to be viewed within the specified
   *                 object base.
   */

  public DBObject viewDBObject(short baseID, int objectID)
  {
    return viewDBObject(baseID, objectID, false);
  }

  /**
   * <P>Get a reference to a read-only copy of an object in the 
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</P>
   *
   * <P>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.GanymedeSession#view_db_object(arlut.csd.ganymede.Invid) 
   * view_db_object()} method to get the object.</P>
   *
   * @param baseID The short id number of the DBObjectBase containing the object to
   *               be viewed.
   *
   * @param objectID The int id number of the object to be viewed within the specified
   *                 object base.
   *
   * @param getOriginal if true, viewDBObject will return the original
   * version of a DBEditObject in this session if the specified object
   * is in the middle of being deleted 
   */

  public DBObject viewDBObject(short baseID, int objectID, boolean getOriginal)
  {
    DBObjectBase base;
    DBObject     obj = null;

    /* -- */

    base = Ganymede.db.getObjectBase(baseID);

    if (base == null)
      {
	return null;
      }

    // this should be safe, as there shouldn't be any threads doing a
    // viewDBObject while the schema is being edited, by virtue of the
    // loginSemaphore.. otherwise, something wacky might happen, like
    // the DBObjectBase being broken down and having the objectTable
    // field set to null.

    // We use the DBObjectTable's synchronized get() method so that we
    // can look up objects even while the DBObjectBase is locked
    // during another transaction's commit

    obj = base.getObject(objectID);

    // if we aren't editing anything, we can't possibly have our own
    // version of the object checked out

    if (!isTransactionOpen())
      {
	return obj;
      }

    // if we are editing something, we need to be more careful about
    // synchronization with editing methods

    synchronized (this)
      {
	if (obj == null)
	  {
	    // not in transaction.. maybe we created it?
	    
	    return editSet.findObject(new Invid(baseID, objectID));
	  }
	
	// okay, we found it and we've got a transaction open.. see if the
	// object is being edited and, if so, if it is us that is doing it
	
	DBEditObject shadow = obj.shadowObject;
	
	if (shadow == null || shadow.getSession() != this)
	  {
	    return obj;
	  }

	// okay, the object is being edited by us.. if we are supposed to
	// return the original version of an object being deleted, and
	// this one is, return the original
	
	if (getOriginal && shadow.getStatus() == ObjectStatus.DELETING)
	  {
	    return obj;
	  }
	
	// else return the object being edited
	
	return shadow;
      }
  }

  /**
   * <P>Remove an object from the database</P>
   *
   * <P>This method method can only be called in the context of an open
   * transaction.  This method will mark an object for deletion.  When
   * the transaction is committed, the object is removed from the
   * database.  If the transaction is aborted, the object remains in
   * the database unchanged.</P>
   *
   * @param invid Invid of the object to be deleted
   */

  public ReturnVal deleteDBObject(Invid invid)
  {
    return deleteDBObject(invid.getType(), invid.getNum()); // *sync*
  }

  /**
   * <P>Remove an object from the database</P>
   *
   * <P>This method method can only be called in the context of an open
   * transaction.  This method will check an object out of the 
   * {@link arlut.csd.ganymede.DBStore DBStore}
   * and add it to the editset's deletion list.  When the transaction
   * is committed, the object has its remove() method called to do
   * cleanup, and the editSet nulls the object's slot in the DBStore.
   * If the transaction is aborted, the object remains in the database
   * unchanged.</P>
   *
   * @param baseID id of the object base containing the object to be deleted
   * @param objectID id of the object to be deleted
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

    if (isTerminating())
      {
	throw new RuntimeException("deleteDBObject called during transaction commit/abort");
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
   * <P>Remove an object from the database</P>
   *
   * <P>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the
   * only way to obtain a {@link arlut.csd.ganymede.DBEditObject DBEditObject}),
   * no other locking is
   * required. This method will take an object out of the
   * {@link arlut.csd.ganymede.DBStore DBStore}, do
   * whatever immediate removal logic is required, and mark it as
   * deleted in the transaction.  When the transaction is committed,
   * the object will be expunged from the database.</P>
   *
   * <P>Note that this method does not check to see whether permission
   * has been obtained to delete the object.. that's done in
   * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.GanymedeSession#remove_db_object(arlut.csd.ganymede.Invid) remove_db_object()}
   * method.</P>
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
      case DBEditObject.EDITING:

	// we have to checkpoint before we set the status to delete,
	// or else a later rollback will still leave the object in
	// must-delete status if the transaction is committed

	checkpoint(key);

	// by calling the synchronized setDeleteStatus() method on the
	// DBDeletionManager, we announce our intention to delete this
	// object, and lock out any other objects from establishing
	// asymmetrical links to this object..  if this fails, another
	// object in an open transaction has linked to us without
	// having checked us out for editing (which always means an
	// asymmetrical link), and we can't let the object be deleted

	if (!DBDeletionManager.setDeleteStatus(eObj, this))
	  {
	    // if setDeleteStatus() fails, nothing will have been changed,
	    // so we can just pop our checkpoint

	    popCheckpoint(key);

	    return Ganymede.createErrorDialog("Can't delete " + eObj.toString(),
					      eObj.toString() + " can't be deleted because an object which points " +
					      "to it is currently checked out for editing by someone else.");
	  }

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
	// ok, go ahead and finalize.. the finalizeRemove method will
	// handle doing a rollback or popCheckpoint if necessary

	// it is essential that we do this call, or else we might
	// leave namespace handles referencing this object

	retVal2 = eObj.finalizeRemove(true);

	return retVal2;
      }
  }

  /**
   * <p>Inactivate an object in the database</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the only
   * way to obtain a {@link arlut.csd.ganymede.DBEditObject DBEditObject}),
   * no other locking is required. This method
   * will take an object out of the {@link arlut.csd.ganymede.DBStore DBStore}
   * and proceed to do whatever is
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
   * {@link arlut.csd.ganymede.GanymedeSession#reactivate_db_object(arlut.csd.ganymede.Invid) 
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
   * <p>This method, when called on an embedded DBObject, recurses up the embedding
   * hierarchy to find the top-level embedding object.  If the embedded object is
   * an editable object in the process of being deleted (its status is set to 'DELETING'),
   * the returned top-level embedding object will be the original, pre-edited version,
   * so that the original label might be retrieved.</p>
   */

  DBObject getContainingObj(DBObject object)
  {
    DBObject localObj;
    InvidDBField inf = null;
    Invid inv = null;
    int loopcount = 0;
    boolean original;

    /* -- */

    // if we're looking at an embedded object, lets cascade up and
    // find the top-level ancestor

    if (false)
      {
	System.err.println("Trying to find top-level object for " + 
			   object.getTypeDesc() + ":" + 
			   object.getInvid().toString());
      }

    localObj = object;

    while (localObj != null && localObj.isEmbedded())
      {
	original = false;		// we haven't switched to check the original yet
	inf = (InvidDBField) localObj.getField(SchemaConstants.ContainerField);

	// if we need to find the top-level containing object for an
	// embedded object that has been or is in the process of being
	// deleted, we'll need to consult the original version of the
	// object to find its containing parent.

	if (inf == null)
	  {
	    if (true)
	      {
		Ganymede.debug("Couldn't initially get a container field for object " + localObj.toString());
	      }

	    if (localObj instanceof DBEditObject)
	      {
		if (false)
		  {
		    System.err.println("Object " + localObj.toString() + " is a DBEditObject.");
		  }

		DBEditObject localEditObj = (DBEditObject) localObj;

		if (localEditObj.getStatus() == ObjectStatus.DELETING)
		  {
		    if (false)
		      {
			System.err.println("Object " + localObj.toString() + " has status DELETING.");
		      }

		    localObj = localEditObj.getOriginal();
		  }

		inf = (InvidDBField) localObj.getField(SchemaConstants.ContainerField);
		original = true;
	      }
	  }

	if (inf == null)
	  {
	    if (true)
	      {
		Ganymede.debug("getContainingObj(): Couldn't get a container field for object " + localObj.toString() + " at all.");
	      }

	    localObj = null;
	    break;
	  }

	inv = (Invid) inf.getValueLocal();

	if (inv == null && !original)
	  {
	    if (localObj instanceof DBEditObject)
	      {
		if (false)
		  {
		    System.err.println("Object " + localObj.toString() + " is a DBEditObject.");
		  }

		DBEditObject localEditObj = (DBEditObject) localObj;

		if (localEditObj.getStatus() == ObjectStatus.DELETING)
		  {
		    if (false)
		      {
			System.err.println("Object " + localObj.toString() + " has status DELETING.");
		      }

		    localObj = localEditObj.getOriginal();
		  }

		inf = (InvidDBField) localObj.getField(SchemaConstants.ContainerField);
		original = true;
	      }

	    inv = (Invid) inf.getValueLocal();
	  }

	if (inv == null)
	  {
	    if (true)
	      {
		Ganymede.debug("getContainingObj() error <2:" + loopcount +
			       "> .. Couldn't get a container invid for object " + localObj.getLabel());
	      }

	    localObj = null;
	    break;
	  }

	// remember, viewDBObject() will get an object even if it was
	// created in the current transaction

	localObj = viewDBObject(inv);
	loopcount++;
      }

    if (localObj == null)
      {
	throw new IntegrityConstraintException("getContainingObj() couldn't find owner of embedded object " + 
					       object.getLabel());
      }

    return localObj;
  }

  /**
   * <p>Convenience pass-through method</p>
   *
   * <p>This method may block if another thread has already checkpointed
   * this transaction.  Checkpoints are intended to be of definite extent,
   * as the interleaving of checkpoints by multiple threads would lead
   * to trouble.</p>
   * 
   * @see arlut.csd.ganymede.DBEditSet#checkpoint(java.lang.String) 
   */

  public final void checkpoint(String name)
  {
    if (editSet != null)
      {
	editSet.checkpoint(name); // *synchronized*
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
	point = editSet.popCheckpoint(name); // *synchronized*
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
	return editSet.rollback(name); // *synchronized*
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
   * <p>Establishes a read lock for the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}s
   * in bases.</p>
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

    // we'll never be able to establish a read lock if we have to
    // wait for this thread to release an existing write lock..

    if (lockVect.size() != 0)
      {
	boolean lockOK;

	for (int i = 0; i < lockVect.size(); i++)
	  {
	    DBLock oldLock = (DBLock) lockVect.elementAt(i);

	    if (oldLock instanceof DBWriteLock)
	      {
		if (oldLock.overlaps(bases))
		  {
		    throw new InterruptedException("Can't establish read lock, session " + getID() +
						   " already has overlapping write lock:\n" +
						   oldLock.toString());
		  }
	      }
	  }
      }

    lock = new DBReadLock(store, bases);

    lockVect.addElement(lock);

    lock.establish(key);

    return lock;
  }

  /**
   * <P>openReadLock establishes a read lock for the entire
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>The thread calling this method will block until the read lock 
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.</P>
   *
   * <P>All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.</P>
   */

  public synchronized DBReadLock openReadLock() throws InterruptedException
  {
    DBReadLock lock;

    /* -- */

    // we'll never be able to establish a read lock if we have to
    // wait for this thread to release an existing write lock..

    if (lockVect.size() != 0)
      {
	for (int i = 0; i < lockVect.size(); i++)
	  {
	    DBLock oldLock = (DBLock) lockVect.elementAt(i);

	    if (oldLock instanceof DBWriteLock)
	      {
		throw new InterruptedException("Can't establish global read lock, session " + getID() +
					       " already has write lock:\n" +
					       oldLock.toString());
	      }
	  }
      }

    lock = new DBReadLock(store);
    lockVect.addElement(lock);

    lock.establish(key);

    return lock;
  }

  /**
   * <p>Establishes a write lock for the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}s
   * in bases.</p>
   *
   * <p>The thread calling this method will block until the write lock
   * can be established.  If this DBSession already possesses a write lock,
   * read lock, or dump lock, the openWriteLock() call will fail with
   * an InterruptedException.</p>
   *
   * <p>If one or more different DBSessions (besides this) have locks in
   * place that would block acquisition of the write lock, this method
   * will block until the lock can be acquired.</p>
   */

  public synchronized DBWriteLock openWriteLock(Vector bases) throws InterruptedException
  {
    DBWriteLock lock;

    /* -- */

    // we'll never be able to establish a write lock if we have to
    // wait for this thread to release read, write, or dump locks..
    // and we must not have pre-existing locks on bases not
    // overlapping with our bases parameter either, or else we risk
    // dead-lock later on..

    if (lockVect.size() != 0)
      {
	StringBuffer resultBuffer = new StringBuffer();

	for (int i = 0; i < lockVect.size(); i++)
	  {
	    resultBuffer.append(lockVect.elementAt(i).toString());
	    resultBuffer.append("\n");
	  }

	throw new InterruptedException("Can't establish write lock, session " + getID() + 
				       " already has locks:\n" +
				       resultBuffer.toString());
      }

    lock = new DBWriteLock(store, bases);

    lockVect.addElement(lock);

    lock.establish(key);

    return lock;
  }

  /**
   * <P>This method establishes a dump lock on all object bases in this Ganymede
   * server.</P>
   */

  public synchronized DBDumpLock openDumpLock() throws InterruptedException
  {
    DBDumpLock lock;

    /* -- */

    // we'll never be able to establish a dump lock if we have to
    // wait for this thread to release an existing write lock..

    if (lockVect.size() != 0)
      {
	for (int i = 0; i < lockVect.size(); i++)
	  {
	    DBLock oldLock = (DBLock) lockVect.elementAt(i);

	    if (oldLock instanceof DBWriteLock)
	      {
		throw new InterruptedException("Can't establish global dump lock, session " + getID() +
					       " already has write lock:\n" +
					       oldLock.toString());
	      }
	  }
      }

    lock = new DBDumpLock(store);
    lockVect.addElement(lock);

    lock.establish(key);

    return lock;
  }

  /**
   * <P>releaseLock releases a particular lock held by this session.
   * This method will not force a lock being held by another thread to
   * drop out of its establish method.. it is intended to be called by
   * the same thread that established the lock.</P>
   *
   * <P>This method must be synchronized to avoid conflicting with
   * iterations on lockVect.</P> */

  public synchronized void releaseLock(DBLock lock)
  {
    lock.release();		// *sync* DBStore
    lockVect.removeElement(lock);
  }

  /**
   * <P>releaseAllLocks() releases all locks held by this
   * session.</P>
   *
   * <P>This method is *not* synchronized.  This method must
   * only be called by code synchronized on this DBSession
   * instance, as for instance {@link arlut.csd.ganymede.DBSession#logout() logout()}
   * and {@link arlut.csd.ganymede.DBSession#commitTransaction() commitTransaction()}.</P>
   */

  public void releaseAllLocks()
  {
    DBLock lock;
    Enumeration enum = lockVect.elements();

    /* -- */

    if (debug)
      {
	System.err.println(key + ": releasing all locks");
      }
    
    while (enum.hasMoreElements())
      {
	lock = (DBLock) enum.nextElement();
	lock.abort();		// blocks until the lock can be cleared
      }

    lockVect.removeAllElements();
  }

  /**
   * <P>openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject() and 
   * createDBObject() to obtain {@link arlut.csd.ganymede.DBEditObject DBEditObject}s.
   * Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction() is called.</P>
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @see arlut.csd.ganymede.DBEditObject
   */ 

  public void openTransaction(String describe)
  {
    this.openTransaction(describe, true);
  }

  /**
   * <P>openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject() and 
   * createDBObject() to obtain {@link arlut.csd.ganymede.DBEditObject DBEditObject}s.
   * Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction() is called.</P>
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @param interactive If false, this transaction will operate in
   * non-interactive mode.  Certain Invid operations will be optimized
   * to avoid doing choice list queries and bind checkpoint
   * operations.  When a transaction is operating in non-interactive mode,
   * any failure that cannot be handled cleanly due to the optimizations will
   * result in the transaction refusing to commit when commitTransaction()
   * is attempted.  This mode is intended for batch operations.
   *
   * @see arlut.csd.ganymede.DBEditObject 
   */

  public synchronized void openTransaction(String describe, boolean interactive)
  {
    if (editSet != null)
      {
	throw new IllegalArgumentException("transaction already open.");
      }

    terminating = false;

    editSet = new DBEditSet(store, this, describe, interactive);
  }

  /**
   * <P>commitTransaction causes any changes made during the context of
   * a current transaction to be performed.  Appropriate portions of the
   * database are locked, changes are made to the in-memory image of
   * the database, and a description of the changes is placed in the
   * {@link arlut.csd.ganymede.DBStore DBStore} journal file.  Event
   * logging / mail notification may take place.</P>
   *
   * <P>The session must not hold any locks when commitTransaction is
   * called.  The symmetrical invid references between related objects
   * and the atomic namespace management code should guarantee that no
   * incompatible change is made with respect to any checked out objects
   * while the Bases are unlocked.</P>
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.DBEditObject
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

    // we can't commit a transaction with locks held, because that
    // might lead to deadlock.  we release all locks now, then when we
    // call editSet.commit(), that will attempt to establish whatever
    // write locks we need, for the duration of the commit() call.

    releaseAllLocks();

    if (debug)
      {
	System.err.println(key + ": commiting editset");
      }

    try
      {
	terminating = true;

	retVal = editSet.commit(); // *synchronized*
	
	if (retVal == null || retVal.didSucceed())
	  {
	    Ganymede.debug(key + ": committed transaction " + editSet.description);
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
      }
    finally
      {
	terminating = false;
      }

    return retVal;		// later on we'll figure out how to do this right
  }

  /**
   * <P>abortTransaction causes all {@link arlut.csd.ganymede.DBEditObject DBEditObject}s
   * that were pulled during
   * the course of the session's transaction to be released without affecting
   * the state of the database.  Any changes made to
   * {@link arlut.csd.ganymede.DBObject DBObject}s pulled for editing
   * by this session during this transaction are abandoned.  Any objects created
   * or destroyed by this session during this transaction are abandoned / unaffected
   * by the actions during the transaction.</P>
   *
   * <P>Calling abortTransaction() has no affect on any locks held by
   * this session, but generally no locks should be held here.
   * abortTransaction() will attempt to abort a write lock being
   * established by a commitTransaction() call on another thread.</P>
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
	// if we are called while our DBEditSet transaction object is
	// waiting on a write lock in order to commit() on another
	// thread, try to kill it off.  We synchronize on
	// Ganymede.db.lockSync here because we are using that as a
	// monitor for all lock operations, and we need the
	// wLock.inEstablish check to be sync'ed so that we don't
	// force an abort after the editSet has gotten its lock
	// established and is busy mucking with the server's
	// DBObjectTables.

	synchronized (Ganymede.db.lockSync)
	  {
	    if (editSet.wLock.inEstablish)
	      {
		terminating = true;

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
      }

    editSet.release();		// *synchronized*
    editSet = null;		// for gc

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
   * <p>This method returns true if this session is carrying out a
   * transaction on behalf of an interactive client.</p>
   */

  public synchronized boolean isInteractive()
  {
    if (editSet == null)
      {
	throw new IllegalArgumentException("isInteractive() called with null editSet");
      }

    return editSet.isInteractive();
  }

  /**
   * <p>This method returns true if this session is in the middle of committing
   * or aborting this session's transaction.</p>
   */

  public boolean isTerminating()
  {
    return terminating;
  }

  /**
   * <P>internal method for setting error messages resulting from session activities.</P>
   *
   * <P>this method may eventually be hooked up to a more general logging
   * mechanism.</P>
   *
   * @deprecated This method is obsoleted by the use of ReturnVal
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
   * <P>This method is responsible for providing an identifier string
   * for the user who this session belongs to, and is used for
   * logging and what-not.</P>
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
   * <P>This method returns a handle to the Ganymede Session
   * that owns this DBSession.</P>
   */

  public GanymedeSession getGSession()
  {
    return GSession;
  }

  /**
   * <P>This method returns a handle to the objectHook for
   * a particular Invid.</P>
   */

  public DBEditObject getObjectHook(Invid invid)
  {
    DBObjectBase base;

    base = Ganymede.db.getObjectBase(invid.getType());
    return base.objectHook;
  }

  public String toString()
  {
    if (editSet != null)
      {
	return "DBSession[" + editSet.description + "]";
      }
    else
      {
	return super.toString();
      }
  }
}
