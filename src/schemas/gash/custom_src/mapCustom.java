/*

   mapCustom.java

   This file is a management class for automounter map objects in Ganymede.
   
   Created: 6 December 1997
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
                                                                       mapCustom

------------------------------------------------------------------------------*/

public class mapCustom extends DBEditObject implements SchemaConstants {

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

  public mapCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public mapCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public mapCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * This method is used to provide a hook to allow different
   * objects to generate different labels for a given object
   * based on their perspective.  This is used to sort
   * of hackishly simulate a relational-type capability for
   * the purposes of viewing backlinks.
   *
   * See the automounter map and NFS volume DBEditObject
   * subclasses for how this is to be used, if you have
   * them.
   *
   */

  public String lookupLabel(DBObject object)
  {
    // we want to create our own, map-centric view of mapEntry objects

    if (object.getTypeID() == 278)
      {
	String userName, volName;
	Invid tmpInvid;
	InvidDBField iField;

	/* -- */


	iField = (InvidDBField) object.getField((short) 0); // containing object, the user
	tmpInvid = iField.value();

	if (editset != null)
	  {
	    userName = editset.getSession().getGSession().viewObjectLabel(tmpInvid);
	  }
	else if (Ganymede.internalSession != null)
	  {
	    userName = Ganymede.internalSession.viewObjectLabel(tmpInvid);
	  }
	else
	  {
	    userName = tmpInvid.toString();
	  }

	iField = (InvidDBField) object.getField((short) 257); // volume invid
	tmpInvid = iField.value();

	if (editset != null)
	  {
	    volName = editset.getSession().getGSession().viewObjectLabel(tmpInvid);
	  }
	else if (Ganymede.internalSession != null)
	  {
	    volName = Ganymede.internalSession.viewObjectLabel(tmpInvid);
	  }
	else
	  {
	    volName = tmpInvid.toString();
	  }


	return userName + " - " + volName;
      }
    else
      {
	return super.lookupLabel(object);
      }
  }

}
