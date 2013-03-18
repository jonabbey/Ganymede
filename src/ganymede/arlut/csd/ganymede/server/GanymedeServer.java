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
import arlut.csd.ganymede.common.AdminEntry;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.ErrorTypeEnum;
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

public final class GanymedeServer implements Server {

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
   * <p>Vector of {@link arlut.csd.ganymede.server.GanymedeSession
   * GanymedeSession} objects for user sessions to be monitored by the
   * admin console.</p>
   *
   * <p>Note that there may be GanymedeSession objects active that are
   * not listed in this sessions Vector; GanymedeSession objects used for
   * server-side internal operations are not counted here.  This Vector is
   * primarily used to keep track of things for the admin console code in
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}.</p>
   */

  static private Vector<GanymedeSession> userSessions = new Vector<GanymedeSession>();

  /**
   * <p>A hashtable mapping session names to identity.  Used by the
   * login process to insure that each active session will be given a
   * unique identifying name.</p>
   *
   * <p>Will hold a superset of the keys in the activeUserSessionNames
   * hash unless code is operating within a synchronized block on this
   * hash.</p>
   */

  static private Hashtable<String, String> activeSessionNames = new Hashtable<String, String>();

  /**
   * <p>A hashtable mapping user session names to identity.  Used by
   * the login process to insure that each active user session will be
   * given a unique identifying name.</p>
   */

  static private Hashtable<String, String> activeUserSessionNames = new Hashtable<String, String>();

  /**
   * <p>A hashtable mapping user Invids to a java.lang.Date object
   * representing the last time that user logged into Ganymede.  This
   * data structure is used to check to see if the server's motd.html
   * file has changed since the user last logged in.  This hash of
   * timestamps is not preserved in the ganymede.db file, so whenever
   * the server is restarted, all users are presumed to need to see
   * the motd.html file on their next login.</p>
   */

  static private Hashtable<Invid, Date> userLogOuts = new Hashtable<Invid, Date>();

  /**
   * <p>If true, the server is waiting for all users to disconnect
   * so that it can shut itself down.</p>
   */

  static boolean shutdown = false;

  /**
   * <p>Our handy, all purpose counting semaphore for managing user
   * sessions.</p>
   */

  static loginSemaphore lSemaphore = new loginSemaphore();

  /**
   * <p>Another handy semaphore, this time used to defer
   * shutdowns from build processes</p>
   */

  static loginSemaphore shutdownSemaphore = new loginSemaphore();

  /**
   * <p>Message string explaining reason for shut down.  Displayed to
   * connected users and/or users blocked from logging in while we're
   * waiting to shut down.</p>
   */

  static private String shutdownReason = null;

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
   * GanymedeServer constructor.  We only want one server running
   * per invocation of Ganymede, so we'll check that here.
   */

  public GanymedeServer() throws RemoteException
  {
    synchronized (GanymedeServer.class)
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
    ReturnVal retVal = processLogin(username, password, true, false);

    if (!retVal.didSucceed())   // XXX processLogin never returns null
      {
        return retVal;
      }

    GanymedeSession mySession = (GanymedeSession) retVal.getSession();
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
   * @param directSession If true, the GanymedeSession returned be
   * published for remote RMI access.
   * @param exportObjects If true, the DBObjects viewed and edited by
   * the GanymedeSession will be exported for remote RMI access.
   */

  private synchronized ReturnVal processLogin(String clientName, String clientPass,
                                              boolean directSession,
                                              boolean exportObjects) throws RemoteException
  {
    ReturnVal semaphoreResult = incrementAndTestLoginSemaphore();

    if (!ReturnVal.didSucceed(semaphoreResult))
      {
        return semaphoreResult;
      }

    boolean success = false;

    try
      {
        DBObject userOrAdminObj = validateUserOrAdminLogin(clientName, clientPass);

        if (userOrAdminObj == null)
          {
            return reportFailedLogin(clientName);
          }

        DBObject personaObj = null;
        DBObject userObj = null;

        if (userOrAdminObj.getTypeID() == SchemaConstants.PersonaBase)
          {
            personaObj = userOrAdminObj;
            userObj = getUserFromPersona(personaObj);
            clientName = personaObj.getLabel(); // canonicalize
          }
        else if (userOrAdminObj.getTypeID() == SchemaConstants.UserBase)
          {
            userObj = userOrAdminObj;
            clientName = userObj.getLabel();
          }

        // the GanymedeSession constructor calls one of the
        // register session name methods on us

        GanymedeSession session = new GanymedeSession(GanymedeServer.registerUserSessionName(clientName),
                                                      userObj, personaObj,
                                                      directSession,
                                                      exportObjects);
        monitorUserSession(session);
        success = true;

        return reportSuccessLogin(session);
      }
    finally
      {
        if (!success)
          {
            GanymedeServer.lSemaphore.decrement();

            // notify the consoles after decrementing the login
            // semaphore so the notify won't show the transient
            // semaphore increment

            Ganymede.debug(ts.l("reportFailedLogin.badlogevent", clientName, GanymedeServer.getClientHost()));
          }
      }
  }

  /**
   * Returns null if we were able to increment the login semaphore, or
   * a ReturnVal encoding the problem if not.
   */

  private ReturnVal incrementAndTestLoginSemaphore()
  {
    String error = GanymedeServer.lSemaphore.increment();

    if ("shutdown".equals(error))
      {
        if (shutdownReason != null)
          {
            // "No logins allowed"
            // "The server is currently waiting to shut down.  No
            // logins will be accepted until the server has
            // restarted.\n\nReason for shutdown: {0}"
            return Ganymede.createErrorDialog(ts.l("incrementAndTestLoginSemaphore.nologins"),
                                              ts.l("incrementAndTestLoginSemaphore.nologins_shutdown_reason",
                                                   shutdownReason));
          }
        else
          {
            // "No logins allowed"
            // "The server is currently waiting to shut down.  No
            // logins will be accepted until the server has
            // restarted."
            return Ganymede.createErrorDialog(ts.l("incrementAndTestLoginSemaphore.nologins"),
                                              ts.l("incrementAndTestLoginSemaphore.nologins_shutdown"));
          }
      }
    else if (error != null)
      {
        // "No logins allowed"
        // "Can''t log in to the Ganymede server.. semaphore disabled: {0}"
        return Ganymede.createErrorDialog(ts.l("incrementAndTestLoginSemaphore.nologins"),
                                          ts.l("incrementAndTestLoginSemaphore.nologins_semaphore", error));
      }

    return null;
  }

  /**
   * <p>Logs and reports a failed login for user / admin clientName
   * coming from host clientHost.</p>
   *
   * <p>Returns a ReturnVal to pass back to the client describing the
   * failure.</p>
   */

  private ReturnVal reportFailedLogin(String clientName)
  {
    if (Ganymede.log != null)
      {
        // "Bad login attempt for username: {0} from host {1}"
        Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
                                                   ts.l("reportFailedLogin.badlogevent",
                                                        clientName,
                                                        GanymedeServer.getClientHost()),
                                                   null,
                                                   clientName,
                                                   null,
                                                   null));
      }

    // "Bad login attempt"
    // "Bad username or password, login rejected."
    ReturnVal badCredsRetVal = Ganymede.createErrorDialog(ts.l("reportFailedLogin.badlogin"),
                                                          ts.l("reportFailedLogin.badlogintext"));

    badCredsRetVal.setErrorType(ErrorTypeEnum.BADCREDS);

    return badCredsRetVal;
  }

  /**
   * <p>Logs the successful login of a user and returns a ReturnVal
   * that includes a remote reference to the newly created
   * GanymedeSession.</p>
   */

  private ReturnVal reportSuccessLogin(GanymedeSession session)
  {
    // "{0} logged in from {1}"
    Ganymede.debug(ts.l("reportSuccessLogin.loggedin",
                        session.getIdentity(),
                        session.getClientHostName()));

    if (Ganymede.log != null)
      {
        Vector<Invid> objects = new Vector<Invid>();

        objects.add(session.getIdentityInvid());

        // "OK login for username: {0} from host {1}"
        Ganymede.log.logSystemEvent(new DBLogEvent("normallogin",
                                                   ts.l("reportSuccessLogin.logevent",
                                                        session.getIdentity(),
                                                        session.getClientHostName()),
                                                   session.getIdentityInvid(),
                                                   session.getIdentity(),
                                                   objects,
                                                   null));
      }

    ReturnVal retVal = ReturnVal.success();
    retVal.setSession(session);

    return retVal;
  }

  /**
   * This method is called by the {@link
   * arlut.csd.ganymede.server.timeOutTask timeOutTask} scheduled
   * task, and forces an idle time check on any users logged in.
   */

  public void clearIdleSessions()
  {
    // clone the sessions Vector so any forceOff() resulting from a
    // timeCheck() call won't disturb the loop, and so that we won't
    // have to synchronize on sessions and risk nested monitor
    // deadlock

    Vector<GanymedeSession> sessionsCopy = (Vector<GanymedeSession>) userSessions.clone();

    for (GanymedeSession session: sessionsCopy)
      {
        session.timeCheck();
      }
  }

  /**
   * <p>This method is called by the admin console via the {@link
   * arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} class to
   * kick all user sessions off of the server.</p>
   */

  public void killAllUsers(String reason)
  {
    // clone the sessions Vector so any forceOff() won't disturb the
    // loop

    Vector<GanymedeSession> sessionsCopy = (Vector<GanymedeSession>) userSessions.clone();

    for (GanymedeSession session: sessionsCopy)
      {
        session.forceOff(reason);
      }
  }

  /**
   * <p>This method is called by the admin console via the {@link
   * arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} class to
   * kick a specific user session off of the server.</p>
   */

  public boolean killUser(String username, String reason)
  {
    // it's okay to loop inside sessions since we'll exit the loop as
    // soon as we do a forceOff (which can cause the sessions Vector
    // to be modified in a way that would otherwise disturb the loop

    synchronized (userSessions)
      {
        for (GanymedeSession session: userSessions)
          {
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
    String error = GanymedeServer.lSemaphore.checkEnabled();

    if (error != null)
      {
        return Ganymede.createErrorDialog(ts.l("admin.connect_failure"),
                                          ts.l("admin.semaphore_failure", error));
      }

    DBObject adminObj = validateAdminLogin(clientName, clientPass);
    String clientHost = GanymedeServer.getClientHost();
    int validationResult = validateConsoleAdminPersona(adminObj);

    if (validationResult == 0)
      {
        if (Ganymede.log != null)
          {
            Ganymede.log.logSystemEvent(new DBLogEvent("badpass",
                                                       ts.l("admin.badlogevent", clientName, clientHost),
                                                       null,
                                                       clientName,
                                                       null,
                                                       null));
          }

        return Ganymede.createErrorDialog(ts.l("admin.badlogin"),
                                          ts.l("admin.baduserpass"));
      }

    // creating a new GanymedeAdmin can block if we are currently
    // looping over the connected consoles.

    adminSession aSession = new GanymedeAdmin(validationResult >= 2, clientName, clientHost);

    // now Ganymede.debug() will write to the newly attached console,
    // even though we haven't returned the admin session to the admin
    // console client

    String eventStr = ts.l("admin.goodlogevent", clientName, clientHost);

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
   * <p>Returns the user linked to the provided personaObj, or null if
   * no user is attached.</p>
   */

  public DBObject getUserFromPersona(DBObject personaObj)
  {
    if (personaObj.getTypeID() != SchemaConstants.PersonaBase)
      {
        throw new IllegalArgumentException();
      }

    Invid userInvid = (Invid) personaObj.getFieldValueLocal(SchemaConstants.PersonaAssocUser);

    if (userInvid == null)
      {
        return null;
      }

    return loginSession.getDBSession().viewDBObject(userInvid);
  }

  /**
   * <p>Returns a user or admin persona DBObject if the given
   * user/admin name exists, and has the given password in the
   * database, and is permitted to login and have a
   * GanymedeSession.</p>
   *
   * <p>Returns null if no such user or admin persona / password pair
   * exists.</p>
   */

  public DBObject validateUserOrAdminLogin(String name, String clientPass)
  {
    DBObject personaObj = this.validateAdminLogin(name, clientPass);

    if (personaObj == null)
      {
        return this.validateUserLogin(name, clientPass);
      }

    // don't let the monitor account login to the client
    //
    //    if (personaObj.getInvid().equals(Invid.createInvid(SchemaConstants.PersonaBase,
    //                                                       SchemaConstants.PersonaMonitorObj)))
    //      {
    //        return null;
    //      }

    DBObject userObj = getUserFromPersona(personaObj);

    if (userObj != null && userObj.isInactivated())
      {
        return null;
      }

    return personaObj;
  }

  /**
   * <p>Returns a user DBObject if the given user name has the given
   * password in the database.</p>
   *
   * <p>Returns null if no such user / password pair exists.</p>
   */

  public synchronized DBObject validateUserLogin(String userName, String clientPass)
  {
    Query userQuery = new Query(SchemaConstants.UserBase,
                                new QueryDataNode(SchemaConstants.UserUserName,
                                                  QueryDataNode.NOCASEEQ, userName),
                                false);

    Result result = loginSession.internalSingletonQuery(userQuery);

    if (result != null)
      {
        DBObject user = loginSession.getDBSession().viewDBObject(result.getInvid());
        PasswordDBField pdbf = (PasswordDBField) user.getField(SchemaConstants.UserPassword);

        if (pdbf != null && pdbf.matchPlainText(clientPass))
          {
            return user;
          }
      }

    return null;
  }

  /**
   * <p>Returns an admin persona DBObject if the given personaName
   * exists and has the given password in the database.</p>
   *
   * <p>Returns null if no such admin persona / password pair
   * exists.</p>
   */

  public synchronized DBObject validateAdminLogin(String personaName, String clientPass)
  {
    Query adminQuery = new Query(SchemaConstants.PersonaBase,
                                 new QueryDataNode(SchemaConstants.PersonaLabelField,
                                                   QueryDataNode.NOCASEEQ, personaName),
                                 false);

    Result result = loginSession.internalSingletonQuery(adminQuery);

    if (result != null)
      {
        DBObject personaObj = loginSession.getDBSession().viewDBObject(result.getInvid());
        PasswordDBField pdbf = (PasswordDBField) personaObj.getField(SchemaConstants.PersonaPasswordField);

        if (pdbf != null && pdbf.matchPlainText(clientPass))
          {
            return personaObj;
          }
      }

    return null;
  }

  /**
   * This method determines whether the specified username/password
   * combination is valid for an admin persona.
   *
   * @param DBObject corresponding to an admin object to check for
   * console privs.
   * @return  0 if the admin doesn't have admin console privileges,
   *          1 the admin is allowed basic admin console access,
   *          2 the admin is allowed full admin console privileges,
   *          3 the admin is allowed interpreter access.
   */

  public int validateConsoleAdminPersona(DBObject adminObj)
  {
    if (adminObj == null)
      {
        return 0;
      }

    if (adminObj.getTypeID() != SchemaConstants.PersonaBase)
      {
        throw new RuntimeException("Invalid object type");
      }

    // Are we the One True Amazing Supergash Root User Person? He
    // gets full privileges by default.

    if (adminObj.getInvid().equals(Invid.createInvid(SchemaConstants.PersonaBase,
                                                     SchemaConstants.PersonaSupergashObj)))
      {
        return 3;
      }
    else
      {
        // Is this user prohibited from accessing the admin
        // console?

        if (!adminObj.isSet(SchemaConstants.PersonaAdminConsole))
          {
            return 0;
          }

        // Ok, they can access the admin console...but do they
        // have full privileges?

        if (!adminObj.isSet(SchemaConstants.PersonaAdminPower))
          {
            return 1;
          }

        // Ok, they have full privileges...but can they access the
        // admin interpreter?

        if (!adminObj.isSet(SchemaConstants.PersonaInterpreterPower))
          {
            return 2;
          }

        return 3;
      }
  }

  /** ------------------------------------------------------------------------------

      Static Methods

      ------------------------------------------------------------------------------ */

  /**
   * <p>This method is used by GanymedeSession.login() to find and
   * record a unique name for an internal session.  It is matched with
   * clearSession(), below.</p>
   */

  static String registerInternalSessionName(String sessionName)
  {
    if (sessionName == null)
      {
        throw new IllegalArgumentException("invalid null sessionName");
      }

    String temp = sessionName;
    int i = 2;

    synchronized (activeSessionNames)
      {
        while (activeSessionNames.containsKey(sessionName))
          {
            sessionName = temp + "[" + i + "]";
            i++;
          }

        activeSessionNames.put(sessionName, sessionName);
      }

    return sessionName;
  }

  /**
   * <p>This method is used by GanymedeSession.login() to find and
   * record a unique name for a user session.  It is matched with
   * clearSession(), below.</p>
   */

  static String registerUserSessionName(String sessionName)
  {
    if (sessionName == null)
      {
        throw new IllegalArgumentException("invalid null sessionName");
      }

    String temp = sessionName;
    int i = 2;

    synchronized (activeSessionNames)
      {
        while (activeSessionNames.contains(sessionName))
          {
            sessionName = temp + "[" + i + "]";
            i++;
          }

        activeSessionNames.put(sessionName, sessionName);
        activeUserSessionNames.put(sessionName, sessionName);
      }

    return sessionName;
  }

  /**
   * <p>This method handles clearing a remote session's session name
   * from the activeSessionNames and activeRemoteSessionNames
   * hashes.</p>
   *
   * <p>If this server is in deferred shutdown mode and this is the
   * last logged in remote session, we'll proceed to shut-down.</p>
   *
   * <p>Note that the session parameter must not have cleared its
   * permManager reference before calling this method.</p>
   */

  static void clearSession(GanymedeSession session)
  {
    boolean userSession = false;

    Invid userInvid = session.getPermManager().getUserInvid();
    String sessionName = session.getPermManager().getSessionName();

    if (userInvid != null)
      {
        userLogOuts.put(userInvid, new Date());
      }

    synchronized (activeSessionNames)
      {
        activeSessionNames.remove(sessionName);

        if (activeUserSessionNames.remove(sessionName) != null)
          {
            userSession = true;

            // if we are in deferred shutdown mode and this was the last
            // remote user logged in, spin off a thread to shut the server
            // down

            if (shutdown && activeUserSessionNames.size() == 0)
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

                      GanymedeServer.shutdown(null);
                    }
                  }, ts.l("clearActiveUser.deathThread"));

                deathThread.start();
              }
          }
      }

    if (session.isUserSession())
      {
        try
          {
            GanymedeServer.lSemaphore.decrement();
          }
        catch (IllegalArgumentException ex)
          {
            Ganymede.logError(ex);
          }
      }

    if (userSession)
      {
        unmonitorUserSession(session);
      }
  }

  /**
   * <p>This method is used by the {@link
   * arlut.csd.ganymede.server.GanymedeAdmin#shutdown(boolean,
   * java.lang.String) shutdown()} method to put the server into
   * 'shutdown soon' mode.</p>
   */

  public static void setShutdown(String reason)
  {
    if (reason != null)
      {
        GanymedeServer.shutdownReason = reason;
      }

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

        GanymedeServer.shutdown(null);

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

  public static ReturnVal shutdown(String reason)
  {
    if (reason != null)
      {
        GanymedeServer.shutdownReason = reason;
      }

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
            throw ex;           // maybe didn't lock, so go down hard
          }

        // ok, we now are left holding a dump lock.  it should be safe to kick
        // everybody off and shut down the server

        // "Server going down.. database locked"
        Ganymede.debug(ts.l("shutdown.locked"));

        // "Server going down.. disconnecting clients"
        Ganymede.debug(ts.l("shutdown.clients"));

        // forceOff modifies GanymedeServer.userSessions, so we need
        // to copy our list before we iterate over it.

        Vector<GanymedeSession> tempList = (Vector<GanymedeSession>) userSessions.clone();

        for (GanymedeSession temp: tempList)
          {
            if (shutdownReason != null)
              {
                // "Server going down"
                temp.forceOff(ts.l("shutdown.clientNotification"));
              }
            else
              {
                // "Server going down\n\nReason:{0}"
                temp.forceOff(ts.l("shutdown.clientNotification_reason", shutdownReason));
              }
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

        if (Ganymede.log != null)
          {
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
   * <p>Returns the name of the client that is doing an RMI call on
   * the server.</p>
   *
   * <p>Will return "unknown" if the thread that calls this method
   * wasn't initiated by an RMI call.</p>
   */

  public static String getClientHost()
  {
    try
      {
        String ipAddress = UnicastRemoteObject.getClientHost();

        try
          {
            java.net.InetAddress addr = java.net.InetAddress.getByName(ipAddress);
            return addr.getHostName();
          }
        catch (java.net.UnknownHostException ex)
          {
            return ipAddress;
          }
      }
    catch (ServerNotActiveException ex)
      {
        return "unknown";
      }
  }

  /**
   * <p>This method is triggered from the admin console when the user
   * runs an 'Invid Sweep'.  It is designed to scan through the
   * Ganymede datastore's reference fields and clean out any
   * references found that point to non-existent objects.</p>
   *
   * @return true if there were any invalid invids in the database
   */

  public boolean sweepInvids()
  {
    Vector<Short>
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

    DBSession session = Ganymede.internalSession.getDBSession();

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

        return false;           // actually we just failed, but same difference
      }

    try
      {
        for (DBObjectBase base: Ganymede.db.bases())
          {
            Ganymede.debug(ts.l("sweepInvids.sweeping", base.toString()));

            for (DBObject object: base.getObjects())
              {
                removeVector = new Vector<Short>();

                // loop 3: iterate over the fields present in this object

                synchronized (object.fieldAry)
                  {
                    for (DBField field: object.fieldAry)
                      {
                        if (field == null || !(field instanceof InvidDBField))
                          {
                            continue;   // only check invid fields
                          }

                        InvidDBField iField = (InvidDBField) field;

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
                                    iField.getVectVal().add(invid); // keep this invid
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
                                removeVector.add(Short.valueOf(iField.getID()));
                              }
                          }
                        else
                          {
                            Invid invid = (Invid) iField.value;

                            if (session.viewDBObject(invid) == null)
                              {
                                swept = true;
                                removeVector.add(Short.valueOf(iField.getID()));

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

                for (Short fieldID: removeVector)
                  {
                    object.clearField(fieldID.shortValue());

                    Ganymede.debug(ts.l("sweepInvids.undefining",
                                        fieldID.toString(),
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

    DBSession session = Ganymede.internalSession.getDBSession();

    /* -- */

    DBDumpLock lock = new DBDumpLock(Ganymede.db);

    try
      {
        lock.establish("checkInvids"); // wait until we get our lock
      }
    catch (InterruptedException ex)
      {
        Ganymede.debug(ts.l("checkInvids.noproceed"));

        return false;           // actually we just failed, but same difference
      }

    try
      {
        // first we're going to do our forward test, making sure that
        // all pointers registered in the objects in our data store
        // point to valid objects and that they have valid symmetric
        // back pointers or virtual back pointer registrations in the
        // DBLinkTracker class.

        for (DBObjectBase base: Ganymede.db.bases())
          {
            Ganymede.debug(ts.l("checkInvids.checking", base.getName()));

            for (DBObject object: base.getObjects())
              {
                synchronized (object.fieldAry)
                  {
                    for (DBField field: object.fieldAry)
                      {
                        // we only care about invid fields

                        if (field == null || !(field instanceof InvidDBField))
                          {
                            continue;
                          }

                        InvidDBField iField = (InvidDBField) field;

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

        return false;           // actually we just failed, but same difference
      }

    try
      {
        // loop over the object bases

        for (DBObjectBase base: Ganymede.db.bases())
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
    Vector<Invid> invidsToDelete = new Vector<Invid>();

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
        for (DBObjectBase base: Ganymede.db.bases())
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
                    invidsToDelete.add(object.getInvid());
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
        gSession = new GanymedeSession(":embeddedSweep");
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

        DBSession session = gSession.getDBSession();

        // we want a non-interactive transaction.. if an object removal fails, the
        // whole transaction will fail, no rollbacks.

        gSession.openTransaction("embedded object sweep", false); // non-interactive

        for (Invid objInvid: invidsToDelete)
          {
            ReturnVal retVal = session.deleteDBObject(objInvid);

            if (!ReturnVal.didSucceed(retVal))
              {
                // "Couldn''t delete object {0}"
                Ganymede.debug(ts.l("sweepEmbeddedObjects.delete_failure", gSession.getDBSession().getObjectLabel(objInvid)));
              }
            else
              {
                // "Deleted object {0}"
                Ganymede.debug(ts.l("sweepEmbeddedObjects.delete_ok", gSession.getDBSession().getObjectLabel(objInvid)));
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
   * {@link arlut.csd.ganymede.server.GanymedeServer#userSessions userSessions}
   * field, which is used by the admin console code to iterate
   * over connected users when logging user actions to the
   * Ganymede admin console.</p>
   */

  public static void monitorUserSession(GanymedeSession session)
  {
    synchronized (userSessions)
      {
        sendMessageToRemoteSessions(ClientMessage.LOGIN, ts.l("addRemoteUser.logged_in", session.getUserName()));

        // we send the above message before adding the user to the
        // userSessions Vector so that the user doesn't get bothered
        // with a 'you logged in' message

        userSessions.add(session);

        sendMessageToRemoteSessions(ClientMessage.LOGINCOUNT, Integer.toString(userSessions.size()));
      }
  }

  /**
   * <p>This method is called to remove a remote user's
   * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}
   * object from the GanymedeServer's static
   * {@link arlut.csd.ganymede.server.GanymedeServer#userSessions userSessions}
   * field, which is used by the admin console code to iterate
   * over connected users when logging user actions to the
   * Ganymede admin console.</p>
   */

  public static void unmonitorUserSession(GanymedeSession session)
  {
    synchronized (userSessions)
      {
        if (userSessions.remove(session))
          {
            // we just removed the session of the user who logged out, so
            // they won't receive the log out message that we'll send to
            // the other clients

            sendMessageToRemoteSessions(ClientMessage.LOGOUT, ts.l("removeRemoteUser.logged_out", session.getUserName()));
            sendMessageToRemoteSessions(ClientMessage.LOGINCOUNT, Integer.toString(userSessions.size()));
          }
      }

    // update the admin consoles

    GanymedeAdmin.refreshUsers();
  }

  /**
   * <p>This method is used by the
   * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin}
   * refreshUsers() method to get a summary of the state of the
   * monitored user sessions.</p>
   */

  public static Vector<AdminEntry> getUserTable()
  {
    Vector<AdminEntry> entries = null;

    synchronized (userSessions)
      {
        entries = new Vector<AdminEntry>(userSessions.size());

        for (GanymedeSession session: userSessions)
          {
            if (session.isLoggedIn())
              {
                entries.add(session.getAdminEntry());
              }
          }
      }

    return entries;
  }

  /**
   * <p>Used by the Ganymede server to transmit notifications to
   * connected clients.</p>
   *
   * @param type Should be a valid value from arlut.csd.common.ClientMessage
   * @param message The message to send
   */

  public static void sendMessageToRemoteSessions(int type, String message)
  {
    sendMessageToRemoteSessions(type, message, null);
  }

  /**
   * <p>Used by the Ganymede server to transmit notifications to
   * connected clients.</p>
   *
   * @param type Should be a valid value from arlut.csd.common.ClientMessage
   * @param message The message to send
   * @param self If non-null, sendMessageToRemoteSessions will skip sending the
   * message to self
   */

  public static void sendMessageToRemoteSessions(int type, String message, GanymedeSession self)
  {
    Vector<GanymedeSession> sessionsCopy = (Vector<GanymedeSession>) userSessions.clone();

    for (GanymedeSession session: sessionsCopy)
      {
        if (session != self)
          {
            session.sendMessage(type, message);
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
   *
   * @param userToDateCompare If not null, the Invid of a user on
   * whose behalf we want to retrieve the message.  If the user has
   * logged in more recently than the timestamp of the file has
   * changed, we will return a null result
   *
   * @param html If true, return the .html version.  If false, return
   * the .txt version.
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
}
