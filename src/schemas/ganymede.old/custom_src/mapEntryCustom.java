/*

   mapEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.
   
   Created: 9 December 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  mapEntryCustom

------------------------------------------------------------------------------*/

public class mapEntryCustom extends DBEditObject implements SchemaConstants, mapEntrySchema {

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * user's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public mapEntryCustom(DBObject original, DBEditSet editset) throws RemoteException
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
    InvidDBField field, field2;
    StringBuffer buff = new StringBuffer();

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    field = (InvidDBField) object.getField(MAP); // map name
    field2 = (InvidDBField) object.getField(VOLUME); // volume

    try
      {
	if (field != null)
	  {
	    buff.append(field.getValueString() + ":");
	  }

	if (field2 != null)
	  {
	    buff.append(field2.getValueString());
	  }
      }
    catch (IllegalArgumentException ex)
      {
	buff.append("<?:?>");
      }

    return buff.toString();
  }
}
