/*

   DBSchemaEdit.java

   Server side interface for schema editing
   
   Created: 17 April 1997
   Version: $Revision: 1.21 $ %D%
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

  private boolean developMode = false; // CAUTION!! Should be false unless the schema elements
             			       // that the Ganymede server's
				       // internal logic depends on
				       // are being deliberately
				       // altered!

  private boolean locked;	// if true, this DBSchemaEdit object has already been
				// committed or aborted

  DBStore store;		// the DBStore object whose DBObjectBases are being edited

  Hashtable newBases;		// this holds a copy of the DBObjectBase objects comprising
				// the DBStore's database.  All changes made during Base editing
				// are performed on the copies held in this hashtable.. if the
				// DBSchemaEdit session is aborted, newBases is thrown away.
				// If the DBSchemaEdit session is confirmed, newBases replaces
				// store.db.objectBases.

  short maxId;			// id of the highest DBObjectBase object in newBases

  Vector oldNameSpaces;		// this holds the original vector of namespace objects extant
				// at the time the DBSchemaEdit editing session is established.

  DBBaseCategory rootCategory;	// root node of the working DBBaseCategory tree.. if the
				// DBSchemaEdit session is committed, this DBBaseCategory tree
				// will replace store.rootCategory.

  Admin console;		// remote client handle

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
	// duplicate the existing category tree and all the contained
	// bases

	maxId = store.maxBaseId;
	newBases = new Hashtable();

	// this DBBaseCategory constructor recursively copies the bases referenced
	// from store.rootCategory into newBases

	rootCategory = new DBBaseCategory(store, store.rootCategory, newBases, this);

	// make a shallow copy of our namespaces vector.. note that we
	// since DBNameSpace's are immutable once created, we don't
	// need to worry about creating new ones, or about correcting
	// the DBNameSpace references in the duplicated DBObjectBaseFields.

	// we use oldNameSpaces to undo any namespace additions or deletions
	// we do in store.nameSpaces during our editing.

	// note that we'll have to change our namespaces logic if/when
	// DBNameSpace objects become mutable.

	oldNameSpaces = new Vector();
    
	for (int i=0; i < store.nameSpaces.size(); i++)
	  {
	    ns = (DBNameSpace) store.nameSpaces.elementAt(i);
	    oldNameSpaces.addElement(ns);
	  }
      } // end synchronized (store)
  }

  /**
   *
   * Returns true if the schema editor is allowing
   * the 'constant' fields to be edited.  This is
   * provided solely so the Ganymede developers can
   * make incompatible changes to the 'constant' schema
   * items during development.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public boolean isDevelopMode()
  {
    return developMode;
  }

  /**
   *
   * When the server is in develop mode, it is possible to create new
   * built-in fields, or fields that can be relied on by the server
   * code to exist in every non-embedded object type defined.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public BaseField createNewBuiltIn()
  {
    DBObjectBaseField field = null;

    /* -- */

    if (!developMode)
      {
	throw new IllegalArgumentException("can't create new built-ins when developMode is false");
      }

    // We use the userbase as our model for the built-ins.. when we commit,
    // we'll make all non-embedded bases have the same built-in fields
    // as userbase

    synchronized(store)
      {
	DBObjectBase base = (DBObjectBase) getBase(SchemaConstants.UserBase);
	Vector fields = base.getFields();
	short nextId = 0;

	/* -- */

	// identify the id for the next new built-in field

	for (int i = 0; i < fields.size(); i++)
	  {
	    field = (DBObjectBaseField) fields.elementAt(i);

	    if (field.isBuiltIn() && field.getID() >= nextId)
	      {
		nextId = (short) (field.getID() + 1);
	      }
	  }

	if (nextId > 99)
	  {
	    throw new IllegalArgumentException("Error, we have no more room to create built-in fields");
	  }

	field = base.createNewBuiltIn(nextId);

	String template = "New Built-In Field";
	String newName = template;
	int i = 2;

	while (base.getField(newName) != null)
	  {
	    newName = template + (i++);
	  }

	field.setName(newName);
      }

    return (BaseField) field;
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
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public synchronized Base[] getBases(boolean embedded)
  {
    Base[] bases;
    Enumeration enum;
    int i = 0;
    int size = 0;
    Base base;

    /* -- */

    enum = newBases.elements();

    while (enum.hasMoreElements())
      {
	base = (Base) enum.nextElement();

	try
	  {
	    if (base.isEmbedded())
	      {
		if (embedded)
		  {
		    size++;
		  }
	      }
	    else
	      {
		if (!embedded)
		  {
		    size++;
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    bases = new Base[size];
    enum = newBases.elements();

    while (enum.hasMoreElements())
      {
	base = (Base) enum.nextElement();

	try
	  {
	    if (base.isEmbedded())
	      {
		if (embedded)
		  {
		    bases[i++] = base;
		  }
	      }
	    else
	      {
		if (!embedded)
		  {
		    bases[i++] = base;
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    return bases;
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
    Base base;

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

  public synchronized Base createNewBase(Category category, boolean embedded, boolean builtIn)
  {
    DBObjectBase base;
    DBBaseCategory localCat = null;
    String path;
    short id = 0;

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

    if (builtIn)
      {
	Enumeration enum = store.objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();

	    if ((base.getTypeID() > id) && (base.getTypeID() < 256))
	      {
		id = (short) (base.getTypeID() + 1);
	      }
	  }
      }
    else
      {
	id = ++maxId;
      }

    try
      {
	base = new DBObjectBase(store, id, embedded, this);
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

	// yeah, this is cheesy.. if we can't synchronize the built in
	// fields due to a name conflict, we're just going to release
	// the changes.. this wouldn't be adequate if we expected
	// end-users or even end administrators to mess with the built
	// in fields, but this is just for we, the developers of
	// Ganymede, while we're getting things worked out.

	if (developMode && !synchronizeBuiltInFields())
	  {
	    release();
	    return;
	  }

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
		Ganymede.debug("did not got base.. this shouldn't happen");
	      }

	    base.clearEditor(this);
	    base.updateBaseRefs();
	  }

	// ** need to unlink old objectBases / rootCategory for GC here? **

	store.maxBaseId = maxId;
	store.objectBases = newBases; // all the bases already have containingHash pointing to newBases
	store.rootCategory = rootCategory;

	// and unlock the server

	store.schemaEditInProgress = false;

	// and update the serialized representation of the category/base structure.. note that
	// we want it to be created with supergash privs.

	Ganymede.catTransport = new CategoryTransport(store.rootCategory, Ganymede.internalSession);
	Ganymede.baseTransport = new BaseListTransport(Ganymede.internalSession);

	GanymedeAdmin.setState("Normal Operation");
      }

    locked = false;

    return;
  }

  /**
   *
   * The schema editor edits built-in fields by editing the
   * set of built-in fields associated with base SchemaConstants.UserBase
   * (which is chosen randomly as a representative non-embedded object).
   *
   * IMPORTANT NOTE: This is only to be used when the Schema Editing rig
   * is being used to alter the basic built-in fields that the Ganymede
   * schema depends on.  As such, it is critical that built-ins only
   * be modified with great caution.  Note also that DBObjectBase()'s
   * constructor is responsible for implementing built-ins for newly
   * created object base types.  DBObjectBase.java should be edited
   * before any new bases are created after any editing of the built-in
   * fields.
   *
   */

  private boolean synchronizeBuiltInFields()
  {
    DBObjectBase 
      userBase,
      base;

    Vector
      fields,
      builtInFields = new Vector();

    DBObjectBaseField
      fieldDef,
      newFieldDef;

    Base[] bases;

    /* -- */

    userBase = (DBObjectBase) getBase(SchemaConstants.UserBase);

    fields = userBase.getFields();

    for (int i = 0; i < fields.size(); i++)
      {
	fieldDef = (DBObjectBaseField) fields.elementAt(i);

	if (fieldDef.isBuiltIn())
	  {
	    builtInFields.addElement(fieldDef);
	  }
      }

    bases = getBases(false);
    
    for (int i = 0; i < bases.length; i++)
      {
	base = (DBObjectBase) bases[i];

	if (base == userBase)
	  {
	    continue;
	  }

	// now we need to make sure that base
	// has just those built-in field definitions that
	// were in userBase

	fields = base.getFields();

	for (int j = 0; j < fields.size(); j++)
	  {
	    fieldDef = (DBObjectBaseField) fields.elementAt(j);
	    
	    if (fieldDef.isBuiltIn())
	      {
		base.deleteField(fieldDef);
	      }
	  }

	for (int j = 0; j < builtInFields.size(); j++)
	  {
	    fieldDef = (DBObjectBaseField) builtInFields.elementAt(j);

	    // make sure that the base doesn't already have a field with
	    // the same name

	    if (base.getField(fieldDef.getName()) != null)
	      {
		return false;
	      }
	    
	    // copy it

	    try
	      {
		newFieldDef = new DBObjectBaseField(fieldDef, this);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("bizarre remote exception: " + ex);
	      }

	    newFieldDef.setBase(base);
	    base.fieldHash.put(new Short(newFieldDef.getID()), newFieldDef);
	  }
      }

    // now put in the built-ins in the embedded types

    bases = getBases(true);
    
    for (int i = 0; i < bases.length; i++)
      {
	base = (DBObjectBase) bases[i];

	if (base == userBase)
	  {
	    continue;
	  }

	// now we need to make sure that base
	// has just those built-in field definitions that
	// were in userBase

	fields = base.getFields();

	for (int j = 0; j < fields.size(); j++)
	  {
	    fieldDef = (DBObjectBaseField) fields.elementAt(j);
	    
	    if (fieldDef.isBuiltIn())
	      {
		base.deleteField(fieldDef);
	      }
	  }

	for (int j = 0; j < builtInFields.size(); j++)
	  {
	    fieldDef = (DBObjectBaseField) builtInFields.elementAt(j);

	    // the only built-in field we want in embedded objects
	    // is the backlink field.

	    if (fieldDef.getID() != SchemaConstants.BackLinksField)
	      {
		continue;
	      }

	    // make sure that the base doesn't already have a field with
	    // the same name

	    if (base.getField(fieldDef.getName()) != null)
	      {
		return false;
	      }
	    
	    // copy it

	    try
	      {
		newFieldDef = new DBObjectBaseField(fieldDef, this);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("bizarre remote exception: " + ex);
	      }

	    newFieldDef.setBase(base);
	    base.fieldHash.put(new Short(newFieldDef.getID()), newFieldDef);
	  }
      }
    return true;
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

    // tell all the bases who they should look at for
    // future name change validations

    Enumeration enum = store.objectBases.elements();

    while (enum.hasMoreElements())
      {
	base = (DBObjectBase) enum.nextElement();

	base.setContainingHash(store.objectBases);
      }

    synchronized (store)
      {
	// restore the namespace vector
	
	store.nameSpaces = oldNameSpaces;
	
	// unlock the server
	
	store.schemaEditInProgress = false;
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

