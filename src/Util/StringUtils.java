/*

   StringUtils.java

   Created: 24 March 2000
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2000/03/25 00:28:06 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
                                                                     StringUtils

------------------------------------------------------------------------------*/

/**
 * <P>This class contains a variety of utility String manipulating static 
 * methods for use in Ganymede.</P>
 */

public class StringUtils {

  /**
   * <P>This method strips out any characters from inputString that are
   * not present in legalChars.</P>
   *
   * <P>This method will always return a non-null String.</P>
   */

  public static String strip(String inputString, String legalChars)
  {
    if (inputString == null || legalChars == null)
      {
	return "";
      }

    StringBuffer buffer = new StringBuffer();

    for (int i = 0; i < inputString.length(); i++)
      {
	char c = inputString.charAt(i);

	if (legalChars.indexOf(c) != -1)
	  {
	    buffer.append(c);
	  }
      }

    return buffer.toString();
  }
}
