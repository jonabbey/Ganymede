/*

   QueryAndNode.java

   Created: 10 July 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    QueryAndNode

------------------------------------------------------------------------------*/

public class QueryAndNode extends QueryNode {

  QueryNode child1, child2;
  
  /* -- */

  public QueryAndNode(QueryNode child1, QueryNode child2)
  {
    this.child1 = child1;
    this.child2 = child2;
  }
}

