/*

   phoneClient.java

   A custom text client for doing quick-creation of VOIP phones in
   ARL's network, in which we let our custom ARL schema on the server
   pick out the IP address for the newly created phones, using the
   normal interactive address-autoselect which the xmlclient does not
   support.
   
   Created: 29 January 2007

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey and Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2007
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client.arl_phone;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Properties;

import arlut.csd.ganymede.client.ClientBase;
import arlut.csd.ganymede.client.ClientEvent;
import arlut.csd.ganymede.client.ClientListener;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.invid_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     phoneClient

------------------------------------------------------------------------------*/

public class phoneClient implements ClientListener {

  final static boolean debug = false;
  static String url;

  // ---

  /**
   * A ClientBase object forms the core of any Ganymede
   * client.  It does the work to get us connected and
   * logged into the server, and serves as a reference
   * point for the server to talk to if something
   * unusual happens.
   */

  ClientBase client = null;

  String ganymedeAccount;
  String ganymedePassword;

  String systemName;
  String networkName;
  String roomName;
  String macAddress;
  String manufacturer;
  String os;
  String model;
  String primaryUser;

  Invid roomInvid;
  Invid networkInvid;
  Invid primaryUserInvid;

  Invid systemTypeInvid;
  Invid ownerInvid;

  /* -- */

  public phoneClient(String serverURL) throws RemoteException
  {
    client = new ClientBase(serverURL, this);
  }

  /**
   * This method actually does the work of logging into the server,
   * setting up the new phone, committing the transaction, and
   * disconnecting.
   */

  public boolean createPhone()
  {
    arlut.csd.ganymede.rmi.Session sess;
    QueryResult results;

    /* -- */

    if (debug)
      {
	System.out.println(" logging into server");
      }

    try
      {
	client.connect();
      }
    catch (Exception ex)
      {
	error("Connection to Ganymede server failed: " + ex.getMessage());
	return false;
      }

    try
      {
	sess = client.login(ganymedeAccount, ganymedePassword);

	// If the username/password combination doesn't match, then
	// the session object will be null.  For the purposes of this
	// client, it is suficient to just return false and require
	// the user to rerun the phone client if the session is null.

	if (sess == null)
	  {
	    error("Bad username/password.");
	    return false;
	  }

	// Now that we have a sess object, we need to open a
	// transaction.  All changes must be made with an open
	// tranaction.

	sess.openTransaction("phoneClient");
	
        // first order of business is to find the network.
        
        results = sess.query(new Query("I.P. Network",
                                       new QueryDataNode("Network Name",
                                                         QueryDataNode.EQUALS,
                                                         networkName),
                                       false));

        if (results == null || results.size() != 1)
          {
            error("Couldn't find a network named " + networkName);
            client.disconnect();
            return false;
          }

        networkInvid = results.getInvid(0);

        // find the room

        results = sess.query(new Query("Room",
                                       new QueryDataNode("Room Number",
                                                         QueryDataNode.EQUALS,
                                                         roomName),
                                       false));

        if (results == null || results.size() != 1)
          {
            error("Couldn't find a room named " + roomName);
            client.disconnect();
            return false;
          }

        roomInvid = results.getInvid(0);

        // find the user

        results = sess.query(new Query("User",
                                       new QueryDataNode("Username",
                                                         QueryDataNode.EQUALS,
                                                         primaryUser),
                                       false));

        if (results == null || results.size() != 1)
          {
            error("Couldn't find a primary user named " + primaryUser);
            client.disconnect();
            return false;
          }
        
        primaryUserInvid = results.getInvid(0);

        // find the system type

        results = sess.query(new Query("System Types",
                                       new QueryDataNode("System Type",
                                                         QueryDataNode.EQUALS,
                                                         "IP Telephone"),
                                       false));

        if (results == null || results.size() != 1)
          {
            error("Couldn't find the IP Telephone system type");
            client.disconnect();
            return false;
          }

        systemTypeInvid = results.getInvid(0);

        // find the owner group

        results = sess.query(new Query("Owner Group",
                                       new QueryDataNode("Name",
                                                         QueryDataNode.EQUALS,
                                                         "TeleCom"),
                                       false));
        
        if (results == null || results.size() != 1)
          {
            error("Couldn't find an owner group named TeleCom");
            client.disconnect();
            return false;
          }

        ownerInvid = results.getInvid(0);

        // and let's go to town

        ReturnVal retVal = sess.create_db_object("System");
        db_object phone = (db_object) retVal.getObject(); 

        // If create_db_object returns a null object, it usually
        // means that the user doesn't have sufficient permission
        // to create the object.

        if (phone == null)
          {
            error(retVal);
            client.disconnect();
            return false;
          }

        // set the owner first off

        invid_field owners = (invid_field) phone.getField((short)0);  // field 0 is the owner list

        if (bad(owners.deleteAllElements()))
          {
            client.disconnect();
            return false;
          }

        if (bad(owners.addElement(ownerInvid)))
          {
            client.disconnect();
            return false;
          }

        // and let's set the data fields

        if (bad(phone.setFieldValue("System Name", systemName)))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.setFieldValue("Manufacturer", manufacturer)))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.setFieldValue("O.S.", os)))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.setFieldValue("Model", model)))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.setFieldValue("System Type", systemTypeInvid)))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.setFieldValue("Room", roomInvid)))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.setFieldValue("Primary User", primaryUserInvid)))
          {
            client.disconnect();
            return false;
          }

        // now we need to get the interface object that was created
        // for us by default

        invid_field interfaces = (invid_field) phone.getField("Interface");
        Invid interfaceInvid = (Invid) interfaces.getElement(0);

        ReturnVal retVal2 = sess.edit_db_object(interfaceInvid);

        if (bad(retVal2))
          {
            client.disconnect();
            return false;
          }

        db_object interfaceObj = retVal2.getObject();

        if (interfaceObj == null || bad(interfaceObj.setFieldValue("I.P. Network", networkInvid)))
          {
            client.disconnect();
            return false;
          }

        if (bad(interfaceObj.setFieldValue("Ethernet Info", macAddress)))
          {
            client.disconnect();
            return false;
          }

        if (bad(sess.commitTransaction()))
          {
            client.disconnect();
            return false;
          }

        error("Phone created successfully");
        client.disconnect();
        return true;
      }
    catch (Throwable ex)
      {
        error("Caught exception processing phone creation:");
        ex.printStackTrace();
        return false;
      }
  }

  /**
   * Simple helper, tests a ReturnVal from the server, and if it
   * failed in some fashion, prints any error text to stderr.
   *
   * Return true if there was a problem.
   */

  public boolean bad(ReturnVal retVal)
  {
    if (retVal != null && !retVal.didSucceed())
      {
        error(retVal);

        return true;
      }

    return false;
  }
  
  /**
   * Our error handling routine.
   */

  public void error(String message)
  {
    System.err.println(message);
  }

  public void error(ReturnVal dialog)
  {
    System.err.println(dialog.getDialogText());
  }

  // ***
  //
  // The following two methods comprise the ClientListener interface
  // which we need to implement to give the ClientBase object someone
  // to talk to.
  //
  // ***

  /**
   *
   * Called when the server forces a disconnect.<br><br>
   *
   * Call getMessage() on the ClientEvent to get the
   * reason for the disconnect.
   *
   * @see arlut.csd.ganymede.client.ClientListener
   * @see arlut.csd.ganymede.client.ClientEvent
   *
   */

  public void disconnected(ClientEvent e)
  {
    error("Error, prematurely kicked off by the server. " + e.getMessage());
  }

  /**
   *
   * Called when the ClientBase needs to report something
   * to the client.  The client is expected to then put
   * up a dialog or do whatever else is appropriate.<br><br>
   *
   * Call getMessage() on the ClientEvent to get the
   * message.
   *
   * @see arlut.csd.ganymede.client.ClientListener
   * @see arlut.csd.ganymede.client.ClientEvent
   *
   */

  public void messageReceived(ClientEvent e)
  {
  }

  /**
   * If this class is run from the command line, it will act as a
   * text-mode phone client.
   */
  
  public static void main(String argv[])
  {
    phoneClient client = null;

    /* -- */

    if (argv.length != 1)
      {
	System.err.println("Wrong number of params: required param is <properties>");
	System.exit(1);
      }

    // Get the server URL

    loadProperties(argv[0]);

    // Create the client

    try
      {
	client = new phoneClient(url);
      }
    catch (Exception ex)
      {
        System.err.println("Couldn't connect to server.");
	ex.printStackTrace();
        System.exit(1);
      }

    // Get the phone information from stdin

    try
      {
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

        client.ganymedeAccount = in.readLine();
        client.ganymedePassword = in.readLine();
        client.systemName = in.readLine();
        client.networkName = in.readLine();
        client.macAddress = in.readLine();
        client.manufacturer = in.readLine();
        client.os = in.readLine();
        client.model = in.readLine();
        client.roomName = in.readLine();
        client.primaryUser = in.readLine();
      }
    catch (java.io.IOException ex)
      {
        System.err.println("Exception getting data from stdin:");
        ex.printStackTrace();
        System.exit(1);
      }

    if (client.createPhone())
      {
        System.exit(0);
      }
    else
      {
	System.out.println("Phone creation failed.");
        System.exit(1);
      }
  }

  private static boolean loadProperties(String filename)
  {
    Properties props = new Properties();
    boolean success = true;
    String serverhost;

    /* -- */

    try
      {
	props.load(new BufferedInputStream(new FileInputStream(filename)));
      }
    catch (IOException ex)
      {
	return false;
      }

    serverhost = props.getProperty("ganymede.serverhost");
    
    int registryPortProperty = 1099;

    String registryPort = props.getProperty("ganymede.registryPort");

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

    if (serverhost == null)
      {
	System.err.println("Couldn't get the server host property");
	success = false;
      }
    else
      {
	url = "rmi://" + serverhost + ":" + registryPortProperty + "/ganymede.server";
      }

    return success;
  }

}
