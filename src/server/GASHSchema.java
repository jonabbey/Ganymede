/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Version: $Revision: 1.31 $ %D%
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

import gjt.Box;
import gjt.Util;
import gjt.RowLayout;
import gjt.ColumnLayout;

import arlut.csd.Tree.*;


/*------------------------------------------------------------------------------
                                                                           class
                                                                      GASHSchema

------------------------------------------------------------------------------*/

public class GASHSchema extends Frame implements treeCallback, treeDragDropCallback, ActionListener {

  SchemaEdit 
    editor;

  Image
    images[];

  treeControl 
    tree;

  treeNode
    objects, namespaces;
  treeNode
    currentNode = null;

  MenuItem
    createObjectMI = null,
    deleteObjectMI = null,
    createNameMI = null,
    deleteNameMI = null,
    createFieldMI = null,
    deleteFieldMI = null;
  
  YesNoDialog 
    deleteFieldDialog = null,
    deleteNameDialog = null;

  PopupMenu
    baseMenu = null,
    fieldMenu = null,
    namespaceObjectMenu = null;

  CardLayout
    card;

  Panel 
    displayPane,
    buttonPane,
    attribPane,
    attribCardPane,
    emptyPane,
    baseEditPane,
    fieldEditPane,
    namespaceEditPane,
    attribButtonPane;

  BaseEditor
    be;

  BaseFieldEditor
    fe;

  NameSpaceEditor
    ne;
  
  boolean
    showingBase,
    showingField;

  Button
    okButton, cancelButton, attribOkButton;

  Toolkit
    toolkit;

  Base[] bases;

  Hashtable
    baseHash,
    fieldHash;

  Color
    bgColor = SystemColor.control;

  /* -- */

  public GASHSchema(String title, SchemaEdit editor)
  {
    super(title);

    this.editor = editor;
    
    setLayout(new BorderLayout());

    displayPane = new Panel();
    displayPane.setLayout(new BorderLayout());

    attribPane = new Panel();
    attribPane.setBackground(bgColor);
    attribPane.setLayout(new BorderLayout());

    card = new CardLayout();

    attribCardPane = new Panel();
    attribCardPane.setBackground(bgColor);
    attribCardPane.setLayout(card);

    baseEditPane = new Panel();
    baseEditPane.setBackground(bgColor);
    baseEditPane.setLayout(new BorderLayout());

    // initialize the base editor

    be = new BaseEditor(this);
    baseEditPane.add("Center", be);

    fieldEditPane = new Panel();
    fieldEditPane.setBackground(bgColor);
    fieldEditPane.setLayout(new BorderLayout());

    fe = new BaseFieldEditor(this);
    fieldEditPane.add("Center", fe);

    namespaceEditPane = new Panel();
    namespaceEditPane.setBackground(bgColor);
    namespaceEditPane.setLayout(new BorderLayout());

    ne = new NameSpaceEditor(this);
    namespaceEditPane.add("Center", ne);

    emptyPane = new Panel();
    emptyPane.setBackground(bgColor);

    attribCardPane.add("empty", emptyPane);
    attribCardPane.add("base", baseEditPane);
    attribCardPane.add("field", fieldEditPane);
    attribCardPane.add("name", namespaceEditPane);

    attribButtonPane = new Panel();
    attribButtonPane.setBackground(bgColor);
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
			   Color.black, SystemColor.window, this, images,
			   null);
    tree.setDrag(this, tree.DRAG_LINE);

    PopupMenu objectMenu = new PopupMenu();
    createObjectMI = new MenuItem("Create Object Type");
    objectMenu.add(createObjectMI);

    objects = new treeNode(null, "Object Types", null, true, 0, 1, objectMenu);
    tree.setRoot(objects);

    PopupMenu nameSpaceMenu = new PopupMenu("Namespace Menu");
    createNameMI = new MenuItem("Create Namespace");
    nameSpaceMenu.add(createNameMI);

    namespaces = new treeNode(null, "Namespaces", objects, true, 0, 1, nameSpaceMenu);
    tree.insertNode(namespaces, false);

    namespaceObjectMenu = new PopupMenu();
    deleteNameMI = new MenuItem("Delete Namespace");
    namespaceObjectMenu.add(deleteNameMI);

    deleteNameDialog = new YesNoDialog(this,
    				       "Confirm Name Space deletion",
    				       "Are you sure you want to delete the name space?",
    				       this);

    Box leftBox = new Box(tree, "Schema Objects");

    InsetPanel leftPanel = new InsetPanel(5, 10, 5, 5);
    leftPanel.setLayout(new BorderLayout());
    leftPanel.add("Center", leftBox);

    InsetPanel rightPanel = new InsetPanel(5, 5, 5, 10);
    rightPanel.setLayout(new BorderLayout());
    rightPanel.add("Center", rightBox);

    displayPane.add("West", leftPanel);

    displayPane.add("Center", rightPanel);

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

    deleteObjectMI = new MenuItem("Delete Object Type");
    createFieldMI = new MenuItem("Create Field");
    deleteFieldMI = new MenuItem("Delete Field");

    deleteFieldDialog = new YesNoDialog(this,
					"Confirm field deletion",
					"Are you sure you want to delete this field?",
					this);

    baseMenu = new PopupMenu("Base Menu");
    baseMenu.add(createFieldMI);
    baseMenu.add(deleteObjectMI);

    fieldMenu = new PopupMenu("Field Menu");
    fieldMenu.add(deleteFieldMI);

    fieldHash = new Hashtable();
    baseHash = new Hashtable();

    objectsRefresh();

    pack();
    setSize(800, 600);
    show();

    System.out.println("GASHSchema created");
  }


  /**
   *
   *
   *
   */

  public SchemaEdit getSchemaEdit()
    {
      if (editor == null)
	System.out.println("editor is null in GASHSchema");
      return editor;
    }

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
    int i;
    
    /* -- */

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

    (new QuickSort(bases,  
		   new arlut.csd.Util.Compare()
		   {
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
		   }
		   )).sort();

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

    while ((i < bases.length) || (bNode != null))
      {
	if (i < bases.length)
	  {
	    base = bases[i];
	  }
	else
	  {
	    base = null;
	  }

	if ((bNode == null) ||
	    ((base != null) &&
	     (base.getTypeID() < bNode.getBase().getTypeID())))
	  {
	    // insert a new base node 

	    newNode = new BaseNode(parentNode, base.getName(), base,
				   oldNode, true, 0, 1, baseMenu);

	    tree.insertNode(newNode, false);
	    baseHash.put(base, newNode);

	    refreshFields(newNode, false);

	    oldNode = newNode;
	    bNode = (BaseNode) oldNode.getNextSibling();

	    i++;
	  }
	else if ((base == null) || 
		 (base.getTypeID() > bNode.getBase().getTypeID()))
	  {
	    // delete a base node

	    if (showingBase && (be.base == bNode.getBase()))
	      {
		card.show(attribCardPane, "empty");
	      }

	    baseHash.remove(bNode.getBase());

	    // System.err.println("Deleting: " + bNode.getText());
	    newNode = (BaseNode) bNode.getNextSibling();
	    tree.deleteNode(bNode, false);

	    bNode = newNode;
	  }
	else
	  {
	    bNode.setText(base.getName());
	    //	    System.err.println("Setting: " + base.getName());
	    refreshFields(bNode, false);

	    oldNode = bNode;
	    bNode = (BaseNode) oldNode.getNextSibling();
	    
	    i++;
	  }
      }
  }

  void refreshFields(BaseNode node, boolean doRefresh) throws RemoteException
  {
    Base base;
    BaseField field, fields[];
    Vector vect;
    BaseNode parentNode;
    FieldNode oldNode, newNode, fNode;
    int i;

    /* -- */

    base = node.getBase();

    vect = base.getFields();

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
			     if (aF.getDisplayOrder() < bF.getDisplayOrder())
			       {
				 return -1;
			       }
			     else if (aF.getDisplayOrder() > bF.getDisplayOrder())
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
	
    while ((i < fields.length) || (fNode != null))
      {
	if (i < fields.length)
	  {
	    field = fields[i];
	  }
	else
	  {
	    field = null;
	  }

	if ((fNode == null) ||
	    ((field != null) && 
	     (field.getID() < fNode.getField().getID())))
	  {
	    // insert a new field node

	    newNode = new FieldNode(parentNode, field.getName(), field,
				    oldNode, false, 2, 2, fieldMenu);

	    tree.insertNode(newNode, false);
	    fieldHash.put(field, newNode);

	    oldNode = newNode;
	    fNode = (FieldNode) oldNode.getNextSibling();

	    i++;
	  }
	else if ((field == null) ||
		 (field.getID() > fNode.getField().getID()))
	  {
	    // delete a field node

	    if (showingField && (fe.fieldDef == fNode.getField()))
	      {
		card.show(attribCardPane, "empty");
	      }

	    fieldHash.remove(fNode.getField());

	    // System.err.println("Deleting: " + fNode.getText());
	    newNode = (FieldNode) fNode.getNextSibling();
	    tree.deleteNode(fNode, false);

	    fNode = newNode;
	  }
	else
	  {
	    fNode.setText(field.getName());
	    // System.err.println("Setting: " + field.getName());

	    oldNode = fNode;
	    fNode = (FieldNode) oldNode.getNextSibling();

	    i++;
	  }
      }

    if (doRefresh)
      {
	tree.refresh();
      }
  }

  void refreshFields(Base base, boolean doRefresh) throws RemoteException
  {
    refreshFields((BaseNode) baseHash.get(base), doRefresh);
  }

  void refreshNamespaces()
  { 
    boolean isOpen = namespaces.isOpen();
    
    tree.removeChildren(namespaces, false);

    NameSpace[] spaces = null;
    try 
      {
	spaces = editor.getNameSpaces();
      }
    catch (RemoteException e)
      {
	System.out.println("Exception getting NameSpaces: " + e);
      }
    
    for (int i = 0; i < spaces.length ; i++)
      {
	try 
	  {
	    SpaceNode newNode = new SpaceNode(namespaces, spaces[i].getName(), spaces[i], 
					      null, false, 2, 2, namespaceObjectMenu);
	    tree.insertNode(newNode, true);
	  }
	catch (RemoteException e)
	  {
	    System.out.println("Exception getting NameSpaces: " + e);
	  }

      }

    if (isOpen)
      tree.expandNode(namespaces, false);

    tree.refresh();
 }
  

  void editBase(Base base)
  {
    be.editBase(base);
    card.show(attribCardPane,"base");
    showingBase = true;
    showingField = false;

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
    showingBase = false;
    showingField = true;

    // attach the button pane to the field editor
    
    attribButtonPane.removeAll();
    attribOkButton = new Button("ok");
    attribOkButton.addActionListener(fe);
    attribButtonPane.add(attribOkButton);
    validate();
  }

  void editNameSpace(NameSpace space)
  {
    ne.editNameSpace(space);
    card.show(attribCardPane, "name");
    showingBase = false;
    showingField = false;

    attribButtonPane.removeAll();
    attribOkButton = new Button("ok");
    attribOkButton.addActionListener(ne);
    attribButtonPane.add(attribOkButton);
    validate();
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
    else if (node instanceof SpaceNode)
      {
	System.out.println("namespacenode selected");
	NameSpace nameSpace = ((SpaceNode) node).getSpace();
	editNameSpace(nameSpace);
      }
    else
      {
	card.show(attribCardPane, "empty");
      }
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
    if (!otherNode)
      {
	card.show(attribCardPane, "empty");
      }

    System.out.println("node " + node.getText() + " unselected");
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    String nodeText;

    currentNode = node;

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
	System.out.println("Create namespace chosen");
	DialogRsrc dialogResource = new DialogRsrc(this, "Create new namespace", "Create a new namespace", "Create", "Cancel");

	dialogResource.addString("Namespace:");
	dialogResource.addBoolean("Case Insensitive:");

	StringDialog dialog = new StringDialog(dialogResource);
	Hashtable results = dialog.DialogShow();

	String newNameSpace = null;
	Boolean insensitive = null;
	
	//Now check the hash

	if (results == null)
	  {
	    System.out.println("null hashtable, no action taken");
	  }
	else 
	  {
	    System.out.println("Printing the hash:");
	    Enumeration enum = results.keys();
	    while (enum.hasMoreElements()) 
	      {
		//String label = (String)enum.nextElement();
		String label = (String)enum.nextElement();
		Object ob = results.get(label);
		if (ob instanceof String) 
		  {
		    if (label == "Namespace:")
		      {
			System.out.println("Namespace is " + (String)ob);
			newNameSpace = (String)ob;
		      }
		    else
		      {
			System.out.println("Red alert!  unknown string returned: " + (String)ob);
		      }
			
		  }
		else if (ob instanceof Boolean)
		  {
		    Boolean bool = (Boolean)ob;
		    if (label == "Case Insensitive:")
		      {
			System.out.println("Sensitivity set to: " + bool);
			insensitive = bool;
		      }
		    else 
		      {
			System.out.println("Unknown Boolean returned by Dialog.");
		      }
		  }
		else 
		  {
		    System.out.println("Unknown type returned by Dialog.");
		  }
	      }
	    if ((newNameSpace != null) && (insensitive != null))
	      {
		try
		  {
		    System.out.println("Adding new NameSpace: " + newNameSpace);
		    editor.createNewNameSpace(newNameSpace, insensitive.booleanValue());
		  }
		catch (java.rmi.RemoteException e)
		  {
		    System.out.println("Exception while creating NameSpace: " + e);
		  }

	      }
	  }
	// List out the NameSpaces for testing

	NameSpace[] spaces  = null;

	System.out.println("Actual NameSpaces:");
	try
	  {
	    spaces = editor.getNameSpaces();
	  }
	catch (java.rmi.RemoteException e)
	  {
	    System.out.println("Exception while listing NameSpace: " + e);
	  }
	
	boolean Insensitive = false;
	String name = null;

	for (int i = 0; i < spaces.length ; i++ )
	  {
	    try
	      {
		Insensitive = spaces[i].isCaseInsensitive();
		name = spaces[i].getName();
	      }
	    catch (java.rmi.RemoteException e)
	      {
		
		System.out.println("Exception while listing NameSpace: " + e);
	      }
	    if (Insensitive)
	      {
		System.out.println("   " + name + " is case insensitive.");
	      }
	    else
	      {
		System.out.println("   " + name + " is not case insensitive.");
	      }
	    
	    
	  }
	refreshNamespaces();
	if (showingField)
	  {
	    fe.refreshFieldEdit();
	  }
	
      }
    else if (event.getSource() == deleteNameMI)
      {
	System.out.println("deleting Namespace");
	treeNode tNode = (treeNode)node;
		
	deleteNameDialog.setVisible(true);
      }
    else if (event.getSource() == deleteObjectMI)
      {
	BaseNode bNode = (BaseNode) node;
	Base b = bNode.getBase();

	try
	  {
	    System.err.println("Deleting base " + b.getName());
	    editor.deleteBase(b);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't delete base: remote exception " + ex);
	  }

	objectsRefresh();
      }
    else if (event.getSource() == createFieldMI)
      {
	// find the base that asked for the field

	try
	  {
	    BaseNode bNode = (BaseNode) node;
	    System.err.println("Calling editField");

	    // create a name for the new field

	    BaseField bF, bF2;
	    Base b;

	    String newname = "New Field";
	    int j;
	    boolean done;

	    b = bNode.getBase();
	    Vector fieldVect = b.getFields();

	    done = false;

	    j = 0;

	    while (!done)
	      {
		if (j > 0)
		  {
		    newname = "New Field " + (j + 1);
		  }

		done = true;

		for (int i = 0; done && i < fieldVect.size(); i++)
		  {
		    bF2 = (BaseField) fieldVect.elementAt(i);
		    
		    if (bF2.getName().equals(newname))
		      {
			done = false;
		      }
		  }

		j++;
	      }

	    bF = b.createNewField();
	    bF.setName(newname);
	    objectsRefresh();
	    editField(bF);
	    System.err.println("Called editField");
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new field" + ex);
	  }
      }
    else if (event.getSource() == deleteFieldMI)
      {
	System.err.println("deleting field node");

	deleteFieldDialog.setVisible(true);
	/*
	try
	  {
	    FieldNode fNode = (FieldNode) node;
	    BaseNode bNode = (BaseNode) node.getParent();
	    
	    if (!bNode.getBase().fieldInUse(fNode.getField()))
	      {
		bNode.getBase().deleteField(fNode.getField());
		refreshFields(bNode.getBase(), true);
		ne.refreshSpaceList();
		be.refreshLabelChoice();
	      }
	    else
	      {
		// field in use
		
		System.err.println("Couldn't delete field.. field in use");
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't delete field" + ex);
	  }
	
	  */
      }
  }

  // action handler

  public void actionPerformed(ActionEvent event)
  {
    System.out.println("event: " + event);

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
    else if (event.getSource() == deleteNameDialog)
      {
	if (deleteNameDialog.answeredYes())
	  {
	    if (currentNode == null)
	      {
		System.err.println("currentNode is null");
	      }
	    else
	      {
		treeNode tNode = (treeNode)currentNode;
		System.out.println("deleting Namespace " + tNode.getText());
		try
		  {
		    editor.deleteNameSpace(tNode.getText());
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("Couldn't delete namespace: remote exception " + ex);
		  }
		refreshNamespaces();
	
		if (showingField)
		  {
		    fe.refreshFieldEdit();
		  }
		
	      }
	  }
	else
	  {
	    System.out.println("Not deleting Name Space");
	  }
       

      }
    else if (event.getSource() == deleteFieldDialog)
      {
	if (deleteFieldDialog.answeredYes())
	  {


	    System.out.println("Answered yes");
	    if (currentNode == null)
	      {
		System.out.println("currentNode is null");
	      }
	    else
	      {
		try
		  {
		    FieldNode fNode = (FieldNode) currentNode;
		    BaseNode bNode = (BaseNode) currentNode.getParent();
		    
		    if (!bNode.getBase().fieldInUse(fNode.getField()))
		      {
			bNode.getBase().deleteField(fNode.getField());
			refreshFields(bNode.getBase(), true);
			ne.refreshSpaceList();
			be.refreshLabelChoice();
		      }
		    else
		      {
			// field in use
			
			System.err.println("Couldn't delete field.. field in use");
		      }
		  }
		catch (RemoteException ex)
		  {
		    System.err.println("couldn't delete field" + ex);
		  }
	      }
	  }
	else
	  {
	    System.out.println("Not deleting field");
	  }




      }
    else
      {
	System.err.println("Unknown Action Performed in GASHSchema");
      }
  }

  // **
  // The following methods comprise the implementation of arlut.csd.Tree.treeDragDropCallback,
  // and provide the intelligence behind the Schema Editor tree's drag and drop behavior.
  // **

  /**
   *
   * This method determines which nodes may be dragged.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public boolean startDrag(treeNode dragNode)
  {
    return (dragNode instanceof FieldNode);
  }

  /**
   *
   * This method doesn't truly apply to the drag and drop behavior implemented in
   * the Schema Editor.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public boolean iconDragOver(treeNode dragNode, treeNode targetNode)
  {
    return false;
  }

  /**
   *
   * This method doesn't truly apply to the drag and drop behavior implemented in
   * the Schema Editor.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public void iconDragDrop(treeNode dragNode, treeNode targetNode)
  {
  }

  /**
   *
   * Method to control whether the drag line may be moved between a pair of given
   * nodes.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public boolean dragLineTween(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
    treeNode parent = dragNode.getParent();
    return (((aboveNode instanceof FieldNode) && (aboveNode != null) && (aboveNode.getParent() == parent)) || 
	     ((belowNode instanceof FieldNode) && (belowNode != null) && (belowNode.getParent() == parent)));
  }

  /**
   *
   * This method is called when a drag and drop operation in the Schema Editor's tree is completed.
   *
   * @see arlut.csd.Tree.treeDragDropCallback
   */

  public void dragLineRelease(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
    System.out.println("dragNode = " + dragNode.getText());
    System.out.println("aboveNode = " + aboveNode.getText());
    System.out.println("belowNode = " + belowNode.getText());

    if (dragNode instanceof FieldNode)
      {
	FieldNode oldNode = (FieldNode)dragNode;
	BaseNode parentNode = (BaseNode)oldNode.getParent();
	System.out.println("parent = " + parentNode);
	
	if (aboveNode instanceof FieldNode)
	  {
	    if (aboveNode != dragNode)
	      {
		//Insert below the aboveNode
		FieldNode newNode = new FieldNode(parentNode, oldNode.getText(), oldNode.getField(),
						  aboveNode, false, 2, 2, fieldMenu);
		
		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);
	      }
	    else
	      {
		System.out.println("aboveNode == dragNode, Not moving it");
	      }
	    
	  }
	else if (belowNode instanceof FieldNode)
	  {
	    if (belowNode != dragNode)
	      {
		//First node, insert below parent
		FieldNode newNode = new FieldNode(parentNode, oldNode.getText(), oldNode.getField(),
						  null, false, 2, 2, fieldMenu);
		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);
	      }
	    else
	      {
		System.out.println("belowNode == dragNode, Not moving it");
	      }
	  }
	else
	  {
	    System.err.println("Dropped away from FieldNodes, shouldn't happen");
	  }
	
	//Ok, that mostly works, plugging ahead

	//Renumber the fields of this parent.
	
	FieldNode currentNode = (FieldNode)parentNode.getChild();
	if (currentNode != null)
	  {
	    try
	      {
		short i = 0;
		while (currentNode != null)
		  {
		    currentNode.getField().setDisplayOrder(++i);
		    currentNode = (FieldNode)currentNode.getNextSibling();
		  }
		System.out.println("Reordered " + i + " fields");
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception reordering fields: " + rx);
	      }
	  }
	else
	  {
	    System.err.println("No children to renumber, something not right");
	  }
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      BaseEditor

------------------------------------------------------------------------------*/

class BaseEditor extends ScrollPane implements setValueCallback, ActionListener, ItemListener {

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

  public void editBase(Base base)
  {
    this.base = base;

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
	    throw new IllegalArgumentException("exception setting label field: " + rx);
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

  void addRow(Panel parent, Component comp,  String label, int row)
  {
    Label l = new Label(label);
    
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);

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
    vectorCF,			// all
    labeledCF,			// boolean
    editInPlaceCF;		// invid

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
    
    mainPanel = new Panel();
    mainPanel.setLayout(new BorderLayout());
    
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
    addRow(editPanel, OKCharS, "Allowed Chars:", 9);

    BadCharS = new stringField(20, 100, ca, true, false, null, null);
    BadCharS.setCallback(this);
    addRow(editPanel, BadCharS, "Disallowed Chars:", 10);

    namespaceC = new Choice();
    namespaceC.addItemListener(this);

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

    editInPlaceCF = new checkboxField(null, false, ca, true);
    editInPlaceCF.setCallback(this);
    addRow(editPanel, editInPlaceCF, "Edit In Place:", 15);

    targetC = new Choice();
    targetC.addItemListener(this);
    addRow(editPanel, targetC, "Target Object:", 16);

    fieldC = new Choice();
    fieldC.addItemListener(this);
    addRow(editPanel, fieldC, "Target Field:", 17);

    booleanShowing = true;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;

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
    setRowVisible(maxArrayN, vectorCF.getState());

    // Now check the nameC choice stuff

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
      }
    else
      {
	setRowVisible(editInPlaceCF, false);
	setRowVisible(targetC, false);
	setRowVisible(fieldC, false);
      }

    editPanel.doLayout();
    mainPanel.invalidate();
    this.validate();
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
	baseList = owner.getSchemaEdit().getBases();
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
	targetBase = owner.getSchemaEdit().getBase(target);

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

  void editField(BaseField fieldDef)
  {
    System.err.println("in FieldEditor.editField()");

    //    if (fieldDef == this.fieldDef)
    //      {
    //	return;
    //      }

    clearFields();
    this.fieldDef = fieldDef;

    booleanShowing = false;
    numericShowing = false;
    dateShowing = false;
    stringShowing = false;
    referenceShowing = false;

    try
      {
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

		targetBase = se.getBase(targetB);

		if (targetBase == null)
		  {
		    throw new Error("targetbase is null when it shouldn't be: server error");
		  }

		String string = targetBase.getName();

		System.out.println("Choosing " + string);
		targetC.select(string);

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

	checkVisibility();
      }
    catch (RemoteException ex)
      {
	System.err.println("remote exception in FieldEditor.editField: " + ex);
      }
  }

  /**
   *
   * Reinitialize the BaseFieldEditor with the current field.
   *
   */

  public void refreshFieldEdit()
  {
    this.editField(fieldDef);
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

	    if ((currentFieldName != null) && currentLabel.equals(currentFieldName))
	      {
		DialogRsrc labelRsrc = new DialogRsrc(owner, 
						      "Warning: changing object type",
						      "Changing the type of this field will invalidate the label for this base.  Are you sure you want to continue?",
						      "Confirm",
						      "Cancel");

		changeLabelTypeDialog = new StringDialog(labelRsrc);

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
                                                                 NameSpaceEditor

------------------------------------------------------------------------------*/

class NameSpaceEditor extends ScrollPane implements ActionListener {
  
  stringField nameS;
  List spaceL;
  Checkbox caseCB;
  Panel namePanel;
  componentAttr ca;
  GASHSchema owner;
  String currentNameSpaceLabel = null;
  
  /* -- */

  NameSpaceEditor(GASHSchema owner)
    {

      if (owner == null)
	throw new IllegalArgumentException("owner must not be null");

      System.err.println("NameSpaceEditor constructed");

      this.owner = owner;

      namePanel = new InsetPanel(10,10,10,10);
      namePanel.setLayout(new TableLayout(false));

      ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			     Color.black, Color.white);
      
      nameS = new stringField(20, 100, ca, false, false, null, null);
      addRow(namePanel, nameS, "Namespace:", 0);
      
      caseCB = new Checkbox();
      caseCB.setEnabled(false);
      addRow(namePanel, caseCB, "Case insensitive:", 1);

      spaceL = new List(5);
      //spaceL.setEnabled(false);
      addRow(namePanel, spaceL, "Fields in this space:", 2);
            
      add(namePanel);
    }
  public void editNameSpace(NameSpace space)
    {
      try
	{
	  nameS.setText(space.getName());
	  caseCB.setState(space.isCaseInsensitive());
	  currentNameSpaceLabel = space.getName();
	  refreshSpaceList();
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Remote Exception gettin gNameSpace attributes " + rx);

	}
    }

  public void actionPerformed(ActionEvent e)
    {
      System.out.println("action Performed in NameSpaceEditor");
    }

  public void refreshSpaceList()
    {
      spaceL.removeAll();
      SchemaEdit se = owner.getSchemaEdit();
      Base[] bases = null;
      try
	{
	  bases = se.getBases();
	}
      catch (RemoteException rx)
	{
	  throw new IllegalArgumentException("Exception: can't get bases: " + rx);
	}
      Vector fields = null;
      BaseField currentField = null;
      String thisBase = null;
      String thisField = null;
      String thisSpace = null;
      if ((bases == null) || (currentNameSpaceLabel == null))
	{
	  System.out.println("bases or currentNameSpaceLabel is null");
	}
      else
	{
	  System.out.println("currentNameSpaceLabel= " + currentNameSpaceLabel);
	  
	  for (int i = 0; i < bases.length; i++)
	    {
	      try
		{
		  thisBase = bases[i].getName();
		  fields = bases[i].getFields();
		}
	      catch (RemoteException rx)
		{
		  throw new IllegalArgumentException("exception getting fields: " + rx);
		}
	      if (fields == null)
		{
		  System.out.println("fields == null");
		}
	      else
		{
		  for (int j = 0; j < fields.size(); j++)
		    {
		      try 
			{
			  currentField = (BaseField)fields.elementAt(j);
			  if (currentField.isString())
			    {
			      thisSpace = currentField.getNameSpaceLabel();
			      if ((thisSpace != null) && (thisSpace.equals(currentNameSpaceLabel)))
				{
				  System.out.println("Adding to spaceL: " + thisBase + ":" + currentField.getName());;
				  spaceL.addItem(thisBase + ":" + currentField.getName());
				  

				}
			      
			    }


			}
		      catch (RemoteException rx)
			{
			  throw new IllegalArgumentException("Exception generating spaceL: " + rx);
			}
		    }


		}
	      



	    }

	}
    }

  void addRow(Panel parent, Component comp,  String label, int row)
    {
      Label l = new Label(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
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
