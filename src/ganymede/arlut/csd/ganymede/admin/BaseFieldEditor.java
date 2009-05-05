/*

   BaseFieldEditor.java

   Base Field editor component for GASHSchema
   
   Created: 14 August 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey and Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.admin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JcheckboxField;
import arlut.csd.JDataComponent.JnumberField;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JstringArea;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.JLabelPanel;
import arlut.csd.JDataComponent.JStretchPanel;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.NameSpace;
import arlut.csd.ganymede.rmi.SchemaEdit;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 BaseFieldEditor

------------------------------------------------------------------------------*/

/**
 * Part of the admin console's graphical schema editor.  This panel is
 * responsible for displaying and editing field definitions.
 */

class BaseFieldEditor extends JStretchPanel implements JsetValueCallback, ItemListener {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede admin console.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.BaseFieldEditor");

  // localized strings describing the types of fields we support, to
  // appear in the field type drop-down.

  static final String booleanToken = ts.l("global.boolean"); // "Boolean"
  static final String numericToken = ts.l("global.numeric"); // "Numeric"
  static final String fieldOptionToken = ts.l("global.fieldOption"); // "Field Options"
  static final String floatToken = ts.l("global.float"); // "Float"
  static final String dateToken = ts.l("global.date");	 // "Date"
  static final String stringToken = ts.l("global.string"); // "String"
  static final String objectRefToken = ts.l("global.objectRef"); // "Object Reference"
  static final String passwordToken = ts.l("global.password");	 // "Password"
  static final String ipToken = ts.l("global.ip");		 // "I.P."
  static final String permissionToken = ts.l("global.permission"); // "Permission Matrix"

  static final String allToken = ts.l("global.all"); // "<all>"
  static final String anyToken = ts.l("global.any"); // "<any>"

  static final String noneToken = ts.l("global.none"); // "<none>"

  // ---

  boolean
    listenToCallbacks = true;

  FieldNode
    fieldNode;

  BaseField 
    fieldDef;			// remote reference

  //  java.awt.CardLayout
  // card;

  JLabelPanel 
    editPanel;

  GASHSchema 
    owner;

  StringDialog
    changeLabelTypeDialog;

  JstringArea
    commentT;			// all

  JstringField
    nameS,			// all
    trueLabelS,			// boolean
    falseLabelS,		// boolean
    OKCharS,			// string, password
    BadCharS,			// string, password
    regexpS,			// string
    regexpDescS;		// string

  JnumberField
    idN,			// all
    maxArrayN,			// all
    minLengthN,			// string
    maxLengthN,			// string
    shaUnixCryptRoundsN;	// password

  JcheckboxField
    vectorCF,			// invid, string, ip
    labeledCF,			// boolean
    editInPlaceCF,		// invid
    cryptedCF,			// password
    md5cryptedCF,		// password
    apachemd5cryptedCF,		// password
    winHashcryptedCF,		// password
    sshaHashcryptedCF,		// password
    shaUnixCryptedCF,		// password
    plainTextCF,		// password
    multiLineCF;		// string

  JComboBox
    typeC,			// all
    shaUnixCryptTypeC,		// password
    namespaceC,			// string, numeric, ip
    targetC,			// invid
    fieldC;			// invid

  Hashtable
    rowHash;			// to keep track of field labels

  boolean
    booleanShowing,
    numericShowing,
    floatShowing,
    fieldOptionShowing,    
    dateShowing,
    stringShowing,
    referenceShowing,
    passwordShowing,
    ipShowing,
    permissionShowing;

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

    editPanel = setupEditPanel();
    setComponent(editPanel);
  }
   
  private JLabelPanel setupEditPanel()
  {
    idN = new JnumberField(20,  false, false, 0, 0);

    // only allow characters that can be used as an XML entity name.
    // We allow the space char (which is not allowed as an XML entity
    // name), but disallow the underscore char, which we use in place
    // of the space when we write out the base name as an XML entity.

    nameS = new JstringField(20, 100,  true, false,
			     "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .-", 
			     null);
    nameS.setCallback(this);

    commentT = new JstringArea(4, 20);
    commentT.setCallback(this);

    // This one is different:
    vectorCF = new JcheckboxField(null, false, true);
    vectorCF.setCallback(this);

    maxArrayN = new JnumberField(20,  true, false, 0, Integer.MAX_VALUE);
    maxArrayN.setCallback(this);

    typeC = new JComboBox();

    typeC.addItem(booleanToken);
    typeC.addItem(numericToken);
    typeC.addItem(fieldOptionToken);
    typeC.addItem(floatToken);
    typeC.addItem(dateToken);
    typeC.addItem(stringToken);
    typeC.addItem(objectRefToken);
    typeC.addItem(passwordToken);
    typeC.addItem(ipToken);
    typeC.addItem(permissionToken);
    typeC.addItemListener(this);

    cryptedCF = new JcheckboxField(null, false, true);
    cryptedCF.setCallback(this);

    md5cryptedCF = new JcheckboxField(null, false, true);
    md5cryptedCF.setCallback(this);

    apachemd5cryptedCF = new JcheckboxField(null, false, true);
    apachemd5cryptedCF.setCallback(this);

    winHashcryptedCF = new JcheckboxField(null, false, true);
    winHashcryptedCF.setCallback(this);

    sshaHashcryptedCF = new JcheckboxField(null, false, true);
    sshaHashcryptedCF.setCallback(this);

    shaUnixCryptedCF = new JcheckboxField(null, false, true);
    shaUnixCryptedCF.setCallback(this);

    shaUnixCryptTypeC = new JComboBox();
    shaUnixCryptTypeC.addItem("SHA256");
    shaUnixCryptTypeC.addItem("SHA512");
    shaUnixCryptTypeC.addItemListener(this);

    shaUnixCryptRoundsN = new JnumberField(20, true, false, 1000, 999999999);
    shaUnixCryptRoundsN.setCallback(this);

    plainTextCF = new JcheckboxField(null, false, true);
    plainTextCF.setCallback(this);

    multiLineCF = new JcheckboxField(null, false, true);
    multiLineCF.setCallback(this);

    minLengthN = new JnumberField(20,  true, false, 0, Integer.MAX_VALUE);
    minLengthN.setCallback(this);

    maxLengthN = new JnumberField(20,  true, false, 0, Integer.MAX_VALUE);
    maxLengthN.setCallback(this);

    regexpS = new JstringField(20, 100, true, false, null, null);
    regexpS.setCallback(this);

    regexpDescS = new JstringField(20, 400, true, false, null, null);
    regexpDescS.setCallback(this);

    OKCharS = new JstringField(20, 100,  true, false, null, null);
    OKCharS.setCallback(this);

    BadCharS = new JstringField(20, 100,  true, false, null, null);
    BadCharS.setCallback(this);

    namespaceC = new JComboBox();
    namespaceC.addItemListener(this);

    labeledCF = new JcheckboxField(null, false, true);
    labeledCF.setCallback(this);

    trueLabelS = new JstringField(20, 100,  true, false, null, null);
    trueLabelS.setCallback(this);

    falseLabelS = new JstringField(20, 100,  true, false, null, null);
    falseLabelS.setCallback(this);

    editInPlaceCF = new JcheckboxField(null, false, true);
    editInPlaceCF.setCallback(this);

    targetC = new JComboBox();
    targetC.addItemListener(this);

    fieldC = new JComboBox();
    fieldC.addItemListener(this);

    // ------------------------------------------------------------

    editPanel = new JLabelPanel();
    editPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    editPanel.setFixedSizeLabelCells(true);
    editPanel.addFillRow(ts.l("setupEditPanel.fieldIdRow"), idN); // "Field ID:"
    editPanel.addFillRow(ts.l("setupEditPanel.fieldNameRow"), nameS); // "Field Name:"
    editPanel.addFillRow(ts.l("setupEditPanel.commentRow"), commentT); // "Comment:"
    editPanel.addFillRow(ts.l("setupEditPanel.vectorRow"), vectorCF);	// "Vector:"
    editPanel.addFillRow(ts.l("setupEditPanel.arraySizeRow"), maxArrayN); // "Max Array Size:"
    editPanel.addFillRow(ts.l("setupEditPanel.fieldTypeRow"), typeC); // "Field Type:"
    editPanel.addFillRow(ts.l("setupEditPanel.unixCryptRow"), cryptedCF); // "UNIX Crypted:"
    editPanel.addFillRow(ts.l("setupEditPanel.freeBSDMD5CryptRow"), md5cryptedCF); // "FreeBSD-style MD5 Crypted:"
    editPanel.addFillRow(ts.l("setupEditPanel.apacheMD5CryptRow"), apachemd5cryptedCF); // "Apache-style MD5 Crypted:"
    editPanel.addFillRow(ts.l("setupEditPanel.ntCryptRow"), winHashcryptedCF); // "Windows/Samba Crypted:"
    editPanel.addFillRow(ts.l("setupEditPanel.sshaCryptRow"), sshaHashcryptedCF); // "SSHA Crypted:"
    editPanel.addFillRow(ts.l("setupEditPanel.shaUnixCryptRow"), shaUnixCryptedCF); // "SHA Unix Crypted:"
    editPanel.addFillRow(ts.l("setupEditPanel.shaUnixCryptVariantRow"), shaUnixCryptTypeC); // "SHA Unix Crypt Variant:"
    editPanel.addFillRow(ts.l("setupEditPanel.shaUnixCryptRoundsRow"), shaUnixCryptRoundsN); // "SHA Unix Crypt Rounds:"
    editPanel.addFillRow(ts.l("setupEditPanel.plaintextRow"), plainTextCF); // "Store PlainText:"
    editPanel.addFillRow(ts.l("setupEditPanel.multilineRow"), multiLineCF); // "Multiline Field:"
    editPanel.addFillRow(ts.l("setupEditPanel.minStringSizeRow"), minLengthN); // "Minimum String Size:"
    editPanel.addFillRow(ts.l("setupEditPanel.maxStringSizeRow"), maxLengthN); // "Maximum String Size:"
    editPanel.addFillRow(ts.l("setupEditPanel.regexpRow"), regexpS); // "Regular Expression Pattern:"
    editPanel.addFillRow(ts.l("setupEditPanel.regexpDescRow"), regexpDescS); // "RegExp Description:"
    editPanel.addFillRow(ts.l("setupEditPanel.allowedCharsRow"), OKCharS); // "Allowed Chars:"
    editPanel.addFillRow(ts.l("setupEditPanel.disallowedCharsRow"), BadCharS); // "Disallowed Chars:"
    editPanel.addFillRow(ts.l("setupEditPanel.namespaceRow"), namespaceC); // "Namespace:"
    editPanel.addFillRow(ts.l("setupEditPanel.labeledBoolRow"), labeledCF); // "Labeled:"
    editPanel.addFillRow(ts.l("setupEditPanel.trueLabelRow"), trueLabelS); // "True Label:"
    editPanel.addFillRow(ts.l("setupEditPanel.falseLabelRow"), falseLabelS); // "False Label:"
    editPanel.addFillRow(ts.l("setupEditPanel.editInPlaceRow"), editInPlaceCF); // "Edit In Place:"
    editPanel.addFillRow(ts.l("setupEditPanel.targetObjRow"), targetC); // "Target Object:"
    editPanel.addFillRow(ts.l("setupEditPanel.targetFieldRow"), fieldC); // "Target Field:"

    booleanShowing = true;
    numericShowing = false;
    floatShowing = false;
    fieldOptionShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    passwordShowing = false;
    ipShowing = false;

    return editPanel;
  }

  void clearFields()
  {
    commentT.setText("");

    nameS.setText("");

    trueLabelS.setText("");
    falseLabelS.setText("");
    regexpS.setText("");
    regexpDescS.setText("");
    OKCharS.setText("");
    BadCharS.setText("");

    idN.setText("");
    maxArrayN.setText("");
    minLengthN.setText("");
    maxLengthN.setText("");
  }

  // This goes through all the components, and sets the visibilities

  void checkVisibility()
  {
    if (debug)
      {
	System.out.println(" Checking visibility");
      }

    if (stringShowing || ipShowing || referenceShowing)
      {
	editPanel.setRowVisible(vectorCF, true);
	editPanel.setRowVisible(maxArrayN, vectorCF.isSelected());
      }
    else			// anything else can't be vector
      {
	editPanel.setRowVisible(vectorCF, false);
	editPanel.setRowVisible(maxArrayN, false);
      }

    if (passwordShowing)
      {
	editPanel.setRowVisible(cryptedCF, true);
	editPanel.setRowVisible(md5cryptedCF, true);
	editPanel.setRowVisible(apachemd5cryptedCF, true);
	editPanel.setRowVisible(winHashcryptedCF, true);
	editPanel.setRowVisible(sshaHashcryptedCF, true);
	editPanel.setRowVisible(shaUnixCryptedCF, true);
	editPanel.setRowVisible(shaUnixCryptTypeC, true);
	editPanel.setRowVisible(shaUnixCryptRoundsN, true);
	editPanel.setRowVisible(plainTextCF, true);
      }
    else
      {
	editPanel.setRowVisible(cryptedCF, false);
	editPanel.setRowVisible(md5cryptedCF, false);
	editPanel.setRowVisible(apachemd5cryptedCF, false);
	editPanel.setRowVisible(winHashcryptedCF, false);
	editPanel.setRowVisible(sshaHashcryptedCF, false);
	editPanel.setRowVisible(shaUnixCryptedCF, false);
	editPanel.setRowVisible(shaUnixCryptTypeC, false);
	editPanel.setRowVisible(shaUnixCryptRoundsN, false);
	editPanel.setRowVisible(plainTextCF, false);
      }

    editPanel.setRowVisible(labeledCF, booleanShowing);

    if (booleanShowing)
      {
	editPanel.setRowVisible(trueLabelS, labeledCF.isSelected());
	editPanel.setRowVisible(falseLabelS, labeledCF.isSelected());
      }
    else
      {
	editPanel.setRowVisible(trueLabelS, false);
	editPanel.setRowVisible(falseLabelS, false);
      }

    editPanel.setRowVisible(multiLineCF, stringShowing  && !vectorCF.isSelected());
    editPanel.setRowVisible(regexpS, stringShowing);
    editPanel.setRowVisible(regexpDescS, stringShowing);
    editPanel.setRowVisible(OKCharS, stringShowing || passwordShowing);
    editPanel.setRowVisible(BadCharS, stringShowing || passwordShowing);
    editPanel.setRowVisible(minLengthN, stringShowing || passwordShowing);
    editPanel.setRowVisible(maxLengthN, stringShowing || passwordShowing);
    editPanel.setRowVisible(namespaceC, stringShowing || numericShowing || ipShowing);
    editPanel.setRowVisible(shaUnixCryptTypeC, passwordShowing && shaUnixCryptedCF.isSelected());
    editPanel.setRowVisible(shaUnixCryptRoundsN, passwordShowing && shaUnixCryptedCF.isSelected());

    if (referenceShowing)
      {
	editPanel.setRowVisible(editInPlaceCF, true);
	editPanel.setRowVisible(targetC, true);

	if (editInPlaceCF.isSelected())
	  {
	    // edit in place fields don't target specific fields.

	    editPanel.setRowVisible(fieldC, false);
	  }
	else
	  {
	    if (((String)targetC.getModel().getSelectedItem()).equalsIgnoreCase(anyToken))
	      {
		editPanel.setRowVisible(fieldC, false);
	      }
	    else
	      {
		editPanel.setRowVisible(fieldC, true);
	      }
	  }
      }
    else
      {
	editPanel.setRowVisible(editInPlaceCF, false);
	editPanel.setRowVisible(targetC, false);
	editPanel.setRowVisible(fieldC, false);
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
      
     namespaceC.addItem(noneToken);      

     if (nameSpaces == null || nameSpaces.length == 0)
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

    targetC.addItem(anyToken);

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
   * in the BaseFieldEditor when the targetBase is not "&lt;any&gt;".  
   *
   * This method doesn't make a selection, so upon exit of this
   * method, "&lt;none&gt;" will be selected in the fieldC widget.
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
    
    target = (String) targetC.getModel().getSelectedItem();

    try
      {
	if (target.equals(allToken))
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

    fieldC.addItem(noneToken);
    
    if (fields == null)
      {
	if (debug)
	  {
	    System.out.println("fields == null");
	  }

	// By default, the Choice item will keep the
	// first item added.. the following line is
	// redundant, at least under JDK 1.1.2
	//	fieldC.select(noneToken);
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
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	System.err.println("changeTypeChoice(" + selectedItem + ")");
      }

    try
      {
	if (selectedItem.equalsIgnoreCase(booleanToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.BOOLEAN)))
	      {
		clearTypeChoice();
		booleanShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(numericToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.NUMERIC)))
	      {
		clearTypeChoice();
		numericShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(floatToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.FLOAT)))
	      {
		clearTypeChoice();
		floatShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(fieldOptionToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.FIELDOPTIONS)))
	      {
		clearTypeChoice();
		fieldOptionShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(dateToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.DATE)))
	      {
		clearTypeChoice();
		dateShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(stringToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.STRING)))
	      {
		clearTypeChoice();
		stringShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(objectRefToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.INVID)))
	      {
		clearTypeChoice();
		referenceShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(passwordToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.PASSWORD)))
	      {
		clearTypeChoice();
		passwordShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(ipToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.IP)))
	      {
		clearTypeChoice();
		ipShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else if (selectedItem.equalsIgnoreCase(permissionToken))
	  {
	    if (handleReturnVal(fieldDef.setType(FieldType.PERMISSIONMATRIX)))
	      {
		clearTypeChoice();
		permissionShowing = true;
	      }

	    refreshFieldEdit(true);
	    return;
	  }
	else
	  {
	    throw new RuntimeException("unrecognized selectedItem string: " + selectedItem);
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
   * Helper method for changeTypeChoice().
   */

  private void clearTypeChoice()
  {
    booleanShowing = false;
    numericShowing = false;
    floatShowing = false;
    fieldOptionShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    passwordShowing = false;
    ipShowing = false;
    permissionShowing = false;
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
    if (fieldNode == null)
      {
	throw new IllegalArgumentException("null fieldNode");
      }

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

    if (fieldNode == null)
      {
	throw new IllegalArgumentException("null fieldNode");
      }

    listenToCallbacks = false;	// so we don't get confused by programmatic edits
    owner.setWaitCursor();

    clearFields();

    if (!forceRefresh && (fieldNode == this.fieldNode))
      {
    	return;
      }

    this.fieldNode = fieldNode;

    this.fieldDef = fieldNode.getField();

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
	    regexpDescS.setText(fieldDef.getRegexpDesc());
	    OKCharS.setText(fieldDef.getOKChars());
	    BadCharS.setText(fieldDef.getBadChars());
	    
	    typeC.getModel().setSelectedItem(stringToken);
	    stringShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    if (debug)
	      {
		System.out.println(fieldDef.getNameSpaceLabel());
	      }

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.getModel().setSelectedItem(noneToken);

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
	    
	    typeC.getModel().setSelectedItem(passwordToken);
	    passwordShowing = true;

	    cryptedCF.setValue(fieldDef.isCrypted());
	    md5cryptedCF.setValue(fieldDef.isMD5Crypted());
	    apachemd5cryptedCF.setValue(fieldDef.isApacheMD5Crypted());
	    winHashcryptedCF.setValue(fieldDef.isWinHashed());
	    sshaHashcryptedCF.setValue(fieldDef.isSSHAHashed());
	    shaUnixCryptedCF.setValue(fieldDef.isShaUnixCrypted());

	    if (shaUnixCryptedCF.isSelected())
	      {
		if (fieldDef.isShaUnixCrypted512())
		  {
		    shaUnixCryptTypeC.getModel().setSelectedItem("SHA512");
		  }
		else
		  {
		    shaUnixCryptTypeC.getModel().setSelectedItem("SHA256");
		  }

		shaUnixCryptRoundsN.setValue(fieldDef.getShaUnixCryptRounds());
	      }

	    plainTextCF.setValue(fieldDef.isPlainText());

	    // if a password is not crypted, it *must* keep
	    // passwords in plaintext
	  }
	else if (fieldDef.isIP())
	  {
	    typeC.getModel().setSelectedItem(ipToken);
	    ipShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    if (debug)
	      {
		System.out.println(fieldDef.getNameSpaceLabel());
	      }

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.getModel().setSelectedItem(noneToken);

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

	    typeC.getModel().setSelectedItem(booleanToken);
	    booleanShowing = true;
	  }
	else if (fieldDef.isInvid())
	  {
	    editInPlaceCF.setValue(fieldDef.isEditInPlace());

	    // all edit in place references are vectors

	    if (fieldDef.isEditInPlace())
	      {
		vectorCF.setSelected(true, false);
		handleReturnVal(fieldDef.setArray(true));
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
		    targetC.getModel().setSelectedItem(anyToken);
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
			targetC.addItem(allToken);
			targetC.getModel().setSelectedItem(allToken);
		      }
			
		    string = allToken;
			
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
			    handleReturnVal(fieldDef.setTargetBase(null));
			  }
			catch (RemoteException rx)
			  {
			    throw new IllegalArgumentException("Exception couldn't clear target base: " + rx);
			  }
			
			if (updateTargetC)
			  {
			    targetC.getModel().setSelectedItem(anyToken);
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

		    fieldC.getModel().setSelectedItem(noneToken);
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
			    handleReturnVal(fieldDef.setTargetField(null));
			  }
			catch (RemoteException rx)
			  {
			    throw new IllegalArgumentException("Exception couldn't clear target base: " + rx);
			  }
			
			fieldC.getModel().setSelectedItem(noneToken);
		      }
		  }
	      } // else targetB != -1
	    
	    typeC.getModel().setSelectedItem(objectRefToken);
	    referenceShowing = true;
	  }
	else if (fieldDef.isDate())
	  {
	    typeC.getModel().setSelectedItem(dateToken);
	    dateShowing = true;
	  }
	else if (fieldDef.isNumeric())
	  {
	    typeC.getModel().setSelectedItem(numericToken);
	    numericShowing = true;

	    // add all defined namespaces here

	    refreshNamespaceChoice();

	    if (debug)
	      {
		System.out.println(fieldDef.getNameSpaceLabel());
	      }

	    if (fieldDef.getNameSpaceLabel() == null)
	      {
		namespaceC.getModel().setSelectedItem(noneToken);

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
 	else if (fieldDef.isFloat())
 	  {
 	    typeC.getModel().setSelectedItem(floatToken);
 	    floatShowing = true;
 	  }
 	else if (fieldDef.isFieldOptions())
 	  {
 	    typeC.getModel().setSelectedItem(fieldOptionToken);
 	    fieldOptionShowing = true;
 	  }
	else if (fieldDef.isPermMatrix())
	  {
	    typeC.addItem(permissionToken);
	    typeC.getModel().setSelectedItem(permissionToken);
	  }
	else
	  {
	    throw new RuntimeException("unrecognized field type");
	  }

	// Here is where the editability is checked.

	commentT.setEditable(true);
	nameS.setEditable(true);
	trueLabelS.setEditable(true);
	falseLabelS.setEditable(true);
	regexpS.setEditable(true);
	regexpDescS.setEditable(true);
	OKCharS.setEditable(true);
	BadCharS.setEditable(true);
	maxArrayN.setEditable(true);
	minLengthN.setEditable(true);
	maxLengthN.setEditable(true);
	idN.setEditable(false);

	multiLineCF.setEnabled(true);
	cryptedCF.setEnabled(true);
	md5cryptedCF.setEnabled(true);
	apachemd5cryptedCF.setEnabled(true);
	winHashcryptedCF.setEnabled(true);
	sshaHashcryptedCF.setEnabled(true);
	shaUnixCryptedCF.setEnabled(true);

	if (passwordShowing)
	  {
	    // if we aren't using a hashed form for password storage,
	    // we have to use plaintext

	    if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || 
		  apachemd5cryptedCF.isSelected() || winHashcryptedCF.isSelected() ||
		  sshaHashcryptedCF.isSelected() || shaUnixCryptedCF.isSelected())
		&& plainTextCF.isSelected())
	      {
		plainTextCF.setEnabled(false);
	      }
	  }
	else
	  {
	    plainTextCF.setEnabled(true);
	  }

	vectorCF.setEnabled(true);
	labeledCF.setEnabled(true);
	editInPlaceCF.setEnabled(true);

	typeC.setEnabled(true);
	namespaceC.setEnabled(true);
	targetC.setEnabled(true);
	fieldC.setEnabled(true);

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
   * Reinitialize the BaseFieldEditor with the current field.
   */

  public void refreshFieldEdit(boolean updateTargetC)
  {
    this.editField(fieldNode, true, updateTargetC);
  }

  /*
   * This method is called when we switch away from editing this
   * field.. this gives us a chance to process the stringArea
   * components.. they, like the one-line string fields, are
   * focus-sensitive, but it's generally not as obvious to people that
   * a multi-line string component is focus sensitive in the same way.
   */

  public void switchAway()
  {
    commentT.sendCallback();
  }

  /**
   * For string, numeric, and checkbox fields
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

	    if (handleReturnVal(fieldDef.setName((String) v.getValue())))
	      {
		fieldNode.setText((String) v.getValue());
		owner.tree.refresh();
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == commentT)
	  {
	    if (debug)
	      {
		System.out.println("commentT");
	      }

	    if (!handleReturnVal(fieldDef.setComment((String) v.getValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == maxArrayN)
	  {
	    if (debug)
	      {
		System.out.println("maxArrayN");
	      }

	    if (!handleReturnVal(fieldDef.setMaxArraySize(((Integer)v.getValue()).shortValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == vectorCF)
	  {
	    //editPanel.setRowVisible(maxArrayN, vectorCF.getValue());

	    if (debug)
	      {
		System.out.println("vectorCF");
	      }

	    if (handleReturnVal(fieldDef.setArray(vectorCF.isSelected())))
	      {
		checkVisibility();
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == regexpS)
	  {
	    if (debug)
	      {
		System.out.println("regexpS");
	      }

	    // setting a regexp can fail if it can't be properly
	    // parsed

	    if (!handleReturnVal(fieldDef.setRegexpPat((String) v.getValue())))
	      {
		regexpS.setText(fieldDef.getRegexpPat());
	      }
	  }
	else if (comp == regexpDescS)
	  {
	    if (debug)
	      {
		System.out.println("regexpDescS");
	      }

	    if (!handleReturnVal(fieldDef.setRegexpDesc((String) v.getValue())))
	      {
		regexpDescS.setText(fieldDef.getRegexpDesc());
	      }
	  }
	else if (comp == OKCharS)
	  {
	    if (debug)
	      {
		System.out.println("OkCharS");
	      }

	    if (!handleReturnVal(fieldDef.setOKChars((String) v.getValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == BadCharS)
	  {
	    if (debug)
	      {
		System.out.println("BadCharS");
	      }

	    if (!handleReturnVal(fieldDef.setBadChars((String) v.getValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == minLengthN)
	  {
	    if (debug)
	      {
		System.out.println("minLengthN");
	      }

	    if (!handleReturnVal(fieldDef.setMinLength(((Integer)v.getValue()).shortValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == maxLengthN)
	  {
	    if (debug)
	      {
		System.out.println("maxLengthN");
	      }

	    if (!handleReturnVal(fieldDef.setMaxLength(((Integer)v.getValue()).shortValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == trueLabelS)
	  {
	    if (debug)
	      {
		System.out.println("trueLabelS");
	      }

	    if (!handleReturnVal(fieldDef.setTrueLabel((String) v.getValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == falseLabelS)
	  {
	    if (debug)
	      {
		System.out.println("falseLabelS");
	      }

	    if (!handleReturnVal(fieldDef.setFalseLabel((String) v.getValue())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == labeledCF)
	  {
	    if (debug)
	      {
		System.out.println("labeledCF");
	      }

	    owner.handleReturnVal(fieldDef.setLabeled(labeledCF.isSelected()));
	    checkVisibility();
	  }
	else if (comp == editInPlaceCF)
	  {
	    if (debug)
	      {
		System.out.println("editInPlaceCF");
	      }

	    handleReturnVal(fieldDef.setEditInPlace(editInPlaceCF.isSelected()));

	    refreshFieldEdit(true); // we need to refresh on success or failure here
	  }
	else if (comp == cryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("cryptedCF");
	      }

	    if (handleReturnVal(fieldDef.setCrypted(cryptedCF.isSelected())))
	      {
		// a password field has to have plaintext stored if it
		// is not to store the password in crypted form.
		
		if (!cryptedCF.isSelected() && !md5cryptedCF.isSelected() && 
		    !apachemd5cryptedCF.isSelected() && !winHashcryptedCF.isSelected() &&
		    !sshaHashcryptedCF.isSelected() && !shaUnixCryptedCF.isSelected()
		    && !plainTextCF.isSelected())
		  {
		    plainTextCF.setValue(true);
		  }

		if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || 
		      apachemd5cryptedCF.isSelected() || winHashcryptedCF.isSelected() ||
		      sshaHashcryptedCF.isSelected() || shaUnixCryptedCF.isSelected()))
		  {
		    plainTextCF.setEnabled(false);
		  }
		else
		  {
		    plainTextCF.setEnabled(true);
		  }
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == md5cryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("md5cryptedCF");
	      }

	    if (handleReturnVal(fieldDef.setMD5Crypted((md5cryptedCF.isSelected()))))
	      {
		// a password field has to have plaintext stored if it
		// is not to store the password in crypted form.

		if (!cryptedCF.isSelected() && !md5cryptedCF.isSelected() && !winHashcryptedCF.isSelected()
		    && !sshaHashcryptedCF.isSelected()
		    && !shaUnixCryptedCF.isSelected()
		    && !plainTextCF.isSelected())
		  {
		    plainTextCF.setValue(true);
		  }

		if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || winHashcryptedCF.isSelected() ||
		      sshaHashcryptedCF.isSelected() || shaUnixCryptedCF.isSelected()))
		  {
		    plainTextCF.setEnabled(false);
		  }
		else
		  {
		    plainTextCF.setEnabled(true);
		  }
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == apachemd5cryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("apachemd5cryptedCF");
	      }

	    if (handleReturnVal(fieldDef.setApacheMD5Crypted((apachemd5cryptedCF.isSelected()))))
	      {
		// a password field has to have plaintext stored if it
		// is not to store the password in crypted form.
		
		if (!cryptedCF.isSelected() && !md5cryptedCF.isSelected() && 
		    !apachemd5cryptedCF.isSelected() && !winHashcryptedCF.isSelected() &&
		    !sshaHashcryptedCF.isSelected() && !shaUnixCryptedCF.isSelected()
		    && !plainTextCF.isSelected())
		  {
		    plainTextCF.setValue(true);
		  }
		
		if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || 
		      apachemd5cryptedCF.isSelected() || winHashcryptedCF.isSelected() ||
		      sshaHashcryptedCF.isSelected() || shaUnixCryptedCF.isSelected()))
		  {
		    plainTextCF.setEnabled(false);
		  }
		else
		  {
		    plainTextCF.setEnabled(true);
		  }
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == winHashcryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("winHashcryptedCF");
	      }

	    if (handleReturnVal(fieldDef.setWinHashed((winHashcryptedCF.isSelected()))))
	      {
		// a password field has to have plaintext stored if it
		// is not to store the password in crypted form.
		
		if (!cryptedCF.isSelected() && !md5cryptedCF.isSelected() &&
		    !apachemd5cryptedCF.isSelected() && !winHashcryptedCF.isSelected()
		    && !plainTextCF.isSelected())
		  {
		    plainTextCF.setValue(true);
		  }
		
		if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || 
		      apachemd5cryptedCF.isSelected() || winHashcryptedCF.isSelected()))
		  {
		    plainTextCF.setEnabled(false);
		  }
		else
		  {
		    plainTextCF.setEnabled(true);
		  }
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == sshaHashcryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("sshaHashcryptedCF");
	      }

	    if (handleReturnVal(fieldDef.setSSHAHashed((sshaHashcryptedCF.isSelected()))))
	      {
		// a password field has to have plaintext stored if it
		// is not to store the password in crypted form.
		
		if (!cryptedCF.isSelected() && !md5cryptedCF.isSelected() &&
		    !apachemd5cryptedCF.isSelected() && !winHashcryptedCF.isSelected() &&
		    !sshaHashcryptedCF.isSelected()
		    && !plainTextCF.isSelected())
		  {
		    plainTextCF.setValue(true);
		  }
		
		if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || 
		      apachemd5cryptedCF.isSelected() || winHashcryptedCF.isSelected() ||
		      sshaHashcryptedCF.isSelected()))
		  {
		    plainTextCF.setEnabled(false);
		  }
		else
		  {
		    plainTextCF.setEnabled(true);
		  }
	      }
	    else
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == shaUnixCryptedCF)
	  {
	    if (debug)
	      {
		System.out.println("shaUnixCryptedCF");
	      }

	    if (handleReturnVal(fieldDef.setShaUnixCrypted((shaUnixCryptedCF.isSelected()))))
	      {
		// a password field has to have plaintext stored if it
		// is not to store the password in crypted form.
		
		if (!cryptedCF.isSelected() && !md5cryptedCF.isSelected() &&
		    !apachemd5cryptedCF.isSelected() && !winHashcryptedCF.isSelected() &&
		    !sshaHashcryptedCF.isSelected() && !shaUnixCryptedCF.isSelected()
		    && !plainTextCF.isSelected())
		  {
		    plainTextCF.setValue(true);
		  }
		
		if (!(cryptedCF.isSelected() || md5cryptedCF.isSelected() || 
		      apachemd5cryptedCF.isSelected() || winHashcryptedCF.isSelected() ||
		      sshaHashcryptedCF.isSelected() || shaUnixCryptedCF.isSelected()))
		  {
		    plainTextCF.setEnabled(false);
		  }
		else
		  {
		    plainTextCF.setEnabled(true);
		  }
	      }

	    refreshFieldEdit(false);
	  }
	else if (comp == shaUnixCryptRoundsN)
	  {
	    if (debug)
	      {
		System.out.println("shaUnixCryptRoundsN");
	      }

	    handleReturnVal(fieldDef.setShaUnixCryptRounds(((Integer)v.getValue()).shortValue()));
	  }
	else if (comp == plainTextCF)
	  {
	    if (debug)
	      {
		System.out.println("plainTextCF");
	      }

	    if (!handleReturnVal(fieldDef.setPlainText(plainTextCF.isSelected())))
	      {
		refreshFieldEdit(false);
	      }
	  }
	else if (comp == multiLineCF)
	  {
	    if (debug)
	      {
		System.out.println("multiLineCF: " + multiLineCF.isSelected());
	      }

	    if (!handleReturnVal(fieldDef.setMultiLine(multiLineCF.isSelected())))
	      {
		refreshFieldEdit(false);
	      }
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

	if (!item.equals(numericToken) && !item.equals(floatToken) && !item.equals(stringToken))
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
		// "Warning: Changing Object Type"
		// "Changing the type of this field will invalidate the label for this base.\nAre you sure you want to continue?"

		changeLabelTypeDialog = new StringDialog(owner, 
							 ts.l("itemStateChanged.typeChangeWarningTitle"),
							 ts.l("itemStateChanged.typeChangeLabelMessage"),
							 StringDialog.getDefaultOk(),
							 StringDialog.getDefaultCancel());
		
		Hashtable answer = changeLabelTypeDialog.showDialog();

		if (answer != null)  //Ok button was clicked
		  {
		    try
		      {
			if (debug)
			  {
			    System.out.println(" clicked ok");
			  }

			// we're making this field unacceptable as a label

			handleReturnVal(currentBase.setLabelField(null));
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
			    typeC.getModel().setSelectedItem(numericToken);
			  }
			else if (fieldDef.isFloat())
			  {
			    typeC.getModel().setSelectedItem(floatToken);
			  }
			else if (fieldDef.isString())
			  {
			    typeC.getModel().setSelectedItem(stringToken);
			  }
			else
			  {
			    System.err.println("Field is not String, Float or Numeric, not changing type choice");
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
		if (debug)
		  {
		    System.out.println("not the label, ok to change");
		  }
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
	    if (item.equalsIgnoreCase(noneToken))
	      {
		if (!handleReturnVal(fieldDef.setNameSpace(null)))
		  {
		    refreshFieldEdit(true);
		  }
	      }
	    else
	      {
		if (!handleReturnVal(fieldDef.setNameSpace(item)))
		  {
		    refreshFieldEdit(true);
		  }
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

	if (item.equalsIgnoreCase(anyToken))
	  {
	    try
	      {
		if (!handleReturnVal(fieldDef.setTargetBase(null)))
		  {
		    refreshFieldEdit(true);
		  }
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

		    if (handleReturnVal(fieldDef.setTargetBase(item)))
		      {
			// if we've changed our target base, clear out the
			// target field to avoid accidental confusion if our
			// new target base has a valid target field with the
			// same id code as our old target field.
			
			if ((oldBaseName != null) && !oldBaseName.equals(item))
			  {
			    handleReturnVal(fieldDef.setTargetField(null));
			  }
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
	    if (item.equals(noneToken))
	      {
		if (!handleReturnVal(fieldDef.setTargetField(null)))
		  {
		    refreshFieldEdit(true);
		  }
	      }
	    else
	      {
		if (!handleReturnVal(fieldDef.setTargetField(item)))
		  {
		    refreshFieldEdit(true);
		  }
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException ("Exception setting TargetField: " + rx);
	  }
      }
    else if (e.getItemSelectable() == shaUnixCryptTypeC)
      {
	item = (String)shaUnixCryptTypeC.getSelectedItem();

	if (debug)
	  {
	    System.out.println("Setting sha unix crypt type to " + item);
	  }

	try
	  {
	    if (item.equals("SHA512"))
	      {
		handleReturnVal(fieldDef.setShaUnixCrypted512(true));
	      }
	    else
	      {
		handleReturnVal(fieldDef.setShaUnixCrypted512(false));
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException ("Exception setting sha unix crypt variant: " + rx);
	  }
      }
  }

  /**
   * GC-aiding dissolution method.  Should be called on GUI thread.
   */

  public void cleanup()
  {
    this.fieldNode = null;
    this.fieldDef = null;	// remote reference
    this.owner = null;
    this.changeLabelTypeDialog = null;
    this.commentT = null;

    this.nameS = null;
    this.trueLabelS = null;
    this.falseLabelS = null;
    this.OKCharS = null;
    this.BadCharS = null;
    this.regexpS = null;
    this.regexpDescS = null;

    this.idN = null;
    this.maxArrayN = null;
    this.minLengthN = null;
    this.maxLengthN = null;

    this.vectorCF = null;
    this.labeledCF = null;
    this.editInPlaceCF = null;
    this.cryptedCF = null;
    this.md5cryptedCF = null;
    this.apachemd5cryptedCF = null;
    this.winHashcryptedCF = null;
    this.sshaHashcryptedCF = null;
    this.shaUnixCryptedCF = null;
    this.plainTextCF = null;
    this.multiLineCF = null;

    this.typeC = null;
    this.namespaceC = null;
    this.targetC = null;
    this.fieldC = null;

    this.rowHash = null;

    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread

    if (editPanel != null)
      {
	editPanel.cleanup();
	editPanel = null;
      }
  }

  private boolean handleReturnVal(ReturnVal retVal)
  {
    ReturnVal rv = owner.handleReturnVal(retVal);

    return (rv == null || rv.didSucceed());
  }
}
