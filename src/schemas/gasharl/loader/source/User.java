/*

   User.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 22 August 1997
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 1999/01/22 18:05:14 $
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
                                                                            User

------------------------------------------------------------------------------*/

public class User {

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
  int uid;
  int gid;
  String fullname;
  String room;
  String division;
  String officePhone;
  String homePhone;
  String directory;
  String shell;

  // ARL specific

  String socialsecurity;
  String category;
  Date expirationDate;

  boolean valid;

  // instance constructor

  public User()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    parser = new Parser(tokens);

    // eat the EOL if that's where we are

    if (parser.EOFnext())
      {
	valid = false;
	return true;
      }

    name = parser.getNextBit();

    //    System.out.println("User = " + name);

    password = parser.getNextBit();

    //    System.out.println("Password = " + password);

    try
      {
	uid = parser.getNextInt();
	gid = parser.getNextInt();
      }
    catch (NumberFormatException ex)
      {
	System.err.println("** Warning, bad uid or gid field for user " + name + ":" + ex);
	System.err.println("** Skipping user_info entry for user " + name + ":" + ex);

	parser.skipToEndLine();
	valid = false;
	return parser.atEOF();
      }

    fullname = parser.getNextBit();

    /* now we need to get the next bit..  in the ARL user_info format,
       it's "Room Division", separated by a space.  */

    String tmp = parser.getNextBit();
    int index = tmp.indexOf(' ');

    if (index == -1)
      {
	// no space in the string.. maybe we got
	// a comma where we shouldn't have?

	System.err.println("Warning, possible bad room/division field for user " + name);

	room = tmp;
	division = "";
      }
    else
      {
	room = tmp.substring(0, index); 
	division = tmp.substring(index+1); 
      }

    /* and some more : or , separated fields... */

    officePhone = parser.getNextBit();

    /* home phone is optional in the ARL user_info gcos file.. */

    if (parser.checkNextToken() == ',')
      {
	homePhone = parser.getNextBit();
      }

    directory = parser.getNextBit();
    shell = parser.getNextBit();

    /* Ok, we've got all the mandatory felds.. check for the
      trailing 'optional' fields. */

    if (parser.checkNextToken() == ':')
      {
	socialsecurity = parser.getNextBit();

	//	System.err.println("Social Security = " + socialsecurity);
      }

    if (parser.checkNextToken() == ':')
      {
	category = parser.getNextBit();

	//	System.err.println("Category = " + category);
      }

    if (parser.checkNextToken() == ':')
      {
	String expiration = parser.getNextBit();

	if (expiration != null && !(expiration.equals("") || expiration.equals("0")))
	  {
	    try
	      {
		long datecode = java.lang.Long.parseLong(expiration);

		expirationDate = new Date(datecode * 1000);
	      }
	    catch (NumberFormatException ex)
	      {
		System.err.println("User: couldn't parse expiration date.");
		expirationDate = null;
	      }
	  }
	else
	  {
	    expirationDate = null;
	  }

	//	System.err.println("Category = " + category);
      }

    parser.skipToEndLine();

    valid = true;

    return parser.atEOF();
  }

  public void display()
  {
    System.out.println("User: " + name + ", pass: " + password + ", uid: " + uid + ", gid: " + gid);
    System.out.println("\tfullname: " + fullname + ", room: " + room + ", division: " + division +
		       ", officePhone: " + officePhone + ", homePhone: " + homePhone);
    System.out.println("Directory: " + directory + ", shell: " + shell);
    System.out.println("SS#: " + socialsecurity + ", category: " + category);
  }
}
