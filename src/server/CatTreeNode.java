/*

   CatTreeNode.java

   Category tree node for GASHSchema
   
   Created: 14 August 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.JTree.*;
import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     CatTreeNode

------------------------------------------------------------------------------*/

public class CatTreeNode extends arlut.csd.JTree.treeNode {

  private Category category;

  /* -- */

  public CatTreeNode(treeNode parent, String text, Category category, treeNode insertAfter,
		     boolean expandable, int openImage, int closedImage, treeMenu menu)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.category = category;
  }

  public Category getCategory()
  {
    return category;
  }

  public void setCategory(Category category)
  {
    this.category = category;
  }
}
