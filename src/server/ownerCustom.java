/*

   ownerCustom.java

   This file is a management class for owner-group records in Ganymede.
   
   Created: 9 December 1997
   Version: $Revision: 1.7 $ %D%
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
    // We really don't want the supergash owner group from ever
    // having any explicit ownership links.

    if (field.getOwner().getInvid().getNum() == SchemaConstants.OwnerSupergash &&
	getID() == SchemaConstants.OwnerObjectsOwned)
      {
	return false;
      }

    return super.verifyNewValue(field, value);
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID, GanymedeSession gsession)
  {
    // If an admin is a member of our group, we'll let them link
    // in objects to us, otherwise, forget it.

    if (fieldID == SchemaConstants.OwnerObjectsOwned)
      {
	if (object.getInvid().getNum() == SchemaConstants.OwnerSupergash)
	  {
	    // we don't ever want to explicitly list objects under
	    // the supergash owner group.

	    return false;
	  }

	if (gsession.isSuperGash())
	  {
	    return true;
	  }

	Vector tmpInvidList = new Vector();
	tmpInvidList.addElement(object.getInvid());

	if (gsession.isMemberAll(tmpInvidList))
	  {
	    return true;
	  }
      }
    
    return false;
  }
}
