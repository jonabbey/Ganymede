/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.13 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

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

public class DBObjectBase {

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
  boolean canInactivate;

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

  /* -- */

  public DBObjectBase(DBStore store)
  {
    writerList = new Vector();
    readerList = new Vector();
    dumperList = new Vector();
    this.store = store;
    object_name = null;
    classname = null;
    classdef = null;
    type_code = 0;
    canInactivate = false;
    fieldHash = null;
    objectHash = null;
    maxid = 0;
  }

  public DBObjectBase(DataInput in, DBStore store) throws IOException
  {
    writerList = new Vector();
    readerList = new Vector();
    dumperList = new Vector();
    this.store = store;
    receive(in);
  }

  void emit(DataOutput out) throws IOException
  {
    int size;
    Enumeration enum;
    Enumeration baseEnum;

    /* -- */

    out.writeUTF(object_name);
    out.writeUTF(classname);
    out.writeShort(type_code);
    out.writeBoolean(new Boolean(canInactivate));

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

  void receive(DataInput in) throws IOException
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

    canInactivate = in.readBoolean().booleanValue();

    type_code = in.readShort();	// read our index for the DBStore's objectbase hash

    size = in.readShort();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): " + size + " fields in dictionary");
      }

    fieldHash = new Hashtable(size);

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
    DBEditObject e_object;
    Invid invid;

    /* -- */

    invid = new Invid(getTypeID(), getNextID());

    if (classdef == null)
      {
	e_object = new DBEditObject(this, invid, editset);
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
	    e_object = c.newInstance(parameterArray);
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
	editSet.addObject(e_object);
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

  public void print(PrintStream out)
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
   */

  public String getName()
  {
    return object_name;
  }

  /**
   *
   * Returns the name of the class managing this object type
   *
   */
  
  public String getClassName()
  {
    return classname;
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
   * Returns true if this object type can be inactivated
   *
   */

  public boolean canInactivate()
  {
    return canInactivate;
  }

  /**
   *
   * Returns the invid type id for this object definition
   *
   */

  public int getTypeID()
  {
    return type_code;
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
   */

  public Vector getFields()
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
}

