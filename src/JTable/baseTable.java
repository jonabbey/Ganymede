/*

  baseTable.java

  A JDK 1.1 table AWT component.

  Copyright (C) 1997, 1998, 1999  The University of Texas at Austin.

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

  Created: 29 May 1996
  Version: $Revision: 1.45 $
  Last Mod Date: $Date: 1999/01/16 01:38:23 $
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.JTable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       baseTable

------------------------------------------------------------------------------*/

/**
 * <p>baseTable is an AWT table component, supporting a wide variety
 * of table displays based on an underlying grid model.  Columns and
 * rows may be separated by lines or not, and baseTable supports a
 * variety of vertical filling behaviors for those cases in which the
 * baseTable doesn't contain enough rows for its size.</p>
 *
 * <p>baseTable supports a hierarchy of attribute sets for its rendering.
 * An attribute set comprises a font, justification, and foreground /
 * background colors.  Such an attribute set can be assigned to the
 * headers of the table, to the body of the table as a whole, to the individual
 * columns of the table, and to the individual cells themselves.</p>
 *
 * <p>The baseTable supports intelligent scrollbars, dynamic column sizing,
 * user adjustable columns, and popup menus.</p>
 *
 * <p>baseTable is not yet a full featured table/grid component.
 * baseTable currently doesn't support the selection of individual
 * cells and/or columns, nor does it support cut and paste or in-cell
 * editing.  These are not currently priorities for baseTable, and
 * the current code is not designed for such applications.</p>
 *
 * <p>baseTable is intended to provide a flexible row-oriented report
 * table in which the user can select a row for action by a program that
 * incorporates a baseTable. The ability to select rows can be turned off,
 * in which case baseTable becomes strictly a display component.</p>
 *
 * @see arlut.csd.JTable.rowTable
 * @see arlut.csd.JTable.gridTable
 * @author Jonathan Abbey
 * @version $Revision: 1.45 $ %D%
 */

public class baseTable extends JComponent implements AdjustmentListener, ActionListener {
  
  static final boolean debug = false;

  /* - */
  
  tableCanvas 
    canvas;

  // the following variables are non-private. tableCanvas accesses them.

  JScrollBar 
    hbar, 
    vbar;

  int
    origTotalWidth,		// starting width of all columns, not counting vLines
    row_height,
    row_baseline,
    vLineThickness = 1,
    hHeadLineThickness = 1,
    hRowLineThickness = 0,
    rowsToShow = -1;

  float
    scalefact;

  Rectangle
    bounding_rect;

  Insets
    in;

  boolean
    hbar_visible,
    vbar_visible,
    horizLines = false,		// show horizontal lines in table
    vertLines = true,		// show vertical lines
    vertFill = true,		// fill table with empty space to bottom of component
    hVertFill = false;		// if horizLines && hVertFill, draw horiz lines to bottom
				// of component, otherwise don't draw horiz lines below
				// last non-empty row

  Vector
    colPos;			// x position of vertical lines.. colPos[0] is 0,
				// colPos[cols.size()] is x pos of right most edge

  tableAttr
    headerAttrib,
    tableAttrib;

  Color
    vHeadLineColor,
    vRowLineColor,
    hHeadLineColor,
    hRowLineColor;

  Vector 
    rows;			// vector of tableRows, which actually hold the table content

  Vector
    cols;			// header information, column attributes

  JPopupMenu
    headerMenu,			// popup menu to appear in header row
    menu;			// popup menu attached to table as a whole

  int 
    menuRow = -1,
    menuCol = -1;		// holds the row, col of the last popup launch

  int 
    selectedRow = -1;

  /* -- */

  // -------------------- Constructors --------------------

  // Create a canvas and two scrollbars and lay them out in the panel.
  // Use a BorderLayout to get the scrollbars flush against the
  // right and bottom sides of the canvas.  When the panel grows,
  // the canvas and scrollbars will also grow appropriately.

  /**
   * This is the base constructor for baseTable, which allows
   * all aspects of the baseTable's appearance and behavior
   * to be customized.
   *
   * @param headerAttrib attribute set for the column headers
   * @param tableAttrib  default attribute set for the body of the table
   * @param colAttribs   per column attribute sets
   * @param colWidths    array of initial column widths
   * @param vHeadLineColor color of vertical lines in the column headers, if any
   * @param vRowLineColor  color of vertical lines in the table body, if any
   * @param hHeadLineColor color of horizontal lines in the column headers, if any
   * @param vRowLineColor  color of vertical lines in the table body, if any
   * @param headers array of column header titles, must be same size as colWidths
   * @param horizLines  true if horizontal lines should be shown between rows in report table
   * @param vertLines   true if vertical lines should be shown between columns in report table
   * @param vertFill    true if table should expand vertically to fill size of baseTable
   * @param hVertFill   true if horizontal lines should be drawn in the vertical fill region
   * 			(only applies if vertFill and horizLines are true)
   *
   */

  public baseTable(tableAttr headerAttrib, 
		   tableAttr tableAttrib,
		   tableAttr[] colAttribs, 
		   int[] colWidths, 
		   Color vHeadLineColor,
		   Color vRowLineColor,
		   Color hHeadLineColor,
		   Color hRowLineColor,
		   String[] headers,
		   boolean horizLines, boolean vertLines,
		   boolean vertFill, boolean hVertFill,
		   JPopupMenu menu,
		   JPopupMenu headerMenu)
  {
    // implicit super() call here creates the panel

    if (debug)
      {
	System.err.println("** entering primary constructor");
      }

    this.headerAttrib = headerAttrib;
    this.tableAttrib = tableAttrib;
    this.vHeadLineColor = vHeadLineColor;
    this.vRowLineColor = vRowLineColor;
    this.hHeadLineColor = hHeadLineColor;
    this.hRowLineColor = hRowLineColor;
    this.horizLines = horizLines;
    this.vertLines = vertLines;
    this.vertFill = vertFill;
    this.hVertFill = hVertFill;
    this.menu = menu;
    this.headerMenu = headerMenu;

    if (menu!= null)
      {
	Component elements[];
	JMenuItem temp;

	elements = menu.getComponents();

	for (int i = 0; i < elements.length; i++)
	  {
	    if (elements[i] instanceof JMenuItem)
	      {
		temp = (JMenuItem) elements[i];
		temp.addActionListener(this);
	      }
	  }
      }

    if (this.horizLines)
      {
	hRowLineThickness = 1;
      }
    else
      {
	hRowLineThickness = 0;
      }

    if (this.vertLines)
      {
	vLineThickness = 1;
      }
    else
      {
	vLineThickness = 0;
      }

    // Initialize our bounding rectangle.
    // This will get filled when the AWT calls our
    // setBounds() method.

    bounding_rect = new Rectangle();

    // Initialize our columns

    if (debug)
      {
	System.err.println("** calculating columns");
      }

    cols = new Vector(colWidths.length);
    colPos = new Vector(colWidths.length + 1);
    origTotalWidth = 0;

    for (int i = 0; i < colWidths.length; i++)
      {
	cols.addElement(new tableCol(this, headers[i], colWidths[i],
				     colAttribs != null?colAttribs[i]:null));
	origTotalWidth += colWidths[i];
	
	colPos.addElement(new Integer(0));
      }

    // and one to grow one for the last pole

    colPos.addElement(new Integer(0));

    // initialize our vector of tableRow's
    // we don't actually allocate cells until they are set

    rows = new Vector();

    // and actually build our component

    if (debug)
      {
	System.err.println("** building component");
      }

    canvas = new tableCanvas(this);
    this.setLayout(new BorderLayout(0,0));
    this.add("Center", canvas);

    // create our scroll bars, but don't add them to our
    // container until we know we need them.

    hbar = new JScrollBar(JScrollBar.HORIZONTAL);
    hbar.addAdjustmentListener(this);
    vbar = new JScrollBar(JScrollBar.VERTICAL);
    vbar.addAdjustmentListener(this);

    // calculate column boundaries and center points

    if (debug)
      {
	System.err.println("** calling calcFonts()");
      }

    calcFonts();

    if (debug)
      {
	System.err.println("** exiting full constructor");
      }

    if (menu != null)
      {
	canvas.add(menu);
      }
  }

  /**
   * Constructor with default fonts, justification, and behavior
   *
   * @param colWidths  array of initial column widths
   * @param headers    array of column header titles, must be same size as colWidths
   *
   */

  public baseTable(int[] colWidths, String[] headers,
		   JPopupMenu menu, JPopupMenu headerMenu)
  {
    this(new tableAttr(null, new Font("Helvetica", Font.BOLD, 14), 
			     Color.white, Color.blue, tableAttr.JUST_CENTER),
	 new tableAttr(null, new Font("Helvetica", Font.PLAIN, 12),
			     Color.black, Color.white, tableAttr.JUST_LEFT),
	 (tableAttr[]) null,
	 colWidths, 
	 Color.black,
	 Color.black,
	 Color.black,
	 Color.black,
	 headers,
	 false, true, true, false,
	 menu,
	 headerMenu);

    if (debug)
      {
	System.err.println("++ entering default valued constructor");
      }

    // we couldn't pass this to the baseTableConstructors
    // above, so we set it directly here, then force metrics
    // calculation

    headerAttrib.c = this;
    headerAttrib.calculateMetrics();
    tableAttrib.c = this;
    tableAttrib.calculateMetrics();

    if (debug)
      {
	System.err.println("++ calling calcFonts");
      }

    calcFonts();

    if (debug)
      {
	System.err.println("++ exiting default valued constructor");
      }
  }

  // -------------------- Cell Access Methods --------------------

  /**
   * Access a cell from the table
   *
   * @param x column number in range [0..# of columns-1]
   * @param y row number in range [0..# of rows-1]
   */

  public final tableCell getCell(int x, int y)
  {
    tableRow row;

    /* -- */

    row = (tableRow) rows.elementAt(y);

    // this shouldn't happen

    if (row == null)
      {
	row = new tableRow(this, cols.size());
	rows.setElementAt(row, y);
      }

    // this often will
    
    if (row.elementAt(x) == null)
      {
	row.setElementAt(new tableCell((tableCol)cols.elementAt(x)), x);
      }

    return row.elementAt(x);
  }

  /**
   * Sets the contents of a cell in the table.
   *
   * @param cell the cell to change
   * @param cellText the text to place into cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellText(tableCell cell, String cellText, boolean repaint)
  {
    cell.setText(cellText);

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param cell the cell to read text from
   */

  public final String getCellText(tableCell cell)
  {
    return cell.origText;
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param cell the cell to read text from
   */

  public final String getWrappedCellText(tableCell cell)
  {
    return cell.text;
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param x column number in range [0..# of columns-1]
   * @param y row number in range [0..# of rows-1]
   */

  public String getCellText(int x, int y)
  {
    tableCell cell = getCell(x, y);
    return cell.origText;
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param x column number in range [0..# of columns-1]
   * @param y row number in range [0..# of rows-1]
   */

  public String getWrappedCellText(int x, int y)
  {
    tableCell cell = getCell(x, y);
    return cell.text;
  }

  // -------------------- Attribute Methods --------------------

  // -------------------- cell attribute methods
  
  /**
   * Sets the tableAttr of a cell in the table.
   *
   * @param cell the tableCell to assign to
   * @param attr the tableAttr to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellAttr(tableCell cell, tableAttr attr, boolean repaint)
  {
    cell.attr = attr;

    calcFonts();
    
    cell.refresh();

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Gets the tableAttr of a cell in the table.
   *
   * @param cell the cell to retrieve a tableAttr from
   *
   */

  public final tableAttr getCellAttr(tableCell cell)
  {
    return cell.attr;
  }

  /**
   * Sets the font of a cell in the table.
   *
   * A font of (Font) null will cause baseTable to revert to using the
   * table or column's default font for this cell.
   *
   * @param cell the cell to change
   * @param font the Font to assign to cell, may be null to use default
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellFont(tableCell cell, Font font, boolean repaint)
  {
    if (cell.attr == null)
      {
	cell.attr = new tableAttr(this);
      }
    
    cell.attr.setFont(font);

    calcFonts();

    cell.refresh();
    
    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the justification of a cell in the table.
   *
   * Use tableAttr.JUST_INHERIT to have this cell use default justification
   *
   * @param cell the cell to change
   * @param just the justification to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   * @see tableAttr
   */

  public final void setCellJust(tableCell cell, int just, boolean repaint)
  {
    if (cell.attr == null)
      {
	cell.attr = new tableAttr(this);
      }
    
    if (just < tableAttr.JUST_LEFT || just > tableAttr.JUST_INHERIT)
      {
	throw new IllegalArgumentException();
      }

    cell.attr.align = just;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the foreground color of a cell
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * foreground selected for the column (if defined) or the foreground for
   * the table.
   *
   * @param cell the cell to change
   * @param color the Color to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellColor(tableCell cell, Color color, boolean repaint)
  {
    if (cell.attr == null)
      {
	cell.attr = new tableAttr(this);
      }

    cell.attr.fg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the background color of a cell
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * background selected for the column (if defined) or the background for
   * the table.
   *
   * @param cell the cell to change
   * @param color the Color to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellBackColor(tableCell cell, Color color, boolean repaint)
  {
    if (cell.attr == null)
      {
	cell.attr = new tableAttr(this);
      }

    cell.attr.bg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  // -------------------- column attribute methods

  /**
   * Sets the tableAttr for a column
   *
   * A attr of (tableAttr) null will cause baseTable to revert to using the
   * table defaults for this column.
   *
   * @param x the column of the table to change color, in the range [0..# of columns-1]
   * @param color the Color to assign to column x
   * @param repaint true if the table should be redrawn after changing column x
   *
   */

  public synchronized final void setColAttr(int x, tableAttr attr, boolean repaint)
  {
    tableCol element;
    tableRow row;

    /* -- */

    element = (tableCol) cols.elementAt(x);
    element.attr = attr;

    calcFonts();

    // recalc the cell's word wrapping, spacing

    for (int i = 0; i < rows.size(); i++)
      {
	row = (tableRow) rows.elementAt(i);

	row.elementAt(x).refresh();
      }

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the font of a column
   *
   * A font of (Font) null will cause baseTable to revert to using the
   * table's default font for this column.
   *
   * @param x the column of the table to change color, in the range [0..# of columns-1]
   * @param font the Font to assign to column x
   * @param repaint true if the table should be redrawn after changing column x
   *
   */

  public synchronized final void setColFont(int x, Font font, boolean repaint)
  {
    tableRow row;

    /* -- */

    tableCol element = (tableCol) cols.elementAt(x);

    if (element.attr == null)
      {
	element.attr = new tableAttr(this);
      }

    element.attr.setFont(font);

    calcFonts();

    // recalc the cell's word wrapping, spacing

    for (int i = 0; i < rows.size(); i++)
      {
	row = (tableRow) rows.elementAt(i);

	row.elementAt(x).refresh();
      }

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the justification of a column in the table.
   *
   * Use tableAttr.JUST_INHERIT to have this column use default justification
   *
   * @param x the column to change, in the range [0..# of columns - 1]
   * @param just the justification to assign to column x
   * @param repaint true if the table should be redrawn after changing column x
   *
   * @see tableAttr
   */

  public final void setColJust(int x, int just, boolean repaint)
  {
    tableCol element = (tableCol) cols.elementAt(x);

    if (element.attr == null)
      {
	element.attr = new tableAttr(this);
      }
    
    if (just < tableAttr.JUST_LEFT || just > tableAttr.JUST_INHERIT)
      {
	throw new IllegalArgumentException();
      }

    element.attr.align = just;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the foreground color of a column
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * foreground selected for the table.
   *
   * @param x the column of the table to change color, in the range [0..# of columns-1]
   * @param color the Color to assign to column x
   * @param repaint true if the table should be redrawn after changing column x
   *
   */

  public final void setColColor(int x, Color color, boolean repaint)
  {
    tableCol element = (tableCol) cols.elementAt(x);

    if (element.attr == null)
      {
	element.attr = new tableAttr(this);
      }

    element.attr.fg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the background color of a column
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * background selected for the table.
   *
   * @param x the column of the table to change color, in the range [0..# of columns-1]
   * @param color the Color to assign to column x
   * @param repaint true if the table should be redrawn after changing column x
   *
   */

  public final void setColBackColor(int x, Color color, boolean repaint)
  {
    tableCol element = (tableCol) cols.elementAt(x);

    if (element.attr == null)
      {
	element.attr = new tableAttr(this);
      }

    element.attr.bg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  // -------------------- table attribute methods

  /**
   *
   * This method sets the number of rows that the table will display.
   * If x == -1, as many rows as possible will be displayed.
   *
   */

  public void setRowsVisible(int x)
  {
    rowsToShow = x;
  }

  /**
   * Sets the tableAttr for the table
   *
   * @param attr the tableAttr to assign to table, must be non-null,
   * @param repaint true if the table should be redrawn after changing attr
   *
   */

  public final void setTableAttr(tableAttr attr, boolean repaint)
  {
    tableRow row;

    /* -- */

    if (attr == null)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib = attr;

    calcFonts();

    // need to refresh all cells

    for (int i = 0; i < rows.size(); i++)
      {
	row = (tableRow) rows.elementAt(i);

	for (int j = 0; j < cols.size(); j++)
	  {
	    row.elementAt(j).refresh();
	  }
      }

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the default font for the table
   *
   * @param font the Font to assign to table, must be non-null
   * @param repaint true if the table should be redrawn after changing font
   *
   */
  
  public final void setTableFont(Font font, boolean repaint)
  {
    tableRow row;

    /* -- */

    if (font == null)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib.setFont(font);

    calcFonts();

    // need to refresh all cells

    for (int i = 0; i < rows.size(); i++)
      {
	row = (tableRow) rows.elementAt(i);

	for (int j = 0; j < cols.size(); j++)
	  {
	    row.elementAt(j).refresh();
	  }
      }

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the default justification of cells in the table.
   *
   * tableAttr.JUST_INHERIT is not a valid value for just
   *
   * @param just the justification to make default for cells in table
   * @param repaint true if the table should be redrawn after changing justification
   *
   * @see tableAttr
   */

  public final void setTableJust(int just, boolean repaint)
  {
    if (just < tableAttr.JUST_LEFT || just >= tableAttr.JUST_INHERIT)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib.align = just;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the foreground color of the table
   *
   * @param color the Color to assign to table foreground, must be non-null
   * @param repaint true if the table should be redrawn after changing color
   *
   */
  
  public final void setTableColor(Color color, boolean repaint)
  {
    if (color == null)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib.fg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the background color of the table
   *
   * @param color the Color to assign to table background, must be non-null
   * @param repaint true if the table should be redrawn after changing color
   *
   */
  
  public final void setTableBackColor(Color color, boolean repaint)
  {
    if (color == null)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib.bg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  // -------------------- header attribute methods

  /**
   * Sets the tableAttr of headers in the table.
   *
   * @param attr the tableAttr to assign to the headers - must be non-null
   * @param repaint true if the table should be redrawn after changing headers
   *
   */

  public final void setHeadAttr(tableAttr attr, boolean repaint)
  {
    if (attr == null)
      {
	throw new IllegalArgumentException();
      }

    headerAttrib = attr;

    calcFonts();

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the foreground color of headers in the table.
   *
   * @param color the Color to assign to the headers - must be non-null
   * @param repaint true if the table should be redrawn after changing headers
   *
   */

  public final void setHeadColor(Color color, boolean repaint)
  {
    if (color == null)
      {
	throw new IllegalArgumentException();
      }

    headerAttrib.fg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the background color of headers in the table.
   *
   * @param color the Color to assign to the headers - must be non-null
   * @param repaint true if the table should be redrawn after changing headers
   *
   */

  public final void setHeadBackColor(Color color, boolean repaint)
  {
    if (color == null)
      {
	throw new IllegalArgumentException();
      }

    headerAttrib.bg = color;

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Sets the font for headers in the table.
   *
   * @param font the Font to assign to the headers - must be non-null
   * @param repaint true if the table should be redrawn after changing headers
   *
   */

  public final void setHeadFont(Font font, boolean repaint)
  {
    if (font == null)
      {
	throw new IllegalArgumentException();
      }

    headerAttrib.setFont(font);

    calcFonts();

    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   *
   * This method takes the given column out of the table entirely.
   *
   */

  public synchronized void deleteColumn(int index, boolean reportion)
  {
    tableRow row;
    tableCol col;

    /* -- */

    if (cols.size() == 1)
      {
	return;			// can't delete only column
      }

    for (int i = 0; i < rows.size(); i++)
      {
	row = (tableRow) rows.elementAt(i);
	row.removeElementAt(index);
      }

    cols.removeElementAt(index);
    colPos.removeElementAt(colPos.size()-1); // doesn't matter which, we're about to recalc it

    if (reportion)
      {
	int newWidth = origTotalWidth / cols.size();

	for (int i=0; i < cols.size(); i++)
	  {
	    col = (tableCol) cols.elementAt(i);
	    col.origWidth = newWidth; 
	  }
      }

    reShape();
  }

  // -------------------- Click / Selection Methods --------------------

  /**
   * Hook for subclasses to implement selection logic
   *
   * @param x col of cell clicked in
   * @param y row of cell clicked in
   */

  public synchronized void clickInCell(int x, int y)
  {
  }

  /**
   * Hook for subclasses to implement selection logic
   *
   * @param x col of cell double clicked in
   * @param y row of cell double clicked in
   */

  public synchronized void doubleClickInCell(int x, int y)
  {
  }

  /**
   * Mark a row as selected
   * @param y row to select
   */

  public final synchronized void selectRow(int y)
  {
    for (int i = 0; i < cols.size(); i++)
      {
	selectCell(i, y);
      }

    selectedRow = y;
  }

  /**
   * Mark a row as unselected
   * @param y row to unselect
   */

  public final synchronized void unSelectRow(int y)
  {
    if (selectedRow == y)
      {
	selectedRow = -1;

	for (int i = 0; i < cols.size(); i++)
	  {
	    unSelectCell(i, y);
	  }
      }
  }

  /**
   * Mark a column as selected
   * @param x col to select
   */

  public final void selectCol(int x)
  {
    for (int i = 0; i < rows.size(); i++)
      {
	selectCell(x,i);
      }
  }

  /**
   * Mark a column as unselected
   * @param x col to unselect
   */

  public final void unSelectCol(int x)
  {
    for (int i = 0; i < rows.size(); i++)
      {
	unSelectCell(x,i);
      }
  }

  /**
   * Unselect all cells
   */

  public void unSelectAll()
  {
    for (int i = 0; i < cols.size(); i++)
      {
	unSelectCol(i);
      }

    refreshTable();
  }

  /**
   * Mark a cell as selected
   * @param x col of cell to select
   * @param y row of cell to select
   */

  public final void selectCell(int x, int y)
  {
    getCell(x,y).selected = true;
  }

  /**
   * Mark a cell as unselected
   * @param x col of cell to unselect
   * @param y row of cell to unselect
   */

  public final void unSelectCell(int x, int y)
  {
    getCell(x,y).selected = false;
  }

  /**
   * Returns true if row y is currently selected
   * @param y row to test
   */

  public final boolean testRowSelected(int y)
  {
    for (int i = 0; i < cols.size(); i++)
      {
	if (!getCell(i,y).selected)
	  {
	    return false;
	  }
      }

    return true;
  }

  /**
   * Returns true if col x is currently selected
   * @param x col to test
   */

  public final boolean testColSelected(int x)
  {
    for (int i = 0; i < rows.size(); i++)
      {
	if (!getCell(x,i).selected)
	  {
	    return false;
	  }
      }

    return true;
  }

  /**
   * Returns true if cell (x,y) is currently selected
   * @param x col of cell to test
   * @param y row of cell to test   
   */

  public final boolean testCellSelected(int x, int y)
  {
    return getCell(x,y).selected;
  }

  // -------------------- Table Methods --------------------

  /**
   * Changes the number of rows in the table
   *
   * @param numRows how many rows should be in the table
   * @param repaint true if the table should be redrawn after changing size
   */

  public void setRows(int numRows, boolean repaint)
  {
    rows.setSize(numRows);

    if (repaint)
      {
	reShape();
	refreshTable();
      }
  }

  /**
   * Adds a new row to the bottom of the table
   *
   * @param repaint true if the table should be redrawn after changing size
   */

  public void addRow(boolean repaint)
  {
    tableRow 
      newRow,
      oldRow;

    int
      bottom;

    /* -- */

    newRow = new tableRow(this, cols.size());

    if (rows.size() == 0)
      {
	newRow.setTopEdge(displayRegionFirstLine());
      }
    else
      {
	int newVal;

	oldRow = (tableRow) rows.lastElement();
	newVal = oldRow.getBottomEdge() + hRowLineThickness;

	if (debug)
	  {
	    System.err.println("Setting topEdge for row " + rows.size() + " to " + newVal);
	  }

	newRow.setTopEdge(newVal);
      }

    bottom =  newRow.getTopEdge() + (newRow.getRowSpan() * (row_height + hRowLineThickness));

    if (debug)
      {
	System.err.println("Setting bottomEdge for row " + rows.size() + " to " + bottom);
      }

    newRow.setBottomEdge(bottom);

    rows.addElement(newRow);

    if (repaint)
      {
	reShape();
	refreshTable();
      }
  }

  /**
   *
   * This method will go through all of the columns and optimize
   * the pole placement to minimize wasted space and provide a decent
   * balance of row and column sizes.
   *
   * Somehow.
   *
   */

  public synchronized void optimizeCols()
  {
    tableRow row;
    tableCell cell;
    tableCol col;
    int nominalWidth[];
    int localNW, newWidth;
    float totalOver, spareSpace, scaledWidth;

    float 
      percentSpace,
      shrinkFactor,
      percentOver,
      growthFactor;

    float totalGrowth = (float) 0.0;
    float totalShrink = (float) 0.0;
    float redistribute = (float) 0.0;

    /* -- */

    if (debug)
      {
	System.err.println("baseTable.optimizeCols(): entering");
      }

    /*
      This method uses the following variables to do its calculations.

      nominalWidth[] - An array of ints holding the width needed by each column.

      totalOver - the aggregate amount of horizontal space that the columns are short,
                  in the absence of wordwrapping.

      spareSpace - the aggregate amount of horizontal space that the columns have to
                   spare.

			      ----------

      In addition, the following variable is defined in baseTable that this method uses:

      scalefact - A float that indicates how big the total width of the table is relative
                  to the requested total width of the columns.

    */

    nominalWidth = new int[cols.size()];
    totalOver = (float) 0.0;
    spareSpace = (float) 0.0;

    for (int i = 0; i < cols.size(); i++)
      {
	nominalWidth[i] = canvas.mincolwidth;
	col = (tableCol) cols.elementAt(i);

	for (int j = 0; j < rows.size(); j++)
	  {
	    row = (tableRow) rows.elementAt(j);
	    cell = row.elementAt(i);

	    localNW = cell.getNominalWidth() + 5;

	    if (localNW > nominalWidth[i])
	      {
		nominalWidth[i] = localNW;
	      }
	  }

	// nominalWidth[i] is now the required width of this column

	scaledWidth = scalefact * col.origWidth;

	if (debug)
	  {
	    System.err.println("Column " + i + " has nominalWidth of " + nominalWidth[i] +
			       " and a current scaled width of " + scaledWidth);
	  }

	if (nominalWidth[i] < scaledWidth)
	  {
	    spareSpace += scaledWidth - nominalWidth[i];

	    /*
	      if ((scaledWidth - (nominalWidth[i] * scalefact)) > 0)
	      {
	      spareSpace += scaledWidth - (nominalWidth[i] * scalefact);
	      }
	    */
	  }
	else
	  {
	    totalOver += (float) nominalWidth[i] - scaledWidth;
	  }
      }

    if (debug)
      {
	System.err.println("spareSpace = " + spareSpace + ", totalOver = " + totalOver);
      }

    redistribute = java.lang.Math.min(spareSpace, totalOver);

    if (debug)
      {
	System.err.println("redistribute = " + redistribute);
      }

    for (int i = 0; i < cols.size(); i++)
      {
	col = (tableCol) cols.elementAt(i);

	// are we going to be actually doing some redistributing?

	if (redistribute > 1.0)
	  {
	    scaledWidth = scalefact * col.origWidth;

	    // Does this column have space to give?

	    if (nominalWidth[i] < scaledWidth)
	      {
		percentSpace = (scaledWidth - nominalWidth[i]) / spareSpace;
		shrinkFactor = (redistribute * percentSpace) / scalefact;

		col.origWidth -= shrinkFactor;

		totalShrink += shrinkFactor * scalefact;

		if (debug)
		  {
		    System.err.println("Column " + i + ": percentSpace = " + percentSpace +
				       " , reducing by " + shrinkFactor + ", new width = " +
				       col.origWidth * scalefact);
		  }
	      }
	    else // need to grow
	      {
		// what percentage of the overage goes to this col?

		percentOver = (nominalWidth[i] - scaledWidth) / totalOver; 
		growthFactor = (redistribute * percentOver) / scalefact;

		col.origWidth += growthFactor;

		totalGrowth += growthFactor * scalefact;

		if (debug)
		  {
		    System.err.println("Column " + i + ": percentOver = " + percentOver + 
				       " , growing by " + growthFactor + ", new width = " + 
				       col.origWidth * scalefact);
		  }
	      }

	    if (debug)
	      {
		System.err.println("totalShrink " + totalShrink + ", totalGrowth = " + totalGrowth);
	      }
	  }

	// now we need to wrap the column

	for (int j = 0; j < rows.size(); j++)
	  {
	    row = (tableRow) rows.elementAt(j);
	    cell = row.elementAt(i);
	    cell.wrap(Math.round(col.origWidth * scalefact));
	  }
      }

    calcCols();
    reCalcRowPos(0);
  }

  /**
   *
   * This method returns the first line of the display area,
   * below the headers.
   * 
   */

  final int displayRegionFirstLine()
  {
    return headerAttrib.height + (2 * hHeadLineThickness);
  }

  /**
   *
   * This method is used to recalculate the vertical position of all
   * of the rows in the table below startRow.  If startRow is 0, all
   * rows will be readjusted.
   *
   * This method is provided to support variable-height rows.
   *
   */

  void reCalcRowPos(int startRow)
  {
    int
      bottomEdge;

    tableRow
      row;

    /* -- */

    if (startRow > rows.size() - 1)
      {
	return;
      }

    if (startRow == 0)
      {
	bottomEdge = headerAttrib.height + hHeadLineThickness; // 
      }
    else
      {
	bottomEdge = ((tableRow) rows.elementAt(startRow - 1)).getBottomEdge();
      }

    // each time through the loop, bottom edge is the y position
    // of the bottom line of the cell above.  Our top edge is going
    // to be below the separating line between the above cell's bottom
    // edge and us.

    for (int i = startRow; i < rows.size(); i++)
      {
	row = (tableRow) rows.elementAt(i);
	row.setTopEdge(bottomEdge + hRowLineThickness);

	bottomEdge = row.getTopEdge() + row.getRowSpan() * (row_height + hRowLineThickness) - hRowLineThickness;

	row.setBottomEdge(bottomEdge);
      }

    reShape();
  }

  /**
   * Deletes a row from the table
   *
   * @param num The index of the row to be deleted
   * @param repaint true if the table should be redrawn after the row is deleted
   */

  public void deleteRow(int num, boolean repaint)
  {
    rows.removeElementAt(num);
    reCalcRowPos(num);		// move everybody at and below the deleted row up

    if (repaint)
      {
	reShape();
	refreshTable();
      }
  }

  /**
   * Causes the table to be updated and redisplayed.
   */
  
  public void refreshTable()
  {
    if (debug)
      {
	System.err.println("refreshTable()");
      }

    this.canvas.render();

    if (debug)
      {
	System.err.println("refreshTable(): render complete");
      }

    this.canvas.repaint();

    if (debug)
      {
	System.err.println("refreshTable(): repaint complete");
      }
  }

  /**
   * Erases all the cells in the table and removes any per-cell
   * attribute sets.  
   */

  public void clearCells()
  {
    tableCell cell;

    /* -- */

    for (int i = 0; i < cols.size(); i++)
      {
	for (int j = 0; j < rows.size(); j++)
	  {
	    cell = getCell(i,j);
	    cell.clear();
	  }
      }

    rows = new Vector();

    reShape();
    refreshTable();
  }

  /**
   * Reinitializes the table with a new set of columns / headers
   *
   */

  public void reinitialize(int[] colWidths, String[] headers)
  {
    reinitialize((tableAttr[]) null, colWidths, headers);
  }

  /**
   * Reinitializes the table with a new set of columns / headers
   *
   */

  public synchronized void reinitialize(tableAttr[] colAttribs, int[] colWidths, String[] headers)
  {
    clearCells();

    cols = new Vector(colWidths.length); 
    colPos = new Vector(colWidths.length + 1);
    origTotalWidth = 0;

    for (int i = 0; i < colWidths.length; i++)
      {
	cols.addElement(new tableCol(this, headers[i], colWidths[i],
				     colAttribs != null?colAttribs[i]:null));
	origTotalWidth += colWidths[i];

	colPos.addElement(new Integer(0));
      }

    // and one to grow on for our last pole

    colPos.addElement(new Integer(0));

    rows = new Vector();

    reShape();
    refreshTable();
  }

  /**
   *
   * This method returns a Vector of Strings containing the
   * titles of columns currently in the table.
   *
   */

  public synchronized Vector getTableHeaders()
  {
    Vector result = new Vector();
    tableCol col;

    /* -- */

    for (int i = 0; i < cols.size(); i++)
      {
	col = (tableCol) cols.elementAt(i);

	result.addElement(col.header);
      }

    return result;
  }

  /**
   *
   * This method returns the number of rows in the table.
   *
   */

  public synchronized int getRowCount()
  {
    return rows.size();
  }

  /**
   *
   * Override this method to implement the popup menu hook.
   *
   */

  public void actionPerformed(ActionEvent e)
  {
    return;
  }

  /**
   * Handles scrollbar events.
   */

  public synchronized void adjustmentValueChanged (AdjustmentEvent e)
  {
    canvas.repaint();
  }

  /**
   * This method is called when our size is changed.  We need to know
   * this so we can update the scrollbars and what-not.
   *
   */

  public synchronized void setBounds(int x, int y, int width, int height)
  {
    if (debug)
      {
	System.err.println("setBounds(" + x + "," + y + "," + width + "," + height + ")");
      }
    
    super.setBounds(x,y,width,height);

    validate();		// we need to do this to get our canvas resized before we call
				// reShape() and refreshTable() below

    if ((width != bounding_rect.width) ||
	(height != bounding_rect.height))
      {
	bounding_rect.x = x;
	bounding_rect.y = y;
	bounding_rect.width = width;
	bounding_rect.height = height;

	in = getInsets();

	bounding_rect.width -= (in.left + in.right);
	bounding_rect.height -= (in.top + in.bottom);

	reShape();
	refreshTable();
      }

    if (debug)
      {
	System.err.println("exiting setBounds()");
      }
  }

  /**
   *
   * Internal method
   *
   * This method recalculates the general parameters of our table's
   * display.  That is, it calculates whether or not we need scroll
   * bars, adds or deletes the scroll bars, and scales the column
   * positions to match the general rendering parameters.
   *
   */

  void reShape()
  {
    if (debug)
      {
	System.err.println("reShape()");
      }

    // calculate whether we need scrollbars, add/remove them
      
    adjustScrollbars();
    calcCols();

    if (debug)
      {
	System.err.println("exiting reShape()");
      }
  }

  /**
   *
   * Internal method
   *
   * Check to see whether we need scrollbars in our current component size,
   * set the min/max/visible parameters
   *
   * This method is intended to be called from reShape().
   *
   */

  void adjustScrollbars()
  {
    int
      hSize, 
      vSize;

    /* -- */

    if (debug)
      {
	System.err.println("adjustScrollbars()");
	System.err.println("canvas.getBounds().width = " + canvas.getBounds().width);
	System.err.println("canvas.getBounds().height = " + canvas.getBounds().height);
      }

    if ((canvas.getBounds().width < 1) ||
	(canvas.getBounds().height < 1))
      {
	// we haven't appeared yet.. return

	return;
      }

    // calculate how wide our table is total, not counting any scroll
    // bars.  That is, how narrow can we be before we need to have a
    // horizontal scrollbar?
    
    hSize = vLineThickness;

    for (int i = 0; i < cols.size(); i++)
      {
	hSize += ((tableCol) cols.elementAt(i)).origWidth + vLineThickness;
      }

    // calculate how tall or table is, not counting any scroll bars.
    // That is, how short can we be before we need to have a vertical
    // scrollbar?

    if (debug)
      {
	System.err.println("Number of rows defined (rows.size()) = " + rows.size());
      }

    if (rows.size() != 0)
      {
	vSize = ((tableRow) rows.lastElement()).getBottomEdge() + hRowLineThickness;
      }
    else
      {
	vSize = 0;
      }

    if (debug)
      {
	System.err.println("vertical size due to combined vertical height of rows = " + vSize);
      }	

    // calculate whether we need scrollbars

    // check to see if we need a horizontal scrollbar

    if (hSize > canvas.getBounds().width)
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

    // check to see if we need a vertical scrollbar

    if (vSize > canvas.getBounds().height)
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

    if (hSize > canvas.getBounds().width)
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
	int my_scrollbar_height = canvas.getBounds().height - displayRegionFirstLine();

	int max_acceptable_value = calcVSize() - my_scrollbar_height;

	if (vbar.getValue() > max_acceptable_value)
	  {
	    vbar.setValues(max_acceptable_value,
			   my_scrollbar_height,
			   0,
			   calcVSize());
	  }
	else
	  {
	    vbar.setValues(vbar.getValue(),
			   my_scrollbar_height,
			   0,
			   calcVSize());
	  }

	vbar.setUnitIncrement(row_height + hRowLineThickness);    // we want the up/down buttons to go a line at a time
	    
	vbar.setBlockIncrement((canvas.getBounds().height - displayRegionFirstLine())/2);
      }

    // Adjust the Horizontal Scrollbar's Parameters

    if (hbar_visible && (canvas.getBounds().width != 0))
      {
	int my_current_width = origTotalWidth  + (cols.size() + 1) * vLineThickness;

	int max_acceptable_value = my_current_width - canvas.getBounds().width;

	if (hbar.getValue() > max_acceptable_value)
	  {
	    hbar.setValues(max_acceptable_value,
			   canvas.getBounds().width,
			   0,
			   my_current_width);
	  }
	else
	  {
	    hbar.setValues(hbar.getValue(),
			   canvas.getBounds().width,
			   0,
			   my_current_width);
	  }

	hbar.setBlockIncrement(canvas.getBounds().width / 2);    
      }

    if (debug)
      {
	System.err.println("exiting adjustScrollbars()");
      }
  }

  /** 
   * Internal method
   *
   * calculate the total vertical size of the rows only
   *
   */

  int calcVSize()
  {
    if (rows.size() == 0)
      {
	return 0;
      }

    return ((tableRow) rows.lastElement()).getBottomEdge() - displayRegionFirstLine() +
      hRowLineThickness;
  }

  /**
   * Internal method
   *
   * Calculate our columns.  This method is called both by
   * reShape() and by our canvas, in response to the user
   * adjusting the columns by hand.
   *
   */

  void calcCols()
  {
    int 
      pos;

    tableCol element;

    /* -- */

    if (debug)
      {
	System.err.println("Processing calcCols()");
      }

    // if we allow the user to adjust the columns manually,
    // we'll want to just abort this and not try to dynamically
    // size our columns.  if an adjustment feature is added, we'll
    // want a boolean for floating vs. non-floating

    if (hbar_visible)
      {
	if (debug)
	  {
	    System.err.println("Horizontal scrollbar visible");
	  }

	// our desired width is too wide for our component.  We'll
	// use our original column widths, and let the scrollbar
	// handle things

	scalefact = (float) 1.0;

	// Calculate vertical bar positions
	
	pos = 0;

	for (int i = 0; i < cols.size(); i++)
	  {
	    element = (tableCol) cols.elementAt(i);
	    element.width = (int) element.origWidth;

	    colPos.setElementAt(new Integer(pos), i);
	    pos += element.width + vLineThickness;
	  }

	// set the last pole directly to avoid scaling artifacts

	colPos.setElementAt(new Integer(origTotalWidth + (cols.size() + 1) * vLineThickness),
			    cols.size());

	// and set the last column's width directly to avoid the same

	if (cols.size() > 0)
	  {
	    element = (tableCol) cols.elementAt(cols.size() - 1);
	    
	    element.width = ((Integer)colPos.elementAt(cols.size())).intValue() -
	      ((Integer)colPos.elementAt(cols.size()-1)).intValue();
	  }
      }
    else
      {
	// okay, we know that we can fit our original columns
	// into the component without needing to scroll.  Now,
	// do we have extra space?  If so, let's calculate
	// how big we can make our columns

	if (debug)
	  {
	    System.err.println("Horizontal scrollbar not visible");
	    System.err.println("Scaling");
	    System.err.println("Canvas width: " + canvas.getBounds().width);
	    System.err.println("Canvas height: " + canvas.getBounds().height);
	  }

	// figure out how much we need to scale the column sizes to fill the 
	// available horizontal space

	/*  note that when we do column resizing we'll need to update this
	    algorithm, particularly the use of origTotalWidth as the source
	    or our scrolling.. column adjustment should change relative width
	    of columns relative to the scalefact, probably.
	    
	    make scalefact an object global float? */
	
	scalefact = (canvas.getBounds().width - (cols.size() + 1) * vLineThickness) / 
	  (float) origTotalWidth;

	if (debug)
	  {
	    System.err.println("Scaling factor: " + scalefact);
	  }

	// calculate column positions

	pos = 0;

	for (int i = 0; i < cols.size(); i++)
	  {
	    element = (tableCol) cols.elementAt(i);
	    
	    colPos.setElementAt(new Integer(pos), i);
	    element.width = Math.round(element.origWidth * scalefact);
	    pos += element.width + vLineThickness;
	  }

	// we know where the last colPos should be if we are not doing
	// a scrollbar.  set it directly to avoid integer/float
	// precision problems.

	colPos.setElementAt(new Integer(canvas.getBounds().width - 1), cols.size());

	if (cols.size() > 0)
	  {
	    element = (tableCol) cols.elementAt(cols.size()-1);
	    element.width = ((Integer)colPos.elementAt(cols.size())).intValue() -
	      ((Integer)colPos.elementAt(cols.size()-1)).intValue() - vLineThickness;
	  }
      }

    if (debug)
      {
	System.err.println("Exiting calcCols()");
      }
  }

  /**
   * Internal method
   *
   * Calculate our fonts and measurements.  If they have changed,
   * go ahead and reshape ourselves.
   *
   */

  void calcFonts()
  {
    tableCell cell;
    tableCol element = null;

    int 
      old_rheight = row_height,
      old_rbline = row_baseline;

    /* -- */

    // We want row_height to be big enough
    // to hold the maximum font in use in the
    // table
    //
    // Note that row_height and row_baseline
    // are constant throughout the table

    row_height = 0;
    row_baseline = 0;

    for (int i = 0; i < cols.size(); i++)
      {
	try
	  {
	    element = (tableCol) cols.elementAt(i);

	    if (element.attr.height > row_height)
	      {
		row_height = element.attr.height;
		row_baseline = element.attr.baseline;
	      }
	  }
	catch (RuntimeException ex)
	  {
	    // null pointer or array violation.. continue with the loop
	  }
      }

    for (int i = 0; i < cols.size(); i++)
      {
	for (int j = 0; j < rows.size(); j++)
	  {
	    cell = getCell(i,j);

	    if ((cell.attr != null) && (cell.attr.font != null))
	      {
		if (cell.attr.height > row_height)
		  {
		    row_height = cell.attr.height;
		    row_baseline = cell.attr.baseline;
		  }
	      }
	  }
      }

    if (tableAttrib.height > row_height)
      {
	// this row will use the table default
	
	row_height = tableAttrib.height;
	row_baseline = tableAttrib.baseline;
      }

    // If our effective row size changed, we need to go ahead
    // and reshape.

    if ((old_rheight != row_height) || (old_rbline != row_baseline))
      {
	reCalcRowPos(0);
	reShape();
      }
  }

  /**
   *
   * This method is used to adjust the vertical scrollbar (if present)
   * such that row <rowIndex> has its topEdge at y
   *
   */

  synchronized void scrollRowTo(int rowIndex, int y)
  {
    tableRow row;
    int currenty;
    int currentVbar;

    /* -- */

    if (!vbar_visible)
      {
	return;
      }
    
    row = (tableRow) rows.elementAt(rowIndex);

    currentVbar = vbar.getValue();
    currenty = row.getTopEdge() - currentVbar;

    if (debug)
      {
	System.err.println("scrollRowTo: row " + row + " is currently at " + currenty);
      }
    
    // ok, currenty is where the row's top edge is now.
    // we want it to be at y, so we adjust the vbar's value.

    if (debug)
      {
	System.err.println("vbar is currently at " + currentVbar);
      }

    int newval = currentVbar + currenty - y;

    if (debug)
      {
	System.err.println("setting vbar to " + newval);
      }

    vbar.setValue(newval);
  }
}

