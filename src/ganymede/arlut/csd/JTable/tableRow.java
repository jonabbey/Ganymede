/*
   tableRow.java

   A JDK 1.1 table Swing component.

   Created: 15 January 1999

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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

import java.util.Vector;

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
        this.cells.addElement(new tableCell((tableCol) rt.cols.elementAt(i)));
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
