/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.44 $ %D%
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
import arlut.csd.Util.ParseArgs;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        Ganymede

------------------------------------------------------------------------------*/

public class Ganymede {
  
  public static GanymedeServer server;
  public static GanymedeSession internalSession;
  public static GanymedeScheduler scheduler;
  public static DBStore db;
  public static final boolean debug = true;

  public static Date startTime = new Date();
  public static String debugFilename = null;
  public static DBLog log = null;
  public static CategoryTransport catTransport = null;
  public static BaseListTransport baseTransport = null;
  public static Vector builderTasks = new Vector();

  // properties from the ganymede.properties file
  
  public static String dbFilename = null;
  public static String journalProperty = null;
  public static String logProperty = null;
  public static String schemaProperty = null;
  public static String htmlProperty = null;
  public static String serverHostProperty = null;
  public static String rootname = null;
  public static String defaultrootpassProperty = null;
  public static String mailHostProperty = null;
  public static String returnaddrProperty = null;
  public static String signatureFileProperty = null;
  public static String helpbaseProperty = null;
  public static String monitornameProperty = null;
  public static String defaultmonitorpassProperty = null;

  public static boolean resetadmin = false;
  public static boolean firstrun = false;

  /* -- */

  public static void main(String argv[]) 
  {
    File dataFile, logFile;
    String propFilename = null;

    /* -- */

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.out.println("Error: invalid command line parameters");
	System.out.println("Usage: java Ganymede [-resetadmin] properties=<property file> [debug=<rmi debug file>]");
	return;
      }

    debugFilename = ParseArgs.getArg("debug", argv);

    resetadmin = ParseArgs.switchExists("resetadmin", argv);
    
    if (!loadProperties(propFilename))
      {
	System.out.println("Error, couldn't successfully load properties from file " + propFilename + ".");
	return;
      }
    else
      {
	System.out.println("Ganymede server: loaded properties successfully from " + propFilename);
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
	firstrun = true;

	debug("No DBStore exists under filename " + dbFilename + ", not loading");
	debug("Initializing new schema");
	db.initializeSchema();
	debug("Template schema created.");

	try 
	  {
	    db.journal = new DBJournal(db, Ganymede.journalProperty);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?
	    
	    throw new RuntimeException("couldn't initialize journal");
	  }

	debug("Creating " + rootname + " object");
	db.initializeObjects();
	debug(rootname + " object created");

	firstrun = false;
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

	debug("Creating master BaseListTransport object");
	Ganymede.baseTransport = new BaseListTransport(Ganymede.internalSession);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't establish internal session: " + ex);
      }

    // set up the log

    try
      {
	log = new DBLog(logProperty, internalSession);
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

    cal.add(Calendar.HOUR, 6);

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
	firstrun = true;

	debug("No DBStore exists under filename " + dbFilename + ", not loading");
	debug("Initializing new schema");
	db.initializeSchema();
	debug("Template schema created.");

	try 
	  {
	    db.journal = new DBJournal(db, Ganymede.journalProperty);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?
	    
	    throw new RuntimeException("couldn't initialize journal");
	  }

	debug("Creating " + rootname + " object");
	db.initializeObjects();
	debug(rootname + " object created");

	firstrun = false;
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

    debug("Fixing up passwords");

    resetadmin = true;
    startupHook();

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

    if (resetadmin)
      {
	System.out.println("Resetting supergash password.");

	internalSession.openTransaction("Ganymede startupHook");

	e_object = (DBEditObject) internalSession.session.editDBObject(new Invid(SchemaConstants.PersonaBase,
										 SchemaConstants.PersonaSupergashObj));

	if (e_object == null)
	  {
	    throw new RuntimeException("Error!  Couldn't pull " + rootname + " object");
	  }

	PasswordDBField p = (PasswordDBField) e_object.getField("Password");
	ReturnVal retval = p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password

	if (retval != null && !retval.didSucceed())
	  {
	    throw new RuntimeException("Error!  Couldn't reset " + rootname + " password");
	  }

	System.out.println(rootname + " password reset to value specified in Ganymede properties file");

	retval = internalSession.commitTransaction();

	if (retval != null && !retval.didSucceed())
	  {
	    // if doNormalProcessing is true, the
	    // transaction was not cleared, but was
	    // left open for a re-try.  Abort it.

	    if (retval.doNormalProcessing)
	      {
		internalSession.abortTransaction();
	      }
	  }
      }

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
	    pm.setPerm(SchemaConstants.UserBase, new PermEntry(true, false, false, false)); 

	    // By default, users will not be able to view, create, or edit anything.  The supergash
	    // administrator is free to reconfigure this.
	
	    pm = (PermissionMatrixDBField) e_object.getField(SchemaConstants.PermDefaultMatrix);
	    pm.setPerm(SchemaConstants.UserBase, new PermEntry(false, false, false, false)); 
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
    String builderName;
    String builderClass;
    Vector objects = internalSession.getObjects(SchemaConstants.BuilderBase);
    DBObject object;
    Class classdef;

    /* -- */

    if (objects != null)
      {
	if (objects.size() == 0)
	  {
	    System.err.println("** Empty list of builder tasks found in database!");
	  }

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
    else
      {
	System.err.println("** No builder tasks found in database!");
      }
  }

  /**
   *
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

  /**
   *
   * This method loads properties from the ganymede.properties
   * file.
   *
   * This method is public so that loader code linked with the
   * Ganymede server code can initialize the properties without
   * going through Ganymede.main().
   * 
   */

  public static boolean loadProperties(String filename)
  {
    Properties props = new Properties(System.getProperties());
    boolean success = true;

    /* -- */

    try
      {
	props.load(new BufferedInputStream(new FileInputStream(filename)));
      }
    catch (IOException ex)
      {
	return false;
      }

    // make the combined properties file accessible throughout our server
    // code.

    System.setProperties(props);

    dbFilename = System.getProperty("ganymede.database");
    journalProperty = System.getProperty("ganymede.journal");
    logProperty = System.getProperty("ganymede.log");
    schemaProperty = System.getProperty("ganymede.schemadump");
    htmlProperty = System.getProperty("ganymede.htmldump");
    serverHostProperty = System.getProperty("ganymede.serverhost");
    rootname = System.getProperty("ganymede.rootname");
    defaultrootpassProperty = System.getProperty("ganymede.defaultrootpass");
    mailHostProperty = System.getProperty("ganymede.mailhost");
    signatureFileProperty = System.getProperty("ganymede.signaturefile");
    returnaddrProperty = System.getProperty("ganymede.returnaddr");
    helpbaseProperty = System.getProperty("ganymede.helpbase");
    monitornameProperty = System.getProperty("ganymede.monitorname");
    defaultmonitorpassProperty = System.getProperty("ganymede.defaultmonitorpass");

    if (dbFilename == null)
      {
	System.err.println("Couldn't get the database property");
	success = false;
      }

    if (journalProperty == null)
      {
	System.err.println("Couldn't get the journal property");
	success = false;
      }

    if (logProperty == null)
      {
	System.err.println("Couldn't get the journal property");
	success = false;
      }

    if (schemaProperty == null)
      {
	System.err.println("Couldn't get the schema property");
	success = false;
      }

    if (htmlProperty == null)
      {
	System.err.println("Couldn't get the html dump property");
	success = false;
      }

    if (serverHostProperty == null)
      {
	System.err.println("Couldn't get the server host property");
	success = false;
      }

    if (rootname == null)
      {
	System.err.println("Couldn't get the root name property");
	success = false;
      }

    if (defaultrootpassProperty == null)
      {
	System.err.println("Couldn't get the default rootname password property");
	success = false;
      }

    if (mailHostProperty == null)
      {
	System.err.println("Couldn't get the mail host property");
	success = false;
      }

    if (returnaddrProperty == null)
      {
	System.err.println("Couldn't get the email return address property");
	success = false;
      }

    if (signatureFileProperty == null)
      {
	System.err.println("Couldn't get the signature file property");
	success = false;
      }

    if (helpbaseProperty == null || helpbaseProperty.equals(""))
      {
	System.err.println("Couldn't get the help base property.. setting to null");
	helpbaseProperty = null;
      }

    if (monitornameProperty == null)
      {
	System.err.print("Couldn't get the monitor name property.");
	success = false;
      }

    if (defaultmonitorpassProperty == null)
      {
	System.err.print("Couldn't get the default monitor password property.. ");
	System.err.println("may have problems if initializing a new db");
      }

    return success;
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

    /* -- */

    try
      {
	if (Ganymede.db.journal.clean())
	  {
	    Ganymede.debug("Deferring dump task - the journal is clean");
	    return;
	  }

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
