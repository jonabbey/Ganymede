/*

   FieldTemplate.java

   This class is a serializable object to return all the static
   information defining a field.
   
   Created: 5 November 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   FieldTemplate

------------------------------------------------------------------------------*/

public class FieldTemplate implements java.io.Serializable, FieldType {

  // common field data

  String name;
  String comment;
  short type;
  short fieldID;
  boolean vector;

  // perm_editor needs us to store this for it

  short baseID;

  // built in?

  boolean builtIn;

  // array attributes

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

  // invid attributes

  boolean editInPlace = false;
  short allowedTarget = -1;

  // password attributes
  
  boolean crypted = false;

  /* -- */

  public FieldTemplate(DBObjectBaseField fieldDef)
  {
    name = fieldDef.getName();
    comment = fieldDef.getComment();
    type = fieldDef.getType();
    fieldID = fieldDef.getID();
    baseID = fieldDef.base.getTypeID();

    vector = fieldDef.isArray();

    if (vector)
      {
	limit = fieldDef.getMaxArraySize();
      }

    builtIn = fieldDef.isBuiltIn();

    switch (type)
      {
      case BOOLEAN:
	labeled = fieldDef.isLabeled();

	if (labeled)
	  {
	    trueLabel = fieldDef.getTrueLabel();
	    falseLabel = fieldDef.getFalseLabel();
	  }
	break;

      case STRING:
	minLength = fieldDef.getMinLength();
	maxLength = fieldDef.getMaxLength();
	okChars = fieldDef.getOKChars();
	badChars = fieldDef.getBadChars();
	break;

      case INVID:
	editInPlace = fieldDef.isEditInPlace();
	allowedTarget = fieldDef.getTargetBase();
	break;

      case PASSWORD:
	crypted = fieldDef.isCrypted();
	break;
      }
  }

  public short getType()
  {
    return type;
  }

  public short getID()
  {
    return fieldID;
  }

  public short getBaseID()
  {
    return baseID;
  }

  public String getName()
  {
    return name;
  }

  public String getComment()
  {
    return comment;
  }

  public boolean isArray()
  {
    return vector;
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
    return (type == BOOLEAN);
  }

  /**
   * 
   * Returns true if this field is of numeric type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isNumeric()
  {
    return (type == NUMERIC);
  }

  /**
   * 
   * Returns true if this field is of date type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isDate()
  {
    return (type == DATE);
  }

  /**
   * 
   * Returns true if this field is of string type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isString()
  {
    return (type == STRING);
  }

  /**
   * 
   * Returns true if this field is of invid type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isInvid()
  {
    return (type == INVID);
  }

  /**
   * 
   * Returns true if this field is of permission matrix type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPermMatrix()
  {
    return (type == PERMISSIONMATRIX);
  }

  /**
   * 
   * Returns true if this field is of password type
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPassword()
  {
    return (type == PASSWORD);
  }

  /**
   * 
   * Returns true if this field is of IP type 
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isIP()
  {
    return (type == IP);
  }

  public boolean isBuiltIn()
  {
    return builtIn;
  }

  public boolean isLabeled()
  {
    return labeled;
  }

  public String getTrueLabel()
  {
    return trueLabel;
  }

  public String getFalseLabel()
  {
    return falseLabel;
  }
  
  public short getMinLength()
  {
    return minLength;
  }

  public short getMaxLength()
  {
    return maxLength;
  }

  public String getOKChars()
  {
    return okChars;
  }

  public String getBadChars()
  {
    return badChars;
  }

  public boolean isEditInPlace()
  {
    return editInPlace;
  }

  public short getTargetBase()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return allowedTarget;
  }

  public boolean isCrypted()
  {
    return crypted;
  }
}
