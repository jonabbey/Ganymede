/*

   groupCustom.java

   This file is a management class for group objects in Ganymede.
   
   Created: 30 July 1997
   Release: $Name:  $
   Version: $Revision: 1.13 $
   Last Mod Date: $Date: 1999/03/10 06:05:47 $
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

import arlut.csd.Util.*;
import arlut.csd.JDialog.JDialogBuff;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     groupCustom

------------------------------------------------------------------------------*/

public class groupCustom extends DBEditObject implements SchemaConstants, groupSchema {
  
  static final boolean debug = true;
  static final boolean debug2 = false;

  static QueryResult contractChoices = new QueryResult();
  static Date contractChoiceStamp = null;


  /**
   *
   * Customization Constructor
   *
   */

  public groupCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public groupCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public groupCustom(DBObject original, DBEditSet editset)
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

    // we don't want to do any of this initialization during
    // bulk-loading.

    if (!getGSession().enableOversight)
      {
	return true;
      }

    // need to find a gid for this group

    NumericDBField numField = (NumericDBField) getField((short) 258);

    if (numField == null)
      {
	System.err.println("groupCustom.initializeNewObject(): couldn't get gid field");
	return false;
      }

    DBNameSpace namespace = numField.getNameSpace();

    if (namespace == null)
      {
	System.err.println("groupCustom.initializeNewObject(): couldn't get gid namespace");
	return false;
      }

    // now, find a gid.. unfortunately, we have to use immutable Integers here.. not
    // the most efficient at all.

    Integer gidVal = new Integer(1001);

    while (!namespace.testmark(editset, gidVal))
      {
	gidVal = new Integer(gidVal.intValue()+1);
      }

    // we use setValueLocal so we can set a value that the user can't edit.

    retVal = numField.setValueLocal(gidVal);

    return (retVal == null || retVal.didSucceed());
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canBeInactivated()
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the group has permission
   * to inactivate a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return true;
  }

  /**
   *
   * Called when object is inactivated.  This will take care of users
   * who have this group as the home group.  
   *
   * Overides inactivate() in DBEditObject.
   */

  public ReturnVal inactivate()
  {
    return inactivate(false, false);
  }

  public ReturnVal inactivate(boolean suceeded, boolean fromWizard) 
  {
    ReturnVal retVal = null;
    groupInactivateWizard wiz;

    /* -- */

    if (fromWizard)
      {
	if (suceeded)
	  {
	    if (debug)
	      {
		System.err.println("groupCustom.inactivate: setting removal date.");
	      }

	    DateDBField date;
	    Calendar cal = Calendar.getInstance();
	    Date time;
	    
	    // make sure that the expiration date is cleared.. we're on
	    // the removal track now.
	    
	    date = (DateDBField) getField(SchemaConstants.ExpirationField);
	    retVal = date.setValueLocal(null);
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		super.finalizeInactivate(false);
		return retVal;
	      }
	    
	    // determine what will be the date 3 months from now
	    
	    time = new Date();
	    cal.setTime(time);
	    cal.add(Calendar.MONTH, 3);
	    
	    // and set the removal date
	    if (debug)
	      {
		System.err.println("groupCustom.inactivate: setting removal date to: " + cal.getTime());
	      }

	    date = (DateDBField) getField(SchemaConstants.RemovalField);
	    retVal = date.setValueLocal(cal.getTime());

	    if ((retVal == null) || (retVal.didSucceed()))
	      {
		if (debug)
		  {
		    System.err.println("groupCustom.inactivate: retVal returns true, I am finalizing.");
		  }

		finalizeInactivate(true);
	      }

		return retVal;
	  }
	else
	  {
	    finalizeInactivate(false);
	    return new ReturnVal(false);
	  }
      }
    else
      {
	try
	  {
	    if (debug)
	      {
		System.err.println("groupCustom.inactivate: Starting new groupInactivateWizard");
	      }

	    wiz = new groupInactivateWizard(this.gSession, this);
	    
	    return wiz.getStartDialog();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not create groupInactivateWizard: " + rx);
	  }
      }

  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   * */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case groupSchema.GROUPNAME:
      case groupSchema.GID:
	return true;
      }

    return false;
  }

  /**
   * Handle the wizards for dealing with users that have this as the home group.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    groupHomeGroupWizard homeWizard;
    ReturnVal retVal = null;

    /* -- */

    // First find out what they are changing

    if (debug)
      {
	System.out.println("Field name: " + field.getName() + 
			   " Field typeDesc: " + field.getTypeDesc());
      }

    if (field.getID() == HOMEUSERS) // from groupSchema
      {    
	// What are they doing to the home users?

	switch (operation) 
	  {
	  case ADDELEMENT:

	    if (debug)
	      {
		print("it's an ADDELEMENT, ignoring it.");
	      }

	    // we don't need to rescan anything, do we?
	    return null;

	  case DELELEMENT:

	    if (debug)
	      {
		print("HOMEUSERS field changing.");
	      }
	
	    Vector users = getFieldValuesLocal(HOMEUSERS);
	    int index = ((Integer) param1).intValue();
	    Invid userInvid = (Invid)users.elementAt(index);

	    if (gSession == null)
	      {
		// If there is no session, the server is doing something special.
		// Assume the server knows what is going on, and let it do the deed.
		return null;
	      }
	    else if (!gSession.enableWizards)
	      {
		// Stupid client won't let us show wizards.  We'll teach them!
		// First, find out what is going on.  How many groups is this user in?
		
		db_object user = gSession.edit_db_object(userInvid).getObject();
		int size = 0;

		try
		  {
		    size = user.getField(userSchema.GROUPLIST).size();
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("How come I can't talk to the server, when I AM the server? " + rx);
		  }
	      
		if (size == 2)
		  {
		    // They belong to two groups: this one, and one other one.
		    // We will make the other one the home group.

		    try
		      {
			db_field groupListField = user.getField(userSchema.GROUPLIST);
			Vector groupList = groupListField.getValues();    
			
			for (int i = 0; i < groupList.size() ;i++)
			  {
			    if (!this.equals(groupList.elementAt(i)))
			      {
				// this will be the new home group

				if (debug)
				  {
				    print("Found the other group, changing the user's home group.");
				  }
			      
				db_field homeGroup = user.getField(userSchema.HOMEGROUP);
				retVal = homeGroup.setValue(groupList.elementAt(i));
				
				break;
			      }
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Again, with the remote exceptions: " + rx);
		      }
		  }
		else if (size < 1)
		  {
		    // They are only in one group, so what good is that?
		    return Ganymede.createErrorDialog("Group Change Failed",
						      "This user has this group for a home group. " +
						      " You cannot remove this user, since this is his only group.");
		  }
		else 
		  {
		    return Ganymede.createErrorDialog("Group Change Failed",
						      "This user has many groups to choose from. " +
						      " You must choose one to be the home group, or turn wizards on.");
		  }
	      }
  
	    // This calls for a wizard.  See if one is running already

	    if (gSession.isWizardActive() && gSession.getWizard() instanceof groupHomeGroupWizard)
	      {
		if (debug)
		  {
		    print("Ok, wizard is running.  Checking to see if it is done.");
		  }

		// We are already in this wizard, lets see where we are

		homeWizard = (groupHomeGroupWizard)gSession.getWizard();
	      
		if (homeWizard.getState() == homeWizard.DONE)
		  {
		    // Ok, the home wizard has done its deed, so get rid of it
		    homeWizard.unregister();

		    // I don't think it is a good idea to return null here.
		    if (debug)
		      {
			print("Returning null, because I am in groupCustom.wizardHook with an active wizard that is done.");
		      }

		    return null;
		  }
		else
		  {
		    if (homeWizard.groupObject != this)
		      {
			print("bad object, group objects confused somehow.");
		      }
		    
		    if (homeWizard.getState() != homeWizard.DONE)
		      {
			print(" bad state: " + homeWizard.getState());
		      }
		  
		    homeWizard.unregister(); // get rid of it, so it doesn't mess other stuff up
		    return Ganymede.createErrorDialog("Group object error",
						      "The client is attempting to do an operation " +
						      "on a user object with an active wizard.");
		  }
	      }
	    else if (gSession.isWizardActive() &&
		     !(gSession.getWizard() instanceof groupHomeGroupWizard))
	      {
		return Ganymede.createErrorDialog("Group Object Error",
						  "The client is trying to change the group object " +
						  "while other wizards are running around.");
	      }
	  
	    // Ok, if we get to here, then we need to start up a new wizard.
	    // The user is trying to remove someone out of the HOMEUSER field, which may cause problems.
	    
	    try
	      {
		if (debug)
		  {
		    print("Starting up a new wizard");
		  }
		
		homeWizard = new groupHomeGroupWizard(this.gSession, this, userInvid);
		
		return homeWizard.getStartDialog();
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not send wizard to client: " + rx);
	      }
	  }
      }
    else
      {
	if (debug)
	  {
	    print("it's not the HOMEGROUP");
	  }
      }

    // otherwise, we don't care, at least not yet

    return retVal;
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

  public QueryResult obtainChoiceList(DBField field)
  {
    switch (field.getID())
      {
      case CONTRACT:			// login shell

	updateContractChoices();

	if (debug2)
	  {
	    System.err.println("groupCustom: obtainChoice returning " + contractChoices + " for contract field.");

	    System.err.println(contractChoices.getBuffer());
	  }

	return contractChoices;
      }

    return super.obtainChoiceList(field);
  }

  void updateContractChoices()
  {
    synchronized (contractChoices)
      {
	DBObjectBase base = Ganymede.db.getObjectBase((short) 280);

	// just go ahead and throw the null pointer if we didn't get our base.

	if (contractChoiceStamp == null || contractChoiceStamp.before(base.getTimeStamp()))
	  {
	    if (debug)
	      {
		System.err.println("groupCustom - updateContractChoices()");
	      }

	    contractChoices = new QueryResult();

	    Query query = new Query((short) 280, null, false);

	    // internalQuery doesn't care if the query has its filtered bit set
	    
	    if (debug)
	      {
		System.err.println("groupCustom - issuing query");
	      }

	    Vector results = internalSession().internalQuery(query);

	    (new VecQuickSort(results,
			      new arlut.csd.Util.Compare()
			      {
				public int compare(Object a, Object b)
				  {
				    Result aF, bF;
				    
				    aF = (Result) a;
				    bF = (Result) b;
				    
				    return aF.toString().compareTo(bF.toString());
				  }
			      }
			      )).sort();
			      
	    if (debug)
	      {
		System.err.println("groupCustom - processing query results");
	      }
	
	    for (int i = 0; i < results.size(); i++)
	      {
		contractChoices.addRow(null, results.elementAt(i).toString(), false); // no invid
	      }

	    if (contractChoiceStamp == null)
	      {
		contractChoiceStamp = new Date();
	      }
	    else
	      {
		contractChoiceStamp.setTime(System.currentTimeMillis());
	      }
	  }
      }
  }

  private void print(String s)
  {
    System.err.println("groupCustom.wizardHook(): " + s);
  }
}
