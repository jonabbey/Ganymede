/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

------------------------------------------------------------------------------*/

/**
 * An entry in the DBObjectBase dictionary.  This class defines the type of
 * an object field, along with any namespace information pertaining to the field.
 */

public class DBObjectBaseField {

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
  byte symmetry;
  short targetField;

  /* -- */

  DBObjectBaseField(DBObjectBase base)
  {
    this.base = base;
    field_name = null;
    field_code = 0;
    field_type = 0;
    limit = -1;
  }

  DBObjectBaseField(DataInput in, DBObjectBase base) throws IOException
  {
    this.base = base;
    receive(in);
  }

  void emit(DataOutput out) throws IOException
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
	    out.writeUTF(namespace.name());
	  }
	else
	  {
	    out.writeUTF("");
	  }
      }
    else if (isInvid())
      {
	out.writeShort(allowedTarget);
	out.writeByte(symmetry);
	out.writeShort(targetField);
      }
  }

  void receive(DataInput in) throws IOException
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
	symmetry = in.readByte();
	targetField = in.readShort();
      }
  }

  /**
   *
   * Returns the name of this field
   *
   */

  public String getName()
  {
    return field_name;
  }

  void setName(String name)
  {
    field_name = name;
  }

  /**
   *
   * Returns the name of the class managing instances of this field
   *
   */

  public String getClassName()
  {
    return classname;
  }

  void setClassName(String name)
  {
    Class newclassdef;

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
  }

  /**
   *
   * Returns the comment defined in the schema for this field
   *
   */

  public String getComment()
  {
    return comment;
  }

  /**
   *
   * Sets the comment defined in the schema for this field
   *
   */

  public void setComment(String s)
  {
    comment = s;
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
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the DBStore class:</p>
   *
   *   static final short BOOLEAN = 0;
   *   static final short NUMERIC = 1;
   *   static final short DATE = 2;
   *   static final short STRING = 3;
   *   static final short INVID = 4;
   *
   * @see arlut.csd.ganymede.DBStore
   */

  public short getType()
  {
    return field_type;
  }

  void setType(short type)
  {
    if (type < DBStore.FIRST || type > DBStore.LAST)
      {
	throw new IllegalArgumentException("type argument out of range");
      }

    field_type = type;
  }

  // type identification convenience methods

  public boolean isBoolean()
  {
    return (field_type == DBStore.BOOLEAN);
  }

  public boolean isNumeric()
  {
    return (field_type == DBStore.NUMERIC);
  }

  public boolean isDate()
  {
    return (field_type == DBStore.DATE);
  }

  public boolean isString()
  {
    return (field_type == DBStore.STRING);
  }

  public boolean isInvid()
  {
    return (field_type == DBStore.INVID);
  }

  //

  public byte getVisibility()
  {
    return visibility;
  }

  void setVisibility(byte b)
  {
    visibility = b;
  }

  /**
   *
   * <p>Returns true if this field is a vector field, false otherwise.</p>
   *
   */
  public boolean isArray()
  {
    return array;
  }

  void setArray(boolean b)
  {
    array = b;
  }

  /**
   *
   * <p>Returns id code for this field.  Each field in a DBObject
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by DBEditObject
   * to choose what field to change in the setField method.</p>
   *
   */

  public short id()
  {
    return field_code;
  }

  void setId(short id)
  {
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

  void setBase(DBObjectBase base)
  {
    this.base = base;
  }

  // **
  // array attribute methods
  // **

  /**
   *
   * <p>Returns the array size limitation for this field if it is an array field</p>
   *
   */
  public short getMaxArraySize()
  {
    if (!array)
      {
	throw new IllegalArgumentException("not an array field");
      }

    return limit;
  }

  void setMaxArraySize(short limit)
  {
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
   */
  public boolean isLabeled()
  {
    if (!isBoolean())
      {
	throw new IllegalArgumentException("not a boolean field");
      }
    
    return labeled;
  }

  void setLabeled(boolean b)
  {
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
   */
  public String getTrueLabel()
  {
    if (isLabeled())
      {
	return trueLabel;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  void setTrueLabel(String label)
  {
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
   */
  public String getFalseLabel()
  {
    if (isLabeled())
      {
	return falseLabel;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  void setFalseLabel(String label)
  {
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
   */
  public short getMinLength()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return minLength;
  }

  void setMinLength(short val)
  {
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
   */
  public short getMaxLength()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return maxLength;
  }

  void setMaxLength(short val)
  {
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
   */
  public String getOKChars()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return okChars;
  }

  void setOKChars(String s)
  {
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
   */
  public String getBadChars()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return badChars;
  }

  void setBadChars(String s)
  {
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

  void setNameSpace(String nameSpaceId)
  {
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

	    if (tmpNS.name().equalsIgnoreCase(nameSpaceId))
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
   */
  public short getAllowedTarget()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return allowedTarget;
  }

  void setAllowedTarget(short val)
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }
    
    allowedTarget = val;
  }

  /**
   *
   * <p>If this field is a target restricted invid field, this method will return
   * a byte indicating the symmetry relationship of this field to the target</p>
   *
   */
  public byte getSymmetry()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return symmetry;
  }

  void setSymmetry(byte b)
  {
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
   */
  public short getTargetField()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return targetField;
  }

  void setTargetField(short val)
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    targetField = val;
  }

  // general convenience methods

  public String getTypeDesc()
  {
    String result;

    switch (field_type)
      {
      case DBStore.BOOLEAN:
	result = "boolean";
	break;

      case DBStore.NUMERIC:
	result = "numeric";
	break;

      case DBStore.DATE:
	result = "date";
	break;

      case DBStore.STRING:
	result = "string";
	break;

      case DBStore.INVID:
	result = "invid";
	break;
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
