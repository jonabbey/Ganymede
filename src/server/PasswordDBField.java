/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997
   Version: $Revision: 1.21 $ %D%
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

  String cryptedPass;
  String uncryptedPass;

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

    cryptedPass = field.cryptedPass;
    uncryptedPass = field.uncryptedPass;

    if (cryptedPass != null || uncryptedPass != null)
      {
	defined = true;
      }
    else
      {
	defined = false;
      }
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
    if (cryptedPass == null)
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(cryptedPass);
      }

    if (uncryptedPass == null)
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(uncryptedPass);
      }
  }

  void receive(DataInput in) throws IOException
  {
    if ((Ganymede.db.file_major > 1) || (Ganymede.db.file_minor >= 10))
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }
	
	uncryptedPass = in.readUTF();

	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }
      }
    else
      {
	if (definition.isCrypted())
	  {
	    cryptedPass = in.readUTF();
	    uncryptedPass = null;
	  }
	else
	  {
	    uncryptedPass = in.readUTF();
	    cryptedPass = null;
	  }
      }
  }

  /**
   *
   * We always return null here..
   *
   */

  public Object getValue()
  {
    return null;
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  public synchronized String getValueString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (cryptedPass != null || uncryptedPass != null)
      {
	return "<Password>";
      }
    else
      {
	return null;
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

    /* -- */

    if (!(orig instanceof PasswordDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PasswordDBField) orig;

    if ((cryptedPass != origP.cryptedPass) || (uncryptedPass != origP.uncryptedPass))
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

  public synchronized boolean matchPlainText(String text)
  {
    String cryptedText;

    /* -- */

    if ((cryptedPass == null && uncryptedPass == null) || text == null)
      {
	return false;
      }

    if (uncryptedPass != null)
      {
	return text.equals(uncryptedPass);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("present crypted text == " + cryptedPass);

	    System.err.println("getSalt() == '" + getSalt() + "'");
	  }

	cryptedText = jcrypt.crypt(getSalt(), text);

	if (debug)
	  {
	    System.err.println("comparison crypted text == " + cryptedText);
	  }

	if (cryptedPass.equals(cryptedText))
	  {
	    // If we're set up to keep plaintext copies or our
	    // encrypted passwords, we're going to go ahead and make a
	    // note of the plaintext password we just matched to the
	    // crypt text.  This is really pretty funky, because this
	    // is being done outside of any transactional context, but
	    // we're really not changing the *content* of this
	    // password field, we're just remembering another thing
	    // about the password we already are keeping.. to wit, the
	    // actual plain text.  By doing this, Ganymede can accumulate
	    // plaintext copies of the passwords whenever anyone logs in
	    // to it (assuming that the schema is set up to have the user's
	    // password field keep a plaintext copy.)

	    if (definition.isPlainText())
	      {
		uncryptedPass = text;
	      }

	    return true;
	  }
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
    if (cryptedPass == null || text == null)
      {
	return false;
      }

    if (!text.startsWith(getSalt()))
      {
	throw new IllegalArgumentException("bad salt");
      }

    return text.equals(cryptedPass);
  }

  /**
   *
   * This method returns the UNIX-encrypted password text.
   *
   */

  public String getUNIXCryptText()
  {
    if (crypted())
      {
	return cryptedPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return jcrypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /**
   *
   * Method to obtain the SALT for a stored crypted password.  If the
   * client is going to submit a pre-crypted password for comparison
   * against a stored crypted password by use of the matchCryptText()
   * method, it must be salted by the salt returned by this method.
   * If the password is stored in plaintext, null will be returned.
   * 
   * @see arlut.csd.ganymede.pass_field 
   */

  public String getSalt()
  {
    if (definition.isCrypted() && cryptedPass != null)
      {
	return cryptedPass.substring(0,2);
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
	    if (text != null)
	      {
		cryptedPass = jcrypt.crypt(text);

		// see whether the schema editor has us trying to save
		// plain text

		if (definition.isPlainText())
		  {
		    uncryptedPass = text;
		  }
		else
		  {
		    uncryptedPass = null;
		  }

		if (debug)
		  {
		    System.err.println("Receiving plain text pass.. crypted = " + cryptedPass + ", plain = " + text);
		  }

		defined = true;
	      }
	    else
	      {
		cryptedPass = null;
		uncryptedPass = null;

		defined = false;
	      }
	  }
	else
	  {
	    cryptedPass = null;
	    uncryptedPass = text;

	    defined = (uncryptedPass != null);
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
	// whenever the crypt password is directly set, we lose 
	// plaintext

	if ((text == null) || (text.equals("")))
	  {
	    cryptedPass = null;
	    uncryptedPass = null;
	    defined = false;
	  }
	else
	  {
	    cryptedPass = text;
	    uncryptedPass = null;
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
