/*

   User.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 22 August 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2000/04/19 17:00:45 $
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
  }

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

  StreamTokenizer tokens;

  // instance constructor

  public User()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    this.tokens = tokens;

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

    name = getNextBit(tokens);

    //    System.out.println("name = '" + name + "'");

    password = getNextBit(tokens); 

    // System.out.println("password = '" + password + "'");

    String uidString = getNextBit(tokens);
    uid = new Integer(uidString).intValue();

    // System.out.println("uid = '" + uid + "'");

    String gidString = getNextBit(tokens);
    gid = new Integer(gidString).intValue();

    // System.out.println("gid = '" + gid + "'");

    /*
     * we expect to see gcos fields that look like:
     *
     * Jonathan Abbey,S321 CSD,3199,8343915
     *
     */

    fullname = getNextBit(tokens); // until next comma

    // System.out.println("fullname = '" + fullname + "'");

    String tmp = getNextBit(tokens);
    int index = tmp.indexOf(' ');

    if (index == -1)
      {
	// no space in the string.. maybe we got
	// a comma where we shouldn't have?

	room = getNextBit(tokens);
      }
    else
      {
	room = tmp.substring(0, index); 
      }

    // System.out.println("room = '" + room + "'");
    division = tmp.substring(index+1); 

    // System.out.println("division = '" + division + "'");

    officePhone = getNextBit(tokens);

    // System.out.println("officePhone = '" + officePhone + "'");

    int token = tokens.nextToken();

    // should be a comma

    if (token != ',')
      {
	System.err.println("No comma after office phone..");
      }
    else
      {
	token = tokens.nextToken();
      }

    if (token == ':')
      {
	// no home phone

	homePhone = "";
      }
    else
      {
	// System.err.println("XXX");
	tokens.pushBack();
	homePhone = getNextBit(tokens);

	// System.out.println("homePhone = '" + homePhone + "'");
      }

    directory = getNextBit(tokens);

    // System.out.println("directory = '" + directory + "'");

    shell = getNextBit(tokens);

    // System.out.println("shell = '" + shell + "'");

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
    System.out.println("User: " + name + ", pass: " + password + ", uid: " + uid + ", gid: " + gid);
    System.out.println("\tfullname: " + fullname + ", room: " + room + ", division: " + division +
		       ", officePhone: " + officePhone + ", homePhone: " + homePhone);
    System.out.println("Directory: " + directory + ", shell: " + shell);
  }

  /**
   *
   * Returns true if we're at EOL
   *
   */

  private boolean atEOL()
  {
    return tokens.ttype == StreamTokenizer.TT_EOL;
  }

  /**
   *
   * Returns true if we're at EOF
   *
   */

  private boolean atEOF()
  {
    return tokens.ttype == StreamTokenizer.TT_EOF;
  }

  /**
   *
   * This method runs tokens to the end of the line.
   *
   */

  private void skipToEndLine() throws IOException
  {
    while (!atEOL() && !atEOF())
      {
	tokens.nextToken();
      }
  }
  
  /**
   *
   * getNextBit() returns the next String from the StreamTokenizer,
   * where the bits are separated by colons and commas.
   *
   */
  
  private String getNextBit(StreamTokenizer tokens) throws IOException
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
  
  private String getNextBit(StreamTokenizer tokens, boolean skipleading) throws IOException
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
	    token = checkNextToken(tokens);

	    if (token != ':' && token != ',')
	      {
		token = tokens.nextToken();
	      }
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

  private int checkNextToken(StreamTokenizer tokens) throws IOException
  {
    tokens.nextToken();
    int result = tokens.ttype;
    tokens.pushBack();
    return result;
  }
}
