/*

  treeNode.java

  A node in the arlut.csd.Tree display.

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
  
  Created: 3 March 1997
  Version: $Revision: 1.1 $ %D%
  Module By: Jonathan Abbey              jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTree;

import java.awt.PopupMenu;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        treeNode

------------------------------------------------------------------------------*/

/**
 *
 * <p>treeNode is a node in the treeCanvas widget.  A treeNode consists of
 * a text string with pointers to maintain its place in the tree displayed
 * by the treeCanvas.</p>
 *
 * @author Jonathan Abbey
 * @version $Revision: 1.1 $ %D%
 *
 * @see arlut.csd.Tree.treeCanvas
 *
 */

public class treeNode {

  String text;
  boolean expandable;
  boolean expanded;
  boolean selected;
  int openImage;
  int closedImage;

  int boxX1, boxX2, boxY1, boxY2;

  treeNode parent;
  treeNode child;
  treeNode prevSibling;
  treeNode nextSibling;

  treeMenu menu;

  int row;			// # of the row in the tree this node is currently visible at

  Stack childStack;

  /* -- */

  /**
   *
   * @param parent Parent node to insert this node under, null if this is the root node
   * @param text Content of this node
   * @param insertAfter sibling to insert this node after, null if this is the root node
   * @param expandable this node is a folder node, and should always have a +/- box
   * @param openImage Index of treeCanvas image used to display this node if it is not expanded
   * @param openImage Index of treeCanvas image used to display this node if it is expanded
   * @param menu Popup menu to attach to this node
   *
   */
  
  public treeNode(treeNode parent, String text, treeNode insertAfter,
		  boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    this.parent = parent;
    this.text = text;
    this.expandable = expandable;
    this.openImage = openImage;
    this.closedImage = closedImage;
    this.menu = menu;

    child = null;
    prevSibling = insertAfter;
    nextSibling = null;
    childStack = null;

    expanded = false;
    selected = false;

    row = -1;			// undetermined
  }

  /**
   *
   * @param parent Parent node to insert this node under, null if this is the root node
   * @param text Content of this node
   * @param insertAfter sibling to insert this node after, null if this is the root node
   * @param expandable this node is a folder node, and should always have a +/- box
   * @param openImage Index of treeCanvas image used to display this node if it is not expanded
   * @param openImage Index of treeCanvas image used to display this node if it is expanded
   *
   */
  
  public treeNode(treeNode parent, String text, treeNode insertAfter,
		  boolean expandable, int openImage, int closedImage)
  {
    this(parent, text, insertAfter, expandable, openImage, closedImage, null);
  }

  /**
   *
   * @param parent Parent node to insert this node under, null if this is the root node
   * @param text Content of this node
   * @param insertAfter sibling to insert this node after, null if this is the root node
   * @param expandable this node is a folder node, and should always have a +/- box
   *
   */
  
  public treeNode(treeNode parent, String text, treeNode insertAfter, boolean expandable)
  {
    this(parent, text, insertAfter, expandable, -1, -1, null);
  }

  public String getText()
  {
    return text;
  }

  public void setText(String text)
  {
    this.text = text;
  }
  
  // Variety of methods to change the images

  public void setImages(int openImage, int closedImage)
  {
    this.openImage = openImage;
    this.closedImage = closedImage;
  }

  public void setOpenImage(int openImage)
  {
    this.openImage = openImage;
  }

  public void setClosedImage(int closedImage)
  {
    this.closedImage = closedImage;
  }

  public int getOpenImage()
  {
    return openImage;
  }

  public int getClosedImage()
  {
    return closedImage;
  }

  public treeNode getParent()
  {
    return parent;
  }

  public treeNode getPrevSibling()
  {
    return prevSibling;
  }

  public treeNode getChild()
  {
    return child;
  }

  /**
   *
   * Returns the child node with name 'key',
   * if there is any such.
   *
   */

  public treeNode getChild(String key)
  {
    treeNode result = child;

    while (result != null)
      {
	if (result.getText().equals(key))
	  {
	    return result;
	  }

	result = result.getNextSibling();
      }

    return null;
  }

  public treeNode getNextSibling()
  {
    return nextSibling;
  }

  public boolean isOpen()
  {
    return expanded;
  }

}
