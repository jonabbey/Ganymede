/*

   BaseEditor.java

   Base Editor component for GASHSchema.
   
   Created: 14 August 1997
   Version: $Revision: 1.23 $
   Last Mod Date: $Date: 2000/03/25 05:36:37 $
   Release: $Name:  $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;

import javax.swing.*;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import arlut.csd.JTree.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      BaseEditor

------------------------------------------------------------------------------*/

/**
 * <p>Part of the admin console's graphical schema editor.  This panel
 * is responsible for displaying and editing base definitions (title,
 * label, class).</p>
 */

class BaseEditor extends JPanel implements JsetValueCallback, ItemListener {

  static final boolean debug = false;

  // ---

  boolean 
    listenToCallbacks = true;

  BaseNode
    baseNode;

  Base 
    base;

  JnumberField 
    typeN;

  JstringField 
    nameS, 
    classS;

  JComboBox
    labelC;

  JPanel 
    editPanel;

  GASHSchema
     owner;

  GridBagLayout
    gbl = new GridBagLayout();
  
  GridBagConstraints
    gbc = new GridBagConstraints();

  /* -- */

  BaseEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    if (debug)
      {
	System.err.println("BaseEditor constructed");
      }

    base = null;
    this.owner = owner;

    editPanel = new JPanel(false);
    editPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    editPanel.setLayout(gbl);
    
    typeN = new JnumberField(30, false, false, 0, 0);
    typeN.setCallback(this);
    addRow(editPanel, typeN, "ObjectType ID:", 0);

    // only allow characters that can be used as an XML entity name.
    // We allow the space char (which is not allowed as an XML entity
    // name), but disallow the underscore char, which we use in place
    // of the space when we write out the field name as an XML entity.

    nameS = new JstringField(40, 100, true, false,
			     "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .-", 
			     null);
    nameS.setCallback(this);
    addRow(editPanel, nameS, "Object Type:", 1);

    classS = new JstringField(50, 100, true, false, null, null);
    classS.setCallback(this);
    addRow(editPanel, classS, "Class name:", 2);

    labelC = new JComboBox();
    labelC.addItemListener(this);
    addRow(editPanel, labelC, "Label:", 3);

    add(editPanel);
  }

  /**
   *
   * This method is used to retarget the base editor to a new base
   * without having to break down and reconstruct the panels.
   */

  public void editBase(BaseNode baseNode)
  {
    listenToCallbacks = false;
    owner.setWaitCursor();

    this.baseNode = baseNode;
    this.base = baseNode.getBase();

    try
      {
	typeN.setValue(base.getTypeID());
	nameS.setText(base.getName());
	classS.setText(base.getClassName());
	refreshLabelChoice();
      }
    catch (RemoteException ex)
      {
	System.err.println("editBase: accessor failed: " + ex);
      }

    owner.setNormalCursor();
    listenToCallbacks = true;
  }

  /**
   *
   * This method is an internal helper method to update the label choice displayed
   * in the base editor.
   *
   */

  void refreshLabelChoice()
  {
    Vector fields = null;
    BaseField currentField;
    String labelField = null;

    /* -- */

    try
      {
	labelC.removeAllItems();
      }
    catch (IndexOutOfBoundsException ex)
      {
	// Swing 1.1 beta 2 will do this to us, just
	// ignore it.

	System.err.println("refreshLabelChoice(): Swing Bug Bites Again");
      }

    if (base == null)
      {
	if (debug)
	  {
	    System.out.println("base is null, not refreshing labelC");
	  }
	return;
      }

    try
      {
	fields = base.getFields(true);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("exception getting fields: " + rx);
      }
    
    labelC.addItem("<none>");
    
    if (fields == null)
      {
	if (debug)
	  {
	    System.out.println("No fields to add");
	  }
	return;
      }

    for (int i = 0; i < fields.size() ; i++)
      {
	currentField = (BaseField) fields.elementAt(i);

	if (currentField != null)
	  {
	    try
	      {
		if (currentField.isString() || currentField.isNumeric() ||
		    currentField.isIP() || currentField.isFloat())
		  {
		    labelC.addItem(currentField.getName());
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("exception getting field name: " + rx);
	      }
	  }
      }

    try
      {
	labelField = base.getLabelFieldName();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Exception getting base label: " + rx);
      }

    if (labelField == null)
      {
	if (debug)
	  {
	    System.out.println("selecting <none>");
	  }

	labelC.getModel().setSelectedItem("<none>");
      }
    else
      {
	try
	  {
	    if (debug)
	      {
		System.out.println("selecting label: " + labelField);
	      }

	    labelC.getModel().setSelectedItem(labelField);
	  }
	catch (NullPointerException ex)
	  {
	    System.out.println("Attempted to set label to field not in choice, setting it to <none>");
	    labelC.setSelectedItem("<none>");
	  }
      }
  }

  public void itemStateChanged(ItemEvent e)
  {
    if (!listenToCallbacks)
      {
	if (debug)
	  {
	    System.out.println("I'm not listening, go away.");
	  }
	return;
      }

    String label = null;

    /* -- */

    if (debug)
      {
	System.out.println("itemStateChanged");
      }

    if (e.getItemSelectable() == labelC)
      {
	try
	  {
	    label = (String)labelC.getModel().getSelectedItem();

	    if (debug)
	      {
		System.out.println("setting label to " + label);
	      }

	    if ((label == null) || (label.equals("<none>")))
	      {
		if (debug)
		  {
		    System.out.println("Setting label field to null");
		  }

		base.setLabelField(null);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("Setting label field to " + label);
		  }

		base.setLabelField(label);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("exception setting label field: " + rx);
	  }
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    if (!listenToCallbacks)
      {
	if (debug)
	  {
	    System.out.println("Not listening");
	  }

	return true;
      }

    java.awt.Component source;
    String val;

    /* -- */

    // we really shouldn't find ourselves called if
    // base is null, but just in case..

    if (base == null)
      {
	return false;
      }

    if (debug)
      {
	System.err.println("setValuePerformed:" + v);
      }

    source = v.getSource();
    val = (String) v.getValue();

    try
      {
	if (source == nameS)
	  {
	    ReturnVal retVal = owner.handleReturnVal(base.setName(val));

	    if (retVal == null || retVal.didSucceed())
	      {
		baseNode.setText(base.getName());
		nameS.setText(baseNode.getText());
		owner.tree.refresh();
	      }
	    else
	      {
		return false;
	      }
	  }
	else if (source == classS)
	  {
	    owner.handleReturnVal(base.setClassName(val));
	  }
      }
    catch (RemoteException ex)
      {
	System.err.println("Couldn't set attribute on base: " + ex);
	return false;
      }
    
    return true;
  }

  synchronized void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 0.0;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbl.setConstraints(l, gbc);
    parent.add(l);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(comp, gbc);
    parent.add(comp);
  }
}

