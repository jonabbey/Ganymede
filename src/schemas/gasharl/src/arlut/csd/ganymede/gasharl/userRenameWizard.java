/*

   userRenameWizard.java

   A wizard to manage user rename interactions for the userCustom object.

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

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.GanymediatorWizard;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                userRenameWizard

------------------------------------------------------------------------------*/

/**
 * A wizard to handle the wizard interactions required when a user is
 * renamed.  All that this wizard actually does is pop up a dialog box
 * advising the user about the implications of renaming a user
 * account, and asking the user for a confirmation that he really
 * wants to do this.
 *
 * @see arlut.csd.ganymede.common.ReturnVal
 * @see arlut.csd.ganymede.rmi.Ganymediator
 */

public class userRenameWizard extends GanymediatorWizard {

  /**
   * The user-level session context that this wizard is acting in.  This
   * object is used to handle necessary checkpoint/rollback activity by
   * this wizard, as well as to handle any necessary label lookups.
   */

  GanymedeSession session;

  /**
   * <p>Keeps track of the state of the wizard.  Each time respond() is called,
   * state is checked to see what results from the user are expected and
   * what the appropriate dialogs or actions to perform in turn are.</p>
   *
   * <p>state is also used by the userCustom object to make sure that
   * we have finished our interactions with the user when we tell the
   * user object to go ahead and remove the group.</p>
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

  //  int state; from superclass.. we don't want to shadow it here

  /**
   * The actual user object that this wizard is acting on
   */

  userCustom userObject;

  /**
   * The username field in the user object that we may change
   */

  DBField field;

  /**
   * The proposed new name for the user
   */

  String newname;

  /**
   * The old name for the user
   */

  String oldname;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  /**
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The user object that this wizard will work with.
   * @param newname The proposed new name for the user.
   */

  public userRenameWizard(GanymedeSession session,
                          userCustom userObject,
                          DBField field,
                          String newname,
                          String oldname) throws RemoteException
  {
    super(session);             // register ourselves

    this.session = session;
    this.userObject = userObject;
    this.field = field;
    this.newname = newname;
    this.oldname = oldname;
  }

  /**
   * This method provides a default response if a user
   * hits cancel on a wizard dialog.  This should be
   * subclassed if a wizard wants to provide a more
   * detailed cancel response.
   */

  @Override public ReturnVal cancel()
  {
    return fail("User Rename Cancelled",
                "OK, good decision.",
                "Yeah, I guess",
                null,
                "ok.gif");
  }

  /**
   * This method starts off the wizard process
   */

  public ReturnVal processDialog0()
  {
    ReturnVal retVal = null;

    /* -- */

    retVal = continueOn("User Rename Dialog",
                        "Warning.\n\n" +
                        "Renaming a user is a serious operation, with serious potential consequences.\n\n"+
                        "If you rename this user, the user's directory and mail file will need to be renamed.\n\n"+
                        "Any scripts or programs that refer to this user's name will need to be changed.",
                        "OK",
                        "Never Mind",
                        "question.gif");

    retVal.getDialog().addBoolean("Keep old name as an email alias?");

    return retVal;
  }

  /**
   * The client will call us in this state with a Boolean
   * param for key "Keep old name as an email alias?"
   */

  public ReturnVal processDialog1()
  {
    ReturnVal retVal = null;

    /* -- */

    System.err.println("userRenameWizard: USER_RENAME state 1 processing return vals from dialog");

    Boolean answer = (Boolean) getParam("Keep old name as an email alias?");

    // One thing we need to check is whether the new name that they
    // are wanting is already an alias for them..

    StringDBField aliases = userObject.getStringField(userSchema.ALIASES);

    if (aliases != null)
      {
        if (aliases.containsElementLocal(newname))
          {
            aliases.deleteElementLocal(newname);
          }
      }

    System.err.println("userRenameWizard: Calling field.setValue()");

    state = DONE;               // let the userCustom wizardHook know to go
                                // ahead and pass this operation through now

    // note that this setValue() operation will pass
    // through userObject.wizardHook().  wizardHook will see that we are
    // an active userRenameWizard, and are at state DONE, so it
    // will go ahead and unregister us and let the name change
    // go through to completion.

    try
      {
        retVal = field.setValue(newname);
      }
    catch (GanyPermissionsException ex)
      {
        retVal = Ganymede.createErrorDialog(this.session,
                                            "permissions",
                                            "permissions error setting user name " + ex);
      }

    System.err.println("userRenameWizard: Returned from field.setValue()");

    // now we need to add the old name to the alias list, possibly.

    if (answer != null && answer.booleanValue())
      {
        aliases.addElementLocal(oldname);
      }

    if (retVal == null)
      {
        retVal = success("User Rename Performed",
                         "OK, User renamed.",
                         "Thanks",
                         null,
                         "ok.gif");

        retVal.addRescanField(userObject.getInvid(), userSchema.SIGNATURE);
        retVal.addRescanField(userObject.getInvid(), userSchema.HOMEDIR);
      }
    else if (retVal.didSucceed())
      {
        retVal.setDialog(new JDialogBuff("User Rename Performed",
                                         "OK, User renamed.",
                                         "Thanks",
                                         null,
                                         "ok.gif"));

        retVal.addRescanField(userObject.getInvid(), userSchema.SIGNATURE);
        retVal.addRescanField(userObject.getInvid(), userSchema.HOMEDIR);
      }

    return retVal;
  }
}
