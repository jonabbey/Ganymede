/*

   BaseDump.java

   This class is intended to serve as a stub to hold basic
   information about server side object bases for the client
   to process locally.

   Note that even though this class is implementing a remote
   interface, it is doing so for the purpose of providing
   a consistent interface for the client, not for actual
   remote access.  Thus, we are not extending UnicastRemoteObject
   as we would if we were truly a remote object.

   Created: 12 February 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.common;

import java.util.Vector;

import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.CategoryNode;
import arlut.csd.ganymede.rmi.Session;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        BaseDump

------------------------------------------------------------------------------*/

/**
 * <p>This class is intended to serve as a stub to hold basic
 * information about server side categories for the client to process
 * locally.</p>
 *
 * <p>Many of the methods in the Base interface are there to support
 * remote schema editing by the admin console's schema editor, and
 * have no effect when called on instances of this
 * class. RuntimeException's will be thrown if those methods are
 * called on BaseDump.</p>
 *
 * <p>Note that even though this class is implementing a remote
 * interface, it is doing so for the purpose of providing a consistent
 * interface for the client, not for actual remote access.  Thus, we
 * are not extending UnicastRemoteObject as we would if we were truly
 * a remote object.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class BaseDump implements Base, CategoryNode {

  CategoryDump parent;

  String name;
  String pathedName;
  short type_code;
  short label_id;
  String labelFieldName;
  boolean canInactivate;
  boolean canCreate;
  boolean isEmbedded;

  private int lastIndex = -1;

  /* -- */

  /**
   * Constructor for use by a CategoryDump object
   */

  public BaseDump(CategoryDump parent, char[] src, int index)
  {
     // assume whoever called us already extracted the 'cat' chunk.

    this.parent = parent;
    this.name = getChunk(src, index);

    // getChunk() updates lastIndex for us

    this.pathedName = getChunk(src, lastIndex);

    try
      {
        this.type_code = Short.valueOf(getChunk(src, lastIndex)).shortValue();
      }
    catch (NumberFormatException ex)
      {
        throw new RuntimeException("couldn't parse type code chunk " + ex);
      }

    try
      {
        this.label_id = Short.valueOf(getChunk(src, lastIndex)).shortValue();
      }
    catch (NumberFormatException ex)
      {
        throw new RuntimeException("couldn't parse label id chunk " + ex);
      }

    labelFieldName = getChunk(src, lastIndex);

    this.canInactivate = Boolean.valueOf(getChunk(src, lastIndex)).booleanValue();
    this.canCreate = Boolean.valueOf(getChunk(src, lastIndex)).booleanValue();
    this.isEmbedded = Boolean.valueOf(getChunk(src, lastIndex)).booleanValue();
  }

  /**
   * Constructor for use by a BaseListTransport object
   */

  public BaseDump(BaseListTransport baselist, char[] src, int index)
  {
    /* -- */

    // assume whoever called us already extracted the 'base' chunk.

    this.parent = null;
    this.name = getChunk(src, index);
    this.pathedName = getChunk(src, lastIndex);

    // getChunk() updates lastIndex for us

    try
      {
        this.type_code = Short.valueOf(getChunk(src, lastIndex)).shortValue();
      }
    catch (NumberFormatException ex)
      {
        throw new RuntimeException("couldn't parse type code chunk " + ex);
      }

    try
      {
        this.label_id = Short.valueOf(getChunk(src, lastIndex)).shortValue();
      }
    catch (NumberFormatException ex)
      {
        throw new RuntimeException("couldn't parse label id chunk " + ex);
      }

    labelFieldName = getChunk(src, lastIndex);

    this.canInactivate = Boolean.valueOf(getChunk(src, lastIndex)).booleanValue();
    this.canCreate = Boolean.valueOf(getChunk(src, lastIndex)).booleanValue();
    this.isEmbedded = Boolean.valueOf(getChunk(src, lastIndex)).booleanValue();
  }

  public int getLastIndex()
  {
    return lastIndex;
  }

  // ***
  //
  // CategoryNode methods
  //
  // ***

  /**
   * This method tells the CategoryNode what it's containing
   * category is.
   */

  public void setCategory(Category category)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  // ***
  //
  // Base methods
  //
  // ***

  public boolean isRemovable()
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public boolean isEmbedded()
  {
    return isEmbedded;
  }

  public String getName()
  {
    return name;
  }

  public String getPath()
  {
    return pathedName;
  }

  public String getClassName()
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public String getClassOptionString()
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public short getTypeID()
  {
    return type_code;
  }

  public short getLabelField()
  {
    return label_id;
  }

  public BaseField getLabelFieldDef()
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public String getLabelFieldName()
  {
    return labelFieldName;
  }

  public Vector<BaseField> getFields(boolean includeBuiltIns)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public Vector<BaseField> getFields()
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public BaseField getField(short id)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public BaseField getField(String name)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  /**
   * We ignore the Session param here, since we're a client side dump
   * associated with a known Session.
   */

  public boolean canCreate(Session session)
  {
    return canCreate;
  }

  public boolean canInactivate()
  {
    return canInactivate;
  }

  // the following methods are only valid when the Base reference
  // is obtained from a SchemaEdit reference.

  public ReturnVal setName(String newName)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public ReturnVal setClassInfo(String newClassName, String newClassOptionString)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public ReturnVal moveFieldAfter(String fieldName, String previousFieldName)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public ReturnVal moveFieldBefore(String fieldName, String nextFieldName)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public ReturnVal setLabelField(String fieldName)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public ReturnVal setLabelField(short fieldID)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public Category getCategory()
  {
    return parent;
  }

  public BaseField createNewField()
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public ReturnVal deleteField(String baseName)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public boolean fieldInUse(String baseName)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  // ***
  //
  // private methods
  //
  // ***

  private String getChunk(char[] chars, int startDex)
  {
    StringBuilder result = new StringBuilder();

    /* -- */

    for (lastIndex = startDex; lastIndex < chars.length; lastIndex++)
      {
        if (chars[lastIndex] == '|')
          {
            lastIndex++;
            return result.toString();
          }
        else if (chars[lastIndex] == '\\')
          {
            result.append(chars[++lastIndex]);
          }
        else
          {
            result.append(chars[lastIndex]);
          }
      }

    throw new RuntimeException("Ran out of chunk data: " + result.toString());
  }

  /**
   * <p>as a convenience for using the standard VecQuickSort comparator.</p>
   */

  public String toString()
  {
    return getName();
  }
}
