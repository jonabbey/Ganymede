/*

   Ganymede.java

   Server main module

   This class is the main server module, providing the static main()
   method executed to start the server.

   This class is never instantiated, but instead provides a bunch of
   static variables and convenience methods in addition to the main()
   start method.

   Created: 17 January 1997
   Release: $Name:  $
   Version: $Revision: 1.75 $
   Last Mod Date: $Date: 1999/08/27 17:25:20 $
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

/** 
 * <p>This class is the main server module, providing the static
 * main() method executed to start the server.  This class is never
 * instantiated, but instead provides a bunch of static variables and
 * convenience methods in addition to the main() start method.</p>
 *
 * <p>When started, the Ganymede server creates a 
 * {@link arlut.csd.ganymede.DBStore DBStore} object, which in turn
 * creates and loads a set of {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
 * objects, one for each type of object held in the ganymede.db file.  Each
 * DBObjectBase contains {@link arlut.csd.ganymede.DBObject DBObject}
 * objects which hold the {@link arlut.csd.ganymede.DBField DBField}'s
 * which ultimately hold the actual data from the database.</p>
 *
 * <p>The ganymede.db file may define a number of task classes that are to
 * be run by the server at defined times.  The server's main() method starts
 * a background {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}
 * thread to handle background tasks.</p>
 *
 * <p>When the database has been loaded from disk, the main() method
 * creates a {@link arlut.csd.ganymede.GanymedeServer GanymedeServer}
 * object.  GanymedeServer implements the {@link arlut.csd.ganymede.Server
 * Server} RMI remote interface, and is published in the RMI registry.</p>
 *
 * <p>Clients and admin consoles may then connect to the published GanymedeServer
 * object via RMI to establish a connection to the server.</p>
 *
 * <p>The GanymedeServer's {@link arlut.csd.ganymede.GanymedeServer#login(arlut.csd.ganymede.Client) login}
 * method is used to create a {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}
 * object to manage permissions and communications with an individual client.  The
 * client communicates with the GanymedeSession object through the 
 * {@link arlut.csd.ganymede.Session Session} RMI remote interface.</p>
 *
 * <p>While the GanymedeServer's login method is used to handle client connections,
 * the GanymedeServer's
 * {@link arlut.csd.ganymede.GanymedeServer#admin(arlut.csd.ganymede.Admin) admin}
 * method is used to create a {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin} object
 * to handle the admin console's communications with the server.  The admin
 * console communicates with the GanymedeAdmin object through the  
 * {@link arlut.csd.ganymede.adminSession adminSession} RMI remote interface.</p>
 *
 * <p>Most of the server's database logic is handled by the DBStore object
 * and its related classes ({@link arlut.csd.ganymede.DBObject DBObject},
 * {@link arlut.csd.ganymede.DBEditSet DBEditSet}, {@link arlut.csd.ganymede.DBNameSpace DBNameSpace},
 * and {@link arlut.csd.ganymede.DBJournal DBJournal}).</p>
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
   * <p>If true, the server's schema editing code will allow
   * the admin console's schema editor to change the definitions
   * of object and field types that the server depends on for
   * its operations.</p>
   */

  public static boolean developSchema = false;

  /**
   * <p>If true, {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}
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
   * {@link arlut.csd.ganymede.GanymedeSession#getCategoryTree() GanymedeSession.getCategoryTree()}.</p>
   */

  public static CategoryTransport catTransport = null;

  /**
   * <p>A cached reference to a master base list serialization object.
   * Initialized on server start up and re-initialized when the schema
   * is edited.  This object is provided to clients when they call
   * {@link arlut.csd.ganymede.GanymedeSession#getBaseList() GanymedeSession.getBaseList()}.</p>
   */

  public static BaseListTransport baseTransport = null;

  /**
   * <p>A vector of {@link arlut.csd.ganymede.GanymedeBuilderTask GanymedeBuilderTask}
   * objects initialized on database load.</p>
   */

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
  public static String messageDirectoryProperty = null;
  public static String schemaDirectoryProperty = null;
  public static int    registryPortProperty = 1099;

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
   * the {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}
   * class will not worry about not finding the default permissions
   * role in the database.</p> 
   */

  public static boolean firstrun = false;

  /* -- */

  /**
   *
   * The Ganymede server start point.
   *
   */

  public static void main(String argv[]) 
  {
    File dataFile, logFile;
    String propFilename = null;

    /* -- */

    if (ParseArgs.switchExists("decode", argv))
      {
	String dbFilename = ParseArgs.getArg("dbfile", argv);

	if (dbFilename == null)
	  {
	    System.err.println("Error: missing dbfile parameter for dbfile decode.");
	    System.exit(1);
	  }
	else
	  {
	    //	    databaseReport(dbFilename, ParseArgs.switchExists("showall", argv),
	    //			   ParseArgs.switchExists("perms", argv));

	    databaseReport(dbFilename, ParseArgs.switchExists("showall", argv),
			   true);
	    System.exit(0);
	  }
      }

    if (ParseArgs.switchExists("permdecode", argv))
      {
	String dbFilename = ParseArgs.getArg("dbfile", argv);

	if (dbFilename == null)
	  {
	    System.err.println("Error: missing dbfile parameter for dbfile permdecode.");
	    System.exit(1);
	  }
	else
	  {
	    permReport(dbFilename);
	    System.exit(0);
	  }
      }

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.err.println("Error: invalid command line parameters");
 	System.err.print("Usage: java Ganymede [-decode dbfile=<dbfilename> [-showall]] [-resetadmin] ");
	System.err.print("[-developschema] ");
	System.err.println("properties=<property file> [debug=<rmi debug file>]");
	return;
      }

    debugFilename = ParseArgs.getArg("debug", argv);

    resetadmin = ParseArgs.switchExists("resetadmin", argv);

    developSchema = ParseArgs.switchExists("developschema", argv);

    if (developSchema)
      {
	System.out.println("Fundamental object types open for schema editing (-developschema)"); 
      }
    
    if (!loadProperties(propFilename))
      {
	System.out.println("Error, couldn't successfully load properties from file " + propFilename + ".");
	return;
      }
    else
      {
	System.out.println("Ganymede server: loaded properties successfully from " + propFilename);
      }

    // see whether our RMI registry currently has ganymede.server
    // bound.. doesn't matter much, but useful for logging

    boolean inUse = true;

    try
      {
	Naming.lookup("rmi://localhost:" + registryPortProperty + "/ganymede.server");
      }
    catch (NotBoundException ex)
      {
	inUse = false;		// this is what we want to have happen
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

    if (inUse)
      {
	System.err.println("Warning: Ganymede server already bound by other process / Naming failure.");
	System.err.println("(not likely fatal, the server probably restarted with an existing rmi registry)");
      }

    // create the database 

    debug("Creating DBStore structures");

    db = new DBStore();		// And how can this be!?  For he IS the kwizatch-haderach!!

    // load the database

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	debug("Loading DBStore contents");
	db.load(dbFilename, true);
      }
    else
      {
	// no database on disk.. create a new one, along with a new
	// journal

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

	// create the database objects required for the server's operation

	debug("Creating mandatory database objects");
	db.initializeObjects();
	debug("Mandatory database objects created.");

	firstrun = false;
      }

    // Java 2 makes it a real pain to change out security
    // managers.. since we don't need to do classfile transfer from
    // the client (since all clients should use
    // arlut.csd.ganymede.client.ClientBase), we just don't bother
    // with it.

    if (false)
      {
	debug("Initializing Security Manager");

	System.setSecurityManager(new RMISecurityManager());
      }
    else
      {
	debug("Not Initializing RMI Security Manager.. not supporting classfile transfer");
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
	    System.err.println("couldn't open RMI debug log: " + ex);
	  }
      }

    // Create a GanymedeServer object to support the logging
    // code... the GanymedeServer's main purpose (to allow logins)
    // won't come into play until we bind the server object into the
    // RMI registry.

    try
      {
	debug("Creating GanymedeServer object");

	server = new GanymedeServer();
      }
    catch (Exception ex)
      {
	debug("Couldn't create GanymedeServer: " + ex);
	throw new RuntimeException(ex.getMessage());
      }

    // create the internal GanymedeSession that we use for system
    // database maintenance

    try
      {
	debug("Creating internal Ganymede Session");
	internalSession = new GanymedeSession();
	internalSession.enableWizards(false);
	internalSession.enableOversight(false);

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

    // log our restart

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

    // take care of any startup-time database modifications

    startupHook();

    // Create the background scheduler

    scheduler = new GanymedeScheduler(true);

    // set the background scheduler running on its own thread

    new Thread(scheduler).start();

    // and install the tasks listed in the database

    registerTasks();

    // Bind the GanymedeServer object in the RMI registry so clients
    // and admin consoles can connect to us.

    try
      {
	debug("Binding GanymedeServer in RMI Registry");

	Naming.rebind("rmi://localhost:" + registryPortProperty + "/ganymede.server", server);
      }
    catch (Exception ex)
      {
	debug("Couldn't establish server binding: " + ex);
	throw new RuntimeException(ex.getMessage());
      }

    // at this point clients can log in to the server.. the RMI system
    // will spawn threads as necessary to handle RMI client activity..
    // the main thread has nothing left to do and can go ahead and
    // terminate here.

    // wa-la

    if (debug)
      {
	debug("Setup and bound server object OK");
      }
  }

  /**
   * <p>This method is used to examine a Ganymede database file for the
   * purposes of doing a schema report on it.</p>
   */

  public static void databaseReport(String dbFilename, boolean showBuiltins,
				    boolean showPerms)
  {
    File dataFile;

    /* -- */

    remotelyAccessible = false;

    Ganymede.dbFilename = dbFilename;

    Ganymede.debug = false;

    db = new DBStore();

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	db.load(dbFilename, false, false); // don't try to load journal

	StringWriter outString = new StringWriter();
	PrintWriter out = new PrintWriter(outString);
	
	db.printCategoryTree(out, showBuiltins);

	System.out.println(outString);

	if (showPerms)
	  {
	    PermissionMatrixDBField ownedPerm = null;
	    PermissionMatrixDBField defaultPerm = null;

	    DBObjectBase base = db.getObjectBase((short) 2);

	    Enumeration objs = base.objectTable.elements();

	    while (objs.hasMoreElements())
	      {
		DBObject permObj = (DBObject) objs.nextElement();

		ownedPerm = (PermissionMatrixDBField) permObj.getField((short) 101);
		defaultPerm = (PermissionMatrixDBField) permObj.getField((short) 103);

		System.err.println("Role " + permObj + " -- owned bits --\n");
		ownedPerm.debugdump();
		System.err.println("Role " + permObj + " -- default bits --\n");
		defaultPerm.debugdump();
	      }
	  }

	return;
      }

    System.err.println("Couldn't load database " + dbFilename);
  }

  /**
   * <p>This method is used to examine a Ganymede database file for the
   * purposes of doing a schema report on it.</p>
   */

  public static void permReport(String dbFilename)
  {
    File dataFile;

    /* -- */

    remotelyAccessible = false;

    Ganymede.dbFilename = dbFilename;

    Ganymede.debug = false;

    db = new DBStore();

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	db.load(dbFilename, true, false);

	PermissionMatrixDBField ownedPerm = null;
	PermissionMatrixDBField defaultPerm = null;

	System.err.println("Decoding Role objects");

	DBObjectBase base = db.getObjectBase((short) 2);
	
	Enumeration objs = base.objectTable.elements();
	
	while (objs.hasMoreElements())
	  {
	    DBObject permObj = (DBObject) objs.nextElement();
	    
	    ownedPerm = (PermissionMatrixDBField) permObj.getField((short) 101);
	    defaultPerm = (PermissionMatrixDBField) permObj.getField((short) 103);
	    
	    System.err.println("Role " + permObj + " -- owned bits --\n");
	    ownedPerm.debugdump();
	    System.err.println("Role " + permObj + " -- default bits --\n");
	    defaultPerm.debugdump();
	  }

	return;
      }

    System.err.println("Couldn't load database " + dbFilename);
  }

  /**
   * <p>This method is used to initialize the Ganymede system when it is
   * being driven by a direct-linked loader main() entry point, as a
   * single process.  This method is not used when the server is started
   * up normally.</p>
   */

  public static GanymedeServer directInit(String dbFilename) 
  {
    File dataFile;

    /* -- */

    remotelyAccessible = false;

    Ganymede.dbFilename = dbFilename;

    debug("Creating DBStore structures");

    db = new DBStore();

    dataFile = new File(dbFilename);
    
    if (dataFile.exists())
      {
	debug("Loading DBStore contents");
	db.load(dbFilename, true);
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

	server = new GanymedeServer();

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
	internalSession.enableWizards(false);
	internalSession.enableOversight(false);
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

  /**
   *
   * This is a convenience method used by server-side code to send
   * debug output to stdout and to any attached admin consoles.
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
   * standard error dialog.
   */

  static public ReturnVal createErrorDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(false);
    retVal.setDialog(new JDialogBuff(title,
				     body,
				     "OK",
				     null,
				     "error.gif"));

    if (logErrorDialogs)
      {
	System.err.println("Ganymede.createErrorDialog(): dialog says " + body);
      }

    return retVal;
  }

  /**
   * <p>This method is provided to allow us to hook in creation of new
   * objects with specified invid's that the server code references.</p>
   *
   * <p>It's intended for use during server development as we evolve
   * the schema.</p>
   */

  static public void startupHook()
  {
    Invid supergashinvid = new Invid(SchemaConstants.PersonaBase,
				     SchemaConstants.PersonaSupergashObj);
    DBObject v_object;
    DBEditObject e_object;

    PasswordDBField p;

    Invid defaultInv;
    StringDBField s;
    PermissionMatrixDBField pm;
    
    /* -- */

    if (resetadmin && Ganymede.defaultrootpassProperty != null)
      {
	// check to see if we need to reset the password to match our
	// properties file

	v_object = internalSession.session.viewDBObject(supergashinvid);
	p = (PasswordDBField) v_object.getField("Password");

	if (!p.matchPlainText(Ganymede.defaultrootpassProperty))
	  {
	    System.out.println("Resetting supergash password.");

	    internalSession.openTransaction("Ganymede startupHook");

	    e_object = (DBEditObject) internalSession.session.editDBObject(supergashinvid);

	    if (e_object == null)
	      {
		throw new RuntimeException("Error!  Couldn't pull " + rootname + " object");
	      }

	    p = (PasswordDBField) e_object.getField("Password");
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
      }

    // this bit was used to evolve the constant schema portions at
    // one point.. of strictly historical interest now

    if (false)
      {
	// manually insert the root (supergash) admin object

	internalSession.openTransaction("Ganymede startupHook");

	defaultInv = new Invid(SchemaConstants.RoleBase,
			       SchemaConstants.RoleDefaultObj);

	if (internalSession.session.viewDBObject(defaultInv) == null)
	  {
	    System.err.println("Creating the RoleDefaultObj");

	    // need to create the self perm object

	    // create SchemaConstants.RoleDefaultObj

	    ReturnVal retVal = internalSession.session.createDBObject(SchemaConstants.RoleBase, 
							       defaultInv,
							       null);

	    if (retVal != null && retVal.didSucceed())
	      {
		e_object = (DBEditObject) retVal.getObject();

		s = (StringDBField) e_object.getField(SchemaConstants.RoleName);
		s.setValueLocal("Default Permissions");
		
		// By default, users will be able to view themselves and all their fields, anything
		// else will have to be manually configured by the supergash administrator.
		
		pm = (PermissionMatrixDBField) e_object.getField(SchemaConstants.RoleMatrix);
		pm.setPerm(SchemaConstants.UserBase, new PermEntry(true, false, false, false)); 
		
		// By default, users will not be able to view, create, or edit anything.  The supergash
		// administrator is free to reconfigure this.
		
		pm = (PermissionMatrixDBField) e_object.getField(SchemaConstants.RoleDefaultMatrix);
		pm.setPerm(SchemaConstants.UserBase, new PermEntry(false, false, false, false)); 
	      }
	  }
	else
	  {
	    System.err.println("Not Creating the RoleDefaultObj");
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
   * This method scans the database for valid BuilderTask entries and 
   * adds them to the builderTasks vector.
   */

  static private void registerTasks()
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
	    System.err.println("** Empty list of builder tasks found in database!");
	  }

	for (int i = 0; i < objects.size(); i++)
	  {
	    object = (DBObject) objects.elementAt(i);

	    if (debug)
	      {
		System.err.println("Processing task object for " + object);
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
	System.err.println("** No tasks found in database!");
      }
  }

  static void registerBuilderTask(String taskName)
  {
    if (debug)
      {
	System.err.println("Registering " + taskName + " for execution on transaction commit.");
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
	System.err.println("Unregistering " + taskName + " for execution on transaction commit.");
      }

    synchronized (builderTasks)
      {
	builderTasks.removeElement(taskName);
      }
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
    messageDirectoryProperty = System.getProperty("ganymede.messageDirectory");
    schemaDirectoryProperty = System.getProperty("ganymede.schemaDirectory");

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
	System.err.println("Couldn't get the log property");
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
	    System.err.println("Couldn't get a valid registry port number from ganymede properties file: " + 
			       registryPort);
	  }
      }

    // if the main ganymede.properties file has a
    // ganymede.schemaDirectory property, load in the properties from
    // the schema's properties file.

    if (schemaDirectoryProperty != null && !schemaDirectoryProperty.equals(""))
      {
	try
	  {
	    props.load(new BufferedInputStream(new FileInputStream(schemaDirectoryProperty)));
	  }
	catch (IOException ex)
	  {
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
 * {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}.</p>
 */

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

	if (Ganymede.db.schemaEditInProgress)
	  {
	    Ganymede.debug("Deferring dump task - schema being edited");
	    return;
	  }

	started = true;
	Ganymede.debug("Running dump task");

	try
	  {
	    Ganymede.db.dump(Ganymede.dbFilename, true, false);
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

/*------------------------------------------------------------------------------
                                                                           class
                                                              dumpAndArchiveTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a journal sync.  Issued by the 
 * {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}.</p>
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
	if (Ganymede.server.activeUsers.size() > 0)
	  {
	    Ganymede.debug("Deferring dump/archive task - users logged in");
	    return;
	  }

	if (Ganymede.db.schemaEditInProgress)
	  {
	    Ganymede.debug("Deferring dump/archive task - schema being edited");
	    return;
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

	Ganymede.debug("Completed dump/archive task");
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
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                          gcTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a synchronous garbage collection run.  Issued
 * by the {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}.</p>
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
   }
}
