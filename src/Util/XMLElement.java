/*
   GASH 2

   XMLElement.java

   The GANYMEDE object storage system.

   Created: 9 March 2000
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2000/03/10 03:15:55 $
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

import java.util.*;

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
  Hashtable attributes;

  /* -- */

  XMLElement(String name, AttributeList atts)
  {
    int length;
    String label;
    String value;

    /* -- */

    this.name = name;

    if (atts != null)
      {
	length = atts.getLength();

	attributes = new Hashtable(length + 1, (float) 1.0);

	for (int i = 0; i < atts.getLength(); i++)
	  {
	    label = atts.getName(i);
	    value = atts.getValue(i);

	    attributes.put(label, value);
	  }
      }
  }

  /**
   * <P>This method returns the name of this element.</P>
   */

  public String getName()
  {
    return name;
  }

  /**
   * <P>This method returns true if this element is named
   * &lt;name&gt;</P>
   */

  public boolean matches(String name)
  {
    return name != null && name.equals(this.name);
  }

  /**
   * <P>This method returns the number of attributes that this
   * element has.</P>
   */

  public int getAttributeCount()
  {
    if (attributes == null)
      {
	return 0;
      }
    else
      {
	return attributes.size();
      }
  }

  /**
   * <P>This method returns the attribute value for attribute
   * &lt;name&gt;, if any.  If this element does not contain
   * an attribute of the given name, null is returned.</P>
   */

  public String getAttribute(String name)
  {
    if (attributes == null)
      {
	return null;
      }

    return (String) attributes.get(name);
  }

  public String toString()
  {
    Object key, value;
    int i = 0;
    StringBuffer buffer = new StringBuffer();

    /* -- */

    buffer.append("XML Open Element <");
    buffer.append(name);
    
    if (attributes != null)
      {
	Enumeration keys = attributes.keys();

	while (keys.hasMoreElements())
	  {
	    buffer.append(" ");

	    key = keys.nextElement();
	    value = attributes.get(key);

	    buffer.append(key);
	    buffer.append("= \"");
	    buffer.append(value);
	    buffer.append("\"");

	    i++;
	  }

	buffer.append(">");
      }

    return buffer.toString();
  }
}
