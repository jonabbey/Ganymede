/*

   LDAPBuilderTask.java

   This class is intended to dump the Directory Droid datastore to a Mac OS
   X/RFC 2307 LDAP environment by way of LDIF.
   
   Created: 22 March 2004

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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.SharedStringBuffer;
import arlut.csd.crypto.Base64;
import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.common.SchemaConstants;
import arlut.csd.ddroid.server.DBObject;
import arlut.csd.ddroid.server.Ganymede;
import arlut.csd.ddroid.server.GanymedeBuilderTask;
import arlut.csd.ddroid.server.PasswordDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 LDAPBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Directory Droid datastore to a Mac OS
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

  private static boolean tryKerberos = false;

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
	    throw new RuntimeException("LDAPBuilderTask.builderPhase1(): couldn't open users.ldif file: " + ex);
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
	    throw new RuntimeException("LDAPBuilderTask.builderPhase1(): couldn't open groups.ldif file: " + ex);
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
      }

    if (baseChanged(SchemaConstants.UserBase) ||
        /* Groups */
        baseChanged((short) 257) ||
        /* User netgroups */
        baseChanged((short) 270) ||
        /* Systems */
        baseChanged((short) 263) ||
        /* System netgroups */
        baseChanged((short) 271))
      {
        /* Reset some of our locals for this second pass */
	out = null;
    
        if (dnsdomain == null)
          {
            dnsdomain = System.getProperty("ganymede.gash.dnsdomain");

            if (dnsdomain == null)
              {
                throw new RuntimeException(
                    "LDAPBuilder not able to determine DNS domain.");
              }

            // prepend a dot if we need to

            if (dnsdomain.indexOf('.') != 0)
              {
                dnsdomain = "." + dnsdomain;
              }
          } 
          
	try
	  {
	    out = openOutFile(path + "netgroups.ldif", "ldap");
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("LDAPBuilderTask.builderPhase1(): couldn't open netgroups.ldif file: " + ex);
	  }
	  
	if (out != null)
	  {
	    try
	      {
		DBObject netgroup;
		
		// First we'll do the user netgroups
		Enumeration netgroups = enumerateObjects((short) 270);
		
		while (netgroups.hasMoreElements())
		  {
		    netgroup = (DBObject) netgroups.nextElement();
		    writeLDIFNetgroupEntry(out, netgroup);
		  }
		  
		// And now for the system netgroups
		netgroups = enumerateObjects((short) 271);
    
		while (netgroups.hasMoreElements())
		  {
		    netgroup = (DBObject) netgroups.nextElement();
		    writeLDIFNetgroupEntry(out, netgroup);
		  }
	      }
	    finally
	      {
		out.close();
	      }
	  }
      }

    Ganymede.debug("LDAPBuilderTask builderPhase1 completed");

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

    if (!tryKerberos)
      {
	// now write out the password.  If the user was inactivated, there
	// won't be a password.. to make sure that ldapdiff does the right
	// thing, we just won't emit a userPassword field in that case.

	PasswordDBField pdbf = (PasswordDBField) user.getField(userSchema.PASSWORD);

	if (pdbf != null)
	  {
	    String passText = pdbf.getSSHAHashText();

	    if (passText != null) {
	      // getSSHAHashText preformats the password for LDAP

	      writeBinaryLDIF(out, "userPassword", passText);
	    } else {
	      passText = pdbf.getUNIXCryptText();

	      if (passText != null) {
		writeBinaryLDIF(out, "userPassword", "{CRYPT}" + passText);
	      }
	    }
	  }
      }

    writeLDIF(out, "loginShell", user.getFieldValueLocal(userSchema.LOGINSHELL).toString());
    writeLDIF(out, "uidNumber", user.getFieldValueLocal(userSchema.UID).toString());

    DBObject group = getObject((Invid) user.getFieldValueLocal(userSchema.HOMEGROUP));
    writeLDIF(out, "gidNumber", group.getFieldValueLocal(groupSchema.GID).toString());

    if (tryKerberos)
      {
	writeLDIF(out, "authAuthority", "1.0;Kerberos:ARLUT.UTEXAS.EDU;");
      }
    else
      {
	writeLDIF(out, "authAuthority", ";basic;");
      }

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
  
  private void writeLDIFNetgroupEntry(PrintWriter out, DBObject netgroup)
  {
    String 
      name,
      membername;
    
    Vector
      contents, 
      memberNetgroups;
    
    Invid
      invid;
      
    /* Let's figure out what kind of netgroup this is: user or system */
    short typeid = netgroup.getTypeID();
    
    if (typeid == 270)
      {
        /* This is a user netgroup */
        name = (String) netgroup.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);
        contents = netgroup.getFieldValuesLocal(userNetgroupSchema.USERS);
        memberNetgroups = netgroup.getFieldValuesLocal(userNetgroupSchema.MEMBERGROUPS);
      }
    else
      {
        /* This is a system netgroup */
        name = (String) netgroup.getFieldValueLocal(systemNetgroupSchema.NETGROUPNAME);
        contents = netgroup.getFieldValuesLocal(systemNetgroupSchema.SYSTEMS);
        memberNetgroups = netgroup.getFieldValuesLocal(systemNetgroupSchema.MEMBERGROUPS);
      }
      
    /* Write out the LDIF header for this netgroup. We'll make the CN the netgroup name. */
    writeLDIF(out, "dn", "cn=" + name + ",cn=netgroups,dc=xserve");
    writeLDIF(out, "objectclass", "nisNetgroup");
    writeLDIF(out, "cn", name);
    
    if (contents != null)
      {
        for (Iterator iter = contents.iterator(); iter.hasNext();)
          {
            invid = (Invid) iter.next();
            membername = getLabel(invid);
            typeid = invid.getType();
    
            /* The members of this netgroup can either be User objects or System objects.
             * We need to write out the correct LDIF triple for each case. An LDIF netgroup
             * triple is of the form (hostname,username,domain), where each part is optional.
             */
            
            if (typeid == SchemaConstants.UserBase)
              {
                writeLDIF(out, "nisNetgroupTriple", "(," + membername + ",)");
              }
            else if (typeid == 263) 
              {
                writeLDIF(out, "nisNetgroupTriple", "(" + membername + dnsdomain + ",,)");
              }
          }
      }
     
    /* This part handles prining out what sub-netgroups belong to this one. This
     * is done by using the LDAP attribute "memberNisNetgroup". */
    
    if (memberNetgroups != null)
      {
        for (Iterator iter = memberNetgroups.iterator(); iter.hasNext();)
          {
            invid = (Invid) iter.next();
            membername = getLabel(invid);
            writeLDIF(out, "memberNisNetgroup", membername);
          }
      }
     
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
    
    String binaryEncoded = fixNewlines(Base64.encodeBytes(value.getBytes()));

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