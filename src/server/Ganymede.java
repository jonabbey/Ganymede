/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.27 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

import arlut.csd.JDialog.JDialogBuff;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        Ganymede

------------------------------------------------------------------------------*/

public class Ganymede {
  
  public static GanymedeServer server;
  public static GanymedeSession internalSession;
  public static GanymedeScheduler scheduler;
  public static DBStore db;
  public static String dbFilename;
  public static final boolean debug = true;
  public static Date startTime = new Date();
  public static String debugFilename = null;
  public static String logFilename = "db/log";
  public static DBLog log = null;
  public static CategoryTransport catTransport = null;
  public static Vector builderTasks = new Vector();
  
  /* -- */

  public static void main(String argv[]) 
  {
    File dataFile, logFile;

    /* -- */

    if (argv.length < 1)
      {
	System.out.println("Error: invalid command line parameters");
	System.out.println("Usage: java Ganymede <dbfile> [<rmi debug file>]");
	return;
      }
    else
      {
	dbFilename = argv[0];

	if (argv.length >= 2)
	  {
	    debugFilename = argv[1];
	  }
      }

    boolean stop = true;

    try
      {
	Naming.lookup("rmi://localhost/ganymede.server");
      }
    catch (NotBoundException ex)
      {
	stop = false;		// this is what we want to have happen
      }
    catch (java.net.MalformedURLException ex)
      {
	System.out.println("MalformedURL:" + ex);
      }
    catch (UnknownHostException ex)
      {
	System.out.println("UnknownHost:" + ex);
      }
    catch (RemoteException ex)
      {
	System.out.println("Remote:" + ex);
      }

    if (stop)
      {
	System.err.println("Warning: Ganymede server already bound by other process / Naming failure.");
      }

    debug("Creating DBStore structures");

    db = new DBStore();

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	debug("Loading DBStore contents");
	db.load(dbFilename);
      }
    else
      {
	debug("No DBStore exists under filename " + dbFilename + ", not loading");
	debug("Initializing new schema");
	db.initializeSchema();
	debug("Template schema created.");

	try 
	  {
	    db.journal = new DBJournal(db, GanymedeConfig.journal);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?
	    
	    throw new RuntimeException("couldn't initialize journal");
	  }

	debug("Creating supergash object");
	db.initializeObjects();
	debug("supergash object created");
      }

    debug("Initializing Security Manager");

    System.setSecurityManager(new RMISecurityManager());

    if (debugFilename != null)
      {
	try
	  {
	    RemoteServer.setLog(new FileOutputStream(debugFilename));
	  }
	catch (IOException ex)
	  {
	    System.err.println("couldn't open RMI debug log: " + ex);
	  }
      }

    // Create a Server object

    try
      {
	debug("Creating GanymedeServer object");

	server = new GanymedeServer(10);

	debug("Binding GanymedeServer in RMI Registry");

	Naming.rebind("ganymede.server", server);
      }
    catch (Exception ex)
      {
	debug("Couldn't establish server binding: " + ex);
	return;
      }

    try
      {
	debug("Creating internal Ganymede Session");
	internalSession = new GanymedeSession();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't establish internal session: " + ex);
      }

    // set up the log

    try
      {
	log = new DBLog(logFilename, internalSession);
      }
    catch (IOException ex)
      {
	throw new RuntimeException("Couldn't initialize log file");
      }

    String startMesg;

    if (debugFilename != null)
      {
	startMesg = "Server startup - Debug mode";
      }
    else
      {
	startMesg = "Server startup - Not in Debug mode";
      }

    log.logSystemEvent(new DBLogEvent("restart",
				      startMesg,
				      null,
				      null,
				      null,
				      null));

    startupHook();

    // start the background scheduler

    scheduler = new GanymedeScheduler(true);
    new Thread(scheduler).start();

    // throw in a couple of tasks, just for grins

    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.add(Calendar.MINUTE, 5);

    scheduler.addPeriodicAction(cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				1440, 
				new gcTask(), "Garbage Collection Task");

    cal.add(Calendar.MINUTE, 10);

    scheduler.addPeriodicAction(cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				120, 
				new dumpTask(), "Database Dumper Task");

    //    scheduler.addActionOnDemand(new sampleTask("Demand Test"), "Demand Test");
    
    scheduler.addDailyAction(0, 0, new GanymedeExpirationTask(), "Expiration Task");

    scheduler.addDailyAction(12, 0, new GanymedeWarningTask(), "Warning Task");

    // and install the builder tasks listed in the database

    registerBuilderTasks();

    // and wa-la

    if (debug)
      {
	debug("Setup and bound server object OK");
      }
  }

  /**
   *
   * This method is used to initialize the Ganymede system when it is
   * being driven by a direct-linked loader main() entry point, as a
   * single process.
   * 
   */

  public static GanymedeServer directInit(String dbFilename) 
  {
    File dataFile;

    /* -- */

    Ganymede.dbFilename = dbFilename;

    boolean stop = true;

    debug("Creating DBStore structures");

    db = new DBStore();

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	debug("Loading DBStore contents");
	db.load(dbFilename);
      }
    else
      {
	debug("No DBStore exists under filename " + dbFilename + ", not loading");
	debug("Initializing new schema");
	db.initializeSchema();
	debug("Template schema created.");

	try 
	  {
	    db.journal = new DBJournal(db, GanymedeConfig.journal);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?
	    
	    throw new RuntimeException("couldn't initialize journal");
	  }

	debug("Creating supergash object");
	db.initializeObjects();
	debug("supergash object created");
      }

    debug("Initializing Security Manager");

    // Create a Server object

    try
      {
	debug("Creating GanymedeServer object");

	server = new GanymedeServer(10);

	debug("Binding GanymedeServer in RMI Registry");
      }
    catch (Exception ex)
      {
	debug("Couldn't establish server binding: " + ex);
	return null;
      }

    try
      {
	debug("Creating internal Ganymede Session");
	internalSession = new GanymedeSession();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't establish internal session: " + ex);
      }

    if (debug)
      {
	debug("Sweeping invid links");
	server.sweepInvids();
      }

    if (debug)
      {
	debug("Setup and bound server object OK");
      }

    return server;
  }

  // debug routine

  static public void debug(String string)
  {
    if (debug)
      {
	System.err.println(string);
      }
    GanymedeAdmin.setStatus(string);
  }

  /***
   *
   * This is a convenience method used by the server to return a
   * standard error dialog.
   *
   */

  static public ReturnVal createErrorDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(false);
    retVal.setDialog(new JDialogBuff(title,
				     body,
				     "OK",
				     null,
				     "error.gif"));

    if (debug)
      {
	System.err.println("Ganymede.createErrorDialog(): dialog says " + body);
      }

    return retVal;
  }

  /**
   *
   * This method is provided to allow us to hook in creation of new
   * objects with specified invid's that the server code references.
   *
   * It's intended for use during server development as we evolve
   * the schema.
   *
   */

  static public void startupHook()
  {
    DBEditObject e_object;
    Invid defaultInv;
    StringDBField s;
    PermissionMatrixDBField pm;
    
    /* -- */

    if (false)
      {
	// manually insert the root (supergash) admin object

	internalSession.openTransaction("Ganymede startupHook");

	defaultInv = new Invid(SchemaConstants.PermBase,
				SchemaConstants.PermDefaultObj);

	if (internalSession.session.viewDBObject(defaultInv) == null)
	  {
	    System.err.println("Creating the PermDefaultObj");

	    // need to create the self perm object

	    // create SchemaConstants.PermDefaultObj

	    e_object = (DBEditObject) internalSession.session.createDBObject(SchemaConstants.PermBase, 
									     defaultInv,
									     null);
	    
	    s = (StringDBField) e_object.getField(SchemaConstants.PermName);
	    s.setValue("Default Permissions");
	
	    // By default, users will be able to view themselves and all their fields, anything
	    // else will have to be manually configured by the supergash administrator.
	
	    pm = (PermissionMatrixDBField) e_object.getField(SchemaConstants.PermMatrix);
	    pm.setPerm(SchemaConstants.UserBase, new PermEntry(true, false, false)); 

	    // By default, users will not be able to view, create, or edit anything.  The supergash
	    // administrator is free to reconfigure this.
	
	    pm = (PermissionMatrixDBField) e_object.getField(SchemaConstants.PermDefaultMatrix);
	    pm.setPerm(SchemaConstants.UserBase, new PermEntry(false, false, false)); 
	  }
	else
	  {
	    System.err.println("Not Creating the PermDefaultObj");
	  }

	ReturnVal retVal = internalSession.commitTransaction();
    
	if (retVal == null || retVal.didSucceed())
	  {
	    System.err.println("Ganymede.startupHook() succeeded");
	  }
	else
	  {
	    System.err.println("Ganymede.startupHook() did not succeed");
	  }
      }
  }

  /**
   *
   * This method scans the database for valid BuilderTask entries and 
   * adds them to the builderTasks vector.
   *
   *
   */

  static private void registerBuilderTasks()
  {
    QueryResult results = internalSession.queryDispatch(new Query(SchemaConstants.BuilderBase),
							true, false, null);
    String builderName;
    String builderClass;
    Vector objects;
    DBObject object;
    Class classdef;

    /* -- */

    if (results != null)
      {
	objects = results.getObjects();

	for (int i = 0; i < objects.size(); i++)
	  {
	    if (debug)
	      {
		System.err.println("Processing builder task object # " + i);
	      }

	    object = (DBObject) objects.elementAt(i);
	    
	    builderName = (String) object.getFieldValue(SchemaConstants.BuilderTaskName);
	    builderClass = (String) object.getFieldValue(SchemaConstants.BuilderTaskClass);

	    if (builderName != null && builderClass != null)
	      {
		try
		  {
		    classdef = Class.forName(builderClass);
		  }
		catch (ClassNotFoundException ex)
		  {
		    System.err.println("Ganymede.registerBuilderTasks(): class definition could not be found: " + ex);
		    classdef = null;
		  }
		
		GanymedeBuilderTask task = null;

		try
		  {
		    task = (GanymedeBuilderTask) classdef.newInstance(); // using no param constructor
		  }
		catch (IllegalAccessException ex)
		  {
		    System.err.println("IllegalAccessException " + ex);
		  }
		catch (InstantiationException ex)
		  {
		    System.err.println("InstantiationException " + ex);
		  }

		if (task != null)
		  {
		    scheduler.addActionOnDemand(task, builderName);
		    builderTasks.addElement(builderName);
		  }
	      }
	  }
      }
  }

  /**
   * This method scans schedules all registered builder tasks for
   * execution.  This method will be called when a user commits a
   * transaction.
   * 
   */

  static void runBuilderTasks()
  {
    for (int i = 0; i < builderTasks.size(); i++)
      {
	scheduler.demandTask((String) builderTasks.elementAt(i));
      }
  }
}

class dumpTask implements Runnable {

  public dumpTask()
  {
  }

  public void run()
   {
     boolean started = false;
     boolean completed = false;

     try
       {
	 if (Ganymede.server.activeUsers.size() > 0)
	   {
	     Ganymede.debug("Deferring dump task - users logged in");
	     return;
	   }

	 started = true;
	 Ganymede.debug("Running dump task");

	 try
	   {
	     Ganymede.db.dump(Ganymede.dbFilename, true);
	   }
	 catch (IOException ex)
	   {
	     Ganymede.debug("dump could not succeed.. IO error " + ex.getMessage());
	   }

	 Ganymede.debug("Completed dump task");
	 completed = true;
       }
     finally
       {
	 // we'll go through here if our task was stopped
	 // note that the DBStore dump code will handle
	 // thread death ok.

	 if (started && !completed)
	   {
	     Ganymede.debug("dumpTask forced to stop");
	   }
       }
   }
}

class gcTask implements Runnable {

  public gcTask()
  {
  }

  public void run()
   {
     Ganymede.debug("Running garbage collection task");
     System.gc();
     Ganymede.debug("Garbage collection task finished");
   }

}
