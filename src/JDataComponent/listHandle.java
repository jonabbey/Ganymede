/*

   listHandle.java

   A wrapper for Strings and Objects for use in JstringListBox.

   Created: 25 Aug 1997
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 1999/03/22 22:37:56 $
   Module By: Mike Mulvaney

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

package arlut.csd.JDataComponent;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      listHandle

------------------------------------------------------------------------------*/

/**
 *
 * A wrapper to hold both a String and (optionally) a related object,
 * such as an Invid.  The JstringListBox uses them to allow the client
 * to manipulate labeled object pointers.<br><br>
 *
 * listHandle's are also used extensively throughout the client code to
 * handle client-side object label caching.
 * 
 * @see JstringListBox
 * @version $Revision: 1.6 $ $Date: 1999/03/22 22:37:56 $ $Name:  $
 * @author Mike Mulvaney
 *
 */

public class listHandle {

  String
    label;

  Object
    object = null;

  /* -- */

  public listHandle(String label)
  {
    this(label, null);
  }

  public listHandle(String label, Object object)
  {
    this.label = label;
    this.object = object;
  }

  public String getLabel()
  {
    return label;
  }

  public String toString()
  {
    return label;
  }

  public void setLabel(String label)
  {
    this.label = label;
  }

  public Object getObject()
  {
    return object;
  }

  public void setObject(Object object)
  {
    this.object = object;
  }

  /**
   * Returns the value of this listHandle.
   *
   * If the object has not been set, getValue() returns the label.  Otherwise,
   * the object is returned.
   */

  public Object getValue()
  {
    if (object == null)
      {
	return label;
      }

    return object;
  }

  public boolean equals(Object val)
  {
    if (!(val instanceof listHandle))
      {
	return false;
      }

    listHandle handle = (listHandle) val;

    if (handle == null)
      {
	return false;
      }

    if (object != null)
      {
	return object.equals(handle.object) &&
	  label.equals(handle.label);
      }
    else if (label != null)
      {
	return label.equals(handle.label);
      }
    else
      {
	return false;
      }
  }
}
