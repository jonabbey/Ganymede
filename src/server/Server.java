/*

   Server.java

   Client side remote interface.

   Remote interface definition for the Ganymede Server Object.  The
   Ganymede Server object provides the interface that clients use to
   log in to the Ganymede Server.

   Created: 1 April 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

// Server is our remote access interface 

public interface Server extends Remote {
  Session login(Client client) throws RemoteException;
  adminSession admin(Admin admin) throws RemoteException;
}
