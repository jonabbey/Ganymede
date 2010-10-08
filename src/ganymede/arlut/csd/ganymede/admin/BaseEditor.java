/*

   BaseEditor.java

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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.border.EtchedBorder;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JnumberField;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.JButtonPanel;
import arlut.csd.JDataComponent.JLabelPanel;
import arlut.csd.JDataComponent.JStretchPanel;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      BaseEditor

------------------------------------------------------------------------------*/

/**
 * Part of the admin console's graphical schema editor.  This panel
 * is responsible for displaying and editing base definitions (title,
 * label, class).
 */

class BaseEditor extends JStretchPanel implements JsetValueCallback, ItemListener, ActionListener {

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
    classS,
    classOptionS;

  JButton
    classInfoResetButton, classInfoSetButton;

  JComboBox
    labelC;

  JLabelPanel 
    editPanel, classPanel;

  JButtonPanel buttonPanel;

  GASHSchema
     owner;

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

    editPanel = this.setupEditPanel();
    setComponent(editPanel);
  }

  private JLabelPanel setupEditPanel()
  {
    typeN = new JnumberField(30, false, false, 0, 0);
    typeN.setCallback(this);

    // only allow characters that can be used as an XML entity name.
    // We allow the space char (which is not allowed as an XML entity
    // name), but disallow the underscore char, which we use in place
    // of the space when we write out the field name as an XML entity.

    nameS = new JstringField(40, 100, true, false, null, null);
    nameS.setCallback(this);

    labelC = new JComboBox();
    labelC.addItemListener(this);

    // Let's create an interior panel to store our class name / option
    // setting stuff.. we won't set a callback on these string fields,
    // as we only want to process them when we get a button press on
    // the 'set' button.

    classS = new JstringField(30, 100, true, false,
			      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.", 
			      null);

    classOptionS = new JstringField(30, 100, true, false,
				    null, null);

    classInfoResetButton = new JButton("Reset");
    classInfoSetButton = new JButton("Set");

    buttonPanel = new JButtonPanel(JButtonPanel.RIGHT,false);
    buttonPanel.addButton(classInfoResetButton);
    buttonPanel.addButton(classInfoSetButton);
    buttonPanel.addListeners(this);

    classPanel = new JLabelPanel(false);
    classPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    classPanel.setFontStyle(Font.PLAIN);
    classPanel.addFillRow("Name:", classS);
    classPanel.addFillRow("Option String:", classOptionS);
    classPanel.addFillRow(null, buttonPanel);

    editPanel = new JLabelPanel(false);
    editPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    editPanel.addFillRow("ObjectType ID:", typeN);
    editPanel.addFillRow("Object Type:", nameS);
    editPanel.addRow("Class Information:", classPanel);
    editPanel.addRow("Label:", labelC);

    return editPanel;
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
	classOptionS.setText(base.getClassOptionString());
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
    
    //labelC.addItem("<none>");
    
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
		if ((currentField.isString() || currentField.isNumeric() ||
		     currentField.isIP()) &&
		    currentField.getNameSpaceLabel() != null)
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

  /**
   * implementing {@link java.awt.event.ItemListener ItemListener}
   */

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

  /**
   * implementing {@link java.awt.event.ActionListener ActionListener}
   */

  public void actionPerformed(ActionEvent e)
  {
    ReturnVal retVal = null;

    /* -- */

    if (e.getSource() == classInfoResetButton)
      {
	try
	  {
	    classS.setText(base.getClassName());
	    classOptionS.setText(base.getClassOptionString());
	  }
	catch (RemoteException ex)
	  {
	  }
      }
    else if (e.getSource() == classInfoSetButton)
      {
	try
	  {
	    retVal = owner.handleReturnVal(base.setClassInfo(classS.getValue(), classOptionS.getValue()));

	    if (retVal != null && !retVal.didSucceed())
	      {
		// revert
		classS.setText(base.getClassName());
		classOptionS.setText(base.getClassOptionString());
	      }
	  }
	catch (RemoteException ex)
	  {
	  }
      }
  }

  /**
   * implementing {@link arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback}
   */

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
      }
    catch (RemoteException ex)
      {
	System.err.println("Couldn't set attribute on base: " + ex);
	return false;
      }
    
    return true;
  }

  /**
   * GC-aiding dissolution method.  Should be called on GUI thread.
   */

  public void cleanup()
  {
    this.baseNode = null;
    this.base = null;	// remote reference
    this.typeN = null;
    this.nameS = null;
    this.classS = null;
    this.classOptionS = null;
    this.classInfoResetButton = null;
    this.classInfoSetButton = null;
    this.labelC = null;
    this.owner = null;

    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread

    if (editPanel != null)
      {
	editPanel.cleanup();
	editPanel = null;
      }

    if (classPanel != null)
      {
	classPanel.cleanup();
	classPanel = null;
      }

    if (buttonPanel != null)
      {
	buttonPanel.cleanup();
	buttonPanel = null;
      }
  }
}

