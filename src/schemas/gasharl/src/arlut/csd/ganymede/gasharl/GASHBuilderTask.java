/*

   GASHBuilderTask.java

   This class is intended to dump the Ganymede datastore to GASH.
   
   Created: 21 May 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

package arlut.csd.ganymede.gasharl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.SharedStringBuffer;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBLogEvent;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeBuilderTask;
import arlut.csd.ganymede.server.IPDBField;
import arlut.csd.ganymede.server.InvidDBField;
import arlut.csd.ganymede.server.PasswordDBField;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 GASHBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to GASH.
 *
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class GASHBuilderTask extends GanymedeBuilderTask {

  private static String path = null;
  private static String dnsdomain = null;
  private static String buildScript = null;

  // ---

  private Date now = null;
  private SharedStringBuffer result = new SharedStringBuffer();

  private Invid normalCategory = null;

  /**
   * customOptions is a Set of Invids for custom type definitions that
   * we encountered during a cycle of writing out DHCP information.
   *
   * We'll use this Set to keep track of custom options that we find
   * during the generation of our dhcp output.  At all other times,
   * this Map will be null.
   */

  private Set customOptions = null;

  /* -- */

  public GASHBuilderTask(Invid _taskObjInvid)
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

    Ganymede.debug("GASHBuilderTask builderPhase1 running");

    if (path == null)
      {
	path = System.getProperty("ganymede.builder.output");
 
	if (path == null)
	  {
	    throw new RuntimeException("GASHBuilder not able to determine output directory.");
	  }

	path = PathComplete.completePath(path);
      }

    if (dnsdomain == null)
      {
	dnsdomain = System.getProperty("ganymede.gash.dnsdomain");

	if (dnsdomain == null)
	  {
	    throw new RuntimeException("GASHBuilder not able to determine DNS domain.");
	  }

	// prepend a dot if we need to

	if (dnsdomain.indexOf('.') != 0)
	  {
	    dnsdomain = "." + dnsdomain;
	  }
      }

    now = null;

    // passwd

    if (baseChanged(SchemaConstants.UserBase))
      {
	Ganymede.debug("Need to build user map");

	out = null;

	try
	  {
	    out = openOutFile(path + "user_info", "gasharl");
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.builderPhase1(): couldn't open user_info file: " + ex);
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
	writeSambafileVersion2();
	writeUserSyncFile();
	writeHTTPfiles();

	success = true;
      }

    // group

    if (baseChanged((short) 257) ||
	baseChanged(SchemaConstants.UserBase)) // in case a user was renamed
      {
	Ganymede.debug("Need to build group map");

	out = null;

	try
	  {
	    out = openOutFile(path + "group_info", "gasharl");
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.builderPhase1(): couldn't open group_info file: " + ex);
	  }
	
	PrintWriter out2 = null;

	try
	  {
	    out2 = openOutFile(path + "group.owner", "gasharl");
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.builderPhase1(): couldn't open group.owner file: " + ex);
	  }

	try
	  {
	    DBObject group;
	    Enumeration groups = enumerateObjects((short) 257);
		
	    while (groups.hasMoreElements())
	      {
		group = (DBObject) groups.nextElement();
		
		if (out != null)
		  {
		    writeGroupLine(group, out);
		  }

		if (out2 != null)
		  {
		    writeGroupOwnerLine(group, out2);
		  }
	      }
	  }
	finally
	  {
	    if (out != null)
	      {
		out.close();
	      }

	    if (out2 != null)
	      {
		out2.close();
	      }
	  }

	success = true;
      }

    if (baseChanged(SchemaConstants.UserBase) || // users
	baseChanged((short) 274) || // mail lists
	baseChanged((short) 275) || // external mail addresses
	baseChanged((short) 260)) // mailman lists  
      {
	Ganymede.debug("Need to build aliases map");

	if (writeAliasesFile())
	  {
	    success = true;
	  }
      }

    if (baseChanged((short) 260)) // mailman lists  
      {
	Ganymede.debug("Need to call mailman ns8 sync script");

	if (writeMailmanListsFile())
	  {
	    success = true;
	  }
      }

    if (baseChanged((short) 271) || // system netgroups
	baseChanged((short) 270) || // user netgroups
	baseChanged(SchemaConstants.UserBase) || // in case users were renamed
	baseChanged((short) 263)) // in case systems were renamed
      {
	Ganymede.debug("Need to build netgroup map");

	if (writeNetgroupFile())
	  {
	    success = true;
	  }

	if (writeUserNetgroupFile())
	  {
	    success = true;
	  }
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

    if (baseChanged((short) 263) || // system base
	baseChanged((short) 267) || // I.P. Network base
	baseChanged((short) 265)) // system interface base
      {
	Ganymede.debug("Need to build DNS tables");
	writeSysFile();
	writeSysDataFile();
	success = true;
      }

    if (baseChanged((short) 263) || // system base
	baseChanged((short) 267) || // I.P. Network base
	baseChanged((short) 265) || // system interface base
        baseChanged((short) 262) || // DHCP Group
        baseChanged((short) 264) || // Embedded DHCP Option Value
        baseChanged((short) 266))  // DHCP Option definition
      {
	Ganymede.debug("Need to build DHCP configuration file");
        writeDHCPFile();
	success = true;
      }

    Ganymede.debug("GASHBuilderTask builderPhase1 completed");

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

    Ganymede.debug("GASHBuilderTask builderPhase2 running");

    if (buildScript == null)
      {
	buildScript = System.getProperty("ganymede.builder.scriptlocation");

        if (buildScript == null)
          {
            Ganymede.debug("GASHBuilderTask couldn't find any property definition for ganymede.builder.scriptlocation.");
            Ganymede.debug("\nNot executing external build for GASHBuilderTask.");
            return false;
          }

	buildScript = PathComplete.completePath(buildScript);
	buildScript = buildScript + "gashbuilder";
      }

    int resultCode = -999;      // a resultCode of 0 is success

    file = new File(buildScript);

    if (file.exists())
      {
	try
	  {
	    resultCode = FileOps.runProcess(buildScript);
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

    if (resultCode != 0)
      {
        String path = "";

        try
          {
            path = file.getCanonicalPath();
          }
        catch (IOException ex)
          {
            path = buildScript;
          }

        String message = "Error encountered running sync script \"" + path + "\" for the GASHBuilderTask." +
          "\n\nI got a result code of " + resultCode + " when I tried to run it.";

        DBLogEvent event = new DBLogEvent("externalerror", message, null, null, null, null);

        Ganymede.log.logSystemEvent(event);
      }

    Ganymede.debug("GASHBuilderTask builderPhase2 completed");

    return true;
  }

  // ***
  //
  // The following private methods are used to support the GASH builder logic.
  //
  // ***

  /**
   *
   * This method writes out a line to the user_info GASH source file.
   *
   * The lines in this file look like the following.
   *
   * broccol:393T6k3e/9/w2:12003:12010:Jonathan Abbey,S321 CSD,3199,8343915:/home/broccol:/bin/tcsh:ss#:normal:exp:lastadm
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
    String div;
    String officePhone;
    String homePhone;
    String directory;
    String shell;
    Invid categoryInvid;
    String category;

    PasswordDBField passField;
    Invid groupInvid;
    DBObject group;

    boolean inactivated = false;

    /* -- */

    result.setLength(0);

    username = (String) object.getFieldValueLocal(SchemaConstants.UserUserName);

    passField = (PasswordDBField) object.getField(SchemaConstants.UserPassword);

    if (passField != null)
      {
	cryptedPass = passField.getUNIXCryptText();
      }
    else
      {
	inactivated = true;

	// System.err.println("GASHBuilder.writeUserLine(): null password for user " + username);
	cryptedPass = "**Nopass**";
      }

    uid = ((Integer) object.getFieldValueLocal(userSchema.UID)).intValue();

    // extra security precaution.. homey don't play no root accounts in NIS games.

    if (uid == 0)
      {
	Ganymede.debug("GASHBuilder.writeUserLine(): *** root uid in user " + username + ", skipping!! ***");
	return;			// no writeLine
      }

    // get the gid
    
    groupInvid = (Invid) object.getFieldValueLocal(userSchema.HOMEGROUP); // home group

    if (groupInvid == null)
      {
	System.err.println("GASHBuilder.writeUserLine(): null gid for user " + username);
	gid = -1;
      }
    else
      {
	group = getObject(groupInvid);
	gid = ((Integer) group.getFieldValueLocal(groupSchema.GID)).intValue();
      }

    name = (String) object.getFieldValueLocal(userSchema.FULLNAME);
    room = (String) object.getFieldValueLocal(userSchema.ROOM);
    div = (String) object.getFieldValueLocal(userSchema.DIVISION);
    officePhone = (String) object.getFieldValueLocal(userSchema.OFFICEPHONE);
    homePhone = (String) object.getFieldValueLocal(userSchema.HOMEPHONE);
    directory = (String) object.getFieldValueLocal(userSchema.HOMEDIR);

    // force /bin/false if the user is inactivated

    if (inactivated)
      {
	shell = "/bin/false";
      }
    else
      {
	shell = (String) object.getFieldValueLocal(userSchema.LOGINSHELL);
      }

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
    result.append(",");
    result.append(room);
    result.append(" ");
    result.append(div);
    result.append(",");
    result.append(officePhone);

    if (homePhone != null && !homePhone.equals(""))
      {
	result.append(",");
	result.append(homePhone);
      }

    result.append(":");
    result.append(directory);
    result.append(":");
    result.append(shell);
    result.append("::");

    categoryInvid = (Invid) object.getFieldValueLocal(userSchema.CATEGORY);

    if (categoryInvid != null)
      {
	category = getLabel(categoryInvid);
	result.append(category);
      }

    result.append(":");

    Date expDate = (Date) object.getFieldValueLocal(SchemaConstants.ExpirationField);

    if (expDate != null)
      {
	long timecode = expDate.getTime();

	// we want to emit a UNIX timecode, which is one thousandth the
	// value of the Java timecode.  We will overflow here if the
	// expiration date is past 2038, but this will make Steve happy.

	int mytimecode = (int) (timecode/1000);
	result.append(mytimecode);
      }
    else
      {
	result.append("0");
      }

    // Back in the GASH days, we appended the name of the admin who
    // last modified this record.  nowadays we completely ignore this
    // field, but it's here for compatibility with external scripts
    // that we've carried over from the GASH world.

    result.append(":ganymede");

    if (result.length() > 1024)
      {
	System.err.println("GASHBuilder.writeUserLine(): Warning!  user " + 
			   username + " overflows the GASH line length!");
      }

    writer.println(result.toString());
  }

  /**
   * we write out a file that maps badge numbers to a
   * user's primary email address and user name for the
   * personnel office's phonebook database to use
   *
   * This method writes lines to the maildirect2 GASH output file.
   *
   * The lines in this file look like the following.
   *
   * 4297 jonabbey@arlut.utexas.edu broccol
   *
   */

  private void writeMailDirect2()
  {
    PrintWriter out;
    Hashtable map = new Hashtable(); // map badge numbers to DBObject
    Hashtable results = new Hashtable(); // map badge numbers to strings

    /* -- */

    try
      {
	out = openOutFile(path + "maildirect2", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.builderPhase1(): couldn't open maildirect2 file: " + ex);
	return;
      }
	
    try
      {
	DBObject user;
	Enumeration users = enumerateObjects(SchemaConstants.UserBase);
		
	while (users.hasMoreElements())
	  {
	    user = (DBObject) users.nextElement();

	    String username = (String) user.getFieldValueLocal(SchemaConstants.UserUserName);
	    String signature = (String) user.getFieldValueLocal(userSchema.SIGNATURE);
	    String badgeNum = (String) user.getFieldValueLocal(userSchema.BADGE);
	    Invid category = (Invid) user.getFieldValueLocal(userSchema.CATEGORY);

	    if (normalCategory == null)
	      {
		if (category != null)
		  {
		    String label;

		    label = getLabel(category);

		    if (label != null && label.equals("normal"))
		      {
			normalCategory = category;
		      }
		  }
	      }
    
	    if (username != null && signature != null && badgeNum != null && 
		category != null && category.equals(normalCategory) && !user.isInactivated())
	      {
		if (map.containsKey(badgeNum))
		  {
		    // we've got more than one entry with the same
		    // badge number.. that should only
		    // happen if one of the users is an GASH admin, or
		    // if one is inactivated.

		    DBObject oldUser = (DBObject) map.get(badgeNum);

		    DBField field = (DBField) oldUser.getField(userSchema.PERSONAE);

		    if (field != null && field.isDefined())
		      {
			continue; // we've already got an admin record for this badge number
		      }
		  }

		result.setLength(0);

		result.append(badgeNum);
		result.append(" ");
		result.append(signature);
		result.append("@");
		result.append(dnsdomain.substring(1)); // skip leading .
		result.append(" ");
		result.append(username);

		map.put(badgeNum, user);
		results.put(badgeNum, result.toString());
	      }
	  }

	Enumeration lines = results.elements();

	while (lines.hasMoreElements())
	  {
	    out.println((String) lines.nextElement());
	  }
      }
    finally
      {
	out.close();
      }
  }
  
  /**
   *
   * This method writes out a line to the group_info GASH source file.
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

    Vector invids;
    Invid userInvid;
    String userName;
    String contract;
    String description;

    /* -- */

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(groupSchema.GROUPNAME);

    // currently in the GASH schema, we skip group passwords

    gid = ((Integer) object.getFieldValueLocal(groupSchema.GID)).intValue();
    
    invids = object.getFieldValuesLocal(groupSchema.USERS);

    if (invids == null)
      {
	// System.err.println("GASHBuilder.writeGroupLine(): null user list for group " + groupname);
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
    result.append("::");
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

    // okay, this marks the end of what we care about for the NIS
    // group map.  We'll check the line length here, even though we
    // may add more GASH-type stuff afterwards.  All of that gets
    // skipped when it comes time to make the maps.

    if (result.length() > 1024)
      {
	System.err.println("GASHBuilder.writeGroupLine(): Warning!  group " + 
			   groupname + " overflows the GASH line length!");
      }

    description = (String) object.getFieldValueLocal(groupSchema.DESCRIPTION);

    Invid contractLink = (Invid) object.getFieldValueLocal(groupSchema.CONTRACTLINK);

    if (contractLink != null)
      {
	contract = getLabel(contractLink);
      }
    else
      {
	contract = (String) object.getFieldValueLocal(groupSchema.CONTRACT);
      }

    result.append(":");
    result.append(contract);
    result.append(":");
    result.append(description);

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a line to the group.owner source file.
   *
   * The lines in this file look like the following.
   *
   * adgacc:ITL,ATL
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this user line
   *
   */

  private void writeGroupOwnerLine(DBObject object, PrintWriter writer)
  {
    String groupname;
    Vector ownerList;
    Vector ownerStringList = new Vector();
    
    /* -- */

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(groupSchema.GROUPNAME);
    ownerList = object.getFieldValuesLocal(SchemaConstants.OwnerListField);

    if (ownerList != null)
      {
	for (int i = 0; i < ownerList.size(); i++)
	  {
	    ownerStringList.addElement(getLabel((Invid) ownerList.elementAt(i)));
	  }
      }

    // now build our output line

    result.append(groupname);
    result.append(":");
    
    for (int i = 0; i < ownerStringList.size(); i++)
      {
	if (i != 0)
	  {
	    result.append(",");
	  }

	result.append((String) ownerStringList.elementAt(i));
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method generates a Netgroup file.
   *
   */

  private boolean writeNetgroupFile()
  {
    PrintWriter netgroupFile = null;
    DBObject netgroup;
    Enumeration netgroups;

    /* -- */

    try
      {
	netgroupFile = openOutFile(path + "netgroup", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeNetgroup(): couldn't open netgroup file: " + ex);
      }

    try
      {
	// first the user netgroups

	netgroups = enumerateObjects((short) 270);

	while (netgroups.hasMoreElements())
	  {
	    netgroup = (DBObject) netgroups.nextElement();
	
	    writeUserNetgroup(netgroup, netgroupFile);
	  }

	// now the system netgroups
    
	netgroups = enumerateObjects((short) 271);

	while (netgroups.hasMoreElements())
	  {
	    netgroup = (DBObject) netgroups.nextElement();
	
	    writeSystemNetgroup(netgroup, netgroupFile);
	  }
      }
    finally
      {
	netgroupFile.close();
      }

    return true;
  }

  /**
   *
   * This method writes out a single user netgroup out to disk,
   * wrapping the netgroup if it gets too long.
   *
   * omg-u	csd-u (-,broccol,) (-,gomod,) (-,etcrh,)
   *
   */

  private void writeUserNetgroup(DBObject object, PrintWriter writer)
  {
    StringBuffer buffer = new StringBuffer();

    String name;
    Vector users;
    Vector memberNetgroups;

    Invid ref;
    String refLabel;

    int lengthlimit;
    int subgroup = 1;
    String subname;

    /* -- */

    name = (String) object.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);
    users = object.getFieldValuesLocal(userNetgroupSchema.USERS);
    memberNetgroups = object.getFieldValuesLocal(userNetgroupSchema.MEMBERGROUPS);

    // NIS limits the length of a line to 1024 characters.
    // If the line looks like it'll go over, we'll truncate
    // it, put in an entry to link the netgroup with a
    // sub netgroup for continuation.

    // Thus, we want to save enough space to be able to put the link
    // information at the end.  We reduce it by a further 6 chars to
    // leave space for the per-entry syntax.

    // We could do this check during our buffer building loop, but by
    // doing it up front we guarantee that we're never going to exceed
    // the real limit during any single iteration of our netgroup line
    // construction without having to constantly be adding 6 to the
    // item length.

    lengthlimit = 900 - name.length() - 6;

    buffer.append(name);

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    ref = (Invid) memberNetgroups.elementAt(i);
	    refLabel = getLabel(ref);
	    
	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		if (subgroup > 1)
		  {
		    subname = name + "-ext" + subgroup;
		  }
		else
		  {
		    subname = name + "-ext";
		  }
		
		buffer.append(" ");
		buffer.append(subname);
		writer.println(buffer.toString());
		buffer = new StringBuffer();
		buffer.append(subname);
		subgroup++;
	      }
	    
	    buffer.append(" ");
	    buffer.append(refLabel);
	  }
      }

    if (users != null)
      {
	for (int i = 0; i < users.size(); i++)
	  {
	    ref = (Invid) users.elementAt(i);
	    refLabel = getLabel(ref);

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		if (subgroup > 1)
		  {
		    subname = name + "-ext" + subgroup;
		  }
		else
		  {
		    subname = name + "-ext";
		  }
		
		buffer.append(" ");
		buffer.append(subname);
		writer.println(buffer.toString());
		buffer = new StringBuffer();
		buffer.append(subname);
		subgroup++;
	      }

	    buffer.append(" ");
	    buffer.append("(-,");
	    buffer.append(refLabel);
	    buffer.append(",)");
	  }
      }

    writer.println(buffer.toString());
  }

  /**
   *
   * This method writes out a single system netgroup out to disk,
   * wrapping the netgroup if it gets too long.
   *
   * omg-s	csd-s (csdsun1.arlut.utexas.edu,-,) (ns1.arlut.utexas.edu,-,)
   *
   */

  private void writeSystemNetgroup(DBObject object, PrintWriter writer)
  {
    StringBuffer buffer = new StringBuffer();

    String name;
    Vector systems;
    Vector memberNetgroups;

    Invid ref;
    String refLabel;

    int lengthlimit;
    int subgroup = 1;
    String subname;

    /* -- */

    name = (String) object.getFieldValueLocal(systemNetgroupSchema.NETGROUPNAME);
    systems = object.getFieldValuesLocal(systemNetgroupSchema.SYSTEMS);
    memberNetgroups = object.getFieldValuesLocal(systemNetgroupSchema.MEMBERGROUPS);

    // NIS limits the length of a line to 1024 characters.
    // If the line looks like it'll go over, we'll truncate
    // it, put in an entry to link the netgroup with a
    // sub netgroup for continuation.

    // Thus, we want to save enough space to be able to put
    // the link information at the end.  We reduce it by
    // a further 6 chars to leave space for the per-entry
    // syntax.

    // We could do this check during our buffer building loop, but by
    // doing it up front we guarantee that we're never going to exceed
    // the real limit during any single iteration of our netgroup line
    // construction without having to constantly be adding 6 to the
    // item length.

    lengthlimit = 900 - name.length() - 6;

    buffer.append(name);

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    ref = (Invid) memberNetgroups.elementAt(i);
	    refLabel = getLabel(ref);

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		if (subgroup > 1)
		  {
		    subname = name + "-ext" + subgroup;
		  }
		else
		  {
		    subname = name + "-ext";
		  }
		
		buffer.append(" ");
		buffer.append(subname);
		writer.println(buffer.toString());
		buffer = new StringBuffer();
		buffer.append(subname);
		subgroup++;
	      }

	    buffer.append(" ");
	    buffer.append(refLabel);
	  }
      }

    if (systems != null)
      {
	for (int i = 0; i < systems.size(); i++)
	  {
	    ref = (Invid) systems.elementAt(i);
	    refLabel = getLabel(ref);
	    refLabel += dnsdomain;

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		if (subgroup > 1)
		  {
		    subname = name + "-ext" + subgroup;
		  }
		else
		  {
		    subname = name + "-ext";
		  }
		
		buffer.append(" ");
		buffer.append(subname);
		writer.println(buffer.toString());
		buffer = new StringBuffer();
		buffer.append(subname);
		subgroup++;
	      }

	    buffer.append(" ");
	    buffer.append("(");
	    buffer.append(refLabel);
	    buffer.append(",-,)");
	  }
      }

    writer.println(buffer.toString());
  }

  /**
   *
   * This method generates a simplified user netgroup file, which maps
   * user netgroups to user names without including any system
   * netgroups or sub-groups.  effectively, this maps netgroup names
   * to the transitive closure of members.
   *
   */

  private boolean writeUserNetgroupFile()
  {
    PrintWriter writer = null;
    DBObject netgroup;
    Enumeration netgroups;

    String name;

    Hashtable members = new Hashtable();
    Enumeration users;

    /* -- */

    try
      {
	writer = openOutFile(path + "netgroup.users", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeUserNetgroup(): couldn't open netgroup file: " + ex);
      }

    try
      {
	// first the user netgroups

	netgroups = enumerateObjects((short) 270);

	while (netgroups.hasMoreElements())
	  {
	    netgroup = (DBObject) netgroups.nextElement();

	    name = (String) netgroup.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);

	    members.clear();
	    unionizeMembers(netgroup, members);

	    if (members.size() > 0)
	      {
		writer.print(name);

		users = members.elements();

		while (users.hasMoreElements())
		  {
		    name = (String) users.nextElement();
		    
		    writer.print(" ");
		    writer.print(name);
		  }
		
		writer.println();
	      }
	  }
      }
    finally
      {
	writer.close();
      }

    return true;
  }

  /**
   *
   * Recursive helper method for writeUserNetgroupFile().. takes
   * a netgroup object and a hashtable, inserts all user members
   * in the netgroup object into the hash, then calls itself
   * on all member groups in the netgroup.
   *
   */

  private void unionizeMembers(DBObject netgroup, Hashtable hash)
  {
    Vector users;
    Vector memberNetgroups;

    Invid ref;
    String member;
    DBObject subNetgroup;

    /* -- */

    memberNetgroups = netgroup.getFieldValuesLocal(userNetgroupSchema.MEMBERGROUPS);
    users = netgroup.getFieldValuesLocal(userNetgroupSchema.USERS);

    if (users != null)
      {
	for (int i = 0; i < users.size(); i++)
	  {
	    ref = (Invid) users.elementAt(i);
	    member = getLabel(ref);
	    hash.put(member, member);
	  }
      }

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    subNetgroup = getObject((Invid)memberNetgroups.elementAt(i));
	    unionizeMembers(subNetgroup, hash);
	  }
      }
  }

  /**
   *
   * This method generates an auto.vol file, along with auto.home.*
   * files for all automounter records in the Ganymede database.
   *
   */

  private boolean writeAutoMounterFiles()
  {
    PrintWriter autoFile = null;
    DBObject map, obj;
    Enumeration vols, maps, entries;
    SharedStringBuffer buf = new SharedStringBuffer();
    String mountopts, mapname;
    String volName, sysName;
    Vector tempVect;
    Invid ref, userRef;

    /* -- */

    // first, write out the auto.vol file

    try
      {
	autoFile = openOutFile(path + "auto.vol", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeAutoMounterFiles(): couldn't open auto.vol: " + ex);
      }

    try
      {
	// find the volume definitions

	vols = enumerateObjects((short) 276);

	while (vols.hasMoreElements())
	  {
	    obj = (DBObject) vols.nextElement();

	    buf.setLength(0);

	    volName = (String) obj.getFieldValueLocal(volumeSchema.LABEL);

	    if (volName == null)
	      {
		Ganymede.debug("Couldn't emit a volume definition.. null label");
		continue;
	      }

	    buf.append(volName); // volume label
	    buf.append("\t\t");

	    // mount options.. NeXT's like this.  Ugh.

	    mountopts = (String) obj.getFieldValueLocal(volumeSchema.MOUNTOPTIONS); 

	    if (mountopts != null && !mountopts.equals(""))
	      {
		buf.append(mountopts);
		buf.append(" ");
	      }

	    sysName = getLabel((Invid) obj.getFieldValueLocal(volumeSchema.HOST));

	    if (sysName == null)
	      {
		Ganymede.debug("Couldn't emit proper volume definition for " + 
			       volName + ", no system found");
		continue;
	      }

	    buf.append(sysName);
	    buf.append(dnsdomain);

	    buf.append(":");

	    // mount path

	    buf.append((String) obj.getFieldValueLocal(volumeSchema.PATH)); 

	    autoFile.println(buf.toString());
	  }
      }
    finally
      {
	autoFile.close();
      }

    // second, write out all the auto.home.* files mapping user name
    // to volume name.  We depend on the GASH build scripts to convert
    // these to the form that NIS will actually use.. we could and
    // possibly will change this to write out the combined
    // auto.home/auto.vol info rather than forcing it to be done
    // after-the-fact via perl.

    maps = enumerateObjects((short) 277);

    while (maps.hasMoreElements())
      {
	map = (DBObject) maps.nextElement();

	mapname = (String) map.getFieldValueLocal(mapSchema.MAPNAME);

	try
	  {
	    autoFile = openOutFile(path + mapname, "gasharl");
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.writeAutoMounterFiles(): couldn't open " + 
			       mapname + ": " + ex);
	  }

	try
	  {
	    tempVect = map.getFieldValuesLocal(mapSchema.ENTRIES);

	    if (tempVect == null)
	      {
		autoFile.close();
		continue;
	      }

	    entries = tempVect.elements();

	    while (entries.hasMoreElements())
	      {
		ref = (Invid) entries.nextElement();
		obj = getObject(ref);

		// the entry is embedded in the user's record.. get
		// the user' id and label

		userRef = (Invid) obj.getFieldValueLocal(mapEntrySchema.CONTAININGUSER);

		if (userRef.getType() != SchemaConstants.UserBase)
		  {
		    throw new RuntimeException("Schema and/or database error");
		  }

		buf.setLength(0);
	    
		buf.append(getLabel(userRef)); // the user's name
		buf.append("\t");

		// nfs volume for this entry

		ref = (Invid) obj.getFieldValueLocal(mapEntrySchema.VOLUME); 

		if (ref == null || ref.getType() != (short) 276)
		  {
		    Ganymede.debug("Error, can't find a volume entry for user " + getLabel(userRef) +
				   " on automounter map " + mapname);
		    continue;
		  }

		buf.append(getLabel(ref));

		autoFile.println(buf.toString());
	      }
	  }
	finally
	  {
	    autoFile.close();
	  }
      }

    return true;
  }


  /**
   *
   * This method generates a file with the Mailman lists info.  This method must be run during
   * builderPhase1 so that it has access to the enumerateObjects() method
   * from our superclass. To be passed to Mailman server.
   *
   */

  private boolean writeMailmanListsFile()
  {
    PrintWriter mailman_sync_file = null;
    DBObject mailmanList;
    Enumeration mailmanLists;

    /* -- */

    try
      {
	mailman_sync_file = openOutFile(path + "ganymede_mailman_lists", "gasharl"); 
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeMailmanListsFile(): couldn't open mailman_sync_file file: " + ex);
      }

    try
      {
	// and the mailman mail lists
    
	mailmanLists = enumerateObjects((short) 260);  

	while (mailmanLists.hasMoreElements())
	  {
	    mailmanList = (DBObject) mailmanLists.nextElement();	
	    writeMailmanList(mailmanList, mailman_sync_file);
	  }
      }
    finally
      {
	mailman_sync_file.close();
      }

    return true;
  }

  

  /**
   *
   * This Method writes out a mailman list target line to the mailman lists file.<br><br>
   *
   * The mail list lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * listname\towneremail\tpassword
   *
   * </pre>
   *
   * Where listname is the name of the mailman list, owneremail is the 
   * email of the owner, and password is the password for the mailing list.
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   * 
   */

  private void writeMailmanList(DBObject object, PrintWriter writer)
  {
    result.setLength(0);

    String name = (String) object.getFieldValueLocal(MailmanListSchema.NAME);
    String ownerEmail = (String) object.getFieldValueLocal(MailmanListSchema.OWNEREMAIL);
    PasswordDBField passField = (PasswordDBField) object.getField(MailmanListSchema.PASSWORD);
    String password = (String) passField.getPlainText();

    Invid serverInvid = (Invid) object.getFieldValueLocal(MailmanListSchema.SERVER);
    DBObject server = getObject(serverInvid);    
    String hostname = getLabel((Invid) server.getFieldValueLocal(MailmanServerSchema.HOST));    

    result.append(hostname);
    result.append("\t");
    result.append(name);
    result.append("\t");
    result.append(ownerEmail);
    result.append("\t");
    result.append(password);
	
    writer.println(result.toString());
  }







  // ***
  //
  // The following private methods are used to support the DNS builder logic.
  //
  // ***

  /**
   *
   * This method generates an aliases_info file.  This method must be run during
   * builderPhase1 so that it has access to the enumerateObjects() method
   * from our superclass.
   *
   */

  private boolean writeAliasesFile()
  {
    PrintWriter aliases_info = null;
    DBObject user, group, external, MailmanList;
    Enumeration users, mailgroups, externals, MailmanLists;

    /* -- */

    try
      {
	aliases_info = openOutFile(path + "aliases_info", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeAliasesFile(): couldn't open aliases_info file: " + ex);
      }

    try
      {
	// our email aliases database is spread across three separate object
	// bases.

	users = enumerateObjects(SchemaConstants.UserBase);

	while (users.hasMoreElements())
	  {
	    user = (DBObject) users.nextElement();
	
	    writeUserAlias(user, aliases_info);
	  }

	// now the mail lists
    
	mailgroups = enumerateObjects((short) 274);

	while (mailgroups.hasMoreElements())
	  {
	    group = (DBObject) mailgroups.nextElement();
	
	    writeGroupAlias(group, aliases_info);
	  }

	// and the external mail addresses
    
	externals = enumerateObjects((short) 275);

	while (externals.hasMoreElements())
	  {
	    external = (DBObject) externals.nextElement();
	
	    writeExternalAlias(external, aliases_info);
	  }

	// add in Mailman Lists now.
    
	MailmanLists = enumerateObjects((short) 260);

	while (MailmanLists.hasMoreElements())
	  {
	    MailmanList = (DBObject) MailmanLists.nextElement();
	
	    writeMailmanListAlias(MailmanList, aliases_info);
	  }

      }
    finally
      {
	aliases_info.close();
      }

    return true;
  }
  

  /**
   * This method writes out a mailman alias line to the aliases_info GASH source file.<br><br>
   *
   * The mailman alias lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * test:test@arlut.utexas.edu
   *
   * </pre>
   *
   * Where listname is the name of the mailman list, owneremail is the 
   * email of the owner, and password is the password for the mailing list.
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   * 
   */

  private void writeMailmanListAlias(DBObject object, PrintWriter writer)
  {
    String name = (String) object.getFieldValueLocal(MailmanListSchema.NAME);
    Invid serverInvid = (Invid) object.getFieldValueLocal(MailmanListSchema.SERVER);
    DBObject server = getObject(serverInvid);    
    String hostname = getLabel((Invid) server.getFieldValueLocal(MailmanServerSchema.HOST));    

    result.setLength(0);
    result.append("<xxx>");
    result.append(name);
    result.append(":");
    result.append(name);
    result.append("@");
    result.append(hostname);
    result.append(".arlut.utexas.edu");
    writer.println(result.toString());


    // Loop over aliases target.    
    Vector aliases = object.getFieldValuesLocal(MailmanListSchema.ALIASES);

    if (aliases == null)
      {
	System.err.println("GASHBuilder.writeMailmanAliases(): null alias list for mailman list name " + name);
      }
    else
      {
	for (int i = 0; i < aliases.size(); i++)
	  {
	    String aliasName = (String) aliases.elementAt(i);	    

	    if (aliasName != null)
	      {
	        result.setLength(0);
	        result.append("<xxx>");
	        result.append(aliasName);
	        result.append(":");
	        result.append(aliasName);
		result.append("@");
	        result.append(hostname);
		result.append(".arlut.utexas.edu");
	        writer.println(result.toString());
	      }
	  }
      }
  }




  /**
   * This method writes out a user alias line to the aliases_info GASH source file.<br><br>
   *
   * The user alias lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * broccol:jonabbey, abbey, broccol, rust-admin:broccol@arlut.utexas.edu, broccol@csdsun4.arlut.utexas.edu
   *
   * </pre>
   *
   * The first item in the second field (jonabbey, above) is the 'signature'
   * alias, which the GASH makefile configures sendmail to rewrite as the
   * From: line.
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   */

  private void writeUserAlias(DBObject object, PrintWriter writer)
  {
    String username;
    String signature;
    Vector aliases;
    String alias;
    Vector addresses;
    String target;

    /* -- */

    result.setLength(0);

    username = (String) object.getFieldValueLocal(userSchema.USERNAME);
    signature = (String) object.getFieldValueLocal(userSchema.SIGNATURE);
    aliases = object.getFieldValuesLocal(userSchema.ALIASES);
    addresses = object.getFieldValuesLocal(userSchema.EMAILTARGET);

    result.append(username);
    result.append(":");
    result.append(signature);

    if (aliases != null)
      {
	// we don't include the username in the list of aliases,
	// but the build/gash stuff requires that it be included
	// in aliases_info, so if we didn't write it out as the
	// signature, make it the second alias.  The ordering
	// doesn't matter past the first, so this is ok.

	if (!signature.equals(username))
	  {
	    result.append(", ");
	    result.append(username);
	  }

	for (int i = 0; i < aliases.size(); i++)
	  {
	    alias = (String) aliases.elementAt(i);
	    
	    if (alias.equals(signature))
	      {
		continue;
	      }

	    result.append(", ");
	    result.append(alias);
	  }
      }

    result.append(":");

    if (addresses != null)
      {
	for (int i = 0; i < addresses.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }

	    target = (String) addresses.elementAt(i);

	    result.append(target);
	  }
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a mail list alias line to the aliases_info GASH source file.<br><br>
   *
   * The mail list lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * :oms:csd-news-dist-omg:csd_staff, granger, iselt, lemma, jonabbey@eden.com
   *
   * </pre>
   *
   * Where the leading colon identifies to the GASH makefile that it is a group
   * line and 'oms' is the GASH ownership code.  Ganymede won't try to emit
   * a GASH ownership code that could be used to load the aliases_info file
   * back into GASH.
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   *
   */

  private void writeGroupAlias(DBObject object, PrintWriter writer)
  {
    String groupname;
    Vector group_targets;
    Vector external_targets;
    Invid userInvid;
    String target;

    int lengthlimit_remaining;
    int subgroup = 1;
    String subname;

    /* -- */

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(emailListSchema.LISTNAME);
    group_targets = object.getFieldValuesLocal(emailListSchema.MEMBERS);
    external_targets = object.getFieldValuesLocal(emailListSchema.EXTERNALTARGETS);

    result.append(":xxx:");
    result.append(groupname);
    result.append(":");

    // NIS forces us to a 1024 character limit per key and value, we
    // need to truncate and extend to match, here.  We'll cut it down
    // to 900 to give ourselves some slack so we can write out our
    // chain link at the end of the line

    lengthlimit_remaining = 900 - result.length();

    if (group_targets != null)
      {
	for (int i = 0; i < group_targets.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }

	    userInvid = (Invid) group_targets.elementAt(i);

            target = getLabel(userInvid);

            if (2 + target.length() > lengthlimit_remaining)
              {
		if (subgroup > 1)
		  {
		    subname = groupname + "-gext" + subgroup;
		  }
		else
		  {
		    subname = groupname + "-gext";
		  }

                // point to the linked sublist, terminate this entry
                // line

                result.append(subname);
                result.append("\n");

                // and initialize the next line, containing the linked
                // sublist

                result.append(":xxx:");
                result.append(subname);
                result.append(":");
                lengthlimit_remaining = 900 - subname.length() - 6;
              }

	    result.append(target);
            lengthlimit_remaining = lengthlimit_remaining - (2 + target.length());
	  }
      }

    if (external_targets != null)
      {
	for (int i = 0; i < external_targets.size(); i++)
	  {
	    if ((i > 0) || (group_targets != null && group_targets.size() > 0))
	      {
		result.append(", ");
	      }

	    target = (String) external_targets.elementAt(i);

            if (2 + target.length() > lengthlimit_remaining)
              {
		if (subgroup > 1)
		  {
		    subname = groupname + "-gext" + subgroup;
		  }
		else
		  {
		    subname = groupname + "-gext";
		  }

                // point to the linked sublist, terminate this entry
                // line

                result.append(subname);
                result.append("\n");

                // and initialize the next line, containing the linked
                // sublist

                result.append(":xxx:");
                result.append(subname);
                result.append(":");
                lengthlimit_remaining = 900 - subname.length() - 6;
              }

	    result.append(target);
            lengthlimit_remaining = lengthlimit_remaining - (2 + target.length());
	  }
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a mail list alias line to the aliases_info GASH source file.<br><br>
   *
   * The mail list lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * &lt;omj&gt;abuse:abuse, postmaster:postmaster@ns1.arlut.utexas.edu
   *
   * </pre>
   *
   * Where the leading < identifies to GASH and the GASH makefile that
   * it is an external user line and 'omj' is the GASH ownership code.
   * Ganymede won't try to emit a GASH ownership code that could be
   * used to load the aliases_info file back into GASH.
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   * 
   */

  private void writeExternalAlias(DBObject object, PrintWriter writer)
  {
    String name;
    Vector aliases;
    String alias;
    Vector targets;
    String target;

    /* -- */

    result.setLength(0);

    name = (String) object.getFieldValueLocal(emailRedirectSchema.NAME);
    targets = object.getFieldValuesLocal(emailRedirectSchema.TARGETS);
    aliases = object.getFieldValuesLocal(emailRedirectSchema.ALIASES);

    result.append("<xxx>");
    result.append(name);
    result.append(":");
    result.append(name);	// the name is one of the aliases
	
    if (aliases != null)
      {
	for (int i = 0; i < aliases.size(); i++)
	  {
	    result.append(", ");

	    alias = (String) aliases.elementAt(i);
	    
	    result.append(alias);
	  }
      }

    result.append(":");

    // targets shouldn't ever be null, but i'm tired of having
    // NullPointerExceptions pop up then having to recompile to
    // fix.

    if (targets != null)
      {
	for (int i = 0; i < targets.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }
	    
	    target = (String) targets.elementAt(i);
	    
	    result.append(target);
	  }
      }

    writer.println(result.toString());
  }

  /**
   * <p>Samba version 1:</p>
   *
   * <p>broccol:12003:612EE67D1EFC2FB60B42BCD4578197DF:27A4F1E1E377CAD237C95B6146457F86:Jonathan Abbey,S321 CSD,3199,6803522:/home/broccol:/bin/tcsh</p>
   *
   */

  private boolean writeSambafileVersion1()
  {
    PrintWriter sambaFile = null;

    try
      {
	sambaFile = openOutFile(path + "smb.passwd", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeSambaFileVersion1(): couldn't open smb.passwd file: " + ex);
	return false;
      }

    try
      {
	Enumeration users = enumerateObjects(SchemaConstants.UserBase);

	while (users.hasMoreElements())
	  {
	    DBObject user = (DBObject) users.nextElement();

	    if (user.isInactivated())
	      {
		// we just leave inactivated users out of a Version 1
		// Samba password file

		continue;
	      }

	    String username = (String) user.getFieldValueLocal(userSchema.USERNAME);

	    PasswordDBField passField = (PasswordDBField) user.getField(userSchema.PASSWORD);

	    if (passField == null)
	      {
		continue;
	      }
	    
	    String hash1 = passField.getLANMANCryptText();

	    if (hash1 == null || hash1.equals(""))
	      {
		continue;
	      }

	    String hash2 = passField.getNTUNICODECryptText();

	    if (hash2 == null || hash2.equals(""))
	      {
		continue;
	      }

	    Integer uid = (Integer) user.getFieldValueLocal(userSchema.UID);

	    if (uid == null)
	      {
		continue;
	      }

	    String fullname = cleanString((String) user.getFieldValueLocal(userSchema.FULLNAME));
	    String room = cleanString((String) user.getFieldValueLocal(userSchema.ROOM));
	    String div = cleanString((String) user.getFieldValueLocal(userSchema.DIVISION));
	    String workphone = cleanString((String) user.getFieldValueLocal(userSchema.OFFICEPHONE));
	    String homephone = cleanString((String) user.getFieldValueLocal(userSchema.HOMEPHONE));
	    String homedir = cleanString((String) user.getFieldValueLocal(userSchema.HOMEDIR));
	    String shell = cleanString((String) user.getFieldValueLocal(userSchema.LOGINSHELL));
	    String composite = cleanString(fullname + "," + 
					   room + " " + div + "," + 
					   workphone + "," + homephone);

	    sambaFile.println(username + ":" + uid.intValue() + ":" + 
			      hash1 + ":" + hash2 + ":" +
			      composite + ":" + homedir + ":" + shell);
	  }
      }
    finally
      {
	sambaFile.close();
      }

    return true;
  }

  /**
   * <p>Samba version 2:</p>
   *
   * <p>broccol:12003:612EE67D1EFC2FB60B42BCD4578197DF:27A4F1E1E377CAD237C95B6146457F86:[U          ]:LCT-375412BE:</p>
   */

  private boolean writeSambafileVersion2()
  {
    String hash1 = null;
    String hash2 = null;

    PrintWriter sambaFile = null;

    try
      {
	sambaFile = openOutFile(path + "smb.passwd2", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeSambaFileVersion2(): couldn't open smb.passwd2 file: " + ex);
	return false;
      }

    try
      {
	Enumeration users = enumerateObjects(SchemaConstants.UserBase);

	while (users.hasMoreElements())
	  {
	    DBObject user = (DBObject) users.nextElement();

	    boolean inactivated = user.isInactivated();
	    
	    String username = (String) user.getFieldValueLocal(userSchema.USERNAME);

	    if (username == null || username.equals(""))
	      {
		continue;
	      }

	    PasswordDBField passField = (PasswordDBField) user.getField(userSchema.PASSWORD);

	    if (passField == null)
	      {
		inactivated = true;
	      }
	    
	    if (!inactivated)
	      {
		hash1 = passField.getLANMANCryptText();

		if (hash1 == null || hash1.equals(""))
		  {
		    inactivated = true;
		  }
		else
		  {
		    hash2 = passField.getNTUNICODECryptText();
		    
		    if (hash2 == null || hash2.equals(""))
		      {
			inactivated = true;
		      }
		  }
	      }

	    if (inactivated)
	      {
		hash1 = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
		hash2 = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
	      }

	    Integer uid = (Integer) user.getFieldValueLocal(userSchema.UID);

	    if (uid == null)
	      {
		continue;
	      }

	    // Samba 2.0 uses a flag string with 11 spaces and/or flag chars
	    // between a pair of brackets.

	    String flagString;

	    if (inactivated)
	      {
		flagString = "[UD         ]";
	      }
	    else
	      {
		flagString = "[U          ]";
	      }

	    // sanity checking

	    if (hash1 == null || hash1.length() != 32)
	      {
		throw new RuntimeException("bad LANMAN hash string: " + hash1);
	      }

	    if (hash2 == null || hash2.length() != 32)
	      {
		throw new RuntimeException("bad LANMAN hash string: " + hash1);
	      }

	    if (flagString.length() != 13)
	      {
		throw new RuntimeException("bad flag string");
	      }

	    String dateString = "LCT-" + dateToSMBHex(System.currentTimeMillis());

	    sambaFile.println(username + ":" + uid.intValue() + ":" + 
			      hash1 + ":" + hash2 + ":" +
			      flagString + ":" + dateString);
	  }
      }
    finally
      {
	sambaFile.close();
      }

    return true;
  }

  /**
   * <p>Samba knows how to handle an 8 byte hex encoded date from the
   * version 2 smb.passwd file.  This method takes a standard long
   * Java timecode and generates an 8 byte hex string which holds the
   * number of seconds since epoch.</p>
   *
   * <p><b><blink>Note that this will overflow in the year 2038.</blink></b></p>
   */

  private String dateToSMBHex(long timecode)
  {
    timecode = timecode / 1000;

    if (timecode < 0)
      {
	throw new IllegalArgumentException("Time code is out of range from before the epoch");
      }

    if (timecode > java.lang.Integer.MAX_VALUE)
      {
	throw new IllegalArgumentException("Time code has overflowed");
      }

    StringBuffer timeString = new StringBuffer();

    timeString.append(java.lang.Integer.toHexString((int) timecode));

    // make sure we pad it out to 8 characters if it is less

    if (timeString.length() < 8)
      {
	for (int i = timeString.length(); i < 8; i++)
	  {
	    timeString.insert(0, "0");
	  }
      }

    return timeString.toString().toUpperCase();
  }

  /**
   * <p>This method writes out a userSync.txt file which includes the
   * username, password, and invid.  This is used to allow generic
   * username/password synchronization for external SQL applications.</p>
   *
   * <p>We include the invid to provide a guaranteed unique identifier,
   * which will remain invariant even in the face of user rename.</p>
   *
   * <p>The userSync.txt file contains lines of the following format:</p>
   *
   * <PRE>
   * username|cryptText|invid|emailAddress|fullName
   * </PRE>
   *
   * <p>i.e.,</p>
   *
   * <PRE>
   * broccol|MMn1MiLY1ZbZ.|3:627|jonabbey@arlut.utexas.edu|Jonathan Abbey
   * </PRE>
   *
   * <p>Note that if the user is inactivated or the user's password is undefined,
   * the cryptText field in the userSync.txt file will be empty.  This
   * should be construed as having the user be unusable, *not* having the user
   * be usable with no password.</p>
   *
   */

  private boolean writeUserSyncFile()
  {
    PrintWriter out = null;

    /* -- */

    try
      {
	out = openOutFile(path + "userSync.txt", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.builderPhase1(): couldn't open userSync.txt file: " + ex);
	return false;
      }

    try
      {
	DBObject user;
	Enumeration users = enumerateObjects(SchemaConstants.UserBase);
	
	String username;
	Invid invid;
	String cryptText;
	String signature;
	String fullname;
	
	while (users.hasMoreElements())
	  {
	    user = (DBObject) users.nextElement();
	    
	    username = user.getLabel();
	    invid = user.getInvid();
	    signature = (String) user.getFieldValueLocal(userSchema.SIGNATURE);
	    fullname = (String) user.getFieldValueLocal(userSchema.FULLNAME);
	    cryptText = null;

	    if (!user.isInactivated())
	      {
		PasswordDBField passField = (PasswordDBField) user.getField(SchemaConstants.UserPassword);
		
		if (passField != null)
		  {
		    cryptText = passField.getMD5CryptText();
		  }
	      }

	    // ok, we've got a user with valid cryptText password
	    // info.  Write it.

	    out.print(username);
	    out.print("|");

	    if (cryptText != null)
	      {
		out.print(cryptText);
	      }

	    out.print("|");
	    out.print(invid);
	    out.print("|");
	    out.print(signature);
	    out.print("@arlut.utexas.edu");
	    out.print("|");
	    out.println(fullname);
	  }
      }
    finally
      {
	out.close();
      }

    return true;
  }

  /**
   * <p>This method writes out password and group files compatible with
   * with the Apache web server.  The password file is formatted according to
   * the standard .htpasswd file format, as follows:</p>
   *
   * <PRE>
   * user1:3vWsXVZDX5E7E
   * user2:DX5E7E3vWsXVZ
   * </PRE>
   *
   * <p>The group file is likewise formatted for use with Apache, as follows:</p>
   *
   * <PRE>
   * group1: user1 user2 user3
   * group2: user9 user2 user1
   * </PRE>
   *
   * <p>All users and all groups and user netgroups will be written to the
   * files.</p>
   *
   */

  private boolean writeHTTPfiles()
  {
    PrintWriter webPassword = null;
    PrintWriter webGroups = null;
    DBObject user;
    DBObject group;
    Enumeration users;
    Enumeration groups;

    try
      {
	webPassword = openOutFile(path + "httpd.pass", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeHTTPfiles(): couldn't open httpd.pass file: " + ex);
	return false;
      }

    try
      {
	users = enumerateObjects(SchemaConstants.UserBase);

	while (users.hasMoreElements())
	  {
	    user = (DBObject) users.nextElement();

	    if (user.isInactivated())
	      {
		continue;
	      }

	    PasswordDBField passField = (PasswordDBField) user.getField(SchemaConstants.UserPassword);

	    if (passField == null)
	      {
		continue;
	      }
	    
	    String password = passField.getUNIXCryptText();

	    if (password == null)
	      {
		continue;
	      }

	    // ok, we've got a user with valid UNIXCrypt password
	    // info.  Write it.

	    webPassword.print(user.getLabel());
	    webPassword.print(":");
	    webPassword.println(password);
	  }
      }
    finally
      {
	webPassword.close();
      }

    try
      {
	webGroups = openOutFile(path + "httpd.groups", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeHTTPfiles(): couldn't open httpd.groups file: " + ex);
	return false;
      }

    try
      {
	// first we write out UNIX account groups

	groups = enumerateObjects((short) 257);

	while (groups.hasMoreElements())
	  {
	    group = (DBObject) groups.nextElement();

	    if (group.isInactivated())
	      {
		continue;
	      }
	    
	    InvidDBField usersField = (InvidDBField) group.getField(groupSchema.USERS);
	    
	    if (usersField == null)
	      {
		continue;
	      }

	    String usersList = usersField.getValueString();

	    if (usersList == null || usersList.equals(""))
	      {
		continue;
	      }

	    webGroups.print(group.getLabel());
	    webGroups.print(": ");

	    // InvidDBField.getValueString() returns a comma separated
	    // list..  we want a space separated list for Apache

	    webGroups.println(usersList.replace(',',' '));
	  }

	// second we write out user netgroups

	groups = enumerateObjects((short) 270);

	while (groups.hasMoreElements())
	  {
	    group = (DBObject) groups.nextElement();

	    if (group.isInactivated())
	      {
		continue;
	      }

	    String usersList = VectorUtils.vectorString(netgroupMembers(group));

	    if (usersList == null || usersList.equals(""))
	      {
		continue;
	      }

	    webGroups.print(group.getLabel());
	    webGroups.print(": ");

	    // VectorUtils.vectorString() returns a comma separated
	    // list..  we want a space separated list for Apache

	    webGroups.println(usersList.replace(',',' '));
	  }
      }
    finally
      {
	webGroups.close();
      }

    return true;
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

  /** 
   * We can't have any : characters in the Samba password file other than
   * as field separators, so we strip any we find out.
   */

  private String cleanString(String in)
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
	if (ary[i] == ':')
	  {
	    continue;
	  }
	else
	  {
	    buffer.append(ary[i]);
	  }
      }

    return buffer.toString();
  }

  /** 
   * We can't have any : characters in passwords in the rshNT.txt
   * file we generate, since we use : chars as field separators in
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
	if (ary[i] == ':')
	  {
	    buffer.append("\\:");
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

  /**
   * <p>This method generates a file that maps i.p. addresses to mac addresses, system names,
   * room of the system, and usernames (if any).  This method must be run during
   * builderPhase1 so that it has access to the enumerateObjects() method
   * from our superclass.</p>
   */

  private boolean writeSysDataFile()
  {
    PrintWriter sys_dataFile = null;
    DBObject system;
    Enumeration systems;

    /* -- */

    try
      {
	sys_dataFile = openOutFile(path + "sysdata_info", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeSysFile(): couldn't open sysdata_info file: " + ex);
	return false;
      }

    try
      {
	// the hosts_info file is kind of squirrely.  We emit all of
	// the system lines first, followed by all of the interface
	// lines.

	systems = enumerateObjects((short) 263);

	while (systems.hasMoreElements())
	  {
	    system = (DBObject) systems.nextElement();
	
	    writeSysDataLine(system, sys_dataFile);
	  }
      }
    finally
      {
	sys_dataFile.close();
      }

    return true;
  }

  /**
   *
   * <p>Writes out one or more lines that maps I.P. addresses to MAC addresses,
   * system names, room of the system, and usernames (if any).</p>
   *
   * <p>Format:</p>
   *
   * <code>129.116.224.12|01:02:03:04:05:06|sysname|room|username</code>
   *
   */ 

  private void writeSysDataLine(DBObject object, PrintWriter writer)
  {
    String sysname;
    Vector interfaceInvids;
    Invid roomInvid;
    String room;
    String interfaceName;
    Invid primaryUserInvid;
    String primaryUser = null;
    String MACstring = null;
    String IPstring = null;
    String ownerString = null;

    /* -- */

    sysname = (String) object.getFieldValueLocal(systemSchema.SYSTEMNAME);
    sysname += dnsdomain;

    interfaceInvids = object.getFieldValuesLocal(systemSchema.INTERFACES);

    if (interfaceInvids != null)
      {
	for (int i = 0; i < interfaceInvids.size(); i++)
	  {
	    String local_sysname;
	    DBObject interfaceObj;

	    /* -- */

	    result.setLength(0);

	    interfaceObj = getObject((Invid) interfaceInvids.elementAt(i));

	    interfaceName = getInterfaceHostname(interfaceObj);
	    
	    if (interfaceName != null)
	      {
		local_sysname = interfaceName;
	      }
	    else
	      {
		local_sysname = sysname;
	      }

	    roomInvid = (Invid) object.getFieldValueLocal(systemSchema.ROOM);
	    
	    if (roomInvid != null)
	      {
		room = getLabel(roomInvid);
	      }
	    else
	      {
		room = "<unknown>";
	      }

	    primaryUserInvid = (Invid) object.getFieldValueLocal(systemSchema.PRIMARYUSER);
	    
	    if (primaryUserInvid != null)
	      {
		primaryUser = getLabel(primaryUserInvid);
	      }

	    try
	      {
		IPstring = interfaceObj.getField(interfaceSchema.ADDRESS).getValueString();
	      }
	    catch (RemoteException ex)
	      {
	      }
	    catch (NullPointerException ex)
	      {
	      }

	    try
	      {
		MACstring = interfaceObj.getField(interfaceSchema.ETHERNETINFO).getValueString();

		MACstring = MACstring.replace('-',':');
	      }
	    catch (RemoteException ex)
	      {
	      }
	    catch (NullPointerException ex)
	      {
	      }

	    if (IPstring == null || MACstring == null)
	      {
		continue;
	      }

	    try
	      {
		ownerString = object.getField(SchemaConstants.OwnerListField).getValueString();
	      }
	    catch (RemoteException ex)
	      {
	      }
	    catch (NullPointerException ex)
	      {
	      }

	    result.append(IPstring);
	    result.append("|");
	    result.append(MACstring);
	    result.append("|");
	    result.append(local_sysname);
	    result.append("|");
	    
	    if (ownerString == null || ownerString.equals(""))
	      {
		result.append("supergash");
	      }
	    else
	      {
		result.append(ownerString);
	      }

	    result.append("|");
	    result.append(room);
	    result.append("|");

	    if (primaryUser != null)
	      {
		result.append(primaryUser);
	      }

	    writer.println(result.toString());
	  }
      }
  }

  /**
   *
   * This method generates a hosts_info file.  This method must be run during
   * builderPhase1 so that it has access to the enumerateObjects() method
   * from our superclass.
   * 
   */

  private boolean writeSysFile()
  {
    PrintWriter hosts_info = null;
    DBObject system, interfaceObj;
    Enumeration systems, interfaces;

    /* -- */

    try
      {
	hosts_info = openOutFile(path + "hosts_info", "gasharl");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeSysFile(): couldn't open hosts_info file: " + ex);
	return false;
      }

    try
      {
	// the hosts_info file is kind of squirrely.  We emit all of
	// the system lines first, followed by all of the interface
	// lines.

	systems = enumerateObjects((short) 263);

	while (systems.hasMoreElements())
	  {
	    system = (DBObject) systems.nextElement();
	
	    writeSystem(system, hosts_info);
	  }

	// now the interfaces
    
	interfaces = enumerateObjects((short) 265);

	while (interfaces.hasMoreElements())
	  {
	    interfaceObj = (DBObject) interfaces.nextElement();
	
	    writeInterface(interfaceObj, hosts_info);
	  }
      }
    finally
      {
	hosts_info.close();
      }

    return true;
  }

  /**
   *
   * This method writes out a type 1 line to the hosts_info DNS source file.<br><br>
   *
   * The lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * ns1.arlut.utexas.edu, ns1b ns1d ns1f ns1e ns1z ns1g ns1h ns1i ns1j ns1k ns1l ns1a ns1c ns1m , \
   * news imap-server arlvs1 mail-firewall mail mailhost pop-server ftp sunos sunos2 wais fs1 gopher \
   * cso www2 ldap-server www : gl, halls, gil, broccol : S219 : Servers : Sun : SparcCenter 2000 : 2.5.1 : 
   *
   * </pre>
   *
   * @param object An object from the Ganymede system object base
   * @param writer The destination for this system line
   *
   */

  private void writeSystem(DBObject object, PrintWriter writer)
  {
    String sysname;
    Vector interfaceInvids;
    Vector interfaceNames = new Vector();
    Vector sysAliases;
    Invid roomInvid;
    String room;
    Invid typeInvid;
    String type;
    String manufacturer;
    String model;
    String os;
    String interfaceName;
    Invid primaryUserInvid;
    String primaryUser = null;

    /* -- */

    result.setLength(0);

    sysname = (String) object.getFieldValueLocal(systemSchema.SYSTEMNAME);
    sysname += dnsdomain;

    interfaceInvids = object.getFieldValuesLocal(systemSchema.INTERFACES);

    if (interfaceInvids != null)
      {
	for (int i = 0; i < interfaceInvids.size(); i++)
	  {
	    interfaceName = getInterfaceHostname(getObject((Invid) interfaceInvids.elementAt(i)));
	    
	    if (interfaceName != null)
	      {
		interfaceNames.addElement(interfaceName);
	      }
	  }
      }

    sysAliases = object.getFieldValuesLocal(systemSchema.SYSTEMALIASES);

    roomInvid = (Invid) object.getFieldValueLocal(systemSchema.ROOM);

    if (roomInvid != null)
      {
	room = getLabel(roomInvid);
      }
    else
      {
	room = "<unknown>";
      }

    typeInvid = (Invid) object.getFieldValueLocal(systemSchema.SYSTEMTYPE);

    if (typeInvid != null)
      {
	type = getLabel(typeInvid);
      }
    else
      {
	type = "<unknown>";
      }

    manufacturer = (String) object.getFieldValueLocal(systemSchema.MANUFACTURER);

    model = (String) object.getFieldValueLocal(systemSchema.MODEL);

    os = (String) object.getFieldValueLocal(systemSchema.OS);

    primaryUserInvid = (Invid) object.getFieldValueLocal(systemSchema.PRIMARYUSER);

    if (primaryUserInvid != null)
      {
	primaryUser = getLabel(primaryUserInvid);
      }

    // now build our output line

    result.append(sysname);
    result.append(", ");

    // interfaceNames may be empty, but never null

    for (int i = 0; i < interfaceNames.size(); i++)
      {
	result.append((String) interfaceNames.elementAt(i));
	result.append(" ");
      }
    
    result.append(", ");

    if (sysAliases != null)
      {
	for (int i = 0; i < sysAliases.size(); i++)
	  {
	    result.append((String) sysAliases.elementAt(i));
	    result.append(" ");
	  }
      }

    result.append(": : ");	// no admins
    result.append(room);
    result.append(" : ");
    result.append(type);
    result.append(" : ");
    result.append(manufacturer);
    result.append(" : ");
    result.append(model);
    result.append(" : ");
    result.append(os);
    result.append(" : ");

    if (primaryUser != null)
      {
	result.append(primaryUser);
      }

    if (result.length() > 1024)
      {
	System.err.println("GASHBuilder.writeSystem(): Warning!  hosts_info line " + 
			   sysname + " overflows the GASH line length!");
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a type 2 line to the hosts_info DNS source file.<br><br>
   *
   * The lines in this file look like the following:<br><br>
   *
   * <pre>
   *
   * >ns1a, ns1.arlut.utexas.edu, hostalias : 129.116.240.2 : 8-0-20-1b-d7-23
   *
   * </pre>
   *
   * for a multi-host system, or 
   *
   * <pre>
   *
   * >, sgdmac201.arlut.utexas.edu,  : 129.116.208.201 : 0-0-89-1-c4-c
   *
   * </pre>
   *
   * for a single-interface system.
   *
   * @param object An object from the Ganymede system object base
   * @param writer The destination for this system line
   *
   */

  private void writeInterface(DBObject object, PrintWriter writer)
  {
    String hostname = null;
    String sysname;
    String IPString;
    String MAC;
    Vector hostAliases = null;

    IPDBField ipField;

    /* -- */

    result.setLength(0);

    // we need to assemble the information that gash uses for our output

    MAC = (String) object.getFieldValueLocal(interfaceSchema.ETHERNETINFO);

    // We want to use dashes to separate the hex bytes in our ethernet addr

    MAC = MAC.replace(':','-');

    // an interface is contained in the associated system, so we check our
    // containing object for its name.. we assume that this interface *does*
    // have a containing field (it's embedded, so it must, eh?), so we don't
    // check for null container field here.

    sysname = getLabel((Invid) object.getFieldValueLocal(SchemaConstants.ContainerField));
    sysname += dnsdomain;

    // an interface can theoretically have multiple IP records and DNS records, but
    // GASH only supported one.

    // get the IP address for this interface

    ipField = (IPDBField) object.getField(interfaceSchema.ADDRESS);

    if (ipField == null)
      {
	System.err.println("GASHBuilder.writeInterface(): WARNING!  Interface for " + sysname + 
			   " has no IP address!  Skipping!");
	return;
      }

    if (!ipField.isIPV4())
      {
	System.err.println("GASHBuilder.writeInterface(): WARNING!  Interface for " + sysname + 
			   " has an IPV6 record!  This isn't compatible with the GASH makefiles!  Skipping!");
	return;
      }

    IPString = ipField.getValueString();

    // and the DNS info

    hostname = (String) object.getFieldValueLocal(interfaceSchema.NAME);

    hostAliases = object.getFieldValuesLocal(interfaceSchema.ALIASES);

    // now build our output line

    result.append(">");

    if (hostname != null)
      {
	result.append(hostname);
      }

    result.append(", ");

    result.append(sysname);

    result.append(",");

    if (hostAliases != null)
      {
	for (int i = 0; i < hostAliases.size(); i++)
	  {
	    result.append(" ");
	    result.append((String) hostAliases.elementAt(i));
	  }
      }

    result.append(" : ");
    result.append(IPString);
    result.append(" : ");
    result.append(MAC);

    if (result.length() > 1024)
      {
	System.err.println("GASHBuilder.writeInterface(): Warning!  hosts_info type 2 line " + 
			   ((hostname == null) ? sysname : hostname) +
			   " overflows the GASH line length!");
      }

    writer.println(result.toString());
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
   * This method writes out an excerpt of ISC DHCP server
   * configuration file from the data in the Ganymede data store.
   *
   * This file will need to be elaborated by an external script to
   * create the complete dhcpd.conf file.
   */

  private boolean writeDHCPFile()
  {
    PrintWriter dhcp_dataFile = null;
    DBObject system;
    Enumeration systems;

    /* -- */

    this.customOptions = new HashSet();

    try
      {
        try
          {
            dhcp_dataFile = openOutFile(path + "dhcpd_info", "gasharl");
          }
        catch (IOException ex)
          {
            Ganymede.debug("GASHBuilderTask.writeDHCPFile(): couldn't open dhcpd_info file: " + ex);

            return false;
          }

        dhcp_dataFile.println("# Generated by Ganymede GASHBuilderTask, revision $Rev$");
        dhcp_dataFile.println("# " + new Date().toString());
        dhcp_dataFile.println("#");
        dhcp_dataFile.println("# Per host data");
        dhcp_dataFile.println("#===============================================================================");

        try
          {
            systems = enumerateObjects((short) 263);

            while (systems.hasMoreElements())
              {
                system = (DBObject) systems.nextElement();

                writeDHCPInfo(system, dhcp_dataFile);
              }
          }
        finally
          {
            dhcp_dataFile.close();
          }

        /*
          The writeDHCPInfo() method call, above, keeps track of any
          custom DHCP options in use and stores them in the
          customOptions Set.

          We need to write out declarations for these customOptions so
          that they can be prepended to the dhcpd.conf file.

          The declarations have this form:

          # For Cisco IP phones
          #
          option local-cisco-tftp-server code 150 = ip-address;
        */

        try
          {
            dhcp_dataFile = openOutFile(path + "dhcpd_custom", "gasharl");
          }
        catch (IOException ex)
          {
            Ganymede.debug("GASHBuilderTask.writeDHCPFile(): couldn't open dhcpd_custom file: " + ex);

            return false;
          }

        // if customOptions.size() is 0, we'll wind up with an empty
        // dhcpd_custom file, which is just what we want to happen.

        try
          {
            if (customOptions.size() > 0)
              {
                dhcp_dataFile.println("# Generated by Ganymede GASHBuilderTask, revision $Rev$");
                dhcp_dataFile.println("# " + new Date().toString());
                dhcp_dataFile.println("#");
                dhcp_dataFile.println("# Custom Option Declarations");
                dhcp_dataFile.println("#===============================================================================");

                Iterator it = this.customOptions.iterator();

                // loop once to find custom option spaces

                HashSet foundOptions = new HashSet();

                while (it.hasNext())
                  {
                    Invid optionInvid = (Invid) it.next();
                    DBObject obj = getObject(optionInvid);

                    if (obj != null)
                      {
                        String name = (String) obj.getFieldValueLocal(dhcpOptionSchema.OPTIONNAME);

                        if (name.indexOf('.') != -1)
                          {
                            String optionSpace = name.substring(0, name.indexOf('.'));

                            if (!foundOptions.contains(optionSpace))
                              {
                                dhcp_dataFile.println("option space " + optionSpace + ";");
                                foundOptions.add(optionSpace);
                              }
                          }

                      }
                  }

                // loop again to declare our custom options

                it = this.customOptions.iterator();

                while (it.hasNext())
                  {
                    Invid optionInvid = (Invid) it.next();
                    DBObject obj = getObject(optionInvid);

                    if (obj != null)
                      {
                        String name = (String) obj.getFieldValueLocal(dhcpOptionSchema.OPTIONNAME);
                        String type = (String) obj.getFieldValueLocal(dhcpOptionSchema.OPTIONTYPE);
                        Integer code = (Integer) obj.getFieldValueLocal(dhcpOptionSchema.CUSTOMCODE);

                        // we need to use the expanded syntax for the
                        // option types for the ISC DHCP server

                        if (type.equals("uint8"))
                          {
                            type = "unsigned integer 8";
                          }
                        else if (type.equals("int8"))
                          {
                            type = "signed integer 8";
                          }
                        else if (type.equals("uint16"))
                          {
                            type = "unsigned integer 16";
                          }
                        else if (type.equals("int16"))
                          {
                            type = "signed integer 16";
                          }
                        else if (type.equals("uint32"))
                          {
                            type = "unsigned integer 16";
                          }
                        else if (type.equals("int32"))
                          {
                            type = "signed integer 16";
                          }

                        dhcp_dataFile.println("option " + name +
                                              " code " + code +
                                              " = " + type + ";");
                      }
                  }

                dhcp_dataFile.println("#===============================================================================");
              }
          }
        finally
          {
            dhcp_dataFile.close();
          }
      }
    finally
      {
        this.customOptions = null;
      }

    return true;
  }

  /**
   * This method writes out DHCP info for a single system to the dhcp_info file.
   *
   * @param object An object from the Ganymede system object base
   * @param writer The destination for this system line
   */

  private void writeDHCPInfo(DBObject object, PrintWriter writer)
  {
    String sysname = null;
    Vector interfaceInvids = null;
    DBObject interfaceObj = null;
    IPDBField ipField = null;
    String ipAddress = null;
    StringDBField macField = null;
    String macAddress = null;

    HashMap options = new HashMap(); 

    /* -- */

    result.setLength(0);

    interfaceInvids = object.getFieldValuesLocal(systemSchema.INTERFACES);

    if (interfaceInvids == null || interfaceInvids.size() > 1)
      {
        return;                 // we don't write out DHCP for systems
                                // with more than one interface
      }

    interfaceObj = getObject((Invid) interfaceInvids.elementAt(0));

    ipField = (IPDBField) interfaceObj.getField(interfaceSchema.ADDRESS);
    macField = (StringDBField) interfaceObj.getField(interfaceSchema.ETHERNETINFO);

    ipAddress = ipField.getEncodingString();
    macAddress = macField.getEncodingString();

    if (macAddress.equals("00:00:00:00:00:00"))
      {
        return;                 // don't write out DHCP for systems
                                // with unspecified mac addresses
      }

    Invid networkInvid = (Invid) interfaceObj.getFieldValueLocal(interfaceSchema.IPNET);

    DBObject networkObj = getObject(networkInvid);

    if (!networkObj.isSet(networkSchema.DHCP))
      {
        return;                 // no DHCP for this network
      }

    sysname = (String) object.getFieldValueLocal(systemSchema.SYSTEMNAME);

    // now let's see if we have any custom dhcp options for this
    // system.  we'll look up options from dhcp group membership
    // first, then from locally defined options.  In this way the
    // locally defined options will overwrite any group-derived
    // options.

    if (object.isDefined(systemSchema.DHCPGROUPS))
      {
        Vector dhcpGroupInvids = object.getFieldValuesLocal(systemSchema.DHCPGROUPS);

        for (int i = 0; dhcpGroupInvids != null && i < dhcpGroupInvids.size(); i++)
          {
            DBObject dhcpGroup = getObject((Invid) dhcpGroupInvids.elementAt(i));

            findDHCPOptions(options, dhcpGroup.getFieldValuesLocal(dhcpGroupSchema.OPTIONS));
          }
      }

    if (object.isDefined(systemSchema.DHCPOPTIONS))
      {
        findDHCPOptions(options, object.getFieldValuesLocal(systemSchema.DHCPOPTIONS));
      }

    // now build our output stanza

    result.append("host ");
    result.append(sysname);
    result.append("\n\t{\n");

    result.append("\thardware ethernet\t\t");
    result.append(macAddress);
    result.append(" ;\n");

    result.append("\tfixed-address\t\t\t");
    result.append(ipAddress);
    result.append(" ;\n");

    result.append("\toption host-name\t\t");
    result.append(quote(sysname));
    result.append(";\n");

    Iterator values = options.values().iterator();

    // first make sure we've declared any site-option-space that we'll need to use

    HashSet spaces = new HashSet();

    while (values.hasNext())
      {
        dhcp_entry entry = (dhcp_entry) values.next();

        if (entry.builtin)
          {
            continue;
          }

        if (entry.name.indexOf('.') != -1)
          {
            String spaceName = entry.name.substring(0, entry.name.indexOf('.'));

            if (spaces.size() == 0)
              {
                result.append("\tsite-option-space \"" + spaceName + "\";\n");
                spaces.add(spaceName);
              }
            else
              {
                if (!spaces.contains(spaceName))
                  {
                    Ganymede.debug("GASHBuilderTask: writeDHCPInfo() ran into problems with " + object.getLabel() + " due to conflicting DHCP option spaces.");
                  }
              }
          }
      }

    // second make sure that we've forced any mandatory options

    HashSet forcedOptions = new HashSet();

    values = options.values().iterator();

    while (values.hasNext())
      {
        dhcp_entry entry = (dhcp_entry) values.next();

        if (entry.forced)
          {
            forcedOptions.add(entry);
          }
      }

    if (forcedOptions.size() > 0)
      {
        StringBuffer hexOptionCodes = new StringBuffer();

        values = forcedOptions.iterator();

        while (values.hasNext())
          {
            dhcp_entry entry = (dhcp_entry) values.next();

            if (entry.forced && entry.code != 0)
              {
                hexOptionCodes.append(",");
                hexOptionCodes.append(java.lang.Integer.toHexString(entry.code));
              }
          }

        result.append("\tif exists dhcp-parameter-request-list {\n");
        result.append("\t\t# Ganymede forced dhcp options\n");
        result.append("\t\toption dhcp-parameter-request-list = concat(option dhcp-parameter-request-list");
        result.append(hexOptionCodes);
        result.append(");\n");
        result.append("\t}\n");
      }

    // third, let's write out the actual options for this host

    values = options.values().iterator();

    while (values.hasNext())
      {
        dhcp_entry entry = (dhcp_entry) values.next();

        int length = 0;

        if (!entry.builtin)
          {
            result.append("\toption ");
            length = 8;
          }
        else
          {
            result.append("\t");
            length = 0;
          }

        result.append(entry.name);

        if (length + entry.name.length() < 16)
          {
            result.append("\t\t\t");
          }
        else if (length + entry.name.length() < 24)
          {
            result.append("\t\t");
          }
        else
          {
            result.append("\t");
          }
        
        if (entry.type.equals("string") || entry.type.equals("text"))
          {
            result.append(quote(entry.value));
            result.append(" ;\n");
          }
        else
          {
            result.append(entry.value);
            result.append(" ;\n");
          }
      }

    result.append("\t} # END host");

    writer.println(result.toString());
  }

  private String quote(String in)
  {
    return "\"" + in + "\"";
  }

  private void findDHCPOptions(HashMap resultMap, Vector entryInvids)
  {
    if (entryInvids == null)
      {
        return;
      }

    for (int i = 0; i < entryInvids.size(); i++)
      {
        Invid entryInvid = (Invid) entryInvids.elementAt(i);

        DBObject entryObject = getObject(entryInvid);

        Invid optionInvid = (Invid) entryObject.getFieldValueLocal(dhcpEntrySchema.TYPE);
        String value = (String) entryObject.getFieldValueLocal(dhcpEntrySchema.VALUE);

        DBObject optionObject = getObject(optionInvid);

        String typeName = (String) optionObject.getFieldValueLocal(dhcpOptionSchema.OPTIONNAME);
        String typeString = (String) optionObject.getFieldValueLocal(dhcpOptionSchema.OPTIONTYPE);

        Integer typeCode = (Integer) optionObject.getFieldValueLocal(dhcpOptionSchema.CUSTOMCODE);

        int typecode = 0;

        if (typeCode != null)
          {
            typecode = typeCode.intValue();
          }

        resultMap.put(typeName, new dhcp_entry(typeName, typeString, value, optionObject.isSet(dhcpOptionSchema.BUILTIN), typecode, optionObject.isSet(dhcpOptionSchema.FORCESEND)));

        if (!optionObject.isSet(dhcpOptionSchema.BUILTIN) && optionObject.isSet(dhcpOptionSchema.CUSTOMOPTION))
          {
            customOptions.add(optionInvid);
          }
      }
  }
}

/**
 * Non-public data carrying "struct" class used to make things easier
 * for us as we assemble our DHCP output in the GASHBuilderTask.
 */

class dhcp_entry {

  public String name;
  public String type;
  public String value;
  public boolean builtin;
  public int code;
  public boolean forced;

  public dhcp_entry(String name, String type, String value, boolean builtin, int code, boolean forced)
  {
    this.name = name;
    this.type = type;
    this.value = value;
    this.builtin = builtin;
    this.code = code;
    this.forced = forced;
  }
}
