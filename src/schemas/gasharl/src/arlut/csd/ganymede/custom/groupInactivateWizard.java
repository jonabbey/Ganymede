/* 
   groupInactivateWizard.java

   A wizard to facilitate the inactivation of a group.

   Created: 23 June 1998
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2003/03/12 03:48:40 $
   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

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
                                                           groupInactivateWizard

------------------------------------------------------------------------------*/
/**
 * <p>This class handles the inactivation of a group.  If any users have
 * this group as the home group, then those users will need a new
 * group designated as the home group.  The dialog will prompt with a
 * choice list containing the groups that the user is a member of.  If
 * the user doesn't have any other groups, the wizard will cancel the
 * inactivation and provide a list of the users who need new groups.</p>
 *
 * <p>If the group doesn't have any home users, then a simple
 * confirmation dialog will be presented.</p>
 *
 * States:
 * <table>
 * <tr><td>Start</td><td>Explanation.  If group has no home users, then the dialog is just a confirmation, and next state is 50.</td></tr>
 * <tr><td>1</td><td>List of home users, along with choice of new group.  If a user has no other groups, this will instead be a list of those users and a cancel message.</td></tr>
 * <tr><td>2</td><td>Confirmation of inactivation..</td></tr>
 * <tr><td>50</td><td>Confirmaion of inactivation.</td></tr>
 * </table>
 */

public class groupInactivateWizard extends GanymediatorWizard {

  private final static boolean debug = true;

  // ---

  /**
   * The user-level session context that this wizard is acting in.  This
   * object is used to handle necessary checkpoint/rollback activity by
   * this wizard, as well as to handle any necessary label lookups.
   */

  GanymedeSession session;

  /**
   * The actual group object that this wizard is acting on.
   */

  groupCustom groupObject;

  /**
   * Hash of user names to user objects.  This is used to keep track
   * of the open user objects, which will be passed to the
   * groupCustom. They will be passed in another hash, however.
   */

  Hashtable
    userObjectHash = new Hashtable();

  /**
   * Hash of group names to group object invid's.  This is used to process
   * the string choices we get back if the user requested users' home
   * groups be changed.
   */

  Hashtable
    groupNameHash = new Hashtable();

  /**
   * Vector field of all the home users for this group.  If there are no
   * home users, getStartDialog chooses a different path for the dialog.
   */
  InvidDBField 
    homeField;

  // From superclass.
  // int state;

  /**
   *
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The group object that this wizard will work with.
   *
   */

  public groupInactivateWizard(GanymedeSession session,
			       groupCustom group) throws RemoteException
  {
    super(session);
    
    this.session = session;
    this.groupObject = group;
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
    return fail("Group Inactivation Canceled",
		"Group Inactivation Canceled",
		"OK", null, "ok.gif");
  }

  /**
   *
   * This method starts off the wizard process
   *
   * If the home users field is empty, then we don't have to worry
   * about cleaning up any of the users.  So we just ask the user if
   * they really want to inactivate, and set the state to 50 so the
   * next dialog will just be a confirmation if everything worked.
   *
   * Otherwise, follow the normal 1,2 state procession.
   */

  public ReturnVal processDialog0()
  {
    homeField = (InvidDBField) groupObject.getField(groupSchema.HOMEUSERS);

    StringBuffer buffer = new StringBuffer();

    if (homeField.size() == 0)
      {
	if (debug)
	  {
	    System.err.println("groupInactivateWizard: there are no home users.");
	  }
	buffer.append("Inactivating " + groupObject.getLabel());
	buffer.append("\n\nAre you sure you want to inactivate " + groupObject.getLabel() + "?");

	setNextState(50);

	return continueOn("Group Inactivate Dialog", 
			  buffer.toString(),
			  "Inactivate",
			  "Cancel",
			  "question.gif");
	

      }
    else
      {

	if (debug)
	  {
	    System.err.println("groupInactivateWizard: creating inactivation wizard, there are " + 
			       homeField.size() + " home users.");
	  }
	
	buffer.append("Inactivating ");
	buffer.append(groupObject.getLabel());
	
	buffer.append("\n\nThis group will be rendered unusable, but will be kept ");
	buffer.append("in the database for 3 months to preserve accounting information.\n\n");
	buffer.append("If any users have this group as their home group, you will ");
	buffer.append("have to provide a new home group for them.");
	
	return continueOn("Group Inactivate Dialog",
			  buffer.toString(),
			  "Ok",
			  "Cancel",
			  "question.gif");
      }
  }

  /**
   *
   * Process first result from the client.. if we get to this point, the
   * user said ok, go ahead.. we don't have any parameters to deal with
   * at this point.
   *
   */
  
  public ReturnVal processDialog1() throws NotLoggedInException
  {
    ReturnVal retVal;

    retVal = continueOn("Group Inactivation Wizard",
			"Choose a new home group for each user.  The choices " +
			"are the groups the user is currently a memeber of.  " +
			"If the user isn't in any other groups, then I guess " +
			"I will give you something else.",
			"Ok",
			"Cancel",
			"question.gif");

    // First get a list of the home users

    QueryResult qr = homeField.encodedValues();

    Vector homeUsers = qr.getHandles();

    if (debug)
      {
	System.err.println("groupInactivateWizard: " + homeUsers.size() + 
			   " home users.");
      }
    
    // Loop through the home users, and find out what other groups
    // they are in.  What if they are in no groups?  Must have a
    // button or something.
    
    Vector rejectedUsers = new Vector();
    
    for (int i = 0; i < homeUsers.size(); i++)
      {
	ObjectHandle lh = (ObjectHandle) homeUsers.elementAt(i);
	DBObject user;

	if (debug)
	  {
	    System.err.println("groupInactivateWizard: dealing with user: " + lh.getLabel());
	  }

	ReturnVal rv = session.edit_db_object(lh.getInvid());
	
	if ((rv != null) && (!rv.didSucceed()))
	  {
	    if (debug)
	      {
		System.err.println("groupInactivateWizard: edit_db_object failed, aborting");
	      }

	    groupObject.inactivate(false, true); // not sucessful, from wizard
	    return rv;
	  }
	
	user = (DBObject) rv.getObject();

	// Keep a reference to the user object.

	userObjectHash.put(lh.getLabel(), user);

	InvidDBField userGroupField = (InvidDBField) user.getField(userSchema.GROUPLIST);
	QueryResult queryr = userGroupField.encodedValues();

	// The list of groups will contain the current group,
	// which is being inactivated.  We need to remove that
	// group from the vector.

	Vector handles = queryr.getHandles();
	Invid invid = groupObject.getInvid();

	for (int j = 0; j < handles.size(); j++)
	  {
	    if (queryr.getInvid(j).equals(invid))
	      {
		handles.removeElementAt(j);
		break;
	      }
	    else
	      {
		groupNameHash.put(queryr.getLabel(j), queryr.getInvid(j));
	      }
	  }

	if (debug)
	  {
	    System.err.println("groupInactivateWizard: " + lh.getLabel() + 
			       " has " + handles.size() + 
			       " groups left, besides the home group.");
	  }

	// If there aren't any groups left, get the hell out of
	// here.  The user is not ready for this operation.

	if (handles.size() < 1)
	  {
	    rejectedUsers.addElement(lh.getLabel());
	  }
		
	retVal.getDialog().addChoice(lh.getLabel(), queryr.getLabels());
      }

    // were there users who had no other groups to belong to?  If so,
    // we'll need to refuse the group inactivation.

    if (rejectedUsers.size() != 0)
      {
	StringBuffer rejectedBuffer = new StringBuffer();

	rejectedBuffer.append("The following users don't have any other groups:\n\n");

	for (int i = 0; i < rejectedUsers.size(); i++)
	  {
	    rejectedBuffer.append(rejectedUsers.elementAt(i));
	    rejectedBuffer.append("\n");
	  }

	rejectedBuffer.append("\nYou must modify or inactivate these users before ");
	rejectedBuffer.append("inactivating this group.");

	groupObject.inactivate(false, true); // Not sucessful, from wizard

	return fail("Group Inactivate Failed",
		    rejectedBuffer.toString(),
		    "Ok", null, "ok.gif");
      }

    if (debug)
      {
	System.err.println("groupInactivateWizard: returning dialog with all the choices, state now = 2");
      }

    return retVal;
  }

  /**
   * If we are called here, the user responded to a dialog asking them
   * to select new home groups for users who had this group chosen as
   * their home group.
   * 
   */

  public ReturnVal processDialog2() throws NotLoggedInException
  {
    ReturnVal finalReturnVal = new ReturnVal(true);

    if (debug)
      {
	System.err.println("groupInactivateWizard: state = 2");
      }
    
    Enumeration keys = getKeys();

    while (keys.hasMoreElements())
      {
	String userName = (String) keys.nextElement();
	String newHomeGroupName = (String) getParam(userName);
	Invid newHomeGroup = (Invid) groupNameHash.get(newHomeGroupName);

	if (debug)
	  {
	    System.err.println("groupInactivateWizard: fixing up " + userName);
	  }
	    
	DBObject usr = (DBObject) userObjectHash.get(userName);

	if (debug)
	  {
	    System.err.println("Setting home group for " + userName + " to " + newHomeGroup);
	  }

	InvidDBField ugField = (InvidDBField) usr.getField(userSchema.HOMEGROUP);
	ReturnVal retv = ugField.setValue(newHomeGroup);

	if ((retv != null) && (! retv.didSucceed()))
	  {
	    groupObject.inactivate(false, true);
	    return retv;
	  }
	else
	  {
	    finalReturnVal.unionRescan(retv);
	  }
	
      }

    groupObject.inactivate(true, true);

    if (debug)
      {
	System.err.println("groupInactivateWizard: all done.");
      }
    
    ReturnVal result = success("Group Inactivation performed",
			       "The Group has been inactivated, and all " +
			       "the users have new home groups.",
			       "OK", null, "ok.gif").unionRescan(finalReturnVal);

    System.err.println(result.dumpRescanInfo());

    return result;
  }

  /**
   * This is the state where there are no home users, and the first
   * "are you sure?" dialog has been shown.  From here, we just inactivate
   * the group and go about our merry ways.
   */
  public ReturnVal processDialog50() throws NotLoggedInException
  {
    groupObject.inactivate(true, true);
    if (debug)
      {
	System.err.println("groupInactivateWizard: all done, no home groups.");
      }

    return success("Group Inactivation performed",
		   "The group has been inactivated.", "OK", null, "ok.gif");
  }


}
