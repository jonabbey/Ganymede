/*

   baseTable.java

   A GUI component

   Created: 29 May 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/
package csd.Table;

import java.awt.*;
import java.util.*;
import csd.Table.*;

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
 * <p>The baseTable supports intelligent scrollbars, dynamic column sizing, and
 * user adjustable columns.</p>
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
 * @see csd.rowTable
 * @author Jonathan Abbey
 * @version $Revision: 1.1 $ %D% 
 */

public class baseTable extends Panel {

  private tableCanvas 
    canvas;

  // the following variables are non-private. tableCanvas accesses them.

  Scrollbar 
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
    canvas_rect,
    bounding_rect;

  Insets
    in;

  boolean
    hbar_visible,
    vbar_visible,
    allow_select = false,
    horizLines = false,		// show horizontal lines in table
    vertLines = true,		// show vertical lines
    vertFill = true,		// fill table with empty space to bottom of component
    hVertFill = false;		// if horizLines && hVertFill, draw horiz lines to bottom
				// of component, otherwise don't draw horiz lines below
				// last non-empty row
  int[]
    colPos;			// x position of vertical lines.. colPos[0] is 0,
				// colPos[cols.length] is x pos of right most edge
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

  tableCol[]
    cols;			// header information, column attributes

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
   * @param allow_select true if rows should be selectable by the user
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
		     boolean allow_select)
  {
    // implicit super() call here creates the panel

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
    this.allow_select = allow_select;

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

    // Initialize our columns

    cols = new tableCol[colWidths.length];
    colPos = new int[colWidths.length+1];
    origTotalWidth = 0;

    for (int i = 0; i < colWidths.length; i++)
      {
	cols[i] = new tableCol(this, headers[i], colWidths[i],
				colAttribs != null?colAttribs[i]:null);
	origTotalWidth += colWidths[i];
      }

    // initialize our vector of tableRow's
    // we don't actually allocate cells until they are set

    rows = new Vector();

    // and actually build our component

    canvas = new tableCanvas(this);
    this.setLayout(new BorderLayout(0,0));
    this.add("Center", canvas);

    hbar = new Scrollbar(Scrollbar.HORIZONTAL);
    vbar = new Scrollbar(Scrollbar.VERTICAL);

    // calculate column boundaries and center points

    calcFonts();

    calcRects(this.bounds());
  }

  /**
   * Constructor with default fonts, justification, and behavior
   *
   * @param colWidths  array of initial column widths
   * @param headers    array of column header titles, must be same size as colWidths
   *
   */

  public baseTable(int[] colWidths, String[] headers)
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
	 false, true, true, false, false);

    // we couldn't pass this to the baseTableConstructors
    // above, so we set it directly here, then force metrics
    // calculation

    headerAttrib.c = this;
    headerAttrib.calculateMetrics();
    tableAttrib.c = this;
    tableAttrib.calculateMetrics();

    calcFonts();
    calcCols();
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
	row = new tableRow(cols.length);
	rows.setElementAt(row, y);
      }

    // this often will
    
    if (row.cells[x] == null)
      {
	row.cells[x] = new tableCell();
      }

    return row.cells[x];
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
    cell.text = cellText;

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
    calcCols();

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
    calcCols();

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

  public final void setColAttr(int x, tableAttr attr, boolean repaint)
  {
    cols[x].attr = attr;

    calcFonts();
    calcCols();

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

  public final void setColFont(int x, Font font, boolean repaint)
  {
    if (cols[x].attr == null)
      {
	cols[x].attr = new tableAttr(this);
      }

    cols[x].attr.setFont(font);

    calcFonts();
    calcCols();

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
    tableCell cell;

    /* -- */

    if (cols[x].attr == null)
      {
	cols[x].attr = new tableAttr(this);
      }
    
    if (just < tableAttr.JUST_LEFT || just > tableAttr.JUST_INHERIT)
      {
	throw new IllegalArgumentException();
      }

    cols[x].attr.align = just;

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
    if (cols[x].attr == null)
      {
	cols[x].attr = new tableAttr(this);
      }

    cols[x].attr.fg = color;

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
    if (cols[x].attr == null)
      {
	cols[x].attr = new tableAttr(this);
      }

    cols[x].attr.bg = color;

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
    if (attr == null)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib = attr;

    calcFonts();
    calcCols();

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
    if (font == null)
      {
	throw new IllegalArgumentException();
      }

    tableAttrib.setFont(font);

    calcFonts();
    calcCols();

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
    tableCell cell;

    /* -- */

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
    calcCols();

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
    calcCols();

    if (repaint)
      {
	refreshTable();
      }
  }


  // -------------------- Selection Methods --------------------

  /**
   * Mark a row as selected
   */

  public final void selectRow(int y)
  {
    for (int i = 0; i < cols.length; i++)
      {
	selectCell(i, y);
      }
  }

  /**
   * Mark a row as unselected
   */

  public final void unSelectRow(int y)
  {
    for (int i = 0; i < cols.length; i++)
      {
	unSelectCell(i, y);
      }
  }

  /**
   * Mark a column as selected
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

  public final void unSelectAll()
  {
    for (int i = 0; i < cols.length; i++)
      {
	unSelectCol(i);
      }
  }

  /**
   * Mark a cell as selected
   */

  public final void selectCell(int x, int y)
  {
    getCell(x,y).selected = true;
  }

  /**
   * Mark a cell as unselected
   */

  public final void unSelectCell(int x, int y)
  {
    getCell(x,y).selected = false;
  }

  /**
   * Returns true if row y is currently selected
   */

  public final boolean testRowSelected(int y)
  {
    for (int i = 0; i < cols.length; i++)
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
    rows.addElement(new tableRow(cols.length));
    if (repaint)
      {
	refreshTable();
      }
  }

  /**
   * Causes the table to be updated and redisplayed.
   */
  
  public void refreshTable()
  {
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

    for (int i = 0; i < cols.length; i++)
      {
	for (int j = 0; j < rows.size(); j++)
	  {
	    cell = getCell(i,j);
	    cell.text = null;
	    cell.attr = null;
	  }
      }
  }

  /**
   * Handles scrollbar events.
   */

  public synchronized boolean handleEvent (Event e)
  {
    if (e.target == hbar)
      {
	switch (e.id)
	  {
	  case Event.SCROLL_LINE_UP:
	  case Event.SCROLL_PAGE_UP:
	  case Event.SCROLL_LINE_DOWN:
	  case Event.SCROLL_PAGE_DOWN:
	  case Event.SCROLL_ABSOLUTE:
	    calcScrollX(((Integer)e.arg).intValue());
	    canvas.repaint();
	    return true;
	  }
      }
    else if (e.target == vbar)
      {
	switch (e.id)
	  {
	  case Event.SCROLL_LINE_UP:
	  case Event.SCROLL_PAGE_UP:
	  case Event.SCROLL_LINE_DOWN:
	  case Event.SCROLL_PAGE_DOWN:
	  case Event.SCROLL_ABSOLUTE:
	    calcScrollY(((Integer)e.arg).intValue());
	    canvas.repaint();
	    return true;
	  }
      }

    // If we didn't handle it above, pass it on to the superclass
    // handleEvent routine, which will check its type and call the
    // mouseDown(), mouseDrag(), and other methods (which we have
    // overridden?).

    return super.handleEvent(e);
  }

  // This method is called when our size is changed.  We need to know
  // this so we can update the scrollbars

  public synchronized void reshape(int x, int y, int width, int
				   height)
  {
    // System.err.println("reshape()");

    if ((width != bounding_rect.width) ||
	(height != bounding_rect.height))
      {
	calcRects(new Rectangle(x,y,width,height));
	
	this.repaint();
      }

    super.reshape(x,y,width,height);
  }

  // Internal method
  //
  // Calculate our fonts and measurements
  //
  void calcFonts()
  {
    tableCell cell;

    /* -- */

    // We want row_height to be big enough
    // to hold the maximum font in use in the
    // table
    //
    // Note that row_height and row_baseline
    // are constant throughout the table

    row_height = 0;
    row_baseline = 0;

    for (int i = 0; i < cols.length; i++)
      {
	try
	  {
	    if (cols[i].attr.height > row_height)
	      {
		row_height = cols[i].attr.height;
		row_baseline = cols[i].attr.baseline;
	      }
	  }
	catch (RuntimeException ex)
	  {
	    // null pointer or array violation.. continue with the loop
	  }
      }

    for (int i = 0; i < cols.length; i++)
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
  }

  // Internal method
  //
  // Calculate our bounding rects
  //
  void calcRects(Rectangle rect)
  {
    // System.err.println("calcRects(rect)");
    
    bounding_rect = rect;

    in = insets();

    bounding_rect.width -= (in.left + in.right);
    bounding_rect.height -= (in.top + in.bottom);

    if (canvas_rect == null)
      {
	canvas_rect = new Rectangle();
      }

    canvas_rect.x = bounding_rect.x;
    canvas_rect.y = bounding_rect.y;
    canvas_rect.width = bounding_rect.width;
    canvas_rect.height = bounding_rect.height;
    
    calcRects();
  }

  // Internal method
  //
  // Update our internal rects if we haven't gotten a new bounding
  // rect
  //
  void calcRects()
  {
    // System.err.println("calcRects()");

    /*
      calculate whether we need scrollbars, add/remove them

      note that adding a horizontal scrollbar may force us to
      add a vertical scrollbar and vice versa, if adding the
      scrollbar for one dimension reduces our canvas_rect
      size below threshold along the other.
     */
      
    checkScroll();		// first pass

    if (vbar_visible)
      {
	canvas_rect.width = bounding_rect.width - vbar.size().width;
      }
    else
      {
	canvas_rect.width = bounding_rect.width;
      }

    if (hbar_visible)
      {
	canvas_rect.height = bounding_rect.height - hbar.size().height;
      }
    else
      {
	canvas_rect.height = bounding_rect.height;
      }

    checkScroll();		// second pass

    if (vbar_visible)
      {
	canvas_rect.width = bounding_rect.width - vbar.size().width;
      }
    else
      {
	canvas_rect.width = bounding_rect.width;
      }

    if (hbar_visible)
      {
	canvas_rect.height = bounding_rect.height - hbar.size().height;
      }
    else
      {
	canvas_rect.height = bounding_rect.height;
      }

    calcCols();
  }

  // Internal method
  //
  // Check to see whether we need scrollbars in our current component size
  //
  // If we do need them, if they aren't visible, add them to our panel
  // If we don't need them and if they are visible, remove them.
  //
  void checkScroll()
  {
    int size;
    boolean was_vbar, was_hbar;

    /* -- */

    // System.err.println("checkScroll()");

    was_vbar = vbar_visible;
    was_hbar = hbar_visible;

    size = vLineThickness;

    for (int i = 0; i < cols.length; i++)
      {
	size += cols[i].origWidth + vLineThickness;
      }

    if (size > canvas_rect.width)
      {
	if (!hbar_visible)
	  {
	    this.add("South", hbar);
	  }
	hbar_visible = true;
      }
    else
      {
	if (hbar_visible)
	  {
	    this.remove(hbar);
	  }
	hbar_visible = false;
	canvas.h_offset = 0;
      }

    // for vertical size, leave space for first horiz line, titles, second horizline

    size = headerAttrib.height + hHeadLineThickness * 2;

    // System.err.println("rows.size() = " + rows.size());

    for (int i=0; i < rows.size(); i++)
      {
	size += row_height + hRowLineThickness;
      }

    // System.err.println("size = " + size);

    // System.err.println("canvas_rect.height = " + canvas_rect.height);

    if (size > canvas_rect.height)
      {
	if (!vbar_visible)
	  {
	    this.add("East", vbar);
	  }
	vbar_visible = true;
      }
    else
      {
	if (vbar_visible)
	  {
	    this.remove(vbar);
	  }
	vbar_visible = false;
	canvas.v_offset = 0;
      }

    // we want to add the scrollbar to visibility
    // after setting the appropriate scrollbar values

    calcScroll();

    if ((was_hbar != hbar_visible) ||
	(was_vbar != vbar_visible))
      {
	this.layout();
      }
  }

  // Internal method
  //
  // Calculate our columns
  //
  void calcCols()
  {
    int 
      pos;

    /* -- */

    // System.err.println("Processing calcCols()");

    // assume that calcScroll() has been called at this point.  We
    // may go ahead and do that here, depending on the context in
    // which we are called

    // if we allow the user to adjust the columns manually,
    // we'll want to just abort this and not try to dynamically
    // size our columns.  if an adjustment feature is added, we'll
    // want a boolean for floating vs. non-floating

    if (hbar_visible)
      {
	// our desired width is too wide for our component.  We'll
	// use our original column widths, and let the scrollbar
	// handle things

	// Calculate vertical bar positions
	
	pos = 0;

	for (int i = 0; i < cols.length; i++)
	  {
	    cols[i].width = (int) cols[i].origWidth;
	    colPos[i] = pos;
	    pos += cols[i].width + vLineThickness;
	  }

	colPos[cols.length] = pos;
      }
    else
      {
	// okay, we know that we can fit our original columns
	// into the component without needing to scroll.  Now,
	// do we have extra space?  If so, let's calculate
	// how big we can make our columns

	// System.err.println("Scaling");
	
	// System.err.println("Canvas width: " + canvas_rect.width);
	// System.err.println("Canvas height: " + canvas_rect.height);

	// figure out how much we need to scale the column sizes to fill the 
	// available horizontal space

	/*  note that when we do column resizing we'll need to update this
	    algorithm, particularly the use of origTotalWidth as the source
	    or our scrolling.. column adjustment should change relative width
	    of columns relative to the scalefact, probably.
	    
	    make scalefact an object global float? */
	
	scalefact = (canvas_rect.width - (cols.length + 1) * vLineThickness) / 
	  (float) origTotalWidth;

	// System.err.println("Scaling factor: " + scalefact);

	// calculate column positions

	pos = 0;

	for (int i = 0; i < cols.length; i++)
	  {
	    colPos[i] = pos;
	    cols[i].width = (int) (cols[i].origWidth * scalefact);
	    pos += cols[i].width + vLineThickness;
	  }

	// we know where the last colPos should be if we are not doing
	// a scrollbar.  set it directly to avoid integer/float
	// precision problems.

	colPos[cols.length] = canvas_rect.width - 1;
	cols[cols.length-1].width = colPos[cols.length] - 
	  colPos[cols.length - 1] - vLineThickness;
      }
  }

  // ---- Scrollbar handling ----

  private void calcScroll()
  {
    // System.err.println("processing calcScroll");
    
    calcScrollY(vbar.getValue());

    calcScrollX(hbar.getValue());
  }

  private void calcScrollY(int y)
  {
    vbar.setValues(y,
		   canvas_rect.height - headerAttrib.height - (2 * hHeadLineThickness),
		   0,
		   (rows.size() * 
		    (row_height + hRowLineThickness)) -
		   (canvas_rect.height - 
		    headerAttrib.height - (2 * hHeadLineThickness)));

    vbar.setLineIncrement(row_height + hRowLineThickness);    // we want the up/down buttons to go a line at a time

    // we want the page up / page down clicks to do 1/2 viewable row area

    vbar.setPageIncrement((canvas_rect.height - headerAttrib.height - 2 * hHeadLineThickness)/2);
  }

  private void calcScrollX(int x)
  {
    hbar.setValues(x,
		   canvas_rect.width,
		   0,
		   origTotalWidth  + (cols.length + 1) * vLineThickness -
		   canvas_rect.width);

    // make the page clicks do 1/2 canvas

    hbar.setPageIncrement(canvas_rect.width / 2);    
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    tableCanvas

------------------------------------------------------------------------------*/

class tableCanvas extends Canvas {

  baseTable rt;
  Image backing;
  Rectangle backing_rect;
  Graphics bg;

  boolean
    dClick = false;

  int 
    hbar_old = 0, 
    vbar_old = 0,
    colDrag = 0,
    colXOR = -1,
    v_offset = 0,
    h_offset = 0;

  long
    lastClick = 0;

  /* -- */

  // -------------------- Constructors --------------------

  public tableCanvas(baseTable rt)
  {
    this.rt = rt;
  }

  // -------------------- Access Methods --------------------

  //
  // Copy the backing image into the canvas
  //
  public synchronized void paint(Graphics g) 
  {
    // System.err.println("paint called");

    if ((backing == null) ||
	(backing_rect.width != rt.canvas_rect.width + 1) ||
	(backing_rect.height != rt.canvas_rect.height + 1) ||
	(hbar_old != rt.hbar.getValue()) ||
	(vbar_old != rt.vbar.getValue()))
      {
	render();
      }

    // System.err.println("copying image");

    g.drawImage(backing, rt.in.left, rt.in.top, this);

    // System.err.println("image copied");
  }

  //
  // Scheduled for us by repaint()
  //
  public void update(Graphics g)
  {
    // System.err.println("update called");

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
     an cellImage object for each cell of a particular cell, and this
     cellImage object is used for all cell rendering and clipping.
     
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

    Graphics
      g = null;

    Image 
      cellImage = null;

    Rectangle
      cellRect = null;

    /* -- */

    // System.err.println("render called");

    // prep our backing image

    if (backing == null) 
      {
	// System.err.println("creating backing image");
	backing = createImage(rt.canvas_rect.width, rt.canvas_rect.height);
	backing_rect = new Rectangle(rt.canvas_rect.x, rt.canvas_rect.y, 
				     rt.canvas_rect.width+1, rt.canvas_rect.height+1);
	bg = backing.getGraphics();
      }
    else if ((backing_rect.width != (rt.canvas_rect.width + 1)) ||
	     (backing_rect.height != (rt.canvas_rect.height + 1)))
      {
	// need to get a new backing image

	// System.err.println("creating new backing image");
	backing.flush();	// free old image resources
	backing = createImage(rt.canvas_rect.width, rt.canvas_rect.height);
	backing_rect = new Rectangle(rt.canvas_rect.x, rt.canvas_rect.y, 
				     rt.canvas_rect.width+1, rt.canvas_rect.height+1);
	bg = backing.getGraphics();
      }
    else if ((hbar_old != rt.hbar.getValue()) ||
	     (vbar_old != rt.vbar.getValue()))
      {
	// System.err.println("rendering new scroll pos");
	hbar_old = rt.hbar.getValue();
	vbar_old = rt.vbar.getValue();
      }

    // System.err.println("\tFilling Rect");

    bg.setColor(Color.lightGray);
    bg.fillRect(0, 0, backing_rect.width, backing_rect.height);

    /* Calculate horizontal offset, rendering parameters */

    if (rt.hbar_visible)
      {
	h_offset = rt.hbar.getValue(); // this may not work on win32?

	/* calculate first col visible */

	first_col = 0;	
	xpos =  rt.vLineThickness + rt.cols[first_col].width;

	while (xpos < h_offset)
	  {
	    xpos += rt.vLineThickness + rt.cols[++first_col].width;
	  }

	/* calculate last col visible */

	last_col = first_col;
	leftedge = rt.canvas_rect.width + h_offset;
	
	while ((xpos < leftedge) && (last_col < rt.cols.length - 1))
	  {
	    xpos += rt.vLineThickness + rt.cols[++last_col].width;
	  }
      }
    else
      {
	h_offset = 0;
	first_col = 0;
	last_col = rt.cols.length -1;
      }

    /* Calculate vertical offset, rendering parameters */

    if (rt.vbar_visible)
      {
	v_offset = rt.vbar.getValue(); // this may not work on win32?

	/* what is the first row that we can see?  that is, the first
	   row whose last line is > v_offset.

	   v_offset is the first line of the display area that we will
	   see.. that is, if v_offset = rt.row_height +
	   rt.hRowLineThickness + 1, we will see the second line of the
	   second row as the first line in our scrolling area */

	first_row = 0;
	ypos = 0;

	while (ypos + rt.row_height + rt.hRowLineThickness < v_offset)
	  {
	    ypos += rt.row_height + rt.hRowLineThickness;
	    first_row++;
	  }

	/* what is the last row we can see?  that is, the last row
	   whose first line is < rt.canvas_rect.height  */

	last_row = first_row;
	bottomedge = v_offset + rt.canvas_rect.height - 1 - rt.headerAttrib.height - 2 * rt.hHeadLineThickness;

	// System.err.println("bottomedge calculated as " + bottomedge);

	while (ypos + rt.row_height + rt.hRowLineThickness < bottomedge)
	  {
	    ypos += rt.row_height + rt.hRowLineThickness;
	    last_row++;
	  }

	if (last_row >= rt.rows.size())
	  {
	    last_row = rt.rows.size() - 1;
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
	    last_row = (rt.canvas_rect.height - rt.headerAttrib.height - 2 * rt.hHeadLineThickness) /
	    (rt.row_height + rt.hRowLineThickness);
	  }

      }

    /* ------------------- okay, we've got our general parameters.
                           we can start doing our drawing. ------------------ */

    /* draw a column at a time, to take advantage of the fact that all cells
       in a column are the same size */

    // System.err.println("Rendering cols: first_col = " + first_col + ", last_col = " + last_col);

    for (int j = first_col; j <= last_col; j++)
      {
	/* prep our cellImage for doing clipping */

	if ((cellImage == null) || (g == null))
	  {
	    cellRect = new Rectangle(0, 0, rt.cols[j].width, rt.row_height);
	    cellImage = createImage(cellRect.width, cellRect.height);
	    g = cellImage.getGraphics();
	  }
	else
	  {
	    /* if we already have an image of the right size, we don't
	       have to do anything.  Otherwise, flush the old image and
	       create a new image and Graphics object for rendering
	       this column */

	    if ((cellRect != null) && (cellRect.width != rt.cols[j].width))
	      {
		cellImage.flush();
		cellRect = new Rectangle(0, 0, rt.cols[j].width, rt.row_height);
		cellImage = createImage(cellRect.width, cellRect.height);
		g = cellImage.getGraphics();
	      }
	  }

	/* render the column 

	   note that this loop can go past the last row in the
	   rt.rows vector (if rt.vertFill is true).. we do this
	   so that we can extend our columns to the bottom of the
	   display, even if the rows are undefined */

	// System.err.println("Rendering rows: first_row = " + first_row + ", last_row = " + last_row);
	
	for (int i = first_row; i <= last_row; i++)
	  {
	    if (i < rt.rows.size())
	      {
		cell = rt.getCell(j,i);
	      }
	    else
	      {
		cell = null;	// if we are vertically filling
	      }

	    // fill in our backgruond

	    if (cell != null)
	      {
		if ((cell.attr != null) && (cell.attr.bg != null) && (cell.attr.fg != null))
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
		else if ((rt.cols[j].attr != null) &&
			 (rt.cols[j].attr.bg != null) &&
			 (rt.cols[j].attr.fg != null))
		  {
		    if (cell.selected)
		      {
			g.setColor(rt.cols[j].attr.fg);
		      }
		    else
		      {
			g.setColor(rt.cols[j].attr.bg);
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
		if ((rt.cols[j].attr != null) &&
		    (rt.cols[j].attr.bg != null) &&
		    (rt.cols[j].attr.fg != null))
		  {
		    g.setColor(rt.cols[j].attr.bg);
		  }
		else
		  {
		    g.setColor(rt.tableAttrib.bg);
		  }
	      }
	    
	    g.fillRect(0, 0, cellRect.width, cellRect.height);

	    // render the contents of our cell if it is not empty

	    if (cell != null)
	      {
		// set our font

		if (cell.text != null)
		  {
		    if ((cell.attr != null) && (cell.attr.font != null))
		      {
			g.setFont(cell.attr.font);
			strwidth = cell.attr.fontMetric.stringWidth(cell.text);
		      }
		    else if ((rt.cols[j].attr != null) &&
			     (rt.cols[j].attr.font != null))
		      {
			g.setFont(rt.cols[j].attr.font);
			strwidth = rt.cols[j].attr.fontMetric.stringWidth(cell.text);
		      }
		    else
		      {
			g.setFont(rt.tableAttrib.font);
			strwidth = rt.tableAttrib.fontMetric.stringWidth(cell.text);
		      }
		    
		    // set our color
		    
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
		    else if ((rt.cols[j].attr != null) &&
			     (rt.cols[j].attr.fg != null) &&
			     (rt.cols[j].attr.bg != null))
		      {
			if (cell.selected)
			  {
			    g.setColor(rt.cols[j].attr.bg);
			  }
			else
			  {
			    g.setColor(rt.cols[j].attr.fg);
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
		    
		    // and draw
		    
		    if ((cell.attr != null) && (cell.attr.align != tableAttr.JUST_INHERIT))
		      {
			just = cell.attr.align;
		      }
		    else if ((rt.cols[j].attr != null) &&
			     (rt.cols[j].attr.align != tableAttr.JUST_INHERIT))
		      {
			just = rt.cols[j].attr.align;
		      }
		    else
		      {
			just = rt.tableAttrib.align;
		      }
		    
		    switch (just)
		      {
		      case tableAttr.JUST_LEFT:
			g.drawString(cell.text, 2, rt.row_baseline);
			break;
			
		      case tableAttr.JUST_RIGHT:
			g.drawString(cell.text, cellRect.width - strwidth - 2, 
				     rt.row_baseline);
			break;
			
		      case tableAttr.JUST_CENTER:
			g.drawString(cell.text, cellRect.width / 2 - (strwidth/2),
				     rt.row_baseline);
			break;
		      }
		  }
	      }

	    // okay, we're done drawing into our cell.  Now we need to
	    // blit the cell into the main backing image.

	    bg.drawImage(cellImage, rt.colPos[j] - h_offset + rt.vLineThickness, 
			 rt.headerAttrib.height + (2 * rt.hHeadLineThickness) + 
			 ((rt.row_height + rt.hRowLineThickness) * i) - v_offset, 
			 this);
	  }
      }

    // Drawing headers ------------------------------------------------------------

    // System.err.println("\tDrawing headers");

    cellImage = null;

    for (int j = first_col; j <= last_col; j++)
      {
	if (cellImage == null)
	  {
	    cellRect = new Rectangle(0, 0, rt.cols[j].width, rt.headerAttrib.height);
	    cellImage = createImage(rt.cols[j].width, rt.headerAttrib.height);
	    g = cellImage.getGraphics();
	  }
	else
	  {
	    // if we already have an image of the right size, we don't
	    // have to do anything.  Otherwise, flush the old image and
	    // create a new image and Graphics object for rendering
	    // this column

	    if (cellRect.width != rt.cols[j].width)
	      {
		cellImage.flush();
		cellRect = new Rectangle(0, 0, rt.cols[j].width, rt.headerAttrib.height);
		cellImage = createImage(rt.cols[j].width, rt.headerAttrib.height);
		g = cellImage.getGraphics();
	      }
	  }

	cellText = rt.cols[j].header;

	g.setFont(rt.headerAttrib.font);
	g.setColor(rt.headerAttrib.bg);
	g.fillRect(0, 0, cellRect.width, cellRect.height);

	if (cellText != null)
	  {
	    g.setColor(rt.headerAttrib.fg);
	    
	    strwidth = rt.headerAttrib.fontMetric.stringWidth(cellText);
	    g.drawString(cellText, cellRect.width / 2 - (strwidth/2), rt.headerAttrib.baseline);
	  }

	// okay, we're done drawing into our cell.  Now we need to
	// blit the cell into the main backing image.
	
	bg.drawImage(cellImage,
		     rt.colPos[j] + rt.vLineThickness - h_offset,
		     rt.hHeadLineThickness, this);
      }

    // Draw lines ------------------------------------------------------------

    // draw horizontal lines

    bg.setColor(rt.hHeadLineColor);

    bg.drawLine(0, 0, rt.canvas_rect.width-1, 0); // top line

    bg.setColor(rt.hRowLineColor);

    bg.drawLine(0,
		rt.headerAttrib.height + rt.hHeadLineThickness,
		rt.canvas_rect.width-1, 
		rt.headerAttrib.height + rt.hHeadLineThickness); // line between header and table

    if (rt.vertFill || rt.vbar_visible)
      {
	ypos = rt.canvas_rect.height - 1;
      }
    else
      {
	ypos = rt.hHeadLineThickness * 2 + rt.headerAttrib.height + 
	  rt.rows.size() * (rt.row_height + rt.hRowLineThickness);
      }

    if (!rt.vertFill && !rt.vbar_visible)
      {
	bg.drawLine(0, ypos, rt.canvas_rect.width - 1, ypos); // bottom line if not vert filling
      }

    // if rt.horizLines is true, draw the horizontal lines
    // in the body of the table

    // if rt.horizLines is false, rt.hRowLineThickness should be
    // 0, and we have no lines to draw

    if (rt.horizLines)
      {
	for (int i = first_row; i <= last_row; i++)
	  {
	    if ((i > rt.rows.size()) && !rt.hVertFill)
	      {
		bg.setColor(rt.tableAttrib.bg);
	      }

	    ypos2 = rt.headerAttrib.height +  2 * rt.hHeadLineThickness +
	      (rt.row_height + rt.hRowLineThickness) * i - v_offset - 1;
	    
	    if (ypos2 > rt.headerAttrib.height + 2 * rt.hHeadLineThickness)
	      {
		bg.drawLine(0,
			    ypos2,
			    rt.canvas_rect.width-1, 
			    ypos2);
	      }
	  }
      }

    // draw vertical lines

    if (rt.vertLines)
      {
	for (int j = first_col; j <= last_col + 1; j++)
	  {
	    xpos = rt.colPos[j] - h_offset;
	    
	    bg.setColor(rt.vHeadLineColor);
	    bg.drawLine(xpos, 0, xpos, rt.headerAttrib.height + 2 * rt.hHeadLineThickness - 1);
	    bg.setColor(rt.vRowLineColor);
	    bg.drawLine(xpos, rt.headerAttrib.height + 2 * rt.hHeadLineThickness, xpos, ypos);
	  }
      }

    // System.err.println("Exiting render");
  }

  // ------------------------------------------------------------
  //
  // initiate column dragging and/or select row

  public synchronized boolean mouseDown(Event e, int x, int y)
  {
    int
      selectedRow,
      col;

    /* -- */

    if (rt.hbar_visible)
      {
	x = x + h_offset;		// adjust for horizontal scrolling
      }

    // System.err.println("mouseDown x = " + x + ", y = " + y);

    // mouse down near column line

    colDrag = 0;

    for (col = 1; col < rt.cols.length; col++)
      {
	// nice wide grab range
	
	if ((x > rt.colPos[col]-4) &&
	    (x < rt.colPos[col]+4))
	  {
	    // System.err.println("column " + col);
	    colDrag = col;
	  }
      }

    if (colDrag == 0)
      {
	// not a column drag.. row chosen?

	if (rt.allow_select && (y > rt.headerAttrib.height + 2 * rt.hHeadLineThickness))
	  {
	    selectedRow = (y + v_offset - rt.headerAttrib.height - 2 * rt.hHeadLineThickness) / 
	      (rt.row_height + rt.hRowLineThickness);

	    if (selectedRow >= rt.rows.size())
	      {
		// deselect the selected row

		rt.unSelectAll();
	      }

	    if (rt.testRowSelected(selectedRow))
	      {
		if (e.when - lastClick < 500)
		  {
		    dClick = true;
		    // System.err.println("Double click");
		  }
		else
		  {
		    rt.unSelectRow(selectedRow);
		    dClick = false;
		  }
	      }
	    else
	      {
		rt.selectRow(selectedRow);
	      }

	    lastClick = e.when;
	    
	    // System.err.println("Selected row " + selectedRow);
	    
	    render();
	    repaint();
	  }
      }

    return true;
  }

  // ------------------------------------------------------------
  //
  // perform column dragging

  public synchronized boolean mouseDrag(Event e, int x, int y)
  {
    if (rt.hbar_visible)
      {
	x = x + h_offset;		// adjust for horizontal scrolling
      }

    if (colDrag != 0)
      {
// 	 System.err.println("colDragging column " + colDrag);

// 	 System.err.println("x = " + x);
// 	 System.err.println("colPos["+(colDrag-1)+"] = " + rt.colPos[colDrag-1]);
// 	 System.err.println("colPos["+(colDrag)+"] = " + rt.colPos[colDrag]);

	// minimum col width is 20 pixels

	if ((x > rt.colPos[colDrag-1] + 20) && (x < rt.colPos[colDrag+1] - 20))
	  {
	    bg.setXORMode(Color.red); // needs to be settable
	    if (colXOR != -1)
	      {
		bg.drawLine(colXOR, 0, colXOR, rt.canvas_rect.height-1);
	      }

	    colXOR = x - h_offset;

	    bg.drawLine(colXOR, 0, colXOR, rt.canvas_rect.height-1);

	    update(this.getGraphics());

	    bg.setPaintMode();
	  }

	return false;
      }

    return true;
  }

  // ------------------------------------------------------------
  //
  // finish column dragging

  public synchronized boolean mouseUp(Event e, int x, int y)
  {
    float x1;

    /* -- */

    if (rt.hbar_visible)
      {
	x = x + h_offset;
      }

    if (colDrag != 0)
      {
// 	System.err.println("placing column " + colDrag);

// 	System.err.println("x = " + x);
// 	System.err.println("colPos["+(colDrag-1)+"] = " + rt.colPos[colDrag-1]);
// 	System.err.println("colPos["+(colDrag)+"] = " + rt.colPos[colDrag]);

	// minimum col width is 20 pixels

	if ((x > rt.colPos[colDrag-1] + 20) && (x < rt.colPos[colDrag+1] - 20))
	  {
	    //	    System.err.println("Adjusting column " + colDrag);

	    rt.colPos[colDrag] = x;

	    if (rt.hbar_visible)
	      {
		// we are not scaled

// 		System.err.println("Adjusting..OLD cols["+(colDrag-1)+"].origWidth = " +
// 				   rt.cols[colDrag-1].origWidth + ", cols["+colDrag+"].origWidth = " +
// 				   rt.cols[colDrag].origWidth);

		rt.cols[colDrag - 1].origWidth = x - rt.colPos[colDrag - 1] - rt.vLineThickness;
		rt.cols[colDrag].origWidth = rt.colPos[colDrag + 1] - x - rt.vLineThickness;

// 		 System.err.println("Adjusting..NEW cols["+(colDrag-1)+"].origWidth = " +
// 				   rt.cols[colDrag-1].origWidth + ", cols["+colDrag+"].origWidth = " +
// 				   rt.cols[colDrag].origWidth);
	      }
	    else
	      {
		// we are probably scaled

// 		System.err.println("Scaling and adjusting..OLD cols["+(colDrag-1)+"].origWidth = " +
// 				   rt.cols[colDrag-1].origWidth + ", cols["+colDrag+"].origWidth = " +
// 				   rt.cols[colDrag].origWidth);
// 		System.err.println("scalefact = " + rt.scalefact);

		x1 = rt.cols[colDrag-1].origWidth;
		rt.cols[colDrag-1].origWidth = ((x - rt.colPos[colDrag - 1] - rt.vLineThickness) / rt.scalefact);
		rt.cols[colDrag].origWidth += x1 - rt.cols[colDrag - 1].origWidth;

// 		 System.err.println("Scaling and adjusting..NEW cols["+(colDrag-1)+"].origWidth = " +
// 				   rt.cols[colDrag-1].origWidth + ", cols["+colDrag+"].origWidth = " +
// 				   rt.cols[colDrag].origWidth);		
	      }
	    rt.calcCols();	// update colPos[] based on origColWidths[]
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
		bg.drawLine(colXOR, 0, colXOR, rt.canvas_rect.height-1);
		bg.setPaintMode();
		update(this.getGraphics());
	      }
	  }

	colDrag = 0;
	colXOR = -1;

	return false;
      }

    colDrag = 0;
    colXOR = -1;
    return true;
  }



  
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      tableCell

------------------------------------------------------------------------------*/

// should the methods in this class be public?

class tableCell {

  String text;
  tableAttr attr;
  boolean selected;

  public tableCell(String text, tableAttr attr)
  {
    this.text = text;
    this.attr = attr;
    this.selected = false;
  }

  public tableCell(String text)
  {
    this.text = text;
    this.attr = null;
    this.selected = false;
  }

  public tableCell()
  {
    this.text = null;
    this.attr = null;
    this.selected = false;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableRow

------------------------------------------------------------------------------*/

// should the methods in this class be public?

class tableRow {

  tableCell[] cells;

  tableRow(tableCell[] cells)
  {
    this.cells = cells;
  }

  tableRow(int size)
  {
    this.cells = new tableCell[size];
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableCol

------------------------------------------------------------------------------*/

// should the methods in this class be public?

class tableCol {

  baseTable rt;
  String header;
  tableAttr attr;
  float origWidth;
  int width;

  tableCol(baseTable rt, String header, float origWidth, tableAttr attr)
  {
    this.rt = rt;
    this.header = header;
    this.origWidth = origWidth;
    this.attr = attr;

    if (this.attr != null)
      {
	this.attr.calculateMetrics();
      }

    this.width = (int) origWidth;
  }
}
