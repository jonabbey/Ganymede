/*
   GASH 2

   DBEditObject.java

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
                                                                    DBEditOBject

A DBEditObject is a copy of a DBObject that has been exclusively checked out
from the main database so that a thread can edit the fields of the object.  The
DBEditObject class keeps track of the changes made to fields, keeping things
properly synchronized with unique field name spaces.

All DBEditObjects are obtained in the context of a DBEditSet.  When the 
DBEditSet is committed, the DBEditObject is made to replace the original object
from the DBStore.  If the EditSet is aborted, the DBEditObject is dropped.

------------------------------------------------------------------------------*/

public class DBEditObject extends DBObject {

  DBObject original;
  DBEditSet editset; 

  /* -- */

  DBEditObject(DBObject original, DBEditSet editset)
  {
    this.editset = editset;
    this.original = original;
    this.id = original.id;
    this.fieldcount = original.fieldcount;
    this.objectBase = original.objectBase;
    this.shadowObject = null;

    // this clones the hash, but not the individual fields.. since
    // non-array fields can't be modified once created this is
    // okay.. we'll clone the array fields' arrays when/if we need to
    
    fields = original.fields.clone();
  }

  // when we change a field in a shadow object we are just going to
  // replace the dang thing

  public boolean setField(short fieldcode, DBField field)
  {
    DBNameSpace namespace;
    short type;
    DBNameSpaceHandle handle;
    DBField oldField;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    if (!objectBase.fieldhash.containsKey(fieldINT))
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
	oldField = fields.get(fieldINT);

	if (namespace != null)
	  {
	    // make the old values available for the editset.. we should
	    // always be able to do this

	    if (!oldField.unmark(editSet, namespace))
	      {
		throw new RuntimeException("Argh, namespace/editset corruption");
	      }
	  }
      }

    // now need to mark new values

    if (namespace != null)
      {
	if (!field.mark(editSet, namespace))
	  {
	    return false;	// couldn't set this value.. namespace conflict
	  }
      }  

    // and replace the changed field in our hashtable..

    fields.put(new Integer(fieldcode), field);
    return true;
  }

  // addElement needs to work with the nameSpace stuff
  // when addElement is called for the first time on
  // a field, the original field should be cloned
  
  // we don't want to cause the original vector field
  // to be modified
  
  public boolean addElement(short fieldcode, Object value)
  {
    DBField field;
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

    field = fields.get(fieldINT);

    if (!field instanceof DBArrayField)
      {
	throw new RuntimeException("addElement called on a scalar field code");
      }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our add

    if (original.fields.containsKey(fieldINT) && (original.fields.get(fieldINT) == field))
      {
	field = field.clone();
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

  public boolean removeElement(short fieldcode, Object value)
  {
    DBField field;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).namespace;

    if (!fields.containsKey(fieldINT))
      {
	throw new RuntimeException("removeElement called on a non-existant or null field");
      }

    field = fields.get(fieldINT);

    if (!field instanceof DBArrayField)
      {
	throw new RuntimeException("deleteElement called on a scalar field code");
      }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our remove

    if (original.fields.containsKey(fieldINT) && (original.fields.get(fieldINT) == field))
      {
	field = field.clone();
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

  public boolean removeElementAt(short fieldcode, int index)
  {
    DBField field;
    Integer fieldINT;

    /* -- */

    fieldINT = new Integer(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(fieldINT)).namespace;

    if (!fields.containsKey(fieldINT))
      {
	throw new RuntimeException("removeElement called on a non-existant or null field");
      }

    field = fields.get(fieldINT);

    if (!field instanceof DBArrayField)
      {
	throw new RuntimeException("deleteElement called on a scalar field code");
      }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our remove

    if (original.fields.containsKey(fieldINT) && (original.fields.get(fieldINT) == field))
      {
	field = field.clone();
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

  public void release()
  {
    
  }
}
