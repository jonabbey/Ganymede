/*

   Admin.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 29 September 1997
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/03/03 00:33:45 $
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
                                                                           Admin

------------------------------------------------------------------------------*/

public class Admin {

  static final boolean debug = false;

  // -- 

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');
  }

  // member fields

  String name;
  String password;
  int lowuid;
  int highuid;
  int lowgid;
  int highgid;
  String code;
  String mask;
  String address;
  String approval_address;

  // instance constructor

  public Admin()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    // read username

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
	if (debug)
	  {
	    System.out.println("Early EOF!");
	  }
	return true;
      }
    else
      {
	if (debug)
	  {
	    System.out.println("pushback:" + tokens.sval);
	  }
	tokens.pushBack();
      }

    name = getNextBit(tokens);

    if (debug)
      {
	System.out.println("name = '" + name + "'");
      }

    password = getNextBit(tokens); 

    if (debug)
      {
	System.out.println("password = '" + password + "'");
      }

    code = getNextBit(tokens);

    if (debug)
      {
	System.out.println("admin code = '" + code + "'");
      }

    getNextBit(tokens);		// skip bitfield

    String uidString = getNextBit(tokens);
    lowuid = new Integer(uidString).intValue();

    uidString = getNextBit(tokens);
    highuid = new Integer(uidString).intValue();

    if (debug)
      {
	System.out.println("uid range = " + lowuid + "-" + highuid);
      }

    String gidString = getNextBit(tokens);
    lowgid = new Integer(gidString).intValue();

    gidString = getNextBit(tokens);
    highgid = new Integer(gidString).intValue();

    if (debug)
      {
	System.out.println("gid range = " + lowgid + "-" + highgid);
      }

    mask = getNextBit(tokens);	// assume all the masks are the same

    if (debug)
      {
	System.out.println("mask = '" + mask + "'");
      }

    // skip second mask

    getNextBit(tokens);

    // skip third mask

    getNextBit(tokens);

    // skip fourth mask

    getNextBit(tokens);

    // skip fifth mask

    getNextBit(tokens);

    // get the email address for this admin

    address = getNextBit(tokens);

    if (address != null)
      {
	System.out.println("Hey, found address: " + address);

	approval_address = getNextBit(tokens);
      }

    if (approval_address != null)
      {
	System.out.println("Hey, found address: " + approval_address);
      }

    // shouldn't be anything left, but just in case..

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println("Admin: " + name + ", pass: " + password + ", mask = " + mask +
		       "\n\tlowuid: " + lowuid + ", highuid: " + highuid);
    System.out.println("\tlowgid: " + lowgid + ", highgid: " + highgid);
    System.out.println("\taddress: " + address + ", approval_address: " + approval_address);
  }
  
  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;

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
	if (debug)
	  {
	    System.err.println("returning native word");
	  }
	return tokens.sval;
      }

    if (tokens.ttype == StreamTokenizer.TT_NUMBER)
      {
	if (debug)
	  {
	    System.err.println("returning converted word");
	  }
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
