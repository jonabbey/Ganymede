/*

   adminPersonaCustom.java

   This file is a management class for admin personae objects in Ganymede.
   
   Created: 8 October 1997
   Version: $Revision: 1.10 $ %D%
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
   * This method is the hook that DBEditObject subclasses use to interpose
   * wizards when a field's value is being changed.<br><br>
   *
   * Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.<br><br>
   *
   * In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.<br><br>
   *
   * If server-local code has called
   * GanymedeSession.enableOversight(false), this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.<br><br>
   *
   * This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.<br><br>
   *
   * This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * the finalize*() methods have no power to set object/field rescan
   * or return dialogs to the client, however.. in cases where such
   * is necessary, a custom plug-in class must have wizardHook() and
   * finalize*() configured to work together to both provide proper field
   * rescan notification and to check the operation being performed and
   * make any changes necessary to other fields and/or objects.<br><br>
   *
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.
   *
   * @return a ReturnVal object indicated success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing flag
   * that will indicate to the field code whether or not the operation
   * should continue to completion using the field's standard logic.
   * <b>It is very important that wizardHook return a new ReturnVal(true, true)
   * if the wizardHook wishes to simply specify rescan information while
   * having the field perform its standard operation.</b>  wizardHook() may
   * return new ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true for the
   * respond() method in GanymediatorWizard subclasses.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    // if we are being deleted, go ahead and approve whatever.

    if (deleting)
      {
	return null;
      }

    // otherwise, if they aren't setting the associated user field,
    // complain if the associated user isn't set.

    if (field.getID() != SchemaConstants.PersonaAssocUser)
      {
	DBField assocUser = (DBField) getField(SchemaConstants.PersonaAssocUser);

	if (assocUser == null || !assocUser.isDefined())
	  {
	    return Ganymede.createErrorDialog("Client Error",
					      "Error, the client has not set the associated user for " +
					      "this admin persona.  The client is supposed to handle linking " +
					      "this admin persona with a user.  Something's wrong on the client.");
	  }
      }

    return null;		// by default, we just ok whatever
  }

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
	// whoops, looks like our persona name is being changed.. make
	// sure that it's okay to do that.

	str = (String) value;

	// if we are being deleted, sure, we're ok with the persona name
	// being cleared.

	if (str == null && deleting)
	  {
	    return true;
	  }

	inv = (InvidDBField) getField(SchemaConstants.PersonaAssocUser);

	if (inv != null)
	  {
	    invid = (Invid) inv.getValue();
	    
	    if (invid != null)
	      {
		obj = session.viewDBObject(invid);

		if (obj != null)
		  {
		    sf = (StringDBField) obj.getField(SchemaConstants.UserUserName);
		
		    name = (String) sf.getNewValue();

		    // now, if we weren't called from inside the user rename
		    // logic, getNewValue() will be null.  Check it out.

		    if (name == null)
		      {
			name = (String) sf.getValue();
		      }
		
		    if (!str.startsWith(name + ":"))
		      {
			session.setLastError("persona names must start with username:, not " + name);
			return false;
		      }
		  }
		else
		  {
		    session.setLastError("adminPersona customizer: can't find associated user");
		  }
	      }
	    else
	      {
		session.setLastError("adminPersona customizer: no associated user set");
	      }
	  }
	else
	  {
	    session.setLastError("adminPersona customizer: no associated user set");
	  }
      }

    return true;
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
      case SchemaConstants.PersonaNameField:
      case SchemaConstants.PersonaPasswordField:
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
    // hide the associated user field.. this cannot be changed by
    // the client, and should be treated as a 'behind-the-scenes'
    // field used to tie things together in the background.

    if (field.getID() == SchemaConstants.PersonaAssocUser)
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
