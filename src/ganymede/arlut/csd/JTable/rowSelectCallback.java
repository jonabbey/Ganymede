/*
   rowSelectCallback.java

   An interface that objects can implement to allow arlut.csd.JTable tables
   to report when a row is selected.

   Created: 19 June 1996

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

/**
 * An interface that objects can implement to allow arlut.csd.JTable tables
 * to report when a row is selected.
 *
 * @see arlut.csd.JTable.rowTable
 */

public interface rowSelectCallback {

  /**
   * Called when a row is selected in rowTable
   * 
   * @param key Hash key for the selected row
   */

  public void rowSelected(Object key);

  /**
   * Called when a row is double selected (double clicked) in rowTable
   * 
   * @param key Hash key for the selected row
   */

  public void rowDoubleSelected(Object key);
  
  /**
   * Called when a row is unselected in rowTable
   * 
   * @param key Hash key for the unselected row
   * @param endSelected false if the callback should assume that the final
   *                    state of the system due to the user's present 
   *                    action will have no row selected
   */

  public void rowUnSelected(Object key, boolean endSelected);

  /**
   * Called when a row is unselected in rowTable
   * 
   * @param key Hash key for the row on which the popup menu item was performed
   * @param event the original ActionEvent from the popupmenu.  
   *              See event.getSource() to identify the menu item performed.
   */

  public void rowMenuPerformed(Object key, java.awt.event.ActionEvent event);

  /**
   * Called when a pop-up menu is fired on a column header.
   *
   * @param menuCol The index for the column that the pop-up was fired on.
   * @param event the original ActionEvent from the column menu.
   */

  public void colMenuPerformed(int menuCol, java.awt.event.ActionEvent event);
}
