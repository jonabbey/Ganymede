/*

   ClientBase.java

   The core of a client.  Provides all the logic necessary to establish
   a connection to the server and get logged in.  By using this class,
   the server will only need an RMI stub for this class, regardless of
   what client is written.
   
   Created: 31 March 1998
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 2000/03/29 01:29:47 $
   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
 * <p>The core of a client.  Provides all the logic necessary to
 * establish a connection to the server and get logged in.  By using
 * this class, the server will only need an RMI stub for this class,
 * regardless of what client is written.</p>
 *
 * @version $Revision: 1.15 $ $Date: 2000/03/29 01:29:47 $ $Name:  $
 * @author Mike Mulvaney
 */

public class ClientBase extends UnicastRemoteObject implements Client {

  private final static boolean debug = false;

  // ---

  /**
   * RMI reference to a Ganymede server
   */

  private Server server = null;

  /**
   * RMI reference to a client session on a Ganymede server
   */

  private Session session = null;
 
  private String username = null;
  private String password = null;
  private boolean connected = false;
  private Vector listeners = new Vector();

  /* -- */

  /**
   * <p>This constructor takes a URL for the Ganymede server to connect to, a
   * reference to an object implementing the ClientListener interface to
   * report problems.  The constructor will establish an initial connection
   * to the server and prepare itself for subsequent login before returning.</p>
   *
   * @param serverURL An rmi:// URL for a Ganymede server.
   * @param listener A ClientListener to report problems and disconnection to.
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
	    boolean test = server.up();
	  }
      }
    catch (NotBoundException ex)
      {
	connected = false;
	
	if (debug)
	  {
	    System.err.println("RMI: Couldn't bind to server url " + serverURL + "\n" + ex );
	  }
	
	sendErrorMessage("RMI: Couldn't bind to server url " + serverURL + "\n" + ex );
      }
    catch (java.rmi.UnknownHostException ex)
      {
	connected = false;
	
	if (debug)
	  {
	    System.err.println("RMI: Couldn't find server\n" + serverURL );
	  }
	
	sendErrorMessage("RMI: Couldn't find server\n" + serverURL );
      }
    catch (java.net.MalformedURLException ex)
      {
	connected = false;
	
	if (debug)
	  {
	    System.err.println("RMI: Malformed URL " + serverURL );
	  }
	
	sendErrorMessage("RMI: Malformed URL " + serverURL );
      }
  }

  /**
   * <p>This method is used by a client to actually get logged into the
   * server.  The {@link arlut.csd.ganymede.Session Session} handle
   * returned is then used to do all server operations appropriate 
   * for a normal client.  Calling the Session logout() method will
   * end the client's connection to the server.</p>
   *
   * @return null if login failed, else a valid server Session reference
   *
   * @see arlut.csd.ganymede.Session
   */

  public Session login(String username, String password) throws RemoteException
  {
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
	connected = false;

	if (debug)
	  {
	    System.err.println("Error: Didn't get server reference.  Exiting now.");
	  }

	sendErrorMessage( "Error: Didn't get server reference.  Exiting now.");
      }
    catch (Exception ex)
      {
	connected = false;

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
   */

  public boolean isLoggedIn()
  {
    return session != null;
  }

  /**
   * <p>This method can be used to retrieve a handle to the client's
   * login session.  This simply returns the same handle that
   * login() returned, in case the client forgets it or something.</p>
   */

  public Session getSession()
  {
    return session;
  }

  /**
   * <p>This method returns true if the client holds a valid reference to
   * the server.  This will always return true unless the server has
   * forced a disconnect.</p>
   */

  public boolean isConnected()
  {
    if (!connected)
      {
	return false;
      }

    boolean test = false;

    try
      {
	test = server.up();
      }
    catch (Exception ex)
      {
	return false;
      }

    return true;
  }

  /**
   * <p>Register a client listener.  A client listener is an object
   * that is to be notified if we get an asynchronous callback from
   * the Ganymede server, such as a forced log-off, or if we need
   * to report an error during login.</p>
   */

  public synchronized void addClientListener(ClientListener l)
  {
    listeners.addElement(l);
  }

  /**
   * <p>Remove a client listener.</p>
   */

  public synchronized void removeClientListener(ClientListener l)
  {
    listeners.removeElement(l);
  }

  /**
   * <p>Calls the logout() method on the Session object.  This
   * could be done by the client using the Session reference
   * returned by the login() method, but using this method
   * allows us to reflect login status internally.</p>
   */

  public void disconnect() throws RemoteException
  {
    if (session != null)
      {
	session.logout();
	session = null;
      }
  }

  // **
  //
  // The following three methods implement the 
  // arlut.csd.ganymede.Client interface that the server
  // needs in order to talk to us.
  //
  // **

  /**
   * <p>Allows the server to retrieve the username.</p>
   *
   * @see arlut.csd.ganymede.Client
   */

  public String getName() 
  {
    return username;
  }

  /**
   * <p>Allows the server to retrieve the password.</p>
   *
   * @see arlut.csd.ganymede.Client
   */

  public String getPassword()
  {
    return password;
  }

  /**
   * <p>Allows the server to force us off when it goes down.</p>
   *
   * @see arlut.csd.ganymede.Client
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

  /**
   * <p>Allows the server to send an asynchronous message to the
   * client..  Used by the server to tell the client when a build
   * is/is not being performed on the server.</P> 
   *
   * @see arlut.csd.ganymede.Client
   */

  public synchronized void sendMessage(int messageType, String status)
  {
    ClientEvent e = new ClientEvent(messageType, status);

    for (int i = 0; i < listeners.size(); i++)
      {
	((ClientListener)listeners.elementAt(i)).messageReceived(e);
      }
  }

  // ***
  //
  // Private convenience methods
  //
  // ***

  /**
   * <p>Private method to inform clientListeners if we get an error
   * from the server after construction..</p>
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
