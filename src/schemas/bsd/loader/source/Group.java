/*

   Group.java

   Class to load and store the data from a line in the
   BSD 4.4 group file
   
   Created: 29 August 1997
   Version: $Revision: 1.3 $ %D%
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

  static final boolean debug = true;

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('\n');
  }

  // member fields

  String name;
  String password;
  int gid;
  Vector users;

  boolean valid;

  StreamTokenizer tokens;

  // instance constructor

  public Group()
  {
    users = new Vector();
  }

  /**
   *
   * Returns true if we're at EOF.
   *
   */

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    int token;

    /* -- */

    this.tokens = tokens;

    // read groupname

    tokens.nextToken();

    if (atEOF())
      {
	valid = false;
	return true;
      }
    else
      {
	tokens.pushBack();
      }

    name = getNextBit(tokens);

    if (debug)
      {
	System.out.println("name = '" + name + "'");
      }

    password = getNextBit(tokens); 

    if (debug)
      {
	System.out.println("password = '" + password + "'");
      }

    String gidString = getNextBit(tokens);

    if (gidString != null && !gidString.equals(""))
      {
	gid = new Integer(gidString).intValue();
      }
    else
      {
	System.err.println("Error, group " + name + " has no gid.. skipping line.");

	skipToEndLine();
	valid = false;
	return atEOF();
      }

    if (debug)
      {
	System.out.println("gid = '" + gid + "'");
      }

    // skip :

    token = tokens.nextToken();

    if (tokens.ttype == ':')
      {
	token = tokens.nextToken();
      }
    else
      {
	System.err.println("Parse error after gid");
      }

    // read the user list

    while (tokens.ttype != ':' && (tokens.ttype != StreamTokenizer.TT_EOL) &&
	    (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	if (tokens.ttype != StreamTokenizer.TT_WORD)
	  {
	    System.err.println("parse error in user list");
	  }
	else
	  {
	    if (debug)
	      {
	        System.out.print(" " + tokens.sval);
	      }

	    users.addElement(tokens.sval);
	  }

	token = tokens.nextToken();

	if (tokens.ttype == ',')
	  {
	    token = tokens.nextToken();
	  }
      }

    skipToEndLine();
    valid = true;

    return atEOF();
  }

  /**
   *
   * Debug routine.
   *
   */

  public void display()
  {
    System.out.println("Group: " + name + ", pass: " + password + ", gid: " + gid);

    System.out.print("Users: ");

    for (int i = 0; i < users.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)users.elementAt(i));
      }
  }

  /**
   *
   * Returns true if we're at EOL
   *
   */

  private boolean atEOL()
  {
    return tokens.ttype == StreamTokenizer.TT_EOL;
  }

  /**
   *
   * Returns true if we're at EOF
   *
   */

  private boolean atEOF()
  {
    return tokens.ttype == StreamTokenizer.TT_EOF
;
  }

  /**
   *
   * This method runs tokens to the end of the line.
   *
   */

  private void skipToEndLine() throws IOException
  {
    while (!atEOL() && !atEOF())
      {
	tokens.nextToken();
      }
  }

  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer,
   * where the bits are separated by colons and commas.
   *
   */
  
  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    return getNextBit(tokens, true);
  }

  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer,
   * where the bits are separated by colons and commas.
   *
   * @param skipleading if true, getNextBit will chew through leading
   * commas and colons until it gets to either a normal string or
   * eol/eof.
   *
   */

  private String getNextBit(StreamTokenizer tokens, boolean skipleading) throws IOException
  {
    int token;
    String result;

    token = tokens.nextToken();

    if (atEOF() || atEOL())
      {
	return "";
      }

    // eat any leading :'s or ,'s

    if (!skipleading)
      {
	// skip only the single leading token

	if (tokens.ttype == ':' || tokens.ttype == ',')
	  {
	    token = tokens.nextToken();
	  }
      }
    else
      {
	// skip any leading colons and commas

	while (tokens.ttype == ':' || tokens.ttype == ',')
	  {
	    //	System.err.println("*");
	    token = tokens.nextToken();
	  }
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	//	System.err.println("returning native word");
	return tokens.sval;
      }

    if (tokens.ttype == StreamTokenizer.TT_NUMBER)
      {
	// System.err.println("returning converted word");
	result = Integer.toString(new Double(tokens.nval).intValue());

	while (tokens.ttype != ':' && tokens.ttype != ',' && tokens.ttype != StreamTokenizer.TT_EOF &&
	       tokens.ttype != StreamTokenizer.TT_EOL)
	  {
	    token = tokens.nextToken();
	    
	    if (tokens.ttype == StreamTokenizer.TT_WORD)
	      {
		result += tokens.sval;
	      }
	  }

	return result;
      }


    return null;
  }

}
