/*

   userCustom.java

   This file is a management class for user objects in Ganymede.
   
   Created: 30 July 1997
   Release: $Name:  $
   Version: $Revision: 1.106 $
   Last Mod Date: $Date: 2002/02/28 21:00:10 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.FileOps;

import java.util.*;
import java.io.*;
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

  static String renameFilename = null;
  static File renameHandler = null;

  static String createFilename = null;
  static File createHandler = null;

  static String deleteFilename = null;
  static File deleteHandler = null;

  static final int lowUID = 1001;

  static int file_identifier = 0;

  public static synchronized int getNextAuthIdent()
  {
    if (file_identifier == Integer.MAX_VALUE)
      {
	file_identifier = 0;
      }
    else
      {
	file_identifier++;
      }

    return file_identifier;
  }

  public static File getNextFileName()
  {
    return new File("/tmp/ganymede_ext_validate_" + getNextAuthIdent());
  }

  // ---

  QueryResult groupChoices = null;

  String newUsername = null;

  private boolean amChangingExpireDate = false;

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
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.  If this DBEditObject is an embedded type, it will
   * have been linked into its parent object before this method
   * is called.</p>
   *
   * <p>This method is responsible for filling in any default
   * values that can be calculated from the 
   * {@link arlut.csd.ganymede.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.DBSession#createDBObject(short, arlut.csd.ganymede.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure code, the
   * calling method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.</p>
   *
   * <p>If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.</p>
   *
   * <p>This method should be overridden in subclasses.</p> 
   */

  public ReturnVal initializeNewObject()
  {
    ReturnVal retVal;
    Random rand = new Random();
    Integer uidVal = null;

    /* -- */

    // we don't want to do any of this initialization during
    // bulk-loading.

    if (!getGSession().enableOversight)
      {
	return null;
      }

    // need to find a uid for this user

    // see if we have an owner set, check it for our starting uid

    Vector owners = getFieldValuesLocal(SchemaConstants.OwnerListField);

    /*

    if (owners != null && owners.size() > 0)
      {
	Invid primaryOwner = (Invid) owners.elementAt(0);

	DBObject owner = getSession().viewDBObject(primaryOwner);

	if (owner != null)
	  {
	    // field 256 in the owner group is the GASHARL starting
	    // uid/gid

	    uidVal = (Integer) owner.getFieldValueLocal((short) 256);

	    if (uidVal == null)
	      {
		uidVal = new Integer(lowUID);
	      }
	  }
      }

    */

    NumericDBField numField = (NumericDBField) getField(UID);

    if (numField == null)
      {
	return Ganymede.createErrorDialog("User Initialization Failure",
					  "Couldn't find the uid field.. schema problem?");
      }

    DBNameSpace namespace = numField.getNameSpace();

    if (namespace == null)
      {
	return Ganymede.createErrorDialog("User Initialization Failure",
					  "Couldn't find the uid namespace.. schema problem?");
      }

    // now, find a uid.. unfortunately, we have to use immutable Integers here.. not
    // the most efficient at all.

    int count = 0;
    uidVal = new Integer(rand.nextInt(31767) + lowUID);
    
    while (!namespace.reserve(getEditSet(), uidVal, true) && count < 30000)
      {
	uidVal = new Integer(rand.nextInt(31767) + lowUID);
	count++;
      }

    if (count > 30000)
      {
	// we've been looping too long, maybe there's no
	// uid's free?  let's do an exhaustive search
	
	uidVal = new Integer(lowUID);
	
	while (!namespace.reserve(getEditSet(), uidVal, true))
	  {
	    uidVal = new Integer(uidVal.intValue() + 1);
	    
	    if (uidVal.intValue() > 32767)
	      {
		throw new RuntimeException("Couldn't find an allocatable uid through random search");
	      }
	  }
      }

    // we use setValueLocal so we can set a value that the user can't edit.

    retVal = numField.setValueLocal(uidVal);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // create a volume entry for the user.
    
    InvidDBField invf = (InvidDBField) getField(userSchema.VOLUMES);

    retVal = invf.createNewEmbedded(true);
    
    if ((retVal == null) || (!retVal.didSucceed()))
      {
	return retVal;
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
		return retVal;
	      }
	
	    // we want the permissions system to reject edit privs
	    // on this now.. by setting permCache to null, we allow
	    // the mapEntryCustom permOverride method to get a chance
	    // to refuse edit privileges.

	    eObj.clearFieldPerm(mapEntrySchema.MAP);
	  }
      }

    return null;
  }

  /**
   * <p>This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.</p>
   *
   * <p>This method does not affect those fields which are actually present
   * in the object's record in the
   * {@link arlut.csd.ganymede.DBStore DBStore}.  What this method allows
   * you to do is have a subclass decide whether it wants to instantiate
   * a potential field (one that is declared in the field dictionary for
   * this object, but which doesn't happen to be presently defined in
   * this object) in this particular object.</p>
   *
   * <p>A concrete example will help here.  The Permissions Object type
   * (base number SchemaConstants.PermBase) holds a permission
   * matrix, a descriptive title, and a list of admin personae that hold
   * those permissions for objects they own.</p>
   *
   * <p>There are a few specific instances of SchemaConstants.PermBase
   * that don't properly need the list of admin personae, as their
   * object invids are hard-coded into the Ganymede security system, and
   * their permission matrices are automatically consulted in certain
   * situations.  In order to support this, we're going to want to have
   * a DBEditObject subclass for managing permission objects.  In that
   * subclass, we'll define instantiateNewField() so that it will return
   * false if the fieldID corresponds to the admin personae list if the
   * object's ID is that of one of these special objects.  As a result,
   * when the objects are viewed by an administrator, the admin personae
   * list will not be seen.</p>
   */

  public boolean instantiateNewField(short fieldID)
  {
    if (fieldID == userSchema.PASSWORDCHANGETIME)
      {
	return true;
      }

    return super.instantiateNewField(fieldID);
  }

  /**
   * <p>Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canCloneField(DBSession session, DBObject object, DBField field)
  {
    short fieldid = field.getID();

    switch (fieldid)
      {
      case USERNAME:
      case UID:
      case CATEGORY:		// would trigger wizard, otherwise
      case userSchema.PASSWORD:
      case HOMEDIR:
      case PERSONAE:
      case ALIASES:
      case SIGNATURE:
      case EMAILTARGET:
      case PASSWORDCHANGETIME:
	return false;
      }
    
    return super.canCloneField(session, object, field);
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.</p>
   */

  public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    if (debug)
      {
	System.err.println("Attempting to clone User " + origObj.getLabel());
      }

    boolean problem = false;
    ReturnVal tmpVal;
    StringBuffer resultBuf = new StringBuffer();
    ReturnVal retVal = super.cloneFromObject(session, origObj, local);

    if (retVal != null && retVal.getDialog() != null)
      {
	resultBuf.append("\n\n");
	resultBuf.append(retVal.getDialog().getText());
	
	problem = true;
      }

    // we have the default canCloneField() refuse to clone
    // userSchema.CATEGORY to avoid dealing or bypassing with the
    // wizard.  If we are cloning a normal user, it is safe enough to
    // copy that value.  Else we'll leave it blank for the user to
    // set.

    Invid category = (Invid) origObj.getFieldValue(userSchema.CATEGORY);
	
    if (session.getGSession().viewObjectLabel(category).equals("normal"))
      {
	((DBField) getField(userSchema.CATEGORY)).setValue(category, local, true);
      }
    
    if (debug)
      {
	System.err.println("User " + origObj.getLabel() + " cloned, working on embeddeds");
      }

    // and clone the embedded objects

    InvidDBField newVolumes = (InvidDBField) getField(userSchema.VOLUMES);
    InvidDBField oldVolumes = (InvidDBField) origObj.getField(userSchema.VOLUMES);

    Vector newOnes;
    Vector oldOnes;

    if (local)
      {
	newOnes = (Vector) newVolumes.getValuesLocal().clone();
	oldOnes = (Vector) oldVolumes.getValuesLocal().clone();
      }
    else
      {
	newOnes = newVolumes.getValues();
	oldOnes = oldVolumes.getValues();
      }

    DBObject origVolume;
    DBEditObject workingVolume;
    int i;

    for (i = 0; i < newOnes.size(); i++)
      {
	if (debug)
	  {
	    System.err.println("User clone sub " + i);
	  }

	workingVolume = (DBEditObject) session.editDBObject((Invid) newOnes.elementAt(i));
	origVolume = session.viewDBObject((Invid) oldOnes.elementAt(i));

	if (debug)
	  {
	    System.err.println("Attempting to clone user volume " + origVolume.getLabel());
	  }

	tmpVal = workingVolume.cloneFromObject(session, origVolume, local);

	if (tmpVal != null && tmpVal.getDialog() != null)
	  {
	    resultBuf.append("\n\n");
	    resultBuf.append(tmpVal.getDialog().getText());
	    
	    problem = true;
	  }
      }

    Invid newInvid;

    if (i < oldOnes.size())
      {
	for (; i < oldOnes.size(); i++)
	  {
	    if (debug)
	      {
		System.err.println("User clone sub sub " + i);
	      }

	    tmpVal = newVolumes.createNewEmbedded(local);

	    if (!tmpVal.didSucceed())
	      {
		if (debug)
		  {
		    System.err.println("User clone couldn't allocate new embedded");
		  }

		if (tmpVal != null && tmpVal.getDialog() != null)
		  {
		    resultBuf.append("\n\n");
		    resultBuf.append(tmpVal.getDialog().getText());
		    
		    problem = true;
		  }
		continue;
	      }

	    newInvid = tmpVal.getInvid();

	    workingVolume = (DBEditObject) session.editDBObject(newInvid);
	    origVolume = session.viewDBObject((Invid) oldOnes.elementAt(i));

	    if (debug)
	      {
		System.err.println("Attempting to clone user volume " + origVolume.getLabel());
	      }

	    tmpVal = workingVolume.cloneFromObject(session, origVolume, local);

	    if (tmpVal != null && tmpVal.getDialog() != null)
	      {
		resultBuf.append("\n\n");
		resultBuf.append(tmpVal.getDialog().getText());
	    
		problem = true;
	      }
	  }
      }

    retVal = new ReturnVal(true, !problem);

    if (problem)
      {
	retVal.setDialog(new JDialogBuff("Possible Clone Problem", resultBuf.toString(),
					 "Ok", null, "ok.gif"));
      }
    
    return retVal;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * indicate that the given object is interested in receiving notification
   * when changes involving it occur, and can provide one or more addresses for
   * such notification to go to.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean hasEmailTarget(DBObject object)
  {
    return true;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * return a Vector of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public Vector getEmailTargets(DBObject object)
  {
    // don't tell this user's email address if this user is in the process
    // of being created.  this will avoid causing email to be sent to
    // the newly created account, which would likely bounce at this point
    // in time

    if (object instanceof DBEditObject)
      {
	if (((DBEditObject) object).getStatus() == ObjectStatus.CREATING)
	  {
	    return null;
	  }
      }

    Vector x = new Vector();

    /* -- */

    // do a union so that we clone the raw DBField vector, and so that
    // we handle any null result
    
    x = VectorUtils.union(x, object.getFieldValuesLocal(userSchema.EMAILTARGET));
    
    return x;
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>This version of anonymousLinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link based on what field of what object wants to link
   * to it.</P>
   *
   * <p>By default, the 3 variants of the DBEditObject anonymousLinkOK() 
   * method are chained together, so that the customizer can choose
   * which level of detail he is interested in.
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.InvidDBField#bind(arlut.csd.ganymede.Invid,arlut.csd.ganymede.Invid,boolean) bind()}
   * method calls this version.  This version calls the three parameter
   * version, which calls the two parameter version, which returns
   * false by default.  Customizers can implement any of the three
   * versions, but unless you maintain the version chaining yourself,
   * there's no point to implementing more than one of them.</P>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in
   * @param targetFieldID The field that the link is to be created in
   * @param sourceObject The object on the other side of the proposed link
   * @param sourceFieldID  The field on the other side of the proposed link
   * @param gsession Who is trying to do this linking?
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
				 DBObject sourceObject, short sourceFieldID,
				 GanymedeSession gsession)
  {
    // if they can edit the group, they can put us in it.. the
    // gasharl schema specifies the mandatory type for the other
    // end of the GROUPLIST field's link, so we don't have to 
    // check that here

    if (targetFieldID == userSchema.GROUPLIST)
      {
	return true;
      }

    // go ahead and allow the same for netgroups

    if (targetFieldID == userSchema.NETGROUPS)
      {
	return true;
      }

    // if someone tries to put this user in an email list, let them.

    if ((targetFieldID == SchemaConstants.BackLinksField) &&
	(sourceObject.getTypeID() == 274) && // email list
	(sourceFieldID == 257))	// email list members
      {
	return true;
      }

    // the default anonymousLinkOK() method returns false

    return super.anonymousLinkOK(targetObject, targetFieldID,
				 sourceObject, sourceFieldID, gsession);
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
	  case userSchema.CATEGORY:
	  case userSchema.HOMEGROUP:
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
	  case userSchema.CATEGORY:
	  case userSchema.HOMEGROUP:
	    return true;
	  }
      }

    // Whether or not the social security field is required depends on
    // the user category.

    if (fieldid == userSchema.SOCIALSECURITY)
      {
	try
	  {
	    Invid catInvid = (Invid) object.getFieldValueLocal(userSchema.CATEGORY);

	    // we're PSEUDOSTATIC, so we need to get ahold of the internal session
	    // so we can look up objects
	    
	    DBObject category = internalSession().getSession().viewDBObject(catInvid);

	    Boolean ssRequired = (Boolean) category.getFieldValueLocal(userCategorySchema.SSREQUIRED);

	    return ssRequired.booleanValue();
	  }
	catch (NullPointerException ex)
	  {
	    // if we can't get the category reference, assume that we
	    // aren't gonna require the category.. the user will still
	    // be prompted to set a category, and once they go back
	    // and do that and try to re-commit, they'll hit us again
	    // and we can make the proper determination at that point.

	    return false;
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
	    if (calledByWizard)
	      {
		finalizeInactivate(false);
	      }

	    return retVal;
	  }

	// we're not going to set the shell to /bin/false
	// anymore.. we'll depend on our builder task to write it out
	// as /bin/false for us.

	if (false)
	  {
	    // set the shell to /bin/false
	    
	    stringfield = (StringDBField) getField(LOGINSHELL);
	    retVal = stringfield.setValueLocal("/bin/false");
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		if (calledByWizard)
		  {
		    finalizeInactivate(false);
		  }
		
		return retVal;
	      }
	  }

	// reset the forwarding address?

	if (forward != null && !forward.equals(""))
	  {
	    stringfield = (StringDBField) getField(EMAILTARGET);
	
	    while (stringfield.size() > 0)
	      {
		retVal = stringfield.deleteElement(0);
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    if (calledByWizard)
		      {
			finalizeInactivate(false);
		      }

		    return retVal;
		  }
	      }

	    stringfield.addElement(forward);
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
	    if (calledByWizard)
	      {
		finalizeInactivate(false);
	      }

	    return retVal;
	  }

	// make sure that the expiration date is cleared.. we're on
	// the removal track now.

	date = (DateDBField) getField(SchemaConstants.ExpirationField);
	retVal = date.setValueLocal(null);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (calledByWizard)
	      {
		finalizeInactivate(false);
	      }

	    return retVal;
	  }

	// success, have our DBEditObject superclass clean up.

	if (calledByWizard)
	  {
	    finalizeInactivate(true);
	  }

	// ok, we succeeded, now we have to tell the client
	// what to refresh to see the inactivation results

	ReturnVal result = new ReturnVal(true);

	result.addRescanField(this.getInvid(), SchemaConstants.RemovalField);
	result.addRescanField(this.getInvid(), userSchema.LOGINSHELL);
	result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	return result;
      }
    else  // interactive, but not called by wizard.. return a wizard
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

	return theWiz.respond(null);
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
    
    return theWiz.respond(null);
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
    boolean success = false;

    /* -- */

    if (reactivateWizard == null)
      {
	return Ganymede.createErrorDialog("userCustom.reactivate() error",
					  "Error, reactivate() called without a valid user wizard");
      }

    try
      {
	// reset the password

	if (reactivateWizard.password != null && reactivateWizard.password.length() != 0)
	  {
	    passfield = (PasswordDBField) getField(userSchema.PASSWORD);
	    retVal = passfield.setPlainTextPass(reactivateWizard.password);
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }
	else
	  {
	    return Ganymede.createErrorDialog("userCustom.reactivate() error",
					      "Error, reactivate() called without a password selected");
	  }

	// reset the shell

	if (reactivateWizard.shell != null)
	  {
	    stringfield = (StringDBField) getField(LOGINSHELL);
	    retVal = stringfield.setValue(reactivateWizard.shell);
	    
	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	// reset the forwarding address

	if (reactivateWizard.forward != null && !reactivateWizard.forward.equals(""))
	  {
	    stringfield = (StringDBField) getField(EMAILTARGET);
	
	    while (stringfield.size() > 0)
	      {
		retVal = stringfield.deleteElement(0);
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }

	    String[] strings = arlut.csd.Util.StringUtils.split(reactivateWizard.forward, ",");

	    for (int i = 0; i < strings.length; i++)
	      {
		stringfield.addElement(strings[i]);
	      }
	  }

	// make sure that the removal date is cleared..

	date = (DateDBField) getField(SchemaConstants.RemovalField);
	retVal = date.setValueLocal(null);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }

	finalizeReactivate(true);
	success = true;

	// ok, we succeeded, now we have to tell the client
	// what to refresh to see the reactivation results

	ReturnVal result = new ReturnVal(true);

	result.addRescanField(this.getInvid(), SchemaConstants.RemovalField);
	result.addRescanField(this.getInvid(), userSchema.LOGINSHELL);
	result.addRescanField(this.getInvid(), userSchema.EMAILTARGET);

	return result;
      }
    finally
      {
	if (!success)
	  {
	    finalizeReactivate(false);
	  }
      }
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

	String name = newUsername;

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

  /**
   *
   * We update the groupChoices list to contain all of the groups
   * the user is currently in.
   *
   */

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
	    
	    // must be editable because the client cares

	    groupChoices.addRow(invid, gSession.viewObjectLabel(invid), true);
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
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given Date field has a restricted
   * range of possibilities.
   *
   */

  public boolean isDateLimited(DBField field)
  {
    if (field.getID() == SchemaConstants.ExpirationField)
      {
	return true;
      }

    if (field.getID() == userSchema.PASSWORDCHANGETIME)
      {
	return true;
      }

    return super.isDateLimited(field);
  }

  /**
   * This method is used to specify the earliest acceptable date
   * for the specified {@link arlut.csd.ganymede.DateDBField DateDBField}.
   */

  public Date minDate(DBField field)
  {
    if (field.getID() == userSchema.PASSWORDCHANGETIME)
      {
	return new Date(); // no values in the past, thanks
      }

    return super.minDate(field);
  }

  /**
   * This method is used to specify the latest acceptable date
   * for the specified {@link arlut.csd.ganymede.DateDBField DateDBField}.
   */

  public Date maxDate(DBField field)
  {
    if (field.getID() == SchemaConstants.ExpirationField)
      {
	// if we have a user category set, limit the acceptable date to
	// current date + max days

	Date currentDate = new Date();

	Calendar cal = Calendar.getInstance();

	cal.setTime(currentDate);

	try
	  {
	    Invid catInvid = (Invid) this.getFieldValueLocal(userSchema.CATEGORY);

	    DBObject category = internalSession().getSession().viewDBObject(catInvid);

	    Integer maxDays = (Integer) category.getFieldValueLocal(userCategorySchema.LIMIT);
	
	    cal.add(Calendar.DATE, maxDays.intValue());
	  }
	catch (NullPointerException ex)
	  {
	    // oops, no category set.. <shrug>

	    return super.maxDate(field);
	  }

	return cal.getTime();
      }

    if (field.getID() == userSchema.PASSWORDCHANGETIME)
      {
	GanymedeSession mySession = this.getGSession();

	// if we are supergash or we are reacting to a password change
	// cascade, don't restrict what the date can be set to.

	if (mySession == null || mySession.isSuperGash() || amChangingExpireDate)
	  {
	    return super.maxDate(field);
	  }

	Date maxDate = getMaxPasswordExtension();

	DateDBField passDateField = (DateDBField) getField(userSchema.PASSWORDCHANGETIME);

	if (passDateField != null)
	  {
	    Date currentDate = passDateField.value();

	    if (currentDate != null && currentDate.after(maxDate))
	      {
		maxDate = currentDate;
	      }
	  }

	return maxDate;
      }

    return super.maxDate(field);
  }

  /**
   *
   * This method is called after the set value operation has been ok'ed
   * by any appropriate wizard code.
   *
   */

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
	    if (newUsername != null)
	      {
		String expected = homedir + (String) newUsername;
		    
		if (!dir.equals(expected))
		  {
		    return Ganymede.createErrorDialog("Schema Error",
						      "Home directory should be " + expected + ".\n" +
						      "This is a restriction encoded in userCustom.java.");
		  }
	      }
	  }

	return null;
      }

    if (field.getID() == userSchema.PASSWORD)
      {
	// the password is being changed, update the time that it will need to
	// be changed again

	DateDBField dateField = (DateDBField) getField(userSchema.PASSWORDCHANGETIME);

	if (dateField != null)
	  {
	    Date passwordDate = getNewPasswordExpirationDate();
	    Date currentDate = dateField.value();

	    // be sure and check to make sure we never pull a password
	    // expiration date backwards in time

	    if (currentDate == null || !currentDate.after(passwordDate))
	      {
		// set the amChangingExpireDate flag to true so that we
		// won't try and restrict the forward date when the date
		// set operation cascades forward through our maxDate()
		// method

		ReturnVal result;
		
		amChangingExpireDate = true;

		try
		  {
		    result = dateField.setValueLocal(passwordDate);
		  }
		finally
		  {
		    amChangingExpireDate = false;
		  }
		
		if (result != null)
		  {
		    System.err.println("UserCustom: setValueLocal on PASSWORDCHANGETIME field failed: " + result);
		  }
	      }
	  }
	else
	  {
	    System.err.println("UserCustom: can't find PASSWORDCHANGETIME field");
	  }

	ReturnVal result = new ReturnVal(true, true);	
	    
	result.addRescanField(this.getInvid(), userSchema.PASSWORDCHANGETIME);

	return result;
      }

    // our maxDate() and isDateLimited() methods have pre-filtered any
    // non-null expiration date for us.. just need to check to see
    // whether the field can be cleared here.

    if ((field.getID() == SchemaConstants.ExpirationField) && value == null)
      {
	if (deleting)
	  {
	    // approve it, everything's being cleaned out.

	    return null;
	  }

	if (willBeRemoved())
	  {
	    // it's okay for us to null the expiration date, since we already
	    // have a removal date set

	    return null;
	  }

	// check to see if the user category doesn't mind not having an expiration
	// or removal date set.

	try
	  {
	    Invid catInvid = (Invid) this.getFieldValueLocal(userSchema.CATEGORY);
	    
	    DBObject category = internalSession().getSession().viewDBObject(catInvid);
	    
	    Boolean expDateRequired = (Boolean) category.getFieldValueLocal(userCategorySchema.EXPIRE);
	    
	    if (expDateRequired.booleanValue())
	      {
		return Ganymede.createErrorDialog("Schema Error",
						  "This user requires an expiration date because of its " +
						  "user category.");
	      }
	    else
	      {
		// ok, then
		
		return null;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    // ah, no category or limit set.. go ahead and let em do
	    // it

	    return null;
	  }
      }

    // when we rename a user, we have lots to do.. a number of other
    // fields in this object and others need to be updated to match.

    if (field.getID() == USERNAME)
      {
	// remember the new user name we are changing to, so that the
	// other fields that we will change as a result of the
	// username change will be able to get the new name.

	newUsername = (String) value;

	try
	  {
	    // if we are being told to clear the user name field, go ahead and
	    // do it.. we assume this is being done by user removal logic,
	    // so we won't press the issue.

	    if (deleting && (value == null))
	      {
		return null;
	      }

	    // signature alias field will need to be rescanned

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
		return null;	// success
	      }

	    // rename all the associated personae with the new user name

	    personaeInvids = inv.getValues();

	    for (int i = 0; i < personaeInvids.size(); i++)
	      {
		adminPersonaCustom adminObj = (adminPersonaCustom) session.editDBObject((Invid) personaeInvids.elementAt(i));

		adminObj.refreshLabelField(null, null, (String) value);
	      }
	  }
	finally
	  {
	    newUsername = null;
	  }
      }

    return null;		// success by default
  }

  /**
   * <p>This method calculates what time the password expiration field should be set to
   * if the password is being changed right now.</p>
   */

  private Date getNewPasswordExpirationDate()
  {
    Calendar myCal = new GregorianCalendar();
    myCal.add(Calendar.MONTH, 3);

    // if the expiration date will fall between Dec 20
    // and January 10, bump the date forward three
    // weeks to skip over the year-end holidays

    int month = myCal.get(Calendar.MONTH);
    int day = myCal.get(Calendar.DATE);

    if ((month == Calendar.DECEMBER && day >= 20) ||
	(month == Calendar.JANUARY && day < 10))
      {
	myCal.add(Calendar.DATE, 21);
      }

    return myCal.getTime();
  }

  /**
   * <p>This method calculates what the maximum time the password
   * expiration field may be set to by a ganymede admin.</p> */

  private Date getMaxPasswordExtension()
  {
    Calendar myCal = new GregorianCalendar();
    myCal.add(Calendar.DATE, 14);

    // if the maximum expiration date will fall between Dec 20
    // and January 10, bump the date forward three
    // weeks to skip over the year-end holidays

    int month = myCal.get(Calendar.MONTH);
    int day = myCal.get(Calendar.DATE);

    if ((month == Calendar.DECEMBER && day >= 20) ||
	(month == Calendar.JANUARY && day < 10))
      {
	myCal.add(Calendar.DATE, 21);
      }

    return myCal.getTime();
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
	if (field.getID() == userSchema.PASSWORD && operation == SETPASSPLAIN)
	  {
	    result = validatePasswordChoice((String) param1, getGSession().isSuperGash());
		
	    return result;
	  }

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
	      case ADDELEMENTS:

		// ok, no big deal, but we will need to have the client
		// rescan the choice list for the home group field

		result = new ReturnVal(true, true);
		result.addRescanField(this.getInvid(), HOMEGROUP);
		groupChoices = null;
		return result;

	      case DELELEMENT:

		if (deleting)
		  {
		    return null;
		  }

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
	    
		return groupWizard.respond(null);

	      case DELELEMENTS:

		// see if any of the values is the home group

		Vector valuesToDelete = (Vector) param1;

		if (!valuesToDelete.contains(getFieldValueLocal(HOMEGROUP)))
		  {
		    result = new ReturnVal(true, true);
		    result.addRescanField(this.getInvid(), HOMEGROUP); // rebuild choice list
		    groupChoices = null;
		    return result;
		  }
		else
		  {
		    return Ganymede.createErrorDialog("User Validation Error",
						      "Can't remove home group in bulk transfer.");
		  }
	      }
	  }

	// if the user category is changed, we need to be sure and get
	// the expiration date set..

	if (field.getID() == CATEGORY)
	  {
	    if (gSession.isWizardActive() && 
		gSession.getWizard() instanceof userCategoryWizard)
	      {
		userCategoryWizard uw = (userCategoryWizard) gSession.getWizard();
		
		if (uw.getState() == uw.DONE)
		  {
		    // ok, assume the wizard has taken care of getting everything prepped and
		    // approved for us.  An active wizard has approved the operation

		    return null;
		  }
	      }

	    try
	      {
		if (param1 != null || !deleting)
		  {
		    return new userCategoryWizard(getGSession(), this, 
						  (Invid) getFieldValueLocal(userSchema.CATEGORY),
						  (Invid) param1).respond(null);
		  }
		else
		  {
		    return null;
		  }
	      }
	    catch (RemoteException ex)
	      {
		return Ganymede.createErrorDialog("Server error",
						  "userCustom.wizardHook(): can't initialize userCategoryWizard.");
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
	else if (gSession.isWizardActive())
	  {
	    return Ganymede.createErrorDialog("User Object Error",
					      "The client is attempting to do an operation on " +
					      "a user object with mismatched active wizard.\n" +
					      "Wizard id: " + gSession.getWizard());
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
	    // that wizard.respond() returns will have the success code
	    // set to false, so whatever triggered us will prematurely exit,
	    // returning the wizard's dialog.

	    return renameWizard.respond(null);
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
   * <p>This method checks the given plaintext password against an
   * external password validator library.  validatePasswordChoice
   * specifies a file name to write the evaluation results to, runs
   * the validator, and then reads from the file to see if the
   * operation succeeded.</p>
   */

  public ReturnVal validatePasswordChoice(String password, boolean asRoot)
  {
    String resultString = null;
    String validatorName = "/opt/bin/ganypassValidate";

    String username = (String) getFieldValueLocal(USERNAME);

    if (username == null)
      {
	return Ganymede.createErrorDialog("Error",
					  "I need to know the username before I can set the password");
      }

    File resultFile = userCustom.getNextFileName();

    String validatorCommand = validatorName + " " + username + " " + resultFile.getPath();

    File file = new File(validatorName);

    if (file.exists())
      {
	Process process = null;

	/* -- */

	try
	  {
	    process = Runtime.getRuntime().exec(validatorCommand);

	    PrintWriter out = new PrintWriter(process.getOutputStream());
	    out.println(password);
	    out.close();

	    process.waitFor();

	    if (process.exitValue() != 0)
	      {
		try
		  {
		    BufferedReader in = new BufferedReader(new FileReader(resultFile));
		    resultString = in.readLine();
		    in.close();

		    if (!asRoot)
		      {
			return Ganymede.createErrorDialog("Password Rejected",
							  resultString);
		      }
		    else
		      {
			return Ganymede.createInfoDialog("Warning: password not secure",
							 "The password quality checker has not approved this password.  As supergash " +
							 "you may go ahead and use the password you chose, but it may not be secure.\n\n" +
							 resultString);
		      }
		  }
		catch (FileNotFoundException ex)
		  {
		    ex.printStackTrace();
		  }
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't exec validatorCommand (" + validatorCommand + 
			   ") due to IOException: " + ex);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("Failure during exec of validatorCommand (" + validatorCommand + "): " + ex);
	  }
	finally
	  {
	    FileOps.cleanupProcess(process);

	    try
	      {
		resultFile.delete();
	      }
	    catch (Exception ex)
	      {
		ex.printStackTrace();
	      }
	  }
      }
    else
      {
	Ganymede.debug(validatorCommand + " doesn't exist, not running external password validation program");
      }

    return null;
  }

  /**
   * <p>This method is used to save the chosen password to npasswd's
   * on-disk non-reuse history so that npasswd can reject a too-soon
   * recurrence of this password later.</p>
   *
   * <p>If supergash is editing this account, the password might not
   * be to npasswd's liking, in which case the password save operation
   * will fail, but we won't worry about that it if it happens.</p>
   */

  public ReturnVal savePasswordChoice(String password)
  {
    String resultString = null;
    String saverName = "/opt/bin/ganypassSave";

    String username = (String) getFieldValueLocal(USERNAME);

    if (username == null)
      {
	return Ganymede.createErrorDialog("Error",
					  "I need to know the username before I can save the password");
      }

    String saverCommand = saverName + " " + username;

    File file = new File(saverName);

    if (file.exists())
      {
	Process process = null;

	/* -- */

	try
	  {
	    process = Runtime.getRuntime().exec(saverCommand);

	    PrintWriter out = new PrintWriter(process.getOutputStream());
	    out.println(password);
	    out.close();

	    process.waitFor();

	    if (process.exitValue() != 0)
	      {
		// the calling code doesn't really care about the
		// failure to save, but
		// Ganymede.createErrorDialog() will log this to
		// stderr.
		
		return Ganymede.createErrorDialog("Password History Not Saved",
						  "External error in npasswd saver");
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't exec saverCommand (" + saverCommand + 
			   ") due to IOException: " + ex);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("Failure during exec of saverCommand (" + saverCommand + "): " + ex);
	  }
	finally
	  {
	    FileOps.cleanupProcess(process);
	  }
      }
    else
      {
	Ganymede.debug(saverCommand + " doesn't exist, not saving password history");
      }

    return null;
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

	// remember the user's initial password for our password history support

	PasswordDBField pField = (PasswordDBField) getField(userSchema.PASSWORD);

	if (pField != null)
	  {
	    String pass = pField.getPlainText();

	    if (pass != null)
	      {
		savePasswordChoice(pass);
	      }
	  }

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
	    handleUserRename(oldname, name);
	  }

	// did we change home directory volumes?

	handleVolumeChanges();

	// did the user's password change?

	PasswordDBField pField1 = (PasswordDBField) getField(userSchema.PASSWORD);
	String pass1 = pField1.getPlainText();

	PasswordDBField pField2 = (PasswordDBField) original.getField(userSchema.PASSWORD);

	if (pField2 != null)
	  {
	    String pass2 = pField2.getPlainText();

	    if (pass1 != null && !pass1.equals(pass2))
	      {
		savePasswordChoice(pass1);
	      }
	  }
	else if (pass1 != null)
	  {
	    savePasswordChoice(pass1);
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
    boolean success = false;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom: " + getLabel() + ", in createUserExternals().");
      }

    // get the volumes defined for the user on auto.home.default

    InvidDBField mapEntries = (InvidDBField) getField(userSchema.VOLUMES);
    Vector entries = mapEntries.getValues();

    if (entries.size() < 1)
      {
	System.err.println("Couldn't handle createUserExternals for user " + getLabel() +
			   ", because we don't have a volume defined");
	return;
      }

    for (int i = 0; i < entries.size(); i++)
      {
	user_added_to_vol((Invid) entries.elementAt(i));
      }
  }

  /**
   * Helper method to create a directory for a user
   */

  private void user_added_to_vol(Invid entryInvid)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    DBObject entryObj = getSession().viewDBObject(entryInvid);

    Invid volumeInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.VOLUME);
    DBObject volumeObj = getSession().viewDBObject(volumeInvid);
    String volName = (String) volumeObj.getFieldValueLocal(volumeSchema.LABEL);

    Invid mapInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.MAP);
    DBObject mapObj = getSession().viewDBObject(mapInvid);
    String mapName = mapObj.getLabel();

    Integer id = (Integer) getFieldValueLocal(userSchema.UID);
    Invid homegroupInvid = (Invid) getFieldValueLocal(userSchema.HOMEGROUP);

    Vector ownerInvids = (Vector) this.getFieldValuesLocal(SchemaConstants.OwnerListField);
    String ownerName;

    if (ownerInvids != null && ownerInvids.size() > 0)
      {
	Invid ownerOne = (Invid) ownerInvids.elementAt(0);

	DBObject ownerObj = getSession().viewDBObject(ownerOne);
	ownerName = ownerObj.getLabel();

	// we want underscores to separate words, not spaces

	ownerName = ownerName.replace(' ', '_');
      }
    else
      {
	ownerName = "";
      }

    if (homegroupInvid == null)
      {
	// the user didn't completely fill out this user
	// object.. return silently and let the transaction logic tell
	// the user what the problem is.

	return;
      }

    DBObject homeGroup = getSession().viewDBObject(homegroupInvid);
    Integer gid = (Integer) homeGroup.getFieldValueLocal(groupSchema.GID);

    boolean success = false;

    try
      {
	if (createHandler == null)
	  {
	    if (debug)
	      {
		System.err.println("userCustom: createUserExternals: getting createFilename");
	      }

	    createFilename = System.getProperty("ganymede.builder.scriptlocation");

	    if (createFilename == null)
	      {
		Ganymede.debug("userCustom.createUserExternals(): Couldn't find " + 
			       "ganymede.builder.scriptlocation property");
		return;
	      }

	    // make sure we've got the path separator at the end of
	    // createFilename, add our script name

	    createFilename = PathComplete.completePath(createFilename) + "/scripts/directory_maker";

	    if (debug)
	      {
		System.err.println("userCustom: createUserExternals: createFilename = " + 
				   createFilename);
	      }

	    createHandler = new File(createFilename);
	  }

	if (createHandler.exists())
	  {
	    try
	      {
		// we'll call our external script with the following
		//
		// parameters: <volumename/volume_directory> <username> <user id> <group id> <mapname> <owner>
		
		String execLine = createFilename + " " + volName + " " + 
		  getLabel() + " " + id + " " + gid + " " + mapName + " " + ownerName;

		if (debug)
		  {
		    System.err.println("createUserExternals: running " + execLine);
		  }

		try
		  {
		    if (debug)
		      {
			System.err.println("createUserExternals: blocking ");
		      }

		    int result = FileOps.runProcess(execLine);

		    if (debug)
		      {
			System.err.println("createUserExternals: done ");
		      }

		    if (result != 0)
		      {
			Ganymede.debug("Couldn't handle externals for creating user " + getLabel() +
				       "\n" + createFilename + " returned a non-zero result: " + result);
		      }
		    else
		      {
			success = true;
		      }
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("Couldn't handle externals for creating user " + getLabel() + "\n" +
				   ex.getMessage());
		  }
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("Couldn't handle externals for creating user " + getLabel() + "\n" +
			       ex.getMessage());
	      }
	  }
      }
    finally
      {
	mail_user_added_to_vol(entryInvid, !success);
      }
  }

  /**
   * Helper method to send out mail to owners of the system that the
   * user's home directory is being placed on.
   */

  private void mail_user_added_to_vol(Invid entryInvid, boolean need_to_create)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    StringBuffer buffer = new StringBuffer();

    DBObject entryObj = getSession().viewDBObject(entryInvid);

    Invid mapInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.MAP);
    DBObject mapObj = getSession().viewDBObject(mapInvid);
    String mapName = mapObj.getLabel();

    Invid volumeInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.VOLUME);
    DBObject volumeObj = getSession().viewDBObject(volumeInvid);
    String volName = volumeObj.getLabel();
    String volPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);

    Invid sysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    DBObject sysObj = getSession().viewDBObject(sysInvid);
    String sysName = sysObj.getLabel();

    Vector objects = new Vector();
    objects.addElement(sysInvid);
    Vector addresses = DBLog.calculateOwnerAddresses(objects, getSession());

    String subject = null;

    if (need_to_create)
      {
	buffer.append("Hi.  User ");
	buffer.append(getLabel());
	buffer.append(" was added to volume ");
	buffer.append(volName);
	buffer.append(" in the ");
	buffer.append(mapName);
	buffer.append(" automounter home map.\n\nSince you are listed in the Ganymede");
	buffer.append(" system database as an administrator for a system contained in");
	buffer.append(" volume ");
	buffer.append(volName);
	buffer.append(", you need to take whatever action is appropriate to create a");
	buffer.append(" home directory for this user on ");
	buffer.append(sysName);
	buffer.append(", if one does not already exist..\n\n");
	buffer.append("Volume ");
	buffer.append(volName);
	buffer.append(" is currently defined as:\n");
	buffer.append(sysName);
	buffer.append(":");
	buffer.append(volPath);
	buffer.append("\n\nThanks for your cooperation.\nYour friend,\n\tGanymede.\n");

	subject = "User " + getLabel() + " needs a home directory on " + sysName;
      }
    else
      {
	buffer.append("A home directory for user ");
	buffer.append(getLabel());
	buffer.append(" has been constructed on volume ");
	buffer.append(volName);
	buffer.append(".  The user's home directory has been registered in the ");
	buffer.append(mapName);
	buffer.append(" automounter home map.");

	subject = "User " + getLabel() + " home directory created";
      }

    editset.logMail(addresses, subject, buffer.toString());
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
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom: " + getLabel() + ", in deleteUserExternals().");
      }

    handleUserDelete(getLabel());

    // get the volumes defined for the user on auto.home.default

    InvidDBField mapEntries = (InvidDBField) getOriginal().getField(userSchema.VOLUMES);
    Vector entries = mapEntries.getValues();

    if (entries.size() < 1)
      {
	System.err.println("Couldn't handle deleteUserExternals for user " + getLabel() +
			   ", because we don't have a volume defined");
	return;
      }

    for (int i = 0; i < entries.size(); i++)
      {
	mail_user_removed_from_vol((Invid) entries.elementAt(i));
      }
  }

  /**
   * Helper method to send out mail to owners of the system that the
   * user's home directory is being scrubbed from.
   */

  private void mail_user_removed_from_vol(Invid entryInvid)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    StringBuffer buffer = new StringBuffer();

    DBObject entryObj = getSession().viewDBObject(entryInvid, true);

    Invid mapInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.MAP);
    DBObject mapObj = getSession().viewDBObject(mapInvid, true);

    String mapName = mapObj.getLabel();

    Invid volumeInvid = (Invid) entryObj.getFieldValueLocal(mapEntrySchema.VOLUME);
    DBObject volumeObj = getSession().viewDBObject(volumeInvid, true);
    String volName = volumeObj.getLabel();
    String volPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);

    Invid sysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    DBObject sysObj = getSession().viewDBObject(sysInvid, true);
    String sysName = sysObj.getLabel();

    Vector objects = new Vector();
    objects.addElement(sysInvid);
    Vector addresses = DBLog.calculateOwnerAddresses(objects, getSession());

    String subject = null;

    buffer.append("User ");
    buffer.append(getLabel());
    buffer.append(" has been removed from volume ");
    buffer.append(volName);
    buffer.append(" in the ");
    buffer.append(mapName);
    buffer.append(" automounter home map.\n\nSince you are listed in the Ganymede");
    buffer.append(" system database as an administrator for a system contained in");
    buffer.append(" volume ");
    buffer.append(volName);
    buffer.append(", you need to take whatever action is appropriate to remove this user");
    buffer.append(" from ");
    buffer.append(volName);
    buffer.append(" if you are sure that the user will no longer be using his or her directory");
    buffer.append(" on this volume.\n\n");
    buffer.append("Volume ");
    buffer.append(volName);
    buffer.append(" is currently defined as:\n");
    buffer.append(sysName);
    buffer.append(":");
    buffer.append(volPath);
    buffer.append("\n\nThanks for your cooperation.\nYour friend,\n\tGanymede.\n");

    subject = "User " + getLabel() + " needs to be removed on " + sysName;

    editset.logMail(addresses, subject, buffer.toString());
  }

  /**
   * This method handles external actions for deleting a user.
   */

  private void handleUserDelete(String name)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    // This would be unusual for a delete, but..

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom.handleUserDelete(): user " + name +
			   "is being deleted");
      }

    try
      {
	if (deleteHandler == null)
	  {
	    deleteFilename = System.getProperty("ganymede.builder.scriptlocation");

	    if (deleteFilename != null)
	      {
		// make sure we've got the path separator at the end of
		// deleteFilename, add our script name
	    
		deleteFilename = PathComplete.completePath(deleteFilename) + "/scripts/user_deleter";
	    
		deleteHandler = new File(deleteFilename);
	      }
	    else
	      {
		Ganymede.debug("userCustom.handleUserDelete(): Couldn't find " +
			       "ganymede.builder.scriptlocation property");
	      }
	  }

	if (deleteHandler.exists())
	  {
	    try
	      {
		String execLine = deleteFilename + " " + name;

		if (debug)
		  {
		    System.err.println("handleUserDelete: running " + execLine);
		  }

		try
		  {
		    if (debug)
		      {
			System.err.println("handleUserDelete: blocking");
		      }

		    int result = FileOps.runProcess(execLine);

		    if (debug)
		      {
			System.err.println("handleUserDelete: done");
		      }

		    if (result != 0)
		      {
			Ganymede.debug("Couldn't handle externals for deleting user " + name + 
				       "\n" + deleteFilename + 
				       " returned a non-zero result: " + result);
		      }
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("Couldn't handle externals for deleting user " + name + ": " +
				   ex.getMessage());
		  }
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("Couldn't handle externals for deleting user " + name + ": " +
			       ex.getMessage());
	      }
	  }
      }
    finally
      {
	Invid admin = getGSession().getPersonaInvid();
	String adminName = getGSession().getMyUserName();
	Vector objects = new Vector();
	objects.addElement(getInvid());

	StringBuffer buffer = new StringBuffer();

	buffer.append("User ");
	buffer.append(name);
	buffer.append(" has been expunged from the Ganymede database.\n\n");

	editset.logEvent("userdeleted",
			 buffer.toString(),
			 admin, adminName, objects, null);
      }
  }

  /**
   * This method is designed to send out mail notifying admins of changes
   * made to a user's volume mappings, if any
   */

  private void handleVolumeChanges()
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    Hashtable oldEntryMap = new Hashtable();
    Hashtable newEntryMap = new Hashtable();

    Hashtable oldVolMap = new Hashtable();
    Hashtable newVolMap = new Hashtable();

    Vector oldMapNames = new Vector();
    Vector newMapNames = new Vector();

    Vector oldVolumes = new Vector();
    Vector newVolumes = new Vector();

    Invid mapEntryInvid;
    Invid volumeId;

    DBObject mapEntryObj;
    mapEntryCustom mapEntry;
    String mapName;
    String volName;

    Vector oldEntries = original.getFieldValuesLocal(userSchema.VOLUMES);

    if (oldEntries != null)
      {
	for (int i = 0; i < oldEntries.size(); i++)
	  {
	    mapEntryInvid = (Invid) oldEntries.elementAt(i);

	    mapEntryObj = getSession().viewDBObject(mapEntryInvid);

	    if (mapEntryObj instanceof mapEntryCustom)
	      {
		mapEntry = (mapEntryCustom) mapEntryObj;

		mapName = mapEntry.getOriginalMapName();
		volumeId = mapEntry.getOriginalVolumeInvid();
	    
		oldVolumes.addElement(volumeId);
		oldEntryMap.put(mapName, mapEntryInvid);
		oldMapNames.addElement(mapName);

		// if we see the same volume in multiple maps, we'll just
		// remember the last one seen.. doesn't matter much, for
		// our purposes

		oldVolMap.put(volumeId, mapEntryInvid);

		if (debug)
		  {
		    System.err.println("Old entry.. " + mapName + ", " + volumeId);
		  }
	      }
	  }
      }

    Vector newEntries = getFieldValuesLocal(userSchema.VOLUMES);

    if (newEntries != null)
      {
	for (int i = 0; i < newEntries.size(); i++)
	  {
	    mapEntryInvid = (Invid) newEntries.elementAt(i);
	    mapEntryObj = getSession().viewDBObject(mapEntryInvid);

	    if (mapEntryObj instanceof mapEntryCustom)
	      {
		mapEntry = (mapEntryCustom) mapEntryObj;

		mapName = mapEntry.getMapName();
		volumeId = mapEntry.getVolumeInvid();
	    
		newVolumes.addElement(volumeId);
		newEntryMap.put(mapName, mapEntryInvid);
		newMapNames.addElement(mapName);

		// if we see the same volume in multiple maps, we'll just
		// remember the last one seen.. doesn't matter much, for
		// our purposes

		newVolMap.put(volumeId, mapEntryInvid);

		if (debug)
		  {
		    System.err.println("New entry.. " + mapName + ", " + volumeId);
		  }
	      }
	  }
      }

    Vector addedVolumes = VectorUtils.difference(newVolumes, oldVolumes);
    Vector deletedVolumes = VectorUtils.difference(oldVolumes, newVolumes);

    Vector keptMapNames = VectorUtils.intersection(newMapNames, oldMapNames);

    for (int i = 0; i < keptMapNames.size(); i++)
      {
	mapName = (String) keptMapNames.elementAt(i);

	if (debug)
	  {
	    System.err.println("Checking map " + mapName + " for a volume change");
	  }
	
	Invid oldMapEntryInvid = (Invid) oldEntryMap.get(mapName);
	Invid newMapEntryInvid = (Invid) newEntryMap.get(mapName);

	if (oldMapEntryInvid.equals(newMapEntryInvid))
	  {
	    // we know the map entry obj is an editing copy, don't
	    // need to check here

	    mapEntry = (mapEntryCustom) getSession().viewDBObject(oldMapEntryInvid);

	    Invid oldVolInvid = mapEntry.getOriginalVolumeInvid();
	    Invid newVolInvid = mapEntry.getVolumeInvid();

	    if (!oldVolInvid.equals(newVolInvid))
	      {
		if (debug)
		  {
		    System.err.println("In map " + mapName + ", old vol was " + oldVolInvid +
				       ", is now " + newVolInvid);
		  }

		// we have moved the user's home directory on this map.. we won't
		// try to create the new home directory ourselves

		user_moved_from_vol_to_vol(oldVolInvid, newVolInvid, mapName);

		// we've already handled notification for the moving
		// between these volumes, don't need to do anything more
		// for it

		deletedVolumes.removeElement(oldVolInvid);
		addedVolumes.removeElement(newVolInvid);
	      }
	  }
      }

    for (int i = 0; i < addedVolumes.size(); i++)
      {
	volumeId = (Invid) addedVolumes.elementAt(i);

	if (debug)
	  {
	    System.err.println("Gained volume " + volumeId);
	  }

	// the user might have the same volume registered on multiple
	// maps, but we don't care enough to send mail out for it

	user_added_to_vol((Invid) newVolMap.get(volumeId));
      }

    for (int i = 0; i < deletedVolumes.size(); i++)
      {
	volumeId = (Invid) deletedVolumes.elementAt(i);

	if (debug)
	  {
	    System.err.println("Lost volume " + volumeId);
	  }

	// the user might have had the same volume registered on
	// multiple maps, but we don't care enough to send mail out
	// for it

	mail_user_removed_from_vol((Invid) oldVolMap.get(volumeId));
      }
  }

  /**
   * <P>This method takes care of executing whatever external code is required
   * to handle this user being moved from volume to volume</P>
   *
   * @param oldVolume Invid for old volume listed on a given map
   * @param newVolume Invid for new volume listed on a given map
   * @param mapName Name of the map this user is being moved on.
   */

  private void user_moved_from_vol_to_vol(Invid oldVolume, Invid newVolume, String mapName)
  {
    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    DBObject volumeObj;
    DBObject sysObj;

    String oldVolName;
    String oldVolPath;
    Invid oldSysInvid;
    String oldSysName;

    String newVolName;
    String newVolPath;
    Invid newSysInvid;
    String newSysName;

    Vector objects = new Vector();

    StringBuffer buffer = new StringBuffer();

    /* -- */

    volumeObj = getSession().viewDBObject(oldVolume, true);
    
    oldVolName = volumeObj.getLabel();
    oldVolPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);
    oldSysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    objects.addElement(oldSysInvid);
    sysObj = getSession().viewDBObject(oldSysInvid, true);
    oldSysName = sysObj.getLabel();    

    volumeObj = getSession().viewDBObject(newVolume);

    newVolName = volumeObj.getLabel();
    newVolPath = (String) volumeObj.getFieldValueLocal(volumeSchema.PATH);
    newSysInvid = (Invid) volumeObj.getFieldValueLocal(volumeSchema.HOST);
    objects.addElement(newSysInvid);
    sysObj = getSession().viewDBObject(newSysInvid);
    newSysName = sysObj.getLabel();    

    Vector addresses = DBLog.calculateOwnerAddresses(objects, getSession());    

    buffer.append("Hi.  User ");
    buffer.append(getLabel());
    buffer.append(" was moved from volume ");
    buffer.append(oldVolName);
    buffer.append(" to volume ");
    buffer.append(newVolName);
    buffer.append(" in the ");
    buffer.append(mapName);
    buffer.append(" automounter home map.\n\nSince you are listed in the Ganymede system database");
    buffer.append(" as an administrator for a system contained in volume ");
    buffer.append(oldVolName);
    buffer.append(", you need to take whatever action is appropriate to move this user's");
    buffer.append(" directory from ");
    buffer.append(oldVolName);
    buffer.append(" if you are sure that the user will no longer be using his or her directory");
    buffer.append(" on this volume.\n\n");
    buffer.append("Volume ");
    buffer.append(oldVolName);
    buffer.append(" is currently defined as:\n\t");
    buffer.append(oldSysName);
    buffer.append(":");
    buffer.append(oldVolPath);
    buffer.append("\n\n");
    buffer.append("Volume ");
    buffer.append(newVolName);
    buffer.append(" is currently defined as:\n\t");
    buffer.append(newSysName);
    buffer.append(":");
    buffer.append(newVolPath);
    buffer.append("\n\n");
    buffer.append("Thanks for your cooperation.\nYour friend,\n\tGanymede.\n");

    editset.logMail(addresses, 
		    "User home directory on map " + mapName + " moved",
		    buffer.toString());
  }

  /**
   * This method handles external actions for renaming a user.
   */

  private void handleUserRename(String orig, String newname)
  {
    boolean success = false;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userCustom.handleUserRename(): user " + orig +
			   "is being renamed to " + newname);
      }

    try
      {
	if (renameHandler == null)
	  {
	    renameFilename = System.getProperty("ganymede.builder.scriptlocation");

	    if (renameFilename != null)
	      {
		// make sure we've got the path separator at the end of
		// renameFilename, add our script name
	    
		renameFilename = PathComplete.completePath(renameFilename) + "/scripts/directory_namer";
	    
		renameHandler = new File(renameFilename);
	      }
	    else
	      {
		Ganymede.debug("userCustom.handleUserRename(): Couldn't find " +
			       "ganymede.builder.scriptlocation property");
	      }
	  }

	if (renameHandler.exists())
	  {
	    try
	      {
		String execLine = renameFilename + " " + orig + " " + newname;

		if (debug)
		  {
		    System.err.println("handleUserRename: running " + execLine);
		  }

		try
		  {
		    if (debug)
		      {
			System.err.println("handleUserRename: blocking");
		      }

		    int result = FileOps.runProcess(execLine);

		    if (debug)
		      {
			System.err.println("handleUserRename: done");
		      }

		    if (result != 0)
		      {
			Ganymede.debug("Couldn't handle externals for renaming user " + orig + 
				       " to " + newname + "\n" + renameFilename + 
				       " returned a non-zero result: " + result);
		      }
		    else
		      {
			success = true;
		      }
		  }
		catch (InterruptedException ex)
		  {
		    Ganymede.debug("Couldn't handle externals for renaming user " + orig + 
				   " to " + 
				   newname + "\n" + 
				   ex.getMessage());
		  }
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("Couldn't handle externals for renaming user " + orig + 
			       " to " + 
			       newname + "\n" + 
			       ex.getMessage());
	      }
	  }
      }
    finally
      {
	Invid admin = getGSession().getPersonaInvid();
	String adminName = getGSession().getMyUserName();
	Vector objects = new Vector();
	objects.addElement(getInvid());

	StringBuffer buffer = new StringBuffer();

	buffer.append("User ");
	buffer.append(orig);
	buffer.append(" has been renamed to ");
	buffer.append(newname);
	buffer.append(".\n\n");

	if (success)
	  {
	    buffer.append("The user's main home directory has been renamed.  You may need ");
	    buffer.append("to take some action to make sure that the user's account name change ");
	    buffer.append("doesn't cause problems in your local scripts, etc.\n\n");
	  }
	else
	  {
	    buffer.append("The user's main home directory was not able to be properly renamed ");
	    buffer.append("by Ganymede.  You should contact a systems administrator on the user's");
	    buffer.append("main server to make sure his or her home directory is renamed properly.\n\n");
	    buffer.append("In addition, you may need ");
	    buffer.append("to take some action to make sure that the user's account name change ");
	    buffer.append("doesn't cause problems in your local scripts, etc.\n\n");
	  }

	editset.logEvent("userrenamed",
			 buffer.toString(),
			 admin, adminName, objects, null);
      }
  }
}
