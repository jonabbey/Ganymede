/*

   SchemaEdit.java

   Client side interface for schema editing
   
   Created: 17 April 1997
   Release: $Name:  $
   Version: $Revision: 1.9 $
   Last Mod Date: $Date: 1999/01/22 18:05:54 $
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
                                                                      SchemaEdit

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface definition for the the Ganymede Schema Editing class.
 *
 */

public interface SchemaEdit extends Remote {

  /**
   *
   * Returns true if the schema editor is allowing
   * the 'constant' fields to be edited.  This is
   * provided solely so the Ganymede developers can
   * make incompatible changes to the 'constant' schema
   * items during development.
   *
   */

  public boolean isDevelopMode() throws RemoteException;

  /**
   *
   * When the server is in develop mode, it is possible to create new
   * built-in fields, or fields that can be relied on by the server
   * code to exist in every non-embedded object type defined.
   * 
   */

  public BaseField createNewBuiltIn() throws RemoteException;

  //
  // From here on are the normal schema editing methods
  //

  public Category getRootCategory() throws RemoteException;

  public Base[] getBases(boolean embedded) throws RemoteException;
  public Base[] getBases() throws RemoteException;

  public Base getBase(short id) throws RemoteException;
  public Base getBase(String baseName) throws RemoteException;

  public Base createNewBase(Category category, boolean embedded, boolean lowRange) throws RemoteException;
  public void deleteBase(Base b) throws RemoteException;

  public NameSpace[] getNameSpaces() throws RemoteException;
  public NameSpace getNameSpace(String spaceName) throws RemoteException;

  public NameSpace createNewNameSpace(String name, boolean caseInsensitive) throws RemoteException;
  public boolean deleteNameSpace(String name) throws RemoteException;

  public void commit() throws RemoteException;
  public void release() throws RemoteException;
}
