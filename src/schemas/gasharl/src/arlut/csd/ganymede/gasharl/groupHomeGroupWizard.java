/*

   groupHomeGroupWizard.java

   A wizard to allow deletion of a user's home group from the group edit window.

   Created: 8 April 1998

   Module by: Mike Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.invid_field;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.GanymediatorWizard;

/*------------------------------------------------------------------------------
                                                                           class
                                                            groupHomeGroupWizard

------------------------------------------------------------------------------*/

public class groupHomeGroupWizard extends GanymediatorWizard implements groupSchema {

  final static int ERROR = -27;

  final boolean debug = true;

  Invid
    userInvid;

  groupCustom
    groupObject;

  GanymedeSession
    session;

  userCustom user = null;

  /* -- */

  /**
   * State
   *
   * 1 - Wizard has shown the first informational dialog.
   * 2 - Wizard is waiting for the user to choose another group.
   * DONE - Wizard is done.
   */

  public groupHomeGroupWizard(GanymedeSession session,
                              groupCustom groupObject,
                              Invid userInvid) throws RemoteException
  {
    super(session);

    this.userInvid = userInvid;
    this.groupObject = groupObject;
    this.session = session;

  }

  /**
   *
   * This method provides a default response if a user
   * hits cancel on a wizard dialog.  This should be
   * subclassed if a wizard wants to provide a more
   * detailed cancel response.
   *
   */

  public ReturnVal cancel()
  {
    return fail("User removal Canceled",
                "User removal Canceled",
                "OK",
                null,
                "ok.gif");
  }

  public ReturnVal processDialog0() throws NotLoggedInException
  {
    print("Starting new dialog");

    user = (userCustom) (session.edit_db_object(userInvid).getObject());

    if (user == null)
      {
        return Ganymede.createErrorDialog(this.session,
                                          "groupHomeGroupWizard error",
                                          "Could not get the user.");
      }

    if (debug)
      {
        System.err.println("groupHomeGroupWizard: creating start dialog.");
      }

    int size = 0;

    try
      {
        size = user.getField(userSchema.GROUPLIST).size();
      }
    catch (RemoteException rx)
      {
        throw new RuntimeException("could not get the size. " + rx);
      }

    // If size is less than one, then there won't be any other groups to change to.

    if (size > 1)
      {
        return continueOn("Home Group Change",
                          "In order to remove a user's home group, you must choose another home group for that user.",
                          "Next", "Cancel", "question.gif");

      }
    else
      {
        // no groups to choose from

        return fail("Home Group Change",
                    "Each user must have a home group.  For that user, this is it.  So don't.",
                    "Sorry", null, "ok.gif");
      }
  }

  public ReturnVal processDialog1() throws NotLoggedInException
  {
    ReturnVal retVal = null;

    /* -- */

    // We have already shown the first info dialog, so now it is time to show
    // the dialog with the choices

    if (debug)
      {
        System.err.println("groupHomeGroupWizard.respond(): state == 1");
      }

    retVal = continueOn("Change Home Group", "",
                        "OK", "Cancel", "question.gif");

    try
      {
        print("Getting values.");

        QueryResult values = ((invid_field)user.getField(userSchema.GROUPLIST)).encodedValues();

        print("Adding choices to dialog.");

        Vector labels = values.getLabels();
        String currentGroup = groupObject.getLabel();

        for (int i = 0; i < labels.size(); i++)
          {
            if (currentGroup.equals((String)labels.elementAt(i)))
              {
                labels.removeElementAt(i);
                break;
              }
          }

        retVal.getDialog().addChoice("Home Group:" , labels, null);
      }
    catch (RemoteException rx)
      {
        throw new RuntimeException("Could not get the groups.");
      }

    if (debug)
      {
        System.err.println("groupHomeGroupWizard.respond(): state == 1, returning dialog");
      }

    return retVal;
  }

  public ReturnVal processDialog2() throws NotLoggedInException
  {
    ReturnVal retVal = null;

    db_field userHomeGroupField;
    Invid newGroup;

    /* -- */

    if (debug)
      {
        System.err.println("groupHomeGroupWizard.respond: state == 2");

        Enumeration en = getKeys();
        int i = 0;

        while (en.hasMoreElements())
          {
            Object key = en.nextElement();
            Object value = getParam(key);
            System.err.println("Item: (" + i++ + ") " + key + ":" + value);
          }
      }

    String gString = (String) getParam("Home Group:");

    // Now we have to do a query to find which group has this name.

    QueryDataNode node = new QueryDataNode(QueryDataNode.EQUALS, gString);
    Query query = new Query(groupObject.getTypeID(), node);
    QueryResult qr = session.query(query);

    if (qr.size() == 1)
      {
        // this is what we want.
        newGroup = qr.getInvid(0);

        userHomeGroupField = user.getField(userSchema.HOMEGROUP);

        try
          {
            if (debug)
              {
                print("Setting user home group field to " + newGroup);
              }

            retVal = userHomeGroupField.setValue(newGroup);
          }
        catch (RemoteException rx)
          {
            throw new RuntimeException("Could not set the value: " + rx);
          }

        if ((retVal == null) || (retVal.didSucceed()))
          {
            ReturnVal ret = success("Home group changed",
                                    "User's home group successfully changed.",
                                    "OK", null, "ok.gif");

            if (debug)
              {
                print("Before union: setValue return: " + retVal.dumpRescanInfo());
                print("Before union: success: " + ret.dumpRescanInfo());
              }

            ReturnVal rv = ret.unionRescan(retVal);

            if (debug)
              {
                print(rv.dumpRescanInfo());
              }

            return(rv);
          }
        else if (retVal.getDialog() == null)
          {
            // we failed :(
            retVal = Ganymede.createErrorDialog(this.session,
                                                "groupHomeGroupWizard: error",
                                                "Ran into trouble during user interaction.");
          }
      }
    else // could not find the group
      {
        retVal = Ganymede.createErrorDialog(this.session,
                                            "groupHomeGroupWizard: error",
                                            "Could not find the group you wanted, Sorry.");
      }

    return retVal;
  }

  private void print(String s)
  {
    System.err.println("groupHomeGroupWizard: " + s);
  }
}
