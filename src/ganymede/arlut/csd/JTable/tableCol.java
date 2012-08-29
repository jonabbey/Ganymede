/*
   tableCol.java

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

import java.awt.Component;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
  float origWidth;              // the basic width of a column.. needs to be multiplied by scalefact
  int width;
  JPopupMenu menu;

  /* -- */

  public tableCol(baseTable rt, String header, float origWidth, tableAttr attr, 
                  JPopupMenu menu)
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
        Component elements[];
        JMenuItem temp;

        elements = menu.getComponents();

        for (int i = 0; i < elements.length; i++)
          {
            if (elements[i] instanceof JMenuItem)
              {
                temp = (JMenuItem) elements[i];
                temp.addActionListener(rt);
              }
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
