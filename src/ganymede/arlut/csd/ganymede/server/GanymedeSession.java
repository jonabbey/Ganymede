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

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System

   Copyright (C) 1996-2008
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

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.net.ProtocolException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import Qsmtp.Qsmtp;
import arlut.csd.Util.RandomUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.WordWrap;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.ganymede.common.AdminEntry;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.PermMatrix;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.QueryResultContainer;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.ClientAsyncResponder;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 GanymedeSession

------------------------------------------------------------------------------*/

/**
 * User-level session object in the Ganymede server.  Each client
 * that logs in to the Ganymede server through the {@link
 * arlut.csd.ganymede.server.GanymedeServer GanymedeServer} {@link
 * arlut.csd.ganymede.server.GanymedeServer#login(java.lang.String, java.lang.String)
 * login()} method gets a GanymedeSession object, which oversees that
 * client's interactions with the server.  The client talks to its
 * GanymedeSession object through the {@link
 * arlut.csd.ganymede.rmi.Session Session} RMI interface, making calls to
 * access schema information and database objects in the server.
 *
 * The GanymedeSession class provides query and editing services to
 * the client, tracks the client's status, and manages permissions.
 * Most of the actual database handling is done through a second,
 * database-layer session object called a {@link
 * arlut.csd.ganymede.server.DBSession DBSession} that GanymedeSession
 * maintains for each user.  GanymedeSession methods like {@link
 * arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
 * view_db_object()} and {@link
 * arlut.csd.ganymede.server.GanymedeSession#edit_db_object(arlut.csd.ganymede.common.Invid)
 * edit_db_object()} make calls on the DBSession to actually access
 * {@link arlut.csd.ganymede.server.DBObject DBObjects} in the Ganymede
 * database.  The client then receives a serialized {@link
 * arlut.csd.ganymede.common.ReturnVal ReturnVal} which may include a remote
 * {@link arlut.csd.ganymede.rmi.db_object db_object} reference so that
 * the client can directly talk to the DBObject or
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} over RMI.
 *
 * Once a GanymedeSession is created by the GanymedeServer's login()
 * method, the client is considered to be authenticated, and may make
 * as many Session calls as it likes, until the GanymedeSession's
 * {@link arlut.csd.ganymede.server.GanymedeSession#logout() logout()} method
 * is called.  If the client dies or loses its network connection to 
 * the Ganymede server for more than 10 minutes, the RMI system will
 * automatically call GanymedeSession's
 * {@link arlut.csd.ganymede.server.GanymedeSession#unreferenced() unreferenced()}
 * method, which will log the client out from the server.
 *
 * The Ganymede server is transactional, and the client must call
 * {@link
 * arlut.csd.ganymede.server.GanymedeSession#openTransaction(java.lang.String)
 * openTransaction()} before making any changes via edit_db_object()
 * or other editing methods.  In turn, changes made by the client
 * using the edit_db_object() and related methods will not actually be
 * 'locked-in' to the Ganymede database until the {@link
 * arlut.csd.ganymede.server.GanymedeSession#commitTransaction()
 * commitTransaction()} method is called.
 *
 * Most methods in this class are synchronized to avoid race condition
 * security holes between the persona change logic and the actual operations.
 * 
 * @version $Id$
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
   * Async responder for sending async messages to the client.
   */

  private serverClientAsyncResponder asyncPort = null;

  /**
   * if this session is on the GanymedeServer's lSemaphore, this boolean
   * will be true.
   */

  boolean semaphoreLocked = false;

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

  boolean timedout = false;

  /**
   * A flag indicating whether the client has supergash priviliges.  We
   * keep track of this to speed internal operations.
   */

  boolean supergashMode = false;

  /**
   * GanymedeSessions created for internal operations always operate
   * with supergash privileges.  We'll set this flag to true to avoid
   * having to do persona membership checks on initial set-up.
   */

  boolean beforeversupergash = false; // Be Forever Yamamoto

  /**
   * A count of how many objects this session has currently checked out.
   */

  int objectsCheckedOut = 0;

  /**
   * This variable tracks whether or not the client desires to have
   * wizards presented.  If this is false, custom plug-in code
   * for the object types stored in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore} may either
   * refuse certain operations or will resort to taking a default action.
   */
  
  public boolean enableWizards = true;

  /**
   * If this variable is set to false, no custom wizard code will ever
   * be invoked, and required fields will not be forced.  This is
   * intended primarily for direct database loading.
   *
   * This variable is not intended ever to be available to the client,
   * but should only be set by local server code.
   */
  
  public boolean enableOversight = true;

  /**
   * The time that this client initially connected to the server.  Used
   * by the admin console code.
   */

  Date connecttime;

  /** 
   * The time of the user's last top-level operation.. Used to
   * provide guidance on time-outs.  Updated whenever checklogin()
   * is called.
   */

  Date lastActionTime = new Date();

  /**
   * The name of the user logged in.  If the person logged in is using
   * supergash, username will be supergash, even though supergash
   * isn't technically a user.
   */

  private String username;

  /**
   * The unique name that the user is connected to the server
   * under.. this may be &lt;username&gt;[2], &lt;username&gt;[3],
   * etc., if the user is connected to the server multiple times.  The
   * sessionName will be unique on the server at any given time.
   *
   * sessionName should never be null.  If a client logs in directly
   * to a persona, sessionName will be that personaname plus an
   * optional session integer.
   */

  private String sessionName;

  /**
   *
   * The object reference identifier for the logged in user, if any.
   * If the client logged in directly to a persona account, this will
   * be null.  See personaInvid in that case.
   *
   */

  Invid userInvid;

  /**
   *
   * The DNS name for the client's host
   *
   */

  String clienthost;

  /**
   * The current status message for this client.  The 
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} code
   * that manages the admin consoles will consult this String when it
   * updates the admin consoles.
   */

  String status = null;

  /**
   * Description of the last action recorded for this client.  The
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}
   * code that manages the admin consoles will consult
   * this String when it updates the admin consoles.
   */

  String lastEvent = null;

  /**
   * Our DBSession object.  DBSession is the generic DBStore access
   * layer.  A GanymedeSession is layered on top of a DBSession to
   * provide access control and remote access via RMI.  The DBSession
   * object is accessible to server-side code only and provides
   * transaction support.
   */

  DBSession session;

  /**
   * A GanymedeSession can have a single wizard active.  If this variable
   * is non-null, a custom type-specific
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass has instantiated
   * a wizard to interact with the user.
   */

  GanymediatorWizard wizard = null;

  /**
   *
   * Convenience persistent reference to the adminPersonae object base
   *
   */

  DBObjectBase personaBase = null;

  /**
   *
   * When did we last check our persona permissions?
   *
   */

  Date personaTimeStamp = null;

  /**
   * A reference to our current persona object.  We save this so
   * we can look up owner groups and what not more quickly.  An
   * end-user logged in without any extra privileges will have
   * a null personaObj value.
   */

  DBObject personaObj = null;

  /**
   *
   * The name of the current persona, of the form '&lt;username&gt;:&lt;description&gt;',
   * for example, 'broccol:GASH Admin'.  If the user is logged in with
   * just end-user privileges, personaName will be null.
   *
   */

  String personaName = null;

  /**
   *
   * The object reference identifier for the current persona, if any.
   *
   */

  Invid personaInvid;

  /**
   *
   * Convenience persistent reference to the Permission Matrix object base
   *
   */

  DBObjectBase permBase = null;

  /**
   *
   * When did we last update our local cache/summary of permissions records?
   *
   */

  Date permTimeStamp;

  /**
   * This variable stores the permission bits that are applicable to objects
   * that the current persona has ownership privilege over.  This matrix
   * is always a permissive superset of
   * {@link arlut.csd.ganymede.server.GanymedeSession#defaultPerms defaultPerms}.
   */

  PermMatrix personaPerms;

  /**
   * This variable stores the permission bits that are applicable
   * to generic objects not specifically owned by this persona.
   *
   * Each permission object in the Ganymede database includes
   * permissions as apply to objects owned by the persona and as apply
   * to objects not owned by the persona.
   *
   * This variable holds the union of the 'as apply to objects not
   * owned by the persona' matrices across all permissions objects
   * that apply to the current persona.
   */

  PermMatrix defaultPerms;

  /**
   * This variable stores the permission bits that are applicable to
   * objects that the current persona has ownership privilege over and
   * which the current admin has permission to delegate to subordinate
   * roles.  This matrix is always a permissive superset of
   * {@link arlut.csd.ganymede.server.GanymedeSession#delegatableDefaultPerms
   * delegatableDefaultPerms}.
   */

  PermMatrix delegatablePersonaPerms;

  /**
   * This variable stores the permission bits that are applicable to
   * generic objects not specifically owned by this persona and which
   * the current admin has permission to delegate to subordinate
   * roles.
   *
   * Each permission object in the Ganymede database includes
   * permissions as apply to objects owned by the persona and as apply
   * to objects not owned by the persona.
   *
   * This variable holds the union of the 'as apply to objects not
   * owned by the persona' matrices across all permissions objects
   * that apply to the current persona.
   */

  PermMatrix delegatableDefaultPerms;

  /**
   * A reference to the Ganymede {@link arlut.csd.ganymede.server.DBObject DBObject}
   * storing our default permissions,
   * or the permissions that applies when we are not in supergash mode
   * and we do not have any ownership over the object in question.
   */

  DBObject defaultObj;

  /**
   * This variable is a vector of object references
   * ({@link arlut.csd.ganymede.common.Invid Invid}'s) to the owner groups
   * that the client has requested newly created objects be placed in.  While
   * this vector is not-null, any new objects created will be owned by the list
   * of ownergroups held here.
   */

  Vector newObjectOwnerInvids = null;

  /**
   * This variable is a vector of object references (Invid's) to the
   * owner groups that the client has requested the listing of objects
   * be restricted to.  That is, the client has requested that the
   * results of Queries and Dumps only include those objects owned by
   * owner groups in this list.  This feature is used primarily for
   * when a client is logged in with supergash privileges, but the
   * user wants to restrict the visibility of objects for convenience.
   */

  Vector visibilityFilterInvids = null;

  /**
   * This variable caches the {@link arlut.csd.ganymede.common.AdminEntry AdminEntry}
   * object which is reported to admin consoles connected to the
   * server when the console is updated.
   */

  AdminEntry userInfo = null;

  /**
   * If true, this GanymedeSession will export its objects and fields for
   * direct access via RMI.
   */

  boolean exportObjects = false;

  /**
   * If this session is being driven by a GanymedeXMLSession, this reference
   * will be non-null.
   */

  GanymedeXMLSession xSession = null;

  /**
   * List of exported DBObjects (and DBEditObjects and subclasses thereof), so we
   * can forcibly unexport them at logout time.
   */

  ArrayList exported = new ArrayList();

  /* -- */

  /**
   * Constructor for a server-internal GanymedeSession.  Used when
   * the server's internal code needs to do a query, etc.  Note that
   * the Ganymede server will create one of these fairly early
   * on, and will keep it around for internal usage.  Note that we
   * don't add this to the data structures used for the admin
   * console.
   *
   * Note that all internal session activities (queries, etc.) are
   * currently using a single, synchronized GanymedeSession object.. this
   * mean that only one user at a time can currently be processed for
   * login. 8-(
   * 
   * Internal sessions, as created by this constructor, have full
   * privileges to do any possible operation.
   */

  public GanymedeSession() throws RemoteException
  {
    this("internal");
  }

  /**
   * Constructor for a server-internal GanymedeSession.  Used when
   * the server's internal code needs to do a query, etc.  Note that
   * the Ganymede server will create one of these fairly early
   * on, and will keep it around for internal usage.  Note that we
   * don't add this to the data structures used for the admin
   * console.
   *
   * Note that all internal session activities (queries, etc.) are
   * currently using a single, synchronized GanymedeSession object.. this
   * mean that only one user at a time can currently be processed for
   * login. 8-(
   * 
   * Internal sessions, as created by this constructor, have full
   * privileges to do any possible operation.
   */

  public GanymedeSession(String sessionLabel) throws RemoteException
  {
    // handle the server login semaphore for this session

    // if we are attempting to start a builder session, we'll proceed
    // even if the server is waiting to handle a deferred shutdown.

    // otherwise, if we are not starting one of the master internal
    // sessions (either Ganymede.internalSession or
    // GanymedeServer.loginSession), we'll want to increment the login
    // semaphore to make sure we are allowing logins and to keep the
    // server up to date

    String disabledMessage = GanymedeServer.lSemaphore.checkEnabled();
    
    if (sessionLabel.startsWith("builder:"))
      {
	if (disabledMessage != null && !disabledMessage.equals("shutdown"))
	  {
	    Ganymede.debug(ts.l("init.no_semaphore", sessionLabel, disabledMessage));

	    throw new RuntimeException(ts.l("init.semaphore_error", disabledMessage));
	  }
      }
    else if ((!sessionLabel.equals("internal") && !sessionLabel.startsWith("builder:")) || 
	     (sessionLabel.startsWith("builder:") && disabledMessage == null))
      {
        String error = GanymedeServer.lSemaphore.increment();

        if (error != null)
          {
            Ganymede.debug(ts.l("init.no_semaphore", sessionLabel, error));

            throw new RuntimeException(ts.l("init.semaphore_error", error));	    
          }
        else
          {
            semaphoreLocked = true;
          }
      }

    // construct our DBSession

    loggedInSemaphore.set(true);

    sessionName = sessionLabel;
    username = sessionLabel;
    clienthost = sessionLabel;
    session = new DBSession(Ganymede.db, this, sessionLabel);

    supergashMode = true;
    beforeversupergash = true;
    this.exportObjects = false;

    updatePerms(true);
  }

  /**
   * Constructor used to create a server-side attachment for a Ganymede
   * client.
   *
   * This constructor is called by the
   * {@link arlut.csd.ganymede.server server}
   * {@link arlut.csd.ganymede.rmi.Server#login(java.lang.String username, java.lang.String password) login()}
   * method.
   *
   * A Client can log in either as an end-user or as a admin persona.  Typically,
   * a client will log in with their end-user name and password, then use
   * selectPersona to gain admin privileges.  The server may allow users to
   * login directly with an admin persona (supergash, say), if so configured.
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
   * @see arlut.csd.ganymede.server#login(java.lang.String, java.lang.String)
   */
  
  public GanymedeSession(String loginName, DBObject userObject, 
			 DBObject personaObject, boolean exportObjects,
			 boolean clientIsRemote) throws RemoteException
  {
    // --

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

	Ganymede.rmi.publishObject(this); // may throw RemoteException
      }

    if (userObject != null)
      {
	userInvid = userObject.getInvid();
	username = userObject.getLabel();
      }
    else
      {
	userInvid = null;
      }

    if (personaObject != null)
      {
	personaInvid = personaObject.getInvid();
	personaName = personaObject.getLabel();

	if (username == null)
	  {
	    username = personaName; // for supergash, monitor
	  }
      }
    else
      {
	personaInvid = null;	// shouldn't happen
      }

    // find a unique name for this session

    sessionName = GanymedeServer.registerActiveUser(loginName);

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
	clienthost = "local (unknown)";
      }

    // record our login time

    connecttime = new Date();

    // construct our DBSession

    session = new DBSession(Ganymede.db, this, sessionName);

    // Let the GanymedeServer know that this session is now active for
    // purposes of admin console updating.

    GanymedeServer.addRemoteUser(this);

    // update status, update the admin consoles

    loggedInSemaphore.set(true);
    status = ts.l("init.loggedin");
    lastEvent = ts.l("init.loggedin");
    GanymedeAdmin.refreshUsers();

    // precalc the permissions applicable for this user

    updatePerms(true);

    // and we're done

    if (permsdebug)
      {
	Ganymede.debug("User " + username + " is " + (supergashMode ? "" : "not ") + 
		       "active with " + Ganymede.rootname + " privs");
      }
  }

  //************************************************************
  //
  // Non-remote methods (for server-side code)
  //
  //************************************************************

  public void setXSession(GanymedeXMLSession xSession)
  {
    this.xSession = xSession;
  }

  public boolean isSuperGash()
  {
    return supergashMode;
  }

  public boolean isLoggedIn()
  {
    return loggedInSemaphore.isSet();
  }

  /**
   * This method is used by the server to increment the admin
   * console's display of the number of objects this user session has
   * checked out and/or created.
   */

  public synchronized void checkOut()
  {
    objectsCheckedOut++;
    this.userInfo = null;	// clear admin console info cache

    GanymedeAdmin.refreshUsers();
  }

  /**
   * This method is used by the server to decrement the admin
   * console's display of the number of objects this user session has
   * checked out and/or created.
   */

  public synchronized void checkIn()
  {
    objectsCheckedOut--;

    if (objectsCheckedOut < 0)
      {
	try
	  {
	    throw new RuntimeException(ts.l("checkIn.exception", sessionName));
	  }
	catch (RuntimeException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	  }
      }

    this.userInfo = null;	// clear admin console info cache

    GanymedeAdmin.refreshUsers();
  }

  /**
   * This method is used to generate a serializable
   * {@link arlut.csd.ganymede.common.AdminEntry AdminEntry}
   * object summarizing this GanymedeSession's state for
   * the admin console.
   *
   * Used by code in
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}.
   */

  public AdminEntry getAdminEntry()
  {
    AdminEntry info = userInfo;

    if (info == null)
      {
	info = new AdminEntry(sessionName,
			      personaName,
			      clienthost,
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
   * This method used to be used to flag an error condition that the
   * client could then call getLastError() to look up.  It has
   * been deprecated from that usage, and now simply logs the error.
   */

  void setLastError(String error)
  {
    Ganymede.debug("GanymedeSession [" + sessionName + "]: setLastError (" + error + ")");
  }

  /**
   * This method is called by a background thread on the server, and 
   * knocks this user off if they are a remote user who has been inactive
   * for a long time.
   *
   * Note that this method is not synchronized, to avoid
   * nested-monitor deadlock by the timeOutTask between a
   * GanymedeSession object and the GanymedeServer object.
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

    long millisIdle = System.currentTimeMillis() - lastActionTime.getTime();

    int minutesIdle = (int) (millisIdle / 60000);

    if (Ganymede.softtimeout)
      {
	// we don't time out users logged in without admin privileges in softtimeout

	if (personaName == null && !isSuperGash())
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

		if (userInvid != null)
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
		forceOff(ts.l("timeCheck.forceOffNoObjs", new Integer(Ganymede.timeoutIdleNoObjs)));
	      }
	    else if (minutesIdle > (Ganymede.timeoutIdleWithObjs + 2))
	      {
		forceOff(ts.l("timeCheck.forceOffWithObjs", new Integer(Ganymede.timeoutIdleWithObjs)));
	      }
	  }

	return;
      }

    if (minutesIdle > Ganymede.timeoutIdleNoObjs && objectsCheckedOut == 0)
      {
	forceOff(ts.l("timeCheck.forceOffNoObjs", new Integer(Ganymede.timeoutIdleNoObjs)));
      }
    else if (minutesIdle > Ganymede.timeoutIdleWithObjs)
      {
	forceOff(ts.l("timeCheck.forceOffWithObjs", new Integer(Ganymede.timeoutIdleWithObjs)));
      }
  }

  /**
   * If the server decides this person needs to get off (if the user
   * times out, is forced off by an admin, or if the server is going
   * down), it will call this method to knock them off.
   *
   * Note that this method is not synchronized, to avoid the possibility
   * of deadlocking the admin console in the case of a deadlocked 
   * GanymedeSession.
   */

  void forceOff(String reason)
  {
    if (loggedInSemaphore.isSet())
      {
	// Construct a vector of invid's to place in the log entry we
	// are about to create.  This lets us search the log easily.

	Vector objects = new Vector();
	    
	if (userInvid != null)
	  {
	    objects.addElement(userInvid);
	  }
		
	if (personaInvid != null)
	  {
	    objects.addElement(personaInvid);
	  }
		
	if (Ganymede.log != null)
	  {
	    // "Abnormal termination for username: {0}\n\n{1}"
	    Ganymede.log.logSystemEvent(new DBLogEvent("abnormallogout",
						       ts.l("forceOff.log_event", username, reason),
						       userInvid,
						       username,
						       objects,
						       null));
	  }

	// "Forcing {0} off for {1}."
	Ganymede.debug(ts.l("forceOff.forcing", username, reason));

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
   * This method is used to send an asynchronous message
   * to the client.  It is used to update the clients so they
   * know when a build is being processed.
   *
   * See {@link arlut.csd.ganymede.common.ClientMessage ClientMessage}
   * for the list of acceptable client message types.
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
   * This method is called when the Java RMI system detects that this
   * remote object is no longer referenced by any remote objects.
   *
   * This method handles abnormal logouts and time outs for us.  By
   * default, the 1.1 RMI time-out is 10 minutes.
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

  //************************************************************
  //
  // All methods from this point on are part of the Server remote
  // interface, and can be called by the client via RMI.
  //
  //************************************************************

  /**
   * Log out this session.  After this method is called, no other
   * methods may be called on this session object.
   *
   * This method is partially synchronized, to avoid locking up
   * the admin console if this user's session has become deadlocked.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void logout()
  {
    this.logout(false);
  }

  /**
   * Log out this session.  After this method is called, no other
   * methods may be called on this session object.
   *
   * This method is partially synchronized, to avoid locking up
   * the admin console if this user's session has become deadlocked.
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

		session.logout();	// *sync* DBSession
		return;
	      }

	    //	Ganymede.debug("User " + username + " logging off");

	    if (this.asyncPort != null)
	      {
		this.asyncPort.shutdown();
		this.asyncPort = null;
	      }

	    // logout the client, abort any DBSession transaction going

	    session.logout();	// *sync* DBSession

	    // if we have DBObjects left exported through RMI, make
	    // them inaccesible

	    unexportObjects(true);

	    // if we ourselves were exported, unexport

	    if (remoteClient)
	      {
		try
		  {
		    // must force, since we ourselves are probably in
		    // the middle of an RMI call

		    Ganymede.rmi.unpublishObject(this, true);
		  }
		catch (NoSuchObjectException ex)
		  {
		  }
	      }

	    // if we weren't forced off, do normal logout logging

	    if (!forced_off)
	      {
		// Construct a vector of invid's to place in the log entry we
		// are about to create.  This lets us search the log easily.

		Vector objects = new Vector();

		if (userInvid != null)
		  {
		    objects.addElement(userInvid);
		  }

		if (personaInvid != null)
		  {
		    objects.addElement(personaInvid);
		  }

		if (Ganymede.log != null)
		  {
		    Ganymede.log.logSystemEvent(new DBLogEvent("normallogout",
							       ts.l("logout.normal_event", username),
							       userInvid,
							       username,
							       objects,
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
		GanymedeServer.lSemaphore.decrement();
	      }

	    // if we are the last user logged in and the server is in
	    // deferred shutdown mode, clearActiveUser() will shut the
	    // server down, so the rest of of the stuff below may not
	    // happen
	    
	    GanymedeServer.clearActiveUser(sessionName);

	    // help the garbage collector

	    connecttime = null;
	    lastActionTime = null;
	    // skip username.. we'll do it below
	    // skip userInvid.. we'll do it below
	    clienthost = null;
	    status = null;
	    lastEvent = null;
	    session = null;
	    wizard = null;
	    personaBase = null;
	    permTimeStamp = null;
	    personaObj = null;
	    personaName = null;
	    // skip personaInvid.. we'll do it below
	    permBase = null;
	    personaPerms = null;
	    defaultPerms = null;
	    delegatablePersonaPerms = null;
	    delegatableDefaultPerms = null;
	    defaultObj = null;
	    newObjectOwnerInvids = null;
	    visibilityFilterInvids = null;
	    userInfo = null;
	    xSession = null;
	  }

	// guess we're still running.  Remember the last time this
	// user logged out for the motd-display check

	if (userInvid != null)
	  {
	    GanymedeServer.userLogOuts.put(userInvid, new Date());
	  }
	else
	  {
	    GanymedeServer.userLogOuts.put(personaInvid, new Date());
	  }

	Ganymede.debug(ts.l("logout.logged_off", username));

	userInvid = null;
	personaInvid = null;
	username = null;
      }
  }

  /**
   * This method is used to allow a client to request that wizards
   * not be provided in response to actions by the client.  This
   * is intended to allow non-interactive or non-gui clients to
   * do work without having to go through a wizard interaction
   * sequence.
   *
   * Wizards are enabled by default.
   *
   * @param val If true, wizards will be enabled.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public void enableWizards(boolean val)
  {
    this.enableWizards = val;
  }

  /**
   * This method is used to allow local server-side code to request
   * that no oversight be maintained over changes made to the server
   * through this GanymedeSession.
   *
   * This is intended <b>only</b> for trusted code that does its own
   * checking and validation on changes made to the database.  If
   * oversight is turned off, no wizard code will be called, and the
   * required field logic will be bypassed.  Extreme care must
   * be used in disabling oversight, and oversight should only be
   * turned off for direct loading and other situations where there
   * won't be multi-user use, to avoid breaking constraints that
   * custom plug-ins count on.
   *
   * Oversight is enabled by default.
   *
   * @param val If true, oversight will be enabled.
   * 
   */

  public void enableOversight(boolean val)
  {
    this.enableOversight = val;
  }

  /**
   * This method is used to tell the client where to look
   * to access the Ganymede help document tree.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public String getHelpBase()
  {
    return Ganymede.helpbaseProperty;
  }

  /**
   * This method is used to allow the client to retrieve messages like
   * the motd from the server.  The client can specify that it only
   * wants to see a message if it has changed since the user last
   * logged out.  This is intended to support a message of the day
   * type functionality.  The server will not necessarily remember the
   * last log out across server restart.
   *
   * @param key A string, like "motd", indicating what message to retrieve.
   * @param onlyShowIfNew If true, the message will only be returned if
   *                      it has changed since the user last logged out.
   *
   * @return A StringBuffer containing the message, if found, or null if no
   * message exists for the key, or if onlyShowIfNew was set and the message
   * was not new.
   */

  public StringBuffer getMessage(String key, boolean onlyShowIfNew)
  {
    Invid invidToCompare = null;

    /* -- */

    if (onlyShowIfNew)
      {
	invidToCompare = userInvid;

	if (invidToCompare == null)
	  {
	    invidToCompare = personaInvid;
	  }
      }

    return GanymedeServer.getTextMessage(key, invidToCompare, false);
  }

  /**
   * This method is used to allow the client to retrieve messages like
   * the motd from the server.  The client can specify that it only
   * wants to see a message if it has changed since the user last
   * logged out.  This is intended to support a message of the day
   * type functionality.  The server will not necessarily remember the
   * last log out across server restart.
   *
   * @param key A string, like "motd", indicating what message to retrieve.
   * @param onlyShowIfNew If true, the message will only be returned if
   *                      it has changed since the user last logged out.
   *
   * @return A StringBuffer containing the message, if found, or null if no
   * message exists for the key, or if onlyShowIfNew was set and the message
   * was not new.
   *   
   */

  public StringBuffer getMessageHTML(String key, boolean onlyShowIfNew)
  {
    Invid invidToCompare = null;

    /* -- */

    if (onlyShowIfNew)
      {
	invidToCompare = userInvid;

	if (invidToCompare == null)
	  {
	    invidToCompare = personaInvid;
	  }
      }

    return GanymedeServer.getTextMessage(key, invidToCompare, true);
  }

  /**
   * This method is used to allow the client to retrieve a remote reference to
   * a {@link arlut.csd.ganymede.server.serverClientAsyncResponder}, which will allow
   * the client to poll the server for asynchronous messages from the server.
   *
   * This is used to allow the server to send build status change notifications and
   * shutdown notification to the client, even if the client is behind a network
   * or personal system firewall.  The serverClientAsyncResponder blocks while there
   * is no message to send, and the client will poll for new messages.
   */

  public synchronized ClientAsyncResponder getAsyncPort() throws RemoteException
  {
    if (asyncPort != null)
      {
	return asyncPort;
      }

    asyncPort = new serverClientAsyncResponder();
    return (ClientAsyncResponder) asyncPort;
  }

  /**
   * This method returns the name of the user that is logged into this session.
   *
   * If supergash is using this session, this method will return supergash as
   * well, even though technically supergash isn't a user.
   * 
   * @see arlut.csd.ganymede.rmi.Session
   */

  public String getMyUserName()
  {
    return this.username;
  }

  /**
   * This method returns the unique name of this session.
   */

  public String getSessionName()
  {
    return this.sessionName;
  }

  /**
   * This method returns the name of the persona who is active, or the
   * raw user name if no persona privileges have been assumed.
   */

  public String getPersonaLabel()
  {
    if (personaName == null || personaName.equals(""))
      {
        return username;
      }
    else
      {
        return personaName;
      }
  }

  /**
   * This method returns the name of the system that the client
   * is connected from.
   */

  public String getClientHostName()
  {
    return this.clienthost;
  }

  /**
   * This method provides a handy description of this session.
   */

  public String toString()
  {
    return "GanymedeSession [" + username + "," + personaName + "]";
  }

  /**
   * This method returns a list of personae names available
   * to the user logged in.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector getPersonae() throws NotLoggedInException
  {
    checklogin();

    /* - */

    DBObject user;
    Vector results;
    InvidDBField inv;

    /* -- */

    user = getUser();

    if (user == null)
      {
	return null;
      }

    results = new Vector();

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    if (inv != null)
      {
	// it's okay to loop on this field since we should be looking
	// at a DBObject and not a DBEditObject

	for (int i = 0; i < inv.size(); i++)
	  {
	    results.addElement(viewObjectLabel((Invid)inv.getElementLocal(i)));
	  }
      }

    results.addElement(user.getLabel()); // add their 'end-user' persona

    return results;
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

    return personaName;
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

    /* - */

    DBObject 
      user,
      personaObject = null;

    InvidDBField inv;
    Invid invid;
    PasswordDBField pdbf;

    /* -- */

    user = getUser();

    if (user == null)
      {
	return false;
      }

    // if they are selecting their base username, go ahead and clear
    // out the persona privs and return true

    if (user.getLabel().equals(newPersona))
      {
	// if we've timed out, one of two things might be happening
	// here.  if the client gave us a password, they may be
	// revalidating their login for end-user access.
	// 
	// if they gave us no password, the client is itself
	// downshifting to the non-privileged level.

	if (timedout && password != null)
	  {
	    // "User {0} attempting to re-authenticate non-privileged login after being timed out."
	    Ganymede.debug(ts.l("selectPersona.attempting_timecheck", this.username));

	    pdbf = (PasswordDBField) user.getField(SchemaConstants.UserPassword);
	    
	    if (pdbf != null && pdbf.matchPlainText(password))
	      {
		timedout = false;
	      }
	    else
	      {
		// "User {0} failed to re-authenticate a login that timed out."
		Ganymede.debug(ts.l("selectPersona.failed_timecheck", this.username));
		return false;
	      }
	  }
	else
	  {
	    // "User {0}''s privileged login as {1} timed out.  Downshifting to non-privileged access."
	    Ganymede.debug(ts.l("selectPersona.giving_up", this.username, this.personaName));
	  }

	// the GUI client will close transactions first, but since we
	// might not be working on behalf of the GUI client, let's
	// make sure

	if (session.editSet != null)
	  {
	    String description = session.editSet.description;
	    boolean interactive = session.editSet.isInteractive();

	    // close the existing transaction

	    abortTransaction();

	    // open a new one with the same description and
	    // interactivity

	    openTransaction(description, interactive);
	  }

	personaObject = null;
	this.personaInvid = null;
	this.personaName = null;
	updatePerms(true);
	this.visibilityFilterInvids = null;
	this.userInfo = null;	// null our admin console cache
	this.username = user.getLabel(); // in case they logged in directly as an admin account
	setLastEvent("selectPersona: " + newPersona);
	return true;
      }

    // ok, we need to find out persona they are trying to switch to

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    // it's okay to loop on this field since we should be looking at a
    // DBObject and not a DBEditObject

    for (int i = 0; i < inv.size(); i++)
      {
	invid = (Invid) inv.getElementLocal(i);

	// it's okay to use the faster viewDBObject() here, because we
	// are always going to be doing this for internal purposes

	personaObject = session.viewDBObject(invid);

	if (!personaObject.getLabel().equals(newPersona))
	  {
	    personaObject = null;
	  }
	else
	  {
	    break;
	  }
      }

    if (personaObject == null)
      {
	// "Couldn''t find persona {0} for user: {1}"
	Ganymede.debug(ts.l("selectPersona.no_persona", newPersona, this.username));
	return false;
      }

    pdbf = (PasswordDBField) personaObject.getField(SchemaConstants.PersonaPasswordField);
    
    if (pdbf != null && pdbf.matchPlainText(password))
      {
	// "User {0} switched to persona {1}."
	Ganymede.debug(ts.l("selectPersona.switched", this.username, newPersona));

	this.personaName = personaObject.getLabel();

	if (timedout)
	  {
	    timedout = false;
	  }

	// the GUI client will close transactions first, but since we
	// might not be working on behalf of the GUI client, let's
	// make sure

	if (session.editSet != null)
	  {
	    String description = session.editSet.description;
	    boolean interactive = session.editSet.isInteractive();

	    // close the existing transaction

	    abortTransaction();

	    // open a new one with the same description and
	    // interactivity

	    openTransaction(description, interactive);
	  }

	this.personaInvid = personaObject.getInvid();
	updatePerms(true);
	this.userInfo = null;	// null our admin console cache
	this.username = user.getLabel(); // in case they logged in directly as an admin account
	this.visibilityFilterInvids = null;
	setLastEvent("selectPersona: " + newPersona);
	return true;
      }

    return false;
  }

  /**
   * This method returns a QueryResult of owner groups that the current
   * persona has access to.  This list is the transitive closure of
   * the list of owner groups in the current persona.  That is, the
   * list includes all the owner groups in the current persona along
   * with all of the owner groups those owner groups own, and so on.
   */

  public synchronized QueryResult getOwnerGroups() throws NotLoggedInException
  {
    checklogin();

    /* - */

    Query q;
    QueryResult result = new QueryResult();
    QueryResult fullOwnerList;
    Vector alreadySeen = new Vector();
    Invid inv;

    /* -- */

    if (personaInvid == null)
      {
	return result;		// End users don't have any owner group access
      }

    q = new Query(SchemaConstants.OwnerBase);
    q.setFiltered(false);

    fullOwnerList = query(q);

    // if we're in supergash mode, return a complete list of owner groups

    if (supergashMode)
      {
	return fullOwnerList;
      }

    // otherwise, we've got to do a very little bit of legwork

    for (int i = 0; i < fullOwnerList.size(); i++)
      {
	alreadySeen.removeAllElements();

	inv = fullOwnerList.getInvid(i);

	if (recursePersonaMatch(inv, alreadySeen))
	  {
	    result.addRow(inv, session.viewDBObject(inv).getLabel(), false);
	  }
      }
    
    return result;
  }

  /**
   * This method may be used to set the owner groups of any objects
   * created hereafter.
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal setDefaultOwner(Vector ownerInvids) throws NotLoggedInException
  {
    checklogin();

    /* - */

    Vector tmpInvids;
    Invid ownerInvidItem;

    /* -- */

    if (ownerInvids == null)
      {
	newObjectOwnerInvids = null;
	return null;
      }

    tmpInvids = new Vector();

    for (int i = 0; i < ownerInvids.size(); i++)
      {
	ownerInvidItem = (Invid) ownerInvids.elementAt(i);

	// this check is actually redundant, as the InvidDBField link
	// logic would catch such for us, but it makes a nice couplet
	// with the getNum() check below, so I'll leave it here.
	
	if (ownerInvidItem.getType() != SchemaConstants.OwnerBase)
	  {
	    return Ganymede.createErrorDialog(ts.l("setDefaultOwner.error_title"),
					      ts.l("setDefaultOwner.error_text"));
	  }

	// we don't want to explicitly place the object in
	// supergash.. all objects are implicitly availble to
	// supergash, no sense in making a big deal of it.

	// this is also redundant, since DBSession.createDBObject()
	// will filter this out as well.  Err.. I probably should
	// have faith in DBSession.createDBObject() and take this
	// whole loop out, but I'm gonna leave it for now.

	if (ownerInvidItem.getNum() == SchemaConstants.OwnerSupergash)
	  {
	    continue;
	  }

	tmpInvids.addElement(ownerInvidItem);
      }

    if (!supergashMode && !isMemberAll(tmpInvids))
      {
	return Ganymede.createErrorDialog(ts.l("setDefaultOwner.error_title"),
					  ts.l("setDefaultOwner.error_text2"));
      }
    else
      {
	newObjectOwnerInvids = tmpInvids;
	setLastEvent("setDefaultOwner");
	return null;
      }
  }

  /**
   * This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.
   *
   * This method cannot be used to grant access to objects that are
   * not accessible by the client's adminPersona.
   *
   * Calling this method with ownerInvids set to null will turn off the filtering.
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal filterQueries(Vector ownerInvids) throws NotLoggedInException
  {
    checklogin();

    /* - */

    if (ownerInvids == null || ownerInvids.size() == 0)
      {
	visibilityFilterInvids = null;
	return null;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
	return Ganymede.createErrorDialog(ts.l("filterQueries.error"),
					  ts.l("setDefaultOwner.error_text2"));
      }
    else
      {
	visibilityFilterInvids = ownerInvids;
	setLastEvent("filterQueries");
	return null;
      }
  }

  //  Database operations

  /**
   * This method returns a list of remote references to the Ganymede
   * object type definitions.  This method will throws a RuntimeException
   * if it is called when the server is in schemaEditMode.
   *
   * @deprecated Superseded by the more efficient getBaseList()
   * 
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector getTypes() throws NotLoggedInException
  {
    checklogin();

    /* - */

    Enumeration en;
    Vector result = new Vector();

    /* -- */

    en = Ganymede.db.objectBases.elements();

    while (en.hasMoreElements())
      {
	DBObjectBase base = (DBObjectBase) en.nextElement();
	
	if (getPerm(base.getTypeID(), true).isVisible())
	  {
	    result.addElement(base);
	  }
      }

    return result;
  }

  /**
   * Returns the root of the category tree on the server
   *
   * @deprecated Superseded by the more efficient getCategoryTree()
   *
   * @see arlut.csd.ganymede.rmi.Category
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Category getRootCategory() throws NotLoggedInException
  {
    checklogin();

    /* - */

    return Ganymede.db.rootCategory;
  }

  /**
   * Returns a serialized representation of the basic category
   * and base structure on the server.  The returned CategoryTransport
   * will include only object types that are editable by the user.
   *
   * @see arlut.csd.ganymede.rmi.Category
   * @see arlut.csd.ganymede.rmi.Session
   */

  public CategoryTransport getCategoryTree() throws NotLoggedInException
  {
    return this.getCategoryTree(true);
  }

  /**
   * Returns a serialized representation of the basic category
   * and base structure on the server.
   *
   * This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the CategoryTransport
   * constructor calls other synchronized methods on GanymedeSession.
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

    /* - */

    if (supergashMode)
      {
	// All sessions with supergash privileges can use the cached
	// full category tree transport object.. we'll build it here
	// if we need to.

	if (Ganymede.catTransport == null)
	  {
	    // pass Ganymede.internalSession so that the master
	    // CategoryTransport object will correctly grant
	    // object creation privs for all object types

	    Ganymede.catTransport = Ganymede.db.rootCategory.getTransport(Ganymede.internalSession, true);
	  }

	if (debug)
	  {
	    System.err.println("getCategoryTree(): returning system's complete category tree");
	  }

	return Ganymede.catTransport;
      }
    else
      {
	// not in supergash mode.. download a subset of the category tree to the user

	CategoryTransport transport = Ganymede.db.rootCategory.getTransport(this, hideNonEditables);

	if (debug)
	  {
	    System.err.println("getCategoryTree(): generated custom category tree");
	  }

	if (false)
	  {
	    System.err.println("%%% Printing PersonaPerms");
	    PermissionMatrixDBField.debugdump(personaPerms);

	    System.err.println("%%% Printing DefaultPerms");
	    PermissionMatrixDBField.debugdump(defaultPerms);
	  }
	
	return transport;
      }
  }

  /**
   * Returns a serialized representation of the object types
   * defined on the server.  This BaseListTransport object
   * will not include field information.  The client is
   * obliged to call getFieldTemplateVector() on any
   * bases that it needs field information for.
   *
   * This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the BaseListTransport
   * constructor calls other synchronized methods on GanymedeSession
   *
   * @see arlut.csd.ganymede.common.BaseListTransport
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized BaseListTransport getBaseList() throws NotLoggedInException
  {
    checklogin();

    /* - */

    if (supergashMode)
      {
	if (Ganymede.baseTransport != null)
	  {
	    return Ganymede.baseTransport;
	  }
	else
	  {
	    Enumeration bases;
	    DBObjectBase base;

	    BaseListTransport transport = new BaseListTransport();

	    // *sync* on DBStore, this GanymedeSession

	    // we sync on Ganymede.db to make sure that no one adds or deletes
	    // any object bases while we're creating our BaseListTransport.
	    // We could use the loginSemaphore, but that would be a bit heavy
	    // for our purposes here.
	    
	    synchronized (Ganymede.db)
	      {
		bases = Ganymede.db.objectBases.elements();
		
		while (bases.hasMoreElements())
		  {
		    base = (DBObjectBase) bases.nextElement();
		    base.addBaseToTransport(transport, null);
		  }
	      }

	    Ganymede.baseTransport = transport;

	    return transport;
	  }
      }
    else
      {
	Enumeration bases;
	DBObjectBase base;

	BaseListTransport transport = new BaseListTransport();

	// *sync* on DBStore, this GanymedeSession

	// we sync on Ganymede.db to make sure that no one adds or deletes
	// any object bases while we're creating our BaseListTransport.
	// We could use the loginSemaphore, but that would be a bit heavy
	// for our purposes here.

	synchronized (Ganymede.db)
	  {
	    bases = Ganymede.db.objectBases.elements();

	    while (bases.hasMoreElements())
	      {
		base = (DBObjectBase) bases.nextElement();
		base.addBaseToTransport(transport, this);
	      }
	  }

	return transport;
      }
  }

  /**
   * Returns a vector of field definition templates, in display order.
   *
   * This vector may be cached, as it is static for this object type over
   * the lifetime of any GanymedeSession.
   *
   * @see arlut.csd.ganymede.common.FieldTemplate
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector getFieldTemplateVector(String baseName) throws NotLoggedInException
  {
    checklogin();

    /* - */
    
    DBObjectBase base = Ganymede.db.getObjectBase(baseName);
    return base.getFieldTemplateVector();
  }

  /**
   * Returns a vector of field definition templates, in display order.
   *
   * This vector may be cached, as it is static for this object type over
   * the lifetime of any GanymedeSession.
   *
   * @see arlut.csd.ganymede.common.FieldTemplate
   * @see arlut.csd.ganymede.rmi.Session
   */

  public Vector getFieldTemplateVector(short baseId) throws NotLoggedInException
  {
    checklogin();

    /* - */
    
    DBObjectBase base = Ganymede.db.getObjectBase(baseId);
    return base.getFieldTemplateVector();
  }

  /**
   * This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).
   *
   * Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an error dialog will be returned in that case.
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   * 
   * @see arlut.csd.ganymede.rmi.Session
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal openTransaction(String describe) throws NotLoggedInException
  {
    return this.openTransaction(describe, true);
  }

  /**
   * This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).
   *
   * Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an error dialog will be returned in that case.
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   * 
   * @see arlut.csd.ganymede.rmi.Session
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
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

    if (session.editSet != null)
      {
        // "Server: Error in openTransaction()"
        // "Error.. transaction already opened"
	return Ganymede.createErrorDialog(ts.l("openTransaction.error"),
					  ts.l("openTransaction.error_text"));
      }

    /* - */

    session.openTransaction(describe, interactive); // *sync* DBSession

    this.status = "Transaction: " + describe;
    setLastEvent("openTransaction");

    return null;
  }

  /**
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.
   *
   * If the transaction cannot be committed for some reason,
   * commitTransaction() will abort the transaction if abortOnFail is
   * true.  In any case, commitTransaction() will return a ReturnVal
   * indicating whether or not the transaction could be committed, and
   * whether or not the transaction remains open for further attempts
   * at commit.  If ReturnVal.doNormalProcessing is set to true, the
   * transaction remains open and it is up to the client to decide
   * whether to abort the transaction by calling abortTransaction(),
   * or to attempt to fix the reported problem and try another call
   * to commitTransaction().
   *
   * This method is synchronized to avoid nested-monitor deadlock in
   * {@link arlut.csd.ganymede.server.DBSession#commitTransaction() DBSession.commitTransaction()}
   * .
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

    if (session.editSet == null)
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

    retVal = session.commitTransaction(); // *sync* DBSession DBEditSet

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
        GanymedeServer.sendMessageToRemoteSessions(ClientMessage.COMMITNOTIFY, ts.l("commitTransaction.user_committed", getPersonaLabel()), this);
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
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.
   *
   * commitTransaction() will return a ReturnVal indicating whether or
   * not the transaction could be committed, and whether or not the
   * transaction remains open for further attempts at commit.  If
   * ReturnVal.doNormalProcessing is set to true, the transaction
   * remains open and it is up to the client to decide whether to
   * abort the transaction by calling abortTransaction(), or to
   * attempt to fix the reported problem and try another call to
   * commitTransaction().
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
    return commitTransaction(false);
  }

  /**
   *
   * This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.
   *
   * @return a ReturnVal object if the transaction could not be aborted,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal abortTransaction() throws NotLoggedInException
  {
    checklogin();

    if (session.editSet == null)
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

    ReturnVal retVal = session.abortTransaction(); // *sync* DBSession 

    // "User {0} cancelled transaction."
    GanymedeServer.sendMessageToRemoteSessions(ClientMessage.ABORTNOTIFY, ts.l("abortTransaction.user_aborted", getPersonaLabel()), this);

    return retVal;
  }

  /**
   * This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.
   *
   * body and HTMLbody are StringBuffer's instead of Strings
   * because RMI (formerly had) a 64k serialization limit on the
   * String class.
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The content of the message.
   */

  public void sendMail(String address, String subject, StringBuffer body) throws NotLoggedInException
  {
    // If the server has been told to not send out any emails, then just bail
    // out.
    if (Ganymede.suppressEmail)
      {
      	return;
      }
    
    checklogin();

    /* - */

    Qsmtp mailer;
    String returnAddr;
    String mailsuffix;
    StringBuffer signature = new StringBuffer();
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

        // do we have a real user name, or a persona name?

        if (username.equals(Ganymede.rootname))
          {
            // supergash.. use the default return address

            returnAddr = Ganymede.returnaddrProperty;
          }
        else
          {
            returnAddr = this.username;

            mailsuffix = System.getProperty("ganymede.defaultmailsuffix");

            if (mailsuffix != null)
              {
                returnAddr += mailsuffix;
              }
          }

        // create the signature

        // "This message was sent by {0}, who is running the Ganymede client on {1}."
        signature.append(ts.l("sendMail.signature", username, clienthost));

        body.append("\n--------------------------------------------------------------------------------\n");
        body.append(WordWrap.wrap(signature.toString(), 78, null));
        body.append("\n--------------------------------------------------------------------------------\n");

        try
          {
            mailer.sendmsg(returnAddr,
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
   * This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.
   *
   * body and HTMLbody are StringBuffer's instead of Strings because RMI
   * has a 64k serialization limit on the String class.
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The plain-ASCII content of the message, or null if none.
   * @param HTMLbody The HTML content of the message, or null if none.
   *
   */

  public void sendHTMLMail(String address, String subject, StringBuffer body, StringBuffer HTMLbody) throws NotLoggedInException
  {
    // If the server has been told to not send out any emails, then just bail
    // out.
    if (Ganymede.suppressEmail)
      {
      	return;
      }
    
    checklogin();

    /* - */

    Qsmtp mailer;
    String returnAddr;
    String mailsuffix;
    StringBuffer asciiContent = new StringBuffer();
    StringBuffer signature = new StringBuffer();
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

        // do we have a real user name, or a persona name?

        if (username.equals(Ganymede.rootname))
          {
            // supergash.. use the default return address

            returnAddr = Ganymede.returnaddrProperty;
          }
        else
          {
            if (username.indexOf(':') == -1)
              {
                // real username, save it as is

                returnAddr = username;
              }
            else
              {
                // persona, extract the user's name out of it
                returnAddr = username.substring(0, username.indexOf(':'));
              }
    
            mailsuffix = System.getProperty("ganymede.defaultmailsuffix");

            if (mailsuffix != null)
              {
                returnAddr += mailsuffix;
              }
          }

        // create the signature

        if (body != null)
          {
            asciiContent.append(body);
            asciiContent.append("\n\n");
          }

        // "This message was sent by {0}, who is running the Ganymede client on {1}."
        signature.append(ts.l("sendMail.signature", username, clienthost));

        asciiContent.append("\n--------------------------------------------------------------------------------\n");
        asciiContent.append(WordWrap.wrap(signature.toString(), 78, null));
        asciiContent.append("\n--------------------------------------------------------------------------------\n");

        try
          {
            mailer.sendHTMLmsg(returnAddr,
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
   * This method allows clients to report client-side error/exception traces to
   * the server for logging and what-not.
   *
   * This method will also email the bug report to the email address
   * specified in the ganymede.bugsaddress property if it is set in
   * the server's ganymede.properties file.  This doesn't happen, of course,
   * if the server's emailing is disabled, either through the use of the
   * -suppressEmail flag in runServer, or by leaving the ganymede.mailhost
   * property undefined in ganymede.properties.
   *
   * @param clientIdentifier A string identifying any information
   * about the client that the client feels like providing.
   * @param exceptionReport A string holding any stack trace
   * information that might be helpful for the server to log or
   * transmit to a developer.
   */

  public void reportClientBug(String clientIdentifier, String exceptionReport) throws NotLoggedInException
  {
    String userIdentifier;

    if (personaName != null)
      {
	userIdentifier = personaName;
      }
    else if (personaName == null)
      {
	userIdentifier = username;
      }
    else
      {
	userIdentifier = "unknown user";
      }

    StringBuffer report = new StringBuffer();

    // "\nCLIENT ERROR DETECTED:\nuser == "{0}"\nhost == "{1}"\nclient id string == "{2}"\nexception trace == "{3}"\n"
    report.append(ts.l("reportClientBug.logPattern", userIdentifier, clienthost, clientIdentifier, exceptionReport));

    Ganymede.debug(report.toString());

    if (Ganymede.bugReportAddressProperty != null && !Ganymede.bugReportAddressProperty.equals(""))
      {
	sendMail(Ganymede.bugReportAddressProperty, "Ganymede Client Bug Report", report);
      }
  }

  /**
   * This method allows the client to get a status update on a
   * specific list of invids.
   *
   * If any of the invids are not currently defined in the server, or
   * if the client doesn't have permission to view any of the invids,
   * those invids' status will not be included in the returned
   * QueryResult.
   *
   * @param invidVector Vector of Invid's to get the status for.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized QueryResult queryInvids(Vector invidVector) throws NotLoggedInException
  {
    checklogin();

    /* - */

    QueryResult result = new QueryResult(true);	// for transport
    DBObject obj;
    Invid invid;
    PermEntry perm;

    /* -- */

    for (int i = 0; i < invidVector.size(); i++)
      {
	invid = (Invid) invidVector.elementAt(i);

	// the DBSession.viewDBObject() will look in the
	// current DBEditSet, if any, to find the version of
	// the object as it exists in the current transaction.
	
	obj = session.viewDBObject(invid);

	if (obj == null)
	  {
	    continue;
	  }

	perm = getPerm(obj);

	if (!perm.isVisible())
	  {
	    continue;
	  }

	result.addRow(obj.getInvid(), obj.getLabel(), obj.isInactivated(),
		      obj.willExpire(), obj.willBeRemoved(), perm.isEditable());
      }

    return result;
  }

  /**
   * Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.
   *
   * If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.
   *
   * This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.
   *
   * @param objectName Label for the object to lookup
   * @param objectType Name of the object type
   */

  public Invid findLabeledObject(String objectName, String objectType) throws NotLoggedInException
  {
    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
	// "Error, "{0}" is not a valid object type in this Ganymede server."
	throw new RuntimeException(ts.l("global.no_such_object_type", objectType));
      }

    return this.findLabeledObject(objectName, base.getTypeID());
  }

  /**
   * Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.
   *
   * If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.
   *
   * This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.
   *
   * @param name Label for an object
   * @param type Object type id number
   */

  public Invid findLabeledObject(String name, short type) throws NotLoggedInException
  {
    Invid value;

    /* -- */

    checklogin();

    Query localquery = new Query(type,
				 new QueryDataNode(QueryDataNode.EQUALS, name),
				 false);

    Vector results = internalQuery(localquery);

    if (debug)
      {
	Ganymede.debug("findLabeledObject() found results, size = " + results.size());
      }

    if (results == null || results.size() != 1)
      {
	return null;
      }

    Result tmp = (Result) results.elementAt(0);

    value = tmp.getInvid();

    // make sure we've got the right kind of object back.. this is a
    // debugging assertion to make sure that we're always handling
    // embedded objects properly.

    if (value.getType() != type)
      {
	throw new RuntimeException("findLabeledObject() ASSERTFAIL: Error in query processing," + 
				   " didn't get back right kind of object");
      }

    if (debug)
      {
	Ganymede.debug("findLabeledObject() found results, returning = " + value);
      }

    return value;
  }

  /**
   * This method provides the hook for doing a
   * fast database dump to a string form.  The 
   * {@link arlut.csd.ganymede.common.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.
   *
   * This version of dump() takes a query in string
   * form, based on Deepak's ANTLR-specified Ganymede
   * query grammar.
   *
   * This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.
   *
   * @see arlut.csd.ganymede.common.Query
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized DumpResult dump(String queryString) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query query = transmuter.transmuteQueryString(queryString);
    
    return dump(query);
  }

  /**
   * This method provides the hook for doing a
   * fast database dump to a string form.  The 
   * {@link arlut.csd.ganymede.common.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.
   *
   * This method uses the GanymedeSession query() apparatus, and
   * may not be called from a DBEditObject's commitPhase1/2() methods
   * without risking deadlock.
   *
   * @see arlut.csd.ganymede.common.Query
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized DumpResult dump(Query query) throws NotLoggedInException
  {
    checklogin();

    /* - */

    DumpResultBuilder resultBuilder;

    /**
     *
     * What base is the query being done on?
     *
     */

    DBObjectBase base = null;

    boolean embedded;

    /* -- */

    if (query == null)
      {
	setLastError("null query");
	return null;
      }

    // objectType will be -1 if the query is specifying the
    // base with the base's name

    if (query.objectType != -1)
      {
	base = Ganymede.db.getObjectBase(query.objectType);
      }
    else if (query.objectName != null)
      {
	base = Ganymede.db.getObjectBase(query.objectName);
      }

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    if (debug)
      {
	Ganymede.debug("Processing dump query\nSearching for matching objects of type " + base.getName());
      }

    setLastEvent("dump");

    if (debug)
      {
	if (!query.hasPermitSet())
	  {
	    Ganymede.debug("Returning default fields");
	  }
	else
	  {
	    Ganymede.debug("Returning custom fields");
	  }
      }

    if (debug)
      {
	Ganymede.debug("dump(): " + username + " : got read lock");
      }

    // search for the invid's matching the given query

    QueryResult temp_result = queryDispatch(query, false, false, null, null);

    if (debug)
      {
	System.err.println("dump(): processed queryDispatch, building dumpResult buffer");
      }

    // Figure out which fields we want to include in our result buffer

    Vector fieldDefs = new Vector();
    DBObjectBaseField field;
    Vector baseFields = base.getFields();

    for (int i = 0; i < baseFields.size(); i++)
      {
	field = (DBObjectBaseField) baseFields.elementAt(i);
	
	if (!query.hasPermitSet())
	  {
	    // If they haven't specified the list of fields they want
	    // back, make sure we don't show them built in fields and
	    // we don't show them the objects owned field in the
	    // OwnerBase.. that could entail many thousands of objects
	    // listed.  If they really, really want to see them, let
	    // them say so explicitly.

	    // XXX Note that as of DBStore version 2.7, we no longer
	    // include the OwnerObjectsOwned field in the OwnerBase,
	    // so this bit of logic isn't really operative any
	    // more. XXX

	    if (!(base.getTypeID() == SchemaConstants.OwnerBase &&
		  field.getID() == SchemaConstants.OwnerObjectsOwned))
	      {
		if (supergashMode)
		  {
		    fieldDefs.addElement(field);
		  }
		else if (getPerm(base.getTypeID(), field.getID(), true).isVisible())
		  {
		    fieldDefs.addElement(field);
		  }
	      }
	  }
	else if (query.returnField(field.getKey()))
	  {
	    if (supergashMode)
	      {
		fieldDefs.addElement(field);
	      }
	    else if (getPerm(base.getTypeID(), field.getID(), true).isVisible())
	      {
		fieldDefs.addElement(field);
	      }
	  }
      }

    // prepare the result buffer, given the requested fields

    resultBuilder = new DumpResultBuilder(fieldDefs);

    // and encode the desired fields into the result

    if (temp_result != null)
      {
	Invid invid;

	Vector invids = temp_result.getInvids();

	for (int i = 0; i < invids.size(); i++)
	  {
	    invid = (Invid) invids.elementAt(i);

	    if (debug)
	      {
		System.err.print(".");
	      }

	    // it's okay to use session.viewDBObject() because
	    // DumpResult.addRow() uses the GanymedeSession reference
	    // we pass in to handle per-field permissions

	    // using view_db_object() here would be disastrous,
	    // because it would entail making exported duplicates of
	    // all objects matching our query

	    resultBuilder.addRow(session.viewDBObject(invid), this);
	  }
      }

    if (debug)
      {
	Ganymede.debug("dump(): completed processing, returning buffer");
      }

    return resultBuilder.getDumpResult();
  }

  /**
   * This method applies this GanymedeSession's current owner filter
   * to the given QueryResult &lt;qr&gt; and returns a QueryResult
   * with any object handles that are not matched by the filter
   * stripped.
   *
   * If the submitted QueryResult &lt;qr&gt; is null, filterQueryResult()
   * will itself return null.
   */

  public QueryResult filterQueryResult(QueryResult qr)
  {
    if (qr == null)
      {
	return null;
      }

    QueryResult result = new QueryResult(qr.isForTransport());
    ObjectHandle handle;

    /* -- */

    Vector handles = qr.getHandles();

    for (int i = 0; i < handles.size(); i++)
      {
	handle = (ObjectHandle) handles.elementAt(i);

	Invid invid = handle.getInvid();

	if (invid != null)
	  {
	    DBObject obj = session.viewDBObject(invid);

	    if (filterMatch(obj))
	      {
		result.addRow(handle);
	      }
	  }
      }

    return result;
  }

  /**
   * This method provides the hook for doing all manner of simple
   * object listing for the Ganymede database.
   *
   * This version of query() takes a query in string form, based on
   * Deepak's ANTLR-specified Ganymede query grammar.
   *
   * This method may not be called from a DBEditObject's
   * commitPhase1/2() methods without risking deadlock.
   *
   * @see arlut.csd.ganymede.rmi.Session 
   */

  public QueryResult query(String queryString) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query query = transmuter.transmuteQueryString(queryString);
    
    return queryDispatch(query, false, true, null, null);
  }

  /**
   * This method provides the hook for doing all
   * manner of simple object listing for the Ganymede
   * database.
   *
   * This method may not be called from a DBEditObject's
   * commitPhase1/2() methods without risking deadlock.
   *
   * @see arlut.csd.ganymede.rmi.Session 
   */

  public QueryResult query(Query query) throws NotLoggedInException
  {
    checklogin();
    return queryDispatch(query, false, true, null, null);
  }

  /**
   * Server-side method for doing object listing with support for DBObject's
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method.
   *
   * @param query The query to be performed
   * @param perspectiveObject There are occasions when the server will want to do internal
   * querying in which the label of an object matching the query criteria is synthesized
   * for use in a particular context.  If non-null, perspectiveObject's 
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method will be used to generate the label for a result entry.
   */

  public QueryResult query(Query query, DBEditObject perspectiveObject) throws NotLoggedInException
  {
    checklogin();

    return queryDispatch(query, false, true, null, perspectiveObject);
  }

  public QueryResultContainer testQuery(String queryString) throws NotLoggedInException, GanyParseException
  {
    return testQuery(queryString, QueryResultContainer.ARRAYROWS);
  }
  
  public QueryResultContainer testQuery(String queryString, int rowType) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query q = transmuter.transmuteQueryString(queryString);
    QueryResult qr = queryDispatch(q, false, true, null, null); 
    QueryResultContainer qrc = new QueryResultContainer(rowType);

    /* Get the list of fields the user wants returned */
    Set fieldIDs = q.getFieldSet();
    
    /* Now add them to the result container */
    String fieldName;
    Short fieldID;
    DBObjectBaseField field;
    for (Iterator iter = fieldIDs.iterator(); iter.hasNext();)
      {
      	fieldID = (Short) iter.next();
      	field = (DBObjectBaseField) Ganymede.db.getObjectBase(q.objectName).getField(fieldID.shortValue());
      	fieldName = field.getName();
      	qrc.addField(fieldName, fieldID);
      } 

    /* Now we'll add a row for each matching object */
    List invids = qr.getInvids();
    Invid invid;
    DBObject object;
    PermEntry perm;
    boolean editable;
    Object[] row;
    for (Iterator iter = invids.iterator(); iter.hasNext();)
      {
        invid = (Invid) iter.next();
        object = Ganymede.db.getObject(invid);

        if (supergashMode)
          {
            perm = PermEntry.fullPerms;
          }
        else
          {
            perm = getPerm(object);
          }

        if ((perm == null) || (q.editableOnly && !perm.isEditable())
            || (!perm.isVisible()))
          {
            editable = false;
          }
        else
          {
            editable = perm.isEditable();
          }

        Short key;
        Object value;
	row = new Object[fieldIDs.size()];
        int i = 0;
        for (Iterator iter2 = fieldIDs.iterator(); iter2.hasNext(); i++)
          {
            key = (Short) iter2.next();
            value = object.getFieldValueLocal(key.shortValue());
            row[i] = value;
          }

        qrc.addRow(invid, object.getLabel(), row, object.isInactivated(), object
            .willExpire(), object.willBeRemoved(), editable);
      }

    return qrc;
  }

  /**
   * This method provides the hook for doing all manner of internal
   * object listing for the Ganymede database.  This method will not
   * take into account any optional owner filtering, but it will honor
   * the editableOnly flag in the Query.
   *
   * @return A Vector of {@link arlut.csd.ganymede.common.Result Result} objects
   */

  public Vector internalQuery(Query query)
  {
    Vector result = new Vector();
    QueryResult internalResult = queryDispatch(query, true, false, null, null);
    Invid key;
    String val;

    /* -- */

    if (internalResult != null)
      {
	for (int i = 0; i < internalResult.size(); i++)
	  {
	    key = (Invid) internalResult.getInvid(i);
	    val = (String) internalResult.getLabel(i);

	    result.addElement(new Result(key, val));
	  }
      }

    return result;
  }

  /**
   * This method is the primary Query engine for the Ganymede
   * databases.  It is used by dump(), query(), and internalQuery().
   *
   * @param query The query to be handled
   * @param internal If true, the query filter setting will not be honored
   * @param forTransport If true, the QueryResult will build a buffer for serialization
   * @param extantLock If non-null, queryDispatch will not attempt to establish its
   * own lock on the relevant base(s) for the duration of the query.  The extantLock must
   * have any bases that the queryDispatch method determines it needs access to locked, or
   * an IllegalArgumentException will be thrown.
   * @param perspectiveObject There are occasions when the server will want to do internal
   * querying in which the label of an object matching the query criteria is synthesized
   * for use in a particular context.  If non-null, perspectiveObject's 
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel}
   * method will be used to generate the label for a result entry.
   */

  public synchronized QueryResult queryDispatch(Query query, boolean internal, 
						boolean forTransport, DBLock extantLock,
						DBEditObject perspectiveObject)
  {
    QueryResult result = new QueryResult(forTransport);
    DBObjectBase base = null;
    Enumeration en;
    DBObject obj;
    DBLock rLock = null;

    /* -- */

    if (query == null)
      {
	throw new IllegalArgumentException(ts.l("queryDispatch.null_query"));
      }

    // objectType will be -1 if the query is specifying the
    // base with the base's name

    if (query.objectType != -1)
      {
	base = Ganymede.db.getObjectBase(query.objectType);
      }
    else if (query.objectName != null)
      {
	base = Ganymede.db.getObjectBase(query.objectName); // *sync* DBStore
      }

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    // are we able to optimize the query into a direct lookup?  If so,
    // we won't need to get a lock on the database, since viewDBObject()
    // will be nice and atomic for our needs

    if ((query.root instanceof QueryDataNode) &&
	((QueryDataNode) query.root).comparator == QueryDataNode.EQUALS)
      {
	QueryDataNode node = (QueryDataNode) query.root;
	DBObjectBaseField fieldDef = null;

	/* -- */

	// we're looking for a specific invid.. go ahead and do it

	if (node.fieldId == -2)
	  {
	    DBObject resultobject = session.viewDBObject((Invid) node.value);

	    addResultRow(resultobject, query, result, internal, perspectiveObject);

	    return result;
	  }

	// we're looking at a data field.. determine which field we're
	// looking at, find the dictionary definition for that field,
	// see if it is in a namespace so we can do a direct lookup
	// via a namespace hash.

	if (node.fieldId >= 0)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(node.fieldId);
	  }
	else if (node.fieldname != null)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(node.fieldname); // *sync* DBObjectBase
	  }
	else if (node.fieldId == -1)
	  {
	    fieldDef = (DBObjectBaseField) base.getField(base.getLabelField()); // *sync* DBObjectBase
	  }

        if (fieldDef == null)
          {
            // "Invalid field identifier"
            throw new IllegalArgumentException(ts.l("queryDispatch.bad_field"));
          }

	// now we've got a field definition that we can try to do a
	// direct look up on.  check to see if it has a namespace
	// index we can use

	if (fieldDef.namespace != null)
	  {
	    // aha!  We've got an optimized case!
	    
	    if (debug)
	      {
		System.err.println("Eureka!  Optimized query!\n" + query.toString());
	      }

	    DBObject resultobject;
	    DBNameSpace ns = fieldDef.namespace;

	    synchronized (ns)
	      {
		DBField resultfield = null;

		// if we are looking to match against an IP address
		// field and we were given a String, we need to
		// convert that String to an array of Bytes before
		// looking it up in the namespace

		if (fieldDef.isIP() && node.value instanceof String)
		  {
		    Byte[] ipBytes = null;

		    try
		      {
			ipBytes = IPDBField.genIPV4bytes((String) node.value);
		      }
		    catch (IllegalArgumentException ex)
		      {
		      }
		    
		    if (ipBytes != null)
		      {
			resultfield = ns.lookupMyValue(this, ipBytes);
		      }

		    // it's hard to tell here whether any fields of
		    // this type will accept IPv6 bytes, so if we
		    // don't find it as an IPv4 address, look for it
		    // as an IPv6 address

		    if (resultfield == null)
		      {
			try
			  {
			    ipBytes = IPDBField.genIPV6bytes((String) node.value);
			  }
			catch (IllegalArgumentException ex)
			  {
			  }

			if (ipBytes != null)
			  {
			    resultfield = ns.lookupMyValue(this, ipBytes);
			  }
		      }
		  }
		else
		  {
		    // we don't allow associating Invid fields
		    // with a namespace, so we don't need to try
		    // to convert strings to invids here for a
		    // namespace-optimized lookup

		    resultfield = ns.lookupMyValue(this, node.value); // *sync* DBNameSpace

		    if (debug)
		      {
			System.err.println("Did a namespace lookup in " + ns.getName() + 
					   " for value " + node.value);
			System.err.println("Found " + resultfield);
		      }
		  }

		if (resultfield == null)
		  {
		    return result;
		  }
		else
		  {
		    // a namespace can map across different field and
		    // object types.. make sure we've got an instance
		    // of the right kind of field

		    if (resultfield.getFieldDef() != fieldDef)
		      {
			if (debug)
			  {
			    System.err.println("Error, didn't find the right kind of field");
			    System.err.println("Found: " + resultfield.getFieldDef());
			    System.err.println("Wanted: " + fieldDef);
			  }

			return result;
		      }

		    // since we used this GanymedeSession to do
		    // the namespace lookup, we know that the
		    // owner object will be in the version we are
		    // editing, if any

		    resultobject = resultfield.owner;

		    if (debug)
		      {
			System.err.println("Found object: " + resultobject);
		      }

		    // addResultRow() will do our permissions checking for us

		    addResultRow(resultobject, query, result, internal, perspectiveObject);

		    if (debug)
		      {
			System.err.println("Returning result from optimized query");
		      }
		
		    return result;
		  }
	      }
	  }
      }

    // okay, so we weren't able to do a namespace index lookup

    // now we need to generate a vector listing the object bases that
    // need to be locked to perform this query.  Note that we need to
    // get each of these bases locked at the same time to avoid potential
    // deadlock situations.  DBSession.openReadLock() will take care of
    // that for us by taking a vector to lock.

    // XXX need to revise this bit to try and create a list of bases
    // to lock by traversing over any QueryDeRefNodes in the QueryNode
    // tree.

    Vector baseLock = new Vector();

    baseLock.addElement(base);

    // lock the containing base as well, if it differs.. this will
    // keep things consistent

    if (debug)
      {
	System.err.println("Query: " + username + " : opening read lock on " + VectorUtils.vectorString(baseLock));
      }

    // okay.. now we want to lock the database, handle the search, and
    // return results.  We'll depend on the try..catch to handle
    // releasing the read lock if it is one we open.

    try
      {
	// with the new DBObjectBase.iterationSet support, we no
	// longer need to use a DBReadLock to lock the database unless
	// we are needing to do a query involving invid field
	// dereferencing as when doing a query that includes embedded
	// types.  If our baseLock vector only has one base, we can
	// just do a direct iteration over the base's iterationSet
	// snapshot, and avoid doing locking entirely.

	if (extantLock != null) 
	  {
	    // check to make sure that the lock we were passed in has everything
	    // locked that we'll need to examine.

	    if (!extantLock.isLocked(baseLock))	// *sync* DBStore
	      {
		throw new IllegalArgumentException(ts.l("queryDispatch.lock_exception"));
	      }

	    rLock = extantLock;
	  }
	else if (baseLock.size() > 1)
	  {
	    try
	      {
		rLock = session.openReadLock(baseLock);	// *sync* DBSession DBStore
	      }
	    catch (InterruptedException ex)
	      {
		setLastError("lock interrupted");
		return null;		// we're probably being booted off
	      }
	  }

	if (rLock != null)
	  {
	    if (debug)
	      {
		System.err.println("Query: " + username + " : got read lock");
	      }

	    en = base.objectTable.elements();
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Query: " + username + 
				   " : skipping read lock, iterating over iterationSet snapshot");
	      }

	    en = base.getIterationSet().elements();
	  }

	// iterate over the objects in the base we're searching on,
	// looking for matching objects.  Note that we need to check
	// in here to see if we've had our DBSession's logout() method
	// called.. this shouldn't really ever happen here due to
	// synchronization on GanymedeSession, but if somehow it does
	// happen, we want to go ahead and break out of our query.  We
	// could well have our lock revoked during execution
	// of a query, so we'll check that as well.

	while (loggedInSemaphore.isSet() && 
	       (rLock == null || session.isLocked(rLock)) && en.hasMoreElements())
	  {
	    obj = (DBObject) en.nextElement();

	    // if we're editing it, let's look at our version of it

	    if (obj.shadowObject != null && obj.shadowObject.getSession() == session)
	      {
		obj = obj.shadowObject;
	      }

	    if (DBQueryHandler.matches(this, query, obj))
	      {
		addResultRow(obj, query, result, internal, perspectiveObject);
	      }
	  }

	if (!loggedInSemaphore.isSet())
	  {
	    throw new RuntimeException(ts.l("queryDispatch.logged_out_exception"));
	  }

	if (rLock != null && !session.isLocked(rLock))
	  {
	    throw new RuntimeException(ts.l("queryDispatch.read_lock_exception"));
	  }

	if (debug)
	  {
	    System.err.println("Query: " + username + " : completed query over primary hash.");
	  }

	// find any objects created or being edited in the current
	// transaction that match our criteria that we didn't see before

	// note that we have to do this even though
	// DBSession.viewDBObject() will look in our transaction's
	// working set for us, as there may be newly created objects
	// that are not yet held in the database, so our loop over
	// iterationSet might have missed something.

	// that is, viewDBObject() above will provide the version of
	// the object in our transaction if it has been changed in the
	// transaction, but that doesn't mean that we will have seen
	// objects that haven't yet been integrated into the object
	// tables, so we check our transaction's working set here.

	if (session.isTransactionOpen()) // should be safe since we are sync'ed on GanymedeSession
	  {
	    if (debug)
	      {
		System.err.println("Query: " + username +
				   " : scanning intratransaction objects");
	      }

	    DBEditObject transactionObjects[] = session.editSet.getObjectList();

	    for (int i = 0; i < transactionObjects.length; i++)
	      {
		DBEditObject transaction_object = transactionObjects[i];

		// don't consider objects of the wrong type here.

		if (transaction_object.getTypeID() != base.getTypeID())
		  {
		    continue;
		  }

		// don't consider objects we already have stored in the result

		if (result.containsInvid(transaction_object.getInvid()))
		  {
		    continue;
		  }

		// don't show objects in our transaction that are
		// being deleted or dropped

		if (transaction_object.getStatus() == ObjectStatus.DELETING ||
		    transaction_object.getStatus() == ObjectStatus.DROPPING)
		  {
		    continue;
		  }

		if (DBQueryHandler.matches(this, query, transaction_object))
		  {
		    addResultRow(transaction_object, query, result, internal, perspectiveObject);
		  }
	      }

	    if (debug)
	      {
		System.err.println("Query: " + username + 
				   " : completed scanning intratransaction objects");
	      }
	  }
    
	if (debug)
	  {
	    Ganymede.debug("Query: " + username + ", object type " + 
			   base.getName() + " completed");
	  }

	return result;
      }
    finally
      {
	// no matter where we depart, make sure to release our locks if
	// we created them here.

	if (extantLock == null && rLock != null && rLock.isLocked())
	  {
	    session.releaseLock(rLock);	// *sync* DBSession DBStore
	  }
      }
  }

  /**
   * This private method takes care of adding an object to a query
   * result, checking permissions and what-not as needed.
   *
   * This method is not synchronized for performance reasons, but
   * is only to be called from methods synchronized on this
   * GanymedeSession. 
   *
   * @param obj The object to add to the query results
   * @param query The query that we are processing, used to get
   * the list of fields we're wanting to return
   * @param result The QueryResult we're building up
   * @param internal If true, we won't check permissions
   * @param perspectiveObject This is an object that can be consulted
   * to see what its
   * {@link arlut.csd.ganymede.server.DBObject#lookupLabel(arlut.csd.ganymede.server.DBObject) lookupLabel()}
   * method will return.  This can be null without harmful effect, but if
   * is it not null, a custom DBEditObject subclass can choose to present
   * the label of obj from its perspective.  This is used to simulate
   * a sort of relational effect for objects linked from the object
   * being added, by letting different fields in the object take on the
   * role of the label when seen from different objects.  Yes, it is a big ugly mess.
   */

  private void addResultRow(DBObject obj, Query query, 
			    QueryResult result, boolean internal,
			    DBEditObject perspectiveObject)
  {
    PermEntry perm;

    /* -- */

    // if the object we're looking at is being deleted or dropped,
    // we'll consider it an ex-object, and not include it in the query
    // results.

    if (obj instanceof DBEditObject)
      {
	DBEditObject eObj = (DBEditObject) obj;

	if (eObj.getStatus() == ObjectStatus.DELETING ||
	    eObj.getStatus() == ObjectStatus.DROPPING)
	  {
	    return;
	  }
      }

    if (supergashMode)
      {
	// we'll report it as editable

	perm = PermEntry.fullPerms;
      }
    else
      {
	perm = getPerm(obj);
	
	if (perm == null)
	  {
	    return;		// permissions prohibit us from adding this result
	  }

	if (query.editableOnly && !perm.isEditable())
	  {
	    return;		// permissions prohibit us from adding this result
	  }

	if (!perm.isVisible())
	  {
	    return;		// permissions prohibit us from adding this result
	  }
      }

    if (debug)
      {
	if (perspectiveObject == null)
	  {
	    Ganymede.debug("Query: " + username + " : adding element " +
			   obj.getLabel() + ", invid: " + obj.getInvid());
	  }
	else
	  {
	    Ganymede.debug("Query: " + username + " : adding element " +
			   perspectiveObject.lookupLabel(obj) + ", invid: " + obj.getInvid());
	  }
      }
    
    if (internal || !query.filtered || filterMatch(obj))
      {
	if (debug)
	  {
	    Ganymede.debug("not discounting out of hand");
	  }
	
	if (perspectiveObject == null)
	  {
	    if (debug)
	      {
                Ganymede.debug("not using perspective object");
	      }

	    if (obj.isEmbedded())
	      {
		result.addRow(obj.getInvid(), obj.getEmbeddedObjectDisplayLabel(),
			      obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			      perm.isEditable());
	      }
	    else
	      {
		result.addRow(obj.getInvid(), obj.getLabel(), 
			      obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			      perm.isEditable());
	      }
	  }
	else
	  {
	    if (debug)
	      {
		Ganymede.debug("using perspective object");
	      }

	    result.addRow(obj.getInvid(), perspectiveObject.lookupLabel(obj), 
			  obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			  perm.isEditable());
	  }
      }
  }

  /**
   * This method is intended as a lightweight way of returning the
   * current label of the specified invid.  No locking is done,
   * and the label returned will be viewed through the context
   * of the current transaction, if any.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */ 

  public String viewObjectLabel(Invid invid)
  {
    // We don't check permissions here, as we use session.viewDBObject().

    // We have made the command decision that finding the label for an
    // invid is not something we need to guard against.  Using
    // session.viewDBObject() here makes this a much more lightweight
    // operation.

    try
      {
	return session.viewDBObject(invid).getLabel();
      }
    catch (NullPointerException ex)
      {
	return null;
      }
  }

  /** 
   * This method is intended as a lightweight way of returning a
   * handy description of the type and label of the specified invid.
   * No locking is done, and the label returned will be viewed through
   * the context of the current transaction, if any. 
   */

  public String describe(Invid invid)
  {
    // We don't check permissions here, as we use session.viewDBObject().

    // We have made the command decision that finding the label for an
    // invid is not something we need to guard against.  Using
    // session.viewDBObject() here makes this a much more lightweight
    // operation.
    
    try
      {
	DBObject obj = session.viewDBObject(invid);

	return obj.getTypeName() + " " + obj.getLabel();
      }
    catch (NullPointerException ex)
      {
	return null;
      }
  }

  /**
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this time, or all events if this is null.
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since) throws NotLoggedInException
  {
    return viewObjectHistory(invid, since, null, true);
  }

  /**
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.
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
    return viewObjectHistory(invid, since, null, fullTransactions);
  }

  /**
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this time, or all events if this is null.
   * @param before Report events occuring on or before this time
   * @param fullTransactions If false, only events directly involving the requested
   * object will be included in the result buffer.
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since, Date before, boolean fullTransactions) throws NotLoggedInException
  {
    DBObject obj;

    /* -- */

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

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
        // "Can''t return history for an object that has been deleted or does not exist ({0})"
        setLastError(ts.l("viewObjectHistory.null_pointer", String.valueOf(invid)));
        return null;
      }

    if (!getPerm(obj).isVisible())
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
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.
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
    DBObject obj;

    /* -- */

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

    // we do our own permissions checking, so we can use session.viewDBObject().

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException(ts.l("viewAdminHistory.null_pointer", String.valueOf(invid)));
      }

    if (!getPerm(obj).isVisible())
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
   * View an object from the database.  The ReturnVal returned will
   * carry a {@link arlut.csd.ganymede.rmi.db_object db_object} reference,
   * which can be obtained by the client
   * calling {@link arlut.csd.ganymede.common.ReturnVal#getObject() ReturnVal.getObject()}.
   * If the object could not be
   * viewed for some reason, the ReturnVal will carry an encoded error
   * dialog for the client to display.
   *
   * view_db_object() can be done at any time, outside of the
   * bounds of any transaction.  view_db_object() returns a read-only
   * snapshot of the object's state at the time the view_db_object()
   * call is processed, and will be transaction-consistent
   * internally.
   *
   * If view_db_object() is called during a transaction, the object
   * will be returned as it stands during the transaction.. that is,
   * if the object has been changed during the transaction, a
   * read-only view of that changed object will be returned, even if
   * the transaction has not yet been committed, and other clients
   * would not be able to see that version of the object.
   *
   * NOTE: It is critical that any code that looks at the values of
   * fields in a {@link arlut.csd.ganymede.server.DBObject DBObject}
   * go through a view_db_object() method
   * or else the object will not properly know who owns it, which
   * is critical for it to be able to properly authenticate field
   * access.  Keep in mind, however, that view_db_object clones the
   * DBObject in question, so this method is very heavyweight.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  public synchronized ReturnVal view_db_object(Invid invid) throws NotLoggedInException
  {
    ReturnVal result;
    db_object objref;
    DBObject obj;

    /* -- */

    checklogin();

    // we'll let a NullPointerException be thrown if we were given a null
    // Invid.

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	return Ganymede.createErrorDialog(ts.l("view_db_object.no_object_error"),
					  ts.l("view_db_object.no_object_error_text", String.valueOf(invid)));
      }

    if (getPerm(obj).isVisible())
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

	objref = new DBObject(obj, this);

	result = new ReturnVal(true); // success

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
   * Check an object out from the database for editing.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling
   * {@link arlut.csd.ganymede.common.ReturnVal#getObject() ReturnVal.getObject()}.
   * If the object could not be checked out for editing for some
   * reason, the ReturnVal will carry an encoded error dialog for the
   * client to display.
   *
   * Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once checked out, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}
   * or {@link arlut.csd.ganymede.server.GanymedeSession#abortTransaction() abortTransaction()}.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  public synchronized ReturnVal edit_db_object(Invid invid) throws NotLoggedInException
  {
    ReturnVal result;
    db_object objref = null;
    DBObject obj;


    /* -- */

    checklogin();

    obj = session.viewDBObject(invid);

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

    if (getPerm(obj).isEditable())
      {
	if (!obj.isEmbedded())
	  {
	    setLastEvent("edit " + obj.getTypeName() + ":" + obj.getLabel());
	  }

	try
          {
            objref = session.editDBObject(invid);
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
	    result = new ReturnVal(true);
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
		    edit_username = editing.gSession.username;
		    edit_hostname = editing.gSession.clienthost;

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
   * Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
   *
   * @param objectType The kind of object to create.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal create_db_object(String objectType) throws NotLoggedInException
  {
    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
	// "Error, "{0}" is not a valid object type in this Ganymede server."
	return Ganymede.createErrorDialog(ts.l("global.no_such_object_type", objectType));
      }

    return this.create_db_object(base.getTypeID());
  }

  /**
   * Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
   *
   * @param type The kind of object to create.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public ReturnVal create_db_object(short type) throws NotLoggedInException
  {
    return this.create_db_object(type, false, null);
  }

  /**
   * Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
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
   * Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * Keep in mind that only one GanymedeSession can have a particular
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
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
    QueryResult ownerList = null;
    boolean randomOwner = false;
    Vector ownerInvids = null;

    /* -- */

    checklogin();
    
    if (!getPerm(type, true).isCreatable())
      {
	DBObjectBase base = Ganymede.db.getObjectBase(type);
	
	String error;

	if (base == null)
	  {
	    // "Permission to create object of *invalid* type {0} denied."
	    error = ts.l("create_db_object.invalid_type", new Integer(type));
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
	if (newObjectOwnerInvids != null)
	  {
	    ownerInvids = newObjectOwnerInvids;
	  }
	else
	  {
	    // supergash is allowed to create objects with no owners,
	    // so we won't worry about what supergash might try to do.

	    if (!supergashMode)
	      {
		ownerInvids = new Vector();
		ownerList = getOwnerGroups();
		
		if (ownerList.size() == 0)
		  {
		    // "Can''t Create Object"
		    // "Can''t create new object, no owner group to put it in."
		    return Ganymede.createErrorDialog(ts.l("create_db_object.cant_create"),
						      ts.l("create_db_object.no_owner_group"));
		  }
		
		// If we're interactive, the client really should have
		// helped us out by prompting the user for their
		// preferred default owner list, but if we are talking
		// to a custom client, this might not be the case, in
		// which case we'll just pick the first owner group we
		// can put it into and put it there.
                //
                // The client can always manually set the owner group
                // in a created object after we return it, of course.
		    
                ownerInvids.addElement(ownerList.getInvid(0));
		    
                if (ownerList.size() > 1)
                  {
                    randomOwner = true;
                  }
	      }
	  }
      }

    // now create and process

    try
      {
        retVal = session.createDBObject(type, preferredInvid, ownerInvids); // *sync* DBSession
        
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

    if (randomOwner)
      {
        // "Warning, picked an Owner Group at random"
        // "Warning: randomly selected owner group {1} to place new {0} object in."
        retVal = Ganymede.createInfoDialog(ts.l("create_db_object.random"),
                                           ts.l("create_db_object.random_text",
                                                newObj.getTypeName(),
                                                viewObjectLabel(ownerList.getInvid(0))));
      }
    else
      {
        retVal = new ReturnVal(true);
      }

    if (this.exportObjects)
      {
        exportObject(newObj);
      }

    retVal.setInvid(newObj.getInvid());
    retVal.setObject(newObj);
	    
    return retVal;
  }

  /**
   * Clone a new object from object &lt;invid&gt;.  The ReturnVal returned
   * will carry a db_object reference, which can be obtained by the
   * client calling ReturnVal.getObject().  If the object could not
   * be checked out for editing for some reason, the ReturnVal will
   * carry an encoded error dialog for the client to display.
   *
   * This method must be called within a transactional context.
   *
   * Typically, only certain values will be cloned.  What values are
   * retained is up to the specific code module provided for the
   * invid type of object.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *    
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal clone_db_object(Invid invid) throws NotLoggedInException
  {
    DBObject vObj;
    DBEditObject newObj;
    ReturnVal retVal;
    boolean checkpointed = false;

    /* -- */

    checklogin();

    if (invid == null)
      {
	return Ganymede.createErrorDialog(ts.l("clone_db_object.clone_error"),
					  ts.l("clone_db_object.clone_error_text"));
      }

    retVal = view_db_object(invid); // get a copy customized for per-field visibility

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }
    
    vObj = (DBObject) retVal.getObject();

    DBEditObject objectHook = Ganymede.db.getObjectBase(invid.getType()).getObjectHook();

    if (!objectHook.canClone(session, vObj))
      {
	// "Cloning DENIED"
	// "Cloning operation refused for {0} object {1}."
	return Ganymede.createErrorDialog(ts.l("clone_db_object.denied"),
					  ts.l("clone_db_object.denied_msg", vObj.getTypeName(), vObj.getLabel()));
      }

    String ckp = RandomUtils.getSaltedString("clone_db_object[" + invid.toString() + "]");

    session.checkpoint(ckp);
    checkpointed = true;

    try
      {
        retVal = create_db_object(invid.getType());
    
        if (!ReturnVal.didSucceed(retVal))
          {
            return retVal;
          }

        newObj = (DBEditObject) retVal.getObject();

        // the merge operation will do the right thing here and
        // preserve the encoded object and invid in the retVal for our
        // pass-through to the client, so long as the cloneFromObject
        // method succeeds.

        retVal = ReturnVal.merge(retVal, newObj.cloneFromObject(session, vObj, false));

        if (ReturnVal.didSucceed(retVal))
          {
            session.popCheckpoint(ckp);
            checkpointed = false;
          }

        return retVal;
      }
    finally
      {
        if (checkpointed)
          {
            session.rollback(ckp);
          }
      }
  }

  /**
   * Inactivate an object in the database
   *
   * This method must be called within a transactional context.  The object's
   * change in status will not be visible to other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
   *
   * Objects inactivated will typically be altered to reflect their inactive
   * status, but the object itself might not be purged from the Ganymede
   * server for a defined period of time, to allow other network systems
   * to have time to do accounting, clean up, etc., before a user id or
   * network address is re-used.
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized ReturnVal inactivate_db_object(Invid invid) throws NotLoggedInException
  {
    DBObject vObj;
    DBEditObject eObj;

    /* -- */

    checklogin();

    vObj = session.viewDBObject(invid);

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

    if (!getPerm(vObj).isDeletable())
      {
	setLastError(ts.l("inactivate_db_object.permission_text",
			  vObj.getTypeName(),
			  vObj.getLabel()));

	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.permission_text",
					       vObj.getTypeName(), vObj.getLabel()));
      }

    ReturnVal result = edit_db_object(invid); // *sync* DBSession DBObject

    eObj = (DBEditObject) result.getObject();

    if (eObj == null)
      {
	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.no_checkout",
					       vObj.getTypeName(), vObj.getLabel()));
      }

    if (!eObj.canBeInactivated() || !eObj.canInactivate(session, eObj))
      {
	return Ganymede.createErrorDialog(ts.l("inactivate_db_object.error"),
					  ts.l("inactivate_db_object.not_inactivatable", eObj.getLabel()));
      }

    setLastEvent("inactivate " + eObj.getTypeName() + ":" + eObj.getLabel());

    // note!  DBEditObject's finalizeInactivate() method does the
    // event logging

    return session.inactivateDBObject(eObj); // *sync* DBSession 
  }

  /**
   * Reactivates an inactivated object in the database
   *
   * This method is only applicable to inactivated objects.  For such,
   * the object will be reactivated if possible, and the removal date
   * will be cleared.  The object may retain an expiration date,
   * however.
   *
   * The client should check the returned ReturnVal's
   * {@link arlut.csd.ganymede.common.ReturnVal#getObjectStatus() getObjectStatus()}
   * method to see whether the re-activated object has an expiration date set.
   *
   * This method must be called within a transactional context.  The object's
   * change in status will not be visible to other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
   *
   * @see arlut.csd.ganymede.rmi.Session
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal reactivate_db_object(Invid invid) throws NotLoggedInException
  {
    DBObject vObj;
    DBEditObject eObj;

    /* -- */

    checklogin();

    vObj = session.viewDBObject(invid);

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

    if (!getPerm(vObj).isDeletable())
      {
	return Ganymede.createErrorDialog(ts.l("reactivate_db_object.error"),
					  ts.l("reactivate_db_object.permission_text",
					       vObj.getTypeName(),
					       vObj.getLabel()));
      }

    ReturnVal result = edit_db_object(invid); // *sync* DBSession DBObject

    eObj = (DBEditObject) result.getObject();

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

    return session.reactivateDBObject(eObj); // *sync* DBSession, DBObject possible
  }

  /**
   * Remove an object from the database
   *
   * This method must be called within a transactional context.
   *
   * Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   * Any problems with permissions to remove this object will result
   * in a dialog being returned in the ReturnVal.
   *
   * This method must be called within a transactional context.  The object's
   * removal will not be visible to other sessions until this session calls 
   * {@link arlut.csd.ganymede.server.GanymedeSession#commitTransaction() commitTransaction()}.
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
    DBObject vObj = session.viewDBObject(invid);

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
	if (!getPerm(vObj).isDeletable())
	  {
	    return Ganymede.createErrorDialog(ts.l("remove_db_object.error"),
					      ts.l("remove_db_object.permission_text",
						   vObj.getTypeName(),
						   vObj.getLabel()));
	  }

	ReturnVal retVal = objBase.getObjectHook().canRemove(session, vObj);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (retVal.getDialog() != null)
	      {
		return retVal;
	      }

	    // if an object type can be inactivated, then it *must* be
	    // inactivated, unless the user is supergash

	    if (!isSuperGash() && objBase.getObjectHook().canBeInactivated())
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
    
    return session.deleteDBObject(invid);
  }

  /**
   * This method is called by the XML client to initiate a dump of
   * Ganymede objects in XML format matching the GanyQL search
   * criteria specified in the queryString.  The ReturnVal returned
   * will, if the operation is approved, contain a reference to an RMI
   * FileTransmitter interface, which can be iteratively called by the
   * XML client to pull pieces of the transmission down in sequence.
   */

  public ReturnVal runXMLQuery(String queryString) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    GanyQueryTransmuter transmuter = new GanyQueryTransmuter();
    Query query = transmuter.transmuteQueryString(queryString);

    // get a simple list of matching invids without bothering to do
    // transport setup.

    QueryResult rows = this.queryDispatch(query, false, false, null, null);

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
   * This method is called by the XML client to initiate a dump of
   * Ganymede objects in XML format matching the search criteria
   * specified in the query object.  The ReturnVal returned will, if
   * the operation is approved, contain a reference to an RMI
   * FileTransmitter interface, which can be iteratively called by the
   * XML client to pull pieces of the transmission down in sequence.
   */

  public ReturnVal runXMLQuery(Query query) throws NotLoggedInException, GanyParseException
  {
    checklogin();

    // get a simple list of matching invids without bothering to do
    // transport setup.

    QueryResult rows = this.queryDispatch(query, false, false, null, null);

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
   * This method is called by the XML client to initiate a dump of
   * the server's schema definition in XML format.  The ReturnVal
   * returned will, if the operation is approved, contain a reference
   * to an RMI FileTransmitter interface, which can be iteratively
   * called by the XML client to pull pieces of the transmission down
   * in sequence.
   *
   * This method is only available to a supergash-privileged
   * GanymedeSession.
   */

  public ReturnVal getSchemaXML() throws NotLoggedInException
  {
    return this.getXML(false, true, null, false, false);
  }

  /**
   * This method is called by the XML client to initiate a dump of
   * the entire data contents of the server.  The ReturnVal returned
   * will, if the operation is approved, contain a reference to
   * an RMI FileTransmitter interface, which can be iteratively called
   * by the XML client to pull pieces of the transmission down in
   * sequence.
   *
   * This method is only available to a supergash-privileged
   * GanymedeSession. 
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
   */

  public ReturnVal getDataXML(String syncChannel, boolean includeHistory, boolean includeOid) throws NotLoggedInException
  {
    Ganymede.debug("GanymedeSession.getDataXML(" + syncChannel + ")");
    return this.getXML(true, false, syncChannel, includeHistory, includeOid);
  }

  /**
   * This method is called by the XML client to initiate a dump of
   * the server's entire database, schema and data, in XML format.
   * The ReturnVal will, if the operation is approved, contain a
   * reference to an RMI FileTransmitter interface, which can be
   * iteratively called by the XML client to pull pieces of the
   * transmission down in sequence.
   *
   * This method is only available to a supergash-privileged
   * GanymedeSession.
   *
   * @param includeHistory If true, the historical fields (creation
   * date & info, last modification date & info) will be included in
   * the xml stream.
   * @param includeOid If true, the objects written out to the xml
   * stream will include an "oid" attribute which contains the precise
   * Invid of the object.
   */

  public ReturnVal getXMLDump(boolean includeHistory, boolean includeOid) throws NotLoggedInException
  {
    return this.getXML(true, true, null, includeHistory, includeOid);
  }

  /**
   * Private server-side helper method used to pass a {@link
   * arlut.csd.ganymede.rmi.FileTransmitter FileTransmitter} reference
   * back that can be called to pull pieces of an XML
   * transmission.
   */

  private ReturnVal getXML(boolean sendData, boolean sendSchema, String syncChannel, boolean includeHistory, boolean includeOid) throws NotLoggedInException
  {
    checklogin();

    if (!supergashMode)
      {
	String message;

	if (sendData)
	  {
	    // "You do not have permissions to dump the server''s data with the xml client"
	    message = ts.l("getXML.data_refused");
	  }
	else
	  {
	    // "You do not have permissions to dump the server''s schema definition with the xml client"
	    message = ts.l("getXML.schema_refused");
	  }

	return Ganymede.createErrorDialog(ts.l("global.permissions_error"),
					  message);
      }

    XMLTransmitter transmitter = null;

    try
      {
	transmitter = new XMLTransmitter(sendData, sendSchema, syncChannel, includeHistory, includeOid);
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog(ts.l("getXML.transmitter_error"),
					  ts.l("getXML.transmitter_error_msg", ex.getMessage()));
      }

    ReturnVal retVal = new ReturnVal(true);
    retVal.setFileTransmitter(transmitter);
    return retVal;
  }

  /****************************************************************************
   *                                                                          *
   * From here on down, the methods are not remotely accessible to the client,*
   * but are instead for server-side use only.                                *
   *                                                                          *
   ****************************************************************************/

  /**
   *
   * Convenience method to get access to this session's UserBase
   * instance.
   *
   */

  DBObject getUser()
  {
    if (userInvid != null)
      {
	// okay to use session.viewDBObject() here, because getUser()
	// is only used for internal purposes, and we don't need or
	// want to do permissions checking

	return session.viewDBObject(userInvid);
      }
    
    return null;
  }

  /**
   * This is a method to allow code in the server to quickly and
   * safely get a full list of objects in an object base.
   *
   * This is only a server-side method.  getObjects() does
   * not do anything to check access permissions.
   *
   * It is the responsiblity of the code that gets a Vector
   * back from this method not to modify the Vector returned
   * in any way, as it may be shared by other threads.
   *
   * Any objects returned by getObjects() will reflect the
   * state of that object in this session's transaction, if a
   * transaction is open.
   *
   * @return a Vector of DBObject references.
   */

  public synchronized Vector getObjects(short baseid) throws NotLoggedInException
  {
    DBObjectBase base;

    /* -- */

    checklogin();

    base = Ganymede.db.getObjectBase(baseid);

    if (base == null)
      {
	try
	  {
	    throw new RuntimeException(ts.l("getObjects.no_base", new Integer(baseid)));
	  }
	catch (RuntimeException ex)
	  {
	    Ganymede.debug(Ganymede.stackTrace(ex));
	    return null;
	  }
      }

    if (!session.isTransactionOpen())
      {
	// return a snapshot reference to the base's iteration set

	return base.getIterationSet();
      }
    else
      {
	Vector iterationSet;
	Map objects;

	// grab a snapshot reference to the vector of objects
	// checked into the database

	iterationSet = base.getIterationSet();

	// grab a snapshot copy of the objects checked out in this transaction
	
	objects = session.editSet.getObjectHashClone();
	
	// and generate our list

	Vector results = new Vector(iterationSet.size(), 100);

	// optimize this loop a bit

	int setSize = iterationSet.size();
	
	for (int i = 0; i < setSize; i++)
	  {
	    DBObject obj = (DBObject) iterationSet.elementAt(i);
	    
	    if (objects.containsKey(obj.getInvid()))
	      {
		results.addElement(objects.get(obj.getInvid()));
	      }
	    else
	      {
		results.addElement(obj);
	      }
	  }

	// drop our reference to the iterationSet

	iterationSet = null;
	
	// we've recorded any objects that are in the database.. now
	// look to see if there are any objects that are newly created
	// in our transaction's object list and add them as well.
	    
	Iterator iter = objects.values().iterator();
	    
	while (iter.hasNext())
	  {
	    DBEditObject eObj = (DBEditObject) iter.next();
	    
	    if ((eObj.getStatus() == ObjectStatus.CREATING) && (eObj.getTypeID()==baseid))
	      {
		results.addElement(eObj);
	      }
	  }
	
	return results;
      }
  }

  /**
   *
   * Convenience method to get access to this session's user invid.
   *
   */

  public Invid getUserInvid()
  {
    return userInvid;
  }

  /**
   *
   * Convenience method to get access to this session's persona invid.
   *
   */

  public Invid getPersonaInvid()
  {
    return personaInvid;
  }

  // **
  // the following are the non-exported permissions management
  // **

  /**
   *
   * This method finds the ultimate owner of an embedded object
   *
   */

  DBObject getContainingObj(DBObject object)
  {
    return session.getContainingObj(object);
  }

  /**
   * This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the object.  getPerm() will OR any proprietary
   * ownership bits with the default permissions to give
   * an appopriate result.
   */

  public PermEntry getPerm(DBObject object)
  {
    boolean doDebug = permsdebug && object.getInvid().getType() == 267;
    boolean useSelfPerm = false;
    PermEntry result;

    /* -- */

    if (object == null)
      {
	return null;
      }

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    // find the top-level object if we were passed an embedded object

    if (doDebug)
      {
	System.err.println("GanymedeSession.getPerm(" + object + ")");
      }
    
    object = getContainingObj(object);

    // does this object type have an override?

    result = object.getBase().getObjectHook().permOverride(this, object);

    if (result != null)
      {
	if (doDebug)
	  {
	    System.err.println("getPerm(): found an object override, returning " + result);
	  }

	return result;
      }

    // no override.. do we have an expansion?

    result = object.getBase().getObjectHook().permExpand(this, object);

    if (result == null)
      {
	result = PermEntry.noPerms;
      }

    // make sure we have personaPerms up to date

    updatePerms(false);		// *sync*

    // if we are operating on behalf of an end user and the object in
    // question happens to be that user's record, we'll act as if we
    // own ourself.  We'll then wind up with the default permission
    // object's objects owned privs.

    useSelfPerm = (userInvid != null) && userInvid.equals(object.getInvid());

    // If we aren't editing ourselves, go ahead and check to see
    // whether the custom logic for this object type wants to grant
    // ownership of this object.

    if (!useSelfPerm && object.getBase().getObjectHook().grantOwnership(this, object))
      {
	if (doDebug)
	  {
	    System.err.println("getPerm(): grantOwnership() returned true");
	  }

	useSelfPerm = true;
      }

    // If the current persona owns the object, look to our
    // personaPerms to get the permissions applicable, else
    // look at the default perms

    if (useSelfPerm || personaMatch(object))
      {
	if (doDebug)
	  {
	    System.err.println("getPerm(): personaMatch or useSelfPerm returned true");
	  }

	PermEntry temp = personaPerms.getPerm(object.getTypeID());

	if (doDebug)
	  {
	    System.err.println("getPerm(): personaPerms.getPerm(" + object + ") returned " + temp);

	    System.err.println("%%% Printing PersonaPerms");
	    PermissionMatrixDBField.debugdump(personaPerms);
	  }

	PermEntry val = result.union(temp);

	if (doDebug)
	  {
	    System.err.println("getPerm(): returning " + val);
	  }

	return val;
      }
    else
      {
	if (doDebug)
	  {
	    System.err.println("getPerm(): personaMatch and useSelfPerm returned false");
	  }

	PermEntry temp = defaultPerms.getPerm(object.getTypeID());

	if (doDebug)
	  {
	    System.err.println("getPerm(): defaultPerms.getPerm(" + object + ") returned " + temp);

	    System.err.println("%%% Printing DefaultPerms");
	    PermissionMatrixDBField.debugdump(defaultPerms);
	  }

	PermEntry val = result.union(temp);

	if (doDebug)
	  {
	    System.err.println("getPerm(): returning " + val);
	  }

	return val;
      }
  }

  /**
   * This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the field in the object.
   *
   * This method duplicates the logic of {@link
   * arlut.csd.ganymede.server.GanymedeSession#getPerm(arlut.csd.ganymede.server.DBObject)
   * getPerm(object)} internally for efficiency.  This method is
   * called <B>quite</B> a lot in the server, and has been tuned
   * to use the pre-calculated GanymedeSession
   * {@link arlut.csd.ganymede.server.GanymedeSession#defaultPerms defaultPerms}
   * and {@link arlut.csd.ganymede.server.GanymedeSession#personaPerms personaPerms}
   * objects which cache the effective permissions for fields in the
   * Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore} for the current
   * persona.
   */

  public PermEntry getPerm(DBObject object, short fieldId)
  {
    // if this is true, the object was considered to be owned.

    boolean objectIsOwned = false;

    // reference to which PermMatrix we use to look up permissions..
    // that for objects we own, or that for objects we don't.

    PermMatrix applicablePerms = null;

    // reference to custom pseudostatic DBEditObject handler
    // registered with the object's type, if any

    DBEditObject objectHook;

    // object permissions resulting from DBEditObject subclass
    // customization

    PermEntry overrideObjPerm = null;
    PermEntry expandObjPerm = null;

    // field permissions resulting from DBEditObject subclass
    // customization

    PermEntry overrideFieldPerm = null;
    PermEntry expandFieldPerm = null;

    // and our results

    PermEntry objectPerm = null;
    PermEntry fieldPerm = null;

    /* -- */

    if (permsdebug)
      {
	System.err.println("Entering GanymedeSession.getPerm(" + object + "," + fieldId + ")");
      }

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    objectHook = object.getBase().getObjectHook();

    // check for permissions overrides or expansions from the object's
    // custom plug-in class.. all of these objectHook calls will
    // return null if there is no customization

    overrideFieldPerm = objectHook.permOverride(this, object, fieldId);

    if (overrideFieldPerm == null)
      {
	expandFieldPerm = objectHook.permExpand(this, object, fieldId);
      }

    overrideObjPerm = objectHook.permOverride(this, object);

    if (overrideObjPerm == null)
      {
	expandObjPerm = objectHook.permExpand(this, object);
      }

    // make sure we have personaPerms up to date

    updatePerms(false);		// *sync*

    // embedded object ownership is determined by the top-level object
    
    DBObject containingObj = getContainingObj(object);

    if ((userInvid != null && userInvid.equals(containingObj.getInvid())) ||
	objectHook.grantOwnership(this, object) || 
	objectHook.grantOwnership(this, containingObj) ||
	personaMatch(containingObj))
      {
	if (permsdebug)
	  {
	    System.err.println("GanymedeSession.getPerm(" + object + "," + fieldId + ") choosing persona perms");
	  }

	objectIsOwned = true;

	applicablePerms = personaPerms;	// superset of defaultPerms
      }
    else
      {
	if (permsdebug)
	  {
	    System.err.println("GanymedeSession.getPerm(" + object + "," + fieldId + ") choosing default perms");
	  }

	applicablePerms = defaultPerms;
      }

    if (overrideObjPerm != null)
      {
	objectPerm = overrideObjPerm;
      }
    else
      {
	objectPerm = applicablePerms.getPerm(object.getTypeID());

	if (objectPerm == null)
	  {
	    if (permsdebug)
	      {
		System.err.println("GanymedeSession.getPerm(" + object + "," + fieldId + ") found no object perm");
	      }

	    objectPerm = PermEntry.noPerms;
	  }
	
	objectPerm = objectPerm.union(expandObjPerm);
      }
    
    if (overrideFieldPerm != null)
      {
	if (permsdebug)
	  {
	    System.err.println("GanymedeSession.getPerm(" + object + "," + fieldId + ") returning override perm");
	  }

	// allow field create perm even if they don't have object create perm

	PermEntry temp = overrideFieldPerm.intersection(objectPerm);

	// add back the create bit if the field is creatable

	if (overrideFieldPerm.isCreatable())
	  {
	    temp = temp.union(PermEntry.getPermEntry(false, false, true, false));
	  }
	
	return temp;
      }
    
    fieldPerm = applicablePerms.getPerm(object.getTypeID(), fieldId);

    // if we don't have an explicit permissions entry for the field,
    // return the effective one for the object.
    
    if (fieldPerm == null)
      {
	if (permsdebug)
	  {
	    System.err.println("GanymedeSession.getPerm(" + object + "," + fieldId + ") returning object perms");
	  }

	// if we are returning permissions for the owner list field
	// and the object in question has not been granted owner ship
	// privileges, make sure that we don't allow editing of the
	// owner list field, which could be used to make the object
	// owned, and thus gain privileges

	// likewise, we don't want to allow non-privileged end users
	// to edit the owner list field at all.

	if (fieldId == SchemaConstants.OwnerListField &&
	    (!objectIsOwned || personaObj == null))
	  {
	    return objectPerm.intersection(PermEntry.viewPerms);
	  }

	// nor do we want anyone to be able to modify the historical
	// fields

	if (fieldId == SchemaConstants.CreationDateField ||
	    fieldId == SchemaConstants.CreatorField ||
	    fieldId == SchemaConstants.ModificationDateField ||
	    fieldId == SchemaConstants.ModifierField)
	  {
	    return objectPerm.intersection(PermEntry.viewPerms);
	  }

	return objectPerm;
      }

    // we want to return the more restrictive permissions of the
    // object's permissions and the field's permissions.. we can never
    // look at a field in an object we can't look at.

    if (permsdebug)
      {
	System.err.println("GanymedeSession.getPerm(" + object + "," + fieldId + ") returning field perms");

	System.err.println("fieldPerm = " + fieldPerm);
	System.err.println("objectPerm = " + objectPerm);
	System.err.println("expandFieldPerm = " + expandFieldPerm);
      }

    // we never want to allow users who don't own an object to edit
    // the object ownership list, nor do we ever want to allow
    // non-privileged end users to edit the ownership list.

    // nor do we allow editing the historical fields

    if ((fieldId == SchemaConstants.OwnerListField &&
	(!objectIsOwned || personaObj == null)) ||
	(fieldId == SchemaConstants.CreationDateField ||
	 fieldId == SchemaConstants.CreatorField ||
	 fieldId == SchemaConstants.ModificationDateField ||
	 fieldId == SchemaConstants.ModifierField))
      {
	return fieldPerm.union(expandFieldPerm).intersection(objectPerm).intersection(PermEntry.viewPerms);
      }
    else
      {
	// allow field create perm even if they don't have object create perm

	PermEntry temp = fieldPerm.union(expandFieldPerm);
	PermEntry temp2 = temp.intersection(objectPerm);

	// add back the create bit if the field is creatable

	if (temp.isCreatable())
	  {
	    temp = temp2.union(PermEntry.getPermEntry(false, false, true, false));
	  }
	
	return temp;
      }
  }

  /**
   * This method returns the generic permissions for a object type.
   * This is currently used primarily to check to see whether a user
   * has privileges to create an object of a specific type.
   *
   * This method takes the administrator's current persona's set of
   * appropriate permission matrices, does a binary OR'ing of the
   * permission bits for the given base, and returns the effective
   * permission entry.
   * 
   * @param includeOwnedPerms If true, this method will return the
   * permission that the current persona would have for an object that
   * was owned by the current persona.  If false, this method will
   * return the default permissions that apply to objects not owned by
   * the persona.
   */

  PermEntry getPerm(short baseID, boolean includeOwnedPerms)
  {
    PermEntry result;

    /* -- */

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    updatePerms(false); // *sync* make sure we have personaPerms up to date

    // note that we can use personaPerms, since the persona's
    // base type privileges apply generically to objects of the
    // given type

    if (includeOwnedPerms)
      {
	result = personaPerms.getPerm(baseID);
      }
    else
      {
	result = defaultPerms.getPerm(baseID);
      }

    if (result == null)
      {
	return PermEntry.noPerms;
      }
    else
      {
	return result;
      }
  }

  /**
   * This method returns the current persona's default permissions for
   * a base and field.  This permission applies generically to objects
   * that are not owned by this persona and to objects that are
   * owned.
   *
   * This is used by the
   * {@link arlut.csd.ganymede.server.GanymedeSession#dump(arlut.csd.ganymede.common.Query) dump()} 
   * code to determine whether a field should
   * be added to the set of possible fields to be returned at the
   * time that the dump results are being prepared.
   *
   * @param includeOwnedPerms If true, this method will return the permission
   * that the current persona would have for an object that was owned
   * by the current persona.  If false, this method will return the default
   * permissions that apply to objects not owned by the persona.
   */

  PermEntry getPerm(short baseID, short fieldID, boolean includeOwnedPerms)
  {
    PermEntry 
      result = null;

    /* -- */

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    // make sure we have defaultPerms and personaPerms up to date

    updatePerms(false);		// *sync*

    // remember that personaPerms is a permissive superset of
    // defaultPerms

    if (includeOwnedPerms)
      {
	if (personaPerms != null)
	  {
	    result = personaPerms.getPerm(baseID, fieldID);
 	
	    // if we don't have a specific permissions entry for
	    // this field, inherit the one for the base	
	
	    if (result == null)
	      {
		result = personaPerms.getPerm(baseID);
	      }
	  }
      }
    else
      {
	result = defaultPerms.getPerm(baseID, fieldID);

	// if we don't have a specific permissions entry for
	// this field, inherit the one for the base

	if (result == null)
	  {
	    result = defaultPerms.getPerm(baseID);
	  }
      }

    if (result == null)
      {
	return PermEntry.noPerms;
      }
    else
      {
	return result;
      }
  }

  /**
   * This convenience method resets defaultPerms from the default
   * permission object in the Ganymede database.
   */

  private void resetDefaultPerms()
  {
    PermissionMatrixDBField pField;

    /* -- */

    pField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.RoleDefaultMatrix);

    if (pField == null)
      {
	defaultPerms = new PermMatrix();
	delegatableDefaultPerms = new PermMatrix();
      }
    else
      {
	defaultPerms = pField.getMatrix();

	// default permissions are implicitly delegatable

	delegatableDefaultPerms = pField.getMatrix();
      }
  }

  /**
   * This non-exported method is used to generate a comprehensive permissions
   * matrix that applies to all objects owned by the active persona for this
   * user.
   *
   * This method is synchronized, and a whole lot of operations in the server
   * need to pass through here to ensure that the effective permissions for this
   * session haven't changed.  This method is designed to return very quickly
   * if permissions have not changed and forceUpdate is false.
   *
   * @param forceUpdate If false, updatePerms() will do nothing if the Ganymede
   * permissions database has not been changed since updatePerms() was last
   * called in this GanymedeSession.
   */

  public synchronized void updatePerms(boolean forceUpdate)
  { 
    PermissionMatrixDBField permField;

    /* -- */

    if (forceUpdate)
      {
	// clear our time stamp to force an update further on

	personaTimeStamp = null;

	if (permsdebug)
	  {
	    System.err.println("updatePerms(true)");
	  }
      }
    else
      {
	if (permsdebug)
	  {
	    System.err.println("updatePerms(false)");
	  }
      }

    if (permBase == null)
      {
	permBase = Ganymede.db.getObjectBase(SchemaConstants.RoleBase);
      }

    // first, make sure we have a copy of our default role
    // DBObject.. permTimeStamp is used to track this.

    if (permTimeStamp == null || !permTimeStamp.before(permBase.lastChange))
      {
	defaultObj = session.viewDBObject(SchemaConstants.RoleBase,
					  SchemaConstants.RoleDefaultObj);

	if (defaultObj == null)
	  {
	    if (!Ganymede.firstrun)
	      {
		Ganymede.debug(ts.l("updatePerms.no_default_perms"));
		throw new RuntimeException(ts.l("updatePerms.no_default_perms"));
	      }
	    else
	      {
		// we're loading the database with a bulk-loader
		// linked to the server code.  Don't bother with
		// permissions

		supergashMode = true;
		return;
	      }
	  }

	// remember we update this so we don't need to do it again
	
	if (permTimeStamp == null)
	  {
	    permTimeStamp = new Date();
	  }
	else
	  {
	    permTimeStamp.setTime(System.currentTimeMillis());
	  }
      }
   
    if (personaBase == null)
      {
	personaBase = Ganymede.db.getObjectBase(SchemaConstants.PersonaBase);
      }

    // here's where we break out if nothing needs to be updated.. note
    // that we are testing personaTimeStamp here, not permTimeStamp.
 
    if (personaTimeStamp != null && personaTimeStamp.after(personaBase.lastChange))
      {
	return;
      }

    if (permsdebug)
      {
	System.err.println("GanymedeSession.updatePerms(): doing full permissions recalc for " + 
			   (personaName == null ? username : personaName));
      }

    // persona invid may well have changed since we last loaded
    // personaInvid.. thus, we have to set it here.  Setting
    // personaObj is one of the primary reasons that other parts of
    // GanymedeSession call updatePerms(), so don't mess with this. I
    // tried it, believe me, it didn't work.

    if (personaInvid != null)
      {
	personaObj = session.viewDBObject(personaInvid);

	// if this session is editing the personaObj at the moment,
	// let's make a point of getting the version that isn't
	// checked out for editing so we don't risk inter-thread
	// interactions below

	if (personaObj instanceof DBEditObject)
	  {
	    personaObj = ((DBEditObject) personaObj).getOriginal();
	  }
      }
    else
      {
	personaObj = null;
      }

    // if we're not locked into supergash mode (for internal sessions,
    // etc.), lets find out whether we're in supergash mode currently
    
    if (!beforeversupergash)
      {
	supergashMode = false;

	// ok, we're not supergash.. or at least, we might not be.  If
	// we are not currently active as a persona, personaPerms will
	// just be our defaultPerms

	if (personaObj == null)
	  {
	    // ok, we're not only not supergash, but we're also not
	    // even a privileged persona.  Load defaultPerms and
	    // personaPerms with the two permission matrices from the
	    // default permission object.

	    PermMatrix selfPerm = null;

	    /* -- */

	    resetDefaultPerms();

	    permField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.RoleMatrix);

	    if (permField == null)
	      {
		selfPerm = new PermMatrix();
	      }
	    else
	      {
		// selfPerm is the permissions that the default
		// permission object has for objects owned.

		selfPerm = permField.getMatrix();
		
		if (selfPerm == null)
		  {
		    System.err.println(ts.l("updatePerms.null_selfperm"));
		  }
	      }

	    // personaPerms starts off as the union of permissions
	    // applicable to all objects and all objects owned, from
	    // the default permissions object.

	    personaPerms = new PermMatrix(defaultPerms).union(selfPerm);
	    delegatablePersonaPerms = new PermMatrix(defaultPerms).union(selfPerm);

	    if (permsdebug)
	      {
		System.err.println("GanymedeSession.updatePerms(): returning.. no persona obj for " + 
				   (personaName == null ? username : personaName));
	      }

	    // remember the last time we pulled personaPerms / defaultPerms

	    if (personaTimeStamp == null)
	      {
		personaTimeStamp = new Date();
	      }
	    else
	      {
		personaTimeStamp.setTime(System.currentTimeMillis());
	      }
	    
	    return;
	  }
	else
	  {
	    if (permsdebug)
	      {
		System.err.println("updatePerms(): calculating new personaPerms");;
	      }

	    InvidDBField idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaGroupsField);
	    Invid inv;
		
	    if (idbf != null)
	      {
		Vector vals = idbf.getValuesLocal();

		// *** Caution!  getValuesLocal() does not clone the
		// field's contents, and neither do we, for speed
		// reasons.  Since we get personaObj using
		// viewDBObject(), and we made sure that we got a
		// non-editable view, there's no chance that this loop
		// will get trashed by another thread messing with
		// vals.

		// DO NOT modify vals here!

		// loop over the owner groups this persona is a member
		// of, see if it includes the supergash owner group
		    
		// it's okay to loop on this field since we should be looking
		// at a DBObject and not a DBEditObject

		for (int i = 0; i < vals.size(); i++)
		  {
		    inv = (Invid) vals.elementAt(i);
			
		    if (inv.getNum() == SchemaConstants.OwnerSupergash)
		      {
			supergashMode = true;
			break;
		      }
		  }
	      }

	    if (!supergashMode)
	      {
		// since we're not in supergash mode, we need to take
		// into account the operational privileges granted us
		// by the default permission matrix and all the
		// permission matrices associated with this persona.
		// Calculate the union of all of the applicable
		// permission matrices.

		// make sure that defaultPerms is reset to the
		// baseline, and initialize personaPerms from it.

		resetDefaultPerms();

		// Personas do not get the default 'objects-owned'
		// privileges for the wider range of objects under
		// their ownership.  Any special privileges granted to
		// admins over objects owned by them must be derived
		// from a non-default role.

		// they do get the default permissions that all users have
		// for non-owned objects, though.

		personaPerms = new PermMatrix(defaultPerms);

		// default permissions on non-owned are implicitly delegatable.

		delegatablePersonaPerms = new PermMatrix(defaultPerms);

		// now we loop over all permissions objects referenced
		// by our persona, or'ing in both the objects owned
		// permissions and default permissions to augment defaultPerms
		// and personaPerms.

		idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaPrivs);

		if (idbf != null)
		  {
		    Vector vals = idbf.getValuesLocal();

		    // *** Caution!  getValuesLocal() does not clone the field's contents..
		    // 
		    // DO NOT modify vals here!

		    PermissionMatrixDBField pmdbf, pmdbf2;
		    Hashtable pmdbfMatrix1 = null, pmdbfMatrix2 = null;
		    DBObject pObj;

		    /* -- */
		    
		    // it's okay to loop on this field since we should be looking
		    // at a DBObject and not a DBEditObject

		    for (int i = 0; i < vals.size(); i++)
		      {
			inv = (Invid) vals.elementAt(i);
			    
			pObj = session.viewDBObject(inv);
			
			if (pObj != null)
			  {
			    if (permsdebug)
			      {
				System.err.println("updatePerms(): unioning " + pObj + " into personaPerms and defaultPerms");

				System.err.println("personaPerms is currently:");

				PermissionMatrixDBField.debugdump(personaPerms);
			      }

			    // The default permissions for this
			    // administrator consists of the union of
			    // all default perms fields in all
			    // permission matrices for this admin
			    // persona.

			    // personaPerms is the union of all
			    // permissions applicable to objects that
			    // are owned by this persona

			    pmdbf = (PermissionMatrixDBField) pObj.getField(SchemaConstants.RoleMatrix);

			    if (pmdbf != null)
			      {
				pmdbfMatrix1 = pmdbf.matrix;
			      }

			    pmdbf2 = (PermissionMatrixDBField) pObj.getField(SchemaConstants.RoleDefaultMatrix);

			    if (pmdbf2 != null)
			      {
				pmdbfMatrix2 = pmdbf2.matrix;
			      }

			    if (permsdebug)
			      {
				PermMatrix pm = new PermMatrix(pmdbfMatrix1);

				System.err.println("updatePerms(): RoleMatrix for " + pObj + ":");

				PermissionMatrixDBField.debugdump(pm);

				pm = new PermMatrix(pmdbfMatrix2);

				System.err.println("updatePerms(): RoleDefaultMatrix for " + pObj + ":");

				PermissionMatrixDBField.debugdump(pm);
			      }

			    personaPerms = personaPerms.union(pmdbfMatrix1);

			    if (permsdebug)
			      {
				System.err.println("updatePerms(): personaPerms after unioning with RoleMatrix is");

				PermissionMatrixDBField.debugdump(personaPerms);
			      }

			    personaPerms = personaPerms.union(pmdbfMatrix2);

			    if (permsdebug)
			      {
				System.err.println("updatePerms(): personaPerms after unioning with RoleDefaultMatrix is");

				PermissionMatrixDBField.debugdump(personaPerms);
			      }

			    defaultPerms = defaultPerms.union(pmdbfMatrix2);

			    // we want to maintain our notion of
			    // delegatable permissions separately..

			    Boolean delegatable = (Boolean) pObj.getFieldValueLocal(SchemaConstants.RoleDelegatable);

			    if (delegatable != null && delegatable.booleanValue())
			      {
				delegatablePersonaPerms = delegatablePersonaPerms.union(pmdbfMatrix1).union(pmdbfMatrix2);
				delegatableDefaultPerms = delegatableDefaultPerms.union(pmdbfMatrix2);
			      }
			  }
		      }
		  }
	      }
	  }
      }

    // remember the last time we pulled personaPerms / defaultPerms

    if (personaTimeStamp == null)
      {
	personaTimeStamp = new Date();
      }
    else
      {
	personaTimeStamp.setTime(System.currentTimeMillis());
      }

    if (permsdebug)
      {
	System.err.println("GanymedeSession.updatePerms(): finished full permissions recalc for " + 
			   (personaName == null ? username : personaName));

	System.err.println("personaPerms = \n\n" + personaPerms);
	System.err.println("\n\ndefaultPerms = \n\n" + defaultPerms);
      }
    
    return;
  }

  /**
   * This method gives access to the DBObject for the administrator's
   * persona record, if any.  This is used by
   * {@link arlut.csd.ganymede.server.DBSession DBSession} to get the
   * label for the administrator for record keeping.
   */
  
  public DBObject getPersona()
  {
    return personaObj;
  }

  /**
   * This method returns a reference to the 
   * {@link arlut.csd.ganymede.server.DBSession DBSession} object encapsulated
   * by this GanymedeSession object.  This is intended to be used by
   * subclasses of {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * that might not necessarily be in the arlut.csd.ganymede package.
   */

  public DBSession getSession()
  {
    return session;
  }

  //
  //
  // Wizard management functions
  //
  //

  /**
   * Returns true if a wizard is currently interacting
   * with the user.
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public boolean isWizardActive()
  {
    return (wizard != null) && (wizard.isActive());
  }

  /**
   * Returns true if a particular wizard is currently
   * interacting with the user.
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public boolean isWizardActive(GanymediatorWizard wizard)
  {
    return (this.wizard == wizard) && (this.wizard.isActive());
  }

  /**
   * Returns the active wizard, if any, for
   * this GanymedeSession.
   *
   * @see arlut.csd.ganymede.server.GanymediatorWizard
   */

  public GanymediatorWizard getWizard()
  {
    return wizard;
  }

  /**
   * This method is used to register a wizard for this GanymedeSession.
   *
   * If an active wizard is already registered, this method will return
   * false.
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
   * Unregisters a wizard from this GanymedeSession.
   *
   * If there is no active wizard registered, or if the registered wizard
   * is not equal to the wizard parameter, an IllegalArgumentException will
   * be thrown.
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

  /**
   * Returns true if the active persona has some sort of
   * owner/access relationship with the object in question through
   * its list of owner groups.
   */

  public boolean personaMatch(DBObject obj)
  {
    Vector owners;
    InvidDBField inf;
    boolean showit = false;

    /* -- */

    if (obj == null || personaInvid == null)
      {
	//	Ganymede.debug("Null obj/personaInvid");
	return false;
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField); // owner or container

    if (inf == null)
      {
	owners = new Vector();
      }
    else
      {
	// we have to clone the value returned to us by
	// getValuesLocal() because getValuesLocal() returns the
	// actual vector held in the field, and if we were to change
	// that, bad things would happen.

	owners = (Vector) inf.getValuesLocal().clone();
      }

    // All owner group objects are considered to be self-owning.

    if (obj.getTypeID() == SchemaConstants.OwnerBase)
      {
	if (permsdebug)
	  {
	    System.err.println("** Augmenting owner group " + obj.getLabel() + " with self-ownership");
	    showit = true;
	  }

	if (!owners.contains(obj.getInvid()))
	  {
	    owners.addElement(obj.getInvid());
	  }
      }

    // All admin personae are considered to be owned by the owner groups
    // that they are members of

    if (obj.getTypeID() == SchemaConstants.PersonaBase)
      {
	if (permsdebug)
	  {
	    System.err.print("** Augmenting admin persona " + obj.getLabel() + " ");
	    showit = true;
	  }

	InvidDBField inf2 = (InvidDBField) obj.getField(SchemaConstants.PersonaGroupsField);

	if (inf2 != null)
	  {
	    if (permsdebug)
	      {
                Vector values = inf2.getValuesLocal();

                // *** Caution!  getValuesLocal() does not clone the field's contents..
                // 
                // DO NOT modify values here!

		// it's okay to loop on this field since we should be
		// looking at a DBObject and not a DBEditObject

		for (int i = 0; i < values.size(); i++)
		  {
		    if (i > 0)
		      {
			System.err.print(", ");
		      }
		    
		    System.err.print(values.elementAt(i));
		  }
		
		System.err.println();
	      }

	    // we don't have to clone inf2.getValuesLocal() since union() will copy
	    // the elements rather than just setting owners to the vector returned
	    // by inf2.getValuesLocal() if owners is currently null. 

	    owners = arlut.csd.Util.VectorUtils.union(owners, inf2.getValuesLocal());
	  }
	else
	  {
	    if (permsdebug)
	      {
		System.err.println("<no owner groups in this persona>");
	      }
	  }
      }

    boolean result = recursePersonasMatch(owners, new Vector());

    if (showit)
      {
	System.err.println("++ Result = " + result);
      }
    
    return result;
  }

  /**
   *
   * Recursive helper method for personaMatch.. this method does a
   * depth first search up the owner tree for each Invid contained in
   * the invids Vector to see if personaInvid is a member of any of
   * the containing owner groups.
   *
   * @param owners A vector of invids pointing to OwnerBase objects
   * @param alreadySeen A vector of owner group Invid's that have
   * already been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   * 
   */

  private boolean recursePersonasMatch(Vector owners, Vector alreadySeen)
  {
    // *** It is critical that this method not modify the owners parameter passed
    // *** in, as it is 'live' in a DBField.

    if (owners == null)
      {
	return false;
      }

    for (int i = 0; i < owners.size(); i++)
      {
	if (recursePersonaMatch((Invid) owners.elementAt(i), alreadySeen))
	  {
	    return true;
	  }
      }
    
    return false;
  }

  /**
   *
   * Recursive helper method for personaMatch.. this method does a
   * depth first search up the owner tree for the owner Invid to
   * see if personaInvid is a member of any of the containing owner groups.
   *
   * @param owner An Invid pointing to an OwnerBase object
   * @param alreadySeen A vector of owner group Invid's that have
   * already been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   * 
   */

  private boolean recursePersonaMatch(Invid owner, Vector alreadySeen)
  {
    DBObject ownerObj;
    InvidDBField inf;

    /* -- */

    if (owner == null)
      {
	throw new IllegalArgumentException("Null owner passed to recursePersonaMatch");
      }

    if (alreadySeen.contains(owner))
      {
	return false;
      }
    else
      {
	alreadySeen.addElement(owner);
      }
    
    ownerObj = session.viewDBObject(owner);
    
    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);
    
    if (inf != null)
      {
	if (inf.getValuesLocal().contains(personaInvid))
	  {
	    return true;
	  }
      }
    
    // didn't find, recurse up
    
    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);
    
    if (inf != null)
      {
	if (recursePersonasMatch(inf.getValuesLocal(), alreadySeen))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   *
   * This helper method iterates through the owners
   * vector and checks to see if the current personaInvid is a
   * member of all of the groups through either direct membership
   * or through membership of an owning group.  This method 
   * depends on recursePersonasMatch().
   *
   */

  private boolean isMemberAll(Vector owners)
  {
    Invid owner;
    DBObject ownerObj;
    InvidDBField inf;
    boolean found;

    /* -- */

    if (owners == null)
      {
	return false;		// shouldn't happen in context
      }

    // loop over all the owner groups in the vector, make sure
    // that we are a valid member of each of these groups, either
    // directly or through being a member of a group that owns
    // one of these groups.

    for (int i = 0; i < owners.size(); i++)
      {
	found = false;	// yes, but what have you done for me _lately_?

	owner = (Invid) owners.elementAt(i);

	if (owner.getType() != SchemaConstants.OwnerBase)
	  {
	    Ganymede.debug("GanymedeSession.isMemberAll(): bad invid passed " + owner.toString());
	    return false;
	  }

	ownerObj = session.viewDBObject(owner);

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

	if (inf != null && inf.getValuesLocal().contains(personaInvid))
	  {
	    found = true;
	  }
	else
	  {
	    // didn't find, recurse up
	    
	    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);
	    
	    if (inf != null)
	      {
		// using getValuesLocal() here is safe only because
		// recursePersonasMatch() never tries to modify the
		// owners value passed in.  Otherwise, we'd have to
		// clone the results from getValuesLocal().
		
		if (recursePersonasMatch(inf.getValuesLocal(), new Vector()))
		  {
		    found = true;
		  }
	      }
	  }

	if (!found)
	  {
	    return false;
	  }
      }

    return true;
  }

  /**
   * This method returns true if the visibility filter vector allows
   * visibility of the object in question.  The visibility vector
   * works by direct ownership identity (i.e., no recursing up), so
   * it's a simple loop-di-loop.
   */

  private boolean filterMatch(DBObject obj)
  {
    Vector owners;
    InvidDBField inf;
    Invid tmpInvid;

    /* -- */

    if (obj == null)
      {
	return false;
      }

    if (visibilityFilterInvids == null || visibilityFilterInvids.size() == 0)
      {
	return true;		// no visibility restriction, go for it
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField);

    if (inf == null)
      {
	return false;   // we have a restriction, but the object is only owned by supergash.. nope.
      }
    
    owners = inf.getValuesLocal();

    // *** Caution!  getValuesLocal() does not clone the field's contents..
    // 
    // DO NOT modify owners here!

    if (owners == null)
      {
	return false;   // we shouldn't get here, but we don't really care either
      }
    
    // we've got the owners for this object.. now, is there any match between our
    // visibilityFilterInvids and the owners of this object?

    for (int i = 0; i < visibilityFilterInvids.size(); i++)
      {
	tmpInvid = (Invid) visibilityFilterInvids.elementAt(i);

	for (int j = 0; j < owners.size(); j++)
	  {
	    if (tmpInvid.equals((Invid)owners.elementAt(j)))
	      {
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   * Export this object through RMI, so the client can make calls on it.
   *
   * Note that object may be (and often will be) a DBEditObject or subclass
   * thereof, not just a DBObject.
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
	for (Iterator it = exported.iterator(); it.hasNext();)
	  {
	    if (object == it.next())
	      {
		return;
	      }
	  }

	try
	  {
	    Ganymede.rmi.publishObject(object);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException(ex);
	  }

	exported.add(object);
	object.exportFields();
      }
  }


  /**
   * Unexport all exported objects, preventing any further RMI
   * calls from reaching them (for security's sake) and possibly
   * hastening garbage collection / lowering memory usage on the
   * server (for performance's sake).
   *
   * This method can safely be called without regard to whether
   * this GanymedeSession actually did export anything, as
   * exportObject() will only place objects in our local exported
   * ArrayList if this GanymedeSession is configured for remote access
   * with exported objects.
   *
   * @param all if false, unexportObjects() will only unexport editing
   * objects, leaving view-only objects exported.
   *
   */

  private void unexportObjects(boolean all)
  {
    DBObject x;

    /* -- */

    synchronized (exported)
      {
	// count down from the top so we can remove things as we go
	for (int i = exported.size()-1; i >= 0; i--)
	  {
	    x = (DBObject) exported.get(i);

	    if (all || x instanceof DBEditObject)
	      {
		exported.remove(i);

		try
		  {
		    Ganymede.rmi.unpublishObject((Remote) x, true); // go ahead and force
		  }
		catch (NoSuchObjectException ex)
		  {
		    Ganymede.debug(Ganymede.stackTrace(ex)); // report but continue unexporting
		  }

		x.unexportFields();
	      }
	  }
      }
  }

  private void setLastEvent(String text)
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
    this.userInfo = null;
    GanymedeAdmin.refreshUsers();
  }

  /**
   *
   * This private method is called by all methods in
   * GanymedeSession that require the client to be logged
   * in to operate.
   *
   */

  void checklogin() throws NotLoggedInException
  {
    if (!loggedInSemaphore.isSet())
      {
	throw new NotLoggedInException();
      }

    lastActionTime.setTime(System.currentTimeMillis());
  }
}
