/*

   GanymedeAdmin.java

   GanymedeAdmin is the server-side implementation of the adminSession
   interface;  GanymedeAdmin provides the means by which privileged users
   can carry out privileged operations on the Ganymede server, including
   status monitoring and administrative activities.
   
   Created: 17 January 1997
   Release: $Name:  $
   Version: $Revision: 1.43 $
   Last Mod Date: $Date: 2000/02/15 02:59:43 $
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
 * @version $Revision: 1.43 $ $Date: 2000/02/15 02:59:43 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

class GanymedeAdmin extends UnicastRemoteObject implements adminSession, Unreferenced {

  /**
   * Static vector of GanymedeAdmin instances, used to
   * keep track of the attached admin consoles.
   */

  static Vector consoles = new Vector();

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

  serverAdminProxy proxy;
  
  /* -- */

  /**
   * This static method is used to send debug log info to
   * the consoles.  It is used by
   * {@link arlut.csd.ganymede.Ganymede#debug(java.lang.String) Ganymede.debug()}
   * to append information to the console logs.
   */

  public static void setStatus(String status)
  {
    GanymedeAdmin temp;
    String stampedLine = new Date() + " [" + GanymedeServer.lSemaphore.getCount() + "] " + status + "\n";

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.proxy.changeStatus(stampedLine, true);
		temp.proxy.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
	      }
	    catch (RemoteException ex)
	      {
		// don't call Ganymede.debug() here or we'll get into an infinite
		// loop if we have any problems.

		System.err.println("Couldn't update Status on an admin console" + ex);
		badConsoles.addElement(temp);
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
	    
	    updateTransCount(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the current transcount
   * to an individual consoles.
   */

  public static void updateTransCount(GanymedeAdmin console)
  {
    try
      {
	console.proxy.setTransactionsInJournal(Ganymede.db.journal.transactionsInJournal);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an
	// infinite loop if we have any problems.

	System.err.println("Couldn't update transaction count on an admin console" + ex);
	badConsoles.addElement(console);
      }
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
   * This method updates the last dump time on a all
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

	    updateLastDump(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This method updates the last dump time on a single
   * console
   */

  public static void updateLastDump(GanymedeAdmin console)
  {
    try
      {
	console.proxy.setLastDumpTime(GanymedeAdmin.lastDumpDate);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.
	System.err.println("Couldn't update dump date on an admin console" + ex);
	badConsoles.addElement(console);
      }
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

	    updateCheckedOut(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This method updates the objects checked out count on a single
   * console
   */

  public static void updateCheckedOut(GanymedeAdmin console)
  {
    try
      {
	console.proxy.setObjectsCheckedOut(Ganymede.db.objectsCheckedOut);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.

	System.err.println("Couldn't update objects checked out count on an admin console" + ex);
	badConsoles.addElement(console);
      }
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

	    updateLocksHeld(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This method updates the number of locks held on a single
   * console
   */

  public static void updateLocksHeld(GanymedeAdmin console)
  {
    try
      {
	console.proxy.setLocksHeld(Ganymede.db.lockSync.getLockCount());
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.

	System.err.println("Couldn't update locks held on an admin console" + ex);
	badConsoles.addElement(console);
      }
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

  public static void setState()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    setState(temp);
	  }
      }

    detachBadConsoles();
  }

  /**
   * This method updates the state on a single
   * console
   */

  public static void setState(GanymedeAdmin console)
  {
    try
      {
	console.proxy.changeState(GanymedeAdmin.state);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.

	System.err.println("Couldn't update Status on an admin console" + ex);
	badConsoles.addElement(console);
      }
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
		temp.proxy.changeUsers(entries);
	      }
	    catch (RemoteException ex)
	      {
		Ganymede.debug("Couldn't update user list on an admin console" + ex);
		badConsoles.addElement(temp);
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
		temp.proxy.changeTasks(scheduleHandles);
	      }
	    catch (RemoteException ex)
	      {
		Ganymede.debug("Couldn't update task list on an admin console" + ex);
		badConsoles.addElement(temp);
	      }
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

    this.proxy = new serverAdminProxy(admin);
    this.fullprivs = fullprivs;
    this.adminName = adminName;
    this.clientHost = clientHost;

    consoles.addElement(this);	// this can block if we are currently looping on consoles

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
    updateTransCount(this);
    updateLastDump(this);
    updateCheckedOut(this);
    updateLocksHeld(this);
    proxy.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
    setState(this);

    refreshUsers();
    refreshTasks();
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
   * <P>dump the current db schema to disk</P>
   */

  public ReturnVal dumpSchema()
  {
    if (!fullprivs)
      {
	return Ganymede.createErrorDialog("Permissions Denied",
					  "You do not have permissions to execute a schema dump");
      }

    setState("Dumping schema");

    try
      {
	Ganymede.db.dumpSchema(Ganymede.schemaProperty, true); // release the lock when the dump is complete
      }
    catch (IOException ex)
      {
	return Ganymede.createErrorDialog("Schema Dump Error",
					  "Schema could not be dumped successfully. " + ex);
      }
    finally
      {
	setState("Normal Operation");
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

  public ReturnVal reloadCustomClasses()
  {
    return Ganymede.createErrorDialog("Not implemented",
				      "This function never really worked, and has been removed to simplify the code.");

    /*
      Enumeration enum;
      DBObjectBase base;

      
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
      }
      }

      return null;
    */
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

    // All the DBLock establish methods synchronize on the Ganymede
    // dbstore object, so we are safe against lock establish race
    // conditions by synchronizing this section on Ganymede.db.

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

	    // this will affect consoles, which is why we are
	    // synchronized on GanymedeAdmin.consoles.

	    temp.logout("error communicating with console");
	  }

	badConsoles.setSize(0);
      }
  }
}
