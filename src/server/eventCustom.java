/*

   eventCustom.java

   This file is a management class for event-class records in Ganymede.
   
   Created: 9 December 1997
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 1999/01/22 18:05:57 $
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

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     eventCustom

------------------------------------------------------------------------------*/

public class eventCustom extends DBEditObject implements SchemaConstants {

  /**
   *
   * Customization Constructor
   *
   */

  public eventCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public eventCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public eventCustom(DBObject original, DBEditSet editset) throws RemoteException
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
      case SchemaConstants.EventToken:
      case SchemaConstants.EventName:
	return true;
      }

    return false;
  }
}
