/*

   LDAPBuilderTask.java

   This class is intended to dump the Ganymede datastore to a Mac OS
   X/RFC 2307 LDAP environment by way of LDIF.
   
   Created: 22 March 2004
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2004/03/23 00:29:25 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003, 2004
   The University of Texas at Austin.

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

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
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
                                                                   LDAPBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to a Mac OS
 * X/RFC 2307 LDAP environment by way of LDIF.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class LDAPBuilderTask extends GanymedeBuilderTask {

  private static String path = null;
  private static String dnsdomain = null;
  private static String buildScript = null;
  private static Runtime runtime = null;

  // ---

  private Date now = null;
  private SharedStringBuffer result = new SharedStringBuffer();

  private Invid normalCategory = null;

  /* -- */

  public LDAPBuilderTask(Invid _taskObjInvid)
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
    boolean success = false;

    /* -- */

    Ganymede.debug("LDAPBuilderTask builderPhase1 running");

    if (path == null)
      {
	path = System.getProperty("ganymede.builder.output");

	if (path == null)
	  {
	    throw new RuntimeException("LDAPBuilder not able to determine output directory.");
	  }

	path = PathComplete.completePath(path);
      }

    now = null;

    // passwd

    if (baseChanged(SchemaConstants.UserBase) || 
	baseChanged((short) 257) | // groups
	baseChanged((short) 270)) // user netgroups
	
      {
	Ganymede.debug("Need to build LDAP output");

	out = null;

	try
	  {
	    out = openOutFile(path + "sync.ldif", "ldap");
	  }
	catch (IOException ex)
	  {
	    System.err.println("LDAPBuilderTask.builderPhase1(): couldn't open sync.ldif file: " + ex);
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
		    
		    writeUserLine(user, out);
		  }
	      }
	    finally
	      {
		out.close();
	      }
	  }

	writeMailDirect2();
	writeSambafileVersion1();
	writeNTfile();
	writeUserSyncFile();
	writeHTTPfiles();

	success = true;
      }

    if (baseChanged((short) 277) || // automounter maps
	baseChanged((short) 276) || // nfs volumes
	baseChanged((short) 263) || // in case systems were renamed
	baseChanged(SchemaConstants.UserBase) || // in case users were renamed
	baseChanged((short) 278)) // automounter map entries
      {
	Ganymede.debug("Need to build automounter maps");

	if (writeAutoMounterFiles())
	  {
	    success = true;
	  }
      }

    Ganymede.debug("LDAPBuilderTask builderPhase1 completed");

    return success;
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

    Ganymede.debug("LDAPBuilderTask builderPhase2 running");

    if (buildScript == null)
      {
	buildScript = System.getProperty("ganymede.builder.scriptlocation");
	buildScript = PathComplete.completePath(buildScript);
	buildScript = buildScript + "gashbuilder";
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
	Ganymede.debug(buildScript + " doesn't exist, not running external GASH build script");
      }

    Ganymede.debug("LDAPBuilderTask builderPhase2 completed");

    return true;
  }

  /**
   *
   * This method extracts an embedded hostname from a top-level interface
   * object.
   *
   */

  private String getInterfaceHostname(DBObject object)
  {
    return (String) object.getFieldValueLocal(interfaceSchema.NAME);
  }

  /**
   * <P>This method generates a transitive closure of the members of a
   * user netgroup, including all users in all member netgroups,
   * recursively.</P> 
   */

  private Vector netgroupMembers(DBObject object)
  {
    return netgroupMembers(object, null, null);
  }

  private Vector netgroupMembers(DBObject object, Vector oldMembers, Hashtable graphCheck)
  {
    if (oldMembers == null)
      {
	oldMembers = new Vector();
      }

    if (graphCheck == null)
      {
	graphCheck = new Hashtable();
      }

    // make sure we don't get into an infinite loop if someone made
    // the user netgroup graph circular

    if (graphCheck.containsKey(object.getInvid()))
      {
	return oldMembers;
      }
    else
      {
	graphCheck.put(object.getInvid(), object.getInvid());
      }

    // add users in this Netgroup to oldMembers

    InvidDBField users = (InvidDBField) object.getField(userNetgroupSchema.USERS);

    if (users != null)
      {
	oldMembers = VectorUtils.union(oldMembers, 
				       VectorUtils.stringVector(users.getValueString(), ", "));
      }

    // recursively add in users in any netgroups in this netgroup

    InvidDBField subGroups = (InvidDBField) object.getField(userNetgroupSchema.MEMBERGROUPS);

    if (subGroups != null)
      {
	for (int i = 0; i < subGroups.size(); i++)
	  {
	    DBObject subGroup = getObject(subGroups.value(i));
	    
	    if (!subGroup.isInactivated())
	      {
		oldMembers = netgroupMembers(subGroup, oldMembers, graphCheck);
	      }
	  }
      }

    return oldMembers;
  }
}
