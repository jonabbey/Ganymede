/*
   GASH 2

   DBObject.java

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
                                                                        DBObject

Class to hold a database object as represented in the DBStore.

Note that we don't include the object type in the object explicitly.. this
is encoded by the objectBase reference.

A DBObject does not allow its fields to be directly modified by anyone once the
object is instantiated.  When a user wants to edit an object, he gets a handle
to it from the DBStore, then calls createShadow to get an editable version
of the object.  If the user is satisfied with the changes made to the object,
the user will cause the editSet to commit, which will get a write lock on
the database, then replace the old version of this object with the modified
shadow object.  The shadow object field of the replaced object is cleared,
and then someone else can pull the object out for editing.

Changes made to the shadow object do not affect the original object until
the transaction is committed;  other processes can examine the current
contents of the object until commit time.

This is kind of like modern dating.

------------------------------------------------------------------------------*/

public class DBObject {

  DBObjectBase objectBase;
  int id;			// 32 bit id - the object's invariant id
  short fieldcount;
  Hashtable fields;
  DBEditObject shadowObject;	// if this object is being edited

  /* -- */

  DBObject(DBObjectBase objectBase)
  {
    this.objectBase = objectBase;
    id = 0;
    fieldcount = 0;
    fields = null;
    shadowObject = null;
  }

  DBObject(DBObjectBase objectBase, DataInputStream in) throws IOException
  {
    this.objectBase = objectBase;
    shadowObject = null;
    receive(in);
  }

  void emit(DataOutputStream out) throws IOException
  {
    Enumeration enum;
    short key;

    /* -- */

    out.writeInt(id);
    out.writeShort(fieldcount);

    enum = fields.keys();

    while (enum.hasMoreElements())
      {
	key = (Integer) enum.nextElement();
	out.writeShort((short) key);
	((DBField) fields.get(new Integer(key))).emit(out);
      }
  }

  void receive(DataInputStream in) throws IOException
  {
    DBField tmp;
    DBArrayField tmp2;
    DBObjectBaseField definition;
    short fieldcode;
    short type;
    Integer fieldINT;

    /* -- */

    // get our unique id

    id = in.readInt();

    // get number of fields

    fieldcount = in.readShort();

    fields =  new Hashtable(fieldcount * 1.5);

    for (int i = 0; i < fieldcount; i++)
      {
	// read our field code, look it up in our
	// DBObjectBase

	fieldcode = in.readShort();
	fieldINT = new Integer(fieldcode);

	definition = (DBObjectBaseField) base.fieldHash.get(fieldINT);

	type = definition.field_type;

	// can't use a switch since the compiler doesn't take DBStore.WHATEVER
	// as a legal switch label

	if (type == DBStore.BOOLEAN)
	  { 
	    tmp = new BooleanDBField(in, definition);
	  }
	else if (type == DBStore.BOOLEANARRAY)
	  {
	    tmp = new BooleanArrayDBField(in, definition);
	  }
	else if (type == DBStore.NUMERIC)
	  {
	    tmp = new NumericDBField(in, definition);
	  }
	else if (type == DBStore.NUMERICARRAY)
	  {
	    tmp = new NumericArrayDBField(in, definition);
	  }
	else if (type == DBStore.DATE)
	  {
	    tmp = new DateDBField(in, definition);
	  }
	else if (type == DBStore.DATEARRAY)
	  {
	    tmp = new DateArrayDBField(in, definition);
	  }
	else if (type == DBStore.STRING)
	  {
	    tmp = new StringDBField(in, definition);
	  }
	else if (type == DBStore.STRINGARRAY)
	  {
	    tmp = new StringArrayDBField(in, definition);
	  }
	else if (type == DBStore.INVID)
	  {
	    tmp = new InvidDBField(in, definition);
	  }
	else if (type == DBStore.INVIDARRAY)
	  {
	    tmp = new InvidArrayDBField(in, definition);
	  }
	else
	  {
	    throw new Error("Don't recognize field type in datastore");
	  }

	if (definition.namespace != null)
	  {
	    if (tmp instanceof DBArrayField)
	      {
		tmp2 = (DBArrayField) tmp;

		// mark the elements in the vector in the namespace
		// note that we don't use the namespace mark method here, 
		// because we are just setting up the namespace, not
		// manipulating it in the context of an editset

		for (int i = 0; i < tmp2.size(); i++)
		  {
		    if (definition.namespace.uniqueHash.containsKey(tmp2.key(i)))
		      {
			throw new RuntimeException("Duplicate unique value detected: " + tmp2.key(i));
		      } 

		    definition.namespace.uniqueHash.put(tmp2.key(i), new DBNameSpaceHandle(null, true, tmp2));
		  }
	      }
	    else
	      {
		// mark the scalar value in the namespace
		
		if (definition.namespace.uniqueHash.containsKey(tmp.key()))
		  {
		    throw new RuntimeException("Duplicate unique value detected: " + tmp.key());
		  }

		definition.namespace.uniqueHash.put(tmp.key(), new DBNameSpaceHandle(null, true, tmp));
	      }
	  }
	    
	// now add the field to our fields hash
	
	fields.put(fieldINT, tmp);
      }
  }

  // If this object is being edited, we say that it has a shadow
  // object;  a thread gets a copy of this object.. the copy
  // is actually a DBEditObject, which has the intelligence to
  // allow the client to modify the (copies of the) data fields.

  synchronized public DBEditObject createShadow(DBEditSet editset)
  {
    if (shadowObject != null)
      {
	return null;
      }

    shadowObject = new DBEditObject(this, editset);

    editset.addChangedObject(shadowObject);
    
    return shadowObject;
  }
  
  public DBField viewField(short id)
  {
    return (DBField) fields.get(new Integer(id));
  }
}
