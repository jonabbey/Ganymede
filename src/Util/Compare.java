/*

   Compare.java

   Comparison interface for arlut.csd.Util sort classes.
   
   Created: 24 April 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

public interface Compare {

  /**
   *
   * Comparator for arlut.csd.Util sort classes.  compare returns
   * -1 if a < b, 0 if a = b, and 1 if a > b in sort order.
   *
   */

  public int compare(Object a, Object b);
}
