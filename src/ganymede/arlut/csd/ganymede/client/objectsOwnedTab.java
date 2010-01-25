/*

   objectsOwnedTab.java

   This class manages the objects owned tab (for Owner Group objects)
   in the Ganymede client.
   
   Created: 21 June 2005

   Module By: Jonathan Abbey

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 objectsOwnedTab

------------------------------------------------------------------------------*/

/**
 * This class manages the objects owned tab (for Owner Group objects)
 * in the Ganymede client.
 */

public class objectsOwnedTab extends clientTab {

  private ownershipPanel objects_owned_panel;
  private JScrollPane contentPane;

  public objectsOwnedTab(framePanel parent, JTabbedPane pane, String tabName)
  {
    super(parent, pane, tabName);
  }

  public JComponent getComponent()
  {
    if (contentPane == null)
      {
	contentPane = new JScrollPane();
      }

    return contentPane;
  }

  public void initialize()
  {
    contentPane.getVerticalScrollBar().setUnitIncrement(15);
    objects_owned_panel = new ownershipPanel(parent.isEditable(), parent);
    contentPane.setViewportView(objects_owned_panel);
  }

  public void update()
  {
  }

  public void dispose()
  {
    if (contentPane != null)
      {
	contentPane.removeAll();
	contentPane = null;
      }

    if (objects_owned_panel != null)
      {
	objects_owned_panel.dispose();
	objects_owned_panel = null;
      }

    super.dispose();
  }
}
