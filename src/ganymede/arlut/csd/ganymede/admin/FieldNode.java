/*

   FieldNode.java

   field tree node for GASHSchema
   
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

package arlut.csd.ganymede.admin;

import arlut.csd.JTree.treeMenu;
import arlut.csd.JTree.treeNode;
import arlut.csd.ganymede.rmi.BaseField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldNode

------------------------------------------------------------------------------*/

class FieldNode extends arlut.csd.JTree.treeNode {

  private BaseField field;      // remote reference

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

  public void cleanup()
  {
    this.field = null;
  }
}
