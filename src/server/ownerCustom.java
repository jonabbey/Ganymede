/*

   ownerCustom.java

   This file is a management class for owner-group records in Ganymede.
   
   Created: 9 December 1997
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     ownerCustom

------------------------------------------------------------------------------*/

public class ownerCustom extends DBEditObject implements SchemaConstants {

  /**
   *
   * Customization Constructor
   *
   */

  public ownerCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public ownerCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public ownerCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
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
    // We don't force a choice on the object owned field, because
    // it can point to anything.
    
    if (field.getID() == SchemaConstants.OwnerObjectsOwned)
      {
	return false;
      }

    return super.mustChoose(field);
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    // We want to force the client to check the field choices here,
    // since the choices will never include itself as a valid choice.

    if (field.getID() == SchemaConstants.OwnerListField)
      {
	return null;
      }

    return super.obtainChoicesKey(field);
  }

  /**
   *
   * This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.<br><br>
   *
   * Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?
   * 
   */

  public boolean verifyNewValue(DBField field, Object value)
  {
    // We really don't want the supergash owner group ever
    // having any explicit ownership links.

    if (field.getOwner().getInvid().getNum() == SchemaConstants.OwnerSupergash &&
	getID() == SchemaConstants.OwnerObjectsOwned)
      {
	field.setLastError("Can't modify supergash objects owned field");
	Ganymede.debug("Can't modify supergash objects owned field");
	return false;
      }

    // we don't want owner groups to ever explicitly list themselves
    // as owners.

    if ((field.getID() == SchemaConstants.OwnerObjectsOwned) ||
	(field.getID() == SchemaConstants.OwnerListField))
      {
	Invid testInvid = (Invid) value;

	if (testInvid != null && testInvid.equals(field.getOwner().getInvid()))
	  {
	    field.setLastError("Can't make an owner group own itself.. this is implicitly true");
	    return false;
	  }
      }

    return super.verifyNewValue(field, value);
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   *
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID)
  {
    // In order to take an admin out of an owner group, you have
    // to have permission to edit that owner group, as well as
    // the admin.

    if (fieldID == SchemaConstants.OwnerMembersField)
      {
	return false;
      }
    
    return super.anonymousUnlinkOK(object, fieldID);
  }
}
