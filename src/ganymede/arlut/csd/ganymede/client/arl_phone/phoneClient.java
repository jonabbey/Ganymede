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
    arlut.csd.ganymede.rmi.Session session;

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
	session = client.login(ganymedeAccount, ganymedePassword);

	// If the username/password combination doesn't match, then
	// the session object will be null.  For the purposes of this
	// client, it is suficient to just return false and require
	// the user to rerun the password client if the session is null.

	if (session == null)
	  {
	    error("Bad username/password.");
	    return false;
	  }

	// Now that we have a session object, we need to open a
	// transaction.  All changes must be made with an open
	// tranaction.

	session.openTransaction("phoneClient");
	
        // first order of business is to find the network.

	QueryResult results = session.query(new Query("I.P. Network",
						      new QueryDataNode("Network Name",
									QueryDataNode.EQUALS,
                                                                        networkName)));

        if (results == null || results.size() != 1)
          {
            error("Couldn't find a network named " + networkName);
            client.disconnect();
            return false;
          }

        // find the network

        networkInvid = results.getInvid(0);

        results = session.query(new Query("Rooms",
                                          new QueryDataNode("Room Number",
                                                            QueryDataNode.EQUALS,
                                                            roomName)));

        if (results == null || results.size() != 1)
          {
            error("Couldn't find a room named " + roomName);
            client.disconnect();
            return false;
          }

        // find the room

        roomInvid = results.getInvid(0);

        if (primaryUser != null && !primaryUser.equals(""))
          {
            results = session.query(new Query("User",
                                              new QueryDataNode("Username",
                                                                QueryDataNode.EQUALS,
                                                                primaryUser)));

            if (results == null || results.size() != 1)
              {
                error("Couldn't find a primary user named " + primaryUser);
                client.disconnect();
                return false;
              }

            primaryUserInvid = results.getInvid(0);
          }

        // find the system type

        results = session.query(new Query("System Types",
                                          new QueryDataNode("System Type",
                                                            QueryDataNode.EQUALS,
                                                            "IP Telephone")));
        
        if (results == null || results.size() != 1)
          {
            error("Couldn't find the IP Telephone system type");
            client.disconnect();
            return false;
          }

        systemTypeInvid = results.getInvid(0);

        // find the owner group

        results = session.query(new Query("Owner Group",
                                          new QueryDataNode("Name",
                                                            QueryDataNode.EQUALS,
                                                            "TeleCom")));
        
        if (results == null || results.size() != 1)
          {
            error("Couldn't find an owner group named TeleCom");
            client.disconnect();
            return false;
          }

        ownerInvid = results.getInvid(0);

        // and let's go to town

        ReturnVal retVal = session.create_db_object("System");
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

        // set the owner

        invid_field owners = (invid_field) phone.getField(0);  // field 0 is the owner list

        if (bad(phone.deleteAllElements()))
          {
            client.disconnect();
            return false;
          }

        if (bad(phone.addElement(ownerInvid)))
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

        if (bad(phone.setFieldValue("O.S.", "Phone")))
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
        db_object interfaceObj = (db_object) interfaces.getElement(0);

        if (bad(interfaceObj.setFieldValue("I.P. Network", networkInvid)))
          {
            client.disconnect();
            return false;
          }

        if (bad(interfaceObj.setFieldValue("Ethernet Info", macAddress)))
          {
            client.disconnect();
            return false;
          }

        if (bad(session.commitTransaction()))
          {
            client.disconnect();
            return false;
          }
        else
          {
            error("Phone created successfully");
            return true;
          }
      }
    catch (RemoteException ex)
      {
	error("Caught remote exception in authenticate: " + ex);
      }
    
    return false;
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
   *
   * Send output to this.  
   *
   * This just prints out the message, but could be directed to a
   * dialog or something later.
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
    error("ClientBase says: " + e.getMessage());
  }

  /**
   *
   * If this class is run from the command line, it will act as a
   * text-mode password client.
   *
   */
  
  public static void main(String argv[])
  {
    phoneClient client = null;

    /* -- */

    if (argv.length != 2)
      {
	System.err.println("Wrong number of params: required params is <properties> <user>");
	System.exit(0);
      }

    // Get the server URL

    loadProperties(argv[0]);

    /* RMI initialization stuff. */
      
    /* This causes problems in Java 1.2.

       System.setSecurityManager(new RMISecurityManager());*/

    // Create the client

    try
      {
	client = new phoneClient(url);
      }
    catch (Exception ex)
      {
	ex.printStackTrace();
	throw new RuntimeException("Couldn't connect to authentication server.. " + 
				   ex.getMessage());
      }

    // Get the passwords from standard in

    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

    // get old password, new password

    String oldPassword = null, newPassword = null;

    try
      {
	// WARNING - This client won't hide the user's text.  When we
	// use this client, we wrap it in a shell script that calls
	// "stty -echo" before starting the client.

	// Get the old password

	System.out.print("Old password:");
	oldPassword = in.readLine();
	System.out.println();

	String verify = null;

	// Get the new password.  Loop until the password is entered
	// correctly twice.
	do
	  {
	    System.out.print("New password:");
	    newPassword = in.readLine();
	    System.out.println();

	    System.out.print("Verify:");
	    verify = in.readLine();
	    System.out.println();
	    
	    if (verify.equals(newPassword))
	      {
		break;
	      }
	    
	    System.out.println("Passwords do not match.  Try again.");
	  } while (true);
      }
    catch (java.io.IOException ex)
      {
	throw new RuntimeException("Exception getting input: " + ex);
      }

    // Now change the password with the passwordClient.

    boolean success = client.changePassword(argv[1], oldPassword, newPassword);

    if (success)
      {
	System.out.println("Successfully changed password.");
      }
    else
      {
	System.out.println("Password change failed.");
      }

    System.exit(0);
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
