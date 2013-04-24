/*
   GASH 2

   DBObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.python.core.PyInteger;

import arlut.csd.Util.EmptyVector;
import arlut.csd.Util.JythonMap;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        DBObject

------------------------------------------------------------------------------*/

/**
 * <p>Class to hold a typed, read-only database object as represented
 * in the Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore}
 * database.  DBObjects can be exported via RMI for remote access by
 * remote clients. Clients directly access instances of DBObject for
 * viewing or editing in the form of a {@link
 * arlut.csd.ganymede.rmi.db_object db_object} RMI interface type passed
 * as return value in calls made on the {@link
 * arlut.csd.ganymede.rmi.Session Session} remote interface.</p>
 *
 * <p>A DBObject is identified by a unique identifier called an {@link
 * arlut.csd.ganymede.common.Invid Invid} and contains a set of {@link
 * arlut.csd.ganymede.server.DBField DBField} objects which hold the actual
 * data values held in the object.  The client typically interacts
 * with the fields held in this object directly using the {@link
 * arlut.csd.ganymede.rmi.db_field db_field} remote interface which is
 * returned by the DBObject getField methods.  DBObject is not
 * directly involved in the client's interaction with the DBFields,
 * although the DBFields will call methods on the owning DBObject to
 * consult about permissions and the like.  Clients that call the
 * GanymedeSession's {@link
 * arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
 * view_db_object()} method to view a DBObject actually interact with
 * a copy of the DBObject created by the view_db_object() method to
 * enforce appropriate read permissions.</p>
 *
 * <p>A plain DBObject is not editable;  all value-changing calls to DBFields contained
 * in a plain DBObject will reject any change requests.  In order to edit a DBObject,
 * a client must get access to a {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
 * object derived from the DBObject.  This is typically done by calling
 * {@link arlut.csd.ganymede.rmi.Session#edit_db_object(arlut.csd.ganymede.common.Invid) edit_db_object}
 * on the server's {@link arlut.csd.ganymede.rmi.Session Session} remote interface.</p>
 *
 * <p>The DBStore contains a single read-only DBObject in its database for each Invid.
 * In order to change a DBObject, that DBObject must have its
 * {@link arlut.csd.ganymede.server.DBObject#createShadow(arlut.csd.ganymede.server.DBEditSet) createShadow}
 * method called.  This is a synchronized method which attaches a new DBEditObject
 * to the DBObject.  Only one DBEditObject can be created from a single DBObject at
 * a time, and it must be created in the context of a
 * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} transaction object.  Once the DBEditObject
 * is created, that transaction has exclusive right to make changes to the DBEditObject.  When
 * the transaction is committed, a new DBObject is created from the values held in the
 * DBEditObject.  That DBObject is then placed back into the DBStore, replacing the
 * original DBObject.  If instead the transaction is aborted, the DBObject forgets about
 * the DBEditObject that had been attached to it and the DBObject is once again available
 * for other transactions to edit.</p>
 *
 * <p>Actually, the above picture is a bit too simple.  The server's DBStore object does
 * not directly contain DBObjects, but instead contains
 * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects, which define a type
 * of DBObject, and contain all DBObjects of that type in turn.  The DBObjectBase
 * is responsible for making sure that each DBObject has its own unique Invid based
 * on the DBObjectBase's type id and a unique number for the individual DBObject.</p>
 *
 * <p>In terms of type definition, the DBObjectBase object acts as a template for
 * objects of the type.  Each DBObjectBase contains a set of
 * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} objects which
 * define the names and types of DBFields that a DBObject of that type is
 * meant to store.</p>
 *
 * <p>In addition, each DBObjectBase can be linked to a custom DBEditObject subclass
 * that oversees all kinds of operations on DBObjects of this kind.  Custom
 * DBEditObject subclasses can define special logic for object creation, viewing,
 * and editing, including custom object linking logic, acceptable value constraints,
 * and even step-by-step wizard dialog sequences to oversee certain kinds of
 * operations.</p>
 *
 * <p>All DBObjects have a certain number of DBFields pre-defined, including an
 * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} listing the owner groups
 * that this DBObject belongs to, a number of {@link arlut.csd.ganymede.server.StringDBField StringDBField}s
 * that contain information about the last admin to modify this DBObject,
 * {@link arlut.csd.ganymede.server.DateDBField DateDBField}s recording the creation and
 * last modification dates of this object, and so on.  See
 * {@link arlut.csd.ganymede.common.SchemaConstants SchemaConstants} for details on the
 * built-in field types.</p>
 *
 * <p>DBObject has had its synchronization revised so that only the createShadow,
 * clearShadow, getFieldPerm, receive, and emitXML methods are sync'ed on
 * the DBObject itself.  Everything else syncs on the field table held within
 * the DBObject.  createShadow() and clearShadow() in particular must remain
 * sync'ed on the same monitor, but for most things we want to sync on the
 * interior fieldAry.</p>
 *
 * <p>Is all this clear?  Good!</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBObject implements db_object, FieldType, Remote, JythonMap {

  static boolean debug = false;
  final static boolean debugEmit = false;
  final static boolean debugReceive = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBObject");

  /**
   * Counter field we use to display loading statistics at server start time.
   */

  static public int objectCount = 0;

  // ---

  public static void setDebug(boolean val)
  {
    debug = val;
  }

  /* - */

  /**
   * The type definition for this object.
   */

  protected DBObjectBase objectBase;

  /**
   * <p>Our fields, ordered by ascending field id.</p>
   *
   * <p>This member variable will be null in the case where we are
   * constructed as a DBEditObject subclass for use as a pseudo-static
   * objectHook.</p>
   *
   * @see arlut.csd.ganymede.server.DBField
   */

  protected DBField[] fieldAry;

  /**
   * Permission cache for our fields, in ascending field id order
   * using the same indexing as fieldAry.
   */

  protected PermEntry[] permCacheAry;

  /**
   * If this object is being edited or removed, this points
   * to the DBEditObject copy that is being edited.  If
   * this object is not being edited, this field will be null,
   * and we are available for someone to edit.
   */

  DBEditObject shadowObject;

  /**
   * If this object is being viewed by a particular
   * Ganymede Session, we record that here.
   */

  protected GanymedeSession gSession;

  /**
   * If thisobject is being viewed in a particular permissions
   * context, we record the permManager here.
   */

  protected DBPermissionManager permManager;

  /**
   * A fixed copy of our Invid, so that we don't have to create
   * new ones all the time when people call getInvid() on us.
   */

  Invid myInvid = null;

  /**
   * used by the DBObjectTable logic
   */

  DBObject next = null;

  /* -- */

  /**
   * <p>No param constructor, here to allow DBEditObject to
   * super()-chain to us with a no-param constructor for the
   * pseudo-static objectHook case.</p>
   */

  public DBObject()
  {
    gSession = null;
    permManager = null;
  }

  /**
   * <p>Base constructor, used to create a new object of
   * type objectBase.  Note that DBObject itself is
   * a mere carrier of data and there is nothing application
   * type specific in a base DBObject.  The only type
   * information is represented by the DBObjectBase passed
   * in to this constructor.</p>
   *
   * <p>This constructor is used through super() chaining
   * by the DBEditObject check-out constructor.</p>
   */

  DBObject(DBObjectBase objectBase)
  {
    this.objectBase = objectBase;
    fieldAry = null;
    permCacheAry = null;

    shadowObject = null;

    myInvid = Invid.createInvid(objectBase.getTypeID(), 0);
    gSession = null;
    permManager = null;
  }

  /**
   * <p>Constructor to create an object of type objectBase
   * with the specified object number.</p>
   *
   * <p>This is used through super() chaining by the DBEditObject
   * create object constructor.</p>
   */

  DBObject(DBObjectBase objectBase, int id)
  {
    this(objectBase);
    myInvid = Invid.createInvid(objectBase.getTypeID(), id);
    gSession = null;
    permManager = null;
  }

  /**
   * <p>Read constructor.  Constructs an objectBase from a DataInput
   * stream.</p>
   */

  DBObject(DBObjectBase objectBase, DataInput in, boolean journalProcessing) throws IOException
  {
    if (objectBase == null)
      {
        throw new RuntimeException("Error, null object base");
      }

    this.objectBase = objectBase;
    shadowObject = null;
    receive(in, journalProcessing);
    gSession = null;
    permManager = null;

    DBObject.objectCount++;
  }

  /**
   * <p>This check-in constructor is used to create a non-editable
   * DBObject from a DBEditObject that we have finished editing.
   * Whenever a transaction checks a created or edited shadow back
   * into the DBStore, it actually does so by creating a new DBObject
   * to replace any previous version of the object in the DBStore.</p>
   *
   * @param eObj The shadow object to copy into the new DBObject
   *
   * @see arlut.csd.ganymede.server.DBEditSet#commit(java.lang.String)
   * @see arlut.csd.ganymede.server.DBEditSet#release()
   */

  DBObject(DBEditObject eObj)
  {
    DBField field;

    /* -- */

    objectBase = eObj.objectBase;
    myInvid = eObj.myInvid;

    shadowObject = null;

    short count = 0;

    if (eObj.fieldAry == null)
      {
        // "Error, tried to call the DBObject view-copy constructor with a pseudo-static DBEditObject"
        throw new NullPointerException(ts.l("global.pseudostatic_constructor"));
      }

    synchronized (eObj.fieldAry)
      {
        for (short i = 0; i < eObj.fieldAry.length; i++)
          {
            field = eObj.fieldAry[i];

            if (field != null && field.isDefined())
              {
                count++;
              }
          }

        // put any defined fields into the object we're going
        // to commit back into our DBStore

        fieldAry = new DBField[count];

        int j = 0;

        for (short i = 0; i < eObj.fieldAry.length; i++)
          {
            field = eObj.fieldAry[i];

            if (field != null && field.isDefined())
              {
                // clean up any cached data the field was holding during
                // editing

                try
                  {
                    field.cleanup();
                  }
                catch (Exception ex)
                  {
                    // we don't want to throw an uncaught exception in
                    // the check-in constructor, because we'll break
                    // the transaction commit in a really bad way if
                    // that happens.
                    //
                    // field cleanup should be a
                    // fail-without-consequence kind of thing, so
                    // we'll just carry on if it happens

                    ex.printStackTrace();
                  }

                // Create a new copy and save it in the new DBObject.  We
                // *must not* directly save the field from the DBEditObject,
                // because that field has likely been RMI exported to a
                // remote client, and if we keep the exported field in
                // local use, all of the extra bulk of the RMI mechanism
                // will also be retained, as the DBField's Stub and Skel
                // are associated with the field through a weak hash ref.  By
                // letting the old field from the DBEditObject get locally
                // garbage collected, we make it possible for all the RMI
                // stuff to get garbage collected as well.

                // Making a copy here rather than saving a ref to the
                // exported field makes a *huge* difference in overall
                // memory usage on the Ganymede server.

                fieldAry[j++] = field.getCopy(this); // safe since we started with an empty fieldAry
              }
          }
      }

    gSession = null;
    permManager = null;
  }

  /**
   * <p>This is a view-copy constructor, designed to make a view-only
   * duplicate of an object from the database.  This view-only object
   * knows who is looking at it through its GanymedeSession reference,
   * and so can properly enforce field access permissions.</p>
   *
   * <p>&lt;gSession&gt; may be null, in which case the returned DBObject
   * will be simply an un-linked fresh copy of &lt;original&gt;.</p>
   */

  public DBObject(DBObject original, GanymedeSession gSession)
  {
    DBField field;

    /* -- */

    objectBase = original.objectBase;
    myInvid = original.myInvid;

    shadowObject = null;

    if (original.fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic_constructor"));
      }

    synchronized (original.fieldAry)
      {
        // make fieldAry big enough to hold all fields defined, because
        // DBObjectDeltaRec uses this constructor when doing
        // journal edits to an object

        fieldAry = new DBField[original.fieldAry.length];

        for (int i = 0; i < original.fieldAry.length; i++)
          {
            fieldAry[i] = original.fieldAry[i].getCopy(this);
          }
      }

    this.gSession = gSession;

    if (gSession == null)
      {
        this.permManager = null;
      }
    else
      {
        this.permManager = gSession.getPermManager();
      }
  }

  /**
   * <p>This method makes the fields in this object remotely accessible.
   * Used by GanymedeSession when it provides a DBObject to the
   * client.</p>
   */

  public final void exportFields()
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        for (DBField field: fieldAry)
          {
            // export can fail if the object has already
            // been exported.. don't worry about it if
            // it happens.. the client will know about it
            // if we try to pass a non-exported object
            // back to it, anyway.

            Ganymede.rmi.publishObject(field);
          }
      }
  }


  /**
   * <p>This method pulls the fields in this object from remote accessibility through
   * RMI, possibly improving our security posture and reducing the memory loading
   * on the RMI system.</p>
   */

  public final void unexportFields()
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        for (DBField field: fieldAry)
          {
            // unexport can fail (return false) if the object has
            // already been unexported, or if it was never exported,
            // but we don't care as long as it's not exported after
            // this point.

            Ganymede.rmi.unpublishObject(field, true);
          }
      }
  }

  public final int hashCode()
  {
    return myInvid.getNum();
  }

  /**
   * <p>Returns the numeric id of the object in the objectBase</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final int getID()
  {
    return myInvid.getNum();
  }

  /**
   * <p>Returns the invid of this object for the db_object remote
   * interface</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Invid getInvid()
  {
    return myInvid;
  }

  /**
   * <p>Returns the numeric id of the object's objectBase</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final short getTypeID()
  {
    return objectBase.getTypeID();
  }

  /**
   * <p>Returns the name of the object's objectBase</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final String getTypeName()
  {
    return objectBase.getName();
  }

  /**
   * <p>Returns the data dictionary for this object</p>
   */

  public final DBObjectBase getBase()
  {
    return objectBase;
  }

  /**
   * <p>Returns the field definition for the given field code, or null
   * if that field code is not registered with this object type.</p>
   */

  public final DBObjectBaseField getFieldDef(String fieldName)
  {
    return (DBObjectBaseField) objectBase.getField(fieldName);
  }

  /**
   * <p>Returns the field definition for the given field code, or null
   * if that field code is not registered with this object type.</p>
   */

  public final DBObjectBaseField getFieldDef(short fieldcode)
  {
    return (DBObjectBaseField) objectBase.getField(fieldcode);
  }

  /**
   * <p>Returns the permission that apply to the given fieldName in this
   * object.</p>
   *
   * <p>If this object was not made in the context of a specific
   * GanymedeSession, full permissions will be given for access to the
   * field.</p>
   */

  public final PermEntry getFieldPerm(String fieldName)
  {
    DBField f = (DBField) getField(fieldName);

    if (f == null)
      {
        // "Can''t find permissions for non-existent field "{0}""
        throw new IllegalArgumentException(ts.l("getFieldPerm.nofield", fieldName));
      }

    return this.getFieldPerm(f.getID());
  }

  /**
   * <p>Returns the permission that apply to the field with the given
   * fieldcode in this object.</p>
   *
   * <p>If this object was not made in the context of a specific
   * GanymedeSession, full permissions will be given for access to the
   * field.</p>
   */

  public final synchronized PermEntry getFieldPerm(short fieldcode)
  {
    PermEntry result = null;

    /* -- */

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    if (permManager == null)
      {
        return PermEntry.fullPerms; // assume supergash if we have no session
      }

    short index = findField(fieldcode);

    if (index == -1)
      {
        // "Unrecognized fieldcode: {0}"
        throw new IllegalArgumentException(ts.l("getFieldPerm.nofield", Integer.valueOf(fieldcode)));
      }

    if (permCacheAry == null)
      {
        permCacheAry = new PermEntry[fieldAry.length];
      }
    else
      {
        result = permCacheAry[index];
      }

    if (result == null)
      {
        result = permManager.getPerm(this, fieldcode);

        if (result == null)
          {
            result = permManager.getPerm(this);
          }
      }

    permCacheAry[index] = result;

    return result;
  }

  /**
   * <p>Returns the GanymedeSession that this object is checked out in
   * care of.</p>
   */

  public final GanymedeSession getGSession()
  {
    return gSession;
  }

  /**
   * <p>Returns the DBSession that this object is checked out in care
   * of, or null if it is checked out from the persistent store.</p>
   *
   * @deprecated Use {@link #getDBSession()} instead.
   */

  @Deprecated
  public final DBSession getSession()
  {
    return this.getDBSession();
  }

  /**
   * <p>Returns the DBSession that this object is checked out in care
   * of, or null if it is checked out from the persistent store.</p>
   */

  public final DBSession getDBSession()
  {
    try
      {
        return gSession.getDBSession();
      }
    catch (NullPointerException ex)
      {
        return null;
      }
  }

  /**
   * <p>Provide easy server-side access to this object's name in a
   * String context for debug and non-critical output.</p>
   */

  public String toString()
  {
    return getLabel() + " (" + getInvid() + ")";
  }

  /**
   * <p>Simple equals test.. doesn't really test to see if things are
   * value-equals, but rather identity equals.</p>
   */

  public boolean equals(Object param)
  {
    if (!(param instanceof DBObject))
      {
        return false;
      }

    try
      {
        return (getInvid().equals(((DBObject) param).getInvid()));
      }
    catch (NullPointerException ex)
      {
        return false;
      }
  }

  /**
   * <p>Returns the primary label of this object.</p>
   *
   * <p>We don't synchronize getLabel(), as it is very, very
   * frequently called from all over, and we don't want to chance
   * deadlock.  getField() and getValueString() are both synchronized
   * on subcomponents of DBObject, so this method should be adequately
   * safe as written.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public String getLabel()
  {
    DBField f = (DBField) getField(objectBase.getLabelField());

    if (f != null && f.isDefined())
      {
        String result = f.getValueString();

        // the label must be unique, but if we're a newly created
        // object, we won't have any label value yet.  Go ahead and
        // synthesize one for the time being.

        if (result == null || result.length() == 0)
          {
            // "New {0}: {1,number,#}"
            result = ts.l("getLabel.null_label", getTypeName(), Integer.valueOf(getID()));
          }

        return result;
      }
    else
      {
        // we should never get here.. objects shouldn't be in the
        // database without their label field.

        // "New {0}: {1,number,#}"
        return ts.l("getLabel.null_label", getTypeName(), Integer.valueOf(getID()));
      }
  }

  /**
   * <p>If this object type is embedded, this method will return the
   * desired display label for the embedded object.</p>
   *
   * <p>This label may not be the same as returned by getLabel(),
   * which is guaranteed to be derived from a namespace constrained
   * label field, suitable for use in the XML context.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public String getEmbeddedObjectDisplayLabel()
  {
    return objectBase.getObjectHook().getEmbeddedObjectDisplayLabelHook(this);
  }

  /**
   * <p>Get access to the field that serves as this object's
   * label.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final db_field getLabelField()
  {
    return getField(objectBase.getLabelField());
  }

  /**
   * <p>Get access to the field id for the field that serves as this
   * object's label.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final short getLabelFieldID()
  {
    return objectBase.getLabelField();
  }

  /**
   * <p>Returns true if this object is an embedded type.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final boolean isEmbedded()
  {
    return objectBase.isEmbedded();
  }

  /**
   * <p>The emit() method is part of the process of dumping the DBStore
   * to disk.  emit() dumps an object in its entirety to the
   * given out stream.</p>
   *
   * @param out A {@link arlut.csd.ganymede.server.DBJournal DBJournal} or
   * {@link arlut.csd.ganymede.server.DBStore DBStore} writing stream.
   */

  final void emit(DataOutput out) throws IOException
  {
    //    System.err.println("Emitting " + objectBase.getName() + " <" + id + ">");

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    out.writeInt(getID());      // write out our object id

    synchronized (fieldAry)
      {
        short count = 0;

        for (DBField field: fieldAry)
          {
            if (field != null && field.isDefined())
              {
                count++;
              }
          }

        if (count == 0)
          {
            // "**** Error: writing object with no fields: {0}"
            Ganymede.debug(ts.l("emit.nofields",
                                objectBase.getName() + " <" + getID() + ">"));
          }

        out.writeShort(count);

        for (DBField field: fieldAry)
          {
            if (field != null && field.isDefined())
              {
                out.writeShort(field.getID());
                field.emit(out);
              }
          }
      }
  }

  /**
   * <p>The receive() method is part of the process of loading the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}
   * from disk.  receive() reads an object from the given in stream and
   * instantiates it into the DBStore.</p>
   *
   * <p>This method is synchronized, but there are a lot of other methods
   * in DBObject which are not synchronized and which could cause problems
   * if they are run concurrently with receive.  All the ones that
   * play in the fieldAry array.  This is only workable because receive
   * is not called on an object after it has been loaded into the
   * database.</p>
   */

  final synchronized void receive(DataInput in, boolean journalProcessing) throws IOException
  {
    DBField
      tmp = null;

    DBObjectBaseField
      definition;

    short
      fieldcode,
      type;

    int
      tmp_count,
      upgradeSkipCount = 0;

    /* -- */

    // get our unique id

    myInvid = Invid.createInvid(objectBase.getTypeID(), in.readInt());

    if (debugReceive)
      {
        System.err.println("Reading invid " + myInvid);
      }

    // get number of fields

    tmp_count = in.readShort();

    if (debug && tmp_count == 0)
      {
        // "DBObject.receive(): No fields reading object {0}"
        System.err.println(ts.l("receive.nofields", Integer.valueOf(getID())));
      }

    fieldAry = new DBField[tmp_count];
    permCacheAry = null;        // okay in synchronized block

    for (int i = 0; i < tmp_count; i++)
      {
        // read our field code, look it up in our
        // DBObjectBase

        fieldcode = in.readShort();

        definition = this.getFieldDef(fieldcode);

        // we used to have a couple of Invid vector fields that we
        // have gotten rid of, for the sake of improving Ganymede's
        // concurrency and reducing inter-object lock contention.  The
        // BackLinksField we got rid of a long time ago, the
        // OwnerBase's OwnerObjectsOwned field we got rid of at
        // DBStore 2.7.

        if ((fieldcode == SchemaConstants.BackLinksField) ||
            (getTypeID() == SchemaConstants.OwnerBase && fieldcode == SchemaConstants.OwnerObjectsOwned))
          {
            // the backlinks field was always a vector of invids, so
            // now that we are no longer explicitly recording
            // asymmetric relationships with the backlinks field, we
            // can just skip forward in the database file and skip the
            // backlinks info

            // Ditto, starting at DBStore version 2.7, with the
            // OwnerBase's OwnerObjectsOwned

            if (Ganymede.db.isLessThan(2,3))
              {
                upgradeSkipCount = in.readShort();
              }
            else
              {
                upgradeSkipCount = in.readInt();
              }

            int count = upgradeSkipCount; // our vector count

            while (count-- > 0)
              {
                in.readShort();
                in.readInt();
              }

            continue;
          }
        else if (definition == null)
          {
            // "What the heck?  Null definition for {0}, fieldcode = {1}, {2}th field in object"
            throw new RuntimeException(ts.l("receive.nulldef",
                                            this.getTypeName(),
                                            Integer.valueOf(fieldcode),
                                            Integer.valueOf(i)));
          }

        if (debugReceive)
          {
            System.err.println("Reading field " + definition);
          }

        tmp = DBField.readField(this, in, definition);

        if (tmp == null)
          {
            // "Don't recognize field type in datastore."
            throw new Error(ts.l("receive.badfieldtype"));
          }

        if (!journalProcessing && (definition.getNameSpace() != null))
          {
            if (tmp.isVector())
              {
                // mark the elements in the vector in the namespace
                // note that we don't use the namespace mark method here,
                // because we are just setting up the namespace, not
                // manipulating it in the context of an editset

                for (int j = 0; j < tmp.size(); j++)
                  {
                    if (definition.getNameSpace().containsKey(tmp.key(j)))
                      {
                        try
                          {
                            // "Non-unique value {0} detected in vector field {1} which is constrained by namespace {2}"
                            throw new RuntimeException(ts.l("receive.vectornamespace",
                                                            GHashtable.keyString(tmp.key(j)),
                                                            definition, definition.getNameSpace()));
                          }
                        catch (RuntimeException ex)
                          {
                            ex.printStackTrace();
                          }
                      }

                    definition.getNameSpace().receiveValue(tmp.key(j), tmp);
                  }
              }
            else
              {
                // mark the scalar value in the namespace

                if (definition.getNameSpace().containsKey(tmp.key()))
                  {
                    // "Non-unique value {0} detected in scalar field {1} which is constrained by namespace {2}"
                    try
                      {
                        throw new RuntimeException(ts.l("receive.scalarnamespace",
                                                        GHashtable.keyString(tmp.key()),
                                                        definition, definition.getNameSpace()));
                      }
                    catch (RuntimeException ex)
                      {
                        ex.printStackTrace();
                      }
                  }

                definition.getNameSpace().receiveValue(tmp.key(), tmp);
              }
          }

        // now add the field to our fields table

        if (Ganymede.db.isAtLeast(2, 15))
          {
            // starting at DBStore 2.15, we know that the fields are
            // coming from the db file ordered by ascending field code

            fieldAry[i] = tmp;
          }
        else
          {
            // we have to be more conservative when loading an older
            // db or journal block.

            if (tmp.isDefined())
              {
                saveField(tmp);
              }
            else
              {
                // "%%% Loader skipping empty field {0}"
                System.err.println(ts.l("receive.skipping", definition.getName()));
              }
          }
      }

    if (getTypeID() == SchemaConstants.OwnerBase && upgradeSkipCount != 0)
      {
        // "Skipped over {0} objects in deprecated OwnerObjectsOwned field while reading owner group {1}"
        System.err.println(ts.l("receive.upgradeSkippingOwned", Integer.valueOf(upgradeSkipCount), this.getLabel()));
      }
  }

  /**
   * <p>This method is used when this object is being dumped.</p>
   */

  public final synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent("object");
    xmlOut.attribute("type", XMLUtils.XMLEncode(getTypeName()));
    xmlOut.attribute("id", getLabel());

    if (xmlOut.isDumpingOid())
      {
        xmlOut.attribute("oid", this.getInvid().toString());
      }

    xmlOut.indentOut();

    // by using getFieldVector(), we get the fields in display
    // order

    Vector<DBField> fieldVec = getFieldVector(false);

    for (DBField field: fieldVec)
      {
        if (field.isDefined())
          {
            if (xmlOut.mayInclude(field))
              {
                field.emitXML(xmlOut);
              }
          }
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("object");
  }

  /**
   * <p>Check this object out from the datastore for editing.  This
   * method is intended to be called by the editDBObject method in
   * DBSession.. createShadow should not be called on an arbitrary
   * viewed object in other contexts.. probably should do something to
   * guarantee this?</p>
   *
   * <p>If this object is being edited, we say that it has a shadow
   * object; a session gets a copy of this object.. the copy is
   * actually a DBEditObject, which has the intelligence to allow the
   * client to modify the (copies of the) data fields.</p>
   *
   * <p>note: this is only used for editing pre-existing objects..
   * the code for creating new objects is in DBSession..  this method
   * might be better incorporated into DBSession as well.</p>
   *
   * @param editset The transaction to own this shadow.
   */

  final synchronized DBEditObject createShadow(DBEditSet editset)
  {
    if (shadowObject != null)
      {
        // this object has already been checked out
        // for editing / deleting

        return null;
      }

    shadowObject = objectBase.createNewObject(this, editset);

    // if this object currently points to an object that
    // is being deleted by way of an asymmetric InvidDBField,
    // addObject() may fail.  In this case, we have to deny
    // the edit

    if (!editset.addObject(shadowObject))
      {
        shadowObject = null;
        return null;
      }

    // update the session's checkout count first, then
    // update the database's overall checkout, which
    // will trigger a console update

    if (editset.session.GSession != null)
      {
        editset.session.GSession.checkOut(); // update session checked out count
      }

    objectBase.getStore().checkOut(); // update checked out count

    return shadowObject;
  }

  /**
   * <p>This method is the complement to createShadow, and
   * is used during editset release.</p>
   *
   * @param editset The transaction owning this object's shadow.
   *
   * @see arlut.csd.ganymede.server.DBEditSet#release()
   */

  final synchronized boolean clearShadow(DBEditSet editset)
  {
    if (editset != shadowObject.editset)
      {
        // couldn't clear the shadow..  this editSet
        // wasn't the one to create the shadow

        // "DBObject.clearShadow(): couldn't clear, editset mismatch"
        Ganymede.debug(ts.l("clearShadow.mismatch"));

        return false;
      }

    shadowObject = null;

    if (editset.session.GSession != null)
      {
        editset.session.GSession.checkIn();
      }

    objectBase.getStore().checkIn(); // update checked out count

    return true;
  }

  /**
   * <p>Get read-only Vector of DBFieldInfo objects for the custom
   * DBFields contained in this object, in display order.</p>
   *
   * <p>This method is called by the client so as to download all of
   * the field values in an object in a single remote method call.</p>
   *
   * <p>If the client does not have permission to view a field, that
   * field will be left out of the resulting Vector.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Vector<FieldInfo> getFieldInfoVector()
  {
    Vector<FieldInfo> results = new Vector<FieldInfo>();
    DBField field;

    /* -- */

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        for (DBObjectBaseField fieldDef: objectBase.getCustomFields())
          {
            field = retrieveField(fieldDef.getID());

            if (field != null)
              {
                try
                  {
                    results.add(new FieldInfo(field));
                  }
                catch (GanyPermissionsException ex)
                  {
                    // swallow the exception without comment, we'll
                    // just leave the field out of the vector
                  }
              }
          }
      }

    return results;
  }

  /**
   * <p>This method provides a Vector copy of the DBFields contained
   * in this object in a fashion that does not contribute to fieldAry
   * threadlock.</p>
   */

  public final Vector<DBField> getFieldVect()
  {
    Vector<DBField> fieldVect = new Vector<DBField>(fieldAry.length);

    synchronized (fieldAry)
      {
        for (DBField field: fieldAry)
          {
            fieldVect.add(field);
          }
      }

    return fieldVect;
  }

  /**
   * <p>This method adds a DBField to this object.  It is used when
   * loading journal entries that cause new fields to be added, and by
   * InvidDBField.bind(), when anonymously adding an InvidDBField to
   * an object that the user would not normally have needs to add a
   * field after the fact.</p>
   */

  final synchronized void addField(DBField field)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    if (field == null)
      {
        // "null value passed to addField"
        throw new IllegalArgumentException(ts.l("addField.null"));
      }

    if (retrieveField(field.getID()) != null)
      {
        // "Error, field {0} is already present in object {1}."
        throw new IllegalArgumentException(ts.l("addField.preexisting", field.getID(), this.toString()));
      }

    DBField[] newFieldAry = new DBField[fieldAry.length + 1];

    java.lang.System.arraycopy(fieldAry, 0, newFieldAry, 0, fieldAry.length);

    fieldAry = newFieldAry;

    saveField(field);
  }

  /**
   * <p>This method places a DBField into a slot in this object's
   * fieldAry DBField array.  As a (probably reckless) speed
   * optimization, this method makes no checks to ensure that another
   * DBField with the same field id has not previously been stored, so
   * it should only be used when the DBObject's fieldAry is in a known
   * state.  Otherwise, {@link
   * arlut.csd.ganymede.server.DBObject#clearField(short) clearField()}
   * should be called before calling saveField(), so that duplicate
   * field id's are not accidentally introduced into the DBObject's
   * fieldAry.</p>
   *
   * <p>saveField() saves fields in field id order to try and speed up
   * field retrieving, by allowing us to do boolean search to find
   * elements.</p>
   */

  private void saveField(DBField field)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    if (field == null)
      {
        // "null value passed to saveField"
        throw new IllegalArgumentException(ts.l("saveField.null"));
      }

    synchronized (fieldAry)
      {
        int i = 0;

        while (i < fieldAry.length)
          {
            if (fieldAry[i] == null)
              {
                fieldAry[i] = field;

                return;
              }

            if (fieldAry[i].getID() > field.getID())
              {
                break;
              }

            i++;
          }

        if (i == fieldAry.length)
          {
            throw new ArrayIndexOutOfBoundsException("saveField overran field array length");
          }

        DBField currentField = null;
        DBField bubbleField = field;

        while (i < fieldAry.length)
          {
            currentField = fieldAry[i];
            fieldAry[i] = bubbleField;
            bubbleField = currentField;

            i++;
          }
      }
  }

  /**
   * <p>This method replaces a DBField with a given field id in this
   * object's fieldAry DBField array with a new DBField sharing the
   * same id.  If this DBObject does not contain a field with the same
   * id as the field argument for this method, no action will be taken
   * and an IllegalArgumentException will be thrown.</p>
   *
   * <p>replaceField() saves fields in field id order to try and speed
   * up field retrieving, by allowing us to do boolean search to find
   * elements.</p>
   */

  final void replaceField(DBField field)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    if (field == null)
      {
        // "null value passed to replaceField"
        throw new IllegalArgumentException(ts.l("replaceField.null"));
      }

    synchronized (fieldAry)
      {
        int index = java.util.Arrays.binarySearch(fieldAry, field);

        if (index < 0)
          {
            // "Error, DBObject.replaceField could not find matching field"
            throw new IllegalArgumentException(ts.l("replaceField.none"));
          }

        fieldAry[index] = field;

        if (permCacheAry != null)
          {
            permCacheAry[index] = null;
          }
      }
  }

  /**
   * <p>This method removes a DBField that has the a field id matching
   * the argument from this object's fieldAry.  This method will never
   * fail..  if there is no field matching the given field id, the
   * method will return without changing the fieldAry.</p>
   */

  final void clearField(short id)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        int index = java.util.Arrays.binarySearch(fieldAry, id);

        if (index < 0)
          {
            // "Error, DBObject.replaceField could not find matching field"
            throw new IllegalArgumentException(ts.l("replaceField.none"));
          }

        fieldAry[index] = null;

        if (permCacheAry != null)
          {
            permCacheAry[index] = null;
          }
      }
  }

  /**
   * <p>This method retrieves a DBField from this object's fieldAry
   * DBField array.  retrieveField() uses a hashing algorithm to try
   * and speed up field retrieving, but we are optimizing for low
   * memory usage rather than O(1) operations.</p>
   */

  final DBField retrieveField(short id)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        int index = java.util.Arrays.binarySearch(fieldAry, id);

        if (index < 0)
          {
            return null;
          }

        return fieldAry[index];
      }
  }

  /**
   * <p>This method retrieves a DBField from this object's fieldAry
   * DBField array by field name.  This access is done slowly, using a
   * simple iteration over the values in our packed hash
   * structure.</p>
   */

  final DBField retrieveField(String fieldName)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        for (DBField field: fieldAry)
          {
            if (field != null && field.getName().equalsIgnoreCase(fieldName))
              {
                return field;
              }
          }
      }

    return null;
  }

  /**
   * <p>This method finds the index for the given field id in this object's
   * fieldAry and permCacheAry tables.</p>
   *
   * @return -1 if we couldn't find a field with the given id
   */

  private final short findField(short id)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        return (short) java.util.Arrays.binarySearch(fieldAry, id);
      }
  }

  /**
   * <p>This method clears any cached PermEntry value for the
   * given field id.</p>.
   */

  public final void clearFieldPerm(short id)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        int index = java.util.Arrays.binarySearch(fieldAry, id);

        if (index < 0)
          {
            return;
          }

        if (permCacheAry != null)
          {
            permCacheAry[index] = null;
          }
      }
  }

  /**
   * <p>Get access to a field from this object.  This method
   * is exported to clients over RMI.</p>
   *
   * @param id The field code for the desired field of this object.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final db_field getField(short id)
  {
    return retrieveField(id);
  }

  /**
   * <p>Get read-only access to a field from this object, by name.</p>
   *
   * @param fieldname The fieldname for the desired field of this object
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final db_field getField(String fieldname)
  {
    return retrieveField(fieldname);
  }

  /**
   * <p>Returns the name of a field from this object.</p>
   *
   * @param id The field code for the desired field of this object.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final String getFieldName(short id)
  {
    DBField field = retrieveField(id);

    if (field != null)
      {
        return field.toString();
      }

    return "<<" + id + ">>";
  }

  /**
   * <p>This method returns the short field id code for the named
   * field, if the field is present in this object, or -1 if the
   * field could not be found.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final short getFieldId(String fieldname)
  {
    DBField field;

    /* -- */

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        for (int i = 0; i < fieldAry.length; i++)
          {
            field = fieldAry[i];

            if (field != null && field.getName().equalsIgnoreCase(fieldname))
              {
                return field.getID();
              }
          }
      }

    return -1;
  }

  /**
   * <p>Get complete list of db_fields contained in this object.  The
   * list returned will appear in field id order.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final db_field[] listFields()
  {
    return (db_field[]) listDBFields();
  }

  /**
   * <p>Get complete list of DBFields contained in this object.  The
   * list returned will appear in field id order.</p>
   */

  public final DBField[] listDBFields()
  {
    DBField result[];
    short count = 0;

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        for (DBField field: fieldAry)
          {
            if (field != null)
              {
                count++;
              }
          }

        result = new DBField[count];

        count = 0;

        for (DBField field: fieldAry)
          {
            if (field != null)
              {
                result[count++] = field;
              }
          }
      }

    return result;
  }

  /**
   * <p>Returns true if inactivate() is a valid operation on
   * checked-out objects of this type.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final boolean canInactivate()
  {
    return objectBase.canInactivate();
  }

  /**
   * <p>Returns true if this object has been inactivated and is
   * pending deletion.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean isInactivated()
  {
    return (objectBase.canInactivate() &&
            (getFieldValueLocal(SchemaConstants.RemovalField) != null));
  }

  /**
   * <p>This method examines all fields in the object and verifies
   * that they satisfy the elementary value constraints specified in
   * the Ganymede schema.</p>
   *
   * <p>If any fields do not meet the field constraints, a ReturnVal will
   * be returned with a free-form dialog describing the violations.</p>
   *
   * <p>If there are no field constraint violations, null will be
   * returned.</p>
   */

  public final ReturnVal validateFieldIntegrity()
  {
    StringBuilder resultBuffer = new StringBuilder();

    /* -- */

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    // sync on fieldAry since we are looping over our fields and since
    // retrieveField itself sync's on fieldAry.  if we sync up front
    // we may reduce our lock acquisition time marginally

    synchronized (fieldAry)
      {
        // loop over the fields in display order (rather than the hash
        // order in the fieldAry)

        Vector<FieldTemplate> fieldTemplates = objectBase.getFieldTemplateVector();

        for (FieldTemplate template: fieldTemplates)
          {
            DBField field = retrieveField(template.getID());

            if (field != null && field.isDefined())
              {
                ReturnVal retVal = field.validateContents();

                if (!ReturnVal.didSucceed(retVal))
                  {
                    if (resultBuffer.length() > 0)
                      {
                        resultBuffer.append("\n");
                      }

                    resultBuffer.append(retVal.getDialogText());
                  }
              }
          }
      }

    if (resultBuffer.length() > 0)
      {
        // we're using the non-logging ReturnVal.setErrorText() rather
        // than using Ganymede.createErrorDialog() because the
        // DBField.validateContents() call itself uses
        // Ganymede.createErrorDialog().

        ReturnVal retVal = new ReturnVal(false);
        retVal.setErrorText(resultBuffer.toString());
        return retVal;
      }

    return null;
  }

  /**
   * <p>Returns true if this object has all its required fields defined</p>
   *
   * <p>This method can be overridden in DBEditObject subclasses to do a
   * more refined validity check if desired.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean isValid()
  {
    return (checkRequiredFields() == null);
  }

  /**
   * <p>This method scans through all custom fields defined in the
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} for
   * this object type and determines if all required fields have been
   * filled in.  If everything is ok, this method will return null.
   * If any required fields are found not to have been filled out,
   * this method returns a vector of field names that need to be
   * filled out.</p>
   *
   * <p>This method is used by the transaction commit logic to ensure a
   * consistent transaction. If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean) GanymedeSession.enableOversight(false)}
   * this method will not be called at transaction commit time.</p>
   */

  public final Vector<String> checkRequiredFields()
  {
    Vector<String> localFields = new Vector<String>();

    /* -- */

    // sync on fieldAry since we are looping over our fields and since retrieveField itself
    // sync's on fieldAry

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        // assume that the object type's fields will not be changed at a
        // time when this method is called.  A reasonable assumption,
        // as the objectbase field table is only altered when the
        // schema is being edited.

        for (DBObjectBaseField fieldDef: objectBase.getCustomFields())
          {
            try
              {
                // nota bene: calling fieldRequired here could
                // potentially leave us open for threadlock, depending
                // on how the fieldRequired method is written.  I
                // think this is a low-level risk, but not zero.

                if (objectBase.getObjectHook().fieldRequired(this, fieldDef.getID()))
                  {
                    DBField field = retrieveField(fieldDef.getID());

                    if (field == null || !field.isDefined())
                      {
                        localFields.add(fieldDef.getName());
                      }
                  }
              }
            catch (NullPointerException ex)
              {
                Ganymede.logError(ex, "Null pointer exception in checkRequiredFields().\n" +
                                  "My type is " + getTypeName() + "\nMy invid is " + getInvid());
              }
          }
      }

    // if all required fields checked out, return null to signify success

    if (localFields.size() == 0)
      {
        return null;
      }
    else
      {
        return localFields;
      }
  }

  /**
   * <p>Returns the date that this object is to go through final removal
   * if it has been inactivated.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Date getRemovalDate()
  {
    DateDBField dbf = (DateDBField) getField(SchemaConstants.RemovalField);

    if (dbf == null)
      {
        return null;
      }

    return dbf.value();
  }

  /**
   * <p>Returns true if this object has an expiration date set.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final boolean willExpire()
  {
    return (getFieldValueLocal(SchemaConstants.ExpirationField) != null);
  }

  /**
   * <p>Returns true if this object has a removal date set.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final boolean willBeRemoved()
  {
    return (getFieldValueLocal(SchemaConstants.RemovalField) != null);
  }

  /**
   * <p>Returns the date that this object is to be automatically
   * inactivated if it has an expiration date set.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Date getExpirationDate()
  {
    DateDBField dbf = (DateDBField) getField(SchemaConstants.ExpirationField);

    if (dbf == null)
      {
        return null;
      }

    return dbf.value();
  }

  /**
   * <p>Returns true if this object has an 'in-care-of' email address
   * that should be notified when this object is changed.</p>
   */

  public final boolean hasEmailTarget()
  {
    return objectBase.getObjectHook().hasEmailTarget(this);
  }

  /**
   * <p>Returns a vector of email addresses that can be used to send
   * 'in-care-of' email for this object.</p>
   */

  public final List<String> getEmailTargets()
  {
    return (List<String>) objectBase.getObjectHook().getEmailTargets(this);
  }

  /**
   * <p>Returns a String containing a URL that can be used by the
   * client to retrieve a picture representating this object.</p>
   *
   * <p>Intended to be used for users, primarily.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final String getImageURL()
  {
    return objectBase.getObjectHook().getImageURLForObject(this);
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final ReturnVal setFieldValue(String fieldName, Object value) throws GanyPermissionsException
  {
    DBField field = (DBField) getField(fieldName);

    if (field == null)
      {
        // "Error, object {0} does not contain a field named "{1}"."
        return Ganymede.createErrorDialog(ts.l("global.bad_field_name", this.getTypeName(), fieldName));
      }

    // NB: we would go ahead and do like we did for getFieldValue(),
    // and define a third setFieldValue() function that would take a
    // DBField to implement the common logic, but we've had a history
    // of letting folks subclass setFieldValue(short id, Object value)
    // specifically to override setFieldValue behavior.  We'll keep
    // bridging into the short-using setFieldValue() to preserve this
    // behavior.

    return this.setFieldValue(field.getID(), value);
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public ReturnVal setFieldValue(short fieldID, Object value) throws GanyPermissionsException
  {
    // we override in DBEditObject

    // "Server: Error in DBObject.setFieldValue()"
    // "setFieldValue called on a non-editable object"

    return Ganymede.createErrorDialog(ts.l("setFieldValue.noneditable"),
                                      ts.l("setFieldValue.noneditabletext"));
  }

  /**
   * <p>Shortcut method to get a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Object getFieldValue(String fieldName) throws GanyPermissionsException
  {
    return this.getFieldValue((DBField) getField(fieldName));
  }

  /**
   * <p>Shortcut method to get a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Object getFieldValue(short fieldID) throws GanyPermissionsException
  {
    return this.getFieldValue((DBField) getField(fieldID));
  }

  private Object getFieldValue(DBField f) throws GanyPermissionsException
  {
    if (f == null)
      {
        return null;
      }

    if (f.isVector())
      {
        // "Couldn't get scalar value on vector field {0}"
        throw new IllegalArgumentException(ts.l("getFieldValue.badtype", f.getName()));
      }

    return f.getValue();
  }

  /**
   * <p>Shortcut method to get a field's value.  Used only
   * on the server, as permissions are not checked.</p>
   */

  public final Object getFieldValueLocal(String fieldName)
  {
    return this.getFieldValueLocal((DBField) getField(fieldName));
  }

  /**
   * <p>Shortcut method to get a field's value.  Used only
   * on the server, as permissions are not checked.</p>
   */

  public final Object getFieldValueLocal(short fieldID)
  {
    return this.getFieldValueLocal((DBField) getField(fieldID));
  }

  private Object getFieldValueLocal(DBField f)
  {
    if (f == null)
      {
        return null;
      }

    if (f.isVector())
      {
        // "Couldn't get scalar value on vector field {0}"
        throw new IllegalArgumentException(ts.l("getFieldValue.badtype", f.getName()));
      }

    return f.getValueLocal();
  }

  /**
   * <p>This helper method is for use on the server, so that custom
   * code subclasses can call a simple method to look up an Invid and
   * get the appropriate DBObject, taking into account whether the
   * lookup is being done within a transaction or no.</p>
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
   * <p>This method will return null if the Invid provided does not
   * exist in the session or the persistent store.</p>
   *
   * @param target The Invid to retrieve.
   */

  public final DBObject lookupInvid(Invid target)
  {
    return this.lookupInvid(target, false);
  }

  /**
   * <p>This helper method is for use on the server, so that custom
   * code subclasses can call a simple method to look up an Invid and
   * get the appropriate DBObject, taking into account whether the
   * lookup is being done within a transaction or no.</p>
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
   * <p>This method will return null if the Invid provided does not
   * exist in the session or the persistent store.</p>
   *
   * @param target The Invid to retrieve.
   * @param forceOriginal If true and the lookup is being done in the
   * middle of an editing session, we'll return a reference to the
   * original read-only DBObject from the persistent datastore, rather
   * than the checked out DBEditObject version being edited in the
   * transaction.
   */

  public final DBObject lookupInvid(Invid target, boolean forceOriginal)
  {
    return lookupInvid(target, forceOriginal, null);
  }

  /**
   * <p>This helper method is for use on the server, so that custom
   * code subclasses can call a simple method to look up an Invid and
   * get the appropriate DBObject, taking into account whether the
   * lookup is being done within a transaction or no.</p>
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
   * <p>This method will return null if the Invid provided does not
   * exist in the session or the persistent store.</p>
   *
   * @param target The Invid to retrieve.
   * @param forceOriginal If true and the lookup is being done in the
   * middle of an editing session, we'll return a reference to the
   * original read-only DBObject from the persistent datastore, rather
   * than the checked out DBEditObject version being edited in the
   * transaction.
   * @param session Should be set if the invid needs to be looked up
   * in a session context even when this DBObject has not been checked
   * out for viewing in a GanymedeSession.  As in InvidDBField.emitInvidXML().
   */

  public final DBObject lookupInvid(Invid target, boolean forceOriginal, DBSession session)
  {
    if (target == null)
      {
        return null;
      }

    if (session == null)
      {
        if (this.gSession != null)
          {
            session = this.gSession.getDBSession();
          }
      }

    if (session != null)
      {
        DBObject retObj = session.viewDBObject(target);

        if (retObj == null)
          {
            return null;
          }

        if (retObj instanceof DBEditObject && forceOriginal)
          {
            return ((DBEditObject) retObj).getOriginal();
          }
        else
          {
            return retObj;
          }
      }
    else
      {
        // we're not being viewed in a session context.. go ahead and
        // look it up in the DBStore directly

        return Ganymede.db.getObject(target);
      }
  }

  /**
   * <p>This helper method is for use on the server, so that custom
   * code subclasses can call a simple method to look up an Invid and
   * get the appropriate label, taking into account whether the lookup
   * is being done within a transaction or no.</p>
   *
   * <p>This method will return null if the Invid provided does not
   * exist in the session or the persistent store.</p>
   *
   * @param target The Invid whose label we want to retrieve.
   */

  public final String lookupInvidLabel(Invid target)
  {
    return lookupInvidLabel(target, false);
  }

  /**
   * <p>This helper method is for use on the server, so that custom
   * code subclasses can call a simple method to look up an Invid and
   * get the appropriate label, taking into account whether the lookup
   * is being done within a transaction or no.</p>
   *
   * <p>This method will return null if the Invid provided does not
   * exist in the session or the persistent store.</p>
   *
   * <p>This method returns the canonical label for the Invid, rather
   * than using the possibly overridden lookupLabel() method to get
   * the label.</p>
   *
   * @param target The Invid whose label we want to retrieve.
   * @param forceOriginal If true and the lookup is being done in the
   * middle of an editing session, we'll return the label of the
   * original read-only DBObject from the persistent datastore, rather
   * than the checked out DBEditObject version being edited in the
   * transaction.
   */

  public final String lookupInvidLabel(Invid target, boolean forceOriginal)
  {
    if (target == null)
      {
        return null;
      }

    DBObject temp = lookupInvid(target, forceOriginal);

    if (temp == null)
      {
        return null;
      }

    return temp.getLabel();
  }

  /**
   * <p>For an embedded object, returns the Invid of the parent object
   * which contains contains this embedded object.</p>
   *
   * <p>Otherwise, returns null.</p>
   */

  public final Invid getParentInvid()
  {
    if (!this.isEmbedded())
      {
        return null;
      }

    return (Invid) this.getFieldValueLocal(SchemaConstants.ContainerField);
  }

  /**
   * <p>For an embedded object, returns a reference to the object
   * which contains this embedded object.</p>
   *
   * <p>Otherwise, returns null.</p>
   */

  public final DBObject getParentObj()
  {
    if (!this.isEmbedded())
      {
        return null;
      }

    return lookupInvid(getParentInvid(), false);
  }

  /**
   * <p>Returns true if this object has a field named fieldName and if
   * that object has a defined (i.e., non-empty) value set.</p>
   */

  public final boolean isDefined(String fieldName)
  {
    return this.isDefined((DBField) getField(fieldName));
  }

  /**
   * <p>Returns true if this object has a field with a field id of
   * fieldID and if that object has a defined (i.e., non-empty) value
   * set.</p>
   */

  public final boolean isDefined(short fieldID)
  {
    return this.isDefined((DBField) getField(fieldID));
  }

  /**
   * <p>Returns true if the given field is defined.</p>
   */

  public final boolean isDefined(DBField f)
  {
    return (f != null) && f.isDefined();
  }

  /**
   * <p>This method is for use on the server, so that custom code can
   * call a simple method to test to see if a boolean field is defined
   * and has a true value.</p>
   *
   * <p>An exception will be thrown if the field is not a boolean.</p>
   */

  public final boolean isSet(String fieldName)
  {
    return this.isSet((DBField) getField(fieldName));
  }

  /**
   * <p>This method is for use on the server, so that custom code can
   * call a simple method to test to see if a boolean field is defined
   * and has a true value.</p>
   *
   * <p>An exception will be thrown if the field is not a boolean.</p>
   */

  public final boolean isSet(short fieldID)
  {
    return this.isSet((DBField) getField(fieldID));
  }

  /**
   * <p>This method is for use on the server, so that custom code can
   * call a simple method to test to see if a boolean field is defined
   * and has a true value.</p>
   *
   * <p>An exception will be thrown if the field is not a boolean.</p>
   */

  private boolean isSet(DBField f)
  {
    if (f == null)
      {
        return false;
      }

    if (f.isVector())
      {
        // "Can't call isSet on a vector field."
        throw new RuntimeException(ts.l("isSet.vector"));
      }

    Boolean bool = (Boolean) f.getValueLocal();

    return (bool != null && bool.booleanValue());
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If the field is defined
   * on this object type but is undefined in this individual object,
   * an immutable empty Vector will be returned.</p>
   *
   * <p>Will never return null.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Vector getFieldValues(String fieldName) throws GanyPermissionsException
  {
    DBField field = (DBField) this.getField(fieldName);

    if (field == null)
      {
        // Okay, this field doesn't have a copy of the desired field.
        // Let's see if we can go ahead and return a synthesized
        // EmptyVector to the caller.

        DBObjectBaseField fieldDef = this.getFieldDef(fieldName);

        if (fieldDef == null)
          {
            throw new RuntimeException("No field named " + fieldName + " defined.");
          }

        if (this.permManager != null)
          {
            PermEntry perm = this.permManager.getPerm(this, fieldDef.getID());

            if (!perm.isVisible())
              {
                // "Don''t have permission to read field {0} in object {1}"
                throw new GanyPermissionsException(ts.l("global.no_read_perms", fieldName, this.getLabel()));
              }
          }

        if (!fieldDef.isArray())
          {
            // "Couldn't get vector values on scalar field {0}"
            throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
          }

        return new EmptyVector();
      }

    if (!field.isVector())
      {
        // "Couldn't get vector values on scalar field {0}"
        throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
      }

    return field.getValues();
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If the field is defined
   * on this object type but is undefined in this individual object,
   * an immutable empty Vector will be returned.</p>
   *
   * <p>Will never return null.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Vector getFieldValues(short fieldID) throws GanyPermissionsException
  {
    DBField field = (DBField) this.getField(fieldID);
    String fieldName = null;

    if (field == null)
      {
        // Okay, this field doesn't have a copy of the desired field.
        // Let's see if we can go ahead and return a synthesized
        // EmptyVector to the caller.

        DBObjectBaseField fieldDef = this.getFieldDef(fieldID);

        if (fieldDef == null)
          {
            throw new RuntimeException("No field numbered " + fieldID + " is defined.");
          }

        fieldName = fieldDef.getName();

        if (this.permManager != null)
          {
            PermEntry perm = this.permManager.getPerm(this, fieldID);

            if (!perm.isVisible())
              {
                // "Don''t have permission to read field {0} in object {1}"
                throw new GanyPermissionsException(ts.l("global.no_read_perms", fieldName, this.getLabel()));
              }
          }

        if (!fieldDef.isArray())
          {
            // "Couldn't get vector values on scalar field {0}"
            throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
          }

        return new EmptyVector();
      }

    fieldName = field.getName();

    if (!field.isVector())
      {
        // "Couldn't get vector values on scalar field {0}"
        throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
      }

    return field.getValues();
  }

  /**
   * <p>Shortcut method to set a field's value.  This is a server-side
   * method, but it can be a quick way to get a vector of
   * elements.</p>
   *
   * <p><b>Warning!</b> The Vector returned by getFieldValuesLocal()
   * is not a clone, but is direct access to the vector held in the
   * DBField.  Clone the vector you get back if you need to do
   * anything with it other than read it.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If the field is defined
   * on this object type but is undefined in this individual object,
   * an immutable empty Vector will be returned.</p>
   *
   * <p>Will never return null.</p>
   */

  public final Vector getFieldValuesLocal(String fieldName)
  {
    DBField field = (DBField) this.getField(fieldName);

    if (field == null)
      {
        DBObjectBaseField fieldDef = this.getFieldDef(fieldName);

        if (fieldDef == null)
          {
            throw new RuntimeException("No field named " + fieldName + " defined.");
          }

        if (!fieldDef.isArray())
          {
            // "Couldn't get vector values on scalar field {0}"
            throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
          }

        return new EmptyVector();
      }

    if (!field.isVector())
      {
        // "Couldn't get vector values on scalar field {0}"
        throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
      }

    return field.getValuesLocal();
  }

  /**
   * <p>Shortcut method to set a field's value.  This
   * is a server-side method, but it can be a quick
   * way to get a vector of elements.</p>
   *
   * <p><b>Warning!</b>  The Vector returned by getFieldValuesLocal()
   * is not a clone, but is direct access to the vector
   * held in the DBField.  Clone the vector you get back
   * if you need to do anything with it other than read
   * it.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If the field is defined
   * on this object type but is undefined in this individual object,
   * an immutable empty Vector will be returned.</p>
   *
   * <p>Will never return null.</p>
   */

  public final Vector getFieldValuesLocal(short fieldID)
  {
    DBField field = (DBField) this.getField(fieldID);
    String fieldName = null;

    if (field == null)
      {
        DBObjectBaseField fieldDef = this.getFieldDef(fieldID);

        if (fieldDef == null)
          {
            throw new RuntimeException("No field numbered " + fieldID + " is defined.");
          }

        fieldName = fieldDef.getName();

        if (!fieldDef.isArray())
          {
            // "Couldn't get vector values on scalar field {0}"
            throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
          }

        return new EmptyVector();
      }

    fieldName = field.getName();

    if (!field.isVector())
      {
        // "Couldn't get vector values on scalar field {0}"
        throw new IllegalArgumentException(ts.l("getFieldValues.badtype", fieldName));
      }

    return field.getValuesLocal();
  }

  /**
   * <p>Get a display-order sorted list of DBFields contained in this
   * object.</p>
   *
   * <p>This is a server-side only operation.. permissions are not
   * checked.</p>
   */

  public final Vector<DBField> getFieldVector(boolean customOnly)
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    Vector<DBField> results = new Vector<DBField>();

    synchronized (fieldAry)
      {
        // use objectBase.getCustomFields so that we're in display
        // order

        for (DBObjectBaseField fieldDef: objectBase.getCustomFields())
          {
            DBField field = retrieveField(fieldDef.getID());

            if (field != null)
              {
                results.add(field);
              }
          }

        if (!customOnly)
          {
            for (DBField field: fieldAry)
              {
                if (field != null && field.isBuiltIn())
                  {
                    results.add(field);
                  }
              }
          }
      }

    return results;
  }

  /**
   * <p>Shortcut method to retrieve a indexed value from a named
   * vector field in this object.</p>
   *
   * <p>Will throw IllegalArgumentException if called on a scalar
   * field.</p>
   *
   * <p>This method checks access permissions, and will throw
   * GanyPermissionsException on an access violation.</p>
   */

  public final Object getFieldElement(String fieldName, int index) throws GanyPermissionsException
  {
    return getFieldElement((DBField) getField(fieldName), index);
  }

  /**
   * <p>Shortcut method to retrieve a indexed value from a vector
   * field in this object.</p>
   *
   * <p>Will throw IllegalArgumentException if called on a scalar field.</p>
   *
   * <p>This method checks access permissions, and will throw
   * GanyPermissionsException on an access violation.</p>
   */

  public final Object getFieldElement(short fieldID, int index) throws GanyPermissionsException
  {
    return getFieldElement((DBField) getField(fieldID), index);
  }

  /**
   * <p>Shortcut method to retrieve a indexed value from a named
   * vector field in this object.</p>
   *
   * <p>Will throw IllegalArgumentException if called on a scalar field.</p>
   *
   * <p>This method checks access permissions, and will throw
   * GanyPermissionsException on an access violation.</p>
   */

  public final Object getFieldElement(DBField f, int index) throws GanyPermissionsException
  {
    if (f == null)
      {
        return null;
      }

    return f.getElement(index);
  }

  /**
   * <p>Shortcut method to retrieve a indexed value from a named
   * vector field in this object.</p>
   *
   * <p>Will throw IllegalArgumentException if called on a scalar
   * field.</p>
   *
   * <p>This method does not check access permissions.</p>
   */

  public final Object getFieldElementLocal(String fieldName, int index)
  {
    return getFieldElementLocal((DBField) getField(fieldName), index);
  }

  /**
   * <p>Shortcut method to retrieve a indexed value from a vector
   * field in this object.</p>
   *
   * <p>Will throw IllegalArgumentException if called on a scalar
   * field.</p>
   *
   * <p>This method does not check access permissions.</p>
   */

  public final Object getFieldElementLocal(short fieldID, int index)
  {
    return getFieldElementLocal((DBField) getField(fieldID), index);
  }

  /**
   * <p>Shortcut method to retrieve a indexed value from a vector
   * field in this object.</p>
   *
   * <p>Will throw IllegalArgumentException if called on a scalar
   * field.</p>
   *
   * <p>This method does not check access permissions.</p>
   */

  public final Object getFieldElementLocal(DBField f, int index)
  {
    if (f == null)
      {
        return null;
      }

    return f.getElementLocal(index);
  }

  /**
   * <p>This method is used to provide a hook to allow different
   * objects to generate different labels for a given object based on
   * their perspective.  This is used to sort of hackishly simulate a
   * relational-type capability for the purposes of viewing
   * context-sensitive labels of objects that are linked from Invid
   * fields in this object.</p>
   *
   * <p>This method primarily affects the results returned by {@link
   * arlut.csd.ganymede.server.InvidDBField#encodedValues()}, but it
   * can also affect the results shown by {@link
   * arlut.csd.ganymede.server.DBQueryEngine#query(arlut.csd.ganymede.common.Query,
   * arlut.csd.ganymede.server.DBEditObject)} when the
   * perspectiveObject parameter is non-null.</p>
   *
   * <p>See the automounter map and NFS volume DBEditObject
   * subclasses for how this is to be used, if you have
   * them.</p>
   */

  public String lookupLabel(DBObject object)
  {
    return object.getLabel();   // default
  }

  /**
   * <p>This method is used to correct this object's base pointers
   * when the base changes.  This happens when the schema is
   * edited.. this method is called on all objects under a {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} to make the object
   * point to the new version of the DBObjectBase.  This method also
   * takes care of cleaning out any fields that have become undefined
   * due to a change in the schema for the field, as in a change from
   * a vector to a scalar field, or vice-versa.</p>
   */

  final void updateBaseRefs(DBObjectBase newBase)
  {
    this.objectBase = newBase;

    short count = 0;

    // we need to be double-synchronized because we are looping
    // over fieldAry and because we are replacing fieldAry in midstream

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    DBField oldAry[] = fieldAry;

    synchronized (oldAry)
      {
        for (DBField field: fieldAry)
          {
            if (field == null)
              {
                continue;
              }

            if (newBase.getField(field.getID()) != null && field.isDefined())
              {
                count++;
              }
          }

        DBField tmpFieldAry[] = new DBField[count];

        // we sync on the new field ary before we update the fieldAry ref so
        // that we can preemptively block other threads from messing
        // with fieldAry until we get it set the way we want

        synchronized (tmpFieldAry)
          {
            fieldAry = tmpFieldAry;

            permCacheAry = null;        // okay in synchronized block

            for (DBField field: oldAry)
              {
                if (field == null)
                  {
                    continue;
                  }

                if (newBase.getField(field.getID()) != null && field.isDefined())
                  {
                    saveField(field); // safe since we start with an empty fieldAry
                  }
              }
          }
      }
  }

  /**
   * <p>This method is a convenience for server-side code.  If
   * this object is an embedded object, this method will
   * return the label of the containing object.  If this
   * object is not embedded, or the containing object's
   * label cannot be determined, null will be returned.</p>
   */

  public String getContainingLabel()
  {
    if (!isEmbedded())
      {
        return null;
      }

    InvidDBField field = (InvidDBField) getField(SchemaConstants.ContainerField);

    if (field == null)
      {
        return null;
      }

    return field.getValueString();
  }

  /**
   * <p>This method returns a Vector of Invids for objects that are
   * pointed to from this object by way of non-symmetric links.  These
   * are Invids that may need to be marked as non-deletable if this
   * object is checked out by a DBEditSet.</p>
   */

  public final Set<Invid> getASymmetricTargets()
  {
    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    HashSet<Invid> results = new HashSet<Invid>();

    synchronized (fieldAry)
      {
        for (DBField field: fieldAry)
          {
            if (field == null)
              {
                continue;
              }

            if (field instanceof InvidDBField)
              {
                InvidDBField invField = (InvidDBField) field;

                if (!invField.isDefined() || invField.getFieldDef().isSymmetric())
                  {
                    continue;
                  }

                if (invField.isVector())
                  {
                    results.addAll((Vector<Invid>) invField.getValuesLocal());
                  }
                else
                  {
                    results.add((Invid) invField.getValueLocal());
                  }
              }
          }
      }

    return results;
  }

  /**
   * <p>This method returns a Vector of Invids that point to this
   * object via forward asymmetric link fields.</p>
   */

  public final Vector<Invid> getBackLinks()
  {
    return new Vector(Ganymede.db.aSymLinkTracker.getForwardLinkSources(getDBSession(), getInvid()));
  }

  /**
   * <p>This method is called to register all asymmetric pointers in
   * this object with the DBStore's aSymLinkTracker hash
   * structure.</p>
   *
   * <p>Typically this will be done when an object is first loaded
   * from the database, at a time when the DBStore aSymLinkTracker
   * hash structure has no entries for this object at all.</p>
   *
   * <p>During the commit process of a normal transaction, the {@link
   * arlut.csd.ganymede.server.DBLinkTracker#commit(arlut.csd.ganymede.server.DBSession)
   * commit()} method in the {@link
   * arlut.csd.ganymede.server.DBLinkTracker DBLinkTracker} class handles
   * these updates instead.</p>
   */

  final void registerAsymmetricLinks()
  {
    Ganymede.db.aSymLinkTracker.registerObject(null, getASymmetricTargets(), getInvid());
  }

  /**
   * <p>This method is called to unregister all asymmetric pointers in
   * this object from the DBStore's aSymLinkTracker hash structure.</p>
   *
   * <p>Typically this will be done when an object is being deleted from
   * the database in response to a journal entry, or if the object is
   * being replaced with an updated version from the journal.</p>
   *
   * <p>During the commit process of a normal transaction, the {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} class handles
   * these updates instead.</p>
   */

  final void unregisterAsymmetricLinks()
  {
    Ganymede.db.aSymLinkTracker.unregisterObject(null, getASymmetricTargets(), getInvid());
  }

  /**
   * <p>Generate a complete printed representation of the object,
   * suitable for printing to a debug or log stream.</p>
   */

  public final void print(PrintStream out)
  {
    out.print(getPrintString());
  }

  /**
   * <p>Generate a complete printed representation of the object,
   * suitable for printing to a debug or log stream.</p>
   */

  public final void print(PrintWriter out)
  {
    out.print(getPrintString());
  }

  /**
   * <p>This server-side method returns a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.</p>
   *
   * <p>This method calls
   * {@link arlut.csd.ganymede.server.DBObject#appendObjectInfo(java.lang.StringBuffer,
   * java.lang.String, boolean) appendObjectInfo} to do most of its work.</p>
   */

  public String getPrintString()
  {
    StringBuffer result = new StringBuffer();

    this.appendObjectInfo(result, null, true);

    return result.toString();
  }

  /**
   * <p>This method is used to provide a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.  This method is remotely callable by the client,
   * and so will only reveal fields that the user has permission
   * to view.  This method returns a StringBuffer to work around
   * problems with serializing large strings in early versions of the
   * JDK.</p>
   *
   * <p>This method calls
   * {@link arlut.csd.ganymede.server.DBObject#appendObjectInfo(java.lang.StringBuffer,
   * java.lang.String, boolean) appendObjectInfo} to do most of its work.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public StringBuffer getSummaryDescription()
  {
    StringBuffer result = new StringBuffer();

    if (permManager != null && !permManager.getPerm(this).isVisible())
      {
        return result;
      }

    this.appendObjectInfo(result, null, false);

    return result;
  }

  /**
   * <p>This method is used to concatenate a textual description of this
   * object to the passed-in StringBuffer.  This description is relatively
   * free-form, and is intended to be used for human consumption and not for
   * programmatic operations.</p>
   *
   * <p>This method is called by
   * {@link arlut.csd.ganymede.server.DBObject#getSummaryDescription() getSummaryDescription}.</p>
   *
   * @param buffer The StringBuffer to append this object's description to
   * @param prefix Used for recursive calls on embedded objects, this prefix will
   * be inserted at the beginning of each line of text concatenated to buffer
   * by this method.
   * @param local If false, read permissions will be checked for each field before
   * adding it to the buffer.
   */

  private void appendObjectInfo(StringBuffer buffer, String prefix, boolean local)
  {
    for (DBObjectBaseField fieldDef: objectBase.getCustomFields())
      {
        DBField field = retrieveField(fieldDef.getID());

        if (field != null && field.isDefined() && (local || field.isVisible()))
          {
            if (!field.isEditInPlace())
              {
                if (prefix != null)
                  {
                    buffer.append(prefix);
                  }

                buffer.append(field.getName());
                buffer.append(" : ");
                buffer.append(field.getValueString());
                buffer.append("\n");
              }
            else
              {
                InvidDBField invField = (InvidDBField) field;

                for (int j = 0; j < invField.size(); j++)
                  {
                    if (prefix != null)
                      {
                        buffer.append(prefix);
                      }

                    buffer.append(field.getName());
                    buffer.append("[");
                    buffer.append(j);
                    buffer.append("]");
                    buffer.append("\n");

                    Invid x = invField.value(j);

                    DBObject remObj = null;

                    if (gSession != null)
                      {
                        // if this object has been checked out for
                        // viewing by a session, we'll use
                        // view_db_object() so that we don't
                        // reveal fields that should not be seen.

                        try
                          {
                            ReturnVal retVal = gSession.view_db_object(x);
                            remObj = (DBObject) retVal.getObject();
                          }
                        catch (NotLoggedInException ex)
                          {
                          }
                      }

                    if (remObj == null)
                      {
                        // we use DBStore's static viewDBObject
                        // method so that we can call this even
                        // before the GanymedeServer object is
                        // initialized

                        remObj = DBStore.viewDBObject(x);
                      }

                    if (remObj instanceof DBEditObject)
                      {
                        DBEditObject eO = (DBEditObject) remObj;

                        if (eO.getStatus() == ObjectStatus.DELETING)
                          {
                            remObj = eO.getOriginal();
                          }
                      }

                    if (prefix != null)
                      {
                        remObj.appendObjectInfo(buffer, prefix + "\t", local);
                      }
                    else
                      {
                        remObj.appendObjectInfo(buffer, "\t", local);
                      }
                  }
              }
          }
      }

    if (fieldAry == null)
      {
        throw new NullPointerException(ts.l("global.pseudostatic"));
      }

    synchronized (fieldAry)
      {
        // okay, got all the custom fields.. now we need to summarize all the
        // built-in fields that were not listed in customFields.

        for (DBField field: fieldAry)
          {
            if (field == null || !field.isBuiltIn() || !field.isDefined())
              {
                continue;
              }

            if (local || field.isVisible())
              {
                if (prefix != null)
                  {
                    buffer.append(prefix);
                  }

                buffer.append(field.getName());
                buffer.append(" : ");
                buffer.append(field.getValueString());
                buffer.append("\n");
              }
          }
      }
  }

  /**************************************************************************
   *
   * The following methods are for Jython/Map support
   *
   * For this object, the Map interface allows for indexing based on either
   * the name or the numeric ID of a DBField. Indexing by numeric id, however,
   * is only supported for "direct" access to the Map; the numeric id numbers
   * won't appear in the list of keys for the Map.
   *
   * EXAMPLE:
   * MyDBObject.get("field_x") will return the DBField with the label
   * of "field_x".
   */

  /**
   * Part of the JythonMap interface.
   */

  public boolean has_key(Object key)
  {
    return (retrieveField((String) key) != null);
  }

  /**
   * Part of the JythonMap interface.
   */

  public List items()
  {
    List<Object[]> list = new ArrayList<Object[]>();
    Object[] tuple;

    for (DBField field: getFieldVect())
      {
        tuple = new Object[2];
        tuple[0] = field.getName();
        tuple[1] = field;
        list.add(tuple);
      }

    return list;
  }

  /**
   * Part of the JythonMap interface.
   */

  public Set keys()
  {
    Set<String> keys = new HashSet<String>();

    for (DBField field: getFieldVect())
      {
        keys.add(field.getName());
      }

    return keys;
  }

  /**
   * Part of the JythonMap interface.
   */

  public boolean containsKey(Object key)
  {
    return has_key(key);
  }

  /**
   * <p>This method only returns true if a DBField is passed in which
   * is contained in this object, by object identity.</p>
   *
   * <p>Part of the JythonMap interface.</p>
   */

  public boolean containsValue(Object value)
  {
    return getFieldVect().contains(value);
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public Set entrySet()
  {
    Set<Entry> entrySet = new HashSet<Entry>();

    for (DBField field: getFieldVect())
      {
        entrySet.add(new Entry(field));
      }

    return entrySet;
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public Object get(Object key)
  {
    if (key instanceof PyInteger)
      {
        PyInteger pi = (PyInteger) key;
        return (DBField) getField(Integer.valueOf(pi.getValue()).shortValue());
      }
    else if (key instanceof Integer)
      {
        return (DBField) getField(((Integer) key).shortValue());
      }
    else if (key instanceof Short)
      {
        return (DBField) getField(((Short) key).shortValue());
      }
    else if (key instanceof String)
      {
        return (DBField) getField((String) key);
      }
    return null;
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public boolean isEmpty()
  {
    return getFieldVect().isEmpty();
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public Set keySet()
  {
    return keys();
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public int size()
  {
    return getFieldVect().size();
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public Collection values()
  {
    return getFieldVect();
  }

  /**
   * <p>This is an embedded inner class within the
   * arlut.csd.ganymede.server.DBObject class.  It is used in the
   * context of the Jython/Map support that Deepak added to
   * DBObject.</p>
   *
   * <p>Part of the JythonMap interface.</p>
   */

  static class Entry implements Map.Entry
  {
    Object key, value;

    public Entry( DBField obj )
    {
      key = obj.getName();
      value = obj;
    }

    public Object getKey()
    {
      return key;
    }

    public Object getValue()
    {
      return value;
    }

    public Object setValue(Object value)
    {
      return null;
    }
  }

  /*
   * These methods are are no-ops since we don't want this object
   * messed with via the Map interface.
   */

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public Object put(Object key, Object value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public void putAll(Map t)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Part of the JythonMap interface.</p>
   */

  public Object remove(Object key)
  {
    throw new UnsupportedOperationException();
  }
}
