/*

   userHomeGroupDelWizard.java

   A wizard to handle the wizard interactions required when a user attempts
   to delete the group that they have selected for their default group.
   
   Created: 29 January 1998
   Version: $Revision: 1.10 $ %D%
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
 * A wizard to handle the wizard interactions required when a user attempts
 * to delete the group that they have selected for their default group.
 *
 * <br>When a user deletes a group from their list of groups (264)
 * that they are a member of, the userCustom object will check to see
 * if that group is the one they have selected as their default (home)
 * group in field 265.  If so, this wizard gets invoked to make sure
 * that the user understands the consequences of this act, and to solicit
 * from the user their choice of other group to set as their default (home)
 * group.<br>
 *
 * <br>See userSchema.java for a list of field definitions used by this wizard.<br>
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator 
 * @see arlut.csd.ganymede.custom.userSchema
 */

public class userHomeGroupDelWizard extends GanymediatorWizard implements userSchema {
  
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
   *             for the user to either choose a new home group, or
   *             cancel out.
   * DONE (99) - Wizard has approved the proposed action, and is signalling
   *             the user object code that it is okay to proceed with the
   *             action without further consulting this wizard.
   * </pre>
   */

  //  int state;  We don't want to shadow the state variable from GanymediatorWizard

  /**
   * The actual user object that this wizard is acting on.
   */

  userCustom userObject;

  /**
   * The Integer index of the group entry that we are being asked to
   * help delete.  We keep track of this so that when the user finishes
   * the interaction sequence we know which element to go ahead and
   * remove.
   */

  int index;

  /* -- */

  /**
   *
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The user object that this wizard will work with.
   * @param param An Integer object specifying the index of the GROUPLIST
   * field that the user wishes to delete.
   *
   */

  public userHomeGroupDelWizard(GanymedeSession session, 
				userCustom userObject, 
				Object param) throws RemoteException
  {
    super(session);		// register ourselves

    this.session = session;
    this.userObject = userObject;

    if (!(param instanceof Integer))
      {
	throw new IllegalArgumentException("Error, expecting an Integer array index");
      }

    this.index = ((Integer) param).intValue();
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
	dialog = new JDialogBuff("Home Group Removal Cancelled",
				 "Home Group Removal Cancelled",
				 "OK",
				 null,
				 "ok.gif");
	retVal.setDialog(dialog);
	
	this.unregister(); // we're stopping here, so we'll unregister ourselves
	
	return retVal;
      }

    if (state == 1)
      {
	System.err.println("userHomeGroupDelWizard.respond(): state == 1");

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Home Group Change",
				 "What group do you want to set as the new default for this user?",
				 "OK",
				 "Cancel",
				 "question.gif");

	// get the list of choices, synthesize a list that contains every choice but
	// the one being deleted

	userObject.updateGroupChoiceList();

	QueryResult groupChoice = userObject.groupChoices;

	// Which group is being deleted?

	Invid val = (Invid) userObject.getFieldValuesLocal(GROUPLIST).elementAt(index);

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

	String group = (String) returnHash.get("New Home Group");

	// get the list of groups

	userObject.updateGroupChoiceList();

	QueryResult groupChoice = userObject.groupChoices;

	// find the group we're changing to, find the id, change it

	boolean found = false;
	session.checkpoint("homegroupdel" + userObject.getLabel());

	for (int i = 0; i < groupChoice.size(); i++)
	  {
	    if (groupChoice.getLabel(i).equals(group))
	      {
		found = true;

		// right now, this might fail if the user is in a group that the
		// current admin doesn't have permission to edit.  Unlinking for
		// groups is free (see groupCustom), but adding, even adding to
		// the home users field for users that are already in the superset
		// users list is not automatically permitted.  This may change.

		retVal = userObject.setFieldValue(HOMEGROUP, groupChoice.getInvid(i));
		break;
	      }
	  }

	if (!found)
	  {
	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("Home Group Removal Cancelled",
				     "Home Group Removal Cancelled\n\nError, couldn't deal with the group selected",
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

	    InvidDBField invF = (InvidDBField) userObject.getField(GROUPLIST);

	    // note that this deleteElement() operation will pass
	    // through userObject.wizardHook().  wizardHook will see that we are
	    // an active userHomeGroupDelWizard, and are at state DONE, so it
	    // will go ahead and unregister us and let the GROUPLIST modification
	    // go through to completion.

	    retVal = invF.deleteElement(index);

	    if (retVal == null || retVal.didSucceed())
	      {
		retVal = new ReturnVal(true);
		dialog = new JDialogBuff("Home Group Change Performed",
					 "The user's old home group has been successfully removed, and a new default set.",
					 "OK",
					 null,
					 "ok.gif");

		retVal.addRescanField(HOMEGROUP);
		retVal.setDialog(dialog);
	      }
	    else
	      {
		if (!session.rollback("homegroupdel" + userObject.getLabel()))
		  {
		    retVal = Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
							"Ran into a problem during home group deletion, and rollback failed");
		  }

		this.unregister(); // we're stopping here, so we'll unregister ourselves
	      }
	  }
	else if (retVal.getDialog() == null)
	  {
	    // argh, failure..

	    if (!session.rollback("homegroupdel" + userObject.getLabel()))
	      {
		retVal = Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
						    "Ran into a problem during home group change, and rollback failed");
	      }

	    this.unregister(); // we're stopping here, so we'll unregister ourselves
	  }

	return retVal;
      }

    // are we in an unexpected state?
	
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
    ReturnVal retVal;

    /* -- */

    System.err.println("userHomeGroupDelWizard: creating home group deletion wizard");
    
    userObject.updateGroupChoiceList();
    
    if (userObject.groupChoices.size() == 1)
      {
	buffer.append("Can't delete lone group for user ");
	buffer.append(userObject.getLabel());
	buffer.append("\n\nYou may not delete the last group from a user's account.  All active users in UNIX need ");
	buffer.append("to be a member of at least a single account group.");

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("User Home Group Change Dialog",
				 buffer.toString(),
				 "OK",
				 null,
				 "error.gif");

	retVal.setDialog(dialog);

	return retVal;
      }

    buffer.append("Changing home group for user ");
    buffer.append(userObject.getLabel());
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
