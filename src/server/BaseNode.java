/*

   BaseNode.java

   tree node subtype for the GASHSchema editor.
   
   Created: 14 August 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.JTree.*;
import java.awt.*;


/*------------------------------------------------------------------------------
                                                                           class
                                                                        BaseNode

------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.JTree.treeNode {

  private Base base;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;
  }

  public Base getBase()
  {
    return base;
  }

  public void setBase(Base base)
  {
    this.base = base;
  }
}
