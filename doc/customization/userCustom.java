/*

   userCustom.java

   This file is a management class for user objects in Ganymede.
   
   Created: 30 July 1997
   Version: %I% %D%
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

  static String mailsuffix = null;
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
    boolean success = true;

    /* -- */

    // we don't want to do any of this initialization during
    // bulk-loading.

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

    if (retVal != null && !retVal.didSucceed())
      {
	success = false;
      }

    // create a volume entry for the user.
    
    InvidDBField invf = (InvidDBField) getField(userSchema.VOLUMES);

    retVal = invf.createNewEmbedded(true);
    
    if ((retVal == null) || (!retVal.didSucceed()))
      {
	return false;
      }
    
    Invid invid = retVal.getInvid();
    
    if (invid != null)
      {
	// find the auto.home.default map, if we can.

	Vector results = getGSession().internalQuery(new Query((short) 277, 
							       new QueryDataNode(QueryDataNode.EQUALS,
										 "auto.home.default")));
    
	// if we found auto.home.default, set the new volume entry map
	// field to point to auto.home.default.
    
	if (results != null && results.size() == 1)
	  {
	    Result objid = (Result) results.elementAt(0);
	
	    DBEditObject eObj = getSession().editDBObject(invid);
	    invf = (InvidDBField) eObj.getField(mapEntrySchema.MAP);
	
	    retVal = invf.setValueLocal(objid.getInvid());

	    if (retVal != null && !retVal.didSucceed())
	      {
		success = false;
	      }
	
	    // we want the permissions system to reject edit privs
	    // on this now.. by setting permCache to null, we allow
	    // the mapEntryCustom permOverride method to get a chance
	    // to refuse edit privileges.
	
	    invf.clearPermCache();	// *sync*
	  }
      }

    return success;
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
    ReturnVal retVal;

    /* -- */

    if (field.getID() == VOLUMES)	// auxiliary volume mappings
      {
	fieldDef = field.getFieldDef();

	if (fieldDef.getTargetBase() > -1)
	  {
	    newObject = getSession().createDBObject(fieldDef.getTargetBase(),
						    null, null);
	    return newObject.getInvid();
	  }
	else
	  {
	    throw new RuntimeException("error in schema.. interface field target base not restricted..");
	  }
      }
    else
      {
	return null;		// default
      }
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    if (object.isInactivated())
      {
	switch (fieldid)
	  {
	  case userSchema.USERNAME:
	  case userSchema.UID:
	  case userSchema.LOGINSHELL:
	  case userSchema.HOMEDIR:
	  case userSchema.VOLUMES:
	    return true;
	  }
      }
    else
      {
	switch (fieldid)
	  {
	  case userSchema.USERNAME:
	  case userSchema.PASSWORD:
	  case userSchema.SIGNATURE:
	  case userSchema.EMAILTARGET:
	  case userSchema.UID:
	  case userSchema.LOGINSHELL:
	  case userSchema.HOMEDIR:
	  case userSchema.VOLUMES:
	    return true;
	  }
      }

    return false;
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canBeInactivated()
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the user has permission
   * to inactivate a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return true;
  }

  /**
   * This method handles inactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * inactivate() method can then be called on the object to put the
   * object into inactive mode.  inactivate() will set the object's
   * removal date and fix up any other state information to reflect
   * the object's inactive status.<br><br>
   *
   * inactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to inactive the object.<br><br>
   *
   * The inactive() method can cause other objects to be deleted, can cause
   * strings to be removed from fields in other objects, whatever.<br><br>
   *
   * If remove() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, then DBSEssion.inactivateDBObject() method will rollback any changes
   * made by this method.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's inactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling finalizeInactivate() with a boolean
   * indicating ultimate success of the operation.<br><br>
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object inactivation when
   * the transaction is committed..
   *
   * @param interactive If true, the inactivate() logic can present
   * a wizard to the client to customize the inactivation logic.
   *
   * @see #commitPhase1()
   * @see #commitPhase2() 
   */

  public ReturnVal inactivate()
  {
    return inactivate(null, false);
  }

  public ReturnVal inactivate(String forward, boolean calledByWizard)
  {
    ReturnVal retVal;
    StringDBField stringfield;
    PasswordDBField passfield;
    DateDBField date;
    Calendar cal = Calendar.getInstance(); 
    Date time;

    /* -- */

    if (!gSession.enableWizards || calledByWizard)
      {
	// ok, we want to null the password field and set the
	// removal time to current time + 3 months.

	passfield = (PasswordDBField) getField(userSchema.PASSWORD);
	retVal = passfield.setCryptPass(null); // we know our schema uses crypted pass'es

	if (retVal != null && !retVal.didSucceed())
	  {
	    finalizeInactivate(false);
	    return retVal;
	  }

	// set the shell to /bin/false
	
	stringfield = (StringDBField) getField(LOGINSHELL);
	retVal = stringfield.setValueLocal("/bin/false");

	if (retVal != null && !retVal.didSucceed())
	  {
	    finalizeInactivate(false);
	    return retVal;
	  }

	// reset the forwarding address?

	if (forward != null)
	  {
	    stringfield = (StringDBField) getField(EMAILTARGET);
	
	    while (stringfield.size() > 0)
	      {
		retVal = stringfield.deleteElement(0);
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    finalizeInactivate(false);
		    return retVal;
		  }
	      }

	    stringfield.addElement(forward);
	  }

	// make sure that the expiration date is cleared.. we're on
	// the removal track now.

	date = (DateDBField) getField(SchemaConstants.ExpirationField);
	retVal = date.setValueLocal(null);

	if (retVal != null && !retVal.didSucceed())
	  {
	    finalizeInactivate(false);
	    return retVal;
	  }

	// determine what will be the date 3 months from now

	time = new Date();
	cal.setTime(time);
	cal.add(Calendar.MONTH, 3);

	// and set the removal date

	date = (DateDBField) getField(SchemaConstants.RemovalField);
	retVal = date.setValueLocal(cal.getTime());

	if (retVal != null && !retVal.didSucceed())
	  {
	    finalizeInactivate(false);
	    return retVal;
	  }

	finalizeInactivate(true);

	// ok, we succeeded, now we have to tell the client
	// what to refresh to see the inactivation results

	ReturnVal result = new ReturnVal(true);

	result.addRescanField(this.getInvid(), SchemaConstants.RemovalField);
	result.addRescanField(this.getInvid(), userSchema.LOGINSHELL);
	result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	return result;
      }
    else  // interactive, but not called by wizard
      {
	userInactivateWizard theWiz;

	try
	  {
	    if (debug)
	      {
		System.err.println("userCustom: creating inactivation wizard");
	      }

	    theWiz = new userInactivateWizard(this.gSession, this);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("oops, userCustom couldn't create wizard for remote ex " + ex); 
	  }

	if (debug)
	  {
	    System.err.println("userCustom: returning inactivation wizard");
	  }

	return theWiz.getStartDialog();
      }
  }

  /**
   * This method handles reactivation logic for this object type.  A
   * DBEditObject must first be checked out for editing, then the
   * reactivate() method can then be called on the object to make the
   * object active again.  reactivate() will clear the object's
   * removal date and fix up any other state information to reflect
   * the object's reactive status.<br><br>
   *
   * reactive() is designed to run synchronously with the user's
   * request for inactivation.  It can return a wizard reference
   * in the ReturnVal object returned, to guide the user through
   * a set of interactive dialogs to reactive the object.<br>
   *
   * If reactivate() returns a ReturnVal that has its success flag set to false
   * and does not include a JDialogBuff for further interaction with the
   * user, then DBSEssion.inactivateDBObject() method will rollback any changes
   * made by this method.<br><br>
   *
   * IMPORTANT NOTE: If a custom object's reactivate() logic decides
   * to enter into a wizard interaction with the user, that logic is
   * responsible for calling editset.rollback("reactivate" +
   * getLabel()) in the case of a failure to properly do all the reactivation
   * stuff, where getLabel() must be the name of the object
   * prior to any attempts to clear fields which could impact the
   * returned label.<br><br>
   *
   * Finally, it is up to commitPhase1() and commitPhase2() to handle
   * any external actions related to object reactivation when
   * the transaction is committed..
   *
   * @see arlut.csd.ganymede.DBEditObject#commitPhase1()
   * @see arlut.csd.ganymede.DBEditObject#commitPhase2()
   */

  public ReturnVal reactivate()
  {
    userReactivateWizard theWiz;

    /* -- */

    try
      {
	if (debug)
	  {
	    System.err.println("userCustom: creating reactivation wizard");
	  }
	
	theWiz = new userReactivateWizard(this.gSession, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("oops, userCustom couldn't create wizard for remote ex " + ex); 
      }

    if (debug)
      {
	System.err.println("userCustom: returning reactivation wizard");
      }
    
    return theWiz.getStartDialog();
  }

  /**
   * This method is called by the userReactivateWizard on successfully
   * obtaining the necessary information from the client on a
   * reactivate operation.  We then do the actual work to reactivate
   * the user in this method.
   * 
   * @see arlut.csd.ganymede.custom.userReactivateWizard
   *
   */

  public ReturnVal reactivate(userReactivateWizard reactivateWizard)
  {
    ReturnVal retVal = null;
    StringDBField stringfield;
    PasswordDBField passfield;
    DateDBField date;

    /* -- */

    if (reactivateWizard != null)
      {
	// reset the password

	if (reactivateWizard.password != null && reactivateWizard.password.length() != 0)
	  {
	    passfield = (PasswordDBField) getField(userSchema.PASSWORD);
	    retVal = passfield.setPlainTextPass(reactivateWizard.password);
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		finalizeReactivate(false);
		return retVal;
	      }
	  }
	else
	  {
	    retVal = new ReturnVal(false);

	    JDialogBuff dialog = new JDialogBuff("Reactivate User",
						 "You must set a password",
						 "OK",
						 "Cancel",
						 "question.gif");
	    dialog.addPassword("New Password", true);

	    updateShellChoiceList();
	    dialog.addChoice("Shell", userCustom.shellChoices.getLabels());

	    dialog.addString("Forwarding Address");
	    
	    retVal.setDialog(dialog);
	    retVal.setCallback(reactivateWizard);
	    reactivateWizard.state = 2;	// make sure ths wizard will be ready to process this
	
	    return retVal;
	  }

	// reset the shell

	if (reactivateWizard.shell != null)
	  {
	    stringfield = (StringDBField) getField(LOGINSHELL);
	    retVal = stringfield.setValue(reactivateWizard.shell);
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		finalizeReactivate(false);
		return retVal;
	      }
	  }

	// reset the forwarding address

	if (reactivateWizard.forward != null)
	  {
	    stringfield = (StringDBField) getField(EMAILTARGET);
	
	    while (stringfield.size() > 0)
	      {
		retVal = stringfield.deleteElement(0);
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    finalizeReactivate(false);
		    return retVal;
		  }
	      }

	    stringfield.addElement(reactivateWizard.forward);
	  }

	// make sure that the removal date is cleared..

	date = (DateDBField) getField(SchemaConstants.RemovalField);
	retVal = date.setValueLocal(null);

	if (retVal != null && !retVal.didSucceed())
	  {
	    finalizeReactivate(false);
	    return retVal;
	  }

	finalizeReactivate(true);

	// ok, we succeeded, now we have to tell the client
	// what to refresh to see the reactivation results

	ReturnVal result = new ReturnVal(true);

	result.addRescanField(this.getInvid(), SchemaConstants.RemovalField);
	result.addRescanField(this.getInvid(), userSchema.LOGINSHELL);
	result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	return result;
      }

    return Ganymede.createErrorDialog("userCustom.reactivate() error",
				      "Error, reactivate() called without a valid user wizard");
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
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given string field can only
   * choose from a choice provided by obtainChoiceList()
   *
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == SIGNATURE)
      {
	// we want to force signature alias choosing

	return true;
      }

    return super.mustChoose(field);
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
	
      case SIGNATURE:			// signature alias

	QueryResult result = new QueryResult();

	/* -- */

	// our list of possible aliases includes the user's name

	// note that we first check the new value, if any, for the
	// user name.. this way the user rename code can change the
	// signature alias without having the StringDBField for the
	// signature alias reject the new name.

	String name = (String) ((DBField) getField(USERNAME)).getNewValue();

	if (name != null)
	  {
	    result.addRow(null, name, false);
	  }
	else
	  {
	    name = (String) ((DBField) getField(USERNAME)).getValue();

	    if (name != null)
	      {
		result.addRow(null, name, false);
	      }
	  }

	// and any aliases defined

	Vector values = ((DBField) getField(ALIASES)).getValues();

	for (int i = 0; i < values.size(); i++)
	  {
	    result.addRow(null, (String) values.elementAt(i), false);
	  }

	return result;
	
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
	    if (debug)
	      {
		System.err.println("userCustom - updateShellChoiceList()");
	      }

	    shellChoices = new QueryResult();

	    Query query = new Query("Shell Choice", null, false);

	    // internalQuery doesn't care if the query has its filtered bit set
	    
	    if (debug)
	      {
		System.err.println("userCustom - issuing query");
	      }

	    Vector results = internalSession().internalQuery(query);
	    
	    if (debug)
	      {
		System.err.println("userCustom - processing query results");
	      }
	
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
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given field, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.<br><br>
   *
   * This method should be used very sparingly.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    if (fieldid != UID)
      {
	return null;
      }

    // we don't want to allow anyone other than supergash to change our
    // uid once it is set.

    if (object.getFieldValueLocal(UID) != null)
      {
	return new PermEntry(true, false, true, false);
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * This method is called after the set value operation has been ok'ed
   * by any appropriate wizard code.
   *
   */

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

    // we don't want to allow the home directory to be changed except
    // by when the username field is being changed.

    if (field.getID() == HOMEDIR)
      {
	String dir = (String) value;

	/* -- */

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
			return false;
		      }
		  }
	      }
	  }

	return true;
      }

    // when we rename a user, we have lots to do.. a number of other
    // fields in this object and others need to be updated to match.

    if (field.getID() == USERNAME)
      {
	// if we are being told to clear the user name field, go ahead and
	// do it.. we assume this is being done by user removal logic,
	// so we won't press the issue.
	
	if (deleting && (value == null))
	  {
	    return true;
	  }

	// signature alias field will need to be rescanned,
	// but we don't need to do the persona rename stuff.

	sf = (StringDBField) getField(USERNAME); // old user name

	oldName = (String) sf.getValueLocal();

	if (oldName != null)
	  {
	    sf = (StringDBField) getField(SIGNATURE); // signature alias

	    // if the signature alias was the user's name, we'll want
	    // to continue that.
		
	    if (oldName.equals((String) sf.getValueLocal()))
	      {
		sf.setValueLocal(value); // set the signature alias to the user's new name
	      }
	  }

	// update the home directory location.. we assume that if
	// the user has permission to rename the user, they can
	// automatically execute this change to the home directory.

	if (homedir == null)
	  {
	    homedir = System.getProperty("ganymede.homedirprefix");
	  }

	// do we have a homedir prefix?  if so, set the home dir here

	if (homedir != null && homedir.length() != 0)
	  {
	    sf = (StringDBField) getField(HOMEDIR);

	    sf.setValueLocal(homedir + (String) value);	// ** ARL
	  }

	// if we don't have a signature set, set it to the username.

	sf = (StringDBField) getField(SIGNATURE);

	String sigVal = (String) sf.getValueLocal();

	if (sigVal == null || sigVal.equals(oldName))
	  {
	    sf.setValueLocal(value);
	  }

	// update the email target field.  We want to look for
	// oldName@arlut.utexas.edu and replace it if we find it.

	sf = (StringDBField) getField(EMAILTARGET);

	if (mailsuffix == null)
	  {
	    mailsuffix = System.getProperty("ganymede.defaultmailsuffix");
	  }

	if (mailsuffix == null)
	  {
	    Ganymede.debug("Error in userCustom: couldn't find property ganymede.defaultmailsuffix!");
	  }

	String oldMail = oldName + mailsuffix;

	if (sf.containsElement(oldMail))
	  {
	    sf.deleteElement(oldMail);
	    sf.addElement(value + mailsuffix);
	  }
	else if (sf.size() == 0)
	  {
	    sf.addElement(value + mailsuffix);
	  }
	
	inv = (InvidDBField) getField(PERSONAE);
	
	if (inv == null)
	  {
	    return true;
	  }

	// rename all the associated personae with the new user name

	personaeInvids = inv.getValues();

	String tempString;
	
	for (int i = 0; i < personaeInvids.size(); i++)
	  {
	    eobj = session.editDBObject((Invid) personaeInvids.elementAt(i));

	    sf = (StringDBField) eobj.getField(SchemaConstants.PersonaNameField);
	    oldName = (String) sf.getValue();
	    oldNames.addElement(oldName);
	    suffix = oldName.substring(oldName.indexOf(':')+1);
	    
	    tempString = value + ":" + suffix;

	    if (debug)
	      {
		System.err.println("trying to rename admin persona " + oldName + " to "+ tempString);
	      }

	    ReturnVal retVal = sf.setValueLocal(tempString);

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
			sf.setValueLocal(oldNames.elementAt(j));
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
	System.err.println("userCustom ** entering wizardHook, field = " + 
			   field.getName() + ", op= " + operation);
      }

    try
      {
	// if we are changing the list of email aliases, we'll want
	// to update the list of choices for the signature field.

	if (field.getID() == ALIASES)
	  {
	    // the second true in the ReturnVal constructor makes the
	    // Ganymede logic go ahead and complete the operation
	    // normally, just taking the rescan information as an
	    // extra to pass back to the client.

	    result = new ReturnVal(true, true);	
	    
	    result.addRescanField(this.getInvid(), userSchema.SIGNATURE);

	    return result;
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

		if (debug)
		  {
		    System.err.println("userCustom: deleting group element " + 
				       gSession.viewObjectLabel(delVal));
		  }

		if (!delVal.equals(getFieldValueLocal(HOMEGROUP)))
		  {
		    // whew, no big deal.. they are not removing the
		    // home group.  The client will need to rescan,
		    // but no biggie.

		    if (debug)
		      {
			System.err.println("userCustom: I don't think " + 
					   gSession.viewObjectLabel(delVal) + 
					   " is the home group");
		      }

		    result = new ReturnVal(true, true);
		    result.addRescanField(this.getInvid(), HOMEGROUP);
		    groupChoices = null;
		    return result;
		  }

		if (gSession.isWizardActive() && 
		    gSession.getWizard() instanceof userHomeGroupDelWizard)
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
			    System.err.println("userCustom.wizardHook(): bad state: " + 
					       groupWizard.getState());
			  }

			groupWizard.unregister();

			return Ganymede.createErrorDialog("User Object Error",
							  "The client is attempting to do an operation on " +
							  "a user object with an active wizard.");
		      }
		  }
		else if (gSession.isWizardActive() && 
			 !(gSession.getWizard() instanceof userHomeGroupDelWizard))
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
		// that wizard.getStartDialog() returns will have the success code
		// set to false, so whatever triggered us will prematurely exit,
		// returning the wizard's dialog.
	    
		return groupWizard.getStartDialog();
	      }
	  }

	if ((field.getID() != USERNAME) ||
	    (operation != SETVAL))
	  {
	    return null;		// by default, we just ok whatever else
	  }

	// ok, we're doing a user rename.. check to see if we need to do a
	// wizard
    
	// If this is a newly created user, we won't pester them about setting
	// or changing the user name field.

	if ((field.getValue() == null) || (getStatus() == ObjectStatus.CREATING))
	  {
	    result = new ReturnVal(true, true); // have setValue() do the right thing

	    result.addRescanField(this.getInvid(), userSchema.HOMEDIR);
	    result.addRescanField(this.getInvid(), userSchema.ALIASES);
	    result.addRescanField(this.getInvid(), userSchema.SIGNATURE);
	    result.addRescanField(this.getInvid(), userSchema.VOLUMES);
	    result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	    return result;
	  }

	String oldname = (String) field.getValue();

	if (!gSession.enableWizards)
	  {
	    return null;		// no wizards if the user is non-interactive.
	  }

	// Huh!  Wizard time!  We'll check here to see if there is a
	// registered userRenameWizard in the system taking care of us.

	if (gSession.isWizardActive() && gSession.getWizard() instanceof userRenameWizard)
	  {
	    renameWizard = (userRenameWizard) gSession.getWizard();

	    if ((renameWizard.getState() == renameWizard.DONE) &&
		(renameWizard.field == field) &&
		(renameWizard.userObject == this) &&
		(renameWizard.newname == param1))
	      {
		// ok, assume the wizard has taken care of getting
		// everything prepped and approved for us.  An active
		// wizard has approved the operation
		
		renameWizard.unregister();

		// note that we don't have to return the rescan fields
		// directive here.. the active wizard is what is going to
		// respond directly to the user, we are presumably just
		// here because the wizard task-completion code went ahead
		// and called setValue on the user's name.. we'll trust
		// that code to return the rescan indicators.
		
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
		    System.err.println("userCustom.wizardHook(): bad state: " + 
				       renameWizard.getState());
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
		// Mike Jittlov is the Wizard of Speed and Time

		renameWizard = new userRenameWizard(this.gSession,
						    this,
						    field,
						    (String) param1,
						    oldname);
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

	    return renameWizard.getStartDialog();
	  }
      }
    finally
      {
	if (debug)
	  {
	    System.err.println("userCustom ** exiting wizardHook");
	  }
      }
  }

  /**
   *
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.<br><br>
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server, the process invokation should be placed here,
   * rather than in commitPhase1().<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.DBEditSet
   */

  public void commitPhase2()
  {
    switch (getStatus())
      {
      case DROPPING:
	// the user never really existed.. no external actions required.
	break;

      case CREATING:

	// handle creating the user.. creating their home directory, setting
	// up their mail spool, etc., etc.

	createUserExternals();
	break;

      case DELETING:
	deleteUserExternals();
	break;
	
      case EDITING:

	// did the user's name change?

	String name = getLabel();
	String oldname = original.getLabel();

	if (!name.equals(oldname))
	  {
	    handleUserRename();
	  }

	// did we change home directory volumes?

	Invid volumeId;
	String volumeName;
	DBObject mapEntry;
	Invid mapInvid;

	Vector newEntries = getFieldValuesLocal(userSchema.VOLUMES);
	Vector oldEntries = original.getFieldValuesLocal(userSchema.VOLUMES);

	int newSize;
	int oldSize;

	if (newEntries != null)
	  {
	    newSize = newEntries.size() + 1;
	  }
	else
	  {
	    newSize = 0;
	  }

	if (oldEntries != null)
	  {
	    oldSize = oldEntries.size() + 1;
	  }
	else
	  {
	    oldSize = 0;
	  }

	Hashtable newVolumes = new Hashtable(newSize, 1.0f);
	Hashtable oldVolumes = new Hashtable(oldSize, 1.0f);

	Vector addedVolumes = new Vector();
	Vector deletedVolumes = new Vector();

	if (oldEntries != null)
	  {
	    for (int i = 0; i < oldEntries.size(); i++)
	      {
		mapInvid = (Invid) oldEntries.elementAt(i);
		mapEntry = (DBObject) getSession().viewDBObject(mapInvid);
		
		volumeId = (Invid) mapEntry.getFieldValueLocal(mapEntrySchema.VOLUME);
		volumeName = gSession.viewObjectLabel(volumeId);
		
		oldVolumes.put(volumeId, volumeName);
	      }
	  }

	if (newEntries != null)
	  {
	    for (int i = 0; i < newEntries.size(); i++)
	      {
		mapInvid = (Invid) newEntries.elementAt(i);
		mapEntry = (DBObject) getSession().viewDBObject(mapInvid);
		
		volumeId = (Invid) mapEntry.getFieldValueLocal(mapEntrySchema.VOLUME);
		volumeName = gSession.viewObjectLabel(volumeId);
		
		newVolumes.put(volumeId, volumeName);
		
		if (!oldVolumes.containsKey(volumeId))
		  {
		    addedVolumes.addElement(volumeName);
		  }
	      }
	  }

	Enumeration oldValues = oldVolumes.keys();

	while (oldValues.hasMoreElements())
	  {
	    Object key = oldValues.nextElement();

	    if (!newVolumes.containsKey(key))
	      {
		deletedVolumes.addElement(oldVolumes.get(key));
	      }
	  }

	// okay, addedVolumes and deletedVolumes have a list of changes..

	if (addedVolumes.size() != 0 && deletedVolumes.size() != 0)
	  {
	    handleUserDirectoryChange(addedVolumes, deletedVolumes);
	  }
      }

    return;
  }

  /**
   *
   * This method runs from userCustom's commitPhase2() and runs an external
   * script that can create the user's home directory, and anything else
   * that might need doing.
   *
   */

  private void createUserExternals()
  {
    if (debug)
      {
	System.err.println("userCustom: " + getLabel() + ", in createUserExternals().");
      }
  }

  /**
   *
   * This method runs from userCustom's commitPhase2() and runs an external
   * script that can do whatever bookkeeping might be desired when a user
   * is taken out of the passwd/user_info file generated by Ganymede.  This
   * may include removing the user's mailbox, home directory, and files, or
   * simply notifying someone that the user is no longer valid.
   *
   */

  private void deleteUserExternals()
  {
    if (debug)
      {
	System.err.println("userCustom: " + getLabel() + ", in deleteUserExternals().");
      }
  }

  /**
   *
   * This method takes care of executing whatever external code is required
   * to handle this user being moved from volume to volume
   *
   */

  private void handleUserDirectoryChange(Vector addedVolumes, Vector deletedVolumes)
  {
    if (debug)
      {
	System.err.println("userCustom.handleUserDirectoryChange(): user " + getLabel());
	
	if (addedVolumes != null)
	  {
	    System.err.print("has been added to");

	    for (int i = 0; i < addedVolumes.size(); i++)
	      {
		System.err.print(" ");
		System.err.print(addedVolumes.elementAt(i));
	      }

	    System.err.println();
	  }

	if (addedVolumes != null)
	  {
	    System.err.print("has been deleted from");

	    for (int i = 0; i < deletedVolumes.size(); i++)
	      {
		System.err.print(" ");
		System.err.print(deletedVolumes.elementAt(i));
	      }

	    System.err.println();
	  }
      }
  }

  private void handleUserRename()
  {
    if (debug)
      {
	System.err.println("userCustom.handleUserRename(): user " + original.getLabel() +
			   "has been renamed to " + getLabel());
      }
  }
}
