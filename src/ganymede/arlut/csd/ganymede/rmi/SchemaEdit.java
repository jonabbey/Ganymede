/*

   SchemaEdit.java

   Client side interface for schema editing

   Created: 17 April 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      SchemaEdit

------------------------------------------------------------------------------*/

/**
 * <p>Primary remote interface for Ganymede schema editing.  The
 * Ganymede admin console calls the
 * {@link arlut.csd.ganymede.server.GanymedeAdmin#editSchema() editSchema()} method on
 * a server-side {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} object
 * to get access to
 * the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects which
 * hold the type definitions for the objects held in the Ganymede server.  The
 * DBObjectBase objects in turn provide access to the definitions for
 * the fields held in those object types.</p>
 */

public interface SchemaEdit extends Remote {

  //
  // From here on are the normal schema editing methods
  //

  /**
   * <p>Returns the root category node from the server</p>
   */

  public Category getRootCategory() throws RemoteException;

  /**
   * <p>Returns a list of bases from the current (non-committed) state of the system.</p>
   *
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   */

  public Base[] getBases(boolean embedded) throws RemoteException;

  /**
   * <p>Returns a list of bases from the current (non-committed) state of the system.</p>
   */

  public Base[] getBases() throws RemoteException;

  /**
   * <p>Returns a {@link arlut.csd.ganymede.rmi.Base Base} reference to
   * match the id, or null if no match.</p>
   */

  public Base getBase(short id) throws RemoteException;

  /**
   * <p>Returns a {@link arlut.csd.ganymede.rmi.Base Base} reference to
   * match the baseName, or null if no match.</p>
   */

  public Base getBase(String baseName) throws RemoteException;

  /**
   * <p>This method creates a new {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} object and returns
   * a remote handle to it so that the admin client can set fields on
   * the base, set attributes, and generally make a nuisance of
   * itself.</p>
   */

  public Base createNewBase(Category category, boolean embedded, boolean lowRange) throws RemoteException;

  /**
   * <p>This method deletes a {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}, removing it from the
   * Schema Editor's working set of bases.  The removal won't
   * take place for real unless the SchemaEdit is committed.</p>
   */

  public ReturnVal deleteBase(String baseName) throws RemoteException;

  /**
   * <p>This method returns an array of defined
   * {@link arlut.csd.ganymede.rmi.NameSpace NameSpace} objects.</p>
   */

  public NameSpace[] getNameSpaces() throws RemoteException;

  /**
   * <p>This method returns a {@link arlut.csd.ganymede.rmi.NameSpace NameSpace} by matching name,
   * or null if no match is found.</p>
   */

  public NameSpace getNameSpace(String spaceName) throws RemoteException;

  /**
   * <p>This method creates a new {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace}
   * object and returns a remote handle
   * to it so that the admin client can set attributes on the DBNameSpace,
   * and generally make a nuisance of itself.</p>
   */

  public NameSpace createNewNameSpace(String name, boolean caseInsensitive) throws RemoteException;

  /**
   * <p>This method deletes a
   *  {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace} object, returning true if
   * the deletion could be carried out, false otherwise.</p>
   */

  public ReturnVal deleteNameSpace(String name) throws RemoteException;

  /**
   * <p>This method deletes all {@link
   * arlut.csd.ganymede.server.DBNameSpace DBNameSpace} objects in the
   * server's schema that are not currently attached to any
   * namespace-constrained field.</p>
   */

  public ReturnVal deleteUnusedNameSpaces() throws RemoteException;

  /**
   * <p>Commit this schema edit, instantiate the modified schema</p>
   *
   * <p>It is an error to attempt any schema editing operations after this
   * method has been called.</p>
   */

  public ReturnVal commit() throws RemoteException;

  /**
   * <p>Abort this schema edit, return the schema to its prior state.</p>
   *
   * <p>It is an error to attempt any schema editing operations after this
   * method has been called.</p>
   */

  public void release() throws RemoteException;
}
