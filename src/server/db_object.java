package arlut.csd.ganymede;

import java.util.Date;
import java.rmi.RemoteException;

/**
 *
 * Base class for GANYMEDE client-visible objects.
 *
 * @version $Revision: 1.3 $ %D% (Original file created 11 April 1996)
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
   * Returns the numeric id of the object in the objectBase
   *
   */

  public short getTypeID() throws RemoteException;

  /**
   *
   * Returns the unique object id (invid) of this object
   *
   */

  public Invid getInvid() throws RemoteException;

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
   * <p>Get list of DBFields contained in this object.</p>
   *
   */

  public db_field[] listFields() throws RemoteException;

  /**
   *
   * Returns the primary label of this object.
   *
   */

  public String getLabel() throws RemoteException;

  /**
   *
   * <p>Returns true if the last field change peformed on this
   * object necessitates the client rescanning this object to
   * reveal previously invisible fields or to hide previously
   * visible fields.</p>
   *
   * <p>shouldRescan() will reset itself after returning true.</p>
   *
   */

  public boolean shouldRescan() throws RemoteException;

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
}
