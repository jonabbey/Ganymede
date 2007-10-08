/*

   dhcpOptionCustom.java

   This file is a management class for NFS volume objects in Ganymede.
   
   Created: 8 October 2007
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2007
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.gasharl;

import java.util.Vector;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                dhcpOptionCustom

------------------------------------------------------------------------------*/

public class dhcpOptionCustom extends DBEditObject implements SchemaConstants, dhcpOptionSchema {

  private static QueryResult result = new QueryResult(true);

  static
  {
    result.addRow("flag");
    result.addRow("uint8");
    result.addRow("uint16");
    result.addRow("uint32");
    result.addRow("string");
    result.addRow("text");
    result.addRow("ip-address");
    result.addRow("array of ip-address");
    result.setNonEditable();
  }

  /**
   *
   * Customization Constructor
   *
   */

  public dhcpOptionCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public dhcpOptionCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public dhcpOptionCustom(DBObject original, DBEditSet editset)
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
      case dhcpOptionSchema.OPTIONNAME:
      case dhcpOptionSchema.OPTIONTYPE:
      case dhcpOptionSchema.CUSTOMCODE:
	return object.isSet(dhcpOptionSchema.CUSTOMOPTION);
      }

    return false;
  }


  /**
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of 
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    if (field.getID() == dhcpOptionSchema.CUSTOMCODE)
      {
        return field.getOwner().isSet(dhcpOptionSchema.CUSTOMOPTION);
      }

    return super.canSeeField(session, field);
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
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
   * To be overridden on necessity in DBEditObject subclasses,
   * particularly if you have a StringDBField that you want to force
   * to pick from the list of choices provided by your DBEditObject
   * subclass' obtainChoiceList() method.
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == dhcpOptionSchema.OPTIONTYPE)
      {
	return true;
      }

    return super.mustChoose(field);
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
    if (field.getID() == dhcpOptionSchema.OPTIONTYPE)
      {
        return result;
      }

    return super.obtainChoiceList(field);
  }

  /**
   * This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.server.GanymediatorWizard wizards} when a field's
   * value is being changed.
   *
   * Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.
   *
   * In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.
   *
   * If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean) 
   * enableOversight(false)},
   * this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.
   *
   * This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.
   *
   * This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.
   *
   * The operation parameter will be a small integer, and should hold one of the
   * following values:
   *
   * <dl>
   * <dt>1 - SETVAL</dt>
   * <dd>This operation is used whenever a simple scalar field is having
   * it's value set.  param1 will be the value being placed into the field.</dd>
   * <dt>2 - SETELEMENT</dt>
   * <dd>This operation is used whenever a value in a vector field is being
   * set.  param1 will be an Integer holding the element index, and
   * param2 will be the value being set.</dd>
   * <dt>3 - ADDELEMENT</dt>
   * <dd>This operation is used whenever a value is being added to the
   * end of a vector field.  param1 will be the value being added.</dd>
   * <dt>4 - DELELEMENT</dt>
   * <dd>This operation is used whenever a value in a vector field is being
   * deleted.  param1 will be an Integer holding the element index.</dd>
   * <dt>5 - ADDELEMENTS</dt>
   * <dd>This operation is used whenever a set of elements is being
   * added to a vector field en masse.  param1 will be a Vector containing
   * the values that are being added.</dd>
   * <dt>6 - DELELEMENTS</dt>
   * <dd>This operation is used whenever a set of elements is being
   * deleted from a vector field en masse.  param1 will be a Vector containing
   * the values that are being deleted.</dd>
   * <dt>7 - SETPASSPLAIN</dt>
   * <dd>This operation is used when a password field is having its password
   * set using a plaintext source.  param1 will be a String containing the
   * submitted password, or null if the password is being cleared.</dd>
   * <dt>8 - SETPASSCRYPT</dt>
   * <dd>This operation is used when a password field is having its password
   * set using a UNIX crypt() hashed source.  param1 will be a String containing the
   * submitted hashed password, or null if the password is being cleared.</dd>
   * <dt>9 - SETPASSMD5</dt>
   * <dd>This operation is used when a password field is having its password
   * set using an md5Ccrypt() hashed source.  param1 will be a String containing the
   * submitted hashed password, or null if the password is being cleared.</dd>
   * <dt>10 - SETPASSWINHASHES</dt>
   * <dd>This operation is used when a password field is having its password
   * set using Windows style password hashes.  param1 will be the password in
   * LANMAN hash form, param2 will be the password in NT Unicode MD4 hash
   * form.  Either or both of param1 and param2 may be null.</dd>
   * <dt>11 - SETPASSSSHA</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenLDAP-style SSHA password hash.  param1
   * will be the password in SSHA form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * </dl>
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * @return null if the operation is approved without comment, or a
   * ReturnVal object indicating success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing
   * flag that will indicate to the field code whether or not the
   * operation should continue to completion using the field's
   * standard logic.  <b>It is very important that wizardHook return a
   * new ReturnVal(true, true) if the wizardHook wishes to simply
   * specify rescan information while having the field perform its
   * standard operation.</b> wizardHook() may return new
   * ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true
   * for the respond() method in GanymediatorWizard subclasses.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if (field.getID() == dhcpOptionSchema.CUSTOMOPTION)
      {
        ReturnVal result = new ReturnVal(true);
        result.addRescanField(field.getOwner().getInvid(), dhcpOptionSchema.CUSTOMCODE);

        return result;
      }

    return super.wizardHook(field, operation, param1, param2);
  }
}
