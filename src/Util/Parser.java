/*

   Parser.java

   This class provides some extra utility methods to apply to a
   StreamTokenizer to make parsing the GASH files easier.
   
   Created: 6 August 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Utils;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Parser

------------------------------------------------------------------------------*/

public class Parser {

  StreamTokenizer tokens;

  /* -- */

  public Parser (StreamTokenizer tokens)
  {
    this.tokens = tokens;
  }

  /**
   *
   * This method connects this Parser object to a different
   * StreamTokenizer.
   *
   */

  public void setStream(StreamTokenizer tokens)
  {
    this.tokens = tokens;
  }

  /**
   *
   * Returns true if we're at EOL
   *
   */

  public boolean atEOL()
  {
    return tokens.ttype == StreamTokenizer.TT_EOL;
  }

  /**
   *
   * Returns true if we're at EOF
   *
   */

  public boolean atEOF()
  {
    return tokens.ttype == StreamTokenizer.TT_EOF;
  }

  /**
   *
   * Returns true if the next thing to be read is EOL
   *
   */

  public boolean EOLnext() throws IOException
  {
    return checkNextToken() == StreamTokenizer.TT_EOL;
  }

  /**
   *
   * Returns true if the next thing to be read is EOF
   *
   */

  public boolean EOFnext() throws IOException
  {
    return checkNextToken() == StreamTokenizer.TT_EOF;
  }

  /**
   *
   * This method runs tokens to the end of the line.
   *
   */

  public void skipToEndLine() throws IOException
  {
    while (!atEOL() && !atEOF())
      {
	tokens.nextToken();
      }
  }

  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer
   * that this Parser was initialized with.
   *
   */

  public int getNextInt() throws IOException
  {
    String nextBit = getNextBit();

    return new Integer(nextBit).intValue();
  }

  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer
   * that this Parser was initialized with, skipping a single leading
   * :'s and ,'s along the way
   *
   */
  
  public String getNextBit() throws IOException
  {
    return getNextBit(tokens, false);
  }

  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer
   * that this Parser was initialized with.
   *
   * @param skipleading if true, getNextBit will chew through leading
   * commas and colons until it gets to either a normal string or
   * eol/eof.
   *
   */
  
  public String getNextBit(boolean skipleading) throws IOException
  {
    return getNextBit(tokens, skipleading);
  }
  
  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer,
   * where the bits are separated by colons and commas.
   *
   * @param tokens The StreamTokenizer to read from
   *
   */
  
  public String getNextBit(StreamTokenizer tokens) throws IOException
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
  
  public String getNextBit(StreamTokenizer tokens, boolean skipleading) throws IOException
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

  /**
   *
   * This method returns a peek at the next token in the
   * StreamTokenizer that this Parser was initialized with.
   *
   */

  public int checkNextToken() throws IOException
  {
    return checkNextToken(tokens);
  }

  /**
   *
   * This method returns a peek at the next token in the
   * StreamTokenizer passed in.
   *
   */

  public int checkNextToken(StreamTokenizer tokens) throws IOException
  {
    tokens.nextToken();
    int result = tokens.ttype;
    tokens.pushBack();
    return result;
  }
}
