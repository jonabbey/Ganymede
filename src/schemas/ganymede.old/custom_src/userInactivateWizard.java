/*

   userInactivateWizard.java

   A wizard to manage step-by-step interactions for the userCustom object.
   
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
                                                            userInactivateWizard

------------------------------------------------------------------------------*/

/**
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator
 */

public class userInactivateWizard extends GanymediatorWizard {

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

  public userInactivateWizard(GanymedeSession session, 
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
	System.err.println("userInactivateWizard: USER_INACTIVATE state 1 processing return vals from dialog");

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

	Enumeration enum = returnHash.keys();
	int i = 0;

	while (enum.hasMoreElements())
	  {
	    Object key = enum.nextElement();
	    Object value = returnHash.get(key);
		
	    System.err.println("Item: (" + i++ + ") = " + key + ":" + value);
	  }
	    
	String forward = (String) returnHash.get("Forwarding Address");

	// and do the inactivation
	    
	retVal = ((userCustom) object).inactivate(forward, true);

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
	    // failure.. need to do the rollback that would have originally
	    // been done for us if we hadn't gone through the wizard process

	    if (!object.getEditSet().rollback("inactivate" + object.getLabel()))
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

    /* -- */

    System.err.println("userInactivateWizard: creating inactivation wizard");

    buffer.append("Inactivating ");
    buffer.append(object.getLabel());
    buffer.append("\n\nThis user account will be rendered unusable, but will be ");
    buffer.append("kept in the database for 3 months to preserve accounting information.\n\n");
    buffer.append("It is recommended that you provide a forwarding email address for this user. ");
	
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
