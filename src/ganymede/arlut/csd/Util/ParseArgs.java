/*

   ParseArgs.java

   A simple utility to parse command line arguments in a java program.
   
   Created: 5 March 1998


   Module By: Mike Mulvaney

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
                                                                       ParseArgs

------------------------------------------------------------------------------*/

public class ParseArgs {

  private ParseArgs() {}

  /**
   * Check for to see if an argument exists.
   *
   * All command line switches must start with a dash. (-)
   *
   * @param argument The string to look for.  If the string doesn't start with a dash(-), one will be prepended.
   * @param args The command line arguments passed to tbe main method.
   */

  public static boolean switchExists(String argument, String[] args)
  {
    if (!argument.startsWith("-"))
      {
        argument = "-" + argument;
      }
    
    for (int i = 0; i < args.length; i++)
      {
        if (args[i].toLowerCase().equals(argument.toLowerCase()))
          {
            return true;
          }
      }
        
    return false;
  }

  /**
   * Get the value of an argument
   *
   * Arguments should use this form: argument=value.
   *
   * @param argument The part of the argument before the equals sign.
   * @param args The comand line arguments passed to the main method.
   * @return The String after the equals sign(the value).
   */

  public static String getArg(String argument, String[] args)
  {
    if (argument.indexOf('=') != -1)
      {
        throw new IllegalArgumentException("Error, = character passed into getArg(" + argument + ")");
      }

    for (int i = 0; i < args.length; i++)
      {
        if (args[i].toLowerCase().startsWith(argument.toLowerCase()))
          {
            int index = args[i].indexOf("=");

            if (index > 0)
              {
                return decodeArg(args[i].substring(index + 1));
              }
            else
              {
                return null;
              }
          }
      }

    return null;
  }

  /**
   * Simple decode method to handle unescaping %20 for space
   * substitution, as used with the xmlclient, and etc.
   */

  public static String decodeArg(String argument)
  {
    return argument.replaceAll("%20", " ");
  }
}
