/*

   perm_button.java

   Description.
   
   Created: 20 January 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Erik Grostic
              Jonathan Abbey

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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JButton;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.rmi.perm_field;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     perm_button

------------------------------------------------------------------------------*/

class perm_button extends JButton implements ActionListener {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.perm_button");

  perm_field field;
  boolean enabled;
  Hashtable basehash;
  gclient gc;
  String title;
  perm_editor editor = null;
  boolean isActiveAlready = false;

  /* -- */
  
  /**
   *
   * perm_button constructor
   *
   * @param field What field are we going to edit permissions for?
   * @param enabled If true, will allow editing of the permission matrix
   * @param basehash Map of Bases to field vectors
   * @param justShowUser Should be false when editing the self-permissions object
   *
   */

  public perm_button (perm_field field, 
		      boolean enabled, 
		      gclient gc,
		      boolean justShowUser,
		      String title)
    {
      if (enabled)
	{
	  // "Edit Permissions"
	  setText(ts.l("init.edit"));
	}
      else
	{
	  // "View Permissions"
	  setText(ts.l("init.view"));
	}
      
      this.field = field;
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
    if ((e.getSource() == this))
      {
	if (debug)
	  {
	    System.out.println("Edit Button was pushed- table not selected");
	  }
	  
	// Need to take care of accidental double clicks resulting
	// in two or more instances of perm_editor being
	// created. Even though perm_editor is modal, there is a
	// small gap in time between clicking the perm button and
	// the modal state taking effect- enough time for multiple
	// clicks on the button to create multiple editors.

	if ((editor == null) || (!editor.isActiveEditor())) 
	  { 
	    Frame parent = new Frame();

	    // "Permissions Editor: {0}"
	    editor = new perm_editor(field,
				     enabled, gc,
				     parent,
				     ts.l("actionPerformed.title", title));
	    
	    if (debug)
	      {
		System.out.println("Editor Created by perm button");
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
