/*
   GASH 2

   NumericDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.18 $ %D%
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

  NumericDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
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

  NumericDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    
    value = null;
    values = null;		// numeric fields cannot be arrays
  }

  /**
   *
   * Copy constructor.
   *
   */

  public NumericDBField(DBObject owner, NumericDBField field)
  {
    this.owner = owner;
    definition = field.definition;
    
    value = field.value;
    values = null;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public NumericDBField(DBObject owner, int value, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    this.value = new Integer(value);

    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public NumericDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone()
  {
    return new NumericDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeInt(((Integer) value).intValue());
  }

  void receive(DataInput in) throws IOException
  {
    value = new Integer(in.readInt());
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
    throw new IllegalArgumentException("vector accessor called on scalar field");
  }

  public synchronized String getValueString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (value == null)
      {
	return "null";
      }

    return Integer.toString(this.value());
  }

  /**
   *
   * For numbers, our default getValueString() encoding is adequate.
   *
   */

  public String getEncodingString()
  {
    return getValueString();
  }
  
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

  public String getDiffString(DBField orig)
  {
    NumericDBField origN;
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof NumericDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origN = (NumericDBField) orig;

    if (origN.value() != this.value())
      {
	result.append("\tOld: ");
	result.append(Integer.toString(origN.value()));
	result.append("\n\tNew: ");
	result.append(Integer.toString(this.value()));
	result.append("\n");
	
	return result.toString();
      }
    else
      {
	return null;
      }
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

    if (!isEditable(true))
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

    if (!isEditable(true))
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

    if (!isEditable(true))
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
    return ((o == null) || (o instanceof Integer));
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Integer I;

    /* -- */

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    if (o == null)
      {
	return eObj.verifyNewValue(this, o);
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
