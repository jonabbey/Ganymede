/*

   DBBaseCategory.java

   This module represents an objectbase folder in the server's
   category hierarchy.
   
   Created: 11 August 1997
   Release: $Name:  $
   Version: $Revision: 1.30 $
   Last Mod Date: $Date: 2002/03/29 03:57:56 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
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

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import com.jclark.xml.output.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  DBBaseCategory

------------------------------------------------------------------------------*/

/**
 * <P>A DBBaseCategory is a 'red folder node' in the server's category and object
 * hierarchy.  The purpose of DBBaseCategory is to be able to group object
 * types with related purpose into a common folder for display on the client.</P>
 *
 * <P>The {@link arlut.csd.ganymede.DBStore DBStore} contains a tree of 
 * {@link arlut.csd.ganymede.CategoryNode CategoryNode}s, each of which is either
 * a {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} or a DBBaseCategory.  The
 * {@link arlut.csd.ganymede.Category Category} RMI interface is used by the
 * server and the schema editor to perform browsing and manipulating the server's
 * category tree.</P>
 */

public class DBBaseCategory extends UnicastRemoteObject implements Category, CategoryNode {

  private final static boolean debug = false;

  private static arlut.csd.Util.Compare comparator =
    new arlut.csd.Util.Compare() {
    public int compare(Object a, Object b) 
      {
	int valA, valB;

	if (a instanceof DBBaseCategory)
	  {
	    valA = ((DBBaseCategory) a).tmp_displayOrder;
	  }
	else
	  {
	    valA = ((DBObjectBase) a).tmp_displayOrder;
	  }

	if (b instanceof DBBaseCategory)
	  {
	    valB = ((DBBaseCategory) b).tmp_displayOrder;
	  }
	else
	  {
	    valB = ((DBObjectBase) b).tmp_displayOrder;
	  }

	if (valA < valB)
	  {
	    return -1;
	  }
	else if (valB > valA)
	  { 
	    return 1;
	  } 
	else
	  { 
	    return 0;
	  }
      }
  };

  //

  private String name;
  private DBBaseCategory parent;
  private DBStore store;

  /**
   * The actual members of this category.  Each member will be either
   * a {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} or another
   * {@link arlut.csd.ganymede.DBBaseCategory DBBaseCategory}.
   */

  private Vector contents;

  /**
   * In order to keep compatibility with versions 1.17 and previous of
   * the ganymede.db file format, we'll keep this field so we can do a
   * sort after loading when reading an old file.
   */


  int tmp_displayOrder = -1;

  /**
   *
   * We use this baseHash to keep a map of DBObjectBase.getKey() to
   * instances of DBObjectBase.  addNodeAfter() uses this to find a
   * server-local DBObjectBase from a remote Base reference passed
   * us by the schema editor on the client.
   *
   */

  private Hashtable baseHash;

  /**
   *
   * A reference to the DBSchemaEdit object that is editing us
   * for a client-side schema editor.
   *
   */

  private DBSchemaEdit editor = null;

  /* -- */

  /**
   * Primary constructor.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   *              for us.
   * @param name Name for this base
   * @param parent If we're not being constructed at the top level, who is our parent?
   */

  public DBBaseCategory(DBStore store, String name, DBBaseCategory parent) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    this.setName(name);
    this.store = store;
    this.parent = parent;

    // All children of a top-level baseHash generated by recurseDown will
    // share a single baseHash so they can properly re-install a base if it
    // is moved around in the tree.

    if (parent != null)
      {
	this.baseHash = parent.baseHash;
	this.editor = parent.editor;
      }

    contents = new Vector();
  }

  /**
   * Default value constructor.  This is used to construct a top-level category.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   *              for us.
   * @param name Name for this category
   */

  public DBBaseCategory(DBStore store, String name) throws RemoteException
  {
    this(store, name, null);
  }

  /**
   * Receive constructor.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   *              for us.
   * @param in DataInput stream to load the db representation of this category from
   */

  public DBBaseCategory(DBStore store, DataInput in) throws RemoteException, IOException
  {
    super();			// UnicastRemoteObject initialization

    this.store = store;
    contents = new Vector();
    receive(in, null);
  }

  /**
   * Receive constructor.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   *              for us.
   * @param in DataInput stream to load the db representation of this category from
   */

  public DBBaseCategory(DBStore store, DataInput in, DBBaseCategory parent) throws RemoteException, IOException
  {
    super();			// UnicastRemoteObject initialization

    this.store = store;
    contents = new Vector();
    receive(in, parent);
  }

  /**
   * Recursive duplication constructor.  This constructor recurses down
   * through the newly created DBBaseCategory and creates copies of
   * the bases and categories therein.
   */

  public DBBaseCategory(DBStore store, DBBaseCategory rootCategory,
			Hashtable baseHash, DBSchemaEdit editor) throws RemoteException
  {
    this.editor = editor;
    this.store = store;
    contents = new Vector();

    this.baseHash = baseHash;

    setName(rootCategory.getName());
    
    recurseDown(rootCategory, baseHash, editor);
  }

  /**
   * <p>This method takes all the children of the passed-in category
   * (both {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} objects
   * and contained {@link arlut.csd.ganymede.DBBaseCategory DBBaseCategory}
   * objects) and makes copies under this.</p>
   */

  private void recurseDown(DBBaseCategory category, Hashtable baseHash,
			   DBSchemaEdit editor) throws RemoteException
  {
    Vector children = category.getNodes();
    CategoryNode node;
    DBObjectBase oldBase, newBase;
    DBBaseCategory oldCategory, newCategory;

    /* -- */

    if (debug)
      {
	Ganymede.debug("** recurseDown");

	if (editor == null)
	  {
	    Ganymede.debug("**#?!?!!! DBBaseCategory.recurseDown(): editor == null!!!");
	  }
      }

    for (int i = 0; i < children.size(); i++)
      {
	node = (CategoryNode) children.elementAt(i);

	if (node instanceof DBObjectBase)
	  {
	    oldBase = (DBObjectBase) node;

	    // a new copy, with the same objects under it

	    newBase = new DBObjectBase(oldBase, editor); 
	    baseHash.put(newBase.getKey(), newBase);

	    if (false)
	      {
		Ganymede.debug("Created newBase " + newBase.getName() + 
			       " in recursive category tree duplication");
	      }

	    // we want this base to be added to the current end of this category

	    addNodeAfter(newBase, null);

	    if (false)
	      {
		Ganymede.debug("Added " + newBase.getName() + " to new category tree");
	      }
	  }
	else if (node instanceof DBBaseCategory)
	  {
	    oldCategory = (DBBaseCategory) node;
	    newCategory = (DBBaseCategory) newSubCategory(oldCategory.getName());
	    newCategory.editor = editor;

	    if (false)
	      {
		Ganymede.debug("Created newCategory " + newCategory.getName() + 
			       " in recursive category tree duplication");
	      }

	    newCategory.recurseDown(oldCategory, baseHash, editor);
	  }
      }
  }


  /**
   * This method is used when a schema editor is 'checking in'
   * a category tree.
   */

  public synchronized void clearEditor()
  {
    CategoryNode node;

    /* -- */

    this.editor = null;

    for (int i = 0; i < contents.size(); i++)
      {
	node = (CategoryNode) contents.elementAt(i);

	if (node instanceof DBBaseCategory)
	  {
	    ((DBBaseCategory) node).clearEditor();
	  }
      }
  }

  /**
   * <p>Recursively prints a portion of an HTML
   * representation of this category
   * to &lt;out&gt;  Must be called from
   * {@link arlut.csd.ganymede.DBStore#printCategoryTreeHTML(java.io.PrintWriter)
   * DBStore.printCategoryTreeHTML()}, as it assumes the HTML context
   * generated by that method.</p>
   */

  public synchronized void printHTML(PrintWriter out)
  {
    out.print("<H2>");
    out.print(getName());
    out.println("</H2>");

    out.println("<UL>");

    for (int i = 0; i < contents.size(); i++)
      {
	out.println("<LI>");

	if (contents.elementAt(i) instanceof DBBaseCategory)
	  {
	    ((DBBaseCategory) contents.elementAt(i)).printHTML(out);
	  }
	else if (contents.elementAt(i) instanceof DBObjectBase)
	  {
	    ((DBObjectBase) contents.elementAt(i)).printHTML(out);
	  }

	out.println("</LI>");
      }

    out.println("</UL>");
  }

  /**
   * <p>Recursively prints a portion of a text
   * representation of this category
   * to &lt;out&gt;, with leading indent.</p>
   *
   * @param out
   * @param indent leading indent for this category and below
   * @param showAll if true, show built-in field types
   */

  public synchronized void print(PrintWriter out, String indent)
  {
    out.println(indent + getName());

    for (int i = 0; i < contents.size(); i++)
      {
	if (contents.elementAt(i) instanceof DBBaseCategory)
	  {
	    ((DBBaseCategory) contents.elementAt(i)).print(out, indent + "  ");
	  }
	else if (contents.elementAt(i) instanceof DBObjectBase)
	  {
	    ((DBObjectBase) contents.elementAt(i)).print(out, indent + "  ");
	  }
      }
  }

  /**
   * <p>Emits this category and its contents to &lt;out&gt;, in
   * ganymede.db form.</p>
   */

  synchronized void emit(DataOutput out) throws IOException
  {
    Object element;

    /* -- */

    out.writeUTF(this.getPath());
    out.writeInt(contents.size());

    for (int i = 0; i < contents.size(); i++)
      {
	element = contents.elementAt(i);

	// in DBStore 2.0 and later, we emit all bases during our
	// DBBaseCategory dump.

	if (element instanceof DBBaseCategory)
	  {
	    out.writeBoolean(false); // it's a category
	    ((DBBaseCategory) element).emit(out);
	  }
	else if (element instanceof DBObjectBase)
	  {
	    out.writeBoolean(true); // it's a base
	    ((DBObjectBase) element).emit(out, true);
	  }
      }
  }

  /**
   * <p>Reads this category and its contents from &lt;in&gt;, in
   * ganymede.db form.</p>
   */

  synchronized void receive(DataInput in, DBBaseCategory parent) throws IOException
  {
    String 
      pathName,
      path;

    int 
      count,
      lastSlash;

    /* -- */

    // read in category name

    pathName = in.readUTF();

    // we stopped using an explicitly stored display order field at
    // DBStore 2.0
    
    if (store.isLessThan(2,0))
      {
	tmp_displayOrder = in.readInt();
      }
    else
      {
	tmp_displayOrder = -1;
      }

    // now parse our path name to get our path

    lastSlash = pathName.lastIndexOf('/');
    path = pathName.substring(0, lastSlash);
    
    // and take our leaf's name

    name = pathName.substring(lastSlash + 1);

    // and get our parent

    this.parent = parent;

    count = in.readInt();

    if (false)
      {
	System.err.println("DBBaseCategory.receive(): reading in " + count + 
			   " subcategories and bases");
      }

    for (int i = 0; i < count; i++)
      {
	// starting at 2.0, we started reading DBObjectBases in during
	// category reading.

	if (store.isAtLeast(2,0))
	  {
	    if (in.readBoolean())
	      {
		DBObjectBase tempBase = new DBObjectBase(in, store);

		store.setBase(tempBase); // register in DBStore objectBases hash

		// we want to add this node to the end of this
		// category, since we are reading them in order.

		addNodeAfter(tempBase, null);

		if (store.debug)
		  {
		    System.err.println("loaded base " + tempBase.getTypeID() + 
				       ", obj count loaded = " + tempBase.objectTable.size());
		  }
	      }
	    else
	      {
		// we want to add this node to the end of this
		// category, since we are reading them in order.

		addNodeAfter(new DBBaseCategory(store, in), null);
	      }
	  }
	else
	  {
	    // we're reading an old file, and we'll never see a
	    // DBObjectBase here

	    addNodeAfter(new DBBaseCategory(store, in), null);
	  }
      }
  }

  /**
   * <p>Emits this category and its contents to &lt;out&gt;, in
   * XML form.</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    boolean lastCategory = false;

    /* -- */

    xmlOut.startElementIndent("category");
    xmlOut.attribute("name", getName());
    xmlOut.skipLine();		// skip line after category start

    xmlOut.indentOut();

    for (int i = 0; i < contents.size(); i++)
      {
	if (contents.elementAt(i) instanceof DBBaseCategory)
	  {
	    ((DBBaseCategory) contents.elementAt(i)).emitXML(xmlOut);

	    xmlOut.skipLine();
	    lastCategory = true;
	  }
	else if (contents.elementAt(i) instanceof DBObjectBase)
	  {
	    ((DBObjectBase) contents.elementAt(i)).emitXML(xmlOut);
	    lastCategory = false;
	  }
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("category");
  }

  /**
   * <p>Resorts this category's contents based on the tmp_displayOrder field.</p>
   *
   * <p>We only use this when loading DBBaseCategory's from old-style
   * ganymede.db files.  The modern way of doing things depends on the
   * order of categories within a contents Vector, and needs no
   * explicit tmp_displayOrder or sorting to be done.</p> 
   */

  public void resort()
  {
    new VecQuickSort(contents, comparator).sort();

    if (false)
      {
	System.err.println("** Sorted category " + getPath());

	for (int i = 0; i < contents.size(); i++)
	  {
	    Object x = contents.elementAt(i);

	    if (x instanceof DBBaseCategory)
	      {
		System.err.print("Cat[" + ((DBBaseCategory) x).tmp_displayOrder);
		System.err.println("] = " + ((DBBaseCategory) x).getPath());
	      }
	    else if (x instanceof DBObjectBase)
	      {
		System.err.print("Base[" + ((DBObjectBase) x).tmp_displayOrder);
		System.err.println("] = " + ((DBObjectBase) x).getName());
	      }
	  }
      }

    // re-sort subcategories

    for (int i = 0; i < contents.size(); i++)
      {
	Object x = contents.elementAt(i);

	if (x instanceof DBBaseCategory)
	  {
	    ((DBBaseCategory) x).resort();
	  }
      }
  }

  /**
   * <p>Returns the full path to this category, with levels
   * in the hierarchy separated by '/'s.</p>
   *
   * @see arlut.csd.ganymede.Category
   */

  public String getPath()
  {
    if (parent != null)
      {
	return parent.getPath() + "/" + name;
      }
    else
      {
	return "/" + name;
      }
  }

  /**
   * Returns the name of this category.
   *
   * @see arlut.csd.ganymede.Category
   */

  public String getName()
  {
    return name;
  }

  /**
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   *
   * @see arlut.csd.ganymede.CategoryNode
   */

  public boolean setName(String newName)
  {
    if (newName == null)
      {
	throw new IllegalArgumentException("DBBaseCategory can't have null name");
      }

    if (newName.indexOf('/') != -1)
      {
	throw new IllegalArgumentException("DBBaseCategory name can't include /");
      }

    if (parent != null)
      {
	if (!newName.equals(name))
	  {
	    if (parent.contains(newName))
	      {
		throw new IllegalArgumentException("DBBaseCategory name conflicts with existing name in this category");
	      }
	  }
      }

    this.name = newName;

    return true;
  }

  /**
   *
   * This method returns the category that this
   * category node belongs to.
   *
   * @see arlut.csd.ganymede.Category
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public Category getCategory()
  {
    return parent;
  }

  /**
   *
   * This method tells the CategoryNode what it's containing
   * category is.
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public void setCategory(Category category)
  {
    DBBaseCategory cat;
    String path;

    /* -- */

    if (category == null)
      {
	parent = null;
	return;
      }

    if (!(category instanceof DBBaseCategory))
      {
	// we need a local reference

	try
	  {
	    path = category.getPath();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get path of remote category: " + ex);
	  }

	if (debug)
	  {
	    System.err.println("** Attempting to find local copy of category " + path);
	  }

	// getCategoryNode could also return a DBObjectBase, but not
	// in this context

	if (editor == null)
	  {
	    cat = (DBBaseCategory) store.getCategoryNode(path);
	  }
	else
	  {
	    cat = (DBBaseCategory) editor.getCategoryNode(path);
	  }

	if (cat == null)
	  {
	    throw new RuntimeException("setCategory: couldn't find local parent category");
	  }

	parent = cat;
      }
    else
      {
	parent = (DBBaseCategory) category;
      }
  }

  /**
   * <p>This method is used to place a Category Node under us.  This method
   * adds a new node into this category, after prevNodeName if prevNodeName
   * is not null, or at the end of the category if it is.</p>
   *
   * @param node Node to place under this category
   * @param prevNodeName the name of the node that the new node is to be added after,
   * must not be path-qualified.
   *
   * @see arlut.csd.ganymede.Category
   */

  public synchronized void addNodeAfter(CategoryNode node, String prevNodeName)
  {
    CategoryNode
      cNode;

    /* -- */

    if (node == null)
      {
	throw new IllegalArgumentException("Can't add a null node, not even after " + prevNodeName);
      }

    // make sure we've got a local reference if we're being given a
    // Base

    if ((node instanceof Base) && !(node instanceof DBObjectBase))
      {
	node = getBaseFromBase((Base) node);
      }

    if (debug)
      {
	try
	  {
	    System.err.println("DBBaseCategory<" + getName() + ">.addNodeAfter(" + 
			       node.getPath() + "," + prevNodeName +")");
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't check node path: " + ex);
	  }
      }

    // find our insertion point

    //    if (debug)
    //      {
    //	System.err.println("DBBaseCategory.addNodeAfter(): searching to see if node is already in this category");
    //      }

    for (int i = 0; i < contents.size(); i++)
      {
	cNode = (CategoryNode) contents.elementAt(i);

	try
	  {
	    if (cNode.getName().equals(node.getName()))
	      {
		throw new IllegalArgumentException("can't add a node that's already in the category");
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't check node name: " + ex);
	  }
      }


    // put our node into our content list

    // if prevNodeName is null, we're just going to add this node to
    // the end of our category list.

    if (prevNodeName == null)
      {
	contents.addElement(node);
      }
    else
      {
	for (int i = 0; i < contents.size(); i++)
	  {
	    cNode = (CategoryNode) contents.elementAt(i);

	    try
	      {
		if (cNode.getName().equals(prevNodeName))
		  {
		    contents.insertElementAt(node, i+1);
		    break;
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException(ex.getMessage());
	      }
	  }
      }

    // tell the node who's its daddy

    try
      {
	//	if (debug)
	//	  {
	//	    System.err.println("DBBaseCategory.addNodeAfter(): setting category for node");
	//	  }

	node.setCategory(this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception " + ex);
      }
  }

  /**
   * <p>This method is used to place a Category Node under us.  This method
   * adds a new node into this category, before nextNodeName if nextNodeName
   * is not null, or at the beginning of the category if it is.</p>
   *
   * @param node Node to place under this category
   * @param nextNodeName the name of the node that the new node is to be added before,
   * must not be path-qualified.
   *
   * @see arlut.csd.ganymede.Category
   */

  public synchronized void addNodeBefore(CategoryNode node, String nextNodeName)
  {
    CategoryNode
      cNode;

    /* -- */

    if (node == null)
      {
	throw new IllegalArgumentException("Can't add a null node, not even before " + nextNodeName);
      }

    if (debug)
      {
	try
	  {
	    System.err.println("DBBaseCategory<" + getName() + ">.addNodeBefore(" + 
			       node.getPath() + "," + nextNodeName +")");
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException(ex.getMessage());
	  }
      }

    // make sure we've got a local reference if we're being given a
    // Base

    if ((node instanceof Base) && !(node instanceof DBObjectBase))
      {
	node = getBaseFromBase((Base) node);
      }

    // find our insertion point

    //    if (debug)
    //      {
    //	System.err.println("DBBaseCategory.addNodeBefore(): searching to see if node is already in this category");
    //      }

    for (int i = 0; i < contents.size(); i++)
      {
	cNode = (CategoryNode) contents.elementAt(i);

	try
	  {
	    if (cNode.getName().equals(node.getName()))
	      {
		throw new IllegalArgumentException("can't add a node that's already in the category");
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Couldn't check node name: " + ex);
	  }
      }

    // put our node into our content list

    // if nextNodeName is null, we're just going to add this node to
    // the end of our category list.

    if (nextNodeName == null)
      {
	contents.insertElementAt(node, 0);
      }
    else
      {
	for (int i = 0; i < contents.size(); i++)
	  {
	    cNode = (CategoryNode) contents.elementAt(i);

	    try
	      {
		if (cNode.getName().equals(nextNodeName))
		  {
		    contents.insertElementAt(node, i);
		    break;
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException(ex.getMessage());
	      }
	  }
      }

    // tell the node who's its daddy

    try
      {
	//	if (debug)
	//	  {
	//	    System.err.println("DBBaseCategory.addNodeBefore(): setting category for node");
	//	  }

	node.setCategory(this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception " + ex);
      }
  }

  /** 
   * <p>This method can be used to move a Category from another
   * Category to this Category, or to move a Category around within
   * this Category.</p>
   *
   * @param catPath the fully specified path of the node to be moved
   * @param prevNodeName the name of the node that the catPath node is
   * to be placed after in this category, or null if the node is to
   * be placed at the first element of this category
   *
   * @see arlut.csd.ganymede.Category 
   */

  public synchronized void moveCategoryNode(String catPath, String prevNodeName)
  {
    if (debug)
      {
	System.err.println("DBBaseCategory.moveCategoryNode(" + catPath + "," + prevNodeName + ")");
      }

    CategoryNode categoryNode;
    DBBaseCategory oldCategory;

    try
      {
	categoryNode = editor.getCategoryNode(catPath);
	oldCategory = (DBBaseCategory) categoryNode.getCategory();
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException("wow, surprising remote local exception");
      }

    if (oldCategory == this)
      {
	if (debug)
	  {
	    System.err.println("DBBaseCategory.moveCategoryNode(): moving node within category");
	  }

	contents.removeElement(categoryNode);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("DBBaseCategory.moveCategoryNode(): moving node from " + 
			       oldCategory.getPath() + " to " + getPath());
	  }

	try
	  {
	    oldCategory.removeNode(categoryNode);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Local category threw a remote exception.. ? " + ex);
	  }
      }

    try
      {
	categoryNode.setCategory(this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Local category node threw a remote exception.. ? " + ex);
      }

    if (prevNodeName == null)
      {
	contents.insertElementAt(categoryNode, 0);
      }
    else
      {
	for (int i = 0; i < contents.size(); i++)
	  {
	    CategoryNode cNode = (CategoryNode) contents.elementAt(i);

	    try
	      {
		if (cNode.getName().equals(prevNodeName))
		  {
		    contents.insertElementAt(categoryNode, i+1);
		    return;
		  }
	      }
	    catch (RemoteException ex)
	      {
	      }
	  }

	throw new RuntimeException("Couldn't move category node " + catPath +
				   " after non-existent " +
				   prevNodeName);
      }
  }

  /**
   * <p>This method is used to remove a Category Node from under us.</p>
   *
   * @see arlut.csd.ganymede.Category
   */

  public synchronized void removeNode(CategoryNode node) throws RemoteException
  {
    int
      i,
      index = -1;

    /* -- */

    if (node == null)
      {
	throw new IllegalArgumentException("Can't remove a null node");
      }

    // find our deletion point

    if (debug)
      {
	try
	  {
	    Ganymede.debug("DBBaseCategory (" + getName() + ").removeNode(" + node.getPath() + ")");
	  }
	catch (RemoteException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException("rmi local failure?" + ex.getMessage());
	  }
      }

    for (i = 0; i < contents.size(); i++)
      {
	if (debug)
	  {
	    try
	      {
		Ganymede.debug(" examining: " + ((CategoryNode) contents.elementAt(i)).getPath());
	      }
	    catch (RemoteException ex)
	      {
		ex.printStackTrace();
		throw new RuntimeException("rmi local failure?" + ex.getMessage());
	      }
	  }

	if (contents.elementAt(i).equals(node))
	  {
	    index = i;
	  }
      }

    if (index == -1)
      {
	throw new IllegalArgumentException("can't delete a node that's not in the category");
      }

    // remove our node from our content list

    contents.removeElementAt(index);

    if (false)
      {
	if (node instanceof DBObjectBase)
	  {
	    DBObjectBase base = (DBObjectBase) node;

	    if (base.editor == null)
	      {
		System.err.println("DBBaseCategory.removeNode(): " + base.getName() + " has a null editor!");
	      }
	    else
	      {
		System.err.println("DBBaseCategory.removeNode(): " + base.getName() + " has a non-null editor!");
	      }
	  }
      }

    // Sorry, kid, yer on your own now!

    node.setCategory(null);
  }

  /**
   * <p>This method is used to remove a Category Node from under us.</p>
   *
   * @see arlut.csd.ganymede.Category
   */

  public synchronized void removeNode(String name) throws RemoteException
  {
    int
      i,
      index = -1;

    CategoryNode 
      node = null;

    /* -- */

    if (name == null)
      {
	throw new IllegalArgumentException("Can't remove a null name");
      }

    // find our deletion point

    if (debug)
      {
	Ganymede.debug("DBBaseCategory (" + getName() + ").removeNode(" + name + ")");
      }

    for (i = 0; i < contents.size() && (index == -1); i++)
      {
	if (debug)
	  {
	    Ganymede.debug(" examining: " + contents.elementAt(i));
	  }

	node = (CategoryNode) contents.elementAt(i);

	try
	  {
	    if (node.getName().equals(name))
	      {
		index = i;
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    if (index == -1)
      {
	throw new IllegalArgumentException("can't delete a name that's not in the category");
      }
    else if (debug)
      {
	System.err.println("DBBaseCategory.removeNode(): found node " + node);
	
	if (node instanceof DBObjectBase)
	  {
	    System.err.println("DBBaseCategory.removeNode(): node is DBObjectBase");
	  }
	else if (node instanceof Base)
	  {
	    System.err.println("DBBaseCategory.removeNode(): node is Base");
	  }
	else if (node instanceof DBBaseCategory)
	  {
	    System.err.println("DBBaseCategory.removeNode(): node is DBBaseCategory");
	  }
	else if (node instanceof Category)
	  {
	    System.err.println("DBBaseCategory.removeNode(): node is Category");
	  }
	else
	  {
	    System.err.println("DBBaseCategory.removeNode(): node is <unrecognized>");
	  }
      }

    // remove our node from our content list

    contents.removeElementAt(index);

    if (debug)
      {
	if (node instanceof DBObjectBase)
	  {
	    DBObjectBase base = (DBObjectBase) node;

	    if (base.editor == null)
	      {
		System.err.println("DBBaseCategory.removeNode(2): " + base.getName() + " has a null editor!");
	      }
	    else
	      {
		System.err.println("DBBaseCategory.removeNode(2): " + base.getName() + " has a non-null editor!");
	      }
	  }
      }

    // Sorry, kid, yer on your own now!

    node.setCategory(null);
  }

  /**
   * <p>Returns a subcategory of name &lt;name&gt;.</p>
   *
   * @see arlut.csd.ganymede.Category
   */

  public CategoryNode getNode(String name)
  {
    CategoryNode candidate;

    /* -- */

    for (int i = 0; i < contents.size(); i++)
      {
	candidate = (CategoryNode) contents.elementAt(i);
	
	try
	  {
	    if (candidate.getName().equals(name))
	      {
		return candidate;
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    return null;
  }

  /**
   * Returns child nodes
   *
   * @see arlut.csd.ganymede.Category
   */

  public synchronized Vector getNodes()
  {
    return (Vector) contents.clone();
  }

  /**
   * <p>This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.</p>
   */

  public Category newSubCategory(String name)
  {
    DBBaseCategory bc;

    try
      {
	bc = new DBBaseCategory(store, name, this);
      }
    catch (RemoteException ex)
      {
	return null;
      }

    addNodeAfter(bc, null);

    return bc;
  }

  /**
   * <p>This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.</p>
   *
   * @see arlut.csd.ganymede.Category
   */

  public Category newSubCategory()
  {
    DBBaseCategory bc;
    String name;
    int i;

    /* -- */

    name = "New Category";

    i = 2;

    while (this.contains(name))
      {
	name = "New Category " + i++;
      }

    try
      {
	bc = new DBBaseCategory(store, name, this);
      }
    catch (RemoteException ex)
      {
	return null;
      }

    addNodeAfter(bc, null);

    return bc;
  }

  /**
   * <p>This method returns true if this
   * is a subcategory of &lt;cat&gt;.</p>
   *
   * @see arlut.csd.ganymede.Category
   */

  public boolean isUnder(Category cat)
  {
    if (cat == null)
      {
	return false;
      }

    if (cat.equals(this))
      {
	return true;
      }

    if (parent == null)
      {
	return false;
      }
    else
      {
	return parent.isUnder(cat);
      }
  }

  /**
   * <p>This method returns true if this category directly
   * contains a {@link arlut.csd.ganymede.CategoryNode CategoryNode}
   * with name &lt;name&gt;</p>
   */

  public synchronized boolean contains(String name)
  {
    CategoryNode node;

    /* -- */

    for (int i = 0; i < contents.size(); i++)
      {
	node = (CategoryNode) contents.elementAt(i);

	try
	  {
	    if (node.getName().equals(name))
	      {
		return true;
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    return false;
  }

  /**
   * This method returns a reference to the top of this category's
   * tree.
   */

  public DBBaseCategory getRoot()
  {
    DBBaseCategory node = this;

    while (node.parent != null)
      {
	node = node.parent;
      }

    return node;
  }

  /**
   * <p>This method is used to convert an RMI remote reference
   * to a Base object to a reference to the local copy.</p>
   *
   * <p>Needed for RMI under JDK 1.1.</p>
   */

  public DBObjectBase getBaseFromBase(Base base)
  {
    try
      {
	return getBaseFromKey(base.getTypeID());
      }
    catch (RemoteException ex)
      {
	return null;
      }
  }

  /**
   * <p>This method is used to convert an RMI remote reference
   * to a Base object to a reference to the local copy.</p>
   *
   * <p>Needed for RMI under JDK 1.1.</p>
   */

  public DBObjectBase getBaseFromKey(short id)
  {
    if (editor != null)
      {
	return (DBObjectBase) baseHash.get(new Short(id));
      }
    else
      {
	return store.getObjectBase(id);
      }
  }
}
