/*

   Base.java

   Client side interface to the object type dictionary
   
   Created: 17 April 1997
   Version: $Revision: 1.4 $ %D%
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

public interface Base extends Remote {
  public String getName() throws RemoteException;
  public String getClassName() throws RemoteException;
  public boolean canCreate(Session session) throws RemoteException;
  public boolean canInactivate() throws RemoteException;
  public short getTypeID() throws RemoteException;
  public Vector getFields() throws RemoteException;

  // the following methods are only valid when the Base reference
  // is obtained from a SchemaEdit reference.

  public void setName(String newName) throws RemoteException;
  public void setClassName(String newName) throws RemoteException;
  public BaseField createNewField() throws RemoteException;
  public boolean deleteField(BaseField bF) throws RemoteException;
  public boolean fieldInUse(BaseField bF) throws RemoteException;
}
