/*

   userInactivateWizard.java

   A wizard to manage step-by-step interactions for the userCustom object.
   
   Created: 29 January 1998
   Version: $Revision: 1.2 $ %D%
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

  int state;

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
   * This method is used to provide feedback to the server from a client
   * in response to a specific request. 
   *
   * @param returnHash a hashtable mapping strings to values.  The strings
   * are titles of fields specified in a dialog that was provided to the
   * client.  If returnHash is null, this corresponds to the user hitting
   * cancel on such a dialog.
   *
   * @see arlut.csd.ganymede.Ganymediator
   * @see arlut.csd.ganymede.ReturnVal
   *
   */

  public ReturnVal respond(Hashtable returnHash)
  {
    JDialogBuff dialog;
    ReturnVal retVal = null;

    /* -- */

    if (state == 1)
      {
	System.err.println("userInactivateWizard: state 1 processing return vals from dialog");

	if (returnHash == null)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("User Inactivation Canceled",
				     "User Inactivation Canceled",
				     "OK",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves

	    return retVal;
	  }

	String forward = (String) returnHash.get("Forwarding Address");

	// and do the inactivation
	    
	retVal = userObject.inactivate(forward, true);

	if (retVal == null || retVal.didSucceed())
	  {
	    retVal = new ReturnVal(true);
	    dialog = new JDialogBuff("User Inactivation Performed",
				     "User has been inactivated",
				     "OK",
				     null,
				     "ok.gif");
		    
	    retVal.setDialog(dialog);
	  }
	else
	  {
	    // failure.. need to do the rollback that would have
	    // originally been done for us if we hadn't gone through
	    // the wizard process.  Look at DBEditObject.inactivate()
	    // method for documentation on this.

	    if (!session.rollback("inactivate" + userObject.getLabel()))
	      {
		retVal = Ganymede.createErrorDialog("userInactivateWizard: Error",
						    "Ran into a problem during user inactivation, and rollback failed");
	      }
	  }

	this.unregister(); // we're stopping here, so we'll unregister ourselves

	return retVal;
      }

    return Ganymede.createErrorDialog("userInactivateWizard: Error",
				      "No idea what you're talking about");	
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
	
    retVal = new ReturnVal(false);
    dialog = new JDialogBuff("User Inactivation Dialog",
			     buffer.toString(),
			     "OK",
			     "Cancel",
			     "question.gif");
    dialog.addString("Forwarding Address");

    retVal.setDialog(dialog);
    retVal.setCallback(this);
    
    state = 1;
    
    return retVal;
  }
}
