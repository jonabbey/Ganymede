/*

   ParseArgs.java

   A simple utility to parse command line arguments in a java program.
   
   Created: 5 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

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
	if (args[i].equals(argument))
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

    for (int i = 0; i < args.length; i++)
      {
	if (args[i].startsWith(argument))
	  {
	    int index = args[i].indexOf("=");
	    if (index > 0)
	      {
		return args[i].substring(index + 1);
	      }
	    else
	      {
		return null;
	      }
	  }
      }

    return null;
   
  }

}
