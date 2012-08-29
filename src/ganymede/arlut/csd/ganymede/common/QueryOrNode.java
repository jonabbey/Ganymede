/*

   QueryOrNode.java

   Created: 10 July 1997

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

/*------------------------------------------------------------------------------
                                                                           class
                                                                     QueryOrNode

------------------------------------------------------------------------------*/

/**
 * <P>Logical OR node for use in a serialized {@link arlut.csd.ganymede.common.Query Query}
 * object's {@link arlut.csd.ganymede.common.QueryNode QueryNode} tree.</P>
 */

public class QueryOrNode extends QueryNode {

  static final long serialVersionUID = 437285815734883531L;

  // ---

  public QueryNode child1, child2;  

  /* -- */

  public QueryOrNode(QueryNode child1, QueryNode child2)
  {
    this.child1 = child1;
    this.child2 = child2;
  }

  public String toString()
  {
    return "(" + child1.toString() + ") OR (" + child2.toString() + ")";
  }
}
