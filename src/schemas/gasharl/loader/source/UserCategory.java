/*

   UserCategory.java

   Class to load and store the data from a line in the
   GASH user_categories file
   
   Created: 7 August 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;
import arlut.csd.Util.Parser;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    UserCategory

------------------------------------------------------------------------------*/

public class UserCategory {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('\n');
  }

  /**
   *
   * Our (extra-super-simple) parser.
   *
   */

  Parser parser;

  // member fields

  String name;
  int days_limit;
  boolean ssrequired;
  String maillist;
  String shortdescrip;
  String longdescrip;

  // instance constructor

  public UserCategory()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    parser = new Parser(tokens);

    if (parser.EOFnext())
      {
	valid = false;
	return true;
      }

    name = parser.getNextBit();

    try
      {
	days_limit = parser.getNextInt();
      }
    catch (NumberFormatException ex)
      {
	System.err.println("** Warning, bad expiration limit for category " + name);
	System.err.println("** Skipping category " + name);

	parser.skipToEndLine();
	valid = false;
	return parser.atEOF();
      }

    String flag = parser.getNextBit();

    if (flag.equals("y"))
      {
	ssrequired = true;
      }

    maillist = parser.getNextLongBit();

    shortdescrip = parser.getNextLongBit();

    longdescrip = parser.getNextLongBit();

    parser.skipToEndLine();

    valid = true;

    return parser.atEOF();
  }

  public void display()
  {
    System.out.println("UserCategory: " + name + ", limit: " + days_limit);
    
    if (ssrequired)
      {
	System.out.println("Social Security # required");
      }

    System.out.println("maillist = " + maillist);
    System.out.println("short = " + shortdescrip);
    System.out.println("long = " + longdescrip);
  }
}
