/*

   fieldoption_button.java

   A GUI component to be placed in the container panel to represent
   the field option editor in sync channel objects.
   
   Created: 20 January 1997

   Module By: Deepak Giridharagopal

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2011
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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Hashtable;
import javax.swing.JButton;

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.field_option_field;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class 
                                                              fieldoption_button

------------------------------------------------------------------------------*/

/**
 * This class is a button that launches the client "widget" that
 * allows a user to edit the field options for a particular builder
 * task.
 */

class fieldoption_button extends JButton implements ActionListener {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.fieldoption_button");

  field_option_field field;
  boolean enabled;
  Hashtable basehash;
  gclient gc;
  String title;
  fieldoption_editor editor = null;
  boolean isActiveAlready = false;

  /**
   * <p>Reference to a remote Sync Channel db_object that we can examine
   * to decide whether we want to allow option values for an
   * incremental or a full-state Sync Channel.</p>
   *
   * <p>We take this as a remote db_object rather than a boolean at
   * construction time because the user can change the Sync Channel
   * mode asynchronously.</p>
   */

  db_object object_ref = null;

  /**
   * fieldoption_button constructor
   *
   * @param field What field are we going to edit field options for?
   * @param object_ref Remote reference to the db_object we're viewing/editing
   * @param enabled If true, will allow editing of the field options matrix
   */

  public fieldoption_button(field_option_field field,
                            db_object object_ref,
                            boolean enabled,
                            gclient gc,
                            String title)
  {
    if (enabled)
      {
        setText(ts.l("global.edit")); // "Edit Field Options"
      }
    else
      {
        setText(ts.l("global.view")); // "View Field Options"
      }
      
    this.field = field;
    this.object_ref = object_ref;
    this.enabled = enabled;
    this.gc = gc;
    this.title = title;
      
    addActionListener(this);
  }
  
  /**
   * When clicked, this button invokes an instance of the permissions
   * editor class.
   */
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == this)
      {
        if (debug)
          {
            System.out.println("Edit Button was pushed- table not selected");
          }
          
        // Need to take care of accidental double clicks resulting
        // in two or more instances of fieldoption_editor being
        // created. Even though queue_editor is modal, there is a
        // small gap in time between clicking the perm button and
        // the modal state taking effect- enough time for multiple
        // clicks on the button to create multiple editors.

        if ((editor == null) || (!editor.isActiveEditor())) 
          { 
            int channelType = 0;

            try
              {
                channelType = ((Integer) object_ref.getFieldValue(SchemaConstants.SyncChannelTypeNum)).intValue();
              }
            catch (RemoteException ex)
              {
                gc.processExceptionRethrow(ex);
              }

            boolean fullState = (channelType == 2); // cf. arlut.csd.ganymede.server.SyncRunner.SyncType

            Frame parent = new Frame();
            editor = new fieldoption_editor(field, 
                                            enabled,
                                            fullState,
                                            gc,
                                            parent, "Field Options Editor: " + title);
            
            if (debug)
              {
                System.out.println("Editor Created by field options button");
              }
          } 
        else 
          {
            if (debug)
              {
                System.out.println("An editor already exists- new one not created");
              }
          }
      }
  }

  /**
   * Calling this method makes this component get rid of any secondary
   * windows and to do some gc reference clearing.
   */

  public synchronized void unregister()
  {
    if (editor != null)
      {
        editor.cleanUp();
        editor = null;
      }

    gc = null;
    basehash = null;
    field = null;
  }
}
