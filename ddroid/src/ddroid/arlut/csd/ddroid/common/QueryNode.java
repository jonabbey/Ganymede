/*

   QueryNode.java

   The QueryNode class is used to represent a node in a query tree.
   Each node can be an and node, an or node, a not node, or a leaf
   node.  Leaf nodes actually represent a comparator test.

   The QueryNode classes are serializable, for bodily transmission
   over an RMI connection.
   
   Created: 21 October 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ddroid.common;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       QueryNode

------------------------------------------------------------------------------*/

/**
 * <P>A QueryNode is a node in a serialized {@link arlut.csd.ddroid.common.Query Query}
 * tree.  The QueryNodes form a tree of boolean operators and comparison nodes
 * that are submitted to the server by the client, and which are interpreted
 * by the {@link arlut.csd.ddroid.server.DBQueryHandler DBQueryHandler}.</P>
 */

public abstract class QueryNode implements java.io.Serializable {
  static final long serialVersionUID = -4396943100372813308L;
}

