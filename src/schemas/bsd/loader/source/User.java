/*

   User.java

   Class to load and store the data from a line in the
   BSD master.passwd file
   
   Created: 22 August 1997
   Version: $Revision: 1.2 $ %D%
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

  // instance constructor

  public User()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    // read username

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
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
    uid = new Integer(uidString).intValue();

    if (debug)
      {
	System.out.println("uid = '" + uid + "'");
      }

    String gidString = getNextBit(tokens);
    gid = new Integer(gidString).intValue();

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

    room = getNextBit(tokens, false);

    if (debug)
      {
	System.out.println("room = '" + room + "'");
      }

    officePhone = getNextBit(tokens, false);

    if (debug)
      {
	System.out.println("officePhone = '" + officePhone + "'");
      }
    
    homePhone = getNextBit(tokens, false);

    if (debug)
      {
	System.out.println("homePhone = '" + homePhone + "'");
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

    // get to the end of line

    // System.err.println("HEY! Token = " + token + ", ttype = " + tokens.ttype);

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	// System.err.print(".");
	token = tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println("User: " + name + ", pass: " + password + ", uid: " + uid + ", gid: " + gid);
    System.out.println("\tfullname: " + fullname + ", room: " + room + ", division: " + division +
		       ", officePhone: " + officePhone + ", homePhone: " + homePhone);
    System.out.println("Directory: " + directory + ", shell: " + shell);
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

  private int checkNextToken(StreamTokenizer tokens)
  {
    tokens.nextToken();
    int result = tokens.ttype;
    tokens.pushBack();
    return result;
  }
}
