/*

   User.java

   Class to load and store the data from a line in the
   BSD master.passwd file
   
   Created: 22 August 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                            User

------------------------------------------------------------------------------*/

public class User {

  static final boolean debug = false;

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('\n');
  }

  // ---

  // member fields

  String name;
  String password;
  int uid;
  int gid;
  String fullname;
  String room;
  String officePhone;
  String homePhone;
  String directory;
  String shell;
  String classification;
  int lastchange;
  int expire;

  boolean valid;
  StreamTokenizer tokens;

  // instance constructor

  public User()
  {
  }

  /**
   *
   * Returns true if we're at EOF.
   *
   */

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    this.tokens = tokens;

    // read username

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

    String uidString = getNextBit(tokens);

    if (uidString != null && !uidString.equals(""))
      {
	uid = new Integer(uidString).intValue();
      }
    else
      {
	System.err.println("Error, user " + name + " has no uid.. skipping line.");

	skipToEndLine();
	valid = false;
	return atEOF();
      }

    if (debug)
      {
	System.out.println("uid = '" + uid + "'");
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

    classification = getNextBit(tokens, false);

    if (debug)
      {
	System.out.println("classification = '" + classification + "'");
      }

    String changeString = getNextBit(tokens);
    lastchange = new Integer(changeString).intValue();

    if (debug)
      {
	System.out.println("lastchange = '" + lastchange + "'");
      }
    
    String expireString = getNextBit(tokens);
    expire = new Integer(expireString).intValue();

    if (debug)
      {
	System.out.println("expire = '" + expire + "'");
      }

    fullname = getNextBit(tokens);

    if (debug)
      {
	System.out.println("fullname = '" + fullname + "'");
      }

    if (checkNextToken(tokens) == ',')
      {
	room = getNextBit(tokens, false);
	
	if (debug)
	  {
	    System.out.println("room = '" + room + "'");
	  }
      }

    if (checkNextToken(tokens) == ',')
      {
	officePhone = getNextBit(tokens, false);
	
	if (debug)
	  {
	    System.out.println("officePhone = '" + officePhone + "'");
	  }
      }

    if (checkNextToken(tokens) == ',')
      {
	homePhone = getNextBit(tokens, false);
	
	if (debug)
	  {
	    System.out.println("homePhone = '" + homePhone + "'");
	  }
      }

    directory = getNextBit(tokens);

    if (debug)
      {
	System.out.println("directory = '" + directory + "'");
      }

    shell = getNextBit(tokens);

    if (debug)
      {
	System.out.println("shell = '" + shell + "'");
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
    System.out.println("User: " + name + ", pass: " + password + ", uid: " + uid + ", gid: " + gid);
    System.out.println("\tfullname: " + fullname + ", room: " + room + 
		       ", officePhone: " + officePhone + ", homePhone: " + homePhone);
    System.out.println("Directory: " + directory + ", shell: " + shell);
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
    return tokens.ttype == StreamTokenizer.TT_EOF;
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
	    token = checkNextToken(tokens);

	    if (token != ':' && token != ',')
	      {
		token = tokens.nextToken();
	      }
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

  private int checkNextToken(StreamTokenizer tokens) throws IOException
  {
    tokens.nextToken();
    int result = tokens.ttype;
    tokens.pushBack();
    return result;
  }
}
