/*
   GASH 2

   JythonMap.java

   The GANYMEDE object storage system.

   Created: 28 July 2004

   Module By: Deepak Giridharagopal <deepak@arlut.utexas.edu>

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
package arlut.csd.Util;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specifies some additional methods of interest for Jython-based access
 * to a Map.
 */
public interface JythonMap extends Map {

  /**
   * <p>
   * This is the python analogue to Java's
   * {@link java.util.Map#containsKey(java.lang.Object) containsKey} method.
   * </p>
   * 
   * @param key
   * @return True or False based on if the Map has the given key
   */
  boolean has_key(Object key);

  /**
   * <p>
   * This is the python analogue to Java's
   * {@link java.util.Map#entrySet()} method. Instead of returning a
   * {@link java.util.Set Set} of {@link java.util.Map.Entry} objects,
   * this instead should return a List of Object[] arrays of size 2. The first
   * element in the array is the key, the second is the value.
   * </p>
   * 
   * @return List of Object[2] (aka key/value tuples)
   */
  List items();
  
  /**
   * <p>
   * This is the python analogue to Java's
   * {@link java.util.Map#keySet()} method.
   * </p>
   * @return A {@link java.util.Set Set} containing all keys for this
   * java.util.Map Map
   */
  Set keys();
  
  /**
   * This method should not be relied upon to generate a friendly, general-purpose
   * string representation of this object. For use in a Jython context, this method
   * should display some information about the java.util.Map Map keys it
   * contains.
   */ 
  String toString();
}
