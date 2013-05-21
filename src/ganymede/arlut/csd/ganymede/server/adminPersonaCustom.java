/*

   adminPersonaCustom.java

   This file is a management class for admin personae objects in Ganymede.

   Created: 8 October 1997

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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                              adminPersonaCustom

------------------------------------------------------------------------------*/

/**
 * This file is a management class for Admin Persona objects in
 * Ganymede.
 */

public class adminPersonaCustom extends DBEditObject implements SchemaConstants {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts =
    TranslationService.getTranslationService("arlut.csd.ganymede.server.adminPersonaCustom");

  /**
   * <p>This method takes an Invid pointing to an Admin Persona or
   * User record, and returns a string that can be used to send email
   * to that person.  This method will return null if no address could
   * be determined for this administrator / user.</p>
   */

  static public String convertAdminInvidToString(Invid adminInvid, DBSession session)
  {
    DBObject admin;

    /* -- */

    if (adminInvid.getType() != SchemaConstants.PersonaBase &&
        adminInvid.getType() != SchemaConstants.UserBase)
      {
        throw new RuntimeException("not an administrator or user invid");
      }

    if (session == null)
      {
        session = Ganymede.internalSession.getDBSession();
      }

    admin = session.viewDBObject(adminInvid);

    List<String> addresses = admin.getEmailTargets();

    if (addresses == null || addresses.size() == 0)
      {
        return null;
      }

    return addresses.get(0);
  }

  /**
   *
   * Customization Constructor
   *
   */

  public adminPersonaCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public adminPersonaCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public adminPersonaCustom(DBObject original, DBEditSet editset) throws RemoteException
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

  @Override public boolean hasEmailTarget(DBObject object)
  {
    return true;
  }

  /**
   * <p>This method provides a hook to allow custom DBEditObject subclasses to
   * return a Vector of Strings comprising a list of addresses to be
   * notified above and beyond the normal owner group notification when
   * the given object is changed in a transaction.  Used for letting end-users
   * be notified of changes to their account, etc.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public List<String> getEmailTargets(DBObject object)
  {
    List<String> x = new ArrayList<String>();

    String address = (String) object.getFieldValueLocal(SchemaConstants.PersonaMailAddr);

    if (address == null || address.trim().equals(""))
      {
        // okay, we got no address pre-registered for this
        // admin.. we need now to try to guess at one, by looking
        // to see this admin's name is of the form user:role, in
        // which case we can just try to send to 'user', which will
        // work as long as Ganymede's users cohere with the user names
        // at Ganymede.mailHostProperty.

        String adminName = object.getLabel();

        int colondex = adminName.indexOf(':');

        if (colondex == -1)
          {
            // supergash?

            return null;
          }

        address = adminName.substring(0, colondex);
      }

    if (x != null)
      {
        x.add(address);
      }

    return x;
  }

  /**
   * <p>We want any change to the 'name' or associated user field to
   * update our hidden label field, which both provides our composite
   * label and does our namespace checks for us.  We do this in
   * finalizeSetValue() so that this operation is always done, even
   * if our GanymedeSession's enableOversight is set to false.</p>
   *
   * <p>----</p>
   *
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
    if (field.getID() == SchemaConstants.PersonaNameField)
      {
        if (debug)
          {
            System.err.println("adminPersonaCustom.finalizeSetValue(): setting persona name, refreshing label");
          }

        return refreshLabelField((String) value, null, null);
      }

    if (field.getID() == SchemaConstants.PersonaAssocUser)
      {
        if (debug)
          {
            System.err.println("adminPersonaCustom.finalizeSetValue(): setting persona user, refreshing label");
          }

        // Hide the associated user field if we are looking at the
        // supergash or monitor persona objects.

        if ((field.getID() == SchemaConstants.PersonaAssocUser) &&
            (getID() <= 2))
          {
            // "It is not permitted (or necessary) to set an associated user on the supergash or monitor persona objects."
            return Ganymede.createErrorDialog(ts.l("finalizeSetValue.restricted_persona"));
          }

        return refreshLabelField(null, (Invid) value, null);
      }

    return null;
  }

  /**
   * This private method is used to keep the hidden label field up-to-date.
   */

  public ReturnVal refreshLabelField(String descrip, Invid userInvid, String newName)
  {
    if (descrip == null)
      {
        StringDBField nameField = (StringDBField) getField(SchemaConstants.PersonaNameField);

        if (nameField != null)
          {
            descrip = (String) nameField.getValueLocal();
          }
      }

    if ((userInvid == null) && (newName == null))
      {
        InvidDBField assocUserField = (InvidDBField) getField(SchemaConstants.PersonaAssocUser);

        if (assocUserField != null)
          {
            userInvid = (Invid) assocUserField.getValueLocal();
          }
      }

    // if we are messing with the supergash or monitor persona
    // objects, don't try to mess around with the associated user.

    if (getID() <= 2)
      {
        return setFieldValueLocal(SchemaConstants.PersonaLabelField, descrip);
      }

    if ((newName == null && userInvid == null) || descrip == null)
      {
        return setFieldValueLocal(SchemaConstants.PersonaLabelField, null);
      }

    if (newName == null)
      {
        newName = this.getGSession().getDBSession().getObjectLabel(userInvid);
      }

    if (debug)
      {
        System.err.println("Trying to set label to " + newName + ":" + descrip);
      }

    return setFieldValueLocal(SchemaConstants.PersonaLabelField, newName + ":" + descrip);
  }

  /**
   * <p>This method returns a key that can be used by the client to
   * cache the value returned by choices().  If the client already has
   * the key cached on the client side, it can provide the choice list
   * from its cache rather than calling choices() on this object
   * again.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  @Override public Object obtainChoicesKey(DBField field)
  {
    // by default, we return a Short containing the base
    // id for the field's target

    if (field.getID() != SchemaConstants.PersonaPrivs)
      {
        return super.obtainChoicesKey(field);
      }

    return null;                // not going to cache PersonaPrivs field
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide such.
   * String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields.</p>
   */

  @Override public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (debug)
      {
        System.err.println("Entering adminPersona obtainChoiceList for field " +
                           field.getName());
      }

    if (field.getID() != SchemaConstants.PersonaPrivs)
      {
        return super.obtainChoiceList(field);
      }

    if (debug)
      {
        System.err.println("Returning adminPersona restricted list");
      }

    if (field.isEditable() && (field instanceof InvidDBField) &&
        !field.isEditInPlace())
      {
        DBObjectBaseField fieldDef;
        short baseId;

        /* -- */

        fieldDef = field.getFieldDef();

        baseId = fieldDef.getTargetBase();

        if (baseId < 0)
          {
            return null;
          }

        if (Ganymede.internalSession == null)
          {
            return null;
          }

        // We don't want the Default Role to be shown as a valid
        // choice for this.. everyone has Default implicitly, no point
        // in showing it.

        QueryNode root = new QueryNotNode(new QueryDataNode(QueryDataNode.INVIDVAL,
                                                            QueryDataNode.EQUALS,
                                                            Invid.createInvid(SchemaConstants.RoleBase,
                                                                              SchemaConstants.RoleDefaultObj)));

        // note that the query we are submitting here *will* be filtered by the
        // current visibilityFilterInvid field in GanymedeSession.

        return editset.getDBSession().getGSession().query(new Query(baseId, root, true));
      }

    return null;
  }

  /**
   * <p>Customization method to verify overall consistency of
   * a DBObject.  This method is intended to be overridden
   * in DBEditObject subclasses, and will be called by
   * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase1() commitPhase1()}
   * to verify the readiness of this object for commit.  The
   * DBObject passed to this method will be a DBEditObject,
   * complete with that object's GanymedeSession reference
   * if this method is called during transaction commit, and
   * that session reference may be used by the verifying code if
   * the code needs to access the database.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal consistencyCheck(DBObject object)
  {
    // we want to return a failure if there is no role set and if the
    // persona is not a member of the supergash owner set, which would
    // make roles superfluous

    Vector<Invid> roles = (Vector<Invid>) object.getFieldValuesLocal(SchemaConstants.PersonaPrivs);
    Vector<Invid> ownerSets = (Vector<Invid>) object.getFieldValuesLocal(SchemaConstants.PersonaGroupsField);

    Invid supergashPersona = Invid.createInvid(SchemaConstants.PersonaBase, SchemaConstants.PersonaSupergashObj);
    Invid monitor = Invid.createInvid(SchemaConstants.PersonaBase, SchemaConstants.PersonaMonitorObj);

    if (supergashPersona.equals(object.getInvid()) || monitor.equals(object.getInvid()))
      {
        return null;
      }

    Invid supergashOwner = Invid.createInvid(SchemaConstants.OwnerBase, SchemaConstants.OwnerSupergash);

    if (roles.size() == 0 && !ownerSets.contains(supergashOwner))
      {
        // "Persona object "{0}" is incomplete. Personas must either have
        // a role defined or be a member of the supergash owner set."
        return Ganymede.createErrorDialog(ts.l("consistencyCheck.role_needed", this.getLabel()));
      }

    return null;
  }

  /**
   * <p>Customization method to control whether a specified field is
   * required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case SchemaConstants.PersonaAssocUser:

        // supergash and monitor don't have to have associated users
        // defined.

        if (object.getID() <= 2)
          {
            return false;
          }

      case SchemaConstants.PersonaNameField:
      case SchemaConstants.PersonaPasswordField:
      case SchemaConstants.PersonaLabelField:
        return true;
      }

    return false;
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
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean canSeeField(DBSession session, DBField field)
  {
    // hide the label field.. this cannot be changed by the client,
    // and should be treated as a 'behind-the-scenes' field used to
    // tie things together in the background.

    if (field.getID() == SchemaConstants.PersonaLabelField)
      {
        return false;
      }

    // Hide the associated user field if we are looking at the
    // supergash or monitor persona objects.

    if (field.getID() == SchemaConstants.PersonaAssocUser)
      {
        DBObject object = field.getOwner();

        if (object.getID() <= 2)
          {
            return false;
          }
      }

    return super.canSeeField(session, field);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable
   * to make a link to the given field in this DBObject type when the
   * user only has editing access for the source InvidDBField and not
   * the target.</p>
   *
   * @param object The object that the link is to be created in
   * @param fieldID The field that the link is to be created in
   */

  @Override public boolean anonymousLinkOK(DBObject object, short fieldID)
  {
    // if they have permission to edit an owner group, who are
    // we to say no?

    if (fieldID == SchemaConstants.PersonaGroupsField)
      {
        return true;
      }

    return false;               // by default, permission is denied
  }
}
