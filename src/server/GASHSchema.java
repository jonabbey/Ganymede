/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Version: $Revision: 1.60 $ %D%
   Module By: Jonathan Abbey and Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;
import arlut.csd.JDialog.JInsetPanel;
import arlut.csd.JTree.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

import tablelayout.*;

import java.rmi.*;
import java.rmi.server.*;
//import java.awt.*;
import java.awt.event.*;
import java.util.*;

import jdj.PackageResources;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GASHSchema

------------------------------------------------------------------------------*/

public class GASHSchema extends JFrame implements treeCallback, treeDragDropCallback, ActionListener {

  public static final boolean debug = false;

  // --

  boolean developMode;	// if this is true, we can mangle otherwise inviolable bases/fields

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
    namespaces,			// top-level node for namespace listing
    builtIns;			// top-level node for (non-embedded) built-in field defs

  java.awt.MenuItem
    createCategoryMI = null,
    deleteCategoryMI = null,
    createObjectMI = null,
    createInternalObjectMI = null,
    createLowObjectMI = null,
    deleteObjectMI = null,
    createNameMI = null,
    deleteNameMI = null,
    createFieldMI = null,
    createLowFieldMI = null,
    deleteFieldMI = null,
    createBuiltInMI = null;
  
  treeMenu
    categoryMenu = null,
    baseMenu = null,
    fieldMenu = null,
    builtInMenu = null,
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

  /* -- */

  public GASHSchema(String title, SchemaEdit editor)
  {
    super(title);

    this.editor = editor;

    try
      {
	developMode = editor.isDevelopMode();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't determine develop mode. " + ex);
      }

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
    JPanel attribPaneBorder = new JPanel(new java.awt.BorderLayout());
    attribPaneBorder.add("Center", attribPane);
    attribPaneBorder.setBorder(new TitledBorder("Attributes"));

    JInsetPanel rightJPanel = new JInsetPanel(5, 5, 5, 10);
    rightJPanel.setLayout(new java.awt.BorderLayout());
    rightJPanel.add("Center", attribPaneBorder);

    // Set up button pane

    buttonPane = new JPanel();

    okButton = new JButton("ok");
    okButton.addActionListener(this);

    cancelButton = new JButton("cancel");
    cancelButton.addActionListener(this);

    buttonPane.add(okButton);
    buttonPane.add(cancelButton);

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
			   java.awt.Color.black, java.awt.SystemColor.window, this, treeImages,
			   null);
    tree.setMinimumWidth(200);
    tree.setDrag(this, tree.DRAG_LINE | tree.DRAG_ICON);

    //
    //
    //   **** Set up display tree panels
    //
    //

    
    JPanel leftBox = new JPanel(new java.awt.BorderLayout());
    leftBox.add("Center", tree);
    leftBox.setBorder(new TitledBorder("Schema Objects"));

    JInsetPanel leftJPanel = new JInsetPanel(5, 10, 5, 5);
    leftJPanel.setLayout(new java.awt.BorderLayout());
    leftJPanel.add("Center", leftBox);

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

    createCategoryMI = new java.awt.MenuItem("Create Category");
    deleteCategoryMI = new java.awt.MenuItem("Delete Category");
    createObjectMI = new java.awt.MenuItem("Create Object Type");
    createInternalObjectMI = new java.awt.MenuItem("Create Embedded Object Type");
    createLowObjectMI = new java.awt.MenuItem("Create Low Range Object Type");

    categoryMenu.add(createCategoryMI);
    categoryMenu.add(deleteCategoryMI);
    categoryMenu.add(createObjectMI);
    categoryMenu.add(createInternalObjectMI);

    if (developMode)
      {
	categoryMenu.add(createLowObjectMI);
      }

    // builtIn menu

    createBuiltInMI = new java.awt.MenuItem("Create Built-in Field");

    builtInMenu = new treeMenu("Built-in Fields");
    builtInMenu.add(createBuiltInMI);

    // namespace menu

    nameSpaceMenu = new treeMenu("Namespace Menu");
    createNameMI = new java.awt.MenuItem("Create Namespace");
    nameSpaceMenu.add(createNameMI);

    // namespace object menu

    nameSpaceObjectMenu = new treeMenu();
    deleteNameMI = new java.awt.MenuItem("Delete Namespace");
    nameSpaceObjectMenu.add(deleteNameMI);

    // base menu

    baseMenu = new treeMenu("Base Menu");
    deleteObjectMI = new java.awt.MenuItem("Delete Object Type");
    createFieldMI = new java.awt.MenuItem("Create Field");
    createLowFieldMI = new java.awt.MenuItem("Create low-range Field");

    baseMenu.add(createFieldMI);

    if (developMode)
      {
	baseMenu.add(createLowFieldMI);
      }

    baseMenu.add(deleteObjectMI);

    // field menu

    fieldMenu = new treeMenu("Field Menu");
    deleteFieldMI = new java.awt.MenuItem("Delete Field");
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

	System.err.println("Created rootCategory node: " + rootCategory.getName());
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't get rootCategory " + ex);
      }

    tree.setRoot(objects);

    // create builtIn node

    if (developMode)
      {
	builtIns = new treeNode(null, "Built-In Fields", objects, true, 0, 1, builtInMenu);
	tree.insertNode(builtIns, false);

	// create namespaces node
	
	namespaces = new treeNode(null, "Namespaces", builtIns, true, 0, 1, nameSpaceMenu);
	tree.insertNode(namespaces, false);
	
	nodeAfterCategories = builtIns;
      }
    else
      {
	// create namespaces node
	
	namespaces = new treeNode(null, "Namespaces", objects, true, 0, 1, nameSpaceMenu);
	tree.insertNode(namespaces, false);
	
	nodeAfterCategories = namespaces;
      }


    // and initialize tree

    initializeDisplayTree();

    pack();
    setSize(800, 600);
    show();

    System.out.println("GASHSchema created");
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

    if (developMode)
      {
	try
	  {
	    refreshBuiltIns();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't refresh built-ins" + ex);
	  }
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
    Vector vect, vect2;
    BaseNode parentNode;
    FieldNode oldNode, newNode, fNode;
    int i;

    /* -- */

    base = node.getBase();

    // get the list of fields we want to display
    // note that we don't want to show built-in fields

    vect = base.getFields();

    vect2 = new Vector();
    
    for (i = 0; i < vect.size(); i++)
      {
	field = (BaseField) vect.elementAt(i);

	if (!field.isBuiltIn())
	  {
	    vect2.addElement(field);
	  }
      }

    fields = new BaseField[vect2.size()];
    
    for (i = 0; i < fields.length; i++)
      {
	fields[i] = (BaseField) vect2.elementAt(i);
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

    // this loop here is intended to do a minimum-work updating
    // of a field list
	
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

  void refreshBuiltIns() throws RemoteException
  {
    Base base;
    BaseField field, fields[];
    Vector vect, vect2;
    FieldNode oldNode, newNode, fNode;
    int i;

    /* -- */

    boolean isOpen = builtIns.isOpen();
    
    tree.removeChildren(builtIns, false);

    // we assume that we can rely on the user base having
    // a full complement of built in fields.

    base = editor.getBase(SchemaConstants.UserBase);

    // get the list of fields we want to display
    // note that we don't want to show built-in fields

    vect = base.getFields();

    vect2 = new Vector();
    
    for (i = 0; i < vect.size(); i++)
      {
	field = (BaseField) vect.elementAt(i);

	if (field.isBuiltIn())
	  {
	    vect2.addElement(field);
	  }
      }

    fields = new BaseField[vect2.size()];
    
    for (i = 0; i < fields.length; i++)
      {
	fields[i] = (BaseField) vect2.elementAt(i);
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

    oldNode = null;
    fNode = (FieldNode) builtIns.getChild();
    i = 0;

    // this loop here is intended to do a minimum-work updating
    // of a field list
	
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

	    newNode = new FieldNode(builtIns, field.getName(), field,
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

    if (isOpen)
      {
	tree.expandNode(builtIns, false);
      }

    tree.refresh();
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
    System.err.println("in GASHSchema.editField");
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
	System.out.println("namespacenode selected");
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

    System.out.println("node " + node.getText() + " unselected");
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

    System.out.println("node " + nodeText + ", action: " + event );

    if (event.getSource() == createCategoryMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();

	    Category newCategory = category.newSubCategory();

	    // we want to insert at the bottom of the base

	    treeNode n = node.getChild();
	    short order = 0;
	    
	    if (n != null)
	      {
		while (n.getNextSibling() != null)
		  {
		    try
		      {
			if (n instanceof BaseNode)
			  {
			    order = (short) (((BaseNode) n).getBase().getDisplayOrder() + 1);
			  }
			else
			  {
			    order = (short) (((CatTreeNode) n).getCategory().getDisplayOrder() + 1);
			  }
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("couldn't get display order for " + n);
		      }

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
    else if (event.getSource() == createLowObjectMI)
      {
	try
	  {
	    CatTreeNode cNode = (CatTreeNode) node;
	    Category category = cNode.getCategory();
	    Base newBase = editor.createNewBase(category, false, true);
	    
	    BaseNode newNode = new BaseNode(node, newBase.getName(), newBase,
					    null, false, 2, 2, baseMenu);

	    tree.insertNode(newNode, false);

	    tree.expandNode(node, false);

	    refreshFields(newNode, true);

	    editBase(newNode);
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new low-range base." + ex);
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
	System.out.println("Create namespace chosen");

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
		fe.refreshFieldEdit();
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

	if (isRemovable || developMode)
	  {
	    if (new StringDialog(this,
				 "Confirm deletion of Object",
				 "Are you sure you want to delete this object?",
				 "Confirm",
				 "Cancel").DialogShow() == null)
	      {
		System.out.println("Deletion canceled");
	      }
	    else //Returned confirm
	      {	    
		try
		  {
		    System.err.println("Deleting base " + b.getName());
		    editor.deleteBase(b);
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
    else if ((event.getSource() == createFieldMI) ||
	     (event.getSource() == createLowFieldMI))
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

	    bF = b.createNewField((event.getSource() == createLowFieldMI));
	    bF.setName(newname);

	    // we want to insert the child's field node
	    // at the bottom of the base

	    treeNode n = node.getChild();
	    short order = 0;
	    
	    if (n != null)
	      {
		while (n.getNextSibling() != null)
		  {
		    try
		      {
			order = (short) (((FieldNode) n).getField().getDisplayOrder() + 1);
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("couldn't get display order for " + n);
		      }

		    n = n.getNextSibling();
		  }
	      }

	    try
	      {
		bF.setDisplayOrder(order);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("couldn't set display order for " + bF);
	      }

	    FieldNode newNode = new FieldNode(node, newname, bF, n,
					      false, 3, 3, fieldMenu);
	    tree.insertNode(newNode, false);
	    tree.expandNode(node, true);

	    editField(newNode);
	    System.err.println("Called editField");
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
	boolean isEditable = false;
	boolean isRemovable = false;

	try
	  {
	    isRemovable = field.isRemovable();
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Can't get isRemoveable, assuming false: " +rx);
	  }

	if (isRemovable || developMode)
	  {
	    try
	      {
		isEditable = field.isEditable();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("can't tell if field is editable, assuming false: " + rx);
	      }

	    if (isEditable || developMode)
	      {
		System.err.println("deleting field node");

		DialogRsrc dialogResource = new DialogRsrc(this,
							   "Confirm Field Deletion",
							   "Confirm Field Deletion",
							   "Delete", "Cancel",
							   questionImage);

		Hashtable results = new StringDialog(dialogResource).DialogShow();

		if (results != null)
		  {
		    if (node.getParent() instanceof BaseNode)
		      {
			BaseNode bNode = (BaseNode) node.getParent();

			try
			  {
			    if (!bNode.getBase().fieldInUse(fNode.getField()))
			      {
				bNode.getBase().deleteField(fNode.getField());
				refreshFields(bNode, true);
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
		    else if (developMode)
		      {
			// assume we're deleting a built-in field

			try
			  {
			    editor.getBase(SchemaConstants.UserBase).deleteField(fNode.getField());
			    refreshBuiltIns();
			  }
			catch (RemoteException ex)
			  {
			    throw new RuntimeException("danger will robinson! couldn't delete built-in field! " + ex);
			  }

			ne.refreshSpaceList();
			be.refreshLabelChoice();
		      }
		  }
	      }
	    else
	      {
		new StringDialog(this, 
				 "Error: field not editable",
				 "This field is not editable.  You cannot delete it.",
				 "Ok",
				 null).DialogShow();
	      }
	  }
	else
	  {
	    new StringDialog(this,
			     "Error: field not removable",
			     "This field is not removable.",
			     "Ok",
			     null).DialogShow();
	  }
      }
    else if (event.getSource() == createBuiltInMI)
      {
	// find the base that asked for the field

	try
	  {
	    // create a name for the new field

	    BaseField bF;

	    bF = editor.createNewBuiltIn();

	    // we'll go ahead and insert the new node at the top of
	    // the Built-In Fields subtree

	    FieldNode newNode = new FieldNode(node, bF.getName(), bF, null,
					      false, 3, 3, fieldMenu);
	    tree.insertNode(newNode, false);
	    tree.expandNode(node, true);

	    editField(newNode);
	    System.err.println("Called editField");
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
    else
      {
	System.err.println("Unknown Action Performed in GASHSchema");
      }
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
    if (dragNode instanceof FieldNode)
      {
	FieldNode fN = (FieldNode) dragNode;

	try
	  {
	    if (fN.getField().isBuiltIn())
	      {
		return false;	// we don't allow reordering of built-ins
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote exception when checking field for drag start:" + ex);
	  }
      }

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
	return false;
      }

    if (dragNode instanceof FieldNode)
      {
	return false;
      }

    if (dragNode instanceof BaseNode)
      {
	return (targetNode instanceof CatTreeNode);
      }

    if (dragNode instanceof CatTreeNode)
      {
	if (!(targetNode instanceof CatTreeNode))
	  {
	    return false;
	  }
	
	CatTreeNode cNode = (CatTreeNode) dragNode;
	CatTreeNode cNode1 = (CatTreeNode) targetNode;

	try
	  {
	    return (!cNode1.getCategory().isUnder(cNode.getCategory()));
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }
    
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

		System.err.println("Dropping base " + base.getName() + " from " +
				   oldCategory.getName() + " onto " + newCategory.getName());

		base.setDisplayOrder(0);

		System.err.println("Removing " + base.getName() + " from " + oldCategory.getName());

		oldCategory.removeNode(base.getName());

		System.err.println("Adding " + base.getName() + " to " + newCategory.getName());

		newCategory.addNode((CategoryNode) base, false, true);

		BaseNode newNode = new BaseNode(targetNode, base.getName(), base,
						null, true, 2, 2, baseMenu);

		System.err.println("Deleting dragNode: " + dragNode.getText());

		tree.deleteNode(dragNode, false);

		System.err.println("Inserting newNode: " + newNode.getText());

		tree.insertNode(newNode, false);

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

		System.err.println("Dropping category " + category.getName() + " from " +
				   oldCategory.getName() + " onto " + newCategory.getName());

		category.setDisplayOrder(0);

		oldCategory.removeNode(category.getName());
		newCategory.addNode((CategoryNode) category, false, true);

		CatTreeNode newNode = new CatTreeNode(targetNode, category.getName(), category,
						      null, true, 0, 1, categoryMenu);

		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);

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
    System.out.println("dragNode = " + dragNode.getText());
    System.out.println("aboveNode = " + aboveNode.getText());
    System.out.println("belowNode = " + belowNode.getText());

    if (aboveNode.equals(dragNode) || belowNode.equals(dragNode))
      {
	System.err.println("No change necessary");
	return;
      }

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
						  aboveNode, false, 3, 3, fieldMenu);
		
		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
		  }
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
						  null, false, 3, 3, fieldMenu);

		tree.deleteNode(dragNode, false);
		tree.insertNode(newNode, true);

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
		  }
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
	
	// Ok, that mostly works, plugging ahead

	// Renumber the fields of this parent.
	
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
    else if (dragNode instanceof BaseNode)
      {
	System.err.println("Releasing baseNode");

	try
	  {
	    BaseNode bn = (BaseNode) dragNode;
	    Base base = bn.getBase();
	    Category oldCategory = base.getCategory();

	    Category newCategory = null;
	    CatTreeNode newParent = null;
	    treeNode previousNode = null;
	    int displayOrder = 0;

	    if (aboveNode instanceof CatTreeNode)
	      {
		if (aboveNode == objects)
		  {
		    newCategory = rootCategory;
		    newParent = (CatTreeNode) aboveNode;
		    previousNode = null;
		    displayOrder = 0;
		  }
		else
		  {
		    if (aboveNode.isOpen())
		      {
			newCategory = ((CatTreeNode) aboveNode).getCategory();
			newParent = (CatTreeNode) aboveNode;
			previousNode = null;
			displayOrder = 0;
		      }
		    else
		      {
			newParent = (CatTreeNode) aboveNode.getParent();
			newCategory = newParent.getCategory();
			previousNode = aboveNode;
			displayOrder = ((CatTreeNode) aboveNode).getCategory().getDisplayOrder() + 1;
		      }
		  }
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		newCategory = ((BaseNode) aboveNode).getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent();
		displayOrder = ((BaseNode) aboveNode).getBase().getDisplayOrder() + 1;

		previousNode = aboveNode;
	      }
	    else if (aboveNode instanceof FieldNode)
	      {
		newCategory = ((FieldNode) aboveNode).getField().getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent().getParent();
		displayOrder = ((FieldNode) aboveNode).getField().getBase().getDisplayOrder() + 1;
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

	    if (newCategory.equals(oldCategory))
	      {
		if (debug)
		  {
		    System.err.println("Moving within the same category");
		  }

		if (base.getDisplayOrder() < displayOrder)
		  {
		    displayOrder--;
		  }
	      }

	    if (debug)
	      {
		System.err.println("new displayOrder = " + displayOrder);
	      }

	    oldCategory.removeNode(base.getName());

	    base.setDisplayOrder(displayOrder);
	    newCategory.addNode((CategoryNode) base, false, true);

	    tree.deleteNode(dragNode, false);

	    BaseNode newNode = new BaseNode(newParent, base.getName(), base, previousNode,
					    false, 2, 2, baseMenu);

	    tree.insertNode(newNode, false);

	    refreshFields(newNode, true);

	    if (be.baseNode == dragNode)
	      {
		be.baseNode = newNode;
	      }

	    if (debug)
	      {
		System.err.println("Reinserted base " + base.getName());
		System.err.println("reinserted order = " + base.getDisplayOrder());
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
	System.err.println("Releasing CatTreeNode");

	try
	  {
	    CatTreeNode cn = (CatTreeNode) dragNode;
	    Category category = cn.getCategory();
	    Category oldCategory = category.getCategory();

	    Category newCategory = null;
	    CatTreeNode newParent = null;
	    treeNode previousNode = null;
	    int displayOrder = 0;

	    if (aboveNode instanceof CatTreeNode)
	      {
		if (aboveNode == objects)
		  {
		    newCategory = rootCategory;
		    newParent = (CatTreeNode) aboveNode;
		    previousNode = null;
		    displayOrder = 0;
		  }
		else
		  {
		    if (aboveNode.isOpen())
		      {
			newCategory = ((CatTreeNode) aboveNode).getCategory();
			newParent = (CatTreeNode) aboveNode;
			previousNode = null;
			displayOrder = 0;
		      }
		    else
		      {
			newParent = (CatTreeNode) aboveNode.getParent();
			newCategory = newParent.getCategory();
			previousNode = aboveNode;
			displayOrder = ((CatTreeNode) aboveNode).getCategory().getDisplayOrder() + 1;
		      }
		  }
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		newCategory = ((BaseNode) aboveNode).getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent();
		displayOrder = ((BaseNode) aboveNode).getBase().getDisplayOrder() + 1;

		previousNode = aboveNode;
	      }
	    else if (aboveNode instanceof FieldNode)
	      {
		newCategory = ((FieldNode) aboveNode).getField().getBase().getCategory();
		newParent = (CatTreeNode) aboveNode.getParent().getParent();
		displayOrder = ((FieldNode) aboveNode).getField().getBase().getDisplayOrder() + 1;
		previousNode = aboveNode.getParent();
	      }

	    if (false)
	      {
		System.err.println("New Category = " + newCategory.getPath());
		System.err.println("new parent = " + newParent.getText());

		if (previousNode != null)
		  {
		    System.err.println("new prevNode = " + previousNode.getText());
		  }
	      }

	    if (newCategory.equals(oldCategory))
	      {
		if (false)
		  {
		    System.err.println("Moving within the same category");
		  }

		if (category.getDisplayOrder() < displayOrder)
		  {
		    displayOrder--;
		  }
	      }

	    if (false)
	      {
		System.err.println("new displayOrder = " + displayOrder);
	      }

	    oldCategory.removeNode(category.getName());

	    category.setDisplayOrder(displayOrder);
	    newCategory.addNode((CategoryNode) category, true, true);
	    tree.deleteNode(dragNode, false);

	    CatTreeNode newNode = new CatTreeNode(newParent, category.getName(), category, previousNode,
						  false, 0, 1, categoryMenu);

	    tree.insertNode(newNode, false);

	    if (ce.catNode == dragNode)
	      {
		ce.catNode = newNode;
	      }

	    if (false)
	      {
		System.err.println("Reinserted category " + category.getName());
		System.err.println("reinserted order = " + category.getDisplayOrder());
		System.err.println("reinserted category = " + category.getCategory().getName());
	      }

	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote : " + ex);
	  }

      }
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                                 NameSpaceEditor

------------------------------------------------------------------------------*/

class NameSpaceEditor extends JPanel implements ActionListener {
  
  SpaceNode node;
  NameSpace space;
  JstringField nameS;
  JList spaceL;
  JCheckBox caseCB;
  JPanel nameJPanel;
  JcomponentAttr ca;
  GASHSchema owner;
  String currentNameSpaceLabel = null;
  
  /* -- */

  NameSpaceEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    System.err.println("NameSpaceEditor constructed");

    this.owner = owner;

    nameJPanel = new JInsetPanel(10,10,10,10);
    nameJPanel.setLayout(new TableLayout(false));

    ca = new JcomponentAttr(this, new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12),
			   java.awt.Color.black, java.awt.Color.white);
      
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
    System.out.println("action Performed in NameSpaceEditor");
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
				System.out.println("Adding to spaceV: " + thisBase + ":" + currentField.getName());;
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

  void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);

    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  CategoryEditor

------------------------------------------------------------------------------*/

class CategoryEditor extends JPanel implements JsetValueCallback {

  GASHSchema owner;  
  JPanel catJPanel;
  JstringField catNameS;
  CatTreeNode catNode;
  Category category;

  /* -- */

  CategoryEditor(GASHSchema owner)
  {
    JcomponentAttr ca;

    /* -- */

    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }
    
    System.err.println("CategoryEditor constructed");

    this.owner = owner;
    
    catJPanel = new JInsetPanel(10,10,10,10);
    catJPanel.setLayout(new TableLayout(false));
    
    ca = new JcomponentAttr(this, new java.awt.Font("SansSerif",java.awt.Font.BOLD, 12),
			   java.awt.Color.black, java.awt.Color.white);
    
    catNameS = new JstringField(20, 100, true, false, null, null, this);
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
	    if (category.setName((String) v.getValue()))
	      {
		catNode.setText((String) v.getValue());
		owner.tree.refresh();
		return true;
	      }
	    else
	      {
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

  void addRow(JPanel parent, java.awt.Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);
  }
}
