/*

   userCategoryWizard.java

   A wizard to manage changes in the user object's user category
   field.
   
   Created: 13 October 1998
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/07/14 21:51:51 $
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
                                                              userCategoryWizard

------------------------------------------------------------------------------*/

/**
 * A wizard to handle the wizard interactions required when a user's category is
 * changed.  This wizard takes care of asking for everything required to
 * set and/or change a user category.
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator 
 */

public class userCategoryWizard extends GanymediatorWizard {

  final static boolean debug = true;

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
   * DONE (99) - Wizard has approved the proposed action, and is signalling
   *             the user object code that it is okay to proceed with the
   *             action without further consulting this wizard.
   * </pre>
   */

  //  int state; from superclass.. we don't want to shadow it here

  /**
   * The actual user object that this wizard is acting on
   */

  userCustom userObject;

  /**
   * The username field in the user object that we may change
   */

  DBField field;

  /**
   * The invid for the new category for the user
   */

  Invid newCatInvid;

  /**
   * The proposed new category for the user
   */

  DBObject newCategory;

  /**
   * The invid for the user's old category
   */

  Invid oldCatInvid;

  /**
   * The old category for the user
   */

  DBObject oldCategory;

  /**
   * Do we need to get an explanation from the user about this
   * change?
   */

  boolean notificationRequired = false;

  /**
   * List of email addresses to send the change notification to.
   */

  Vector notifyList = null;

  /**
   * Do we need to set an expiration date?
   */

  boolean needExpireDate = false;

  /**
   * Date that the user will be expired.
   */

  Date expirationDate = null;

  /**
   * Latest date that the user's expiration may be set to.
   */

  Date terminationDate = null;

  /**
   * The user's explanation of the category change.
   */

  String justification = null;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  /**
   *
   * This constructor registers the wizard as an active wizard
   * on the provided session.
   *
   * @param session The GanymedeSession object that this wizard will
   * use to interact with the Ganymede data store.
   * @param userObject The user object that this wizard will work with.
   * @param oldInvid The old category's Invid, if any
   * @param newInvid The new category's invid
   *
   */

  public userCategoryWizard(GanymedeSession session, 
			    userCustom userObject, 
			    Invid oldInvid,
			    Invid newInvid) throws RemoteException
  {
    super(session);		// register ourselves

    this.session = session;
    this.userObject = userObject;

    this.newCatInvid = newInvid;
    this.oldCatInvid = oldInvid;

    if (oldInvid != null)
      {
	this.oldCategory = userObject.getSession().viewDBObject(oldInvid);
      }

    if (newInvid != null)
      {
	this.newCategory = userObject.getSession().viewDBObject(newInvid);
      }

    if (oldCategory != null)
      {
	if (debug)
	  {
	    System.err.println("userCategoryWizard(): oldCategory = " + oldCategory.getLabel());
	  }

	Boolean notifyBoolean = (Boolean) oldCategory.getFieldValueLocal(userCategorySchema.APPROVALREQ);

	if (notifyBoolean != null && notifyBoolean.booleanValue())
	  {
	    this.notificationRequired = true;

	    Vector notifyList2 = oldCategory.getFieldValuesLocal(userCategorySchema.APPROVALLIST);

	    this.notifyList = arlut.csd.Util.VectorUtils.union(this.notifyList, notifyList2);
	  }
      }
    else if (debug)
      {
	System.err.println("userCategoryWizard(): oldCategory = null");
      }

    if (newCategory != null)
      {
	if (debug)
	  {
	    System.err.println("userCategoryWizard(): newCategory = " + newCategory.getLabel());
	  }

	Boolean notifyBoolean = (Boolean) newCategory.getFieldValueLocal(userCategorySchema.APPROVALREQ);

	if (notifyBoolean != null && notifyBoolean.booleanValue())
	  {
	    this.notificationRequired = true;

	    Vector notifyList2 = newCategory.getFieldValuesLocal(userCategorySchema.APPROVALLIST);

	    this.notifyList = arlut.csd.Util.VectorUtils.union(this.notifyList, notifyList2);
	  }

	// get the number of days in the future that the user's
	// expiration may be set to.

	Integer timelimit = (Integer) newCategory.getFieldValueLocal(userCategorySchema.LIMIT);

	if (timelimit != null)
	  {
	    int days = timelimit.intValue();

	    if (debug)
	      {
		System.err.println("userCategoryWizard(): termination limit days = " + days);
	      }

	    Date nowDate = new Date();
	    Calendar cal = Calendar.getInstance();

	    cal.setTime(nowDate);
	    cal.add(Calendar.DATE, days);

	    this.terminationDate = cal.getTime();

	    if (debug)
	      {
		System.err.println("userCategoryWizard(): termination limit = " + terminationDate.toString());
	      }
	  }
	else if (debug)
	  {
	    System.err.println("userCategoryWizard(): no limit set");
	  }

	Boolean expireBoolean = (Boolean) newCategory.getFieldValueLocal(userCategorySchema.EXPIRE);

	if ((expireBoolean != null && expireBoolean.booleanValue()) ||
	    (terminationDate != null))
	  {
	    this.needExpireDate = true;
	  }
      }
    else if (debug)
      {
	System.err.println("userCategoryWizard(): newCategory = null");
      }

    if (debug)
      {
	if (needExpireDate)
	  {
	    System.err.println("userCategoryWizard(): needExpireDate is true");
	  }
	else
	  {
	    System.err.println("userCategoryWizard(): needExpireDate is false");
	  }
      }

    // what is the user's expiration set to now?

    this.expirationDate = (Date) userObject.getFieldValueLocal(SchemaConstants.ExpirationField);
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
    return fail("Category Change Cancelled",
		"Category Change Cancelled",
		"OK",
		null,
		"ok.gif");
  }


  /**
   * <P>This method starts off the wizard process.</P>
   */

  public ReturnVal processDialog0()
  {
    ReturnVal retVal = null;
    StringBuffer tempBuffer = new StringBuffer();

    /* -- */

    if ((oldCatInvid == null) && !notificationRequired &&
	(notifyList == null) && !needExpireDate)
      {
	// don't need to ask the user anything, let things go on.

	return null;
      }
    
    if (oldCatInvid == null)
      {
	tempBuffer.append("In order to put user ");
	tempBuffer.append(userObject.getLabel());
	tempBuffer.append(" in category ");
	tempBuffer.append(newCategory.getLabel());
	tempBuffer.append(", ");

	if (needExpireDate)
	  {
	    tempBuffer.append("you need to set an expiration date for this user");

	    if (notificationRequired)
	      {
		tempBuffer.append(" and enter a short justification for this classification.");
	      }
	  }
	else if (notificationRequired)
	  {
	    tempBuffer.append(" you must enter a short justification for this classification.");
	  }
      }
    else
      {
	tempBuffer.append("In order to move user ");
	tempBuffer.append(userObject.getLabel());
	tempBuffer.append(" from category ");
	tempBuffer.append(oldCategory.getLabel());
	tempBuffer.append(" to category ");
	tempBuffer.append(newCategory.getLabel());
	tempBuffer.append(", ");

	if (needExpireDate)
	  {
	    tempBuffer.append("you need to set an expiration date for this user");

	    if (notificationRequired)
	      {
		tempBuffer.append(" and enter a short justification for the new classification.");
	      }
	  }
	else if (notificationRequired)
	  {
	    tempBuffer.append(" you must enter a short justification for the new classification.");
	  }
      }

    retVal = continueOn("User Category Change Dialog",
			tempBuffer.toString(),
			"Next",
			"Cancel",
			"question.gif");

    if (!needExpireDate)
      {
	setNextState(2);	// from GanymediatorWizard
      }

    return retVal;
  }

  /**
   *
   * The client will call us here with no params.. this step
   * will prompt for the expire date for this user.
   *
   */

  public ReturnVal processDialog1()
  {
    ReturnVal retVal = null;
    StringBuffer tempBuffer = new StringBuffer();

    /* -- */

    if (debug)
      {
	System.err.println("Entering userCategoryWizard.processDialog1()");
      }

    tempBuffer.append("Category ");
    tempBuffer.append(newCategory.getLabel());
    tempBuffer.append("requires that an expiration date be set.");

    if (terminationDate != null)
      {
	tempBuffer.append("  You may set this expiration date to any day on or before\n\n");
	tempBuffer.append(terminationDate.toString());
      }

    retVal = continueOn("Category Requires an Expiration",
			tempBuffer.toString(),
			"Next",
			"Cancel",
			"question.gif");

    retVal.getDialog().addDate("Expiration Date:", expirationDate, terminationDate);

    return retVal;
  }

  /**
   *
   * Process expirationDate if needed, prompt for explanation if
   * needed.
   *
   */

  public ReturnVal processDialog2()
  {
    ReturnVal retVal = null;

    /* -- */

    if (debug)
      {
	System.err.println("Entering userCategoryWizard.processDialog2()");
      }

    if (needExpireDate)
      {
	if (debug)
	  {
	    System.err.println("userCategoryWizard.processDialog2(): processing date");
	  }

	this.expirationDate = (Date) getParam("Expiration Date:");

	if (this.expirationDate == null)
	  {
	    return fail("Category Change Cancelled",
			"Error, an expiration date is required to put " + userObject.getLabel() +
			" into category " + newCategory.getLabel() + ".",
			"OK", null, "ok.gif");
	  }
      }

    // if we need to get an explanation from the user, ask for it here.

    if (notificationRequired)
      {
	if (debug)
	  {
	    System.err.println("userCategoryWizard.processDialog2(): prompting for notification");
	  }

	StringBuffer tempBuffer = new StringBuffer();

	tempBuffer.append("Before you can change ");
	tempBuffer.append(userObject.getLabel());
	tempBuffer.append("'s classification, you need to provide a short justification of ");

	if (oldCatInvid == null)
	  {
	    tempBuffer.append("this user's classification.");
	  }
	else
	  {
	    tempBuffer.append("this user's change in classification.");
	  }

	retVal = continueOn("Justification for Category Change",
			    tempBuffer.toString(),
			    "Next",
			    "Cancel",
			    "question.gif");

	retVal.getDialog().addMultiString("Reason:", null);

	return retVal;
      }
    else
      {
	return doIt();
      }
  }

  /**
   *
   * Process explanation.
   *
   */

  public ReturnVal processDialog3()
  {
    if (debug)
      {
	System.err.println("Entering userCategoryWizard.processDialog3()");
      }

    this.justification = (String) getParam("Reason:");

    if (this.justification == null || this.justification.equals(""))
      {
	return fail("Category Change Cancelled",
		    "Error, an explanation is required to put " + userObject.getLabel() +
		    " into category " + newCategory.getLabel() + ".",
		    "OK", null, "ok.gif");
      }
    else if (debug)
      {
	System.err.println("userCategoryWizard.processDialog3(): justification = \n" + justification);
      }

    return doIt();
  }

  /**
   *
   * Do the thing.
   *
   */

  public ReturnVal doIt()
  {
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	System.err.println("Entering userCategoryWizard.doIt()");
      }

    // let the userCustom object know that we approve the category change.

    state = DONE;

    // if we can't do both of these, we'll just leave it up to the
    // user to hit cancel.  We could do a checkpoint here, but it
    // probably isn't necessary.. the category change is the only
    // thing likely to fail, if the categoryCustom object doesn't have
    // anonymousLinkOK() enabled.

    if (debug)
      {
	if (newCatInvid != null)
	  {
	    System.err.println("userCategoryWizard.doIt(): setting category to " + newCatInvid);
	  }
	else
	  {
	    System.err.println("userCategoryWizard.doIt(): setting category to null");
	  }
      }

    retVal = userObject.setFieldValue(userCustom.CATEGORY, newCatInvid);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    if (debug)
      {
	if (expirationDate != null)
	  {
	    System.err.println("userCategoryWizard.doIt(): setting expiration to " + expirationDate);
	  }
	else
	  {
	    System.err.println("userCategoryWizard.doIt(): setting date to null");
	  }
      }

    retVal = userObject.setFieldValue(SchemaConstants.ExpirationField, expirationDate);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    StringBuffer tempBuffer = new StringBuffer();

    if (oldCatInvid == null)
      {
	tempBuffer.append("User ");
	tempBuffer.append(userObject.getLabel());
	tempBuffer.append(" has been placed in category ");
	tempBuffer.append(newCategory.getLabel());
	tempBuffer.append(".");
      }
    else
      {
	tempBuffer.append("User ");
	tempBuffer.append(userObject.getLabel());
	tempBuffer.append(" has been reclassified as ");
	tempBuffer.append(newCategory.getLabel());
	tempBuffer.append(".");
      }

    if (justification != null)
      {
	tempBuffer.append("\n\n");
	tempBuffer.append(justification);
      }

    if (notifyList != null)
      {
	userObject.getEditSet().logMail(notifyList,
					"CategorySet: User " + userObject.getLabel() + " in " +
					newCategory.getLabel(),
					tempBuffer.toString());
      }

    retVal = success("Category Changed",
		     tempBuffer.toString(),
		     "OK", null, "ok.gif");

    retVal.addRescanField(userObject.getInvid(), SchemaConstants.ExpirationField);

    return retVal;
  }

}
