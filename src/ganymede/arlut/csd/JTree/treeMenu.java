/*

   treeMenu.java

   This class is intended to serve as a subclass of popupMenu
   that keeps track of whether it has had its listener set
   by the tree.

   Created: 5 September 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.JTree;

import java.awt.Component;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        treeMenu

------------------------------------------------------------------------------*/

public class treeMenu extends JPopupMenu {

  boolean registered = false;

  /* -- */

  public treeMenu()
  {
    super();
  }

  public treeMenu(String label)
  {
    super(label);
  }

  /**
   * This method registers all menu items in this menu with the
   * appropriate listener.  If this menu has already been registered
   * with anything, it won't add items to the listener.<p>
   *
   * This means that a treeMenu cannot be used in multiple trees
   * simultaneously, as written.<p>
   *
   * @return a boolean, true if this menu is being registered
   * for the first time.
   * 
   */

  public boolean registerItems(treeControl listener)
  {
    Component elements[];
    JMenuItem temp;

    /* -- */

    if (registered)
      {
	return false;
      }

    elements = getComponents();

    for (int i = 0; i < elements.length; i++)
      {
	if (elements[i] instanceof JMenuItem)
	  {
	    temp = (JMenuItem) elements[i];
	    temp.addActionListener(listener);
	  }
      }

    registered = true;

    return true;
  }
}
