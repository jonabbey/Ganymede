/*

   GanymedeServer.java

   The GanymedeServer object is created by Ganymede at start-up time
   and published to the net for client logins via RMI.  As such,
   the GanymedeServer object is the first Ganymede code that a client
   will directly interact with.
   
   Created: 17 January 1997
   Release: $Name:  $
   Version: $Revision: 1.56 $
   Last Mod Date: $Date: 2000/02/14 20:44:58 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GanymedeServer

------------------------------------------------------------------------------*/

/**
 * <p>The GanymedeServer object is created by the
 * {@link arlut.csd.ganymede.Ganymede Ganymede class} at start-up time
 * and published to the net for client logins via RMI.  As such,
 * the GanymedeServer object is the first Ganymede code that a client
 * will directly interact with.</p>
 */

public class GanymedeServer extends UnicastRemoteObject implements Server {

  static GanymedeServer server = null;
  static Vector sessions = new Vector();
  static Hashtable activeUsers = new Hashtable();
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

  /* -- */

  /**
   *
   * GanymedeServer constructor.  We only want one server running
   * per invocation of Ganymede, so we'll check that here.
   *
   */

  public GanymedeServer() throws RemoteException
  {
    super();			// UnicastRemoteObject initialization
 
    if (server == null)
      {
	server = this;
      }
    else
      {
	Ganymede.debug("Error: attempted to start a second server");
	throw new RemoteException("Error: attempted to start a second server");
      }
  } 

  /**
   * <p>Simple RMI test method.. this method is here so that the
   * {@link arlut.csd.ganymede.client.ClientBase ClientBase} class
   * can test to see whether it has truly gotten a valid RMI reference
   * to the server.</p>
   */

  public boolean up() throws RemoteException
  {
    return true;
  }

  /** 
   * <p>Client login method.  Establishes a {@link
   * arlut.csd.ganymede.GanymedeSession GanymedeSession} object in the
   * server for the client, and returns a {@link
   * arlut.csd.ganymede.Session Session} remote reference to the
   * client.  The GanymedeSession object contains all of the server's
   * knowledge about a given client's status., and is tracked by
   * the GanymedeServer object for statistics and for the admin
   * console's monitoring support.</P>
   * 
   * @see arlut.csd.ganymede.Server 
   */

  public Session login(Client client) throws RemoteException
  {
    String clientName;
    String clientPass;
    boolean found = false;
    boolean success = false;
    Query userQuery;
    QueryNode root;
    DBObject user = null, persona = null;
    PasswordDBField pdbf;

    /* -- */

    try
      {
	String error = GanymedeServer.lSemaphore.increment(0);

	if (error != null)
	  {
	    client.forceDisconnect("Can't login to Ganymede server.. semaphore disabled: " + error);
	    return null;
	  }
      }
    catch (InterruptedException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    try
      {
	synchronized (this)
	  {
	    // force logins to lowercase so we can keep track of things
	    // cleanly..  we do a case insensitive match against user/persona
	    // name later on, but we want to have a canonical name to track
	    // multiple logins with.
    
	    clientName = client.getName().toLowerCase();
	    clientPass = client.getPassword();

	    root = new QueryDataNode(SchemaConstants.UserUserName,QueryDataNode.EQUALS, clientName);
	    userQuery = new Query(SchemaConstants.UserBase, root, false);

	    Vector results = Ganymede.internalSession.internalQuery(userQuery);

	    // results.size() really shouldn't be any larger than 1, since we
	    // are doing a match on username and username is managed by a
	    // namespace in the schema.

	    for (int i = 0; !found && results != null && (i < results.size()); i++)
	      {
		user = Ganymede.internalSession.session.viewDBObject(((Result) results.elementAt(i)).getInvid());
	
		pdbf = (PasswordDBField) user.getField(SchemaConstants.UserPassword);
	
		if (pdbf != null && pdbf.matchPlainText(clientPass))
		  {
		    found = true;
		  }
	      }

	    // if we didn't find a user, perhaps they tried logging in by
	    // entering their persona name directly?  For the time being we
	    // are allowing this, so we'll go ahead and look for a matching
	    // persona.

	    if (!found)
	      {
		// we want to match against either the persona name field or
		// the new persona label field.  This lets us work with old
		// versions of the database or new.  Going forward we'll
		// want to match here against the PersonaLabelField.

		// note.. this is a hack for compatibility.. the
		// personalabelfield will always be good, but if it does not
		// exist, we'll go ahead and match against the persona name
		// field as long as we don't have an associated user in that
		// persona.. this is to avoid confusing 'broccol:supergash'
		// with 'supergash'

		// the persona label field would be like 'broccol:supergash',
		// whereas the persona name would be 'supergash', which could
		// be confused with the supergash account.

		root = new QueryOrNode(new QueryDataNode(SchemaConstants.PersonaLabelField, QueryDataNode.EQUALS, clientName),
				       new QueryAndNode(new QueryDataNode(SchemaConstants.PersonaNameField, 
									  QueryDataNode.EQUALS, clientName),
							new QueryNotNode(new QueryDataNode(SchemaConstants.PersonaAssocUser,
											   QueryDataNode.DEFINED, null))));

		userQuery = new Query(SchemaConstants.PersonaBase, root, false);

		results = Ganymede.internalSession.internalQuery(userQuery);

		// find the entry with the matching name and the matching
		// password.  We no longer expect the PersonaNameField to be
		// managed by a namespace, so we could conceivably get
		// multiple matches back from our query.  This is particularly
		// true if the user tries to log in as 'GASH Admin', which
		// might be the "name" of many persona accounts.  In this case
		// we'll depend on the password telling us which to match
		// against.

		// once we get all the ganymede.db files transitioned over to
		// the new persona format, we'll probably want to just match
		// against PersonaLabelField to avoid the possibility of
		// ambiguous admin selection here.

		for (int i = 0; !found && (i < results.size()); i++)
		  {
		    persona = Ganymede.internalSession.session.viewDBObject(((Result) results.elementAt(i)).getInvid());
	    
		    pdbf = (PasswordDBField) persona.getField(SchemaConstants.PersonaPasswordField);

		    if (pdbf == null)
		      {
			System.err.println("GanymedeServer.login(): Couldn't get password for persona " + persona.getLabel());
		      }
		    else
		      {
			if (clientPass == null)
			  {
			    System.err.println("GanymedeServer.login(): null clientpass.. ");
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
	
		if (clientName.indexOf(':') != -1)
		  {
		    String userName = clientName.substring(0, clientName.indexOf(':'));
	    
		    root = new QueryDataNode(SchemaConstants.UserUserName,QueryDataNode.EQUALS, userName);
		    userQuery = new Query(SchemaConstants.UserBase, root, false);
	    
		    results = Ganymede.internalSession.internalQuery(userQuery);

		    if (results.size() == 1)
		      {
			user = Ganymede.internalSession.session.viewDBObject(((Result) results.elementAt(0)).getInvid());
		      }
		  }
	      }

	    if (found)
	      {
		// the GanymedeSession constructor calls registerActiveUser()
		// on us, as well as directly adding itself to our sessions
		// Vector.

		GanymedeSession session = new GanymedeSession(client, clientName, user, persona);
		Ganymede.debug(session.username + " logged in");

		Vector objects = new Vector();

		if (user != null)
		  {
		    objects.addElement(user.getInvid());
		  }
		else
		  {
		    objects.addElement(persona.getInvid());
		  }

		String clienthost;

		try
		  {
		    clienthost = getClientHost();
		  }
		catch (ServerNotActiveException ex)
		  {
		    clienthost = "unknown";
		  }

		if (Ganymede.log != null)
		  {
		    Ganymede.log.logSystemEvent(new DBLogEvent("normallogin",
							       "OK login for username: " + 
							       clientName + 
							       " from host " + 
							       clienthost,
							       null,
							       clientName,
							       objects,
							       null));
		  }

		success = true;

		return (Session) session;
	      }
	    else
	      {
		String clienthost;

		try
		  {
		    clienthost = getClientHost();
		  }
		catch (ServerNotActiveException ex)
		  {
		    clienthost = "unknown";
		  }

		Ganymede.debug("Bad login attempt: " + clientName + " from host " + clienthost);

		if (Ganymede.log != null)
		  {
		    Vector recipients = new Vector();

		    //	    recipients.addElement(clientName); // this might well bounce.  C'est la vie.

		    Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
							       "Bad login attempt for username: " + 
							       clientName + " from host " + 
							       clienthost,
							       null,
							       clientName,
							       null,
							       recipients));
		  }

		return null;
	      }
	  }
      }
    finally
      {
	if (!success)
	  {
	    GanymedeServer.lSemaphore.decrement();
	  }
      }
  }

  /**
   * This method is called by the {@link arlut.csd.ganymede.timeOutTask timeOutTask}
   * scheduled task, and forces an idle time check on any users logged in.
   */

  public void clearIdleSessions()
  {
    // clone the sessions Vector so any forceOff() resulting from a
    // timeCheck() call won't disturb the loop

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
   * This method is called by the admin console via the {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin}
   * class to kick a specific user off of the server.
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

	    if (session.username.equals(username))
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
	    username = temp + i;
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
	    });

	    deathThread.start();
	  }
      }
  }

  /** 
   * <P>This method retrieves a message from a specified directory in
   * the Ganymede installation and passes it back as a StringBuffer.
   * Used by the Ganymede server to pass motd information to the
   * client.</P> 
   */

  public static StringBuffer getTextMessage(String key, Invid userToDateCompare,
					    boolean html)
  {
    if ((key.indexOf('/') != -1) || (key.indexOf('\\') != -1))
      {
	throw new IllegalArgumentException("Error, attempt to use path separator in message key.");
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
	Ganymede.debug("GanymedeServer.getTextMessage(): messageDirectoryProperty not set.  Can't provide " + key);
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
	    Ganymede.debug("IOException in GanymedeServer.getTextMessage(" + 
			   filename + "):\n" + ex.getMessage());
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
   * <P>Establishes an GanymedeAdmin object in the server.</P>
   *
   * <P>Adds &lt;admin&gt; as a monitoring admin console.</P>
   *
   * @see arlut.csd.ganymede.Server
   */

  public synchronized adminSession admin(Admin admin) throws RemoteException
  {
    String clientName;
    String clientPass;
    boolean fullprivs = false;
    boolean found = false;
    Query userQuery;
    QueryNode root;
    DBObject obj;
    PasswordDBField pdbf;

    String clienthost;

    /* -- */

    String error = GanymedeServer.lSemaphore.checkEnabled();

    if (error != null)
      {
	throw new RuntimeException("Can't connect admin console to server.. semaphore disabled: " + error);
      }

    clientName = admin.getName();
    clientPass = admin.getPassword();

    // we want to match against either the persona name field or
    // the new persona label field.  This lets us work with old
    // versions of the database or new.  Going forward we'll
    // want to match here against the PersonaLabelField.
    
    // note.. this is a hack for compatibility.. the
    // personalabelfield will always be good, but if it does not
    // exist, we'll go ahead and match against the persona name
    // field as long as we don't have an associated user in that
    // persona.. this is to avoid confusing 'broccol:supergash'
    // with 'supergash'
    
    // the persona label field would be like 'broccol:supergash',
    // whereas the persona name would be 'supergash', which could
    // be confused with the supergash account.
    
    root = new QueryOrNode(new QueryDataNode(SchemaConstants.PersonaLabelField, QueryDataNode.EQUALS, clientName),
			   new QueryAndNode(new QueryDataNode(SchemaConstants.PersonaNameField, 
							      QueryDataNode.EQUALS, clientName),
					    new QueryNotNode(new QueryDataNode(SchemaConstants.PersonaAssocUser,
									       QueryDataNode.DEFINED, null))));
    
    userQuery = new Query(SchemaConstants.PersonaBase, root, false);
    
    Vector results = Ganymede.internalSession.internalQuery(userQuery);
    
    for (int i = 0; !found && (i < results.size()); i++)
      {
	obj = Ganymede.internalSession.session.viewDBObject(((Result) results.elementAt(i)).getInvid());
	    
	pdbf = (PasswordDBField) obj.getField(SchemaConstants.PersonaPasswordField);
	    
	if (pdbf != null && pdbf.matchPlainText(clientPass))
	  {
	    if (clientName.equals(Ganymede.rootname))
	      {
		found = true;
		fullprivs = true;
	      }
	    else
	      {
		BooleanDBField privField = (BooleanDBField) obj.getField(SchemaConstants.PersonaAdminConsole);

		if (privField != null && privField.value())
		  {
		    found = true;
		  }
		else
		  {
		    continue;	// no perms for admin console
		  }

		BooleanDBField fullField = (BooleanDBField) obj.getField(SchemaConstants.PersonaAdminPower);

		if (fullField != null && fullField.value())
		  {
		    fullprivs = true;
		  }
		else
		  {
		    fullprivs = false;
		  }
	      }
	  }
      }

    try
      {
	clienthost = getClientHost();
      }
    catch (ServerNotActiveException ex)
      {
	clienthost = "unknown";
      }
    
    if (!found)
      {
	if (Ganymede.log != null)
	  {
	    Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
						       "Bad console attach attempt by: " + 
						       clientName + " from host " + 
						       clienthost,
						       null,
						       clientName,
						       null,
						       null));
	  }
	
	return null;
      }

    // creating a new GanymedeAdmin can block if we are currently
    // looping over the connected consoles.

    adminSession aSession = new GanymedeAdmin(admin, fullprivs, clientName, clienthost);

    // now Ganymede.debug() will write to the newly attached console,
    // even though we haven't returned the admin session to the admin
    // console client

    Ganymede.debug("Admin console attached for admin " + clientName + " from: " + clienthost);

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("adminconnect",
						   "Admin console attached by: " + 
						   clientName + " from host " + 
						   clienthost,
						   null,
						   clientName,
						   null,
						   null));
      }

    return aSession;
  }

  /**
   * <p>This method is used by the
   * {@link arlut.csd.ganymede.GanymedeAdmin#shutdown(boolean, java.lang.String) shutdown()}
   * method to put the server into 'shutdown soon' mode.</p>
   */

  public static void setShutdown()
  {
    try
      {
	GanymedeServer.lSemaphore.disable("shutdown", false, 0);
      }
    catch (InterruptedException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    GanymedeAdmin.setState("Server going down.. waiting for users to log out");
  }

  /**
   * <p>This method actually does the shutdown.</p>
   */

  public static ReturnVal shutdown()
  {
    Vector tempList;
    GanymedeSession temp;
    GanymedeAdmin atmp;

    /* -- */

    String semaphoreState = GanymedeServer.lSemaphore.checkEnabled();

    if (!"shutdown".equals(semaphoreState))
      {
	try
	  {
	    semaphoreState = GanymedeServer.lSemaphore.disable("shutdown", false, 0); // no blocking
	  }
	catch (InterruptedException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }

	if (semaphoreState != null)
	  {
	    return Ganymede.createErrorDialog("Shutdown failure",
					      "Shutdown failure.. couldn't shutdown the server, semaphore " +
					      "already locked with condition " + semaphoreState);
	  }
      }
    
    GanymedeAdmin.setState("Server going down.. performing final dump");

    // dump, then shut down.  Our second dump parameter is false, so
    // that we are guaranteed that no internal client can get a
    // writelock and maybe get a transaction off that would cause us
    // confusion.

    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, false, false); // don't release lock, don't archive last
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog("Shutdown Error",
					  "shutdown error: couldn't successfully dump db:" + ex);
      }

    // ok, we now are left holding a dump lock.  it should be safe to kick
    // everybody off and shut down the server

    // forceOff modifies GanymedeServer.sessions, so we need to copy our list
    // before we iterate over it.

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

	temp.forceOff("Server going down");
      }

    // stop any background tasks running

    Ganymede.scheduler.stop();

    // disconnect the admin consoles

    for (int i = 0; i < GanymedeAdmin.consoles.size(); i++)
      {
	atmp = (GanymedeAdmin) GanymedeAdmin.consoles.elementAt(i);

	try
	  {
	    atmp.proxy.forceDisconnect("Server going down now.");
	  }
	catch (RemoteException ex)
	  {
	    // don't worry about it
	  }
      }

    Ganymede.log.logSystemEvent(new DBLogEvent("shutdown",
					       "Server shutdown",
					       null,
					       null,
					       null,
					       null));

    System.err.println("\nServer completing shutdown.. waiting for log thread to complete.");

    try
      {
	Ganymede.log.close();
      }
    catch (IOException ex)
      {
	System.err.println("IO Exception closing log file:" + ex);
      }

    System.err.println("\nServer shutdown complete.");

    System.exit(0);

    return null;
  }

  /**
   *
   * This method is used by directLoader code to dump the
   * database to disk at the end of the bulk-loading
   * process.
   *
   */

  public void dump()
  {
    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, false, false); // don't release lock, don't archive
      }
    catch (IOException ex)
      {
	throw new RuntimeException("GanymedeServer.dump error: " + ex);
      }
  }

  /**
   *
   * This method is used when the Ganymede server module is being
   * driven by a direct-linked main method.  This method sweeps
   * through all invid's listed in the (loaded) database, and
   * removes any invid's that point to objects not in the database.
   *
   * @return true if there were any invalid invids in the database
   *
   */

  public boolean sweepInvids()
  {
    Enumeration
      enum1, enum2, enum3, enum4;

    DBObjectBase
      base;

    DBObject
      object;

    DBField
      field;

    InvidDBField
      iField;

    Vector
      removeVector,
      tempVector;

    Invid
      invid;

    boolean
      vectorEmpty = true,
      swept = false;

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
	Ganymede.debug("sweepInvids couldn't proceed.");

	return false;		// actually we just failed, but same difference
      }

    try
      {
	// loop 1: iterate over the object bases

	enum1 = Ganymede.db.objectBases.elements();

	while (enum1.hasMoreElements())
	  {
	    base = (DBObjectBase) enum1.nextElement();

	    Ganymede.debug("GanymedeServer.sweepInvids(): sweeping " + base);

	    // loop 2: iterate over the objects in the current object base

	    enum2 = base.objectTable.elements();

	    while (enum2.hasMoreElements())
	      {
		object = (DBObject) enum2.nextElement();

		removeVector = new Vector();

		// loop 3: iterate over the fields present in this object

		enum3 = object.fields.elements();

		while (enum3.hasMoreElements())
		  {
		    field = (DBField) enum3.nextElement();

		    if (!(field instanceof InvidDBField))
		      {
			continue;	// only check invid fields
		      }

		    iField = (InvidDBField) field;

		    if (iField.isVector())
		      {
			tempVector = iField.getVectVal();
			vectorEmpty = true;

			// clear out the invid's held in this field pending
			// successful lookup

			iField.value = new Vector(); 

			// iterate over the invid's held in this vector
		    
			enum4 = tempVector.elements();

			while (enum4.hasMoreElements())
			  {
			    invid = (Invid) enum4.nextElement();

			    if (session.viewDBObject(invid) != null)
			      {
				iField.getVectVal().addElement(invid); // keep this invid
				vectorEmpty = false;
			      }
			    else
			      {
				Ganymede.debug("Removing invid: " + invid + 
					       " from vector field " + iField.getName() +
					       " from object " +  base.getName() + 
					       ":" + object.getLabel());
				swept = true;
			      }
			  }

			// now, if the vector is totally empty, we'll be removing
			// this field from definition

			if (vectorEmpty)
			  {
			    removeVector.addElement(new Short(iField.getID()));
			  }
		      }
		    else
		      {
			invid = (Invid) iField.value;

			if (session.viewDBObject(invid) == null)
			  {
			    swept = true;
			    removeVector.addElement(new Short(iField.getID()));

			    Ganymede.debug("Removing invid: " + invid + 
					   " from scalar field " + iField.getName() +
					   " from object " +  base.getName() + 
					   ":" + object.getLabel());
			  }
		      }
		  }

		// need to remove undefined fields now

		for (int i = 0; i < removeVector.size(); i++)
		  {
		    object.fields.remove(((Short) removeVector.elementAt(i)).shortValue());

		    Ganymede.debug("Undefining (now) empty field: " + 
				   removeVector.elementAt(i) +
				   " from object " +  base.getName() + 
				   ":" + object.getLabel());
		  }
	      }
	  }
      }
    finally
      {
	lock.release();
      }

    Ganymede.debug("GanymedeServer.sweepInvids(): completed");

    return swept;
  }

  /**
   * <P>This method is used for testing.  This method sweeps 
   * through all invid's listed in the (loaded) database, and
   * checks to make sure that they all have valid back pointers.</P>
   *
   * <P>Since the backlinks field (SchemaConstants.BackLinksField)
   * is a general sink for invid's with no homes, we won't explicitly
   * check to see if an invid in a backlink field has a field pointing
   * to it in the target object.. we'll just verify the existence of
   * the object listed.</P>
   *
   * @return true if there were any invids without back-pointers in
   * the database
   */

  public boolean checkInvids()
  {
    Enumeration
      enum1, enum2, enum3, enum4;

    DBObjectBase
      base;

    DBObject
      object;

    DBField
      field;

    InvidDBField
      iField;

    Invid
      invid;

    boolean
      ok = true;

    DBSession session = Ganymede.internalSession.session;

    /* -- */
    
    DBDumpLock lock = new DBDumpLock(Ganymede.db);

    try
      {
	lock.establish("checkInvids"); // wait until we get our lock
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug("checkInvids couldn't proceed.");

	return false;		// actually we just failed, but same difference
      }

    try
      {
	// loop over the object bases

	enum1 = Ganymede.db.objectBases.elements();

	while (enum1.hasMoreElements())
	  {
	    base = (DBObjectBase) enum1.nextElement();

	    // loop over the objects in this base

	    Ganymede.debug("Testing invid links for objects of type " + base.getName());
	
	    enum2 = base.objectTable.elements();

	    while (enum2.hasMoreElements())
	      {
		object = (DBObject) enum2.nextElement();

		//	Ganymede.debug("Testing invid links for object " + object.getLabel());

		// loop over the fields in this object	    

		enum3 = object.fields.elements();

		while (enum3.hasMoreElements())
		  {
		    field = (DBField) enum3.nextElement();

		    // we only care about invid fields

		    if (!(field instanceof InvidDBField))
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

	synchronized (Ganymede.db.backPointers)
	  {
	    Ganymede.debug("Testing Ganymede backPointers hash structure for validity");
	    Ganymede.debug("Ganymede backPointers hash structure tracking " + Ganymede.db.backPointers.size() +
			   " invid's.");

	    Enumeration keys = Ganymede.db.backPointers.keys();

	    while (keys.hasMoreElements())
	      {
		Invid key = (Invid) keys.nextElement();
		Hashtable ptrTable = (Hashtable) Ganymede.db.backPointers.get(key);
		Enumeration backpointers = ptrTable.keys();

		while (backpointers.hasMoreElements())
		  {
		    Invid backTarget = (Invid) backpointers.nextElement();

		    if (session.viewDBObject(backTarget) == null)
		      {
			ok = false;
		    
			Ganymede.debug("*** Backpointers hash for object " +
				       Ganymede.internalSession.describe(key) +
				       " has an invid pointing to a non-existent object: " + backTarget);
		      }
		  }
	      }
	  }
      }
    finally
      {
	lock.release();
      }

    Ganymede.debug("Ganymede invid link test complete");

    return ok;
  }
}
