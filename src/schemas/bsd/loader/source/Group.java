/*

   Group.java

   Class to load and store the data from a line in the
   BSD 4.4 group file
   
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

  // member fields

  String name;
  String password;
  int gid;
  Vector users;

  // instance constructor

  public Group()
  {
    users = new Vector();
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    int token;

    /* -- */

    // read groupname

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

    //    System.out.println("name = '" + name + "'");

    password = getNextBit(tokens); 

    // System.out.println("password = '" + password + "'");

    String gidString = getNextBit(tokens);
    gid = new Integer(gidString).intValue();

    // System.out.println("gid = '" + gid + "'");

    token = tokens.nextToken();

    if (tokens.ttype == ':')
      {
	token = tokens.nextToken();
      }
    else
      {
	System.err.println("Parse error after gid");
      }

    while (tokens.ttype != ':' && (tokens.ttype != StreamTokenizer.TT_EOL) &&
	    (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	if (tokens.ttype != StreamTokenizer.TT_WORD)
	  {
	    System.err.println("parse error in user list");
	  }
	else
	  {
	    //	    System.out.print(" " + tokens.sval);
	    users.addElement(tokens.sval);
	  }

	token = tokens.nextToken();

	if (tokens.ttype == ',')
	  {
	    token = tokens.nextToken();
	  }
      }

    if (tokens.ttype == StreamTokenizer.TT_EOL || tokens.ttype == StreamTokenizer.TT_EOF)
      {
	contract = "";
	description = "";
	
	return (tokens.ttype == StreamTokenizer.TT_EOF);
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
  
  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;

    token = tokens.nextToken();

    if ((tokens.ttype == StreamTokenizer.TT_EOF) ||
	(tokens.ttype == StreamTokenizer.TT_EOL))
      {
	return "";
      }

    while (tokens.ttype == ':' || tokens.ttype == ',')
      {
	//	System.err.println("*");
	token = tokens.nextToken();
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
