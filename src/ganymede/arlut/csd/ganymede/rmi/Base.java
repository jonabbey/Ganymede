/*

   Base.java

   Client side interface to the object type dictionary

   Created: 17 April 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                            Base

------------------------------------------------------------------------------*/

/**
 * <P>Client side interface definition for the Ganymede
 * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} class.  This
 * interface allows the client to query type information remotely, and allows
 * the schema editor in the admin console to remotely edit object type information.</P>
 *
 * <P>The {@link arlut.csd.ganymede.rmi.Category Category} interface is also vital to
 * the client and schema editor's work with object types.</P>
 *
 * @version $Id$
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
   * Returns the name of this object type.  Guaranteed
   * to be unique in the Ganymede server.
   */

  public String getName() throws RemoteException;

  /**
   * Returns the name and category path of this object type.
   * Guaranteed to be unique in the Ganymede server.
   */

  public String getPath() throws RemoteException;

  /**
   * Returns the name of the class managing this object type
   */

  public String getClassName() throws RemoteException;

  /**
   * Returns the option string for the class definition.  This option
   * string would be used if the className for this Base refers to a
   * {@link arlut.csd.ganymede.server.JythonEditObjectFactory}
   * subclass.
   */

  public String getClassOptionString() throws RemoteException;

  /**
   * Returns the invid type id for this object definition
   */

  public short getTypeID() throws RemoteException;

  /**
   * Returns the short type id for the field designated as this object's
   * primary label field.
   */

  public short getLabelField() throws RemoteException;

  /**
   * Returns the BaseField for the field designated as this object's
   * primary label field.
   */

  public BaseField getLabelFieldDef() throws RemoteException;

  /**
   * Returns the field name for the field designated as this object's
   * primary label field.
   */

  public String getLabelFieldName() throws RemoteException;

  /**
   * <p>Returns {@link arlut.csd.ganymede.rmi.BaseField BaseField}
   * base field definitions for objects of this type.
   *
   * <P>If includeBuiltIns is false, the fields returned will be the
   * custom fields defined for this object type, and they will be
   * returned in display order.  If includeBuiltIns is true, the
   * built-in fields will be appended to the Vector after the custom
   * types, in random order.</P>
   */

  public Vector<BaseField> getFields(boolean includeBuiltIns) throws RemoteException;

  /**
   * <p>This method returns a list of all
   * {@link arlut.csd.ganymede.rmi.BaseField BaseField} references for the
   * fields defined by this object type, in random order.</p>
   */

  public Vector<BaseField> getFields() throws RemoteException;

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
   * @see arlut.csd.ganymede.rmi.Base
   */

  public boolean canInactivate() throws RemoteException;

  /**
   * <p>Sets the name for this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public ReturnVal setName(String newName) throws RemoteException;

  /**
   * <p>This method is used to associate a management class with this
   * object base.</p>
   *
   * <p>The newClassName argument must be fully qualified, and must
   * refer to one of two kinds of classes.  The first is a {@link
   * arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass that
   * implements the requisite three constructors, a la the traditional
   * Ganymede customization hook.  The second is any class
   * implementing the {link arlut.csd.ganymede.common.DDPluginFactory
   * DDPluginFactory} interface, which provides a set of factory
   * methods which return DBEditObject instances.</p>
   *
   * <p>If newClassName implements DDPluginFactory, the
   * newOptionString argument will be available to the factory methods
   * so that the constructed objects can be dynamically customized.
   * This is intended to support the use of DBEditObject subclasses
   * written in Jython, with support for dynamic reloading during
   * server execution.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public ReturnVal setClassInfo(String newClassName, String newOptionString) throws RemoteException;

  /**
   * <p>This method is used to adjust the ordering of a custom field
   * in this Base.</p>
   *
   * @param fieldName The name of the field to move
   * @param previousFieldName The name of the field that fieldName is going to
   * be put after, or null if fieldName is to be the first field displayed
   * in this object type.
   */

  public ReturnVal moveFieldAfter(String fieldName, String previousFieldName) throws RemoteException;

  /**
   * <p>This method is used to adjust the ordering of a custom field
   * in this Base.</p>
   *
   * @param fieldName The name of the field to move
   * @param nextFieldName The name of the field that fieldName is going to
   * be put before, or null if fieldName is to be the last field displayed
   * in this object type.
   */

  public ReturnVal moveFieldBefore(String fieldName, String nextFieldName) throws RemoteException;

  /**
   * <p>Sets the required label field for this Base by field name.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public ReturnVal setLabelField(String fieldName) throws RemoteException;

  /**
   * <p>Sets the required label field for this Base by numeric field id.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public ReturnVal setLabelField(short fieldID) throws RemoteException;

  /**
   * <p>Get the parent Category for this object type.  This is used by the
   * Ganymede client and schema editor to present object types in
   * a hierarchical tree.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public Category getCategory() throws RemoteException;

  /**
   * <p>Creates a new base field and inserts it
   * into the DBObjectBase field definitions hash.  The newly created
   * field will the first short id code value available at or above
   * 256.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   */

  public BaseField createNewField() throws RemoteException;

  /**
   * <p>This method is used to remove a field definition from
   * the current schema.</p>
   *
   * <p>Of course, this removal will only take effect if
   * the schema editor commits.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public ReturnVal deleteField(String fieldName) throws RemoteException;

  /**
   * This method is used by the SchemaEditor to detect whether any
   * objects are using a field definition.  If any objects of this
   * type in the database have this field defined, this method
   * will return true.
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public boolean fieldInUse(String fieldName) throws RemoteException;
}
