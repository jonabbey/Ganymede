/*

   Admin.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 29 September 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Admin

------------------------------------------------------------------------------*/

public class Admin {

  static final boolean debug = false;

  // -- 

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');
  }

  // member fields

  String name;
  String password;
  int lowuid;
  int highuid;
  int lowgid;
  int highgid;
  String mask;

  // instance constructor

  public Admin()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    // read username

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
	if (debug)
	  {
	    System.out.println("Early EOF!");
	  }
	return true;
      }
    else
      {
	if (debug)
	  {
	    System.out.println("pushback:" + tokens.sval);
	  }
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

    getNextBit(tokens);		// skip admin code

    getNextBit(tokens);		// skip bitfield

    String uidString = getNextBit(tokens);
    lowuid = new Integer(uidString).intValue();

    uidString = getNextBit(tokens);
    highuid = new Integer(uidString).intValue();

    if (debug)
      {
	System.out.println("uid range = " + lowuid + "-" + highuid);
      }

    String gidString = getNextBit(tokens);
    lowgid = new Integer(gidString).intValue();

    gidString = getNextBit(tokens);
    highgid = new Integer(gidString).intValue();

    if (debug)
      {
	System.out.println("gid range = " + lowgid + "-" + highgid);
      }

    mask = getNextBit(tokens);	// assume all the masks are the same

    if (debug)
      {
	System.out.println("mask = '" + mask + "'");
      }

    // get to the end of line.. skip the email info at the end

    // System.err.println("HEY! Token = " + token + ", ttype = " + tokens.ttype);

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	// System.err.print(".");
	tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println("Admin: " + name + ", pass: " + password + ", mask = " + mask + ", lowuid: " + lowuid + ", highuid: " + highuid);
    System.out.println("\tlowgid: " + lowgid + ", highgid: " + highgid);
  }
  
  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;

    token = tokens.nextToken();

    while (tokens.ttype == ':' || tokens.ttype == ',')
      {
	if (debug)
	  {
	    System.err.println("*");
	  }
	token = tokens.nextToken();
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	if (debug)
	  {
	    System.err.println("returning native word");
	  }
	return tokens.sval;
      }

    if (tokens.ttype == StreamTokenizer.TT_NUMBER)
      {
	if (debug)
	  {
	    System.err.println("returning converted word");
	  }
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
