/*

   SystemType.java

   This file is used to load and represent a GASH System Type record
   for Ganymede.
   
   Created: 24 October 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SystemType

------------------------------------------------------------------------------*/

public class SystemType {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    //    tokens.parseNumbers();
    tokens.ordinaryChar(':');
    tokens.ordinaryChar('\n');
    tokens.eolIsSignificant(true);
  }

  // member fields

  String name;
  int start;
  int end;
  boolean requireUser = false;

  // instance constructor

  public SystemType()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    // read username

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
	return true;
      }

    name = tokens.sval;

    tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("parse error 1 in SystemType.loadLine(): " + tokens.ttype);
	System.err.println("token val = " + tokens.sval);
      }
    
    tokens.nextToken();

    try
      {
	start = (int) Integer.parseInt(tokens.sval);
      }
    catch (NumberFormatException ex)
      {
	System.err.println("Crap! " + tokens.sval);
      }

    tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("parse error 2 in SystemType.loadLine(): " + tokens.ttype);
	System.err.println("token val = " + tokens.sval);
      }
    
    tokens.nextToken();

    try
      {
	end = (int) Integer.parseInt(tokens.sval);
      }
    catch (NumberFormatException ex)
      {
	System.err.println("Crap++! " + tokens.sval);
      }

    tokens.nextToken();		// either : or EOL

    if ((tokens.ttype != StreamTokenizer.TT_EOL) &&
	(tokens.ttype != StreamTokenizer.TT_EOF))
      {
	//System.err.println("Requiring user: next Token was " + tokens.ttype);
	//System.err.println("token val = " + tokens.sval);
	requireUser = true;
      }
    else
      {
	//	System.err.println("Early return: " + (tokens.ttype == StreamTokenizer.TT_EOF));
	return (tokens.ttype == StreamTokenizer.TT_EOF);
      }

    // skip to the end of the line

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println(name + ":" + start + ":" + end + (requireUser ? ":username" : ""));
  }
}
