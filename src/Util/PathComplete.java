/*

   PathComplete.java

   A utility class to fix up a path string.
   
   Created: 12 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    PathComplete

------------------------------------------------------------------------------*/

/**
 * A utility class to fix up a path string.
 *
 */

public class PathComplete {

  static String separator = null;

  public static String completePath(String path)
  {
    if (separator == null)
      {
	separator = System.getProperty("file.separator");
      }

    if (!path.endsWith(separator))
      {
	return path + separator;
      }
    else
      {
	return path;
      }
  }
}
