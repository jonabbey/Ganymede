/*

   GASHSchema.java

   Schema editing frame to work in conjunction with the
   Admin console.
   
   Created: 24 April 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;
import arlut.csd.DataComponent.*;

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
import arlut.csd.Dialog.YesNoDialog;

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
    deleteNameMI = null;

  Panel 
    displayPane,
    buttonPane,
    attribPane,
    attribEditPane,
    attribButtonPane;

  Button
    okButton, cancelButton, attribOkButton;

  GridBagLayout 
    gbl;

  GridBagConstraints
    gbc;

  Toolkit
    toolkit;

  Base[] bases;

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

    attribEditPane = new Panel();
    attribEditPane.setBackground(Color.white);
    attribEditPane.setLayout(new BorderLayout());

    attribButtonPane = new Panel();
    attribButtonPane.setBackground(Color.white);
    attribButtonPane.setLayout(new RowLayout());

    attribPane.add("Center", attribEditPane);
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
    createObjectMI = new MenuItem("Create Object");
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
    deleteObjectMI = new MenuItem("Delete Object");
    
    objectsRefresh();

    pack();
    show();
  }

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


  void objectsRefresh()
  {
    Base base;
    treeNode parentNode, oldNode, newNode;
    boolean wasOpen;
    String baseName = null;

    /* -- */

    PopupMenu menu = new PopupMenu();
    menu.add(deleteObjectMI);

    wasOpen = objects.isOpen();
    tree.removeChildren(objects, false);

    try
      {
	bases = editor.getBases();
      }
    catch (RemoteException ex)
      {
	System.err.println("GASHSchema: objectsRefresh(): couldn't get bases");
	System.err.println(ex.toString());

	throw new RuntimeException("couldn't get bases" + ex);
      }

    (new QuickSort(bases,  this)).sort();

    parentNode = objects;
    oldNode = null;

    for (int i = 0; i < bases.length; i++)
      {
	base = bases[i];
	
	try
	  {
	    baseName = base.getName();
	  }
	catch (RemoteException ex)
	  {
	    System.err.println("GASHSchema: objectsRefresh(): couldn't get base name");
	    System.err.println(ex.toString());

	    throw new RuntimeException("couldn't get base name" + ex);
	  }
	catch (Exception ex)
	  {
	    System.err.println("i = " + i + ", bases.length = " + bases.length + ", ex = " + ex);
	  }

	if (baseName == null)
	  {
	    throw new NullPointerException("baseName == null");
	  }

	newNode = new treeNode(parentNode, baseName, oldNode,
			       true, 0, 1, menu);

	tree.insertNode(newNode, false);
	
	oldNode = newNode;
      }

    if (wasOpen)
      {
	tree.expandNode(objects, false);
      }

    tree.refresh();
  }

  void namespacesRefresh()
  {
    // tree.removeChildren(namespaces, false);
  }

  void editBase(Base base)
  {
    BaseEditor be;

    /* -- */

    attribEditPane.removeAll();

    be = new BaseEditor(this, base);
    attribEditPane.add("Center", be);
    
    attribButtonPane.removeAll();
    attribOkButton = new Button("ok");
    attribOkButton.addActionListener(be);
    attribButtonPane.add(attribOkButton);

    displayPane.validate();
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

    n = node.getParent();

    while (n != null && n != objects && n!= namespaces)
      {
	n = n.getParent();
      }

    if (n == objects)
      {
	a = node.getText();

	System.out.println("object node " + a + " selected");

	objectsRefresh();

	if (bases != null)
	  {
	    System.err.println("Searching for matching base");

	    try
	      {
		for (int i = 0; i < bases.length; i++)
		  {
		    b = bases[i].getName();

		    System.err.println("comparing vs i=" + i + "(" + b + ")");
		    if (b.equals(a))
		      {
			editBase(bases[i]);
			return;
		      }
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote exception getting base match for " + 
					   node.getText() + ", " + ex);
	      }
	  }
	else
	  {
	    System.err.println("null bases");
	  }
      }
  }

  public void treeNodeUnSelected(treeNode node)
  {
    System.out.println("node " + node.getText() + " unselected");
  }

  public void treeNodeMenuPerformed(treeNode node,
				    java.awt.event.ActionEvent event)
  {
    System.out.println("node " + node.getText() + ", action: " + event );

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

  BaseEditor(GASHSchema owner, Base base)
  {
    if (base == null)
      {
	throw new IllegalArgumentException("base must not be null");
      }

    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    System.err.println("BaseEditor constructed");

    this.base = base;
    this.owner = owner;

    editPanel = new Panel();
    editPanel.setLayout(new ColumnLayout());
    
    ca = new componentAttr(this, new Font("SansSerif", Font.BOLD, 12),
			   Color.black, Color.white);

    typeN = new numberField(20, ca, false, false, 0, 0);
    typeN.setCallback(this);
    
    try
      {
	typeN.setValue(base.getTypeID());
      }
    catch (RemoteException ex)
      {
	System.err.println("BaseEditor constructor: base.getTypeID failed: " + ex);
      }

    editPanel.add(new FieldWrapper("Object Type ID:", typeN));

    nameS = new stringField(20, 100, ca, true, false, null, null);
    nameS.setCallback(this);

    try
      {
	nameS.setText(base.getName());
      }
    catch (RemoteException ex)
      {
	System.err.println("BaseEditor constructor: base.getName failed: " + ex);
      }

    editPanel.add(new FieldWrapper("Object Type:", nameS));

    classS = new stringField(20, 100, ca, true, false, null, null);
    classS.setCallback(this);

    try
      {
	classS.setText(base.getClassName());
      }
    catch (RemoteException ex)
      {
	System.err.println("BaseEditor constructor: base.getClassName failed: " + ex);
      }

    editPanel.add(new FieldWrapper("Class name:", classS));

    add(editPanel);
    //    doLayout();
  }

  public boolean setValuePerformed(ValueObject v)
  {
    Component source;
    String val;

    /* -- */

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

class BaseFieldEditor extends ScrollPane implements setValueCallback {

  BaseFieldEditor(BaseField fieldDef)
  {
  }

  public boolean setValuePerformed(ValueObject v)
  {
    return true;
  }
}
