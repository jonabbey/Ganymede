/*

   LinuxBuilderTask.java

   This class is intended to dump the Ganymede datastore to the
   Linux passwd and group files.
   
   Created: 8 September 1998
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.PathComplete;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                LinuxBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to the
 * Linux passwd and group files.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class LinuxBuilderTask extends GanymedeBuilderTask {

  private static String path = null;
  private static String buildScript = null;
  private static Runtime runtime = null;

  // ---

  private Date now = null;

  /* -- */

  /**
   *
   * This method runs with a dumpLock obtained for the builder task.
   *
   * Code run in builderPhase1() can call enumerateObjects() and
   * baseChanged().
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   *
   */

  public boolean builderPhase1()
  {
    PrintWriter out;
    boolean result = false;

    /* -- */

    if (path == null)
      {
	path = System.getProperty("ganymede.builder.output");

	if (path == null)
	  {
	    throw new RuntimeException("LinuxBuilder not able to determine output directory.");
	  }

	path = PathComplete.completePath(path);
      }

    now = null;

    // passwd

    if (baseChanged(SchemaConstants.UserBase))
      {
	Ganymede.debug("Need to build user map");

	out = null;

	try
	  {
	    out = openOutFile(path + "passwd");
	  }
	catch (IOException ex)
	  {
	    System.err.println("LinuxBuilderTask.builderPhase1(): couldn't open passwd file: " + ex);
	  }
	
	if (out != null)
	  {
	    DBObject user;
	    Enumeration users = enumerateObjects(SchemaConstants.UserBase);

	    while (users.hasMoreElements())
	      {
		user = (DBObject) users.nextElement();

		writeUserLine(user, out);
	      }

	    out.close();
	  }

	result = true;
      }

    // group

    if (baseChanged((short) 257))
      {
	Ganymede.debug("Need to build group map");

	out = null;

	try
	  {
	    out = openOutFile(path + "group");
	  }
	catch (IOException ex)
	  {
	    System.err.println("LinuxBuilderTask.builderPhase1(): couldn't open group file: " + ex);
	  }
	
	if (out != null)
	  {
	    DBObject group;
	    Enumeration groups = enumerateObjects((short) 257);

	    while (groups.hasMoreElements())
	      {
		group = (DBObject) groups.nextElement();

		writeGroupLine(group, out);
	      }

	    out.close();
	  }

	result = true;
      }

    return result;
  }

  /**
   *
   * This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.
   *
   * builderPhase2 is only run if builderPhase1 returns true.
   *
   */

  public boolean builderPhase2()
  {
    File
      file;

    /* -- */

    if (buildScript == null)
      {
	buildScript = path + "linux_builder";
      }

    file = new File(buildScript);

    if (file.exists())
      {
	if (runtime == null)
	  {
	    runtime = Runtime.getRuntime();
	  }

	try
	  {
	    Process process;

	    /* -- */

	    process = runtime.exec(buildScript);

	    process.waitFor();
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't exec buildScript (" + buildScript + ") due to IOException: " + ex);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("Failure during exec of buildScript (" + buildScript + "): " + ex);
	  }
      }
    else
      {
	Ganymede.debug(buildScript + " doesn't exist, not running external Linux build script");
      }

    return true;
  }

  // ***
  //
  // The following private methods are used to support the Linux builder logic.
  //
  // ***

  /**
   *
   * This method opens the specified file for writing out a text stream.
   *
   * If there already exists a file by that name in the system, the old
   * version will be moved to a backup before the file is recreated for
   * writing.
   *
   */

  private PrintWriter openOutFile(String filename) throws IOException
  {
    File
      file,
      oldFile;

    /* -- */

    file = new File(filename);

    // back up the file if it exists.

    if (file.exists())
      {
	if (lastRunTime != null)
	  {
	    oldFile = new File(filename + lastRunTime.toString());
	  }
	else
	  {
	    if (now == null)
	      {
		now = new Date();
	      }

	    oldFile = new File(filename + now.toString());
	  }

	file.renameTo(oldFile);
      }

    return new PrintWriter(new BufferedWriter(new FileWriter(file)));
  }

  /**
   *
   * This method writes out a line to the passwd Linux source file.
   *
   * The lines in this file look like the following.
   *
   * broccol:393T6k3e/9/w2:12003:12010:Jonathan Abbey,S321 CSD,3199,8343915:/home/broccol:/bin/tcsh
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this user line
   *
   */

  private void writeUserLine(DBObject object, PrintWriter writer)
  {
    String username;
    String cryptedPass;
    int uid;
    int gid;
    String name;
    String room;
    String officePhone;
    String homePhone;
    String directory;
    String shell;

    PasswordDBField passField;
    Vector invids;
    Invid groupInvid;
    DBObject group;

    StringBuffer result = new StringBuffer();

    /* -- */

    username = (String) object.getFieldValueLocal(SchemaConstants.UserUserName);

    passField = (PasswordDBField) object.getField(SchemaConstants.UserPassword);

    if (passField != null)
      {
	cryptedPass = passField.getUNIXCryptText();
      }
    else
      {
	System.err.println("LinuxBuilder.writeUserLine(): null password for user " + username);
	cryptedPass = "**Nopass**";
      }

    uid = ((Integer) object.getFieldValueLocal(userSchema.UID)).intValue();

    // get the gid
    
    groupInvid = (Invid) object.getFieldValueLocal(userSchema.HOMEGROUP); // home group

    if (groupInvid == null)
      {
	System.err.println("LinuxBuilder.writeUserLine(): null gid for user " + username);
	gid = -1;
      }
    else
      {
	group = getObject(groupInvid);

	if (group == null)
	  {
	    System.err.println("LinuxBuilder.writeUserLine(): couldn't get reference to home group");

	    gid = -1;
	  }
	else
	  {
	    Integer gidInt = (Integer) group.getFieldValueLocal(groupSchema.GID);

	    if (gidInt == null)
	      {
		System.err.println("LinuxBuilder.writeUserLine(): couldn't get gid value");

		gid = -1;
	      }
	    else
	      {
		gid = gidInt.intValue();
	      }
	  }
      }

    name = (String) object.getFieldValueLocal(userSchema.FULLNAME);
    room = (String) object.getFieldValueLocal(userSchema.ROOM);
    officePhone = (String) object.getFieldValueLocal(userSchema.OFFICEPHONE);
    homePhone = (String) object.getFieldValueLocal(userSchema.HOMEPHONE);
    directory = (String) object.getFieldValueLocal(userSchema.HOMEDIR);
    shell = (String) object.getFieldValueLocal(userSchema.LOGINSHELL);

    // now build our output line

    result.append(username);
    result.append(":");
    result.append(cryptedPass);
    result.append(":");
    result.append(Integer.toString(uid));
    result.append(":");
    result.append(Integer.toString(gid));
    result.append(":");

    result.append(name);

    if (room != null && !room.equals(""))
      {
	result.append(",");
	result.append(room);
      }

    if (officePhone != null && !officePhone.equals(""))
      {
	result.append(",");
	result.append(officePhone);
      }

    if (homePhone != null && !homePhone.equals(""))
      {
	result.append(",");
	result.append(homePhone);
      }

    result.append(":");
    result.append(directory);
    result.append(":");
    result.append(shell);

    if (result.length() > 1024)
      {
	System.err.println("LinuxBuilder.writeUserLine(): Warning!  user " + 
			   username + " overflows the Linux line length!");
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a line to the passwd Linux source file.
   *
   * The lines in this file look like the following.
   *
   * adgacc:ZzZz:4015:hammp,jgeorge,dd,doodle,dhoss,corbett,monk
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this user line
   *
   */

  private void writeGroupLine(DBObject object, PrintWriter writer)
  {
    String groupname;
    String pass;
    int gid;
    Vector users = new Vector();

    PasswordDBField passField;
    Vector invids;
    Invid userInvid;
    String userName;

    StringBuffer result = new StringBuffer();

    /* -- */

    groupname = (String) object.getFieldValueLocal(groupSchema.GROUPNAME);

    // currently in the Ganymede schema, group passwords aren't in passfields.

    pass = (String) object.getFieldValueLocal(groupSchema.PASSWORD);

    Integer gidInt = (Integer) object.getFieldValueLocal(groupSchema.GID);

    if (gidInt == null)
      {
	System.err.println("Error, couldn't find gid for group " + groupname);
	return;
      }

    gid = gidInt.intValue();
    
    // we currently don't explicitly record the home group.. just take the first group
    // that the user is in.

    invids = object.getFieldValuesLocal(groupSchema.USERS);

    if (invids == null)
      {
	// System.err.println("LinuxBuilder.writeUserLine(): null user list for group " + groupname);
      }
    else
      {
	for (int i = 0; i < invids.size(); i++)
	  {
	    userInvid = (Invid) invids.elementAt(i);
	    
	    userName = getLabel(userInvid);

	    if (userName != null)
	      {
		users.addElement(userName);
	      }
	  }
      }

    // now build our output line

    result.append(groupname);
    result.append(":");
    result.append(pass);
    result.append(":");
    result.append(Integer.toString(gid));
    result.append(":");

    for (int i = 0; i < users.size(); i++)
      {
	if (i > 0)
	  {
	    result.append(",");
	  }

	result.append((String) users.elementAt(i));
      }

    if (result.length() > 1024)
      {
	System.err.println("LinuxBuilder.writeGroupLine(): Warning!  group " + 
			   groupname + " overflows the Linux line length!");
      }

    writer.println(result.toString());
  }
}
