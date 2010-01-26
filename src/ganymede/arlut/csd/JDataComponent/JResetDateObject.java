
/*
   JSetValueObject.java

   Subclass of JValueObject that represents a calendar reset operation

   Created: 13 June 2008


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

import java.util.Date;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JSetValueObject

------------------------------------------------------------------------------*/

/**
 * <p>Subclass of JValueObject that represents a calendar reset operation</p>
 *   
 * @version $Revision$ $Date$ $Name:  $
 * @author Jonathan Abbey
 */

public class JResetDateObject extends JValueObject {

  private Component source;
  private Date value;

  /* -- */

  public JResetDateObject(Component source, Date value)
  {
    this.source = source;
    this.value = value;
  }

  /**
   * Returns the arlut.csd.JDataComponent GUI component that originated this message.
   */

  public Component getSource()
  {
    return source;
  }

  /**
   * This method is intended to allow re-sourcing of the component for
   * this object, so that a component generating a callback can be
   * wrapped in higher level components whose identity is known to the
   * ultimate client of the component.
   *
   * See JpopUpCalendar for an example of the use of this method.
   */

  public void setSource(Component newSource)
  {
    this.source = newSource;
  }

  /**
   * Returns an auxiliary value.  Used for passing information about pop-up menu items, but may
   * be used for different purposes if needed.
   */

  public Object getParameter() 
  {
    return null;
  }

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public int getIndex() 
  {
    return -1;
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
   * Returns the value of the object being affected by this message.
   */

  public Date getDateValue()
  {
    return value;
  }

  /**
   * The callback can call this method to set the date that the
   * calendar object should reset the date to.
   *
   * If newDate is null, the calendar will revert to an unset state.
   */

  public void setTransformedDate(Date newDate)
  {
    this.value = newDate;
  }

  /**
   *
   * Method to get a human-readable description of the event carried
   * by this object
   * 
   */

  public String toString()
  {
    return source.toString() +  " reset date " + String.valueOf(value);
  }
}
