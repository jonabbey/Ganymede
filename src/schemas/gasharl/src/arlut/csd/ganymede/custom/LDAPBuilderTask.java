/*

   LDAPBuilderTask.java

   This class is intended to dump the Ganymede datastore to a Mac OS
   X/RFC 2307 LDAP environment by way of LDIF.
   
   Created: 22 March 2004
   Release: $Name:  $
   Version: $Revision: 1.8 $
   Last Mod Date: $Date: 2004/03/24 04:22:03 $
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

import net.iharder.xmlizable.Base64;

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

  private static String AppleOptions = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\"><plist version=\"1.0\"><dict><key>simultaneous_login_enabled</key><true/></dict></plist>";


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

    if (baseChanged(SchemaConstants.UserBase) || 
	baseChanged((short) 257))
      {
	Ganymede.debug("Need to build LDAP output");

	out = null;

	try
	  {
	    out = openOutFile(path + "users.ldif", "ldap");
	  }
	catch (IOException ex)
	  {
	    System.err.println("LDAPBuilderTask.builderPhase1(): couldn't open users.ldif file: " + ex);
	  }
	
	if (out != null)
	  {
	    try
	      {
		DBObject entity;
		Enumeration users = enumerateObjects(SchemaConstants.UserBase);
		
		while (users.hasMoreElements())
		  {
		    entity = (DBObject) users.nextElement();
		    
		    writeLDIFUserEntry(out, entity);
		  }
	      }
	    finally
	      {
		out.close();
	      }
	  }

	out = null;

	try
	  {
	    out = openOutFile(path + "groups.ldif", "ldap");
	  }
	catch (IOException ex)
	  {
	    System.err.println("LDAPBuilderTask.builderPhase1(): couldn't open groups.ldif file: " + ex);
	  }

	if (out != null)
	  {
	    try
	      {
		DBObject entity;
		Enumeration groups = enumerateObjects((short) 257);
		
		while (groups.hasMoreElements())
		  {
		    entity = (DBObject) groups.nextElement();
		    
		    writeLDIFGroupEntry(out, entity);
		  }
	      }
	    finally
	      {
		out.close();
	      }
	  }

	success = true;
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
	buildScript = buildScript + "ldapbuilder";
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

    Ganymede.debug("LDAPBuilderTask builderPhase2 completed");

    return true;
  }

  private void writeLDIFUserEntry(PrintWriter out, DBObject user)
  {
    Invid invid = user.getInvid();

    if (invid.getType() != SchemaConstants.UserBase)
      {
	throw new IllegalArgumentException("Wrong entity type");
      }

    writeLDIF(out, "dn", "uid=" + user.getLabel() + ",cn=users,dc=xserve");
    writeLDIF(out, "apple-generateduid", (String) user.getFieldValueLocal(userSchema.GUID).toString());
    writeLDIF(out, "sn", user.getLabel());

    PasswordDBField pdbf = (PasswordDBField) user.getField(userSchema.PASSWORD);

    if (pdbf != null)
      {
	writeBinaryLDIF(out, "userPassword", "{CRYPT}" + pdbf.getUNIXCryptText());
      }
    else
      {
	writeBinaryLDIF(out, "userPassword", "");
      }

    writeLDIF(out, "loginShell", user.getFieldValueLocal(userSchema.LOGINSHELL).toString());
    writeLDIF(out, "uidNumber", user.getFieldValueLocal(userSchema.UID).toString());

    DBObject group = getObject((Invid) user.getFieldValueLocal(userSchema.HOMEGROUP));
    writeLDIF(out, "gidNumber", group.getFieldValueLocal(groupSchema.GID).toString());

    writeLDIF(out, "authAuthority", ";basic;");
    writeLDIF(out, "objectClass", "inetOrgPerson");
    writeLDIF(out, "objectClass", "posixAccount");
    writeLDIF(out, "objectClass", "shadowAccount");
    writeLDIF(out, "objectClass", "apple-user");
    writeLDIF(out, "objectClass", "extensibleObject");
    writeLDIF(out, "objectClass", "organizationalPerson");
    writeLDIF(out, "objectClass", "top");
    writeLDIF(out, "objectClass", "person");
    writeLDIF(out, "uid", user.getLabel());

    String fullName = (String) user.getFieldValueLocal(userSchema.FULLNAME);

    if (fullName != null)
      {
	writeLDIF(out, "cn", fullName);
      }
    else
      {
	writeLDIF(out, "cn", user.getLabel());
      }

    String homeDirectory = (String) user.getFieldValueLocal(userSchema.HOMEDIR);

    if (homeDirectory != null)
      {
	writeLDIF(out, "homeDirectory", homeDirectory);
      }

    writeLDIF(out, "apple-mcxflags", AppleOptions);

    out.println();
  }

  private void writeLDIFGroupEntry(PrintWriter out, DBObject group)
  {
    Invid invid = group.getInvid();

    if (invid.getType() != 257)
      {
	throw new IllegalArgumentException("Wrong entity type");
      }

    writeLDIF(out, "dn", "cn=" + group.getLabel() + ",cn=groups,dc=xserve");
    writeLDIF(out, "gidNumber", group.getFieldValueLocal(groupSchema.GID).toString());

    Vector users = group.getFieldValuesLocal(groupSchema.USERS);

    if (users != null)
      {
	for (int i = 0; i < users.size(); i++)
	  {
	    DBObject user = getObject((Invid) users.elementAt(i));
	    
	    writeLDIF(out, "memberUid", user.getLabel());
	  }
      }

    writeLDIF(out, "objectClass", "posixGroup");
    writeLDIF(out, "objectClass", "apple-group");
    writeLDIF(out, "objectClass", "extensibleObject");

    if (users != null)
      {
	for (int i = 0; i < users.size(); i++)
	  {
	    DBObject user = getObject((Invid) users.elementAt(i));
	    
	    writeLDIF(out, "uniqueMember", "uid=" + user.getLabel() + ",cn=users,dc=xserve");
	  }
      }

    writeLDIF(out, "cn", group.getLabel());

    out.println();
  }

  /**
   * This private method writes out an attribute/value pair, doing whatever encoding
   * is necessary.
   */

  private void writeLDIF(PrintWriter out, String attribute, String value)
  {
    if (value == null)
      {
	return;
      }

    if (isBinary(value))
      {
	writeBinaryLDIF(out, attribute, value);
      }
    else
      {
	out.print(attribute);
	out.print(": ");
	out.println(value);
      }
  }

  /**
   * This private method writes out an attribute/value pair, doing whatever encoding
   * is necessary.
   */

  private void writeBinaryLDIF(PrintWriter out, String attribute, String value)
  {
    out.print(attribute);
    out.print(":: ");
    
    String binaryEncoded = fixNewlines(Base64.encodeString(value));

    if (binaryEncoded.indexOf('\n') != -1)
      {
	binaryEncoded = fixNewlines(binaryEncoded);
      }

    out.println(binaryEncoded);
  }

  private boolean isBinary(String value)
  {
    if (value.charAt(0) == ';' || value.charAt(0) == ' ')
      {
	return true;
      }

    for (int i = 0; i < value.length(); i++)
      {
	char x = value.charAt(i);

	if (x < 32 || x > 192)
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * Base64.encodeString breaks lines with bare newlines, not
   * newline-space pairs as is required in LDIF, so we may need to
   * tweak things a bit here.
   */

  private String fixNewlines(String input)
  {
    if (input.indexOf('\n') == -1)
      {
	return input;
      }

    StringBuffer outBuf = new StringBuffer();

    for (int i = 0; i < input.length(); i++)
      {
	char c = input.charAt(i);

	if (c == '\n')
	  {
	    outBuf.append("\n ");
	  }
	else
	  {
	    outBuf.append(c);
	  }
      }

    return outBuf.toString();
  }

  /*

  <?xml version="1.0" encoding="UTF-8"?> <!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd"> <plist version="1.0"> <dict> <key>simultaneous_login_enabled</key><true/></dict></plist>

  */
}