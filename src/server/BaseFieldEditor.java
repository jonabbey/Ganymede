/*

   BaseFieldEditor.java

   Base Field editor component for GASHSchema
   
   Created: 14 August 1997
   Version: $Revision: 1.36 $
   Last Mod Date: $Date: 1999/09/22 23:15:21 $
   Release: $Name:  $

   Module By: Jonathan Abbey and Michael Mulvaney

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
                                                                 BaseFieldEditor

------------------------------------------------------------------------------*/

/**
 * <p>Part of the admin console's graphical schema editor.  This panel is
 * responsible for displaying and editing field definitions.</p>
 */

class BaseFieldEditor extends JPanel implements JsetValueCallback, ItemListener, TextListener {

  static final boolean debug = false;

  // ---

  boolean
    listenToCallbacks = true;

  FieldNode
    fieldNode;

  BaseField 
    fieldDef;

  //  java.awt.CardLayout
  // card;

  JPanel 
    editPanel;

  GASHSchema 
    owner;

  StringDialog
    changeLabelTypeDialog;

  JTextArea
    commentT;			// all

  JstringField
    nameS,			// all
    classS,			// all
    trueLabelS,			// boolean
    falseLabelS,		// boolean
    OKCharS,			// string, password
    BadCharS,			// string, password
    regexpS;			// string

  JnumberField
    idN,			// all
    maxArrayN,			// all
    minLengthN,			// string
    maxLengthN;			// string

  JcheckboxField
    vectorCF,			// all but password, boolean
    labeledCF,			// boolean
    editInPlaceCF,		// invid
    cryptedCF,			// password
    plainTextCF,		// password
    multiLineCF;		// string

  JComboBox
    typeC,			// all
    namespaceC,			// string
    targetC,			// invid
    fieldC;			// invid

  Hashtable
    rowHash;			// to keep track of field labels

  boolean
    booleanShowing,
    numericShowing,
    dateShowing,
    stringShowing,
    referenceShowing,
    passwordShowing,
    ipShowing,
    permissionShowing;

  GridBagLayout
    gbl = new GridBagLayout();
  
  GridBagConstraints
    gbc = new GridBagConstraints();

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
    
    editPanel = new JPanel();
    editPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    editPanel.setLayout(gbl);

    int rowcount = 0;
    
    idN = new JnumberField(20,  false, false, 0, 0);
    idN.setCallback(this);
    addRow(editPanel, idN, "Field ID:", rowcount++);
    
    nameS = new JstringField(20, 100,  true, false, null, null);
    nameS.setCallback(this);
    addRow(editPanel, nameS, "Field Name:", rowcount++);

    classS = new JstringField(20, 100,  true, false, null, null);
    classS.setCallback(this);
    addRow(editPanel, classS, "Class name:", rowcount++);

    commentT = new JTextArea(4, 20);
    JScrollPane commentScroll = new JScrollPane(commentT);
    //commentT.addTextListener(this);
    addRow(editPanel, commentScroll, "Comment:", rowcount++);
    
    // This one is different:
    vectorCF = new JcheckboxField(null, false, true);
    vectorCF.setCallback(this);
    addRow(editPanel, vectorCF, "Vector:", rowcount++);

    maxArrayN = new JnumberField(20,  true, false, 0, Integer.MAX_VALUE);
    maxArrayN.setCallback(this);
    addRow(editPanel, maxArrayN, "Max Array Size:", rowcount++);

    typeC = new JComboBox();
    typeC.addItem("Boolean");
    typeC.addItem("Numeric");
    typeC.addItem("Date");
    typeC.addItem("String");
    typeC.addItem("Object Reference");
    typeC.addItem("Password");
    typeC.addItem("I.P.");
    typeC.addItem("Permission Matrix");
    typeC.addItemListener(this);

    //choose the one that is the default
    changeTypeChoice("Boolean");

    addRow(editPanel, typeC, "Field Type:", rowcount++);

    cryptedCF = new JcheckboxField(null, false, true);
    cryptedCF.setCallback(this);
    addRow(editPanel, cryptedCF, "UNIX/MD5 Crypted:" , rowcount++);

    plainTextCF = new JcheckboxField(null, false, true);
    plainTextCF.setCallback(this);
    addRow(editPanel, plainTextCF, "Store PlainText:" , rowcount++);

    multiLineCF = new JcheckboxField(null, false, true);
    multiLineCF.setCallback(this);
    addRow(editPanel, multiLineCF, "MultiLine Field:" , rowcount++);

    minLengthN = new JnumberField(20,  true, false, 0, Integer.MAX_VALUE);
    minLengthN.setCallback(this);
    addRow(editPanel, minLengthN, "Minimum String Size:", rowcount++);
    
    maxLengthN = new JnumberField(20,  true, false, 0, Integer.MAX_VALUE);
    maxLengthN.setCallback(this);
    addRow(editPanel, maxLengthN, "Maximum String Size:", rowcount++);

    regexpS = new JstringField(20, 100, true, false, null, null);
    regexpS.setCallback(this);
    addRow(editPanel, regexpS, "Regular Expression:", rowcount++);
   
    OKCharS = new JstringField(20, 100,  true, false, null, null);
    OKCharS.setCallback(this);
    addRow(editPanel, OKCharS, "Allowed Chars:", rowcount++);

    BadCharS = new JstringField(20, 100,  true, false, null, null);
    BadCharS.setCallback(this);
    addRow(editPanel, BadCharS, "Disallowed Chars:", rowcount++);

    namespaceC = new JComboBox();
    namespaceC.addItemListener(this);

    addRow(editPanel, namespaceC, "Namespace:", rowcount++);
    
    labeledCF = new JcheckboxField(null, false, true);
    labeledCF.setCallback(this);
    addRow(editPanel, labeledCF, "Labeled:", rowcount++);

    trueLabelS = new JstringField(20, 100,  true, false, null, null);
    trueLabelS.setCallback(this);
    addRow(editPanel, trueLabelS, "True Label:", rowcount++);

    falseLabelS = new JstringField(20, 100,  true, false, null, null);
    falseLabelS.setCallback(this);
    addRow(editPanel, falseLabelS, "False Label:", rowcount++);

    editInPlaceCF = new JcheckboxField(null, false, true);
    editInPlaceCF.setCallback(this);
    addRow(editPanel, editInPlaceCF, "Edit In Place:", rowcount++);

    targetC = new JComboBox();
    targetC.addItemListener(this);
    addRow(editPanel, targetC, "Target Object:", rowcount++);

    fieldC = new JComboBox();
    fieldC.addItemListener(this);
    addRow(editPanel, fieldC, "Target Field:", rowcount++);

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
    regexpS.setText("");
    OKCharS.setText("");
    BadCharS.setText("");

    idN.setText("");
    maxArrayN.setText("");
    minLengthN.setText("");
    maxLengthN.setText("");
  }

  void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    addRow(parent, comp, label, row, true);
  }

  synchronized void addRow(JPanel parent, java.awt.Component comp,  String label, int row, boolean visible)
  {
    JLabel l = new JLabel(label);

    rowHash.put(comp, l);
    
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

    setRowVisible(comp, visible);
  }

  void setRowVisible(java.awt.Component comp, boolean b)
  {
    java.awt.Component c = (java.awt.Component) rowHash.get(comp);

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
    if (debug)
      {
	System.out.println(" Checking visibility");
      }

    if (passwordShowing || booleanShowing || numericShowing || dateShowing)
      {
	setRowVisible(vectorCF, false);
	setRowVisible(maxArrayN, false);
      }
    else
      {
	setRowVisible(vectorCF, true);
	setRowVisible(maxArrayN, vectorCF.isSelected());
      }

    if (passwordShowing)
      {
	setRowVisible(cryptedCF, true);
	setRowVisible(plainTextCF, true);
      }
    else
      {
	setRowVisible(cryptedCF, false);
	setRowVisible(plainTextCF, false);
      }

    setRowVisible(labeledCF, booleanShowing);

    if (booleanShowing)
      {
	setRowVisible(trueLabelS, labeledCF.isSelected());
	setRowVisible(falseLabelS, labeledCF.isSelected());
      }
    else
      {
	setRowVisible(trueLabelS, false);
	setRowVisible(falseLabelS, false);
      }

    setRowVisible(multiLineCF, stringShowing  && !vectorCF.isSelected());
    setRowVisible(regexpS, stringShowing);
    setRowVisible(OKCharS, stringShowing || passwordShowing);
    setRowVisible(BadCharS, stringShowing || passwordShowing);
    setRowVisible(minLengthN, stringShowing || passwordShowing);
    setRowVisible(maxLengthN, stringShowing || passwordShowing);
    setRowVisible(namespaceC, stringShowing || numericShowing || ipShowing);

    if (referenceShowing)
      {
	setRowVisible(editInPlaceCF, true);
	setRowVisible(targetC, true);

	if (((String)targetC.getModel().getSelectedItem()).equalsIgnoreCase("<any>"))
	  {
	    setRowVisible(fieldC, false);
	  }
	else
	  {
	    setRowVisible(fieldC, true);
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

    if (debug)
      {
	System.out.println(" Done checking visibility");
      }
  }

  void refreshNamespaceChoice()
   {
     NameSpace[] nameSpaces = null;

     /* -- */

     try
       {
	 namespaceC.removeAllItems();
       }
     catch (IndexOutOfBoundsException ex)
       {
	 // Swing 1.1 beta 2 will do this to us, just
	 // ignore it for now.

	 System.err.println("refreshNamespaceChoice(): Swing Bug Bites Again");
       }

     SchemaEdit test = owner.getSchemaEdit();

     if (test == null)
       {
	 if (debug)
	   {
	     System.err.println("owner.editor is null");
	   }
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
	 if (debug)
	   {
	     System.err.println("No other namespaces to add");
	   }
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

    try
      {
	targetC.removeAllItems();
      }     
    catch (IndexOutOfBoundsException ex)
      {
	// Swing 1.1 beta 2 will do this to us, just
	// ignore it.

	System.err.println("refreshTargetChoice(): Swing Bug Bites Again");
      }

    try
      {

	// if this field is edit in place, we only want to list embeddable
	// object types
	
	if (fieldDef.isEditInPlace())
	  {
	    baseList = owner.getSchemaEdit().getBases(fieldDef.isEditInPlace());
	  }
	else
	  {
	    baseList = owner.getSchemaEdit().getBases(); // list all object types
	  }
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
    
    target = (String)targetC.getModel().getSelectedItem();

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
	    if (debug)
	      {
		System.out.println("targetBase is null");
	      }
	  }
	else
	  {
	    fields = targetBase.getFields(true);
	  }
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("Exception getting bases in refreshFieldChoice " + rx);
      }

    try
      {
	fieldC.removeAllItems();
      }
    catch (IndexOutOfBoundsException ex)
      {
	// Swing 1.1 beta 2 will do this to us, just
	// ignore it.
	System.err.println("refreshFieldChoice(): Swing Bug Bites Again");
      }

    fieldC.addItem("<none>");
    
    if (fields == null)
      {
	if (debug)
	  {
	    System.out.println("fields == null");
	  }

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

	    if (debug)
	      {
		System.out.println("checking type: " + type);
	      }

	    try
	      {
		if (fieldDef.isEditInPlace())
		  {
		    // in an edit in place field, we can only
		    // be linked to a target object's container link field
		
		    if (bf.getID() == SchemaConstants.ContainerField)
		      {
			fieldC.addItem(bf.getName());
		      }
		  }
		else
		  {
		    if (type == FieldType.INVID)
		      {
			try
			  {
			    if (debug)
			      {
				System.out.println("adding " + bf.getName());
			      }

			    fieldC.addItem(bf.getName());
			  }
			catch (RemoteException rx)
			  {
			    throw new IllegalArgumentException("Exception getting base field name " + rx);
			  }
		      }
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new IllegalArgumentException("Exception getting base field edit in place status " + ex);
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
    permissionShowing = false;

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
	else if (selectedItem.equalsIgnoreCase("Permission Matrix"))
	  {
	    permissionShowing = true;
	    fieldDef.setType(FieldType.PERMISSIONMATRIX);
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

  void editField(FieldNode fieldNode, boolean forceRefresh)
  {
    editField(fieldNode, forceRefresh, true);
  }

  /**
   *
   * Edit the given field.  This method prepares the BaseFieldEditor
   * for display, initializing all items in the BaseFieldEditor panel
   * with the contents of fieldDef.
   *
   */

  void editField(FieldNode fieldNode, boolean forceRefresh, boolean updateTargetC)
  {
    if (debug)
      {
	System.err.println(" -in FieldEditor.editField()");
      }

    listenToCallbacks = false;
    owner.setWaitCursor();

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

    // if we are in testing and development mode, we want to be able
    // to edit fields regardless of what the server reports for its
    // preference

    if (owner.developMode)
      {
	isEditable = true;
      }

    booleanShowing = false;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    passwordShowing = false;
    ipShowing = false;

    if (debug)
      {
	System.out.println(" before try");
      }

    try
      {
	if (debug)
	  {
	    System.out.println(" in try");
	  }

	idN.setValue(fieldDef.getID());
	nameS.setText(fieldDef.getName());
	classS.setText(fieldDef.getClassName());
	commentT.setText(fieldDef.getComment());

        if (fieldDef.isArray())
	  {
	    vectorCF.setSelected(true, false);
	    maxArrayN.setValue(fieldDef.getMaxArraySize());
	  }
	else
	  {
	    vectorCF.setSelected(false, false);
	  }

	if (fieldDef.isString())
	  {
	    multiLineCF.setSelected(fieldDef.isMultiLine(), false);
	    minLengthN.setValue(fieldDef.getMinLength());
	    maxLengthN.setValue(fieldDef.getMaxLength());
	    regexpS.setText(fieldDef.getRegexpPat());
	    OKCharS.setText(fieldDef.getOKChars());
	    BadCharS.setText(fieldDef.getBadChars());
	    
	    typeC.getModel().setSelectedItem("String");
	    stringShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    if (debug)
	      {
		System.out.println(fieldDef.getNameSpaceLabel());
	      }

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.getModel().setSelectedItem("<none>");

		if (debug)
		  {
		    System.out.println("selecting <none> for NameSpace");
		  }
	      }
	    else
	      {
		namespaceC.getModel().setSelectedItem(fieldDef.getNameSpaceLabel());

		if (debug)
		  {
		    System.out.println("selecting " + fieldDef.getNameSpaceLabel());
		  }
	      }
	  }
	else if (fieldDef.isPassword())
	  {
	    minLengthN.setValue(fieldDef.getMinLength());
	    maxLengthN.setValue(fieldDef.getMaxLength());
	    OKCharS.setText(fieldDef.getOKChars());
	    BadCharS.setText(fieldDef.getBadChars());
	    
	    typeC.getModel().setSelectedItem("Password");
	    passwordShowing = true;

	    cryptedCF.setValue(fieldDef.isCrypted());
	    plainTextCF.setValue(fieldDef.isPlainText());

	    // if a password is not crypted, it *must* keep
	    // passwords in plaintext


	  }
	else if (fieldDef.isIP())
	  {
	    typeC.getModel().setSelectedItem("I.P.");
	    ipShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    if (debug)
	      {
		System.out.println(fieldDef.getNameSpaceLabel());
	      }

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.getModel().setSelectedItem("<none>");

		if (debug)
		  {
		    System.out.println("selecting <none> for NameSpace");
		  }
	      }
	    else
	      {
		namespaceC.getModel().setSelectedItem(fieldDef.getNameSpaceLabel());

		if (debug)
		  {
		    System.out.println("selecting " + fieldDef.getNameSpaceLabel());
		  }
	      }
	  }
	else if (fieldDef.isBoolean())
	  {
	    if (fieldDef.isLabeled())
	      {
		labeledCF.setValue(true);
		trueLabelS.setText(fieldDef.getTrueLabel());
		falseLabelS.setText(fieldDef.getFalseLabel());
	      }
	    else
	      {
		labeledCF.setValue(false);
		trueLabelS.setText("");
		falseLabelS.setText("");
	      }

	    typeC.getModel().setSelectedItem("Boolean");
	    booleanShowing = true;
	  }
	else if (fieldDef.isInvid())
	  {
	    editInPlaceCF.setValue(fieldDef.isEditInPlace());

	    // all edit in place references are vectors

	    if (fieldDef.isEditInPlace())
	      {
		vectorCF.setSelected(true, false);
		fieldDef.setArray(true);
	      }

	    // important.. we want to avoid mucking with the targetC GUI combobox if
	    // our refresh is being initiated by actions on the targetC GUI.  Swing
	    // 1.0.2 gets real, real cranky if we try that.

	    if (updateTargetC)
	      {
		refreshTargetChoice();
	      }

	    SchemaEdit se = owner.getSchemaEdit();
	    short targetB = fieldDef.getTargetBase();
		
	    if (targetB == -1)
	      {
		if (debug)
		  {
		    System.out.println("unknown target base");
		  }
		
		if (updateTargetC)
		  {
		    targetC.getModel().setSelectedItem("<any>");
		  }
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
			
		    if (debug)
		      {
			System.out.println("new 'alltarget' base");
		      }
		    
		    if (updateTargetC)
		      {
			targetC.addItem("<all>");
			targetC.getModel().setSelectedItem("<all>");
		      }
			
		    string = "<all>";
			
		    targetBase = se.getBase((short) 0);	// assume the field is present in first base
		  }
		else
		  {
		    targetBase = se.getBase(targetB);
			
		    if (targetBase == null)
		      {
			if (debug)
			  {
			    System.err.println("targetbase is null when it shouldn't be: server error : base id " + 
					       targetB);
			    
			    System.out.println("Choosing <any>");
			  }
			    
			// we want to clear this bad reference
			    
			try
			  {
			    fieldDef.setTargetBase(null);
			  }
			catch (RemoteException rx)
			  {
			    throw new IllegalArgumentException("Exception couldn't clear target base: " + rx);
			  }
			
			if (updateTargetC)
			  {
			    targetC.getModel().setSelectedItem("<any>");
			  }
		      }
		    else
		      {
			string = targetBase.getName();
			    
			if (debug)
			  {
			    System.out.println("Choosing " + string);
			  }

			if (updateTargetC)
			  {
			    targetC.getModel().setSelectedItem(string);
			  }
		      }
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
		    if (debug)
		      {
			System.out.println("unknown target field");
		      }

		    fieldC.getModel().setSelectedItem("<none>");
		  }
		else
		  {
		    BaseField targetField;
		    
		    // see if our old field target value is still
		    // appropriate for the currently chosen base
		    
		    if (targetBase != null)
		      {
			try
			  {
			    targetField = targetBase.getField(targetF);
			    
			    if (targetField != null)
			      {
				string = targetField.getName();

				if (debug)
				  {
				    System.out.println("selecting " + string);
				  }

				fieldC.getModel().setSelectedItem(string);
			      }
			  }
			catch (RemoteException rx)
			  {
			    throw new IllegalArgumentException("exception getting field " + rx);
			  }
		      }
		    else
		      {
			if (debug)
			  {
			    System.err.println("targetbase is null, clearing targetField.");
			
			    System.out.println("Choosing <none>");
			  }
			
			// we want to clear this bad reference
			
			try
			  {
			    fieldDef.setTargetField(null);
			  }
			catch (RemoteException rx)
			  {
			    throw new IllegalArgumentException("Exception couldn't clear target base: " + rx);
			  }
			
			fieldC.getModel().setSelectedItem("<none>");
		      }
		  }
	      } // else targetB != -1
	    
	    typeC.getModel().setSelectedItem("Object Reference");
	    referenceShowing = true;
	  }
	else if (fieldDef.isDate())
	  {
	    typeC.getModel().setSelectedItem("Date");
	    dateShowing = true;
	  }
	else if (fieldDef.isNumeric())
	  {
	    typeC.getModel().setSelectedItem("Numeric");
	    numericShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    if (debug)
	      {
		System.out.println(fieldDef.getNameSpaceLabel());
	      }

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.getModel().setSelectedItem("<none>");

		if (debug)
		  {
		    System.out.println("selecting <none> for NameSpace");
		  }
	      }
	    else
	      {
		namespaceC.getModel().setSelectedItem(fieldDef.getNameSpaceLabel());
		
		if (debug)
		  {
		    System.out.println("selecting " + fieldDef.getNameSpaceLabel());
		  }
	      }
	  }
	else if (fieldDef.isPermMatrix())
	  {
	    typeC.addItem("Permission Matrix");
	    typeC.getModel().setSelectedItem("Permission Matrix");
	  }

	// Here is where the editability is checked.

	if (debug)
	  {
	    System.out.println("+Setting enabled to: " + isEditable);
	  }

	commentT.setEditable(isEditable);
	nameS.setEditable(isEditable);
	classS.setEditable(isEditable);
	trueLabelS.setEditable(isEditable);
	falseLabelS.setEditable(isEditable);
	regexpS.setEditable(isEditable);
	OKCharS.setEditable(isEditable);
	BadCharS.setEditable(isEditable);
	idN.setEditable(isEditable);
	maxArrayN.setEditable(isEditable);
	minLengthN.setEditable(isEditable);
	maxLengthN.setEditable(isEditable);

	multiLineCF.setEnabled(isEditable);
	cryptedCF.setEnabled(isEditable);

	if (passwordShowing)
	  {
	    if (!cryptedCF.isSelected() && plainTextCF.isSelected())
	      {
		plainTextCF.setEnabled(false);
	      }
	  }
	else
	  {
	    plainTextCF.setEnabled(isEditable);
	  }

	vectorCF.setEnabled(isEditable);
	labeledCF.setEnabled(isEditable);
	editInPlaceCF.setEnabled(isEditable);

	typeC.setEnabled(isEditable);
	namespaceC.setEnabled(isEditable);
	targetC.setEnabled(isEditable);
	fieldC.setEnabled(isEditable);

	if (debug)
	  {
	    System.out.println(" calling checkVisibility");
	  }

	checkVisibility();
      }
    catch (RemoteException ex)
      {
	System.err.println("remote exception in FieldEditor.editField: " + ex);
      }

    /*
    typeC.addItemListener(this);
    fieldC.addItemListener(this);
    targetC.addItemListener(this);
    namespaceC.addItemListener(this);
    */

    owner.setNormalCursor();
    listenToCallbacks = true;

    if (debug)
      {
	System.out.println(" done in editField");
      }
  }

  /**
   * <p>Reinitialize the BaseFieldEditor with the current field.</p>
   */

  public void refreshFieldEdit(boolean updateTargetC)
  {
    this.editField(fieldNode, true, updateTargetC);
  }

  /**
   * <p>For string, numeric, and checkbox fields</p>
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   */

  public boolean setValuePerformed(JValueObject v)
  {
    if (!listenToCallbacks)
      {
	if (debug)
	  {
	    System.out.println("I'm not listening!");
	  }

	return true;  //return true because we want to component to change, just don't act on callback
      }

    java.awt.Component comp = v.getSource();

    try
      {
	if (comp == nameS)
	  {
	    if (debug)
	      {
		System.out.println("nameS");
	      }

	    fieldDef.setName((String) v.getValue());
	    fieldNode.setText((String) v.getValue());
	    owner.tree.refresh();
	  }
	else if (comp == classS)
	  {
	    if (debug)
	      {
		System.out.println("classS");
	      }

	    fieldDef.setClassName((String) v.getValue());
	  }
	else if (comp == idN)
	  {
	    if (debug)
	      {
		System.out.println("idN");
	      }

	    fieldDef.setID(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == maxArrayN)
	  {
	    if (debug)
	      {
		System.out.println("maxArrayN");
	      }

	    fieldDef.setMaxArraySize(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == vectorCF)
	  {
	    //setRowVisible(maxArrayN, vectorCF.getValue());

	    if (debug)
	      {
		System.out.println("vectorCF");
	      }

	    fieldDef.setArray(vectorCF.isSelected());
	    checkVisibility();
	  }
	else if (comp == regexpS)
	  {
	    if (debug)
	      {
		System.out.println("regexpS");
	      }

	    // setting a regexp can fail if it can't be properly
	    // parsed

	    if (!fieldDef.setRegexpPat((String) v.getValue()))
	      {
		regexpS.setText(fieldDef.getRegexpPat());
	      }
	  }
	else if (comp == OKCharS)
	  {
	    if (debug)
	      {
		System.out.println("OkCharS");
	      }

	    fieldDef.setOKChars((String) v.getValue());
	  }
	else if (comp == BadCharS)
	  {
	    if (debug)
	      {
		System.out.println("BadCharS");
	      }

	    fieldDef.setBadChars((String) v.getValue());
	  }
	else if (comp == minLengthN)
	  {
	    if (debug)
	      {
		System.out.println("minLengthN");
	      }

	    fieldDef.setMinLength(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == maxLengthN)
	  {
	    if (debug)
	      {
		System.out.println("maxLengthN");
	      }

	    fieldDef.setMaxLength(((Integer)v.getValue()).shortValue());
	  }
	else if (comp == trueLabelS)
	  {
	    if (debug)
	      {
		System.out.println("trueLabelS");
	      }

	    fieldDef.setTrueLabel((String) v.getValue());
	  }
	else if (comp == falseLabelS)
	  {
	    if (debug)
	      {
		System.out.println("falseLabelS");
	      }

	    fieldDef.setFalseLabel((String) v.getValue());
	  }
	else if (comp == labeledCF)
	  {
	    if (debug)
	      {
		System.out.println("labeledCF");
	      }

	    fieldDef.setLabeled(labeledCF.isSelected());
	    checkVisibility();
	  }
	else if (comp == editInPlaceCF)
	  {
	    if (debug)
	      {
		System.out.println("editInPlaceCF");
	      }

	    fieldDef.setEditInPlace(editInPlaceCF.isSelected());
	    editField(fieldNode, true);	// force full recalc and refresh
	  }
	else if (comp == cryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("cryptedCF");
	      }

	    fieldDef.setCrypted(cryptedCF.isSelected());

	    // a password field has to have plaintext stored if it
	    // is not to store the password in crypted form.

	    if (!cryptedCF.isSelected() && !plainTextCF.isSelected())
	      {
		plainTextCF.setValue(true);
	      }

	    if (!cryptedCF.isSelected())
	      {
		plainTextCF.setEnabled(false);
	      }
	    else
	      {
		plainTextCF.setEnabled(fieldDef.isEditable() || owner.developMode);
	      }
	  }
	else if (comp == plainTextCF)
	  {
	    if (debug)
	      {
		System.out.println("plainTextCF");
	      }

	    fieldDef.setPlainText(plainTextCF.isSelected());
	  }
	else if (comp == multiLineCF)
	  {
	    if (debug)
	      {
		System.out.println("multiLineCF: " + multiLineCF.isSelected());
	      }

	    fieldDef.setMultiLine(multiLineCF.isSelected());
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
    if (!listenToCallbacks)
      {
	if (debug)
	  {
	    System.out.println("I'm not listening to callbacks right now.");
	  }
	return;
      }
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
	item = (String)typeC.getModel().getSelectedItem();

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
			if (debug)
			  {
			    System.out.println(" clicked ok");
			  }
			currentBase.setLabelField(null); // we're making this field unacceptable as a label
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("exception setting label to null: " + rx);
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.out.println(" Canceled, not changing field type");
		      }
		    okToChange = false;

		    try 
		      {
			if (fieldDef.isNumeric())
			  {
			    typeC.getModel().setSelectedItem("Numeric");
			  }
			else if (fieldDef.isString())
			  {
			    typeC.getModel().setSelectedItem("String");
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
	    refreshFieldEdit(true);	// and refresh
	  }
      }
    else if (e.getItemSelectable() == namespaceC)
      {
	item = (String)namespaceC.getModel().getSelectedItem();

	if (debug)
	  {
	    System.out.println("Namespace: " + item);
	    System.out.println("Setting namespace to " + item);
	  }

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
	item = (String)targetC.getModel().getSelectedItem();

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
		    if (debug)
		      {
			System.out.println("Setting target base to " + item);
		      }

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

	refreshFieldEdit(false);
	checkVisibility();
      }
    else if (e.getItemSelectable() == fieldC)
      {
	item = (String)fieldC.getSelectedItem();

	if (debug)
	  {
	    System.out.println("Setting field to " + item);
	  }

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
    if (!listenToCallbacks)
      {
	return;
      }
    Object obj = e.getSource();

    if (obj == commentT)
      {
	java.awt.TextComponent text = (java.awt.TextComponent)obj;

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
