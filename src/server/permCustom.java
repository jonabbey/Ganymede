/*

   permCustom.java

   This file is a management class for event-class records in Ganymede.
   
   Created: 21 January 1998
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
                                                                      permCustom

------------------------------------------------------------------------------*/

public class permCustom extends DBEditObject implements SchemaConstants {

  /**
   *
   * Customization Constructor
   *
   */

  public permCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public permCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public permCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * This method provides a hook that can be used to indicate whether
   * a field that is defined in this object's field dictionary
   * should be newly instantiated in this particular object.
   *
   * This method does not affect those fields which are actually present
   * in the object's record in the DBStore.  What this method allows
   * you to do is have a subclass decide whether it wants to instantiate
   * a potential field (one that is declared in the field dictionary for
   * this object, but which doesn't happen to be presently defined in
   * this object) in this particular object.
   *
   * A concrete example will help here.  The Permissions Object type
   * (base number SchemaConstants.PermBase) holds a permission
   * matrix, a descriptive title, and a list of admin personae that hold
   * those permissions for objects they own.
   *
   * There are a few specific instances of SchemaConstants.PermBase
   * that don't properly need the list of admin personae, as their
   * object invids are hard-coded into the Ganymede security system, and
   * their permission matrices are automatically consulted in certain
   * situations.  In order to support this, we're going to want to have
   * a DBEditObject subclass for managing permission objects.  In that
   * subclass, we'll define instantiateNewField() so that it will return
   * false if the fieldID corresponds to the admin personae list if the
   * object's ID is that of one of these special objects.  As a result,
   * when the objects are viewed by an administrator, the admin personae
   * list will not be seen.
   *
   */

  public boolean instantiateNewField(short fieldID)
  {
    if (fieldID == SchemaConstants.PermPersonae)
      {
	if (id == SchemaConstants.PermDefaultObj)
	  {
	    return false;
	  }
      }

    return true;
  }

}
