/*

   GanymedeAdmin.java

   GanymedeAdmin is the server-side implementation of the adminSession
   interface;  GanymedeAdmin provides the means by which privileged users
   can carry out privileged operations on the Ganymede server, including
   status monitoring and administrative activities.
   
   Created: 17 January 1997
   Release: $Name:  $
   Version: $Revision: 1.55 $
   Last Mod Date: $Date: 2002/01/26 05:27:28 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

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
 * @version $Revision: 1.55 $ $Date: 2002/01/26 05:27:28 $
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

  /**
   * The overall server state.. 'normal operation', 'shutting down', etc.
   */

  private static String state;

  /**
   * Timestamp that the Ganymede server last consolidated its journal
   * file and dumped its database to disk.
   */

  private static Date lastDumpDate;

  /**
   * Free memory statistic that the server sends to admin consoles
   */

  private static long freeMem;

  /**
   * Total memory statistic that the server sends to admin consoles
   */

  private static long totalMem;

  /* -----====================--------------------====================-----

			         static methods

     -----====================--------------------====================----- */

  /**
   * <p>This public static method handles sending disconnect messages
   * to all attached consoles and cleaning up the
   * GanymedeAdmin.consoles Vector.</p>
   */

  public static void closeAllConsoles(String reason)
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < GanymedeAdmin.consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) GanymedeAdmin.consoles.elementAt(i);

	    try
	      {
		temp.forceDisconnect(reason);
	      }
	    catch (RemoteException ex)
	      {
		// don't worry about it
	      }
	  }

	GanymedeAdmin.consoles.removeAllElements();
      }
  }

  /**
   * This static method is used to send debug log info to
   * the consoles.  It is used by
   * {@link arlut.csd.ganymede.Ganymede#debug(java.lang.String) Ganymede.debug()}
   * to append information to the console logs.
   */

  public static void setStatus(String status)
  {
    GanymedeAdmin temp;
    String stampedLine;

    synchronized (GanymedeServer.lSemaphore)
      {
	if (GanymedeServer.lSemaphore.checkEnabled() == null)
	  {
	    stampedLine = new Date() + " [" + GanymedeServer.lSemaphore.getCount() + "] " + status + "\n";
	  }
	else
	  {
	    stampedLine = new Date() + " [*] " + status + "\n";
	  }
      }

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.proxy.changeStatus(stampedLine, true);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * <p>This method sends an updated console count figure to all of the
   * attached admin consoles.</p>
   */

  public static void setConsoleCount()
  {
    GanymedeAdmin temp;
    String message;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	message = consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached";

	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.proxy.changeAdmins(message);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the current transcount
   * to the consoles.
   */

  public static void updateTransCount()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateTransCount();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the last dump time to
   * the consoles.
   */

  public static void updateLastDump(Date date)
  {
    GanymedeAdmin.lastDumpDate = date;
    updateLastDump();
  }

  /**
   * This method updates the last dump time to all
   * consoles.
   */

  public static void updateLastDump()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateLastDump();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update and transmit the server's
   * memory status to the consoles.
   */

  public static void updateMemState(long freeMem, long totalMem)
  {
    GanymedeAdmin.freeMem = freeMem;
    GanymedeAdmin.totalMem = totalMem;
    updateMemState();
  }

  /**
   * This method is used to send the server's memory status to all
   * connected admin consoles.
   */

  public static void updateMemState()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateMemState();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This method updates the objects checked out count on all consoles.
   */

  public static void updateCheckedOut()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateCheckedOut();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  public static void updateLocksHeld()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateLocksHeld();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This method changes the system state and
   * sends it out to the consoles
   */

  public static void setState(String state)
  {
    GanymedeAdmin temp;

    /* -- */

    GanymedeAdmin.state = state;

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doSetState();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update the list of connnected users that
   * appears in any admin consoles attached to the Ganymede server.
   */

  public static void refreshUsers()
  {
    GanymedeAdmin temp;
    Vector entries;

    /* -- */

    // figure out the vector we want to pass along

    entries = GanymedeServer.getUserTable();

    // update the consoles

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);
	    
	    try
	      {
		temp.doRefreshUsers(entries);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update the list of connnected users that
   * appears in any admin consoles attached to the Ganymede server.
   */

  public static void refreshTasks()
  {
    GanymedeAdmin temp;
    Vector scheduleHandles;

    /* -- */
    
    if (Ganymede.scheduler == null)
      {
	return;
      }

    scheduleHandles = Ganymede.scheduler.reportTaskInfo();

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doRefreshTasks(scheduleHandles);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * <p>This method handles communications link failures.  Note that
   * the serverAdminProxy will handle single instances of admin
   * console RemoteExceptions so that only two RemoteExceptions in
   * sequence will raise a RemoteException.</p>
   */

  private final static void handleConsoleRMIFailure(GanymedeAdmin console, RemoteException ex)
  {
    // don't use Ganymede.debug so that we can avoid infinite loops
    // during error handling

    System.err.println("Communications failure to " + console.toString());
    badConsoles.addElement(console);
  }

  /**
   * This is a private convenience function, it's purpose is to 
   * out any consoles that we caught a remote exception from, 
   * the context of a loop over consoles that we might interfere 
   * here.
   */
   
  private static void detachBadConsoles()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i=0; i < badConsoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) badConsoles.elementAt(i);

	    // the logout() method will cause the console to remove
	    // itself from the static GanymedeAdmin.consoles vecotr,
	    // which is why we are synchronized on
	    // GanymedeAdmin.consoles here.

	    temp.logout("error communicating with console");
	  }

	badConsoles.setSize(0);
      }
  }

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
   * <p>A server-side proxy that maintains an event queue for the admin
   * console attached to this GanymedeAdmin object.</p>
   */

  private serverAdminProxy proxy;
  
  /* -- */

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

    // if the timeOutTask hasn't previously updated the 

    if (GanymedeAdmin.freeMem == 0 && GanymedeAdmin.totalMem == 0)
      {
	GanymedeAdmin.freeMem = Runtime.getRuntime().freeMemory();
	GanymedeAdmin.totalMem = Runtime.getRuntime().totalMemory();
      }

    this.proxy = new serverAdminProxy(admin);
    this.fullprivs = fullprivs;
    this.adminName = adminName;
    this.clientHost = clientHost;

    consoles.addElement(this);	// this can block if we are currently looping on consoles

    try
      {
	setConsoleCount();
	admin.setServerStart(Ganymede.startTime);
	doUpdateTransCount();
	doUpdateTransCount();
	doUpdateLastDump();
	doUpdateCheckedOut();
	doUpdateLocksHeld();
	doUpdateMemState();
	doSetState();
	
	doRefreshUsers(GanymedeServer.getUserTable());
	doRefreshTasks(Ganymede.scheduler.reportTaskInfo());
      }
    catch (RemoteException ex)
      {
	handleConsoleRMIFailure(this, ex);
      }
  }

  /**
   * <p>This private method is used to update this admin console
   * handle's transaction count.</p>
   */

  private void doUpdateTransCount() throws RemoteException
  {
    proxy.setTransactionsInJournal(Ganymede.db.journal.transactionsInJournal);
  }

  /**
   * This private method updates the last dump time on this admin
   * console
   */

  private void doUpdateLastDump() throws RemoteException
  {
    proxy.setLastDumpTime(GanymedeAdmin.lastDumpDate);
  }

  /**
   * This private method updates the memory statistics display on this
   * admin console
   */

  private void doUpdateMemState() throws RemoteException
  {
    proxy.setMemoryState(GanymedeAdmin.freeMem, GanymedeAdmin.totalMem);
  }

  /**
   * This private method updates the objects checked out display on
   * this admin console 
   */

  private void doUpdateCheckedOut() throws RemoteException
  {
    proxy.setObjectsCheckedOut(Ganymede.db.objectsCheckedOut);
  }

  /**
   * This private method updates the number of locks held display on
   * this admin console
   */

  private void doUpdateLocksHeld() throws RemoteException
  {
    proxy.setLocksHeld(Ganymede.db.lockSync.getLockCount());
  }

  /**
   * This private method updates the server state display on
   * this admin console
   */

  private void doSetState() throws RemoteException
  {
    proxy.changeState(GanymedeAdmin.state);
  }

  /**
   * This private method updates the user status table on
   * this admin console
   */

  private void doRefreshUsers(Vector entries) throws RemoteException
  {
    proxy.changeUsers(entries);
  }

  /**
   * This private method updates the task status table on
   * this admin console
   */

  private void doRefreshTasks(Vector scheduleHandles) throws RemoteException
  {
    proxy.changeTasks(scheduleHandles);
  }

  /**
   * <p>This public method forces a disconnect of the remote admin console
   * and cleans up the proxy.</p>
   *
   * <p>This method *does not* handle removing this GanymedeAdmin
   * console object from the static GanymedeAdmin.consoles Vector, so
   * that it can be called from a loop over GanymedeAdmin.consoles in
   * closeAllConsoles() when the server is being shut down.</p>
   */

  public void forceDisconnect(String reason) throws RemoteException
  {
    proxy.forceDisconnect(reason);
  }

  /* -----====================--------------------====================-----

			    remotely callable methods

     -----====================--------------------====================----- */

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

  public void logout(String reason)
  {
    if (proxy.isAlive())
      {
	proxy.shutdown();
      }

    synchronized (GanymedeAdmin.consoles)
      {
	if (consoles.contains(this))
	  {
	    consoles.removeElement(this);
	
	    if (reason == null)
	      {
		Ganymede.debug("Admin console " + adminName + " detached from " + clientHost);
	      }
	    else
	      {
		Ganymede.debug("Admin console " + adminName + " detached from " + clientHost + ": " + reason);
	      }

	    setConsoleCount();
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

  public void refreshMe() throws RemoteException
  {
    proxy.setServerStart(Ganymede.startTime);
    doUpdateTransCount();
    doUpdateLastDump();
    doUpdateCheckedOut();
    doUpdateLocksHeld();
    doUpdateMemState();
    proxy.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
    doSetState();

    doRefreshUsers(GanymedeServer.getUserTable());
    doRefreshTasks(Ganymede.scheduler.reportTaskInfo());
  }

  /**
   * <p>This method is called by admin console code to force
   * a complete rebuild of all external builds.  This means that
   * all databases will have their last modification timestamp
   * cleared and all builder tasks will be scheduled for immediate
   * execution.</p>
   */

  public ReturnVal forceBuild()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to force a full rebuild");
      }

    Ganymede.debug("Admin console forcing full network build...");

    Ganymede.forceBuilderTasks();

    return null;
  }

  /**
   * Kick all users off of the Ganymede server on behalf of this admin console
   */

  public ReturnVal killAll()
  {
    GanymedeSession temp;

    /* -- */

    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to knock all users off of the server");
      }

    GanymedeServer.server.killAllUsers("Admin console disconnecting you");

    return null;
  }

  /**
   * Kick a user off of the Ganymede server on behalf of this admin console
   */

  public ReturnVal kill(String user)
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to kill of user " + user);
      }

    if (GanymedeServer.server.killUser(user, "Admin console disconnecting you"))
      {
	return null;
      }

    return Ganymede.createErrorDialog("Kill Error",
				      "I couldn't find any active user named " + user);
  }

  /**
   * <p>shutdown the server cleanly, on behalf of this admin console.</p>
   *
   * @param waitForUsers if true, shutdown will be deferred until all users are logged
   * out.  No new users will be allowed to login.
   */

  public ReturnVal shutdown(boolean waitForUsers)
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to shut down the server");
      }

    if (waitForUsers)
      {
	GanymedeServer.setShutdown();

	return Ganymede.createInfoDialog("Server Set For Shutdown",
					 "The server is prepared for shut down.  Shutdown will commence as soon " +
					 "as all current users log out.");
      }
    else
      {
	return GanymedeServer.shutdown(); // we may never return if the shutdown succeeds.. the client
				          // will catch an exception in that case.
      }
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

    setState("Dumping database");

    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, true, true); // release, archive
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog("Database Dump Error",
					  "Database could not be dumped successfully. " + ex);
      }
    finally
      {
	setState("Normal Operation");
      }

    Ganymede.debug("Database dumped");

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
    Ganymede.debug("Running Invid Sweep");
	 
    Ganymede.server.sweepInvids();

    GanymedeAdmin.setState("Normal Operation");
    Ganymede.debug("Normal Operation");

    return null;
  }

  /**
   *
   * run a verification on the integrity of embedded objects and
   * their containers
   *
   */

  public ReturnVal runEmbeddedTest()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute an embedded objects integrity test on the server");
      }

    GanymedeAdmin.setState("Running Embedded Test");
	 
    if (Ganymede.server.checkEmbeddedObjects())
      {
	Ganymede.debug("Embedded Objects Test completed successfully, no problems boss.");
      }
    else
      {
	Ganymede.debug("Embedded Objects Test encountered problems.  Oi, you're in the soup now, boss.");
      }

    GanymedeAdmin.setState("Normal Operation");

    return null;
  }

  /**
   * <P>Removes any embedded objects which do not have containers.</P>
   */

  public ReturnVal runEmbeddedSweep()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute an Embedded objects sweep on the server");
      }

    return Ganymede.server.sweepEmbeddedObjects();
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

    try
      {
	String semaphoreCondition = GanymedeServer.lSemaphore.disable("schema edit", true, 0);

	if (semaphoreCondition != null)
	  {
	    Ganymede.debug("Can't edit schema, semaphore error: " + semaphoreCondition);
	    return null;
	  }
      }
    catch (InterruptedException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    // okay at this point we've asserted our interest in editing the
    // schema and made sure that no one is logged in or can log in.
    // Now we just need to make sure that we don't have any of the
    // bases locked by anything that is skipping the semaphore, such
    // as tasks.

    // In fact, I believe that the server is now safe against lock
    // races due to all tasks that might involve DBObjectBase access
    // being guarded by the loginSemaphore, but there is little cost
    // in sync'ing here.

    // All the DBLock establish methods synchronize on the DBLockSync
    // object referenced by Ganymede.db.lockSync, so we are safe
    // against lock establish race conditions by synchronizing this
    // section on Ganymede.db.lockSync.

    synchronized (Ganymede.db.lockSync)
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
		    GanymedeServer.lSemaphore.enable("schema edit");
		    return null;
		  }
	      }
	  }

	 // should be okay

	 Ganymede.debug("Ok to create DBSchemaEdit");

	 GanymedeAdmin.setState("Schema Edit In Progress");

	 try
	   {
	     DBSchemaEdit result = new DBSchemaEdit(proxy);
	     return result;
	   }
	 catch (RemoteException ex)
	   {
	     GanymedeServer.lSemaphore.enable("schema edit");
	     return null;
	   }
      }
  }

  public String toString()
  {
    if (fullprivs)
      {
	return "console: " + adminName + " on " + clientHost + " with full access";
      }
    else
      {
	return "console: " + adminName + " on " + clientHost + " with monitor access";
      }
  }
}
