/*

   Room.java

   Class to load and store the data from a line in the
   GASH networks_by_room.cpp file
   
   Created: 15 May 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                            Room

------------------------------------------------------------------------------*/

public class Room {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.commentChar('#'); 
    tokens.slashSlashComments(true);
    tokens.whitespaceChars(':', ':');
    tokens.whitespaceChars('[', '[');
    tokens.whitespaceChars(']', ']');
    tokens.whitespaceChars('|', '|');
    tokens.whitespaceChars(',', ',');
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');
  }

  // member fields

  /**
   * name of room
   */

  String name;

  /**
   * vector of Strings specifying the IP subnets in this room
   */

  Vector nets = new Vector();

  /**
   * true if the Room was loaded successfully.
   */

  boolean loaded = false;

  // instance constructor

  public Room()
  {
  }

  /**
   *
   * This method loads this room definition from the StreamTokenizer passed in.
   * <br><br>
   *
   * @return true if loadLine has read the last line from the StreamTokenizer
   */

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    int token;
    boolean ipOK;

    /* -- */

    // test for EOF

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
	return true;
      }
    else
      {
	tokens.pushBack();
      }

    // read room name

    name = getNextBit(tokens);

    // get to the end of line

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	token = tokens.nextToken();

	ipOK = true;

	if (tokens.ttype == StreamTokenizer.TT_WORD)
	  {
	    String net = tokens.sval;

	    char[] cAry = net.toCharArray();

	    for (int i = 0; i < cAry.length; i++)
	      {
		if (cAry[i] != '.' && !Character.isDigit(cAry[i]))
		  {
		    ipOK = false;
		  }
	      }

	    if (ipOK)
	      {
		loaded = true;
		nets.addElement(net);
	      }
	  }
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println("\nRoom: " + name);

    System.out.print("Nets: ");

    for (int i = 0; i < nets.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)nets.elementAt(i));
      }

    System.out.println();
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;

    /* -- */

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
