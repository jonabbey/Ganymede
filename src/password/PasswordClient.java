/*

   PasswordClient.java

   The core of a gui/text password changing client for Ganymede.
   
   Created: 28 January 1998
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/01/22 18:04:21 $
   Module By: Michael Mulvaney

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

package arlut.csd.ganymede.client.password;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.client.*;

import java.util.Properties;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  PasswordClient

------------------------------------------------------------------------------*/

public class PasswordClient implements ClientListener {

  final static boolean debug = false;
  static String url;

  // ---

  /**
   *
   * A ClientBase object forms the core of any Ganymede
   * client.  It does the work to get us connected and
   * logged into the server, and serves as a reference
   * point for the server to talk to if something
   * unusual happens.
   *
   */

  ClientBase client = null;

  /* -- */

  public PasswordClient(String serverURL) throws RemoteException
  {
    client = new ClientBase(serverURL, this);
  }

  /**
   *
   * This method actually does the work of logging into the server,
   * changing the password, committing the transaction, and disconnecting.
   *
   * There are three basic steps involved in changing the password.
   * First, the client must log on to the system, getting a handle on
   * the Session object.  Next, we get a handle on the password field
   * of the user object, and change the value.  Finally, we commit the
   * transaction.
   */

  public boolean changePassword(String username, String oldPassword, String newPassword)
  {
    arlut.csd.ganymede.Session session;

    /* -- */

    if (debug)
      {
	System.out.println(" logging into server");
      }

    try
      {
	// First we need a referrence to a Session object.  This is
	// accomplished through the ClientBase's login method.
	session = client.login(username, oldPassword);

	// If the username/password combination doesn't match, then
	// the session object will be null.  For the purposes of this
	// client, it is suficient to just return false and require
	// the user to rerun the password client if the session is null.
	if (session == null)
	  {
	    if (debug)
	      {
		System.out.println(" logged in, looking for :" + username + ":");
	      }
	    
	    error("Wrong password.");
	    return false;
	  }

	// Now that we have a session object, we need to open a
	// transaction.  All changes must be made with an open
	// tranaction.

	session.openTransaction("PasswordClient");
	
	// In order to change the password, we must first get a handle
	// on the user object.  This is accomplished through the
	// server's Query engine.

	QueryResult results = session.query(new Query(SchemaConstants.UserBase,
						      new QueryDataNode(SchemaConstants.UserUserName,
									QueryDataNode.EQUALS, username)));
	
	if (results != null && results.size() == 1)
	  {
	    if (debug)
	      {
		System.out.println(" Changing password");
	      }
	    
	    // Invid's are id numbers for objects, the basic way to
	    // referrencing objects in the server.
	    Invid invid = results.getInvid(0);
	    
	    // To edit the object, we must check out the user through
	    // the Session object.

	    ReturnVal retVal = session.edit_db_object(invid);
	    db_object user = retVal.getObject(); 

	    // If edit_db_object returns a null object, it usually
	    // means someone else is editing the object.  It could
	    // also mean that the user doesn't have sufficient
	    // permission to edit the object.

	    if (user == null)
	      {
		error("Could not get handle on user object.  Someone else might be editing it.");
		session.abortTransaction();
		client.disconnect();
		return false;
	      }

	    // pass_field is a subclass of db_field, which represents
	    // the fields in each object.  We need a referrence to the
	    // user's password field, so we can change it.
	    pass_field pass = (pass_field)user.getField(SchemaConstants.UserPassword);
	    
	    // Changes to objects on the server return a ReturnVal.
	    // ReturnVal contains information about the change just
	    // made, including a list of fields that may have changed
	    // as a result of this change, or dialogs prompting the
	    // user for more information.

	    // For the purposes of this application, we don't care
	    // about the extra stuff in ReturnVal; we only want to
	    // know if the password change worked or not.
	    ReturnVal returnValue = pass.setPlainTextPass(newPassword);

	    // A null value means success.
	    if (returnValue == null)
	      {
		if (debug)
		  {
		    error("It worked, returnValue is null");
		  }
	      }
	    // If ReturnVal is not null, the didSucceed() boolean is
	    // set to indicate failure or success.
	    else if (returnValue.didSucceed())
	      {
		if (debug)
		  {
		    error("returnValue is not null, but it did suceed.");
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    error("It didn't work.");
		  }
		
		client.disconnect();
		return false;
	      }
	  }
	else
	  {
	    if (results == null)
	      {
		System.out.println("No user " + username + ", can't change password");
	      }
	    else
	      {
		System.out.println("Error, found multiple matching user records.. can't happen?");
	      }
	    
	    client.disconnect();
	    return false;
	  }

	// After making changes to the database, the session changes
	// must be committed.  This also returns a ReturnVal.
	ReturnVal rv = session.commitTransaction();
	
	if (rv == null || rv.didSucceed())
	  {
	    if (debug)
	      {
		System.out.println("It worked.");
	      }
	    
	    client.disconnect();
	    return true;
	  }
	else
	  {
	    error("Error commiting transaction, password change failed.");
	  }
      }
  
    catch (RemoteException ex)
      {
	error("Caught remote exception in authenticate: " + ex);
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
    System.out.println(message);
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
    PasswordClient client = null;

    /* -- */

    if (argv.length != 2)
      {
	System.err.println("Wrong number of params: required params is <properties> <user>");
	System.exit(0);
      }

    // Get the server URL
    loadProperties(argv[0]);

    /* RMI initialization stuff. */
      
    System.setSecurityManager(new RMISecurityManager());

    // Create the client
    try
      {
	client = new PasswordClient(url);
      }
    catch (Exception ex)
      {
	throw new RuntimeException("Couldn't connect to authentication server.. " + ex);
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

    if (serverhost == null)
      {
	System.err.println("Couldn't get the server host property");
	success = false;
      }
    else
      {
	url = "rmi://" + serverhost + "/ganymede.server";
      }

    return success;
  }

}
