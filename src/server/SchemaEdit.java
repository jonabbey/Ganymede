/*

   SchemaEdit.java

   Client side interface for schema editing
   
   Created: 17 April 1997
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 2000/02/29 09:35:19 $
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
 * <P>Primary remote interface for Ganymede schema editing.  The
 * Ganymede admin console calls the 
 * {@link arlut.csd.ganymede.GanymedeAdmin#editSchema editSchema()} method on
 * a server-side {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin} object
 * to get access to
 * the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} objects which
 * hold the type definitions for the objects held in the Ganymede server.  The
 * DBObjectBase objects in turn provide access to the definitions for
 * the fields held in those object types.</P>
 *
 */

public interface SchemaEdit extends Remote {

  //
  // From here on are the normal schema editing methods
  //

  /**
   * <P>Returns the root category node from the server</P>
   */

  public Category getRootCategory() throws RemoteException;

  /**
   * <P>Returns a list of bases from the current (non-committed) state of the system.</P>
   *
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   */

  public Base[] getBases(boolean embedded) throws RemoteException;

  /**
   * <P>Returns a list of bases from the current (non-committed) state of the system.</P>
   */

  public Base[] getBases() throws RemoteException;

  /**
   * <P>Returns a {@link arlut.csd.ganymede.Base Base} reference to 
   * match the id, or null if no match.</P>
   */

  public Base getBase(short id) throws RemoteException;

  /** 
   * <P>Returns a {@link arlut.csd.ganymede.Base Base} reference to
   * match the baseName, or null if no match.</P>
   */

  public Base getBase(String baseName) throws RemoteException;

  /** 
   * <P>This method creates a new {@link
   * arlut.csd.ganymede.DBObjectBase DBObjectBase} object and returns
   * a remote handle to it so that the admin client can set fields on
   * the base, set attributes, and generally make a nuisance of
   * itself.</P>
   */

  public Base createNewBase(Category category, boolean embedded, boolean lowRange) throws RemoteException;

  /**
   * <P>This method deletes a {@link
   * arlut.csd.ganymede.DBObjectBase DBObjectBase}, removing it from the
   * Schema Editor's working set of bases.  The removal won't
   * take place for real unless the SchemaEdit is committed.</P>
   */

  public ReturnVal deleteBase(String baseName) throws RemoteException;

  /**
   * <P>This method returns an array of defined 
   * {@link arlut.csd.ganymede.NameSpace NameSpace} objects.</P>
   */

  public NameSpace[] getNameSpaces() throws RemoteException;

  /**
   * <P>This method returns a {@link arlut.csd.ganymede.NameSpace NameSpace} by matching name,
   * or null if no match is found.</P>
   */

  public NameSpace getNameSpace(String spaceName) throws RemoteException;

  /**
   * <P>This method creates a new {@link arlut.csd.ganymede.DBNameSpace DBNameSpace} 
   * object and returns a remote handle
   * to it so that the admin client can set attributes on the DBNameSpace,
   * and generally make a nuisance of itself.</P>
   */

  public NameSpace createNewNameSpace(String name, boolean caseInsensitive) throws RemoteException;

  /**
   * <P>This method deletes a
   *  {@link arlut.csd.ganymede.DBNameSpace DBNameSpace} object, returning true if
   * the deletion could be carried out, false otherwise.</P>
   */

  public ReturnVal deleteNameSpace(String name) throws RemoteException;

  /**
   * <P>Commit this schema edit, instantiate the modified schema</P>
   *
   * <P>It is an error to attempt any schema editing operations after this
   * method has been called.</P>
   */

  public void commit() throws RemoteException;

  /**
   * <P>Abort this schema edit, return the schema to its prior state.</P>
   *
   * <P>It is an error to attempt any schema editing operations after this
   * method has been called.</P>
   */

  public void release() throws RemoteException;
}
