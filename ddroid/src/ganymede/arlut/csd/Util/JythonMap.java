/*
   GASH 2

   JythonMap.java

   The GANYMEDE object storage system.

   Created: 28 July 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Deepak Giridharagopal <deepak@arlut.utexas.edu>

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
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
   * {@link java.util.Map.contains contains} method.
   * </p>
   * 
   * @param key
   * @return True or False based on if the Map has the given key
   */
  boolean has_key(Object key);

  /**
   * <p>
   * This is the python analogue to Java's
   * {@link java.util.Map.entrySet entrySet} method. Instead of returning a
   * {@link java.util.Set Set} of {@link java.util.Map.Entry Entry} objects,
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
   * {@link java.util.Map.keySet keySet} method.
   * </p>
   * @return A {@link java.util.Set Set} containing all keys for this
   * {@link java.util.Map Map}
   */
  Set keys();
  
  /**
   * This method should not be relied upon to generate a friendly, general-purpose
   * string representation of this object. For use in a Jython context, this method
   * should display some information about the {@link java.util.Map Map} keys it
   * contains.
   */ 
  String toString();
}
