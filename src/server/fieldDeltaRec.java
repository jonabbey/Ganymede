/*

   fieldDeltaRec.java

   The fieldDeltaRec class is used to record the changes that have
   been made to a particular field in a DBObject.  This class is used
   by the DBObjectDeltaRec class to keep track of changes to fields.
   
   Created: 7 July 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   fieldDeltaRec

------------------------------------------------------------------------------*/

/**
 *
 * The fieldDeltaRec class is used to record the changes that have
 * been made to a particular field in a DBObject.  This class is used
 * by the DBObjectDeltaRec class to keep track of changes to fields.
 *
 * @see arlut.csd.ganymede.DBObjectDeltaRec
 * @see arlut.csd.ganymede.DBField
 * 
 */

class fieldDeltaRec {

  short fieldcode;
  boolean vector;
  DBField scalarValue = null;
  Vector addValues = null;
  Vector delValues = null;

  /* -- */

  /**
   * Scalar value constructor.  This constructor may actually be used
   * for vector fields when those vector fields are newly defined.. in
   * this case, we are actually doing a complete definition of the
   * field, rather than just a vector add/remove record.<br><br>
   *
   * If &lt;scalar&gt; is null, this fieldDeltaRec is recording the
   * deletion of a field.
   * 
   */

  fieldDeltaRec(short fieldcode, DBField scalar)
  {
    this.fieldcode = fieldcode;
    this.scalarValue = scalar;
    vector = false;
  }

  /**
   *
   * Vector constructor.  This constructor is used when we are doing a
   * vector differential record.
   *
   */

  fieldDeltaRec(short fieldcode)
  {
    this.fieldcode = fieldcode;
    vector = true;
    addValues = new Vector();
    delValues = new Vector();
  }

  /**
   *
   * This method is used to record a value that has been added
   * to this vector field.
   * 
   */

  void addValue(Object value)
  {
    if (value instanceof Byte[])
      {
	value = new IPwrap((Byte []) value);
      }

    if (delValues.contains(value))
      {
	delValues.removeElement(value);
      }
    else if (!addValues.contains(value))
      {
	addValues.addElement(value);
      }
  }

  /**
   *
   * This method is used to record a value that has been removed
   * from this vector field.
   * 
   */

  void delValue(Object value)
  {
    if (value instanceof Byte[])
      {
	value = new IPwrap((Byte []) value);
      }

    if (addValues.contains(value))
      {
	addValues.removeElement(value);
      }
    else if (!delValues.contains(value))
      {
	delValues.addElement(value);
      }
  }

}
