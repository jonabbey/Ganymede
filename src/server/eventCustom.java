/*

   eventCustom.java

   This file is a management class for event-class records in Ganymede.
   
   Created: 9 December 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     eventCustom

------------------------------------------------------------------------------*/

public class eventCustom extends DBEditObject implements SchemaConstants {

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

  public eventCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public eventCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public eventCustom(DBObject original, DBEditSet editset) throws RemoteException
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
    // both fields defined in event are required

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
    if (field.getID() == 104)
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

    if (field.getID() == 104)	// mail list
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
