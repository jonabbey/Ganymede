/*

   IRISListCustom.java

   Custom plug-in for managing fields in the IRIS email list object type.

   Created: 2 February 2011

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import arlut.csd.Util.VectorUtils;

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
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 IRISListCustom

------------------------------------------------------------------------------*/

/**
 * <p>Custom plug-in for managing fields in the IRIS email list object
 * type.</p>
 */

public class IRISListCustom extends DBEditObject implements SchemaConstants, IRISListSchema {

  /**
   * Customization Constructor
   */

  public IRISListCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   * Create new object constructor
   */

  public IRISListCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   */

  public IRISListCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
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
    if (field.getID() == IRISListSchema.MEMBERS)
      {
        return new QueryResult(true); // empty list
      }

    return super.obtainChoiceList(field);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses,
   * particularly if you have a StringDBField that you want to force
   * to pick from the list of choices provided by your DBEditObject
   * subclass' obtainChoiceList() method.</p>
   */

  @Override public boolean mustChoose(DBField field)
  {
    if (field.getID() == IRISListSchema.MEMBERS)
      {
        return false;           // don't force choice
      }

    return super.mustChoose(field);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>This version of anonymousLinkOK takes additional parameters
   * to allow an object type to decide that it does or does not want
   * to allow a link based on what field of what object wants to link
   * to it.</P>
   *
   * <p>By default, the 3 variants of the DBEditObject
   * anonymousLinkOK() method are chained together, so that the
   * customizer can choose which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link
   * arlut.csd.ganymede.server.InvidDBField#bind(arlut.csd.ganymede.common.Invid,arlut.csd.ganymede.common.Invid,boolean)
   * bind()} method calls this version.  This version calls the three
   * parameter version, which calls the two parameter version, which
   * returns false by default.  Customizers can implement any of the
   * three versions, but unless you maintain the version chaining
   * yourself, there's no point to implementing more than one of
   * them.</P>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in
   * @param targetFieldID The field that the link is to be created in
   * @param sourceObject The object on the other side of the proposed link
   * @param sourceFieldID  The field on the other side of the proposed link
   * @param gsession Who is trying to do this linking?
   */

  @Override public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
                                           DBObject sourceObject, short sourceFieldID,
                                           GanymedeSession gsession)
  {
    // if someone tries to put this list in another email list, let
    // them.

    if ((targetFieldID == SchemaConstants.BackLinksField) &&
        (sourceObject.getTypeID() == 274) && // email list
        (sourceFieldID == 257)) // email list members
      {
        return true;
      }

    // the default anonymousLinkOK() method returns false

    return super.anonymousLinkOK(targetObject, targetFieldID,
                                 sourceObject, sourceFieldID, gsession);
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean fieldRequired(DBObject object, short fieldid)
  {
    // the email list name is required

    if (fieldid == LISTNAME)
      {
        return true;
      }

    return super.fieldRequired(object, fieldid);
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
    if (field.getID() == QUERY)
      {
        String queryString = (String) value;

        if (queryString != null && !queryString.trim().equals(""))
          {
            if (queryString.toLowerCase().contains("update"))
              {
                return Ganymede.createErrorDialog(this.getGSession(),
                                                  null,
                                                  "Update statements not allowed in Query field.");
              }
          }
      }

    return super.verifyNewValue(field, value);
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
    try
      {
        if (field.getID() == QUERY)
          {
            String queryString = (String) value;

            if (queryString == null || queryString.trim().equals(""))
              {
                // set query members to the empty list
                return setQueryMembers(new HashSet<String>());
              }
            else
              {
                try
                  {
                    return setQueryMembers(IRISLink.getUsernames(queryString));
                  }
                catch (java.sql.SQLException ex)
                  {
                    return Ganymede.createErrorDialog(this.getGSession(),
                                                      null,
                                                      ex.getMessage());
                  }
              }
          }
      }
    catch (NotLoggedInException ex)
      {
      }

    return null;
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
    if (fieldid == MEMBERS)
      {
        return PermEntry.getPermEntry(true, false, true, true);
      }

    return null;
  }

  /**
   * <p>This custom server-side method is used to set the members in the
   * members field to the list of users generated by the query string.</p>
   *
   * <p>Returns a non-null ReturnVal indicating success or throws a
   * RuntimeException on failure.</p>
   */

  public ReturnVal setQueryMembers(Set<String> users) throws NotLoggedInException
  {
    DBField memberField = (DBField) this.getField(MEMBERS);

    Date x = new Date();

    try
      {
        editset.checkpoint(x.toString());

        memberField.setUndefined(true);

        for (String username: users)
          {
            Invid userInvid = getGSession().findLabeledObject(username, SchemaConstants.UserBase);

            if (userInvid != null)
              {
                memberField.addElementLocal(userInvid);
              }
          }

        ReturnVal result = ReturnVal.success();
        result.requestRefresh(this.getInvid(), MEMBERS);

        return result;
      }
    catch (Exception ex)
      {
        editset.rollback(x.toString());

        throw new RuntimeException(ex);
      }
    finally
      {
        editset.popCheckpoint(x.toString());
      }
  }

  /**
   * <p>Queries IRIS to update the list of members in the passed
   * listObject.</p>
   *
   * <p>Returns null if no change was made, a successful ReturnVal if
   * a change was made, and a failure ReturnVal if there was some
   * problem.</p>
   */

  static public ReturnVal handleUpdate(DBObject listObject, GanymedeSession gsession) throws NotLoggedInException
  {
    String queryString = (String) listObject.getFieldValueLocal(IRISListSchema.QUERY);

    if (queryString == null || queryString.trim().equals(""))
      {
        return null;
      }

    Set<String> userNames = null;

    try
      {
        userNames = IRISLink.getUsernames(queryString);
      }
    catch (java.sql.SQLException ex)
      {
        return Ganymede.createErrorDialog(listObject.getGSession(),
                                          null,
                                          ex.getMessage());
      }

    List<Invid> userInvids = new ArrayList<Invid>();

    for (String user: userNames)
      {
        Invid userInvid = gsession.findLabeledObject(user, SchemaConstants.UserBase);

        if (userInvid != null)
          {
            userInvids.add(userInvid);
          }
      }

    if (VectorUtils.equalMembers(userInvids, listObject.getFieldValuesLocal(IRISListSchema.MEMBERS)))
      {
        return null;
      }

    // we need to make a change

    ReturnVal retVal = gsession.edit_db_object(listObject.getInvid());

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    IRISListCustom listEditObject = (IRISListCustom) retVal.getObject();

    return listEditObject.setQueryMembers(userNames);
  }
}
