/*

   clientTab.java

   Simple class to hold containerPanels for the custom (server-side
   defined) tabs in the framePanel in the Ganymede client.
   
   Created: 10 June 2005

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

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       clientTab

------------------------------------------------------------------------------*/

/**
 * Simple class to hold {@link
 * arlut.csd.ganymede.client.containerPanel containerPanels} for the
 * custom (server-side) defined tabs in the {@link
 * arlut.csd.ganymede.client.framePanel} in the Ganymede client.
 */

public abstract class clientTab {

  private boolean created = false;

  String tabName;
  JTabbedPane pane;
  ImageIcon icon = null;
  framePanel parent = null;

  /* -- */

  public clientTab(framePanel parent, JTabbedPane pane, String tabName)
  {
    this.parent = parent;
    this.pane = pane;
    this.tabName = tabName;
  }

  public void setImageIcon(ImageIcon icon)
  {
    this.icon = icon;

    int index = getIndex();

    if (index != -1)
      {
        pane.setIconAt(index, icon);
      }
  }

  /**
   * This method adds this clientTab object to the end of the given
   * JTabbedPane.
   */

  public void addToPane(Vector tabList)
  {
    synchronized (tabList)
      {
        pane.addTab(getTabName(), icon, getComponent());
        tabList.addElement(this);
      }
  }

  public String getTabName()
  {
    return this.tabName;
  }
  
  /**
   * This method provides the javax.swing.JComponent that this tab
   * will contain.  The various subclasses must define this method.
   */

  public abstract JComponent getComponent();

  public void showTab()
  {
    if (!this.isCreated())
      {
        this.initialize();
        this.created = true;
      }

    pane.setSelectedIndex(this.getIndex());
  }

  public int getIndex()
  {
    return pane.indexOfComponent(this.getComponent());
  }

  public boolean isCreated()
  {
    return this.created;
  }

  /**
   * This method does whatever work is required to get the tab ready
   * for display.
   */

  public abstract void initialize();

  /**
   * This method does whatever work is required to refresh the
   * information displayed in this tab.
   */

  public abstract void update();

  /**
   * This method does internal cleanup of the contents of this tab,
   * generally by nulling references and the like.
   *
   *  Subclasses must call super.dispose() at the end of their
   *  specific disposal logic.
   */

  public void dispose()
  {
    tabName = null;
    pane = null;
    icon = null;
    parent = null;
  }
}
