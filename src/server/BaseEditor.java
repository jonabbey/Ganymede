/*

   BaseEditor.java

   Base Editor component for GASHSchema.
   
   Created: 14 August 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.DataComponent.*;
import arlut.csd.Dialog.*;

import tablelayout.*;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.applet.*;
import java.util.*;

import jdj.PackageResources;

import gjt.Box;
import gjt.Util;
import gjt.RowLayout;
import gjt.ColumnLayout;

import arlut.csd.Tree.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      BaseEditor

------------------------------------------------------------------------------*/

class BaseEditor extends ScrollPane implements setValueCallback, ItemListener {

  BaseNode
    baseNode;

  Base 
    base;

  numberField 
    typeN;

  stringField 
    nameS, 
    classS;

  Choice
    labelC;

  componentAttr 
    ca;

  Panel 
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

    System.err.println("BaseEditor constructed");

    base = null;
    this.owner = owner;

    editPanel = new InsetPanel(10, 10, 10, 10);
    editPanel.setLayout(new TableLayout(false));
    
    ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white);

    typeN = new numberField(20, ca, false, false, 0, 0);
    typeN.setCallback(this);
    addRow(editPanel, typeN, "ObjectType ID:", 0);


    nameS = new stringField(20, 100, ca, true, false, null, null);
    nameS.setCallback(this);
    addRow(editPanel, nameS, "Object Type:", 1);

    classS = new stringField(20, 100, ca, true, false, null, null);
    classS.setCallback(this);
    addRow(editPanel, classS, "Class name:", 2);

    labelC = new Choice();
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

    labelC.removeAll();

    if (base == null)
      {
	System.out.println("base is null, not refreshing labelC");
	return;
      }

    try
      {
	fields = base.getFields();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("exception getting fields: " + rx);
      }
    
    labelC.add("<none>");
    
    if (fields == null)
      {
	System.out.println("No fields to add");
	return;
      }

    for (int i = 0; i < fields.size() ; i++)
      {
	currentField = (BaseField) fields.elementAt(i);

	if (currentField != null)
	  {
	    try
	      {
		if (currentField.isString() || currentField.isNumeric())
		  {
		    labelC.add(currentField.getName());
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
	System.out.println("selecting <none>");
	labelC.select("<none>");
      }
    else
      {
	try
	  {
	    System.out.println("selecting label: " + labelField);
	    labelC.select(labelField);
	  }
	catch (NullPointerException ex)
	  {
	    System.out.println("Attempted to set label to field not in choice, setting it to <none>");
	    labelC.select("<none>");
	  }
      }
  }

  public void itemStateChanged(ItemEvent e)
  {
    String label = null;

    /* -- */

    System.out.println("itemStateChanged");

    if (e.getItemSelectable() == labelC)
      {
	try
	  {
	    label = labelC.getSelectedItem();

	    System.out.println("setting label to " + label);

	    if ((label == null) || (label.equals("<none>")))
	      {
		System.out.println("Setting label field to null");
		base.setLabelField(null);
	      }
	    else
	      {
		System.out.println("Setting label field to " + label);
		base.setLabelField(label);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("exception setting label field: " + rx);
	  }
      }
  }

  public boolean setValuePerformed(ValueObject v)
  {
    Component source;
    String val;

    /* -- */

    // we really shouldn't find ourselves called if
    // base is null, but just in case..

    if (base == null)
      {
	return false;
      }

    System.err.println("setValuePerformed:" + v);

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

  void addRow(Panel parent, Component comp,  String label, int row)
  {
    Label l = new Label(label);
    
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);

  }

}

