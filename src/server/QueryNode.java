/*

   QueryNode.java

   The QueryNode class is used to represent a node in a query tree.
   Each node can be an and node, an or node, a not node, or a leaf
   node.  Leaf nodes actually represent a comparator test.

   The QueryNode classes are serializable, for bodily transmission
   over an RMI connection.
   
   Created: 21 October 1996
   Version: %D% $Revision: 1.1 $
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.ganymede.*;

public abstract class QueryNode implements java.io.Serializable {
}

class QueryNotNode extends QueryNode {

  QueryNode child;

  /* -- */

  public QueryNotNode(QueryNode child)
  {
    this.child = child;
  }

}

class QueryAndNode extends QueryNode {

  QueryNode child1, child2;
  
  /* -- */

  public QueryAndNode(QueryNode child1, QueryNode child2)
  {
    this.child1 = child1;
    this.child2 = child2;
  }
}

class QueryOrNode extends QueryNode {

  QueryNode child1, child2;  

  /* -- */

  public QueryOrNode(QueryNode child1, QueryNode child2)
  {
    this.child1 = child1;
    this.child2 = child2;
  }

}

class QueryDataNode extends QueryNode {

  static final byte FIRST = 1;

  static final byte EQUALS = 1;
  static final byte LESS = 2;
  static final byte LESSEQ = 3;
  static final byte GREAT = 4;
  static final byte GREATEQ = 5;
  static final byte NOCASEEQ = 6; // case insensitive string equals

  static final byte LAST = 6;

  /* - */

  String fieldname;
  int fieldId;
  byte comparator;
  Object value;

  /* -- */

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
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   */

  public QueryDataNode(int fieldId, byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = fieldId;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;
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
  }
}

