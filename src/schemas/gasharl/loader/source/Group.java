/*

   Group.java

   Class to load and store the data from a line in the
   GASH group_info file
   
   Created: 29 August 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Group

------------------------------------------------------------------------------*/

public class Group {

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
  String password;
  int gid;
  Vector users;

  String contract = "";
  String description = "";

  // instance constructor

  public Group()
  {
    users = new Vector();
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    parser = new Parser(tokens);

    if (parser.EOFnext())
      {
	return true;
      }

    name = parser.getNextBit();
    password = parser.getNextBit(); 
    gid = parser.getNextInt();

    // skip the : before the user list

    tokens.nextToken();

    while (parser.checkNextToken() != ':' && !parser.atEOF() && !parser.atEOL())
      {
	users.addElement(parser.getNextBit());
      }

    if (parser.checkNextToken() == ':')
      {
	contract = parser.getNextBit();
      }

    if (parser.checkNextToken() == ':')
      {
	description = parser.getNextBit();
      }

    parser.skipToEndLine();

    return parser.atEOF();
  }

  public void display()
  {
    System.out.println("Group: " + name + ", pass: " + password + ", gid: " + gid);

    System.out.print("\tUsers: ");

    for (int i = 0; i < users.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)users.elementAt(i));
      }

    System.out.println("\tContract: " + contract + ", Descrip: " + description);
  }

}
