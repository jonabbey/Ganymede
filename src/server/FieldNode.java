/*

   FieldNode.java

   field tree node for GASHSchema
   
   Created: 14 August 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.Tree.*;
import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldNode

------------------------------------------------------------------------------*/

class FieldNode extends arlut.csd.Tree.treeNode {

  private BaseField field;

  /* -- */

  FieldNode(treeNode parent, String text, BaseField field, treeNode insertAfter,
	    boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.field = field;
  }

  public BaseField getField()
  {
    return field;
  }

  public void setField(BaseField field)
  {
    this.field = field;
  }
}
