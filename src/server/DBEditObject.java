/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.96 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBEditOBject

------------------------------------------------------------------------------*/

/**
 *
 * DBEditObject is the main base class that is subclassed by individual
 * application object types to provide editing and management intelligence.
 * Both static and instance methods are defined in DBEditObject which can
 * be subclassed to provide object management intelligence.<br><br> 
 *
 * A instance of DBEditObject is a copy of a DBObject that has been
 * exclusively checked out from the main database so that a DBSession
 * can edit the fields of the object.  The DBEditObject class keeps
 * track of the changes made to fields, keeping things properly
 * synchronized with unique field name spaces.<br><br>
 *
 * All DBEditObjects are obtained in the context of a DBEditSet.  When
 * the DBEditSet is committed, the DBEditObject is made to replace the
 * original object from the DBStore.  If the EditSet is aborted, the
 * DBEditObject is dropped.<br><br>
 *
 * <b>IMPORTANT PROGRAMMING NOTE!</b>: It is critical that
 * synchronized methods in DBEditObject and in subclasses thereof do not
 * call synchronized methods in DBSession, as there is a strong possibility
 * of nested monitor deadlocking.
 *   
 * @version $Revision: 1.96 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 *
 */

public class DBEditObject extends DBObject implements ObjectStatus, FieldType {

  static boolean debug = false;

  public final static int FIRSTOP = 0;
  public final static int SETVAL = 1;
  public final static int SETELEMENT = 2;
  public final static int ADDELEMENT = 3;
  public final static int DELELEMENT = 4;
  public final static int LASTOP = 4;

  public final static void setDebug(boolean val)
  {
    debug = val;
  }

  /* --------------------- Instance fields and methods --------------------- */

  protected DBObject original;
  boolean committing;

  /**
   * true if the object is in the middle of carrying
   * out deletion logic.. consulted by subclasses
   * to determine whether they should object to fields
   * being set to null
   */

  protected boolean deleting = false;	

  /**
   * true if this object has been processed
   * by a DBEditSet's commit logic
   */

  boolean finalized = false;	

  byte status;

  /**
   * true if the object has a version currently
   * stored in the DBStore
   */

  boolean stored;		

  /**
   *
   * Used as a coordinating signal with InvidDBField to handle
   * clearing the backlinks field in finalizeRemove().
   *
   * Should never ever ever ever be messed with outside this
   * object.
   *
   */

  boolean clearingBackLinks = false;

  /* -- */

  /**
   *
   * Dummy constructor, is responsible for creating a DBEditObject strictly
   * for the purpose of having a handle to call customization methods on.
   *
   */

  public DBEditObject(DBObjectBase base)
  {
    this.objectBase = base;
    editset = null;		// this will be our cue to our static handle status for our methods
  }

  /**
   *
   * Creation constructor, is responsible for creating a new editable
   * object with all fields listed in the DBObjectBaseField instantiated
   * but undefined.<br><br>
   *
   * This constructor is not really intended to be overriden in subclasses.
   * Creation time field value initialization is to be handled by
   * initializeNewObject().
   *
   * @see arlut.csd.ganymede.DBField
   */

  public DBEditObject(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid.getNum());

    if (editset == null)
      {
	throw new NullPointerException("null editset");
      }

    original = null;
    this.editset = editset;
    this.gSession = editset.getSession().getGSession();
    committing = false;
    stored = false;
    status = CREATING;

    /* -- */

    Enumeration 
      enum = null;

    DBObjectBaseField 
      fieldDef;

    DBField 
      tmp = null;

    /* -- */

    fields = new DBFieldTable(objectBase.fieldTable.size(), (float) 1.0);

    synchronized (objectBase)
      {
	enum = objectBase.fieldTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();

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
		fields.putNoSyncNoRemove(tmp);
	      }
	  }
      }
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public DBEditObject(DBObject original, DBEditSet editset)
  {
    super(original.objectBase);

    Enumeration 
      enum;

    DBObjectBaseField 
      fieldDef;

    DBField 
      field, 
      tmp = null;

    Short key;

    /* -- */

    shadowObject = null;
    this.editset = editset;
    committing = false;
    stored = true;
    status = EDITING;

    fields = new DBFieldTable(objectBase.fieldTable.size(), (float) 1.0);

    gSession = getSession().getGSession();

    synchronized (original)
      {
	this.original = original;
	this.id = original.id;
	this.myInvid = original.myInvid;
	this.objectBase = original.objectBase;
      }

    // clone the fields from the original object
    // since we own these, the field-modifying
    // methods on the copied fields will allow editing
    // to go forward

    if (original.fields != null)
      {
	enum = original.fields.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBField) enum.nextElement();

	    switch (field.getType())
	      {
	      case BOOLEAN:
		tmp = new BooleanDBField(this, (BooleanDBField) field);
		break;
		    
	      case NUMERIC:
		tmp = new NumericDBField(this, (NumericDBField) field);
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
		fields.putNoSyncNoRemove(tmp);
	      }
	  }
      }
	
    // now create slots for any fields that are in this object type's
    // DBObjectBase, but which were not present in the original
    
    synchronized (objectBase)
      {
	enum = objectBase.fieldTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();
	    
	    if (!fields.containsKey(fieldDef.getID()))
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

		fields.putNoSyncNoRemove(tmp);
	      }
	  }
      }
  }

  /**
   *
   * Returns the DBSession that this object is checked out in
   * care of.
   *
   * @see arlut.csd.ganymede.DBSession
   *
   */

  public final DBSession getSession()
  {
    return editset.getSession();
  }

  /**
   * Returns the GanymedeSession that this object is checked out in
   * care of.
   *
   * @see arlut.csd.ganymede.GanymedeSession
   * 
   */

  public final GanymedeSession getGSession()
  {
    return getSession().getGSession();
  }

  /**
   *
   * Returns the original version of the object that we were created
   * to edit.  If we are a newly created object, this method will
   * return null.
   * 
   */

  public final DBObject getOriginal()
  {
    return original;
  }

  /**
   *
   * Returns a code indicating whether this object
   * is being created, edited, or deleted.
   *
   * @see arlut.csd.ganymede.ObjectStatus#CREATING
   * @see arlut.csd.ganymede.ObjectStatus#EDITING
   * @see arlut.csd.ganymede.ObjectStatus#DELETING
   * @see arlut.csd.ganymede.ObjectStatus#DROPPING
   *
   */

  public final byte getStatus()
  {
    return status;
  }

  /**
   * This method is used to make sure that the built-in fields that
   * the server assumes will always be present in any editable object
   * will be in place.<br><br>
   * 
   * This method checks with instantiateNewField() if the field id is
   * not one of those that is needfull.  If instantiateNewField() approves
   * the creation of a new field, checkNewField() will check to see if
   * the GanymedeSession's permissions permit the field creation.
   *
   */

  public final boolean checkNewField(short fieldID)
  {
    if (fieldID <= 8)
      {
	return true;		// we always allow the built in fields
      }

    return instantiateNewField(fieldID);
  }

  /**
   *
   * Sets this object's status code
   *
   * @see arlut.csd.ganymede.ObjectStatus#CREATING
   * @see arlut.csd.ganymede.ObjectStatus#EDITING
   * @see arlut.csd.ganymede.ObjectStatus#DELETING
   * @see arlut.csd.ganymede.ObjectStatus#DROPPING
   *
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
   *
   * Shortcut method to set a field's value.  Using this
   * method can save the client a roundtrip to the server.<br><br>
   *
   * This method cannot be used on permission fields or password
   * fields.
   *
   * @see arlut.csd.ganymede.db_object 
   */

  public final ReturnVal setFieldValue(short fieldID, Object value)
  {
    // note! this *must* be setValue(), not setValueLocal(), as this
    // is a method that the client calls directly.

    try
      {
	return getField(fieldID).setValue(value);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote on a local op: " + ex);
      }
  }

  /**
   *
   * Shortcut method to set a field's value.  This version bypasses
   * permission checking and is only intended for server-side
   * use.<br><br>
   *
   * This method cannot be used on permission fields or password
   * fields.
   *
   */

  public final ReturnVal setFieldValueLocal(short fieldID, Object value)
  {
    ReturnVal retval;
    DBField field = (DBField) getField(fieldID);

    /* -- */

    if (field != null)
      {
	return field.setValueLocal(value);
      }

    return Ganymede.createErrorDialog("DBEditObject.setFieldValueLocal() error",
				      "DBEditObject.setFieldValueLocal() couldn't find field " + fieldID + 
				      " in object " + getLabel());
  }

  /**
   *
   * Returns true if the object has ever been stored in the DBStore under the
   * current invid.
   *
   */

  public final boolean isStored()
  {
    return stored;
  }

  /**
   *
   * Clears out any non-valued fields, used to clean out any fields that remained
   * undefined after editing is done.
   *
   */

  final synchronized void clearTransientFields()
  {
    Enumeration enum;
    DBField field;
    Vector removeList;
    
    /* -- */

    removeList = new Vector();
    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	// we don't want to emit fields that don't have anything in them..
	// DBField.isDefined() is supposed to tell us whether a field should
	// be kept.

	if (!field.isDefined())
	  {
	    removeList.addElement(field);

	    if (false)
	      {
		System.err.println("going to be removing transient: " + ((DBField) field).getName()); 
	      }
	  }
      }

    enum = removeList.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
	fields.remove(field.getID());
      }
  }

  /**
   *
   * This method scans through all fields defined in the DBObjectBase
   * for this object type and determines if all required fields have
   * been filled in.  If everything is ok, this method will return
   * null.  If any required fields are found not to have been filled
   * out, a ReturnVal will be returned with didSucceed() set to false
   * and a dialog encoded describing the fields that need to be
   * filled out before this object can be checked in to the database.<br><br>
   *
   * If server-local code has called
   * GanymedeSession.enableOversight(false), this method will not be
   * called at transaction commit time.
   *  
   */

  public final synchronized ReturnVal checkRequiredFields()
  {
    Vector localFields = new Vector();
    DBObjectBaseField fieldDef;
    DBField field;
    StringBuffer errorBuf = new StringBuffer();

    /* -- */

    // assume that the sortedFields will not be changed
    // at a time when this method is called.  A reasonable
    // assumption, as sortedFields is only altered when
    // the schema is being edited.

    for (int i = 0; i < objectBase.sortedFields.size(); i++)
      {
	fieldDef = (DBObjectBaseField) objectBase.sortedFields.elementAt(i);

	// we don't care at this point about built in fields

	if (fieldDef.isBuiltIn())
	  {
	    continue;
	  }

	if (fieldRequired(this, fieldDef.getID()))
	  {
	    field = (DBField) getField(fieldDef.getID());

	    if (field == null || !field.isDefined())
	      {
		localFields.addElement(fieldDef.getName());
	      }
	  }
      }

    // if all required fields checked out, return success

    if (localFields.size() == 0)
      {
	return null;
      }
    
    errorBuf.append("Error, ");
    errorBuf.append(objectBase.getName());
    errorBuf.append(" object ");
    errorBuf.append(getLabel());
    errorBuf.append(" has not been completely filled out.  The following fields need ");
    errorBuf.append("to be filled in before this transaction can be committed:\n\n");
    
    for (int i = 0; i < localFields.size(); i++)
      {
	errorBuf.append((String) localFields.elementAt(i));
	errorBuf.append("\n");
      }

    return Ganymede.createErrorDialog("Error, required fields not filled in",
				      errorBuf.toString());
  }



  /* -------------------- pseudo-static Customization hooks -------------------- 


     The following block of methods are intended to be used in static fashion..
     that is, a DBObjectBase can load in a class that extends DBEditObjectBase
     and hold an instance of such as DBObjectBase.objectHook.  The following
     methods are used in a static fashion, that is they are primarily intended
     to perform actions on designated external DBObjects rather than on the
     per-DBObjectBase instance.

  */

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param object The object that the link is to be created in
   * @param fieldID The field that the link is to be created in
   * @param gsession Who is trying to do this linking?
   *
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID, GanymedeSession gsession)
  {
    return anonymousLinkOK(object, fieldID);
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   * @param gsession Who is trying to do this unlinking?
   *
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID, GanymedeSession gsession)
  {
    return anonymousUnlinkOK(object, fieldID);
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param object The object that the link is to be created in
   * @param fieldID The field that the link is to be created in
   *
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    return false;		// by default, permission is denied
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   *
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID)
  {
    return true;		// by default, permission is granted to unlink
  }

  /**
   *
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given object, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.<br><br>
   *
   * This method should be used very sparingly.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   *
   * Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given object.<br><br>
   *
   * This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object)
  {
    return null;
  }

  /**
   *
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given field, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.<br><br>
   *
   * This method should be used very sparingly.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   *
   * Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given field.<br><br>
   *
   * This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object, short fieldid)
  {
    return null;
  }

  /**
   *
   * Customization method to verify overall consistency of
   * a DBObject.  While default code has not yet been
   * written for this method, it may need to have its
   * parameter list modified to include the controlling
   * DBSession to allow coordination of DBLock and the
   * the use of DBEditSet.findObject() to get a transaction
   * consistent view of related objects.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   *
   */

  public ReturnVal consistencyCheck(DBObject object)
  {
    return null;
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    return false;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to view a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for viewing to the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canRead(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.<br><br>
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.<br><br>
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   * 
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // by default, return the field definition's visibility

    if (field.getFieldDef().base != this.objectBase)
      {
	throw new IllegalArgumentException("field/object mismatch");
      }

    return field.getFieldDef().isVisible(); 
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to edit a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for editing by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canWrite(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canBeInactivated()
  {
    return false;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to inactivate a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.<br><br>
   *
   * Note that unlike canRemove(), canInactivate() takes
   * a DBEditObject instead of a DBObject.  This is because
   * inactivating an object is based on editing the object,
   * and so we have the GanymedeSession/DBSession classes
   * go ahead and check the object out for editing before
   * calling us.  This serves to force the session classes
   * to check for write permission before attempting inactivation.
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return false;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to remove a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for removal by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canRemove(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to clone a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for cloning by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canClone(DBSession session, DBObject object)
  {
    return false;
  }


  /**
   *
   * Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public DBEditObject cloneObject(DBSession session, DBObject object)
  {
    return null;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to create an instance of this object type.  The client's DBSession object
   * will call the canCreate method in the DBObjectBase for this object type
   * to determine whether creation is allowed to the user.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
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
   *
   * Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public String getLabelHook(DBObject object)
  {
    return null;		// no default
  }

  /**
   *
   * Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    return false;
  }


  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * user's name interposed.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean virtualizeField(short fieldID)
  {
    return false;
  }

  /**
   *
   * This method provides a hook to return interposed values for
   * fields that have their data massaged by a DBEditObject
   * subclass.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public Object getVirtualValue(DBField field)
  {
    return null;
  }

  /* -------------------- editing/creating Customization hooks -------------------- 


     The following block of methods are intended to be subclassed to
     provide intelligence for the object creation/editing process.

  */

  /**
   *
   * Initialize a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * DBObjectBase have been instantiated without defined
   * values.<br><br>
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the DBSession
   * associated with the editset defined in this DBEditObject.<br><br>
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return false.  If the owning GanymedeSession is not in
   * bulk-loading mode (i.e., enableOversight is true),
   * DBSession.createDBObject() will checkpoint the transaction before
   * calling this method.  If this method returns false, the calling
   * method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.<br><br>
   *
   * If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.<br><br>
   *
   * This method should be overridden in subclasses.
   *   
   */

  public boolean initializeNewObject()
  {
    return true;
  }

  /**
   *
   * This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.<br><br>
   *
   * This method does not affect those fields which are actually present
   * in the object's record in the DBStore.  What this method allows
   * you to do is have a subclass decide whether it wants to instantiate
   * a potential field (one that is declared in the field dictionary for
   * this object, but which doesn't happen to be presently defined in
   * this object) in this particular object.<br><br>
   *
   * A concrete example will help here.  The Permissions Object type
   * (base number SchemaConstants.PermBase) holds a permission
   * matrix, a descriptive title, and a list of admin personae that hold
   * those permissions for objects they own.<br><br>
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
   *
   */

  public boolean instantiateNewField(short fieldID)
  {
    return gSession.getPerm(getTypeID(), fieldID, true).isCreatable(); // *sync* GanymedeSession
  }

  /**
   * This method is the hook that DBEditObject subclasses use to interpose
   * wizards when a field's value is being changed.<br><br>
   *
   * Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.<br><br>
   *
   * In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.<br><br>
   *
   * If server-local code has called
   * GanymedeSession.enableOversight(false), this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.<br><br>
   *
   * This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.<br><br>
   *
   * This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * the finalize*() methods have no power to set object/field rescan
   * or return dialogs to the client, however.. in cases where such
   * is necessary, a custom plug-in class must have wizardHook() and
   * finalize*() configured to work together to both provide proper field
   * rescan notification and to check the operation being performed and
   * make any changes necessary to other fields and/or objects.<br><br>
   *
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.
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
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    return null;		// by default, we just ok whatever
  }

  /**
   *
   * Hook to have this object create a new embedded object
   * in the given field.  
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public Invid createNewEmbeddedObject(InvidDBField field)
  {
    throw new IllegalArgumentException("Error: createNewEmbeddedObject called on base DBEditObject");
  }

  /**
   *
   * This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.<br><br>
   *
   * Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?
   * 
   */

  public boolean verifyNewValue(DBField field, Object value)
  {
    return true;
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
   *
   * This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.<br><br>
   *
   */

  public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    return null;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval of
   * any vector add operation, and to take any special actions in
   * reaction to the add.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.<br><br>
   *
   */

  public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    return null;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval of
   * any vector set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.<br><br>
   *
   */

  public ReturnVal finalizeSetElement(DBField field, int index, Object value)
  {
    return null;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval of
   * any scalar set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its value.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.<br><br>
   *
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    return null;
  }

  /**
   * This method returns true if field1 should not show any choices
   * that are currently selected in field2, where both field1 and
   * field2 are fields in this object.<br><br>
   * 
   * The purpose of this method is to allow mutual exclusion between
   * a pair of fields with mandatory choices.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   * 
   */

  public boolean excludeSelected(db_field field1, db_field field2)
  {
    return false;
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.
   *
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
	    //	    Ganymede.debug("DBEditObject: Returning null 2 for choiceList for field: " + field.getName());
	    return null;
	  }

	return new Short(baseId);
      }

    return null;
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.<br><br>
   *
   * This method will provide a reasonable default for targetted
   * invid fields.<br><br>
   *
   * NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
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

	if (Ganymede.internalSession == null)
	  {
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

	// note that the query we are submitting here *will* be filtered by the
	// current visibilityFilterInvid field in GanymedeSession.

	return editset.getSession().getGSession().query(new Query(baseId, root, editOnly), this);
      }
    
    //    Ganymede.debug("DBEditObject: Returning null for choiceList for field: " + field.getName());
    return null;
  }

  /**
   *
   * This method is used to tell the client whether the list of options it gets
   * back for a field can be taken out of the cache.  If this method returns
   * true, that means that some of the results that obtainChoiceList() will
   * return will include items that normally wouldn't be availble to the
   * client, but are in this case because of the anonymousLinkOK() results.
   *
   */

  public boolean choiceListHasExceptions(DBField field)
  {
    if (!(field instanceof InvidDBField))
      {
	throw new IllegalArgumentException("choiceListHasExceptions(): field not an InvidDBField.");
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

    return targetBase.getObjectHook().anonymousLinkOK(this, targetField, this.gSession);
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
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
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to determine whether it is permissible to enter
   * IPv6 address in a particular (IP) DBField.
   *
   */

  public boolean isIPv6OK(DBField field)
  {
    return false;
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given Date field has a restricted
   * range of possibilities.
   *
   */

  public boolean isDateLimited(DBField field)
  {
    return false;
  }

  /**
   *
   * This method is used to specify the earliest acceptable date
   * for the specified field.
   *
   */

  public Date minDate(DBField field)
  {
    return new Date(Long.MIN_VALUE);
  }

  /**
   *
   * This method is used to specify the latest acceptable date
   * for the specified field.
   *
   */

  public Date maxDate(DBField field)
  {
    return new Date(Long.MAX_VALUE);
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given Numeric field has a restricted
   * range of possibilities.
   *
   */

  public boolean isIntLimited(DBField field)
  {
    return false;
  }

  /**
   *
   * This method is used to specify the minimum acceptable value
   * for the specified field.
   *
   */

  public int minInt(DBField field)
  {
    return Integer.MIN_VALUE;
  }

  /**
   *
   * This method is used to specify the maximum acceptable value
   * for the specified field.
   *
   */

  public int maxInt(DBField field)
  {
    return Integer.MAX_VALUE;
  }

  /**
   * This method handles inactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * inactivate() method can then be called on the object to put the
   * object into inactive mode.  inactivate() will set the object's
   * removal date and fix up any other state information to reflect
   * the object's inactive status.<br><br>
   *
   * inactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to inactive the object.<br><br>
   *
   * The inactive() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.<br><br>
   *
   * If inactivate() returns a ReturnVal that has its success flag set
   * to false and does not include a JDialogBuff for further
   * interaction with the user, then DBSEssion.inactivateDBObject()
   * method will rollback any changes made by this method.<br><br>
   *
   * If inactivate() returns a success value, we expect that the object
   * will have a removal date set.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.<br><br>
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object inactivation when
   * the transaction is committed..<br><br>
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public ReturnVal inactivate()
  {
    return Ganymede.createErrorDialog("DBEditObject.inactivate() error",
				      "Base DBEditObject does not support inactivation");
  }

  /**
   *
   * This method is to be called by the custom DBEditObject inactivate()
   * logic when the inactivation is performed so that logging can be
   * done.<br><br>
   *
   * If inactivation of an object causes the label to be null, this
   * won't work as well as we'd really like.
   *
   */

  final protected void finalizeInactivate(boolean success)
  {
    if (success)
      {
	Object val = getFieldValueLocal(SchemaConstants.RemovalField);

	if (val != null)
	  {
	    Vector invids = new Vector();
	    
	    invids.addElement(this.getInvid());
	
	    StringBuffer buffer = new StringBuffer();

	    buffer.append(getTypeDesc());
	    buffer.append(" ");
	    buffer.append(getLabel());
	    buffer.append(" has been inactivated.\n\nThe object is due to be removed from the database at ");
	    buffer.append(getFieldValueLocal(SchemaConstants.RemovalField).toString());
	    buffer.append(".");
	
	    editset.logEvents.addElement(new DBLogEvent("inactivateobject",
							buffer.toString(),
							(gSession.personaInvid == null ?
							 gSession.userInvid : gSession.personaInvid),
							gSession.username,
							invids,
							null));
	  }
	else
	  {
	    Vector invids = new Vector();
	    
	    invids.addElement(this.getInvid());
	
	    StringBuffer buffer = new StringBuffer();

	    buffer.append(getTypeDesc());
	    buffer.append(" ");
	    buffer.append(getLabel());
	    buffer.append(" has been inactivated.\n\nThe object has no removal date set.");
	
	    editset.logEvents.addElement(new DBLogEvent("inactivateobject",
							buffer.toString(),
							(gSession.personaInvid == null ?
							 gSession.userInvid : gSession.personaInvid),
							gSession.username,
							invids,
							null));
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
   * the object's reactive status.<br><br>
   *
   * reactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to reactive the object.<br><br>
   *
   * If reactivate() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, then DBSEssion.inactivateDBObject() method will rollback any changes
   * made by this method.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.<br><br>
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

    return Ganymede.createErrorDialog("DBEditObject.reactivate() error",
				      "Object not inactivated.");
  }

  /**
   *
   * This method is to be called by the custom DBEditObject reactivate()
   * logic when the reactivation is performed so that logging can be
   * done.
   *
   */

  final protected void finalizeReactivate(boolean success)
  {
    if (success)
      {
	Vector invids = new Vector();

	invids.addElement(this.getInvid());

	StringBuffer buffer = new StringBuffer();

	buffer.append(getTypeDesc());
	buffer.append(" ");
	buffer.append(getLabel());
	buffer.append(" has been reactivated.\n");
	
	editset.logEvents.addElement(new DBLogEvent("reactivateobject",
						    buffer.toString(),
						    (gSession.personaInvid == null ?
						     gSession.userInvid : gSession.personaInvid),
						    gSession.username,
						    invids,
						    null));
      }
    else
      {
	editset.rollback("reactivate" + getLabel());
      }
  }

  /**
   *
   * This method handles removal logic for this object type.  This method
   * will be called immediately from DBSession.deleteDBObject().<br><br>
   *
   * The remove() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.<br><br>
   *
   * If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, the DBSession.deleteDBObject() method will roll back any changes
   * made by this method.<br><br>
   *
   * remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's remove() logic decides to
   * enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeRemove() on the object when
   * it is determined that the object really should be removed,
   * with a boolean indicating whether success was had.
   *
   */

  public ReturnVal remove()
  {
    return null;
  }

  /**
   * This method handles Ganymede-internal deletion logic for this
   * object type.  finalizeRemove() is responsible for dissolving any
   * invid inter-object references in particular.<br><br>
   *
   * It is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object removal when
   * the transaction is committed..<br><br>
   *
   * finalizeremove() returns a ReturnVal indicating whether the
   * internal removal bookkeeping was successful.  A failure result
   * will cause the DBSession to rollback the transaction to the state
   * prior to any removal actions for this object were
   * attempted.<br><br>
   *
   * remove() is intended for subclassing, whereas finalizeRemove() is
   * not.  finalizeRemove() provides the standard logic for wiping out
   * fields and what not to cause the object to be unlinked from
   * other objects.<br><br>
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
    Enumeration enum;
    DBSession session;
    String label = getLabel();	// remember the label before we clear it

    /* -- */

    if (!success)
      {
	editset.rollback("del" + label); // *sync*
	return Ganymede.createErrorDialog("Object Removal Failure",
					  "Could not delete object " + label +
					  ", custom code rejected this operation.");
      }

    // we want to delete / null out all fields.. this will take care
    // of invid links, embedded objects, and namespace allocations.
    
    // set the deleting flag to true so that our subclasses won't
    // freak about values being set to null.

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

    this.deleting = true;

    try
      {
	enum = fields.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBField) enum.nextElement();

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

		try
		  {
		    if (field.getID() == SchemaConstants.BackLinksField)
		      {
			// let the InvidDBField code know that it doesn't need
			// to wrap unbindAll() with its own checkpoint/rollback.

			clearingBackLinks = true;
		      }

		    while (field.size() > 0)
		      {
			// if this is an InvidDBField, deleteElement()
			// will convert this request into a deletion of
			// the embedded object.
			
			retVal = field.deleteElement(0); // *sync*
			
			if (retVal != null && !retVal.didSucceed())
			  {
			    session = editset.getSession();
			    
			    if (session != null)
			      {
				session.setLastError("DBEditObject disapproved of deleting element from field " + 
						     field.getName());
			      }
			    
			    editset.rollback("del" + label); // *sync*
			    
			    return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
							      "DBEditObject disapproved of deleting element from field " + 
							      field.getName());
			  }
			else
			  {
			    finalResult.unionRescan(retVal);
			  }
		      }
		  }
		finally
		  {
		    clearingBackLinks = false;
		  }
	      }
	    else
	      {
		// permission matrices and passwords don't allow us to
		// call set value directly.  We're mainly concerned
		// with invid's (for linking), i.p. addresses and
		// strings (for the namespace) here anyway.

		if (debug)
		  {
		    System.err.println("++ Attempting to clear scalar field " + field.getName());
		  }

		if (field.getType() != PERMISSIONMATRIX &&
		    field.getType() != PASSWORD)
		  {
		    retVal = field.setValueLocal(null); // *sync*

		    if (retVal != null && !retVal.didSucceed())
		      {
			session = editset.getSession();
		    
			if (session != null)
			  {
			    session.setLastError("DBEditObject could not clear field " + 
						 field.getName());
			  }

			editset.rollback("del" + label); // *sync*

			return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
							  "DBEditObject could not clear field " + 
							  field.getName());
		      }
		    else
		      {
			finalResult.unionRescan(retVal);
		      }
		  }
		else
		  {
		    // catchall for permission matrix and password
		    // fields, which do this their own way.

		    field.setUndefined(true);
		  }
	      }
	  }

	// ok, we've cleared all fields but field 0.. clear that to finish up.

	field = (DBField) getField((short) 0);

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
			session = editset.getSession();
		    
			if (session != null)
			  {
			    session.setLastError("DBEditObject disapproved of deleting element from field " + 
						 field.getName());
			  }

			editset.rollback("del" + label); // *sync*

			return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
							  "DBEditObject disapproved of deleting element from field " + 
							  field.getName());
		      }
		    else
		      {
			finalResult.unionRescan(retVal);
		      }
		  }
	      }
	    else
	      {
		// scalar field 0 is the ContainerField for an embedded
		// object.  Note that setting this field to null will
		// not unlink us from the the container object, since
		// the ContainerField pointer is a generic one.

		retVal = field.setValueLocal(null); // *sync*

		if (retVal != null && !retVal.didSucceed())
		  {
		    session = editset.getSession();
		    
		    if (session != null)
		      {
			session.setLastError("DBEditObject could not clear field " + 
					     field.getName());
		      }

		    editset.rollback("del" + label); // *sync*

		    return Ganymede.createErrorDialog("Server: Error in DBEditObject.finalizeRemove()",
						      "DBEditObject could not clear field " + 
						      field.getName());
		  }
	      }
	  }

	// ok, we should be successful if we get here.  log the object deletion.

	Vector invids = new Vector();
	invids.addElement(this.getInvid());

	editset.logEvents.addElement(new DBLogEvent("deleteobject",
						    getTypeDesc() + ":" + label,
						    (gSession.personaInvid == null ?
						     gSession.userInvid : gSession.personaInvid),
						    gSession.username,
						    invids,
						    null));

	return finalResult;
      }
    finally
      {
	// make sure we clear deleting before we return
	
	deleting = false;
      }
  }

  /**
   *
   * This method performs verification for the first phase of
   * the two-phase commit algorithm.  If this object returns
   * true from commitPhase1() when called during an editSet's
   * commit() routine, this object CAN NOT refuse commit()
   * at a subsequent point.  Once commitPhase1() is called,
   * the object CAN NOT be changed until the transaction
   * is either fully committed or abandoned. <br><br>
   *
   * This method is intended to be subclassed by application
   * objects that need to include extra-Ganymede processes
   * in the two-phase commit protocol.  If a particular
   * subclass of DBEditObject does not need to involve outside
   * processes in the full two-phase commit protocol, this
   * method should not be overridden.<br><br>
   *
   * If this method is overridden, be sure and set this.committing to
   * true before doing anything else.  Failure to set committing to
   * true in this method will cause the two phase commit mechanism to
   * behave unpredictably.
   *
   * @see arlut.csd.ganymede.DBEditSet 
   *
   */

  public synchronized ReturnVal commitPhase1()
  {
    committing = true;
    return consistencyCheck(this);
  }

  /**
   *
   * This method returns true if this object has already gone
   * through phase 1 of the commit process, which requires
   * the DBEditObject to not accept further changes.
   *
   */

  public final boolean isCommitting()
  {
    return committing;
  }

  /**
   *
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.<br><br>
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invokation should be placed here,
   * rather than in commitPhase1().<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.DBEditSet
   */

  public void commitPhase2()
  {
    return;
  }

  /**
   *
   * This method is a hook for subclasses to do clean up action if the
   * commit process is not able to go to completion for some reason.
   * Generally, release() should be responsible for doing cleanup for
   * processes initiated by commitPhase1().  If commitPhase1() does
   * not do anything external to Ganymede, release() shouldn't either.
   * release() should return immediately if isCommitting() is false;<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.<br><br>
   *
   * If this method is overridden, be sure and set this.committing to
   * false as part of your release method.  If this is not done, no
   * further changes will be possible to this object.
   *  
   */

  public void release()
  {
    committing = false;
    return;
  }

  // ***
  //
  // Checkpoint / Rollback support
  //
  // ***

  synchronized final Hashtable checkpoint()
  {
    Enumeration enum;
    Object key, value;
    Hashtable result = new Hashtable();
    DBField field;

    /* -- */

    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
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

    return result;
  }

  synchronized final void rollback(Hashtable ckpoint)
  {
    Enumeration enum;
    Short key;
    Object value;
    Hashtable result = new Hashtable();
    DBField field;

    /* -- */

    enum = ckpoint.keys();

    while (enum.hasMoreElements())
      {
	key = (Short) enum.nextElement();

	field = fields.get(key.shortValue());

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
   *
   * This method is used to generate a String describing the difference
   * between the current state of the DBEditObject and the original
   * object's state.<br><br>
   *
   * This method can also be used if this object is newly created.. in
   * this case, it will just return a string containing many 'FieldAdded'
   * entries.
   *
   * @return null if no difference was found
   *
   */

  public synchronized String diff()
  {
    boolean diffFound = false;
    StringBuffer result = new StringBuffer();
    DBObjectBaseField fieldDef;
    DBField origField, currentField;
    StringBuffer added = new StringBuffer();
    StringBuffer deleted = new StringBuffer();
    StringBuffer changed = new StringBuffer();

    /* -- */

    // algorithm: iterate over base.sortedFields to find all fields
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

    Enumeration enum = objectBase.sortedFields.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();

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
	    added.append("\t");
	    added.append(fieldDef.getName());
	    added.append(":");
	    added.append(currentField.getValueString());
	    added.append("\n");

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
	    deleted.append("\t");
	    deleted.append(fieldDef.getName());
	    deleted.append(":");
	    deleted.append(origField.getValueString());
	    deleted.append("\n");

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
	    result.append("Fields Added:\n\n");
	    result.append(added);
	    result.append("\n");
	  }

	if (changed.length() > 0)
	  {
	    result.append("Fields changed:\n\n");
	    result.append(changed);
	    result.append("\n");
	  }

	if (deleted.length() > 0)
	  {
	    result.append("Fields Deleted:\n\n");
	    result.append(deleted);
	    result.append("\n");
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

  protected final GanymedeSession internalSession()
  {
    return Ganymede.internalSession;
  }
}
