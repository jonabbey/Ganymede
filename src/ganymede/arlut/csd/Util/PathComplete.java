/*

   PathComplete.java

   A utility class to fix up a path string.
   
   Created: 12 March 1998


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

/*------------------------------------------------------------------------------
                                                                           class
                                                                    PathComplete

------------------------------------------------------------------------------*/

/**
 * A utility class to fix up a path string.
 */

public class PathComplete {

  static String separator = null;

  /**
   * Returns path with the system path separator concatenated to the
   * end if not already present.
   */

  public static String completePath(String path)
  {
    if (path == null)
      {
        return null;
      }

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
