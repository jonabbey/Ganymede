/*

   RandomUtils.java

   Created: 7 February 2008,
   Last Modified: 15 October 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
              James Ratcliff, falazar@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.Util;

import java.util.Random;
import java.security.SecureRandom;

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
  private static SecureRandom secureRandomizer = new SecureRandom();
  
  private static final String charsetA = "abcdefghijklmnopqrstuvwxyz";
  private static final String charsetAN = "0123456789abcdefghijklmnopqrstuvwxyz";
  private static final String charsetC = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  /**
   * <p>This method returns a random 32 bit hex string, used to ensure
   * that a string including it is random.</p>
   *
   * <p>The random number generator for this method is low security, and
   * should not be used for cryptographic purposes where
   * unpredictability is paramount.</p>
   */

  public static String getRandomHex()
  {
    return Integer.toHexString(randomizer.nextInt());
  }

  /**
   * <p>This method takes a string, adds a random salt to it, and returns
   * it.</p>
   *
   * <p>The random number generator for this method is low security, and
   * should not be used for cryptographic purposes where
   * unpredictability is paramount.</p>
   *
   * <p>Generally, salts need not be unpredictable, as long as they are
   * not often repeated.</p>
   */

  public static String getSaltedString(String in)
  {
    return in + " " + getRandomHex();
  }

  /**
   * Get a random string, using all characters from given charset,
   * length chars long
   */

  public static String getRandomString(int length, String charset)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < length; i++)
      {
        int pos = secureRandomizer.nextInt(charset.length());
        sb.append(charset.charAt(pos));
      }

    return sb.toString();
  }

  /**
   * Get a random username, 8 characters alpha-numeric, starting with
   * an alphabetic character.
   */

  public static String getRandomUsername()
  {
    return getRandomString(1, charsetA) + getRandomString(7, charsetAN);
  }

  /**
   * Get a random password, using all characters, length chars long
   */

  public static String getRandomPassword(int length)
  {
    return getRandomString(length, charsetC);
  }

  /**
   * Test rig
   */

  public static void main(String args[])
  {
    for (int i = 0; i < 10; i++)
      {
        String username = getRandomUsername();
        String password = getRandomPassword(20);

        System.out.println(username);
        System.out.println(password);
        System.out.println("");

        try
          {
            // if you generate more than 1 time, you must
            // put the process to sleep for awhile
            // otherwise it will return the same random string
            Thread.sleep(100);
          }
        catch (InterruptedException e)
          {
            e.printStackTrace();
          }
      }
  }
}
