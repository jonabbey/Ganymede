/* 
   groupInactivateWizard.java

   A wizard to facilitate the inactivation of a group.

   Created: 23 June 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Mike Mulvaney
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
                                                           groupInactivateWizard

------------------------------------------------------------------------------*/

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
	if (debug)
	  {
	    System.err.println("Group inactivate cancelled.");
	  }

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Group Inactivation Canceled",
				 "Group Inactivation Canceled",
				 "OK", null, "ok.gif");

	retVal.setDialog(dialog);

	this.unregister();  // We're done

	return retVal;
      }

    // State of 1 means that the start dialog was shown, and now we
    // need to give them a list of users.

    if (state == 1)
      {
	if (debug)
	  {
	    System.err.println("groupInactivateWizard: state == 1");
	  }

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Group Inactivation Wizard",
				 "Choose a new home group for each user.  The choices " +
				 "are the groups the user is currently a memeber of.  " +
				 "If the user isn't in any other groups, then I guess " +
				 "I will give you something else.",
				 "Ok",
				 "Cancel",
				 "question.gif");
	retVal.setDialog(dialog);

	// First get a list of the home users

	InvidDBField homeField = (InvidDBField) groupObject.getField(groupSchema.HOMEUSERS);
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

		this.unregister();
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
		
	    dialog.addChoice(lh.getLabel(), queryr.getLabels());
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

	    retVal = new ReturnVal(false);
	    dialog = new JDialogBuff("Group Inactivate Failed",
				     rejectedBuffer.toString(),
				     "Ok", null, "ok.gif");
	    
	    retVal.setDialog(dialog);
	    
	    this.unregister();
	    groupObject.inactivate(false, true); // Not sucessful, from wizard
	    return retVal;
	  }

	if (debug)
	  {
	    System.err.println("groupInactivateWizard: returning dialog with all the choicse, state now = 2");
	  }

	state = 2;

	retVal.setDialog(dialog);
	retVal.setCallback(this);
	return retVal;

      }
    // State of 2 means that the user has chosen groups for everyone,
    // so now we need to do the deed and get out
    else if (state == 2)
      {
	if (debug)
	  {
	    System.err.println("groupInactivateWizard: state = 2");
	  }

	Enumeration keys = returnHash.keys();

	while (keys.hasMoreElements())
	  {
	    String userName = (String) keys.nextElement();
	    String newHomeGroupName = (String) returnHash.get(userName);
	    Invid newHomeGroup = (Invid) groupNameHash.get(newHomeGroupName);

	    if (debug)
	      {
		System.err.println("groupInactivateWizard: fixing up " + userName);
	      }
	    
	    DBObject usr = (DBObject)userObjectHash.get(userName);

	    if (debug)
	      {
		System.err.println("Setting home group for " + userName + " to " + newHomeGroup);
	      }

	    InvidDBField ugField = (InvidDBField) usr.getField(userSchema.GROUPLIST);
	    ReturnVal retv = ugField.setValue(newHomeGroup);
	    if ((retv != null) && (! retv.didSucceed()))
	      {
		this.unregister();
		groupObject.inactivate(false, true);
		return retv;
	      }
	  }

	retVal = new ReturnVal(true);
	retVal.setDialog(new JDialogBuff("Group Inactivation performed",
					 "The Group has been inactivated, and all " +
					 "the users have new home groups.",
					 "OK", null, "ok.gif"));

	if (debug)
	  {
	    System.err.println("groupInactivateWizard: all done.");
	  }

	groupObject.inactivate(true, true);
	this.unregister();
	return retVal;
      }
    
    // If we get to here, we are lost.  There should have been a
    // return at the end of one of the state branches.

    return Ganymede.createErrorDialog("groupInactivateWizard: Error", 
				      "Whoa, state = " + state + ", I am lost big-time.");
    
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

    if (debug)
      {
	System.err.println("groupInactivateWizard: creating inactivation wizard");
      }

    buffer.append("Inactivating ");
    buffer.append(groupObject.getLabel());

    buffer.append("\n\nThis group will be rendered unusable, but will be kept ");
    buffer.append("in the database for 3 months to preserve accounting information.\n\n");
    buffer.append("If any users have this group as their home group, you will ");
    buffer.append("have to provide a new home group for them.");

    retVal = new ReturnVal(false);
    dialog = new JDialogBuff("Group Inactivate Dialog",
			     buffer.toString(),
			     "Ok",
			     "Cancel",
			     "question.gif");

    retVal.setDialog(dialog);
    retVal.setCallback(this);
    
    state = 1;

    return retVal;
  }
}
