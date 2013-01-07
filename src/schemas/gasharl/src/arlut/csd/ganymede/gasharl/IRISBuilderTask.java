/*

   IRISBuilderTask.java

   This class is intended to dump the Ganymede datastore to the ARL:UT
   IRIS system.

   Created: 27 September 2004


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.server.DBLogEvent;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeBuilderTask;
import arlut.csd.ganymede.server.PasswordDBField;
import arlut.csd.ganymede.server.ServiceNotFoundException;
import arlut.csd.ganymede.server.ServiceFailedException;
import arlut.csd.ganymede.common.SchemaConstants;
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
 *  This class is intended to dump the Ganymede datastore to the ARL:UT
 *  IRIS system.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class IRISBuilderTask extends GanymedeBuilderTask {

  static final boolean debug = false;

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
    boolean needBuild = false;

    /* -- */

    Ganymede.debug("build: IRISBuilderTask writing files");

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
        if (debug)
          {
            Ganymede.debug("Need to build IRIS output");
          }

        needBuild = true;

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
                        if (debug)
                          {
                            System.err.println("Writing out IRIS user " + user.getLabel());
                          }

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

    if (debug)
      {
        Ganymede.debug("IRISBuilderTask builderPhase1 completed");
      }

    return needBuild;
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

    Ganymede.debug("build: IRISBuilderTask running build");

    if (buildScript == null)
      {
        buildScript = System.getProperty("ganymede.builder.scriptlocation");
        buildScript = PathComplete.completePath(buildScript);
        buildScript = buildScript + "irisbuilder";
      }

    int resultCode = -999;      // a resultCode of 0 is success

    file = new File(buildScript);

    boolean startedOk = false;

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
        Ganymede.debug(buildScript + " doesn't exist, not running external IRIS build script");
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

        String message = "Error encountered running sync script \"" + path + "\" for the IRISBuilderTask." +
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

    if (debug)
      {
        Ganymede.debug("IRISBuilderTask builderPhase2 completed");
      }

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

    if (netgroups == null)
      {
        return false;
      }

    for (int i = 0; i < netgroups.size(); i++)
      {
        DBObject netgroup = getObject((Invid) netgroups.elementAt(i));

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
    String name = (String) netgroup.getFieldValueLocal(userNetgroupSchema.NETGROUPNAME);

    if (name.equals(netgroupName))
      {
        return true;
      }

    Vector netgroups = netgroup.getFieldValuesLocal(userNetgroupSchema.OWNERNETGROUPS);

    if (netgroups == null)
      {
        return false;
      }

    for (int i = 0; i < netgroups.size(); i++)
      {
        if (netgroup_or_parent_equals(getObject((Invid) netgroups.elementAt(i)), netgroupName))
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

    username = (String) userObject.getFieldValueLocal(userSchema.USERNAME);
    invidString = userObject.getInvid().toString();
    badge = (String) userObject.getFieldValueLocal(userSchema.BADGE);

    PasswordDBField passField = (PasswordDBField) userObject.getField(userSchema.PASSWORD);

    if (passField == null)
      {
        md5Crypt = "*";
        plaintext = "*";
      }
    else
      {
        md5Crypt = passField.getMD5CryptText();
        plaintext = passField.getPlainText();
      }

    StringBuilder output = new StringBuilder();

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

    StringBuilder buffer = new StringBuilder();
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
