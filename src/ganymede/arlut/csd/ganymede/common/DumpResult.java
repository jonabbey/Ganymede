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

package arlut.csd.ganymede.common;

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
 * This class is a serializable transport object, used to transmit the results
 * of a data dump query to the client.  DumpResult objects are created by the
 * {@link arlut.csd.ganymede.server.DumpResultBuilder DumpResultBuilder} factory class.
 *
 * The way it works is that DumpResultBuilder creates the
 * DumpResult objects, which is transmitted through RMI to the client.
 * The client can then call the various accessor methods to access the
 * serialized query results.
 *
 * DumpResult encodes a list of field headers by name, a list of field types
 * encoded as {@link java.lang.Short Shorts} coded with the
 * values enumerated in the {@link arlut.csd.ganymede.common.FieldType FieldType}
 * interface, and a list of object rows, each of which contains a Vector of
 * encoded field values.
 *
 * Field values are encoded as follows:
 *
 * Date fields as {@link java.util.Date Date} objects
 * Float fields as {@link java.lang.Double Double} objects
 * Numeric fields as {@link java.lang.Integer Integer} objects
 * <br/>
 * And Strings for everything else.
 *
 * The GUI client uses this object to generate its query result tables..
 *
 * Later Note:
 *
 *  Yes, I know how utterly horrifying this is.  It's something I did
 *  very early on during development, and it worked well for high
 *  speed data dumping, so I just kept it.  Mea culpa, mea maxima
 *  culpa.
 *
 *  Yet, it works.
 */

public class DumpResult implements java.io.Serializable, List {

  static final boolean debug = false;

  static final long serialVersionUID = 8688161796723967714L;

  // --

  // for transport

  public StringBuffer buffer = null;

  // for use post-serialized.. note that transient fields don't
  // actually get initialzed post serialization, so the initializers
  // here are actually redundant and non-operative on the client side
  // post serialization.

  transient private boolean unpacked = false;

  transient Vector headerObjects = null;
  transient Vector headers = null;
  transient Vector invids = null;
  transient Vector rows = null;

  /* -- */

  public DumpResult()
  {
    buffer = new StringBuffer();
  }

  /**
   * This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field names, used to generate the
   * list of column headers in the GUI client.
   *
   * Note: The Vector returned is "live", and should not be modified
   * by the caller, at the risk of surprising behavior.
   */

  public synchronized Vector getHeaders()
  {
    checkBuffer();

    if (headers == null)
      {
        headers = new Vector(headerObjects.size());

        for (int i = 0; i < headerObjects.size(); i++)
          {
            headers.addElement(((DumpResultCol) headerObjects.elementAt(i)).getName());
          }
      }

    return headers;
  }

  /**
   * This method can be called on the client to obtain an independent
   * Vector of {@link arlut.csd.ganymede.common.DumpResultCol
   * DumpResultCol} objects, which define the field name, field id,
   * and field type for each column in this DumpResult.
   *
   * Note: The Vector returned is "live", and should not be modified
   * by the caller, at the risk of surprising behavior.
   */

  public Vector getHeaderObjects()
  {
    checkBuffer();

    return headerObjects;
  }

  /**
   * Returns the name of the field encoded in column col of the DumpResult.
   */

  public String getFieldName(int col)
  {
    checkBuffer();

    DumpResultCol headerObj = (DumpResultCol) headerObjects.elementAt(col);

    return headerObj.getName();
  }

  /**
   * Returns the field code for the field encoded in column col of the
   * DumpResult.
   */

  public short getFieldId(int col)
  {
    checkBuffer();

    DumpResultCol headerObj = (DumpResultCol) headerObjects.elementAt(col);

    return headerObj.getFieldId();
  }

  /**
   * Returns the field type for the field encoded in column col of the
   * DumpResult.
   *
   * The field type returned is to be interpreted according to the
   * values enumerated in the {@link
   * arlut.csd.ganymede.common.FieldType FieldType} interface.
   */

  public short getFieldType(int col)
  {
    checkBuffer();

    DumpResultCol headerObj = (DumpResultCol) headerObjects.elementAt(col);

    return headerObj.getFieldType();
  }

  /**
   * This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of {@link arlut.csd.ganymede.common.Invid
   * Invids}, identifying the objects that are being returned in the
   * DumpResult.
   *
   * Note: The Vector returned is "live", and should not be modified
   * by the caller, at the risk of surprising behavior.
   */

  public Vector getInvids()
  {
    checkBuffer();

    return invids;
  }

  /**
   * This method can be called on the client to obtain the object
   * identifier {@link arlut.csd.ganymede.common.Invid Invid} for a
   * given result row.
   */

  public Invid getInvid(int row)
  {
    checkBuffer();

    return (Invid) invids.elementAt(row);
  }

  /**
   * This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of Vectors, each of which contains
   * the data values returned for each object, in field order matching
   * the field names and types returned by {@link arlut.csd.ganymede.common.DumpResult#getHeaders getHeaders()}
   * and {@link arlut.csd.ganymede.common.DumpResult#getHeaderObjects getHeaderObjects()}.
   *
   * Note: The Vector returned is "live", and should not be modified
   * by the caller, at the risk of surprising behavior.
   */

  public Vector getRows()
  {
    checkBuffer();

    return rows;
  }

  /**
   * This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} containing the data values returned for
   * the object at row <i>row</i>, in field order matching the field
   * names and types returned by {@link
   * arlut.csd.ganymede.common.DumpResult#getHeaders getHeaders()} and
   * {@link arlut.csd.ganymede.common.DumpResult#getHeaderObjects
   * getHeaderObjects()}.
   */
  
  public Vector getFieldRow(int rowNumber)
  {
    checkBuffer();

    Map rowMap = (Map) rows.get(rowNumber);
    Vector row = new Vector(headerObjects.size());
    Iterator iter;
    String currentHeader;
    
    for (iter = headerObjects.iterator(); iter.hasNext();)
      {
      	currentHeader = ((DumpResultCol) iter.next()).getName();
      	row.add(rowMap.get(currentHeader));
      }

    return row;
  }

  /**
   * This method can be called on the client to obtain an Object
   * encoding the result value for the <i>col</i>th field in the
   * <i>row</i>th object.  These Objects may be a {@link java.lang.Double Double}
   * for Float fields, an {@link java.lang.Integer Integer} for Numeric fields,
   * a {@link java.util.Date Date} for Date fields, or a String for other
   * fields.
   */

  public Object getResult(int row, int col)
  {
    checkBuffer();

    return (getFieldRow(row)).elementAt(col);
  }

  /**
   * This method can be called on the client to determine the
   * number of objects encoded in this DumpResult.
   */

  public int resultSize()
  {
    checkBuffer();

    return rows.size();
  }

  /**
   * This method takes care of deserializing the StringBuffer we
   * contain, to crack out the data we are interested in conveying.
   */

  private synchronized void checkBuffer()
  {
    if (unpacked)
      {
        return;
      }

    char[] chars = buffer.toString().toCharArray();;
    arlut.csd.Util.SharedStringBuffer tempString = new arlut.csd.Util.SharedStringBuffer();
    int index = 0;
    Map rowMap;
    short currentFieldType = -1;
    String currentHeader;

    /* -- */

    headerObjects = new Vector();
    headers = new Vector();
    invids = new Vector();
    rows = new Vector();

    // read in the header definition line

    if (debug)
      {
	System.err.println("*** unpacking buffer");
      }
    
    while (chars[index] != '\n')
      {
        String fieldName;
        short fieldId;
        short fieldType;

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

        fieldName = tempString.toString();

        tempString.setLength(0);  // truncate the buffer again

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

        index++;                // skip trailing | marker

        fieldId = Short.valueOf(tempString.toString()).shortValue();

        tempString.setLength(0);  // truncate the buffer again

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

        index++;                // skip trailing | marker

        fieldType = Short.valueOf(tempString.toString()).shortValue();

        headerObjects.addElement(new DumpResultCol(fieldName, fieldId, fieldType));
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

	invids.addElement(Invid.createInvid(tempString.toString()));

	index++;		// skip over |

	// now read in the fields for this invid

	rowMap = new HashMap(headerObjects.size());

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

            DumpResultCol header = (DumpResultCol) headerObjects.elementAt(rowMap.size());

	    currentFieldType = header.getFieldType();
	    currentHeader = header.getName();

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
    if (headerObjects != null)
      {
	headerObjects.removeAllElements();
	headerObjects = null;
      }

    if (headers != null)
      {
	headers.removeAllElements();
	headers = null;
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
    checkBuffer();

    return rows.contains(o);
  }
  
  /* 
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection c)
  {
    checkBuffer();

    return rows.containsAll(c);
  }
  
  /* 
   * @see java.util.List#get(int)
   */
  public Object get(int index)
  {
    checkBuffer();

    return rows.get(index);
  }
  
  /* 
   * @see java.util.List#indexOf(java.lang.Object)
   */
  public int indexOf(Object o)
  {
    checkBuffer();

    return rows.indexOf(o);
  }
  
  /* 
   * @see java.util.Collection#isEmpty()
   */
  public boolean isEmpty()
  {
    checkBuffer();

    return rows.isEmpty();
  }
  
  /* 
   * @see java.util.Collection#iterator()
   */
  public Iterator iterator()
  {
    checkBuffer();

    return rows.iterator();
  }
  
  /* 
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(Object o)
  {
    checkBuffer();

    return rows.lastIndexOf(o);
  }
  
  /* 
   * @see java.util.List#listIterator()
   */
  public ListIterator listIterator()
  {
    checkBuffer();

    return rows.listIterator();
  }
  
  /* 
   * @see java.util.List#listIterator(int)
   */
  public ListIterator listIterator(int index)
  {
    checkBuffer();

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
    checkBuffer();

    return rows.size();
  }
  
  /* 
   * @see java.util.List#subList(int, int)
   */
  public List subList(int fromIndex, int toIndex)
  {
    checkBuffer();

    return rows.subList(fromIndex, toIndex);
  }
  
  /* 
   * @see java.util.Collection#toArray()
   */
  public Object[] toArray()
  {
    checkBuffer();

    return rows.toArray();
  }
  
  /* 
   * @see java.util.Collection#toArray(java.lang.Object[])
   */
  public Object[] toArray(Object[] a)
  {
    checkBuffer();

    return rows.toArray(a);
  }
}
