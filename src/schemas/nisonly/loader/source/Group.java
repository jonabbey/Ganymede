/*

   Group.java

   Class to load and store the data from a line in the
   GASH group_info file
   
   Created: 29 August 1997
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/22 18:05:23 $
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
  }

  // member fields

  String name;
  String password;
  int gid;
  Vector users;
  String contract;
  String description;

  // instance constructor

  public Group()
  {
    users = new Vector();
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    int token;

    /* -- */

    // read groupname

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

    String gidString = getNextBit(tokens);
    gid = new Integer(gidString).intValue();

    // System.out.println("gid = '" + gid + "'");

    token = tokens.nextToken();

    if (tokens.ttype == ':')
      {
	token = tokens.nextToken();
      }
    else
      {
	System.err.println("Parse error after gid");
      }

    while (tokens.ttype != ':' && (tokens.ttype != StreamTokenizer.TT_EOL) &&
	    (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	if (tokens.ttype != StreamTokenizer.TT_WORD)
	  {
	    System.err.println("parse error in user list");
	  }
	else
	  {
	    //	    System.out.print(" " + tokens.sval);
	    users.addElement(tokens.sval);
	  }

	token = tokens.nextToken();

	if (tokens.ttype == ',')
	  {
	    token = tokens.nextToken();
	  }
      }

    if (tokens.ttype == StreamTokenizer.TT_EOL || tokens.ttype == StreamTokenizer.TT_EOF)
      {
	contract = "";
	description = "";
	
	return (tokens.ttype == StreamTokenizer.TT_EOF);
      }

    contract = getNextBit(tokens);
    description = getNextBit(tokens);

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

    while (tokens.ttype == ':' || tokens.ttype == ',')
      {
	//	System.err.println("*");
	token = tokens.nextToken();
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
