/*

   ExternMail.java

   This module represents a class to store the information to be
   represented in an external email ref base in the server.
   
   Created: 1 December 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      ExternMail

------------------------------------------------------------------------------*/

public class ExternMail {

  static final boolean debug = false;

  // --

  String externalName;
  String ownerCode;
  Vector aliases = new Vector();
  Vector targets = new Vector();

  /* -- */

  public ExternMail(String line) throws IOException
  {
    StringReader reader = new StringReader(line);
    StreamTokenizer tokens = new StreamTokenizer(reader);
    int token;
    String tmp;

    /* -- */

    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('<');
    tokens.ordinaryChar('>');
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');

    // and handle the string

    ownerCode = getNextBit(tokens);
    externalName = getNextBit(tokens);

    token = tokens.nextToken();

    // skip :

    token = tokens.nextToken();

    // get all the aliases

    while (tokens.ttype == ',' ||
	   tokens.ttype == StreamTokenizer.TT_WORD)
      {
	if (tokens.ttype == ',')
	  {
	    token = tokens.nextToken();
	    continue;
	  }

	aliases.addElement(tokens.sval);
	token = tokens.nextToken();
      }

    // ok, we should be at : -- get our target list

    while (true)
      {
	tmp = getNextBit(tokens);

	if (tmp != null)
	  {
	    targets.addElement(tmp);
	  }
	else
	  {
	    return;
	  }
      }
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;

    /* -- */

    token = tokens.nextToken();

    while (tokens.ttype == ':' || tokens.ttype == ',' ||
	   tokens.ttype == '>' || tokens.ttype == '<')
      {
	if (debug)
	  {
	    System.err.println("*");
	  }
	token = tokens.nextToken();
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	return tokens.sval;
      }

    return null;
  }

  public String toString()
  {
    String result;

    result = "<" + ownerCode + ">" + externalName + ":";
    
    for (int i = 0; i < aliases.size(); i++)
      {
	if (i > 0)
	  {
	    result = result + ",";
	  }

	result = result + aliases.elementAt(i).toString();
      }

    result = result + ":";

    for (int i = 0; i < targets.size(); i++)
      {
	if (i > 0)
	  {
	    result = result + ",";
	  }

	result = result + targets.elementAt(i).toString();
      }

    return result;
  }
}

