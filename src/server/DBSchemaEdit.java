/*

   DBSchemaEdit.java

   Server side interface for schema editing
   
   Created: 17 April 1997
   Release: $Name:  $
   Version: $Revision: 1.43 $
   Last Mod Date: $Date: 2000/06/24 18:36:40 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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
 * <P>The schema editing code in the server currently has only a
 * limited ability to verify that changes made in the schema editor
 * will not break the database's consistency constraints in some
 * fashion.  Generally speaking, you should be using the schema editor
 * to define new fields, or to change field definitions for fields
 * that are not yet in use in the database, not to try to redefine
 * parts of the database that are in actual use and which hold actual
 * data.</P>
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
    if (debug)
      {
	System.err.println("DBSchemaEdit constructor entered");
      }

    // the GanymedeAdmin editSchema() method should have disabled the
    // login semaphore with a "schema edit" condition.  Check this
    // just to be sure.

    if (!"schema edit".equals(GanymedeServer.lSemaphore.checkEnabled()))
      {
	throw new RuntimeException("can't edit schema without lock");
      }

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

	// make a shallow copy of our namespaces vector.. note that
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
	    DBNameSpace ns = (DBNameSpace) store.nameSpaces.elementAt(i);
	    oldNameSpaces.addElement(ns);
	  }
      } // end synchronized (store)
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

  public synchronized Base createNewBase(Category category, boolean embedded, boolean lowRange)
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

    if (lowRange)
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

    try
      {
	path = category.getPath();
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException("should never happen " + ex.getMessage());
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
    // tree.. we load it up here so that the addNodeAfter() will be able to find
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

    localCat.addNodeAfter(base, null);

    if (debug)
      {
	if (base.editor == null)
	  {
	    throw new RuntimeException("DBSchemaEdit.createNewBase(): base's editor field is null after addNodeAfter()");
	  }
	else
	  {
	    DBObjectBase testbase = (DBObjectBase) newBases.get(base.getKey());

	    if (testbase.editor == null)
	      {
		throw new RuntimeException("DBSchemaEdit.createNewBase(): testbase's editor field is null after addNodeAfter()");
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

  public ReturnVal deleteBase(String baseName) throws RemoteException
  {
    DBObjectBase base, tmpBase;
    Category parent;
    short id;

    /* -- */

    base = (DBObjectBase) getBase(baseName);

    if (base == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Base " + baseName + " not found in DBStore, can't delete.");
      }

    id = base.getTypeID();

    if (debug)
      {
	System.err.println("Calling deleteBase on base " + base.getName());
      }

    if (!locked)
      {
	Ganymede.debug("deleteBase failure: already released/committed");
	throw new RuntimeException("already released/committed");
      }
    
    tmpBase = (DBObjectBase) newBases.get(new Short(id));

    if (tmpBase != null)
      {
	parent = tmpBase.getCategory();
	parent.removeNode(tmpBase);
	newBases.remove(new Short(id));
      }
    else
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Base " + baseName + " not found in DBStore, consistency error.");
      }

    return null;
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

  public synchronized ReturnVal deleteNameSpace(String name)
  {
    DBNameSpace ns = null;
    int index = 0;

    /* -- */

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    for (index = 0; index < store.nameSpaces.size(); index++)
      {
	ns = (DBNameSpace) store.nameSpaces.elementAt(index);
	
	if (ns.getName().equals(name))
	  {
	    break;
	  }
	else
	  {
	    ns = null;
	  }
      }

    if (ns == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Can't remove namespace " + name + ", that namespace was not found.");
      }

    // check to make sure this namespace isn't tied to a field still

    Enumeration enum = newBases.elements();

    while (enum.hasMoreElements())
      {
	DBObjectBase base = (DBObjectBase) enum.nextElement();

	Vector fieldDefs = base.getFields();

	for (int i = 0; i < fieldDefs.size(); i++)
	  {
	    DBObjectBaseField fieldDef = (DBObjectBaseField) fieldDefs.elementAt(i);

	    if (fieldDef.getNameSpace() == ns)
	      {
		return Ganymede.createErrorDialog("Schema Editing Error",
						  "Can't remove namespae " + name +
						  ", that namespace is still tied to " + fieldDef);
	      }
	  }
      }

    store.nameSpaces.removeElementAt(index);

    return null;
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

	enum = newBases.elements();

	if (debug)
	  {
	    Ganymede.debug("DBSchemaEdit: established enum");
	  }

	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	    base.clearEditor();
	  }

	// ** need to unlink old objectBases / rootCategory for GC here? **

	store.maxBaseId = maxId;

	// all the bases already have containingHash pointing to
	// newBases

	store.objectBases = newBases;
	rootCategory.clearEditor();
	store.rootCategory = rootCategory;
      }

    // update the serialized representation of the
    // category/base structure.. note that we want it to be
    // created with supergash privs.

    Ganymede.catTransport = new CategoryTransport(store.rootCategory, Ganymede.internalSession);
    Ganymede.baseTransport = new BaseListTransport(Ganymede.internalSession);

    Ganymede.debug("DBSchemaEdit: schema changes committed.");

    // and unlock the server

    GanymedeAdmin.setState("Normal Operation");

    locked = false;
    
    GanymedeServer.lSemaphore.enable("schema edit");

    return;
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

    synchronized (store)
      {
	// restore the namespace vector
	store.nameSpaces.setSize(0);
	store.nameSpaces = oldNameSpaces;
      }

    // unlock the server
	
    Ganymede.debug("DBSchemaEdit: released");

    GanymedeAdmin.setState("Normal Operation");

    locked = false;

    // speed gc

    newBases.clear();
    newBases = null;

    GanymedeServer.lSemaphore.enable("schema edit");

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

