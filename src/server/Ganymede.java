/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.15 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

public class Ganymede {
  
  public static GanymedeServer server;
  public static GanymedeSession internalSession;
  public static DBStore db;
  public static String dbFilename;
  public static final boolean debug = true;
  public static Date startTime = new Date();
  public static String debugFilename = null;

  /* -- */

  public static void main(String argv[]) 
  {
    File dataFile;

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
	//	System.exit(0);
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

    if (debug)
      {
	debug("Setup and bound server object OK");
      }
  }

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

}
