/*

   DBSchemaEdit.java

   Server side interface for schema editing
   
   Created: 17 April 1997
   Release: $Name:  $
   Version: $Revision: 1.39 $
   Last Mod Date: $Date: 2000/01/26 04:49:29 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
 * <P>Server-side schema editing class.  This class implements the
 * {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} remote interface to support
 * schema editing by the admin console.</P>  
 *
 * <P>Only one DBSchemaEdit object may be active in the server at a time;  only
 * one admin console can edit the server's schema at a time.  While the server's
 * schema is being edited, no users may be logged on to
 * the system. An admin console puts the server into schema-editing mode by calling the
 * {@link arlut.csd.ganymede.GanymedeAdmin#editSchema editSchema()} method on
 * a server-side {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin} object.</P>
 *
 * <P>When the DBSchemaEdit object is created, it makes copies of all of the
 * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} type definition objects
 * in the server.  The admin console can then talk to those DBObjectBase objects
 * remotely by way of the {@link arlut.csd.ganymede.Base Base} remote interface,
 * accessing data fields, reordering the type tree visible in the client, and
 * so forth.</P>
 *
 * <P>When the user has made the desired changes, the 
 * {@link arlut.csd.ganymede.DBSchemaEdit#commit() commit()} method is called,
 * which replaces the set of DBObjectBase objects held in the server's
 * {@link arlut.csd.ganymede.DBStore DBStore} with the modified set that was
 * created and modified by DBSchemaEdit.</P>
 *
 * <P>Note that the schema editing system doesn't support changing the field type
 * of existing fields very gracefully, nor are background tasks suspended while
 * the server is in schema editing mode.  Generally speaking, you should be
 * using the schema editor to define new fields, or to change field definitions
 * for fields that are not yet in use in the database, not to try to redefine
 * parts of the database that are in actual use and which hold actual data.</P>
 *
 * <P>The schema editing system is really the most fragile
 * thing in the Ganymede server.  It generally works, but it is not as robust
 * as it ought to be.  It's always a good idea to make a backup copy of your
 * ganymede.db file before going in and editing your database schema.</P>
 */

public class DBSchemaEdit extends UnicastRemoteObject implements Unreferenced, SchemaEdit {

  private final static boolean debug = true;

  // ---

  /**
   * CAUTION!! Should be false unless the schema elements that the
   * Ganymede server's internal logic depends on are being
   * deliberately altered!
   */

  boolean developMode = false;

  /**
   * if true, this DBSchemaEdit object has already been
   * committed or aborted
   */

  private boolean locked;

  /**
   * the DBStore object whose DBObjectBases are being edited
   */

  DBStore store;		

  /**
   * this holds a copy of the DBObjectBase objects comprising
   * the DBStore's database.  All changes made during Base editing
   * are performed on the copies held in this hashtable.. if the
   * DBSchemaEdit session is aborted, newBases is thrown away.
   * If the DBSchemaEdit session is confirmed, newBases replaces
   * store.db.objectBases.
   */

  Hashtable newBases;		

  /**
   * id of the highest DBObjectBase object in newBases
   */

  short maxId;

  /**
   * this holds the original vector of namespace objects extant
   * at the time the DBSchemaEdit editing session is established.
   */

  Vector oldNameSpaces;

  /**
   * root node of the working DBBaseCategory tree.. if the
   * DBSchemaEdit session is committed, this DBBaseCategory tree
   * will replace store.rootCategory.
   */

  DBBaseCategory rootCategory;	

  /**
   * remote client handle
   */

  Admin console;		

  /* -- */

  /**
   * <P>Constructor.  This constructor should only be called in
   * a critical section synchronized on the primary 
   * {@link arlut.csd.ganymede.DBStore DBStore} object.</P>
   */

  public DBSchemaEdit(Admin console) throws RemoteException
  {
    Enumeration enum;
    DBObjectBase base;
    DBNameSpace ns;

    /* -- */

    if (debug)
      {
	System.err.println("DBSchemaEdit constructor entered");
      }

    // if we haven't been forced into schema development mode, check
    // with the Ganymede class to see whether the command line parameter
    // set developMode on.

    if (!developMode)
      {
	developMode = Ganymede.developSchema;
      }

    this.console = console;
    locked = true;
    
    store = Ganymede.db;

    // make editable copies of the object bases

    synchronized (store)
      {
	store.setSchemaEditInProgress(true);

	// duplicate the existing category tree and all the contained
	// bases

	maxId = store.maxBaseId;
	newBases = new Hashtable();

	// this DBBaseCategory constructor recursively copies the
	// bases referenced from store.rootCategory into newBases

	rootCategory = new DBBaseCategory(store, store.rootCategory, newBases, this);

	// All the bases referenced in newBases now should have their
	// editor fields pointing to us.. check to make sure

	if (debug)
	  {
	    Enumeration debugenum = newBases.elements();

	    while (debugenum.hasMoreElements())
	      {
		DBObjectBase thisbase = (DBObjectBase) debugenum.nextElement();

		if (thisbase.editor == null)
		  {
		    throw new RuntimeException("ERROR: The recursive copy left off setting editor for " + 
					       thisbase.getName());
		  }
	      }
	  }

	// make a shallow copy of our namespaces vector.. note that we
	// since DBNameSpace's are immutable once created, we don't
	// need to worry about creating new ones, or about correcting
	// the DBNameSpace references in the duplicated
	// DBObjectBaseFields.

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
   * <P>Returns true if the schema editor is allowing
   * the 'constant' fields to be edited.  This is
   * provided solely so the Ganymede developers can
   * make incompatible changes to the 'constant' schema
   * items during development.</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public boolean isDevelopMode()
  {
    return developMode;
  }

  /**
   * <P>When the server is in develop mode, it is possible to create new
   * built-in fields, or fields that can be relied on by the server
   * code to exist in every non-embedded object type defined.</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public BaseField createNewBuiltIn()
  {
    DBObjectBaseField field = null;

    /* -- */

    if (!developMode)
      {
	throw new IllegalArgumentException("can't create new built-ins when developMode is false");
      }

    // We use the userbase as our model for the built-ins.. when we
    // commit, we'll make all non-embedded bases have the same
    // built-in fields as userbase

    synchronized(store)
      {
	DBObjectBase base = (DBObjectBase) getBase(SchemaConstants.UserBase);
	Vector fields = base.getFields();
	short nextId = 9;	// remember SchemaConstants.BackLinksField(8) is a pariah

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
   * <P>Returns the root category node from the server</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public Category getRootCategory()
  {
    if (debug)
      {
	Enumeration debugenum = newBases.elements();

	while (debugenum.hasMoreElements())
	  {
	    DBObjectBase thisbase = (DBObjectBase) debugenum.nextElement();

	    if (thisbase.editor == null)
	      {
		throw new RuntimeException("ERROR: getRootCategory(): the recursive copy left off setting editor for " + 
					   thisbase.getName());
	      }
	  }
      }

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
	// has no 'name' of its own), so we need to skip into the root
	// node.

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
	    // note that slashes are the only non-word token we should
	    // ever get, so they are implicitly separators.
	    
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
   * <P>Returns a list of bases from the current (non-committed) state of the system.</P>
   *
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   *
   * @see arlut.csd.ganymede.SchemaEdit
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
   * <P>Returns a list of bases from the current (non-committed) state of the system.</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
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
   * <P>Returns a {@link arlut.csd.ganymede.Base Base} reference to match the id, or
   * null if no match.</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public Base getBase(short id)
  {
    return (Base) newBases.get(new Short(id));
  }

  /**
   * <P>Returns a {@link arlut.csd.ganymede.Base Base} reference to match the baseName,
   * or null if no match.</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
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
   * <P>This method creates a new {@link
   * arlut.csd.ganymede.DBObjectBase DBObjectBase} object and returns
   * a remote handle to it so that the admin client can set fields on
   * the base, set attributes, and generally make a nuisance of
   * itself.</P>
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
	Ganymede.debug("DBSchemaEdit.createNewBase(): couldn't initialize new ObjectBase" + ex);
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
	Ganymede.debug("DBSchemaEdit: createNewBase couldn't find local copy of category object");
	throw new RuntimeException("couldn't get local version of " + path);
      }

    // newBases is the same as the baseHash variables used in the category
    // tree.. we load it up here so that the addNode() will be able to find
    // it when goes through the RMI stub->local object getBaseFromBase()
    // operation.

    newBases.put(base.getKey(), base);

    if (debug)
      {
	if (base.editor == null)
	  {
	    throw new RuntimeException("DBSchemaEdit.createNewBase(): base's editor field is null after newBases.put()");
	  }
      }

    localCat.addNode(base, false, true);

    if (debug)
      {
	if (base.editor == null)
	  {
	    throw new RuntimeException("DBSchemaEdit.createNewBase(): base's editor field is null after addNode()");
	  }
	else
	  {
	    DBObjectBase testbase = (DBObjectBase) newBases.get(base.getKey());

	    if (testbase.editor == null)
	      {
		throw new RuntimeException("DBSchemaEdit.createNewBase(): testbase's editor field is null after addNode()");
	      }
	  }
	
      }
    
    if (debug)
      {
	Ganymede.debug("DBScemaEdit: created base: " + base.getKey());
      }

    return base;
  }

  /**
   * <P>This method deletes a {@link
   * arlut.csd.ganymede.DBObjectBase DBObjectBase}, removing it from the
   * Schema Editor's working set of bases.  The removal won't
   * take place for real unless the SchemaEdit is committed.</P>
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

    if (debug)
      {
	System.err.println("Calling deleteBase on base " + b.getName());
      }

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
   * <P>This method returns an array of defined 
   * {@link arlut.csd.ganymede.NameSpace NameSpace} objects.</P>
   *
   * @see arlut.csd.ganymede.SchemaEdit
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
   * <P>This method returns a {@link arlut.csd.ganymede.NameSpace NameSpace} by matching name,
   * or null if no match is found.</P>
   *
   * @see arlut.csd.ganymede.NameSpace
   * @see arlut.csd.ganymede.SchemaEdit
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
   * <P>This method creates a new {@link arlut.csd.ganymede.DBNameSpace DBNameSpace} 
   * object and returns a remote handle
   * to it so that the admin client can set attributes on the DBNameSpace,
   * and generally make a nuisance of itself.</P>
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
   * <P>This method deletes a
   *  {@link arlut.csd.ganymede.DBNameSpace DBNameSpace} object, returning true if
   * the deletion could be carried out, false otherwise.</P>
   *
   * <P>Currently, this method doesn't do any checking to see if the
   * namespace has any fields created on it.  If you delete a namespace
   * that fields are using and commit the schema change, when
   * you stop and restart the server, the loader will complain that
   * the fields refer to a non-existent namespace.  This code should
   * really scan through the
   * {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField} objects
   * in the schema editor's working set and clear any fields that point
   * to this namespace.</P>
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
   * <P>Commit this schema edit, instantiate the modified schema</P>
   *
   * <P>It is an error to attempt any schema editing operations after this
   * method has been called.</P>
   */

  public synchronized void commit()
  {
    Enumeration enum;
    DBObjectBase base;
    DBNameSpace ns;

    /* -- */

    Ganymede.debug("DBSchemaEdit: commiting schema changes");

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    // do commit here

    synchronized (store)
      {
	// first the object bases

	if (debug)
	  {
	    Ganymede.debug("DBSchemaEdit: entered synchronized block");
	  }

	// yeah, this is cheesy.. if we can't synchronize the built in
	// fields due to a name conflict, we're just going to release
	// the changes.. this wouldn't be adequate if we expected
	// end-users or even end administrators to mess with the built
	// in fields, but this is just for we, the developers of
	// Ganymede, while we're getting things worked out.

	if (developMode && !synchronizeBuiltInFields())
	  {
	    release();
	    Ganymede.debug("DBSchemaEdit: couldn't synchronize built-in fields.. abandoning commit.");
	    return;
	  }

	enum = newBases.elements();

	if (debug)
	  {
	    Ganymede.debug("DBSchemaEdit: established enum");
	  }

	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();

	    if (false)		// debug
	      {
		// hm, why do i have such a strange test here?  What was I thinking?

		if (base != null)
		  {
		    Ganymede.debug("got base");
		  }
		else
		  {
		    Ganymede.debug("did not got base.. this shouldn't happen");
		  }
	      }

	    base.clearEditor(this);
	    base.updateBaseRefs();
	  }

	// ** need to unlink old objectBases / rootCategory for GC here? **

	store.maxBaseId = maxId;

	// all the bases already have containingHash pointing to
	// newBases

	store.objectBases = newBases; 
	rootCategory.clearEditor();
	store.rootCategory = rootCategory;

	// and unlock the server

	store.setSchemaEditInProgress(false);

	// and update the serialized representation of the
	// category/base structure.. note that we want it to be
	// created with supergash privs.

	Ganymede.catTransport = new CategoryTransport(store.rootCategory, Ganymede.internalSession);
	Ganymede.baseTransport = new BaseListTransport(Ganymede.internalSession);

	GanymedeAdmin.setState("Normal Operation");
      }

    locked = false;

    Ganymede.debug("DBSchemaEdit: schema changes committed.");

    return;
  }

  /**
   * <P>The schema editor edits built-in fields by editing the set of
   * built-in fields associated with base SchemaConstants.UserBase
   * (which is chosen randomly as a representative non-embedded
   * object).  This method replicates the builtin fields from
   * SchemaConstants.UserBase into the rest of the non-embedded object
   * types defined in th server.</P>
   *
   * <P>IMPORTANT NOTE: This is only to be used when the Schema Editing rig
   * is being used to alter the basic built-in fields that the Ganymede
   * schema depends on.  As such, it is critical that built-ins only
   * be modified with great caution.  Note also that DBObjectBase()'s
   * constructor is responsible for implementing built-ins for newly
   * created object base types.  DBObjectBase.java should be edited
   * before any new bases are created after any editing of the built-in
   * fields.</P>
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

    // assemble a list of built-in fields specified in the user base

    userBase = (DBObjectBase) getBase(SchemaConstants.UserBase);

    fields = userBase.getFields();

    for (int i = 0; i < fields.size(); i++)
      {
	fieldDef = (DBObjectBaseField) fields.elementAt(i);

	if (fieldDef.isBuiltIn())
	  {
	    //	    Ganymede.debug("Builtins " + i + ": " + fieldDef);
	    builtInFields.addElement(fieldDef);
	  }
      }

    bases = getBases(false);	// only want to mess with the non-embedded types
    
    for (int i = 0; i < bases.length; i++)
      {
	base = (DBObjectBase) bases[i];

	// we're using the userBase as our canonical source of
	// built-in field definitions.

	if (base == userBase)
	  {
	    continue;
	  }

	// now we need to make sure that base has just those built-in
	// field definitions that were in userBase.. delete the built-in
	// fields currently registered for this base

	fields = base.getFields();

	for (int j = 0; j < fields.size(); j++)
	  {
	    fieldDef = (DBObjectBaseField) fields.elementAt(j);
	    
	    if (fieldDef.isBuiltIn())
	      {
		if (false)
		  {
		    Ganymede.debug("DBSchemaEdit.synchronizeBuiltInFields(): deleting field " + 
				   fieldDef.getName() + " in " + base.getName());
		    
		    Ganymede.debug("*** before delete:");
		    
		    Enumeration nowFields = base.fieldTable.elements();
		    
		    while (nowFields.hasMoreElements())
		      {
			Ganymede.debug("* " + nowFields.nextElement());
		      }
		  }

		base.deleteField(fieldDef);

		if (false)
		  {
		    Ganymede.debug("*** after delete:");
		    
		    Enumeration nowFields = base.fieldTable.elements();
		    
		    while (nowFields.hasMoreElements())
		      {
			Ganymede.debug("* " + nowFields.nextElement());
		      }
		    
		    Ganymede.debug("*** that's it:");
		  }
	      }
	    else if (false)
	      {
		Ganymede.debug("DBSchemaEdit.synchronizeBuiltInFields(): not deleting field " + fieldDef.getName() +
			       " in " + base.getName());
	      }
	  }

	// now add in copies of the (possibly revised) built-in field
	// definitions

	for (int j = 0; j < builtInFields.size(); j++)
	  {
	    fieldDef = (DBObjectBaseField) builtInFields.elementAt(j);

	    // make sure that the base doesn't already have a field with
	    // the same name

	    if (false)
	      {
		Ganymede.debug("DBSchemaEdit.synchronizeBuiltInFields(): adding back built-in " + j +
			       ", " + fieldDef.getName());
	      }

	    if (base.getField(fieldDef.getName()) != null)
	      {
		Ganymede.debug("DBSchemaEdit.synchronizeBuiltInFields(): already have a " + fieldDef.getName() +
			       " field in " + base.getName());

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
	    base.addField(newFieldDef);
	  }
      }

    return true;
  }

  /**
   * <P>Abort this schema edit, return the schema to its prior state.</P>
   *
   * <P>It is an error to attempt any schema editing operations after this
   * method has been called.</P>
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
	
	store.setSchemaEditInProgress(false);
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

