/*

   boolean_field.java

   Remote interface definition.

   Created: 14 November 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

public interface boolean_field extends db_field {
  boolean labeled() throws RemoteException;
  String trueLabel() throws RemoteException;
  String falseLabel() throws RemoteException;
}
