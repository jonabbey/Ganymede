/*

   Parser.java

   This class provides some extra utility methods to apply to a
   StreamTokenizer to make parsing the GASH files easier.

   The Parser code assumes that the tokenizer has been set up to
   treat ':', ',', and '\n' as distinct tokens, with everything else
   treated as word chars.
   
   Created: 6 August 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Parser

------------------------------------------------------------------------------*/

/**
 * This class provides some extra utility methods to apply to a
 * StreamTokenizer to make parsing the GASH files easier.<br><br>
 *
 * The Parser code assumes that the tokenizer has been set up to
 * treat ':', ',', and '\n' as distinct tokens, with everything else
 * treated as word chars.
 */

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

  public int getNextInt() throws IOException, NumberFormatException
  {
    String nextBit = getNextBit();

    try
      {
	return new Integer(nextBit).intValue();
      }
    catch (NumberFormatException ex)
      {
	System.err.println("Parser.getNextInt(): couldn't turn '" + nextBit + "' to an Integer");
	throw ex;
      }
  }

  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer
   * that this Parser was initialized with, skipping a single leading
   * ':'s and ','s along the way, up to the next ',' or ':'.
   *
   */
  
  public String getNextBit() throws IOException
  {
    return getNextBit(tokens, false, false);
  }

  /**
   *
   * getNextLongBit() returns the next String from the StreamTokenizer
   * that this Parser was initialized with, skipping a single leading
   * ':'s along the way, up to the next ':'.
   *
   */
  
  public String getNextLongBit() throws IOException
  {
    StringBuffer buffer = new StringBuffer();

    /* -- */

    if (checkNextToken() == ':')
      {
	tokens.nextToken();
      }

    while ((checkNextToken() != ':') && !EOLnext() && !EOFnext())
      {
	if (checkNextToken() == ',')
	  {
	    tokens.nextToken();
	    buffer.append(",");
	  }

	// handle back-slashed colons

	if (checkNextToken() == '\\')
	  {
	    tokens.nextToken();

	    if (checkNextToken() == ':')
	      {
		tokens.nextToken();
		buffer.append(":");
	      }
	  }

	buffer.append(getNextBit(tokens, false, true));
      }

    return buffer.toString();
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
    return getNextBit(tokens, skipleading, false);
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
    return getNextBit(tokens, true, false);
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
   * @param includeCommas if true, getNextBit will not treat commas as
   * field separators
   *
   */
  
  public String getNextBit(StreamTokenizer tokens, boolean skipleading, boolean includeCommas) throws IOException
  {
    int token;
    String result;

    /* -- */

    token = tokens.nextToken();

    if (atEOL())
      {
	token = tokens.nextToken();
      }

    if (atEOF())
      {
	return "";
      }

    if (includeCommas)
      {
	// make , just part of a word for parsing purposes

	tokens.wordChars(',', ',');
      }

    try
      {
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
		token = tokens.nextToken();
	      }
	  }

	if (tokens.ttype == StreamTokenizer.TT_WORD)
	  {
	    return tokens.sval;
	  }

	if (tokens.ttype == StreamTokenizer.TT_NUMBER)
	  {
	    result = Integer.toString(new Double(tokens.nval).intValue());

	    return result;
	  }

	return null;
      }
    finally
      {
	// turn , back into a separate token

	if (includeCommas)
	  {
	    tokens.ordinaryChar(',');
	  }
      }
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
