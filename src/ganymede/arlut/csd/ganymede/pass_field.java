/*

   pass_field.java

   Remote interface definition.

   Created: 21 July 1997
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 2002/03/29 03:57:58 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      pass_field

------------------------------------------------------------------------------*/

public interface pass_field extends db_field {

  int maxSize() throws RemoteException;
  int minSize() throws RemoteException;

  /**
   *
   * Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.
   * 
   */

  String allowedChars() throws RemoteException;

  /**
   *
   * Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.
   *
   */

  String disallowedChars() throws RemoteException;

  /**
   *
   * Convenience method to identify if a particular
   * character is acceptable in this field.
   *
   */

  boolean allowed(char c) throws RemoteException;

  /**
   *
   * Returns true if the password stored in this field is hash-crypted.
   *
   */

  boolean crypted() throws RemoteException;

  /**
   * <p>This method is used for authenticating a provided plaintext
   * password against the stored contents of this password field.  The
   * password field may have stored the password in plaintext, or in
   * any of a variety of cryptographic hash formats.  matchPlainText()
   * will perform whatever operation on the provided plaintext as is
   * required to determine whether or not it matches with the stored
   * password data.</p>
   *
   * @return true if the given plaintext matches the stored password
   */

  boolean matchPlainText(String text) throws RemoteException;

  /** 
   * <p>This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.</p> 
   */

  ReturnVal setPlainTextPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted password for this field.
   * This method will return false if this password field is not
   * stored crypted.</p>
   */

  ReturnVal setCryptPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted FreeBSD-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p> 
   */

  ReturnVal setMD5CryptedPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set a pre-crypted Apache-style
   * MD5Crypt password for this field.  This method will return
   * false if this password field is not stored crypted.</p> 
   */

  ReturnVal setApacheMD5CryptedPass(String text) throws RemoteException;

  /**
   * <p>This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in Samba's
   * encrypted password files.  This method will return
   * an error code if this password field is not configured to accept
   * Windows-hashed password strings.</p> 
   */

  ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4) throws RemoteException;


  /**
   * <p>This method is used to force all known hashes into this password
   * field.  Ganymede does no verifications to insure that all of these
   * hashes really match the same password, so caveat emptor.  If any of
   * these hashes are null or empty string, those hashes will be cleared.</p>
   *
   * <p>Calling this method will clear the password's stored plaintext,
   * if any.</p>
   *
   * <p>If this password field is not configured to support any of the
   * various hash formats in the Ganymede schema, an error will be returned.</p>
   */

  public ReturnVal setAllHashes(String crypt, String md5crypt, String apacheMd5crypt,
				String LANMAN, String NTUnicodeMD4, 
				boolean local, boolean noWizards) throws RemoteException;

}
