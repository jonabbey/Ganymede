/*

   userReactivateWizard.java

   A wizard to manage user reactivation interactions for the userCustom object.

   Created: 29 January 1998
   Release: $Name:  $
   Version: $Revision: 1.8 $
   Last Mod Date: $Date: 1999/07/14 21:51:58 $
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
                                                            userReactivateWizard

------------------------------------------------------------------------------*/
/**
 *
 * A wizard to handle the wizard interactions required when a user reactivates
 * a user account.
 *
 * <br>This wizard, unlike the userHomeGroupDelWizard and userRenameWizard, does
 * not directly manipulate fields in the user object.  Instead, it works with
 * methods written for its benefit in userCustom.<br>
 *
 * <br>Object Reactivation basically consists of rendering an object fit for use
 * once again, by clearing the removal date, and fixing up any fields that need
 * fixing.<br>
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator 
 * @see arlut.csd.ganymede.custom.userCustom
 * @see arlut.csd.ganymede.custom.userSchema
 */

public class userReactivateWizard extends GanymediatorWizard implements userSchema {

  static final boolean debug = false;

  // ---

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
   *             for the user to provide the password, shell, and forwarding
   *             addresses needed to reactivate this account.
   * DONE (99) - Wizard has approved the proposed action, and is signalling
   *             the user object code that it is okay to proceed with the
   *             action without further consulting this wizard.
   * </pre>
   */

  //  int state; from superclass

  /**
   * The actual user object that this wizard is acting on.
   */

  userCustom userObject;

  // reactivation params

  // These variables are directly accessed by userCustom when this
  // wizard calls back to the userObject to perform the reactivation
  // logic.

  String password;
  String shell;

  /**
   *
   * Constructor
   *
   */

  public userReactivateWizard(GanymedeSession session, 
			      userCustom userObject) throws RemoteException
  {
    super(session);		// register ourselves

    this.session = session;
    this.userObject = userObject;
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
    return fail("User Reactivation Canceled",
		"User Reactivation Canceled",
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

    /* -- */

    System.err.println("userReactivateWizard: creating reactivation wizard");

    buffer.append("Reactivating ");
    buffer.append(userObject.getLabel());
    buffer.append("\n\nIn order to reactivate this account, you need to provide a password, ");
    buffer.append("a login shell, and a new address to send email for this account to.");
	
    return continueOn("User Reactivation Dialog",
		      buffer.toString(),
		      "Next",
		      "Cancel",
		      "question.gif");
  }

  /**
   * 
   * This method is called without any parameters from the user's
   * dialog
   * 
   */

  public ReturnVal processDialog1()
  {
    ReturnVal retVal;

    /* -- */

    System.err.println("userReactivateWizard.respond(): state == 1");

    retVal = continueOn("Reactivate User",
			"",
			"OK",
			"Cancel",
			"question.gif");
    
    retVal.getDialog().addPassword("New Password", true);

    StringDBField stringfield = (StringDBField) userObject.getField(LOGINSHELL);

    userObject.updateShellChoiceList();
    retVal.getDialog().addChoice("Shell", 
				 userObject.shellChoices.getLabels(),
				 (String) stringfield.getValueLocal());

    System.err.println("userReactivateWizard.respond(): state == 1, returning dialog");
    
    return retVal;
  }

  /**
   * 
   * This method is called parameters from the client under key "New Password"
   * and "Shell".
   * 
   */

  public ReturnVal processDialog2()
  {
    ReturnVal retVal = null;

    if (debug)
      {
	System.err.println("userReactivateWizard.respond(): state == 2");

	Enumeration enum = returnHash.keys();
	int i = 0;

	while (enum.hasMoreElements())
	  {
	    Object key = enum.nextElement();
	    Object value = returnHash.get(key);
		
	    System.err.println("Item: (" + i++ + ") = " + key + ":" + value);
	  }
      } 

    shell = (String) getParam("Shell");
    password = (String) getParam("New Password");

    // and do the inactivation.. userObject will consult us for
    // shell and password
	    
    retVal = userObject.reactivate(this);
    
    if (retVal == null)
      {
	return success("User Reactivation Performed",
		       "User has been reactivated",
		       "OK",
		       null,
		       "ok.gif");
      }
    else if (!retVal.didSucceed())
      {
	// failure.. need to do the rollback that would have
	// originally been done for us if we hadn't gone through the
	// wizard process

	if (!session.rollback("reactivate" + userObject.getLabel()))
	  {
	    return Ganymede.createErrorDialog("userReactivateWizard: Error",
					      "Ran into a problem during user reactivation, and rollback failed");
	  }
      }

    return retVal;
  }
}
