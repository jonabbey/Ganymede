/*

   Volume.java

   Class to load and store the data from a line in the
   GASH auto.vol file
   
   Created: 4 December 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:40 $
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
                                                                          Volume

------------------------------------------------------------------------------*/

public class Volume {

  static final boolean debug = true;

  // --

  String volumeName;
  String mountOptions;
  String hostName;
  String path;

  /* -- */

  public Volume(String line)
  {
    int startMHindex;
    int startPath;
    int startHost;
    int i;

    char[] chars;

    /* -- */

    chars = line.toCharArray();

    i = 0;

    while (i < chars.length && !isWhiteSpace(chars[i]))
      {
	i++;
      }

    if (i == chars.length)
      {
	throw new RuntimeException("parse error 1: premature end of line");
      }

    volumeName = line.substring(0, i);

    while (i < chars.length && isWhiteSpace(chars[i]))
      {
	i++;
      }

    if (i == chars.length)
      {
	throw new RuntimeException("parse error 2: premature end of line");
      }

    startMHindex = i;		// start of either the mount options or the host name

    i = chars.length - 1;	// start at end

    while (i > startMHindex && chars[i] != ':')
      {
	i--;
      }

    if (i == startMHindex)
      {
	throw new RuntimeException("parse error 3: null path component");
      }

    startPath = i+1;		// start of path component, goes to end

    path = line.substring(startPath, chars.length);

    while (i >= startMHindex && !isWhiteSpace(chars[i]))
      {
	i--;
      }

    startHost = i+1;		// start of host component

    if (startHost == startPath)
      {
	throw new RuntimeException("parse error 4: null host component");
      }

    hostName = line.substring(startHost, startPath-1);

    if (startHost > startMHindex)
      {
	mountOptions = line.substring(startMHindex, startHost -1);
      }
    else
      {
	mountOptions = "";
      }
  }

  private final boolean isWhiteSpace(char c)
  {
    return (c == ' ') || (c == '\t') || (c == '\n');
  }

  public String toString()
  {
    return volumeName + "\t" + mountOptions + " " + hostName + ":" + path;
  }
}
