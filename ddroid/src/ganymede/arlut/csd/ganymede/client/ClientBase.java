/*

   ClientBase.java

   The core of a client.  Provides all the logic necessary to establish
   a connection to the server and get logged in.  By using this class,
   the server will only need an RMI stub for this class, regardless of
   what client is written.
   
   Created: 31 March 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.clientAsyncMessage;
import arlut.csd.ganymede.rmi.ClientAsyncResponder;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.XMLSession;

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
 * @version $Id$
 * @author Mike Mulvaney
 */

public class ClientBase implements Runnable {

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

  /**
   * RMI reference to an asynchronous message port on the Ganymede
   * server
   */

  private ClientAsyncResponder asyncPort = null;

  /**
   * Thread that we'll create to continuously do a blocking poll on
   * the Ganymede server for asynchronous messages.
   */

  private Thread asyncThread = null;

  /**
   * RMI reference to a client XMLSession on a Ganymede server
   */

  private XMLSession xSession = null;
 
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
	    server.up();
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
   * server.  The {@link arlut.csd.ganymede.rmi.Session Session} handle
   * returned is then used to do all server operations appropriate 
   * for a normal client.  Calling the Session logout() method will
   * end the client's connection to the server.</p>
   *
   * @return null if login failed, else a valid server Session reference
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Session login(String username, String password) throws RemoteException
  {
    if (isLoggedIn())
      {
	throw new IllegalArgumentException("Already logged in.  Construct a " +
					   "new ClientBase if you need to login again");
      }

    try
      {
	// the server may send us a message using our
	// forceDisconnect() method during the login process

	ReturnVal retVal = server.login(username, password);

	if (retVal.didSucceed())
	  {
	    session = retVal.getSession();
	  }

	if (!retVal.didSucceed() || session == null)
	  {
	    String error = retVal.getDialogText();

	    if (error != null && !error.equals(""))
	      {
		sendErrorMessage(error);
	      }
	    else
	      {
		sendErrorMessage("Couldn't login to server... bad username/password?");
	      }

	    return null;
	  }

	if (debug)
	  {
	    System.out.println("logged in");
	  }

	asyncPort = session.getAsyncPort();

	if (asyncPort != null)
	  {
	    asyncThread = new Thread(this, "Ganymede Async Reader");
	    asyncThread.start();
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
   * <p>This method is used by a client to actually get logged into
   * the server.  The {@link arlut.csd.ganymede.rmi.XMLSession
   * XMLSession} handle returned is then used to do all server
   * operations appropriate for the xml client.  Calling the XMLSession
   * xmlEnd() method will end the client's connection to the
   * server.</p>
   *
   * @return null if login failed, else a valid server Session reference
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public XMLSession xmlLogin(String username, String password) throws RemoteException
  {
    if (isLoggedIn())
      {
	throw new IllegalArgumentException("Already logged in.  Construct a " +
					   "new ClientBase if you need to login again");
      }

    try
      {
	// the server may send us a message using our
	// forceDisconnect() method during the login process

	ReturnVal retVal = server.xmlLogin(username, password);

	if (retVal.didSucceed())
	  {
	    xSession = retVal.getXMLSession();
	  }

	if (!retVal.didSucceed() || xSession == null)
	  {
	    String error = retVal.getDialogText();

	    if (error != null && !error.equals(""))
	      {
		sendErrorMessage(error);
	      }
	    else
	      {
		sendErrorMessage("Couldn't login to server... bad username/password?");
	      }

	    return null;
	  }

	if (debug)
	  {
	    System.out.println("logged in");
	  }

	session = xSession.getSession();
	asyncPort = session.getAsyncPort();

	if (asyncPort != null)
	  {
	    asyncThread = new Thread(this, "Ganymede Async Reader");
	    asyncThread.start();
	  }
	
	session = null;	// avoid lingering reference we don't need for xmlclient
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
  
    return xSession;
  }

  /**
   *
   * This method returns true if the client has already logged in.
   *
   */

  public boolean isLoggedIn()
  {
    return session != null || xSession != null;
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
   * <p>This method can be used to retrieve a handle to the client's
   * login session.  This simply returns the same handle that
   * login() returned, in case the client forgets it or something.</p>
   */

  public XMLSession getXSession()
  {
    return xSession;
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

    try
      {
	server.up();
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
   * <p>Allows the server to force us off when it goes down, by way of
   * a message sent us through the asyncPort.</p>
   */

  public void forceDisconnect(String reason)
  {
    connected = false;
    session = null;

    ClientEvent e = new ClientEvent("Server forced disconect: " + reason);

    Vector myVect = (Vector) listeners.clone();

    for (int i = 0; i < myVect.size(); i++)
      {
	((ClientListener)myVect.elementAt(i)).disconnected(e);
      }
  }

  /**
   * <p>Allows the server to send an asynchronous message to the
   * client..  Used by the server to tell the client when a build
   * is/is not being performed on the server.</P> 
   */

  public void sendMessage(int messageType, String status)
  {
    ClientEvent e = new ClientEvent(messageType, status);

    Vector myVect = (Vector) listeners.clone();

    for (int i = 0; i < myVect.size(); i++)
      {
	((ClientListener)myVect.elementAt(i)).messageReceived(e);
      }
  }

  /**
   * <p>We continuously query the server so that any asynchronous
   * messages can be passed back to us without us having to be open
   * for a callback.</p>
   */

  public void run()
  {
    clientAsyncMessage event = null;

    /* -- */

    if (asyncPort == null)
      {
	return;
      }

    try
      {
	while (true)
	  {
	    event = asyncPort.getNextMsg(); // will block on server

	    if (event == null)
	      {
		return;
	      }

	    switch (event.getMethod())
	      {
	      case clientAsyncMessage.SHUTDOWN:
		forceDisconnect(event.getString(0));
		return;

	      case clientAsyncMessage.SENDMESSAGE:
		sendMessage(event.getInt(0), event.getString(1));
		break;
	      }
	  }
      }
    catch (RemoteException ex)
      {
      }
    finally
      {
	asyncPort = null;
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

  private void sendErrorMessage(String message)
  {
    ClientEvent e = new ClientEvent(message);

    for (int i = 0; i < listeners.size(); i++)
      { 
	((ClientListener)listeners.elementAt(i)).messageReceived(e);
      }
  }
}
