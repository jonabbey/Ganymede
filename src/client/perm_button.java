/*

   perm_button.java

   Description.
   
   Created: 20 January 1997
   Version: $Revision: 1.2 $ %D%
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

import tablelayout.*;

import com.sun.java.swing.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     perm_button

------------------------------------------------------------------------------*/

class perm_button extends JButton implements ActionListener {

  perm_field field;
  boolean enabled;
  Hashtable basehash;
  gclient gc;
  boolean justShowUser;

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
		      boolean enabled, Hashtable basehash,
		      boolean justShowUser)
  {
    super("Edit Permissions");
    
    this.field = field;
    this.enabled = enabled;
    this.basehash = basehash;
    this.justShowUser = justShowUser;
    
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
	System.out.println("Edit Button was pushed");
	
	Frame parent = new Frame();
	perm_editor editor = new perm_editor(field, 
					     enabled, basehash, 
					     parent, "Permissions Editor",
					     justShowUser);

	System.out.println("Editor Created by perm button");
      }
  }
}
