/*

   mapEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.
   
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
                                                                  mapEntryCustom

------------------------------------------------------------------------------*/

/**
 *
 * This class represents the automounter entry objects that are
 * embedded in user objects in the Ganymede schema.
 *
 */

public class mapEntryCustom extends DBEditObject implements SchemaConstants, mapEntrySchema {

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

    // ok, we want to go ahead and approve the operation,
    // but we want to cause the client to rescan the MAP
    // field in all of our siblings so that their choice
    // list gets updated to not show whatever map *we*
    // just chose.

    // First, we create a ReturnVal that will apply to
    // our siblings

    ReturnVal rescanPlease = new ReturnVal(true); // bool doesn't matter
    rescanPlease.addRescanField(mapEntrySchema.MAP);

    // second, we create a ReturnVal which will cause
    // the field.setValue() call which triggered us
    // to continue normal processing, and return
    // our list of rescan preferences to the client.

    ReturnVal result = new ReturnVal(true, true);
    Vector entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
	result.addRescanObject((Invid) entries.elementAt(i),
			       rescanPlease);
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
    Invid userInvid = (Invid) getFieldValueLocal(mapEntrySchema.CONTAININGUSER);
    DBObject user = getSession().viewDBObject(userInvid);
    return  user.getFieldValuesLocal(userSchema.VOLUMES);
  }
}
