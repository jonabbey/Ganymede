/*

   adminSession.java

   Client side remote interface.

   Client side interface definition for the Ganymede adminSession Object.  The
   Ganymede adminSession object holds the state for the Ganymede Admin console.

   Created: 28 May 1996
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.ganymede;

import java.rmi.*;
import java.util.Date;

public interface adminSession extends Remote {

  // Client/server interface operations

  void        logout() throws RemoteException;

  /**
   *
   * This method lets the admin console explicitly request
   * a refresh.
   *
   */

  void        refreshMe() throws RemoteException;
  boolean     kill(String user) throws RemoteException;
  boolean     killAll() throws RemoteException;
  String      getInfo(String user) throws RemoteException;
  boolean     shutdown() throws RemoteException;
  boolean     dumpDB() throws RemoteException;
  boolean     dumpSchema() throws RemoteException;
  boolean     reloadCustomClasses() throws RemoteException;
  void        runInvidTest() throws RemoteException;
  void        runInvidSweep() throws RemoteException;

  boolean     runTaskNow(String name) throws RemoteException;
  boolean     stopTask(String name) throws RemoteException;
  boolean     disableTask(String name) throws RemoteException;
  boolean     enableTask(String name) throws RemoteException;
  boolean     rescheduleTask(String name, Date time, int interval) throws RemoteException;

  SchemaEdit  editSchema() throws RemoteException;
}
