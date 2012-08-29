/*
 * @(#)JTreeTable.java  1.2 98/10/27
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 * 
 * The Swing Connection has given permission for the following code
 * to be freely used, modified and redistributed. However, since it
 * is example code, Sun does not support it and warns that it has not 
 * been as rigorously tested as Sun's product code.
 *
 * For further details, see The Swing Connection's Copyright Notice and 
 * Disclaimer at http://java.sun.com/products/jfc/tsc/page_2/page_2.html
 *
 * The articles upon which the TreeTable idea is based are located at 
 * http://java.sun.com/products/jfc/tsc/tech_topics/tech_topics.html
 */

package arlut.csd.ganymede.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * This example shows how to create a simple JTreeTable component, 
 * by using a JTree as a renderer (and editor) for the cells in a 
 * particular column in the JTable.  
 *
 * @version $Id$
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public class JTreeTable extends JTable {
  /** A subclass of JTree. */
  protected TreeTableCellRenderer tree;

  public JTreeTable(TreeTableModel treeTableModel)
  {
    super();

    /* Keep the JTable from scrolling inappropriately */
    setAutoscrolls(false);

    // Create the tree. It will be used as a renderer and editor. 
    tree = new TreeTableCellRenderer(treeTableModel);

    // Install a adapter wrapper around our treeTableModel, to
    // represent the visible rows in the tree.
    super.setModel(new TreeTableModelAdapter(treeTableModel, tree));

    // Install the tree editor renderer and editor. 
    setDefaultRenderer(TreeTableModel.class, tree); 
    setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());

    // No grid.
    setShowGrid(false);

    // No intercell spacing
    setIntercellSpacing(new Dimension(0, 0));   

    // Increase the height of the rows to match the gclient tree. 
    setRowHeight(getRowHeight()+5);

    // Make the tree and table row heights the same. 
    tree.setRowHeight(getRowHeight());

    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);  
    tree.putClientProperty("JTree.lineStyle", "Angled");      
  }

  /**
   * Overridden to message super and forward the method to the tree.
   * Since the tree is not actually in the component hieachy it will
   * never receive this unless we forward it in this manner.
   */
  public void updateUI()
  {
    super.updateUI();

    if (tree != null)
      {
        tree.updateUI();
      }

    // Use the tree's default foreground and background colors in the
    // table. 
    LookAndFeel.installColorsAndFont(this, "Tree.background",
                                     "Tree.foreground", "Tree.font");
  }

  /**
   * Determines if the specified column contains hierarchical nodes.
   *
   * @param column zero-based index of the column
   * @return true if the class of objects in the specified column implement
   * the {@link javax.swing.tree.TreeNode} interface; false otherwise.
   */
  public boolean isHierarchical(int column)
  {
    return TreeTableModel.class.isAssignableFrom(getColumnClass(column));
  }

  /**
   * Overriden to invoke repaint for the particular location if
   * the column contains the tree. This is done as the tree editor does
   * not fill the bounds of the cell, we need the renderer to paint
   * the tree in the background, and then draw the editor over it.
   * You should not need to call this method directly.
   *
   * {@inheritDoc}
   */
  public boolean editCellAt(int row, int column, EventObject e)
  {
    expandOrCollapseNode(e);    // RG: Fix Issue 49!

    boolean canEdit = super.editCellAt(row, column, e);

    // we assume that column zero is the tree column

    if (canEdit && column == 0)
      {
        repaint(getCellRect(row, column, false));
      }
    return canEdit;
  }

  private void expandOrCollapseNode(EventObject e)
  {
    if (e instanceof MouseEvent)
      {
        MouseEvent me = (MouseEvent) e;
        // If the modifiers are not 0 (or the left mouse button),
        // tree may try and toggle the selection, and table
        // will then try and toggle, resulting in the
        // selection remaining the same. To avoid this, we
        // only dispatch when the modifiers are 0 (or the left mouse
        // button).

        if (me.getModifiers() == 0 || me.getModifiers() == InputEvent.BUTTON1_MASK)
          {
            final int count = getColumnCount();

            for (int i = count - 1; i >= 0; i--)
              {
                if (isHierarchical(i))
                  {
                    int savedHeight = tree.getRowHeight();
                    tree.setRowHeight(getRowHeight());
                    MouseEvent pressed = new MouseEvent
                      (tree,
                       me.getID(),
                       me.getWhen(),
                       me.getModifiers(),
                       me.getX() - getCellRect(0, i, false).x,
                       me.getY(),
                       me.getClickCount(),
                       me.isPopupTrigger());

                    tree.dispatchEvent(pressed);
                    // For Mac OS X, we need to dispatch a MOUSE_RELEASED as well
                    MouseEvent released = new MouseEvent
                      (tree,
                       java.awt.event.MouseEvent.MOUSE_RELEASED,
                       pressed.getWhen(),
                       pressed.getModifiers(),
                       pressed.getX(),
                       pressed.getY(),
                       pressed.getClickCount(),
                       pressed.isPopupTrigger());

                    tree.dispatchEvent(released);
                    tree.setRowHeight(savedHeight);
                    break;
                  }
              }
          }
      }
  }
  
  /* Workaround for BasicTableUI anomaly. Make sure the UI never tries to 
   * paint the editor. The UI currently uses different techniques to 
   * paint the renderers and editors and overriding setBounds() below 
   * is not the right thing to do for an editor. Returning -1 for the 
   * editing row in this case, ensures the editor is never painted. 
   */
  public int getEditingRow()
  {
    return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 :
      editingRow;  
  }

  /**
   * Overridden to pass the new rowHeight to the tree.
   */
  public void setRowHeight(int rowHeight)
  {
    super.setRowHeight(rowHeight); 

    if (tree != null && tree.getRowHeight() != rowHeight)
      {
        tree.setRowHeight(getRowHeight()); 
      }
  }

  /**
   * Returns the tree that is being shared between the model.
   */
  public JTree getTree()
  {
    return tree;
  }

  /**
   * A TreeCellRenderer that displays a JTree.
   */

  public class TreeTableCellRenderer extends JTree implements TableCellRenderer {

    /** Last table/tree row asked to renderer. */
    protected int visibleRow;

    public TreeTableCellRenderer(TreeModel model)
    {
      super(model); 
    }

    /**
     * updateUI is overridden to set the colors of the Tree's renderer
     * to match that of the table.
     */
    public void updateUI()
    {
      super.updateUI();
      // Make the tree's cell renderer use the table's cell selection
      // colors. 
      TreeCellRenderer tcr = getCellRenderer();

      if (tcr instanceof DefaultTreeCellRenderer)
        {
          DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer)tcr); 
          // For 1.1 uncomment this, 1.2 has a bug that will cause an
          // exception to be thrown if the border selection color is
          // null.
          // dtcr.setBorderSelectionColor(null);
          dtcr.setTextSelectionColor(UIManager.getColor("Table.selectionForeground"));

          dtcr.setBackgroundSelectionColor(UIManager.getColor("Table.selectionBackground"));

          dtcr.setBackgroundNonSelectionColor(Color.white);
        }
    }

    /**
     * Sets the row height of the tree, and forwards the row height to
     * the table.
     */
    public void setRowHeight(int rowHeight)
    {
      if (rowHeight > 0)
        {
          super.setRowHeight(rowHeight); 

          if (JTreeTable.this != null &&
              JTreeTable.this.getRowHeight() != rowHeight)
            {
              JTreeTable.this.setRowHeight(getRowHeight()); 
            }
        }
    }
    
    /**
     * This is overridden to set the height to match that of the JTable.
     */
    public void setBounds(int x, int y, int w, int h)
    {
      super.setBounds(x, 0, w, JTreeTable.this.getHeight());
    }

    /**
     * Sublcassed to translate the graphics such that the last visible
     * row will be drawn at 0,0.
     */
    public void paint(Graphics g)
    {
      g.translate(0, -visibleRow * getRowHeight());
      super.paint(g);
    }

    /**
     * TreeCellRenderer method. Overridden to update the visible row.
     */
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row, int column)
    {
      visibleRow = row;
      return this;
    }
  }


  /**
   * TreeTableCellEditor implementation. Component returned is the
   * JTree.
   */
  public class TreeTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int r, int c)
    {
      return tree;
    }

    /**
     * Overridden to return false, and if the event is a mouse event
     * it is forwarded to the tree.
     *
     * The behavior for this is debatable, and should really be offered
     * as a property. By returning false, all keyboard actions are
     * implemented in terms of the table. By returning true, the
     * tree would get a chance to do something with the keyboard
     * events. For the most part this is ok. But for certain keys,
     * such as left/right, the tree will expand/collapse where as
     * the table focus should really move to a different column. Page
     * up/down should also be implemented in terms of the table.
     * By returning false this also has the added benefit that clicking
     * outside of the bounds of the tree node, but still in the tree
     * column will select the row, whereas if this returned true
     * that wouldn't be the case.
     *
     * By returning false we are also enforcing the policy that
     * the tree will never be editable (at least by a key sequence).
     */
    public boolean isCellEditable(EventObject e)
    {
      return true;
    }
  }
}
