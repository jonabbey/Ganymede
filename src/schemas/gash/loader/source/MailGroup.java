/*

   MailGroup.java

   This module represents a class to store the information to be
   represented in a Email List base in the server.
   
   Created: 1 December 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       MailGroup

------------------------------------------------------------------------------*/

public class MailGroup {

  static final boolean debug = true;

  // --

  String listName;
  String ownerCode;
  Vector targets = new Vector();

  /* -- */

  public MailGroup(String line) throws IOException
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
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');

    // and handle the string

    ownerCode = getNextBit(tokens);
    listName = getNextBit(tokens);

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

    return null;
  }

}

