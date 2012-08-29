
/*
   JParameterValueObject.java

   Subclass of JValueObject that represents an action performed on an
   item in a list.

   Created: 25 October 2004


   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin.

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

package arlut.csd.JDataComponent;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                JParameterValueObject

------------------------------------------------------------------------------*/

/**
 * <p>Subclass of JValueObject that represents the movement of a value in
 * a list.</p>
 *
 * @version $Revision$ $Date$ $Name:  $
 * @author Jonathan Abbey
 */

public class JParameterValueObject extends JValueObject {

  private Component source;
  private Object value = null;
  private int index = -1;
  private Object parameter = null;

  /* -- */

  public JParameterValueObject(Component source, int index, Object itemValue, Object parameter)
  {
    this.source = source;
    this.index = index;
    this.value = itemValue;
    this.parameter = parameter;
  }

  /**
   * Returns the arlut.csd.JDataComponent GUI component that originated this message.
   */

  public Component getSource()
  {
    return source;
  }

  /**
   * Returns an auxiliary value.  Used for passing information about pop-up menu items, but may
   * be used for different purposes if needed.
   */

  public Object getParameter() 
  {
    return parameter;
  }

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public int getIndex() 
  {
    return index;
  }

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public int getIndex2() 
  {
    return -1;
  }

  /**
   * Returns the value of the object being affected by this message.
   */

  public Object getValue() 
  {
    return value;
  }

  /**
   *
   * Method to get a human-readable description of the event carried
   * by this object
   * 
   */

  public String toString()
  {
    return source.toString() +  " parameter(" + index + ", " + String.valueOf(value) + ", " + String.valueOf(parameter) + ")";
  }
}
