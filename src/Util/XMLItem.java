/*
   GASH 2

   XMLITem.java

   The GANYMEDE object storage system.

   Created: 9 March 2000
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2000/03/15 03:36:51 $
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

import java.util.Enumeration;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         XMLItem

------------------------------------------------------------------------------*/

/**
 * <P>Abstract base class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 *
 * <P>This class implements stubs for most of the methods
 * in its subclass {@link arlut.csd.Util.XMLElement XMLElement}, so that
 * the Ganymede server code can process elements without having to constantly
 * perform cast operations.  The matches() method returns false unless this
 * XMLItem is actually an XMLElement with the appropriate label,
 * and the matchesClose() method returns false unless this XMLItem is
 * actually an {@link arlut.csd.Util.XMLCloseElement XMLCloseElement} with
 * the appropriate label.</P>
 */

public abstract class XMLItem {

  /**
   * <P>This method returns true if this item is an
   * {@link arlut.csd.Util.XMLElement XMLElement} named
   * &lt;name&gt;, false otherwise.</P>
   */

  public boolean matches(String name)
  {
    return false;
  }

  /**
   * <P>This method returns true if this item is an
   * {@link arlut.csd.Util.XMLCloseElement XMLCloseElement} named
   * &lt;name&gt;, false otherwise.</P>
   */

  public boolean matchesClose(String name)
  {
    return false;
  }

  /**
   * <P>This method returns true if this is an empty element.</P>
   */

  public boolean isEmpty()
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns true if this is an open element.</P>
   */

  public boolean isOpen()
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns the number of attributes that this
   * element has.</P>
   */

  public int getAttrCount()
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns an enumeration of attribute names
   * present in this element.  getAttrStr() can be used on these
   * keys in order to get the raw string values associated with
   * the attribute names.</P>
   */

  public Enumeration getAttrKeysEnum()
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns the attribute value for attribute
   * &lt;name&gt;, if any.  If this element does not contain
   * an attribute of the given name, null is returned.</P>
   */

  public String getAttrStr(String name)
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns the boolean attribute value for attribute
   * &lt;name&gt;, if any.  For Ganymede's purposes, an attribute
   * value is true if the attribute is present with a string
   * value of "1".  If this element does not contain
   * an attribute of the given name, false is returned.</P>
   */

  public boolean getAttrBoolean(String name)
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns the Integer attribute value for attribute
   * &lt;name&gt;, if any.  If this element does not contain
   * an attribute of the given name, or if the attribute does not
   * contain an integer value, a null value is returned.</P>
   */

  public Integer getAttrInt(String name)
  {
    throw new IllegalArgumentException("not an XMLElement.");
  }

  /**
   * <P>This method returns the character data for this XMLItem.</P>
   */

  public String getString()
  {
    throw new IllegalArgumentException("not an XMLCharData.");
  }

  /**
   * <P>This method returns the character data for this XMLItem with
   * leading and trailing whitespace filtered out.</P>
   */

  public String getCleanString()
  {
    throw new IllegalArgumentException("not an XMLCharData.");
  }
}
