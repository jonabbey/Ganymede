/*

   DBBaseCategory.java

   This module represents an objectbase folder in the server's
   category hierarchy.
   
   Created: 11 August 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Util.*;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;


/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBObjectBase

------------------------------------------------------------------------------*/

/**
 *
 * This class represents an objectbase folder in the server's
 * category hierarchy.
 *
 */

public class DBBaseCategory extends UnicastRemoteObject implements Category, CategoryNode {

  private final static boolean debug = true;

  //

  private String name;
  private int displayOrder;
  private DBBaseCategory parent;
  private DBStore store;
  private Vector contents;

  private DBSchemaEdit editor;

  /* -- */

  /**
   *
   * Primary constructor.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   * for us.
   *
   * @param parent If we're not being constructed at the top level, who is our parent?
   *
   */

  public DBBaseCategory(DBStore store, String name, DBBaseCategory parent) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    this.setName(name);
    this.store = store;
    this.parent = parent;

    contents = new Vector();
  }

  /**
   *
   * Default value constructor.  This is used to construct a top-level category.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   * for us.
   *
   * @param name Name for this category
   *
   */

  public DBBaseCategory(DBStore store, String name) throws RemoteException
  {
    this(store, name, null);
  }

  /**
   *
   * Receive constructor.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   * for us.
   *
   * @param in DataInput stream to load the db representation of this category from
   *
   */

  public DBBaseCategory(DBStore store, DataInput in) throws RemoteException, IOException
  {
    super();			// UnicastRemoteObject initialization

    this.store = store;
    contents = new Vector();
    receive(in, null);
  }


  /**
   *
   * Receive constructor.
   *
   * @param store DBStore that is managing us.  We'll ask it to look up parents
   * for us.
   *
   * @param in DataInput stream to load the db representation of this category from
   *
   */

  public DBBaseCategory(DBStore store, DataInput in, DBBaseCategory parent) throws RemoteException, IOException
  {
    super();			// UnicastRemoteObject initialization

    this.store = store;
    contents = new Vector();
    receive(in, parent);
  }

  /**
   *
   * Recursive duplication constructor.  This constructor recurses down
   * through the newly created DBBaseCategory and creates copies of
   * the bases and categories therein.
   *
   */

  public DBBaseCategory(DBStore store, DBBaseCategory rootCategory,
			Hashtable baseHash, DBSchemaEdit editor) throws RemoteException
  {
    this.editor = editor;
    this.store = store;
    contents = new Vector();

    setName(rootCategory.getName());
    setDisplayOrder(rootCategory.getDisplayOrder());
    
    recurseDown(rootCategory, baseHash, editor);
  }

  private void recurseDown(DBBaseCategory category, Hashtable baseHash, DBSchemaEdit editor) throws RemoteException
  {
    Vector children = category.getNodes();
    CategoryNode node;
    DBObjectBase oldBase, newBase;
    DBBaseCategory oldCategory, newCategory;

    /* -- */

    if (debug)
      {
	Ganymede.debug("** recurseDown");
      }

    for (int i = 0; i < children.size(); i++)
      {
	node = (CategoryNode) children.elementAt(i);

	if (node instanceof Base)
	  {
	    oldBase = (DBObjectBase) node;
	    newBase = new DBObjectBase(oldBase, editor); // a new copy, with the same objects under it
	    baseHash.put(newBase.getKey(), newBase);

	    if (debug)
	      {
		Ganymede.debug("Created newBase " + newBase.getName() + " in recursive category tree duplication");
	      }

	    addNode(newBase, false, false);

	    if (debug)
	      {
		Ganymede.debug("Added " + newBase.getName() + " to new category tree");
	      }

	  }
	else if (node instanceof DBBaseCategory)
	  {
	    oldCategory = (DBBaseCategory) node;
	    newCategory = (DBBaseCategory) newSubCategory(oldCategory.getName());

	    if (debug)
	      {
		Ganymede.debug("Created newCategory " + newCategory.getName() + " in recursive category tree duplication");
	      }

	    newCategory.recurseDown(oldCategory, baseHash, editor);
	  }
      }
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    int count = 0;
    DBBaseCategory bc;

    /* -- */

    for (int i = 0; i < contents.size(); i++)
      {
	if (contents.elementAt(i) instanceof DBBaseCategory)
	  {
	    count++;
	  }
      }

    out.writeUTF(this.getPath());
    out.writeInt(displayOrder);
    out.writeInt(count);
    
    for (int i = 0; i < contents.size(); i++)
      {
	if (contents.elementAt(i) instanceof DBBaseCategory)
	  {
	    bc = (DBBaseCategory) contents.elementAt(i);
	    bc.emit(out);
	  }
      }
  }

  synchronized void receive(DataInput in, DBBaseCategory parent) throws IOException
  {
    String 
      pathName,
      path;

    int 
      count,
      lastSlash;

    /* -- */

    pathName = in.readUTF();
    displayOrder = in.readInt();

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
	System.err.println("DBBaseCategory.receive(): reading in " + count + " subcategories");
      }

    for (int i = 0; i < count; i++)
      {
	addNode(new DBBaseCategory(store, in), false, false);
      }
    
    resort();
  }

  /**
   *
   * Returns the full path to this category, with levels
   * in the hierarchy separated by '/'s.
   *
   * @see arlut.csd.ganymede.Category
   *
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
   *
   * Returns the name of this category.
   *
   * @see arlut.csd.ganymede.Category
   *
   */

  public String getName()
  {
    return name;
  }

  /**
   *
   * Gets the order of this node in the containing category
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public int getDisplayOrder()
  {
    return displayOrder;
  }

  /**
   *
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
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
   *
   * @see arlut.csd.ganymede.Category
   *
   */

  public void setDisplayOrder(int val)
  {
    displayOrder = val;
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

	if (editor == null)
	  {
	    cat = store.getCategory(path);
	  }
	else
	  {
	    cat = editor.getCategory(path);
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
   *
   * sort the elements according to display order
   *
   */

  synchronized void resort()
  {
    new VecQuickSort(contents, 
		     new arlut.csd.Util.Compare()
		     {
		       public int compare(Object a, Object b)
			 {
			   CategoryNode aN, bN;

			   aN = (CategoryNode) a;
			   bN = (CategoryNode) b;

			   try
			     {
			       if (aN.getDisplayOrder() < bN.getDisplayOrder())
				 {
				   return -1;
				 }
			       else if (aN.getDisplayOrder() > bN.getDisplayOrder())
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
			       throw new RuntimeException("caught remote exception " + ex);
			     }
			 }
		     }
		     ).sort();
  }

  /**
   *
   * This method is used to place a Category Node under us.  The node
   * will be placed according to the node's displayOrder value, if resort
   * and/or adjustNodes are true.
   *
   * addNode() places the new node in the list according to the node's
   * displayOrder value.
   *
   * @param node Node to place under this category
   * @param sort If true, the nodes under this category will be resorted after insertion
   * @param adjustNodes If true, the nodes under this category will have their displayOrder recalculated.
   * this should not be done lightly, and not at all if any more nodes with precalculated or saved
   * displayOrder's are to be later inserted.
   *
   * @see arlut.csd.ganymede.Category
   *
   */

  public synchronized void addNode(CategoryNode node, boolean sort, boolean adjustNodes)
  {
    int
      i,
      index = -1;

    boolean found = false;

    CategoryNode
      cNode;

    /* -- */

    if (node == null)
      {
	throw new IllegalArgumentException("Can't add a null node");
      }

    try
      {
	if (this.contains(node.getName()))
	  {
	    throw new IllegalArgumentException("Can't add this node.. name already registered " + node.getName());
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote " + ex);
      } 

    // find our insertion point

    for (i = 0; !found && i < contents.size(); i++)
      {
	if (contents.elementAt(i).equals(node))
	  {
	    found = true;
	  }
      }

    if (found)
      {
	throw new IllegalArgumentException("can't add a node that's already in the category");
      }

    // put our node into our content list

    // note that we *don't* want to put in a rmi stub here for Bases
    // here.. we'd much rather have the full-access local object so
    // that we can do full direct DBObjectBase operations as the
    // category tree is managed by the Schema editor.

    if (!adjustNodes)
      {
	if (node instanceof DBObjectBase)
	  {
	    contents.addElement((DBObjectBase) node);
	  }
	else if (node instanceof Base)
	  {
	    try
	      {
		contents.addElement(store.getObjectBase(((Base) node).getName()));
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught bad remote " + ex);
	      }
	  }
	else
	  {
	    contents.addElement(node);
	  }
      }
    else
      {
	try
	  {
	    if (node instanceof DBObjectBase)
	      {
		if (node.getDisplayOrder() >= contents.size())
		  {
		    contents.addElement(node);
		  }
		else
		  {
		    contents.insertElementAt((DBObjectBase) node, node.getDisplayOrder());
		  }
	      }
	    else if (node instanceof Base)
	      {
		DBObjectBase newBase = null;

		try
		  {
		    newBase = store.getObjectBase(((Base) node).getName());
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught bad remote " + ex);
		  }

		if (node.getDisplayOrder() >= contents.size())
		  {
		    contents.addElement(newBase);
		  }
		else
		  {
		    contents.insertElementAt(newBase, newBase.getDisplayOrder());
		  }
	      }
	    else if (node instanceof Category)
	      {
		// presumably we've got a Category here..

		if (node.getDisplayOrder() >= contents.size())
		  {
		    contents.addElement(node);
		  }
		else
		  {
		    contents.insertElementAt(node, node.getDisplayOrder());
		  }
	      }
	    else
	      {
		throw new IllegalArgumentException("don't recognize node");
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't get node display order for in-order insertion: " + ex);
	  }
      }

    // sort the elements according to display order

    if (sort)
      {
	resort();
      }

    if (adjustNodes)
      {
	for (i = 0; i < contents.size(); i++)
	  {
	    cNode = (CategoryNode) contents.elementAt(i);

	    try
	      {
		cNode.setDisplayOrder(i);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("this should not happen " + ex);
	      }
	  }
      }

    // tell the node who's its daddy

    try
      {
	node.setCategory(this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception " + ex);
      }
  }

  /**
   *
   * This method is used to remove a Category Node from under us.
   *
   * Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.
   *
   * @see arlut.csd.ganymede.Category
   * 
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

    Ganymede.debug("Searching for " + node);

    for (i = 0; i < contents.size(); i++)
      {
	if (debug)
	  {
	    Ganymede.debug(" examining: " + contents.elementAt(i));
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

    // now pull up the other nodes.. note that we assume that when
    // removeNode is called, no nodes will be added with stored
    // displayOrder's from a DBStore file, say.

    for (i = index; i < contents.size(); i++)
      {
	( (CategoryNode) contents.elementAt(i)).setDisplayOrder(i);
      }

    // tell our node what it's display order is.

    node.setDisplayOrder(0);

    // Sorry, kid, yer on your own now!

    node.setCategory(null);
  }

  /**
   *
   * This method is used to remove a Category Node from under us.
   *
   * Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.
   *
   * @see arlut.csd.ganymede.Category
   * 
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

    Ganymede.debug("Searching for " + name);

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

    // remove our node from our content list

    contents.removeElementAt(index);

    // now pull up the other nodes.. note that we assume that when
    // removeNode is called, no nodes will be added with stored
    // displayOrder's from a DBStore file, say.

    for (i = index; i < contents.size(); i++)
      {
	( (CategoryNode) contents.elementAt(i)).setDisplayOrder(i);
      }

    // tell our node what it's display order is.

    node.setDisplayOrder(0);

    // Sorry, kid, yer on your own now!

    node.setCategory(null);
  }

  /**
   *
   * Returns a subcategory of name <name>.
   *
   * @see arlut.csd.ganymede.Category
   *
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
   *
   * Returns child nodes
   *
   * @see arlut.csd.ganymede.Category
   *
   */

  public synchronized Vector getNodes()
  {
    return (Vector) contents.clone();
  }

  /**
   *
   * This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.
   *
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

    bc.setDisplayOrder(contents.size() + 1);
    addNode(bc, false, true);

    return bc;
  }


  /**
   *
   * This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.
   *
   * @see arlut.csd.ganymede.Category
   * 
   */

  public Category newSubCategory()
  {
    DBBaseCategory bc;
    String name;
    int i;

    /* -- */

    name = "New Category";

    i = 2;

    while (getNode(name) != null)
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

    bc.setDisplayOrder(contents.size() + 1);
    addNode(bc, false, true);

    return bc;
  }

  /**
   *
   * This method returns true if this
   * is a subcategory of cat.
   *
   * @see arlut.csd.ganymede.Category
   *
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
}
