/*

   userReactivateWizard.java

   A wizard to manage user reactivation interactions for the userCustom object.

   Created: 29 January 1998
   Version: $Revision: 1.3 $ %D%
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
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator 
 * @see arlut.csd.ganymede.custom.userCustom
 * @see arlut.csd.ganymede.custom.userSchema
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

  int state;

  /**
   * The actual user object that this wizard is acting on.
   */

  userCustom userObject;

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
			      userCustom userObject) throws RemoteException
  {
    super(session);		// register ourselves

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

    if (returnHash == null)
      {
	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("User Reactivation Cancelled",
				 "User Reactivation Cancelled",
				 "OK",
				 null,
				 "ok.gif");
	retVal.setDialog(dialog);

	// note that we don't set the callback on the ReturnVal.. this
	// terminates the wizard process
	
	this.unregister(); // we're stopping here, so we'll unregister ourselves
	
	return retVal;
      }

    if (state == 1)
      {
	System.err.println("userReactivateWizard.respond(): state == 1");

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Reactivate User",
				 "",
				 "OK",
				 "Cancel",
				 "question.gif");

	dialog.addPassword("New Password");

	StringDBField stringfield = (StringDBField) userObject.getField(LOGINSHELL);

	userObject.updateShellChoiceList();
	dialog.addChoice("Shell", 
			 userObject.shellChoices.getLabels(),
			 (String) stringfield.getValueLocal());

	dialog.addString("Forwarding Address");
	    
	retVal.setDialog(dialog);
	retVal.setCallback(this); // have the client get back to us
	
	state = 2;

	System.err.println("userReactivateWizard.respond(): state == 1, returning dialog");

	return retVal;
      }
    else if (state == 2)
      {
	if (debug)
	  {
	    System.err.println("userReactivateWizard.respond(): state == 2");

	    Enumeration enum = returnHash.keys();
	    int i = 0;

	    while (enum.hasMoreElements())
	      {
		Object key = enum.nextElement();
		Object value = returnHash.get(key);
		
		System.err.println("Item: (" + i++ + ") = " + key + ":" + value);
	      }
	  } 

	forward = (String) returnHash.get("Forwarding Address");
	shell = (String) returnHash.get("Shell");
	password = (String) returnHash.get("New Password");

	// and do the inactivation.. userObject will consult us for
	// forward, shell, and password
	    
	retVal = userObject.reactivate(this);

	if (retVal == null || retVal.didSucceed())
	  {
	    retVal = new ReturnVal(true);
	    dialog = new JDialogBuff("User Reactivation Performed",
				     "User has been reactivated",
				     "OK",
				     null,
				     "ok.gif");
		    
	    retVal.setDialog(dialog);
	  }
	else if (retVal.getDialog() == null)
	  {
	    // failure.. need to do the rollback that would have originally
	    // been done for us if we hadn't gone through the wizard process

	    if (!session.rollback("reactivate" + userObject.getLabel()))
	      {
		retVal = Ganymede.createErrorDialog("userReactivateWizard: Error",
						    "Ran into a problem during user reactivation, and rollback failed");
	      }
	  }
	else
	  {
	    return retVal;
	  }

	this.unregister(); // we're stopping here, so we'll unregister ourselves

	return retVal;
      }
	
    return Ganymede.createErrorDialog("userReactivateWizard: Error",
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

    System.err.println("userReactivateWizard: creating reactivation wizard");

    buffer.append("Reactivating ");
    buffer.append(userObject.getLabel());
    buffer.append("\n\nIn order to reactivate this account, you need to provide a password, ");
    buffer.append("a login shell, and a new address to send email for this account to.");
	
    retVal = new ReturnVal(false);
    dialog = new JDialogBuff("User Reactivation Dialog",
			     buffer.toString(),
			     "Next",
			     "Cancel",
			     "question.gif");
    
    retVal.setDialog(dialog);
    retVal.setCallback(this);
    
    state = 1;
    
    return retVal;
  }
}
