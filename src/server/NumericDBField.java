/*
   GASH 2

   NumericDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  NumericDBField

------------------------------------------------------------------------------*/

public class NumericDBField extends DBField implements num_field {

  /**
   *
   * Receive constructor.  Used to create a NumericDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  NumericDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException, RemoteException
  {
    value = values = null;
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

  NumericDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
  {
    this.owner = owner;
    this.definition = definition;
    
    defined = false;
    value = values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public NumericDBField(DBObject owner, NumericDBField field) throws RemoteException
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

  public NumericDBField(DBObject owner, int value, DBObjectBaseField definition) throws RemoteException
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.definition = definition;
    this.value = new Integer(value);

    defined = true;
    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public NumericDBField(DBObject owner, Vector values, DBObjectBaseField definition) throws RemoteException
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
  }

  public Object clone()
  {
    try
      {
	return new NumericDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't clone NumericDBField: " + ex);
      }
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	out.writeShort(values.size());
	for (int i = 0; i < values.size(); i++)
	  {
	    out.writeInt(((Integer) values.elementAt(i)).intValue());
	  }
      }
    else
      {
	out.writeInt(((Integer) value).intValue());
      }
  }

  void receive(DataInput in) throws IOException
  {
    int count;

    /* - */

    if (isVector())
      {
	count = in.readShort();
	values = new Vector(count);
	for (int i = 0; i < count; i++)
	  {
	    values.addElement(new Integer(in.readInt()));
	  }
      }
    else
      {
	value = new Integer(in.readInt());
      }

    defined = true;
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public int value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return ((Integer) value).intValue();
  }

  public int value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    return ((Integer) values.elementAt(index)).intValue();
  }

  // ****
  //
  // num_field specific methods
  //
  // ****

  /**
   *
   * Returns true if this field has max/min
   * limitations.
   *
   * @see arlut.csd.ganymede.num_field
   *
   */

  public boolean limited()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.isIntLimited(this);
  }

  /**
   *
   * Returns the minimum acceptable value for this field if this field
   * has max/min limitations.  
   *
   * @see arlut.csd.ganymede.num_field
   * 
   */

  public int getMinValue()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.minInt(this);
  }

  /**
   *
   * Returns the maximum acceptable value for this field if this field
   * has max/min limitations.
   *
   * @see arlut.csd.ganymede.num_field
   * 
   */

  public int getMaxValue()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.maxInt(this);
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return (o instanceof Integer);
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Integer I;

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

    I = (Integer) o;

    if (limited())
      {
	if (getMinValue() > I.intValue())
	  {
	    setLastError("value out of range (underflow)");
	    return false;
	  }

	if (getMaxValue() < I.intValue())
	  {
	    setLastError("value out of range (overflow)");
	    return false;
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

}
