/*

   treeDragDropCallback.java

   Drag and Drop interface for the tree callback.

   Created: 22 June 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

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

package arlut.csd.JTree;

/*------------------------------------------------------------------------------
                                                                       interface
                                                            treeDragDropCallback

------------------------------------------------------------------------------*/

/**
 * Callback interface to be implemented by objects that manage drag and drop
 * behavior within a treeControl.
 *
 * @author Jonathan Abbey
 * @version $Id$
 *
 * @see arlut.csd.JTree.treeControl
 */

public interface treeDragDropCallback {

  /**
   * Called whenever a drag begins, to verify that the node in question
   * may be dragged.
   *
   * @param dragNode The node in the tree that the user is attempting to drag.
   *
   */

  public boolean startDrag(treeNode dragNode);

  /**
   * Called during a DRAG_ICON drag whenever the icon being dragged is
   * moved onto a new node's row.  The iconDragOver callback can
   * return false, in which case the row that the node is being
   * dragged onto will not be selected.
   *
   * @param dragNode The node in the tree that the user is attempting to drag.
   * @param targetNode The node in the tree that the dragNode is being moved onto.
   * */

  public boolean iconDragOver(treeNode dragNode, treeNode targetNode);

  /**
   * Called whenever a DRAG_ICON drag ends, to notify the client what
   * node the dragNode is being dropped onto.  iconDragDrop() will not
   * be called if the client refused the iconDragOver() for the
   * targetNode in question.
   *
   * @param dragNode The node in the tree that the user is attempting to drag.
   * @param targetNode The node in the tree that the dragNode is being dropped onto.
   * */

  public void iconDragDrop(treeNode dragNode, treeNode targetNode);

  /**
   * Called during a DRAG_LINE drag operation, when the dragNode is
   * pulled between the midline of a pair of rows.
   *
   * @param dragNode The node in the tree that the user is attempting to drag.
   * @param aboveNode The node in the tree below which the line is being dragged to.
   * @param belowNode The node in the tree above which the line is being dragged to.
   * */

  public boolean dragLineTween(treeNode dragNode, treeNode aboveNode, treeNode belowNode);

  /**
   * Called when a DRAG_LINE drag operation ends.  If the client
   * returned false when the line was pulled between aboveNode and
   * belowNode, dragLineRelease() will not be called.
   *
   * @param dragNode The node in the tree that the user is attempting to drag.
   * @param aboveNode The node in the tree below which the line is being released.
   * @param belowNode The node in the tree above which the line is being released.
   * */

  public void dragLineRelease(treeNode dragNode, treeNode aboveNode, treeNode belowNode);
}
