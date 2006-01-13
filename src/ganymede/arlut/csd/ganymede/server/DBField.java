/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2006
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.db_field;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/

/**
 * This abstract base class encapsulates the basic logic for fields in the
 * Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore},
 * including permissions and unique value handling.
 *
 * DBFields are the actual carriers of field value in the Ganymede
 * server.  Each {@link arlut.csd.ganymede.server.DBObject DBObject} holds a
 * set of DBFields in an array.  Each DBField is associated with a {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} field
 * definition (see {@link arlut.csd.ganymede.server.DBField#getFieldDef()
 * getFieldDef()}) by way of its owner's type and it's own field code,
 * which defines the type of the field as well as various generic and
 * type-specific attributes for the field.  The DBObjectBaseField
 * information is created and edited with the Ganymede schema
 * editor.
 *
 * DBField is an abstract class.  There is a different subclass of DBField
 * for each kind of data that can be held in the Ganymede server, as follows:
 *
 * <UL>
 * <LI>{@link arlut.csd.ganymede.server.StringDBField StringDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.BooleanDBField BooleanDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.NumericDBField NumericDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.FloatDBField FloatDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.DateDBField DateDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.InvidDBField InvidDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.IPDBField IPDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.PasswordDBField PasswordDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.server.PermissionMatrixDBField PermissionMatrixDBField}</LI>
 * </UL>
 *
 * Each DBField subclass is responsible for writing itself to disk
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
 * IPDBField support vectors of values.
 *
 * The Ganymede client can directly access fields in RMI-published
 * objects using the {@link arlut.csd.ganymede.rmi.db_field db_field} RMI
 * interface.  Each concrete subclass of DBField has its own special
 * RMI interface which provides special methods for the client.
 * Adding a new data type to the Ganymede server will involve creating
 * a new DBField subclass, as well as a new RMI interface for any
 * special field methods.  All client code would also need to be
 * modified to be aware of the new field type.  DBObjectBaseField,
 * DBEditObject and DBObject would also need to be modified to be
 * aware of the new field type for schema editing, customization, and object loading.
 * The schema editor would have to be modified as well.
 *
 * But you can do it if you absolutely have to.  Just be careful and take a good
 * look around at the code.
 *
 * Note that while DBField was designed to be subclassed, it should only be
 * necessary for adding a new data type to the server.  All other likely 
 * customizations you'd want to do are handled by
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} customization methods.  Most
 * DBField methods at some point call methods on the DBObject/DBEditObject
 * that contains it.  All methods that cause changes to fields call out to
 * finalizeXYZ() and/or wizardHook() methods in DBEditObject.  Consult the
 * DBEditObject customization guide for details on the field/object interactions.
 *
 * An important note about synchronization: it is possible to encounter a
 * condition called a <b>nested monitor deadlock</b>, where a synchronized
 * method on a field can block trying to enter a synchronized method on
 * a {@link arlut.csd.ganymede.server.DBSession DBSession}, 
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}, or 
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} object that is itself blocked
 * on another thread trying to call a synchronized method on the same field.
 *
 * To avoid this condition, no field methods that call synchronized methods on
 * other objects should themselves be synchronized in any fashion.
 */

public abstract class DBField implements Remote, db_field {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBField");

  // ---

  /**
   * the object's current value.  May be a Vector for vector fields, in
   * which case getVectVal() may be used to perform the cast.
   */

  Object value = null;
  
  /**
   * The object this field is contained within
   */

  DBObject owner;

  /**
   * The identifying field number for this field within the
   * owning object.  This number is an index into the
   * owning object type's field dictionary.
   */

  short fieldcode;

  /* -- */

  public DBField()
  {
  }

  /**
   * This method is used to return a copy of this field, with the field's owner
   * set to newOwner.
   */

  abstract public DBField getCopy(DBObject newOwner);

  /**
   * This method is designed to handle casting this field's value into
   * a vector as needed.  We don't bother to check whether value is a Vector
   * here, as the code which would have used the old values field should
   * do that for us themselves.
   *
   * This method does no permissions checking at all, and should only
   * be used from within DBField and subclass code.  For other purposes,
   * use getValuesLocal().
   *
   * This method should always return a valid vector if this field
   * is truly a vector field, as we don't keep empty vector fields in
   * non-editable objects, and if this is an editable object we'll
   * have created a vector when this field was initialized for
   * editing.
   */
  
  public final Vector getVectVal()
  {
    return (Vector) value;
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

  public int size()		// returns number of elements in array
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
	return 1;		// should throw exception?
      }
    else
      {
	return getFieldDef().getMaxArraySize();
      }     
  }      

  /**
   * This method is responsible for writing out the contents of
   * this field to an binary output stream.  It is used in writing
   * fields to the ganymede.db file and to the journal file.
   *
   * This method only writes out the value contents of this field.
   * The {@link arlut.csd.ganymede.server.DBObject DBObject}
   * {@link arlut.csd.ganymede.server.DBObject#emit(java.io.DataOutput) emit()}
   * method is responsible for writing out the field identifier information
   * ahead of the field's contents.
   */

  abstract void emit(DataOutput out) throws IOException;

  /**
   * This method is responsible for reading in the contents of
   * this field from an binary input stream.  It is used in reading
   * fields from the ganymede.db file and from the journal file.
   *
   * The code that calls receive() on this field is responsible for
   * having read enough of the binary input stream's context to
   * place the read cursor at the point in the file immediately after
   * the field's id and type information has been read.
   */

  abstract void receive(DataInput in, DBObjectBaseField definition) throws IOException;

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().
   */

  abstract void emitXML(XMLDumpContext dump) throws IOException;

  /**
   * Returns true if obj is a field with the same value(s) as
   * this one.
   *
   * This method is ok to be synchronized because it does not call
   * synchronized methods on any other object that is likely to have
   * another thread trying to call another synchronized method on
   * us. 
   */

  public synchronized boolean equals(Object obj)
  {
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
   * This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  public synchronized ReturnVal copyFieldTo(DBField target, boolean local)
  {
    ReturnVal retVal;

    /* -- */

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

	valuesToCopy = getValuesLocal();

	// We want to inhibit wizards and allow partial failure.
	//
	// We'll use addElementsLocal() here because we've already
	// verified read permission and write permission, above.

	retVal = target.addElementsLocal(valuesToCopy, true, true);

	// the above operation could fail if we don't have write
	// privileges for the target field, so we'll return an error
	// code back to the cloneFromObject() method, which will pass
	// it in an over-all advisory (non-fatal) warning back to the
	// client
	
	return retVal;
      }
  }

  /**
   * This method is intended to run a consistency check on the
   * contents of this field against the constraints specified in the
   * {@link arlut.csd.ganymede.server.DBObjectBaseField} controlling
   * this field.
   *
   * Returns a {@link arlut.csd.ganymede.common.ReturnVal} describing
   * the error if the field's contents does not meet its constraints, or null
   * if the field is in compliance with its constraints.
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
		for (int i = 0; i < values.size(); i++)
		  {
		    Object element = values.elementAt(i);

		    ReturnVal retVal = this.verifyBasicConstraints(element);

		    if (retVal != null && !retVal.didSucceed())
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
						       new Integer(size()),
						       new Integer(getMaxArraySize())));
	      }
	  }
      }

    return null;
  }

  /**
   * This method is intended to be called when this field is being checked into
   * the database.  Subclasses of DBField will override this method to clean up
   * data that is cached for speed during editing.
   */

  public void cleanup()
  {
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
    return getFieldDef().template;
  }

  /**
   *
   * Returns a handy field description packet for this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final FieldInfo getFieldInfo()
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
   *
   * Returns the object this field is part of.
   * 
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
   * This method returns a text encoded value for this DBField
   * without checking permissions.
   *
   * This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ganymede.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ganymede.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#getPerm(arlut.csd.ganymede.server.DBObject) getPerm()} 
   * method.
   *
   * If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.
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
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   */

  abstract public String getDiffString(DBField orig);

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
   * This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.
   *
   * Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.
   *
   * NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.
   */

  public synchronized ReturnVal setUndefined(boolean local) throws GanyPermissionsException
  {
    if (isVector())
      {
	if (!isEditable(local))	// *sync* GanymedeSession possible
	  {
	    // "DBField.setUndefined(): couldn''t clear vector elements from field {0} in object {1}, due to a lack of write permissions."
	    throw new GanyPermissionsException(ts.l("setUndefined.no_perm_vect", getName(), owner.getLabel()));
	  }

	// we have to clone our values Vector in order to use
	// deleteElements().

	Vector currentValues = (Vector) getVectVal().clone();

	if (currentValues.size() != 0)
	  {
	    return deleteElementsLocal(currentValues);
	  }
	else
	  {
	    return null;	// success
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
   * Returns true if this field is editable, false
   * otherwise.
   *
   * Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final boolean isEditable()
  {
    return isEditable(false);
  }

  /**
   * Returns true if this field is editable, false
   * otherwise.
   *
   * Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.
   *
   * Server-side method only
   *
   * <B>*Deadlock Hazard.*</B>
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
      getFieldDef().base.getObjectHook().canSeeField(null, this);
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
   *
   * Returns the value of this field, if a scalar.  An IllegalArgumentException
   * will be thrown if this field is a vector.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
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
   * Sets the value of this field, if a scalar.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * This method is intended to be called by code that needs to go
   * through the permission checking regime, and that needs to have
   * rescan information passed back.  This includes most wizard
   * setValue calls.
   *
   * @see arlut.csd.ganymede.server.DBSession
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal setValue(Object value) throws GanyPermissionsException
  {
    ReturnVal result;

    /* -- */

    // do the thing, calling into our subclass

    result = setValue(value, false, false);

    return rescanThisField(result);
  }

  /**
   * Sets the value of this field, if a scalar.
   *
   * <B>This method is server-side only, and bypasses
   * permissions checking.</B>
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
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
   * Sets the value of this field, if a scalar.
   *
   * <B>This method is server-side only, and bypasses permissions
   * checking.</B>
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.
   *
   * @param value Value to set this field to
   * @param noWizards If true, wizards will be skipped
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
   * Sets the value of this field, if a scalar.
   *
   * <B>This method is server-side only.</B>
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.
   *
   * @param submittedValue Value to set this field to
   * @param local If true, permissions checking will be skipped
   */

  public final ReturnVal setValue(Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return setValue(submittedValue, local, false);
  }

  /**
   * Sets the value of this field, if a scalar.
   *
   * <b>This method is server-side only.</b>
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.
   *
   * This method will be overridden by DBField subclasses with special
   * needs.
   *
   * @param submittedValue Value to set this field to
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   */

  public synchronized ReturnVal setValue(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* possible
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
	return retVal;		// no change (useful for null)
      }

    if (submittedValue instanceof String)
      {
	submittedValue = ((String) submittedValue).intern();
      }
    else if (submittedValue instanceof Invid)
      {
	submittedValue = ((Invid) submittedValue).intern();
      }

    retVal = verifyNewValue(submittedValue);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    /* check to see if verifyNewValue canonicalized the submittedValue */

    if (retVal.hasTransformedValue())
      {
	submittedValue = retVal.getTransformedValueObject();
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	newRetVal = eObj.wizardHook(this, DBEditObject.SETVAL, submittedValue, null);

	// if a wizard intercedes, we are going to let it take the
	// ball.  we'll lose any transformation/rescan from the
	// verifyNewValue() call above, but the fact that the wizard
	// is taking over means that we're not directly accepting
	// whatever the user gave us, anyway.

	if (newRetVal != null && !newRetVal.doNormalProcessing)
	  {
	    return newRetVal;
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

    newRetVal = eObj.finalizeSetValue(this, submittedValue);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	this.value = submittedValue;

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeSetValue() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
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

	// go ahead and return the dialog that was set by finalizeSetValue().

	return newRetVal;
      }
  }

  /** 
   * Returns a Vector of the values of the elements in this field,
   * if a vector.
   *
   * This is only valid for vectors.  If the field is a scalar, use
   * getValue().
   *
   * This method checks for read permissions.
   *
   * <b>Be very careful using this for server-side code, because
   * the Vector returned is not cloned from the field's actual data
   * Vector, for performance reasons.  If this is called by the client,
   * the serialization process will protect us from the client being
   * able to mess with our contents.</b>
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

    return getVectVal();
  }

  /**
   *
   * Returns the value of an element of this field,
   * if a vector.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
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
						      new Integer(index),
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
						      new Integer(index),
						      getName(),
						      owner.getLabel()));
      }

    return getVectVal().elementAt(index);
  }

  /**
   * Sets the value of an element of this field, if a vector.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.
   *
   * The ReturnVal resulting from a successful setElement will
   * encode an order to rescan this field.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @see arlut.csd.ganymede.server.DBSession
   * @see arlut.csd.ganymede.rmi.db_field
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
						      new Integer(index),
						      getName(),
						      owner.getLabel()));
      }

    return rescanThisField(setElement(index, value, false, false));
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @see arlut.csd.ganymede.server.DBSession
   *
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
						      new Integer(index),
						      getName(),
						      owner.getLabel()));
      }

    try
      {
	return setElement(index, value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should not happen
      }
  }

  /**
   * Sets the value of an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   */

  public final ReturnVal setElement(int index, Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return setElement(index, submittedValue, local, false);
  }

  /**
   * Sets the value of an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  A null result means the
   * operation was carried out successfully and no information
   * needed to be passed back about side-effects.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   */
  
  public synchronized ReturnVal setElement(int index, Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isVector())
      {
	// "Vector method called on a scalar field: {0} in object {1}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (!isEditable(local))	// *sync* on GanymedeSession possible.
      {
	// "Can''t change field {0} in object {1}, due to a lack of permissions or the object being in a non-editable state."
	throw new GanyPermissionsException(ts.l("global.no_write_perms", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    // make sure we're not duplicating an item

    int oldIndex = values.indexOf(submittedValue);

    if (oldIndex == index)
      {
	return null;		// no-op
      }
    else if (oldIndex != -1)
      {
	return getDuplicateValueDialog("setElement", submittedValue); // duplicate
      }

    // make sure that the constraints on this field don't rule out, prima facie, the proposed value

    retVal = verifyNewValue(submittedValue);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    /* check to see if verifyNewValue canonicalized the submittedValue */

    if (retVal.hasTransformedValue())
      {
	submittedValue = retVal.getTransformedValueObject();
      }

    // allow the plugin class to review the operation

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	newRetVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), submittedValue);

	// if a wizard intercedes, we are going to let it take the
	// ball.  we'll lose any transformation/rescan from the
	// verifyNewValue() call above, but the fact that the wizard
	// is taking over means that we're not directly accepting
	// whatever the user gave us, anyway.

	if (newRetVal != null && !newRetVal.doNormalProcessing)
	  {
	    return newRetVal;
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

    newRetVal = eObj.finalizeSetElement(this, index, submittedValue);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	values.setElementAt(submittedValue, index);

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeSetElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
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

	// return the error dialog from finalizeSetElement().

	return newRetVal;
      }
  }

  /**
   * Adds an element to the end of this field, if a vector.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.
   *
   * The ReturnVal resulting from a successful addElement will
   * encode an order to rescan this field.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal addElement(Object value) throws GanyPermissionsException
  {
    return rescanThisField(addElement(value, false, false));
  }

  /**
   * Adds an element to the end of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   */

  public final ReturnVal addElementLocal(Object value)
  {
    try
      {
	return addElement(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * Adds an element to the end of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @param submittedValue Value to be added
   * @param local If true, permissions checking will be skipped
   */

  public final ReturnVal addElement(Object submittedValue, boolean local) throws GanyPermissionsException
  {
    return addElement(submittedValue, local, false);
  }

  /**
   * Adds an element to the end of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @param submittedValue Value to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   */

  public synchronized ReturnVal addElement(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
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

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    /* check to see if verifyNewValue canonicalized the submittedValue */

    if (retVal.hasTransformedValue())
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

	newRetVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, submittedValue, null);

	// if a wizard intercedes, we are going to let it take the
	// ball.  we'll lose any transformation/rescan from the
	// verifyNewValue() call above, but the fact that the wizard
	// is taking over means that we're not directly accepting
	// whatever the user gave us, anyway.

	if (newRetVal != null && !newRetVal.doNormalProcessing)
	  {
	    return newRetVal;
	  }
      }

    ns = getNameSpace();

    if (ns != null)
      {
	if (!mark(submittedValue))	// *sync* DBNameSpace
	  {
	    return getConflictDialog("DBField.addElement()", submittedValue);
	  }
      }

    newRetVal = eObj.finalizeAddElement(this, submittedValue);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	getVectVal().addElement(submittedValue);

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeAddElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
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
		unmark(submittedValue);	// *sync* DBNameSpace
	      }
	  }

	// return the error dialog created by finalizeAddElement

	return newRetVal;
      }
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.
   *
   * The ReturnVal resulting from a successful addElements will
   * encode an order to rescan this field. 
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal addElements(Vector values) throws GanyPermissionsException
  {
    return rescanThisField(addElements(values, false, false));
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   */

  public final ReturnVal addElementsLocal(Vector values)
  {
    try
      {
	return addElements(values, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @param submittedValues Values to be added
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values
   * that it can, even if some values are refused by the server logic.
   * Any values that are skipped will be reported in a dialog passed
   * back in the returned ReturnVal.  This is intended to support
   * vector field cloning, in which we add what values may be cloned,
   * and skip the rest.
   */

  public final ReturnVal addElementsLocal(Vector submittedValues, boolean noWizards, boolean copyFieldMode)
  {
    try
      {
	return addElements(submittedValues, true, noWizards, copyFieldMode);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   */

  public final ReturnVal addElements(Vector submittedValues, boolean local) throws GanyPermissionsException
  {
    return addElements(submittedValues, local, false);
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   */

  public final ReturnVal addElements(Vector submittedValues, boolean local,
				     boolean noWizards) throws GanyPermissionsException
  {
    return addElements(submittedValues, local, noWizards, false);
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.
   *
   * Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values that
   * it can, even if some values are refused by the server logic.  Any
   * values that are skipped will be reported in a dialog passed back
   * in the returned ReturnVal
   */

  public synchronized ReturnVal addElements(Vector submittedValues, boolean local, 
					    boolean noWizards, boolean copyFieldMode) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector approvedValues = new Vector();
    boolean transformed = false;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
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
					       new Integer(submittedValues.size()),
					       new Integer(size()),
					       new Integer(getMaxArraySize())));
      }

    // check to see if all of the submitted values are acceptable in
    // type and in identity.  if copyFieldMode, we won't complain
    // unless none of the submitted values are acceptable

    StringBuffer errorBuf = new StringBuffer();

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

	if (retVal.hasTransformedValue())
	  {
	    submittedValue = retVal.getTransformedValueObject();
	    transformed = true;
	  }

	if (retVal != null && !retVal.didSucceed())
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

	if (retVal != null && !retVal.doNormalProcessing)
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

    newRetVal = eObj.finalizeAddElements(this, approvedValues);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	// okay, we're allowed to do it, so we add them all

	for (int i = 0; i < approvedValues.size(); i++)
	  {
	    getVectVal().addElement(approvedValues.elementAt(i));
	  }

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeAddElement() call.

	if (retVal != null)
	  {
	    newRetVal = retVal.unionRescan(newRetVal);
	  }

	if (newRetVal == null)
	  {
	    newRetVal = new ReturnVal(true, true);
	  }

	// if we were not able to copy some of the values (and we
	// had copyFieldMode set), encode a description of what
	// happened along with the success code
	
	if (errorBuf.length() != 0)
	  {
	    // "Warning"
	    newRetVal.setDialog(new JDialogBuff(ts.l("addElements.warning"),
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

	    newRetVal.requestRefresh(owner.getInvid(), this.getID());
	  }

	return newRetVal;
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

	// return the error dialog created by finalizeAddElements

	return newRetVal;
      }
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * The ReturnVal object returned encodes success or failure, 
   * and may optionally pass back a dialog.
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal deleteElement(int index) throws GanyPermissionsException
  {
    return rescanThisField(deleteElement(index, false, false));
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   */

  public final ReturnVal deleteElementLocal(int index)
  {
    try
      {
	return deleteElement(index, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   */

  public final ReturnVal deleteElement(int index, boolean local) throws GanyPermissionsException
  {
    return deleteElement(index, local, false);
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   */

  public synchronized ReturnVal deleteElement(int index, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* GanymedeSession possible
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
	throw new ArrayIndexOutOfBoundsException(ts.l("global.out_of_range", new Integer(index), getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, new Integer(index), null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    newRetVal = eObj.finalizeDeleteElement(this, index);

    if (newRetVal == null || newRetVal.didSucceed())
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

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeDeleteElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      }
    else
      {
	return newRetVal;
      }
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * The ReturnVal object returned encodes success or failure, 
   * and may optionally pass back a dialog.
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal deleteElement(Object value) throws GanyPermissionsException
  {
    return rescanThisField(deleteElement(value, false, false));
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   */

  public final ReturnVal deleteElementLocal(Object value)
  {
    try
      {
	return deleteElement(value, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   */

  public final ReturnVal deleteElement(Object value, boolean local) throws GanyPermissionsException
  {
    return deleteElement(value, local, false);
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * Server-side method only
   *
   * The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.
   */

  public synchronized ReturnVal deleteElement(Object value, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    if (!isEditable(local))	// *sync* GanymedeSession possible
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

    return deleteElement(index, local, noWizards);	// *sync* DBNameSpace possible
  }

  /**
   * Removes all elements from this field, if a
   * vector.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.
   *
   * The ReturnVal resulting from a successful deleteAllElements will
   * encode an order to rescan this field. 
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public ReturnVal deleteAllElements() throws GanyPermissionsException
  {
    return this.deleteElements(this.getValues());
  }

  /**
   * Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.
   *
   * The ReturnVal resulting from a successful deleteElements will
   * encode an order to rescan this field. 
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public final ReturnVal deleteElements(Vector values) throws GanyPermissionsException
  {
    return rescanThisField(deleteElements(values, false, false));
  }

  /**
   * Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.
   *
   * Server-side method only
   */

  public final ReturnVal deleteElementsLocal(Vector values)
  {
    try
      {
	return deleteElements(values, true, false);
      }
    catch (GanyPermissionsException ex)
      {
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.
   *
   * Server-side method only
   */

  public final ReturnVal deleteElements(Vector valuesToDelete, boolean local) throws GanyPermissionsException
  {
    return deleteElements(valuesToDelete, local, false);
  }

  /**
   * Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.  If a success code is returned,
   * all elements in values was removed from this field.  If a 
   * failure code is returned, no elements in values were removed.
   *
   * Server-side method only
   */

  public synchronized ReturnVal deleteElements(Vector valuesToDelete, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector currentValues;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
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

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // okay, see if the DBEditObject is willing to allow all of these
    // elements to be removed

    newRetVal = eObj.finalizeDeleteElements(this, valuesToDelete);

    if (newRetVal == null || newRetVal.didSucceed()) 
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

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      } 
    else
      {
	return newRetVal;
      }
  }

  /**
   * Returns true if this field is a vector field and value is contained
   *  in this field.
   *
   * This method always checks for read privileges.
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
   * This method returns true if this field is a vector
   * field and value is contained in this field.
   *
   * This method is server-side only, and never checks for read
   * privileges.
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
	throw new RuntimeException(ex);	// should never happen
      }
  }

  /**
   * This method returns true if this field is a vector
   * field and value is contained in this field.
   *
   * This method is server-side only.
   *
   * @param value The value to look for in this field
   * @param local If false, read permissin is checked for this field
   */

  public boolean containsElement(Object value, boolean local) throws GanyPermissionsException
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
   * Returns a {@link arlut.csd.ganymede.server.fieldDeltaRec fieldDeltaRec} 
   * object listing the changes between this field's state and that
   * of the prior oldField state.
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

  /**
   * Package-domain method to set the owner of this field.
   *
   * Used by the DBObject copy constructor.
   */

  synchronized void setOwner(DBObject owner)
  {
    this.owner = owner;
  }

  // ****
  //
  // Server-side namespace management functions
  //
  // ****

  /** 
   * unmark() is used to make any and all namespace values in this
   * field as available for use by other objects in the same editset.
   * When the editset is committed, any unmarked values will be
   * flushed from the namespace.
   *
   * <b>*Calls synchronized methods on DBNameSpace*</b>
   */

  void unmark()
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
   * Unmark a specific value associated with this field, rather
   * than unmark all values associated with this field.  Note
   * that this method does not check to see if the value is
   * currently associated with this field, it just goes ahead
   * and unmarks it.  This is to be used by the vector
   * modifiers (setElement, addElement, deleteElement, etc.)
   * to keep track of namespace modifications as we go along.
   *
   * If there is no namespace associated with this field, this
   * method will always return true, as a no-op.
   *
   * <b>*Calls synchronized methods on DBNameSpace*</b>
   */

  boolean unmark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
	return true;		// do a no-op
      }

    if (value == null)
      {
	return true;		// no previous value
      }

    return namespace.unmark(editset, value, this);
  }

  /** 
   * mark() is used to mark any and all values in this field as taken
   * in the namespace.  When the editset is committed, marked values
   * will be permanently reserved in the namespace.  If the editset is
   * instead aborted, the namespace values will be returned to their
   * pre-editset status.
   *
   * If there is no namespace associated with this field, this
   * method will always return true, as a no-op.
   *  
   * <b>*Calls synchronized methods on DBNameSpace*</b>
   */

  boolean mark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();

    if (namespace == null)
      {
	return true;		// do a no-op
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
   * Mark a specific value associated with this field, rather than
   * mark all values associated with this field.  Note that this
   * method does not in any way associate this value with this field
   * (add it, set it, etc.), it just marks it.  This is to be used by
   * the vector modifiers (setElement, addElement, etc.)  to keep
   * track of namespace modifications as we go along.
   * 
   * <b>*Calls synchronized methods on DBNameSpace*</b>
   */

  boolean mark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = ((DBEditObject) owner).getEditSet();

    if (namespace == null)
      {
	return false;		// should we throw an exception?
      }

    if (value == null)
      {
	return false;
	//	throw new NullPointerException("null value in mark()");
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
   * Overridable method to verify that an object submitted to this
   * field has an appropriate value.
   *
   * This check is more limited than that of verifyNewValue().. all it
   * does is make sure that the object parameter passes the simple
   * value constraints of the field.  verifyNewValue() does that plus
   * a bunch more, including calling to the DBEditObject hook for the
   * containing object type to see whether it happens to feel like
   * accepting the new value or not.
   *
   * verifyBasicConstraints() is used to double check for values that
   * are already in fields, in addition to being used as a likely
   * component of verifyNewValue() to verify new values.
   */

  public ReturnVal verifyBasicConstraints(Object o)
  {
    return null;
  }

  /**
   * Overridable method to verify that an object submitted to this
   * field has an appropriate value.
   *
   * This method is intended to make the final go/no go decision about
   * whether a given value is appropriate to be placed in this field,
   * by whatever means (vector add, vector replacement, scalar
   * replacement).
   *
   * This method is expected to call the 
   * {@link arlut.csd.ganymede.server.DBEditObject#verifyNewValue(arlut.csd.ganymede.server.DBField,java.lang.Object)} 
   * method on {@link arlut.csd.ganymede.server.DBEditObject} in order to allow custom
   * plugin classes to deny any given value that the plugin might not
   * care for, for whatever reason.  Otherwise, the go/no-go decision
   * will be made based on the checks performed by 
   * {@link arlut.csd.ganymede.server.DBField#verifyBasicConstraints(java.lang.Object) verifyBasicConstraints}.
   *
   * The ReturnVal that is returned may have transformedValue set, in
   * which case the code that calls this verifyNewValue() method
   * should consider transformedValue as replacing the 'o' parameter
   * as the value that verifyNewValue wants to be put into this field.
   * This usage of transformedValue is for canonicalizing input data.
   */

  abstract public ReturnVal verifyNewValue(Object o);

  /** 
   * Overridable method to verify that the current {@link
   * arlut.csd.ganymede.server.DBSession DBSession} / {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} has permission to read
   * values from this field.
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
   * Overridable method to verify that the current {@link
   * arlut.csd.ganymede.server.DBSession DBSession} / {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet} has permission to read
   * values from this field.
   *
   * This version of verifyReadPermission() is intended to be used
   * in a context in which it would be too expensive to make a
   * read-only duplicate copy of a DBObject from the DBObjectBase's
   * object table, strictly for the purpose of associating a
   * GanymedeSession with the DBObject for permissions
   * verification.
   */

   public boolean verifyReadPermission(GanymedeSession gSession)
   {
     if (gSession == null)
       {
	 return true; // we don't know who is looking at us, assume it's a server-local access
       }

     PermEntry pe = gSession.getPerm(owner, getID());

     // if there is no permission explicitly recorded for the field,
     // inherit from the object as a whole

     if (pe == null)
       {
	 pe = gSession.getPerm(owner);
       }

     if (pe == null)
       {
	 return false;
       }

     return pe.isVisible();
   }

  /**
   * Overridable method to verify that the current
   * DBSession / DBEditSet has permission to write
   * values into this field.
   */

  public boolean verifyWritePermission()
  {
    if (owner instanceof DBEditObject)
      {
	PermEntry pe = owner.getFieldPerm(getID());

	if (pe == null)
	  {
	    return false;
	  }

	return pe.isEditable();
      }
    else
      {
	return false;  // if we're not in a transaction, we certainly can't be edited.
      }
  }

  /**
   * Sub-class hook to support elements for which the default
   * equals() test is inadequate, such as IP addresses (represented
   * as arrays of Byte[] objects.
   *
   * Returns -1 if the value was not found in this field.
   *
   * This method assumes that the calling method has already verified
   * that this is a vector field.
   */

  public int indexOfValue(Object value)
  {
    return getVectVal().indexOf(value);
  }

  /** 
   * Returns a Vector of the values of the elements in this field, if
   * a vector.
   *
   * This is intended to be used within the Ganymede server, it
   * bypasses the permissions checking that getValues() does.
   *
   * The server code <b>*must not*</b> make any modifications to the
   * returned vector as doing such may violate the namespace maintenance
   * logic.  Always, <b>always</b>, use the addElement(), deleteElement(),
   * setElement() methods in this class.
   *
   * Remember, this method gives you <b>*direct access</b> to the vector
   * from this field.  Always always clone the Vector returned if you
   * find you need to modify the results you get back.  I'm trusting you
   * here.  Pay attention.
   */

  public Vector getValuesLocal()
  {
    if (!isVector())
      {
	// "Vector method called on a scalar field: {0} in object {1}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    return getVectVal();
  }

  /** 
   * Returns an Object carrying the value held in this field.
   *
   * This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.
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
   * This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later if need be.
   *
   * This method is not synchronized because all operations performed
   * by this method are either synchronized at a lower level or are
   * atomic.
   *
   * Called by {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s
   * {@link arlut.csd.ganymede.server.DBEditObject#checkpoint() checkpoint()}
   * method.
   */

  public Object checkpoint()
  {
    if (isVector())
      {
	return getVectVal().clone();
      }
    else
      {
	return value;
      }
  }

  /**
   * This method is used to basically force state into this field.
   *
   * It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity, and
   * without doing any of the checking or side effects that calling
   * setValue() will typically do.
   *
   * In particular, it is not necessary to subclass this method for
   * use with {@link arlut.csd.ganymede.server.InvidDBField InvidDBField}, since
   * the {@link arlut.csd.ganymede.server.DBEditSet#rollback(java.lang.String) rollback()}
   * method will always rollback all objects in the transaction at the same
   * time.  It is not necessary to have the InvidDBField subclass handle
   * binding/unbinding during rollback, since all objects which could conceivably 
   * be involved in a link will also have their own states rolled back.
   *
   * Called by {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}'s
   * {@link arlut.csd.ganymede.server.DBEditObject#rollback(java.util.Hashtable) rollback()}
   * method.
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
   * This method takes the result of an operation on this field
   * and wraps it with a {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * that encodes an instruction to the client to rescan
   * this field.  This isn't normally necessary for most client
   * operations, but it is necessary for the case in which wizards
   * call DBField.setValue() on behalf of the client, because in those
   * cases, the client otherwise won't know that the wizard modified
   * the field.
   *
   * This makes for a significant bit of overhead on client calls
   * to the field modifier methods, but this is avoided if code 
   * on the server uses setValueLocal(), setElementLocal(), addElementLocal(),
   * or deleteElementLocal() to make changes to a field.
   *
   * If you are ever in a situation where you want to use the local
   * variants of the modifier methods (to avoid permissions checking
   * overhead), but you <b>do</b> want to have the field's rescan
   * information returned, you can do something like:
   *
   * <pre>
   *
   * return field.rescanThisField(field.setValueLocal(null));
   *
   * </pre> 
   */

  public final ReturnVal rescanThisField(ReturnVal original)
  {
    if (original != null && !original.didSucceed())
      {
        return original;
      }

    if (original == null)
      {
	original = new ReturnVal(true);
      }

    if (this.getID() == owner.getLabelFieldID())
      {
	original.setObjectLabelChanged(owner.getInvid(), this.getValueString());
      }
      
    original.addRescanField(getOwner().getInvid(), getID());

    return original;
  }

  /**
   *
   * For debugging
   *
   */

  public String toString()
  {
    return "[" + owner.toString() + ":" + getName() + "]";
  }

  /**
   * Handy utility method for reporting namespace conflict.  This
   * method will work to identify the object and field which is in conflict,
   * and will return an appropriate {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * with an appropriate error dialog.
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

	    // This action could not be completed because "{0}" is already being used in a transaction.
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
	ex.printStackTrace();

	return Ganymede.createErrorDialog(ts.l("getConflictDialog.errorTitle", methodName),
					  ts.l("getConflictDialog.simpleError", conflictValue));
      }
  }

  /**
   * Handy utility method for reporting an attempted duplicate
   * submission to a vector field.
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
   * Handy utility method for reporting an attempted duplicate
   * submission to a vector field.
   */

  public ReturnVal getDuplicateValuesDialog(String methodName, String conflictValues)
  {
    // "Server: Error in {0}"
    // "This action could not be performed because "{0}" are already contained in field {1} in object {2}."
    return Ganymede.createErrorDialog(ts.l("getDuplicateValueDialog.error_in_method_title", methodName),
				      ts.l("getDuplicateValuesDialog.error_body",
					   conflictValues, getName(), owner.getLabel()));
  }

  /**
   * 
   * This method is for use primarily within a Jython context and accessed by
   * calling ".val" on a {@link arlut.csd.ganymede.server.DBField DBField} object,
   * but it can theoretically be used in Java code in lieu of calling
   * {@link arlut.csd.ganymede.server.DBField#getValue getValue} or
   * {@link arlut.csd.ganymede.server.DBField#getValues getValues} (but <b>there
   * are some subtle differences! </b>).
   * 
   * 
   * This method will return this field's value, be it vector or scalar.
   * However, when it encounters an {@link arlut.csd.ganymede.common.Invid Invid}
   * object (either as the value proper or as a member of this fields value
   * vector), it will instead return the
   * {@link arlut.csd.ganymede.server.DBObject DBObject} that the Invid points to.
   * 
   * 
   * @return This field's value. This can take the form of scalar types,
   *         {@link arlut.csd.ganymede.server.DBObject DBObjects}, or a
   *         {@link java.util.Vector Vector}containing either.
   */
  public Object getVal()
  {
    if (isVector())
      {
        Vector values = getValuesLocal();
        
        /* Dereference each Invid object in the values vector */
        List returnList = new ArrayList(values.size());
        for (Iterator iter = values.iterator(); iter.hasNext();)
          {
            returnList.add(dereferenceObject(iter.next()));
          }
          
        return returnList;
      }
    else
      {
        /* Return the field value, and dereference it if it is an Invid */
        return dereferenceObject(getValueLocal());
      }
  }
  
  /**
   * If the argument is an Invid, this method will return a reference to the
   * actual DBObject the Invid points to. Otherwise, it returns the same object
   * that was passed in.
   *
   * @param obj 
   * @return a DBObject if <b>obj</b> is an Invid, otherwise return <b>obj</b>
   */
  private Object dereferenceObject(Object obj)
  {
    if (obj instanceof Invid)
      {
        return Ganymede.db.getObject((Invid) obj);
      }
    else
      {
        return obj;
      }
  }
}
