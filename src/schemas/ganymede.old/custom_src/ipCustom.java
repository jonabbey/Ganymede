/*

   ipCustom.java

   This file is a management class for ip objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        ipCustom

------------------------------------------------------------------------------*/

public class ipCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * ip's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public ipCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public ipCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public ipCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }
  
  /**
   *
   * Hook to determine whether it is permissible to enter
   * an IPv6 address in a particular (IP) DBField.
   *
   */

  public boolean isIPv6OK(DBField field)
  {
    // our IP records have an IP address in field 257

    if (field.getID() == 257)
      {
	// our IP records have a pointer to the containing I.P. net record
	// in field 258

	InvidDBField invF = (InvidDBField) getField((short)258);

	if (invF == null)
	  {
	    return false;	// we shouldn't really get here in production
	  }

	// get our I.P. network record

	Invid inv = invF.value();

	if (inv == null)
	  {
	    return false;
	  }

	DBObject ipNet = editset.getSession().viewDBObject(inv);

	if (ipNet == null)
	  {
	    return false;
	  }
	else
	  {
	    // our I.P. Net records have a boolean indicating whether or not v6
	    // addresses are allowed in field 262.

	    BooleanDBField bF = (BooleanDBField) ipNet.getField((short)262);
	    
	    if (bF == null || !bF.isDefined())
	      {
		return false;
	      }
	    else
	      {
		return bF.value();
	      }
	  }
      }

    return false;
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
    InvidDBField container;

    /* -- */

    if (field.getID() == 256)
      {
	fieldDef = field.getFieldDef();
	
	if (fieldDef.getTargetBase() > -1)
	  {
	    targetBase = Ganymede.db.getObjectBase(fieldDef.getTargetBase());
	    newObject = targetBase.createNewObject(editset);

	    // link it in

	    container = (InvidDBField) newObject.getField(SchemaConstants.ContainerField);
	    container.setValue(getInvid());
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
