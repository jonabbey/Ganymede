/**
 * iClient does all the heavy lifting to connect the server with the client, and
 * provides callbacks that the server can use to notify the client when something
 * happens.
 */

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;

//import java.awt.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.Vector;

public class ClientBase extends UnicastRemoteObject implements Client {

  private final boolean debug = true;

  private Server server = null;
  private String username = null;
  private String password = null;
  private Session session = null;

  private boolean connected = false;

  private String server_url;

  Vector listeners = new Vector();
  /* -- */

  public ClientBase(String serverURL, ClientListener listener) throws RemoteException
  {
    super();

    this.server_url = serverURL;
    listeners.addElement(listener);
    // UnicastRemoteObject can throw RemoteException 

    if (debug)
      {
	System.err.println("Initializing BaseClient object");
      }

    try
      {
	connected = true;

	Remote obj = Naming.lookup(server_url);
	  
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
	
	System.err.println("RMI: Couldn't find server\n" + server_url );
      }
    catch (java.net.MalformedURLException ex)
      {
	connected = false;
	  	  
	System.err.println("RMI: Malformed URL " + server_url );
      }


  }

  public Session getSession()
  {
    return session;
  }

  public Session login(String username, String password) throws RemoteException
  {

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
	    System.out.println("logged in");
	  }
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	//System.exit(0);
	
	sendErrorMessage( "Error: Didn't get server reference.  Exiting now.");
	
      }
    catch (Exception ex)
      {
	System.err.println("Got some other exception: " + ex);
      }
  
    return session;

  }

  /**
   * Register a client listener.
   */
  public void addClientListener(ClientListener l)
  {
    listeners.addElement(l);
  }

  /**
   * Remove a client listener.
   */
  public void removeClientListener(ClientListener l)
  {
    listeners.removeElement(l);
  }


  /**
   * Calls the logout() method on the Session object
   */
  public void disconnect() throws RemoteException
  {
    session.logout();
  }

  /**
   * Allows the server to retrieve the username
   */
  public String getName() 
  {
    return username;
  }

  /**
   * Allows the server to retrieve the password
   */
  public String getPassword()
  {
    return password;
  }

  /**
   * Allows the server to force us off when it goes down
   */
  public void forceDisconnect(String reason)
  {
    ClientEvent e = new ClientEvent("Server forced disconect: " + reason);
    for (int i = 0; i < listeners.size(); i++)
      {
	((ClientListener)listeners.elementAt(i)).disconnected(e);
      }
  }

  private void sendErrorMessage(String message)
  {
    ClientEvent e = new ClientEvent(message);
    for (int i = 0; i < listeners.size(); i++)
      { 
	((ClientListener)listeners.elementAt(i)).messageReceived(e);
      }
  }
}
