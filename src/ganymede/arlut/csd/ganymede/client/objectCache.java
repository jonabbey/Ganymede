/*

   objectCache.java

   This class implements an information cache for the client.  Client
   code can store information about objects on the server here and
   can use it wherever.
   
   Created: 7 February 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2008
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.QueryResult;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     objectCache

------------------------------------------------------------------------------*/

/**
 * Implements an information cache for the client.  Client
 * code can store information about objects on the server here and
 * can use it wherever.
 *
 * objectCache maintains a mapping between hash keys (typically
 * Short values corresponding to object type ids on the server) and
 * {@link arlut.csd.ganymede.client.objectList objectList} objects
 * which track status of objects for that hash key.
 */

public class objectCache {

  static final boolean debug = false;
  Hashtable idMap = new Hashtable();

  /* -- */

  public objectCache()
  {
  }

  public boolean containsList(Object key)
  {
    return idMap.containsKey(key);
  }

  public objectList getList(Object key)
  {
    return (objectList) idMap.get(key);
  }

  /**
   * This method returns true if the specified list contains any
   * non-editable handles.
   */

  public boolean containsNonEditable(Object key)
  {
    objectList list = getList(key);

    if (list == null)
      {
        throw new IllegalArgumentException("no such list in cache: " + key);
      }

    return list.containsNonEditable();
  }

  /**
   * This method returns a sorted Vector of listHandles for the cache
   * for &lt;key&gt;.  The vector is essentially a read-out of the current
   * state of the objectList, and will not track any future changes to
   * this objectList.
   *
   * @param includeInactives if false, the list returned will not include entries
   * for any inactive objects 
   * 
   */

  public Vector getListHandles(Object key, boolean includeInactives)
  {
    return getListHandles(key, includeInactives, false);
  }

  /**
   * This method returns a sorted Vector of listHandles for the cache
   * for &lt;key&gt;.  The vector is essentially a read-out of the current
   * state of the objectList, and will not track any future changes to
   * this objectList.
   *
   * @param includeInactives if false, the list returned will not include entries
   * for any inactive objects 
   *
   * @param includeNonEditables if false, the list returned will not
   * include entries for any non-editable objects
   * 
   */

  public Vector getListHandles(Object key, boolean includeInactives,
                               boolean includeNonEditables)
  {
    objectList list = getList(key);

    if (list == null)
      {
        return null;
      }

    return list.getListHandles(includeInactives, includeNonEditables);
  }

  /**
   * This method returns a sorted Vector of object labels.  The vector
   * is essentially a read-out of the current state of the objectList,
   * and will not track any future changes to this objectList.
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   */

  public Vector getLabels(Object key, boolean includeInactives)
  {
    return getLabels(key, includeInactives, false);
  }

  /**
   * This method returns a sorted Vector of object labels.  The vector
   * is essentially a read-out of the current state of the objectList,
   * and will not track any future changes to this objectList.
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   * 
   * @param includeNonEditables if false, the list returned will not
   * include entries for any non-editable objects
   */

  public Vector getLabels(Object key, boolean includeInactives,
                          boolean includeNonEditables)
  {
    objectList list = getList(key);

    if (list == null)
      {
        return null;
      }

    return list.getLabels(includeInactives, includeNonEditables);
  }

  /**
   * This method retrieves an object handle matching the given invid
   * from the specified object list.
   *
   * This isn't the fastest operation, but hopefully won't be too bad.
   *
   * @return The matching handle, or null if it wasn't found.
   */

  public ObjectHandle getInvidHandle(Object key, Invid invid)
  {
    objectList list = getList(key);

    if (list == null)
      {
        return null;
      }

    return list.getObjectHandle(invid);
  }

  public void putList(Object key, QueryResult qr)
  {
    if (debug)
      {
        System.err.println("objectCache: caching key " + key);
      }

    idMap.put(key, new objectList(qr));
  }

  /**
   * This method is intended to augment an existing list with
   * non-editable object handles.
   */

  public void augmentList(Object key, QueryResult qr)
  {
    objectList list = (objectList) idMap.get(key);

    if (list == null)
      {
        throw new RuntimeException("error, no list found with key " + key);
      }

    if (debug)
      {
        System.err.println("objectCache: augmenting key " + key);
      }

    list.augmentListWithNonEditables(qr);
  }

  public void putList(Object key, objectList list)
  {
    if (debug)
      {
        System.err.println("objectCache: caching key " + key + " (2)");
      }

    idMap.put(key, list);
  }

  public void removeList(Object key)
  {
    idMap.remove(key);
  }

  public void clearCaches()
  {
    idMap.clear();
  }
}
