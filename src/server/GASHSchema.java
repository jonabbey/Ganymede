/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Release: $Name:  $
   Version: $Revision: 1.93 $
   Last Mod Date: $Date: 2001/11/17 00:10:40 $
   Module By: Jonathan Abbey and Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System

   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;
import arlut.csd.JDialog.JInsetPanel;
import arlut.csd.JTree.*;

import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GASHSchema

------------------------------------------------------------------------------*/

/**
 * <P>GUI Schema Editor, part of the Ganymede admin console.</P>
 *
 * <P>GASHSchema talks to the server by way of the
 * {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} remote interface.</P>
 */

public class GASHSchema extends JFrame implements treeCallback, treeDragDropCallback, ActionListener {

  public static final boolean debug = false;

  // --

  JMenuItem schemaMI;

  SchemaEdit 
    editor;

  java.awt.Image
    questionImage,
    treeImages[];

  treeControl 
    tree;

  Category
    rootCategory;

  CatTreeNode
    objects;			// root category node

  treeNode
    nodeAfterCategories,
    namespaces;			// top-level node for namespace listing

  javax.swing.JMenuItem
    createCategoryMI = null,
    deleteCategoryMI = null,
    createObjectMI = null,
    createInternalObjectMI = null,
    deleteObjectMI = null,
    createNameMI = null,
    deleteNameMI = null,
    createFieldMI = null,
    deleteFieldMI = null;
  
  treeMenu
    categoryMenu = null,
    baseMenu = null,
    fieldMenu = null,
    nameSpaceMenu = null,
    nameSpaceObjectMenu = null;

  java.awt.CardLayout
    card;

  JPanel 
    buttonPane,
    attribPane,
    attribCardPane,
    emptyPane,
    categoryEditPane;

  JScrollPane
    fieldEditPane,
    namespaceEditPane,
    baseEditPane;

  BaseEditor
    be;

  BaseFieldEditor
    fe;

  NameSpaceEditor
    ne;

  CategoryEditor
    ce;
  
  boolean
    showingBase,
    showingField;

  JButton
    okButton, cancelButton;

  java.awt.Color
    bgColor = java.awt.SystemColor.control;

  public EmptyBorder
    emptyBorder5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5),
    emptyBorder10 = (EmptyBorder)BorderFactory.createEmptyBorder(10,10,10,10);

  public BevelBorder
    raisedBorder = (BevelBorder)BorderFactory.createBevelBorder(BevelBorder.RAISED),
    loweredBorder = (BevelBorder)BorderFactory.createBevelBorder(BevelBorder.LOWERED);
      
  public LineBorder
    lineBorder = (LineBorder)BorderFactory.createLineBorder(Color.black);

  public CompoundBorder
    statusBorder = BorderFactory.createCompoundBorder(loweredBorder, emptyBorder5),
    statusBorderRaised = BorderFactory.createCompoundBorder(raisedBorder, emptyBorder5);

  /* -- */

  public GASHSchema(String title, SchemaEdit editor, JMenuItem schemaMI)
  {
    super(title);

    this.schemaMI = schemaMI;
    this.editor = editor;

    questionImage = PackageResources.getImageResource(this, "question.gif", getClass());

    //
    //
    //   **** Set up panels
    //
    //
    
    getContentPane().setLayout(new java.awt.BorderLayout());

    attribPane = new JPanel();
    //attribPane.setBackground(bgColor);
    attribPane.setLayout(new java.awt.BorderLayout());

    card = new java.awt.CardLayout();

    attribCardPane = new JPanel();
    //    attribCardPane.setBackground(bgColor);
    attribCardPane.setLayout(card);

    // set up the base editor

    baseEditPane = new JScrollPane();
    //    baseEditPane.setBackground(bgColor);

    be = new BaseEditor(this);
    baseEditPane.setViewportView(be);

    // set up the base field editor

    fieldEditPane = new JScrollPane();
    //    fieldEditPane.setBackground(bgColor);

    fe = new BaseFieldEditor(this);
    fieldEditPane.setViewportView(fe);

    // set up the name space editor

    namespaceEditPane = new JScrollPane();
    //    namespaceEditPane.setBackground(bgColor);

    ne = new NameSpaceEditor(this);
    namespaceEditPane.setViewportView(ne);

    // set up the category editor

    categoryEditPane = new JPanel();
    //    categoryEditPane.setBackground(bgColor);
    categoryEditPane.setLayout(new java.awt.BorderLayout());

    ce = new CategoryEditor(this);
    categoryEditPane.add("Center", ce);

    // set up the empty card

    emptyPane = new JPanel();
    //    emptyPane.setBackground(bgColor);

    // Finish attribPane setup

    attribCardPane.add("empty", emptyPane);
    attribCardPane.add("base", baseEditPane);
    attribCardPane.add("field", fieldEditPane);
    attribCardPane.add("name", namespaceEditPane);
    attribCardPane.add("category", categoryEditPane);

    attribPane.add("Center", attribCardPane);

    JPanel rightJPanel = new JPanel();

    JPanel rightTop = new JPanel(false);
    rightTop.setBorder(statusBorderRaised);
    JLabel rightL = new JLabel("Attributes");
    rightTop.setLayout(new BorderLayout());
    rightTop.add("Center", rightL);
    
    rightJPanel.setLayout(new java.awt.BorderLayout());
    rightJPanel.add("Center", attribPane);
    rightJPanel.add("North", rightTop);

    // Set up button pane

    buttonPane = new JPanel();



    okButton = new JButton("ok");
    okButton.addActionListener(this);

    cancelButton = new JButton("cancel");
    cancelButton.addActionListener(this);

    buttonPane.add(okButton);
    buttonPane.add(cancelButton);

    buttonPane.setBorder(loweredBorder);

    getContentPane().add("South", buttonPane);

    //
    //
    //   **** Set up display tree
    //
    //

    treeImages = new java.awt.Image[5];

    treeImages[0] = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    treeImages[1] = PackageResources.getImageResource(this, "folder.gif", getClass());
    treeImages[2] = PackageResources.getImageResource(this, "list.gif", getClass());
    treeImages[3] = PackageResources.getImageResource(this, "i043.gif", getClass());
    treeImages[4] = PackageResources.getImageResource(this, "transredlist.gif", getClass());

    tree = new treeControl(new java.awt.Font("SansSerif",java.awt.Font.BOLD, 12),
			   java.awt.Color.black, java.awt.Color.white, this, treeImages,
			   null);
    tree.setMinimumWidth(200);
    tree.setDrag(this, tree.DRAG_LINE | tree.DRAG_ICON);

    //
    //
    //   **** Set up display tree panels
    //
    //

    JPanel leftJPanel = new JPanel();
    leftJPanel.setLayout(new java.awt.BorderLayout());

    JPanel leftTop = new JPanel(false);
    leftTop.setBorder(statusBorderRaised);
    JLabel leftL = new JLabel("Schema Objects");
    leftTop.setLayout(new BorderLayout());

    leftTop.add("Center", leftL);

    leftJPanel.add("North", leftTop);
    leftJPanel.add("Center", tree);

    //    displayPane.add("West", leftJPanel);

    //    add("Center", displayPane);

    JSplitPane sPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftJPanel, rightJPanel);

    getContentPane().add("Center", sPane);

    //
    //
    //   **** Set up tree popup menus
    //
    //

    // category menu

    categoryMenu = new treeMenu();

    createCategoryMI = new javax.swing.JMenuItem("Create Category");
    deleteCategoryMI = new javax.swing.JMenuItem("Delete Category");
    createObjectMI = new javax.swing.JMenuItem("Create Object Type");
    createInternalObjectMI = new javax.swing.JMenuItem("Create Embedded Object Type");

    categoryMenu.add(createCategoryMI);
    categoryMenu.add(deleteCategoryMI);
    categoryMenu.add(createObjectMI);
    categoryMenu.add(createInternalObjectMI);

    // namespace menu

    nameSpaceMenu = new treeMenu("Namespace Menu");
    createNameMI = new javax.swing.JMenuItem("Create Namespace");
    nameSpaceMenu.add(createNameMI);

    // namespace object menu

    nameSpaceObjectMenu = new treeMenu();
    deleteNameMI = new javax.swing.JMenuItem("Delete Namespace");
    nameSpaceObjectMenu.add(deleteNameMI);

    // base menu

    baseMenu = new treeMenu("Base Menu");
    deleteObjectMI = new javax.swing.JMenuItem("Delete Object Type");
    createFieldMI = new javax.swing.JMenuItem("Create Field");

    baseMenu.add(createFieldMI);

    baseMenu.add(deleteObjectMI);

    // field menu

    fieldMenu = new treeMenu("Field Menu");
    deleteFieldMI = new javax.swing.JMenuItem("Delete Field");
    fieldMenu.add(deleteFieldMI);

    //
    //
    //   **** Set up tree 
    //
    //

    try
      {
	rootCategory = editor.getRootCategory();

	objects = new CatTreeNode(null, rootCategory.getName(), rootCategory,
				  null, true, 0, 1, categoryMenu);

	if (debug)
	  {
	    System.err.println("Created rootCategory node: " + rootCategory.getName());
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't get rootCategory " + ex);
      }

    tree.setRoot(objects);

    // create namespaces node
	
    namespaces = new treeNode(null, "Namespaces", objects, true, 0, 1, nameSpaceMenu);
    tree.insertNode(namespaces, false);
    
    nodeAfterCategories = namespaces;

    // and initialize tree

    initializeDisplayTree();

    pack();
    setSize(800, 600);
    show();

    if (debug)
      {
	System.out.println("GASHSchema created");
      }
  }

  /**
   *
   */

  public SchemaEdit getSchemaEdit()
    {
      if (editor == null)
	{
	  System.out.println("editor is null in GASHSchema");
	}

      return editor;
    }

  void initializeDisplayTree()
  {
    try
      {
	recurseDownCategories(objects);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't refresh categories" + ex);
      }

    refreshNamespaces();
  }

  void recurseDownCategories(CatTreeNode node) throws RemoteException
  {
    Vector
      children;

    Category 
      c;

    CategoryNode
      cNode;

    treeNode 
      thisNode,
      prevNode;

    /* -- */

    c = node.getCategory();

    node.setText(c.getName());

    // get this category's children

    children = c.getNodes();

    prevNode = null;
    thisNode = node.getChild();

    for (int i = 0; i < children.size(); i++)
      {
	// find the CategoryNode at this point in the server's category tree

	cNode = (CategoryNode) children.elementAt(i);

	prevNode = insertCategoryNode(cNode, prevNode, node);
	
	if (prevNode instanceof CatTreeNode)
	  {
	    recurseDownCategories((CatTreeNode) prevNode);
	  }
      }
  }

  /**
   *
   * Local helper method to place a new CategoryNode (either a Base or a Category)
   * into the schema editor's display tree.
   *   
   */

  treeNode insertCategoryNode(CategoryNode node, treeNode prevNode, treeNode parentNode) throws RemoteException
  {
    treeNode newNode = null;

    /* -- */

    if (node instanceof Base)
      {
	Base base = (Base) node;

	if (base.isEmbedded())
	  {
	    newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
				   false, 4, 4, baseMenu);
	  }
	else
	  {
	    newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
				   false, 2, 2, baseMenu);
	  }
      }
    else if (node instanceof Category)
      {
	Category category = (Category) node;

	newNode = new CatTreeNode(parentNode, category.getName(), category,
				  prevNode, true, 0, 1, categoryMenu);
      }

    tree.insertNode(newNode, true);

    if (newNode instanceof BaseNode)
      {
	refreshFields((BaseNode)newNode, false);
      }

    return newNode;
  }

  /**
   *
   * This method generates the per-field children of the specified baseNode.
   *
   */

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

    // get the list of fields we want to display
    // note that we don't want to show built-in fields

    vect = base.getFields(false);

    // we copy the fields into a local array for historical reasons,
    // plus laziness..  no good reason to do this anymore, but no
    // pressing reason to mess with it, either

    fields = new BaseField[vect.size()];
    
    for (i = 0; i < fields.length; i++)
      {
	fields[i] = (BaseField) vect.elementAt(i);
      }

    parentNode = node;
    oldNode = null;
    fNode = (FieldNode) node.getChild();
    i = 0;

    // this loop here is intended to do a minimum-work updating of a
    // field list, using a ratcheting algorithm similar to that used
    // in gclient.refreshObjects().
	
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
				    oldNode, false, 3, 3, fieldMenu);

	    tree.insertNode(newNode, true);

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
					      null, false, 2, 2, nameSpaceObjectMenu);
	    tree.insertNode(newNode, true);
	  }
	catch (RemoteException e)
	  {
	    System.out.println("Exception getting NameSpaces: " + e);
	  }

      }

    if (isOpen)
      {
	tree.expandNode(namespaces, false);
      }

    tree.refresh();
  }
  

  void editBase(BaseNode node)
  {
    be.editBase(node);
    card.show(attribCardPane,"base");
    showingBase = true;
    showingField = false;

    fe.fieldNode = null;
    
    validate();
  }

  void editField(FieldNode node)
  {
    if (debug)
      {
	System.err.println("in GASHSchema.editField");
      }

    fe.editField(node, false);
    card.show(attribCardPane, "field");
    showingBase = false;
    showingField = true;

    // if we switch back to the base editor, it'll need to know that
    // it needs to refresh

    be.baseNode = null;
    
    validate();
  }

  void editNameSpace(SpaceNode node)
  {
    ne.editNameSpace(node);
    card.show(attribCardPane, "name");
    showingBase = false;
    showingField = false;

    // if we switch back to the field editor
    // or base editor, they need to know that they'll
    // need to refresh

    fe.fieldNode = null;
    be.baseNode = null;

    validate();
  }

  void editCategory(CatTreeNode node)
  {
    ce.editCategory(node);
    card.show(attribCardPane, "category");

    showingBase = false;
    showingField = false;

    // if we switch back to the field editor
    // or base editor, they need to know that they'll
    // need to refresh

    fe.fieldNode = null;
    be.baseNode = null;

    validate();
  }

  public void setWaitCursor()
  {
    this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
  }

  public void setNormalCursor()
  {
    this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
  }


  // treeCallback methods

  public void treeNodeExpanded(treeNode node)
  {
    return;
  }

  public void treeNodeContracted(treeNode node)
  {
    return;
  }

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
	editBase((BaseNode) node);
      }
    else if (node instanceof FieldNode)
      {
	editField((FieldNode) node);
      }
    else if (node instanceof SpaceNode)
      {
	if (debug)
	  {
	    System.out.println("namespacenode selected");
	  }

	editNameSpace((SpaceNode) node);
      }
    else if (node instanceof CatTreeNode)
      {
	editCategory((CatTreeNode) node);
      }
    else
      {
	card.show(attribCardPane, "empty");

	// if we switch back to the field editor
	// or base editor, they need to know that they'll
	// need to refresh

	fe.fieldNode = null;
	be.baseNode = null;
      }
  }

  public void treeNodeUnSelected(treeNode node, boolean otherNode)
  {
    if (!otherNode)
      {
	card.show(attribCardPane, "empty");
      }

    if (debug)
      {
	System.out.println("node " + node.getText() + " unselected");
      }
  }

  /**
   *
   * Called when an item in the tree is double-clicked.
   *
   * @param node The node selected in the tree.
   *
   * @see arlut.csd.JTree.treeCallback
   */

  public void treeNodeDoubleClicked(treeNode node)
  {
    return;
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    String nodeText;

    /* -- */

    nodeText = node.getText();

    if (debug)
      {
	System.out.println("node " + nodeText + ", action: " + event );
      }

    if (event.getSource() == createCategoryMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();

	    Category newCategory = category.newSubCategory();

	    // we want to insert at the bottom of the base

	    treeNode n = node.getChild();

	    if (n != null)
	      {
		while (n.getNextSibling() != null)
		  {
		    n = n.getNextSibling();
		  }
	      }
	    
	    CatTreeNode newNode = new CatTreeNode(node, newCategory.getName(), newCategory,
						  n, true, 0, 1, categoryMenu);

	    tree.insertNode(newNode, false);
	    
	    tree.expandNode(node, true);

	    editCategory(newNode);
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new base." + ex);
	  }
      }
    else if (event.getSource() == deleteCategoryMI)
      {
	CatTreeNode cNode = (CatTreeNode) node;
	Category category = cNode.getCategory();
	DialogRsrc dialogResource;

	if (node == objects)
	  {
	    new StringDialog(this,
			     "Error:  Category not removable",
			     "You are not allowed to remove the root category node.",
			     "Ok",
			     null).DialogShow();
	    return;
	  }

	try
	  {
	    if (category.getNodes().size() != 0)
	      {
		new StringDialog(this,
				 "Error:  Category not removable",
				 "This category has nodes under it.  You must remove the contents before deleting this category.",
				 "Ok",
				 null).DialogShow();
		return;
	      }

	    dialogResource = new DialogRsrc(this, 
					    "Delete category", 
					    "Are you sure you want to delete category " + category.getPath() + "?",
					    "Delete", "Cancel", 
					    questionImage);

	    Hashtable results = new StringDialog(dialogResource).DialogShow();

	    if (results != null)
	      {
		Category parent = category.getCategory();
		parent.removeNode(category.getName());
		tree.deleteNode(node, true);
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote " + ex);
	  }
      }
    else if (event.getSource() == createObjectMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();
	    Base newBase = editor.createNewBase(category, false, false);
	    
	    BaseNode newNode = new BaseNode(node, newBase.getName(), newBase,
					    null, false, 2, 2, baseMenu);

	    tree.insertNode(newNode, false);

	    tree.expandNode(node, false);

	    refreshFields(newNode, true);

	    editBase(newNode);
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new base." + ex);
	  }
      }
    else if (event.getSource() == createInternalObjectMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();
	    Base newBase = editor.createNewBase(category, true, false);
	    
	    BaseNode newNode = new BaseNode(node, newBase.getName(), newBase,
					    null, false, 4, 4, baseMenu);

	    tree.insertNode(newNode, false);

	    tree.expandNode(node, false);

	    refreshFields(newNode, true);

	    editBase(newNode);
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new base." + ex);
	  }
      }
    else if (event.getSource() == createNameMI)
      {
	if (debug)
	  {
	    System.out.println("Create namespace chosen");
	  }

	DialogRsrc dialogResource = new DialogRsrc(this, 
						   "Create new namespace", 
						   "Create a new namespace", 
						   "Create", "Cancel", 
						   questionImage);

	dialogResource.addString("Namespace:");
	dialogResource.addBoolean("Case Insensitive:");

	Hashtable results = new StringDialog(dialogResource).DialogShow();

	String newNameSpace = null;
	Boolean insensitive = null;
	
	// Now check the hash

	if (results == null)
	  {
	    if (debug)
	      {
		System.out.println("null hashtable, no action taken");
	      }
	  }
	else 
	  {
	    if (debug)
	      {
		System.out.println("Printing the hash:");
	      }

	    Enumeration enum = results.keys();

	    while (enum.hasMoreElements()) 
	      {
		String label = (String) enum.nextElement();
		Object ob = results.get(label);

		if (ob instanceof String) 
		  {
		    if (label == "Namespace:")
		      {
			if (debug)
			  {
			    System.out.println("Namespace is " + (String)ob);
			  }

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
			if (debug)
			  {
			    System.out.println("Sensitivity set to: " + bool);
			  }

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
		    if (debug)
		      {
			System.out.println("Adding new NameSpace: " + newNameSpace);
		      }
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

	if (debug)
	  {
	    System.out.println("Actual NameSpaces:");
	  }

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

	    if (debug)
	      {
		if (Insensitive)
		  {
		    System.out.println("   " + name + " is case insensitive.");
		  }
		else
		  {
		    System.out.println("   " + name + " is not case insensitive.");
		  }
	      }
	  }

	refreshNamespaces();

	if (showingField)
	  {
	    fe.refreshFieldEdit(true);
	  }
	
      }
    else if (event.getSource() == deleteNameMI)
      {
	if (debug)
	  {
	    System.out.println("deleting Namespace");
	  }

	treeNode tNode = (treeNode)node;

	DialogRsrc dialogResource = new DialogRsrc(this,
						   "Confirm Name Space Deletion",
						   "Confirm Name Space Deletion",
						   "Delete", "Cancel",
						   questionImage);

	Hashtable results = new StringDialog(dialogResource).DialogShow();

	if (results != null)
	  {
	    try
	      {
		editor.deleteNameSpace(node.getText());
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Couldn't delete namespace: remote exception " + ex);
	      }

	    refreshNamespaces();

	    if (showingField)
	      {
		fe.refreshFieldEdit(true);
	      }
	  }
      }
    else if (event.getSource() == deleteObjectMI)
      {
	BaseNode bNode = (BaseNode) node;
	Base b = bNode.getBase();

	// Check to see if this base is removable.  If it's not, then politely
	// inform the user.  Otherwise, pop up a dialog to make them confirm 
	// the deletion.

	boolean isRemovable = false;

	try 
	  {
	    isRemovable = b.isRemovable();
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("exception in isRemovalbe(): " + rx);
	  }

	if (isRemovable)
	  {
	    if (new StringDialog(this,
				 "Confirm deletion of Object",
				 "Are you sure you want to delete this object?",
				 "Confirm",
				 "Cancel").DialogShow() == null)
	      {
		if (debug)
		  {
		    System.out.println("Deletion canceled");
		  }
	      }
	    else //Returned confirm
	      {	    
		try
		  {
		    if (debug)
		      {
			System.err.println("Deleting base " + b.getName());
		      }

		    editor.deleteBase(b.getName());
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("Couldn't delete base: remote exception " + ex);
		  }
		
		tree.deleteNode(node, true);
	      }
	  }
	else
	  {
	    new StringDialog(this,
			     "Error:  Base not removable",
			     "You are not allowed to remove this base.",
			     "Ok",
			     null).DialogShow();
	  }
      }
    else if (event.getSource() == createFieldMI)
      {
	// find the base that asked for the field

	try
	  {
	    BaseNode bNode = (BaseNode) node;

	    if (debug)
	      {
		System.err.println("Calling editField");
	      }

	    // create a name for the new field

	    BaseField bF;
	    Base b = bNode.getBase();

	    bF = b.createNewField(); // the server picks a new default field name

	    String name = bF.getName();

	    // we want to insert the child's field node at the bottom
	    // of the base, which is where createNewField() places it.

	    treeNode n = node.getChild();
	    
	    if (n != null)
	      {
		while (n.getNextSibling() != null)
		  {
		    n = n.getNextSibling();
		  }
	      }

	    FieldNode newNode = new FieldNode(node, name, bF, n,
					      false, 3, 3, fieldMenu);
	    tree.insertNode(newNode, false);
	    tree.expandNode(node, true);

	    editField(newNode);

	    if (debug)
	      {
		System.err.println("Called editField");
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new field" + ex);
	  }
      }
    else if (event.getSource() == deleteFieldMI)
      {
	FieldNode fNode = (FieldNode) node;
	BaseField field = fNode.getField();

	String label = fNode.getText();
	String parentLabel = fNode.getParent().getText();

	DialogRsrc dialogResource = new DialogRsrc(this,
						   "Confirm Field Deletion",
						   "Ok to delete field " + label + 
						   "from object type " + parentLabel + "?",
						   "Delete", "Cancel",
						   questionImage);

	Hashtable results = new StringDialog(dialogResource).DialogShow();

	if (results != null)
	  {
	    BaseNode bNode = (BaseNode) node.getParent();
		
	    try
	      {
		ReturnVal retVal = bNode.getBase().deleteField(fNode.getText());
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    handleReturnVal(retVal);
		    return;
		  }
		
		refreshFields(bNode, true);
		ne.refreshSpaceList();
		be.refreshLabelChoice();
	      }
	    catch (RemoteException ex)
	      {
		new JErrorDialog(this,
				 "Couldn't delete field",
				 "Caught an exception from the server trying to delete field.. " + ex);
	      }
	  }
      }
  }

  // action handler

  public void actionPerformed(ActionEvent event)
  {
    if (debug)
      {
	System.out.println("event: " + event);
      }

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

	editor = null;

	schemaMI.setEnabled(true);
	setVisible(false);

	// speed up GC a little bit

	cleanup();
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

	editor = null;

	schemaMI.setEnabled(true);
	setVisible(false);

	// speed up GC a little bit

	cleanup();
      }
    else
      {
	System.err.println("Unknown Action Performed in GASHSchema");
      }
  }

  /**
   * <p>Make sure that we clean up and get rid of our
   * remote references to the server's schema editing
   * objects if our window is closed on us.</p>
   */

  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSED)
      {
	super.processWindowEvent(e);
	cleanup();
      }
    else
      {
	super.processWindowEvent(e);
      }
  }

  /**
   * <p>GC-aiding dissolution method.  Should be called after the
   * schema editor window has been removed from view, on the GUI
   * thread.</p>
   */

  private void cleanup()
  {
    if (this.editor != null)
      {
	try
	  {
	    this.editor.release();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't release: " + ex);
	  }

	this.editor = null;
      }

    if (this.attribCardPane != null)
      {
	this.attribCardPane.removeAll();
	this.attribCardPane = null;
      }

    if (this.be != null)
      {
	this.be.cleanup();
	this.be = null;
      }

    if (this.fe != null)
      {
	this.fe.cleanup();
	this.fe = null;
      }

    if (this.ne != null)
      {
	this.ne.cleanup();
	this.ne = null;
      }
    
    if (this.ce != null)
      {
	this.ce.cleanup();
	this.ce = null;
      }

    this.rootCategory = null;
    this.objects = null;
    this.tree = null;
    this.schemaMI = null;

    this.removeAll();		// should be done on GUI thread
  }

  // **
  // The following methods comprise the implementation of arlut.csd.JTree.treeDragDropCallback,
  // and provide the intelligence behind the Schema Editor tree's drag and drop behavior.
  // **

  /**
   *
   * This method determines which nodes may be dragged.
   *
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public boolean startDrag(treeNode dragNode)
  {
    return ((dragNode instanceof FieldNode) ||
	    (dragNode instanceof BaseNode) ||
	    (dragNode instanceof CatTreeNode &&
	     dragNode != objects));
  }

  /**
   *
   * This method provides intelligence to the tree, determining which
   * nodes the dragNode may be dropped on.
   *
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public boolean iconDragOver(treeNode dragNode, treeNode targetNode)
  {
    if (targetNode.isOpen())
      {
	if (debug)
	  {
	    System.err.println("iconDragOver(): failing " + dragNode.getText() + 
			       "over " + targetNode.getText() + " because node is open.");
	  }

	return false;
      }

    if (dragNode instanceof FieldNode)
      {
	if (debug)
	  {
	    System.err.println("iconDragOver(): failing " + dragNode.getText() + 
			       "over " + targetNode.getText() + " because can't drag over field nodes");
	  }

	return false;
      }

    if (dragNode instanceof BaseNode)
      {
	boolean success = (targetNode instanceof CatTreeNode);

	if (debug)
	  {
	    if (success)
	      {
		System.err.println("iconDragOver(): succeeding base " + dragNode.getText() + 
				   " over category " + targetNode.getText());
	      }
	    else
	      {
		System.err.println("iconDragOver(): failing base " + dragNode.getText() + 
				   " over non-category " + targetNode.getText());
	      }
	  }

	return success;
      }

    if (dragNode instanceof CatTreeNode)
      {
	if (!(targetNode instanceof CatTreeNode))
	  {
	    if (debug)
	      {
		System.err.println("iconDragOver(): failing " + dragNode.getText() + 
				   "over " + targetNode.getText() + " because can't drag category over non-category");
	      }

	    return false;
	  }
	
	CatTreeNode cNode = (CatTreeNode) dragNode;
	CatTreeNode cNode1 = (CatTreeNode) targetNode;

	try
	  {
	    if (!cNode1.getCategory().isUnder(cNode.getCategory()))
	      {
		if (debug)
		  {
		    System.err.println("iconDragOver(): succeeding " + dragNode.getText() + 
				       " over " + targetNode.getText());
		  }

		return true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("iconDragOver(): failing " + dragNode.getText() + 
				       " over " + targetNode.getText() +
				       " because move category into its own subcategory");
		  }

		return false;
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    System.err.println("iconDragOver(): failing " + dragNode.getText() + 
		       "over " + targetNode.getText() + ", don't recognize the drag node type");
    
    return false;
  }

  /**
   *
   * This method provides intelligence to the tree, determining what
   * action is to be taken if a node is dropped on another node.
   *
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public void iconDragDrop(treeNode dragNode, treeNode targetNode)
  {
    if (debug)
      {
	System.err.println("Dropping node " + dragNode.getText() + " on " + targetNode.getText());
      }

    if (dragNode instanceof BaseNode)
      {
	try
	  {
	    BaseNode bn = (BaseNode) dragNode;
	    Base base = bn.getBase();
	    Category oldCategory = base.getCategory();

	    Category newCategory = null;

	    if (targetNode instanceof CatTreeNode)
	      {
		// it had better be

		newCategory = ((CatTreeNode) targetNode).getCategory();

		if (debug)
		  {
		    System.err.println("Dropping base " + base.getName() + " from " +
				       oldCategory.getName() + " onto " + newCategory.getName());
		  }

		if (debug)
		  {
		    System.err.println("Removing " + base.getName() + " from " + oldCategory.getName());
		  }

		oldCategory.removeNode(base.getName());

		if (debug)
		  {
		    System.err.println("Adding " + base.getName() + " to " + newCategory.getName());
		  }

		newCategory.addNodeBefore(base, null);

		BaseNode newNode = (BaseNode) tree.moveNode(dragNode, targetNode, null, true);

		refreshFields(newNode, true);

		if (be.baseNode == dragNode)
		  {
		    be.baseNode = newNode;
		  }
	      }
	    else
	      {
		throw new RuntimeException("what?  dropped on a non-category node..");
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }
    else if (dragNode instanceof CatTreeNode)
      {
	try
	  {
	    CatTreeNode cn = (CatTreeNode) dragNode;
	    Category category = cn.getCategory();
	    Category oldCategory = category.getCategory();
	    
	    Category newCategory = null;

	    if (targetNode instanceof CatTreeNode)
	      {
		// it had better be

		newCategory = ((CatTreeNode) targetNode).getCategory();

		if (debug)
		  {
		    System.err.println("Dropping category " + category.getName() + " from " +
				       oldCategory.getName() + " onto " + newCategory.getName());
		  }

		newCategory.moveCategoryNode(category.getPath(), null);

		CatTreeNode newNode = (CatTreeNode) tree.moveNode(dragNode, targetNode, null, true);

		if (ce.catNode == dragNode)
		  {
		    ce.catNode = newNode;
		  }
	      }
	    else
	      {
		throw new RuntimeException("what?  dropped on a non-category node..");
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }
  }

  /**
   *
   * Method to control whether the drag line may be moved between a pair of given
   * nodes.
   *
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public boolean dragLineTween(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
    treeNode parent = dragNode.getParent();

    /* -- */

    if (debug)
      {
	if (aboveNode != null && belowNode != null)
	  {
	    System.err.println("dragLineTween(): " + dragNode.getText() +
			       " above = " + aboveNode.getText() + ", below = " + belowNode.getText());
	  }
	else if (aboveNode != null)
	  {
	    System.err.println("dragLineTween(): " + dragNode.getText() +
			       " above = " + aboveNode.getText());
	  }
	else
	  {
	    System.err.println("dragLineTween(): " + dragNode.getText() +
			       " below = " + belowNode.getText());
	  }
      }

    if (belowNode == objects)
      {
	return false;
      }

    if (dragNode instanceof FieldNode)
      {
	return (((aboveNode instanceof FieldNode) && (aboveNode != null) && (aboveNode.getParent() == parent)) || 
		((belowNode instanceof FieldNode) && (belowNode != null) && (belowNode.getParent() == parent)));
      }
    else if (dragNode instanceof BaseNode)
      {
	if (belowNode instanceof FieldNode)
	  {
	    return false;
	  }

	if (belowNode == nodeAfterCategories)
	  {
	    return true;
	  }

	return ((aboveNode instanceof BaseNode) || 
		(aboveNode instanceof CatTreeNode) ||
		(belowNode instanceof BaseNode) || 
		(belowNode instanceof CatTreeNode));
      }
    else if (dragNode instanceof CatTreeNode)
      {
	try
	  {
	    if (belowNode instanceof FieldNode)
	      {
		return false;
	      }

	    if (belowNode == nodeAfterCategories)
	      {
		return true;
	      }

	    if (aboveNode instanceof CatTreeNode)
	      {
		return !((CatTreeNode) aboveNode).getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }
	    
	    if (belowNode instanceof CatTreeNode)
	      {
		return !((CatTreeNode) belowNode).getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }

	    if (aboveNode instanceof BaseNode)
	      {
		return !((BaseNode) aboveNode).getBase().getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }
  
	    if (belowNode instanceof BaseNode)
	      {
		return !((BaseNode) belowNode).getBase().getCategory().isUnder(((CatTreeNode) dragNode).getCategory());
	      }
		    
	    return false;
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get category details for drag " + ex);
	  }
      }
    else
      {
	return false;
      }
  }

  /**
   *
   * This method is called when a drag and drop operation in the Schema Editor's tree is completed.
   *
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public void dragLineRelease(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
    if (debug)
      {
	System.out.println("dragNode = " + dragNode.getText());
	System.out.println("aboveNode = " + aboveNode.getText());
	System.out.println("belowNode = " + belowNode.getText());
      }

    if (aboveNode.equals(dragNode) || belowNode.equals(dragNode))
      {
	if (debug)
	  {
	    System.err.println("No change necessary");
	  }

	return;
      }

    if (dragNode instanceof FieldNode)
      {
	FieldNode oldNode = (FieldNode)dragNode;
	BaseNode parentNode = (BaseNode)oldNode.getParent();
	Base base = parentNode.getBase();

	if (debug)
	  {
	    System.out.println("parent = " + parentNode);
	  }
	
	if (aboveNode instanceof FieldNode)
	  {
	    if (aboveNode != dragNode)
	      {
		// Insert below the aboveNode

		try
		  {
		    base.moveFieldAfter(dragNode.getText(), aboveNode.getText());
		  }
		catch (RemoteException ex)
		  {
		    ex.printStackTrace();
		    throw new RuntimeException(ex.getMessage());
		  }

		FieldNode newNode = (FieldNode) tree.moveNode(dragNode, parentNode, aboveNode, true);

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
		  }
	      }
	    else if (debug)
	      {
		System.out.println("aboveNode == dragNode, Not moving it");
	      }
	  }
	else if (belowNode instanceof FieldNode)
	  {
	    if (belowNode != dragNode)
	      {
		// First node, insert below parent

		try
		  {
		    base.moveFieldAfter(dragNode.getText(), null);
		  }
		catch (RemoteException ex)
		  {
		    ex.printStackTrace();
		    throw new RuntimeException(ex.getMessage());
		  }

		FieldNode newNode = (FieldNode) tree.moveNode(dragNode, parentNode, null, true);

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
		  }
	      }
	    else if (debug)
	      {
		System.out.println("belowNode == dragNode, Not moving it");
	      }
	  }
	else
	  {
	    System.err.println("Dropped away from FieldNodes, shouldn't happen");
	  }
      }
    else if (dragNode instanceof BaseNode)
      {
	if (debug)
	  {
	    System.err.println("Releasing baseNode");
	  }

	try
	  {
	    BaseNode bn = (BaseNode) dragNode;
	    Base base = bn.getBase();
	    Category oldCategory = base.getCategory();

	    Category newCategory = null;
	    CatTreeNode newParent = null;
	    treeNode previousNode = null;

	    if (aboveNode instanceof CatTreeNode)
	      {
		if (aboveNode == objects)
		  {
		    newCategory = rootCategory;
		    newParent = (CatTreeNode) aboveNode;
		    previousNode = null;
		  }
		else
		  {
		    if (aboveNode.isOpen())
		      {
			// we want to place ourselves within the category whose
			// node is right above the drop point

			newCategory = ((CatTreeNode) aboveNode).getCategory();
			newParent = (CatTreeNode) aboveNode;
			previousNode = null;
		      }
		    else
		      {
			// we want to place ourselves in the category
			// containing the above node

			newParent = (CatTreeNode) aboveNode.getParent();
			newCategory = newParent.getCategory();
			previousNode = aboveNode;
		      }
		  }
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		newCategory = ((BaseNode) aboveNode).getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent();

		previousNode = aboveNode;
	      }
	    else if (aboveNode instanceof FieldNode)
	      {
		newCategory = ((FieldNode) aboveNode).getField().getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent().getParent();
		previousNode = aboveNode.getParent();
	      }

	    if (debug)
	      {
		System.err.println("New Category = " + newCategory.getPath());
		System.err.println("new parent = " + newParent.getText());

		if (previousNode != null)
		  {
		    System.err.println("new prevNode = " + previousNode.getText());
		  }
	      }

	    if (previousNode != null)
	      {
		newCategory.moveCategoryNode(base.getPath(), previousNode.getText());
	      }
	    else
	      {
		newCategory.moveCategoryNode(base.getPath(), null);
	      }

	    BaseNode newNode = (BaseNode) tree.moveNode(dragNode, newParent, previousNode, true);

	    refreshFields(newNode, true);

	    if (be.baseNode == dragNode)
	      {
		be.baseNode = newNode;
	      }

	    if (debug)
	      {
		System.err.println("Reinserted base " + base.getName());
		System.err.println("reinserted category = " + base.getCategory().getName());
	      }

	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote : " + ex);
	  }
	
      }
    else if (dragNode instanceof CatTreeNode)
      {
	if (debug)
	  {
	    System.err.println("Releasing CatTreeNode");
	  }

	try
	  {
	    CatTreeNode cn = (CatTreeNode) dragNode;
	    Category category = cn.getCategory();
	    Category oldCategory = category.getCategory();

	    Category newCategory = null;
	    CatTreeNode newParent = null;
	    treeNode previousNode = null;

	    if (aboveNode instanceof CatTreeNode)
	      {
		if (aboveNode == objects)
		  {
		    newCategory = rootCategory;
		    newParent = (CatTreeNode) aboveNode;
		    previousNode = null;
		  }
		else
		  {
		    if (aboveNode.isOpen())
		      {
			newCategory = ((CatTreeNode) aboveNode).getCategory();
			newParent = (CatTreeNode) aboveNode;
			previousNode = null;
		      }
		    else
		      {
			newParent = (CatTreeNode) aboveNode.getParent();
			newCategory = newParent.getCategory();
			previousNode = aboveNode;
		      }
		  }
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		newCategory = ((BaseNode) aboveNode).getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent();

		previousNode = aboveNode;
	      }
	    else if (aboveNode instanceof FieldNode)
	      {
		newCategory = ((FieldNode) aboveNode).getField().getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent().getParent();
		previousNode = aboveNode.getParent();
	      }

	    if (debug)
	      {
		System.err.println("New Category = " + newCategory.getPath());
		System.err.println("new parent = " + newParent.getText());

		if (previousNode != null)
		  {
		    System.err.println("new prevNode = " + previousNode.getText());
		  }
	      }

	    if (previousNode == null)
	      {
		newCategory.moveCategoryNode(category.getPath(), null);
	      }
	    else
	      {
		newCategory.moveCategoryNode(category.getPath(), previousNode.getText());
	      }

	    if (debug)
	      {
		System.err.println("Moved category " + category.getPath());
	      }
	    

	    CatTreeNode newNode = (CatTreeNode) tree.moveNode(dragNode, newParent, previousNode, true);

	    if (ce.catNode == dragNode)
	      {
		ce.catNode = newNode;
	      }

	    if (debug)
	      {
		System.err.println("Reinserted category " + category.getName());
		System.err.println("reinserted category = " + category.getCategory().getName());
	      }

	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote : " + ex);
	  }

      }
  }

  /**
   * <p>This method takes a ReturnVal object from the server and, if necessary,
   * runs through a wizard interaction sequence, possibly displaying several
   * dialogs before finally returning a final result code.</p>
   *
   * <p>Use the ReturnVal returned from this function after this function is
   * called to determine the ultimate success or failure of any operation
   * which returns ReturnVal, because a wizard sequence may determine the
   * ultimate result.</p>
   *
   * <p>This method should not be synchronized, since handleReturnVal
   * may pop up modal (thread-blocking) dialogs, and if we we
   * synchronize this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.</p> 
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    Hashtable dialogResults;

    /* -- */

    if (debug)
      {
	System.err.println("GASHSchema.handleReturnVal(): Entering");
      }

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	if (debug)
	  {
	    System.err.println("GASHSchema.handleReturnVal(): retrieving dialog");
	  }

	JDialogBuff jdialog = retVal.getDialog();

	if (debug)
	  {
	    System.err.println("GASHSchema.handleReturnVal(): extracting dialog");
	  }

	DialogRsrc resource = jdialog.extractDialogRsrc(this);

	if (debug)
	  {
	    System.err.println("GASHSchema.handleReturnVal(): constructing dialog");
	  }

	StringDialog dialog = new StringDialog(resource);

	if (debug)
	  {
	    System.err.println("GASHSchema.handleReturnVal(): displaying dialog");
	  }

	// display the Dialog sent to us by the server, get the
	// result of the user's interaction with it.
	    
	dialogResults = dialog.DialogShow();

	if (debug)
	  {
	    System.err.println("GASHSchema.handleReturnVal(): dialog done");
	  }

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		if (debug)
		  {
		    System.err.println("GASHSchema.handleReturnVal(): Sending result to callback: " + dialogResults);
		  }

		// send the dialog results to the server

		retVal = retVal.getCallback().respond(dialogResults);

		if (debug)
		  {
		    System.err.println("GASHSchema.handleReturnVal(): Received result from callback.");
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Caught remote exception: " + ex.getMessage());
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("GASHSchema.handleReturnVal(): No callback, breaking");
	      }

	    break;		// we're done
	  }
      }

    if (debug)
      {
	if (retVal != null)
	  {
	    if (retVal.didSucceed())
	      {
		System.err.println("GASHSchema.handleReturnVal(): returning success code");
	      }
	    else
	      {
		System.err.println("GASHSchema.handleReturnVal(): returning failure code");
	      }
	  }
	else
	  {
	    System.err.println("GASHSchema.handleReturnVal(): returning null retVal (success)");
	  }
      }

    return retVal;
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                                 NameSpaceEditor

------------------------------------------------------------------------------*/

class NameSpaceEditor extends JPanel implements ActionListener {

  static final boolean debug = false;
  
  SpaceNode node;
  NameSpace space;
  JstringField nameS;
  JList spaceL;
  JCheckBox caseCB;
  JPanel nameJPanel;
  GASHSchema owner;
  String currentNameSpaceLabel = null;

  GridBagLayout
    gbl = new GridBagLayout();

  GridBagConstraints
    gbc = new GridBagConstraints();
  
  /* -- */

  NameSpaceEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    if (debug)
      {
	System.err.println("NameSpaceEditor constructed");
      }

    this.owner = owner;

    nameJPanel = new JInsetPanel(10,10,10,10);
    nameJPanel.setLayout(gbl);

    nameS = new JstringField(20, 100, false, false, null, null);
    addRow(nameJPanel, nameS, "Namespace:", 0);
      
    caseCB = new JCheckBox();
    caseCB.setEnabled(false);
    addRow(nameJPanel, caseCB, "Case insensitive:", 1);
    
    spaceL = new JList();
    //spaceL.setEnabled(false);
    addRow(nameJPanel, spaceL, "Fields in this space:", 2);

    setLayout(new java.awt.BorderLayout());
    add("Center", nameJPanel);
  }

  public void editNameSpace(SpaceNode node)
  {
    this.node = node;
    space = node.getSpace();
    
    try
      {
	nameS.setText(space.getName());
	caseCB.setSelected(space.isCaseInsensitive());
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
    if (debug)
      {
	System.out.println("action Performed in NameSpaceEditor");
      }
  }

  public void refreshSpaceList()
  {
    spaceL.removeAll();
    SchemaEdit se = owner.getSchemaEdit();
    Base[] bases = null;

    try
      {
	bases = se.getBases(); // we want to find all fields that refer to this namespace
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("Exception: can't get bases: " + rx);
      }

    Vector fields = null;
    Vector spaceV = new Vector();
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
	if (debug)
	  {
	    System.out.println("currentNameSpaceLabel= " + currentNameSpaceLabel);
	  }
	  
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
		if (debug)
		  {
		    System.out.println("fields == null");
		  }
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
				if (debug)
				  {
				    System.out.println("Adding to spaceV: " + thisBase +
						       ":" + currentField.getName());;
				  }

				spaceV.addElement(thisBase + ":" + currentField.getName());
			      }
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("Exception generating spaceL: " + rx);
		      }
		    
		  }

		spaceL.setListData(spaceV);
	      }
	  }
      }
  }

  synchronized void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    gbc.weightx = 0.0;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbl.setConstraints(l, gbc);
    parent.add(l);

    gbc.gridx = 1;
    gbc.weightx = 1.0;
    gbl.setConstraints(comp, gbc);
    parent.add(comp);
  }

  /**
   * <p>GC-aiding dissolution method.  Should be called on GUI thread.</p>
   */

  public void cleanup()
  {
    this.node = null;
    this.space = null;	// remote reference
    this.nameS = null;
    this.spaceL = null;
    this.caseCB = null;
    this.nameJPanel = null;
    this.owner = null;
    this.currentNameSpaceLabel = null;

    this.gbl = null;
    this.gbc = null;

    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  CategoryEditor

------------------------------------------------------------------------------*/

class CategoryEditor extends JPanel implements JsetValueCallback {

  static final boolean debug = false;

  // ---

  GASHSchema owner;  
  JPanel catJPanel;
  JstringField catNameS;
  CatTreeNode catNode;
  Category category;

  GridBagLayout
    gbl = new GridBagLayout();
  
  GridBagConstraints
    gbc = new GridBagConstraints();

  /* -- */

  CategoryEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }
    
    if (debug)
      {
	System.err.println("CategoryEditor constructed");
      }

    this.owner = owner;
    
    catJPanel = new JInsetPanel(10,10,10,10);
    catJPanel.setLayout(gbl);
    
    catNameS = new JstringField(20, 100, true, false, null, "/", this);
    addRow(catJPanel, catNameS, "Category Label:", 0);
    
    setLayout(new java.awt.BorderLayout());
    add("Center", catJPanel);
  }

  void editCategory(CatTreeNode catNode)
  {
    this.catNode = catNode;
    this.category = catNode.getCategory();

    try
      {
	catNameS.setText(category.getName());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Remote Exception gettin gNameSpace attributes " + rx);
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    if (v.getSource() == catNameS)
      {
	try
	  {
	    String newValue = (String) v.getValue();

	    // we can't allow categories to have null names, because
	    // if they do, trying to delete the category would
	    // be.. unfortunate.  We really should have some way of
	    // *telling* the user why we're not letting them do this,
	    // but I don't know if we have a handy way of doing that
	    // from this class.

	    if (newValue.equals(""))
	      {
		return false;
	      }

	    if (debug)
	      {
		System.err.println("Trying to set category name to " + newValue);
	      }

	    if (category.setName(newValue))
	      {
		// update the node in the tree

		catNode.setText(newValue);
		owner.tree.refresh();

		if (debug)
		  {
		    System.err.println("Was able to set category name to " + newValue);
		  }

		return true;
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("Was not able to set category name to " + newValue);
		  }

		return false;
	      }
	  }
	catch (RemoteException ex)
	  {
	    return false;
	  }
      }

    return true;		// what the?
  }

  synchronized void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);

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
  }

  /**
   * <p>GC-aiding dissolution method.  Should be called on GUI thread.</p>
   */

  public void cleanup()
  {
    this.owner = null;
    this.catJPanel = null;
    this.catNameS = null;
    this.catNode = null;

    this.category = null;	// remote reference

    this.gbl = null;
    this.gbc = null;

    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread
  }
}
