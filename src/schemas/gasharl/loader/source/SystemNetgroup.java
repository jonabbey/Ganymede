/*

   SystemNetgroup.java

   Class to load and store the data from system netgroup lines in the
   GASH netgroup_ file
   
   Created: 17 October 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  SystemNetgroup

------------------------------------------------------------------------------*/

public class SystemNetgroup {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');
  }

  // member fields

  String netgroup_name;
  Vector systems;
  Vector subnetgroups;

  // instance constructor

  public SystemNetgroup()
  {
    systems = new Vector();
    subnetgroups = new Vector();
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    int token;

    /* -- */

    // read netgroup_name

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
	return true;
      }
    else
      {
	tokens.pushBack();
      }

    netgroup_name = getNextBit(tokens);

    // if the netgroup name ends in -u, we've got 
    // a user netgroup and we want to skip it

    while (netgroup_name.endsWith("-u"))
      {
	while ((tokens.ttype != StreamTokenizer.TT_EOL) &&
	       (tokens.ttype != StreamTokenizer.TT_EOF))
	  {
	    token = tokens.nextToken();
	  }

	if (tokens.ttype == StreamTokenizer.TT_EOF)
	  {
	    return false;
	  }

	netgroup_name = getNextBit(tokens);
      }

    // okay, we're in a line that has a system netgroup. figure
    // out what it contains

    while ((tokens.ttype != StreamTokenizer.TT_EOL) &&
	   (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	if (tokens.ttype != StreamTokenizer.TT_WORD)
	  {
	    System.err.println("parse error in system list");
	  }
	else
	  {
	    //	    System.out.print(" " + tokens.sval);

	    String tmp = getNextBit(tokens);

	    if (tmp.indexOf('(') == -1)
	      {
		// absence of parens mean this is a sub netgroup reference

		subnetgroups.addElement(tmp);
	      }
	    else
	      {
		// we've got a system entry

		String tmp2 = tmp.substring(1, tmp.indexOf(','));
		systems.addElement(tmp2);
	      }
	  }
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
    System.out.println("SystemNetgroup: " + netgroup_name);
    System.out.print("\tSystems: ");

    for (int i = 0; i < systems.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)systems.elementAt(i));
      }

    System.out.println();

    System.out.print("\tNetgroups: ");

    for (int i = 0; i < subnetgroups.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)subnetgroups.elementAt(i));
      }
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

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	//	System.err.println("returning native word");
	return tokens.sval;
      }

    return null;
  }

}
