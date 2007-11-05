/*

   DumpResultBuilder.java

   This class is a server-side factory tool used to generate serializable DumpResult
   objects free from any references to server-side only objects.
   
   Created: 14 April 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2007
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

package arlut.csd.ganymede.server;

import java.util.Vector;

import arlut.csd.ganymede.common.DumpResult;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DumpResultBuilder

------------------------------------------------------------------------------*/

/**
 * <p>The DumpResultBuilder is a server-side factory tool used to
 * generate the serializable {@link arlut.csd.ganymede.common.DumpResult
 * DumpResult} object used to pass data dump results to the client.</p>
 */

public class DumpResultBuilder {

  static final boolean debug = false;

  // ---

  /** our transport
   */

  private DumpResult transport = null;
  private Vector fieldDefs = null;

  /* -- */

  public DumpResultBuilder(Vector fieldDefs)
  {
    this.initializeFields(fieldDefs);
  }

  public void initializeFields(Vector fieldDefs)
  {
    DBObjectBaseField field;
    char[] chars;
    StringBuffer buffer = null;

    /* -- */

    this.fieldDefs = fieldDefs;

    transport = new DumpResult();

    buffer = transport.buffer;

    // first write out a line that defines the field names

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	field = (DBObjectBaseField) fieldDefs.elementAt(i);

	// need to also check here for permission restrictions on 
	// field visibility

	chars = field.getName().toCharArray();
	
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		buffer.append("\\|");
	      }
	    else if (chars[j] == '\n')
	      {
		buffer.append("\\\n");
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

    buffer.append("\n");

    // then a line that defines the field id numbers

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	field = (DBObjectBaseField) fieldDefs.elementAt(i);

        buffer.append(field.getID());
        buffer.append("|");
      }

    buffer.append("\n");

    // then a line that defines the numeric type codes of the fields
    // (see arlut.csd.ganymede.common.FieldType for interpretation)

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	field = (DBObjectBaseField) fieldDefs.elementAt(i);
	buffer.append(field.getType());
	buffer.append("|");
      }

    buffer.append("\n");
  }

  /**
   * <p>Returns the {@link arlut.csd.ganymede.common.DumpResult DumpResult} object
   * created by this DumpResultBuilder.</p>
   */

  public DumpResult getDumpResult()
  {
    return transport;
  }

  /**
   *
   * This method is used to add an object's information to
   * the dumpResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public void addRow(DBObject object)
  {
    addRow(object, null);
  }

  /**
   *
   * This method is used to add an object's information to
   * the dumpResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public void addRow(DBObject object, GanymedeSession owner)
  {
    StringBuffer localBuffer = new StringBuffer();
    DBObjectBaseField fieldDef;
    DBField field;
    char[] chars;

    /* -- */

    if (debug)
      {
	System.err.println("DumpResultBuilder: addRow(" + object.getLabel() + ")");
      }

    localBuffer.append(object.getInvid().toString());
    localBuffer.append("|");

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	fieldDef = (DBObjectBaseField) fieldDefs.elementAt(i);

	if (debug)
	  {
	    System.err.print("_");
	  }

	// make sure we have permission to see this field

	if (owner != null && !owner.getPerm(object, fieldDef.getID()).isVisible())
	  {
	    // nope, no permission, just terminate this field and
	    // continue

	    localBuffer.append("|");

	    if (debug)
	      {
		System.err.println("n");
	      }

	    continue;
	  }

	if (debug)
	  {
	    System.err.print("y");
	  }
	
	field = (DBField) object.getField(fieldDef.getID());

	if (field == null)
	  {
	    localBuffer.append("|");

	    if (debug)
	      {
		System.err.println(" x");
	      }

	    continue;
	  }

	// we use getEncodingString() here primarily so that
	// our dates are encoded in a fashion that can be
	// sorted on the client, and which can be presented in
	// whatever fashion the client chooses.

	if (debug)
	  {
	    System.err.println("+");
	  }

	String valString = field.getEncodingString();

	// I got a null pointer exception here 

	if (valString == null)
	  {
	    Ganymede.debug("Error, DumpResultBuilder.addRow found null encoding string in field " + field);
	    Ganymede.debug("Skipping data for object " + object);
	    return;
	  }

	chars = valString.toCharArray();

	if (debug)
	  {
	    System.err.println(" ok");
	  }
		
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		localBuffer.append("\\|");
	      }
	    else if (chars[j] == '\n')
	      {
		localBuffer.append("\\\n");
	      }
	    else if (chars[j] == '\\')
	      {
		localBuffer.append("\\\\");
	      }
	    else
	      {
		localBuffer.append(chars[j]);
	      }
	  }
	
	localBuffer.append("|");
      }

    localBuffer.append("\n");

    transport.buffer.append(localBuffer.toString());
  }
}
