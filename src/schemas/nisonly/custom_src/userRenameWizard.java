/*

   userRenameWizard.java

   A wizard to manage user rename interactions for the userCustom object.
   
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
                                                                userRenameWizard

------------------------------------------------------------------------------*/

/**
 * A wizard to handle the wizard interactions required when a user is
 * renamed.  All that this wizard actually does is pop up a dialog box
 * advising the user about the implications of renaming a user
 * account, and asking the user for a confirmation that he really
 * wants to do this.
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator 
 */

public class userRenameWizard extends GanymediatorWizard {

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

  /* -- */

  /**
   *
   * Constructor
   *
   */

  /**
   *
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The user object that this wizard will work with.
   * @param newname The proposed new name for the user.
   *
   */

  public userRenameWizard(GanymedeSession session, 
         		  userCustom userObject, 
		          DBField field,
		          String newname) throws RemoteException
  {
    super(session);		// register ourselves

    this.session = session;
    this.userObject = userObject;
    this.field = field;
    this.newname = newname;
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
    boolean aborted;

    /* -- */

    if (state == 1)
      {
	System.err.println("userRenameWizard: USER_RENAME state 1 processing return vals from dialog");

	if (returnHash != null)
	  {
	    Boolean answer = (Boolean) returnHash.get("Yes, I'm sure I want to do this");
	    aborted = (answer == null) || !answer.booleanValue();
	  }
	else
	  {
	    aborted = true;
	  }

	if (aborted)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("User Rename Cancelled",
				     "OK, good decision.",
				     "Yeah, I guess",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves

	    return retVal;
	  }

	Enumeration enum = returnHash.keys();
	int i = 0;

	while (enum.hasMoreElements())
	  {
	    Object key = enum.nextElement();
	    Object value = returnHash.get(key);
	    
	    System.err.println("Item: (" + i++ + ") = " + key + ":" + value);
	  }
	
	System.err.println("userRenameWizard: Calling field.setValue()");

	state = DONE;		// let the userCustom wizardHook know to go 
				// ahead and pass this operation through now

	// note that this setValue() operation will pass
	// through userObject.wizardHook().  wizardHook will see that we are
	// an active userRenameWizard, and are at state DONE, so it
	// will go ahead and unregister us and let the name change
	// go through to completion.

	retVal = field.setValue(newname);
	System.err.println("userRenameWizard: Returned from field.setValue()");

	if (retVal == null)
	  {
	    retVal = new ReturnVal(true); // should cause DBField.setValue() to proceed
	    dialog = new JDialogBuff("User Rename Performed",
				     "OK, buddy, your funeral.",
				     "Thanks a lot",
				     null,
				     "ok.gif");
	    
	    retVal.setDialog(dialog);
	  }
	
	// just in case the setValue didn't go through

	this.unregister(); // we're stopping here, so we'll unregister ourselves

	System.err.println("Returning confirmation dialog");
	
	return retVal;
      }

    // are we in an unexpected state?

    return Ganymede.createErrorDialog("userRenameWizard: Error",
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

    retVal = new ReturnVal(false);
    dialog = new JDialogBuff("User Rename Dialog",
			     "Warning.\n\n" + 
			     "Renaming a user is a serious operation, with serious potential consequences.\n\n"+
			     "If you rename this user, the user's directory and mail file will need to be renamed.\n\n"+
			     "Any scripts or programs that refer to this user's name will need to be changed.",
			     "OK",
			     "Never Mind",
			     "question.gif");
    dialog.addBoolean("Yes, I'm sure I want to do this");

    retVal.setDialog(dialog);
    retVal.setCallback(this);

    state = 1;
    
    return retVal;
  }
}
