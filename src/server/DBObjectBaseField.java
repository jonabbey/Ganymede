/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Release: $Name:  $
   Version: $Revision: 1.59 $
   Last Mod Date: $Date: 2000/01/04 05:57:29 $
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
import java.rmi.server.UnicastRemoteObject;
import gnu.regexp.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

------------------------------------------------------------------------------*/

/**
 * <p>An entry in the Ganymede server's {@link arlut.csd.ganymede.DBStore DBStore}
 * schema dictionary.  DBStore contains a collection of 
 * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} objects, which define
 * the schema information for a particular type of object held in the Ganymede
 * database.  A DBObjectBaseField is contained within a DBObjectBase, and defines
 * the name, id, type, and constraints of a particular field that can be held
 * in {@link arlut.csd.ganymede.DBObject DBObjects} of that type, including
 * a controlling {@link arlut.csd.ganymede.DBNameSpace DBNameSpace}, if
 * appropriate.</P>
 *
 * <P>Each {@link arlut.csd.ganymede.DBField DBField} held in the server's
 * database holds a reference to a DBObjectBaseField, and the DBField's methods
 * will consult the DBObjectBaseField during run-time to make decisions based
 * on specified constraints defined in the DBObjectBaseField.</P>
 *
 * <P>The Ganymede schema editor uses the {@link arlut.csd.ganymede.BaseField BaseField}
 * remote interface to make changes to a DBObjectBaseField's constraint information
 * during schema editing. The Ganymede client may also use the BaseField interface
 * to learn about the field's type information, but it may also download a
 * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate} that carries a
 * DBObjectBaseField's type information in an efficiently retrieved summary.</P>
 */

public final class DBObjectBaseField extends UnicastRemoteObject implements BaseField, FieldType {

  /**
   * Object type definition for the database object class we are member of
   */

  DBObjectBase base;

  /**
   * name of this field
   */

  String field_name = null;

  /**
   * id of this field in the current object type
   */

  short field_code;

  /**
   * {@link arlut.csd.ganymede.FieldType Field Type} for this field
   */

  short field_type;

  /**
   * display order for this field, used to display fields in order on the
   * client.
   */

  short field_order;

  /**
   * Can this field be edited in the schema editor?  Will be false for
   * fields that are required for the server's own functioning in the
   * mandatory object types.
   */

  boolean editable = true;

  /** 
   * Can this field be removed from the owning Base in the schema
   * editor?  Will be false for fields that are required for the
   * server's own functioning in the mandatory object types.  
   */

  boolean removable = true;

  /**
   * Should this field be displayed to the client?  May be false for
   * some fields intended for 'scratch-pad' use, as in serving as
   * anchors for compound namespace use.  (i.e., the case where two
   * fields in an object are considered together for namespace use..
   * in this case, a hidden field might be defined with custom code
   * updating the hidden field whenever either of the two visible
   * fields are changed.  The hidden field will have a value of
   * XY where X is the contents of field 1 and Y the contents of
   * field 2.  Oh, never mind.)
   */

  boolean visibility = true;

  /** 
   * is this field a built-in universal field, applicable to all
   * (nonembedded) object types?
   */

  boolean builtIn = false;

  /**
   * name of class to manage user interactions with this field
   */

  String classname = null;

  /**
   * string to be displayed in the client as a tooltip explaining this field
   */

  String comment = null;

  /**
   * class object containing the code managing dbfields of this type
   */

  Class classdef;

  /**
   * true if this field is an array type
   */

  boolean array = false;

  /**
   * true if we're in the middle of loading
   */

  boolean loading = false;

  // array attributes

  /**
   * max length of array
   */

  short limit = Short.MAX_VALUE;

  // boolean attributes

  boolean labeled = false;
  String trueLabel = null;
  String falseLabel = null;

  // string attributes

  short minLength = 0;
  short maxLength = Short.MAX_VALUE;
  String okChars = null;
  String badChars = null;
  DBNameSpace namespace = null;
  boolean multiLine = false;

  /**
   * Regular Expression string for input-filtering
   * in {@link arlut.csd.ganymede.StringDBField}s.
   */

  String regexpPat = null;	// introduced in ganymede.db version 1.14

  /**
   * Compiled regular expression for input-filtering
   * in {@link arlut.csd.ganymede.StringDBField}s.
   */

  gnu.regexp.RE regexp = null;

  // invid attributes

  boolean editInPlace = false;
  short allowedTarget = -1;	// no target restrictions
  short targetField = -1;	// no field symmetry.. we use the DBStore backPointers structure by default

  // password attributes

  boolean crypted = true;	// UNIX encryption is the default.
  boolean md5crypted = false;	// OpenBSD style md5crypt() is not
  boolean storePlaintext = false; // nor is plaintext

  // schema editing

  /**
   * If we are being edited, this will point to an instance
   * of a server-side schema editing class.
   */

  DBSchemaEdit editor;
  boolean changed;

  /**
   * Downloadable FieldTemplate representing the constant field type
   * attributes represented by this DBObjectBaseField.  This template
   * is regenerated whenever clearEditor() is called, upon schema
   * editing completion.
   */

  FieldTemplate template;

  // for DBBaseFieldTable

  DBObjectBaseField next = null;

  /* -- */

  /**
   *
   * Generic field constructor.
   *
   */

  DBObjectBaseField(DBObjectBase base) throws RemoteException
  {
    this.base = base;
    field_name = "";
    classname = "";
    comment = "";
    trueLabel = "";
    falseLabel = "";
    //    okChars = "";
    //    badChars = "";
    
    field_code = 0;
    field_type = 0;
    field_order = 0;
    editor = null;
    changed = false;
  }

  /**
   *
   * Editing base constructor.  This constructor is used to create a new
   * field definition object in an editing context. 
   *
   */

  DBObjectBaseField(DBObjectBase base, DBSchemaEdit editor) throws RemoteException
  {
    this(base);
    this.editor = editor;
  }

  /**
   *
   * Receive constructor.
   *
   */

  DBObjectBaseField(DataInput in, DBObjectBase base) throws IOException, RemoteException
  {
    this(base);
    receive(in);
    template = new FieldTemplate(this);
  }

  /**
   * <p>Copy constructor, used during schema editing.</p>
   *
   * <p><b>IMPORTANT: BE SURE TO ALWAYS EDIT THIS METHOD IF YOU ADD ANY FIELDS
   * TO THIS CLASS!</b></p>
   */

  DBObjectBaseField(DBObjectBaseField original, DBSchemaEdit editor) throws RemoteException
  {
    this(original.base);

    field_name = original.field_name; // name of this field
    field_code = original.field_code; // id of this field in the current object
    field_type = original.field_type; // data type contained herein
    field_order = original.field_order;	// display order

    editable = original.editable;
    removable = original.removable;
    visibility = original.visibility;
    builtIn = original.builtIn;

    classname = original.classname; // name of class to manage user interactions with this field
    comment = original.comment;
    classdef = original.classdef; // class object containing the code managing dbfields of this type
    array = original.array;	// true if this field is an array type
    limit = original.limit;

    labeled = original.labeled;
    trueLabel = original.trueLabel;
    falseLabel = original.falseLabel;

    minLength = original.minLength;
    maxLength = original.maxLength;
    okChars = original.okChars;
    badChars = original.badChars;
    namespace = original.namespace; // we point to the original namespace.. not a problem, since they are immutable
    multiLine = original.multiLine;
    regexpPat = original.regexpPat;
    regexp = original.regexp;

    editInPlace = original.editInPlace;
    allowedTarget = original.allowedTarget;
    targetField = original.targetField;

    crypted = original.crypted;
    md5crypted = original.md5crypted;
    storePlaintext = original.storePlaintext;

    // We'll just re-use the original's FieldTemplate for the time
    // being.. when the SchemaEditor is done, it will call
    // clearEditor() on our DBObjectBase, which will create a new
    // FieldTemplate for us.

    template = original.template;
    this.editor = editor;
    changed = false;
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field definition to disk.  It is mated with receive().</p>
   */

  synchronized void emit(DataOutput out) throws IOException
  {
    out.writeUTF(field_name);
    out.writeShort(field_code);
    out.writeShort(field_type);
    out.writeUTF(classname);
    out.writeUTF(comment);
    out.writeBoolean(editable);
    out.writeBoolean(removable);

    if ((base.store.major_version >= 1) || (base.store.minor_version >= 6))
      {
	out.writeBoolean(visibility); // added at file version 1.6
      }

    if ((base.store.major_version >= 1) || (base.store.minor_version >= 7))
      {
	out.writeBoolean(builtIn); // added at file version 1.7
      }

    if ((base.store.major_version >= 1) || (base.store.minor_version >= 1))
      {
	out.writeShort(field_order); // added at file version 1.1
      }

    out.writeBoolean(array);

    if (array)
      {
	out.writeShort(limit);
      }

    if (isBoolean())
      {
	out.writeBoolean(labeled);
	if (labeled)
	  {
	    out.writeUTF(trueLabel);
	    out.writeUTF(falseLabel);
	  }
      }
    else if (isString())
      {
	out.writeShort(minLength);
	out.writeShort(maxLength);
	if (okChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(okChars);
	  }
	
	if (badChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(badChars);
	  }

	if (namespace != null)
	  {
	    out.writeUTF(namespace.getName());
	  }
	else
	  {
	    out.writeUTF("");
	  }

	if ((base.store.major_version >= 1) || (base.store.minor_version >= 9))
	  {
	    out.writeBoolean(multiLine); // added at file version 1.9
	  }

	if ((base.store.major_version >= 1) || (base.store.minor_version >= 14))
	  {
	    if (regexpPat == null)
	      {
		out.writeUTF("");
	      }
	    else
	      {
		out.writeUTF(regexpPat); // added at file version 1.14
	      }
	  }

      }
    else if (isNumeric())
      {
	if (namespace != null)
	  {
	    out.writeUTF(namespace.getName());
	  }
	else
	  {
	    out.writeUTF("");
	  }
      }
    else if (isIP())
      {
	if (namespace != null)
	  {
	    out.writeUTF(namespace.getName());
	  }
	else
	  {
	    out.writeUTF("");
	  }
      }
    else if (isInvid())
      {
	out.writeShort(allowedTarget);
	out.writeBoolean(editInPlace);
	out.writeShort(targetField);
      }
    else if (isPassword())
      {
	out.writeShort(minLength);
	out.writeShort(maxLength);
	if (okChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(okChars);
	  }
	
	if (badChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(badChars);
	  }

	out.writeBoolean(crypted);
	out.writeBoolean(md5crypted);
	out.writeBoolean(storePlaintext);
      }
  }

  /**
   * <p>This method is used when the database is being loaded, to read
   * in this field definition from disk.  It is mated with emit().</p>
   */

  synchronized void receive(DataInput in) throws IOException
  {
    loading = true;

    field_name = in.readUTF();
    field_code = in.readShort();
    field_type = in.readShort();
    classname = in.readUTF();

    if (classname != null && !classname.equals(""))
      {
	try 
	  {
	    classdef = Class.forName(classname);
	  }
	catch (ClassNotFoundException ex)
	  {	    
	    System.err.println("DBObjectBaseField.receive(): class definition could not be found: " + ex);
	    classdef = null;
	  }
      }

    comment = in.readUTF();

    editable = in.readBoolean();
    removable = in.readBoolean();

    // at file version 1.6, we introduced field visibility

    if ((base.store.file_major > 1) || (base.store.file_minor >= 6))
      {
	visibility = in.readBoolean();
      }
    else
      {
	visibility = true;
      }

    // at file version 1.7, we introduced an explicit built-in flag

    if ((base.store.file_major > 1) || (base.store.file_minor >= 7))
      {
	builtIn = in.readBoolean();
      }
    else
      {
	builtIn = !base.isEmbedded() && field_code < 100;
      }

    // at file version 1.1, we introduced field_order

    if ((base.store.file_major > 1) || (base.store.file_minor >= 1))
      {
	field_order = in.readShort();
      }
    else
      {
	field_order = 0;
      }

    array = in.readBoolean();

    if (array)
      {
	limit = in.readShort();
      }
    else
      {
	limit = 1;
      }

    if (isBoolean())
      {
	labeled = in.readBoolean();
	if (labeled)
	  {
	    trueLabel = in.readUTF();
	    falseLabel = in.readUTF();
	  }
      }
    else if (isString())
      {
	String nameSpaceId;

	/* - */

	minLength = in.readShort();
	maxLength = in.readShort();
	okChars = in.readUTF();
	
	if (okChars.equals(""))
	  {
	    okChars = null;
	  }

	badChars = in.readUTF();

	if (badChars.equals(""))
	  {
	    badChars = null;
	  }

	nameSpaceId = in.readUTF();
	
	if (!nameSpaceId.equals(""))
	  {
	    setNameSpace(nameSpaceId);
	  }

	// at file version 1.9, we introduced multiLine
	
	if ((base.store.file_major > 1) || (base.store.file_minor >= 9))
	  {
	    multiLine = in.readBoolean();
	  }
	else
	  {
	    multiLine = false;
	  }

	// at file version 1.14, we introduced regexps for string fields
	
	if ((base.store.file_major > 1) || (base.store.file_minor >= 14))
	  {
	    setRegexpPat(in.readUTF());
	  }
	else
	  {
	    setRegexpPat(null);
	  }
      }
    else if (isNumeric())
      {
	String nameSpaceId;

	/* - */

	// at 1.8 we introduced namespaces for number fields

	if ((base.store.file_major > 1) || (base.store.file_minor >= 8))
	  {
	    nameSpaceId = in.readUTF();
	    
	    if (!nameSpaceId.equals(""))
	      {
		setNameSpace(nameSpaceId);
	      }
	  }
      }
    else if (isIP())
      {
	String nameSpaceId;

	/* - */

	// at 1.8 we introduced namespaces for IP fields

	if ((base.store.file_major > 1) || (base.store.file_minor >= 8))
	  {
	    nameSpaceId = in.readUTF();
	    
	    if (!nameSpaceId.equals(""))
	      {
		setNameSpace(nameSpaceId);
	      }
	  }
      }
    else if (isInvid())
      {
	allowedTarget = in.readShort();
	editInPlace = in.readBoolean();
	targetField = in.readShort();

	// In DBStore file version 1.17 we dropped the use of the back
	// links field.  Some folks apparently used the schema editor
	// to set an Invid Field to point to the backlinks field
	// explicitly (rather than 'none').  We handle that here so
	// that the code that takes care of handling asymmetric fields
	// doesn't get confused.

	if (targetField == SchemaConstants.BackLinksField)
	  {
	    targetField = -1;
	  }
      }
    else if (isPassword())
      {
	minLength = in.readShort();
	maxLength = in.readShort();
	okChars = in.readUTF();
	
	if (okChars.equals(""))
	  {
	    okChars = null;
	  }

	badChars = in.readUTF();

	if (badChars.equals(""))
	  {
	    badChars = null;
	  }

	crypted = in.readBoolean();

	// at 1.16 we introduce md5crypted

	if ((base.store.file_major >1) || (base.store.file_minor >= 16))
	  {
	    md5crypted = in.readBoolean();
	  }
	else
	  {
	    md5crypted = false;
	  }

	if ((base.store.file_major >1) || (base.store.file_minor >= 10))
	  {
	    storePlaintext = in.readBoolean();
	  }
	else
	  {
	    storePlaintext = false;
	  }
      }

    loading = false;
  }

  // ----------------------------------------------------------------------

  /**
   * <p>This method returns true if this field definition can be edited
   * in the schema editor.</p>
   *
   * <p>This method will return always true if the DBSchemaEdit class is configured
   * with developMode set to true.  Otherwise, this method will return false
   * for the built-in field types that the server is dependent on for its
   * own functioning.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isEditable()
  {
    return editable || (editor != null && editor.developMode);
  }

  /**
   * <p>This method returns true if this field definition can be removed
   * by the schema editor.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isRemovable()
  {
    return removable;
  }

  /**
   * <p>This method returns true if this field definition is designated
   * as a built-in.  built-in fields are fields that are present in
   * all object types held in the database.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isBuiltIn()
  {
    return builtIn;
  }

  /**
   * <p>This method returns true if this field
   * is intended to be visible to the client normally,
   * false otherwise.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isVisible()
  {
    return visibility;
  }

  /**
   * <p>Returns the Base we are a part of.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public Base getBase()
  {
    return base;
  }

  /**
   * <p>Returns the name of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getName()
  {
    return field_name;
  }

  /**
   * <p>Sets the name of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setName(String name)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }
    
    field_name = name;
  }

  /**
   * <p>Returns the name of the class managing instances of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getClassName()
  {
    return classname;
  }

  /**
   * <p>Sets the name of the class managing instances of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setClassName(String name)
  {
    Class newclassdef;

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!name.equals(classname))
      {
	try 
	  {
	    newclassdef = Class.forName(name);

	    // won't get here if name was bad

	    classname = name;	
	    classdef = newclassdef;
	  }
	catch (ClassNotFoundException ex)
	  {	    
	    System.err.println("DBObjectBaseField.setClassName(): class definition could not be found: " + ex);
	  }
      }
    
    changed = true;
  }

  /**
   * <p>Returns the Class object managing instances of this field</p>
   */

  public Class getClassDef()
  {
    return classdef;
  }

  /**
   * <p>Returns the comment defined in the schema for this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getComment()
  {
    return comment;
  }

  /**
   * <p>Sets the comment defined in the schema for this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setComment(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    comment = s;
  }      

  /**
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the {@link arlut.csd.ganymede.FieldType FieldType}
   * interface:</p>
   *
   * <pre>
   *   static short BOOLEAN = 0;
   *   static short NUMERIC = 1;
   *   static short DATE = 2;
   *   static short STRING = 3;
   *   static short INVID = 4;
   *   static short PERMISSIONMATRIX = 5;
   *   static short PASSWORD = 6;
   *   static short IP = 7;
   *   static short FLOAT = 8;
   * </pre>
   *
   * @see arlut.csd.ganymede.DBStore
   * @see arlut.csd.ganymede.BaseField
   */

  public short getType()
  {
    return field_type;
  }

  /**
   * <p>Sets the {@link arlut.csd.ganymede.FieldType field type}
   * for this field.  Changing the basic type of a field that is already being
   * used in the server will cause very bad things to happen.  The
   * right way to change an existing field is to delete the field, commit
   * the schema edit, edit the schema again, and recreate the field with
   * the desired field type.</P>
   *
   * <p>If the new field type is not string, invid, or IP, the field
   * will be made a scalar field.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setType(short type)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (type < FIRSTFIELD || type > LASTFIELD)
      {
	throw new IllegalArgumentException("type argument out of range");
      }

    field_type = type;

    // only strings, invids, and ip fields can be vectors

    if (!((isString() || isInvid() || isIP())))
      {
	array = false;
      }
  }

  // type identification convenience methods

  /**
   * <p>Returns true if this field is of boolean type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isBoolean()
  {
    return (field_type == BOOLEAN);
  }

  /**
   * <p>Returns true if this field is of numeric type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isNumeric()
  {
    return (field_type == NUMERIC);
  }

  /**
   * <p>Returns true if this field is of float type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isFloat()
  {
    return (field_type == FLOAT);
  }

  /**
   * <p>Returns true if this field is of date type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isDate()
  {
    return (field_type == DATE);
  }

  /**
   * <p>Returns true if this field is of string type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isString()
  {
    return (field_type == STRING);
  }

  /**
   * <p>Returns true if this field is of invid type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isInvid()
  {
    return (field_type == INVID);
  }

  /**
   * <p>Returns true if this field is of permission matrix type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPermMatrix()
  {
    return (field_type == PERMISSIONMATRIX);
  }

  /**
   * <p>Returns true if this field is of password type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPassword()
  {
    return (field_type == PASSWORD);
  }

  /**
   * <p>Returns true if this field is of IP type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isIP()
  {
    return (field_type == IP);
  }
  
  /**
   * <p>Returns true if this field is a vector field, false otherwise.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isArray()
  {
    return array;
  }

  /**
   * <p>Set this field to be a vector or scalar.  If b is true, this field will
   * be a vector, if false, scalar.</p>
   *
   * <p>Only strings, invid's, and ip fields may be vectors.  Attempting to 
   * setArray(true) for other field types will cause an IllegalArgumentException
   * to be thrown.</p>
   *
   * <p>It may be possible to compatibly handle the conversion from
   * scalar to vector, but a vector to scalar change is an incompatible
   * change.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setArray(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (b && !(isString() || isInvid() || isIP()))
      {
	throw new IllegalArgumentException("can't set this field type to vector");
      }

    array = b;
  }

  /**
   * <p>Returns id code for this field.  Each field in a
   * {@link arlut.csd.ganymede.DBObject DBObject}
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by 
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject}
   * to choose what field to change in the setField method.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getID()
  {
    return field_code;
  }

  /**
   * <p>Returns the order of this field within the containing
   * base.  Used to determine the layout order of object
   * viewing panels.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getDisplayOrder()
  {
    return field_order;
  }

  /**
   * <p>Returns the type id for this field definition as
   * a Short, suitable for use in a hash.</p>
   */

  public Short getKey()
  {
    return new Short(field_code);
  }

  /**
   * <p>Set the identifying code for this field.</p>
   *
   * <p>This is an incompatible change.  In fact, it
   * is so incompatible that it only makes sense in
   * the context of creating a new field in a particular
   * DBObjectBase.. otherwise we would wind up indexed
   * improperly if we don't somehow move ourselves to
   * a different key slot in the DBObjectBase fieldHash.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setID(short id)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    field_code = id;
  }

  /**
   * <p>Set the display order of this field in the SchemaEditor's
   * tree, and in the object display panels in the client.</p>
   *
   * <p>Note that this method does not check to make sure that fields
   * don't have duplicated order values.. the schema editor needs
   * to do the proper logic to make sure that all fields have a
   * reasonable order after any field creation, deletion, or 
   * re-ordering.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setDisplayOrder(short order)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    field_order = order;

    base.sortFields();
  }

  /**
   * <p>Returns the object definition that this field is defined under.</p>
   */

  public DBObjectBase base()
  {
    return base;
  }

  synchronized void setBase(DBObjectBase base)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    this.base = base;
  }

  // **
  // array attribute methods
  // **

  /**
   * <p>Returns the array size limitation for this field if it is an array field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMaxArraySize()
  {
    if (!array)
      {
	throw new IllegalArgumentException("not an array field");
      }

    return limit;
  }

  /**
   * <p>Set the maximum number of values allowed in this vector field.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setMaxArraySize(short limit)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!array)
      {
	throw new IllegalArgumentException("not an array field");
      }

    this.limit = limit;
  }

  // **
  // boolean attribute methods
  // **

  /**
   * <p>Returns true if this is a boolean field with labels</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isLabeled()
  {
    if (!isBoolean())
      {
	throw new IllegalArgumentException("not a boolean field");
      }
    
    return labeled;
  }

  /**
   * <p>Turn labeled choices on/off for a boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setLabeled(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isBoolean())
      {
	throw new IllegalArgumentException("not a boolean field");
      }
    
    labeled = b;
  }

  /**
   * <p>Returns the true Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getTrueLabel()
  {
    if (isLabeled())
      {
	return trueLabel;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  /**
   * <p>Sets the label associated with the true choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setTrueLabel(String label)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (isLabeled())
      {
	trueLabel = label;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  /**
   * <p>Returns the false Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getFalseLabel()
  {
    if (isLabeled())
      {
	return falseLabel;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  /**
   * <p>Sets the label associated with the false choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setFalseLabel(String label)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (isLabeled())
      {
	falseLabel = label;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  // **
  // string attribute methods
  // **

  /**
   * <p>Returns the minimum acceptable string length if this is a string or
   * password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMinLength()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return minLength;
  }

  /**
   * <p>Sets the minimum acceptable length for this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setMinLength(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }
    
    minLength = val;
  }

  /**
   * <p>Returns the maximum acceptable string length if this is a string 
   * or password field.</p>
   * 
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMaxLength()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return maxLength;
  }

  /** 
   * <p>Sets the maximum acceptable length for this string or
   * password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setMaxLength(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }
    
    maxLength = val;
  }

  /**
   * <p>Returns the set of acceptable characters if this is a string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getOKChars()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return okChars;
  }

  /**
   * <p>Sets the set of characters that are allowed in this string or 
   * password field.  If s is null, all characters by default 
   * are acceptable.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setOKChars(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    okChars = s;
  }

  /**
   * <p>Returns the set of unacceptable characters if this is a 
   * string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getBadChars()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return badChars;
  }

  /**
   * <p>Sets the set of characters that are specifically disallowed in
   * this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setBadChars(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    badChars = s;
  }

  /**
   * <p>Returns true if this string field is intended to be a multi-line
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isMultiLine()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return multiLine;
  }

  /**
   * <p>Sets whether or not this string field should be presented as a
   * multiline field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setMultiLine(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    multiLine = b;
  }

  /**
   * <p>Returns the regexp pattern string constraining this string
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getRegexpPat()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return regexpPat;
  }

  /**
   * <p>Sets the regexp pattern string constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized boolean setRegexpPat(String s)
  {
    if (editor == null && !loading)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    if (s == null || s.equals(""))
      {
	regexpPat = null;
	regexp = null;

	return true;
      }
    else
      {
	try
	  {
	    regexp = new gnu.regexp.RE(s);
	  }
	catch (gnu.regexp.REException ex)
	  {
	    return false;	// bad regexp syntax?
	  }

	regexpPat = s;
	return true;
      }
  }

  /** 
   * <p>Returns the DBNameSpace that this string, numeric, or IP
   * field is associated with.</p> 
   */

  public DBNameSpace getNameSpace()
  {
    // several pieces of code have already been written to expect a null
    // value for a field's namespace if none is defined, regardless of
    // field type.  No need for us to be overly fastidious here.

    return namespace;
  }

  /**
   * <p>Returns the label of this string, numeric, or IP field's namespace.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getNameSpaceLabel()
  {
    // several pieces of code have already been written to expect a null
    // value for a field's namespace if none is defined, regardless of
    // field type.  No need for us to be overly fastidious here.

    if (namespace != null)
      {
	return namespace.name;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Set a namespace constraint for this string, numeric, or
   * IP field.</p>
   *
   * <p>Note that this is intended to be called from the Schema Editor,
   * and won't take effect until the next time the system is stopped
   * and reloaded.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string, numeric, or IP type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setNameSpace(String nameSpaceId)
  {
    if (editor == null && !loading)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isNumeric() && !isIP())
      {
	throw new IllegalArgumentException("this field type does not accept a namespace constraint");
      }

    if (nameSpaceId == null)
      {
	namespace = null;
      }
    else if (nameSpaceId.equals(""))
      {
	namespace = null;
      }
    else
      {
	// this field is associated with a namespace.

	Enumeration values;
	DBNameSpace tmpNS;
	
	/* -- */
	
	values = base.store.nameSpaces.elements();
	namespace = null;

	while (values.hasMoreElements() && (namespace == null))
	  {
	    tmpNS = (DBNameSpace) values.nextElement();

	    if (tmpNS.getName().equalsIgnoreCase(nameSpaceId))
	      {
		namespace = tmpNS;
	      }
	  }

	// if we didn't find it, complain.

	if (namespace == null)
	  {
	    // throw new IllegalArgumentException("Unknown namespace id specified for field");
	    System.err.println("**** Unknown namespace id <" + nameSpaceId + "> specified for field " + 
			       base.toString() + ", field: " + toString());
	  }
      }
  }

  /**
   * <p>This method is used internally to set a namespace constraint.</p>
   *
   * <p>It does not appear that this method is currently used.. rather that
   * {@link arlut.csd.ganymede.DBObjectBaseField#setNameSpace(java.lang.String)
   * setNameSpace(string)} is.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string, numeric, or IP type.</p>
   */

  void setNameSpace(DBNameSpace namespace)
  {
    if (!isString() && !isNumeric() && !isIP())
      {
	throw new IllegalArgumentException("not a string/numeric field");
      }

    this.namespace = namespace;
  }

  // **
  // invid attribute methods
  // **

  /**
   * <p>Returns true if this is an invid field which is intended as an editInPlace
   * reference for the client's rendering.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isEditInPlace()
  {
    return editInPlace;
  }

  /**
   * <p>Sets whether or not this field is intended as an editInPlace
   * reference for the client's rendering.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public void setEditInPlace(boolean b)
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }
    else
      {
	editInPlace = b;
      }
  }

  /**
   * <p>Returns true if this is a target restricted invid field</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isTargetRestricted()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return (allowedTarget != -1);
  }

  /**
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is a specified symmetric field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getTargetBase()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return allowedTarget;
  }

  /**
   * <p>Sets the allowed target object code of this invid field to &lt;val&gt;.
   * If val is -1, this invid field can point to objects of any type.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setTargetBase(short val)
  {
    Base b;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (val < 0)
      {
	allowedTarget = val;
	return;
      }
    
    b = base.editor.getBase(val);

    if (b != null)
      {
	allowedTarget = val;
      }
    else
      {
	throw new IllegalArgumentException("not a valid base id");
      }
  }

  /**
   * <p>Sets the allowed target object code of this invid field to &lt;baseName&gt;.
   * If val is null, this invid field can point to objects of any type.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setTargetBase(String baseName)
  {
    Base b;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (baseName == null)
      {
	allowedTarget = -1;
	return;
      }

    b = editor.getBase(baseName);

    try
      {
	if (b != null)
	  {
	    allowedTarget = b.getTypeID();
	  }
	else
	  {
	    throw new IllegalArgumentException("not a valid base name");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote except: " + ex);
      }
  }

  /**
   * <p>If this field is a target restricted invid field, this method will return
   * true if this field has a symmetry relationship to the target</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isSymmetric()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return ((allowedTarget != -1) && (targetField != -1));
  }

  /**
   * <p>If this field is a target restricted invid field, this method will return
   * a short indicating the field in the target object that the symmetry relation
   * applies to.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getTargetField()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return targetField;
  }

  /**
   * <p>Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().  If
   * val == -1, the targetField will be set to a value representing
   * no selection.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setTargetField(short val)
  {
    Base b;
    BaseField bF;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (val < 0)
      {
	targetField = val;
	return;
      }

    if (allowedTarget == -1)
      {
	throw new IllegalArgumentException("not a symmetry maintained field");
      }

    try
      {
	b = editor.getBase(allowedTarget);

	if (b == null)
	  {
	    throw new IllegalArgumentException("invalid target base");
	  }
	
	bF = b.getField(val);

	if (bF == null)
	  {
	    throw new IllegalArgumentException("invalid target field in base " + b.getName());
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    targetField = val;
  }

  /**
   * <p>Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().  If &lt;fieldName&gt;
   * is null, the targetField will be cleared.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setTargetField(String fieldName)
  {
    Base b;
    BaseField bF;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (fieldName == null)
      {
	targetField = -1;
	return;
      }

    // look for fieldName in the base currently specified in
    // allowedTarget

    if (allowedTarget == -1)
      {
	throw new IllegalArgumentException("not a symmetry maintained field");
      }

    b = editor.getBase(allowedTarget);

    try
      {
	if (b == null)
	  {
	    throw new IllegalArgumentException("invalid target base");
	  }
	
	bF = b.getField(fieldName);

	if (bF == null)
	  {
	    throw new IllegalArgumentException("invalid target field in base " + b.getName());
	  }

	targetField = bF.getID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <p>This method returns true if this is a password field that
   * stores passwords in UNIX crypt format, and can thus accept
   * pre-crypted passwords.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isCrypted()
  {
    return crypted;
  }

  /**
   * <p>This method is used to specify that this password field
   * should store passwords in UNIX crypt format.  If passwords
   * are stored in UNIX crypt format, they will not be kept in
   * plaintext on disk, regardless of the setting of setPlainText().</p>
   *
   * <p>setCrypted() is not mutually exclusive with setMD5Crypted().</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public void setCrypted(boolean b)
  {    
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isPassword())
      {
	throw new IllegalArgumentException("not an password field");
      }

    crypted = b;
  }

  /** 
   * <p>This method returns true if this is a password field that
   * stores passwords in OpenBSD/FreeBSD/PAM md5crypt() format, and
   * can thus accept pre-crypted passwords.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public boolean isMD5Crypted()
  {
    return md5crypted;
  }

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in OpenBSD/FreeBSD/PAM md5crypt() format.  If
   * passwords are stored in md5crypt() format, they will not be kept
   * in plaintext on disk, regardless of the setting of setPlainText().</p>
   *
   * <p>setMD5Crypted() is not mutually exclusive with setCrypted().</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public void setMD5Crypted(boolean b)
  {    
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isPassword())
      {
	throw new IllegalArgumentException("not an password field");
      }

    md5crypted = b;
  }

  /**
   * <p>This method returns true if this is a password field that
   * will keep a copy of the password in plaintext.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPlainText()
  {
    return storePlaintext;
  }

  /**
   * <p>This method is used to specify that this password field
   * should keep a copy of the password in plaintext, in
   * addition to a UNIX crypted copy.  If crypted is
   * false, plaintext will be treated as true, whether
   * or not this is explicitly set by the schema editor.</p>
   *
   * <p>If crypted or md5crypted is true, fields of this type will never retain
   * the plaintext password information on disk.  Plaintext 
   * password information will only be retained in the on-disk
   * ganymede.db file if crypted and md5crypted are both false.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public void setPlainText(boolean b)
  {    
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isPassword())
      {
	throw new IllegalArgumentException("not an password field");
      }

    storePlaintext = b;
  }

  // general convenience methods

  /**
   * <p>This method is intended to produce a human readable
   * representation of this field definition's type attributes.  This
   * method should not be used programatically to determine this
   * field's type information.</p>
   *
   * <p>This method is only for human information, and the precise
   * results returned are subject to change at any time.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public String getTypeDesc()
  {
    String result;

    switch (field_type)
      {
      case BOOLEAN:
	result = "boolean";
	break;

      case NUMERIC:
	result = "numeric";
	break;

      case FLOAT:
	result = "float";
	break;

      case DATE:
	result = "date";
	break;

      case STRING:

	result = "string <min: " + minLength + ", max:" + maxLength + ">";
	
	if (okChars != null)
	  {
	    result += ", okChars: '" + okChars + "'";
	  }

	if (badChars != null)
	  {
	    result += ", badChars: '" + badChars + "'";
	  }

	if (namespace != null)
	  {
	    result += ", namespace: " + namespace.getName();
	  }

	if (regexpPat != null)
	  {
	    result += ", regexpPat: '" + regexpPat + "'";
	  }

	break;

      case INVID:
	result = "invid";

	if (editInPlace)
	  {
	    result += " <edit-in-place> ";
	  }

	if (allowedTarget >= 0)
	  {
	    DBObjectBase refBase;

	    refBase = base.store.getObjectBase(allowedTarget);

	    if (refBase != null)
	      {
		result += ", --> [" + refBase.getName() + "] ";
		
		if (targetField != -1)
		  {
		    try
		      {
			result += ", <-- [" + refBase.getField(targetField).getName() + "] ";
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote: " + ex);
		      }
		    catch (NullPointerException ex)
		      {
			result += ", <-- [INVALID FIELD TARGET!!] ";
		      }
		  }
	      }
	    else
	      {
		result += ", --> [INVALID BASE!!] ";
	      }
	  }
	else if (allowedTarget == -1)
	  {
	    result += ", --> [any]";
	  }
	else if (allowedTarget == -2)
	  {
	    result += ", --> [any]";

	    // if allowed Target == -2 and targetField != -1, we assume
	    // that we've got a field that's guaranteed to be present in
	    // all bases, including our parent.
	    
	    if (targetField != -1)
	      {
		try
		  {
		    result += ", <-- [" + base.getField(targetField).getName() + "] ";
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }
	      }
	  }
	
	break;

      case PERMISSIONMATRIX:
	result = "permission matrix";
	break;

      case PASSWORD:
	result = "password";

	if (crypted)
	  {
	    result += " <crypted>";
	  }

	if (md5crypted)
	  {
	    result += " <md5 crypted>";
	  }

	if (storePlaintext)
	  {
	    result += " <plaintext>";
	  }

	break;

      case IP:
	result = "i.p. field";
	break;

      default:
	result = "<<bad type code>>";
      }

    if (array)
      {
	return result + " array [" + limit + "]";
      }
    else
      {
	return result;
      }
  }

  /**
   * <p>This method is intended to produce a human readable
   * representation of this field definition's type attributes.  This
   * method should not be used programatically to determine this
   * field's type information.</p>
   *
   * <p>This method is only for human elucidation, and the precise
   * results returned are subject to change at any time.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public String getTypeDescHTML()
  {
    String result;

    switch (field_type)
      {
      case BOOLEAN:
	result = "<td>boolean</td>";
	break;

      case NUMERIC:
	result = "<td>numeric</td>";
	break;

      case FLOAT:
	result = "<td>float</td>";
	break;

      case DATE:
	result = "<td>date</td>";
	break;

      case STRING:
	result = "<td>string</td>";
	break;

      case INVID:
	result = "<td>invid</td>";
	break;

      case PERMISSIONMATRIX:
	result = "<td>permission matrix</td>";
	break;

      case PASSWORD:
	result = "<td>password</td>";
	break;

      case IP:
	result = "<td>i.p. field</td>";
	break;

      default:
	result = "<td>&lt;&lt;bad type code&gt;&gt;</td>";
      }

    if (array)
      {
	result += "<td>[0.." + limit + "]</td>";
      }
    else
      {
	result += "<td><FONT COLOR=\"#FF0000\">N</font></td>";
      }

    if (namespace != null)
      {
	result += "<td>" + namespace.getName() + "</td>";
      }
    else
      {
	result += "<td><FONT COLOR=\"#FF0000\">N</font></td>";
      }

    // generate the notes field

    result += "<td>";

    switch (field_type)
      {
      case STRING:

	result += "min: " + minLength + ", max: " + maxLength;

	if (okChars != null)
	  {
	    result += " okChars: '" + okChars + "'";
	  }

	if (badChars != null)
	  {
	    result += " badChars: '" + badChars + "'";
	  }

	if (regexpPat != null)
	  {
	    result += " regexpPat: '" + regexpPat + "'";
	  }
	
	break;
	
      case INVID:

	if (editInPlace)
	  {
	    result += "edit-in-place ";
	  }

	if (allowedTarget >= 0)
	  {
	    DBObjectBase refBase;

	    refBase = base.store.getObjectBase(allowedTarget);

	    if (refBase == null)
	      {
		result += "targets [INVALID OBJECT TYPE]";
	      }
	    else
	      {
		result += "targets [" + refBase.getName() + "] ";

		if (targetField != -1)
		  {
		    try
		      {
			result += "reverse link [" + refBase.getField(targetField).getName() + "] ";
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote: " + ex);
		      }
		  }
	      }
	  }
	else if (allowedTarget == -1)
	  {
	    result += "targets [any]";
	  }
	else if (allowedTarget == -2)
	  {
	    result += "targets [any] ";

	    // if allowed Target == -2 and targetField != -1, we assume
	    // that we've got a field that's guaranteed to be present in
	    // all bases, including our parent.
	    
	    if (targetField != -1)
	      {
		try
		  {
		    result += "reverse link [" + base.getField(targetField).getName() + "] ";
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }
	      }

	  }
	break;

      case PASSWORD:

	if (crypted)
	  {
	    result += "crypted";
	  }

	if (md5crypted)
	  {
	    result += " md5crypted";
	  }

	if (storePlaintext)
	  {
	    result += " plaintext";
	  }

	break;

      default:
	break;
      }
    
    result += "</td>";
    return result;
  }

  /**
   * <p>This method is used when the Ganymede server dumps its schema.
   * It prints an HTML description of this field type to the PrintWriter
   * specified.</p>
   *
   * <p>This method was written in concert with the other DBStore objects'
   * printHTML methods, and assumes that it will be run in the context
   * of the full DBStore.printCategoryTreeHTML() method.</p>
   */

  public void printHTML(PrintWriter out)
  {
    out.print("<td>" + field_name + "</td><td>" + field_code + "</td>");
    out.print(getTypeDescHTML());
    out.println();
  }

  /**
   * <p>This method is used when the Ganymede server dumps its schema.
   * It prints an ASCII description of this field type to the PrintWriter
   * specified.</p>
   *
   * <p>This method was written in concert with the other DBStore objects'
   * print methods, and assumes that it will be run in the context
   * of the full DBStore.printBases() method.</p>
   */

  public void print(PrintWriter out, String indent)
  {
    out.print(indent + field_name + "(" + field_code + "):");
    out.print(indent + getTypeDesc());
    out.println();
  }

  public String toString()
  {
    return field_name;
  }
}
