/*

   ipCustom.java

   This file is a management class for ip objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.1 $ %D%
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

    if (field.getID() == 256)
      {
	fieldDef = field.getFieldDef();
	
	if (fieldDef.getTargetBase() > -1)
	  {
	    targetBase = Ganymede.db.getObjectBase(fieldDef.getTargetBase());
	    newObject = targetBase.createNewObject(editset);
	    
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
