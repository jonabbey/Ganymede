/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.71 $
   Last Mod Date: $Date: 1999/06/15 02:48:17 $
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

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/

/**
 * <P>This abstract base class encapsulates the basic logic for fields in the
 * Ganymede {@link arlut.csd.ganymede.DBStore DBStore},
 * including permissions and unique value handling.</P>
 *
 * <P>DBFields are the actual carriers of field value in the Ganymede server.  Each
 * {@link arlut.csd.ganymede.DBObject DBObject} holds a set of DBFields in
 * a {@link arlut.csd.ganymede.DBFieldTable DBFieldTable}.  Each DBField is
 * associated with a {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
 * field definition (see {@link arlut.csd.ganymede.DBField#getFieldDef() getFieldDef()})
 * which defines the type of the field as well as various generic and type-specific
 * attributes for the field.  The DBObjectBaseField information is created and
 * edited with the Ganymede schema editor.</P>
 *
 * <P>DBField is an abstract class.  There is a different subclass of DBField
 * for each kind of data that can be held in the Ganymede server, as follows:</P>
 *
 * <UL>
 * <LI>{@link arlut.csd.ganymede.StringDBField StringDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.BooleanDBField BooleanDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.NumericDBField NumericDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.DateDBField DateDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.InvidDBField InvidDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.IPDBField IPDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.PasswordDBField PasswordDBField}</LI>
 * <LI>{@link arlut.csd.ganymede.PermissionMatrixDBField PermissionMatrixDBField}</LI>
 * </UL>
 *
 * <P>Each DBField subclass is responsible for writing itself to disk
 * on command with the {@link
 * arlut.csd.ganymede.DBField#emit(java.io.DataOutput) emit()} method,
 * and reading its state in with the {@link
 * arlut.csd.ganymede.DBField#receive(java.io.DataInput) receive()}
 * method.  Each DBField subclass may also have extensive special
 * logic to handle special operations on fields of the appropriate
 * type.  For instance, the InvidDBField class has lots and lots of
 * logic for handling the bi-directional object linking that the
 * server depends on for its object handling.  Mostly the DBField
 * subclasses provide customization that modifies how things like
 * {@link arlut.csd.ganymede.DBField#setValue(java.lang.Object)
 * setValue()} and {@link arlut.csd.ganymede.DBField#getValue()
 * getValue()} work, but PasswordDBField and PermissionMatrixDBField
 * don't fit with the standard generic value-container model, and
 * contain their own methods for manipulating and accessing data held
 * in the Ganymede database. Most DBField subclasses only allow a
 * single value to be held, but StringDBField, InvidDBField, and
 * IPDBField support vectors of values.</P>
 *
 * <P>The Ganymede client can directly access fields in RMI-published objects
 * using the {@link arlut.csd.ganymede.db_field db_field} RMI interface.  Each
 * concrete subclass of DBField has its own special RMI interface which provides
 * special methods for the client.  Adding a new data type to the Ganymede server
 * will involve creating a new DBField subclass, as well as a new RMI interface
 * for any special field methods.  All client code would also need to be modified
 * to be aware of the new field type.  DBObjectBaseField and DBObject would also
 * need to be modified to be aware of the new field type for schema editing and
 * object loading.  The schema editor would have to be modified as well.</P>
 *
 * <P>But you can do it if you absolutely have to.  Just be careful and take a good
 * look around at the code.</P>
 *
 * <P>Note that while DBField was designed to be subclassed, it should only be
 * necessary for adding a new data type to the server.  All other likely 
 * customizations you'd want to do are handled by
 * {@link arlut.csd.ganymede.DBEditObject DBEditObject} customization methods.  Most
 * DBField methods at some point call methods on the DBObject/DBEditObject
 * that contains it.  All methods that cause changes to fields call out to
 * finalizeXXX() and/or wizardHook() methods in DBEditObject.  Consult the
 * DBEditObject customization guide for details on the field/object interactions.</P>
 *
 * <P>An important note about synchronization: it is possible to encounter a
 * condition called a <b>nested monitor deadlock</b>, where a synchronized
 * method on a field can block trying to enter a synchronized method on
 * a {@link arlut.csd.ganymede.DBSession DBSession}, 
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}, or 
 * {@link arlut.csd.ganymede.DBEditObject DBEditObject} object that is itself blocked
 * on another thread trying to call a synchronized method on the same field.</P>
 *
 * <P>To avoid this condition, no field methods that call synchronized methods on
 * other objects should themselves be synchronized in any fashion.</P> 
 */

public abstract class DBField implements Remote, db_field, Cloneable {

  /**
   * the object's current value, for scalars
   */

  Object value = null;
  
  /**
   * the object's new value, for use by custom verification classes
   */

  Object newValue = null;

  /**
   * the object's current value, for vectors
   */

  Vector values;

  /**
   * The object this field is contained within
   */

  DBObject owner;

  /**
   * <P>Link to the field definition for this field</P>
   *
   * <P>For a very minor memory optimization, we could ditch
   * this field and have the {@link arlut.csd.ganymede.DBField#getFieldDef() getFieldDef()}
   * method do a dynamic look-up based on {@link arlut.csd.ganymede.DBField#owner owner}
   * and {@link arlut.csd.ganymede.DBField#fieldID fieldID}, maybe.</P>
   */

  DBObjectBaseField definition;
  
  /**
   * required for use with {@link arlut.csd.ganymede.DBFieldTable DBFieldTable}.
   */

  DBField next = null;

  /**
   * <P>Field number for this field in the containing {@link arlut.csd.ganymede.DBObject DBObject}.</P>
   */

  short fieldID = -1;

  /**
   * <P>Permissions record for this field in the current
   * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} context,
   * used when an object has been checked out for viewing or editing
   * by GanymedeSession to avoid redundant synchronized calls on GanymedeSession,
   * both for dead lock prevention and for speed-ups.</P>
   *
   * <P>Doing a permissions look-up in GanymedeSession is a relatively expensive
   * operation, after all.</P>
   */

  PermEntry
    permCache = null;

  /* -- */

  public DBField()
  {
    permCache = null;
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
	throw new IllegalArgumentException("scalar accessor called on vector field " + getName());
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
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    return values.elementAt(index);
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
	return values.size();
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
   * <P>This method is responsible for writing out the contents of
   * this field to an binary output stream.  It is used in writing
   * fields to the ganymede.db file and to the journal file.</P>
   *
   * <P>This method only writes out the value contents of this field.
   * The {@link arlut.csd.ganymede.DBObject DBObject}
   * {@link arlut.csd.ganymede.DBObject#emit(java.io.DataOutput) emit()}
   * method is responsible for writing out the field identifier information
   * ahead of the field's contents.</P>
   */

  abstract void emit(DataOutput out) throws IOException;

  /**
   * <P>This method is responsible for reading in the contents of
   * this field from an binary input stream.  It is used in reading
   * fields from the ganymede.db file and from the journal file.</P>
   *
   * <P>The code that calls receive() on this field is responsible for
   * having read enough of the binary input stream's context to
   * place the read cursor at the point in the file immediately after
   * the field's id and type information has been read.</P>
   */

  abstract void receive(DataInput in) throws IOException;

  /**
   * <P>Returns true if obj is a field with the same value(s) as
   * this one.</P>
   *
   * <P>This method is ok to be synchronized because it does not
   * call synchronized methods on any other object.</P>
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
   * @see arlut.csd.ganymede.db_field
   */

  public final FieldTemplate getFieldTemplate()
  {
    return getFieldDef().template;
  }

  /**
   *
   * Returns a handy field description packet for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final FieldInfo getFieldInfo()
  {
    return new FieldInfo(this);
  }

  /**
   *
   * Returns the schema name for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final String getName()
  {
    return getFieldDef().getName();
  }

  /**
   *
   * Returns the field # for this field.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final short getID()
  {
    if (fieldID == -1)
      {
	fieldID = getFieldDef().getID();
      }

    return fieldID;
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
   * @see arlut.csd.ganymede.db_field
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
   * @see arlut.csd.ganymede.db_field
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
   * @see arlut.csd.ganymede.db_field
   *
   */

  public final short getType()
  {
    return getFieldDef().getType();
  }

  /**
   *
   * Returns the display order for this field from the schema.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public final short getDisplayOrder()
  {
    return getFieldDef().getDisplayOrder();
  }

  /**
   *
   * Returns a String representing the value of this field.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  abstract public String getValueString();

  /**
   * <P>Returns a String representing a reversible encoding of the
   * value of this field.  Each field type will have its own encoding,
   * suitable for embedding in a {@link arlut.csd.ganymede.DumpResult DumpResult}.</P>
   *
   * @see arlut.csd.ganymede.db_field
   */

  abstract public String getEncodingString();

  /**
   * <P>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</P>
   *
   * <P>If there is no change in the field, null will be returned.</P>
   */

  abstract public String getDiffString(DBField orig);

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public synchronized boolean isDefined()
  {
    if (isVector())
      {
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
   * <P>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of DBField may
   * implement this in different ways, if simply setting the field's
   * value member to null is not appropriate.  Any namespace values claimed
   * by the field will be released, and when the transaction is
   * committed, this field will be released.</P>
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isVector())
      {
	if (!isEditable(local))	// *sync* GanymedeSession possible
	  {
	    return Ganymede.createErrorDialog("Couldn't clear field",
					      "DBField.setUndefined(): couldn't " +
					      "clear a vector element from field " +
					      getName());
	  }

	// we know we're editable, so..

	ReturnVal tempResult = null;

	DBSession session = ((DBEditObject) owner).getSession();
	String key = "DBField.setUndefined:" + new Date();

	session.checkpoint(key);

	while ((tempResult == null || tempResult.didSucceed()) && 
	       values.size() > 0)
	  {
	    tempResult = deleteElement(0, local);
	  }

	if (tempResult != null && !tempResult.didSucceed())
	  {
	    session.rollback(key);
	  }
	else
	  {
	    session.popCheckpoint(key);
	  }
	
	return tempResult;
      }
    else
      {
	return setValue(null, local);
      }
  }

  /**
   *
   * Returns true if this field is a vector, false
   * otherwise.
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final boolean isVector()
  {
    return getFieldDef().isArray();
  }

  /**
   * <P>Returns true if this field is editable, false
   * otherwise.</P>
   *
   * <P>Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject}.</P>
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final boolean isEditable()
  {
    return isEditable(false);
  }

  /**
   * <P>Returns true if this field is editable, false
   * otherwise.</P>
   *
   * <P>Note that DBField are only editable if they are
   * contained in a subclass of
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject}.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P><B>*Deadlock Hazard.*</B></P>
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

    if (!local && !verifyWritePermission()) // *sync* possible on GanymedeSession
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    // if our owner has already started the commit
    // process, we can't allow any changes

    if (eObj.isCommitting())
      {
	return false;
      }

    return true;
  }

  /**
   *
   * Returns true if this field is a built-in field, common
   * to all non-embedded objects.
   *
   * @see arlut.csd.ganymede.db_field
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
   * @see arlut.csd.ganymede.db_field
   */

  public final boolean isVisible()
  {
    return verifyReadPermission() && 
      getFieldDef().base.objectHook.canSeeField(null, this);
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
    return definition;
  }

  /**
   *
   * Returns the value of this field, if a scalar.  An IllegalArgumentException
   * will be thrown if this field is a vector.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public Object getValue()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field " + getName());
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector " + getName());
      }

    // if this field is virtualized, let our customizer provide the
    // value

    if (owner.objectBase.objectHook.virtualizeField(getID()))
      {
	return owner.objectBase.objectHook.getVirtualValue(this);
      }

    return value;
  }

  /**
   * <P>This method is used to allow server-side custom code to get access to
   * a proposed new value before it is finalized, used when making a change
   * to this field causes other fields to be changed which need to insure
   * that this field has an appropriate value first.</P>
   *
   * <P><B>This method is not intended to be accessible to the client.</B></P>
   *
   * <P>This method does not support virtualized fields.</P>
   *
   * <P>This method will only have a useful value during the
   * course of the containing objects' finalizeSetValue() call.  It
   * is intended that this field will set the new value in setValue(),
   * call owner.finalizeSetValue(), then clear the newValue.</P>
   *
   * <P>What, this code a hack?</P>
   */

  public Object getNewValue()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector " + getName());
      }

    return newValue;
  }

  /**
   *
   * <P>This method is used to allow server-side custom code to get access to
   * a proposed new value before it is finalized, used when making a change
   * to this field causes other fields to be changed which need to insure
   * that this field has an appropriate value first.</P>
   *
   * <P><B>This method is not intended to be accessible to the client.</B></P>
   *
   * <P>This method does not support virtualized fields.</P>
   *
   * <P>This method will only have a useful value during the
   * course of the containing objects' finalizeSetValue() call.  It
   * is intended that this field will set the new value in setValue(),
   * call owner.finalizeSetValue(), then clear the newValue.</P>
   *
   * <P>What, this code a hack?</P>
   */

  public Object getOldValue()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector " + getName());
      }

    return value;
  }

  /**
   *
   * Returns the value of this field, if a scalar.  This method
   * is intended to be used by the virtualizing hook to use to
   * return the default value if the virtualizer wants to pass
   * through.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public Object getValue(boolean virtualize)
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field " + getName());
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector " + getName());
      }

    if (virtualize && owner.objectBase.objectHook.virtualizeField(getID()))
      {
	return owner.objectBase.objectHook.getVirtualValue(this);
      }

    return value;
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <P>This method is intended to be called by code that needs to go
   * through the permission checking regime, and that needs to have
   * rescan information passed back.  This includes most wizard
   * setValue calls.</P>
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   */

  public final ReturnVal setValue(Object value)
  {
    ReturnVal result, rescan;

    /* -- */

    // do the thing, calling into our subclass

    result = setValue(value, false);

    return rescanThisField(result);
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P><B>This method is server-side only, and bypasses
   * permissions checking.</B></P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   */

  public final ReturnVal setValueLocal(Object value)
  {
    return setValue(value, true);
  }

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P><B>This method is server-side only.</B></P>
   *
   * <P>The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.</P>
   *
   * <P>This method will be overridden by DBField subclasses with special
   * needs.</P>
   *
   * @param value Value to set this field to
   * @param local If true, permissions checking will be skipped
   */

  public synchronized ReturnVal setValue(Object value, boolean local)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* possible
      {
	return Ganymede.createErrorDialog("Field Permissions Error",
					  "don't have permission to change field /  non-editable object for field " +
					  getName() + " in object " + owner.getLabel());
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar method called on a vector field for field " + getName());
      }

    if (this.value == value)
      {
	return retVal;		// no change
      }

    retVal = verifyNewValue(value);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETVAL, value, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
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

	if (value != null)
	  {
	    if (!mark(value))
	      {
		if (this.value != null)
		  {
		    mark(this.value); // we aren't clearing the old value after all
		  }
		
		setLastError("value " + value + " already taken in namespace");

		return Ganymede.createErrorDialog("Server: Error in DBField.setValue()",
						  "value " + value +
						  " already taken in namespace");
	      }
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    this.newValue = value;

    newRetVal = eObj.finalizeSetValue(this, value);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	if (value != null)
	  {
	    this.value = value;
	  }
	else
	  {
	    this.value = null;
	  }

	this.newValue = null;

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
	this.newValue = null;

	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    unmark(value);
	    mark(this.value);
	  }

	// go ahead and return the dialog that was set by finalizeSetValue().

	return newRetVal;
      }
  }

  /** 
   * <p>Returns a Vector of the values of the elements in this field,
   * if a vector.</p>
   *
   * <p>This is only valid for vectors.  If the field is a scalar, use
   * getValue().</p>
   *
   * @see arlut.csd.ganymede.db_field
   */

  public Vector getValues()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field " + 
					   getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    return values; // this is ok, since this is being serialized. the client's not
                   // gaining the ability to add or remove items from this field
  }

  /**
   *
   * <p>Returns the value of an element of this field,
   * if a vector.</p>
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public Object getElement(int index)
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field " + getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (index < 0)
      {
	throw new IllegalArgumentException("invalid index " + index + " on field " + getName());
      }

    return values.elementAt(index);
  }

  /**
   * <p>Sets the value of an element of this field, if a vector.</p>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.</p>
   *
   * <p>The ReturnVal resulting from a successful setElement will
   * encode an order to rescan this field.</p>
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   */
  
  public final ReturnVal setElement(int index, Object value)
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

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    return rescanThisField(setElement(index, value, false));
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
   * @see arlut.csd.ganymede.DBSession
   *
   */
  
  public final ReturnVal setElementLocal(int index, Object value)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (value == null)
      {
	return Ganymede.createErrorDialog("Error, bad value",
					  "Null value passed to " + owner.getLabel() + ":" + 
					  getName() + ".setElement()");
      }

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    return setElement(index, value, true);
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
   */
  
  public synchronized ReturnVal setElement(int index, Object value, boolean local)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible.
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object, field " +
					   getName());
      }

    retVal = verifyNewValue(value);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), value);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index));

	if (!mark(value))
	  {
	    mark(values.elementAt(index)); // we aren't clearing the old value after all

	    setLastError("value " + value + " already taken in namespace");

	    return Ganymede.createErrorDialog("Server: Error in DBField.setElement()",
					      "value " + value +
					      " already taken in namespace");
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    newRetVal = eObj.finalizeSetElement(this, index, value);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	values.setElementAt(value, index);

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
	    unmark(value);
	    mark(values.elementAt(index));
	  }

	// return the error dialog from finalizeSetElement().

	return newRetVal;
      }
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
   * @see arlut.csd.ganymede.db_field
   */

  public final ReturnVal addElement(Object value)
  {
    return rescanThisField(addElement(value, false));
  }

  /**
   * <P>Adds an element to the end of this field, if a vector.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   */

  public final ReturnVal addElementLocal(Object value)
  {
    return addElement(value, true);
  }

  /**
   * <P>Adds an element to the end of this field, if a vector.</P>
   *
   * <P>Server-side method only</P>
   *
   * <P>The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   */

  public synchronized ReturnVal addElement(Object value, boolean local)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " + 
					   getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    // verifyNewValue should setLastError for us.

    retVal = verifyNewValue(value);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    if (size() >= getMaxArraySize())
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					  "Field " + getName() + 
					  " already at or beyond array size limit");
      }

    eObj = (DBEditObject) owner;

    if (!local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, value, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    ns = getNameSpace();

    if (ns != null)
      {
	if (!mark(value))	// *sync* DBNameSpace
	  {
	    setLastError("value " + value + " already taken in namespace");

	    return Ganymede.createErrorDialog("Server: Error in DBField.addElement()",
					      "value " + value + 
					      " already taken in namespace");
	  }
      }

    newRetVal = eObj.finalizeAddElement(this, value);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	values.addElement(value);

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
	    unmark(value);	// *sync* DBNameSpace
	  }

	// return the error dialog created by finalizeAddElement

	return newRetVal;
      }
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
   * @see arlut.csd.ganymede.db_field
   */

  public final ReturnVal deleteElement(int index)
  {
    return rescanThisField(deleteElement(index, false));
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public final ReturnVal deleteElementLocal(int index)
  {
    return deleteElement(index, true);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public synchronized ReturnVal deleteElement(int index, boolean local)
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* GanymedeSession possible
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " + 
					   getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if ((index < 0) || (index >= values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index + 
					   " in deleting element in field " + getName());
      }

    eObj = (DBEditObject) owner;

    if (!local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, new Integer(index), null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index)); // *sync* DBNameSpace
      }

    newRetVal = eObj.finalizeDeleteElement(this, index);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	values.removeElementAt(index);

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
	if (ns != null)
	  {
	    mark(values.elementAt(index)); // *sync* DBNameSpace
	  }

	// return the error dialog from finalizeDeleteElement().

	return newRetVal;
      }
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
   * @see arlut.csd.ganymede.db_field
   */

  public final ReturnVal deleteElement(Object value)
  {
    return rescanThisField(deleteElement(value, false));
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public final ReturnVal deleteElementLocal(Object value)
  {
    return deleteElement(value, true);
  }

  /**
   * <p>Deletes an element of this field, if a vector.</p>
   *
   * <p>Server-side method only</p>
   *
   * <p>The ReturnVal resulting from a successful deleteElement will
   * encode an order to rescan this field.</p>
   */

  public synchronized ReturnVal deleteElement(Object value, boolean local)
  {
    DBNameSpace ns;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))	// *sync* GanymedeSession possible
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " +
					   getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    if (value == null)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElement()",
					  "Could not delete null value from field " + getName());
      }

    int index = indexOfValue(value);

    if (index == -1)
      {
	return Ganymede.createErrorDialog("Server: Error in DBField.deleteElement()",
					  "Could not delete value " + value +
					  ", not present in field " + getName());
      }

    return deleteElement(index, local);	// *sync* DBNameSpace possible
  }

  /**
   * <p>Returns true if this field is a vector field and value is contained
   *  in this field.</p>
   *
   * <p>This method always checks for read privileges.</p>
   *
   * @param value The value to look for in this field
   *
   * @see arlut.csd.ganymede.db_field
   */

  public final boolean containsElement(Object value)
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
    return containsElement(value, true);
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

  public boolean containsElement(Object value, boolean local)
  {
    if (!local && !verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field " + getName());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    return (indexOfValue(value) != -1);
  }

  /**
   * Returns a {@link arlut.csd.ganymede.fieldDeltaRec fieldDeltaRec} 
   * object listing the changes between this field's state and that
   * of the prior oldField state.
   */

  public fieldDeltaRec getVectorDiff(DBField oldField)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + getName());
      }

    if (oldField == null)
      {
	throw new IllegalArgumentException("can't compare fields.. oldField is null");
      }

    if ((oldField.getID() != getID()) ||
	(oldField.getObjTypeID() != getObjTypeID()))
      {
	throw new IllegalArgumentException("can't compare fields.. incompatible fields");
      }

    /* - */

    fieldDeltaRec deltaRec = new fieldDeltaRec(getID());
    Vector oldValues = oldField.values;
    Vector newValues = values;
    Object compareValue;

    // make hashes of our before and after state so that we
    // can do the add/delete calculations in a linear order.

    Hashtable oldHash = new Hashtable(oldValues.size() + 1, 1.0f);

    for (int i = 0; i < oldValues.size(); i++)
      {
	compareValue = oldValues.elementAt(i);

	if (compareValue instanceof Byte[])
	  {
	    compareValue = new IPwrap((Byte[]) compareValue);
	  }

	oldHash.put(compareValue, compareValue);
      }

    Hashtable newHash = new Hashtable(newValues.size()+1, 1.0f);

    for (int i = 0; i < newValues.size(); i++)
      {
	compareValue = newValues.elementAt(i);

	if (compareValue instanceof Byte[])
	  {
	    compareValue = new IPwrap((Byte[]) compareValue);
	  }

	newHash.put(compareValue, compareValue);
      }

    // and do the compare

    for (int i = 0; i < oldValues.size(); i++)
      {
	compareValue = oldValues.elementAt(i);
	
	if (compareValue instanceof Byte[])
	  {
	    compareValue = new IPwrap((Byte[]) compareValue);
	  }

	if (!newHash.containsKey(compareValue))
	  {
	    deltaRec.delValue(compareValue);
	  }
      }

    for (int i = 0; i < newValues.size(); i++)
      {
	compareValue = newValues.elementAt(i);

	if (compareValue instanceof Byte[])
	  {
	    compareValue = new IPwrap((Byte[]) compareValue);
	  }
	    
	if (!oldHash.containsKey(compareValue))
	  {
	    deltaRec.addValue(compareValue);
	  }
      }

    return deltaRec;
  }

  /**
   * <p>Package-domain method to set the owner of this field.</p>
   *
   * <p>Used by the DBObject copy constructor.</p>
   */

  synchronized void setOwner(DBObject owner)
  {
    this.owner = owner;
    permCache = null;
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

  boolean unmark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = owner.editset;

    if (namespace == null)
      {
	return false;
      }

    if (!isVector())
      {
	return namespace.unmark(editset, this.key());
      }
    else
      {
	synchronized (namespace)
	  {
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.testunmark(editset, key(i)))
		  {
		    return false;
		  }
	      }
	
	    for (int i = 0; i < size(); i++)
	      {
		if (!namespace.unmark(editset, key(i)))
		  {
		    throw new RuntimeException("error: testunmark / unmark inconsistency");
		  }
	      }

	    return true;
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
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  boolean unmark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = owner.editset;

    if (namespace == null)
      {
	return false;		// should we throw an exception?
      }

    if (value == null)
      {
	return true;		// no previous value
      }

    return namespace.unmark(editset, value);
  }

  /** 
   * <p>mark() is used to mark any and all values in this field as taken
   * in the namespace.  When the editset is committed, marked values
   * will be permanently reserved in the namespace.  If the editset is
   * instead aborted, the namespace values will be returned to their
   * pre-editset status.</p>
   *  
   * <p><b>*Calls synchronized methods on DBNameSpace*</b></p>
   */

  boolean mark()
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = owner.editset;

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

  boolean mark(Object value)
  {
    DBNameSpace namespace;
    DBEditSet editset;

    /* -- */

    namespace = getFieldDef().getNameSpace();
    editset = owner.editset;

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
  // Server-side convenience methods
  //
  // ****

  void setLastError(String val)
  {
    try
      {
	owner.editset.session.setLastError(val);
      }
    catch (NullPointerException ex)
      {
      }
  }

  String getLastError()
  {
    try
      {
	return owner.editset.session.lastError;
      }
    finally
      {
	return null;
      }
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
   * Overridable method to verify that an object
   * submitted to this field has an appropriate
   * value.
   */

  abstract public ReturnVal verifyNewValue(Object o);

  /**
   * Overridable method to verify that the current
   * DBSession / DBEditSet has permission to read
   * values from this field.
   */

   public boolean verifyReadPermission()
   {
     GanymedeSession gSession;
     PermEntry perm1, perm2;

     /* -- */

     if (!((owner.editset != null && owner.editset.getSession() != null) ||
	   (owner.gSession != null)))
       {
	 return true; // we don't know who is looking at us, assume it's a server-local access
       }

     synchronized (this)
       {
	 updatePermCache();		// *sync* this GanymedeSession
	 
	 if (permCache == null)
	   {
	     return false;
	   }
	 
	 return permCache.isVisible();
       }
   }

  /**
   * Overridable method to verify that the current
   * DBSession / DBEditSet has permission to write
   * values into this field.
   */

  public boolean verifyWritePermission()
  {
    if (owner.editset != null)
      {
	synchronized (this)
	  {
	    updatePermCache();	// *sync* this GanymedeSession

	    if (permCache == null)
	      {
		return false;
	      }
	    
	    return permCache.isEditable();
	  }
      }
    else
      {
	return false;  // if we're not in a transaction, we certainly can't be edited.
      }
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
    return values.indexOf(value);
  }

  /**
   * This method clears this field's permCache.
   */

  public synchronized void clearPermCache()
  {
    permCache = null;
  }

  /**
   * <P>This method is a deadlock hazard, due to its
   * calling synchronized methods on 
   * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession},
   * but since it caches permissions, the window
   * of vulnerability is significantly reduced.</P>
   */

  synchronized private void updatePermCache()
  {
    GanymedeSession gSession;

    /* -- */

    if (permCache != null)
      {
	return;
      }

    if (owner.editset != null && owner.editset.getSession() != null)
      {
	gSession = owner.editset.getSession().getGSession();
      }
    else if (owner.gSession != null)
      {
	gSession = owner.gSession;
      }
    else
      {
	return;			// can't update permCache
      }

    permCache = gSession.getPerm(owner, getID()); // *sync* on gSession

    // If we don't specifically have a permission record for this
    // field, inherit the permissions for our owner
    
    if (permCache == null)
      {
	permCache = gSession.getPerm(owner); // *sync* on gSession
      }
  }

  /** 
   * <P>Returns a Vector of the values of the elements in this field, if
   * a vector.</P>
   *
   * <P>This is intended to be used within the Ganymede server, it
   * bypasses the permissions checking that getValues() does.</P>
   *
   * <P>The server code <b>*must not*</b> make any modifications to the
   * returned vector as doing such may violate the namespace maintenance
   * logic.  Always, <b>always</b>, use the addElement(), deleteElement(),
   * setElement() methods in this class.</P>
   *
   * <P>Remember, this method gives you <b>*direct access</b> to the vector
   * from this field.  Always always clone the Vector returned if you
   * find you need to modify the results you get back.  I'm trusting you
   * here.  Pay attention.</P>
   */

  public Vector getValuesLocal()
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " + 
					   getName());
      }

    return values;
  }

  /** 
   * <P>Returns an Object carrying the value held in this field.</P>
   *
   * <P>This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.</P>
   */

  public Object getValueLocal()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field " + 
					   getName());
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
   * <P>This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later if need be.</P>
   *
   * <P>This method is not synchronized because all operations performed
   * by this method are either synchronized at a lower level or are
   * atomic.</P>
   */

  public Object checkpoint()
  {
    if (isVector())
      {
	return values.clone();
      }
    else
      {
	return value;
      }
  }

  /**
   * <P>This method is used to basically force state into this field.</P>
   *
   * <P>It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity.</P>
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
	    this.values = (Vector) oldval;
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
   * <P>This method takes the result of an operation on this field
   * and wraps it with a {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * that encodes an instruction to the client to rescan
   * this field.  This isn't normally necessary for most client
   * operations, but it is necessary for the case in which wizards
   * call DBField.setValue() on behalf of the client, because in those
   * cases, the client otherwise won't know that the wizard modified
   * the field.</P>
   *
   * <P>This makes for a significant bit of overhead on client calls
   * to the field modifier methods, but this is avoided if code 
   * on the server uses setValueLocal(), setElementLocal(), addElementLocal(),
   * or deleteElementLocal() to make changes to a field.</P>
   *
   * <P>If you are ever in a situation where you want to use the local
   * variants of the modifier methods (to avoid permissions checking
   * overhead), but you <b>do</b> want to have the field's rescan
   * information returned, you can do something like:</P>
   *
   * <pre>
   *
   * return field.rescanThisField(field.setValueLocal(null));
   *
   * </pre> 
   */

  public final ReturnVal rescanThisField(ReturnVal original)
  {
    ReturnVal rescan;

    /* -- */

    if (original != null && !original.didSucceed())
      {
	return original;
      }

    if (original == null)
      {
	original = new ReturnVal(true);
      }

    original.addRescanField(getOwner().getInvid(), getID());

    return original;
  }
}

