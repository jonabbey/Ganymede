/*

   FieldInfo.java

   This class is a serializable object to return all the information
   the container panel needs to render a field.
   
   Created: 4 November 1997
   Release: $Name:  $
   Version: $Revision: 1.12 $
   Last Mod Date: $Date: 2002/03/02 01:43:52 $
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

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldInfo

------------------------------------------------------------------------------*/

/**
 * <p>This class is a serializable object used to return all the value information
 * the client's {@link arlut.csd.ganymede.client.containerPanel containerPanel}
 * needs to render a specific field instance, including the current value held
 * in this field and the current editability/visibility this field has with respect
 * to the user's {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}..</p>
 *
 * <p>The {@link arlut.csd.ganymede.FieldTemplate FieldTemplate} object is used to return
 * the invariant (during the client's connection) type information associated
 * with the field generically across all objects of the type containing this field.</p>
 *
 * @version $Revision: 1.12 $ $Date: 2002/03/02 01:43:52 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class FieldInfo implements java.io.Serializable {

  static final long serialVersionUID = -3986768111784239002L;

  // ---

  db_field
    field;

  short
    ID;

  boolean
    defined,
    editable,
    visible;

  Object
    value;

  /* -- */

  public FieldInfo(DBField field)
  {
    this.field = (db_field) field;

    if (field instanceof PasswordDBField)
      {
	value = field.getValueString(); // this doesn't check permissions!
      }
    else if (!field.isVector())
      {
	value = field.getValue(); // can throw IllegalArgumentException on perms failure
      }
    else
      {
	value = field.getValues();// can throw IllegalArgumentException on perms failure
      }

    defined = field.isDefined();
    editable = field.isEditable();
    visible = field.isVisible();

    ID = field.getID();
  }

  public db_field getField()
  {
    return field;
  }

  public short getID()
  {
    return ID;
  }

  public boolean isDefined()
  {
    return defined;
  }

  public boolean isEditable()
  {
    return editable;
  }

  public boolean isVisible()
  {
    return visible;
  }

  public Object getValue()
  {
    return value;
  }

}
