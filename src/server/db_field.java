/*

   db_field.java

   A db_field is an item in a db_object.  A db_field can be a vector
   or a scalar.  

   Created: 10 April 1996
   Release: $Name:  $
   Version: $Revision: 1.21 $
   Last Mod Date: $Date: 2000/03/03 02:05:01 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                        db_field

------------------------------------------------------------------------------*/
/**
 * <p>Remote reference to a Ganymede {@link arlut.csd.ganymede.DBField DBField}, the
 * db_field is used by the client to make changes to a field when editing the
 * {@link arlut.csd.ganymede.db_object db_object} the field is contained within.</p>
 *
 * @version $Revision: 1.21 $ $Date: 2000/03/03 02:05:01 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public interface db_field extends java.rmi.Remote {

  /**
   * <p>Returns this field type's static type and constraint information.</p>
   */

  FieldTemplate getFieldTemplate() throws RemoteException;

  /**
   * <p>Returns a summary of this field's current value and visibility/editability status.</p>
   */

  FieldInfo getFieldInfo() throws RemoteException;

  /**
   * Returns the schema name for this field.
   */

  String getName() throws RemoteException;


  public boolean isBuiltIn() throws RemoteException;

  /**
   * Returns the field # for this field.
   */

  short getID() throws RemoteException;

  /**
   * Returns the description of this field from the
   * schema.
   */

  String getComment() throws RemoteException;

  /**
   * Returns the description of this field's type from
   * the schema.
   */

  String getTypeDesc() throws RemoteException;

  /**
   * Returns the type code for this field from the
   * schema.
   */

  short getType() throws RemoteException;

  /**
   * Returns a String representing the value of this field.
   */

  String getValueString() throws RemoteException;

  /**
   * Returns a String representing a reversible encoding of the
   * value of this field.  Each field type will have its own encoding,
   * suitable for embedding in a DumpResult.
   */

  String getEncodingString() throws RemoteException;

  /**
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   */

  boolean isDefined() throws RemoteException;

  /**
   * Returns true if this field is a vector, false
   * otherwise.
   */

  boolean isVector() throws RemoteException;

  /**
   * <p>Returns true if this field is editable, false
   * otherwise.</p>
   *
   * <p>Note that DBField are only editable if they are
   * contained in a subclass of DBEditObject.</p>
   */

  boolean isEditable() throws RemoteException;

  /**
   * Returns true if this field should be displayed in the
   * current client context.
   */

  boolean isVisible() throws RemoteException;

  /**
   * Returns true if this field is edit in place.
   */

  boolean isEditInPlace() throws RemoteException;

  // for scalars

  /**
   * Returns the value of this field, if a scalar.  An exception
   * will be thrown if this field is a vector.
   */

  Object getValue() throws RemoteException;

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   */

  ReturnVal setValue(Object value) throws RemoteException;

  // for vectors

  /**
   * Returns number of elements in vector if this is a vector field.  If
   * this is not a vector field, will return 1. (Should throw exception?)
   */

  int size() throws RemoteException;

  /** 
   * <p>Returns a Vector of the values of the elements in this field,
   * if a vector.</p>
   *
   * <p>This is only valid for vectors.  If the field is a scalar, use
   * getValue().</p>
   */

  Vector getValues() throws RemoteException;

  /**
   * <p>Returns the value of an element of this field,
   * if a vector.</p>
   */

  Object getElement(int index) throws RemoteException;

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.</p>
   */

  ReturnVal setElement(int index, Object value) throws RemoteException;

  /**
   * <p>Adds an element to the end of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful addElement will
   * encode an order to rescan this field.</p>
   */

  ReturnVal addElement(Object value) throws RemoteException;

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>The ReturnVal resulting from a successful addElements will
   * encode an order to rescan this field.</p> 
   */

  ReturnVal addElements(Vector values) throws RemoteException;

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, 
   * and may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  ReturnVal deleteElement(int index) throws RemoteException;

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, 
   * and may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  ReturnVal deleteElement(Object value) throws RemoteException;

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were deleted.  If failure is returned, no values
   * were deleted.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElements will
   * encode an order to rescan this field.</p> 
   */

  ReturnVal deleteElements(Vector values) throws RemoteException;

  /**
   * <p>Returns true if this field is a vector field and value is contained
   *  in this field.</p>
   *
   * <p>This method always checks for read privileges.</p>
   *
   * @param value The value to look for in this field
   */

  boolean containsElement(Object value) throws RemoteException;
}
