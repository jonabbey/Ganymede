/*

   BaseFieldEditor.java

   Base Field editor component for GASHSchema
   
   Created: 14 August 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey and Michael Mulvaney
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
                                                                 BaseFieldEditor

------------------------------------------------------------------------------*/

class BaseFieldEditor extends ScrollPane implements setValueCallback, ItemListener, TextListener {

  FieldNode
    fieldNode;

  BaseField 
    fieldDef;

  CardLayout
    card;

  Panel 
    editPanel;

  GASHSchema 
    owner;

  StringDialog
    changeLabelTypeDialog;

  TextArea
    commentT;			// all

  stringField
    nameS,			// all
    classS,			// all
    trueLabelS,			// boolean
    falseLabelS,		// boolean
    OKCharS,			// string
    BadCharS;			// string

  numberField
    idN,			// all
    maxArrayN,			// all
    minLengthN,			// string
    maxLengthN;			// string

  checkboxField
    vectorCF,			// all but password, boolean
    labeledCF,			// boolean
    editInPlaceCF,		// invid
    cryptedCF;			// password

  Choice
    typeC,			// all
    namespaceC,			// string
    targetC,			// invid
    fieldC;			// invid

  componentAttr 
    ca;

  Hashtable
    rowHash;			// to keep track of field labels

  boolean
    booleanShowing,
    numericShowing,
    dateShowing,
    stringShowing,
    referenceShowing,
    passwordShowing,
    ipShowing;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  BaseFieldEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    rowHash = new Hashtable();

    fieldDef = null;
    this.owner = owner;
    
    editPanel = new InsetPanel(10, 10, 10, 10);
    editPanel.setLayout(new TableLayout(false));
    
    ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white);
    
    idN = new numberField(20, ca, false, false, 0, 0);
    idN.setCallback(this);
    addRow(editPanel, idN, "Field ID:", 0);
    
    nameS = new stringField(20, 100, ca, true, false, null, null);
    nameS.setCallback(this);
    addRow(editPanel, nameS, "Field Name:", 1);

    classS = new stringField(20, 100, ca, true, false, null, null);
    classS.setCallback(this);
    addRow(editPanel, classS, "Class name:", 2);

    commentT = new TextArea(4, 20);
    commentT.addTextListener(this);
    addRow(editPanel, commentT, "Comment:", 3);
    
    // This one is different:
    vectorCF = new checkboxField(null, false, ca, true);
    vectorCF.setCallback(this);
    addRow(editPanel, vectorCF, "Vector:", 4);

    maxArrayN = new numberField(20, ca, true, false, 0, Integer.MAX_VALUE);
    maxArrayN.setCallback(this);
    addRow(editPanel, maxArrayN, "Max Array Size:", 5);

    typeC = new Choice();
    typeC.add("Boolean");
    typeC.add("Numeric");
    typeC.add("Date");
    typeC.add("String");
    typeC.add("Object Reference");
    typeC.add("Password");
    typeC.add("I.P.");
    typeC.addItemListener(this);

    //choose the one that is the default
    changeTypeChoice("Boolean");

    cryptedCF = new checkboxField(null, false, ca, true);
    cryptedCF.setCallback(this);
    addRow(editPanel, cryptedCF, "Stored Crypted:" , 7);
   
    addRow(editPanel, typeC, "Field Type:", 6);

    minLengthN = new numberField(20, ca, true, false, 0, Integer.MAX_VALUE);
    minLengthN.setCallback(this);
    addRow(editPanel, minLengthN, "Minimum String Size:", 8);
    
    maxLengthN = new numberField(20, ca, true, false, 0, Integer.MAX_VALUE);
    maxLengthN.setCallback(this);
    addRow(editPanel, maxLengthN, "Maximum String Size:", 9);
   
    OKCharS = new stringField(20, 100, ca, true, false, null, null);
    OKCharS.setCallback(this);
    addRow(editPanel, OKCharS, "Allowed Chars:", 10);

    BadCharS = new stringField(20, 100, ca, true, false, null, null);
    BadCharS.setCallback(this);
    addRow(editPanel, BadCharS, "Disallowed Chars:", 11);

    namespaceC = new Choice();
    namespaceC.addItemListener(this);

    addRow(editPanel, namespaceC, "Namespace:", 12);
    
    labeledCF = new checkboxField(null, false, ca, true);
    labeledCF.setCallback(this);
    addRow(editPanel, labeledCF, "Labeled:", 13);

    trueLabelS = new stringField(20, 100, ca, true, false, null, null);
    trueLabelS.setCallback(this);
    addRow(editPanel, trueLabelS, "True Label:", 14);

    falseLabelS = new stringField(20, 100, ca, true, false, null, null);
    falseLabelS.setCallback(this);
    addRow(editPanel, falseLabelS, "False Label:", 15);

    editInPlaceCF = new checkboxField(null, false, ca, true);
    editInPlaceCF.setCallback(this);
    addRow(editPanel, editInPlaceCF, "Edit In Place:", 16);

    targetC = new Choice();
    targetC.addItemListener(this);
    addRow(editPanel, targetC, "Target Object:", 17);

    fieldC = new Choice();
    fieldC.addItemListener(this);
    addRow(editPanel, fieldC, "Target Field:", 18);

    booleanShowing = true;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    passwordShowing = false;
    ipShowing = false;

    add(editPanel);
  }

  void clearFields()
  {
    commentT.setText("");

    nameS.setText("");
    classS.setText("");

    trueLabelS.setText("");
    falseLabelS.setText("");
    OKCharS.setText("");
    BadCharS.setText("");

    idN.setText("");
    maxArrayN.setText("");
    minLengthN.setText("");
    maxLengthN.setText("");
  }

  void addRow(Panel parent, Component comp,  String label, int row)
  {
    addRow(parent, comp, label, row, true);
  }

  void addRow(Panel parent, Component comp,  String label, int row, boolean visible)
  {
    Label l = new Label(label);

    rowHash.put(comp, l);
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);

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
  }

  // This goes through all the components, and sets the visibilities

  void checkVisibility()
  {
    System.out.println(" Checking visibility");
    if (passwordShowing || booleanShowing)
      {
	setRowVisible(vectorCF, false);
	setRowVisible(maxArrayN, vectorCF.getState());
      }
    else
      {
	setRowVisible(vectorCF, true);
	setRowVisible(maxArrayN, vectorCF.getState());
      }

    if (passwordShowing)
      {
	setRowVisible(cryptedCF, true);
      }
    else
      {
	setRowVisible(cryptedCF, false);
      }

    setRowVisible(labeledCF, booleanShowing);

    if (booleanShowing)
      {
	setRowVisible(trueLabelS, labeledCF.getState());
	setRowVisible(falseLabelS, labeledCF.getState());
      }
    else
      {
	setRowVisible(trueLabelS, false);
	setRowVisible(falseLabelS, false);
      }

    setRowVisible(OKCharS, stringShowing || passwordShowing);
    setRowVisible(BadCharS, stringShowing || passwordShowing);
    setRowVisible(minLengthN, stringShowing || passwordShowing);
    setRowVisible(maxLengthN, stringShowing || passwordShowing);
    setRowVisible(namespaceC, stringShowing);

    if (referenceShowing)
      {
	setRowVisible(editInPlaceCF, true);
	setRowVisible(targetC, true);

	if (targetC.getSelectedItem().equalsIgnoreCase("<any>"))
	  {
	    setRowVisible(fieldC, false);
	  }
	else
	  {
	    setRowVisible(fieldC, true);
	  }

	try
	  {
	    if (fieldDef.isEditInPlace())
	      {
		setRowVisible(fieldC, false);
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }
    else
      {
	setRowVisible(editInPlaceCF, false);
	setRowVisible(targetC, false);
	setRowVisible(fieldC, false);
      }

    editPanel.doLayout();
    this.validate();
    System.out.println(" Done checking visibility");
  }

  void refreshNamespaceChoice()
   {
     NameSpace[] nameSpaces = null;

     /* -- */
      
     namespaceC.removeAll();

     SchemaEdit test = owner.getSchemaEdit();

     if (test == null)
       {	
	 System.err.println("owner.editor is null");
       }
     
     try
       {
	 nameSpaces = owner.getSchemaEdit().getNameSpaces();
       }
     catch (RemoteException rx)
       {
	 System.err.println("RemoteException getting namespaces: " + rx);
       }
      
     namespaceC.addItem("<none>");      

     if ( (nameSpaces.length == 0) || (nameSpaces == null) )
       {
	 System.err.println("No other namespaces to add");
       }
     else
       {
	 for (int i=0 ; i < nameSpaces.length ; i++)
	   {
	     try
	       {
		 namespaceC.addItem(nameSpaces[i].getName());
	       }
	     catch (RemoteException rx)
	       {
		 System.err.println("RemoteException getting namespace: " + rx);
	       }    
	   }
       }
   }

  /**
   *
   * This method regenerates the list of valid target base choices
   * in the BaseFieldEditor.
   *
   */
    
  void refreshTargetChoice()
  {
    Base[] baseList;

    /* -- */

    targetC.removeAll();

    try
      {
	baseList = owner.getSchemaEdit().getBases(fieldDef.isEditInPlace());
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("Exception getting Bases: " + rx);
      }

    targetC.addItem("<any>");

    for (int i = 0 ; i < baseList.length ; i++)
      {
	try
	  {
	    targetC.addItem(baseList[i].getName());
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Exception getting bases name: " + rx);
	  }
      }
  }

  /**
   *
   * This method regenerates the list of valid target field choices
   * in the BaseFieldEditor when the targetBase is not "<any>".  
   *
   * This method doesn't make a selection, so upon exit of this
   * method, "<none>" will be selected in the fieldC widget.
   *
   */

  void refreshFieldChoice()
  {
    String target;
    short type;
    Base targetBase;
    BaseField bf;
    Vector fields = null;

    /* -- */
    
    target = targetC.getSelectedItem();

    try
      {
	if (target.equals("<all>"))
	  {
	    targetBase = owner.getSchemaEdit().getBase((short)0);
	  }
	else
	  {
	    targetBase = owner.getSchemaEdit().getBase(target);
	  }

	if (targetBase == null)
	  {
	    System.out.println("targetBase is null");
	  }
	else
	  {
	    fields = targetBase.getFields();
	  }
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("Exception getting bases in refreshFieldChoice " + rx);
      }

    fieldC.removeAll();
    fieldC.addItem("<none>");
    
    if (fields == null)
      {
	System.out.println("fields == null");

	// By default, the Choice item will keep the
	// first item added.. the following line is
	// redundant, at least under JDK 1.1.2
	//	fieldC.select("<none>");
      }
    else
      {
	for (int i = 0; i < fields.size(); ++i)
	  {
	    bf = (BaseField)fields.elementAt(i);

	    try
	      {
		type = bf.getType();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Exception getting type description " + rx);
	      }

	    System.out.println("checking type: " + type);

	    if (type == FieldType.INVID)
	      {
		try
		  {
		    System.out.println("adding " + bf.getName());
		    fieldC.add(bf.getName());
		  }
		catch (RemoteException rx)
		  {
		    throw new IllegalArgumentException("Exception getting base field name " + rx);
		  }
	      }
	  }
      }
  }
  
  /**
   *
   * This method changes the type on the server and updates
   * the booleans that BaseFieldEditor uses to keep track
   * of what field attributes should be visible.  We do not
   * do any of the BaseFieldEditor updates that a change
   * to the field type in question would require.  This
   * is currently done elsewhere, primarily by a call to
   * refreshFieldEdit().
   *
   */

  void changeTypeChoice(String selectedItem)
  {
    booleanShowing = false;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    passwordShowing = false;
    ipShowing = false;

    try
      {
	if (selectedItem.equalsIgnoreCase("Boolean"))
	  {
	    booleanShowing = true;
	    fieldDef.setType(FieldType.BOOLEAN);
	  }
	else if (selectedItem.equalsIgnoreCase("Numeric"))
	  {
	    numericShowing = true;
	    fieldDef.setType(FieldType.NUMERIC);
	  }
	else if (selectedItem.equalsIgnoreCase("Date"))
	  {
	    dateShowing = true;
	    fieldDef.setType(FieldType.DATE);
	  }
	else if (selectedItem.equalsIgnoreCase("String"))
	  {
	    stringShowing = true;
	    fieldDef.setType(FieldType.STRING);
	  }
	else if (selectedItem.equalsIgnoreCase("Object Reference"))
	  {
	    referenceShowing = true;
	    fieldDef.setType(FieldType.INVID);
	  }
	else if (selectedItem.equalsIgnoreCase("Password"))
	  {
	    passwordShowing = true;
	    fieldDef.setType(FieldType.PASSWORD);
	  }
	else if (selectedItem.equalsIgnoreCase("I.P."))
	  {
	    ipShowing = true;
	    fieldDef.setType(FieldType.IP);
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("changeTypeChoice: got RemoteException: " + ex);
      }
    catch (NullPointerException ex)
      {
	// we don't have fieldDef set yet.  Just ignore.
      }
  }

  /**
   *
   * Edit the given field.  This method prepares the BaseFieldEditor
   * for display, initializing all items in the BaseFieldEditor panel
   * with the contents of fieldDef.
   *
   */

  void editField(FieldNode fieldNode, boolean forceRefresh)
  {
    System.err.println(" -in FieldEditor.editField()");

    clearFields();

    if (!forceRefresh && (fieldNode == this.fieldNode))
      {
    	return;
      }

    this.fieldNode = fieldNode;

    this.fieldDef = fieldNode.getField();

    // Check to see if this field is editable.
    // Assume it is not, then ask server.
    // Each field will be set to editable depending on this variable.

    boolean isEditable = false;

    try
      {
	isEditable = fieldDef.isEditable();
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("exception: isEditable in editField: " + rx);
      }

    booleanShowing = false;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    passwordShowing = false;
    ipShowing = false;

    System.out.println(" before try");
    try
      {
	System.out.println(" in try");
	idN.setValue(fieldDef.getID());
	nameS.setText(fieldDef.getName());
	classS.setText(fieldDef.getClassName());
	commentT.setText(fieldDef.getComment());

        if (fieldDef.isArray())
	  {
	    vectorCF.setState(true);
	  }
	else
	  {
	    vectorCF.setState(false);
	  }

	if (fieldDef.isString())
	  {
	    minLengthN.setValue(fieldDef.getMinLength());
	    maxLengthN.setValue(fieldDef.getMaxLength());
	    OKCharS.setText(fieldDef.getOKChars());
	    BadCharS.setText(fieldDef.getBadChars());
	    
	    typeC.select("String");
	    stringShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    System.out.println(fieldDef.getNameSpaceLabel());

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.select("<none>");
		System.out.println("selecting <none> for NameSpace");
	      }
	    else
	      {
		namespaceC.select(fieldDef.getNameSpaceLabel());
		System.out.println("selecting " + fieldDef.getNameSpaceLabel());
	      }
	  }
	else if (fieldDef.isPassword())
	  {
	    minLengthN.setValue(fieldDef.getMinLength());
	    maxLengthN.setValue(fieldDef.getMaxLength());
	    OKCharS.setText(fieldDef.getOKChars());
	    BadCharS.setText(fieldDef.getBadChars());
	    
	    typeC.select("Password");
	    passwordShowing = true;

	    cryptedCF.setState(fieldDef.isCrypted());
	  }
	else if (fieldDef.isIP())
	  {
	    typeC.select("I.P.");
	    ipShowing = true;
	  }
	else if (fieldDef.isBoolean())
	  {
	    if (fieldDef.isLabeled())
	      {
		labeledCF.setState(true);
		trueLabelS.setText(fieldDef.getTrueLabel());
		falseLabelS.setText(fieldDef.getFalseLabel());
	      }
	    else
	      {
		labeledCF.setState(false);
		trueLabelS.setText("");
		falseLabelS.setText("");
	      }

	    typeC.select("Boolean");
	    booleanShowing = true;
	  }
	else if (fieldDef.isInvid())
	  {
	    editInPlaceCF.setState(fieldDef.isEditInPlace());

	    refreshTargetChoice();

	    SchemaEdit se = owner.getSchemaEdit();
	    short targetB = fieldDef.getTargetBase();

	    if (targetB == -1)
	      {
		System.out.println("unknown target base");
		targetC.select("<any>");
	      }
	    else
	      {
		Base targetBase = null;
		String string = null;

		if (targetB == -2)
		  {
		    // we're assuming that we've got a known target field in
		    // all objects bases in the system.. this is mainly for
		    // the 'owner list' field.. we'll just pick the field from
		    // the current fieldDef and go with it.
		    
		    System.out.println("new 'alltarget' base");
		    targetC.addItem("<all>");
		    targetC.select("<all>");

		    string = "<all>";

		    targetBase = se.getBase((short) 0);	// assume the field is present in first base
		  }
		else
		  {
		    targetBase = se.getBase(targetB);

		    if (targetBase == null)
		      {
			throw new Error("targetbase is null when it shouldn't be: server error : base id " + targetB);
		      }
		    
		    string = targetBase.getName();
		    
		    System.out.println("Choosing " + string);
		    targetC.select(string);
		  }

		// regenerate the list of choices in fieldC

		refreshFieldChoice();

		// Now that we have an appropriate list of
		// choice items in the fieldC, let's see
		// if we can't find something to select
		// in fieldC

		short targetF = fieldDef.getTargetField();

		if (targetF == -1)
		  {
		    System.out.println("unknown target field");
		    fieldC.select("<none>");
		  }
		else
		  {
		    BaseField targetField;

		    // see if our old field target value is still
		    // appropriate for the currently chosen base

		    try
		      {
			targetField = targetBase.getField(targetF);
			
			if (targetField != null)
			  {
			    string = targetField.getName();
			    System.out.println("selecting " + string);
			    fieldC.select(string);
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("exception getting field " + rx);
		      }
		  }
	      } // else targetB != -1
		
	    typeC.select("Object Reference");
	    referenceShowing = true;
	  }
	else if (fieldDef.isDate())
	  {
	    typeC.select("Date");
	    dateShowing = true;
	  }
	else if (fieldDef.isNumeric())
	  {
	    typeC.select("Numeric");
	    numericShowing = true;
	  }
	else if (fieldDef.isPermMatrix())
	  {
	    typeC.addItem("Permission Matrix");
	    typeC.select("Permission Matrix");
	  }

	//Here is where the editability is checked.

	commentT.setEditable(isEditable);
	nameS.setEditable(isEditable);
	classS.setEditable(isEditable);
	trueLabelS.setEditable(isEditable);
	falseLabelS.setEditable(isEditable);
	OKCharS.setEditable(isEditable);
	BadCharS.setEditable(isEditable);
	idN.setEditable(isEditable);
	maxArrayN.setEditable(isEditable);
	minLengthN.setEditable(isEditable);
	maxLengthN.setEditable(isEditable);

	cryptedCF.setEnabled(isEditable);
	vectorCF.setEnabled(isEditable);
	labeledCF.setEnabled(isEditable);
	editInPlaceCF.setEnabled(isEditable);
	typeC.setEnabled(isEditable);
	namespaceC.setEnabled(isEditable);
	targetC.setEnabled(isEditable);
	fieldC.setEnabled(isEditable);
	
	System.out.println(" calling checkVisibility");
	checkVisibility();
      }
    catch (RemoteException ex)
      {
	System.err.println("remote exception in FieldEditor.editField: " + ex);
      }

    System.out.println(" done in editField");
  }

  /**
   *
   * Reinitialize the BaseFieldEditor with the current field.
   *
   */

  public void refreshFieldEdit()
  {
    this.editField(fieldNode, true);
  }

  /**
   * 
   * For string and numeric fields
   *
   * @see arlut.csd.DataComponent.setValueCallback
   *
   */

  public boolean setValuePerformed(ValueObject v)
  {
    Component comp = v.getSource();

    try
      {
	if (comp == nameS)
	  {
	    System.out.println("nameS");
	    fieldDef.setName((String) v.getValue());
	    fieldNode.setText((String) v.getValue());
	    owner.tree.refresh();
	  }
	else if (comp == classS)
	  {
	    System.out.println("classS");
	    fieldDef.setClassName((String) v.getValue());
	  }
	else if (comp == idN)
	  {
	    System.out.println("idN");
	    fieldDef.setID(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == maxArrayN)
	  {
	    System.out.println("maxArrayN");
	    fieldDef.setMaxArraySize(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == vectorCF)
	  {
	    //setRowVisible(maxArrayN, vectorCF.getState());
	    System.out.println("vectorCF");
	    fieldDef.setArray(vectorCF.getState());
	    checkVisibility();
	  }
	else if (comp == OKCharS)
	  {
	    System.out.println("OkCharS");
	    fieldDef.setOKChars((String) v.getValue());
	  }
	else if (comp == BadCharS)
	  {
	    System.out.println("BadCharS");
	    fieldDef.setBadChars((String) v.getValue());
	  }
	else if (comp == minLengthN)
	  {
	    System.out.println("minLengthN");
	    fieldDef.setMinLength(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == maxLengthN)
	  {
	    System.out.println("maxLengthN");
	    fieldDef.setMaxLength(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == trueLabelS)
	  {
	    System.out.println("trueLabelS");
	    fieldDef.setTrueLabel((String) v.getValue());
	  }
	else if (comp == falseLabelS)
	  {
	    System.out.println("falseLabelS");
	    fieldDef.setFalseLabel((String) v.getValue());
	  }
	else if (comp == labeledCF)
	  {
	    System.out.println("labeledCF");
	    fieldDef.setLabeled(labeledCF.getState());
	    checkVisibility();
	  }
	else if (comp == editInPlaceCF)
	  {
	    System.out.println("editInPlaceCF");
	    fieldDef.setEditInPlace(editInPlaceCF.getState());
	  }
	else if (comp == cryptedCF)
	  {
	    System.out.println("cryptedCF");
	    fieldDef.setCrypted(cryptedCF.getState());
	  }
	return true;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception in setting field value " + ex);
      }
  }

  /**
   *
   * For choice fields
   *
   */

  public void itemStateChanged(ItemEvent e)
  {
    String item = null;
    Base newBase = null;
    String oldBaseName = null;
    short baseID;
    Base oldBase;
    Base currentBase = null;
    String currentLabel = null;
    String currentFieldName = null;

    /* -- */

    if (e.getItemSelectable() == typeC)
      {
	boolean okToChange = true;
	item = typeC.getSelectedItem();

	if (!item.equals("Numeric") && !item.equals("String"))
	  {
	    // Now it can't be a label.. was it a label before?

	    try
	      {
		currentBase = fieldDef.getBase();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception getting base: " + rx);
	      }

	    try
	      {
		currentLabel = currentBase.getLabelFieldName();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception getting label: " + rx);
	      }

	    try
	      {
		currentFieldName = fieldDef.getName();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception getting field name: " + rx);
	      }

	    if ((currentFieldName != null) && (currentLabel != null)  &&
		currentLabel.equals(currentFieldName))
	      {
		changeLabelTypeDialog = new StringDialog(owner, 
							 "Warning: changing object type",
							 "Changing the type of this field will invalidate the label for this base.  Are you sure you want to continue?",
							 "Confirm",
							 "Cancel");
		
		Hashtable answer = changeLabelTypeDialog.DialogShow();

		if (answer != null)  //Ok button was clicked
		  {
		    try
		      {
			System.out.println(" clicked ok");
			currentBase.setLabelField(null); // we're making this field unacceptable as a label
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("exception setting label to null: " + rx);
		      }
		  }
		else
		  {
		    System.out.println(" Canceled, not changing field type");
		    okToChange = false;

		    try 
		      {
			if (fieldDef.isNumeric())
			  {
			    typeC.select("Numeric");
			  }
			else if (fieldDef.isString())
			  {
			    typeC.select("String");
			  }
			else
			  {
			    System.err.println("Field is not String or Numeric, not changing type choice");
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("exception getting old type");
		      }
		  }
	      }
	    else
	      {
		    System.out.println("not the label, ok to change");
		    
	      }
	  }

	if (okToChange)
	  {
	    changeTypeChoice(item);	// switch the visible rows to fit the new type
	    refreshFieldEdit();	// and refresh
	  }
      }
    else if (e.getItemSelectable() == namespaceC)
      {
	item = namespaceC.getSelectedItem();

	System.out.println("Namespace: " + item);
	System.out.println("Setting namespace to " + item);

	try 
	  {
	    if (item.equalsIgnoreCase("<none>"))
	      {
		fieldDef.setNameSpace(null);
	      }
	    else
	      {
		fieldDef.setNameSpace(item);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Remote Exception setting NameSpace: " + rx);
	  }
      }
    else if (e.getItemSelectable() == targetC)
      {
	item = targetC.getSelectedItem();

	try
	  {
	    baseID = fieldDef.getTargetBase();
	    oldBase = owner.getSchemaEdit().getBase(baseID);

	    if (oldBase != null)
	      {
		oldBaseName = oldBase.getName();
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get old base name " + ex);
	  }

	if (item.equalsIgnoreCase("<any>"))
	  {
	    try
	      {
		fieldDef.setTargetBase(null);
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Exception couldn't clear target base: " + rx);
	      }
	  }
	else
	  {
	    try
	      {
		newBase = owner.getSchemaEdit().getBase(item);
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Exception getting base: " + rx);
	      }

	    if (newBase == null)
	      {
		throw new IllegalArgumentException("Could not match selection with a Base");
	      }
	    else
	      {
		try
		  {
		    System.out.println("Setting target base to " + item);

		    fieldDef.setTargetBase(item);

		    // if we've changed our target base, clear out the
		    // target field to avoid accidental confusion if our
		    // new target base has a valid target field with the
		    // same id code as our old target field.
		    
		    if ((oldBaseName != null) && !oldBaseName.equals(item))
		      {
			fieldDef.setTargetField(null);
		      }
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Remote Exception setting Target: " + rx);
		  }
	      }
	  }

	refreshFieldEdit();
	checkVisibility();
      }
    else if (e.getItemSelectable() == fieldC)
      {
	item = fieldC.getSelectedItem();

	System.out.println("Setting field to " + item);

	try
	  {
	    if (item.equals("<none>"))
	      {
		fieldDef.setTargetField(null);
	      }
	    else
	      {
		fieldDef.setTargetField(item);
	      } 
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException ("Exception setting TargetField: " + rx);
	  }
      }
  }

  // for the multiline comment field

  public void textValueChanged(TextEvent e)
  {
    Object obj = e.getSource();

    if (obj == commentT)
      {
	TextComponent text = (TextComponent)obj;

	try
	  {
	    fieldDef.setComment(text.getText());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Remote exception setting comment: " +rx);
	  }
      }
  }
}
