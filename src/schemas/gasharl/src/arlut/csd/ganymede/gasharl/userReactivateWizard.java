/*

   userReactivateWizard.java

   A wizard to manage user reactivation interactions for the userCustom object.

   Created: 29 January 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
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

import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.GanymediatorWizard;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                            userReactivateWizard

------------------------------------------------------------------------------*/
/**
 *
 * A wizard to handle the wizard interactions required when a user reactivates
 * a user account.
 *
 * <br>This wizard, unlike the userHomeGroupDelWizard and userRenameWizard, does
 * not directly manipulate fields in the user object.  Instead, it works with
 * methods written for its benefit in userCustom.<br>
 *
 * <br>Object Reactivation basically consists of rendering an object fit for use
 * once again, by clearing the removal date, and fixing up any fields that need
 * fixing.<br>
 *
 * @see arlut.csd.ganymede.common.ReturnVal
 * @see arlut.csd.ganymede.rmi.Ganymediator
 * @see arlut.csd.ganymede.gasharl.userCustom
 * @see arlut.csd.ganymede.gasharl.userSchema
 */

public class userReactivateWizard extends GanymediatorWizard implements userSchema {

  static final boolean debug = false;

  // ---

  /**
   * The user-level session context that this wizard is acting in.  This
   * object is used to handle necessary checkpoint/rollback activity by
   * this wizard, as well as to handle any necessary label lookups.
   */

  GanymedeSession session;

  /**
   * Keeps track of the state of the wizard.  Each time respond() is called,
   * state is checked to see what results from the user are expected and
   * what the appropriate dialogs or actions to perform in turn are.<br>
   *
   * state is also used by the userCustom object to make sure that
   * we have finished our interactions with the user when we tell the
   * user object to go ahead and remove the group.  <br>
   *
   * <pre>
   * Values:
   *         1 - Wizard has been initialized, initial explanatory dialog
   *             has been generated.
   *         2 - Wizard has generated the second dialog and is waiting
   *             for the user to provide the password, shell, and forwarding
   *             addresses needed to reactivate this account.
   * DONE (99) - Wizard has approved the proposed action, and is signalling
   *             the user object code that it is okay to proceed with the
   *             action without further consulting this wizard.
   * </pre>
   */

  //  int state; from superclass

  /**
   * The actual user object that this wizard is acting on.
   */

  userCustom userObject;

  String ckp_label;

  // reactivation params

  // These variables are directly accessed by userCustom when this
  // wizard calls back to the userObject to perform the reactivation
  // logic.

  String password;
  String shell;
  String forward;

  /**
   *
   * Constructor
   *
   */

  public userReactivateWizard(GanymedeSession session,
                              userCustom userObject,
                              String ckp_label) throws RemoteException
  {
    super(session);             // register ourselves

    this.session = session;
    this.userObject = userObject;
    this.ckp_label = ckp_label;
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
    return fail("User Reactivation Canceled",
                "User Reactivation Canceled",
                "OK",
                null,
                "ok.gif");
  }

  /**
   *
   * This method starts off the wizard process
   *
   */

  public ReturnVal processDialog0()
  {
    StringBuffer buffer = new StringBuffer();
    ReturnVal retVal = null;

    /* -- */

    System.err.println("userReactivateWizard: creating reactivation wizard");

    buffer.append("Reactivating ");
    buffer.append(userObject.getLabel());
    buffer.append("\n\nIn order to reactivate this account, you need to provide a password, ");
    buffer.append("a login shell, and a new address to send email for this account to.");

    retVal = continueOn("User Reactivation Dialog",
                        buffer.toString(),
                        "Next",
                        "Cancel",
                        "question.gif");

    return retVal;
  }

  /**
   *
   * If we have gotten here, the user clicked Ok when asked
   * if he really wanted to reactivate this user.
   *
   */

  public ReturnVal processDialog1()
  {
    if (debug)
      {
        System.err.println("userReactivateWizard.respond(): state == 1");
      }

    return genPhase2("Reactivate User", "");
  }

  /**
   *
   * The user will have provided us with all the reactivation information
   * if processDialog2() is called..<br><br>
   *
   * <pre>
   * Keys:
   *
   * "Email Target"
   * "Shell"
   * "New Password"
   * </pre>
   *
   */

  public ReturnVal processDialog2()
  {
    ReturnVal retVal = null;

    /* -- */

    if (debug)
      {
        System.err.println("userReactivateWizard.respond(): state == 2");

        Enumeration en = getKeys();
        int i = 0;

        while (en.hasMoreElements())
          {
            Object key = en.nextElement();
            Object value = getParam(key);

            System.err.println("Item: (" + i++ + ") = " + key + ":" + value);
          }
      }

    forward = (String) getParam("Email Target");
    shell = (String) getParam("Shell");
    password = (String) getParam("New Password");

    if (password == null || password.length() == 0)
      {
        setNextState(2);        // try again

        retVal = genPhase2("Reactivate User", "You must set a password");

        return retVal;
      }

    // and do the reactivation.. userObject will consult us for

    // forward, shell, and password

    retVal = userObject.reactivate(this, ckp_label);

    if (retVal == null || retVal.didSucceed())
      {
        return success("User Reactivation Performed",
                       "User has been reactivated",
                       "OK",
                       null,
                       "ok.gif").unionRescan(retVal);
      }

    return retVal;
  }

  /**
   * <p>Generate the dialog to be processed by processDialog2</p>
   */

  private ReturnVal genPhase2(String title, String body)
  {
    ReturnVal retVal = continueOn(title,
                                  body,
                                  "OK",
                                  "Cancel",
                                  "question.gif");

    retVal.getDialog().addPassword("New Password", true);

    StringDBField stringfield = userObject.getStringField(LOGINSHELL);

    userObject.updateShellChoiceList();
    retVal.getDialog().addChoice("Shell",
                                 userObject.shellChoices.getLabels(),
                                 (String) stringfield.getValueLocal());

    StringDBField addrField = userObject.getStringField(EMAILTARGET);

    if (addrField != null && addrField.size() > 0)
      {
        retVal.getDialog().addString("Email Target", addrField.getValueString());
      }
    else
      {
        retVal.getDialog().addString("Email Target");
      }

    System.err.println("userReactivateWizard.respond(): state == 1, returning dialog");

    return retVal;
  }
}
