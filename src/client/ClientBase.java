/**
 * iClient does all the heavy lifting to connect the server with the client, and
 * provides callbacks that the server can use to notify the client when something
 * happens.
 */

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;

import java.awt.*;
import java.rmi.*;
import java.rmi.server.*;


import arlut.csd.JDialog.*;

public class ClientBase extends UnicastRemoteObject implements Client {

  protected Server server = null;
  protected String username = null;
  protected String password = null;
  protected Session session = null;

  /* -- */

  public iClient(glogin applet, Server server, String username, String password) throws RemoteException
  {
    super();

    // UnicastRemoteObject can throw RemoteException 

    this.applet = applet;
    this.server = server;
    this.username = username;
    this.password = password;

    System.err.println("Initializing iClient object");

    try
      {
	session = server.login(this);

	if (session == null)
	  {
	    System.err.println("Couldn't log in to server... bad username/password?");

	    JErrorDialog d = new JErrorDialog(applet.my_frame, "Couldn't log in to server... bad username/password?");
	  }
	else
	  {
	    System.out.println("logged in");
	  }
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());

	JErrorDialog d = new JErrorDialog(applet.my_frame, "RMI Error: Couldn't log in to server.\n" + ex.getMessage());
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	//System.exit(0);

	JErrorDialog d = new JErrorDialog(applet.my_frame, "Error: Didn't get server reference.  Exiting now.");
	
      }
    catch (Exception ex)
      {
	System.err.println("Got some other exception: " + ex);
      }

    //    System.err.println("Got session");

/*    try
      {
	Type typeList[] = session.types();
	
	for (int i=0; i < typeList.length; i++)
	  {
	    System.err.println("Type: " + typeList[i]);
	  }
      }
    catch (Exception ex)
      {
	System.err.println("typecatch: " + ex);
      }
*/	
  }

  public Session getSession()
  {
    return session;
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
    System.err.println("Server forced disconnect: " + reason);
    
    final String finalReason = reason;

    // Need to spawn off another thread here, so forceDisconnect can return.  Server
    // will wait for forceDisconnect to return, so it will hang if we don't.

    Runnable shutdown = new Runnable() {
      public void run() {
	if (applet.g_client != null)
	  {
	    
	    applet.g_client.dispose();
	    
	    applet.g_client = null;
	  }
	
	JErrorDialog d = new JErrorDialog(new com.sun.java.swing.JFrame(), "You are not worthy", "Press ok to disconnect.\n\n " + finalReason);
	
	
	System.exit(0);
      }
    };
    
    Thread thread = new Thread(shutdown);
    thread.start();
  }
}
