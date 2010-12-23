 /*

   GASHBuilderTask.java

   This class is intended to dump the Ganymede datastore to GASH.

   Created: 21 May 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import arlut.csd.Util.CaseInsensitiveSet;
import arlut.csd.Util.FileOps;
import arlut.csd.Util.NullWriter;
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
import arlut.csd.ganymede.server.ServiceNotFoundException;
import arlut.csd.ganymede.server.ServiceFailedException;

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

  private static final String normalCategoryLabel = "normal";
  private static final String agencyCategoryLabel = "agency worker";

  // ---

  private Date now = null;
  private SharedStringBuffer result = new SharedStringBuffer();

  /**
   * We cache the normalCategory during the build cycle to make the
   * generation of export data for IRIS a bit more efficient.
   */

  private Invid normalCategory = null;

  /**
   * We cache the agencyCategory during the build cycle to make the
   * generation of export data for IRIS a bit more efficient.
   */

  private Invid agencyCategory = null;

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
   * Code run in builderPhase1() can call getObjects() and
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
            throw new RuntimeException("GASHBuilder not able to determine output directory.. need to set the ganymede.builder.output property in ganymede.properties.");
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
                for (DBObject user: getObjects(SchemaConstants.UserBase))
                  {
                    writeUserLine(user, out);
                  }
              }
            finally
              {
                out.close();
              }
          }

        writeMailDirect2();
        writeMailDirect3();     // the new gany_iris_export.txt file for Carrie
        writeSambafileVersion1();
        writeSambafileVersion2();
        writeUserSyncFile();
        writeExternalMailFiles();

        success = true;
      }

    if (baseChanged(userSchema.BASE) ||
        baseChanged(groupSchema.BASE) || // in case of rename
        baseChanged(userNetgroupSchema.BASE)) // ditto
      {
        writeHTTPfiles();

        success = true;
      }

    // group

    if (baseChanged(groupSchema.BASE) ||
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
            for (DBObject group: getObjects(groupSchema.BASE))
              {
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
        baseChanged(groupSchema.BASE) ||  // account groups
        baseChanged(userNetgroupSchema.BASE) || // user netgroups
        baseChanged(emailListSchema.BASE) || // mail lists
        baseChanged(emailRedirectSchema.BASE) || // external mail addresses
        baseChanged(MailmanListSchema.BASE)) // mailman lists
      {
        Ganymede.debug("Need to build aliases map");

        if (writeAliasesFile() && writeEmailLists())
          {
            success = true;
          }

        // the postfix versions of those files.
        // jgs, 15 feb 2010

        try
          {
            if (writeHashAliasesFile())
              {
                success = true;
              }
          }
        catch (IOException ex)
          {
            throw new RuntimeException(ex);
          }
      }

    if (baseChanged(MailmanListSchema.BASE)) // mailman lists
      {
        Ganymede.debug("Need to call mailman ns8 sync script");

        if (writeMailmanListsFile())
          {
            success = true;
          }
      }

    if (baseChanged(systemNetgroupSchema.BASE) || // system netgroups
        baseChanged(userNetgroupSchema.BASE) || // user netgroups
        baseChanged(SchemaConstants.UserBase) || // in case users were renamed
        baseChanged(systemSchema.BASE)) // in case systems were renamed
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

    if (baseChanged(mapSchema.BASE) || // automounter maps
        baseChanged(volumeSchema.BASE) || // nfs volumes
        baseChanged(systemSchema.BASE) || // in case systems were renamed
        baseChanged(SchemaConstants.UserBase) || // in case users were renamed
        baseChanged(mapEntrySchema.BASE)) // automounter map entries
      {
        Ganymede.debug("Need to build automounter maps");

        if (writeAutoMounterFiles())
          {
            success = true;
          }
      }

    if (baseChanged(systemSchema.BASE) || // system base
        baseChanged(networkSchema.BASE) || // I.P. Network base
        baseChanged(interfaceSchema.BASE)) // system interface base
      {
        Ganymede.debug("Need to build DNS tables");
        writeSysFile();
        writeSysDataFile();
        success = true;
      }

    if (baseChanged(systemSchema.BASE) ||
        baseChanged(networkSchema.BASE) ||
        baseChanged(interfaceSchema.BASE) ||
        baseChanged(dhcpGroupSchema.BASE) ||
        baseChanged(dhcpOptionSchema.BASE) ||
        baseChanged(dhcpEntrySchema.BASE) ||
        baseChanged(dhcpNetworkSchema.BASE))
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

    boolean startedOk = false;

    if (file.exists())
      {
        try
          {
            resultCode = FileOps.runProcess(buildScript);

	    startedOk = true;
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

	if (startedOk)
	  {
	    throw new ServiceFailedException("gashbuilder returned a failure code: " + resultCode);
	  }
	else
	  {
	    if (!file.exists())
	      {
		throw new ServiceNotFoundException("Couldn't find " + path);
	      }
	    else
	      {
		throw new ServiceNotFoundException("Couldn't run " + path);
	      }
	  }
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
        return;                 // no writeLine
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
        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
            String username = (String) user.getFieldValueLocal(SchemaConstants.UserUserName);
            String signature = (String) user.getFieldValueLocal(userSchema.SIGNATURE);
            String badgeNum = (String) user.getFieldValueLocal(userSchema.BADGE);
            Invid category = (Invid) user.getFieldValueLocal(userSchema.CATEGORY);

            if (username != null && signature != null && badgeNum != null &&
                category != null && category.equals(getNormalCategory()) && !user.isInactivated())
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
   * we write out a file that maps badge numbers to a
   * user's primary email address and user name for the
   * personnel office's phonebook database to use
   *
   * This method writes lines to the gany_iris_export.txt GASH output
   * file.
   *
   * The lines in this file look like the following.
   *
   * XXXXXXYYYYYYYYZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ
   *
   * Where XXXXXX is the badge number with trailing spaces if necessary,
   * YYYYYYYY is the username with trailing spaces if necessary (up to 8 chars),
   * and ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ is the email
   * address, with trailing spaces if necessary (up to 50 characters,
   * 32 plus '@arlut.utexas.edu'.
   *
   */

  private void writeMailDirect3()
  {
    PrintWriter out;
    HashMap<String, DBObject> map = new HashMap<String, DBObject>(); // map badge numbers to DBObject
    HashMap<String, String> results = new HashMap<String, String>(); // map badge numbers to strings

    /* -- */

    try
      {
        out = openOutFile(path + "gany_iris_export.txt", "gasharl");
      }
    catch (IOException ex)
      {
        System.err.println("GASHBuilderTask.builderPhase1(): couldn't open gany_iris_export.txt file: " + ex);
        return;
      }

    try
      {
        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
            String username = (String) user.getFieldValueLocal(SchemaConstants.UserUserName);
            String signature = (String) user.getFieldValueLocal(userSchema.SIGNATURE);
            String badgeNum = (String) user.getFieldValueLocal(userSchema.BADGE);
            Invid category = (Invid) user.getFieldValueLocal(userSchema.CATEGORY);

            if (username != null && signature != null && badgeNum != null &&
                category != null &&
                (category.equals(getNormalCategory()) || category.equals(getAgencyCategory())) &&
                !user.isInactivated())
              {
                if (map.containsKey(badgeNum))
                  {
                    // we've got more than one entry with the same
                    // badge number.. that should only
                    // happen if one of the users is an GASH admin, or
                    // if one is inactivated.

                    DBObject oldUser = map.get(badgeNum);

                    DBField field = (DBField) oldUser.getField(userSchema.PERSONAE);

                    if (field != null && field.isDefined())
                      {
                        continue; // we've already got an admin record for this badge number
                      }
                  }

                result.setLength(0);

                result.append(badgeNum);

                int length = 6 - badgeNum.length();

                for (int i = 0; i < length; i++)
                  {
                    result.append(" ");
                  }

                result.append(username);

                length = 8 - username.length();

                for (int i = 0; i < length; i++)
                  {
                    result.append(" ");
                  }

                String emailAddr = signature + "@" + dnsdomain.substring(1);

                result.append(emailAddr);

                length = 50 - emailAddr.length();

                for (int i = 0; i < length; i++)
                  {
                    result.append(" ");
                  }

                map.put(badgeNum, user);
                results.put(badgeNum, result.toString());
              }
          }

        for (String line: results.values())
          {
            out.println(line);
          }
      }
    finally
      {
        out.close();
      }
  }


  /**
   * This method writes out a simple list of all ARL employees who are
   * to receive email when lab-wide email is sent.
   *
   * The file is simple, and contains one user name per line.
   *
   * We take the trouble in this method to eliminate redundant
   * mailings that would come to the same person if
   */

  private boolean writeEmailLists()
  {
    PrintWriter out, out2;
    Set targets = new HashSet();  // record delivery targets we've seen
    Set targets2 = new HashSet(); // the same, for employees only

    /* -- */

    try
      {
        out = openOutFile(path + "all_users.txt", "gasharl");
      }
    catch (IOException ex)
      {
        System.err.println("GASHBuilderTask.builderPhase1(): couldn't open all_users.txt file: " + ex);
        return false;
      }

    try
      {
        out2 = openOutFile(path + "all_employees.txt", "gasharl");
      }
    catch (IOException ex)
      {
        System.err.println("GASHBuilderTask.builderPhase1(): couldn't open all_employees.txt file: " + ex);

        out.close();
        return false;
      }

    try
      {
        out.println("# all_users.txt");
        out.println("#");
        out.println("# Complete list of user names who should receive email when mail is sent");
        out.println("# to 'all users' in laboratory, filtered to avoid redundant delivery.");
        out.println("#");
        out.println("# " + (new Date()).toString());
        out.println();

        out2.println("# all_employees.txt");
        out2.println("#");
        out2.println("# Complete list of employees in the laboratory who should receive email when mail is sent");
        out2.println("# to 'all employees' in laboratory, filtered to avoid redundant delivery.");
        out2.println("#");
        out2.println("# " + (new Date()).toString());
        out2.println();

        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
            if (user.isInactivated())
              {
                continue;
              }

            Invid category = (Invid) user.getFieldValueLocal(userSchema.CATEGORY);

            if (category == null ||
                (!category.equals(getNormalCategory()) && !category.equals(getAgencyCategory())))
              {
                continue;
              }

            String username = (String) user.getFieldValueLocal(SchemaConstants.UserUserName);
            Vector deliveryAddresses = user.getFieldValuesLocal(userSchema.EMAILTARGET);

            // first all users

            if (!targets.contains(username) && (category.equals(getNormalCategory()) || category.equals(getAgencyCategory())))
              {
                boolean allSeen = true;

                for (int i = 0; i < deliveryAddresses.size(); i++)
                  {
                    if (!targets.contains(deliveryAddresses.elementAt(i)))
                      {
                        allSeen = false;
                        targets.add(deliveryAddresses.elementAt(i));
                      }
                  }

                if (!allSeen)
                  {
                    targets.add(username);

                    out.println(username);
                  }
              }

            // then employees only

            if (!targets2.contains(username) && category.equals(getNormalCategory()))
              {
                boolean allSeen = true;

                for (int i = 0; i < deliveryAddresses.size(); i++)
                  {
                    if (!targets2.contains(deliveryAddresses.elementAt(i)))
                      {
                        allSeen = false;
                        targets2.add(deliveryAddresses.elementAt(i));
                      }
                  }

                if (!allSeen)
                  {
                    targets2.add(username);

                    out2.println(username);
                  }
              }
          }
      }
    finally
      {
        out.close();
        out2.close();
      }

    return true;
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

        for (DBObject netgroup: getObjects(userNetgroupSchema.BASE))
          {
            writeUserNetgroup(netgroup, netgroupFile);
          }

        // now the system netgroups

        for (DBObject netgroup: getObjects(systemNetgroupSchema.BASE))
          {
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
   * omg-u      csd-u (-,broccol,) (-,gomod,) (-,etcrh,)
   *
   */

  private void writeUserNetgroup(DBObject object, PrintWriter writer)
  {
    StringBuilder buffer = new StringBuilder();

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
                buffer = new StringBuilder();
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
                buffer = new StringBuilder();
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
   * omg-s      csd-s (csdsun1.arlut.utexas.edu,-,) (ns1.arlut.utexas.edu,-,)
   *
   */

  private void writeSystemNetgroup(DBObject object, PrintWriter writer)
  {
    StringBuilder buffer = new StringBuilder();

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
                buffer = new StringBuilder();
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
                buffer = new StringBuilder();
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
    String name;
    Hashtable members = new Hashtable();

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

        for (DBObject netgroup: getObjects(userNetgroupSchema.BASE))
          {
            name = (String) netgroup.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);

            members.clear();
            unionizeMembers(netgroup, members);

            if (members.size() > 0)
              {
                writer.print(name);

                Enumeration users = members.elements();

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
    SharedStringBuffer buf = new SharedStringBuffer();

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

        for (DBObject obj: getObjects(volumeSchema.BASE))
          {
            buf.setLength(0);

            String volName = (String) obj.getFieldValueLocal(volumeSchema.LABEL);

            if (volName == null)
              {
                Ganymede.debug("Couldn't emit a volume definition.. null label");
                continue;
              }

            buf.append(volName); // volume label
            buf.append("\t\t");

            // mount options.. NeXTs like this.  Ugh.

            String mountopts = (String) obj.getFieldValueLocal(volumeSchema.MOUNTOPTIONS);

            if (mountopts != null && !mountopts.equals(""))
              {
                buf.append(mountopts);
                buf.append(" ");
              }

            String sysName = getLabel((Invid) obj.getFieldValueLocal(volumeSchema.HOST));

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

    for (DBObject map: getObjects(mapSchema.BASE))
      {
        String mapname = (String) map.getFieldValueLocal(mapSchema.MAPNAME);

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
            List<Invid> tempVect = (List<Invid>) map.getFieldValuesLocal(mapSchema.ENTRIES);

            if (tempVect == null)
              {
                autoFile.close();
                continue;
              }

            for (Invid ref: tempVect)
              {
                DBObject obj = getObject(ref);

                // the entry is embedded in the user's record.. get
                // the user' id and label

                Invid userRef = (Invid) obj.getFieldValueLocal(mapEntrySchema.CONTAININGUSER);

                if (userRef.getType() != SchemaConstants.UserBase)
                  {
                    throw new RuntimeException("Schema and/or database error");
                  }

                buf.setLength(0);

                buf.append(getLabel(userRef)); // the user's name
                buf.append("\t");

                // nfs volume for this entry

                ref = (Invid) obj.getFieldValueLocal(mapEntrySchema.VOLUME);

                if (ref == null || ref.getType() != volumeSchema.BASE)
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
   * builderPhase1 so that it has access to the getObjects() method
   * from our superclass. To be passed to Mailman server.
   *
   */

  private boolean writeMailmanListsFile()
  {
    PrintWriter mailman_sync_file = null;

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

        for (DBObject mailmanList: getObjects(MailmanListSchema.BASE))
          {
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
   * This Method writes out a mailman list target line to the mailman lists file.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
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

  /**
   *
   * This method generates an aliases_info file.  This method must be run during
   * builderPhase1 so that it has access to the getObjects() method
   * from our superclass.
   *
   */

  private boolean writeAliasesFile()
  {
    PrintWriter aliases_info = null;

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

        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
            writeUserAlias(user, aliases_info);
          }

        // now the mail lists

        for (DBObject group: getObjects(emailListSchema.BASE))
          {
            writeGroupAlias(group, aliases_info);
          }

        // add in emailable account groups

        for (DBObject group: getObjects(groupSchema.BASE))
          {
            writeAccountGroupAlias(group, aliases_info);
          }

        // add in emailable user netgroups

        for (DBObject group: getObjects(userNetgroupSchema.BASE))
          {
            writeUserNetgroupAlias(group, aliases_info);
          }

        // and the external mail addresses

        for (DBObject external: getObjects(emailRedirectSchema.BASE))
          {
            writeExternalAlias(external, aliases_info);
          }

	// as well as the aliases for the mailman lists

        for (DBObject mailman: getObjects(MailmanListSchema.BASE))
          {
            writeMailmanListAlias(mailman, aliases_info);
          }
      }
    finally
      {
        aliases_info.close();
      }

    return true;
  }

  /**
   * This method writes out a mailman alias line to the aliases_info GASH source file.<br/><br/>
   *
   * The mailman alias lines in this file look like the following:<br/><br/>
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
    List<String> aliases = (List<String>) object.getFieldValuesLocal(MailmanListSchema.ALIASES);

    if (aliases == null)
      {
        System.err.println("GASHBuilder.writeMailmanAliases(): null alias list for mailman list name " + name);
      }
    else
      {
        for (String aliasName: aliases)
          {
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
   * This method writes out a user alias line to the aliases_info GASH source file.<br/><br/>
   *
   * The user alias lines in this file look like the following:<br/><br/>
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
    List<String> aliases;
    List<String> addresses;

    /* -- */

    result.setLength(0);

    username = (String) object.getFieldValueLocal(userSchema.USERNAME);
    signature = (String) object.getFieldValueLocal(userSchema.SIGNATURE);
    aliases = (List<String>) object.getFieldValuesLocal(userSchema.ALIASES);
    addresses = (List<String>) object.getFieldValuesLocal(userSchema.EMAILTARGET);

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

        for (String alias: aliases)
          {
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

            result.append(addresses.get(i));
          }
      }

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a mail list alias line to the aliases_info GASH source file.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
   *
   * <pre>
   *
   * :csd-news-dist-omg:alias, alias, alias:csd_staff, granger, iselt, lemma, jonabbey@eden.com
   *
   * </pre>
   *
   * Where the leading colon identifies to the GASH makefile that it is a group
   * line.
   *
   * NB: I've modified this method in 2008 to add the aliases field,
   * above.  This is a modification of the classic GASH aliases_info
   * file, which did not support aliases for email lists.
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   */

  private void writeGroupAlias(DBObject object, PrintWriter writer)
  {
    String groupname;
    Vector group_targets;
    Vector group_aliases;
    Vector external_targets;
    Invid memberInvid;
    String target;

    int lengthlimit_remaining;
    int subgroup = 1;
    String subname;

    /* -- */

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(emailListSchema.LISTNAME);
    group_aliases = object.getFieldValuesLocal(emailListSchema.ALIASES);
    group_targets = object.getFieldValuesLocal(emailListSchema.MEMBERS);
    external_targets = object.getFieldValuesLocal(emailListSchema.EXTERNALTARGETS);

    result.append(":");
    result.append(groupname);
    result.append(":");

    if (group_aliases != null)
      {
        for (int i = 0; i < group_aliases.size(); i++)
          {
            String alias = (String) group_aliases.elementAt(i);

            if (i > 0)
              {
                result.append(", ");
              }

            result.append(alias);
          }
      }

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
            memberInvid = (Invid) group_targets.elementAt(i);

            if (isVeryDeadUser(memberInvid))
              {
                continue;
              }

            if (i > 0)
              {
                result.append(", ");
              }

            target = getLabel(memberInvid);

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
   * This method writes out a mail list alias line to the aliases_info
   * GASH source file, as sourced from a gasharl account
   * group.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
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

  private void writeAccountGroupAlias(DBObject object, PrintWriter writer)
  {
    String groupname;
    Vector group_targets;
    Invid userInvid;
    String target;

    int lengthlimit_remaining;
    int subgroup = 1;
    String subname;

    /* -- */

    if (!object.isSet(groupSchema.EMAILOK))
      {
        return;
      }

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(groupSchema.GROUPNAME);
    group_targets = object.getFieldValuesLocal(groupSchema.USERS);

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
            userInvid = (Invid) group_targets.elementAt(i);

            if (isVeryDeadUser(userInvid))
              {
                continue;
              }

            if (i > 0)
              {
                result.append(", ");
              }

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

    writer.println(result.toString());
  }


  /**
   *
   * This method writes out a mail list alias line to the aliases_info
   * GASH source file, as sourced from a gasharl user netgroup object.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
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

  private void writeUserNetgroupAlias(DBObject object, PrintWriter writer)
  {
    String groupname;
    Vector group_targets;
    Vector sub_netgroups;
    Vector targets;

    String target;

    int lengthlimit_remaining;
    int subgroup = 1;
    String subname;

    /* -- */

    if (!object.isSet(userNetgroupSchema.EMAILOK))
      {
        return;
      }

    result.setLength(0);

    groupname = (String) object.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);
    group_targets = object.getFieldValuesLocal(userNetgroupSchema.USERS);
    sub_netgroups = object.getFieldValuesLocal(userNetgroupSchema.MEMBERGROUPS);

    targets = new Vector();

    if (group_targets != null)
      {
        for (int i = 0; i < group_targets.size(); i++)
          {
            Invid targetInvid = (Invid) group_targets.elementAt(i);

            if (isVeryDeadUser(targetInvid))
              {
                continue;
              }

            targets.addElement(getLabel(targetInvid));
          }
      }

    if (sub_netgroups != null)
      {
        for (int i = 0; i < sub_netgroups.size(); i++)
          {
            DBObject subnetgroup = getObject((Invid) sub_netgroups.elementAt(i));

            if (subnetgroup.isSet(userNetgroupSchema.EMAILOK))
              {
                targets.addElement(subnetgroup.getLabel());
              }
          }
      }

    result.append(":xxx:");
    result.append(groupname);
    result.append(":");

    // NIS forces us to a 1024 character limit per key and value, we
    // need to truncate and extend to match, here.  We'll cut it down
    // to 900 to give ourselves some slack so we can write out our
    // chain link at the end of the line

    lengthlimit_remaining = 900 - result.length();

    for (int i = 0; i < targets.size(); i++)
      {
        if (i > 0)
          {
            result.append(", ");
          }

        target = (String) targets.elementAt(i);

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

    writer.println(result.toString());
  }

  /**
   *
   * This method writes out a mail list alias line to the aliases_info
   * GASH source file, as sourced from an emailable user
   * netgroup.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
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
    result.append(name);        // the name is one of the aliases

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
   * This method generates a postfix-compatible aliases (name
   * undetermined so far) file.  This method must be run during
   * builderPhase1 so that it has access to the getObjects()
   * method from our superclass.
   *
   *    AHEM!!!  where you see "write Hash*" below...  what that
   *    really means is:
   *    write the flat file that postfix (through postalias or
   *    postmap) will turn into a hash file.
   *    the file has to get flung over via ssh and then
   *    something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private boolean writeHashAliasesFile() throws IOException
  {
    boolean success = false;

    PrintWriter pfgenerics = openOutFile(path + "pfgenerics", "gasharl");

    try
      {
        PrintWriter pfmalias = openOutFile(path + "pfmalias", "gasharl");

        try
          {
            PrintWriter pftransport = openOutFile(path + "pftransport", "gasharl");

            try
              {
                PrintWriter pfknownu = openOutFile(path + "pfknown_users", "gasharl");

                try
                  {
                    writeHashTransport(pftransport);
                    writeHashKnownuser(pfknownu);

                    for (DBObject user: getObjects(SchemaConstants.UserBase))
                      {
                        writeHashGenerics(user, pfgenerics);
                        writeHashUserAlias(user, pfmalias);
                      }
    
                    // mail lists
    
                    for (DBObject group: getObjects(emailListSchema.BASE))
                      {
                        writeHashGroupAlias(group, pfmalias);
                      }
    
                    // emailable account groups
    
                    for (DBObject group: getObjects(groupSchema.BASE))
                      {
                        writeHashAccountGroupAlias(group, pfmalias);
                      }
    
                    // emailable user netgroups
    
                    for (DBObject group: getObjects(userNetgroupSchema.BASE))
                      {
                        writeHashUserNetgroupAlias(group, pfmalias);
                      }
    
                    // external mail addresses
    
                    for (DBObject external: getObjects(emailRedirectSchema.BASE))
                      {
                        writeHashExternalAlias(external, pfmalias);
                      }

                    success = true;
                  }
                finally
                  {
                    pfknownu.close();
                  }
              }
            finally
              {
                pftransport.close();
              }
          }
        finally
          {
            pfmalias.close();
          }
      }
    finally
      {
        pfgenerics.close();
      }

    return success;
  }

  /**
   * This method writes out a user alias line to the pfmalias file.<br/><br/>
   *
   * The user alias lines in this file look like the following:<br/><br/>
   *
   * <pre>
   *
   * aliasthing: real1, real2, real3
   *
   * </pre>
   *
   * Where aliasthing is the name of an alias, and
   * real<n> are actual email addresses to deliver to (but, as you know,
   * those things can be aliases themselves).
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   *
   *    AHEM!!!  where you see "write HashUserAlias" below...
   *    what that really means is:
   *    write the flat file that postfix (through postalias or
   *    postmap) will turn into a hash file.
   *    the file has to get flung over via ssh and then
   *    something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashUserAlias(DBObject object, PrintWriter writer)
  {
    String username = (String) object.getFieldValueLocal(userSchema.USERNAME);
    String signature = (String) object.getFieldValueLocal(userSchema.SIGNATURE);

    result.setLength(0);

    Vector<String> addresses = (Vector<String>) object.getFieldValuesLocal(userSchema.EMAILTARGET);

    if (!empty(addresses))
      {
        result.setLength(0);
        result.append(signature);
        result.append(": ");

        for (int i = 0; i < addresses.size(); i++)
          {
            if (i > 0)
              {
                result.append(", ");
              }

            result.append(fixup(addresses.get(i)));
          }

        writer.println(result.toString().toLowerCase());
      }

    Vector<String> aliases = (Vector<String>) object.getFieldValuesLocal(userSchema.ALIASES);

    if (!empty(aliases))
      {
        for (String alias: aliases)
          {
            if (alias.equals(signature))
              {
                continue;
              }

            writer.print(alias.toLowerCase());
            writer.print(": ");
            writer.println(signature.toLowerCase());
          }
      }

    if (!empty(aliases) && !empty(addresses))
      {
        for (String alias: aliases)
          {
            if (!alias.equals(signature))
              {
                continue;
              }

            result.setLength(0);
            result.append(username);
            result.append(": ");

            for (int i = 0; i < addresses.size(); i++)
              {
                if (i > 0)
                  {
                    result.append(", ");
                  }

                result.append(fixup(addresses.get(i)));
              }

            writer.println(result.toString().toLowerCase());
          }
      }
  }

  /**
   * This method writes out a user alias line to the pfmalias file.<br/><br/>
   *
   * AHEM!!!  where you see "write Hash Generics" below...
   * what that really means is:
   * write the flat file that postfix (through postalias or
   * postmap) will turn into a hash file.
   * the file has to get flung over via ssh and then
   * something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashGenerics(DBObject object, PrintWriter writer)
  {
    String username = (String) object.getFieldValueLocal(userSchema.USERNAME);
    String signature = (String) object.getFieldValueLocal(userSchema.SIGNATURE);
    Vector<String> aliases = (Vector<String>) object.getFieldValuesLocal(userSchema.ALIASES);
    Vector<String> addresses = (Vector<String>) object.getFieldValuesLocal(userSchema.EMAILTARGET);

    result.setLength(0);

    // we should never have @ chars in signature aliases, but if we
    // do, trim

    if (signature.indexOf('@') != -1)
      {
        try
          {
            throw new RuntimeException("Warning, @ in signature alias!");
          }
        catch (RuntimeException ex)
          {
            Ganymede.logError(ex);
          }

        signature = signature.substring(0, signature.indexOf('@'));
      }

    result.append(signature);
    result.append(": ");
    result.append(signature);
    result.append("@arlut.utexas.edu.");
    writer.println(result.toString().toLowerCase());

    if (!empty(aliases))
      {
        for (String alias: aliases)
          {
            if (alias.equals(signature))
              {
                result.setLength(0);
                result.append(username);
                result.append(": ");
                result.append(signature);
                result.append("@arlut.utexas.edu.");

                writer.println(result.toString().toLowerCase());
              }
            else
              {
                result.setLength(0);
                result.append(alias);
                result.append(": ");
                result.append(signature);
                result.append("@arlut.utexas.edu.");

                writer.println(result.toString().toLowerCase());
              }
          }
      }
  }

  /**
   * This method writes out a mail list alias line to the pfmalias file.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
   *
   * <pre>
   *
   * aliasthing: real1, real2, real3
   *
   * </pre>
   *
   * Where aliasthing is the name of an alias, and
   * real<n> are actual email addresses to deliver to (but, as you know,
   * those things can be aliases themselves).
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   *
   * AHEM!!!  where you see "write HashGroupAlias" below...
   * what that really means is:
   * write the flat file that postfix (through postalias or
   * postmap) will turn into a hash file.
   * the file has to get flung over via ssh and then
   * something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashGroupAlias(DBObject object, PrintWriter writer)
  {
    String groupname = (String) object.getFieldValueLocal(emailListSchema.LISTNAME);
    Vector<String> group_aliases = (Vector<String>) object.getFieldValuesLocal(emailListSchema.ALIASES);
    Vector<Invid> group_targets = (Vector<Invid>) object.getFieldValuesLocal(emailListSchema.MEMBERS);
    Vector<String> external_targets = (Vector<String>) object.getFieldValuesLocal(emailListSchema.EXTERNALTARGETS);
    Integer didSomething = 0;

    //  if the idea is to write each group out as the full list, then,
    //  okay, i guess we can do that.  actually, that is a chore,
    //  isn't it?  so let's spit out each alias and the groupname,
    //  then just do the groupname once.

    if (!empty(group_aliases))
      {
        for (String alias: group_aliases)
          {
            result.setLength(0);
            result.append(alias);
            result.append(": ");
            result.append(groupname);
            writer.println(result.toString().toLowerCase());
          }
      }

    // whoops.  need to know that we have something to spit out.

    if (!empty(group_targets) || !empty(external_targets))
      {
        result.setLength(0);
        result.append(groupname);
        result.append(": ");

        if (!empty(group_targets))
          {
            for (int i = 0; i < group_targets.size(); i++)
              {
                Invid memberInvid = (Invid) group_targets.get(i);

                if (isVeryDeadUser(memberInvid))
                  {
                    continue;
                  }

                if (i > 0)
                  {
                    result.append(", ");
                  }

                result.append(getLabel(memberInvid));
	        didSomething++;
              }
          }

        if (!empty(external_targets))
          {
            for (int i = 0; i < external_targets.size(); i++)
              {
                if (i > 0 || !empty(group_targets))
                  {
                    result.append(", ");
                  }

                result.append(external_targets.get(i));
	        didSomething++;
              }
          }

        if( didSomething > 0 )
	  {
            writer.println(result.toString().toLowerCase());
	  }
      }
  }

  /**
   * This method writes out a mail list alias line to the pfmalias
   * file, as sourced from a gasharl account
   * group.<br/><br/>
   *
   * <pre>
   *
   * aliasthing: real1, real2, real3
   *
   * </pre>
   *
   * Where aliasthing is the name of an alias, and
   * real<n> are actual email addresses to deliver to (but, as you know,
   * those things can be aliases themselves).
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   *
   * AHEM!!!  where you see "write HashAccountGroupAlias" below...
   * what that really means is:
   * write the flat file that postfix (through postalias or
   * postmap) will turn into a hash file.
   * the file has to get flung over via ssh and then
   * something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashAccountGroupAlias(DBObject object, PrintWriter writer)
  {
    if (!object.isSet(groupSchema.EMAILOK))
      {
        return;
      }

    String groupname = (String) object.getFieldValueLocal(groupSchema.GROUPNAME);
    Vector<Invid> group_targets = (Vector<Invid>) object.getFieldValuesLocal(groupSchema.USERS);

    if (!empty(group_targets))
      {
        result.setLength(0);
        result.append(groupname);
        result.append(": ");

        for (int i = 0; i < group_targets.size(); i++)
          {
            Invid userInvid = group_targets.get(i);

            if (isVeryDeadUser(userInvid))
              {
                continue;
              }

            if (i > 0)
              {
                result.append(", ");
              }

            result.append(getLabel(userInvid));
          }

        writer.println(result.toString().toLowerCase());
      }
  }

  /**
   *
   * This method writes out a mail list alias line to the pfmalias
   * file, as sourced from a gasharl user netgroup object.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
   * <pre>
   *
   * aliasthing: real1, real2, real3
   *
   * </pre>
   *
   * Where aliasthing is the name of an alias, and
   * real<n> are actual email addresses to deliver to (but, as you know,
   * those things can be aliases themselves).
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   *
   *       AHEM!!!  where you see "write HashUserNetgroupAlias" below...
   *       what that really means is:
   *       write the flat file that postfix (through postalias or
   *       postmap) will turn into a hash file.
   *       the file has to get flung over via ssh and then
   *       something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashUserNetgroupAlias(DBObject object, PrintWriter writer)
  {
    if (!object.isSet(userNetgroupSchema.EMAILOK))
      {
        return;
      }

    String groupname = (String) object.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);
    Vector<Invid> group_targets = (Vector<Invid>) object.getFieldValuesLocal(userNetgroupSchema.USERS);
    Vector<Invid> sub_netgroups = (Vector<Invid>) object.getFieldValuesLocal(userNetgroupSchema.MEMBERGROUPS);

    Vector<String> targets = new Vector<String>();

    if (!empty(group_targets))
      {
        for (Invid targetInvid: group_targets)
          {
            if (isVeryDeadUser(targetInvid))
              {
                continue;
              }

            targets.add(getLabel(targetInvid));
          }
      }

    if (!empty(sub_netgroups))
      {
        for (Invid subNetGroup: sub_netgroups)
          {
            DBObject subnetgroup = getObject(subNetGroup);

            if (subnetgroup.isSet(userNetgroupSchema.EMAILOK))
              {
                targets.add(subnetgroup.getLabel());
              }
          }
      }

    if (!empty(targets))
      {
	result.setLength(0);
	result.append(groupname);
	result.append(": ");

	for (int i = 0; i < targets.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }

	    result.append(targets.get(i));
	  }

	writer.println(result.toString().toLowerCase());
      }
  }

  /**
   * This method writes out a mail list alias line to the pfmalias
   * file, as sourced from an emailable user
   * netgroup.<br/><br/>
   *
   * The mail list lines in this file look like the following:<br/><br/>
   * <pre>
   *
   * aliasthing: real1, real2, real3
   *
   * </pre>
   *
   * Where aliasthing is the name of an alias, and
   * real<n> are actual email addresses to deliver to (but, as you know,
   * those things can be aliases themselves).
   *
   * @param object An object from the Ganymede user object base
   * @param writer The destination for this alias line
   *
   *       AHEM!!!  where you see "write HashExternalAlias" below...
   *       what that really means is:
   *       write the flat file that postfix (through postalias or
   *       postmap) will turn into a hash file.
   *       the file has to get flung over via ssh and then
   *       something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashExternalAlias(DBObject object, PrintWriter writer)
  {
    String name = (String) object.getFieldValueLocal(emailRedirectSchema.NAME);
    Vector<String> targets = (Vector<String>) object.getFieldValuesLocal(emailRedirectSchema.TARGETS);
    Vector<String> aliases = (Vector<String>) object.getFieldValuesLocal(emailRedirectSchema.ALIASES);

    if (!empty(aliases))
      {
        for (String alias: aliases)
          {
            if (!alias.equals(name))
              {
                result.setLength(0);
                result.append(alias);
                result.append(": ");
                result.append(name);
                writer.println(result.toString().toLowerCase());
              }
          }
      }

    //  if targets is null, we mustn't put out a stub line.

    if (!empty(targets))
      {
        result.setLength(0);
        result.append(name);
        result.append(": ");

        for (int i = 0; i < targets.size(); i++)
          {
            if (i > 0)
              {
                result.append(", ");
              }

            result.append(fixup(targets.get(i)));
          }

        writer.println(result.toString().toLowerCase());
      }
  }

  /**
   * This method writes out a transport file for use by postfix.<br/><br/>
   *
   * The lines look like this:<br/><br/>
   *
   * <pre>
   *
   * thingy.arlut.utexas.edu    smtp:[thingy.arlut.utexas.edu]
   *
   * </pre>
   *
   * Where thingy is a mail server that does local delivery.
   *
   *
   *    AHEM!!!  where you see "write HashSomething" below...
   *    what that really means is:
   *    write the flat file that postfix (through postalias or
   *    postmap) will turn into a hash file.
   *    the file has to get flung over via ssh and then
   *    something has to run postmap/postalias on the file.
   *
   * jgs
   */

  private void writeHashTransport(PrintWriter writer)
  {
    Set<String> set = new CaseInsensitiveSet();

    //  some things in here will NOT be found by the loop following
    //  this one.  so you do have to do this.

    for (DBObject loluser: getObjects(SchemaConstants.UserBase))
      {
        Vector<String> addresses = (Vector<String>) loluser.getFieldValuesLocal(userSchema.EMAILTARGET);

        if (!empty(addresses))
          {
            for (String address: addresses)
              {
                String host = getEmailHost(fixup(address));
                
                if (host.endsWith("arlut.utexas.edu"))
                  {
                    set.add(host);
                  }
              }
          }
      }

    for (DBObject external: getObjects(emailRedirectSchema.BASE))
      {
        Vector<String> targets = (Vector<String>) external.getFieldValuesLocal(emailRedirectSchema.TARGETS);

        if (!empty(targets))
          {
            for (String target: targets)
              {
                String host = getEmailHost(fixup(target));

                if (host.endsWith("arlut.utexas.edu"))
                  {
                    set.add(host);
                  }
              }
          }
      }

    // XXX
    //
    // No emailListSchema checking here?
    //
    // XXX

    for (String host: set)
      {
        writer.print(host);
        writer.print("\tsmtp:[");
        writer.print(host);
        writer.println("]");
      }
  }

  /**
   * This method writes out a list of the users allowed to use mail;
   * the list is for use by postfix.<br/><br/>
   *
   * The lines look like this:<br/><br/>
   *
   * <pre>
   *
   * user       OK
   *
   * </pre>
   *
   * jgs
   */

  private void writeHashKnownuser(PrintWriter writer)
  {
    Set<String> set = new CaseInsensitiveSet();

    for (DBObject user: getObjects(SchemaConstants.UserBase))
      {
        String username = (String) user.getFieldValueLocal(userSchema.USERNAME);

        set.add(username);

        Vector<String> aliases = (Vector<String>) user.getFieldValuesLocal(userSchema.ALIASES);

        if (aliases != null)
          {
            for (String alias: aliases)
              {
                set.add(alias);
              }
          }

        Vector<String> targets = (Vector<String>) user.getFieldValuesLocal(userSchema.EMAILTARGET);

        if (targets != null)
          {
            for (String target: targets)
              {
                String account = getEmailAccount(target);
                String host = getEmailHost(target);

                if (host.endsWith("arlut.utexas.edu"))
                  {
                    // account could be the user's name or any of his
                    // aliases, above, but set.add() will check that
                    // for us efficiently
                    //
                    // we could check for whether account is equals to
                    // "no_longer_employed" here..

                    set.add(account);
                  }
              }
          }
      }

    for (DBObject group: getObjects(emailListSchema.BASE))
      {
        String groupname = (String) group.getFieldValueLocal(emailListSchema.LISTNAME);
        Vector<String> group_aliases = (Vector<String>) group.getFieldValuesLocal(emailListSchema.ALIASES);
        Vector<Invid> group_targets = (Vector<Invid>) group.getFieldValuesLocal(emailListSchema.MEMBERS);
        Vector external_targets = group.getFieldValuesLocal(emailListSchema.EXTERNALTARGETS);

        if (!empty(group_aliases) || !empty(group_targets) || !empty(external_targets))
          {
	    set.add(groupname);
          }

        if (!empty(group_aliases))
          {
            for (String alias: group_aliases)
              {
                set.add(alias);
              }
          }


        if (!empty(group_targets))
          {
            for (Invid memberInvid: group_targets)
              {
                if (!isVeryDeadUser(memberInvid))
                  {
                    set.add(getLabel(memberInvid));
                  }
              }
          }
      }

    for (DBObject group: getObjects(groupSchema.BASE))
      {
        if (group.isSet(groupSchema.EMAILOK))
          {
            String groupname = (String) group.getFieldValueLocal(groupSchema.GROUPNAME);

            set.add(groupname);
          }
      }

    for (DBObject group: getObjects(userNetgroupSchema.BASE))
      {
        if (group.isSet(userNetgroupSchema.EMAILOK))
          {
            String groupname = (String) group.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);

            set.add(groupname);
          }
      }

    for (DBObject external: getObjects(emailRedirectSchema.BASE))
      {
        String name = (String) external.getFieldValueLocal(emailRedirectSchema.NAME);

        set.add(name);

        Vector<String> aliases = (Vector<String>) external.getFieldValuesLocal(emailRedirectSchema.ALIASES);

        if (!empty(aliases))
          {
            for (String alias: aliases)
              {
                if (!alias.equals(name))
                  {
                    set.add(alias);
                  }
              }
          }

        Vector<String> targets = (Vector<String>) external.getFieldValuesLocal(emailRedirectSchema.TARGETS);

        if (!empty(targets))
          {
            for (String target: targets)
              {
                String host = getEmailHost(target);
                String user = getEmailAccount(target);

                if (host.endsWith("arlut.utexas.edu"))
                  {
                    set.add(user);
                  }
              }
          }
      }

    for (String account: set)
      {
	// needs to end in @arlut.utexas.edu on postfix side.
	// jgs 17 nov 2010

        writer.print(account);
        writer.println("@arlut.utexas.edu OK");
      }

    return;
  }

  /**
   * Cleans up / fixes up address for our use in generating Postfix
   * email input files.
   *
   * jon/jgs
   */

  private String fixup(Object in)
  {
    // if the target has @arlut.utexas.edu
    // change it to @arlmail.arlut.utexas.edu.  sigh.

    return in.toString().replace("@arlut.utexas.edu",
                                 "@arlmail.arlut.utexas.edu");
  }

  /**
   * Returns a lower case String containing everything after the @
   * sign in address, or the empty string if no @ was found in the
   * address parameter.
   */

  private String getEmailHost(String address)
  {
    if (address.indexOf('@') != -1)
      {
        return address.substring(address.indexOf('@') + 1).toLowerCase();
      }

    return "";
  }

  /**
   * Returns the (lower cased) account/alias name in front of the @
   * sign in address, or throws an InvalidArgumentException if the
   * address is null.
   */

  private String getEmailAccount(String address)
  {
    if (address == null || "".equals(address))
      {
        throw new IllegalArgumentException();
      }

    if (address.indexOf('@') == -1)
      {
	return address.toLowerCase();
      }

    return address.substring(0, address.indexOf('@')).toLowerCase();
  }

  /**
   * Convenience method, returns true if in is null or empty.
   */

  private boolean empty(Vector in)
  {
    return in == null || in.size() == 0;
  }

//------------------------------------------------------------------------------
//------------------------------------------------------------------------------

  /**
   * This method checks to see if an invid is a user, and if that user
   * was inactivated (or at least last edited by) anyone other than
   * the password aging task.
   *
   * If we find such a user, we will not include him in email lists of
   * any kind, lest we generate bounce messages to people sending to
   * those lists.  If anyone other than the password aging task
   * inactivated a user, we're going to assume that the user should
   * not receive any more mail that was sent to a Ganymede mail list
   * (of any variety), rather than directly to him.
   */

  private boolean isVeryDeadUser(Invid invid)
  {
    if (invid.getType() != 3)
      {
        return false;
      }

    DBObject userObject = getObject(invid);

    if (!userObject.isInactivated())
      {
        return false;
      }

    Vector emailTargets = userObject.getFieldValuesLocal(userSchema.EMAILTARGET);

    if (emailTargets == null)
      {
        // huh, no targets?  that's pretty dead!

        return true;
      }

    if (emailTargets.size() > 1)
      {
        // multiple addresses?  someone's doing something fancy on
        // purpose, let it pass

        return false;
      }

    String target = (String) emailTargets.elementAt(0);

    if (target.indexOf('@') == -1)
      {
        // we're pointing to another user, presumably.  let it pass

        return false;
      }

    if (target.toLowerCase().endsWith("redirect"))
      {
        return true;            // no sending to bounce addresses, thanks
      }

    String modifierName = (String) userObject.getFieldValueLocal(SchemaConstants.ModifierField);

    if (modifierName.equals("[" + PasswordAgingTask.name + "]"))
      {
        return false;
      }

    if (target.startsWith((userObject.getLabel() + "@")) && target.endsWith("arlut.utexas.edu"))
      {
        return true;            // we're mailing to the user himself,
                                // and they weren't password
                                // expired, skip mailing to this user
      }

    return false;
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
        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
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
        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
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

    StringBuilder timeString = new StringBuilder();

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
        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
            String username = user.getLabel();
            Invid invid = user.getInvid();
            String signature = (String) user.getFieldValueLocal(userSchema.SIGNATURE);
            String fullname = (String) user.getFieldValueLocal(userSchema.FULLNAME);
            String cryptText = null;

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
   * <p>This method writes out password and group files compatible
   * with the Apache web server.  The password file is formatted
   * according to the standard .htpasswd file format, as follows:</p>
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
        for (DBObject user: getObjects(SchemaConstants.UserBase))
          {
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

        for (DBObject group: getObjects(groupSchema.BASE))
          {
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

        for (DBObject group: getObjects(userNetgroupSchema.BASE))
          {
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
   * <p>This method writes out credentials to our external SMTP server.
   * The credentials file is formatted as follows:</p>
   *
   * <PRE>
   * mailusername mailpassword
   * mailusername mailpassword
   * </PRE>
   *
   * <p>It also writes out credentials for our internal server.
   * The credentials file is formatted as follows:</p>
   *
   * <PRE>
   * username mailusername mailpassword
   * username mailusername mailpassword
   * </PRE>
   */

  private boolean writeExternalMailFiles()
  {
    PrintWriter mailCredentials = null;
    PrintWriter extIMAPCredentials = null;

    try
      {
        mailCredentials = openOutFile(path + "extMailCredentials", "gasharl");
      }
    catch (IOException ex)
      {
        System.err.println("GASHBuilderTask.writeExternalMailFiles(): couldn't open extMailCredentials file: " + ex);
        return false;
      }

    try
      {
        try
          {
            extIMAPCredentials = openOutFile(path + "extIMAPCredentials", "gasharl");
          }
        catch (IOException ex)
          {
            System.err.println("GASHBuilderTask.writeExternalMailFiles(): couldn't open extIMAPCredentials file: " + ex);
            return false;
          }

        try
          {
            for (DBObject user: getObjects(SchemaConstants.UserBase))
              {
                if (user.isInactivated() ||
                    !user.isSet(userSchema.ALLOWEXTERNAL) ||
                    !user.isDefined(userSchema.MAILUSER) ||
                    !user.isDefined(userSchema.MAILPASSWORD2))
                  {
                    continue;
                  }

                StringDBField usernameField = (StringDBField) user.getField(userSchema.USERNAME);
                String username = (String) usernameField.getValueLocal();

                StringDBField mailUsernameField = (StringDBField) user.getField(userSchema.MAILUSER);
                String mailUsername = (String) mailUsernameField.getValueLocal();

                PasswordDBField mailpassField = (PasswordDBField) user.getField(userSchema.MAILPASSWORD2);
                String mailpass = mailpassField.getPlainText();

                mailCredentials.print(mailUsername);
                mailCredentials.print(" ");
                mailCredentials.println(mailpass);

                extIMAPCredentials.print(username);
                extIMAPCredentials.print(" ");
                extIMAPCredentials.print(mailUsername);
                extIMAPCredentials.print(" ");
                extIMAPCredentials.println(mailpass);

                if (user.isDefined(userSchema.OLDMAILUSER) && user.isDefined(userSchema.OLDMAILPASSWORD2))
                  {
                    mailUsernameField = (StringDBField) user.getField(userSchema.OLDMAILUSER);
                    mailUsername = (String) mailUsernameField.getValueLocal();

                    mailpassField = (PasswordDBField) user.getField(userSchema.OLDMAILPASSWORD2);
                    mailpass = mailpassField.getPlainText();

                    mailCredentials.print(mailUsername);
                    mailCredentials.print(" ");
                    mailCredentials.println(mailpass);

                    extIMAPCredentials.print(username);
                    extIMAPCredentials.print(" ");
                    extIMAPCredentials.print(mailUsername);
                    extIMAPCredentials.print(" ");
                    extIMAPCredentials.println(mailpass);
                  }
              }
          }
        finally
          {
            extIMAPCredentials.close();
          }
      }
    finally
      {
        mailCredentials.close();
      }

    return true;
  }

  /**
   * <p>This method generates a transitive closure of the members of a
   * user netgroup, including all users in all member netgroups,
   * recursively.</p>
   */

  private Vector netgroupMembers(DBObject object)
  {
    return netgroupMembers(object, null, null);
  }

  /**
   * <p>This method generates a transitive closure of the members of a
   * user netgroup, including all users in all member netgroups,
   * recursively.</p>
   */

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

    StringBuilder buffer = new StringBuilder();
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

    StringBuilder buffer = new StringBuilder();
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

  // ***
  //
  // The following private methods are used to support the DNS builder logic.
  //
  // ***

  /**
   * <p>This method generates a file that maps i.p. addresses to mac addresses, system names,
   * room of the system, and usernames (if any).  This method must be run during
   * builderPhase1 so that it has access to the getObjects() method
   * from our superclass.</p>
   */

  private boolean writeSysDataFile()
  {
    PrintWriter sys_dataFile = null;

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

        for (DBObject system: getObjects(systemSchema.BASE))
          {
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
   * builderPhase1 so that it has access to the getObjects() method
   * from our superclass.
   *
   */

  private boolean writeSysFile()
  {
    PrintWriter hosts_info = null;

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

        for (DBObject system: getObjects(systemSchema.BASE))
          {
            writeSystem(system, hosts_info);
          }

        // now the interfaces

        for (DBObject interfaceObj: getObjects(interfaceSchema.BASE))
          {
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
   * This method writes out a type 1 line to the hosts_info DNS source file.<br/><br/>
   *
   * The lines in this file look like the following:<br/><br/>
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

    result.append(": : ");      // no admins
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
   * This method writes out a type 2 line to the hosts_info DNS source file.<br/><br/>
   *
   * The lines in this file look like the following:<br/><br/>
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

    if (object.isDefined(interfaceSchema.ETHERNETINFO))
      {
        MAC = (String) object.getFieldValueLocal(interfaceSchema.ETHERNETINFO);

        // We want to use dashes to separate the hex bytes in our ethernet addr

        MAC = MAC.replace(':','-');
      }
    else
      {
        MAC = "00-00-00-00-00-00";
      }

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

  // ***
  //
  // The following private methods are used to support the DHCP emitter logic.
  //
  // ***

  /**
   * This method writes out the ISC DHCP server
   * configuration file from the data in the Ganymede data store.
   *
   * The pieces of this file include:
   *   Custom Option Declarations.
   *   _GLOBAL_ dhcp network options
   *   List of DHCP Network settings
   *   List of System DHCP settings
   */

  private boolean writeDHCPFile()
  {
    NullWriter nullWriter = new NullWriter();
    PrintWriter dhcpFileWriter = null;
    List<DBObject> networks = null;

    // Do a first pass to collect the customOptions declarations we'll
    // need up top, using a side effect of writeDHCPNetwork() and
    // writeDHCPSystem().

    this.customOptions = new HashSet();

    networks = (List<DBObject>) java.util.Collections.list(enumerateObjects(dhcpNetworkSchema.BASE));
    java.util.Collections.sort(networks, new NetworkSortByName());

    for (DBObject networkObject: networks)
      {
        writeDHCPNetwork(networkObject, nullWriter);
      }

    for (DBObject systemObject: getObjects(systemSchema.BASE))
      {
        writeDHCPSystem(systemObject, nullWriter);
      }

    // okay, we've got our custom options, we can go ahead and write
    // out the file in a single, second pass

    try
      {
        try
          {
            dhcpFileWriter = openOutFile(path + "new_dhcpd.conf", "gasharl");
          }
        catch (IOException ex)
          {
            Ganymede.debug("GASHBuilderTask.writeDHCPFile(): couldn't open new_dhcpd_temp file: " + ex);
            return false;
          }

        dhcpFileWriter.println("# Generated by Ganymede GASHBuilderTask, revision $Rev$");
        dhcpFileWriter.println("# " + new Date().toString());
        dhcpFileWriter.println("#");
        dhcpFileWriter.println("# NOTE: This file, in its entirety, is now being generated by Ganymede.");
        dhcpFileWriter.println("#");
        dhcpFileWriter.println("#       To reconfigure global or shared network dhcp options, edit the");
        dhcpFileWriter.println("#       DHCP Network objects in Ganymede under the Configuration section.");
        dhcpFileWriter.println("#");
        dhcpFileWriter.println("#       -- James and Jon");
        dhcpFileWriter.println("#");
        dhcpFileWriter.println("######################################################################");
        dhcpFileWriter.println("");
        dhcpFileWriter.println("authoritative;");
        dhcpFileWriter.println("ddns-update-style none;");
        dhcpFileWriter.println("");

        writeDHCPCustomOptions(dhcpFileWriter);

        dhcpFileWriter.println("\n#===============================================================================");
        dhcpFileWriter.println("# Shared Networks Data");
        dhcpFileWriter.println("#===============================================================================");

        // we're going to sort the DHCPNetwork objects by name so that
        // we are sure to write out the _GLOBAL_ record first.

        networks = (List<DBObject>) java.util.Collections.list(enumerateObjects(dhcpNetworkSchema.BASE));
        java.util.Collections.sort(networks, new NetworkSortByName());

        for (DBObject networkObject: networks)
          {
            writeDHCPNetwork(networkObject, dhcpFileWriter);
          }

        dhcpFileWriter.println("\n#===============================================================================");
        dhcpFileWriter.println("# Per System Data");
        dhcpFileWriter.println("#===============================================================================");

        for (DBObject systemObject: getObjects(systemSchema.BASE))
          {
            writeDHCPSystem(systemObject, dhcpFileWriter);
          }

        return true;
      }
    finally
      {
        this.customOptions = null;

        if (dhcpFileWriter != null)
          {
            dhcpFileWriter.close();
          }
      }
  }

  /**
   * This method writes out all DHCP custom option declarations
   * discovered during generation of the shared network and individual
   * host record definitions in the writeDHCPFile() method, which in
   * turn calls us so that we can write these options to the top of
   * the generated dhcp_dataFile.
   */

  private boolean writeDHCPCustomOptions(PrintWriter writer)
  {
    if (this.customOptions.size() == 0)
      {
        return true;
      }

    writer.println("# Custom Option Declarations");
    writer.println("#===============================================================================");

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
                    writer.println("option space " + optionSpace + ";");
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

            writer.println("option " + name + " code " + code + " = " + type + ";");
          }
      }

    writer.println("#===============================================================================");

    return true;
  }

  /**
   * This method writes out DHCP info for a shared network to the new dhcpd file.
   *
   * @param object An object from the Ganymede system object base
   * @param writer The destination for this system line
   */

  private void writeDHCPNetwork(DBObject object, Writer writer)
  {
    String name = null;
    IPDBField ipField = null;
    String network_number = "";
    String network_mask = "";
    HashMap options = new HashMap();

    name = (String) object.getFieldValueLocal(dhcpNetworkSchema.NAME);

    // If global, just write out options only now.

    if (name.equals("_GLOBAL_") && object.isDefined(dhcpNetworkSchema.OPTIONS))
      {
        findDHCPOptions(options, object.getFieldValuesLocal(dhcpNetworkSchema.OPTIONS));
        writeDHCPOptionList(options, object, writer, "");
        return;
      }

    try
      {
        writer.write("\n#===============================================================================\n");
        writer.write("shared-network " + name + "\n");

        ipField = (IPDBField) object.getField(dhcpNetworkSchema.NETWORK_NUMBER);

        if (ipField != null)
          {
            network_number = (String) ipField.getEncodingString();
          }

        ipField = (IPDBField) object.getField(dhcpNetworkSchema.NETWORK_MASK);

        if (ipField != null)
          {
            network_mask = (String) ipField.getEncodingString();
          }

        writer.write("{\n");
        writer.write("\tsubnet\t" + network_number + "\tnetmask\t\t" + network_mask + "\n");
        writer.write("\t{ \n");

        if (object.isDefined(dhcpNetworkSchema.OPTIONS))
          {
            findDHCPOptions(options, object.getFieldValuesLocal(dhcpNetworkSchema.OPTIONS));
            writeDHCPOptionList(options, object, writer, "\t\t");
          }

        if (object.isSet(dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS))
          {
            writer.write("\t\tpool\n");
            writer.write("\t\t{\n");
            String guest_range = (String) object.getFieldValueLocal(dhcpNetworkSchema.GUEST_RANGE);
            HashMap options2 = new HashMap();
            writer.write("\t\t\trange\t" + guest_range + ";\n");

            if (object.isDefined(dhcpNetworkSchema.GUEST_OPTIONS))
              {
                findDHCPOptions(options2, object.getFieldValuesLocal(dhcpNetworkSchema.GUEST_OPTIONS));
                writeDHCPOptionList(options2, object, writer, "\t\t\t");
              }

            writer.write("\t\t\tallow known clients;\n");
            writer.write("\t\t}\n");
          }

        writer.write("\t} # END SUBNET " + network_number + "\n");
        writer.write("} # END SHARED-NETWORK " + name + "\n");
      }
    catch (IOException ex)
      {
        System.err.println("GASHBuilderTask.writeDHCPNetwork(): couldn't write to file: " + ex);
      }
  }

  /**
   * This method writes out DHCP info for a single system to the new dhcpd file.
   *
   * @param object An object from the Ganymede system object base
   * @param writer The destination for this system line
   */

  private void writeDHCPSystem(DBObject object, Writer writer)
  {
    String sysname = null;
    Vector interfaceInvids = null;
    DBObject interfaceObj = null;
    IPDBField ipField = null;
    String ipAddress = null;
    StringDBField macField = null;
    String macAddress = null;

    HashMap options = new HashMap();

    StringBuilder buffer = new StringBuilder();

    /* -- */

    interfaceInvids = object.getFieldValuesLocal(systemSchema.INTERFACES);

    if (interfaceInvids == null || interfaceInvids.size() > 1)
      {
        return;                 // we don't write out DHCP for systems
                                // with more than one interface
      }

    interfaceObj = getObject((Invid) interfaceInvids.elementAt(0));

    ipField = (IPDBField) interfaceObj.getField(interfaceSchema.ADDRESS);
    ipAddress = ipField.getEncodingString();

    if (interfaceObj.isDefined(interfaceSchema.ETHERNETINFO))
      {
        macField = (StringDBField) interfaceObj.getField(interfaceSchema.ETHERNETINFO);
        macAddress = macField.getEncodingString();

        if (macAddress.equals("00:00:00:00:00:00"))
          {
            return;             // don't write out DHCP for systems
                                // with unspecified MAC addresses
          }
      }
    else
      {
        return;                 // ditto
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
    //
    // we need to loop twice on this, the first time writing out
    // the fixed address, the second time the roaming definition

    for (int i = 0; i < 2; i++)
      {
        buffer.setLength(0);

        buffer.append("host ");

        if (i == 0)
          {
            buffer.append(sysname);
          }
        else
          {
            buffer.append(sysname + "_roaming");
          }

        buffer.append("\n{\n");

        buffer.append("\thardware ethernet\t\t");
        buffer.append(macAddress);
        buffer.append(";\n");

        if (i == 0)
          {
            // we'll skip this the second time for our roaming entry

            buffer.append("\tfixed-address\t\t\t");
            buffer.append(ipAddress);
            buffer.append(";\n");
          }

        buffer.append("\toption host-name\t\t");
        buffer.append(quote(sysname));
        buffer.append(";\n");

        try
          {
            writer.write(buffer.toString());

            writeDHCPOptionList(options, object, writer, "\t");

            if (i == 0)
              {
                writer.write("} # END host\n\n");
              }
            else
              {
                writer.write("} # END roaming host entry\n\n");
              }
          }
        catch (IOException ex)
          {
            System.err.println("GASHBuilderTask.writeDHCPSystem(): couldn't write to file: " + ex);
          }


        if (options.size() == 0)
          {
            // no custom dhcp, so we don't need to create a roaming
            // entry, just break out

            break;
          }
      }
  }

  /**
   * This method writes out the DHCP option definitions to be
   * contained within a global, shared network, or host record in the
   * generated DHCP file.
   */

  private void writeDHCPOptionList(HashMap options, DBObject object, Writer writer, String tabs)
  {
    Iterator values = options.values().iterator();

    result.setLength(0);

    // first make sure we've declared any site-option-space that
    // we'll need to use

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
                result.append(tabs+"site-option-space\t\t\"" + spaceName + "\";\n");
                spaces.add(spaceName);
              }
            else
              {
                if (!spaces.contains(spaceName))
                  {
                    Ganymede.debug("GASHBuilderTask: writeDHCPSystem() ran into problems with " +
                                   object.getLabel() + " due to conflicting DHCP option spaces.");
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
        StringBuilder hexOptionCodes = new StringBuilder();
        StringBuilder concatPrefix = new StringBuilder();

        values = forcedOptions.iterator();

        while (values.hasNext())
          {
            dhcp_entry entry = (dhcp_entry) values.next();

            if (entry.forced && entry.code != 0)
              {
                if (hexOptionCodes.length() == 0)
                  {
                    hexOptionCodes.append(",");
                  }
                else
                  {
                    hexOptionCodes.append("),");
                  }

                if (concatPrefix.length () == 0)
                  {
                    concatPrefix.append("concat(option dhcp-parameter-request-list");
                  }
                else
                  {
                    concatPrefix.insert(0, "concat(");
                  }

                hexOptionCodes.append(java.lang.Integer.toHexString(entry.code));
              }
          }

        if (hexOptionCodes.length() != 0)
          {
            hexOptionCodes.append(");\n");

            result.append(tabs+"if exists dhcp-parameter-request-list {\n");
            result.append(tabs+"\t# Ganymede forced dhcp options\n");
            result.append(tabs+"\toption dhcp-parameter-request-list = ");
            result.append(concatPrefix);
            result.append(hexOptionCodes);
            result.append(tabs+"}\n");
          }
      }

    // third, let's write out the actual options for this host

    values = options.values().iterator();

    while (values.hasNext())
      {
        dhcp_entry entry = (dhcp_entry) values.next();

        int length = 0;

        result.append(tabs);

        if (!entry.builtin)
          {
            result.append("option ");
            length = 7;
          }
        else
          {
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
            result.append(";\n");
          }
        else
          {
            result.append(entry.value);
            result.append(";\n");
          }
      }

    try
      {
        writer.write(result.toString());
      }
    catch (IOException ex)
      {
        System.err.println("GASHBuilderTask.writeDHCPOptionList(): couldn't write to file: " + ex);
      }
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

        resultMap.put(typeName, new dhcp_entry(typeName,
                                               typeString,
                                               value,
                                               optionObject.isSet(dhcpOptionSchema.BUILTIN),
                                               typecode,
                                               optionObject.isSet(dhcpOptionSchema.FORCESEND)));

        if (!optionObject.isSet(dhcpOptionSchema.BUILTIN) &&
            optionObject.isSet(dhcpOptionSchema.CUSTOMOPTION))
          {
            this.customOptions.add(optionInvid);
          }
      }
  }

  /**
   * This method returns the Invid for the 'normal' user category in
   * the gasharl schema from our local cache if we've looked it up
   * before, or else scans the user category objects looking for it.
   *
   * Note that this only works so long as scanCategories() has the
   * proper constant for the name of the normal user category.
   */

  private Invid getNormalCategory()
  {
    if (this.normalCategory != null)
      {
        return this.normalCategory;
      }

    scanCategories();

    return this.normalCategory;
  }

  /**
   * This method returns the Invid for the 'agency worker' user
   * category in the gasharl schema from our local cache if we've
   * looked it up before, or else scans the user category objects
   * looking for it.
   *
   * Note that this only works so long as scanCategories() has the
   * proper constant for the name of the agency worker category.
   */

  private Invid getAgencyCategory()
  {
    if (this.agencyCategory != null)
      {
        return this.agencyCategory;
      }

    scanCategories();

    return this.agencyCategory;
  }

  /**
   * This method scans the user category object base in order to
   * identify the normal worker and agency worker user category
   * invids.
   */

  private void scanCategories()
  {
    this.normalCategory = findLabeledObject(normalCategoryLabel, userCategorySchema.BASE);
    this.agencyCategory = findLabeledObject(agencyCategoryLabel, userCategorySchema.BASE);

    if (this.normalCategory == null)
      {
        Ganymede.debug("ERROR: GASHBuilderTask.scanCategories() couldn't find the " + normalCategoryLabel + " user category!");
      }

    if (this.agencyCategory == null)
      {
        Ganymede.debug("ERROR: GASHBuilderTask.scanCategories() couldn't find the " + agencyCategoryLabel + " user category!");
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      dhcp_entry

------------------------------------------------------------------------------*/

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
  
  /* -- */

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

/*------------------------------------------------------------------------------
                                                                           class
                                                               NetworkSortByName

------------------------------------------------------------------------------*/

/**
 * Comparator to aid in sorting DHCPNetwork objects in the Ganymede server.
 */

class NetworkSortByName implements Comparator<DBObject>
{
  public int compare(DBObject o1, DBObject o2)
  {
    String s1 = (String) o1.getFieldValueLocal(dhcpNetworkSchema.NAME);
    String s2 = (String) o2.getFieldValueLocal(dhcpNetworkSchema.NAME);
    return s1.compareTo(s2);
  }
}
