/*

   networkCustom.java

   This file is a management class for IP network objects in Ganymede.
   
   Created: 20 May 1998
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2001/04/25 19:23:59 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   networkCustom

------------------------------------------------------------------------------*/

public class networkCustom extends DBEditObject {

  /**
   *
   * Customization Constructor
   *
   */

  public networkCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public networkCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public networkCustom(DBObject original, DBEditSet editset)
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
    if (fieldid == networkSchema.NAME)
      {
	return true;
      }

    // the network number is only required if the allocrange is not
    // set

    if (fieldid == networkSchema.NETNUMBER)
      {
	DBField allocField = (DBField) object.getField(networkSchema.ALLOCRANGE);

	if (allocField == null || !allocField.isDefined())
	  {
	    return true;
	  }
      }

    // the allocrange is only required if the network number is not
    // set

    if (fieldid == networkSchema.ALLOCRANGE)
      {
	DBField netnumField = (DBField) object.getField(networkSchema.NETNUMBER);

	if (netnumField == null || !netnumField.isDefined())
	  {
	    return true;
	  }
      }

    return false;
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
    if ((fieldID == networkSchema.INTERFACES) ||
	(fieldID == networkSchema.ROOMS))
      {
	return true;
      }

    return false;
  }

  /**
   * This method is the hook that DBEditObject subclasses use to interpose
   * wizards when a field's value is being changed.<br><br>
   *
   * Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.<br><br>
   *
   * In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.<br><br>
   *
   * If server-local code has called
   * GanymedeSession.enableOversight(false), this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.<br><br>
   *
   * This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.
   *
   * @return a ReturnVal object indicated success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing flag
   * that will indicate to the field code whether or not the operation
   * should continue to completion using the field's standard logic.
   * <b>It is very important that wizardHook return a new ReturnVal(true, true)
   * if the wizardHook wishes to simply specify rescan information while
   * having the field perform its standard operation.</b>  wizardHook() may
   * return new ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true for the
   * respond() method in GanymediatorWizard subclasses.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if (field.getID() == networkSchema.INTERFACES)
      {
	if ((operation == DELELEMENT || operation == DELELEMENTS) &&
	    getStatus() != DBEditObject.DELETING && getStatus() != DBEditObject.DROPPING)
	  {
	    return Ganymede.createErrorDialog("Sorry, you can't delete interfaces here",
					      "You can't delete interfaces from the network object.  If you were " +
					      "to do so, the interface you are attempting to delete would be left " +
					      "without a network connection.  Please edit the system containing the " +
					      "interface on this net directly to change its network connection.");
	  }
      }

    return null;		// by default, we just ok whatever
  }

  /**
   * <p>Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    // I.P. Network objects have type id of 267

    if (object.getTypeID() != 267)
      {
	return false;
      }

    // we want to grant ownership if the "Public Network" checkbox
    // is selected

    Boolean value = (Boolean) object.getFieldValueLocal(networkSchema.PUBLICNETWORK);

    if (value == null || !value.booleanValue())
      {
	return false;
      }

    return true;
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
    // no choices for interfaces.. make them edit the system

    if (field.getID() == networkSchema.INTERFACES)
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
    // no choices for interfaces.. make them edit the system

    if (field.getID() == networkSchema.INTERFACES)
      {
	return null;	
      }

    return super.obtainChoiceList(field);
  }

  /**
   *
   * This method is used to provide a hook to allow different
   * objects to generate different labels for a given object
   * based on their perspective.  This is used to sort
   * of hackishly simulate a relational-type capability for
   * the purposes of viewing backlinks.<br><br>
   *
   * See the automounter map and NFS volume DBEditObject
   * subclasses for how this is to be used, if you have
   * them.
   *
   */

  public String lookupLabel(DBObject object)
  {
    // we want to create our own, network-centric view of interface objects
    //
    // single host system interface objects will not have a name of
    // their own.. in such a case, we'll want to get the name of the
    // system to display.

    if (object.getTypeID() == 265)
      {
	InvidDBField iField;
	Invid tmpInvid;
	String sysName;

	/* -- */

	String name = (String) object.getFieldValueLocal(interfaceSchema.NAME);

	if (name != null)
	  {
	    return name;
	  }

	iField = (InvidDBField) object.getField((short) 0); // containing object, the system
	tmpInvid = iField.value();

	if (editset != null)
	  {
	    sysName = editset.getSession().getGSession().viewObjectLabel(tmpInvid);
	  }
	else if (Ganymede.internalSession != null)
	  {
	    sysName = Ganymede.internalSession.viewObjectLabel(tmpInvid);
	  }
	else
	  {
	    sysName = tmpInvid.toString();
	  }

	return sysName;
      }
    else
      {
	return super.lookupLabel(object);
      }
  }
}
