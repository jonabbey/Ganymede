/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.4 $ %D%
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

  // -- 
  
  gclient
    parent;			// our interface to the server

  db_object
    object;			// the object we're editing
  
  windowPanel
    winP;			// for interacting with our containing context

  Hashtable
    rowHash, 
    objectHash;
  
  JPanel 
    panel;			// currently not very useful.. ?
  
  TableLayout 
    layout;
  
  db_field[] 
    fields = null;
  
  JScrollPane
    scrollpane;
  

  JButton
    editB;

  int row = 0;			// we'll use this to keep track of rows added as we go along

  boolean
    editable;
  
  /* -- */

  /**
   *
   * Main constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   *
   */

  public containerPanel(db_object object, boolean editable, gclient parent, windowPanel window)
  {
    String tempString = null;

    /* -- */

    if (object == null)
      {
	System.err.println("null object passed to containerPanel");
	return;
      }

    this.parent = parent;
    this.winP = window;
    this.object = object;
    this.editable = editable;

    objectHash = new Hashtable();
    rowHash = new Hashtable();

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
      
    if (fields != null)
      {
	for (int i = 0; i < fields.length ; i++)
	  {
	    try
	      {
		tempString = fields[i].getName();
		addFieldComponent(fields[i]);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote exception adding field " + tempString);
	      }
	  }
      }
      
    if (debug)
      {
	System.out.println("Done with loop");
      }
    
    //scrollPane = new JScrollPane();
    //scrollPane.add(panel);
    
    add("Center", panel);
      
    //scrollpane = new JScrollPane();
    
    //scrollpane.setViewport(vp);
    
    //scrollpane.getViewport().add(jpanel);
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
	System.out.println("Done updating container panel");
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    boolean returnValue = false;

    /* -- */

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

    if (e.getSource() instanceof JComboBox)
      {
	db_field field = (db_field)objectHash.get(e.getSource());

	try
	  {
	    field.setValue((String)e.getItem());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set combo box value: " + rx);
	  }
      }
  }
  
  void addRow(JComponent parent, Component comp,  String label, boolean visible)
  {
    addRow(parent, comp, label);
    setRowVisible(comp, visible);
  }

  void addRow(JComponent parent, Component comp,  String label)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);
    comp.setBackground(ClientColor.ComponentBG);
    parent.add("0 " + row + " lthwHW", l);
    parent.add("1 " + row + " lthwHW", comp);

    row++;
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

  /**
   *
   * Helper method to add a component during constructor operation
   *
   */

  private void addFieldComponent(db_field field) throws RemoteException
  {
    short type;
    String name = null;
    boolean isVector;
    boolean isEditInPlace;

    /* -- */

    if (field == null)
      {
	throw new IllegalArgumentException("null field");
      }

    type = field.getType();
    name = field.getName();
    isVector = field.isVector();
    isEditInPlace = field.isEditInPlace();

    if (debug)
      {
	System.out.println("Name: " + name + "Field type desc: " + type);
      }
    
    if (isVector)
      {
	if (type == FieldType.STRING)
	  {
	    addStringVector((string_field) field);
	  }
	else if (type == FieldType.INVID && !isEditInPlace)
	  {
	    addInvidVector((invid_field) field);
	  }
	else			// generic vector
	  {
	    addVectorPanel(field);
	  }
      }
    else if (type == FieldType.INVID && isEditInPlace)
      {
	// Add a new Container Panel here

	try 
	  {
	    addRow(panel, new JLabel("new container panel will go here"), name, field.isVisible());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check visibility");
	  }
      }
    else
      {
	// plain old component

	switch (type)
	  {
	  case -1:
	    System.err.println("Could not get field information");
	    break;
		      
	  case FieldType.STRING:
	    addStringField((string_field) field);
	    break;
		      
	  case FieldType.PASSWORD:
	    addPasswordField((pass_field) field);
	    break;
		      
	  case FieldType.NUMERIC:
	    addNumericField(field);
	    break;
		      
	  case FieldType.DATE:
	    addDateField(field);
	    break;
		      
	  case FieldType.BOOLEAN:
	    addBooleanField(field);
	    break;
		      
	  case FieldType.PERMISSIONMATRIX:
	    addPermissionField(field);
	    break;
		      
	  case FieldType.INVID:
	    addInvidField(field);
	    break;
		      
	  default:
	    JLabel label = new JLabel("(Unknown)Field type ID = " + type);
	    addRow(panel, label, name);
	  }
      }
  }

  /**
   *
   * private helper method to instantiate a string vector in this
   * container panel
   *
   */

  private void addStringVector(string_field field) throws RemoteException
  {
    System.out.println("Adding StringSelector, its a vector of strings!");

    stringSelector ss = new stringSelector(gclient.parseDump(field.choices()),
					   field.getValues(), 
					   this,
					   editable);
    addRow(panel, ss, field.getName(), field.isVisible()); 
  }

  /**
   *
   * private helper method to instantiate an invid vector in this
   * container panel
   *
   */

  private void addInvidVector(invid_field field) throws RemoteException
  {
    Vector
      valueResults,
      valueHandles,
      choiceResults = null, 
      choiceHandles = null;

    Result 
      result;

    /* -- */

    System.out.println("Adding StringSelector, its a vector of invids!");
    
    valueResults = gclient.parseDump(field.encodedValues());

    if (editable)
      {
	choiceResults = gclient.parseDump(field.choices());
	choiceHandles = new Vector();
      }

    valueHandles = new Vector();

    for (int i = 0; i < valueResults.size(); i++)
      {
	result = (Result) valueResults.elementAt(i);

	valueHandles.addElement(new listHandle(result.toString(), result.getInvid()));
      }

    if (editable)
      {
	for (int i = 0; i < choiceResults.size(); i++)
	  {
	    result = (Result) choiceResults.elementAt(i);
	    
	    choiceHandles.addElement(new listHandle(result.toString(), result.getInvid()));
	  }
      }

    stringSelector ss = new stringSelector(choiceHandles, valueHandles, this, editable);
    addRow(panel, ss, field.getName(), field.isVisible()); 
  }

  /**
   *
   * private helper method to instantiate a vector panel in this
   * container panel
   *
   */

  private void addVectorPanel(db_field field) throws RemoteException
  {
    boolean isEditInPlace = field.isEditInPlace();

    /* -- */

    if (debug)
      {
	if (isEditInPlace)
	  {
	    System.out.println("Adding editInPlace vector panel");
	  }
	else
	  {
	    System.out.println("Adding normal vector panel");
	  }
      }

    vectorPanel vp = new vectorPanel(field, winP, editable, isEditInPlace);

    try
      {
	addRow(panel, vp, field.getName(), field.isVisible());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
  }

  /**
   *
   * private helper method to instantiate a string field in this
   * container panel
   *
   */

  private void addStringField(string_field field) throws RemoteException
  {
    JstringField
      sf;

    /* -- */

    try 
      {
	if (field.canChoose())
	  {
	    System.out.println("You can choose");
			      
	    if (field.mustChoose())
	      {
		System.out.println("You must choose.");
				  
				// Add a choice
				  
		JChoice choice = new JChoice();
		Vector choices = gclient.parseDump(field.choices());
				  
		for (int j = 0; j < choices.size(); j++)
		  {
		    choice.addItem((String)choices.elementAt(j));
		  }
				  
		choice.setEditable(editable);
		choice.addItemListener(this); // register callback
		choice.setVisible(true);
				  
		try
		  {
		    choice.setCurrentValue(field.getValue());
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not get value for field: " + rx);
		  }
				  
		objectHash.put(choice, field);

		try
		  {
		    addRow(panel, choice, field.getName(), field.isVisible());
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

		Vector choices = gclient.parseDump(field.choices());
		String currentChoice = (String) field.getValue();
		boolean found = false;

		for (int j = 0; j < choices.size(); j++)
		  {
		    String thisChoice = (String)choices.elementAt(j);
		    combo.addPossibleValue(thisChoice);

		    if (!found)
		      {
			if (thisChoice.equals(currentChoice))
			  {
			    found = true;
			  }
		      }

		    System.out.println("Adding " + (String)choices.elementAt(j));
		  }

		// if the current value wasn't in the choice, add it in now
				  
		if (!found)
		  {
		    combo.addPossibleValue(currentChoice);
		  }

		combo.setMaximumRowCount(8);
		combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
		combo.setEditable(false);
		combo.setVisible(true);

		combo.setCurrentValue(currentChoice);
		System.out.println("Setting current value: " + currentChoice);
				  
		combo.addItemListener(this); // register callback
		objectHash.put(combo, field);
		System.out.println("Adding to panel");

		try
		  {
		    addRow(panel, combo, field.getName(), field.isVisible());
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
				  field.maxSize(),
				  new JcomponentAttr(null,
						     new Font("Helvetica",Font.PLAIN,12),
						     Color.black,Color.white),
				  editable,
				  false,
				  null,
				  null,
				  this);
			      
	    objectHash.put(sf, field);
			      
	    try
	      {
		sf.setText((String)field.getValue());
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get value for field: " + rx);
	      }
			    
	    sf.setCallback(this);
	    sf.setEditable(editable);

	    try
	      {
		sf.setToolTipText(field.getComment());

		// System.out.println("Setting tool tip to " + field.getComment());
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get tool tip text: " + rx);
	      }
					    

	    try
	      {
		addRow(panel, sf, field.getName(), field.isVisible());
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
  }

  /**
   *
   * private helper method to instantiate a password field in this
   * container panel
   *
   */

  private void addPasswordField(pass_field field) throws RemoteException
  {
    JstringField sf;

    /* -- */

    if (editable)
      {
	JpassField pf = new JpassField(parent, true, 10, 8, editable);
	objectHash.put(pf, field);
			  
	pf.setCallback(this);
			  
	try
	  {
	    addRow(panel, pf, field.getName(), field.isVisible());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check visibility");
	  }
      }
    else
      {
	sf = new JstringField(20,
			      field.maxSize(),
			      new JcomponentAttr(null,
						 new Font("Helvetica",Font.PLAIN,12),
						 Color.black,Color.white),
			      true,
			      false,
			      null,
			      null);

	objectHash.put(sf, field);
			  
	// the server won't give us an unencrypted password, we're clear here
			  
	try
	  {
	    sf.setText((String)field.getValue());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get value for field: " + rx);
	  }
		      
	sf.setEditable(false);

	try
	  {
	    sf.setToolTipText(field.getComment());
	    
	    // System.out.println("Setting tool tip to " + field.getComment());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get tool tip text: " + rx);
	  }
		
	try
	  {
	    addRow(panel, sf, field.getName(), field.isVisible());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check visibility");
	  }
      }
  }

  /**
   *
   * private helper method to instantiate a numeric field in this
   * container panel
   *
   */

  private void addNumericField(db_field field) throws RemoteException
  {
    System.out.println("Numeric field. skipping");
  }

  /**
   *
   * private helper method to instantiate a date field in this
   * container panel
   *
   */

  private void addDateField(db_field field) throws RemoteException
  {
    JdateField df = new JdateField();
		      
    objectHash.put(df, field);
    df.setEditable(editable);

    try
      {
	Date date = ((Date)field.getValue());
			  
	if (date != null)
	  {
	    df.setDate(date);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date: " + rx);
      }

    // note that we set the callback after we initially set the
    // date, to avoid having the callback triggered on a listing

    df.setCallback(this);

    try
      {
	addRow(panel, df, field.getName(), field.isVisible());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
  }

  /**
   *
   * private helper method to instantiate a boolean field in this
   * container panel
   *
   */

  private void addBooleanField(db_field field) throws RemoteException
  {
    //JcheckboxField cb = new JcheckboxField();

    JCheckbox cb = new JCheckbox();
    objectHash.put(cb, field);
    cb.setEnabled(editable);
    cb.addActionListener(this);	// register callback

    try
      {
	cb.setSelected(((Boolean)field.getValue()).booleanValue());
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
	addRow(panel, cb, field.getName(), field.isVisible());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
  }

  /**
   *
   * private helper method to instantiate a permission matrix field in this
   * container panel
   *
   */

  private void addPermissionField(db_field field) throws RemoteException
  {
    if (debug)
      {
	System.out.println("Adding perm matrix");
      }

    // note that the permissions editor does its own callbacks to
    // the server, albeit using our transaction / session.

    perm_button pb = new perm_button((perm_field) field,
				     editable,
				     parent.baseHash);
    try
      {
	addRow(panel, pb, field.getName(), field.isVisible());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
  }

  /**
   *
   * private helper method to instantiate an invid field in this
   * container panel
   *
   */

  private void addInvidField(db_field field) throws RemoteException
  {
    if (editable && field.isEditable())
      {
	// really probably ought to do a combo box here.. we shouldn't
	// ever have an invid field without a list of choices provided
	// us by the server
      }
    else
      {
	if (field.getValue() != null)
	  {
	    String label = (String)parent.getSession().view_db_object((Invid)field.getValue()).getLabel();
	    addRow(panel, new JLabel(label), field.getName(), field.isVisible());
	  }
	else
	  {
	    addRow(panel, new JLabel("null invid"), field.getName(), field.isVisible());
	  }
      }
  }


}
