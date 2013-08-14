/*

   CategoryTransport.java

   This class is intended to provide a serializable object that
   can be used to bulk-dump a static description of the category
   and base structures on the server to the client.

   Created: 12 February 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
                                                               CategoryTransport

------------------------------------------------------------------------------*/

/**
 * This class is intended to provide a serializable object that
 * can be used to bulk-dump a static description of the category
 * and base structures on the server to the client.
 */

public class CategoryTransport implements java.io.Serializable {

  static final long serialVersionUID = 3428227875010080755L;

  // ---

  StringBuffer buffer;

  /* -- */

  public CategoryTransport()
  {
  }

  /**
   *
   * Client side accessor
   *
   */

  public CategoryDump getTree()
  {
    return new CategoryDump(null, buffer.toString().toCharArray(), 0);
  }

  /**
   * <p>This method is provided so that server-side code can add chunks of data for serialization.
   * In the CategoryTransport case, the
   * {@link arlut.csd.ganymede.server.DBBaseCategory#addCategoryToTransport(arlut.csd.ganymede.common.CategoryTransport,arlut.csd.ganymede.server.GanymedeSession, boolean)}
   * method is responsible for calling addChunk to build
   * 'this' up for serialization.</p>
   */

  public void addChunk(String text)
  {
    char[] chars;

    /* -- */

    //    System.err.println("Server adding chunk " + label + ":" + operand);

    if (buffer == null)
      {
        buffer = new StringBuffer();
      }

    // add our label

    if (text != null)
      {
        chars = text.toCharArray();
      }
    else
      {
        buffer.append("|");
        return;
      }

    for (int j = 0; j < chars.length; j++)
      {
        if (chars[j] == '|')
          {
            buffer.append("\\|");
          }
        else if (chars[j] == '\\')
          {
            buffer.append("\\\\");
          }
        else
          {
            buffer.append(chars[j]);
          }
      }

    buffer.append("|");
  }
}
