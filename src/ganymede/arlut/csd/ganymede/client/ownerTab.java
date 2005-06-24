/*

   ownerTab.java

   This class manages the owner tab in the Ganymede client.
   
   Created: 21 June 2005
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

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.invid_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        ownerTab

------------------------------------------------------------------------------*/

/**
 * This class manages the owner tab in the Ganymede client.
 */

public class ownerTab extends clientTab {

  private ownerPanel owner_panel;
  private JScrollPane contentPane;
  private invid_field invf;

  public ownerTab(framePanel parent, JTabbedPane pane, String tabName)
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
    try
      {
	contentPane.getVerticalScrollBar().setUnitIncrement(15);
	invf = (invid_field) parent.getObject().getField(SchemaConstants.OwnerListField);
	owner_panel = new ownerPanel(invf, parent.isEditable() && invf.isEditable(), parent);
	contentPane.setViewportView(owner_panel);
      }
    catch (RemoteException ex)
      {
	parent.getgclient().processExceptionRethrow(ex);
      }
  }

  public void update()
  {
    try
      {
	owner_panel.updateInvidStringSelector();
      }
    catch (Exception ex)
      {
	parent.getgclient().processExceptionRethrow(ex);
      }
  }

  public void dispose()
  {
    invf = null;

    if (contentPane != null)
      {
	contentPane.removeAll();
	contentPane = null;
      }

    if (owner_panel != null)
      {
	owner_panel.dispose();
	owner_panel = null;
      }

    super.dispose();
  }
}
