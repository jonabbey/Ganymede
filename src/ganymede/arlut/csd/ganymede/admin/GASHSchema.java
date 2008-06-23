/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey and Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System

   Copyright (C) 1996-2008
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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JTree.treeCallback;
import arlut.csd.JTree.treeControl;
import arlut.csd.JTree.treeDragDropCallback;
import arlut.csd.JTree.treeMenu;
import arlut.csd.JTree.treeNode;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.CatTreeNode;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.CategoryNode;
import arlut.csd.ganymede.rmi.NameSpace;
import arlut.csd.ganymede.rmi.SchemaEdit;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GASHSchema

------------------------------------------------------------------------------*/

/**
 * GUI Schema Editor, part of the Ganymede admin console.
 *
 * GASHSchema talks to the server by way of the
 * {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} remote interface.
 */

public class GASHSchema extends JFrame implements treeCallback, treeDragDropCallback, ActionListener {

  public static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.GASHSchema");

  public static final int OPENFOLDERICON = 0;
  public static final int CLOSEDFOLDERICON = 1;
  public static final int BASEICON = 2;
  public static final int FIELDICON = 3;
  public static final int EMBEDDEDBASEICON = 4;
  public static final int OPENTAB = 5;
  public static final int CLOSEDTAB = 6;

  public static final int IMAGECOUNT = 7;

  // --

  SchemaEdit			// remote reference
    editor;

  GASHAdminDispatch
    dispatch;

  java.awt.Image
    questionImage,
    treeImages[];

  treeControl 
    tree;

  Category			// remote reference
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
    createTabMI = null,
    deleteTabMI = null,
    createFieldMI = null,
    deleteFieldMI = null,
    createEmbeddedFieldMI = null,
    deleteEmbeddedObjectMI = null;
  
  treeMenu
    categoryMenu = null,
    baseMenu = null,
    embeddedBaseMenu = null,
    tabMenu = null,
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
    categoryEditPane,
    tabEditPane;

  JScrollPane
    fieldEditPane,
    namespaceEditPane,
    baseEditPane;

  BaseEditor
    be;				// contains remote ref

  BaseFieldEditor
    fe;				// contains remote ref

  NameSpaceEditor
    ne;				// contains remote ref

  CategoryEditor
    ce;				// contains remote ref

  TabEditor
    te;
  
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

  public GASHSchema(String title, SchemaEdit editor, GASHAdminDispatch dispatch)
  {
    super(title);

    this.editor = editor;
    this.dispatch = dispatch;

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

    // set up the tab editor

    tabEditPane = new JPanel();
    tabEditPane.setLayout(new java.awt.BorderLayout());

    te = new TabEditor(this);
    tabEditPane.add("Center", te);

    // set up the empty card

    emptyPane = new JPanel();
    //    emptyPane.setBackground(bgColor);

    // Finish attribPane setup

    attribCardPane.add("empty", emptyPane);
    attribCardPane.add("base", baseEditPane);
    attribCardPane.add("field", fieldEditPane);
    attribCardPane.add("name", namespaceEditPane);
    attribCardPane.add("category", categoryEditPane);
    attribCardPane.add("tab", tabEditPane);

    attribPane.add("Center", attribCardPane);

    JPanel rightJPanel = new JPanel();

    JPanel rightTop = new JPanel(false);
    rightTop.setBorder(statusBorderRaised);

    // "Attributes"
    JLabel rightL = new JLabel(ts.l("init.attributeLabel"));

    rightTop.setLayout(new BorderLayout());
    rightTop.add("Center", rightL);
    
    rightJPanel.setLayout(new java.awt.BorderLayout());
    rightJPanel.add("Center", attribPane);
    rightJPanel.add("North", rightTop);

    // Set up button pane

    buttonPane = new JPanel();

    // "Ok"
    okButton = new JButton(ts.l("global.ok"));
    okButton.addActionListener(this);

    // "Cancel"
    cancelButton = new JButton(ts.l("global.cancel"));
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

    treeImages = new java.awt.Image[IMAGECOUNT];

    treeImages[OPENFOLDERICON] = PackageResources.getImageResource(this, "openfolder.gif", getClass());
    treeImages[CLOSEDFOLDERICON] = PackageResources.getImageResource(this, "folder.gif", getClass());
    treeImages[BASEICON] = PackageResources.getImageResource(this, "list.gif", getClass());
    treeImages[FIELDICON] = PackageResources.getImageResource(this, "i043.gif", getClass());
    treeImages[EMBEDDEDBASEICON] = PackageResources.getImageResource(this, "transredlist.gif", getClass());
    treeImages[OPENTAB] = PackageResources.getImageResource(this, "opentab.gif", getClass());
    treeImages[CLOSEDTAB] = PackageResources.getImageResource(this, "closedtab.gif", getClass());

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

    // "Schema Objects"
    JLabel leftL = new JLabel(ts.l("init.schemaObjectsLabel"));
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

    // "Create Category"
    createCategoryMI = new javax.swing.JMenuItem(ts.l("init.createCategoryMenu"));

    // "Delete Category"
    deleteCategoryMI = new javax.swing.JMenuItem(ts.l("init.deleteCategoryMenu"));

    // "Create Object Type"
    createObjectMI = new javax.swing.JMenuItem(ts.l("init.createBaseMenu"));

    // "Create Embedded Object Type"
    createInternalObjectMI = new javax.swing.JMenuItem(ts.l("init.createEmbeddedMenu"));

    categoryMenu.add(createCategoryMI);
    categoryMenu.add(deleteCategoryMI);
    categoryMenu.add(createObjectMI);
    categoryMenu.add(createInternalObjectMI);

    // namespace menu

    // "Namespace Menu"
    nameSpaceMenu = new treeMenu(ts.l("init.namespaceMenu"));

    // "Create Namespace"
    createNameMI = new javax.swing.JMenuItem(ts.l("init.createNamespaceMenu"));
    nameSpaceMenu.add(createNameMI);

    // namespace object menu

    nameSpaceObjectMenu = new treeMenu();

    // "Delete Namespace"
    deleteNameMI = new javax.swing.JMenuItem(ts.l("init.deleteNamespaceMenu"));
    nameSpaceObjectMenu.add(deleteNameMI);

    // base menu

    // "Object Menu"
    baseMenu = new treeMenu(ts.l("init.baseMenu"));

    // "Delete Object Type"
    deleteObjectMI = new javax.swing.JMenuItem(ts.l("init.deleteBaseMenu"));

    // "Create Tab"
    createTabMI = new javax.swing.JMenuItem(ts.l("init.createTabMenu"));

    baseMenu.add(deleteObjectMI);
    baseMenu.add(createTabMI);

    // tab menu

    // "Tab Menu"
    tabMenu = new treeMenu(ts.l("init.tabMenu"));

    // "Create Field"
    createFieldMI = new javax.swing.JMenuItem(ts.l("init.createFieldMenu"));

    tabMenu.add(createFieldMI);

    // "Embedded Object Menu"
    embeddedBaseMenu = new treeMenu(ts.l("init.embeddedBaseMenu"));

    // "Delete Object Type"
    deleteEmbeddedObjectMI = new javax.swing.JMenuItem(ts.l("init.deleteBaseMenu"));

    // "Create Field"
    createEmbeddedFieldMI = new javax.swing.JMenuItem(ts.l("init.createFieldMenu"));

    embeddedBaseMenu.add(deleteEmbeddedObjectMI);
    embeddedBaseMenu.add(createEmbeddedFieldMI);

    // field menu

    // "Field Menu"
    fieldMenu = new treeMenu(ts.l("init.fieldMenu"));

    // "Delete Field"
    deleteFieldMI = new javax.swing.JMenuItem(ts.l("init.deleteFieldMenu"));

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

    // "Namespaces"	
    namespaces = new treeNode(null, ts.l("init.namespaceNodeText"), objects, true, 0, 1, nameSpaceMenu);
    tree.insertNode(namespaces, false);
    
    nodeAfterCategories = namespaces;

    // and initialize tree

    initializeDisplayTree();

    pack();
    setSize(800, 600);
    this.setVisible(true);

    if (debug)
      {
	System.out.println("GASHSchema created");
      }

    // along with processWindowEvent(), this method allows us
    // to properly handle window system close events.

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
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
      prevNode;

    /* -- */

    c = node.getCategory();

    node.setText(c.getName());

    // get this category's children

    children = c.getNodes();

    prevNode = null;

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
				   true, EMBEDDEDBASEICON, EMBEDDEDBASEICON, embeddedBaseMenu);
	  }
	else
	  {
	    newNode = new BaseNode(parentNode, base.getName(), base, prevNode,
				   true, BASEICON, BASEICON, baseMenu);
	  }
      }
    else if (node instanceof Category)
      {
	Category category = (Category) node;

	newNode = new CatTreeNode(parentNode, category.getName(), category,
				  prevNode, true, OPENFOLDERICON, CLOSEDFOLDERICON, categoryMenu);
      }
    else
      {
        throw new RuntimeException("ASSERT: Unrecognized CategoryNode type.");
      }

    tree.insertNode(newNode, true);

    if (newNode instanceof BaseNode)
      {
	refreshFields((BaseNode)newNode, false, true);
      }

    return newNode;
  }

  /**
   * This method deletes all tab and field nodes contained under a
   * BaseNode in the tree and regenerates all the nodes from the
   * base's current fields on the server.
   */

  void refreshFields(BaseNode baseNode, boolean doRefresh, boolean openAllTabs) throws RemoteException
  {
    Base base;
    BaseField field;
    Vector vect;
    FieldNode oldNode, newNode;
    TabNode tabNode;
    boolean openAtStart;
    String tabString = null, oldTabString = null;
    boolean needTabs = true;
    Hashtable tabNodes = null;

    /* -- */

    // the tree.removeChildren call will collapse the folder if it is
    // open.. remember whether it (and any tab folders underneath it)
    // were open before our call so that we can restore the open
    // states before the tree is repainted

    openAtStart = baseNode.isOpen();

    if (!openAllTabs && baseNode.getChild() instanceof TabNode)
      {
	tabNodes = new Hashtable();
	tabNode = (TabNode) baseNode.getChild();

	while (tabNode != null)
	  {
	    tabNodes.put(tabNode.getText(), Boolean.valueOf(tabNode.isOpen()));
	    tabNode = (TabNode) tabNode.getNextSibling();
	  }
      }

    tree.removeChildren(baseNode, false);

    base = baseNode.getBase();

    if (base.isEmbedded())
      {
	needTabs = false;
      }

    // get the list of fields we want to display
    // note that we don't want to show built-in fields

    vect = base.getFields(false);

    // our algorithm is as follows.  we go over the list of fields
    // from the object base in display order.  at the start, we create
    // a tab node with the name of the tab string specified in the
    // first field in the object base.  If the first field has a
    // 'null' tab string, we'll use
    // ts.l("global.defaultTabName")

    // from this point on, we iterate through the list.  whenever we
    // look at a new field, we check the tab string.  if it is the
    // same as the last one, we'll just add the field.  if it
    // different, we'll pop up one level and add a new tab node to the
    // base children and then add the field there under.

    oldNode = null;
    tabNode = null;

    for (int i = 0; i < vect.size(); i++)
      {
	field = (BaseField) vect.elementAt(i);

	if (needTabs)
	  {
	    tabString = field.getTabName();

	    if (tabString == null || tabString.equals(""))
	      {
		tabString = ts.l("global.defaultTabName"); // "General"
	      }

	    if (!StringUtils.stringEquals(tabString, oldTabString))
	      {
		tabNode = new TabNode(baseNode, tabString, tabNode, // we insert after the old tabnode, if non-null
				      true, OPENTAB, CLOSEDTAB, tabMenu);

		tree.insertNode(tabNode, false);

		// if we previously saw this tab node and it was open,
		// keep it open here

		if (openAllTabs || (tabNodes != null &&tabNodes.containsKey(tabString) && ((Boolean) tabNodes.get(tabString)).booleanValue()))
		  {
		    tree.expandNode(tabNode, false);
		  }

		// since we're creating a new tab node for the rest of the
		// fields to be added with, we want to clear the oldNode
		// reference which we are using to handle setting
		// 'prevSibling' at field node creation time.

		oldNode = null;
		oldTabString = tabString;
	      }

	    newNode = new FieldNode(tabNode, field.getName(), field, oldNode,
				    false, FIELDICON, FIELDICON, fieldMenu);
	  }
	else
	  {
	    newNode = new FieldNode(baseNode, field.getName(), field, oldNode,
				    false, FIELDICON, FIELDICON, fieldMenu);
	  }

	tree.insertNode(newNode, false);

	oldNode = newNode;
      }

    if (openAtStart)
      {
	tree.expandNode(baseNode, false, false);
      }
      
    if (doRefresh)
      {
	tree.refresh();
      }
  }

  /**
   * This method scans nodes under a BaseNode in the tree, looking for
   * tabs and fields, and sets the appropriate tab names and field
   * ordering, using the tree as the template.
   *
   * This method basically does the reverse of refreshFields().
   */

  void syncFieldsFromTree(BaseNode bNode) throws RemoteException
  {
    Base base = bNode.getBase();
    TabNode tNode;
    FieldNode fNode;
    String previousFieldName;

    /* -- */

    if (!base.isEmbedded())
      {
	tNode = (TabNode) bNode.getChild();
	previousFieldName = null;

	while (tNode != null)
	  {
	    fNode = (FieldNode) tNode.getChild();

	    while (fNode != null)
	      {
		BaseField bF = fNode.getField();
		bF.setTabName(tNode.getText());
		
		base.moveFieldAfter(fNode.getText(), previousFieldName);
		previousFieldName = fNode.getText();
		
		fNode = (FieldNode) fNode.getNextSibling();
	      }

	    tNode = (TabNode) tNode.getNextSibling();
	  }
      }
    else
      {
	fNode = (FieldNode) bNode.getChild();
	previousFieldName = null;

	while (fNode != null)
	  {
	    BaseField bF = fNode.getField();
		
	    base.moveFieldAfter(fNode.getText(), previousFieldName);
	    previousFieldName = fNode.getText();
	    
	    fNode = (FieldNode) fNode.getNextSibling();
	  }
      }
  }

  void refreshNamespaces()
  { 
    boolean isOpen = namespaces.isOpen();
    
    tree.removeChildren(namespaces, false);

    try 
      {
        NameSpace[] spaces = null;

	spaces = editor.getNameSpaces();

        for (int i = 0; i < spaces.length ; i++)
          {
	    SpaceNode newNode = new SpaceNode(namespaces, spaces[i].getName(), spaces[i], 
					      null, false, BASEICON, BASEICON, nameSpaceObjectMenu);
	    tree.insertNode(newNode, true);
	  }
      }
    catch (RemoteException e)
      {
	System.out.println("Exception getting NameSpaces: " + e);
      }
    
    if (isOpen)
      {
	tree.expandNode(namespaces, false);
      }

    tree.refresh();
  }
  
  void editBase(BaseNode node)
  {
    if (showingField)
      {
	fe.switchAway();
      }

    be.editBase(node);
    card.show(attribCardPane,"base");
    showingBase = true;
    showingField = false;

    fe.fieldNode = null;
    
    validate();
  }

  void editField(FieldNode node)
  {
    if (showingField)
      {
	fe.switchAway();
      }

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
    if (showingField)
      {
	fe.switchAway();
      }

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
    if (showingField)
      {
	fe.switchAway();
      }

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

  void editTab(TabNode node)
  {
    if (showingField)
      {
	fe.switchAway();
      }

    te.editTab(node);
    card.show(attribCardPane, "tab");

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
    else if (node instanceof TabNode)
      {
	editTab((TabNode) node);
      }
    else if (node instanceof CatTreeNode)
      {
	editCategory((CatTreeNode) node);
      }
    else
      {
	if (showingField)
	  {
	    fe.switchAway();
	  }

	showingBase = false;
	showingField = false;

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
	if (attribCardPane != null)
	  {
	    card.show(attribCardPane, "empty");
	  }
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

  public void treeNodeMenuPerformed(treeNode node, java.awt.event.ActionEvent event)
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
						  n, true, OPENFOLDERICON, CLOSEDFOLDERICON, categoryMenu);

	    tree.insertNode(newNode, false);
	    tree.expandNode(node, false);
	    tree.refresh();

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
	    // "Error: Category not removable"
	    // "You are not allowed to remove the root category node."
	    new StringDialog(this,
			     ts.l("treeNodeMenuPerformed.mandatory_category"),
			     ts.l("treeNodeMenuPerformed.root_category_delete"),
			     ts.l("global.ok"),
			     null).showDialog();
	    return;
	  }

	try
	  {
	    if (category.getNodes().size() != 0)
	      {
		// "Error: Category not removable"
		// "This category has nodes under it.  You must remove the contents before deleting this category."
		new StringDialog(this,
				 ts.l("treeNodeMenuPerformed.mandatory_category"),
				 ts.l("treeNodeMenuPerformed.category_not_empty"),
				 ts.l("global.ok"),
				 null).showDialog();
		return;
	      }

	    // "Delete Category"
	    // "Are you sure you want to delete category {0}?"
	    dialogResource = new DialogRsrc(this,
					    ts.l("treeNodeMenuPerformed.delete_category"),
					    ts.l("treeNodeMenuPerformed.verify_delete_category", node.getText()),
					    ts.l("treeNodeMenuPerformed.deleteButton"), ts.l("global.cancel"),
					    questionImage);

	    Hashtable results = new StringDialog(dialogResource).showDialog();

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
					    null, true, BASEICON, BASEICON, baseMenu);

	    tree.insertNode(newNode, false);
	    tree.expandNode(node, false);
	    refreshFields(newNode, true, false);
	    tree.unselectAllNodes(true);
	    tree.selectNode(newNode); // triggers an editBase call on the node

	    TabNode genTabNode = new TabNode(newNode, ts.l("global.defaultTabName"), null,
					     true, OPENTAB, CLOSEDTAB, tabMenu);
	    tree.expandNode(newNode, false);
	    tree.insertNode(genTabNode, false);
	    tree.refresh();
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
					    null, true, EMBEDDEDBASEICON, EMBEDDEDBASEICON, embeddedBaseMenu);

	    tree.insertNode(newNode, false);
	    tree.expandNode(node, false);
	    refreshFields(newNode, true, false);
	    tree.unselectAllNodes(true);
	    tree.selectNode(newNode); // triggers an editBase call on the node
	    tree.refresh();
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

	// "Create New Namespace"
	// "Enter the name of the namespace you want to create, and select the checkbox if you want this namespace to be case insensitive."
	DialogRsrc dialogResource = new DialogRsrc(this,
						   ts.l("treeNodeMenuPerformed.create_namespace_title"),
						   ts.l("treeNodeMenuPerformed.create_namespace_text"),
						   ts.l("treeNodeMenuPerformed.createButton"),
						   ts.l("global.cancel"),
						   questionImage);

	dialogResource.addString(ts.l("treeNodeMenuPerformed.namespace_name_field")); // "Namespace:"
	dialogResource.addBoolean(ts.l("treeNodeMenuPerformed.namespace_case_field")); // "Case Insensitive:"

	Hashtable results = new StringDialog(dialogResource).showDialog();

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

	    Enumeration en = results.keys();

	    while (en.hasMoreElements()) 
	      {
		String label = (String) en.nextElement();
		Object ob = results.get(label);

		if (ob instanceof String) 
		  {
		    if (label.equals(ts.l("treeNodeMenuPerformed.namespace_name_field")))
		      {
			if (debug)
			  {
			    System.out.println("Namespace is " + (String)ob);
			  }

			newNameSpace = (String)ob;
		      }
		    else if (debug)
		      {
			System.out.println("Red alert!  unknown string returned: " + (String)ob);
		      }
		  }
		else if (ob instanceof Boolean)
		  {
		    Boolean bool = (Boolean)ob;

		    if (label.equals(ts.l("treeNodeMenuPerformed.namespace_case_field")))
		      {
			if (debug)
			  {
			    System.out.println("Sensitivity set to: " + bool);
			  }

			insensitive = bool;
		      }
		    else if (debug)
		      {
			System.out.println("Unknown Boolean returned by Dialog.");
		      }
		  }
		else if (debug)
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

	// "Confirm Name Space Deletion"
	// "Are you sure you want to delete the {0} namespace?"
	DialogRsrc dialogResource = new DialogRsrc(this,
						   ts.l("treeNodeMenuPerformed.deleteNamespaceTitle"),
						   ts.l("treeNodeMenuPerformed.deleteNamespaceText", node.getText()),
						   ts.l("treeNodeMenuPerformed.deleteButton"),
						   ts.l("global.cancel"),
						   questionImage);

	Hashtable results = new StringDialog(dialogResource).showDialog();

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
    else if (event.getSource() == deleteObjectMI || event.getSource() == deleteEmbeddedObjectMI)
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
	    // "Confirm Object Type Deletion"
	    // "Are you sure you want to delete the {0} object type?"

	    if (new StringDialog(this,
				 ts.l("treeNodeMenuPerformed.deleteObjectTitle"),
				 ts.l("treeNodeMenuPerformed.deleteObjectText", node.getText()),
				 ts.l("treeNodeMenuPerformed.deleteButton"),
				 ts.l("global.cancel")).showDialog() == null)
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
	    // "Object Type Deletion Error"
	    // "Sorry, you are not allowed to delete the {0} object type, as the Ganymede server is dependent on it for proper functioning."

	    new StringDialog(this,
			     ts.l("treeNodeMenuPerformed.badDeleteObjectTitle"),
			     ts.l("treeNodeMenuPerformed.badDeleteObjectText", node.getText()),
			     ts.l("global.ok"), null).showDialog();
	  }
      }
    else if (event.getSource() == createTabMI)
      {
	BaseNode bNode = (BaseNode) node;

	// create a name for the new tab

	String name = null;
	boolean safeName = false;

	int i = 1;

	while (!safeName)
	  {
	    if (i < 2)
	      {
		name = ts.l("treeNodeMenuPerformed.newTabName");	// "New Tab"
	      }
	    else
	      {
		name = ts.l("treeNodeMenuPerformed.newTabNumName", new Integer(i)); // "New Tab {0}"
	      }

	    TabNode tabNode = (TabNode) bNode.getChild();

	    safeName = true;

	    while (safeName && tabNode != null)
	      {
		if (tabNode.getText().equals(name))
		  {
		    safeName = false;
		  }

		tabNode = (TabNode) tabNode.getNextSibling();
	      }

	    i = i + 1;
	  }

	// we want to insert the new tab at the bottom of the object
	// display order

	treeNode n = bNode.getChild();
	    
	if (n != null)
	  {
	    while (n.getNextSibling() != null)
	      {
		n = n.getNextSibling();
	      }
	  }

	TabNode newNode = new TabNode(bNode, name, n,
				      true, OPENTAB, CLOSEDTAB, tabMenu);
	tree.insertNode(newNode, false);
	tree.expandNode(bNode, false);
	tree.unselectAllNodes(true);
	tree.selectNode(newNode); // to trigger the call back and bring up the editTab() pane
	tree.refresh();
      }
    else if (event.getSource() == deleteTabMI)
      {
	TabNode tNode = (TabNode) node;
	BaseNode bNode = (BaseNode) tNode.getParent();

	if (tNode.getPrevSibling() == null)
	  {
	    // "Tab Deletion Error"
	    // "Sorry, you are not allowed to delete the first tab in an object type."
	    new JErrorDialog(this,
			     ts.l("treeNodeMenuPerformed.badDeleteTabTitle"),
			     ts.l("treeNodeMenuPerformed.badDeleteTabText"));
	  }
	else if (tNode.getChild() != null)
	  {
	    // "Tab Deletion Error"
	    // "Error, the {0} tab in the {1} object type still contains fields."
	    new JErrorDialog(this,
			     ts.l("treeNodeMenuPerformed.badDeleteTabTitle"),
			     ts.l("treeNodeMenuPerformed.nonEmptyDeleteTabText", tNode.getText(), bNode.getText()));
	  }
	else
	  {
	    if (new StringDialog(this,
				 ts.l("treeNodeMenuPerformed.deleteTabTitle"),
				 ts.l("treeNodeMenuPerformed.deleteTabText", tNode.getText(), bNode.getText()),
				 ts.l("treeNodeMenuPerformed.deleteButton"),
				 ts.l("global.cancel")).showDialog() == null)
	      {
		if (debug)
		  {
		    System.out.println("Deletion canceled");
		  }
	      }
	    else //Returned confirm
	      {
		// we want to delete the tab node from the tree, then call
		// a function on the base node to fix up all of the tab
		// names under the base node.
		
		tree.deleteNode(tNode, false);

		try
		  {
		    syncFieldsFromTree(bNode);
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException(ex);
		  }
	      }
	  }
      }
    else if (event.getSource() == createFieldMI || event.getSource() == createEmbeddedFieldMI)
      {
	// createFieldMI is called on a tab node in the tree for
	// non-embedded objects, and on a baseNode for embedded
	// objects.

	try
	  {
	    if (node instanceof TabNode)
	      {
		TabNode tNode = (TabNode) node;
		BaseNode bNode = (BaseNode) tNode.getParent();
		FieldNode newNode = null;

		if (debug)
		  {
		    System.err.println("Calling editField");
		  }

		// create a name for the new field

		BaseField bF;
		Base b = bNode.getBase();

		bF = b.createNewField(); // the server picks a new default field name

		String name = bF.getName();

		bF.setTabName(tNode.getText());

		// we want to insert the child's field node at the bottom
		// of the tab, which is where createNewField() places it.

		treeNode pSiblingNode = tNode.getChild();
	    
		if (pSiblingNode != null)
		  {
		    while (pSiblingNode.getNextSibling() != null)
		      {
			pSiblingNode = pSiblingNode.getNextSibling();
		      }

		    newNode = new FieldNode(tNode, name, bF, pSiblingNode,
					    false, FIELDICON, FIELDICON, fieldMenu);
		  }
		else
		  {
		    // we're creating a field in an empty tab.  we
		    // can't compare its position with other fields in
		    // the tab, so we have to look around to find the
		    // field that this new field will go after

		    if (tNode.getPrevSibling() != null)
		      {
			TabNode prevTabNode = (TabNode) tNode.getPrevSibling();

			while (prevTabNode.getChild() == null && prevTabNode.getPrevSibling() != null)
			  {
			    prevTabNode = (TabNode) prevTabNode.getPrevSibling();
			  }

			if (prevTabNode.getChild() == null)
			  {
			    pSiblingNode = null;
			  }
			else
			  {
			    // we've found the next most previous tab
			    // node that has children.. find the last
			    // node in the tab

			    pSiblingNode = prevTabNode.getChild();
			    
			    while (pSiblingNode.getNextSibling() != null)
			      {
				pSiblingNode = pSiblingNode.getNextSibling();
			      }
			  }
		      }

		    // The tree node we're creating has no previous
		    // sibling under the tab node, so we'll pass null
		    // as the fourth param.  We'll just use the
		    // pSiblingNode below to figure out how to tell
		    // the server to order the fields in the display
		    // list.  Remember that tabs don't actually exist
		    // as any kind of container in the server
		    // structures.. we just mark each field in display
		    // order with a string describing the tab it
		    // belongs in

		    newNode = new FieldNode(tNode, name, bF, null,
					    false, FIELDICON, FIELDICON, fieldMenu);
		  }

		// we've put the node into the right place in the tree

		if (pSiblingNode == null)
		  {
		    b.moveFieldBefore(name, null);
		  }
		else
		  {
		    b.moveFieldAfter(name, pSiblingNode.getText());
		  }

		tree.insertNode(newNode, false);
		tree.expandNode(tNode, false);
		tree.unselectAllNodes(true);
		tree.selectNode(newNode); // to trigger the editField() call that brings up the new field in the pane
		tree.refresh();

		if (debug)
		  {
		    System.err.println("Called editField");
		  }
	      }
	    else
	      {
		BaseNode bNode = (BaseNode) node;

		if (debug)
		  {
		    System.err.println("Calling editField on embedded type");
		  }

		// create a name for the new field

		BaseField bF;
		Base b = bNode.getBase();

		bF = b.createNewField(); // the server picks a new default field name

		String name = bF.getName();

		// we want to insert the child's field node at the
		// bottom of the object, which is where
		// createNewField() places it.

		treeNode n = bNode.getChild();
	    
		if (n != null)
		  {
		    while (n.getNextSibling() != null)
		      {
			n = n.getNextSibling();
		      }
		  }

		FieldNode newNode = new FieldNode(bNode, name, bF, n,
						  false, FIELDICON, FIELDICON, fieldMenu);
		tree.insertNode(newNode, false);
		tree.expandNode(bNode, false);
		tree.unselectAllNodes(true);
		tree.selectNode(newNode); // to trigger the editField() call that brings up the new field in the pane
		tree.refresh();

		if (debug)
		  {
		    System.err.println("Called editField");
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("couldn't create new field" + ex);
	  }
      }
    else if (event.getSource() == deleteFieldMI)
      {
	String label, baseLabel;
	TabNode tNode;
	BaseNode bNode;
	FieldNode fNode;

	/* -- */

	if (node.getParent() instanceof BaseNode)
	  {
	    tNode = null;
	    bNode = (BaseNode) node.getParent();
	  }
	else
	  {
	    tNode = (TabNode) node.getParent();
	    bNode = (BaseNode) tNode.getParent();
	  }

	fNode = (FieldNode) node;

	label = fNode.getText();
	baseLabel = bNode.getText();

	// "Confirm Field Deletion"
	// "Are you sure you want to delete the {0} field from the {1} object type?"

	DialogRsrc dialogResource = new DialogRsrc(this,
						   ts.l("treeNodeMenuPerformed.deleteFieldTitle"),
						   ts.l("treeNodeMenuPerformed.deleteFieldText", label, baseLabel),
						   ts.l("treeNodeMenuPerformed.deleteButton"),
						   ts.l("global.cancel"),
						   questionImage);

	Hashtable results = new StringDialog(dialogResource).showDialog();

	if (results != null)
	  {
	    try
	      {
		ReturnVal retVal = bNode.getBase().deleteField(fNode.getText());
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    handleReturnVal(retVal);
		    return;
		  }
		
		refreshFields(bNode, true, false);
		ne.refreshSpaceList();
		be.refreshLabelChoice();
	      }
	    catch (RemoteException ex)
	      {
		// "Field Deletion Error"
		// "An exception was caught from the server while trying to delete the {0} field from the {1} object type:\n{2}"
		new JErrorDialog(this,
				 ts.l("treeNodeMenuPerformed.badDeleteFieldTitle"),
				 ts.l("treeNodeMenuPerformed.badDeleteFieldText", label, baseLabel, ex));
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
	commit();
      }
    else if (event.getSource() == cancelButton)
      {
	cancel();
      }
    else
      {
	System.err.println("Unknown Action Performed in GASHSchema");
      }
  }

  /**
   * Make sure that we clean up and get rid of our
   * remote references to the server's schema editing
   * objects if our window is closed on us.
   */

  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	super.processWindowEvent(e); // go ahead and close it, please
	cleanup();
      }
    else
      {
	super.processWindowEvent(e);
      }
  }

  public void commit()
  {
    if (editor == null)
      {
	return;
      }

    try
      {
	ReturnVal retVal = editor.commit();

	if (retVal != null)
	  {
	    handleReturnVal(retVal);
	    return;
	  }

	editor = null;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't commit: " + ex);
      }

    setVisible(false);
    cleanup();
  }

  public void cancel()
  {
    if (editor == null)
      {
	return;
      }

    try
      {
	editor.release();
	editor = null;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't release: " + ex);
      }

    setVisible(false);
    cleanup();
  }

  /**
   * GC-aiding dissolution method.  Should be called after the
   * schema editor window has been removed from view, on the GUI
   * thread.
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

    /*
      let's ditch the fucking RMI references in the tree's nodes,
      please.  i'm tired of the schema editor keeping memory pinned
      in the Ganymede server.
    */

    this.tree.destroyTree();
    this.tree.dispose();
    this.tree = null;

    this.editor = null;
    this.dispatch.clearSchemaEditor();
    this.dispatch = null;

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
	    ((dragNode instanceof TabNode &&
	      (dragNode.getPrevSibling() != null ||
	       dragNode.getNextSibling() != null))) ||
	    (dragNode instanceof BaseNode) ||
	    (dragNode instanceof CatTreeNode &&
	     dragNode != objects));
  }

  /**
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
			       " over " + targetNode.getText() + " because node is open.");
	  }

	return false;
      }

    if (dragNode instanceof FieldNode)
      {
	if (dragNode.getParent() instanceof TabNode)
	  {
	    BaseNode bNode = (BaseNode) dragNode.getParent().getParent();

	    if (targetNode instanceof TabNode && targetNode.getParent() == bNode)
	      {
		return true;
	      }
	  }

	if (debug)
	  {
	    System.err.println("iconDragOver(): failing " + dragNode.getText() + 
			       " over " + targetNode.getText() + " because can't drag over field nodes");
	  }

	return false;
      }

    if (dragNode instanceof TabNode)
      {
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
				   " over " + targetNode.getText() + " because can't drag category over non-category");
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
		       " over " + targetNode.getText() + ", don't recognize the drag node type");
    
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

		refreshFields(newNode, true, false);

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
	    
	    Category newCategory = null;

	    if (targetNode instanceof CatTreeNode)
	      {
		// it had better be

		newCategory = ((CatTreeNode) targetNode).getCategory();

		if (debug)
		  {
		    System.err.println("Dropping category " + category.getName() + " from " +
				       category.getCategory().getName() + " onto " + newCategory.getName());
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
    else if (dragNode instanceof FieldNode)
      {
	try
	  {
	    if (!(targetNode instanceof TabNode))
	      {
		throw new RuntimeException("what?  field node dropped on a non-tab node..");
	      }

	    FieldNode fNode = (FieldNode) dragNode;
	    TabNode tNode = (TabNode) targetNode;

	    tree.moveNode(dragNode, targetNode, null, true);
	    syncFieldsFromTree((BaseNode) tNode.getParent());
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }
  }

  /**
   * Method to control whether the drag line may be moved between a pair of given
   * nodes.
   * @see arlut.csd.JTree.treeDragDropCallback
   */

  public boolean dragLineTween(treeNode dragNode, treeNode aboveNode, treeNode belowNode)
  {
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

    if (dragNode instanceof TabNode)
      {
	BaseNode bNode = (BaseNode) dragNode.getParent();

	if (aboveNode instanceof BaseNode)
	  {
	    return aboveNode == bNode;
	  }

	if (aboveNode instanceof TabNode)
	  {
	    return aboveNode.getParent() == bNode && !aboveNode.isOpen();
	  }

	if (aboveNode instanceof FieldNode)
	  {
	    if (belowNode instanceof TabNode)
	      {
		return belowNode.getParent() == bNode;
	      }
	    else if (aboveNode.getNextSibling() == null && aboveNode.getParent() != dragNode && aboveNode.getParent().getParent() == bNode)
	      {
		return true;
	      }
	    else
	      {
		return false;
	      }
	  }

	return false;
      }
    else if (dragNode instanceof FieldNode)
      {
	TabNode tNode;
	BaseNode bNode;

	if (dragNode.getParent() instanceof TabNode)
	  {
	    tNode = (TabNode) dragNode.getParent();
	    bNode = (BaseNode) tNode.getParent();
	  }
	else
	  {
	    tNode = null;
	    bNode = (BaseNode) dragNode.getParent();
	  }

	if (aboveNode instanceof BaseNode && tNode != null)
	  {
	    return false;
	  }

	if (aboveNode instanceof BaseNode && aboveNode != bNode)
	  {
	    return false;
	  }
	
	if (aboveNode instanceof TabNode)
	  {
	    TabNode aboveTabNode = (TabNode) aboveNode;

	    if (aboveTabNode.getParent() != bNode || !aboveTabNode.isOpen())
	      {
		return false;
	      }
	  }

	if (aboveNode instanceof FieldNode)
	  {
	    BaseNode aboveBaseNode = null;

	    if (aboveNode.getParent() instanceof TabNode)
	      {
		// non-embedded field above

		aboveBaseNode = (BaseNode) aboveNode.getParent().getParent();
	      }
	    else if (aboveNode.getParent() instanceof BaseNode)
	      {
		// embedded field above

		aboveBaseNode = (BaseNode) aboveNode.getParent();
	      }

	    if (aboveBaseNode != bNode || !aboveBaseNode.isOpen())
	      {
		return false;
	      }
	  }

	if (!(aboveNode instanceof BaseNode || aboveNode instanceof TabNode || aboveNode instanceof FieldNode))
	  {
	    return false;
	  }

	return true;
      }
    else if (dragNode instanceof BaseNode)
      {
	if (belowNode instanceof FieldNode)
	  {
	    return false;
	  }

	if (belowNode instanceof TabNode)
	  {
	    return false;
	  }

	if (belowNode == nodeAfterCategories)
	  {
	    return true;
	  }

	if (aboveNode instanceof FieldNode)
	  {
	    if (aboveNode.getParent() instanceof TabNode)
	      {
		if (aboveNode.getParent().getParent() == dragNode)
		  {
		    return false;
		  }
	      }
	    else if (aboveNode.getParent() == dragNode)
	      {
		return false;
	      }

	    return true;
	  }

	return ((aboveNode instanceof BaseNode) || 
		(aboveNode instanceof CatTreeNode) ||
		(belowNode instanceof BaseNode) || 
		(belowNode instanceof CatTreeNode));
      }
    else if (dragNode instanceof CatTreeNode)
      {
	if (belowNode instanceof FieldNode)
	  {
	    return false;
	  }

	if (belowNode instanceof TabNode)
	  {
	    return false;
	  }

	// if we're already the last category, we don't want to
	// drag to the bottom

	if (belowNode == nodeAfterCategories && dragNode.getNextSibling() != null)
	  {
	    return true;
	  }

	if (aboveNode instanceof CatTreeNode)
	  {
	    return !aboveNode.isUnder(dragNode);
	  }
	    
	if (belowNode instanceof CatTreeNode)
	  {
	    return !belowNode.isUnder(dragNode);
	  }

	if (aboveNode instanceof BaseNode)
	  {
	    return !aboveNode.isUnder(dragNode);
	  }
  
	if (belowNode instanceof BaseNode)
	  {
	    return !belowNode.isUnder(dragNode);
	  }
		    
	return false;
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

    try
      {
	if (dragNode instanceof FieldNode)
	  {
	    TabNode tNode;
	    BaseNode bNode;
	    FieldNode oldNode = (FieldNode) dragNode;

	    if (oldNode.getParent() instanceof TabNode)
	      {
		tNode = (TabNode) oldNode.getParent();
		bNode = (BaseNode) tNode.getParent();
	      }
	    else
	      {
		tNode = null;
		bNode = (BaseNode) oldNode.getParent();
	      }

	    Base base = bNode.getBase();

	    if (debug)
	      {
		System.out.println("base = " + bNode);
	      }
	
	    if (aboveNode instanceof FieldNode)
	      {
		TabNode aboveTabNode;
		BaseNode aboveBaseNode;

		/* -- */

		if (aboveNode.getParent() instanceof TabNode)
		  {
		    aboveTabNode = (TabNode) aboveNode.getParent();
		    aboveBaseNode = (BaseNode) aboveTabNode.getParent();
		  }
		else
		  {
		    aboveTabNode = null;
		    aboveBaseNode = (BaseNode) aboveNode.getParent();
		  }

		base.moveFieldAfter(dragNode.getText(), aboveNode.getText());

		FieldNode newNode = (FieldNode) tree.moveNode(dragNode, aboveTabNode != null ? (treeNode) aboveTabNode: (treeNode) aboveBaseNode, aboveNode, true);

		if (aboveTabNode != null)
		  {
		    BaseField bF = newNode.getField();
		    bF.setTabName(aboveTabNode.getText());
		  }

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
		  }
	      }
	    else if (aboveNode instanceof TabNode)
	      {
		TabNode aboveTabNode = (TabNode) aboveNode;

		if (belowNode instanceof FieldNode)
		  {
		    base.moveFieldBefore(dragNode.getText(), belowNode.getText());
		  }
		else
		  {
		    // we need to make this field the first field in the
		    // tab, but we don't know where the field goes in the
		    // display order, since this tab has no fields under
		    // it that we can easily use for comparison.
		    //
		    // so we'll have to root around a little bit to find
		    // our bearings.

		    TabNode prevTabNode = (TabNode) aboveTabNode.getPrevSibling();

		    while (prevTabNode != null && prevTabNode.getChild() == null)
		      {
			prevTabNode = (TabNode) prevTabNode.getPrevSibling();
		      }

		    if (prevTabNode == null)
		      {
			// move the field to the top of the display list
			base.moveFieldAfter(dragNode.getText(), null);
		      }
		    else
		      {
			// okay, we've got a preceding tab node that has
			// fields under it.. find the last one

			FieldNode prevTabFNode = (FieldNode) prevTabNode.getChild();
		    
			while (prevTabFNode.getNextSibling() != null)
			  {
			    prevTabFNode = (FieldNode) prevTabFNode.getNextSibling();
			  }

			base.moveFieldAfter(dragNode.getText(), prevTabFNode.getText());
		      }
		  }

		// okay, we've fixed up the field ordering on the server.
		// go ahead and move the field node in the tree

		FieldNode newNode = (FieldNode) tree.moveNode(dragNode, aboveTabNode, null, true);

		BaseField bF = newNode.getField();
		bF.setTabName(tNode.getText());

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
		  }
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		// the dragLineTween routine will only allow a field to be
		// dragged under a an object base node if it is contained
		// in an embedded object

		BaseNode aboveBaseNode = (BaseNode) aboveNode;

		base.moveFieldAfter(dragNode.getText(), null);

		FieldNode newNode = (FieldNode) tree.moveNode(dragNode, aboveBaseNode, null, true);

		if (fe.fieldNode == dragNode)
		  {
		    fe.fieldNode = newNode;
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

	    BaseNode bn = (BaseNode) dragNode;
	    Base base = bn.getBase();

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

		if (aboveNode.getParent() instanceof TabNode)
		  {
		    newParent = (CatTreeNode) aboveNode.getParent().getParent().getParent();
		  }
		else
		  {
		    newParent = (CatTreeNode) aboveNode.getParent().getParent();
		  }
		previousNode = aboveNode.getParent();
	      }
            else
              {
                // for FindBugs' sake.. iconDragOver() and
                // dragLineTween() should prevent this

                throw new RuntimeException("ASSERT: aboveNode is an invalid type");
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

	    refreshFields(newNode, true, false);

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
	else if (dragNode instanceof TabNode)
	  {
	    BaseNode bNode = (BaseNode) dragNode.getParent();
	    TabNode newNode;

	    if (aboveNode instanceof TabNode)
	      {
		newNode = (TabNode) tree.moveNode(dragNode, bNode, aboveNode, true);
	      }
	    else if (aboveNode instanceof FieldNode)
	      {
		newNode = (TabNode) tree.moveNode(dragNode, bNode, aboveNode.getParent(), true);
	      }
	    else if (aboveNode instanceof BaseNode)
	      {
		newNode = (TabNode) tree.moveNode(dragNode, bNode, null, true);
	      }
	    else
	      {
		throw new RuntimeException("bad drag target location for tabnode tween drag");
	      }

	    syncFieldsFromTree(bNode);

	    if (te.tabNode == dragNode)
	      {
		te.tabNode = newNode;
	      }
	  }
	else if (dragNode instanceof CatTreeNode)
	  {
	    if (debug)
	      {
		System.err.println("Releasing CatTreeNode");
	      }

	    CatTreeNode cn = (CatTreeNode) dragNode;
	    Category category = cn.getCategory();

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
		newParent = (CatTreeNode) aboveNode.getParent().getParent().getParent();
		previousNode = aboveNode.getParent();
	      }
            else
              {
                // for FindBugs' sake.. iconDragOver() and
                // dragLineTween() should prevent this

                throw new RuntimeException("ASSERT: aboveNode is an invalid type");
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
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * This method takes a ReturnVal object from the server and, if
   * necessary, runs through a wizard interaction sequence, possibly
   * displaying several dialogs before finally returning a final
   * result code.
   *
   * Use the ReturnVal returned from this function after this function
   * is called to determine the ultimate success or failure of any
   * operation which returns ReturnVal, because a wizard sequence may
   * determine the ultimate result.
   *
   * This method should not be synchronized, since handleReturnVal may
   * pop up modal (thread-blocking) dialogs, and if we we synchronize
   * this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.
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

	DialogRsrc resource = jdialog.extractDialogRsrc(this, null);

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
	    
	dialogResults = dialog.showDialog();

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
