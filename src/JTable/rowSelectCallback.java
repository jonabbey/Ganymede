/*

  rowSelectCallback.java

  An interface that objects can implement to allow arlut.csd.JTable tables
  to report when a row is selected.

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

  Created: 19 June 1996
  Version: $Revision: 1.5 $ %D%
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

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
}
