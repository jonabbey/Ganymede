/*

   QueryAndNode.java

   Created: 10 July 1997
   Version: $Revision: 1.2 $ %D%
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

  /**
   *
   * This method returns a string in the form of:
   * 
   * and("value child 1" "value child 2")
   *
   */

  public String dumpToString()
    {
      System.out.println("AND DUMP");
      
      String returnVal = ("and(" + child1.dumpToString() + child2.dumpToString() + ")");
      return returnVal;
    }


}

