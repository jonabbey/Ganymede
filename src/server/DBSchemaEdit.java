/*

   DBSchemaEdit.java

   Server side interface for schema editing
   
   Created: 17 April 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

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

  private boolean locked;
  Admin console;
  DBStore store;
  Vector oldNameSpaces;
  Hashtable oldBases;
  short oldMaxBaseId;

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
	oldMaxBaseId = store.maxBaseId;
	oldBases = new Hashtable();

	enum = store.objectBases.elements();

	if (enum == null)
	  {
	    System.err.println("DBSchemaEdit constructor: null enum 1");
	  }
	else
	  {
	    while (enum.hasMoreElements())
	      {
		base = (DBObjectBase) enum.nextElement();
		oldBases.put(base.getKey(), base);
	      }
	  }

	// replace the bases in the DBStore's hash with
	// copies.. this preserves the original state of the
	// DBObjectBases, and will allow us to do the commit/revert
	// when we finish up with this DBSchemaEdit

	// we just have to be sure to clear the original field when
	// we commit this schema edit so that garbage collection can
	// clean up the old bases

	enum = oldBases.elements();
	if (enum == null)
	  {
	    System.err.println("DBSchemaEdit constructor: null enum 2");
	  }
	else
	  {
	    while (enum.hasMoreElements())
	      {
		base = (DBObjectBase) enum.nextElement();
		store.setBase(new DBObjectBase(base, this));
	      }
	  }

	// make copies of all of our namespace

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
   * Returns an enumeration of Base references.
   *
   * @see arlut.csd.ganymede.Base
   * @see arlut.csd.ganymede.SchemaEdit
   *
   */

  public Base[] getBases()
  {
    Base[] bases;
    Enumeration enum;
    int i;

    /* -- */

    synchronized (store)
      {
	bases = new Base[store.objectBases.size()];
	i = 0;
	
	enum = store.objectBases.elements();
	while (enum.hasMoreElements())
	  {
	    bases[i++] = (Base) enum.nextElement();
	  }
      }

    return bases;
  }

  /**
   *
   * This method creates a new DBObjectBase object and returns a remote handle
   * to it so that the admin client can set fields on the base, set attributes,
   * and generally make a nuisance of itself.
   *
   * @see arlut.csd.ganymede.SchemaEdit
   */

  public synchronized Base createNewBase()
  {
    DBObjectBase base;

    /* -- */

    System.err.println("createNewBase");

    if (!locked)
      {
	Ganymede.debug("createNewBase failure: already released/committed");
	throw new RuntimeException("already released/committed");
      }

    synchronized (store)
      {
	try
	  {
	    base = new DBObjectBase(store, store.getNextBaseID(), this);
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("createNewBase: couldn't initialize new ObjectBase" + ex);
	    throw new RuntimeException("couldn't initialize new ObjectBase" + ex);
	  }

	Ganymede.debug("created base: " + base.getKey());

	store.setBase(base);
      }

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

  public void deleteBase(Base b)
  {
    DBObjectBase base;
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

    synchronized (store)
      {
	store.objectBases.remove(new Short(id));
      }
  }


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
	    spaces[i] = (NameSpace) enum.nextElement();
	  }
      }

    return spaces;
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

	enum = store.objectBases.elements();

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
	  }

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

    Ganymede.debug("DBSchemaEdit: release");

    if (!locked)
      {
	throw new RuntimeException("already released/committed");
      }

    synchronized (store)
      {
	// first the object base hash
	
	store.maxBaseId = oldMaxBaseId;
	store.objectBases = oldBases;
	
	// and then the namespace vector
	
	store.nameSpaces = oldNameSpaces;
	
	// unlock the server
	
	store.schemaEditInProgress = false;
      }

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

