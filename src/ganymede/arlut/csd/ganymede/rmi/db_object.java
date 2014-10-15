/*

   db_object.java

   This interface defines the methods that the client can remotely
   call on a object held in the Ganymede server.

   Created: 11 April 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
import java.util.Date;
import java.util.Vector;

import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       db_object

------------------------------------------------------------------------------*/

/**
 * <p>Remote reference to a Ganymede {@link
 * arlut.csd.ganymede.server.DBObject DBObject} or {@link
 * arlut.csd.ganymede.server.DBEditObject DBEditObject}, the db_object
 * is used by the client to get information about and/or make changes
 * to a object held in the Ganymede server.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface db_object extends java.rmi.Remote {

  /**
   * <p>Returns the numeric id of the object in the objectBase</p>
   */

  public int getID() throws RemoteException;

  /**
   * <p>Returns the base id of the object's type</p>
   */

  public short getTypeID() throws RemoteException;

  /**
   * <p>Returns the name of the object's type</p>
   */

  public String getTypeName() throws RemoteException;

  /**
   * <p>Returns the unique object id (invid) of this object</p>
   */

  public Invid getInvid() throws RemoteException;

  /**
   * <p>Returns a vector of custom field information records, in display order.</p>
   *
   * <p>This method is called by the client so as to download all of
   * the field values in an object in a single remote method call.</p>
   *
   * <p>If the client does not have permission to view a field, that
   * field will be left out of the resulting Vector.</p>
   *
   * @see arlut.csd.ganymede.common.FieldInfo
   */

  public Vector<FieldInfo> getFieldInfoVector() throws RemoteException;

  /**
   * <p>Get access to a field from this object.</p>
   *
   * @param id The field code for the desired field of this object.
   */

  public db_field getField(short id) throws RemoteException;

  /**
   * <p>Get access to a field from this object, by name.</p>
   *
   * @param fieldname The fieldname for the desired field of this object
   */

  public db_field getField(String fieldname) throws RemoteException;

  /**
   * <P>Returns the name of a field from this object.</P>
   *
   * @param id The field code for the desired field of this object.
   */

  public String getFieldName(short id) throws RemoteException;

  /**
   * <p>This method returns the short field id code for the named
   * field, if the field is present in this object, or -1 if the
   * field could not be found.</p>
   */

  public short getFieldId(String fieldname) throws RemoteException;

  /**
   * Get access to the field that serves as this object's label
   */

  public db_field getLabelField() throws RemoteException;

  /**
   * Get access to the field id for the field that serves as this
   * object's label.
   */

  public short getLabelFieldID() throws RemoteException;

  /**
   * <p>Get list of all db_fields contained in this object,
   * in unsorted order.</p>
   */

  public db_field[] listFields() throws RemoteException;

  /**
   * Returns the label of this object.
   */

  public String getLabel() throws RemoteException;

  /**
   * <p>If this object is not embedded, returns the label of this
   * object in the same way that getLabel() does.</p>
   *
   * <p>If this object is embedded, returns a /-separated label
   * containing the name of all containing objects followed by this
   * object's label.</p>
   */

  public String getPathLabel() throws RemoteException;

  /**
   * <p>Returns true if this object is an embedded type</p>
   */

  public boolean isEmbedded() throws RemoteException;

  /**
   * <p>If this object type is embedded, this method will return the
   * desired display label for the embedded object.</p>
   *
   * <p>This label may not be the same as returned by getLabel(), which
   * is guaranteed to be derived from a namespace constrained label
   * field, suitable for use in the XML context.</p>
   */

  public String getEmbeddedObjectDisplayLabel() throws RemoteException;

  /**
   * <p>Returns true if inactivate() is a valid operation on
   * checked-out objects of this type.</p>
   */

  public boolean canInactivate() throws RemoteException;

  /**
   * <p>Returns true if this object has been inactivated and is
   * pending deletion.</p>
   */

  public boolean isInactivated() throws RemoteException;

  /**
   * <p>Returns true if this object has all its required fields defined</p>
   */

  public boolean isValid() throws RemoteException;

  /**
   * <p>Returns the date that this object is to go through final removal
   * if it has been inactivated.</p>
   */

  public Date getRemovalDate() throws RemoteException;

  /**
   * <p>Returns true if this object has an expiration date set.</p>
   */

  public boolean willExpire() throws RemoteException;

  /**
   * <p>Returns the date that this object is to be automatically
   * inactivated if it has an expiration date set.</p>
   */

  public Date getExpirationDate() throws RemoteException;

  /**
   * <p>Returns a String containing a URL that can be used by the
   * client to retrieve a picture representating this object.</p>
   *
   * <p>Intended to be used for users, primarily.</p>
   */

  public String getImageURL() throws RemoteException;

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   */

  public ReturnVal setFieldValue(String fieldName, Object value) throws RemoteException;

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   */

  public ReturnVal setFieldValue(short fieldID, Object value) throws RemoteException;

  /**
   * <p>Shortcut method to get a scalar field's value.  Using this
   * method saves a roundtrip to the server.</p>
   */

  public Object getFieldValue(String fieldName) throws RemoteException;

  /**
   * <p>Shortcut method to get a scalar field's value.  Using this
   * method saves a roundtrip to the server.</p>
   */

  public Object getFieldValue(short fieldID) throws RemoteException;

  /**
   * <p>Shortcut method to get a vector field's values.  Using this
   * method saves a roundtrip to the server.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If the field is defined
   * on this object type but is undefined in this individual object,
   * an empty Vector will be created and returned.</p>
   *
   * <p>Will never return null.</p>
   */

  public Vector getFieldValues(String fieldName) throws RemoteException;

  /**
   * <p>Shortcut method to get a vector field's values.  Using this
   * method saves a roundtrip to the server.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If the field is defined
   * on this object type but is undefined in this individual object,
   * an empty Vector will be created and returned.</p>
   *
   * <p>Will never return null.</p>
   */

  public Vector getFieldValues(short fieldID) throws RemoteException;

  /**
   * <p>Shortcut method to test to see if a vector field contains a
   * given value.</p>
   *
   * <p>If no such Vector field is defined on this object type, an
   * IllegalArgumentException will be thrown.  If such a Vector field
   * is defined on this object type but is not present in this
   * instance, false will be returned.</p>
   */

  public boolean containsFieldValue(short fieldID, Object val) throws RemoteException;

  /**
   * Server-side type casting field accessor for boolean_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a boolean_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public boolean_field getBooleanField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for BooleanDBFields.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a BooleanDBField if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean_field getBooleanField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for date_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a date_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public date_field getDateField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for date_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a date_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public date_field getDateField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for field_option_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a field_option_field if field fieldID is present and of
   * the proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public field_option_field getFieldOptionsField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for field_option_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a field_option_field if field fieldname is present and of
   * the proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public field_option_field getFieldOptionsField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for float_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a float_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public float_field getFloatField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for float_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a float_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public float_field getFloatField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for invid_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return an invid_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public invid_field getInvidField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for invid_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return an invid_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public invid_field getInvidField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for ip_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return an ip_field if field fieldID is present and of the proper
   * type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public ip_field getIPField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for ip_field.
   *
   * @param fieldname The name of the field to retrieve
   *
   * @return an ip_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public ip_field getIPField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for num_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a num_field if field fieldID is present and of the proper
   * type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public num_field getNumericField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for num_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a num_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public num_field getNumericField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for pass_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a pass_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public pass_field getPassField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for pass_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a pass_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public pass_field getPassField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for perm_field.
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a perm_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public perm_field getPermField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for perm_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a perm_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public perm_field getPermField(String fieldname) throws RemoteException;

  /**
   * Server-side type casting field accessor for string_field
   *
   * @param fieldID The field id to retrieve.
   *
   * @return a string_field if field fieldID is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public string_field getStringField(short fieldID) throws RemoteException;

  /**
   * Server-side type casting field accessor for string_field.
   *
   * @param fieldname The name of the field to retrieve.
   *
   * @return a string_field if field fieldname is present and of the
   * proper type, or null if it does not exist.
   *
   * @throw ClassCastException if the field doesn't have the
   * appropriate type.
   */

  public string_field getStringField(String fieldname) throws RemoteException;

  /**
   * <p>This method is used to provide a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.  This method is remotely callable by the client,
   * and so will only reveal fields that the user has permission
   * to view.</p>
   */

  public StringBuffer getSummaryDescription() throws RemoteException;
}
