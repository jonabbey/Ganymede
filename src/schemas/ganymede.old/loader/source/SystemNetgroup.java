/*

   SystemNetgroup.java

   Class to load and store the data from system netgroup lines in the
   GASH netgroup_ file
   
   Created: 17 October 1997
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 1999/01/22 18:04:39 $
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

	    if (!tmp.equals(""))
	      {
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
