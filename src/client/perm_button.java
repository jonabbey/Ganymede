/*

   perm_button.java

   Description.
   
   Created: 20 January 1997
   Version: $Revision: 1.1 $ %D%
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

  /* -- */

  /**
   *
   * perm_button constructor
   *
   * @param client
   * @param field
   * @param enabled
   * @param basehash
   *
   */

  public perm_button (gclient client, perm_field field, 
		      boolean enabled, Hashtable basehash)
  {
    super("Edit Permissions");
    
    this.gc = client;
    this.field = field;
    this.enabled = enabled;
    this.basehash = basehash;
    
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
	perm_editor editor = new perm_editor(gc, field, 
					     enabled, basehash, 
					     parent, "Permissions Editor");

	System.out.println("Editor Created by perm button");
      }
  }
}
