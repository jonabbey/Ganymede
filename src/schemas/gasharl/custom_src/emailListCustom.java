/*

   emailListCustom.java

   Custom plug-in for managing fields in the email list object type.
   
   Created: 16 February 1999
   Release: $Name:  $
   Version: $Revision: 1.10 $
   Last Mod Date: $Date: 2001/11/05 20:58:39 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.VectorUtils;

import java.util.*;

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
   * <P>This method is used to provide a hook to allow different
   * objects to generate different labels for a given object
   * based on their perspective.  This is used to sort
   * of hackishly simulate a relational-type capability for
   * the purposes of viewing backlinks.</P>
   *
   * <P>See the automounter map and NFS volume DBEditObject
   * subclasses for how this is to be used, if you have
   * them.</P>
   */
  
  public String lookupLabel(DBObject object)
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
    
    if (object.getTypeID() == 274)
      {
	return super.lookupLabel(object) + " (email list)";
      }

    // mark external email records

    if (object.getTypeID() == 275)
      {
	Vector addresses = object.getFieldValuesLocal((short) 257);
	
	return super.lookupLabel(object) + " (" + VectorUtils.vectorString(addresses) + ")";
      }

    return super.lookupLabel(object);
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
    // we don't want the members field to be cached, since we are
    // amalgamating several kinds of things into one invid field.

    if (field.getID() == MEMBERS)
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

  public QueryResult obtainChoiceList(DBField field, boolean applyFilter)
  {
    if (field.getID() != emailListSchema.MEMBERS)
      {
	return super.obtainChoiceList(field, applyFilter);
      }

    if (membersChoice == null)
      {
	// we want to present a list of all users, mail groups besides
	// this one, and external mail aliases (email addresses that
	// have local aliases in ARL's mail system) as valid choices
	// for the MEMBERS field.

	Query query1 = new Query(SchemaConstants.UserBase, null, false); // list all users
	query1.setFiltered(applyFilter);

	Query query2 = new Query((short) 275, null, false); // list all external email targets
	query2.setFiltered(applyFilter);
	
	QueryNode root3 = new QueryNotNode(new QueryDataNode((short) -2, QueryDataNode.EQUALS, this.getInvid()));
	Query query3 = new Query((short) 274, root3, false); // list all other email groups, but not ourselves
	query3.setFiltered(applyFilter);
	
	QueryResult result = editset.getSession().getGSession().query(query1, this);

	result.append(editset.getSession().getGSession().query(query2, this));
	result.append(editset.getSession().getGSession().query(query3, this));
	
	membersChoice = result;
      }

    return membersChoice;
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.DBObject DBObject} type when the
   * user only has editing access for the source 
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField} and not
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
   * {@link arlut.csd.ganymede.InvidDBField InvidDBField}'s
   * {@link arlut.csd.ganymede.InvidDBField#bind(arlut.csd.ganymede.Invid,arlut.csd.ganymede.Invid,boolean) bind()}
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

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID,
				 DBObject sourceObject, short sourceFieldID,
				 GanymedeSession gsession)
  {
    // if someone tries to put this list in another email list, let
    // them.

    if ((targetFieldID == SchemaConstants.BackLinksField) &&
	(sourceObject.getTypeID() == 274) && // email list
	(sourceFieldID == 257))	// email list members
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

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    // the email list name is required

    if (fieldid == 256)
      {
	return true;
      }

    return super.fieldRequired(object, fieldid);
  }

}
