/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;
import csd.DBStore.*;

public class Ganymede {
  
  public static GanymedeServer server;
  public static DBStore db;
  public static GanymedeSchema schema;
  public static String dbFilename;
  public static String schemaFilename;
  public static final boolean debug = true;

  /* -- */

  public static void main(String argv[]) 
  {
    if (argv.length != 2)
      {
	System.out.println("Error: invalid command line parameters");
	System.out.println("Usage: java Ganymede <dbfile> <schemafile>");
	return;
      }
    else
      {
	dbFilename = argv[0];
	schemaFilename = argv[1];
      }

    debug("Creating DBStore structures");

    db = new DBStore();

    debug("Loading DBStore contents");
    
    db.load(dbFilename);

    debug("Creating Ganymede Schema");

    try
      {
	schema = new GanymedeSchema(schemaFilename);
      }
    catch (IOException ex)
      {
	debug("Ganymede Schema creation failed.");
      }

    debug("Initializing Security Manager");

    System.setSecurityManager(new RMISecurityManager());

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
