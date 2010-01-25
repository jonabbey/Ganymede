/*

   CategoryEditor.java

   Base Editor component for GASHSchema.
   
   Created: 14 August 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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

package arlut.csd.ganymede.admin;

import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.JLabelPanel;
import arlut.csd.JDataComponent.JStretchPanel;
import arlut.csd.ganymede.common.CatTreeNode;
import arlut.csd.ganymede.rmi.Category;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  CategoryEditor

------------------------------------------------------------------------------*/

public class CategoryEditor extends JStretchPanel implements JsetValueCallback {

  static final boolean debug = false;

  // ---

  GASHSchema owner;  
  JLabelPanel catJPanel;
  JstringField catNameS;
  CatTreeNode catNode;
  Category category;

  /* -- */

  CategoryEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }
    
    if (debug)
      {
	System.err.println("CategoryEditor constructed");
      }

    this.owner = owner;

    catNameS = new JstringField(20, 100, true, false, null, "/", this);

    catJPanel = new JLabelPanel();
    catJPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    catJPanel.addRow("Category Label:", catNameS);

    setComponent(catJPanel);
  }

  void editCategory(CatTreeNode catNode)
  {
    this.catNode = catNode;
    this.category = catNode.getCategory();

    try
      {
	catNameS.setText(category.getName());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Remote Exception gettin gNameSpace attributes " + rx);
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    if (v.getSource() == catNameS)
      {
	try
	  {
	    String newValue = (String) v.getValue();

	    // we can't allow categories to have null names, because
	    // if they do, trying to delete the category would
	    // be.. unfortunate.  We really should have some way of
	    // *telling* the user why we're not letting them do this,
	    // but I don't know if we have a handy way of doing that
	    // from this class.

	    if (newValue.equals(""))
	      {
		return false;
	      }

	    if (debug)
	      {
		System.err.println("Trying to set category name to " + newValue);
	      }

	    if (category.setName(newValue))
	      {
		// update the node in the tree

		catNode.setText(newValue);
		owner.tree.refresh();

		if (debug)
		  {
		    System.err.println("Was able to set category name to " + newValue);
		  }

		return true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Was not able to set category name to " + newValue);
		  }

		return false;
	      }
	  }
	catch (RemoteException ex)
	  {
	    return false;
	  }
      }

    return true;		// what the?
  }

  /**
   * GC-aiding dissolution method.  Should be called on GUI thread.
   */

  public void cleanup()
  {
    this.owner = null;

    if (this.catJPanel != null)
      {
	this.catJPanel.cleanup();
	this.catJPanel = null;
      }

    this.catNameS = null;
    this.catNode = null;

    this.category = null;	// remote reference

    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread
  }
}
