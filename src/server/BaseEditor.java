/*

   BaseEditor.java

   Base Editor component for GASHSchema.
   
   Created: 14 August 1997
   Version: $Revision: 1.10 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;

import com.sun.java.swing.*;

import tablelayout.*;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.event.*;
import java.util.*;

import jdj.PackageResources;

import arlut.csd.JTree.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      BaseEditor

------------------------------------------------------------------------------*/

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
    editPanel.setLayout(new TableLayout(false));
    
    typeN = new JnumberField(20, false, false, 0, 0);
    typeN.setCallback(this);
    addRow(editPanel, typeN, "ObjectType ID:", 0);


    nameS = new JstringField(20, 100, true, false, null, null);
    nameS.setCallback(this);
    addRow(editPanel, nameS, "Object Type:", 1);

    classS = new JstringField(20, 100, true, false, null, null);
    classS.setCallback(this);
    addRow(editPanel, classS, "Class name:", 2);

    labelC = new JComboBox();
    labelC.addItemListener(this);
    addRow(editPanel, labelC, "Label:", 3);

    add(editPanel);
    //    doLayout();
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

    labelC.removeAllItems();

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
		    currentField.isIP())
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
	    if (base.setName(val))
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
	    base.setClassName(val);
	  }
      }
    catch (RemoteException ex)
      {
	System.err.println("Couldn't set attribute on base: " + ex);
	return false;
      }
    
    return true;
  }

  void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);

  }

}

