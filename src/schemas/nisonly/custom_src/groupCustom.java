/*

   groupCustom.java

   This file is a management class for group objects in Ganymede.
   
   Created: 30 July 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import arlut.csd.JDialog.JDialogBuff;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     groupCustom

------------------------------------------------------------------------------*/

public class groupCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * group's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public groupCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public groupCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public groupCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * Initialize a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has
   * been created and all fields defined in the
   * controlling DBObjectBase have been instantiated
   * without defined values.<br><br>
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the DBSession
   * associated with the editset defined in this DBEditObject.<br><br>
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return false.  Right now there is no infrastructure in
   * Ganymede to allow the transaction to be aborted from
   * within the DBSession's createDBObject() method.  As a result,
   * if this method is to fail to properly initialize the object,
   * it should be able to not leave an impact on the rest of the
   * DBStore.. in other words, setting InvidField values that
   * involve symmetry relationships could be problematic. <br><br>
   *
   * This method should be overridden in subclasses.
   *
   */

  public boolean initializeNewObject()
  {
    ReturnVal retVal;

    /* -- */

    // need to find a gid for this group

    NumericDBField numField = (NumericDBField) getField((short) 258);

    if (numField == null)
      {
	System.err.println("groupCustom.initializeNewObject(): couldn't get gid field");
	return false;
      }

    DBNameSpace namespace = numField.getNameSpace();

    if (namespace == null)
      {
	System.err.println("groupCustom.initializeNewObject(): couldn't get gid namespace");
	return false;
      }

    // now, find a gid.. unfortunately, we have to use immutable Integers here.. not
    // the most efficient at all.

    Integer gidVal = new Integer(1001);

    while (!namespace.testmark(editset, gidVal))
      {
	gidVal = new Integer(gidVal.intValue()+1);
      }

    // we use setValueLocal so we can set a value that the user can't edit.

    retVal = numField.setValueLocal(gidVal);

    return (retVal == null || retVal.didSucceed());
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
      case groupSchema.GROUPNAME:
      case groupSchema.GID:
	return true;
      }

    return false;
  }

  /**
   *
   * Customization method to verify whether this object type has an inactivation
   * mechanism.
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canBeInactivated()
  {
    return true;
  }

  /**
   *
   * Customization method to verify whether the group has permission
   * to inactivate a given object.  The client's DBSession object
   * will call this per-class method to do an object type-
   * sensitive check to see if this object feels like being
   * available for inactivating by the client.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public boolean canInactivate(DBSession session, DBEditObject object)
  {
    return true;
  }
}
