/*

   invid_field.java

   Remote interface definition.

   Created: 14 November 1996
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 1999/06/15 02:48:31 $
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
import java.util.*;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                     invid_field

------------------------------------------------------------------------------*/

/**
 * <P>Client-side remote interface for the
 * server-side {@link arlut.csd.ganymede.InvidDBField InvidDBField} class.
 */

public interface invid_field extends db_field {

  boolean limited() throws RemoteException;
  int getAllowedTarget() throws RemoteException;

  // the following methods provided stringbuffer-encoded
  // information on the values and choices present
  // for this field.

  QueryResult encodedValues() throws RemoteException;

  /**
   *
   * Returns true if the only valid values for this invid field are in
   * the QueryResult returned by choices().  In particular, if mustChoose()
   * returns true, <none> is not an acceptable choice for this field
   * after the field's value is initially set.
   *
   */

  boolean mustChoose() throws RemoteException;
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

  /**
   *
   * This method is used to create a new embedded object in an
   * invid field that contains a vector of edit-in-place/embedded
   * objects.  The ReturnVal returned indicates success/failure,
   * and on success will provide the Invid of the newly created
   * embedded when ReturnVal.getInvid() is called on it.
   *
   */

  ReturnVal createNewEmbedded() throws RemoteException;

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
