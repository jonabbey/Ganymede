/*

   PasswordClient.java

   The core of a gui/text password changing client for Ganymede.
   
   Created: 28 January 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client.password;

import arlut.csd.ganymede.*;

import java.util.Properties;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  PasswordClient

------------------------------------------------------------------------------*/

public class PasswordClient extends UnicastRemoteObject implements arlut.csd.ganymede.Client {

  final static boolean debug = false;

  static String serverhost;
  static String url;

  /* -- */

  String 
    username,
    password;

  arlut.csd.ganymede.Server 
    my_server;

  arlut.csd.ganymede.Session 
    session;

  /* -- */  

  public PasswordClient() throws RemoteException
  {
    this("rmi://www.arlut.utexas.edu/ganymede.server");
  }

  public PasswordClient(String serverURL) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization.. RemoteException may be thrown

    try
      {
	Remote obj = Naming.lookup(serverURL);
    
	if (obj instanceof arlut.csd.ganymede.Server)
	  {
	    my_server = (arlut.csd.ganymede.Server) obj;
	  }
	else
	  {
	    throw new RuntimeException("Unrecognized object type returned: " + obj.getClass());
	  }
      }
    catch (NotBoundException ex)
      {
	throw new RuntimeException("RMI: Couldn't bind to server object\n" + ex );
      }
    catch (java.rmi.UnknownHostException ex)
      {
	throw new RuntimeException("RMI: Couldn't find server\n");
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("RMI: RemoteException during lookup.\n" + ex);
      }
    catch (java.net.MalformedURLException ex)
      {
	throw new RuntimeException("RMI: Malformed URL ");
      }
  }

  public boolean changePassword(String username, String oldPassword, String newPassword)
  {
    this.username = username;
    this.password = oldPassword;

    if (debug)
      {
	System.out.println(" logging into server");
      }

    try
      {
	session = my_server.login(this);

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
		    session.logout();
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
		    session.logout();
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

		session.logout();
		return false;
	      }

	    ReturnVal rv = session.commitTransaction();

	    if (rv == null || rv.didSucceed())
	      {
		if (debug)
		  {
		    System.out.println("It worked.");
		  }

		this.password = newPassword;
		session.logout();
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
   * Send output to this.  
   *
   * This just prints out the message, but could be directed to a dialog or something later.
   */

  public void error(String message)
  {
    System.out.println(message);
  }

  /**
   * Allows the server to retrieve the username
   */

  public String getName() 
  {
    return username;
  }

  /**
   * Allows the server to retrieve the password
   */

  public String getPassword()
  {
    return password;
  }

  /**
   * Allows the server to force us off when it goes down
   */

  public void forceDisconnect(String reason)
  {
    error("Server forced disconnect: " + reason);
    System.exit(0);
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
