/*

   UserCategory.java

   Class to load and store the data from a line in the
   GASH user_categories file
   
   Created: 7 August 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;
import arlut.csd.Util.Parser;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    UserCategory

------------------------------------------------------------------------------*/

public class UserCategory {

  static final boolean debug = false;

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('\n');
    tokens.ordinaryChar('\\');
  }

  // ---

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
  Vector mailvec;
  String shortdescrip;
  String longdescrip;

  boolean valid;

  /* -- */

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

    if (debug)
      {
	System.err.println("Creating category " + name);
      }

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

    String maillist = parser.getNextLongBit();

    if (debug)
      {
	System.err.println(maillist);
      }

    mailvec = arlut.csd.Util.VectorUtils.stringVector(maillist, ", ");

    shortdescrip = parser.getNextLongBit();

    if (debug)
      {
	System.err.println(shortdescrip);
      }

    longdescrip = parser.getNextLongBit();

    if (debug)
      {
	System.err.println(longdescrip);
      }

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

    System.out.print("mailvec = ");

    for (int i = 0; i < mailvec.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print(mailvec.elementAt(i));
      }
    System.out.println();

    System.out.println("short = " + shortdescrip);
    System.out.println("long = " + longdescrip);
  }
}
