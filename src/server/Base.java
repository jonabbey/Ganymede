/*

   Base.java

   Client side interface to the object type dictionary
   
   Created: 17 April 1997
   Version: $Revision: 1.11 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
