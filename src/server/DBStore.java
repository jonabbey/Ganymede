/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.26 $ %D%
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
 */

public class DBStore {

  // type identifiers used in the object store

  static final String id_string = "Gstore";
  static final byte major_version = 1;
  static final byte minor_version = 6;

  static final boolean debug = true;

  /* - */

  /*
    All of the following should only be modified/accessed
    in a critical section synchronized on the DBStore object.
   */
  
  boolean schemaEditInProgress;	// lock for schema revision
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
	in = new DataInputStream(inStream);

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

    lockHash = new Hashtable(baseCount);

    try 
      {
	journal = new DBJournal(this, "journal"); // need to parametrize filename
      }
    catch (IOException ex)
      {
	// what do we really want to do here?

	throw new RuntimeException("couldn't initialize journal");
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
    File dbFile = null;
    FileOutputStream outStream = null;
    DataOutputStream out = null;
    FileOutputStream textOutStream = null;
    PrintWriter textOut = null;
    Enumeration basesEnum;
    short baseCount, namespaceCount, categoryCount;
    DBDumpLock lock = null;
    DBNameSpace ns;
    DBBaseCategory bc;

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

	// and dump the whole thing

	outStream = new FileOutputStream(filename);
	out = new DataOutputStream(outStream);

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

	// and dump the schema out in a human readable form
	
	textOutStream = new FileOutputStream("/home/broccol/public_html/gash2/design/schema");
	textOut = new PrintWriter(textOutStream);
	//	printBases(textOut);
	printCategoryTree(textOut);
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

    if (journal != null)
      {
	journal.reset();
      }

    GanymedeAdmin.updateLastDump(new Date());
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
    File dbFile = null;
    FileOutputStream outStream = null;
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
	out = new DataOutputStream(outStream);

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

	    if (base.type_code == SchemaConstants.AdminBase ||
		 base.type_code == SchemaConstants.PermBase)
	      {
		base.emit(out, true); // gotta retain admin login ability
	      }
	    else
	      {
		base.emit(out, false);
	      }
	  } 

	// and dump the schema out in a human readable form
	
	textOutStream = new FileOutputStream("/home/broccol/public_html/gash2/design/schema");
	textOut = new PrintWriter(textOutStream);
	//	printBases(textOut);
	printCategoryTree(textOut);
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

    if (journal != null)
      {
	journal.reset();
      }

    GanymedeAdmin.updateLastDump(new Date());
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
    if (schemaEditInProgress)
      {
	throw new RuntimeException("can't login, the server's in schema edit mode");
      }

    return new DBSession(this, null, key);
  }

  /**
   *
   * <p>Do a printable dump of the category hierarchy</p>
   *
   *
   * @param out PrintStream to print to
   *
   */

  public synchronized void printCategoryTree(PrintWriter out)
  {
    rootCategory.print(out, "");
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
    Enumeration enum;

    /* -- */

    enum = objectBases.elements();

    while (enum.hasMoreElements())
      {
	((DBObjectBase) enum.nextElement()).print(out, "");
      }
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

  /**
   *
   * Returns a base id for a newly created base
   * 
   */

  public synchronized short getNextBaseID()
  {
    return maxBaseId++;
  }

  /**
   *
   * Let go of a baseId if the base create was not
   * committed.
   * 
   */

  public synchronized void releaseBaseID(short id)
  {
    if (id == maxBaseId)
      {
	maxBaseId--;
      }
  }

  /**
   * 
   * Method to replace/add a DBObjectBase in the DBStore.
   *
   */

  public synchronized void setBase(DBObjectBase base)
  {
    objectBases.put(base.getKey(), base);
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

    System.err.println("DBStore.getCategory(): searching for " + pathName);

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
		System.err.println("DBStore.getCategory(): Looking for node " + tokens.sval);
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
	
	ns = new DBNameSpace("username", true);
	nameSpaces.addElement(ns);

	ns = new DBNameSpace("access", true);
	nameSpaces.addElement(ns);

	// create admin base

	b = new DBObjectBase(this, false);
	b.object_name = "Admin";
	b.type_code = (short) SchemaConstants.AdminBase; // 0
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminGroupField;
	bf.field_type = FieldType.BOOLEAN;
	bf.field_name = "Group";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If this boolean is true, this administrator object represents a group of administrators";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminNameField;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Name";
	bf.loading = true;
	bf.setNameSpace("username");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Name of this admin group.. if this admin represents a user's admin privs, name is null";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminPasswordField;
	bf.field_type = FieldType.PASSWORD;
	bf.field_name = "Password";
	bf.maxLength = 32;
	bf.removable = false;
	bf.editable = false;
	bf.crypted = true;
	bf.comment = "Admin Password";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminMembersField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Members";
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Members of this admin group";
	bf.allowedTarget = SchemaConstants.AdminBase;
	bf.targetField = SchemaConstants.AdminGroupsField;
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminGroupsField;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Groups";
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Admin group this administrative account is a member of";
	bf.allowedTarget = SchemaConstants.AdminBase;
	bf.targetField = SchemaConstants.AdminMembersField;
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminAssocUser;
	bf.field_type = FieldType.INVID;
	bf.field_name = "User";
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If this administrative account is a user admin, the user account";
	bf.allowedTarget = SchemaConstants.UserBase;
	bf.targetField = SchemaConstants.UserAdminRole;
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminPrivs;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Privileges";
	bf.allowedTarget = SchemaConstants.PermBase;
	bf.array = true;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Permissions for this admin entity";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_order = bf.field_code = SchemaConstants.AdminObjectsOwned;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Objects owned";
	bf.allowedTarget = -2;	// any
	bf.targetField = SchemaConstants.OwnerListField;	// owner list field
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Permissions for this admin entity";
	b.fieldHash.put(new Short(bf.field_code), bf);

	b.setLabelField(SchemaConstants.AdminNameField);

	setBase(b);

	b = new DBObjectBase(this, false);
	b.object_name = "Access Set";
	b.type_code = (short) SchemaConstants.PermBase; // 1
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false); // add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PermName;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Set Name";
	bf.field_order = 2;
	bf.loading = true;
	bf.setNameSpace("access");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "What do we call this access category?";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.PermMatrix;
	bf.field_type = FieldType.PERMISSIONMATRIX;
	bf.field_name = "Access Bits";
	bf.field_order = 3;
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Access bits, by object type";
	b.fieldHash.put(new Short(bf.field_code), bf);

	b.setLabelField(SchemaConstants.PermName);

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
	bf.badChars = " :";
	bf.field_order = 2;
	bf.loading = true;
	bf.setNameSpace("username");
	bf.loading = false;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "User name for an individual privileged to log into Ganymede and/or the network";
	b.fieldHash.put(new Short(bf.field_code), bf);

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
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.UserAdminRole;
	bf.field_type = FieldType.INVID;
	bf.allowedTarget = SchemaConstants.AdminBase;
	bf.targetField = SchemaConstants.AdminAssocUser;
	bf.field_name = "Admin Role";
	bf.field_order = 4;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "If this user can act as an administrator, this field points to the admin object for this user";
	b.fieldHash.put(new Short(bf.field_code), bf);

	b.setLabelField(SchemaConstants.UserUserName);
    
	setBase(b);

	// Create history base

	b = new DBObjectBase(this, false);
	b.object_name = "History";
	b.type_code = (short) SchemaConstants.HistoryBase; // 3
	b.displayOrder = b.type_code;

	adminCategory.addNode(b, false, false);	// add it to the end is ok

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.HistoryDescrip;
	bf.field_type = FieldType.STRING;
	bf.field_name = "Event Description";
	bf.field_order = 0;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Description of a historical event";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.HistoryObjects;
	bf.field_type = FieldType.INVID;
	bf.field_name = "Objects";
	bf.allowedTarget = -2;	// any kind of object, but with a fixed target field
	bf.targetField = SchemaConstants.HistoryField;
	bf.editInPlace = false;
	bf.field_order = 1;
	bf.removable = false;
	bf.editable = false;
 	bf.comment = "List of objects involved in this event";
	b.fieldHash.put(new Short(bf.field_code), bf);

	bf = new DBObjectBaseField(b);
	bf.field_code = SchemaConstants.HistoryDate;
	bf.field_type = FieldType.DATE;
	bf.field_name = "Date";
	bf.field_order = 2;
	bf.removable = false;
	bf.editable = false;
	bf.comment = "Date this event occurred";
	b.fieldHash.put(new Short(bf.field_code), bf);

	b.setLabelField(SchemaConstants.UserUserName);

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
    // manually insert the root (supergash) admin object

    DBSession session = login("supergash");

    session.openTransaction();

    DBEditObject eO =(DBEditObject) session.createDBObject(SchemaConstants.AdminBase); // create a new admin object

    StringDBField s = (StringDBField) eO.getField("Name");
    s.setValue("supergash");
    
    PasswordDBField p = (PasswordDBField) eO.getField("Password");
    p.setPlainTextPass(GanymedeConfig.newSGpass); // default supergash password

    eO =(DBEditObject) session.createDBObject(SchemaConstants.AdminBase); // create a new admin object

    s = (StringDBField) eO.getField("Name");
    s.setValue("monitor");
    
    p = (PasswordDBField) eO.getField("Password");
    p.setPlainTextPass(GanymedeConfig.newMonpass); // default monitor password
    
    session.commitTransaction();
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

