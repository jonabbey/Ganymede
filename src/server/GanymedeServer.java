/*

   GanymedeServer.java

   The GanymedeServer object is created by Ganymede at start-up time
   and published to the net for client logins via RMI.  As such,
   the GanymedeServer object is the first Ganymede code that a client
   will directly interact with.
   
   Created: 17 January 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GanymedeServer

------------------------------------------------------------------------------*/

/**
 *
 * The GanymedeServer object is created by Ganymede at start-up time
 * and published to the net for client logins via RMI.  As such,
 * the GanymedeServer object is the first Ganymede code that a client
 * will directly interact with.
 *
 */

class GanymedeServer extends UnicastRemoteObject implements Server {

  static GanymedeServer server = null;
  static Vector sessions = new Vector();
  static Hashtable activeUsers = new Hashtable(); 
  private int limit;

  private String[] users = {
    "jonabbey",
    "imkris",
    "navin",
    "circle",
    "root"
  };

  /* -- */

  public GanymedeServer(int limit) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    if (server == null)
      {
	this.limit = limit;
	server = this;
      }
    else
      {
	Ganymede.debug("Error: attempted to start a second server");
	throw new RemoteException("Error: attempted to start a second server");
      }
  } 

  /**
   * Establishes a Session object in the server.  The Session object
   * contains all of the server's knowledge about a given client's
   * status.  This method is to be called by the client via RMI.  In
   * addition to returning a Session RMI reference to the client,
   * login() keeps a local reference to the Ganymede Session object
   * for the server's bookkeeping.
   *
   * Does login() need to be synchronized?  If two threads were doing
   * login() simultaneously, would they have their own copies of i?
   * Would clients.addElement() do the right thing?  Is vector.addElement()
   * synchronized?
   * 
   */
  public synchronized Session login(Client client) throws RemoteException
  {
    boolean found = false;

    for (int i = 0; i < users.length; i++)
      {
	try
	  {
	    if (users[i].equals(client.getName()))
	      {
		found = true;
	      }
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Couldn't get client's user name: " + ex + "\n");
	  }
      }

    if (found)
      {
	GanymedeSession session = new GanymedeSession(client);
 	Ganymede.debug("Client logged in: " + session.username);
	return (Session) session;
      }
    else
      {
	throw new RemoteException("No such user, couldn't log in");
      }
  }

  /**
   * Establishes an GanymedeAdmin object in the server. 
   *
   * Adds <admin> as a monitoring admin console
   *
   */
  public synchronized adminSession admin(Admin admin) throws RemoteException
  {
    try
      {
	if (!(admin.getPassword().equals("Dilbert")))
	  {
	    throw new RemoteException("Bad Admin Password"); // do we have to throw remote here?
	  }
      }
    catch (RemoteException ex)
      {
	Ganymede.debug("Couldn't get admin console login information: " + ex);
      }

    adminSession aSession = new GanymedeAdmin(admin);
    Ganymede.debug("Admin console attached" + new Date());
    return aSession;
  }
}
