/*

   userReactivateWizard.java

   A wizard to manage user reactivation interactions for the userCustom object.

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
                                                            userReactivateWizard

------------------------------------------------------------------------------*/

/**
 *
 * A wizard to manage user reactivation interactions for the userCustom object.
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator
 */

public class userReactivateWizard extends GanymediatorWizard {

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

  public userReactivateWizard(GanymedeSession session, 
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
	System.err.println("userReactivateWizard.respond(): state == 1");

	if (returnHash == null)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("User Reactivation Canceled",
				     "User Reactivation Canceled",
				     "OK",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves

	    return retVal;
	  }

	System.err.println("userReactivateWizard.respond(): state == 1, creating dialog");

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Reactivate User",
				 "",
				 "OK",
				 "Cancel",
				 "question.gif");

	dialog.addPassword("New Password");

	StringDBField stringfield = (StringDBField) object.getField((short) 263);

	((userCustom) object).updateShellChoiceList();
	dialog.addChoice("Shell", 
			 userCustom.shellChoices.getLabels(),
			 (String) stringfield.getValueLocal());

	dialog.addString("Forwarding Address");
	    
	retVal.setDialog(dialog);
	retVal.setCallback(this);
	
	state = 2;

	System.err.println("userReactivateWizard.respond(): state == 1, returning dialog");

	return retVal;
      }
    else if (state == 2)
      {
	System.err.println("userReactivateWizard.respond(): state == 2");

	if (returnHash == null)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("User Reactivation Canceled",
				     "User Reactivation Canceled",
				     "OK",
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
	    
	forward = (String) returnHash.get("Forwarding Address");
	shell = (String) returnHash.get("Shell");
	password = (String) returnHash.get("New Password");

	// and do the inactivation
	    
	retVal = ((userCustom) object).reactivate(this);

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

	    if (!object.getEditSet().rollback("reactivate" + object.getLabel()))
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

    /* -- */

    System.err.println("userReactivateWizard: creating reactivation wizard");

    buffer.append("Reactivating ");
    buffer.append(object.getLabel());
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
