/*

   perm_field.java

   Client-side interface to the PermissionMatrixDBField class.
   
   Created: 27 June 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      perm_field

------------------------------------------------------------------------------*/

public interface perm_field extends db_field {
  public PermMatrix getMatrix() throws RemoteException;
  public PermEntry getPerm(short baseID, short fieldID) throws RemoteException;
  public PermEntry getPerm(short baseID) throws RemoteException;
  public PermEntry getPerm(Base base, BaseField field) throws RemoteException;
  public PermEntry getPerm(Base base) throws RemoteException;
  public void setPerm(short baseID, short fieldID, PermEntry entry) throws RemoteException;
  public void setPerm(short baseID, PermEntry entry) throws RemoteException;
  public void setPerm(Base base, BaseField field, PermEntry entry) throws RemoteException;
  public void setPerm(Base base, PermEntry entry) throws RemoteException;
}
