/*

   userHomeGroupDelWizard.java

   A wizard to handle the wizard interactions required when a user attempts
   to delete the group that they have selected for their default group.
   
   Created: 29 January 1998
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 1999/07/14 21:51:48 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
   * This method provides a default response if a user
   * hits cancel on a wizard dialog.  This should be
   * subclassed if a wizard wants to provide a more
   * detailed cancel response.
   *
   */

  public ReturnVal cancel()
  {
    return fail("Home Group Removal Canceled",
		"Home Group Removal Canceled",
		"OK",
		null,
		"ok.gif");
  }


  /**
   *
   * This method starts off the wizard process
   *
   */

  public ReturnVal processDialog0()
  {
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

	return fail("User Home Group Change Dialog",
		    buffer.toString(),
		    "OK",
		    null,
		    "error.gif");
      }

    buffer.append("Changing home group for user ");
    buffer.append(userObject.getLabel());
    buffer.append("\n\nThe group you are attempting to remove this user from is the user's default group. ");
    buffer.append("In order to remove the user from this group, you are going to need to select another group ");
    buffer.append("to be the default group for this user at login time.");

    return continueOn("User Home Group Change Dialog",
		      buffer.toString(),
		      "Next",
		      "Cancel",
		      "question.gif");
  }

  public ReturnVal processDialog1()
  {
    ReturnVal retVal = null;

    /* -- */

    System.err.println("userHomeGroupDelWizard.respond(): state == 1");

    retVal = continueOn("Home Group Change",
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
    
    retVal.getDialog().addChoice("New Home Group", 
				 choices,
				 (String) choices.elementAt(0));

    System.err.println("userHomeGroupDelWizard.respond(): state == 2, returning dialog");

    return retVal;
  }

  /**
   *
   * This method will be called if the client progressed from the second
   * dialog.<br><br>
   *
   * <pre>
   * Keys:
   *
   * "New Home Group"
   * </pre>
   *
   */

  public ReturnVal processDialog2()
  {
    ReturnVal retVal = null;
    String checkPointKey = "homegroupdel" + userObject.getLabel();

    /* -- */

    System.err.println("userHomeGroupDelWizard.respond(): state == 2");

    String group = (String) getParam("New Home Group");

    // get the list of groups

    userObject.updateGroupChoiceList();

    QueryResult groupChoice = userObject.groupChoices;

    // find the group we're changing to, find the id, change it
    
    boolean found = false;

    // we're going to check point here, so that we can undo things if
    // we can't complete all parts of this operation.  This is needed
    // because we are doing two separate operations together.. the
    // InvidDBField logic does its own checkpointing, but we need
    // one that includes the two operations <<ensemble>>.

    session.checkpoint(checkPointKey);
    
    for (int i = 0; i < groupChoice.size(); i++)
      {
	if (groupChoice.getLabel(i).equals(group))
	  {
	    found = true;

	    // right now, this might fail if the user is in a group
	    // that the current admin doesn't have permission to edit.
	    // Unlinking for groups is free (see groupCustom), but
	    // adding, even adding to the home users field for users
	    // that are already in the superset users list is not
	    // automatically permitted.  This may change.

	    retVal = userObject.setFieldValue(HOMEGROUP, groupChoice.getInvid(i));
	    break;
	  }
      }

    if (!found)
      {
	return fail("Home Group Removal Cancelled",
		    "Home Group Removal Cancelled\n\nError, couldn't deal with the group selected",
		    "OK",
		    null,
		    "ok.gif");
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
	    retVal = success("Home Group Change Performed",
			     "The user's old home group has been successfully removed, and a new default set.",
			     "OK",
			     null,
			     "ok.gif");

	    retVal.addRescanField(userObject.getInvid(), HOMEGROUP);

	    // we succeeded, so pop off our checkpoint

	    session.getSession().popCheckpoint(checkPointKey);
	    
	    return retVal;
	  }
	else
	  {
	    // try to undo everything.. if we can, we'll go ahead
	    // and return the failure report from invF.deleteElement()

	    if (!session.rollback(checkPointKey))
	      {
		retVal = Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
						    "Ran into a problem during home group deletion, and rollback failed");
	      }

	    return retVal;
	  }
      }
    else if (retVal.getDialog() == null)
      {
	// argh, failure with no explanation..

	if (!session.rollback(checkPointKey))
	  {
	    retVal = Ganymede.createErrorDialog("userHomeGroupDelWizard: Error",
						"Ran into a problem during home group change, and rollback failed");
	  }

	return retVal;
      }
    else
      {
	return retVal;
      }
  }

}
