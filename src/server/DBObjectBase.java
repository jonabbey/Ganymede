/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

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

  static boolean debug = false;

  static void setDebug(boolean val)
  {
    debug = val;
  }

  /* - */

  DBStore store;
  String object_name;
  short type_code;
  Hashtable fieldHash;		// field dictionary
  Hashtable objectHash;		// objects in our objectBase
  int object_count;
  int maxid;			// highest invid to date

  // Used by the DBLock Classes

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
    type_code = 0;
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

    type_code = in.readShort();

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
  }

  /**
   *
   * allocate a new object id 
   *
   */

  synchronized int getNextId()
  {
    return maxid++;
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
}

