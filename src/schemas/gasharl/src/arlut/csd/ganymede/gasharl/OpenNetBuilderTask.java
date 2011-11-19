 /*

   OpenNetBuilderTask.java

   This class is intended to dump the Ganymede datastore to the open
   net NIS domain.

   Created: 18 November 2011

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2011
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
import java.util.Date;
import java.util.Vector;

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
import arlut.csd.ganymede.server.InvidDBField;
import arlut.csd.ganymede.server.PasswordDBField;
import arlut.csd.ganymede.server.StringDBField;
import arlut.csd.ganymede.server.ServiceNotFoundException;
import arlut.csd.ganymede.server.ServiceFailedException;

/*------------------------------------------------------------------------------
                                                                           class
                                                              OpenNetBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to the open net NIS.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class OpenNetBuilderTask extends GanymedeBuilderTask {

  private static String path = null;
  private static String buildScript = null;

  // ---

  private Date now = null;
  private SharedStringBuffer result = new SharedStringBuffer();

  /* -- */

  public OpenNetBuilderTask(Invid _taskObjInvid)
  {
    // set the taskDefObjInvid in GanymedeBuilderTask so
    // we can look up option strings

    taskDefObjInvid = _taskObjInvid;
  }

  /**
   * <p>This method runs with a dumpLock obtained for the builder
   * task.</p>
   *
   * <p>Code run in builderPhase1() can call getObjects() and
   * baseChanged().</p>
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   */

  public boolean builderPhase1()
  {
    PrintWriter out;
    boolean success = false;

    /* -- */

    Ganymede.debug("OpenNetBuilderTask builderPhase1 running");

    if (path == null)
      {
        path = System.getProperty("opennet.path");

        if (path == null)
          {
            throw new RuntimeException("OpenNetBuilder not able to determine output directory.. need to set the opennet.path property in ganymede.properties.");
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
            out = openOutFile(path + "passwd", "opennet");
          }
        catch (IOException ex)
          {
            System.err.println("OpenNetBuilderTask.builderPhase1(): couldn't open passwd file: " + ex);
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

	out = null;

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
            out = openOutFile(path + "group", "opennet");
          }
        catch (IOException ex)
          {
            System.err.println("OpenNetBuilderTask.builderPhase1(): couldn't open group file: " + ex);
          }

        try
          {
            for (DBObject group: getObjects(groupSchema.BASE))
              {
                if (out != null)
                  {
                    writeGroupLine(group, out);
                  }
              }
          }
        finally
          {
            if (out != null)
              {
                out.close();
              }
          }

        success = true;
      }

    Ganymede.debug("OpenNetBuilderTask builderPhase1 completed");

    return success;
  }

  /**
   * <p>This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.</p>
   *
   * <p>builderPhase2 is only run if builderPhase1 returns true.</p>
   */

  public boolean builderPhase2()
  {
    File
      file;

    /* -- */

    Ganymede.debug("OpenNetBuilderTask builderPhase2 running");

    if (buildScript == null)
      {
        buildScript = System.getProperty("opennet.scriptlocation");

        if (buildScript == null)
          {
            Ganymede.debug("OpenNetBuilderTask couldn't find any property definition for opennet.scriptlocation.");
            Ganymede.debug("\nNot executing external build for OpenNetBuilderTask.");
            return false;
          }

        buildScript = PathComplete.completePath(buildScript);
        buildScript = buildScript + "openbuilder";
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

        String message = "Error encountered running sync script \"" + path + "\" for the OpenNetBuilderTask." +
          "\n\nI got a result code of " + resultCode + " when I tried to run it.";

        DBLogEvent event = new DBLogEvent("externalerror", message, null, null, null, null);

        Ganymede.log.logSystemEvent(event);

	if (startedOk)
	  {
	    throw new ServiceFailedException("open net builder returned a failure code: " + resultCode);
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

    Ganymede.debug("OpenNetBuilderTask builderPhase2 completed");

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
        cryptedPass = passField.getShaUnixCryptText();
      }
    else
      {
        inactivated = true;

        // System.err.println("OpenNetBuilder.writeUserLine(): null password for user " + username);
        cryptedPass = "**Nopass**";
      }

    uid = ((Integer) object.getFieldValueLocal(userSchema.UID)).intValue();

    // extra security precaution.. homey don't play no root accounts in NIS games.

    if (uid == 0)
      {
        Ganymede.debug("OpenNetBuilder.writeUserLine(): *** root uid in user " + username + ", skipping!! ***");
        return;                 // no writeLine
      }

    // get the gid

    groupInvid = (Invid) object.getFieldValueLocal(userSchema.HOMEGROUP); // home group

    if (groupInvid == null)
      {
        System.err.println("OpenNetBuilder.writeUserLine(): null gid for user " + username);
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

    if (result.length() > 1024)
      {
        System.err.println("OpenNetBuilder.writeUserLine(): Warning!  user " +
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
        // System.err.println("OpenNetBuilder.writeGroupLine(): null user list for group " + groupname);
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
        System.err.println("OpenNetBuilder.writeGroupLine(): Warning!  group " +
                           groupname + " overflows the GASH line length!");
      }

    writer.println(result.toString());
  }
}