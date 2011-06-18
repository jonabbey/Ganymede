/*

   PasswordClient.java

   This class provides both the core of Ganymede password changing
   clients and a command line client.

   Created: 28 January 1998

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2011
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

package arlut.csd.ganymede.client.password;

import java.io.Console;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Properties;

import arlut.csd.Util.TranslationService;
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
import arlut.csd.ganymede.rmi.pass_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  PasswordClient

------------------------------------------------------------------------------*/

/**
 * <p>The core of a user password changing applet / application, whether
 * command line or GUI.</p>
 *
 * <p>This class can be invoked from the command line to act as an
 * interactive CLI password change client.</p>
 */

public class PasswordClient implements ClientListener {

  final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.password.PasswordClient");

  /**
   * URL of the Ganymede server's RMI port to connect to.
   */

  private static String url;

  // ---

  /**
   * <p>A ClientBase object forms the core of any Ganymede
   * client.  It does the work to get us connected and
   * logged into the server, and serves as a reference
   * point for the server to talk to if something
   * unusual happens.</p>
   */

  ClientBase client = null;

  /* -- */

  public PasswordClient(String serverURL) throws RemoteException
  {
    client = new ClientBase(serverURL, this);
  }

  /**
   * <p>This method actually does the work of logging into the server,
   * changing the password, committing the transaction, and
   * disconnecting.</p>
   *
   * <p>There are three basic steps involved in changing the password.
   * First, the client must log on to the system, getting a handle on
   * the Session object.  Next, we get a handle on the password field
   * of the user object, and change the value.  Finally, we commit the
   * transaction.</p>
   */

  public boolean changePassword(String username, String oldPassword, String newPassword)
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
        // "Connection to Ganymede server failed: {0}"
	error(ts.l("changePassword.connection_fail", ex.getMessage()));
	return false;
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

            // "Wrong password."
	    error(ts.l("changePassword.wrong_pass"));
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

	if (results == null)
	  {
	    // "No user {0} found, can''t change password."
	    System.out.println(ts.l("changePassword.no_such_user", username));

	    return false;
	  }
	else if (results.size() != 1)
	  {
	    System.out.println("Error, found multiple matching user records.. can't happen?");

	    return false;
	  }

	if (debug)
	  {
	    System.out.println("Changing password");
	  }

	// Invid's are id numbers for objects, the basic way to
	// referrencing objects in the server.

	Invid invid = results.getInvid(0);

	// To edit the object, we must check out the user through
	// the Session object.

	ReturnVal retVal = session.edit_db_object(invid);
	db_object user = (db_object) retVal.getObject();

	// If edit_db_object returns a null object, it usually
	// means someone else is editing the object.  It could
	// also mean that the user doesn't have sufficient
	// permission to edit the object.

	if (user == null)
	  {
	    // "Could not get handle on user object.  Someone else might be editing it."
	    error(ts.l("changePassword.locked"));
	    session.abortTransaction();
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

	if (!ReturnVal.didSucceed(returnValue))
	  {
	    if (debug)
	      {
		error("It didn't work.");
	      }

	    String resultText = returnValue.getDialogText();

	    if (resultText != null && !resultText.equals(""))
	      {
		System.err.println(resultText);
	      }

	    return false;
	  }

	// After making changes to the database, the session changes
	// must be committed.  This also returns a ReturnVal.

	ReturnVal rv = session.commitTransaction();

	if (ReturnVal.didSucceed(rv))
	  {
	    if (debug)
	      {
		System.out.println("It worked.");
	      }

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
    finally
      {
	try
	  {
	    client.disconnect();
	  }
	catch (RemoteException ex)
	  {
	  }
      }

    return false;
  }

  /**
   * <p>Send output to this.</p>
   *
   * <p>This just prints out the message, but could be directed to a
   * dialog or something later.</p>
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
   * <p>Called when the server forces a disconnect.</p>
   *
   * <p>Call getMessage() on the ClientEvent to get the reason for the
   * disconnect.</p>
   *
   * @see arlut.csd.ganymede.client.ClientListener
   * @see arlut.csd.ganymede.client.ClientEvent
   */

  public void disconnected(ClientEvent e)
  {
    // "Error, the server forced us to disconnect. {0}"
    error(ts.l("disconnected.kicked", e.getMessage()));
  }

  /**
   * <p>Called when the ClientBase needs to report something to the
   * client.  The client is expected to then put up a dialog or do
   * whatever else is appropriate.</p>
   *
   * <p>Call getMessage() on the ClientEvent to get the message.</p>
   *
   * @see arlut.csd.ganymede.client.ClientListener
   * @see arlut.csd.ganymede.client.ClientEvent
   */

  public void messageReceived(ClientEvent e)
  {
    // "Server: {0}"
    error(ts.l("messageReceived", e.getMessage()));
  }

  /**
   * <p>If this class is run from the command line, it will act as a
   * text-mode password client.</p>
   */

  public static void main(String argv[])
  {
    PasswordClient client = null;

    /* -- */

    if (argv.length != 2)
      {
        // "Wrong number of command line parameters: required parameters are <properties> <user>"

	System.err.println(ts.l("main.args_error"));
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
	client = new PasswordClient(url);
      }
    catch (Exception ex)
      {
	ex.printStackTrace();
	throw new RuntimeException("Couldn't connect to authentication server.. " +
				   ex.getMessage());
      }

    try
      {
	// get old password, new password

	Console cons = System.console();
	PrintWriter out = cons.writer();

	String oldPassword = null;
	String newPassword = null;
	String verifyPassword = null;

	// Get the old password

	// "Old password:"
	oldPassword = new String(cons.readPassword("[%s]", ts.l("main.old_pass_prompt")));

	// Get the new password.  Loop until the password is entered
	// correctly twice.

	do
	  {
	    // "New password:"
	    newPassword = new String(cons.readPassword("[%s]", ts.l("main.new_pass_prompt")));

	    // "Verify:"
	    verifyPassword = new String(cons.readPassword("[%s]", ts.l("main.verify_prompt")));

	    if (verifyPassword.equals(newPassword))
	      {
		break;
	      }

	    // "Passwords do not match.  Try again."
	    out.println(ts.l("main.no_match"));
	  } while (true);

	// Now change the password with the passwordClient.

	boolean success = client.changePassword(argv[1], oldPassword, newPassword);

	if (success)
	  {
	    // "Successfully changed password."
	    out.println(ts.l("main.success"));
	  }
	else
	  {
	    // "Password change failed."
	    out.println(ts.l("main.fail"));
	  }
      }
    finally
      {
	System.exit(0);
      }
  }

  /**
   * <p>Load the properties file that we use to identify the Ganymede
   * server and port for the command line client.</p>
   */

  private static boolean loadProperties(String filename)
  {
    Properties props = new Properties();
    boolean success = true;
    String serverhost;

    /* -- */

    BufferedInputStream bis = null;

    try
      {
	bis = new BufferedInputStream(new FileInputStream(filename));

	props.load(bis);
      }
    catch (IOException ex)
      {
	return false;
      }
    finally
      {
	if (bis != null)
	  {
	    try
	      {
		bis.close();
	      }
	    catch (IOException e)
	      {
	      }
	  }
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
            // Couldn''t get a valid registry port number from the ganymede.properties file: {0}
	    System.err.println(ts.l("loadProperties.bad_port_property", registryPort));
	  }
      }

    if (serverhost == null)
      {
        // "Couldn''t get the server host property."
	System.err.println(ts.l("loadProperties.bad_host_property"));
	success = false;
      }
    else
      {
	url = "rmi://" + serverhost + ":" + registryPortProperty + "/ganymede.server";
      }

    return success;
  }

}
