/*

   GanymedeAdmin.java

   GanymedeAdmin is the server-side implementation of the adminSession
   interface;  GanymedeAdmin provides the means by which privileged users
   can carry out privileged operations on the Ganymede server, including
   status monitoring and administrative activities.
   
   Created: 17 January 1997
   Version: $Revision: 1.6 $ %D%
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

	    entries.addElement(new AdminEntry(session.username, session.clienthost,
					      "", session.connecttime.toString(),
					      session.lastError));
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
   * This is the GanymedeAdmin constructor, used to create a new
   * server-side admin console attachment.
   *
   * Admin is an RMI remote object exported by the client in the
   * form of a callback.
   *
   */

  public GanymedeAdmin(Admin admin) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    this.admin = admin;

    consoles.addElement(admin);

    admin.setServerStart(Ganymede.startTime);
    updateTransCount(admin);
    updateLastDump(admin);
    updateCheckedOut(admin);
    updateLocksHeld(admin);
    setState(admin);
    
    refreshUsers();
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
   * Kick a user off of the Ganymede server on behalf of this admin console
   *
   */

  public synchronized boolean kill(String user)
  {
    GanymedeSession temp;

    for (int i = 0; i < GanymedeServer.sessions.size(); i++)
      {
	temp = (GanymedeSession) GanymedeServer.sessions.elementAt(i);

	if (temp.username.equals(user))
	  {
	    temp.forceOff("Admin console booting you off");
	    return true;
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
   * lock the server and edit the schema
   *
   */

  public SchemaEdit editSchema()
  {
    Enumeration enum;
    DBObjectBase base;

    /* -- */

    Ganymede.debug("entering editSchema");

    synchronized (Ganymede.db)
      {
	Ganymede.debug("entering editSchema synchronization block");

	 if (Ganymede.db.schemaEditInProgress)
	   {
	     Ganymede.debug("Can't edit Schema, edit already in progress. ");
	     return null;
	   }

	 Ganymede.debug("Schema edit not in progress");

	 enum = Ganymede.db.objectBases.elements();

	 if (enum != null)
	   {
	     while (enum.hasMoreElements())
	       {
		 base = (DBObjectBase) enum.nextElement();
		 if (base.currentLock != null)
		   {
		     Ganymede.debug("Can't edit Schema, lock held on " + base.getName());
		     return null;
		   }
	       }
	   }

	 // should be okay

	 Ganymede.db.schemaEditInProgress = true;

	 Ganymede.debug("Ok to create DBSchemaEdit");

	 GanymedeAdmin.setState("Schema Edit In Progress");

	 try
	   {
	     return new DBSchemaEdit(admin);
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
   
  private static void detachBadConsoles()
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
