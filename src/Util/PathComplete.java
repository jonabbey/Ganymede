/*

   PathComplete.java

   A utility class to fix up a path string.
   
   Created: 12 March 1998
   Version: $Revision: 1.2 $ %D%
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

	if (separator == null)
	  {
	    System.err.println("ERROR! PathComplete couldn't read file.separator system property.");
	    System.err.println("PathComplete defaulting to UNIX separator");
	    separator = "/";
	  }
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
