/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.16 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/
public abstract class DBField extends UnicastRemoteObject implements db_field, Cloneable {

  Object value;
  Vector values;  
  DBObject owner;
  DBObjectBaseField definition;
  boolean defined;

  /* -- */

  public DBField() throws RemoteException
  {
    super();
  }
  
  /**
   *
   * Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   *
   */

  public Object key()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return value;
  }

  /**
   *
   * Object value of a vector DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   *
   */

  public Object key(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    return values.elementAt(index);
  }

  /**
   *
   * Returns number of elements in vector if this is a vector field.  If
   * this is not a vector field, will return 1. (Should throw exception?)
   *
   */

  public int size()		// returns number of elements in array
  {
    if (!isVector())
      {
	return 1;
      }
    else
      {
	return values.size();
      }
  }

  /**
   *
   * Returns the maximum length of an array in this field type
   *
   *
   */

  public int getMaxArraySize()
  {
    if (!isVector())
      {
	return 1;		// should throw exception?
      }
    else
      {
	return definition.getMaxArraySize();
      }     
  }      

  abstract void emit(DataOutput out) throws IOException;
  abstract void receive(DataInput in) throws IOException;

  /**
   *
   * Returns true if obj is a field with the same value(s) as
   * this one.
   *
   */

  public boolean equals(Object obj)
  {
    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    DBField f = (DBField) obj;

    if (!isVector())
      {
	return f.key().equals(this.key());
      }
    else
      {
	if (f.size() != this.size())
	  {
	    return false;
	  }

	for (int i = 0; i < size(); i++)
	  {
	    if (!f.key(i).equals(this.key(i)))
	      {
		return false;
	      }
	  }

	return true;
      }
  }

  // ****
  //
  // db_field methods
  // 
  // ****

  /**
   *
   * Returns the schema name for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public String getName()
  {
    return definition.getName();
  }

  /**
   *
   * Returns the field # for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public short getID()
  {
    return definition.getID();
  }

  /**
   *
   * Returns the object this field is part of.
   * 
   */

  public DBObject getOwner()
  {
    return owner;
  }

  /**
   *
   * Returns the description of this field from the
   * schema.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public String getComment()
  {
    return definition.getComment();
  }

  /**
   *
   * Returns the description of this field's type from
   * the schema.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public String getTypeDesc()
  {
    return definition.getTypeDesc();
  }

  /**
   *
   * Returns the type code for this field from the
   * schema.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public short getType()
  {
    return definition.getType();
  }

  /**
   *
   * Returns the display order for this field from the schema.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public short getDisplayOrder()
  {
    return definition.getDisplayOrder();
  }

  /**
   *
   * Returns a String representing the value of this field.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  abstract public String getValueString();

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean isDefined()
  {
    return defined;
  }

  /**
   *
   * Returns true if this field is a vector, false
   * otherwise.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public boolean isVector()
  {
    return definition.isArray();
  }

  /**
   *
   * Returns true if this field is editable, false
   * otherwise.
   *
   * Note that DBField are only editable if they are
   * contained in a subclass of DBEditObject.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public boolean isEditable()
  {
    DBEditObject eObj;

    /* -- */

    if (!(owner instanceof DBEditObject))
      {
	return false;
      }

    if (!verifyWritePermission())
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    // if our owner has already started the commit
    // process, we can't allow any changes

    if (eObj.isCommitting())
      {
	return false;
      }

    return true;
  }

  /**
   *
   * Returns true if this field should be displayed in the
   * current client context.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public boolean isVisible()
  {
    return true;		// for now.  NEED TO WORK ON THIS MORE
  }

  /**
   *
   * Returns true if this field is edit in place.
   *
   */

  public boolean isEditInPlace()
  {
    return definition.isEditInPlace();
  }

  /**
   *
   * Returns the object type id for the object containing this field.
   *
   */

  public int getObjTypeID()
  {
    return definition.base().getTypeID();
  }

  /**
   *
   * Returns the DBNameSpace that this field is associated with, or
   * null if no NameSpace field is associated with this field.
   *
   */

  public DBNameSpace getNameSpace()
  {
    return definition.getNameSpace();
  }

  /**
   *
   * Returns the DBObjectBaseField for this field.
   *
   */

  public DBObjectBaseField getFieldDef()
  {
    return definition;
  }

  /**
   *
   * Returns the value of this field, if a scalar.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public Object getValue()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    if (owner.objectBase.objectHook.virtualizeField(getID()))
      {
	return owner.objectBase.objectHook.getVirtualValue(this);
      }

    return value;
  }

  /**
   *
   * Returns the value of this field, if a scalar.  This method
   * is intended to be used by the virtualizing hook to use to
   * return the default value if the virtualizer wants to pass
   * through.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public Object getValue(boolean virtualize)
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    if (virtualize && owner.objectBase.objectHook.virtualizeField(getID()))
      {
	return owner.objectBase.objectHook.getVirtualValue(this);
      }

    return value;
  }

  /**
   *
   * Sets the value of this field, if a scalar.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean setValue(Object value)
  {
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar method called on a vector field");
      }

    if (!verifyNewValue(value))
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(this.value);
	if (!mark(value))
	  {
	    mark(this.value); // we aren't clearing the old value after all

	    setLastError("value " + value + " already taken in namespace");
	    return false;
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetValue(this, value))
      {
	this.value = value;
	defined = true;
	return true;
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    unmark(value);
	    mark(this.value);
	  }

	return false;
      }
  }


  /**
   *
   * Returns the value of an element of this field,
   * if a vector.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public Object getElement(int index)
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if (index < 0)
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    return values.elementAt(index);
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */
  
  public boolean setElement(int index, Object value)
  {
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    if (!verifyNewValue(value))
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index));
	if (!mark(value))
	  {
	    mark(values.elementAt(index)); // we aren't clearing the old value after all

	    setLastError("value " + value + " already taken in namespace");
	    return false;
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetElement(this, index, value))
      {
	values.setElementAt(value, index);

	defined = true;
	
	return true;
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    unmark(value);
	    mark(values.elementAt(index));
	  }

	return false;
      }
  }

  /**
   *
   * Adds an element to the end of this field, if a vector.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean addElement(Object value)
  {
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    // verifyNewValue should setLastError for us.

    if (!verifyNewValue(value))
      {
	return false;
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");
	return false;
      }

    eObj = (DBEditObject) owner;

    ns = getNameSpace();

    if (ns != null)
      {
	if (!mark(value))
	  {
	    setLastError("value " + value + " already taken in namespace");
	    return false;
	  }
      }

    if (eObj.finalizeAddElement(this, value)) 
      {
	values.addElement(value);
	defined = true;
	return true;
      } 
    else
      {
	if (ns != null)
	  {
	    unmark(value);
	  }
	return false;
      }
  }

  /**
   *
   * Deletes an element of this field, if a vector.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean deleteElement(int index)
  {
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if ((index < 0) || (index >= values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    eObj = (DBEditObject) owner;

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index));
      }

    if (eObj.finalizeDeleteElement(this, index))
      {
	values.removeElementAt(index);

	defined = (values.size() > 0);

	return true;
      }
    else
      {
	if (ns != null)
	  {
	    mark(values.elementAt(index));
	  }
	return false;
      }
  }

  /**
   *
   * Package-domain method to set the owner of this field.
   *
   * Used by the DBObject copy constructor.
   *
   */

  synchronized void setOwner(DBObject owner)
  {
    this.owner = owner;
  }

  // ****
  //
  // Server-side namespace management functions
  //
  // ****

  /** unmark() is used to make any and all namespace values in this
   * field as available for use by other objects in the same editset.
   * When the editset is committed, any unmarked values will be
   * flushed from the namespace.
   * 
   */

  boolean unmark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = definition.getNameSpace();
    editset = owner.editset;

    if (namespace == null)
      {
	return false;
      }

    if (!isVector())
      {
	return namespace.unmark(editset, this.key());
      }
    else
      {
	synchronized (namespace)
	  {
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.testunmark(editset, key(i)))
		  {
		    return false;
		  }
	      }
	
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.unmark(editset, key(i)))
		  {
		    throw new RuntimeException("error: testunmark / unmark inconsistency");
		  }
	      }

	    return true;
	  }
      }
  }

  /**
   *
   * Unmark a specific value associated with this field, rather
   * than unmark all values associated with this field.  Note
   * that this method does not check to see if the value is
   * currently associated with this field, it just goes ahead
   * and unmarks it.  This is to be used by the vector
   * modifiers (setElement, addElement, deleteElement, etc.)
   * to keep track of namespace modifications as we go along.
   *
   */

  boolean unmark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = definition.getNameSpace();
    editset = owner.editset;

    if (namespace == null)
      {
	return false;		// should we throw an exception?
      }

    if (value == null)
      {
	return true;		// no previous value
      }

    return namespace.unmark(editset, value);
  }

  /** 
   *
   * mark() is used to mark any and all values in this field as taken
   * in the namespace.  When the editset is committed, marked values
   * will be permanently reserved in the namespace.  If the editset is
   * instead aborted, the namespace values will be returned to their
   * pre-editset status.
   *  
   */

  boolean mark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = definition.getNameSpace();
    editset = owner.editset;

    if (!isVector())
      {
	return namespace.mark(editset, this.key(), this);
      }
    else
      {
	synchronized (namespace)
	  {
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.testmark(editset, key(i)))
		  {
		    return false;
		  }
	      }
	
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.mark(editset, key(i), this))
		  {
		    throw new RuntimeException("error: testmark / mark inconsistency");
		  }
	      }

	    return true;
	  }
      }
  }

  /**
   *
   * Mark a specific value associated with this field, rather than
   * mark all values associated with this field.  Note that this
   * method does not in any way associate this value with this field
   * (add it, set it, etc.), it just marks it.  This is to be used by
   * the vector modifiers (setElement, addElement, etc.)  to keep
   * track of namespace modifications as we go along.
   * 
   */

  boolean mark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = definition.getNameSpace();
    editset = owner.editset;

    if (namespace == null)
      {
	return false;		// should we throw an exception?
      }

    if (value == null)
      {
	throw new NullPointerException("null value in mark()");
      }

    if (editset == null)
      {
	throw new NullPointerException("null editset in mark()");
      }

    return namespace.mark(editset, value, this);
  }

  // ****
  //
  // Server-side convenience methods
  //
  // ****

  void setLastError(String val)
  {
    owner.editset.session.setLastError(val);
  }

  // ****
  //
  // Methods for subclasses to override to implement the
  // behavior for this field.
  //
  // ****

  /**
   *
   * Overridable method to determine whether an
   * Object submitted to this field is of an appropriate
   * type.
   *
   */

  abstract public boolean verifyTypeMatch(Object o);

  /**
   *
   * Overridable method to verify that an object
   * submitted to this field has an appropriate
   * value.
   *
   */

  abstract public boolean verifyNewValue(Object o);

  /**
   *
   * Overridable method to verify that the current
   * DBSession / DBEditSet has permission to read
   * values from this field.
   *
   */

   public boolean verifyReadPermission()
   {
     return true;
   }

  /**
   *
   * Overridable method to verify that the current
   * DBSession / DBEditSet has permission to write
   * values into this field.
   *
   */

  public boolean verifyWritePermission()
  {
    return true;
  }
}
