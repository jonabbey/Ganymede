/*

   objectList.java

   This class is used to keep track of a list of objects from
   the server, storing various pieces of information about the
   objects, including their expiration/removal/inactive status,
   their current state in the client, and more.
   
   Created: 6 February 1998
   Release: $Name:  $
   Version: $Revision: 1.10 $
   Last Mod Date: $Date: 1999/01/22 18:04:17 $
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

package arlut.csd.ganymede.client;

import java.util.*;		// vector, hashtable

import arlut.csd.ganymede.ObjectHandle;
import arlut.csd.ganymede.QueryResult;
import arlut.csd.ganymede.Invid;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      objectList

------------------------------------------------------------------------------*/

/**
 * This class is used to keep track of a list of objects from
 * the server, storing various pieces of information about the
 * objects, including their expiration/removal/inactive status,
 * their current state in the client, and more.
 *
 */

public class objectList {

  final static boolean debug = false;

  static Compare comparator = new arlut.csd.Util.Compare() 
    {
      public int compare(Object o_a, Object o_b) 
	{
	  ObjectHandle a, b;
	  
	  a = (ObjectHandle) o_a;
	  b = (ObjectHandle) o_b;
	  int comp = 0;
	  
	  comp = a.getLabel().compareTo(b.getLabel());
	  
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
  private Vector handles;
  boolean sorted = false;

  private Vector activeHandles;
  private Hashtable invids;
  boolean activeSorted = false;
  boolean containsNonEditable = false;

  /* -- */

  public objectList(QueryResult result)
  {
    ObjectHandle handle;

    /* -- */

    invids = new Hashtable();
    handles = result.getHandles();

    activeHandles = (Vector) handles.clone(); // quickly dup the vector of handles

    // now, count down from top of activeHandles vector, removing
    // any inactive objects

    for (int i = activeHandles.size() - 1; i >= 0; i--)
      {
	handle = (ObjectHandle) activeHandles.elementAt(i);
	invids.put(handle.getInvid(), handle);

	if (!handle.isEditable())
	  {
	    containsNonEditable = true;
	  }

	if (handle.isInactive())
	  {
	    activeHandles.removeElementAt(i);

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
   *
   * This method returns true if this list contains any non-editable
   * handles.
   *
   */

  public boolean containsNonEditable()
  {
    return containsNonEditable;
  }

  /**
   *
   * This method is used to augment an object list with non-editables.
   *
   *
   */

  public synchronized void augmentListWithNonEditables(QueryResult result)
  {
    Enumeration enum;
    Vector localhandles;
    ObjectHandle handle;

    /* -- */

    if (containsNonEditable)
      {
	throw new IllegalArgumentException("already contains non-editables");
      }

    localhandles = result.getHandles();

    enum = localhandles.elements();

    while (enum.hasMoreElements())
      {
	handle = (ObjectHandle) enum.nextElement();

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
   * This method returns a sorted Vector of listHandles.  The vector
   * is essentially a read-out of the current state of the objectList,
   * and will not track any future changes to this objectList.
   *
   * @param includeInactives if false, the list returned will not include entries
   * for any inactive objects 
   */
  
  public Vector getListHandles(boolean includeInactives)
  {
    return getListHandles(includeInactives, false);
  }

  /**
   * This method returns a sorted Vector of listHandles.  The vector
   * is essentially a read-out of the current state of the objectList,
   * and will not track any future changes to this objectList.
   *
   * @param includeInactives if false, the list returned will not include entries
   * for any inactive objects 
   *
   * @param includeNonEditables if false, the list returned will not
   * include entries for any non-editable objects
   *
   */
  
  public synchronized Vector getListHandles(boolean includeInactives,
					    boolean includeNonEditables)
  {
    ObjectHandle handle;
    Vector results;

    /* -- */

    sortHandles();

    if (includeInactives)
      {
	results = new Vector(handles.size());

	for (int i = 0; i < handles.size(); i++)
	  {
	    handle = (ObjectHandle) handles.elementAt(i);

	    if (includeNonEditables || handle.isEditable())
	      {
		results.addElement(handle.getListHandle());
	      }
	  }
      }
    else
      {
	results = new Vector(activeHandles.size());

	for (int i = 0; i < activeHandles.size(); i++)
	  {
	    handle = (ObjectHandle) activeHandles.elementAt(i);

	    if (includeNonEditables || handle.isEditable())
	      {
		results.addElement(handle.getListHandle());
	      }
	  }
      }

    return results;
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

  public Vector getLabels(boolean includeInactives)
  {
    return getLabels(includeInactives, false);
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

  public synchronized Vector getLabels(boolean includeInactives,
				       boolean includeNonEditables)
  {
    ObjectHandle handle;
    Vector results;

    /* -- */

    sortHandles();

    if (includeInactives)
      {
	results = new Vector(handles.size());

	for (int i = 0; i < handles.size(); i++)
	  {
	    handle = (ObjectHandle) handles.elementAt(i);

	    if (includeNonEditables || handle.isEditable())
	      {
		results.addElement(handle.getLabel());
	      }
	  }
      }
    else
      {
	results = new Vector(activeHandles.size());

	for (int i = 0; i < handles.size(); i++)
	  {
	    handle = (ObjectHandle) activeHandles.elementAt(i);

	    if (includeNonEditables || handle.isEditable())
	      {
		results.addElement(handle.getLabel());
	      }
	  }
      }

    return results;
  }

  /**
   *
   * This method returns a sorted copy of an object handles vector.
   *
   * No adds or deletes to the returned vector will be reflected
   * in this objectList, but any changes to the status of the individual
   * ObjectHandle's will be reflected in the objectList.
   *
   */

  public Vector getObjectHandles(boolean includeInactives)
  {
    return getObjectHandles(includeInactives, false);
  }

  /**
   *
   * This method returns a sorted copy of an object handles vector.
   *
   * No adds or deletes to the returned vector will be reflected
   * in this objectList, but any changes to the status of the individual
   * ObjectHandle's will be reflected in the objectList.
   *
   */

  public synchronized Vector getObjectHandles(boolean includeInactives,
					      boolean includeNonEditables)
  {
    sortHandles();

    if (includeNonEditables || !containsNonEditable)
      {
	if (includeInactives)
	  {
	    return (Vector) handles.clone();
	  }
	else
	  {
	    return (Vector) activeHandles.clone();
	  }
      }
    else
      {
	ObjectHandle handle;
	Vector result = new Vector();

	/* -- */

	if (includeInactives)
	  {
	    for (int i = 0; i < handles.size(); i++)
	      {
		handle = (ObjectHandle) handles.elementAt(i);

		if (handle.isEditable())
		  {
		    result.addElement(handle);
		  }
	      }
	  }
	else
	  {
	    for (int i = 0; i < activeHandles.size(); i++)
	      {
		handle = (ObjectHandle) activeHandles.elementAt(i);

		if (handle.isEditable())
		  {
		    result.addElement(handle);
		  }
	      }
	  }

	return result;
      }
  }

  /**
   *
   * This method relabels an object handle in this list.
   *
   * Use this method rather than changing an object
   * handle reference got by getObjectHandle() in
   * order to let objectList maintain sort order.
   *
   */

  public synchronized void relabelObject(Invid invid, String newLabel)
  {
    ObjectHandle handle = removeInvid(invid);

    if (handle == null)
      {
	return;			// throw exception?
      }

    handle.setLabel(newLabel);

    addObjectHandle(handle);
  }

  /**
   *
   * This method adds an object handle to this list, quickly,
   * in sorted order.
   *
   */

  public synchronized void addObjectHandle(ObjectHandle handle)
  {
    sortHandles();

    if (inserter == null)
      {
	System.out.println("Inserter is null!");
      }

    inserter.insert(handles, handle);

    if (!handle.isInactive())
      {
	inserter.insert(activeHandles, handle);
      }
  }

  /**
   *
   * This method returns a live reference to the object handle
   * corresponding to invid, or null if none such is in this
   * object list.
   *
   */

  public ObjectHandle getObjectHandle(Invid invid)
  {
    return (ObjectHandle) invids.get(invid);
  }

  /**
   *
   * This method removes object handles matching the given
   * invid from the object list.
   *
   * This isn't the fastest operation, but hopefully won't
   * be too bad.
   *
   * @return The handle removed, or null if it wasn't found.
   *
   */

  public synchronized ObjectHandle removeInvid(Invid invid)
  {
    ObjectHandle handle;

    /* -- */

    handle = (ObjectHandle) invids.remove(invid);

    if (handle == null)
      {
	return null;
      }

    handles.removeElement(handle);
    activeHandles.removeElement(handle);

    return handle;
  }

  public synchronized String toString()
  {
    StringBuffer tempBuf = new StringBuffer();

    for (int i = 0; handles != null && i < handles.size(); i++)
      {
	tempBuf.append(handles.elementAt(i));
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
