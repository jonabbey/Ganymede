/*

   DumpResult.java

   This class is a serializable dump result object, which conveys
   results from a dump operation along with methods that can be
   used to extract the results  out of the dump.
   
   Created: 25 September 1997
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DumpResult

------------------------------------------------------------------------------*/

/**
 * <p>This class is a serializable transport object, used to transmit the results
 * of a data dump query to the client.  DumpResult objects are created by the
 * {@link arlut.csd.ddroid.server.DumpResultBuilder DumpResultBuilder} factory class.</p>
 *
 * <p>The way it works is that DumpResultBuilder creates the
 * DumpResult objects, which is transmitted through RMI to the client.
 * The client can then call the various accessor methods to access the
 * serialized query results.</p>
 *
 * <p>DumpResult encodes a list of field headers by name, a list of field types
 * encoded as {@link java.lang.Short Shorts} coded with the
 * values enumerated in the {@link arlut.csd.ddroid.common.FieldType FieldType}
 * interface, and a list of object rows, each of which contains a Vector of
 * encoded field values.</p>
 *
 * <p>Field values are encoded as follows:</p>
 *
 * <p>Date fields as {@link java.util.Date Date} objects</p>
 * <p>Float fields as {@link java.lang.Double Double} objects</p>
 * <p>Numeric fields as {@link java.lang.Integer Integer} objects</p>
 * <br/>
 * <p>And Strings for everything else.</p>
 *
 * <p>The GUI client uses this object to generate its query result tables.</p>.
 */

public class DumpResult implements java.io.Serializable, List {

  static final boolean debug = false;

  /* XXX  static final long serialVersionUID = 2515833829305301719L; */

  // --

  // for transport

  public StringBuffer buffer = null;

  // for use post-serialized.. note that transient fields don't
  // actually get initialzed post serialization, so the initializers
  // here are actually redundant and non-operative on the client side
  // post serialization.

  transient private boolean unpacked = false;

  transient Vector headers = null;
  transient Vector types = null;
  transient Vector invids = null;
  transient Vector rows = null;

  /* -- */

  public DumpResult()
  {
    buffer = new StringBuffer();
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field names, used to generate the
   * list of column headers in the GUI client.</p>
   */

  public Vector getHeaders()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return headers;
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field type {@link java.lang.Short
   * Shorts} (enumerated in the {@link
   * arlut.csd.ddroid.common.FieldType FieldType} static constants),
   * identifying the types of fields for each column in the
   * DumpResult.</p>
   */

  public Vector getTypes()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return types;
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of {@link arlut.csd.ddroid.common.Invid
   * Invids}, identifying the objects that are being returned in the
   * DumpResult.</p>
   */

  public Vector getInvids()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return invids;
  }

  /**
   * <p>This method can be called on the client to obtain the object
   * identifier {@link arlut.csd.ddroid.common.Invid Invid} for a
   * given result row.</p>
   */

  public Invid getInvid(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (Invid) invids.elementAt(row);
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of Vectors, each of which contains
   * the data values returned for each object, in field order matching
   * the field names and types returned by {@link arlut.csd.ddroid.common.DumpResult#getHeaders getHeaders()}
   * and {@link arlut.csd.ddroid.common.DumpResult#getTypes getTypes()}.</p>
   */

  public Vector getRows()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows;
  }


  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} containing the data values returned for
   * the object at row <i>row</i>, in field order matching the field
   * names and types returned by {@link
   * arlut.csd.ddroid.common.DumpResult#getHeaders getHeaders()} and
   * {@link arlut.csd.ddroid.common.DumpResult#getTypes
   * getTypes()}.</p>
   */
  
  public Vector getFieldRow(int rowNumber)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    Map rowMap = (Map) rows.get(rowNumber);
    Vector row = new Vector(headers.size());
    Iterator iter;
    String currentHeader;
    
    for (iter = headers.iterator(); iter.hasNext();)
      {
      	currentHeader = (String) iter.next();
      	row.add(rowMap.get(currentHeader));
      }
    return row;
  }

  /**
   * <p>This method can be called on the client to obtain an Object
   * encoding the result value for the <i>col</i>th field in the
   * <i>row</i>th object.</p>  These Objects may be a {@link java.lang.Double Double}
   * for Float fields, an {@link java.lang.Integer Integer} for Numeric fields,
   * a {@link java.util.Date Date} for Date fields, or a String for other
   * fields.</p>
   */

  public Object getResult(int row, int col)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (getFieldRow(row)).elementAt(col);
  }

  /**
   * <p>This method can be called on the client to determine the
   * number of objects encoded in this DumpResult.</p>
   */

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
    Map rowMap;
    short currentFieldType = -1;
    String currentHeader;

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

	rowMap = new HashMap(headers.size());

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

	    currentFieldType = ((Short) types.elementAt(rowMap.size())).shortValue();
	    currentHeader = (String) headers.elementAt(rowMap.size());

	    switch (currentFieldType)
	      {
	      case FieldType.DATE:

		if (debug)
		  {
		    System.err.println("parsing date: " + tempString.toString());
		  }

		if (tempString.toString().equals("null") || tempString.toString().equals(""))
		  {
		    rowMap.put(currentHeader, null);
		  }
		else
		  {
		    try
		      {
			rowMap.put(currentHeader, new Date(Long.parseLong(tempString.toString())));
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
		    rowMap.put(currentHeader, null);
		  }
		else
		  {
		    try
		      {
			rowMap.put(currentHeader, new Integer(tempString.toString()));
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
 		    rowMap.put(currentHeader, null);
 		  }
 		else
 		  {
 		    try
 		      {
 			rowMap.put(currentHeader, new Double(tempString.toString()));
 		      }
 		    catch (NumberFormatException ex)
 		      {
 			throw new RuntimeException("couldn't parse float encoding for string *" + 
 						   tempString.toString() + "* :" + ex);
 		      }
 		  }
 		break;

	      default:
		rowMap.put(currentHeader, tempString.toString());
	      }
	  }
	
	rows.addElement(rowMap);

	index++; // skip newline
      }

    unpacked = true;
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

  /* ------------------------------------------------------------------------
   * This is the start of the List interface implementation
   * 
   */
  
  /* This is a no-op since a DumpResult is immutable.
   * 
   * @see java.util.List#add(int, java.lang.Object)
   */
  public void add(int index, Object element)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since a DumpResult is immutable.
   * 
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(Object o)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#addAll(java.util.Collection)
   */
  public boolean addAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.List#addAll(int, java.util.Collection)
   */
  public boolean addAll(int index, Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#clear()
   */
  public void clear()
  {
    throw new UnsupportedOperationException();
  }
  
  /* 
   * @see java.util.Collection#contains(java.lang.Object)
   */
  public boolean contains(Object o)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.contains(o);
  }
  
  /* 
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection c)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.containsAll(c);
  }
  
  /* 
   * @see java.util.List#get(int)
   */
  public Object get(int index)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.get(index);
  }
  
  /* 
   * @see java.util.List#indexOf(java.lang.Object)
   */
  public int indexOf(Object o)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.indexOf(o);
  }
  
  /* 
   * @see java.util.Collection#isEmpty()
   */
  public boolean isEmpty()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.isEmpty();
  }
  
  /* 
   * @see java.util.Collection#iterator()
   */
  public Iterator iterator()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.iterator();
  }
  
  /* 
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(Object o)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.lastIndexOf(o);
  }
  
  /* 
   * @see java.util.List#listIterator()
   */
  public ListIterator listIterator()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.listIterator();
  }
  
  /* 
   * @see java.util.List#listIterator(int)
   */
  public ListIterator listIterator(int index)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.listIterator(index);
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.List#remove(int)
   */
  public Object remove(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#remove(java.lang.Object)
   */
  public boolean remove(Object o)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#removeAll(java.util.Collection)
   */
  public boolean removeAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#retainAll(java.util.Collection)
   */
  public boolean retainAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.List#set(int, java.lang.Object)
   */
  public Object set(int index, Object element)
  {
    throw new UnsupportedOperationException();
  }
  
  /* 
   * @see java.util.Collection#size()
   */
  public int size()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.size();
  }
  
  /* 
   * @see java.util.List#subList(int, int)
   */
  public List subList(int fromIndex, int toIndex)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.subList(fromIndex, toIndex);
  }
  
  /* 
   * @see java.util.Collection#toArray()
   */
  public Object[] toArray()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.toArray();
  }
  
  /* 
   * @see java.util.Collection#toArray(java.lang.Object[])
   */
  public Object[] toArray(Object[] a)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.toArray(a);
  }
}
