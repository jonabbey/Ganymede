/*

   Ganymede.java

   Server main module

   This class is the main server module, providing the static main()
   method executed to start the server.

   This class is never instantiated, but instead provides a bunch of
   static variables and convenience methods in addition to the main()
   start method.

   Created: 17 January 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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

package arlut.csd.ddroid.server;

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        Ganymede

------------------------------------------------------------------------------*/

/** 
 * <p>This class is the main server module, providing the static
 * main() method executed to start the server.  This class is never
 * instantiated, but instead provides a bunch of static variables and
 * convenience methods in addition to the main() start method.</p>
 *
 * <p>When started, the Directory Droid server creates a 
 * {@link arlut.csd.ddroid.server.DBStore DBStore} object, which in turn
 * creates and loads a set of {@link arlut.csd.ddroid.server.DBObjectBase DBObjectBase}
 * objects, one for each type of object held in the ganymede.db file.  Each
 * DBObjectBase contains {@link arlut.csd.ddroid.server.DBObject DBObject}
 * objects which hold the {@link arlut.csd.ddroid.server.DBField DBField}'s
 * which ultimately hold the actual data from the database.</p>
 *
 * <p>The ganymede.db file may define a number of task classes that are to
 * be run by the server at defined times.  The server's main() method starts
 * a background {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler}
 * thread to handle background tasks.</p>
 *
 * <p>When the database has been loaded from disk, the main() method
 * creates a {@link arlut.csd.ddroid.server.GanymedeServer GanymedeServer}
 * object.  GanymedeServer implements the {@link arlut.csd.ddroid.rmi.Server
 * Server} RMI remote interface, and is published in the RMI registry.</p>
 *
 * <p>Clients and admin consoles may then connect to the published GanymedeServer
 * object via RMI to establish a connection to the server.</p>
 *
 * <p>The GanymedeServer's {@link arlut.csd.ddroid.server.GanymedeServer#login(arlut.csd.ganymede.Client) login}
 * method is used to create a {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}
 * object to manage permissions and communications with an individual client.  The
 * client communicates with the GanymedeSession object through the 
 * {@link arlut.csd.ddroid.rmi.Session Session} RMI remote interface.</p>
 *
 * <p>While the GanymedeServer's login method is used to handle client connections,
 * the GanymedeServer's
 * {@link arlut.csd.ddroid.server.GanymedeServer#admin(arlut.csd.ganymede.Admin) admin}
 * method is used to create a {@link arlut.csd.ddroid.server.GanymedeAdmin GanymedeAdmin} object
 * to handle the admin console's communications with the server.  The admin
 * console communicates with the GanymedeAdmin object through the  
 * {@link arlut.csd.ddroid.rmi.adminSession adminSession} RMI remote interface.</p>
 *
 * <p>Most of the server's database logic is handled by the DBStore object
 * and its related classes ({@link arlut.csd.ddroid.server.DBObject DBObject},
 * {@link arlut.csd.ddroid.server.DBEditSet DBEditSet}, {@link arlut.csd.ddroid.server.DBNameSpace DBNameSpace},
 * and {@link arlut.csd.ddroid.server.DBJournal DBJournal}).</p>
 *
 * <p>All client permissions and communications are handled by the GanymedeSession class.</p> 
 */

public class Ganymede {

  public static boolean debug = true;

  /**
   * <p>If true, Ganymede.createErrorDialog() will print the
   * content of error dialogs to the server's stderr.</p>
   */

  public static boolean logErrorDialogs = true;

  /**
   * <p>If true, Ganymede.createInfoDialog() will print the
   * content of info dialogs to the server's stderr.</p>
   */

  public static boolean logInfoDialogs = true;

  /**
   * <p>We keep the server's start time for display in the
   * admin console.</p>
   */

  public static Date startTime = new Date();

  /**
   * <p>If the server is started with debug=&lt;filename&gt; on
   * the command line, debugFilename will hold the name
   * of the file to write our RMI debug log to.</p>
   */

  public static String debugFilename = null;

  /**
   * <p>If true, {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}
   * will export any objects being viewed, edited, or created before
   * returning it to the client.  This will be false during direct
   * loading, which should double load speed.</p>
   */

  public static boolean remotelyAccessible = true;

  /**
   * <p>Once the server is started and able to accept RMI clients,
   * this field will hold the GanymedeServer object which clients
   * talk to in order to login to the server.</p>
   */

  public static GanymedeServer server;

  /**
   * <p>A number of operations in the Directory Droid server require 'root' 
   * access to the database.  This GanymedeSession object is provided
   * for system database operations.</p>
   */

  public static GanymedeSession internalSession;

  /**
   * <p>The background task scheduler.</p>
   */

  public static GanymedeScheduler scheduler;

  /**
   *
   * The Directory Droid object store.
   * 
   */

  public static DBStore db;

  /**
   * <p>This object provides access to the Directory Droid log file, providing
   * logging, email, and search services.</p>
   */

  public static DBLog log = null;

  /**
   * <p>A cached reference to a master category tree serialization
   * object.  Initialized the first time a user logs on to the server,
   * and re-initialized when the schema is edited.  This object is
   * provided to clients when they call 
   * {@link arlut.csd.ddroid.server.GanymedeSession#getCategoryTree() GanymedeSession.getCategoryTree()}.</p>
   */

  public static CategoryTransport catTransport = null;

  /**
   * <p>A cached reference to a master base list serialization object.
   * Initialized on server start up and re-initialized when the schema
   * is edited.  This object is provided to clients when they call
   * {@link arlut.csd.ddroid.server.GanymedeSession#getBaseList() GanymedeSession.getBaseList()}.</p>
   */

  public static BaseListTransport baseTransport = null;

  /**
   * <p>A vector of {@link arlut.csd.ddroid.server.GanymedeBuilderTask GanymedeBuilderTask}
   * objects initialized on database load.</p>
   */

  public static Vector builderTasks = new Vector();

  // properties from the ganymede.properties file
  
  public static String dbFilename = null;
  public static String journalProperty = null;
  public static String logProperty = null;
  public static String mailLogProperty = null;
  public static String htmlProperty = null;
  public static String serverHostProperty = null;
  public static String rootname = null;
  public static String defaultrootpassProperty = null;
  public static String mailHostProperty = null;
  public static String returnaddrProperty = null;
  public static String subjectPrefixProperty = null;
  public static String signatureFileProperty = null;
  public static String helpbaseProperty = null;
  public static String monitornameProperty = null;
  public static String defaultmonitorpassProperty = null;
  public static String messageDirectoryProperty = null;
  public static String schemaDirectoryProperty = null;
  public static int    registryPortProperty = 1099;
  public static String logHelperProperty = null;
  public static boolean softtimeout = false;
  public static int timeoutIdleNoObjs = 15;
  public static int timeoutIdleWithObjs = 20;

  /**
   * <p>If the server is started with the -resetadmin command line flag,
   * this field will be set to true and the server's startupHook() will
   * reset the supergash password to that specified in the server's
   * ganymede.properties file.</p>
   */

  public static boolean resetadmin = false;

  /** 
   * <p>This flag is true if the server was started with no
   * pre-existing ganymede.db file.  This will be true when the server
   * code is run from a schema kit's runDirectLoader script.  If true,
   * the {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}
   * class will not worry about not finding the default permissions
   * role in the database.</p> 
   */

  public static boolean firstrun = false;

  /** 
   * <p>This flag is true if the server was started with a -forcelocalhost
   * command line argument, which will allow the server to run even if
   * it will only be accessible to localhost.</p>
   */

  public static boolean forcelocalhost = false;

  /**
   * <p>TranslationService object for handling string localization in the Ganymede
   * server.</p>
   */

  public static TranslationService ts = null;

  /* -- */

  /**
   *
   * The Directory Droid server start point.
   *
   */

  public static void main(String argv[]) 
  {
    File dataFile;
    String propFilename = null;

    /* -- */

    ts = TranslationService.getTranslationService("arlut.csd.ddroid.server.ganymede");

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.err.println(ts.l("main.cmd_line_error"));
 	System.err.println(ts.l("main.cmd_line_usage"));
	return;
      }

    debugFilename = ParseArgs.getArg("debug", argv);

    resetadmin = ParseArgs.switchExists("resetadmin", argv);

    forcelocalhost = ParseArgs.switchExists("forcelocalhost", argv);

    if (!loadProperties(propFilename))
      {
	System.err.println(ts.l("main.error_propload", propFilename));
	return;
      }
    else
      {
	System.out.println(ts.l("main.ok_propload", propFilename));
      }

    // see whether our RMI registry currently has ganymede.server
    // bound.. doesn't matter much, but useful for logging

    boolean inUse = true;

    try
      {
	String rmiServerURL = "rmi://" + 
	  java.net.InetAddress.getLocalHost().getHostName() + ":" + 
	  registryPortProperty + "/ganymede.server";

	Remote obj = Naming.lookup(rmiServerURL); // hopefully we'll throw an exception here

	if (obj instanceof Server)
	  {
	    Server serv = (Server) obj;
	    
	    if (serv.up())	// another exception opportunity
	      {
		System.err.println(ts.l("main.error_already_running", rmiServerURL));
		System.exit(1);
	      }
	  }
      }
    catch (NotBoundException ex)
      {
	inUse = false;		// this is what we want to have happen
      }
    catch (java.net.MalformedURLException ex)
      {
	System.err.println("MalformedURL:" + ex);
      }
    catch (java.net.UnknownHostException ex)
      {
	System.err.println("UnknownHost:" + ex);
      }
    catch (RemoteException ex)
      {
	// we expect to see a RemoteException if we had an old
	// server bound
      }

    // inUse can be true if we were able to lookup an RMI object by
    // name, yet not able to actually talk to it, which would happen
    // if an old server bound on this system had died/been killed
    // without restarting the rmi registry process

    if (inUse)
      {
	System.err.println(ts.l("main.info_reusing_registry"));
      }

    // create the database 

    debug(ts.l("main.info_creating_dbstore"));

    db = new DBStore();		// And how can this be!?  For he IS the kwizatch-haderach!!

    // load the database

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	debug(ts.l("main.info_loading_dbstore"));
	db.load(dbFilename, true);
      }
    else
      {
	// no database on disk.. create a new one, along with a new
	// journal

	firstrun = true;

	debug(ts.l("main.info_new_dbstore", dbFilename));
	debug(ts.l("main.info_initializing_schema"));
	db.initializeSchema();
	debug(ts.l("main.info_created_schema"));

	try 
	  {
	    db.journal = new DBJournal(db, Ganymede.journalProperty);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?
	    
	    throw new RuntimeException(ts.l("main.error_no_init_journal"));
	  }

	// create the database objects required for the server's operation

	debug(ts.l("main.info_creating_mandatory"));
	db.initializeObjects();
	debug(ts.l("main.info_created_mandatory"));

	firstrun = false;
      }

    // Java 2 makes it a real pain to change out security
    // managers.. since we don't need to do classfile transfer from
    // the client (since all clients should use
    // arlut.csd.ddroid.client.ClientBase), we just don't bother
    // with it.

    if (false)
      {
	debug(ts.l("main.info_init_security"));

	System.setSecurityManager(new RMISecurityManager());
      }
    else
      {
	debug(ts.l("main.info_no_security"));
      }

    // if debug=<filename> was specified on the command line, tell the
    // RMI system to log RMI calls and exceptions that occur in
    // response to RMI calls.

    if (debugFilename != null)
      {
	try
	  {
	    RemoteServer.setLog(new FileOutputStream(debugFilename));
	  }
	catch (IOException ex)
	  {
	    System.err.println(ts.l("main.error_fail_debug") + ex);
	  }
      }

    // Create a GanymedeServer object to support the logging
    // code... the GanymedeServer's main purpose (to allow logins)
    // won't come into play until we bind the server object into the
    // RMI registry.

    try
      {
	debug(ts.l("main.info_creating_server"));

	server = new GanymedeServer();
      }
    catch (Exception ex)
      {
	debug(ts.l("main.error_fail_server") + ex);
	throw new RuntimeException(ex.getMessage());
      }

    // create the internal GanymedeSession that we use for system
    // database maintenance

    try
      {
	debug(ts.l("main.info_creating_def_session"));
	internalSession = new GanymedeSession();
	internalSession.enableWizards(false);
	internalSession.enableOversight(false);

	debug(ts.l("main.info_creating_baselist_trans"));

	Ganymede.baseTransport = Ganymede.internalSession.getBaseList();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException(ts.l("main.error_fail_session") + ex);
      }

    // set up the log

    try
      {
	if (mailLogProperty != null && !mailLogProperty.equals(""))
	  {
	    log = new DBLog(new DBLogFileController(logProperty), 
			    new DBLogFileController(mailLogProperty), 
			    internalSession);
	  }
	else
	  {
	    log = new DBLog(new DBLogFileController(logProperty), 
			    null,
			    internalSession);
	  }
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ts.l("main.error_log_file"));
      }

    // log our restart

    String startMesg;

    if (debugFilename != null)
      {
	startMesg = ts.l("main.info_debug_start");
      }
    else
      {
	startMesg = ts.l("main.info_nodebug_start");
      }

    log.logSystemEvent(new DBLogEvent("restart",
				      startMesg,
				      null,
				      null,
				      null,
				      null));

    // take care of any startup-time database modifications

    try
      {
	startupHook();
      }
    catch (NotLoggedInException ex)
      {
	throw new Error(ts.l("main.error_myst_nologged") + ex.getMessage());
      }

    // Create the background scheduler

    scheduler = new GanymedeScheduler(true);

    // set the background scheduler running on its own thread

    scheduler.start();

    // and install the tasks listed in the database

    try
      {
	registerTasks();
      }
    catch (NotLoggedInException ex)
      {
	throw new Error(ts.l("main.error_myst_nologged") + ex.getMessage());
      }

    // Bind the GanymedeServer object in the RMI registry so clients
    // and admin consoles can connect to us.

    try
      {
	debug(ts.l("main.info_binding_registry"));

	// we use rebind so that we can bind successfully if the rmi
	// registry is still running from a previous Directory Droid server
	// session.

	String hostname = null;

	if (!java.net.InetAddress.getLocalHost().getHostAddress().equals("127.0.0.1"))
	  {
	    hostname = java.net.InetAddress.getLocalHost().getHostName();
	  }
	else
	  {
	    // we don't want to bind to a system name that will
	    // resolve to 127.0.0.1, or else other the rmiregistry
	    // will attempt to report our address as loopback, which
	    // won't do anyone any good

	    // try to use the name specified in our ganymede.properties file

	    if (serverHostProperty != null && !serverHostProperty.equals(""))
	      {
		hostname = serverHostProperty;

		if (java.net.InetAddress.getByName(hostname).getHostAddress().equals("127.0.0.1"))
		  {
		    // nope, give up

		    if (forcelocalhost)
		      {
			Ganymede.debug("\n** " + ts.l("main.warning") + " **\n");
		      }
		    else
		      {
			Ganymede.debug("\n** " + ts.l("main.error") + " **\n");
		      }

		    Ganymede.debug(ts.l("main.error_loopback",
					java.net.InetAddress.getLocalHost().getHostName(),
					serverHostProperty));
		    Ganymede.debug("\n");
		    Ganymede.debug(ts.l("main.error_loopback_explain"));

		    if (!forcelocalhost)
		      {
			Ganymede.debug(ts.l("main.error_loopback_explain2"));
			
			Ganymede.debug("\n" + ts.l("main.info_shutting_down") + "\n");
			
			GanymedeServer.shutdown();
		      }
		  }
		else
		  {
		    Ganymede.debug(ts.l("main.info_avoiding_loopback",
					java.net.InetAddress.getLocalHost().getHostName(),
					hostname));
		  }
	      }
	  }

	// tell the RMI registry where to find the server

	Naming.rebind("rmi://" + hostname + ":" + registryPortProperty + "/ganymede.server", 
		      server);
      }
    catch (Exception ex)
      {
	debug(ts.l("main.error_no_binding") + ex);
	throw new RuntimeException(ex.getMessage());
      }

    // at this point clients can log in to the server.. the RMI system
    // will spawn threads as necessary to handle RMI client activity..
    // the main thread has nothing left to do and can go ahead and
    // terminate here.

    // wa-la

    if (debug)
      {
	debug(ts.l("main.info_setup_okay"));
      }

    debug(ts.l("main.info_ready"));
  }

  // debug routine

  /**
   *
   * This is a convenience method used by server-side code to send
   * debug output to stderr and to any attached admin consoles.
   *
   */

  static public void debug(String string)
  {
    if (debug)
      {
	System.err.println(string);
      }

    GanymedeAdmin.setStatus(string);
  }

  /**
   * This is a convenience method used by the server to get a
   * stack trace from a throwable object in String form.
   */

  static public String stackTrace(Throwable thing)
  {
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    thing.printStackTrace(writer);
    writer.close();

    return stringTarget.toString();
  }

  /**
   * This is a convenience method used by the server to return a
   * standard informative dialog.
   */

  static public ReturnVal createInfoDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(true,true); // success ok, doNormalProcessing ok
    retVal.setDialog(new JDialogBuff(title,
				     body,
				     "OK",
				     null,
				     "ok.gif"));

    if (logInfoDialogs)
      {
	System.err.println(ts.l("createInfoDialog.log_info", body));
      }

    return retVal;
  }

  /**
   * This is a convenience method used by the server to return a
   * standard error dialog.
   */

  static public ReturnVal createErrorDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(false);
    retVal.setDialog(new JDialogBuff(title,
				     body,
				     ts.l("createErrorDialog.ok"),
				     null,
				     "error.gif"));

    if (logErrorDialogs)
      {
	System.err.println(ts.l("createErrorDialog.log_error", body));
      }

    return retVal;
  }

  /**
   * This is a convenience method used by the server to return a
   * very standard error dialog.
   *
   * The Exception parameter is ignored for now, so that this method
   * can do something with it later if necessary without having to go
   * through all the code which calls this method.
   */

  static public ReturnVal loginError(Exception ex)
  {
    return Ganymede.createErrorDialog(ts.l("loginError.error"),
				      ts.l("loginError.explain"));
  }

  /**
   * <p>This method is provided to allow us to hook in creation of new
   * objects with specified invid's that the server code references.</p>
   *
   * <p>It's intended for use during server development as we evolve
   * the schema.</p>
   */

  static public void startupHook() throws NotLoggedInException
  {
    Invid supergashinvid = Invid.createInvid(SchemaConstants.PersonaBase,
					     SchemaConstants.PersonaSupergashObj);
    DBObject v_object;
    DBEditObject e_object;

    PasswordDBField p;

    Invid defaultInv;
    StringDBField s;
    PermissionMatrixDBField pm;

    ReturnVal retVal;
    
    /* -- */

    // check to make sure the datastructures for supergash in the
    // ganymede.db file are set.

    Ganymede.db.initializeObjects();

    // and reset the password if we need to

    if (resetadmin && Ganymede.defaultrootpassProperty != null && !Ganymede.defaultrootpassProperty.trim().equals(""))
      {
	// check to see if we need to reset the password to match our
	// properties file

	v_object = DBStore.viewDBObject(supergashinvid);
	p = (PasswordDBField) v_object.getField("Password");

	if (p == null || !p.matchPlainText(Ganymede.defaultrootpassProperty))
	  {
	    System.out.println(ts.l("startupHook.resetting"));

	    internalSession.openTransaction("Directory Droid startupHook");

	    e_object = (DBEditObject) internalSession.session.editDBObject(supergashinvid);

	    if (e_object == null)
	      {
		throw new RuntimeException(ts.l("startupHook.no_supergash", rootname));
	      }

	    p = (PasswordDBField) e_object.getField("Password");
	    ReturnVal retval = p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password
	    
	    if (retval != null && !retval.didSucceed())
	      {
		throw new RuntimeException(ts.l("startupHook.failed_reset", rootname));
	      }

	    System.out.println(ts.l("startupHook.password_reset", rootname));

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
      }
  }

  /**
   * This method scans the database for valid BuilderTask entries and 
   * adds them to the builderTasks vector.
   */

  static private void registerTasks() throws NotLoggedInException
  {
    String builderName;
    String builderClass;
    Vector objects = internalSession.getObjects(SchemaConstants.TaskBase);
    DBObject object;
    Class classdef;

    /* -- */

    if (objects != null)
      {
	if (objects.size() == 0)
	  {
	    System.err.println(ts.l("registerTasks.empty_builders"));
	  }

	for (int i = 0; i < objects.size(); i++)
	  {
	    object = (DBObject) objects.elementAt(i);

	    if (debug)
	      {
		System.err.println(ts.l("registerTasks.processing_task", object.toString()));
	      }

	    scheduler.registerTaskObject(object);

	    if (object.isSet(SchemaConstants.TaskRunOnCommit))
	      {
		registerBuilderTask((String) object.getFieldValueLocal(SchemaConstants.TaskName));
	      }
	  }
      }
    else
      {
	System.err.println(ts.l("registerTasks.empty_tasks"));
      }

    // register background time-out task

    scheduler.addPeriodicAction(new Date(System.currentTimeMillis() + 60000),
				1,
				new timeOutTask(),
				ts.l("registerTasks.idle_task"));

    scheduler.addPeriodicAction(new Date(System.currentTimeMillis() + 60000),
				1,
				new memoryStatusTask(),
				ts.l("registerTasks.memory_status_task"));

    scheduler.addActionOnDemand(new gcTask(),
				ts.l("registerTasks.gc_task"));
  }

  static void registerBuilderTask(String taskName)
  {
    if (debug)
      {
	System.err.println(ts.l("registerBuilderTask.debug_register", taskName));
      }

    synchronized (builderTasks)
      {
	arlut.csd.Util.VectorUtils.unionAdd(builderTasks, taskName);
      }
  }

  static void unregisterBuilderTask(String taskName)
  {
    if (debug)
      {
	System.err.println(ts.l("unregisterBuilderTask.debug_unregister", taskName));
      }

    builderTasks.removeElement(taskName); // sync'ed on builderTasks vector method
  }

  /**
   * This method schedules all registered builder tasks for
   * execution.  This method will be called when a user commits a
   * transaction.
   */

  static void runBuilderTasks()
  {
    synchronized (builderTasks)
      {
	for (int i = 0; i < builderTasks.size(); i++)
	  {
	    scheduler.demandTask((String) builderTasks.elementAt(i));
	  }
      }
  }

  /**
   * This method schedules all registered builder tasks for
   * execution, with an option set that will cause all builder tasks
   * to consider object bases as changed since the last build, thus
   * triggering a full external rebuild.
   */

  static void forceBuilderTasks()
  {
    String[] options = {"forcebuild"};

    synchronized (builderTasks)
      {
	for (int i = 0; i < builderTasks.size(); i++)
	  {
	    scheduler.demandTask((String) builderTasks.elementAt(i), options);
	  }
      }
  }

  /**
   * </P>This method is called by the GanymedeBuilderTask base class to
   * record that the server is processing a build.</P>
   */

  static void updateBuildStatus()
  {
    int p1 = GanymedeBuilderTask.getPhase1Count();
    int p2 = GanymedeBuilderTask.getPhase2Count();

    // phase 1 can have the database locked, so show that
    // for preference

    if (p1 > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(1, "building");
      }
    else if (p2 > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(1, "building2");
      }
    else
      {
	GanymedeServer.sendMessageToRemoteSessions(1, "idle");
      }
  }

  /**
   * <p>This method loads properties from the ganymede.properties
   * file.</p>
   *
   * <p>This method is public so that loader code linked with the
   * Directory Droid server code can initialize the properties without
   * going through Ganymede.main().</p>
   */

  public static boolean loadProperties(String filename)
  {
    Properties props = new Properties(System.getProperties());
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    boolean success = true;

    /* -- */

    try
      {    
	fis = new FileInputStream(filename);
	bis = new BufferedInputStream(fis);
	props.load(bis);
      }
    catch (IOException ex)
      {
	return false;
      }
    finally
      {
	// don't wait for GC to close the file descriptors

	try
	  {
	    if (bis != null)
	      {
		bis.close();
	      }
	  }
	catch (IOException ex)
	  {
	  }

	try
	  {
	    if (fis != null)
	      {
		fis.close();
	      }
	  }
	catch (IOException ex)
	  {
	  }
      }

    // make the combined properties file accessible throughout our server
    // code.

    System.setProperties(props);

    dbFilename = System.getProperty("ganymede.database");
    journalProperty = System.getProperty("ganymede.journal");
    logProperty = System.getProperty("ganymede.log");
    mailLogProperty = System.getProperty("ganymede.maillog");
    htmlProperty = System.getProperty("ganymede.htmldump");
    serverHostProperty = System.getProperty("ganymede.serverhost");
    rootname = System.getProperty("ganymede.rootname");
    defaultrootpassProperty = System.getProperty("ganymede.defaultrootpass");
    mailHostProperty = System.getProperty("ganymede.mailhost");
    signatureFileProperty = System.getProperty("ganymede.signaturefile");
    returnaddrProperty = System.getProperty("ganymede.returnaddr");
    subjectPrefixProperty = System.getProperty("ganymede.subjectprefix");
    helpbaseProperty = System.getProperty("ganymede.helpbase");
    monitornameProperty = System.getProperty("ganymede.monitorname");
    defaultmonitorpassProperty = System.getProperty("ganymede.defaultmonitorpass");
    messageDirectoryProperty = System.getProperty("ganymede.messageDirectory");
    schemaDirectoryProperty = System.getProperty("ganymede.schemaDirectory");
    logHelperProperty = System.getProperty("ganymede.loghelper");

    String softtimeoutString = System.getProperty("ganymede.softtimeout");

    if (softtimeoutString != null && softtimeoutString.equalsIgnoreCase("true"))
      {
	softtimeout = true;
      }

    String timeoutIdleNoObjsString = System.getProperty("ganymede.timeoutIdleNoObjs");

    if (timeoutIdleNoObjsString != null)
      {
	try
	  {
	    timeoutIdleNoObjs = java.lang.Integer.parseInt(timeoutIdleNoObjsString);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println(ts.l("loadProperties.no_parse_timeoutIdleNoObjs", timeoutIdleNoObjsString));
	  }
      }

    String timeoutIdleWithObjsString = System.getProperty("ganymede.timeoutIdleWithObjs");

    if (timeoutIdleWithObjsString != null)
      {
	try
	  {
	    timeoutIdleWithObjs = java.lang.Integer.parseInt(timeoutIdleNoObjsString);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println(ts.l("loadProperties.no_parse_timeoutIdleWithObjs",
				    timeoutIdleWithObjsString));
	  }
      }

    if (dbFilename == null)
      {
	System.err.println(ts.l("loadProperties.no_db"));
	success = false;
      }

    if (journalProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_journal"));
	success = false;
      }

    if (logProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_log"));
	success = false;
      }

    if (htmlProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_html"));
	success = false;
      }

    if (serverHostProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_server_host"));
	success = false;
      }

    if (rootname == null)
      {
	System.err.println(ts.l("loadProperties.no_root_name"));
	success = false;
      }

    // we don't care if we don't get the defaultrootpassProperty,
    // since we don't require the user to keep their password in the
    // server's properties file unless they want to reset it

    if (mailHostProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_mail_host"));
	success = false;
      }

    if (returnaddrProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_email_addr"));
	success = false;
      }

    // if the subjectPrefixProperty is not defined or if it does not begin and
    // end with single quote marks, use a default prefix

    if ((subjectPrefixProperty == null) ||
	(subjectPrefixProperty.charAt(0) != '\'' ||
	 subjectPrefixProperty.charAt(subjectPrefixProperty.length()-1) != '\''))
      {
	subjectPrefixProperty = "Ganymede: ";
      }
    else
      {
	subjectPrefixProperty = subjectPrefixProperty.substring(1, subjectPrefixProperty.length()-1);
      }

    if (signatureFileProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_sig"));
	success = false;
      }

    if (helpbaseProperty == null || helpbaseProperty.equals(""))
      {
	System.err.println(ts.l("loadProperties.no_help_base"));
	helpbaseProperty = null;
      }

    if (monitornameProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_monitor_name"));
	success = false;
      }

    if (defaultmonitorpassProperty == null)
      {
	System.err.println(ts.l("loadProperties.no_monitor_pass"));
      }

    // get the registry port number

    String registryPort = System.getProperty("ganymede.registryPort");

    if (registryPort != null)
      {
	try
	  {
	    registryPortProperty = java.lang.Integer.parseInt(registryPort);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println(ts.l("loadProperties.no_registry_port", registryPort));
	  }
      }

    // if the main ganymede.properties file has a
    // ganymede.schemaDirectory property, load in the properties from
    // the schema's properties file.

    if (schemaDirectoryProperty != null && !schemaDirectoryProperty.equals(""))
      {
	try
	  {
	    String propName = arlut.csd.Util.PathComplete.completePath(schemaDirectoryProperty) + 
	      "schema.properties";

	    System.err.println(ts.l("loadProperties.reading_schema_props", propName));

	    fis = new FileInputStream(propName);
	    bis = new BufferedInputStream(fis);
	    props.load(bis);
	  }
	catch (IOException ex)
	  {
	  }
	finally
	  {
	    // don't wait for GC to close the file descriptors

	    try
	      {
		if (bis != null)
		  {
		    bis.close();
		  }
	      }
	    catch (IOException ex)
	      {
	      }

	    try
	      {
		if (fis != null)
		  {
		    fis.close();
		  }
	      }
	    catch (IOException ex)
	      {
	      }
	  }

      }

    return success;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                        dumpTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a journal sync.  Issued by the 
 * {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler}.</p>
 */

class dumpTask implements Runnable {

  static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ddroid.server.dumptask");

  public dumpTask()
  {
  }

  public void run()
  {
    boolean started = false;
    boolean completed = false;
    boolean gotSemaphore = false;

    /* -- */

    try
      {
	if (Ganymede.db.journal.isClean())
	  {
	    Ganymede.debug(ts.l("deferring"));
	    return;
	  }

	try
	  {
	    String error = GanymedeServer.lSemaphore.increment(0);
	
	    if (error != null)
	      {
		Ganymede.debug(ts.l("semaphore_disabled", error));
		return;
	      }
	    else
	      {
		gotSemaphore = true;
	      }
	  }
	catch (InterruptedException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }

	started = true;
	Ganymede.debug(ts.l("running", Ganymede.db.journal.transactionsInJournal));

	try
	  {
	    Ganymede.db.dump(Ganymede.dbFilename, true, false);
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug(ts.l("dump_error", ex.getMessage()));
	  }

	completed = true;
      }
    finally
      {
	// we'll go through here if our task was stopped
	// note that the DBStore dump code will handle
	// thread death ok.

	if (started && !completed)
	  {
	    Ganymede.debug(ts.l("forced_stop"));
	  }

	if (gotSemaphore)
	  {
	    GanymedeServer.lSemaphore.decrement();
	  }

	Ganymede.debug(ts.l("completed"));
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                              dumpAndArchiveTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a journal sync.  Issued by the 
 * {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler}.</p>
 */

class dumpAndArchiveTask implements Runnable {

  public dumpAndArchiveTask()
  {
  }

  public void run()
  {
    boolean started = false;
    boolean completed = false;

    /* -- */

    try
      {
	try
	  {
	    String error = GanymedeServer.lSemaphore.increment(0);
	    
	    if (error != null)
	      {
		Ganymede.debug("Deferring dump/archive task - semaphore disabled: " + error);
		return;
	      }
	  }
	catch (InterruptedException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }

	started = true;
	Ganymede.debug("Running dump/archive task");

	try
	  {
	    Ganymede.db.dump(Ganymede.dbFilename, true, true);
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("dump/archive could not succeed.. IO error " + ex.getMessage());
	  }

	completed = true;
      }
    finally
      {
	// we'll go through here if our task was stopped
	// note that the DBStore dump code will handle
	// thread death ok.

	if (started && !completed)
	  {
	    Ganymede.debug("dumpAndArchiveTask forced to stop");
	  }

	GanymedeServer.lSemaphore.decrement();

	Ganymede.debug("Completed dump/archive task");
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                          gcTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a synchronous garbage collection run.  Issued
 * by the {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler}.</p>
 *
 * <p>I'm not sure that there is any point to having a synchronous garbage
 * collection task.. the idea was that we could schedule a full gc when
 * the server was likely not to be busy so as to keep things trim for when
 * the server was busy, but the main() entry point isn't yet scheduling this
 * for a particularly good time.</p>
 */

class gcTask implements Runnable {

  public gcTask()
  {
  }

  public void run()
  {
    Ganymede.debug("Running garbage collection task");
    System.gc();
    Ganymede.debug("Garbage collection task finished");
    GanymedeAdmin.updateMemState(Runtime.getRuntime().freeMemory(),
				 Runtime.getRuntime().totalMemory());
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                     timeOutTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a periodic sweep of idle users.  Registered with
 * the {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler} by
 * {@link arlut.csd.ddroid.server.Ganymede#registerTasks() registerTasks()}, to
 * run every minute.</p>
 */

class timeOutTask implements Runnable, silentTask {

  public timeOutTask()
  {
  }

  public void run()
  {
    Ganymede.server.clearIdleSessions();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                memoryStatusTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to update the memory status fields in the admin
 * console.  Registered with the {@link
 * arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler} by {@link
 * arlut.csd.ddroid.server.Ganymede#registerTasks() registerTasks()}, to
 * run every minute.</p> 
 */

class memoryStatusTask implements Runnable, silentTask {

  /**
   * <p>The debug flag in memoryStatusTask is used to control
   * whether memoryStatusTask will log memory usage to Ganymede's
   * standard error log.  This is useful for tracking memory usage patterns.</p>
   */

  static final boolean debug = true;

  /**
   * <p>The period value is used to set how often the memory
   * statistics will be logged to Ganymede's standard error
   * log.  This period value is counted in terms of the number
   * of runs of the memoryStatusTask.  By default, memoryStatusTask
   * is run once a minute from the Directory Droid scheduler, so the
   * period count is minutes.</p>
   */

  static final int period = 15;

  static int count = 0;

  // ---

  public memoryStatusTask()
  {
  }

  public void run()
  {
    Runtime rt = Runtime.getRuntime();

    /* -- */

    if (debug)
      {
	if (count == period)
	  {
	    count = 0;
	  }
	
	if (count == 0)
	  {
	    Ganymede.debug(">> [" + new Date() + "] memory status dump: " +
			   "in use = " + (rt.totalMemory() - rt.freeMemory()) +
			   ", free = " + rt.freeMemory() +
			   ", total = " + rt.totalMemory());
	  }
      }

    GanymedeAdmin.updateMemState(rt.freeMemory(), rt.totalMemory());

    if (debug)
      {
	count++;
      }
  }
}
