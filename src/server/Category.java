/*

   Category.java

   Remote interface to a DBBaseCategory object.
   
   Created: 11 August 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:05:28 $
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
                                                                       interface
                                                                        Category

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface definition for the Ganymede DBBaseCategory class.  This
 * interface allows the client to manipulate a category's relationships.
 *
 */

public interface Category extends Remote {

  /**
   *
   * Returns the name of this category.
   *
   */

  public String getName() throws RemoteException;

  /**
   *
   * Returns the full path to this category, with levels
   * in the hierarchy separated by '/'s.
   *
   */

  public String getPath() throws RemoteException;

  /**
   *
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   *
   */

  public boolean setName(String newName) throws RemoteException;

  /**
   *
   * This method returns the category that this
   * category node belongs to.  If this is the DBStore's
   * root category, this will return null.
   *
   */

  public Category getCategory() throws RemoteException;

  /**
   *
   * Returns child nodes
   *
   */

  public Vector getNodes() throws RemoteException;

  /**
   *
   * Returns a subcategory of name <name>.
   *
   */

  public CategoryNode getNode(String name) throws RemoteException;

  /**
   *
   * This method is used to place a Category Node under us.  The node
   * will be placed according to the node's displayOrder value, if resort
   * and/or adjustNodes are true.
   *
   * @param node Node to place under this category
   * @param sort If true, the nodes under this category will be resorted after insertion
   * @param adjustNodes If true, the nodes under this category will have their displayOrder recalculated.
   * this should not be done lightly, and not at all if any more nodes with precalculated or saved
   * displayOrder's are to be later inserted.
   *
   */

  public void addNode(CategoryNode node, boolean resort, boolean adjustNodes) throws RemoteException;

  /**
   *
   * This method can be used to move a Category from another Category to this Category,
   * or to move a Category around within this Category.
   *
   * @param catPath the fully specified path of the node to be moved
   * @param displayOrder where to place this node within this category.
   *
   */

  public void moveCategoryNode(String catPath, int displayOrder) throws RemoteException;

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

  public void removeNode(CategoryNode node) throws RemoteException;

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

  public void removeNode(String name) throws RemoteException;

  /**
   *
   * This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.
   *
   */

  public Category newSubCategory() throws RemoteException;

  /**
   *
   * This method returns true if this
   * is a subcategory of cat.
   *
   */

  public boolean isUnder(Category cat) throws RemoteException;

  /**
   *
   * Gets the order of this node in the containing category
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public int getDisplayOrder() throws RemoteException;

  /**
   *
   * Sets the order of this node in the containing category
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public void setDisplayOrder(int order) throws RemoteException;

}

