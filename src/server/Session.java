/*

   Session.java

   Client side remote interface.

   Client side interface definition for the Ganymede Session Object.  The
   Ganymede Session object holds the state for a Ganymede client's session
   with the Ganymede server.  The Ganymede session will also provide the
   primary interface for accessing ganymede db objects.

   Created: 1 April 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                         Session

------------------------------------------------------------------------------*/

/**
 *   Client side interface definition for the Ganymede Session Object.  The
 *   Ganymede Session object holds the state for a .Ganymede client's session
 *   with the Ganymede server.  The Ganymede session will also provide the
 *   primary interface for accessing ganymede db objects.

 */

public interface Session extends Remote {

  // Client/server interface operations

  String      getLastError() throws RemoteException;
  boolean     set_admin_info() throws RemoteException;
  void        logout() throws RemoteException;

  //  Database operations

  /**
   *
   * List types of objects stored and manipulated through the Ganymede server
   *
   */

  Type[] types() throws RemoteException;

  /**
   *
   * List objects in the database meeting the given query criteria
   *
   */

  Result[]    query(Query query) throws RemoteException;

  /**
   *
   * View an object from the database.  If the return value is null,
   * getLastError() should be called for a description of the problem. 
   *
   */

  db_object   view_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Check an object out from the database for editing.  If the return
   * value is null, getLastError() should be called for a description
   * of the problem. 
   *
   */

  storable_object   edit_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Create a new object of the given type. If the return value is null,
   * getLastError() should be called for a description of the problem. 
   *
   * @return the newly created object for editing
   *
   */
  storable_object   create_db_object(int type) throws RemoteException;

  /**
   *
   * Inactivate an object in the database
   *
   * Objects inactivated will typically be altered to reflect their inactive
   * status, but the object itself might not be purged from the Ganymede
   * server for a defined period of time, to allow other network systems
   * to have time to do accounting, clean up, etc., before a user id or
   * network address is re-used.
   *
   * @return true if the object was inactivated, if false, check
   * getLastError()
   *
   */

  boolean     inactivate_db_object(Invid invid) throws RemoteException;
}

