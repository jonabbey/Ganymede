/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import jcrypt;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordDBField

------------------------------------------------------------------------------*/

public class PasswordDBField extends DBField implements pass_field {

  boolean crypted;

  /* -- */

  /**
   *
   * Receive constructor.  Used to create a PasswordDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  PasswordDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException, RemoteException
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

  PasswordDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
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

  public PasswordDBField(DBObject owner, PasswordDBField field) throws RemoteException
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

  public PasswordDBField(DBObject owner, String value, DBObjectBaseField definition) throws RemoteException
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

  public PasswordDBField(DBObject owner, Vector values, DBObjectBaseField definition) throws RemoteException
  {
    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone()
  {
    try
      {
	return new PasswordDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't clone PasswordDBField: " + ex);
      }
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeBoolean(crypted);
    out.writeUTF((String)value);
  }

  void receive(DataInput in) throws IOException
  {
    crypted = in.readBoolean();
    value = in.readUTF();
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
    throw new IllegalArgumentException("vector accessor called on scalar");
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

    return this.value();
  }

  // ****
  //
  // pass_field methods 
  //
  // ****

  /**
   *
   * Returns the maximum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.pass_field
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
   * @see arlut.csd.ganymede.pass_field
   *
   */

  public int minSize()
  {
    return definition.getMinLength();
  }

  /**
   *
   * Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.
   *
   * @see arlut.csd.ganymede.pass_field
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
   * @see arlut.csd.ganymede.pass_field
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
   * @see arlut.csd.ganymede.pass_field
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

  /**
   *
   * Verification method for comparing a plaintext entry with a crypted
   * value.
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public boolean matchPlainText(String text)
  {
    if (value == null || text == null)
      {
	return false;
      }

    return (((String) value).equals(jcrypt.crypt(getSalt(), text)));
  }

  /**
   *
   * Verification method for comparing a crypt'ed entry with a crypted
   * value.  The salts for the stored and submitted values must match
   * in order for a comparison to be made, else an illegal argument
   * exception will be thrown
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public boolean matchCryptText(String text)
  {
    if (value == null || text == null)
      {
	return false;
      }

    if (!((String) text).startsWith(getSalt()))
      {
	throw new IllegalArgumentException("bad salt");
      }

    return (text.equals((String)value));
  }

  /**
   *
   * Method to obtain the SALT for a stored crypted password.  If the
   * client is going to submit a pre-crypted password, it must be
   * salted by the salt returned by this method.  If the password
   * is stored in plaintext, null will be returned.
   * 
   */

  public String getSalt()
  {
    if (crypted && value != null)
      {
	return ((String) value).substring(2);
      }
    else
      {
	return null;
      }
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

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }
}
