/*

   adminPersonaCustom.java

   This file is a management class for admin personae objects in Ganymede.
   
   Created: 8 October 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              adminPersonaCustom

------------------------------------------------------------------------------*/

public class adminPersonaCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

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
	str = (String) value;
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
		
		    name = (String) sf.getValue();
		
		    if (!str.startsWith(name + ":"))
		      {
			session.setLastError("persona names must start with username:");
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
    System.err.println("Entering adminPersona obtainChoiceList for field " + field.getName());

    if (field.getID() != SchemaConstants.PersonaPrivs)
      {
	return super.obtainChoiceList(field);
      }

    System.err.println("Returning adminPersona restricted list");

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

	// We don't want the PermSelfUserObj or PermEndUserObj to be shown as valid choices
	// for this 

	QueryNode root = new QueryAndNode(new QueryNotNode(new QueryDataNode(QueryDataNode.INVIDVAL, 
									     QueryDataNode.EQUALS,
									     new Invid(SchemaConstants.PermBase,
										       SchemaConstants.PermSelfUserObj))),
					  new QueryNotNode(new QueryDataNode(QueryDataNode.INVIDVAL,
									     QueryDataNode.EQUALS,
									     new Invid(SchemaConstants.PermBase,
										       SchemaConstants.PermDefaultObj))));

	// note that the query we are submitting here *will* be filtered by the
	// current visibilityFilterInvid field in GanymedeSession.

	return editset.getSession().getGSession().query(new Query(baseId, root, true));
      }

    return null;
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
    if (field.getID() == SchemaConstants.PersonaAssocUser)
      {
	return false;
      }
    else
      {
	return super.canSeeField(session, field);
      }
  }

}
