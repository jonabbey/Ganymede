/*

   RandomUtils.java

   Created: 7 February 2008

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2008
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.Util;

import java.util.Random;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     RandomUtils

------------------------------------------------------------------------------*/

/**
 * <P>This class contains a variety of utility static methods that
 * deal in Random numbers for use in Ganymede.</P>
 */

public class RandomUtils {

  private static Random randomizer = new Random();

  /**
   * This method returns a random hex string, used to ensure that a
   * string including it is random.
   */

  public static String getRandomHex()
  {
    return Integer.toHexString(randomizer.nextInt());
  }

  /**
   * This method takes a string, adds a random salt to it, and returns
   * it.
   */

  public static String getSaltedString(String in)
  {
    return in + " " + getRandomHex();
  }
}
