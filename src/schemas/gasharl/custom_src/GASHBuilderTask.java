/*

   GASHBuilderTask.java

   This class is intended to dump the Ganymede datastore to GASH.
   
   Created: 21 May 1998
   Release: $Name:  $
   Version: $Revision: 1.14 $
   Last Mod Date: $Date: 1999/02/04 01:26:10 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.PathComplete;

import java.util.*;
import java.text.*;
import java.io.*;

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
  private static Runtime runtime = null;

  // ---

  private Date now = null;
  private boolean backedup = false;
  private StringBuffer result = new StringBuffer();

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
    boolean success = false;

    /* -- */

    backedup = false;

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
	    out = openOutFile(path + "user_info");
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.builderPhase1(): couldn't open user_info file: " + ex);
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

	success = true;
      }

    // group

    if (baseChanged((short) 257))
      {
	Ganymede.debug("Need to build group map");

	out = null;

	try
	  {
	    out = openOutFile(path + "group_info");
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.builderPhase1(): couldn't open group_info file: " + ex);
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

	success = true;
      }

    if (baseChanged(SchemaConstants.UserBase) || // users
	baseChanged((short) 274) || // mail lists
	baseChanged((short) 275)) // external mail addresses
      {
	Ganymede.debug("Need to build aliases map");

	if (writeAliasesFile())
	  {
	    success = true;
	  }
      }

    if (baseChanged((short) 271) || // system netgroups
	baseChanged((short) 270)) // user netgroups
      {
	Ganymede.debug("Need to build netgroup map");

	if (writeNetgroupFile())
	  {
	    success = true;
	  }
      }

    if (baseChanged((short) 277) || // automounter maps
	baseChanged((short) 276) || // nfs volumes
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
	success = true;
      }

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

    if (buildScript == null)
      {
	buildScript = path + "gashbuilder";
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
	Ganymede.debug(buildScript + " doesn't exist, not running external GASH build script");
      }

    return true;
  }

  // ***
  //
  // The following private methods are used to support the GASH builder logic.
  //
  // ***

  /**
   *
   * This method opens the specified file for writing out a text stream.
   *
   * If the files have not yet been backed up this run time, openOutFile()
   * will cause the files in Ganymede's output directory to be zipped up
   * before overwriting any files.
   *
   */

  private synchronized PrintWriter openOutFile(String filename) throws IOException
  {
    File
      file,
      oldFile;

    /* -- */

    if (!backedup)
      {
	String label;
	Date labelDate;

	if (lastRunTime != null)
	  {
	    labelDate = lastRunTime;
	  }
	else
	  {
	    if (now == null)
	      {
		now = new Date();
	      }

	    labelDate = now;
	  }

	DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", 
						    java.util.Locale.US);
	label=formatter.format(labelDate);

	backupFiles(label);

	backedup = true;
      }

    file = new File(filename);

    if (file.exists())
      {
	file.delete();
      }

    return new PrintWriter(new BufferedWriter(new FileWriter(file)));
  }

  /**
   *
   * This method writes out a line to the user_info GASH source file.
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
    String div;
    String officePhone;
    String homePhone;
    String directory;
    String shell;

    PasswordDBField passField;
    Vector invids;
    Invid groupInvid;
    DBObject group;

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
	System.err.println("GASHBuilder.writeUserLine(): null password for user " + username);
	cryptedPass = "**Nopass**";
      }

    uid = ((Integer) object.getFieldValueLocal(userSchema.UID)).intValue();

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

    if (result.length() > 1024)
      {
	System.err.println("GASHBuilder.writeGroupLine(): Warning!  user " + 
			   username + " overflows the GASH line length!");
      }

    writer.println(result.toString());
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

    PasswordDBField passField;
    Vector invids;
    Invid userInvid;
    String userName;

    /* -- */

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(groupSchema.GROUPNAME);

    // currently in the Ganymede schema, group passwords aren't in passfields.

    pass = (String) object.getFieldValueLocal(groupSchema.PASSWORD);
    gid = ((Integer) object.getFieldValueLocal(groupSchema.GID)).intValue();
    
    // we currently don't explicitly record the home group.. just take the first group
    // that the user is in.

    invids = object.getFieldValuesLocal(groupSchema.USERS);

    if (invids == null)
      {
	// System.err.println("GASHBuilder.writeUserLine(): null user list for group " + groupname);
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
	System.err.println("GASHBuilder.writeGroupLine(): Warning!  group " + 
			   groupname + " overflows the GASH line length!");
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
	netgroupFile = openOutFile(path + "netgroup");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeNetgroup(): couldn't open netgroup file: " + ex);
      }

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

    netgroupFile.close();

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
    int subgroup = 2;
    String subname;

    /* -- */

    name = (String) object.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);
    users = object.getFieldValuesLocal(userNetgroupSchema.USERS);
    memberNetgroups = object.getFieldValuesLocal(userNetgroupSchema.MEMBERGROUPS);

    // NIS limits the length of a line to 1024 characters.
    // If the line looks like it'll go over, we'll truncate
    // it, put in an entry to link the netgroup with a
    // sub netgroup for continuation.

    // Thus, we want to save enough space to be able to put
    // the link information at the end.  We reduce it by
    // a further 6 chars to leave space for the per-entry
    // syntax.

    lengthlimit = 1024 - name.length() - 6;

    buffer.append(name);

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    ref = (Invid) memberNetgroups.elementAt(i);
	    refLabel = getLabel(ref);
	    
	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		subname = name + subgroup;
		
		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
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
		subname = name + subgroup;

		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
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
   * This method writes out a single user netgroup out to disk,
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
    int subgroup = 2;
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

    lengthlimit = 1024 - name.length() - 6;

    buffer.append(name);

    if (memberNetgroups != null)
      {
	for (int i = 0; i < memberNetgroups.size(); i++)
	  {
	    ref = (Invid) memberNetgroups.elementAt(i);
	    refLabel = getLabel(ref);

	    if (buffer.length() + refLabel.length() > lengthlimit)
	      {
		subname = name + subgroup;

		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
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
		subname = name + subgroup;

		buffer.append(" ");
		buffer.append(subname);
		buffer.append("\n");
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
   * This method generates an auto.vol file, along with auto.home.*
   * files for all automounter records in the Ganymede database.
   *
   */

  private boolean writeAutoMounterFiles()
  {
    PrintWriter autoFile = null;
    DBObject map, obj, user;
    Enumeration vols, maps, entries;
    StringBuffer buf = new StringBuffer();
    String mountopts, mapname;
    String volName, sysName;
    Vector tempVect;
    Invid ref, userRef;

    /* -- */

    // first, write out the auto.vol file

    try
      {
	autoFile = openOutFile(path + "auto.vol");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeAutoMounterFiles(): couldn't open auto.vol: " + ex);
      }

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

	mountopts = (String) obj.getFieldValueLocal(volumeSchema.MOUNTOPTIONS); // mount options.. NeXT's like this.  Ugh.

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
	buf.append((String) obj.getFieldValueLocal(volumeSchema.PATH)); // mount path

	autoFile.println(buf.toString());
      }

    autoFile.close();

    // second, write out all the auto.home.* files mapping user name
    // to volume name.  We depend on the GASH build scripts to convert
    // these to the form that NIS will actually use.. we could and possibly
    // will change this to write out the combined auto.home/auto.vol info
    // rather than forcing it to be done after-the-fact via perl.

    maps = enumerateObjects((short) 277);

    while (maps.hasMoreElements())
      {
	map = (DBObject) maps.nextElement();

	mapname = (String) map.getFieldValueLocal(mapSchema.MAPNAME);

	try
	  {
	    autoFile = openOutFile(path + mapname);
	  }
	catch (IOException ex)
	  {
	    System.err.println("GASHBuilderTask.writeAutoMounterFiles(): couldn't open " + mapname + ": " + ex);
	  }

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

	    // the entry is embedded in the user's record.. get the user' id and label

	    userRef = (Invid) obj.getFieldValueLocal(mapEntrySchema.CONTAININGUSER);

	    if (userRef.getType() != SchemaConstants.UserBase)
	      {
		throw new RuntimeException("Schema and/or database error");
	      }

	    buf.setLength(0);
	    
	    buf.append(getLabel(userRef)); // the user's name
	    buf.append("\t");

	    ref = (Invid) obj.getFieldValueLocal(mapEntrySchema.VOLUME); // nfs volume for this entry

	    if (ref == null || ref.getType() != (short) 276)
	      {
		throw new RuntimeException("Schema and/or database error");
	      }

	    buf.append(getLabel(ref));

	    autoFile.println(buf.toString());
	  }

	autoFile.close();
      }

    return true;
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
    DBObject user, group, external;
    Enumeration users, mailgroups, externals;

    /* -- */

    try
      {
	aliases_info = openOutFile(path + "aliases_info");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeAliasesFile(): couldn't open aliases_info file: " + ex);
      }

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

    aliases_info.close();

    return true;
  }
  
  /**
   *
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
   *
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

    /* -- */

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(emailListSchema.LISTNAME);
    group_targets = object.getFieldValuesLocal(emailListSchema.MEMBERS);
    external_targets = object.getFieldValuesLocal(emailListSchema.EXTERNALTARGETS);

    result.append(":xxx:");
    result.append(groupname);
    result.append(":");

    if (group_targets != null)
      {
	for (int i = 0; i < group_targets.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }
	    
	    userInvid = (Invid) group_targets.elementAt(i);
	    
	    result.append(getLabel(userInvid));
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

    if (aliases != null)
      {
	for (int i = 0; i < aliases.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }
	    
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
	hosts_info = openOutFile(path + "hosts_info");
      }
    catch (IOException ex)
      {
	System.err.println("GASHBuilderTask.writeSysFile(): couldn't open hosts_info file: " + ex);
      }

    // the hosts_info file is kind of squirrely.  We emit all of the system lines
    // first, followed by all of the interface lines.

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

    hosts_info.close();

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
		interfaceName += dnsdomain;
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
	result.append(dnsdomain);
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

}
