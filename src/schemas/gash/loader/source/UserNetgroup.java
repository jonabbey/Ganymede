/*

   UserNetgroup.java

   Class to load and store the data from user netgroup lines in the
   GASH netgroup_ file
   
   Created: 17 October 1997
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:04:59 $
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
                                                                    UserNetgroup

------------------------------------------------------------------------------*/

public class UserNetgroup {

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
  Vector users;
  Vector subnetgroups;

  // instance constructor

  public UserNetgroup()
  {
    users = new Vector();
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

    //    System.err.println("Netgroup Name: " + netgroup_name);

    // if the netgroup name ends in -s, we've got 
    // a system netgroup and we want to skip it

    while (netgroup_name.endsWith("-s"))
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

	//	System.err.println("Netgroup Name: " + netgroup_name);
      }

    // okay, we're in a line that has a user netgroup. figure
    // out what it contains

    while ((tokens.ttype != StreamTokenizer.TT_EOL) &&
	   (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	if (tokens.ttype != StreamTokenizer.TT_WORD)
	  {
	    System.err.println("parse error in user list");
	  }
	else
	  {
	    //	    System.out.print(" " + tokens.sval);

	    String tmp = getNextBit(tokens);
	    
	    if (tmp.equals(""))
	      {
		continue;
	      }

	    if (tmp.indexOf('(') == -1)
	      {
		// absence of parens mean this is a sub netgroup reference

		//		System.err.println("Sub ref: " + tmp);

		subnetgroups.addElement(tmp);
	      }
	    else
	      {
		// we've got a user entry

		String tmp2 = tmp.substring(tmp.indexOf(',') + 1, tmp.lastIndexOf(','));

		// System.err.println("User: " + tmp2);
		users.addElement(tmp2);
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
    System.out.println("UserNetgroup: " + netgroup_name);
    System.out.print("\tUsers: ");

    for (int i = 0; i < users.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print((String)users.elementAt(i));
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

    System.out.println();
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
