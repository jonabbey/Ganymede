/*

   Base.java

   Client side interface to the object type dictionary
   
   Created: 17 April 1997
   Release: $Name:  $
   Version: $Revision: 1.14 $
   Last Mod Date: $Date: 1999/01/22 18:05:25 $
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

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                            Base

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface definition for the Ganymede DBObjectBase class.  This
 * interface allows the client to query type information remotely.
 *
 */

public interface Base extends CategoryNode, Remote {

  public boolean isRemovable() throws RemoteException;

  public boolean isEmbedded() throws RemoteException;

  public String getName() throws RemoteException;
  public String getClassName() throws RemoteException;
  public short getTypeID() throws RemoteException;
  public short getLabelField() throws RemoteException;
  public String getLabelFieldName() throws RemoteException;

  /**
   *
   * This method returns a list of field definitions used by this
   * object type.  If includeBuiltIns is false, common fields (such
   * as last modification date, etc.) will be not be included in the
   * list.  The list is sorted by field display order.
   *
   */

  public Vector getFields(boolean includeBuiltIns) throws RemoteException;

  /**
   *
   * This method returns a list of field definitions used by this
   * object type, including builtins. The list is sorted by field
   * display order.
   * 
   */

  public Vector getFields() throws RemoteException;
  public BaseField getField(short id) throws RemoteException;
  public BaseField getField(String name) throws RemoteException;

  public boolean canCreate(Session session) throws RemoteException;
  public boolean canInactivate() throws RemoteException;

  // the following methods are only valid when the Base reference
  // is obtained from a SchemaEdit reference.

  public boolean setName(String newName) throws RemoteException;
  public void setClassName(String newName) throws RemoteException;

  public void setLabelField(String fieldName) throws RemoteException;
  public void setLabelField(short fieldID) throws RemoteException;
 
  public Category getCategory() throws RemoteException;

  /**
   *
   * If lowRange is true, the field's id will start at 100 and go up,
   * other wise it will start at 256 and go up.
   *
   */

  public BaseField createNewField(boolean lowRange) throws RemoteException;
  public boolean deleteField(BaseField bF) throws RemoteException;
  public boolean fieldInUse(BaseField bF) throws RemoteException;
}
