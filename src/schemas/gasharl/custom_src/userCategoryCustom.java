/*

   userCategoryCustom.java

   This file is a management class for user Category objects in Ganymede.
   
   Created: 7 October 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              userCategoryCustom

------------------------------------------------------------------------------*/

/**
 * This class is the custom plug-in to handle the user category object
 * type in the Ganymede server.<br>
 *
 * <br>See the userCategorySchema.java file for a list of field definitions that this
 * module expects to work with.<br>
 *
 * @see arlut.csd.ganymede.custom.userCategorySchema
 * @see arlut.csd.ganymede.DBEditObject
 * */

public class userCategoryCustom extends DBEditObject implements SchemaConstants {

  /**
   *
   * Customization Constructor
   *
   */

  public userCategoryCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public userCategoryCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public userCategoryCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
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
    if (fieldID == SchemaConstants.BackLinksField)
      {
	return true;
      }

    return false;		// by default, permission is denied
  }
  
}
