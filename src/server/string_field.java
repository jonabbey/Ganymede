/*

   string_field.java

   Remote interface definition.

   Created: 14 November 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;
import java.util.*;

public interface string_field extends db_field {
  int maxSize() throws RemoteException;

  boolean showEcho() throws RemoteException;
  boolean canChoose() throws RemoteException;
  boolean mustChoose() throws RemoteException;

  String allowedChars() throws RemoteException;
  String disallowedChars() throws RemoteException;
  boolean allowed(char c) throws RemoteException;

  Vector choices() throws RemoteException;
}
