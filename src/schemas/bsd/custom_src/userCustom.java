/*

   userCustom.java

   This file is a management class for user objects in Ganymede.
   
   Created: 30 July 1997
   Release: $Name:  $
   Version: $Revision: 1.32 $
   Last Mod Date: $Date: 1999/07/14 21:51:14 $
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

import arlut.csd.ganymede.*;

import arlut.csd.JDialog.JDialogBuff;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      userCustom

------------------------------------------------------------------------------*/

/**
 *
 * This class is the custom plug-in to handle the user object type in the
 * Ganymede server.  It does special validations of operations on the user,
 * handles inactivation and reactivation logic, and generates Wizards as
 * needed.<br>
 *
 * <br>See the userSchema.java file for a list of field definitions that this
 * module expects to work with.<br>
 *
 * @see arlut.csd.ganymede.custom.userSchema
 * @see arlut.csd.ganymede.DBEditObject
 *
 */

public class userCustom extends DBEditObject implements SchemaConstants, userSchema {
  
  static final boolean debug = false;
  static QueryResult shellChoices = new QueryResult();
  static Date shellChoiceStamp = null;

  static String homedir = null;

  // ---

  QueryResult groupChoices = null;

  /**
   *
   * Customization Constructor
   *
   */

  public userCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public userCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public userCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   *
   * Initialize a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * DBObjectBase have been instantiated without defined
   * values.<br><br>
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the DBSession
   * associated with the editset defined in this DBEditObject.<br><br>
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return false.  If the owning GanymedeSession is not in
   * bulk-loading mode (i.e., enableOversight is true),
   * DBSession.createDBObject() will checkpoint the transaction before
   * calling this method.  If this method returns false, the calling
   * method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.<br><br>
   *
   * If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.<br><br>
   *
   * This method should be overridden in subclasses.
   *   
   */

  public boolean initializeNewObject()
  {
    ReturnVal retVal;

    /* -- */

    // we don't want to do initialization if we are bulk-loading.

    if (!getGSession().enableOversight)
      {
	return true;
      }

    // need to find a uid for this user

    NumericDBField numField = (NumericDBField) getField(UID);

    if (numField == null)
      {
	System.err.println("userCustom.initializeNewObject(): couldn't get uid field");
	return false;
      }

    DBNameSpace namespace = numField.getNameSpace();

    if (namespace == null)
      {
	System.err.println("userCustom.initializeNewObject(): couldn't get uid namespace");
	return false;
      }

    // now, find a uid.. unfortunately, we have to use immutable Integers here.. not
    // the most efficient at all.

    Integer uidVal = new Integer(1001);

    while (!namespace.testmark(editset, uidVal))
      {
	uidVal = new Integer(uidVal.intValue()+1);
      }

    // we use setValueLocal so we can set a value that the user can't edit.

    retVal = numField.setValueLocal(uidVal);

    return (retVal == null || retVal.didSucceed());
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case userSchema.USERNAME:
      case userSchema.UID:
      case userSchema.LOGINSHELL:
      case userSchema.HOMEDIR:
      case userSchema.PASSWORD:
	return true;
      }

    return false;
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.<br><br>
   *
   * We don't want the HOMEGROUP field's choice list to be cached on
   * the client because it is dynamically generated for this
   * context, and doesn't make sense in other contexts.
   * 
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == HOMEGROUP)
      {
	return null;
      }
    else
      {
	return super.obtainChoicesKey(field);
      }
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   *
   * Notice that fields 263 (login shell) and 268 (signature alias)
   * do not have their choice lists cached on the client, because
   * they are custom generated without any kind of accompanying
   * cache key.
   * 
   */

  public synchronized QueryResult obtainChoiceList(DBField field)
  {
    switch (field.getID())
      {
      case LOGINSHELL:			// login shell

	updateShellChoiceList();

	if (debug)
	  {
	    System.err.println("userCustom: obtainChoice returning " + shellChoices + " for shell field.");
	  }

	return shellChoices;

      case HOMEGROUP:			// home group

	updateGroupChoiceList();
	return groupChoices;

      default:
	return super.obtainChoiceList(field);
      }
  }

  void updateGroupChoiceList()
  {
    if (groupChoices == null)
      {
	groupChoices = new QueryResult();
	
	Vector invids = getFieldValuesLocal(GROUPLIST); // groups list
	Invid invid;
	
	for (int i = 0; i < invids.size(); i++)
	  {
	    invid = (Invid) invids.elementAt(i);
	    
	    groupChoices.addRow(invid, gSession.viewObjectLabel(invid), true); // must be editable because the client cares
	  }
      }
  }

  void updateShellChoiceList()
  {
    synchronized (shellChoices)
      {
	DBObjectBase base = Ganymede.db.getObjectBase("Shell Choice");

	// just go ahead and throw the null pointer if we didn't get our base.

	if (shellChoiceStamp == null || shellChoiceStamp.before(base.getTimeStamp()))
	  {
	    System.err.println("userCustom - updateShellChoiceList()");

	    shellChoices = new QueryResult();

	    Query query = new Query("Shell Choice", null, false);

	    // internalQuery doesn't care if the query has its filtered bit set

	    System.err.println("userCustom - issuing query");

	    Vector results = internalSession().internalQuery(query);

	    System.err.println("userCustom - processing query results");
	
	    for (int i = 0; i < results.size(); i++)
	      {
		shellChoices.addRow(null, results.elementAt(i).toString(), false); // no invid
	      }

	    if (shellChoiceStamp == null)
	      {
		shellChoiceStamp = new Date();
	      }
	    else
	      {
		shellChoiceStamp.setTime(System.currentTimeMillis());
	      }
	  }
      }
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to remove a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for removal by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canRemove(DBSession session, DBObject object)
  {
    String name = (String) object.getFieldValueLocal(userSchema.USERNAME);

    if (name != null && name.equals("root"))
      {
	return false;
      }

    return true;
  }

  public synchronized ReturnVal finalizeSetValue(DBField field, Object value)
  {
    InvidDBField inv;
    Vector personaeInvids;
    Vector oldNames = new Vector();
    DBSession session = editset.getSession();
    DBEditObject eobj;
    String oldName, suffix;
    StringDBField sf;
    boolean okay = true;

    /* -- */

    if (field.getID() == HOMEDIR)
      {
	String dir = (String) value;

	/* -- */

	sf = (StringDBField) getField(USERNAME);

	if (homedir == null)
	  {
	    homedir = System.getProperty("ganymede.homedirprefix");
	  }

	// we will only check against a defined prefix if
	// we have set one in our properties file.
	
	if (homedir != null && homedir.length() != 0)
	  {
	    sf = (StringDBField) getField(USERNAME);

	    if (sf != null)
	      {
		if (sf.getNewValue() != null)
		  {
		    String expected = homedir + (String) sf.getNewValue();
		    
		    if (!dir.equals(expected))
		      {
			return Ganymede.createErrorDialog("Schema Error",
							  "Home directory should be " + expected + ".\n" +
							  "This is a restriction encoded in userCustom.java.");
		      }
		  }
	      }
	  }

	return null;
      }

    if (field.getID() == USERNAME)
      {
	// if we are being told to clear the user name field, go ahead and
	// do it.. we assume this is being done by user removal logic,
	// so we won't force everything to go through a wizard.
	
	if (deleting && (value == null))
	  {
	    return null;
	  }

	// update the home directory location.. we assume that if
	// the user has permission to rename the user, they can
	// automatically execute this change to the home directory.

	if (homedir == null)
	  {
	    homedir = System.getProperty("ganymede.homedirprefix");
	  }

	if (homedir != null)
	  {
	    sf = (StringDBField) getField(HOMEDIR);
	    sf.setValueLocal(homedir + (String) value);
	  }

	// rename all the associated persona with the new user name

	inv = (InvidDBField) getField(PERSONAE);
	
	if (inv == null)
	  {
	    return null;
	  }

	sf = (StringDBField) getField(USERNAME); // old user name

	oldName = (String) sf.getValueLocal();

	personaeInvids = inv.getValues();

	for (int i = 0; i < personaeInvids.size(); i++)
	  {
	    adminPersonaCustom adminObj = (adminPersonaCustom) session.editDBObject((Invid) personaeInvids.elementAt(i));

	    adminObj.refreshLabelField(null, null, (String) value);
	  }
      }

    return null;
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards whenever
   * a sensitive field is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    userHomeGroupDelWizard groupWizard = null;
    userRenameWizard renameWizard = null;
    ReturnVal result;
    
    /* -- */

    // if the groups field is being changed, we may need to intervene

    if (debug)
      {
	System.err.println("userCustom ** entering wizardHook, field = " + field.getName() + ", op= " + operation);
      }

    if (field.getID() == GROUPLIST)
      {
	switch (operation)
	  {
	  case ADDELEMENT:

	    // ok, no big deal, but we will need to have the client
	    // rescan the choice list for the home group field

	    result = new ReturnVal(true, true);
	    result.addRescanField(this.getInvid(), HOMEGROUP);
	    groupChoices = null;
	    return result;

	  case DELELEMENT:

	    // ok, this is more of a big deal.. first, see if the value
	    // being deleted is the home group.  If not, still no big
	    // deal.

	    int index = ((Integer) param1).intValue();

	    Vector valueAry = getFieldValuesLocal(GROUPLIST);
	    Invid delVal = (Invid) valueAry.elementAt(index);

	    System.err.println("userCustom: deleting group element " + gSession.viewObjectLabel(delVal));

	    if (!delVal.equals(getFieldValueLocal(HOMEGROUP)))
	      {
		// whew, no big deal.. they are not removing the
		// home group.  The client will need to rescan,
		// but no biggie.

		System.err.println("userCustom: I don't think " + gSession.viewObjectLabel(delVal) + 
				   " is the home group");

		result = new ReturnVal(true, true);
		result.addRescanField(this.getInvid(), HOMEGROUP);
		groupChoices = null;
		return result;
	      }

	    if (gSession.isWizardActive() && gSession.getWizard() instanceof userHomeGroupDelWizard)
	      {
		groupWizard = (userHomeGroupDelWizard) gSession.getWizard();
		
		if (groupWizard.getState() == groupWizard.DONE)
		  {
		    // ok, assume the wizard has taken care of getting everything prepped and
		    // approved for us.  An active wizard has approved the operation
		
		    groupWizard.unregister();
		
		    return null;
		  }
		else
		  {
		    if (groupWizard.userObject != this)
		      {
			System.err.println("userCustom.wizardHook(): bad object");
		      }
		    
		    if (groupWizard.getState() != groupWizard.DONE)
		      {
			System.err.println("userCustom.wizardHook(): bad state: " + groupWizard.getState());
		      }

		    groupWizard.unregister();

		    return Ganymede.createErrorDialog("User Object Error",
						      "The client is attempting to do an operation on " +
						      "a user object with an active wizard.");
		  }
	      }
	    else if (gSession.isWizardActive() && !(gSession.getWizard() instanceof userHomeGroupDelWizard))
	      {
		return Ganymede.createErrorDialog("User Object Error",
						  "The client is attempting to do an operation on " +
						  "a user object with mismatched active wizard.");
	      }

	    // eek.  they are deleting the home group.  Why Lord, why?!

	    try
	      {
		groupWizard = new userHomeGroupDelWizard(this.gSession,
							 this,
							 param1);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Couldn't create userWizard " + ex.getMessage());
	      }

	    // if we get here, the wizard was able to register itself.. go ahead
	    // and return the initial dialog for the wizard.  The ReturnVal code
	    // that wizard.respond(null) returns will have the success code
	    // set to false, so whatever triggered us will prematurely exit,
	    // returning the wizard's dialog.
	    
	    return groupWizard.respond(null);
	  }
      }

    if ((field.getID() != USERNAME) ||
	(operation != SETVAL))
      {
	return null;		// by default, we just ok whatever else
      }

    // if we have not previously committed this user, we don't care if the
    // user's name is set.

    if ((field.getValue() == null) || (getStatus() == ObjectStatus.CREATING))
      {
	result = new ReturnVal(true, true); // have setValue() do the right thing

	result.addRescanField(this.getInvid(), userSchema.HOMEDIR);

	return result;
      }

    if (field.getValue().equals("root"))
      {
	return Ganymede.createErrorDialog("Can't rename root",
					  "Sorry, you can't rename or remove root.  Really not what you want to do.");
      }

    if (!gSession.enableWizards)
      {
	return null;		// no wizards if the user is non-interactive.
      }

    // looks like we're renaming this user

    if (gSession.isWizardActive() && gSession.getWizard() instanceof userRenameWizard)
      {
	renameWizard = (userRenameWizard) gSession.getWizard();

	if ((renameWizard.getState() == renameWizard.DONE) &&
	    (renameWizard.field == field) &&
	    (renameWizard.userObject == this) &&
	    (renameWizard.newname == param1))
	  {
	    // ok, assume the wizard has taken care of getting everything prepped and
	    // approved for us.  An active wizard has approved the operation
		
	    renameWizard.unregister();
		
	    return null;
	  }
	else
	  {
	    if (renameWizard.field != field)
	      {
		System.err.println("userCustom.wizardHook(): bad field");
	      }

	    if (renameWizard.userObject != this)
	      {
		System.err.println("userCustom.wizardHook(): bad object");
	      }

	    if (renameWizard.newname != param1)
	      {
		System.err.println("userCustom.wizardHook(): bad param");
	      }

	    if (renameWizard.getState() != renameWizard.DONE)
	      {
		System.err.println("userCustom.wizardHook(): bad state: " + renameWizard.getState());
	      }

	    renameWizard.unregister();
	    return Ganymede.createErrorDialog("User Object Error",
					      "The client is attempting to do an operation on " +
					      "a user object with an active wizard.");
	  }
      }
    else if (gSession.isWizardActive() && !(gSession.getWizard() instanceof userRenameWizard))
      {
	return Ganymede.createErrorDialog("User Object Error",
					  "The client is attempting to do an operation on " +
					  "a user object with mismatched active wizard.");
      }
    else
      {
	// there's no wizard active, and this operation has to be approved by one.  Go ahead
	// and set up the wizard and let the client play with it.

	// if we're setting the field to null, don't need to pass it through
	// a wizard.. we're probably just deleting this user.

	if (deleting && (param1 == null))
	  {
	    return null;
	  }

	try
	  {
	    renameWizard = new userRenameWizard(this.gSession,
						this,
						field,
						(String) param1);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't create userWizard " + ex.getMessage());
	  }
	
	// if we get here, the wizard was able to register itself.. go ahead
	// and return the initial dialog for the wizard.  The ReturnVal code
	// that wizard.respond(null) returns will have the success code
	// set to false, so whatever triggered us will prematurely exit,
	// returning the wizard's dialog.

	return renameWizard.respond(null);
      }
  }
}
