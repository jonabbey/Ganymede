/*

   groupCustom.java

   This file is a management class for group objects in Ganymede.
   
   Created: 30 July 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import arlut.csd.JDialog.JDialogBuff;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     groupCustom

------------------------------------------------------------------------------*/

public class groupCustom extends DBEditObject implements SchemaConstants, groupSchema {
  
  static final boolean debug = false;

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * group's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public groupCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public groupCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public groupCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * Initialize a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has
   * been created and all fields defined in the
   * controlling DBObjectBase have been instantiated
   * without defined values.<br><br>
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the DBSession
   * associated with the editset defined in this DBEditObject.<br><br>
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return false.  Right now there is no infrastructure in
   * Ganymede to allow the transaction to be aborted from
   * within the DBSession's createDBObject() method.  As a result,
   * if this method is to fail to properly initialize the object,
   * it should be able to not leave an impact on the rest of the
   * DBStore.. in other words, setting InvidField values that
   * involve symmetry relationships could be problematic. <br><br>
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
		
		db_object user = gSession.edit_db_object(userInvid);
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
				homeGroup.setValue(groupList.elementAt(i));
				
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

    return null;
  }

  private void print(String s)
  {
    System.err.println("groupCustom.wizardHook(): " + s);
  }
}
