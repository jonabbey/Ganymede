/*

   invid_field.java

   Remote interface definition.

   Created: 14 November 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;
import java.util.*;
import java.rmi.RemoteException;

public interface invid_field extends db_field {
  boolean limited() throws RemoteException;
  int getAllowedTarget() throws RemoteException;
  Vector choices() throws RemoteException;
}
