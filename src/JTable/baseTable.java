/*

  baseTable.java

  A JDK 1.1 table AWT component.

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

  Created: 29 May 1996
  Version: $Revision: 1.20 $ %D%
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.JTable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import com.sun.java.swing.*;

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
 * @version $Revision: 1.20 $ %D%
 */

public class baseTable extends JBufferedPane implements AdjustmentListener, ActionListener {
  
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
    hRowLineThickness = 0;

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

  PopupMenu
    headerMenu,			// popup menu to appear in header row
    menu;			// popup menu attached to table as a whole

  int 
    menuRow = -1,
    menuCol = -1;		// holds the row, col of the last popup launch

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
		   PopupMenu menu,
		   PopupMenu headerMenu)
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
	MenuItem temp;

	for (int i = 0; i < menu.getItemCount(); i++)
	  {
	    temp = menu.getItem(i);
	    temp.addActionListener(this);
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
		   PopupMenu menu, PopupMenu headerMenu)
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
	row.setElementAt(new tableCell(this, (tableCol)cols.elementAt(x)), x);
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

	for (int j = 0; j < rows.size(); j++)
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

	for (int j = 0; j < rows.size(); j++)
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

	for (int i=0; i<cols.size(); i++)
	  {
	    col = (tableCol) cols.elementAt(i);
	    col.origWidth = newWidth; 

	    // reShape() will calculate the appropriate scaled with for all columns
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

  public final void selectRow(int y)
  {
    for (int i = 0; i < cols.size(); i++)
      {
	selectCell(i, y);
      }
  }

  /**
   * Mark a row as unselected
   * @param y row to unselect
   */

  public final void unSelectRow(int y)
  {
    for (int i = 0; i < cols.size(); i++)
      {
	unSelectCell(i, y);
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
	newRow.setTopEdge(headerAttrib.height + (2 * hHeadLineThickness) + 1);
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
    float totalOver, spareSpace, percentOver, scaledWidth, growthFactor;

    /* -- */

    nominalWidth = new int[cols.size()];
    totalOver = (float) 0.0;
    spareSpace = (float) 0.0;

    for (int i = 0; i < cols.size(); i++)
      {
	nominalWidth[i] = 0;
	col = (tableCol) cols.elementAt(i);

	for (int j = 0; j < rows.size(); j++)
	  {
	    row = (tableRow) rows.elementAt(j);
	    cell = row.elementAt(i);

	    localNW = cell.getNominalWidth();

	    if (localNW > nominalWidth[i])
	      {
		nominalWidth[i] = localNW;
	      }
	  }

	// nominalWidth is now the required width of this column

	scaledWidth = scalefact * col.origWidth;

	if (debug)
	  {
	    System.err.println("Column " + i + " has nominalWidth of " + nominalWidth[i] + 
			       " and a current scaled width of " + scaledWidth);
	  }

	if (nominalWidth[i] < scaledWidth)
	  {
	    if (debug)
	      {
		System.err.println("Reducing column " + i + " to nominal + 5");
	      }

	    spareSpace += scaledWidth;
	    col.origWidth = (float) (nominalWidth[i] + 5) / scalefact;
	    spareSpace -= col.origWidth * scalefact;
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

    // we've shrunk the columns as much as we can.. now let's take
    // our spareSpace and apportion it out to those columns whose
    // nominal widths are bigger than they 

    for (int i = 0; i < cols.size(); i++)
      {
	col = (tableCol) cols.elementAt(i);
	scaledWidth = scalefact * col.origWidth;

	if (nominalWidth[i] > scaledWidth)
	  {
	    percentOver = (nominalWidth[i] - scaledWidth) / totalOver;
	    growthFactor = (spareSpace * percentOver) / scalefact;

	    if (debug)
	      {
		System.err.print("Column " + i + ": percentOver = " + percentOver + " , growing by " + growthFactor);
	      }

	    col.origWidth += growthFactor;

	    for (int j = 0; j < rows.size(); j++)
	      {
		row = (tableRow) rows.elementAt(j);
		cell = row.elementAt(i);
		cell.wrap(Math.round(col.origWidth * scalefact));
	      }
	  }
      }

    calcCols();
    reCalcRowPos(0);
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
    this.canvas.repaint();
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
	System.err.println("setBounds()");
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
	vbar.setValues(vbar.getValue(),
		       canvas.getBounds().height - headerAttrib.height - (2 * hHeadLineThickness),
		       0,
		       calcVSize());

	vbar.setUnitIncrement(row_height + hRowLineThickness);    // we want the up/down buttons to go a line at a time
	    
	vbar.setBlockIncrement((canvas.getBounds().height - headerAttrib.height - 2 * hHeadLineThickness)/2);
      }

    // Adjust the Horizontal Scrollbar's Parameters

    if (hbar_visible && (canvas.getBounds().width != 0))
      {
	hbar.setValues(hbar.getValue(),
		       canvas.getBounds().width,
		       0,
		       origTotalWidth  + (cols.size() + 1) * vLineThickness);

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

    return ((tableRow) rows.lastElement()).getBottomEdge() - headerAttrib.height - 2 * hHeadLineThickness +
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
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                     tableCanvas

------------------------------------------------------------------------------*/

/**
 *
 * This class is the actual pane that is rendered on to create the table.  The
 * tableCanvas is double buffered, and optimized for speed.
 *
 */

class tableCanvas extends JBufferedPane implements MouseListener, MouseMotionListener {

  static final boolean debug = false;
  static final int colgrab = 4;
  static final int mincolwidth = 20;

  /* - */

  baseTable rt;
  Image backing;
  Rectangle backing_rect;
  Graphics bg;

  int 
    hbar_old = 0, 
    vbar_old = 0,
    colDrag = 0,
    colXOR = -1,
    v_offset = 0,		// y value of the topmost displayed pixel
    h_offset = 0,		// x value of the leftmost displayed pixel
    oldClickCol = -1,
    oldClickRow = -1; 

  long
    lastClick = 0;

  /* -- */

  // -------------------- Constructors --------------------

  public tableCanvas(baseTable rt)
  {
    this.rt = rt;
    addMouseListener(this);
    addMouseMotionListener(this);

    setBuffered(false);		// we do the buffering ourselves currently.
  }

  // -------------------- Access Methods --------------------

  //
  // Copy the backing image into the canvas
  //
  public synchronized void paint(Graphics g) 
  {
    if (debug)
      {
	System.err.println("paint called");
      }

    if ((backing == null) ||
	(backing_rect.width != getBounds().width) ||
	(backing_rect.height != getBounds().height) ||
	(hbar_old != rt.hbar.getValue()) ||
	(vbar_old != rt.vbar.getValue()))
      {
	render();
      }

    if (debug)
      { 
	System.err.println("copying image");
      }

    g.drawImage(backing, 0, 0, this);

    if (debug)
      {
	System.err.println("image copied");
      }
  }

  //
  // Scheduled for us by repaint()
  //
  public void update(Graphics g)
  {
    if (debug)
      {
	System.err.println("update called");
      }

    if (backing != null)
      {
	paint(g);
      }
  }

  /* ----------------------------------------------------------------------

     Ok, we need to draw our table into the backing store.

     The rendering algorithm has been optimized to use the minimum
     number of Graphics contexts for clipping.  The original algorithm
     for rendering obtained a separate Graphics object and clipRect
     for each cell in the table, which took an enormous amount of time
     under Netscape 3.0b4 on Solaris.  The current algorithm sets up
     an cellImage object for each cell of a particular column, and
     this cellImage object is used for all cell rendering and
     clipping.
     
     For now we're just going to center our strings within each
     header.  Later on maybe we'll make this modifiable.

     ---------------------------------------------------------------------- */

  synchronized void render()
  {
    int 
      strwidth,
      just,
      xpos,
      leftedge,
      ypos,
      ypos2,
      bottomedge,
      first_col,
      last_col,
      first_row,
      last_row;

    tableCell
      cell = null;		// make compiler happy

    String
      cellText = null;

    Rectangle
      cellRect = new Rectangle();

    tableRow
      tr = null;

    int
      tempI;

    /* -- */
    
    /* ------------------------------------------------------------------------

       General algortithm: clear our backing image, draw cells, draw headers,
       draw lines.

       Detailed cell drawing algorithm: since all cells in a column are the
       same size, we can allocate an image of the appropriate size for all
       cells in the column, draw the cells one at a time into the cell Image,
       allowing the image's graphics context to do the clipping, then paste
       that cell Image onto the backing image in the right location.  We
       retain the cell Image object for as long as we can, reusing it for
       multiple columns if they have the same cell size.  We don't attempt
       to do any manual/explicit clipping.. we depend on the edges of the
       backing image doing clipping for us, and we will draw our headers
       last, which will take care of clipping the table area against
       the headers.

       ------------------------------------------------------------------------ */
       

    if (debug)
      {
	System.err.println("render called");
      }

    // prep our backing image

    if (backing == null) 
      {
	if (debug)
	  {
	    System.err.println("creating backing image");

	    System.err.println("width = " + getBounds().width);
	    System.err.println("height = " + getBounds().height);
	  }

	backing_rect = new Rectangle(0, 0, getBounds().width, getBounds().height);

	int width = getBounds().width;
	int height = getBounds().height;

	if (debug)
	  {
	    System.err.println("Trying to create image of size " + width + " x " + height);
	  }

	backing = createImage(width, height);
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
    else if ((hbar_old != rt.hbar.getValue()) ||
	     (vbar_old != rt.vbar.getValue()))
      {
	if (debug)
	  {
	    System.err.println("rendering new scroll pos");
	  }

	// paint() uses hbar_old and vbar_old to decide
	// whether it needs to call render

	hbar_old = rt.hbar.getValue();
	vbar_old = rt.vbar.getValue();
      }

    if (debug)
      {
	System.err.println("\tFilling Rect");
      }

    /* Calculate horizontal offset, rendering parameters */

    if (rt.hbar_visible)
      {
	h_offset = rt.hbar.getValue(); // this may not work on win32?

	if (debug)
	  {
	    System.err.println("h_offset = " + h_offset);
	    System.err.println("maximum = " + rt.hbar.getMaximum());
	    System.err.println("calculated right edge = " + (h_offset + getBounds().width));
	    System.err.println("canvas width = " + getBounds().width);
	    System.err.println("position of right pole = " + rt.colPos.elementAt(rt.cols.size()));
	  }

	/* calculate first col visible */

	first_col = 0;	
	xpos =  rt.vLineThickness + ((tableCol) rt.cols.elementAt(first_col)).width;

	while (xpos < h_offset)
	  {
	    xpos += rt.vLineThickness + ((tableCol) rt.cols.elementAt(++first_col)).width;
	  }

	/* calculate last col visible */

	last_col = first_col;
	leftedge = getBounds().width + h_offset;
	
	while ((xpos < leftedge) && (last_col < rt.cols.size() - 1))
	  {
	    xpos += rt.vLineThickness + ((tableCol) rt.cols.elementAt(++last_col)).width;
	  }
      }
    else
      {
	h_offset = 0;
	first_col = 0;
	last_col = rt.cols.size() -1;
      }

    /* Calculate vertical offset, rendering parameters */

    if (rt.vbar_visible)
      {
	v_offset = rt.vbar.getValue();

	/* what is the first row that we can see?  that is, the first
	   row whose last line is > v_offset.

	   v_offset is the first line of the display area that we will
	   see.. that is, if v_offset = rt.row_height +
	   rt.hRowLineThickness + 1, we will see the second line of the
	   second row as the first line in our scrolling area */

	first_row = 0;
	ypos = 0;

	tr = (tableRow) rt.rows.elementAt(first_row);

	while (tr.getBottomEdge() < v_offset && first_row < rt.rows.size())
	  {
	    tr = (tableRow) rt.rows.elementAt(++first_row);
	  }

	if (debug)
	  {
	    System.err.println("Calculated first_row as " + first_row);
	  }

	/* what is the last row we can see?  that is, the last row
	   whose first line is < getBounds().height  */

	last_row = first_row;

	bottomedge = v_offset + getBounds().height - 1 - rt.headerAttrib.height - 2 * rt.hHeadLineThickness;

	while (last_row < rt.rows.size() && ((tableRow) rt.rows.elementAt(last_row)).getTopEdge() < bottomedge)
	  {
	    last_row++;
	  }

	if (debug)
	  {
	    System.err.println("Calculated last_row as " + last_row);
	  }
      }
    else
      {
	v_offset = 0;
	first_row = 0;

	if (!rt.vertFill)
	  {
	    last_row = rt.rows.size() - 1;
	  }
	else
	  {
	    // we'll have varying row sizes for the loaded rows,
	    // followed by a series of rows of single row_height 

	    last_row = rt.rows.size() + 
	      (getBounds().height - rt.headerAttrib.height - 2 * rt.hHeadLineThickness - rt.calcVSize()) /
	      (rt.row_height + rt.hRowLineThickness);

	    if (debug)
	      {
		System.err.println("Precalc: last_row = " + last_row);
	      }
	  }
      }

    /* ------------------- okay, we've got our general parameters.
                           we can start doing our drawing. ------------------ */
    if (debug)
      {
	System.err.println("Rendering cols: first_col = " + first_col + ", last_col = " + last_col);
      }

    tableCol column;
    tableRow row;
    int topLine;
    int leftEdge;

    for (int j = first_col; j <= last_col; j++)
      {
	column = (tableCol) rt.cols.elementAt(j);

	/* render the column 

	   note that this loop can go past the last row in the
	   rt.rows vector (if rt.vertFill is true).. we do this
	   so that we can extend our columns to the bottom of the
	   display, even if the rows are undefined */

	if (debug)
	  {
	    System.err.println("Rendering rows: first_row = " + first_row + ", last_row = " + last_row);
	  }

	leftEdge = ((Integer) rt.colPos.elementAt(j)).intValue() - h_offset + rt.vLineThickness;

	for (int i = first_row; i <= last_row; i++)
	  {
	    row = (tableRow) rt.rows.elementAt(i);

	    topLine = row.getTopEdge() - v_offset;

	    cellRect.setBounds(leftEdge, topLine, column.width, row.getHeight() + 1);
	    bg.setClip(cellRect);
	    
	    renderBlitCell(cellRect, bg, j, i, column);
	  }

	// and now render the header

	cellRect.setBounds(leftEdge, 0, column.width, rt.headerAttrib.height + 1);
	bg.setClip(cellRect);

	bg.setFont(rt.headerAttrib.font);

	bg.setColor(rt.headerAttrib.bg);
	bg.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height);

	if (column.header != null)
	  {
	    bg.setColor(rt.headerAttrib.fg);
	    
	    strwidth = rt.headerAttrib.fontMetric.stringWidth(column.header);
	    bg.drawString(column.header, cellRect.x + cellRect.width / 2 - (strwidth/2), 
			  cellRect.y + rt.headerAttrib.baseline);
	  }
      }

    // Draw lines ------------------------------------------------------------

    // max out our clip so we can draw the lines

    bg.setClip(0,0, getBounds().width, getBounds().height);

    // draw horizontal lines

    bg.setColor(rt.hHeadLineColor);

    bg.drawLine(0, 0, getBounds().width-1, 0); // top line

    bg.setColor(rt.hRowLineColor);

    bg.drawLine(0,
		rt.headerAttrib.height + rt.hHeadLineThickness,
		getBounds().width-1, 
		rt.headerAttrib.height + rt.hHeadLineThickness); // line between header and table

    // draw a line across the bottom of the table

    if (!rt.vbar_visible)
      {
	// very bottom of the canvas

    	ypos = getBounds().height - 1; 
      }
    else
      {
	// bottom of the last row defined (this makes a difference in fill mode)

	ypos = ((tableRow) rt.rows.lastElement()).getBottomEdge() - v_offset;
      }

    drawHorizLine(ypos);
    
    // if rt.horizLines is true, draw the horizontal lines
    // in the body of the table

    // if rt.horizLines is false, rt.hRowLineThickness should be
    // 0, and we have no lines to draw

    if (rt.horizLines)
      {
	topLine = rt.headerAttrib.height + 2 * rt.hHeadLineThickness;

	int horizlinePos = topLine;

	for (int i = first_row; i <= last_row; i++)
	  {
	    if (i > rt.rows.size())
	      {
		if (rt.hVertFill)
		  {
		    horizlinePos += rt.row_height + rt.hRowLineThickness;

		    if (horizlinePos > topLine)
		      {
			drawHorizLine(horizlinePos);
		      }
		  }
	      }
	    else
	      {
		row = (tableRow) rt.rows.elementAt(i);

		horizlinePos = row.getBottomEdge() - v_offset;

		if (horizlinePos > topLine)
		  {
		    drawHorizLine(horizlinePos);
		  }
	      }
	  }
      }

    // draw vertical lines

    if (rt.vertLines)
      {
	for (int j = first_col; j <= last_col + 1; j++)
	  {
	    xpos = ((Integer) rt.colPos.elementAt(j)).intValue() - h_offset;
	    
	    bg.setColor(rt.vHeadLineColor);
	    bg.drawLine(xpos, 0, xpos, rt.headerAttrib.height + 2 * rt.hHeadLineThickness - 1);
	    bg.setColor(rt.vRowLineColor);
	    bg.drawLine(xpos, rt.headerAttrib.height + 2 * rt.hHeadLineThickness, xpos, ypos);
	  }
      }

    if (debug)
      {
	System.err.println("Exiting render");
      }
  }

  private final void drawHorizLine(int y)
  {
    bg.drawLine(0, y, getBounds().width - 1, y);
  }

  private void setCellForeColor(Graphics g, tableCell cell, tableCol element)
  {
    if (cell != null)
      {
	if ((cell.attr != null) && (cell.attr.fg != null) && (cell.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(cell.attr.bg);
	      }
	    else
	      {
		g.setColor(cell.attr.fg);
	      }
	  }
	else if ((element.attr != null) &&
		 (element.attr.fg != null) &&
		 (element.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(element.attr.bg);
	      }
	    else
	      {
		g.setColor(element.attr.fg);
	      }
	  }
	else
	  {
	    if (cell.selected)
	      {
		g.setColor(rt.tableAttrib.bg);
	      }
	    else
	      {
		g.setColor(rt.tableAttrib.fg);
	      }
	  }
      }
    else
      {
	if ((element != null) &&
	    (element.attr != null) &&
	    (element.attr.bg != null) &&
	    (element.attr.fg != null))
	  {
	    g.setColor(element.attr.fg);
	  }
	else
	  {
	    g.setColor(rt.tableAttrib.fg);
	  }
      }
  }

  private void setCellBackColor(Graphics g, tableCell cell, tableCol element)
  {
    if (cell != null)
      {
	if ((cell.attr != null) && (cell.attr.fg != null) && (cell.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(cell.attr.fg);
	      }
	    else
	      {
		g.setColor(cell.attr.bg);
	      }
	  }
	else if ((element.attr != null) &&
		 (element.attr.fg != null) &&
		 (element.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(element.attr.fg);
	      }
	    else
	      {
		g.setColor(element.attr.bg);
	      }
	  }
	else
	  {
	    if (cell.selected)
	      {
		g.setColor(rt.tableAttrib.fg);
	      }
	    else
	      {
		g.setColor(rt.tableAttrib.bg);
	      }
	  }
      }
    else
      {
	if ((element != null) &&
	    (element.attr != null) &&
	    (element.attr.bg != null) &&
	    (element.attr.fg != null))
	  {
	    g.setColor(element.attr.bg);
	  }
	else
	  {
	    g.setColor(rt.tableAttrib.bg);
	  }
      }
  }

  /**
   *
   * This method handles rendering into the blit template for
   * column <col>, row <row>, portion <spanSubset>.  That is,
   * if a particular row is more than one standard row_height
   * tall, spanSubset indicates what portion of the row
   * should be rendered into the blitCell.
   *
   *
   */

  private void renderBlitCell(Rectangle cellRect, Graphics g, 
			      int col, int row,
			      tableCol element)
  {
    tableCell cell;

    int 
      baseLine,
      strwidth,
      just;

    String
      renderString;

    /* -- */

    if (row < rt.rows.size())
      {
	cell = rt.getCell(col, row);
      }
    else
      {
	cell = null;	// if we are vertically filling
      }
    
    // fill in our background

    setCellBackColor(g, cell, element);	// note this is the canvas' setCBC, not the tables
	    
    g.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height);

    if (cell == null)
      {
	return;
      }

    // render the contents of our cell if it is not empty

    // set our font

    g.setFont(cell.getFont());
    strwidth = cell.getCurrentWidth();
	    
    // set our color

    setCellForeColor(g, cell, element);

    for (int i = 0; i < cell.getRowSpan(); i++)
      {
	renderString = cell.getText(i);
	baseLine = cellRect.y + rt.row_baseline + ((rt.row_height + rt.hRowLineThickness) * i);

	if (renderString != null)
	  {
	    // and draw

	    just = cell.getJust();
		    
	    switch (just)
	      {
	      case tableAttr.JUST_LEFT:
		g.drawString(renderString, cellRect.x + 2, baseLine);
		break;
			
	      case tableAttr.JUST_RIGHT:
		g.drawString(renderString, cellRect.x + cellRect.width - strwidth - 2, baseLine);
		break;
			
	      case tableAttr.JUST_CENTER:
		g.drawString(renderString, cellRect.x + cellRect.width / 2 - (strwidth/2), baseLine);
		break;
	      }
	  }
      }
  }

  //
  // Our MouseListener Support
  //

  public void mouseClicked(MouseEvent e)
  {
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

  /**
   *
   * This method takes a y coordinate in virtual table space
   * (i.e., after vertical scrollbar transform) and returns
   * the index for the row containing that point.
   *
   */
  
  int mapClickToRow(int vy)
  {
    int 
      base,
      row,
      rowHeight;

    tableRow
      tr;

    /* -- */
    
    base = rt.headerAttrib.height + 2 * rt.hHeadLineThickness;
    
    row = 0;

    for (row = 0; row < rt.rows.size() && base < vy; row++)
      {
	tr = (tableRow) rt.rows.elementAt(row);
	rowHeight = tr.getRowSpan() * (rt.row_height + rt.hRowLineThickness);

	base += rowHeight;
      }

    return row - 1;
  }

  // initiate column dragging and/or select row

  public synchronized void mousePressed(MouseEvent e)
  {
    int
      x, y,
      vx, vy,
      clickRow,
      clickCol,
      col;

    /* -- */

    x = e.getX();
    y = e.getY();

    if (rt.hbar_visible)
      {
	vx = x + h_offset;	// adjust for horizontal scrolling
      }
    else
      {
	vx = x;
      }

    if (rt.vbar_visible)
      {
	vy = y + v_offset;	// adjust for vertical scrolling
      }
    else
      {
	vy = y;
      }

    if (debug)
      {
	System.err.println("mouseDown x = " + x + ", y = " + y);
      }

    colDrag = 0;
    clickCol = -1;

    // We don't want a popupTrigger event to initialize a column drag

    if (!e.isPopupTrigger())
      {
	// mouse down near column line?

	for (col = 1; col < rt.cols.size(); col++)
	  {
	    int colLoc = ((Integer) rt.colPos.elementAt(col)).intValue();

	    // nice wide grab range
	
	    if ((vx > colLoc - colgrab) &&
		(vx < colLoc + colgrab))
	      {
		if (debug)
		  {
		    System.err.println("column " + col);
		  }

		// we've clicked close to an interior
		// pole.. we'll drag it

		colDrag = col;
	      }
	    else if ((vx >= (colLoc + colgrab)) &&
		     (vx <= (((Integer) rt.colPos.elementAt(col+1)).intValue() - colgrab)))
	      {
		clickCol = col;
	      }
	  }

	// note that the above loop is mostly intended to handle the
	// column adjustment.. since the far left column is not adjustable,
	// the above loop misses it.  Check for it here.

	if (clickCol == -1)
	  {
	    if ((vx >= ((Integer) rt.colPos.elementAt(0)).intValue()) &&
		(vx <= (((Integer) rt.colPos.elementAt(1)).intValue() - colgrab)))
	      {
		clickCol = 0;
	      }
	  }
      }

    if (clickCol != -1)
      {
	// not a column drag.. row chosen?

	if ((y > rt.headerAttrib.height + 2 * rt.hHeadLineThickness))
	  {
	    clickRow = mapClickToRow(vy);

	    // if the user clicked below the last defined row, unselect
	    // anything selected and return.

	    if (clickRow >= rt.rows.size())
	      {
		rt.unSelectAll();
		return;
	      }

	    if ((clickRow == oldClickRow) && (clickCol == oldClickCol))
	      {
		if (e.getWhen() - lastClick < 500)
		  {
		    rt.doubleClickInCell(clickCol, clickRow);
		  }
		else
		  {
		    rt.clickInCell(clickCol, clickRow);
		  }
	      }
	    else
	      {
		rt.clickInCell(clickCol, clickRow);
	      }

	    oldClickRow = clickRow;
	    oldClickCol = clickCol;
	    lastClick = e.getWhen();
	  }
      }

    if (e.isPopupTrigger())
      {
	popupHandler(e);
      }
  }

  // finish column dragging

  public synchronized void mouseReleased(MouseEvent e)
  {
    int
      x, y;

    float 
      x1;

    /* -- */

    x = e.getX();
    y = e.getY();

    if (rt.hbar_visible)
      {
	x = x + h_offset;
      }

    // for Win32

    if (e.isPopupTrigger())
      {
	popupHandler(e);
	return;
      }

    if (colDrag != 0)
      {
	if (debug)
	  {
	    System.err.println("placing column " + colDrag);
	    
	    System.err.println("x = " + x);
	    System.err.println("colPos["+(colDrag-1)+"] = " + rt.colPos.elementAt(colDrag-1));
	    System.err.println("colPos["+(colDrag)+"] = " + rt.colPos.elementAt(colDrag));
	  }

	// if we have gone past the edge of our valid column adjust region,
	// restrict it to a valid range

	int prior = ((Integer) rt.colPos.elementAt(colDrag-1)).intValue();
	int next = ((Integer) rt.colPos.elementAt(colDrag+1)).intValue();

	if (x <= prior + mincolwidth)
	  {
	    x = prior + mincolwidth + 1;
	  }

	if (x >= next - mincolwidth)
	  {
	    x = next - mincolwidth - 1;
	  }

	if (debug)
	  {
	    System.err.println("Adjusting column " + colDrag);
	  }

	rt.colPos.setElementAt(new Integer(x), colDrag);

	if (rt.hbar_visible)
	  {
	    // we are not scaled

	    tableCol priorCol, thisCol;

	    priorCol = (tableCol) rt.cols.elementAt(colDrag-1);
	    thisCol = (tableCol) rt.cols.elementAt(colDrag);

	    if (debug)
	      {
		System.err.println("Adjusting..OLD cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);
	      }

	    priorCol.origWidth = x - prior - rt.vLineThickness;
	    thisCol.origWidth = next - x - rt.vLineThickness;

	    if (debug)
	      {
		System.err.println("Adjusting..NEW cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);
	      }
	  }
	else
	  {
	    // we are probably scaled

	    tableCol priorCol, thisCol;

	    priorCol = (tableCol) rt.cols.elementAt(colDrag-1);
	    thisCol = (tableCol) rt.cols.elementAt(colDrag);

	    if (debug)
	      {
		System.err.println("Scaling and adjusting..OLD cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);
		System.err.println("scalefact = " + rt.scalefact);
	      }

	    x1 = priorCol.origWidth;
	    priorCol.origWidth = ((x - prior - rt.vLineThickness) / rt.scalefact);
	    thisCol.origWidth += x1 - priorCol.origWidth;

	    if (debug)
	      {
		System.err.println("Scaling and adjusting..NEW cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);		
	      }
	  }

	rt.calcCols();	// update colPos[] based on origColWidths[]

	tableRow tr;
	tableCell cl;
	tableCol 
	  priorCol = (tableCol) rt.cols.elementAt(colDrag-1),
	  thisCol = (tableCol) rt.cols.elementAt(colDrag);

	for (int i = 0; i < rt.rows.size(); i++)
	  {
	    tr = (tableRow) rt.rows.elementAt(i);
	    cl = (tableCell) tr.elementAt(colDrag - 1);
	    cl.wrap(priorCol.width);
	    cl = (tableCell) tr.elementAt(colDrag);
	    cl.wrap(thisCol.width);
	  }

	rt.reCalcRowPos(0);

	render();
	repaint();
      }
    else
      {
	// even if we couldn't recalc the column due to a minimum
	// column width violation, we still need to erase our
	// XOR line

	if (colXOR != -1)
	  {
	    bg.setXORMode(Color.red); // needs to be settable
	    bg.drawLine(colXOR, 0, colXOR, getBounds().height-1);
	    bg.setPaintMode();
	    update(this.getGraphics());
	  }
      }

    colDrag = 0;
    colXOR = -1;
    
    return;
  }

  // private popupmenu handler

  private void popupHandler(MouseEvent e)
  {
    int
      x, y,
      vx, vy,
      clickRow,
      clickCol;

    tableCell
      cell;

    tableCol
      col;

    /* -- */

    x = e.getX();
    y = e.getY();

    if (rt.hbar_visible)
      {
	vx = x + h_offset;	// adjust for horizontal scrolling
      }
    else
      {
	vx = x;
      }

    if (rt.vbar_visible)
      {
	vy = y + v_offset;	// adjust for vertical scrolling
      }
    else
      {
	vy = y;
      }

    if (debug)
      {
	System.err.println("popup x = " + x + ", y = " + y);
      }

    // What column were we triggered on?

    clickCol = -1;

    for (int i = 0; i < rt.cols.size(); i++)
      {
	// nice wide grab range
	if ((vx >= (((Integer) rt.colPos.elementAt(i)).intValue())) &&
	    (vx <= (((Integer) rt.colPos.elementAt(i+1)).intValue())))
	  {
	    clickCol = i;
	  }
      }

    if (clickCol == -1)
      {
	return;
      }

    // What row were we triggered on?

    if ((y > rt.headerAttrib.height + 2 * rt.hHeadLineThickness))
      {
	clickRow = mapClickToRow(vy);
	
	// if the user clicked below the last defined row, ignore it
	
	if (clickRow >= rt.rows.size())
	  {
	    return;
	  }
      }
    else
      {
	// we've got a header column..

	clickRow = -1;
	rt.menuRow = -1;
	rt.menuCol = clickCol;

	if (rt.headerMenu != null)
	  {
	    rt.headerMenu.show(this,x,y);
	    return;
	  }
	else
	  {
	    return;
	  }

      }

    // remember what row and column we launched the popup from so 
    // rowTable, etc., can report the row/col in its callback

    rt.menuRow = clickRow;
    rt.menuCol = clickCol;

    rt.clickInCell(clickCol, clickRow);

    if (debug)
      {
	System.err.println("Base table: menuRow = " + rt.menuRow + ", menuCol = " + rt.menuCol);
      }

    cell = ((tableRow) rt.rows.elementAt(clickRow)).elementAt(clickCol);

    if (cell.menu != null)
      {
	if (debug)
	  {
	    System.err.println("Showing cell menu");
	  }
	cell.menu.show(this, x, y);
	return;
      }

    col = (tableCol) rt.cols.elementAt(clickCol);

    if (col.menu != null)
      {
	if (debug)
	  {
	    System.err.println("Showing column menu");
	  }
	col.menu.show(this, x, y);
	return;
      }

    if (rt.menu != null)
      {
	if (debug)
	  {
	    System.err.println("Showing topLevel menu");
	  }
	rt.menu.show(this,x,y);
	return;
      }
    
  }

  // 
  // MouseMotionListener methods
  //

  public void mouseMoved(MouseEvent e)
  {
    // set the cursor?
  }

  // perform column dragging

  public synchronized void mouseDragged(MouseEvent e)
  {
    int 
      x, y;

    /* -- */

    x = e.getX();
    y = e.getY();
    
    if (rt.hbar_visible)
      {
	x = x + h_offset;		// adjust for horizontal scrolling
      }

    if (colDrag != 0)
      {
	if (debug)
	  {
	    System.err.println("colDragging column " + colDrag);
	    System.err.println("x = " + x);
	    System.err.println("colPos["+(colDrag-1)+"] = " + 
			       ((Integer) rt.colPos.elementAt(colDrag-1)).intValue());

	    System.err.println("colPos["+(colDrag)+"] = " + 
			       ((Integer) rt.colPos.elementAt(colDrag)).intValue());
	  }

	if ((x > (((Integer)rt.colPos.elementAt(colDrag-1)).intValue() + mincolwidth + 1)) && 
	    (x < ((Integer)rt.colPos.elementAt(colDrag+1)).intValue() - mincolwidth - 1))
	  {
	    bg.setXORMode(Color.red); // needs to be settable

	    // erase the old line if we had one

	    if (colXOR != -1)
	      {
		bg.drawLine(colXOR, 0, colXOR, getBounds().height-1);
	      }

	    // XOR in our new line, remember where it is with colXOR
	    // so that we can undraw it later.

	    colXOR = x - h_offset;

	    bg.drawLine(colXOR, 0, colXOR, getBounds().height-1);

	    bg.setPaintMode();

	    update(this.getGraphics());
	  }
      }
  }

  // component overrides

  public Dimension getPreferredSize()
  {
    if (rt.rows.size() < 5)
      {
	return new Dimension(rt.origTotalWidth + (rt.cols.size() + 1) * rt.vLineThickness,
			     rt.headerAttrib.height + 2 * rt.hHeadLineThickness +
			     5 * (rt.row_height + rt.hRowLineThickness));
      }
    else
      {
	return new Dimension(rt.origTotalWidth + (rt.cols.size() + 1) * rt.vLineThickness,
			     rt.headerAttrib.height + 2 * rt.hHeadLineThickness +
			     rt.rows.size() * (rt.row_height + rt.hRowLineThickness));
      }
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(rt.origTotalWidth,
			 rt.headerAttrib.height + 2 * rt.hHeadLineThickness +
			 2 * (rt.row_height + rt.hRowLineThickness));
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableCell

------------------------------------------------------------------------------*/

/**
 *
 * tableCell represents the contents of a single cell in the table.
 *
 * This class is responsible for the mechanics of word wrapping.
 *
 */

class tableCell {

  static final boolean debug = false;

  // --

  String 
    origText,
    text;

  tableAttr 
    attr;

  boolean 
    selected;

  PopupMenu menu;

  baseTable rt;
  tableCol col;

  int
    nominalWidth,		// width before any wrapping
    currentWidth,		// width of rightmost pixel of real text in this
				// cell, after wrapping
    lastOfficialWidth = 0;	// what were we last wrapped to?

  private int rowSpan;

  /* -- */

  public tableCell(baseTable rt, tableCol col, String text, tableAttr attr, PopupMenu menu)
  {
    this.rt = rt;
    this.col = col;
    this.attr = attr;
    this.selected = false;
    this.nominalWidth = 0;
    this.currentWidth = 0;
    this.setText(text);

    if (rt == null && menu != null)
      {
	throw new IllegalArgumentException("must define baseTable in cell to attach popup menu.");
      }
    else
      {
	this.menu = menu;

	if (menu!= null)
	  {
	    MenuItem temp;

	    for (int i = 0; i < menu.getItemCount(); i++)
	      {
		temp = menu.getItem(i);
		temp.addActionListener(rt);
	      }

	    rt.canvas.add(menu);
	  }
      }

    calcRowSpan();
  }

  public tableCell(baseTable rt, tableCol col, String text)
  {
    this(rt, col, text, null, null);
  }

  public tableCell(baseTable rt, String text)
  {
    this(rt, null, text, null, null);
  }

  public tableCell(baseTable rt, tableCol col)
  {
    this(rt, col, null, null, null);
  }

  public tableCell(baseTable rt)
  {
    this(rt, null, null, null, null);
  }

  /**
   *
   * This method reinitializes the cell to its virgin state.
   *
   */

  public void clear()
  {
    this.selected = false;
    this.nominalWidth = 0;
    this.currentWidth = 0;
    this.setText(null);
  }

  /**
   *
   * This method sets the text for this cell.
   *
   */

  public void setText(String newText)
  {
    origText = text = newText;
    
    if (origText != null)
      {
	currentWidth = nominalWidth = getMetrics().stringWidth(origText);
    
	if (lastOfficialWidth != 0)
	  {
	    this.wrap(lastOfficialWidth);
	  }
      }
    else
      {
	currentWidth = nominalWidth = 0;
      }

    calcRowSpan();
  }

  /**
   *
   * This method refreshes the cell's measurements, and should
   * be called after the fontmetrics for this cell have changed.
   *
   */

  public void refresh()
  {
    currentWidth = nominalWidth = getMetrics().stringWidth(origText);
    
    if (lastOfficialWidth != 0)
      {
	this.wrap(lastOfficialWidth);
      }

    calcRowSpan();
  }

  /**
   *
   * Return the fontmetrics that apply to this cell.
   *
   */

  public FontMetrics getMetrics()
  {
    if (attr != null && attr.fontMetric !=null)
      {
	return attr.fontMetric;
      }
    else if (col != null && col.attr != null && col.attr.fontMetric != null)
      {
	return col.attr.fontMetric;
      }
    else 
      {
	return rt.tableAttrib.fontMetric;
      }
  }

  /**
   *
   * Return this cell's font
   *
   */

  public Font getFont()
  {
    if (attr != null && attr.fontMetric !=null)
      {
	return attr.font;
      }
    else if (col != null && col.attr != null && col.attr.font != null)
      {
	return col.attr.font;
      }
    else 
      {
	return rt.tableAttrib.font;
      }
  }

  /**
   *
   * Return this cell's justification
   *
   */

  public int getJust()
  {
    if (attr != null && attr.align != tableAttr.JUST_INHERIT)
      {
	return attr.align;
      }
    else if (col != null && col.attr != null && col.attr.align != tableAttr.JUST_INHERIT)
      {
	return col.attr.align;
      }
    else 
      {
	return rt.tableAttrib.align;
      }
  }

  /**
   *
   * This method returns the width of this cell at its widest point,
   * after word wrapping has been performed.
   *
   */

  public int getCurrentWidth()
  {
    return currentWidth;
  }

  /**
   *
   * This method returns the width that the cell's current text would
   * have if not wrapped.
   * 
   */

  public int getNominalWidth()
  {
    return nominalWidth;
  }

  /**
   *
   * This method returns the nth row of this
   * cell's text, where the first row is 0.
   *
   */

  public String getText(int n)
  {
    if (n+1 > rowSpan)
      {
	return "";
      }
    else
      {
	int pos, oldpos = -1;

	for (int i = 0; i < n; i++)
	  {
	    pos = text.indexOf('\n', oldpos + 1);

	    if (pos != -1)
	      {
		oldpos = pos;
	      }
	  }

	if (text.indexOf('\n', oldpos+1) == -1)
	  {
	    return text.substring(oldpos+1);
	  }
	else
	  {
	    return text.substring(oldpos+1, text.indexOf('\n', oldpos+1));
	  }
      }
  }

  /**
   *
   * This method wraps the contained text to a certain
   * number of pixels.
   *
   * @param wrap_length The width of the cell to wrap to, in pixels
   *
   */

  public synchronized void wrap(int wrap_length)
  {
    char[] 
      charAry;

    int 
      p,
      p2,
      offset = 0,
      marker;

    StringBuffer
      result = new StringBuffer();

    FontMetrics
      fm;

    /* -- */

    if (wrap_length < 5)
      {
	throw new IllegalArgumentException("bad params: wrap_length specified as " + wrap_length);
      }

    // if the adjustment is a small enough reduction that it won't affect our
    // line breaking, just return.  Likewise, if we were already unwrapped
    // and our cell width just got bigger, we don't need to wrap.

    if (((wrap_length > currentWidth) && (wrap_length <= lastOfficialWidth)) ||
	((currentWidth == nominalWidth) && (wrap_length >= nominalWidth)))
      {
	return;
      }
    else
      {
	lastOfficialWidth = wrap_length;
      }

    fm = getMetrics();

    if (debug)
      {
	System.err.println("String size = " + origText.length());
      }

    this.currentWidth = 0;
    this.rowSpan = 1;

    charAry = origText.toCharArray();

    p = marker = 0;

    // each time through the loop, p starts out pointing to the same char as marker

    int localWidth;
    
    while (marker < charAry.length)
      {
	localWidth = 0;

	while ((p < charAry.length) && (charAry[p] != '\n') && (localWidth < wrap_length))
	  {
	    localWidth += fm.charWidth(charAry[p++]);
	  }

	// now p points to the character that terminated the loop.. either
	// the first character that extends past the desired wrap_length,
	// or the first newline after marker, or it will have overflowed
	// to be == charAry.length

	if (localWidth > this.currentWidth)
	  {
	    this.currentWidth = localWidth;
	  }
	
	if (p == charAry.length)
	  {
	    if (debug)
	      {
		System.err.println("At completion..");
	      }

	    result.append(origText.substring(marker, p));
	    text = result.toString();

	    return;
	  }

	if (debug)
	  {
	    System.err.println("Step 1: p = " + p + ", marker = " + marker);
	  }

	if (charAry[p] == '\n')
	  {
	    /* We've got a newline.  This newline is bound to have
	       terminated the while loop above.  Step p and marker past
	       the newline and continue on with our loop. */

	    result.append(origText.substring(marker, p));

	    if (debug)
	      {
		System.err.println("found natural newline.. current result = " + result.toString());
	      }

	    p = marker = p+1;
	    rowSpan++;

	    continue;
	  }

	if (debug)
	  {
	    System.err.println("Step 2: hit wrap length, back searching for whitespace break point");
	  }

	p2 = p;

	/* We've either hit the end of the string, or we've
	   gotten past the wrap_length.  Back p2 up to the last space
	   before the wrap_length, if there is such a space.

	   Note that if the next character in the string (the character
	   immediately after the break point) is a space, we don't need
	   to back up at all.  We'll just print up to our current
	   location, do the newline, and skip to the next line. */
	
	if (p < charAry.length)
	  {
	    if (isspace(charAry[p]))
	      {
		offset = 1;	/* the next character is white space.  We'll
				   want to skip that. */
	      }
	    else
	      {
		/* back p2 up to the last white space before the break point */

		while ((p2 > marker) && !isspace(charAry[p2]))
		  {
 		    p2--;
		  }

		offset = 0;
	      }
	  }

	// now we're guaranteed that p2 points to our break character,
	// or that p2 == marker, indicating no whitespace in this row
	// to split on

	/* If the line was completely filled (no place to break),
	   we'll just copy the whole line out and force a break. */

	if (p2 == marker)
	  {
	    p2 = p-1;

	    if (debug)
	      {
		System.err.println("Step 3: no opportunity for break, forcing..");
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Step 3: found break at column " + p2);
	      }
	  }

	if (!isspace(charAry[p2]))
	  {
	    /* If weren't were able to back up to a space, copy
	       out the whole line, including the break character 
	       (in this case, we'll be making the string one
	       character longer by inserting a newline). */

	    if (debug)
	      {
		System.err.println("appending: marker = " + marker + ", p2 = " + p2 + "+1");
	      }
	    
	    result.append(origText.substring(marker, p2+1));
	  }
	else
	  {
	    /* The break character is whitespace.  We'll
	       copy out the characters up to but not
	       including the break character, which
	       we will effectively replace with a
	       newline. */

	    if (debug)
	      {
		System.err.println("appending: marker = " + marker + ", p2 = " + p2);
	      }

	    result.append(origText.substring(marker, p2));
	  }

	/* If we have not reached the end of the string, newline */

	if (p < charAry.length) 
	  {
	    result.append("\n");
	    rowSpan++;
	  }

	p = marker = p2 + 1 + offset;
      }

    text = result.toString();
  }

  private boolean isspace(char c)
  {
    return (c == '\n' || c == ' ' || c == '\t');
  }

  private void calcRowSpan()
  {
    if (text == null)
      {
	rowSpan = 1;
	return;
      }

    char[] cAry = text.toCharArray();

    rowSpan = 1;

    for (int i = 0; i < cAry.length; i++)
      {
	if (cAry[i] == '\n')
	  {
	    rowSpan++;
	  }
      }
  }

  /**
   *
   * This method returns the number of lines this cell desires
   * to occupy.
   *
   */

  public int getRowSpan()
  {
    return rowSpan;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableRow

------------------------------------------------------------------------------*/

/**
 *
 * This class holds all the information for a particular row in the
 * table, including current position of the row in the table, height
 * of row in elemental lines, and a vector of the current cells in
 * this row.
 * 
 */

class tableRow {

  baseTable rt;
  Vector cells = new Vector();
  int rowSpan = 1;
  int topEdge = 0, bottomEdge = 0;

  /* -- */

  /**
   *
   * tableCell array constructor
   *
   * @param rt The baseTable this row belongs to
   * @param cells The initial contents of this row
   *
   */

  tableRow(baseTable rt, tableCell[] cells)
  {
    this.rt = rt;

    for (int i = 0; i < cells.length; i++)
      {
	this.cells.addElement(cells[i]);
      }
  }

  /**
   *
   * size constructor
   *
   * @param rt The baseTable this row belongs to
   * @param size The number of cells to create in this row
   *
   */

  tableRow(baseTable rt, int size)
  {
    this.rt = rt;
    this.cells = new Vector(size);

    for (int i = 0; i < size; i++)
      {
	this.cells.addElement(new tableCell(rt));
      }
  }

  /**
   *
   * This method removes a cell from this row.  Used
   * by baseTable when a column in the table is being
   * deleted.
   *
   */

  void removeElementAt(int index)
  {
    cells.removeElementAt(index);
  }

  /**
   *
   * Cell accessor.
   *
   */

  tableCell elementAt(int index)
  {
    return (tableCell) cells.elementAt(index);
  }

  /**
   *
   * This method is used to replace an existing
   * cell in this row with a new cell.
   *
   */

  void setElementAt(tableCell cell, int index)
  {
    cells.setElementAt(cell, index);

    if (cell.getRowSpan() > rowSpan)
      {
	rowSpan = cell.getRowSpan();
      }
  }

  /**
   *
   * This method returns the number of lines that
   * this row requires when displayed.
   *
   */

  int getRowSpan()
  {
    rowSpan = 0;
    tableCell cell;

    for (int i = 0; i < cells.size(); i++)
      {
	cell = (tableCell) cells.elementAt(i);

	if (cell.getRowSpan() > rowSpan)
	  {
	    rowSpan = cell.getRowSpan();
	  }
      }

    return rowSpan;
  }

  /**
   *
   * This method is used to record the current position
   * of the top edge of this row within the table.  This
   * position is measured with respect to the full height
   * of the table, irrespective of the current scrollbar
   * position.
   *
   */

  void setTopEdge(int y)
  {
    topEdge = y;
  }

  /**
   *
   * This method returns the current position
   * of the top edge of this row within the table.
   *
   */

  int getTopEdge()
  {
    return topEdge;
  }

  /**
   *
   * This method records the current position of the bottom edge of
   * this row within the table.
   *
   */

  void setBottomEdge(int y)
  {
    bottomEdge = y;
  }

  /**
   *
   * This method returns the current position of the bottom edge of
   * this row within the table.
   * 
   */
  
  int getBottomEdge()
  {
    return bottomEdge;
  }

  int getHeight()
  {
    return rowSpan * (rt.row_height + rt.hRowLineThickness);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                        tableCol

------------------------------------------------------------------------------*/

/**
 *
 * This class holds the information on a particular column in the table,
 * including the header, header pop-up menu, current column width, and
 * any special font or style or color information to apply to cells in
 * this column.
 *
 */

class tableCol {

  baseTable rt;
  String header;
  tableAttr attr;
  float origWidth;		// the basic width of a column.. needs to be multiplied by scalefact
  int width;
  PopupMenu menu;

  /* -- */

  public tableCol(baseTable rt, String header, float origWidth, tableAttr attr, 
	   PopupMenu menu)
  {
    if (rt == null && menu != null)
      {
	throw new IllegalArgumentException("must define baseTable in col to attach popup menu.");
      }

    this.rt = rt;
    this.header = header;
    this.origWidth = origWidth;
    this.attr = attr;
    this.menu = menu;

    // the code below is necessary for when we enable column
    // menus.

    if (menu!= null)
      {
	MenuItem temp;

	for (int i = 0; i < menu.getItemCount(); i++)
	  {
	    temp = menu.getItem(i);
	    temp.addActionListener(rt);
	  }

	rt.canvas.add(menu);
      }

    if (this.attr != null)
      {
	this.attr.calculateMetrics();
      }

    this.width = (int) origWidth;
  }

  public tableCol(baseTable rt, String header, float origWidth, tableAttr attr)
  {
    this(rt, header, origWidth, attr, null);
  }
}
