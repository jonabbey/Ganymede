/*

   pass_field.java

   Remote interface definition.

   Created: 21 July 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
