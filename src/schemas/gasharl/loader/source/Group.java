/*

   Group.java

   Class to load and store the data from a line in the
   GASH group_info file
   
   Created: 29 August 1997
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/01/22 18:05:12 $
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

import arlut.csd.Util.Parser;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Group

------------------------------------------------------------------------------*/

public class Group {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('\n');
    tokens.ordinaryChar('\\');
  }

  /**
   *
   * Our (extra-super-simple) parser.
   *
   */

  Parser parser;

  // member fields

  String name;
  String password;
  int gid;
  Vector users;

  String contract = "";
  String description = "";

  boolean valid = false;

  // instance constructor

  public Group()
  {
    users = new Vector();
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    parser = new Parser(tokens);

    // eat the EOL if that's where we are

    if (parser.atEOF())
      {
	valid = false;
	return true;
      }

    name = parser.getNextBit();
    password = parser.getNextBit(); 

    try
      {
	gid = parser.getNextInt();
      }
    catch (NumberFormatException ex)
      {
	parser.skipToEndLine();
	valid = false;
	return parser.atEOF();
      }

    // skip the : before the user list

    if (parser.checkNextToken() != ':')
      {
	valid = false;
	return parser.atEOF();
      }
    else
      {
	// skip it

	tokens.nextToken();
      }

    // read in the users

    while (parser.checkNextToken() != ':' && !parser.EOFnext() && !parser.EOLnext())
      {
	users.addElement(parser.getNextBit());
      }

    if (parser.checkNextToken() == ':')
      {
	contract = parser.getNextBit();
      }

    if (parser.checkNextToken() == ':')
      {
	description = parser.getNextBit();
      }

    parser.skipToEndLine();

    valid = true;

    return parser.atEOF();
  }

  public void display()
  {
    System.out.println("Group: " + name + ", pass: " + password + ", gid: " + gid);

    System.out.print("\tUsers: ");

    for (int i = 0; i < users.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)users.elementAt(i));
      }

    System.out.println("\tContract: " + contract + ", Descrip: " + description);
  }

}
