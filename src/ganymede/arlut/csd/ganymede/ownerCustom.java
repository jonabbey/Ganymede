/*

   ownerCustom.java

   This file is a management class for owner-group records in Ganymede.
   
   Created: 9 December 1997
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 2002/10/09 01:19:54 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

package arlut.csd.ganymede;

import arlut.csd.Util.*;

import java.util.*;
import java.rmi.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     ownerCustom

------------------------------------------------------------------------------*/

public class ownerCustom extends DBEditObject implements SchemaConstants {

  /**
   * <P>This method takes an {@link arlut.csd.ganymede.Invid Invid} for
   * an Owner Group {@link arlut.csd.ganymede.DBObject DBObject}
   * and returns a Vector of Strings containing the list
   * of email addresses for that owner group.</P>
   */

  static public Vector getAddresses(Invid ownerInvid, DBSession session)
  {
    DBObject ownerGroup;
    Vector result = new Vector();
    InvidDBField emailInvids;
    StringDBField externalAddresses;

    /* -- */

    if (session == null)
      {
	session = Ganymede.internalSession.getSession();
      }

    ownerGroup = session.viewDBObject(ownerInvid);

    if (ownerGroup == null)
      {
	if (debug)
	  {
	    System.err.println("getOwnerGroupAddresses(): Couldn't look up owner group " + 
			       ownerInvid.toString());
	  }
	
	return result;
      }

    // should we cc: the admins?

    Boolean cc = (Boolean) ownerGroup.getFieldValueLocal(SchemaConstants.OwnerCcAdmins);

    if (cc != null && cc.booleanValue())
      {
	Vector adminList = new Vector();
	Vector adminInvidList;
	Invid adminInvid;
	String adminAddr;

	adminInvidList = ownerGroup.getFieldValuesLocal(SchemaConstants.OwnerMembersField);

	if (adminInvidList != null)
	  {
	    for (int i = 0; i < adminInvidList.size(); i++)
	      {
		adminInvid = (Invid) adminInvidList.elementAt(i);
		adminAddr = adminPersonaCustom.convertAdminInvidToString(adminInvid, session);
		
		if (adminAddr != null)
		  {
		    adminList.addElement(adminAddr);
		  }
	      }
	  }

	result = VectorUtils.union(result, adminList);
      }

    // do we have any external addresses?

    externalAddresses = (StringDBField) ownerGroup.getField(SchemaConstants.OwnerExternalMail);

    if (externalAddresses == null)
      {
	if (debug)
	  {
	    System.err.println("getOwnerGroupAddresses(): No external mail list defined for owner group " + 
			       ownerInvid.toString());
	  }
      }
    else
      {
	// we don't have to clone externalAddresses.getValuesLocal()
	// since union() will copy the elements rather than just
	// setting result to the vector returned by
	// externalAddresses.getValuesLocal() if result is currently
	// null.

	result = VectorUtils.union(result, externalAddresses.getValuesLocal());
      }

    if (debug)
      {
	System.err.print("getOwnerGroupAddresses(): returning: ");

	for (int i = 0; i < result.size(); i++)
	  {
	    if (i > 0)
	      {
		System.err.print(", ");
	      }

	    System.err.print(result.elementAt(i));
	  }

	System.err.println();
      }

    return result;
  }


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

  // and now the customizations

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * indicate that the given object is interested in receiving notification
   * when changes involving it occur, and can provide one or more addresses for
   * such notification to go to.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean hasEmailTarget(DBObject object)
  {
    return true;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * return a Vector of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public Vector getEmailTargets(DBObject object)
  {
    Vector x = new Vector();
    DBSession session;
    Boolean cc = (Boolean) object.getFieldValueLocal(SchemaConstants.OwnerCcAdmins);

    /* -- */

    try
      {
	session = object.getGSession().getSession();
      }
    catch (NullPointerException ex)
      {
	session = Ganymede.internalSession.getSession();
      }
    
    if (cc != null && cc.booleanValue())
      {
	Vector members = object.getFieldValuesLocal(SchemaConstants.OwnerMembersField);

	if (members != null)
	  {
	    for (int i = 0; i < members.size(); i++)
	      {
		Invid admin = (Invid) members.elementAt(i);
		
		DBObject adminObj = session.viewDBObject(admin, true);
		
		x = VectorUtils.union(x, adminObj.getEmailTargets());
	      }
	  }
      }

    x = VectorUtils.union(x, object.getFieldValuesLocal(SchemaConstants.OwnerExternalMail));
    
    return x;
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
   */

  public boolean mustChoose(DBField field)
  {
    // We don't force a choice on the object owned field, because
    // it can point to anything.
    
    if (field.getID() == SchemaConstants.OwnerObjectsOwned)
      {
	return false;
      }

    return super.mustChoose(field);
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
    // We want to force the client to check the field choices here,
    // since the choices will never include itself as a valid choice.

    if (field.getID() == SchemaConstants.OwnerListField)
      {
	return null;
      }

    return super.obtainChoicesKey(field);
  }

  /**
   *
   * This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.<br><br>
   *
   * Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?
   * 
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    // We really don't want the supergash owner group ever
    // having any explicit ownership links.

    if (field.getOwner().getID() == SchemaConstants.OwnerSupergash &&
	getID() == SchemaConstants.OwnerObjectsOwned)
      {
	return Ganymede.createErrorDialog("Owner Object Error",
					  "Can't modify supergash objects owned field.");
      }

    // we don't want owner groups to ever explicitly list themselves
    // as owners.

    if ((field.getID() == SchemaConstants.OwnerObjectsOwned) ||
	(field.getID() == SchemaConstants.OwnerListField))
      {
	Invid testInvid = (Invid) value;

	if (testInvid != null && testInvid.equals(field.getOwner().getInvid()))
	  {
	    return Ganymede.createErrorDialog("Owner Object Error",
					      "Can't make an owner group own itself.. this is implicitly true");
	  }
      }

    return super.verifyNewValue(field, value);
  }

  /**
   *
   * This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
   *
   */

  public boolean anonymousUnlinkOK(DBObject object, short fieldID)
  {
    // In order to take an admin out of an owner group, you have
    // to have permission to edit that owner group, as well as
    // the admin.

    if (fieldID == SchemaConstants.OwnerMembersField)
      {
	return false;
      }
    
    return super.anonymousUnlinkOK(object, fieldID);
  }
}
