/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

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

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.*;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.db_field;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/

/**
 * <p>This abstract base class encapsulates the basic logic for fields in the
 * Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore},
 * including permissions and unique value handling.</p>
 *
 * <p>DBFields are the actual carriers of field value in the Ganymede
 * server.  Each {@link arlut.csd.ganymede.server.DBObject DBObject} holds a
 * set of DBFields in an array.  Each DBField is associated with a {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} field
 * definition (see {@link arlut.csd.ganymede.server.DBField#getFieldDef()
 * getFieldDef()}) by way of its owner's type and it's own field code,
 * which defines the type of the field as well as various generic and
 * type-specific attributes for the field.  The DBObjectBaseField
 * information is created and edited with the Ganymede schema
 * editor.</p>
 *
 * <p>DBField is an abstract class.  There is a different subclass of DBField
 * for each kind of data that can be held in the Ganymede server, as follows:</p>
 *
 * <ul>
 * <li>{@link arlut.csd.ganymede.server.StringDBField StringDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.BooleanDBField BooleanDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.NumericDBField NumericDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.FloatDBField FloatDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.DateDBField DateDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.InvidDBField InvidDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.IPDBField IPDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.PasswordDBField PasswordDBField}</li>
 * <li>{@link arlut.csd.ganymede.server.PermissionMatrixDBField PermissionMatrixDBField}</li>
 * </ul>
 *
 * <p>Each DBField subclass is responsible for writing itself to disk
 * on command with the {@link
 * arlut.csd.ganymede.server.DBField#emit(java.io.DataOutput) emit()} method,
 * and reading its state in with the {@link
 * arlut.csd.ganymede.server.DBField#receive(java.io.DataInput, arlut.csd.ganymede.server.DBObjectBaseField) receive()}
 * method.  Each DBField subclass may also have extensive special
 * logic to handle special operations on fields of the appropriate
 * type.  For instance, the InvidDBField class has lots and lots of
 * logic for handling the bi-directional object linking that the
 * server depends on for its object handling.  Mostly the DBField
 * subclasses provide customization that modifies how things like
 * {@link arlut.csd.ganymede.server.DBField#setValue(java.lang.Object)
 * setValue()} and {@link arlut.csd.ganymede.server.DBField#getValue()
 * getValue()} work, but PasswordDBField and PermissionMatrixDBField
 * don't fit with the standard generic value-container model, and
 * contain their own methods for manipulating and accessing data held
 * in the Ganymede database. Most DBField subclasses only allow a
 * single value to be held, but StringDBField, InvidDBField, and
 * IPDBField support vectors of values.</p>
 *
 * <p>The Ganymede client can directly access fields in RMI-published
 * objects using the {@link arlut.csd.ganymede.rmi.db_field db_field} RMI
 * interface.  Each concrete subclass of DBField has its own special
 * RMI interface which provides special methods for the client.
 * Adding a new data type to the Ganymede server will involve creating
 * a new DBField subclass, as well as a new RMI interface for any
 * special field methods.  All client code would also need to be
 * modified to be aware of the new field type.  DBObjectBaseField,
 * DBEditObject and DBObject would also need to be modified to be
 * aware of the new field type for schema editing, customization, and object loading.
 * The schema editor would have to be modified as well.</p>
 *
 * <p>But you can do it if you absolutely have to.  Just be careful and take a good
 * look around at the code.</p>
 *
 * <p>Note that while DBField was designed to be subclassed, it should only be
 * necessary for adding a new data type to the server.  All other likely
 * customizations you'd want to do are handled by
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} customization methods.  Most
 * DBField methods at some point call methods on the DBObject/DBEditObject
 * that contains it.  All methods that cause changes to fields call out to
 * finalizeXYZ() and/or wizardHook() methods in DBEditObject.  Consult the
 * DBEditObject customization guide for details on the field/object interactions.</p>
 *
 * <p>An important note about synchronization: it is possible to encounter a
 * condition called a <b>nested monitor deadlock</b>, where a synchronized
 * method on a field can block trying to enter a synchronized method on
 * a {@link arlut.csd.ganymede.server.DBSession DBSession},
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}, or
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} object that is itself blocked
 * on another thread trying to call a synchronized method on the same field.</p>
 *
 * <p>To avoid this condition, no field methods that call synchronized methods on
 * other objects should themselves be synchronized in any fashion.</p>
 */

public abstract class DBField implements Remote, db_field, FieldType, Comparable {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBField");

  /**
   * Counter field we use to display loading statistics at server start time.
   */

  static public int fieldCount = 0;

  /**
   * <p>This method acts as a factory class to create a typed DBField
   * subclass and attach it to a DBObject.</p>
   *
   * <p>Used by the DBEditObject's create object and check-out constructors.</p>
   */

  static DBField createTypedField(DBObject object, DBObjectBaseField fieldDef)
  {
    switch (fieldDef.getType())
      {
      case BOOLEAN:
        return new BooleanDBField(object, fieldDef);

      case NUMERIC:
        return new NumericDBField(object, fieldDef);

      case FLOAT:
        return new FloatDBField(object, fieldDef);

      case FIELDOPTIONS:
        return new FieldOptionDBField(object, fieldDef);

      case DATE:
        return new DateDBField(object, fieldDef);

      case STRING:
        return new StringDBField(object, fieldDef);

      case INVID:
        return new InvidDBField(object, fieldDef);

      case PERMISSIONMATRIX:
        return new PermissionMatrixDBField(object, fieldDef);

      case PASSWORD:
        return new PasswordDBField(object, fieldDef);

      case IP:
        return new IPDBField(object, fieldDef);

      default:
        throw new IllegalArgumentException("Bad field def type in DBField.createTypedField:" + fieldDef.getType());
      }
  }

  /**
   * <p>This method acts as a factory class to copy a DBField subclass
   * and attach it to a DBObject, using the appropriate DBField
   * subclass' copy constructor.</p>
   *
   * <p>Used by the DBEditObject's check-out and check-in constructor,
   * but not by object cloning, which is creating a new object whose
   * fields are being copied from the old object as if a user was
   * doing it manually.  This copyField method, by contrast, will only
   * do a dumb data copy, and will not fix up and InvidDBField
   * bindings, etc.</p>
   *
   * <p>Note that it is essential that this method never throw an
   * uncaught exception, because that will break commits in a very
   * ugly way.</p>
   */

  static DBField copyField(DBObject object, DBField orig)
  {
    switch (orig.getType())
      {
      case BOOLEAN:
        return new BooleanDBField(object, (BooleanDBField) orig);

      case NUMERIC:
        return new NumericDBField(object, (NumericDBField) orig);

      case FLOAT:
        return new FloatDBField(object, (FloatDBField) orig);

      case FIELDOPTIONS:
        return new FieldOptionDBField(object, (FieldOptionDBField) orig);

      case DATE:
        return new DateDBField(object, (DateDBField) orig);

      case STRING:
        return new StringDBField(object, (StringDBField) orig);

      case INVID:
        return new InvidDBField(object, (InvidDBField) orig);

      case PERMISSIONMATRIX:
        return new PermissionMatrixDBField(object, (PermissionMatrixDBField) orig);

      case PASSWORD:
        return new PasswordDBField(object, (PasswordDBField) orig);

      case IP:
        return new IPDBField(object, (IPDBField) orig);

      default:
        throw new IllegalArgumentException("Bad field def type in DBField.copyField:" + orig.getType());
      }
  }

  /**
   * <p>This method is used to handle creating new objects from the
   * ganymede.db input stream when we are loading the database from
   * disk.</p>
   *
   * <p>Again, effectively used to map type constants to classes.</p>
   */

  static DBField readField(DBObject object, DataInput in, DBObjectBaseField definition) throws IOException
  {
    DBField.fieldCount++;

    switch (definition.getType())
      {
      case BOOLEAN:
        return new BooleanDBField(object, in, definition);

      case NUMERIC:
        return new NumericDBField(object, in, definition);

      case FLOAT:
        return new FloatDBField(object, in, definition);

      case FIELDOPTIONS:
        return new FieldOptionDBField(object, in, definition);

      case DATE:
        return new DateDBField(object, in, definition);

      case STRING:
        return new StringDBField(object, in, definition);

      case INVID:
        return new InvidDBField(object, in, definition);

      case PERMISSIONMATRIX:
        return new PermissionMatrixDBField(object, in, definition);

      case PASSWORD:
        return new PasswordDBField(object, in, definition);

      case IP:
        return new IPDBField(object, in, definition);

      default:
        throw new IllegalArgumentException("Bad field def type in DBField.readField:" + definition.getType());
      }
  }

  /**
   * <p>Returns true if both field1 and field2 are non-null and belong
   * to the same Invid and share the same field id.</p>
   */

  public static boolean matches(DBField field1, DBField field2)
  {
    if (field1 == null || field2 == null)
      {
        return false;
      }

    return field1.matches(field2);
  }

  // ---

  /**
   * <p>The object's current value.  May be a Vector for vector
   * fields, in which case getVectVal() may be used to perform the
   * cast.</p>
   *
   * <p>package private</p>
   */

  Object value = null;

  /**
   * The object this field is contained within.
   */

  final DBObject owner;

  /**
   * The identifying field number for this field within the owning
   * object.  This number is an index into the owning object type's
   * field dictionary.
   */

  final short fieldcode;

  /* -- */

  public DBField(DBObject owner, short fieldcode)
  {
    this.owner = owner;
    this.fieldcode = fieldcode;
  }

  /**
   * <p>This method is used to return a copy of this field, with the
   * field's owner set to newOwner.</p>
   */

  public final DBField getCopy(DBObject newOwner)
  {
    return DBField.copyField(newOwner, this);
  }

  /**
   * <p>Returns the DBObject that this field is contained within.</p>
   *
   * <p>This method is duplicative of getOwner(), but we are keeping
   * both around for backwards compatibility.</p>
   */

  public final DBObject getObject()
  {
    return owner;
  }

  /**
   * <p>This method is designed to handle casting this field's value into
   * a vector as needed.  We don't bother to check whether value is a Vector
   * here, as the code which would have used the old values field should
   * do that for us themselves.</p>
   *
   * <p>This method does no permissions checking at all, and should only
   * be used from within DBField and subclass code.  For other purposes,
   * use getValuesLocal().</p>
   *
   * <p>This method should always return a valid Vector if this field
   * is truly a vector field, as we don't keep empty vector fields in
   * non-editable objects, and if this is an editable object we'll
   * have created a vector when this field was initialized for
   * editing.</p>
   *
   * <p>Note that this method gives direct access to the value vector.
   * Any modifications made to the returned vector will affect the
   * value held in this field.</p>
   */

  protected final Vector getVectVal()
  {
    return (Vector) value;
  }

  /**
   * <p>This method implements the Comparable interface.</p>
   *
   * <p>We are comparable in terms of the field id number for this field.</p>
   *
   * <p>The o parameter can be a Short, a short (using Java 5
   * autoboxing), or another DBField.</p>
   */

  public int compareTo(Object o)
  {
    if (o instanceof Number)
      {
        return fieldcode - ((Number) o).shortValue();
      }
    else
      {
        DBField otherField = (DBField) o;

        return fieldcode - otherField.fieldcode;
      }
  }

  /**
   *
   * Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   *
   */

  public Object key()
  {
    if (isVector())
      {
        throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    return value;
  }

  /**
   *
   * Object value of a vector DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   *
   */

  public Object key(int index)
  {
    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    return getVectVal().elementAt(index);
  }

  /**
   * Returns number of elements in vector if this is a vector field.  If
   * this is not a vector field, will return 1. (Should throw exception?)
   */

  public int size()             // returns number of elements in array
  {
    if (!isVector())
      {
        return 1;
      }
    else
      {
        return getVectVal().size();
      }
  }

  /**
   *
   * Returns the maximum length of an array in this field type
   *
   */

  public int getMaxArraySize()
  {
    if (!isVector())
      {
        return 1;               // should throw exception?
      }
    else
      {
        return getFieldDef().getMaxArraySize();
      }
  }

  /**
   * <p>This method is responsible for writing out the contents of
   * this field to an binary output stream.  It is used in writing
   * fields to the ganymede.db file and to the journal file.</p>
   *
   * <p>This method only writes out the value contents of this field.
   * The {@link arlut.csd.ganymede.server.DBObject DBObject}
   * {@link arlut.csd.ganymede.server.DBObject#emit(java.io.DataOutput) emit()}
   * method is responsible for writing out the field identifier information
   * ahead of the field's contents.</p>
   */

  abstract void emit(DataOutput out) throws IOException;

  /**
   * <p>This method is responsible for reading in the contents of
   * this field from an binary input stream.  It is used in reading
   * fields from the ganymede.db file and from the journal file.</p>
   *
   * <p>The code that calls receive() on this field is responsible for
   * having read enough of the binary input stream's context to
   * place the read cursor at the point in the file immediately after
   * the field's id and type information has been read.</p>
   */

  abstract void receive(DataInput in, DBObjectBaseField definition) throws IOException;

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.
   */

  abstract void emitXML(XMLDumpContext dump) throws IOException;

  /**
   * We don't expect these fields to ever be stored in a hash.
   */

  public int hashCode()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Returns true if obj is a field with the same value(s) as
   * this one.</p>
   *
   * <p>This method is ok to be synchronized because it does not call
   * synchronized methods on any other object that is likely to have
   * another thread trying to call another synchronized method on
   * us.</p>
   */

  public synchronized boolean equals(Object obj)
  {
    if (obj == null)
      {
        return false;
      }

    if (!(obj.getClass().equals(this.getClass())))
      {
        return false;
      }

    DBField f = (DBField) obj;

    if (!isVector())
      {
        return f.key().equals(this.key());
      }
    else
      {
        if (f.size() != this.size())
          {
            return false;
          }

        for (int i = 0; i < size(); i++)
          {
            if (!f.key(i).equals(this.key(i)))
              {
                return false;
              }
          }

        return true;
      }
  }

  /**
   * <p>Returns true if otherField has the same invid and field id as
   * this field.</p>
   */

  public boolean matches(DBField otherField)
  {
    return otherField != null &&
      otherField.getOwner().getInvid().equals(getOwner().getInvid()) &&
      otherField.getID() == getID();
  }

  /**
   * <p>This method copies the current value of this DBField to
   * target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.</p>
   *
   * <p>This includes most particularly the InvidDBField bind
   * logic.</p>
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   *
   * @return A ReturnVal indicating success or failure.  May be simply
   * 'null' to indicate success if no feedback need be provided.
   */

  public synchronized ReturnVal copyFieldTo(DBField target, boolean local)
  {
    if (!local)
      {
        if (!verifyReadPermission())
          {
            // "Copy field error"
            // "Can''t copy from field {0} in object {1}, due to a lack of read privileges."
            return Ganymede.createErrorDialog(ts.l("copyFieldTo.copy_error_sub"),
                                              ts.l("copyFieldTo.no_read", getName(), owner.getLabel()));
          }
      }

    if (!target.isEditable(local))
      {
        // "Copy field error"
        // "Can''t copy to field {0} in object {1}, due to a lack of write privileges."
        return Ganymede.createErrorDialog(ts.l("copyFieldTo.copy_error_sub"),
                                          ts.l("copyFieldTo.no_write",
                                               target.getName(), target.owner.getLabel()));
      }

    if (!isVector())
      {
        return target.setValueLocal(getValueLocal(), true); // inhibit wizards..
      }
    else
      {
        Vector valuesToCopy;

        /* -- */

        valuesToCopy = getVectVal();

        if (valuesToCopy == null || valuesToCopy.size() == 0)
          {
            return null;
          }

        // We want to inhibit wizards and allow partial failure.
        //
        // We'll use addElementsLocal() here because we've already
        // verified read permission and write permission, above.

        // This could fail if we don't have write privileges for the
        // target field, so we'll return an error code back to the
        // cloneFromObject() method, which will pass it in an over-all
        // advisory (non-fatal) warning back to the client

        return target.addElementsLocal(valuesToCopy, true, true);
      }
  }

  /**
   * <p>This method is intended to run a consistency check on the
   * contents of this field against the constraints specified in the
   * {@link arlut.csd.ganymede.server.DBObjectBaseField} controlling
   * this field.</p>
   *
   * <p>Returns a {@link arlut.csd.ganymede.common.ReturnVal}
   * describing the error if the field's contents does not meet its
   * constraints, or null if the field is in compliance with its
   * constraints.</p>
   */

  public ReturnVal validateContents()
  {
    if (!isVector())
      {
        return this.verifyBasicConstraints(this.value);
      }
    else
      {
        if (isVector())
          {
            Vector values = (Vector) this.value;

            synchronized (values)
              {
                for (Object element: values)
                  {
                    ReturnVal retVal = this.verifyBasicConstraints(element);

                    if (!ReturnVal.didSucceed(retVal))
                      {
                        return retVal;
                      }
                  }
              }

            if (size() > getMaxArraySize())
              {
                // "Field {0} in object {1} contains more elements ({2,number,#}) than is allowed ({3,number,#})."
                return Ganymede.createErrorDialog(ts.l("validateContents.too_big_array",
                                                       this.getName(),
                                                       owner.getLabel(),
                                                       Integer.valueOf(size()),
                                                       Integer.valueOf(getMaxArraySize())));
              }
          }
      }

    return null;
  }

  /**
   * <p>This method is intended to be called when this field is being
   * checked into the database.  Subclasses of DBField will override
   * this method to clean up data that is cached for speed during
   * editing.</p>
   */

  public void cleanup()
  {
  }

  /**
   * <p>Returns the DBSession that this field is associated with or null
   * if it is being viewed from the persistent store.</p>
   *
   * @deprecated Use {@link #getDBSession()} instead.
   */

  @Deprecated
  public final DBSession getSession()
  {
    return this.getDBSession();
  }

  /**
   * <p>Returns the DBSession that this field is associated with or null
   * if it is being viewed from the persistent store.</p>
   */

  public final DBSession getDBSession()
  {
    try
      {
        return owner.getDBSession();
      }
    catch (NullPointerException ex)
      {
        return null;
      }
  }

  /**
   * Returns the GanymedeSession that this field is associated with,
   * or null if it is being viewed from a naked DBSession or directly
   * from the persistent store.
   */

  public final GanymedeSession getGSession()
  {
    try
      {
        return getDBSession().getGSession();
      }
    catch (NullPointerException ex)
      {
        return null;
      }
  }

  // ****
  //
  // db_field methods
  //
  // ****

  /**
   *
   * Returns a handy field description packet for this field,
   * containing the static field elements for this field..
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final FieldTemplate getFieldTemplate()
  {
    return getFieldDef().getTemplate();
  }

  /**
   *
   * Returns a handy field description packet for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final FieldInfo getFieldInfo() throws GanyPermissionsException
  {
    return new FieldInfo(this);
  }

  /**
   *
   * Returns the schema name for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final String getName()
  {
    return getFieldDef().getName();
  }

  /**
   * Returns the name for this field, encoded
   * in a form suitable for use as an XML element
   * name.
   */

  public final String getXMLName()
  {
    return arlut.csd.Util.XMLUtils.XMLEncode(getFieldDef().getName());
  }

  /**
   *
   * Returns the field # for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final short getID()
  {
    return fieldcode;
  }

  /**
   * <p>Returns the object this field is part of.</p>
   *
   * <p>This method is duplicative of getObject(), but we are keeping
   * both around for backwards compatibility.</p>
   */

  public final DBObject getOwner()
  {
    return owner;
  }

  /**
   *
   * Returns the description of this field from the
   * schema.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final String getComment()
  {
    return getFieldDef().getComment();
  }

  /**
   *
   * Returns the description of this field's type from
   * the schema.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final String getTypeDesc()
  {
    return getFieldDef().getTypeDesc();
  }

  /**
   *
   * Returns the type code for this field from the
   * schema.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public final short getType()
  {
    return getFieldDef().getType();
  }

  /**
   * <p>This method returns a text encoded value for this DBField
   * without checking permissions.</p>
   *
   * <p>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the {@link
   * arlut.csd.ganymede.server.DBObject#getLabel() getLabel()} logic
   * for {@link arlut.csd.ganymede.server.DBObject DBObject}.</p>
   *
   * <p>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</p>
   */

  abstract public String getValueString();

  /**
   * Returns a String representing a reversible encoding of the
   * value of this field.  Each field type will have its own encoding,
   * suitable for embedding in a {@link arlut.csd.ganymede.common.DumpResult DumpResult}.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  abstract public String getEncodingString();

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  abstract public String getDiffString(DBField orig);

  /**
   * This method returns true if this field is owned by an editable
   * object and its contents differ from the same field in the
   * DBEditObject's original object.  If this field belongs to a newly
   * created DBEditObject, hasChanged() will always return true.
   */

  public boolean hasChanged()
  {
    if (!(getOwner() instanceof DBEditObject))
      {
        return false;
      }

    DBObject orig = ((DBEditObject) getOwner()).getOriginal();

    if (orig == null)
      {
        return true;
      }

    return hasChanged((DBField) orig.getField(getID()));
  }

  /**
   * This method returns true if this field differs from the orig.
   * It is intended to do a quick before/after comparison when we are
   * handling a transaction commit.
   */

  public boolean hasChanged(DBField orig)
  {
    if (orig == null)
      {
        return true;
      }

    if (!(orig.getClass().equals(this.getClass())))
      {
        throw new IllegalArgumentException("bad field comparison");
      }

    return (!this.equals(orig));
  }

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public synchronized boolean isDefined()
  {
    if (isVector())
      {
        Vector values = getVectVal();

        if (values != null && values.size() > 0)
          {
            return true;
          }
        else
          {
            return false;
          }
      }
    else
      {
        if (value != null)
          {
            return true;
          }
        else
          {
            return false;
          }
      }
  }

  /**
   * <p>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.</p>
   *
   * <p>Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.</p>
   *
   * <p>NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal setUndefined(boolean local) throws GanyPermissionsException
  {
    if (isVector())
      {
        if (!isEditable(local)) // *sync* GanymedeSession possible
          {
            // "DBField.setUndefined(): couldn''t clear vector elements from field {0} in object {1}, due to a lack of write permissions."
            throw new GanyPermissionsException(ts.l("setUndefined.no_perm_vect", getName(), owner.getLabel()));
          }

        // we have to clone our values Vector in order to use
        // deleteElements().

        Vector currentValues = new Vector(getVectVal());

        if (currentValues.size() != 0)
          {
            return deleteElementsLocal(currentValues);
          }
        else
          {
            return null;        // success
          }
      }
    else
      {
        return setValue(null, local, false);
      }
  }

  /**
   *
   * Returns true if this field is a vector, false
   * otherwise.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isVector()
  {
    return getFieldDef().isArray();
  }

  /**
   * <p>Returns true if this field is editable, false
   * otherwise.</p>
   *
   * <p>Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isEditable()
  {
    return isEditable(false);
  }

  /**
   * <p>Returns true if this field is editable, false
   * otherwise.</p>
   *
   * <p>Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p><b>*Deadlock Hazard.*</b></p>>
   *
   * @param local If true, skip permissions checking
   *
   */

  public final boolean isEditable(boolean local)
  {
    DBEditObject eObj;

    /* -- */

    if (!(owner instanceof DBEditObject))
      {
        return false;
      }

    eObj = (DBEditObject) owner;

    // if our owner has already started the commit process, we can't
    // allow any changes, local access or no

    if (eObj.isCommitting())
      {
        return false;
      }

    if (!local && !verifyWritePermission()) // *sync* possible on GanymedeSession
      {
        return false;
      }

    return true;
  }

  /**
   * This method returns true if this field is one of the
   * system fields present in all objects.
   */

  public final boolean isBuiltIn()
  {
    return getFieldDef().isBuiltIn();
  }

  /**
   *
   * Returns true if this field should be displayed in the
   * current client context.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isVisible()
  {
    return verifyReadPermission() &&
      getFieldDef().base().getObjectHook().canSeeField(null, this);
  }

  /**
   *
   * Returns true if this field is edit in place.
   *
   */

  public final boolean isEditInPlace()
  {
    return getFieldDef().isEditInPlace();
  }

  /**
   *
   * Returns the object type id for the object containing this field.
   *
   */

  public final int getObjTypeID()
  {
    return getFieldDef().base().getTypeID();
  }

  /**
   *
   * Returns the DBNameSpace that this field is associated with, or
   * null if no NameSpace field is associated with this field.
   *
   */

  public final DBNameSpace getNameSpace()
  {
    return getFieldDef().getNameSpace();
  }

  /**
   *
   * Returns the DBObjectBaseField for this field.
   *
   */

  public final DBObjectBaseField getFieldDef()
  {
    return owner.getFieldDef(fieldcode);
  }

  /**
   *
   * This version of getFieldDef() is intended for use by code
   * sections that need to interrogate a field's type definition
   * before it is linked to an owner object.
   *
   */

  public final DBObjectBaseField getFieldDef(short objectType)
  {
    DBObjectBase base = Ganymede.db.getObjectBase(objectType);

    if (base == null)
      {
        return null;
      }

    return (DBObjectBaseField) base.getField(fieldcode);
  }

  /**
   *
   * This version of getFieldDef() is intended for use by code
   * sections that need to interrogate a field's type definition
   * before it is linked to an owner object.
   *
   */

  public final DBObjectBaseField getFieldDef(DBObjectBase base)
  {
    if (base == null)
      {
        return null;
      }

    return (DBObjectBaseField) base.getField(fieldcode);
  }

  /**
   * <p>Returns the value of this field, if a scalar.  An
   * IllegalArgumentException will be thrown if this field is a
   * vector.</p>
   *
   * <p>This method will throw a GanyPermissionsException if this
   * DBObject is being viewed by a GanymedeSession, and that
   * GanymedeSession lacks appropriate permission to see the
   * value.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public Object getValue() throws GanyPermissionsException
  {
    if (!verifyReadPermission())
      {
        // "Don''t have permission to read field {0} in object {1}"
        throw new GanyPermissionsException(ts.l("global.no_read_perms", getName(), owner.getLabel()));
      }

    if (isVector())
      {
        // "Scalar method called on a vector field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    return value;
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * <p>This method is intended to be called by code that needs to go
   * through the permission checking regime, and that needs to have
   * rescan information passed back.  This includes most wizard
   * setValue calls.</p>
   *
   * @see arlut.csd.ganymede.server.DBSession
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal setValue(Object value) throws GanyPermissionsException
  {
    ReturnVal result;

    /* -- */

    // do the thing, calling into our subclass

    result = setValue(value, false, false);

    if (ReturnVal.hasTransformedValue(result))
      {
        result = rescanThisField(result);
      }

    return result;
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p><b>This method is server-side only, and bypasses
   * permissions checking.</b></p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal setValueLocal(Object value)
  {
    try
      {
        return setValue(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex);
      }
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p><b>This method is server-side only, and bypasses permissions
   * checking.</b></p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * @param value Value to set this field to
   * @param noWizards If true, wizards will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal setValueLocal(Object value, boolean noWizards)
  {
    try
      {
        return setValue(value, true, noWizards);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex);
      }
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p><b>This method is server-side only.</b></p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.</p>
   *
   * @param submittedValue Value to set this field to
   * @param local If true, permissions checking will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal setValue(Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return setValue(submittedValue, local, false);
  }

  /**
   * <p>Sets the value of this field, if a scalar.</p>
   *
   * <p><b>This method is server-side only.</b></p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.</p>
   *
   * <p>This method will be overridden by DBField subclasses with special
   * needs.</p>
   *
   * @param submittedValue Value to set this field to
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal setValue(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))     // *sync* possible
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (isVector())
      {
        // "Scalar method called on a vector field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    if (this.value == submittedValue || (this.value != null && this.value.equals(submittedValue)))
      {
        return null;            // no change (useful for null and for xmlclient)
      }

    if (submittedValue instanceof String)
      {
        submittedValue = ((String) submittedValue).intern();
      }
    else if (submittedValue instanceof Invid)
      {
        submittedValue = ((Invid) submittedValue).intern();
      }
    else if (submittedValue instanceof Date)
      {
        submittedValue = new Date(((Date) submittedValue).getTime()); // defensive copy
      }

    retVal = verifyNewValue(submittedValue);

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    /* check to see if verifyNewValue canonicalized the submittedValue */

    if (ReturnVal.hasTransformedValue(retVal))
      {
        submittedValue = retVal.getTransformedValueObject();
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = ReturnVal.merge(retVal, eObj.wizardHook(this, DBEditObject.SETVAL, submittedValue, null));

        // if a wizard intercedes, we are going to let it take the
        // ball.  we'll lose any transformation/rescan from the
        // verifyNewValue() call above, but the fact that the wizard
        // is taking over means that we're not directly accepting
        // whatever the user gave us, anyway.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
        unmark(this.value);

        // if we're not being told to clear this field, try to mark the
        // new value

        if (submittedValue != null)
          {
            if (!mark(submittedValue))
              {
                if (this.value != null)
                  {
                    mark(this.value); // we aren't clearing the old value after all
                  }

                return getConflictDialog("DBField.setValue()", submittedValue);
              }
          }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    retVal = ReturnVal.merge(retVal, eObj.finalizeSetValue(this, submittedValue));

    if (ReturnVal.didSucceed(retVal))
      {
        this.value = submittedValue;
      }
    else
      {
        // our owner disapproved of the operation,
        // undo the namespace manipulations, if any,
        // and finish up.

        if (ns != null)
          {
            unmark(submittedValue);
            mark(this.value);
          }
      }

    // go ahead and return the dialog that was set by finalizeSetValue().

    return retVal;
  }

  /**
   * <p>Returns a Vector of the values of the elements in this field,
   * if a vector.</p>
   *
   * <p>This is only valid for vectors.  If the field is a scalar, use
   * getValue().</p>
   *
   * <p>This method checks for read permissions.</p>
   *
   * <p>This method returns a safe copy of the contained Vector.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public Vector getValues() throws GanyPermissionsException
  {
    if (!verifyReadPermission())
      {
        // "Don''t have permission to read field {0} in object {1}"
        throw new GanyPermissionsException(ts.l("global.no_read_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    return new Vector(getVectVal()); // defensive copy
  }

  /**
   * Returns the value of an element of this field,
   * if a vector.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public Object getElement(int index) throws GanyPermissionsException
  {
    if (!verifyReadPermission())
      {
        // "Don''t have permission to read field {0} in object {1}"
        throw new GanyPermissionsException(ts.l("global.no_read_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (index < 0)
      {
        // "Invalid index {0,num,#} for array access on field {0} in object {1}."
        throw new ArrayIndexOutOfBoundsException(ts.l("global.out_of_range",
                                                      Integer.valueOf(index),
                                                      getName(),
                                                      owner.getLabel()));
      }

    return getVectVal().elementAt(index);
  }

  /**
   * Returns the value of an element of this field,
   * if a vector.
   */

  public Object getElementLocal(int index)
  {
    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (index < 0)
      {
        // "Invalid index {0,num,#} for array access on field {0} in object {1}."
        throw new ArrayIndexOutOfBoundsException(ts.l("global.out_of_range",
                                                      Integer.valueOf(index),
                                                      getName(),
                                                      owner.getLabel()));
      }

    return getVectVal().elementAt(index);
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <P>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful setElement will
   * encode an order to rescan this field.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.server.DBSession
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal setElement(int index, Object value) throws GanyPermissionsException
  {
    if (!isVector())
      {
        throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (value == null)
      {
        return Ganymede.createErrorDialog("Field Error",
                                          "Null value passed to " + owner.getLabel() + ":" +
                                          getName() + ".setElement()");
      }

    if ((index < 0) || (index > getVectVal().size()))
      {
        // "Invalid index {0,num,#} for array access on field {0} in object {1}."
        throw new ArrayIndexOutOfBoundsException(ts.l("global.out_of_range",
                                                      Integer.valueOf(index),
                                                      getName(),
                                                      owner.getLabel()));
      }

    ReturnVal result = setElement(index, value, false, false);

    if (ReturnVal.hasTransformedValue(result))
      {
        result = rescanThisField(result);
      }

    return result;
  }

  /**
   *
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.server.DBSession
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal setElementLocal(int index, Object value)
  {
    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (value == null)
      {
        // "Null value passed to setElement() on field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("setElementLocal.bad_null", getName(), owner.getLabel()));
      }

    if ((index < 0) || (index > getVectVal().size()))
      {
        // "Invalid index {0,num,#} for array access on field {0} in object {1}."
        throw new ArrayIndexOutOfBoundsException(ts.l("global.out_of_range",
                                                      Integer.valueOf(index),
                                                      getName(),
                                                      owner.getLabel()));
      }

    try
      {
        return setElement(index, value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should not happen
      }
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   */

  public final ReturnVal setElement(int index, Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return setElement(index, submittedValue, local, false);
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   */

  public synchronized ReturnVal setElement(int index, Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (!isEditable(local))     // *sync* on GanymedeSession possible.
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    // make sure we're not duplicating an item

    int oldIndex = values.indexOf(submittedValue);

    if (oldIndex == index)
      {
        return null;            // no-op
      }
    else if (oldIndex != -1)
      {
        return getDuplicateValueDialog("setElement", submittedValue); // duplicate
      }

    // make sure that the constraints on this field don't rule out, prima facie, the proposed value

    retVal = verifyNewValue(submittedValue);

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    /* check to see if verifyNewValue canonicalized the submittedValue */

    if (ReturnVal.hasTransformedValue(retVal))
      {
        submittedValue = retVal.getTransformedValueObject();
      }

    // allow the plugin class to review the operation

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                         DBEditObject.SETELEMENT,
                                                         Integer.valueOf(index),
                                                         submittedValue));

        // if a wizard intercedes, we are going to let it take the
        // ball.  we'll lose any transformation/rescan from the
        // verifyNewValue() call above, but the fact that the wizard
        // is taking over means that we're not directly accepting
        // whatever the user gave us, anyway.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // okay, we're going to proceed.. unless there's a namespace
    // violation

    ns = this.getNameSpace();

    if (ns != null)
      {
        unmark(values.elementAt(index));

        if (!mark(submittedValue))
          {
            mark(values.elementAt(index)); // we aren't clearing the old value after all

            return getConflictDialog("DBField.setElement()", submittedValue);
          }
      }

    // check our owner, do it.  Checking our owner should be the last
    // thing we do.. if it returns true, nothing should stop us from
    // running the change to completion

    retVal = ReturnVal.merge(retVal, eObj.finalizeSetElement(this, index, submittedValue));

    if (ReturnVal.didSucceed(retVal))
      {
        values.setElementAt(submittedValue, index);
      }
    else
      {
        // our owner disapproved of the operation,
        // undo the namespace manipulations, if any,
        // and finish up.

        if (ns != null)
          {
            // values is in its final state.. if the submittedValue
            // isn't in it anywhere, unmark it in the namespace

            if (!values.contains(submittedValue))
              {
                unmark(submittedValue);
              }

            // mark the old value.. we can always do this safely, even
            // if the value was already marked

            mark(values.elementAt(index));
          }
      }

    return retVal;
  }

  /**
   * <p>Adds an element to the end of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful addElement will
   * encode an order to rescan this field.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElement(Object value) throws GanyPermissionsException
  {
    ReturnVal result = addElement(value, false, false);

    if (ReturnVal.hasTransformedValue(result))
      {
        result = rescanThisField(result);
      }

    return result;
  }

  /**
   * <p>Adds an element to the end of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElementLocal(Object value)
  {
    try
      {
        return addElement(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>Adds an element to the end of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValue Value to be added
   * @param local If true, permissions checking will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElement(Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return addElement(submittedValue, local, false);
  }

  /**
   * <p>Adds an element to the end of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValue Value to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal addElement(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))     // *sync* on GanymedeSession possible
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (submittedValue == null)
      {
        // "Null value passed to addElement() on field {0} in object {1}."
        throw new IllegalArgumentException(ts.l("addElement.bad_null", getName(), owner.getLabel()));
      }

    if (submittedValue instanceof String)
      {
        submittedValue = ((String) submittedValue).intern();
      }
    else if (submittedValue instanceof Invid)
      {
        submittedValue = ((Invid) submittedValue).intern();
      }

    // make sure we're not duplicating an item

    if (getVectVal().contains(submittedValue))
      {
        return getDuplicateValueDialog("addElement", submittedValue); // duplicate
      }

    // verifyNewValue should setLastError for us.

    retVal = verifyNewValue(submittedValue);

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    /* check to see if verifyNewValue canonicalized the submittedValue */

    if (ReturnVal.hasTransformedValue(retVal))
      {
        submittedValue = retVal.getTransformedValueObject();
      }

    if (size() >= getMaxArraySize())
      {
        // "addElement() Error: Field {0} in object {1} is already at or beyond its maximum allowed size."
        return Ganymede.createErrorDialog(ts.l("addElement.overflow", getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                         DBEditObject.ADDELEMENT,
                                                         submittedValue,
                                                         null));

        // if a wizard intercedes, we are going to let it take the
        // ball.  we'll lose any transformation/rescan from the
        // verifyNewValue() call above, but the fact that the wizard
        // is taking over means that we're not directly accepting
        // whatever the user gave us, anyway.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    ns = getNameSpace();

    if (ns != null)
      {
        if (!mark(submittedValue))      // *sync* DBNameSpace
          {
            return getConflictDialog("DBField.addElement()", submittedValue);
          }
      }

    retVal = ReturnVal.merge(retVal, eObj.finalizeAddElement(this, submittedValue));

    if (ReturnVal.didSucceed(retVal))
      {
        getVectVal().addElement(submittedValue);
      }
    else
      {
        if (ns != null)
          {
            // if the value that we were going to add is not
            // left in our vector, unmark the to-be-added
            // value

            if (!getVectVal().contains(submittedValue))
              {
                unmark(submittedValue); // *sync* DBNameSpace
              }
          }
      }

    return retVal;
  }

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
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElements(Vector values) throws GanyPermissionsException
  {
    // the interior addElements call will automatically call for a
    // rescanThisField() for us if it encountered any transformed
    // values while processing the objects in values

    return addElements(values, false, false);
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElementsLocal(Vector values)
  {
    try
      {
        return addElements(values, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values
   * that it can, even if some values are refused by the server logic.
   * Any values that are skipped will be reported in a dialog passed
   * back in the returned ReturnVal.  This is intended to support
   * vector field cloning, in which we add what values may be cloned,
   * and skip the rest.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElementsLocal(Vector submittedValues, boolean noWizards, boolean copyFieldMode)
  {
    try
      {
        return addElements(submittedValues, true, noWizards, copyFieldMode);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElements(Vector submittedValues, boolean local) throws GanyPermissionsException
  {
    return addElements(submittedValues, local, false);
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal addElements(Vector submittedValues, boolean local,
                                     boolean noWizards) throws GanyPermissionsException
  {
    return addElements(submittedValues, local, noWizards, false);
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values that
   * it can, even if some values are refused by the server logic.  Any
   * values that are skipped will be reported in a dialog passed back
   * in the returned ReturnVal
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal addElements(Vector submittedValues, boolean local,
                                            boolean noWizards, boolean copyFieldMode) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector approvedValues = new Vector();
    boolean transformed = false;

    /* -- */

    if (!isEditable(local))     // *sync* on GanymedeSession possible
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (submittedValues == null || submittedValues.size() == 0)
      {
        // "Null or empty Vector passed to addElements() on field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("addElements.bad_null", getName(), owner.getLabel()));
      }

    if (submittedValues == getVectVal())
      {
        // "Error, attempt to add self elements to field {0} in object {1}."
        throw new IllegalArgumentException(ts.l("addElements.self_add", getName(), owner.getLabel()));
      }

    Vector duplicateValues = VectorUtils.intersection(getVectVal(), submittedValues);

    if (duplicateValues.size() > 0)
      {
        if (!copyFieldMode)
          {
            return getDuplicateValuesDialog("addElements", VectorUtils.vectorString(duplicateValues));
          }
        else
          {
            submittedValues = VectorUtils.difference(submittedValues, getVectVal());
          }
      }

    // can we add this many values?

    if (size() + submittedValues.size() > getMaxArraySize())
      {
        // "addElements() Error: Field {0} in object {1} can''t take {2,number,#} new values..\n
        // It already has {3,number,#} elements, and may not have more than {4,number,#} total."
        return Ganymede.createErrorDialog(ts.l("addElements.overflow",
                                               getName(),
                                               owner.getLabel(),
                                               Integer.valueOf(submittedValues.size()),
                                               Integer.valueOf(size()),
                                               Integer.valueOf(getMaxArraySize())));
      }

    // check to see if all of the submitted values are acceptable in
    // type and in identity.  if copyFieldMode, we won't complain
    // unless none of the submitted values are acceptable

    StringBuilder errorBuf = new StringBuilder();

    for (int i = 0; i < submittedValues.size(); i++)
      {
        Object submittedValue = submittedValues.elementAt(i);

        // intern our strings and invids

        if (submittedValue instanceof String)
          {
            submittedValues.set(i, ((String) submittedValue).intern());
          }
        else if (submittedValue instanceof Invid)
          {
            submittedValues.set(i, ((Invid) submittedValue).intern());
          }

        retVal = verifyNewValue(submittedValue);

        if (ReturnVal.hasTransformedValue(retVal))
          {
            submittedValue = retVal.getTransformedValueObject();
            transformed = true;
          }

        if (!ReturnVal.didSucceed(retVal))
          {
            if (!copyFieldMode)
              {
                return retVal;
              }
            else
              {
                if (retVal.getDialog() != null)
                  {
                    if (errorBuf.length() != 0)
                      {
                        errorBuf.append("\n\n");
                      }

                    errorBuf.append(retVal.getDialog().getText());
                  }
              }
          }
        else
          {
            approvedValues.addElement(submittedValue);
          }
      }

    if (approvedValues.size() == 0)
      {
        // "addElements() Error"
        return Ganymede.createErrorDialog(ts.l("addElements.unapproved_title"),
                                          errorBuf.toString());
      }

    // see if our container wants to intercede in the adding operation

    eObj = (DBEditObject) owner;
    editset = eObj.getEditSet();

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENTS, approvedValues, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // check to see if all of the values being added are acceptable to
    // a namespace constraint

    ns = getNameSpace();

    if (ns != null)
      {
        synchronized (ns)
          {
            for (int i = 0; i < approvedValues.size(); i++)
              {
                if (!ns.testmark(editset, approvedValues.elementAt(i)))
                  {
                    return getConflictDialog("DBField.addElements()", approvedValues.elementAt(i));
                  }
              }

            for (int i = 0; i < approvedValues.size(); i++)
              {
                if (!ns.mark(editset, approvedValues.elementAt(i), this))
                  {
                    throw new RuntimeException("error: testmark / mark inconsistency");
                  }
              }
          }
      }

    // okay, see if the DBEditObject is willing to allow all of these
    // elements to be added

    retVal = ReturnVal.merge(retVal, eObj.finalizeAddElements(this, approvedValues));

    if (ReturnVal.didSucceed(retVal))
      {
        // okay, we're allowed to do it, so we add them all

        for (int i = 0; i < approvedValues.size(); i++)
          {
            getVectVal().addElement(approvedValues.elementAt(i));
          }

        if (retVal == null)
          {
            retVal = ReturnVal.success();
          }

        // if we were not able to copy some of the values (and we
        // had copyFieldMode set), encode a description of what
        // happened along with the success code

        if (errorBuf.length() != 0)
          {
            // "Warning"
            retVal.setDialog(new JDialogBuff(ts.l("addElements.warning"),
                                                errorBuf.toString(),
                                                Ganymede.OK, // localized
                                                null,
                                                "ok.gif"));
          }

        if (transformed)
          {
            // one or more of the values we were given to add was
            // canonicalized or otherwise transformed.  let the client
            // know it will need to ask us for the final state of the
            // field.

            retVal.requestRefresh(owner.getInvid(), this.getID());
          }
      }
    else
      {
        if (ns != null)
          {
            // for each value that we were going to add (and which we
            // marked in our namespace above), we need to unmark it if
            // it is not contained in our vector at this point.

            Vector currentValues = getVectVal();

            // build up a hashtable of our current values so we can
            // efficiently do membership checks for our namespace

            Hashtable valuesLeft = new Hashtable(currentValues.size());

            for (int i = 0; i < currentValues.size(); i++)
              {
                valuesLeft.put(currentValues.elementAt(i), currentValues.elementAt(i));
              }

            // for each item we were submitted, unmark it in our
            // namespace if we don't have it left in our vector.

            for (int i = 0; i < approvedValues.size(); i++)
              {
                if (!valuesLeft.containsKey(approvedValues.elementAt(i)))
                  {
                    if (!ns.unmark(editset, approvedValues.elementAt(i), this))
                      {
                        throw new RuntimeException(ts.l("global.bad_unmark", approvedValues.elementAt(i), this));
                      }
                  }
              }
          }
      }

    return retVal;
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure,
   * and may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElement(int index) throws GanyPermissionsException
  {
    return deleteElement(index, false, false);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElementLocal(int index)
  {
    try
      {
        return deleteElement(index, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElement(int index, boolean local) throws GanyPermissionsException
  {
    return deleteElement(index, local, false);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal deleteElement(int index, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))     // *sync* GanymedeSession possible
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    if ((index < 0) || (index >= values.size()))
      {
        // "Invalid index {0,number,#} for array access on field {0} in object {1}."
        throw new ArrayIndexOutOfBoundsException(ts.l("global.out_of_range", Integer.valueOf(index), getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, Integer.valueOf(index), null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    retVal = ReturnVal.merge(retVal, eObj.finalizeDeleteElement(this, index));

    if (ReturnVal.didSucceed(retVal))
      {
        Object valueToDelete = values.elementAt(index);
        values.removeElementAt(index);

        // if this field no longer contains the element that
        // we are deleting, we're going to unmark that value
        // in our namespace

        if (!values.contains(valueToDelete))
          {
            unmark(valueToDelete);
          }
      }

    return retVal;
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure,
   * and may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElement(Object value) throws GanyPermissionsException
  {
    return deleteElement(value, false, false);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElementLocal(Object value)
  {
    try
      {
        return deleteElement(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElement(Object value, boolean local) throws GanyPermissionsException
  {
    return deleteElement(value, local, false);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final synchronized ReturnVal deleteElement(Object value, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    if (!isEditable(local))     // *sync* GanymedeSession possible
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (value == null)
      {
        // "deleteElement() Error: Can''t delete null value from field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("deleteElement.bad_null", getName(), owner.getLabel()));
      }

    int index = indexOfValue(value);

    if (index == -1)
      {
        // "deleteElement() Error: Value ''{0}'' not present to be deleted from field {1} in object {2}."
        return Ganymede.createErrorDialog(ts.l("deleteElement.missing_element", value, getName(), owner.getLabel()));
      }

    return deleteElement(index, local, noWizards);      // *sync* DBNameSpace possible
  }

  /**
   * <p>Removes all elements from this field, if a
   * vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteAllElements will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteAllElements() throws GanyPermissionsException
  {
    return this.deleteElements(this.getValues());
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElements will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElements(Vector values) throws GanyPermissionsException
  {
    return deleteElements(values, false, false);
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>Server-side method only</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElementsLocal(Vector values)
  {
    try
      {
        return deleteElements(values, true, false);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>Server-side method only</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public final ReturnVal deleteElements(Vector valuesToDelete, boolean local) throws GanyPermissionsException
  {
    return deleteElements(valuesToDelete, local, false);
  }

  /**
   * <p>Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a
   * failure code is returned, no elements in values were removed.</p>
   *
   * <p>Server-side method only</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal deleteElements(Vector valuesToDelete, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector currentValues;

    /* -- */

    if (!isEditable(local))     // *sync* on GanymedeSession possible
      {
        // "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
        throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (valuesToDelete == null || valuesToDelete.size() == 0)
      {
        // "Null or empty Vector passed to deleteElements() on field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("deleteElements.bad_null", getName(), owner.getLabel()));
      }

    // get access to our value vector.

    currentValues = getVectVal();

    // make sure the two vectors we're going to be manipulating aren't
    // actually the same vector

    if (valuesToDelete == currentValues)
      {
        // "Error, attempt to delete self elements from field {0} in object {1}."
        throw new IllegalArgumentException(ts.l("deleteElements.self_delete", getName(), owner.getLabel()));
      }

    // see if we are being asked to remove items not in our vector

    Vector notPresent = VectorUtils.minus(valuesToDelete, currentValues);

    if (notPresent.size() != 0)
      {
        // "deleteElements() Error: Values ''{0}'' not present to be deleted from field {1} in object {2}."
        return Ganymede.createErrorDialog(ts.l("deleteElements.missing_elements",
                                               VectorUtils.vectorString(notPresent),
                                               getName(), owner.getLabel()));
      }

    // see if our container wants to intercede in the removing operation

    eObj = (DBEditObject) owner;
    editset = eObj.getEditSet();

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.DELELEMENTS, valuesToDelete, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // okay, see if the DBEditObject is willing to allow all of these
    // elements to be removed

    retVal = ReturnVal.merge(retVal, eObj.finalizeDeleteElements(this, valuesToDelete));

    if (ReturnVal.didSucceed(retVal))
      {
        // okay, we're allowed to remove, so take the items out

        for (int i = 0; i < valuesToDelete.size(); i++)
          {
            currentValues.removeElement(valuesToDelete.elementAt(i));
          }

        // if this vector is connected to a namespace, clear out what
        // we've left out from the namespace

        ns = getNameSpace();

        if (ns != null)
          {
            // build up a hashtable of our current values so we can
            // efficiently do membership checks for our namespace

            Hashtable valuesLeft = new Hashtable(currentValues.size());

            for (int i = 0; i < currentValues.size(); i++)
              {
                valuesLeft.put(currentValues.elementAt(i), currentValues.elementAt(i));
              }

            // for each item we were submitted, unmark it in our
            // namespace if we don't have it left in our vector.

            for (int i = 0; i < valuesToDelete.size(); i++)
              {
                if (!valuesLeft.containsKey(valuesToDelete.elementAt(i)))
                  {
                    if (!ns.unmark(editset, valuesToDelete.elementAt(i), this))
                      {
                        // "Error encountered attempting to dissociate
                        // reserved value {0} from field {1}.  This
                        // may be due to a server error, or it may be
                        // due to a non-interactive transaction
                        // currently at work trying to shuffle
                        // namespace values between multiple objects.
                        // In the latter case, you may be able to
                        // succeed at this operation after the
                        // non-interactive transaction gives up."

                        throw new RuntimeException(ts.l("global.bad_unmark", valuesToDelete.elementAt(i), this));
                      }
                  }
              }
          }
      }

    return retVal;
  }

  /**
   * <p>Returns true if this field is a vector field and value is contained
   * in this field.</p>
   *
   * <p>This method always checks for read privileges.</p>
   *
   * @param value The value to look for in this field
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean containsElement(Object value) throws GanyPermissionsException
  {
    return containsElement(value, false);
  }

  /**
   * <p>This method returns true if this field is a vector
   * field and value is contained in this field.</p>
   *
   * <p>This method is server-side only, and never checks for read
   * privileges.</p>
   *
   * @param value The value to look for in this fieldu
   */

  public final boolean containsElementLocal(Object value)
  {
    try
      {
        return containsElement(value, true);
      }
    catch (GanyPermissionsException ex)
      {
        throw new RuntimeException(ex); // should never happen
      }
  }

  /**
   * <p>This method returns true if this field is a vector
   * field and value is contained in this field.</p>
   *
   * <p>This method is server-side only.</p>
   *
   * @param value The value to look for in this field
   * @param local If false, read permissin is checked for this field
   */

  public final boolean containsElement(Object value, boolean local) throws GanyPermissionsException
  {
    if (!local && !verifyReadPermission())
      {
        // "Don''t have permission to read field {0} in object {1}"
        throw new GanyPermissionsException(ts.l("global.no_read_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    return (indexOfValue(value) != -1);
  }

  /**
   * Returns a {@link arlut.csd.ganymede.server.fieldDeltaRec
   * fieldDeltaRec} object listing the changes between this field's
   * state and that of the prior oldField state.
   */

  public fieldDeltaRec getVectorDiff(DBField oldField)
  {
    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (oldField == null)
      {
        // "Bad call to getVectorDiff() on field {0} in object {1}.  oldField is null."
        throw new IllegalArgumentException(ts.l("getVectorDiff.null_old", getName(), owner.getLabel()));
      }

    if ((oldField.getID() != getID()) ||
        (oldField.getObjTypeID() != getObjTypeID()))
      {
        // "Bad call to getVectorDiff() on field {0} in object {1}.  Incompatible fields."
        throw new IllegalArgumentException(ts.l("getVectorDiff.bad_type", getName(), owner.getLabel()));
      }

    /* - */

    fieldDeltaRec deltaRec = new fieldDeltaRec(getID());
    Vector oldValues = oldField.getVectVal();
    Vector newValues = getVectVal();
    Vector addedValues = VectorUtils.difference(newValues, oldValues);
    Vector deletedValues = VectorUtils.difference(oldValues, newValues);

    for (int i = 0; i < addedValues.size(); i++)
      {
        deltaRec.addValue(addedValues.elementAt(i));
      }

    for (int i = 0; i < deletedValues.size(); i++)
      {
        deltaRec.delValue(deletedValues.elementAt(i));
      }

    return deltaRec;
  }

  // ****
  //
  // Server-side namespace management functions
  //
  // ****

  /**
   * <p>unmark() is used to make any and all namespace values in this
   * field as available for use by other objects in the same editset.
   * When the editset is committed, any unmarked values will be
   * flushed from the namespace.</p>
   *
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  final void unmark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
        return;
      }

    if (!isVector())
      {
        if (!namespace.unmark(editset, this.key(), this))
          {
            throw new RuntimeException(ts.l("global.bad_unmark", this.key(), this));
          }
      }
    else
      {
        synchronized (namespace)
          {
            for (int i = 0; i < size(); i++)
              {
                if (!namespace.testunmark(editset, key(i), this))
                  {
                    throw new RuntimeException(ts.l("global.bad_unmark", this.key(), this));
                  }
              }

            for (int i = 0; i < size(); i++)
              {
                if (!namespace.unmark(editset, key(i), this))
                  {
                    // "Error: testunmark() / unmark() inconsistency"
                    throw new RuntimeException(ts.l("unmark.testunmark_problem"));
                  }
              }

            return;
          }
      }
  }

  /**
   * <p>Unmark a specific value associated with this field, rather
   * than unmark all values associated with this field.  Note
   * that this method does not check to see if the value is
   * currently associated with this field, it just goes ahead
   * and unmarks it.  This is to be used by the vector
   * modifiers (setElement, addElement, deleteElement, etc.)
   * to keep track of namespace modifications as we go along.</p>
   *
   * <p>If there is no namespace associated with this field, this
   * method will always return true, as a no-op.</p>
   *
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  final boolean unmark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
        return true;            // do a no-op
      }

    if (value == null)
      {
        return true;            // no previous value
      }

    return namespace.unmark(editset, value, this);
  }

  /**
   * <p>mark() is used to mark any and all values in this field as taken
   * in the namespace.  When the editset is committed, marked values
   * will be permanently reserved in the namespace.  If the editset is
   * instead aborted, the namespace values will be returned to their
   * pre-editset status.</p>
   *
   * <p>If there is no namespace associated with this field, this
   * method will always return true, as a no-op.</p>
   *
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  final boolean mark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();

    if (namespace == null)
      {
        return true;            // do a no-op
      }

    editset = ((DBEditObject) owner).getEditSet();

    if (!isVector())
      {
        return namespace.mark(editset, this.key(), this);
      }
    else
      {
        synchronized (namespace)
          {
            for (int i = 0; i < size(); i++)
              {
                if (!namespace.testmark(editset, key(i)))
                  {
                    return false;
                  }
              }

            for (int i = 0; i < size(); i++)
              {
                if (!namespace.mark(editset, key(i), this))
                  {
                    throw new RuntimeException("error: testmark / mark inconsistency");
                  }
              }

            return true;
          }
      }
  }

  /**
   * <p>Mark a specific value associated with this field, rather than
   * mark all values associated with this field.  Note that this
   * method does not in any way associate this value with this field
   * (add it, set it, etc.), it just marks it.  This is to be used by
   * the vector modifiers (setElement, addElement, etc.)  to keep
   * track of namespace modifications as we go along.</p>
   *
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  final boolean mark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
        return false;           // should we throw an exception?
      }

    if (value == null)
      {
        return false;
        //      throw new NullPointerException("null value in mark()");
      }

    if (editset == null)
      {
        throw new NullPointerException("null editset in mark()");
      }

    return namespace.mark(editset, value, this);
  }

  // ****
  //
  // Methods for subclasses to override to implement the
  // behavior for this field.
  //
  // ****

  /**
   * Overridable method to determine whether an
   * Object submitted to this field is of an appropriate
   * type.
   */

  abstract public boolean verifyTypeMatch(Object o);

  /**
   * <p>Overridable method to verify that an object submitted to this
   * field has an appropriate value.</p>
   *
   * <p>This check is more limited than that of verifyNewValue().. all it
   * does is make sure that the object parameter passes the simple
   * value constraints of the field.  verifyNewValue() does that plus
   * a bunch more, including calling to the DBEditObject hook for the
   * containing object type to see whether it happens to feel like
   * accepting the new value or not.</p>
   *
   * <p>verifyBasicConstraints() is used to double check for values that
   * are already in fields, in addition to being used as a likely
   * component of verifyNewValue() to verify new values.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal verifyBasicConstraints(Object o)
  {
    return null;
  }

  /**
   * <p>Overridable method to verify that an object submitted to this
   * field has an appropriate value.</p>
   *
   * <p>This method is intended to make the final go/no go decision about
   * whether a given value is appropriate to be placed in this field,
   * by whatever means (vector add, vector replacement, scalar
   * replacement).</p>
   *
   * <p>This method is expected to call the
   * {@link arlut.csd.ganymede.server.DBEditObject#verifyNewValue(arlut.csd.ganymede.server.DBField,java.lang.Object)}
   * method on {@link arlut.csd.ganymede.server.DBEditObject} in order to allow custom
   * plugin classes to deny any given value that the plugin might not
   * care for, for whatever reason.  Otherwise, the go/no-go decision
   * will be made based on the checks performed by
   * {@link arlut.csd.ganymede.server.DBField#verifyBasicConstraints(java.lang.Object) verifyBasicConstraints}.</p>
   *
   * <p>The ReturnVal that is returned may have transformedValue set, in
   * which case the code that calls this verifyNewValue() method
   * should consider transformedValue as replacing the 'o' parameter
   * as the value that verifyNewValue wants to be put into this field.
   * This usage of transformedValue is for canonicalizing input data.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  abstract public ReturnVal verifyNewValue(Object o);

  /**
   * <p>Overridable method to verify that the current {@link
   * arlut.csd.ganymede.server.DBSession DBSession} / {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} has permission to read
   * values from this field.</p>
   */

   public boolean verifyReadPermission()
   {
     if (owner.getGSession() == null)
       {
         return true; // we don't know who is looking at us, assume it's a server-local access
       }

     PermEntry pe = owner.getFieldPerm(getID());

     if (pe == null)
       {
         return false;
       }

     return pe.isVisible();
   }

  /**
   * <p>Overridable method to verify that the current {@link
   * arlut.csd.ganymede.server.DBSession DBSession} / {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} has permission to read
   * values from this field.</p>
   *
   * <p>This version of verifyReadPermission() is intended to be used
   * in a context in which it would be too expensive to make a
   * read-only duplicate copy of a DBObject from the DBObjectBase's
   * object table, strictly for the purpose of associating a
   * GanymedeSession with the DBObject for permissions
   * verification.</p>
   */

   public boolean verifyReadPermission(GanymedeSession gSession)
   {
     if (gSession == null)
       {
         return true; // we don't know who is looking at us, assume it's a server-local access
       }

     PermEntry pe = gSession.getPermManager().getPerm(owner, getID());

     // if there is no permission explicitly recorded for the field,
     // inherit from the object as a whole

     if (pe == null)
       {
         pe = gSession.getPermManager().getPerm(owner);
       }

     if (pe == null)
       {
         return false;
       }

     return pe.isVisible();
   }

  /**
   * <p>Overridable method to verify that the current DBSession /
   * DBEditSet has permission to write values into this field.</p>
   */

  public boolean verifyWritePermission()
  {
    if (!(owner instanceof DBEditObject))
      {
        return false;  // if we're not in a transaction, we certainly can't be edited.
      }

    if (owner.getGSession() != null)
      {
        try
          {
            owner.getGSession().checklogin();  // mostly for the lastaction update side-effect
          }
        catch (NotLoggedInException ex)
          {
            return false;
          }
      }

    PermEntry pe = owner.getFieldPerm(getID());

    if (pe == null)
      {
        return false;
      }

    return pe.isEditable();
  }

  /**
   * <p>Sub-class hook to support elements for which the default
   * equals() test is inadequate, such as IP addresses (represented
   * as arrays of Byte[] objects.</p>
   *
   * <p>Returns -1 if the value was not found in this field.</p>
   *
   * <p>This method assumes that the calling method has already verified
   * that this is a vector field.</p>
   */

  public int indexOfValue(Object value)
  {
    return getVectVal().indexOf(value);
  }

  /**
   * <p>Returns a Vector of the values of the elements in this field, if
   * a vector.</p>
   *
   * <p>This is intended to be used within the Ganymede server, it
   * bypasses the permissions checking that getValues() does.</p>
   *
   * <p>This method returns a safe copy of the contained Vector.</p>
   */

  public Vector getValuesLocal()
  {
    if (!isVector())
      {
        // "Vector method called on a scalar field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    return new Vector(getVectVal()); // defensive copy
  }

  /**
   * <p>Returns an Object carrying the value held in this field.</p>
   *
   * <p>This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.</p>
   */

  public Object getValueLocal()
  {
    if (isVector())
      {
        // "Scalar method called on a vector field: {0} in object {1}"
        throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    return value;
  }

  // ***
  //
  // The following two methods implement checkpoint and rollback facilities for
  // DBField.  These methods save the field's internal state and restore it
  // on demand at a later time.  The intent is to allow checkpoint/restore
  // without changing the object identity (memory address) of the DBField so
  // that the DBEditSet checkpoint/restore logic can work.
  //
  // ***

  /**
   * <p>This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later if need be.</p>
   *
   * <p>This method is not synchronized because all operations performed
   * by this method are either synchronized at a lower level or are
   * atomic.</p>
   *
   * <p>Called by {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s
   * {@link arlut.csd.ganymede.server.DBEditObject#checkpoint() checkpoint()}
   * method.</p>
   */

  public Object checkpoint()
  {
    if (isVector())
      {
        return new Vector(getVectVal());
      }
    else
      {
        return value;
      }
  }

  /**
   * <p>This method is used to basically force state into this field.</p>
   *
   * <p>It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity, and
   * without doing any of the checking or side effects that calling
   * setValue() will typically do.</p>
   *
   * <p>In particular, it is not necessary to subclass this method for
   * use with {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}, since
   * the {@link arlut.csd.ganymede.server.DBEditSet#rollback(java.lang.String) rollback()}
   * method will always rollback all objects in the transaction at the same
   * time.  It is not necessary to have the InvidDBField subclass handle
   * binding/unbinding during rollback, since all objects which could conceivably
   * be involved in a link will also have their own states rolled back.</p>
   *
   * <p>Called by {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s
   * {@link arlut.csd.ganymede.server.DBEditObject#rollback(java.util.Hashtable) rollback()}
   * method.</p>
   */

  public synchronized void rollback(Object oldval)
  {
    if (!(owner instanceof DBEditObject))
      {
        throw new RuntimeException("Invalid rollback on field " +
                                   getName() + ", not in an editable context");
      }

    if (isVector())
      {
        if (!(oldval instanceof Vector))
          {
            throw new RuntimeException("Invalid vector rollback on field " +
                                       getName());
          }
        else
          {
            // in theory we perhaps should iterate through the oldval
            // Vector to make sure that each element is of the right
            // type.. in practice, that would be a lot of overhead to
            // guard against something that should never happen,
            // anyway.
            //
            // i'm just saying this to cover my ass in case it does,
            // so i'll know that i was deliberately rather than
            // accidentally stupid.

            this.value = oldval;
          }
      }
    else
      {
        if (!verifyTypeMatch(oldval))
          {
            throw new RuntimeException("Invalid scalar rollback on field " +
                                       getName());
          }
        else
          {
            this.value = oldval;
          }
      }
  }

  /**
   * <p>This method takes the result of an operation on this field
   * and wraps it with a {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * that encodes an instruction to the client to rescan
   * this field.  This isn't normally necessary for most client
   * operations, but it is necessary for the case in which wizards
   * call DBField.setValue() on behalf of the client, because in those
   * cases, the client otherwise won't know that the wizard modified
   * the field.</p>
   *
   * <p>This makes for a significant bit of overhead on client calls
   * to the field modifier methods, but this is avoided if code
   * on the server uses setValueLocal(), setElementLocal(), addElementLocal(),
   * or deleteElementLocal() to make changes to a field.</p>
   *
   * <p>If you are ever in a situation where you want to use the local
   * variants of the modifier methods (to avoid permissions checking
   * overhead), but you <b>do</b> want to have the field's rescan
   * information returned, you can do something like:</p>
   *
   * <pre>
   *
   * return field.rescanThisField(field.setValueLocal(null));
   *
   * </pre>
   */

  public final ReturnVal rescanThisField(ReturnVal original)
  {
    if (!ReturnVal.didSucceed(original))
      {
        return original;
      }

    if (original == null)
      {
        original = ReturnVal.success();
      }

    if (this.getID() == owner.getLabelFieldID())
      {
        original.setObjectLabelChanged(owner.getInvid(), this.getValueString());
      }

    original.addRescanField(getOwner().getInvid(), getID());

    return original;
  }

  /**
   * For debugging
   */

  public String toString()
  {
    return "[" + owner.toString() + ":" + getName() + "]";
  }

  /**
   * <p>Handy utility method for reporting namespace conflict.  This
   * method will work to identify the object and field which is in
   * conflict, and will return an appropriate {@link
   * arlut.csd.ganymede.common.ReturnVal ReturnVal} with an
   * appropriate error dialog.</p>
   */

  public ReturnVal getConflictDialog(String methodName, Object conflictValue)
  {
    DBNameSpace ns = getNameSpace();

    try
      {
        DBField conflictField = ns.lookupPersistent(conflictValue);

        if (conflictField != null)
          {
            DBObject conflictObject = conflictField.getOwner();
            String conflictLabel = conflictObject.getLabel();
            String conflictClassName = conflictObject.getTypeName();

            // This action could not be completed because "{0}" is already being used.
            //
            // {1} "{2}" contains this value in its {3} field.
            //
            // You can choose a different value here, or you can try to edit or delete the "{2}" object to remove the conflict.

            return Ganymede.createErrorDialog(ts.l("getConflictDialog.errorTitle", methodName),
                                              ts.l("getConflictDialog.persistentError",
                                                   conflictValue, conflictClassName, conflictLabel, conflictField.getName()));
          }
        else
          {
            conflictField = ns.lookupShadow(conflictValue);

            DBObject conflictObject = conflictField.getOwner();
            String conflictLabel = conflictObject.getLabel();
            String conflictClassName = conflictObject.getTypeName();

            // This action could not be completed because "{0}" is currently being manipulated in a concurrent transaction.
            //
            // {1} "{2}" contains this value in its {3} field.
            //
            // You can choose a different value here, or you can try to edit or delete the "{2}" object to remove the conflict.

            return Ganymede.createErrorDialog(ts.l("getConflictDialog.errorTitle", methodName),
                                              ts.l("getConflictDialog.transactionError",
                                                   conflictValue, conflictClassName, conflictLabel, conflictField.getName()));
          }
      }
    catch (NullPointerException ex)
      {
        Ganymede.logError(ex);

        return Ganymede.createErrorDialog(ts.l("getConflictDialog.errorTitle", methodName),
                                          ts.l("getConflictDialog.simpleError", conflictValue));
      }
  }

  /**
   * <p>Handy utility method for reporting an attempted duplicate
   * submission to a vector field.</p>
   */

  public ReturnVal getDuplicateValueDialog(String methodName, Object conflictValue)
  {
    // "Server: Error in {0}"
    // "This action could not be performed because "{0}" is already contained in field {1} in object {2}."
    return Ganymede.createErrorDialog(ts.l("getDuplicateValueDialog.error_in_method_title", methodName),
                                      ts.l("getDuplicateValueDialog.error_body",
                                           String.valueOf(conflictValue), getName(), owner.getLabel()));
  }

  /**
   * <p>Handy utility method for reporting an attempted duplicate
   * submission to a vector field.</p>
   */

  public ReturnVal getDuplicateValuesDialog(String methodName, String conflictValues)
  {
    // "Server: Error in {0}"
    // "This action could not be performed because "{0}" are already contained in field {1} in object {2}."
    return Ganymede.createErrorDialog(ts.l("getDuplicateValueDialog.error_in_method_title", methodName),
                                      ts.l("getDuplicateValuesDialog.error_body",
                                           conflictValues, getName(), owner.getLabel()));
  }
}
