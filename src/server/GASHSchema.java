/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Version: $Revision: 1.12 $ %D%
   Module By: Jonathan Abbey and Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.DataComponent.*;

import tablelayout.*;

import java.rmi.*;
import java.rmi.server.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.applet.*;
import java.util.*;

import gjt.Box;
import gjt.Util;
import gjt.RowLayout;
import gjt.ColumnLayout;

import arlut.csd.Tree.*;
// import arlut.csd.Dialog.YesNoDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GASHSchema

------------------------------------------------------------------------------*/

public class GASHSchema extends Frame implements treeCallback, ActionListener, Compare {

  SchemaEdit 
    editor;

  Image
    images[];

  treeControl 
    tree;

  treeNode
    objects, namespaces;

  MenuItem
    createObjectMI = null,
    deleteObjectMI = null,
    createNameMI = null,
    deleteNameMI = null,
    createFieldMI = null,
    deleteFieldMI = null;

  CardLayout
    card;

  Panel 
    displayPane,
    buttonPane,
    attribPane,
    attribCardPane,
    baseEditPane,
    fieldEditPane,
    namespaceEditPane,
    attribButtonPane;

  BaseEditor
    be;

  BaseFieldEditor
    fe;

  Button
    okButton, cancelButton, attribOkButton;

  GridBagLayout 
    gbl;

  GridBagConstraints
    gbc;

  Toolkit
    toolkit;

  Base[] bases;

  Hashtable
    baseHash,
    fieldHash;

  /* -- */

  public GASHSchema(String title, SchemaEdit editor)
  {
    super(title);
    
    setLayout(new BorderLayout());

    displayPane = new Panel();
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    displayPane.setLayout(gbl);

    attribPane = new Panel();
    attribPane.setBackground(Color.white);
    attribPane.setLayout(new BorderLayout());

    card = new CardLayout();

    attribCardPane = new Panel();
    attribCardPane.setBackground(Color.white);
    attribCardPane.setLayout(card);

    baseEditPane = new Panel();
    baseEditPane.setBackground(Color.white);
    baseEditPane.setLayout(new BorderLayout());

    // initialize the base editor

    be = new BaseEditor(this);
    baseEditPane.add("Center", be);

    fieldEditPane = new Panel();
    fieldEditPane.setBackground(Color.white);
    fieldEditPane.setLayout(new BorderLayout());

    fe = new BaseFieldEditor(this);
    fieldEditPane.add("Center", fe);

    namespaceEditPane = new Panel();
    namespaceEditPane.setBackground(Color.white);
    namespaceEditPane.setLayout(new BorderLayout());

    attribCardPane.add("base", baseEditPane);
    attribCardPane.add("field", fieldEditPane);
    attribCardPane.add("name", namespaceEditPane);

    attribButtonPane = new Panel();
    attribButtonPane.setBackground(Color.white);
    attribButtonPane.setLayout(new RowLayout());

    attribPane.add("Center", attribCardPane);
    attribPane.add("South", attribButtonPane);

    Box rightBox = new Box(attribPane, "Attributes");

    toolkit = Toolkit.getDefaultToolkit();

    images = new Image[3];

    try
      {
	images[0] = toolkit.getImage(new URL("http://www.arlut.utexas.edu/~broccol/gash2/openfolder.gif"));
	Util.waitForImage(this, images[0]);

	images[1] = toolkit.getImage(new URL("http://www.arlut.utexas.edu/~broccol/gash2/folder.gif"));
	Util.waitForImage(this, images[1]);

	images[2] = toolkit.getImage(new URL("http://www.arlut.utexas.edu/~broccol/gash2/list.gif"));
	Util.waitForImage(this, images[2]);
      }
    catch (MalformedURLException e)
      {
	System.err.println("Bad URL");
      }

    tree = new treeControl(new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white, this, images,
			   null);

    PopupMenu objectMenu = new PopupMenu();
    createObjectMI = new MenuItem("Create Object Type");
    objectMenu.add(createObjectMI);

    objects = new treeNode(null, "Object Types", null, true, 0, 1, objectMenu);
    tree.setRoot(objects);

    PopupMenu nameSpaceMenu = new PopupMenu();
    createNameMI = new MenuItem("Create Namespace");
    nameSpaceMenu.add(createNameMI);

    namespaces = new treeNode(null, "Namespaces", objects, true, 0, 1, nameSpaceMenu);
    tree.insertNode(namespaces, false);

    Box leftBox = new Box(tree, "Schema Objects");

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0.50;
    gbc.weighty = 1.0;
    gbl.setConstraints(leftBox, gbc);

    displayPane.add(leftBox);

    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.weightx = 0.50;
    gbc.weighty = 1.0;
    gbl.setConstraints(rightBox, gbc);

    displayPane.add(rightBox);

    buttonPane = new Panel();
    buttonPane.setLayout(new RowLayout());

    okButton = new Button("ok");
    okButton.addActionListener(this);

    cancelButton = new Button("cancel");
    cancelButton.addActionListener(this);

    buttonPane.add(okButton);
    buttonPane.add(cancelButton);

    add("Center", displayPane);
    add("South", buttonPane);

    this.editor = editor;

    deleteObjectMI = new MenuItem("Delete Object Type");
    createFieldMI = new MenuItem("Create Field");
    deleteFieldMI = new MenuItem("Delete Field");

    fieldHash = new Hashtable();
    baseHash = new Hashtable();

    objectsRefresh();

    pack();
    setSize(800, 600);
    show();
  }

  /**
   *
   * Base ID comparator for the Quicksort used in objectsRefresh.
   *
   * @see arlut.csd.Util.Compare
   */

  public int compare(Object a, Object b) 
  {
    Base aB, bB;
      
    aB = (Base) a;
    bB = (Base) b;

    try
      {
	if (aB.getTypeID() < bB.getTypeID())
	  {
	    return -1;
	  }
	else if (aB.getTypeID() > bB.getTypeID())
	  {
	    return 1;
	  }
	else
	  {
	    return 0;
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't compare bases " + ex);
      }
  }

  /**
   *
   *
   *
   */

  void objectsRefresh()
  {
    try
      {
	refreshBases();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't refresh bases" + ex);
      }

    refreshNamespaces();
    tree.refresh();
  }

  void refreshBases() throws RemoteException
  {
    Base base;
    BaseNode bNode, oldNode, newNode;
    treeNode parentNode;
    String baseName = null;
    PopupMenu menu;
    int i;
    
    /* -- */

    menu = new PopupMenu("Base Menu");
    menu.add(createFieldMI);
    menu.add(deleteObjectMI);

    try
      {
	bases = editor.getBases();
      }
    catch (RemoteException ex)
      {
	System.err.println("GASHSchema: refreshBases(): couldn't get bases");
	System.err.println(ex.toString());

	throw new RuntimeException("couldn't get bases" + ex);
      }

    // editor.getBases() returns items in hash order.. sort
    // them by baseID before adding them to tree

    (new QuickSort(bases,  this)).sort();

    // now that we've got our bases in order, we need to go
    // through our basenodes in the tree in order, inserting
    // any bases that aren't in the tree into the tree in 
    // the proper place.  We'll go ahead and call setText()
    // on the existent nodes to make sure that we've caught
    // a base rename.

    parentNode = objects;
    oldNode = null;
    bNode = (BaseNode) parentNode.getChild();
    i = 0;

    while (i < bases.length)
      {
	base = bases[i];

	if ((bNode == null) ||
	    (base.getTypeID() > bNode.getBase().getTypeID()))
	  {
	    // insert a new base node 

	    newNode = new BaseNode(parentNode, base.getName(), base,
				   oldNode, true, 0, 1, menu);

	    tree.insertNode(newNode, false);
	    baseHash.put(base, newNode);

	    refreshFields(newNode, false);

	    oldNode = newNode;
	    bNode = (BaseNode) oldNode.getNextSibling();
	  }
	else if (base.getTypeID() < bNode.getBase().getTypeID())
	  {
	    // delete a base node

	    bNode = (BaseNode) bNode.getNextSibling();
	    baseHash.remove(bNode.getBase());

	    tree.deleteNode(bNode.getPrevSibling(), false);
	  }
	else
	  {
	    bNode.setText(base.getName());
	    refreshFields(bNode, false);

	    oldNode = bNode;
	    bNode = (BaseNode) oldNode.getNextSibling();
	  }
	
	i++;
      }
  }

  void refreshFields(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Base base;
    BaseField field, fields[];
    Vector vect;
    BaseNode parentNode;
    FieldNode oldNode, newNode, fNode;
    PopupMenu menu;
    int i;

    /* -- */

    base = node.getBase();

    vect = base.getFields();

    menu = new PopupMenu(base.getName() + " Field Menu");
    menu.add(deleteFieldMI);
    
    fields = new BaseField[vect.size()];
    
    for (i = 0; i < fields.length; i++)
      {
	fields[i] = (BaseField) vect.elementAt(i);
      }
    
    // Sort the fields by ID, using a funky anonymous
    // class
    
    (new QuickSort(fields, 
		   new arlut.csd.Util.Compare() 
		   {
		     public int compare(Object a, Object b) 
		       {
			 BaseField aF, bF;
      
			 aF = (BaseField) a;
			 bF = (BaseField) b;
	
			 try
			   {
			     if (aF.getID() < bF.getID())
			       {
				 return -1;
			       }
			     else if (aF.getID() > bF.getID())
			       {
				 return 1;
			       }
			     else
			       {
				 return 0;
			       }
			   }
			 catch (RemoteException ex)
			   {
			     throw new RuntimeException("couldn't compare base fields " + ex);
			   }
		       }
		   }
		   )).sort();

    parentNode = node;
    oldNode = null;
    fNode = (FieldNode) node.getChild();
    i = 0;
	
    while (i < fields.length)
      {
	field = fields[i];

	if ((fNode == null) ||
	    (field.getID() > fNode.getField().getID()))
	  {
	    // insert a new field node

	    newNode = new FieldNode(parentNode, field.getName(), field,
				    oldNode, false, 2, 2, menu);

	    tree.insertNode(newNode, false);
	    fieldHash.put(field, newNode);

	    oldNode = newNode;
	    fNode = (FieldNode) oldNode.getNextSibling();
	  }
	else if (field.getID() < fNode.getField().getID())
	  {
	    // delete a field node

	    fNode = (FieldNode) fNode.getNextSibling();
	    baseHash.remove(fNode.getField());

	    tree.deleteNode(fNode.getPrevSibling(), false);
	  }
	else
	  {
	    fNode.setText(field.getName());

	    oldNode = fNode;
	    fNode = (FieldNode) oldNode.getNextSibling();
	  }

	i++;
      }
  }

  void refreshFields(Base base, boolean doRefresh) throws RemoteException
  {
    refreshFields((BaseNode) baseHash.get(base), doRefresh);
  }

  void refreshNamespaces()
  {
    // tree.removeChildren(namespaces, false);
  }

  void editBase(Base base)
  {
    be.editBase(base);
    card.show(attribCardPane,"base");

    // attach the button pane to the base editor
    
    attribButtonPane.removeAll();
    attribOkButton = new Button("ok");
    attribOkButton.addActionListener(be);
    attribButtonPane.add(attribOkButton);
    validate();
  }

  void editField(BaseField field)
  {
    System.err.println("in GASHSchema.editField");
    fe.editField(field);
    card.show(attribCardPane, "field");

    // attach the button pane to the field editor
    
    attribButtonPane.removeAll();
    attribOkButton = new Button("ok");
    attribOkButton.addActionListener(fe);
    attribButtonPane.add(attribOkButton);
    validate();
  }

  void editNameSpace(NameSpace space)
  {
    //
  }

  // treeCallback methods

  public void treeNodeSelected(treeNode node)
  {
    String a, b;
    treeNode n;

    /* -- */

    if (node == null)
      {
	throw new IllegalArgumentException("null node");
      }

    if (node instanceof BaseNode)
      {
	Base base = ((BaseNode) node).getBase();
	editBase(base);
      }
    else if (node instanceof FieldNode)
      {
	BaseField field = ((FieldNode) node).getField();
	editField(field);
      }
    else
      {
      }
  }

  public void treeNodeUnSelected(treeNode node)
  {
    System.out.println("node " + node.getText() + " unselected");
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    String nodeText;

    nodeText = node.getText();

    System.out.println("node " + nodeText + ", action: " + event );
    
    if (event.getSource() == createObjectMI)
      {
	try
	  {
	    editBase(editor.createNewBase());
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new base." + ex);
	  }
      }
    else if (event.getSource() == createNameMI)
      {
      }
    else if (event.getSource() == createFieldMI)
      {
	// find the base that asked for the field

	Base editbase = null;
	String tmp;
	
	try
	  {
	    for (int i = 0; i < bases.length; i++)
	      {
		tmp = bases[i].getName();
		if (tmp.equals(nodeText))
		  {
		    editbase = bases[i];
		    break;
		  }
	      }

	    if (editbase != null)
	      {
		System.err.println("Calling editField");
		editField(editbase.createNewField());
		System.err.println("Called editField");
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new field" + ex);
	  }
      }
  }

  // action handler

  public void actionPerformed(ActionEvent event)
  {
    //    System.out.println("event: " + event);

    if (event.getSource() == okButton)
      {
	try
	  {
	    editor.commit();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't commit: " + ex);
	  }
	setVisible(false);
      }
    else if (event.getSource() == cancelButton)
      {
	try
	  {
	    editor.release();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't release: " + ex);
	  }
	setVisible(false);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      BaseEditor

------------------------------------------------------------------------------*/

class BaseEditor extends ScrollPane implements setValueCallback, ActionListener {

  Base base;
  numberField typeN;
  stringField nameS, classS;
  componentAttr ca;
  Panel editPanel;
  GASHSchema owner;

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

    editPanel = new Panel();
    editPanel.setLayout(new ColumnLayout());
    
    ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white);

    typeN = new numberField(20, ca, false, false, 0, 0);
    typeN.setCallback(this);
    editPanel.add(new FieldWrapper("Object Type ID:", typeN));

    nameS = new stringField(20, 100, ca, true, false, null, null);
    nameS.setCallback(this);
    editPanel.add(new FieldWrapper("Object Type:", nameS));

    classS = new stringField(20, 100, ca, true, false, null, null);
    classS.setCallback(this);
    editPanel.add(new FieldWrapper("Class name:", classS));

    add(editPanel);
    //    doLayout();
  }

  /**
   *
   * This method is used to retarget the base editor to a new base
   * without having to break down and reconstruct the panels.
   */

  public void editBase(Base base)
  {
    this.base = base;

    try
      {
	typeN.setValue(base.getTypeID());
	nameS.setText(base.getName());
	classS.setText(base.getClassName());
      }
    catch (RemoteException ex)
      {
	System.err.println("editBase: accessor failed: " + ex);
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
	    base.setName(val);
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

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == owner.attribOkButton)
      {
	owner.objectsRefresh();
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 BaseFieldEditor

------------------------------------------------------------------------------*/

class BaseFieldEditor extends ScrollPane implements setValueCallback, ActionListener, ItemListener, TextListener {

  BaseField 
    fieldDef;

  CardLayout
    card;

  Panel 
    mainPanel,
    editPanel;
    //    cardPanel,
    // emptyPanel,
    //stringPanel,
    //booleanPanel,
    //invidPanel;

  GASHSchema 
    owner;

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
    vectorCF,			// all
    labeledCF,			// boolean
    targetLimitCF,		// invid
    symmetryCF;			// invid

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
    referenceShowing;

  BaseFieldEditor(GASHSchema owner)
  {
    rowHash = new Hashtable();

    fieldDef = null;
    this.owner = owner;
    
    mainPanel = new Panel();
    mainPanel.setLayout(new BorderLayout());
    
    editPanel = new Panel();
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
    typeC.addItemListener(this);

    //choose the one that is the default
    changeTypeChoice("Boolean");

   
    addRow(editPanel, typeC, "Field Type:", 6);

    minLengthN = new numberField(20, ca, true, false, 0, Integer.MAX_VALUE);
    minLengthN.setCallback(this);
    addRow(editPanel, minLengthN, "Minimum String Size:", 7);
    
    maxLengthN = new numberField(20, ca, true, false, 0, Integer.MAX_VALUE);
    maxLengthN.setCallback(this);
    addRow(editPanel, maxLengthN, "Maximum String Size:", 8);
   
    OKCharS = new stringField(20, 100, ca, true, false, null, null);
    OKCharS.setCallback(this);
    addRow(editPanel, OKCharS, " Allowed Chars:", 9);

    BadCharS = new stringField(20, 100, ca, true, false, null, null);
    BadCharS.setCallback(this);
    addRow(editPanel, BadCharS, "Disallowed Chars:", 10);

    namespaceC = new Choice();
    namespaceC.addItemListener(this);
/***************************************************
 *  NameSpaces don't work yet, so can't add anything.
    NameSpace[] nameSpaces = null;
    try
      {
	nameSpaces = owner.editor.getNameSpaces();
      }
    catch (RemoteException rx)
      {
	System.err.println("RemoteException getting namespaces: " + rx);
      }
      
    if ( (nameSpaces.length == 0) || (nameSpaces == null) )
      namespaceC.addItem("<none>");
    else
      for (int i=0 ; i < nameSpaces.length ; i++)
	{
	  try
	    {
	      namespaceC.addItem(nameSpaces[i].getName());
	    }
	  catch (RemoteException rx)
	    {
	      System.err.println("RemoteException getting namespaces: " + rx);
	      
	      
	    }    
	}
**********************************************/
    namespaceC.addItem("<none>");
    addRow(editPanel, namespaceC, "Namespace:", 11);
    
    labeledCF = new checkboxField(null, false, ca, true);
    labeledCF.setCallback(this);
    addRow(editPanel, labeledCF, "Labeled:", 12);

    trueLabelS = new stringField(20, 100, ca, true, false, null, null);
    trueLabelS.setCallback(this);
    addRow(editPanel, trueLabelS, "True Label:", 13);

    falseLabelS = new stringField(20, 100, ca, true, false, null, null);
    falseLabelS.setCallback(this);
    addRow(editPanel, falseLabelS, "False Label:", 14);

    targetLimitCF = new checkboxField(null, false, ca, true);
    targetLimitCF.setCallback(this);
    addRow(editPanel, targetLimitCF, "Restricted Target:", 15);

    targetC = new Choice();
    targetC.addItemListener(this);
    addRow(editPanel, targetC, "Target Object:", 16);

    symmetryCF = new checkboxField(null, false, ca, true);
    symmetryCF.setCallback(this);
    addRow(editPanel, symmetryCF, "Maintain Symmetry:", 17);

    fieldC = new Choice();
    fieldC.addItemListener(this);
    fieldC.addItem("<none>");
    addRow(editPanel, fieldC, "Target Field:", 18);

    booleanShowing = true;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;

    checkVisibility();

    add(editPanel);
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
    parent.add("1 " + row + " lhH", comp);
    if (visible)
      setRowVisible(comp, true);
    else
      setRowVisible(comp, false);
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

  void checkVisibility()
  //This goes through all the components, and sets the visibilities
  {
    setRowVisible(maxArrayN, vectorCF.getState());

    setRowVisible(targetC, targetLimitCF.getState());
    setRowVisible(symmetryCF, targetLimitCF.getState());
    
    setRowVisible(fieldC, symmetryCF.getState());

    //Now check the nameC choice stuff
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

    setRowVisible(OKCharS, stringShowing);
    setRowVisible(BadCharS, stringShowing);
    setRowVisible(minLengthN, stringShowing);
    setRowVisible(maxLengthN, stringShowing);
    setRowVisible(namespaceC, stringShowing);

    setRowVisible(targetLimitCF, referenceShowing);
    if (referenceShowing && targetLimitCF.getState())
      {
	setRowVisible(targetC, referenceShowing);
	
	setRowVisible(symmetryCF, referenceShowing);
	if (symmetryCF.getState())
	  setRowVisible(fieldC, referenceShowing);
	else
	  setRowVisible(fieldC, false);
      }
    else
      {
	setRowVisible(targetC, false);
	
	setRowVisible(symmetryCF, false);
	
	
      }
    editPanel.doLayout();
    mainPanel.invalidate();
    this.validate();
  }
    
  void changeTypeChoice(String selectedItem)
  {
    booleanShowing = false;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;
    if (selectedItem == "Boolean")
      booleanShowing = true;
    else if (selectedItem == "Numeric")
      numericShowing = true;
    else if (selectedItem == "Date")
      dateShowing = true;
    else if (selectedItem == "String")
      stringShowing = true;
    else if (selectedItem == "Object Reference")
      referenceShowing = true;
      

  }
  // edit the given field

  public void editField(BaseField fieldDef)
  {
    System.err.println("in FieldEditor.editField()");
    this.fieldDef = fieldDef;

    try
      {
	idN.setValue(fieldDef.getID());
	nameS.setText(fieldDef.getName());
	classS.setText(fieldDef.getClassName());
	commentT.setText(fieldDef.getComment());
        if (fieldDef.isArray())
	  {
	    vectorCF.setState(true);
	    //maxArrayN.setEnabled(true);
	  }
    
	else
	  {
	    vectorCF.setState(false);
	    //maxArrayN.setEnabled(false);
	  }
	if (fieldDef.isString())
	  {
	    minLengthN.setValue(fieldDef.getMinLength());
	    maxLengthN.setValue(fieldDef.getMaxLength());
	    OKCharS.setText(fieldDef.getOKChars());
	    BadCharS.setText(fieldDef.getBadChars());
	    namespaceC.removeAll();
	    namespaceC.add("<None>");
	    // add all defined namespaces here
	  }
	else if (fieldDef.isBoolean())
	  {
	    if (fieldDef.isLabeled())
	      {
		labeledCF.setState(true);
		trueLabelS.setText(fieldDef.getTrueLabel());
		//trueLabelS.setEnabled(true);
		falseLabelS.setText(fieldDef.getFalseLabel());
		//falseLabelS.setEnabled(true);
	      }

	    else
	      {
		labeledCF.setState(false);
		trueLabelS.setText("");
		//trueLabelS.setEnabled(false);
		falseLabelS.setText("");
		//falseLabelS.setEnabled(false);
	      }

	    
	    //card.show(cardPanel, "boolean");
	  }
	else if (fieldDef.isInvid())
	  {
	    if (fieldDef.isTargetRestricted())
	      {
		targetLimitCF.setState(true);
		//targetC.setEnabled(true);
		targetC.removeAll();
		// add object types

		//symmetryCF.setEnabled(true);

		if (fieldDef.isSymmetric())
		  {
		    symmetryCF.setState(true);
		    //fieldC.setEnabled(true);
		    fieldC.removeAll();
		    // add field types
		  }
		else
		  {
		    symmetryCF.setState(false);
		    //fieldC.setEnabled(false);
		    fieldC.removeAll();
		  }
	      }
	    else
	      {
		targetLimitCF.setState(false);
		//targetC.setEnabled(false);
		targetC.removeAll();
		//symmetryCF.setEnabled(false);
		//fieldC.setEnabled(false);
		fieldC.removeAll();
	      }

	    //card.show(cardPanel,"invid");
	  }
	else
	  {
	    //card.show(cardPanel,"empty");
	  }
      }
    catch (RemoteException ex)
      {
	System.err.println("remote exception in FieldEditor.editField: " + ex);
      }
  }

  // for string and numeric fields

  public boolean setValuePerformed(ValueObject v)
  {
    Component comp = v.getSource();

    try
      {
	if (comp == nameS)
	  {
	    System.out.println("nameS");
	    fieldDef.setName((String) v.getValue());
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
	else if (comp == typeC)
	  {
	    System.out.println("typeC");

	    String typename = (String) v.getValue();

	    if (typename.equals("Boolean"))
	      {
		fieldDef.setType(FieldType.BOOLEAN);
	      }
	    else if (typename.equals("Numeric"))
	      {
		fieldDef.setType(FieldType.NUMERIC);
	      }
	    else if (typename.equals("Date"))
	      {
		fieldDef.setType(FieldType.DATE);
	      }
	    else if (typename.equals("String"))
	      {
		fieldDef.setType(FieldType.STRING);
	      }
	    else if (typename.equals("Object Reference"))
	      {
		fieldDef.setType(FieldType.INVID);
	      }
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
	  }
	else if (comp == maxLengthN)
	  {
	    System.out.println("maxLengthN");
	  }
	else if (comp == namespaceC)
	  {
	    System.out.println("namespaceC");
	    fieldDef.setNameSpace((String) v.getValue());
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
	    checkVisibility();
	  }
	else if (comp == targetLimitCF)
	  {
	    System.out.println("targetLimitCF");
	    checkVisibility();
	  }
	else if (comp == symmetryCF)
	  {
	    System.out.println("symmetryCF");
	    checkVisibility();
	  }
	else if (comp == targetC)
	  {
	    System.out.println("targetC");
	  }
	else if (comp == fieldC)
	  { 
	    System.out.println("fieldC");
	  }
    
	return true;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception in setting field value " + ex);
      }
  }

  // for choice fields

  public void itemStateChanged(ItemEvent e)
  {
    //System.out.println("itemStateChanged");
    //System.out.println(e.getItem());
    if (e.getItemSelectable() == typeC)
      changeTypeChoice((String)e.getItem());
    else if (e.getItemSelectable() == namespaceC)
      System.out.println("Namespace: " + e.getItem());
    else if (e.getItemSelectable() == targetC)
      System.out.println("target: " + e.getItem());
    else if (e.getItemSelectable() == fieldC)
      System.out.println("field: " + e.getItem());
    checkVisibility();
  }

  // for the multiline comment field

  public void textValueChanged(TextEvent e)
  {
  }

  // for ok/cancel buttons

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == owner.attribOkButton)
      {
	owner.objectsRefresh();
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                        BaseNode

------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.Tree.treeNode {

  private Base base;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, PopupMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;
  }

  public Base getBase()
  {
    return base;
  }

  public void setBase(Base base)
  {
    this.base = base;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldNode

------------------------------------------------------------------------------*/

class FieldNode extends arlut.csd.Tree.treeNode {

  private BaseField field;

  /* -- */

  FieldNode(treeNode parent, String text, BaseField field, treeNode insertAfter,
	    boolean expandable, int openImage, int closedImage, PopupMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.field = field;
  }

  public BaseField getField()
  {
    return field;
  }

  public void setField(BaseField field)
  {
    this.field = field;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       SpaceNode

------------------------------------------------------------------------------*/

class SpaceNode extends arlut.csd.Tree.treeNode {

  private NameSpace space;

  /* -- */

  SpaceNode(treeNode parent, String text, NameSpace space, treeNode insertAfter,
	    boolean expandable, int openImage, int closedImage, PopupMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.space = space;
  }

  public NameSpace getSpace()
  {
    return space;
  }

  public void setSpace(NameSpace space)
  {
    this.space = space;
  }
}
