/*

   perm_field.java

   Client-side interface to the PermissionMatrixDBField class.
   
   Created: 27 June 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
