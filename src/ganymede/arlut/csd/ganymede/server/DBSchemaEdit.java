/*

   DBSchemaEdit.java

   Server side interface for schema editing

   Created: 17 April 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.CategoryNode;
import arlut.csd.ganymede.rmi.NameSpace;
import arlut.csd.ganymede.rmi.SchemaEdit;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBSchemaEdit

------------------------------------------------------------------------------*/

/**
 * <p>Server-side schema editing class.  This class implements the {@link
 * arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} remote interface to
 * support schema editing by the admin console.</p>
 *
 * <p>Only one DBSchemaEdit object may be active in the server at a time;
 * only one admin console can edit the server's schema at a time.
 * While the server's schema is being edited, no users may be logged
 * on to the system. An admin console puts the server into
 * schema-editing mode by calling the {@link
 * arlut.csd.ganymede.server.GanymedeAdmin#editSchema editSchema()}
 * method on a server-side {@link
 * arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} object.</p>
 *
 * <p>When the DBSchemaEdit object is created, it makes copies of all of
 * the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
 * type definition objects in the server.  The admin console can then
 * talk to those DBObjectBase objects remotely by way of the {@link
 * arlut.csd.ganymede.rmi.Base Base} remote interface, accessing data
 * fields, reordering the type tree visible in the client, and so
 * forth.</p>
 *
 * <p>When the user has made the desired changes, the {@link
 * arlut.csd.ganymede.server.DBSchemaEdit#commit() commit()} method is
 * called, which replaces the set of DBObjectBase objects held in the
 * server's {@link arlut.csd.ganymede.server.DBStore DBStore} with the
 * modified set that was created and modified by DBSchemaEdit.</p>
 *
 * <p>The schema editing code in the server currently has only a limited
 * ability to verify that changes made in the schema editor will not
 * break the database's consistency constraints in some fashion.
 * Generally speaking, you should be using the schema editor to define
 * new fields, or to change field definitions for fields that are not
 * yet in use in the database, not to try to redefine parts of the
 * database that are in actual use and which hold actual data.</p>
 *
 * <p>The schema editing system is really the most fragile thing in the
 * Ganymede server.  It generally works, but it is not as robust as it
 * ought to be.  It's always a good idea to make a backup copy of your
 * ganymede.db file before going in and editing your database schema.</p>
 */

public final class DBSchemaEdit implements Unreferenced, SchemaEdit {

  final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBSchemaEdit");

  // ---

  /**
   * if true, this DBSchemaEdit object has already been
   * committed or aborted
   */

  private boolean locked;

  /**
   * the DBStore object whose DBObjectBases are being edited
   */

  private final DBStore store;

  /**
   * A copy of the DBObjectBase objects comprising the DBStore's
   * database.  All changes made during Base editing are performed on
   * the copies held in this hashtable.. if the DBSchemaEdit session
   * is aborted, newBases is thrown away.  If the DBSchemaEdit session
   * is confirmed, newBases replaces store.db.objectBases.
   */

  Hashtable<Short, DBObjectBase> newBases;

  /**
   * The original vector of namespace objects extant at the time the
   * DBSchemaEdit editing session is established.
   */

  Vector<DBNameSpace> oldNameSpaces;

  /**
   * Root node of the working DBBaseCategory tree.. if the
   * DBSchemaEdit session is committed, this DBBaseCategory tree
   * will replace store.rootCategory.
   */

  DBBaseCategory rootCategory;

  /* -- */

  /**
   * Constructor.  This constructor should only be called in a
   * critical section synchronized on the Ganymede.db.lockSync object.
   */

  public DBSchemaEdit() throws RemoteException
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

    locked = true;

    store = Ganymede.db;

    // make editable copies of the object bases

    synchronized (store)
      {
        // duplicate the existing category tree and all the contained
        // bases

        newBases = new Hashtable<Short, DBObjectBase>();

        // this DBBaseCategory constructor recursively copies the
        // bases referenced from store.rootCategory into newBases,
        // making copies for us to edit along the way.

        rootCategory = new DBBaseCategory(store, store.rootCategory, newBases, this);

        // make a shallow copy of our namespaces vector.. note that
        // since DBNameSpace's are immutable once created, we don't
        // need to worry about creating new ones, or about correcting
        // the DBNameSpace references in the duplicated
        // DBObjectBaseFields.

        // we use oldNameSpaces to undo any namespace additions or deletions
        // we do in store.nameSpaces during our editing.

        // note that we'll have to change our namespaces logic if/when
        // DBNameSpace objects become mutable.

        oldNameSpaces = new Vector<DBNameSpace>();

        for (DBNameSpace ns: store.nameSpaces)
          {
            Ganymede.rmi.publishObject(ns);

            oldNameSpaces.add(ns);
          }
      }

    Ganymede.rmi.publishObject(this);
  }

  /**
   * Returns the root category node from the server
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public Category getRootCategory()
  {
    return rootCategory;
  }

  /**
   * Gets a category from the category list, by it's fully qualified
   * path name.
   */

  public CategoryNode getCategoryNode(String pathName)
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
                CategoryNode cn = bc.getNode(tokens.sval);

                if (cn instanceof DBBaseCategory)
                  {
                    bc = (DBBaseCategory) cn;
                  }
                else if (cn instanceof DBObjectBase)
                  {
                    return cn;
                  }
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
   * <p>Returns an array of Base references from the current
   * (non-committed) state of the system.</p>
   *
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public Base[] getBases(boolean embedded)
  {
    return (Base[]) this.getDBBases(embedded);
  }

  /**
   * <p>Returns an array of DBObjectBase from the current
   * (non-committed) state of the system.</p>
   *
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   */

  public synchronized DBObjectBase[] getDBBases(boolean embedded)
  {
    DBObjectBase[] bases;
    int i = 0;
    int size = 0;

    /* -- */

    // calculate how many bases of the proper type (embedded or
    // non-embedded) we're going to return to the caller

    for (DBObjectBase base: newBases.values())
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

    // and create the return list

    bases = new DBObjectBase[size];

    for (DBObjectBase base: newBases.values())
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

    return bases;
  }

  /**
   * <p>Returns an array of Base references from the current
   * (non-committed) state of the system.</p>
   *
   * @param embedded If true, getBases() will only show bases that are intended
   * for embedding in other objects.  If false, getBases() will only show bases
   * that are not to be embedded.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public Base[] getBases()
  {
    return (Base[]) this.getDBBases();
  }

  /**
   * <p>Returns an array of DBObjectBases from the current
   * (non-committed) state of the system.</p>
   */

  public synchronized DBObjectBase[] getDBBases()
  {
    DBObjectBase[] bases;
    int i = 0;

    /* -- */

    bases = new DBObjectBase[newBases.size()];

    for (DBObjectBase base: newBases.values())
      {
        bases[i++] = base;
      }

    return bases;
  }

  /**
   * Returns a {@link arlut.csd.ganymede.rmi.Base Base} reference to match the id, or
   * null if no match.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public Base getBase(short id)
  {
    return newBases.get(Short.valueOf(id));
  }

  /**
   * Returns a {@link arlut.csd.ganymede.rmi.Base Base} reference to match the baseName,
   * or null if no match.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public synchronized Base getBase(String baseName)
  {
    for (DBObjectBase base: newBases.values())
      {
        if (base.getName().equalsIgnoreCase(baseName))
          {
            return (Base) base;
          }
      }

    return null;
  }

  /**
   * This method creates a new {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} object and returns
   * a remote handle to it so that the admin client can set fields on
   * the base, set attributes, and generally make a nuisance of
   * itself.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public synchronized Base createNewBase(Category category, boolean embedded, boolean lowRange)
  {
    short id;

    if (lowRange)
      {
        for (id = 0; id < 256; id++)
          {
            if (getBase(id) == null)
              {
                break;
              }
          }

        if (id == 256)
          {
            return null;
          }
      }
    else
      {
        for (id = 256; id <= Short.MAX_VALUE; id++)
          {
            if (getBase(id) == null)
              {
                break;
              }
          }
      }

    return createNewBase(category, embedded, id);
  }

  /**
   * This method creates a new {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} object and returns
   * a remote handle to it so that the admin client can set fields on
   * the base, set attributes, and generally make a nuisance of
   * itself.
   */

  public synchronized Base createNewBase(Category category, boolean embedded, short id)
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

    if (store.getObjectBase(id) != null)
      {
        throw new RuntimeException("Error, DBSchemaEdit.createNewBase() tried to create a new base " +
                                   "with a pre-allocated id: " + id);
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

    String newName = ts.l("createNewBase.new_base"); // "New Base"

    int i = 2;

    while (getBase(newName) != null)
      {
        // "New Base {0,number,#}"
        newName = ts.l("createNewBase.new_base_indexed", Integer.valueOf(i++));
      }

    base.setName(newName);

    try
      {
        path = category.getPath();
      }
    catch (RemoteException ex)
      {
        Ganymede.logError(ex);
        throw new RuntimeException("should never happen " + ex.getMessage());
      }

    if (debug)
      {
        Ganymede.debug("DBSchemaEdit: title is " + newName + ", working to add to category " + path);
      }

    // getCategoryNode can also return DBObjectBases, if the path
    // points to a leaf base rather than a category.. but that should
    // never happen in this context, so we'll let the
    // ClassCastExceptions fall where they may.

    localCat = (DBBaseCategory) getCategoryNode(path);

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

    localCat.addNodeAfter(base, null);

    if (debug)
      {
        Ganymede.debug("DBSchemaEdit: created base: " + base.getKey());
      }

    return base;
  }

  /**
   * This method deletes a {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}, removing it from the
   * Schema Editor's working set of bases.  The removal won't
   * take place for real unless the SchemaEdit is committed.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
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
        // "Schema Editing Error"
        // "Deletion Error.\nObject Base "{0}" not found in the Ganymede DBStore."
        return Ganymede.createErrorDialog(ts.l("global.schema_error"),
                                          ts.l("deleteBase.none_such", baseName));
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

    tmpBase = newBases.get(Short.valueOf(id));

    if (tmpBase.getObjectCount() > 0)
      {
        // "Schema Editing Error"
        // "Deletion Error.\nObject Base "{0}" is currently in use in the Ganymede DBStore."
        return Ganymede.createErrorDialog(ts.l("global.schema_error"),
                                          ts.l("deleteBase.in_use", tmpBase.getName()));
      }

    parent = tmpBase.getCategory();
    parent.removeNode(tmpBase);
    newBases.remove(Short.valueOf(id));

    return null;
  }

  /**
   * This method returns an array of defined
   * {@link arlut.csd.ganymede.rmi.NameSpace NameSpace} objects.
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public synchronized NameSpace[] getNameSpaces()
  {
    NameSpace[] spaces;
    int i;

    /* -- */

    synchronized (store)
      {
        spaces = new NameSpace[store.nameSpaces.size()];
        i = 0;

        for (DBNameSpace ns: store.nameSpaces)
          {
            spaces[i++] = ns;
          }
      }

    return spaces;
  }

  /**
   * This method returns a {@link arlut.csd.ganymede.rmi.NameSpace NameSpace} by matching name,
   * or null if no match is found.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public synchronized NameSpace getNameSpace(String name)
  {
    synchronized (store)
      {
        for (DBNameSpace ns: store.nameSpaces)
          {
            if (ns.getName().equals(name))
              {
                return ns;
              }
          }
      }

    return null;
  }

  /**
   * <p>This method creates a new {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace}
   * object and returns a remote handle
   * to it so that the admin client can set attributes on the DBNameSpace,
   * and generally make a nuisance of itself.</p>
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
   */

  public synchronized NameSpace createNewNameSpace(String name, boolean caseInsensitive)
  {
    DBNameSpace ns;

    /* -- */

    if (!locked)
      {
        throw new RuntimeException("already released/committed");
      }

    synchronized (store)
      {
        ns = new DBNameSpace(name, caseInsensitive);

        store.nameSpaces.add(ns);
      }

    Ganymede.rmi.publishObject(ns);

    return ns;
  }

  /**
   * <p>This method deletes a {@link
   * arlut.csd.ganymede.server.DBNameSpace DBNameSpace} object,
   * returning true if the deletion could be carried out, false
   * otherwise.</p>
   *
   * @see arlut.csd.ganymede.rmi.SchemaEdit
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

    // we declare index outside of this loop, and use it to keep track
    // of the namespace to remove

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
        // "Schema Editing Error"
        // "Namespace Deletion Error.\nNamespace "{0}" not found in the Ganymede DBStore."
        return Ganymede.createErrorDialog(ts.l("global.schema_error"),
                                          ts.l("deleteNameSpace.missing_namespace", name));
      }

    // check to make sure this namespace isn't tied to a field still

    for (DBObjectBase base: newBases.values())
      {
        Vector<DBObjectBaseField> fieldDefs = base.getFields();

        for (DBObjectBaseField fieldDef: fieldDefs)
          {
            if (fieldDef.getNameSpace() == ns)
              {
                // "Schema Editing Error"
                // "Namespace Deletion Error.\nNamespace "{0}" is currently in use, bound to {1}."
                return Ganymede.createErrorDialog(ts.l("global.schema_error"),
                                                  ts.l("deleteNameSpace.in_use", name, fieldDef));
              }
          }
      }

    store.nameSpaces.removeElementAt(index);

    return null;
  }

  /**
   * <p>This method deletes all {@link
   * arlut.csd.ganymede.server.DBNameSpace DBNameSpace} objects in the
   * server's schema that are not currently attached to any
   * namespace-constrained field.</p>
   */

  public ReturnVal deleteUnusedNameSpaces() throws RemoteException
  {
    Iterator<DBNameSpace> it = store.nameSpaces.iterator();

    while (it.hasNext())
      {
        DBNameSpace space = it.next();

        boolean ok_to_delete = true;

        Iterator<DBObjectBase> obIt = newBases.values().iterator();

        while (ok_to_delete && obIt.hasNext())
          {
            DBObjectBase base = obIt.next();

            Vector fieldDefs = base.getFields();

            for (int i = 0; ok_to_delete && i < fieldDefs.size(); i++)
              {
                DBObjectBaseField fieldDef = (DBObjectBaseField) fieldDefs.elementAt(i);

                if (fieldDef.getNameSpace() == space)
                  {
                    ok_to_delete = false;
                  }
              }
          }

        if (ok_to_delete)
          {
            it.remove();
          }
      }

    return null;
  }

  /**
   * <p>Commits this schema edit, replaces the server's schema with
   * the modified schema produced by this DBSchemaEdit session.</p>
   *
   * <p>It is an error to attempt any schema editing operations after this
   * method has been called.</p>
   */

  public synchronized ReturnVal commit()
  {
    ReturnVal retVal = null;

    /* -- */

    Ganymede.debug("DBSchemaEdit: commiting schema changes");

    if (!locked)
      {
        throw new RuntimeException("already released/committed");
      }

    // do commit here

    synchronized (store)
      {
        // make sure our schema passes all required constraints.

        retVal = checkCommitState();

        if (retVal != null)
          {
            return retVal;
          }

        // Clear the Jython class cache

        JythonEditObjectFactory.unloadAllURIs();

        // first the new object bases

        for (DBObjectBase base: newBases.values())
          {
            if (debug)
              {
                System.err.println("Checking in " + base);
              }

            base.clearEditor();
          }

        // now the namespaces.  we won't worry about the oldNameSpaces, GC should
        // take care of those for us.

        for (DBNameSpace ns: store.nameSpaces)
          {
            ns.schemaEditCommit();
          }

        unexportNameSpaces();

        // ** need to unlink old objectBases / rootCategory for GC here? **

        // all the bases already have containingHash pointing to
        // newBases

        store.objectBases = newBases;
        rootCategory.clearEditor();
        store.rootCategory = rootCategory;
      }

    // update the serialized representation of the
    // category/base structure.. note that we want it to be
    // created with supergash privs.

    Ganymede.catTransport = store.rootCategory.getTransport(null, false);

    try
      {
        Ganymede.baseTransport = null; // to force internalSession to regenerate it next
        Ganymede.baseTransport = Ganymede.internalSession.getBaseList();
      }
    catch (NotLoggedInException ex)
      {
        // oops, something odd got fubar'ed, but we don't want to
        // abort the commit for this.. let's record what happened

        Ganymede.debug(Ganymede.stackTrace(ex));
      }

    Ganymede.debug("DBSchemaEdit: schema changes committed.");

    // disallow any more schema editing activity

    locked = false;

    // and dump the changed schema out to disk before we allow any
    // transactions

    GanymedeAdmin.setState("Dumping Database");

    Ganymede.debug("DBSchemaEdit: Dumping Database.");

    boolean dumpedOk = false;

    while (!dumpedOk)
      {
        try
          {
            Ganymede.db.dump(Ganymede.dbFilename, true, true); // release, archive

            dumpedOk = true;
          }
        catch (IOException ex)
          {
            Ganymede.logError(ex);
            dumpedOk = true;    // if we had an io exception, retrying isn't likely to succeed
          }
        catch (InterruptedException ex)
          {
            Ganymede.logError(ex);
            Ganymede.debug("DBSchemaEdit: retrying database dump");
          }
      }

    // and unlock the server.
    //
    // Notice that we are not doing anything to clear any internal
    // GanymedeSessions like GanymedeServer.loginSession, because
    // GanymedeSession isn't keeping any references to the old
    // DBObjectBase / DBObjectBaseField definitions.

    GanymedeAdmin.setState(DBStore.normal_state); // "Normal Operation"

    Ganymede.debug("DBSchemaEdit: Re-enabling logins.");

    GanymedeServer.lSemaphore.enable("schema edit");

    return null;
  }

  /**
   * This private helper method verifies that the proposed schema is
   * acceptable to be committed.
   */

  private synchronized ReturnVal checkCommitState()
  {
    ReturnVal retVal = null;

    for (DBObjectBase base: newBases.values())
      {
        retVal = base.checkSchemaState();

        if (retVal != null)
          {
            return retVal;
          }
      }

    return null;
  }

  /**
   * <p>Abort this schema edit, return the schema to its prior state.</p>
   *
   * <p>It is an error to attempt any schema editing operations after this
   * method has been called.</p>
   */

  public synchronized void release()
  {
    Ganymede.debug("DBSchemaEdit: releasing");

    if (!locked)
      {
        throw new RuntimeException("already released/committed");
      }

    synchronized (store)
      {
        unexportNameSpaces();

        // restore the namespace vector
        store.nameSpaces.setSize(0);
        store.nameSpaces = oldNameSpaces;

        for (DBNameSpace ns: store.nameSpaces)
          {
            ns.schemaEditAbort();
          }
      }

    // unlock the server

    Ganymede.debug("DBSchemaEdit: released");

    GanymedeAdmin.setState(DBStore.normal_state);

    locked = false;

    // speed gc

    newBases.clear();
    newBases = null;

    // we've already copied and unexported oldNameSpaces, no need to
    // keep a ref here

    oldNameSpaces = null;

    // ditch rootCategory or else we'll keep a bunch of stuff pinned
    // in memory until the admin console releases its remote reference
    // to us.

    rootCategory = null;

    GanymedeServer.lSemaphore.enable("schema edit");

    return;
  }

  private void unexportNameSpaces()
  {
    if (oldNameSpaces != null)
      {
        for (DBNameSpace ns: oldNameSpaces)
          {
            Ganymede.rmi.unpublishObject(ns, true);
          }
      }

    for (DBNameSpace ns: store.nameSpaces)
      {
        Ganymede.rmi.unpublishObject(ns, true);
      }
  }

  /**
   * This method is called when the client loses connection.  unreferenced()
   * should do cleanup.
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
