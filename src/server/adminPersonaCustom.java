/*

   adminPersonaCustom.java

   This file is a management class for admin personae objects in Ganymede.
   
   Created: 8 October 1997
   Version: $Revision: 1.11 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
   *
   * We want any change to the 'name' or associated user field to
   * update our hidden label field, which both provides our composite
   * label and does our namespace checks for us.  We do this in
   * finalizeSetValue() so that this operation is always done, even
   * if our GanymedeSession's enableOversight is set to false.
   * 
   */

  public boolean finalizeSetValue(DBField field, Object value)
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
	return refreshLabelField((String) value, null);
      }

    if (field.getID() == SchemaConstants.PersonaAssocUser)
      {
	return refreshLabelField(null, (Invid) value);
      }

    return true;
  }

  /**
   *
   * This private method is used to keep the hidden label field up-to-date.
   *
   */

  private boolean refreshLabelField(String descrip, Invid userInvid)
  {
    ReturnVal result;

    if (descrip == null)
      {
	StringDBField nameField = (StringDBField) getField(SchemaConstants.PersonaNameField);

	if (nameField != null)
	  {
	    descrip = (String) nameField.getValueLocal();
	  }
      }

    if (userInvid == null)
      {
	InvidDBField assocUserField = (InvidDBField) getField(SchemaConstants.PersonaAssocUser);

	if (assocUserField != null)
	  {
	    userInvid = (Invid) assocUserField.getValueLocal();
	  }
      }

    if (getInvid().getNum() <= 2)
      {
	result = setFieldValueLocal(SchemaConstants.PersonaLabelField, descrip);

	return (result == null || result.didSucceed());
      }

    if (userInvid == null || descrip == null)
      {
	result = setFieldValueLocal(SchemaConstants.PersonaLabelField, null);

	return (result == null || result.didSucceed());
      }

    String username = this.getGSession().viewObjectLabel(userInvid);

    result = setFieldValueLocal(SchemaConstants.PersonaLabelField, username + ":" + descrip);

    return (result == null || result.didSucceed());
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
    else
      {
	return super.canSeeField(session, field);
      }
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
