/*

   Admin.java

   Server-side interface for the Admin callbacks.

   The methods in the Admin interface are implemented by
   the Admin console, and are called by the server as
   appropriate.

   Created: 28 May 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

public interface Admin extends Remote {
  String getPassword() throws RemoteException;
  void forceDisconnect(String reason) throws RemoteException;

  void setServerStart(Date date) throws RemoteException;
  void setLastDumpTime(Date date) throws RemoteException;
  void setTransactionsInJournal(int trans) throws RemoteException;
  void setObjectsCheckedOut(int objs) throws RemoteException;
  void setLocksHeld(int locks) throws RemoteException;

  void changeState(String state) throws RemoteException;
  void changeStatus(String status) throws RemoteException;
  void changeAdmins(String adminStatus) throws RemoteException;
  void changeUsers(Vector entries) throws RemoteException;
}
