/*

   pass_field.java

   Remote interface definition.

   Created: 21 July 1997
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/01/22 18:05:59 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;
import java.util.*;

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
   *
   * Verification method for comparing a plaintext entry with a crypted
   * value.
   *
   */

  boolean matchPlainText(String text) throws RemoteException;

  /**
   *
   * Verification method for comparing a crypt'ed entry with a crypted
   * value.  The salts for the stored and submitted values must match
   * in order for a comparison to be made, else an illegal argument
   * exception will be thrown
   *
   */

  boolean matchCryptText(String text) throws RemoteException;

  /**
   *
   * Method to obtain the SALT for a stored crypted password.  If the
   * client is going to submit a pre-crypted password for comparison
   * via matchCryptText(), it must be salted by the salt returned by
   * this method.
   *  
   */

  String getSalt() throws RemoteException;

  /**
   *
   * This method is used to set the password for this field,
   * crypting it if this password field is stored crypted.
   *
   */

  ReturnVal setPlainTextPass(String text) throws RemoteException;

  /**
   *
   * This method is used to set a pre-crypted password for this field.
   * This method will return false if this password field is not
   * stored crypted.
   *
   */

  ReturnVal setCryptPass(String text) throws RemoteException;
}
