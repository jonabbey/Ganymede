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
   Release: $Name:  $
   Version: $Revision: 1.123 $
   Last Mod Date: $Date: 1999/02/25 05:19:54 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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

import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.net.*;
import java.io.*;

import Qsmtp;
import arlut.csd.Util.*;
import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 GanymedeSession

------------------------------------------------------------------------------*/

/**
 *
 * This class is the heart of the Ganymede server.<br><br>
 *
 * Each client logged on to the server will hold a reference to a
 * unique GanymedeSession object.  This object provides services to
 * the client, tracks the client's status, manages permissions, and
 * keeps track of the client's transactions.<br><br>
 *
 * Most methods in this class are synchronized to avoid race condition
 * security holes between the persona change logic and the actual operations.
 * 
 * @version $Revision: 1.123 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 *   
 */

final public class GanymedeSession extends UnicastRemoteObject implements Session, Unreferenced {

  static final boolean debug = false;

  // ---

  /**
   *
   * Remote reference to our client
   *
   */

  Client client;

  // client status tracking

  boolean logged_in;
  boolean forced_off = false;
  boolean supergashMode = false;
  boolean beforeversupergash = false; // Be Forever Yamamoto

  /**
   *
   * This variable tracks whether or not the client desires to have
   * wizards presented.  If this is false, custom plug-in code
   * for the object types stored in the GanymedeServer may either
   * refuse certain operations or will resort to taking a default action.
   *
   */
  
  public boolean enableWizards = true;

  /**
   *
   * If this variable is set to false, no custom wizard code will ever
   * be invoked, and required fields will not be forced.  This is
   * intended primarily for direct database loading. <br><br>
   *
   * This variable is not intended ever to be available to the client,
   * but should only be set by local server code.
   *   
   */
  
  public boolean enableOversight = true;

  /**
   *
   * The time that this client initially connected to the server.  Used
   * by the admin console code.
   *
   */

  Date connecttime;

  /**
   * The name that the user is connected to the server under.. this
   * may be &lt;username&gt;2, &lt;username&gt;3, etc., if the user is
   * connected to the server multiple times.  The username will be
   * unique on the server at any given time.<br><br>
   *
   * username should never be null.  If a client logs in directly
   * to a persona, username will be that personaname plus an optional
   * session id.
   * 
   */

  String username;

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
   *
   * We used to depend primarily on setLastError()/getLastError() to
   * provide diagnostics, but we've moved more to using ReturnVal result
   * objects to provide diagnostic dialog results.
   *
   */

  String lastError;

  /**
   *
   * The current status message for this client.  The GanymedeAdmin code
   * that manages the admin consoles will consult this String when it
   * updates the admin consoles.
   *
   */

  String status = null;

  /**
   *
   * Description of the last action recorded for this client.  The
   * GanymedeAdmin code that manages the admin consoles will consult
   * this String when it updates the admin consoles.
   * 
   */

  String lastEvent = null;

  /**
   *
   * Our DBSession object.  DBSession is the generic DBStore access
   * layer.  A GanymedeSession is layered on top of a DBSession to
   * provide access control and remote access via RMI.  The DBSession
   * object is accessible to server-side code only and provides
   * transaction support.
   * 
   */

  DBSession session;

  /**
   *
   * A GanymedeSession can have a single wizard active.  If this variable
   * is non-null, a custom type-specific DBEditObject subclass has instantiated
   * a wizard to interact with the user.
   *
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
   *
   * A reference to our current persona object.  We save this so
   * we can look up owner groups and what not more quickly.  An
   * end-user logged in without any extra privileges will have
   * a null personaObj value. 
   *
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
   *
   * This variable stores the permission bits that are applicable to objects
   * that the current persona has ownership privilege over.  This matrix
   * is always a permissive superset of defaultPerms.
   *
   */

  PermMatrix personaPerms;

  /**
   *
   * This variable stores the permission bits that are applicable to generic
   * objects not specifically owned by this persona.<br><br>
   *
   * Each permission object in the Ganymede database includes
   * permissions as apply to objects owned by the persona and as apply
   * to objects not owned by the persona.<br><br>
   *
   * This variable holds the union of the 'as apply to objects not
   * owned by the persona' matrices across all permissions objects
   * that apply to the current persona.
   *  
   */

  PermMatrix defaultPerms;

  /**
   *
   * This variable stores the permission bits that are applicable to
   * objects that the current persona has ownership privilege over and
   * which the current admin has permission to delegate to subordinate
   * roles.  This matrix is always a permissive superset of
   * delegatableDefaultPerms.
   * 
   */

  PermMatrix delegatablePersonaPerms;

  /**
   *
   * This variable stores the permission bits that are applicable to
   * generic objects not specifically owned by this persona and which
   * the current admin has permission to delegate to subordinate
   * roles.<br><br>
   *
   * Each permission object in the Ganymede database includes
   * permissions as apply to objects owned by the persona and as apply
   * to objects not owned by the persona.<br><br>
   *
   * This variable holds the union of the 'as apply to objects not
   * owned by the persona' matrices across all permissions objects
   * that apply to the current persona.
   *   
   */

  PermMatrix delegatableDefaultPerms;

  /**
   *
   * A reference to the Ganymede object storing our default permissions,
   * or the permissions that applies when we are not in supergash mode
   * and we do not have any ownership over the object in questin.
   *
   */

  DBObject defaultObj;

  /**
   *
   * This variable is a vector of object references (Invid's) to the owner groups
   * that the client has requested newly created objects be placed in.  While
   * this vector is not-null, any new objects created will be owned by the list
   * of ownergroups held here.
   *
   */

  Vector newObjectOwnerInvids = null;

  /**
   *
   * This variable is a vector of object references (Invid's) to the
   * owner groups that the client has requested the listing of objects
   * be restricted to.  That is, the client has requested that the
   * results of Queries and Dumps only include those objects owned by
   * owner groups in this list.  This feature is used primarily for
   * when a client is logged in with supergash privileges, but the
   * user wants to restrict the visibility of objects for convenience.
   * 
   */

  Vector visibilityFilterInvids = null;

  /**
   *
   * This variable caches the results of the getOwnerGroups() method.  It
   * stores the list of owner groups that the current persona has any
   * kind of membership in, either through direct membership or by being
   * a member of a group that owns other owner groups.
   *
   */

  QueryResult ownerList = null;

  /* -- */

  /**
   *
   * Constructor for a server-internal GanymedeSession.  Used when
   * the server's internal code needs to do a query, etc.  Note that
   * the Ganymede server will create this fairly early on, and will
   * keep it around for internal usage.  Note that we don't add
   * this to the data structures used for the admin console.<br><br>
   *
   * Note that all internal session activities (queries, etc.) are
   * currently using a single, synchronized DBSession object.. this
   * mean that only one user at a time can currently be processed for
   * login. 8-(<br><br>
   * 
   * Internal sessions, as created by this constructor, have full
   * privileges to do any possible operation.
   *
   */

  GanymedeSession() throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    // construct our DBSession

    logged_in = true;
    client = null;
    username = "internal";
    clienthost = "internal";
    session = new DBSession(Ganymede.db, this, "internal");

    supergashMode = true;
    beforeversupergash = true;

    updatePerms(true);
  }

  /**
   *
   * Constructor used to create a server-side attachment for a Ganymede
   * client.<br><br>
   *
   * This constructor is called by the GanymedeServer login() method.<br><br>
   *
   * A Client can log in either as an end-user or as a admin persona.  Typically,
   * a client will log in with their end-user name and password, then use
   * selectPersona to gain admin privileges.  The server may allow users to
   * login directly with an admin persona (supergash, say), if so configured.
   *
   * @param client Remote object exported by the client, provides id callbacks
   * @param userObject The user record for this login
   * @param personaObject The user's initial admin persona 
   *
   * @see arlut.csd.ganymede.GanymedeServer.login()
   * 
   */
  
  GanymedeSession(Client client, String loginName, 
		  DBObject userObject, DBObject personaObject) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    // --

    // record information about the client that we'll need
    // to have while the client is connected to us.
    
    this.client = client;

    if (userObject != null)
      {
	userInvid = userObject.getInvid();
      }
    else
      {
	userInvid = null;
      }

    if (personaObject != null)
      {
	personaInvid = personaObject.getInvid();
	personaName = personaObject.getLabel();
      }
    else
      {
	personaInvid = null;	// shouldn't happen
      }

    // find a unique name for this session

    username = GanymedeServer.registerActiveUser(loginName);

    // find out where the user is coming from

    try
      {
	clienthost = getClientHost();
      }
    catch (ServerNotActiveException ex)
      {
	clienthost = "unknown";
      }

    // record our login time

    connecttime = new Date();

    // construct our DBSession

    session = new DBSession(Ganymede.db, this, username);

    // Let the GanymedeServer know that this session is
    // now active.

    GanymedeServer.sessions.addElement(this);

    // update status, update the admin consoles

    logged_in = true;
    status = "logged in";
    lastEvent = "logged in";
    GanymedeAdmin.refreshUsers();

    // precalc the permissions applicable for this user

    updatePerms(true);

    // and we're done

    if (debug)
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

  public boolean isSuperGash()
  {
    return supergashMode;
  }

  /**
   *
   * This method is used to flag an error condition that the client
   * can then call getLastError() to look up.  This system functions
   * similarly to the errno system in C, and has similar problems
   * in a multi-thread environment.  Most Ganymede code now uses
   * ReturnVal objects to pass back error information in response
   * to a client operation.
   *
   */

  void setLastError(String status)
  {
    lastError = status;
    Ganymede.debug("GanymedeSession [" + username + "]: setLastError (" + lastError + ")");
  }

  /**
   *
   * If the server decides this person needs to get off (if the user
   * times out, is forced off by an admin, or if the server is going
   * down), it will call this method to knock them off.
   * 
   */

  void forceOff(String reason)
  {
    Ganymede.debug("Forcing " + username + " off for " + reason);

    if (client != null)
      {
	try
	  {
	    client.forceDisconnect(reason);
	  }
	catch (RemoteException e)
	  {
	    // ok, they're already gone.. (?)
	  }
      }

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
	Ganymede.log.logSystemEvent(new DBLogEvent("abnormallogout",
						   "Abnormal termination for username: " + username + "\n" +
						   reason,
						   userInvid,
						   username,
						   objects,
						   null));
      }

    forced_off = true;		// keep logout from logging a normal logout

    this.logout();
  }

  /**
   *
   * This method is called when the Java RMI system detects that this
   * remote object is no longer referenced by any remote objects.<br><br>
   *
   * This method handles abnormal logouts and time outs for us.  By
   * default, the 1.1 RMI time-out is 10 minutes.
   *
   * @see java.rmi.server.Unreferenced
   *
   */

  public void unreferenced()
  {
    if (logged_in)
      {
	forceOff("dead client");
      }
  }

  //************************************************************
  //
  // All methods from this point on are part of the Server remote
  // interface, and can be called by the client via RMI.
  //
  //************************************************************

  /** 
   *
   * getLastError() returns text explaining the last error condition.<br><br>
   *
   * This method is now all-but-deprecated, as most code in the Ganymede
   * server now uses ReturnVal objects to return error information.
   *
   * @see setLastError
   * @see arlut.csd.ganymede.Session 
   */
  
  public String getLastError()
  {
    String result = null;

    /* -- */

    if (lastError != null)
      {
	result = lastError;
	lastError = null;
      }
    else if (session != null)
      {
	result = session.lastError;
	session.lastError = null;
      }

    return result;
  }

  /**
   *
   * Log out this session.  After this method is called, no other
   * methods may be called on this session object.<br><br>
   *
   * This method is partially synchronized, to avoid locking up
   * the admin console if this user's session has become deadlocked.
   *
   * @see arlut.csd.ganymede.Session
   * 
   */

  public void logout()
  {
    // This method is not synchronized to help stave off threads
    // piling up trying to kill off a user which is deadlocked.

    // Obviously we want to prevent the deadlock in the first place,
    // but this will help keep hapless admins on the console from
    // locking their console trying to kill deadlocked users.
    
    if (!logged_in)
      {
	return;
      }

    logged_in = false;

    synchronized (this)
      {
	if (client == null)
	  {
	    // We don't need to update GanymedeServer's lists for internal sessions

	    session.logout();	// *sync* DBSession
	    ownerList = null;
	    return;
	  }

	Ganymede.debug("User " + username + " logging off");

	this.client = null;

	// logout the client, abort any DBSession transaction going

	session.logout();	// *sync* DBSession

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
							   "OK logout for username: " + username,
							   userInvid,
							   username,
							   objects,
							   null));
	      }
	  }

	// Update the server's records, refresh the admin consoles.

	GanymedeServer.sessions.removeElement(this);
	GanymedeServer.clearActiveUser(username);

	if (userInvid != null)
	  {
	    GanymedeServer.userLogOuts.put(userInvid, new Date());
	  }
	else
	  {
	    GanymedeServer.userLogOuts.put(personaInvid, new Date());
	  }

	GanymedeAdmin.refreshUsers();

	// help the garbage collector

	ownerList = null;
	personaObj = null;
	permBase = null;
	defaultObj = null;
	newObjectOwnerInvids = null;
	visibilityFilterInvids = null;
	personaPerms = null;
	defaultPerms = null;
	delegatablePersonaPerms = null;
	delegatableDefaultPerms = null;

	Ganymede.debug("User " + username + " logged off");

	this.username = null;
	this.lastError = null;
      }
  }

  /**
   *
   * This method is used to allow a client to request that wizards
   * not be provided in response to actions by the client.  This
   * is intended to allow non-interactive or non-gui clients to
   * do work without having to go through a wizard interaction
   * sequence.<br><br>
   *
   * Wizards are enabled by default.
   *
   * @param val If true, wizards will be enabled.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public void enableWizards(boolean val)
  {
    this.enableWizards = val;
  }

  /**
   *
   * This method is used to allow local server-side code to request
   * that no oversight be maintained over changes made to the server
   * through this GanymedeSession.<br><br>
   *
   * This is intended <b>only</b> for trusted code that does its own
   * checking and validation on changes made to the database.  If
   * oversight is turned off, no wizard code will be called, and the
   * required field logic will be bypassed.  Extreme care must
   * be used in disabling oversight, and oversight should only be
   * turned off for direct loading and other situations where there
   * won't be multi-user use, to avoid breaking constraints that
   * custom plug-ins count on.<br><br>
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
   *
   * This method is used to tell the client where to look
   * to access the Ganymede help document tree.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public String getHelpBase()
  {
    return Ganymede.helpbaseProperty;
  }

  /**
   *
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
   *
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
   *
   * This method returns the identification string that the server
   * has assigned to the user.
   * 
   * @see arlut.csd.ganymede.Session
   *
   */

  public String getMyUserName()
  {
    return username;
  }

  /**
   *
   * This method returns a list of personae names available
   * to the user logged in.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public Vector getPersonae()
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
	for (int i = 0; i < inv.size(); i++)
	  {
	    results.addElement(viewObjectLabel((Invid)inv.getElement(i)));
	  }
      }

    results.addElement(user.getLabel()); // add their 'end-user' persona

    return results;
  }

  /**
   *
   * This method is used to select an admin persona, changing the
   * permissions that the user has and the objects that are
   * accessible in the database.
   *
   * @see arlut.csd.ganymede.Session
   * 
   */

  public synchronized boolean selectPersona(String persona, String password)
  {
    checklogin();

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

    // if they are selecting their username, go ahead and 
    // clear out the persona privs and return true

    if (user.getLabel().equals(persona))
      {
	personaObject = null;
	personaInvid = null;
	personaName = null;
	updatePerms(true);
	ownerList = null;
	setLastEvent("selectPersona: " + persona);
	return true;
      }

    // ok, we need to find out persona they are trying to switch to

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    for (int i = 0; i < inv.size(); i++)
      {
	invid = (Invid) inv.getElement(i);

	// it's okay to use the faster viewDBObject() here, because we
	// are always going to be doing this for internal purposes

	personaObject = session.viewDBObject(invid);

	if (personaObject.getLabel().equals(persona))
	  {
	    personaName = personaObject.getLabel();
	    break;
	  }
	else
	  {
	    personaObject = null;
	  }
      }

    if (personaObject == null)
      {
	Ganymede.debug("Couldn't find persona " + persona + " for user:" + user.getLabel());
	return false;
      }

    pdbf = (PasswordDBField) personaObject.getField(SchemaConstants.PersonaPasswordField);
    
    if (pdbf != null && pdbf.matchPlainText(password))
      {
	Ganymede.debug("Found persona " + persona + " for user:" + user.getLabel());
	personaInvid = personaObject.getInvid();
	updatePerms(true);
	ownerList = null;
	setLastEvent("selectPersona: " + persona);
	return true;
      }

    return false;
  }

  /**
   *
   * This method returns a QueryResult of owner groups that the current
   * persona has access to.  This list is the transitive closure of
   * the list of owner groups in the current persona.  That is, the
   * list includes all the owner groups in the current persona along
   * with all of the owner groups those owner groups own, and so on.<br><br>
   *
   * Note that getOwnerGroups caches its owner group list in the object
   * member ownerList for efficiency.
   *
   */

  public synchronized QueryResult getOwnerGroups()
  {
    checklogin();

    /* - */

    QueryResult result = new QueryResult();
    InvidDBField inf;
    Vector groups, children, temp;
    Hashtable seen = new Hashtable();
    Invid inv;
    DBObject owner;

    /* -- */

    if (ownerList != null)
      {
	return ownerList;
      }

    if (personaInvid == null)
      {
	return result;		// End users don't have any owner group access
      }

    // if we're in supergash mode, return all the owner groups
    // defined

    if (supergashMode)
      {
	Query q = new Query(SchemaConstants.OwnerBase);
	q.setFiltered(false);

	ownerList = query(q);	// save for our cache

	return ownerList;
      }

    // otherwise, we've got to do a very little bit of legwork

    inf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaGroupsField);

    if (inf == null)
      {
	return result;		// empty list
      }

    // do a breadth-first search of the owner groups

    groups = inf.getValuesLocal();

    // *** Caution!  getValuesLocal() does not clone the field's contents..
    // 
    // DO NOT modify groups here!

    while (groups != null && (groups.size() > 0))
      {
	children = new Vector();

	for (int i = 0; i < groups.size(); i++)
	  {
	    inv = (Invid) groups.elementAt(i);

	    if (seen.containsKey(inv))
	      {
		continue;
	      }

	    // it's okay to use session.viewDBObject here because we
	    // are always going to be doing this operation for supergash's
	    // benefit.  Objects that are pulled directly from the hashes
	    // don't have owners, and so will always grant us access.

	    owner = session.viewDBObject(inv);

	    if (owner == null)
	      {
		continue;
	      }
	    else
	      {
		seen.put(inv, inv);
	      }

	    result.addRow(inv, owner.getLabel(), false);

	    // got the parent.. now add any ownerbase objects owned by it

	    inf = (InvidDBField) owner.getField(SchemaConstants.OwnerObjectsOwned);

	    if (inf != null)
	      {
		temp = inf.getValuesLocal();

		// *** Caution!  getValuesLocal() does not clone the field's contents..
		// 
		// DO NOT modify temp here!

		for (int j = 0; j < temp.size(); j++)
		  {
		    inv = (Invid) temp.elementAt(j);
		    
		    if (inv.getType() == SchemaConstants.OwnerBase)
		      {
			children.addElement(inv);
		      }
		  }
	      }
	  }

	groups = children;
      }
    
    ownerList = result;
    return ownerList;
  }

  /**
   *
   * This method may be used to set the owner groups of any objects
   * created hereafter.
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   *
   */

  public synchronized ReturnVal setDefaultOwner(Vector ownerInvids)
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

	// this check is actually redundant, as the InvidDBField link logic
	// would catch such for us, but it makes a nice couplet with the
	// getNum() check below, so I'll leave it here.
	
	if (ownerInvidItem.getType() != SchemaConstants.OwnerBase)
	  {
	    return Ganymede.createErrorDialog("Error in setDefaultOwner()",
					      "Error.. ownerInvids contains an invalid invid");
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
	return Ganymede.createErrorDialog("Error in setDefaultOwner()",
					  "Error.. ownerInvids contains invid that the persona is not a member of.");
      }
    else
      {
	newObjectOwnerInvids = tmpInvids;
	setLastEvent("setDefaultOwner");
	return null;
      }
  }

  /**
   *
   * This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.<br><br>
   *
   * This method cannot be used to grant access to objects that are
   * not accessible by the client's adminPersona.<br><br>
   *
   * Calling this method with ownerInvids set to null will turn off the filtering.
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   * 
   */

  public synchronized ReturnVal filterQueries(Vector ownerInvids)
  {
    checklogin();

    /* - */

    if (ownerInvids == null)
      {
	visibilityFilterInvids = ownerInvids;
	return null;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
	return Ganymede.createErrorDialog("Server: Error in filterQueries()",
					  "Error.. ownerInvids contains invid that the persona is not a member of.");
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
   *
   * This method returns a list of remote references to the Ganymede
   * object type definitions.  This method will throws a RuntimeException
   * if it is called when the server is in schemaEditMode.<br><br>
   *
   * @deprecated Superseded by the more efficient getBaseList()
   * 
   * @see arlut.csd.ganymede.Session
   */

  public Vector getTypes()
  {
    checklogin();

    /* - */

    Enumeration enum;
    Vector result = new Vector();

    /* -- */

    synchronized (Ganymede.db)
      {
	try
	  {
	    if (Ganymede.db.schemaEditInProgress)
	      {
		throw new RuntimeException("schemaEditInProgress");
	      }
	    
	    enum = Ganymede.db.objectBases.elements();

	    while (enum.hasMoreElements())
	      {
		DBObjectBase base = (DBObjectBase) enum.nextElement();
		
		if (getPerm(base.getTypeID(), true).isVisible())
		  {
		    result.addElement(base);
		  }
	      }
	  }
	finally
	  {
	    Ganymede.db.notifyAll(); // for lock code
	  }
      }

    return result;
  }

  /**
   *
   * Returns the root of the category tree on the server
   *
   * @deprecated Superseded by the more efficient getCategoryTree()
   *
   * @see arlut.csd.ganymede.Category
   * @see arlut.csd.ganymede.Session
   */

  public Category getRootCategory()
  {
    checklogin();

    /* - */

    return Ganymede.db.rootCategory;
  }

  /**
   *
   * Returns a serialized representation of the basic category
   * and base structure on the server.<br><br>
   *
   * This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the CategoryTransport
   * constructor calls other synchronized methods on GanymedeSession.
   *
   * @see arlut.csd.ganymede.Category
   * @see arlut.csd.ganymede.Session
   */

  public synchronized CategoryTransport getCategoryTree()
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
	    synchronized (Ganymede.db) // *sync* on DBStore
	      {
		// pass Ganymede.internalSession so that the master
		// CategoryTransport object will correctly grant
		// object creation privs for all object types

		Ganymede.catTransport = new CategoryTransport(Ganymede.db.rootCategory, 
							      Ganymede.internalSession); // *sync* possible on this
		Ganymede.db.notifyAll(); // in case of locks
	      }
	  }

	return Ganymede.catTransport;
      }
    else
      {
	// not in supergash mode.. download a subset of the category tree to the user

	CategoryTransport transport;

	synchronized (Ganymede.db)
	  {
	    transport = new CategoryTransport(Ganymede.db.rootCategory, this);
	    Ganymede.db.notifyAll(); // in case of locks
	  }

	if (debug)
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
   *
   * Returns a serialized representation of the object types
   * defined on the server.  This BaseListTransport object
   * will not include field information.  The client is
   * obliged to call getFieldTemplateVector() on any
   * bases that it needs field information for.<br><br>
   *
   * This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the BaseListTransport
   * constructor calls other synchronized methods on GanymedeSession
   *
   * @see arlut.csd.ganymede.BaseListTransport
   * @see arlut.csd.ganymede.Session
   */

  public synchronized BaseListTransport getBaseList()
  {
    checklogin();

    /* - */

    if (supergashMode)
      {
	return Ganymede.baseTransport;
      }
    else
      {
	return new BaseListTransport(this); // *sync* on DBStore, this GanymedeSession
      }
  }

  /**
   *
   * Returns a vector of field definition templates, in display order.<br><br>
   *
   * This vector may be cached, as it is static for this object type over
   * the lifetime of any GanymedeSession.
   *
   * @see arlut.csd.ganymede.FieldTemplate
   * @see arlut.csd.ganymede.Session
   */

  public synchronized Vector getFieldTemplateVector(short baseId)
  {
    checklogin();

    /* - */

    Vector results = new Vector();
    Enumeration enum;
    DBObjectBaseField fieldDef;
    DBObjectBase base;

    /* -- */

    base = Ganymede.db.getObjectBase(baseId);

    synchronized (base)
      {
	enum = base.sortedFields.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();

	    results.addElement(fieldDef.template);
	  }
      }

    return results;
  }


  /**
   *
   * This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).<br><br>
   *
   * Currently each client can only have one transaction open.. it
   * is an error to call openTransaction() while another transaction
   * is still open, and an error dialog will be returned in that case.<br><br>
   *
   * @param describe An optional string containing a comment to be
   * stored in the modification history for objects modified by this
   * transaction.
   * 
   * @see arlut.csd.ganymede.Session
   */

  public synchronized ReturnVal openTransaction(String describe)
  {
    checklogin();

    if (session.editSet != null)
      {
	return Ganymede.createErrorDialog("Server: Error in openTransaction()",
					  "Error.. transaction already opened");
      }

    /* - */

    session.openTransaction(describe); // *sync* DBSession

    this.status = "Transaction: " + describe;
    setLastEvent("openTransaction");

    return null;
  }

  /**
   *
   * This method call causes the server to checkpoint the current
   * state of an open transaction on the server.  At any time between
   * the checkpoint() call and a concluding commitTransaction() or
   * abortTransaction() thereafter, the server can be instructed to
   * revert the transaction to the state at the time of this
   * checkpoint by calling rollback() with the same key.<br><br>
   *
   * Checkpointing only makes sense in the context of a transaction;
   * it is an error to call either checkpoint() or rollback() if
   * the server does not have a transaction open.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public synchronized void checkpoint(String key)
  {
    checklogin();

    if (session.editSet == null)
      {
	throw new RuntimeException("checkpoint called in the absence of a transaction");
      }

    /* - */

    session.checkpoint(key);
    setLastEvent("checkpoint:" + key);
  }

  /**
   *
   * This method call causes the server to roll back the state
   * of an open transaction on the server.<br><br>
   *
   * Checkpoints are held in a Stack on the server;  it is never
   * permissible to try to 'rollforward' to a checkpoint that
   * was itself rolled back.  That is, the following sequence is 
   * not permissible.<br><br>
   *
   * <pre>
   * checkpoint("1");
   * &lt;changes&gt;
   * checkpoint("2");
   * &lt;changes&gt;
   * rollback("1");
   * rollback("2");
   * </pre>
   *
   * At the time that the rollback("1") call is made, the server
   * forgets everything that has occurred in the transaction since
   * checkpoint 1.  checkpoint 2 no longer exists, and so the second
   * rollback call will return false.<br><br>
   *
   * Checkpointing only makes sense in the context of a transaction;
   * it is an error to call either checkpoint() or rollback() if
   * the server does not have a transaction open.
   *
   * @return true if the rollback could be carried out successfully.
   * 
   * @see arlut.csd.ganymede.Session
   *
   */

  public synchronized boolean rollback(String key)
  {
    checklogin();

    if (session.editSet == null)
      {
	throw new RuntimeException("rollback called in the absence of a transaction");
      }

    /* - */

    setLastEvent("rollback:" + key);

    return session.rollback(key);
  }

  /**
   *
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.<br><br>
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
   * to commitTransaction().<br><br>
   *
   * This method is synchronized to avoid nested-monitor deadlock in
   * DBSession.commitTransaction().
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
   * @see arlut.csd.ganymede.Session 
   *
   */

  public synchronized ReturnVal commitTransaction(boolean abortOnFail)
  {
    checklogin();

    if (session.editSet == null)
      {
	return Ganymede.createErrorDialog("Server: Error in commitTransaction()",
					  "Error.. no transaction in progress");
      }

    /* - */

    ReturnVal retVal;

    /* -- */

    this.status = "";
    setLastEvent("commitTransaction");

    retVal = session.commitTransaction(); // *sync* DBSession DBEditSet

    // if we succeeded, we'll schedule our
    // builder tasks to run

    if (retVal == null || retVal.didSucceed())
      {
	Ganymede.runBuilderTasks();
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
   *
   * This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.<br><br>
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
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems.  If the transaction was
   *         forcibly terminated due to a major error, the 
   *         doNormalProcessing flag in the returned ReturnVal will be
   *         set to false.
   * 
   */

  public ReturnVal commitTransaction()
  {
    return commitTransaction(false);
  }

  /**
   *
   * This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized ReturnVal abortTransaction()
  {
    checklogin();

    if (session.editSet == null)
      {
	throw new IllegalArgumentException("no transaction in progress");
      }

    /* - */

    this.status = "";
    setLastEvent("abortTransaction");

    return session.abortTransaction(); // *sync* DBSession 
  }

  /**
   *
   * This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.<br><br>
   *
   * body and HTMLbody are StringBuffer's instead of Strings because RMI
   * has a 64k serialization limit on the String class.
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The content of the message.
   *
   */

  public void sendMail(String address, String subject, StringBuffer body)
  {
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

    signature.append("This message was sent by ");
    signature.append(username);
    signature.append(", who is running the Ganymede client on ");
    signature.append(clienthost);
    signature.append(".");

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

  /**
   *
   * This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.<br><br>
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

  public void sendHTMLMail(String address, String subject, StringBuffer body, StringBuffer HTMLbody)
  {
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

    signature.append("This message was sent by ");
    signature.append(username);
    signature.append(", who is running the Ganymede client on ");
    signature.append(clienthost);
    signature.append(".");

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

  /**
   *
   * This is a method to allow code in the server to quickly and
   * safely get a full list of objects in an object base.
   *
   * @return a Vector of DBObject references.
   * 
   */

  public Vector getObjects(short baseid)
  {
    Vector bases = new Vector();
    Vector results = new Vector();
    DBLock readLock = null;
    DBObjectBase base;
    Enumeration enum;

    /* -- */

    checklogin();

    base = Ganymede.db.getObjectBase(baseid);
    bases.addElement(base);

    try
      {
	readLock = session.openReadLock(bases);
      }
    catch (InterruptedException ex)
      {
      }

    if (readLock == null || !readLock.isLocked())
      {
	return null;
      }

    enum = base.objectTable.elements();

    while (enum.hasMoreElements() && readLock.isLocked())
      {
	results.addElement(enum.nextElement());
      }

    session.releaseLock(readLock);

    return results;
  }

  /**
   *
   * This method allows the client to get a status update on a
   * specific list of invids.<br><br>
   *
   * If any of the invids are not currently defined in the server, or
   * if the client doesn't have permission to view any of the invids,
   * those invids' status will not be included in the returned
   * QueryResult.
   *
   * @param invidVector Vector of Invid's to get the status for.
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized QueryResult queryInvids(Vector invidVector)
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
   * This method provides the hook for doing a fast, full or partial,
   * database dump to a string form.
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   *
   * @see arlut.csd.ganymede.Session
   * 
   */

  public synchronized DumpResult dump(Query query)
  {
    checklogin();

    /* - */

    DumpResult result;

    /**
     *
     * What base is the query being done on?
     *
     */

    DBObjectBase base = null;

    /**
     *
     * What sort of result should we give back?   If the query
     * was done on an embedded type, we're going to return
     * the object(s) that ultimately contain matching results.
     *
     */

    DBObjectBase containingBase = null;
    DBObject obj;

    DBField dbf;
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

    if (query.returnType != -1)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnType);
      }
    else if (query.returnName != null)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnName);
      }

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    if (containingBase == null)
      {
	setLastError("No such return type");
	return null;
      }

    embedded = base.isEmbedded();

    if (debug)
      {
	Ganymede.debug("Processing dump query\nSearching for matching objects of type " + base.getName());
      }

    setLastEvent("dump");

    if (embedded)
      {
	if (debug)
	  {
	    Ganymede.debug("Searching for results of type " + containingBase.getName());
	  }
      }

    if (debug)
      {
	if (query.permitList == null)
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

    for (int i = 0; i < containingBase.sortedFields.size(); i++)
      {
	field = (DBObjectBaseField) containingBase.sortedFields.elementAt(i);
	
	if (query.permitList == null)
	  {
	    // If they haven't specified the list of fields they want
	    // back, make sure we don't show them built in fields and
	    // we don't show them the objects owned field in the
	    // OwnerBase.. that could entail many thousands of objects
	    // listed.  If they really, really want to see them, let
	    // them say so explicitly.

	    if (!field.isBuiltIn() && 
		!(containingBase.getTypeID() == SchemaConstants.OwnerBase &&
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
	else if (query.permitList.get(field.getKey()) != null)
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

    result = new DumpResult(fieldDefs);

    // and encode the desired fields into the result

    if (temp_result != null)
      {
	Invid invid;

	Enumeration enum = temp_result.invidHash.keys();

	while (enum.hasMoreElements())
	  {
	    invid = (Invid) enum.nextElement();

	    if (debug)
	      {
		System.err.print(".");
	      }

	    // it's okay to use session.viewDBObject because
	    // DumpResult.addRow() uses the GanymedeSession reference
	    // we pass in to handle per-field permissions
	    //
	    // using view_db_object() here would be disastrous,
	    // because it would entail making duplicates of all
	    // objects matching our query

	    result.addRow(session.viewDBObject(invid), this);
	  }
      }

    if (debug)
      {
	Ganymede.debug("dump(): completed processing, returning buffer");
      }

    return result;
  }

  /**
   *
   * This method provides the hook for doing all
   * manner of simple object listing for the Ganymede
   * database.  
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   *
   * @see arlut.csd.ganymede.Session
   */

  public QueryResult query(Query query)
  {
    checklogin();
    return queryDispatch(query, false, true, null, null);
  }

  /**
   *
   * This method is a server-side method for doing object listing
   * with support for DBEditObject's lookupLabel method.
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   *
   */

  public QueryResult query(Query query, DBEditObject perspectiveObject)
  {
    checklogin();

    QueryResult result = queryDispatch(query, false, true, null, perspectiveObject);

    // any associated queries?

    if (query.linkedQueries != null)
      {
	for (int i = 0; i < query.linkedQueries.size(); i++)
	  {
	    result = result.intersection(queryDispatch((Query) query.linkedQueries.elementAt(i), 
						       false, true, null, perspectiveObject));
	  }
      }

    return result;
  }

  /**
   *
   * This method provides the hook for doing all
   * manner of internal object listing for the Ganymede
   * database.  Unfiltered.
   *
   * @return A Vector of Result objects
   *
   * @see arlut.csd.ganymede.Query
   * @see arlut.csd.ganymede.Result
   */

  public Vector internalQuery(Query query)
  {
    Vector result = new Vector();
    QueryResult internalResult = queryDispatch(query, true, false, null, null);
    Invid key;
    String val;
    Enumeration enum;

    /* -- */

    if (internalResult != null)
      {
	enum = internalResult.invidHash.keys();

	while (enum.hasMoreElements())
	  {
	    key = (Invid) enum.nextElement();
	    val = (String) internalResult.invidHash.get(key);

	    result.addElement(new Result(key, val));
	  }
      }

    return result;
  }

  /**
   *
   * This method is the primary Query engine for the Ganymede
   * databases.  It is used by dump(), query(), and internalQuery().<br><br>
   *
   * This method is partially synchronized to prevent deadlock problems when
   * extantLock is null.
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
   * for use in a particular context.  If non-null, perspectiveObject's lookupLabel()
   * method will be used to generate the label for a result entry.
   *
   */

  public synchronized QueryResult queryDispatch(Query query, boolean internal, 
						boolean forTransport, DBLock extantLock,
						DBEditObject perspectiveObject)
  {
    QueryResult result = new QueryResult(forTransport);
    DBObjectBase base = null;
    DBObjectBase containingBase = null;
    Enumeration enum;
    DBObject obj;
    DBLock rLock = null;

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
	base = Ganymede.db.getObjectBase(query.objectName); // *sync* DBStore
      }

    if (query.returnType != -1)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnType);
      }
    else if (query.returnName != null)
      {
	containingBase = Ganymede.db.getObjectBase(query.returnName); // *sync* DBStore
      }

    if (base == null)
      {
	setLastError("No such base");
	return null;
      }

    if (containingBase == null)
      {
	setLastError("No such return type");
	return null;
      }

    if (debug)
      {
	// is this base corresponding to an embedded object?

	if (base.isEmbedded())
	  {
	    System.err.println("Query on embedded type: " + base.getName());
	  }
	else
	  {
	    System.err.println("Query on non-embedded type " + base.getName());
	  }
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

	    // the whole query system is predicated on returning top-level
	    // objects only, so we have to do it here, too.

	    resultobject = getContainingObj(resultobject);

	    addResultRow(resultobject, query, result, internal, perspectiveObject);

	    // if we have any linked queries here, handle that.  It
	    // would be weird if someone wanted a particular invid
	    // only if some criteria on an embedded object was
	    // acceptable, but who are we to pre-judge?

	    return intersectQueries(query, result, null);
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
	    // we're looking for the label of an object

	    if (node.fieldId == -1 && base.getLabelField() != -1)
	      {
		fieldDef = (DBObjectBaseField) base.getField(base.getLabelField()); // *sync* DBObjectBase
	      }
	    else
	      {
		setLastError("Couldn't find field identifier in query optimizer");
		return null;
	      }
	  }

	if (fieldDef == null)
	  {
	    Ganymede.debug("ERROR: wound up with a null fieldDef in query optimizer");
	    return null;
	  }

	// now we've got the field definition.. is it tracked in a namespace hash?

	if (fieldDef.namespace != null)
	  {
	    // aha!  We've got an optimized case!
	    
	    if (debug)
	      {
		System.err.println("Eureka!  Optimized query!");
	      }

	    DBObject resultobject;
	    DBNameSpace ns = fieldDef.namespace;

	    synchronized (ns)
	      {
		DBField resultfield = ns.lookup(node.value); // *sync* DBNameSpace

		// note that DBNameSpace.lookup() will always point us
		// to a field that is currently in the main database,
		// so we don't need to worry that the namespace slot
		// is pointing to a field in a checked-out object.
		
		if (resultfield == null)
		  {
		    return intersectQueries(query, result, null);
		  }
		else
		  {
		    // a namespace can map across different field and
		    // object types.. make sure we've got an instance
		    // of the right kind of field

		    if (resultfield.definition != fieldDef)
		      {
			return intersectQueries(query, result, null);
		      }

		    resultobject = resultfield.owner;

		    // if we're matching an embedded, we want to return our
		    // container.

		    resultobject = getContainingObj(resultobject);
		    
		    // addResultRow() will do our permissions checking for us

		    addResultRow(resultobject, query, result, internal, perspectiveObject);
		
		    if (debug)
		      {
			System.err.println("Returning result from optimized query");
		      }
		
		    return intersectQueries(query, result, null);
		  }
	      }
	  }
      }

    // now we need to generate a vector listing the object bases that
    // need to be locked to perform this query.  Note that we need to
    // get each of these bases locked at the same time to avoid potential
    // deadlock situations.  DBSession.openReadLock() will take care of
    // that for us by taking a vector to lock.

    Vector baseLock = new Vector();

    baseLock.addElement(base);

    // lock the containing base as well, if it differs.. this will
    // keep things consistent

    if (containingBase != null)
      {
	VectorUtils.unionAdd(baseLock, containingBase);
      }

    // if we have linked queries, we'll want to lock all bases that
    // get touched.
    
    if (query.linkedQueries != null)
      {
	DBObjectBase adjunctBase = null;
	DBObjectBase adjunctContainingBase = null;

	for (int i = 0; i < query.linkedQueries.size(); i++)
	  {
	    Query adjunctQuery = (Query) query.linkedQueries.elementAt(i);

	    // the Query spec says that linked queries won't have their
	    // linked queries honored.. enforce that here.

	    if (adjunctQuery.linkedQueries != null)
	      {
		Ganymede.debug("GanymedeSession.queryDispatch(): linked queries have linked queries");

		adjunctQuery.linkedQueries = null;
	      }
	    
	    if (adjunctQuery.objectType != -1)
	      {
		adjunctBase = Ganymede.db.getObjectBase(adjunctQuery.objectType);
	      }
	    else if (adjunctQuery.objectName != null)
	      {
		adjunctBase = Ganymede.db.getObjectBase(adjunctQuery.objectName);
	      }

	    VectorUtils.unionAdd(baseLock, adjunctBase);

	    if (adjunctQuery.returnType != -1)
	      {
		adjunctContainingBase = Ganymede.db.getObjectBase(adjunctQuery.returnType);
	      }
	    else if (adjunctQuery.returnName != null)
	      {
		adjunctContainingBase = Ganymede.db.getObjectBase(adjunctQuery.returnName);
	      }

	    VectorUtils.unionAdd(baseLock, adjunctContainingBase);
	  }
      }

    if (debug)
      {
	System.err.println("Query: " + username + " : opening read lock on " + VectorUtils.vectorString(baseLock));
      }

    // okay.. now we want to lock the database, handle the search, and
    // return results.  We'll depend on the try..catch to handle
    // releasing the read lock if it is one we open.

    try
      {
	if (extantLock != null) 
	  {
	    // check to make sure that the lock we were passed in has everything
	    // locked that we'll need to examine.

	    for (int i = 0; i < baseLock.size(); i++)
	      {
		if (!extantLock.isLocked((DBObjectBase) baseLock.elementAt(i)))
		  {
		    throw new IllegalArgumentException("error, didn't have base " + 
						       baseLock.elementAt(i) +
						       " locked with extantLock");
		  }
	      }

	    rLock = extantLock;
	  }
	else
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

	if (debug)
	  {
	    System.err.println("Query: " + username + " : got read lock");
	  }

	// iterate over the objects in the base we're searching on,
	// looking for matching objects.  Note that we need to check
	// in here to see if we've had our DBSession's logout() method
	// called.. this shouldn't really ever happen here due to
	// synchronization on GanymedeSession, but if somehow it does
	// happen, we want to go ahead and break out of our query.  We
	// could well have our logged_in flag cleared during execution
	// of a query, so we'll check that as well.

	enum = base.objectTable.elements();

	while (logged_in && session.isLocked(rLock) && enum.hasMoreElements())
	  {
	    obj = (DBObject) enum.nextElement();

	    if (DBQueryHandler.matches(this, query, obj))
	      {
		// if we are processing an embedded type, we want to add
		// the ultimate container of the embedded object to the
		// result list

		obj = getContainingObj(obj);

		if (obj == null)
		  {
		    if (debug)
		      {
			Ganymede.debug("queryDispatch(): Couldn't find containing object!");
		      }

		    continue;	// try next match
		  }

		addResultRow(obj, query, result, internal, perspectiveObject);
	      }
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
	// that are not yet held in the database.

	if (session.isTransactionOpen())
	  {
	    if (debug)
	      {
		System.err.println("Query: " + username +
				   " : scanning intratransaction objects");
	      }

	    synchronized (session.editSet)
	      {
		enum = session.editSet.objects.elements();

		while (enum.hasMoreElements())
		  {
		    DBEditObject transaction_object = (DBEditObject) enum.nextElement();

		    // don't consider objects of the wrong type here.

		    if (transaction_object.getTypeID() != query.objectType)
		      {
			continue;
		      }

		    // don't consider objects we already have stored in the result

		    if (result.containsInvid(transaction_object.getInvid()))
		      {
			if (debug)
			  {
			    System.err.println("don't need to add invid " + transaction_object.getInvid() +
					       ", we got it first time");
			  }

			continue;
		      }

		    if (transaction_object.getStatus() == ObjectStatus.CREATING ||
			transaction_object.getStatus() == ObjectStatus.EDITING)
		      {
			if (DBQueryHandler.matches(this, query, transaction_object))
			  {
			    obj = getContainingObj(transaction_object);

			    if (obj == null)
			      {
				Ganymede.debug("Error, couldn't find a containing object for an embedded query");
				continue;	// try next match
			      }

			    // make sure we've found an object of the
			    // proper type.. if we're not querying on an
			    // embedded object type, the above clause
			    // won't have been run.

			    // DBQueryHandler.matches() doesn't check
			    // object type, so we need to do it here
			    // before we add this to our result.

			    if (obj.getTypeID() != containingBase.getTypeID())
			      {
				if (debug)
				  {
				    Ganymede.debug("queryDispatch(): Type mismatch in object open in trans!");
				  }

				continue;
			      }

			    addResultRow(obj, query, result, internal, perspectiveObject);
			  }
		      }
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

	// now handle any linked queries, return the results to the
	// user.

	return intersectQueries(query, result, rLock);
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
   *
   * This private method takes care of intersecting the result
   * of a query operation against any linked queries to filter
   * a cluster of queries.  This is only of interest when performing
   * a query on both embedded and non-embedded fields of a given
   * object type.
   * 
   */

  private final QueryResult intersectQueries(Query query, QueryResult temp_result, DBLock rLock)
  {
    if (query.linkedQueries != null)
      {
	if (debug)
	  {
	    System.err.println("Primary query result: " + temp_result.size());
	  }

	for (int i = 0; i < query.linkedQueries.size(); i++)
	  {
	    Query adjunctQuery = (Query) query.linkedQueries.elementAt(i);

	    // note that we pass rLock into queryDispatch so that we re-use our
	    // lock

	    QueryResult adjunctResult = queryDispatch(adjunctQuery, false, false,
						      rLock, null);

	    if (debug)
	      {
		System.err.println("Adjunct query (" + adjunctQuery.objectName +
				   ") result: " + adjunctResult.size());
	      }

	    if (temp_result != null)
	      {
		temp_result = temp_result.intersection(adjunctResult);

		if (debug)
		  {
		    System.err.println("Intersection result: " + temp_result.size());
		  }
	      }
	  }
      }

    return temp_result;
  }

  /**
   *
   * This private method takes care of adding an object to a query
   * result, checking permissions and what-not as needed.
   * 
   */

  private final void addResultRow(DBObject obj, Query query, 
				  QueryResult result, boolean internal,
				  DBEditObject perspectiveObject)
  {
    PermEntry perm;

    /* -- */

    if (!supergashMode && !internal)
      {
	perm = getPerm(obj);

	if (perm == null ||
	    (query.editableOnly || !perm.isVisible()) &&
	    (!query.editableOnly || !perm.isEditable()))
	  {
	    return;		// permissions prohibit us from adding this result
	  }
      }
    else
      {
	// we'll report it as editable

	perm = PermEntry.fullPerms;
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
	DBEditObject x;
	
	if (session.isTransactionOpen())
	  {
	    // Do we have a version of this object checked out?

	    x = session.editSet.findObject(obj.getInvid()); // *sync* DBEditSet

	    if (x == null)
	      {
		// nope, go ahead and return the object as we found it in the
		// main hash

		if (perspectiveObject == null)
		  {
		    result.addRow(obj.getInvid(), obj.getLabel(), obj.isInactivated(),
				  obj.willExpire(), obj.willBeRemoved(), perm.isEditable());
		  }
		else
		  {
		    result.addRow(obj.getInvid(), perspectiveObject.lookupLabel(obj), 
				  obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
				  perm.isEditable());
		  }
	      }
	    else
	      {
		// yup we found it.. if we are deleting or dropping it, we don't want 
		// to return it
		
		if (x.getStatus() != ObjectStatus.DELETING &&
		    x.getStatus() != ObjectStatus.DROPPING)
		  {
		    // ok, we're editing it or creating it.. now, does it still meet
		    // the search criteria?
		    
		    if (DBQueryHandler.matches(this, query, x))
		      {
			if (perspectiveObject == null)
			  {
			    result.addRow(x.getInvid(), x.getLabel(), 
					  x.isInactivated(), x.willExpire(), x.willBeRemoved(),
					  true);
			  }
			else
			  {
			    result.addRow(x.getInvid(), perspectiveObject.lookupLabel(x), 
					  x.isInactivated(), x.willExpire(), x.willBeRemoved(),
					  true);
			  }
			// we must be able to edit it, since it's checked out
		      }
		  }
	      }
	  }
	else
	  {
	    // we don't have a transaction open, so there's no worry
	    // about us having a different version of the object open
	    // in our transaction
	    
	    if (perspectiveObject == null)
	      {
		result.addRow(obj.getInvid(), obj.getLabel(), 
			      obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			      perm.isEditable());
	      }
	    else
	      {
		result.addRow(obj.getInvid(), perspectiveObject.lookupLabel(obj), 
			      obj.isInactivated(), obj.willExpire(), obj.willBeRemoved(),
			      perm.isEditable());
	      }
	  }
      }
  }

  /**
   *
   * This method is intended as a lightweight way of returning the
   * current label of the specified invid.  No locking is done,
   * and the label returned will be viewed through the context
   * of the current transaction, if any.
   *
   * @see arlut.csd.ganymede.Session
   */ 

  public String viewObjectLabel(Invid invid)
  {
    checklogin();

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
   *
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @see arlut.csd.ganymede.Session
   */

  public StringBuffer viewObjectHistory(Invid invid, Date since)
  {
    DBObject obj;

    /* -- */

    checklogin();

    if (invid == null)
      {
	setLastError("Null invid passed into viewObjectHistory");
	return null;
      }

    // we do our own permissions checking, so we can use
    // session.viewDBObject().

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException("argh!! null object in viewObjectHistory on invid " +
				       invid.toString());
      }

    if (!getPerm(obj).isVisible())
      {
	setLastError("Permissions denied to view the history for this invid.");
	return null;
      }

    if (Ganymede.log == null)
      {
	setLastError("Log not active, can't view invid history");
	return null;
      }

    return Ganymede.log.retrieveHistory(invid, since, false); // *sync* DBLog 
  }

  /**
   *
   * This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.
   *
   * @param invid The invid identifier for the admin Persona whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @return A String containing a record of events for the Invid in question,
   * or null if permissions are denied to view the history.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public StringBuffer viewAdminHistory(Invid invid, Date since)
  {
    DBObject obj;

    /* -- */

    checklogin();

    if (invid == null)
      {
	setLastError("Null invid passed into viewObjectHistory");
	return null;
      }

    if (invid.getType() != SchemaConstants.PersonaBase)
      {
	setLastError("Wrong type of invid passed into viewAdminHistory");
	return null;
      }

    // we do our own permissions checking, so we can use session.viewDBObject().

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	throw new NullPointerException("argh!! null obj in viewAdminHistory on " +
				       invid.toString());
      }

    if (!getPerm(obj).isVisible())
      {
	setLastError("Permissions denied to view the history for this invid.");
	return null;
      }

    if (Ganymede.log == null)
      {
	setLastError("Log not active, can't view invid history");
	return null;
      }

    return Ganymede.log.retrieveHistory(invid, since, true); // *sync* DBLog
  }

  /**
   * View an object from the database.  The ReturnVal returned will
   * carry a db_object reference, which can be obtained by the client
   * calling ReturnVal.getObject().  If the object could not be
   * viewed for some reason, the ReturnVal will carry an encoded error
   * dialog for the client to display.<br><br>
   *
   * view_db_object() can be done at any time, outside of the bounds of
   * any transaction.  view_db_object() returns a snapshot of the object's
   * state at the time the view_db_object() call is processed, and will
   * be transaction-consistent internally.<br><br>
   *
   * If view_db_object() is called during a transaction, the object
   * will be returned as it stands during the transaction.. that is,
   * if the object has been changed during the transaction, that
   * changed object will be returned, even if the transaction has
   * not yet been committed, and other clients would not be able to
   * see that version of the object.<br><br>
   *
   * NOTE: It is critical that any code that looks at the values of
   * fields in a DBObject go through a view_db_object() method
   * or else the object will not properly know who owns it, which
   * is critical for it to be able to properly authenticate field
   * access.  Keep in mind, however, that view_db_object clones the
   * DBObject in question, so this method is very heavyweight.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   * 
   * @see arlut.csd.ganymede.Session
   *
   */

  public ReturnVal view_db_object(Invid invid)
  {
    ReturnVal result;
    db_object objref;
    DBObject obj;
    PermEntry perm;
    
    /* -- */

    checklogin();

    // let a NullPointerException be thrown if we were given a null
    // Invid.

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	return Ganymede.createErrorDialog("Object Not Found",
					  "Could not find object " + invid.toString() +
					  " in the database.  Perhaps the object doesn't exist?");
      }

    if (getPerm(obj).isVisible())
      {
	setLastEvent("view_db_object: " + obj.getLabel());

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
	result.setObject(objref);

	if (Ganymede.remotelyAccessible)
	  {
	    // the exportObject call will fail if the object has
	    // already been exported.  Unfortunately, there doesn't
	    // seem to be much way to tell this beforehand, so
	    // we won't bother to try.

	    try
	      {
		UnicastRemoteObject.exportObject(objref);
	      }
	    catch (RemoteException ex)
	      {
		//		    ex.printStackTrace();
	      }

	    ((DBObject) objref).exportFields();
	  }

	return result;
      }
    else
      {
	return Ganymede.createErrorDialog("Permissions Error",
					  "Permission to view object [" + 
					  viewObjectLabel(invid) +
					  " - " + invid + "] denied.");
      }
  }

  /**
   *
   * Check an object out from the database for editing.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public ReturnVal edit_db_object(Invid invid)
  {
    ReturnVal result;
    db_object objref;
    DBObject obj;

    /* -- */

    checklogin();

    obj = session.viewDBObject(invid);

    if (obj == null)
      {
	return Ganymede.createErrorDialog("Object not found",
					  "Error, object [" + invid + "] does not appear to exist.  Couldn't edit it.");
      }

    // we always want to check permissions, even if the object has
    // already been checked out by our DBSession.. some of the
    // InvidDBField Invid binding logic can check out the object for
    // editing in a way that the user's permissions would not normally
    // allow.  By checking perms up front, we may be preventing the
    // user from getting access to the object in supergash context.

    if (getPerm(obj).isEditable())
      {
	setLastEvent("edit_db_object: " + obj.getLabel());

	objref = session.editDBObject(invid);

	if (objref != null)
	  {
	    result = new ReturnVal(true);
	    result.setObject(objref);

	    if (Ganymede.remotelyAccessible)
	      {
		// the exportObject call will fail if the object has
		// already been exported.  Unfortunately, there doesn't
		// seem to be much way to tell this beforehand, so
		// we won't bother to try.

		try
		  {
		    UnicastRemoteObject.exportObject(objref);
		  }
		catch (RemoteException ex)
		  {
		    // ex.printStackTrace();
		  }

		((DBObject) objref).exportFields();
	      }

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

		    return Ganymede.createErrorDialog("Error, object already being edited",
						      obj.getTypeName() + " [" + 
						      viewObjectLabel(invid) + " - " + invid + 
						      "] is already being edited by user " +
						      edit_username + " on host " + edit_hostname);
		  }
	      }

	    return Ganymede.createErrorDialog("Error checking object out for editing",
					      "Error checking out " + obj.getTypeName() + " [" + 
					      viewObjectLabel(invid) + " - " + invid + 
					      "] for editing.\nPerhaps someone else was editing it?");
	  }
      }
    else
      {
	return Ganymede.createErrorDialog("Permissions Error",
					  "Permission to edit " + obj.getTypeName() + " [" + 
					  viewObjectLabel(invid) +
					  " - " + invid + "] denied.");
      }
  }

  /**
   *
   * Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * @param type The kind of object to create.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public ReturnVal create_db_object(short type)
  {
    return this.create_db_object(type, false);
  }

  /**
   *
   * Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.
   *
   * @param type The kind of object to create.
   * @param embedded If true, assume the object created is embedded and
   * does not need to have owners set.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *
   */

  public ReturnVal create_db_object(short type, boolean embedded)
  {
    DBObject newObj;

    /* -- */

    checklogin();
    
    if (getPerm(type, true).isCreatable())
      {
	if (embedded)
	  {
	    newObj = session.createDBObject(type, null); // *sync* DBSession

	    if (newObj == null)
	      {
		return Ganymede.createErrorDialog("Can't create",
						  "Can't create new object, the operation was refused");
	      }

	    setLastEvent("create_db_object: " + newObj.getBase().getName());

	    ReturnVal result = new ReturnVal(true);

	    result.setObject(newObj);

	    if (Ganymede.remotelyAccessible)
	      {
		// the exportObject call will fail if the object has
		// already been exported.  Unfortunately, there doesn't
		// seem to be much way to tell this beforehand, so
		// we won't bother to try.

		try
		  {
		    UnicastRemoteObject.exportObject(newObj);
		  }
		catch (RemoteException ex)
		  {
		    // ex.printStackTrace();
		  }

		((DBObject) newObj).exportFields();
	      }

	    return result;
	  }
	else
	  {
	    if (newObjectOwnerInvids == null)
	      {
		if (ownerList == null)
		  {
		    getOwnerGroups(); // *sync*
		  }

		// if we have only one group possible, we'll assume we're
		// putting it in that, otherwise since the client hasn't
		// done a setDefaultOwner() call, we're gonna have to
		// abort before even trying to create the object.

		if (ownerList.size() != 1)
		  {
		    return Ganymede.createErrorDialog("Can't create",
						      "Can't create new object, no way of knowing which " +
						      "owner group to place it in");
		  }
	      }

	    // calculate ownership for this object

	    Vector ownerInvids = new Vector();

	    // we may have either a vector of Invids in
	    // newObjectOwnerInvids, or a query result containing a list
	    // of a single Invid
	
	    if (newObjectOwnerInvids != null)
	      {
		for (int i = 0; i < newObjectOwnerInvids.size(); i++)
		  {
		    ownerInvids.addElement(newObjectOwnerInvids.elementAt(i));
		  }
	      }
	    else
	      {
		ownerInvids.addElement(ownerList.getInvid(0));
	      }

	    newObj = session.createDBObject(type, ownerInvids); // *sync* DBSession

	    if (newObj == null)
	      {
		return Ganymede.createErrorDialog("Can't create",
						  "Can't create new object, the operation was refused");
	      }
	  }
      }
    else
      {
	DBObjectBase base = Ganymede.db.getObjectBase(type);
	
	String error;

	if (base == null)
	  {
	    error = "Permission to create object of *invalid* type " + type + " denied.";
	  }
	else
	  {
	    error = "Permission to create object of type " + base.getName() + " denied.";
	  }

	return Ganymede.createErrorDialog("Can't create",
					  error);
      }

    setLastEvent("create_db_object: " + newObj.getBase().getName());

    ReturnVal result = new ReturnVal(true);

    result.setObject(newObj);

    if (Ganymede.remotelyAccessible)
      {
	// the exportObject call will fail if the object has
	// already been exported.  Unfortunately, there doesn't
	// seem to be much way to tell this beforehand, so
	// we won't bother to try.

	try
	  {
	    UnicastRemoteObject.exportObject(newObj);
	  }
	catch (RemoteException ex)
	  {
	    // ex.printStackTrace();
	  }

	((DBObject) newObj).exportFields();
      }

    return result;
  }

  /**
   *
   * Clone a new object from object &lt;invid&gt;.  The ReturnVal returned
   * will carry a db_object reference, which can be obtained by the
   * client calling ReturnVal.getObject().  If the object could not
   * be checked out for editing for some reason, the ReturnVal will
   * carry an encoded error dialog for the client to display.<br><br>
   *
   * This method must be called within a transactional context.<br><br>
   *
   * Typically, only certain values will be cloned.  What values are
   * retained is up to the specific code module provided for the
   * invid type of object.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *    
   * @see arlut.csd.ganymede.Session
   *
   */

  public ReturnVal clone_db_object(Invid invid)
  {
    checklogin();

    return Ganymede.createErrorDialog("Invalid operation",
				      "clone_db_object is not yet implemented.");	      
  }

  /**
   * Inactivate an object in the database<br><br>
   *
   * This method must be called within a transactional context.<br><br>
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
   * @see arlut.csd.ganymede.Session
   */

  public synchronized ReturnVal inactivate_db_object(Invid invid) 
  {
    DBObject vObj;
    DBEditObject eObj;

    /* -- */

    checklogin();

    vObj = session.viewDBObject(invid);

    if (vObj == null)
      {
	setLastError("Can't inactivate non-existent object");

	return Ganymede.createErrorDialog("Server: Error in inactivate_db_object()",
					  "Error.. can't inactivate non-existent object");
      }

    if (vObj.isInactivated())
      {
	return Ganymede.createErrorDialog("Server: Can't inactivate an inactive object",
					  "Error.. can't inactivate " + vObj.getTypeName() + " " + 
					  vObj.getLabel() + ", object is already inactivated.");
      }

    if (!getPerm(vObj).isDeletable())
      {
	setLastError("Don't have permission to inactivate " + 
		     vObj.getTypeName() + " " + vObj.getLabel());

	return Ganymede.createErrorDialog("Server: Error in inactivate_db_object()",
					  "Don't have permission to inactivate " +
					  vObj.getTypeName() + " " + vObj.getLabel());
      }

    ReturnVal result = edit_db_object(invid); // *sync* DBSession DBObject

    eObj = (DBEditObject) result.getObject();

    if (eObj == null)
      {
	return Ganymede.createErrorDialog("Server: Error in inactivate_db_object()",
					  "Couldn't check out this object for inactivation");
      }

    if (!eObj.canBeInactivated() || !eObj.canInactivate(session, eObj))
      {
	return Ganymede.createErrorDialog("Server: Error in inactivate_db_object()",
					  "Object " + eObj.getLabel() +
					  " is not of a type that may be inactivated");
      }

    setLastEvent("inactivate_db_object: " + eObj.getLabel());

    // note!  DBEditObject's finalizeInactivate() method does the
    // event logging

    return session.inactivateDBObject(eObj); // *sync* DBSession 
  }

  /**
   *
   * Reactivates an inactivated object in the database<br><br>
   *
   * This method is only applicable to inactivated objects.  For such,
   * the object will be reactivated if possible, and the removal date
   * will be cleared.  The object may retain an expiration date,
   * however.<br><br>
   *
   * The client should check the returned ReturnVal's
   * getObjectStatus() method to see whether the re-activated object
   * has an expiration date set.
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized ReturnVal reactivate_db_object(Invid invid)
  {
    DBObject vObj;
    DBEditObject eObj;

    /* -- */

    checklogin();

    vObj = session.viewDBObject(invid);

    if (vObj == null)
      {
	return Ganymede.createErrorDialog("Server: Error in reactivate_db_object()",
					  "Error.. can't inactivate non-existent object");
      }

    if (!vObj.isInactivated())
      {
	return Ganymede.createErrorDialog("Server: Error in reactivate_db_object()",
					  vObj.getTypeName() + " " + vObj.getLabel() +
					  " is not inactivated");
      }

    // we'll treat the object's deletion bit as the power-over-life-and-death bit

    if (!getPerm(vObj).isDeletable())
      {
	return Ganymede.createErrorDialog("Server: Error in reactivate_db_object()",
					  "Don't have permission to reactivate " + 
					  vObj.getTypeName() + " " + vObj.getLabel());
      }

    ReturnVal result = edit_db_object(invid); // *sync* DBSession DBObject

    eObj = (DBEditObject) result.getObject();

    if (eObj == null)
      {
	return Ganymede.createErrorDialog("Server: Error in reactivate_db_object()",
					  "Couldn't check out this object for reactivation");
      }

    setLastEvent("reactivate_db_object: " + eObj.getLabel());

    // note!  DBEditObject's finalizeReactivate() method does the
    // event logging at transaction commit time

    return session.reactivateDBObject(eObj); // *sync* DBSession, DBObject possible
  }

  /**
   * Remove an object from the database<br><br>
   *
   * This method must be called within a transactional context.<br><br>
   *
   * Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   *
   * @see arlut.csd.ganymede.Session
   */

  public synchronized ReturnVal remove_db_object(Invid invid) 
  {
    checklogin();

    if (debug)
      {
	Ganymede.debug("Attempting to delete object: " + invid);
      }

    if ((invid.getType() == SchemaConstants.RoleBase) &&
	(invid.getNum() == SchemaConstants.RoleDefaultObj))
      {
	return Ganymede.createErrorDialog("Server: Error in remove_db_object()",
					  "Error.. can't delete default permissions definitions");
      }

    if ((invid.getType() == SchemaConstants.PersonaBase) &&
	(invid.getNum() == 1))
      {
	return Ganymede.createErrorDialog("Server: Error in remove_db_object()",
					  "Error.. can't delete " + 
					  Ganymede.rootname + " persona");
      }

    DBObjectBase objBase = Ganymede.db.getObjectBase(invid.getType());
    DBObject vObj = session.viewDBObject(invid);

    if (vObj == null)
      {
	return Ganymede.createErrorDialog("Server: Error in remove_db_object()",
					  "Error.. can't delete non-existent object");
      }

    // we call canRemove() here so that adopters can override
    // canRemove() in DBEditObject subclasses and by-pass this check

    if (objBase.objectHook.canBeInactivated() && !isSuperGash() && !objBase.objectHook.canRemove(session, vObj))
      {
	return Ganymede.createErrorDialog("Server: Error in remove_db_object()",
					  "You do not have permission to remove " + vObj +
					  ".\n\nOnly supergash-level admins can remove objects of this type," +
					  "other admins must use inactivate.");
      }

    if (!getPerm(vObj).isDeletable())
      {
	return Ganymede.createErrorDialog("Server: Error in remove_db_object()",
					  "Don't have permission to delete object" +
					  vObj.getLabel());
      }

    if (!objBase.objectHook.canRemove(session, vObj))
      {
	return Ganymede.createErrorDialog("Server: Error in remove_db_object()",
					  "Object Manager refused deletion for " + 
					  vObj.getLabel());
      }

    setLastEvent("remove_db_object: " + vObj.getLabel());

    // note!  DBEditObject's finalizeRemove() method does the event logging
    
    return session.deleteDBObject(invid);
  }

  /*******************************************************************************
   *                                                                             *
   * From here on down, the methods are not remotely accessible to the client,   *
   * but are instead for server-side use only.                                   *
   *                                                                             *
   *******************************************************************************/

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
   *
   * Convenience method to get access to this session's user invid.
   *
   */

  public Invid getUserInvid()
  {
    return userInvid;
  }

  // **
  // the following are the non-exported permissions management
  // **

  /**
   *
   * This method finds the ultimate owner of an embedded object
   *
   */

  final DBObject getContainingObj(DBObject object)
  {
    DBObject localObj;
    InvidDBField inf = null;
    Invid inv = null;
    int loopcount = 0;

    /* -- */

    // if we're looking at an embedded object, lets cascade up and
    // find the top-level ancestor

    if (debug)
      {
	System.err.println("Trying to find top-level object for " + 
			   object.getTypeDesc() + ":" + 
			   object.getInvid().toString());
      }

    localObj = object;

    while (localObj != null && localObj.isEmbedded())
      {
	inf = (InvidDBField) localObj.getField(SchemaConstants.ContainerField);

	if (inf == null)
	  {
	    setLastError("getContainingObj() error.. couldn't find owner of embedded object " + 
			 localObj.getLabel());
	    localObj = null;
	    break;
	  }

	inv = (Invid) inf.getValueLocal();

	if (inv == null)
	  {
	    setLastError("getContainingObj() error <2:" + loopcount + ">.. couldn't find owner of embedded object " + 
			 localObj.getLabel());
	    localObj = null;
	    break;
	  }

	// remember, viewDBObject() will get an object even if it was
	// created in the current transaction

	localObj = session.viewDBObject(inv);
	loopcount++;
      }

    if (localObj == null)
      {
	setLastError("getContainingObj() error <3>.. couldn't find owner of embedded object" + 
		     object.getLabel());
	throw new RuntimeException("getContainingObj() couldn't find owner of embedded object");
      }

    return localObj;
  }

  /**
   *
   * This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the object.  getPerm() will or any proprietary
   * ownership bits with the default permissions to give
   * an appopriate result.
   *
   */

  public final PermEntry getPerm(DBObject object)
  {
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

    if (debug)
      {
	System.err.println("GanymedeSession.getPerm(): calling getContainingObj()");
      }
    
    object = getContainingObj(object);

    // does this object type have an override?

    result = object.getBase().getObjectHook().permOverride(this, object);

    if (result != null)
      {
	return result;
      }

    // no override.. do we have an expansion?

    result = object.getBase().getObjectHook().permExpand(this, object);

    // make sure we have personaPerms up to date

    updatePerms(false);		// *sync*

    // if we are operating on behalf of an end user and the object in
    // question happens to be that user's record, we'll act as if we
    // own ourself.  We'll then wind up with the default permission
    // object's objects owned privs.

    useSelfPerm = (userInvid != null) && userInvid.equals(object.getInvid());

    // We also want to check to see whether the custom logic for this
    // object type wants to grant ownership of this object.

    if (!useSelfPerm && object.getBase().getObjectHook().grantOwnership(this, object))
      {
	useSelfPerm = true;
      }

    // If the current persona owns the object, look to our
    // personaPerms to get the permissions applicable, else
    // look at the default perms

    if (useSelfPerm || personaMatch(object))
      {
	if (result == null)
	  {
	    result = personaPerms.getPerm(object.getTypeID());

	    if (result == null)
	      {
		return PermEntry.noPerms;
	      }
	    else
	      {
		return result;
	      }
	  }
	else
	  {
	    return result.union(personaPerms.getPerm(object.getTypeID()));
	  }
      }
    else
      {
	if (result == null)
	  {
	    result = defaultPerms.getPerm(object.getTypeID());

	    if (result == null)
	      {
		return PermEntry.noPerms;
	      }
	    else
	      {
		return result;
	      }
	  }
	else
	  {
	    return result.union(defaultPerms.getPerm(object.getTypeID()));
	  }
      }
  }

  /**
   *
   * This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the field in the object.
   *
   */

  final public PermEntry getPerm(DBObject object, short fieldId)
  {
    PermEntry objPerm;
    PermEntry result;
    DBObject containingObj;
    boolean useSelfPerm = false;
    InvidDBField inf = null;
    Invid inv = null;
    DBField field;
    boolean forceObjPerm = false;

    /* -- */

    if (object == null)
      {
	return null;
      }

    if (supergashMode)
      {
	return PermEntry.fullPerms;
      }

    // does this object type have an override?

    result = object.getBase().getObjectHook().permOverride(this, object, fieldId);

    if (result != null)
      {
	return result;
      }

    // no override.. do we have an expansion?

    result = object.getBase().getObjectHook().permExpand(this, object, fieldId);

    // find the top-level object if we were passed an embedded object
    
    containingObj = getContainingObj(object);

    // make sure we have personaPerms up to date

    updatePerms(false);		// *sync*

    // if we are operating on behalf of an end user and the object in
    // question happens to be that user's record, we may gain some
    // permissions for this object

    useSelfPerm = userInvid != null && userInvid.equals(containingObj.getInvid());

    // we need to get the object permission so that we can inherit
    // permisisons when not specified for the field.. we do this here
    // rather than just calling GanymedeSession.getPerm() to avoid the
    // overhead of checking for a persona match twice.

    // does this object type have an override?

    objPerm = containingObj.getBase().getObjectHook().permOverride(this, containingObj);

    if (objPerm == null)
      {
	objPerm = containingObj.getBase().getObjectHook().permExpand(this, containingObj);
      }
    else
      {
	forceObjPerm = true;
      }

    // no permissions 

    if (objPerm == null)
      {
	objPerm = PermEntry.noPerms;
      }

    // We also want to check to see whether the custom logic for this
    // object type wants to grant ownership of this object.

    if (!useSelfPerm && object.getBase().getObjectHook().grantOwnership(this, object))
      {
	useSelfPerm = true;
      }

    // look to see if we have permissions set for the object.. this will
    // be our default permissions for each field in the object unless
    // we have an explicit other permission for the field

    if (useSelfPerm || personaMatch(containingObj))
      {
	if (!forceObjPerm)
	  {
	    objPerm = objPerm.union(personaPerms.getPerm(containingObj.getTypeID()));
	  }

	if (result == null)
	  {
	    result = personaPerms.getPerm(object.getTypeID(), fieldId);
	  }
	else
	  {
	    result = result.union(personaPerms.getPerm(object.getTypeID(), fieldId));
	  }
      }
    else
      {
	if (!forceObjPerm)
	  {
	    objPerm = objPerm.union(defaultPerms.getPerm(containingObj.getTypeID()));
	  }

	if (result == null)
	  {
	    result = defaultPerms.getPerm(object.getTypeID(), fieldId);
	  }
	else
	  {
	    result = result.union(defaultPerms.getPerm(object.getTypeID(), fieldId));
	  }
      }

    if (objPerm == null)
      {
	System.err.println("GanymedeSession.getPerm(object, field): null starting point.. AGH!");
	return PermEntry.noPerms;
      }

    if (result == null)
      {
	// we've got no per-field permissions set, so we'll give
	// default access to this field, according to the permissions
	// applicable to the containing object.
	
	return objPerm;
      }
    else
      {
	// we want to return the more restrictive permissions of the 
	// object's permissions and the field's permissions.. we can
	// never look at a field in an object we can't look at.

	return result.intersection(objPerm);
      }
  }

  /**
   *
   * This method returns the generic permissions for a object type.
   * This is currently used primarily to check to see whether a user
   * has privileges to create an object of a specific type.<br><br>
   *
   * This method takes the administrator's current persona's set of
   * appropriate permission matrices, does a binary OR'ing of the
   * permission bits for the given base, and returns the effective
   * permission entry.<br><br>
   * 
   * @param includeOwnedPerms If true, this method will return the
   * permission that the current persona would have for an object that
   * was owned by the current persona.  If false, this method will
   * return the default permissions that apply to objects not owned by
   * the persona.
   * 
   */

  final PermEntry getPerm(short baseID, boolean includeOwnedPerms)
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
   *
   * This method returns the current persona's default permissions for
   * a base and field.  This permission applies generically to objects
   * that are not owned by this persona and to objects that are
   * owned.<br><br>
   *
   * This is used by the Dump code to determine whether a field should
   * be added to the set of possible fields to be returned at the
   * time that the dump results are being prepared.<br><br>
   *
   * @param includeOwnedPerms If true, this method will return the permission
   * that the current persona would have for an object that was owned
   * by the current persona.  If false, this method will return the default
   * permissions that apply to objects not owned by the persona.
   *  
   */

  final PermEntry getPerm(short baseID, short fieldID, boolean includeOwnedPerms)
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
   *
   * This convenience method resets defaultPerms from the default permission
   * object in the Ganymede database.
   *
   */

  private final void resetDefaultPerms()
  {
    PermissionMatrixDBField pField;

    /* -- */

    pField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.RoleDefaultMatrix);

    if (pField == null)
      {
	Ganymede.debug("resetDefaultPerms(): Error: no Default matrix field in default permission object");
	throw new RuntimeException("resetDefaultPerms(): Error: no  Default matrix field in default permission object");
      }
    
    defaultPerms = pField.getMatrix();

    // default permissions are implicitly delegatable

    delegatableDefaultPerms = pField.getMatrix();
  }

  /**
   *
   * This non-exported method is used to generate a comprehensive permissions
   * matrix that applies to all objects owned by the active persona for this
   * user.
   *
   */

  final synchronized void updatePerms(boolean forceUpdate)
  { 
    PermissionMatrixDBField permField;

    /* -- */

    if (forceUpdate)
      {
	personaTimeStamp = null;
      }

    if (permBase == null)
      {
	permBase = Ganymede.db.getObjectBase(SchemaConstants.RoleBase);
      }

    // first, make sure we have a copy of defaultObj

    if (permTimeStamp == null || !permTimeStamp.before(permBase.lastChange))
      {
	defaultObj = session.viewDBObject(SchemaConstants.RoleBase, SchemaConstants.RoleDefaultObj);

	if (defaultObj == null)
	  {
	    if (!Ganymede.firstrun)
	      {
		Ganymede.debug("Serious error!  No default permissions object found in database!");
		throw new RuntimeException("Serious error!  No default permissions object found in database!");
	      }
	    else
	      {
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

    if (personaTimeStamp != null && personaTimeStamp.after(personaBase.lastChange))
      {
	return;
      }

    if (true)
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
      }
    else
      {
	personaObj = null;
      }

    // if we're not locked into supergash mode (for internal sessions, etc.), lets find
    // out whether we're in supergash mode currently
    
    if (!beforeversupergash)
      {
	supergashMode = false;

	// ok, we're not supergash.. or at least, we might not be.  If
	// we are not currently active as a persona, personaPerms will
	// just be our defaultPerms

	if (personaObj == null)
	  {
	    // ok, we're not only not supergash, but we're also not even a privileged
	    // persona.  Load defaultPerms and personaPerms with the two permission
	    // matrices from the default permission object.

	    PermMatrix selfPerm = null;

	    /* -- */

	    resetDefaultPerms();

	    permField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.RoleMatrix);

	    if (permField == null)
	      {
		Ganymede.debug("updatePerms(): Error: no PermMatrix field in default permission object");
	      }
	    else
	      {
		// selfPerm is the permissions that the default permission object has for 
		// objects owned.

		selfPerm = permField.getMatrix();
		
		if (selfPerm == null)
		  {
		    System.err.println("updatePerms(): Error: PermMatrix field's value is null in selfperm object");
		  }
	      }

	    // personaPerms starts off as the union of permissions applicable to
	    // all objects and all objects owned, from the default permissions
	    // object.

	    personaPerms = new PermMatrix(defaultPerms).union(selfPerm);
	    delegatablePersonaPerms = new PermMatrix(defaultPerms).union(selfPerm);

	    System.err.println("GanymedeSession.updatePerms(): returning.. no persona obj for " + 
			       (personaName == null ? username : personaName));

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
	    InvidDBField idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaGroupsField);
	    Invid inv;
		
	    if (idbf != null)
	      {
		Vector vals = idbf.getValuesLocal();

		// *** Caution!  getValuesLocal() does not clone the field's contents..
		// 
		// DO NOT modify vals here!

		// loop over the owner groups this persona is a member
		// of, see if it includes the supergash owner group
		    
		for (int i = 0; i < vals.size(); i++)
		  {
		    inv = (Invid) vals.elementAt(i);
			
		    if (inv.getNum() == SchemaConstants.OwnerSupergash)
		      {
			supergashMode = true;
			Ganymede.debug("GanymedeSession.updatePerms(): setting supergashMode to true");
			break;
		      }
		  }
	      }

	    if (!supergashMode)
	      {
		// since we're not in supergash mode, we need to take into account the
		// operational privileges granted us by the default permission matrix
		// and all the permission matrices associated with this persona.  Calculate
		// the union of all of the applicable permission matrices.

		// make sure that defaultPerms is reset to the baseline, and initialize
		// personaPerms from it.

		PermMatrix selfPerm = null;

		/* -- */

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
		    DBObject pObj;

		    /* -- */
		    
		    for (int i = 0; i < vals.size(); i++)
		      {
			inv = (Invid) vals.elementAt(i);
			    
			pObj = session.viewDBObject(inv);
			
			if (pObj != null)
			  {
			    // The default permissions for this administrator consists of the
			    // union of all default perms fields in all permission matrices for
			    // this admin persona.

			    // personaPerms is the union of all permissions applicable to objects that
			    // are owned by this persona

			    pmdbf = (PermissionMatrixDBField) pObj.getField(SchemaConstants.RoleMatrix);
			    pmdbf2 = (PermissionMatrixDBField) pObj.getField(SchemaConstants.RoleDefaultMatrix);

			    personaPerms = personaPerms.union(pmdbf).union(pmdbf2);
			    defaultPerms = defaultPerms.union(pmdbf2);

			    // we want to maintain our notion of
			    // delegatable permissions separately..

			    Boolean delegatable = (Boolean) pObj.getFieldValueLocal(SchemaConstants.RoleDelegatable);

			    if (delegatable != null && delegatable.booleanValue())
			      {
				delegatablePersonaPerms = delegatablePersonaPerms.union(pmdbf).union(pmdbf2);
				delegatableDefaultPerms = delegatableDefaultPerms.union(pmdbf2);
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

    if (true)
      {
	System.err.println("GanymedeSession.updatePerms(): finished full permissions recalc for " + 
			   (personaName == null ? username : personaName));
      }
    
    return;
  }

  /**
   *
   * This method gives access to the DBObject for the administrator's
   * persona record, if any.  This is used by DBSession to get the
   * label for the administrator for record keeping.
   * 
   */
  
  public DBObject getPersona()
  {
    return personaObj;
  }

  /**
   *
   * This method returns a reference to the DBSession object encapsulated
   * by this GanymedeSession object.  This is intended to be used by
   * subclasses of DBEditObject that might not necessarily be in the
   * arlut.csd.ganymede package.
   *
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
   *
   * This method returns true if a wizard is currently interacting
   * with the user.
   *
   * @see arlut.csd.GanymediatorWizard
   *
   */

  public synchronized boolean isWizardActive()
  {
    return (wizard != null) && (wizard.isActive());
  }

  /**
   * This method returns true if a particular wizard is currently
   * interacting with the user.
   *
   * @see arlut.csd.GanymediatorWizard
   *
   */

  public synchronized boolean isWizardActive(GanymediatorWizard wizard)
  {
    return (this.wizard == wizard) && (this.wizard.isActive());
  }

  /**
   *
   * This method is used to return the active wizard, if any, for
   * this GanymedeSession.
   *
   * @see arlut.csd.GanymediatorWizard
   *
   */

  public synchronized GanymediatorWizard getWizard()
  {
    return wizard;
  }

  /**
   *
   * This method is used to register a wizard for this GanymedeSession.
   *
   * If an active wizard is already registered, this method will return
   * false.
   *
   * @see arlut.csd.GanymediatorWizard
   *
   */

  public synchronized boolean registerWizard(GanymediatorWizard wizard)
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
   *
   * This method is used to register a wizard for this GanymedeSession.
   *
   * If an active wizard is already registered, this method will return
   * false.
   *
   * @see arlut.csd.GanymediatorWizard
   *
   */

  public synchronized void unregisterWizard(GanymediatorWizard wizard)
  {
    if (this.wizard == wizard)
      {
	this.wizard = null;
      }
    else
      {
	throw new IllegalArgumentException("tried to unregister a wizard that wasn't registered");
      }
  }

  /**
   *
   * This method returns true if the active persona has some sort of
   * owner/access relationship with the object in question through
   * its list of owner groups.
   * 
   */

  private final boolean personaMatch(DBObject obj)
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
	if (debug)
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
	if (debug)
	  {
	    System.err.print("** Augmenting admin persona " + obj.getLabel() + " ");
	    showit = true;
	  }

	InvidDBField inf2 = (InvidDBField) obj.getField(SchemaConstants.PersonaGroupsField);

	if (inf2 != null)
	  {
	    Vector values = inf2.getValuesLocal();

	    // *** Caution!  getValuesLocal() does not clone the field's contents..
	    // 
	    // DO NOT modify values here!

	    if (debug)
	      {
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
	    if (debug)
	      {
		System.err.println("<no owner groups in this persona>");
	      }
	  }
      }

    boolean result = recursePersonaMatch(owners, new Vector());

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

  private final boolean recursePersonaMatch(Vector owners, Vector alreadySeen)
  {
    Invid owner;
    DBObject ownerObj;
    InvidDBField inf;
    Vector members;

    /* -- */

    // *** It is critical that this method not modify the owners parameter passed
    // *** in, as it is 'live' in a DBField.

    if (owners == null)
      {
	return false;
      }

    for (int i = 0; i < owners.size(); i++)
      {
	owner = (Invid) owners.elementAt(i);

	if (alreadySeen.contains(owner))
	  {
	    return false;
	  }
	else
	  {
	    alreadySeen.addElement(owner);
	  }

	ownerObj = session.viewDBObject(owner);

	if (ownerObj == null)
	  {
	    continue;
	  }

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

	if (inf == null)
	  {
	    continue;
	  }

	members = inf.getValuesLocal();

	// *** Caution!  getValuesLocal() does not clone the field's contents..
	// 
	// DO NOT modify members here!

	for (int j = 0; j < members.size(); j++)
	  {
	    if (personaInvid.equals((Invid) members.elementAt(j)))
	      {
		return true;
	      }
	  }

	// didn't find, recurse up

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);

	if (inf != null)
	  {
	    if (inf.isVector())
	      {
		// using getValuesLocal() here is safe only because
		// recursePersonaMatch() never tries to modify the
		// owners value passed in.  Otherwise, we'd have to
		// clone the results from getValuesLocal().

		if (recursePersonaMatch(inf.getValuesLocal(), alreadySeen))
		  {
		    return true;
		  }
	      }
	    else
	      {
		throw new RuntimeException("Owner field not a vector!!");
	      }
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
   * depends on recursePersonaMatch().
   *
   */

  public final boolean isMemberAll(Vector owners)
  {
    Invid owner;
    DBObject ownerObj;
    InvidDBField inf;
    Vector members;
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

	if (ownerObj == null)
	  {
	    return false;	// we expect to find it
	  }

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

	if (inf == null)
	  {
	    return false;	// we expect to find it
	  }

	// see if we are a member of this particular owner group

	members = inf.getValuesLocal();

	// *** Caution!  getValuesLocal() does not clone the field's contents..
	// 
	// DO NOT modify members here!

	for (int j = 0; j < members.size(); j++)
	  {
	    if (personaInvid.equals((Invid) members.elementAt(j)))
	      {
		found = true;
	      }
	  }

	// didn't find, recurse up

	inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);

	if (inf != null)
	  {
	    if (inf.isVector())
	      {
		// using getValuesLocal() here is safe only because
		// recursePersonaMatch() never tries to modify the
		// owners value passed in.  Otherwise, we'd have to
		// clone the results from getValuesLocal().

		if (recursePersonaMatch(inf.getValuesLocal(), new Vector()))
		  {
		    found = true;
		  }
	      }
	    else
	      {
		throw new RuntimeException("Owner field not a vector!!");
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
   *
   * This method returns true if the visibility filter vector allows
   * visibility of the object in question.  The visibility vector
   * works by direct ownership identity (i.e., no recursing up), so
   * it's a simple loop-di-loop.
   *  
   */

  private final boolean filterMatch(DBObject obj)
  {
    Vector owners;
    InvidDBField inf;
    Invid tmpInvid;

    /* -- */

    if (obj == null)
      {
	return false;
      }

    if (visibilityFilterInvids == null)
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

  private void setLastEvent(String text)
  {
    this.lastEvent = text;
    GanymedeAdmin.refreshUsers();
  }

  /**
   *
   * This private method is called by all methods in
   * GanymedeSession that require the client to be logged
   * in to operate.
   *
   */

  private void checklogin()
  {
    if (!logged_in)
      {
	throw new IllegalArgumentException("not logged in");
      }
  }
}
