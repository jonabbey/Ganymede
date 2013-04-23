/*

   mapEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.

   Created: 9 December 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.gasharl;

import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.PermEntry;
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
                                                                  mapEntryCustom

------------------------------------------------------------------------------*/

/**
 * This class represents the automounter entry objects that are
 * embedded in user objects in the Ganymede schema.
 */

public class mapEntryCustom extends DBEditObject implements SchemaConstants, mapEntrySchema {

  static PermEntry noEditPerm = new PermEntry(true, false, false, false);

  // ---

  /**
   *
   * Customization Constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public mapEntryCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public mapEntryCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>Customization method to allow this Ganymede object type to
   * override the default permissions mechanism for special
   * purposes.</p>
   *
   * <p>If this method returns null, the default permissions mechanism
   * will be followed.  If not, the permissions system will grant the
   * permissions specified by this method for access to the given
   * field, and no further elaboration of the permission will be
   * performed.  If permOverride() returns a non-null value for a
   * given field, permExpand() will not be consulted for that field.
   * Just as with permExpand(), this method can never cause greater
   * permissions to be granted to a field than is available to the
   * object as a whole, and this override capability does not
   * apply to operations performed in supergash mode.</p>
   *
   * <p>This method should be used very sparingly.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public PermEntry permOverride(GanymedeSession session, DBObject object, short fieldid)
  {
    if (fieldid != mapEntrySchema.MAP)
      {
        return null;
      }

    DBField field = (DBField) object.getField(fieldid);

    if (field == null)
      {
        return null;
      }

    String label = field.getValueString();

    // XXX Note: this schema assumes that all users will have entries in auto.home.default

    if (label.equals("auto.home.default"))
      {
        return noEditPerm;
      }

    return null;
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p>Note as well that the designated label field for objects are
   * always required, whatever this method returns, and that this
   * requirement holds without regard to the GanymedeSession's
   * enableOversight value.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean fieldRequired(DBObject object, short fieldid)
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
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>The default logic in this method is designed to cause the client
   * to cache choice lists for invid fields in the 'all objects of
   * invid target type' cache bucket.  If your InvidDBField needs to
   * provide a restricted subset of objects of the targeted type as
   * the choice list, you'll need to override this method to either
   * return null (to turn off choice list caching), or generate some
   * kind of unique key that won't collide with the Short objects used
   * to represent the default object list caches.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)}
   * hook, which controls whether or not the default logic will
   * encourage the client to cache a given InvidDBField's choice list.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  @Override public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == mapEntrySchema.MAP)
      {
        return null;
      }

    return super.obtainChoicesKey(field);
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields, filtered by the GanymedeSession's
   * visibilityFilterInvids list.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  @Override public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
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

    Vector<Invid> mapsToSkip = new Vector<Invid>();
    DBObject entry;
    Invid mapInvid;

    Vector<Invid> entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
        entry = getDBSession().viewDBObject(entries.get(i));

        mapInvid = (Invid) entry.getFieldValueLocal(mapEntrySchema.MAP);

        mapsToSkip.add(mapInvid);
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
   * <p>This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.server.GanymediatorWizard wizards} when a field's
   * value is being changed.</p>
   *
   * <p>Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.</p>
   *
   * <p>In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.</p>
   *
   * <p>If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean)
   * enableOversight(false)},
   * this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.</p>
   *
   * <p>This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.</p>
   *
   * <p>This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.</p>
   *
   * <p>The operation parameter will be a small integer, and should hold one of the
   * following values:</p>
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
   * <dt>11 - SETPASSAPACHEMD5</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the Apache variant of the md5crypt algorithm.
   * param1 will be the password in Apache md5crypt hash form, or null
   * if the password hash is being cleared.  param2 will be null.</dd>
   * <dt>12 - SETPASSSSHA</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenLDAP-style SSHA password hash.  param1
   * will be the password in SSHA form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * <dt>13 - SETPASS_SHAUNIXCRYPT</dt>
   * <dd>This operation is used when a password field is having its
   * password set using Ulrich Drepper's SHA256 or SHA512 Unix Crypt
   * algorithms.  param1 will be the password in SHA Unix Crypt form,
   * or null if the password is being cleared.  param2 will be
   * null.</dd>
   * <dt>14 - SETPASS_BCRYPT</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenBSD-style BCrypt password hash.  param1
   * will be the password in BCrypt form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * </dl>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
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

  @Override public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if ((field.getID() != mapEntrySchema.MAP) ||
        (operation != DBEditObject.SETVAL))
      {
        return null;            // by default, we just ok whatever
      }

    InvidDBField invf = (InvidDBField) getField(mapEntrySchema.MAP);

    // if we aren't deleting this entry, reject any attempt to unlink
    // us from auto.home.default, if we are linked there.

    if (!isDeleting() && invf.getValueString().equals("auto.home.default"))
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
    Vector<Invid> entries = getSiblingInvids();

    for (Invid sibling: entries)
      {
        result.addRescanField(sibling, mapEntrySchema.MAP);
      }

    return result;
  }

  /**
   * <p>If this DBEditObject is managing an embedded object, the
   * getEmbeddedObjectLabel() can be overridden to display a synthetic
   * label in the context of viewing or editing the containing object,
   * and when doing queries on the containing type.</p>
   *
   * <p>The getLabel() method will not consult this hook, however, and
   * embedded objects will be represented with their unique label
   * field when processed in an XML context.</p>
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  @Override public final String getEmbeddedObjectDisplayLabelHook(DBObject object)
  {
    InvidDBField field, field2;
    StringBuilder buff = new StringBuilder();

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

  /**
   * <p>Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.</p>
   *
   * <p>Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.</p>
   *
   * <p>If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean canSeeField(DBSession session, DBField field)
  {
    // don't show off our hidden label for direct editing or viewing

    if (field.getID() == mapEntrySchema.XMLLABEL)
      {
        return false;
      }

    return super.canSeeField(session, field);
  }

  private Vector<Invid> getSiblingInvids()
  {
    Vector<Invid> result;
    Invid userInvid = (Invid) getFieldValueLocal(mapEntrySchema.CONTAININGUSER);
    DBObject user = getDBSession().viewDBObject(userInvid);

    result = (Vector<Invid>) user.getFieldValuesLocal(userSchema.VOLUMES).clone();

    // we are not our own sibling.

    result.remove(getInvid());

    return result;
  }

  String getMapName()
  {
    return getDBSession().getObjectLabel((Invid) getFieldValueLocal(mapEntrySchema.MAP));
  }

  String getOriginalMapName()
  {
    return getDBSession().getObjectLabel((Invid) getOriginal().getFieldValueLocal(mapEntrySchema.MAP));
  }

  Invid getMapInvid()
  {
    return (Invid) getFieldValueLocal(mapEntrySchema.MAP);
  }

  Invid getOriginalMapInvid()
  {
    return (Invid) getOriginal().getFieldValueLocal(mapEntrySchema.MAP);
  }

  String getVolumeName()
  {
    return getDBSession().getObjectLabel((Invid) getFieldValueLocal(mapEntrySchema.VOLUME));
  }

  String getOriginalVolumeName()
  {
    return getDBSession().getObjectLabel((Invid) getOriginal().getFieldValueLocal(mapEntrySchema.VOLUME));
  }

  Invid getVolumeInvid()
  {
    return (Invid) getFieldValueLocal(mapEntrySchema.VOLUME);
  }

  Invid getOriginalVolumeInvid()
  {
    return (Invid) getOriginal().getFieldValueLocal(mapEntrySchema.VOLUME);
  }

}
