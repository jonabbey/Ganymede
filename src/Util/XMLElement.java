/*
   GASH 2

   XMLElement.java

   The GANYMEDE object storage system.

   Created: 9 March 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/03/10 02:02:04 $
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
                                                                      XMLElement

------------------------------------------------------------------------------*/

/**
 * <P>Element Start class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

public class XMLElement extends XMLItem {

  String name;
  AttributeList atts;

  /* -- */

  XMLElement(String name, AttributeList atts)
  {
    this.name = name;
    this.atts = atts;
  }

  public String getName()
  {
    return name;
  }

  public AttributeList getAttributeList()
  {
    return atts;
  }

  public String toString()
  {
    StringBuffer buffer = new StringBuffer();

    buffer.append("XML Open Element [");
    buffer.append(name);
    buffer.append("]");
    
    if (atts != null && atts.getLength() > 0)
      {
	buffer.append("<");

	for (int i = 0; i < atts.getLength(); i++)
	  {
	    if (i != 0)
	      {
		buffer.append(", ");
	      }

	    buffer.append(atts.getName(i));
	    buffer.append("= \"");
	    buffer.append(atts.getValue(i));
	    buffer.append("\"");
	  }
      }

    return buffer.toString();
  }
}
