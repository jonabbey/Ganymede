/*

   DumpResultBuilder.java

   This class is a server-side factory tool used to generate serializable DumpResult
   objects free from any references to server-side only objects.

   Created: 14 April 2004

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

package arlut.csd.ganymede.server;

import java.util.List;

import arlut.csd.ganymede.common.DumpResult;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DumpResultBuilder

------------------------------------------------------------------------------*/

/**
 * <p>The DumpResultBuilder is a server-side factory tool used to
 * generate the serializable {@link
 * arlut.csd.ganymede.common.DumpResult DumpResult} object used to
 * pass data dump results to the client.</p>
 */

public class DumpResultBuilder {

  private DumpResult transport = null;
  private List<DBObjectBaseField> fieldDefs = null;

  /* -- */

  public DumpResultBuilder(List<DBObjectBaseField> fieldDefs)
  {
    this.initializeFields(fieldDefs);
  }

  /**
   * <p>Write the header names into the transport buffer.</p>
   */

  private void initializeFields(List<DBObjectBaseField> fieldDefs)
  {
    StringBuffer buffer = null;

    /* -- */

    this.fieldDefs = fieldDefs;

    transport = new DumpResult();

    buffer = transport.buffer;

    // first write out a line that defines the field information, in
    // bar-separated triples:
    // fieldName|fieldId|fieldType|fieldName|fieldId|fieldType|, etc.

    for (DBObjectBaseField field: fieldDefs)
      {
        for (char ch: field.getName().toCharArray())
          {
            if (ch == '|')
              {
                buffer.append("\\|");
              }
            else if (ch == '\n')
              {
                buffer.append("\\\n");
              }
            else if (ch == '\\')
              {
                buffer.append("\\\\");
              }
            else
              {
                buffer.append(ch);
              }
          }

        buffer.append("|");

        buffer.append(field.getID());
        buffer.append("|");

        buffer.append(field.getType());
        buffer.append("|");
      }

    buffer.append("\n");
  }

  /**
   * <p>Returns the {@link arlut.csd.ganymede.common.DumpResult
   * DumpResult} object created by this DumpResultBuilder.</p>
   */

  public DumpResult getDumpResult()
  {
    return transport;
  }

  /**
   * This method is used to add an object's information to
   * the dumpResult's serializable buffer.  It is intended
   * to be called on the server.
   */

  public void addRow(DBObject object)
  {
    addRow(object, null);
  }

  /**
   * This method is used to add an object's information to
   * the dumpResult's serializable buffer.  It is intended
   * to be called on the server.
   */

  public void addRow(DBObject object, GanymedeSession owner)
  {
    StringBuilder localBuffer = new StringBuilder();

    /* -- */

    localBuffer.append(object.getInvid().toString());
    localBuffer.append("|");

    for (DBObjectBaseField fieldDef: fieldDefs)
      {
        // make sure we have permission to see this field

        if (owner != null && !owner.getPermManager().getPerm(object, fieldDef.getID()).isVisible())
          {
            // nope, no permission, just terminate this field and
            // continue

            localBuffer.append("|");
            continue;
          }

        DBField field = (DBField) object.getField(fieldDef.getID());

        if (field == null)
          {
            localBuffer.append("|");
            continue;
          }

        // we use getEncodingString() here primarily so that
        // our dates are encoded in a fashion that can be
        // sorted on the client, and which can be presented in
        // whatever fashion the client chooses.

        String valString = field.getEncodingString();

        // I got a null pointer exception here

        if (valString == null)
          {
            Ganymede.debug("Error, DumpResultBuilder.addRow found null encoding string in field " + field);
            Ganymede.debug("Skipping data for object " + object);
            return;
          }

        for (char ch: valString.toCharArray())
          {
            if (ch == '|')
              {
                localBuffer.append("\\|");
              }
            else if (ch == '\n')
              {
                localBuffer.append("\\\n");
              }
            else if (ch == '\\')
              {
                localBuffer.append("\\\\");
              }
            else
              {
                localBuffer.append(ch);
              }
          }

        localBuffer.append("|");
      }

    localBuffer.append("\n");

    transport.buffer.append(localBuffer.toString());
  }
}
