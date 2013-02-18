/*

   DBSession.java

   The GANYMEDE object storage system.

   Created: 26 August 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import arlut.csd.Util.RandomUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.QueryDescriber;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBSession

------------------------------------------------------------------------------*/

/**
 * <p>DBSession is the Ganymede server's
 * {@link arlut.csd.ganymede.server.DBStore DBStore}-level session class.  Each
 * client or server process that interacts with the Ganymede database
 * must eventually do so through a DBSession object.  Clients and
 * server processes generally interact directly with a
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}, by way of
 * the {@link arlut.csd.ganymede.rmi.Session Session} interface on the
 * part of the client.  The GanymedeSession talks to the DBSession class
 * to actually interact with the database.</p>
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
 * <p>The DBSession contains code and logic to actually manipulate the
 * Ganymede database (the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase},
 * {@link arlut.csd.ganymede.server.DBObject DBObject}, and
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} objects held
 * in the DBStore).  The DBSession class connects to the extensive
 * transaction logic implemented in the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
 * class, as well as the database locking handled by the
 * {@link arlut.csd.ganymede.server.DBLock DBLock} class.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

final public class DBSession implements QueryDescriber {

  static boolean debug = false;

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  /**
   * <p>TranslationService object for handling string localization in the Ganymede
   * server.</p>
   */

  static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBSession");

  // ---

  /**
   * <p>User-level session reference.  As mentioned above,
   * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession} has the
   * user-level permissions handling, while DBSession has the database
   * handling.</p>
   */

  GanymedeSession GSession;

  /**
   * <p>Root object of the Ganymede database system.</p>
   */

  DBStore store;

  /**
   * <p>Manager for locks held by this session.</p>
   */

  private DBSessionLockManager lockManager;

  /**
   * <p>Transaction handle for this session.</p>
   */

  DBEditSet editSet;

  /**
   * <p>Identifying key used in the lock system to identify owner of
   * locks.</p>
   */

  private Object key;

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
   * @param key An identifying key with meaning to whatever code is
   * using arlut.csd.ganymede.  Must be unique for DBObjectBase locking.
   *
   */

  DBSession(DBStore store, GanymedeSession GSession, Object key)
  {
    this.store = store;
    this.key = key;
    this.GSession = GSession;

    editSet = null;

    lockManager = new DBSessionLockManager(this);
  }

  /**
   * <p>Close out this DBSession, aborting any open transaction, and releasing
   * any held read/write/dump locks.</p>
   */

  public synchronized void logout()
  {
    releaseAllLocks();

    if (editSet != null)
      {
        abortTransaction();
      }

    // help GC

    store = null;
    GSession = null;
    lockManager = null;
    key = null;
  }

  /**
   * <p>This method is provided so that custom
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} subclasses
   * can get access to methods on our DBStore.</p>
   */

  public DBStore getStore()
  {
    return store;
  }

  /**
   * <p>Create a new object in the database.</p>
   *
   * <p>This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.</p>
   *
   * <p>The created object will be given an object id.
   * The {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} can
   * be queried to determine its invid.</p>
   *
   * <p>The created DBEditObject will have its fields initialized
   * by the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * {@link arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook}
   * custom DBEditObject's
   * {@link arlut.csd.ganymede.server.DBEditObject#initializeNewObject() initializeNewObject()}
   * method.</p>
   *
   * <p>This method will return null if the object could
   * not be constructed and initialized for some reason.</p>
   *
   * @param object_type Type of the object to be created
   * @param chosenSlot Invid to create the new object with.
   * normally only used in internal Ganymede code in conjunction with
   * the addition of new kinds of built-in objects during development
   * @param owners Vector of invids for owner group objects to make initial
   * owners for the newly created object
   *
   * @see arlut.csd.ganymede.server.DBStore
   */

  public synchronized ReturnVal createDBObject(short object_type, Invid chosenSlot, Vector<Invid> owners)
  {
    DBObjectBase base;
    DBEditObject e_object;
    ReturnVal retVal = null;

    /* -- */

    if (editSet == null)
      {
        throw new RuntimeException(ts.l("global.notransaction", "createDBObject"));
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

        // "Object Creation Failure"
        // "Couldn''t create the new object in the database."
        return Ganymede.createErrorDialog(ts.l("createDBObject.failure"),
                                          ts.l("createDBObject.failure_text"));
      }

    // Checkpoint the transaction at this point so that we can
    // recover if we can't get the object into the owner groups
    // it needs to go into

    String ckp_label = RandomUtils.getSaltedString("create[" + base.getName() + "]");

    checkpoint(ckp_label);
    boolean checkpointed = true;

    try
      {
        // set ownership for this new object if it is not an embedded object

        if (!base.isEmbedded() && (owners != null))
          {
            InvidDBField inf = (InvidDBField) e_object.getField(SchemaConstants.OwnerListField);

            /* -- */

            for (Invid tmpInvid: owners)
              {
                if (tmpInvid.getType() != SchemaConstants.OwnerBase)
                  {
                    throw new RuntimeException(ts.l("createDBObject.badowner"));
                  }

                // we don't want to explicitly record supergash ownership

                if (tmpInvid.getNum() == SchemaConstants.OwnerSupergash)
                  {
                    continue;
                  }

                retVal = inf.addElementLocal(tmpInvid);

                if (!ReturnVal.didSucceed(retVal))
                  {
                    try
                      {
                        DBObject owner = viewDBObject(tmpInvid);
                        String name = owner.getLabel();

                        String checkedOutBy = owner.shadowObject.editset.description;

                        retVal.getDialog().appendText("\n" + ts.l("createDBObject.checkedout", name, checkedOutBy));
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
            return Ganymede.createErrorDialog(ts.l("createDBObject.failure"),
                                              ts.l("createDBObject.addObject_failed"));
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

            if (!ReturnVal.didSucceed(retVal))
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
            Ganymede.debug(ts.l("createDBObject.created", e_object.getLabel(), e_object.getInvid().toString()));
            db_field[] fields = e_object.listFields();

            for (int i = 0; i < fields.length; i++)
              {
                Ganymede.debug(ts.l("createDBObject.field_report", Integer.valueOf(i), Integer.valueOf(fields[i].getID()), fields[i].getName()));
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
   * <p>Create a new object in the database.</p>
   *
   * <p>This method creates a slot in the object base of the
   * proper object type.  The created object is associated
   * with the current transaction.  When the transaction
   * is committed, the created object will inserted into
   * the database, and will become visible to other
   * sessions.</p>
   *
   * <p>The created object will be given an object id.
   * The {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * can be queried to determine its invid.</p>
   *
   * <p>The created DBEditObject will have its fields initialized
   * by the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * {@link arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook}
   * custom DBEditObject's
   * {@link arlut.csd.ganymede.server.DBEditObject#initializeNewObject() initializeNewObject()}
   * method.</p>
   *
   * <p>This method returns a ReturnVal object to convey the
   * result of the creation.  Call the
   * {@link arlut.csd.ganymede.common.ReturnVal#getObject() getObject()} method on
   * the returned ReturnVal in order to get the created DBEditObject.  Note
   * that the ReturnVal.getObject() method is intended to support passing
   * a remote db_object reference to the client, so on the server, it is
   * necessary to cast the db_object reference to a DBEditObject reference
   * for use on the server.</p>
   *
   * @param object_type Type of the object to be created
   *
   * @see arlut.csd.ganymede.server.DBStore
   */

  public ReturnVal createDBObject(short object_type, Vector<Invid> owners)
  {
    return createDBObject(object_type, null, owners);
  }

  /**
   * <p>Pull an object out of the database for editing.</p>
   *
   * <p>This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.</p>
   *
   * <p>The session has to have a transaction opened before it can pull
   * an object out for editing.</p>
   *
   * @param invid The invariant id of the object to be modified.
   *
   * @return null if the object could not be found for editing
   *
   * @see arlut.csd.ganymede.server.DBObjectBase
   */

  public DBEditObject editDBObject(Invid invid)
  {
    return editDBObject(invid.getType(), invid.getNum());
  }

  /**
   * <p>Pull an object out of the database for editing.</p>
   *
   * <p>This method is used to check an object out of the database for editing.
   * Only one session can have a particular object checked out for editing at
   * a time.</p>
   *
   * <p>The session has to have a transaction opened before it can pull a
   * new object out for editing.  If the object specified by &lt;baseID,
   * objectID&gt; is part of the current transaction, the transactional
   * copy will be returned, and no readLock is strictly necessary in
   * that case.</p>
   *
   * <p>This method doesn't do permission checking.. that is performed at the
   * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession} level.</p>
   *
   * @param baseID The short id number of the
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} containing the object to
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
        throw new RuntimeException(ts.l("global.notransaction", "editDBObject"));
      }

    obj = viewDBObject(baseID, objectID);

    if (obj == null)
      {
        System.err.println(ts.l("editDBObject.noobject", Integer.valueOf(baseID), Integer.valueOf(objectID)));
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
   * <p>Get a reference to a read-only copy of an object in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</p>
   *
   * <p>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
   * view_db_object()} method to get the object.</p>
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
   * <p>Get a reference to a read-only copy of an object in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</p>
   *
   * <p>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
   * view_db_object()} method to get the object.</p>
   *
   * @param invid The invariant id of the object to be viewed.
   */

  public DBObject viewDBObject(Invid invid)
  {
    return viewDBObject(invid.getType(), invid.getNum());
  }

  /**
   * <p>Get a reference to a read-only copy of an object in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</p>
   *
   * <p>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
   * view_db_object()} method to get the object.</p>
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
   * <p>Get a reference to a read-only copy of an object in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>If this session has a transaction currently open, this method will return
   * the checked out shadow of invid, if it has been checked out by this
   * transaction.</p>
   *
   * <p>Note that unless the object has been checked out by the current session,
   * this method will return access to the object as it is stored directly
   * in the main datastore hashes.  This means that the object will be
   * read-only and will grant all accesses, as it will have no notion of
   * what session or transaction owns it.  If you need to have access to the
   * object's fields be protected, use {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
   * view_db_object()} method to get the object.</p>
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
            // not in the persistent store.. maybe we created it in
            // this transaction, or maybe it just doesn't exist.

            return editSet.findObject(Invid.createInvid(baseID, objectID));
          }

        // okay, we found it and we've got a transaction open.. see if the
        // object is being edited and, if so, if it is us that is doing it

        DBEditObject shadow = obj.shadowObject;

        if (shadow == null || shadow.getDBSession() != this)
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
   * <p>Remove an object from the database</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction.  This method will mark an object for deletion.  When
   * the transaction is committed, the object is removed from the
   * database.  If the transaction is aborted, the object remains in
   * the database unchanged.</p>
   *
   * @param invid Invid of the object to be deleted
   */

  public ReturnVal deleteDBObject(Invid invid)
  {
    return deleteDBObject(invid.getType(), invid.getNum()); // *sync*
  }

  /**
   * <p>Remove an object from the database</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction.  This method will check an object out of the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}
   * and add it to the editset's deletion list.  When the transaction
   * is committed, the object has its remove() method called to do
   * cleanup, and the editSet nulls the object's slot in the DBStore.
   * If the transaction is aborted, the object remains in the database
   * unchanged.</p>
   *
   * @param baseID id of the object base containing the object to be deleted
   * @param objectID id of the object to be deleted
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal deleteDBObject(short baseID, int objectID)
  {
    DBObject obj;
    DBEditObject eObj;

    /* -- */

    if (editSet == null)
      {
        throw new RuntimeException(ts.l("global.notransaction", "deleteDBObject"));
      }

    obj = viewDBObject(baseID, objectID);

    // we have to have an editable object in order to delete it.. see
    // if we can do that

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
        return Ganymede.createErrorDialog(ts.l("deleteDBObject.cant_delete", obj.getLabel()),
                                          ts.l("deleteDBObject.cant_delete_text", obj.getLabel()));
      }

    return deleteDBObject(eObj);
  }

  /**
   * <p>Remove an object from the database</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the
   * only way to obtain a {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}),
   * no other locking is
   * required. This method will take an object out of the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}, do
   * whatever immediate removal logic is required, and mark it as
   * deleted in the transaction.  When the transaction is committed,
   * the object will be expunged from the database.</p>
   *
   * <p>Note that this method does not check to see whether permission
   * has been obtained to delete the object.. that's done in
   * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#remove_db_object(arlut.csd.ganymede.common.Invid) remove_db_object()}
   * method.</p>
   *
   * @param eObj An object checked out in the current transaction to be deleted
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal deleteDBObject(DBEditObject eObj)
  {
    ReturnVal retVal, retVal2;
    String ckp_label;

    /* -- */

    if (editSet == null)
      {
        throw new RuntimeException(ts.l("global.notransaction", "deleteDBObject"));
      }

    ckp_label = RandomUtils.getSaltedString("del[" + eObj.getLabel() + "]");

    switch (eObj.getStatus())
      {
      case DBEditObject.CREATING:
      case DBEditObject.EDITING:

        // we have to checkpoint before we set the status to delete,
        // or else a later rollback will still leave the object in
        // must-delete status if the transaction is committed

        checkpoint(ckp_label);

        // by calling the synchronized setDeleteStatus() method on the
        // DBDeletionManager, we announce our intention to delete this
        // object, and lock out any other objects from establishing
        // asymmetrical links to this object..  if this fails, another
        // object in an open transaction has linked to this eObj
        // without having checked it out for editing (which always
        // means an asymmetrical link), and we can't let the object be
        // deleted

        if (!DBDeletionManager.setDeleteStatus(eObj, this))
          {
            // if setDeleteStatus() fails, nothing will have been changed,
            // so we can just pop our checkpoint

            popCheckpoint(ckp_label);

            return Ganymede.createErrorDialog(ts.l("deleteDBObject.cant_delete", eObj.toString()),
                                              ts.l("deleteDBObject.cant_delete_text2", eObj.toString()));
          }

        break;

      case DBEditObject.DELETING:
      case DBEditObject.DROPPING:

        // already to be deleted

        return null;
      }

    try
      {
        retVal = eObj.remove();
      }
    catch (Throwable ex)
      {
        Ganymede.logError(ex);

        rollback(ckp_label);

        return Ganymede.createErrorDialog(ts.l("deleteDBObject.error"),
                                          ts.l("deleteDBObject.error_text", eObj.toString(), ex.getMessage()));
      }

    // the remove logic can entirely bypass our normal finalize logic

    if (!ReturnVal.didSucceed(retVal))
      {
        if (retVal.getCallback() == null)
          {
            // oops, irredeemable failure.  rollback.

            rollback(ckp_label);
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

        retVal2 = eObj.finalizeRemove(true, ckp_label);

        return retVal2;
      }
  }

  /**
   * <p>Inactivate an object in the database</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the only
   * way to obtain a {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}),
   * no other locking is required. This method
   * will take an object out of the {@link arlut.csd.ganymede.server.DBStore DBStore}
   * and proceed to do whatever is
   * necessary to cause that object to be 'inactivated'.</p>
   *
   * <p>Note that this method does not check to see whether permission
   * has been obtained to inactivate the object.. that's done in
   * {@link arlut.csd.ganymede.server.GanymedeSession#inactivate_db_object(arlut.csd.ganymede.common.Invid)
   * GanymedeSession.inactivate_db_object()}.</p>
   *
   * @param eObj An object checked out in the current transaction to be inactivated
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal inactivateDBObject(DBEditObject eObj)
  {
    ReturnVal retVal;
    String ckp_label;

    /* -- */

    if (editSet == null)
      {
        throw new RuntimeException(ts.l("global.notransaction", "inactivateDBObject"));
      }

    ckp_label = RandomUtils.getSaltedString("inactivate[" + eObj.getLabel() + "]");

    switch (eObj.getStatus())
      {
      case DBEditObject.EDITING:
      case DBEditObject.CREATING:
        break;

      default:
        return Ganymede.createErrorDialog(ts.l("inactivateDBObject.error"),
                                          ts.l("inactivateDBObject.error_text"));
      }

    checkpoint(ckp_label);

    if (debug)
      {
        System.err.println("DBSession.inactivateDBObject(): Calling eObj.inactivate()");
      }

    try
      {
        retVal = eObj.inactivate(ckp_label);
      }
    catch (Throwable ex)
      {
        Ganymede.logError(ex);

        // oops, irredeemable failure.  rollback.

        eObj.finalizeInactivate(false, ckp_label);

        return Ganymede.createErrorDialog(ts.l("inactivateDBObject.error"),
                                          ts.l("inactivateDBObject.error_text2", eObj.toString(), ex.getMessage()));
      }

    if (debug)
      {
        System.err.println("DBSession.inactivateDBObject(): Got back from eObj.inactivate()");
      }

    if (!ReturnVal.didSucceed(retVal))
      {
        if (retVal.getCallback() == null)
          {
            // oops, irredeemable failure.  rollback.

            System.err.println("DBSession.inactivateDBObject(): object refused inactivation, rolling back");

            eObj.finalizeInactivate(false, ckp_label);
          }

        // otherwise, we've got a wizard that the client will deal with.
      }
    else
      {
        // immediate success!

        eObj.finalizeInactivate(true, ckp_label);
      }

    return retVal;
  }

  /**
   * <p>Reactivates an object in the database.</p>
   *
   * <p>This method method can only be called in the context of an open
   * transaction. Because the object must be checked out (which is the only
   * way to obtain a {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}),
   * no other locking is required. This method
   * will take an object out of the {@link arlut.csd.ganymede.server.DBStore DBStore}
   * and proceed to do whatever is
   * necessary to cause that object to be 'inactivated'.</p>
   *
   * <p>Note that this method does not specifically check to see whether permission
   * has been obtained to reactivate the object.. that's done in
   * {@link arlut.csd.ganymede.server.GanymedeSession#reactivate_db_object(arlut.csd.ganymede.common.Invid)
   * GanymedeSession.reactivate_db_object()}.</p>
   *
   * @param eObj An object checked out in the current transaction to be reactivated
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal reactivateDBObject(DBEditObject eObj)
  {
    ReturnVal retVal;
    String ckp_label;

    /* -- */

    if (editSet == null)
      {
        throw new RuntimeException(ts.l("global.notransaction", "reactivateDBObject"));
      }

    ckp_label = RandomUtils.getSaltedString("reactivate[" + eObj.getLabel() + "]");

    switch (eObj.getStatus())
      {
      case DBEditObject.DELETING:
      case DBEditObject.DROPPING:
        return Ganymede.createErrorDialog(ts.l("reactivateDBObject.error"),
                                          ts.l("reactivateDBObject.error_text"));
      }

    if (!eObj.isInactivated())
      {
        return Ganymede.createErrorDialog(ts.l("reactivateDBObject.error"),
                                          ts.l("reactivateDBObject.error_text2"));
      }

    checkpoint(ckp_label);

    System.err.println(ts.l("reactivateDBObject.debug1"));

    try
      {
        retVal = eObj.reactivate(ckp_label);
      }
    catch (Throwable ex)
      {
        Ganymede.logError(ex);

        // oops, irredeemable failure.  rollback.

        rollback(ckp_label);

        return Ganymede.createErrorDialog(ts.l("reactivateDBObject.error"),
                                          ts.l("reactivateDBObject.error_text3", eObj.toString(), ex.getMessage()));
      }

    System.err.println(ts.l("reactivateDBObject.debug2"));

    if (!ReturnVal.didSucceed(retVal))
      {
        if (retVal.getCallback() == null)
          {
            // oops, irredeemable failure.  rollback.

            System.err.println(ts.l("reactivateDBObject.debug3"));

            rollback(ckp_label);
          }
      }
    else
      {
        // immediate success!

        eObj.finalizeReactivate(true, ckp_label);
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
    DBObject localObj = object;

    while (localObj != null && localObj.isEmbedded())
      {
        if (!localObj.isDefined(SchemaConstants.ContainerField) &&
            (localObj instanceof DBEditObject) &&
            ((DBEditObject) localObj).getStatus() == ObjectStatus.DELETING)
          {
            localObj = ((DBEditObject) localObj).getOriginal();
          }

        Invid inv = (Invid) localObj.getFieldValueLocal(SchemaConstants.ContainerField);

        if (inv == null)
          {
            // "getContainingObj() couldn''t find owner of embedded object {0}"
            throw new IntegrityConstraintException(ts.l("getContainingObj.integrity", object.getLabel()));
          }

        localObj = viewDBObject(inv);
      }

    return localObj;
  }

  /**
   * <p>This is a method to allow code in the server to quickly and
   * safely get a full list of objects in an object base.</p>
   *
   * <p>This is only a server-side method.  getObjects() does
   * not do anything to check access permissions.</p>
   *
   * <p>It is the responsiblity of the code that gets a List
   * back from this method not to modify the List returned
   * in any way, as it may be shared by other threads.</p>
   *
   * <p>Any objects returned by getObjects() will reflect the
   * state of that object in this session's transaction, if a
   * transaction is open.</p>
   *
   * @return a List of DBObject references.
   */

  public synchronized List<DBObject> getTransactionalObjects(short baseid)
  {
    DBObjectBase base;

    /* -- */

    base = Ganymede.db.getObjectBase(baseid);

    if (base == null)
      {
        try
          {
            throw new RuntimeException(ts.l("getTransactionalObjects.no_base", Integer.valueOf(baseid)));
          }
        catch (RuntimeException ex)
          {
            Ganymede.debug(Ganymede.stackTrace(ex));
            return null;
          }
      }

    if (!isTransactionOpen())
      {
        // return a snapshot reference to the base's iteration set

        return base.getIterationSet();
      }
    else
      {
        List<DBObject> iterationSet;
        Map<Invid, DBEditObject> objects;

        // grab a snapshot reference to the vector of objects
        // checked into the database

        iterationSet = base.getIterationSet();

        // grab a snapshot copy of the objects checked out in this transaction

        objects = editSet.getObjectHashClone();

        // and generate our list

        List<DBObject> results = new ArrayList<DBObject>(iterationSet.size());

        for (DBObject obj: iterationSet)
          {
            if (objects.containsKey(obj.getInvid()))
              {
                results.add(objects.get(obj.getInvid()));
              }
            else
              {
                results.add(obj);
              }
          }

        // drop our reference to the iterationSet

        iterationSet = null;

        // we've recorded any objects that are in the database.. now
        // look to see if there are any objects that are newly created
        // in our transaction's object list and add them as well.

        for (DBEditObject eObj: objects.values())
          {
            if ((eObj.getStatus() == ObjectStatus.CREATING) && (eObj.getTypeID()==baseid))
              {
                results.add(eObj);
              }
          }

        return results;
      }
  }

  /**
   * <p>Convenience pass-through method</p>
   *
   * <p>This method may block if another thread has already checkpointed
   * this transaction.  Checkpoints are intended to be of definite extent,
   * as the interleaving of checkpoints by multiple threads would lead
   * to trouble.</p>
   *
   * @see arlut.csd.ganymede.server.DBEditSet#checkpoint(java.lang.String)
   */

  public final void checkpoint(String name)
  {
    editSet.checkpoint(name); // *synchronized*
  }

  /**
   * <p>Convenience pass-through method</p>
   *
   * @see arlut.csd.ganymede.server.DBEditSet#popCheckpoint(java.lang.String)
   */

  public final boolean popCheckpoint(String name)
  {
    DBCheckPoint point = null;

    /* -- */

    point = editSet.popCheckpoint(name); // *synchronized*

    return (point != null);
  }

  /**
   * <p>Convenience pass-through method</p>
   *
   * @see arlut.csd.ganymede.server.DBEditSet#rollback(java.lang.String)
   */

  public final boolean rollback(String name)
  {
    return editSet.rollback(name); // *synchronized*
  }

  /**
   * <p>Returns true if the session's lock is currently locked, false
   * otherwise.</p>
   */

  public boolean isLocked(DBLock lockParam)
  {
    return lockManager.isLocked(lockParam);
  }

  /**
   * <p>Establishes a read lock for the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}s
   * in bases.</p>
   *
   * <p>The thread calling this method will block until the read lock
   * can be established.  If any of the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBases}
   * in the bases vector have transactions
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.</p>
   *
   * <p>All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.</p>
   */

  public DBReadLock openReadLock(Vector<DBObjectBase> bases) throws InterruptedException
  {
    return lockManager.openReadLock(bases);
  }

  /**
   * <p>openReadLock establishes a read lock for the entire
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>The thread calling this method will block until the read lock
   * can be established.  If transactions on the database are
   * currently committing, the establishment of the read lock will be suspended
   * until all such transactions are committed.</p>
   *
   * <p>All viewDBObject calls done within the context of an open read lock
   * will be transaction consistent.  Other sessions may pull objects out for
   * editing during the course of the session's read lock, but no visible changes
   * will be made to those ObjectBases until the read lock is released.</p>
   */

  public DBReadLock openReadLock() throws InterruptedException
  {
    return lockManager.openReadLock();
  }

  /**
   * <p>Establishes a write lock for the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}s
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

  public DBWriteLock openWriteLock(Vector<DBObjectBase> bases) throws InterruptedException
  {
    return lockManager.openWriteLock(bases);
  }

  /**
   * <p>This method establishes a dump lock on all object bases in this Ganymede
   * server.</p>
   */

  public DBDumpLock openDumpLock() throws InterruptedException
  {
    return lockManager.openDumpLock();
  }

  /**
   * <p>releaseLock releases a particular lock held by this session.
   * This method will not force a lock being held by another thread to
   * drop out of its establish method.. it is intended to be called by
   * the same thread that established the lock.</p>
   */

  public void releaseLock(DBLock lock)
  {
    lockManager.releaseLock(lock);
  }

  /**
   */

  private void releaseAllLocks()
  {
    lockManager.releaseAllLocks();
  }

  /**
   * <p>openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject() and
   * createDBObject() to obtain {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}s.
   * Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction() is called.</p>
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @see arlut.csd.ganymede.server.DBEditObject
   */

  public void openTransaction(String describe)
  {
    this.openTransaction(describe, true);
  }

  /**
   * <p>openTransaction establishes a transaction context for this session.
   * When this method returns, the session can call editDBObject() and
   * createDBObject() to obtain {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}s.
   * Methods can then be called
   * on the DBEditObjects to make changes to the database.  These changes
   * are actually performed when and if commitTransaction() is called.</p>
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
   * @see arlut.csd.ganymede.server.DBEditObject
   */

  public synchronized void openTransaction(String describe, boolean interactive)
  {
    if (editSet != null)
      {
        throw new IllegalArgumentException(ts.l("openTransaction.transaction"));
      }

    editSet = new DBEditSet(store, this, describe, interactive);
  }

  /**
   * <p>commitTransaction causes any changes made during the context of
   * a current transaction to be performed.  Appropriate portions of the
   * database are locked, changes are made to the in-memory image of
   * the database, and a description of the changes is placed in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore} journal file.  Event
   * logging / mail notification may take place.</p>
   *
   * <p>The session must not hold any locks when commitTransaction is
   * called.  The symmetrical invid references between related objects
   * and the atomic namespace management code should guarantee that no
   * incompatible change is made with respect to any checked out objects
   * while the Bases are unlocked.</p>
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.server.DBEditObject
   */

  public ReturnVal commitTransaction()
  {
    return commitTransaction(null);
  }

  /**
   * <p>commitTransaction causes any changes made during the context of
   * a current transaction to be performed.  Appropriate portions of the
   * database are locked, changes are made to the in-memory image of
   * the database, and a description of the changes is placed in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore} journal file.  Event
   * logging / mail notification may take place.</p>
   *
   * <p>The session must not hold any locks when commitTransaction is
   * called.  The symmetrical invid references between related objects
   * and the atomic namespace management code should guarantee that no
   * incompatible change is made with respect to any checked out objects
   * while the Bases are unlocked.</p>
   *
   * @param comment If not null, a comment to attach to logging and
   * email generated in response to this transaction.
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.server.DBEditObject
   */

  public synchronized ReturnVal commitTransaction(String comment)
  {
    ReturnVal retVal = null;

    /* -- */

    // we need to release our readlock, if we have one,
    // so that the commit can establish a writelock..

    // should i make it so that a writelock can be established
    // if the possessor of a readlock doesn't give it up?

    if (debug)
      {
        System.err.println(ts.l("commitTransaction.debug1", String.valueOf(key)));
      }

    if (editSet == null)
      {
        throw new RuntimeException(ts.l("commitTransaction.notransaction", String.valueOf(key)));
      }

    // we can't commit a transaction with locks held, because that
    // might lead to deadlock.  we release all locks now, then when we
    // call editSet.commit(), that will attempt to establish whatever
    // write locks we need, for the duration of the commit() call.

    releaseAllLocks();

    if (debug)
      {
        System.err.println(ts.l("commitTransaction.debug2", String.valueOf(key)));
      }

    String description = editSet.description; // get before commit() clears it

    retVal = editSet.commit(comment); // *synchronized*

    if (ReturnVal.didSucceed(retVal))
      {
        if (description != null)
          {
            // "{0}: committed transaction {1}"
            Ganymede.debug(ts.l("commitTransaction.debug3", String.valueOf(key), description));
          }
        else
          {
            // "{0}: committed transaction"
            Ganymede.debug(ts.l("commitTransaction.debug4", String.valueOf(key)));
          }

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

    return retVal;              // later on we'll figure out how to do this right
  }

  /**
   * <p>abortTransaction causes all {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}s
   * that were pulled during
   * the course of the session's transaction to be released without affecting
   * the state of the database.  Any changes made to
   * {@link arlut.csd.ganymede.server.DBObject DBObject}s pulled for editing
   * by this session during this transaction are abandoned.  Any objects created
   * or destroyed by this session during this transaction are abandoned / unaffected
   * by the actions during the transaction.</p>
   *
   * <p>Calling abortTransaction() has no affect on any locks held by
   * this session, but generally no locks should be held here.
   * abortTransaction() will attempt to abort a write lock being
   * established by a commitTransaction() call on another thread.</p>
   *
   * @return null if the transaction was committed successfully,
   *         a non-null ReturnVal if there was a commit failure.
   *
   * @see arlut.csd.ganymede.server.DBEditObject
   */

  public synchronized ReturnVal abortTransaction()
  {
    if (editSet == null)
      {
        throw new RuntimeException(ts.l("global.notransaction", "abortTransaction"));
      }

    if (!editSet.abort())
      {
        Ganymede.debug(ts.l("abortTransaction.cant_abort", String.valueOf(key)));

        return Ganymede.createErrorDialog(ts.l("abortTransaction.error"),
                                          ts.l("abortTransaction.error_text"));
      }
    else
      {
        editSet = null;         // for gc
      }

    return null;
  }

  /**
   * <p>Returns true if this session has an transaction open</p>
   */

  public boolean isTransactionOpen()
  {
    return (editSet != null);
  }

  /**
   * <p>This method returns true if this session is carrying out a
   * transaction on behalf of an interactive client.</p>
   */

  public boolean isInteractive()
  {
    return editSet.isInteractive();
  }

  /**
   * <p>Simple accessor to allow us to trap derefence later if we want.</p>
   */

  public DBEditSet getEditSet()
  {
    return editSet;
  }

  /**
   * <p>This method returns a handle to the objectHook for
   * a particular Invid.</p>
   */

  public DBEditObject getObjectHook(Invid invid)
  {
    DBObjectBase base;

    base = Ganymede.db.getObjectBase(invid.getType());
    return base.getObjectHook();
  }

  /**
   * <p>Gets our lock key</p>
   */

  public Object getKey()
  {
    return key;
  }

  /**
   * <p>This method is responsible for providing an identifier string
   * for the user who this session belongs to, and is used for
   * logging and what-not.</p>
   */

  public String getID()
  {
    return GSession.getPermManager().getIdentity();
  }

  /**
   * <p>This method returns a handle to the Ganymede Session
   * that owns this DBSession.</p>
   */

  public GanymedeSession getGSession()
  {
    return GSession;
  }

  /**
   * <p>Returns the label of a given Invid in this session.</p>
   */

  public String getObjectLabel(Invid invid)
  {
    try
      {
        return viewDBObject(invid).getLabel();
      }
    catch (NullPointerException ex)
      {
        return null;
      }
  }

  /**
   * This method is intended as a lightweight way of returning a
   * handy description of the type and label of the specified invid.
   * No locking is done, and the label returned will be viewed through
   * the context of the current transaction, if any.
   */

  public String describe(Invid invid)
  {
    try
      {
        DBObject obj = viewDBObject(invid);

        if (obj != null)
          {
            return obj.getTypeName() + " " + obj.getLabel();
          }
        else
          {
            DBObjectBase base = Ganymede.db.getObjectBase(invid.getType());

            return base.getName() + " " + invid.toString() + " (non-existing)";
          }
      }
    catch (NullPointerException ex)
      {
        return null;
      }
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

  //******************************************************************
  //
  // To satisfy the arlut.csd.ganymede.common.QueryDescriber interface
  //
  //******************************************************************

  /**
   * This method is intended as a lightweight way of returning a handy
   * description of the specified type.
   */

  public String describeType(short type)
  {
    try
      {
        DBObjectBase base = Ganymede.db.getObjectBase(type);

        return base.getName();
      }
    catch (Exception ex)
      {
        return String.valueOf(type);
      }
  }

  public String describeField(short objType, short fieldType)
  {
    try
      {
        DBObjectBase base = Ganymede.db.getObjectBase(objType);
        DBObjectBaseField field = base.getFieldDef(fieldType);

        return field.getName();
      }
    catch (Exception ex)
      {
        return String.valueOf(fieldType);
      }
  }

  public String describeField(String objTypeName, short fieldType)
  {
    try
      {
        DBObjectBase base = Ganymede.db.getObjectBase(objTypeName);
        DBObjectBaseField field = base.getFieldDef(fieldType);

        return field.getName();
      }
    catch (Exception ex)
      {
        return String.valueOf(fieldType);
      }
  }
}
