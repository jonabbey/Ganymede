/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.8 $ %D%
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
  public static DBStore db;
  public static String dbFilename;
  public static final boolean debug = true;
  public static Date startTime = new Date();

  /* -- */

  public static void main(String argv[]) 
  {
    File dataFile;

    /* -- */

    if (argv.length != 1)
      {
	System.out.println("Error: invalid command line parameters");
	System.out.println("Usage: java Ganymede <dbfile>");
	return;
      }
    else
      {
	dbFilename = argv[0];
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
	System.out.println("Ganymede server already bound by other process / Naming failure.");
	System.exit(0);
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
	db.initialize();
	debug("Template schema created.");

	try 
	  {
	    db.journal = new DBJournal(db, "journal"); // need to parametrize filename
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

    // Create a Server object

    try
      {
	debug("Creating GanymedeServer object");

	server = new GanymedeServer(10);

	debug("Binding GanymedeServer in RMI Registry");

	Naming.bind("ganymede.server", server);
      }
    catch (Exception ex)
      {
	debug("Couldn't establish server binding: " + ex);
	return;
      }

    if (debug)
      {
	debug("Setup and bound server object OK");
      }
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
