/*
   xmlclient.java

   This is a text client for the Ganymede server.  This client is
   designed to take the filename for an XML file on the command line,
   load the file, parse it, then connect to the server and attempt to
   transfer the objects specified in the XML file to the server using
   the standard Ganymede RMI API.

   --

   Created: 2 May 2000
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/05/04 04:17:43 $
   Release: $Name:  $

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

package arlut.csd.ganymede.client;

import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;

import arlut.csd.ganymede.*;
import arlut.csd.Util.ParseArgs;
import arlut.csd.Util.PackageResources;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       xmlclient

------------------------------------------------------------------------------*/

/**
 * <p>This is a text client for the Ganymede server.  This client is
 * designed to take the filename for an XML file on the command line,
 * load the file, parse it, then connect to the server and attempt to
 * transfer the objects specified in the XML file to the server using
 * the standard Ganymede RMI API.</p>
 *
 * @version $Revision: 1.1 $ $Date: 2000/05/04 04:17:43 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlclient implements ClientListener {

  public static String serverHostProperty = null;
  public static int    registryPortProperty = 1099;
  public static String username = null;
  public static String password = null;
  public static boolean disconnectNow = false;

  /**
   * <P>Hashtable mapping object type names to
   * hashes which map local object designations to
   * actual {@link arlut.csd.ganymede.client.xmlobject xmlobject}
   * records.</P>
   */

  public static Hashtable objectTypes = new Hashtable();

  /**
   * <P>The loader is a thread that obtains information from
   * the server on object type definitions present in the
   * server.  This is used to help guide the interpretation
   * of the XML file.</P>
   */

  public static Loader loader;

  /**
   * <P>RMI object to handle getting us logged into the server, and to
   * handle asynchronous callbacks from the server on our behalf.</P> 
   */

  private static ClientBase my_client;

  // ---

  public static void main(String argv[])
  {
    String propFilename = null;
    String server_url;
    String input = null;
    Session session = null;
    ReturnVal attempt = null;

    /* -- */

    // find the properties command line argument

    propFilename = ParseArgs.getArg("properties", argv);

    if (propFilename == null)
      {
	System.err.println("Ganymede xmlclient: Error, invalid command line parameters");
 	System.err.println("Usage: java xmlclient properties=<property file>");
	System.exit(1);
      }

    if (!loadProperties(propFilename))
      {
	System.out.println("Ganymede xmlclient: Error, couldn't successfully load properties from file " + 
			   propFilename + ".");
	System.exit(1);
      }

    server_url = "rmi://" + serverHostProperty + ":" + registryPortProperty + "/ganymede.server";

    // after the ClientBase is constructed, we'll be an active RMI
    // server, so we need to always do System.exit() to shut down the
    // VM, from this point forward

    try
      {
	my_client = new ClientBase(server_url, new xmlclient());
      }
    catch (RemoteException ex)
      {
	System.err.println("Could not connect to server" + ex.getMessage());
	System.exit(1);
      }

    try
      {
	session = my_client.login(username, password);
      }
    catch (RemoteException ex)
      {
	System.err.println("Ganymede xmlclient: couldn't log in for username " + username);
	System.exit(1);
      }

    // from this point on, we'll exit via the finally clause
    // below, so that we do a proper logout from the server

    try
      {
	// Loader is inherited from java.lang.Thread, so we can just
	// create one and start it running so that it will talk to the
	// server and download type and mapping information from the
	// server, in the background

	loader = new Loader(session, true);
	loader.start();

	try
	  {
	    attempt = session.openTransaction("xmlclient client (" + username + ")");

	    if (attempt != null && !attempt.didSucceed())
	      {
		if (attempt.getDialog() != null)
		  {
		    System.err.println("Ganymede xmlclient: couldn't open transaction " + username +
				       ": " + attempt.getDialog().getText());
		  }
		else
		  {
		    System.err.println("Ganymede xmlclient: couldn't open transaction " + username);
		  }

		return;
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("Ganymede xmlclient: couldn't open transaction " + 
			       username + ": " + ex.getMessage());
	    return;
	  }

	try
	  {
	    attempt = session.commitTransaction(true);

	    if (attempt != null && !attempt.didSucceed())
	      {
		if (attempt.getDialog() != null)
		  {
		    System.err.println("Ganymede xmlclient: couldn't commit transaction " + username +
				       ": " + attempt.getDialog().getText());
		    return;
		  }
		else
		  {
		    System.err.println("Ganymede xmlclient: couldn't commit transaction " + username);
		    return;
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("Ganymede.xmlclient: remote exception " + ex.getMessage());
	  }
      }
    finally
      {
	try
	  {
	    my_client.disconnect();
	  }
	catch (RemoteException ex)
	  {
	  }
	finally
	  {
	    System.exit(0);
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

    // make the combined properties file accessible throughout our
    // code.

    System.setProperties(props);

    serverHostProperty = System.getProperty("ganymede.serverhost");

    if (serverHostProperty == null)
      {
	System.err.println("Couldn't get the server host property");
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

  /**
   * <p>Called when the server forces a disconnect.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p>
   */

  public void disconnected(ClientEvent e)
  {
    disconnectNow = true;
  }

  /**
   * <p>Called when the ClientBase needs to report something
   * to the client.  The client is expected to then put
   * up a dialog or do whatever else is appropriate.</p>
   *
   * <p>Call getMessage() on the
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} 
   * to get the reason for the disconnect.</p>
   */

  public void messageReceived(ClientEvent e)
  {
  }

  /**
   * <p>This method is responsible for processing the XML filename
   * given and building up a hashtable which maps object type
   * names to hashes which map the local identifier to the
   * {@link arlut.csd.ganymede.client.xmlobject xmlobject}.</p>
   */

  public void loadFromXML(String filename)
  {
  }
}
