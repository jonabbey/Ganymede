/*

  groupHomeGroupWizard.java

  A wizard to allow deletion of a user's home group from the group edit window.

  Created: 8 April 1998
  Version: $Revision: 1.6 $ %D%
  Module by: Mike Mulvaney

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

  /**
   * State
   *
   * 1 - Wizard has shown the first informational dialog.
   * 2 - Wizard is waiting for the user to choose another group.
   * DONE - Wizard is done.
   */
  public groupHomeGroupWizard(GanymedeSession session, groupCustom groupObject, Invid userInvid) throws RemoteException
  {
    super(session);

    this.userInvid = userInvid;
    this.groupObject = groupObject;
    this.session = session;

  }

  public ReturnVal respond(Hashtable returnHash)
  {
    JDialogBuff dialog;
    ReturnVal retVal = null;

    /*  -- Here we go --  */
    if ((returnHash == null) || (state == ERROR))
      {
	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("User removal cancelled",
				 "User removal cancelled.",
				 "OK", null, "ok.gif");

	this.unregister();

	retVal.setDialog(dialog);

	return retVal;
      }
    
    if (state == 1)
      {
	// We have already shown the first info dialog, so now it is time to show
	// the dialog with the choices

	if (debug)
	  {
	    System.err.println("groupHomeGroupWizard.respond(): state == 1");
	  }

	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Change Home Group", "",
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
	    dialog.addChoice("Home Group:" , labels, null);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get the groups.");
	  }

	retVal.setDialog(dialog);
	retVal.setCallback(this);

	state = 2;
	
	if (debug)
	  {
	    System.err.println("groupHomeGroupWizard.respond(): state ==1, returning dialog");
	  }

	return retVal;
      }
    else if (state == 2)
      {
	db_field userHomeGroupField;
	Invid newGroup;

	/* -- */

	if (debug)
	  {
	    System.err.println("groupHomeGroupWizard.respond: state == 2");
	    
	    Enumeration enum = returnHash.keys();
	    int i = 0;

	    while (enum.hasMoreElements())
	      {
		Object key = enum.nextElement();
		Object value = returnHash.get(key);
		System.err.println("Item: (" + i++ + ") " + key + ":" + value);
	      } 
	  }
	
	String gString = (String)returnHash.get("Home Group:");

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
		retVal = new ReturnVal(true);
		dialog = new JDialogBuff("Home group changed",
					 "User's home group successfully changed.",
					 "OK", null, "ok.gif");
		retVal.setDialog(dialog);
	      }
	    else if (retVal.getDialog() == null)
	      {
		// we failed :(
		retVal = Ganymede.createErrorDialog("groupHomeGroupWizard: error",
						    "Ran into trouble during user interacion.");
	      }
	    else
	      {
		return retVal;
	      }

	  }
	else // could not find the group
	  {
	    retVal = Ganymede.createErrorDialog("groupHomeGroupWizard: error",
						"Could not find the group you wanted, Sorry.");

	  }	    
	this.unregister();
	return retVal;
	
      }
    else if (state == DONE)
      {
	System.err.println("groupHomeGroupWizard.respond: got to the DONE part.  That shouldn't happen.");
      }
    
    return Ganymede.createErrorDialog("groupHomeGroupWizard: Error", "No idea what is going on, I got lost somewhere.");
  }

  public ReturnVal getStartDialog()
  {
    JDialogBuff dialog;
    ReturnVal retVal = null;
    
    user = (userCustom) (session.edit_db_object(userInvid)).getObject();

    if (user == null)
      {
	System.err.println("Could not get the user.");
	state = ERROR;
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
	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Home Group Change", 
				 "In order to remove a user's home group, you must choose another home group for that user.",
				 "Next", "Cancel", "question.gif");
	
	retVal.setDialog(dialog);
	retVal.setCallback(this);
	
	state = 1;

	return retVal;
      }
    else
      {
	// no groups to choose from
	retVal = new ReturnVal(false);
	dialog = new JDialogBuff("Home Group Change",
				 "Each user must have a home group.  For that user, this is it.  So don't.",
				 "Sorry", null, "ok.gif");

	retVal.setDialog(dialog);
	retVal.setCallback(this);
	
	state = ERROR;
	return retVal;
      }
  }

  private void print(String s)
  {
    System.err.println("groupHomeGroupWizard: " + s);
  }
}
