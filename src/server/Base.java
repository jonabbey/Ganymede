/*

   Base.java

   Client side interface to the object type dictionary
   
   Created: 17 April 1997
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 1999/03/24 03:29:48 $
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

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                            Base

------------------------------------------------------------------------------*/

/**
 *
 * <p>Client side interface definition for the Ganymede DBObjectBase class.  This
 * interface allows the client to query type information remotely, and allows
 * the schema editor in the admin console to remotely edit object type information.</p>
 *
 * <p>The {@link arlut.csd.ganymede.Category Category} interface is also vital to
 * the client and schema editor's work with object types.</p>
 *
 * @version $Revision: 1.15 $ $Date: 1999/03/24 03:29:48 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public interface Base extends CategoryNode, Remote {

  /**
   * <p>This method indicates whether this base may be removed in
   * the Schema Editor.</p>
   *
   * <p>We don't allow removal of built-in Bases that the server
   * depends on for its operation, such as permissions, notification,
   * and logging object types.</p>
   */

  public boolean isRemovable() throws RemoteException;

  /**
   * This method returns true if this object base is for
   * an embedded object.  Embedded objects do not have
   * their own expiration and removal dates, do not have
   * history trails, and can be only owned by a single
   * object, not by a list of administrators.
   */

  public boolean isEmbedded() throws RemoteException;

  /**
   * Returns the name of this object type
   */

  public String getName() throws RemoteException;

  /**
   * Returns the name of the class managing this object type
   */

  public String getClassName() throws RemoteException;

  /**
   * Returns the invid type id for this object definition
   */

  public short getTypeID() throws RemoteException;

  /**
   * Returns the short type id for the field designated as this object's
   * primary label field, if any.  Objects do not need to have a primary
   * label field designated if labels for this object type are dynamically
   * generated.
   */

  public short getLabelField() throws RemoteException;

  /**
   * Returns the field name for the field designated as this object's
   * primary label field.  null is returned if no label has been
   * designated.
   */

  public String getLabelFieldName() throws RemoteException;

  /**
   * <p>This method returns a list of 
   * {@link arlut.csd.ganymede.BaseField BaseField} references for the
   * fields defined by this object type.</p>
   * 
   * <p>If includeBuiltIns is false, common fields (such
   * as last modification date, etc.) will be not be included in the
   * list.  The list is sorted by field display order.</p>
   */

  public Vector getFields(boolean includeBuiltIns) throws RemoteException;

  /**
   * <p>This method returns a list of 
   * {@link arlut.csd.ganymede.BaseField BaseField} references for the
   * fields defined by this object type.</p>
   */

  public Vector getFields() throws RemoteException;

  /**
   * Returns the field definition for the field matching id,
   * or null if no match found.
   */

  public BaseField getField(short id) throws RemoteException;

  /**
   * Returns the field definition for the field matching name,
   * or null if no match found.
   */

  public BaseField getField(String name) throws RemoteException;

  /**
   * Returns true if the current session is permitted to
   * create an object of this type.
   */

  public boolean canCreate(Session session) throws RemoteException;

  /**
   *
   * Returns true if this object type can be inactivated.  Not all
   * object types will have inactivation protocols defined in the
   * server.  Those that do not can just be deleted.
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean canInactivate() throws RemoteException;

  /**
   * <p>Sets the name for this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public boolean setName(String newName) throws RemoteException;

  /**
   * <p>Sets the fully qualified classname of the class 
   * managing this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public void setClassName(String newName) throws RemoteException;

  /**
   * <p>Choose what field will serve as this objectBase's label.  A fieldName
   * parameter of null will cause the object's label field to be undefined,
   * in which case the object will have to generate its own label using the
   * {@link arlut.csd.ganymede.DBEditObject.getLabelHook(arlut.csd.ganymede.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public void setLabelField(String fieldName) throws RemoteException;

  /**
   * <p>Choose what field will serve as this objectBase's label.  A fieldID
   * parameter of -1 will cause the object's label field to be undefined,
   * in which case the object will have to generate its own label using the
   * {@link arlut.csd.ganymede.DBEditObject.getLabelHook(arlut.csd.ganymede.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public void setLabelField(short fieldID) throws RemoteException;

  /**
   * <p>Get the parent Category for this object type.  This is used by the
   * Ganymede client and schema editor to present object types in
   * a hierarchical tree.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */
 
  public Category getCategory() throws RemoteException;

  /**
   * <p>Creates a new base field and inserts it
   * into the DBObjectBase field definitions hash.</p>
   *
   * <p>If lowRange is true, the field's id will start at 100 and go up,
   * other wise it will start at 256 and go up.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public BaseField createNewField(boolean lowRange) throws RemoteException;

  /**
   * <p>This method is used to remove a field definition from 
   * the current schema.</p>
   *
   * <p>Of course, this removal will only take effect if
   * the schema editor commits.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean deleteField(BaseField bF) throws RemoteException;

  /**
   * This method is used by the SchemaEditor to detect whether any
   * objects are using a field definition.  If any objects of this
   * type in the database have this field defined, this method
   * will return true.
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean fieldInUse(BaseField bF) throws RemoteException;
}
