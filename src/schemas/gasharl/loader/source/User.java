/*

   User.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 22 August 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;
import arlut.csd.Util.Parser;

/*------------------------------------------------------------------------------
                                                                           class
                                                                            User

------------------------------------------------------------------------------*/

public class User {

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
  int uid;
  int gid;
  String fullname;
  String room;
  String division;
  String officePhone;
  String homePhone;
  String directory;
  String shell;

  // ARL specific

  String socialsecurity;
  String category;
  String expiration;

  // instance constructor

  public User()
  {
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
    uid = parser.getNextInt();
    gid = parser.getNextInt();
    fullname = parser.getNextBit();

    /* now we need to get the next bit..  in the ARL user_info format,
       it's "Room Division", separated by a space.  */

    String tmp = parser.getNextBit();
    int index = tmp.indexOf(' ');

    if (index == -1)
      {
	// no space in the string.. maybe we got
	// a comma where we shouldn't have?

	room = parser.getNextBit();
      }
    else
      {
	room = tmp.substring(0, index); 
      }

    division = tmp.substring(index+1); 

    /* and some more : or , separated fields... */

    officePhone = parser.getNextBit();

    /* home phone is optional in the ARL user_info file.. */

    if (parser.checkNextToken() == ',')
      {
	homePhone = parser.getNextBit();
      }

    directory = parser.getNextBit();
    shell = parser.getNextBit();

    /* Ok, we've got all the mandatory felds.. check for the
      trailing 'optional' fields. */

    if (parser.checkNextToken() == ':')
      {
	socialsecurity = parser.getNextBit();
      }

    if (parser.checkNextToken() == ':')
      {
	category = parser.getNextBit();
      }

    parser.skipToEndLine();

    return parser.atEOF();
  }

  public void display()
  {
    System.out.println("User: " + name + ", pass: " + password + ", uid: " + uid + ", gid: " + gid);
    System.out.println("\tfullname: " + fullname + ", room: " + room + ", division: " + division +
		       ", officePhone: " + officePhone + ", homePhone: " + homePhone);
    System.out.println("Directory: " + directory + ", shell: " + shell);
    System.out.println("SS#: " + socialsecurity + ", category: " + category);
  }
}
