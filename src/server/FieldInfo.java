/*

   FieldInfo.java

   This class is a serializable object to return all the information
   the container panel needs to render a field.
   
   Created: 4 November 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldInfo

------------------------------------------------------------------------------*/

public class FieldInfo implements java.io.Serializable {

  short type;
  String name;
  short field_code;
  String comment;

  boolean
    defined,
    editable,
    visible,
    builtIn,
    editInPlace,
    vector;

  public FieldInfo(DBField field)
  {
    field_code = field.getID();
    name = field.getName();
    type = field.getType();
    comment = field.getComment();
    defined = field.isDefined();
    editable = field.isEditable();
    visible = field.isVisible();
    builtIn = field.isBuiltIn();
    editInPlace = field.isEditInPlace();
    vector = field.isVector();
  }

  public short getFieldID()
  {
    return field_code;
  }

  public String getName()
  {
    return name;
  }

  public short getType()
  {
    return type;
  }

  public String getComment()
  {
    return comment;
  }

  public boolean isDefined()
  {
    return defined;
  }

  public boolean isEditable()
  {
    return editable;
  }

  public boolean isVisible()
  {
    return visible;
  }

  public boolean isBuiltIn()
  {
    return builtIn;
  }

  public boolean isEditInPlace()
  {
    return editInPlace;
  }

  public boolean isVector()
  {
    return vector;
  }

}
