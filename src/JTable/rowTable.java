/*

   rowTable.java

   A GUI component

   Created: 14 June 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/
package csd.Table;

import java.awt.*;
import java.util.*;
import csd.Table.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        rowTable

------------------------------------------------------------------------------*/

/**
 * <p>rowTable is a specialized baseTable, supporting a per-row
 * access model based on a hashtable.
 *
 *
 * @see csd.Table.baseTable
 * @author Jonathan Abbey
 * @version $Revision: 1.2 $ %D% 
 */

public class rowTable extends baseTable {

  Hashtable 
    index;

  Vector
    crossref;

  /**
   * This is the base constructor for rowTable, which allows
   * all aspects of the rowTable's appearance and behavior
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

  public rowTable(tableAttr headerAttrib, 
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
    super(headerAttrib, tableAttrib, colAttribs, colWidths,
	  vHeadLineColor, vRowLineColor, hHeadLineColor, hRowLineColor,
	  headers, horizLines, vertLines, vertFill, hVertFill,
	  allow_select);

    index = new Hashtable();
    crossref = new Vector();
  }

  /**
   * Creates a new row, adds it to the hashtable
   *
   * @param key A hashtable key to be used to refer to this row in the future
   */
  public void newRow(Object key)
  {
    rowHandle element;

    /* -- */

    if (index.containsKey(key))
      {
	// a row with that key already exists.. what to do?
      }

    element = new rowHandle(this);

    index.put(key, element);
  }

  /**
   * Deletes a row
   *
   * @param key A hashtable key for the row to delete
   */

  public void deleteRow(Object key)
  {
    rowHandle element;

    /* -- */

    if (!index.containsKey(key))
      {
	// no such row exists.. what to do?
      }

    element = (rowHandle) index.get(key);

    index.remove(key);

    // delete the row from our parent.. we do this
    // directly here because our parent doesn't
    // support any means of row access that isn't
    // sensitive to numeric position

    rows.removeElementAt(element.rownum);

    // sync up the rowHandles 

    crossref.removeElementAt(element.rownum);

    // and make sure the rownums are correct.

    for (int i = element.rownum; i < crossref.size(); i++)
      {
	((rowHandle) crossref.elementAt(i)).rownum = i;
      }
  }

  /**
   * Gets a cell based on hashkey
   *
   * @param key A hashtable key for the row of the cell
   * @param col Column number, range 0..# of columns - 1
   *
   */

  // ? can this be done?  will java do the right thing
  // for method overloading?

  public tableCell getCell(Object key, int col)
  {
    return super.getCell(((rowHandle)index.get(key)).rownum, col);
  }

  // -------------------- convenience methods --------------------

  /**
   * Sets the contents of a cell in the table.
   *
   * @param key key to the row of the cell to be changed
   * @param col column of the cell to be changed
   * @param cellText the text to place into cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellText(Object key, int col, String cellText, boolean repaint)
  {
    setCellText(getCell(key, col), cellText, repaint);
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param key key to the row of the cell
   * @param col column of the cell
   */

  public final String getCellText(Object key, int col)
  {
    return getCellText(getCell(key,col));
  }

  /**
   * Sets the tableAttr of a cell in the table.
   *
   * @param key key to the row of the cell to be changed
   * @param col column of the cell to be changed
   * @param attr the tableAttr to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellAttr(Object key, int col, tableAttr attr, boolean repaint)
  {
    setCellAttr(getCell(key,col), attr, repaint);
  }

  /**
   * Gets the tableAttr of a cell in the table.
   *
   * @param key key to the row of the cell
   * @param col column of the cell
   */

  public final tableAttr getCellAttr(Object key, int col)
  {
    return getCellAttr(getCell(key,col));
  }

  /**
   * Sets the font of a cell in the table.
   *
   * A font of (Font) null will cause baseTable to revert to using the
   * table or column's default font for this cell.
   *
   * @param key key to the row of the cell
   * @param col column of the cell 
   * @param font the Font to assign to cell, may be null to use default
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellFont(Object key, int col, Font font, boolean repaint)
  {
    setCellFont(getCell(key,col), font, repaint);
  }

  /**
   * Sets the justification of a cell in the table.
   *
   * Use tableAttr.JUST_INHERIT to have this cell use default justification
   *
   * @param key key to the row of the cell
   * @param col column of the cell 
   * @param just the justification to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   * @see tableAttr
   */

  public final void setCellJust(Object key, int col, int just, boolean repaint)
  {
    setCellJust(getCell(key,col),just,repaint);
  }

  /**
   * Sets the foreground color of a cell
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * foreground selected for the column (if defined) or the foreground for
   * the table.
   *
   * @param key key to the row of the cell
   * @param col column of the cell 
   * @param color the Color to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellColor(Object key, int col, Color color, boolean repaint)
  {
    setCellColor(getCell(key,col),color,repaint);
  }

  /**
   * Sets the background color of a cell
   *
   * A color of (Color) null will cause baseTable to revert to using the
   * background selected for the column (if defined) or the background for
   * the table.
   *
   * @param key key to the row of the cell
   * @param col column of the cell 
   * @param color the Color to assign to cell
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellBackColor(Object key, int col, Color color, boolean repaint)
  {
    setCellBackColor(getCell(key,col),color,repaint);
  }


}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       rowHandle

This class is used to map a hash key to a position in the table.

------------------------------------------------------------------------------*/

class rowHandle {

  int rownum;

  public rowHandle(rowTable parent)
  {
    parent.addRow(false);	// don't repaint table
    rownum = parent.rows.size() - 1; // we always add the new row to the end

    // crossref's index for RowHash element should be same as
    // rows's index for the corresponding ReportRow

    parent.crossref.addElement(this);

    // check to make sure 

    if (parent.crossref.indexOf(this) != rownum)
      {
	throw new RuntimeException("rowTable / baseTable mismatch");
      }
  }

}
