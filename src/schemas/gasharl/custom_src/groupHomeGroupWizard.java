/*

  groupHomeGroupWizard.java

  A wizard to allow deletion of a user's home group from the group edit window.

  Created: 8 April 1998
  Version: $Revision: 1.3 $ %D%
  Module by: Mike Mulvaney
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
                                                            groupHomeGroupWizard

------------------------------------------------------------------------------*/

public class groupHomeGroupWizard extends GanymediatorWizard implements groupSchema {

  final static int ERROR = -27;

  final boolean debug = true;

  Invid
    userInvid;

  groupCustom
    groupObject;

  GanymedeSession
    session;

  userCustom user = null;

  /* -- */

  /**
   * State
   *
   * 1 - Wizard has shown the first informational dialog.
   * 2 - Wizard is waiting for the user to choose another group.
   * DONE - Wizard is done.
   */

  public groupHomeGroupWizard(GanymedeSession session,
			      groupCustom groupObject, 
			      Invid userInvid) throws RemoteException
  {
    super(session);

    this.userInvid = userInvid;
    this.groupObject = groupObject;
    this.session = session;

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
    return fail("User removal Canceled",
		"User removal Canceled",
		"OK",
		null,
		"ok.gif");
  }

  public ReturnVal processDialog1()
  {
    ReturnVal retVal = null;

    /* -- */

    // We have already shown the first info dialog, so now it is time to show
    // the dialog with the choices

    if (debug)
      {
	System.err.println("groupHomeGroupWizard.respond(): state == 1");
      }
    
    retVal = continueOn("Change Home Group", "",
			"OK", "Cancel", "question.gif");
    
    try
      {
	print("Getting values.");
	
	QueryResult values = ((invid_field)user.getField(userSchema.GROUPLIST)).encodedValues();
	
	print("Adding choices to dialog.");
	
	Vector labels = values.getLabels();
	String currentGroup = groupObject.getLabel();
	
	for (int i = 0; i < labels.size(); i++)
	  {
	    if (currentGroup.equals((String)labels.elementAt(i)))
	      {
		labels.removeElementAt(i);
		break;
	      }
	  }
	
	retVal.getDialog().addChoice("Home Group:" , labels, null);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get the groups.");
      }
    
    if (debug)
      {
	System.err.println("groupHomeGroupWizard.respond(): state == 1, returning dialog");
      }

    return retVal;
  }

  public ReturnVal processDialog2()
  {
    ReturnVal retVal = null;

    db_field userHomeGroupField;
    Invid newGroup;

    /* -- */

    if (debug)
      {
	System.err.println("groupHomeGroupWizard.respond: state == 2");
	
	Enumeration enum = getKeys();
	int i = 0;

	while (enum.hasMoreElements())
	  {
	    Object key = enum.nextElement();
	    Object value = getParam(key);
	    System.err.println("Item: (" + i++ + ") " + key + ":" + value);
	  } 
      }
	
    String gString = (String) getParam("Home Group:");

    // Now we have to do a query to find which group has this name.
    
    QueryDataNode node = new QueryDataNode(QueryDataNode.EQUALS, gString);
    Query query = new Query(groupObject.getTypeID(), node);
    QueryResult qr = session.query(query);
    
    if (qr.size() == 1)
      {
	// this is what we want.
	newGroup = qr.getInvid(0);

	userHomeGroupField = user.getField(userSchema.HOMEGROUP);

	try
	  {
	    retVal = userHomeGroupField.setValue(newGroup);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set the value: " + rx);
	  }
	    
	if ((retVal == null) || (retVal.didSucceed()))
	  {
	    return success("Home group changed",
			   "User's home group successfully changed.",
			   "OK", null, "ok.gif");
	  }
	else if (retVal.getDialog() == null)
	  {
	    // we failed :(
	    retVal = Ganymede.createErrorDialog("groupHomeGroupWizard: error",
						"Ran into trouble during user interaction.");
	  }
      }
    else // could not find the group
      {
	retVal = Ganymede.createErrorDialog("groupHomeGroupWizard: error",
					    "Could not find the group you wanted, Sorry.");

      }

    return retVal;
  }

  public ReturnVal getStartDialog()
  {
    JDialogBuff dialog;
    ReturnVal retVal = null;
    
    user = (userCustom) (session.edit_db_object(userInvid).getObject());

    if (user == null)
      {
	return Ganymede.createErrorDialog("groupHomeGroupWizard error",
					  "Could not get the user.");
      }

    if (debug)
      {
	System.err.println("groupHomeGroupWizard: creating start dialog.");
      }

    int size = 0;

    try
      {
	size = user.getField(userSchema.GROUPLIST).size();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("could not get the size. " + rx);
      }

    // If size is less than one, then there won't be any other groups to change to.

    if (size > 1)  
      {
	return continueOn("Home Group Change", 
			  "In order to remove a user's home group, you must choose another home group for that user.",
			  "Next", "Cancel", "question.gif");
	
      }
    else
      {
	// no groups to choose from

	return fail("Home Group Change",
		    "Each user must have a home group.  For that user, this is it.  So don't.",
		    "Sorry", null, "ok.gif");
      }
  }

  private void print(String s)
  {
    System.err.println("groupHomeGroupWizard: " + s);
  }
}
