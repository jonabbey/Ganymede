/*
   directLoader.java

   Ganymede directLoader module

   This module is intended to be bound to the bulk of the Ganymede
   server and automatically create a whole bunch of objects
   to initialize the database from GASH data.

   --

   Created: 20 October 1997
   Version: $Revision: 1.15 $ %D%
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

import arlut.csd.ganymede.custom.userSchema;
import arlut.csd.ganymede.custom.personSchema;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    directLoader

------------------------------------------------------------------------------*/

public class directLoader {

  static GanymedeServer my_server;
  static Session my_session;
  static directLoaderClient my_client;

  static Hashtable adminUsers = new Hashtable();
  static Vector ownerGroups = new Vector();
  static Hashtable personaMap = new Hashtable();

  static Hashtable persons = new Hashtable();

  static Hashtable users = new Hashtable();
  static Hashtable userInvids = new Hashtable();

  static Vector externalAliases = new Vector();
  static Vector mailGroups = new Vector();
  static Hashtable userMail = new Hashtable();
  static Hashtable mailInvids = new Hashtable();

  static Hashtable groups = new Hashtable();
  static Hashtable groupID = new Hashtable();
  static Hashtable groupInvid = new Hashtable();

  static Hashtable userNetgroup = new Hashtable();
  static Hashtable systemNetgroup = new Hashtable();

  static Hashtable systemTypes = new Hashtable();
  static Hashtable systemTypeInvids = new Hashtable();

  static Hashtable systemInvid = new Hashtable();

  static Hashtable volumes = new Hashtable(); // map volume name -> Volume object
  static Hashtable volumeInvids = new Hashtable(); // map volume name -> invid

  static Vector maps = new Vector();
  static Hashtable mapInvids = new Hashtable();	// map map name -> invid

  static Vector mapEntries = new Vector();

  static Invid gashadminPermInvid;		// the standard GASH admin privileges

  static FileInputStream inStream = null;
  static boolean done = false;

  static User userObj;
  static Group groupObj;
  static UserNetgroup uNetObj;
  static SystemNetgroup sNetObj;

  static SystemLoader sysLoader;

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

    System.err.println("---------------------------------------- Initiating GASH file scan --------------------");

    scanOwnerGroups();
    scanUsers();
    scanEmail();
    scanGroups();
    scanUserNetgroups();
    scanSystemTypes();
    scanSystems();
    scanSystemNetgroups();
    scanAutomounter();

    System.err.println("\n---------------------------------------- Completed GASH file scan --------------------\n");

    // Okay.. at this point we've scanned the files we need to scan..
    // now we initialize the database module and create the objects

    try
      {
	my_client.session.openTransaction("GASH directLoader");

	String key;
	Invid invid, objInvid;
	Enumeration enum;
	OwnerGroup ogRec;
	db_object current_obj;
	db_field current_field;
	pass_field p_field;
	DBObjectBase base;
	ReturnVal retVal;

	/* -- */

	System.err.println("\nCreating GASH standard permission matrix");

	my_client.session.checkpoint("GASHAdmin");

	current_obj = createObject(SchemaConstants.PermBase);
	gashadminPermInvid = current_obj.getInvid();

	System.err.println("Trying to create a new GASHAdmin perm object: " + gashadminPermInvid.toString());

	retVal = current_obj.setFieldValue(SchemaConstants.PermName, "GASH Admin");

	if (retVal != null && !retVal.didSucceed())
	  {
	    // the gash privilege matrix object is already in the schema.. rollback

	    my_client.session.rollback("GASHAdmin");

	    // note that QueryDataNode uses the label as the
	    // comparator if you don't provide a field id

	    Query q = new Query(SchemaConstants.PermBase, 
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

	    perm_field pf = (perm_field) current_obj.getField(SchemaConstants.PermMatrix);

	    PermEntry defPerm = new PermEntry(true, true, true, true);
	
	    pf.setPerm(SchemaConstants.UserBase, defPerm);
	    pf.setPerm((short) 279, defPerm); // person privs
	    pf.setPerm((short) 257, defPerm); // group privs
	    pf.setPerm((short) 263, defPerm); // systems privs
	    pf.setPerm((short) 267, defPerm); // I.P. network
	    pf.setPerm((short) 268, defPerm); // DNS domain
	    pf.setPerm((short) 264, defPerm); // DNS record
	    pf.setPerm((short) 265, defPerm); // System Interface
	    pf.setPerm((short) 266, defPerm); // I.P. Record
	    pf.setPerm((short) 271, defPerm); // Systems Netgroups
	    pf.setPerm((short) 270, defPerm); // User Netgroups
	    pf.setPerm((short) 269, defPerm); // room
	    pf.setPerm((short) 258, defPerm); // shell choices
	  }

	// now, the ownerGroups Vector has been loaded for us by the scanOwnerGroups() method.
	// go ahead and register owner groups for the set of differentiable owner prefixes
	// from the GASH admin_info file

	System.err.println("\nCreating ownergroups in server: ");

	enum = ownerGroups.elements();

	while (enum.hasMoreElements())
	  {
	    ogRec = (OwnerGroup) enum.nextElement();
	    System.out.print("Creating ownergroup:" + ogRec.prefix);

	    // create the owner group on the server

	    current_obj = createObject(SchemaConstants.OwnerBase);
	    invid = current_obj.getInvid();

	    // record the invid so that we can refer to this ownerbase later

	    if (invid == null)
	      {
		System.err.println("**** Complain!");
	      }

	    ogRec.setInvid(invid);

	    System.out.println(" [" + invid + "]");

	    // and set the name of the owner group.. using the prefix mask isn't
	    // great, but it will serve for now.  The administrators will be able to
	    // edit them through the client

	    current_obj.setFieldValue(SchemaConstants.OwnerNameField, ogRec.prefix);
	  }

	commitTransaction();	// lock in perm object

	System.out.println("\nRegistering users\n");
	my_client.session.openTransaction("GASH directLoader");
	registerUsers();
	commitTransaction();

	my_client.session.openTransaction("GASH directLoader");
	registerPersons();
	commitTransaction();

	my_client.session.openTransaction("GASH directLoader");
	registerEmail();
	commitTransaction();

	System.out.println("\nRegistering groups\n");
	my_client.session.openTransaction("GASH directLoader");
	registerGroups();
	commitTransaction();

	System.out.println("\nRegistering user netgroups\n");
	my_client.session.openTransaction("GASH directLoader");
	registerUserNetgroups();
	commitTransaction();

	System.out.println("\nRegistering System Types\n");
	my_client.session.openTransaction("GASH directLoader");
	registerSystemTypes();
	commitTransaction();

	System.out.println("\nRegistering systems\n");
	my_client.session.openTransaction("GASH directLoader");
	registerSystems();
	commitTransaction();

	System.out.println("\nRegistering system netgroups\n");
	my_client.session.openTransaction("GASH directLoader");
	registerSystemNetgroups();
	commitTransaction();

	System.out.println("\nRegistering automounter configuration\n");
	my_client.session.openTransaction("GASH directLoader");
	registerAutomounter();
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
  // the following methods handle the parsing of a particular GASH file
  //

  /*----------------------------------------------------------------------------*/

  private static void scanOwnerGroups()
  {
    // scan the admin_info file, coalesce a group of owner groups

    try
      {
	inStream = new FileInputStream("input/admin_info");
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find admin_info");
	done = true;
      }

    Admin adminObj;
    StreamTokenizer tokens = new StreamTokenizer(inStream);

    Admin.initTokenizer(tokens);

    System.out.println("Scanning admin_info");

    while (!done)
      {
	try
	  {
	    adminObj = new Admin();
	    done = adminObj.loadLine(tokens);

	    if (adminObj.name != null)
	      {
		if (adminObj.name.equals(Ganymede.rootname))
		  {
		    System.err.println("Skipping over supergash");
		    continue;
		  }

		boolean found = false;
		OwnerGroup ogRec = null;

		// go through the existing owner group definitions, see if
		// there is one that matches the new admin object.. if so,
		// just remember that that user is a member of that owner
		// group
		
		for (int i = 0; !found && i < ownerGroups.size(); i++)
		  {
		    ogRec = (OwnerGroup) ownerGroups.elementAt(i);
		    
		    if (ogRec.compatible(adminObj))
		      {
			ogRec.addAdmin(adminObj.name, adminObj.password);
			found = true;
		      }
		  }
		
		if (!found)
		  {
		    // create a new owner group

		    ogRec = new OwnerGroup(adminObj);
		    ogRec.addAdmin(adminObj.name, adminObj.password);
		    ownerGroups.addElement(ogRec);
		  }
		
		// System.out.println("Putting " + adminObj.name + " : " + ogRec);

		adminUsers.put(adminObj.name, ogRec);

		if (!done)
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

    System.err.println("\nOwner Groups identified:");

    for (int i = 0; i < ownerGroups.size(); i++)
      {
	System.err.println(ownerGroups.elementAt(i));
      }
  }

  /**
   *
   */

  private static void scanUsers()
  {
    // now process the user_info file

    try
      {
	inStream = new FileInputStream("input/user_info");
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find user_info");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inStream);

    User.initTokenizer(tokens);

    System.out.println("Scanning user_info");

    while (!done)
      {
	try
	  {
	    userObj = new User();
	    done = userObj.loadLine(tokens);

	    if (!done)
	      {
		System.out.print(".");
		users.put(userObj.name, userObj);
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

    System.out.println();
    System.out.println("Done scanning user_info");

    try
      {
	inStream.close();
      }
    catch (IOException ex)
      {
	System.err.println("unknown IO exception caught: " + ex);
      }
  }

  private static void scanEmail()
  {
    // now process the aliases_info file
    
    BufferedReader in = null;
    String line;

    try
      {
	in = new BufferedReader(new FileReader("input/aliases_info"));
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find aliases_info");
	done = true;
      }

    System.out.println("Scanning aliases_info");

    while (!done)
      {
	try
	  {
	    line = in.readLine();

	    if (line == null)
	      {
		done = true;
	      }
	    else
	      {
		System.err.println("x");

		switch (line.charAt(0))
		  {
		  case '<':
		    ExternMail z = new ExternMail(line);
		    
		    externalAliases.addElement(z);
		    
		    System.err.println(z);
		    break;
		    
		  case ':':
		    MailGroup y = new MailGroup(line);
		    
		    mailGroups.addElement(y);
		    
		    System.err.println(y);
		    break;
		    
		  default:
		    UserMail x = new UserMail(line);
		    
		    userMail.put(x.userName, x);
		    
		    System.err.println(x);
		    break;
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

    System.out.println();
    System.out.println("Done scanning aliases_info");

    try
      {
	in.close();
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
	inStream = new FileInputStream("input/group_info");
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find group_info");
	done = true;
      }


    StreamTokenizer tokens = new StreamTokenizer(inStream);

    Group.initTokenizer(tokens);

    System.out.println("Scanning group_info");

    while (!done)
      {
	try
	  {
	    groupObj = new Group();
	    done = groupObj.loadLine(tokens);

	    if (!done)
	      {
		System.out.print(".");
		groups.put(groupObj.name, groupObj);
		groupID.put(new Integer(groupObj.gid), groupObj);
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

    System.out.println();
    System.out.println("Done scanning group_info");

  }

  /**
   *
   */

  private static void scanUserNetgroups()
  {
    inStream = null;
    done = false;

    try
      {
	inStream = new FileInputStream("input/netgroup");
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find netgroup");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inStream);

    UserNetgroup.initTokenizer(tokens);

    System.out.println("Scanning netgroup for user entries");

    while (!done)
      {
	try
	  {
	    uNetObj = new UserNetgroup();
	    done = uNetObj.loadLine(tokens);

	    //	    uNetObj.display();

	    if (!done)
	      {
		System.out.print(".");
		userNetgroup.put(uNetObj.netgroup_name, uNetObj);
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

    System.out.println();
    System.out.println("Done scanning netgroup for user objects");
  }

  /**
   *
   */

  private static void scanSystemTypes()
  {
    System.out.println("\nScanning System Types\n");

    inStream = null;
    done = false;

    try
      {
	inStream = new FileInputStream("input/internet_assignment");
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find system types file");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inStream);

    SystemType.initTokenizer(tokens);

    SystemType st;

    while (!done)
      {
	try
	  {
	    st = new SystemType();
	    done = st.loadLine(tokens);

	    if (!done)
	      {
		st.display();
		systemTypes.put(st.name, st);
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

    System.out.println();
    System.out.println("Done scanning internet_assignment file for system type objects");
  }

  /**
   *
   */

  private static void scanSystems()
  {
    System.out.println("\nScanning Systems\n");

    try
      {
	sysLoader = new SystemLoader("input/hosts_info");
      }
    catch (IOException ex)
      {
	throw new RuntimeException("io exception in systemLoader constructor: " + ex);
      }
  }

  /**
   *
   */

  private static void scanSystemNetgroups()
  {
    inStream = null;
    done = false;

    try
      {
	inStream = new FileInputStream("input/netgroup");
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find netgroup");
	done = true;
      }

    StreamTokenizer tokens = new StreamTokenizer(inStream);

    SystemNetgroup.initTokenizer(tokens);

    System.out.println("\nScanning netgroup for system entries\n");

    while (!done)
      {
	try
	  {
	    sNetObj = new SystemNetgroup();
	    done = sNetObj.loadLine(tokens);

	    //	    sNetObj.display();
	    //	    System.out.println();

	    if (!done)
	      {
		System.out.print(".");
		systemNetgroup.put(sNetObj.netgroup_name, sNetObj);
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

    System.out.println();
    System.out.println("Done scanning netgroup for system objects");
  }

  /**
   *
   */

  private static void scanAutomounter()
  {
    BufferedReader in = null;
    String line;
    Volume v;
    MapEntry m;

    /* -- */

    try
      {
	in = new BufferedReader(new FileReader("input/auto.vol"));
	done = false;
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find input/auto.vol");
	done = true;
      }

    System.out.println("Scanning input/auto.vol");

    while (!done)
      {
	try
	  {
	    line = in.readLine();

	    if (line == null)
	      {
		done = true;
	      }
	    else
	      {
		v = new Volume(line);
		volumes.put(v.volumeName, v);

		System.err.println("Created: " + v);
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

    System.out.println("\nDone scanning auto.vol");

    System.out.println("\nScanning directory for auto.home.* files\n");

    File dir = new File("input");

    if ((dir == null) || !dir.isDirectory())
      {
	throw new RuntimeException("Couldn't open input directory");
      }

    String[] sAry = dir.list(new FilenameFilter()
			     {
			       public boolean accept(File dir,
						     String name)
				 {
				   return (name.startsWith("auto.home."));
				 }
			     }
			     );

    for (int i = 0; i < sAry.length; i++)
      {
	System.out.println("Scanning " + sAry[i]);

	maps.addElement(sAry[i]);

	try
	  {
	    in = new BufferedReader(new FileReader("input/" + sAry[i]));
	    done = false;
	  }
	catch (FileNotFoundException ex)
	  {
	    System.err.println("Couldn't open input/" + sAry[i]);
	    done = true;
	  }

	while (!done)
	  {
	    try
	      {
		line = in.readLine();

		if (line == null)
		  {
		    done = true;
		  }
		else
		  {
		    m = new MapEntry(sAry[i], line);
		    mapEntries.addElement(m);

		    System.err.println("Created: " + m);
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

	System.out.println("\nDone scanning " + sAry[i]);
      }
  }

  /**
   *
   */

  private static void registerUserNetgroups() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    db_object current_obj;
    db_field current_field;
    Hashtable netHash = new Hashtable();
    ReturnVal retVal;

    /* -- */

    enum = userNetgroup.keys();
    
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	uNetObj = (UserNetgroup) userNetgroup.get(key);

	System.out.print("Creating " + key);

	current_obj = createObject((short) 270);	// base 270 is for user netgroups
	objInvid = current_obj.getInvid();

	System.out.println(" [" + objInvid + "]");

	current_obj.setFieldValue((short) 256, key); // netgroup name
	netHash.put(key, objInvid);

	// now we have to put the users in

	current_field = current_obj.getField((short) 257); // users

	Vector users = uNetObj.users;
	String username;

	for (int i = 0; i < users.size(); i++)
	  {
	    username = (String) users.elementAt(i);

	    if (username.equals("-"))
	      {
		// empty marker

		continue;
	      }

	    if (username != null)
	      {
		invid = (Invid) userInvids.get(username);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + username + ", [" + invid.toString()+"]");
		    current_field.addElement(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }
	      }
	    else
	      {
		System.err.println("null username");
	      }
	  }

	// loop through our owner groups, add this netgroup to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchMask(key))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		f.addElement(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }

	System.out.println();
      }

    // register user netgroups in the server, part 2.. the sub netgroups

    enum = userNetgroup.keys();
	
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	uNetObj = (UserNetgroup) userNetgroup.get(key);

	System.out.print("Loading subnetgroups for " + key);

	objInvid = (Invid) netHash.get(key);
	current_obj = editObject(objInvid);

	System.out.println(" [" + objInvid + "]");

	// we have to put the sub netgroups in

	current_field = current_obj.getField((short) 258); // member netgroups

	Vector subnets = uNetObj.subnetgroups;
	String subName;
	boolean ok = false;

	for (int i = 0; i < subnets.size(); i++)
	  {
	    subName = (String) subnets.elementAt(i);

	    if (subName != null)
	      {
		invid = (Invid) netHash.get(subName);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + subName + ", [" + invid.toString()+"]");

		    if (!ok)
		      {
			retVal = current_field.addElement(invid);

			ok = (retVal == null || retVal.didSucceed());
		      }
		  }
		else
		  {
		    System.err.println(key + ": *** null invid attempting to add " + subName);
		  }
	      }
	    else
	      {
		System.err.println("null subnet name");
	      }
	  }
	
	if (!ok && subnets.size()>0)
	  {
	    System.err.println("Not successful while processing subnetgroups for " + key);
	  }
      }
  }

  /**
   *
   */

  private static void registerUsers() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    db_object current_obj;
    db_field current_field;
    pass_field p_field;
    User userObj;
    ReturnVal retVal;

    /* -- */

    System.err.println("\nCreating users in server: ");

    enum = users.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	userObj = (User) users.get(key);

	System.out.print("Creating " + key);

	current_obj = createObject(SchemaConstants.UserBase);
	invid = current_obj.getInvid();

	userObj.userInvid = invid;

	System.out.print(" [" + invid + "] ");

	userInvids.put(userObj.name, invid);

	// set the username
	    
	current_obj.setFieldValue(userSchema.USERNAME, key);

	// enable unix

	current_obj.setFieldValue(userSchema.UNIX, new Boolean(true));

	// enable unix login if it wasn't an inactivated GASH account

	if (userObj.password.equals("*"))
	  {
	    current_obj.setFieldValue(userSchema.UNIXENABLED, new Boolean(false));
	  }
	else
	  {
	    current_obj.setFieldValue(userSchema.UNIXENABLED, new Boolean(true));
	  }

	// assume privileged for now

	current_obj.setFieldValue(userSchema.PRIVILEGED, new Boolean(true));

	// set the UID

	current_obj.setFieldValue(userSchema.UID, new Integer(userObj.uid));

	// set the password

	p_field = (pass_field) current_obj.getField(userSchema.PASSWORD);
	p_field.setCryptPass(userObj.password);

	// set the home directory

	current_obj.setFieldValue(userSchema.HOMEDIR, userObj.directory);

	// set the shell

	current_obj.setFieldValue(userSchema.LOGINSHELL, userObj.shell);

	// register email aliases for this user

	UserMail aliasInfo = (UserMail) userMail.get(key);

	if (aliasInfo != null)
	  {
	    db_field sf = current_obj.getField((short) 267);

	    String tmpStr;

	    for (int i = 0; i < aliasInfo.aliases.size(); i++)
	      {
		tmpStr = (String) aliasInfo.aliases.elementAt(i);

		if (!tmpStr.equals(key))
		  {
		    sf.addElement(aliasInfo.aliases.elementAt(i));
		  }
	      }

	    // set the signature alias

	    current_obj.setFieldValue(userSchema.SIGNATURE, aliasInfo.aliases.elementAt(0));

	    // set the email targets

	    sf = current_obj.getField(userSchema.EMAILTARGET);

	    // the setValue logic for the user object will have created
	    // a default email target for us.. we don't want it.

	    while (sf.size() != 0)
	      {
		retVal = sf.deleteElement(0);

		if (retVal != null && !retVal.didSucceed())
		  {
		    break;	// oh, well.
		  }
	      }

	    for (int i = 0; i < aliasInfo.targets.size(); i++)
	      {
		sf.addElement(aliasInfo.targets.elementAt(i));
	      }
	  }

	// loop through our owner groups, add this user to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchUID(userObj.uid))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		f.addElement(invid); // add this user
		System.out.print(" " + ogRec.prefix);
	      }
	  }

	System.out.println();

	// now, is this user an admin?

	ogRec = (OwnerGroup) adminUsers.get(key);

	if (ogRec != null)
	  {
	    System.out.print("User " + key + " is a GASH admin.. creating persona object ");

	    db_object newPersona = createObject(SchemaConstants.PersonaBase);
	    Invid personaInvid = newPersona.getInvid();

	    System.out.println("[" + personaInvid + "]");

	    newPersona.setFieldValue(SchemaConstants.PersonaAssocUser, invid);

	    newPersona.setFieldValue(SchemaConstants.PersonaNameField, key + ":GASH Admin");

	    pass_field passField = (pass_field) newPersona.getField(SchemaConstants.PersonaPasswordField);
	    passField.setCryptPass(ogRec.password(key));

	    //	    newPersona.setFieldValue(SchemaConstants.PersonaPasswordField,ogRec.password(key));

	    newPersona.setFieldValue(SchemaConstants.PersonaAdminConsole, new Boolean(true));

	    newPersona.setFieldValue(SchemaConstants.PersonaAdminPower, new Boolean(false));

	    db_field personaField = newPersona.getField(SchemaConstants.PersonaGroupsField);
	    personaField.addElement(ogRec.getInvid());

	    // this is a GASH admininstrator, so give it the GASH perm matrix

	    personaField = newPersona.getField(SchemaConstants.PersonaPrivs);
	    personaField.addElement(gashadminPermInvid);
	  }

	// we want to record a chain of user accounts linked by social security
	// number

	if (!persons.containsKey(userObj.socialsecurity))
	  {
	    Vector x = new Vector();
	    x.addElement(userObj);

	    persons.put(userObj.socialsecurity, x);
	  }
	else
	  {
	    Vector x = (Vector) persons.get(userObj.socialsecurity);

	    x.addElement(userObj);
	  }
      }
  }

  /**
   *
   */

  private static void registerPersons() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    db_object current_obj;
    db_field current_field;
    pass_field p_field;
    Person person;
    User userObj, localUser;
    Vector accounts;;

    /* -- */

    System.err.println("\nCreating persons in server: ");

    enum = persons.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	accounts = (Vector) persons.get(key);

	if (accounts.size() == 1)
	  {
	    // we've just got one account for this social security key..
	    // we'll use this info for the person record

	    userObj = (User) accounts.elementAt(0);
	  }
	else
	  {
	    // we've got multiple accounts for this social security key..
	    // we want the one that corresponds to a gash admin

	    userObj = null;

	    for (int i = 0; i < accounts.size(); i++)
	      {
		localUser = (User) accounts.elementAt(i);

		if (adminUsers.containsKey(localUser.name))
		  {
		    userObj = localUser;
		    break;
		  }
	      }
	  }

	if (userObj == null)
	  {
	    System.err.println("*** Complain!  Couldn't find primary user: " + key);
	  }
	else
	  {
	    person = new Person(userObj);

	    System.out.println("-- Creating person for user " + userObj.name);

	    current_obj = createObject((short) 279);
	    current_obj.setFieldValue(personSchema.LASTNAME, person.lastName);
	    current_obj.setFieldValue(personSchema.FIRSTNAME, person.firstName);
	    current_obj.setFieldValue(personSchema.HOMEPHONE, person.homePhone);
	    current_obj.setFieldValue(personSchema.OFFICEPHONE, person.officePhone);
	    current_obj.setFieldValue(personSchema.CELLPHONE, person.cellPhone);
	    current_obj.setFieldValue(personSchema.FAX, person.faxNumber);
	    current_obj.setFieldValue(personSchema.DIVISION, person.division);
	    current_obj.setFieldValue(personSchema.ROOM, person.room);
	    current_obj.setFieldValue(personSchema.PAGER, person.pagerNumber);
	    current_obj.setFieldValue(personSchema.BADGE, person.badgeNumber);
	    current_obj.setFieldValue(personSchema.EMPLOYEETYPE, person.employeeType);

	    // record the accounts.. the schema will automatically back-link these for
	    // us.

	    invid_field invids = (invid_field) current_obj.getField(personSchema.ACCOUNTS);

	    for (int i = 0; i < accounts.size(); i++)
	      {
		localUser = (User) accounts.elementAt(i);
		invids.addElement(localUser.userInvid);
	      }
	  }
      }
  }

  /**
   *
   */

  private static void registerEmail() throws RemoteException
  {
    
    ExternMail mailRec;
    MailGroup mailGroup;
    db_object current_obj;
    db_field current_field;
    Invid objInvid, targetInvid;
    OwnerGroup ogRec;
    String temp;

    /* -- */

    System.err.println("Registering email objects");

    // register external email references

    System.err.println("External References");

    for (int i = 0; i < externalAliases.size(); i++)
      {
	mailRec = (ExternMail) externalAliases.elementAt(i);

	current_obj = createObject((short) 275);	// base 275 is for Email Redirect objects
	objInvid = current_obj.getInvid();

	// remember that we have this external record as a possible target for
	// other things

	mailInvids.put(mailRec.externalName.toLowerCase(), objInvid);

	current_obj.setFieldValue((short) 256, mailRec.externalName);

	// set targets

	current_field = current_obj.getField((short) 257);

	for (int j = 0; j < mailRec.targets.size(); j++)
	  {
	    current_field.addElement(mailRec.targets.elementAt(j));
	  }

	// set aliases

	current_field = current_obj.getField((short) 258);

	for (int j = 0; j < mailRec.aliases.size(); j++)
	  {
	    String tmp = (String) mailRec.aliases.elementAt(j);

	    // we don't care about signature aliases for
	    // external references, so just leave out the
	    // label for the user in the aliases list
	    
	    if (!tmp.equals(mailRec.externalName))
	      {
		current_field.addElement(tmp);
	      }
	  }

	// loop through our owner groups, add this external mail rec to the appropriate owner groups

	for (int j = 0; j < ownerGroups.size(); j++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(j);

	    if (ogRec.matchMask(mailRec.ownerCode))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		f.addElement(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }
      }

    // register email groups

    // step 1.. create all the groups, hash the names to invids

    System.err.println("Mail Lists.. step 1");

    for (int i = 0; i < mailGroups.size(); i++)
      {
	mailGroup = (MailGroup) mailGroups.elementAt(i);

	current_obj = createObject((short) 274);	// base 274 is for Email List objects
	objInvid = current_obj.getInvid();

	mailInvids.put(mailGroup.listName.toLowerCase(), objInvid);

	current_obj.setFieldValue((short) 256, mailGroup.listName);

	// set external targets

	current_field = current_obj.getField((short) 258);

	for (int j = 0; j < mailGroup.targets.size(); j++)
	  {
	    temp = (String) mailGroup.targets.elementAt(j);

	    if (temp.indexOf('@') != -1)
	      {
		current_field.addElement(mailGroup.targets.elementAt(j));
	      }
	  }

	// loop through our owner groups, add this mail list to the appropriate owner groups

	for (int j = 0; j < ownerGroups.size(); j++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(j);

	    if (ogRec.matchMask(mailGroup.ownerCode))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		f.addElement(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }
      }

    // okay, now we can loop through and set the internal targets

    System.err.println("Mail Lists.. step 2");

    for (int i = 0; i < mailGroups.size(); i++)
      {
	mailGroup = (MailGroup) mailGroups.elementAt(i);

	current_obj = editObject((Invid) mailInvids.get(mailGroup.listName.toLowerCase()));

	// set internal targets

	current_field = current_obj.getField((short) 257);

	for (int j = 0; j < mailGroup.targets.size(); j++)
	  {
	    temp = (String) mailGroup.targets.elementAt(j);

	    if (temp.indexOf('@') == -1)
	      {
		targetInvid = (Invid) mailInvids.get(temp.toLowerCase());

		if (targetInvid != null)
		  {
		    current_field.addElement(targetInvid);
		  }
		else
		  {
		    targetInvid = (Invid) userInvids.get(temp.toLowerCase());

		    if (targetInvid != null)
		      {
			current_field.addElement(targetInvid);
		      }
		    else
		      {
			System.err.println("Can't find email target reference for " + temp);
		      }
		  }
	      }
	  }
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
    OwnerGroup ogRec;
    db_object current_obj;
    db_field current_field, current_field2;
    User userObj;

    /* -- */

    enum = groups.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	groupObj = (Group) groups.get(key);
	
	System.out.print("Creating " + key);
	
	current_obj = createObject((short) 257);	// base 257 is for groups
	objInvid = current_obj.getInvid();
	
	System.out.print(" [" + objInvid + "]");

	groupInvid.put(groupObj.name, objInvid);

	// set the group name
	    
	current_obj.setFieldValue((short) 256, key);

	// set the GID

	current_obj.setFieldValue((short) 258, new Integer(groupObj.gid));

	// set the password

	current_obj.setFieldValue((short) 257, groupObj.password);

	// set the description

	current_obj.setFieldValue((short) 259, groupObj.description);

	// set the contract info

	current_obj.setFieldValue((short) 260, groupObj.contract);

	// add users

	current_field = current_obj.getField((short) 261);

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
		    current_field.addElement(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }

		// does this user have this group specified as his/her home group?
		// if so, add him/her to the home users list as well.

		userObj = (User) users.get(username);

		if (userObj.gid == groupObj.gid)
		  {
		    System.err.println("-- home group add " + username);
		    current_field2 = current_obj.getField((short) 262);
		    current_field2.addElement(invid);
		  }
	      }
	    else
	      {
		System.err.println("null username");
	      }
	  }

	// loop through our owner groups, add this group to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchGID(groupObj.gid))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		f.addElement(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }

	System.out.println();
      }
  }

  /**
   *
   */

  private static void registerSystemTypes() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    SystemType st;
    db_object current_obj;
    db_field current_field;

    /* -- */

    enum = systemTypes.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	st = (SystemType) systemTypes.get(key);
	
	System.out.print("Creating " + key);
	
	current_obj = createObject((short) 272);	// base 272 is for system types
	objInvid = current_obj.getInvid();
	
	System.out.print(" [" + objInvid + "]");

	systemTypeInvids.put(st.name, objInvid);

	// set the group name
	    
	current_obj.setFieldValue((short) 256, key);

	// set the start mark

	current_obj.setFieldValue((short) 257, new Integer(st.start));

	// set the end mark

	current_obj.setFieldValue((short) 258, new Integer(st.end));

	// set the associated user required flag

	current_obj.setFieldValue((short) 259, new Boolean(st.requireUser));
      }
  }

  /**
   *
   */

  private static void registerSystems() throws RemoteException
  {
    String key;
    Invid invid, dnsInvid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    db_object current_obj;
    db_field current_field;
    Hashtable roomHash = new Hashtable();
    Invid roomInvid;

    system sysObj;

    /* -- */

    // create an arlut.utexas.edu DNS domain entry

    current_obj = createObject((short) 268); // base 268 is for DNS domain
    dnsInvid = current_obj.getInvid();

    current_obj.setFieldValue((short) 257, "arlut.utexas.edu");

    enum = sysLoader.systems.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	sysObj = (system) sysLoader.systems.get(key);
	
	System.out.print("Creating " + key);
	
	current_obj = createObject((short) 263);	// base 263 is for systems
	objInvid = current_obj.getInvid();
	
	System.out.print(" [" + objInvid + "]");

	systemInvid.put(sysObj.systemName, objInvid);

	// set the system name
	    
	current_obj.setFieldValue((short) 261,key);

	// set the DNS domain
	    
	current_obj.setFieldValue((short) 263, dnsInvid);

	// set the room

	// this is an invid.. need to see if we've created the room yet,
	// if not, create then set the invid

	roomInvid = (Invid) roomHash.get(sysObj.room);

	if (roomInvid == null)
	  {
	    db_object room_obj = createObject((short) 269);	// base 269 is for rooms
	    roomInvid = room_obj.getInvid();

	    // label the room with the room number

	    room_obj.setFieldValue((short) 256, sysObj.room);

	    // remember the room

	    roomHash.put(sysObj.room, roomInvid);
	  }

	current_obj.setFieldValue((short) 264, roomInvid);

	// set the type

	Invid typeInvid = (Invid) systemTypeInvids.get(sysObj.type);

	if (typeInvid == null)
	  {
	    System.err.println("\n\n***** No such type found: " + sysObj.type + "\n\n");
	  }

	current_obj.setFieldValue((short) 266, typeInvid);

	// set the manu

	current_obj.setFieldValue((short) 257, sysObj.manu);

	// set the model

	current_obj.setFieldValue((short) 258, sysObj.model);

	// set the os

	current_obj.setFieldValue((short) 256, sysObj.os);

	// set the user

	if (sysObj.user != null && !sysObj.user.equals(""))
	  {
	    Invid userInv = (Invid) userInvids.get(sysObj.user);

	    if (userInv != null)
	      {
		current_obj.setFieldValue((short) 267, userInv);
	      }
	  }

	// set the aliases

	current_field = current_obj.getField((short) 262);

	for (int i = 0; i < sysObj.aliases.size(); i++)
	  {
	    current_field.addElement(sysObj.aliases.elementAt(i));
	  }

	// set the interfaces

	current_field = current_obj.getField((short) 260);

	Invid intInvid;
	interfaceObj iO;
	db_object interfaceRef;
	db_field interfaceField;
	
	Invid ipInvid;
	db_object ipRec;
	db_field ipField;

	Invid dInvid;
	db_object dRec;
	db_field dField;

	for (int i = 0; i < sysObj.interfaces.size(); i++)
	  {
	    iO = (interfaceObj) sysObj.interfaces.elementAt(i);

	    intInvid = ((invid_field) current_field).createNewEmbedded().getInvid();
	    interfaceRef = editObject(intInvid);

	    // set the Ethernet Info for this Interface

	    interfaceRef.setFieldValue((short) 256, iO.Ether);

	    // set the IP address for this interface

	    // step 1: create an ip record

	    interfaceField = interfaceRef.getField((short) 257);
	    ipInvid = ((invid_field) interfaceField).createNewEmbedded().getInvid();

	    // we've created an ip record.. let's get an edit reference
	    // to it

	    ipRec = editObject(ipInvid);

	    ipRec.setFieldValue((short) 257, iO.IP); // ip address

	    // if the system has a single interface record in GASH, it's
	    // really a single interface system, and we don't need to
	    // have any (redundant) dns information recorded here.

	    if (sysObj.interfaces.size() > 1)
	      {
		// step 2: create the dns record for this i.p. address

		ipField = ipRec.getField((short) 256); // dns record

		dInvid = ((invid_field) ipField).createNewEmbedded().getInvid();

		// we've created a dns record.. get an edit reference

		dRec = editObject(dInvid);

		// set the name of the interface if this isn't a single-interface
		// systems (single interface systems don't have distinct interface
		// names in GASH)
		
		if (iO.interfaceName != null && !iO.interfaceName.equals(""))
		  {
		    dRec.setFieldValue((short)257, iO.interfaceName);	// dns name
		  }
		
		dField = dRec.getField((short)258);	// aliases
		
		for (int j = 0; j < iO.aliases.size(); j++)
		  {
		    dField.addElement(iO.aliases.elementAt(j));
		  }
		
		dRec.setFieldValue((short) 256, dnsInvid); // dns domain
	      }
	  }

	// loop through our owner groups, add this system to the appropriate admin/owner groups

	Hashtable owners = new Hashtable(); // to make sure we don't put this system in groups > once

	for (int i = 0; i < sysObj.admins.size(); i++)
	  {
	    String u = (String) sysObj.admins.elementAt(i);

	    ogRec = (OwnerGroup) adminUsers.get(u);

	    if (ogRec != null)
	      {
		if (owners.get(ogRec.getInvid()) == null)
		  {
		    // we haven't put the system into this owner group yet

		    db_object ownerGroup = editObject(ogRec.getInvid());
		    db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		    f.addElement(objInvid); // add this group
		    System.out.print(" " + ogRec.prefix);
		    owners.put(ogRec.getInvid(), ogRec);
		  }
	      }
	    else
	      {
		// assume u is a netgroup

		for (int j = 0; j < ownerGroups.size(); j++)
		  {
		    ogRec = (OwnerGroup) ownerGroups.elementAt(j);

		    if (owners.get(ogRec.getInvid()) != null)
		      {
			continue; // don't add redundantly
		      }

		    // if it's a netgroup name, let's see if this ogRec
		    // has edit permission based on the first few chars of the name 
		    
		    if (ogRec.matchMask(u))
		      {
			db_object ownerGroup = editObject(ogRec.getInvid());
			db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
			
			f.addElement(objInvid); // add this group.. this is a bi-directional link-up

			System.out.print(" " + ogRec.prefix);
			owners.put(ogRec.getInvid(), ogRec);
		      }
		  }
	      }
	  }

	System.out.println();
      }
  }

  /**
   *
   */

  private static void registerSystemNetgroups() throws RemoteException
  {
    String key;
    Invid invid, objInvid;
    Enumeration enum;
    OwnerGroup ogRec;
    db_object current_obj;
    db_field current_field;
    Hashtable netHash = new Hashtable();

    /* -- */

    enum = systemNetgroup.keys();
    
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	sNetObj = (SystemNetgroup) systemNetgroup.get(key);

	System.out.print("\nCreating " + key);

	current_obj = createObject((short) 271);	// base 271 is for system netgroups
	objInvid = current_obj.getInvid();

	System.out.println(" [" + objInvid + "]");

	current_obj.setFieldValue((short) 256, key); // netgroup name

	netHash.put(key, objInvid);

	// now we have to put the systems in

	current_field = current_obj.getField((short) 257); // systems

	Vector systems = sNetObj.systems;
	String systemname;

	for (int i = 0; i < systems.size(); i++)
	  {
	    systemname = (String) systems.elementAt(i);

	    if (systemname.equals("-"))
	      {
		// empty marker

		continue;
	      }

	    if (systemname != null)
	      {
		invid = (Invid) systemInvid.get(systemname);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + systemname + ", [" + invid.toString()+"]");
		    current_field.addElement(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }
	      }
	    else
	      {
		System.err.println("null systemname");
	      }
	  }

	// loop through our owner groups, add this netgroup to the appropriate owner groups

	for (int i = 0; i < ownerGroups.size(); i++)
	  {
	    ogRec = (OwnerGroup) ownerGroups.elementAt(i);

	    if (ogRec.matchMask(key))
	      {
		db_object ownerGroup = editObject(ogRec.getInvid());
		db_field f = ownerGroup.getField(SchemaConstants.OwnerObjectsOwned);
		    
		f.addElement(objInvid); // add this group
		System.out.print(" " + ogRec.prefix);
	      }
	  }
      }

    // register system netgroups in the server, part 2.. the sub netgroups

    enum = systemNetgroup.keys();
	
    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	sNetObj = (SystemNetgroup) systemNetgroup.get(key);

	System.out.print("Loading subnetgroups for " + key);

	objInvid = (Invid) netHash.get(key);
	current_obj = editObject(objInvid);

	System.out.println(" [" + objInvid + "]");

	// we have to put the sub netgroups in

	current_field = current_obj.getField((short) 258); // member netgroups

	Vector subnets = sNetObj.subnetgroups;
	String subName;

	for (int i = 0; i < subnets.size(); i++)
	  {
	    subName = (String) subnets.elementAt(i);

	    if (subName != null)
	      {
		invid = (Invid) netHash.get(subName);

		if (invid != null)
		  {
		    System.err.println("Attempting to add " + subName + ", [" + invid.toString()+"]");
		    current_field.addElement(invid);
		  }
		else
		  {
		    System.err.println("null invid");
		  }
	      }
	    else
	      {
		System.err.println("null subnet name");
	      }
	  }
      }
  }

  /**
   *
   */

  private static void registerAutomounter() throws RemoteException
  {
    Volume v;
    MapEntry m;
    Enumeration enum;
    String key;
    db_object current_obj, user_obj, embed_obj;
    db_field current_field;
    invid_field embed_field;
    Invid objInvid, hostInvid, userInvid;

    /* -- */

    // step 1: create the volumes, cache the name->invid mapping

    System.out.println("\nCreating automounter volumes\n");

    enum = volumes.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();
	v = (Volume) volumes.get(key);

	System.out.print("Creating " + key + " on host: " + v.hostName);
	
	current_obj = createObject((short) 276); // nfs volume
	objInvid = current_obj.getInvid();

	System.out.println(" [" + objInvid + "]");

	volumeInvids.put(key, objInvid); // save the invid in our hashtable

	current_obj.setFieldValue((short) 256, key); // volume name
	current_obj.setFieldValue((short) 258, v.path);	// path
	
	hostInvid = (Invid) systemInvid.get(v.hostName);

	current_obj.setFieldValue((short) 257, hostInvid);

	current_obj.setFieldValue((short) 260, v.mountOptions);
      }

    // step 2: create all automounter maps

    System.out.println("\nCreating automounter maps\n");

    enum = maps.elements();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	current_obj = createObject((short) 277); // automounter map

	current_obj.setFieldValue((short) 256, key); // set the name of the map

	objInvid = current_obj.getInvid();

	mapInvids.put(key, objInvid);
      }

    // step 3: create all automounter map entries

    System.out.println("\nCreating automounter map entries\n");

    enum = mapEntries.elements();

    while (enum.hasMoreElements())
      {
	m = (MapEntry) enum.nextElement();

	System.out.println("Creating " + m.toString());

	userInvid = (Invid) userInvids.get(m.userName);

	// need to edit this user, create an embedded object

	user_obj = editObject(userInvid);

	embed_field = (invid_field) user_obj.getField((short) 271); // Map Entries

	embed_obj = editObject(embed_field.createNewEmbedded().getInvid());

	// we've got the new map entry, load 'er up

	embed_obj.setFieldValue((short) 256, mapInvids.get(m.mapName));	// map invid

	embed_obj.setFieldValue((short) 257, volumeInvids.get(m.volName)); // volume invid
      }
    
    System.out.println("\nFinished creating automounter map entries\n");
  }

  private static db_object createObject(short type)
  {
    return my_client.session.create_db_object(type).getObject();
  }

  private static db_object editObject(Invid invid)
  {
    return my_client.session.edit_db_object(invid).getObject();
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
