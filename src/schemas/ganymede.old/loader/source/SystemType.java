/*

   SystemType.java

   This file is used to load and represent a GASH System Type record
   for Ganymede.
   
   Created: 24 October 1997
   Version: $Revision: 1.2 $ %D%
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
    //    tokens.parseNumbers();
    tokens.ordinaryChar(':');
    tokens.ordinaryChar('\n');
    tokens.eolIsSignificant(true);
  }

  // member fields

  String name;
  int start;
  int end;
  boolean requireUser = false;

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

    name = tokens.sval;

    tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("parse error 1 in SystemType.loadLine(): " + tokens.ttype);
	System.err.println("token val = " + tokens.sval);
      }
    
    tokens.nextToken();

    try
      {
	start = (int) Integer.parseInt(tokens.sval);
      }
    catch (NumberFormatException ex)
      {
	System.err.println("Crap! " + tokens.sval);
      }

    tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("parse error 2 in SystemType.loadLine(): " + tokens.ttype);
	System.err.println("token val = " + tokens.sval);
      }
    
    tokens.nextToken();

    try
      {
	end = (int) Integer.parseInt(tokens.sval);
      }
    catch (NumberFormatException ex)
      {
	System.err.println("Crap++! " + tokens.sval);
      }

    tokens.nextToken();		// either : or EOL

    if ((tokens.ttype != StreamTokenizer.TT_EOL) &&
	(tokens.ttype != StreamTokenizer.TT_EOF))
      {
	//System.err.println("Requiring user: next Token was " + tokens.ttype);
	//System.err.println("token val = " + tokens.sval);
	requireUser = true;
      }
    else
      {
	//	System.err.println("Early return: " + (tokens.ttype == StreamTokenizer.TT_EOF));
	return (tokens.ttype == StreamTokenizer.TT_EOF);
      }

    // skip to the end of the line

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println(name + ":" + start + ":" + end + (requireUser ? ":username" : ""));
  }
}
