/*

   expirationRemovalTab.java

   This class manages the owner tab in the Ganymede client.
   
   Created: 22 June 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client;

import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

import arlut.csd.ganymede.rmi.date_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                            expirationRemovalTab

------------------------------------------------------------------------------*/

/**
 * This class manages the owner tab in the Ganymede client.
 */

public class expirationRemovalTab extends clientTab {

  private JScrollPane contentPane;
  private String datePanelTitle;
  private datePanel date_panel;
  private date_field server_field;

  public expirationRemovalTab(framePanel parent, JTabbedPane pane, String tabName)
  {
    super(parent, pane, tabName);
  }

  public void setDateField(date_field server_field)
  {
    this.server_field = server_field;
  }

  public void setPanelTitle(String title)
  {
    this.datePanelTitle = title;
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
    try
      {
	contentPane.getVerticalScrollBar().setUnitIncrement(15);

	date_panel = new datePanel(server_field, parent.gc.getFieldTemplate(parent.getObjectInvid().getType(), server_field.getID()),
				   this.datePanelTitle,
				   parent.isEditable() && server_field.isEditable(), 
				   parent);
	
	contentPane.setViewportView(date_panel);
      }
    catch (RemoteException ex)
      {
	parent.getgclient().processExceptionRethrow(ex);
      }
  }

  public void update()
  {
    date_panel.refresh();
  }

  public void dispose()
  {
    server_field = null;
    contentPane = null;

    if (date_panel != null)
      {
	date_panel.dispose();
	date_panel = null;
      }

    super.dispose();
  }
}
