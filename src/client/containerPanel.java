/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.1 $ %D%
    Module By: Michael Mulvaney
    Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.ganymede.client;

import tablelayout.*;
import com.sun.java.swing.*;
import com.sun.java.swing.event.*;


import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.client.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  containerPanel

------------------------------------------------------------------------------*/
public class containerPanel extends JPanel implements ActionListener, JsetValueCallback, ItemListener{  
  static final boolean debug = true;
  
  gclient
    parent;

  db_object
    object;
  
  windowPanel
    winP;

  Hashtable
    rowHash, 
    objectHash;
  
  JPanel 
    panel;
  
  TableLayout 
    layout;
  
  db_field[] 
    fields = null;
  
  JScrollPane
    scrollpane;
  
  JstringField
    sf;

  JButton
    editB;
  
  /* -- */

  /**
   * Main constructor for containerPanel
   *
   * @param object The object to be displayed
   * @param editable 
   * @param objectHash Hashtable of elements to fields, from the windowPanel
   * @param parent Parent gclient of this container
   * @param window windowPanel containing this containerPanel
   */
  public containerPanel(db_object object, boolean editable, gclient parent, windowPanel window)
    {

      if (object == null)
	{
	  System.err.println("null object passed to containerPanel");
	  return;
	}

      objectHash = new Hashtable();
      rowHash = new Hashtable();

      this.parent = parent;
      this.winP = window;
      this.objectHash = objectHash;
      this.object = object;

      if (editable)
	{
	  parent.setStatus("Opening object for edit");
	  
	  if (debug)
	    {
	      System.out.println("Setting status for edit");
	    }
	}
      else
	{
	  parent.setStatus("Getting object for viewing");
	  
	  if (debug)
	    {
	      System.out.println("Setting status for viewing");
	    }
	}
      
      setLayout(new BorderLayout());

    panel = new JPanel();
    layout = new TableLayout(false);
    layout.rowSpacing(5);
    panel.setLayout(layout);

    // Get the list of fields

    try
      {
	fields = object.listFields();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get the fields: " + rx);
      }

    if (debug)
      {
	System.out.println("Entering big loop");
      }

    if ((fields != null) && (fields.length > 0))
      {
	short type = -1;
	String name;
	boolean isVector = false;
	boolean isEditInPlace = false;

	for (int i = 0; i < fields.length ; i++)
	  {
	    type = -1;
	    name = null;
	    isVector = false;

	    try
	      {
		type = fields[i].getType();
		name = fields[i].getName();

		if (debug)
		  {
		    System.out.println("Name: " + name + "Field type desc: " + type);
		  }
		
		isVector = fields[i].isVector();
		isEditInPlace = fields[i].isEditInPlace();
	      }
	    catch  (RemoteException rx)
	      {
		throw new RuntimeException("Could not get field info: " + rx);
	      }
	    
	    if (isEditInPlace)
	      {
		// Add a new Container Panel
		try 
		  {
		    addRow(panel, new JLabel("edit in place scaler"), name, i, fields[i].isVisible());
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not check visibility");
		  }
	      }
	    else if (isVector)
	      {
		if (debug)
		  {
		    System.out.println("Adding vector panel");
		  }

		if (fields[i] == null)
		  {
		    System.out.println("fields[i] is null");
		  }
		else
		  {
		    vectorPanel vp = new vectorPanel(fields[i], winP, editable);
		    try
		      {
			addRow(panel, vp, name, i, fields[i].isVisible());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not check visibility");
		      }
		  }
	      }
	    else
	      {

		switch (type)
		  {
		  case -1:
		    
		    System.err.println("Could not get field information");
		    
		    break;
		    
		  case FieldType.STRING:
		    System.out.println();
		    try 
		      {
			if (((string_field)fields[i]).canChoose())
			  {
			    System.out.println("You can choose");

			    if (((string_field)fields[i]).mustChoose())
			      {
				System.out.println("You must choose.");

				// Add a choice

				JChoice choice = new JChoice();
				Vector choices = ((string_field)fields[i]).choices();

				for (int j = 0; j < choices.size(); j++)
				  {
				    choice.addItem((String)choices.elementAt(j));
				  }

				choice.setEditable(editable);
				choice.addItemListener(this);
				choice.setVisible(true);

				try
				  {
				    choice.setCurrentValue(fields[i].getValue());
				  }
				catch (RemoteException rx)
				  {
				    throw new RuntimeException("Could not get value for field: " + rx);
				  }

				objectHash.put(choice, fields[i]);
				try
				  {
				    addRow(panel, choice, name, i, fields[i].isVisible());
				  }
				catch (RemoteException rx)
				  {
				    throw new RuntimeException("Could not check visibility");
				  }
			      }
			    else
			      {
				// Add a combo box

				JComboBox combo = new JComboBox();
				//Choice combo = new Choice();
				Vector choices = ((string_field)fields[i]).choices();

				for (int j = 0; j < choices.size(); j++)
				  {
				    combo.addPossibleValue((String)choices.elementAt(j));
				    //combo.addItem((String)choices.elementAt(j));
				    System.out.println("Adding " + (String)choices.elementAt(j));
				  }
				// This is what's doing it.

				combo.setMaximumRowCount(4);
				combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
				combo.setEditable(true);
				combo.setVisible(true);

				try
				  {
				    combo.setCurrentValue(fields[i].getValue());
				    System.out.println("Setting current value: " + fields[i].getValue());
				  }
				catch (RemoteException rx)
				  {
				    throw new RuntimeException("Could not get value for field: " + rx);
				  }
				
				combo.addItemListener(this);
				objectHash.put(combo, fields[i]);
				System.out.println("Adding to panel");
				try
				  {
				    addRow(panel, combo, name, i, fields[i].isVisible());
				  }
				catch (RemoteException rx)
				  {
				    throw new RuntimeException("Could not check visibility");
				  }
			      }
			  }
			else
			  {
			    // It's not a choice
			    System.out.println("This is not a choice");

			    sf = new JstringField(20,
						  19,
						  new JcomponentAttr(null,
								     new Font("Helvetica",Font.PLAIN,12),
								     Color.black,Color.white),
						  editable,
						  false,
						  null,
						  null,
						  this);
			    
			    objectHash.put(sf, fields[i]);
			    
			    try
			      {
				sf.setText((String)fields[i].getValue());
			      }
			    catch (RemoteException rx)
			      {
				throw new RuntimeException("Could not get value for field: " + rx);
			      }
			    
			    //sf.setCallback(this);
			    //sf.setEditable(editable);
			    
			    try
			      {
				sf.setToolTipText((String)fields[i].getComment());
				//System.out.println("Setting tool tip to " + (String)fields[i].getComment());
			      }
			    catch (RemoteException rx)
			      {
				throw new RuntimeException("Could not get tool tip text: " + rx);
			      }
			    try
			      {
				addRow(panel, sf, name, i, fields[i].isVisible());
			      }
			    catch (RemoteException rx)
			      {
				throw new RuntimeException("Could not check visibility");
			      }
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not set up stringfield: " + rx);
		      }

		    break;

		  case FieldType.PASSWORD:

		    if (editable)
		      {
			JpassField pf = new JpassField(parent, true, 10, 8, editable);
			objectHash.put(pf, fields[i]);
		      
			pf.setCallback(this);
		      
			try
			  {
			    pf.setToolTipText((String)fields[i].getComment());
			    //System.out.println("Setting tool tip to " + (String)fields[i].getComment());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not get tool tip text: " + rx);
			  }
			try
			  {
			    addRow(panel, pf, name, i, fields[i].isVisible());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not check visibility");
			  }
		      }
		    else
		      {
			sf = new JstringField(20,
					      19,
					      new JcomponentAttr(null,
								 new Font("Helvetica",Font.PLAIN,12),
								 Color.black,Color.white),
					      true,
					      false,
					      null,
					      null);

			objectHash.put(sf, fields[i]);

			// the server won't give us an unencrypted password, we're clear here

			try
			  {
			    sf.setText((String)fields[i].getValue());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not get value for field: " + rx);
			  }
		      
			sf.setEditable(false);
			try
			  {
			    addRow(panel, sf, name, i, fields[i].isVisible());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not check visibility");
			  }
		      }
		    
		    break;

		  case FieldType.NUMERIC:

		    System.out.println("Numreic field. skipping");

		    break;

		  case FieldType.DATE:

		    JdateField df = new JdateField();

		    objectHash.put(df, fields[i]);
		    df.setEditable(editable);
		    df.setCallback(this);

		    try
		      {
			Date date = ((Date)fields[i].getValue());

			if (date != null)
			  {
			    df.setDate(date);
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not get date: " + rx);
		      }
		    try
		      {
			addRow(panel, df, name, i, fields[i].isVisible());
		      }
		    catch (RemoteException rx)
		      {
		    throw new RuntimeException("Could not check visibility");
		      }
		    break;

		  case FieldType.BOOLEAN:

		    //JcheckboxField cb = new JcheckboxField();
		    JCheckbox cb = new JCheckbox();
		    objectHash.put(cb, fields[i]);
		    cb.setEnabled(editable);
		    cb.addActionListener(this);
		    //cb.setCallback(this);

		    try
		      {
			cb.setSelected(((Boolean)fields[i].getValue()).booleanValue());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not set checkbox value: " + rx);
		      }
		    catch (NullPointerException ex)
		      {
			System.out.println("Null pointer: " + ex);
		      }
		    try
		      {
			addRow(panel, cb, name, i, fields[i].isVisible());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not check visibility");
		      }

		    break;

		  case FieldType.PERMISSIONMATRIX:

		    if (debug)
		      {
			System.out.println("Adding perm matrix");
		      }

		    perm_button pb = new perm_button((perm_field) fields[i],
						     editable,
						     parent.baseHash);
		    try
		      {
			addRow(panel, pb, name, i, fields[i].isVisible());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not check visibility");
		      }

		    break;

		  case FieldType.INVID:

		    try
		      {
			if (editable && fields[i].isEditable())
			  {
			    
			    
			  }
			else
			  {
			    if (fields[i].getValue() != null)
			      {
				String label = (String)parent.getSession().view_db_object((Invid)fields[i].getValue()).getLabel();
				addRow(panel, new JLabel(label), name, i, fields[i].isVisible());
			      }
			    else
			      {
				
				addRow(panel, new JLabel("null invid"), name, i, fields[i].isVisible());
			      }
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not get invid field: " + rx);
		      }
		    break;

		  default:
		    
		    JLabel label = new JLabel("(Unknown)Field type ID = " + type);
		    addRow(panel, label, name, i);
		  }
	      }
	  }
      }
    
    if (debug)
      {
	System.out.println("Done with loop");
      }
    
    add("Center", panel);
    
    //scrollpane = new JScrollPane();
    
    //scrollpane.setViewport(vp);
    
    //scrollpane.getViewport().add(jpanel);
    //w.add(scrollpane);
    
    //w.setBounds(20,20, panel.getPreferredSize().width, panel.getPreferredSize().height);
    
    }

  /**
   * Goes through all the components and checks to see if they should be visible,
   * and updates their contents.
   *
   */

  public void update()
  {
    if (debug)
      {
	System.out.println("Updating container panel");
      }
    Enumeration enum = objectHash.keys();
    while (enum.hasMoreElements())
      {
	Component comp = (Component)enum.nextElement();
	try
	  {
	    db_field field = (db_field)objectHash.get(comp);
	    setRowVisible(comp, field.isVisible());
	    if (comp instanceof JstringField)
	      {
		((JstringField)comp).setText((String)field.getValue());
	      }
	    else if (comp instanceof JdateField)
	      {
		((JdateField)comp).setDate((Date)field.getValue());
	      }
	    else if (comp instanceof JnumberField)
	      {
		((JnumberField)comp).setText((String)field.getValue());
	      }
	    else if (comp instanceof JcheckboxField)
	      {
		((JcheckboxField)comp).setSelected(((Boolean)field.getValue()).booleanValue());
	      }
	    else if (comp instanceof JCheckbox)
	      {
		((JCheckbox)comp).setSelected(((Boolean)field.getValue()).booleanValue());
	      }
	    else if (comp instanceof JComboBox)
	      {
		// arg...
	      }
	    else if (comp instanceof JLabel)
	      {
		((JLabel)comp).setText((String)field.getValue());
	      }
	    else if (comp instanceof JpassField)
	      {
		System.out.println("Passfield, ingnoring");
	      }
	    else 
	      {
		System.err.println("field of unknown type: " + comp);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check visibility");
	  }


      }
    if (debug)
      {
	System.out.println("Updating container panel");
      }

  }

  public boolean setValuePerformed(JValueObject v)
  {
    boolean returnValue = false;
    if (v.getSource() instanceof JstringField)
      {
	System.out.println((String)v.getValue());
	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());

	    if (field.setValue(v.getValue()))
	      {
		parent.somethingChanged = true;
		returnValue = true;
	      }
	    else
	      {
		System.err.println("Could not change field, reverting to " + (String)field.getValue());
		((JstringField)v.getSource()).setText((String)field.getValue());
		System.err.println("Here's what went wrong: " + parent.getSession().getLastError());
		returnValue = false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else if (v.getSource() instanceof JpassField)
      {
	System.out.println((String)v.getValue());
	pass_field field = (pass_field)objectHash.get(v.getSource());

	try
	  {
	    System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());

	    if (field.setPlainTextPass((String)v.getValue()))
	      {
		parent.somethingChanged = true;
		returnValue = true;
	      }
	    else
	      {
		System.err.println("Could not change field");
		returnValue =  false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
 
      }
    else if (v.getSource() instanceof JdateField)
      {
	System.out.println("date field changed");
	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    parent.somethingChanged = true;
	    returnValue =  field.setValue(((JdateField)v.getSource()).getDate());
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else if (v.getSource() instanceof vectorPanel)
      {
	System.out.println("Something happened in the vector panel");
	parent.somethingChanged = true;
      }
    else
      {
	System.out.println("Value performed from unknown source");
      }

    // Check to see if anything needs updating.
    try
      {
	if (object.shouldRescan())
	  {
	    update();
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not call shouldRescan(): " + rx);
      }

    return returnValue;
  }


  public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() instanceof JCheckbox)
	{
	  db_field field = (db_field)objectHash.get(e.getSource());
	  
	  try
	    {
	      
	      if (field.setValue(new Boolean(((JCheckbox)e.getSource()).isSelected())))
		{
		  parent.somethingChanged = true;
		}
	      else
		{
		  System.err.println("Could not change checkbox, resetting it now");
		  ((JCheckbox)e.getSource()).setSelected(((Boolean)field.getValue()).booleanValue());
		}
	    }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
	}
      else
	{
	  System.err.println("Unknown ActionEvent in containerPanel");
	}

      // Check to see if anything needs updating.
      try
	{
	  if (object.shouldRescan())
	    {
	      update();
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not call shouldRescan(): " + rx);
	}
     
    }
  public void itemStateChanged(ItemEvent e)
    {
      System.out.println("Item changed: " + e.getItem());
    }
  
  void addRow(JComponent parent, Component comp,  String label, int row, boolean visible)
  {
    addRow(parent, comp, label, row);
    setRowVisible(comp, visible);
  }

  void addRow(JComponent parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);
    comp.setBackground(ClientColor.ComponentBG);
    parent.add("0 " + row + " lthwHW", l);
    parent.add("1 " + row + " lthwHW", comp);
  }

  void setRowVisible(Component comp, boolean b)
  {
    Component c = (Component) rowHash.get(comp);

    if (c == null)
      {
	return;
      }

    comp.setVisible(b);
    c.setVisible(b);
  }
}
