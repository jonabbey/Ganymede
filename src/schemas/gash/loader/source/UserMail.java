/*

   UserMail.java

   This module represents a class to store the information to be
   represented in an user's email ref base in the server.
   
   Created: 1 December 1997
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

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        UserMail

------------------------------------------------------------------------------*/

public class UserMail {

  static final boolean debug = false;

  // --

  String userName;
  Vector aliases = new Vector();
  Vector targets = new Vector();

  /* -- */

  public UserMail(String line) throws IOException
  {
    StringReader reader = new StringReader(line);
    StreamTokenizer tokens = new StreamTokenizer(reader);
    int token;
    String tmp;

    /* -- */

    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');

    // and handle the string

    userName = getNextBit(tokens);

    token = tokens.nextToken();

    // skip :

    token = tokens.nextToken();

    // get all the aliases

    while (tokens.ttype == ',' ||
	   tokens.ttype == StreamTokenizer.TT_WORD)
      {
	if (tokens.ttype == ',')
	  {
	    token = tokens.nextToken();
	    continue;
	  }

	aliases.addElement(tokens.sval);
	token = tokens.nextToken();
      }

    // ok, we should be at : -- get our target list

    while (true)
      {
	tmp = getNextBit(tokens);

	if (tmp != null)
	  {
	    targets.addElement(tmp);
	  }
	else
	  {
	    return;
	  }
      }
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;

    /* -- */

    token = tokens.nextToken();

    while (tokens.ttype == ':' || tokens.ttype == ',')
      {
	if (debug)
	  {
	    System.err.println("*");
	  }
	token = tokens.nextToken();
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	return tokens.sval;
      }

    return null;
  }

  public String toString()
  {
    String result;

    result = userName + ":";
    
    for (int i = 0; i < aliases.size(); i++)
      {
	if (i > 0)
	  {
	    result = result + ",";
	  }

	result = result + aliases.elementAt(i).toString();
      }

    result = result + ":";

    for (int i = 0; i < targets.size(); i++)
      {
	if (i > 0)
	  {
	    result = result + ",";
	  }

	result = result + targets.elementAt(i).toString();
      }

    return result;
  }
}

