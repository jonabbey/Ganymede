/*

   VecSortInsert.java

   This class is used to do an ordered insert using a binary
   search.  It's designed for speed.
   
   Created: 6 February 1998


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

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

import java.util.Comparator;
import java.util.Vector;

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
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public class VecSortInsert implements Comparator {

  static final boolean debug = false;

  /**
   * The Comparator we're using to handle object insertion into a
   * given Vector.
   */

  Comparator comparator;

  /* -- */

  public static final Comparator defaultComparator = new Comparator() 
    {
      public int compare(Object a, Object b) 
        {
          int comp = 0;
          
          comp = a.toString().compareToIgnoreCase(b.toString());
          
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
    };

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

    VecSortInsert inserter = new VecSortInsert();

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

    System.out.println("\nInserting b");

    inserter.insert(test, "b");

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

    System.out.println("\nInserting k");

    inserter.insert(test, "k");
    printTest(test);

    System.out.println("\nInserting 5");

    inserter.insert(test, "5");
    printTest(test);

    System.out.println("\nInserting i");

    inserter.insert(test, "i");
    printTest(test);

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

  /**
   * This static method does the work.
   */

  public static void insert(Vector objects, Object element, Comparator comparatorParam)
  {
    Comparator myComparator;
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

    if (comparatorParam == null)
      {
        myComparator = defaultComparator;
      }
    else
      {
        myComparator = comparatorParam;
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

        if (myComparator.compare(element,objects.elementAt(mid)) < 0)
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

    if ((mid == objects.size()-1) && myComparator.compare(element, objects.elementAt(objects.size()-1)) > 0)
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

        //      printTest(objects.size(), low, mid, high);
      }
  }

  /**
   * <p>Constructor.  By not specifying a comparator, an ordinary
   * string comparison will be performed on elements inserted.</p>
   */

  public VecSortInsert()
  {
    this.comparator = defaultComparator;
  }

  /**
   * <p>Constructor.  If comparator is null, an ordinary string
   * comparison will be performed on elements inserted.</p>
   */

  public VecSortInsert(Comparator comparatorParam)
  {
    if (comparatorParam == null)
      {
        this.comparator = defaultComparator;
      }
    else
      {
        this.comparator = comparatorParam;
      }
  }

  /**
   * This method does the work.
   */

  public void insert(Vector objects, Object element)
  {
    VecSortInsert.insert(objects, element, this.comparator);
  }

  /**
   * <p>Default comparator, does a string comparison on the
   * toString() output of the objects for ordering.</p>
   */

  public int compare(Object a, Object b)
  {
    return a.toString().compareTo(b.toString());
  }
}
