/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.61 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         DBStore

------------------------------------------------------------------------------*/

/**
 * <p>DBStore is the main data store class.  Any code that intends to make use
 * of the arlut.csd.ganymede package needs to instantiate an object of type DBStore.
 *
 * A user can have any number of DBStore objects active, but there is probably
 * no good reason for doing so since a single DBStore can store and cross reference
 * up to 32k different kinds of objects.</p>
 *
 * IMPORTANT NOTE: All synchronized methods in this class must do a this.notifyAll()
 * on exiting a synchronized block because the DBLock classes use DBStore as their
 * synchronization object.  If any do not, then the server can deadlock.
 *
 */

public class DBStore {

  // type identifiers used in the object store

  static final String id_string = "Gstore";
  static final byte major_version = 1;
  static final byte minor_version = 8;

  static final boolean debug = true;

  /* - */

  /*
    All of the following should only be modified/accessed
    in a critical section synchronized on the DBStore object.
   */
  
  boolean schemaEditInProgress;	// lock for schema revision
  boolean sweepInProgress;	// lock for invid sweep
  short maxBaseId = 256;	// to keep track of what ID to assign to new bases
  Hashtable objectBases;	// hash mapping object type to DBObjectBase's
  Hashtable lockHash;		// identifier keys for current locks
  Vector nameSpaces;		// unique valued hashes
  boolean loading = false;	// if true, DBObjectBase set methods will be enabled

  DBBaseCategory rootCategory;

  byte file_major, file_minor;

  DBJournal journal = null;

  // debugging info

  int objectsCheckedOut = 0;
  int locksHeld = 0;

  /* -- */

  /**
   *
   * This is the constructor for DBStore.
   *
   * Currently, once you construct a DBStore object, all you can do to
   * initialize it is call load().  This API needs to be extended to
   * provide for programmatic bootstrapping, or another tool needs
   * to be produced for the purpose.
   *
   */

  public DBStore()
  {
    objectBases = new Hashtable(20); // default 
    lockHash = new Hashtable(20); // default
    nameSpaces = new Vector();

    try
      {
	rootCategory = new DBBaseCategory(this, "Categories");
      }
    catch (RemoteException ex)
      {
	throw new Error("couldn't initialize rootCategory");
      }

    schemaEditInProgress = false;
    GanymedeAdmin.setState("Normal Operation");
  }

  /**
   *
   * Load the database from disk.
   *
   * This method loads both the database type
   * definition and database contents from a single disk file.
   *
   * @param filename Name of the database file
   * @see arlut.csd.ganymede.DBJournal
   *
   */

  public synchronized void load(String filename)
  {
    FileInputStream inStream = null;
    BufferedInputStream bufStream = null;
    DataInputStream in;

    DBObjectBase tempBase;
    short baseCount, namespaceCount, categoryCount;
    String namespaceID;
    boolean caseInsensitive;
    String file_id;

    /* -- */

    loading = true;

    nameSpaces.removeAllElements();

    try
      {
	inStream = new FileInputStream(filename);
	bufStream = new BufferedInputStream(inStream);
	in = new DataInputStream(bufStream);

	try
	  {
	    file_id = in.readUTF();
	    
	    if (!file_id.equals(id_string))
	      {
		System.err.println("DBStore initialization error: DBStore id mismatch for " + filename);
		throw new RuntimeException("DBStore initialization error (" + filename + ")");
	      }
	  }
	catch (IOException ex)
	  {
 	    System.err.println("DBStore initialization error: DBStore id read failure for " + filename);
	    System.err.println("IOException: " + ex);
	    throw new RuntimeException("DBStore initialization error (" + filename + ")");
	  }

	file_major = in.readByte();
	file_minor = in.readByte();

	if (debug)
	  {
	    System.err.println("DBStore load(): file version " + file_major + "." + file_minor);
	  }

	if (file_major != major_version)
	  {
	    System.err.println("DBStore initialization error: major version mismatch");
	    throw new Error("DBStore initialization error (" + filename + ")");
	  }

	// read in the namespace definitions

	namespaceCount = in.readShort();

	if (debug)
	  {
	    System.err.println("DBStore load(): loading " + namespaceCount + " namespaces");
	  }

	for (int i = 0; i < namespaceCount; i++)
	  {
	    nameSpaces.addElement(new DBNameSpace(in));
	  }

	// read in the object categories

	if (debug)
	  {
	    System.err.println("DBStore load(): loading  category definitions");
	  }

	if (file_major >= 1 && file_minor >= 3)
	  {
	    rootCategory = new DBBaseCategory(this, in);
	  }
	
	baseCount = in.readShort();

	if (debug)
	  {
	    System.err.println("DBStore load(): loading " + baseCount + " bases");
	  }

	if (baseCount > 0)
	  {
	    objectBases = new Hashtable(baseCount);
	  }
	else
	  {
	    objectBases = new Hashtable();	
	  }

	// Actually read in the object bases
	
	for (short i = 0; i < baseCount; i++)
	  {
	    tempBase = new DBObjectBase(in, this);
	    
	    setBase(tempBase);

	    if (debug)
	      {
		System.err.println("loaded base " + tempBase.getTypeID());
	      }

	    if (tempBase.getTypeID() > maxBaseId)
	      {
		maxBaseId = tempBase.getTypeID();
	      }
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore initialization error: couldn't properly process " + filename);
	System.err.println("IOException: " + ex);
	throw new RuntimeException("DBStore initialization error (" + filename + ")");
      }
    finally
      {
	if (bufStream != null)
	  {
	    try
	      {
		bufStream.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }

	if (inStream != null)
	  {
	    try
	      {
		inStream.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }

    lockHash = new Hashtable(baseCount); // reset lockHash

    try 
      {
	journal = new DBJournal(this, Ganymede.journalProperty);
      }
    catch (IOException ex)
      {
	// what do we really want to do here?

	ex.printStackTrace();

	throw new RuntimeException("couldn't initialize journal:" + ex.getMessage());
      }

    if (!journal.clean())
      {
	try
	  {
	    if (!journal.load())
	      {
		throw new RuntimeException("problem loading journal");
	      }
	    else
	      {
		// go ahead and consolidate the journal into the DBStore
		// before we really get under way.

		if (!journal.clean())
		  {
		    dump(filename, true);
		  }
	      }
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?

	    throw new RuntimeException("couldn't load journal");
	  }
      }

    loading = false;
  }

  /**
   *
   * Dump the database to disk
   *
   * This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.  Note that this method *will* dump the database, even
   * if no changes have been made.  You should check the DBStore journal's 
   * clean() method to determine whether or not a dump is really needed.
   *
   * The dump is guaranteed to be transaction consistent.
   *
   * @param filename Name of the database file to emit
   * @param releaseLock boolean.  If releaseLock==false, dump() will not release
   *                              the dump lock when it is done with the dump.  This
   *                              is intended to allow for a clean shut down.  For
   *                              non-terminal dumps, releaseLock should be true.
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   *
   */

  public synchronized void dump(String filename, boolean releaseLock) throws IOException
  {
    try
      {
	File dbFile = null;
	FileOutputStream outStream = null;
	BufferedOutputStream bufStream = null;
	DataOutputStream out = null;

	FileOutputStream textOutStream = null;
	PrintWriter textOut = null;

	Enumeration basesEnum;
	short baseCount, namespaceCount, categoryCount;
	DBDumpLock lock = null;
	DBNameSpace ns;
	DBBaseCategory bc;

	boolean movedFile = false;
	boolean completed = false;

	/* -- */

	if (debug)
	  {
	    System.err.println("DBStore: Dumping");
	  }

	lock = new DBDumpLock(this);

	try
	  {
	    lock.establish("System");	// wait until we get our lock 
	  }
	catch (InterruptedException ex)
	  {
	  }
    
	// Move the old version of the file to a backup
    
	try
	  {
	    dbFile = new File(filename);

	    if (dbFile.isFile())
	      {
		dbFile.renameTo(new File(filename + ".bak"));
	      }

	    movedFile = true;

	    // and dump the whole thing

	    outStream = new FileOutputStream(filename);
	    bufStream = new BufferedOutputStream(outStream);
	    out = new DataOutputStream(bufStream);

	    out.writeUTF(id_string);
	    out.writeByte(major_version);
	    out.writeByte(minor_version);

	    namespaceCount = (short) nameSpaces.size();

	    out.writeShort(namespaceCount);

	    for (int i = 0; i < namespaceCount; i++)
	      {
		ns = (DBNameSpace) nameSpaces.elementAt(i);
		ns.emit(out);
	      }

	    if (major_version >= 1 && minor_version >= 3)
	      {
		rootCategory.emit(out);
	      }

	    baseCount = (short) objectBases.size();

	    out.writeShort(baseCount);
	
	    basesEnum = objectBases.elements();

	    while (basesEnum.hasMoreElements())
	      {
		((DBObjectBase) basesEnum.nextElement()).emit(out, true);
	      } 

	    completed = true;	// we don't care as much about our schema dump

	    // and dump the schema out in a human readable form
	
	    if (Ganymede.htmlProperty != null)
	      {
		textOutStream = new FileOutputStream(Ganymede.htmlProperty);
		textOut = new PrintWriter(textOutStream);
		
		printCategoryTreeHTML(textOut);
	      }
	  }
	catch (IOException ex)
	  {
	    System.err.println("DBStore error dumping to " + filename);

	    throw ex;
	  }
	finally
	  {
	    if (releaseLock)
	      {
		if (lock != null)
		  {
		    lock.release();
		  }
	      }

	    if (out != null)
	      {
		out.close();
	      }

	    if (bufStream != null)
	      {
		bufStream.close();
	      }
	   
	    if (outStream != null)
	      {
		outStream.close();
	      }

	    if (textOut != null)
	      {
		textOut.close();
	      }

	    if (textOutStream != null)
	      {
		textOutStream.close();
	      }

	    // in case of thread dump

	    if (movedFile && !completed)
	      {
		dbFile = new File(filename + ".bak");
	    
		if (dbFile.isFile())
		  {
		    dbFile.renameTo(new File(filename));
		  }

		Ganymede.debug("dump aborted, undoing dump.");
	      }
	  }

	// if our thread was terminated above, we won't get here.  If
	// we've got here, we had an ok dump, and we don't need to
	// worry about the journal.. if it isn't truncated properly
	// somebody can just remove it and ganymede will recover
	// ok.

	if (journal != null)
	  {
	    journal.reset();
	  }

	GanymedeAdmin.updateLastDump(new Date());

	if (Ganymede.log != null)
	  {
	    Ganymede.log.logSystemEvent(new DBLogEvent("dump",
						       "Database dumped",
						       null,
						       null,
						       null,
						       null));
	  }
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Dump the schema to disk
   *
   * This method dumps the entire database to disk, minus any actual objects.
   *
   * The thread that calls the dump method will be suspended until
   * there are no threads performing update writes to the in-memory
   * database.  In practice this will likely never be a long interval.
   *
   * @param filename Name of the database file to emit
   * @param releaseLock boolean.  If releaseLock==false, dump() will not release
   *                              the dump lock when it is done with the dump.  This
   *                              is intended to allow for a clean shut down.  For
   *                              non-terminal dumps, releaseLock should be true.
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   * @see arlut.csd.ganymede.adminSession
   * 
   */

  public synchronized void dumpSchema(String filename, boolean releaseLock) throws IOException
  {
    try
      {
	File dbFile = null;
	FileOutputStream outStream = null;
	BufferedOutputStream bufStream = null;
	DataOutputStream out = null;

	FileOutputStream textOutStream = null;
	PrintWriter textOut = null;
	Enumeration basesEnum;
	short baseCount, namespaceCount, categoryCount;
	DBDumpLock lock = null;
	DBNameSpace ns;
	DBBaseCategory bc;
	DBObjectBase base;

	/* -- */

	if (debug)
	  {
	    System.err.println("DBStore: Dumping");
	  }

	lock = new DBDumpLock(this);

	try
	  {
	    lock.establish("System");	// wait until we get our lock 
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("DBStore.dumpSchema(): dump lock establish interrupted, schema not dumped");
	    return;
	  }
    
	// Move the old version of the file to a backup
    
	try
	  {
	    dbFile = new File(filename);

	    if (dbFile.isFile())
	      {
		dbFile.renameTo(new File(filename + ".bak"));
	      }

	    // and dump the whole thing

	    outStream = new FileOutputStream(filename);
	    bufStream = new BufferedOutputStream(outStream);
	    out = new DataOutputStream(bufStream);

	    out.writeUTF(id_string);
	    out.writeByte(major_version);
	    out.writeByte(minor_version);

	    namespaceCount = (short) nameSpaces.size();

	    out.writeShort(namespaceCount);

	    for (int i = 0; i < namespaceCount; i++)
	      {
		ns = (DBNameSpace) nameSpaces.elementAt(i);
		ns.emit(out);
	      }

	    if (major_version >= 1 && minor_version >= 3)
	      {
		rootCategory.emit(out);
	      }

	    baseCount = (short) objectBases.size();

	    out.writeShort(baseCount);
	
	    basesEnum = objectBases.elements();

	    while (basesEnum.hasMoreElements())
	      {
		base = (DBObjectBase) basesEnum.nextElement();

		if (base.type_code == SchemaConstants.OwnerBase ||
		    base.type_code == SchemaConstants.PersonaBase ||
		    base.type_code == SchemaConstants.PermBase ||
		    base.type_code == SchemaConstants.EventBase)
		  {
		    base.partialEmit(out); // gotta retain admin login ability
		  }
		else if (base.type_code == SchemaConstants.BuilderBase)
		  {
		    base.emit(out, true); // save the builder information
		  }
		else
		  {
		    base.emit(out, false); // just write out the schema info
		  }
	      } 

	    // and dump the schema out in a human readable form
	
	    textOutStream = new FileOutputStream("/home/broccol/public_html/gash2/design/schema.html");
	    textOut = new PrintWriter(textOutStream);

	    printCategoryTreeHTML(textOut);
	  }
	catch (IOException ex)
	  {
	    System.err.println("DBStore error dumping schema to " + filename);
	    throw ex;
	  }
	finally
	  {
	    if (releaseLock)
	      {
		if (lock != null)
		  {
		    lock.release();
		  }
	      }

	    if (out != null)
	      {
		out.close();
	      }

	    if (bufStream != null)
	      {
		bufStream.close();
	      }
	   
	    if (outStream != null)
	      {
		outStream.close();
	      }

	    if (textOut != null)
	      {
		textOut.close();
	      }

	    if (textOutStream != null)
	      {
		textOutStream.close();
	      }
	  }
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * <p>Get a session handle on this database</p>
   *
   * <p>This is intended primarily for internal use
   * for database initialization, hence the 'protected'.
   *
   * @param key Identifying key
   *
   */

  protected synchronized DBSession login(Object key)
  {
    try
      {
	DBSession retVal;

	/* -- */

	if (schemaEditInProgress)
	  {
	    throw new RuntimeException("can't login, the server's in schema edit mode");
	  }

	if (sweepInProgress)
	  {
	    throw new RuntimeException("can't login, the server is performing an invid sweep");
	  }

	retVal = new DBSession(this, null, key);

	return retVal;
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * <p>Do a printable dump of the category hierarchy</p>
   *
   *
   * @param out PrintStream to print to
   *
   */

  public synchronized void printCategoryTreeHTML(PrintWriter out)
  {
    try
      {
	out.println("<HTML><HEAD><TITLE>Ganymede Schema Dump -- " + new Date() + "</TITLE></HEAD>");
	out.println("<BODY BGCOLOR=\"#FFFFFF\"><H1>Ganymede Schema Dump -- " + new Date() + "</H1>");
	out.println("<HR>");

	rootCategory.printHTML(out);

	out.println("<HR>");
	out.println("Emitted: " + new Date());
	out.println("</BODY>");
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * <p>Do a printable dump of the object databases</p>
   *
   * @param out PrintStream to print to
   *
   */

  public synchronized void printBases(PrintWriter out)
  {
    try
      {
	Enumeration enum;

	/* -- */

	enum = objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    ((DBObjectBase) enum.nextElement()).print(out, "");
	  }
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Returns a vector of Strings, the names of the bases currently
   * defined in this DBStore.
   *
   */

  public synchronized Vector getBaseNameList()
  {
    Vector result = new Vector();

    try
      {
	Enumeration enum;

	/* -- */

	enum = objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    result.addElement(((DBObjectBase) enum.nextElement()).getName());
	  }
      }
    finally
      {
	this.notifyAll();
      }

    return result;
  }

  /**
   *
   * Returns the object definition class for the id class.
   *
   * @param id Type id for the base to be returned
   *
   */

  public DBObjectBase getObjectBase(Short id)
  {
    return (DBObjectBase) objectBases.get(id);
  }

  /**
   *
   * Returns the object definition class for the id class.
   *
   * @param id Type id for the base to be returned
   *
   */

  public DBObjectBase getObjectBase(short id)
  {
    return (DBObjectBase) objectBases.get(new Short(id));
  }

  /**
   *
   * Returns the object definition class for the id class.
   *
   * @param baseName Name of the base to be returned
   *
   */

  public synchronized DBObjectBase getObjectBase(String baseName)
  {
    try
      {
	DBObjectBase base;
	Enumeration enum;

	/* -- */

	enum = objectBases.elements();

	while (enum.hasMoreElements())
	  {
	    base = (DBObjectBase) enum.nextElement();
	
	    if (base.getName().equals(baseName))
	      {
		return base;
	      }
	  }

	return null;
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Returns a base id for a newly created base
   * 
   */

  public synchronized short getNextBaseID()
  {
    try
      {
	return maxBaseId++;
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Let go of a baseId if the base create was not
   * committed.
   * 
   */

  public synchronized void releaseBaseID(short id)
  {
    try
      {
	if (id == maxBaseId)
	  {
	    maxBaseId--;
	  }
      }
    finally
      {
	this.notifyAll();
      }
  }

  /**
   * 
   * Method to replace/add a DBObjectBase in the DBStore.
   *
   */

  public synchronized void setBase(DBObjectBase base)
  {
    try
      {
	objectBases.put(base.getKey(), base);
      }
    finally
      {
	this.notifyAll();
      }
  }

  public synchronized DBNameSpace getNameSpace(String name)
  {
    DBNameSpace namespace;

    /* -- */

    for (int i = 0; i < nameSpaces.size(); i++)
      {
	namespace = (DBNameSpace) nameSpaces.elementAt(i);

	if (namespace.name.equals(name))
	  {
	    return namespace;
	  }
      }

    return null;
  }

  /**
   *
   * Method to get a category from the category list, by
   * it's full path name.
   *
   */

  public synchronized DBBaseCategory getCategory(String pathName)
  {
    try
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

	// System.err.println("DBStore.getCategory(): searching for " + pathName);

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
		    // System.err.println("DBStore.getCategory(): Looking for node " + tokens.sval);

		    bc = (DBBaseCategory) bc.getNode(tokens.sval);

		    if (bc == null)
		      {
			System.err.println("DBStore.getCategory(): found null");
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
    finally
      {
	this.notifyAll();
      }
  }

  /**
   *
   * Initialization method for a newly created DBStore.. this
   * method creates a new Schema from scratch, defining the
   * mandatory Ganymede object types.
   *
   *
   * Note that editing this method to redefine the default bases
   * and fields should be matched by editing of DBObjectBase.isRemovable()
   * and DBObjectBaseField.isRemovable() and DBObjectBaseField.isEditable()
   */

  void initializeSchema()
  {
    DBObjectBase b;
    DBObjectBaseField bf;
    DBNameSpace ns;

    /* -- */

    loading = true;

    try
      {
	DBBaseCategory adminCategory = new DBBaseCategory(this, "Admin-Level Objects", rootCategory);
	rootCategory.addNode(adminCategory, false, false);

	ns = new DBNameSpace("ownerbase", true);
	nameSpaces.addElement(ns);
	
	ns = new DBNameSpace("username", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("access", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("persona", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("eventtoken", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("buildertask", true);
	nameSpaces.addElement(ns);

	// create owner base

	b = new DBObjectBase(this, false);
	b.object_name = "Owner Group";
	b.type_code = (short) SchemaConstants.OwnerBase; // 0
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.OwnerNameField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("ownerbase");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The name of this ownership group";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.OwnerMembersField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Members";
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaGroupsField;
	bf.comment = "List of admin personae that are members of this owner set";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.OwnerObjectsOwned;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Objects owned";
	bf.allowedTarget = -2;	// any
	bf.targetField = SchemaConstants.OwnerListField;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "What objects are owned by this owner set";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.OwnerMailList;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Mail List";
	bf.allowedTarget = -2;	// any (we'll depend on subclassing to refine this)
	bf.targetField = -1;	// use default backlink field
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "What email addresses should be notified of changes to objects owned?";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.OwnerNameField);

	setBase(b);

	// create persona base

	b = new DBObjectBase(this, false);
	b.object_name = "Admin Persona";
	b.type_code = (short) SchemaConstants.PersonaBase; // 1
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaNameField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.field_order = 0;
	bf.loading = true;
	bf.setNameSpace("persona");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The unique name for this admin persona";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PersonaPasswordField;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.field_order = 1;
	bf.removable = false;
	bf.editable = false;
	bf.crypted = true;
	bf.comment = "Persona password";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaGroupsField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Owner Sets";
	bf.allowedTarget = SchemaConstants.OwnerBase;	// any
	bf.targetField = SchemaConstants.OwnerMembersField;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "What owner sets are this persona members of?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaAssocUser;
	bf.field_type = FieldType.INVID;
	bf.field_name = "User";
	bf.allowedTarget = SchemaConstants.UserBase;	// any
	bf.targetField = SchemaConstants.UserAdminPersonae;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.array = false;
	bf.comment = "What user is this admin persona associated with?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaPrivs;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Privilege Sets";
	bf.allowedTarget = SchemaConstants.PermBase;
	bf.targetField = SchemaConstants.PermPersonae;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "What permission matrices are this admin persona associated with?";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaAdminConsole;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Admin Console";
	bf.array = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this persona can be used to access the admin console";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PersonaAdminPower;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Full Console";
	bf.array = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, this persona can kill users and edit the schema";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.PersonaNameField);

	setBase(b);

	// create Role base

	b = new DBObjectBase(this, false);
	b.object_name = "Role";
	b.type_code = (short) SchemaConstants.PermBase; // 2
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PermName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("access");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The name of this permission matrix";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PermMatrix;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Objects Owned Access Bits";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Access bits, by object type for objects owned by admins using this permission object";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PermDefaultMatrix;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Default Access Bits";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Access bits, by object type for all objects on the part of admins using this permission object";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.PermPersonae;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Persona entities";
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaPrivs;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "What personae are using this permission matrix?";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.PermName);

	setBase(b);

	// create System Events base

	DBBaseCategory eventCategory = new DBBaseCategory(this, "Events", adminCategory);
	adminCategory.addNode(eventCategory, false, false);

	b = new DBObjectBase(this, false);
	b.object_name = "System Event";
	b.type_code = (short) SchemaConstants.EventBase;  
	b.displayOrder = b.type_code;

	eventCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventToken;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Token";
	bf.badChars = " :";
	bf.field_order = 1;
	bf.loading = true;
	bf.setNameSpace("eventtoken");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Single-word token to identify this event type in Ganymede source code";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Name";
	bf.badChars = ":";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Short name for this event class, suitable for an email message title";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventDescription;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.badChars = ":";
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Fuller description for this event class, suitable for an email message body";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.EventMailBoolean;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Send Mail?";
	bf.field_order = 4;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, occurrences of this event will be emailed";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.EventMailList;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Fixed Mail List";
	bf.allowedTarget = -2;	// any (we'll depend on subclassing to refine this)
	bf.targetField = -1;	// use default backlink field
	bf.field_order = 5;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "List of email addresses to always send events of this type to";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.EventMailToSelf;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: Admin?";
	bf.field_order = 6;

	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, mail for this event will always be cc'ed to the admin performing the action";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.EventToken);
    
	setBase(b);

	// create Object Events base

	b = new DBObjectBase(this, false);
	b.object_name = "Object Event";
	b.type_code = (short) SchemaConstants.ObjectEventBase;  
	b.displayOrder = b.type_code;

	eventCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventToken;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Token";
	bf.badChars = " :";
	bf.field_order = 1;
	bf.loading = true;
	bf.setNameSpace("eventtoken");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Single-word token to identify this event type in Ganymede source code";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventObjectName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Object Type Name";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "The name of the object that this event is tracking";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Name";
	bf.badChars = ":";
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Short name for this event class, suitable for an email message title";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventDescription;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.badChars = ":";
	bf.field_order = 4;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Fuller description for this event class, suitable for an email message body";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventMailBoolean;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Send Mail?";
	bf.field_order = 5;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, occurrences of this event will be emailed";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventMailToSelf;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: Admin?";
	bf.field_order = 6;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, mail for this event will always be cc'ed to the admin performing the action";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventMailOwners;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Cc: Owner Groups?";
	bf.field_order = 7;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If true, mail for this event will always be cc'ed to the owner groups owning the object";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.ObjectEventMailList;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Fixed Mail List";
	bf.allowedTarget = -2;	// any (we'll depend on subclassing to refine this)
	bf.targetField = -1;	// use default backlink field
	bf.field_order = 8;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "List of email addresses to always send events of this type to";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.ObjectEventObjectType;
	bf.field_type = FieldType.NUMERIC;
	bf.field_name = "Object Type ID";
	bf.removable = false;
	bf.editable = false;
	bf.visibility = false;	// we don't want this to be seen by the client
	bf.comment = "The type code of the object that this event is tracking";
	b.fieldTable.put(bf);

	setBase(b);

	// create user base

	DBBaseCategory userCategory = new DBBaseCategory(this, "User-Level Objects", rootCategory);
	rootCategory.addNode(userCategory, false, false);

	b = new DBObjectBase(this, false);
	b.object_name = "User";
	b.type_code = (short) SchemaConstants.UserBase; // 2
	b.displayOrder = b.type_code;

	userCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserUserName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Username";
	bf.minLength = 2;
	bf.maxLength = 8;
	bf.badChars = " :=><|+[]\\/*;:.,?\""; // See p.252, teach yourself WinNT Server 4 in 14 days
	bf.field_order = 2;
	bf.loading = true;
	bf.setNameSpace("username");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "User name for an individual privileged to log into Ganymede and/or the network";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserPassword;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.field_order = 3;
	bf.removable = false;
	bf.editable = false;
	bf.crypted = true;
	bf.isCrypted();
	bf.comment = "Password for an individual privileged to log into Ganymede and/or the network";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserAdminPersonae;
	bf.field_type = FieldType.INVID;
	bf.allowedTarget = SchemaConstants.PersonaBase;
	bf.targetField = SchemaConstants.PersonaAssocUser;
	bf.field_name = "Admin Personae";
	bf.field_order = bf.field_code;
	bf.removable = false;
	bf.editable = false;
	bf.array = true;
	bf.comment = "A list of admin personae this user can assume";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.UserUserName);
    
	setBase(b);

	// create Builder Task Base

	b = new DBObjectBase(this, false);
	b.object_name = "Builder Task";
	b.type_code = (short) SchemaConstants.BuilderBase; // 5
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.BuilderTaskName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Task Name";
	bf.field_order = 1;
	bf.loading = true;
	bf.setNameSpace("buildertask");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Name of this plug-in builder task";
	b.fieldTable.put(bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.BuilderTaskClass;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Classname";
	bf.badChars = "/:";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Name of the plug-in class to load on server restart to handle this task";
	b.fieldTable.put(bf);

	b.setLabelField(SchemaConstants.BuilderTaskName);
    
	setBase(b);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("remote :" + ex);
      }

    loading = false;
  }

  void initializeObjects()
  {
    DBEditObject eO;
    Invid inv;
    StringDBField s;
    PasswordDBField p;
    InvidDBField i;
    BooleanDBField b;
    GanymedeSession gSession = null;
    DBSession session;
    PermissionMatrixDBField pm;
    
    /* -- */

    // manually insert the root (supergash) admin object

    try
      {
	gSession = new GanymedeSession();
      }
    catch (RemoteException ex)
      {
	throw new Error("RMI system could not initialize GanymedeSession");
      }

    session = gSession.session;
    session.openTransaction("DBStore bootstrap initialization");

    eO =(DBEditObject) session.createDBObject(SchemaConstants.OwnerBase, null); // create a new owner group 
    inv = eO.getInvid();

    s = (StringDBField) eO.getField("Name");
    s.setValue(Ganymede.rootname);
    
    // create a supergash admin persona object 

    eO =(DBEditObject) session.createDBObject(SchemaConstants.PersonaBase, null);

    s = (StringDBField) eO.getField("Name");
    s.setValue(Ganymede.rootname);
    
    p = (PasswordDBField) eO.getField("Password");
    p.setPlainTextPass(Ganymede.defaultrootpassProperty); // default supergash password

    i = (InvidDBField) eO.getField(SchemaConstants.PersonaGroupsField);
    i.addElement(inv);

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminConsole);
    b.setValue(new Boolean(true));

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminPower);
    b.setValue(new Boolean(true));

    // create a monitor admin persona object 

    eO =(DBEditObject) session.createDBObject(SchemaConstants.PersonaBase, null);

    s = (StringDBField) eO.getField("Name");

    if (Ganymede.monitornameProperty != null)
      {
	s.setValue(Ganymede.monitornameProperty);
      }
    else
      {
	throw new NullPointerException("monitor name property not loaded, can't initialize monitor account");
      }
    
    p = (PasswordDBField) eO.getField("Password");

    if (Ganymede.defaultmonitorpassProperty != null)
      {
	p.setPlainTextPass(Ganymede.defaultmonitorpassProperty); // default monitor password
      }
    else
      {
	throw new NullPointerException("monitor password property not loaded, can't initialize monitor account");
      }

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminConsole);
    b.setValue(new Boolean(true));

    b = (BooleanDBField) eO.getField(SchemaConstants.PersonaAdminPower);
    b.setValue(new Boolean(false));

    // create SchemaConstants.PermDefaultObj

    eO =(DBEditObject) session.createDBObject(SchemaConstants.PermBase, null); 

    s = (StringDBField) eO.getField(SchemaConstants.PermName);
    s.setValue("Default");

    // what can users do with objects they own?  Includes users themselves

    pm = (PermissionMatrixDBField) eO.getField(SchemaConstants.PermMatrix);
    pm.setPerm(SchemaConstants.UserBase, new PermEntry(true, false, false, false)); // view self, nothing else

    // what can arbitrary users do with objects they don't own?  nothing by default

    // pm = (PermissionMatrixDBField) eO.getField(SchemaConstants.PermDefaultMatrix);

    session.commitTransaction();
    gSession.logout();
  }

  /*

    -- The following methods are used to keep track of DBStore state and
       are reflected in updates to any connected Admin consoles.  These
       methods are for statistics keeping only. --

   */

  /**
   *
   * This method is used to increment the count of checked out objects.
   *
   */

  void checkOut()
  {
    objectsCheckedOut++;
    GanymedeAdmin.updateCheckedOut();
  }

  /**
   *
   * This method is used to decrement the count of checked out objects.
   *
   */

  void checkIn()
  {
    objectsCheckedOut--;
    GanymedeAdmin.updateCheckedOut();
  }

  /**
   *
   * This method is used to increment the count of held locks
   *
   */

  void addLock()
  {
    locksHeld++;
    GanymedeAdmin.updateLocksHeld();
  }

  /**
   *
   * This method is used to decrement the count of held locks
   *
   */

  void removeLock()
  {
    locksHeld--;
    GanymedeAdmin.updateLocksHeld();
  }

}

