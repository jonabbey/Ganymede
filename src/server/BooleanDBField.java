/*
   GASH 2

   BooleanDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  BooleanDBField

------------------------------------------------------------------------------*/

public class BooleanDBField extends DBField implements boolean_field {
  
  /**
   *
   * Receive constructor.  Used to create a BooleanDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  BooleanDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    defined = true;
    value = null;
    values = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   *
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the DBObjectBase
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the DBStore.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  BooleanDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
  {
    this.owner = owner;
    this.definition = definition;
    
    defined = false;
    value = null;
    values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public BooleanDBField(DBObject owner, BooleanDBField field) throws RemoteException
  {
    this.owner = owner;
    definition = field.definition;
    
    if (isVector())
      {
	values = (Vector) field.values.clone();
	value = null;
      }
    else
      {
	value = field.value;
	values = null;
      }

    defined = true;
  }


  /**
   *
   * Scalar value constructor.
   *
   */

  public BooleanDBField(DBObject owner, boolean value, DBObjectBaseField definition) throws RemoteException
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.definition = definition;
    this.value = new Boolean(value);

    values = null;
    defined = true;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public BooleanDBField(DBObject owner, Vector values, DBObjectBaseField definition) throws RemoteException
  {
    if (!definition.isArray())
      {
	throw new IllegalArgumentException("vector constructor called on scalar field");
      }

    this.owner = owner;
    this.definition = definition;
    
    if (values == null)
      {
	this.values = new Vector();
	defined = false;
      }
    else
      {
	this.values = (Vector) values.clone();
	defined = true;
      }

    value = null;
    defined = true;
  }

  public Object clone()
  {
    try
      {
	return new BooleanDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't clone boolean field: " + ex);
      }
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	out.writeShort(values.size());
	for (int i = 0; i < values.size(); i++)
	  {
	    out.writeBoolean(value(i));
	  }
      }
    else
      {
	out.writeBoolean(value());
      }
  }

  void receive(DataInput in) throws IOException
  {
    int count;

    /* -- */

    if (isVector())
      {
	count = in.readShort();
	values = new Vector(count);
	for (int i = 0; i < count; i++)
	  {
	    values.addElement(new Boolean(in.readBoolean()));
	  }
      }
    else
      {
	value = new Boolean(in.readBoolean());
      }
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public boolean value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    return ((Boolean) value).booleanValue();
  }

  public boolean value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return ((Boolean) values.elementAt(index)).booleanValue();
  }


  // ****
  //
  // boolean_field methods
  //
  // ****
  
  /**
   *
   * Returns true if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.boolean_field
   */

  public boolean labeled()
  {
    return definition.isLabeled();
  }

  /**
   *
   * Returns the true label if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.boolean_field
   */

  public String trueLabel()
  {
    return definition.getTrueLabel();
  }

  /**
   *
   * Returns the false label if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.boolean_field
   */

  public String falseLabel()
  {
    return definition.getFalseLabel();
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return (o instanceof Boolean);
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

}

