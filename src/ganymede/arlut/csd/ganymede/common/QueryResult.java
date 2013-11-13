/*

   QueryResult.java

   This class is a serializable object-list result object, which
   conveys results from a query/list operation along with methods that
   can be used to extract the results out of the query/list.

   Created: 1 October 1997

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

import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import arlut.csd.JDataComponent.listHandle;
import arlut.csd.Util.VecSortInsert;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     QueryResult

------------------------------------------------------------------------------*/

/**
 * <p>This class is a serializable object-list result object, which
 * conveys results from a query/list operation along with methods that
 * can be used to extract the results out of the query/list.</p>
 */

public class QueryResult implements java.io.Serializable {

  static final long serialVersionUID = 8575279549274172762L;

  static final boolean debug = false;

  public static final Comparator comparator = new Comparator()
    {
      public int compare(Object o_a, Object o_b)
        {
          ObjectHandle a, b;

          a = (ObjectHandle) o_a;
          b = (ObjectHandle) o_b;
          int comp = 0;

          comp = a.getLabel().compareToIgnoreCase(b.getLabel());

          if (comp < 0)
            {
              return -1;
            }
          else if (comp > 0)
            {
              return 1;
            }
          else
            {
              return 0;
            }
        }
    };

  // ---

  // for use pre-serialized

  private transient HashMap<Invid, String> invidMap = null;
  private transient Set<String> labelSet = null;
  private boolean forTransport = true;
  private transient boolean nonEditable = false;

  // for transport

  StringBuffer buffer;

  // for use post-serialized

  transient private boolean unpacked = false;

  transient Vector<ObjectHandle> handles = null;
  transient Vector<String> labelList = null;
  transient Vector<Invid> invidList = null;

  transient VecSortInsert inserter;

  /* -- */

  public QueryResult()
  {
    buffer = new StringBuffer();
    invidMap = new HashMap<Invid, String>();
    labelSet = new HashSet<String>();
    handles = new Vector<ObjectHandle>();
  }

  /**
   * Constructor.
   *
   * @param forTransport If true, this QueryResult will prepare information
   * fed into it for transport by maintaining a StringBuffer.
   */

  public QueryResult(boolean forTransport)
  {
    this();
    this.forTransport = forTransport;
  }

  /**
   * This method is used to add a simple String to the QueryResult's
   * serializable buffer.  It is intended to be called on the server,
   * but may also be called on the client for result augmentation.
   */

  public void addRow(String label)
  {
    addRow(null, label, false, false, false, false);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public void addRow(Invid invid, String label, boolean editable)
  {
    addRow(invid, label, false, false, false, editable);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public void addRow(ObjectHandle handle)
  {
    addRow(handle.invid, handle.label, handle.inactive,
           handle.expirationSet, handle.removalSet,
           handle.editable);
  }

  /**
   * This method is used to lock this QueryResult so that it cannot be
   * changed.  This will be used in circumstances where the Ganymede
   * server is caching a singleton QueryResult for some static list of
   * values.
   */

  public void setNonEditable()
  {
    this.nonEditable = true;
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public synchronized void addRow(Invid invid, String label,
                                  boolean inactive,
                                  boolean expirationSet,
                                  boolean removalSet,
                                  boolean editable)
  {
    if (debug)
      {
        System.err.println("QueryResult: addRow(" + invid + "," + label + ")");
      }

    if (nonEditable)
      {
        throw new RuntimeException("QueryResult.addRow(): non-editable QueryResult");
      }

    if (label == null)
      {
        throw new NullPointerException("QueryResult.addRow(): null label passed in");
      }

    // don't add an object we've already got here

    if (invid != null && containsInvid(invid))
      {
        return;
      }
    else if (invid == null && containsLabel(label))
      {
        return;
      }

    handles.add(new ObjectHandle(label, invid,
                                 inactive, expirationSet,
                                 removalSet, editable));

    if (forTransport)
      {
        // encode any true bits

        if (inactive)
          {
            buffer.append("A");
          }

        if (expirationSet)
          {
            buffer.append("B");
          }

        if (removalSet)
          {
            buffer.append("C");
          }

        if (editable)
          {
            buffer.append("D");
          }

        buffer.append("|");

        if (invid != null)
          {
            buffer.append(invid.toString());
          }

        buffer.append("|");
        char[] chars = label.toCharArray();

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

        buffer.append("\n");
      }

    if (invid != null)
      {
        invidMap.put(invid, label);

        if (invidList != null)
          {
            invidList.add(invid);
          }
      }

    labelSet.add(label);

    if (labelList != null)
      {
        labelList.add(label);
      }

    unpacked = false;
  }

  // ***
  //
  // The following methods are intended to be called on a QueryResult
  // after it has been serialized and passed from the server to the
  // client.
  //
  // ***

  /**
   * <p>This method is used by arlut.csd.ganymede.client.objectList to
   * get access to the raw and sorted vector of ObjectHandle's
   * post-serialization.</p>
   *
   * <p>Note that this method does not clone our handles vector, we'll
   * just assume that whatever the objectList class on the client does
   * to this vector, we're not going to disturb anyone else who will
   * be looking at the handle list on this query result object.</p>
   */

  public Vector<ObjectHandle> getHandles()
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    if (nonEditable)
      {
        Vector result = new Vector();
        result.addAll(handles);

        return result;
      }
    else
      {
        return handles;
      }
  }

  public Invid getInvid(int row)
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    return handles.get(row).getInvid();
  }

  public Vector<Invid> getInvids()
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    if (nonEditable)
      {
        Vector<Invid> myInvidList = new Vector<Invid>();

        for (ObjectHandle handle: handles)
          {
            myInvidList.add(handle.getInvid());
          }

        return myInvidList;
      }
    else
      {
        if (this.invidList == null)
          {
            this.invidList = new Vector<Invid>();

            for (ObjectHandle handle: handles)
              {
                this.invidList.add(handle.getInvid());
              }
          }

        return this.invidList;
      }
  }

  public Vector<String> getLabels()
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    if (nonEditable)
      {
        Vector<String> myLabelList = new Vector<String>();

        for (ObjectHandle handle: handles)
          {
            myLabelList.add(handle.getLabel());
          }

        return myLabelList;
      }
    else
      {
        if (labelList == null)
          {
            labelList = new Vector<String>();

            for (ObjectHandle handle: handles)
              {
                labelList.add(handle.getLabel());
              }
          }

        return labelList;
      }
  }

  public String getLabel(int row)
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    return handles.get(row).getLabel();
  }

  public boolean isForTransport()
  {
    return forTransport;
  }

  public int size()
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    return handles.size();
  }

  /**
   * Returns a complete {@link arlut.csd.JDataComponent.listHandle listHandle}
   * Vector representation of the results included in this QueryResult.
   */

  public Vector<listHandle> getListHandles()
  {
    return getListHandles(true, true);
  }

  /**
   * Returns a (possibly filtered) {@link arlut.csd.JDataComponent.listHandle listHandle}
   * Vector representation of the results included in this QueryResult.
   *
   * @param includeInactives if false, inactive objects' handles won't be included
   * in the returned vector
   * @param includeNonEditables if false, non-editable objects' handles won't be included
   * in the returned vector
   */

  public synchronized Vector<listHandle> getListHandles(boolean includeInactives,
                                                        boolean includeNonEditables)
  {
    Vector<listHandle> valueHandles = new Vector<listHandle>();

    /* -- */

    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    for (ObjectHandle handle: handles)
      {
        if ((includeInactives || !handle.isInactive()) &&
            (includeNonEditables || handle.isEditable()))
          {
            valueHandles.add(handle.getListHandle());
          }
      }

    return valueHandles;
  }

  /**
   * Returns the listHandle for this row.
   */

  public listHandle getListHandle(int row)
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    return handles.get(row).getListHandle();
  }

  /**
   * Returns the ObjectHandle for this row.
   */

  public ObjectHandle getObjectHandle(int row)
  {
    if (forTransport && !unpacked)
      {
        unpackBuffer();
      }

    return handles.get(row);
  }

  // ***
  //
  // pre-serialization (server-side) methods
  //
  // ***

  /**
   * <p>This method is provided for the server to optimize it's
   * QueryResult loading operations, and is not intended for use
   * post-serialization.</p>
   */

  public synchronized boolean containsInvid(Invid invid)
  {
    return invidMap.containsKey(invid);
  }

  /**
   * <p>This method is provided for the server to optimize it's
   * QueryResult loading operations, and is not intended for use
   * post-serialization.</p>
   */

  public synchronized boolean containsLabel(String label)
  {
    return labelSet.contains(label);
  }

  /**
   * <p>This is a pre-serialization method for concatenating another
   * (for transport) QueryResult to ourself.</p>
   */

  public void append(QueryResult result)
  {
    if (nonEditable)
      {
        throw new RuntimeException("Can't append to a non-editable QueryResult");
      }

    buffer.append(result.buffer.toString());
    unpacked = false;

    this.invidMap.putAll(result.invidMap);
    this.labelSet.addAll(result.labelSet);
  }

  /**
   * <p>This method returns a QueryResult which holds the intersection
   * of the contents of this QueryResult and the contents of
   * operand.</p>
   */

  public synchronized QueryResult intersection(QueryResult operand)
  {
    if (nonEditable)
      {
        throw new RuntimeException("Can't intersect a non-editable QueryResult");
      }

    QueryResult result = new QueryResult(forTransport);

    /* -- */

    if (operand == null || operand.size() == 0)
      {
        return result;
      }

    for (ObjectHandle handle: handles)
      {
        if (handle.getInvid() != null)
          {
            if (operand.containsInvid(handle.getInvid()))
              {
                result.addRow(handle);
              }
          }
        else
          {
            if (operand.containsLabel(handle.getLabel()))
              {
                result.addRow(handle);
              }
          }
      }

    return result;
  }

  // ***
  //
  // private methods
  //
  // ***

  /**
   * <p>Private method to handle building up our datastructure on the
   * post-serialization side.  Sorts the handles vector by label as it
   * is extracted.</p>
   */

  private synchronized void unpackBuffer()
  {
    char[] chars;
    String results = buffer.toString();
    StringBuilder tempString = new StringBuilder();
    int index = 0;
    int rows = 0;

    String label;
    Invid invid;
    boolean inactive, expirationSet, removalSet, editable;

    /* -- */

    if (debug)
      {
        System.err.println("QueryResult.unpackBuffer()");
      }

    // prepare our handle vector

    handles = new Vector();

    inserter = new VecSortInsert(comparator);

    // turn our serialized buffer into an array of chars for fast
    // processing

    chars = results.toCharArray();

    if (debug)
      {
        System.err.println("*** unpacking buffer");
      }

    // now read in all the result lines

    while (index < chars.length)
      {
        inactive = false;
        expirationSet = false;
        removalSet = false;
        editable = false;

        // first read in the bits

        while (chars[index] != '|')
          {
            if (chars[index] == 'A')
              {
                inactive = true;
              }
            else if (chars[index] == 'B')
              {
                expirationSet = true;
              }
            else if (chars[index] == 'C')
              {
                removalSet = true;
              }
            else if (chars[index] == 'D')
              {
                editable = true;
              }

            index++;
          }

        index++;                // skip separator |

        // now read in the Invid

        tempString.setLength(0); // truncate the buffer

        while (chars[index] != '|')
          {
            if (chars[index] == '\n')
              {
                throw new RuntimeException("parse error in row" + rows);
              }

            tempString.append(chars[index++]);
          }

        if (tempString.toString().length() != 0)
          {
            invid = Invid.createInvid(tempString.toString());
          }
        else
          {
            invid = null;
          }

        index++;                // skip over |

        // now read in the label for this invid

        tempString.setLength(0); // truncate the buffer

        while (chars[index] != '\n')
          {
            // if we have a backslashed character, take the backslashed char
            // as a literal

            if (chars[index] == '\\')
              {
                index++;
              }

            tempString.append(chars[index++]);
          }

        label = tempString.toString();

        inserter.insert(handles, new ObjectHandle(label, invid,
                                                  inactive, expirationSet,
                                                  removalSet, editable));

        rows++;
        index++; // skip newline
      }

    unpacked = true;
  }

  /**
   * For debug.
   */

  public String getBuffer()
  {
    return buffer.toString();
  }
}
