/*

   ClientBase.java

   The core of a client.  Provides all the logic necessary to establish
   a connection to the server and get logged in.  By using this class,
   the server will only need an RMI stub for this class, regardless of
   what client is written.
   
   Created: 31 March 1998
   Version: $Revision: 1.5 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      ClientBase

------------------------------------------------------------------------------*/

/**
 *
 * The core of a client.  Provides all the logic necessary to
 * establish a connection to the server and get logged in.  By using
 * this class, the server will only need an RMI stub for this class,
 * regardless of what client is written.
 *
 */

public class ClientBase extends UnicastRemoteObject implements Client {

  private final static boolean debug = false;

  // ---

  /**
   *
   * RMI reference to a Ganymede server
   *
   */

  private Server server = null;

  /**
   *
   * RMI reference to a client session on a Ganymede server
   * 
   */

  private Session session = null;
 
  private String username = null;
  private String password = null;
  private boolean connected = false;
  private Vector listeners = new Vector();

  /* -- */

  /**
   *
   * This constructor takes a URL for the Ganymede server to connect to, a
   * reference to an object implementing the ClientListener interface to
   * report problems.  The constructor will establish an initial connection
   * to the server and prepare itself for subsequent login before returning.
   *
   * @param serverURL An rmi:// URL for a Ganymede server.
   * @param listener A ClientListener to report problems and disconnection to.
   *
   */

  public ClientBase(String serverURL, ClientListener listener) throws RemoteException
  {
    super();    // UnicastRemoteObject can throw RemoteException 

    /* -- */

    if (listener == null || serverURL == null || serverURL.length() == 0)
      {
	throw new IllegalArgumentException("bad argument");
      }

    listeners.addElement(listener);

    if (debug)
      {
	System.err.println("Initializing BaseClient object");
      }

    try
      {
	connected = true;

	Remote obj = Naming.lookup(serverURL);
	  
	if (obj instanceof Server)
	  {
	    server = (Server) obj;
	  }
      }
    catch (NotBoundException ex)
      {
	connected = false;
	
	System.err.println("RMI: Couldn't bind to server object\n" + ex );
      }
    catch (java.rmi.UnknownHostException ex)
      {
	connected = false;
	
	System.err.println("RMI: Couldn't find server\n" + serverURL );
      }
    catch (java.net.MalformedURLException ex)
      {
	connected = false;
	  	  
	System.err.println("RMI: Malformed URL " + serverURL );
      }
  }

  /**
   *
   * This method is used by a client to actually get logged into the
   * server.  The Session handle returned is then used to do all
   * server operations appropriate for a normal client.  Calling the
   * Session logout() method will end the client's connection to the
   * server.
   *
   * @return null if login failed, else a valid server Session reference
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public Session login(String username, String password) throws RemoteException
  {
    // isLoggedIn always returns false, for now.  There should be a logout() method,
    // which would fix everything up.  Maybe for dev2.
    if (isLoggedIn())
      {
	throw new IllegalArgumentException("Already logged in.  Construct a " +
					   "new ClientBase if you need to login again");
      }

    this.username = username;
    this.password = password;

    try
      {
	session = server.login(this);
	
	if (session == null)
	  {
	    sendErrorMessage("Couldn't log in to server... bad username/password?");
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("logged in");
	      }
	  }
      }
    catch (NullPointerException ex)
      {
	if (debug)
	  {
	    System.err.println("Error: Didn't get server reference.  Exiting now.");
	  }

	sendErrorMessage( "Error: Didn't get server reference.  Exiting now.");
      }
    catch (Exception ex)
      {
	if (debug)
	  {
	    System.err.println("Got some other exception: " + ex);
	  }

	sendErrorMessage("Got some other exception: " + ex);
      }
  
    return session;
  }

  /**
   *
   * This method returns true if the client has already logged in.
   *
   * This is not implemented now; it will always return false.
   */

  public boolean isLoggedIn()
  {
    // This can be reinstated after a logout() method clears the session.  We
    // don't have a logout() method yet; it is on the to-do list.
    //return session != null;
    return false;
  }

  /**
   *
   * This method can be used to retrieve a handle to the client's
   * login session.  This simply returns the same handle that
   * login() returned, in case the client forgets it or something.
   *
   */

  public Session getSession()
  {
    return session;
  }

  /**
   *
   * This method returns true if the client holds a valid reference to
   * the server.  This will always return true unless the server has
   * forced a disconnect.
   * 
   */

  public boolean isConnected()
  {
    return connected;
  }

  /**
   *
   * Register a client listener.
   *
   */

  public synchronized void addClientListener(ClientListener l)
  {
    listeners.addElement(l);
  }

  /**
   *
   * Remove a client listener.
   *
   */

  public synchronized void removeClientListener(ClientListener l)
  {
    listeners.removeElement(l);
  }

  /**
   *
   * Calls the logout() method on the Session object.  This
   * could be done by the client using the Session reference
   * returned by the login() method, but using this method
   * allows us to reflect login status internally.
   *
   */

  public void disconnect() throws RemoteException
  {
    session.logout();
    session = null;
  }

  // **
  //
  // The following three methods implement the 
  // arlut.csd.ganymede.Client interface that the server
  // needs in order to talk to us.
  //
  // **

  /**
   *
   * Allows the server to retrieve the username.
   *
   * @see arlut.csd.ganymede.Client
   *
   */

  public String getName() 
  {
    return username;
  }

  /**
   *
   * Allows the server to retrieve the password.
   *
   * @see arlut.csd.ganymede.Client
   *
   */

  public String getPassword()
  {
    return password;
  }

  /**
   *
   * Allows the server to force us off when it goes down.
   *
   * @see arlut.csd.ganymede.Client
   *
   */

  public synchronized void forceDisconnect(String reason)
  {
    connected = false;
    session = null;

    ClientEvent e = new ClientEvent("Server forced disconect: " + reason);

    for (int i = 0; i < listeners.size(); i++)
      {
	((ClientListener)listeners.elementAt(i)).disconnected(e);
      }
  }

  // ***
  //
  // Private convenience methods
  //
  // ***

  /**
   *
   * Private method to inform clientListeners if we get an error
   * from the server after construction..
   *
   */

  private synchronized void sendErrorMessage(String message)
  {
    ClientEvent e = new ClientEvent(message);

    for (int i = 0; i < listeners.size(); i++)
      { 
	((ClientListener)listeners.elementAt(i)).messageReceived(e);
      }
  }
}
