/*

   BaseField.java

   Remote interface allowing the client access to field typing information
   from a field.
   
   Created: 17 April 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System

   Copyright (C) 1996-2011
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       BaseField

------------------------------------------------------------------------------*/

/**
 * <p>Client side interface definition for the Ganymede
 * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
 * class.  This interface allows the client to query and edit type information
 * remotely.</p>
 *
 * <p>All of the editing methods may be only be called when the server is
 * in schema editing mode.</p>
 */

public interface BaseField extends Remote {

  /**
   * <p>This method returns true if this field is one of the
   * system fields present in all objects.</p>
   */

  public boolean isBuiltIn() throws RemoteException;

  /**
   * <p>This method returns true if this field definition can be removed
   * by the schema editor.</p>
   */

  public boolean isRemovable() throws RemoteException;

  /**
   * <p>This method returns true if this field
   * is intended to be visible to the client normally,
   * false otherwise.</p>
   */

  public boolean isVisible() throws RemoteException;

  // general

  /**
   * <p>Returns the Base we are a part of.</p>
   */

  public Base getBase() throws RemoteException;

  /**
   * <p>Returns the name of this field</p>
   */

  public String getName() throws RemoteException;

  /**
   * <p>Returns the comment defined in the schema for this field</p>
   */

  public String getComment() throws RemoteException;

  /**
   * Returns the name of the tab that is to contain this field on the client.
   */

  public String getTabName() throws RemoteException;

  /**
   * <p>Returns id code for this field.  Each field in a
   * {@link arlut.csd.ganymede.server.DBObject DBObject}
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by 
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * to choose what field to change in the setField method.</p>
   */

  public short getID() throws RemoteException;

  // all of the setter methods below can only be called when a SchemaEdit
  // is in progress.

  /**
   * <p>Sets the name of this field</p>
   */

  public ReturnVal setName(String name) throws RemoteException;

  /**
   * <p>Sets the comment defined in the schema for this field</p>
   */

  public ReturnVal setComment(String s) throws RemoteException;

  /**
   * Sets the name of the tab that is to contain this field on the client.
   */

  public ReturnVal setTabName(String s) throws RemoteException;

  // type info

  /**
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the {@link arlut.csd.ganymede.common.FieldType FieldType}
   * interface:</p>
   *
   * <pre>
   *   static short BOOLEAN = 0;
   *   static short NUMERIC = 1;
   *   static short DATE = 2;
   *   static short STRING = 3;
   *   static short INVID = 4;
   *   static short PERMISSIONMATRIX = 5;
   *   static short PASSWORD = 6;
   *   static short IP = 7;
   *   static short FLOAT = 8;
   *   static short FIELDOPTIONS = 9;
   * </pre>
   */

  public short getType() throws RemoteException;

  /**
   * <p>Returns true if this field is of boolean type</p>
   */

  public boolean isBoolean() throws RemoteException;

  /**
   * <p>Returns true if this field is of numeric type</p>
   */

  public boolean isNumeric() throws RemoteException;

  /**
   * <p>Returns true if this field is of float type</p>
   */

  public boolean isFloat() throws RemoteException;

  /**
   * <p>Returns true if this field is of field options type</p>
   */

  public boolean isFieldOptions() throws RemoteException;

  /**
   * <p>Returns true if this field is of date type</p>
   */

  public boolean isDate() throws RemoteException;

  /**
   * <p>Returns true if this field is of string type</p>
   */

  public boolean isString() throws RemoteException;

  /**
   * <p>Returns true if this field is of invid type</p>
   */

  public boolean isInvid() throws RemoteException;

  /**
   * <p>Returns true if this field is of permission matrix type</p>
   */

  public boolean isPermMatrix() throws RemoteException;

  /**
   * <p>Returns true if this field is of password type</p>
   */

  public boolean isPassword() throws RemoteException;

  /**
   * <p>Returns true if this field is of IP address type</p>
   */

  public boolean isIP() throws RemoteException;

  /**
   * <p>Method to set the visibility or invisibility of this field.</p>
   *
   * <p>Used by the DBStore to mark certain scratch fields as being
   * permanently hidden without having to set a custom DBEditObject
   * subclass to declare the non-visibility of the field.</p>
   */

  public void setVisibility(boolean visibility) throws RemoteException;

  /**
   * <p>Sets the {@link arlut.csd.ganymede.common.FieldType field type}
   * for this field.  Changing the basic type of a field that is already being
   * used in the server will cause very bad things to happen.  The
   * right way to change an existing field is to delete the field, commit
   * the schema edit, edit the schema again, and recreate the field with
   * the desired field type.</P>
   *
   * <p>If the new field type is not string, invid, or IP, the field
   * will be made a scalar field.</p>
   */

  public ReturnVal setType(short type) throws RemoteException;

  // vector info

  /**
   * <p>Returns true if this field is a vector field, false otherwise.</p>
   */

  public boolean isArray() throws RemoteException;

  /**
   * <p>Returns the array size limitation for this field if it is an array field</p>
   */

  public short getMaxArraySize() throws RemoteException;

  /**
   * <p>Set this field to be a vector or scalar.  If b is true, this field will
   * be a vector, if false, scalar.</p>
   *
   * <p>Only strings, invid's, and ip fields may be vectors.  Attempting to 
   * setArray(true) for other field types will cause an IllegalArgumentException
   * to be thrown.</p>
   *
   * <p>It may be possible to compatibly handle the conversion from
   * scalar to vector, but a vector to scalar change is an incompatible
   * change.</p>
   */

  public ReturnVal setArray(boolean b) throws RemoteException;

  /**
   * <p>Set the maximum number of values allowed in this vector field.</p>
   */

  public ReturnVal setMaxArraySize(short limit) throws RemoteException;

  // boolean

  /**
   * <p>Returns true if this is a boolean field with labels</p>
   */

  public boolean isLabeled() throws RemoteException;

  /**
   * <p>Returns the true Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   */

  public String getTrueLabel() throws RemoteException;

  /**
   * <p>Returns the false Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   */

  public String getFalseLabel() throws RemoteException;

  /**
   * <p>Turn labeled choices on/off for a boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a boolean type.</p>
   */

  public ReturnVal setLabeled(boolean b) throws RemoteException;

  /**
   * <p>Sets the label associated with the true choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   */

  public ReturnVal setTrueLabel(String label) throws RemoteException;

  /**
   * <p>Sets the label associated with the false choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   */

  public ReturnVal setFalseLabel(String label) throws RemoteException;

  // string

  /**
   * <p>Returns the minimum acceptable string length if this is a string or
   * password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public short getMinLength() throws RemoteException;

  /**
   * <p>Returns the maximum acceptable string length if this is a string 
   * or password field.</p>
   * 
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public short getMaxLength() throws RemoteException;

  /**
   * <p>Returns the set of acceptable characters if this is a string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public String getOKChars() throws RemoteException;

  /**
   * <p>Returns the set of unacceptable characters if this is a 
   * string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public String getBadChars() throws RemoteException;

  /**
   * <p>Returns true if this string field is intended to be a multi-line
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   */

  public boolean isMultiLine() throws RemoteException;

  /**
   * <p>Sets the minimum acceptable length for this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public ReturnVal setMinLength(short val) throws RemoteException;

  /** 
   * <p>Sets the maximum acceptable length for this string or
   * password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public ReturnVal setMaxLength(short val) throws RemoteException;

  /**
   * <p>Sets the set of characters that are allowed in this string or 
   * password field.  If s is null, all characters by default 
   * are acceptable.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   */

  public ReturnVal setOKChars(String s) throws RemoteException;

  /**
   * <p>Sets the set of characters that are specifically disallowed in
   * this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setBadChars(String s) throws RemoteException;

  /**
   * <p>Sets whether or not this string field should be presented as a
   * multiline field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   */

  public ReturnVal setMultiLine(boolean b) throws RemoteException;

  /**
   * <p>Returns the regexp pattern string constraining this string
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   */

  public String getRegexpPat() throws RemoteException;

  /**
   * <p>Returns the text description of the regexp pattern string
   * constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   */

  public String getRegexpDesc() throws RemoteException;

  /**
   * <p>Sets the regexp pattern string constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   */

  public ReturnVal setRegexpPat(String s) throws RemoteException;

  /**
   * <p>Sets the text descriptionf or the regexp pattern string
   * constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   */

  public ReturnVal setRegexpDesc(String s) throws RemoteException;

  // these two apply to strings, numbers, and IP addresses

  /**
   * <p>Returns the label of this string, numeric, or IP field's namespace.</p>
   */

  public String getNameSpaceLabel() throws RemoteException;

  /**
   * <p>Set a namespace constraint for this string, numeric, or
   * IP field.</p>
   *
   * <p>Note that this is intended to be called from the Schema Editor,
   * and won't take effect until the next time the system is stopped
   * and reloaded.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string, numeric, or IP type.</p>
   */

  public ReturnVal setNameSpace(String s) throws RemoteException;
  
  // invid

  /**
   * <p>Returns true if this is an invid field which is intended as an editInPlace
   * reference for the client's rendering.</p>
   */

  public boolean isEditInPlace() throws RemoteException;

  /**
   * <p>Sets whether or not this field is intended as an editInPlace
   * reference for the client's rendering.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public ReturnVal setEditInPlace(boolean b) throws RemoteException;

  /**
   * <p>Returns true if this is a target restricted invid field</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public boolean isTargetRestricted() throws RemoteException;

  /**
   * <p>If this field is a target restricted invid field, this method will return
   * true if this field has a symmetry relationship to the target</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public boolean isSymmetric() throws RemoteException;

  /**
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is a specified symmetric field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public short getTargetBase() throws RemoteException;

  /**
   * <p>If this field is a target restricted invid field, this method will return
   * a short indicating the field in the target object that the symmetry relation
   * applies to.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public short getTargetField() throws RemoteException;

  /**
   * <p>Sets the allowed target object code of this invid field to &lt;val&gt;.
   * If val is -1, this invid field can point to objects of any type.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public ReturnVal setTargetBase(short val) throws RemoteException;

  /**
   * <p>Sets the allowed target object code of this invid field to &lt;baseName&gt;.
   * If val is null, this invid field can point to objects of any type.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public ReturnVal setTargetBase(String baseName) throws RemoteException;

  /**
   * <p>Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().  If
   * val == -1, the targetField will be set to a value representing
   * no selection.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public ReturnVal setTargetField(short val) throws RemoteException;

  /**
   * <p>Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().  If &lt;fieldName&gt;
   * is null, the targetField will be cleared.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   */

  public ReturnVal setTargetField(String fieldName) throws RemoteException;

  // password

  /**
   * <p>Returns true if this field is a password field that has been
   * configured to have values submitted be checked against
   * cracklib.</p>
   */

  public boolean isCracklibChecked() throws RemoteException;

  /**
   * <p>Returns true if this field is a password field that is
   * cracklib checked which will allow supergash to disregard warnings
   * about cracklib.</p>
   */

  public boolean hasCracklibCheckException() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field
   * should check passwords entered with org.solinger.cracklib.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @param use_cracklib Control whether cracklib is applied on this
   * field at all.
   * @param supergash_exception If true, supergash-level admins will
   * be given an info dialog about a cracklib check failure, but will
   * be allowed to ignore the warning.
   */

  public ReturnVal setCracklibChecked(boolean use_cracklib, boolean supergash_exception) throws RemoteException;

  /**
   * <p>Returns true if this field is a password field that has been
   * configured to have values submitted be checked previous password
   * values linked to this DBObject.</p>
   */

  public boolean isHistoryChecked() throws RemoteException;

  /**
   * <p>Returns true if this field is a password field that is history
   * checked which will allow supergash to disregard warnings about
   * history repeats.</p>
   */

  public boolean hasHistoryCheckException() throws RemoteException;

  /**
   * <p>Returns the number of historical password hash values that
   * will be kept for this field.</p>
   */

  public int getHistoryDepth() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field should
   * check passwords against previous values associated with this
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @param use_history Control whether history checks are performed
   * on this field at all.
   * @param supergash_exception If true, supergash-level admins will
   * be given an info dialog about a cracklib check failure, but will
   * be allowed to ignore the warning.
   * @param depth An integer greater than or equal to zero which
   * controls how many previous password hashes should be retained for
   * history checking.
   */

  public ReturnVal setHistoryChecked(boolean use_history, boolean supergash_exception, int depth) throws RemoteException;

  /**
   * <p>This method returns true if this is a password field that
   * stores passwords in UNIX crypt format, and can thus accept
   * pre-crypted passwords.</p>
   */

  public boolean isCrypted() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field
   * should store passwords in UNIX crypt format.  If passwords
   * are stored in UNIX crypt format, they will not be kept in
   * plaintext on disk, regardless of the setting of setPlainText().</p>
   *
   * <p>setCrypted() is not mutually exclusive with setMD5Crypted().</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   */

  public ReturnVal setCrypted(boolean b) throws RemoteException;

  /** 
   * <p>This method returns true if this is a password field that
   * stores passwords in OpenBSD/FreeBSD/PAM md5crypt() format, and
   * can thus accept pre-crypted passwords.</p>
   */

  public boolean isMD5Crypted() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in OpenBSD/FreeBSD/PAM md5crypt() format.  If
   * passwords are stored in md5crypt() format, they will not be kept
   * in plaintext on disk, unless isPlainText() returns true.</p>
   *
   * <p>setMD5Crypted() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   */

  public ReturnVal setMD5Crypted(boolean b) throws RemoteException;

  /** 
   * <p>This method returns true if this is a password field that
   * stores passwords in Apache md5crypt() format, and
   * can thus accept pre-crypted passwords.</p>
   */

  public boolean isApacheMD5Crypted() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in Apache md5crypt() format.  If
   * passwords are stored in Apache md5crypt() format, they will not be kept
   * in plaintext on disk, unless isPlainText() returns true.</p>
   *
   * <p>setApacheMD5Crypted() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   */

  public ReturnVal setApacheMD5Crypted(boolean b) throws RemoteException;

  /** 
   * <p>This method returns true if this is a password field that will
   * store passwords in the two hashing formats used by Samba/Windows,
   * the older 14-char LANMAN hash, and the newer md5/Unicode hash
   * used by Windows NT.  If passwords are stored in the windows
   * hashing formats, they will not be kept in plaintext on disk,
   * unless isPlainText() returns true.</p>
   */

  public boolean isWinHashed() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in the Samba/Windows hashing formats.</p>
   *
   * <p>setWinHashed() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   */

  public ReturnVal setWinHashed(boolean b) throws RemoteException;

  /** 
   * <p>This method returns true if this is a password field that will
   * store passwords in the Netscape SSHA (salted SHA) hash format,
   * used in LDAP. If passwords are stored in the SSHA hashing format,
   * they will not be kept in plaintext on disk, unless isPlainText()
   * returns true.</p>
   */

  public boolean isSSHAHashed() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in the Netscape SSHA (salted SHA) LDAP format.</p>
   *
   * <p>setSSHAHashed() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   */

  public ReturnVal setSSHAHashed(boolean b) throws RemoteException;

  /** 
   * <p>This method returns true if this is a password field that will
   * store passwords in the OpenBSD BCrypt format.</p>
   *
   * <p>If passwords are stored in the BCrypt format, they will not be
   * kept in plaintext on disk, unless isPlainText() returns true.</p>
   */

  public boolean isBCrypted() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in the OpenBSD BCrypt format.</p>
   *
   * <p>setBCrypted() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if this field
   * definition is not a password type.</p>
   */

  public ReturnVal setBCrypted(boolean b) throws RemoteException;

  /** 
   * <p>This method returns the complexity factor (in the exponential
   * number of rounds) to be applied to password hash text generated
   * in this password field definition by the OpenBSD BCrypt
   * format.</p>
   */

  public int getBCryptRounds() throws RemoteException;

  /**
   * <p>This method is used to specify the complexity factor (in the
   * exponential number of rounds) to be applied to password hash text
   * generated in this password field definition by the OpenBSD
   * BCrypt format.</p>
   *
   * <p>This method will throw an IllegalArgumentException if this
   * field definition is not a BCrypt using password type.</p>
   */

  public ReturnVal setBCryptRounds(int n) throws RemoteException;

  /** 
   * This method returns true if this is a password field that will
   * store passwords in the SHA Unix Crypt format, specified by Ulrich
   * Drepper at http://people.redhat.com/drepper/sha-crypt.html.
   *
   * If passwords are stored in the SHA Unix Crypt format, they will
   * not be kept in plaintext on disk, unless isPlainText() returns
   * true.
   */

  public boolean isShaUnixCrypted() throws RemoteException;

  /**
   * This method is used to specify that this password field should
   * store passwords in the SHA Unix Crypt format, specified by Ulrich
   * Drepper at http://people.redhat.com/drepper/sha-crypt.html.
   *
   * setShaUnixCrypted() is not mutually exclusive with any other
   * encryption or plaintext options.
   *
   * This method will throw an IllegalArgumentException if this field
   * definition is not a password type.
   */

  public ReturnVal setShaUnixCrypted(boolean b) throws RemoteException;

  /** 
   * This method returns true if this is a shaUnixCrypted password
   * field that will store passwords using the SHA512 variant of the
   * SHA Unix Crypt format, specified by Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   */

  public boolean isShaUnixCrypted512() throws RemoteException;

  /**
   * This method is used to specify that this password field should
   * store passwords in the SHA512 variant of the SHA Unix Crypt
   * format, specified by Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   *
   * This method will throw an IllegalArgumentException if this field
   * definition is not a ShaUnixCrypt using password type.
   */

  public ReturnVal setShaUnixCrypted512(boolean b) throws RemoteException;

  /** 
   * This method returns the complexity factor (in number of rounds)
   * to be applied to password hash text generated in this password
   * field definition by the SHA Unix Crypt format, specified by
   * Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   */

  public int getShaUnixCryptRounds() throws RemoteException;

  /**
   * This method is used to specify the complexity factor (in number
   * of rounds) to be applied to password hash text generated in this
   * password field definition by the SHA Unix Crypt format, specified
   * by Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   *
   * This method will throw an IllegalArgumentException if this field
   * definition is not a ShaUnixCrypt using password type.
   */

  public ReturnVal setShaUnixCryptRounds(int n) throws RemoteException;

  /**
   * <p>This method returns true if this is a password field that
   * will keep a copy of the password in plaintext in the Ganymede
   * server's on-disk database.</p>
   */

  public boolean isPlainText() throws RemoteException;

  /**
   * <p>This method is used to specify that this password field
   * should keep a copy of the password in plaintext on disk,
   * even if other hash methods are in use which could be
   * used for Ganymede login authentication.  If no hash methods
   * are enabled for this password field, plaintext will be stored
   * on disk even if isPlainText() returns false for this field definition.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   */

  public ReturnVal setPlainText(boolean b) throws RemoteException;
  
  // convenience methods

  /**
   * <p>This method is intended to produce a human readable
   * representation of this field definition's type attributes.  This
   * method should not be used programatically to determine this
   * field's type information.</p>
   *
   * <p>This method is only for human information, and the precise
   * results returned are subject to change at any time.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public String getTypeDesc() throws RemoteException;
}
