/*

   adminPersonaCustom.java

   This file is a management class for admin personae objects in Ganymede.
   
   Created: 8 October 1997
   Release: $Name:  $
   Version: $Revision: 1.21 $
   Last Mod Date: $Date: 1999/07/22 05:34:19 $
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

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              adminPersonaCustom

------------------------------------------------------------------------------*/

/**
 *
 * This file is a management class for admin personae objects in Ganymede.
 *
 */

public class adminPersonaCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  /**
   * <P>This method takes an Invid pointing to an Admin persona
   * record, and returns a string that can be used to send
   * email to that person.  This method will return null
   * if no address could be determined for this administrator.</P>
   */

  static public String convertAdminInvidToString(Invid adminInvid, DBSession session)
  {
    DBObject admin;
    String address;
    int colondex;

    /* -- */

    if (adminInvid.getType() != SchemaConstants.PersonaBase)
      {
	throw new RuntimeException("not an administrator invid");
      }

    if (session == null)
      {
	session = Ganymede.internalSession.getSession();
      }

    admin = session.viewDBObject(adminInvid);

    address = (String) admin.getFieldValueLocal(SchemaConstants.PersonaMailAddr);

    if (address == null)
      {
	// okay, we got no address pre-registered for this
	// admin.. we need now to try to guess at one, by looking
	// to see this admin's name is of the form user:role, in
	// which case we can just try to send to 'user', which will
	// work as long as Ganymede's users cohere with the user names
	// at Ganymede.mailHostProperty.

	String adminName = session.getGSession().viewObjectLabel(adminInvid);

	colondex = adminName.indexOf(':');
	
	if (colondex == -1)
	  {
	    // supergash?

	    return null;
	  }
    
	address = adminName.substring(0, colondex);
      }

    return address;
  }

  /**
   *
   * Customization Constructor
   *
   */

  public adminPersonaCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public adminPersonaCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public adminPersonaCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  // and now the customizations

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
    Vector x = new Vector();

    String address = (String) object.getFieldValueLocal(SchemaConstants.PersonaMailAddr);

    if (address == null)
      {
	// okay, we got no address pre-registered for this
	// admin.. we need now to try to guess at one, by looking
	// to see this admin's name is of the form user:role, in
	// which case we can just try to send to 'user', which will
	// work as long as Ganymede's users cohere with the user names
	// at Ganymede.mailHostProperty.

	String adminName = object.getLabel();

	int colondex = adminName.indexOf(':');
	
	if (colondex == -1)
	  {
	    // supergash?

	    return null;
	  }
    
	address = adminName.substring(0, colondex);
      }

    if (x != null)
      {
	x.addElement(address);
      }

    return x;
  }

  /**
   *
   * We want any change to the 'name' or associated user field to
   * update our hidden label field, which both provides our composite
   * label and does our namespace checks for us.  We do this in
   * finalizeSetValue() so that this operation is always done, even
   * if our GanymedeSession's enableOversight is set to false.
   * 
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    String str, name;
    DBSession session = editset.getSession();
    DBObject obj;
    StringDBField sf;
    InvidDBField inv;
    Invid invid;

    /* -- */

    if (field.getID() == SchemaConstants.PersonaNameField)
      {
	if (debug)
	  {
	    System.err.println("adminPersonaCustom.finalizeSetValue(): setting persona name, refreshing label");
	  }

	return refreshLabelField((String) value, null, null);
      }

    if (field.getID() == SchemaConstants.PersonaAssocUser)
      {
	if (debug)
	  {
	    System.err.println("adminPersonaCustom.finalizeSetValue(): setting persona user, refreshing label");
	  }

	// Hide the associated user field if we are looking at the
	// supergash or monitor persona objects.

	if ((field.getID() == SchemaConstants.PersonaAssocUser) &&
	    (getInvid().getNum() <= 2))
	  {
	    return Ganymede.createErrorDialog("Permissions Error",
					      "It is not permitted to set an associated user on either the supergash " +
					      "or monitor persona objects.");
	  }

	return refreshLabelField(null, (Invid) value, null);
      }

    return null;
  }

  /**
   *
   * This private method is used to keep the hidden label field up-to-date.
   *
   */

  public ReturnVal refreshLabelField(String descrip, Invid userInvid, String newName)
  {
    if (descrip == null)
      {
	StringDBField nameField = (StringDBField) getField(SchemaConstants.PersonaNameField);

	if (nameField != null)
	  {
	    descrip = (String) nameField.getValueLocal();
	  }
      }

    if ((userInvid == null) && (newName == null))
      {
	InvidDBField assocUserField = (InvidDBField) getField(SchemaConstants.PersonaAssocUser);

	if (assocUserField != null)
	  {
	    userInvid = (Invid) assocUserField.getValueLocal();
	  }
      }

    // if we are messing with the supergash or monitor persona
    // objects, don't try to mess around with the associated user.

    if (getInvid().getNum() <= 2)
      {
	return setFieldValueLocal(SchemaConstants.PersonaLabelField, descrip);
      }

    if ((newName == null && userInvid == null) || descrip == null)
      {
	return setFieldValueLocal(SchemaConstants.PersonaLabelField, null);
      }

    if (newName == null)
      {
	newName = this.getGSession().viewObjectLabel(userInvid);
      }

    if (debug)
      {
	System.err.println("Trying to set label to " + newName + ":" + descrip);
      }

    return setFieldValueLocal(SchemaConstants.PersonaLabelField, newName + ":" + descrip);
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    // by default, we return a Short containing the base
    // id for the field's target

    if (field.getID() != SchemaConstants.PersonaPrivs)
      {
	return super.obtainChoicesKey(field);
      }

    return null;		// not going to cache PersonaPrivs field
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (debug)
      {
	System.err.println("Entering adminPersona obtainChoiceList for field " + 
			   field.getName());
      }

    if (field.getID() != SchemaConstants.PersonaPrivs)
      {
	return super.obtainChoiceList(field);
      }

    if (debug)
      {
	System.err.println("Returning adminPersona restricted list");
      }

    if (field.isEditable() && (field instanceof InvidDBField) && 
	!field.isEditInPlace())
      {
	DBObjectBaseField fieldDef;
	short baseId;

	/* -- */

	fieldDef = field.getFieldDef();
	
	baseId = fieldDef.getTargetBase();

	if (baseId < 0)
	  {
	    return null;
	  }

	if (Ganymede.internalSession == null)
	  {
	    return null;
	  }

	// We don't want the Default Role to be shown as a valid
	// choice for this.. everyone has Default implicitly, no point
	// in showing it.

	QueryNode root = new QueryNotNode(new QueryDataNode(QueryDataNode.INVIDVAL,
							    QueryDataNode.EQUALS,
							    new Invid(SchemaConstants.RoleBase,
								      SchemaConstants.RoleDefaultObj)));

	// note that the query we are submitting here *will* be filtered by the
	// current visibilityFilterInvid field in GanymedeSession.

	return editset.getSession().getGSession().query(new Query(baseId, root, true));
      }

    return null;
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
    switch (fieldid)
      {
      case SchemaConstants.PersonaAssocUser:

	// supergash and monitor don't have to have associated users
	// defined.

	if (object.getInvid().getNum() <= 2)
	  {
	    return false;
	  }

      case SchemaConstants.PersonaNameField:
      case SchemaConstants.PersonaPasswordField:
      case SchemaConstants.PersonaLabelField:
	return true;
      }

    return false;
  }

  /**
   *
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.
   *
   * To be overridden in DBEditObject subclasses.
   * 
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // hide the label field.. this cannot be changed by the client,
    // and should be treated as a 'behind-the-scenes' field used to
    // tie things together in the background.

    if (field.getID() == SchemaConstants.PersonaLabelField)
      {
	return false;
      }

    // Hide the associated user field if we are looking at the
    // supergash or monitor persona objects.

    if (field.getID() == SchemaConstants.PersonaAssocUser)
      {
	DBObject object = field.getOwner();

	if (object.getInvid().getNum() <= 2)
	  {
	    return false;
	  }
      }

    return super.canSeeField(session, field);
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   * @param object The object that the link is to be created in
   * @param fieldID The field that the link is to be created in
   *
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    // if they have permission to edit an owner group, who are
    // we to say no?

    if (fieldID == SchemaConstants.PersonaGroupsField)
      {
	return true;
      }

    return false;		// by default, permission is denied
  }

}
