/*

   perm_button.java

   Description.
   
   Created: 20 January 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Erik Grostic
              Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
      
      
      // Commented out stuff is to allow access to "good" editor while testing another
      
      //    JLabel label = new JLabel("myLabel");
      //    tableView = new JCheckBox("Table View",false);
      //    label.setBorder(new BevelBorder(BevelBorder.RAISED));
      
      if (enabled)
	{
	  setText("Edit Permissions");
	}
      else
	{
	  setText("View Permissions");
	}
      
      //     this.setLayout(new BorderLayout());
      //     this.add(label, "North");
      //     this.add(tableView, "South");
      
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
	  // 	System.out.println("Edit Button was pushed- table selected");
	  
	  // 	Frame parent = new Frame();
	  // 	perm_editor editor = new perm_editor(field, 
	  // 					     enabled, gc, 
	  // 					     parent, "Permissions Editor: " + title);
	  
	  // 	System.out.println("Editor Created by perm button");
	  //       }
 
	  //     else {
	  
	  System.out.println("Edit Button was pushed- table not selected");
	  

	  // Need to take care of accidental double clicks resulting in two or more instances 
	  // of perm_editor being created. Even though perm_editor is modal, there is a small 
	  // gap in time between clicking the perm button and the modal state taking effect- 
	  // enough time for multiple clicks on the button to create multiple editors. 
	  if ((editor == null) || (!editor.isActiveEditor())) { 
	    Frame parent = new Frame();
	    editor = new perm_editor(field, 
				     enabled, gc, 
				     parent, "Permissions Editor: " + title);
	    
	    System.out.println("Editor Created by perm button");
	  } else {
	    System.out.println("An editor already exists- new one not created");
	  }
	}
      
    }
}
