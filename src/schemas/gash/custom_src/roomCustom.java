/*

   roomCustom.java

   This file is a management class for room objects in Ganymede.
   
   Created: 21 May 1998
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
                                                                      roomCustom

------------------------------------------------------------------------------*/

public class roomCustom extends DBEditObject {

  /**
   *
   * Customization Constructor
   *
   */

  public roomCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public roomCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public roomCustom(DBObject original, DBEditSet editset)
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
    if (fieldid == roomSchema.ROOMNUMBER)
      {
	return true;
      }

    return false;
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    if (fieldID == roomSchema.SYSTEMS)
      {
	return true;
      }

    return false;
  }
}
