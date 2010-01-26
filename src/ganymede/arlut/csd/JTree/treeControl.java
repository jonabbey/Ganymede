/*

   treeControl.java

   A hierarchical tree panel for Java

   This component allows the display of a tree structured
   graph of nodes, each node being a small image and a line of text.
   Nodes with children can be opened or closed, allowing the child
   nodes to be made visible or hidden.  Each node can be selected
   and can have a pop-up menu attached.  Nodes can be dragged, with
   both 'drag-tween' and 'drag on' drag supported.

   Created: 3 March 1997

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.System;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Stack;
import java.util.Vector;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;


/*------------------------------------------------------------------------------
                                                                           class
                                                                     treeControl

------------------------------------------------------------------------------*/

/**
 * This component allows the display of a tree structured graph of
 * nodes, each node being a small image and a line of text.  Nodes
 * with children can be opened or closed, allowing the child nodes to
 * be made visible or hidden.  Each node can be selected and can have
 * a pop-up menu attached.  Nodes can be dragged, with both
 * 'drag-tween' and 'drag on' drag supported.
 *
 * @author Jonathan Abbey
 *
 * @see arlut.csd.JTree.treeCallback
 * @see arlut.csd.JTree.treeNode
 */

public class treeControl extends JPanel implements AdjustmentListener, ActionListener, FocusListener, MouseWheelListener
{
  static final boolean debug = false;

  static final int borderSpace = 0;

  // drag mode codes, used as bit masks

  public static final int DRAG_NONE = 0;
  public static final int DRAG_ICON = 1;
  public static final int DRAG_LINE = 2;

  // ---

  // general tree datastructures

  treeNode root;
  treeCallback callback;
  treeCanvas canvas;

  treeNode selectedNode = null;

  // drag and drop support

  treeDragDropCallback
    dCallback = null;

  int
    dragMode = DRAG_NONE;

  treeNode
    oldNode = null,
    dragNode = null,
    dragOverNode = null,	// used in DRAG_ICON mode
    dragBelowNode = null,	// used in DRAG_LINE mode
    dragAboveNode = null;	// used in DRAG_LINE mode

  // popup menu support

  treeMenu
    menu;			// default popup menu attached to tree as a whole

  // housekeeping

  JScrollBar
    hbar,
    vbar;

  Rectangle
    bounding_rect;

  boolean
    hbar_visible,
    vbar_visible;

  int
    minWidth = 50,
    maxWidth = 0,		// how wide is our tree at its maximum width currently?
    row_height;			// how tall is a row of our tree, all told?

  Vector
    rows;			// map visible rows to nodes in the tree

  treeNode
    menuedNode = null;		// node most recently selected by a popup menu

  /* -- */

  /**
   * Constructor
   *
   * @param font Font for text in the Tree Canvas
   * @param fgColor Foreground color for text in the treeCanvas
   * @param bgColor Background color for text in the treeCanvas
   * @param callback Object to receive notification of events
   * @param images Array of images to be used in the canvas;  nodes refer to images by index
   * @param menu Popup menu to attach to the treeControl
   */

  public treeControl(Font font, Color fgColor, Color bgColor, treeCallback callback,
		     Image[] images, treeMenu menu)
  {
    super(false);		// no double buffering for us, we'll do it ourselves

    /* Initialize our bounding rectangle.
       This will get filled when the AWT calls our
       setBounds() method. */

    this.callback = callback;
    this.menu = menu;

    bounding_rect = new Rectangle();

    setLayout(new BorderLayout());

    canvas = new treeCanvas(this, font, fgColor, bgColor, images);

    add("Center", canvas);

    // create our scroll bars, but don't add them to our
    // container until we know we need them.

    hbar = new JScrollBar(JScrollBar.HORIZONTAL);
    hbar.addAdjustmentListener(this);
    vbar = new JScrollBar(JScrollBar.VERTICAL);
    vbar.addAdjustmentListener(this);

    // register the popup menu on ourselves

    if (menu!= null)
      {
	if (menu.registerItems(this))
	  {
	    canvas.add(menu);
	  }
      }

    rows = new Vector();

    addMouseWheelListener(this);

    addKeyListener(new myKeyListener());

    initializeKeyboardActions();

    setFocusable(true);

    addFocusListener(this);
  }

  /**
   * Constructor
   *
   * @param font Font for text in the Tree Canvas
   * @param fgColor Foreground color for text in the treeCanvas
   * @param bgColor Background color for text in the treeCanvas
   * @param callback Object to receive notification of events
   * @param images Array of images to be used in the canvas;  nodes refer to images by index
   */

  public treeControl(Font font, Color fgColor, Color bgColor, treeCallback callback,
		     Image[] images)
  {
    this(font, fgColor, bgColor, callback, images, null);
  }

  /**
   * This method is used to set the drag behavior of the tree.
   *
   * @param dCallback DragDrop manager object
   * @param mode A binary or'ing of of treeControl.DRAG_NONE, treeControl.DRAG_ICON,
   * and treeControl.DRAG_LINE.
   *
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public synchronized void setDrag(treeDragDropCallback dCallback, int mode)
  {
    this.dCallback = dCallback;

    if (dCallback == null)
      {
	dragMode = DRAG_NONE;
      }
    else
      {
	dragMode = mode;
      }
  }

  /**
   * <p>Call this on the GUI thread to break apart and destroy this
   * tree.</p>
   */

  public synchronized void destroyTree()
  {
    this.callback = null;
    this.clearTree();
    this.removeAll();
  }

  /**
   * Clear out the tree.
   */

  public synchronized void clearTree()
  {
    if (root == null)
      {
	return;
      }

    // get rid of the nodes

    breakdownTree(root);
    root = null;

    // clear our visibility vector

    unselectAllNodes(false);

    if (rows != null)
      {
	rows.removeAllElements();
      }
    else
      {
	rows = new Vector();	// shouldn't ever be the case, but just
				// to be safe..
      }

    refresh();
  }

  /**
   * Clear the tree and establish a new root node.
   */

  public synchronized void setRoot(treeNode root)
  {
    if (root == null)
      {
	throw new IllegalArgumentException("can't setRoot(null), use clearTree");
      }

    unselectAllNodes(false);

    if (this.root != null)
      {
	breakdownTree(this.root);
      }

    if (rows != null)
      {
	rows.removeAllElements();
      }
    else
      {
	rows = new Vector();
      }

    // let the node know who it is attached to

    root.tree = this;

    this.root = root;

    root.row = 0;

    if (root.menu != null)
      {
	if (root.menu.registerItems(this))
	  {
	    canvas.add(root.menu);
	  }
      }

    rows.addElement(root);

    selectNode(root);

    reShape();
    refreshTree();
  }

  /**
   * Sets the minimum width the treeControl will demand in a sliding
   * pane.
   */

  public void setMinimumWidth(int minWidth)
  {
    this.minWidth = minWidth;
  }

  /**
   * Inserts a new node into the tree.  newNode's prevSibling is
   * checked first.  If it is non-null, newNode is inserted after
   * newNode.prevSibling, regardless of what newNode.parent says.  If
   * prevSibling is null, newNode is made the first child of its
   * requested parent.
   *
   * @param newNode The node to be inserted.  Properties of the node determine where the node is inserted.
   * @param repaint If true, immediately re-render and refresh the treeCanvas.
   */

  public synchronized void insertNode(treeNode newNode, boolean repaint)
  {
    treeNode p;

    /* -- */

    if (debug)
      {
	System.err.println("treeControl: Inserting node " + newNode.getText() + ", repaint is " + repaint);
      }

    newNode.childStack = null;

    if (newNode.prevSibling == null && newNode.parent == null)
      {
	throw new IllegalArgumentException("error, no insert after or parent set.  Use setRoot to establish root node");
      }

    // let the node know who we are, so it can change
    // its menu later

    newNode.tree = this;

    // attach the menu to us

    if (newNode.menu != null)
      {
	if (newNode.menu.registerItems(this))
	  {
	    canvas.add(newNode.menu);
	  }
      }

    if (newNode.prevSibling != null)
      {
	// if we're being inserted after the last sibling
	// in the family and they had a childStack
	// constructed, we need to invalidate that

	if (newNode.prevSibling.nextSibling == null &&
	    newNode.prevSibling.childStack != null)
	  {
	    clearStacks(newNode.prevSibling); // not newNode.prevSibling.child!
	  }

	// if our prevSibling was visible, we're going to be
	// visible too.  We just need to figure out where
	// we are in relation to our sibling

	if (newNode.prevSibling.row != -1)
	  {
	    if (newNode.prevSibling.child == null || !newNode.prevSibling.expanded)
	      {
		// we're going to be the next row

		newNode.row = newNode.prevSibling.row + 1;
		rows.insertElementAt(newNode, newNode.row);
	      }
	    else
	      {
		// we're going to be the row after all the kiddies

		// create an empty range.. getVisibleDescendantRange will
		// expand this to represent the span taken by the visible
		// children of newNode.prevSibling.

		Range range = new Range(newNode.prevSibling.child.row,
					newNode.prevSibling.child.row);

		getVisibleDescendantRange(newNode.prevSibling.child, range);

		// and we're next after range.high

		newNode.row = range.high + 1;
		rows.insertElementAt(newNode, newNode.row);
	      }

	    // adjust everybody below

	    for (int i = newNode.row + 1; i < rows.size(); i++)
	      {
		p = (treeNode) rows.elementAt(i);
		p.row = i;
	      }
	  }
	else
	  {
	    newNode.row = -1;
	  }

	newNode.nextSibling = newNode.prevSibling.nextSibling;
	newNode.prevSibling.nextSibling = newNode;

	if (newNode.nextSibling != null)
	  {
	    newNode.nextSibling.prevSibling = newNode;
	  }

	newNode.parent = newNode.prevSibling.parent;
      }
    else if (newNode.parent != null)
      {
	// insert us right before the first child
	// node of our parent

	newNode.nextSibling = newNode.parent.child;

	if (newNode.nextSibling != null)
	  {
	    newNode.nextSibling.prevSibling = newNode;
	  }

	newNode.parent.child = newNode;

	// Figure out our row and adjust all rows below
	// us if we're visible

	if (newNode.parent.row != -1 && newNode.parent.expanded)
	  {
	    newNode.row = newNode.parent.row + 1;
	    rows.insertElementAt(newNode, newNode.row);

	    for (int i = newNode.row + 1; i < rows.size(); i++)
	      {
		p = (treeNode) rows.elementAt(i);
		p.row = i;
	      }
	  }
	else
	  {
	    // oops, not visible

	    newNode.row = -1;
	  }
      }

    if (repaint)
      {
	refresh();
      }
  }

  /**
   * <p>Removes a node from the tree, along with all its children.
   * Any child nodes attached to the node to be deleted will be
   * unlinked from one another.  If you want to be able to re-insert
   * the deleted node elsewhere in the tree, you probably should use
   * moveNode() instead.</p>
   *
   * @param node The node to be removed.
   * @param repaint If true, immediately re-render and refresh the treeCanvas.
   */

  public synchronized void deleteNode(treeNode node, boolean repaint)
  {
    treeNode p;

    /* -- */

    // if we're visible and have children visible, contract to
    // hide those

    if (node.selected)
      {
	unselectNode(node, false);
      }

    if (node.row != -1)
      {
	if (node.expanded)
	  {
	    contractNode(node, false);
	  }
      }

    // if our removal would change the picture for our prevSibling's
    // children in the render algorithm, null out the childStacks
    // to force a recalc

    if (node.nextSibling == null && node.prevSibling != null && node.prevSibling.child != null)
      {
	clearStacks(node.prevSibling);
      }

    // unlink us from the tree

    if (node.parent != null)
      {
	if (node.prevSibling == null)
	  {
	    node.parent.child = node.nextSibling;
	  }
      }
    else
      {
	root = node.nextSibling;
      }

    if (node.prevSibling != null)
      {
	node.prevSibling.nextSibling = node.nextSibling;
      }

    if (node.nextSibling != null)
      {
	node.nextSibling.prevSibling = node.prevSibling;
      }

    // if node.row == -1, the node is not currently visible for
    // display.

    if (node.row != -1)
      {
	rows.removeElementAt(node.row);

	// move everybody else up

	for (int i = node.row; i < rows.size(); i++)
	  {
	    p = (treeNode) rows.elementAt(i);
	    p.row = i;
	  }
      }

    // clean things out for GC.

    breakdownTree(node.child);
    node.tree = null;
    node.row = -1;
    node.parent = null;
    node.childStack = null;
    node.text = null;
    node.menu = null;

    if (repaint)
      {
	refresh();
      }
  }

  /**
   * Moves a node (possibly the root of an extensive subtree) from one
   * location in the tree to another.<br><br>
   *
   * Note that this method is currently implemented in a fairly simplistic
   * manner, using the deleteNode and insertNode primitives, cloning nodes
   * as they are copied into the new location in the tree.  This works reliably,
   * but this might not be the best implementation for moving large sub-trees.
   *
   * @param node The node to be moved.
   * @param parent Parent node to insert this node under, null if this is a top-level node
   * @param insertAfter sibling to insert this node after
   * @param repaint If true, immediately re-render and refresh the treeCanvas after moving the node.
   *
   * @return a reference to a copy of the node in its new location
   */

  public synchronized treeNode moveNode(treeNode node, treeNode parent,
					treeNode insertAfter, boolean repaint)
  {
    if (insertAfter == null && parent == null)
      {
	throw new IllegalArgumentException("error, no insert after or parent set.  Use setRoot to establish root node");
      }

    if (node == null)
      {
	throw new IllegalArgumentException("can't move null");
      }

    if (node == parent)
      {
	throw new IllegalArgumentException("error, attempt made to move node under itself.");
      }

    /* - */

    treeNode newNode, child, oldchild, nextchild;

    /* -- */

    // make a copy of the node to put into the new position.  we clone
    // it so that we can get any additional data-carrying fields held
    // by subclasses of treeNode, even though the resetNode() call
    // immediately after clears the node of all of its previous
    // linkages

    newNode = (treeNode) node.clone();

    newNode.resetNode();
    newNode.parent = parent;
    newNode.prevSibling = insertAfter;

    insertNode(newNode, false);

    if (node.isOpen())
      {
	expandNode(newNode, false, false);
      }

    // now, if the node we moved has children, move them over too.

    oldchild = null;
    child = node.child;

    while (child != null)
      {
	nextchild = child.nextSibling;
	oldchild = moveNode(child, newNode, oldchild, false);
	child = nextchild;
      }

    // now take the node we moved out of the tree

    deleteNode(node, false);

    if (repaint)
      {
	refresh();
      }

    return newNode;
  }

  /**
   * Removes all children of the specified node from the tree.
   *
   * @param node The node whose children should be removed
   * @param repaint If true, immediately re-render and refresh the treeCanvas.
   */

  public synchronized void removeChildren(treeNode node, boolean repaint)
  {
    // if we're visible and have children visible, contract to
    // hide those

    if (node.row != -1)
      {
	if (node.expanded)
	  {
	    contractNode(node, false);
	  }
      }

    breakdownTree(node.child);

    node.child = null;

    if (repaint)
      {
	refresh();
      }
  }

  /**
   * Helper function to break down links in a tree to speed GC.
   * breakdownTree will disassociate all nodes in an unlinked
   * tree, so new nodes will need to be assembled in order
   * to be resubmitted to the tree.
   */

  void breakdownTree(treeNode node)
  {
    if (node == null)
      {
	return;
      }

    node.tree = null;
    node.row = -1;
    node.text = null;
    node.childStack = null;
    node.menu = null;
    node.parent = null;

    breakdownTree(node.child);

    node.child = null;

    breakdownTree(node.nextSibling);

    node.nextSibling = null;
    node.prevSibling = null;

    node.cleanup();
  }

  /**
   * Helper method to force recalculation of childStacks.  This method
   * should be called on the child of a node, not on the node itself,
   * lest clearStacks recurse along the node's siblings.. not that
   * that will cause any great hardship, but it would slow things down
   * a very little bit.
   */

  private void clearStacks(treeNode node)
  {
    if (node == null)
      {
	return;
      }

    if (node.child != null)
      {
	node.childStack = null;
	clearStacks(node.child);
      }

    clearStacks(node.nextSibling);
  }

  /**
   * Get access to the root of the treeCanvas's tree of nodes.
   */

  public treeNode getRoot()
  {
    return root;
  }

  /**
   * Recalculate and redraw the tree.
   */

  public void refresh()
  {
    reShape();
    refreshTree();
  }

  /**
   * Internal diagnostic method, prints the structure of this tree to
   * System.err.
   */

  public void dumpRows()
  {
    treeNode p;

    for (int i = 0; i < rows.size(); i++)
      {
	p = (treeNode) rows.elementAt(i);

	System.err.print("row " + i + ": " + p.text + ", ");

	if (p.parent != null)
	  {
	    System.err.println(p.parent.childStack.size() + "levels in");
	  }
	else
	  {
	    System.err.println("top level");
	  }

	if (p.prevSibling != null)
	  {
	    System.err.print("previous sibling = " + p.prevSibling.getText() + " ");
	  }

	if (p.nextSibling != null)
	  {
	    System.err.println("next sibling = " + p.nextSibling.getText());
	  }
      }
  }

  // internal methods, called by treeCanvas

  /**
   * open the given node
   */

  public void expandNode(treeNode node, boolean repaint)
  {
    expandNode(node, repaint, true);
  }

  /**
   * open the given node
   */

  public synchronized void expandNode(treeNode node, boolean repaint, boolean doCallback)
  {
    int row;
    treeNode p;

    /* -- */

    if (node.expanded)
      {
	return;
      }

    node.expanded = true;

    row = makeDescendantsVisible(node.child, node.row + 1) + 1;

    // adjust everything below the expanded rows

    for (int i = row; i < rows.size(); i++)
      {
	p = (treeNode) rows.elementAt(i);
	p.row = i;
      }

    if (repaint)
      {
	refresh();
      }

    if (doCallback && callback != null)
      {
	callback.treeNodeExpanded(node);
      }
  }

  /**
   * recursive routine to make descendant nodes visible.  will not
   * expand any contracted nodes, but will add any nodes reachable
   * without passing below a contracted node to the visibility
   *
   * @param row the row to match the node to.
   *
   * @return the row number of the last descendant made visible
   */

  private int makeDescendantsVisible(treeNode node, int row)
  {
    while (node != null)
      {
	node.row = row;
	rows.insertElementAt(node, row);

	if (node.child != null && node.expanded)
	  {
	    row = makeDescendantsVisible(node.child, row + 1);
	  }

	node = node.nextSibling;
	row++;
      }

    return row-1;
  }

  /**
   * close the given node
   */

  public void contractNode(treeNode node, boolean repaint)
  {
    contractNode(node, repaint, true);
  }

  /**
   * close the given node
   */

  public synchronized void contractNode(treeNode node, boolean repaint, boolean doCallback)
  {
    Range range;
    treeNode n;

    /* -- */

    if (!node.expanded)
      {
	return;
      }

    node.expanded = false;

    if (debug)
      {
	System.err.println("contractNode: row " + node.row + ", text=" + node.text);
      }

    if (node.child != null)
      {
	range = new Range(node.child.row, node.child.row);
	getVisibleDescendantRange(node.child, range);

	// remove our descendants from the visibility vector

	if (debug)
	  {
	    System.err.println("Before contraction, size = " + rows.size());
	    System.err.println("Contracting rows " + range.low + " through " + range.high);
	  }

	for (int i = range.low; i <= range.high; i++)
	  {
	    n = (treeNode) rows.elementAt(range.low);

	    if (debug)
	      {
		System.err.println("Contracting row " + n.row + ", text=" + n.text);
	      }

	    if (n.selected)
	      {
                moveSelection(node);
	      }

	    rows.removeElementAt(range.low);
	    n.row = -1;
	  }

	// move everybody else up

	for (int i = range.low; i < rows.size(); i++)
	  {
	    n = (treeNode) rows.elementAt(i);

	    if (debug)
	      {
		System.err.println("moving up row " + n.row + " to " + i);
	      }

	    n.row = i;
	  }

	if (debug)
	  {
	    System.err.println("After contraction, size = " + rows.size());
	  }
      }

    if (repaint)
      {
	refresh();
      }

    if (doCallback && callback != null)
      {
	callback.treeNodeContracted(node);
      }
  }

  /**
   * Calculates the rows that are visible at and below node,
   * so that contractNode() can remove all nodes in that
   * range from visibility
   *
   * @param node The node of the subtree we need to examine
   * @param range We encode the range below node in the Range object
   */

  private void getVisibleDescendantRange(treeNode node, Range range)
  {
    treeNode myNode = node;

    while (myNode != null)
      {
	// a row of -1 means we've hit an invisible row, which we
	// never should do.

	if (myNode.row == -1)
	  {
	    throw new RuntimeException("invisible row hit");
	  }

	if (myNode.row > range.high)
	  {
	    range.high = myNode.row;
	  }

	if (myNode.row < range.low)
	  {
	    range.low = myNode.row;
	  }

	// if we have a sibling below us, we don't need to
	// check the kids

	if (myNode.nextSibling != null)
	  {
	    myNode = myNode.nextSibling;
	  }
	else if (myNode.child != null && myNode.expanded)
	  {
	    myNode = myNode.child;
	  }
	else			// we're done going down
	  {
	    break;
	  }
      }
  }

  /**
   * Select a node without issuing a callback to the client.<br><br>
   *
   * Used to implement highlighting during drag-and-drop.
   */

  void transientSelectNode(treeNode node)
  {
    if (node.selected)
      {
	return;
      }

    node.selected = true;

    selectedNode = node;
  }

  /**
   * Used to trigger the default action in the callback.
   */

  void doubleClickNode(treeNode node)
  {
    if (callback != null)
      {
	callback.treeNodeDoubleClicked(node);
      }
  }

  /**
   * Mark a node as selected, issuing a callback to the client
   * reporting the selection.<br><br>
   *
   * This method does not deselect other nodes.
   *
   * @param node The node to select
   */

  public void selectNode(treeNode node)
  {
    if (node.selected)
      {
	return;
      }

    node.selected = true;

    selectedNode = node;

    if (callback != null)
      {
	callback.treeNodeSelected(node);
      }
  }

  /**
   * This method deselects the currently selected node (if any) and
   * selects the paramater node.
   *
   * @param node The node to select
   */

  void moveSelection(treeNode node)
  {
    if (node.selected)
      {
	return;
      }

    if (selectedNode != null)
      {
	unselectNode(selectedNode, true);
      }

    selectNode(node);
  }

  /**
   * Deselect a node without issuing a callback to the client.<br><br>
   *
   * Used to implement highlighting during drag-and-drop.
   *
   * @param node The node to deselect
   */

  void transientUnselectNode(treeNode node)
  {
    if (!node.selected)
      {
	return;
      }

    node.selected = false;

    selectedNode = null;
  }

  /**
   * Deselect a node, issuing a callback to the client
   * reporting the deselection.<br><br>
   *
   * @param anySelected If true, the client will be told
   * that some node will remain selected after this operation
   * is completed
   * @param node The node to deselect
   */

  void unselectNode(treeNode node, boolean anySelected)
  {
    if (!node.selected)
      {
	return;
      }

    node.selected = false;

    selectedNode = null;

    if (callback != null)
      {
	callback.treeNodeUnSelected(node, anySelected);
      }
  }

  /**
   * Deselect all nodes, issuing a callback to the client
   * reporting the deselection.<br><br>
   *
   * @param anySelected If true, the client will be told
   * that some node will remain selected after this operation
   * is completed.
   */

  public void unselectAllNodes(boolean anySelected)
  {
    treeNode node;

    /* -- */

    for (int i = 0; i < rows.size(); i++)
      {
	node = (treeNode) rows.elementAt(i);

	if (node.selected)
	  {
	    unselectNode(node, anySelected);
	  }
      }
  }

  /**
   * Handle notification from popupmenus and from key board navigation
   * actions
   *
   * @param e The event we're receiving as an ActionListener 
   */

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof JMenuItem)
      {
	if (callback == null)
	  {
	    return;
	  }

	if (menuedNode == null)
	  {
	    return;
	  }

	callback.treeNodeMenuPerformed(menuedNode, e);

	menuedNode = null;
	return;
      }


    String actionCommand = e.getActionCommand();

    if (actionCommand == null)
      {
	System.err.println("Null action command from " + e);

	return;
      }

    if (actionCommand.equals("unitdown"))
      {
	if (selectedNode != null)
	  {
	    int rowIndex = selectedNode.row;

	    if (rowIndex + 1 < rows.size())
	      {
		treeNode newNode = (treeNode) rows.elementAt(rowIndex + 1);
		moveSelection(newNode);

		scrollToSelectedRow();

		canvas.render();
		canvas.repaint();
	      }
	  }
      }
    else if (actionCommand.equals("unitup"))
      {
	if (selectedNode != null)
	  {
	    int rowIndex = selectedNode.row;

	    if (rowIndex > 0)
	      {
		treeNode newNode = (treeNode) rows.elementAt(rowIndex - 1);
		moveSelection(newNode);

		scrollToSelectedRow();

		canvas.render();
		canvas.repaint();
	      }
	  }
      }
    else if (actionCommand.equals("scrolldown"))
      {
	if (selectedNode != null)
	  {
	    // move our selection down, scrolling if necessary

	    int rowIndex = selectedNode.row;

	    int jumpIncrement = (canvas.getBottomRow() - canvas.getTopRow()) - 1;

	    int newSelectionIndex = rowIndex + jumpIncrement;

	    if (newSelectionIndex >= rows.size())
	      {
		newSelectionIndex = rows.size() - 1;
	      }

	    moveSelection((treeNode) rows.elementAt(newSelectionIndex));

	    scrollToSelectedRow();
	    canvas.render();
	    canvas.repaint();
	  }
	else if (vbar_visible)
	  {
	    // no selected unit, just scroll

            int adj = vbar.getBlockIncrement();
            int presentValue = vbar.getValue();

            int maxValue = rows.size() * row_height - canvas.getBounds().height;

            if (presentValue + adj > maxValue)
              {
                presentValue = maxValue;
              }
            else
              {
                presentValue = presentValue + adj;
              }

            vbar.setValue(presentValue);
	  }
      }
    else if (actionCommand.equals("scrollup"))
      {
	if (selectedNode != null)
	  {
	    // move our selection down, scrolling if necessary

	    int rowIndex = selectedNode.row;

	    int jumpIncrement = (canvas.getBottomRow() - canvas.getTopRow()) - 1;

	    int newSelectionIndex = rowIndex - jumpIncrement;

	    if (newSelectionIndex <= 0)
	      {
		newSelectionIndex = 0;
	      }

	    moveSelection((treeNode) rows.elementAt(newSelectionIndex));

	    scrollToSelectedRow();
	    canvas.render();
	    canvas.repaint();
	  }
	else if (vbar_visible)
	  {
	    // no selected unit, just scroll

            int adj = vbar.getBlockIncrement();
            int presentValue = vbar.getValue();

            if (presentValue - adj < 0)
              {
                presentValue = 0;
              }
            else
              {
                presentValue = presentValue - adj;
              }

            vbar.setValue(presentValue);
	  }
      }
    else if (actionCommand.equals("scrolltop"))
      {
	moveSelection((treeNode) rows.elementAt(0));
	scrollToSelectedRow();
	canvas.render();
	canvas.repaint();
      }
    else if (actionCommand.equals("scrollbottom"))
      {
	moveSelection((treeNode) rows.elementAt(rows.size() - 1));
	scrollToSelectedRow();
	canvas.render();
	canvas.repaint();
      }
    else if (actionCommand.equals("right"))
      {
	if (selectedNode != null && selectedNode.expandable && !selectedNode.isOpen())
	  {
	    expandNode(selectedNode, true);
	  }
      }
    else if (actionCommand.equals("left"))
      {
	if (selectedNode != null)
          {
            if (selectedNode.expandable && selectedNode.isOpen())
              {
                contractNode(selectedNode, true);
              }
            else if (selectedNode.getParent() != null)
              {
                moveSelection(selectedNode.getParent());
                scrollToSelectedRow();
                canvas.render();
                canvas.repaint();
              }
          }
      }
    else if (actionCommand.equals("enter"))
      {
	if (selectedNode != null)
	  {
	    if (selectedNode.expandable)
	      {
		expandNode(selectedNode, true);
	      }
	    else
	      {
		doubleClickNode(selectedNode);
	      }
	  }
      }
    else if (actionCommand.equals("context"))
      {
        if (selectedNode != null)
          {
            int x = 50;
            int y = selectedNode.row * row_height - canvas.v_offset + (row_height / 2);
            treeNode tempNode = selectedNode;

            while (tempNode != null)
              {
                x = x + canvas.tabStep;
                tempNode = tempNode.getParent();
              }

            MouseEvent me = new MouseEvent(this, 0, System.currentTimeMillis(), 0, x, y, 1, true);
            canvas.popupHandler(me, selectedNode);
          }
      }
  }

  /**
   * This method scrolls the treeCanvas so that the selected row is
   * visible.
   *
   * This method returns true if a scroll (and redraw) was performed,
   * false otherwise.
   */

  private void scrollToSelectedRow()
  {
    if (!vbar_visible)
      {
	return;
      }

    if (selectedNode.row <= canvas.getTopRow())
      {
	vbar.setValue(row_height * selectedNode.row);
      }
    else if (selectedNode.row + 1 >= canvas.getBottomRow())
      {
	vbar.setValue(row_height * (selectedNode.row + 1) - canvas.getBounds().height);
      }
  }

  /**
   * Handles scrollbar events.
   */

  public synchronized void adjustmentValueChanged (AdjustmentEvent e)
  {
    refreshTree();
  }

  /**
   * This method is called when our size is changed.  We need to know
   * this so we can update the scrollbars and what-not.
   */

  public void resize(int x, int y, int width, int height)
  {
    setBounds(x,y,width,height);
  }

  /**
   * Called when we are being resized by our container.
   */

  public synchronized void setBounds(int x, int y, int width, int height)
  {
    if (debug)
      {
	System.err.println("setBounds(): x:" + x + ", y:" + y + ", width:" + width + ", height:" + height);
      }

    super.setBounds(x,y,width,height);
    validate();

    Rectangle rect = new Rectangle(bounding_rect);

    if ((width != bounding_rect.width) ||
	(height != bounding_rect.height))
      {
	bounding_rect.x = x;
	bounding_rect.y = y;
	bounding_rect.width = width;
	bounding_rect.height = height;

	reShape();
      }

    if (width > rect.width || height > rect.height)
      {
	refreshTree();
      }

    if (debug)
      {
	System.err.println("exiting setBounds()");
      }
  }

  // Internal method

  /**
   * This method recalculates the general parameters of our tree's
   * display.  That is, it calculates whether or not we need scroll
   * bars, adds or deletes the scroll bars, and scales the column
   * positions to match the general rendering parameters.
   */

  private void reShape()
  {
    if (debug)
      {
	System.err.println("reShape()");
      }

    // calculate whether we need scrollbars, add/remove them

    adjustScrollbars();

    if (debug)
      {
	System.err.println("exiting reShape()");
      }
  }

  // Internal method

  /**
   * Check to see whether we need scrollbars in our current component size,
   * set the min/max/visible parameters<br><br>
   *
   * This method is intended to be called from reShape().
   */

  private void adjustScrollbars()
  {
    int
      vSize;

    /* -- */

    if (debug)
      {
	System.err.println("adjustScrollbars()");
	System.err.println("canvas.getBounds().width = " + canvas.getBounds().width);
	System.err.println("canvas.getBounds().height = " + canvas.getBounds().height);
      }

    // calculate how tall or table is, not counting any scroll bars.
    // That is, how short can we be before we need to have a vertical
    // scrollbar?

    vSize = row_height * rows.size();

    // calculate whether we need scrollbars

    // check to see if we need a horizontal scrollbar

    canvas.calcWidth();

    if (canvas.getBounds().width < maxWidth)
      {
	if (!hbar_visible)
	  {
	    this.add("South", hbar);
	    hbar.setValue(0);
	    this.doLayout();
	    //	    getParent().doLayout();
	  }
	hbar_visible = true;

	if (debug)
	  {
	    System.err.println("hbar being made visible");
	  }
      }
    else
      {
	if (hbar_visible)
	  {
	    this.remove(hbar);
	    this.doLayout();
	    //	    getParent().doLayout();
	  }
	hbar_visible = false;

	if (debug)
	  {
	    System.err.println("hbar being made INvisible");
	  }

	canvas.h_offset = 0;
      }

    // check to see if we need a vertical scrollbar

    if (canvas.getBounds().height < vSize)
      {
	if (!vbar_visible)
	  {
	    this.add("East", vbar);
	    vbar.setValue(0);
	    this.doLayout();
	  }
	vbar_visible = true;

	if (debug)
	  {
	    System.err.println("vbar being made visible");
	  }
      }
    else
      {
	if (vbar_visible)
	  {
	    this.remove(vbar);
	    this.doLayout();
	  }
	vbar_visible = false;

	if (debug)
	  {
	    System.err.println("vbar being made INvisible");
	  }

	canvas.v_offset = 0;
      }

    // check again to see if we need a horizontal scrollbar..
    // we need to recheck this in case adding our vertical
    // scrollbar squeezed us horizontally enough that we
    // need to put in a horizontal scrollbar

    if (canvas.getBounds().width < maxWidth)
      {
	if (!hbar_visible)
	  {
	    this.add("South", hbar);
	    hbar.setValue(0);
	    this.doLayout();
	  }
	hbar_visible = true;

	if (debug)
	  {
	    System.err.println("hbar being made visible");
	  }
      }
    else
      {
	if (hbar_visible)
	  {
	    this.remove(hbar);
	    this.doLayout();
	  }
	hbar_visible = false;

	if (debug)
	  {
	    System.err.println("hbar being made INvisible");
	  }

	canvas.h_offset = 0;
      }

    // now we've got our scrollbars the way we want them as
    // far as visible vs. non visible.. now we need to
    // see about getting them properly configured

    // Adjust the Vertical Scrollbar's Parameters

    if (vbar_visible && (canvas.getBounds().height != 0))
      {
	int my_total_height = rows.size() * row_height;

	int max_acceptable_value = my_total_height - canvas.getBounds().height;

	if (vbar.getValue() > max_acceptable_value)
	  {
	    vbar.setValues(max_acceptable_value,
			   canvas.getBounds().height,
			   0,
			   my_total_height);
	  }
	else
	  {
	    vbar.setValues(vbar.getValue(),
			   canvas.getBounds().height,
			   0,
			   my_total_height);
	  }

	vbar.setUnitIncrement(row_height);    // we want the up/down buttons to go a line at a time

	vbar.setBlockIncrement(canvas.getBounds().height/2);
      }

    // Adjust the Horizontal Scrollbar's Parameters

    if (hbar_visible && (canvas.getBounds().width != 0))
      {
	int max_acceptable_value = maxWidth - canvas.getBounds().width;

	if (hbar.getValue() > max_acceptable_value)
	  {
	    hbar.setValues(max_acceptable_value,
			   canvas.getBounds().width,
			   0,
			   maxWidth);
	  }
	else
	  {
	    hbar.setValues(hbar.getValue(),
			   canvas.getBounds().width,
			   0,
			   maxWidth);
	  }

	hbar.setBlockIncrement(canvas.getBounds().width / 2);
      }

    if (debug)
      {
	System.err.println("exiting adjustScrollbars()");
      }
  }

  /**
   * Redraw the canvas.
   */

  void refreshTree()
  {
    canvas.render();
    canvas.repaint();
  }

  /**
   * Method to be called after this treeControl is removed from use
   * and visibility.
   */

  public synchronized void dispose()
  {
    if (canvas != null)
      {
        canvas.dispose();
        canvas = null;
      }
  }

  // focusListener methods

  public synchronized void focusGained(FocusEvent e)
  {
    refreshTree();
  }

  public synchronized void focusLost(FocusEvent e)
  {
    refreshTree();
  }

  // MouseWheelListener

  public synchronized void mouseWheelMoved(MouseWheelEvent e)
  {
    if (vbar_visible)
      {
        int adj = vbar.getBlockIncrement();

        int totalScrollAmount = e.getWheelRotation() * adj;

        vbar.setValue(vbar.getValue() + totalScrollAmount);
      }
  }

  private void initializeKeyboardActions()
  {
    InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);

    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), "unitdown");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "unitdown");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), "unitup");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "unitup");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "scrolldown");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "scrollup");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), "scrolltop");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), "scrollbottom");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), "right");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), "left");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "context");

    ActionMap actionMap = getActionMap();

    actionMap.put("unitdown", new treeControlAction("unitdown"));
    actionMap.put("unitup", new treeControlAction("unitup"));
    actionMap.put("scrolldown", new treeControlAction("scrolldown"));
    actionMap.put("scrollup", new treeControlAction("scrollup"));
    actionMap.put("scrolltop", new treeControlAction("scrolltop"));
    actionMap.put("scrollbottom", new treeControlAction("scrollbottom"));
    actionMap.put("right", new treeControlAction("right"));
    actionMap.put("left", new treeControlAction("left"));
    actionMap.put("enter", new treeControlAction("enter"));
    actionMap.put("context", new treeControlAction("context"));
  }

  /**
   * This method is used by the myKeyListener on this tree to advance
   * the selection mark in response to data entry keyboard input from
   * the user.
   */

  private synchronized void tryMatchString(String matchOn)
  {
    if (selectedNode == null)
      {
	selectNode(root);
      }

    treeNode firstNode = selectedNode;

    if (firstNode.expanded && firstNode.child != null)
      {
	firstNode = firstNode.child;
      }

    treeNode matchNode = firstNode;

    Pattern pattern = Pattern.compile("^" + matchOn, Pattern.CASE_INSENSITIVE);

    boolean found = pattern.matcher(matchNode.text).find() &&
      (matchOn.length() > 1 || firstNode != selectedNode) ;

    while (!found)
      {
	if (matchNode.nextSibling != null)
	  {
	    matchNode = matchNode.nextSibling;
	  }
	else if (matchNode.nextSibling == null && matchNode.parent == null)
	  {
	    matchNode = root;
	  }
	else if (matchNode.nextSibling == null && matchNode.parent != null)
	  {
	    matchNode = matchNode.parent.child;
	  }

	if (matchNode == firstNode)
	  {
	    break;
	  }

	found = pattern.matcher(matchNode.text).find();
      }

    if (found)
      {
	moveSelection(matchNode);
	scrollToSelectedRow();
	canvas.render();
	canvas.repaint();
      }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                               treeControlAction

  ----------------------------------------------------------------------------*/

  private class treeControlAction extends javax.swing.AbstractAction
  {
    public treeControlAction(String name)
    {
      super(name);
    }

    public void actionPerformed(ActionEvent e)
    {
      ActionEvent ae = new ActionEvent(treeControl.this, 0, (String) getValue(NAME));
      treeControl.this.actionPerformed(ae);
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                                   myKeyListener

  ----------------------------------------------------------------------------*/

  private class myKeyListener implements KeyListener
  {
    /**
     * The time code (in milliseconds since epoch UTC) before which we
     * need to see the next keystroke, or else we'll clear the key buffer.
     */

    private long matchTime = 0;

    /**
     * Milliseconds interval for continued key buffer accretion.
     */

    private long matchTimeout = 1000;

    /**
     * The key buffer we've built up through sub-second keystroke
     * intervals.
     */

    private StringBuilder matchString = new StringBuilder();

    /* -- */

    private myKeyListener()
    {
    }

    public void keyTyped(KeyEvent e)
    {
      long timeNow = java.lang.System.currentTimeMillis();

      if (timeNow > matchTime)
        {
          matchString.setLength(0);
        }

      if (e.getKeyChar() == '\n' || e.getKeyChar() == '\b')
        {
          return;
        }

      matchTime = timeNow + matchTimeout;
      matchString.append(e.getKeyChar());

      tryMatchString(matchString.toString());
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyPressed(KeyEvent e)
    {
      if (e.isActionKey() ||
	  e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
	  e.getKeyCode() == KeyEvent.VK_DELETE ||
	  e.getKeyCode() == KeyEvent.VK_ENTER)
        {
          matchString.setLength(0);
        }
    }
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      treeCanvas

------------------------------------------------------------------------------*/

/**
 * <p>This class is the actual rendering surface used by the JTree {@link
 * arlut.csd.JTree.treeControl treeControl} class.</p>
 */

class treeCanvas extends JComponent implements MouseListener, MouseMotionListener {

  static final boolean debug = false;
  static final boolean debug2 = false;
  static final Object SPACE = new Object();
  static final Object LINE = new Object();

  /* - */

  private treeControl ctrl;

  private Font font;
  private Color fgColor;
  private Color bgColor;
  private Color lineColor = Color.black;
  private Color dragLineColor = Color.black;

  private FontMetrics fontMetric;
  private int rowHeight;
  private int rowAscent;
  private int rowDescent;
  private int rowLeading;
  private Image[] images;
  private int maxImageHeight;

  private int leftSpacing = 5;
  protected int tabStep = 20;
  private int iconTextSpacing = 3;

  private int boxSize = 10;

  private Image plusBox = null;
  private Image minusBox = null;

  private Image backing;
  private Rectangle backing_rect;
  private Graphics bg;

  private Rectangle boundingBox = null;
  private Point spriteLoc = null;
  private boolean spriteVisible = false;
  private boolean dontdrag = false;
  private boolean drawLine = false;
  private boolean dragSelected = false;
  private Image sprite = null;

  int loBound = 0, hiBound = 0;

  int h_offset;
  int v_offset;

  /* -- */

  /**
   * Constructor
   *
   * @param font Font for text in the Tree Canvas
   * @param fgColor Foreground color for text in the treeCanvas
   * @param bgColor Background color for text in the treeCanvas
   * @param images Array of images to be used in the canvas;  nodes refer to images by index
   */

  public treeCanvas(treeControl ctrl, Font font, Color fgColor, Color bgColor, Image[] images)
  {
    //    super(false);		// no double buffering for us, we'll do it ourselves

    this.ctrl = ctrl;
    this.font = font;
    this.fgColor = fgColor;
    this.bgColor = bgColor;
    this.images = images;

    fontMetric = getFontMetrics(font);
    rowAscent = fontMetric.getMaxAscent();
    rowDescent = fontMetric.getMaxDescent();
    rowHeight = rowAscent + rowDescent;
    rowLeading = fontMetric.getLeading();

    maxImageHeight = 0;

    for (int i=0; i<images.length; i++)
      {
	int temp;

	temp = images[i].getHeight(this);

	if (temp > rowHeight)
	  {
	    rowHeight = temp;
	  }

	if (temp > maxImageHeight)
	  {
	    maxImageHeight = temp;
	  }
      }

    ctrl.row_height = rowHeight + rowLeading; // let our ctrl know the inter-line separation

    addMouseListener(this);
    addMouseMotionListener(this);

    ctrl.maxWidth = ctrl.minWidth;
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(ctrl.minWidth, ctrl.row_height * 5);
  }

  public Dimension getPreferredSize()
  {
    Dimension result;

    if (ctrl.maxWidth < ctrl.minWidth)
      {
	ctrl.maxWidth = ctrl.minWidth;
      }

    if (ctrl.rows.size() < 5)
      {
	result = new Dimension(ctrl.maxWidth + ctrl.borderSpace, ctrl.row_height * 5);
	if (debug)
	  {
	    System.err.println("getPreferredSize:" + result);
	  }
	return result;
      }
    else
      {
	result = new Dimension(ctrl.maxWidth + ctrl.borderSpace, ctrl.row_height * ctrl.rows.size());
	if (debug)
	  {
	    System.err.println("getPreferredSize:" + result);
	  }
	return result;
      }
  }

  public boolean isRequestFocusEnabled()
  {
    return true;
  }

  /**
   * Copy our backing store to our canvas
   */

  public void paint(Graphics g)
  {
    if ((backing == null) ||
	(backing.getWidth(this) < getBounds().width) ||
	(backing.getHeight(this) < getBounds().height))
      {
	render();
      }

    g.drawImage(backing, 0, 0, this);

    if (drawLine)
      {
	int y;

	if (ctrl.dragBelowNode != null)
	  {
	    y = ctrl.dragBelowNode.row * ctrl.row_height - (rowAscent / 2) + (ctrl.row_height / 2);
	  }
	else
	  {
	    y = (ctrl.dragAboveNode.row + 1) * ctrl.row_height - (rowAscent / 2) + (ctrl.row_height / 2);
	  }

	g.setColor(dragLineColor);

	g.drawLine(0, y - v_offset, backing.getWidth(this), y - v_offset);
      }

    if (spriteVisible && (sprite != null) && (spriteLoc != null))
      {
	g.drawImage(sprite, spriteLoc.x, spriteLoc.y, this);
      }
  }

  /**
   * Call our paint method without
   * clearing to prevent flashing
   */

  public void update(Graphics g)
  {
    paint(g);
  }

  /**
   * This method returns the index of the top-most visible row in the
   * table.
   */

  int getTopRow()
  {
    int v_offset;

    if (ctrl.vbar_visible)
      {
	v_offset = ctrl.vbar.getValue();
      }
    else
      {
	v_offset = 0;
      }

    return v_offset / ctrl.row_height;
  }

  /**
   * This method returns the index of the bottom-most visible row in the
   * table.
   */

  int getBottomRow()
  {
    int result = getTopRow() + (getBounds().height / ctrl.row_height) + 1;

    if (result > ctrl.rows.size())
      {
	result = ctrl.rows.size();
      }

    return result;
  }

  /**
   * This is our main rendering routine, which does all the work to
   * generate our tree image, minus the drag/drop sprite effects.
   */

  synchronized void render()
  {
    int
      top_row,
      bottom_row;

    treeNode
      node;

    Vector
      drawVector;

    // we may have our bounds set with a 0 width or height by some
    // clueless layout managers.. homey don't play that game.

    if (debug) {
      System.err.println("Called render");
    }

    if (!isVisible() || getBounds().width <= 0 || getBounds().height <= 0)
      {
	return;
      }

    // We don't want to construct our box images until we're sure that
    // the AWT has set up our peers and is really ready for us to go.

    if (plusBox == null || minusBox == null)
      {
	// buildBoxes will return false if our peer isn't
	// ready for us to create images

	if (!buildBoxes())
	  {
	    return;
	  }
      }

    if (backing == null)
      {
	if (debug)
	  {
	    System.err.println("creating backing image");

	    System.err.println("width = " + getBounds().width);
	    System.err.println("height = " + getBounds().height);
	  }

	backing_rect = new Rectangle(0, 0, getBounds().width, getBounds().height);
	backing = createImage(getBounds().width, getBounds().height);
	bg = backing.getGraphics();
      }
    else if ((backing_rect.width != getBounds().width) ||
	     (backing_rect.height != getBounds().height))
      {
	// need to get a new backing image

	if (debug)
	  {
	    System.err.println("creating new backing image");
	  }

	backing_rect = new Rectangle(0, 0, getBounds().width, getBounds().height);

	backing.flush();	// free old image resources
	backing = createImage(getBounds().width, getBounds().height);
	bg = backing.getGraphics();
      }

    // set our font.

    bg.setFont(font);

    // and draw our tree

    if (ctrl.vbar_visible)
      {
	v_offset = ctrl.vbar.getValue();
      }
    else
      {
	v_offset = 0;
      }

    if (ctrl.hbar_visible)
      {
	h_offset = ctrl.hbar.getValue();
      }
    else
      {
	h_offset = 0;
      }

    bg.setColor(bgColor);
    bg.fillRect(0,0, getBounds().width + 1, getBounds().height + 1);

    top_row = v_offset / ctrl.row_height;
    bottom_row = top_row + (getBounds().height / ctrl.row_height) + 1;

    if (bottom_row >= ctrl.rows.size())
      {
	bottom_row = ctrl.rows.size() - 1;
      }

    for (int i = top_row; i <= bottom_row; i ++)
      {
	node = (treeNode) ctrl.rows.elementAt(i);

	if (node.parent == null)
	  {
	    drawVector = null;
	  }
	else if (node.parent.childStack == null)
	  {
	    node.parent.childStack = new Stack();
	    recursiveGenStack(node.parent, node.parent.childStack);
	    drawVector = node.parent.childStack;
	  }
	else
	  {
	    drawVector = node.parent.childStack;
	  }

	drawRow(node, i, drawVector);
      }
  }

  /**
   * Internal rendering method
   *
   * Should only be called from render(), which provides the
   * thread-synchronized entry point.
   *
   * Generate and record information about the columns to the left of the
   * current node, used by drawRow to properly position each drawn node in
   * the hierarchy
   */

  private void recursiveGenStack(treeNode node, Stack stack)
  {
    if (node == null)
      {
	return;
      }

    recursiveGenStack(node.parent, stack);

    if (node.nextSibling != null)
      {
	stack.push(LINE);
      }
    else
      {
	stack.push(SPACE);
      }

    return;
  }

  /**
   * drawRow() is the internal rendering method for each row of the
   * treeControl.
   *
   * Should only be called from render(), which provides the
   * thread-synchronized entry point
   *
   * @param node The treeNode corresponding to the row to be drawn
   * @param row The row's number in the overall tree's list of rows
   * @param s A Stack containing LINE and SPACE objects that need to
   * precede the icon and text for this node.  We use this draw Stack
   * to properly render the containment graphics for this row.
   */

  private void drawRow(treeNode node, int row, Vector s)
  {
    int
      horizLine,		// y pos of this row's horizontal connecting line
      x1,			// working variable
      x2,			// working variable
      imageIndex;

    Image temp;

    /* -- */

    if (debug)
      {
	if (s != null)
	  {
	    System.err.println("Drawing node " + node.text + ", row = " +
			       row + "(" + node.row + "), " + s.size() + " steps in");
	  }
	else
	  {
	    System.err.println("Drawing node " + node.text + ", row = " +
			       row + "(" + node.row + "), top level");
	  }
      }

    // this equation, especially the .75 figure, is a fudge factor to try to
    // get the midline and the text to line up reasonably well

    horizLine = (ctrl.row_height * row) +
      rowLeading + (int) (rowAscent*.75) - v_offset;

    if (plusBox == null || minusBox == null)
      {
	// buildBoxes will return false if our peer isn't
	// ready for us to create images

	if (!buildBoxes())
	  {
	    return;
	  }
      }

    x1 = x2 = leftSpacing + (minusBox.getWidth(this) / 2) - h_offset;

    bg.setColor(lineColor);

    if (s != null)
      {
	for (int i = 0; i < s.size(); i++)
	  {
	    x1 = x2;
	    x2 = x1 + tabStep;

	    if (s.elementAt(i) == SPACE)
	      {
		continue;
	      }

	    // Draw our vertical line up to our upstairs neighbor

	    if (row > 0)
	      {
		bg.drawLine(x1, ctrl.row_height * row - v_offset, x1, horizLine);
	      }

	    // If appropriate, draw our vertical line down to where we'll meet
	    // with our downstairs neighbor

	    bg.drawLine(x1, horizLine, x1, ctrl.row_height * (row + 1) - v_offset);
	  }
      }

    x1 = x2; x2 = x1 + tabStep;

    if (node.parent != null || node.prevSibling != null)
      {
	bg.drawLine(x1, ctrl.row_height * row - v_offset, x1, horizLine);
      }

    if (node.nextSibling != null)
      {
	bg.drawLine(x1, horizLine, x1, ctrl.row_height * (row + 1) - v_offset);
      }

    // Now draw from our connecting point over to where we'll draw our icon

    bg.drawLine(x1, horizLine, x2, horizLine);

    if (node.expandable || node.child != null)
      {
	if (node.expanded)
	  {
	    // draw our minus box over the intersection

	    node.boxX1 = x1 - (minusBox.getWidth(this)/2);
	    node.boxX2 = node.boxX1 + minusBox.getWidth(this);

	    node.boxY1 = horizLine - (minusBox.getHeight(this)/2);
	    node.boxY2 = node.boxY1 + minusBox.getHeight(this);

	    bg.drawImage(minusBox,
			 node.boxX1,
			 node.boxY1,
			 this);
	  }
	else
	  {
	    // draw our plus box over the intersection

	    node.boxX1 = x1 - (plusBox.getWidth(this)/2);
	    node.boxX2 = node.boxX1 + plusBox.getWidth(this);

	    node.boxY1 = horizLine - (plusBox.getHeight(this)/2);
	    node.boxY2 = node.boxY1 + plusBox.getHeight(this);

	    bg.drawImage(plusBox,
			 node.boxX1,
			 node.boxY1,
			 this);
	  }
      }

    x1 = x2;

    // (x1, horizLine) is the point we want to center our icon on

    // if we have an open child under us, draw down

    if (node.expanded && node.child != null)
      {
	bg.drawLine(x1, horizLine, x1, ctrl.row_height * (row + 1) - v_offset);
      }

    imageIndex = node.expanded ? node.openImage : node.closedImage;

    if (imageIndex >= 0 && imageIndex < images.length)
      {
	temp = images[imageIndex];

	bg.drawImage(temp,
		     x1 - (temp.getWidth(this)/2),
		     horizLine - (temp.getHeight(this)/2),
		     this);

	x1 = x1 + temp.getWidth(this)/2 + iconTextSpacing;
      }

    x2 = x1 + fontMetric.stringWidth(node.text);

    if (node.selected)
      {
	if (ctrl.isFocusOwner())
	  {
	    bg.setColor(fgColor);
	  }
	else
	  {
	    Color transparentFgColor = new Color(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue(), 64);

	    bg.setColor(transparentFgColor);
	  }

	bg.fillRect(x1,
		    (horizLine - rowAscent/2),
		    x2-x1+1,
		    rowAscent + rowDescent + rowLeading + 1);

	bg.setColor(bgColor);
      }
    else if (spriteVisible && node == ctrl.dragNode)
      {
	bg.setColor(java.awt.Color.blue);

	bg.fillRect(x1,
		    (horizLine - rowAscent/2),
		    x2-x1+1,
		    rowAscent + rowDescent + rowLeading + 1);

	bg.setColor(bgColor);
      }
    else
      {
	bg.setColor(fgColor);
      }

    bg.drawString(node.text,
		  x1,
		  horizLine + rowAscent/2);

    if (x2 > ctrl.maxWidth)
      {
	if (debug)
	  {
	    System.err.println("Width overrun in drawRow for node " + node);
	  }
      }
  }

  /**
   * This method calculates the maximum width required for the tree in
   * its current state.
   */

  int calcWidth()
  {
    int
      x,
      imageIndex;

    treeNode node;
    Vector drawVector;

    /* -- */

    if (debug)
      {
        System.err.println("entering calcWidth()");
      }

    ctrl.maxWidth = ctrl.minWidth;

    synchronized (ctrl.rows)
      {
        for (int i = 0; i < ctrl.rows.size(); i ++)
          {
            node = (treeNode) ctrl.rows.elementAt(i);

            if (debug && node.row != i)
              {
                System.err.println("ASSERT! node's row is " + node.row + ", not " + i);
              }

            if (node.parent == null)
              {
                drawVector = null;
              }
            else if (node.parent.childStack == null)
              {
                node.parent.childStack = new Stack();
                recursiveGenStack(node.parent, node.parent.childStack);
                drawVector = node.parent.childStack;
              }
            else
              {
                drawVector = node.parent.childStack;
              }

            x = leftSpacing + (boxSize / 2) + tabStep;

            if (drawVector != null)
              {
                x = x + tabStep * drawVector.size();
              }

            imageIndex = node.expanded ? node.openImage : node.closedImage;

            x = x + images[imageIndex].getWidth(this)/2 + iconTextSpacing + fontMetric.stringWidth(node.text);

            if (x > ctrl.maxWidth)
              {
                if (debug)
                  {
                    System.err.println("\nmax row width found at " + node.text);
                    System.err.println("old width = " + ctrl.maxWidth);
                    System.err.println("new width = " + x);
                    System.err.println("----------");
                  }

                ctrl.maxWidth = x;
              }
	  }
      }

    return ctrl.maxWidth;
  }

  /**
   * Generate our +/- box images
   */

  private boolean buildBoxes()
  {
    Graphics g;

    /* -- */

    int midpoint = boxSize/2;
    int p1 = 1;
    int p2 = boxSize - 1;

    plusBox = this.createImage(boxSize,boxSize);

    // it's possible we're being called before our peer is really
    // ready for us to create images.. if so, we'll just return
    // false and we'll try again at a later point

    if (plusBox == null)
      {
	return false;
      }

    g = plusBox.getGraphics();

    // how do we know what color we're going to clear this to?

    g.setColor(bgColor);
    g.fillRect(0,0, boxSize, boxSize);

    g.setColor(Color.black);
    g.drawRect(p1, p1, p2-1, p2-1);

    g.setColor(Color.blue);
    g.drawLine(p1 + 2, midpoint, p2 - 2, midpoint);

    g.drawLine(midpoint, p1 + 2, midpoint, p2 - 2);

    minusBox = createImage(boxSize, boxSize);
    g = minusBox.getGraphics();

    g.setColor(bgColor);
    g.fillRect(0,0, boxSize, boxSize);

    g.setColor(Color.black);
    g.drawRect(p1, p1, p2 - 1, p2 - 1);

    g.setColor(Color.blue);
    g.drawLine(p1 + 2, midpoint, p2 - 2, midpoint);

    return true;
  }

  // mouseListener methods

  public void mouseClicked(MouseEvent e)
  {
    treeNode node;
    int row;
    int x, y;

    /* -- */

    ctrl.requestFocus();

    x = e.getX();
    y = e.getY();

    row = (y + v_offset) / ctrl.row_height;

    try
      {
	node = (treeNode) ctrl.rows.elementAt(row);
      }
    catch (ArrayIndexOutOfBoundsException ex)
      {
	return;
      }

    if (debug2)
      {
	System.err.println("Clicked on node " + node.getText() + ", row " + row);

	System.err.println("Location: " + x + ", " + y);

	System.err.println("v_offset = " + v_offset);

	if (node.expandable)
	  {
	    System.err.println("Box for this row currently at (" +
			       node.boxX1 + "," + node.boxY1 + "-" +
			       node.boxX2 + "," + node.boxY2 + ")");
	  }
      }

    // if they double clicked outside of a box, open/close the node

    if (e.getClickCount() >= 2)
      {
	if ((node.expandable || node.child != null) &&
	    (x < node.boxX1 || y < node.boxY1 || x > node.boxX2 || y > node.boxY2))
	  {
	    if (node.expanded)
	      {
		ctrl.contractNode(node, true);
	      }
	    else
	      {
		ctrl.expandNode(node, true);
	      }
	    return;
	  }
	else if (!node.expandable)
	  {
	    ctrl.doubleClickNode(node);
	  }
      }
    else
      {
	if (debug2)
	  {
	    System.err.println("Click in row " + row);
	  }

	if ((node.expandable || node.child != null) &&
	    (x >= node.boxX1 && y >= node.boxY1 && x <= node.boxX2 && y <= node.boxY2))
	  {
	    // mousePressed will have taken care of this for us.

	    return;
	  }
      }
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

  public void mousePressed(MouseEvent e)
  {
    treeNode node;
    int row;
    int x, y;

    /* -- */

    x = e.getX();
    y = e.getY();

    row = (y + v_offset) / ctrl.row_height;

    try
      {
	node = (treeNode) ctrl.rows.elementAt(row);
      }
    catch (ArrayIndexOutOfBoundsException ex)
      {
	// out of range, deselect all rows

	if (debug)
	  {
	    System.err.println("Hey!  received out of bounds exception, row = " + row);
	  }

	ctrl.unselectAllNodes(false);
	render();
	repaint();
	return;
      }

    // we don't want to take a right or middle mouse button click as anything
    // but a pop-up driver

    if (((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) ||
	((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0))
      {
	if (!spriteVisible)
	  {
	    ctrl.unselectAllNodes(true); // another node is being selected
	    ctrl.selectNode(node);
	    render();
	    repaint();

	    if (e.isPopupTrigger())
	      {
		popupHandler(e, node);
	      }
	  }

	return;
      }

    if ((node.expandable || node.child != null) &&
	(x >= node.boxX1 && y >= node.boxY1 && x <= node.boxX2 && y <= node.boxY2))
      {
	if (node.expanded)
	  {
	    ctrl.contractNode(node, true);
	  }
	else
	  {
	    ctrl.expandNode(node, true);
	  }

	return;
      }

    // remember that the mouse was last pressed on this node if
    // we subsequently receive mouseDragged events.

    if (ctrl.dragNode == null)
      {
	try
	  {
	    ctrl.dragNode = (treeNode) ctrl.rows.elementAt(row);

	    if (debug2)
	      {
		System.err.println("mousePressed(): I'm setting dragNode to " + ctrl.dragNode.getText() + "!!!");
	      }
	  }
	catch (ArrayIndexOutOfBoundsException ex)
	  {
	    // ignore
	  }
      }

    if (debug2)
      {
	System.err.println("Press in row " + row);
      }

    // we'll do the selection/popup logic here as well, just in case
    // we run on some weird system where the popup trigger is on
    // the main button without the CTRL or META modifiers (which
    // are equivalent to middle and right mouse buttons on AWT)

    if (!node.selected && !spriteVisible)
      {
	ctrl.unselectAllNodes(true); // another node is being selected
	ctrl.selectNode(node);

	render();
	repaint();
      }

    if (e.isPopupTrigger() && !spriteVisible)
      {
	popupHandler(e, node);
	return;
      }
  }

  public void mouseReleased(MouseEvent e)
  {
    treeNode node;
    int row;

    /* -- */

    row = (e.getY() + v_offset) / ctrl.row_height;

    if (dragSelected && (ctrl.dragNode != ctrl.dragOverNode))
      {
	ctrl.dCallback.iconDragDrop(ctrl.dragNode, ctrl.dragOverNode);
      }
    else if (drawLine && (ctrl.dragNode != ctrl.dragAboveNode) && (ctrl.dragNode != ctrl.dragBelowNode))
      {
	ctrl.dCallback.dragLineRelease(ctrl.dragNode, ctrl.dragAboveNode, ctrl.dragBelowNode);
      }

    drawLine = false;
    dragSelected = false;

    if (spriteVisible)
      {
	spriteVisible = false;
	this.setCursor(Cursor.getDefaultCursor());
	ctrl.refreshTree();
      }

    dontdrag = false;
    ctrl.dragNode = null;

    if (debug2)
      {
	System.err.println("Released in row " + row);
      }

    try
      {
	node = (treeNode) ctrl.rows.elementAt(row);
      }
    catch (ArrayIndexOutOfBoundsException ex)
      {
	// out of range
	return;
      }

    if (node == null)
      {
	throw new RuntimeException("null node");
      }

    // Win32 does popup trigger on mouse release

    if (e.isPopupTrigger() && !spriteVisible)
      {
	popupHandler(e, node);
	return;
      }
  }

  public void mouseDragged(MouseEvent e)
  {
    treeNode n = null;
    int row;
    boolean reRender = false;

    /* -- */

    if (dontdrag || ctrl.dragNode == null)
      {
	return;
      }

    row = (e.getY() + v_offset) / ctrl.row_height;

    if (debug2)
      {
	System.err.println("Dragging over row " + row);
      }

    try
      {
	n = (treeNode) ctrl.rows.elementAt(row);
      }
    catch (ArrayIndexOutOfBoundsException ex)
      {
	// out of range.. go ahead and allow the
	// drag down below, but don't do any
	// further processing

	if (spriteVisible)
	  {
	    repaint();
	  }

	return;
      }

    if (debug2)
      {
	System.err.println("Dragging over Node: " + n.getText());
      }

    // **
    //   Check for drag start
    // **

    if (!spriteVisible && (ctrl.dragMode != treeControl.DRAG_NONE))
      {
	if (ctrl.dCallback.startDrag(ctrl.dragNode))
	  {
	    spriteVisible = true;

	    if (debug2)
	      {
		System.err.println("mouseDragged(): I'm setting dragNode to " + ctrl.dragNode.getText() + "!!!");
	      }

	    this.setCursor(Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR));
	    sprite = images[n.closedImage];
	    spriteLoc = e.getPoint();

	    //		System.err.println("Created sprite");

	    if ((ctrl.dragMode & ctrl.DRAG_ICON) != 0)
	      {
		ctrl.oldNode = ctrl.dragNode;
	      }
	  }
	else
	  {
	    dontdrag = true;
	  }
      }

    // if we are dragging, check out what we want to be doing

    if (spriteVisible)
      {
	spriteLoc = e.getPoint();

	if (debug)
	  {
	    System.err.println("pos = " + spriteLoc.y);
	  }

	// do drag mode specific processing

	if ((ctrl.dragMode & ctrl.DRAG_ICON) != 0)
	  {
	    // we only want to do the drag over test if we're not over
	    // the same node we were last time

	    if (!dragSelected || ctrl.oldNode != n)
	      {
		if (ctrl.dCallback.iconDragOver(ctrl.dragNode, n))
		  {
		    if (debug2)
		      {
			System.err.println("** treeControl: iconDragOver <" + n.getText() + "> returned true");
		      }

		    dragSelected = true;
		    ctrl.dragOverNode = n;
		    drawLine = false;

		    ctrl.transientSelectNode(n);
		    reRender = true;
		  }
		else
		  {
		    if (debug2)
		      {
			System.err.println("** treeControl: iconDragOver <" + n.getText() + "> returned false");
		      }

		    dragSelected = false;
		    ctrl.dragOverNode = null;
		    ctrl.transientUnselectNode(n);
		  }

		if ((ctrl.oldNode != n) && (ctrl.oldNode != null))
		  {
		    ctrl.transientUnselectNode(ctrl.oldNode);
		  }

		ctrl.oldNode = n;
	      }
	    else
	      {
		if (dragSelected)
		  {
		    if (debug2)
		      {
			System.err.println("** still dragging over selected node " + n.getText());
		      }

		    drawLine = false;
		  }
		else
		  {
		    if (debug2)
		      {
			System.err.println("** still dragging over unselected node " + n.getText());
		      }
		  }
	      }
	  }

	// okay, we've done determinations on dragging the icon onto a row
	// if necessary.. now check to see if we are also in a position
	// to place a line between rows.. if we are, we'll essentially forget
	// about the dragOver if we're not near the center of the row.

	if ((ctrl.dragMode & ctrl.DRAG_LINE) != 0)
	  {
	    treeNode aboveNode, belowNode;

	    /* -- */

	    // if the mouse is below the midline of row n, n will be above the
	    // line we're calculating

	    if (spriteLoc.y > (n.row * ctrl.row_height + (ctrl.row_height / 2) - v_offset))
	      {
		aboveNode = n;

		if (debug2)
		  {
		    System.err.println("aboveNode is " + aboveNode.getText());
		  }

		try
		  {
		    belowNode = (treeNode) ctrl.rows.elementAt(aboveNode.row + 1);

		    if (debug2)
		      {
			System.err.println("belowNode is " + belowNode.getText());
		      }
		  }
		catch (ArrayIndexOutOfBoundsException ex)
		  {
		    belowNode = null;
		  }
	      }
	    else
	      {
		belowNode = n;

		if (debug2)
		  {
		    System.err.println("belowNode is " + belowNode.getText());
		  }

		try
		  {
		    aboveNode = (treeNode) ctrl.rows.elementAt(belowNode.row - 1);

		    if (debug2)
		      {
			System.err.println("aboveNode is " + aboveNode.getText());
		      }
		  }
		catch (ArrayIndexOutOfBoundsException ex)
		  {
		    aboveNode = null;
		  }
	      }

	    // if we've moved into a new region, update the ctrl's notion
	    // of our position

	    if ((aboveNode != ctrl.dragAboveNode) || (belowNode != ctrl.dragBelowNode))
	      {
		if (debug)
		  {
		    System.err.println("Setting hiBound/loBound");
		  }

		hiBound = n.row * ctrl.row_height + (ctrl.row_height / 6) - v_offset;
		loBound = (n.row + 1) * ctrl.row_height - (ctrl.row_height / 6) - v_offset;

		ctrl.dragAboveNode = aboveNode;
		ctrl.dragBelowNode = belowNode;
	      }

	    if (debug)
	      {
		System.err.println("hiBound for node " + n.getText() + " is " + hiBound);
		System.err.println("loBound for node " + n.getText() + " is " + loBound);
		System.err.println("(spriteLoc.y == " + spriteLoc.y + ")");
	      }

	    if (!dragSelected ||
		((spriteLoc.y < hiBound) || (spriteLoc.y > loBound)))
	      {
		if (debug2)
		  {
		    if (dragSelected)
		      {
			System.err.println("dragSelected is true.. mixed mode check");
		      }
		    else
		      {
			System.err.println("dragSelected is false.. just tweening");
		      }

		    System.err.println("Checking for dragLineTween");
		  }

		if (ctrl.dCallback.dragLineTween(ctrl.dragNode, aboveNode, belowNode))
		  {
		    if (debug2)
		      {
			System.err.println("dragLineTween affirm");
		      }

		    drawLine = true;
		    dragSelected = false;

		    if (ctrl.dragOverNode != null)
		      {
			ctrl.transientUnselectNode(ctrl.dragOverNode);
		      }

		    reRender = true;
		  }
		else
		  {
		    drawLine = false;
		  }
	      }
	  }
	else
	  {
	    drawLine = false;
	  }
      }

    if (reRender)
      {
	if (debug)
	  {
	    System.err.println("treeControl: ** Rendering");
	  }

	render();
      }

    if (reRender || spriteVisible)
      {
	repaint();
      }
  }

  public void mouseMoved(MouseEvent e)
  {
  }

  /**
   * popup dispatcher
   */

  void popupHandler(MouseEvent e, treeNode node)
  {
    if (debug2)
      {
	System.err.println("popupHandler");
      }

    ctrl.menuedNode = node;

    if (node.menu != null)
      {
	if (debug2)
	  {
	    System.err.println("node popup");
	  }

	node.menu.show(this, e.getX(), e.getY());
      }
    else if (ctrl.menu != null)
      {
	if (debug2)
	  {
	    System.err.println("ctrl popup");
	  }

	ctrl.menu.show(this, e.getX(), e.getY());
      }
  }

  /**
   * Method to be called after this treeCanvas is removed from use and
   * visibility.
   */

  public synchronized void dispose()
  {
    if (bg != null)
      {
        bg.dispose();
        bg = null;
      }

    if (plusBox != null)
      {
        plusBox.flush();
        plusBox = null;
      }

    if (minusBox != null)
      {
        minusBox.flush();
        minusBox = null;
      }

    if (backing != null)
      {
        backing.flush();
        backing = null;
      }

    if (images != null)
      {
        for (int i = 0; i < images.length; i++)
          {
            if (images[i] != null)
              {
                images[i].flush();
                images[i] = null;
              }
          }
      }

    if (sprite != null)
      {
        sprite.flush();
        sprite = null;
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Range

------------------------------------------------------------------------------*/

/**
 * This class is used as a simple struct to hold scratch information
 * for the {@link arlut.csd.JTree.treeControl treeControl} rendering
 * logic.
 */

class Range {

  int low;
  int high;

  Range(int low, int high)
  {
    this.low = low;
    this.high = high;
  }
}
