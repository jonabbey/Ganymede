/*

   VecQuickSort.java

   A Vector implementation of the QuickSort algorithm.
   
   Created: 12 August 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
