/*

   FieldTemplate.java

   This class is a serializable object to return all the static
   information defining a field.
   
   Created: 5 November 1997
   Version: $Revision: 1.2 $ %D%
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

  // password attributes
  
  boolean crypted = false;

  /* -- */

  public FieldTemplate(DBObjectBaseField fieldDef)
  {
    name = fieldDef.getName();
    comment = fieldDef.getComment();
    type = fieldDef.getType();
    fieldID = fieldDef.getID();

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

  public boolean isCrypted()
  {
    return crypted;
  }
}
