/*

   ipCustom.java

   This file is a management class for ip objects in Ganymede.
   
   Created: 15 October 1997
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 1999/01/22 18:04:29 $
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

    /* -- */

    if (field.getID() == 256)
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
	    throw new RuntimeException("error in schema.. interface field target base not restricted..");
	  }
      }
    else
      {
	return null;		// default
      }
  }
}
