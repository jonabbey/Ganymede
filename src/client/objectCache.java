/*

   objectCache.java

   This class implements an information cache for the client.  Client
   code can store information about objects on the server here and
   can use it wherever.
   
   Created: 7 February 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.util.*;		// vector, hashtable

import arlut.csd.ganymede.ObjectHandle;
import arlut.csd.ganymede.QueryResult;
import arlut.csd.ganymede.Invid;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     objectCache

------------------------------------------------------------------------------*/

public class objectCache {

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
   * This method returns a sorted Vector of listHandles for the cache
   * for <key>.  The vector is essentially a read-out of the current
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
   * for <key>.  The vector is essentially a read-out of the current
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
   *
   * This method returns a sorted Vector of object labels.  The vector
   * is essentially a read-out of the current state of the objectList,
   * and will not track any future changes to this objectList.
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   *
   */

  public Vector getLabels(Object key, boolean includeInactives)
  {
    return getLabels(key, includeInactives, false);
  }

  /**
   *
   * This method returns a sorted Vector of object labels.  The vector
   * is essentially a read-out of the current state of the objectList,
   * and will not track any future changes to this objectList.
   *
   * @param includeInactives if false, the list returned will not
   * include entries for any inactive objects
   * 
   * @param includeNonEditables if false, the list returned will not
   * include entries for any non-editable objects
   *
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
   *
   * This method retrieves an object handle matching the given
   * invid from the specified object list.
   *
   * This isn't the fastest operation, but hopefully won't
   * be too bad.
   *
   * @return The matching handle, or null if it wasn't found.
   *
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
    idMap.put(key, new objectList(qr));
  }

  /**
   *
   * This method is intended to augment an existing list with
   * non-editable object handles.
   *
   */

  public void augmentList(Object key, QueryResult qr)
  {
    objectList list = (objectList) idMap.get(key);

    if (key == null)
      {
	throw new RuntimeException("error, no list found with key " + key);
      }

    list.augmentListWithNonEditables(qr);
  }

  public void putList(Object key, objectList list)
  {
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
