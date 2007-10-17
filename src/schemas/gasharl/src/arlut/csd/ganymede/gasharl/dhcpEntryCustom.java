/*

   dhcpEntryCustom.java

   This file is a management class for Automounter map entry objects in Ganymede.
   
   Created: 10 October 2007
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
 *
 * This class represents the option value objects that are embedded in
 * the options field in the DHCP Entry object.
 *
 */

public class dhcpEntryCustom extends DBEditObject implements SchemaConstants, dhcpEntrySchema {

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
      case dhcpEntrySchema.LABEL:
      case dhcpEntrySchema.TYPE:
      case dhcpEntrySchema.VALUE:
	return true;
      }

    return false;
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

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
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

    Vector typesToSkip = new Vector();

    Vector siblings = getSiblingInvids();
    Invid typeInvid;

    for (int i = 0; i < siblings.size(); i++)
      {
	DBObject entry = lookupInvid((Invid) siblings.elementAt(i), false);
	typeInvid = (Invid) entry.getFieldValueLocal(dhcpEntrySchema.TYPE);
	typesToSkip.addElement(typeInvid);
      }
    
    // ok, typesToSkip has a list of invid's to skip in our choice
    // list.

    Vector suggestedTypes = super.obtainChoiceList(field).getInvids();
    Vector acceptableTypes = VectorUtils.difference(suggestedTypes, typesToSkip);

    QueryResult result = new QueryResult();

    for (int i = 0; i < acceptableTypes.size(); i++)
      {
	typeInvid = (Invid) acceptableTypes.elementAt(i);

        result.addRow(typeInvid, lookupInvidLabel(typeInvid), false);
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

  private Vector getSiblingInvids()
  {
    Vector result = null;

    if (ownedByDHCPGroup())
      {
        result = (Vector) getParentObj().getFieldValuesLocal(dhcpGroupSchema.OPTIONS).clone();
      }
    else if (ownedBySystem())
      {
        result = (Vector) getParentObj().getFieldValuesLocal(systemSchema.DHCPOPTIONS).clone();
      }
    
    // we are not our own sibling.

    result.removeElement(getInvid());

    return result;
  }

  /**
   * This method provides a pre-commit hook that runs after the user
   * has hit commit but before the system has established write locks
   * for the commit.
   *
   * The intended purpose of this hook is to allow objects that
   * dynamically maintain hidden label fields to update those fields
   * from the contents of the object's other fields at commit time.
   *
   * This method runs in a checkpointed context.  If this method fails
   * in any operation, you should return a ReturnVal with a failure
   * dialog encoded, and the transaction's commit will be blocked and
   * a dialog explaining the problem will be presented to the user.
   */

  public ReturnVal preCommitHook()
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
   * If this DBEditObject is managing an embedded object, the
   * getEmbeddedObjectLabel() can be overridden to display a synthetic
   * label in the context of viewing or editing the containing object,
   * and when doing queries on the containing type.
   *
   * The getLabel() method will not consult this hook, however, and
   * embedded objects will be represented with their unique label
   * field when processed in an XML context.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public final String getEmbeddedObjectDisplayLabelHook(DBObject object)
  {
    InvidDBField typeField;
    StringDBField valueField;
    StringBuffer buff = new StringBuffer();

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
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions in
   * reaction to the set.  When a scalar field has its value set, it
   * will call its owners finalizeSetValue() method, passing itself as
   * the &lt;field&gt; parameter, and passing the new value to be
   * approved as the &lt;value&gt; parameter.  A Ganymede customizer
   * who creates custom subclasses of the DBEditObject class can
   * override the finalizeSetValue() method and write his own logic
   * to examine any change and either approve or reject the change.
   *
   * A custom finalizeSetValue() method will typically need to
   * examine the field parameter to see which field is being changed,
   * and then do the appropriate checking based on the value
   * parameter.  The finalizeSetValue() method can call the normal
   * this.getFieldValueLocal() type calls to examine the current state
   * of the object, if such information is necessary to make
   * appropriate decisions.
   *
   * If finalizeSetValue() returns null or a ReturnVal object with
   * a positive success value, the DBField that called us is
   * guaranteed to proceed to make the change to its value.  If this
   * method returns a non-success code in its ReturnVal, as with the
   * result of a call to Ganymede.createErrorDialog(), the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.  Any error dialog returned from finalizeSetValue()
   * will be passed to the user.
   *
   * The DBField that called us will take care of all standard
   * checks on the operation (including a call to our own
   * verifyNewValue() method before calling this method.  Under normal
   * circumstances, we won't need to do anything here.
   * finalizeSetValue() is useful when you need to do unusually
   * involved checks, and for when you want a chance to trigger other
   * changes in response to a particular field's value being
   * changed.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal finalizeSetValue(DBField field, Object value)
  {
    ReturnVal result = null;
    Invid parentInvid = getParentInvid();

    if (parentInvid == null)
      {
        return null;        // we're being deleted
      }

    // when we rename a group, we have lots to do.. a number of other
    // fields in this object and others need to be updated to match.

    if (field.getID() == TYPE && value != null)
      {
        Invid oldInvid = (Invid) field.getValueLocal();

        if (oldInvid != null)
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

            Vector siblings = getSiblingInvids();

            for (int i = 0; i < siblings.size(); i++)
              {
                result.addRescanField((Invid) siblings.elementAt(i), dhcpEntrySchema.TYPE);
              }
          }

        return result;
      }

    return result;		// success by default
  }

  /**
   * This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.
   *
   * Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    if (field.getID() == dhcpEntrySchema.VALUE)
      {
        Ganymede.debug("attempting to verify: " + String.valueOf(value));

	String inString = (String) value;
	String transformedString;

	if ((inString == null) || (inString.equals("")))
	  {
	    return null; // okay by us!
	  }

        Invid dhcpType = (Invid) getFieldValueLocal(dhcpEntrySchema.TYPE);
        DBObject verifyObject = lookupInvid(dhcpType);

        ReturnVal retVal = dhcpOptionCustom.verifyAcceptableValue(verifyObject, inString);

        if (retVal == null)
          {
            Ganymede.debug("verifying as is: " + String.valueOf(value));

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
   *
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.<br><br>
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.<br><br>
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   * 
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    // don't show off our hidden label for direct editing or viewing

    if (field.getID() == dhcpEntrySchema.LABEL)
      {
	return false;
      }

    return super.canSeeField(session, field);
  }
}
