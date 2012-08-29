/*

   CaseInsensitiveSet.java

   A non thread-safe case insensitive Set of String objects.
   
   Created: 30 March 2010

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/*------------------------------------------------------------------------------
                                                                           class
                                                              CaseInsensitiveSet

------------------------------------------------------------------------------*/

/**
 * <p>CaseInsensitiveSet is a variant of a HashSet of String objects
 * that forces all Strings added / tested against the set to lower
 * case before adding / comparing.</p>
 *
 * <p>All Strings retrieved from a CaseInsensitiveSet will have been
 * converted to lower case at add() time, so if you iterate over
 * members of a CaseInsensitiveSet you will not get any String values
 * back with upper case characters.</p>
 */

final public class CaseInsensitiveSet extends HashSet<String> {

  /* -- */

  public CaseInsensitiveSet()
  {
    super();
  }

  public CaseInsensitiveSet(Collection<String> c)
  {
    super(c);
  }

  public CaseInsensitiveSet(int initialCapacity, float loadFactor)
  {
    super(initialCapacity, loadFactor);
  }

  public CaseInsensitiveSet(int initialCapacity)
  {
    super(initialCapacity);
  }

  public boolean contains(String s)
  {
    return super.contains(s.toLowerCase());
  }

  public boolean add(String s)
  {
    s = s.toLowerCase();

    return super.add(s);
  }

  public boolean remove(String s)
  {
    s = s.toLowerCase();

    return super.remove(s);
  }
}
