/*
   GASH 2

   XMLUtils.java

   The GANYMEDE object storage system.

   Created: 21 February 2000
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2000/03/07 08:15:19 $
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
}
