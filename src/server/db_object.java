/*

   db_object.java

   This interface defines the methods that the client can remotely
   call on a object held in the Ganymede server.
   
   Created: 11 April 1996
   Version: $Revision: 1.14 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

package arlut.csd.ganymede;

import java.util.Date;
import java.util.Vector;
import java.rmi.RemoteException;

/**
 *
 * This interface defines the methods that the client can remotely
 * call on a object held in the Ganymede server.
 *
 * @version $Revision: 1.14 $ %D% (Original file created 11 April 1996)
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 *
 */

public interface db_object extends java.rmi.Remote {


  /**
   *
   * Returns the numeric id of the object in the objectBase
   *
   */

  public int getID() throws RemoteException;

  /**
   *
   * Returns the base id of the object's type
   *
   */

  public short getTypeID() throws RemoteException;

  /**
   *
   * Returns the name of the object's type
   *
   */

  public String getTypeName() throws RemoteException;

  /**
   *
   * Returns the unique object id (invid) of this object
   *
   */

  public Invid getInvid() throws RemoteException;

  /**
   *
   * Returns a vector of field information records, in display order.
   *
   * @see arlut.csd.ganymede.FieldInfo
   *
   * @param customOnly If true, built-in fields won't be included in the returned vector
   *
   */

  public Vector getFieldInfoVector(boolean customOnly) throws RemoteException;

  /**
   *
   * <p>Get access to a field from this object.</p>
   *
   * @param id The field code for the desired field of this object.
   *
   */

  public db_field getField(short id) throws RemoteException;

  /**
   *
   * <p>Get access to a field from this object, by name.</p>
   *
   * @param fieldname The fieldname for the desired field of this object
   *
   */

  public db_field getField(String fieldname) throws RemoteException;

  /**
   *
   * <p>Get access to the field that serves as this object's label</p>
   *
   * <p>Not all objects use simple field values as their labels.  If an
   * object has a calculated label, this method will return null.</p>
   *
   */

  public db_field getLabelField() throws RemoteException;

  /**
   *
   * <p>Get list of DBFields contained in this object.</p>
   *
   * @param customOnly If true, listFields will not include built-in fields
   *
   */

  public db_field[] listFields(boolean customOnly) throws RemoteException;

  /**
   *
   * Returns the primary label of this object.
   *
   */

  public String getLabel() throws RemoteException;

  /**
   *
   * Returns true if this object is an embedded type
   *
   */

  public boolean isEmbedded() throws RemoteException;


  /**
   *
   * <p>Returns true if inactivate() is a valid operation on
   * checked-out objects of this type.</p>
   *
   */

  public boolean canInactivate() throws RemoteException;

  /**
   *
   * <p>Returns true if this object has been inactivated and is
   * pending deletion.</p>
   *
   */

  public boolean isInactivated() throws RemoteException;

  /**
   *
   * <p>Returns the date that this object is to go through final removal
   * if it has been inactivated.</p>
   *
   */

  public Date getRemovalDate() throws RemoteException;

  /**
   *
   * <p>Returns true if this object has an expiration date set.</p>
   *
   */

  public boolean willExpire() throws RemoteException;

  /**
   *
   * <p>Returns the date that this object is to be automatically
   * inactivated if it has an expiration date set.</p>
   *
   */

  public Date getExpirationDate() throws RemoteException;

  /**
   *
   * Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   */

  public ReturnVal setFieldValue(short fieldID, Object value) throws RemoteException;

  /**
   *
   * Shortcut method to get a scalar field's value.  Using this
   * method saves a roundtrip to the server.
   *
   */

  public Object getFieldValue(short fieldID) throws RemoteException;

  /**
   *
   * Shortcut method to get a vector field's values.  Using this
   * method saves a roundtrip to the server.
   *
   */

  public Vector getFieldValues(short fieldID) throws RemoteException;

}
