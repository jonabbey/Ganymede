/*

   objectCache.java

   This class implements an information cache for the client.  Client
   code can store information about objects on the server here and
   can use it wherever.
   
   Created: 7 February 1998
   Version: $Revision: 1.1 $ %D%
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
    objectList list = getList(key);

    if (list == null)
      {
	return null;
      }

    return list.getListHandles(includeInactives);
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
    objectList list = getList(key);

    if (list == null)
      {
	return null;
      }

    return list.getLabels(includeInactives);
  }

  public void putList(Object key, QueryResult qr)
  {
    idMap.put(key, new objectList(qr));
  }

  public void putList(Object key, objectList list)
  {
    idMap.put(key, list);
  }

  public void clearCaches()
  {
    idMap.clear();
  }

}
