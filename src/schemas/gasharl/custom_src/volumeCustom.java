/*

   volumeCustom.java

   This file is a management class for NFS volume objects in Ganymede.
   
   Created: 6 December 1997
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 2003/03/12 03:48:41 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    volumeCustom

------------------------------------------------------------------------------*/

public class volumeCustom extends DBEditObject implements SchemaConstants, volumeSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public volumeCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public volumeCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public volumeCustom(DBObject original, DBEditSet editset)
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
    switch (fieldid)
      {
      case volumeSchema.LABEL:
      case volumeSchema.HOST:
      case volumeSchema.PATH:
	return true;
      }

    return false;
  }

  /**
   *
   * Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    Invid hostInvid = (Invid) object.getFieldValueLocal(volumeSchema.HOST);

    if (hostInvid != null &&
	gSession.personaMatch(gSession.getSession().viewDBObject(hostInvid)))
      {
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
   * calling choices() on this object again.
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == volumeSchema.ENTRIES)
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

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() == volumeSchema.ENTRIES)
      {
	return null;	// no choices for imbeddeds
      }

    return super.obtainChoiceList(field);
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
    if (fieldID == volumeSchema.ENTRIES)
      {
	return true;
      }

    return false;
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
	String mapName, userName;
	Invid tmpInvid;
	InvidDBField iField;

	/* -- */

	iField = (InvidDBField) object.getField(mapEntrySchema.MAP); // map invid
	tmpInvid = iField.value();

	if (editset != null)
	  {
	    mapName = editset.getSession().getGSession().viewObjectLabel(tmpInvid);
	  }
	else if (Ganymede.internalSession != null)
	  {
	    mapName = Ganymede.internalSession.viewObjectLabel(tmpInvid);
	  }
	else
	  {
	    mapName = tmpInvid.toString();
	  }

	iField = (InvidDBField) object.getField(mapEntrySchema.CONTAININGUSER); // containing object, the user
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

	return userName + ":" + mapName;
      }
    else
      {
	return super.lookupLabel(object);
      }
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards when
   * a field's value is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    try
      {
	if (field.getID() != volumeSchema.ENTRIES)
	  {
	    return null;		// by default, we just ok whatever
	  }

	// ok, they are trying to mess with the embedded list.. we really
	// can't allow any deletions from the default map.  If the entry
	// isn't in the default map, we'll allow deletions, but we have to
	// do it by editing the user referenced in the entry specified,
	// and deleting the entry reference in the embedded vector.

	switch (operation)
	  {
	  case DELELEMENT:

	    int index = ((Integer) param1).intValue();

	    Vector entries = getFieldValuesLocal(volumeSchema.ENTRIES);

	    if (entries == null)
	      {
		return Ganymede.createErrorDialog("Logic error in server",
						  "volumeCustom.wizardHook(): can't delete element out of empty field");
	      }
	
	    Invid invid = null;	// the invid for the entry

	    try
	      {
		invid = (Invid) entries.elementAt(index);
	      }
	    catch (ClassCastException ex)
	      {
		return Ganymede.createErrorDialog("Logic error in server",
						  "volumeCustom.wizardHook(): unexpected element in entries field");
	      }
	    catch (ArrayIndexOutOfBoundsException ex)
	      {
		return Ganymede.createErrorDialog("Logic error in server",
						  "volumeCustom.wizardHook(): deleteElement index out of range in entries field");
	      }

	    DBObject vObj = getSession().viewDBObject(invid); // should be a mapEntry object

	    InvidDBField invf = (InvidDBField) vObj.getField(mapEntrySchema.MAP);

	    String mapName = invf.getValueString();

	    if (mapName != null && mapName.equals("auto.home.default"))
	      {
		return Ganymede.createErrorDialog("Cannot remove entries from auto.home.default",
						  "Error, cannot remove entries from auto.home.default. " +
						  "All UNIX users in Ganymede must have an entry in auto.home.default " +
						  "to represent the location of their home directory.");
	      }
	    else
	      {
		// we'll allow the deletion, but we're going to do it the
		// right way here, rather than having
		// DBField.deleteElement() try to do it in its naive
		// fashion.
	    
		// we need to get the user

		Invid user = (Invid) vObj.getFieldValueLocal(mapEntrySchema.CONTAININGUSER);

		// and we need to edit the user.. we'll want to check permissions
		// for this, so we'll use edit_db_object()

		DBEditObject eObj = (DBEditObject) (getGSession().edit_db_object(user).getObject());

		if (eObj == null)
		  {
		    return Ganymede.createErrorDialog("Couldn't remove map entry",
						      "Couldn't remove map entry for " + 
						      getGSession().viewObjectLabel(user) +
						      ", permissions denied to edit the user.");
		  }

		invf = (InvidDBField) eObj.getField(userSchema.VOLUMES);

		if (invf == null)
		  {
		    return Ganymede.createErrorDialog("Couldn't remove map entry",
						      "Couldn't remove map entry for " + 
						      getGSession().viewObjectLabel(user) +
						      ", couldn't access the volumes field in the user record.");
		  }

		// by doing the deleteElement on the field containing
		// the embedded volume object, we will let the user
		// object take care of deleting the embedded volume
		// object.  The invid linking system will then take care
		// of removing it from the map object's MAPENTRIES field.

		ReturnVal retVal = invf.deleteElement(invid);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
		else
		  {
		    return new ReturnVal(true);
		  }
	      }
	  }

	return null;
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.loginError(ex);
      }
  }
}
