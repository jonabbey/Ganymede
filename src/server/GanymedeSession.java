/*

   GanymedeSession.java

   The GanymedeSession class is the template for the server-side objects
   that track a client's login and provide operations to be performed in
   the Ganymede server.
   
   Created: 17 January 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package gash;

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 GanymedeSession

------------------------------------------------------------------------------*/

/**
 *
 * The GanymedeSession class is the template for the server-side objects
 * that track a client's login and provide operations to be performed in
 * the Ganymede server.
 *
 */

class GanymedeSession extends UnicastRemoteObject implements Session {

  GanymedeServer server;
  Client client;
  boolean logged_in;
  String username;
  String clienthost;
  Date connecttime;
  String lastError;

  /* -- */

  /**
   *
   * Constructor used to create a server-side attachment for a Ganymede
   * client.
   *
   * @param client Remote object exported by the client, provides id callbacks
   * 
   */
  
  GanymedeSession(Client client) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    String temp;
    int i=2;
    
    this.client = client;
    
    try
      {
	temp = client.getName();

	username = temp;

	// find a unique name for this user's session

	while (GanymedeServer.activeUsers.containsKey(username))
	  {
	    username = temp + i;
	    i++;
	  }
	
	GanymedeServer.activeUsers.put(username, username);

	logged_in = true;
      }
    catch (RemoteException ex)
      {
	Ganymede.debug("iSession: couldn't verify username on client" + ex);
	logged_in = false;
      }

    try
      {
	clienthost = getClientHost();
      }
    catch (ServerNotActiveException ex)
      {
	clienthost = "unknown";
      }

    // record our login time

    connecttime = new Date();

    GanymedeServer.sessions.addElement(this);
    GanymedeAdmin.refreshUsers();
  }

  //************************************************************
  //
  // Non-remote methods (for server-side code)
  //
  //************************************************************

  synchronized void setLastError(String status)
  {
    lastError = status;
    GanymedeAdmin.refreshUsers();
  }

  // if the server decides this person needs to get off
  // (if the user times out, is forced off by an admin, the
  // server is going down),
  // it will call this method to knock them off.

  void forceOff(String reason)
  {
    Ganymede.debug("Forcing " + username + "off for " + reason);

    if (client != null)
      {
	try
	  {
	    client.forceDisconnect(reason);
	  }
	catch (RemoteException e)
	  {
	  }
      }

    this.logout();
  }

  //************************************************************
  //
  // All methods from this point on are part of the Server remote
  // interface, and can be called by the client via RMI.
  //
  //************************************************************

  /** 
   * getLastError() returns text explaining the last
   * error condition 
   */
  
  synchronized public String getLastError()
  {
    return lastError;
  }

  public boolean set_admin_info()
  {
    return true;
  }

  synchronized public void logout()
  {
    Ganymede.debug("User " + username + " logging off");
    logged_in = false;
    this.client = null;

    GanymedeServer.sessions.removeElement(this);
    GanymedeServer.activeUsers.remove(username);
    GanymedeAdmin.refreshUsers();

    Ganymede.debug("User " + username + " logged off");

    this.username = null;
    this.lastError = null;
  }

  //  Database operations

  public Type[] types()
  {
    return Ganymede.schema.types;
  }

  public Result[] query(Query query)
  {
    return null;
  }

  public data_object view_db_object(Invid invid)
  {
    return null;
  }

  public storable_object edit_db_object(Invid invid)
  {
    return null;
  }

  public storable_object create_db_object(int type) 
  {
    return null;
  }
  
  public boolean inactivate_db_object(Invid invid) 
  {
    return false;
  }

}
