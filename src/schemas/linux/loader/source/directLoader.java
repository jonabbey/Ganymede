/*
   directLoader.java

   Ganymede directLoader module

   This module is intended to be bound to the bulk of the Ganymede
   server and automatically create a whole bunch of objects
   to initialize the database from a pair of BSD 4.4 compatible
   master.passwd/group files.

   --

   Created: 20 October 1997
   Version: $Revision: 1.14 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.net.*;

import java.io.*;
import java.util.*;

import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.custom.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    directLoader

------------------------------------------------------------------------------*/

public class directLoader {

  static final boolean debug = true;

  static GanymedeServer my_server;
  static Session my_session;
  static directLoaderClient my_client;

  static Vector ownerGroups = new Vector();
  static Hashtable personaMap = new Hashtable();

  static Hashtable users = new Hashtable();
  static Hashtable userInvids = new Hashtable();

  static Hashtable groups = new Hashtable();
  static Hashtable groupID = new Hashtable();
  static Hashtable groupInvid = new Hashtable();

  static Invid gashadminPermInvid;		// the standard GASH admin privileges

  static FileInputStream inStream = null;
  static boolean done = false;

  static User userObj;
  static Group groupObj;

  // --

  protected boolean connected = false;

  /* -- */

  public static void main (String args[])
  {
    if (args.length < 1)
      {
	System.err.println("Usage error: java arlut.csd.ganymede.loader.directLoader <properties file>");
      }
    
    Ganymede.loadProperties(args[0]);
      
    my_server = Ganymede.directInit(Ganymede.dbFilename);

    // now login

    try
      {
	my_client = new directLoaderClient(my_server);
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n" + ex.getMessage());
	return;
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.\n\nPlease Quit and Restart.");
	return;
      }
    catch (Exception ex) 
      {
	System.err.println("Error: Unrecognized exception " + ex);
	return;
      }

    try
      {
	System.err.println("Writing schema summary to schema.list");
	FileOutputStream textOutStream = new FileOutputStream("schema.list");
	PrintWriter textOut = new PrintWriter(textOutStream);
	Ganymede.db.printBases(textOut);
	textOut.close();
	textOutStream.close();
	System.err.println("Completed writing schema summary to schema.list");
      }
    catch (IOException ex)
      {
	System.err.println("heck.");
      }

    System.err.println("---------------------------------------- Initiating BSD file scan --------------------");

    scanUsers();
    scanGroups();

    System.err.println("\n---------------------------------------- Completed BSD file scan --------------------\n");
    
    // Okay.. at this point we've scanned the files we need to scan..
    // now we initialize the database module and create the objects

    try
      {
	my_client.session.openTransaction("BSD directLoader");

	String key;
	Invid invid, objInvid;
	Enumeration enum;
	DBEditObject current_obj;
	db_field current_field;
	pass_field p_field;
	DBObjectBase base;
	ReturnVal retVal;

	/* -- */

	System.err.println("\nCreating GASH standard permission matrix");

	my_client.session.checkpoint("GASHAdmin");

	current_obj = (DBEditObject) createObject(SchemaConstants.RoleBase);
	gashadminPermInvid = current_obj.getInvid();

	System.err.println("Trying to create a new GASHAdmin perm object: " + gashadminPermInvid.toString());

	retVal = current_obj.setFieldValueLocal(SchemaConstants.RoleName, "GASH Admin");

	if (retVal != null && !retVal.didSucceed())
	  {
	    // the gash privilege matrix object is already in the schema.. rollback

	    my_client.session.rollback("GASHAdmin");

	    // note that QueryDataNode uses the label as the
	    // comparator if you don't provide a field id

	    Query q = new Query(SchemaConstants.RoleBase, 
				new QueryDataNode(QueryDataNode.EQUALS, "GASH Admin"),
				false);

	    QueryResult results = my_client.session.query(q);
	    
	    if (results != null && results.size() != 1)
	      {
		// didn't find it
		System.err.println("Could neither create a new GASH Admin permission matrix, nor find the old one.");
		System.exit(0);
	      }

	    gashadminPermInvid = results.getInvid(0);

	    // We'll get a null pointer exception here if it didn't work out

	    System.err.println("Using pre-existing gash privilege matrix: " + gashadminPermInvid.toString());
	  }
	else
	  {
	    System.err.println("\n***Default permissions matrix is " + gashadminPermInvid.toString());

	    perm_field pf = (perm_field) current_obj.getField(SchemaConstants.RoleMatrix);

	    PermEntry defPerm = new PermEntry(true, true, true, true);
	
	    pf.setPerm(SchemaConstants.UserBase, defPerm);
	    pf.setPerm((short) 257, defPerm); // group privs
	    pf.setPerm((short) 258, defPerm); // shell
	  }

	commitTransaction();
	my_client.session.openTransaction("BSD directLoader");

	System.out.println("\nRegistering users\n");

	registerUsers();
	commitTransaction();

	System.out.println("\nRegistering groups\n");
	my_client.session.openTransaction("BSD directLoader");
	registerGroups();
	commitTransaction();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
    
    // now logout

    System.out.println("Logging out");

    try 
      {
	my_client.disconnect();
      }
    catch (RemoteException ex) 
      {
	System.err.println("Remote exception disconnecting: " + ex);
      }

    System.out.println("Dumping the database");

    my_server.dump();
      
    System.exit(0);
  }

  private static void commitTransaction() throws RemoteException
  {
    ReturnVal retVal = my_client.session.commitTransaction(true);

    if (retVal != null && !retVal.didSucceed())
      {
	throw new RuntimeException("Could not commit transaction, aborting loader");
      }
  }

  /*----------------------------------------------------------------------------*/

  //
  // the following methods handle the parsing of a particular BSD file
  //

  /*----------------------------------------------------------------------------*/

  /**
   *
   */

  private static void scanUsers()
  {
    // now process the user_info file

    try
      {
	inStream = new FileInputStream("input/master.passwd");
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find master.passwd in the input directory");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inStream);

    User.initTokenizer(tokens);

    System.out.println("Scanning passwd");

    while (!done)
      {
	try
	  {
	    userObj = new User();
	    done = userObj.loadLine(tokens);

	    if (!done && userObj.valid)
	      {
		users.put(userObj.name, userObj);

		if (debug)
		  {
		    System.out.println("\n\n");
		    userObj.display();
		  }
		else
		  {
		    System.out.print(".");
		  }
	      }
	  }
	catch (EOFException ex)
	  {
	    done = true;
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.out.println("Done scanning passwd");

    try
      {
	inStream.close();
      }
    catch (IOException ex)
      {
	System.err.println("unknown IO exception caught: " + ex);
      }
  }

  /**
   *
   */

  private static void scanGroups()
  {
    // and the group file

    inStream = null;
    done = false;

    try
      {
	inStream = new FileInputStream("input/group");
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find group");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inStream);

    Group.initTokenizer(tokens);

    System.out.println("Scanning group");

    while (!done)
      {
	try
	  {
	    groupObj = new Group();
	    done = groupObj.loadLine(tokens);

	    if (!done && groupObj.valid)
	      {
		groups.put(groupObj.name, groupObj);
		groupID.put(new Integer(groupObj.gid), groupObj);

		if (debug)
		  {
		    System.out.println("\n\n");
		    groupObj.display();
		  }
		else
		  {
		    System.out.print(".");
		  }
	      }
	  }
	catch (EOFException ex)
	  {
	    done = true;
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    try
      {
	inStream.close();
      }
    catch (IOException ex)
      {
	System.err.println("unknown IO exception caught: " + ex);
      }

    System.out.println("Done scanning group");

  }

  /**
   *
   */

  private static void registerUsers() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    DBEditObject current_obj;
    db_field current_field;
    pass_field p_field;
    User userObj;

    /* -- */

    System.err.println("\nCreating users in server: ");

    enum = users.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	userObj = (User) users.get(key);

	System.out.print("Creating " + key);

	current_obj = (DBEditObject) createObject(SchemaConstants.UserBase);
	invid = current_obj.getInvid();

	System.out.println(" [" + invid + "] ");

	userInvids.put(userObj.name, invid);

	// set the username
	    
	current_obj.setFieldValueLocal(SchemaConstants.UserUserName, key);

	// set the UID

	current_obj.setFieldValueLocal(userSchema.UID, new Integer(userObj.uid));

	// set the password

	p_field = (pass_field) current_obj.getField(SchemaConstants.UserPassword);
	p_field.setCryptPass(userObj.password);

	// set the fullname

	current_obj.setFieldValueLocal(userSchema.FULLNAME, userObj.fullname);

	// set the division

	current_obj.setFieldValueLocal(userSchema.CLASSIFICATION, userObj.classification);

	// set the room

	current_obj.setFieldValueLocal(userSchema.ROOM, userObj.room);

	// set the office phone

	current_obj.setFieldValueLocal(userSchema.OFFICEPHONE, userObj.officePhone);

	// set the home phone

	current_obj.setFieldValueLocal(userSchema.HOMEPHONE, userObj.homePhone);

	// set the home directory

	current_obj.setFieldValueLocal(userSchema.HOMEDIR, userObj.directory);

	// set the shell

	current_obj.setFieldValueLocal(userSchema.LOGINSHELL, userObj.shell);
      }
  }

  /**
   *
   */

  private static void registerGroups() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    DBEditObject current_obj;
    db_field current_field, current_field2;
    User userObj;

    /* -- */

    enum = groups.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	groupObj = (Group) groups.get(key);
	
	System.out.print("Creating " + key);
	
	current_obj = (DBEditObject) createObject((short) 257);	// base 257 is for groups
	objInvid = current_obj.getInvid();
	
	System.out.println(" [" + objInvid + "]");

	groupInvid.put(groupObj.name, objInvid);

	// set the group name
	    
	current_obj.setFieldValueLocal(groupSchema.GROUPNAME, key);

	// set the GID

	current_obj.setFieldValueLocal(groupSchema.GID, new Integer(groupObj.gid));

	// set the password

	current_obj.setFieldValueLocal(groupSchema.PASSWORD, groupObj.password);

	// add users

	current_field = current_obj.getField(groupSchema.USERS);

	String username;

	for (int i = 0; i < groupObj.users.size(); i++)
	  {
	    username = (String) groupObj.users.elementAt(i);

	    if (username != null)
	      {
		invid = (Invid) userInvids.get(username);

		if (invid != null)
		  {
		    System.err.println("Add " + username + ", [" + invid.toString()+"]");
		    ((DBField) current_field).addElementLocal(invid);
		  }
		else
		  {
		    System.err.println("Error, user '" + username + "' listed in group " + 
				       groupObj.name + 
				       " doesn't really exist");
		  }

		// does this user have this group specified as his/her home group?
		// if so, add him/her to the home users list as well.

		userObj = (User) users.get(username);

		if (userObj == null)
		  {
		    System.err.println("Error, user '" + username + "' listed in group " + 
				       groupObj.name + 
				       " doesn't really exist");
		  }
		else
		  {
		    if (userObj.gid == groupObj.gid)
		      {
		    System.err.println("-- home group add " + username);
		    current_field2 = current_obj.getField(groupSchema.HOMEUSERS);
		    ((DBField) current_field2).addElementLocal(invid);
		      }
		  }
	      }
	    else
	      {
		System.err.println("null username");
	      }
	  }
      }
  }

  private static db_object createObject(short type)
  {
    return my_client.session.create_db_object(type).getObject();
  }
}  

/*------------------------------------------------------------------------------
                                                                           class
                                                              directLoaderClient

------------------------------------------------------------------------------*/

/**
 * directLoaderClient does all the heavy lifting to connect the server with the client, and
 * provides callbacks that the server can use to notify the client when something
 * happens.
 */

class directLoaderClient extends UnicastRemoteObject implements Client {

  protected Server server = null;
  protected String username = Ganymede.rootname;
  protected String password = Ganymede.defaultrootpassProperty;
  protected GanymedeSession session = null;

  /* -- */

  public directLoaderClient(Server server) throws RemoteException
  {
    super();    // UnicastRemoteObject can throw RemoteException 

    this.server = server;

    if (server == null)
      {
	System.err.println("Error, null server in directLoaderClient constructor");
      }

    System.err.println("Initializing directLoaderClient object");

    try
      {
	session = (GanymedeSession) server.login(this);

	if (session == null)
	  {
	    System.err.println("Couldn't log in to server.  Bad user/pass?");

	    System.exit(0);
	  }

	//	Vector invids = new Vector();
	//
	//	invids.addElement(new Invid((short)0,0)); // supergash owner group object
	//
	//	session.setDefaultOwner(invids);

	System.out.println("logged in");
      }
    catch (RemoteException ex)
      {
	System.err.println("RMI Error: Couldn't log in to server.\n");

	ex.printStackTrace();

	System.exit(0);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");

	ex.printStackTrace();

	System.exit(0);
      }
    catch (Exception ex)
      {
	System.err.println("Got some other exception: " + ex);
      }

    System.err.println("Got session");

    // turn off oversight whilst we load

    session.enableOversight(false);
  }

  /**
   * Calls the logout() method on the Session object
   */
  public void disconnect() throws RemoteException
  {
    session.logout();
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
    System.err.println("Server forced disconnect: " + reason);
    System.exit(0);
  }
}
