/*

   Compare.java

   Comparison interface for arlut.csd.Util sort classes.

   Created: 24 April 1997

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

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                         Compare

------------------------------------------------------------------------------*/

/**
 * <p>This interface provides a common comparator operator that can be implemented
 * for use with
 * {@link arlut.csd.Util.VecQuickSort VecQuickSort}, and
 * {@link arlut.csd.Util.VecSortInsert VecSortInsert}.</p>
 *
 * @version Id: 1.4 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface Compare {

  /**
   * <p>Comparator for arlut.csd.Util sort classes.</p>
   *
   * @return -1 if a &lt; b, 0 if a = b, and 1 if a &gt; b in sort order.
   */

  public int compare(Object a, Object b);
}
