/*

   treeMenu.java

   This class is intended to serve as a subclass of popupMenu
   that keeps track of whether it has had its listener set
   by the tree.

   Copyright (C) 1996 - 2004
   The University of Texas at Austin

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
   
   Created: 5 September 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey              jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
