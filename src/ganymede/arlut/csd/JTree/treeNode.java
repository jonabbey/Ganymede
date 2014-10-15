/*

   treeNode.java

   A node in the arlut.csd.Tree display.

   Created: 3 March 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

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

package arlut.csd.JTree;

import java.util.Stack;

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
 * @version $Id$
 *
 * @see arlut.csd.JTree.treeCanvas
 *
 */

public class treeNode implements Cloneable {

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
  treeControl tree;

  int row;                      // # of the row in the tree this node is currently visible at

  Stack childStack;

  /* -- */

  /**
   *
   * @param parent Parent node to insert this node under, null if this is the root node
   * @param text Content of this node
   * @param insertAfter sibling to insert this node after, null if this is the root node
   * @param expandable this node is a folder node, and should always have a +/- box
   * @param openImage Index of treeCanvas image used to display this node if it is expanded
   * @param closedImage Index of treeCanvas image used to display this node if it is not expanded
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

    row = -1;                   // undetermined
  }

  /**
   * @param parent Parent node to insert this node under, null if this is the root node
   * @param text Content of this node
   * @param insertAfter sibling to insert this node after, null if this is the root node
   * @param expandable this node is a folder node, and should always have a +/- box
   * @param openImage Index of treeCanvas image used to display this node if it is expanded
   * @param closedImage Index of treeCanvas image used to display this node if it is not expanded
   */

  public treeNode(treeNode parent, String text, treeNode insertAfter,
                  boolean expandable, int openImage, int closedImage)
  {
    this(parent, text, insertAfter, expandable, openImage, closedImage, null);
  }

  /**
   * @param parent Parent node to insert this node under, null if this is the root node
   * @param text Content of this node
   * @param insertAfter sibling to insert this node after, null if this is the root node
   * @param expandable this node is a folder node, and should always have a +/- box
   */

  public treeNode(treeNode parent, String text, treeNode insertAfter, boolean expandable)
  {
    this(parent, text, insertAfter, expandable, -1, -1, null);
  }

  /**
   * <p>This method does a full clone of this object.  Code that
   * clones a treeNode may want to call resetNode() on the result to
   * prepare the node for re-insertion into the tree.</p>
   */

  public Object clone()
  {
    try
      {
        return super.clone();
      }
    catch (CloneNotSupportedException ex)
      {
        throw new RuntimeException("What the hey?  treeNode superclass not clonable.");
      }
  }

  /**
   * <p>This clears this node's fields relating to the node's state and
   * position in the tree.</p>
   */

  public void resetNode()
  {
    parent = null;
    prevSibling = null;
    child = null;
    nextSibling = null;
    childStack = null;

    expanded = false;
    selected = false;

    row = -1;                   // undetermined
  }

  public String getText()
  {
    return text;
  }

  public void setText(String text)
  {
    this.text = text;
  }

  /**
   * <p>This method allows you to change the popup menu
   * on a tree node.</p>
   *
   * @param menu The popup menu to connect to this node.
   */

  public void setMenu(treeMenu menu)
  {
    this.menu = menu;

    if (tree == null)
      {
        return;
      }

    if (menu.registerItems(tree))
      {
        tree.canvas.add(menu);
      }
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
   * @param key The node to search for
   * @return The child node of the given name
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

  public boolean isUnder(treeNode node)
  {
    treeNode pNode = this;

    while (pNode != null)
      {
        if (pNode == node)
          {
            return true;
          }

        pNode = pNode.getParent();
      }

    return false;
  }

  /**
   * Clean up any additional fields the node may be carrying along, to
   * be overridden in subclasses.
   */

  public void cleanup()
  {
  }
}
