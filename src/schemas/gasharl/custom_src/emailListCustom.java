/*

   emailListCustom.java

   Custom plug-in for managing fields in the email list object type.
   
   Created: 16 February 1999
   Release: $Name:  $
   Version: $Revision: 1.16 $
   Last Mod Date: $Date: 2003/03/12 03:48:40 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

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
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
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

	Query query1 = new Query(SchemaConstants.UserBase, null, false); // list all users

	Query query2 = new Query((short) 275, null, false); // list all external email targets
	
	QueryNode root3 = new QueryNotNode(new QueryDataNode((short) -2, QueryDataNode.EQUALS, this.getInvid()));
	Query query3 = new Query((short) 274, root3, false); // list all other email groups, but not ourselves
	
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
   */

  public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    if (field.getID() != emailListSchema.MEMBERS && field.getID() != emailListSchema.EXTERNALTARGETS)
      {
	return null;
      }

    Vector newItemVect = new Vector();
    
    newItemVect.addElement(value);

    if (!fitsInNIS(newItemVect))
      {
	return Ganymede.createErrorDialog("Overflow error",
					  "The item that you are attempting to add to the " + getTypeName() + 
					  " email list cannot fit.  No NIS email list in the laboratory's " +
					  "network can be longer than 1024 characters when converted to an " +
					  "NIS email alias definition.\n\n" +
					  "If you need this list to be expanded, you should create a new sublist " +
					  "for the overflow items, move some items from this list to the new sublist, " +
					  " and then add the new sublist to this list.");
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
   */

  public ReturnVal finalizeAddElements(DBField field, Vector submittedValues)
  {
    if (field.getID() != emailListSchema.MEMBERS && field.getID() != emailListSchema.EXTERNALTARGETS)
      {
	return null;
      }

    if (!fitsInNIS(submittedValues))
      {
	return Ganymede.createErrorDialog("Overflow error",
					  "The " + submittedValues.size() + 
					  " items that you are attempting to add to the " + getTypeName() + 
					  " email list cannot all fit.  No NIS email list in the laboratory's " +
					  "network can be longer than 1024 characters when converted to an " +
					  "NIS email alias definition.\n\n" +
					  "If you need this list to be expanded, you should create a new sublist " +
					  "for the overflow items, and add the sublist to this list.");
      }

    return null;
  }

  /**
   * <p>This method takes a vector of new items and returns true if the new items should
   * be able to fit in the NIS line built from this emailList object.</p>
   */

  private boolean fitsInNIS(Vector newItemVect)
  {
    StringDBField externalTargets = (StringDBField) getField(emailListSchema.EXTERNALTARGETS);
    InvidDBField members = (InvidDBField) getField(emailListSchema.MEMBERS);

    int totalLength = externalTargets.getValueString().length() + members.getValueString().length();

    for (int i = 0; i < newItemVect.size(); i++)
      {
	Object value = newItemVect.elementAt(i);

	if (value instanceof Invid)
	  {
	    Invid newInvid = (Invid) value;

	    GanymedeSession gsession = getGSession();

	    if (gsession != null)
	      {
		DBObject objectRef = gsession.getSession().viewDBObject(newInvid);

		if (objectRef != null)
		  {
		    String label = objectRef.getLabel();

		    if (label != null)
		      {
			totalLength += (label.length() + 2); // need a comma and space
		      }
		  }
	      }
	  }
	else if (value instanceof String)
	  {
	    totalLength += (((String) value).length() + 2); // need a comma and space
	  }

	if (totalLength >= 1024)
	  {
	    return false;
	  }
      }

    return true;
  }
}
