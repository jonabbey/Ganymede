/*
   GASH 2

   DBEditObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBEditOBject

------------------------------------------------------------------------------*/

/**
 *
 * A DBEditObject is a copy of a DBObject that has been exclusively
 * checked out from the main database so that a DBSession can edit the
 * fields of the object.  The DBEditObject class keeps track of the
 * changes made to fields, keeping things properly synchronized with
 * unique field name spaces.<br><br>
 *
 * All DBEditObjects are obtained in the context of a DBEditSet.  When
 * the DBEditSet is committed, the DBEditObject is made to replace the
 * original object from the DBStore.  If the EditSet is aborted, the
 * DBEditObject is dropped.
 *
 */

public class DBEditObject extends DBObject {

  static boolean debug = true;

  public static void setDebug(boolean val)
  {
    debug = val;
  }

  DBObject original;
  DBEditSet editset; 

  /* -- */

  DBEditObject(DBObject original, DBEditSet editset)
  {
    Enumeration enum;
    Object key;
    DBObjectBaseField fieldDef;
    DBField field, tmp;

    /* -- */

    // do we also want to synchronize on the object base
    // below?

    synchronized (original)
      {
	super(original.objectBase);
	this.editset = editset;
	this.original = original;
	this.id = original.id;
	this.tmp_count = 0;
	this.objectBase = original.objectBase;
	this.shadowObject = null;

	fields = new Hashtable();

	// clone the fields from the original object
	// since we own these, the field-modifying
	// methods on the copied fields will allow editing
	// to go forward

	if (original.fields != null)
	  {
	    enum = original.fields.elements();

	    while (enum.hasMoreElements())
	      {
		field = (DBField) enum.nextElement();

		switch (field.getType())
		  {
		  case DBStore.BOOLEAN:
		    tmp = new BooleanDBField(this, field);
		    break;
		    
		  case DBStore.NUMERIC:
		    tmp = new NumericDBField(this, field);
		    break;

		  case DBStore.DATE:
		    tmp = new DateDBField(this, field);
		    break;

		  case DBStore.STRING:
		    tmp = new StringDBField(this, field);
		    break;
		    
		  case DBStore.INVID:
		    tmp = new InvidDBField(this, field);
		    break;
		  }

		fields.put(key, tmp);
	      }
	  }
      }
	
    // now create slots for any fields that are in this object type's
    // DBObjectBase, but which were not present in the original
    
    synchronized (definition)
      {
	enum = definition.fieldHash.keys();
	
	while (enum.hasMoreElements())
	  {
	    key = enum.nextElement();
	    
	    if (!fields.containsKey(key))
	      {
		fieldDef = (DBObjectBaseField) definition.fieldHash.get(key);
		
		switch (fieldDef.getType())
		  {
		  case DBStore.BOOLEAN:
		    tmp = new BooleanDBField(this, fieldDef);
		    break;
		    
		  case DBStore.NUMERIC:
		    tmp = new NumericDBField(this, fieldDef);
		    break;
		
		  case DBStore.DATE:
		    tmp = new DateDBField(this, fieldDef);
		    break;

		  case DBStore.STRING:
		    tmp = new StringDBField(this, fieldDef);
		    break;
		    
		  case DBStore.INVID:
		    tmp = new InvidDBField(this, fieldDef);
		    break;
		  }

		fields.put(key, tmp);
	      }
	  }
      }
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
   * @see arlut.csd.ganymede.DBField
   *
   */
  public boolean setField(short fieldcode, DBField field)
  {
    DBNameSpace namespace;
    DBObjectBaseField fieldDef;
    DBField oldField;
    Short key;

    /* -- */

    // check to see if we already have an instance of this field

    if (fields.containsKey(key))
      {
	oldField = (DBField) fields.get(key);

	if (namespace != null)
	  {
	    // make the old values available for the editset.. we should
	    // always be able to do this

	    if (!oldField.unmark(editset, namespace))
	      {
		throw new RuntimeException(editset.session.key +
					   ": namespace/editset corruption");
	      }
         }
      }

    // now need to mark new values

    if (namespace != null)
      {
	if (debug)
	  {
	    System.err.println(editset.session.key +
			       ": DBEditObject.setField(): namespace check on " + 
			       namespace.name + " for field " + fieldDef.getName());
	  }

	if (!field.mark(editset, namespace))
	  {
	    editset.session.setLastError("couldn't set field " + 
					 fieldDef.getName() +
					 " due to a namespace conflict");
	    return false;
	  }
	else if (debug)
	  {
	    System.err.println(editset.session.key +
			       ": DBEditObject.setField(): namespace check ok");
	  }
      }  

    // and replace the changed field in our hashtable..

    fields.put(new Short(fieldcode), field);
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
    Short key;
    DBObjectBaseField fieldDef;

    /* -- */

    key = new Short(fieldcode);

    fieldDef = (DBObjectBaseField) objectBase.fieldHash.get(key);

    if (fieldDef.isString())
      {
	namespace = fieldDef.getNameSpace();
      }
    else
      {
	namespace = null;
      }

    if (!(fieldDef.isArray() &&
	  ((fieldDef.isBoolean() && (value instanceof Boolean)) ||
	   (fieldDef.isNumeric() && (value instanceof Integer)) ||
	   (fieldDef.isDate() && (value instanceof Date)) ||
	   (fieldDef.isString() && (value instanceof String)) ||
	   (fieldDef.isInvid() && (value instanceof Invid)))))
      {
	// not an array field, or the field type doesn't match value

	editset.session.setLastError("wrong typed value " +
				     value + " for " + fieldDef.getName());
	
	return false;
      }

    // if we're a string field, check the value being added for appropriate value
    
    if (fieldDef.isString())
      {
	String s = (String) value;

	/* - */

	if (s.length() > fieldDef.getMaxLength())
	  {
	    editset.session.setLastError("couldn't add " + s + " to field " +
					 fieldDef.getName() +
					 ": string too long for field");
	    return false;
	  }

	if (s.length() < fieldDef.getMinLength())
	  {
	    editset.session.setLastError("couldn't add " + s + " to field " + 
					 fieldDef.getName() +
					 ": string too short for field");
	    return false;
	  }

	if (fieldDef.getOKChars() != null)
	  {
	    String ok = fieldDef.getOKChars();

	    for (int i = 0; i < s.length(); i++)
	      {
		if (ok.indexOf(s.charAt(i)) == -1)
		  {
		    editset.session.setLastError("string value" + s +
						 "contains a bad character " +
						 s.charAt(i));
		    return false;
		  }
	      }
	  }

	if (fieldDef.getBadChars() != null)
	  {
	    String bad = fieldDef.getBadChars();

	    for (int i = 0; i < s.length(); i++)
	      {
		if (bad.indexOf(s.charAt(i)) != -1)
		  {
		    editset.session.setLastError("string value" + s +
						 "contains a bad character " + 
						 s.charAt(i));
		    return false;
		  }
	      }
	  }
      }

    if (fieldDef.isInvid())
      {
	Invid invid = (Invid) value;

	/* - */

	if (fieldDef.isTargetRestricted() &&
	    invid.getType() != fieldDef.getAllowedTarget())
	  {
	    editset.session.setLastError("couldn't add " + invid + " to field "
					 + fieldDef.getName() +
					 ": invid type mismatch");
	    return false;
	  }
      }

    //JON: need to handle symmetry issues here

    if (!fields.containsKey(key))
      {
	// do we want to create a new field in this slot for the caller
	// if no such field currently exists in the object?

	throw new RuntimeException("addElement called on a non-existant or null field");
      }

    // we'll throw a cast exception here if we weren't called on a vector field

    field = (DBArrayField) fields.get(key);

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our add

    if (original.fields.containsKey(key) && (original.fields.get(key) == field))
      {
	field = field.duplicate();
	fields.put(key, field);
      }

    // add the new element, checking against the proper namespace if
    // required

    if (namespace != null)
      {
	if (!namespace.mark(editset, value, field))
	  {
	    editset.session.setLastError("couldn't add  " + value + 
					 " to field " + fieldDef.getName() +
					 " due to a namespace conflict");
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
    Short key;

    /* -- */

    key = new Short(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(key)).namespace;

    if (!fields.containsKey(key))
      {
	throw new RuntimeException("removeElement called on a non-existant or null field");
      }

    // we'll throw a cast exception here if we weren't called on an vector field

     field = (DBArrayField) fields.get(key);

//     if (!field instanceof DBArrayField)
//       {
// 	throw new RuntimeException("deleteElement called on a scalar field code");
//       }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our remove

    if (original.fields.containsKey(key) && (original.fields.get(key) == field))
      {
	field = field.duplicate();
	fields.put(key, field);
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
    Short key;

    /* -- */

    key = new Short(fieldcode);

    namespace = ((DBObjectBaseField) objectBase.fieldHash.get(key)).namespace;

    if (!fields.containsKey(key))
      {
	throw new RuntimeException("removeElement called on a non-existant or null field");
      }

    field = (DBArrayField) fields.get(key);

//     if (!field instanceof DBArrayField)
//       {
// 	throw new RuntimeException("deleteElement called on a scalar field code");
//       }

    // if we have not yet cloned the DBArrayField, do so.  this makes sure that
    // we don't change the original vector with our remove

    if (original.fields.containsKey(key) && (original.fields.get(key) == field))
      {
	field = field.duplicate();
	fields.put(key, field);
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
