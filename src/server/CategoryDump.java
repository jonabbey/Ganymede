/*

   CategoryDump.java

   This class is intended to serve as a stub to hold basic
   information about server side categories for the client
   to process locally.

   Note that even though this class is implementing a remote
   interface, it is doing so for the purpose of providing
   a consistent interface for the client, not for actual
   remote access.  Thus, we are not extending UnicastRemoteObject
   as we would if we were truly a remote object.
   
   Created: 12 February 1998
   Release: $Name:  $
   Version: $Revision: 1.9 $
   Last Mod Date: $Date: 2000/02/29 09:35:06 $
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
                                                                    CategoryDump

------------------------------------------------------------------------------*/

/**
 *
 *  <p>This class is intended to serve as a stub to hold basic
 *  information about server side categories for the client
 *  to process locally.</p>
 *
 *  <p>Note that even though this class is implementing a remote
 *  interface, it is doing so for the purpose of providing
 *  a consistent interface for the client, not for actual
 *  remote access.  Thus, we are not extending UnicastRemoteObject
 *  as we would if we were truly a remote object.</p>
 *
 */

public class CategoryDump implements Category, CategoryNode {

  CategoryDump parent;
  String name;
  Vector contents = new Vector();

  private int lastIndex = -1;

  /* -- */

  public CategoryDump(CategoryDump parent, char[] src, int index)
  {
    String token;
    CategoryDump catChild;
    BaseDump baseChild;

    /* -- */

    this.parent = parent;

    if (parent == null)
      {
	// skip the 'cat' chunk if we're the root
	getChunk(src, index);
      }
    else
      {
	lastIndex = index;
      }

    this.name = getChunk(src, lastIndex);

    // getChunk() updates lastIndex for us

    token = getChunk(src, lastIndex);

    if (token.equals("<"))
      {
	// we've got contents

	token = getChunk(src, lastIndex);

	while (!token.equals(">"))
	  {
	    if (token.equals("cat"))
	      {
		catChild = new CategoryDump(this, src, lastIndex);
		lastIndex = catChild.getLastIndex();
		contents.addElement(catChild);
	      }
	    else if (token.equals("base"))
	      {
		baseChild = new BaseDump(this, src, lastIndex);
		lastIndex = baseChild.getLastIndex();
		contents.addElement(baseChild);
	      }
	    else
	      {
		throw new RuntimeException("parse error, unrecognized chunk: " + token);
	      }
	    
	    // get the next member chunk
	    
	    token = getChunk(src, lastIndex);
	  }
      }
    
    if (!token.equals(">"))
      {
	throw new RuntimeException("parse error, couldn't find end of category in dump: " + token);
      }
  }

  public int getLastIndex()
  {
    return lastIndex;
  }

  /**
   *
   * Returns the name of this category.
   *
   */

  public String getName() 
  {
    return name;
  }

  /**
   *
   * Returns the full path to this category, with levels
   * in the hierarchy separated by '/'s.
   *
   */

  public String getPath() 
  {
    if (parent != null)
      {
	return parent.getPath() + "/" + name;
      }
    else
      {
	return "/" + name;
      }
  }

  /**
   *
   * This method returns a vector of BaseDump objects, one for each
   * base held under this base.
   *
   */

  public synchronized Vector getBases()
  {
    Vector result = new Vector();

    /* -- */

    getBases(result);

    return result;
  }

  private void getBases(Vector inout)
  {
    for (int i = 0; i < contents.size(); i++)
      {
	if (contents.elementAt(i) instanceof BaseDump)
	  {
	    inout.addElement(contents.elementAt(i));
	  }
	else
	  {
	    CategoryDump element = (CategoryDump) contents.elementAt(i);
	    element.getBases(inout);
	  }
      }
  }

  /**
   *
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   *
   */

  public boolean setName(String newName) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method tells the CategoryNode what it's containing
   * category is.
   *
   */

  public void setCategory(Category category)
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method returns the category that this
   * category node belongs to.  If this is the DBStore's
   * root category, this will return null.
   *
   */

  public Category getCategory() 
  {
    return parent;
  }

  /**
   *
   * Returns child nodes
   *
   */

  public Vector getNodes() 
  {
    return contents;
  }

  /**
   *
   * Returns a subcategory of name <name>.
   *
   */

  public CategoryNode getNode(String name) 
  {
    CategoryNode candidate;

    /* -- */

    for (int i = 0; i < contents.size(); i++)
      {
	candidate = (CategoryNode) contents.elementAt(i);
	
	try
	  {
	    if (candidate.getName().equals(name))
	      {
		return candidate;
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    return null;
  }

  /**
   * <p>This method is used to place a Category Node under us.  This method
   * adds a new node into this category, after prevNodeName if prevNodeName
   * is not null, or at the end of the category if it is.</p>
   *
   * @param node Node to place under this category
   * @param prevNodeName the name of the node that the new node is to be added after
   *
   * @see arlut.csd.ganymede.Category
   */

  public void addNodeAfter(CategoryNode node, String prevNodeName)
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   * <p>This method is used to place a Category Node under us.  This method
   * adds a new node into this category, before nextNodeName if nextNodeName
   * is not null, or at the beginning of the category if it is.</p>
   *
   * @param node Node to place under this category
   * @param nextNodeName the name of the node that the new node is to be added before,
   * must not be path-qualified.
   *
   * @see arlut.csd.ganymede.Category
   */

  public void addNodeBefore(CategoryNode node, String nextNodeName)
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method can be used to move a Category from another Category to this Category,
   * or to move a Category around within this Category.
   *
   * @param node Node to place under this category
   * @param prevNodeName the name of the node that the new node is to be added after
   *
   */

  public void moveCategoryNode(String catPath, String prevNodeName)
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method is used to remove a Category Node from under us.
   *
   * Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.
   * 
   */

  public void removeNode(CategoryNode node) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method is used to remove a Category Node from under us.
   *
   * Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.
   * 
   */

  public void removeNode(String name) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.
   *
   */

  public Category newSubCategory() 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method returns true if this
   * is a subcategory of cat.
   *
   */

  public boolean isUnder(Category cat) 
  {
    if (cat == null)
      {
	return false;
      }

    if (cat.equals(this))
      {
	return true;
      }

    if (parent == null)
      {
	return false;
      }
    else
      {
	return parent.isUnder(cat);
      }
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
}
