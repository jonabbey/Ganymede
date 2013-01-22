
/*
   GASH 2

   stopServer.java

   Command-line app to shut down a Ganymede server, with password info
   drawn from a properties file.

   Created: 28 April 1999

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.adminSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      stopServer

------------------------------------------------------------------------------*/

/**
 * Command-line app to shut down a Ganymede server, with password info
 * drawn from a properties file.
 */

public class stopServer {

  public static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.stopServer");


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
        // "Ganymede stopServer: Error, invalid command line parameters.\nUsage: java stopServer -delay properties=<property file>"
        System.err.println(ts.l("main.usage"));
        return;
      }

    if (!loadProperties(propFilename))
      {
        // "Ganymede stopServer: Error, couldn''t successfully load properties from file "{0}"."
        System.err.println(ts.l("main.no_props", propFilename));
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
        else
          {
            // "main.not_bound = Ganymede stopServer: Remote RMI object is not a Ganymede server object at URL "{0}". "
            System.err.println(ts.l("main.not_server", server_url));
            System.exit(1);
          }
      }
    catch (NotBoundException ex)
      {
        // "Ganymede stopServer: No server found running at URL "{0}".  This may possibly be due to a connection problem.\n{1}"
        System.err.println(ts.l("main.not_bound", server_url, ex.getMessage()));
        System.exit(1);
      }
    catch (java.rmi.UnknownHostException ex)
      {
        // "Ganymede stopServer: Unknown host exception encountered when looking up URL "{0}"."
        System.err.println(ts.l("main.host", server_url));
        System.exit(1);
      }
    catch (RemoteException ex)
      {
        // "Ganymede stopServer: Remote exception caught trying to connect to server object "{0}".\n\n{1}"
        System.err.println(ts.l("main.remote_exception", server_url, ex.getMessage()));
        System.exit(1);
      }
    catch (java.net.MalformedURLException ex)
      {
        // "Ganymede stopServer: Error, malformed URL for server: "{0}"."
        System.err.println(ts.l("main.malformed", server_url));
        System.exit(1);
      }

    if (defaultrootpassProperty == null || defaultrootpassProperty.equals(""))
      {
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

        // "Ganymede server admin password:\ "
        System.out.print(ts.l("main.pass_prompt"));

        try
          {
            defaultrootpassProperty = in.readLine();
          }
        catch (IOException ex)
          {
            ex.printStackTrace();
            System.exit(1);
          }

        System.out.println();
      }

    // "Ganymede stopServer: Shutting down Ganymede server at "{0}"."
    System.out.println(ts.l("main.working", server_url));

    try
      {
        admin = new stopServerAdmin(server, rootname, defaultrootpassProperty);
      }
    catch (RemoteException rx)
      {
        // "Ganymede stopServer: Remote exception caught trying to order shutdown for server object "{0}".\n\n{1}"
        System.err.println(ts.l("main.remote_exception2", server_url, rx));
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
        // "Ganymede stopServer: Ganymede server "{0}" shut down."
        System.out.println(ts.l("main.shut_down", server_url));
      }
    else
      {
        /* "Ganymede stopServer: Ganymede server shutdown initiated for Ganymede server "{0}".\n\
           Server will shut down as soon as all current users are logged out."*/

        System.out.println(ts.l("main.waiting_to_shut_down", server_url));
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

    BufferedInputStream in = null;

    try
      {
        in = new BufferedInputStream(new FileInputStream(filename));
        props.load(in);
      }
    catch (IOException ex)
      {
        return false;
      }
    finally
      {
        if (in != null)
          {
            try
              {
                in.close();
              }
            catch (IOException ex)
              {
                throw new RuntimeException(ex);
              }
          }
      }

    // make the combined properties file accessible throughout our server
    // code.

    System.setProperties(props);

    serverHostProperty = System.getProperty("ganymede.serverhost");
    rootname = System.getProperty("ganymede.rootname");
    defaultrootpassProperty = System.getProperty("ganymede.defaultrootpass");

    if (serverHostProperty == null)
      {
        // "Couldn''t find a ganymede.serverhost property to load."
        System.err.println(ts.l("loadProperties.no_server_host"));
        success = false;
      }

    if (rootname == null)
      {
        // "Couldn''t find a ganymede.rootname property to load.  Defaulting to "supergash"."
        System.err.println(ts.l("loadProperties.no_root_prop"));
        rootname = "supergash";
      }

    if (debug && defaultrootpassProperty == null)
      {
        System.err.println("Couldn't get the default rootname password property");
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
            // "Couldn''t get a valid Integer registry port number from the Ganymede properties file.  Couldn''t parse "{0}"."
            System.err.println(ts.l("loadProperties.number_format", registryPort));
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

class stopServerAdmin {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.stopServerAdmin");

  private adminSession aSession = null;

  /* -- */

  public stopServerAdmin(Server server, String name, String pass) throws RemoteException
  {
    ReturnVal retVal = null;

    /* -- */

    try
      {
        retVal = server.admin(name, pass);

        if (ReturnVal.didSucceed(retVal))
          {
            aSession = retVal.getAdminSession();
          }
        else
          {
            // "Ganymede.stopServer: Error, couldn''t log into server with admin privileges."
            System.err.println(ts.l("init.login_failure"));

            String error = retVal.getDialogText();

            if (error != null && !error.equals(""))
              {
                System.err.println(error);
              }

            System.exit(1);
          }
      }
    catch (NullPointerException ex)
      {
        // "Ganymede.stopServer: Error, couldn''t log into server with admin privileges."
        System.err.println(ts.l("init.login_failure"));
        System.exit(1);
      }

    if (aSession == null)
      {
        // "Ganymede stopServer: Error, couldn''t log into server with admin privileges.. bad password?"
        System.err.println(ts.l("init.no_session"));
        System.exit(1);
      }
  }

  void shutdown(boolean waitForUsers) throws RemoteException
  {
    aSession.shutdown(waitForUsers, null);
  }

  public void disconnect() throws RemoteException
  {
    aSession.logout();
  }
}
