/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBEditOBject

------------------------------------------------------------------------------*/

/**
 * <p>DBEditObject is the main base class that is subclassed by individual
 * application object types to provide editing and management intelligence.
 * Both static and instance methods are defined in DBEditObject which can
 * be subclassed to provide object management intelligence.</p>
 *
 * <p>A instance of DBEditObject is a copy of a DBObject that has been
 * exclusively checked out from the main database so that a
 * {@link arlut.csd.ganymede.server.DBSession DBSession}
 * can edit the fields of the object.  The DBEditObject class keeps
 * track of the changes made to fields, keeping things properly
 * synchronized with unique field name spaces.</p>
 *
 * <p>Generally, DBEditObjects are obtained in the context of a
 * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} transaction object.  When
 * the DBEditSet is committed, a new {@link arlut.csd.ganymede.server.DBObject DBObject}
 * is created from the contents of the DBEditObject and is made to replace the
 * original object in the DBStore.  If the EditSet is aborted, the
 * DBEditObject is dropped.</p>
 *
 * <p>There is one case, however, in which a DBEditObject will be
 * present in the server outside of a DBEditSet context, and that is
 * the DBEditObject instance used for the {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}'s {@link
 * arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook}
 * customization object.  In this case, a DBEditObject of the
 * appropriate subclass is created using the {@link
 * arlut.csd.ganymede.server.DBEditObject#DBEditObject(arlut.csd.ganymede.server.DBObjectBase)
 * first constructor variant}.  A wide variety of methods in the
 * server will make method calls on the DBObjectBase objectHook to
 * allow a custom DBEditObject subclass to customize the server's
 * behavior.  Such methods are labeled <b>*PSEUDOSTATIC*</b>, which
 * means that those methods are designed not to examine or report on
 * the internal state of the objectHook, but rather are meant to
 * operate based only on parameters passed into the method.  These
 * methods are PSEUDOSTATIC rather than static because if they were
 * true static methods, every place in the server where such methods
 * are called would have to use the relatively cumbersome Java
 * Reflection API rather than being able to call methods on a
 * instance of the appropriate DBEditObject subclass.</p>
 *
 * <p>See the <a href="../../../../../customization/index.html"
 * target="_top">DBEditObject subclassing guide</a> for more
 * information generally on DBEditObject customization.</p>
 *
 * <p><b>IMPORTANT PROGRAMMING NOTE!</b>: It is critical that
 * synchronized methods in DBEditObject and in subclasses thereof do not
 * call synchronized methods in DBSession, as there is a strong possibility
 * of nested monitor deadlocking.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBEditObject extends DBObject implements ObjectStatus {

  static boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBEditObject");

  public final static int FIRSTOP = 0;
  public final static int SETVAL = 1;
  public final static int SETELEMENT = 2;
  public final static int ADDELEMENT = 3;
  public final static int DELELEMENT = 4;
  public final static int ADDELEMENTS = 5;
  public final static int DELELEMENTS = 6;
  public final static int SETPASSPLAIN = 7;
  public final static int SETPASSCRYPT = 8;
  public final static int SETPASSMD5 = 9;
  public final static int SETPASSWINHASHES = 10;
  public final static int SETPASSAPACHEMD5 = 11;
  public final static int SETPASSSSHA = 12;
  public final static int SETPASS_SHAUNIXCRYPT = 13;
  public final static int SETPASS_BCRYPT = 14;
  public final static int LASTOP = 14;

  public final static Date minDate = new Date(Long.MIN_VALUE);
  public final static Date maxDate = new Date(Long.MAX_VALUE);

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  /* --------------------- Instance fields and methods --------------------- */

  /**
   * Unless this DBEditObject represents a newly created Ganymede
   * object, we'll have a reference to the original DBObject which is
   * currently registered in the DBStore.  Only one DBEditObject can
   * be connected to a DBObject at a time, giving us object-level
   * locking.
   */

  protected DBObject original;

  /**
   * true if this object has had its commitPhase1() method called, but
   * has not yet had its commitPhase2() or release() methods called.
   * If commitSemaphore is set to true, the DBField.isEditable()
   * method will always return false for fields in this object, and no
   * editing will be allowed on this object.
   */

  private booleanSemaphore commitSemaphore = new booleanSemaphore(false);

  /**
   * true if the object is in the middle of carrying out deletion
   * logic.. consulted by subclasses by way of the {@link
   * arlut.csd.ganymede.server.DBEditObject#isDeleting()} method to
   * determine whether they should object to fields being set to null
   */

  private boolean deleting = false;

  /**
   * tracks this object's editing status.  See
   * {@link arlut.csd.ganymede.common.ObjectStatus ObjectStatus}.
   */

  private byte status;

  /**
   * true if the object has a version currently
   * stored in the DBStore
   */

  final boolean stored;

  /**
   * transaction that this object has been checked out in
   * care of.
   */

  public DBEditSet editset;

  /* -- */

  /**
   * <p>Dummy constructor, is responsible for creating a DBEditObject
   * strictly for the purpose of having a handle to call our
   * pseudostatic customization methods on.</p>
   *
   * <p>This is the version of the constructor that the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}'s {@link
   * arlut.csd.ganymede.server.DBObjectBase#createHook() createHook()}
   * method uses to create the {@link
   * arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook}
   * object.</p>
   */

  public DBEditObject(DBObjectBase base)
  {
    super(base);

    this.stored = false;
    this.editset = null;             // this will be our cue to our static handle status for our methods
  }

  /**
   * <p>Creation constructor, is responsible for creating a new
   * editable object with all fields listed in the {@link
   * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
   * instantiated but undefined.</p>
   *
   * <p>This constructor is not really intended to be called via
   * super() from subclass constructors.  Creation time field value
   * initialization is to be handled by initializeNewObject().</p>
   *
   * @see arlut.csd.ganymede.server.DBField
   */

  public DBEditObject(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);

    this.original = null;
    this.editset = editset;
    commitSemaphore.set(false);
    this.stored = false;
    this.status = CREATING;
  }

  /**
   * <p>Check-out constructor, used by {@link
   * arlut.csd.ganymede.server.DBObject#createShadow(arlut.csd.ganymede.server.DBEditSet)
   * DBObject.createShadow()} to pull out an object for editing.</p>
   */

  public DBEditObject(DBObject original, DBEditSet editset)
  {
    super(original.objectBase, original.getID(), editset.getDBSession().getGSession());

    /* -- */

    this.editset = editset;
    this.commitSemaphore.set(false);
    this.stored = true;
    this.status = EDITING;
    this.original = original;

    int i = 0;
    DBField newFields[] = new DBField[objectBase.getFieldCount()];

    for (DBObjectBaseField fieldDef: objectBase.getFieldsInFieldOrder())
      {
        short id = fieldDef.getID();
        DBField originalField = original.retrieveField(id);
        DBField copyField;

        if (originalField != null)
          {
            copyField = DBField.copyField(this, originalField);
          }
        else
          {
            copyField = DBField.createTypedField(this, fieldDef);
          }

        newFields[i++] = copyField;
      }

    this.setAllFields(newFields);
  }

  /**
   * <p>Returns the transaction object owning this object, or
   * null if an unowned data object.</p>
   *
   * <p>Note that this is public, but not made available
   * to the client via a remote interface.</p>
   */

  public final DBEditSet getEditSet()
  {
    return editset;
  }

  /**
   * Returns the original version of the object that we were created
   * to edit.  If we are a newly created object, this method will
   * return null.
   */

  @Override public final DBObject getOriginal()
  {
    return original;
  }

  /**
   * Returns a code indicating whether this object
   * is being created, edited, or deleted.
   *
   * @see arlut.csd.ganymede.common.ObjectStatus#CREATING
   * @see arlut.csd.ganymede.common.ObjectStatus#EDITING
   * @see arlut.csd.ganymede.common.ObjectStatus#DELETING
   * @see arlut.csd.ganymede.common.ObjectStatus#DROPPING
   */

  public final byte getStatus()
  {
    return status;
  }

  /**
   * This method returns true if this DBEditObject is in the middle
   * of handling clean up during object deletion.
   */

  public final boolean isDeleting()
  {
    return deleting;
  }

  /**
   * We'll use DBObject's identity-based equals
   */

  @Override public boolean equals(Object param)
  {
    return super.equals(param);
  }

  /**
   * Returns the primary label of this object.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  @Override public final String getLabel()
  {
    if (getStatus() == DELETING)
      {
        return getOriginal().getLabel();
      }

    return super.getLabel();
  }

  /**
   * <p>Returns the primary label of this remote object.</p>
   *
   * <p>For convenience in custom DBEditObject subclasses.</p>
   */

  public final String getLabel(Invid invid)
  {
    return getDBSession().getObjectLabel(invid);
  }

  /**
   * <p>If this object type is embedded, this method will return the
   * desired display label for the embedded object.</p>
   *
   * <p>This label may not be the same as returned by getLabel(), which
   * is guaranteed to be derived from a namespace constrained label
   * field, suitable for use in the XML context.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  @Override public final String getEmbeddedObjectDisplayLabel()
  {
    if (getStatus() == DELETING)
      {
        return getOriginal().getEmbeddedObjectDisplayLabel();
      }

    return super.getEmbeddedObjectDisplayLabel();
  }

  /**
   * <p>If this DBEditObject is managing an embedded object, the
   * getEmbeddedObjectLabel() can be overridden to display a synthetic
   * label in the context of viewing or editing the containing object,
   * and when doing queries on the containing type.</p>
   *
   * <p>The getLabel() method will not consult this hook, however, and
   * embedded objects will be represented with their unique label
   * field when processed in an XML context.</p>
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public String getEmbeddedObjectDisplayLabelHook(DBObject object)
  {
    return object.getLabel();
  }

  /**
   * <p>This method is used to make sure that the built-in fields that
   * the server assumes will always be present in any editable object
   * will be in place.</p>
   *
   * <p>This method checks with instantiateNewField() if the field id is
   * not one of those that is needfull.  If instantiateNewField() approves
   * the creation of a new field, checkNewField() will check to see if
   * the {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * permissions permit the field creation.</p>
   */

  public final boolean checkNewField(short fieldID)
  {
    if (fieldID <= SchemaConstants.FinalSystemField)
      {
        return true;            // we always allow the built in fields
      }

    return instantiateNewField(fieldID);
  }

  /**
   * Sets this object's status code
   *
   * @see arlut.csd.ganymede.common.ObjectStatus#CREATING
   * @see arlut.csd.ganymede.common.ObjectStatus#EDITING
   * @see arlut.csd.ganymede.common.ObjectStatus#DELETING
   * @see arlut.csd.ganymede.common.ObjectStatus#DROPPING
   */

  final void setStatus(byte new_status)
  {
    switch (new_status)
      {
      case CREATING:
      case EDITING:
      case DELETING:
      case DROPPING:
        status = new_status;
        break;

      default:
        throw new RuntimeException("unrecognized status code");
      }
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method can save the client a roundtrip to the server.</p>
   *
   * <p>This method cannot be used on permission fields or password
   * fields.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  @Override public final ReturnVal setFieldValue(short fieldID, Object value) throws GanyPermissionsException
  {
    DBField field = retrieveField(fieldID);

    /* -- */

    if (field != null)
      {
        return field.setValue(value);
      }

    // "DBEditObject.setFieldValue() error"
    // "DBEditObject.setFieldValue() couldn''t find field {0} in object {1}"

    return Ganymede.createErrorDialog(this.getGSession(),
                                      ts.l("setFieldValue.error"),
                                      ts.l("setFieldValue.errorTxt", Short.valueOf(fieldID), getLabel()));
  }

  /**
   * <p>Shortcut method to set a field's value.  This version bypasses
   * permission checking and is only intended for server-side
   * use.</p>
   *
   * <p>This method cannot be used on permission fields or password
   * fields.</p>
   */

  public final ReturnVal setFieldValueLocal(short fieldID, Object value)
  {
    DBField field = retrieveField(fieldID);

    /* -- */

    if (field != null)
      {
        return field.setValueLocal(value);
      }

    // "DBEditObject.setFieldValueLocal() error"
    // "DBEditObject.setFieldValueLocal() couldn''t find field {0} in object {1}"

    return Ganymede.createErrorDialog(this.getGSession(),
                                      ts.l("setFieldValueLocal.error"),
                                      ts.l("setFieldValueLocal.errorTxt", Short.valueOf(fieldID), getLabel()));
  }

  /**
   * Returns true if the object has ever been stored in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore} under the
   * current invid.
   */

  public final boolean isStored()
  {
    return stored;
  }

  /* -------------------- pseudo-static Customization hooks --------------------


     The following block of methods are intended to be used in static fashion..
     that is, a DBObjectBase can load in a class that extends DBEditObjectBase
     and hold an instance of such as DBObjectBase.objectHook.  The following
     methods are used in a static fashion, that is they are intended
     to perform actions on designated external DBObjects rather than on the
     'this' per-DBObjectBase objectHook instance, which may not be fully defined.

  */

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>This version of anonymousLinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link based on what field of what object wants to link
   * to it.</p>
   *
   * <p>By default, the 3 variants of the DBEditObject
   * anonymousLinkOK() method are chained together, so that the
   * customizer can choose which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link
   * arlut.csd.ganymede.server.InvidDBField#bind(arlut.csd.ganymede.common.Invid,arlut.csd.ganymede.common.Invid,boolean)
   * bind()} method calls this version.  This version calls the three
   * parameter version, which calls the two parameter version, which
   * returns false by default.  Customizers can implement any of the
   * three versions, but unless you maintain the version chaining
   * yourself, there's no point to implementing more than one of
   * them.</p>
   *
   * <p>Note that the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of
   * anonymousLinkOK() with a null targetObject, to determine that the
   * client should not use its cache for an InvidDBField's choices.
   * Any overriding done of this method must be able to handle a null
   * targetObject, or else an exception will be thrown
   * inappropriately.</p>
   *
   * <p>The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   * @param sourceObject The object on the other side of the proposed link
   * @param sourceFieldID  The field on the other side of the proposed link
   * @param gsession Who is trying to do this linking?
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
                                 DBObject sourceObject, short sourceFieldID,
                                 GanymedeSession gsession)
  {
    // by default, dispatch to the more generic approval method

    return anonymousLinkOK(targetObject, targetFieldID, gsession);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>This version of anonymousUnlinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link to be rescinded based on what field of what
   * object wants to unlink from it.</p>
   *
   * <p>By default, the 3 variants of the DBEditObject anonymousUnlinkOK()
   * method are chained together, so that the customizer can choose
   * which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.server.InvidDBField#unbind(arlut.csd.ganymede.common.Invid,boolean) unbind()}
   * method calls this version.  This version calls the three parameter
   * version, which calls the two parameter version, which returns
   * true by default.  Customizers can implement any of the three
   * versions, but unless you maintain the version chaining yourself,
   * there's no point to implementing more than one of them.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is removed from
   * @param targetFieldID The field that the link is removed from
   * @param sourceObject The object on the other side of the existing link
   * @param sourceFieldID The object on the other side of the existing link
   * @param gsession Who is trying to do this unlinking?
   */

  public boolean anonymousUnlinkOK(DBObject targetObject, short targetFieldID,
                                   DBObject sourceObject, short sourceFieldID,
                                   GanymedeSession gsession)
  {
    // by default, dispatch to the more generic approval method

    return anonymousUnlinkOK(targetObject, targetFieldID, gsession);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>See {@link arlut.csd.ganymede.server.DBEditObject#anonymousLinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousLinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousLinkOK() method chaining.</p>
   *
   * <p>Note that the {@link arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.</p>
   *
   * <p>The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   * @param gsession Who is trying to do this linking?  */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID, GanymedeSession gsession)
  {
    return anonymousLinkOK(targetObject, targetFieldID);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>By default, the server always allows anonymous unlinking.
   * Overriding this method is only required when you want to DISallow
   * such unlinking.</p>
   *
   * <p>See {@link arlut.csd.ganymede.server.DBEditObject#anonymousUnlinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousUnlinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousUnlinkOK() method chaining.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   * @param gsession Who is trying to do this unlinking?
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID, GanymedeSession gsession)
  {
    return anonymousUnlinkOK(object, fieldID);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>See {@link arlut.csd.ganymede.server.DBEditObject#anonymousLinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousLinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousLinkOK() method chaining.</p>
   *
   * <p>Note that the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.</p>
   *
   * <p>The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID)
  {
    return false;               // by default, permission is denied
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>By default, the server always allows anonymous unlinking.
   * Overriding this method is only required when you want to DISallow
   * such unlinking.</p>
   *
   * <p>See {@link
   * arlut.csd.ganymede.server.DBEditObject#anonymousUnlinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousUnlinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousUnlinkOK() method chaining.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID)
  {
    return true;                // by default, permission is granted to unlink
  }

  /**
   * <p>Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given object, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.</p>
   *
   * <p>Note as well that this permOverride() has no effect when
   * creating new objects of this type. Take a look at overriding
   * {@link arlut.csd.ganymede.server.DBEditObject#canCreate(arlut.csd.ganymede.rmi.Session) canCreate()}
   * if you need to provide an exception to the normal permissions
   * system for creating new objects.</p>
   *
   * <p>This method should be used very sparingly.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given object.</p>
   *
   * <p>Note as well that this permExpand() has no effect when
   * creating new objects of this type. Take a look at overriding
   * {@link arlut.csd.ganymede.server.DBEditObject#canCreate(arlut.csd.ganymede.rmi.Session) canCreate()}
   * if you need to provide an exception to the normal permissions
   * system for creating new objects.</p>
   *
   * <p>This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant the
   * permissions specified by this method for access to the given
   * field, and no further elaboration of the permission will be
   * performed.  If permOverride() returns a non-null value for a
   * given field, permExpand() will not be consulted for that field.
   * Just as with permExpand(), this method can never cause greater
   * permissions to be granted to a field than is available to the
   * object as a whole, and this override capability does not
   * apply to operations performed in supergash mode.</p>
   *
   * <p>This method should be used very sparingly.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   * <p>Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant the
   * union of the permissions returned by this method and those
   * determined normally by GanymedeSession's default field
   * permissions logic.  This method can never cause greater
   * permissions to be granted to a field than is available to the
   * object as a whole, and the results of permExpand() will have
   * no effect on operations performed in supergash mode.</p>
   *
   * <p>This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   * <p>Customization method to verify overall consistency of a
   * DBObject.  This method is intended to be overridden in
   * DBEditObject subclasses, and will be called by {@link
   * arlut.csd.ganymede.server.DBEditObject#commitPhase1()
   * commitPhase1()} to verify the readiness of this object for
   * commit.  The DBObject passed to this method will be a
   * DBEditObject, complete with that object's GanymedeSession
   * reference if this method is called during transaction commit, and
   * that session reference may be used by the verifying code if the
   * code needs to access the database.</p>
   *
   * <p>This method is for custom checks specific to custom
   * DBEditObject subclasses.  Standard checking for missing fields
   * for which fieldRequired() returns true is done by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_checkObjectMissingFields(arlut.csd.ganymede.server.DBEditObject)}
   * during {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_handlePhase1()}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal consistencyCheck(DBObject object)
  {
    return null;
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p>Note as well that the designated label field for objects are
   * always required, whatever this method returns, and that this
   * requirement holds without regard to the GanymedeSession's
   * enableOversight value.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    return false;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to view a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for viewing to the client.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canRead(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to edit a given object.  The client's
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for editing by the client.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canWrite(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   * <p>Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.</p>
   *
   * <p>Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.</p>
   *
   * <p>If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // by default, return the field definition's visibility

    if (field.getFieldDef().base() != this.objectBase)
      {
        // "field/object mismatch"
        throw new IllegalArgumentException(ts.l("canSeeField.mismatch"));
      }

    return field.getFieldDef().isVisible();
  }

  /**
   * <p>Controls whether change information for the field can be safely
   * logged to the Ganymede log and emailed to the owner notification
   * list for the field's containing object.</p>
   *
   * <p>If okToLogField() returns false for a field, the change to the
   * field will be mentioned in email and the log, but the actual
   * values will not be.</p>
   *
   * <p>With the exception of PasswordDBFields, you should consider any
   * field for which okToLogField() returns true to be public data, as
   * any user able to log into Ganymede may be able to see the data in
   * the logs.  PasswordDBFields contain special logic to prevent the
   * data they contain from being revealed in logs or email.<p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean okToLogField(DBField field)
  {
    return true;
  }

  /**
   * <p>Customization method to verify whether this object type has an inactivation
   * mechanism.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canBeInactivated()
  {
    return false;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to inactivate a given object.  The client's
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.</p>
   *
   * <p>Note that unlike canRemove(), canInactivate() takes a
   * DBEditObject instead of a DBObject.  This is because inactivating
   * an object is based on editing the object, and so we have the
   * GanymedeSession/DBSession classes go ahead and check the object
   * out for editing before calling us.  This serves to force the
   * session classes to check for write permission before attempting
   * inactivation.</p>
   *
   * <p>Use canBeInactivated() to test for the presence of an inactivation
   * protocol outside of an edit context if needed.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return false;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to remove a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for removal by the client.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal canRemove(DBSession session, DBObject object)
  {
    // our default behavior is that objects that can be inactivated
    // should not be deleted except by supergash

    if (this.canBeInactivated() && !session.getGSession().getPermManager().isSuperGash())
      {
        return ReturnVal.failure();
      }

    return null;                // success
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to clone a given object.  The client's {@link
   * arlut.csd.ganymede.server.GanymedeSession} object will call this
   * per-class method to do an object type- sensitive check to see if
   * this object feels like being available for cloning by the
   * client.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canClone(DBSession session, DBObject object)
  {
    return !object.isEmbedded();
  }

  /**
   * <p>Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canCloneField(DBSession session, DBObject object, DBField field)
  {
    // don't clone the built-in fields, but do clone
    // anything else

    return field.getID() > SchemaConstants.FinalSystemField;
  }

  /**
   * <p>Customization method to verify whether the user has permission
   * to create an instance of this object type.  The client's
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object
   * will call the canCreate method in the
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} for this object type
   * to determine whether creation is allowed to the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canCreate(Session session)
  {
    if (session != null && (session instanceof GanymedeSession))
      {
        GanymedeSession gSession;

        /* -- */

        gSession = (GanymedeSession) session;

        return gSession.getPermManager().getPerm(getTypeID(), true).isCreatable(); // *sync* GanymedeSession
      }

    // note that we are going ahead and returning false here, as
    // we assume that the client will always use the local BaseDump
    // copy and won't generally call us remotely with a remote
    // interface.

    return false;
  }

  /**
   * <p>Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    return false;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * indicate that the given object is interested in receiving notification
   * when changes involving it occur, and can provide one or more addresses for
   * such notification to go to.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean hasEmailTarget(DBObject object)
  {
    return false;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * return a List of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.</p>
   *
   * <p>If no email targets are present in this object, either a null value
   * or an empty List may be returned.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public List<String> getEmailTargets(DBObject object)
  {
    return null;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject
   * subclasses to return a String containing a URL for an image to
   * represent this object.  Intended to be used for users, primarily.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public String getImageURLForObject(DBObject object)
  {
    return null;                // no image
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject
   * subclasses to react to forthcoming object expiration, during the
   * execution of the GanymedeWarningTask.</p>
   *
   * <p>This method will be called without benefit of an open DBEditSet,
   * so any email generated will need to make use of the
   * non-transactional mail methods in the Ganymede.log object.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return true if the DBEditObject subclass wishes to completely
   * handle the warning, or false if the default warning transmisssion
   * logic should also be sent.
   */

  public boolean reactToExpirationWarning(DBObject object, int days)
  {
    return false;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject
   * subclasses to react to forthcoming object removal.</p>
   *
   * <p>This method will be called without benefit of an open DBEditSet,
   * so any email generated will need to make use of the
   * non-transactional mail methods in the Ganymede.log object.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return true if the DBEditObject subclass wishes to completely
   * handle the warning, or false if the default warning transmisssion
   * logic should also be sent.
   */

  public boolean reactToRemovalWarning(DBObject object, int days)
  {
    return false;
  }

  /**
   * <p>This method provides a hook that can be used by subclasses of
   * DBEditObject to return a list of attribute names and attribute
   * values to include when writing out &lt;invid&gt; elements to a
   * sync channel during transactional sync'ing.</p>
   *
   * <p>The point of this is to allow DBEditObject subclasses to
   * inject additional data into a transactional sync record so that
   * external sync channel service code can have enough information to
   * identify a relationship that was made or broken within the
   * tranaction.</p>
   *
   * <p>The array returned should have an even number of values.  The
   * first value should be an attribute name, the second should be
   * the value for that attribute name, the third should be another
   * attribute name, and etc.</p>
   *
   * <p>It is an error to return an attribute name that conflicts with
   * the set pre-defined for use with the &lt;invid&gt; XML element.
   * This includes <code>type</code>, <code>num</code>, <code>id</code>,
   * and <code>oid</code>.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param invid The Invid that is to be written out.
   * @param sourceObj The object that contains the Invid to be written out.
   * @param targetObj The object that the invid resolves to.
   * @param syncChannel The name of the Sync Channel that this Invid is being written to.
   * @param forceOriginal If true and the foreign sync keys are being
   * generated in a transactional context (as in the incremental Sync
   * Channel writes), getForeignSyncKeys() will attempt to resolve the
   * extra attributes against the original version of the object targeted
   * by invid.
   */

  public String[] getForeignSyncKeys(Invid invid, DBObject sourceObj, DBObject targetObj,
                                     String syncChannel, boolean forceOriginal)
  {
    return null;
  }

  /**
   * <p>This method provides a hook that can be used by subclasses of
   * DBEditObject to return a list of attribute names and attribute
   * values to include when writing out &lt;invid&gt; elements that
   * point to them during a sync channel operation.  This method
   * differs from {@link
   * arlut.csd.ganymede.server.DBEditObject#getForeignSyncKeys(arlut.csd.ganymede.common.Invid,
   * arlut.csd.ganymede.server.DBObject,
   * arlut.csd.ganymede.server.DBObject, java.lang.String, boolean)}
   * in that this method adds extra attributes to &lt;invid&gt;
   * elements pointing to objects of this kind, where
   * getForeignSyncKeys() adds extra attributes to &lt;invid&gt;
   * elements that point from objects of this kind.</p>
   *
   * <p>The point of this is to allow DBEditObject subclasses to
   * inject additional data into a transactional sync record so that
   * external sync channel service code can have enough information to
   * identify a relationship that was made or broken within the
   * transaction.  For instance, if you are synchronizing to an LDAP
   * structure with the XML Sync Channel mechanism, you might want all
   * of your &lt;invid&gt; elements that point to Users to include a
   * dn attribute that provides the fully qualified LDAP DN for the
   * User object.</p>
   *
   * <p>The array returned should have an even number of values.  The
   * first value should be an attribute name, the second should be
   * the value for that attribute name, the third should be another
   * attribute name, and etc.</p>
   *
   * <p>It is an error to return an attribute name that conflicts with
   * the set pre-defined for use with the &lt;invid&gt; XML element.
   * This includes <code>type</code>, <code>num</code>, <code>id</code>,
   * and <code>oid</code>.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param myObj The object that is being targetted by an &lt;invid&gt; element.
   * @param syncChannel The name of the Sync Channel that the &lt;invid&gt; is being written to.
   * @param forceOriginal If true and the foreign sync keys are being
   * generated in a transactional context (as in the incremental Sync
   * Channel writes), getMySyncKeys() will attempt to resolve the
   * extra attributes against the original version of any objects resolved during
   * generation of the extra attributes.
   */

  public String[] getMyExtraInvidAttributes(DBObject myObj, String syncChannel, boolean forceOriginal)
  {
    return null;
  }


  /* -------------------- editing/creating Customization hooks --------------------


     The following block of methods are intended to be subclassed to
     provide intelligence for the object creation/editing process.

  */


  /**
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} have
   * been instantiated without defined values.  If this DBEditObject
   * is an embedded type, it will have been linked into its parent
   * object before this method is called.</p>
   *
   * <p>This method is responsible for filling in any default values
   * that can be calculated from the {@link
   * arlut.csd.ganymede.server.DBSession DBSession} associated with
   * the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.server.DBSession#createDBObject(short,
   * arlut.csd.ganymede.common.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure
   * code, the calling method will rollback the transaction.  This
   * method has no responsibility for undoing partial initialization,
   * the checkpoint/rollback logic will take care of that.</p>
   *
   * <p>If enableOversight is false, DBSession.createDBObject() will
   * not checkpoint the transaction status prior to calling
   * initializeNewObject(), so it is the responsibility of this method
   * to handle any checkpointing needed.</p>
   *
   * <p>This method should be overridden in subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal initializeNewObject()
  {
    return null;
  }

  /**
   * <p>This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.</p>
   *
   * <p>This method does not affect those fields which are actually present
   * in a previously existing object's record in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.  What this method allows
   * you to do is have a subclass decide whether it wants to instantiate
   * a potential field (one that is declared in the field dictionary for
   * this object, but which doesn't happen to be presently defined in
   * this object) in this particular object.</p>
   *
   * <p>A concrete example will help here.  The Permissions Object type
   * (base number SchemaConstants.PermBase) holds a permission
   * matrix, a descriptive title, and a list of admin personae that hold
   * those permissions for objects they own.</p>
   *
   * <p>There are a few specific instances of SchemaConstants.PermBase
   * that don't properly need the list of admin personae, as their
   * object invids are hard-coded into the Ganymede security system, and
   * their permission matrices are automatically consulted in certain
   * situations.  In order to support this, we're going to want to have
   * a DBEditObject subclass for managing permission objects.  In that
   * subclass, we'll define instantiateNewField() so that it will return
   * false if the fieldID corresponds to the admin personae list if the
   * object's ID is that of one of these special objects.  As a result,
   * when the objects are viewed by an administrator, the admin personae
   * list will not be seen.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public boolean instantiateNewField(short fieldID)
  {
    return getGSession().getPermManager().getPerm(getTypeID(), fieldID, true).isCreatable(); // *sync* GanymedeSession
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the
   * work.</p>
   *
   * <p>This method is called on a newly created object, in order to
   * clone the state of origObj into it.  This method does not
   * actually create a new object.. that is handled by {@link
   * arlut.csd.ganymede.server.GanymedeSession#clone_db_object(arlut.csd.ganymede.common.Invid)
   * clone_db_object()} before this method is called on the newly
   * created object.</p>
   *
   * <p>The default (DBEditObject) implementation of this method will
   * only clone fields for which {@link
   * arlut.csd.ganymede.server.DBEditObject#canCloneField(arlut.csd.ganymede.server.DBSession,
   * arlut.csd.ganymede.server.DBObject,
   * arlut.csd.ganymede.server.DBField) canCloneField()} returns true,
   * and which are not connected to a namespace (and thus could not
   * possibly be cloned, because the values are constrained to be
   * unique and non-duplicated).</p>
   *
   * <p>If one or more fields in the original object are unreadable by
   * the cloning session, we will provide a list of fields that could
   * not be cloned due to a lack of read permissions in a dialog in
   * the ReturnVal.  Such a problem will not result in a failure code
   * being returned, however.. the clone will succeed, but an
   * informative dialog will be provided to the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses, but
   * this method's default logic will probably do what you need it to
   * do.  If you need to make changes, try to chain your subclassed
   * method to this one via super.cloneFromObject().</p>
   *
   * @param session The DBSession that the new object is to be created in
   * @param origObj The object we are cloning
   * @param local If true, fields that have choice lists will not be checked against
   * those choice lists and read permissions for each field will not be consulted.
   * The canCloneField() method will still be consulted, however.
   *
   * @return A standard ReturnVal status object.  May be null on success, or
   * else may carry a dialog with information on problems and a success flag.
   */

  public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    ReturnVal retVal;
    Vector<DBField> origFields;
    DBField newField;
    boolean problem = false;
    StringBuilder resultBuf = new StringBuilder();

    /* -- */

    if ((origObj.getTypeID() != getTypeID()))
      {
        // "Clone Error"
        // "Can''t clone an object of the wrong type.  This is an internal error."
        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("cloneFromObject.error"),
                                          ts.l("cloneFromObject.typeError"));
      }

    origFields = origObj.getFieldVector(true); // don't clone system fields

    for (DBField origField: origFields)
      {
        if (canCloneField(session, origObj, origField))
          {
            newField = retrieveField(origField.getID());

            // if we already initialized this field when we were
            // constructed, don't copy over a value onto this field.
            // this is to allow initializeNewObject() to handle
            // object-unique values

            if (newField.isDefined())
              {
                continue;
              }

            // if the field is guarded by a unique value namespace, don't
            // attempt to clone anything to it

            if (newField.getNameSpace() != null)
              {
                continue;
              }

            // if the field is an invid editinplace field, don't try
            // to clone the actual invid pointers to contained
            // objects.

            if (newField.isEditInPlace())
              {
                continue;
              }

            // if the field is a password field, don't try to clone it

            if (newField instanceof PasswordDBField)
              {
                continue;
              }

            // if the field is an invid field whose target is a scalar
            // invid field, then this field has an association which
            // is a one-to-one or many-to-one, and shouldn't be
            // cloned, lest we disrupt the previous association

            if (newField instanceof InvidDBField)
              {
                InvidDBField iField = (InvidDBField) newField;

                DBObjectBaseField targetDef = iField.getTargetFieldDef();

                if (targetDef != null && !targetDef.isArray())
                  {
                    continue;
                  }
              }

            // and do the thing

            // copyFieldTo() checks read permissions on the
            // original object's field, and will return an error
            // dialog if the user doesn't have permission to read
            // the field.  If we have a problem, we'll return a
            // dialog describing the fields that could not be
            // cloned, but we won't fail the operation.

            retVal = origField.copyFieldTo(newField, local);

            if (retVal != null && retVal.getDialog() != null)
              {
                if (resultBuf.length() != 0)
                  {
                    resultBuf.append("\n\n");
                  }

                resultBuf.append(retVal.getDialog().getText());

                problem = true;
              }
          }
      }

    // we're returning a successful result if we've gotten here, but
    // 'normal operations' might be false if some fields could not be
    // cloned due to read permissions.

    retVal = new ReturnVal(true, !problem);

    if (problem)
      {
        // "Possible Clone Problems"
        retVal.setDialog(new JDialogBuff(ts.l("cloneFromObject.possibleError"),
                                         resultBuf.toString(),
                                         ts.l("global.buttonOK"), null, "ok.gif"));
      }

    return retVal;
  }

  /**
   * <p>This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.server.GanymediatorWizard wizards} when a field's
   * value is being changed.</p>
   *
   * <p>Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.</p>
   *
   * <p>In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.</p>
   *
   * <p>If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean)
   * enableOversight(false)},
   * this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.</p>
   *
   * <p>This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.</p>
   *
   * <p>This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.</p>
   *
   * <p>The operation parameter will be a small integer, and should hold one of the
   * following values:</p>
   *
   * <dl>
   * <dt>1 - SETVAL</dt>
   * <dd>This operation is used whenever a simple scalar field is having
   * it's value set.  param1 will be the value being placed into the field.</dd>
   * <dt>2 - SETELEMENT</dt>
   * <dd>This operation is used whenever a value in a vector field is being
   * set.  param1 will be an Integer holding the element index, and
   * param2 will be the value being set.</dd>
   * <dt>3 - ADDELEMENT</dt>
   * <dd>This operation is used whenever a value is being added to the
   * end of a vector field.  param1 will be the value being added.</dd>
   * <dt>4 - DELELEMENT</dt>
   * <dd>This operation is used whenever a value in a vector field is being
   * deleted.  param1 will be an Integer holding the element index.</dd>
   * <dt>5 - ADDELEMENTS</dt>
   * <dd>This operation is used whenever a set of elements is being
   * added to a vector field en masse.  param1 will be a Vector containing
   * the values that are being added.</dd>
   * <dt>6 - DELELEMENTS</dt>
   * <dd>This operation is used whenever a set of elements is being
   * deleted from a vector field en masse.  param1 will be a Vector containing
   * the values that are being deleted.</dd>
   * <dt>7 - SETPASSPLAIN</dt>
   * <dd>This operation is used when a password field is having its password
   * set using a plaintext source.  param1 will be a String containing the
   * submitted password, or null if the password is being cleared.</dd>
   * <dt>8 - SETPASSCRYPT</dt>
   * <dd>This operation is used when a password field is having its password
   * set using a UNIX crypt() hashed source.  param1 will be a String containing the
   * submitted hashed password, or null if the password is being cleared.</dd>
   * <dt>9 - SETPASSMD5</dt>
   * <dd>This operation is used when a password field is having its password
   * set using an md5Ccrypt() hashed source.  param1 will be a String containing the
   * submitted hashed password, or null if the password is being cleared.</dd>
   * <dt>10 - SETPASSWINHASHES</dt>
   * <dd>This operation is used when a password field is having its password
   * set using Windows style password hashes.  param1 will be the password in
   * LANMAN hash form, param2 will be the password in NT Unicode MD4 hash
   * form.  Either or both of param1 and param2 may be null.</dd>
   * <dt>11 - SETPASSAPACHEMD5</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the Apache variant of the md5crypt algorithm.
   * param1 will be the password in Apache md5crypt hash form, or null
   * if the password hash is being cleared.  param2 will be null.</dd>
   * <dt>12 - SETPASSSSHA</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenLDAP-style SSHA password hash.  param1
   * will be the password in SSHA form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * <dt>13 - SETPASS_SHAUNIXCRYPT</dt>
   * <dd>This operation is used when a password field is having its
   * password set using Ulrich Drepper's SHA256 or SHA512 Unix Crypt
   * algorithms.  param1 will be the password in SHA Unix Crypt form,
   * or null if the password is being cleared.  param2 will be
   * null.</dd>
   * <dt>14 - SETPASS_BCRYPT</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenBSD-style BCrypt password hash.  param1
   * will be the password in BCrypt form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * </dl>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @return null if the operation is approved without comment, or a
   * ReturnVal object indicating success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing
   * flag that will indicate to the field code whether or not the
   * operation should continue to completion using the field's
   * standard logic.  <b>It is very important that wizardHook return a
   * new ReturnVal(true, true) if the wizardHook wishes to simply
   * specify rescan information while having the field perform its
   * standard operation.</b> wizardHook() may return new
   * ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true
   * for the respond() method in GanymediatorWizard subclasses.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    return null;                // by default, we just ok whatever
  }

  /**
   * <p>Hook to have this object create a new embedded object
   * in the given field.</p>
   *
   * <p>This method now has the appropriate default logic for creating
   * embedded objects with the user's permissions, but this method may
   * still be overridden to do customization, if needed.</p>
   *
   * <p>XXX: It is essential that createNewEmbeddedObject() use
   * GanymedeSession.create_db_object() in order to check permissions
   * and to export the created embedded object for the client's use, if
   * necessary, whatever other customizations a subclass might choose to
   * do.  We may want to make this method final.</p>
   *
   * @return A ReturnVal indicating success or failure.  If the
   * embedded object was created successfully, the getInvid() method
   * on the returned ReturnVal will provide the Invid of the created
   * object.
   */

  public ReturnVal createNewEmbeddedObject(InvidDBField field) throws NotLoggedInException
  {
    if (getGSession() == null)
      {
        // "Error in custom code on server.  createNewEmbeddedObject() called without a valid GanymedeSession"
        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("global.serverError"),
                                          ts.l("createNewEmbeddedObject.badSession"));
      }

    DBObjectBaseField fieldDef = field.getFieldDef();

    if (!fieldDef.isEditInPlace())
      {
        // "Error in server, DBEditObject.createNewEmbeddedObject() called on non-embedded object"
        throw new RuntimeException(ts.l("createNewEmbeddedObject.embeddedError"));
      }

    // we can't create an embedded object unless we know what type to
    // create

    if (fieldDef.getTargetBase() == -1)
      {
        // "Error in custom code on server.  createNewEmbeddedObject() called without a valid target"
        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("global.serverError"),
                                          ts.l("createNewEmbeddedObject.badTarget"));
      }

    // we use GanymedeSession to check permissions to create the target.

    ReturnVal retVal = getGSession().create_db_object(fieldDef.getTargetBase(), true);

    if (retVal == null)
      {
        // "DBEditObject.createNewEmbeddedObject could not get a useful result from create_db_object"
        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("global.serverError"),
                                          ts.l("createNewEmbeddedObject.badCreate"));
      }

    return retVal;
  }

  /**
   * <p>Provides a hook that can be used to approve, disapprove,
   * and/or transform any values to be set in any field in this
   * object.</p>
   *
   * <p>verifyNewValue can be used to canonicalize a
   * submitted value.  The verifyNewValue method may call
   * {@link arlut.csd.ganymede.common.ReturnVal#setTransformedValueObject(java.lang.Object, arlut.csd.ganymede.common.Invid, short) setTransformedValue()}
   * on the ReturnVal returned in order to substitute a new value for
   * the provided value prior to any other processing on the server.</p>
   *
   * <p>This method is called before any NameSpace checking is done, before the
   * {@link arlut.csd.ganymede.server.DBEditObject#wizardHook(arlut.csd.ganymede.server.DBField,int,java.lang.Object,java.lang.Object) wizardHook()}
   * method, and before the appropriate
   * {@link arlut.csd.ganymede.server.DBEditObject#finalizeSetValue(arlut.csd.ganymede.server.DBField, Object) finalizeSetValue()},
   * {@link arlut.csd.ganymede.server.DBEditObject#finalizeSetElement(arlut.csd.ganymede.server.DBField, int, Object) finalizeSetElement()},
   * {@link arlut.csd.ganymede.server.DBEditObject#finalizeAddElement(arlut.csd.ganymede.server.DBField, java.lang.Object) finalizeAddElement()},
   * or {@link arlut.csd.ganymede.server.DBEditObject#finalizeAddElements(arlut.csd.ganymede.server.DBField, java.util.Vector) finalizeAddElements()}
   * method is called.</p>
   *
   * @param field The DBField contained within this object whose value
   * is being changed
   * @param value The value that is being proposed to go into field.
   *
   * @return A ReturnVal indicating success or failure.  May be simply
   * 'null' to indicate success if no feedback need be provided.  If
   * {@link arlut.csd.ganymede.common.ReturnVal#hasTransformedValue() hasTransformedValue()}
   * returns true when callled on the returned ReturnVal, the value
   * returned by {@link arlut.csd.ganymede.common.ReturnVal#getTransformedValueObject() getTransformedValueObject()}
   * will be used for all further processing in the server, and will
   * be the value actually saved in the DBStore.
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    return null;
  }

  // ****
  //
  // The following methods are here to allow our DBEditObject
  // to be involved in the processing of a particular
  // vector operation on a field in this object.. otherwise
  // we'd have to subclass our fields for any processing
  // that would need to be done in response to an operation..
  //
  // ****

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the {@link arlut.csd.ganymede.server.DBField DBField}
   * that called us is guaranteed to proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is requesting
   * approval for item deletion, and the &lt;index&gt; parameter identifies
   * the element number that is to be deleted.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any vector-vector removal operation, and to take any special
   * actions in reaction to the removal.. if this method returns null
   * or a success code in its ReturnVal, the DBField that called us is
   * guaranteed to proceed to make the change to its vector.  If this
   * method returns a non-success code in its ReturnVal, the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the
   * &lt;valuesToDelete&gt; parameter contains a list of elements to
   * be deleted.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under standard circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal finalizeDeleteElements(DBField field, Vector valuesToDelete)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any vector add operation, and to take any special actions in
   * reaction to the add.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us is guaranteed
   * to proceed to make the change to its vector.  If this method
   * returns a non-success code in its ReturnVal, the DBField that
   * called us will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the &lt;value&gt;
   * parameter carries the value to be added.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any vector-vector add operation, and to take any special
   * actions in reaction to the add.. if this method returns null or a
   * success code in its ReturnVal, the DBField that called us is
   * guaranteed to proceed to make the change to its vector.  If this
   * method returns a non-success code in its ReturnVal, the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the &lt;submittedValues&gt;
   * parameter carries the values to be added.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal finalizeAddElements(DBField field, Vector submittedValues)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any vector set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us is guaranteed
   * to proceed to make the change to its vector.  If this method
   * returns a non-success code in its ReturnVal, the DBField that
   * called us will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, the &lt;index&gt;
   * parameter identifies which element in the given vector field is
   * to be set, and the &lt;value&gt; parameter carries the proposed
   * new value.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal finalizeSetElement(DBField field, int index, Object value)
  {
    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions in
   * reaction to the set.  When a scalar field has its value set, it
   * will call its owners finalizeSetValue() method, passing itself as
   * the &lt;field&gt; parameter, and passing the new value to be
   * approved as the &lt;value&gt; parameter.  A Ganymede customizer
   * who creates custom subclasses of the DBEditObject class can
   * override the finalizeSetValue() method and write his own logic
   * to examine any change and either approve or reject the change.</p>
   *
   * <p>A custom finalizeSetValue() method will typically need to
   * examine the field parameter to see which field is being changed,
   * and then do the appropriate checking based on the value
   * parameter.  The finalizeSetValue() method can call the normal
   * this.getFieldValueLocal() type calls to examine the current state
   * of the object, if such information is necessary to make
   * appropriate decisions.</p>
   *
   * <p>If finalizeSetValue() returns null or a ReturnVal object with
   * a positive success value, the DBField that called us is
   * guaranteed to proceed to make the change to its value.  If this
   * method returns a non-success code in its ReturnVal, as with the
   * result of a call to Ganymede.createErrorDialog(), the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.  Any error dialog returned from finalizeSetValue()
   * will be passed to the user.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including a call to our own
   * verifyNewValue() method before calling this method.  Under normal
   * circumstances, we won't need to do anything here.
   * finalizeSetValue() is useful when you need to do unusually
   * involved checks, and for when you want a chance to trigger other
   * changes in response to a particular field's value being
   * changed.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    return null;
  }

  /**
   * <p>This method returns true if field1 should not show any choices
   * that are currently selected in field2, where both field1 and
   * field2 are fields in this object.</p>
   *
   * <p>The purpose of this method is to allow mutual exclusion between
   * a pair of fields with mandatory choices.</p>
   *
   * To be overridden on necessity in DBEditObject subclasses.
   */

  public boolean excludeSelected(db_field field1, db_field field2)
  {
    return false;
  }

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>The default logic in this method is designed to cause the client
   * to cache choice lists for invid fields in the 'all objects of
   * invid target type' cache bucket.  If your InvidDBField needs to
   * provide a restricted subset of objects of the targeted type as
   * the choice list, you'll need to override this method to either
   * return null (to turn off choice list caching), or generate some
   * kind of unique key that won't collide with the Short objects used
   * to represent the default object list caches.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)}
   * hook, which controls whether or not the default logic will
   * encourage the client to cache a given InvidDBField's choice list.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  public Object obtainChoicesKey(DBField field)
  {
    // by default, we return a Short containing the base
    // id for the field's target

    if ((field instanceof InvidDBField) &&
        !field.isEditInPlace())
      {
        DBObjectBaseField fieldDef;
        short baseId;

        /* -- */

        fieldDef = field.getFieldDef();

        baseId = fieldDef.getTargetBase();

        if (baseId < 0)
          {
            return null;
          }

        return Short.valueOf(baseId);
      }

    return null;
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields, filtered by the GanymedeSession's
   * visibilityFilterInvids list.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  public QueryResult obtainChoiceList(String fieldName) throws NotLoggedInException
  {
    return obtainChoiceList(getField(fieldName));
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields, filtered by the GanymedeSession's
   * visibilityFilterInvids list.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  public QueryResult obtainChoiceList(short fieldID) throws NotLoggedInException
  {
    return obtainChoiceList(getField(fieldID));
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields, filtered by the GanymedeSession's
   * visibilityFilterInvids list.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field instanceof InvidDBField &&
        !field.isEditInPlace() && field.isEditable())
      {
        DBObjectBaseField fieldDef;
        short baseId;

        /* -- */

        fieldDef = field.getFieldDef();

        baseId = fieldDef.getTargetBase();

        if (baseId < 0)
          {
            //      Ganymede.debug("DBEditObject: Returning null 2 for choiceList for field: " + field.getName());
            return null;
          }

        // and we want to return a list of choices.. can use the regular
        // query output here

        QueryNode root;

        // if we are pointing to objects of our own type, we don't want ourselves to be
        // a valid choice by default.. (DBEditObject subclasses can override this, of course)

        // XXX
        //
        // note: if we are omitting ourself from a choice list, we
        // sure shouldn't be cached by the client.  Right now, nothing
        // ensures this.
        //
        // XXX

        if (baseId == getTypeID())
          {
            root = new QueryNotNode(new QueryDataNode((short) -2, QueryDataNode.EQUALS, getInvid()));
          }
        else
          {
            root = null;
          }

        boolean editOnly = !choiceListHasExceptions(field);

        Query myQuery = new Query(baseId, root, editOnly);

        myQuery.setFiltered(false); // be sure not to filter the query

        return getDBSession().getGSession().query(myQuery, this);
      }

    //    Ganymede.debug("DBEditObject: Returning null for choiceList for field: " + field.getName());

    return null;
  }

  /**
   * <p>This method is used to tell the client whether the list of options it gets
   * back for a field can be taken out of the cache.  If this method returns
   * true, that means that some of the results that obtainChoiceList() will
   * return will include items that normally wouldn't be availble to the
   * client, but are in this case because of the anonymousLinkOK() results.</p>
   *
   * <p>This is kind of wacked-out random stuff.  It's basically a way of
   * allowing DBEditObject to get involved in the decision as to whether an
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.server.InvidDBField#choicesKey() choicesKey()}'s
   * method should disallow client-side caching for the field's choice list.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses, though the default
   * logic here tries to be smart about the typical cases.</p>
   */

  public boolean choiceListHasExceptions(DBField field)
  {
    if (!(field instanceof InvidDBField))
      {
        // "field not an InvidDBField"
        throw new IllegalArgumentException(ts.l("choiceListHasExceptions.badType"));
      }

    // --

    DBObjectBaseField fieldDef;
    short baseId;
    short targetField;

    /* -- */

    fieldDef = field.getFieldDef();

    baseId = fieldDef.getTargetBase();

    if (fieldDef.isSymmetric())
      {
        targetField = fieldDef.getTargetField();
      }
    else
      {
        targetField = SchemaConstants.BackLinksField;
      }

    DBObjectBase targetBase = Ganymede.db.getObjectBase(baseId);

    return targetBase.getObjectHook().anonymousLinkOK(null, targetField,
                                                      this, field.getID(),
                                                      this.getGSession());
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses,
   * particularly if you have a StringDBField that you want to force
   * to pick from the list of choices provided by your DBEditObject
   * subclass' obtainChoiceList() method.</p>
   */

  public boolean mustChoose(DBField field)
  {
    // by default, we assume that InvidDBField's are always
    // must choose.

    if (field instanceof InvidDBField)
      {
        return true;
      }

    return false;
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass can use
   * to determine whether it is permissible to enter IPv6 address in a
   * particular (IP) DBField.  The default assumption is that IPv6
   * addresses are not accepted.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public boolean isIPv6OK(DBField field)
  {
    return false;
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.server.DateDBField DateDBField} has a restricted
   * range of possibilities.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public boolean isDateLimited(DBField field)
  {
    if (getGSession() != null && getGSession().enableWizards &&
        ((field.getID() == SchemaConstants.ExpirationField) ||
         (field.getID() == SchemaConstants.RemovalField)))
      {
        return true; // no values in the past, thanks
      }

    return false;
  }

  /**
   * <p>This method is used to specify the earliest acceptable date
   * for the specified {@link arlut.csd.ganymede.server.DateDBField DateDBField}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public Date minDate(DBField field)
  {
    if (getGSession() != null && getGSession().enableWizards &&
        ((field.getID() == SchemaConstants.ExpirationField) ||
         (field.getID() == SchemaConstants.RemovalField)))
      {
        return new Date(); // no values in the past, thanks
      }

    return new Date(Long.MIN_VALUE);
  }

  /**
   * <p>This method is used to specify the latest acceptable date
   * for the specified {@link arlut.csd.ganymede.server.DateDBField DateDBField}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public Date maxDate(DBField field)
  {
    return new Date(Long.MAX_VALUE);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}
   * has a restricted range of possibilities.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public boolean isIntLimited(DBField field)
  {
    return false;
  }

  /**
   * <p>This method is used to specify the minimum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public int minInt(DBField field)
  {
    return Integer.MIN_VALUE;
  }

  /**
   * <p>This method is used to specify the maximum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public int maxInt(DBField field)
  {
    return Integer.MAX_VALUE;
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.server.FloatDBField FloatDBField}
   * has a restricted range of possibilities.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public boolean isFloatLimited(DBField field)
  {
    return false;
  }

  /**
   * <p>This method is used to specify the minimum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.FloatDBField FloatDBField}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public double minFloat(DBField field)
  {
    return Double.MIN_VALUE;
  }

  /**
   * <p>This method is used to specify the maximum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.FloatDBField FloatDBField}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public double maxFloat(DBField field)
  {
    return Double.MAX_VALUE;
  }

  /**
   * <p>This method handles inactivation logic for this object type.
   * A DBEditObject must first be checked out for editing, then the
   * inactivate() method can then be called on the object to put the
   * object into inactive mode.  inactivate() will set the object's
   * removal date and fix up any other state information to reflect
   * the object's inactive status.</p>
   *
   * <p>inactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference in
   * the ReturnVal object returned, to guide the user through a set of
   * interactive dialogs to inactive the object.</p>
   *
   * <p>The inactive() method can cause other objects to be deleted,
   * can cause strings to be removed from fields in other objects,
   * whatever.</p>
   *
   * <p>If inactivate() returns a ReturnVal that has its success flag
   * set to false and does not include a JDialogBuff for further
   * interaction with the user, then DBSEssion.inactivateDBObject()
   * method will rollback any changes made by this method.</p>
   *
   * <p>If inactivate() returns a success value, we expect that the
   * object will have a removal date set.</p>
   *
   * <p>IMPORTANT NOTE 1: This method is intended to be called by the
   * DBSession.inactivateDBObject() method, which establishes a
   * checkpoint before calling inactivate.  If this method is not
   * called by DBSession.inactivateDBObject(), you need to push a
   * checkpoint with the key 'inactivate'+label, where label is the
   * returned name of this object.</p>
   *
   * <p>IMPORTANT NOTE 2: If a custom object's inactivate() logic
   * decides to enter into a wizard interaction with the user, that
   * logic is responsible for calling finalizeInactivate() with a
   * boolean indicating ultimate success of the operation.</p>
   *
   * <p>Finally, it is up to commitPhase1() and commitPhase2() to
   * handle any external actions related to object inactivation when
   * the transaction is committed..</p>
   *
   * @see #commitPhase1()
   * @see #commitPhase2()
   *
   * @param ckp_label The checkpoint label which should be popped or
   * rolledback on necessity by the custom inactivate method.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal inactivate(String ckp_label)
  {
    // "DBEditObject.inactivate() Error"
    // "This object type has not been configured to allow inactivation."
    return Ganymede.createErrorDialog(this.getGSession(),
                                      ts.l("inactivate.error"),
                                      ts.l("inactivate.errorTxt"));
  }

  /**
   * <p>This method is to be called by the custom DBEditObject inactivate()
   * logic when the inactivation is performed so that logging can be
   * done.</p>
   *
   * <p>If inactivation of an object causes the label to be null, this
   * won't work as well as we'd really like.</p>
   */

  public final void finalizeInactivate(boolean success, String ckp_label)
  {
    if (success)
      {
        Object val = getFieldValueLocal(SchemaConstants.RemovalField);

        if (val != null)
          {
            Vector<Invid> invids = new Vector<Invid>();

            invids.add(this.getInvid());

            // "{0} {1} has been inactivated.\n\nThe object is due to be removed from the database at {2}.\n\n"
            editset.logEvent(new DBLogEvent("inactivateobject",
                                            ts.l("finalizeInactivate.removeSet",
                                                 getTypeName(),
                                                 getLabel(),
                                                 getFieldValueLocal(SchemaConstants.RemovalField).toString()),
                                            getGSession().getPermManager().getResponsibleInvid(),
                                            getGSession().getPermManager().getIdentity(),
                                            invids,
                                            getEmailTargets(this)));
          }
        else
          {
            Vector<Invid> invids = new Vector<Invid>();

            invids.add(this.getInvid());

            // "{0} {1} has been inactivated.\n\nThe object has no removal date set.\n\n"
            editset.logEvent(new DBLogEvent("inactivateobject",
                                            ts.l("finalizeInactivate.noRemove", getTypeName(), getLabel()),
                                            getGSession().getPermManager().getResponsibleInvid(),
                                            getGSession().getPermManager().getIdentity(),
                                            invids,
                                            getEmailTargets(this)));
          }

        editset.popCheckpoint(ckp_label);
      }
    else
      {
        editset.rollback(ckp_label);
      }
  }

  /**
   * <p>This method handles reactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * reactivate() method can then be called on the object to make the
   * object active again.  reactivate() will clear the object's
   * removal date and fix up any other state information to reflect
   * the object's reactive status.</p>
   *
   * <p>reactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to reactive the object.</p>
   *
   * <p>If reactivate() returns a ReturnVal that has its success flag set to false
   * and does not include a {@link arlut.csd.JDialog.JDialogBuff JDialogBuff}
   * for further interaction with the
   * user, then
   * {@link arlut.csd.ganymede.server.DBSession#inactivateDBObject(arlut.csd.ganymede.server.DBEditObject) inactivateDBObject()}
   * method will rollback any changes made by this method.</p>
   *
   * <p>IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.</p>
   *
   * <p>Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object reactivation when
   * the transaction is committed..</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @see #commitPhase1()
   * @see #commitPhase2()
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal reactivate(String ckp_label)
  {
    if (isInactivated())
      {
        // by default, we'll just clear the removal field

        setFieldValueLocal(SchemaConstants.RemovalField, null);
        return null;            // success
      }

    // "DBEditObject.reactivate() error"
    // "I can''t reactivate this object.  It was not inactivated."

    return Ganymede.createErrorDialog(this.getGSession(),
                                      ts.l("reactivate.error"),
                                      ts.l("reactivate.errorTxt"));
  }

  /**
   * This method is to be called by the custom DBEditObject reactivate()
   * logic when the reactivation is performed so that logging can be
   * done.
   */

  protected final void finalizeReactivate(boolean success, String ckp_label)
  {
    if (success)
      {
        Vector<Invid> invids = new Vector<Invid>();

        invids.add(this.getInvid());

        // "{0} {1} has been reactivated.\n\n"
        editset.logEvent(new DBLogEvent("reactivateobject",
                                        ts.l("finalizeReactivate.message", getTypeName(), getLabel()),
                                        getGSession().getPermManager().getResponsibleInvid(),
                                        getGSession().getPermManager().getIdentity(),
                                        invids,
                                        getEmailTargets(this)));

        editset.popCheckpoint(ckp_label);
      }
    else
      {
        editset.rollback(ckp_label); // see DBSession.reactivateDBObject() for checkpoint
      }
  }

  /**
   * <p>This method handles removal logic for this object type.  This method
   * will be called immediately from DBSession.deleteDBObject().</p>
   *
   * <p>The remove() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.</p>
   *
   * <p>If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, the DBSession.deleteDBObject() method will roll back any changes
   * made by this method.</p>
   *
   * <p>remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.</p>
   *
   * <p>IMPORTANT NOTE: If a custom object's remove() logic decides to
   * enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeRemove() on the object when
   * it is determined that the object really should be removed,
   * with a boolean indicating whether success was had.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal remove()
  {
    return null;
  }

  /**
   * <p>This method handles Ganymede-internal deletion logic for this
   * object type.  finalizeRemove() is responsible for dissolving any
   * invid inter-object references in particular.</p>
   *
   * <p>It is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object removal when
   * the transaction is committed..</p>
   *
   * <p>finalizeremove() returns a ReturnVal indicating whether the
   * internal removal bookkeeping was successful.  A failure result
   * will cause the DBSession to rollback the transaction to the state
   * prior to any removal actions for this object were
   * attempted.</p>
   *
   * <p>remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.</p>
   *
   * @param success If true, finalizeRemove() will clear all fields,
   * thereby unlinking invid fields and relinquishing namespace claims.
   * If false, finalizeRemove() will rollback to the state the system
   * was in before DBSession.deleteDBObject() was entered.
   *
   * @see #commitPhase1()
   * @see #commitPhase2()
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final synchronized ReturnVal finalizeRemove(boolean success, String ckp_label)
  {
    ReturnVal retVal = null;
    String label = getLabel();  // remember the label before we clear it

    /* -- */

    if (!success)
      {
        editset.rollback(ckp_label); // *sync*

        // "Object Removal Error"
        // "Could not delete object {0}.  Custom code in the server rejected this operation."

        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("finalizeRemove.error"),
                                          ts.l("finalizeRemove.errorTxt", label));
      }

    // we want to delete / null out all fields.. this will take care
    // of invid links, embedded objects, and namespace allocations.

    // set the deleting flag to true so that our subclasses won't
    // freak about values being set to null.

    this.deleting = true;

    try
      {
        // get a sync'ed snapshot of this object's fields

        Vector<DBField> fieldVect = getFieldVect();

        for (DBField memberField: fieldVect)
          {
            // we can't clear field 0 (the owner/container field) yet,
            // since we need that for permissions verifications for
            // other fields

            if (memberField.getID() == 0)
              {
                continue;
              }

            if (memberField.isVector())
              {
                while (memberField.size() > 0)
                  {
                    try
                      {
                        // if this is an edit-in-place InvidDBField,
                        // deleteElement() will convert this request into
                        // a deletion of the embedded object if necessary

                        retVal = ReturnVal.merge(retVal, memberField.deleteElement(0)); // *sync*

                        if (!ReturnVal.didSucceed(retVal))
                          {
                            editset.rollback(ckp_label); // *sync*

                            if (retVal.getDialog() != null)
                              {
                                return retVal;
                              }

                            // "Server: Error in DBEditObject.finalizeRemove()"
                            // "Custom code disapproved of deleting element from field {0}."
                            return Ganymede.createErrorDialog(this.getGSession(),
                                                              ts.l("finalizeRemove.myError"),
                                                              ts.l("finalizeRemove.badDelete", memberField.getName()));
                          }
                      }
                    catch (GanyPermissionsException ex)
                      {
                        editset.rollback(ckp_label); // *sync*

                        // "Server: Error in DBEditObject.finalizeRemove()"
                        // "Permissions violation during attempted deletion of element from field {0}."
                        return Ganymede.createErrorDialog(this.getGSession(),
                                                          ts.l("finalizeRemove.myError"),
                                                          ts.l("finalizeRemove.badDeletePerm", memberField.getName()));
                      }
                  }
              }
            else
              {
                // permission and field option matrices, along with
                // passwords, don't allow us to call set value
                // directly.  We're mainly concerned with invid's (for
                // linking), i.p. addresses and strings (for the
                // namespace) here anyway.

                if (memberField.getType() != PERMISSIONMATRIX &&
                    memberField.getType() != PASSWORD &&
                    memberField.getType() != FIELDOPTIONS)
                  {
                    retVal = ReturnVal.merge(retVal, memberField.setValueLocal(null)); // *sync*

                    if (!ReturnVal.didSucceed(retVal))
                      {
                        editset.rollback(ckp_label); // *sync*

                        if (retVal.getDialog() != null)
                          {
                            return retVal;
                          }

                        // "Server: Error in DBEditObject.finalizeRemove()"
                        // "Custom code disapproved of clearing the value held in field {0}."
                        return Ganymede.createErrorDialog(this.getGSession(),
                                                          ts.l("finalizeRemove.myError"),
                                                          ts.l("finalizeRemove.badScalarClear", memberField.getName()));
                      }
                  }
                else
                  {
                    // catchall for permission matrix, field options,
                    // and password fields, which do this their own
                    // way.

                    try
                      {
                        memberField.setUndefined(true);
                      }
                    catch (GanyPermissionsException ex)
                      {
                        throw new RuntimeException(ex); // should never happen
                      }
                  }
              }
          }

        // ok, we've cleared all fields but field 0.. clear that to finish up.

        DBField field = retrieveField((short) 0);

        if (field != null)
          {
            if (field.isVector())
              {
                // if we're deleting elements out of vector field 0 (the list
                // of owner groups), we'll want to deleteElementLocal.. this
                // will simplify things and will prevent us from losing permission
                // to write to this field in midstream (although the new DBField
                // permCache would actually obviate this problem as well).

                while (field.size() > 0)
                  {
                    retVal = ReturnVal.merge(retVal, field.deleteElementLocal(0)); // *sync*

                    if (!ReturnVal.didSucceed(retVal))
                      {
                        editset.rollback(ckp_label); // *sync*

                        // "Server: Error in DBEditObject.finalizeRemove()"
                        // "Custom code disapproved of deleting element from field {0}."
                        return Ganymede.createErrorDialog(this.getGSession(),
                                                          ts.l("finalizeRemove.myError"),
                                                          ts.l("finalizeRemove.badDelete", field.getName()));
                      }
                  }
              }
            else
              {
                // scalar field 0 is the ContainerField for an
                // embedded object.  Note that setting this field to
                // null will not unlink us from the container object,
                // since the ContainerField pointer is a generic one
                // (it has no symmetric relationship information
                // contained in the invid field definition)

                retVal = ReturnVal.merge(retVal, field.setValueLocal(null)); // *sync*

                if (!ReturnVal.didSucceed(retVal))
                  {
                    editset.rollback(ckp_label);  // *sync*

                    // "Server: Error in DBEditObject.finalizeRemove()"
                    // "Custom code disapproved of clearing the value held in field {0}."
                    return Ganymede.createErrorDialog(this.getGSession(),
                                                      ts.l("finalizeRemove.myError"),
                                                      ts.l("finalizeRemove.badScalarClear", field.getName()));
                  }
              }
          }

        // Finally we need to take care of any back links. This scans
        // all objects which have asymmetric invid fields pointing to
        // us, checks them out for editing if they are not already
        // checked out by this session, and takes this object's Invid
        // out of all fields in those objects.

        retVal = attemptAsymBackLinkClear(true);

        if (!ReturnVal.didSucceed(retVal))
          {
            editset.rollback(ckp_label); // *sync*

            return retVal;
          }

        editset.popCheckpoint(ckp_label);

        return retVal;
      }
    finally
      {
        // make sure we clear deleting before we return

        deleting = false;
      }
  }

  /**
   * <p>This method is used to find all objects which point to us
   * through non-symmetric links, edit them, and break the link.  We
   * do this by consulting the global Ganymede.db.backPointers {@link
   * arlut.csd.ganymede.server.DBLinkTracker DBLinkTracker} to get the
   * list of objects which point to us, and doing the unlink in a
   * fashion similar to InvidDBField.unbindAll().</p>
   *
   * <p><b>This method is private, and is not to be called by any code outside
   * of this class.</b></p>
   *
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @return null on success, or a ReturnVal with an error dialog encoded on failure
   */

  private final ReturnVal attemptAsymBackLinkClear(boolean local)
  {
    ReturnVal
      retVal = null;

    /* -- */

    if (false)
      {
        System.err.println("Entering attemptAsymBackLinkClear() for object " + toString());
      }

    Set<Invid> linkSources = Ganymede.db.aSymLinkTracker.getForwardLinkSources(getDBSession(), getInvid());

    for (Invid remote: linkSources)
      {
        retVal = ReturnVal.merge(retVal, clearBackLink(remote, local));

        if (!ReturnVal.didSucceed(retVal))
          {
            return retVal;      // finalizeRemove() will rollback
          }
      }

    return retVal;
  }

  /**
   * <p>This method is called by attemptAsymBackLinkClear(), and is
   * responsible for checking the object with Invid remote out for
   * editing, and clearing our own Invid out of all of the remote
   * object's asymmetric Invid fields.</p>
   *
   * <p>This method does no checkpointing, so
   * attemptAsymBackLinkClear() has to do that for us.</p>
   *
   * @param remote An Invid for an object that we believe has
   * asymmetric forward links to us.
   * @param local If true, we won't do a permissions check before trying to edit the
   * remote object.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  private ReturnVal clearBackLink(Invid remote, boolean local)
  {
    short targetField;

    DBEditObject
      oldRef = null;

    DBObject
      remobj;

    InvidDBField
      oldRefField = null;

    DBSession
      session = getDBSession();

    ReturnVal
      retVal = ReturnVal.success();

    ArrayList<Short>
      fieldsToUnbind = new ArrayList<Short>();

    /* -- */

    // check to see if we have permission to anonymously unlink
    // this field from the target field, else go through the
    // GanymedeSession layer to have our permissions checked.

    // note that if the GanymedeSession layer has already checked out the
    // object, session.editDBObject() will return a reference to that
    // version, and we'll lose our security bypass.. for that reason,
    // we also use the anon variable to instruct dissolve to disregard
    // write permissions if we have gotten the anonymous OK

    remobj = session.viewDBObject(remote);

    if (remobj == null)
      {
        // "DBEditObject.clearBackLink(): Couldn''t find old reference"
        // "Your operation could not succeed because {0} was linked to a remote reference {1} that could not be resolved for unlinking.\n\nThis is a serious logic error in the server.  You should run the invid diagnostics in the admin console."
        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("clearBackLink.badRef"),
                                          ts.l("clearBackLink.badRefTxt", toString(), remote.toString()));
      }

    if (false)
      {
        System.err.println("DBEditObject.clearBackLink(): Clearing link in " + getGSession().describe(remote));
      }

    // loop over the invid fields in the target, get a list of fields we need to unlink.

    Invid myInvid = getInvid();

    // get a thread sync'ed snapshot of the fields in the remote object

    Vector<DBField> fieldVect = remobj.getFieldVect();

    for (DBField tmpField: fieldVect)
      {
        if (!(tmpField instanceof InvidDBField))
          {
            continue;
          }

        // if the field is symmetric, the InvidDBField logic should
        // take care of the unbinding, so we won't need to take care
        // of it ourselves.

        if (tmpField.getFieldDef().isSymmetric())
          {
            continue;
          }

        // If the invid field we're checking out doesn't reference
        // us, don't bother with it.

        if (tmpField.isVector())
          {
            if (!tmpField.containsElementLocal(myInvid))
              {
                continue;
              }
          }
        else
          {
            Invid tempInvid = (Invid) tmpField.getValueLocal();

            if (!myInvid.equals(tempInvid))
              {
                continue;
              }
          }

        // ok, we know we need to do the unbinding for this field.

        fieldsToUnbind.add(tmpField.getID());
      }

    if (remobj instanceof DBEditObject)
      {
        oldRef = (DBEditObject) remobj;
      }
    else
      {
        if (local || getGSession().getPermManager().getPerm(remobj).isEditable())
          {
            oldRef = (DBEditObject) session.editDBObject(remote);
          }
      }

    if (oldRef == null)
      {
        // it's there, but we can't unlink it

        // "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
        // "You don''t have permission to unlink object {0} from one or more fields in object {1}."
        return Ganymede.createErrorDialog(this.getGSession(),
                                          ts.l("clearBackLink.badUnlink"),
                                          ts.l("clearBackLink.perm", getLabel(), remobj.toString()));
      }

    for (Short remote_fieldid: fieldsToUnbind)
      {
        targetField = remote_fieldid.shortValue();

        retVal.addRescanField(remote, targetField);

        // are we allowed to ignore permissions on this field?

        try
          {
            oldRefField = oldRef.getInvidField(targetField);
          }
        catch (ClassCastException ex)
          {
            // "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
            // "Your operation could not succeed due to an error in the server''s custom schema code.  Target field {0} in object {1} is not an Invid field."
            return Ganymede.createErrorDialog(this.getGSession(),
                                              ts.l("clearBackLink.badUnlink"),
                                              ts.l("clearBackLink.badTarget", oldRef.getField(targetField).getName(), oldRef.getLabel()));
          }

        if (oldRefField == null)
          {
            // editDBObject() will create undefined fields for all fields defined
            // in the DBObjectBase, so if we got a null result we have a schema
            // corruption problem.

            // "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
            // "Your operation could not succeed due to an inconsistency in the server''s database schema.  Target field number {0} in object {1} does not exist, or you do not have permission to access this field."

            return Ganymede.createErrorDialog(this.getGSession(),
                                              ts.l("clearBackLink.badUnlink"),
                                              ts.l("clearBackLink.badSchema", Short.valueOf(targetField), oldRef.getLabel()));
          }

        try
          {
            // clear any reference in this field to us, if we can

            retVal = ReturnVal.merge(retVal, oldRefField.dissolve(getInvid(), local));

            if (!ReturnVal.didSucceed(retVal))
              {
                return retVal;
              }
          }
        catch (IllegalArgumentException ex)
          {
            System.err.println("hm, couldn't dissolve a reference in " + getLabel());

            System.err.println("Did do an anonymous edit on target");

            throw (IllegalArgumentException) ex;
          }
      }

    // tell the client that it needs to rescan the old remote ends of this binding

    return retVal;
  }

  /**
   * <p>This method provides a pre-commit hook that runs after the
   * user has hit commit but before the system has established write
   * locks for the commit.</p>
   *
   * <p>The intended purpose of this hook is to allow objects that
   * dynamically maintain hidden label fields to update those fields
   * from the contents of the object's other fields at commit
   * time.</p>
   *
   * <p>This method runs in a checkpointed context.  If this method
   * fails in any operation, you should return a ReturnVal with a
   * failure dialog encoded, and the transaction's commit will be
   * blocked and a dialog explaining the problem will be presented to
   * the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal preCommitHook()
  {
    return null;
  }

  /**
   * <p>This method performs verification for the first phase of
   * the two-phase commit algorithm.  If this object returns
   * true from commitPhase1() when called during an editSet's
   * commit() routine, this object CAN NOT refuse commit()
   * at a subsequent point.  Once commitPhase1() is called,
   * the object CAN NOT be changed until the transaction
   * is either committed, abandoned, or released from the
   * commit process by the
   * {@link arlut.csd.ganymede.server.DBEditObject#release(boolean) release()}
   * method.</p>
   *
   * <p>This method may be subclassed by application objects that need
   * to include extra-Ganymede processes in the two-phase commit
   * protocol.  If a particular subclass of DBEditObject does not need
   * to involve outside processes in the full two-phase commit
   * protocol, this method should not be overridden.</p>
   *
   * <p>If this method is overridden, be sure and call
   * setCommitting(true) before doing anything else.  Failure to call
   * setCommitting() in this method will cause the two phase commit
   * mechanism to behave unpredictably.</p>
   *
   * <p><b>WARNING!</b> this method is called at a time when portions
   * of the database are locked for the transaction's integration into
   * the database.  You must not call methods that seek to gain a lock
   * on the Ganymede database.  At this point, this means no composite
   * queries on embedded object types, where you seek an object based
   * on a field in an embedded object and in the object itself, using
   * the GanymedeSession query calls, or else you will lock the server.</p>
   *
   * <p>This method should NEVER try to edit or change any DBEditObject
   * in the server.. at this point in the game, the server has fixed the
   * transaction working set and is depending on commitPhase1() not trying
   * to make changes.</p>
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal commitPhase1()
  {
    setCommitting(true);

    // if we are deleting or dropping this user, don't bother to check
    // for consistency

    switch (this.getStatus())
      {
      case ObjectStatus.DELETING:
      case ObjectStatus.DROPPING:
        return null;
      }

    // if we have enableOversight turned on, let's check and see if
    // this object is currently consistent.  If it is not, and it was
    // before this transaction started, report the problem.

    // If this object is in an inconsistent state, and was before the
    // transaction started, don't block the commit, because the object
    // may have been rendered inconsistent by forcible deletion of
    // other objects previously linked by this object, and we don't
    // want to block a commit that a non-privileged, non-supergash
    // admin may not necessarily be able to fix.

    if (getGSession().enableOversight)
      {
        ReturnVal retVal = consistencyCheck(this);

        if (ReturnVal.didSucceed(retVal))
          {
            return retVal;      // no problem
          }

        if (original != null)
          {
            ReturnVal retVal2 = consistencyCheck(original);

            if (!ReturnVal.didSucceed(retVal2))
              {
                return null;    // we were already inconsistent, so don't complain
              }
          }

        // we were consistent before (or newly created in an
        // inconsistent state), so complain

        return retVal;
      }
    else
      {
        return null;
      }
  }

  /**
   * This method returns true if this object has already gone
   * through phase 1 of the commit process, which requires
   * the DBEditObject not to accept further changes.
   *
   * {@link arlut.csd.ganymede.server.DBField DBField}'s
   * {@link arlut.csd.ganymede.server.DBField#isEditable(boolean) isEditable()}
   * method consults this method to determine whether to allow
   * editing of fields.  While a DBEditObject is in the committing
   * process, no changes to fields will be allowed.
   */

  public final boolean isCommitting()
  {
    return commitSemaphore.isSet();
  }

  /**
   * This method is intended to be used by subclasses to set the
   * state of this object's committing flag.
   */

  protected final void setCommitting(boolean state)
  {
    commitSemaphore.set(state);
  }

  /**
   * <p>This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.</p>
   *
   * <p>For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invocation should be placed here,
   * rather than in
   * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase1() commitPhase1()}.</p>
   *
   * <p>commitPhase2() is generally the last method called on a
   * DBEditObject before it is discarded by the server in the
   * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#commit(java.lang.String) commit()} method.</p>
   *
   * <p>Subclasses that override this method may wish to make this method
   * synchronized.</p>
   *
   * <p><b>WARNING!</b> this method is called at a time when portions
   * of the database are locked for the transaction's integration into
   * the database.  You must not call methods that seek to gain a lock
   * on the Ganymede database.  At this point, this means no composite
   * queries on embedded object types, where you seek an object based
   * on a field in an embedded object and in the object itself, using
   * the GanymedeSession query calls, or else you will lock the server.</p>
   *
   * <p>This method should NEVER try to edit or change any DBEditObject
   * in the server.. at this point in the game, the server has fixed the
   * transaction working set and is depending on commitPhase2() not trying
   * to make changes internal to the server.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   */

  public void commitPhase2()
  {
    return;
  }

  /**
   * <p>A hook for subclasses to use to clean up any external
   * resources allocated for this object.  This method can be called
   * after commitPhase1() has been called, or it may be called at any
   * time to indicate that this object is being withdrawn from the
   * transaction (as by a checkpoint rollback).  This method <b>will
   * not</b> be called after commitPhase2() has been called.  If you
   * do anything external in commitPhase2(), make sure that all
   * resources allocated for this object (at any time in this object's
   * editing life-cycle) are released before commitPhase2()
   * completes.</p>
   *
   * <p>Ordinarily, there is no need for customizers to override this
   * method.  The only reason to override the release method is if you
   * need to do maintenance on external data structures or connections
   * that were created in commitPhase1() or when this DBEditObject was
   * created.</p>
   *
   * <p>If &lt;finalAbort&gt; is true, the transaction for which this
   * DBEditObject was created is being completely abandoned (if
   * isCommitting() returns true), or this object is being dropped out
   * of the transaction by a checkpoint rollback.  In either case, a
   * customizer may want to clean up all external structures or
   * connections that were created either at the time this
   * DBEditObject was created/checked-out and/or that were created by
   * commitPhase1().</p>
   *
   * <p>If &lt;finalAbort&gt; is false, isCommitting() should always
   * return true.  In this case, one of the DBEditObjects in the
   * transaction returned false from a later commitPhase1() call
   * and all objects that had their commitPhase1() methods called
   * previously will be revisted and release(false) will be called
   * on them.  Customizers may want to clean up any external structures
   * or connections that were established in commitPhase1().</p>
   *
   * <p>Remember, you will usually want to perform external actions in
   * commitPhase2(), in which case release() is not needed.  release()
   * is only useful when you allocate external structures or
   * connections when the object is created or during commitPhase1().</p>
   *
   * <p>It is safe to call release() from your commitPhase2() method
   * if you wish to have one place to clean up structures allocated by
   * initializeNewObject() or commitPhase1().</p>
   *
   * <p>Customizers subclassing this method may want to keep a couple of
   * things in mind.  First, the default release method is not synchronized,
   * and it basically just clear a boolean flag (call setCommitting(false)) to
   * indicate that edit methods on this object may once again go forward.
   * You may want to synchronize your release method if you do anything
   * at all fancy.  More importantly, it is essential that you call setCommitting(false)
   * if &lt;finalAbort&gt; is false so that this object can be edited afterwards.</p>
   *
   * <p><b>WARNING!</b> this method is called at a time when portions
   * of the database are locked for the transaction's integration into
   * the database.  You must not call methods that seek to gain a lock
   * on the Ganymede database.  At this point, this means no composite
   * queries on embedded object types, where you seek an object based
   * on a field in an embedded object and in the object itself, using
   * the GanymedeSession query calls, or else you will lock the server.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @param finalAbort If true, this object is being dropped, either due to an
   * aborted transaction or a checkpoint rollback.
   */

  public void release(boolean finalAbort)
  {
    if (!finalAbort)
      {
        setCommitting(false);
      }
  }

  // ***
  //
  // Checkpoint / Rollback support
  //
  // ***

  /**
   * <p>Returns a hashtable mapping Short field id's to their current
   * value, used by the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * intra-transaction checkpointing logic to capture this object's
   * state at a given time.</p>
   *
   * <p>Each subclass of {@link arlut.csd.ganymede.server.DBField DBField}
   * is responsible for implementing its own version of
   * {@link arlut.csd.ganymede.server.DBField#checkpoint() checkpoint()} to
   * stuff its state into an Object for inclusion in this method's
   * hashtable.</p>
   */

  final Hashtable<Short, Object> checkpoint()
  {
    Short key;
    Object value;
    Hashtable<Short, Object> result = new Hashtable<Short, Object>();

    /* -- */

    // sync'ing on fieldAry is safe enough to do here since we don't
    // call any methods within the sync block that should trigger any
    // external synchronization issues

    Vector<DBField> fields = this.getFieldVect();

    for (DBField field: fields)
      {
        key = Short.valueOf(field.getID());
        value = field.checkpoint();

        if (value != null)
          {
            result.put(key, value);
          }
        else
          {
            // hack, hack.. we're using a reference
            // to this object to represent a null value

            result.put(key, this);
          }
      }

    return result;
  }

  /**
   * <p>Reinstates this object's state from a hashtable returned
   * by {@link arlut.csd.ganymede.server.DBEditObject#checkpoint() checkpoint()},
   * used by the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * intra-transaction checkpoint rollback logic to restore this object's
   * state at a given time.</p>
   *
   * <p>Each subclass of {@link arlut.csd.ganymede.server.DBField DBField}
   * is responsible for implementing its own version of
   * {@link arlut.csd.ganymede.server.DBField#rollback(java.lang.Object) rollback()} to
   * restore its state.</p>
   */

  synchronized final void rollback(Map<Short, Object> ckpoint)
  {
    for (Map.Entry<Short, Object> entry: ckpoint.entrySet())
      {
        DBField field = retrieveField(entry.getKey().shortValue());
        Object value = entry.getValue();

        // again, we use a reference to ourselves as a
        // hackish way of representing null in the
        // hashtable

        if (value == this)
          {
            field.rollback(null);
          }
        else
          {
            field.rollback(value);
          }
      }
  }

  /**
   * <p>This method is used to write out this DBEditObject and the
   * deltas between this DBEditObject and the original checked in
   * version of this DBEditObject to the provided XMLDumpContext.  The
   * constraints specified in the {@link
   * arlut.csd.ganymede.server.SyncRunner} registered with the
   * XMLDumpContext parameter will be used to control what fields are
   * written.</p>
   *
   * <p>This method should only be called on a DBEditObject that is known
   * to have been edited.. calling this method on a newly created
   * object should not be done.. use the normal DBObject emitXML()
   * method in an &lt;object_delta&gt; element's &lt;after&gt; element
   * instead.</p>
   */

  public final void emitXMLDelta(XMLDumpContext xmlOut) throws IOException
  {
    boolean fieldChanged;

    /* -- */

    if (getStatus() != ObjectStatus.EDITING)
      {
        // "Can''t call emitXMLDelta on a DBEditObject in the CREATING, DELETING, or DROPPING state."
        throw new IllegalArgumentException(ts.l("emitXMLDelta.bad_state"));
      }

    xmlOut.startElementIndent("object_delta");
    xmlOut.indentOut();

    xmlOut.setBeforeStateDumping(true);
    xmlOut.startElementIndent("before");
    xmlOut.indentOut();

    xmlOut.startElementIndent("object");
    xmlOut.attribute("type", XMLUtils.XMLEncode(getTypeName()));
    xmlOut.attribute("id", original.getLabel());
    xmlOut.attribute("oid", getInvid().toString());
    xmlOut.indentOut();

    for (DBObjectBaseField fieldDef: objectBase.getFieldsInDisplayOrder())
      {
        DBField myField = this.getField(fieldDef.getID());
        DBField origField = original.getField(fieldDef.getID());

        if (origField == null)
          {
            // not present in the before state, don't write anything for it

            continue;
          }
        else if (myField != null)
          {
            // we had this field in the before state, let's see if we
            // need to write it out, either because this field was
            // deleted from the after state and our sync constraints
            // require us to write it out, or because the field is
            // present both before and after, but it has changed and
            // our sync constraints require us to write it out in that
            // circumstance.

            if ((!myField.isDefined() && xmlOut.mayInclude(origField))|| (myField.isDefined() && xmlOut.shouldInclude(myField, origField)))
              {
                origField.emitXML(xmlOut);
              }
          }
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("object");

    xmlOut.indentIn();
    xmlOut.endElementIndent("before");
    xmlOut.setBeforeStateDumping(false);

    xmlOut.startElementIndent("after");
    xmlOut.indentOut();

    xmlOut.startElementIndent("object");
    xmlOut.attribute("type", XMLUtils.XMLEncode(getTypeName()));
    xmlOut.attribute("id", getLabel());
    xmlOut.attribute("oid", getInvid().toString());
    xmlOut.indentOut();

    for (DBObjectBaseField fieldDef: objectBase.getFieldsInDisplayOrder())
      {
        DBField myField = this.getField(fieldDef.getID());
        DBField origField = original.getField(fieldDef.getID());

        if (myField == null || !myField.isDefined())
          {
            // not present in the after state, don't write anything for it

            continue;
          }
        else
          {
            // we have this field in the after state, let's see if we
            // need to write it out, either because this field was
            // newly created in the after state and our sync
            // constraints require us to write it out, or because the
            // field is present both before and after, but it has
            // changed and our sync constraints require us to write it
            // out in that circumstance.

            if ((origField == null && xmlOut.mayInclude(myField))|| (origField != null && xmlOut.shouldInclude(myField, origField)))
              {
                myField.emitXML(xmlOut);
              }
          }
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("object");

    xmlOut.indentIn();
    xmlOut.endElementIndent("after");

    xmlOut.indentIn();
    xmlOut.endElementIndent("object_delta");
  }

  /**
   * <p>This method is used to generate a String describing the difference
   * between the current state of the DBEditObject and the original
   * object's state.</p>
   *
   * <p>This method can also be used if this object is newly created.. in
   * this case, it will just return a string containing many 'FieldAdded'
   * entries.</p>
   *
   * @return null if no difference was found
   */

  public synchronized String diff()
  {
    return this.diff(null);
  }

  /**
   * <p>This method does two things.  First and foremost, it is used
   * to generate a String describing the difference between the
   * current state of the DBEditObject and the original object's
   * state.</p>
   *
   * <p>This method can also be used if this object is newly created.. in
   * this case, it will just return a string containing many 'FieldAdded'
   * entries.</p>
   *
   * <p>The second purpose of this method is to generate entries in
   * the changedFieldDefs Set, listing those DBObjectBaseFields for
   * which we have observed a value change when comparing this
   * object's state with its original.  We do this as part of the diff
   * algorithm because the original DBField definition only provided
   * one method to compare two fields for differences, and that is the
   * getDiffString() method.  Since we're calling that here anyway,
   * recording the definition of fields that we know changed is an
   * extremely cheap win.  The changedFieldDefs Set is used in the
   * DBEditSet class to update time stamps in the DBObjectBaseFields,
   * so that builder tasks can tell whether they have been run since
   * any of a given field have been changed in a given
   * DBObjectBase.</p>
   *
   * @param changedFieldDefs If not null, this parameter will be a Set
   * that the diff algorithm should insert DBObjectBaseFields whose
   * value was found to have changed in this diff.
   *
   * @return null if no difference was found
   */

  public final synchronized String diff(Set<DBObjectBaseField> changedFieldDefs)
  {
    boolean diffFound = false;
    DBField origField, currentField;
    StringBuilder added = null;
    StringBuilder deleted = null;
    StringBuilder changed = null;

    /* -- */

    // algorithm: iterate over base.getFieldsInFieldOrder() to find all fields
    // possibly contained in the object.. for each field, check to
    // see if the value has changed.  if so, emit a before and after
    // diff.  if one has a field and the other does not, indicate
    // the change.
    //
    // in the case of vectors, the change description can be a simple
    // delta (x added, y removed)

    // note that we're counting on objectBase.sortedFields not being
    // changed while we're iterating here.. this is an ok assumption,
    // since only the loader and the schema editor will trigger changes
    // in sortedFields.

    if (debug)
      {
        System.err.println("Entering diff for object " + getLabel());
      }

    for (DBObjectBaseField fieldDef: objectBase.getFieldsInFieldOrder())
      {
        // we don't care if certain fields change, as they are
        // guaranteed to change whenever a transaction commits

        if (fieldDef.getID() == SchemaConstants.CreationDateField ||
            fieldDef.getID() == SchemaConstants.CreatorField ||
            fieldDef.getID() == SchemaConstants.ModificationDateField ||
            fieldDef.getID() == SchemaConstants.ModifierField)
          {
            continue;
          }

        if (debug)
          {
            System.err.println("Comparing field " + fieldDef.getName());
          }

        // if we're newly created, we'll just treat the old field as
        // non-existent.

        if (original == null)
          {
            origField = null;

            if (changedFieldDefs != null)
              {
                changedFieldDefs.add(fieldDef);
              }
          }
        else
          {
            origField = original.getField(fieldDef.getID());
          }

        currentField = this.getField(fieldDef.getID());

        if ((origField == null || !origField.isDefined()) &&
            (currentField == null || !currentField.isDefined()))
          {
            continue;
          }

        if (((origField == null) || !origField.isDefined()) &&
            ((currentField != null) && currentField.isDefined()))
          {
            if (changedFieldDefs != null)
              {
                changedFieldDefs.add(fieldDef);
              }

            if (added == null)
              {
                added = new StringBuilder();
              }

            if (okToLogField(currentField))
              {
                // "\t{0}: {1}\n"
                added.append(ts.l("diff.field_template", fieldDef.getName(), currentField.getValueString()));
              }
            else
              {
                // "\t{0}\n"
                added.append(ts.l("diff.anon_field_template", fieldDef.getName()));
              }

            diffFound = true;

            if (debug)
              {
                System.err.println("Field added: " + fieldDef.getName() + "\nValue: " +
                                   currentField.getValueString() + "\n");
              }
          }
        else if (((currentField == null) || !currentField.isDefined()) &&
                 ((origField != null) && origField.isDefined()))

          {
            if (changedFieldDefs != null)
              {
                changedFieldDefs.add(fieldDef);
              }

            if (deleted == null)
              {
                deleted = new StringBuilder();
              }

            if (okToLogField(origField))
              {
                // "\t{0}: {1}\n"
                deleted.append(ts.l("diff.field_template", fieldDef.getName(), origField.getValueString()));
              }
            else
              {
                // "\t{0}\n"
                deleted.append(ts.l("diff.anon_field_template", fieldDef.getName()));
              }

            diffFound = true;

            if (debug)
              {
                System.err.println("Field deleted: " + fieldDef.getName() + "\nValue: " +
                                   origField.getValueString() + "\n");
              }
          }
        else
          {
            String diff = currentField.getDiffString(origField);

            if (diff != null)
              {
                if (changedFieldDefs != null)
                  {
                    changedFieldDefs.add(fieldDef);
                  }

                if (changed == null)
                  {
                    changed = new StringBuilder();
                  }

                if (okToLogField(currentField) && okToLogField(origField))
                  {
                    changed.append(fieldDef.getName());
                    changed.append("\n");
                    changed.append(diff);

                    if (debug)
                      {
                        System.err.println("Field changed: " +
                                           fieldDef.getName() + "\n" +
                                           diff);
                      }
                  }
                else
                  {
                    changed.append(fieldDef.getName());
                    changed.append("\n");
                  }

                diffFound = true;
              }
          }
      }

    if (diffFound)
      {
        StringBuilder result = new StringBuilder();

        if (added != null && added.length() > 0)
          {
            // "Fields Added:\n\n{0}\n"
            result.append(ts.l("diff.added", added));
          }

        if (changed != null && changed.length() > 0)
          {
            // "Fields Changed:\n\n{0}\n"
            result.append(ts.l("diff.changed", changed));
          }

        if (deleted != null && deleted.length() > 0)
          {
            // "Fields Deleted:\n\n{0}\n"
            result.append(ts.l("diff.deleted", deleted));
          }

        return result.toString();
      }
    else
      {
        return null;
      }
  }

  /*----------------------------------------------------------

    Convenience methods for our customization subclasses

  ----------------------------------------------------------*/

  /**
   * Convenience method for our customization subclasses, returns
   * a reference to the server's internal 'supergash' session
   * if a DBEditObject subclass needs to do queries, etc., on
   * the server internally.
   */

  protected final GanymedeSession internalSession()
  {
    return Ganymede.getInternalSession();
  }
}
