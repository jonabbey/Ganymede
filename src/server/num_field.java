/*

   num_field.java

   Remote interface definition.

   Created: 14 November 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;

public interface num_field extends db_field {
  boolean limited() throws RemoteException;
  int getMinValue() throws RemoteException;
  int getMaxValue() throws RemoteException;
}
