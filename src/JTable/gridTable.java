/*

  gridTable.java

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

  Created: 17 June 1996
  Version: $Revision: 1.10 $ %D%
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.JTable;

import java.awt.*;
import java.util.*;

import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       gridTable

------------------------------------------------------------------------------*/

/**
 * <p>gridTable is a specialized baseTable, supporting an x,y 
 * access model
 *
 * @see arlut.csd.JTable.baseTable
 * @author Jonathan Abbey
 * @version $Revision: 1.10 $ %D% 
 */

public class gridTable extends baseTable {

  static final boolean debug = false;

  /* - */

  /**
   * This is the base constructor for gridTable, which allows
   * all aspects of the gridTable's appearance and behavior
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

  public gridTable(tableAttr headerAttrib, 
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
    super(headerAttrib, tableAttrib, colAttribs, colWidths,
	  vHeadLineColor, vRowLineColor, hHeadLineColor, hRowLineColor,
	  headers, horizLines, vertLines, vertFill, hVertFill,
	  menu, headerMenu);

    if (debug)
      {
	System.err.println(">> gridTable primary constructor exiting");
      }
  }


  /**
   * Constructor with default fonts, justification, and behavior
   *
   * @param colWidths  array of initial column widths
   * @param headers    array of column header titles, must be same size as colWidths
   *
   */

  public gridTable(int[] colWidths, String[] headers, JPopupMenu menu)
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
	 true, true, true, true,
	 menu,
	 null);

    if (debug)
      {
	System.err.println(">>> processing gridTable default constructor");
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
	System.err.println(">>> calling calcFonts");
      }

    calcFonts();

    if (debug)
      {
	System.err.println(">>> calling calcCols");
      }

    calcCols();

    if (debug)
      {
	System.err.println(">>> exiting gridTable default constructor");
      }
  }

  /**
   * Sets the contents of a cell in the table.
   *
   * @param x column of the cell to change [0..#cols - 1]
   * @param y row of the cell to change [0..#rows - 1]
   * @param cellText the text to place into cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellText(int x, int y, String cellText, boolean repaint)
  {
    // make sure table is big enough here

    if (y >= rows.size())
      {
	setRows(y+1, false);
      }

    setCellText(getCell(x,y),cellText,repaint);
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param x column of the cell to retrieve [0..#cols - 1]
   * @param y row of the cell to retrieve [0..#rows - 1]
   */

  public final String getCellText(int x, int y)
  {
    return getCellText(getCell(x,y));
  }

  // -------------------- Attribute Methods --------------------

  // -------------------- cell attribute methods
  
  /**
   * Sets the tableAttr of a cell in the table.
   *
   * @param x column of the cell to change [0..#cols - 1]
   * @param y row of the cell to change [0..#rows - 1]
   * @param attr the tableAttr to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellAttr(int x, int y, tableAttr attr, boolean repaint)
  {
    setCellAttr(getCell(x,y),attr,repaint);
  }

  /**
   * Gets the tableAttr of a cell in the table.
   *
   * @param x column of the cell to retrieve tableAttr from [0..#cols - 1]
   * @param y row of the cell to retrieve tableAttr from [0..#rows - 1]
   *
   */

  public final tableAttr getCellAttr(int x, int y)
  {
    return getCellAttr(getCell(x,y));
  }

  /**
   * Sets the font of a cell in the table.
   *
   * A font of (Font) null will cause baseTable to revert to using the
   * table or column's default font for this cell.
   *
   * @param x column of the cell to change [0..#cols - 1]
   * @param y row of the cell to change [0..#rows - 1]
   * @param font the Font to assign to cell, may be null to use default
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellFont(int x, int y, Font font, boolean repaint)
  {
    setCellFont(getCell(x,y),font,repaint);
  }

  /**
   * Sets the justification of a cell in the table.
   *
   * Use tableAttr.JUST_INHERIT to have this cell use default justification
   *
   * @param x column of the cell to change [0..#cols - 1]
   * @param y row of the cell to change [0..#rows - 1]
   * @param just the justification to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   * @see tableAttr
   */

  public final void setCellJust(int x, int y, int just, boolean repaint)
  {
    setCellJust(getCell(x,y),just,repaint);
  }

  /**
   * Sets the foreground color of a cell
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * foreground selected for the column (if defined) or the foreground for
   * the table.
   *
   * @param x column of the cell to change [0..#cols - 1]
   * @param y row of the cell to change [0..#rows - 1]
   * @param color the Color to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellColor(int x, int y, Color color, boolean repaint)
  {
    setCellColor(getCell(x,y),color,repaint);
  }

  /**
   * Sets the background color of a cell
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * background selected for the column (if defined) or the background for
   * the table.
   *
   * @param x column of the cell to change [0..#cols - 1]
   * @param y row of the cell to change [0..#rows - 1]
   * @param color the Color to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellBackColor(int x, int y, Color color, boolean repaint)
  {
    setCellBackColor(getCell(x,y),color,repaint);
  }
}
