/*

   PasswordClient.java

   The core of a gui/text password changing client for Ganymede.
   
   Created: 28 January 1998
   Version: $Revision: 1.3 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

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
	session = client.login(username, oldPassword);

	if (session != null)
	  {
	    if (debug)
	      {
		System.out.println(" logged in, looking for :" + username + ":");
	      }
	    
	    session.openTransaction("PasswordClient");

	    QueryResult results = session.query(new Query(SchemaConstants.UserBase,
							  new QueryDataNode(SchemaConstants.UserUserName,
									    QueryDataNode.EQUALS, username)));
	    
	    if (results != null && results.size() == 1)
	      {
		if (debug)
		  {
		    System.out.println(" Changing password");
		  }

		Invid invid = results.getInvid(0);

		db_object user = session.edit_db_object(invid);

		if (user == null)
		  {
		    error("Could not get handle on user object.  Someone else might be editing it.");
		    session.abortTransaction();
		    client.disconnect();
		    return false;
		  }

		pass_field pass = (pass_field)user.getField(SchemaConstants.UserPassword);
		
		ReturnVal returnValue = pass.setPlainTextPass(newPassword);

		if (returnValue == null)
		  {
		    if (debug)
		      {
			error("It worked, returnValue is null");
		      }
		  }
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
	else
	  {
	    error("Wrong password.");
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
   * This just prints out the message, but could be directed to a dialog or something later.
   *
   */

  public void error(String message)
  {
    System.out.println(message);
  }

  // ***
  //
  // The following two methods comprise the ClientListener interface which
  // we need to implement to give the ClientBase object someone to talk to.
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

    loadProperties(argv[0]);

    /* RMI initialization stuff. */
      
    System.setSecurityManager(new RMISecurityManager());

    try
      {
	client = new PasswordClient(url);
      }
    catch (Exception ex)
      {
	throw new RuntimeException("Couldn't connect to authentication server.. " + ex);
      }

    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

    // get old password, new password

    String oldPassword = null, newPassword = null;

    try
      {
	// Get the old password

	System.out.print("Old password:");
	oldPassword = in.readLine();
	System.out.println();

	String verify = null;

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
