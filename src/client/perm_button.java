/*

   perm_button.java

   Description.
   
   Created: 20 January 1997
   Release: $Name:  $
   Version: $Revision: 1.12 $
   Last Mod Date: $Date: 2001/07/27 05:18:32 $
   Module By: Erik Grostic
              Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     perm_button

------------------------------------------------------------------------------*/

class perm_button extends JButton implements ActionListener {

  static final boolean debug = false;

  perm_field field;
  boolean enabled;
  Hashtable basehash;
  gclient gc;
  String title;
  JCheckBox tableView;
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
	  setText("Edit Permissions");
	}
      else
	{
	  setText("View Permissions");
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
    if ((e.getSource() == this)) // && (tableView.isSelected()))
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
	    editor = new perm_editor(field, 
				     enabled, gc, 
				     parent, "Permissions Editor: " + title);
	    
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

  public void cleanUp()
  {
    if (editor != null && editor.isActiveEditor())
      {
	editor.myshow(false);
      }
  }
}
