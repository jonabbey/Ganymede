/*

   FieldInfo.java

   This class is a serializable object to return all the information
   the container panel needs to render a field.
   
   Created: 4 November 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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

package arlut.csd.ddroid.common;

import java.rmi.RemoteException;

import arlut.csd.ddroid.rmi.db_field;
import arlut.csd.ddroid.server.PasswordDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldInfo

------------------------------------------------------------------------------*/

/**
 * <p>This class is a serializable object used to return all the value information
 * the client's {@link arlut.csd.ddroid.client.containerPanel containerPanel}
 * needs to render a specific field instance, including the current value held
 * in this field and the current editability/visibility this field has with respect
 * to the user's {@link arlut.csd.ddroid.server.GanymedeSession GanymedeSession}..</p>
 *
 * <p>The {@link arlut.csd.ddroid.common.FieldTemplate FieldTemplate} object is used to return
 * the invariant (during the client's connection) type information associated
 * with the field generically across all objects of the type containing this field.</p>
 *
 * @version $Id$
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

  /**
   * <p>This constructor takes a {@link arlut.csd.ddroid.rmi.db_field
   * db_field} interface instead of a {@link
   * arlut.csd.ddroid.server.DBField DBField} object so that we can
   * avoid referencing a server-side object in a transport class, but
   * we will create FieldInfo objects from DBFields on the server side.</p>
   */

  public FieldInfo(db_field field)
  {
    this.field = field;

    try
      {
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
    catch (RemoteException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   * <p>Returns the a remote reference to the field on the server.</p>
   */

  public db_field getField()
  {
    return field;
  }

  /**
   * <p>Returns the field's id number within the containing {@link
   * arlut.csd.ddroid.server.DBObject DBObject} on the server.</p>
   */

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

  /**
   * <p>Returns the value of this field.  The Object may be a String,
   * a Date, a Double, an Integer, an {@link
   * arlut.csd.ddroid.common.Invid Invid}, or even a {@link
   * java.util.Vector Vector} in the case of a multi-value field.</p>
   */

  public Object getValue()
  {
    return value;
  }
}
