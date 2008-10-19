/*
   SmartTable.java

   This Module encapsulates user interactions with a Jtable, including
   right click menus to sort and remove columns

   Created: 14 December 2005
   Last Commit: $Format:%cd$

   Module By: James Ratcliff, falazar@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2008
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.JTable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PrinterException;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.client.gResultTable;


/*------------------------------------------------------------------------------
                                                                           class
                                                                      SmartTable

------------------------------------------------------------------------------*/

/**
 * Extending upon the JTable, adding the ability to Sort Columns with
 * the TableSorter Class, Adding Right click context menu's for the
 * header row and the data rows.  Re-created a Optimize Column Widths
 * with the TextAreaRenderer class to make each cell a TextArea.
 */

public class SmartTable extends JPanel implements ActionListener
{
  static final boolean debug = true;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JTable.SmartTable");

  /**
   * Date pattern for rendering dates in the table.
   */

  static final String datePattern = SmartTable.ts.l("getTableCellRendererComponent.datePattern"); // "M/d/yyyy"

  // ---

  /**
   * The GUI table component.
   */

  public JTable table = null;
  private MyTableModel myModel;
  private TableSorter sorter = null;

  /**
   * Hashable index for selecting rows by key field
   */

  private Map<Object, Integer> index;
  private gResultTable gResultT;

  // Header Menus for right click popup

  private JMenuItem menuTitle     = new JMenuItem(ts.l("init.menu_title"));       // "Column Menu"
  private JMenuItem deleteColMI   = new JMenuItem(ts.l("init.del_col"));          // "Delete This Column"
  private JMenuItem optimizeColWidMI = new JMenuItem(ts.l("init.opt_col_widths"));// "Optimize Column Widths"

  // vars to remember to pass into the mouse actions

  private int remember_row;
  private int remember_col;
  private int remember_col2;

  /* -- */

  public SmartTable(JPopupMenu rowMenu, String[] columnValues, gResultTable gResultT)
  {
    if (debug)
      {
	System.err.println("DEBUG: SmartTable Constructor");
      }

    this.setLayout(new BorderLayout());

    index = new HashMap<Object, Integer>();
    this.gResultT = gResultT;

    myModel = new MyTableModel(columnValues);
    sorter = new TableSorter(myModel);
    table = new JTable(sorter);
    sorter.setTableHeader(table.getTableHeader());
    table.setPreferredScrollableViewportSize(new Dimension(500, 70));

    // Allows horizontal scrolling - Lets all cols be about 100 px as opposed to fitting panel
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // Fix to display dates nicely
    table.setDefaultRenderer(Date.class, new DateCellRenderer());

    addPopupMenus(rowMenu);

    // Sort Function Call Here - default first field ASC
    sorter.setSortingStatus(0, 1);

    // If a column size is changed, turn on text-wrapping for that and
    // next column.

    JTableHeader header = table.getTableHeader();
    TableColumnModel colModel = header.getColumnModel();
    colModel.addColumnModelListener(new myColModelListener());

    JScrollPane scrollPane = new JScrollPane(table);
    this.add(scrollPane);

    table.addAncestorListener(new SmartTableAncestorListener());
    this.addComponentListener(new SmartTableComponentListener());
  }

  /**
   * Add right click menus for header, and row
   * @param rowMenu popup menu passed in from parent, actionListener added here
   */

  public void addPopupMenus(JPopupMenu rowMenu)
  {
    JPopupMenu headerMenu = new JPopupMenu();

    headerMenu.add(menuTitle);
    headerMenu.addSeparator();

    headerMenu.add(deleteColMI);
    headerMenu.add(optimizeColWidMI);
    deleteColMI.addActionListener(this);
    optimizeColWidMI.addActionListener(this);

    table.getTableHeader().addMouseListener(new PopupListener(this, headerMenu));

    if (rowMenu != null)
      {
	Component elements[];
	JMenuItem temp;

	elements = rowMenu.getComponents();

	for (int i = 0; i < elements.length; i++)
	  {
	    if (elements[i] instanceof JMenuItem)
	      {
		temp = (JMenuItem) elements[i];

		// if there is a listener already, dont add another
		// one

		if (temp.getActionListeners().length == 0)
		  {
		    temp.addActionListener(this);
		  }
	      }
	  }
      }

    table.addMouseListener(new PopupListener(this,rowMenu));
  }

  /**
   * Function for the Toolbar, Rightclick Row Menus, called from
   * popuplistener
   */

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() instanceof JMenuItem)
      {
	JMenuItem eventSource = (JMenuItem) event.getSource();
	Container parentContainer = eventSource.getParent();

	if (parentContainer instanceof JPopupMenu)
	  {
	    if (event.getSource() == deleteColMI)
	      {
		if (debug)
		  {
		    System.out.println("mouseevent remove col:" + remember_col2 + "*");
		  }

		table.removeColumn(table.getColumnModel().getColumn(remember_col2));
		gResultT.used[remember_col2] = false;
		fixTableColumns();
	      }
	    else if (event.getSource() == optimizeColWidMI)
	      {
		if (debug)
		  {
		    System.out.println("mouseevent optimize all columns ");
		  }

		optimizeCols();
	      }
	    else // pass back to parent to deal with Row menu actions
	      {
		Object key = getRowKey(sorter.modelIndex(remember_row)); // get real key from sorter model

		if (debug)
		  {
		    System.err.println("actionPerformed processing hash key: row=" + remember_row + ", invid=" + key);
		  }

		gResultT.rowMenuPerformed(key, event);
	      }
	  }
      }
  }

  /**
   * Optimize the columnWidths on start
   */

  public void fixTableColumns()
  {
    int colCount = table.getColumnCount();

    if (colCount == 0) 
      {
	return;
      }

    // default width is 75, if not default, use getPreferredWidth()
    int colWidth = table.getColumnModel().getColumn(0).getPreferredWidth();

    // Get Table Size, then get Container size, if table smaller than
    // container, stretch table out to fit

    if (colWidth*colCount < table.getParent().getWidth())
      {
	table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
      }
    else
      {
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
    if (debug)
      {
	System.err.println("baseTable.optimizeCols(): entering");
      }

    int nominalWidth[];
    float totalOver, spareSpace;

    float 
      percentSpace,
      shrinkFactor,
      percentOver,
      growthFactor;

    float redistribute = (float) 0.0;

    /* -- */

    // Set Each Column to Auto-Wrap text if needed

    for (int i=0; i < table.getColumnCount(); i++)
      {
	if (gResultT.used[i])
	  {
	    setColumnTextWrap(i);
	  }
      }

    /*
      This method uses the following variables to do its calculations.

      nominalWidth[] - An array of ints holding the width needed by each column.

      totalOver - the aggregate amount of horizontal space that the columns are short,
                  in the absence of wordwrapping.

      spareSpace - the aggregate amount of horizontal space that the columns have to
                   spare.

    */

    if (debug)
      {
	System.err.println("this.getBounds().width = " + this.getBounds().width);
      }

    nominalWidth = new int[table.getColumnCount()];
    totalOver = (float) 0.0;
    spareSpace = (float) 0.0;

    for (int i = 0; i < table.getColumnCount(); i++)
      {
	if (!gResultT.used[i])
	  {
	    continue;
	  }

	if (debug)
	  {
	    System.err.println("Examining column " + i);
	  }

	TableColumn col = table.getColumnModel().getColumn(i);
	TextAreaRenderer renderer = (TextAreaRenderer) col.getCellRenderer();

	nominalWidth[i] = 20;

	for (int j = 0; j < myModel.getRowCount(); j++)
	  {
	    Object value = myModel.getValueAt(j, i);

	    int localNW = renderer.getUnwrappedWidth(this.table, value) + 5;

	    if (localNW > nominalWidth[i])
	      {
		nominalWidth[i] = localNW;
	      }
	  }

	// nominalWidth[i] is now the required width of this column

	if (debug)
	  {
	    System.err.println("Column " + i + " has nominalWidth of " + nominalWidth[i] +
			       " and a cellWidth of " + col.getWidth());
	  }

	if (nominalWidth[i] < col.getWidth())
	  {
	    spareSpace += col.getWidth() - nominalWidth[i];
	  }
	else
	  {
	    totalOver += (float) nominalWidth[i] - col.getWidth();
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

    for (int i = 0; i < table.getColumnCount(); i++)
      {
	if (!gResultT.used[i])
	  {
	    continue;
	  }

	TableColumn col = table.getColumnModel().getColumn(i);

	// are we going to be actually doing some redistributing?

	if (redistribute > 1.0)
	  {
	    // Does this column have space to give?

	    if (nominalWidth[i] < col.getWidth())
	      {
		percentSpace = (col.getWidth() - nominalWidth[i]) / spareSpace;
		shrinkFactor = redistribute * percentSpace;

		if (debug)
		  {
		    System.err.println("Column " + i + ": percentSpace = " + percentSpace +
				       " , reducing by " + shrinkFactor + ", new width = " +
				       (col.getWidth() - shrinkFactor));
		  }

		col.setPreferredWidth((int) (col.getWidth() - shrinkFactor));
	      }
	    else // need to grow
	      {
		// what percentage of the overage goes to this col?

		percentOver = (nominalWidth[i] - col.getWidth()) / totalOver; 
		growthFactor = redistribute * percentOver;

		if (debug)
		  {
		    System.err.println("Column " + i + ": percentOver = " + percentOver + 
				       " , growing by " + growthFactor + ", new width = " + 
				       (col.getWidth() + growthFactor));
		  }

		col.setPreferredWidth((int) (col.getWidth() + growthFactor));
	      }
	  }
      }
  }

  /**
   * Turn on text wrapping if the column is not a Date class.
   * this appears to be using physical instead of Index position.
   */

  private void setColumnTextWrap(int colIndex)
  {
    String colClass = myModel.getColumnClass(colIndex).toString();

    if (debug)
      {
	System.err.println("setColumnTextWrap(" + colIndex + ") = " + colClass);
      }

    if (!colClass.equals("class java.util.Date"))
      {
	TableColumnModel cmodel = table.getColumnModel();
	TextAreaRenderer textAreaRenderer = new TextAreaRenderer();

	try
	  {
	    int physPos = myModel.getPhysicalColumnPos(colIndex);

	    cmodel.getColumn(physPos).setCellRenderer(textAreaRenderer);
	  }
	catch (IndexOutOfBoundsException ex)
	  {
	  }
      }
  }

  /**
   * Print the JTable WYSIWYG in landscape mode
   */

  public void print()
  {
    try
      {
	Printable printable = table.getPrintable(JTable.PrintMode.FIT_WIDTH,
						 null,
						 new MessageFormat(ts.l("print.page_template"))); // Page - {0}

	PrinterJob job = PrinterJob.getPrinterJob();

	job.setPrintable(printable);

	PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();

	if (job.printDialog(attr))
	  {
	    job.print(attr);
	  }
      }
    catch (PrinterException pe)
      {
	System.err.println("Error printing: " + pe.getMessage());
      }
  }

  /**
   * Gets a key value from the row at rownum
   *
   * @param rownum integer value of the row number
   */

  public Object getRowKey(int rownum)
  {
    return myModel.getRowHandler(rownum).key;
  }

  /**
   * Creates a new row, adds it to the hashtable
   *
   * @param key A hashtable key to be used to refer to this row in the future
   */

  public void newRow(Object key)
  {
    myModel.newRow(key);
  }

  public void clearRows()
  {
    myModel.clearRows();
  }

  public void refresh()
  {
    myModel.fireTableDataChanged();
  }

  /**
   * Sets the contents of a cell in the table.
   *
   * @param key key to the row of the cell to be changed
   * @param col column of the cell to be changed
   * @param data A piece of data to be held with this cell, will be used for sorting
   */

  public final void setCellValue(Object key, int col, Object value)
  {
    myModel.setCellValue(key, col, value);
  }

  /**
   * Gets the contents of a cell in the table.
   *
   * @param key key to the row of the cell
   * @param col column of the cell
   */

  public final Object getCellValue(Object key, int col)
  {
    return myModel.getCellValue(key, col);
  }

  /**
   * pass in and set entire array of column names, or column headers
   */

  public void setColumnNames(int columnCnt, String[] columns)
  {
    myModel.setColumnNames(columnCnt, columns);
  }

  /**
   * Erases all the cells in the table and removes any per-cell
   * attribute sets.
   */

  public void clearCells()
  {
    index = new HashMap<Object, Integer>();
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                              myColModelListener

  ----------------------------------------------------------------------------*/

  /**
   * The listener class for the table column model.
   */

  public class myColModelListener implements TableColumnModelListener
  {
    public void columnAdded(TableColumnModelEvent e)
    {
    }

    public void columnMarginChanged(ChangeEvent e)
    {
      TableColumn tc = table.getTableHeader().getResizingColumn();

      if (tc != null)
	{
	  int colIndex = tc.getModelIndex();
	  int colIndex2 = getNextColumnIndex(colIndex);

	  setColumnTextWrap(colIndex);

	  if (colIndex2 != -1)
	    {
	      setColumnTextWrap(colIndex2);
	    }
	}
    }

    /**
     * Get the next physical columns index number. Needed for when
     * columns are moved, their indexes remain the same.
     */

    private int getNextColumnIndex(int colIndex)
    {
      TableColumnModel colModel = table.getTableHeader().getColumnModel();

      // get list of columns in physical order.

      Enumeration e2 = colModel.getColumns();
      int i = 0;
      int colIndex2 = -1;
      TableColumn tc2 = null;

      while (e2.hasMoreElements() && colIndex2 != colIndex)
	{
	  tc2 = (TableColumn)e2.nextElement();
	  colIndex2 = tc2.getModelIndex();

	  i++;
	}

      if (e2.hasMoreElements())
	{
	  tc2 = (TableColumn)e2.nextElement();
	  colIndex2 = tc2.getModelIndex();

	  return colIndex2;
	}
      else
	{
	  return -1;
	}
    }

    public void columnMoved(TableColumnModelEvent e)
    {
    }

    public void columnRemoved(TableColumnModelEvent e)
    {
    }

    public void columnSelectionChanged(ListSelectionEvent e)
    {
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                     SmartTableComponentListener

  ----------------------------------------------------------------------------*/

  /**
   * listener for when the main panel is resized
   */

  public class SmartTableComponentListener implements ComponentListener
  {
    public void componentResized(ComponentEvent e)
    {
      fixTableColumns();
    }

    public void componentMoved(ComponentEvent e)
    {
    }

    public void componentShown(ComponentEvent e)
    {
    }

    public void componentHidden(ComponentEvent e)
    {
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                      SmartTableAncestorListener

  ----------------------------------------------------------------------------*/

  /**
   * Class to assist with FixTable Columns, allowing it to be called
   * AFTER the table is drawn, to get in the correct Table and Panel
   * size to match with
   */

  class SmartTableAncestorListener implements AncestorListener
  {
    public void ancestorAdded(AncestorEvent e)
    {
      fixTableColumns();
    }

    public void ancestorMoved(AncestorEvent e)
    {
    }

    public void ancestorRemoved(AncestorEvent e)
    {
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                                   Popuplistener

  ----------------------------------------------------------------------------*/

  /**
   *  Mouse class to control the right click menus
   */

  class PopupListener extends MouseAdapter
  {
    SmartTable master_control;
    JPopupMenu popMenu;

    /* -- */

    public PopupListener(SmartTable master, JPopupMenu popMenu)
    {
      this.master_control = master;
      this.popMenu = popMenu;
    }

    public void mousePressed(MouseEvent e)
    {
      // on some platforms, popups are triggered on mousedown

      showPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
      // on others, on up

      showPopup(e);
    }

    private void showPopup(MouseEvent e)
    {
      if (!e.isPopupTrigger())
	{
	  return;
	}

      if (debug)
	{
	  System.out.println("mouseevent on row:"+ table.rowAtPoint(e.getPoint()) +"*");
	  System.out.println("mouseevent on col:"+ table.columnAtPoint(e.getPoint()) +"*");
	}

      master_control.remember_row = master_control.table.rowAtPoint(e.getPoint());
      master_control.remember_col = master_control.remember_col2 = master_control.table.columnAtPoint(e.getPoint());

      // if table is altered by moving or deleting a column, get true
      // column number here

      if (e.getSource() instanceof JMenuItem)
	{
	  JTableHeader h = (JTableHeader) e.getSource();
	  TableColumnModel columnModel = h.getColumnModel();

	  int viewColumn = columnModel.getColumnIndexAtX(e.getX());
	  int column = columnModel.getColumn(viewColumn).getModelIndex();

	  if (debug)
	    {
	      System.out.println("second way is col:"+ column +"*");
	    }

	  master_control.remember_col = column;
	}

      // Select a single row for right clicks - rows 1 to 1

      table.addRowSelectionInterval(master_control.remember_row, master_control.remember_row);

      popMenu.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                                    MyTableModel

  ----------------------------------------------------------------------------*/

  /**
   * New Class added in to help define the table results.
   */

  class MyTableModel extends AbstractTableModel
  {
    private final boolean DEBUG = true;
    public Vector rows; // vector of rowHandle objects, holds actual data cells
    private String[] columnNames;
    private Class[] columnClasses;

    /* -- */

    public MyTableModel(String[] columnValues)
    {
      rows = new Vector();
      columnNames = columnValues;
      columnClasses = new Class[columnValues.length];
    }

    public int getColumnCount()
    {
      return columnNames.length;
    }

    public int getRowCount()
    {
      return rows.size();
    }

    public void newRow(Object key)
    {
      if (index.containsKey(key))
	{
	  throw new IllegalArgumentException("newRow(): row " + key + " already exists.");
	}

      rowHandler newRow1 = new rowHandler(key, getColumnCount());
      rows.add(newRow1);
      index.put(key, new Integer(rows.size()-1));
    }

    public void clearRows()
    {
      index = new HashMap<Object, Integer>(); // Clear Keys
      rows = new Vector();     // Clear Rows
    }

    public String getColumnName(int col)
    {
      return columnNames[col];
    }

    // pass in and set entire array of column names, or column headers
    public void setColumnNames(int columnCnt, String[] columns)
    {
      columnNames = new String[columnCnt];
      columnNames = columns;
    }

    public Object getValueAt(int row, int col)
    {
      return getRowHandler(row).cells[col];
    }

    public void setValueAt(Object value, int row, int col)
    {
      if (value != null && columnClasses[col] == null)
	{
	  columnClasses[col] = value.getClass();
	}

      getRowHandler(row).cells[col] = value;
    }

    /**
     * Sets the contents of a cell in the table.
     *
     * @param key key to the row of the cell to be changed
     * @param col column of the cell to be changed
     * @param data A piece of data to be held with this cell, will be used for sorting
     */

    public final void setCellValue(Object key, int col, Object value)
    {
      Integer row = index.get(key);
      int row2 = row.intValue();
      setValueAt(value, row2, col);
    }

    /**
     * Gets the contents of a cell in the table.
     *
     * @param key key to the row of the cell
     * @param col column of the cell
     */

    public final Object getCellValue(Object key, int col)
    {
      Integer row = index.get(key);
      int row2 = row.intValue();
      return getValueAt(row2, col);
    }

    public rowHandler getRowHandler(int row)
    {
      return (rowHandler) rows.elementAt(row);
    }

    /**
     * JTable uses this method to determine the default renderer/
     * editor for each cell.
     */

    public Class getColumnClass(int c)
    {
      if (columnClasses[c] == null)
	{
	  return Object.class;
	}
      else
	{
	  return columnClasses[c];
	}
    }

    /**
     * Don't need to implement this method unless your table's
     * editable.
     */

    public boolean isCellEditable(int row, int col)
    {
      return false; // For our tables, all columns are uneditable
      // Note that the data/cell address is constant,
      // no matter where the cell appears onscreen.
    }

    private void printDebugData()
    {
      int numRows = getRowCount();
      int numCols = getColumnCount();

      System.out.println("numCols = " + numCols);

      for (int i=0; i < numRows; i++)
	{
	  System.out.print("    row " + i + ":");

	  for (int j=0; j < numCols; j++)
	    {
	      System.out.print("  " + getValueAt(i,j));
	    }

	  System.out.println();
	}

      System.out.println("--------------------------");
    }

    /**
     * Gets a physical column's position number from a TableModel
     * column index.
     *
     * Needed because colums can be physically slid around by the
     * user, while the TableModel column indexes do not change.
     *
     * @returns The physical index of TableModel column colIndex
     * @throws IndexOutOfBoundsException if colIndex is out of range
     */

    public int getPhysicalColumnPos(int colIndex)
    {
      int i = 0;
      Enumeration columns = table.getTableHeader().getColumnModel().getColumns();

      while (columns.hasMoreElements())
	{
	  TableColumn col = (TableColumn) columns.nextElement();

	  if (col.getModelIndex() == colIndex)
	    {
	      return i;
	    }

	  i++;
	}

      throw new IndexOutOfBoundsException();
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                                DateCellRenderer

  ----------------------------------------------------------------------------*/

  /**
   * A cell renderer for Date values.
   */

  private class DateCellRenderer extends TextAreaRenderer
  {
    /**
     * Cached FontMetrics object, used to calculate the necessary width
     * for a specific string in the SmartTable's optimizeCols()
     * method.
     */

    private FontMetrics metrics = null;

    /* -- */

    /**
     * Returns the component that is used for rendering the value.
     *
     * @param table the JTable
     * @param value the value of the object
     * @param isSelected is the cell selected?
     * @param hasFocus has the cell the focus?
     * @param row the row to render
     * @param column the cell to render
     *
     * @return this component (the default table cell renderer)
     */

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus, int row,
                                                   int column)
    {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                          row, column);
      if (value instanceof Date)
        {
          Date dateValue = (Date) value;
	  
	  setText(getString(dateValue));
        }

      return this;
    }

    /**
     * Returns the necessary width required to render value with this
     * renderer, given the table's defined font.
     */

    public int getUnwrappedWidth(JTable table, Object value)
    {
      if (value == null)
	{
	  return 0;
	}

      if (metrics == null)
	{
	  metrics = this.getFontMetrics(table.getFont());
	}

      Date dateValue = (Date) value;

      return metrics.stringWidth(getString(dateValue));
    }

    private String getString(Date dateValue)
    {
      return new SimpleDateFormat(SmartTable.datePattern).format(dateValue);
    }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      rowHandler

------------------------------------------------------------------------------*/

/**
 * This class is used to map a hash key to a position in the table.
 */

class rowHandler
{
  Object key;
  Object[] cells;

  public rowHandler(Object key, int columns)
  {
    cells = new Object[columns];
    this.key = key;
  }
}
