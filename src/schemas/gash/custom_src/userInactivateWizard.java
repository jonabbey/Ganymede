/*

   userInactivateWizard.java

   A wizard to manage step-by-step interactions for the userCustom object.
   
   Created: 29 January 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import arlut.csd.ganymede.*;

import arlut.csd.JDialog.JDialogBuff;

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
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator 
 * @see arlut.csd.ganymede.custom.userCustom
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

  /**
   *
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The user object that this wizard will work with.
   *
   */

  public userInactivateWizard(GanymedeSession session, 
			      userCustom userObject) throws RemoteException
  {
    super(session);		// ** register ourselves **

    this.session = session;
    this.userObject = userObject;
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
    return fail("User Inactivation Canceled",
		"User Inactivation Canceled",
		"OK",
		null,
		"ok.gif");
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
	    
    retVal = userObject.inactivate(forward, true);

    if (retVal == null || retVal.didSucceed())
      {
	return success("User Inactivation Performed",
		       "User has been inactivated",
		       "OK",
		       null,
		       "ok.gif");
      }
    else
      {
	// failure.. need to do the rollback that would have
	// originally been done for us if we hadn't gone through
	// the wizard process.  Look at DBEditObject.inactivate()
	// method for documentation on this.
	
	if (!session.rollback("inactivate" + userObject.getLabel()))
	  {
	    return Ganymede.createErrorDialog("userInactivateWizard: Error",
					      "Ran into a problem during user inactivation, and rollback failed");
	  }
      }
    
    return retVal;
  }

  /**
   *
   * This method starts off the wizard process
   *
   */

  public ReturnVal getStartDialog()
  {
    JDialogBuff dialog;
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

    retVal.getDialog().addString("Forwarding Address");
    
    return retVal;
  }
}
