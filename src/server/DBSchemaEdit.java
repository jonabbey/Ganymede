/*

   DBSchemaEdit.java

   Server side interface for schema editing
   
   Created: 17 April 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBSchemaEdit

------------------------------------------------------------------------------*/

/**
 *
 * Class to manage schema editing through the admin console.
 *
 */

public class DBSchemaEdit extends UnicastRemoteObject implements Unreferenced, SchemaEdit {

  private final static boolean debug = true;

  //

  private boolean locked;
  Admin console;
  DBStore store;
  Vector oldNameSpaces;
  Hashtable oldBases;
  short maxId;
  DBBaseCategory rootCategory;

  Hashtable newBases;

  /* -- */

  /**
   *
   * Constructor.  This constructor should only be called in
   * a critical section synchronized on the primary DBStore
   * object.
   *
   */

  public DBSchemaEdit(Admin console) throws RemoteException
  {
    Enumeration enum;
    DBObjectBase base;
    DBNameSpace ns;

    /* -- */

    System.err.println("DBSchemaEdit constructor entered");

    this.console = console;
    locked = true;
    
    store = Ganymede.db;

    // make editable copies of the object bases

    synchronized (store)
      {
	// duplicate the existing category tree

	maxId = store.maxBaseId;
	newBases = new Hashtable();

	rootCategory = new DBBaseCategory(store, store.rootCategory, newBases, this);

	// make copies of all of our namespaces.. 

	oldNameSpaces = new Vector();
    
	for (int i=0; i < store.nameSpaces.size(); i++)
	  {
	    ns = (DBNameSpace) store.nameSpaces.elementAt(i);
	    oldNameSpaces.addElement(ns);
	  }

	store.nameSpaces.removeAllElements();

	for (int i=0; i < oldNameSpaces.size(); i++)
	  {
	    ns = (DBNameSpace) oldNameSpaces.elementAt(i);
	    store.nameSpaces.addElement(new DBNameSpace(this, ns));
	  }

      } // end synchronized (store)
  }

  /**
   *
   * Returns the root category node from the server
   *
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public Category getRootCategory()
  {
    return rootCategory;
  }

  /**
   *
   * Method to get a category from the category list, by
   * it's full path name.
   *
   */

  public DBBaseCategory getCategory(String pathName)
  {
    DBBaseCategory 
      bc;

    int
      tok;

    /* -- */

    if (pathName == null)
      {
	throw new IllegalArgumentException("can't deal with null pathName");
      }

    StringReader reader = new StringReader(pathName);
    StreamTokenizer tokens = new StreamTokenizer(reader);

    tokens.wordChars(Integer.MIN_VALUE, Integer.MAX_VALUE);
    tokens.ordinaryChar('/');

    tokens.slashSlashComments(false);
    tokens.slashStarComments(false);

    try
      {
	tok = tokens.nextToken();

	bc = rootCategory;

	// The path is going to include the name of the root node
	// itself (unlike in the UNIX filesystem, where the root node
	// has no 'name' of its own), so we need to skip into the
	// root node.

	if (tok == '/')
	  {
	    tok = tokens.nextToken();
	  }

	if (tok == StreamTokenizer.TT_WORD && tokens.sval.equals(rootCategory.getName()))
	  {
	    tok = tokens.nextToken();
	  }

	while (tok != StreamTokenizer.TT_EOF && bc != null)
	  {
	    // note that slashes are the only non-word token we
	    // should ever get, so they are implicitly separators.
	    
	    if (tok == StreamTokenizer.TT_WORD)
	      {
		bc = (DBBaseCategory) bc.getNode(tokens.sval);
	      }
	    
	    tok = tokens.nextToken();
	  }
      }
    catch (IOException ex)
      {
	throw new RuntimeException("parse error in getCategory: " + ex);
      }

    return bc;
  }

  /**
   *
   * Returns a list of bases from the current (non-committed) state of the system.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public synchronized Base[] getBases()
  {
    Base[] bases;
    Enumeration enum;
    int i = 0;

    /* -- */

    bases = new Base[newBases.size()];
    enum = newBases.elements();

    while (enum.hasMoreElements())
      {
	bases[i++] = (Base) enum.nextElement();
      }

    return bases;
  }

  /**
   *
   * Returns a Base reference to match the id.
   * Returns null if no match.
   *
   * @see arlut.csd.ganymede.Base
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public Base getBase(short id)
  {
    return (Base) newBases.get(new Short(id));
  }

  /**
   *
   * Returns a Base reference to match the baseName.
   * Returns null if no match.
   *
   * @see arlut.csd.ganymede.Base
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public synchronized Base getBase(String baseName)
  {
    Enumeration enum = newBases.elements();
    DBObjectBase base;

    /* -- */

    while (enum.hasMoreElements())
      {
	base = (DBObjectBase) enum.nextElement();

	if (base.getName().equals(baseName))
	  {
	    return (Base) base;
	  }
      }

    return null;
  }

  /**
   *
   * This method creates a new DBObjectBase object and returns a remote handle
   * to it so that the admin client can set fields on the base, set attributes,
   * and generally make a nuisance of itself.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public synchronized Base createNewBase(Category category)
  {
    DBObjectBase base;
    DBBaseCategory localCat = null;
    String path;

    /* -- */

    if (debug)
      {
	Ganymede.debug("DBSchemaEdit: entered createNewBase()");
      }

    if (!locked)
      {
	Ganymede.debug("createNewBase failure: already released/committed");
	throw new RuntimeException("already released/committed");
      }

    try
      {
	base = new DBObjectBase(store, ++maxId, this);
      }
    catch (RemoteException ex)
      {
	Ganymede.debug("createNewBase: couldn't initialize new ObjectBase" + ex);
	throw new RuntimeException("couldn't initialize new ObjectBase" + ex);
      }

    if (debug)
      {
	Ganymede.debug("DBSchemaEdit: created new base, setting title");
      }

    String newName = "New Base";

    int i = 2;

    while (getBase(newName) != null)
      {
	newName = "New Base " + i++;
      }

    base.setName(newName);
    base.setDisplayOrder(0);

    try
      {
	path = category.getPath();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("should never happen " + ex);
      }

    if (debug)
      {
	Ganymede.debug("DBSchemaEdit: title is " + newName + ", working to add to category " + path);
      }

    localCat = getCategory(path);

    if (localCat == null)
      {
	Ganymede.debug("DBScemaEdit: createNewBase couldn't find local copy of category object");
	throw new RuntimeException("couldn't get local version of " + path);
      }

    localCat.addNode(base, false, true);
    
    if (debug)
      {
	Ganymede.debug("DBScemaEdit: created base: " + base.getKey());
      }
    
    newBases.put(base.getKey(), base);

    return base;
  }

  /**
   *
   * This method deletes a DBObjectBase, removing it from the
   * Schema Editor's working set of bases.  The removal won't
   * take place for real unless the SchemaEdit is committed.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public void deleteBase(Base b) throws RemoteException
  {
    DBObjectBase base;
    Category parent;
    short id;

    /* -- */

    try
      {
	id = b.getTypeID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("remote " + ex);
      }

    System.err.println("deleteBase");

    if (!locked)
      {
	Ganymede.debug("deleteBase failure: already released/committed");
	throw new RuntimeException("already released/committed");
      }
    
    base = (DBObjectBase) newBases.get(new Short(id));

    if (base != null)
      {
	parent = base.getCategory();
	parent.removeNode(base);
	newBases.remove(new Short(id));
      }
    else
      {
	throw new IllegalArgumentException("couldn't delete base " + b.getName() + ", not in newBases");
      }
  }

  /**
   * This method returns an array of defined NameSpace objects.
   *
   *
   * @see arlut.csd.ganymede.NameSpace
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public synchronized NameSpace[] getNameSpaces()
  {
    NameSpace[] spaces;
    Enumeration enum;
    int i;

    /* -- */

    synchronized (store)
      {
	spaces = new NameSpace[store.nameSpaces.size()];
	i = 0;
	
	enum = store.nameSpaces.elements();
	while (enum.hasMoreElements())
	  {
	    spaces[i++] = (NameSpace) enum.nextElement();
	  }
      }

    return spaces;
  }

  /**
   * This method returns a NameSpace by matching name,
   * or null if no match is found.
   *
   * @see arlut.csd.ganymede.NameSpace
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public synchronized NameSpace getNameSpace(String name)
  {
    NameSpace ns;
    Enumeration enum;

    /* -- */

    synchronized (store)
      {
	enum = store.nameSpaces.elements();
	while (enum.hasMoreElements())
	  {
	    ns = (NameSpace) enum.nextElement();

	    try
	      {
		if (ns.getName().equals(name))
		  {
		    return ns;
		  }
	      }
	    catch (RemoteException ex)
	      {
		// we'll just fall through to return null below.
	      }
	  }
      }

    return null;
  }

  /**
   *
   * This method creates a new DBNameSpace object and returns a remote handle
   * to it so that the admin client can set attributes on the DBNameSpace,
   * and generally make a nuisance of itself.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public synchronized NameSpace createNewNameSpace(String name, boolean caseInsensitive)
  {
    DBNameSpace ns;

    /* -- */

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    try
      {
	ns = new DBNameSpace(this, name, caseInsensitive);
	synchronized (store)
	  {
	    store.nameSpaces.addElement(ns);
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't initialize new namespace" + ex);
      }

    return ns;
  }

  /**
   *
   * This method deletes a DBNameSpace object, returning true if
   * the deletion could be carried out, false otherwise.
   *
   * Currently, this method doesn't do any checking to see if the
   * namespace has any fields created on it.. this will need to
   * be elaborated as time goes by.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public synchronized boolean deleteNameSpace(String name)
  {
    DBNameSpace ns;

    /* -- */

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    synchronized (store)
      {
	for (int i = 0; i < store.nameSpaces.size(); i++)
	  {
	    ns = (DBNameSpace) store.nameSpaces.elementAt(i);
	    if (ns.getName().equals(name))
	      {
		store.nameSpaces.removeElementAt(i);
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   *
   * Commit this schema edit, instantiate the modified schema
   *
   */

  public synchronized void commit()
  {
    Enumeration enum;
    DBObjectBase base;
    DBNameSpace ns;

    /* -- */

    Ganymede.debug("DBSchemaEdit: commit");

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    // do commit here

    synchronized (store)
      {
	// first the object bases

	Ganymede.debug("DBSchemaEdit: entered synchronized block");

	enum = newBases.elements();

	Ganymede.debug("DBSchemaEdit: established enum");

	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();

	    if (base != null)
	      {
		Ganymede.debug("got base");
	      }
	    else
	      {
		Ganymede.debug("did not got base");
	      }

	    base.clearEditor(this);
	    base.updateBaseRefs();
	  }

	// ** need to unlink old objectBases / rootCategory for GC here? **

	store.maxBaseId = maxId;
	store.objectBases = newBases;
	store.rootCategory = rootCategory;

	Ganymede.debug("DBSchemaEdit: iterating over namespaces");

	// and now the name spaces

	for (int i=0; i < store.nameSpaces.size(); i++)
	  {
	    ns = (DBNameSpace) store.nameSpaces.elementAt(i);
	    ns.original = null;
	  }

	Ganymede.debug("DBSchemaEdit: done with namespaces");

	// and unlock the server

	store.schemaEditInProgress = false;

	GanymedeAdmin.setState("Normal Operation");

	store.notifyAll();
      }

    locked = false;

    return;
  }

  /**
   *
   * Abort this schema edit, return the schema to its prior state.
   *
   */

  public synchronized void release()
  {
    DBObjectBase base;

    /* -- */

    Ganymede.debug("DBSchemaEdit: releasing");

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    synchronized (store)
      {
	// restore the namespace vector
	
	store.nameSpaces = oldNameSpaces;
	
	// unlock the server
	
	store.schemaEditInProgress = false;

	store.notifyAll();
      }

    Ganymede.debug("DBSchemaEdit: released");

    GanymedeAdmin.setState("Normal Operation");

    locked = false;

    return;
  }

  /**
   *
   * This method is called when the client loses connection.  unreferenced()
   * should do cleanup.
   *
   */

  public void unreferenced()
  {
    Ganymede.debug("DBSchemaEdit unreferenced");

    if (locked)
      {
	release();
      }
  }
}

