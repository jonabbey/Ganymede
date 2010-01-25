/*

   CatTreeNode.java

   Category tree node for GASHSchema
   
   Created: 14 August 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System

   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

import arlut.csd.JTree.treeMenu;
import arlut.csd.JTree.treeNode;
import arlut.csd.ganymede.rmi.Category;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     CatTreeNode

------------------------------------------------------------------------------*/

/**
 * This class is a simple {@link arlut.csd.JTree.treeNode treeNode}
 * subclass with a {@link arlut.csd.ganymede.rmi.Category Category}
 * data element.  Used in the Ganymede admin console's schema editor.
 *
 * This class is in the common package because it is also used in the
 * Ganymede client.
 */

public class CatTreeNode extends arlut.csd.JTree.treeNode {

  private Category category;	// remote reference

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

  public void cleanup()
  {
    this.category = null;
  }
}
