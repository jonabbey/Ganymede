/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.24 $ %D%
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

  protected framePanel
    frame;

  Hashtable
    rowHash, 
    objectHash;
  
  JPanel 
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

  JProgressBar
    progressBar;
  
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
    this(object, editable, parent, window, frame, null);
  }
  /**
   *
   * Main constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   *
   */

  public containerPanel(db_object object, boolean editable, gclient parent, windowPanel window, framePanel frame, JProgressBar progressBar)
  {

    /* -- */

    if (object == null)
      {
	System.err.println("null object passed to containerPanel");
	parent.setStatus("Could not get object.  Someone else might be editting it.  Try again at a later time.");
	return;
      }

    this.parent = parent;
    this.winP = window;
    this.object = object;
    this.editable = editable;
    this.frame = frame;
    this.progressBar = progressBar;

    objectHash = new Hashtable();
    rowHash = new Hashtable();

    //    setLayout(new BorderLayout());

    //panel = new JPanel();
    layout = new TableLayout(false);
    layout.rowSpacing(5);
    setLayout(layout);
      
    // Get the list of fields
    
    try
      {
	// get all the custom fields
	fields = object.listFields(true);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get the fields: " + rx);
      }

    if (progressBar != null)
      {
	progressBar.setMinimum(0);
	progressBar.setMaximum(fields.length);
      }

    if (debug)
      {
	System.out.println("Entering big loop");
      }
      
    if (fields != null)
      {
	for (int i = 0; i < fields.length ; i++)
	  {
	    if (progressBar != null)
	      {
		progressBar.setValue(i);
	      }

	    try
	      {
		short ID = fields[i].getID();
		short type = object.getTypeID();
		
		// Skip some fields.  custom panels hold the built ins, and a few others.

		if (((type== SchemaConstants.OwnerBase) && (ID == SchemaConstants.OwnerObjectsOwned)) 
		    || ((type == SchemaConstants.UserBase) && (ID == SchemaConstants.UserAdminPersonae))
		    || ((ID == SchemaConstants.ContainerField) && object.isEmbedded()))
		  {
		    if (debug)
		      {
			System.out.println("Skipping a special field: " + fields[i].getName());
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
    
    if (progressBar != null)
      {
	progressBar.setValue(0);
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

  /**
   *
   * This method does causes the hierarchy of containers above
   * us to be recalculated from the bottom (us) on up.  Normally
   * the validate process works from the top-most container down,
   * which isn't what we want at all in this context.
   *
   */

  public void invalidateRight()
  {
    Component c;

    c = this;

    while ((c != null) && !(c instanceof JViewport))
      {
	//System.out.println("doLayout on " + c);

	c.doLayout();
	c = c.getParent();
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    if (v.getOperationType() == JValueObject.ERROR)
      {
	parent.setStatus((String)v.getValue());
	return true;
      }

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
	    String oldValue = (String)field.getValue();


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
		    System.err.println("Could not change field, reverting to " + oldValue);
		  }
		/*
		((JstringField)v.getSource()).setText("");

		// This isn't working for some reason
		((JstringField)v.getSource()).setText(oldValue);

		System.out.println("text is: " + ((JstringField)v.getSource()).getText());
		System.out.println("text is: " + ((JstringField)v.getSource()).getValue());
		*/
		parent.setStatus("Could not change field: " + parent.getSession().getLastError());

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
	    returnValue =  field.setValue(v.getValue());
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
    else if (v.getSource() instanceof tStringSelector)
      {
	if (debug)
	  {
	    System.out.println("value performed from tStringSelector");
	  }
	if (v.getOperationType() == JValueObject.ERROR)
	  {
	    parent.setStatus((String)v.getValue());
	  }
	else if (v.getValue() instanceof Invid)
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
		    returnValue = (field.deleteElement(invid));
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
	    System.out.println("String tStringSelector callback, not implemented yet");
	    returnValue = false;
	  }
	else
	  {
	    System.out.println("Not an Invid in string selector.");
	  }
      }
    else if (v.getSource() instanceof JIPField)
      {
	if (debug)
	  {
	    System.out.println("ip field changed");
	  }

	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    parent.somethingChanged = true;
	    returnValue =  field.setValue(v.getValue());
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set ip field value: " + rx);
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

    System.out.println("returnValue: " + returnValue);

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
	    Object item = e.getItem();
	    if (item instanceof String)
	      {
		ok = field.setValue((String)e.getItem());
	      }
	    else if (item instanceof listHandle)
	      {
		ok = field.setValue(((Invid) ((listHandle)e.getItem()).getObject() ));

	      }
	    else 
	      {
		System.out.println("Unknown type from JComboBox: " + item);
	      }
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

  void addVectorRow(Component comp, String label, boolean visible)
  {
    // create a dummy label for consistency

    JLabel l = new JLabel("");
    rowHash.put(comp, l);
    
    //comp.setBackground(ClientColor.ComponentBG);
    add("0 " + row + " 2 lthH", comp); // span 2 columns, no label
    row++;

    setRowVisible(comp, visible);
  }
  
  void addRow(Component comp,  String label, boolean visible)
  {
    // create a dummy label for consistency

    JLabel l = new JLabel(label);
    rowHash.put(comp, l);

    comp.setBackground(ClientColor.ComponentBG);

    add("0 " + row + " lthH", l);
    add("1 " + row + " lthH", comp);
    
    row++;

    setRowVisible(comp, visible);
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

    invalidateRight();
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
	    addInvidField((invid_field)field);
	    break;

	  case FieldType.IP:
	    addIPField((ip_field) field);
	    break;
		      
	  default:
	    JLabel label = new JLabel("(Unknown)Field type ID = " + type);
	    addRow( label, name, true);
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

    if (field == null)
      {
	System.out.println("Hey, this is a null field! " + field.getName());

      }

    QueryResult qr = null;


    Object id = field.choicesKey();
    if (id == null)
      {
	qr = field.choices();
      }
    else
      {
	if (parent.cachedLists.containsKey(id))
	  {
	    qr = (QueryResult)parent.cachedLists.get(id);
	  }
	else
	  {	
	    qr =field.choices();
	    if (qr != null)
	      {
		parent.cachedLists.put(id, qr);
	      }
	  }
      }
    


    if (qr == null)
      {
	tStringSelector ss = new tStringSelector(null,
						 field.getValues(), 
						 this,
						 editable,
						 false,  //canChoose
						 false,  //mustChoose
						 100);
	objectHash.put(ss, field);
	ss.setCallback(this);
	addRow( ss, field.getName(), field.isVisible()); 
      }
    else
      {
	tStringSelector ss = new tStringSelector(qr.getLabels(),
						 field.getValues(), 
						 this,
						 editable,
						 true,   //canChoose
						 false,  //mustChoose
						 100);
	objectHash.put(ss, field);
	ss.setCallback(this);
	addRow( ss, field.getName(), field.isVisible()); 
      }
  }

  /**
   *
   * private helper method to instantiate an invid vector in this
   * container panel
   *
   */

  private void addInvidVector(invid_field field) throws RemoteException
  {
    QueryResult
      valueResults = null,
      choiceResults = null;

    Vector
      valueHandles = null,
      choiceHandles = null;

    /* -- */

    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of invids!");
      }

    valueHandles = field.encodedValues().getListHandles();

    if (editable)
      {
	Object key = field.choicesKey();

	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null, downloading new copy");
	      }
	    choiceHandles = field.choices().getListHandles();
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key= " + key);
	      }

	    if (parent.cachedLists.containsKey(key))
	      {
		if (debug)
		  {
		    System.out.println("It's in there, using cached list");
		  }
		choiceHandles = (Vector)parent.cachedLists.get(key);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("It's not in there, downloading anew.");
		  }

		choiceHandles = field.choices().getListHandles();
		parent.cachedLists.put(key, choiceHandles);
	      }
	  }
      }

    // This is taken out, because we use getListHandles() now, 

    /*
    for (int i = 0; i < valueResults.size(); i++)
      {
	valueHandles.addElement(new listHandle(valueResults.getLabel(i), 
					       valueResults.getInvid(i)));
      }

     if (editable)
      {
	if (choiceResults != null)
	  {
	    for (int i = 0; i < choiceResults.size(); i++)
	      {
		choiceHandles.addElement(new listHandle(choiceResults.getLabel(i),
							choiceResults.getInvid(i)));
	      }
	  }
      }*/

    // ss is canChoose, mustChoose

    tStringSelector ss = new tStringSelector(choiceHandles, valueHandles, this, editable, true, true, 100);
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
	addVectorRow( vp, field.getName(), field.isVisible());
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
	    
	    JComboBox combo = new JComboBox();
	    
	    Vector choices = field.choices().getLabels();
	    String currentChoice = (String) field.getValue();
	    boolean found = false;
	    
	    for (int j = 0; j < choices.size(); j++)
	      {
		String thisChoice = (String)choices.elementAt(j);
		combo.addItem(thisChoice);
		
		if (!found && (currentChoice != null))
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
	    
	    if (!found && (currentChoice != null))
	      {
		combo.addItem(currentChoice);
	      }
	    
	    combo.setMaximumRowCount(8);
	    combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	    combo.setEditable(false); // this should be setEditable(mustChoose());
	    combo.setVisible(true);
	    
	    if (currentChoice != null)
	      {
		combo.setSelectedItem(currentChoice);
	      }

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
	else
	  {
	    // It's not a choice
      
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

  private void addInvidField(invid_field field) throws RemoteException
  {
    if (editable && field.isEditable())
      {
	Vector choices = field.choices().getListHandles();
        Invid currentChoice = (Invid) field.getValue();
	listHandle currentListHandle = null;
	listHandle noneHandle = new listHandle("<none>", null);
	boolean found = false;
	JComboBox combo = new JComboBox();
	
	/* -- */

	combo.addItem(noneHandle);
	
	for (int j = 0; j < choices.size(); j++)
	  {
	    listHandle thisChoice = (listHandle) choices.elementAt(j);
	    combo.addItem(thisChoice);
	    
	    if (!found && (currentChoice != null))
	      {
		if (thisChoice.getObject().equals(currentChoice))
		  {
		    if (debug)
		      {
			System.out.println("Found the current object in the list!");
		      }
		    currentListHandle = thisChoice;
		    found = true;
		  }
	      }
	    
	    if (debug)
	      {
		System.out.println("Adding " + (listHandle)choices.elementAt(j));
	      }
	  }
	
	// if the current value wasn't in the choice, add it in now
	
	if (!found)
	  {
	    if (currentChoice != null)
	      {
		currentListHandle = new listHandle(parent.getSession().viewObjectLabel(currentChoice), currentChoice);
		combo.addItem(currentListHandle);
	      }
	  }
	
	combo.setMaximumRowCount(12);
	combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	combo.setEditable(false); // This should be true
	combo.setVisible(true);

	if (currentChoice != null)
	  {
	    if (debug)
	      {
		System.out.println("setting current choice: " + currentChoice);
	      }
	    combo.setSelectedItem(currentListHandle);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("currentChoice is null");
	      }
	    combo.setSelectedItem(noneHandle);
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
   *
   * private helper method to instantiate an ip field in this
   * container panel
   *
   */

  private void addIPField(ip_field field) throws RemoteException
  {
    JIPField
      ipf;

    Byte[] bytes;

    /* -- */

    try
      {
	ipf = new JIPField(new JcomponentAttr(null,
					      new Font("Helvetica",Font.PLAIN,12),
					      Color.black,Color.white),
			   editable,
			   field.v6Allowed());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get determine v6Allowed for ip field: " + rx);
      }
    
    objectHash.put(ipf, field);
    
    try
      {
	bytes = (Byte[]) field.getValue();

	if (bytes != null)
	  {
	    ipf.setValue(bytes);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get value for field: " + rx);
      }
	
    ipf.setCallback(this);

    try
      {
	ipf.setToolTipText(field.getComment());
	    
	// System.out.println("Setting tool tip to " + field.getComment());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get tool tip text: " + rx);
      }
	
    try
      {
	addRow( ipf, field.getName(), field.isVisible());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility: " + rx);
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
