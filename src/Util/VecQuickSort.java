/*

   VecQuickSort.java

   A Vector implementation of the QuickSort algorithm.
   
   Created: 12 August 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:05 $
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

import java.util.Vector;

/* from Fundamentals of Data Structures in Pascal, 
        Ellis Horowitz and Sartaj Sahni,
	Second Edition, p.339
	Computer Science Press, Inc.
	Rockville, Maryland
	ISBN 0-88175-165-0 */

public class VecQuickSort implements Compare {

  Vector objects;
  Compare comparator;

  /* -- */

  /**
   *
   * VecQuickSort constructor.
   *
   * @param objects Vector of objects to be sorted in place
   * @param comparator Compare object.. if null, standard string compare
   * will be done.
   *
   */

  public VecQuickSort(Vector objects, Compare comparator)
  {
    this.objects = objects;

    if (comparator == null)
      {
	this.comparator = this;
      }
    else
      {
	this.comparator = comparator;
      }
  }

  void quick(int first, int last)
  {
    int 
      i,
      j;

    Object
      k, 
      tmp;

    if (first<last)
      {
	i = first; j = last+1; k = objects.elementAt(first);
	do
	  {
	    do
	      {
		i++;
	      } while ((i <= last) && comparator.compare(objects.elementAt(i), k) < 0);

	    do
	      {
		j--;
	      } while ((j >= first) && comparator.compare(objects.elementAt(j), k) > 0);

	    if (i < j)
	      {
		tmp=objects.elementAt(j);
		objects.setElementAt(objects.elementAt(i), j);
		objects.setElementAt(tmp, i);
	      }
	  } while (j > i);

	tmp = objects.elementAt(first);
	objects.setElementAt(objects.elementAt(j), first);
	objects.setElementAt(tmp, j);
	quick(first, j-1);
	quick(j+1, last);
      }
  }

  public void sort()
  {
    if (objects.size() < 2)
      {
	return;
      }
    
    quick(0, objects.size()-1);
  }

  public int compare(Object a, Object b)
  {
    String sa, sb;
      
    sa = (String) a;
    sb = (String) b;

    return sa.compareTo(sb);
  }

}
