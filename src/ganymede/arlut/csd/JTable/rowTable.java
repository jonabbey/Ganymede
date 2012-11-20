/*
   rowTable.java

   A JDK 1.1 table Swing component.

   Created: 14 June 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2011
   The University of Texas at Austin

   Contact information

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

package arlut.csd.JTable;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        rowTable

------------------------------------------------------------------------------*/

/**
 * <p>rowTable is a specialized baseTable, supporting a per-row
 * access model based on a hashtable.
 *
 *
 * @see arlut.csd.JTable.baseTable
 * @author Jonathan Abbey
 * @version $Id$
 */

public class rowTable extends baseTable implements ActionListener {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JTable.rowTable");

  static final public String menuTitle = ts.l("global.menu_title"); // "Column Menu"
  static final public String sortByStr = ts.l("global.sort_by"); // "Sort By This Column"
  static final public String revSortByStr = ts.l("global.rev_sort_by"); // "Reverse Sort By This Column"
  static final public String delColStr = ts.l("global.del_col"); // "Delete This Column"
  static final public String optColWidStr = ts.l("global.opt_col_widths"); // "Optimize Column Widths"

  Hashtable
    index;

  Vector
    crossref;

  rowSelectCallback
    callback;

  JPopupMenu rowMenu;
  JMenuItem SortByMI;
  JMenuItem RevSortByMI;
  JMenuItem DeleteColMI;
  JMenuItem OptimizeMI;

  boolean sortForward = true;

  Object rowSelectedKey;

  /* -- */

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
   * @param hRowLineColor  color of vertical lines in the table body, if any
   * @param headers array of column header titles, must be same size as colWidths
   * @param horizLines  true if horizontal lines should be shown between rows in report table
   * @param vertLines   true if vertical lines should be shown between columns in report table
   * @param vertFill    true if table should expand vertically to fill size of baseTable
   * @param hVertFill   true if horizontal lines should be drawn in the vertical fill region
   *                    (only applies if vertFill and horizLines are true)
   * @param callback    reference to an object that implements the rowSelectCallback interface
   * @param menu  reference to a popup menu to be associated with rows in this table
   * @param allowDeleteColumn if true, a 'Delete This Column' menu item will be added
   * to the per-column popup menu
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
                  rowSelectCallback callback,
                  JPopupMenu menu,
                  boolean allowDeleteColumn)
  {
    super(headerAttrib, tableAttrib, colAttribs, colWidths,
          vHeadLineColor, vRowLineColor, hHeadLineColor, hRowLineColor,
          headers, horizLines, vertLines, vertFill, hVertFill,
          menu, null);

    rowMenu = new JPopupMenu();

    rowMenu.add(new JLabel(menuTitle));
    rowMenu.addSeparator();

    if (colWidths.length > 1)
      {
        SortByMI = new JMenuItem(sortByStr);
        SortByMI.setActionCommand(sortByStr);

        RevSortByMI = new JMenuItem(revSortByStr);
        RevSortByMI.setActionCommand(revSortByStr);

        DeleteColMI = new JMenuItem(delColStr);
        DeleteColMI.setActionCommand(delColStr);

        OptimizeMI = new JMenuItem(optColWidStr);
        OptimizeMI.setActionCommand(optColWidStr);
      }
    else
      {
        SortByMI = new JMenuItem(sortByStr);
        SortByMI.setActionCommand(sortByStr);

        RevSortByMI = new JMenuItem(revSortByStr);
        RevSortByMI.setActionCommand(revSortByStr);
      }

    rowMenu.add(SortByMI);
    rowMenu.add(RevSortByMI);

    if (colWidths.length > 1)
      {
        if (allowDeleteColumn)
          {
            rowMenu.add(DeleteColMI);
          }

        rowMenu.add(OptimizeMI);
      }

    SortByMI.addActionListener(this);
    RevSortByMI.addActionListener(this);

    if (colWidths.length > 1)
      {
        if (allowDeleteColumn)
          {
            DeleteColMI.addActionListener(this);
          }

        OptimizeMI.addActionListener(this);
      }

    canvas.add(rowMenu);

    this.headerMenu = rowMenu;

    this.callback = callback;

    index = new Hashtable();
    crossref = new Vector();
  }

  /**
   * Constructor with default fonts, justification, and behavior
   *
   * @param colWidths  array of initial column widths
   * @param headers    array of column header titles, must be same size as colWidths
   * @param callback    reference to an object that implements the rowSelectCallback interface
   * @param horizLines draw horizontal lines between rows?
   * @param menu  reference to a popup menu to be associated with rows in this table
   *
   */

  public rowTable(int[] colWidths, String[] headers,
                  rowSelectCallback callback,
                  boolean horizLines,
                  JPopupMenu menu, boolean allowDeleteColumn)
  {
    this(new tableAttr(null, new Font("SansSerif", Font.BOLD, 14),
                       Color.white, Color.blue, tableAttr.JUST_CENTER),
         new tableAttr(null, new Font("SansSerif", Font.PLAIN, 12),
                       Color.black, Color.white, tableAttr.JUST_LEFT),
         (tableAttr[]) null,
         colWidths,
         Color.black,
         Color.black,
         Color.black,
         Color.black,
         headers,
         horizLines, true, true, false, callback, menu, allowDeleteColumn);

    // we couldn't pass this to the baseTableConstructors
    // above, so we set it directly here, then force metrics
    // calculation

    headerAttrib.c = this;
    headerAttrib.calculateMetrics();
    tableAttrib.c = this;
    tableAttrib.calculateMetrics();

    calcFonts();
  }

  /**
   * Constructor with default fonts, justification, and behavior
   *
   * @param colWidths  array of initial column widths
   * @param headers    array of column header titles, must be same size as colWidths
   * @param callback    reference to an object that implements the rowSelectCallback interface
   * @param menu  reference to a popup menu to be associated with rows in this table
   *
   */

  public rowTable(int[] colWidths, String[] headers,
                  rowSelectCallback callback,
                  JPopupMenu menu, boolean allowDeleteColumn)
  {
    this(new tableAttr(null, new Font("SansSerif", Font.BOLD, 14),
                       Color.white, Color.blue, tableAttr.JUST_CENTER),
         new tableAttr(null, new Font("SansSerif", Font.PLAIN, 12),
                       Color.black, Color.white, tableAttr.JUST_LEFT),
         (tableAttr[]) null,
         colWidths,
         Color.black,
         Color.black,
         Color.black,
         Color.black,
         headers,
         true, true, true, false, callback, menu, allowDeleteColumn);

    // we couldn't pass this to the baseTableConstructors
    // above, so we set it directly here, then force metrics
    // calculation

    headerAttrib.c = this;
    headerAttrib.calculateMetrics();
    tableAttrib.c = this;
    tableAttrib.calculateMetrics();

    calcFonts();
  }

  /**
   * Hook for subclasses to implement selection logic
   *
   * @param x col of cell clicked in
   * @param y row of cell clicked in
   */

  public synchronized void clickInCell(int x, int y, boolean rightButton)
  {
    rowHandle
      element = null;

    /* -- */

    // find the key for the selected row

    if (debug)
      {
        System.err.println("rowTable.clickInCell(" + x + "," + y + "): seeking key");
      }

    element = findRow(y);

    if (debug)
      {
        if (element == null)
          {
            System.err.println("rowTable.clickInCell(" + x + "," + y + "): key not found");
          }
        else
          {
            System.err.println("rowTable.clickInCell(" + x + "," + y + "): found key " + element.key);
          }
      }

    if (!element.key.equals(rowSelectedKey))
      {
        if (debug)
          {
            System.err.println("rowTable.clickInCell(" + x + "," + y + "): clicked in unselected row.. unselecting");
          }

        unSelectRow();

        if (debug)
          {
            System.err.println("rowTable.clickInCell(" + x + "," + y + "): clicked in unselected row.. selecting");
          }

        selectRow(element.key, false);

        if (debug)
          {
            System.err.println("rowTable.clickInCell(" + x + "," + y + "): clicked in unselected row.. refreshing");
          }

        refreshTable();

        if (debug)
          {
            System.err.println("rowTable.clickInCell(" + x + "," + y + "): table refreshed");
          }
      }
    else
      {
        // go ahead and deselect the current row, if and only if this is a left-button
        // click.

        if (!rightButton)
          {
            if (debug)
              {
                System.err.println("rowTable.clickInCell(" + x + "," + y + "): clicked in selected row.. unselecting");
              }

            unSelectRow();

            if (debug)
              {
                System.err.println("rowTable.clickInCell(" + x + "," + y + "): clicked in selected row.. refreshing");
              }

            refreshTable();

            if (debug)
              {
                System.err.println("rowTable.clickInCell(" + x + "," + y + "): table refreshed");
              }
          }
      }
  }

  /**
   * Hook for subclasses to implement selection logic
   *
   * @param x col of cell double clicked in
   * @param y row of cell double clicked in
   */

  public synchronized void doubleClickInCell(int x, int y)
  {
    rowHandle element = null;

    /* -- */

    element = findRow(y);

    if (element.key.equals(rowSelectedKey))
      {
        callback.rowDoubleSelected(element.key);
      }
    else
      {
        // the first click of our double click deselected
        // the row, go ahead and reselect it

        clickInCell(x,y);
      }
  }

  /**
   *
   * Unselect all cells.. override of a baseTable method.
   *
   */

  public void unSelectAll()
  {
    unSelectRow();
    refreshTable();
  }

  public Object getSelectedRow()
  {
    return rowSelectedKey;
  }

  /**
   *
   * This method unselects any rows currently selected that do not
   * match key and selects the row that does match key (if any).
   *
   * @param key The key to the row to be selected
   *
   */

  public synchronized void selectRow(Object key, boolean refreshTable)
  {
    // unselect the currently selected row, if any.  Note that we
    // are currently only supporting single row selection.

    unSelectRow();

    if (key != null)
      {
        rowHandle row = (rowHandle) index.get(key);

        if (row == null)
          {
            return;
          }

        selectRow(row.rownum);

        rowSelectedKey = key;
      }

    if (refreshTable)
      {
        refreshTable();
      }

    if (callback != null)
      {
        callback.rowSelected(key);
      }
  }

  /**
   *
   * This method unselects the currently selected row.
   *
   */

  public synchronized void unSelectRow()
  {
    if (rowSelectedKey != null)
      {
        rowHandle row = (rowHandle) index.get(rowSelectedKey);

        if (row == null)
          {
            return;
          }

        unSelectRow(row.rownum);

        if (callback != null)
          {
            callback.rowSelected(rowSelectedKey);
          }

        rowSelectedKey = null;
      }
  }

  /**
   *
   * Erases all the cells in the table and removes any per-cell
   * attribute sets.
   *
   */

  public void clearCells()
  {
    index = new Hashtable();
    crossref = new Vector();
    super.clearCells();
    rowSelectedKey = null;
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
        throw new IllegalArgumentException("rowTable.newRow(): row " + key + " already exists.");
      }

    element = new rowHandle(this, key);

    index.put(key, element);
  }

  /**
   * Deletes a row.
   *
   * @param key A hashtable key for the row to delete
   * @param repaint true if the table should be redrawn after the row is deleted
   */

  public void deleteRow(Object key, boolean repaint)
  {
    rowHandle element;

    /* -- */

    if (!index.containsKey(key))
      {
        // no such row exists.. what to do?
        return;
      }

    if (key.equals(rowSelectedKey))
      {
        unSelectRow();
      }

    element = (rowHandle) index.get(key);

    index.remove(key);

    // delete the row from our parent..

    super.deleteRow(element.rownum, repaint);

    // sync up the rowHandles

    crossref.removeElementAt(element.rownum);

    // and make sure the rownums are correct.

    for (int i = element.rownum; i < crossref.size(); i++)
      {
        ((rowHandle) crossref.elementAt(i)).rownum = i;
      }

    reShape();
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
    return super.getCell(col, ((rowHandle)index.get(key)).rownum);
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
   * Sets the contents of a cell in the table.
   *
   * @param key key to the row of the cell to be changed
   * @param col column of the cell to be changed
   * @param cellText the text to place into cell
   * @param data A piece of data to be held with this cell, will be used for sorting
   * @param repaint true if the table should be redrawn after changing cell
   *
   */

  public final void setCellText(Object key, int col, String cellText, Object data, boolean repaint)
  {
    tableCell cell;

    cell = getCell(key, col);
    cell.setData(data);
    setCellText(cell, cellText, repaint);
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

  /**
   * Returns true if a key is already in use in the table
   *
   * @param key key to look for in the table
   */

  public boolean containsKey(Object key)
  {
    return index.containsKey(key);
  }

  /**
   * Return an enumeration of the keys in the table
   *
   */

  public Enumeration keys()
  {
    return index.keys();
  }

  /**
   * Method used to handle the popup menu
   */

  public void actionPerformed(ActionEvent e)
  {
    rowHandle element = null;

    /* -- */

    //    System.err.println("rowTable.actionPerformed");

    if (callback == null)
      {
        return;
      }

    try
      {
        if (menuRow == -1)
          {
            if (e.getSource() == DeleteColMI)
              {
                callback.colMenuPerformed(menuCol, e);
                this.deleteColumn(menuCol, true);
                refreshTable();
              }
            else if (e.getSource() == SortByMI)
              {
                callback.colMenuPerformed(menuCol, e);
                sortForward = true;
                resort(menuCol, true);
              }
            else if (e.getSource() == RevSortByMI)
              {
                callback.colMenuPerformed(menuCol, e);
                sortForward = false;
                resort(menuCol, true);
              }
            else if (e.getSource() == OptimizeMI)
              {
                callback.colMenuPerformed(menuCol, e);
                optimizeCols();
                refreshTable();
              }

            return;
          }

        element = findRow(menuRow);

        // perform our callback

        if (element != null)
          {
            callback.rowMenuPerformed(element.key, e);
          }
      }
    finally
      {
        // clear our lastpopped menu row, col

        menuRow = -1;
        menuCol = -1;
      }
  }

  /**
   *
   * Sort by column <column>, according to the direction
   * of the last sort.
   *
   */

  public void resort(int column, boolean repaint)
  {
    new rowSorter(sortForward, this, column).sort();

    if (repaint)
      {
        refreshTable();
      }
  }

  public void resort(int column, boolean forward, boolean repaint)
  {
    new rowSorter(forward, this, column).sort();

    if (repaint)
      {
        refreshTable();
      }
  }

  private rowHandle findRow(int y)
  {
    for (int i = 0; i < crossref.size(); i++)
      {
        if (((rowHandle) crossref.elementAt(i)).rownum == y)
          {
            return (rowHandle) crossref.elementAt(i);
          }
      }

    throw new RuntimeException("Couldn't find row " + y);
  }

}

/* from Fundamentals of Data Structures in Pascal,
        Ellis Horowitz and Sartaj Sahni,
        Second Edition, p.347
        Computer Science Press, Inc.
        Rockville, Maryland
        ISBN 0-88175-165-0 */

class mergeRec {


  tableRow element;
  rowHandle handle;
  mergeRec link;

  mergeRec(tableRow element, rowHandle handle)
  {
    this.element = element;
    this.handle = handle;
    link = null;
  }

  void setNext(mergeRec link)
  {
    this.link = link;
  }

  mergeRec next()
  {
    return link;
  }

}

class rowSorter {

  Vector mergeRecs;
  boolean forward;
  rowTable parent;
  int column;

  /* -- */

  public rowSorter(boolean forward, rowTable parent, int column)
  {
    this.forward = forward;
    this.parent = parent;
    this.column = column;
  }

  int compare(mergeRec a, mergeRec b)
  {
    Object Adata, Bdata;

    try
      {
        Adata = a.element.elementAt(column).getData();
      }
    catch (NullPointerException ex)
      {
        Adata = null;
      }

    try
      {
        Bdata = b.element.elementAt(column).getData();
      }
    catch (NullPointerException ex)
      {
        Bdata = null;
      }

    // Adata and/or Bdata will be null if we are just comparing
    // strings, rather than the attached integer or date values for
    // numeric or temporal sorting.  If one but not both of Adata and
    // Bdata are null, the text of the column will hopefully be
    // matching null, and we'll do the right thing by always treating
    // the null string as lesser in our sort

    if (Adata == null || Bdata == null)
      {
        String one, two;

        if (forward)
          {
            try
              {
                one = a.element.elementAt(column).text;
              }
            catch (NullPointerException ex)
              {
                one = null;
              }

            try
              {
                two = b.element.elementAt(column).text;
              }
            catch (NullPointerException ex)
              {
                two = null;
              }
          }
        else
          {
            try
              {
                one = b.element.elementAt(column).text;
              }
            catch (NullPointerException ex)
              {
                one = null;
              }

            try
              {
                two = a.element.elementAt(column).text;
              }
            catch (NullPointerException ex)
              {
                two = null;
              }
          }

        // null is always lesser

        if (one == null)
          {
            if (two == null)
              {
                return 0;
              }
            else
              {
                return -1;
              }
          }
        else if (two == null)
          {
            return 1;
          }

        // okay, neither null.

        return one.compareToIgnoreCase(two);
      }

    // if we are sorting dates, we expect everything in this column
    // to be a date

    if (Adata instanceof Date)
      {
        Date Adate = (Date) Adata;
        Date Bdate = (Date) Bdata;

        if (forward)
          {
            if (Adate.before(Bdate))
              {
                return -1;
              }
            else if (Bdate.before(Adate))
              {
                return 1;
              }
            else
              {
                return 0;
              }
          }
        else
          {
            if (Bdate.before(Adate))
              {
                return -1;
              }
            else if (Adate.before(Bdate))
              {
                return 1;
              }
            else
              {
                return 0;
              }
          }
      }

    if (Adata instanceof Integer)
      {
        int ia = ((Integer) Adata).intValue();
        int ib = ((Integer) Bdata).intValue();

        if (forward)
          {
            if (ia < ib)
              {
                return -1;
              }
            else if (ia > ib)
              {
                return 1;
              }
            else
              {
                return 0;
              }
          }
        else
          {
            if (ib < ia)
              {
                return -1;
              }
            else if (ib > ia)
              {
                return 1;
              }
            else
              {
                return 0;
              }
          }
      }

    if (Adata instanceof Double)
      {
        double da = ((Double) Adata).doubleValue();
        double db = ((Double) Bdata).doubleValue();

        if (forward)
          {
            if (da < db)
              {
                return -1;
              }
            else if (da > db)
              {
                return 1;
              }
            else
              {
                return 0;
              }
          }
        else
          {
            if (db < da)
              {
                return -1;
              }
            else if (db > da)
              {
                return 1;
              }
            else
              {
                return 0;
              }
          }
      }

    // unrecognized data type.. can't compare

    return 0;
  }

  mergeRec rmsort(int l, int u)
  {
    int mid;
    mergeRec q, r;

    //    System.err.println("rmsort: low " + l + ", high " + u);

    if (l >= u)
      {
        return (mergeRec) mergeRecs.elementAt(l);
      }
    else
      {
        mid = (l + u) / 2;
        q = rmsort(l, mid);
        r = rmsort(mid+1, u);
        return rmerge(q,r);
      }
  }

  mergeRec rmerge(mergeRec u, mergeRec y)
  {
    mergeRec p1, p2, px, result, node;

    /* -- */

    p1 = u;
    p2 = y;
    result = null;

    if (compare(p1, p2) <= 0)
      {
        result = p1;
        p1 = p1.next();
        result.setNext(null);
      }
    else
      {
        result = p2;
        p2 = p2.next();
        result.setNext(null);
      }

    node = result;

    while (node != null && (p1 != null || p2 != null))
      {
        if (p1 == null || compare(p1,p2) > 0)
          {
            node.setNext(p2);

            if (p2 != null)
              {
                px = p2.next();
                p2.setNext(null);
                p2 = px;
              }
          }
        else
          {
            px = p1.next();
            node.setNext(p1);
            p1.setNext(null);
            p1 = px;
          }

        node = node.next();
      }

    return result;
  }

  public void sort()
  {
    if (parent.rows.size() < 2)
      {
        return;
      }

    mergeRecs = new Vector();

    // System.err.println("Creating mergeRecs");

    for (int i = 0; i < parent.rows.size(); i++)
      {
        mergeRecs.addElement(new mergeRec((tableRow)parent.rows.elementAt(i),
                                          (rowHandle)parent.crossref.elementAt(i)));
      }

    // System.err.println("Sorting from element " + 0 + " to " + (mergeRecs.size()-1));

    mergeRec result = rmsort(0, mergeRecs.size()-1);

    //  System.err.println("Toplevel sorted, fixing crossrefs");

    for (int i = 0; i < parent.rows.size(); i++)
      {
        parent.rows.setElementAt(result.element, i);
        parent.crossref.setElementAt(result.handle, i);
        result.handle.rownum = i;

        result = result.next();
      }

    parent.reCalcRowPos(0);             // recalc vertical positions
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       rowHandle

This class is used to map a hash key to a position in the table.

------------------------------------------------------------------------------*/

class rowHandle {

  Object
    key;

  int
    rownum;

  public rowHandle(rowTable parent, Object key)
  {
    parent.addRow(false);       // don't repaint table
    rownum = parent.rows.size() - 1;
    this.key = key;

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
