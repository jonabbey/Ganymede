/*

   QueryNotNode.java

   Created: 10 July 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    QueryNotNode

------------------------------------------------------------------------------*/

public class QueryNotNode extends QueryNode {

  static final long serialVersionUID = 3644590013672673752L;

  // ---

  QueryNode child;

  /* -- */

  public QueryNotNode(QueryNode child)
  {
    this.child = child;
  }
}
