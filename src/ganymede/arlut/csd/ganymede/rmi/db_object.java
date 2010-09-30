/*

   db_object.java

   This interface defines the methods that the client can remotely
   call on a object held in the Ganymede server.
   
   Created: 11 April 1996

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
 * <p>Remote reference to a Ganymede {@link arlut.csd.ganymede.server.DBObject DBObject}
 * or {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}, the db_object is used by the
 * client to get information about and/or make changes to a object held
 * in the Ganymede server.</p>
 *
 * @version $Id$
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
   */

  public Vector getFieldValues(String fieldName) throws RemoteException;

  /**
   * <p>Shortcut method to get a vector field's values.  Using this
   * method saves a roundtrip to the server.</p>
   */

  public Vector getFieldValues(short fieldID) throws RemoteException;

  /**
   * <p>This method is used to provide a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.  This method is remotely callable by the client,
   * and so will only reveal fields that the user has permission
   * to view.</p>
   */

  public StringBuffer getSummaryDescription() throws RemoteException;
}
