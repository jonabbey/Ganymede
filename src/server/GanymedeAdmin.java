/*

   GanymedeAdmin.java

   GanymedeAdmin is the server-side implementation of the adminSession
   interface;  GanymedeAdmin provides the means by which privileged users
   can carry out privileged operations on the Ganymede server, including
   status monitoring and administrative activities.
   
   Created: 17 January 1997
   Release: $Name:  $
   Version: $Revision: 1.31 $
   Last Mod Date: $Date: 1999/07/08 04:27:44 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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
import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;


/*------------------------------------------------------------------------------
                                                                           class
                                                                   GanymedeAdmin

------------------------------------------------------------------------------*/

/**
 * <p>GanymedeAdmin is the server-side implementation of the
 * {@link arlut.csd.ganymede.adminSession adminSession}
 * interface;  GanymedeAdmin provides the means by which privileged users
 * can carry out privileged operations on the Ganymede server, including
 * status monitoring and administrative activities.</p>
 *
 * <p>GanymedeAdmin is actually a dual purpose class.  One the one hand,
 * GanymedeAdmin implements {@link arlut.csd.ganymede.adminSession adminSession},
 * providing a hook for the admin console to talk to.  On the other,
 * GanymedeAdmin contains a lot of static fields and methods which the
 * server code uses to communicate information to any admin consoles
 * that are attached to the server at any given time.</p>
 *
 * @version $Revision: 1.31 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

class GanymedeAdmin extends UnicastRemoteObject implements adminSession, Unreferenced {

  /**
   * Static vector of GanymedeAdmin instances, used to
   * keep track of the attached admin consoles.
   */

  private static Vector consoles = new Vector();

  /**
   * Static vector of GanymedeAdmin instances for which
   * remote exceptions were caught in the static
   * update methods.  Used by
   * {@link arlut.csd.ganymede.GanymedeAdmin#detachBadConsoles() detachBadConsoles()}
   * to remove consoles that we were not able to communicate with.
   */

  private static Vector badConsoles = new Vector();

  private static String state;
  private static Date lastDumpDate;

  /* --- */

  /**
   * The name that the admin console authenticated with.  We
   * keep it here rather than asking the console later so that
   * the console can't decide it should call itself 'supergash'
   * at some later point.
   */

  private String adminName;

  /**
   * The name or ip address of the system that this admin console
   * is attached from.
   */

  private String clientHost;

  /**
   * If true, the admin console is attached with full privileges to
   * run tasks, shut down the server, and so on.  If false, the user
   * just has privileges to watch the server's operation.
   */

  private boolean fullprivs = false;

  /**
   * <P>The RMI remote reference to the admin console attached to this
   * instance of GanymedeAdmin.</P>
   *
   * <P>Will be null if this GanymedeAdmin object is no longer attached
   * to a real console.</P>
   */

  Admin admin;
  
  /* -- */

  /**
   * This static method is used to send debug log info to
   * the consoles.
   */

  public static synchronized void setStatus(String status)
  {
    GanymedeAdmin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	try
	  {
	    temp.admin.changeStatus(status);
	    temp.admin.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
	  }
	catch (RemoteException ex)
	  {
	    // don't call Ganymede.debug() here or we'll get into an infinite
	    // loop if we have any problems.
	    System.err.println("Couldn't update Status on an admin console" + ex);
	    badConsoles.addElement(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the current transcount
   * to the consoles.
   */

  public static synchronized void updateTransCount()
  {
    GanymedeAdmin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateTransCount(temp);
      }
  }

  /**
   * This static method is used to send the current transcount
   * to an individual consoles.
   */

  public static synchronized void updateTransCount(GanymedeAdmin console)
  {
    if (console.admin == null)
      {
	return;
      }

    try
      {
	console.admin.setTransactionsInJournal(Ganymede.db.journal.transactionsInJournal);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an
	// infinite loop if we have any problems.

	System.err.println("Couldn't update transaction count on an admin console" + ex);
	badConsoles.addElement(console);
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the last dump time to
   * the consoles.
   */

  public static synchronized void updateLastDump(Date date)
  {
    GanymedeAdmin.lastDumpDate = date;
    updateLastDump();
  }

  /**
   * This method updates the last dump time on a all
   * consoles.
   */

  public static synchronized void updateLastDump()
  {
    GanymedeAdmin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateLastDump(temp);
      }
  }

  /**
   * This method updates the last dump time on a single
   * console
   */

  public static void updateLastDump(GanymedeAdmin console)
  {
    if (console.admin == null)
      {
	return;
      }

    try
      {
	console.admin.setLastDumpTime(GanymedeAdmin.lastDumpDate);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.
	System.err.println("Couldn't update dump date on an admin console" + ex);
	badConsoles.addElement(console);
      }

    detachBadConsoles();
  }

  /**
   * This method updates the objects checked out count on all consoles.
   */

  public static synchronized void updateCheckedOut()
  {
    GanymedeAdmin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateCheckedOut(temp);
      }
  }

  /**
   * This method updates the objects checked out count on a single
   * console
   */

  public static synchronized void updateCheckedOut(GanymedeAdmin console)
  {
    if (console.admin == null)
      {
	return;
      }

    try
      {
	console.admin.setObjectsCheckedOut(Ganymede.db.objectsCheckedOut);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.
	System.err.println("Couldn't update objects checked out count on an admin console" + ex);
	badConsoles.addElement(console);
      }

    detachBadConsoles();
  }

  public static synchronized void updateLocksHeld()
  {
    GanymedeAdmin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	if (temp.admin == null)
	  {
	    continue;
	  }

	updateLocksHeld(temp);
      }
  }

  /**
   * This method updates the number of locks held on a single
   * console
   */

  public static synchronized void updateLocksHeld(GanymedeAdmin console)
  {
    if (console.admin == null)
      {
	return;
      }

    try
      {
	console.admin.setLocksHeld(Ganymede.db.locksHeld);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.
	System.err.println("Couldn't update locks held on an admin console" + ex);
	badConsoles.addElement(console);
      }

    detachBadConsoles();
  }

  /**
   * This method changes the system state and
   * sends it out to the consoles
   */

  public static void setState(String state)
  {
    GanymedeAdmin.state = state;
    setState();
  }

  /**
   * This method updates the state on all
   * attached consoles
   */

  public static synchronized void setState()
  {
    GanymedeAdmin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	setState(temp);
      }
  }

  /**
   * This method updates the state on a single
   * console
   */

  public static void setState(GanymedeAdmin console)
  {
    if (console.admin == null)
      {
	return;
      }

    try
      {
	console.admin.changeState(GanymedeAdmin.state);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.
	System.err.println("Couldn't update Status on an admin console" + ex);
	badConsoles.addElement(console);
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update the list of connnected users that
   * appears in any admin consoles attached to the Ganymede server.
   */

  public static synchronized void refreshUsers()
  {
    GanymedeAdmin temp;
    Vector entries = new Vector();
    GanymedeSession session;

    /* -- */

    // figure out the vector we want to pass along

    for (int j = 0; j < GanymedeServer.sessions.size(); j++)
      {
	session = (GanymedeSession) GanymedeServer.sessions.elementAt(j);

	if (session.logged_in)
	  {
	    // note that we really should do something a bit more sophisticated
	    // than using toString on connecttime.

	    entries.addElement(new AdminEntry((session.personaName == null) ? session.username : session.personaName,
					      session.clienthost,
					      (session.status == null) ? "" : session.status,
					      session.connecttime.toString(),
					      (session.lastEvent == null) ? "" : session.lastEvent,
					      session.objectsCheckedOut));
	  }
      }

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	try
	  {
	    temp.admin.changeUsers(entries);
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Couldn't update user list on an admin console" + ex);
	    badConsoles.addElement(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update the list of connnected users that
   * appears in any admin consoles attached to the Ganymede server.
   */

  public static synchronized void refreshTasks()
  {
    GanymedeAdmin temp;
    Vector scheduleHandles;

    /* -- */
    
    if (Ganymede.scheduler == null)
      {
	return;
      }
    
    scheduleHandles = Ganymede.scheduler.reportTaskInfo();

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (GanymedeAdmin) consoles.elementAt(i);

	try
	  {
	    temp.admin.changeTasks(scheduleHandles);
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Couldn't update task list on an admin console" + ex);
	    badConsoles.addElement(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * <p>This is the GanymedeAdmin constructor, used to create a new
   * server-side admin console attachment.</p>
   *
   * <p>Admin is an RMI remote object exported by the client in the
   * form of a callback.</p>
   *
   * <P>This constructor is called from
   * {@link arlut.csd.ganymede.GanymedeServer#admin(arlut.csd.ganymede.Admin) admin()},
   * which is responsible for authenticating the name and password before
   * calling this constructor.</P>
   */

  public GanymedeAdmin(Admin admin, boolean fullprivs, String adminName, String clientHost) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    this.admin = admin;
    this.fullprivs = fullprivs;
    this.adminName = adminName;
    this.clientHost = clientHost;

    consoles.addElement(this);

    admin.setServerStart(Ganymede.startTime);
    updateTransCount(this);
    updateLastDump(this);
    updateCheckedOut(this);
    updateLocksHeld(this);
    setState(this);
    
    refreshUsers();
    refreshTasks();
  }

  /**
   *
   * Disconnect the remote admin console associated with this object
   *
   */

  public void logout()
  {
    this.logout(null);
  }

  /**
   *
   * Disconnect the remote admin console associated with this object
   *
   */

  public synchronized void logout(String reason)
  {
    if (this.admin != null)
      {
	consoles.removeElement(this);

	this.admin = null;
	
	if (reason == null)
	  {
	    Ganymede.debug("Admin console " + adminName + " detached from " + clientHost);
	  }
	else
	  {
	    Ganymede.debug("Admin console " + adminName + " detached from " + clientHost + ": " + reason);
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
   * @see java.rmi.server.Unreferenced
   */

  public void unreferenced()
  {
    this.logout("dead console");
  }

  /**
   * <P>This method lets the admin console explicitly request
   * a refresh.  Upon being called, the server will call several
   * methods on the admin console's {@link arlut.csd.ganymede.Admin Admin}
   * interface to pass current status information to the console.</P>
   */

  public synchronized void refreshMe() throws RemoteException
  {
    admin.setServerStart(Ganymede.startTime);
    updateTransCount(this);
    updateLastDump(this);
    updateCheckedOut(this);
    updateLocksHeld(this);
    admin.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
    setState(this);

    refreshUsers();
    refreshTasks();
  }

  /**
   * Kick all users off of the Ganymede server on behalf of this admin console
   */

  public synchronized ReturnVal killAll()
  {
    GanymedeSession temp;

    /* -- */

    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to knock all users off of the server");
      }

    while (GanymedeServer.sessions.size() != 0)
      {
	temp = (GanymedeSession) GanymedeServer.sessions.elementAt(0);
	temp.forceOff("Admin console disconnecting you");
      }

    return null;
  }

  /**
   * Kick a user off of the Ganymede server on behalf of this admin console
   */

  public synchronized ReturnVal kill(String user)
  {
    GanymedeSession temp;

    /* -- */

    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to kill of user " + user);
      }

    for (int i = 0; i < GanymedeServer.sessions.size(); i++)
      {
	temp = (GanymedeSession) GanymedeServer.sessions.elementAt(i);

	if (temp.personaName != null)
	  {
	    if (temp.personaName.equals(user))
	      {
		temp.forceOff("Admin console disconnecting you");
		return null;
	      }
	  }
	else
	  {
	    if (temp.username.equals(user))
	      {
		temp.forceOff("Admin console disconnecting you");
		return null;
	      }
	  }
      }

    return Ganymede.createErrorDialog("Kill Error",
				      "I couldn't find any active user named " + user);
  }

  /**
   * <p>shutdown the server cleanly, on behalf of this admin console.</p>
   */

  public ReturnVal shutdown()
  {
    GanymedeSession temp;
    GanymedeAdmin atmp;
    Vector tempList;

    /* -- */

    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to shut down the server");
      }

    setStatus("Server going down.. performing final dump");

    // dump, then shut down.  Our second dump parameter is false,
    // so that we are guaranteed that no client can get a writelock
    // and maybe get a transaction off that would cause us confusion.

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

    for (int i = 0; i < GanymedeServer.sessions.size(); i++)
      {
	tempList.addElement(GanymedeServer.sessions.elementAt(i));
      }

    for (int i = 0; i < tempList.size(); i++)
      {
	temp = (GanymedeSession) tempList.elementAt(i);

	temp.forceOff("Server going down");
      }

    // stop any background tasks running

    Ganymede.scheduler.stop();

    // disconnect the admin consoles

    for (int i = 0; i < consoles.size(); i++)
      {
	atmp = (GanymedeAdmin) consoles.elementAt(i);

	try
	  {
	    atmp.admin.forceDisconnect("Server going down now.");
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
    try
      {
	Ganymede.log.close();
      }
    catch (IOException ex)
      {
	System.err.println("IO Exception closing log file:" + ex);
      }

    System.exit(0);

    return null;		// we'll never get here
  }

  /**
   * <P>dump the current state of the db to disk</P>
   */

  public ReturnVal dumpDB()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute a database dump");
      }

    setStatus("Dumping database");

    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, true, true); // release, archive
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog("Database Dump Error",
					  "Database could not be dumped successfully. " + ex);
      }

    Ganymede.debug("Database dumped");

    return null;
  }

  /**
   * <P>dump the current db schema to disk</P>
   */

  public ReturnVal dumpSchema()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute a schema dump");
      }

    setStatus("Dumping schema");

    try
      {
	Ganymede.db.dumpSchema(Ganymede.schemaProperty, true); // release the lock when the dump is complete
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog("Schema Dump Error",
					  "Schema could not be dumped successfully. " + ex);
      }

    Ganymede.debug("Schema dumped");

    return null;
  }

  /**
   * <P>This method causes the server to reload any registered
   * custom classes, and can be run after a schema edit
   * to cause the new classes to take over management of
   * their respective object types.</P>
   *
   * <P>It's not clear how well this actually works.. I think
   * that custom classes that have already been loaded will
   * not be reloaded by the class loader, and classes that
   * have not been loaded previously will need to already
   * be present in the jar file that the server is running out
   * of, so this method is probably not useful in practice.</P>
   *
   * <P>Better to shutdown and restart the server.</P>
   */

  public synchronized ReturnVal reloadCustomClasses()
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */
    
    synchronized (Ganymede.server)
      {
	if (GanymedeServer.sessions.size() != 0)
	  {
	    return Ganymede.createErrorDialog("Can't reload classes",
					      "Can't reload classes, users logged in");
	  }
	else if (!Ganymede.db.schemaEditInProgress)
	  {
	    Ganymede.db.schemaEditInProgress = true;
	  }
	else
	  {
	    return Ganymede.createErrorDialog("Can't reload classes",
					      "Can't reload classes, schema edit already in progress");
	  }
      }

    synchronized (Ganymede.db)
      {
	Ganymede.debug("entering reloadCustomClasses synchronization block");

	enum = Ganymede.db.objectBases.elements();

	if (enum != null)
	  {
	    while (enum.hasMoreElements())
	      {
		base = (DBObjectBase) enum.nextElement();

		// force reload of class

		base.reloadCustomClass();
	      }
	  }

	Ganymede.db.notifyAll(); // in case a DBLock caught on our sync
      }

    synchronized (Ganymede.server)
      {
	Ganymede.db.schemaEditInProgress = false;
      }

    return null;
  }

  /**
   *
   * run a long-running verification suite on the invid links
   *
   */

  public ReturnVal runInvidTest()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute an Invid integrity test on the server");
      }

    GanymedeAdmin.setState("Running Invid Test");
	 
    if (Ganymede.server.checkInvids())
      {
	Ganymede.debug("Invid Test completed successfully, no problems boss.");
      }
    else
      {
	Ganymede.debug("Invid Test encountered problems.  Oi, you're in the soup now, boss.");
      }

    GanymedeAdmin.setState("Normal Operation");

    return null;
  }

  /**
   *
   * run a long-running verification suite on the invid links
   *
   */

  public ReturnVal runInvidSweep()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute an Invid sweep on the server");
      }

    GanymedeAdmin.setState("Running Invid Sweep");
	 
    Ganymede.server.sweepInvids();

    GanymedeAdmin.setState("Normal Operation");

    return null;
  }

  /**
   * <P>Causes a pre-registered task in the Ganymede server
   * to be executed as soon as possible.  This method call
   * will have no effect if the task is currently running.</P>
   */

  public ReturnVal runTaskNow(String name)
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute tasks on the server");
      }

    if (Ganymede.scheduler.runTaskNow(name))
      {
	return null;
      }

    return Ganymede.createErrorDialog("Couldn't run task",
				      "Couldn't run task " + name + ", perhaps the task isn't registered?");
  }

  /**
   * <P>Causes a running task to be stopped as soon as possible.
   * This is not always a safe operation, as the task is stopped
   * abruptly, with possible consequences.  Use with caution.</P>
   */

  public ReturnVal stopTask(String name)
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to stop tasks on the server");
      }

    if (Ganymede.scheduler.stopTask(name))
      {
	return null;
      }

    return Ganymede.createErrorDialog("Couldn't stop task",
				      "Couldn't stop task " + name + ", perhaps the task isn't running?");
  }

  /**
   * <P>Causes a registered task to be made ineligible for execution
   * until {@link arlut.csd.ganymede.GanymedeAdmin#enableTask(java.lang.String) enableTask()}
   * is called.  This method will not stop a task that is currently
   * running.</P>
   */

  public ReturnVal disableTask(String name)
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to disable tasks on the server");
      }

    if (Ganymede.scheduler.disableTask(name))
      {
	return null;
      }

    return Ganymede.createErrorDialog("Couldn't disable task",
				      "Couldn't disable task " + name + ", perhaps the task isn't registered?");
  }

  /**
   * <P>Causes a task that was temporarily disabled by
   * {@link arlut.csd.ganymede.GanymedeAdmin#disableTask(java.lang.String) disableTask()}
   * to be available for execution again.</P>
   */

  public ReturnVal enableTask(String name)
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to enable tasks on the server");
      }

    if (Ganymede.scheduler.enableTask(name))
      {
	return null; 
      }

    return Ganymede.createErrorDialog("Couldn't enable task",
				      "Couldn't enable task " + name + ", perhaps the task isn't registered?");
  }

  /**
   *
   * lock the server and edit the schema
   *
   */

  public SchemaEdit editSchema()
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    if (!fullprivs)
      {
	Ganymede.debug("Attempt made to edit schema by a non-privileged console");
	return null;
      }

    Ganymede.debug("entering editSchema");

    // synchronize on server so we don't get logins while we're checking
    // things out

    synchronized (Ganymede.server)
      {
	if (GanymedeServer.sessions.size() != 0)
	  {
	    Ganymede.debug("Can't edit Schema, users logged in");
	    return null;
	  }
	else if (!Ganymede.db.schemaEditInProgress)
	  {
	    Ganymede.db.schemaEditInProgress = true;
	  }
	else
	  {
	     Ganymede.debug("Can't edit Schema, edit already in progress. ");
	     return null;
	  }
      }

    // okay at this point we've asserted our interest in editing the schema and
    // ascertained that no one is logged on.  By setting Ganymede.db.schemaEditInProgress
    // to true, we've preempted anyone else from logging in.  Check out the lock
    // situation, then go forward.

    synchronized (Ganymede.db)
      {
	Ganymede.debug("entering editSchema synchronization block");

	enum = Ganymede.db.objectBases.elements();

	if (enum != null)
	  {
	    while (enum.hasMoreElements())
	      {
		base = (DBObjectBase) enum.nextElement();

		if (base.isLocked())
		  {
		    Ganymede.debug("Can't edit Schema, lock held on " + base.getName());
		    Ganymede.db.schemaEditInProgress = false;
		    return null;
		  }
	      }
	  }

	 // should be okay

	 Ganymede.debug("Ok to create DBSchemaEdit");

	 GanymedeAdmin.setState("Schema Edit In Progress");

	 try
	   {
	     DBSchemaEdit result = new DBSchemaEdit(admin);
	     return result;
	   }
	 catch (RemoteException ex)
	   {
	     return null;
	   }
      }
  }

  // This is a private convenience function, it's purpose is to clean out
  // any consoles that we caught a remote exception from.  We'll go ahead
  // and try the connection once more as a test before banishing it.
   
  private static synchronized void detachBadConsoles()
  {
    boolean testval;
    GanymedeAdmin temp;

    /* -- */

    for (int i=0; i < badConsoles.size(); i++)
      {
	temp = (GanymedeAdmin) badConsoles.elementAt(i);

	if (temp.admin == null)
	  {
	    continue;
	  }

	testval = false;

	try
	  {
	    temp.admin.getPassword();
	  }
	catch (RemoteException ex)
	  {
	    testval = true;
	  }

	if (testval)
	  {
	    temp.logout("error communicating with console");
	  }
      }

    badConsoles = new Vector();
  }
  
}
