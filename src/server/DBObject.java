/*
   GASH 2

   DBObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        DBObject

------------------------------------------------------------------------------*/

/**
 * <p>Class to hold a database object as represented in the DBStore.</p>
 *
 * <p>Note that we don't include the object type in the object explicitly.. this
 * is encoded by the objectBase reference.</p>
 *
 * <p>A DBObject does not allow its fields to be directly modified by anyone once the
 * object is instantiated.  When a user wants to edit an object, he gets a handle
 * to it from the DBStore, then calls createShadow to get an editable version
 * of the object.  If the user is satisfied with the changes made to the object,
 * the user will cause the editSet to commit, which will get a write lock on
 * the database, then replace the old version of this object with the modified
 * shadow object.  The shadow object field of the replaced object is cleared,
 * and then someone else can pull the object out for editing.</p>
 * 
 * <p>Changes made to the shadow object do not affect the original object until
 * the transaction is committed;  other processes can examine the current
 * contents of the object until commit time.</p>
 *
 * <p>This is kind of like modern dating.</p>
 *
 */

public class DBObject {

  DBObjectBase objectBase;
  int id;			// 32 bit id - the object's invariant id
  short fieldcount;
  Hashtable fields;
  DBEditObject shadowObject;	// if this object is being edited, this points
				// to the shadow

  boolean markedAsDeleted;	// for when the object is deleted in a transaction.
				// i.e., can't check this out even though there
				// is no shadow checked out
  DBEditSet editset;

  /* -- */

  DBObject(DBObjectBase objectBase)
  {
    this.objectBase = objectBase;
    id = 0;
    fieldcount = 0;
    fields = null;
    shadowObject = null;
    editset = null;
    markedAsDeleted = false;
  }

  DBObject(DBObjectBase objectBase, int id)
  {
    this(objectBase);
    this.id = id;
  }

  DBObject(DBObjectBase objectBase, DataInputStream in) throws IOException
  {
    this.objectBase = objectBase;
    shadowObject = null;
    editset = null;
    markedAsDeleted = false;
    receive(in);
  }
  
  /**
   *
   * The emit() method is part of the process of dumping the DBStore
   * to disk.  emit() dumps an object in its entirety to the
   * given out stream.
   *
   */

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
	key = (short) ((Integer) enum.nextElement()).intValue();
	out.writeShort((short) key);
	((DBField) fields.get(new Integer(key))).emit(out);
      }
  }

  /**
   *
   * The emitJournalEntry() method is used to write out a differential
   * DBStore entry, to be written to the DBStore Journal.  A Journal
   * entry only contains the fields that changed in the transaction.
   *
   */

  void emitJournalEntry(DataOutputStream out) throws IOException
  {
    Enumeration enum, enum2;
    Object key;
    DBField shadowField, origField;

    /* -- */

    if (shadowObject == null)
      {
	return; // this object is not edited
      }

    enum = shadowObject.fields.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	if (!fields.contains(key))
	  {
	    out.writeShort((short) ((Integer)key).intValue());
	    ((DBField) fields.get(key)).emit(out);
	  }
	else
	  {
	    origField = (DBField) fields.get(key);
	    shadowField = (DBField) shadowObject.fields.get(key);

	    if (!origField.equals(shadowField))
	      {
		out.writeShort((short) ((Integer)key).intValue());
		((DBField) fields.get(key)).emit(out);
	      }
	  }
      }
  }

  /**
   *
   * The receive() method is part of the process of loading the DBStore
   * from disk.  receive() reads an object from the given in stream and
   * instantiates it into the DBStore.
   *
   */

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

    fields =  new Hashtable(fieldcount);

    for (int i = 0; i < fieldcount; i++)
      {
	// read our field code, look it up in our
	// DBObjectBase

	fieldcode = in.readShort();
	fieldINT = new Integer(fieldcode);

	definition = (DBObjectBaseField) objectBase.fieldHash.get(fieldINT);

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

		for (int j = 0; j < tmp2.size(); i++)
		  {
		    if (definition.namespace.uniqueHash.containsKey(tmp2.key(j)))
		      {
			throw new RuntimeException("Duplicate unique value detected: " + tmp2.key(j));
		      } 

		    definition.namespace.uniqueHash.put(tmp2.key(j), new DBNameSpaceHandle(null, true, tmp2));
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
	
	// let the field know who daddy is
	tmp.owner = this;
	    
	// now add the field to our fields hash
	fields.put(fieldINT, tmp);
      }
  }

  /**
   *
   * Check this object out from the datastore for editing.  This
   * method is intended to be called by the editDBObject method
   * in DBSession.. createShadow should not be called on an
   * arbitrary viewed object in other contexts.. probably should
   * do something to guarantee this?
   *
   * If this object is being edited, we say that it has a shadow
   * object;  a session gets a copy of this object.. the copy
   * is actually a DBEditObject, which has the intelligence to
   * allow the client to modify the (copies of the) data fields.
   *
   * note: this is only used for editing pre-existing objects..
   * the code for creating new objects is in DBSession.. 
   * this method might be better incorporated into DBSession
   * as well.
   *
   */

  synchronized DBEditObject createShadow(DBEditSet editset)
  {
    if ((shadowObject != null) || markedAsDeleted)
      {
	// this object has already been checked out
	// for editing or has been marked as deleted

	return null;
      }

    shadowObject = new DBEditObject(this, editset);

    editset.addChangedObject(shadowObject);
    this.editset = editset;
    
    return shadowObject;
  }

  synchronized boolean clearShadow(DBEditSet editset)
  {
    if (markedAsDeleted || editset != this.editset)
      {
	// couldn't clear the shadow.. either this
	// object was marked as deleted or this editset
	// wasn't the one to create the shadow

	return false;
      }

    this.editset = null;
    shadowObject = null;

    return true;
  }

  /**
   *
   * Mark this object as deleted by the given editset.
   *
   * An object that is marked for deletion cannot be
   * checked out for editing or marked for deletion
   * by another editset.  When the editset that has marked
   * this object has committed, this object will be unlinked
   * from the objectBase.
   *
   */

  synchronized boolean markAsDeleted(DBEditSet editset)
  {
    if ((shadowObject != null) || markedAsDeleted)
      {
	// this object has already been checked out
	// for editing or has been marked as deleted

	return false;
      }

    markedAsDeleted = true;
    this.editset = editset;

    return true;
  }

  /**
   *
   * Clear out a deletion mark.  Used for editset abort.
   *
   */

  synchronized boolean clearDeletionMark(DBEditSet editset)
  {
    if (!markedAsDeleted || editset != this.editset)
      {
	// couldn't clear the deletion mark.. either this
	// object wasn't marked as deleted or this editset
	// wasn't the one to mark us.

	return false;
      }

    markedAsDeleted = false;
    this.editset = null;

    return true;
  }
  
  /**
   *
   * Get read-only access to a field from this object
   *
   */

  public DBField viewField(short id)
  {
    return (DBField) fields.get(new Integer(id));
  }
}
