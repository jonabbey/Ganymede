/*

   emailRedirectCustom.java

   Custom plug-in for managing fields in the email redirect object type.
   
   Created: 25 June 1999
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/06/25 22:01:26 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.VectorUtils;

import java.util.*;

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
    // if someone tries to put this redirect in a email list,
    // let them.

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

}
