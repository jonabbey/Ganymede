/*

   Room.java

   Class to load and store the data from a line in the
   GASH networks_by_room.cpp file
   
   Created: 15 May 1998
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/22 18:04:57 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
