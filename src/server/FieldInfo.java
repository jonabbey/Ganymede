/*

   FieldInfo.java

   This class is a serializable object to return all the information
   the container panel needs to render a field.
   
   Created: 4 November 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldInfo

------------------------------------------------------------------------------*/

public class FieldInfo implements java.io.Serializable {

  db_field
    field;

  boolean
    defined,
    editable,
    visible;

  Object
    value;

  /* -- */

  public FieldInfo(DBField field)
  {
    this.field = (db_field) field;

    if (!field.isVector())
      {
	value = field.getValue();
      }
    else
      {
	value = field.getValues();
      }

    defined = field.isDefined();
    editable = field.isEditable();
    visible = field.isVisible();
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
}
