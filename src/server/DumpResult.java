/*

   DumpResult.java

   This class is a serializable dump result object, which conveys
   results from a dump operation along with methods that can be
   used to extract the results  out of the dump.
   
   Created: 25 September 1997
   Release: $Name:  $
   Version: $Revision: 1.12 $
   Last Mod Date: $Date: 2001/01/01 18:05:04 $
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

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DumpResult

------------------------------------------------------------------------------*/

public class DumpResult implements java.io.Serializable {

  static final boolean debug = false;

  static final long serialVersionUID = 2515833829305301719L;

  // --

  // for transport

  StringBuffer buffer;

  transient private boolean unpacked = false;

  // for use pre-serialized

  transient Vector fieldDefs = null;

  // for use post-serialized

  transient private boolean postTransport = true;
  transient Vector headers = null;
  transient Vector types = null;
  transient Vector invids = null;
  transient Vector rows = null;

  /* -- */

  public DumpResult(Vector fieldDefs)
  {
    DBObjectBaseField field;
    char[] chars;
    Vector typeList = new Vector();

    /* -- */

    postTransport = false;
    this.fieldDefs = fieldDefs;
    buffer = new StringBuffer();

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

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	field = (DBObjectBaseField) fieldDefs.elementAt(i);
	buffer.append(field.getType());
	buffer.append("|");
      }

    buffer.append("\n");
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
	System.err.println("DumpResult: addRow(" + object.getLabel() + ")");
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
	    Ganymede.debug("Error, DumpResult.addRow found null encoding string in field " + field);
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

    buffer.append(localBuffer.toString());
  }

  //
  //
  // The following methods are intended to be called on a DumpResult
  // after it has been serialized and passed from the server to the
  // client.
  //
  //

  public Vector getHeaders()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return headers;
  }

  public Vector getTypes()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return types;
  }

  public Vector getInvids()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return invids;
  }

  public Invid getInvid(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (Invid) invids.elementAt(row);
  }

  public Vector getRows()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows;
  }
  
  public Vector getFieldRow(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (Vector) rows.elementAt(row);
  }

  public Object getResult(int row, int col)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return ((Vector) rows.elementAt(row)).elementAt(col);
  }

  public int resultSize()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows.size();
  }

  private synchronized void unpackBuffer()
  {
    char[] chars;
    String results = buffer.toString();
    arlut.csd.Util.SharedStringBuffer tempString = new arlut.csd.Util.SharedStringBuffer();
    int index = 0;
    Vector rowVect;
    short currentFieldType = -1;

    /* -- */

    headers = new Vector();
    types = new Vector();
    invids = new Vector();
    rows = new Vector();

    chars = results.toCharArray();

    // read in the header definition line

    if (debug)
      {
	System.err.println("*** unpacking buffer");
      }
    
    while (chars[index] != '\n')
      {
	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in header list");
	      }

	    // if we have a backslashed character, take the backslashed char
	    // as a literal

	    if (chars[index] == '\\')
	      {
		index++;
	      }

	    tempString.append(chars[index++]);
	  }

	index++;		// skip past |

	if (debug)
	  {
	    System.err.println("Header[" + headers.size() + "]: " + tempString.toString());
	  }

	headers.addElement(tempString.toString());
      }

    index++;			// skip past \n

    // read in the types line

    if (debug)
      {
	System.err.println("*** unpacking types line");
      }
    
    while (chars[index] != '\n')
      {
	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in header list");
	      }

	    // if we have a backslashed character, take the backslashed char
	    // as a literal

	    if (chars[index] == '\\')
	      {
		index++;
	      }

	    tempString.append(chars[index++]);
	  }

	index++;		// skip past |
	
	if (debug)
	  {
	    System.err.println("Type[" + types.size() + "]: " + tempString.toString());
	  }

	try
	  {
	    types.addElement(Short.valueOf(tempString.toString()));
	    //	    currentFieldType = ((Short) types.lastElement()).shortValue();
	  }
	catch (NumberFormatException ex)
	  {
	    throw new RuntimeException("Ay Carumba!  Number Parse Error! " + ex);
	  }
      }

    index++;			// skip past \n

    // now read in all the result lines

    while (index < chars.length)
      {
	// first read in the Invid

	tempString.setLength(0); // truncate the buffer

	if (debug)
	  {
	    System.err.println("*** Unpacking row " + rows.size());
	  }

	while (chars[index] != '|')
	  {
	    // if we have a backslashed character, take the backslashed char
	    // as a literal
	    
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in row");
	      }
	    
	    tempString.append(chars[index++]);
	  }

	invids.addElement(new Invid(tempString.toString()));

	index++;		// skip over |

	// now read in the fields for this invid

	rowVect = new Vector();

	while (chars[index] != '\n')
	  {
	    tempString.setLength(0); // truncate the buffer

	    while (chars[index] != '|')
	      {
		// if we have a backslashed character, take the backslashed char
		// as a literal

		if (chars[index] == '\n')
		  {
		    throw new RuntimeException("parse error in header list");
		  }

		if (chars[index] == '\\')
		  {
		    index++;
		  }

		tempString.append(chars[index++]);
	      }

	    index++;		// skip |

	    currentFieldType = ((Short) types.elementAt(rowVect.size())).shortValue();

	    switch (currentFieldType)
	      {
	      case FieldType.DATE:

		if (debug)
		  {
		    System.err.println("parsing date: " + tempString.toString());
		  }

		if (tempString.toString().equals("null") || tempString.toString().equals(""))
		  {
		    rowVect.addElement(null);
		  }
		else
		  {
		    try
		      {
			rowVect.addElement(new Date(Long.parseLong(tempString.toString())));
		      }
		    catch (NumberFormatException ex)
		      {
			throw new RuntimeException("couldn't parse Long encoding (" + tempString.toString()+"): " + ex);
		      }
		  }
		break;
		
	      case FieldType.NUMERIC:

		if (tempString.toString().equals("null") ||
		    tempString.toString().equals(""))
		  {
		    rowVect.addElement(null);
		  }
		else
		  {
		    try
		      {
			rowVect.addElement(new Integer(tempString.toString()));
		      }
		    catch (NumberFormatException ex)
		      {
			throw new RuntimeException("couldn't parse numeric encoding for string *" + 
						   tempString.toString() + "* :" + ex);
		      }
		  }
		break;

 	      case FieldType.FLOAT:
 
 		if (tempString.toString().equals("null") ||
 		    tempString.toString().equals(""))
 		  {
 		    rowVect.addElement(null);
 		  }
 		else
 		  {
 		    try
 		      {
 			rowVect.addElement(new Double(tempString.toString()));
 		      }
 		    catch (NumberFormatException ex)
 		      {
 			throw new RuntimeException("couldn't parse float encoding for string *" + 
 						   tempString.toString() + "* :" + ex);
 		      }
 		  }
 		break;

	      default:
		rowVect.addElement(tempString.toString());
	      }
	  }

	rows.addElement(rowVect);

	index++; // skip newline
      }

    unpacked = true;

    if (postTransport)
      {
	buffer = null;		// for GC
      }
  }

  /**
   *
   * This method breaks apart the data structures held
   * by this DumpResult.. it is intended to speed garbage
   * collection when the contents of this DumpResult buffer
   * have been processed and are no longer needed on the client.
   *
   */

  public void dissociate()
  {
    if (headers != null)
      {
	headers.removeAllElements();
	headers = null;
      }
    
    if (types != null)
      {
	types.removeAllElements();
	types = null;
      }

    if (invids != null)
      {
	invids.removeAllElements();
	invids = null;
      }

    if (rows != null)
      {
	rows.removeAllElements();
	rows = null;
      }
  }
}
