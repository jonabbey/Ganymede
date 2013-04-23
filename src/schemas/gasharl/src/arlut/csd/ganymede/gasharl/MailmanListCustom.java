/*

   MailmanListCustom.java

   Custom plug-in for managing fields in the MailmanList object type.

   Created: 25 June 1999

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
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                               MailmanListCustom

------------------------------------------------------------------------------*/

/**
 *   Custom plug-in for managing fields in the MailmanList object type.
 */

public class MailmanListCustom extends DBEditObject implements SchemaConstants, MailmanListSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public MailmanListCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public MailmanListCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public MailmanListCustom(DBObject original, DBEditSet editset)
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
        case MailmanListSchema.NAME:
        case MailmanListSchema.OWNEREMAIL:
        case MailmanListSchema.PASSWORD:
        case MailmanListSchema.SERVER:
        case MailmanListSchema.ALIASES:

        return true;
      }

    return false;
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
    ReturnVal retVal = null;

    if (field.getID() == NAME)
      {
        if (operation == SETVAL)
          {
            retVal = handleCreateMailmanList((String) param1);
          }
      }

    return retVal;
  }

  /**
   * <p>This method provides a pre-commit hook that runs after the
   * user has hit commit but before the system has established write
   * locks for the commit.</p>
   *
   * <p>The intended purpose of this hook is to allow objects that
   * dynamically maintain hidden label fields to update those fields
   * from the contents of the object's other fields at commit
   * time.</p>
   *
   * <p>This method runs in a checkpointed context.  If this method
   * fails in any operation, you should return a ReturnVal with a
   * failure dialog encoded, and the transaction's commit will be
   * blocked and a dialog explaining the problem will be presented to
   * the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal preCommitHook()
  {
    switch (getStatus())
    {
      case DROPPING:
        break;

      case CREATING:
      case EDITING:

        // handle creating the aliases needed for the mailmanlist
        String name = getLabel();
        return handleCreateMailmanList(name);

      case DELETING:
        break;
    }

    return null;
  }


  /**
   * Handle dealing with Mailman Lists: created all the associated aliases.
   * Mailman needs 10 total email addresses to be registered.
   */

  public ReturnVal handleCreateMailmanList(String name)
  {
    ReturnVal retVal = new ReturnVal(true, true);
    StringDBField stringfield;

    stringfield = (StringDBField) getField(ALIASES);

    boolean succeed = false;

    // set a checkpoint so we can verify all the aliases
    // needed are not currently being used.
    String checkPointKey = "MailmanListAliases";
    DBSession dbSession = getGSession().getDBSession();
    dbSession.checkpoint(checkPointKey);

    try
      {
        while (stringfield.size() > 0)
          {
            retVal = stringfield.deleteElementLocal(0);

            if (retVal != null && !retVal.didSucceed())
              {
                return retVal;
              }
          }

        String[] aliases = {name+"-admin", name+"-bounce", name+"-confirm", name+"-join",
                            name+"-leave", name+"-owner", name+"-request", name+"-subscribe", name+"-unsubscribe"};

        for (int i = 0; i < aliases.length; i++)
          {
            retVal = stringfield.addElementLocal(aliases[i]);

            if (retVal != null && !retVal.didSucceed())
              {
                return retVal;
              }
          }

        succeed = true;
      }
    finally
      {
        if (succeed)
          {
            dbSession.popCheckpoint(checkPointKey);

            // tell client to refresh to see the reactivation results
            retVal = new ReturnVal(true, true);
            retVal.addRescanField(this.getInvid(), MailmanListSchema.ALIASES);

            return retVal;
          }
        else
          {
            if (dbSession.rollback(checkPointKey))
              {
                return retVal;
              }
            else
              {
                return Ganymede.createErrorDialog("MailmanListCustom: Error",
                                                  "Ran into a problem during alias creation, and rollback failed");
              }
          }
      }
  }
}
