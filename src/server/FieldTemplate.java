/*

   FieldTemplate.java

   This class is a serializable object to return all the static
   information defining a field.
   
   Created: 5 November 1997
   Version: $Revision: 1.5 $ %D%
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

  /**
   *
   * Returns a type identifier for this field.
   *
   * @see arlut.csd.ganymede.FieldType
   *
   */

  public short getType()
  {
    return type;
  }

  /**
   *
   * Returns the field number for this field.
   *
   */

  public short getID()
  {
    return fieldID;
  }

  /**
   *
   * Returns the object type number for the object containing this
   * field.
   * 
   */

  public short getBaseID()
  {
    return baseID;
  }

  /**
   *
   * Returns the name of this field.
   * 
   */

  public String getName()
  {
    return name;
  }

  /**
   *
   * Returns a description of this field suitable for a tooltip.
   *  
   */

  public String getComment()
  {
    return comment;
  }

  /**
   *
   * Returns true if this field is a vector.
   *
   */

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

  /**
   *
   * Returns true if this field is one of the standard
   * fields that are part of all objects held in
   * the Ganymede server.
   *
   */

  public boolean isBuiltIn()
  {
    return builtIn;
  }

  /**
   *
   * If this field is a BOOLEAN, returns true
   * if this field should be presented as a
   * pair of radio boxes labeled by getTrueLabel() and
   * getFalseLabel().
   *
   */

  public boolean isLabeled()
  {
    return labeled;
  }

  /**
   *
   * If this field is a BOOLEAN and isLabeled() is true,
   * returns the text to be associated with the true
   * choice.
   *
   */

  public String getTrueLabel()
  {
    return trueLabel;
  }

  /**
   *
   * If this field is a BOOLEAN and isLabeled() is true,
   * returns the text to be associated with the false
   * choice.
   *
   */

  public String getFalseLabel()
  {
    return falseLabel;
  }

  /**
   *
   * If this field is a STRING, returns the minimum acceptable length
   * for this field.
   * 
   */
  
  public short getMinLength()
  {
    return minLength;
  }

  /**
   *
   * If this field is a STRING, returns the maximum acceptable length
   * for this field.
   * 
   */

  public short getMaxLength()
  {
    return maxLength;
  }

  /**
   *
   * If this field is a STRING and a limited set of characters are
   * acceptable in this field, returns those characters.
   * 
   */

  public String getOKChars()
  {
    return okChars;
  }

  /**
   *
   * If this field is a STRING and a limited set of characters are
   * not acceptable in this field, returns those characters.
   * 
   */

  public String getBadChars()
  {
    return badChars;
  }

  /**
   *
   * If this field is an INVID, returns true if this field refers
   * to an 'embedded' object.
   *
   */

  public boolean isEditInPlace()
  {
    return editInPlace;
  }

  /**
   *
   * If this field is an INVID and is configured to point to an object
   * of a certain type, returns the object type this field points to.<br><br>
   * 
   * This method will return a negative value if this field can point
   * to objects of differing types.
   *
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
   *
   * If this field is a PASSWORD, returns true if passwords are stored in
   * this field in UNIX Crypt() form.
   *
   */

  public boolean isCrypted()
  {
    return crypted;
  }
}
