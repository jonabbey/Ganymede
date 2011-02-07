/*

   ownerCustom.java

   This file is a management class for owner-group records in Ganymede.
   
   Created: 9 December 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2011
   The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     ownerCustom

------------------------------------------------------------------------------*/

public class ownerCustom extends DBEditObject implements SchemaConstants {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.ownerCustom");

  /**
   * <P>This method takes an {@link arlut.csd.ganymede.common.Invid Invid} for
   * an Owner Group {@link arlut.csd.ganymede.server.DBObject DBObject}
   * and returns a Vector of Strings containing the list
   * of email addresses for that owner group.</P>
   */

  static public Vector getAddresses(Invid ownerInvid, DBSession session)
  {
    DBObject ownerGroup;
    Vector result = new Vector();
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

    if (externalAddresses != null)
      {
	// we don't have to clone externalAddresses.getValuesLocal()
	// since union() will copy the elements rather than just
	// setting result to the vector returned by
	// externalAddresses.getValuesLocal() if result is currently
	// null.

	result = VectorUtils.union(result, externalAddresses.getValuesLocal());
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
   * return a List of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public List<String> getEmailTargets(DBObject object)
  {
    Set<String> set = new HashSet<String>();
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
	List<Invid> members = (List<Invid>) object.getFieldValuesLocal(SchemaConstants.OwnerMembersField);

	if (members != null)
	  {
	    for (Invid admin: members)
	      {
		DBObject adminObj = session.viewDBObject(admin, true);

		set.addAll((List<String>)adminObj.getEmailTargets());
	      }
	  }
      }

    set.addAll((List<String>) object.getFieldValuesLocal(SchemaConstants.OwnerExternalMail));
    
    return new ArrayList<String>(set);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
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
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
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
   * <p>This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.</p>
   *
   * <p>Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?</p>
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    // we don't want owner groups to ever explicitly list themselves
    // as owners.

    if (field.getID() == SchemaConstants.OwnerListField)
      {
	Invid testInvid = (Invid) value;

	if (testInvid != null && testInvid.equals(field.getOwner().getInvid()))
	  {
	    // "Owner Object Error"
	    // "Can''t make an owner group own itself.  All owner groups implicitly own themselves, anyway."
	    return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
					      ts.l("verifyNewValue.self_ownership"));
	  }
      }

    return super.verifyNewValue(field, value);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * rescind a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.</p>
   *
   * @param object The object that the link is to be removed from
   * @param fieldID The field that the linkk is to be removed from
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

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>See {@link arlut.csd.ganymede.server.DBEditObject#anonymousLinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousLinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousLinkOK() method chaining.</p>
   *
   * <p>Note that the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.</p>
   *
   * <p>The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID)
  {
    // If you can edit an object, you have permission to 'donate' that
    // object to another owner group, even if you're not a member of
    // that owner group.
    //
    // This is so admins can 'give away' objects to another owner
    // group if they need to.
    //
    // Note that we don't use symmetric links for objects owned by
    // owner groups anymore, to avoid locking owner groups all the
    // time when database objects are manipulated in Ganymede.
    // Because of this, we need to check on the BackLinksField
    // constant, even though BackLinksField is virtual these days.

    if (targetFieldID == SchemaConstants.BackLinksField)
      {
	return true;
      }

    return super.anonymousLinkOK(targetObject, targetFieldID);
  }
}
