/*

   emailListCustom.java

   Custom plug-in for managing fields in the email list object type.
   
   Created: 16 February 1999
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/02/16 18:57:45 $
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

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 emailListCustom

------------------------------------------------------------------------------*/

/**
 *   Custom plug-in for managing fields in the email list object type.
 */

public class emailListCustom extends DBEditObject implements SchemaConstants, emailListSchema {

  private QueryResult membersChoice = null;

  /* -- */

  /**
   *
   * Customization Constructor
   *
   */

  public emailListCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public emailListCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public emailListCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
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
    if (field.getID() == MEMBERS)
      {
	return null;
      }

    return super.obtainChoicesKey(field);
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.<br><br>
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() != emailListSchema.MEMBERS)
      {
	return super.obtainChoiceList(field);
      }

    if (membersChoice == null)
      {
	Query query1 = new Query(SchemaConstants.UserBase, null, false); // list all users
	Query query2 = new Query((short) 275, null, false); // list all external email targets
	
	QueryNode root3 = new QueryNotNode(new QueryDataNode(-2, QueryDataNode.EQUALS, this.getInvid()));
	Query query3 = new Query((short) 274, root3, false); // list all other email groups, but not ourselves
	
	QueryResult result = editset.getSession().getGSession().query(query1);
	result.append(editset.getSession().getGSession().query(query2));
	result.append(editset.getSession().getGSession().query(query3));
	
	membersChoice = result;
      }

    return membersChoice;
  }

}
