/*

   objectEventCustom.java

   This file is a management class for object event-class records in Ganymede.
   
   Created: 9 July 1998
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               objectEventCustom

------------------------------------------------------------------------------*/

public class objectEventCustom extends DBEditObject implements SchemaConstants {

  /**
   *
   * We're going to present the user with a list of recommended event
   * names to choose from.
   *
   */

  static QueryResult eventNames = null;

  // ---

  /**
   *
   * Since object types can only be changed by the schema editor, we'll
   * cache the object type list for the duration of this object's being
   * edited.  We could even make this a static, but then we'd have to
   * have the DBSchemaEdit code know to clear it when the schema was
   * edited, which would be a hassle.
   *
   */

  QueryResult objectTypeList = null;


  /* -- */

  /**
   *
   * Customization Constructor
   *
   */

  public objectEventCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public objectEventCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public objectEventCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    // both fields defined in event are required

    switch (fieldid)
      {
      case SchemaConstants.ObjectEventToken:
      case SchemaConstants.ObjectEventName:

	return true;
      }

    return false;
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

    if (field.getID() == SchemaConstants.ObjectEventObjectName)
      {
	return true;
      }

    return false;
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
    if (object.getTypeID() != getTypeID())
      {
	throw new IllegalArgumentException("bad object type");
      }

    StringBuffer result = new StringBuffer();

    Integer id = (Integer) object.getFieldValueLocal(SchemaConstants.ObjectEventObjectType);

    if (id != null)
      {
	DBObjectBase base = Ganymede.db.getObjectBase(id.shortValue());

	if (base != null)
	  {
	    result.append(base.getName());
	    result.append(":");
	  }
      }

    String name = (String) object.getFieldValueLocal(SchemaConstants.ObjectEventToken);

    result.append(name);

    return result.toString();
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
    if (field.getID() == SchemaConstants.ObjectEventObjectName)
      {
	if (objectTypeList == null)
	  {
	    Vector list = Ganymede.db.getBaseNameList();

	    objectTypeList = new QueryResult(true);

	    for (int i = 0; i < list.size(); i++)
	      {
		objectTypeList.addRow(null, (String) list.elementAt(i), false);
	      }
	  }

	return objectTypeList;
      }

    if (field.getID() == SchemaConstants.ObjectEventToken)
      {
	if (eventNames == null)
	  {
	    eventNames = new QueryResult(true);

	    eventNames.addRow(null, "objectcreated", false);
	    eventNames.addRow(null, "objectchanged", false);
	    eventNames.addRow(null, "inactivateobject", false);
	    eventNames.addRow(null, "deleteobject", false);
	    eventNames.addRow(null, "reactivateobject", false);
	    eventNames.addRow(null, "expirationwarn", false);
	    eventNames.addRow(null, "expirenotify", false);
	    eventNames.addRow(null, "removalwarn", false);
	    eventNames.addRow(null, "removenotify", false);
	  }

	return eventNames;
      }

    return super.obtainChoiceList(field);
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's value.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.<br><br>
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   *  
   */

  public boolean finalizeSetValue(DBField field, Object value)
  {
    if (field.getID() == SchemaConstants.ObjectEventObjectName)
      {
	// let the field be cleared if this object is being deleted.

	if (value == null)
	  {
	    return true;
	  }

	DBObjectBase base = Ganymede.db.getObjectBase((String) value);

	setFieldValueLocal(SchemaConstants.ObjectEventObjectType, new Integer(base.getTypeID()));
      }

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
   * To be overridden in DBEditObject subclasses.
   * 
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // by default, return the field definition's visibility

    if (field.getObjTypeID() != getTypeID())
      {
	throw new IllegalArgumentException("field/object mismatch");
      }

    // We don't want the user to see the ObjectEventObjectType
    // field, since we use it as a scratch pad internally for
    // keeping the name correct.

    if (field.getID() == SchemaConstants.ObjectEventObjectType)
      {
	return false;
      }

    return field.getFieldDef().isVisible(); 
  }


}
