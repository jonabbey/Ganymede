/*

   ip_field.java

   Remote interface definition.

   Created: 4 Sep 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

public interface ip_field extends db_field {

  /**
   *
   * Returns true if the (scalar) value stored in this IP field is an
   * IPV6 address
   * 
   */

  boolean isIPV6() throws RemoteException;

  /**
   *
   * Returns true if the (scalar) value stored in this IP field is an
   * IPV6 address
   * 
   */

  boolean isIPV4() throws RemoteException;

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV6 address.
   *
   * @param index Array index for the value to be checked
   *
   */

  boolean isIPV6(short index) throws RemoteException;

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV4 address.
   *
   * @param index Array index for the value to be checked
   *
   */

  boolean isIPV4(short index) throws RemoteException;
}
