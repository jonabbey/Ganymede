/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.16 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.lang.reflect.*;
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
 * This class is the data dictionary and object store for a particular kind of
 * object in the DBObjectStore.
 *
 */

public class DBObjectBase extends UnicastRemoteObject implements Base {

  static boolean debug = true;

  public static void setDebug(boolean val)
  {
    System.err.println("DBObjectBase.setDebug(): " + val);
    debug = val;
  }

  /* - */

  DBStore store;

  // schema info from the DBStore file

  String object_name;
  String classname;
  Class classdef;
  short type_code;

  // runtime data

  Hashtable fieldHash;		// field dictionary
  Hashtable objectHash;		// objects in our objectBase
  int object_count;
  int maxid;			// highest invid to date

  // used by the DBLock Classes to synchronize client access

  DBLock currentLock;
  Vector writerList, readerList, dumperList;
  boolean writeInProgress;
  boolean dumpInProgress;

  // Used to keep track of schema editing

  DBSchemaEdit editor;
  DBObjectBase original;	
  boolean save;
  boolean changed;

  /* -- */

  /**
   *
   * Default constructor.
   *
   */

  public DBObjectBase(DBStore store) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    writerList = new Vector();
    readerList = new Vector();
    dumperList = new Vector();
    this.store = store;
    object_name = "";
    classname = "";
    classdef = null;
    type_code = 0;
    fieldHash = new Hashtable();
    objectHash = new Hashtable();
    maxid = 0;

    editor = null;
    original = null;
    save = true;		// by default, we'll want to keep this
    changed = false;
  }

  /**
   *
   * Creation constructor.  Used when the schema editor interface is
   * used to create a new DBObjectBase.
   *
   */

  public DBObjectBase(DBStore store, short id, DBSchemaEdit editor) throws RemoteException
  {
    this(store);
    type_code = id;
    this.editor = editor;
  }

  /**
   *
   * receive constructor.  Used to initialize this DBObjectBase from disk
   * and load the objects of this type in from the standing store.
   *
   */

  public DBObjectBase(DataInput in, DBStore store) throws IOException, RemoteException
  {
    this(store);
    receive(in);
  }

  /**
   *
   * copy constructor.  Used to create a copy that we can play with for
   * schema editing.
   *
   */

  public DBObjectBase(DBObjectBase original, DBSchemaEdit editor) throws RemoteException
  {
    this(original.store);
    this.editor = editor;

    object_name = original.object_name;
    classname = original.classname;
    classdef = original.classdef;
    type_code = original.type_code;
    
    // make copies of all the old field definitions
    // for this object type, and save them into our
    // own field hash.
    
    Enumeration enum;
    DBObjectBaseField field;
    
    enum = original.fieldHash.elements();

    while (enum.hasMoreElements())
      {
	field = (DBObjectBaseField) enum.nextElement();
	fieldHash.put(field.getKey(),
		      new DBObjectBaseField(field, editor));
      }

    // remember the objects

    objectHash = original.objectHash;

    maxid = original.maxid;
    
    changed = false;
    this.original = original;
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    int size;
    Enumeration enum;
    Enumeration baseEnum;

    /* -- */

    out.writeUTF(object_name);
    out.writeUTF(classname);
    out.writeShort(type_code);

    size = fieldHash.size();

    out.writeShort((short) size); // should have no more than 32k fields

    enum = fieldHash.elements();

    while (enum.hasMoreElements())
      {
	((DBObjectBaseField) enum.nextElement()).emit(out);
      }

    out.writeInt(objectHash.size());
   
    baseEnum = objectHash.elements();

    while (baseEnum.hasMoreElements())
      {
	((DBObject) baseEnum.nextElement()).emit(out);
      }
  }

  synchronized void receive(DataInput in) throws IOException
  {
    int size;
    DBObject tempObject;
    int temp_val;
    DBObjectBaseField field;

    /* -- */

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): enter");
      }

    object_name = in.readUTF();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): object base name: " + object_name);
      }

    classname = in.readUTF();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): class name: " + classname);
      }

    if (classname != null && !classname.equals(""))
      {
	try
	  {
	    classdef = Class.forName(classname);
	  }
	catch (ClassNotFoundException ex)
	  {
	    System.err.println("DBObjectBase.receive(): class definition could not be found: " + ex);
	    classdef = null;
	  }
      }

    type_code = in.readShort();	// read our index for the DBStore's objectbase hash

    size = in.readShort();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): " + size + " fields in dictionary");
      }

    if (size > 0)
      {
	fieldHash = new Hashtable(size);
      }
    else
      {
	fieldHash = new Hashtable();
      }

    // read in the field dictionary for this object

    for (int i = 0; i < size; i++)
      {
	field = new DBObjectBaseField(in, this);
	fieldHash.put(new Short(field.field_code), field);
      }

    // read in the objects belonging to this ObjectBase

    object_count = in.readInt();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): reading " + object_count + " objects");
      }

    temp_val = (object_count > 0) ? object_count : 100;

    objectHash = new Hashtable(temp_val, (float) 0.5);

    for (int i = 0; i < object_count; i++)
      {
	tempObject = new DBObject(this, in);

	if (tempObject.id > maxid)
	  {
	    maxid = tempObject.id;
	  }

	objectHash.put(new Integer(tempObject.id), tempObject);
      }

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): maxid for " + object_name + " is " + maxid);
      }
  }

  /**
   *
   * Factory method to create a new DBEditObject of this
   * type.  The created DBEditObject will be connected
   * to the editset, and will not be integrated into the
   * DBStore until the editset is committed.
   *
   */

  synchronized DBEditObject createNewObject(DBEditSet editset)
  {
    DBEditObject 
      e_object = null;

    Invid invid;

    /* -- */

    invid = new Invid(getTypeID(), getNextID());

    if (classdef == null)
      {
	try
	  {
	    e_object = new DBEditObject(this, invid, editset);
	  }
	catch (RemoteException ex)
	  {
	    editset.getSession().setLastError("createNewObject failure: " + ex);
	    e_object = null;
	  }
      }
    else
      {
	Constructor c;
	Class classArray[];
	Object parameterArray[];

	classArray = new Class[3];

	classArray[0] = this.getClass();
	classArray[1] = invid.getClass();
	classArray[2] = editset.getClass();

	parameterArray = new Object[3];

	parameterArray[0] = this;
	parameterArray[1] = invid;
	parameterArray[2] = editset;

	try
	  {
	    c = classdef.getDeclaredConstructor(classArray);
	    e_object = (DBEditObject) c.newInstance(parameterArray);
	  }
	catch (NoSuchMethodException ex)
	  {
	  }
	catch (SecurityException ex)
	  {
	  }
	catch (IllegalAccessException ex)
	  {
	  }
	catch (IllegalArgumentException ex)
	  {
	  }
	catch (InstantiationException ex)
	  {
	  }
	catch (InvocationTargetException ex)
	  {
	  }
      }

    if (e_object == null)
      {
	return null;
      }

    if (e_object.initializeNewObject())
      {
	editset.addObject(e_object);
	return e_object;
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * allocate a new object id 
   *
   */

  synchronized int getNextID()
  {
    if (debug)
      {
	System.err.println("DBObjectBase.getNextID(): " + object_name + "'s maxid is " + maxid);
      }
    return ++maxid;
  }

  /** release an id if an object initially
   * created by createDBObject is rejected
   * due to its transaction being aborted
   *
   * note that we aren't being real fancy
   * here.. if this doesn't work, it doesn't
   * work.. we have 2 billion slots in this
   * object base after all..
   *
   */

  synchronized void releaseId(int id)
  {
    if (id==maxid)
      {
	maxid--;
      }
  }

  /**
   * return an enumeration of the current objects
   * in this DBObjectBase.. we need to make this
   * dependent on a DBReadLock in some fashion.
   */

  public DBEnum elements()
  {
    return new DBEnum(this);
  }

  /**
   * Print a debugging summary of the type information encoded
   * in this objectbase to a PrintStream.
   *
   * @param out PrintStream to print to.
   *
   */

  public synchronized void print(PrintStream out)
  {
    Enumeration enum;

    /* -- */

    out.println("ObjectBase: " + object_name + "(" + type_code + ")");
    
    enum = fieldHash.elements();
    while (enum.hasMoreElements())
      {
	out.print("\t");
	((DBObjectBaseField) enum.nextElement()).print(out);
      }
  }

  /**
   *
   * Returns the name of this object type
   *
   * @see arlut.csd.ganymede.Base
   */

  public String getName()
  {
    return object_name;
  }

  /**
   *
   * Sets the name for this object type
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized void setName(String newName)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    object_name = newName;
    changed = true;
  }

  /**
   *
   * Returns the name of the class managing this object type
   *
   * @see arlut.csd.ganymede.Base
   */
  
  public String getClassName()
  {
    return classname;
  }

  /**
   *
   * Sets the name for this object type
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized void setClassName(String newName)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    classname = newName;

    try
      {
	classdef = Class.forName(classname);
      }
    catch (ClassNotFoundException ex)
      {
	Ganymede.debug("class definition could not be found: " + ex);
	classdef = null;
      }

    changed = true;
  }

  /**
   *
   * Returns the class definition for this object type
   *
   */

  public Class getClassDef()
  {
    return classdef;
  }

  /**
   *
   * Returns true if the current session is permitted to
   * create an object of this type.
   *
   */

  public boolean canCreate(DBSession session)
  {

    // we're going to want to dispatch to the appropriate
    // DBEditObject subclasses canCreate() method.

    return false;
  }

  /**
   *
   * Returns true if the current session is permitted to
   * create an object of this type.
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean canCreate(Session session)
  {

    // we're going to want to dispatch to the appropriate
    // DBEditObject subclasses canCreate() method.

    return false;
  }

  /**
   *
   * Returns true if this object type can be inactivated
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized boolean canInactivate()
  {
    if (classdef == null)
      {
	return DBEditObject.canBeInactivated();
      }
    else
      {
	Method m;
	Boolean B;

	try
	  {
	    m = classdef.getDeclaredMethod("canInactivate", null);
	    B = (Boolean) m.invoke(null, null);
	    return B.booleanValue();
	  }
	catch (NoSuchMethodException ex)
	  {
	    throw new RuntimeException("couldn't call class method" + ex);
	  }
	catch (SecurityException ex)
	  {
	    throw new RuntimeException("couldn't call class method" + ex);
	  }
	catch (IllegalAccessException ex)
	  {
	    throw new RuntimeException("couldn't call class method" + ex);
	  }
	catch (IllegalArgumentException ex)
	  {
	    throw new RuntimeException("couldn't call class method" + ex);
	  }
	catch (InvocationTargetException ex)
	  {
	    throw new RuntimeException("couldn't call class method" + ex);
	  }
      }
  }

  /**
   *
   * Returns the invid type id for this object definition
   *
   * @see arlut.csd.ganymede.Base
   */

  public short getTypeID()
  {
    return type_code;
  }

  /**
   *
   * Returns the invid type id for this object definition as
   * a Short, suitable for use in a hash.
   *
   */

  public Short getKey()
  {
    return new Short(type_code);
  }

  /**
   *
   * Returns the field definitions for the objects stored in this
   * ObjectBase.
   *
   * Returns a vector of DBObjectBaseField objects.
   *
   * Question: do we want to return an array of DBFields here instead
   * of a vector?
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized Vector getFields()
  {
    Vector result;
    Enumeration enum;

    /* -- */

    result = new Vector();
    enum = fieldHash.elements();
    
    while (enum.hasMoreElements())
      {
	result.addElement(enum.nextElement());
      }

    return result;
  }

  /**
   *
   * This method creates a new base field and inserts it
   * into the DBObjectBase field definitions hash.  This
   * method can only be called from a DBSchemaEdit context.
   *
   * @see arlut.csd.ganymede.Base
   */
  
  public synchronized BaseField createNewField()
  {
    short id;
    DBObjectBaseField field;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    id = getNextFieldID();

    try
      {
	field = new DBObjectBaseField(this, editor);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't create field due to initialization error: " + ex);
      }

    // set its id

    field.setID(id);

    // and set it up in our field hash

    fieldHash.put(new Short(id), field);

    return field;
  }

  /**
   *
   * Get the next available field id for a new field.
   *
   */

  synchronized short getNextFieldID()
  {
    short id;
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    id = 0;

    enum = fieldHash.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();
	if (fieldDef.getID() >= id)
	  {
	    id = (short) (fieldDef.getID() + 1);
	  }
      }

    return id;
  }

  /**
   *
   * Clear the editing flag.  This disables the DBObjectBase
   * set methods on this ObjectBase and all dependent field
   * definitions.
   *
   */
  
  synchronized void clearEditor(DBSchemaEdit editor)
  {
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    Ganymede.debug("entered clearEditor");

    if (this.editor != editor)
      {
	throw new IllegalArgumentException("not editing");
      }
    
    this.editor = null;

    enum = fieldHash.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();
	fieldDef.editor = null;
      }

    original = null;
  }
}

