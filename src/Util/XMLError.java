/*
   GASH 2

   XMLError.java

   The GANYMEDE object storage system.

   Created: 9 March 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/03/10 02:02:05 $
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

import org.xml.sax.*;
import com.jclark.xml.sax.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        XMLError

------------------------------------------------------------------------------*/

/**
 * <P>Error class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

public class XMLError extends XMLItem {

  String error;
  int lineNumber;
  int columnNumber;
  boolean fatal;

  /* -- */

  XMLError(SAXParseException exception, org.xml.sax.Locator locator, boolean fatal)
  {
    error = exception.getMessage();
    lineNumber = locator.getLineNumber();
    columnNumber = locator.getColumnNumber();
    this.fatal = fatal;
  }

  public boolean isFatal()
  {
    return fatal;
  }

  public String getMessage()
  {
    return error;
  }

  public int getLineNumber()
  {
    return lineNumber;
  }

  public int getColumnNumber()
  {
    return columnNumber;
  }

  public String toString()
  {
    if (fatal)
      {
	return "XML Fatal Error: " + error + ":[" + lineNumber + "," + columnNumber + "]";
      }
    else
      {
	return "XML Error: " + error + ":[" + lineNumber + "," + columnNumber + "]";
      }
  }
}
