/*

   userCategoryCustom.java

   This file is a management class for user Category objects in Ganymede.
   
   Created: 7 October 1998
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2003/07/31 22:54:50 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
