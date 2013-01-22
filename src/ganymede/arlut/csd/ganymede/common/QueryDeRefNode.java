/*

   QueryDeRefNode.java

   Created: 28 August 2004

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
                                                                  QueryDeRefNode

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to represent a linked Query in a Query tree.
 * A QueryDeRefNode is matched against an Invid field, and the QueryNode
 * tree carried by the QueryDeRefNode is then evaluated against each
 * DBObject pointed to through the Invid field.</p>
 */

public class QueryDeRefNode extends QueryNode {

  public String fieldname;
  public short fieldId;
  public QueryNode queryTree;

  /* -- */

  /**
   * Named field deref constructor.
   */

  public QueryDeRefNode(String fieldname, QueryNode tree)
  {
    this.fieldname = fieldname;
    this.fieldId = -1;
    this.queryTree = tree;
  }

  /**
   * Numbered field deref constructor.
   */

  public QueryDeRefNode(short fieldid, QueryNode tree)
  {
    this.fieldId = fieldid;
    this.fieldname = null;
    this.queryTree = tree;
  }

  /**
   * Diagnostic aid.
   */

  public String toString()
  {
    return this.toString(null);
  }

  public String toString(Query query)
  {
    StringBuilder result = new StringBuilder();

    if (fieldname != null)
      {
        result.append(fieldname);
      }
    else
      {
        if (query != null)
          {
            result.append(query.describeField(fieldId));
          }
        else
          {
            result.append("<");
            result.append(String.valueOf(fieldId));
            result.append(">");
          }
      }

    result.append("->");

    if (queryTree != null)
      {
        result.append(queryTree.toString(null));
      }

    return result.toString();
  }
}
