/*

   Compare.java

   Comparison interface for arlut.csd.Util sort classes.
   
   Created: 24 April 1997
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2001/07/03 17:43:27 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                         Compare

------------------------------------------------------------------------------*/

/**
 * <P>This interface provides a common comparator operator that can be implemented
 * for use with {@link arlut.csd.Util.QuickSort QuickSort}, 
 * {@link arlut.csd.Util.VecQuickSort VecQuickSort}, and
 * {@link arlut.csd.Util.VecSortInsert VecSortInsert}.
 *
 * @version $Revision: 1.4 $ $Date: 2001/07/03 17:43:27 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public interface Compare {

  /**
   *
   * Comparator for arlut.csd.Util sort classes.  compare returns
   * -1 if a < b, 0 if a = b, and 1 if a > b in sort order.
   *
   */

  public int compare(Object a, Object b);
}
