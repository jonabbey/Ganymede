/*

   adminSession.java

   Client side remote interface.

   Client side interface definition for the Ganymede adminSession Object.  The
   Ganymede adminSession object holds the state for the Ganymede Admin console.

   Created: 28 May 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.ganymede;

import java.rmi.*;

public interface adminSession extends Remote {

  // Client/server interface operations

  void        logout() throws RemoteException;
  boolean     kill(String user) throws RemoteException;
  boolean     killAll() throws RemoteException;
  String      getInfo(String user) throws RemoteException;
  boolean     shutdown() throws RemoteException;
  boolean     dumpDB() throws RemoteException;
  boolean     dumpSchema() throws RemoteException;

  SchemaEdit  editSchema() throws RemoteException;
}
