/*

   GanymedeSession.java

   This class is the heart of the Ganymede server.

   Each client logged on to the server will hold a reference to a unique
   GanymedeSession object.  This object provides services to the client, tracks
   the client's status, manages permissions, and keeps track of the client's
   transactions.

   Most methods in this class are synchronized to avoid race condition
   security holes between the persona change logic and the actual
   operations.

   Created: 17 January 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.net.ProtocolException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import Qsmtp.Qsmtp;
import arlut.csd.Util.RandomUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.WordWrap;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.ganymede.common.AdminEntry;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.ClientAsyncResponder;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 GanymedeSession

------------------------------------------------------------------------------*/

/**
 * <p>User-level session object in the Ganymede server.  Each client
 * that logs in to the Ganymede server through the {@link
 * arlut.csd.ganymede.server.GanymedeServer GanymedeServer} {@link
 * arlut.csd.ganymede.server.GanymedeServer#login(java.lang.String, java.lang.String)
 * login()} method gets a GanymedeSession object, which oversees that
 * client's interactions with the server.  The client talks to its
 * GanymedeSession object through the {@link
 * arlut.csd.ganymede.rmi.Session Session} RMI interface, making calls to
 * access schema information and database objects in the server.</p>
 *
 * <p>The GanymedeSession class provides query and editing services to
 * the client, tracks the client's status, and manages permissions.</p>
 *
 * <p>Permissions are managed through the contained {@link
 * arlut.csd.ganymede.server.DBPermissionManager} instance, and
 * querying services are handled by a {@link
 * arlut.csd.ganymede.server.DBQueryEngine} instance.</p>
 *
 * <p>The actual data store handling is done through a second,
 * database-layer session object called a {@link
 * arlut.csd.ganymede.server.DBSession DBSession} that GanymedeSession
 * maintains for each user.  GanymedeSession methods like {@link
 * arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
 * view_db_object()} and {@link
 * arlut.csd.ganymede.server.GanymedeSession#edit_db_object(arlut.csd.ganymede.common.Invid)
 * edit_db_object()} make calls on the DBSession to actually access
 * {@link arlut.csd.ganymede.server.DBObject DBObjects} in the
 * Ganymede database.  The client then receives a serialized {@link
 * arlut.csd.ganymede.common.ReturnVal ReturnVal} which may include a
 * remote {@link arlut.csd.ganymede.rmi.db_object db_object} reference
 * so that the client can directly talk to the DBObject or {@link
 * arlut.csd.ganymede.server.DBEditObject DBEditObject} over RMI.</p>
 *
 * <p>Once a GanymedeSession is created by the GanymedeServer's login()
 * method, the client is considered to be authenticated, and may make
 * as many Session calls as it likes, until the GanymedeSession's
 * {@link arlut.csd.ganymede.server.GanymedeSession#logout() logout()} method
 * is called.  If the client dies or loses its network connection to
 * the Ganymede server for more than 10 minutes, the RMI system will
 * automatically call GanymedeSession's
 * {@link arlut.csd.ganymede.server.GanymedeSession#unreferenced() unreferenced()}
 * method, which will log the client out from the server.</p>
 *
 * <p>The Ganymede server is transactional, and the client must call
 * {@link
 * arlut.csd.ganymede.server.GanymedeSession#openTransaction(java.lang.String)
 * openTransaction()} before making any changes via edit_db_object()
 * or other editing methods.  In turn, changes made by the client
 * using the edit_db_object() and related methods will not actually be
 * 'locked-in' to the Ganymede database until the {@link
 * arlut.csd.ganymede.server.GanymedeSession#commitTransaction()
 * commitTransaction()} method is called.</p>
 *
 * <p>Most methods in this class are synchronized to avoid race condition
 * security holes between the persona change logic and the actual operations.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

final public class GanymedeSession implements Session, Unreferenced {

  static final boolean debug = false;
  static final boolean permsdebug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeSession");

  // ---

  /**
   * <p>This variable tracks whether or not the client desires to have
   * wizards presented.  If this is false, custom plug-in code
   * for the object types stored in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore} may either
   * refuse certain operations or will resort to taking a default action.</p>
   *
   * <p>Note: this variable should be private with an accessor, but
   * end-user custom code has been written which uses this, so we have
   * to keep it public.</p>
   */

  public boolean enableWizards = true;

  /**
   * <p>If this variable is set to false, no custom wizard code will ever
   * be invoked, and required fields will not be forced.  This is
   * intended primarily for direct database loading.</p>
   *
   * <p>This variable is not intended ever to be available to the client,
   * but should only be set by local server code.</p>
   *
   * <p>Note: this variable should be private with an accessor, but
   * end-user custom code has been written which uses this, so we have
   * to keep it public.</p>
   */

  public boolean enableOversight = true;

  // --

  /**
   * Async responder for sending async messages to the client.
   */

  private serverClientAsyncResponder asyncPort = null;

  /**
   * <p>This tracks whether this GanymedeSession counts as a 'login
   * session' which increments {@link arlut.csd.ganymedes.server.GanymedeServer#lSemaphore}.</p>
   *
   * <p>If semaphoreLocked is true, that means that either the {@link
   * arlut.csd.ganymedes.erver.GanymedeServer#processLogin(java.lang.String,java.lang.String,boolean,boolean)}
   * method or the appropriate GanymedeSession constructor has caused
   * the GanymedeServer lSemaphore counting semaphore to be
   * incremented, and we'll need to make sure and decrement the
   * semaphore when this GanymedeSession is shut down.</p>
   */

  private boolean semaphoreLocked = false;

  /**
   * If this flag is true, we're being used by a remote client
   */

  private boolean remoteClient = false;

  /**
   * If true, the user is currently logged in.
   */

  private booleanSemaphore loggedInSemaphore = new booleanSemaphore(false);

  /**
   * If true, the user has had a soft timeout and needs to
   * re-authenticate with their password, even for their
   * non-privileged username
   */

  private boolean timedout = false;

  /**
   * A count of how many objects this session has currently checked out.
   */

  private int objectsCheckedOut = 0;

  /**
   * The time that this client initially connected to the server.  Used
   * by the admin console code.
   */

  private Date connecttime;

  /**
   * The time of the user's last top-level operation.. Used to
   * provide guidance on time-outs.  Updated whenever checklogin()
   * is called.
   */

  private Date lastActionTime = new Date();

  /**
   * <p>The DNS name or IP address name for the client's host.</p>
   *
   * <p>If this session is being run on behalf of a Ganymede server
   * task or internal process, clienthost will be the DNS name that
   * the server is running on.</p>
   */

  private String clienthost;

  /**
   * The current status message for this client.  The
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} code
   * that manages the admin consoles will consult this String when it
   * updates the admin consoles.
   */

  private String status = null;

  /**
   * Description of the last action recorded for this client.  The
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}
   * code that manages the admin consoles will consult
   * this String when it updates the admin consoles.
   */

  private String lastEvent = null;

  /**
   * <p>Our DBSession object.  DBSession is the generic DBStore access
   * layer.  A GanymedeSession is layered on top of a DBSession to
   * provide access control and remote access via RMI.  The DBSession
   * object is accessible to server-side code only and provides
   * transaction support.</p>
   */

  private DBSession dbSession;

  /**
   * Our DBQueryEngine object.  DBQueryEngine has all the routines for
   * doing all of this GanymedeSession's queries on the Ganymede
   * datastore with permission and transaction awareness.
   */

  private DBQueryEngine queryEngine;

  /**
   * Our DBPermissionManager object.  DBPermissionManager manages our
   * access privileges.
   */

  private DBPermissionManager permManager;

  /**
   * A GanymedeSession can have a single wizard active.  If this variable
   * is non-null, a custom type-specific
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass has instantiated
   * a wizard to interact with the user.
   */

  private GanymediatorWizard wizard = null;

  /**
   * This variable caches the {@link arlut.csd.ganymede.common.AdminEntry AdminEntry}
   * object which is reported to admin consoles connected to the
   * server when the console is updated.
   */

  private AdminEntry userInfo = null;

  /**
   * If true, this GanymedeSession will export its objects and fields for
   * direct access via RMI.
   */

  private boolean exportObjects = false;

  /**
   * If this session is being driven by a GanymedeXMLSession, this reference
   * will be non-null.
   */

  private GanymedeXMLSession xSession = null;

  /**
   * List of exported DBObjects (and DBEditObjects and subclasses thereof), so we
   * can forcibly unexport them at logout time.
   */

  private ArrayList<DBObject> exported = new ArrayList<DBObject>();

  /* -- */

  /**
   * <p>Constructor for a server-internal GanymedeSession.  Used when
   * the server's internal code needs to do a query, etc.  Note that
   * the Ganymede server will create one of these fairly early
   * on, and will keep it around for internal usage.  Note that we
   * don't add this to the data structures used for the admin
   * console.</p>
   *
   * <p>Note that all internal session activities (queries, etc.) are
   * currently using a single, synchronized GanymedeSession object.. this
   * mean that only one user at a time can currently be processed for
   * login. 8-(</p>
   *
   * <p>Internal sessions, as created by this constructor, have full
   * privileges to do any possible operation.</p>
   */

  public GanymedeSession() throws RemoteException
  {
    // XXX note: this string must not be changed because the
    // GanymedeSession constructor behaves in a special way
    // for "internal: " session labels.

    this("internal:");
  }

  /**
   * <p>Constructor for a server-internal GanymedeSession.  Used when
   * the server's internal code needs to do a query, etc.  Note that
   * the Ganymede server will create one of these fairly early
   * on, and will keep it around for internal usage.  Note that we
   * don't add this to the data structures used for the admin
   * console.</p>
   *
   * <p>Note that all internal session activities (queries, etc.) are
   * currently using a single, synchronized GanymedeSession object.. this
   * mean that only one user at a time can currently be processed for
   * login. 8-(</p>
   *
   * <p>Internal sessions, as created by this constructor, have full
   * privileges to do any possible operation.</p>
   *
   * @param sessionName A name for this internal session.  There
   * should not be two or more GanymedeSessions active concurrently
   * with the same sessionName.
   */

  public GanymedeSession(String sessionName) throws RemoteException
  {
    if (sessionName.startsWith("builder:") || sessionName.startsWith("sync channel:"))
      {
	String disabledMessage = GanymedeServer.lSemaphore.checkEnabled();

	// if we are attempting to start a builder session, we'll
	// proceed even if the server is waiting to handle a deferred
	// shutdown.  Otherwise we'll abort.

	if (disabledMessage != null && !disabledMessage.equals("shutdown"))
	  {
	    // "Couldn''t create {0} GanymedeSession.. semaphore disabled: {1}"
	    Ganymede.debug(ts.l("init.no_semaphore", sessionName, disabledMessage));

	    // "semaphore error: {0}"
	    throw new RuntimeException(ts.l("init.semaphore_error", disabledMessage));
	  }
      }
    else if (!sessionName.startsWith("internal:"))
      {
	// otherwise, if we are not starting one of the master internal
	// sessions (either Ganymede.internalSession or
	// GanymedeServer.loginSession), we'll want to increment the login
	// semaphore to make sure we are allowing logins and to keep the
	// server up to date

        String error = GanymedeServer.lSemaphore.increment();

        if (error != null)
          {
	    // "Couldn''t create {0} GanymedeSession.. semaphore disabled: {1}"
            Ganymede.debug(ts.l("init.no_semaphore", sessionName, error));

	    // "semaphore error: {0}"
            throw new RuntimeException(ts.l("init.semaphore_error", error));
          }

	semaphoreLocked = true;
      }

    // make sure our session name is unique for locking in builder tasks, etc.

    sessionName = GanymedeServer.registerActiveInternalSessionName(sessionName);

    // construct our DBSession

    loggedInSemaphore.set(true);

    this.exportObjects = false;
    clienthost = Ganymede.serverHostProperty;

    dbSession = new DBSession(Ganymede.db, this, sessionName);
    queryEngine = new DBQueryEngine(this, dbSession);
    permManager = new DBPermissionManager(this).configureInternalSession(sessionName);
  }

  /**
   * <p>Constructor used to create a server-side attachment for a Ganymede
   * client.</p>
   *
   * <p>This constructor is called by the
   * {@link arlut.csd.ganymede.server server}
   * {@link arlut.csd.ganymede.rmi.Server#login(java.lang.String username, java.lang.String password) login()}
   * method.</p>
   *
   * <p>A Client can log in either as an end-user or as a admin persona.  Typically,
   * a client will log in with their end-user name and password, then use
   * selectPersona to gain admin privileges.  The server may allow users to
   * login directly with an admin persona (supergash, say), if so configured.</p>
   *
   * @param loginName The name for the user logging in
   * @param userObject The user record for this login
   * @param personaObject The user's initial admin persona
   * @param exportObjects If true, we'll export any viewed or edited objects for
   * direct RMI access.  We don't need to do this is we're being driven by a server-side
   * {@link arlut.csd.ganymede.server.GanymedeXMLSession}, for instance.
   * @param clientIsRemote If true, we're being driven remotely, either by a direct
   * Ganymede client or by a remotely operated GanymedeXMLSession.  We'll set up an
   * {@link arlut.csd.ganymede.server.serverClientAsyncResponder serverClientAsyncResponder},
   * AKA an asyncPort, for the remote client to poll for async notifications.
   *
   * @see arlut.csd.ganymede.rmi.Server#login(java.lang.String, java.lang.String)
   */

  public GanymedeSession(String loginName, DBObject userObject,
			 DBObject personaObject, boolean exportObjects,
			 boolean clientIsRemote) throws RemoteException
  {
    // GanymedeServer will have already incremented our semaphore in
    // its login() method

    semaphoreLocked = true;

    // record whether we're being driven remotely

    this.remoteClient = clientIsRemote;

    // record whether we should export our objects

    this.exportObjects = exportObjects;

    if (this.remoteClient)
      {
	asyncPort = new serverClientAsyncResponder();

	Ganymede.rmi.publishObject(this);
      }

    // find a unique name for this user session

    String sessionName = GanymedeServer.registerActiveUserSessionName(loginName);

    // find out where the user is coming from

    try
      {
	String ipAddress = java.rmi.server.RemoteServer.getClientHost();

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
	clienthost = "(unknown)";
      }

    // record our login time

    connecttime = new Date();

    // construct our DBSession

    dbSession = new DBSession(Ganymede.db, this, sessionName);
    queryEngine = new DBQueryEngine(this, dbSession);
    permManager = new DBPermissionManager(this).configureClientSession(userObject, personaObject, sessionName);

    // Let the GanymedeServer know that this session is now active for
    // purposes of admin console updating.

    GanymedeServer.addRemoteUser(this);

    // update status, update the admin consoles

    loggedInSemaphore.set(true);
    status = ts.l("init.loggedin");
    lastEvent = ts.l("init.loggedin");
    GanymedeAdmin.refreshUsers();
  }

  //
  //************************************************************
  //
  //
  //
  // All methods from this point on are part of the Server remote
  // interface, and can be called by the client via RMI.
  //
  //
  //
  //************************************************************

  /**
   * <p>Log out this session.  After this method is called, no other
   * methods may be called on this session object.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void logout()
  {
    try
      {
	checklogin();
      }
    catch (NotLoggedInException ex)
      {
	// the only RMI-accessible method on GanymedeSession not to
	// throw a NotLoggedInException, for backwards compatibility
	// reasons.

	return;
      }

    this.logout(false);
  }

  /**
   * <p>This method is used to allow a client to request that wizards
   * not be provided in response to actions by the client.  This
   * is intended to allow non-interactive or non-gui clients to
   * do work without having to go through a wizard interaction
   * sequence.</p>
   *
   * <p>Wizards are enabled by default.</p>
   *
   * @param val If true, wizards will be enabled.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void enableWizards(boolean val) throws NotLoggedInException
  {
    checklogin();

    this.enableWizards = val;
  }

  /**
   * <p>This method is used to tell the client where to look
   * to access the Ganymede help document tree.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public String getHelpBase() throws NotLoggedInException
  {
    checklogin();

    return Ganymede.helpbaseProperty;
  }

  /**
   * <p>This method is used to allow the client to retrieve messages
   * like the motd from the server.  The client can specify that it
   * only wants to see a message if it has changed since the user last
   * logged out.  This is intended to support a message of the day
   * type functionality.  The server will not necessarily remember the
   * last log out across server restart.</p>
   *
   * @param key A string, like "motd", indicating what message to retrieve.
   * @param onlyShowIfNew If true, the message will only be returned if
   *                      it has changed since the user last logged out.
   *
   * @return A StringBuffer containing the message, if found, or null if no
   * message exists for the key, or if onlyShowIfNew was set and the message
   * was not new.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer getMessage(String key, boolean onlyShowIfNew) throws NotLoggedInException
  {
    checklogin();

    Invid invidToCompare = null;

    if (onlyShowIfNew)
      {
	invidToCompare = permManager.getIdentityInvid();
      }

    return GanymedeServer.getTextMessage(key, invidToCompare, false);
  }

  /**
   * <p>This method is used to allow the client to retrieve messages like
   * the motd from the server.  The client can specify that it only
   * wants to see a message if it has changed since the user last
   * logged out.  This is intended to support a message of the day
   * type functionality.  The server will not necessarily remember the
   * last log out across server restart.</p>
   *
   * @param key A string, like "motd", indicating what message to retrieve.
   * @param onlyShowIfNew If true, the message will only be returned if
   *                      it has changed since the user last logged out.
   *
   * @return A StringBuffer containing the message, if found, or null if no
   * message exists for the key, or if onlyShowIfNew was set and the message
   * was not new.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer getMessageHTML(String key, boolean onlyShowIfNew) throws NotLoggedInException
  {
    checklogin();

    Invid invidToCompare = null;

    if (onlyShowIfNew)
      {
	invidToCompare = permManager.getIdentityInvid();
      }

    return GanymedeServer.getTextMessage(key, invidToCompare, true);
  }

  /**
   * <p>This method is used to allow the client to retrieve a remote reference to
   * a {@link arlut.csd.ganymede.server.serverClientAsyncResponder}, which will allow
   * the client to poll the server for asynchronous messages from the server.</p>
   *
   * <p>This is used to allow the server to send build status change notifications and
   * shutdown notification to the client, even if the client is behind a network
   * or personal system firewall.  The serverClientAsyncResponder blocks while there
   * is no message to send, and the client will poll for new messages.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ClientAsyncResponder getAsyncPort() throws NotLoggedInException, RemoteException
  {
    checklogin();

    if (asyncPort != null)
      {
	return asyncPort;
      }

    asyncPort = new serverClientAsyncResponder();
    return (ClientAsyncResponder) asyncPort;
  }

  /**
   * <p>This method returns the identification string that the server
   * has assigned to the user.</p>
   *
   * <p>May return null if this session is running on behalf of a
   * Ganymede server task or internal process.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public String getMyUserName() throws NotLoggedInException
  {
    checklogin();

    return permManager.getUserName();
  }

  /**
   * <p>This method returns a list of personae names available to the
   * user logged in.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector getPersonae() throws NotLoggedInException
  {
    checklogin();

    return permManager.getAvailablePersonae();
  }

  /**
   * This method returns the persona name for the user, or null if
   * the session is non-privileged.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized String getActivePersonaName() throws NotLoggedInException
  {
    checklogin();		// this resets lastAction

    return permManager.getPersonaName();
  }

  /**
   * This method is used to select an admin persona, changing the
   * permissions that the user has and the objects that are
   * accessible in the database.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized boolean selectPersona(String newPersona, String password) throws NotLoggedInException
  {
    checklogin();		// this resets lastAction

    boolean success = false;

    /* -- */

    if (getUser() == null)
      {
	// this session is not associated with a user in the ganymede
	// datastore.. we don't support persona changing in such
	// GanymedeSessions.

	return false;
      }

    if (getUser().getLabel().equals(newPersona) && timedout)
      {
	// if we've timed out and we're switching to our base user
	// privileges, one of two things might be happening here.  if
	// the client gave us a password, they may be revalidating
	// their login for end-user access.
	//
	// if they gave us no password, the client is itself
	// downshifting to the non-privileged level.

	if (password == null)
	  {
	    // "User {0}''s privileged login as {1} timed out.  Downshifting to non-privileged access."
	    Ganymede.debug(ts.l("selectPersona.giving_up",
				permManager.getUserName(),
				permManager.getPersonaName()));
	  }
	else
	  {
	    // "User {0} attempting to re-authenticate non-privileged login after being timed out."
	    Ganymede.debug(ts.l("selectPersona.attempting_timecheck",
				permManager.getUserName()));
	  }
      }

    success = permManager.selectPersona(newPersona, password);

    if (success)
      {
	timedout = false;
      }
    else if (timedout)
      {
	// "User {0} failed to re-authenticate a login that timed out."
	Ganymede.debug(ts.l("selectPersona.failed_timecheck", permManager.getUserName()));
	return false;
      }

    return success;
  }

  /**
   * <p>This method returns a QueryResult of owner groups that the
   * current persona has access to.  This list is the transitive
   * closure of the list of owner groups in the current persona.  That
   * is, the list includes all the owner groups in the current persona
   * along with all of the owner groups those owner groups own, and so
   * on.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized QueryResult getOwnerGroups() throws NotLoggedInException
  {
    checklogin();

    return permManager.getAvailableOwnerGroups();
  }

  /**
   * <p>This method may be used to set the owner groups of any objects
   * created hereafter.</p>
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal setDefaultOwner(Vector<Invid> ownerInvids) throws NotLoggedInException
  {
    checklogin();

    return permManager.setDefaultOwner(ownerInvids);
  }

  /**
   * <p>This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.</p>
   *
   * <p>This method cannot be used to grant access to objects that are
   * not accessible by the client's adminPersona.</p>
   *
   * <p>Calling this method with ownerInvids set to null will turn off the filtering.</p>
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal filterQueries(Vector<Invid> ownerInvids) throws NotLoggedInException
  {
    checklogin();

    return permManager.filterQueries(ownerInvids);
  }

  //  Database operations

  /**
   * <p>Returns a serialized representation of the basic category and
   * base structure on the server.  The returned CategoryTransport
   * will include only object types that are editable by the user.</p>
   *
   * @see arlut.csd.ganymede.rmi.Category
   * @see arlut.csd.ganymede.rmi.Session
   */

  public CategoryTransport getCategoryTree() throws NotLoggedInException
  {
    checklogin();

    return this.getCategoryTree(true);
  }

  /**
   * <p>Returns a serialized representation of the basic category
   * and base structure on the server.</p>
   *
   * <p>This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the CategoryTransport
   * constructor calls other synchronized methods on GanymedeSession.</p>
   *
   * @param hideNonEditables If true, the CategoryTransport returned
   * will only include those object types that are editable by the
   * client.
   *
   * @see arlut.csd.ganymede.rmi.Category
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized CategoryTransport getCategoryTree(boolean hideNonEditables) throws NotLoggedInException
  {
    checklogin();

    return getPermManager().getCategoryTree(hideNonEditables);
  }

  /**
   * <p>Returns a serialized representation of the object types
   * defined on the server.  This BaseListTransport object
   * will not include field information.  The client is
   * obliged to call getFieldTemplateVector() on any
   * bases that it needs field information for.</p>
   *
   * <p>This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the BaseListTransport
   * constructor calls other synchronized methods on GanymedeSession.</p>
   *
   * @see arlut.csd.ganymede.common.BaseListTransport
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized BaseListTransport getBaseList() throws NotLoggedInException
  {
    checklogin();

    return getPermManager().getBaseList();
  }

  /**
   * <p>Returns a vector of field definition templates, in display order.</p>
   *
   * <p>This vector may be cached, as it is static for this object type over
   * the lifetime of any GanymedeSession.</p>
   *
   * @see arlut.csd.ganymede.common.FieldTemplate
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector<FieldTemplate> getFieldTemplateVector(short baseId) throws NotLoggedInException
  {
    checklogin();

    /* - */

    DBObjectBase base = Ganymede.db.getObjectBase(baseId);
    return base.getFieldTemplateVector();
  }

  /**
   * <p>Returns a vector of field definition templates, in display order.</p>
   *
   * <p>This vector may be cached, as it is static for this object type over
   * the lifetime of any GanymedeSession.</p>
   *
   * @see arlut.csd.ganymede.common.FieldTemplate
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector<FieldTemplate> getFieldTemplateVector(String baseName) throws NotLoggedInException
  {
    checklogin();

    /* - */

    DBObjectBase base = Ganymede.db.getObjectBase(baseName);
    return base.getFieldTemplateVector();
  }

  /**
   * <p>This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).</p>
   *
   * <p>Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an error dialog will be returned in that case.</p>
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal openTransaction(String describe) throws NotLoggedInException
  {
    checklogin();

    return this.openTransaction(describe, true);
  }

  /**
   * <p>This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).</p>
   *
   * <p>Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an error dialog will be returned in that case.</p>
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal openTransaction(String describe, boolean interactive) throws NotLoggedInException
  {
    checklogin();

    if (interactive)
      {
	// the client will perform an openTransaction as soon as it is
	// ready to talk to the server.  By sending a building message to
	// the client here, we allow it to set the initial state of the
	// building/idle icon in the client's display

	// note that since we don't sync on GanymedeBuilderTask in any
	// way, we could maybe conceivably get out of sync a bit.

	if (GanymedeBuilderTask.getPhase1Count() > 0)
	  {
	    sendMessage(ClientMessage.BUILDSTATUS, "building");
	  }
	else if (GanymedeBuilderTask.getPhase2Count() > 0)
	  {
	    sendMessage(ClientMessage.BUILDSTATUS, "building2");
	  }
	else
	  {
	    sendMessage(ClientMessage.BUILDSTATUS, "idle");
	  }
      }

    if (dbSession.editSet != null)
      {
        // "Server: Error in openTransaction()"
        // "Error.. transaction already opened"
	return Ganymede.createErrorDialog(ts.l("openTransaction.error"),
					  ts.l("openTransaction.error_text"));
      }

    /* - */

    dbSession.openTransaction(describe, interactive); // *sync* DBSession

    this.status = "Transaction: " + describe;
    setLastEvent("openTransaction");

    return null;
  }

  /**
   * <p>This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.</p>
   *
   * <p>commitTransaction() will return a ReturnVal indicating whether or
   * not the transaction could be committed, and whether or not the
   * transaction remains open for further attempts at commit.  If
   * ReturnVal.doNormalProcessing is set to true, the transaction
   * remains open and it is up to the client to decide whether to
   * abort the transaction by calling abortTransaction(), or to
   * attempt to fix the reported problem and try another call to
   * commitTransaction().</p>
   *
   * @return null if the transaction could be committed without comment, or
   *         a ReturnVal object if there were problems or a need for comment.
   *         If the transaction was forcibly terminated due to a major error,
   *         the doNormalProcessing flag in the returned ReturnVal will be
   *         set to false.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal commitTransaction() throws NotLoggedInException
  {
    checklogin();

    return commitTransaction(false);
  }

  /**
   * <p>This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.</p>
   *
   * <p>If the transaction cannot be committed for some reason,
   * commitTransaction() will abort the transaction if abortOnFail is
   * true.  In any case, commitTransaction() will return a ReturnVal
   * indicating whether or not the transaction could be committed, and
   * whether or not the transaction remains open for further attempts
   * at commit.  If ReturnVal.doNormalProcessing is set to true, the
   * transaction remains open and it is up to the client to decide
   * whether to abort the transaction by calling abortTransaction(),
   * or to attempt to fix the reported problem and try another call
   * to commitTransaction().</p>
   *
   * <p>This method is synchronized to avoid nested-monitor deadlock in
   * {@link arlut.csd.ganymede.server.DBSession#commitTransaction() DBSession.commitTransaction()}.</p>
   *
   * @param abortOnFail If true, the transaction will be aborted if it
   * could not be committed successfully.
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems.  If the transaction was
   *         forcibly terminated due to a major error, the
   *         doNormalProcessing flag in the returned ReturnVal will be
   *         set to false.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal commitTransaction(boolean abortOnFail) throws NotLoggedInException
  {
    checklogin();

    return commitTransaction(abortOnFail, null);
  }

  /**
   * <p>This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.</p>
   *
   * <p>If the transaction cannot be committed for some reason,
   * commitTransaction() will abort the transaction if abortOnFail is
   * true.  In any case, commitTransaction() will return a ReturnVal
   * indicating whether or not the transaction could be committed, and
   * whether or not the transaction remains open for further attempts
   * at commit.  If ReturnVal.doNormalProcessing is set to true, the
   * transaction remains open and it is up to the client to decide
   * whether to abort the transaction by calling abortTransaction(),
   * or to attempt to fix the reported problem and try another call
   * to commitTransaction().</p>
   *
   * <p>This method is synchronized to avoid nested-monitor deadlock in
   * {@link arlut.csd.ganymede.server.DBSession#commitTransaction() DBSession.commitTransaction()}.</p>
   *
   * @param abortOnFail If true, the transaction will be aborted if it
   * could not be committed successfully.
   * @param comment If not null, a comment to attach to logging and
   * email generated in response to this transaction.
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems.  If the transaction was
   *         forcibly terminated due to a major error, the
   *         doNormalProcessing flag in the returned ReturnVal will be
   *         set to false.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal commitTransaction(boolean abortOnFail, String comment) throws NotLoggedInException
  {
    checklogin();

    if (dbSession.editSet == null)
      {
	return Ganymede.createErrorDialog(ts.l("commitTransaction.error"),
					  ts.l("commitTransaction.error_text"));
      }

    /* - */

    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	Ganymede.debug("commitTransaction(" + abortOnFail +")");
      }

    this.status = "";
    setLastEvent("commitTransaction");

    retVal = dbSession.commitTransaction(comment); // *sync* DBSession DBEditSet

    // if we succeeded, we'll schedule our
    // builder tasks to run

    if (ReturnVal.didSucceed(retVal))
      {
	if (isWizardActive())
	  {
	    getWizard().unregister();

	    // and just in case unregister() was overridden

	    if (this.wizard != null)
	      {
		this.wizard = null;
	      }
	  }

        // "User {0} committed transaction."
        GanymedeServer.sendMessageToRemoteSessions(ClientMessage.COMMITNOTIFY,
						   ts.l("commitTransaction.user_committed",
							permManager.getIdentity()),
						   this);
	Ganymede.runBuilderTasks();
	unexportObjects(false);
      }
    else
      {
	// We are only calling abortTransaction() here if the
	// transaction failed, and we were asked to do a full abort on
	// any failure, *and* if the commitTransaction() logic didn't
	// itself clear out the transaction.  If doNormalProcessing
	// were false here, we wouldn't call abortTransaction(),
	// because the DBSession.commitTransaction() method would have
	// done that for us.

	if (abortOnFail && retVal.doNormalProcessing)
	  {
	    abortTransaction();
	  }
      }

    return retVal;
  }

  /**
   * <p>This method causes all changes made by the client to be thrown
   * out by the database, and the transaction is closed.</p>
   *
   * @return a ReturnVal object if the transaction could not be aborted,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal abortTransaction() throws NotLoggedInException
  {
    checklogin();

    if (dbSession.editSet == null)
      {
	throw new IllegalArgumentException(ts.l("abortTransaction.exception"));
      }

    /* - */

    this.status = "";
    setLastEvent("abortTransaction");

    if (isWizardActive())
      {
	getWizard().unregister();

	// and just in case unregister() was overridden

	if (this.wizard != null)
	  {
	    this.wizard = null;
	  }
      }

    unexportObjects(false);

    ReturnVal retVal = dbSession.abortTransaction(); // *sync* DBSession

    // "User {0} cancelled transaction."
    GanymedeServer.sendMessageToRemoteSessions(ClientMessage.ABORTNOTIFY,
					       ts.l("abortTransaction.user_aborted",
						    permManager.getIdentity()),
					       this);

    return retVal;
  }

  /**
   * <p>This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.</p>
   *
   * <p>body and HTMLbody are StringBuffer's instead of Strings
   * because RMI (formerly had) a 64k serialization limit on the
   * String class.</p>
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The content of the message.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void sendMail(String address, String subject, StringBuffer body) throws NotLoggedInException
  {
    checklogin();

    // If the server has been told to not send out any emails, then just bail
    // out.

    if (Ganymede.suppressEmail)
      {
      	return;
      }

    if (StringUtils.isEmpty(address))
      {
	return;
      }

    /* - */

    Qsmtp mailer;
    StringBuilder signature = new StringBuilder();
    Vector addresses = new Vector();
    StringTokenizer tokens = new StringTokenizer(address, ", ", false);

    /* -- */

    mailer = new Qsmtp(Ganymede.mailHostProperty);

    try
      {
        while (tokens.hasMoreElements())
          {
            addresses.addElement(tokens.nextToken());
          }

        // create the signature

	if (exportObjects)
	  {
	    // "This message was sent by {0}, who is running the Ganymede client on {1}."
	    signature.append(ts.l("sendMail.signature", permManager.getUserName(), getClientHostName()));
	  }
	else
	  {
	    // "This message was sent by the {0} process, running inside the Ganymede server."
	    signature.append(ts.l("sendMail.local_signature", permManager.getSessionName()));
	  }

        body.append("\n--------------------------------------------------------------------------------\n");
        body.append(WordWrap.wrap(signature.toString(), 78, null));
        body.append("\n--------------------------------------------------------------------------------\n");

        try
          {
            mailer.sendmsg(permManager.getIdentityReturnAddress(),
                           addresses,
                           "Ganymede: " + subject,
                           body.toString());
          }
        catch (ProtocolException ex)
          {
            throw new RuntimeException("Couldn't figure address " + ex);
          }
        catch (IOException ex)
          {
            throw new RuntimeException("IO problem " + ex);
          }
      }
    finally
      {
        mailer.close();
      }
  }

  /**
   * <p>This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.</p>
   *
   * <p>body and HTMLbody are StringBuffer's instead of Strings because RMI
   * has a 64k serialization limit on the String class.</p>
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The plain-ASCII content of the message, or null if none.
   * @param HTMLbody The HTML content of the message, or null if none.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void sendHTMLMail(String address, String subject, StringBuffer body, StringBuffer HTMLbody) throws NotLoggedInException
  {
    checklogin();

    // If the server has been told to not send out any emails, then just bail
    // out.

    if (Ganymede.suppressEmail)
      {
      	return;
      }

    /* - */

    Qsmtp mailer;
    StringBuilder asciiContent = new StringBuilder();
    StringBuilder signature = new StringBuilder();
    Vector addresses = new Vector();
    StringTokenizer tokens = new StringTokenizer(address, ", ", false);

    /* -- */

    mailer = new Qsmtp(Ganymede.mailHostProperty);

    try
      {
        while (tokens.hasMoreElements())
          {
            addresses.addElement(tokens.nextToken());
          }

        // create the signature

        if (body != null)
          {
            asciiContent.append(body);
            asciiContent.append("\n\n");
          }

	if (exportObjects)
	  {
	    // "This message was sent by {0}, who is running the Ganymede client on {1}."
	    signature.append(ts.l("sendMail.signature", permManager.getUserName(), getClientHostName()));
	  }
	else
	  {
	    // "This message was sent by the {0} process, running inside the Ganymede server."
	    signature.append(ts.l("sendMail.local_signature", permManager.getSessionName()));
	  }

        asciiContent.append("\n--------------------------------------------------------------------------------\n");
        asciiContent.append(WordWrap.wrap(signature.toString(), 78, null));
        asciiContent.append("\n--------------------------------------------------------------------------------\n");

        try
          {
            mailer.sendHTMLmsg(permManager.getIdentityReturnAddress(),
                               addresses,
                               "Ganymede: " + subject,
                               (HTMLbody != null) ? HTMLbody.toString(): null,
                               "greport.html",
                               asciiContent.toString());
          }
        catch (ProtocolException ex)
          {
            throw new RuntimeException("Couldn't figure address " + ex);
          }
        catch (IOException ex)
          {
            throw new RuntimeException("IO problem " + ex);
          }
      }
    finally
      {
        mailer.close();
      }
  }

  /**
   * <p>This method allows clients to report client-side error/exception traces to
   * the server for logging and what-not.</p>
   *
   * <p>This method will also email the bug report to the email address
   * specified in the ganymede.bugsaddress property if it is set in
   * the server's ganymede.properties file.  This doesn't happen, of course,
   * if the server's emailing is disabled, either through the use of the
   * -suppressEmail flag in runServer, or by leaving the ganymede.mailhost
   * property undefined in ganymede.properties.</p>
   *
   * @param clientIdentifier A string identifying any information
   * about the client that the client feels like providing.
   * @param exceptionReport A string holding any stack trace
   * information that might be helpful for the server to log or
   * transmit to a developer.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void reportClientBug(String clientIdentifier, String exceptionReport) throws NotLoggedInException
  {
    checklogin();

    StringBuffer report = new StringBuffer();

    // "\nCLIENT ERROR DETECTED:\nuser == "{0}"\nhost == "{1}"\nclient id string == "{2}"\nexception trace == "{3}"\n"
    report.append(ts.l("reportClientBug.logPattern",
		       permManager.getIdentity(),
		       getClientHostName(),
		       clientIdentifier,
		       exceptionReport));

    Ganymede.debug(report.toString());

    if (Ganymede.bugReportAddressProperty != null && !Ganymede.bugReportAddressProperty.equals(""))
      {
	sendMail(Ganymede.bugReportAddressProperty, "Ganymede Client Bug Report", report);
      }
  }

  /**
   * <p>This method allows clients to report their version information to
   * the server for logging and what-not.</p>
   *
   * <p>This method will also email the bug report to the email address.</p>
   *
   * @param clientIdentifier A string identifying any information
   * about the client that the client feels like providing.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void reportClientVersion(String clientIdentifier) throws NotLoggedInException
  {
    checklogin();

    StringBuffer report = new StringBuffer();

    // "\nClient Version Report:\nuser == "{0}"\nhost == "{1}"\nclient id string == "{2}"
    report.append(ts.l("reportClientVersion.logPattern",
		       permManager.getIdentity(),
		       getClientHostName(),
		       clientIdentifier));

    Ganymede.debug(report.toString());
  }

  /**
   * <p>This method provides the hook for doing a
   * fast database dump to a string form.  The
   * {@link arlut.csd.ganymede.common.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.</p>
   *
   * <p>This version of dump() takes a query in string
   * form, based on Deepak's ANTLR-specified Ganymede
   * query grammar.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.common.Query
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized DumpResult dump(String queryString) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    return queryEngine.dump(queryString);
  }

  /**
   * <p>This method provides the hook for doing a
   * fast database dump to a string form.  The
   * {@link arlut.csd.ganymede.common.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.common.Query
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized DumpResult dump(Query query) throws NotLoggedInException
  {
    checklogin();

    return queryEngine.dump(query);
  }

  /**
   * <p>This method allows the client to get a status update on a
   * specific list of invids.</p>
   *
   * <p>If any of the invids are not currently defined in the server, or
   * if the client doesn't have permission to view any of the invids,
   * those invids' status will not be included in the returned
   * QueryResult.</p>
   *
   * @param invidVector Vector of Invid's to get the status for.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized QueryResult queryInvids(Vector<Invid> invidVector) throws NotLoggedInException
  {
    checklogin();

    return queryEngine.queryInvids(invidVector);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for an object
   * @param type Object type id number
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Invid findLabeledObject(String objectName, short type) throws NotLoggedInException
  {
    checklogin();

    return this.findLabeledObject(objectName, type, false);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for the object to lookup
   * @param objectType Name of the object type
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Invid findLabeledObject(String objectName, String objectType) throws NotLoggedInException
  {
    checklogin();

    return this.findLabeledObject(objectName, objectType, false);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for an object
   * @param type Object type id number.
   * @param allowAliases If true, findLabeledObject will return an
   * Invid that has objectName attached to the same namespace as the
   * label field for the object type sought, even if the Invid is of a
   * different object type.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized Invid findLabeledObject(String objectName, short type, boolean allowAliases) throws NotLoggedInException
  {
    checklogin();

    return queryEngine.findLabeledObject(objectName, type, allowAliases);
  }

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * <p>This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.</p>
   *
   * @param objectName Label for the object to lookup
   * @param objectType Name of the object type
   * @param allowAliases If true, findLabeledObject will return an
   * Invid that has name attached to the same namespace as the label
   * field for the object type sought.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Invid findLabeledObject(String objectName, String objectType, boolean allowAliases) throws NotLoggedInException
  {
    checklogin();

    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
	// "Error, "{0}" is not a valid object type in this Ganymede server."
	throw new RuntimeException(ts.l("global.no_such_object_type", objectType));
      }

    return this.findLabeledObject(objectName, base.getTypeID(), allowAliases);
  }

  /**
   * <p>This method provides the hook for doing all manner of simple
   * object listing for the Ganymede database.</p>
   *
   * <p>This version of query() takes a query in string form, based on
   * Deepak's ANTLR-specified Ganymede query grammar.</p>
   *
   * <p>This method may not be called from a DBEditObject's
   * commitPhase1/2() methods without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized QueryResult query(String queryString) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    return queryEngine.query(queryString);
  }

  /**
   * <p>This method provides the hook for doing all
   * manner of simple object listing for the Ganymede
   * database.</p>
   *
   * <p>This method may not be called from a DBEditObject's
   * commitPhase1/2() methods without risking deadlock.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized QueryResult query(Query query) throws NotLoggedInException
  {
    checklogin();

    return queryEngine.query(query);
  }

  /**
   * <p>This method is intended as a lightweight way of returning the
   * current label of the specified invid.  No locking is done,
   * and the label returned will be viewed through the context
   * of the current transaction, if any.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public String viewObjectLabel(Invid invid) throws NotLoggedInException
  {
    checklogin();

    // We don't check permissions here, as we use session.viewDBObject().

    // We have made the command decision that finding the label for an
    // invid is not something we need to guard against.  Using
    // session.viewDBObject() here makes this a much more lightweight
    // operation.

    return dbSession.getObjectLabel(invid);
  }

  /**
   * <p>This method returns a multi-line string containing excerpts
   * from the Ganymede log relating to &lt;invid&gt;, since time
   * &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this time, or all events if this is null.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since) throws NotLoggedInException
  {
    checklogin();

    return viewObjectHistory(invid, since, null, true);
  }

  /**
   * <p>This method returns a multi-line string containing excerpts
   * from the Ganymede log relating to &lt;invid&gt;, since time
   * &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this time, or all events if this is null.
   * @param fullTransactions If false, only events directly involving the requested
   * object will be included in the result buffer.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since, boolean fullTransactions) throws NotLoggedInException
  {
    checklogin();

    return viewObjectHistory(invid, since, null, fullTransactions);
  }

  /**
   * <p>This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this time, or all events if this is null.
   * @param before Report events occuring on or before this time
   * @param fullTransactions If false, only events directly involving the requested
   * object will be included in the result buffer.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since, Date before, boolean fullTransactions) throws NotLoggedInException
  {
    checklogin();

    if (invid == null)
      {
        // "Null invid passed into viewObjectHistory"
	setLastError(ts.l("viewObjectHistory.null_invid"));
	return null;
      }

    // If we're not supergash, we'll need to get a reference to the
    // object so that we can check for view permission before
    // returning the history

    // Note that this means that in the case where the client has kept
    // a reference to an invid that has been deleted, we won't be able
    // to verify the user's permissions to view the history, so we'll
    // have to return null

    DBObject obj = dbSession.viewDBObject(invid);

    if (obj == null)
      {
        // "Can''t return history for an object that has been deleted or does not exist ({0})"
        setLastError(ts.l("viewObjectHistory.null_pointer", String.valueOf(invid)));
        return null;
      }

    if (!permManager.getPerm(obj).isVisible())
      {
        // "Permissions denied to view the history for this invid."
        setLastError(ts.l("viewObjectHistory.permissions"));
        return null;
      }

    if (Ganymede.log == null)
      {
        // "Log not active, can''t view invid history"
        setLastError(ts.l("viewObjectHistory.no_log"));
        return null;
      }

    // don't bother looking for anything in the log before the object
    // was created

    Date creationDate = (Date) obj.getFieldValueLocal(SchemaConstants.CreationDateField);

    if (since == null || since.before(creationDate))
      {
        since = creationDate;
      }

    Date lastModDate = (Date) obj.getFieldValueLocal(SchemaConstants.ModificationDateField);

    if (before == null || before.after(lastModDate))
      {
        // earlier versions of Ganymede server would apply a slightly
        // (milliseconds) earlier modification date in the data
        // objects than the correponding log entry, so we're going to
        // extend the lastModDate by ten seconds so that we'll be sure
        // to catch the later time stamps in the log file.

        before = new Date(lastModDate.getTime() + 10000);
      }

    return Ganymede.log.retrieveHistory(invid, since, before, false, fullTransactions, false); // *sync* DBLog
  }

  /**
   * <p>This method returns a multi-line string containing excerpts
   * from the Ganymede log relating to &lt;invid&gt;, since time
   * &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the admin Persona whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @return A String containing a record of events for the Invid in question,
   * or null if permissions are denied to view the history.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer viewAdminHistory(Invid invid, Date since) throws NotLoggedInException
  {
    checklogin();

    if (invid == null)
      {
	setLastError(ts.l("viewAdminHistory.null_invid"));
	return null;
      }

    if (invid.getType() != SchemaConstants.PersonaBase)
      {
	setLastError(ts.l("viewAdminHistory.wrong_invid"));
	return null;
      }

    // we do our own permissions checking, so we can use dbSession.viewDBObject().

    DBObject obj = dbSession.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException(ts.l("viewAdminHistory.null_pointer", String.valueOf(invid)));
      }

    if (!permManager.getPerm(obj).isVisible())
      {
	setLastError(ts.l("viewObjectHistory.permissions"));
	return null;
      }

    if (Ganymede.log == null)
      {
	setLastError(ts.l("viewObjectHistory.no_log"));
	return null;
      }

    // don't bother looking for anything in the log before the admin
    // persona was created

    Date creationDate = (Date) obj.getFieldValueLocal(SchemaConstants.CreationDateField);

    if (since == null || since.before(creationDate))
      {
        since = creationDate;
      }

    return Ganymede.log.retrieveHistory(invid, since, null, true, true, false); // *sync* DBLog
  }

  /**
   * <p>View an object from the database.  The ReturnVal returned will
   * carry a {@link arlut.csd.ganymede.rmi.db_object db_object} reference,
   * which can be obtained by the client
   * calling {@link arlut.csd.ganymede.common.ReturnVal#getObject() ReturnVal.getObject()}.
   * If the object could not be
   * viewed for some reason, the ReturnVal will carry an encoded error
   * dialog for the client to display.</p>
   *
   * <p>view_db_object() can be done at any time, outside of the
   * bounds of any transaction.  view_db_object() returns a read-only
   * snapshot of the object's state at the time the view_db_object()
   * call is processed, and will be transaction-consistent
   * internally.</p>
   *
   * <p>If view_db_object() is called during a transaction, the object
   * will be returned as it stands during the transaction.. that is,
   * if the object has been changed during the transaction, a
   * read-only view of that changed object will be returned, even if
   * the transaction has not yet been committed, and other clients
   * would not be able to see that version of the object.</p>
   *
   * <p>NOTE: It is critical that any code that looks at the values of
   * fields in a {@link arlut.csd.ganymede.server.DBObject DBObject}
   * go through a view_db_object() method
   * or else the object will not properly know who owns it, which
   * is critical for it to be able to properly authenticate field
   * access.  Keep in mind, however, that view_db_object clones the
   * DBObject in question, so this method is very heavyweight.</p>
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal view_db_object(Invid invid) throws NotLoggedInException
  {
    checklogin();

    // we'll let a NullPointerException be thrown if we were given a null
    // Invid.

    DBObject obj = dbSession.viewDBObject(invid);

    if (obj == null)
      {
	// "Object Not Found"
	// "Could not find object {0} in the database.  Perhaps the object does not exist?"

	return Ganymede.createErrorDialog(ts.l("view_db_object.no_object_error"),
					  ts.l("view_db_object.no_object_error_text", String.valueOf(invid)));
      }

    if (permManager.getPerm(obj).isVisible())
      {
	if (!obj.isEmbedded())
	  {
	    setLastEvent("view " + obj.getTypeName() + ":" + obj.getLabel());
	  }

	// return a copy that knows what GanymedeSession is
	// looking at it so that it can do per-field visibility
	// checks.

	// copying the object also guarantees that if the
	// DBSession has checked out the object for editing
	// (possibly in a way that the user wouldn't normally have
	// permission to do, as in anonymous invid
	// linking/unlinking), that the client won't be able to
	// directly manipulate the DBEditObject in the transaction
	// to get around permission enforcement.

	db_object objref = new DBObject(obj, this);

	ReturnVal result = new ReturnVal(true); // success

	result.setInvid(((DBObject) objref).getInvid());

	if (this.exportObjects)
	  {
	    exportObject((DBObject) objref);
	  }

	result.setObject(objref);

	return result;
      }
    else
      {
	return Ganymede.createErrorDialog(ts.l("global.permissions_error"),
					  ts.l("view_db_object.permissions_error_text", viewObjectLabel(invid), String.valueOf(invid)));
      }
  }

  /**
   * <p>Check an object out from the database for editing.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling
   * {@link arlut.csd.ganymede.common.ReturnVal#getObject() ReturnVal.getObject()}.
   * If the object could not be checked out for editing for some
   * reason, the ReturnVal will carry an encoded error dialog for the
   * client to display.</p>
   *
   * <p>Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once checked out, the object will be unavailable
   * to any other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}
   * or {@link arlut.csd.ganymede.server.GanymedeSession#abortTransaction() abortTransaction()}.</p>
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal edit_db_object(Invid invid) throws NotLoggedInException
  {
    checklogin();

    DBObject obj = dbSession.viewDBObject(invid);

    if (obj == null)
      {
	// "Object Not Found"
	// "Error, object [{0}] does not appear to exist.  Couldn't edit it."
	return Ganymede.createErrorDialog(ts.l("view_db_object.no_object_error"),
					  ts.l("edit_db_object.no_object_error_text", String.valueOf(invid)));
      }

    // we always want to check permissions, even if the object has
    // already been checked out by our DBSession.. some of the
    // InvidDBField Invid binding logic can check out the object for
    // editing in a way that the user's permissions would not normally
    // allow.  By checking perms up front, we may be preventing the
    // user from getting access to an object which would not normally
    // be editable.  If we didn't do this check and the object had
    // already been checked out by InvidDBField.bind() or the like,
    // the object should still refuse edits due to the field-level
    // permissions, but better to play things strictly by the book,
    // here.

    if (permManager.getPerm(obj).isEditable())
      {
	if (!obj.isEmbedded())
	  {
	    setLastEvent("edit " + obj.getTypeName() + ":" + obj.getLabel());
	  }

	db_object objref = null;

	try
          {
            objref = dbSession.editDBObject(invid);
          }
        catch (GanymedeManagementException ex)
          {
	    // "Error checking object out for editing"
	    // "Error loading custom class for this object."
            return Ganymede.createErrorDialog(ts.l("edit_db_object.checking_out_error"),
                                              ts.l("edit_db_object.custom_class_load_error_text"));
          }

	if (objref != null)
	  {
	    ReturnVal result = new ReturnVal(true);
	    result.setInvid(((DBObject) objref).getInvid());

	    if (this.exportObjects)
	      {
		exportObject((DBObject) objref);
	      }

	    result.setObject(objref);
	    return result;
	  }
	else
	  {
	    // someone else is editing it.. who?

	    String edit_username;
	    String edit_hostname;

	    DBEditObject editing = obj.shadowObject;

	    if (editing != null)
	      {
		if (editing.gSession != null)
		  {
		    edit_username = editing.gSession.getPermManager().getUserName();
		    edit_hostname = editing.gSession.getClientHostName();

		    // "Error, object already being edited"
		    // "{0} [{1} - {2}] is already being edited by user {3} on host {4}"
		    return Ganymede.createErrorDialog(ts.l("edit_db_object.already_editing"),
						      ts.l("edit_db_object.already_editing_text",
							   obj.getTypeName(),
							   viewObjectLabel(invid),
							   String.valueOf(invid),
							   edit_username,
							   edit_hostname));
		  }
	      }

	    // "Error checking object out for editing"
	    // "Error checking out {0} [{1} - {2}] for editing.\nPerhaps someone else was editing it?"
	    return Ganymede.createErrorDialog(ts.l("edit_db_object.checking_out_error"),
					      ts.l("edit_db_object.checking_out_error_text",
						   obj.getTypeName(),
						   viewObjectLabel(invid),
						   String.valueOf(invid)));
	  }
      }
    else
      {
	// "Permissions Error"
	// "Permission to edit {0} [{1} - {2}] denied."
	return Ganymede.createErrorDialog(ts.l("global.permissions_error"),
					  ts.l("edit_db_object.permissions_error_text",
					       obj.getTypeName(),
					       viewObjectLabel(invid),
					       String.valueOf(invid)));
      }
  }

  /**
   * <p>Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.</p>
   *
   * <p>Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * @param type The kind of object to create.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal create_db_object(short type) throws NotLoggedInException
  {
    checklogin();

    return this.create_db_object(type, false, null);
  }

  /**
   * <p>Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.</p>
   *
   * <p>Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * @param objectType The kind of object to create.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal create_db_object(String objectType) throws NotLoggedInException
  {
    checklogin();

    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
	// "Error, "{0}" is not a valid object type in this Ganymede server."
	return Ganymede.createErrorDialog(ts.l("global.no_such_object_type", objectType));
      }

    return this.create_db_object(base.getTypeID());
  }

  /**
   * <p>Clone a new object from object &lt;invid&gt;.  The ReturnVal returned
   * will carry a db_object reference, which can be obtained by the
   * client calling ReturnVal.getObject().  If the object could not
   * be checked out for editing for some reason, the ReturnVal will
   * carry an encoded error dialog for the client to display.</p>
   *
   * <p>This method must be called within a transactional context.</p>
   *
   * <p>Typically, only certain values will be cloned.  What values are
   * retained is up to the specific code module provided for the
   * invid type of object.</p>
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal clone_db_object(Invid invid) throws NotLoggedInException
  {
    checklogin();

    boolean checkpointed = false;

    if (invid == null)
      {
	return Ganymede.createErrorDialog(ts.l("clone_db_object.clone_error"),
					  ts.l("clone_db_object.clone_error_text"));
      }

    ReturnVal retVal = view_db_object(invid); // get a copy customized for per-field visibility

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    DBObject vObj = (DBObject) retVal.getObject();

    DBEditObject objectHook = Ganymede.db.getObjectBase(invid.getType()).getObjectHook();

    if (!objectHook.canClone(dbSession, vObj))
      {
	// "Cloning DENIED"
	// "Cloning operation refused for {0} object {1}."
	return Ganymede.createErrorDialog(ts.l("clone_db_object.denied"),
					  ts.l("clone_db_object.denied_msg", vObj.getTypeName(), vObj.getLabel()));
      }

    String ckp = RandomUtils.getSaltedString("clone_db_object[" + invid.toString() + "]");

    dbSession.checkpoint(ckp);
    checkpointed = true;

    try
      {
        retVal = create_db_object(invid.getType());

        if (!ReturnVal.didSucceed(retVal))
          {
            return retVal;
          }

        DBEditObject newObj = (DBEditObject) retVal.getObject();

        // the merge operation will do the right thing here and
        // preserve the encoded object and invid in the retVal for our
        // pass-through to the client, so long as the cloneFromObject
        // method succeeds.

        retVal = ReturnVal.merge(retVal, newObj.cloneFromObject(dbSession, vObj, false));

        if (ReturnVal.didSucceed(retVal))
          {
            dbSession.popCheckpoint(ckp);
            checkpointed = false;
          }

        return retVal;
      }
    finally
      {
        if (checkpointed)
          {
            dbSession.rollback(ckp);
          }
      }
  }

  /**
   * <p>Inactivate an object in the database</p>
   *
   * <p>This method must be called within a transactional context.  The object's
   * change in status will not be visible to other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * <p>Objects inactivated will typically be altered to reflect their inactive
   * status, but the object itself might not be purged from the Ganymede
   * server for a defined period of time, to allow other network systems
   * to have time to do accounting, clean up, etc., before a user id or
   * network address is re-used.</p>
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal inactivate_db_object(Invid invid) throws NotLoggedInException
  {
    checklogin();

    DBObject vObj = dbSession.viewDBObject(invid);

    if (vObj == null)
      {
	setLastError(ts.l("inactivate_db_object.error_text"));

	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.error_text"));
      }

    if (vObj.isInactivated())
      {
	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.already_inactivated"),
					  ts.l("inactivate_db_object.already_inactivated_text",
					       vObj.getTypeName(),
					       vObj.getLabel()));
      }

    if (!permManager.getPerm(vObj).isDeletable())
      {
	setLastError(ts.l("inactivate_db_object.permission_text",
			  vObj.getTypeName(),
			  vObj.getLabel()));

	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.permission_text",
					       vObj.getTypeName(), vObj.getLabel()));
      }

    ReturnVal result = edit_db_object(invid); // *sync* DBSession DBObject

    DBEditObject eObj = (DBEditObject) result.getObject();

    if (eObj == null)
      {
	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.no_checkout",
					       vObj.getTypeName(), vObj.getLabel()));
      }

    if (!eObj.canBeInactivated() || !eObj.canInactivate(dbSession, eObj))
      {
	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.not_inactivatable", eObj.getLabel()));
      }

    setLastEvent("inactivate " + eObj.getTypeName() + ":" + eObj.getLabel());

    // note!  DBEditObject's finalizeInactivate() method does the
    // event logging

    return dbSession.inactivateDBObject(eObj); // *sync* DBSession
  }

  /**
   * <p>Reactivates an inactivated object in the database</p>
   *
   * <p>This method is only applicable to inactivated objects.  For such,
   * the object will be reactivated if possible, and the removal date
   * will be cleared.  The object may retain an expiration date,
   * however.</p>
   *
   * <p>This method must be called within a transactional context.  The object's
   * change in status will not be visible to other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal reactivate_db_object(Invid invid) throws NotLoggedInException
  {
    checklogin();

    DBObject vObj = dbSession.viewDBObject(invid);

    if (vObj == null)
      {
	return Ganymede.createErrorDialog(ts.l("reactivate_db_object.error"),
					  ts.l("reactivate_db_object.no_such"));
      }

    if (!vObj.isInactivated())
      {
	return Ganymede.createErrorDialog(ts.l("reactivate_db_object.error"),
					  ts.l("reactivate_db_object.not_inactivated",
					       vObj.getTypeName(),
					       vObj.getLabel()));
      }

    // we'll treat the object's deletion bit as the power-over-life-and-death bit

    if (!permManager.getPerm(vObj).isDeletable())
      {
	return Ganymede.createErrorDialog(ts.l("reactivate_db_object.error"),
					  ts.l("reactivate_db_object.permission_text",
					       vObj.getTypeName(),
					       vObj.getLabel()));
      }

    ReturnVal result = edit_db_object(invid); // *sync* DBSession DBObject

    DBEditObject eObj = (DBEditObject) result.getObject();

    if (eObj == null)
      {
	return Ganymede.createErrorDialog(ts.l("reactivate_db_object.error"),
					  ts.l("reactivate_db_object.no_checkout",
					       vObj.getTypeName(),
					       vObj.getLabel()));
      }

    setLastEvent("reactivate " + eObj.getTypeName() + ":" + eObj.getLabel());

    // note!  DBEditObject's finalizeReactivate() method does the
    // event logging at transaction commit time

    return dbSession.reactivateDBObject(eObj); // *sync* DBSession, DBObject possible
  }

  /**
   * <p>Remove an object from the database</p>
   *
   * <p>This method must be called within a transactional context.</p>
   *
   * <p>Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   * Any problems with permissions to remove this object will result
   * in a dialog being returned in the ReturnVal.</p>
   *
   * <p>This method must be called within a transactional context.  The object's
   * removal will not be visible to other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal remove_db_object(Invid invid) throws NotLoggedInException
  {
    checklogin();

    if (debug)
      {
	Ganymede.debug("Attempting to delete object: " + invid);
      }

    if ((invid.getType() == SchemaConstants.RoleBase) &&
	(invid.getNum() == SchemaConstants.RoleDefaultObj))
      {
	return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					  ts.l("remove_db_object.badobj1"));
      }

    if ((invid.getType() == SchemaConstants.PersonaBase) &&
	(invid.getNum() == SchemaConstants.PersonaSupergashObj))
      {
	return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					  ts.l("remove_db_object.badobj2", Ganymede.rootname));
      }

    if ((invid.getType() == SchemaConstants.OwnerBase) &&
	(invid.getNum() == SchemaConstants.OwnerSupergash))
      {
	return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					  ts.l("remove_db_object.badobj3"));
      }

    DBObjectBase objBase = Ganymede.db.getObjectBase(invid.getType());
    DBObject vObj = dbSession.viewDBObject(invid);

    if (vObj == null)
      {
	return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					  ts.l("remove_db_object.no_such"));
      }

    // if the object is newly created or is already marked for
    // deletion, we'll assume the user can go ahead and delete/drop
    // it.

    boolean okToRemove = ((vObj instanceof DBEditObject) &&
			  ((DBEditObject) vObj).getStatus() != ObjectStatus.EDITING);

    if (!okToRemove)
      {
	if (!permManager.getPerm(vObj).isDeletable())
	  {
	    return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					      ts.l("remove_db_object.permission_text",
						   vObj.getTypeName(),
						   vObj.getLabel()));
	  }

	ReturnVal retVal = objBase.getObjectHook().canRemove(dbSession, vObj);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (retVal.getDialog() != null)
	      {
		return retVal;
	      }

	    // if an object type can be inactivated, then it *must* be
	    // inactivated, unless the user is supergash

	    if (!permManager.isSuperGash() && objBase.getObjectHook().canBeInactivated())
	      {
		return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
						  ts.l("remove_db_object.must_inactivate",
						       vObj.getTypeName(),
						       vObj.getLabel()));
	      }

	    // otherwise, generic refusal

	    return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					      ts.l("remove_db_object.deletion_refused",
						   vObj.getTypeName(),
						   vObj.getLabel()));
	  }
      }

    setLastEvent("delete " + vObj.getTypeName() + ":" + vObj.getLabel());

    // we do logging of the object deletion in DBEditSet.commit() when
    // the transaction commits

    return dbSession.deleteDBObject(invid);
  }

  /**
   * <p>This method is called by the XML client to initiate a dump of
   * Ganymede objects in XML format matching the GanyQL search
   * criteria specified in the queryString.  The ReturnVal returned
   * will, if the operation is approved, contain a reference to an RMI
   * FileTransmitter interface, which can be iteratively called by the
   * XML client to pull pieces of the transmission down in
   * sequence.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal runXMLQuery(String queryString) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query query = transmuter.transmuteQueryString(queryString);

    // get a simple list of matching invids without bothering to do
    // transport setup.

    QueryResult rows = queryEngine.queryDispatch(query, false, false, null, null);

    XMLTransmitter transmitter = null;

    try
      {
	transmitter = new XMLTransmitter(this, query, rows);
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog(ts.l("runXMLQuery.transmitter_error"),
					  ts.l("runXMLQuery.transmitter_error_msg", ex.getMessage()));
      }

    ReturnVal retVal = new ReturnVal(true);
    retVal.setFileTransmitter(transmitter);
    return retVal;
  }

  /**
   * <p>This method is called by the XML client to initiate a dump of
   * Ganymede objects in XML format matching the search criteria
   * specified in the query object.  The ReturnVal returned will, if
   * the operation is approved, contain a reference to an RMI
   * FileTransmitter interface, which can be iteratively called by the
   * XML client to pull pieces of the transmission down in
   * sequence.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal runXMLQuery(Query query) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    // get a simple list of matching invids without bothering to do
    // transport setup.

    QueryResult rows = queryEngine.queryDispatch(query, false, false, null, null);

    XMLTransmitter transmitter = null;

    try
      {
	transmitter = new XMLTransmitter(this, query, rows);
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog(ts.l("runXMLQuery.transmitter_error"),
					  ts.l("runXMLQuery.transmitter_error_msg", ex.getMessage()));
      }

    ReturnVal retVal = new ReturnVal(true);
    retVal.setFileTransmitter(transmitter);
    return retVal;
  }

  /**
   * <p>This method is called by the XML client to initiate a dump of
   * the server's schema definition in XML format.  The ReturnVal
   * returned will, if the operation is approved, contain a reference
   * to an RMI FileTransmitter interface, which can be iteratively
   * called by the XML client to pull pieces of the transmission down
   * in sequence.</p>
   *
   * <p>This method is only available to a supergash-privileged
   * GanymedeSession.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal getSchemaXML() throws NotLoggedInException
  {
    checklogin();

    return this.getXML(false, true, null, false, false);
  }

  /**
   * <p>This method is called by the XML client to initiate a dump of
   * the entire data contents of the server.  The ReturnVal returned
   * will, if the operation is approved, contain a reference to
   * an RMI FileTransmitter interface, which can be iteratively called
   * by the XML client to pull pieces of the transmission down in
   * sequence.</p>
   *
   * <p>This method is only available to a supergash-privileged
   * GanymedeSession.</p>
   *
   * @param syncChannel The name of the sync channel whose constraints
   * we want to apply to this dump.  May be null if the client wants
   * an unfiltered dump.
   * @param includeHistory If true, the historical fields (creation
   * date & info, last modification date & info) will be included in
   * the xml stream.
   * @param includeOid If true, the objects written out to the xml
   * stream will include an "oid" attribute which contains the precise
   * Invid of the object.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal getDataXML(String syncChannel, boolean includeHistory, boolean includeOid) throws NotLoggedInException
  {
    checklogin();

    Ganymede.debug("GanymedeSession.getDataXML(" + syncChannel + ")");
    return this.getXML(true, false, syncChannel, includeHistory, includeOid);
  }

  /**
   * <p>This method is called by the XML client to initiate a dump of
   * the server's entire database, schema and data, in XML format.
   * The ReturnVal will, if the operation is approved, contain a
   * reference to an RMI FileTransmitter interface, which can be
   * iteratively called by the XML client to pull pieces of the
   * transmission down in sequence.</p>
   *
   * <p>This method is only available to a supergash-privileged
   * GanymedeSession.</p>
   *
   * @param includeHistory If true, the historical fields (creation
   * date & info, last modification date & info) will be included in
   * the xml stream.
   * @param includeOid If true, the objects written out to the xml
   * stream will include an "oid" attribute which contains the precise
   * Invid of the object.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal getXMLDump(boolean includeHistory, boolean includeOid) throws NotLoggedInException
  {
    checklogin();

    return this.getXML(true, true, null, includeHistory, includeOid);
  }

  //
  //************************************************************
  //
  //
  //
  // From here on down, the methods are not remotely accessible to the client,
  // but are instead for server-side use only.
  //
  //
  //
  //************************************************************

  /**
   * <p>This private method is called by all methods in
   * GanymedeSession that require the client to be logged in to
   * operate.</p>
   *
   * <p>NB: the {@link arlut.csd.ganymede.server.logout()} method
   * unexports this GanymedeSession object so that we shouldn't have
   * any more incoming RMI calls to worry about, but it's possible for
   * RMI calls to be in flight and blocking on the GanymedeSession
   * object monitor, so we do checklogin() as an additional protection
   * in case of that kind of a race at logout time.</p>
   *
   * <p>checklogin() is also useful to make sure that server-side code
   * doesn't try to use a GanymedeSession locally after the logout()
   * method has been called.</p>
   */

  public void checklogin() throws NotLoggedInException
  {
    if (!isLoggedIn())
      {
	throw new NotLoggedInException();
      }

    synchronized (lastActionTime)
      {
        lastActionTime.setTime(System.currentTimeMillis());
      }
  }

  public boolean isLoggedIn()
  {
    return loggedInSemaphore.isSet();
  }

  /**
   * <p>Convenience method to get access to this session's UserBase
   * instance.</p>
   */

  DBObject getUser()
  {
    return permManager.getUser();
  }

  /**
   * <p>This method returns the identification string that the server
   * has assigned to the user.</p>
   *
   * <p>Note: a server-side version of getMyUserName() that doesn't
   * call checklogin().</p>
   */

  public String getUserName()
  {
    return permManager.getUserName();
  }

  /**
   * <p>This method returns the name of this session.  If this session is
   * being driven by a remote client, this name will be unique among
   * sessions on the server so that the admin console can distinguish
   * among them.</p>
   */

  public String getSessionName()
  {
    return permManager.getSessionName();
  }

  /**
   * This method returns the name of the system that the client
   * is connected from.
   */

  public String getClientHostName()
  {
    return this.clienthost;
  }

  public boolean isXMLSession()
  {
    return xSession != null;
  }

  public GanymedeXMLSession getXSession()
  {
    return xSession;
  }

  public void setXSession(GanymedeXMLSession xSession)
  {
    this.xSession = xSession;
  }

  /**
   * <p>This method returns a reference to the {@link
   * arlut.csd.ganymede.server.DBSession DBSession} object
   * encapsulated by this GanymedeSession object.  This is intended to
   * be used by server-side code that wants to carry out certain
   * operations without involving permissions checking.</p>
   */

  public DBSession getDBSession()
  {
    return dbSession;
  }

  /**
   * <p>This method returns a reference to the {@link
   * arlut.csd.ganymede.server.DBSession DBSession} object
   * encapsulated by this GanymedeSession object.  This is intended to
   * be used by server-side code that wants to carry out certain
   * operations without involving permissions checking.</p>
   *
   * <p>Note: getSession() is the original name of the method
   * getDBSession(), and should be considered deprecated for new
   * code.</p>
   */

  public DBSession getSession()
  {
    return dbSession;
  }

  /**
   * <p>This method returns a reference to the {@link
   * arlut.csd.ganymede.server.DBPermissionManager
   * DBPermissionManager} object encapsulated by this GanymedeSession
   * object.  This is intended to be used by subclasses of {@link
   * arlut.csd.ganymede.server.DBEditObject DBEditObject} that might
   * not necessarily be in the arlut.csd.ganymede package.</p>
   */

  public DBPermissionManager getPermManager()
  {
    return permManager;
  }

  /**
   * <p>This method is used to allow local server-side code to request
   * that no oversight be maintained over changes made to the server
   * through this GanymedeSession.</p>
   *
   * <p>This is intended <b>only</b> for trusted code that does its own
   * checking and validation on changes made to the database.  If
   * oversight is turned off, no wizard code will be called, and the
   * required field logic will be bypassed.  Extreme care must
   * be used in disabling oversight, and oversight should only be
   * turned off for direct loading and other situations where there
   * won't be multi-user use, to avoid breaking constraints that
   * custom plug-ins count on.</p>
   *
   * <p>Oversight is enabled by default.</p>
   *
   * <p>Generally this is only used for doing bulk loads via XML or
   * the like.</p>
   *
   * @param val If true, oversight will be enabled.
   */

  public void enableOversight(boolean val)
  {
    this.enableOversight = val;
  }

  /**
   * <p>This method finds the ultimate owner of an embedded object</p>
   */

  DBObject getContainingObj(DBObject object)
  {
    return dbSession.getContainingObj(object);
  }

  /**
   * <p>Log out this session.  After this method is called, no other
   * methods may be called on this session object.</p>
   *
   * <p>This method is partially synchronized, to avoid locking up
   * the admin console if this user's session has become deadlocked.</p>
   */

  public void logout(boolean forced_off)
  {
    // This method is not synchronized to help stave off threads
    // piling up trying to kill off a user which is deadlocked.

    // Obviously we want to prevent the deadlock in the first place,
    // but this will help keep hapless admins on the console from
    // locking their console trying to kill deadlocked users.

    if (!loggedInSemaphore.set(false))
      {
	return;
      }

    // we do want to synchronize on our session for this part, so that
    // the user won't be logged off while they are doing a transaction
    // commit, or the like.

    synchronized (this)
      {
	// we'll do all of our active cleanup in a try clause, so we
	// can wipe out references to aid GC in a finally clause

	try
	  {
	    if (!remoteClient)
	      {
		// We don't need to update GanymedeServer's lists for internal sessions

		dbSession.logout();	// *sync* DBSession
		return;
	      }

	    //	Ganymede.debug("User " + username + " logging off");

	    if (this.asyncPort != null)
	      {
		this.asyncPort.shutdown();
		this.asyncPort = null;
	      }

	    // logout the client, abort any DBSession transaction going

	    dbSession.logout();	// *sync* DBSession

	    // if we have DBObjects left exported through RMI, make
	    // them inaccesible

	    unexportObjects(true);

	    // if we ourselves were exported, unexport

	    if (remoteClient)
	      {
		Ganymede.rmi.unpublishObject(this, true);

		// from this point on, RMI remote calls to this
		// GanymedeSession should fail, but we still have
		// everything guarded by checklogin() in case there
		// are incoming RMI calls blocking on the
		// GanymedeSession object monitor.
	      }

	    // if we weren't forced off, do normal logout logging

	    if (!forced_off)
	      {
		if (Ganymede.log != null)
		  {
		    Ganymede.log.logSystemEvent(new DBLogEvent("normallogout",
							       ts.l("logout.normal_event", permManager.getUserName()),
							       permManager.getUserInvid(),
							       permManager.getUserName(),
							       permManager.getIdentityInvids(),
							       null));
		  }
	      }
	    else
	      {
		// if we are forced off, and we're running under a
		// GanymedeXMLSession, tell the GanymedeXMLSession to
		// kick off

		// if we're not forced off, then presumably the
		// GanymedeXMLSession triggered the logout.

		if (xSession != null)
		  {
		    xSession.abort();
		  }
	      }

	    // Update the server's records, refresh the admin consoles.

	    GanymedeServer.removeRemoteUser(this);

	    // update the admin consoles

	    GanymedeAdmin.refreshUsers();
	  }
	finally
	  {
	    if (semaphoreLocked)
	      {
		try
		  {
		    GanymedeServer.lSemaphore.decrement();
		  }
		catch (IllegalArgumentException ex)
		  {
		    Ganymede.logError(ex);
		  }

		semaphoreLocked = false;
	      }

	    // let go of our session name and let the server know that
	    // it can shut the server down if it is deferred shutdown
	    // mode and we were a user session.

	    GanymedeServer.clearSession(this);
	  }

	// guess we're still running.  Remember the last time this
	// user logged out for the motd-display check

	Ganymede.debug(ts.l("logout.logged_off", permManager.getUserName()));

	permManager = null;
      }
  }

  /**
   * <p>This method applies this GanymedeSession's current owner filter
   * to the given QueryResult &lt;qr&gt; and returns a QueryResult
   * with any object handles that are not matched by the filter
   * stripped.</p>
   *
   * <p>If the submitted QueryResult &lt;qr&gt; is null, filterQueryResult()
   * will itself return null.</p>
   */

  public QueryResult filterQueryResult(QueryResult qr)
  {
    return permManager.filterQueryResult(qr);
  }

  /**
   * <p>Server-side method for doing object listing with support for
   * DBObject's {@link
   * arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject)
   * lookupLabel} method.</p>
   *
   * @param query The query to be performed
   * @param perspectiveObject There are occasions when the server will want to do internal
   * querying in which the label of an object matching the query criteria is synthesized
   * for use in a particular context.  If non-null, perspectiveObject's
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method will be used to generate the label for a result entry.
   */

  public synchronized QueryResult query(Query query, DBEditObject perspectiveObject) throws NotLoggedInException
  {
    checklogin();

    return queryEngine.query(query, perspectiveObject);
  }

  /**
   * <p>This method provides the hook for doing all manner of internal
   * object listing for the Ganymede database.  This method will not
   * take into account any optional owner filtering, but it will honor
   * the editableOnly flag in the Query.</p>
   *
   * @return A Vector of {@link arlut.csd.ganymede.common.Result Result} objects
   */

  public synchronized Vector<Result> internalQuery(Query query)
  {
    return queryEngine.internalQuery(query);
  }

  /**
   * <p>This method is intended as a lightweight way of returning a
   * handy description of the type and label of the specified invid.
   * No locking is done, and the label returned will be viewed through
   * the context of the current transaction, if any.</p>
   */

  public String describe(Invid invid)
  {
    // We don't check permissions here.
    //
    // We have made the command decision that finding the label for an
    // invid is not something we need to guard against.  Using
    // session.viewDBObject() here makes this a much more lightweight
    // operation.

    return dbSession.describe(invid);
  }

  /**
   * <p>Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.</p>
   *
   * <p>Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * @param type The kind of object to create.
   * @param embedded If true, assume the object created is embedded and
   * does not need to have owners set.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  public ReturnVal create_db_object(short type, boolean embedded) throws NotLoggedInException
  {
    return this.create_db_object(type, embedded, null);
  }

  /**
   * <p>Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.</p>
   *
   * <p>Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.</p>
   *
   * @param type The kind of object to create.
   * @param embedded If true, assume the object created is embedded and
   * does not need to have owners set.
   * @param preferredInvid If not null, the created object will be
   * given the preferredInvid.  This is only expected to be used by
   * the GanymedeXMLSession if the -magic_import command line flag was
   * given on the Ganymede server's startup.  If preferredInvid is
   * null, the Invid id number will be automatically picked in
   * monotonically increasing fashion.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  public synchronized ReturnVal create_db_object(short type, boolean embedded, Invid preferredInvid) throws NotLoggedInException
  {
    DBObject newObj;
    ReturnVal retVal = null;
    Vector ownerInvids = null;

    /* -- */

    checklogin();

    if (!permManager.getPerm(type, true).isCreatable())
      {
	DBObjectBase base = Ganymede.db.getObjectBase(type);

	String error;

	if (base == null)
	  {
	    // "Permission to create object of *invalid* type {0} denied."
	    error = ts.l("create_db_object.invalid_type", Integer.valueOf(type));
	  }
	else
	  {
	    // "Permission to create object of type {0} denied."
	    error = ts.l("create_db_object.type_no_perm", base.getName());
	  }

	// "Can''t Create Object"
	return Ganymede.createErrorDialog(ts.l("create_db_object.cant_create"), error);
      }

    // if embedded is true, this code was called from
    // DBEditObject.createNewEmbeddedObject(), and we don't need to
    // worry about setting ownership, etc.

    if (!embedded)
      {
	ownerInvids = permManager.getNewOwnerInvids();

	if (!permManager.isSuperGash() && ownerInvids.size() == 0)
	  {
	    // "Can''t Create Object"
	    // "Can''t create new object, no owner group to put it in."
	    return Ganymede.createErrorDialog(ts.l("create_db_object.cant_create"),
					      ts.l("create_db_object.no_owner_group"));
	  }
      }

    // now create and process

    try
      {
        retVal = dbSession.createDBObject(type, preferredInvid, ownerInvids); // *sync* DBSession
      }
    catch (GanymedeManagementException ex)
      {
        // "Can''t Create Object"
        // "Error loading custom class for this object."
        return Ganymede.createErrorDialog(ts.l("create_db_object.cant_create"),
                                          ts.l("create_db_object.custom_class_load_error_text"));
      }

    if (!ReturnVal.didSucceed(retVal))
      {
        // "Can''t Create Object"
        // "Can't create new object, the operation was refused"
        return ReturnVal.merge(Ganymede.createErrorDialog(ts.l("create_db_object.cant_create"),
                                                          ts.l("create_db_object.operation_refused")),
                               retVal);
      }

    newObj = (DBObject) retVal.getObject();

    setLastEvent("create " + newObj.getTypeName());

    retVal = ReturnVal.success();

    if (this.exportObjects)
      {
        exportObject(newObj);
      }

    retVal.setInvid(newObj.getInvid());
    retVal.setObject(newObj);

    return retVal;
  }

  /**
   * <p>Private server-side helper method used to pass a {@link
   * arlut.csd.ganymede.rmi.FileTransmitter FileTransmitter} reference
   * back that can be called to pull pieces of an XML transmission.</p>
   */

  private ReturnVal getXML(boolean sendData, boolean sendSchema, String syncChannel, boolean includeHistory, boolean includeOid) throws NotLoggedInException
  {
    checklogin();

    if (!permManager.isSuperGash())
      {
	// "Permissions Error"
	// "You do not have permissions to dump the server''s data with the xml client"
	return Ganymede.createErrorDialog(ts.l("global.permissions_error"),
					  ts.l("getXML.data_refused"));
      }

    XMLTransmitter transmitter = null;

    try
      {
	transmitter = new XMLTransmitter(sendData, sendSchema, syncChannel, includeHistory, includeOid);
      }
    catch (IOException ex)
      {
	// "Error transmitting XML"
	// "Exception caught trying to initialize server transmitter\n\n{0}"
	return Ganymede.createErrorDialog(ts.l("getXML.transmitter_error"),
					  ts.l("getXML.transmitter_error_msg", ex.getMessage()));
      }

    ReturnVal retVal = new ReturnVal(true);
    retVal.setFileTransmitter(transmitter);
    return retVal;
  }

  //
  //
  // Wizard management functions
  //
  //

  /**
   * <p>Returns true if a wizard is currently interacting
   * with the user.</p>
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public boolean isWizardActive()
  {
    return (wizard != null) && (wizard.isActive());
  }

  /**
   * <p>Returns true if a particular wizard is currently interacting
   * with the user.</p>
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public boolean isWizardActive(GanymediatorWizard wizard)
  {
    return (this.wizard == wizard) && (this.wizard.isActive());
  }

  /**
   * <p>Returns the active wizard, if any, for this
   * GanymedeSession.</p>
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public GanymediatorWizard getWizard()
  {
    return wizard;
  }

  /**
   * <p>This method is used to register a wizard for this GanymedeSession.
   *
   * If an active wizard is already registered, this method will return
   * false.</p>
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public boolean registerWizard(GanymediatorWizard wizard)
  {
    if (wizard != null && wizard.isActive())
      {
	return false;
      }
    else
      {
	this.wizard = wizard;
	return true;
      }
  }

  /**
   * <p>Unregisters a wizard from this GanymedeSession.</p>
   *
   * <p>If there is no active wizard registered, or if the registered wizard
   * is not equal to the wizard parameter, an IllegalArgumentException will
   * be thrown.</p>
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public void unregisterWizard(GanymediatorWizard wizard)
  {
    if (this.wizard == wizard)
      {
	this.wizard = null;
      }
    else
      {
	throw new IllegalArgumentException(ts.l("unregisterWizard.exception"));
      }
  }

  //
  //
  // RMI object publish/unpublish functions
  //
  //

  /**
   * <p>Export this object through RMI, so the client can make calls on it.</p>
   *
   * <p>Note that object may be (and often will be) a DBEditObject or subclass
   * thereof, not just a DBObject.</p>
   */

  private void exportObject(DBObject object)
  {
    // the exportObject call would fail if the object has already been
    // exported.  This can possibly happen if the client attempts to
    // edit an object in the active transaction which has already been
    // edited, so we'll check ahead of time to make sure we're not
    // re-exporting something.. otherwise, if the exportObject() call
    // fails for some reason, we'll throw up an exception.

    synchronized (exported)
      {
	for (DBObject exportedObject: exported)
	  {
	    if (exportedObject == object)
	      {
		return;
	      }
	  }

	Ganymede.rmi.publishObject(object);

	exported.add(object);
	object.exportFields();
      }
  }

  /**
   * <p>Unexport all exported objects, preventing any further RMI
   * calls from reaching them (for security's sake) and possibly
   * hastening garbage collection / lowering memory usage on the
   * server (for performance's sake).</p>
   *
   * <p>This method can safely be called without regard to whether
   * this GanymedeSession actually did export anything, as
   * exportObject() will only place objects in our local exported
   * ArrayList if this GanymedeSession is configured for remote access
   * with exported objects.</p>
   *
   * @param all if false, unexportObjects() will only unexport editing
   * objects, leaving view-only objects exported.
   */

  private void unexportObjects(boolean all)
  {
    synchronized (exported)
      {
	// count down from the top so we can remove things as we go

	for (int i = exported.size()-1; i >= 0; i--)
	  {
	    DBObject x = exported.get(i);

	    if (all || x instanceof DBEditObject)
	      {
		Ganymede.rmi.unpublishObject(x, true); // go ahead and force
		x.unexportFields();

		exported.remove(i);
	      }
	  }
      }
  }

  //
  //
  // Admin console reporting functions
  //
  //

  /**
   * <p>This method is used by the server to increment the admin
   * console's display of the number of objects this user session has
   * checked out and/or created.</p>
   */

  public synchronized void checkOut()
  {
    objectsCheckedOut++;
    resetAdminEntry();		// clear admin console info cache

    GanymedeAdmin.refreshUsers();
  }

  /**
   * <p>This method is used by the server to decrement the admin
   * console's display of the number of objects this user session has
   * checked out and/or created.</p>
   */

  public synchronized void checkIn()
  {
    objectsCheckedOut--;

    if (objectsCheckedOut < 0)
      {
	try
	  {
	    // "Ganymede session for {0} has a checkIn() cause objectsCheckedOut to go negative"
	    throw new RuntimeException(ts.l("checkIn.exception", getSessionName()));
	  }
	catch (RuntimeException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }
      }

    resetAdminEntry();	// clear admin console info cache

    GanymedeAdmin.refreshUsers();
  }

  /**
   * <p>This method is used to generate a serializable
   * {@link arlut.csd.ganymede.common.AdminEntry AdminEntry}
   * object summarizing this GanymedeSession's state for
   * the admin console.</p>
   *
   * <p>Used by code in
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}.</p>
   */

  public AdminEntry getAdminEntry()
  {
    AdminEntry info = userInfo;

    if (info == null)
      {
	info = new AdminEntry(getSessionName(),
			      permManager.getIdentity(),
			      getClientHostName(),
			      (status == null) ? "" : status,
			      connecttime.toString(),
			      (lastEvent == null) ? "" : lastEvent,
			      objectsCheckedOut);
	userInfo = info;
      }

    // userInfo might have been set null again by another
    // thread.. return the local variable to insure we return
    // something useful

    return info;
  }

  /**
   * <p>Clears the cached AdminEntry data for this session so that the
   * next getAdminEntry() call will generate a new AdminEntry value
   * rather than returning a cached one.</p>
   */

  public void resetAdminEntry()
  {
    this.userInfo = null;
  }

  /**
   * This method used to be used to flag an error condition that the
   * client could then call getLastError() to look up.  It has
   * been deprecated from that usage, and now simply logs the error.
   */

  void setLastError(String error)
  {
    Ganymede.debug("GanymedeSession [" + getSessionName() + "]: setLastError (" + error + ")");
  }

  /**
   * <p>This method is used to report an action performed by the user
   * to the admin consoles.</p>
   */

  void setLastEvent(String text)
  {
    // if we are being driven by the xml client or by an otherwise
    // internal session, we don't want to spam the admin console with
    // status messages.  The serverAdminProxy class will limit
    // excessive communications with the admin console, but no point
    // in getting ourselves all worked up here.

    if (!exportObjects)
      {
	return;
      }

    this.lastEvent = text;
    resetAdminEntry();
    GanymedeAdmin.refreshUsers();
  }

  /**
   * <p>This method is called by a background thread on the server,
   * and knocks this user off if they are a remote user who has been
   * inactive for a long time.</p>
   *
   * <p>Note that this method is not synchronized, to avoid
   * nested-monitor deadlock by the timeOutTask between a
   * GanymedeSession object and the GanymedeServer object.</p>
   */

  void timeCheck()
  {
    if (!remoteClient)
      {
	return;			// server-local session, we won't time it out
      }

    if (!loggedInSemaphore.isSet())
      {
	return;
      }

    long millisIdle = 0;

    synchronized (lastActionTime)
      {
        millisIdle = System.currentTimeMillis() - lastActionTime.getTime();
      }

    int minutesIdle = (int) (millisIdle / 60000);

    if (Ganymede.softtimeout)
      {
	// we don't time out users logged in without admin privileges in softtimeout

	if (!permManager.isPrivileged())
	  {
	    return;
	  }

	// if the time has come to kick the user off, we'll send him a
	// signal telling him to drop down to non-privileged.  If the
	// sendMessage() call throws an exception for some reason,
	// we'll log that.

	try
	  {
	    if ((minutesIdle > Ganymede.timeoutIdleNoObjs && objectsCheckedOut == 0) ||
		minutesIdle > Ganymede.timeoutIdleWithObjs)
	      {
		// if we've got a non-user based admin (i.e.,
		// supergash) logged in, we can't force them
		// non-privileged, so we'll skip sending the timeout
		// message and just wait another couple of minutes for
		// the forceoff.

		if (permManager.isElevated())
		  {
		    // "Sending a timeout message to {0}"
		    System.err.println(ts.l("timeCheck.sending", this.toString()));

		    timedout = true;

		    // sending this message to the client should cause it
		    // to set persona back to the unprivileged state,
		    // coincidentally resetting the lastActionTime in the
		    // process.

		    sendMessage(ClientMessage.SOFTTIMEOUT, null);
		  }
	      }
	  }
	catch (Throwable ex)
	  {
	    // "Throwable condition caught while trying to send a timeout message to {0}:\n\n{1}"
	    Ganymede.debug(ts.l("timeCheck.caught_throwable", this.toString(), Ganymede.stackTrace(ex)));
	  }
	finally
	  {
	    // we give the client two minutes to respond to the
	    // SOFTTIMEOUT message, then we get mean.

	    if (minutesIdle > (Ganymede.timeoutIdleNoObjs + 2) && objectsCheckedOut == 0)
	      {
		forceOff(ts.l("timeCheck.forceOffNoObjs", Integer.valueOf(Ganymede.timeoutIdleNoObjs)));
	      }
	    else if (minutesIdle > (Ganymede.timeoutIdleWithObjs + 2))
	      {
		forceOff(ts.l("timeCheck.forceOffWithObjs", Integer.valueOf(Ganymede.timeoutIdleWithObjs)));
	      }
	  }

	return;
      }

    if (minutesIdle > Ganymede.timeoutIdleNoObjs && objectsCheckedOut == 0)
      {
	forceOff(ts.l("timeCheck.forceOffNoObjs", Integer.valueOf(Ganymede.timeoutIdleNoObjs)));
      }
    else if (minutesIdle > Ganymede.timeoutIdleWithObjs)
      {
	forceOff(ts.l("timeCheck.forceOffWithObjs", Integer.valueOf(Ganymede.timeoutIdleWithObjs)));
      }
  }

  /**
   * <p>If the server decides this person needs to get off (if the user
   * times out, is forced off by an admin, or if the server is going
   * down), it will call this method to knock them off.</p>
   *
   * <p>Note that this method is not synchronized, to avoid the possibility
   * of deadlocking the admin console in the case of a deadlocked
   * GanymedeSession.</p>
   */

  void forceOff(String reason)
  {
    if (loggedInSemaphore.isSet())
      {
	if (Ganymede.log != null)
	  {
	    // "Abnormal termination for username: {0}\n\n{1}"
	    Ganymede.log.logSystemEvent(new DBLogEvent("abnormallogout",
						       ts.l("forceOff.log_event", permManager.getUserName(), reason),
						       permManager.getUserInvid(),
						       permManager.getUserName(),
						       permManager.getIdentityInvids(),
						       null));
	  }

	// "Forcing {0} off for {1}."
	Ganymede.debug(ts.l("forceOff.forcing", permManager.getUserName(), reason));

	if (asyncPort != null)
	  {
	    try
	      {
		asyncPort.shutdown(reason);
	      }
	    catch (RemoteException ex)
	      {
	      }
	  }

	logout(true);		// keep logout from logging a normal logout
      }
  }

  /**
   * <p>This method is used to send an asynchronous message
   * to the client.  It is used to update the clients so they
   * know when a build is being processed.</p>
   *
   * <p>See {@link arlut.csd.ganymede.common.ClientMessage ClientMessage}
   * for the list of acceptable client message types.</p>
   */

  void sendMessage(int type, String message)
  {
    if (type < ClientMessage.FIRST || type > ClientMessage.LAST)
      {
	throw new IllegalArgumentException(ts.l("sendMessage.exception"));
      }

    if (asyncPort != null)
      {
	try
	  {
	    asyncPort.sendMessage(type, message);	// async proxy
	  }
	catch (RemoteException ex)
	  {
	  }
      }
  }

  /**
   * <p>This method is called when the Java RMI system detects that this
   * remote object is no longer referenced by any remote objects.</p>
   *
   * <p>This method handles abnormal logouts and time outs for us.  By
   * default, the 1.1 RMI time-out is 10 minutes.</p>
   *
   * <p>The RMI timeout can be modified by setting the system property
   * sun.rmi.transport.proxy.connectTimeout.</p>
   *
   * @see java.rmi.server.Unreferenced
   */

  public void unreferenced()
  {
    /* the xmlclient may well drop reference to us (the
       GanymedeSession), as all it really cares about is the
       GanymedeXMLSession..  if we have an xSession reference, assume
       that all is well.. the GanymedeXMLSession has its own
       unreferenced method that can handle dead xml clients, after
       all.
    */

    if (xSession != null)
      {
	return;
      }

    if (loggedInSemaphore.isSet())
      {
	// "Network connection to the Ganymede client process has been lost."
	forceOff(ts.l("unreferenced.reason"));
      }
  }

  /**
   * This method provides a handy description of this session.
   */

  public String toString()
  {
    return "GanymedeSession [" + permManager.getUserName() + "," + permManager.getPersonaName() + "]";
  }
}
