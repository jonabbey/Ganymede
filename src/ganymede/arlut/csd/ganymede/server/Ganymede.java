/*
   Ganymede.java

   Server main module

   This class is the main server module, providing the static main()
   method executed to start the server.

   This class is never instantiated, but instead provides a bunch of
   static variables and convenience methods in addition to the main()
   start method.

   Created: 17 January 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.solinger.cracklib.Packer;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.InvidPool;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.scheduleHandle;
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
 * <p>When started, the Ganymede server creates a {@link
 * arlut.csd.ganymede.server.DBStore DBStore} object, which in turn
 * creates and loads a set of {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects, one
 * for each type of object held in the ganymede.db file.  Each
 * DBObjectBase contains {@link arlut.csd.ganymede.server.DBObject
 * DBObject} objects which hold the {@link
 * arlut.csd.ganymede.server.DBField DBField}'s which ultimately hold
 * the actual data from the database.</p>
 *
 * <p>The ganymede.db file may define a number of task classes that
 * are to be run by the server at defined times.  The server's main()
 * method starts a background {@link
 * arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}
 * thread to handle background tasks.</p>
 *
 * <p>When the database has been loaded from disk, the main() method
 * creates a {@link arlut.csd.ganymede.server.GanymedeServer GanymedeServer}
 * object.  GanymedeServer implements the {@link arlut.csd.ganymede.rmi.Server
 * Server} RMI remote interface, and is published in the RMI registry.</p>
 *
 * <p>Clients and admin consoles may then connect to the published
 * GanymedeServer object via RMI to establish a connection to the
 * server.</p>
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
 * <p>All client permissions and communications are handled by the
 * GanymedeSession class.</p>
 */

public class Ganymede {

  static public boolean debug = true;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final public TranslationService ts =
    TranslationService.getTranslationService("arlut.csd.ganymede.server.Ganymede");

  /**
   * <p>If true, Ganymede.createErrorDialog() will print the
   * content of error dialogs to the server's stderr.</p>
   */

  static public boolean logErrorDialogs = true;

  /**
   * <p>If true, Ganymede.createInfoDialog() will print the
   * content of info dialogs to the server's stderr.</p>
   */

  static public boolean logInfoDialogs = true;

  /**
   * <p>If true, the DBLog class will not send out any email.</p>
   */

  static public boolean suppressEmail = false;

  /**
   * <p>If started with this, start Jython server on this port</p>
   */
  static public String portString = null;

  /**
   * <p>We keep the server's start time for display in the
   * admin console.</p>
   */

  static public Date startTime = new Date();

  /**
   * <p>If the server is started with propFilename on
   * the command line, the properties will be loaded
   * from this file.</p>
   */

  static public String propFilename = null;

  /**
   * <p>If the server is started with debug=&lt;filename&gt; on
   * the command line, debugFilename will hold the name
   * of the file to write our RMI debug log to.</p>
   */

  static public String debugFilename = null;

  /**
   * <p>This object is responsible for handling all of the RMI
   * exportation of objects in the server.  According to how it is
   * initialized, objects may or may not be exported over SSL, and may
   * or may not be exported on a fixed port.</p>
   */

  static public GanymedeRMIManager rmi = null;

  /**
   * <p>Once the server is started and able to accept RMI clients,
   * this field will hold the GanymedeServer object which clients
   * talk to in order to login to the server.</p>
   */

  static public GanymedeServer server;

  /**
   * <p>A number of operations in the Ganymede server require 'root'
   * access to the database.  This GanymedeSession object is provided
   * for system database operations.</p>
   */

  static public GanymedeSession internalSession;

  /**
   * <p>The background task scheduler.</p>
   */

  static public GanymedeScheduler scheduler;

  /**
   * <p>The Jython debug console server, if the telnet option is
   * specified on the command line.</p>
   */

  static public JythonServer jythonServer;

  /**
   * <p>Random access password quality check dictionary.</p>
   */

  static public Packer crackLibPacker;

  /**
   * <p>The Ganymede object store.</p>
   */

  static public DBStore db;

  /**
   * <p>This object provides access to the Ganymede log file,
   * providing transaction logging, email, and search services.</p>
   */

  static public DBLog log = null;

  /**
   * <p>A cached reference to a master category tree serialization
   * object.  Initialized the first time a user logs on to the server,
   * and re-initialized when the schema is edited.  This object is
   * provided to clients when they call {@link
   * arlut.csd.ganymede.server.GanymedeSession#getCategoryTree()
   * GanymedeSession.getCategoryTree()}.</p>
   */

  static public CategoryTransport catTransport = null;

  /**
   * <p>A cached reference to a master base list serialization object.
   * Initialized on server start up and re-initialized when the schema
   * is edited.  This object is provided to clients when they call
   * {@link arlut.csd.ganymede.server.GanymedeSession#getBaseList()
   * GanymedeSession.getBaseList()}.</p>
   */

  static public BaseListTransport baseTransport = null;

  /**
   * <p>The Thread that we have registered to handle cleanup if we get a
   * kill/quit signal.</p>
   *
   * <p>This is public so that the GanymedeServer class can de-register
   * this thread at shutdown time to avoid recursion on exit().</p>
   */

  static public Thread signalHandlingThread = null;

  // properties from the ganymede.properties file with their default
  // values

  static public String dbFilename = null;
  static public String journalProperty = null;
  static private String logProperty = null;
  static private String mailLogProperty = null;
  static public String serverHostProperty = null;
  static public String rootname = null;
  static public String defaultrootpassProperty = null;
  static public String mailHostProperty = null;
  static public String defaultDomainProperty = null;
  static public String returnaddrProperty = null;
  static public String subjectPrefixProperty = null;
  static public String signatureFileProperty = null;
  static public String helpbaseProperty = null;
  static public String monitornameProperty = null;
  static public String defaultmonitorpassProperty = null;
  static public String messageDirectoryProperty = null;
  static private String schemaDirectoryProperty = null;
  static public int registryPortProperty = 1099;
  static private int publishedObjectPortProperty = 55555;
  static public String logHelperProperty = null;
  static public boolean softtimeout = false;
  static public int timeoutIdleNoObjs = 15;
  static public int timeoutIdleWithObjs = 20;
  static private boolean cracklibEnabled = false;
  static private String cracklibDirectoryProperty = null;

  /**
   * <p>If the ganymede.bugaddress property is set, that string will
   * be copied into this variable.  It should be an email address to
   * send bug traces encountered by the Ganymede system.</p>
   *
   * <p>This system is used primarily by the client at this writing,
   * through the {@link
   * arlut.csd.ganymede.server.GanymedeSession#reportClientBug(java.lang.String,
   * java.lang.String)} method.</p>
   *
   * <p>If this property string is null or empty, client bugs will not
   * be emailed, but they will be written to the server's standard
   * error stream.</p>
   */

  static public String bugReportAddressProperty = null;

  /**
   * <p>If the Ganymede server is started with the -magic_import command
   * line flag, this field will be set to true and the server will
   * allow invids, creation timestamps, creator info, last
   * modification timestamps and last modification information to be
   * injected into objects loaded from the xmlclient.</p>
   */

  static public boolean allowMagicImport = false;

  /**
   * <p>If the server is started with the -resetadmin command line flag,
   * this field will be set to true and the server's startupHook() will
   * reset the supergash password to that specified in the server's
   * ganymede.properties file.</p>
   */

  static public boolean resetadmin = false;

  /**
   * <p>This flag is true if the server was started with no
   * pre-existing ganymede.db file.  If true, the {@link
   * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} class
   * will not worry about not finding the default permissions role in
   * the database.</p>
   */

  static public boolean firstrun = false;

  /**
   * <p>This flag is true if the server was started with a -forcelocalhost
   * command line argument, which will allow the server to run even if
   * it will only be accessible to localhost.</p>
   */

  static public boolean forcelocalhost = false;

  /**
   * <p>This flag is set true unless the server was started with a
   * -nossl command line argument.  Unless -nossl is provided, the
   * server will force all client-server communications to be
   * encrypted using SSL and the certificate material generated by
   * the Ganymede build process's genkeys ant target.</p>
   */

  static private boolean useSSL = true;

  /**
   * <p>Default localized "Ok" string, usable by server-side Ganymede
   * code which prepares dialogs. </p>
   */

  public final static String OK = ts.l("global.ok");

  /**
   * <p>Default localized "Cancel" string, usable by server-side
   * Ganymede code which prepares dialogs. </p>
   */

  public final static String CANCEL = ts.l("global.cancel");

  /**
   * <p>This upgradeClassMap is used to rewrite the standard task
   * class names if we're loading an older (pre-DBStore 2.7 format)
   * ganymede.db file at task registration time.</p>
   */

  static private Hashtable upgradeClassMap = null;

  /**
   * <p>Uncaught exception handler in use on the Ganymede server.
   * Used to log exceptions and send details about them to the email
   * addresses in the bugReportAddressProperty, if defined.</p>
   */

  static private GanymedeUncaughtExceptionHandler defaultHandler = null;

  /* -- */

  /**
   * <p>The Ganymede server start point.</p>
   *
   * @param argv - command line arguments.
   */

  static public void main(String argv[])
  {
    File dataFile;

    /* -- */

    System.setProperty("java.awt.headless", "true");

    setupUncaughtExceptionHandler();

    try
      {
        checkArgs(argv);

        loadProperties(propFilename);

        // If we are going to use CrackLib, make sure our password
        // dictionary exists, creating it if necessary.

        initializeCrackLib();

        // Create our GanymedeRMIManager to handle RMI exports.  We need
        // to create this before creating our DBStore, as some of the
        // components of DBStore are to be made accessible through RMI

        startRMIManager();

        // "Creating DBStore structures"
        debug(ts.l("main.info_creating_dbstore"));

        db = new DBStore();     // And how can this be!?  For he IS the kwizatch-haderach!!

        // Load the database

        dataFile = new File(dbFilename);

        if (dataFile.exists())
          {
            // "Loading DBStore contents"
            debug(ts.l("main.info_loading_dbstore"));
            db.load(dbFilename);
          }
        else
          {
            // No database on disk.. create a new one, along with a new journal

            createNewDB();
          }

        createGanymedeServer();
        createGanymedeSession();
        startTransactionLog();
        startScheduler();

        // Take care of any startup-time database modifications

        startupHook();
        bindGanymedeRMI();

        // At this point clients can log in to the server.. the RMI system
        // will spawn threads as necessary to handle RMI client activity..
        // the main thread has nothing left to do and can go ahead and
        // terminate here.

        // "Setup and bound server object OK"

        debug(ts.l("main.info_setup_okay"));
        startJythonServer();
        registerKillHandler();

        // "Ganymede Server Ready."
        debug("\n--------------------------------------------------------------------------------");
        debug(ts.l("main.info_ready"));
        debug("--------------------------------------------------------------------------------\n");
      }
    catch (GanymedeStartupException ex)
      {
        if (Ganymede.server != null)
          {
            GanymedeServer.shutdown();
          }

        ex.process();
      }
  }

  /**
   * <p>Checks any command line arguments passed in.</p>
   *
   * @param argv - command line arguments.
   *
   * @throw GanymedeStartupException if a required parameter is missing.
   */

  static private void checkArgs(String argv[]) throws GanymedeStartupException
  {
    String useDirectory = null;

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
        // "Error: invalid command line parameters"
        System.err.println(ts.l("main.cmd_line_error"));

        // "Usage: java arlut.csd.ganymede.server.Ganymede\n properties = [-usedirectory=<server directory>|<property file>] [-resetadmin] [debug = <rmi debug file>]"
        System.err.println(ts.l("main.cmd_line_usage"));

        throw new GanymedeSilentStartupException();
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
    suppressEmail = ParseArgs.switchExists("suppressEmail", argv);
    portString = ParseArgs.getArg("telnet", argv);

    return;
  }

  /**
   * <p>This method loads properties from the ganymede.properties
   * file.</p>
   *
   * @throw GanymedeStartupException If all necessary properties could
   * not be loaded.
   */

  static public void loadProperties(String filename) throws GanymedeStartupException
  {
    Properties props = new Properties(System.getProperties());
    FileInputStream fis = null;
    BufferedInputStream bis = null;

    /* -- */

    // "Ganymede server: loading properties from {0}"
    System.out.println(ts.l("loadProperties.propload", filename));

    try
      {
        fis = new FileInputStream(filename);
        bis = new BufferedInputStream(fis);
        props.load(bis);
      }
    catch (IOException ex)
      {
        throw new GanymedeStartupException(ts.l("loadProperties.nopropfile", filename));
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

    // make the combined properties file accessible throughout our server code.

    System.setProperties(props);

    dbFilename = System.getProperty("ganymede.database");
    journalProperty = System.getProperty("ganymede.journal");
    logProperty = System.getProperty("ganymede.log");
    mailLogProperty = System.getProperty("ganymede.maillog");
    serverHostProperty = System.getProperty("ganymede.serverhost");
    rootname = System.getProperty("ganymede.rootname");
    defaultrootpassProperty = System.getProperty("ganymede.defaultrootpass");
    mailHostProperty = System.getProperty("ganymede.mailhost");
    defaultDomainProperty = System.getProperty("ganymede.defaultdomain");
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

    String cracklibEnabledString = System.getProperty("ganymede.usecracklib");

    if (cracklibEnabledString != null && cracklibEnabledString.equalsIgnoreCase("true"))
      {
        cracklibDirectoryProperty = System.getProperty("ganymede.cracklibDirectory");

        if (cracklibDirectoryProperty == null)
          {
            // "No ganymede.cracklibDirectory property specified, can''t enable cracklib processing."
            throw new GanymedeStartupException(ts.l("loadProperties.no_cracklib_dir"));
          }
        else
          {
            File cracklibDir = new File(cracklibDirectoryProperty);

            if (!cracklibDir.isDirectory() || !cracklibDir.canWrite() || !cracklibDir.canRead())
              {
                // "No usable directory matching the ganymede.cracklibDirectory property ({0}) exists,
                // can''t enable cracklib processing."
                throw new GanymedeStartupException(ts.l("loadProperties.bad_cracklib_dir", cracklibDirectoryProperty));
              }
            else
              {
                cracklibEnabled = true;
              }
          }
      }

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
            throw new GanymedeStartupException(ts.l("loadProperties.no_parse_timeoutIdleNoObjs", timeoutIdleNoObjsString));
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
            throw new GanymedeStartupException(ts.l("loadProperties.no_parse_timeoutIdleWithObjs",
                                                    timeoutIdleWithObjsString));
          }
      }

    if (dbFilename == null)
      {
        // "Couldn''t get the ganymede.database property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_db"));
      }

    if (journalProperty == null)
      {
        // "Couldn''t get the ganymede.journal property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_journal"));
      }

    if (logProperty == null)
      {
        // "Couldn''t get the ganymede.log property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_log"));
      }

    if (serverHostProperty == null)
      {
        // "Couldn''t get the ganymede.serverhost property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_server_host"));
      }

    if (rootname == null)
      {
        // "Couldn''t get the ganymede.rootname property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_root_name"));
      }

    // we don't care if we don't get the mailHostProperty, since
    // we don't require that the server be able to send out mail.

    if (mailHostProperty == null ||
        mailHostProperty.equals(""))
      {
        // "***\n*** Email Sending disabled by use of -suppressEmail command line switch or by lack of ganymede.mailhost property ***\n***"
        System.err.println(ts.l("loadProperties.no_mail_host"));

        mailHostProperty = null;
      }

    if (defaultDomainProperty == null ||
        defaultDomainProperty.equals(""))
      {
        // "No ganymede.defaultdomain property set, won''t be able to normalize user email addresses when sending change mail."
        System.err.println(ts.l("loadProperties.no_default_domain"));

        defaultDomainProperty = null;
      }

    if (returnaddrProperty == null)
      {
        // "Couldn''t get the ganymede.returnaddr return email address property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_email_addr"));
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
        // "Couldn''t get the ganymede.signaturefile property"
        throw new GanymedeStartupException(ts.l("loadProperties.no_sig"));
      }

    if (helpbaseProperty == null || helpbaseProperty.equals(""))
      {
        // "Couldn''t get the ganymede.helpbase property.. setting to null"
        throw new GanymedeStartupException(ts.l("loadProperties.no_help_base"));
      }

    if (monitornameProperty == null)
      {
        // "Couldn''t get the ganymede.monitorname property."
        throw new GanymedeStartupException(ts.l("loadProperties.no_monitor_name"));
      }

    if (defaultmonitorpassProperty == null)
      {
        // not a failure condition

        // "Couldn''t get the ganymede.defaultmonitorpassw property.. may have problems if initializing a new db"
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
            // "Couldn''t get a valid registry port number from the ganymede.registryPort property: {0}"
            throw new GanymedeStartupException(ts.l("loadProperties.no_registry_port", registryPort));
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
            // "Couldn''t get a valid published object port number from ganymede.publishedObjectPort property: {0}"
            throw new GanymedeStartupException(ts.l("loadProperties.no_object_port", publishedObjectPort));
          }
      }

    // if the main ganymede.properties file has a
    // schemaDirectory property, load in the properties from
    // the schema's properties file.

    if (schemaDirectoryProperty != null && !schemaDirectoryProperty.equals(""))
      {
        try
          {
            String propName = arlut.csd.Util.PathComplete.completePath(schemaDirectoryProperty) +
              "schema.properties";

            // "Attempting to read schema properties: {0}"
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
  }

  /**
   * <p>Make sure that we have our random access crack lib dictionary
   * available to us. </p>
   */

  static private void initializeCrackLib()
  {
    if (!cracklibEnabled)
      {
        // "CrackLib disabled, no internal password quality checking available."
        System.err.println(ts.l("main.cracklibDisabled"));
        return;
      }

    // "CrackLib enabled for password quality checking."
    System.err.println(ts.l("main.cracklibEnabled"));

    try
      {
        File crackData = new File(cracklibDirectoryProperty, "cracklib_dict.pwd");
        File crackIndex = new File(cracklibDirectoryProperty, "cracklib_dict.pwi");
        File crackHash = new File(cracklibDirectoryProperty, "cracklib_dict.hwm");
        String pathPrefix = cracklibDirectoryProperty + File.separator + "cracklib_dict";

        if (crackData.isFile() && crackData.canRead() &&
            crackIndex.isFile() && crackIndex.canRead() &&
            crackHash.isFile() && crackHash.canRead())
          {
            // "Loading crack lib dictionary from {0}."
            System.err.println(ts.l("initializeCrackLib.loading_dictionary", pathPrefix));
            crackLibPacker = new Packer(pathPrefix, "r");
          }
        else
          {
            // "Creating random access crack lib dictionary {0}."
            System.err.println(ts.l("initializeCrackLib.creating_dictionary", pathPrefix));
            Packer.make(PackageResources.getPackageResourceAsStream("words", org.solinger.cracklib.CrackLib.class),
                        pathPrefix,
                        false);

            // "Loading crack lib dictionary from {0}."
            System.err.println(ts.l("initializeCrackLib.loading_dictionary", pathPrefix));
            crackLibPacker = new Packer(pathPrefix, "r");
          }

        // "Loaded {0} words from crack lib dictionary.
        System.err.println(ts.l("initializeCrackLib.loaded_dictionary", crackLibPacker.size()));
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
        // "CrackLib disabled, no internal password quality checking available."
        System.err.println(ts.l("main.cracklibDisabled"));
      }
  }

  /**
   * <p>Create our GanymedeRMIManager to handle RMI exports.
   * and start it up. </p>
   */

  static private void startRMIManager()
  {
    // Create our GanymedeRMIManager to handle RMI exports.  We need
    // to create this before creating our DBStore, as some of the
    // components of DBStore are to be made accessible through RMI

    rmi = new GanymedeRMIManager(publishedObjectPortProperty, useSSL);

    // Start up the RMI registry thread.

    try
      {
        rmi.startRMIRegistry(registryPortProperty);
      }
    catch (RemoteException ex)
      {
        // "Error, couln''t start the RMI registry."
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
            // "Couldn''t open RMI debug log: {0}"
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
  }

  /**
   * <p>Sets up our default UncaughtExceptionHandler.</p>
   *
   * <p>This method introduces a dependency on Java 1.5 or later.</p>
   */

  static private void setupUncaughtExceptionHandler()
  {
    defaultHandler = new GanymedeUncaughtExceptionHandler();

    Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
  }

  /**
   *  <p>No database exists on disk.. create a new journal file
   *  but first, let's make sure there is no journal left alone
   *  without a db file.  Does not write out a db file here.</p>
   */

  static public void createNewDB() throws GanymedeStartupException
  {
    File journalFile = new File(journalProperty);

    if (journalFile.exists())
      {
        // ***
        // *** Error, I found an orphan journal ({0}), but no matching database file ({1}) to go with it.
        // ***
        // *** You need either to restore the {1} file, or to remove the {0} file.
        // ***
        throw new GanymedeSilentStartupException(ts.l("main.orphan_journal", journalProperty, dbFilename));
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
        db.journal = new DBJournal(db, journalProperty);
      }
    catch (IOException ex)
      {
        // what do we really want to do here?

        // "couldn''t initialize journal"
        throw new RuntimeException(ts.l("main.error_no_init_journal"));
      }

    // create the database objects required for the server's operation

    // "Creating mandatory database objects"
    debug(ts.l("main.info_creating_mandatory"));
    db.initializeObjects();

    // "Mandatory database objects created."
    debug(ts.l("main.info_created_mandatory"));

    firstrun = false;
  }

  /**
   * <p>Create a GanymedeServer object to support the logging
   * code... the GanymedeServer's main purpose (to allow logins)
   * won't come into play until we bind the server object into the
   * RMI registry.</p>
   */

  static private void createGanymedeServer() throws GanymedeStartupException
  {
    try
      {
        // "Creating GanymedeServer object"
        debug(ts.l("createGanymedeServer.info_creating_server"));
        server = new GanymedeServer();
      }
    catch (Exception ex)
      {
        // "Couldn''t create GanymedeServer: "
        throw new GanymedeStartupException(ts.l("createGanymedeServer.error_fail_server") + stackTrace(ex));
      }
  }

  /**
   * <p>Create the internal GanymedeSession that we use for system
   * database maintenance and general operations.</p>
   */

  static private void createGanymedeSession() throws GanymedeStartupException
  {
    try
      {
        // "Creating internal Ganymede Session"
        debug(ts.l("createGanymedeSession.info_creating_def_session"));

        internalSession = new GanymedeSession();
        internalSession.enableWizards(false);
        internalSession.enableOversight(false);

        // "Creating master BaseListTransport object"
        debug(ts.l("createGanymedeSession.info_creating_baselist_trans"));

        baseTransport = internalSession.getBaseList();
      }
    catch (Exception ex)
      {
        throw new GanymedeStartupException(ts.l("createGanymedeSession.error_fail_session") + ex);
      }
  }

  /**
   * Creates the DBLog and sets options on it.
   */

  static private void startTransactionLog() throws GanymedeStartupException
  {
    // First, we check to see if there is a designated mail host. If
    // not, then we automatically turn off the sending of emails.
    //
    // Likewise, suppressEmail is the email command line "master
    // switch". If it's set to true, then we won't actually send any
    // emails out.

    if (mailHostProperty == null || mailHostProperty.equals(""))
      {
        suppressEmail = true;
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

        // "Couldn''t initialize log file"
        throw new GanymedeStartupException(ts.l("main.error_log_file"));
      }

    // log our restart

    String startMesg;

    if (debugFilename != null)
      {
        // "Server startup - Debug mode"
        startMesg = ts.l("main.info_debug_start");
      }
    else
      {
        // "Server startup - Not in Debug mode"
        startMesg = ts.l("main.info_nodebug_start");
      }

    log.logSystemEvent(new DBLogEvent("restart",
                                      startMesg,
                                      null,
                                      null,
                                      null,
                                      null));
  }

  /**
   * <p>Creates a background scheduler, and registers the tasks and
   * sync channels for it.</p>
   */

  static private void startScheduler() throws GanymedeStartupException
  {
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
            for (scheduleHandle handle: scheduler.getTasksByClass(SyncRunner.class))
              {
                SyncRunner channel = (SyncRunner) handle.task;

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
        // "Mysterious not logged in exception: "
        throw new GanymedeStartupException(ts.l("main.error_myst_nologged") + ex.getMessage());
      }
    }

  /**
   * <p>This method is provided to allow us to hook in creation of new
   * objects with specified invid's that the server code references.</p>
   *
   * <p>It's intended for use during server development as we evolve
   * the schema.</p>
   */

  static private void startupHook()
  {
    // check to make sure the datastructures for supergash in the
    // db file are set.

    db.initializeObjects();

    try
      {
        // and reset the password if we need to.

        if (resetadmin && defaultrootpassProperty != null && !defaultrootpassProperty.trim().equals(""))
          {
            resetAdminPassword();
          }

        // At DBStore 2.11, we added a hidden label field for objectEvent
        // objects.  We'll edit any old ones here and fix up their labels

        if (db.isLessThan(2,11))
          {
            AddLabelUpdate();
          }
      }
    catch (NotLoggedInException ex)
      {
        // "Mysterious not logged in exception: "
        throw new Error(ts.l("main.error_myst_nologged") + ex.getMessage());
      }
  }

  /**
   * <p>Bind the GanymedeServer object in the RMI registry so clients
   * and admin consoles can connect to us.</p>
   */

  static public void bindGanymedeRMI() throws GanymedeStartupException
  {
    try
      {
        // "Binding GanymedeServer in RMI Registry"
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

            // try to use the name specified in our ganymede.properties File

            if (serverHostProperty != null && !serverHostProperty.equals(""))
              {
                hostname = serverHostProperty;

                if (java.net.InetAddress.getByName(hostname).getHostAddress().equals("127.0.0.1"))
                  {
                    // nope, give up

                    if (forcelocalhost)
                      {
                        debug("\n** " + ts.l("main.warning") + " **\n");
                      }
                    else
                      {
                        debug("\n** " + ts.l("main.error") + " **\n");
                      }

                    // "Both the system hostname ({0}) and the
                    // ganymede.serverhost definition ({1}) resolve to
                    // the 127.0.0.1 loopback address"
                    debug(ts.l("main.error_loopback",
                                        java.net.InetAddress.getLocalHost().getHostName(),
                                        serverHostProperty));
                    debug("\n");

                    // "The Ganymede server must have an externally
                    // accessible IP address or else clients \
                    // will not be able to communicate with the
                    // Ganymede server from other than localhost."
                    debug(ts.l("main.error_loopback_explain"));

                    if (!forcelocalhost)
                      {
                        // "If you really want to be only usable for
                        // localhost, edit the runServer script to use
                        // the -forcelocalhost option"
                        debug(ts.l("main.error_loopback_explain2"));

                        // "Shutting down."
                        debug("\n" + ts.l("main.info_shutting_down") + "\n");

                        throw new GanymedeSilentStartupException();
                      }
                  }
                else
                  {
                    // "Avoiding loopback {0} definition, binding to {1}"
                    debug(ts.l("main.info_avoiding_loopback",
                                        java.net.InetAddress.getLocalHost().getHostName(),
                                        hostname));
                  }
              }
          }

        // tell the RMI registry where to find the server

        Naming.bind("rmi://" + hostname + ":" + registryPortProperty + "/ganymede.server",
                      server);
      }
    catch (RuntimeException ex)
      {
        // We're catching RuntimeException separately to placate
        // FindBugs.
        // "Couldn''t establish server binding: "
        throw new GanymedeStartupException(ts.l("main.error_no_binding") + ex);
      }
    catch (Exception ex)
      {
        // "Couldn''t establish server binding: "
        throw new GanymedeStartupException(ts.l("main.error_no_binding") + ex);
      }
  }

  /**
   * <p> If we've been given a telnet port on the command line, set up a
   * Jython console interpreter that implementers can telnet to for debug.</p>
   */

  static private void startJythonServer()
  {
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
            // "Could not start telnet console, {0} is not a valid port number."
            debug(ts.l("main.badport", portString));
          }
      }
  }

  /**
   * <p>Register a thread to respond if the server hits ctrl-C on the
   * server's stdin or sends a SIGQUIT signal, etc.</p>
   */

  static private void registerKillHandler()
  {
    signalHandlingThread = new Thread(new Runnable() {
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

    java.lang.Runtime.getRuntime().addShutdownHook(signalHandlingThread);
  }

  /**
   * <p>This is a convenience method used by server-side code to send
   * debug output to stderr and to any attached admin consoles.</p>
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
   * <p>This is a convenience method used by server-side code to send
   * error logging output to stderr, to any attached admin consoles,
   * and to the registered bugReportAddressProperty.</p>
   */

  static public void logError(Throwable ex)
  {
    if (defaultHandler != null)
      {
        defaultHandler.reportException(ex);
      }
    else
      {
        ex.printStackTrace();
      }
  }

  /**
   * <p>This is a convenience method used by server-side code to send
   * error logging output to stderr, to any attached admin consoles,
   * and to the registered bugReportAddressProperty. </p>
   */

  static public void logError(Throwable ex, String contextMsg)
  {
    if (defaultHandler != null)
      {
        defaultHandler.reportException(contextMsg, ex);
      }
    else
      {
        System.err.println(contextMsg);

        ex.printStackTrace();
      }
  }

  /**
   * <p>This is a convenience method used by the server to get a
   * stack trace from a throwable object in String form. </p>
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
   * <p>This is a convenience method used by the server to generate a
   * stack trace print from a server code. </p>
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
        debug(stackTrace(ex));
      }
  }

  /**
   * <p>This is a convenience method used by the server to return a
   * standard informative dialog.</p>
   */

  static public ReturnVal createInfoDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(true,true); // success ok, doNormalProcessing ok
    retVal.setDialog(new JDialogBuff(title,
                                     body,
                                     OK,
                                     null,
                                     "ok.gif"));

    if (logInfoDialogs)
      {
        System.err.println(ts.l("createInfoDialog.log_info", body));
      }

    return retVal;
  }

  /**
   * <p>This is a convenience method used by the server to return a
   * standard error dialog.</p>
   */

  static public ReturnVal createErrorDialog(String title, String body)
  {
    ReturnVal retVal = new ReturnVal(false);
    retVal.setDialog(new JDialogBuff(title,
                                     body,
                                     OK,
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
                                     OK,
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
   * very standard error dialog.</p>
   *
   * <p>The Exception parameter is ignored for now, so that this method
   * can do something with it later if necessary without having to go
   * through all the code which calls this method.</p>
   */

  static public ReturnVal loginError(Exception ex)
  {
    return createErrorDialog(ts.l("loginError.error"),
                                      ts.l("loginError.explain"));
  }

  /**
   * <p>Reset the password to match our properties file because we have a
   * -resetadmin command line argument. </p>
   */

  static private void resetAdminPassword() throws NotLoggedInException
  {
    Invid supergashinvid = Invid.createInvid(SchemaConstants.PersonaBase,
                                             SchemaConstants.PersonaSupergashObj);
    DBObject v_object;
    DBEditObject e_object;
    PasswordDBField p;

    /* -- */

    v_object = DBStore.viewDBObject(supergashinvid);
    p = (PasswordDBField) v_object.getField(SchemaConstants.PersonaPasswordField);

    if (p == null || !p.matchPlainText(defaultrootpassProperty))
      {
        // "Resetting supergash password."
        System.err.println(ts.l("startupHook.resetting"));

        internalSession.openTransaction("Ganymede startupHook");

        e_object = (DBEditObject) internalSession.getDBSession().editDBObject(supergashinvid);

        if (e_object == null)
          {
            throw new RuntimeException(ts.l("startupHook.no_supergash", rootname));
          }

        p = (PasswordDBField) e_object.getField(SchemaConstants.PersonaPasswordField);
        ReturnVal retVal = p.setPlainTextPass(defaultrootpassProperty); // default supergash password

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

  /**
   * <p> At DBStore 2.11, we added a hidden label field for objectEvent
   * objects.  We'll edit any old ones here and fix up their labels.</p>
   */

  static private void AddLabelUpdate() throws NotLoggedInException
  {
    boolean success = true;
    List<DBObject> objects = internalSession.getDBSession().getTransactionalObjects(SchemaConstants.ObjectEventBase);

    if (objects.size() <= 0)
      {
        return;
      }

    internalSession.openTransaction("Ganymede objectEvent fixup hook");

    try
      {
        for (DBObject object: objects)
          {
            StringDBField labelField = (StringDBField) object.getField(SchemaConstants.ObjectEventLabel);

            if (labelField == null)
              {
                objectEventCustom objectEventObj = (objectEventCustom)
                  internalSession.getDBSession().editDBObject(object.getInvid());

                if (objectEventObj != null)
                  {
                    ReturnVal retVal = objectEventObj.updateLabel(
                                (String) objectEventObj.getFieldValueLocal(SchemaConstants.ObjectEventObjectName),
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
   * This method scans the database for valid task entries and adds
   * them to the scheduler.
   */

  static private void registerTasks() throws NotLoggedInException
  {
    List<DBObject> objects = internalSession.getDBSession().getTransactionalObjects(SchemaConstants.TaskBase);

    /* -- */

    if (objects != null)
      {
        if (objects.size() == 0)
          {
            System.err.println(ts.l("registerTasks.empty_builders"));
          }

        for (DBObject object: objects)
          {
            // At DBStore version 2.7, we changed the package name for
            // our built-in task classes.  If loaded an older file and
            // we recognize one of the built-in task class names, go
            // ahead and rewrite it, hacking down into the
            // StringDBField in a most naughty way.

            if (db.isLessThan(2,7))
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
    // execution.. this is so that the admin can launch it from theadmin console

    scheduler.addActionOnDemand(new gcTask(),
                                ts.l("registerTasks.gc_task"));

    // likewise the GanymedeValidationTask

    scheduler.addActionOnDemand(new GanymedeValidationTask(),
                                ts.l("registerTasks.validation_task"));
  }

  /**
   * <p>This method schedules all registered builder tasks and Sync
   * Runners for execution.  This method will be called when a user
   * commits a transaction.  If a given task is already running, the
   * scheduler will make a note that it needs to be rescheduled on
   * completion.</p>
   */

  static public void runBuilderTasks()
  {
    for (scheduleHandle handle: scheduler.getTasksByType(scheduleHandle.TaskType.BUILDER))
      {
        scheduler.demandTask(handle.getName());
      }

    for (scheduleHandle handle: scheduler.getTasksByClass(SyncRunner.class))
      {
        SyncRunner runner = (SyncRunner) handle.task;

        if (runner.isIncremental() || runner.isFullState())
          {
            scheduler.demandTask(handle.getName());
          }
      }
  }

  /**
   * <p>This method schedules all registered builder tasks for
   * execution, with an option set that will cause all builder tasks
   * to consider object bases as changed since the last build, thus
   * triggering a full external rebuild.</p>
   */

  static public void forceBuilderTasks()
  {
    String[] options = {"forcebuild"};

    for (scheduleHandle handle: scheduler.getTasksByType(scheduleHandle.TaskType.BUILDER))
      {
        scheduler.demandTask(handle.getName(), options);
      }

    // XXX do we want to do something about forcing sync channels
    // here?
  }

  /**
   * <p>This method scans the database for valid SyncChannel entries and
   * adds them to the scheduler.</p>
   */

  static private void registerSyncChannels() throws NotLoggedInException
  {
    List<DBObject> objects = internalSession.getDBSession().getTransactionalObjects(SchemaConstants.SyncChannelBase);

    /* -- */

    if (objects != null)
      {
        if (objects.size() == 0)
          {
            System.err.println(ts.l("registerSyncChannels.no_syncs"));
          }

        for (DBObject object: objects)
          {
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

  static public void registerSyncChannel(SyncRunner channel)
  {
    if (debug)
      {
        System.err.println(ts.l("registerSyncChannel.debug_register", channel.getName()));
      }

    scheduleHandle handle = scheduler.addActionOnDemand(channel, channel.getName());

    channel.setScheduleHandle(handle);
  }

  /**
   * <p>This method unlinks the named SyncRunner object from the list
   * of registered Sync Channels used at transaction commit time, and
   * removes it from the Ganymede scheduler.</p>
   */

  static public void unregisterSyncChannel(String channelName)
  {
    if (debug)
      {
        System.err.println(ts.l("unregisterSyncChannel.debug_unregister", channelName));
      }

    scheduler.unregisterTask(channelName);
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

  static public SyncRunner getSyncChannel(String channelName)
  {
    return (SyncRunner) scheduler.getTask(channelName);
  }

  /**
   * <p>This method is called by the GanymedeBuilderTask base class to
   * record that the server is processing a build.</p>
   */

  static public void updateBuildStatus()
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
   * <p>Static accessor to return a GanymedeSession with supergash
   * authority and both wizards and oversight turned off.</p>
   */

  static public GanymedeSession getInternalSession()
  {
    return Ganymede.internalSession;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                        GanymedeStartupException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific Exception that can be thrown during
 * the server's start up processing.</p>
 */

class GanymedeStartupException extends Exception {

  public GanymedeStartupException()
  {
  }

  public GanymedeStartupException(String mesg)
  {
    super(mesg);
  }

  /**
   * This method is called to handle error processing logic for this
   * GanymedeStartupException.
   */

  public void process()
  {
    this.printStackTrace();

    // "Shutting down."
    Ganymede.debug("\n" + Ganymede.ts.l("main.info_shutting_down") + "\n");

    System.exit(1);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                  GanymedeSilentStartupException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific Exception that can be thrown during
 * the server's start up processing, and which will cause the server
 * to terminate without displaying any message to the console.</p>
 */

class GanymedeSilentStartupException extends GanymedeStartupException {

  public GanymedeSilentStartupException()
  {
  }

  public GanymedeSilentStartupException(String mesg)
  {
    super(mesg);
  }

  /**
   * <p>This method is called to handle error processing logic for
   * this GanymedeSilentStartupException.</p>
   *
   * <p>If we have been given a message, we'll print that to stderr,
   * otherwise processing will be completely silent.</p>
   */

  public void process()
  {
    if (getMessage() != null)
      {
        System.err.println(getMessage());
      }

    System.exit(1);
  }
}
