/*

   VecSortInsert.java

   This class is used to do an ordered insert using a binary
   search.  It's designed for speed.
   
   Created: 6 February 1998
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 2000/03/01 04:46:22 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.Util;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   VecSortInsert

------------------------------------------------------------------------------*/

/**
 * <P>This class is used to do an ordered insert using a binary search.  
 * It's designed for speed.  Used in the Ganymede client to efficiently
 * add new items to the client's object tree.  Uses the
 * {@link arlut.csd.Util.Compare Compare} interface for ordering
 * comparisons.</P>
 *
 * @version $Revision: 1.5 $ $Date: 2000/03/01 04:46:22 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public class VecSortInsert {

  Compare comparator;
  static final boolean debug = false;

  /* -- */

  // debug rig
  
  public static void main(String[] argv)
  {
    Vector test = new Vector();

    test.addElement("B");
    test.addElement("C");
    test.addElement("E");
    test.addElement("H");
    test.addElement("J");
    test.addElement("N");
    test.addElement("O");
    test.addElement("Q");
    test.addElement("X");
    test.addElement("Y");

    VecSortInsert inserter = new VecSortInsert(new arlut.csd.Util.Compare() 
					       {
						 public int compare(Object o_a, Object o_b) 
						   {
						     String a, b;
						     
						     a = (String) o_a;
						     b = (String) o_b;
						     int comp = 0;
						     
						     comp = a.compareTo(b);
						     
						     if (comp < 0)
						       {
							 return -1;
						       }
						     else if (comp > 0)
						       { 
							 return 1;
						       } 
						     else
						       { 
							 return 0;
						       }
						   }
					       });

    System.out.println("Start: ");
    printTest(test);

    System.out.println("\nInserting A");

    inserter.insert(test, "A");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting K");

    inserter.insert(test, "K");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting G");

    inserter.insert(test, "G");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting I");

    inserter.insert(test, "I");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting Z");

    inserter.insert(test, "Z");

    System.out.println("Result: ");
    printTest(test);

    System.out.println("\nInserting K");

    inserter.insert(test, "K");

    System.out.println("Result: ");
    printTest(test);
  }

  static void printTest(Vector vec)
  {
    for (int i = 0; i < vec.size(); i++)
      {
	System.out.print(vec.elementAt(i));
	System.out.print("  ");
      }

    System.out.println();
  }

  static void printTest(int size, int low, int med, int high)
  {
    for (int i = 0; i < size; i++)
      {
	if (i == low)
	  {
	    System.out.print("l");
	  }
	else
	  {
	    System.out.print(" ");
	  }

	if (i == med)
	  {
	    System.out.print("m");
	  }
	else
	  {
	    System.out.print(" ");
	  }

	if (i == high)
	  {
	    System.out.print("h");
	  }
	else
	  {
	    System.out.print(" ");
	  }
      }

    System.out.println();
  }

  public VecSortInsert(Compare comparator)
  {
    this.comparator = comparator;
  }

  /**
   *
   * This method does the work.
   *
   */

  public void insert(Vector objects, Object element)
  {
    int low, high, mid;

    /* -- */

    if (objects.size() == 0)
      {
	if (debug)
	  {
	    System.err.println("Inserting " + element + " at 0 to start list");
	  }

	objects.addElement(element);
	return;
      }

    // java integer division rounds towards zero

    low = 0;
    high = objects.size()-1;
    
    mid = (low + high) / 2;

    while (low < high)
      {
	if (false)
	  {
	    printTest(objects.size(), low, mid, high);
	  }

	if (comparator.compare(element,objects.elementAt(mid)) < 0)
	  {
	    high = mid;
	  }
	else
	  {
	    low = mid + 1;
	  }

	mid = (low + high) / 2;
      }

    if (false)
      {
	printTest(objects.size(), low, mid, high);
      }

    if ((mid == objects.size()-1) && comparator.compare(element, objects.elementAt(objects.size()-1)) > 0)
      {
	if (debug)
	  {
	    System.err.println("Inserting " + element + " at " + mid + " (end) of " + objects.size());
	  }
	
	objects.addElement(element);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("Inserting " + element + " at " + mid + " in " + objects.size());
	  }
	
	objects.insertElementAt(element, mid);
      }

    if (debug)
      {
	for (int i = 0; i < objects.size(); i++)
	  {
	    if (i > 0)
	      {
		System.err.print(" ");
	      }

	    System.err.print(objects.elementAt(i));
	  }

	System.err.println();

	//	printTest(objects.size(), low, mid, high);
      }
  }
}
