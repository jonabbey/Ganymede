/*

   emailListCustom.java

   Custom plug-in for managing fields in the email list object type.

   Created: 16 February 1999

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

import java.util.Vector;

import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryNotNode;
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
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 emailListCustom

------------------------------------------------------------------------------*/

/**
 *   Custom plug-in for managing fields in the email list object type.
 */

public class emailListCustom extends DBEditObject implements SchemaConstants, emailListSchema {

  private QueryResult membersChoice = null;

  /* -- */

  /**
   *
   * Customization Constructor
   *
   */

  public emailListCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public emailListCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public emailListCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
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

  @Override public String lookupLabel(DBObject object)
  {
    if (object.getTypeID() == SchemaConstants.UserBase)
      {
        String fullName = (String) object.getFieldValueLocal(userSchema.FULLNAME);
        String name = (String) object.getFieldValueLocal(userSchema.USERNAME);

        if (fullName != null && name != null)
          {
            return name + " (" + fullName + ")";
          }
      }

    // mark email lists

    if (object.getTypeID() == emailListSchema.BASE)
      {
        return super.lookupLabel(object) + " (email list)";
      }

    // and groups

    if (object.getTypeID() == groupSchema.BASE)
      {
        return super.lookupLabel(object) + " (group)";
      }

    // and user netgroups

    if (object.getTypeID() == userNetgroupSchema.BASE)
      {
        return super.lookupLabel(object) + " (user netgroup)";
      }

    // mark email redirects

    if (object.getTypeID() == emailRedirectSchema.BASE)
      {
        Vector<Invid> addresses = (Vector<Invid>) object.getFieldValuesLocal(emailListSchema.MEMBERS);

        return super.lookupLabel(object) + " (" + VectorUtils.vectorString(addresses) + ")";
      }

    return super.lookupLabel(object);
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
    // we don't want the members field to be cached, since we are
    // amalgamating several kinds of things into one invid field.

    if (field.getID() == MEMBERS)
      {
        return null;
      }

    return super.obtainChoicesKey(field);
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
    if (field.getID() != emailListSchema.MEMBERS)
      {
        return super.obtainChoiceList(field);
      }

    if (membersChoice == null)
      {
        // we want to present a list of all users, mail groups besides
        // this one, and external mail aliases (email addresses that
        // have local aliases in ARL's mail system) as valid choices
        // for the MEMBERS field.

        Query query1 = new Query(SchemaConstants.UserBase, null, false);
        Query query2 = new Query(emailRedirectSchema.BASE, null, false);

        QueryNode root3 = new QueryNotNode(new QueryDataNode((short) -2, QueryDataNode.EQUALS, this.getInvid()));
        Query query3 = new Query(emailListSchema.BASE, root3, false);

        // we also need to union in user netgroups and account groups
        // that have the 'Can Receive Email' box checked.

        // first groups

        QueryNode root4 = new QueryDataNode(groupSchema.EMAILOK, QueryDataNode.DEFINED, null);
        Query query4 = new Query(groupSchema.BASE, root4, false);

        // and then user netgroups

        QueryNode root5 = new QueryDataNode(userNetgroupSchema.EMAILOK, QueryDataNode.DEFINED, null);
        Query query5 = new Query(userNetgroupSchema.BASE, root4, false);

        QueryResult result = editset.getDBSession().getGSession().query(query1, this);

        result.append(editset.getDBSession().getGSession().query(query2, this));
        result.append(editset.getDBSession().getGSession().query(query3, this));
        result.append(editset.getDBSession().getGSession().query(query4, this));
        result.append(editset.getDBSession().getGSession().query(query5, this));

        membersChoice = result;
      }

    return membersChoice;
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
   * <p>By default, the 3 variants of the DBEditObject anonymousLinkOK()
   * method are chained together, so that the customizer can choose
   * which level of detail he is interested in.
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.server.InvidDBField#bind(arlut.csd.ganymede.common.Invid,arlut.csd.ganymede.common.Invid,boolean) bind()}
   * method calls this version.  This version calls the three parameter
   * version, which calls the two parameter version, which returns
   * false by default.  Customizers can implement any of the three
   * versions, but unless you maintain the version chaining yourself,
   * there's no point to implementing more than one of them.</P>
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
        (sourceObject.getTypeID() == emailListSchema.BASE) &&
        (sourceFieldID == emailListSchema.MEMBERS))
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

    if (fieldid == emailListSchema.LISTNAME)
      {
        return true;
      }

    return super.fieldRequired(object, fieldid);
  }
}
