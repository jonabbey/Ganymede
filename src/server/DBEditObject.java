/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.36 $ %D%
   Module By: Jonathan Abbey
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
 * DBEditObject is dropped.
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

  DBObject original;
  boolean committing;
  boolean finalized = false;	// true if this object has been processed
				// by a DBEditSet's commit logic

  byte status;
  boolean stored;		// true if the object has a version currently
				// stored in the DBStore

  /**
   *
   * Dummy constructor, is responsible for creating a DBEditObject strictly
   * for the purpose of having a handle to call customization methods on.
   *
   */

  public DBEditObject(DBObjectBase base) throws RemoteException
  {
    this.objectBase = base;
    editset = null;		// this will be our cue to our static handle status for our methods
  }

  /**
   *
   * Creation constructor, is responsible for creating a new editable
   * object with all fields listed in the DBObjectBaseField instantiated
   * but undefined.
   *
   * This constructor is not really intended to be overriden in subclasses.
   * Creation time field value initialization is to be handled by
   * initializeNewObject().
   *
   * @see arlut.csd.ganymede.DBField
   */

  public DBEditObject(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid.getNum());

    if (editset == null)
      {
	throw new NullPointerException("null editset");
      }

    original = null;
    this.editset = editset;
    committing = false;
    stored = false;
    status = CREATING;

    /* -- */

    Enumeration 
      enum;

    Object 
      key;

    DBObjectBaseField 
      fieldDef;

    DBField 
      tmp = null;

    /* -- */

    fields = new Hashtable();

    synchronized (objectBase)
      {
	enum = objectBase.fieldHash.keys();
	
	while (enum.hasMoreElements())
	  {
	    key = enum.nextElement();
	    
	    fieldDef = (DBObjectBaseField) objectBase.fieldHash.get(key);

	    if (!instantiateNewField(fieldDef.getID()))
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
		fields.put(key, tmp);
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

  public DBEditObject(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original.objectBase);

    Enumeration 
      enum;

    Object 
      key;

    DBObjectBaseField 
      fieldDef;

    DBField 
      field, 
      tmp = null;

    synchronized (original)
      {
	this.original = original;
	this.id = original.id;
	this.myInvid = original.myInvid;
	this.objectBase = original.objectBase;
      }

    shadowObject = null;
    this.editset = editset;
    committing = false;
    stored = true;
    status = EDITING;

    gSession = getSession().getGSession();

    fields = new Hashtable();

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
	    key = new Short(field.getID());

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
		fields.put(key, tmp);
	      }
	  }
      }
	
    // now create slots for any fields that are in this object type's
    // DBObjectBase, but which were not present in the original
    
    synchronized (objectBase)
      {
	enum = objectBase.fieldHash.keys();
	
	while (enum.hasMoreElements())
	  {
	    key = enum.nextElement();
	    
	    if (!fields.containsKey(key))
	      {
		fieldDef = (DBObjectBaseField) objectBase.fieldHash.get(key);

		if (!instantiateNewField(fieldDef.getID()))
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

		fields.put(key, tmp);
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

  DBSession getSession()
  {
    return editset.getSession();
  }

  /**
   *
   * Returns a code indicating whether this object
   * is being created, edited, or deleted.
   *
   * @see #CREATING
   * @see #EDITING
   * @see #DELETING
   * @see #DROPPING
   *
   */

  byte getStatus()
  {
    return status;
  }

  /**
   *
   * Sets this object's status code
   *
   * @see #CREATING
   * @see #EDITING
   * @see #DELETING
   * @see #DROPPING
   *
   */

  synchronized void setStatus(byte new_status)
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
   * Returns true if the object has ever been stored in the DBStore under the
   * current invid.
   *
   */

  boolean isStored()
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
    Object key;
    
    /* -- */

    removeList = new Vector();
    enum = fields.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();
	field = (DBField) fields.get(key);

	// we don't want to emit fields that don't have anything in them..
	// DBField.defined is supposed to be a flag that keeps track of that,
	// but there seems to be a bug in the system someplace.  The value/values
	// checks is supposed to make up for this.. this is really not the way
	// to do it.  THis is rank laziness.

	if (!field.defined || 
	    (!(field instanceof PermissionMatrixDBField) &&
	    ((field.value == null) && 
	     ((field.values == null) || (field.values.size() == 0)))))
	  {
	    removeList.addElement(key);

	    if (false)
	      {
		System.err.println("going to be removing transient: " + ((DBField) field).getName()); 
	      }
	  }
      }

    enum = removeList.elements();

    while (enum.hasMoreElements())
      {
	fields.remove(enum.nextElement());
      }
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    return null;		// by default, we just ok whatever
  }

  /**
   *
   * Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   * @see arlut.csd.ganymede.db_object
   */

  public ReturnVal setFieldValue(short fieldID, Object value)
  {
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
   * the target.
   *
   */

  public boolean anonymousLinkOK(short fieldID)
  {
    // assume that anonymous linking is not ok by default

    return false;
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   */

  public boolean anonymousUnlinkOK(short fieldID)
  {
    // assume that unlinking is ok by default

    return true;
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
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean consistencyCheck(DBObject object)
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to view a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for viewing to the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
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
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canWrite(DBSession session, DBObject object)
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.
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
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canInactivate(DBSession session, DBObject object)
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
   * To be overridden in DBEditObject subclasses.
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
   * To be overridden in DBEditObject subclasses.
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
   * clone, etc.), this customization method will actually do the work.
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
   * to determine whether creation is allowed to the user.
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canCreate(Session session)
  {
    return true;
  }

  /**
   *
   * Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type
   *
   */

  public String getLabelHook(DBObject object)
  {
    return null;		// no default
  }

  /**
   *
   * This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.
   *
   * This method does not affect those fields which are actually present
   * in the object's record in the DBStore.  What this method allows
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
   *
   */

  public boolean instantiateNewField(short fieldID)
  {
    return true;
  }

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * user's name interposed.
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
   * subclass.
   *
   */

  public Object getVirtualValue(DBField field)
  {
    return null;
  }

  /**
   *
   * Initialize a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has
   * been created and all fields defined in the
   * controlling DBObjectBase have been instantiated
   * without defined values.<br><br>
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the DBSession
   * associated with the editset defined in this DBEditObject.<br><br>
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return false.  Right now there is no infrastructure in
   * Ganymede to allow the transaction to be aborted from
   * within the DBSession's createDBObject() method.  As a result,
   * if this method is to fail to properly initialize the object,
   * it should be able to not leave an impact on the rest of the
   * DBStore.. in other words, setting InvidField values that
   * involve symmetry relationships could be problematic. <br><br>
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
   * This method allows the DBEditObject to have executive approval
   * of any vector delete operation, and to take any special actions
   * in reaction to the delete.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * its vector.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   *
   */

  public boolean finalizeDeleteElement(DBField field, int index)
  {
    return true;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any vector add operation, and to take any special actions
   * in reaction to the add.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * its vector.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.
   *
   * The DBField that called us will take care of all possible
   * checks on the operation (including vector bounds, etc.),
   * acceptable values as appropriate (including a call to our
   * own verifyNewValue() method).  Under normal circumstances,
   * we won't need to do anything here.
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   * 
   */

  public boolean finalizeAddElement(DBField field, Object value)
  {
    return true;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any vector set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's vector.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.
   *
   * The DBField that called us will take care of all possible
   * checks on the operation (including vector bounds, etc.),
   * acceptable values as appropriate (including a call to our
   * own verifyNewValue() method.  Under normal circumstances,
   * we won't need to do anything here.
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   * 
   */

  public boolean finalizeSetElement(DBField field, int index, Object value)
  {
    return true;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's value.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   *  
   */

  public boolean finalizeSetValue(DBField field, Object value)
  {
    return true;
  }

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
   * calling choices() on this object again.
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
   * obtainChoiceList() method to get a list of valid choices.
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
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

	// note that the query we are submitting here *will* be filtered by the
	// current visibilityFilterInvid field in GanymedeSession.

	return editset.getSession().getGSession().query(new Query(baseId, root, true));
      }
    
    //    Ganymede.debug("DBEditObject: Returning null for choiceList for field: " + field.getName());
    return null;
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
   * This method is used to specify the earliest acceptable date
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
   *
   * This method handles inactivation logic for this object
   * type.  DBEditObject's are checked out for editing, the
   * inactivate() method can then be called on the object
   * to put the object into inactive mode.  inactivate will
   * set the object's removal date and fix up any
   * other state information to reflect the object's
   * inactive status.<br><br>
   *
   * It is up to commitPhase1() and commitPhase2() to handle
   * any exterior actions related to the object's inactivation.<br><br>
   *
   * inactivate() returns true if the internal inactivation bookkeeping was
   * successful, and false if not.  A false return value will cause
   * the DBSession to abort the transaction;  this method is not responsible
   * for undoing an unsuccessful partial inactivation.
   *
   * @see #commitPhase1()
   * @see #commitPhase2()
   */

  public boolean inactivate()
  {
    return true;
  }

  /**
   *
   * This method handles removal logic for this object type.  This method
   * will be called from DBSession.deleteDBObject() during the transaction's
   * commit logic.  The remove() method can cause other objects to be
   * deleted, can cause strings to be removed from fields in other objects,
   * whatever.
   *
   * If remove() returns false, the transaction that the object is being
   * deleted in will be aborted.
   *
   */

  public synchronized boolean remove()
  {
    return true;
  }

  /**
   *
   * This method handles Ganymede-internal deletion logic for this object
   * type.  remove() is responsible for dissolving any invid inter-object
   * references in particular.<br><br>
   *
   * It is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object removal.<br><br>
   *
   * remove() returns true if the internal removal bookkeeping was
   * successful, and false if not.  A false return value will 
   * cause the DBSession to abort the transaction; this method is not
   * responsible for undoing unsuccessful partial removal bookkeeping.
   *
   * Note that this method is issued from DBEditSet's transaction commit
   * logic, and should generally not be called from anywhere else.
   *
   * @see #commitPhase1()
   * @see #commitPhase2()
   */

  public synchronized boolean finalizeRemove()
  {
    DBField field;
    Enumeration enum;
    DBSession session;

    /* -- */

    // call the remove logic hook

    if (!remove())
      {
	return false;
      }

    // we want to delete / null out all fields.. this will take care
    // of invid links, embedded objects, and namespace allocations.

    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	if (field.isVector())
	  {
	    while (field.size() > 0)
	      {
		if (field.deleteElement(0) != null)
		  {
		    session = editset.getSession();
		    
		    if (session != null)
		      {
			session.setLastError("DBEditObject disapproved of deleting element from field " + field.getName());
		      }

		    return false;
		  }
	      }
	  }
	else
	  {
	    if (field.getType() != PERMISSIONMATRIX)
	      {
		if (field.setValue(null) != null)
		  {
		    session = editset.getSession();
		    
		    if (session != null)
		      {
			session.setLastError("DBEditObject could not clear field " + field.getName());
		      }

		    return false;
		  }
	      }
	  }
      }

    return true;
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
   * method should not be overridden. <br><br>
   *
   * If this method is overridden, be sure and set
   * this.committing to true before doing anything else.  Failure
   * to set committing to true in this method will cause the
   * two phase commit mechanism to behave unpredictably.
   *
   * @see arlut.csd.ganymede.DBEditSet
   */

  public synchronized boolean commitPhase1()
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

  public synchronized boolean isCommitting()
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
   * process being initiated whose success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server, the process invokation should be placed here,
   * rather than in commitPhase1().
   *
   *
   * @see arlut.csd.ganymede.DBEditSet
   */

  public synchronized void commitPhase2()
  {
    clearTransientFields();
    return;
  }

  /**
   *
   * This method is a hook for subclasses to do clean up action if the
   * commit process is not able to go to completion for some reason.
   * Generally, release() should be responsible for doing cleanup for
   * processes initiated by commitPhase1().  If commitPhase1() does
   * not do anything external to Ganymede, release() shouldn't either.
   * release() should return immediately if isCommitting() is false;
   * 
   */

  public synchronized void release()
  {
    return;
  }

  /**
   *
   * This method is used to generate a String describing the difference
   * between the current state of the DBEditObject and the original
   * object's state.
   *
   */

  public synchronized String diff()
  {
    StringBuffer result = new StringBuffer();
    DBObjectBaseField fieldDef;
    DBField origField, currentField;

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

	origField = (DBField) original.getField(fieldDef.getID());
	currentField = (DBField) this.getField(fieldDef.getID());

	if ((origField == null || !origField.defined) && 
	    (currentField == null || !currentField.defined))
	  {
	    continue;
	  }

	if (((origField == null) || !origField.defined) &&
	    ((currentField != null) && currentField.defined))
	  {
	    result.append("Field added: " + fieldDef.getName() + "\nValue: " +
			  currentField.getValueString() + "\n");

	    if (debug)
	      {
		System.err.println("Field added: " + fieldDef.getName() + "\nValue: " +
				   currentField.getValueString() + "\n");
	      }
	  }
	else if (((currentField == null) || !currentField.defined) &&
		 ((origField != null) && origField.defined))
	  {
	    result.append("Field deleted: " + fieldDef.getName() + "\nValue" +
			  origField.getValueString() + "\n");

	    if (debug)
	      {
		System.err.println("Field deleted: " + fieldDef.getName() + "\nValue" +
				   origField.getValueString() + "\n");
	      }
	  }
	else
	  {
	    String diff = currentField.getDiffString(origField);

	    if (diff != null)
	      {
		result.append("Field changed: " + 
			      fieldDef.getName() + "\n" +
			      diff);

		if (debug)
		  {
		    System.err.println("Field changed: " + 
				       fieldDef.getName() + "\n" +
				       diff);
		  }
	      }
	  }
      }

    return result.toString();
  }

  /*----------------------------------------------------------

    Convenience methods for our customization subclasses

  ----------------------------------------------------------*/

  protected GanymedeSession internalSession()
  {
    return Ganymede.internalSession;
  }
}
