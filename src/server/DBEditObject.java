/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBEditOBject

------------------------------------------------------------------------------*/

/**
 *
 * A DBEditObject is a copy of a DBObject that has been exclusively checked out
 * from the main database so that a DBSession can edit the fields of the object.  The
 * DBEditObject class keeps track of the changes made to fields, keeping things
 * properly synchronized with unique field name spaces.<br><br>
 *
 * All DBEditObjects are obtained in the context of a DBEditSet.  When the 
 * DBEditSet is committed, the DBEditObject is made to replace the original object
 * from the DBStore.  If the EditSet is aborted, the DBEditObject is dropped.
 *
 */

public class DBEditObject extends DBObject {

  DBObject original;
  DBEditSet editset; 

  /* -- */

  DBEditObject(DBObject original, DBEditSet editset)
  {
    super(original.objectBase);
    this.editset = editset;
    this.original = original;
    this.id = original.id;
    this.fieldcount = original.fieldcount;
    this.objectBase = original.objectBase;
    this.shadowObject = null;

    // this clones the hash, but not the individual fields.. since
    // non-array fields can't be modified once created this is
    // okay.. we'll clone the array fields' arrays when/if we need to
    
    fields = (Hashtable) original.fields.clone();
  }

  /**
   * DBField's are basically the smallest unit that are managed in
   * the DBStore's memory structures.  DBField's are generally not changed
   * once created, because a session / thread may have a reference to 
   * a field when we are changing this 'shadow' object.  So we simply
   * replace the existing field in this object with a new one we
   * created.
   *
   * @param fieldcode The idcode for the field to be replaced.
   * @param field The new field value to fill the fieldcode slot.
   *
   * @see csd.DBStore.DBField
   *
   */
  public boolean setField(short fieldcode, DBField field)
  {
    DBNameSpace namespace;
    short type;
    DBNameSpaceHandle handle;
    DBField oldField;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    if (!objectBase.fieldHash.containsKey(fieldINT))
      {
	throw new RuntimeException("bad field code");
      }

    type = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).field_type;
    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).namespace;

    if (!(((field instanceof DBArrayField) &&
	   (type == DBStore.BOOLEANARRAY && (field instanceof BooleanArrayDBField)) ||
	   (type == DBStore.NUMERICARRAY && (field instanceof NumericArrayDBField)) ||
	   (type == DBStore.DATEARRAY && (field instanceof DateArrayDBField)) ||
	   (type == DBStore.STRINGARRAY && (field instanceof StringArrayDBField)) ||
	   (type == DBStore.INVIDARRAY  && (field instanceof InvidArrayDBField))) ||
	  (type == DBStore.BOOLEAN && (field instanceof BooleanDBField)) ||
	  (type == DBStore.NUMERIC && (field instanceof NumericDBField)) ||
	  (type == DBStore.DATE && (field instanceof DateDBField)) ||
	  (type == DBStore.STRING && (field instanceof StringDBField)) ||
	  (type == DBStore.INVID && (field instanceof InvidDBField))))
      {
	throw new RuntimeException("Don't recognize field type in datastore");
      }

    // check to see if we already have an instance of this field

    if (fields.containsKey(fieldINT))
      {
	oldField = (DBField) fields.get(fieldINT);

	if (namespace != null)
	  {
	    // make the old values available for the editset.. we should
	    // always be able to do this

	    if (!oldField.unmark(editset, namespace))
	      {
		throw new RuntimeException("Argh, namespace/editset corruption");
	      }
         }
      }

    // now need to mark new values

    if (namespace != null)
      {
	if (!field.mark(editset, namespace))
	  {
	    return false;	// couldn't set this value.. namespace conflict
	  }
      }  

    // and replace the changed field in our hashtable..

    fields.put(new Integer(fieldcode), field);
    return true;
  }

  /**
   *
   * addElement is used to add a value to an array
   * DBField.  addElement will implicitly clone the
   * appropriate DBField before modifying it, to maintain
   * the DBEditObject semantics.<br><br>
   *
   * If the fieldcode does not correspond to a valid
   * array field in this object's DBObjectBase, an exception
   * will be thrown.  This will be a RuntimeException if
   * the field does not exist, a ClassCastException if the
   * the field does exist but is not an array field.
   *
   * @param fieldcode The idcode for the field to be modified in the editobject.
   * @param value The element to add to this array.
   */

  public boolean addElement(short fieldcode, Object value)
  {
    DBNameSpace namespace;
    DBArrayField field;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).namespace;

    if (!fields.containsKey(fieldINT))
      {
	// do we want to create a new field in this slot for the caller
	// if no such field currently exists in the object?

	throw new RuntimeException("addElement called on a non-existant or null field");
      }

    // we'll throw a cast exception here if we weren't called on a vector field

    field = (DBArrayField) fields.get(fieldINT);

//     if (!(field instanceof DBArrayField))
//       {
// 	throw new RuntimeException("addElement called on a scalar field code");
//       }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our add

    if (original.fields.containsKey(fieldINT) && (original.fields.get(fieldINT) == field))
      {
	field = field.duplicate();
	fields.put(fieldINT, field);
      }

    // add the new element, checking against the proper namespace if
    // required

    if (namespace != null)
      {
	if (!namespace.mark(editset, value, field))
	  {
	    return false;
	  }
      }
    
    field.addElement(value);

    return true;
  }

  /**
   *
   * removeElement is used to remove a value from an array
   * DBField.  removeElement will implicitly clone the
   * appropriate DBField before modifying it, to maintain
   * the DBEditObject semantics.<br><br>
   *
   * If the fieldcode does not correspond to a valid
   * array field in this object's DBObjectBase, an exception
   * will be thrown.  This will be a RuntimeException if
   * the field does not exist, a ClassCastException if the
   * the field does exist but is not an array field.
   *
   * @param fieldcode The idcode for the field to be modified in the editobject.
   * @param value The element to remove from this array.
   */

  public boolean removeElement(short fieldcode, Object value)
  {
    DBArrayField field;
    DBNameSpace namespace;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).namespace;

    if (!fields.containsKey(fieldINT))
      {
	throw new RuntimeException("removeElement called on a non-existant or null field");
      }

    // we'll throw a cast exception here if we weren't called on an vector field

     field = (DBArrayField) fields.get(fieldINT);

//     if (!field instanceof DBArrayField)
//       {
// 	throw new RuntimeException("deleteElement called on a scalar field code");
//       }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our remove

    if (original.fields.containsKey(fieldINT) && (original.fields.get(fieldINT) == field))
      {
	field = field.duplicate();
	fields.put(fieldINT, field);
      }

    // okay, now delete the element, marking the value as free for
    // reuse in the namespace if appropriate

    if (namespace != null)
      {
	if (!namespace.unmark(editset, value))
	  {
	    return false;
	  }
      }
    
    field.removeElement(value);

    return true;
  }

  /**
   *
   * removeElementAt is used to remove a specific element from an array
   * DBField.  removeElementAt will implicitly clone the
   * appropriate DBField before modifying it, to maintain
   * the DBEditObject semantics.<br><br>
   *
   * If the fieldcode does not correspond to a valid
   * array field in this object's DBObjectBase, an exception
   * will be thrown.  This will be a RuntimeException if
   * the field does not exist, a ClassCastException if the
   * the field does exist but is not an array field.
   *
   * @param fieldcode The idcode for the field to be modified in the editobject.
   * @param value The index of the vector element to be removed.
   */

  public boolean removeElementAt(short fieldcode, int index)
  {
    DBArrayField field;
    DBNameSpace namespace;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).namespace;

    if (!fields.containsKey(fieldINT))
      {
	throw new RuntimeException("removeElement called on a non-existant or null field");
      }

    field = (DBArrayField) fields.get(fieldINT);

//     if (!field instanceof DBArrayField)
//       {
// 	throw new RuntimeException("deleteElement called on a scalar field code");
//       }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our remove

    if (original.fields.containsKey(fieldINT) && (original.fields.get(fieldINT) == field))
      {
	field = field.duplicate();
	fields.put(fieldINT, field);
      }

    // okay, now delete the element, marking the value as free for
    // reuse in the namespace if appropriate

    if (namespace != null)
      {
	if (!namespace.unmark(editset, field.key(index)))
	  {
	    return false;
	  }
      }
    
    field.removeElementAt(index);

    return true;
  }
}
