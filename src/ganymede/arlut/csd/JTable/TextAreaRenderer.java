/*

   TextAreaRenderer.java

   This Module encapsulates some more user interactions with a Jtable, including
   right click menus to sort and remove columns
   
   Created: 26 January 2006
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

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;


/*------------------------------------------------------------------------------
                                                                           class
                                                                TextAreaRenderer
------------------------------------------------------------------------------*/

public class TextAreaRenderer extends JTextArea implements TableCellRenderer 
{
  private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();
  /** map from table to map of rows to map of column heights */
  private final HashMap cellSizes = new HashMap();

  public TextAreaRenderer() 
  {
    setLineWrap(true);
    setWrapStyleWord(true);
  }

  public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected,
      boolean hasFocus, int row, int column) 
  {
    // set the colours, etc. using the standard for that platform
    adaptee.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
    setForeground(adaptee.getForeground());
    setBackground(adaptee.getBackground());
    setBorder(adaptee.getBorder());
    setFont(adaptee.getFont());
    setText(adaptee.getText());

    // This line was very important to get it working with JDK1.4
    TableColumnModel columnModel = table.getColumnModel();
    setSize(columnModel.getColumn(column).getWidth(), 100000);
    int height_wanted = (int) getPreferredSize().getHeight();
    addSize(table, row, column, height_wanted);
    height_wanted = findTotalMaximumRowSize(table, row);
    if (height_wanted != table.getRowHeight(row)) 
    {
      table.setRowHeight(row, height_wanted);
    }
    return this;
  }

  private void addSize(JTable table, int row, int column, int height) 
  {
    HashMap rows = (HashMap) cellSizes.get(table);
    if (rows == null) 
    {
      rows = new HashMap();
      cellSizes.put(table, rows);
    }
    HashMap rowheights = (HashMap) rows.get(new Integer(row));
    if (rowheights == null) 
    {
      rows.put(new Integer(row), rowheights = new HashMap());
    }
    rowheights.put(new Integer(column), new Integer(height));
  }

  /**
   * Look through all columns and get the renderer.  If it is
   * also a TextAreaRenderer, we look at the maximum height in
   * its hash table for this row.
   */
  private int findTotalMaximumRowSize(JTable table, int row) 
  {
    int maximum_height = 0;
    Enumeration columns = table.getColumnModel().getColumns();
    while (columns.hasMoreElements()) 
    {
      TableColumn tc = (TableColumn) columns.nextElement();
      TableCellRenderer cellRenderer = tc.getCellRenderer();
      if (cellRenderer instanceof TextAreaRenderer) 
      {
        TextAreaRenderer tar = (TextAreaRenderer) cellRenderer;
        maximum_height = Math.max(maximum_height, tar.findMaximumRowSize(table, row));
      }
    }
    return maximum_height;
  }

  private int findMaximumRowSize(JTable table, int row) 
  {
    Map rows = (Map) cellSizes.get(table);
    if (rows == null) return 0;
    Map rowheights = (Map) rows.get(new Integer(row));
    if (rowheights == null) return 0;
    int maximum_height = 0;
    for (Iterator it = rowheights.entrySet().iterator(); it.hasNext();) 
    {
      Map.Entry entry = (Map.Entry) it.next();
      int cellHeight = ((Integer) entry.getValue()).intValue();
      maximum_height = Math.max(maximum_height, cellHeight);
    }
    return maximum_height;
  }
} // TextAreaRenderer
  
