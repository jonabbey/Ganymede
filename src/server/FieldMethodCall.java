/*
   GASH 2

   FieldMethodCall.java

   The GANYMEDE object storage system.

   Created: 28 July 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/07/28 13:06:48 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

import java.util.Vector;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 FieldMethodCall

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to pass a proxied DBField method call from a remote
 * client to the Ganymede server.</p>
 */

public class FieldMethodCall implements Serializable {

  static final long serialVersionUID = 6962669323238785259L;

  /**
   * The id of an DBObject on the server to perform a method call on.
   */

  Invid objectId;

  /**
   * The id of the field in the indicated DBObject to perform a method call
   * on.
   */

  short fieldId;

  /**
   * The name of a method to call on the indicated DBObject.
   */

  String methodName;

  /**
   * The list of parameters to the DBObject method.
   */

  Object[] paramArray;

  /* -- */

  public FieldMethodCall(Invid objectId, short fieldId, String methodName, Vector params)
  {
    this.objectId = objectId;
    this.fieldId = fieldId;
    this.methodName = methodName;

    paramArray = new Object[params.size()];

    for (int i = 0; i < params.size(); i++)
      {
	paramArray[i] = params.elementAt(i);
      }
  }

  public Object dispatch(Session session)
  {
    return null;
  }
}
