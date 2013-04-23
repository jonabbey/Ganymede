/*

   dhcpEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.

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

import arlut.csd.Util.VectorUtils;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectStatus;
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
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 dhcpEntryCustom

------------------------------------------------------------------------------*/

/**
 * This class represents the option value objects that are embedded in
 * the options field in the DHCP Entry object.
 */

public class dhcpEntryCustom extends DBEditObject implements SchemaConstants, dhcpEntrySchema {

  private final static boolean debug = false;

  /**
   *
   * Customization Constructor
   *
   */

  public dhcpEntryCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public dhcpEntryCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public dhcpEntryCustom(DBObject original, DBEditSet editset)
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
      case dhcpEntrySchema.LABEL:
      case dhcpEntrySchema.TYPE:
      case dhcpEntrySchema.VALUE:
        return true;
      }

    return false;
  }

  /**
   * <p>This method returns a key that can be used by the client to
   * cache the value returned by choices().  If the client already has
   * the key cached on the client side, it can provide the choice list
   * from its cache rather than calling choices() on this object
   * again.</p>
   *
   * <p>The default logic in this method is designed to cause the
   * client to cache choice lists for invid fields in the 'all objects
   * of invid target type' cache bucket.  If your InvidDBField needs
   * to provide a restricted subset of objects of the targeted type as
   * the choice list, you'll need to override this method to either
   * return null (to turn off choice list caching), or generate some
   * kind of unique key that won't collide with the Short objects used
   * to represent the default object list caches.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)}
   * hook, which controls whether or not the default logic will
   * encourage the client to cache a given InvidDBField's choice
   * list.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  @Override public Object obtainChoicesKey(DBField field)
  {
    // We want to have the client always query for values in the type
    // field, since we are going to be dynamically filtering values
    // out in order to prevent multiple entries with identical type
    // selections.

    if (field.getID() == dhcpEntrySchema.TYPE)
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
    if (field.getID() != dhcpEntrySchema.TYPE)
      {
        return super.obtainChoiceList(field);
      }

    // Dynamically construct our custom filtered list of available
    // types for this entry

    InvidDBField invf = (InvidDBField) getField(dhcpEntrySchema.TYPE);

    // ok, we are returning the list of choices for what type this
    // entry should belong to.  We don't want it to include any types
    // that we already have an entry for.

    Vector<Invid> typesToSkip = new Vector();

    Vector<Invid> siblings = getSiblingInvids();
    Invid typeInvid;

    for (int i = 0; i < siblings.size(); i++)
      {
        DBObject entry = lookupInvid(siblings.get(i), false);
        typeInvid = (Invid) entry.getFieldValueLocal(dhcpEntrySchema.TYPE);
        typesToSkip.add(typeInvid);
      }

    // ok, typesToSkip has a list of invid's to skip in our choice
    // list.

    Vector<Invid> suggestedTypes = super.obtainChoiceList(field).getInvids();
    Vector<Invid> acceptableTypes = VectorUtils.difference(suggestedTypes, typesToSkip);

    QueryResult result = new QueryResult();

    for (Invid type: acceptableTypes)
      {
        result.addRow(type, lookupInvidLabel(type), false);
      }

    return result;
  }

  private boolean ownedBySystem()
  {
    return this.ownedBySystem(this);
  }

  private boolean ownedBySystem(DBObject object)
  {
    return object.getParentInvid().getType() == (short) 263;
  }

  private boolean ownedByDHCPGroup()
  {
    return this.ownedByDHCPGroup(this);
  }

  private boolean ownedByDHCPGroup(DBObject object)
  {
    return object.getParentInvid().getType() == (short) 262;
  }

  private boolean ownedByDHCPNetwork()
  {
    return this.ownedByDHCPNetwork(this);
  }

  private boolean ownedByDHCPNetwork(DBObject object)
  {
    return object.getParentInvid().getType() == (short) 268;
  }

  private Vector<Invid> getSiblingInvids()
  {
    Vector<Invid> result = null;

    if (ownedByDHCPGroup())
      {
        result = (Vector<Invid>) getParentObj().getFieldValuesLocal(dhcpGroupSchema.OPTIONS).clone();
      }
    else if (ownedBySystem())
      {
        result = (Vector<Invid>) getParentObj().getFieldValuesLocal(systemSchema.DHCPOPTIONS).clone();
      }
    else if (ownedByDHCPNetwork())
      {
        Vector<Invid> optionsVect = (Vector<Invid>) getParentObj().getFieldValuesLocal(dhcpNetworkSchema.OPTIONS).clone();

        if (optionsVect.contains(getInvid()))
          {
            result = optionsVect;
          }
        else if (getParentObj().isDefined(dhcpNetworkSchema.GUEST_OPTIONS))
          {
            Vector<Invid> guestOptionsVect = (Vector<Invid>) getParentObj().getFieldValuesLocal(dhcpNetworkSchema.GUEST_OPTIONS).clone();

            if (guestOptionsVect.contains(getInvid()))
              {
                result = guestOptionsVect;
              }
          }

        if (result == null)
          {
            throw new RuntimeException("couldn't find our own invid in parent dhcp network fields.");
          }
      }

    // we are not our own sibling.

    result.remove(getInvid());

    return result;
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

    String parentName = lookupInvidLabel((Invid) getFieldValueLocal(SchemaConstants.ContainerField));

    return setHiddenLabel(parentName);
  }

  /**
   * This method is used to update the hidden label field (which we
   * have to have for xml address-ability reasons) to match the name
   * of the parent object and the option type that we're pointing to.
   */

  public ReturnVal setHiddenLabel(String parentName)
  {
    Invid typeInvid = (Invid) getFieldValueLocal(dhcpEntrySchema.TYPE);

    return setFieldValueLocal(dhcpEntrySchema.LABEL, parentName + ":" + String.valueOf(lookupInvidLabel(typeInvid)));
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
    InvidDBField typeField;
    StringDBField valueField;
    StringBuilder buff = new StringBuilder();

    /* -- */

    if ((object == null) || (object.getTypeID() != getTypeID()))
      {
        return null;
      }

    typeField = (InvidDBField) object.getField(TYPE);
    valueField = (StringDBField) object.getField(VALUE);

    if (typeField != null)
      {
        if (typeField.getValueLocal() != null)
          {
            buff.append(typeField.getValueString() + " : ");
          }
      }

    if (valueField != null)
      {
        if (valueField.getValueLocal() != null)
          {
            buff.append(valueField.getValueString());
          }
      }

    return buff.toString();
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

  @Override public synchronized ReturnVal finalizeSetValue(DBField field, Object value)
  {
    ReturnVal result = null;
    Invid parentInvid = getParentInvid();

    if (isDeleting())
      {
        return null;        // we're being deleted
      }

    if (field.getID() == TYPE && value != null)
      {
        Invid oldInvid = (Invid) field.getValueLocal();

        if (oldInvid == null)
          {
            // we set the type after the value.  make sure the value
            // is compatible with the type.

            StringDBField valueField = (StringDBField) this.getField(dhcpEntrySchema.VALUE);
            String valueString = (String) valueField.getValueLocal();

            if (valueString != null)
              {
                DBObject verifyObject = lookupInvid((Invid) value);

                result = ReturnVal.merge(result, dhcpOptionCustom.verifyAcceptableValue(verifyObject, valueString));
              }
          }
        else
          {
            DBObject oldOptionObject = lookupInvid(oldInvid);
            String oldOptionType = (String) oldOptionObject.getFieldValueLocal(dhcpOptionSchema.OPTIONTYPE);

            DBObject newOptionObject = lookupInvid((Invid) value);
            String newOptionType = (String) newOptionObject.getFieldValueLocal(dhcpOptionSchema.OPTIONTYPE);

            if (!newOptionType.equals(oldOptionType))
              {
                // we've changed to an incompatible option, clear the
                // value field.

                StringDBField valueField = (StringDBField) getField(dhcpEntrySchema.VALUE);

                result = valueField.setValueLocal("");

                if (result != null && !result.didSucceed())
                  {
                    return result;
                  }

                if (result == null)
                  {
                    result = new ReturnVal(true, true);
                  }

                result.addRescanField(getInvid(), dhcpEntrySchema.VALUE);
              }
          }
      }

    if (field.getID() == TYPE || field.getID() == VALUE)
      {
        if (result == null)
          {
            result = new ReturnVal(true, true);
          }

        if (ownedByDHCPGroup())
          {
            result.addRescanField(parentInvid, dhcpGroupSchema.OPTIONS);
          }
        else if (ownedBySystem())
          {
            result.addRescanField(parentInvid, systemSchema.DHCPOPTIONS);
          }

        if (field.getID() == TYPE)
          {
            // force the client to requery legal (and non-taken) types

            Vector<Invid> siblings = getSiblingInvids();

            for (Invid sibling: siblings)
              {
                result.addRescanField(sibling, dhcpEntrySchema.TYPE);
              }
          }

        return result;
      }

    return result;              // success by default
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
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal verifyNewValue(DBField field, Object value)
  {
    if (field.getID() == dhcpEntrySchema.VALUE)
      {
        if (debug)
          {
            Ganymede.debug("attempting to verify: " + String.valueOf(value));
          }

        String inString = (String) value;
        String transformedString;

        if ((inString == null) || (inString.equals("")))
          {
            return null; // okay by us!
          }

        Invid dhcpType = (Invid) getFieldValueLocal(dhcpEntrySchema.TYPE);

        if (dhcpType == null)
          {
            // okay, we'll verify it later.
            return null;
          }

        DBObject verifyObject = lookupInvid(dhcpType);

        ReturnVal retVal = dhcpOptionCustom.verifyAcceptableValue(verifyObject, inString);

        if (retVal == null)
          {
            if (debug)
              {
                Ganymede.debug("verifying as is: " + String.valueOf(value));
              }

            return super.verifyNewValue(field, value); // no change, so no problem
          }
        else if (retVal.didSucceed() && retVal.hasTransformedValue())
          {
            retVal.requestRefresh(this.getInvid(), dhcpEntrySchema.VALUE);
          }

        return retVal;
      }

    return super.verifyNewValue(field, value);
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

    if (field.getID() == dhcpEntrySchema.LABEL)
      {
        return false;
      }

    return super.canSeeField(session, field);
  }
}
