/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 interfaceCustom

------------------------------------------------------------------------------*/

public class interfaceCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  // ---

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * interface's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public interfaceCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public interfaceCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public interfaceCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
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
    DBField field, field2;
    DBObject ipObj, dnsObj;
    String result = null;
    Invid invid = null;

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    field = (DBField) object.getField((short) 257); // get the I.P. records field

    if (field == null)
      {
	field = (DBField) object.getField((short) 256); // get the Ethernet address
	
	return field.getValueString();
      }

    if (field.size() > 0)
      {
	invid = (Invid) field.getElement(0); // get the primary I.P. address object
      }

    if (invid == null)
      {
	field = (DBField) object.getField((short) 256); // get the Ethernet address

	if (field == null)
	  {
	    return "interface " + getInvid();
	  }
	
	return field.getValueString();
      }

    // okay to use DBSession.viewDBObject() here, as we are basically just looking
    // to get label information
    //
    // Note that we have to use Ganymede.internalSession here because we are
    // getting label information for another object, and not for ourself

    ipObj = Ganymede.internalSession.getSession().viewDBObject(invid);

    if (ipObj == null)
      {
	field = (DBField) object.getField((short) 256); // get the Ethernet address
	
	return field.getValueString();
      }

    field = (DBField) ipObj.getField((short) 256); // vector of dns records

    if (field == null)
      {
	field = (DBField) ipObj.getField((short) 257); // get the i.p. address
	
	return field.getValueString();
      }

    invid = (Invid) field.getElement(0); // get the primary dns record

    if (invid == null)
      {
	field = (DBField) ipObj.getField((short) 257); // get the i.p. address
	
	return field.getValueString();
      }

    // okay to use DBSession.viewDBObject() here, as we are basically just looking
    // to get label information
    //
    // Note that we have to use Ganymede.internalSession here because we are
    // getting label information for another object, and not for ourself

    dnsObj = (DBObject) Ganymede.internalSession.getSession().viewDBObject(invid);

    if (dnsObj == null)
      {
	field = (DBField) ipObj.getField((short) 257); // get the i.p. address

	return result + ":" + field.getValueString();
      }

    field = (DBField) dnsObj.getField((short) 257);

    if (field == null)
      {
	field = (DBField) ipObj.getField((short) 257); // get the i.p. address
	return field.getValueString();
      }
    else
      {
	result = field.getValueString();
      }

    if (result == null || result.equals(""))
      {
	field = (DBField) ipObj.getField((short) 257); // get the i.p. address
	return field.getValueString();
      }
    else
      {
	field = (DBField) ipObj.getField((short) 257); // get the i.p. address

	return result + ":" + field.getValueString();
      }
  }

  /**
   *
   * Hook to have this object create a new embedded object
   * in the given field.  
   *
   */

  public Invid createNewEmbeddedObject(InvidDBField field)
  {
    DBEditObject newObject;
    DBObjectBase targetBase;
    DBObjectBaseField fieldDef;

    /* -- */

    if (field.getID() == 257)
      {
	fieldDef = field.getFieldDef();
	
	if (fieldDef.getTargetBase() > -1)
	  {
	    newObject = getSession().createDBObject(fieldDef.getTargetBase(), null, null);

	    // link it in

	    newObject.setFieldValue(SchemaConstants.ContainerField, getInvid());
	    
	    return newObject.getInvid();
	  }
	else
	  {
	    editset.getSession().setLastError("error in schema.. imbedded object type not restricted..");
	    return null;
	  }
      }
    else
      {
	return null;		// default
      }
  }
}
