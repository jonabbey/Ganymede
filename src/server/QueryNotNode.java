/*

   QueryNotNode.java

   Created: 10 July 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    QueryNotNode

------------------------------------------------------------------------------*/

public class QueryNotNode extends QueryNode {

  QueryNode child;

  /* -- */

  public QueryNotNode(QueryNode child)
  {
    this.child = child;
  }

  
  /**
   *
   * This method returns a string in the form of:
   * 
   * not("value child 1")
   *
   */
  
  public String dumpToString()
    {
      System.out.println("NOT DUMP");
      

      String returnVal = ("not(" + child.dumpToString());
      return returnVal;
    }
  
}
