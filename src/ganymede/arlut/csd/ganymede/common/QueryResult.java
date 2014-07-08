/*

   QueryResult.java

   This class is a serializable object-list result object, which
   conveys results from a query/list operation along with methods that
   can be used to extract the results out of the query/list.

   Created: 1 October 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Comparator;
import java.util.Enumeration;
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
 * conveys sorted results from a query/list operation along with
 * methods that can be used to extract the results out of the
 * query/list.</p>
 *
 * <p>The individual elements of a Query Result are labeled {@link
 * arlut.csd.ganymede.common.Invid} references along with a set of
 * status bits.  See {@link
 * arlut.csd.ganymede.common.ObjectHandle}.</p>
 */

public class QueryResult implements java.io.Externalizable {

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

  private Vector<ObjectHandle> handles = null;
  private Set<Invid> invidSet = null;
  private Set<String> labelSet = null;
  private boolean nonEditable = false;

  /* -- */

  public QueryResult()
  {
    this.invidSet = new HashSet<Invid>();
    this.labelSet = new HashSet<String>();
    this.handles = new Vector<ObjectHandle>();
  }

  /**
   * QueryResult taking a single boolean param for backwards
   * compatibility now that we are no longer distinguishing between
   * QueryResults for transport and not.
   */

  @Deprecated public QueryResult(boolean param)
  {
    this();
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
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   */

  public void addRow(Invid invid, String label, boolean editable)
  {
    addRow(invid, label, false, false, false, editable);
  }

  /**
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   */

  public void addRow(ObjectHandle handle)
  {
    addRow(handle.getInvid(), handle.getLabel(), handle.isInactive(),
           handle.isExpirationSet(), handle.isRemovalSet(),
           handle.isEditable());
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
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
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

    if (this.nonEditable)
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

    this.handles.add(new ObjectHandle(label, invid,
                                      inactive, expirationSet,
                                      removalSet, editable));

    if (invid != null)
      {
        this.invidSet.add(invid);
      }

    labelSet.add(label);
  }

  // ***
  //
  // The following methods are intended to be called on a QueryResult
  // after it has been serialized and passed from the server to the
  // client.
  //
  // ***

  public Vector<ObjectHandle> getHandles()
  {
    if (this.nonEditable)
      {
        return new Vector<ObjectHandle>(this.handles);
      }

    return this.handles;
  }

  public Invid getInvid(int row)
  {
    return this.handles.get(row).getInvid();
  }

  public Vector<Invid> getInvids()
  {
    Vector<Invid> invidList = new Vector<Invid>(handles.size());

    for (ObjectHandle handle: this.handles)
      {
        invidList.add(handle.getInvid());
      }

    return invidList;
  }

  public Vector<String> getLabels()
  {
    Vector<String> labelList = new Vector<String>(handles.size());

    for (ObjectHandle handle: this.handles)
      {
        labelList.add(handle.getLabel());
      }

    return labelList;
  }

  public String getLabel(int row)
  {
    return this.handles.get(row).getLabel();
  }

  public int size()
  {
    return this.handles.size();
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

    for (ObjectHandle handle: this.handles)
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
    return this.handles.get(row).getListHandle();
  }

  /**
   * Returns the ObjectHandle for this row.
   */

  public ObjectHandle getObjectHandle(int row)
  {
    return this.handles.get(row);
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
    return this.invidSet.contains(invid);
  }

  /**
   * <p>This method is provided for the server to optimize it's
   * QueryResult loading operations, and is not intended for use
   * post-serialization.</p>
   */

  public synchronized boolean containsLabel(String label)
  {
    return this.labelSet.contains(label);
  }

  /**
   * <p>This is a pre-serialization method for concatenating another
   * (for transport) QueryResult to ourself.</p>
   */

  public void append(QueryResult result)
  {
    if (this.nonEditable)
      {
        throw new RuntimeException("Can't append to a non-editable QueryResult");
      }

    this.handles.addAll(result.getHandles());
    this.invidSet.addAll(result.invidSet);
    this.labelSet.addAll(result.labelSet);
  }

  /**
   * <p>This method returns a QueryResult which holds the intersection
   * of the contents of this QueryResult and the contents of
   * operand.</p>
   */

  public synchronized QueryResult intersection(QueryResult operand)
  {
    if (this.nonEditable)
      {
        throw new RuntimeException("Can't intersect a non-editable QueryResult");
      }

    QueryResult result = new QueryResult();

    /* -- */

    if (operand == null || operand.size() == 0)
      {
        return result;
      }

    for (ObjectHandle handle: this.handles)
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

  public void writeExternal(ObjectOutput out) throws IOException
  {
    out.writeInt(this.handles.size());

    for (ObjectHandle handle: this.handles)
      {
        handle.writeExternal(out);
      }
  }

  public void readExternal(ObjectInput in) throws IOException
  {
    this.handles = new Vector<ObjectHandle>();

    VecSortInsert inserter = new VecSortInsert(comparator);

    int size = in.readInt();

    for (int i = 0; i < size; i++)
      {
        ObjectHandle handle = new ObjectHandle();
        handle.readExternal(in);

        invidSet.add(handle.getInvid());
        labelSet.add(handle.getLabel());
        inserter.insert(handles, handle);
      }
  }
}
