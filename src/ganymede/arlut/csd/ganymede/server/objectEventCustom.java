/*

   objectEventCustom.java

   This file is a management class for object event-class records in Ganymede.

   Created: 9 July 1998

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

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                               objectEventCustom

------------------------------------------------------------------------------*/

public class objectEventCustom extends DBEditObject implements SchemaConstants {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.objectEventCustom");

  /**
   * We're going to present the user with a list of recommended event
   * names to choose from.
   */

  static QueryResult eventNames = null;

  static
  {
    eventNames = new QueryResult(true);

    eventNames.addRow(null, "objectcreated", false);
    eventNames.addRow(null, "objectchanged", false);
    eventNames.addRow(null, "inactivateobject", false);
    eventNames.addRow(null, "deleteobject", false);
    eventNames.addRow(null, "reactivateobject", false);
    eventNames.addRow(null, "expirationwarn", false);
    eventNames.addRow(null, "expirenotify", false);
    eventNames.addRow(null, "removalwarn", false);
    eventNames.addRow(null, "removenotify", false);
  }

  // ---

  /**
   * Since object types can only be changed by the schema editor, we'll
   * cache the object type list for the duration of this object's being
   * edited.  We could even make this a static, but then we'd have to
   * have the DBSchemaEdit code know to clear it when the schema was
   * edited, which would be a hassle.
   */

  QueryResult objectTypeList = null;

  /* -- */

  /**
   * Customization Constructor
   */

  public objectEventCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   * Create new object constructor
   */

  public objectEventCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   */

  public objectEventCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   * We'll use DBObject's identity-based equals
   */

  @Override public boolean equals(Object param)
  {
    return super.equals(param);
  }

  /**
   * <p>Customization method to control whether a specified field is
   * required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   */

  @Override public boolean fieldRequired(DBObject object, short fieldid)
  {
    // both fields defined in event are required

    switch (fieldid)
      {
      case SchemaConstants.ObjectEventToken:
      case SchemaConstants.ObjectEventName:

        return true;
      }

    return false;
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   */

  @Override public boolean mustChoose(DBField field)
  {
    // by default, we assume that InvidDBField's are always
    // must choose.

    if (field instanceof InvidDBField)
      {
        return true;
      }

    if (field.getID() == SchemaConstants.ObjectEventObjectName)
      {
        return true;
      }

    return false;
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields.</p>
   */

  @Override public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() == SchemaConstants.ObjectEventObjectName)
      {
        if (objectTypeList == null)
          {
            Vector list = Ganymede.db.getBaseNameList();

            objectTypeList = new QueryResult(true);

            for (int i = 0; i < list.size(); i++)
              {
                objectTypeList.addRow(null, (String) list.elementAt(i), false);
              }
          }

        return objectTypeList;
      }

    if (field.getID() == SchemaConstants.ObjectEventToken)
      {
        return eventNames;
      }

    return super.obtainChoiceList(field);
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions in
   * reaction to the set.  When a scalar field has its value set, it
   * will call its owners finalizeSetValue() method, passing itself as
   * the &lt;field&gt; parameter, and passing the new value to be
   * approved as the &lt;value&gt; parameter.  A Ganymede customizer
   * who creates custom subclasses of the DBEditObject class can
   * override the finalizeSetValue() method and write his own logic
   * to examine any change and either approve or reject the change.</p>
   *
   * <p>A custom finalizeSetValue() method will typically need to
   * examine the field parameter to see which field is being changed,
   * and then do the appropriate checking based on the value
   * parameter.  The finalizeSetValue() method can call the normal
   * this.getFieldValueLocal() type calls to examine the current state
   * of the object, if such information is necessary to make
   * appropriate decisions.</p>
   *
   * <p>If finalizeSetValue() returns null or a ReturnVal object with
   * a positive success value, the DBField that called us is
   * guaranteed to proceed to make the change to its value.  If this
   * method returns a non-success code in its ReturnVal, as with the
   * result of a call to Ganymede.createErrorDialog(), the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.  Any error dialog returned from finalizeSetValue()
   * will be passed to the user.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including a call to our own
   * verifyNewValue() method before calling this method.  Under normal
   * circumstances, we won't need to do anything here.
   * finalizeSetValue() is useful when you need to do unusually
   * involved checks, and for when you want a chance to trigger other
   * changes in response to a particular field's value being
   * changed.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    if (field.getID() == SchemaConstants.ObjectEventObjectName)
      {
        // let the field be cleared if this object is being deleted.

        if (value == null)
          {
            return null;
          }

        DBObjectBase base = Ganymede.db.getObjectBase((String) value);

        if (base == null)
          {
            // "Error, no object type matching "{0}" could be found.  This is probably an error in the Ganymede code."
            return Ganymede.createErrorDialog(ts.l("finalizeSetValue.bad_base", (String) value));
          }

        ReturnVal retVal = null;

        retVal = setFieldValueLocal(SchemaConstants.ObjectEventObjectType, Integer.valueOf(base.getTypeID()));

        if (!ReturnVal.didSucceed(retVal))
          {
            return retVal;
          }

        // the change was accepted and made sense so far.. update our hidden label field

        return ReturnVal.merge(retVal,
                               updateLabel((String) value,
                                           (String) field.getOwner().getFieldValueLocal(SchemaConstants.ObjectEventToken)));
      }

    if (field.getID() == SchemaConstants.ObjectEventToken)
      {
        return updateLabel((String) field.getOwner().getFieldValueLocal(SchemaConstants.ObjectEventObjectName),
                           (String) value);
      }

    return null;
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
    if (this.getStatus() == ObjectStatus.DELETING ||
        this.getStatus() == ObjectStatus.DROPPING)
      {
        return null;
      }

    return updateLabel((String) getFieldValueLocal(SchemaConstants.ObjectEventObjectName),
                       (String) getFieldValueLocal(SchemaConstants.ObjectEventToken));
  }

  /**
   * This local method updates the hidden, composite, label field for
   * us.
   */

  ReturnVal updateLabel(String typeName, String token)
  {
    // we only set the label if we have both a typename and a
    // token.. otherwise, the setFieldValueLocal() call we do here
    // will allocate a partial label in the namespace, and once a
    // namespace value is associated with a transaction, it sticks
    // until the transaction is committed or cancelled.
    //
    // by only setting null or a complete label, we avoid having
    // partial names block other transactions who might be assembling
    // an object event themselves.

    if (typeName == null || token == null)
      {
        return setFieldValueLocal(SchemaConstants.ObjectEventLabel, null);
      }

    StringBuilder result = new StringBuilder();

    result.append(typeName);
    result.append(":");
    result.append(token);

    return setFieldValueLocal(SchemaConstants.ObjectEventLabel, result.toString());
  }

  /**
   * <p>Customization method to verify whether the user should be able
   * to see a specific field in a given object.  Instances of DBField
   * will wind up calling up to here to let us override the normal
   * visibility process.</p>
   *
   * <p>Note that it is permissible for session to be null, in which
   * case this method will always return the default visiblity for the
   * field in question.</p>
   *
   * <p>If field is not from an object of the same base as this
   * DBEditObject, an exception will be thrown.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   */

  @Override public boolean canSeeField(DBSession session, DBField field)
  {
    // by default, return the field definition's visibility

    if (field.getObjTypeID() != getTypeID())
      {
        throw new IllegalArgumentException("field/object mismatch");
      }

    // We don't want the user to see the ObjectEventObjectType
    // field, since we use it as a scratch pad internally for
    // keeping the name correct.

    if (field.getID() == SchemaConstants.ObjectEventObjectType)
      {
        return false;
      }

    return field.getFieldDef().isVisible();
  }
}
