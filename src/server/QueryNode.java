/*

   QueryNode.java

   The QueryNode class is used to represent a node in a query tree.
   Each node can be an and node, an or node, a not node, or a leaf
   node.  Leaf nodes actually represent a comparator test.

   The QueryNode classes are serializable, for bodily transmission
   over an RMI connection.
   
   Created: 21 October 1996
   Version: %D% $Revision: 1.5 $
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.ganymede.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       QueryNode

------------------------------------------------------------------------------*/

public abstract class QueryNode implements java.io.Serializable {
  static final long serialVersionUID = -4396943100372813308L;
}

