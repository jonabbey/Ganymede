/*
   GASH 2

   StringDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   StringDBField

------------------------------------------------------------------------------*/

public class StringDBField extends DBField implements string_field {

  /**
   *
   * Receive constructor.  Used to create a StringDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  StringDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    defined = true;
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

  StringDBField(DBObject owner, DBObjectBaseField definition)
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

  public StringDBField(DBObject owner, StringDBField field)
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

  public StringDBField(DBObject owner, String value, DBObjectBaseField definition)
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.definition = definition;
    this.value = value;

    if (value == null)
      {
	defined = true;
      }
    else
      {
	defined = false;
      }

    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public StringDBField(DBObject owner, Vector values, DBObjectBaseField definition)
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

  protected Object clone()
  {
    return new StringDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	out.writeShort(values.size());
	for (int i = 0; i < values.size(); i++)
	  {
	    out.writeUTF((String) values.elementAt(i));
	  }
      }
    else
      {
	out.writeUTF(value);
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
	    values.addElement(in.readUTF());
	  }
      }
    else
      {
	value = in.readUTF();
      }
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  public String value()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return (String) value;
  }

  public String value(int index)
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return (String) values.elementAt(index);
  }

  // ****
  //
  // string_field methods 
  //
  // ****

  /**
   *
   * Returns the maximum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public int maxSize()
  {
    return definition.getMaxLength();
  }

  /**
   *
   * Returns the minimum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public int minSize()
  {
    return definition.getMinLength();
  }

  /**
   *
   * Returns true if the client should echo characters
   * entered into the string field.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */
  
  public boolean showEcho()
  {
    return true;
  }

  /**
   *
   * Returns true if this field has a list of recommended
   * options for choices from the choices() method.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public boolean canChoose()
  {
    if (owner instanceof DBEditObject)
      {
	return (((DBEditObject) owner).obtainChoiceList(this) != null);
      }
    else
      {
	return false;
      }
  }

  /**
   *
   * Returns true if the only valid values
   * for this string field are in the
   * vector returned by choices().
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public boolean mustChoose()
  {
    if (!canChoose())
      {
	return false;
      }

    if (owner instanceof DBEditObject)
      {
	return ((DBEditObject) owner).mustChoose(this);
      }

    return false;
  }

  /**
   *
   * Returns a list of recommended and/or mandatory choices 
   * for this field.  This list is dynamically generated by
   * subclasses of DBEditObject; this method should not need
   * to be overridden.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public Vector choices()
  {
    if (!(owner instanceof DBEditObject))
      {
	throw new IllegalArgumentException("can't get choice list on non-editable object");
      }

    return ((DBEditObject) owner).obtainChoiceList(this);
  }

  /**
   *
   * Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.
   *
   * @see arlut.csd.ganymede.string_field
   * 
   */

  public String allowedChars()
  {
    return definition.getOKChars();
  }

  /**
   *
   * Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public String disallowedChars()
  {
    return definition.getBadChars();
  }

  /**
   *
   * Convenience method to identify if a particular
   * character is acceptable in this field.
   *
   * @see arlut.csd.ganymede.string_field
   *
   */

  public boolean allowed(char c)
  {
    if (allowedChars() != null && (allowedChars().indexOf(c) == -1))
      {
	return false;
      }

    if (disallowedChars() != null && (disallowedChars().indexOf(c) != -1))
      {
	return false;
      }
    
    return true;
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return (o instanceof String);
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s, s2;
    Vector v;
    boolean ok = true;

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

    s = (String) o;

    if (s.length() > maxSize())
      {
	// string too long
	
	setLastError("string value " + s +
		     " is too long for field " + 
		     getName() +
		     " which has a length limit of " + 
		     maxSize());
	return false;
      }

    if (s.length() < minSize())
      {
	// string too short
	
	setLastError("string value " + s +
		     " is too short for field " + 
		     getName() + 
		     " which has a minimum length of " + 
		     minSize());
	return false;
      }
    
    if (allowedChars() != null)
      {
	String okChars = allowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (okChars.indexOf(s.charAt(i)) == -1)
	      {
		setLastError("string value" + s +
			     "contains a bad character " + 
			     s.charAt(i));
		return false;
	      }
	  }
      }
    
    if (disallowedChars() != null)
      {
	String badChars = disallowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (badChars.indexOf(s.charAt(i)) != -1)
	      {
		setLastError("string value" + s +
			     "contains a bad character " + 
			     s.charAt(i));
		return false;
	      }
	  }
      }

    if (mustChoose())
      {
	ok = false;
	v = choices();

	for (int i = 0; i < v.length() && !ok; i++)
	  {
	    s2 = (String) v.elementAt(i);

	    if (s2.equals(s))
	      {
		ok = true;
	      }
	  }

	if (!ok)
	  {
	    setLastError("string value " + s + " is not a valid choice");
	    return false;
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }
}
