/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997
   Version: $Revision: 1.14 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import jcrypt;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordDBField

------------------------------------------------------------------------------*/

public class PasswordDBField extends DBField implements pass_field {

  /* -- */
  
  static final boolean debug = false;

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
    out.writeUTF((String)value);
  }

  void receive(DataInput in) throws IOException
  {
    value = in.readUTF();
  }

  public Object getValue()
  {
    Object result;

    /* -- */

    try
      {
	result = super.getValue();
      }
    catch (IllegalArgumentException ex)
      {
	return null;
      }

    // we can safely return the password if it's
    // stored in hash crypted form.

    if (crypted())
      {
	return result;
      }
    else
      {
	return "<Password>";
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

    // only return the password value if it is hash-crypted

    if (crypted())
      {
	return (String) value;
      }
    else
      {
	return "<Password>";
      }
  }

  /**
   *
   * The default getValueString() encoding is acceptable.
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
    PasswordDBField origP;
    String a, b;

    /* -- */

    if (!(orig instanceof PasswordDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PasswordDBField) orig;

    a = (String) origP.value;
    b = (String) this.value;

    if (!(a.equals(b)))
      {
	return "\tPassword changed";
      }
    else
      {
	return null;
      }
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
   * Returns true if the password stored in this field is hash-crypted.
   *
   * @see arlut.csd.ganymede.pass_field
   */

  public boolean crypted()
  {
    return (definition.isCrypted());
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
    String cryptedText;

    /* -- */

    if (value == null || text == null)
      {
	return false;
      }

    if (definition.isCrypted())
      {
	if (debug)
	  {
	    System.err.println("present crypted text == " + value);

	    System.err.println("getSalt() == '" + getSalt() + "'");
	  }

	cryptedText = jcrypt.crypt(getSalt(), text);

	if (debug)
	  {
	    System.err.println("comparison crypted text == " + cryptedText);
	  }

	return ((String) value).equals(cryptedText);
      }
    else
      {
	return text.equals(value);
      }
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
   * This method returns the UNIX-encrypted password text.
   *
   */

  public String getUNIXCryptText()
  {
    if (value == null)
      {
	return null;
      }

    if (crypted())
      {
	return (String) value;
      }
    else
      {
	return jcrypt.crypt((String) value);
      }
  }

  /**
   *
   * Method to obtain the SALT for a stored crypted password.  If the
   * client is going to submit a pre-crypted password, it must be
   * salted by the salt returned by this method.  If the password
   * is stored in plaintext, null will be returned.
   * 
   * @see arlut.csd.ganymede.pass_field
   */

  public String getSalt()
  {
    if (definition.isCrypted() && value != null)
      {
	return ((String) value).substring(0,2);
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * Sets the value of this field, if a scalar.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   */

  public ReturnVal setValue(Object value, boolean local)
  {
    throw new IllegalArgumentException("can't directly set the value on a password field");
  }

  /**
   *
   * This method is used to set the password for this field,
   * crypting it if this password field is stored crypted.
   *
   * @see arlut.cds.ganymede.pass_field
   *
   */

  public ReturnVal setPlainTextPass(String text)
  {
    String cryptedText = null;
    ReturnVal retVal;

    /* -- */

    if (!verifyNewValue(text))
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setPlainTextPass()",
					 "Invalid password value\n" + getLastError());
      }

    retVal = ((DBEditObject) owner).finalizeSetValue(this, text);

    if (retVal == null || retVal.didSucceed())
      {
	if (definition.isCrypted())
	  {
	    cryptedText = jcrypt.crypt(text);

	    if (debug)
	      {
		System.err.println("Receiving plain text pass.. crypted = " + cryptedText + ", plain = " + text);
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Not encrypting.. plain = " + text);
	      }
	  }

	if ((text == null) || (text.equals("")))
	  {
	    value = null;
	    defined = false;
	  }
	else
	  {
	    this.value = (cryptedText == null) ? text : cryptedText;
	    defined = true;
	  }
      }

    return retVal;
  }

  /**
   *
   * This method is used to set a pre-crypted password for this field.
   *
   * This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.
   *
   * @see arlut.csd.ganymede.pass_field
   *
   */

  public ReturnVal setCryptPass(String text)
  {
    ReturnVal retVal;

    if (!definition.isCrypted())
      {
	owner.editset.session.setLastError("can't set a pre-crypted value into a plaintext password field");

	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setCryptTextPass()",
					  "Can't set a pre-crypted value into a plaintext password field");
      }

    if (!verifyNewValue(text))
      {
	return Ganymede.createErrorDialog("Server: Error in PasswordDBField.setCryptTextPass()",
					  "Invalid crypted password value\n" + getLastError());
      }

    retVal = ((DBEditObject)owner).finalizeSetValue(this, text);

    if (retVal == null || retVal.didSucceed())
      {
	if ((text == null) || (text.equals("")))
	  {
	    value = null;
	    defined = false;
	  }
	else
	  {
	    this.value = text;
	    defined = true;
	  }
      }

    return retVal;
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s, s2;
    Vector v;
    boolean ok = true;

    /* -- */

    if (!isEditable(true))
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    if (o == null)
      {
	return true; // assume we can null out this field
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
