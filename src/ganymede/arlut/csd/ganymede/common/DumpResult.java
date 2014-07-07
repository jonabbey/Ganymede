/*

   DumpResult.java

   This class is a serializable dump result object, which conveys
   results from a dump operation along with methods that can be used
   to extract the results out of the dump.

   Created: 25 September 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
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
 * <p>This class is a serializable transport object, used to transmit
 * the results of a data dump query to the client.  DumpResult objects
 * are created by the {@link
 * arlut.csd.ganymede.server.DumpResultBuilder DumpResultBuilder}
 * factory class.</p>
 *
 * <p>The way it works is that DumpResultBuilder creates the
 * DumpResult objects, which is transmitted through RMI to the client.
 * The client can then call the various accessor methods to access the
 * serialized query results.</p>
 *
 * <p>DumpResult encodes a list of field headers by name, a list of
 * field types encoded as {@link java.lang.Short Shorts} coded with
 * the values enumerated in the {@link
 * arlut.csd.ganymede.common.FieldType FieldType} interface, and a
 * list of object rows, each of which contains a Vector of encoded
 * field values.</p>
 *
 * <p>Field values are encoded as follows:</p>
 *
 * <ul>
 * <li>Date fields as {@link java.util.Date Date} objects</li>
 * <li>Float fields as {@link java.lang.Double Double} objects</li>
 * <li>Numeric fields as {@link java.lang.Integer Integer} objects</li>
 * </ul>
 *
 * <p>And Strings for everything else.</p>
 *
 * <p>The GUI client uses this object to generate its query result
 * tables.</p>
 *
 * <p>Later Note:</p>
 *
 * <p>Yes, I know how utterly horrifying this is.  It's something I
 * did very early on during development, and it worked well for high
 * speed data dumping, so I just kept it.  Mea culpa, mea maxima
 * culpa.</p>
 *
 * <p>Yet, it works.</p>
 */

public class DumpResult implements java.io.Serializable, List {

  static final boolean debug = false;

  static final long serialVersionUID = 8688161796723967714L;

  // ---

  // for transport

  public StringBuffer buffer = null;

  // for use post-serialized.. note that transient fields don't
  // actually get initialzed post serialization, so the initializers
  // here are actually redundant and non-operative on the client side
  // post serialization.

  transient private boolean unpacked = false;

  transient Vector<DumpResultCol> headerObjects = null;
  transient Vector<String> headers = null;
  transient Vector<Invid> invids = null;
  transient Vector<Map<String, Object>> rows = null;

  /* -- */

  public DumpResult()
  {
    buffer = new StringBuffer();
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field names, used to generate the
   * list of column headers in the GUI client.</p>
   *
   * <p>Note: The Vector returned is "live", and should not be
   * modified by the caller, at the risk of surprising behavior.</p>
   */

  public synchronized Vector<String> getHeaders()
  {
    checkBuffer();

    if (headers == null)
      {
        headers = new Vector<String>(headerObjects.size());

        for (DumpResultCol drc: headerObjects)
          {
            headers.add(drc.getName());
          }
      }

    return new Vector(headers);
  }

  /**
   * <p>This method can be called on the client to obtain an
   * independent Vector of {@link
   * arlut.csd.ganymede.common.DumpResultCol DumpResultCol} objects,
   * which define the field name, field id, and field type for each
   * column in this DumpResult.</p>
   *
   * <p>Note: The Vector returned is "live", and should not be
   * modified by the caller, at the risk of surprising behavior.</p>
   */

  public synchronized Vector<DumpResultCol> getHeaderObjects()
  {
    checkBuffer();

    return new Vector(headerObjects);
  }

  /**
   * <p>Returns the name of the field encoded in column col of the
   * DumpResult.</p>
   */

  public synchronized String getFieldName(int col)
  {
    checkBuffer();

    return headerObjects.get(col).getName();
  }

  /**
   * <p>Returns the field code for the field encoded in column col of the
   * DumpResult.</p>
   */

  public synchronized short getFieldId(int col)
  {
    checkBuffer();

    return headerObjects.get(col).getFieldId();
  }

  /**
   * <p>Returns the field type for the field encoded in column col of the
   * DumpResult.</p>
   *
   * <p>The field type returned is to be interpreted according to the
   * values enumerated in the {@link
   * arlut.csd.ganymede.common.FieldType FieldType} interface.</p>
   */

  public synchronized short getFieldType(int col)
  {
    checkBuffer();

    return headerObjects.get(col).getFieldType();
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of {@link
   * arlut.csd.ganymede.common.Invid Invids}, identifying the objects
   * that are being returned in the DumpResult.</p>
   *
   * <p>Note: The Vector returned is "live", and should not be
   * modified by the caller, at the risk of surprising behavior.</p>
   */

  public synchronized Vector<Invid> getInvids()
  {
    checkBuffer();

    return invids;
  }

  /**
   * <p>This method can be called on the client to obtain the object
   * identifier {@link arlut.csd.ganymede.common.Invid Invid} for a
   * given result row.</p>
   */

  public synchronized Invid getInvid(int row)
  {
    checkBuffer();

    return invids.get(row);
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of Vectors, each of which contains the
   * data values returned for each object, in field order matching the
   * field names and types returned by {@link
   * arlut.csd.ganymede.common.DumpResult#getHeaders getHeaders()} and
   * {@link arlut.csd.ganymede.common.DumpResult#getHeaderObjects
   * getHeaderObjects()}.</p>
   *
   * <p>Note: The Vector returned is "live", and should not be
   * modified by the caller, at the risk of surprising behavior.</p>
   */

  public synchronized Vector<Map<String,Object>> getRows()
  {
    checkBuffer();

    return rows;
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} containing the data values returned for
   * the object at row <i>row</i>, in field order matching the field
   * names and types returned by {@link
   * arlut.csd.ganymede.common.DumpResult#getHeaders getHeaders()} and
   * {@link arlut.csd.ganymede.common.DumpResult#getHeaderObjects
   * getHeaderObjects()}.</p>
   */

  public synchronized Vector<Object> getFieldRow(int rowNumber)
  {
    checkBuffer();

    Map<String, Object> rowMap = rows.get(rowNumber);
    Vector<Object> row = new Vector(headerObjects.size());

    for (DumpResultCol drc: headerObjects)
      {
        row.add(rowMap.get(drc.getName()));
      }

    return row;
  }

  /**
   * <p>This method can be called on the client to obtain an Object
   * encoding the result value for the <i>col</i>th field in the
   * <i>row</i>th object.  These Objects may be a {@link
   * java.lang.Double Double} for Float fields, an {@link
   * java.lang.Integer Integer} for Numeric fields, a {@link
   * java.util.Date Date} for Date fields, or a String for other
   * fields.</p>
   */

  public synchronized Object getResult(int row, int col)
  {
    checkBuffer();

    return getFieldRow(row).get(col);
  }

  /**
   * <p>This method can be called on the client to determine the
   * number of objects encoded in this DumpResult.</p>
   */

  public synchronized int resultSize()
  {
    checkBuffer();

    return rows.size();
  }

  /**
   * <p>This method takes care of deserializing the StringBuffer we
   * contain, to crack out the data we are interested in
   * conveying.</p>
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
    Map<String,Object> rowMap;
    short currentFieldType = -1;
    String currentHeader;

    /* -- */

    headerObjects = new Vector<DumpResultCol>();
    invids = new Vector<Invid>();
    rows = new Vector<Map<String,Object>>();

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

        index++;                // skip past |

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

        headerObjects.add(new DumpResultCol(fieldName, fieldId, fieldType));
      }

    index++;                    // skip past \n

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

        index++;                // skip over |

        // now read in the fields for this invid

        rowMap = new HashMap<String,Object>(headerObjects.size());

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

            index++;            // skip |

            DumpResultCol header = headerObjects.get(rowMap.size());

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
                        rowMap.put(currentHeader, Integer.valueOf(tempString.toString()));
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

        rows.add(rowMap);

        index++; // skip newline
      }

    unpacked = true;
  }

  /**
   * <p>This method breaks apart the data structures held by this
   * DumpResult.. it is intended to speed garbage collection when the
   * contents of this DumpResult buffer have been processed and are no
   * longer needed on the client.</p>
   */

  public synchronized void dissociate()
  {
    if (headerObjects != null)
      {
        headerObjects.clear();
        headerObjects = null;
      }

    if (headers != null)
      {
        headers.clear();
        headers = null;
      }

    if (invids != null)
      {
        invids.clear();
        invids = null;
      }

    if (rows != null)
      {
        rows.clear();
        rows = null;
      }
  }

  /* ------------------------------------------------------------------------
   * This is the start of the List interface implementation
   *
   */

  /**
   * This is a no-op since a DumpResult is immutable.
   *
   * @see java.util.List#add(int, java.lang.Object)
   */

  public void add(int index, Object element)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since a DumpResult is immutable.
   *
   * @see java.util.Collection#add(java.lang.Object)
   */

  public boolean add(Object o)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#addAll(java.util.Collection)
   */

  public boolean addAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   *  This is a no-op since DumpResult is immutable.
   *
   * @see java.util.List#addAll(int, java.util.Collection)
   */

  public boolean addAll(int index, Collection c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#clear()
   */

  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @see java.util.Collection#contains(java.lang.Object)
   */

  public boolean contains(Object o)
  {
    checkBuffer();

    return rows.contains(o);
  }

  /**
   * @see java.util.Collection#containsAll(java.util.Collection)
   */

  public boolean containsAll(Collection c)
  {
    checkBuffer();

    return rows.containsAll(c);
  }

  /**
   * @see java.util.List#get(int)
   */

  public Object get(int index)
  {
    checkBuffer();

    return rows.get(index);
  }

  /**
   * @see java.util.List#indexOf(java.lang.Object)
   */

  public int indexOf(Object o)
  {
    checkBuffer();

    return rows.indexOf(o);
  }

  /**
   * @see java.util.Collection#isEmpty()
   */

  public boolean isEmpty()
  {
    checkBuffer();

    return rows.isEmpty();
  }

  /**
   * @see java.util.Collection#iterator()
   */

  public Iterator iterator()
  {
    checkBuffer();

    return rows.iterator();
  }

  /**
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */

  public int lastIndexOf(Object o)
  {
    checkBuffer();

    return rows.lastIndexOf(o);
  }

  /**
   * @see java.util.List#listIterator()
   */

  public ListIterator listIterator()
  {
    checkBuffer();

    return rows.listIterator();
  }

  /**
   * @see java.util.List#listIterator(int)
   */

  public ListIterator listIterator(int index)
  {
    checkBuffer();

    return rows.listIterator(index);
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.List#remove(int)
   */

  public Object remove(int index)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#remove(java.lang.Object)
   */

  public boolean remove(Object o)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#removeAll(java.util.Collection)
   */

  public boolean removeAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#retainAll(java.util.Collection)
   */

  public boolean retainAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a no-op since DumpResult is immutable.
   *
   * @see java.util.List#set(int, java.lang.Object)
   */

  public Object set(int index, Object element)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * @see java.util.Collection#size()
   */

  public int size()
  {
    checkBuffer();

    return rows.size();
  }

  /**
   * @see java.util.List#subList(int, int)
   */

  public List subList(int fromIndex, int toIndex)
  {
    checkBuffer();

    return rows.subList(fromIndex, toIndex);
  }

  /**
   * @see java.util.Collection#toArray()
   */

  public Object[] toArray()
  {
    checkBuffer();

    return rows.toArray();
  }

  /**
   * @see java.util.Collection#toArray(java.lang.Object[])
   */

  public Object[] toArray(Object[] a)
  {
    checkBuffer();

    return rows.toArray(a);
  }
}
