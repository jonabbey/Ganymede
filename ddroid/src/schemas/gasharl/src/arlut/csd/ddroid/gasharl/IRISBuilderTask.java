/*

   IRISBuilderTask.java

   This class is intended to dump the Directory Droid datastore to the ARL:UT
   IRIS system.
   
   Created: 27 September 2004

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ddroid.gasharl;

import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.server.DBObject;
import arlut.csd.ddroid.server.PasswordDBField;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.SharedStringBuffer;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.FileOps;

import java.util.*;
import java.text.*;
import java.io.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 IRISBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 *  This class is intended to dump the Directory Droid datastore to the ARL:UT
 *  IRIS system.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class IRISBuilderTask extends GanymedeBuilderTask {

  private static String path = null;
  private static String dnsdomain = null;
  private static String buildScript = null;
  private static Runtime runtime = null;

  // ---

  private Date now = null;
  private SharedStringBuffer result = new SharedStringBuffer();

  private Invid normalCategory = null;

  /* -- */

  public IRISBuilderTask(Invid _taskObjInvid)
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
    
    /* -- */

    Ganymede.debug("IRISBuilderTask builderPhase1 running");

    if (path == null)
      {
	path = System.getProperty("ganymede.builder.output");

	if (path == null)
	  {
	    throw new RuntimeException("IRISBuilder not able to determine output directory.");
	  }

	path = PathComplete.completePath(path);
      }

    now = null;

    if (baseChanged(SchemaConstants.UserBase) || 
	/* User netgroups */
	baseChanged((short) 270))
      {
	Ganymede.debug("Need to build IRIS output");

	out = null;

	try
	  {
	    out = openOutFile(path + "iris_sync.txt", "iris");
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("IRISBuilderTask.builderPhase1(): couldn't open iris_sync.txt file: " + ex);
	  }
	
	if (out != null)
	  {
	    try
	      {
		DBObject user;
		Enumeration users = enumerateObjects(SchemaConstants.UserBase);
		
		while (users.hasMoreElements())
		  {
		    user = (DBObject) users.nextElement();

		    if (user_in_netgroup(user, "IRIS-users"))
		      {
			write_iris(out, user);
		      }
		  }
	      }
	    finally
	      {
		out.close();
	      }
	  }
      }

    Ganymede.debug("IRISBuilderTask builderPhase1 completed");

    return true;
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

    Ganymede.debug("IRISBuilderTask builderPhase2 running");

    if (buildScript == null)
      {
	buildScript = System.getProperty("ganymede.builder.scriptlocation");
	buildScript = PathComplete.completePath(buildScript);
	buildScript = buildScript + "irisbuilder";
      }

    file = new File(buildScript);

    if (file.exists())
      {
	if (runtime == null)
	  {
	    runtime = Runtime.getRuntime();
	  }

	Process process = null;

	/* -- */

	try
	  {
	    FileOps.runProcess(buildScript);
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't exec buildScript (" + buildScript + 
			   ") due to IOException: " + ex);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("Failure during exec of buildScript (" + buildScript + "): " + ex);
	  }
      }
    else
      {
	Ganymede.debug(buildScript + " doesn't exist, not running external LDAP build script");
      }

    Ganymede.debug("IRISBuilderTask builderPhase2 completed");

    return true;
  }

  /**
   * <p>Returns true if the user object is a member of the given
   * netgroup name, either through direct membership or through
   * transitive closure.</p>
   */

  public boolean user_in_netgroup(DBObject user, String netgroupName)
  {
    Hashtable table = new Hashtable();

    Vector netgroups = user.getFieldValuesLocal(userSchema.NETGROUPS);

    for (int i = 0; i < netgroups.size(); i++)
      {
	DBObject netgroup = getObject(netgroups.elementAt(i));
	
	if (netgroup_or_parent_equals(netgroup, netgroupName))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * <p>Recursive method to determine whether a given netgroup (or its containers)
   * matches netgroupName.</p>
   */

  public boolean netgroup_or_parent_equals(DBObject netgroup, String netgroupName)
  {
    String name = netgroup.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);

    if (name.equals(netgroupName))
      {
	return true;
      }
    
    Vector netgroups = netgroup.getFieldValuesLocal(userNetgroupSchema.OWNERNETGROUPS);

    for (int i = 0; i < netgroups.size(); i++)
      {
	if (netgroup_matches(getObject(netgroups.elementAt(i)), netgroupName))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * <p>Write a line to the IRIS sync file</p>
   *
   * <p>The lines look like this:</p>
   *
   * <p>username|invid|badge number|md5Crypt|plaintext</p>
   *
   * <p>example:</p>
   *
   * <p>broccol|3:627|4297|$1$BoVVyEwQ$wbJYuEHeN/vdMeARLFhG0/|NotMyRealPass</p>
   */

  private void write_iris(PrintWriter out, DBObject userObject)
  {
    String
      username,
      invidString,
      badge,
      md5Crypt,
      plaintext;

    /* -- */
      
    username = userObject.getFieldValueLocal(userSchema.USERNAME);
    invidString = userObject.getInvid().toString();
    badge = userObject.getFieldValueLocal(userSchema.BADGE);
    
    PasswordDBField passField = userObject.getField(userSchema.PASSWORD);
    md5Crypt = passField.getMD5CryptText();
    plaintext = passField.getPlainText();

    StringBuffer output = new StringBuffer();

    output.append(escapeString(username));
    output.append("|");
    output.append(invidString);
    output.append("|");
    output.append(escapeString(badge));
    output.append("|");
    output.append(escapeString(md5Crypt));
    output.append("|");
    output.append(escapeString(plaintext));

    out.println(output.toString());
  }

  /** 
   * We can't have any | characters in passwords in the iris_sync.txt
   * file we generate, since we use | chars as field separators in
   * this file.  Make sure that we backslash any such chars.
   */

  private String escapeString(String in)
  {
    if (in == null)
      {
	return "";
      }

    StringBuffer buffer = new StringBuffer();
    char[] ary = in.toCharArray();

    /* -- */

    // do it

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (ary[i] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(ary[i]);
	  }
      }

    return buffer.toString();
  }
}