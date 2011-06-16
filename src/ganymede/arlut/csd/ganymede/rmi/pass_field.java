/*

   pass_field.java

   Remote interface definition.

   Created: 21 July 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

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

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      pass_field

------------------------------------------------------------------------------*/

/**
 * <p>Remote RMI reference to a Ganymede {@link
 * arlut.csd.ganymede.server.PasswordDBField PasswordDBField}.</p>
 *
 * <p>pass_field is an extension of the {@link
 * arlut.csd.ganymede.rmi.db_field} interface, and provides a set of
 * additional methods that the client can use to interact with a
 * password field on the Ganymede server.</p>
 */

public interface pass_field extends db_field {

  int maxSize() throws RemoteException;
  int minSize() throws RemoteException;

  /**
   * Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.
   */

  String allowedChars() throws RemoteException;

  /**
   * Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.
   */

  String disallowedChars() throws RemoteException;

  /**
   * Convenience method to identify if a particular
   * character is acceptable in this field.
   */

  boolean allowed(char c) throws RemoteException;

  /**
   * <p>Returns true if the password stored in this field is hash-crypted
   * using the traditional Unix Crypt algorithm.</p>
   *
   * <p>NB: This method is part of the pass_field client API for the
   * PasswordDBField class, but it only returns true if the server is
   * holding a traditional Unix crypt hash.  We don't have comparable
   * methods for other hash formats, and the client could always query
   * the field definition using the DBObjectBase base_field remote
   * API, in any case.  We should probably ditch this method from the
   * pass_field rmi interface.</p>
   *
   * @deprecated
   */

  boolean crypted() throws RemoteException;

  /**
   * <p>Authenticates a provided plaintext password against the stored
   * contents of this password field.</p>
   *
   * <p>The password field may have stored the password in plaintext,
   * or in any of a variety of cryptographic hash formats.
   * matchPlainText() will perform whatever operation on the provided
   * plaintext as is required to determine whether or not it matches
   * with the stored password data.</p>
   *
   * <p>If this field is configured to create and retain other hash
   * formats, this method will create the missing hash formats as
   * needed if the input text is successfully matched against the
   * password data held in this field.</p>
   *
   * @return true if the given plaintext matches the stored password
   */

  boolean matchPlainText(String text) throws RemoteException;

  /** 
   * <p>Set the plain text password for this field.</p>
   *
   * <p>If this field is configured to create and retain other hash
   * formats, it will do so as needed during the call to this
   * method.</p>
   */

  ReturnVal setPlainTextPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-hashed password for this field,
   * using the traditional (weak) Unix Crypt algorithm.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store Crypt hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   */

  ReturnVal setCryptPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted FreeBSD-style MD5Crypt
   * password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store MD5Crypt hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   */

  ReturnVal setMD5CryptedPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted Apache-style MD5Crypt
   * password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store Apache-style MD5Crypt hashed password
   * text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   */

  ReturnVal setApacheMD5CryptedPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in
   * Samba's encrypted password files.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to accept Windows-hashed password strings.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   */

  ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted OpenLDAP/Netscape
   * Directory Server Salted SHA (SSHA) password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store SSHA hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   */

  ReturnVal setSSHAPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted Sha256Crypt or
   * Sha512Crypt password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store ShaCrypt hashed password text.</p>
   *
   * <p>The hashText submitted to this method must match one of the
   * following four forms:</p>
   *
   * <pre>
   * $5$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * $5$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   *
   * $6$&lt;saltstring&gt;$&lt;64 bytes of hash text, base 64 encoded&gt;
   * $6$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * </pre>
   *
   * <p>If the round count is specified using the '$rounds=n' syntax, the
   * higher the round count, the more computational work will be
   * required to verify passwords against this hash text.</p>
   *
   * <p>See <a href="http://people.redhat.com/drepper/sha-crypt.html">http://people.redhat.com/drepper/sha-crypt.html</a>
   * for full details of the hash format this method is expecting.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   */

  ReturnVal setShaUnixCryptPass(String hashText) throws RemoteException;
}
