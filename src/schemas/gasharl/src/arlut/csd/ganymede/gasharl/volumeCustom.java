/*

   volumeCustom.java

   This file is a management class for NFS volume objects in Ganymede.

   Created: 6 December 1997

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
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;

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
      case volumeSchema.LABEL:
      case volumeSchema.HOST:
      case volumeSchema.PATH:
        return true;
      }

    return false;
  }

  /**
   * <p>Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    Invid hostInvid = (Invid) object.getFieldValueLocal(volumeSchema.HOST);

    if (hostInvid != null &&
        gSession.getPermManager().personaMatch(gSession.getDBSession().viewDBObject(hostInvid)))
      {
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
    if (field.getID() == volumeSchema.ENTRIES)
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
    if (field.getID() == volumeSchema.ENTRIES)
      {
        return null;    // no choices for imbeddeds
      }

    return super.obtainChoiceList(field);
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

  @Override public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    if (fieldID == volumeSchema.ENTRIES)
      {
        return true;
      }

    return false;
  }

  /**
   * <p>This method is used to provide a hook to allow different
   * objects to generate different labels for a given object based on
   * their perspective.  This is used to sort of hackishly simulate a
   * relational-type capability for the purposes of viewing
   * backlinks.</p>
   *
   * <p>See the automounter map and NFS volume DBEditObject subclasses
   * for how this is to be used, if you have them.</p>
   */

  public String lookupLabel(DBObject object)
  {
    // we want to create our own, map-centric view of mapEntry objects

    if (object.getTypeID() == mapEntrySchema.BASE)
      {
        String mapName, userName;
        Invid tmpInvid;
        InvidDBField iField;

        /* -- */

        iField = (InvidDBField) object.getField(mapEntrySchema.MAP); // map invid
        tmpInvid = iField.value();

        if (editset != null)
          {
            mapName = editset.getDBSession().getObjectLabel(tmpInvid);
          }
        else if (Ganymede.internalSession != null)
          {
            mapName = Ganymede.internalSession.getDBSession().getObjectLabel(tmpInvid);
          }
        else
          {
            mapName = tmpInvid.toString();
          }

        iField = (InvidDBField) object.getField(mapEntrySchema.CONTAININGUSER); // containing object, the user
        tmpInvid = iField.value();

        if (editset != null)
          {
            userName = editset.getDBSession().getObjectLabel(tmpInvid);
          }
        else if (Ganymede.internalSession != null)
          {
            userName = Ganymede.internalSession.getDBSession().getObjectLabel(tmpInvid);
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
    try
      {
        if (field.getID() != volumeSchema.ENTRIES)
          {
            return null;                // by default, we just ok whatever
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

            Vector<Invid> entries = (Vector<Invid>) getFieldValuesLocal(volumeSchema.ENTRIES);

            if (entries == null)
              {
                return Ganymede.createErrorDialog("Logic error in server",
                                                  "volumeCustom.wizardHook(): can't delete element out of empty field");
              }

            Invid invid = null; // the invid for the entry

            try
              {
                invid = entries.get(index);
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

            DBObject vObj = getDBSession().viewDBObject(invid); // should be a mapEntry object

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
                                                      getDBSession().getObjectLabel(user) +
                                                      ", permissions denied to edit the user.");
                  }

                invf = (InvidDBField) eObj.getField(userSchema.VOLUMES);

                if (invf == null)
                  {
                    return Ganymede.createErrorDialog("Couldn't remove map entry",
                                                      "Couldn't remove map entry for " +
                                                      getDBSession().getObjectLabel(user) +
                                                      ", couldn't access the volumes field in the user record.");
                  }

                // by doing the deleteElement on the field containing
                // the embedded volume object, we will let the user
                // object take care of deleting the embedded volume
                // object.  The invid linking system will then take care
                // of removing it from the map object's MAPENTRIES field.

                ReturnVal retVal = null;

                try
                  {
                    retVal = invf.deleteElement(invid);
                  }
                catch (GanyPermissionsException ex)
                  {
                    retVal  = Ganymede.createErrorDialog("volumeCustom: Error",
                                                         "Permissions error unlinking user " + getDBSession().getObjectLabel(user) +
                                                         " from volume.");
                  }

                return retVal;
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
