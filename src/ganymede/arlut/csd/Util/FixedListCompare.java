/*

   FixedListCompare.java

   Comparison interface for arlut.csd.Util sort classes.
   
   Created: 3 July 2001
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2001/07/03 21:50:03 $
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

import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                FixedListCompare

------------------------------------------------------------------------------*/

/**
 * <P>This class implements the {@link arlut.csd.Util.Compare Compare}
 * interface, and provides a sort comparator that can sort things
 * according to a fixed ordering.  Items not in the original ordered
 * list will be placed after items in the original list, and in
 * alphabetical toString() order relative to each other.</P>
 *
 * @version $Revision: 1.2 $ $Date: 2001/07/03 21:50:03 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class FixedListCompare implements Compare {

  final static boolean debug = false;

  // ---

  private Vector items;
  private Compare secondaryComparator;

  /**
   * @param items A list of items whose sort order we will impart on comparisons
   * @param secondaryComparator A Compare object that we will pass objects to
   * that are both missing from the items list.
   */

  public FixedListCompare(Vector items, Compare secondaryComparator)
  {
    this.items = items;
    this.secondaryComparator = secondaryComparator;
  }

  /**
   *
   * Comparator for arlut.csd.Util sort classes.  compare returns
   * -1 if a < b, 0 if a = b, and 1 if a > b in sort order.
   *
   */

  public int compare(Object a, Object b)
  {
    if (debug)
      {
	System.err.println("Comparing a=" + a + "(" + a.getClass() + "), b=" + b + "(" + b.getClass() + "):");
      }

    int ai = items.indexOf(a);
    int bi = items.indexOf(b);

    if (ai == -1 && bi == -1)
      {
	if (debug)
	  {
	    System.err.println("\tCouldn't find either.");
	  }

	if (secondaryComparator == null)
	  {
	    return a.toString().compareTo(b.toString());
	  }
	else
	  {
	    return secondaryComparator.compare(a, b);
	  }
      }
    else if (ai == -1)
      {
	if (debug)
	  {
	    System.err.println("\treturning 1");
	  }

	return 1;
      }
    else if (bi == -1)
      {
	if (debug)
	  {
	    System.err.println("\treturning -1");
	  }

	return -1;
      }
    else
      {
	if (ai == bi)
	  {
	    if (debug)
	      {
		System.err.println("\treturning 0");
	      }

	    return 0;
	  }

	if (ai < bi)
	  {
	    if (debug)
	      {
		System.err.println("\treturning -1");
	      }

	    return -1;
	  }

	// ai > bi

	if (debug)
	  {
	    System.err.println("\treturning 1");
	  }

	return 1;
      }
  }
}
