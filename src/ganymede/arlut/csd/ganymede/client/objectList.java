/*

   objectList.java

   This class is used to keep track of a list of objects from
   the server, storing various pieces of information about the
   objects, including their expiration/removal/inactive status,
   their current state in the client, and more.

   Created: 6 February 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2013
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

package arlut.csd.ganymede.client;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.JDataComponent.listHandle;

import arlut.csd.Util.VecQuickSort;
import arlut.csd.Util.VecSortInsert;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.QueryResult;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      objectList

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to keep track of a list of {@link
 * arlut.csd.ganymede.common.ObjectHandle ObjectHandle} objects from
 * the server, storing various pieces of information about the
 * objects, including their expiration/removal/inactive status, their
 * current label, and more.</p>
 *
 * @author Jonathan Abbey
 */

public class objectList {

  final static boolean debug = false;

  static Comparator comparator = new Comparator()
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

  VecSortInsert inserter;
  private Vector<ObjectHandle> handles;
  boolean sorted = false;

  private Vector<ObjectHandle> activeHandles;
  private Hashtable<Invid, ObjectHandle> invids;
  boolean activeSorted = false;
  boolean containsNonEditable = false;

  /* -- */

  public objectList(QueryResult result)
  {
    ObjectHandle handle;

    /* -- */

    invids = new Hashtable<Invid, ObjectHandle>();

    if (result == null)
      {
        handles = new Vector<ObjectHandle>();
      }
    else
      {
        handles = result.getHandles(); // pre-sorted
      }

    sorted = true;

    activeHandles = (Vector<ObjectHandle>) handles.clone(); // quickly dup the vector of handles

    activeSorted = true;

    // now, count down from top of activeHandles vector, removing
    // any inactive objects

    for (int i = activeHandles.size() - 1; i >= 0; i--)
      {
        handle = activeHandles.get(i);
        invids.put(handle.getInvid(), handle);

        if (!handle.isEditable())
          {
            containsNonEditable = true;
          }

        if (handle.isInactive())
          {
            activeHandles.remove(i);

            if (debug)
              {
                System.err.println("objectList constructor: handle " + handle.debugDump() + " is inactive");
              }
          }
        else if (debug)
          {
            System.err.println("objectList constructor: handle " + handle.debugDump() + " is not inactive");
          }
      }

    // create

    inserter = new VecSortInsert(comparator);
  }

  /**
   * This method returns true if this list contains any non-editable
   * handles.
   */

  public boolean containsNonEditable()
  {
    return containsNonEditable;
  }

  /**
   * This method is used to augment an object list with non-editables.
   */

  public synchronized void augmentListWithNonEditables(QueryResult result)
  {
    if (containsNonEditable)
      {
        throw new IllegalArgumentException("already contains non-editables");
      }

    if (result == null)
      {
        return;
      }

    for (ObjectHandle handle: result.getHandles())
      {
        // we only want to add a handle if we don't have the
        // invid in place already

        if (getObjectHandle(handle.getInvid()) == null)
          {
            if (!handle.isEditable())
              {
                containsNonEditable = true;
              }

            addObjectHandle(handle);
          }
      }
  }

  /**
   * <p>This method returns a sorted Vector of listHandles.  The
   * vector is essentially a read-out of the current state of the
   * objectList, and will not track any future changes to this
   * objectList.</p>
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   */

  public Vector<listHandle> getListHandles(boolean includeInactives)
  {
    return getListHandles(includeInactives, false);
  }

  /**
   * <p>This method returns a sorted Vector of listHandles.  The
   * vector is essentially a read-out of the current state of the
   * objectList, and will not track any future changes to this
   * objectList.</p>
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   *
   * @param includeNonEditables if false, the list returned will not
   * include entries for any non-editable objects
   */

  public synchronized Vector<listHandle> getListHandles(boolean includeInactives,
                                                        boolean includeNonEditables)
  {
    Vector<listHandle> results = null;

    /* -- */

    if (includeInactives)
      {
        results = new Vector<listHandle>(handles.size());

        for (ObjectHandle handle: handles)
          {
            if (includeNonEditables || handle.isEditable())
              {
                results.add(handle.getListHandle());
              }
          }
      }
    else
      {
        results = new Vector<listHandle>(activeHandles.size());

        for (ObjectHandle handle: activeHandles)
          {
            if (includeNonEditables || handle.isEditable())
              {
                results.add(handle.getListHandle());
              }
          }
      }

    return results;
  }

  /**
   * <p>This method returns a sorted Vector of object labels.  The
   * vector is essentially a read-out of the current state of the
   * objectList, and will not track any future changes to this
   * objectList.</p>
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   */

  public Vector<String> getLabels(boolean includeInactives)
  {
    return getLabels(includeInactives, false);
  }

  /**
   * <p>This method returns a sorted Vector of object labels.  The
   * vector is essentially a read-out of the current state of the
   * objectList, and will not track any future changes to this
   * objectList.</p>
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   *
   * @param includeNonEditables if false, the list returned will not
   * include entries for any non-editable objects
   */

  public synchronized Vector<String> getLabels(boolean includeInactives,
                                               boolean includeNonEditables)
  {
    Vector<String> results = null;

    /* -- */

    if (includeInactives)
      {
        results = new Vector<String>(handles.size());

        for (ObjectHandle handle: handles)
          {
            if (includeNonEditables || handle.isEditable())
              {
                results.add(handle.getLabel());
              }
          }
      }
    else
      {
        results = new Vector<String>(activeHandles.size());

        for (ObjectHandle handle: activeHandles)
          {
            if (includeNonEditables || handle.isEditable())
              {
                results.add(handle.getLabel());
              }
          }
      }

    return results;
  }

  /**
   * <p>This method returns a sorted copy of an object handles
   * vector.</p>
   *
   * <p>No adds or deletes to the returned vector will be reflected in
   * this objectList, but any changes to the status of the individual
   * ObjectHandle's will be reflected in the objectList.</p>
   */

  public Vector<ObjectHandle> getObjectHandles(boolean includeInactives)
  {
    return getObjectHandles(includeInactives, false);
  }

  /**
   * <p>This method returns a sorted copy of an object handles
   * vector.</p>
   *
   * <p>No adds or deletes to the returned vector will be reflected in
   * this objectList, but any changes to the status of the individual
   * ObjectHandle's will be reflected in the objectList.</p>
   */

  public synchronized Vector<ObjectHandle> getObjectHandles(boolean includeInactives,
                                                            boolean includeNonEditables)
  {
    if (includeNonEditables || !containsNonEditable)
      {
        if (includeInactives)
          {
            return (Vector<ObjectHandle>) handles.clone();
          }
        else
          {
            return (Vector<ObjectHandle>) activeHandles.clone();
          }
      }
    else
      {
        Vector<ObjectHandle> result = new Vector<ObjectHandle>();

        if (includeInactives)
          {
            for (ObjectHandle handle: handles)
              {
                if (handle.isEditable())
                  {
                    result.add(handle);
                  }
              }
          }
        else
          {
            for (ObjectHandle handle: activeHandles)
              {
                if (handle.isEditable())
                  {
                    result.add(handle);
                  }
              }
          }

        return result;
      }
  }

  /**
   * <p>This method relabels an object handle in this list.</p>
   *
   * <p>Use this method rather than changing an object handle
   * reference got by getObjectHandle() in order to let objectList
   * maintain sort order.</p>
   */

  public synchronized void relabelObject(Invid invid, String newLabel)
  {
    ObjectHandle handle = removeInvid(invid);

    if (handle == null)
      {
        return;                 // throw exception?
      }

    handle.setLabel(newLabel);

    addObjectHandle(handle);
  }

  /**
   * <p>This method adds an object handle to this list, quickly, in
   * sorted order.</p>
   */

  public synchronized void addObjectHandle(ObjectHandle handle)
  {
    inserter.insert(handles, handle);

    if (!handle.isInactive())
      {
        inserter.insert(activeHandles, handle);
      }
  }

  /**
   * <p>This method returns a live reference to the object handle
   * corresponding to invid, or null if none such is in this object
   * list.</p>
   */

  public ObjectHandle getObjectHandle(Invid invid)
  {
    return invids.get(invid);
  }

  /**
   * <p>This method removes object handles matching the given invid
   * from the object list.</p>
   *
   * <p>This isn't the fastest operation, but hopefully won't be too
   * bad.</p>
   *
   * @return The handle removed, or null if it wasn't found.
   */

  public synchronized ObjectHandle removeInvid(Invid invid)
  {
    ObjectHandle handle = invids.remove(invid);

    if (handle == null)
      {
        return null;
      }

    handles.remove(handle);
    activeHandles.remove(handle);

    return handle;
  }

  public synchronized String toString()
  {
    StringBuilder tempBuf = new StringBuilder();

    for (ObjectHandle handle: handles)
      {
        tempBuf.append(handle);
        tempBuf.append("\n");
      }

    return tempBuf.toString();
  }

  // ***
  //
  // Private methods
  //
  // ***

  private void sortHandles()
  {
    if (!sorted)
      {
        (new VecQuickSort(handles, comparator)).sort();
        sorted = true;
      }

    if (!activeSorted)
      {
        (new VecQuickSort(activeHandles, comparator)).sort();
        activeSorted = true;
      }
  }
}
