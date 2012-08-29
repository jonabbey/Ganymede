/*

   invid_field.java

   Remote interface definition.

   Created: 14 November 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
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

import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                     invid_field

------------------------------------------------------------------------------*/

/**
 * <P>Client-side remote interface for the
 * server-side {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} class.
 */

public interface invid_field extends db_field {

  boolean limited() throws RemoteException;

  // the following methods provided stringbuffer-encoded
  // information on the values and choices present
  // for this field.

  QueryResult encodedValues() throws RemoteException;

  /**
   * Returns true if the only valid values for this invid field are in
   * the QueryResult returned by choices().  In particular, if mustChoose()
   * returns true, &lt;none&gt; is not an acceptable choice for this field
   * after the field's value is initially set.
   */

  boolean mustChoose() throws RemoteException;

  /**
   * <p>Returns a StringBuffer encoded list of acceptable invid values
   * for this field.</p>
   */

  QueryResult choices(boolean applyFilter) throws RemoteException;

  /**
   * <p>Returns a StringBuffer encoded list of acceptable invid values
   * for this field.</p>
   */

  QueryResult choices() throws RemoteException;

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>If choicesKey() returns null, the client should not attempt to
   * use any cached values for the choice list, and should go ahead
   * and call choices() to get the freshly generated list.</p>
   */

  Object choicesKey() throws RemoteException;

  /**
   * <p>This method returns true if this invid field should not
   * show any choices that are currently selected in field
   * x, where x is another field in this db_object.</p>
   */

  boolean excludeSelected(db_field x) throws RemoteException;

  // the following methods apply if this is an edit-in-place vector

  /**
   * <p>This method is used to create a new embedded object in an
   * invid field that contains a vector of edit-in-place/embedded
   * objects.  The ReturnVal returned indicates success/failure,
   * and on success will provide the Invid of the newly created
   * embedded when ReturnVal.getInvid() is called on it.</p>
   */

  ReturnVal createNewEmbedded() throws RemoteException;

  /**
   * <p>Return the object type that this invid field is constrained to
   * point to, if set</p>
   *
   * <p>A negative value means there is no one type of object that
   * this field is constrained to point to.</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is
   * a specified symmetric field.</p>
   */

  short getTargetBase() throws RemoteException;
}
