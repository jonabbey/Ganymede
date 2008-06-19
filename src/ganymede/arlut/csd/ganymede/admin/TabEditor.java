/*

   TabEditor.java

   Base Editor component for GASHSchema.
   
   Created: 6 June 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

package arlut.csd.ganymede.admin;

import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.JLabelPanel;
import arlut.csd.JDataComponent.JStretchPanel;
import arlut.csd.JTree.treeNode;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       TabEditor

------------------------------------------------------------------------------*/

public class TabEditor extends JStretchPanel implements JsetValueCallback {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.TabEditor");

  // ---

  GASHSchema owner;  
  JLabelPanel tabJPanel;
  JstringField tabNameS;
  TabNode tabNode;

  /* -- */

  TabEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	// "Error, null owner"
	throw new IllegalArgumentException(ts.l("init.null_param"));
      }

    this.owner = owner;

    tabNameS = new JstringField(20, 100, true, false, null, "/", this);

    tabJPanel = new JLabelPanel();
    tabJPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    // "Tab Name:"
    tabJPanel.addRow(ts.l("init.label"), tabNameS);

    setComponent(tabJPanel);
  }

  /**
   * This method is used to initialize this TabEditor component
   * against a given TabNode, by scanning 
   */

  void editTab(TabNode tabNode)
  {
    this.tabNode = tabNode;

    // we get the name that this tabNode should have by looking at the
    // TabName of our first child.  This is because the tab 'name' is
    // actually not a first class container, but rather is derived
    // from the strings stored in the fields

    tabNameS.setText(tabNode.getText());

    /*
    treeNode node = tabNode.getChild();

    if (node == null)
      {
	tabNameS.setText(ts.l("editTab.new_tab")); // "New Tab"
      }
    else if (node instanceof FieldNode)
      {
	BaseField field = ((FieldNode) node).getField();

	try
	  {
	    tabNameS.setText(field.getTabName());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException(rx);
	  }
      }
    */
  }

  public boolean setValuePerformed(JValueObject v)
  {
    if (v.getSource() == tabNameS)
      {
	try
	  {
	    String newValue = (String) v.getValue();

	    // we can't allow fields to have empty/null tab names
	    // because if they do, that would just be too crazy.  We
	    // really should have some way of *telling* the user why
	    // we're not letting them do this, but I don't know if we
	    // have a handy way of doing that from this class.

	    if (newValue == null || newValue.equals(""))
	      {
		return false;
	      }

	    treeNode node = tabNode.getChild();

	    while (node != null)
	      {
		if (node instanceof FieldNode)
		  {
		    BaseField field = ((FieldNode) node).getField();

		    field.setTabName(newValue);
		    
		    node = node.getNextSibling();
		  }
	      }

	    tabNode.setText(newValue);
	    owner.tree.refresh();
	  }
	catch (RemoteException ex)
	  {
	    return false;
	  }
      }

    return true;
  }

  /**
   * GC-aiding dissolution method.  Should be called on GUI thread.
   */

  public void cleanup()
  {
    this.owner = null;

    if (this.tabJPanel != null)
      {
	this.tabJPanel.cleanup();
	this.tabJPanel = null;
      }

    this.tabNameS = null;
    this.tabNode = null;

    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread
  }
}
