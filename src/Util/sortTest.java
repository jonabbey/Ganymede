/*

   sortTest.java

   Test rig for arlut.csd.Util sort routines
   
   Created: 24 April 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

public class sortTest implements Compare {

  static Integer ints[];

  public static void main(String[] argv)
  {
    ints = new Integer[argv.length];

    for (int i = 0; i < argv.length; i++)
      {
	ints[i] = new Integer(argv[i]);
      }
    
    QuickSort qs = new QuickSort(ints, new sortTest());
    qs.sort();

    for (int i = 0; i < argv.length; i++)
      {
	System.out.print(ints[i]);
	if (i+1 < argv.length)
	  {
	    System.out.print(" ");
	  }
	else
	  {
	    System.out.println();
	  }
      }
  }

  public sortTest()
  {
  }

  public int compare(Object a, Object b)
  {
    int i, j;

    i = ((Integer) a).intValue();
    j = ((Integer) b).intValue();

    if (i < j)
      {
	return -1;
      }
    else if (i > j)
      {
	return 1;
      }
    else
      {
	return 0;
      }
  }

}
