/*

   ownerCustom.java

   This file is a management class for owner-group records in Ganymede.
   
   Created: 9 December 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

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
   * This method is used to control whether or not it is acceptable to
   * make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   */

  public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    // We want users to be able to do remote link/unlink operations
    // to our objects owned field

    if (fieldID == SchemaConstants.OwnerObjectsOwned)
      {
	return true;
      }
    else
      {
	return false;
      }
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
    if (field.getID() == 103)
      {
	return null;
      }

    return super.obtainChoicesKey(field);
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
    if (field.getID() == 103)	// mail list
      {
	QueryResult result;
	GanymedeSession session = editset.getSession().getGSession();

	/* -- */

	result = session.query(new Query(SchemaConstants.UserBase));
	
	if (result == null)
	  {
	    result = session.query(new Query((short) 274));
	  }
	else
	  {
	    result.append(session.query(new Query((short) 274))); // email list
	  }

	if (result == null)
	  {
	    result = session.query(new Query((short) 275)); // email redirect
	  }
	else
	  {
	    result.append(session.query(new Query((short) 275))); // email redirect
	  }

	return result;
      }

    return super.obtainChoiceList(field);
  }
}
