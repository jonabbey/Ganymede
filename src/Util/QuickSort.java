/*

   QuickSort.java

   An implementation of the QuickSort algorithm.
   
   Created: 24 April 1997
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

/* from Fundamentals of Data Structures in Pascal, 
        Ellis Horowitz and Sartaj Sahni,
	Second Edition, p.339
	Computer Science Press, Inc.
	Rockville, Maryland
	ISBN 0-88175-165-0 */

public class QuickSort {

  Object[] objects;
  Compare comparator;

  /* -- */

  public QuickSort(Object[] objects, Compare comparator)
  {
    this.objects = objects;
    this.comparator = comparator;
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
	i = first; j = last+1; k = objects[first];
	do
	  {
	    do
	      {
		i++;
	      } while ((i <= last) && comparator.compare(objects[i], k) < 0);

	    do
	      {
		j--;
	      } while ((j >= first) && comparator.compare(objects[j], k) > 0);

	    if (i < j)
	      {
		tmp=objects[j];
		objects[j] = objects[i];
		objects[i] = tmp;
	      }
	  } while (j > i);

	tmp = objects[first];
	objects[first] = objects[j];
	objects[j] = tmp;
	quick(first, j-1);
	quick(j+1, last);
      }
  }

  public void sort()
  {
    if (objects.length < 2)
      {
	return;
      }
    
    quick(0, objects.length-1);
  }

}
