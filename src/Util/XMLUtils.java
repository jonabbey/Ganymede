/*
   GASH 2

   XMLUtils.java

   The GANYMEDE object storage system.

   Created: 21 February 2000
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2000/11/02 02:20:10 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.Util;

import java.io.*;
import com.jclark.xml.output.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        XMLUtils

------------------------------------------------------------------------------*/

/**
 * <P>This class contains various methods that assist the Ganymede server
 * in reading or writing XML files.</P>
 */

public class XMLUtils {

  /**
   * <P>This is a helper method emitting a newline and proper
   * indention into an XMLWriter stream.</P>
   */

  public static void indent(XMLWriter xmlOut, int indentLevel) throws IOException
  {
    xmlOut.write("\n");

    for (int i = 0; i < indentLevel; i++)
      {
	xmlOut.write("  ");
      }
  }

  /**
   * <P>This is a helper method which converts any spaces in a string
   * to a legal underscore.  Any underscores passed in will be converted
   * to a double underscore.</P>
   */

  public static String XMLEncode(String name)
  {
    StringBuffer buffer = new StringBuffer(name.length());

    for (int i = 0; i < name.length(); i++)
      {
	char c = name.charAt(i);

	if (c == '_')
	  {
	    buffer.append("__");
	  }
	else if (c == ' ')
	  {
	    buffer.append("_");
	  }
	else
	  {
	    buffer.append(c);
	  }
      }

    return buffer.toString();
  }

  /**
   * <P>This is a helper method which undoes the effects of the
   * {@link arlut.csd.Util.XMLUtils#XMLEncode(java.lang.String) XMLEncode} method,
   * which is used to encode all element names in the Ganymede server.  Any
   * pair of subsequent underscores will be replaced in the returned String
   * with a single underscore.  Any single underscore will be replaced with
   * a space.</P>
   */

  public static String XMLDecode(String name)
  {
    if (name == null)
      {
	return null;
      }

    StringBuffer buffer = new StringBuffer(name.length());

    int i = 0;

    while (i < name.length())
      {
	char c = name.charAt(i);

	if (c != '_')
	  {
	    buffer.append(c);
	  }
	else
	  {
	    if ((i + 1) < name.length())
	      {
		if (name.charAt(i+1) == '_')
		  {
		    buffer.append("_");
		    i++;
		  }
		else
		  {
		    buffer.append(" ");
		  }
	      }
	  }

	i++;	
      }

    return buffer.toString();
  }
}
