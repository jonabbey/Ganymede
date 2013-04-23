/*

   emailRedirectCustom.java

   Custom plug-in for managing fields in the email redirect object type.

   Created: 25 June 1999

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                             emailRedirectCustom

------------------------------------------------------------------------------*/

/**
 *   Custom plug-in for managing fields in the email redirect object type.
 */

public class emailRedirectCustom extends DBEditObject implements SchemaConstants, emailRedirectSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public emailRedirectCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public emailRedirectCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public emailRedirectCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
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
   * to it.</p>
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
   * them.</p>
   *
   * <p>Note that the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of
   * anonymousLinkOK() with a null targetObject, to determine that the
   * client should not use its cache for an InvidDBField's choices.
   * Any overriding done of this method must be able to handle a null
   * targetObject, or else an exception will be thrown
   * inappropriately.</p>
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
   * @param sourceObject The object on the other side of the proposed link
   * @param sourceFieldID  The field on the other side of the proposed link
   * @param gsession Who is trying to do this linking?
   */

  @Override public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
                                           DBObject sourceObject, short sourceFieldID,
                                           GanymedeSession gsession)
  {
    // if someone tries to put this redirect in a email list,
    // let them.

    if ((targetFieldID == SchemaConstants.BackLinksField) &&
        (sourceObject.getTypeID() == emailListSchema.BASE) && // email list
        (sourceFieldID == emailListSchema.MEMBERS)) // email list members
      {
        return true;
      }

    // the default anonymousLinkOK() method returns false

    return super.anonymousLinkOK(targetObject, targetFieldID,
                                 sourceObject, sourceFieldID, gsession);
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any vector add operation, and to take any special actions in
   * reaction to the add.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us is guaranteed
   * to proceed to make the change to its vector.  If this method
   * returns a non-success code in its ReturnVal, the DBField that
   * called us will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the &lt;value&gt;
   * parameter carries the value to be added.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    if (field.getID() != emailRedirectSchema.TARGETS)
      {
        return null;
      }

    Vector<String> newItemVect = new Vector<String>();

    newItemVect.add((String)value);

    if (!fitsInNIS(newItemVect))
      {
        return Ganymede.createErrorDialog("Overflow error",
                                          "The address that you are attempting to add to the " + getTypeName() +
                                          " email redirection cannot all fit.  No NIS email alias definition in the laboratory's " +
                                          "network can be longer than 1024 characters.");
      }

    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any vector-vector add operation, and to take any special
   * actions in reaction to the add.. if this method returns null or a
   * success code in its ReturnVal, the DBField that called us is
   * guaranteed to proceed to make the change to its vector.  If this
   * method returns a non-success code in its ReturnVal, the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.</p>
   *
   * <p>The &lt;field&gt; parameter identifies the field that is
   * requesting approval for item deletion, and the &lt;submittedValues&gt;
   * parameter carries the values to be added.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including vector bounds, etc.) before
   * calling this method.  Under normal circumstances, we won't need
   * to do anything here.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal finalizeAddElements(DBField field, Vector submittedValues)
  {
    if (field.getID() != emailRedirectSchema.TARGETS)
      {
        return null;
      }

    if (!fitsInNIS((Vector<String>)submittedValues))
      {
        return Ganymede.createErrorDialog("Overflow error",
                                          "The " + submittedValues.size() +
                                          " addresses that you are attempting to add to the " + getTypeName() +
                                          " email redirection cannot all fit.  No NIS email list in the laboratory's " +
                                          "network can be longer than 1024 characters when converted to an " +
                                          "NIS email alias definition.");
      }

    return null;
  }

  /**
   * <p>This method takes a vector of new items and returns true if the new items should
   * be able to fit in the NIS line built from this emailList object.</p>
   */

  private boolean fitsInNIS(Vector<String> newItemVect)
  {
    StringDBField targets = (StringDBField) getField(emailRedirectSchema.TARGETS);

    int totalLength = targets.getValueString().length();

    for (String label: newItemVect)
      {
        totalLength += label.length() + 2; // need a comma and space

        if (totalLength >= 1024)
          {
            return false;
          }
      }

    return true;
  }
}
