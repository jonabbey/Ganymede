/*

   UNIXBuilderTask.java

   This class is intended to dump the Ganymede datastore to the
   UNIX passwd and group files.
   
   Created: 8 September 1998

   Last Mod Date: $Date: 2004-12-07 22:22:12 -0600 (Tue, 07 Dec 2004) $
   Last Revision Changed: $Rev: 5915 $
   Last Changed By: $Author: broccol $
   SVN URL: $HeadURL: http://db1.arlut.utexas.edu/svn/ganymede/trunk/userKit/src/userKit/arlut/csd/ganymede/userKit/UNIXBuilderTask.java $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin.

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

package arlut.csd.ganymede.userKit;

import arlut.csd.ganymede.server.*;
import arlut.csd.ganymede.common.*;
import arlut.csd.Util.FileOps;

import java.util.*;
import java.text.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 UNIXBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to the
 * UNIX passwd and group files.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class UNIXBuilderTask extends GanymedeBuilderTask {

  private Runtime runtime = null;
  private String buildScript = null;
  private String md5passwdFile =  null;
  private String passwdFile = null;
  private String groupFile = null;

  // ---

  private Date now = null;
  private boolean backedup = false;

  /* -- */

  public UNIXBuilderTask(Invid _taskObjInvid)
  {
    // set the taskDefObjInvid in GanymedeBuilderTask so
    // we can look up option strings

    taskDefObjInvid = _taskObjInvid;
  }

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

    // initialize operational variables for the new run

    backedup = false;
    now = null;

    // now see what our current option values are

    passwdFile = getOptionValue("passwdFile");
    md5passwdFile = getOptionValue("md5passwdFile");

    if (passwdFile == null && md5passwdFile == null)
      {
	Ganymede.debug("UNIXBuilderTask: error, no passwdFile specified");
      }

    if (passwdFile != null && passwdFile.equals(md5passwdFile))
      {
	Ganymede.debug("UNIXBuilderTask: error, md5passwdFile and passwdFile are the same, skipping md5passwdFile");
	md5passwdFile = null;
      }

    groupFile = getOptionValue("groupFile");

    if (groupFile == null)
      {
	Ganymede.debug("UNIXBuilderTask: error, no groupFile specified");
      }

    // passwd

    if (baseChanged(SchemaConstants.UserBase))
      {
	Ganymede.debug("UNIXBuilderTask: Need to build user map");

	if (passwdFile != null) 
	  {
	    out = null;

	    try
	      {
		out = openOutFile(passwdFile, "unix");
	      }
	    catch (IOException ex)
	      {
		ex.printStackTrace();
		Ganymede.debug("UNIXBuilderTask.builderPhase1(): couldn't open " + passwdFile + ": " + ex);
	      }
	  
	    if (out != null)
	      {
		DBObject user;
		Enumeration users = enumerateObjects(SchemaConstants.UserBase);
	      
		while (users.hasMoreElements())
		  {
		    user = (DBObject) users.nextElement();
		  
		    writeUserLine(user, out, false);
		  }
	      
		out.close();
	      }

	    result = true;
	  }

	if (md5passwdFile != null) 
	  {
	    out = null;

	    try
	      {
		out = openOutFile(md5passwdFile, "unix");
	      }
	    catch (IOException ex)
	      {
		ex.printStackTrace();
		Ganymede.debug("UNIXBuilderTask.builderPhase1(): couldn't open " + md5passwdFile + ": " + ex);
	      }
	  
	    if (out != null)
	      {
		DBObject user;
		Enumeration users = enumerateObjects(SchemaConstants.UserBase);
	      
		while (users.hasMoreElements())
		  {
		    user = (DBObject) users.nextElement();
		  
		    writeUserLine(user, out, true);
		  }
	      
		out.close();
	      }

	    result = true;
	  }
      }

    // group

    if (baseChanged((short) 257))
      {
	Ganymede.debug("UNIXBuilderTask: Need to build group map");

	if (groupFile != null)
	  {
	    out = null;

	    try
	      {
		out = openOutFile(groupFile, "unix");
	      }
	    catch (IOException ex)
	      {
		ex.printStackTrace();
		Ganymede.debug("UNIXBuilderTask.builderPhase1(): couldn't open " + groupFile + ": " + ex);
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

    buildScript = getOptionValue("buildScript");

    if (buildScript == null || buildScript.equals(""))
      {
	Ganymede.debug("UNIXBuilderTask: no buildScript defined in task object's option string vector");
	Ganymede.debug("                 not running any external update script.");
	return false;
      }

    file = new File(buildScript);

    if (file.exists())
      {
	Ganymede.debug("UNIXBuilderTask builderPhase2 running");

	try
	  {
	    FileOps.runProcess(buildScript);
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
	Ganymede.debug(buildScript + " doesn't exist, not running external UNIX build script");
      }

    Ganymede.debug("UNIXBuilderTask builderPhase2 completed");

    return true;
  }

  // ***
  //
  // The following private methods are used to support the UNIX builder logic.
  //
  // ***

  /**
   *
   * This method writes out a line to the passwd UNIX source file.
   *
   * The lines in this file look like the following.
   *
   * broccol:393T6k3e/9/w2:12003:12010:Jonathan Abbey,S321 CSD,3199,8343915:/home/broccol:/bin/tcsh
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this user line
   * @param useMD5 If true, passwords will be written out in md5 format if possible
   *
   */

  private void writeUserLine(DBObject object, PrintWriter writer, boolean useMD5)
  {
    String username;
    String cryptedPass = null;
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
	if (useMD5)
	  {
	    cryptedPass = passField.getMD5CryptText();
	  }

	// if the Ganymede server doesn't have an MD5 password for
	// this user, go ahead and devolve to the crypt() password if
	// available.  This might have been set by rpcpass and the NIS
	// passwd daemon.

	if (cryptedPass == null)
	  {
	    cryptedPass = passField.getUNIXCryptText();
	  }
      }
    else
      {
	Ganymede.debug("UNIXBuilderTask.writeUserLine(): null password for user " + username);
	cryptedPass = "**Nopass**";
      }

    uid = ((Integer) object.getFieldValueLocal(userSchema.UID)).intValue();

    // get the gid
    
    groupInvid = (Invid) object.getFieldValueLocal(userSchema.HOMEGROUP); // home group

    if (groupInvid == null)
      {
	Ganymede.debug("UNIXBuilderTask.writeUserLine(): null gid for user " + username);
	gid = -1;
      }
    else
      {
	group = getObject(groupInvid);

	if (group == null)
	  {
	    Ganymede.debug("UNIXBuilderTask.writeUserLine(): couldn't get reference to home group");

	    gid = -1;
	  }
	else
	  {
	    Integer gidInt = (Integer) group.getFieldValueLocal(groupSchema.GID);

	    if (gidInt == null)
	      {
		Ganymede.debug("UNIXBuilderTask.writeUserLine(): couldn't get gid value");

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
	Ganymede.debug("UNIXBuilderTask.writeUserLine(): Warning!  user " + 
		       username + " overflows the UNIX line length!");
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a line to the passwd UNIX source file.
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
	Ganymede.debug("Error, couldn't find gid for group " + groupname);
	return;
      }

    gid = gidInt.intValue();
    
    // we currently don't explicitly record the home group.. just take the first group
    // that the user is in.

    invids = object.getFieldValuesLocal(groupSchema.USERS);

    if (invids == null)
      {
	// Ganymede.debug("UNIXBuilder.writeUserLine(): null user list for group " + groupname);
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
	Ganymede.debug("UNIXBuilderTask.writeGroupLine(): Warning!  group " + 
		       groupname + " overflows the UNIX line length!");
      }

    writer.println(result.toString());
  }
}
