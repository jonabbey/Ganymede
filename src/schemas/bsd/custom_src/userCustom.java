/*

   userCustom.java

   This file is a management class for user objects in Ganymede.
   
   Created: 30 July 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     userCustom

------------------------------------------------------------------------------*/

public class userCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;
  static QueryResult shellChoices = new QueryResult();
  static Date shellChoiceStamp = null;

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * user's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public userCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public userCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public userCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * Hook to have this object create a new embedded object
   * in the given field.  
   *
   */

  public Invid createNewEmbeddedObject(InvidDBField field)
  {
    DBEditObject newObject;
    DBObjectBase targetBase;
    DBObjectBaseField fieldDef;

    /* -- */

    if (field.getID() == 271)	// auxiliary volume mappings
      {
	fieldDef = field.getFieldDef();

	if (fieldDef.getTargetBase() > -1)
	  {
	    targetBase = Ganymede.db.getObjectBase(fieldDef.getTargetBase());
	    newObject = targetBase.createNewObject(editset);
	    return newObject.getInvid(); // just creating, not initializing at current
	  }
	else
	  {
	    editset.getSession().setLastError("error in schema.. imbedded object type not restricted..");
	    return null;
	  }
      }
    else
      {
	return null;		// default
      }
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
   */

  public boolean mustChoose(DBField field)
  {
    return (field.getID() == 268); // we want to force signature alias choosing
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
      case 263:			// login shell

	synchronized (shellChoices)
	  {
	    DBObjectBase base = Ganymede.db.getObjectBase("Shell Choice");

	    // just go ahead and throw the null pointer if we didn't get our base.

	    if (shellChoiceStamp == null || shellChoiceStamp.before(base.getTimeStamp()))
	      {
		shellChoices = new QueryResult();
		Query query = new Query("Shell Choice", null, false);

		// internalQuery doesn't care if the query has its filtered bit set

		Vector results = internalSession().internalQuery(query);
	
		for (int i = 0; i < results.size(); i++)
		  {
		    shellChoices.addRow(null, results.elementAt(i).toString()); // no invid
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

	if (debug)
	  {
	    System.err.println("userCustom: obtainChoice returning " + shellChoices + " for shell field.");
	  }

	return shellChoices;

      case 268:			// signature alias

	QueryResult result = new QueryResult();

	/* -- */

	// our list of possible aliases includes the user's name

	String name = (String) ((DBField) getField(SchemaConstants.UserUserName)).getValue();

	if (name != null)
	  {
	    result.addRow(null, name);
	  }

	// and any aliases defined

	Vector values = ((DBField) getField((short)267)).getValues();

	for (int i = 0; i < values.size(); i++)
	  {
	    result.addRow(null, (String) values.elementAt(i));
	  }

	return result;
	
      default:
	return super.obtainChoiceList(field);
      }
  }

  public synchronized boolean finalizeSetValue(DBField field, Object value)
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

    if (field.getID() == SchemaConstants.UserUserName)
      {
	// signature alias field will need to be rescanned,
	// but we don't need to do the persona rename stuff.

	sf = (StringDBField) getField(SchemaConstants.UserUserName); // old user name

	oldName = (String) sf.getValueLocal();

	if (oldName != null)
	  {
	    sf = (StringDBField) getField((short) 268); // signature alias

	    // if the signature alias was the user's name, we'll want
	    // to continue that.
		
	    if (oldName.equals((String) sf.getValueLocal()))
	      {
		sf.setValue(value);	// set the signature alias to the user's new name
	      }
	  }

	inv = (InvidDBField) getField(SchemaConstants.UserAdminPersonae);
	
	if (inv == null)
	  {
	    return true;
	  }

	// rename all the associated persona with the new user name

	personaeInvids = inv.getValues();
	
	for (int i = 0; i < personaeInvids.size(); i++)
	  {
	    eobj = session.editDBObject((Invid) personaeInvids.elementAt(i));

	    sf = (StringDBField) eobj.getField(SchemaConstants.PersonaNameField);
	    oldName = (String) sf.getValue();
	    oldNames.addElement(oldName);
	    suffix = oldName.substring(oldName.indexOf(':'));
	    
	    ReturnVal retVal = sf.setValue(value + ":" + suffix);

	    if ((retVal != null) && (!retVal.didSucceed()))
	      {
		if (okay)
		  {
		    return false;
		  }
		else
		  {
		    // crap.  we've changed at least one persona
		    // object, but we can't change all of them.  So,
		    // let's try our best to undo what we did.

		    for (int j = 0; j < i; j++)
		      {
			eobj = session.editDBObject((Invid) personaeInvids.elementAt(j));

			sf = (StringDBField) eobj.getField(SchemaConstants.PersonaNameField);
			sf.setValue(oldNames.elementAt(j));
		      }

		    return false;
		  }
	      }
	    else
	      {
		okay = false;	// we've made a change, and we can't just return false
	      }
	  }
      }

    return true;
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    userWizard wizard;

    /* -- */

    if ((field.getID() != SchemaConstants.UserUserName) ||
	(operation != SETVAL))
      {
	return null;		// by default, we just ok whatever
      }

    // looks like we're renaming this user

    if (gSession.isWizardActive() && gSession.getWizard() instanceof userWizard)
      {
	wizard = (userWizard) gSession.getWizard();

	if ((wizard.getState() == wizard.DONE) &&
	    (wizard.operation == wizard.USER_RENAME) &&
	    (wizard.field == field) &&
	    (wizard.object == this) &&
	    (wizard.param == param1))
	  {
	    // ok, assume the wizard has taken care of getting everything prepped and
	    // approved for us.  An active wizard has approved the operation
		
	    wizard.unregister();
		
	    return null;
	  }
	else
	  {
	    if (wizard.field != field)
	      {
		System.err.println("userCustom.wizardHook(): bad field");
	      }

	    if (wizard.operation != wizard.USER_RENAME)
	      {
		System.err.println("userCustom.wizardHook(): bad operation");
	      }

	    if (wizard.object != this)
	      {
		System.err.println("userCustom.wizardHook(): bad object");
	      }

	    if (wizard.param != param1)
	      {
		System.err.println("userCustom.wizardHook(): bad param");
	      }

	    if (wizard.getState() != wizard.DONE)
	      {
		System.err.println("userCustom.wizardHook(): bad state: " + wizard.getState());
	      }

	    wizard.unregister();
	    return Ganymede.createErrorDialog("User Object Error",
					      "The client is attempting to do an operation on " +
					      "a user object with an active wizard.");
	  }
      }
    else
      {
	// there's no wizard active, and this operation has to be approved by one.  Go ahead
	// and set up the wizard and let the client play with it.

	try
	  {
	    wizard = new userWizard(this.gSession,
				    userWizard.USER_RENAME,
				    this,
				    field,
				    param1);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't create userWizard " + ex.getMessage());
	  }
	
	// if we get here, the wizard was able to register itself.. go ahead
	// and return the initial dialog for the wizard.  The ReturnVal code
	// that wizard.getStartDialog() returns will have the success code
	// set to false, so whatever triggered us will prematurely exit,
	// returning the wizard's dialog.

	return wizard.getStartDialog();
      }
  }



}
