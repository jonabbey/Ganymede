/*

   Category.java

   Remote interface to a DBBaseCategory object.
   
   Created: 11 August 1997

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

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

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
   * Returns the name of this category.
   */

  public String getName() throws RemoteException;

  /**
   * Returns the full path to this category, with levels
   * in the hierarchy separated by '/'s.
   */

  public String getPath() throws RemoteException;

  /**
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   */

  public boolean setName(String newName) throws RemoteException;

  /**
   * This method returns the category that this
   * category node belongs to.  If this is the DBStore's
   * root category, this will return null.
   */

  public Category getCategory() throws RemoteException;

  /**
   * Returns child nodes
   */

  public Vector getNodes() throws RemoteException;

  /**
   * Returns a subcategory of name <name>.
   */

  public CategoryNode getNode(String name) throws RemoteException;

  /**
   * This method is used to place a Category Node under us.  The node
   * will be placed according to the node's displayOrder value, if resort
   * and/or adjustNodes are true.
   *
   * @param node Node to place under this category
   * @param previousNodeName The name of the node to place node after,
   * must not be path-qualified.
   */

  public void addNodeAfter(CategoryNode node, String previousNodeName) throws RemoteException;

  /**
   * <p>This method is used to place a Category Node under us.  This method
   * adds a new node into this category, before nextNodeName if nextNodeName
   * is not null, or at the beginning of the category if it is.</p>
   *
   * @param node Node to place under this category
   * @param nextNodeName the name of the node that the new node is to be added before,
   * must not be path-qualified
   */

  public void addNodeBefore(CategoryNode node, String nextNodeName) throws RemoteException;

  /**
   * This method can be used to move a Category from another Category to this Category,
   * or to move a Category around within this Category.
   *
   * @param catPath the fully specified path of the node to be moved
   * @param previousNodeName The name of the node to place node after,
   * must be fully path-qualified.
   */

  public void moveCategoryNode(String catPath, String previousNodeName) throws RemoteException;

  /**
   * <p>This method is used to remove a Category Node from under us.</p>
   *
   * <p>Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.</p>
   */

  public void removeNode(CategoryNode node) throws RemoteException;

  /**
   * <p>This method is used to remove a Category Node from under us.</p>
   *
   * <p>Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.</p>
   */

  public void removeNode(String name) throws RemoteException;

  /**
   * This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.
   */

  public Category newSubCategory() throws RemoteException;

  /**
   * This method returns true if this
   * is a subcategory of cat.
   */

  public boolean isUnder(Category cat) throws RemoteException;
}

