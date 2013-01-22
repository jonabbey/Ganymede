/*

   QueryDescriber.java

   An interface that can be used to provide for object and field name
   look ups when producing toString() output from a Query chain on the
   server

   Created: 4 January 2013

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.common;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                  QueryDescriber

------------------------------------------------------------------------------*/

/**
 * <p>A simple interface that can be used on the Ganymede server to
 * provide name lookups for the toString() method of a Query object
 * chain.</p>
 */

public interface QueryDescriber {

  /**
   * Returns a description of the type id.
   */

  public String describeType(short objType);

  /**
   * Returns a description of the field id.
   */

  public String describeField(short objType, short fieldType);

  /**
   * Returns a description of the field id.
   */

  public String describeField(String objTypeName, short fieldType);
}

