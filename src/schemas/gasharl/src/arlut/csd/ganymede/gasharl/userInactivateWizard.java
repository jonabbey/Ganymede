/*

   userInactivateWizard.java

   A wizard to manage step-by-step interactions for the userCustom object.

   Created: 29 January 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

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
import java.util.Date;

import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.GanymediatorWizard;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                            userInactivateWizard

------------------------------------------------------------------------------*/

/**
 *
 * A wizard to handle the wizard interactions required when a user inactivates
 * a user account.
 *
 * <br>This wizard, unlike the userHomeGroupDelWizard and userRenameWizard, does
 * not directly manipulate fields in the user object.  Instead, it works with
 * methods written for its benefit in userCustom.<br>
 *
 * @see arlut.csd.ganymede.common.ReturnVal
 * @see arlut.csd.ganymede.rmi.Ganymediator
 * @see arlut.csd.ganymede.gasharl.userCustom
 */

public class userInactivateWizard extends GanymediatorWizard {

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

  /**
   *
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The user object that this wizard will work with.
   * @param ckp_label The checkpoint label used to finish up the
   * inactivation
   *
   */

  public userInactivateWizard(GanymedeSession session,
                              userCustom userObject,
                              String ckp_label) throws RemoteException
  {
    super(session);             // ** register ourselves **

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
    // failure.. need to do the rollback that would have
    // originally been done for us if we hadn't gone through
    // the wizard process.

    userObject.finalizeInactivate(false, ckp_label);

    return fail("User Inactivation Canceled",
                "User Inactivation Canceled",
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

    System.err.println("userInactivateWizard: creating inactivation wizard");

    buffer.append("Inactivating ");
    buffer.append(userObject.getLabel());
    buffer.append("\n\nThis user account will be rendered unusable, but will be ");
    buffer.append("kept in the database for 3 months to preserve accounting information.\n\n");
    buffer.append("It is recommended that you provide a forwarding email address for this user.");

    retVal = continueOn("User Inactivation Dialog",
                        buffer.toString(),
                        "OK",
                        "Cancel",
                        "question.gif");

    StringDBField addrField = userObject.getStringField(userSchema.EMAILTARGET);

    if (addrField == null || addrField.size() == 0)
      {
        retVal.getDialog().addString("Forwarding Address");
      }
    else
      {
        retVal.getDialog().addString("Forwarding Address", addrField.getValueString());
      }

    return retVal;
  }

  /**
   *
   * This method expects a dialog with a forwarding
   * address stored on key "Forwarding Address"
   *
   */

  public ReturnVal processDialog1()
  {
    ReturnVal retVal = null;

    /* -- */

    String forward = (String) getParam("Forwarding Address");

    // and do the inactivation

    retVal = userObject.inactivate(forward, true, ckp_label);

    if (retVal == null || retVal.didSucceed())
      {
        Date date = (Date) userObject.getFieldValueLocal(SchemaConstants.RemovalField);

        String message = userObject.getLabel() +
          " has been inactivated, and will be removed on " +
          date.toString();

        return success("User Inactivation Performed",
                       message,
                       "OK",
                       null,
                       "ok.gif").unionRescan(retVal);
      }
    else
      {
        // failure.. need to do the rollback that would have
        // originally been done for us if we hadn't gone through
        // the wizard process.

        userObject.finalizeInactivate(false, ckp_label);
      }

    return retVal;
  }
}
