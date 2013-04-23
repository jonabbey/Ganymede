/*

   dhcpGroupCustom.java

   This file is a management class for NFS volume objects in Ganymede.

   Created: 10 October 2007

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

import arlut.csd.JDialog.JDialogBuff;
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
                                                                 dhcpGroupCustom

------------------------------------------------------------------------------*/

public class dhcpGroupCustom extends DBEditObject implements SchemaConstants, dhcpGroupSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public dhcpGroupCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public dhcpGroupCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public dhcpGroupCustom(DBObject original, DBEditSet editset)
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
      case dhcpGroupSchema.NAME:
      case dhcpGroupSchema.OPTIONS:
        return true;
      }

    return false;
  }

  /**
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} have
   * been instantiated without defined values.  If this DBEditObject
   * is an embedded type, it will have been linked into its parent
   * object before this method is called.</p>
   *
   * <p>This method is responsible for filling in any default values
   * that can be calculated from the {@link
   * arlut.csd.ganymede.server.DBSession DBSession} associated with
   * the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.server.DBSession#createDBObject(short,
   * arlut.csd.ganymede.common.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure
   * code, the calling method will rollback the transaction.  This
   * method has no responsibility for undoing partial initialization,
   * the checkpoint/rollback logic will take care of that.</p>
   *
   * <p>If enableOversight is false, DBSession.createDBObject() will
   * not checkpoint the transaction status prior to calling
   * initializeNewObject(), so it is the responsibility of this method
   * to handle any checkpointing needed.</p>
   *
   * <p>This method should be overridden in subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal initializeNewObject()
  {
    try
      {
        ReturnVal retVal = null;
        InvidDBField invf = (InvidDBField) getField(dhcpGroupSchema.OPTIONS);

        try
          {
            retVal = invf.createNewEmbedded(true);
          }
        catch (GanyPermissionsException ex)
          {
            return Ganymede.createErrorDialog("permissions", "permissions error creating embedded object" + ex);
          }

        return retVal;
      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }
  }

  /**
   * <p>Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean canCloneField(DBSession session, DBObject object, DBField field)
  {
    if (field.getID() == dhcpGroupSchema.MEMBERS)
      {
        return false;           // let's not mess with the members field
      }

    return super.canCloneField(session, object, field);
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the
   * work.</p>
   *
   * <p>This method is called on a newly created object, in order to
   * clone the state of origObj into it.  This method does not
   * actually create a new object.. that is handled by {@link
   * arlut.csd.ganymede.server.GanymedeSession#clone_db_object(arlut.csd.ganymede.common.Invid)
   * clone_db_object()} before this method is called on the newly
   * created object.</p>
   *
   * <p>The default (DBEditObject) implementation of this method will
   * only clone fields for which {@link
   * arlut.csd.ganymede.server.DBEditObject#canCloneField(arlut.csd.ganymede.server.DBSession,
   * arlut.csd.ganymede.server.DBObject,
   * arlut.csd.ganymede.server.DBField) canCloneField()} returns true,
   * and which are not connected to a namespace (and thus could not
   * possibly be cloned, because the values are constrained to be
   * unique and non-duplicated).</p>
   *
   * <p>If one or more fields in the original object are unreadable by
   * the cloning session, we will provide a list of fields that could
   * not be cloned due to a lack of read permissions in a dialog in
   * the ReturnVal.  Such a problem will not result in a failure code
   * being returned, however.. the clone will succeed, but an
   * informative dialog will be provided to the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses, but
   * this method's default logic will probably do what you need it to
   * do.  If you need to make changes, try to chain your subclassed
   * method to this one via super.cloneFromObject().</p>
   *
   * @param session The DBSession that the new object is to be created in
   * @param origObj The object we are cloning
   * @param local If true, fields that have choice lists will not be checked against
   * those choice lists and read permissions for each field will not be consulted.
   * The canCloneField() method will still be consulted, however.
   *
   * @return A standard ReturnVal status object.  May be null on success, or
   * else may carry a dialog with information on problems and a success flag.
   */

  @Override public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    try
      {
        boolean problem = false;
        ReturnVal tmpVal;
        StringBuilder resultBuf = new StringBuilder();
        ReturnVal retVal = super.cloneFromObject(session, origObj, local);

        if (retVal != null)
          {
            if (!retVal.didSucceed())
              {
                return retVal;
              }

            if (retVal.getDialog() != null)
              {
                resultBuf.append("\n\n");
                resultBuf.append(retVal.getDialog().getText());

                problem = true;
              }
          }

        // and clone the embedded objects.
        //
        // Remember, dhcpGroupCustom.initializeNewObject() will create
        // a single embedded option object as part of the normal dhcp
        // group creation process.  We'll put this (single)
        // automatically created embedded object into the newOnes
        // vector, then create any new embedded options necessary when
        // cloning a multiple option dhcp group.

        InvidDBField newOptions = (InvidDBField) getField(dhcpGroupSchema.OPTIONS);
        InvidDBField oldOptions = (InvidDBField) origObj.getField(dhcpGroupSchema.OPTIONS);

        Vector<Invid> newOnes = (Vector<Invid>) newOptions.getValuesLocal().clone();
        Vector<Invid> oldOnes = (Vector<Invid>) oldOptions.getValuesLocal().clone();

        DBObject origOption;
        DBEditObject workingOption;
        int i;

        for (i = 0; i < newOnes.size(); i++)
          {
            workingOption = (DBEditObject) session.editDBObject(newOnes.get(i));
            origOption = session.viewDBObject(oldOnes.get(i));
            tmpVal = workingOption.cloneFromObject(session, origOption, local);

            if (tmpVal != null && tmpVal.getDialog() != null)
              {
                resultBuf.append("\n\n");
                resultBuf.append(tmpVal.getDialog().getText());

                problem = true;
              }
          }

        Invid newInvid;

        if (i < oldOnes.size())
          {
            for (; i < oldOnes.size(); i++)
              {
                try
                  {
                    tmpVal = newOptions.createNewEmbedded(local);
                  }
                catch (GanyPermissionsException ex)
                  {
                    tmpVal = Ganymede.createErrorDialog("permissions",
                                                        "permissions failure creating embedded option " + ex);
                  }

                if (!tmpVal.didSucceed())
                  {
                    if (tmpVal != null && tmpVal.getDialog() != null)
                      {
                        resultBuf.append("\n\n");
                        resultBuf.append(tmpVal.getDialog().getText());

                        problem = true;
                      }
                    continue;
                  }

                newInvid = tmpVal.getInvid();

                workingOption = (DBEditObject) session.editDBObject(newInvid);
                origOption = session.viewDBObject(oldOnes.get(i));
                tmpVal = workingOption.cloneFromObject(session, origOption, local);

                if (tmpVal != null && tmpVal.getDialog() != null)
                  {
                    resultBuf.append("\n\n");
                    resultBuf.append(tmpVal.getDialog().getText());

                    problem = true;
                  }
              }
          }

        retVal = new ReturnVal(true, !problem);

        if (problem)
          {
            retVal.setDialog(new JDialogBuff("Possible Clone Problems", resultBuf.toString(),
                                             "Ok", null, "ok.gif"));
          }

        return retVal;
      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }
  }
}
