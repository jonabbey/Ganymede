/*

   perm_field.java

   Client-side interface to the PermissionMatrixDBField class.
   
   Created: 27 June 1997
   Release: $Name:  $
   Version: $Revision: 1.8 $
   Last Mod Date: $Date: 1999/01/22 18:06:00 $
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

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      perm_field

------------------------------------------------------------------------------*/

/**
 *
 * Client-side interface to the PermissionMatrixDBField class.
 *
 */

public interface perm_field extends db_field {

  /**
   *
   * Return a serializable, read-only copy of this field's permission
   * matrix
   *
   */

  public PermMatrix getMatrix() throws RemoteException;

  /**
   *
   * Return a serializable, read-only copy of the maximum permissions
   * that can be set for this field's permission matrix.  This matrix
   * is drawn from the union of delegatable roles that the client's
   * adminPersona is a member of.<br><br>
   * 
   * This method will return null if this perm_field is not associated
   * with an object that is being edited, or if the client is logged
   * into the server as supergash.
   * 
   */

  public PermMatrix getTemplateMatrix() throws RemoteException;

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;<br><br>
   *
   * If this permissions field has no explicit record for the specified
   * field, the default permissions value for this base will be returned
   * if defined, else getPerm() will return null.
   *
   * @see arlut.csd.ganymede.PermEntry
   */

  public PermEntry getPerm(short baseID, short fieldID) throws RemoteException;

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.PermEntry
   */

  public PermEntry getPerm(short baseID) throws RemoteException;

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;<br><br>
   *
   * If this permissions field has no explicit record for the specified
   * field, the default permissions value for this base will be returned
   * if defined, else getPerm() will return null.
   *
   * @see arlut.csd.ganymede.PermEntry
   */

  public PermEntry getPerm(Base base, BaseField field) throws RemoteException;

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.PermEntry
   */

  public PermEntry getPerm(Base base) throws RemoteException;

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermEntry 
   */

  public ReturnVal setPerm(short baseID, short fieldID, PermEntry entry) throws RemoteException;

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermEntry
   */

  public ReturnVal setPerm(short baseID, PermEntry entry) throws RemoteException;

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermEntry 
   */

  public ReturnVal setPerm(Base base, BaseField field, PermEntry entry) throws RemoteException;

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermEntry
   */

  public ReturnVal setPerm(Base base, PermEntry entry) throws RemoteException;

  /**
   * Sets the default permission entry to apply to fields under base
   * &lt;baseID&gt; to PermEntry &lt;entry&gt;  Once the default fields
   * permission is set for a base, all new fields created under that
   * base will take the default permissions entry.  If the schema editor
   * is used to explicitly set the permissions for a field (even to 
   * no-permissions), that will override the default.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.<br><br>
   *
   * This operation may only be performed by a GanymedeSession 
   * with supergash-level privileges, for security's sake.
   *
   */

  public ReturnVal setDefaultFieldsPerm(short baseID, PermEntry entry) throws RemoteException;

  /**
   * Sets the default permission entry to apply to fields under base
   * &lt;baseID&gt; to PermEntry &lt;entry&gt;  Once the default fields
   * permission is set for a base, all new fields created under that
   * base will take the default permissions entry.  If the schema editor
   * is used to explicitly set the permissions for a field (even to 
   * no-permissions), that will override the default.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.<br><br>
   *
   * This operation may only be performed by a GanymedeSession 
   * with supergash-level privileges, for security's sake.
   *
   */

  public ReturnVal setDefaultFieldsPerm(Base base, PermEntry entry) throws RemoteException;
}
