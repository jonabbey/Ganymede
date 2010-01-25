/*

   serverTab.java

   This class handles the creation and management of custom
   server-defined tabs in the Ganymede client.
   
   Created: 22 June 2005

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

import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       serverTab

------------------------------------------------------------------------------*/

/**
 * This class handles the creation and management of custom
 * server-defined tabs in the Ganymede client.
 */

public class serverTab extends clientTab {

  private JScrollPane contentPane;
  private containerPanel cp;
  private Vector infoVector;

  public serverTab(framePanel parent, JTabbedPane pane, String tabName)
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

  public void setInfoVector(Vector infoVector)
  {
    this.infoVector = infoVector;
  }

  /**
   * This method does whatever work is required to get the tab ready
   * for display.
   */

  public void initialize()
  {
    contentPane.getVerticalScrollBar().setUnitIncrement(15);

    cp = new containerPanel(parent.getObject(), parent.getObjectInvid(), parent.isEditable(),
			    parent.getgclient(), parent.getWindowPanel(), parent, parent.progressBar,
			    false, parent.isCreating, null);

    cp.setTabName(tabName);
    cp.setInfoVector(infoVector);
    cp.load();
    cp.setBorder(parent.getWindowPanel().emptyBorder10Right);

    this.infoVector = null;

    contentPane.setViewportView(cp);
  }

  /**
   * This method does whatever work is required to refresh the
   * information displayed in this tab.
   */

  public void update()
  {
    cp.updateAll();
  }

  /**
   * This method does internal cleanup of the contents of this tab,
   * generally by nulling references and the like.
   */

  public void dispose()
  {
    if (cp != null)
      {
	cp.cleanUp();
	cp = null;
      }

    if (contentPane != null)
      {
	contentPane.removeAll();
	contentPane = null;
      }

    infoVector = null;

    super.dispose();
  }
}
