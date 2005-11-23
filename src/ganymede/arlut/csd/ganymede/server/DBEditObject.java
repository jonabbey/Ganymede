/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.FieldType;
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
 * DBEditObject is the main base class that is subclassed by individual
 * application object types to provide editing and management intelligence.
 * Both static and instance methods are defined in DBEditObject which can
 * be subclassed to provide object management intelligence. 
 *
 * A instance of DBEditObject is a copy of a DBObject that has been
 * exclusively checked out from the main database so that a 
 * {@link arlut.csd.ganymede.server.DBSession DBSession}
 * can edit the fields of the object.  The DBEditObject class keeps
 * track of the changes made to fields, keeping things properly
 * synchronized with unique field name spaces.
 *
 * Generally, DBEditObjects are obtained in the context of a 
 * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} transaction object.  When
 * the DBEditSet is committed, a new {@link arlut.csd.ganymede.server.DBObject DBObject}
 * is created from the contents of the DBEditObject and is made to replace the
 * original object in the DBStore.  If the EditSet is aborted, the
 * DBEditObject is dropped.
 *
 * There is one case, however, in which a DBEditObject will be
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
 * behavior.  Such methods are labeled <B>*PSEUDOSTATIC*</B>, which
 * means that those methods are designed not to examine or report on
 * the internal state of the objectHook, but rather are meant to
 * operate based only on parameters passed into the method.  These
 * methods are PSEUDOSTATIC rather than static because if they were
 * true static methods, every place in the server where such methods
 * are called would have to use the relatively cumbersome Java
 * Reflection API rather than being able to call methods on a
 * DBEditObject instance.
 *
 * See the DBEditObject subclassing guide for more information generally on
 * DBEditObject customization.
 *
 * <b>IMPORTANT PROGRAMMING NOTE!</b>: It is critical that
 * synchronized methods in DBEditObject and in subclasses thereof do not
 * call synchronized methods in DBSession, as there is a strong possibility
 * of nested monitor deadlocking.
 *   
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public class DBEditObject extends DBObject implements ObjectStatus, FieldType {

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
  public final static int LASTOP = 12;

  public final static Date minDate = new Date(Long.MIN_VALUE);
  public final static Date maxDate = new Date(Long.MAX_VALUE);

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  /* --------------------- Instance fields and methods --------------------- */

  /**
   * Unless this DBEditObject was newly created, we'll have a reference
   * to the original DBObject which is currently registered in the DBStore.
   * Only one DBEditObject can be connected to a DBObject at a time, giving
   * us object-level locking.
   */

  protected DBObject original;

  /**
   * true if this object has had its commitPhase1() method called, but
   * has not yet had its commitPhase2() or release() methods called.
   * If commitSemaphore is true, the DBField.isEditable() method will
   * always return false for fields in this object, and no editing
   * will be allowed on this object.
   */

  private booleanSemaphore commitSemaphore = new booleanSemaphore(false);

  /**
   * true if the object is in the middle of carrying
   * out deletion logic.. consulted by subclasses
   * to determine whether they should object to fields
   * being set to null
   */

  private boolean deleting = false;	

  /**
   * tracks this object's editing status.  See
   * {@link arlut.csd.ganymede.common.ObjectStatus ObjectStatus}.
   */

  byte status;

  /**
   * true if the object has a version currently
   * stored in the DBStore
   */

  boolean stored;		

  /**
   * transaction that this object has been checked out in
   * care of.
   */

  public DBEditSet editset;	

  /* -- */

  /** 
   * Dummy constructor, is responsible for creating a DBEditObject
   * strictly for the purpose of having a handle to call our
   * pseudostatic customization methods on. 
   *
   * This is the version of the constructor that the
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}'s 
   * {@link arlut.csd.ganymede.server.DBObjectBase#createHook() createHook()}
   * method uses to create the {@link arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook}
   * object.
   **/

  public DBEditObject(DBObjectBase base)
  {
    super();

    this.objectBase = base;
    editset = null;		// this will be our cue to our static handle status for our methods
  }

  /**
   * Creation constructor, is responsible for creating a new editable
   * object with all fields listed in the
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} instantiated
   * but undefined.
   *
   * This constructor is not really intended to be overriden in subclasses.
   * Creation time field value initialization is to be handled by
   * initializeNewObject().
   *
   * @see arlut.csd.ganymede.server.DBField
   */

  public DBEditObject(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid.getNum());

    if (editset == null)
      {
	// "Null DBEditSet"
	throw new NullPointerException(ts.l("init.notrans"));
      }

    original = null;
    this.editset = editset;
    this.gSession = editset.getSession().getGSession();
    commitSemaphore.set(false);
    stored = false;
    status = CREATING;

    /* -- */

    Enumeration 
      en = null;

    DBObjectBaseField 
      fieldDef;

    DBField 
      tmp = null;

    /* -- */

    fieldAry = new DBField[objectBase.fieldTable.size()];

    synchronized (objectBase)
      {
	en = objectBase.fieldTable.elements();
	
	while (en.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) en.nextElement();

	    // check for permission to create a particular field

	    if (!checkNewField(fieldDef.getID()))
	      {
		continue;
	      }

	    switch (fieldDef.getType())
	      {
	      case BOOLEAN:
		tmp = new BooleanDBField(this, fieldDef);
		break;
		    
	      case NUMERIC:
		tmp = new NumericDBField(this, fieldDef);
		break;

	      case FLOAT:
		tmp = new FloatDBField(this, fieldDef);
		break;

	      case FIELDOPTIONS:
		tmp = new FieldOptionDBField(this, fieldDef);
		break;
		
	      case DATE:
		tmp = new DateDBField(this, fieldDef);
		break;

	      case STRING:
		tmp = new StringDBField(this, fieldDef);
		break;
		    
	      case INVID:
		tmp = new InvidDBField(this, fieldDef);
		break;

	      case PERMISSIONMATRIX:
		tmp = new PermissionMatrixDBField(this, fieldDef);
		break;

	      case PASSWORD:
		tmp = new PasswordDBField(this, fieldDef);
		break;

	      case IP:
		tmp = new IPDBField(this, fieldDef);
		break;
	      }

	    if (tmp != null)
	      {
		saveField(tmp);	// safe, we know our fieldAry was empty
	      }
	  }
      }
  }

  /**
   * Check-out constructor, used by
   * {@link arlut.csd.ganymede.server.DBObject#createShadow(arlut.csd.ganymede.server.DBEditSet) DBObject.createShadow()}
   * to pull out an object for editing.
   */

  public DBEditObject(DBObject original, DBEditSet editset)
  {
    super(original.objectBase);

    Enumeration 
      en;

    DBObjectBaseField 
      fieldDef;

    DBField 
      field, 
      tmp = null;

    /* -- */

    this.editset = editset;
    commitSemaphore.set(false);
    stored = true;
    status = EDITING;

    fieldAry = new DBField[objectBase.fieldTable.size()];

    this.gSession = getSession().getGSession();

    this.original = original;
    this.myInvid = original.myInvid;
    this.objectBase = original.objectBase;

    // clone the fields from the original object
    // since we own these, the field-modifying
    // methods on the copied fields will allow editing
    // to go forward

    Vector fieldVect = original.getFieldVect();

    for (int i = 0; i < fieldVect.size(); i++)
      {
	field = (DBField) fieldVect.elementAt(i);
	
	switch (field.getType())
	  {
	  case BOOLEAN:
	    tmp = new BooleanDBField(this, (BooleanDBField) field);
	    break;
		    
	  case NUMERIC:
	    tmp = new NumericDBField(this, (NumericDBField) field);
	    break;

	  case FLOAT:
	    tmp = new FloatDBField(this, (FloatDBField) field);
	    break;

	  case FIELDOPTIONS:
	    tmp = new FieldOptionDBField(this, (FieldOptionDBField) field);
	    break;

	  case DATE:
	    tmp = new DateDBField(this, (DateDBField) field);
	    break;

	  case STRING:
	    tmp = new StringDBField(this, (StringDBField) field);
	    break;
		    
	  case INVID:
	    tmp = new InvidDBField(this, (InvidDBField) field);
	    break;

	  case PERMISSIONMATRIX:
	    tmp = new PermissionMatrixDBField(this, (PermissionMatrixDBField) field);
	    break;

	  case PASSWORD:
	    tmp = new PasswordDBField(this, (PasswordDBField) field);
	    break;

	  case IP:
	    tmp = new IPDBField(this, (IPDBField) field);
	    break;
	  }

	if (tmp != null)
	  {
	    saveField(tmp);	// safe, we know our fieldAry was empty
	  }
      }
	
    // now create slots for any fields that are in this object type's
    // DBObjectBase, but which were not present in the original
    
    synchronized (objectBase)
      {
	en = objectBase.fieldTable.elements();
	
	while (en.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) en.nextElement();
	    
	    // if we don't have it in our fieldAry already,
	    // we'll want to add it

	    if (retrieveField(fieldDef.getID()) == null)
	      {
		if (!checkNewField(fieldDef.getID()))
		  {
		    continue;
		  }
		
		switch (fieldDef.getType())
		  {
		  case BOOLEAN:
		    tmp = new BooleanDBField(this, fieldDef);
		    break;
		    
		  case NUMERIC:
		    tmp = new NumericDBField(this, fieldDef);
		    break;

		  case FLOAT:
		    tmp = new FloatDBField(this, fieldDef);
		    break;

		  case FIELDOPTIONS:
		    tmp = new FieldOptionDBField(this, fieldDef);
		    break;
		
		  case DATE:
		    tmp = new DateDBField(this, fieldDef);
		    break;

		  case STRING:
		    tmp = new StringDBField(this, fieldDef);
		    break;
		    
		  case INVID:
		    tmp = new InvidDBField(this, fieldDef);
		    break;

		  case PERMISSIONMATRIX:
		    tmp = new PermissionMatrixDBField(this, fieldDef);
		    break;

		  case PASSWORD:
		    tmp = new PasswordDBField(this, fieldDef);
		    break;

		  case IP:
		    tmp = new IPDBField(this, fieldDef);
		    break;

		  }

		saveField(tmp);	// safe, we know we didn't already have a field with the same id
	      }
	  }
      }
  }

  /**
   * Returns the DBSession that this object is checked out in
   * care of.
   *
   * @see arlut.csd.ganymede.server.DBSession
   */

  public final DBSession getSession()
  {
    // to handle use of this method in DBEditObject objectHook
    // subclass usage without throwing a NullPointerException

    if (editset == null)
      {
	return null;
      }

    return editset.getSession();
  }

  /**
   * Returns the transaction object owning this object, or
   * null if an unowned data object.
   *
   * Note that this is public, but not made available
   * to the client via a remote interface.
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

  public final DBObject getOriginal()
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
   * Returns the primary label of this object.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final String getLabel()
  {
    if (getStatus() == DELETING)
      {
	return getOriginal().getLabel();
      }

    return super.getLabel();
  }

  /**
   * If this object type is embedded, this method will return the
   * desired display label for the embedded object.
   *
   * This label may not be the same as returned by getLabel(), which
   * is guaranteed to be derived from a namespace constrained label
   * field, suitable for use in the XML context.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final String getEmbeddedObjectDisplayLabel()
  {
    if (getStatus() == DELETING)
      {
	return getOriginal().getEmbeddedObjectDisplayLabel();
      }

    return super.getEmbeddedObjectDisplayLabel();
  }

  /**
   * If this DBEditObject is managing an embedded object, the
   * getEmbeddedObjectLabel() can be overridden to display a synthetic
   * label in the context of viewing or editing the containing object,
   * and when doing queries on the containing type.
   *
   * The getLabel() method will not consult this hook, however, and
   * embedded objects will be represented with their unique label
   * field when processed in an XML context.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public String getEmbeddedObjectDisplayLabelHook(DBObject object)
  {
    return object.getLabel();
  }

  /**
   * This method is used to make sure that the built-in fields that
   * the server assumes will always be present in any editable object
   * will be in place.
   * 
   * This method checks with instantiateNewField() if the field id is
   * not one of those that is needfull.  If instantiateNewField() approves
   * the creation of a new field, checkNewField() will check to see if
   * the {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * permissions permit the field creation.
   */

  public final boolean checkNewField(short fieldID)
  {
    if (fieldID <= SchemaConstants.FinalSystemField)
      {
	return true;		// we always allow the built in fields
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
   * Shortcut method to set a field's value.  Using this
   * method can save the client a roundtrip to the server.
   *
   * This method cannot be used on permission fields or password
   * fields.
   *
   * @see arlut.csd.ganymede.rmi.db_object 
   */

  public final ReturnVal setFieldValue(short fieldID, Object value) throws GanyPermissionsException
  { 
    DBField field = retrieveField(fieldID);

    /* -- */

    if (field != null)
      {
	return field.setValue(value);
      }

    // "DBEditObject.setFieldValue() error"
    // "DBEditObject.setFieldValue() couldn''t find field {0} in object {1}"

    return Ganymede.createErrorDialog(ts.l("setFieldValue.error"),
				      ts.l("setFieldValue.errorTxt", new Short(fieldID), getLabel()));
  }

  /**
   * Shortcut method to set a field's value.  This version bypasses
   * permission checking and is only intended for server-side
   * use.
   *
   * This method cannot be used on permission fields or password
   * fields.
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

    return Ganymede.createErrorDialog(ts.l("setFieldValueLocal.error"),
				      ts.l("setFieldValueLocal.errorTxt", new Short(fieldID), getLabel()));
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
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.
   *
   * This version of anonymousLinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link based on what field of what object wants to link
   * to it.
   *
   * By default, the 3 variants of the DBEditObject anonymousLinkOK() 
   * method are chained together, so that the customizer can choose
   * which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.server.InvidDBField#bind(arlut.csd.ganymede.common.Invid,arlut.csd.ganymede.common.Invid,boolean) bind()}
   * method calls this version.  This version calls the three parameter
   * version, which calls the two parameter version, which returns
   * false by default.  Customizers can implement any of the three
   * versions, but unless you maintain the version chaining yourself,
   * there's no point to implementing more than one of them.
   *
   * Note that the {@link arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.
   *
   * The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   * @param sourceObject The object on the other side of the proposed link
   * @param sourceFieldID  The field on the other side of the proposed link
   * @param gsession Who is trying to do this linking?  */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
				 DBObject sourceObject, short sourceFieldID,
				 GanymedeSession gsession)
  {
    // by default, dispatch to the more generic approval method
    
    return anonymousLinkOK(targetObject, targetFieldID, gsession);
  }

  /**
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.
   *
   * This version of anonymousUnlinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link to be rescinded based on what field of what
   * object wants to unlink from it.
   *
   * By default, the 3 variants of the DBEditObject anonymousUnlinkOK() 
   * method are chained together, so that the customizer can choose
   * which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.server.InvidDBField#unbind(arlut.csd.ganymede.common.Invid,boolean) unbind()}
   * method calls this version.  This version calls the three parameter
   * version, which calls the two parameter version, which returns
   * true by default.  Customizers can implement any of the three
   * versions, but unless you maintain the version chaining yourself,
   * there's no point to implementing more than one of them.
   *
   * <b>*PSEUDOSTATIC*</b>
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
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.
   *
   * See {@link arlut.csd.ganymede.server.DBEditObject#anonymousLinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousLinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousLinkOK() method chaining.
   *
   * Note that the {@link arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.
   *
   * The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   * @param gsession Who is trying to do this linking?  */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID, GanymedeSession gsession)
  {
    return anonymousLinkOK(targetObject, targetFieldID);
  }

  /**
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.
   *
   * By default, the server always allows anonymous unlinking.
   * Overriding this method is only required when you want to DISallow
   * such unlinking.
   *
   * See {@link arlut.csd.ganymede.server.DBEditObject#anonymousUnlinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousUnlinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousUnlinkOK() method chaining.
   *
   * <b>*PSEUDOSTATIC*</b>
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
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.
   *
   * See {@link arlut.csd.ganymede.server.DBEditObject#anonymousLinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousLinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousLinkOK() method chaining.
   *
   * Note that the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.
   *
   * The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID)
  {
    return false;		// by default, permission is denied
  }

  /**
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.
   *
   * By default, the server always allows anonymous unlinking.
   * Overriding this method is only required when you want to DISallow
   * such unlinking.
   *
   * See {@link 
   * arlut.csd.ganymede.server.DBEditObject#anonymousUnlinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousUnlinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousUnlinkOK() method chaining.
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID)
  {
    return true;		// by default, permission is granted to unlink
  }

  /**
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given object, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.
   *
   * Note as well that this permOverride() has no effect when
   * creating new objects of this type. Take a look at overriding
   * {@link arlut.csd.ganymede.server.DBEditObject#canCreate(arlut.csd.ganymede.rmi.Session) canCreate()}
   * if you need to provide an exception to the normal permissions
   * system for creating new objects.
   *
   * This method should be used very sparingly.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   * Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given object.
   *
   * Note as well that this permExpand() has no effect when
   * creating new objects of this type. Take a look at overriding
   * {@link arlut.csd.ganymede.server.DBEditObject#canCreate(arlut.csd.ganymede.rmi.Session) canCreate()}
   * if you need to provide an exception to the normal permissions
   * system for creating new objects.
   *
   * This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant the
   * permissions specified by this method for access to the given
   * field, and no further elaboration of the permission will be
   * performed.  If permOverride() returns a non-null value for a
   * given field, permExpand() will not be consulted for that field.
   * Just as with permExpand(), this method can never cause greater
   * permissions to be granted to a field than is available to the
   * object as a whole, and this override capability does not
   * apply to operations performed in supergash mode.
   *
   * This method should be used very sparingly.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b> 
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   * Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant the
   * union of the permissions returned by this method and those
   * determined normally by GanymedeSession's default field
   * permissions logic.  This method can never cause greater
   * permissions to be granted to a field than is available to the
   * object as a whole, and the results of permExpand() will have
   * no effect on operations performed in supergash mode.
   *
   * This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b> 
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   * Customization method to verify overall consistency of
   * a DBObject.  This method is intended to be overridden
   * in DBEditObject subclasses, and will be called by
   * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase1() commitPhase1()}
   * to verify the readiness of this object for commit.  The
   * DBObject passed to this method will be a DBEditObject,
   * complete with that object's GanymedeSession reference
   * if this method is called during transaction commit, and
   * that session reference may be used by the verifying code if
   * the code needs to access the database.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
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
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.
   *
   * Note as well that the designated label field for objects are
   * always required, whatever this method returns, and that this
   * requirement holds without regard to the GanymedeSession's
   * enableOversight value.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    return false;
  }

  /**
   * Customization method to verify whether the user has permission
   * to view a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for viewing to the client.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canRead(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of 
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // by default, return the field definition's visibility

    if (field.getFieldDef().base != this.objectBase)
      {
	// "field/object mismatch"
	throw new IllegalArgumentException(ts.l("canSeeField.mismatch"));
      }

    return field.getFieldDef().isVisible(); 
  }

  /**
   * Customization method to verify whether the user has permission
   * to edit a given object.  The client's 
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for editing by the client.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canWrite(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canBeInactivated()
  {
    return false;
  }

  /**
   * Customization method to verify whether the user has permission
   * to inactivate a given object.  The client's 
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.
   *
   * Note that unlike canRemove(), canInactivate() takes a
   * DBEditObject instead of a DBObject.  This is because inactivating
   * an object is based on editing the object, and so we have the
   * GanymedeSession/DBSession classes go ahead and check the object
   * out for editing before calling us.  This serves to force the
   * session classes to check for write permission before attempting
   * inactivation.
   *
   * Use canBeInactivated() to test for the presence of an inactivation
   * protocol outside of an edit context if needed.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return false;
  }

  /**
   * Customization method to verify whether the user has permission
   * to remove a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for removal by the client.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public ReturnVal canRemove(DBSession session, DBObject object)
  {
    // our default behavior is that objects that can be inactivated
    // should not be deleted except by supergash

    if (this.canBeInactivated())
      {
	GanymedeSession myMaster = session.getGSession();

	/* -- */
	
	if (myMaster == null)
	  {
	    // hm, not an end-user.. let it go
	    
	    return null;
	  }

	// only supergash can delete users.. everyone else can only
	// inactivate.
	
	if (myMaster.isSuperGash())
	  {
	    return null;
	  }

	return new ReturnVal(false);
      }
    
    return null;
  }

  /**
   * Customization method to verify whether the user has permission
   * to clone a given object.  The client's {@link
   * arlut.csd.ganymede.server.GanymedeSession} object will call this
   * per-class method to do an object type- sensitive check to see if
   * this object feels like being available for cloning by the
   * client.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canClone(DBSession session, DBObject object)
  {
    return !object.isEmbedded();
  }

  /**
   * Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canCloneField(DBSession session, DBObject object, DBField field)
  {
    // don't clone the built-in fields, but do clone
    // anything else

    return field.getID() > SchemaConstants.FinalSystemField;
  }


  /**
   * Customization method to verify whether the user has permission
   * to create an instance of this object type.  The client's 
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object
   * will call the canCreate method in the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} for this object type
   * to determine whether creation is allowed to the user.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canCreate(Session session)
  {
    if (session != null && (session instanceof GanymedeSession))
      {
	GanymedeSession gSession;

	/* -- */

	gSession = (GanymedeSession) session;

	return gSession.getPerm(getTypeID(), true).isCreatable(); // *sync* GanymedeSession
      }

    // note that we are going ahead and returning false here, as
    // we assume that the client will always use the local BaseDump
    // copy and won't generally call us remotely with a remote
    // interface.

    return false;
  }

  /**
   * Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    return false;
  }

  /**
   * This method provides a hook to allow custom DBEditObject subclasses to
   * indicate that the given object is interested in receiving notification
   * when changes involving it occur, and can provide one or more addresses for
   * such notification to go to.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean hasEmailTarget(DBObject object)
  {
    return false;
  }

  /**
   * This method provides a hook to allow custom DBEditObject subclasses to
   * return a Vector of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.
   *
   * If no email targets are present in this object, either a null value
   * or an empty Vector may be returned.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public Vector getEmailTargets(DBObject object)
  {
    return null;
  }

  /**
   * This method provides a hook that can be used by subclasses of
   * DBEditObject to return a list of attribute names and attribute
   * values to include when writing out &lt;invid&gt; elements to a
   * sync channel during transactional sync'ing.
   *
   * The point of this is to allow DBEditObject subclasses to
   * inject additional data into a transactional sync record so that
   * external sync channel service code can have enough information to
   * identify a relationship that was made or broken within the
   * tranaction.
   *
   * The array returned should have an even number of values.  The
   * first value should be an attribute name, the second should be
   * the value for that attribute name, the third should be another
   * attribute name, and etc.
   *
   * It is an error to return an attribute name that conflicts with
   * the set pre-defined for use with the &lt;invid&gt; XML element.
   * This includes <code>type</code>, <code>num</code>, <code>id</code>,
   * and <code>oid</code>.
   *
   * <b>*PSEUDOSTATIC*</b>
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
   * This method provides a hook that can be used by subclasses of
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
   * elements that point from objects of this kind.
   *
   * The point of this is to allow DBEditObject subclasses to
   * inject additional data into a transactional sync record so that
   * external sync channel service code can have enough information to
   * identify a relationship that was made or broken within the
   * tranaction.  For instance, if you are synchronizing to an LDAP
   * structure with the XML Sync Channel mechanism, you might want all
   * of your &lt;invid&gt; elements that point to Users to include a
   * dn attribute that provides the fully qualified LDAP DN for the
   * User object.
   *
   * The array returned should have an even number of values.  The
   * first value should be an attribute name, the second should be
   * the value for that attribute name, the third should be another
   * attribute name, and etc.
   *
   * It is an error to return an attribute name that conflicts with
   * the set pre-defined for use with the &lt;invid&gt; XML element.
   * This includes <code>type</code>, <code>num</code>, <code>id</code>,
   * and <code>oid</code>.
   *
   * <b>*PSEUDOSTATIC*</b>
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
   * Initializes a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.  If this DBEditObject is an embedded type, it will
   * have been linked into its parent object before this method
   * is called.
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the 
   * {@link arlut.csd.ganymede.server.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.server.DBSession#createDBObject(short, arlut.csd.ganymede.common.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure code, the
   * calling method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.
   *
   * If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.
   *
   * This method should be overridden in subclasses. 
   */

  public ReturnVal initializeNewObject()
  {
    return null;
  }

  /**
   * This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.
   *
   * This method does not affect those fields which are actually present
   * in the object's record in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.  What this method allows
   * you to do is have a subclass decide whether it wants to instantiate
   * a potential field (one that is declared in the field dictionary for
   * this object, but which doesn't happen to be presently defined in
   * this object) in this particular object.
   *
   * A concrete example will help here.  The Permissions Object type
   * (base number SchemaConstants.PermBase) holds a permission
   * matrix, a descriptive title, and a list of admin personae that hold
   * those permissions for objects they own.
   *
   * There are a few specific instances of SchemaConstants.PermBase
   * that don't properly need the list of admin personae, as their
   * object invids are hard-coded into the Ganymede security system, and
   * their permission matrices are automatically consulted in certain
   * situations.  In order to support this, we're going to want to have
   * a DBEditObject subclass for managing permission objects.  In that
   * subclass, we'll define instantiateNewField() so that it will return
   * false if the fieldID corresponds to the admin personae list if the
   * object's ID is that of one of these special objects.  As a result,
   * when the objects are viewed by an administrator, the admin personae
   * list will not be seen.
   */

  public boolean instantiateNewField(short fieldID)
  {
    return gSession.getPerm(getTypeID(), fieldID, true).isCreatable(); // *sync* GanymedeSession
  }

  /**
   * Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.
   *
   * This method is called on a newly created object, in order
   * to clone the state of origObj into it.  This method does not actually
   * create a new object.. that is handled by
   * {@link arlut.csd.ganymede.server.GanymedeSession#clone_db_object(arlut.csd.ganymede.common.Invid) clone_db_object()}
   * before this method is called on the newly created object.
   *
   * The default (DBEditObject) implementation of this method will only clone
   * fields for which
   * {@link arlut.csd.ganymede.server.DBEditObject#canCloneField(arlut.csd.ganymede.server.DBSession,
   * arlut.csd.ganymede.server.DBObject, arlut.csd.ganymede.server.DBField) canCloneField()}
   * returns true, and which are not connected to a namespace (and thus could not
   * possibly be cloned).
   *
   * If one or more fields in the original object are unreadable by the cloning
   * session, we will provide a list of fields that could not be cloned due to
   * a lack of read permissions in a dialog in the ReturnVal.  Such a problem will
   * not result in a failure code being returned, however.. the clone will succeed,
   * but an informative dialog will be provided to the user.
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
    Vector origFields;
    DBField origField;
    DBField newField;
    boolean problem = false;
    StringBuffer resultBuf = new StringBuffer();

    /* -- */

    if ((origObj.getTypeID() != getTypeID()))
      {
	// "Clone Error"
	// "Can''t clone an object of the wrong type.  This is an internal error."
	return Ganymede.createErrorDialog(ts.l("cloneFromObject.error"), 
					  ts.l("cloneFromObject.typeError"));
      }

    origFields = origObj.getFieldVector(true); // don't clone system fields

    for (int i = 0; i < origFields.size(); i++)
      {
	origField = (DBField) origFields.elementAt(i);

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
   * This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.server.GanymediatorWizard wizards} when a field's
   * value is being changed.
   *
   * Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.
   *
   * In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.
   *
   * If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean) 
   * enableOversight(false)},
   * this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.
   *
   * This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.
   *
   * This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.
   *
   * The operation parameter will be a small integer, and should hold one of the
   * following values:
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
   * <dt>11 - SETPASSSSHA</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenLDAP-style SSHA password hash.  param1
   * will be the password in SSHA form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * </dl>
   *
   * @return a ReturnVal object indicated success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing flag
   * that will indicate to the field code whether or not the operation
   * should continue to completion using the field's standard logic.
   * <b>It is very important that wizardHook return a new ReturnVal(true, true)
   * if the wizardHook wishes to simply specify rescan information while
   * having the field perform its standard operation.</b>  wizardHook() may
   * return new ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true for the
   * respond() method in GanymediatorWizard subclasses.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    return null;		// by default, we just ok whatever
  }

  /**
   * Hook to have this object create a new embedded object
   * in the given field.
   *
   * This method now has the appropriate default logic for creating
   * embedded objects with the user's permissions, but this method may
   * still be overridden to do customization, if needed.
   *
   * XXX: It is essential that createNewEmbeddedObject() use
   * GanymedeSession.create_db_object() in order to check permissions
   * and to export the created embedded object for the client's use, if
   * necessary, whatever other customizations a subclass might choose to
   * do.  We may want to make this method final.
   */

  public ReturnVal createNewEmbeddedObject(InvidDBField field) throws NotLoggedInException
  {    
    DBEditObject newObject;
    DBObjectBaseField fieldDef;
    ReturnVal retVal;

    /* -- */

    fieldDef = field.getFieldDef();

    if (!fieldDef.isEditInPlace())
      {
	// "Error in server, DBEditObject.createNewEmbeddedObject() called on non-embedded object"
	throw new RuntimeException(ts.l("createNewEmbeddedObject.embeddedError"));
      }
	
    if (fieldDef.getTargetBase() > -1)
      {
	if (getGSession() != null)
	  {
	    // we use GanymedeSession to check permissions to create the target.

	    retVal = getGSession().create_db_object(fieldDef.getTargetBase(), true);
	
	    if (retVal == null)
	      {
		// "DBEditObject.createNewEmbeddedObject could not get a userful result from create_db_object"
		return Ganymede.createErrorDialog(ts.l("global.serverError"),
						  ts.l("createNewEmbeddedObject.badCreate"));
	      }
	
	    if (!retVal.didSucceed())
	      {
		return retVal;
	      }

	    newObject = (DBEditObject) retVal.getObject();

	    retVal.setInvid(newObject.getInvid());

	    return retVal;
	  }
	else
	  {
	    // "Error in custom code on server.  createNewEmbeddedObject() called without a valid GanymedeSession"
	    return Ganymede.createErrorDialog(ts.l("global.serverError"),
					      ts.l("createNewEmbeddedObject.badSession"));
	  }
      }
    else
      {
	// "Error in custom code on server.  createNewEmbeddedObject() called without a valid target"
	return Ganymede.createErrorDialog(ts.l("global.serverError"),
					  ts.l("createNewEmbeddedObject.badTarget"));
      }
  }

  /**
   * This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.
   *
   * Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?
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
   * This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the {@link arlut.csd.ganymede.server.DBField DBField}
   * that called us is guaranteed to proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.
   *
   * The &lt;field&gt; parameter identifies the field that is requesting
   * approval for item deletion, and the &lt;index&gt; parameter identifies
   * the element number that is to be deleted.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.
   */

  public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    return null;
  }

  /**
   * This method allows the DBEditObject to have executive approval
   * of any vector-vector removal operation, and to take any special
   * actions in reaction to the removal.. if this method returns null
   * or a success code in its ReturnVal, the DBField that called us is
   * guaranteed to proceed to make the change to its vector.  If this
   * method returns a non-success code in its ReturnVal, the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.
   *
   * The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the
   * &lt;valuesToDelete&gt; parameter contains a list of elements to
   * be deleted.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under standard circumstances, we won't need
   * to do anything here.
   */

  public ReturnVal finalizeDeleteElements(DBField field, Vector valuesToDelete)
  {
    return null;
  }

  /**
   * This method allows the DBEditObject to have executive approval
   * of any vector add operation, and to take any special actions in
   * reaction to the add.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us is guaranteed
   * to proceed to make the change to its vector.  If this method
   * returns a non-success code in its ReturnVal, the DBField that
   * called us will not make the change, and the field will be left
   * unchanged.
   *
   * The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the &lt;value&gt;
   * parameter carries the value to be added.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.
   */

  public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    return null;
  }

  /**
   * This method allows the DBEditObject to have executive approval
   * of any vector-vector add operation, and to take any special
   * actions in reaction to the add.. if this method returns null or a
   * success code in its ReturnVal, the DBField that called us is
   * guaranteed to proceed to make the change to its vector.  If this
   * method returns a non-success code in its ReturnVal, the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.
   *
   * The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the &lt;submittedValues&gt;
   * parameter carries the values to be added.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.
   */

  public ReturnVal finalizeAddElements(DBField field, Vector submittedValues)
  {
    return null;
  }

  /**
   * This method allows the DBEditObject to have executive approval
   * of any vector set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us is guaranteed
   * to proceed to make the change to its vector.  If this method
   * returns a non-success code in its ReturnVal, the DBField that
   * called us will not make the change, and the field will be left
   * unchanged.
   *
   * The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, the &lt;index&gt;
   * parameter identifies which element in the given vector field is
   * to be set, and the &lt;value&gt; parameter carries the proposed
   * new value.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.
   */

  public ReturnVal finalizeSetElement(DBField field, int index, Object value)
  {
    return null;
  }

  /**
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions in
   * reaction to the set.  When a scalar field has its value set, it
   * will call its owners finalizeSetValue() method, passing itself as
   * the &lt;field&gt; parameter, and passing the new value to be
   * approved as the &lt;value&gt; parameter.  A Ganymede customizer
   * who creates custom subclasses of the DBEditObject class can
   * override the finalizeSetValue() method and write his own logic
   * to examine any change and either approve or reject the change.
   *
   * A custom finalizeSetValue() method will typically need to
   * examine the field parameter to see which field is being changed,
   * and then do the appropriate checking based on the value
   * parameter.  The finalizeSetValue() method can call the normal
   * this.getFieldValueLocal() type calls to examine the current state
   * of the object, if such information is necessary to make
   * appropriate decisions.
   *
   * If finalizeSetValue() returns null or a ReturnVal object with
   * a positive success value, the DBField that called us is
   * guaranteed to proceed to make the change to its value.  If this
   * method returns a non-success code in its ReturnVal, as with the
   * result of a call to Ganymede.createErrorDialog(), the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.  Any error dialog returned from finalizeSetValue()
   * will be passed to the user.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including a call to our own
   * verifyNewValue() method before calling this method.  Under normal
   * circumstances, we won't need to do anything here.
   * finalizeSetValue() is useful when you need to do unusually
   * involved checks, and for when you want a chance to trigger other
   * changes in response to a particular field's value being
   * changed.
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    return null;
  }

  /**
   * This method returns true if field1 should not show any choices
   * that are currently selected in field2, where both field1 and
   * field2 are fields in this object.
   * 
   * The purpose of this method is to allow mutual exclusion between
   * a pair of fields with mandatory choices.
   *
   * To be overridden in DBEditObject subclasses.
   */

  public boolean excludeSelected(db_field field1, db_field field2)
  {
    return false;
  }

  /**
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.
   *
   * If there is no caching key, this method will return null.
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

	return new Short(baseId);
      }

    return null;
  }

  /**
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   *
   * This method will provide a reasonable default for targetted
   * invid fields, filtered by the GanymedeSession's
   * visibilityFilterInvids list.
   *
   * NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.isEditable() && (field instanceof InvidDBField) && 
	!field.isEditInPlace())
      {
	DBObjectBaseField fieldDef;
	short baseId;

	/* -- */

	fieldDef = field.getFieldDef();
	
	baseId = fieldDef.getTargetBase();

	if (baseId < 0)
	  {
	    //	    Ganymede.debug("DBEditObject: Returning null 2 for choiceList for field: " + field.getName());
	    return null;
	  }

	// and we want to return a list of choices.. can use the regular
	// query output here

	QueryNode root;

	// if we are pointing to objects of our own type, we don't want ourselves to be
	// a valid choice by default.. (DBEditObject subclasses can override this, of course)

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

	return editset.getSession().getGSession().query(myQuery, this);
      }
    
    //    Ganymede.debug("DBEditObject: Returning null for choiceList for field: " + field.getName());

    return null;
  }

  /**
   * This method is used to tell the client whether the list of options it gets
   * back for a field can be taken out of the cache.  If this method returns
   * true, that means that some of the results that obtainChoiceList() will
   * return will include items that normally wouldn't be availble to the
   * client, but are in this case because of the anonymousLinkOK() results.
   *
   * This is kind of wacked-out random stuff.  It's basically a way of
   * allowing DBEditObject to get involved in the decision as to whether an
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s 
   * {@link arlut.csd.ganymede.server.InvidDBField#choicesKey() choicesKey()}'s
   * method should disallow client-side caching for the field's choice list.
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
						      this.gSession);
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
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
   * This method provides a hook that a DBEditObject subclass
   * can use to determine whether it is permissible to enter
   * IPv6 address in a particular (IP) DBField.
   */

  public boolean isIPv6OK(DBField field)
  {
    return false;
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given 
   * {@link arlut.csd.ganymede.server.DateDBField DateDBField} has a restricted
   * range of possibilities.
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
   * This method is used to specify the earliest acceptable date
   * for the specified {@link arlut.csd.ganymede.server.DateDBField DateDBField}.
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
   * This method is used to specify the latest acceptable date
   * for the specified {@link arlut.csd.ganymede.server.DateDBField DateDBField}.
   */

  public Date maxDate(DBField field)
  {
    return new Date(Long.MAX_VALUE);
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}
   * has a restricted range of possibilities.
   */

  public boolean isIntLimited(DBField field)
  {
    return false;
  }

  /**
   * This method is used to specify the minimum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}.
   */

  public int minInt(DBField field)
  {
    return Integer.MIN_VALUE;
  }

  /**
   * This method is used to specify the maximum acceptable value
   * for the specified    
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}.
   */

  public int maxInt(DBField field)
  {
    return Integer.MAX_VALUE;
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.server.FloatDBField FloatDBField}
   * has a restricted range of possibilities.
   */
  
  public boolean isFloatLimited(DBField field)
  {
    return false;
  }
  
  /**
   * This method is used to specify the minimum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.FloatDBField FloatDBField}.
   */
  
  public double minFloat(DBField field)
  {
    return Double.MIN_VALUE;
  }
 
  /**
   * This method is used to specify the maximum acceptable value
   * for the specified    
   * {@link arlut.csd.ganymede.server.FloatDBField FloatDBField}.
   */
  
  public double maxFloat(DBField field)
  {
    return Double.MAX_VALUE;
  }
  
  /**
   * This method handles inactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * inactivate() method can then be called on the object to put the
   * object into inactive mode.  inactivate() will set the object's
   * removal date and fix up any other state information to reflect
   * the object's inactive status.
   *
   * inactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to inactive the object.
   *
   * The inactive() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.
   *
   * If inactivate() returns a ReturnVal that has its success flag set
   * to false and does not include a JDialogBuff for further
   * interaction with the user, then DBSEssion.inactivateDBObject()
   * method will rollback any changes made by this method.
   *
   * If inactivate() returns a success value, we expect that the object
   * will have a removal date set.
   *
   * IMPORTANT NOTE 1: This method is intended to be called by the
   * DBSession.inactivateDBObject() method, which establishes a
   * checkpoint before calling inactivate.  If this method is not
   * called by DBSession.inactivateDBObject(), you need to push
   * a checkpoint with the key 'inactivate'+label, where label is
   * the returned name of this object.
   *
   * IMPORTANT NOTE 2: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object inactivation when
   * the transaction is committed..
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public ReturnVal inactivate()
  {
    // "DBEditObject.inactivate() Error"
    // "This object type has not been configured to allow inactivation."
    return Ganymede.createErrorDialog(ts.l("inactivate.error"),
				      ts.l("inactivate.errorTxt"));
  }

  /**
   * This method is to be called by the custom DBEditObject inactivate()
   * logic when the inactivation is performed so that logging can be
   * done.
   *
   * If inactivation of an object causes the label to be null, this
   * won't work as well as we'd really like.
   */

  public final void finalizeInactivate(boolean success)
  {
    if (success)
      {
	Object val = getFieldValueLocal(SchemaConstants.RemovalField);

	if (val != null)
	  {
	    Vector invids = new Vector();
	    
	    invids.addElement(this.getInvid());
	
	    StringBuffer buffer = new StringBuffer();

	    // "{0} {1} has been inactivated.\n\nThe object is due to be removed from the database at {2}.\n\n"
	    buffer.append(ts.l("finalizeInactivate.removeSet", getTypeName(), getLabel(), getFieldValueLocal(SchemaConstants.RemovalField).toString()));
	
	    editset.logEvent(new DBLogEvent("inactivateobject",
					    buffer.toString(),
					    (gSession.personaInvid == null ?
					     gSession.userInvid : gSession.personaInvid),
					    gSession.getMyUserName(),
					    invids,
					    getEmailTargets(this)));
	  }
	else
	  {
	    Vector invids = new Vector();
	    
	    invids.addElement(this.getInvid());
	
	    StringBuffer buffer = new StringBuffer();

	    // "{0} {1} has been inactivated.\n\nThe object has no removal date set.\n\n"
	    buffer.append(ts.l("finalizeInactivate.noRemove", getTypeName(), getLabel()));

	    editset.logEvent(new DBLogEvent("inactivateobject",
					    buffer.toString(),
					    (gSession.personaInvid == null ?
					     gSession.userInvid : gSession.personaInvid),
					    gSession.getMyUserName(),
					    invids,
					    getEmailTargets(this)));
	  }

	editset.popCheckpoint("inactivate" + getLabel());
      }
    else
      {
	editset.rollback("inactivate" + getLabel());
      }
  }

  /**
   * This method handles reactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * reactivate() method can then be called on the object to make the
   * object active again.  reactivate() will clear the object's
   * removal date and fix up any other state information to reflect
   * the object's reactive status.
   *
   * reactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to reactive the object.
   *
   * If reactivate() returns a ReturnVal that has its success flag set to false
   * and does not include a {@link arlut.csd.JDialog.JDialogBuff JDialogBuff}
   * for further interaction with the
   * user, then 
   * {@link arlut.csd.ganymede.server.DBSession#inactivateDBObject(arlut.csd.ganymede.server.DBEditObject) inactivateDBObject()}
   * method will rollback any changes made by this method.
   *
   * IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object reactivation when
   * the transaction is committed..
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public ReturnVal reactivate()
  {
    if (isInactivated())
      {
	// by default, we'll just clear the removal field

	setFieldValueLocal(SchemaConstants.RemovalField, null);
	return null;		// success
      }

    // "DBEditObject.reactivate() error"
    // "I can''t reactivate this object.  It was not inactivated."

    return Ganymede.createErrorDialog(ts.l("reactivate.error"),
				      ts.l("reactivate.errorTxt"));
  }

  /**
   * This method is to be called by the custom DBEditObject reactivate()
   * logic when the reactivation is performed so that logging can be
   * done.
   */

  final protected void finalizeReactivate(boolean success)
  {
    if (success)
      {
	Vector invids = new Vector();

	invids.addElement(this.getInvid());

	StringBuffer buffer = new StringBuffer();

	// "{0} {1} has been reactivated.\n\n"

	buffer.append(ts.l("finalizeReactivate.message", getTypeName(), getLabel()));

	editset.logEvent(new DBLogEvent("reactivateobject",
					buffer.toString(),
					(gSession.personaInvid == null ?
					 gSession.userInvid : gSession.personaInvid),
					gSession.getMyUserName(),
					invids,
					getEmailTargets(this)));
      }
    else
      {
	editset.rollback("reactivate" + getLabel()); // see DBSession.reactivateDBObject() for checkpoint
      }
  }

  /**
   * This method handles removal logic for this object type.  This method
   * will be called immediately from DBSession.deleteDBObject().
   *
   * The remove() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.
   *
   * If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, the DBSession.deleteDBObject() method will roll back any changes
   * made by this method.
   *
   * remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.
   *
   * IMPORTANT NOTE: If a custom object's remove() logic decides to
   * enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeRemove() on the object when
   * it is determined that the object really should be removed,
   * with a boolean indicating whether success was had.
   */

  public ReturnVal remove()
  {
    return null;
  }

  /**
   * This method handles Ganymede-internal deletion logic for this
   * object type.  finalizeRemove() is responsible for dissolving any
   * invid inter-object references in particular.
   *
   * It is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object removal when
   * the transaction is committed..
   *
   * finalizeremove() returns a ReturnVal indicating whether the
   * internal removal bookkeeping was successful.  A failure result
   * will cause the DBSession to rollback the transaction to the state
   * prior to any removal actions for this object were
   * attempted.
   *
   * remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.
   *
   * @param success If true, finalizeRemove() will clear all fields,
   * thereby unlinking invid fields and relinquishing namespace claims.
   * If false, finalizeRemove() will rollback to the state the system
   * was in before DBSession.deleteDBObject() was entered.
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public final synchronized ReturnVal finalizeRemove(boolean success)
  {
    ReturnVal finalResult = new ReturnVal(true); // we use this to track rescan requests
    ReturnVal retVal = null;
    DBField field;
    String label = getLabel();	// remember the label before we clear it

    /* -- */

    if (!success)
      {
	editset.rollback("del" + label); // *sync*

	// "Object Removal Error"
	// "Could not delete object {0}.  Custom code in the server rejected this operation."

	return Ganymede.createErrorDialog(ts.l("finalizeRemove.error"),
					  ts.l("finalizeRemove.errorTxt", label));
      }

    if (debug)
      {
	System.err.println("++ Attempting to delete object " + label);

	if (isEmbedded())
	  {
	    InvidDBField invf = (InvidDBField) getField(SchemaConstants.ContainerField);

	    if (invf == null)
	      {
		System.err.println("++ Argh, no container field in embedded!");
	      }
	    else
	      {
		System.err.println("++ We are embedded in object " + invf.getValueString());
	      }
	  }
      }

    // we want to delete / null out all fields.. this will take care
    // of invid links, embedded objects, and namespace allocations.
    
    // set the deleting flag to true so that our subclasses won't
    // freak about values being set to null.

    // NOTE: notice that we don't log a DBLogEvent for the object's
    // deletion anywhere in this method, as is done similarly in
    // finalizeInactivate() and finalizeReactivate().  Logging for
    // object removal, like that for object creation and editing, is
    // done in DBEditSet.commit().  We have to take care of logging
    // for inactivation and reactivation in finalizeInactivate() and
    // finalizeReactivate() because otherwise we have no way of
    // determining that we inactivated or reactivated an object in the
    // context of the DBEditSet.commit() method.

    this.deleting = true;

    try				// finally {this.deleting = false;}
      {
	// first we need to take care of any back links. This scans
	// this object for asymmetric fields, checks objects pointed
	// to by us out for editing, and takes this object's invid
	// out of all fields in those objects.

	retVal = attemptBackLinkClear(true);
    
	if (retVal != null && !retVal.didSucceed())
	  {
	    editset.rollback("del" + label); // *sync*

	    return retVal;
	  }
	else if (retVal != null)
	  {
	    finalResult.unionRescan(retVal);
	  }

	// get a sync'ed snapshot of this object's fields

	Vector fieldVect = getFieldVect();

	for (int i = 0; i < fieldVect.size(); i++)
	  {
	    field = (DBField) fieldVect.elementAt(i);

	    // we can't clear field 0 yet, since we need that
	    // for permissions verifications for other fields

	    if (field.getID() == 0)
	      {
		continue;
	      }

	    if (field.isVector())
	      {
		if (debug)
		  {
		    System.err.println("++ Attempting to clear vector field " + field.getName());
		  }
		
		while (field.size() > 0)
		  {
		    // if this is an InvidDBField, deleteElement()
		    // will convert this request into a deletion of
		    // the embedded object.

		    try
		      {
			retVal = field.deleteElement(0); // *sync*

			if (retVal != null && !retVal.didSucceed())
			  {
			    editset.rollback("del" + label); // *sync*
			    
			    if (retVal.getDialog() != null)
			      {
				return retVal;
			      }
			    
			    // "Server: Error in DBEditObject.finalizeRemove()"
			    // "Custom code disapproved of deleting element from field {0}."
			    return Ganymede.createErrorDialog(ts.l("finalizeRemove.myError"),
							      ts.l("finalizeRemove.badDelete", field.getName()));
			  }
			else
			  {
			    finalResult.unionRescan(retVal);
			  }
		      }
		    catch (GanyPermissionsException ex)
		      {
			editset.rollback("del" + label); // *sync*

			// "Server: Error in DBEditObject.finalizeRemove()"
			// "Permissions violation during attempted deletion of element from field {0}."
			return Ganymede.createErrorDialog(ts.l("finalizeRemove.myError"),
							  ts.l("finalizeRemove.badDeletePerm", field.getName()));
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
		
		if (debug)
		  {
		    System.err.println("++ Attempting to clear scalar field " + field.getName());
		  }
		
		if (field.getType() != PERMISSIONMATRIX &&
		    field.getType() != PASSWORD &&
		    field.getType() != FIELDOPTIONS)
		  {
		    retVal = field.setValueLocal(null); // *sync*
		    
		    if (retVal != null && !retVal.didSucceed())
		      {
			editset.rollback("del" + label); // *sync*
			
			if (retVal.getDialog() != null)
			  {
			    return retVal;
			  }
			
			// "Server: Error in DBEditObject.finalizeRemove()"
			// "Custom code disapproved of clearing the value held in field {0}."
			return Ganymede.createErrorDialog(ts.l("finalizeRemove.myError"),
							  ts.l("finalizeRemove.badScalarClear", field.getName()));
		      }
		    else
		      {
			finalResult.unionRescan(retVal);
		      }
		  }
		else
		  {
		    // catchall for permission matrix, field options,
		    // and password fields, which do this their own
		    // way.
		    
		    try
		      {
			field.setUndefined(true);
		      }
		    catch (GanyPermissionsException ex)
		      {
			throw new RuntimeException(ex);	// should never happen
		      }
		  }
	      }
	  }

	// ok, we've cleared all fields but field 0.. clear that to finish up.

	field = retrieveField((short) 0);

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
		    retVal = field.deleteElementLocal(0); // *sync*

		    if (retVal != null && !retVal.didSucceed())
		      {
			editset.rollback("del" + label); // *sync*

			// "Server: Error in DBEditObject.finalizeRemove()"
			// "Custom code disapproved of deleting element from field {0}."
			return Ganymede.createErrorDialog(ts.l("finalizeRemove.myError"),
							  ts.l("finalizeRemove.badDelete", field.getName()));
		      }
		    else
		      {
			finalResult.unionRescan(retVal);
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

		retVal = field.setValueLocal(null); // *sync*

		if (retVal != null && !retVal.didSucceed())
		  {
		    editset.rollback("del" + label); // *sync*

		    // "Server: Error in DBEditObject.finalizeRemove()"
		    // "Custom code disapproved of clearing the value held in field {0}."
		    return Ganymede.createErrorDialog(ts.l("finalizeRemove.myError"),
						      ts.l("finalizeRemove.badScalarClear", field.getName()));
		  }
	      }
	  }

	editset.popCheckpoint("del" + label);

	return finalResult;
      }
    finally
      {
	// make sure we clear deleting before we return
	
	deleting = false;
      }
  }

  /**
   * This method is used to find all objects which point to us through
   * non-symmetric links, edit them, and break the link.  We do this by
   * consulting the global Ganymede.db.backPointers hash structure to get
   * the list of objects which point to us, and doing the unlink in a
   * fashion similar to InvidDBField.unbindAll().
   *
   * <b>This method is private, and is not to be called by any code outside
   * of this class.</b>
   *
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @return null on success, or a ReturnVal with an error dialog encoded on failure
   */

  private final ReturnVal attemptBackLinkClear(boolean local)
  {
    ReturnVal
      retVal = null,
      newRetVal = new ReturnVal(true, true);

    Vector
      targets;

    Invid remote;

    /* -- */

    if (false)
      {
	System.err.println("Entering attemptBackLinkClear() for object " + toString());
      }

    synchronized (Ganymede.db.backPointers)
      {
	Hashtable backPointers = (Hashtable) Ganymede.db.backPointers.get(getInvid());
	
	if (backPointers == null)
	  {
	    return null;
	  }

	targets = new Vector();

	if (false)
	  {
	    System.err.println("Deleting " + toString() + ", chasing back pointers from global hash");
	  }

	Enumeration en = backPointers.keys();

	while (en.hasMoreElements())
	  {
	    remote = (Invid) en.nextElement();

	    if (false)
	      {
		System.err.println("\t" + getGSession().describe(remote));
	      }

	    targets.addElement(remote);
	  }
      }

    Enumeration targetEnum = targets.elements();

    while (targetEnum.hasMoreElements())
      {
	remote = (Invid) targetEnum.nextElement();
	retVal = clearBackLink(remote, local);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }

	newRetVal = newRetVal.unionRescan(retVal);
      }

    return newRetVal;		// success
  }

  /**
   * This method is called by attemptBackLinkClear(), and is responsible for
   * checking the object with Invid remote out for editing, and clearing our
   * own Invid out of all of the remote object's fields.
   *
   * This method does no checkpointing, so attemptBackLinkClear() has to
   * do that for us.
   *
   * @param remote An Invid for an object that we have asymmetric back links to.
   * @param local If true, we won't do a permissions check before trying to edit the
   * remote object.
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
      session = getSession();

    ReturnVal
      retVal = null,
      newRetVal;

    Vector
      fieldsToUnbind = new Vector();

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
	return Ganymede.createErrorDialog(ts.l("clearBackLink.badRef"),
					  ts.l("clearBackLink.badRefTxt", toString(), remote.toString()));
      }

    if (false)
      {
	System.err.println("DBEditObject.clearBackLink(): Clearing link in " + getGSession().describe(remote));
      }

    // loop over the invid fields in the target, get a list of fields we need to unlink.
    
    Invid myInvid = getInvid();

    // get a thread sync'ed snapshot of the fields in the remote object

    Vector fieldVect = remobj.getFieldVect();

    for (int i = 0; i < fieldVect.size(); i++)
      {
	DBField tmpField = (DBField) fieldVect.elementAt(i);
	
	if (!(tmpField instanceof InvidDBField))
	  {
	    continue;
	  }

	// if the field is symmetric and doesn't point to us, we won't
	// try to unlink it here.
	
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
	    
	    if (tempInvid == null || !tempInvid.equals(myInvid))
	      {
		continue;
	      }
	  }
	
	if (false)
	  {
	    System.err.println("\tNeed to clear field " + tmpField.toString());
	  }
	
	// ok, we know we need to do the unbinding for this field.
	
	fieldsToUnbind.addElement(new Short(tmpField.getID()));
      }

    if (remobj instanceof DBEditObject)
      {
	oldRef = (DBEditObject) remobj;
      }
    else
      {
	if (local || getGSession().getPerm(remobj).isEditable())
	  {
	    oldRef = (DBEditObject) session.editDBObject(remote);
	  }
      }

    if (oldRef == null)
      {
	// it's there, but we can't unlink it

	// "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
	// "You don''t have permission to unlink object {0} from one or more fields in object {1}."
	return Ganymede.createErrorDialog(ts.l("clearBackLink.badUnlink"),
					  ts.l("clearBackLink.perm", getLabel(), remobj.toString()));
      }

    // initialize a ReturnVal to remember our rescan information.

    newRetVal = new ReturnVal(true, true);
    
    for (int i = 0; i < fieldsToUnbind.size(); i++)
      {
	Short remote_fieldid = (Short) fieldsToUnbind.elementAt(i);
	targetField = remote_fieldid.shortValue();

	newRetVal.addRescanField(remote, targetField);

	// are we allowed to ignore permissions on this field?

	try
	  {
	    oldRefField = (InvidDBField) oldRef.getField(targetField);
	  }
	catch (ClassCastException ex)
	  {
	    try
	      {
		// "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
		// "Your operation could not succeed due to an error in the server''s custom schema code.  Target field {0} in object {1} is not an Invid field."
		return Ganymede.createErrorDialog(ts.l("clearBackLink.badUnlink"),
						  ts.l("clearBackLink.badTarget", oldRef.getField(targetField).getName(), oldRef.getLabel()));
	      }
	    catch (RemoteException rx)
	      {
		// "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
		// "Your operation could not succeed due to an error in the server''s custom schema code.  Target field {0} in object {1} is not an Invid field."
		return Ganymede.createErrorDialog(ts.l("clearBackLink.badUnlink"),
						  ts.l("clearBackLink.badTarget", new Short(targetField), oldRef.getLabel()));
	      }
	  }

	if (oldRefField == null)
	  {
	    // editDBObject() will create undefined fields for all fields defined
	    // in the DBObjectBase, so if we got a null result we have a schema
	    // corruption problem.

	    // "DBEditObject.clearBackLink(): Couldn''t unlink old reference"
	    // "Your operation could not succeed due to an inconsistency in the server''s database schema.  Target field number {0} in object {1} does not exist, or you do not have permission to access this field."

	    return Ganymede.createErrorDialog(ts.l("clearBackLink.badUnlink"),
					      ts.l("clearBackLink.badSchema", new Short(targetField), oldRef.getLabel()));
	  }

	try
	  {
	    // clear any reference in this field to us, if we can

	    if (false)
	      {
		System.err.println("DBEditObject.clearBackLink(): calling dissolve on " + oldRefField);
	      }

	    retVal = oldRefField.dissolve(getInvid(), local);

	    if (retVal != null && !retVal.didSucceed())
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

    newRetVal.unionRescan(retVal);

    return newRetVal;
  }

  /**
   * This method provides a pre-commit hook that runs after the user
   * has hit commit but before the system has established write locks
   * for the commit.
   *
   * The intended purpose of this hook is to allow objects that
   * dynamically maintain hidden label fields to update those fields
   * from the contents of the object's other fields at commit time.
   *
   * This method runs in a checkpointed context.  If this method fails
   * in any operation, you should return a ReturnVal with a failure
   * dialog encoded, and the transaction's commit will be blocked and
   * a dialog explaining the problem will be presented to the user.
   */

  public ReturnVal preCommitHook()
  {
    return null;
  }

  /**
   * This method performs verification for the first phase of
   * the two-phase commit algorithm.  If this object returns
   * true from commitPhase1() when called during an editSet's
   * commit() routine, this object CAN NOT refuse commit()
   * at a subsequent point.  Once commitPhase1() is called,
   * the object CAN NOT be changed until the transaction
   * is either committed, abandoned, or released from the 
   * commit process by the
   * {@link arlut.csd.ganymede.server.DBEditObject#release(boolean) release()}
   * method.
   *
   * This method may be subclassed by application objects that need
   * to include extra-Ganymede processes in the two-phase commit
   * protocol.  If a particular subclass of DBEditObject does not need
   * to involve outside processes in the full two-phase commit
   * protocol, this method should not be overridden.
   *
   * If this method is overridden, be sure and call
   * setCommitting(true) before doing anything else.  Failure to call
   * setCommitting() in this method will cause the two phase commit
   * mechanism to behave unpredictably.
   *
   * <B>WARNING!</B> this method is called at a time when portions
   * of the database are locked for the transaction's integration into
   * the database.  You must not call methods that seek to gain a lock
   * on the Ganymede database.  At this point, this means no composite
   * queries on embedded object types, where you seek an object based
   * on a field in an embedded object and in the object itself, using
   * the GanymedeSession query calls, or else you will lock the server.
   *
   * This method should NEVER try to edit or change any DBEditObject
   * in the server.. at this point in the game, the server has fixed the
   * transaction working set and is depending on commitPhase1() not trying
   * to make changes.
   *
   * @see arlut.csd.ganymede.server.DBEditSet 
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

    // If this object is an inconsistent state and was before the
    // transaction started, don't block the commit

    if (getGSession().enableOversight)
      {
	ReturnVal retVal = consistencyCheck(this);

	if (retVal == null || retVal.didSucceed())
	  {
	    return retVal;	// no problem
	  }

	if (original != null)
	  {
	    ReturnVal retVal2 = original.objectBase.getObjectHook().consistencyCheck(original);

	    if (retVal2 != null && !retVal2.didSucceed())
	      {
		return null;	// we were already inconsistent, so don't complain
	      }
	    else
	      {
		return retVal;	// we were consistent before, so complain
	      }
	  }
	else
	  {
	    return retVal;	// newly created inconsistent object, complain
	  }
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
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invocation should be placed here,
   * rather than in
   * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase1() commitPhase1()}.
   *
   * commitPhase2() is generally the last method called on a
   * DBEditObject before it is discarded by the server in the
   * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#commit() commit()} method.
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * <B>WARNING!</B> this method is called at a time when portions
   * of the database are locked for the transaction's integration into
   * the database.  You must not call methods that seek to gain a lock
   * on the Ganymede database.  At this point, this means no composite
   * queries on embedded object types, where you seek an object based
   * on a field in an embedded object and in the object itself, using
   * the GanymedeSession query calls, or else you will lock the server.

   * This method should NEVER try to edit or change any DBEditObject
   * in the server.. at this point in the game, the server has fixed the
   * transaction working set and is depending on commitPhase2() not trying
   * to make changes internal to the server.
   */

  public void commitPhase2()
  {
    return;
  }

  /**  
   * A hook for subclasses to use to clean up any external
   * resources allocated for this object.  This method can be called
   * after commitPhase1() has been called, or it may be called at any
   * time to indicate that this object is being withdrawn from the
   * transaction (as by a checkpoint rollback).  This method <b>will
   * not</b> be called after commitPhase2() has been called.  If you
   * do anything external in commitPhase2(), make sure that all
   * resources allocated for this object (at any time in this object's
   * editing life-cycle) are released before commitPhase2()
   * completes.
   *
   * Ordinarily, there is no need for customizers to override this
   * method.  The only reason to override the release method is if you
   * need to do maintenance on external data structures or connections
   * that were created in commitPhase1() or when this DBEditObject was
   * created.
   *
   * If &lt;finalAbort&gt; is true, the transaction for which this
   * DBEditObject was created is being completely abandoned (if
   * isCommitting() returns true), or this object is being dropped out
   * of the transaction by a checkpoint rollback.  In either case, a
   * customizer may want to clean up all external structures or
   * connections that were created either at the time this
   * DBEditObject was created/checked-out and/or that were created by
   * commitPhase1().
   *
   * If &lt;finalAbort&gt; is false, isCommitting() should always
   * return true.  In this case, one of the DBEditObjects in the
   * transaction returned false from a later commitPhase1() call
   * and all objects that had their commitPhase1() methods called
   * previously will be revisted and release(false) will be called
   * on them.  Customizers may want to clean up any external structures
   * or connections that were established in commitPhase1().
   *
   * Remember, you will usually want to perform external actions in
   * commitPhase2(), in which case release() is not needed.  release()
   * is only useful when you allocate external structures or
   * connections when the object is created or during commitPhase1().
   *
   * It is safe to call release() from your commitPhase2() method
   * if you wish to have one place to clean up structures allocated by
   * initializeNewObject() or commitPhase1().
   *
   * Customizers subclassing this method may want to keep a couple of
   * things in mind.  First, the default release method is not synchronized,
   * and it basically just clear a boolean flag (call setCommitting(false)) to
   * indicate that edit methods on this object may once again go forward.
   * You may want to synchronize your release method if you do anything
   * at all fancy.  More importantly, it is essential that you call setCommitting(false)
   * if &lt;finalAbort&gt; is false so that this object can be edited afterwards.
   *
   * <B>WARNING!</B> this method is called at a time when portions
   * of the database are locked for the transaction's integration into
   * the database.  You must not call methods that seek to gain a lock
   * on the Ganymede database.  At this point, this means no composite
   * queries on embedded object types, where you seek an object based
   * on a field in an embedded object and in the object itself, using
   * the GanymedeSession query calls, or else you will lock the server.
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
   * Returns a hashtable mapping Short field id's to their current
   * value, used by the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * intra-transaction checkpointing logic to capture this object's
   * state at a given time.
   *
   * Each subclass of {@link arlut.csd.ganymede.server.DBField DBField}
   * is responsible for implementing its own version of
   * {@link arlut.csd.ganymede.server.DBField#checkpoint() checkpoint()} to
   * stuff its state into an Object for inclusion in this method's
   * hashtable.
   */

  final Hashtable checkpoint()
  {
    Object key, value;
    Hashtable result = new Hashtable();
    DBField field;

    /* -- */

    // sync'ing on fieldAry is safe enough to do here since we don't
    // call any methods within the sync block that should trigger any
    // external synchronization issues

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field == null)
	      {
		continue;
	      }

	    key = new Short(field.getID());
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
      }

    return result;
  }

  /**
   * Reinstates this object's state from a hashtable returned
   * by {@link arlut.csd.ganymede.server.DBEditObject#checkpoint() checkpoint()},
   * used by the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * intra-transaction checkpoint rollback logic to restore this object's
   * state at a given time.
   *
   * Each subclass of {@link arlut.csd.ganymede.server.DBField DBField}
   * is responsible for implementing its own version of
   * {@link arlut.csd.ganymede.server.DBField#rollback(java.lang.Object) rollback()} to
   * restore its state.
   */

  synchronized final void rollback(Hashtable ckpoint)
  {
    Enumeration en;
    Short key;
    Object value;
    DBField field;

    /* -- */

    en = ckpoint.keys();

    while (en.hasMoreElements())
      {
	key = (Short) en.nextElement();

	field = retrieveField(key.shortValue());

	value = ckpoint.get(key);

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
   * This method is used to write out this DBEditObject and the
   * deltas between this DBEditObject and the original checked in
   * version of this DBEditObject to the provided XMLDumpContext.  The
   * constraints specified in the {@link
   * arlut.csd.ganymede.server.SyncRunner} registered with the
   * XMLDumpContext parameter will be used to control what fields are
   * written.
   *
   * This method should only be called on a DBEditObject that is known
   * to have been edited.. calling this method on a newly created
   * object should not be done.. use the normal DBObject emitXML()
   * method in an &lt;object_delta&gt; element's &lt;after&gt; element
   * instead.
   */

  public void emitXMLDelta(XMLDumpContext xmlOut) throws IOException
  {
    DBObjectBaseField fieldDef;
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

    // by using getFieldAry(), we get the fields in display order.  We
    // just have to be careful not to mess with the array we get back

    DBObjectBaseField[] fieldDefs = objectBase.getFieldAry();

    for (int i = 0; i < fieldDefs.length; i++)
      {
	fieldDef = fieldDefs[i];

	DBField myField = (DBField) this.getField(fieldDef.getID());
	DBField origField = (DBField) original.getField(fieldDef.getID());

	if (origField == null)
	  {
	    // not present in the before state, don't write anything for it

	    continue;
	  }
	else
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

    for (int i = 0; i < fieldDefs.length; i++)
      {
	fieldDef = fieldDefs[i];

	DBField myField = (DBField) this.getField(fieldDef.getID());
	DBField origField = (DBField) original.getField(fieldDef.getID());

	if (!myField.isDefined())
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
   * This method is used to generate a String describing the difference
   * between the current state of the DBEditObject and the original
   * object's state.
   *
   * This method can also be used if this object is newly created.. in
   * this case, it will just return a string containing many 'FieldAdded'
   * entries.
   *
   * @return null if no difference was found
   */

  public synchronized String diff()
  {
    return this.diff(null);
  }

  /**
   * This method does two things.  First and foremost, it is used
   * to generate a String describing the difference between the
   * current state of the DBEditObject and the original object's
   * state.
   *
   * This method can also be used if this object is newly created.. in
   * this case, it will just return a string containing many 'FieldAdded'
   * entries.
   *
   * The second purpose of this method is to generate entries in
   * the changedFieldDefs map, listing those DBObjectBaseFields for
   * which we have observed a value change when comparing this
   * object's state with its original.  We do this as part of the diff
   * algorithm because the original DBField definition only provided
   * one method to compare two fields for differences, and that is the
   * getDiffString() method.  Since we're calling that here anyway,
   * recording the definition of fields that we know changed is an
   * extremely cheap win.  The changedFieldDefs Hashtable is used in
   * the DBEditSet class to update time stamps in the
   * DBObjectBaseFields, so that builder tasks can tell whether they
   * have been run since any of a given field have been changed in a given
   * DBObjectBase.
   *
   * @param changedFieldDefs If not null, this parameter will be a
   * HashMap that the diff algorithm should insert unity mappings
   * for each DBObjectBaseField whose value was found to have changed
   * in this diff.
   *
   * @return null if no difference was found
   */

  public synchronized String diff(HashMap changedFieldDefs)
  {
    boolean diffFound = false;
    StringBuffer result = new StringBuffer();
    DBObjectBaseField fieldDef;
    DBField origField, currentField;
    StringBuffer added = new StringBuffer();
    StringBuffer deleted = new StringBuffer();
    StringBuffer changed = new StringBuffer();

    /* -- */

    // algorithm: iterate over base.fieldTable to find all fields
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

    Enumeration en = objectBase.fieldTable.elements();

    while (en.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) en.nextElement();

	// we don't care if certain fields change

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
		changedFieldDefs.put(fieldDef, fieldDef);
	      }
	  }
	else
	  {
	    origField = (DBField) original.getField(fieldDef.getID());
	  }

	currentField = (DBField) this.getField(fieldDef.getID());

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
		changedFieldDefs.put(fieldDef, fieldDef);
	      }

	    // "\t{0}: {1}\n"
	    added.append(ts.l("diff.field_template", fieldDef.getName(), currentField.getValueString()));

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
		changedFieldDefs.put(fieldDef, fieldDef);
	      }

	    // "\t{0}: {1}\n"
	    deleted.append(ts.l("diff.field_template", fieldDef.getName(), origField.getValueString()));

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
		    changedFieldDefs.put(fieldDef, fieldDef);
		  }

		changed.append(fieldDef.getName());
		changed.append("\n");
		changed.append(diff);

		diffFound = true;

		if (debug)
		  {
		    System.err.println("Field changed: " + 
				       fieldDef.getName() + "\n" +
				       diff);
		  }
	      }
	  }
      }

    if (diffFound)
      {
	if (added.length() > 0)
	  {
	    // "Fields Added:\n\n{0}\n"
	    result.append(ts.l("diff.added", added));
	  }

	if (changed.length() > 0)
	  {
	    // "Fields Changed:\n\n{0}\n"
	    result.append(ts.l("diff.changed", changed));
	  }

	if (deleted.length() > 0)
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
    return Ganymede.internalSession;
  }
}
