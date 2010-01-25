/*

   historyTab.java

   This class manages the history tab in the Ganymede client.
   
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

import java.awt.BorderLayout;
import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.date_field;
import arlut.csd.ganymede.rmi.string_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      historyTab

------------------------------------------------------------------------------*/

/**
 * This class manages the history tab in the Ganymede client.
 */

public class historyTab extends clientTab {

  private JPanel contentPane;
  private historyPanel history_panel;

  public historyTab(framePanel parent, JTabbedPane pane, String tabName)
  {
    super(parent, pane, tabName);
  }

  public JComponent getComponent()
  {
    if (contentPane == null)
      {
	contentPane = new JPanel(new BorderLayout());
      }

    return contentPane;
  }

  public void initialize()
  {
    try
      {
	string_field creator_field = (string_field) parent.getObject().getField(SchemaConstants.CreatorField);
	date_field creation_date_field = (date_field) parent.getObject().getField(SchemaConstants.CreationDateField);
	string_field modifier_field = (string_field) parent.getObject().getField(SchemaConstants.ModifierField);
	date_field modification_date_field = (date_field) parent.getObject().getField(SchemaConstants.ModificationDateField);
	
	history_panel = new historyPanel(parent.getObjectInvid(),
					 parent.getgclient(),
					 creator_field,
					 creation_date_field,
					 modifier_field,
					 modification_date_field);

	contentPane.add("Center", history_panel);
      }
    catch (RemoteException ex)
      {
	parent.getgclient().processExceptionRethrow(ex);
      }
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

    if (history_panel != null)
      {
	history_panel.dispose();
	history_panel = null;
      }

    super.dispose();
  }
}
