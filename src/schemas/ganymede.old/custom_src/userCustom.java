/*

   userCustom.java

   This file is a management class for user objects in Ganymede.
   
   Created: 30 July 1997
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
                                                                     userCustom

------------------------------------------------------------------------------*/

public class userCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;
  static StringBuffer shellChoices = new StringBuffer();
  static Date shellChoiceStamp = null;

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

  public userCustom() throws RemoteException
  {
  }

  /**
   *
   * Create new object constructor
   *
   */

  public userCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public userCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   * 
   */

  public StringBuffer obtainChoiceList(DBField field)
  {
    if (!field.getName().equals("Shell"))
      {
	return super.obtainChoiceList(field);
      }

    synchronized (shellChoices)
      {
	DBObjectBase base = Ganymede.db.getObjectBase("Shell Choice");

	// just go ahead and throw the null pointer if we didn't get our base.

	if (shellChoiceStamp == null || shellChoiceStamp.before(base.getTimeStamp()))
	  {
	    shellChoices.setLength(0);
	    shellChoices.append(objectDumpHeader(false)); // non-invid bearing string

	    Query query = new Query("Shell Choice", null, false);
	    Vector results = internalSession().internalQuery(query);
	
	    for (int i = 0; i < results.size(); i++)
	      {
		shellChoices.append(stringDump(((Result) results.elementAt(i)).toString())); // no invid
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

    if (debug)
      {
	System.err.println("userCustom: obtainChoice returning " + shellChoices + " for shell field.");
      }

    return shellChoices;
  }

}
