/*

   personCustom.java

   This file is a management class for person objects in Ganymede.
   
   Created: 25 March 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    personCustom

------------------------------------------------------------------------------*/

public class personCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  // ---

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * interface's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public personCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public personCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public personCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
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
      case personSchema.LASTNAME:
      case personSchema.FIRSTNAME:
	return true;
      }

    return false;
  }

  /**
   *
   * Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type
   *
   */

  public String getLabelHook(DBObject object)
  {
    String lastname, firstname;

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    lastname = (String) object.getFieldValueLocal(personSchema.LASTNAME);
    firstname = (String) object.getFieldValueLocal(personSchema.FIRSTNAME);

    return lastname + ", " + firstname;
  }

  /**
   *
   * Customization method to allow this Ganymede object type to grant
   * permissions above and beyond the default permissions mechanism
   * for special purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the union of the permissions specified by this method for access to the
   * given object.<br><br>
   *
   * This method is essentially different from permOverride() in that
   * the permissions system will not just take the result of this
   * method for an answer, but will grant additional permissions as
   * appropriate.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public PermEntry permExpand(GanymedeSession session, DBObject object)
  {
    PermEntry result = PermEntry.noPerms;
    Vector accounts;
    Invid userInvid;
    DBObject account;
    DBSession dbSession;
    Boolean privileged;

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    if (debug)
      {
	System.err.println("personCustom.permExpand(" + object.getLabel() + ")");
      }

    // ok.. we want to allow access to this person object if the admin
    // is logged in as or has ownership of one of the privileged user
    // accounts connected with this object.

    dbSession = session.getSession();

    accounts = object.getFieldValuesLocal(personSchema.ACCOUNTS);

    // no user accounts?  no permission expansion.

    if (accounts == null)
      {
	return null;
      }

    for (int i = 0; i < accounts.size(); i++)
      {
	userInvid = (Invid) accounts.elementAt(i);
	account = dbSession.viewDBObject(userInvid);

	privileged = (Boolean) account.getFieldValueLocal(userSchema.PRIVILEGED);

	if (privileged != null && privileged.booleanValue())
	  {
	    // ok, if the user is this account or owns this account,
	    // give em permission

	    if (userInvid.equals(session.getUserInvid()))
	      {
		if (debug)
		  {
		    System.err.println("personCustom.permExpand(): Returning full perms for viewing self");
		  }

		return PermEntry.fullPerms; // create doesn't really apply, but this is cheap.
	      }
	    else
	      {
		result = result.union(session.getPerm(account));
	      }
	    
	    if (result.equals(PermEntry.fullPerms))
	      {
		// we've already got full perms, go ahead and return.

		if (debug)
		  {
		    System.err.println("personCustom.permExpand(): Returning full perms for viewing owned person");
		  }

		return result;
	      }
	  }
      }

    if (debug)
      {
	System.err.println("personCustom.permExpand(" + object.getLabel() + ") returning " + result);
      }

    return result;
  }

}
