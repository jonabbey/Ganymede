/*

   QueryAndNode.java

   Created: 10 July 1997
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2000/11/10 05:05:00 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    QueryAndNode

------------------------------------------------------------------------------*/

/**
 * <P>Logical AND node for use in a serialized {@link arlut.csd.ganymede.Query Query}
 * object's {@link arlut.csd.ganymede.QueryNode QueryNode} tree.</P>
 */

public class QueryAndNode extends QueryNode {

  static final long serialVersionUID = -3475701914505388243L;

  // ---

  QueryNode child1, child2;
  
  /* -- */

  public QueryAndNode(QueryNode child1, QueryNode child2)
  {
    this.child1 = child1;
    this.child2 = child2;
  }

  public String toString()
  {
    return "(" + child1.toString() + ") AND (" + child2.toString() + ")";
  }
}

