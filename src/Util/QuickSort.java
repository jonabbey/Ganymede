/*

   QuickSort.java

   An implementation of the QuickSort algorithm.
   
   Created: 24 April 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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

  QuickSort(Object[] objects, Compare comparator)
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
	      } while (comparator.compare(objects[i], k) < 0);

	    do
	      {
		j--;
	      } while (comparator.compare(objects[j], k) > 0);

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

  void sort()
  {
    if (objects.length < 2)
      {
	return;
      }
    
    quick(0, objects.length-1);
  }

}
