/*

   MapEntry.java

   Class to load and store the data from a line in a
   GASH auto.home.* file
   
   Created: 4 December 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        MapEntry

------------------------------------------------------------------------------*/

public class MapEntry {

  static final boolean debug = true;

  // --

  String mapName;
  String userName;
  String volName;

  /* -- */

  public MapEntry(String mapName, String line) throws IOException
  {
    StringReader reader = new StringReader(line);
    StreamTokenizer tokens = new StreamTokenizer(reader);
    int token;
    String tmp;

    /* -- */

    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');

    // and handle the string

    this.mapName = mapName;

    userName = getNextBit(tokens);
    volName = getNextBit(tokens);
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;

    /* -- */

    token = tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	return tokens.sval;
      }

    return null;
  }

  public String toString()
  {
    return mapName + ":" + userName + "\t" + volName;
  }
}
