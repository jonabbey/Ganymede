/*

   invid_field.java

   Remote interface definition.

   Created: 14 November 1996
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;
import java.util.*;
import java.rmi.RemoteException;

public interface invid_field extends db_field {
  boolean limited() throws RemoteException;
  int getAllowedTarget() throws RemoteException;

  // the following methods provided stringbuffer-encoded
  // information on the values and choices present
  // for this field.

  QueryResult encodedValues() throws RemoteException;
  QueryResult choices() throws RemoteException;

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.
   *
   */

  Object choicesKey() throws RemoteException;

  /**
   * This method returns true if this invid field should not
   * show any choices that are currently selected in field
   * x, where x is another field in this db_object.
   *
   */

  boolean excludeSelected(db_field x) throws RemoteException;

  // the following methods apply if this is an edit-in-place vector

  Invid createNewEmbedded() throws RemoteException;

  /**
   *
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>A negative value means there is no one type of object that this field is constrained
   * to point to.</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is a specified symmetric field.</p>
   *
   */

  short getTargetBase() throws RemoteException;
}
