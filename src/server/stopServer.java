/*
   GASH 2

   stopServer.java

   Command-line app to shut down a Ganymede server, with password info
   drawn from a properties file.

   Created: 28 April 1999
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/10/13 20:02:16 $
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

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.Util.ParseArgs;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      stopServer

------------------------------------------------------------------------------*/

/**
 * Command-line app to shut down a Ganymede server, with password info
 * drawn from a properties file.
 */

public class stopServer {

  public static String serverHostProperty = null;
  public static String rootname = null;
  public static String defaultrootpassProperty = null;
  public static int    registryPortProperty = 1099;

  public static void main(String argv[])
  {
    String propFilename = null;
    String server_url;
    Server server = null;
    stopServerAdmin admin = null;
    boolean waitForUsers = false;

    /* -- */

    // if the user invokes us with -delay, we'll do a deferred shutdown.

    waitForUsers = ParseArgs.switchExists("delay", argv);

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.err.println("Ganymede stopServer: Error, invalid command line parameters");
 	System.err.println("Usage: java stopServer properties=<property file>");
	return;
      }

    if (!loadProperties(propFilename))
      {
	System.out.println("Ganymede stopServer: Error, couldn't successfully load properties from file " + 
			   propFilename + ".");
	return;
      }

    server_url = "rmi://" + serverHostProperty + ":" + registryPortProperty + "/ganymede.server";

    try
      {
	Remote obj = Naming.lookup(server_url);

	if (obj instanceof Server)
	  {
	    server = (Server) obj;
	  }
      }
    catch (NotBoundException ex)
      {
	System.err.println("Ganymede stopServer: server url " + 
			   server_url + " not running, or could not connect\n" + ex );
	System.exit(1);
      }
    catch (java.rmi.UnknownHostException ex)
      {
	System.err.println("Ganymede stopServer: bad server URL " + server_url);
	System.exit(1);
      }
    catch (RemoteException ex)
      {
	System.err.println("Ganymede stopServer: couldn't connect to server object " + server_url);
	System.exit(1);
      }
    catch (java.net.MalformedURLException ex)
      {
	System.err.println("Ganymede stopServer: malformed server url " + server_url);
	System.exit(1);
      }

    System.out.println("Ganymede stopServer: Shutting down Ganymede server");

    try
      {
	admin = new stopServerAdmin(server, rootname, defaultrootpassProperty);
      }
    catch (RemoteException rx)
      {
	System.err.println("Ganymede stopServer: Error, server not up (?) " + rx);
	System.exit(1);
      }

    try
      {
	admin.shutdown(waitForUsers);
      }
    catch (RemoteException rx)
      {
	// just have to hope we did shut it down
      }

    if (!waitForUsers)
      {
	System.out.println("Ganymede stopServer: Ganymede server shutdown");
      }
    else
      {
	System.out.println("Ganymede stopServer: Ganymede server shutdown initiated\nServer will "
			   + "shut down when all users are logged out.");
      }

    System.exit(0);
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

    serverHostProperty = System.getProperty("ganymede.serverhost");
    rootname = System.getProperty("ganymede.rootname");
    defaultrootpassProperty = System.getProperty("ganymede.defaultrootpass");

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

    return success;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 stopServerAdmin

------------------------------------------------------------------------------*/

/**
 * <p>Remote object that the server expects to talk to when an admin console
 * connects to the server.</p>
 */

class stopServerAdmin extends UnicastRemoteObject implements Admin {

  private Server server = null;
  private adminSession aSession = null;
  private String adminName = null;
  private String adminPass = null;

  /* -- */

  public stopServerAdmin(Server server, String name, String pass) throws RemoteException
  {
    // UnicastRemoteServer can throw RemoteException 

    this.server = server;
    this.adminName = name;
    this.adminPass = pass;

    try
      {
	aSession = server.admin(this);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Ganymede stopServer: Error, couldn't log into server with admin privs.");
	System.exit(1);
      }
  }

  void shutdown(boolean waitForUsers) throws RemoteException
  {
    aSession.shutdown(waitForUsers);
  }

  public void disconnect() throws RemoteException
  {
    aSession.logout();
  }

  // everything below here are implementation methods for the Admin interface

  public String getName()
  {
    return adminName;
  }

  public String getPassword()
  {
    return adminPass;
  }

  public void setServerStart(Date date)
  {
  }

  public void setLastDumpTime(Date date)
  {
  }

  public void setTransactionsInJournal(int trans)
  {
  }

  public void setObjectsCheckedOut(int objs)
  {
  }

  public void setLocksHeld(int locks)
  {
  }

  public void changeStatus(String status)
  {
  }

  public void changeAdmins(String adminStatus)
  {
  }

  public void changeState(String state)
  {
  }

  public void changeUsers(Vector entries)
  {
  }

  public void changeTasks(Vector tasks)
  {
  }

  public void forceDisconnect(String reason)
  {
    server = null;
  }
}
