/*

   PathComplete.java

   A utility class to fix up a path string.
   
   Created: 12 March 1998
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:04 $
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
