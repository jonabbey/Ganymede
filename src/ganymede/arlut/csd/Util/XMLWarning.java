/*
   GASH 2

   XMLWarning.java

   The Ganymede object storage system.

   Created: 9 March 2000


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.Util;

import org.xml.sax.SAXParseException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      XMLWarning

------------------------------------------------------------------------------*/

/**
 * <P>Warning class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

public class XMLWarning extends XMLItem {

  String error;
  int lineNumber;
  int columnNumber;

  /* -- */

  XMLWarning(SAXParseException exception, org.xml.sax.Locator locator)
  {
    error = exception.getMessage();
    lineNumber = locator.getLineNumber();
    columnNumber = locator.getColumnNumber();
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
    return "XML Warning: " + error + ":[" + lineNumber + "," + columnNumber + "]";
  }
}
