/*

   Result.java

   The result class is used to return labeled invid's for
   database queries.

   Result is serializable.
   
   Created: 21 October 1996 
   Release: $Name:  $
   Version: $Revision: 1.8 $
   Last Mod Date: $Date: 1999/01/22 18:05:53 $
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

package arlut.csd.ganymede;

import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Result

------------------------------------------------------------------------------*/

/**
 * The result class is used to return labeled invid's for
 * database queries.
 */

public class Result implements java.io.Serializable {

  static final long serialVersionUID = -8417751229367613063L;

  // ---
  
  Invid invid;	// remote reference to an object on the server
  String label = null;

  /* -- */

  public Result(Invid invid, String label)
  {
    this.invid = invid;
    this.label = label;
  }

  public String toString()
  {
    return label;
  }

  public Invid getInvid()
  {
    return invid;
  }

  public String resultDump()
  {
    StringBuffer buffer = new StringBuffer();
    char[] chars;

    /* -- */

    chars = invid.toString().toCharArray();
    
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append("|");

    chars = label.toCharArray();
    
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append("\n");

    return buffer.toString();
  }

  // and hashCode

  public int hashCode()
  {
    return invid.hashCode();
  }
}
