/*

   SpaceNode.java

   Namespace tree node for GASHSchema.
   
   Created: 14 August 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Tree.*;
import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       SpaceNode

------------------------------------------------------------------------------*/

class SpaceNode extends arlut.csd.Tree.treeNode {

  private NameSpace space;

  /* -- */

  SpaceNode(treeNode parent, String text, NameSpace space, treeNode insertAfter,
	    boolean expandable, int openImage, int closedImage, PopupMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.space = space;
  }

  public NameSpace getSpace()
  {
    return space;
  }

  public void setSpace(NameSpace space)
  {
    this.space = space;
  }
}
