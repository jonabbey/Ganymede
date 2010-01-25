/*

   notesTab.java

   This class manages the notes tab in the Ganymede client.
   
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

import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.string_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        notesTab

------------------------------------------------------------------------------*/

/**
 * This class manages the notes tab in the Ganymede client.
 */

public class notesTab extends clientTab {

  private JScrollPane contentPane;
  string_field notes_field;
  private notesPanel notes_panel;

  public notesTab(framePanel parent, JTabbedPane pane, String tabName)
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
	notes_field = (string_field) parent.getObject().getField(SchemaConstants.NotesField);
	notes_panel = new notesPanel(notes_field, parent.isEditable(), parent);
	contentPane.setViewportView(notes_panel);
      }
    catch (RemoteException ex)
      {
	parent.getgclient().processExceptionRethrow(ex);
      }
  }

  public void update()
  {
  }

  public notesPanel getNotesPanel()
  {
    return this.notes_panel;
  }

  public void dispose()
  {
    notes_field = null;

    if (contentPane != null)
      {
	contentPane.removeAll();
	contentPane = null;
      }

    if (notes_panel != null)
      {
	notes_panel.dispose();
	notes_panel = null;
      }

    super.dispose();
  }
}
