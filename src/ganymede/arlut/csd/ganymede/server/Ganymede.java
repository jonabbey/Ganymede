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
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

package arlut.csd.ganymede.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.InvidPool;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Server;

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
 * <p>When started, the Ganymede server creates a 
 * {@link arlut.csd.ganymede.server.DBStore DBStore} object, which in turn
 * creates and loads a set of {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
 * objects, one for each type of object held in the ganymede.db file.  Each
 * DBObjectBase contains {@link arlut.csd.ganymede.server.DBObject DBObject}
 * objects which hold the {@link arlut.csd.ganymede.server.DBField DBField}'s
 * which ultimately hold the actual data from the database.</p>
 *
 * <p>The ganymede.db file may define a number of task classes that are to
 * be run by the server at defined times.  The server's main() method starts
 * a background {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}
 * thread to handle background tasks.</p>
 *
 * <p>When the database has been loaded from disk, the main() method
 * creates a {@link arlut.csd.ganymede.server.GanymedeServer GanymedeServer}
 * object.  GanymedeServer implements the {@link arlut.csd.ganymede.rmi.Server
 * Server} RMI remote interface, and is published in the RMI registry.</p>
 *
 * <p>Clients and admin consoles may then connect to the published GanymedeServer
 * object via RMI to establish a connection to the server.</p>
 *
 * <p>The GanymedeServer's {@link
 * arlut.csd.ganymede.rmi.Server#login(java.lang.String username,
 * java.lang.String password) login} method is used to create a {@link
 * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} object
 * to manage permissions and communications with an individual client.
 * The client communicates with the GanymedeSession object through the
 * {@link arlut.csd.ganymede.rmi.Session Session} RMI remote
 * interface.</p>
 *
 * <p>While the GanymedeServer's login method is used to handle client
 * connections, the GanymedeServer's {@link
 * arlut.csd.ganymede.rmi.Server#admin(java.lang.String username,
 * java.lang.String password) admin} method is used to create a {@link
 * arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} object to
 * handle the admin console's communications with the server.  The
 * admin console communicates with the GanymedeAdmin object through
 * the {@link arlut.csd.ganymede.rmi.adminSession adminSession} RMI
 * remote interface.</p>
 *
 * <p>Most of the server's database logic is handled by the DBStore
 * object and its related classes ({@link
 * arlut.csd.ganymede.server.DBObject DBObject}, {@link
 * arlut.csd.ganymede.server.DBEditSet DBEditSet}, {@link
 * arlut.csd.ganymede.server.DBNameSpace DBNameSpace}, and {@link
 * arlut.csd.ganymede.server.DBJournal DBJournal}).</p>
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
   * <p>If true, the DBLog class will not send out any email.</p>
   */
  
  public static boolean suppressEmail = false;

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
   * <p>This object is responsible for handling all of the RMI
   * exportation of objects in the server.  According to how it is
   * initialized, objects may or may not be exported over SSL, and may
   * or may not be exported on a fixed port.</p>
   */

  public static GanymedeRMIManager rmi = null;

  /**
   * <p>Once the server is started and able to accept RMI clients,
   * this field will hold the GanymedeServer object which clients
   * talk to in order to login to the server.</p>
   */

  public static GanymedeServer server;

  /**
   * <p>A number of operations in the Ganymede server require 'root' 
   * access to the database.  This GanymedeSession object is provided
   * for system database operations.</p>
   */

  public static GanymedeSession internalSession;

  /**
   * <p>The background task scheduler.</p>
   */

  public static GanymedeScheduler scheduler;

  /**
   * <p>The Jython debug console server.</p>
   */
  
  public static JythonServer jythonServer;

  /**
   *
   * The Ganymede object store.
   * 
   */

  public static DBStore db;

  /**
   * <p>This object provides access to the Ganymede log file, providing
   * logging, email, and search services.</p>
   */

  public static DBLog log = null;

  /**
   * <p>A cached reference to a master category tree serialization
   * object.  Initialized the first time a user logs on to the server,
   * and re-initialized when the schema is edited.  This object is
   * provided to clients when they call 
   * {@link arlut.csd.ganymede.server.GanymedeSession#getCategoryTree() GanymedeSession.getCategoryTree()}.</p>
   */

  public static CategoryTransport catTransport = null;

  /**
   * <p>A cached reference to a master base list serialization object.
   * Initialized on server start up and re-initialized when the schema
   * is edited.  This object is provided to clients when they call
   * {@link arlut.csd.ganymede.server.GanymedeSession#getBaseList() GanymedeSession.getBaseList()}.</p>
   */

  public static BaseListTransport baseTransport = null;

  /**
   * <p>A vector of {@link arlut.csd.ganymede.server.GanymedeBuilderTask GanymedeBuilderTask}
   * objects initialized on database load.</p>
   */

  public static Vector builderTasks = new Vector();

  /**
   * <p>A vector of {@link arlut.csd.ganymede.server.SyncRunner SyncRunner}
   * objects initialized on database load.</p>
   */

  public static Vector syncRunners = new Vector();

  /**
   * The Thread that we have registered to handle cleanup if we get a
   * kill/quit signal.
   *
   * This is public so that the GanymedeServer class can de-register
   * this thread at shutdown time to avoid recursion on exit().
   */

  public static Thread signalHandlingThread = null;

  // properties from the ganymede.properties file
  
  public static String dbFilename = null;
  public static String journalProperty = null;
  public static String logProperty = null;
  public static String mailLogProperty = null;
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
  public static int    publishedObjectPortProperty = 55555;
  public static String logHelperProperty = null;
  public static boolean softtimeout = false;
  public static int timeoutIdleNoObjs = 15;
  public static int timeoutIdleWithObjs = 20;

  /**
   * <p>If the ganymede.bugaddress property is set, that string will
   * be copied into this variable.  It should be an email address to
   * send bug traces encountered by the Ganymede system.</p>
   *
   * <p>This system is used primarily by the client at this writing,
   * through the {@link arlut.csd.ganymede.server.GanymedeSession#reportClientBug(java.lang.String, java.lang.String)}
   * method.</p>
   *
   * <p>If this property string is null or empty, client bugs will not
   * be emailed, but they will be written to the server's standard
   * error stream.</p>
   */

  public static String bugReportAddressProperty = null;

  /**
   * If the Ganymede server is started with the -magic_import command
   * line flag, this field will be set to true and the server will
   * allow invids, creation timestamps, creator info, last
   * modification timestamps and last modification information to be
   * injected into objects loaded from the xmlclient.
   */

  public static boolean allowMagicImport = false;

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
   * the {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}
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
   * <p>This flag is set true unless the server was started with a
   * -nossl command line argument.  Unless -nossl is provided, the
   * server will force all client-server communications to be
   * encrypted using SSL and the certificate material generated by
   * the Ganymede build process's genkeys ant target.</p>
   */

  private static boolean useSSL = true;

  /**
   * <p>TranslationService object for handling string localization in the Ganymede
   * server.</p>
   */

  public static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.Ganymede");

  /**
   * Default localized "Ok" string, usable by server-side Ganymede code which prepares
   * dialogs.
   */

  public final static String OK = ts.l("global.ok");

  /**
   * Default localized "Cancel" string, usable by server-side Ganymede code which prepares
   * dialogs.
   */

  public final static String CANCEL = ts.l("global.cancel");

  /**
   * <p>This upgradeClassMap is used to rewrite the standard task
   * class names if we're loading an older (pre-DBStore 2.7 format)
   * ganymede.db file at task registration time.</p>
   */

  private static Hashtable upgradeClassMap = null;

  /* -- */

  /**
   *
   * The Ganymede server start point.
   *
   */

  public static void main(String argv[]) 
  {
    File dataFile;
    String propFilename = null;
    String useDirectory = null;

    /* -- */

    // If the "usedirectory" option is set, then use the supplied
    // directory name as the base path to the properties file (which
    // is assumed to be "ganymede.properties" and the debug log
    // (assumed to be debug.log).

    useDirectory = ParseArgs.getArg("usedirectory", argv);

    if (useDirectory != null)
      {
        String fileSeparator = System.getProperty("file.separator");
        
	propFilename = useDirectory + fileSeparator + "ganymede.properties";
        
	if (ParseArgs.switchExists("logrmi", argv))
	  {
	    debugFilename = useDirectory + fileSeparator + "debug.log";
	  }
      }

    if (ParseArgs.getArg("properties", argv) != null)
      {
	propFilename = ParseArgs.getArg("properties", argv);
      }
    
    if (ParseArgs.getArg("logrmi", argv) != null)
      {
	debugFilename = ParseArgs.getArg("logrmi", argv);
      }

    if (propFilename == null)
      {
        System.err.println(ts.l("main.cmd_line_error"));
        System.err.println(ts.l("main.cmd_line_usage"));
        return;
      }

    if (ParseArgs.switchExists("nossl", argv))
      {
	useSSL = false;

	// "***\n*** SSL disabled by use of -nossl command line switch ***\n***"
	System.err.println(ts.l("main.nossl"));
      }
    else
      {
	// "SSL enabled"
	System.err.println(ts.l("main.ssl"));
      }

    resetadmin = ParseArgs.switchExists("resetadmin", argv);

    allowMagicImport = ParseArgs.switchExists("magic_import", argv);

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
    
    // Create our GanymedeRMIManager to handle RMI exports.  We need
    // to create this before creating our DBStore, as some of the
    // components of DBStore are to be made accessible through RMI

    Ganymede.rmi = new GanymedeRMIManager(publishedObjectPortProperty, useSSL);

   /* Start up the RMI registry thread. */
    try
      {
        Ganymede.rmi.startRMIRegistry(registryPortProperty);
      }
    catch (RemoteException ex)
      {
        System.err.println(ts.l("main.error_starting_rmiregistry"));
        throw new RuntimeException(ex.getMessage());
      }    
    
    // if debug=<filename> was specified on the command line, tell the
    // RMI system to log RMI calls and exceptions that occur in
    // response to RMI calls.

    if (debugFilename != null)
      {
	// RMI Logging to {0}
	System.err.println(ts.l("main.info_rmilogging", debugFilename));

	try
	  {
	    RemoteServer.setLog(new FileOutputStream(debugFilename));
	  }
	catch (IOException ex)
	  {
	    System.err.println(ts.l("main.error_fail_debug", ex.toString()));
	  }
      }
    else
      {
	// Make RMI log any exceptions thrown in response to client calls
	// to stderr.. XXX not sure this should always be done if the debug
	// file is not specified on the command line XXX

	System.getProperties().setProperty("sun.rmi.server.exceptionTrace", "true");
      }

    // register our default UncaughtExceptionHandler

    setupUncaughtExceptionHandler();

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

	// but first, let's make sure there is no journal left alone
	// without a ganymede.db file..

	dataFile = new File(Ganymede.journalProperty);

	if (dataFile.exists())
	  {
	    // ***
	    // *** Error, I found an orphan journal ({0}), but no matching database file ({1}) to go with it.
	    // ***
	    // *** You need either to restore the {1} file, or to remove the {0} file.
	    // ***
	    debug(ts.l("main.orphan_journal", Ganymede.journalProperty, dbFilename));
	    return;
	  }

	firstrun = true;

	Invid.setAllocator(new InvidPool());

	// "No DBStore exists under filename {0}, not loading"
	debug(ts.l("main.info_new_dbstore", dbFilename));

	// "Initializing new schema"
	debug(ts.l("main.info_initializing_schema"));

	db.initializeSchema();

	// "Template schema created."
	debug(ts.l("main.info_created_schema"));

	try 
	  {
	    db.journal = new DBJournal(db, Ganymede.journalProperty);
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?

	    // "couldn''t initialize journal"
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
    // arlut.csd.ganymede.client.ClientBase), we just don't bother
    // with it.

    if (false)
      {
	// Initializing Security Manager
	debug(ts.l("main.info_init_security"));

	System.setSecurityManager(new RMISecurityManager());
      }
    else
      {
	// Not Initializing RMI Security Manager.. not supporting classfile transfer
	debug(ts.l("main.info_no_security"));
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
	debug(ts.l("main.error_fail_server") + Ganymede.stackTrace(ex));
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
    
    /* suppressEmail is the email "master switch". If it's set to true, 
     * then we won't actually send any emails out.
     */
     
    /* First, we check to see if there is a designated mail host. If not,
     * then we automatically turn off the sending of emails.
     */
     
    if (mailHostProperty == null || mailHostProperty.equals(""))
      {
      	suppressEmail = true;
      }
    else
      {
      	/* We have a valid mail host, but check to see if a command-line
      	 * argument has instructed us to suppress mailouts anyways.
      	 */
      
      	suppressEmail = ParseArgs.switchExists("suppressEmail", argv);
      }

    try
      {
	if (mailLogProperty != null && !mailLogProperty.equals(""))
	  {
	    log = new DBLog(new DBLogFileController(logProperty), 
			    new DBLogFileController(mailLogProperty), 
			    internalSession,
			    suppressEmail);
	  }
	else
	  {
	    log = new DBLog(new DBLogFileController(logProperty), 
			    null,
			    internalSession,
			    suppressEmail);
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

    // Create the background scheduler

    scheduler = new GanymedeScheduler(true);

    // set the background scheduler running on its own thread

    scheduler.start();

    // and install the tasks and sync channels listed in the database

    try
      {
	registerTasks();
	registerSyncChannels();

	// there is a very, very small chance that an abnormal
	// termination might have left some xml bits in the sync
	// channels for a transaction that ultimately could not be
	// processed.  We check for that here, and clean up any
	// remaining bits if we find them.

	DBJournalTransaction incompleteTransaction = db.journal.getIncompleteTransaction();

	if (incompleteTransaction != null)
	  {
	    for (int i = 0; i < syncRunners.size(); i++)
	      {
		SyncRunner channel = getSyncChannel((String) syncRunners.elementAt(i));

		try
		  {
		    channel.unSync(incompleteTransaction);
		  }
		catch (IOException ex)
		  {
		    // what can we do?  keep clearing them out as best we
		    // can

		    ex.printStackTrace();
		  }
	      }

	    db.journal.clearIncompleteTransaction();
	  }
      }
    catch (NotLoggedInException ex)
      {
	throw new Error(ts.l("main.error_myst_nologged") + ex.getMessage());
      }

    // take care of any startup-time database modifications

    try
      {
	startupHook();
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

	String hostname = null;

	if (!java.net.InetAddress.getLocalHost().getHostAddress().equals("127.0.0.1"))
	  {
	    hostname = java.net.InetAddress.getLocalHost().getHostName();
	  }
	else
	  {
	    // we don't want to bind to a system name that will
	    // resolve to 127.0.0.1, or else otherwise the rmiregistry
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

	Naming.bind("rmi://" + hostname + ":" + registryPortProperty + "/ganymede.server", 
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

    String portString = ParseArgs.getArg("telnet", argv);
    
    if (portString != null)
      {
	try
	  {
	    int portNumber = Integer.parseInt(portString);
	    
	    jythonServer = new JythonServer();
	    jythonServer.run(portNumber);
	  }
	catch (NumberFormatException ex)
	  {
	    debug(ts.l("main.badport", portString));
	  }
      }

    // register a thread to respond if the server gets ctrl-C, kill,
    // etc.

    Ganymede.signalHandlingThread = new Thread(new Runnable() {
        public void run() {
          try
            {
              GanymedeServer.shutdown();
            }
          finally
            {
              java.lang.Runtime.getRuntime().halt(1);
            }
        }
      }, ts.l("main.signalCatchThread"));  // "Ganymede ctrl-C handling thread"

    java.lang.Runtime.getRuntime().addShutdownHook(Ganymede.signalHandlingThread);
    
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

    GanymedeAdmin.logAppend(string);
  }

  /**
   * This is a convenience method used by server-side code to send
   * error logging output to stderr, to any attached admin consoles,
   * and to the registered bugReportAddressProperty.
   */

  static public void logError(Throwable ex)
  {
    ex.printStackTrace();

    GanymedeAdmin.logAppend(stackTrace(ex));
  }

  /**
   * This is a convenience method used by server-side code to send
   * error logging output to stderr, to any attached admin consoles,
   * and to the registered bugReportAddressProperty.
   */

  static public void logError(Throwable ex, String contextMsg)
  {
    System.err.println(contextMsg);

    ex.printStackTrace();

    GanymedeAdmin.logAppend(contextMsg + stackTrace(ex));
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
   * This is a convenience method used by the server to generate a
   * stack trace print from a server code.
   */

  static public void printCallStack()
  {
    try
      {
	throw new RuntimeException("TRACE");
      }
    catch (RuntimeException ex)
      {
	ex.printStackTrace();
      }
  }

  /**
   * <p>This is a convenience method to allow arbitrary Ganymede code
   * to generate a stack trace on the server's log and admin
   * consoles.</p>
   */

  static public void logAssert(String string)
  {
    try
      {
	throw new RuntimeException("ASSERT: " + string);
      }
    catch (RuntimeException ex)
      {
	Ganymede.debug(Ganymede.stackTrace(ex));
      }
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
				     Ganymede.OK,
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
				     Ganymede.OK,
				     null,
				     "error.gif"));

    if (logErrorDialogs)
      {
	System.err.println(ts.l("createErrorDialog.log_error", body));
      }

    return retVal;
  }

  /**
   * <p>This is a convenience method used by the server to return a
   * standard error dialog without a custom title.  This is primarily
   * useful for returning errors from {@link
   * arlut.csd.ganymede.server.GanymedeXMLSession} in which the errors
   * will be reported through a purely textual interface, but may be
   * used anywhere where a default title is acceptable.</p>
   */

  static public ReturnVal createErrorDialog(String body)
  {
    ReturnVal retVal = new ReturnVal(false);
    retVal.setDialog(new JDialogBuff(ts.l("createErrorDialog.default_title"),
				     body,
				     Ganymede.OK,
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
	p = (PasswordDBField) v_object.getField(SchemaConstants.PersonaPasswordField);

	if (p == null || !p.matchPlainText(Ganymede.defaultrootpassProperty))
	  {
	    System.out.println(ts.l("startupHook.resetting"));

	    internalSession.openTransaction("Ganymede startupHook");

	    e_object = (DBEditObject) internalSession.session.editDBObject(supergashinvid);

	    if (e_object == null)
	      {
		throw new RuntimeException(ts.l("startupHook.no_supergash", rootname));
	      }

	    p = (PasswordDBField) e_object.getField(SchemaConstants.PersonaPasswordField);
	    ReturnVal retVal = p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password
	    
	    if (!ReturnVal.didSucceed(retVal))
	      {
		throw new RuntimeException(ts.l("startupHook.failed_reset", rootname));
	      }

	    System.out.println(ts.l("startupHook.password_reset", rootname));

	    retVal = internalSession.commitTransaction();

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// if doNormalProcessing is true, the
		// transaction was not cleared, but was
		// left open for a re-try.  Abort it.
		
		if (retVal.doNormalProcessing)
		  {
		    internalSession.abortTransaction();
		  }
	      }
	  }
      }

    // At DBStore 2.11, we added a hidden label field for objectEvent
    // objects.  We'll edit any old ones here and fix up their labels

    if (true || Ganymede.db.isLessThan(2,11))
      {
	boolean success = true;
	Vector objects = internalSession.getObjects(SchemaConstants.ObjectEventBase);

	if (objects.size() > 0)
	  {
	    internalSession.openTransaction("Ganymede objectEvent fixup hook");

	    try
	      {
		for (int i = 0; i < objects.size(); i++)
		  {
		    DBObject object = (DBObject) objects.elementAt(i);

		    StringDBField labelField = (StringDBField) object.getField(SchemaConstants.ObjectEventLabel);

		    if (labelField == null)
		      {
			objectEventCustom objectEventObj = (objectEventCustom) internalSession.session.editDBObject(object.getInvid());

			if (objectEventObj != null)
			  {
			    ReturnVal retVal = objectEventObj.updateLabel((String) objectEventObj.getFieldValueLocal(SchemaConstants.ObjectEventObjectName),
									  (String) objectEventObj.getFieldValueLocal(SchemaConstants.ObjectEventToken));

			    if (!ReturnVal.didSucceed(retVal))
			      {
				success = false;
			      }
			  }
		      }
		  }

		if (success)
		  {
		    internalSession.commitTransaction();
		  }
		else
		  {
		    internalSession.abortTransaction();
		  }
	      }
	    catch (Throwable ex)
	      {
		internalSession.abortTransaction();
	      }
	  }
      }
  }


  /**
   * <p>At DBStore version 2.7, we changed the package name for our
   * built-in task classes.  This method creates a private Hashtable,
   * {@link arlut.csd.ganymede.server.Ganymede#upgradeClassMap}, which holds
   * a mapping of old class names to new ones for the built-in classes.</p>
   */

  static synchronized private void prepClassMap()
  {
    if (upgradeClassMap == null)
      {
        upgradeClassMap = new Hashtable();
        upgradeClassMap.put("arlut.csd.ganymede.dumpAndArchiveTask", "arlut.csd.ganymede.server.dumpAndArchiveTask");
        upgradeClassMap.put("arlut.csd.ganymede.dumpTask", "arlut.csd.ganymede.server.dumpTask");
        upgradeClassMap.put("arlut.csd.ganymede.GanymedeExpirationTask", "arlut.csd.ganymede.server.GanymedeExpirationTask");
        upgradeClassMap.put("arlut.csd.ganymede.GanymedeValidationTask", "arlut.csd.ganymede.server.GanymedeValidationTask");
        upgradeClassMap.put("arlut.csd.ganymede.GanymedeWarningTask", "arlut.csd.ganymede.server.GanymedeWarningTask");
      }
  }

  /**
   * This method scans the database for valid BuilderTask entries and 
   * adds them to the builderTasks vector.
   */

  static private void registerTasks() throws NotLoggedInException
  {
    Vector objects = internalSession.getObjects(SchemaConstants.TaskBase);
    DBObject object;

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

	    // At DBStore version 2.7, we changed the package name for
	    // our built-in task classes.  If loaded an older file and
	    // we recognize one of the built-in task class names, go
	    // ahead and rewrite it, hacking down into the
	    // StringDBField in a most naughty way.

	    if (Ganymede.db.isLessThan(2,7))
	      {
		prepClassMap();

		StringDBField taskClassStrF = (StringDBField) object.getField(SchemaConstants.TaskClass);

		if (taskClassStrF != null)
		  {
		    String taskClassStr = (String) taskClassStrF.value;

		    if (upgradeClassMap.containsKey(taskClassStr))
		      {
			String newClassName = (String) upgradeClassMap.get(taskClassStr);

			// "Rewriting old system task class {0} as {1}"
			System.err.println(ts.l("registerTasks.rewritingClass", taskClassStr, newClassName));

			// and here's where we force the value into place

			taskClassStrF.value = newClassName.intern();
		      }
		  }
	      }

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

    // register background memory status updater task

    scheduler.addPeriodicAction(new Date(System.currentTimeMillis() + 60000),
				1,
				new memoryStatusTask(),
				ts.l("registerTasks.memory_status_task"));

    // register garbage collection task without any schedule for
    // execution.. this is so that the admin can launch it from the
    // admin console

    scheduler.addActionOnDemand(new gcTask(),
				ts.l("registerTasks.gc_task"));

    // likewise the GanymedeValidationTask

    scheduler.addActionOnDemand(new GanymedeValidationTask(),
				ts.l("registerTasks.validation_task"));
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
   * This method schedules all registered builder tasks and Sync
   * Runners for execution.  This method will be called when a user
   * commits a transaction.  If a given task is already running, the
   * scheduler will make a note that it needs to be rescheduled on
   * completion.
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

    synchronized (syncRunners)
      {
	for (int i = 0; i < syncRunners.size(); i++)
	  {
	    scheduler.demandTask((String) syncRunners.elementAt(i));
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

    // XXX do we want to do something about forcing sync channels
    // here?
  }

  /**
   * This method scans the database for valid SyncChannel entries and
   * adds them to the syncRunners vector.
   */

  static private void registerSyncChannels() throws NotLoggedInException
  {
    Vector objects = internalSession.getObjects(SchemaConstants.SyncChannelBase);
    DBObject object;

    /* -- */

    if (objects != null)
      {
	if (objects.size() == 0)
	  {
	    System.err.println(ts.l("registerSyncChannels.no_syncs"));
	  }

	for (int i = 0; i < objects.size(); i++)
	  {
	    object = (DBObject) objects.elementAt(i);
	    SyncRunner runner = new SyncRunner(object);

	    if (debug)
	      {
		System.err.println(ts.l("registerSyncChannels.processing_sync", runner.toString()));
	      }

	    registerSyncChannel(runner);
	  }
      }
    else
      {
	System.err.println(ts.l("registerSyncChannels.no_syncs"));
      }
  }

  /**
   * <p>This method links the given SyncRunner object into the list of
   * registered Sync Channels used at transaction commit time, and
   * makes it available for the scheduler to run.</p>
   */

  static void registerSyncChannel(SyncRunner channel)
  {
    if (debug)
      {
	System.err.println(ts.l("registerSyncChannel.debug_register", channel.getName()));
      }

    synchronized (syncRunners)
      {
	Ganymede.scheduler.addActionOnDemand(channel, channel.getName());
	arlut.csd.Util.VectorUtils.unionAdd(syncRunners, channel.getName());
      }
  }

  /**
   * <p>This method unlinks the named SyncRunner object from the list
   * of registered Sync Channels used at transaction commit time, and
   * removes it from the Ganymede scheduler.</p>
   */

  static void unregisterSyncChannel(String channelName)
  {
    if (debug)
      {
	System.err.println(ts.l("unregisterSyncChannel.debug_unregister", channelName));
      }

    synchronized (syncRunners)
      {
	Ganymede.scheduler.unregisterTask(channelName);
	syncRunners.removeElement(channelName);
      }
  }

  /**
   * <p>This method returns a reference to a SyncRunner registered in
   * the Ganymede scheduler.  It does not remove the SyncRunner from
   * the scheduler, nor do anything other than return a reference to
   * it.</p>
   *
   * <p>In particular, it is not guaranteed that the SyncRunner
   * returned is not currently running.</p>
   */

  static SyncRunner getSyncChannel(String channelName)
  {
    return (SyncRunner) Ganymede.scheduler.getTask(channelName);
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
	GanymedeServer.sendMessageToRemoteSessions(ClientMessage.BUILDSTATUS, "building");
      }
    else if (p2 > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(ClientMessage.BUILDSTATUS, "building2");
      }
    else
      {
	GanymedeServer.sendMessageToRemoteSessions(ClientMessage.BUILDSTATUS, "idle");
      }
  }

  /**
   * Sets up our default UncaughtExceptionHandler, if we're running
   * on Java 1.5 or later.
   */

  private static void setupUncaughtExceptionHandler()
  {
    // we do this via Reflection so that this class won't have a
    // static dependency on a class which can only be compiled/loaded
    // on a system running Java 5.
    
    try
      {
	Class handlerClass = Class.forName("arlut.csd.ganymede.server.GanymedeUncaughtExceptionHandler");
	Method setupMethod = handlerClass.getMethod("setup", handlerClass);
	setupMethod.invoke(null);

	System.err.println("GanymedeUncaughtExceptionHandler initialized");
      }
    catch (ClassNotFoundException ex)
      {
	System.err.println("GanymedeUncaughtExceptionHandler not present");
	return;
      }
    catch (LinkageError ex)
      {
	System.err.println("GanymedeUncaughtExceptionHandler not supported");
	return;
      }
    catch (IllegalAccessException ex)
      {
	System.err.println("IllegalAccessException loading GanymedeUncaughtExceptionHandler");
	return;
      }
    catch (InvocationTargetException ex)
      {
	System.err.println("InvocationTargetException loading GanymedeUncaughtExceptionHandler");
	return;
      }
    catch (NoSuchMethodException ex)
      {
	System.err.println("NoSuchMethodException loading GanymedeUncaughtExceptionHandler");
	return;
      }
    catch (SecurityException ex)
      {
	System.err.println("SecurityException loading GanymedeUncaughtExceptionHandler");
	return;
      }
  }

  /**
   * <p>This method loads properties from the ganymede.properties
   * file.</p>
   *
   * <p>This method is public so that loader code linked with the
   * Ganymede server code can initialize the properties without
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
    bugReportAddressProperty = System.getProperty("ganymede.bugsaddress");

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

    if (mailHostProperty == null ||
        mailHostProperty.equals(""))
      {
	System.err.println(ts.l("loadProperties.no_mail_host"));
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

    // get the published object port number

    String publishedObjectPort = System.getProperty("ganymede.publishedObjectPort");

    if (publishedObjectPort != null)
      {
	try
	  {
	    publishedObjectPortProperty = java.lang.Integer.parseInt(publishedObjectPort);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println(ts.l("loadProperties.no_object_port", publishedObjectPort));
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
