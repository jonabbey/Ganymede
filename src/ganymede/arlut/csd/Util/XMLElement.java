/*
   GASH 2

   XMLElement.java

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

import org.xml.sax.Attributes;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      XMLElement

------------------------------------------------------------------------------*/

/**
 * <P>Element Start class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

public class XMLElement extends XMLItem {

  public XMLItem[] children;
  String name;
  String attrKeys[];
  String attrVals[];
  boolean empty;

  /* -- */

  XMLElement(String name, Attributes atts)
  {
    int length;

    /* -- */

    this.name = name;

    if (atts != null)
      {
        length = atts.getLength();

        attrKeys = new String[length];
        attrVals = new String[length];

        for (int i = 0; i < atts.getLength(); i++)
          {
            attrKeys[i] = atts.getLocalName(i);
            attrVals[i] = atts.getValue(i);
          }
      }
  }

  /**
   * <P>This method returns an array of children under this item,
   * or null if there are none.</P>
   */

  public XMLItem[] getChildren()
  {
    return children;
  }

  /**
   * <P>This method sets an array of XMLItem references to be
   * this XMLItem's children.</P>
   */

  public void setChildren(XMLItem[] children)
  {
    this.children = children;
  }


  /**
   * <P>This method unlinks this XMLItem and any subnodes of
   * it from each other, as well as clearing this XMLItem's
   * parent reference.  After this is called, children
   * and parent will both be null-valued.</P>
   */

  public void dissolve()
  {
    parent = null;

    if (children != null)
      {
        for (int i = 0; i < children.length; i++)
          {
            children[i].dissolve();
            children[i] = null;
          }
      }

    children = null;
  }

  /**
   * <P>This method is called by {@link arlut.csd.Util.XMLReader XMLReader}
   * if the open element tag for this element is immediately matched by its
   * close element tag.</P>
   */

  void setEmpty()
  {
    this.empty = true;
  }

  /**
   * <P>This method returns true if this is an empty element.</P>
   */

  public boolean isEmpty()
  {
    return empty;
  }

  /**
   * <P>This method returns true if this is an open element.</P>
   */

  public boolean isOpen()
  {
    return !empty;
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

  public int getAttrCount()
  {
    if (attrKeys == null)
      {
        return 0;
      }
    else
      {
        return attrKeys.length;
      }
  }

  /**
   * <p>This method returns the name for a given attribute
   * in this XMLItem.</p>
   */

  public String getAttrKey(int index)
  {
    return attrKeys[index];
  }

  /**
   * <p>This method returns the value for a given attribute
   * in this XMLItem.</p>
   */

  public String getAttrVal(int index)
  {
    return attrVals[index];
  }

  /**
   * <P>This method returns the attribute value for attribute
   * &lt;name&gt;, if any.  If this element does not contain
   * an attribute of the given name, null is returned.</P>
   */

  public String getAttrStr(String name)
  {
    if (attrKeys == null)
      {
        return null;
      }

    for (int i = 0; i < attrKeys.length; i++)
      {
        if (attrKeys[i].equals(name))
          {
            return attrVals[i];
          }
      }

    return null;
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
    if (attrKeys == null)
      {
        return false;
      }

    for (int i = 0; i < attrKeys.length; i++)
      {
        if (attrKeys[i].equals(name))
          {
            return (attrVals[i].equals("1") || 
                    attrVals[i].equalsIgnoreCase("true") || 
                    attrVals[i].equalsIgnoreCase("t") ||
                    attrVals[i].equalsIgnoreCase("yes") ||
                    attrVals[i].equalsIgnoreCase("y"));
          }
      }

    return false;
  }

  /**
   * <P>This method returns the Integer attribute value for attribute
   * &lt;name&gt;, if any.  If this element does not contain
   * an attribute of the given name, or if the attribute does not
   * contain an integer value, a null value is returned.</P>
   */

  public Integer getAttrInt(String name)
  {
    if (attrKeys == null)
      {
        return null;
      }

    for (int i = 0; i < attrKeys.length; i++)
      {
        if (attrKeys[i].equals(name))
          {
            try
              {
                return Integer.valueOf(attrVals[i]);
              }
            catch (NumberFormatException ex)
              {
                return null;
              }
          }
      }

    return null;
  }

  public String toString()
  {
    StringBuilder buffer = new StringBuilder();

    /* -- */

    buffer.append("XML Open Element <");
    buffer.append(name);
    
    if (attrKeys != null)
      {
        for (int i = 0; i < attrKeys.length; i++)
          {
            buffer.append(" ");

            buffer.append(attrKeys[i]);
            buffer.append("=\"");
            buffer.append(attrVals[i]);
            buffer.append("\"");
          }

        if (empty)
          {
            buffer.append("/");
          }

        buffer.append(">");
      }

    return buffer.toString();
  }

  /**
   * <P>This debug method prints out this item and all items
   * under this item if this item is the top node in a
   * tree.</P>
   */

  public void debugPrintTree(int indentLevel)
  {
    for (int i = 0; i < indentLevel; i++)
      {
        System.err.print("  ");
      }

    System.err.println(this.toString());

    if (children != null)
      {
        indentLevel++;

        for (int i = 0; i < children.length; i++)
          {
            children[i].debugPrintTree(indentLevel);
          }
      }
  }

  /**
   * <P>This debug method appends this item and all items
   * under this item if this item is the top node in a
   * tree to the StringBuffer passed in.</P>
   */

  public void getTreeString(StringBuffer buffer, int indentLevel)
  {
    for (int i = 0; i < indentLevel; i++)
      {
        buffer.append("  ");
      }

    buffer.append(this.toString());
    buffer.append("\n");

    if (children != null)
      {
        indentLevel++;

        for (int i = 0; i < children.length; i++)
          {
            children[i].getTreeString(buffer, indentLevel);
          }
      }
  }
}
