/*

  rowTable.java

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

  Created: 14 June 1996
  Version: $Revision: 1.20 $ %D%
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.JTable;

import arlut.csd.Util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        rowTable

------------------------------------------------------------------------------*/

/**
 * <p>rowTable is a specialized baseTable, supporting a per-row
 * access model based on a hashtable.
 *
 *
 * @see arlut.csd.Table.baseTable
 * @author Jonathan Abbey
 * @version $Revision: 1.20 $ %D% 
 */

public class rowTable extends baseTable implements ActionListener {

  Hashtable 
    index;

  Vector
    crossref;

  rowSelectCallback
    callback;

  PopupMenu rowMenu;
  MenuItem SortByMI;
  MenuItem RevSortByMI;
  MenuItem DeleteColMI;
  MenuItem OptimizeMI;

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
   * @param callback    reference to an object that implements the rowSelectCallback interface
   * @param menu  reference to a popup menu to be associated with rows in this table
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
		  PopupMenu menu)
  {
    super(headerAttrib, tableAttrib, colAttribs, colWidths,
	  vHeadLineColor, vRowLineColor, hHeadLineColor, hRowLineColor,
	  headers, horizLines, vertLines, vertFill, hVertFill, 
	  menu, null);
    
    rowMenu = new PopupMenu("Column Menu");
    SortByMI = new MenuItem("Sort By This Column");
    RevSortByMI = new MenuItem("Reverse Sort By This Column");
    DeleteColMI = new MenuItem("Delete This Column");
    OptimizeMI = new MenuItem("Optimize Column Widths");

    rowMenu.add(SortByMI);
    rowMenu.add(RevSortByMI);

    if (colWidths.length > 1)
      {
	rowMenu.add(DeleteColMI);
	rowMenu.add(OptimizeMI);
      }

    SortByMI.addActionListener(this);
    RevSortByMI.addActionListener(this);
    DeleteColMI.addActionListener(this);
    OptimizeMI.addActionListener(this);
    
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
		  PopupMenu menu)
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
	 horizLines, true, true, false, callback, menu);

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
		  PopupMenu menu)
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
	 true, true, true, false, callback, menu);

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

  public synchronized void clickInCell(int x, int y)
  {
    rowHandle 
      element = null;

    /* -- */

    for (int i = 0; i < crossref.size(); i++)
      {
	if (((rowHandle) crossref.elementAt(i)).rownum == y)
	  {
	    element = (rowHandle) crossref.elementAt(i);
	  }
      }

    if (!testRowSelected(y))
      {
	// unselect the currently selected row, if any.  Note that we
	// are currently only supporting single row selection.

	for (int i = 0; i < rows.size(); i++)
	  {
	    if (testRowSelected(i))
	      {
		unSelectRow(i);
		if (callback != null)
		  {
		    // if we get a nullpointer exception on
		    // element here, it means that the tableCanvas
		    // code didn't properly check to make sure that
		    // the location clicked on corresponded to
		    // a proper row
		    callback.rowUnSelected(((rowHandle)crossref.elementAt(i)).key, true);
		  }
	      }
	  }

	selectRow(y);
	refreshTable();

	if (callback != null)
	  {
	    callback.rowSelected(element.key);
	  }
      }
    else
      {
	// go ahead and deselect the current row
	
	unSelectRow(y);
	refreshTable();

	if (callback != null)
	  {
	    callback.rowUnSelected(element.key, false);
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

    for (int i = 0; i < crossref.size(); i++)
      {
	if (((rowHandle) crossref.elementAt(i)).rownum == y)
	  {
	    element = (rowHandle) crossref.elementAt(i);
	  }
      }

    if (testRowSelected(y))
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
   * Unselect all cells
   */

  public void unSelectAll()
  {
    for (int i = 0; i < rows.size(); i++)
      {
	if (testRowSelected(i))
	  {
	    unSelectRow(i);
	    if (callback != null)
	      {
		// if we get a nullpointer exception on
		// element here, it means that the tableCanvas
		// code didn't properly check to make sure that
		// the location clicked on corresponded to
		// a proper row
		callback.rowUnSelected(((rowHandle)crossref.elementAt(i)).key, true);
	      }
	  }
      }

    refreshTable();
  }

  public Object getSelectedRow()
  {
    if (selectedRow == -1)
      {
	return null;
      }

    for (int i = 0; i < crossref.size(); i++)
      {
	if (((rowHandle) crossref.elementAt(i)).rownum == selectedRow)
	  {
	    return ((rowHandle) crossref.elementAt(i)).key;
	  }
      }

    return null;
  }

  /**
   * Erases all the cells in the table and removes any per-cell
   * attribute sets.  
   */
  public void clearCells()
  {
    index = new Hashtable();
    crossref = new Vector();
    super.clearCells();
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

    element = new rowHandle(this, key);

    index.put(key, element);
    reShape();
  }

  /**
   * Deletes a row
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

    if (menuRow == -1)
      {
	if (e.getSource() == DeleteColMI)
	  {
	    this.deleteColumn(menuCol, true);
	    refreshTable();
	    return;
	  }

	if (e.getSource() == SortByMI)
	  {
	    resort(menuCol, true, true);
	  }
	else if (e.getSource() == RevSortByMI)
	  {
	    resort(menuCol, false, true);
	  }
	else if (e.getSource() == OptimizeMI)
	  {
	    optimizeCols();
	    refreshTable();
	  }
      }

    for (int i = 0; i < crossref.size(); i++)
      {
	if (((rowHandle) crossref.elementAt(i)).rownum == menuRow)
	  {
	    element = (rowHandle) crossref.elementAt(i);
	  }
      }
    
    // perform our callback

    if (element != null)
      {
	callback.rowMenuPerformed(element.key, e);
      }

    // clear our lastpopped menu row, col

    menuRow = -1;
    menuCol = -1;

  }

  public void resort(int column, boolean forward, boolean repaint)
  {
    new rowSorter(forward, this, column).sort();

    if (repaint)
      {
	refreshTable();
      }
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

    Adata = a.element.elementAt(column).getData();
    Bdata = b.element.elementAt(column).getData();

    if (Adata == null || Bdata == null)
      {
	if (forward)
	  {
	    return a.element.elementAt(column).text.compareTo(b.element.elementAt(column).text);
	  }
	else
	  {
	    return b.element.elementAt(column).text.compareTo(a.element.elementAt(column).text);
	  }
      }

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

    //    int count1, count2;
    //
    //    count1 = 1;
    //    count2 = 1;
    //
    //    node = u;
    //
    //    while (node.next() != null)
    //      {
    //	node = node.next();
    //	count1++;
    //      }
    //
    //    node = y;
    //
    //    while (node.next() != null)
    //      {
    //	node = node.next();
    //	count2++;
    //      }

    //    System.err.println("Merging chains: u [" + count1 + "], y [" + count2 + "]");

    if (compare(p1, p2) <= 0)
      {
	//	System.err.println("Starting chain on p1 " + p1.element.elementAt(column).text);
	result = p1;
	p1 = p1.next();
	result.setNext(null);
      }
    else
      {
	//	System.err.println("Starting chain on p2 " + p1.element.elementAt(column).text);
	result = p2;
	p2 = p2.next();
	result.setNext(null);
      }

    node = result;
    
    while ((p1 != null) || (p2 != null))
      {
	//	if ((p1 != null) && (p2 != null))
	//	  {
	//	    System.err.println("Looping : " + p1.element.elementAt(column).text + " and " +
	//			       p2.element.elementAt(column).text);
	//	  }

	if (p1 == null)
	  {
	    //	    System.err.println("Force Linking p2: " + p2.element.elementAt(column).text);
	    px = p2.next();
	    node.setNext(p2);
	    p2.setNext(null);
	    p2 = px;
	  }
	else if (p2 == null)
	  {
	    //	    System.err.println("Force Linking p1: " + p1.element.elementAt(column).text);
	    px = p1.next();
	    node.setNext(p1);
	    p1.setNext(null);
	    p1 = px;
	  }
	else if (compare(p1,p2) <= 0)
	  {
	    //	    System.err.println("Linking p1: " + p1.element.elementAt(column).text);
	    px = p1.next();
	    node.setNext(p1);
	    p1.setNext(null);
	    p1 = px;
	  }
	else
	  {
	    //	    System.err.println("Linking p2: " + p2.element.elementAt(column).text);
	    px = p2.next();
	    node.setNext(p2);
	    p2.setNext(null);
	    p2 = px;
	  }

	node = node.next();
      }

    //    System.err.println("Finished merge process");

    //    count1 = 1;

    //    node = result;

    //    while (node.next() != null)
    //      {
    //	node = node.next();
    //	count1++;
    //      }

    //    System.err.println("Chains merged: total length = [" + count1 + "]");

    return result;
  }

  public void sort()
  {
    if (parent.rows.size() < 2)
      {
	return;
      }

    mergeRecs = new Vector();
    
    //    System.err.println("Creating mergeRecs");

    for (int i = 0; i < parent.rows.size(); i++)
      {
	mergeRecs.addElement(new mergeRec((tableRow)parent.rows.elementAt(i),
					  (rowHandle)parent.crossref.elementAt(i)));
      }

    //    System.err.println("Sorting from element " + 0 + " to " + (mergeRecs.size()-1));
    
    mergeRec result = rmsort(0, mergeRecs.size()-1);

    //    System.err.println("Toplevel sorted, fixing crossrefs");

    for (int i = 0; i < parent.rows.size(); i++)
      {
	parent.rows.setElementAt(result.element, i);
	parent.crossref.setElementAt(result.handle, i);
	result.handle.rownum = i;

	result = result.next();
      }

    parent.reCalcRowPos(0);		// recalc vertical positions
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
    parent.addRow(false);	// don't repaint table
    rownum = parent.rows.size() - 1; // we always add the new row to the end

    // System.err.println("New rowHandle created, key = " + key + ", num = " + rownum);

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
