/*

   Client.java

   Server-side interface for the Client object.

   Created: 28 May 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

public interface Client extends Remote {
  String getName() throws RemoteException;
  String getPassword() throws RemoteException;
  void forceDisconnect(String reason) throws RemoteException;
}
