/*
   SmartTable.java

   This Module encapsulates some more user interactions with a Jtable, including
   right click menus to sort and remove columns
   
   Created: 14 December 2005
   Last Mod Date: $Date: 2006-01-02 16:39:01 -0600 (Mon, 02 Jan 2006) $
   Last Revision Changed: $Rev: 7283 $
   Last Changed By: $Author: falazar $
   SVN URL: $HeadURL: https://tools.arlut.utexas.edu/svn/ganymede/branches/falazar_playground/src/ganymede/arlut/csd/JTable/SmartTable.java $

   Module By: James Ratcliff, falazar@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2005
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

import arlut.csd.ganymede.common.Invid;  // test debug


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension; 
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
import java.util.Hashtable;
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


/*------------------------------------------------------------------------------
                                                                           class
                                                                      smartTable
------------------------------------------------------------------------------*/
/**
 * Extending upon the JTable, adding the ability to Sort Columns with the 
 * TableSorter Class, Adding Right click context menu's for the header row and 
 * the data rows.  Re-created a Optimize Column Widths with the TextAreaRenderer 
 * class to make each cell a TextArea.
 * 
 */
public class SmartTable extends JPanel implements ActionListener
{
  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */
  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JTable.SmartTable");

  /**
   * The GUI table component.
   */
  public JTable table = null;
  private MyTableModel myModel;
  private TableSorter sorter = null;
  
  /**
   * Hashable index for selecting rows by key field
   */
  private Hashtable index; 
  private rowSelectCallback callback;

  // Header Menus for right click popup
  private JMenuItem menuTitle     = new JMenuItem(ts.l("init.menu_title"));       // "Column Menu"
  private JMenuItem deleteColMI   = new JMenuItem(ts.l("init.del_col"));          // "Delete This Column"
  private JMenuItem optimizeColWidMI = new JMenuItem(ts.l("init.opt_col_widths"));// "Optimize Column Widths"
  
  // vars to remember to pass into the mouse actions
  private int remember_row;
  private int remember_col;
  private int remember_col2;


  // constructor function
  public SmartTable(JPopupMenu rowMenu, String[] columnValues, rowSelectCallback callback)
  {
    if (debug) System.err.println("DEBUG: SmartTable Constructor");
    this.setLayout(new BorderLayout());

    index = new Hashtable();
    this.callback = callback; 

    // Create and set up the results content pane.
    myModel = new MyTableModel(columnValues);
    sorter = new TableSorter(myModel); 
    table = new JTable(sorter);             
    sorter.setTableHeader(table.getTableHeader()); 
    table.setPreferredScrollableViewportSize(new Dimension(500, 70)); // no real effect    

    // Allows horizontal scrolling - Lets all cols be about 100 px as opposed to fitting panel
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    // Fix to display dates nicely
    table.setDefaultRenderer(Date.class, new DateCellRenderer());

    // adds the rightclick menu's for header and rows
    addPopupMenus(rowMenu);  

    // Sort Function Call Here - default first field ASC
    sorter.setSortingStatus(0, 1); 

    // If a column size is changed, turn on text-wrapping for that and next column.
    JTableHeader header = table.getTableHeader();
    TableColumnModel colModel = header.getColumnModel();
    colModel.addColumnModelListener(new myColModelListener());    

    JScrollPane scrollPane = new JScrollPane(table);
    this.add(scrollPane); 

    // Fix column widths, if too small for panel, stretch them out to full panel.
    table.addAncestorListener(new SmartTableAncestorListener());
    this.addComponentListener(new SmartTableComponentListener());
  } // SmartTable Constructor




  /**
   * Add right click menus for header, and row
   * @param rowMenu popup menu passed in from parent, actionListener added here
   */
  public void addPopupMenus(JPopupMenu rowMenu)
  {
    // Create Header Menus Now
    JPopupMenu headerMenu = new JPopupMenu();
      
    headerMenu.add(menuTitle);
    headerMenu.addSeparator();

    headerMenu.add(deleteColMI);
    headerMenu.add(optimizeColWidMI);
    deleteColMI.addActionListener(this);
    optimizeColWidMI.addActionListener(this);

    // add the listener specifically to the header 
    table.getTableHeader().addMouseListener(new PopupListener(this, headerMenu));
      
    // Iterate thru rowMenu, and add listener
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
	  // if there is alistener already, dont add another one
	  if (temp.getActionListeners().length == 0) temp.addActionListener(this);
	}
      }
    } // if menu !null
      
    // add the Row listener to the jtable rows 
    table.addMouseListener(new PopupListener(this,rowMenu));
  } // addPopupMenus


  // Function for the Toolbar, Rightclick Row Menus, called from popuplistener
  public void actionPerformed(ActionEvent event)
  {    
    if (event.getSource() instanceof JMenuItem)
    {
      JMenuItem eventSource = (JMenuItem) event.getSource();
      Container parentContainer = eventSource.getParent();
      
      if (parentContainer instanceof JPopupMenu)
      {
	// Header Actions
	if (event.getSource() == deleteColMI) // remove curr column from the class
	{
	  if (debug) System.out.println("mouseevent remove col:"+ remember_col2 +"*");			
	  table.removeColumn(table.getColumnModel().getColumn(remember_col2));  
	  fixTableColumns();
	}
	else if (event.getSource() == optimizeColWidMI) 
	{
	  if (debug) System.out.println("mouseevent optimize all columns ");
	  optimizeColumns();
	}
	else // pass back to parent to deal with Row menu actions
	{
	  Object key = getRowKey(sorter.modelIndex(remember_row)); // get real key from sorter model	      
	  if (debug) 
	    {
	      System.err.println("actionPerformed processing hash key: row=" + remember_row + ", invid=" + key);
	    }
	  callback.rowMenuPerformed(key, event);
	}

      } // parentContainer
    } // event.getsource
  } // actionPerformed 


  // Optimize the columnWidths on start
  public void fixTableColumns() 
  {
    // default width is 75, if not default, use getPreferredWidth()
    int colWidth = table.getColumnModel().getColumn(0).getPreferredWidth();
    int colCount = table.getColumnCount();
    //System.out.println("fixTableColumns: table cols "+colCount+" and width "+colWidth);

    // Get Table Size, then get Container size, if table smaller than container, stretch table out to fit
    if (colWidth*colCount < table.getParent().getWidth()) 
      {
	//System.out.println("Stretching out table now.");
	table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN); 
      }
    else 
      {
	//System.out.println("Turning off resize now.");
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
      }
  } // fixTableColumns


  // Optimize the columnWidths, shorten some columns, and expand longer ones if there is room
  public void optimizeColumns() 
  {
    Object cellResult; // hold single cell result of query
    int rows = table.getRowCount();
    int cols = table.getColumnCount();
    int[] columnWidths;
    int[] columnTotWidths;

    // Set Each Column to Auto-Wrap text if needed
    for (int i=0; i < cols; i++)      
    {
      setColumnTextWrap(i);
    }    

    columnWidths = new int[cols];	
    columnTotWidths = new int[cols];	
    // Now Read in all the result lines
    // Get the max textwidth for each column
    for (int i=0; i < rows; i++)
      {
	for (int j=0; j < cols; j++)
	  {
	    cellResult = table.getValueAt(i, j);          
	    if (cellResult != null)
	      {
		columnTotWidths[j] += cellResult.toString().length();
		if (columnWidths[j] < cellResult.toString().length())
		  {
		    columnWidths[j] = cellResult.toString().length();
		  }
	      }
	  }
      } // for i	
    
    TableColumnModel cmodel = table.getColumnModel();

    // FIX THIS, CANT BE SET COLMN WIDTH HERE ARG
    // 10 letters app 75px 8px per char? try
    // decrease all columns that dont need 10 letters
    int decreased = 0;
    int inccnt = 0; // count cols to increase
    for (int j=0; j < cols; j++)
      {
	//System.out.print("col j:"+j+" columnNames[j] \t\t max "+columnWidths[j]+"  total:"+columnTotWidths[j]+"/"+rows+" = "+columnTotWidths[j]/rows);
	if (columnWidths[j] <= 8) 
	  { 
	    cmodel.getColumn(j).setPreferredWidth(columnWidths[j]*9);
	    decreased += 8 - columnWidths[j];
	    //System.out.println(" shrinking col   decreased="+decreased+"   inccnt:"+inccnt);	
	  }
	//else System.out.println(" ");
	if (columnTotWidths[j]/rows > 8) inccnt++;
      } // for j
    
    
    // then increase the rest with the extra stuff
    int charadds =  (int) Math.ceil((double)decreased / (double)inccnt);
    //System.out.println(" columns that need increasing ="+inccnt+" chars avail to spread "+ decreased+" charadds:"+charadds);
    for (int j=0; j < cols; j++)
      {
	if (columnTotWidths[j]/rows > 8) 
	  {
	    cmodel.getColumn(j).setPreferredWidth((8+charadds)*9);  // add in chars here
	    //System.out.println(" increasing col j:"+j+" columnNames[j] \t\t max "+columnWidths[j]+"  total:"+columnTotWidths[j]+"/"+rows+" = "+columnTotWidths[j]/rows);
	  }
      } // for j
  } // optimizeColumns

  // Turn on text wrapping if the column is not a Date class.
  // this appears to be using physical instead of Index position.
  private void setColumnTextWrap(int colIndex)
  {
    String colClass = myModel.getColumnClass(colIndex).toString();
    if (!colClass.equals("class java.util.Date")) 
      {
	TableColumnModel cmodel = table.getColumnModel();
	TextAreaRenderer textAreaRenderer = new TextAreaRenderer();
	int physPos = myModel.getPhysicalColumnPos(colIndex);

	cmodel.getColumn(physPos).setCellRenderer(textAreaRenderer);
      }
  }
  

  // Print the JTable, WYSIWYG, print in landscape mode
  public void print() 
  {
    try
      {
	// fetch the printable
	Printable printable = table.getPrintable(JTable.PrintMode.FIT_WIDTH,
						 null,
						 new MessageFormat("Page - {0}"));

	// fetch a PrinterJob
	PrinterJob job = PrinterJob.getPrinterJob();

	// set the Printable on the PrinterJob
	job.setPrintable(printable);

	// create an attribute set to store attributes from the print dialog
	PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();

	// display a print dialog and record whether or not the user cancels it
	boolean printAccepted = job.printDialog(attr);

	// if the user didn't cancel the dialog
	if (printAccepted) 
	{
	  // do the printing (may need to handle PrinterException)
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
    myModel.fireTableDataChanged(); // redraw table
  } 

  /**
   * Sets the contents of a cell in the table.
   *
   * @param key key to the row of the cell to be changed
   * @param col column of the cell to be changed
   * @param data A piece of data to be held with this cell, will be used for sorting
   *
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

  // pass in and set entire array of column names, or column headers
  public void setColumnNames(int columnCnt, String[] columns) 
  {
    myModel.setColumnNames(columnCnt, columns);
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
    //crossref = new Vector();
    //super.clearCells();
    //rowSelectedKey = null;
  }




  /**
   * UNIMPLEMENTED API CALL!
   * Deletes a row.
   *
   * @param key A hashtable key for the row to delete
   * @param repaint true if the table should be redrawn after the row is deleted
   */

  public void deleteRow(Object key, boolean repaint)
  {
//     rowHandler element;

//     /* -- */

//     if (!index.containsKey(key))
//       {
// 	// no such row exists.. what to do?
// 	return;
//       }

//     if (key.equals(rowSelectedKey))
//       {
// 	unSelectRow();
//       }

//     element = (rowHandler) index.get(key);

//     index.remove(key);

//     // delete the row from our parent..

//     super.deleteRow(element.rownum, repaint);

//     // sync up the rowHandlers 

//     crossref.removeElementAt(element.rownum);

//     // and make sure the rownums are correct.

//     for (int i = element.rownum; i < crossref.size(); i++)
//       {
// 	((rowHandler) crossref.elementAt(i)).rownum = i;
//       }

//     reShape();
  } // delete row









  // The listener class for the table column model.
  public class myColModelListener implements TableColumnModelListener
  {
    public void columnAdded(TableColumnModelEvent e) 
    {
      //System.out.println("Added");
    }
    
    public void columnMarginChanged(ChangeEvent e) 
    {
      //System.out.println("column Margin changed");
      
      TableColumn tc = table.getTableHeader().getResizingColumn();
      if (tc != null)
	{
	  int colIndex = tc.getModelIndex();      
	  int colIndex2 = getNextColumnIndex(colIndex);
	  //System.out.println("\nCol #"+colIndex+" and "+colIndex2+" are being changed");

	  // Turn on text-wrapping for both columns.
	  setColumnTextWrap(colIndex);
	  if (colIndex2 != -1) 
	    {
	      setColumnTextWrap(colIndex2);
	    }
	}
    }


    // Get the next physical columns index number. Needed for when columns are moved, 
    // their indexes remain the same.
    private int getNextColumnIndex(int colIndex)
    {
      // Get next column, can't just add number, because they can be reordered.
      TableColumnModel colModel = table.getTableHeader().getColumnModel();
      
      // get list of columns, physical order.
      Enumeration e2 = colModel.getColumns();
      int i = 0;
      int colIndex2 = -1;
      TableColumn tc2 = null;
      while (e2.hasMoreElements() && colIndex2 != colIndex)
	{	  
	  tc2 = (TableColumn)e2.nextElement();	      
	  colIndex2 = tc2.getModelIndex();	      
	  //System.out.println(i+". col index is:"+ colIndex2);
	  i++;
	}
      
      if (e2.hasMoreElements())
	{
	  tc2 = (TableColumn)e2.nextElement();	      
	  colIndex2 = tc2.getModelIndex();	      
	  //System.out.println("Found next column index: "+colIndex2);
	  return colIndex2;
	}
      else
	{
	  return -1;
	}
    }
        
    public void columnMoved(TableColumnModelEvent e) 
    {
      //System.out.println("Moved");
    }
    
    public void columnRemoved(TableColumnModelEvent e) 
    {
      //System.out.println("Removed");
    }
    
    public void columnSelectionChanged(ListSelectionEvent e) 
    {
      //System.out.println("Selection Changed");
    }
  } // myColModelListener



  /** listener for when the main panel is resized */
  public class SmartTableComponentListener implements ComponentListener
  {
    public void componentResized(ComponentEvent e)
    {
      fixTableColumns();	
    }
    public void componentMoved(ComponentEvent e)
    {
      //System.out.println("HeaderCL: componentMoved");
    }
    
    public void componentShown(ComponentEvent e)
    {
      //System.out.println("HeaderCL: componentShown");
    }
    
    public void componentHidden(ComponentEvent e)
    {
      //System.out.println("HeaderCL: componentHidden");
    }
  }

  

  // Class to assist with FixTable Columns, allowing it to be called AFTER the 
  // table is drawn, to get in the correct Table and Panel size to match with
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


  // Mouse class to control the right click menus
  class PopupListener extends MouseAdapter 
  {
    SmartTable master_control;
    JPopupMenu popMenu;

    public PopupListener(SmartTable master, JPopupMenu popMenu1) 
    {
      this.master_control = master;
      popMenu = popMenu1;   // passing in needed menu
    }
  
    public void mousePressed(MouseEvent e) 
    {
      showPopup(e);
    }

    public void mouseReleased(MouseEvent e) 
    {
      showPopup(e);
    }

    private void showPopup(MouseEvent e) 
    {
      if (e.isPopupTrigger()) {
	if (debug) 
	  {
	    System.out.println("mouseevent on row:"+ table.rowAtPoint(e.getPoint()) +"*");			
	    System.out.println("mouseevent on col:"+ table.columnAtPoint(e.getPoint()) +"*");			
	  }
	// show the passed in menu
	master_control.remember_row = master_control.table.rowAtPoint(e.getPoint());
	master_control.remember_col = master_control.remember_col2 = master_control.table.columnAtPoint(e.getPoint());

	// if table is altered by moving or deleting a column, get true column number here
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
	} // secondary unaltered column

	// Select a single row for right clicks - rows 1 to 1
	table.addRowSelectionInterval(master_control.remember_row, master_control.remember_row);

        popMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  } // Class popupListener


  
  // New Class added in to help define the table results
  // Contructor takes in the results of a query, and constructs a JTable from it
  class MyTableModel extends AbstractTableModel 
  {  
    private boolean DEBUG = true;
    public Vector rows; // vector of rowHandle objects, holds actual data cells 
    private String[] columnNames;
    private Class[] columnClasses;


    public MyTableModel(String[] columnValues)
    {
      if (debug) System.err.println("DEBUG: MyTableModel Constructor");

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
      index = new Hashtable(); // Clear Keys
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
     *
     */
    public final void setCellValue(Object key, int col, Object value)
    {
      Integer row = (Integer) index.get(key);
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
      Integer row = (Integer) index.get(key);
      int row2 = row.intValue();
      return getValueAt(row2, col);
    }

      
    public rowHandler getRowHandler(int row) 
    {
      return (rowHandler) rows.elementAt(row);
    }
      
    /*
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
      System.out.println("numCols = "+numCols);
	
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

    // Get the physical columns position number. Needed for when columns are moved, 
    // their indexes remain the same.
    public int getPhysicalColumnPos(int colIndex)
    {
      TableColumnModel colModel = table.getTableHeader().getColumnModel();
      
      // get list of columns, physical order.
      Enumeration e2 = colModel.getColumns();
      int i = 0;
      int colIndex2 = -1;
      TableColumn tc2 = null;
      while (e2.hasMoreElements() && colIndex2 != colIndex)
	{	  
	  tc2 = (TableColumn)e2.nextElement();	      
	  colIndex2 = tc2.getModelIndex();	      
	  //System.out.println(i+". col index is:"+ colIndex2);
	  i++;
	}
      
      if (colIndex2 == colIndex)
	{
	  return --i;
	}
      else
	{
	  return -1;
	}
    }
  } // MyTableModel class
  


  /**
   * A cell renderer for Date values.
   */
  private class DateCellRenderer extends DefaultTableCellRenderer
  {
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
	  DateFormat df = new SimpleDateFormat("M/d/yyyy");
          setText(df.format(dateValue));
        }
      return this;
    }
  } // class datecellrenderer






} // class SmartTable









/*------------------------------------------------------------------------------
                                                                           class
                                                                      rowHandler

This class is used to map a hash key to a position in the table.

------------------------------------------------------------------------------*/

class rowHandler 
{
  Object  key;
  Object[] cells;  

  public rowHandler(Object key, int columns)
  {
    //System.err.println("New rowHandler created, key = " + key+" with columnscnt:"+columns);
    cells = new Object[columns]; // set the size of cells list
    this.key = key;
  }
} 
