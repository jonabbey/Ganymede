/*

   Category.java

   Remote interface to a DBBaseCategory object.

   Created: 11 August 1997

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

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                        Category

------------------------------------------------------------------------------*/

/**
 * <p>Client side interface definition for the Ganymede DBBaseCategory
 * class.  This interface allows the client to manipulate a category's
 * relationships.</p>
 */

public interface Category extends Remote {

  /**
   * @return The name of this category.
   */

  public String getName() throws RemoteException;

  /**
   * @return The full path to this category, with levels in the
   * hierarchy separated by '/'s.
   */

  public String getPath() throws RemoteException;

  /**
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   *
   * @param newName The new name for this node
   */

  public boolean setName(String newName) throws RemoteException;

  /**
   * @return the category that this category node belongs to.  null if
   * this is the DBStore's root category.
   */

  public Category getCategory() throws RemoteException;

  /**
   * @return child nodes
   */

  public Vector getNodes() throws RemoteException;

  /**
   * @param name The name of the child node to return
   * @return A subcategory of this Category.
   */

  public CategoryNode getNode(String name) throws RemoteException;

  /**
   * <p>This method is used to place a Category Node under us.  The
   * node will be placed according to the node's displayOrder value,
   * if resort and/or adjustNodes are true.</p>
   *
   * @param node Node to place under this category
   * @param previousNodeName The name of the node to place node after,
   * must not be path-qualified.
   */

  public void addNodeAfter(CategoryNode node, String previousNodeName) throws RemoteException;

  /**
   * <p>This method is used to place a Category Node under us.  This
   * method adds a new node into this category, before nextNodeName if
   * nextNodeName is not null, or at the beginning of the category if
   * it is.</p>
   *
   * @param node Node to place under this category
   * @param nextNodeName the name of the node that the new node is to be added before,
   * must not be path-qualified
   */

  public void addNodeBefore(CategoryNode node, String nextNodeName) throws RemoteException;

  /**
   * <p>This method can be used to move a Category from another
   * Category to this Category, or to move a Category around within
   * this Category.</p>
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
   * <p>This creates a new subcategory under this category, with
   * displayOrder after the last item currently in the category.  This
   * method should only be called when there are no nodes left to be
   * added to the category with prefixed displayOrder values.</p>
   *
   * @return A new Category that has been placed under this Category.
   */

  public Category newSubCategory() throws RemoteException;

  /**
   * @return True if this is a subcategory of cat.
   */

  public boolean isUnder(Category cat) throws RemoteException;
}

