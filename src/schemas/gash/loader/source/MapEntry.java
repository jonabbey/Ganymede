/*

   MapEntry.java

   Class to load and store the data from a line in a
   GASH auto.home.* file
   
   Created: 4 December 1997
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/22 18:04:56 $
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
                                                                        MapEntry

------------------------------------------------------------------------------*/

public class MapEntry {

  static final boolean debug = true;

  // --

  String mapName;
  String userName;
  String volName;

  /* -- */

  public MapEntry(String mapName, String line) throws IOException
  {
    StringReader reader = new StringReader(line);
    StreamTokenizer tokens = new StreamTokenizer(reader);
    int token;
    String tmp;

    /* -- */

    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.whitespaceChars(' ', ' ');
    tokens.whitespaceChars('\t', '\t');
    tokens.ordinaryChar('\n');

    // and handle the string

    this.mapName = mapName;

    userName = getNextBit(tokens);
    volName = getNextBit(tokens);
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;

    /* -- */

    token = tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	return tokens.sval;
      }

    return null;
  }

  public String toString()
  {
    return mapName + ":" + userName + "\t" + volName;
  }
}
