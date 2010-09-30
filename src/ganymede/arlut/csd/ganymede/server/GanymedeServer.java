/*

   GanymedeServer.java

   The GanymedeServer object is created by the
   arlut.csd.ganymede.server.Ganymede class at start-up time and
   published to the net for client logins via RMI.  As such, the
   GanymedeServer object is the first Ganymede code that a client will
   directly interact with.
   
   Created: 17 January 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryAndNode;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
import arlut.csd.ganymede.common.QueryOrNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.adminSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GanymedeServer

------------------------------------------------------------------------------*/

/**
 * <p>The GanymedeServer object is created by the
 * {@link arlut.csd.ganymede.server.Ganymede Ganymede class} at start-up time
 * and published to the net for client logins via RMI.  As such,
 * the GanymedeServer object is the first Ganymede code that a client
 * will directly interact with.</p>
 */

public class GanymedeServer implements Server {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeServer");

  /**
   * <p>Singleton server object.  A running Ganymede Server will have one
   * instance of GanymedeServer active and bound into the RMI registry,
   * and this field will point to it.</p>
   */

  static GanymedeServer server = null;

  /**
   * <p>Vector of {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}
   * objects for users that are logged into the Ganymede server remotely.</p>
   *
   * <p>Note that there may be GanymedeSession objects active that are
   * not listed in this sessions Vector; GanymedeSession objects used for
   * server-side internal operations are not counted here.  This Vector is
   * primarily used to keep track of things for the admin console code in
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}.</p>
   */

  static private Vector sessions = new Vector();

  /**
   * <p>A hashtable mapping session names to identity.  Used by the login
   * process to insure that each active remote session will be given a unique
   * identifying name.</p>
   */

  static Hashtable activeUsers = new Hashtable();

  /**
   * <p>A hashtable mapping user Invids to a java.lang.Date object
   * representing the last time that user logged into Ganymede.  This
   * data structure is used to check to see if the server's motd.html
   * file has changed since the user last logged in.  This hash of
   * timestamps is not preserved in the ganymede.db file, so whenever
   * the server is restarted, all users are presumed to need to see
   * the motd.html file on their next login.</p>
   */

  static Hashtable userLogOuts = new Hashtable();

  /**
   * <p>If true, the server is waiting for all users to disconnect
   * so that it can shut itself down.</p>
   */

  static boolean shutdown = false;

  /**
   * <p>Our handy, all purpose login semaphore</p>
   */

  static loginSemaphore lSemaphore = new loginSemaphore();

  /**
   * <p>Another handy semaphore, this time used to defer
   * shutdowns from build processes</p>
   */

  static loginSemaphore shutdownSemaphore = new loginSemaphore();

  /**
   * <p>During the login process, we need to get exclusive access over
   * an extended time to synchronized methods in a privileged
   * GanymedeSession to do the query operations for login.  If we used
   * the generic Ganymede.internalSession for this, we might lock the
   * server up, as Ganymede.internalSession is also used for Invid
   * label look up operations in the transaction commit process, which
   * involves a writeLock that will block the login's read lock from
   * being granted.</p>
   *
   * <p>By having our own private GanymedeSession for logins, we avoid
   * this deadlock possibility.</p>
   */

  private GanymedeSession loginSession;

  /* -- */

  /**
   *
   * GanymedeServer constructor.  We only want one server running
   * per invocation of Ganymede, so we'll check that here.
   *
   */

  public GanymedeServer() throws RemoteException
  {
    if (server == null)
      {
	server = this;
      }
    else
      {
	Ganymede.debug(ts.l("init.multiserver"));
	throw new RemoteException(ts.l("init.multiserver"));
      }

    loginSession = new GanymedeSession(); // supergash

    Ganymede.rmi.publishObject(this);
  } 

  /**
   * <p>Simple RMI ping test method.. this method is here so that the
   * {@link arlut.csd.ganymede.client.ClientBase ClientBase} class can
   * test to see whether it has truly gotten a valid RMI reference to
   * the server.</p>
   */

  public boolean up() throws RemoteException
  {
    return true;
  }

  /** 
   * <p>Client login method.  Establishes a {@link
   * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} object in the
   * server for the client, and returns a serializable {@link
   * arlut.csd.ganymede.common.ReturnVal ReturnVal} object which will contain
   * a {@link arlut.csd.ganymede.rmi.Session Session} remote reference
   * for the client to use, if login was successful.</p>
   *
   * <p>If login is not successful, the ReturnVal object will encode
   * a failure condition, along with a dialog explaining the problem.</p>
   *
   * <p>The GanymedeSession object contains all of the server's
   * knowledge about a given client's status., and is tracked by
   * the GanymedeServer object for statistics and for the admin
   * console's monitoring support.</p>
   *
   * @see arlut.csd.ganymede.rmi.Server 
   */

  public ReturnVal login(String username, String password) throws RemoteException
  {
    return processLogin(username, password, true, true);
  }

  /** 
   * <p>XML Client login method.  Establishes a {@link
   * arlut.csd.ganymede.server.GanymedeXMLSession GanymedeXMLSession} object
   * in the server for the client, and returns a serializable {@link
   * arlut.csd.ganymede.common.ReturnVal ReturnVal} object which will contain
   * a {@link arlut.csd.ganymede.rmi.XMLSession XMLSession} remote reference
   * for the client to use, if login was successful.</p>
   *
   * <p>If login is not successful, the ReturnVal object will encode
   * a failure condition, along with a dialog explaining the problem.</p>
   *
   * <p>The GanymedeXMLSession object in turn contains a
   * GanymedeSession object, which contains all of the server's
   * knowledge about a given client's status., and is tracked by the
   * GanymedeServer object for statistics and for the admin console's
   * monitoring support.</p>
   *
   * @see arlut.csd.ganymede.rmi.Server 
   */

  public ReturnVal xmlLogin(String username, String password) throws RemoteException
  {
    ReturnVal retVal = processLogin(username, password, false, true);

    if (!retVal.didSucceed())   // XXX processLogin never returns null
      {
	return retVal;
      }

    GanymedeSession mySession = (GanymedeSession) retVal.getSession();

    if (mySession == null)
      {
	return null;		// nope, no soup for you.
      }

    GanymedeXMLSession xSession = new GanymedeXMLSession(mySession);
 
    // spawn the GanymedeXMLSession's background parser thread

    xSession.start();

    // publish the GanymedeXMLSession for the client to use

    Ganymede.rmi.publishObject(xSession);

    // replace our remote Session reference with the XMLSession
    // reference, and pass it to the client.. they can query the
    // XMLSession for the Session reference if they need to.

    retVal.setXMLSession(xSession);
    return retVal;
  }

  /**
   * <p>This internal method handles the client login logic for both the normal
   * interactive client and the xml batch client.</p>
   *
   * @param clientName The user/persona name to be logged in
   * @param clientPass The password (in plaintext) to authenticate with
   * @param directSession If true, the GanymedeSession returned will export objects
   * created or referenced by the GanymedeSession for direct RMI access
   * @param clientIsRemote If true, the GanymedeSession will set up for remote access,
   * with user timeouts and the like.
   */

  private ReturnVal processLogin(String clientName, String clientPass, 
				 boolean directSession, boolean clientIsRemote) throws RemoteException
  {
    String clienthost = null;
    boolean found = false;
    boolean success = false;
    Query userQuery;
    QueryNode root;
    DBObject user = null, persona = null;
    PasswordDBField pdbf;

    /* -- */

    String error = GanymedeServer.lSemaphore.increment();

    if (error != null)
      {
        if (error.equals("shutdown"))
          {
            return Ganymede.createErrorDialog(ts.l("processLogin.nologins"),
                                              ts.l("processLogin.nologins_shutdown"));
          }
        else
          {
            return Ganymede.createErrorDialog(ts.l("processLogin.nologins"),
                                              ts.l("processLogin.nologins_semaphore", error));
          }
      }

    // massive try so we decrement our loginSemaphore if the login
    // doesn't go through cleanly

    try
      {
	synchronized (this)
	  {
	    // force logins to lowercase so we can keep track of
	    // things cleanly..  we do a case insensitive match
	    // against user/persona name later on, but we want to have
	    // a canonical name to track multiple logins with.
    
	    clientName = clientName.toLowerCase();

	    root = new QueryDataNode(SchemaConstants.UserUserName,QueryDataNode.NOCASEEQ, clientName);
	    userQuery = new Query(SchemaConstants.UserBase, root, false);

	    Vector results = loginSession.internalQuery(userQuery);

	    // results.size() really shouldn't be any larger than 1,
	    // since we are doing a match on username and username is
	    // managed by a namespace in the schema.

	    for (int i = 0; !found && results != null && (i < results.size()); i++)
	      {
		user = loginSession.session.viewDBObject(((Result) results.elementAt(i)).getInvid());
	
		pdbf = (PasswordDBField) user.getField(SchemaConstants.UserPassword);
	
		if (pdbf != null && pdbf.matchPlainText(clientPass))
		  {
		    found = true;

		    // and re-canonicalize the name so that later on
		    // we can match when we do case sensitive searches

		    clientName = user.getLabel();
		  }
	      }

	    // if we didn't find a user, perhaps they tried logging in
	    // by entering their persona name directly?  For the time
	    // being we are allowing this, so we'll go ahead and look
	    // for a matching persona.

	    if (!found)
	      {
		// we want to match against either the persona name
		// field or the new persona label field.  This lets us
		// work with old versions of the database or new.
		// Going forward we'll want to match here against the
		// PersonaLabelField.

		// note.. this is a hack for compatibility.. the
		// personalabelfield will always be good, but if it
		// does not exist, we'll go ahead and match against
		// the persona name field as long as we don't have an
		// associated user in that persona.. this is to avoid
		// confusing 'broccol:supergash' with 'supergash'

		// the persona label field would be like
		// 'broccol:supergash', whereas the persona name would
		// be 'supergash', which could be confused with the
		// supergash account.

		root = new QueryOrNode(new QueryDataNode(SchemaConstants.PersonaLabelField, 
							 QueryDataNode.NOCASEEQ, clientName),
				       new QueryAndNode(new QueryDataNode(SchemaConstants.PersonaNameField, 
									  QueryDataNode.NOCASEEQ, clientName),
							new QueryNotNode(new QueryDataNode(SchemaConstants.PersonaAssocUser,
											   QueryDataNode.DEFINED, null))));

		userQuery = new Query(SchemaConstants.PersonaBase, root, false);

		results = loginSession.internalQuery(userQuery);

		// find the entry with the matching name and the
		// matching password.  We no longer expect the
		// PersonaNameField to be managed by a namespace, so
		// we could conceivably get multiple matches back from
		// our query.  This is particularly true if the user
		// tries to log in as 'GASH Admin', which might be the
		// "name" of many persona accounts.  In this case
		// we'll depend on the password telling us which to
		// match against.

		// once we get all the ganymede.db files transitioned
		// over to the new persona format, we'll probably want
		// to just match against PersonaLabelField to avoid
		// the possibility of ambiguous admin selection here.

		for (int i = 0; !found && (i < results.size()); i++)
		  {
		    persona = loginSession.session.viewDBObject(((Result) results.elementAt(i)).getInvid());
	    
		    pdbf = (PasswordDBField) persona.getField(SchemaConstants.PersonaPasswordField);

		    if (pdbf == null)
		      {
			System.err.println(ts.l("processLogin.nopersonapass", persona.getLabel()));
		      }
		    else
		      {
			if (clientPass == null)
			  {
			    System.err.println(ts.l("processLogin.nopass"));
			  }
			else
			  {
			    if (pdbf.matchPlainText(clientPass))
			      {
				found = true;
			      }
			  }
		      } 
		  } 
	
		// okay, if the user logged in directly to his persona
		// (broccol:GASH Admin, etc.), try to find his base user
		// account.
	
		if (found && clientName.indexOf(':') != -1)
		  {
		    String userName = clientName.substring(0, clientName.indexOf(':'));
	    
		    root = new QueryDataNode(SchemaConstants.UserUserName,QueryDataNode.EQUALS, userName);
		    userQuery = new Query(SchemaConstants.UserBase, root, false);
	    
		    results = loginSession.internalQuery(userQuery);

		    if (results.size() == 1)
		      {
			user = loginSession.session.viewDBObject(((Result) results.elementAt(0)).getInvid());

			// recanonicalize

			clientName = user.getLabel();

			if (user.isInactivated())
			  {
			    found = false;
			    user = null;
			  }
		      }
		  }
	      }

	    if (found)
	      {
		// the GanymedeSession constructor calls
		// registerActiveUser() on us, as well as directly
		// adding itself to our sessions Vector.

		GanymedeSession session = new GanymedeSession(clientName,
							      user, persona, 
							      directSession, clientIsRemote);

		// "{0} logged in from {1}"
		Ganymede.debug(ts.l("processLogin.loggedin", session.getMyUserName(), session.clienthost));

		Vector objects = new Vector();

		if (user != null)
		  {
		    objects.addElement(user.getInvid());
		  }
		else
		  {
		    objects.addElement(persona.getInvid());
		  }

		if (Ganymede.log != null)
		  {
		    Ganymede.log.logSystemEvent(new DBLogEvent("normallogin",
							       ts.l("processLogin.logevent", clientName, session.clienthost),
							       null,
							       clientName,
							       objects,
							       null));
		  }

		success = true;

		ReturnVal retVal = ReturnVal.success();
		retVal.setSession(session);

		return retVal;
	      }
	    else
	      {
		try
		  {
		    String ipAddress = UnicastRemoteObject.getClientHost();

		    try
		      {
			java.net.InetAddress addr = java.net.InetAddress.getByName(ipAddress);
			clienthost = addr.getHostName();
		      }
		    catch (java.net.UnknownHostException ex)
		      {
			clienthost = ipAddress;
		      }
		  }
		catch (ServerNotActiveException ex)
		  {
		    clienthost = "unknown";
		  }

		if (Ganymede.log != null)
		  {
		    Vector recipients = new Vector();

		    //	    recipients.addElement(clientName); // this might well bounce.  C'est la vie.

		    Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
							       ts.l("processLogin.badlogevent", clientName, clienthost),
							       null,
							       clientName,
							       null,
							       recipients));
		  }

                // "Bad login attempt"
                // "Bad username or password, login rejected."
		return Ganymede.createErrorDialog(ts.l("processLogin.badlogin"),
						  ts.l("processLogin.badlogintext"));
	      }
	  }
      }
    finally
      {
	if (!success)
	  {
	    GanymedeServer.lSemaphore.decrement();

	    // notify the consoles after decrementing the login
	    // semaphore so the notify won't show the semaphore
	    // increment

	    Ganymede.debug(ts.l("processLogin.badlogevent", clientName, clienthost));
	  }
      }
  }

  /**
   * <p>Handy public accessor for the login semaphore, for
   * possible use by plug-in task code.</p>
   */

  public static loginSemaphore getLoginSemaphore()
  {
    return GanymedeServer.lSemaphore;
  }

  /**
   * <p>Gated enabled test.  If this method returns null, logins are allowed
   * at the time checkEnabled() is called.  This method is to be used by admin
   * consoles, which should not connect to the server during schema editing or
   * server shut down, but which should not affect the login count for reasons
   * of blocking a schema edit disable, say.</p>
   *
   * @return null if logins are currently enabled, or a message string if they
   * are disabled.
   */

  public static String checkEnabled()
  {
    return GanymedeServer.lSemaphore.checkEnabled();
  }

  /**
   * <p>This method is called to add a remote user's
   * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}
   * object to the GanymedeServer's static
   * {@link arlut.csd.ganymede.server.GanymedeServer#sessions sessions}
   * field, which is used by the admin console code to iterate
   * over connected users when logging user actions to the
   * Ganymede admin console.</p>
   */

  public static void addRemoteUser(GanymedeSession session)
  {
    synchronized (sessions)
      {
        sendMessageToRemoteSessions(ClientMessage.LOGIN, ts.l("addRemoteUser.logged_in", session.getMyUserName()));

        // we send the above message before adding the user to the
        // sessions Vector so that the user doesn't get bothered with
        // a 'you logged in' message

        sessions.addElement(session);

        sendMessageToRemoteSessions(ClientMessage.LOGINCOUNT, Integer.toString(sessions.size()));
      }
  }

  /**
   * <p>This method is called to remove a remote user's
   * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}
   * object from the GanymedeServer's static
   * {@link arlut.csd.ganymede.server.GanymedeServer#sessions sessions}
   * field, which is used by the admin console code to iterate
   * over connected users when logging user actions to the
   * Ganymede admin console.</p>
   */

  public static void removeRemoteUser(GanymedeSession session)
  {
    synchronized (sessions)
      {
        sessions.removeElement(session);

        // we just removed the session of the user who logged out, so
        // they won't receive the log out message that we'll send to
        // the other clients

        sendMessageToRemoteSessions(ClientMessage.LOGOUT, ts.l("removeRemoteUser.logged_out", session.getMyUserName()));
        sendMessageToRemoteSessions(ClientMessage.LOGINCOUNT, Integer.toString(sessions.size()));
      }
  }

  /**
   * <p>This method is used by the
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}
   * refreshUsers() method to get a summary of the state of the
   * remotely connected users.</p>
   */

  public static Vector getUserTable()
  {
    GanymedeSession session;
    Vector entries;

    /* -- */

    synchronized (sessions)
      {
	entries = new Vector(sessions.size());
	
	for (int i = 0; i < sessions.size(); i++)
	  {
	    session = (GanymedeSession) GanymedeServer.sessions.elementAt(i);
	    
	    if (session.isLoggedIn())
	      {
		entries.addElement(session.getAdminEntry());
	      }
	  }
      }

    return entries;
  }

  /**
   * Used by the Ganymede server to transmit notifications to
   * connected clients.
   *
   * @param type Should be a valid value from arlut.csd.common.ClientMessage
   * @param message The message to send
   */

  public static void sendMessageToRemoteSessions(int type, String message)
  {
    sendMessageToRemoteSessions(type, message, null);
  }


  /**
   * Used by the Ganymede server to transmit notifications to
   * connected clients.
   *
   * @param type Should be a valid value from arlut.csd.common.ClientMessage
   * @param message The message to send
   * @param self If non-null, sendMessageToRemoteSessions will skip sending the
   * message to self
   */

  public static void sendMessageToRemoteSessions(int type, String message, GanymedeSession self)
  {
    Vector sessionsCopy = (Vector) sessions.clone();

    for (int i = 0; i < sessionsCopy.size(); i++)
      {
	GanymedeSession session = (GanymedeSession) sessionsCopy.elementAt(i);

        if (session != self)
          {
            session.sendMessage(type, message);
          }
      }
  }

  /**
   * This method is called by the {@link arlut.csd.ganymede.server.timeOutTask timeOutTask}
   * scheduled task, and forces an idle time check on any users logged in.
   */

  public void clearIdleSessions()
  {
    // clone the sessions Vector so any forceOff() resulting from a
    // timeCheck() call won't disturb the loop, and so that we won't
    // have to synchronize on sessions and risk nested monitor
    // deadlock

    Vector sessionsCopy = (Vector) sessions.clone();

    for (int i = 0; i < sessionsCopy.size(); i++)
      {
	GanymedeSession session = (GanymedeSession) sessionsCopy.elementAt(i);
	session.timeCheck();
      }
  }

  public void killAllUsers(String reason)
  {
    // clone the sessions Vector so any forceOff() won't disturb the
    // loop

    Vector sessionsCopy = (Vector) sessions.clone();

    for (int i = 0; i < sessionsCopy.size(); i++)
      {
	GanymedeSession session = (GanymedeSession) sessionsCopy.elementAt(i);
	session.forceOff(reason);
      }
  }

  /**
   * This method is called by the admin console via the {@link
   * arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} class to
   * kick a specific user session off of the server.
   */

  public boolean killUser(String username, String reason)
  {
    // it's okay to loop inside sessions since we'll exit the loop as
    // soon as we do a forceOff (which can cause the sessions Vector
    // to be modified in a way that would otherwise disturb the loop

    synchronized (sessions)
      {
	for (int i = 0; i < sessions.size(); i++)
	  {
	    GanymedeSession session = (GanymedeSession) sessions.elementAt(i);

	    if (session.getSessionName().equals(username))
	      {
		session.forceOff(reason);
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   *
   * This method is used by GanymedeSession.login() to find
   * a unique name for a session.  It is matched with
   * clearActiveUser(), below.
   *
   */

  static String registerActiveUser(String username)
  {
    String temp;
    int i = 2;

    /* -- */

    temp = username;

    synchronized (activeUsers)
      {
	while (activeUsers.containsKey(username))
	  {
	    username = temp + "[" + i + "]";
	    i++;
	  }
	
	activeUsers.put(username, username);
      }

    return username;
  }

  /**
   *
   * This method is to handle user logout.  It is matched with
   * registerActiveUser(), above.
   *
   */

  static void clearActiveUser(String username)
  {
    synchronized (activeUsers)
      {
	activeUsers.remove(username);

	// if we are in deferred shutdown mode and this was the last
	// user logged in, spin off a thread to shut the server down

	if (shutdown && activeUsers.size() == 0)
	  {
	    Thread deathThread = new Thread(new Runnable() {
	      public void run() {
		// sleep for 5 seconds to let our last client disconnect
		
		try
		  {
		    java.lang.Thread.currentThread().sleep(5000);
		  }
		catch (InterruptedException ex)
		  {
		  }

		GanymedeServer.shutdown();
	      }
	      }, ts.l("clearActiveUser.deathThread"));

	    deathThread.start();
	  }
      }
  }

  /** 
   * <p>This method retrieves a message from a specified directory in
   * the Ganymede installation and passes it back as a StringBuffer.
   * Used by the Ganymede server to pass motd information to the
   * client.</p>
   *
   * @param key A text key indicating the file to be retrieved, minus
   * the .txt or .html extension
   * @param userToDateCompare If not null, the Invid of a user on whose behalf
   * we want to retrieve the message.  If the user has logged in more recently
   * than the timestamp of the file has changed, we will return a null result
   * @param html If true, return the .html version.  If false, return the .txt
   * version.
   */

  public static StringBuffer getTextMessage(String key, Invid userToDateCompare,
					    boolean html)
  {
    if ((key.indexOf('/') != -1) || (key.indexOf('\\') != -1))
      {
	throw new IllegalArgumentException(ts.l("getTextMessage.badargs"));
      }

    if (html)
      {
	key = key + ".html";
      }
    else
      {
	key = key + ".txt";
      }

    if (Ganymede.messageDirectoryProperty == null)
      {
	Ganymede.debug(ts.l("getTextMessage.nodir", key));
	return null;
      }

    /* - */

    String filename = arlut.csd.Util.PathComplete.completePath(Ganymede.messageDirectoryProperty) + key;
    File messageFile;

    /* -- */

    messageFile = new File(filename);

    if (!messageFile.exists() || ! messageFile.isFile())
      {
	return null;
      }

    if (userToDateCompare != null)
      {
	Date lastlogout = (Date) userLogOuts.get(userToDateCompare);

	if (lastlogout != null)
	  {
	    Date timestamp = new Date(messageFile.lastModified());

	    if (lastlogout.after(timestamp))
	      {
		return null;
	      }
	  }
      }

    // okay, read and copy!

    BufferedReader in = null;
    StringBuffer result = null;

    try
      {
	in = new BufferedReader(new FileReader(messageFile));

	result = new StringBuffer();

	try
	  {
	    String line = in.readLine();

	    while (line != null)
	      {
		result.append(line);
		result.append("\n");
		line = in.readLine();
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug(ts.l("getTextMessage.IOExceptionReport", filename, ex.getMessage()));
	    Ganymede.debug(result.toString());
	  }
      }
    catch (FileNotFoundException ex)
      {
	Ganymede.debug("getTextMessage(" + key + "): FileNotFoundException");
	return null;
      }
    finally
      {
	if (in != null)
	  {
	    try
	      {
		in.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }

    return result;
  }

  /**
   * <p>This public remotely accessible method is called by the
   * Ganymede admin console and/or the Ganymede stopServer script to
   * establish a new admin console connection to the server.
   * Establishes an GanymedeAdmin object in the server.</p>
   *
   * <p>Adds &lt;admin&gt; as a monitoring admin console.</p>
   *
   * @see arlut.csd.ganymede.rmi.Server
   */

  public synchronized ReturnVal admin(String clientName, String clientPass) throws RemoteException
  {
    boolean fullprivs = false;
    boolean found = false;
    Query userQuery;
    QueryNode root;
    DBObject obj;
    PasswordDBField pdbf;
    int validationResult;

    String clienthost;

    String error = GanymedeServer.lSemaphore.checkEnabled();

    if (error != null)
      {
	return Ganymede.createErrorDialog(ts.l("admin.connect_failure"),
					  ts.l("admin.semaphore_failure", error));
      }

    try
      {
	String ipAddress = UnicastRemoteObject.getClientHost();
	
	try
	  {
	    java.net.InetAddress addr = java.net.InetAddress.getByName(ipAddress);
	    clienthost = addr.getHostName();
	  }
	catch (java.net.UnknownHostException ex)
	  {
	    clienthost = ipAddress;
	  }
      }
    catch (ServerNotActiveException ex)
      {
	clienthost = "unknown";
      }

    validationResult = validateAdminUser(clientName, clientPass);
    
    if (validationResult == 0)
      {
	if (Ganymede.log != null)
	  {
	    Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
						       ts.l("admin.badlogevent", clientName, clienthost),
						       null,
						       clientName,
						       null,
						       null));
	  }
	
	return Ganymede.createErrorDialog(ts.l("admin.badlogin"),
					  ts.l("admin.baduserpass"));
      }
      
    if (validationResult == 1)
      {
      	fullprivs = false;
      }
    else if (validationResult >= 2)
      {
      	fullprivs = true;
      }

    // creating a new GanymedeAdmin can block if we are currently
    // looping over the connected consoles.

    adminSession aSession = new GanymedeAdmin(fullprivs, clientName, clienthost);

    // now Ganymede.debug() will write to the newly attached console,
    // even though we haven't returned the admin session to the admin
    // console client

    String eventStr = ts.l("admin.goodlogevent", clientName, clienthost);

    Ganymede.debug(eventStr);

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("adminconnect",
						   eventStr,
						   null,
						   clientName,
						   null,
						   null));
      }

    ReturnVal retVal = new ReturnVal(true);
    retVal.setAdminSession(aSession);

    return retVal;
  }

  /**
   * This method determines whether the specified username/password combination
   * is valid.
   * 
   * @param clientName The username
   * @param clientPass The password
   * @return 0 if the login fails,
   * 0 if the login succeeds but the user doesn't have admin console privileges,
   * 1 if the login succeeds and the user is allowed basic admin console access,
   * 2 if the login succeeds and the user is allowed full admin console privileges,
   * 3 if the login succeeds and the user is allowed interpreter access.
   */

  public synchronized int validateAdminUser(String clientName, String clientPass)
  {
    Query userQuery;
    QueryNode root;
    DBObject obj;
    PasswordDBField pdbf;

    root = new QueryDataNode(SchemaConstants.PersonaLabelField, QueryDataNode.EQUALS, clientName);
    userQuery = new Query(SchemaConstants.PersonaBase, root, false);
    Vector results = loginSession.internalQuery(userQuery);

    // If there is no admin persona with the given name, then bail
    if (results.size() == 0)
      {
      	return 0;
      }
    
    obj = loginSession.session.viewDBObject(((Result) results.elementAt(0)).getInvid());
    pdbf = (PasswordDBField) obj.getField(SchemaConstants.PersonaPasswordField);
	    
    if (pdbf != null && pdbf.matchPlainText(clientPass))
      {
        // Are we the One True Amazing Supergash Root User Person? He
        // gets full privileges by default.

        if (obj.getInvid().equals(Invid.createInvid(SchemaConstants.PersonaBase,
						    SchemaConstants.PersonaSupergashObj)))
          {
            return 3;
          }
        else
          {
            // Is this user prohibited from accessing the admin
            // console?

	    if (!obj.isSet(SchemaConstants.PersonaAdminConsole))
	      {
		return 0;
	      }

            // Ok, they can access the admin console...but do they
            // have full privileges?

	    if (!obj.isSet(SchemaConstants.PersonaAdminPower))
	      {
		return 1;
	      }

            // Ok, they have full privileges...but can they access the
            // admin interpreter?

	    if (!obj.isSet(SchemaConstants.PersonaInterpreterPower))
	      {
		return 2;
	      }

	    return 3;
          }
      }
    else
      // The password didn't match
      {
      	return 0;
      }
  }
  
  /**
   * <p>This method is used by the
   * {@link arlut.csd.ganymede.server.GanymedeAdmin#shutdown(boolean) shutdown()}
   * method to put the server into 'shutdown soon' mode.</p>
   */

  public static void setShutdown()
  {
    // turn off the login semaphore.  this will block any new clients
    // or admin consoles from connecting while we shut down

    try
      {
	GanymedeServer.lSemaphore.disable("shutdown", false, 0);
      }
    catch (InterruptedException ex)
      {
        Ganymede.logError(ex);
	throw new RuntimeException(ex.getMessage());
      }

    // if no one is logged in, right now, shut er down.

    if (GanymedeServer.lSemaphore.getCount() == 0)
      {
	GanymedeAdmin.setState(ts.l("setShutDown.nousers_state"));

	GanymedeServer.shutdown();

	return;
      }

    // otherwise by setting the shutdown variable to true, we signal
    // clearActiveUser() to shut us down if the remote client count
    // drops to 0

    shutdown = true;

    GanymedeAdmin.setState(ts.l("setShutDown.waiting_state"));
  }

  /**
   * <p>This method actually does the shutdown.</p>
   */

  public static ReturnVal shutdown()
  {
    Vector tempList;
    GanymedeSession temp;

    /* -- */

    String semaphoreState = GanymedeServer.lSemaphore.checkEnabled();

    if (!"shutdown".equals(semaphoreState))
      {
	// turn off the login semaphore.  this will block any new clients or admin consoles
	// from connecting while we shut down

	try
	  {
	    semaphoreState = GanymedeServer.lSemaphore.disable("shutdown", false, 0); // no blocking
	  }
	catch (InterruptedException ex)
	  {
            Ganymede.logError(ex);
	    throw new RuntimeException(ex.getMessage());
	  }

	if (semaphoreState != null)
	  {
	    return Ganymede.createErrorDialog(ts.l("shutdown.failure"),
					      ts.l("shutdown.failure_text", semaphoreState));
	  }
      }

    // "Server going down.. waiting for any builder tasks to finish phase 2"
    Ganymede.debug(ts.l("shutdown.goingdown"));

    try
      {
	shutdownSemaphore.disable("shutdown", true, -1);
      }
    catch (InterruptedException ex)
      {
	// not much that we can do at this point
      }

    // at this point, no new builder tasks can be scheduled

    // "Server going down.. performing final dump"
    Ganymede.debug(ts.l("shutdown.dumping"));

    try
      {
	// from this point on, we will go down, no matter what
	// exceptions might percolate up to this point

	// dump, then shut down.  This dump call will cause us to
	// block until all write locks queued up are processed and
	// released.  Our second dump parameter is false, so that we
	// are guaranteed that no internal client can get a writelock
	// and maybe get a transaction off that would cause us
	// confusion.

	try
	  {
	    Ganymede.db.dump(Ganymede.dbFilename, false, false); // don't release lock, don't archive last
	  }
	catch (IOException ex)
	  {
            // "shutdown error: couldn''t successfully consolidate db."
	    Ganymede.debug(ts.l("shutdown.dumperror"));
	    throw ex;		// maybe didn't lock, so go down hard
	  }

	// ok, we now are left holding a dump lock.  it should be safe to kick
	// everybody off and shut down the server

        // "Server going down.. database locked"
	Ganymede.debug(ts.l("shutdown.locked"));

        // "Server going down.. disconnecting clients"
	Ganymede.debug(ts.l("shutdown.clients"));

	// forceOff modifies GanymedeServer.sessions, so we need to
	// copy our list before we iterate over it.

	tempList = new Vector();

	synchronized (sessions)
	  {
	    for (int i = 0; i < sessions.size(); i++)
	      {
		tempList.addElement(sessions.elementAt(i));
	      }
	  }

	for (int i = 0; i < tempList.size(); i++)
	  {
	    temp = (GanymedeSession) tempList.elementAt(i);

            // "Server going down"
	    temp.forceOff(ts.l("shutdown.clientNotification"));
	  }

        // "Server going down.. interrupting scheduler"
	Ganymede.debug(ts.l("shutdown.scheduler"));

	Ganymede.scheduler.interrupt();

        // "Server going down.. disconnecting consoles"
	Ganymede.debug(ts.l("shutdown.consoles"));

	GanymedeAdmin.closeAllConsoles(ts.l("shutdown.byeconsoles"));
	
	// disconnect the Jython server
  
	/*
	Ganymede.debug(ts.l("shutdown.jython"));
	
	Ganymede.jythonServer.shutdown();
	*/

	// log our shutdown and close the log

	Ganymede.log.logSystemEvent(new DBLogEvent("shutdown",
						   ts.l("shutdown.logevent"),
						   null,
						   null,
						   null,
						   null));

	System.err.println();

        // "Server completing shutdown.. waiting for log thread to complete."
	System.err.println(ts.l("shutdown.closinglog"));

	try
	  {
	    Ganymede.log.close(); // this will block until the mail queue drains
	  }
	catch (IOException ex)
	  {
	    System.err.println(ts.l("shutdown.logIOException", ex.toString()));
	  }
      }
    catch (Exception ex)
      {
        // "Caught exception during final shutdown:"
        Ganymede.logError(ex, ts.l("shutdown.Exception"));
      }
    catch (Error ex)
      {
        // "Caught error during final shutdown:"
        Ganymede.logError(ex, ts.l("shutdown.Error"));
      }
    finally
      {
	System.err.println();

        // "Server shutdown complete."
	System.err.println(ts.l("shutdown.finally"));

	arlut.csd.ganymede.common.Invid.printCount();

        java.lang.Runtime.getRuntime().removeShutdownHook(Ganymede.signalHandlingThread);

        System.exit(0);

	return null;
      }
  }

  /**
   *
   * This method is triggered from the admin console when the user
   * runs an 'Invid Sweep'.  It is designed to scan through the
   * Ganymede datastore's reference fields and clean out any
   * references found that point to non-existent objects.
   *
   * @return true if there were any invalid invids in the database
   *
   */

  public boolean sweepInvids()
  {
    DBField
      field;

    InvidDBField
      iField;

    Vector
      removeVector;

    boolean
      vectorEmpty = true,
      swept = false;

    // XXX
    // 
    // it's safe to use Ganymede.internalSession's DBSession here only
    // because we don't call the synchronized viewDBObject method on
    // it unless and until we are granted the DBDumpLock, and because
    // we are not a synchronized method on GanymedeServer.
    //
    // XXX

    DBSession session = Ganymede.internalSession.session;

    /* -- */

    // make sure we're ok to sweep

    DBDumpLock lock = new DBDumpLock(Ganymede.db);

    try
      {
	lock.establish("sweepInvids"); // wait until we get our lock
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug(ts.l("sweepInvids.noproceed"));

	return false;		// actually we just failed, but same difference
      }

    try
      {
	for (DBObjectBase base: Ganymede.db.objectBases.values())
	  {
	    Ganymede.debug(ts.l("sweepInvids.sweeping", base.toString()));

	    for (DBObject object: base.getObjects())
	      {
		removeVector = new Vector();

		// loop 3: iterate over the fields present in this object

		synchronized (object.fieldAry)
		  {
		    for (int i = 0; i < object.fieldAry.length; i++)
		      {
			field = object.fieldAry[i];

			if (field == null || !(field instanceof InvidDBField))
			  {
			    continue;	// only check invid fields
			  }

			iField = (InvidDBField) field;

			if (iField.isVector())
			  {
			    Vector<Invid> tempVector = (Vector<Invid>) iField.getVectVal();
			    vectorEmpty = true;

			    // clear out the invid's held in this field pending
			    // successful lookup

			    iField.value = new Vector(); 

			    for (Invid invid: tempVector)
			      {
				if (session.viewDBObject(invid) != null)
				  {
				    iField.getVectVal().addElement(invid); // keep this invid
				    vectorEmpty = false;
				  }
				else
				  {
				    Ganymede.debug(ts.l("sweepInvids.removing_vector",
							invid.toString(),
							iField.getName(),
							base.getName(),
							object.getLabel()));

				    swept = true;
				  }
			      }

			    // now, if the vector is totally empty, we'll be removing
			    // this field from definition

			    if (vectorEmpty)
			      {
				removeVector.addElement(Short.valueOf(iField.getID()));
			      }
			  }
			else
			  {
			    Invid invid = (Invid) iField.value;

			    if (session.viewDBObject(invid) == null)
			      {
				swept = true;
				removeVector.addElement(Short.valueOf(iField.getID()));

				Ganymede.debug(ts.l("sweepInvids.removing_scalar",
						    invid.toString(),
						    iField.getName(),
						    base.getName(),
						    object.getLabel()));
			      }
			  }
		      }
		  }

		// need to remove undefined fields now

		for (int i = 0; i < removeVector.size(); i++)
		  {
		    object.clearField(((Short) removeVector.elementAt(i)).shortValue());

		    Ganymede.debug(ts.l("sweepInvids.undefining",
					removeVector.elementAt(i).toString(),
					base.getName(),
					object.getLabel()));
		  }
	      }
	  }
      }
    finally
      {
	lock.release();
      }

    Ganymede.debug(ts.l("sweepInvids.done"));

    return swept;
  }

  /**
   * <p>This method is used for testing.  This method sweeps through
   * all invid's listed in the (loaded) database, and checks to make
   * sure that they all point to valid objects in the datastore.
   * Invid fields that are in symmetric relationships are tested to
   * make sure both ends of the symmetry properly hold.</p>
   *
   * @return true if there were any broken invids in the database
   */

  public boolean checkInvids()
  {
    DBField
      field;

    InvidDBField
      iField;

    boolean
      ok = true;

    // XXX
    // 
    // it's safe to use Ganymede.internalSession's DBSession here only
    // because we don't call the synchronized viewDBObject method on
    // it unless and until we are granted the DBDumpLock, and because
    // we are not a synchronized method on GanymedeServer.
    //
    // XXX

    DBSession session = Ganymede.internalSession.session;

    /* -- */
    
    DBDumpLock lock = new DBDumpLock(Ganymede.db);

    try
      {
	lock.establish("checkInvids"); // wait until we get our lock
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug(ts.l("checkInvids.noproceed"));

	return false;		// actually we just failed, but same difference
      }

    try
      {
	// first we're going to do our forward test, making sure that
	// all pointers registered in the objects in our data store
	// point to valid objects and that they have valid symmetric
	// back pointers or virtual back pointer registrations in the
	// DBLinkTracker class.

	for (DBObjectBase base: Ganymede.db.objectBases.values())
	  {
	    Ganymede.debug(ts.l("checkInvids.checking", base.getName()));

	    for (DBObject object: base.getObjects())
	      {
		synchronized (object.fieldAry)
		  {
		    for (int i = 0; i < object.fieldAry.length; i++)
		      {
			field = object.fieldAry[i];
			
			// we only care about invid fields
			
			if (field == null || !(field instanceof InvidDBField))
			  {
			    continue;
			  }
			
			iField = (InvidDBField) field;
			
			if (!iField.test(session, (base.getName() + ":" + object.getLabel())))
			  {
			    ok = false;
			  }
		      }
		  }
	      }
	  }

        // validate the backPointers structure that we use to quickly
        // find objects pointing to other objects with non-symmetric
        // links

	if (!Ganymede.db.aSymLinkTracker.checkInvids(session))
	  {
	    ok = false;
	  }
      }
    finally
      {
	lock.release();
      }

    Ganymede.debug(ts.l("checkInvids.done"));

    return ok;
  }

  /**
   * <p>This method is used for testing.  This method sweeps 
   * through all embedded objects in the (loaded) database, and
   * checks to make sure that they all have valid containing objects.</p>
   *
   * @return true if there were any embedded objects without containers in
   * the database
   */

  public boolean checkEmbeddedObjects()
  {
    boolean
      ok = true;

    // XXX
    // 
    // it's safe to use Ganymede.internalSession's DBSession here only
    // because we don't call the synchronized viewDBObject method on
    // it unless and until we are granted the DBDumpLock, and because
    // we are not a synchronized method on GanymedeServer.
    //
    // XXX

    GanymedeSession gSession = Ganymede.internalSession;

    /* -- */
    
    DBDumpLock lock = new DBDumpLock(Ganymede.db);

    try
      {
	lock.establish("checkEmbeddedObjects"); // wait until we get our lock
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug(ts.l("checkEmbeddedObjects.noproceed"));

	return false;		// actually we just failed, but same difference
      }

    try
      {
	// loop over the object bases

	for (DBObjectBase base: Ganymede.db.objectBases.values())
	  {
	    if (!base.isEmbedded())
	      {
		continue;
	      }

	    // loop over the objects in this base

	    Ganymede.debug(ts.l("checkEmbeddedObjects.checking", base.getName()));
	
	    for (DBObject object: base.getObjects())
	      {
		try
		  {
		    gSession.getContainingObj(object);
		  }
		catch (IntegrityConstraintException ex)
		  {
		    Ganymede.debug(ts.l("checkEmbeddedObjects.aha", object.getTypeName(), object.getLabel()));
		    ok = false;
		  }
	      }
	  }
      }
    finally
      {
	lock.release();
      }

    Ganymede.debug(ts.l("checkEmbeddedObjects.done"));

    return ok;
  }

  /**
   * <p>This method is used for fixing the server if it somehow leaks
   * embedded objects..  This method sweeps 
   * through all embedded objects in the (loaded) database, and
   * deletes any that do not have valid containing objects.</p>
   */

  public ReturnVal sweepEmbeddedObjects()
  {
    Vector invidsToDelete = new Vector();

    // XXX
    // 
    // it's safe to use Ganymede.internalSession's DBSession here only
    // because we don't call the synchronized viewDBObject method on
    // it unless and until we are granted the DBDumpLock, and because
    // we are not a synchronized method on GanymedeServer.
    //
    // XXX

    GanymedeSession gSession = Ganymede.internalSession;

    /* -- */
    
    DBDumpLock lock = new DBDumpLock(Ganymede.db);

    try
      {
	lock.establish("checkEmbeddedObjects"); // wait until we get our lock
      }
    catch (InterruptedException ex)
      {
	return Ganymede.createErrorDialog(ts.l("sweepEmbeddedObjects.failure"),
					  ts.l("sweepEmbeddedObjects.failure_text"));
      }

    try
      {
	for (DBObjectBase base: Ganymede.db.objectBases.values())
	  {
	    if (!base.isEmbedded())
	      {
		continue;
	      }

	    // loop over the objects in this base

	    Ganymede.debug(ts.l("sweepEmbeddedObjects.checking", base.getName()));

	    for (DBObject object: base.getObjects())
	      {
		try
		  {
		    gSession.getContainingObj(object);
		  }
		catch (IntegrityConstraintException ex)
		  {
		    invidsToDelete.addElement(object.getInvid());
		  }
	      }
	  }
      }
    finally
      {
	lock.release();
      }

    if (invidsToDelete.size() == 0)
      {
	Ganymede.debug(ts.l("sweepEmbeddedObjects.complete"));

	return null;
      }

    // we want a private,  supergash-privileged GanymedeSession

    try
      {
	gSession = new GanymedeSession("embeddedSweep");
      }
    catch (RemoteException ex)
      {
        Ganymede.logError(ex);
	throw new RuntimeException(ex.getMessage());
      }

    try
      {
	// we're going to delete the objects by skipping the GanymedeSession
	// permission layer, which will break on non-contained embedded objects

	DBSession session = gSession.getSession();

	// we want a non-interactive transaction.. if an object removal fails, the
	// whole transaction will fail, no rollbacks.

	gSession.openTransaction("embedded object sweep", false); // non-interactive
    
	for (int i = 0; i < invidsToDelete.size(); i++)
	  {
	    Invid objInvid = (Invid) invidsToDelete.elementAt(i);

	    ReturnVal retVal = session.deleteDBObject(objInvid);

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "Couldn''t delete object {0}"
		Ganymede.debug(ts.l("sweepEmbeddedObjects.delete_failure", gSession.viewObjectLabel(objInvid)));
	      }
	    else
	      {
		// "Deleted object {0}"
		Ganymede.debug(ts.l("sweepEmbeddedObjects.delete_ok", gSession.viewObjectLabel(objInvid)));
	      }
	  }

	return gSession.commitTransaction();
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.createErrorDialog(ts.l("sweepEmbeddedObjects.error"),
					  ts.l("sweepEmbeddedObjects.error_text", ex.getMessage()));
      }
    finally
      {
	gSession.logout();
      }
  }
}
