/*

  treeDragDropCallback.java

  Drag and Drop interface for the tree callback.

  Copyright (C) 1997  The University of Texas at Austin.

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
   
  Created: 22 June 1997
  Version: $Revision: 1.1 $ %D%
  Module By: Jonathan Abbey              jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTree;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    treeCallback

------------------------------------------------------------------------------*/

/**
 *
 * Callback interface to be implemented by objects that manage drag and drop
 * behavior within a treeControl.
 *
 * @author Jonathan Abbey
 * @version $Revision: 1.1 $ %D%
 *
 * @see arlut.csd.Tree.treeControl
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
