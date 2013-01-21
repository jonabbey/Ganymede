/*

   ClientBase.java

   The core of a client.  Provides all the logic necessary to establish
   a connection to the server and get logged in.  By using this class,
   the server will only need an RMI stub for this class, regardless of
   what client is written.

   Created: 31 March 1998

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.ErrorTypeEnum;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.clientAsyncMessage;
import arlut.csd.ganymede.common.RMISSLClientListener;
import arlut.csd.ganymede.common.RMISSLClientSocketFactory;
import arlut.csd.ganymede.rmi.ClientAsyncResponder;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.XMLSession;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      ClientBase

------------------------------------------------------------------------------*/

/**
 * <p>The communications core of a client.  Provides all the logic
 * necessary to establish a connection to the server and get logged
 * in.</p>
 *
 * <p>The ClientBase is also responsible for retrieving asynchronous
 * messages from the server and passing them to the client through the
 * {@link arlut.csd.ganymede.client.ClientListener} interface.</p>
 *
 * @author Mike Mulvaney
 */

public class ClientBase implements Runnable, RMISSLClientListener {

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.ClientBase");

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

  /**
   * If we have created an RMI SSL connection, we'll record the cipher
   * suite here.
   */

  private String cipherSuite = null;

  private Vector listeners = new Vector();
  private String myServerURL = null;

  private booleanSemaphore connected = new booleanSemaphore(false);

  /* -- */

  /**
   * This constructor takes a URL for the Ganymede server to connect to, a
   * reference to an object implementing the ClientListener interface to
   * report problems.
   *
   * @param serverURL An rmi:// URL for a Ganymede server.
   * @param listener A ClientListener to report problems and disconnection to.
   */

  public ClientBase(String serverURL, ClientListener listener)
  {
    if (listener == null || serverURL == null || serverURL.length() == 0)
      {
        throw new IllegalArgumentException("bad argument");
      }

    myServerURL = serverURL;
    listeners.addElement(listener);

    // and make sure we are notified if the RMI system creates an SSL
    // connection for us

    RMISSLClientSocketFactory.setSSLClientListener(this);
  }

  /**
   * This method attempts to establish and verify an RMI connection to the
   * server.
   */

  public boolean connect() throws RemoteException, NotBoundException, MalformedURLException
  {
    Remote obj = Naming.lookup(myServerURL);

    if (obj instanceof Server)
      {
        server = (Server) obj;
        server.up();
      }

    connected.set(true);

    return true;
  }

  /**
   * This method is used by a client to actually get logged into the
   * server.  The {@link arlut.csd.ganymede.rmi.Session Session} handle
   * returned is then used to do all server operations appropriate
   * for a normal client.  Calling the Session logout() method will
   * end the client's connection to the server.
   *
   * @return null if login failed, else a valid server Session reference
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Session login(String username, String password) throws RemoteException
  {
    if (isLoggedIn())
      {
        // "Already logged in.  Construct a new ClientBase if you need to open a second concurrent session."
        throw new IllegalArgumentException(ts.l("global.logged_in_error"));
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
                if (retVal.getErrorType() == ErrorTypeEnum.BADCREDS)
                  {
                    sendErrorMessage(ClientMessage.BADCREDS, error);
                  }
                else
                  {
                    sendErrorMessage(error);
                  }
              }
            else
              {
                if (retVal.getErrorType() == ErrorTypeEnum.BADCREDS)
                  {
                    // "Couldn''t log in to server.  Bad username/password?"
                    sendErrorMessage(ClientMessage.BADCREDS, ts.l("global.login_failure_msg"));
                  }
                else
                  {
                    // "Couldn''t log in to server.  Bad username/password?"
                    sendErrorMessage(ts.l("global.login_failure_msg"));
                  }
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
        connected.set(false);

        if (debug)
          {
            System.err.println("Error: Didn't get server reference.  Exiting now.");
          }

        // "Error: ClientBase didn''t get server reference.  Giving up on login."
        sendErrorMessage(ts.l("global.no_ref_msg"));
      }
    catch (Exception ex)
      {
        connected.set(false);

        if (debug)
          {
            System.err.println("Got some other exception: " + ex);
          }

        // "ClientBase login caught some other exception:\n{0}"
        sendErrorMessage(ts.l("global.other_exception_msg", ex));
      }

    return session;
  }

  /**
   * This method is used by a client to actually get logged into
   * the server.  The {@link arlut.csd.ganymede.rmi.XMLSession
   * XMLSession} handle returned is then used to do all server
   * operations appropriate for the xml client.  Calling the XMLSession
   * xmlEnd() method will end the client's connection to the
   * server.
   *
   * @return null if login failed, else a valid server Session reference
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public XMLSession xmlLogin(String username, String password) throws RemoteException
  {
    if (isLoggedIn())
      {
        // "Already logged in.  Construct a new ClientBase if you need to open a second concurrent session."
        throw new IllegalArgumentException(ts.l("global.logged_in_error"));
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
                // "Couldn''t log in to server.  Bad username/password?"
                sendErrorMessage(ts.l("global.login_failure_msg"));
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

        session = null; // avoid lingering reference we don't need for xmlclient
      }
    catch (NullPointerException ex)
      {
        connected.set(false);

        if (debug)
          {
            System.err.println("Error: Didn't get server reference.  Exiting now.");
          }

        // "Error: ClientBase didn''t get server reference.  Giving up on login."
        sendErrorMessage(ts.l("global.no_ref_msg"));
      }
    catch (Exception ex)
      {
        connected.set(false);

        if (debug)
          {
            System.err.println("Got some other exception: " + ex);
          }

        // "ClientBase login caught some other exception:\n{0}"
        sendErrorMessage(ts.l("global.other_exception_msg", ex));
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
   * This method can be used to retrieve a handle to the client's
   * login session.  This simply returns the same handle that
   * login() returned, in case the client forgets it or something.
   */

  public Session getSession()
  {
    return session;
  }

  /**
   * This method can be used to retrieve a handle to the client's
   * login session.  This simply returns the same handle that
   * login() returned, in case the client forgets it or something.
   */

  public XMLSession getXSession()
  {
    return xSession;
  }

  /**
   * This method returns true if the client holds a valid reference to
   * the server.  This will always return true unless the server has
   * forced a disconnect.
   */

  public boolean isConnected()
  {
    if (!connected.isSet())
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
   * Register a client listener.  A client listener is an object
   * that is to be notified if we get an asynchronous callback from
   * the Ganymede server, such as a forced log-off, or if we need
   * to report an error during login.
   */

  public synchronized void addClientListener(ClientListener l)
  {
    listeners.addElement(l);
  }

  /**
   * Remove a client listener.
   */

  public synchronized void removeClientListener(ClientListener l)
  {
    listeners.removeElement(l);
  }

  /**
   * Calls the logout() method on the Session object.  This
   * could be done by the client using the Session reference
   * returned by the login() method, but using this method
   * allows us to reflect login status internally.
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
   * Allows the server to force us off when it goes down, by way of
   * a message sent us through the asyncPort.
   */

  public void forceDisconnect(String reason)
  {
    session = null;

    // "Ganymede Server forced client to disconnect: {0}"
    ClientEvent e = new ClientEvent(ts.l("forceDisconnect.forced_off", reason));

    Vector myVect = (Vector) listeners.clone();

    for (int i = 0; i < myVect.size(); i++)
      {
        ((ClientListener)myVect.elementAt(i)).disconnected(e);
      }
  }

  /**
   * Allows the server to send an asynchronous message to the
   * client..  Used by the server to tell the client when a build
   * is/is not being performed on the server.
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
   * Returns true if the RMISSLClientSocketFactory for this client has
   * been invoked to create a socket since JVM startup.
   */

  public boolean isSSLEnabled()
  {
    return RMISSLClientSocketFactory.isSSLEnabled();
  }

  /**
   * This method is from the RMISSLClientListener, and we use it to
   * get notified about the SSL cipher suite used if we wind up using
   * SSL to connect to an RMI server.
   */

  public void notifySSLClient(String host, int port, String cipherSuite)
  {
    this.cipherSuite = cipherSuite;
  }

  /**
   * If we are connected to the server with an SSL connection, this method will return
   * a string description of the cipher suite we are using.  If we are not connected through
   * an SSL RMI connection, this method will return null.
   */

  public String getCipherSuite()
  {
    return this.cipherSuite;
  }

  /**
   * We continuously query the server so that any asynchronous
   * messages can be passed back to us without us having to be open
   * for a callback.
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
    catch (Exception ex)
      {
        // "Exception caught in ClientBase's async message loop: {0}"
        sendErrorMessage(ts.l("run.exception", ex.toString()));
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
   * Private method to inform clientListeners if we get an error
   * from the server after construction..
   */

  private void sendErrorMessage(int errType, String message)
  {
    ClientEvent e = new ClientEvent(errType, message);

    for (int i = 0; i < listeners.size(); i++)
      {
        ((ClientListener)listeners.elementAt(i)).messageReceived(e);
      }
  }

  /**
   * Private method to inform clientListeners if we get an error
   * from the server after construction..
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
