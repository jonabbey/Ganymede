/*

   userHomeGroupDelWizard.java

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
                                                          userHomeGroupDelWizard

------------------------------------------------------------------------------*/

/**
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator
 */

public class userHomeGroupDelWizard extends GanymediatorWizard {

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

  public userHomeGroupDelWizard(GanymedeSession session, 
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
	System.err.println("userHomeGroupDelWizard.respond(): state == 1");

	if (returnHash == null)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("Home Group Removal Canceled",
				     "Home Group Removal Canceled",
				     "OK",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves

	    return retVal;
	  }

	System.err.println("userHomeGroupDelWizard.respond(): state == 1, creating dialog");

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Home Group Change",
				 "What group do you want to make the new default for this user?",
				 "OK",
				 "Cancel",
				 "question.gif");

	// get the list of choices, synthesize a list that contains every choice but
	// the one being deleted

	((userCustom) object).updateGroupChoiceList();

	QueryResult groupChoice = ((userCustom) object).groupChoices;

	// Which group is being deleted?

	Invid val = (Invid) object.getFieldValuesLocal((short) 264).elementAt(((Integer) param).intValue());

	// Make a list of all choices except the one being deleted

	Vector choices = new Vector();

	for (int i = 0; i < groupChoice.size(); i++)
	  {
	    if (!groupChoice.getInvid(i).equals(val))
	      {
		choices.addElement(groupChoice.getLabel(i));
	      }
	  }

	dialog.addChoice("New Home Group", 
			 choices,
			 (String) choices.elementAt(0));

	retVal.setDialog(dialog);
	retVal.setCallback(this);
	
	state = 2;

	System.err.println("userHomeGroupDelWizard.respond(): state == 2, returning dialog");

	return retVal;
      }
    else if (state == 2)
      {
	System.err.println("userHomeGroupDelWizard.respond(): state == 2");

	if (returnHash == null)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("Home Group Removal Canceled",
				     "Home Group Removal Canceled",
				     "OK",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves

	    return retVal;
	  }

	String group = (String) returnHash.get("New Home Group");

	// get the list of groups

	((userCustom) object).updateGroupChoiceList();

	QueryResult groupChoice = ((userCustom) object).groupChoices;

	// find the group we're changing to, find the id, change it

	boolean found = false;
	((userCustom) object).getGSession().checkpoint("homegroupdel" + object.getLabel());

	for (int i = 0; i < groupChoice.size(); i++)
	  {
	    if (groupChoice.getLabel(i).equals(group))
	      {
		found = true;
		retVal = object.setFieldValue((short) 265, groupChoice.getInvid(i));
		break;
	      }
	  }

	if (!found)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("Home Group Removal Canceled",
				     "Home Group Removal Canceled\n\nError, couldn't deal with the group selected",
				     "OK",
				     null,
				     "ok.gif");
	    retVal.setDialog(dialog);

	    this.unregister(); // we're stopping here, so we'll unregister ourselves

	    return retVal;
	  }

	if (retVal == null || retVal.didSucceed())
	  {
	    // we're all systems go, go ahead and do the group
	    // deletion that started this whole thing

	    state = DONE;	// let the wizardHook know to go ahead and pass
				// this operation through now

	    InvidDBField invF = (InvidDBField) object.getField((short) 264);
	    retVal = invF.deleteElement(((Integer) param).intValue());

	    if (retVal == null || retVal.didSucceed())
	      {
		retVal = new ReturnVal(true);
		dialog = new JDialogBuff("Home Group Change Performed",
					 "The user's old home group has been successfully removed, and a new default set",
					 "OK",
					 null,
					 "ok.gif");
		    
		retVal.setDialog(dialog);
	      }
	    else
	      {
		if (!((userCustom) object).getGSession().rollback("homegroupdel" + object.getLabel()))
		  {
		    retVal = Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
							"Ran into a problem during home group deletion, and rollback failed");
		  }
	      }
	  }
	else if (retVal.getDialog() == null)
	  {
	    // argh, failure..

	    if (!object.getEditSet().rollback("homegroupdel" + object.getLabel()))
	      {
		retVal = Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
						    "Ran into a problem during home group change, and rollback failed");
	      }
	  }

	this.unregister(); // we're stopping here, so we'll unregister ourselves

	return retVal;
      }
	
    return Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
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

    System.err.println("userHomeGroupDelWizard: creating home group deletion wizard");
    
    ((userCustom) object).updateGroupChoiceList();
    
    if (((userCustom) object).groupChoices.size() == 1)
      {
	buffer.append("Can't delete lone group for user ");
	buffer.append(object.getLabel());
	buffer.append("\n\nYou may not delete the last group from a user's account.  All active users in UNIX need ");
	buffer.append("to be a member of at least a single account group.");

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("User Home Group Change Dialog",
				 buffer.toString(),
				 "OK",
				 null,
				 "error.gif");

	return retVal;
      }

    buffer.append("Changing home group for user ");
    buffer.append(object.getLabel());
    buffer.append("\n\nThe group you are attempting to remove this user from is the user's default group. ");
    buffer.append("In order to remove the user from this group, you are going to need to select another group ");
    buffer.append("to be the default group for this user at login time.");

    retVal = new ReturnVal(false);
    dialog = new JDialogBuff("User Home Group Change Dialog",
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
