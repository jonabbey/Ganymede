/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

------------------------------------------------------------------------------*/

/**
 * An entry in the DBObjectBase dictionary.  This class defines the type of
 * an object field, along with any namespace information pertaining to the field.
 */

public class DBObjectBaseField extends UnicastRemoteObject implements BaseField, FieldType {

  DBObjectBase base;		// definition for the object type we are part of

  // schema fields

  String field_name;		// name of this field
  short field_code;		// id of this field in the current object
  short field_type;		// data type contained herein
  byte visibility;		// visibility code
  String classname;		// name of class to manage user interactions with this field
  String comment;
  Class classdef;		// class object containing the code managing dbfields of this type
  boolean array;		// true if this field is an array type

  // array attributes

  short limit = Short.MAX_VALUE; // max length of array

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
  boolean caseInsensitive = false;

  // invid attributes

  short allowedTarget = -1;
  boolean symmetry;
  short targetField;

  // schema editing

  DBSchemaEdit editor;
  boolean changed;

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
    okChars = "";
    badChars = "";
    
    field_code = 0;
    field_type = 0;
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
  }

  /**
   *
   * Copy constructor, used during schema editing
   *
   */

  DBObjectBaseField(DBObjectBaseField original, DBSchemaEdit editor) throws RemoteException
  {
    this(original.base);

    field_name = original.field_name; // name of this field
    field_code = original.field_code; // id of this field in the current object
    field_type = original.field_type; // data type contained herein
    visibility = original.visibility; // visibility code
    classname = original.classname; // name of class to manage user interactions with this field
    comment = original.comment;
    classdef = original.classdef; // class object containing the code managing dbfields of this type
    array = original.array;	// true if this field is an array type

    labeled = original.labeled;
    trueLabel = original.trueLabel;
    falseLabel = original.falseLabel;

    minLength = original.minLength;
    maxLength = original.maxLength;
    okChars = original.okChars;
    badChars = original.badChars;
    namespace = original.namespace;
    caseInsensitive = original.caseInsensitive;

    allowedTarget = original.allowedTarget;
    symmetry = original.symmetry;
    targetField = original.targetField;

    this.editor = editor;
    changed = false;
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    out.writeUTF(field_name);
    out.writeShort(field_code);
    out.writeShort(field_type);
    out.writeUTF(classname);
    out.writeUTF(comment);
    out.writeByte(visibility);
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
	out.writeUTF(okChars);
	out.writeUTF(badChars);
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
	out.writeBoolean(symmetry);
	out.writeShort(targetField);
      }
  }

  synchronized void receive(DataInput in) throws IOException
  {
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

    visibility = in.readByte();
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
	badChars = in.readUTF();
	nameSpaceId = in.readUTF();
	
	if (!nameSpaceId.equals(""))
	  {
	    setNameSpace(nameSpaceId);
	  }
      }
    else if (isInvid())
      {
	allowedTarget = in.readShort();
	symmetry = in.readBoolean();
	targetField = in.readShort();
      }
  }

  // ----------------------------------------------------------------------

  /**
   *
   * Returns the name of this field
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getName()
  {
    return field_name;
  }

  /**
   *
   * Sets the name of this field
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
   *
   * Returns the name of the class managing instances of this field
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getClassName()
  {
    return classname;
  }

  /**
   *
   * Sets the name of the class managing instances of this field
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
   *
   * Returns the Class object managing instances of this field
   *
   */

  public Class getClassDef()
  {
    return classdef;
  }

  /**
   *
   * Returns the comment defined in the schema for this field
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getComment()
  {
    return comment;
  }

  /**
   *
   * Sets the comment defined in the schema for this field
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
   *
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the FieldType interface:</p>
   *
   *   static final short BOOLEAN = 0;
   *   static final short NUMERIC = 1;
   *   static final short DATE = 2;
   *   static final short STRING = 3;
   *   static final short INVID = 4;
   *
   * @see arlut.csd.ganymede.DBStore
   * @see arlut.csd.ganymede.BaseField
   */

  public short getType()
  {
    return field_type;
  }

  /**
   *
   * Sets the field type for this field.  Changing the field type
   * is an incompatible change, and will result in the class managing
   * this field type being reset to the default class for the field
   * type.
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
  }

  // type identification convenience methods

  /**
   * 
   * Returns true if this field is of boolean type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isBoolean()
  {
    return (field_type == BOOLEAN);
  }

  /**
   * 
   * Returns true if this field is of numeric type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isNumeric()
  {
    return (field_type == NUMERIC);
  }

  /**
   * 
   * Returns true if this field is of date type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isDate()
  {
    return (field_type == DATE);
  }

  /**
   * 
   * Returns true if this field is of string type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isString()
  {
    return (field_type == STRING);
  }

  /**
   * 
   * Returns true if this field is of invid type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isInvid()
  {
    return (field_type == INVID);
  }

  /**
   *
   * Returns the visibility threshold for this field
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public byte getVisibility()
  {
    return visibility;
  }

  /**
   *
   * Sets the basic visibility threshold for this field.
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setVisibility(byte b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    visibility = b;
  }

  /**
   *
   * <p>Returns true if this field is a vector field, false otherwise.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isArray()
  {
    return array;
  }

  /**
   *
   * Set this field to be a vector or scalar.  If b is true, this field will
   * be a vector, if false, scalar.
   *
   * It may be possible to compatibly handle the conversion from
   * scalar to vector, but a vector to scalar change is an incompatible
   * change.
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setArray(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    array = b;
  }

  /**
   *
   * <p>Returns id code for this field.  Each field in a DBObject
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by DBEditObject
   * to choose what field to change in the setField method.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getID()
  {
    return field_code;
  }

  /**
   *
   * Returns the type id for this field definition as
   * a Short, suitable for use in a hash.
   *
   */

  public Short getKey()
  {
    return new Short(field_code);
  }

  /**
   *
   * Set the identifying code for this field.
   *
   * This is an incompatible change.  In fact, it
   * is so incompatible that it only makes sense in
   * the context of creating a new field in a particular
   * DBObjectBase.. otherwise we would wind up indexed
   * improperly if we don't somehow move ourselves to
   * a different key slot in the DBObjectBase fieldHash.
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
   *
   * <p>Returns the object definition that this field is defined under.</p>
   *
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
   *
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
   *
   * Set the maximum number of values allowed in this vector field.
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
   *
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
   *
   * Turn labeled choices on/off for a boolean field.
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
   *
   * <p> Returns the true Label if  this is a labeled boolean field</p> 
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
   *
   * Sets the label associated with the true choice for this
   * boolean field.
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
   *
   * <p> Returns the false Label if  this is a labeled boolean field</p> 
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
   *
   * Sets the label associated with the false choice for this
   * boolean field.
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
   *
   * <p> Returns the minimum acceptable string length if this is a string field.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMinLength()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return minLength;
  }

  /**
   *
   * Sets the minimum acceptable length for this string field.
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setMinLength(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }
    
    minLength = val;
  }

  /**
   *
   * <p> Returns the maximum acceptable string length if this is a string field.</p>
   * 
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMaxLength()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return maxLength;
  }

  /**
   *
   * Sets the maximum acceptable length for this string field.
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setMaxLength(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }
    
    maxLength = val;
  }

  /**
   *
   * <p> Returns the set of acceptable characters if this is a string field.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getOKChars()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return okChars;
  }

  /**
   *
   * Sets the set of characters that are allowed in this string field.  If
   * s is null, all characters by default are acceptable.
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setOKChars(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    okChars = s;
  }

  /**
   *
   * <p>Returns the set of unacceptable characters if this is a string field.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getBadChars()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return badChars;
  }

  /**
   *
   * Sets the set of characters that are specifically disallowed in
   * this string field.
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setBadChars(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    badChars = s;
  }

  /**
   *
   * <p>Returns the DBNameSpace that this string field is associated with.</p>
   *
   */
  public DBNameSpace getNameSpace()
  {
    // several pieces of code have already been written to expect a null
    // value for a field's namespace if none is defined, regardless of
    // field type.  No need for us to be overly fastidious here.

    return namespace;
  }

  /**
   * <p>Returns the label of this string field's namespace.</p>
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
   *
   * Set a namespace constraint for this string field.
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setNameSpace(String nameSpaceId)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
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
	    throw new IllegalArgumentException("Unknown namespace id specified for field");
	  }
      }
  }

  void setNameSpace(DBNameSpace namespace)
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    this.namespace = namespace;
  }

  // **
  // invid attribute methods
  // **

  /**
   *
   * <p>Returns true if this is a target restricted invid field</p>
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
   *
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getAllowedTarget()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return allowedTarget;
  }

  /**
   *
   * Sets the allowed target object code of this invid field to <val>.
   * If val is -1, this invid field can point to objects of any type.
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized void setAllowedTarget(short val)
  {
    // should we check that this is a valid target code?

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }
    
    allowedTarget = val;
  }

  /**
   *
   * <p>If this field is a target restricted invid field, this method will return
   * true if this field has a symmetry relationship to the target</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isSymmetric()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return symmetry;
  }

  /**
   *
   * Turns symmetry maintenance on/off for this invid field.  If b is
   * true, changes to this invid field will result in symmetric changes
   * being made to an invid that is set/cleared/added/deleted on this
   * field.
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setSymmetry(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    symmetry = b;
  }

  /**
   *
   * <p>If this field is a target restricted invid field, this method will return
   * a short indicating the field in the target object that the symmetry relation
   * applies to.</p>
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
   * 
   * Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized void setTargetField(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    targetField = val;
  }

  // general convenience methods

  /**
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

      case DATE:
	result = "date";
	break;

      case STRING:
	result = "string";
	break;

      case INVID:
	result = "invid";
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

  public void print(PrintStream out)
  {
    out.print(field_name + "(" + field_code + "):");
    out.print(getTypeDesc());
    out.println();
  }
}
