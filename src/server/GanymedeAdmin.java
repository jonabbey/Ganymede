/*

   GanymedeAdmin.java

   GanymedeAdmin is the server-side implementation of the adminSession
   interface;  GanymedeAdmin provides the means by which privileged users
   can carry out privileged operations on the Ganymede server, including
   status monitoring and administrative activities.
   
   Created: 17 January 1997
   Version: $Revision: 1.24 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;


/*------------------------------------------------------------------------------
                                                                           class
                                                                   GanymedeAdmin

------------------------------------------------------------------------------*/

/**
 * GanymedeAdmin is the server-side implementation of the adminSession
 * interface;  GanymedeAdmin provides the means by which privileged users
 * can carry out privileged operations on the Ganymede server, including
 * status monitoring and administrative activities.
 *
 */

class GanymedeAdmin extends UnicastRemoteObject implements adminSession {

  private static Vector consoles = new Vector();
  private static Vector badConsoles = new Vector();

  private static String state;
  private static Date lastDumpDate;

  /* --- */

  private String adminName, adminPass;
  Admin admin;
  
  /* -- */

  /**
   *
   * This static method is used to send debug log info to
   * the consoles.
   *
   */

  public static synchronized void setStatus(String status)
  {
    Admin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	try
	  {
	    temp.changeStatus(status);
	    temp.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
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
   *
   * This static method is used to send the current transcount
   * to the consoles.
   *
   */

  public static synchronized void updateTransCount()
  {
    Admin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateTransCount(temp);
      }
  }

  /**
   *
   * This static method is used to send the current transcount
   * to an individual consoles.
   *
   */

  public static void updateTransCount(Admin console)
  {
    try
      {
	console.setTransactionsInJournal(Ganymede.db.journal.transactionsInJournal);
      }
    catch (RemoteException ex)
      {
	// don't call Ganymede.debug() here or we'll get into an infinite
	// loop if we have any problems.
	System.err.println("Couldn't update transaction count on an admin console" + ex);
	badConsoles.addElement(console);
      }

    detachBadConsoles();
  }

  /**
   *
   * This static method is used to send the last dump time to
   * the consoles.
   *
   */

  public static void updateLastDump(Date date)
  {
    GanymedeAdmin.lastDumpDate = date;
    updateLastDump();
  }

  /**
   *
   * This method updates the last dump time on a all
   * consoles.
   *
   */

  public static synchronized void updateLastDump()
  {
    Admin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateLastDump(temp);
      }
  }

  /**
   *
   * This method updates the last dump time on a single
   * console
   *
   */

  public static void updateLastDump(Admin console)
  {
    try
      {
	console.setLastDumpTime(GanymedeAdmin.lastDumpDate);
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
   *
   * This method updates the objects checked out count on all consoles.
   *
   */

  public static void updateCheckedOut()
  {
    Admin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateCheckedOut(temp);
      }
  }

  /**
   *
   * This method updates the objects checked out count on a single
   * console
   *
   */

  public static void updateCheckedOut(Admin console)
  {
    try
      {
	console.setObjectsCheckedOut(Ganymede.db.objectsCheckedOut);
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
    Admin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	updateLocksHeld(temp);
      }
  }

  /**
   *
   * This method updates the number of locks held on a single
   * console
   *
   */

  public static void updateLocksHeld(Admin console)
  {
    try
      {
	console.setLocksHeld(Ganymede.db.locksHeld);
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
   *
   * This method changes the system state and
   * sends it out to the consoles
   *
   */

  public static void setState(String state)
  {
    GanymedeAdmin.state = state;
    setState();
  }

  /**
   *
   * This method updates the state on all
   * attached consoles
   *
   */

  public static synchronized void setState()
  {
    Admin temp;

    /* -- */

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  {
	    continue;
	  }

	setState(temp);
      }
  }

  /**
   *
   * This method updates the state on a single
   * console
   *
   */

  public static void setState(Admin console)
  {
    try
      {
	console.changeState(GanymedeAdmin.state);
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
   *
   * This static method is used to update the list of connnected users that
   * appears in any admin consoles attached to the Ganymede server.
   *
   */

  public static synchronized void refreshUsers()
  {
    Admin temp;
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
					      (session.lastEvent == null) ? "" : session.lastEvent));
	  }
      }

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  continue;

	try
	  {
	    temp.changeUsers(entries);
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
   *
   * This static method is used to update the list of connnected users that
   * appears in any admin consoles attached to the Ganymede server.
   *
   */

  public static synchronized void refreshTasks()
  {
    Admin temp;
    Vector scheduleHandles;

    /* -- */
    
    if (Ganymede.scheduler == null)
      {
	return;
      }
    
    scheduleHandles = Ganymede.scheduler.reportTaskInfo();

    for (int i = 0; i < consoles.size(); i++)
      {
	temp = (Admin) consoles.elementAt(i);

	if (temp == null)
	  continue;

	try
	  {
	    temp.changeTasks(scheduleHandles);
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
   *
   * This is the GanymedeAdmin constructor, used to create a new
   * server-side admin console attachment.
   *
   * Admin is an RMI remote object exported by the client in the
   * form of a callback.
   *
   */

  public GanymedeAdmin(Admin admin, String adminName, String adminPass) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    this.admin = admin;
    this.adminName = adminName;
    this.adminPass = adminPass;

    consoles.addElement(admin);

    admin.setServerStart(Ganymede.startTime);
    updateTransCount(admin);
    updateLastDump(admin);
    updateCheckedOut(admin);
    updateLocksHeld(admin);
    setState(admin);
    
    refreshUsers();
    refreshTasks();
  }

  /**
   *
   * Disconnect the remote admin console associated with this object
   *
   */

  public synchronized void logout()
  {
    consoles.removeElement(admin);
    this.admin = null;
    Ganymede.debug("Admin console detached " + new Date());
  }


  /**
   *
   * Disconnect the remote admin console associated with this object
   *
   */

  public synchronized void refreshMe() throws RemoteException
  {
    admin.setServerStart(Ganymede.startTime);
    updateTransCount(admin);
    updateLastDump(admin);
    updateCheckedOut(admin);
    updateLocksHeld(admin);
    admin.changeAdmins(consoles.size() + " console" + (consoles.size() > 1 ? "s" : "") + " attached");
    setState(admin);

    refreshUsers();
    refreshTasks();
  }

  /**
   *
   * Kick all users off of the Ganymede server on behalf of this admin console
   *
   */

  public synchronized boolean killAll()
  {
    GanymedeSession temp;

    if (!adminName.equals(Ganymede.rootname))
      {
	return false;
      }

    while (GanymedeServer.sessions.size() != 0)
      {
	temp = (GanymedeSession) GanymedeServer.sessions.elementAt(0);
	temp.forceOff("Admin console disconnecting you");
      }

    return true;
  }

  /**
   *
   * Kick a user off of the Ganymede server on behalf of this admin console
   *
   */

  public synchronized boolean kill(String user)
  {
    GanymedeSession temp;

    if (!adminName.equals(Ganymede.rootname))
      {
	return false;
      }

    for (int i = 0; i < GanymedeServer.sessions.size(); i++)
      {
	temp = (GanymedeSession) GanymedeServer.sessions.elementAt(i);

	if (temp.personaName != null)
	  {
	    if (temp.personaName.equals(user))
	      {
		temp.forceOff("Admin console disconnecting you");
		return true;
	      }
	  }
	else
	  {
	    if (temp.username.equals(user))
	      {
		temp.forceOff("Admin console disconnecting you");
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   *
   * Get information about a particular user that 
   * is logged in.  We will eventually want to return
   * a data_object, probably.
   *
   */

  public synchronized String getInfo(String user)
  {
    return "No Info Available";
  }

  /**
   *
   * shutdown the server cleanly, on behalf of this admin console.
   *
   * right now we don't do any checking to make sure this is kosher
   * to do, we'll have to do that once we actually start doing anything
   * in the database.
   *
   */

 public boolean shutdown()
  {
    GanymedeSession temp;
    Admin atmp;
    Vector tempList;

    /* -- */

    if (!adminName.equals(Ganymede.rootname))
      {
	return false;
      }

    setStatus("Server going down.. performing final dump");

    // dump, then shut down.  Our second dump parameter is false,
    // so that we are guaranteed that no client can get a writelock
    // and maybe get a transaction off that would cause us confusion.

    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, false); // don't release lock
      }
    catch (IOException ex)
      {
	Ganymede.debug("shutdown error: couldn't successfully dump db:" + ex );
	return false;
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

    for (int i = 0; i < consoles.size(); i++)
      {
	atmp = (Admin) consoles.elementAt(i);

	try
	  {
	    atmp.forceDisconnect("Server going down now.");
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

    return true;		// we'll never get here
  }

  /**
   *
   * dump the current state of the db to disk
   *
   */

  public boolean dumpDB()
  {
    if (!adminName.equals(Ganymede.rootname))
      {
	return false;
      }

    setStatus("Dumping database");

    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, true); // release the
				                     // lock when the dump is complete
      }
    catch (IOException ex)
      {
	Ganymede.debug("Database could not be dumped successfully. " + ex);
	return false;
      }

    Ganymede.debug("Database dumped");

    return true;
  }


  /**
   *
   * dump the current db schema to disk
   *
   */

  public boolean dumpSchema()
  {
    if (!adminName.equals(Ganymede.rootname))
      {
	return false;
      }

    setStatus("Dumping schema");

    try
      {
	Ganymede.db.dumpSchema(Ganymede.schemaProperty, true); // release the lock when the dump is complete
      }
    catch (IOException ex)
      {
	Ganymede.debug("Schema could not be dumped successfully. " + ex);
	return false;
      }

    Ganymede.debug("Schema dumped");

    return true;
  }

  /**
   *
   * This method causes the server to reload any registered
   * custom classes, and can be run after a schema edit
   * to cause the new classes to take over management of
   * their respective object types.
   *
   */

  public synchronized boolean reloadCustomClasses()
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */
    
    synchronized (Ganymede.server)
      {
	if (GanymedeServer.sessions.size() != 0)
	  {
	    Ganymede.debug("Can't reload classes, users logged in");
	    return false;
	  }
	else if (!Ganymede.db.schemaEditInProgress)
	  {
	    Ganymede.db.schemaEditInProgress = true;
	  }
	else
	  {
	    Ganymede.debug("Can't reload classes, schema edit already in progress. ");
	    return false;
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

    return true;
  }

  /**
   *
   * run a long-running verification suite on the invid links
   *
   */

  public void runInvidTest()
  {
    if (!adminName.equals(Ganymede.rootname))
      {
	return;
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

    return;
  }

  /**
   *
   * run a long-running verification suite on the invid links
   *
   */

  public void runInvidSweep()
  {
    if (!adminName.equals(Ganymede.rootname))
      {
	return;
      }

    GanymedeAdmin.setState("Running Invid Sweep");
	 
    Ganymede.server.sweepInvids();

    GanymedeAdmin.setState("Normal Operation");

    return;
  }

  public boolean runTaskNow(String name)
  {
    return Ganymede.scheduler.runTaskNow(name);
  }

  public boolean stopTask(String name)
  {
    return Ganymede.scheduler.stopTask(name);
  }

  public boolean disableTask(String name)
  {
    return Ganymede.scheduler.disableTask(name);
  }

  public boolean enableTask(String name)
  {
    return Ganymede.scheduler.enableTask(name);
  }
  
  public boolean rescheduleTask(String name, Date time, int interval)
  {
    return false;
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

    if (!adminName.equals(Ganymede.rootname))
      {
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
		if (base.currentLock != null)
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
    Admin temp;

    /* -- */

    for (int i=0; i < badConsoles.size(); i++)
      {
	temp = (Admin) badConsoles.elementAt(i);

	if (temp == null)
	  continue;

	testval = false;

	try
	  {
	    temp.getPassword();
	  }
	catch (RemoteException ex)
	  {
	    testval = true;
	  }

	if (testval)
	  {
	    consoles.removeElement(temp);
	  }
      }

    badConsoles = new Vector();
  }
  
}
