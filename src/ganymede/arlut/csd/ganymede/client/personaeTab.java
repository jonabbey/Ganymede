/*

   personaeTab.java

   This class manages the admin personae tab (for User objects) in the
   Ganymede client.
   
   Created: 21 June 2005

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2008
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
import arlut.csd.ganymede.rmi.invid_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     personaeTab

------------------------------------------------------------------------------*/

/**
 * This class manages the admin personae tab (for User objects) in the
 * Ganymede client.
 */

public class personaeTab extends clientTab {

  private JPanel contentPane;
  private personaPanel pP;
  private invid_field persona_field;

  public personaeTab(framePanel parent, JTabbedPane pane, String tabName)
  {
    super(parent, pane, tabName);
  }

  public JComponent getComponent()
  {
    if (contentPane == null)
      {
        contentPane = new JPanel(false);
      }

    return contentPane;
  }

  public void initialize()
  {
    try
      {
        persona_field = (invid_field) parent.getObject().getField(SchemaConstants.UserAdminPersonae);
        pP = new personaPanel(persona_field, parent.isEditable(), parent);
        contentPane.setLayout(new BorderLayout());
        contentPane.add("Center", pP);
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

    if (pP != null)
      {
        pP.dispose();
        pP = null;
      }

    persona_field = null;

    super.dispose();
  }
}
