/*
   xmlobject.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2000/05/17 00:06:00 $
   Release: $Name:  $

   Module By: Jonathan Abbey

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.Util.*;
import org.xml.sax.*;

import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       xmlobject

------------------------------------------------------------------------------*/

/**
 * <p>This class is a data holding structure that is intended to hold
 * object and field data for an XML object element for
 * {@link arlut.csd.ganymede.client.xmlclient xmlclient}.</p>
 *
 * @version $Revision: 1.2 $ $Date: 2000/05/17 00:06:00 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlobject {

  /**
   * <p>The local identifier string for this object</p>
   */

  String id = null;

  /**
   * Descriptive typeString for this object
   */

  String typeString = null;

  /**
   * <p>The short object type id for this object type.</p>
   *
   * <p>Will be null if undefined.</p>
   */

  Short type = null;

  /**
   * <p>The server-side object identifier for this object.  Will
   * be null until we create or locate this object in the server.</p>
   */

  Invid invid = null;

  /**
   * <p>The object number, if known.  This may be used to identify
   * an object on the server if the object is not thought to have
   * a unique identifier string.</p>
   *
   * <p>Will be negative one if undefined.</p>
   */

  int num = -1;

  /**
   * <p>Vector of {@link arlut.csd.ganymede.client.xmlfield xmlfield}
   * objects.</p>
   */

  Vector fields = null;

  /* -- */

  /**
   * <p>This constructor takes the XMLElement defining an object to
   * be created or manipulated on the server and loads all information
   * for this object into the xmlobject created.</p>
   *
   * <p>This constructor reads all elements from the xmlclient
   * XML stream up to and including the matching close element for
   * this object.</p>
   */
  
  public xmlobject(XMLElement openElement) throws SAXException
  {
    // handle any attributes in the element

    type = new Short(xmlclient.xc.getTypeNum(openElement.getAttrStr("type")));

    id = openElement.getAttrStr("id"); // may be null

    Integer numInt = openElement.getAttrInt("num");

    if (numInt != null)
      {
	num = numInt.intValue();
      }

    // if we get an inactivate or delete request, our object element
    // might be empty, in which case, deal.

    if (openElement.isEmpty())
      {
	return;
      }

    // okay, we should contain some fields, then

    fields = new Vector();

    XMLItem nextItem = xmlclient.xc.getNextItem();

    while (!nextItem.matchesClose("object") && !(nextItem instanceof XMLEndDocument))
      {
	if (nextItem instanceof XMLElement)
	  {
	    // the xmlfield constructor will consume all elements up
	    // to and including the matching field close element

	    fields.addElement(new xmlfield(this, (XMLElement) nextItem));
	  }
	else
	  {
	    System.err.println("Unrecognized XML content in object " + 
			       openElement + ":" + nextItem);
	  }

	nextItem = xmlclient.xc.getNextItem();
      }
  }

  public short getType()
  {
    return type.shortValue();
  }

  /**
   * <p>This method returns a field definition for a named field.
   * The fieldName string is assumed to be underscore-for-space XML
   * encoded.</p>
   */

  public FieldTemplate getFieldDef(String fieldName)
  {
    return xmlclient.xc.loader.getFieldTemplate(type, XMLUtils.XMLDecode(fieldName));
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    result.append("<object type=\"");
    result.append(typeString);

    if (id != null)
      {
	result.append(" id=\"");
	result.append(id);
	result.append("\"");
      }

    if (num != -1)
      {
	result.append(" num=\"");
	result.append(num);
	result.append("\"");
      }

    result.append(">");

    return result.toString();
  }
}
