/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.14 $ %D%
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

public class containerPanel extends JBufferedPane implements ActionListener, JsetValueCallback, ItemListener{  

  static final boolean debug = false;

  // -- 
  
  gclient
    parent;			// our interface to the server

  db_object
    object;			// the object we're editing
  
  windowPanel
    winP;			// for interacting with our containing context

  protected framePanel
    frame;

  Hashtable
    rowHash, 
    objectHash;
  
  JBufferedPane 
    panel;			// currently not very useful.. ?
  
  TableLayout 
    layout;
  
  db_field[] 
    fields = null;
  
  JViewport
    vp;

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

  public containerPanel(db_object object, boolean editable, gclient parent, windowPanel window, framePanel frame)
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
    this.frame = frame;

    objectHash = new Hashtable();
    rowHash = new Hashtable();

    //    setLayout(new BorderLayout());

    //panel = new JBufferedPane();
    layout = new TableLayout(false);
    layout.rowSpacing(5);
    setLayout(layout);
      
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
		if (debug)
		  {
		    tempString = fields[i].getName();
		  }
	
		if (fields[i].isBuiltIn())
		  {
		    if (debug)
		      {
			System.out.println("Skipping a built in fieldfields");
		      }
		  }
		else
		  {
		    addFieldComponent(fields[i]);
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote exception adding field " + ex);
	      }
	  }
      }
      
    if (debug)
      {
	System.out.println("Done with loop");
      }

    //setViewportView(panel);
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
	    else if (comp instanceof JCheckBox)
	      {
		((JCheckBox)comp).setSelected(((Boolean)field.getValue()).booleanValue());
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
	if (debug)
	  {
	    System.out.println((String)v.getValue());
	  }
	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    if (debug)
	      {
		System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());
	      }

	    if (field.setValue(v.getValue()))
	      {
		parent.somethingChanged = true;
		returnValue = true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Could not change field, reverting to " + (String)field.getValue());
		  }
		((JstringField)v.getSource()).setText((String)field.getValue());
		if (debug)
		  {
		    System.err.println("Here's what went wrong: " + parent.getSession().getLastError());
		  }

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
	if (debug)
	  {
	    System.out.println((String)v.getValue());
	  }

	pass_field field = (pass_field)objectHash.get(v.getSource());

	try
	  {
	    if (debug)
	      {
		System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());
	      }

	    if (field.setPlainTextPass((String)v.getValue()))
	      {
		parent.somethingChanged = true;
		returnValue = true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Could not change field");
		  }

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
	if (debug)
	  {
	    System.out.println("date field changed");
	  }

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
    else if (v.getSource() instanceof stringSelector)
      {
	if (debug)
	  {
	    System.out.println("value performed from stringSelector");
	  }
	if (v.getValue() instanceof Invid)
	  {
	    db_field field = (db_field)objectHash.get(v.getSource());
	    if (field == null)
	      {
		throw new RuntimeException("Could not find field in objectHash");
	      }
	    Invid invid = (Invid)v.getValue();
	    int index = v.getIndex();
	    try
	      {
		if (v.getOperationType() == JValueObject.ADD)
		  {
		    if (debug)
		      {
			System.out.println("Adding new value to string selector");
		      }
		    returnValue = (field.addElement(invid));
		  }
		else if (v.getOperationType() == JValueObject.DELETE)
		  {
		    if (debug)
		      {
			System.out.println("Removing value from field(strig selector)");
		      }
		    returnValue = (field.deleteElement(index));
		  }
		if (debug)
		  {
		    if (returnValue)
		      {
			System.out.println("returned true");
		      }
		    else
		      {
			System.out.println("returned false");
		      }
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not change owner field: " + rx);
	      }
	  }
	else if (v.getValue() instanceof String)
	  {
	    System.out.println("String stringSelector callback, not implemented yet");
	  }
	else
	  {
	    System.out.println("Not an Invid in string selector.");
	  }
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
    if (e.getSource() instanceof JCheckBox)
      {
	db_field field = (db_field)objectHash.get(e.getSource());
	  
	try
	  {
	      
	    if (field.setValue(new Boolean(((JCheckBox)e.getSource()).isSelected())))
	      {
		parent.somethingChanged = true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Could not change checkbox, resetting it now");
		  }
		((JCheckBox)e.getSource()).setSelected(((Boolean)field.getValue()).booleanValue());
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
    if (debug)
      {
	System.out.println("Item changed: " + e.getItem());
      }

    if (e.getSource() instanceof JComboBox)
      {
	db_field field = (db_field)objectHash.get(e.getSource());

	try
	  {
	    boolean ok = false;
	    ok = field.setValue((String)e.getItem());
	    if (debug)
	      {
		if (ok)
		  {
		    System.out.println("field setValue returned true");
		  }
		else
		  {
		    System.out.println("field setValue returned FALSE!!");
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set combo box value: " + rx);
	  }
      }
    else
      {
	System.out.println("Not from a JCombobox");
      }
  }
  
  void addRow(Component comp,  String label, boolean visible)
  {
    addRow(comp, label);
    setRowVisible(comp, visible);
  }

  void addRow(Component comp,  String label)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);
    comp.setBackground(ClientColor.ComponentBG);
    add("0 " + row + " lthwHW", l);
    add("1 " + row + " lthwHW", comp);

    invalidate();
    frame.validate();

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
	try
	  {
	    if (debug)
	      {
		System.out.println("Hey, " + field.getName() + " is edit in place but not a vector, what gives?");
	      }
	    addRow(new JLabel("edit in place non-vector"), name, field.isVisible());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Couldn't even check the name: " + rx);
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
	    addRow( label, name);
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
    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of strings!");
      }

    stringSelector ss = new stringSelector(gclient.parseDump(field.choices()),
					   field.getValues(), 
					   this,
					   editable);
    objectHash.put(ss, field);
    ss.setBorderStyle(1);
    ss.setCallback(this);
    addRow( ss, field.getName(), field.isVisible()); 
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
    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of invids!");
      }

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
    objectHash.put(ss, field);
    ss.setCallback(this);
    addRow( ss, field.getName(), field.isVisible()); 
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

    vectorPanel vp = new vectorPanel(field, winP, editable, isEditInPlace, this);

    try
      {
	addRow( vp, field.getName(), field.isVisible());
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
	    if (debug)
	      {
		System.out.println("You can choose");
	      }
      
	    if (field.mustChoose())
	      {
		if (debug)
		  {
		    System.out.println("You must choose.");
		  }	  
				// Add a choice
				  
		JComboBox choice = new JComboBox();
		Vector choices = gclient.parseDump(field.choices());
				  
		for (int j = 0; j < choices.size(); j++)
		  {
		    choice.addPossibleValue((String)choices.elementAt(j));
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
		    addRow( choice, field.getName(), field.isVisible());
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

		    if (debug)
		      {
			System.out.println("Adding " + (String)choices.elementAt(j));
		      }
		  }

		// if the current value wasn't in the choice, add it in now
				  
		if (!found)
		  {
		    combo.addPossibleValue(currentChoice);
		  }

		combo.setMaximumRowCount(8);
		combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
		combo.setEditable(false); // This should be true
		combo.setVisible(true);

		combo.setCurrentValue(currentChoice);
		if (debug)
		  {
		    System.out.println("Setting current value: " + currentChoice);
		  }	  
		combo.addItemListener(this); // register callback
		objectHash.put(combo, field);
		if (debug)
		  {
		    System.out.println("Adding to panel");
		  }

		try
		  {
		    addRow( combo, field.getName(), field.isVisible());
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
	    if (debug)
	      {
		System.out.println("This is not a choice");
	      }
      
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
		addRow( sf, field.getName(), field.isVisible());
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not check visibility: " + rx);
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
	    addRow( pf, field.getName(), field.isVisible());
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
	    addRow( sf, field.getName(), field.isVisible());
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
    // It's not a choice
    if (debug)
      {
	System.out.println("Adding numeric field");
      }
      
    JnumberField nf = new JnumberField();

			      
    objectHash.put(nf, field);
	
		      
    try
      {
	Integer value = (Integer)field.getValue();
	if (value != null)
	  {
	    nf.setValue(value.intValue());
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get value for field: " + rx);
      }
			    
    nf.setCallback(this);
    nf.setEditable(editable);
    nf.setColumns(20);
    
    try
      {
	nf.setToolTipText(field.getComment());
	
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get tool tip text: " + rx);
      }
    
    
    try
      {
	addRow( nf, field.getName(), field.isVisible());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility: " + rx);
      }
    
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
	addRow( df, field.getName(), field.isVisible());
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

    JCheckBox cb = new JCheckBox();
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
	if (debug)
	  {
	    System.out.println("Null pointer setting selected choice: " + ex);
	  }
      }

    try
      {
	addRow( cb, field.getName(), field.isVisible());
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
				     parent.getBaseHash());
    try
      {
	addRow( pb, field.getName(), field.isVisible());
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
	    addRow( new JLabel(label), field.getName(), field.isVisible());
	  }
	else
	  {
	    addRow( new JLabel("null invid"), field.getName(), field.isVisible());
	  }
      }
  }


  /**
   * The idea here is that you could use this in a catch from a RemoteException
   *
   * like this:
   * try
   *   {
   *      whatever;
   *   }
   * catch (RemoteException rx)
   *   {
   *      error("Something went wrong", rx);
   */
  private void error(String label, RemoteException rx)
    {
      try
	{
	  System.out.println("Last error: " + parent.getSession().getLastError());
	}
      catch (RemoteException ex)
	{
	  System.out.println("Exception getting last error: " + rx);
	}
      throw new RuntimeException(label + rx);
    }

}
