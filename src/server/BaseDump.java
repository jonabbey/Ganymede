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
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2000/02/11 07:16:58 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        BaseDump

------------------------------------------------------------------------------*/

/**
 *
 * <p>This class is intended to serve as a stub to hold basic
 * information about server side categories for the client
 * to process locally.</p>
 *
 * <p>Many of the methods in the Base interface are there to support
 * remote schema editing by the admin console's schema editor, and have no
 * effect when called on instances of this class. RuntimeException's
 * will be thrown if those methods are called on BaseDump.</p>
 *
 * <p>Note that even though this class is implementing a remote
 * interface, it is doing so for the purpose of providing
 * a consistent interface for the client, not for actual
 * remote access.  Thus, we are not extending UnicastRemoteObject
 * as we would if we were truly a remote object.</p>
 *
 * @version $Revision: 1.7 $ $Date: 2000/02/11 07:16:58 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class BaseDump implements Base, CategoryNode {

  CategoryDump parent;

  String name;
  short type_code;
  short label_id;
  String labelFieldName;
  boolean canInactivate;
  boolean canCreate;
  boolean isEmbedded;
  int displayOrder;

  private int lastIndex = -1;

  /* -- */

  /**
   *
   * Constructor for use by a CategoryDump object
   *
   */

  public BaseDump(CategoryDump parent, char[] src, int index)
  {
    String token;

    /* -- */

    // assume whoever called us already extracted the 'cat' chunk.

    this.parent = parent;
    this.name = getChunk(src, index);

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

    try
      {
	this.displayOrder = Integer.valueOf(getChunk(src, lastIndex)).intValue();
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("couldn't parse display order chunk " + ex);
      }
  }

  /**
   *
   * Constructor for use by a BaseListTransport object
   *
   */

  public BaseDump(BaseListTransport baselist, char[] src, int index)
  {
    String token;

    /* -- */

    // assume whoever called us already extracted the 'base' chunk.

    this.parent = null;
    this.name = getChunk(src, index);

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

    try
      {
	this.displayOrder = Integer.valueOf(getChunk(src, lastIndex)).intValue();
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("couldn't parse display order chunk " + ex);
      }
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
   *
   * Returns the display order of this Base within the containing
   * category.
   *
   */

  public int getDisplayOrder()
  {
    return displayOrder;
  }

  /**
   *
   * Sets the display order of this Base within the containing
   * category.
   *
   */

  public void setDisplayOrder(int order)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  /**
   *
   * This method tells the CategoryNode what it's containing
   * category is.
   *
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

  public String getClassName() 
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

  public String getLabelFieldName() 
  {
    return labelFieldName;
  }

  public Vector getFields(boolean includeBuiltIns)
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }  

  public Vector getFields()
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
   *
   * We ignore the Session param here, since we're a client side dump
   * associated with a known Session.
   * 
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

  public boolean setName(String newName) 
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }  

  public void setClassName(String newName) 
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public void setLabelField(String fieldName) 
  {
    throw new RuntimeException("this method not supported in BaseDump");
  } 

  public void setLabelField(short fieldID) 
  {
    throw new RuntimeException("this method not supported in BaseDump");
  } 

  public Category getCategory() 
  {
    return parent;
  }

  /**
   *
   * If lowRange is true, the field's id will start at 100 and go up,
   * other wise it will start at 256 and go up.
   *
   */

  public BaseField createNewField(boolean lowRange) 
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }  

  public boolean deleteField(BaseField bF) 
  {
    throw new RuntimeException("this method not supported in BaseDump");
  }

  public boolean fieldInUse(BaseField bF) 
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
    StringBuffer result = new StringBuffer();

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
