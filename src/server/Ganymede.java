/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.23 $ %D%
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
    Invid selfPermInv;
    StringDBField s;
    PermissionMatrixDBField pm;
    
    /* -- */

    if (false)
      {
	// manually insert the root (supergash) admin object

	internalSession.openTransaction("Ganymede startupHook");

	selfPermInv = new Invid(SchemaConstants.PermBase,
				SchemaConstants.PermSelfUserObj);

	if (internalSession.session.viewDBObject(selfPermInv) == null)
	  {
	    System.err.println("Creating the PermSelfUserObj");

	    // need to create the self perm object

	    // create SchemaConstants.PermSelfUserObj

	    e_object = (DBEditObject) internalSession.session.createDBObject(SchemaConstants.PermBase, selfPermInv);
	    
	    s = (StringDBField) e_object.getField(SchemaConstants.PermName);
	    s.setValue("Self Permissions");
	
	    // By default, users will be able to view themselves and all their fields, anything
	    // else will have to be manually configured by the supergash administrator.
	
	    pm = (PermissionMatrixDBField) e_object.getField(SchemaConstants.PermMatrix);
	    pm.setPerm(SchemaConstants.UserBase, new PermEntry(true, false, false)); 
	  }
	else
	  {
	    System.err.println("Not Creating the PermSelfUserObj");
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
