/*

   Session.java

   Client side remote interface.

   Client side interface definition for the Ganymede Session Object.  The
   Ganymede Session object holds the state for a Ganymede client's session
   with the Ganymede server.  The Ganymede session will also provide the
   primary interface for accessing ganymede db objects.

   Created: 1 April 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                         Session

------------------------------------------------------------------------------*/

/**
 *   Client side interface definition for the Ganymede Session Object.  The
 *   Ganymede Session object holds the state for a .Ganymede client's session
 *   with the Ganymede server.  The Ganymede session will also provide the
 *   primary interface for accessing ganymede db objects.
 *
 * @see arlut.csd.ganymede.DBSession
 */

public interface Session extends Remote {

  // Client/server interface operations

  String      getLastError() throws RemoteException;
  boolean     set_admin_info() throws RemoteException;
  void        logout() throws RemoteException;

  //  Database operations

  /**
   *
   * List types of objects stored and manipulated through the Ganymede server.
   *
   * This method returns an enumeration of Base remote references.
   *
   */

  Vector      getTypes() throws RemoteException;

  /**
   *
   * This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).
   *
   * Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an exception will be thrown.
   * 
   */

  void     openTransaction() throws RemoteException;

  /**
   *
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients, or to subsequent queries and view_db_object() calls by
   * this client.
   *
   * If the transaction cannot be committed for some reason,
   * commitTransaction() will instead abort the transaction.  In any
   * case, calling commitTransaction() will close the transaction.
   *
   * @returns false if the transaction could not be committed.  
   *                getLastError() can be called to obtain an explanation
   *                of commit failure.
   * 
   */

  boolean  commitTransaction() throws RemoteException;

  /**
   *
   * This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.
   *
   */
  
  void     abortTransaction() throws RemoteException;

  /**
   *
   * List objects in the database meeting the given query criteria.
   *
   * The database will be read-locked during the query, assuring
   * a transaction-consistent view of the database.
   *
   */

  Result[]    query(Query query) throws RemoteException;

  /**
   *
   * View an object from the database.  If the return value is null,
   * getLastError() should be called for a description of the problem.
   *
   * view_db_object() can be done at any time, outside of the bounds of
   * any transaction.  view_db_object() returns a snapshot of the object's
   * state at the time the view_db_object() call is processed, and will
   * be transaction-consistent internally.
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

  db_object   edit_db_object(Invid invid) throws RemoteException;

  /**
   *
   * Create a new object of the given type. If the return value is null,
   * getLastError() should be called for a description of the problem. 
   *
   * @return the newly created object for editing
   *
   */

  db_object   create_db_object(short type) throws RemoteException;

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

  /**
   *
   * Remove an object from the database
   *
   * Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   *
   * @return true if the object was removed, if false, check
   * getLastError()
   *
   */

  boolean     remove_db_object(Invid invid) throws RemoteException;
}

