/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import csd.DBStore.*;
import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBObjectBase

This class is the data dictionary and object store for a particular kind of
object in the DBObjectStore.

------------------------------------------------------------------------------*/

public class DBObjectBase {

  DBStore store;
  String object_name;
  short type_code;
  Hashtable fieldHash;		//  field dictionary
  Hashtable objectHash;		//  objects in our objectBase
  int object_count;
  DBLock currentLock;
  Vector writerList, readerList, dumperList;
  boolean writeInProgress;
  boolean dumpInProgress;

  /* -- */

  public DBObjectBase(DBStore store)
  {
    this.store = store;
    object_name = null;
    type_code = 0;
    fieldHash = null;
    objectHash = null;
  }

  public DBObjectBase(DataInputStream in, DBStore store) throws IOException
  {
    this.store = store;
    receive(in);
  }

  void emit(DataOutputStream out) throws IOException
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

  void receive(DataInputStream in) throws IOException
  {
    int size;
    DBObject tempObject;

    /* -- */

    object_name = in.readUTF();
    type_code = in.readShort();

    size = in.readShort();
    fieldHash = new Hashtable(size * 1.5);

    for (int i = 0; i < size; i++)
      {
	fieldHash.put(new Integer(type_code), new DBObjectBaseField(in, this));
      }

    object_count = in.readInt();

    objectHash = new Hashtable(object_count);

    for (int i = 0; i < object_count; i++)
      {
	tempObject = new DBObject(this, in);

	objectHash.put(new Integer(tempObject.id), tempObject);
      }
  }

  // return an enumeration of the current objects
  // in this DBObjectBase

  public DBEnum elements()
  {
    return new DBEnum(this);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

An entry in the DBObjectBase dictionary.  This class defines the type of
an object field, along with any namespace information pertaining to the field.

------------------------------------------------------------------------------*/

class DBObjectBaseField {

  DBObjectBase base;  
  String field_name;
  short field_code;
  short field_type;
  DBNameSpace namespace;

  /* -- */

  DBObjectBaseField(DBObjectBase base)
  {
    this.base = base;
    field_name = null;
    field_code = 0;
    field_type = 0;
    namespace = null;
  }

  DBObjectBaseField(DataInputStream in, DBObjectBase base) throws IOException
  {
    this.base = base;
    receive(in);
  }

  void emit(DataOutputStream out) throws IOException
  {
    int index;

    /* -- */

    out.writeUTF(field_name);
    out.writeShort(field_code);
    out.writeShort(field_type);
    if (namespace == null)
      {
	out.writeShort(-1);
      }
    else
      {
	index = base.nameSpaces.indexOf(namespace, 0);
	out.writeShort((short) index);
      }
  }

  void receive(DataInputStream in) throws IOException
  {
    int index;

    /* -- */

    field_name = in.readUTF();
    field_code = in.readShort();
    field_type = in.readShort();

    // read in our namespace index.  We use this to associate this field
    // with a (potentially preexisting) namespace.

    index = in.readShort();

    if (index == -1)
      {
	namespace = null;
      }
    else
      {
	// this field is associated with a namespace.

	if (index + 1 > base.nameSpaces.size())
	  {
	    base.nameSpaces.setSize(index + 1); 
	  }

	if (base.nameSpaces.elementAt(index) == null)
	  {
	    namespace = new DBNameSpace();
	    base.nameSpaces.setElement(namespace, index);
	  }
	else
	  {
	    namespace = (DBNameSpace) base.nameSpaces.elementAt(index);
	  }
      }
  }
}
