/*

   SystemType.java

   This file is used to load and represent a GASH System Type record
   for Ganymede.
   
   Created: 24 October 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SystemType

------------------------------------------------------------------------------*/

public class SystemType {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    //    tokens.ordinaryChar(',');
    //    tokens.ordinaryChar('\n');
  }

  // member fields

  String name;
  int start;
  int end;
  boolean requireUser;

  // instance constructor

  public SystemType()
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

    name = tokens.sval;

    tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("parse error 1 in SystemType.loadLine()");
      }
    
    tokens.nextToken();

    start = (int) tokens.nval;

    tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("parse error 2 in SystemType.loadLine()");
      }
    
    tokens.nextToken();

    end = (int) tokens.nval;

    tokens.nextToken();

    if (tokens.ttype != StreamTokenizer.TT_EOL &&
	tokens.ttype != StreamTokenizer.TT_EOF)
      {
	requireUser = true;
      }

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println(name + ":" + start + ":" + end + (requireUser ? "" : ":username"));
  }
}
