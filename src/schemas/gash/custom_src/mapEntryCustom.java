/*

   mapEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.
   
   Created: 9 December 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  mapEntryCustom

------------------------------------------------------------------------------*/

/**
 *
 * This class represents the automounter entry objects that are
 * embedded in user objects in the Ganymede schema.
 *
 */

public class mapEntryCustom extends DBEditObject implements SchemaConstants, mapEntrySchema {

  static PermEntry noEditPerm = new PermEntry(true, false, false, false);

  // ---

  /**
   *
   * Customization Constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public mapEntryCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.<br><br>
   *
   * If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant
   * the permissions specified by this method for access to the
   * given field, and no further elaboration of the permission
   * will be performed.  Note that this override capability does
   * not apply to operations performed in supergash mode.<br><br>
   *
   * This method should be used very sparingly.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   */

  public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    if (fieldid != mapEntrySchema.MAP)
      {
	return null;
      }

    DBField field = (DBField) object.getField(fieldid);

    String label = field.getValueString();

    // XXX Note: this schema assumes that all users will have entries in auto.home.default

    if (label.equals("auto.home.default"))
      {
	return noEditPerm;
      }

    return null;
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
      case mapEntrySchema.MAP:
      case mapEntrySchema.VOLUME:
	return true;
      }

    return false;
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
    if (field.getID() == mapEntrySchema.MAP)
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
   * obtainChoiceList() method to get a list of valid choices.<br><br>
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() != mapEntrySchema.MAP)
      {
	return super.obtainChoiceList(field);
      }

    InvidDBField invf = (InvidDBField) getField(mapEntrySchema.MAP);

    if (invf.getValueString().equals("auto.home.default"))
      {
	return new QueryResult(); // can't change a map reference set to auto.home.default
      }

    // ok, we are returning the list of choices for what
    // map to put this entry into.  We don't want it
    // to include any maps that already have an entry
    // for this user.

    // first, get the list of map entries contained in this
    // object.

    Vector mapsToSkip = new Vector();
    DBObject entry;
    Invid mapInvid;

    Vector entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
	entry = getSession().viewDBObject((Invid) entries.elementAt(i));

	mapInvid = (Invid) entry.getFieldValueLocal(mapEntrySchema.MAP);

	mapsToSkip.addElement(mapInvid);
      }
    
    // ok, mapsToSkip has a list of invid's to skip in our choice list.

    QueryResult result = new QueryResult();
    QueryResult baseList = super.obtainChoiceList(field);

    for (int i = 0; i < baseList.size(); i++)
      {
	mapInvid = baseList.getInvid(i);

	if (!mapsToSkip.contains(mapInvid))
	  {
	    result.addRow(baseList.getObjectHandle(i));
	  }
      }
    
    return result;
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards when
   * a field's value is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if ((field.getID() != mapEntrySchema.MAP) ||
	(operation != DBEditObject.SETVAL))
      {
	return null;		// by default, we just ok whatever
      }

    InvidDBField invf = (InvidDBField) getField(mapEntrySchema.MAP);

    // if we aren't deleting this entry, reject any attempt to unlink
    // us from auto.home.default, if we are linked there.

    if (!deleting &&invf.getValueString().equals("auto.home.default"))
      {
	return Ganymede.createErrorDialog("Error, auto.home.default is required",
					  "Sorry, it is mandatory to have a directory entry on the auto.home.default map.");
      }
  
    // ok, we want to go ahead and approve the operation, but we want
    // to cause the client to rescan the MAP field in all of our
    // siblings so that their choice list gets updated to not show
    // whatever map *we* just chose.

    // Create a ReturnVal which will cause the field.setValue() call
    // which triggered us to continue normal processing, and return
    // our list of rescan preferences to the client.

    ReturnVal result = new ReturnVal(true, true);
    Vector entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
	result.addRescanField((Invid) entries.elementAt(i), mapEntrySchema.MAP);
      }

    return result;
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
    InvidDBField field, field2;
    StringBuffer buff = new StringBuffer();

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
	return null;
      }

    field = (InvidDBField) object.getField(MAP); // map name
    field2 = (InvidDBField) object.getField(VOLUME); // volume

    try
      {
	if (field != null)
	  {
	    buff.append(field.getValueString() + ":");
	  }

	if (field2 != null)
	  {
	    buff.append(field2.getValueString());
	  }
      }
    catch (IllegalArgumentException ex)
      {
	buff.append("<?:?>");
      }

    return buff.toString();
  }

  private Vector getSiblingInvids()
  {
    Vector result;
    Invid userInvid = (Invid) getFieldValueLocal(mapEntrySchema.CONTAININGUSER);
    DBObject user = getSession().viewDBObject(userInvid);

    result = (Vector) user.getFieldValuesLocal(userSchema.VOLUMES).clone();
    
    // we are not our own sibling.

    result.removeElement(getInvid());

    return result;
  }
}
