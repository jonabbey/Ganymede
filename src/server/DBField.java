/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.36 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/
public abstract class DBField extends UnicastRemoteObject implements db_field, Cloneable {

  Object 
    value,			// the object's current value, for scalars
    newValue = null;		// the object's new value, for use by custom verification classes

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
   * Returns a handy field description packet for this field,
   * containing the static field elements for this field..
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final FieldTemplate getFieldTemplate()
  {
    return definition.template;
  }

  /**
   *
   * Returns a handy field description packet for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final FieldInfo getFieldInfo()
  {
    return new FieldInfo(this);
  }

  /**
   *
   * Returns the schema name for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final String getName()
  {
    return definition.getName();
  }

  /**
   *
   * Returns the field # for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final short getID()
  {
    return definition.getID();
  }

  /**
   *
   * Returns the object this field is part of.
   * 
   */

  public final DBObject getOwner()
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

  public final String getComment()
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

  public final String getTypeDesc()
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

  public final short getType()
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

  public final short getDisplayOrder()
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
   * Returns a String representing a reversible encoding of the
   * value of this field.  Each field type will have its own encoding,
   * suitable for embedding in a DumpResult.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  abstract public String getEncodingString();

  /**
   *
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   * 
   */

  abstract public String getDiffString(DBField orig);

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public final boolean isDefined()
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

  public final boolean isVector()
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

  public final boolean isEditable()
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
   * Returns true if this field is a built-in field, common
   * to all non-embedded objects.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final boolean isBuiltIn()
  {
    return definition.isBuiltIn();
  }

  /**
   *
   * Returns true if this field should be displayed in the
   * current client context.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final boolean isVisible()
  {
    return definition.base.objectHook.canSeeField(null, this);
  }

  /**
   *
   * Returns true if this field is edit in place.
   *
   */

  public final boolean isEditInPlace()
  {
    return definition.isEditInPlace();
  }

  /**
   *
   * Returns the object type id for the object containing this field.
   *
   */

  public final int getObjTypeID()
  {
    return definition.base().getTypeID();
  }

  /**
   *
   * Returns the DBNameSpace that this field is associated with, or
   * null if no NameSpace field is associated with this field.
   *
   */

  public final DBNameSpace getNameSpace()
  {
    return definition.getNameSpace();
  }

  /**
   *
   * Returns the DBObjectBaseField for this field.
   *
   */

  public final DBObjectBaseField getFieldDef()
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

    // if this field is virtualized, let our customizer provide the
    // value

    if (owner.objectBase.objectHook.virtualizeField(getID()))
      {
	return owner.objectBase.objectHook.getVirtualValue(this);
      }

    return value;
  }

  /**
   *
   * This method is used to allow server-side custom code to get access to
   * a proposed new value before it is finalized, used when making a change
   * to this field causes other fields to be changed which need to insure
   * that this field has an appropriate value first.
   *
   * This method is not intended to be accessible to the client.
   *
   * This method does not support virtualized fields.
   *
   * This method will only have a useful value during the
   * course of the containing objects' finalizeSetValue() call.  It
   * is intended that this field will set the new value in setValue(),
   * call owner.finalizeSetValue(), then clear the newValue.
   *
   * What, this code a hack?
   *
   */

  public Object getNewValue()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    return newValue;
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

  public ReturnVal setValue(Object value)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
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

    if (this.value == value)
      {
	return retVal;		// no change
      }

    if (!verifyNewValue(value))
      {
	// we need a better way of getting the information up from verifyNewValue, since
	// conceivably another thread could intercede before we read the error?

	return Ganymede.createErrorDialog("Server: Error in DBField.setValue()",
					  getLastError());
      }

    eObj = (DBEditObject) owner;

    // Wizard check

    newRetVal = eObj.wizardHook(this, DBEditObject.SETVAL, value, null);

    if ((newRetVal != null) && !newRetVal.didSucceed())
      {
	// we're not done.. return the next wizard dialog, or the failure code

	return newRetVal;
      }
    else
      {
	retVal = newRetVal;
      }

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(this.value);

	// if we're not being told to clear this field, try to mark the
	// new value

	if (value != null)
	  {
	    if (!mark(value))
	      {
		if (this.value != null)
		  {
		    mark(this.value); // we aren't clearing the old value after all
		  }
		
		setLastError("value " + value + " already taken in namespace");

		return Ganymede.createErrorDialog("Server: Error in DBField.setValue()",
						  "value " + value + " already taken in namespace");
	      }
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    this.newValue = value;

    if (eObj.finalizeSetValue(this, value))
      {
	if (value != null)
	  {
	    this.value = value;
	    defined = true;
	  }
	else
	  {
	    this.value = null;
	    defined = false;	// the key
	  }

	this.newValue = null;

	return retVal;		// success
      }
    else
      {
	this.newValue = null;

	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    unmark(value);
	    mark(this.value);
	  }

	return Ganymede.createErrorDialog("Server: Error in DBField.setValue()",
					  "Custom code rejected set operation for value " + value);
      }
  }

  /** 
   *
   * Returns a Vector of the values of the elements in this field,
   * if a vector.
   *
   * This is only valid for vectors.  If the field is a scalar, use
   * getValue().
   *
   * @see arlut.csd.ganymede.db_field
   *
   */
  public Vector getValues()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    return values; // this is ok, since this is being serialized. the client's not
                   // gaining the ability to add or remove items from this field
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
  
  public ReturnVal setElement(int index, Object value)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
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
	return Ganymede.createErrorDialog("Server: Error in DBField.setElement()",
					  getLastError());
      }

    eObj = (DBEditObject) owner;

    // Wizard check

    newRetVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), value);

    if ((newRetVal != null) && !newRetVal.didSucceed())
      {
	return newRetVal;
      }
    else
      {
	retVal = newRetVal;
      }

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

	    return Ganymede.createErrorDialog("Server: Error in DBField.setElement()",
					      "value " + value + " already taken in namespace");
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetElement(this, index, value))
      {
	values.setElementAt(value, index);

	defined = true;
	
	return retVal;
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

	return Ganymede.createErrorDialog("Server: Error in DBField.setElement()",
					  "Custom code rejected set operatin for value " + value);
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

  public ReturnVal addElement(Object value)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
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
	return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					  getLastError());
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");

	return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					  "Field " + getName() + " already at or beyond array size limit");
      }

    eObj = (DBEditObject) owner;

    // Wizard check

    newRetVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, value, null);

    if ((newRetVal != null) && !newRetVal.didSucceed())
      {
	return newRetVal;
      }
    else
      {
	retVal = newRetVal;
      }

    ns = getNameSpace();

    if (ns != null)
      {
	if (!mark(value))
	  {
	    setLastError("value " + value + " already taken in namespace");

	    return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					      "value " + value + " already taken in namespace");
	  }
      }

    if (eObj.finalizeAddElement(this, value)) 
      {
	values.addElement(value);
	defined = true;
	return retVal;
      } 
    else
      {
	if (ns != null)
	  {
	    unmark(value);
	  }

	return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					  "Custom code rejected add operation for value " + 
					  value + "\n" + getLastError());
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

  public ReturnVal deleteElement(int index)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
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

    // Wizard check

    newRetVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, new Integer(index), null);

    if ((newRetVal != null) && !newRetVal.didSucceed())
      {
	return newRetVal;
      }
    else
      {
	retVal = newRetVal;
      }

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index));
      }

    if (eObj.finalizeDeleteElement(this, index))
      {
	values.removeElementAt(index);

	defined = (values.size() > 0);

	return retVal;
      }
    else
      {
	if (ns != null)
	  {
	    mark(values.elementAt(index));
	  }

	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElement()",
					  "Custom code rejected deletion operation for value " + 
					  value + "\n" + getLastError());
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

  public ReturnVal deleteElement(Object value)
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

    if (values.indexOf(value) == -1)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElement()",
					  "Could not delete value " + value + ", not present in field");
      }

    return deleteElement(values.indexOf(value));
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
	return false;
	//	throw new NullPointerException("null value in mark()");
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
    try
      {
	owner.editset.session.setLastError(val);
      }
    catch (NullPointerException ex)
      {
      }
  }

  String getLastError()
  {
    try
      {
	return owner.editset.session.lastError;
      }
    finally
      {
	return null;
      }
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
     GanymedeSession gSession;
     PermEntry perm1, perm2;

     /* -- */

     if (owner.editset != null)
       {
	 gSession = owner.editset.getSession().getGSession();
       }
     else if (owner.gSession != null)
       {
	 gSession = owner.gSession;
       }
     else
       {
	 return true; // we don't know who is looking at us, assume it's a server-local access
       }

     perm2 = gSession.getPerm(owner, getID());

     if (perm2 == null)
       {
	 perm1 = gSession.getPerm(owner);
	 
	 if (perm1 != null)
	   {
	     return perm1.isVisible();
	   }
	 else
	   {
	     return false;
	   }
       }
     else
       {
	 return perm2.isVisible();
       }
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
    GanymedeSession gSession;
    PermEntry perm1, perm2;

    /* -- */

    if (owner.editset != null)
      {
	gSession = owner.editset.getSession().getGSession();

	perm2 = gSession.getPerm(owner, getID());

	if (perm2 == null)
	  {
	    perm1 = gSession.getPerm(owner);
	
	    if (perm1 != null)
	      {
		return perm1.isEditable();
	      }
	    else
	      {
		return false;
	      }
	  }
	else
	  {
	    return perm2.isEditable();
	  }
      }
    else
      {
	return false;  // if we're not in a transaction, we certainly can't be edited.
      }
  }

  /** 
   *
   * Returns a Vector of the values of the elements in this field,
   * if a vector.
   *
   * This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.
   *
   */
  public Vector getValuesLocal()
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    return values; // this is ok, since this is being serialized. the client's not
                   // gaining the ability to add or remove items from this field
  }

  /** 
   *
   * Returns an Object carrying the value held in this field.
   *
   * This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.
   *
   */
  public Object getValueLocal()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return value;
  }

}
