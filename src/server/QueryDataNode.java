/*

   QueryDataNode.java

   Created: 10 July 1997
   Version: $Revision: 1.10 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   QueryDataNode

------------------------------------------------------------------------------*/

public class QueryDataNode extends QueryNode {

  static public final byte FIRST = 1;

  static public final byte EQUALS = 1;
  static public final byte LESS = 2;
  static public final byte LESSEQ = 3;
  static public final byte GREAT = 4;
  static public final byte GREATEQ = 5;
  static public final byte NOCASEEQ = 6; // case insensitive string equals
  static public final byte STARTSWITH = 7;
  static public final byte ENDSWITH = 8;
  static public final byte DEFINED = 9;
  static public final byte LAST = 9;

  static public final byte FIRSTVECOP = 0;
  
  static public final byte NONE = 0;
  static public final byte CONTAINS = 1;
  static public final byte LENGTHEQ = 4;
  static public final byte LENGTHGR = 5;
  static public final byte LENGTHLE = 6;

  static public final byte LASTVECOP = 6;

  static public final short INVIDVAL = -2;

  /* - */

  String fieldname;
  short fieldId;
  byte comparator;
  byte arrayOp;
  Object value;

  /* -- */

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   */

  public QueryDataNode(String fieldname, byte comparator, byte vecOp, Object value)
  {
    this.fieldname = fieldname;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    if (vecOp < FIRSTVECOP || vecOp > LASTVECOP)
      {
	throw new IllegalArgumentException("bad vector operator value: " + vecOp);
      }

    this.arrayOp = vecOp;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   */

  public QueryDataNode(String fieldname, byte comparator, Object value)
  {
    this.fieldname = fieldname;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    this.arrayOp = NONE;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   * If fieldID == -1, the labels of objects in the database will be
   * taken as the field for comparison's sake.
   *
   * If fieldID == -2, the Invid of objects in the database will be
   * taken as the field for comparison's sake.
   * 
   */

  public QueryDataNode(short fieldId, byte comparator, byte vecOp, Object value)
  {
    this.fieldname = null;
    this.fieldId = fieldId;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    if (vecOp < FIRSTVECOP || vecOp > LASTVECOP)
      {
	throw new IllegalArgumentException("bad vector operator value: " + vecOp);
      }

    this.arrayOp = vecOp;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   * If fieldID == -1, the labels of objects in the database will be
   * taken as the field for comparison's sake.
   *
   * If fieldID == -2, the Invid of objects in the database will be
   * taken as the field for comparison's sake.
   * 
   */

  public QueryDataNode(short fieldId, byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = fieldId;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    this.arrayOp = NONE;
  }

  /**
   * Default field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against an object's primary label field.
   *
   */

  public QueryDataNode(byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    this.arrayOp = NONE;
  }
}
