/*
   treeCallback.java

   Interface for the tree callback

   Created: 3 March 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
   The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JTree;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    treeCallback

------------------------------------------------------------------------------*/

/**
 *
 * Callback interface to be implemented by objects that receive notification
 * of user activities on a treeControl.
 *
 * @author Jonathan Abbey
 * @version $Id$
 *
 * @see arlut.csd.JTree.treeControl
 */

public interface treeCallback {

  /**
   *
   * Called when an item in the tree is selected
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeSelected(treeNode node);

  /**
   *
   * Called when an item in the tree is unselected
   *
   * @param node The node selected in the tree.
   * @param someNodeSelected If true, this node is being unselected by the selection
   *                         of another node.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeUnSelected(treeNode node, boolean someNodeSelected);

  /**
   *
   * Called when a popup menu item is selected
   * on a treeNode
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */


  public void treeNodeMenuPerformed(treeNode node, java.awt.event.ActionEvent event);

  /**
   *
   * Called when a node is expanded, to allow the
   * user of the tree to dynamically load the information
   * at that time.
   *
   */

  public void treeNodeExpanded(treeNode node);

  /**
   *
   * Called when a node is closed.
   *
   */

  public void treeNodeContracted(treeNode node);

  /**
   *
   * Called when an item in the tree is double-clicked.
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCanvas
   */

  public void treeNodeDoubleClicked(treeNode node);
 
}
