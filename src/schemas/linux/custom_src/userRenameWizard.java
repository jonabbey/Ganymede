/*

   userRenameWizard.java

   A wizard to manage user rename interactions for the userCustom object.
   
   Created: 29 January 1998
   Version: $Revision: 1.1 $ %D%
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
 *
 * A wizard to manage user rename interactions for the userCustom object.
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator
 */

public class userRenameWizard extends GanymediatorWizard {

  GanymedeSession session;
  int state;
  DBEditObject object;
  DBField field;
  Object param;
  ReturnVal retVal;

  // reactivation params

  String password;
  String shell;
  String forward;

  /**
   *
   * Constructor
   *
   */

  public userRenameWizard(GanymedeSession session, 
         		  DBEditObject object, 
		          DBField field,
		          Object param) throws RemoteException
  {
    super(session);		// register ourselves

    this.session = session;
    this.object = object;
    this.field = field;
    this.param = param;
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

    /* -- */

    if (state == 1)
      {
	System.err.println("userRenameWizard: USER_RENAME state 1 processing return vals from dialog");

	boolean aborted = false;

	if (returnHash == null)
	  {
	    aborted = true;
	  }
	else
	  {
	    Enumeration enum = returnHash.keys();
	    int i = 0;

	    while (enum.hasMoreElements())
	      {
		Object key = enum.nextElement();
		Object value = returnHash.get(key);

		System.err.println("Item: (" + i++ + ") = " + key + ":" + value);
	      }

	    Boolean answer = (Boolean) returnHash.get("Yes, I'm sure I want to do this");
		
	    aborted = (answer == null) || !answer.booleanValue();
	  }

	if (aborted)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("User Rename Canceled",
				     "OK, good decision.",
				     "Yeah, I guess",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves
	  }
	else
	  {
	    System.err.println("userRenameWizard: Calling field.setValue()");

	    state = DONE;	// let the wizardHook know to go ahead and pass
				// this operation through now

	    retVal = field.setValue(param);
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

	    this.unregister(); // we're stopping here, so we'll unregister ourselves
	  }

	System.err.println("Returning second userRenameWizard dialog");

	return retVal;
      }

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
