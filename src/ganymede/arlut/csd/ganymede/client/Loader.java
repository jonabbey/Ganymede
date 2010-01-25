/*

   Loader.java

   A convenient initialization thread, does start up stuff for
   the client.
   
   Created: 1 October 1997

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.VecQuickSort;
import arlut.csd.ganymede.common.BaseDump;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.Session;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Loader

------------------------------------------------------------------------------*/

/**
 * Client-side cache for loading object and field type definitions
 * from the server in the background during the client's start-up, and
 * providing that information to the client during its operations.
 *
 * @version $Id$
 * @author Mike Mulvaney
 */

public class Loader extends Thread {

  private static final boolean debug = false;

  private Hashtable 
    baseMap,
    baseNames,
    baseToShort,
    nameShorts;

  private Vector
    baseList;

  private volatile boolean
    isShutdown = false,
    baseNamesLoaded = false,
    baseListLoaded = false,
    baseMapLoaded = false,
    templateLoading = false;

  private Session
    session;

  /**
   * Hash mapping Short object type id's to Vectors of
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s,
   * used by the client to quickly look up information about fields 
   * in order to populate 
   * {@link arlut.csd.ganymede.client.containerPanel containerPanel}'s.
   *
   * This hash is used by
   * {@link arlut.csd.ganymede.client.gclient#getTemplateVector(java.lang.Short) getTemplateVector}.
   */

  private Hashtable templateHash;

  /**
   * Hash mapping Short object type id's to a Hash that
   * maps field name to {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * object
   */

  private Hashtable templateNameHash;

  /* -- */

  public Loader(Session session, boolean debug)
  {
    if (debug)
      {
	System.err.println("Initializing Loader");
      }

    /*    this.debug = debug; */

    this.session = session;

    if (session == null)
      {
	throw new NullPointerException("Null session parameter in Loader constructor");
      }
  }

  public void run()
  {
    if (debug)
      {
	System.err.println("Starting thread in loader");
      }

    try
      {
        if (isShutdown)
          {
            if (debug)
              {
                System.err.println("**Stopping before baseList is loaded");
              }

            // Ok, it's not really loaded, but this basically means that it is finished.

            baseListLoaded = true;
          }

        loadBaseList();

        if (isShutdown)
          {
            if (debug)
              {
                System.err.println("**Stopping before baseNames are loaded");
              }
            
            baseNamesLoaded = true;
          }

        loadBaseNames();

        if (isShutdown)
          {
            if (debug)
              {
                System.err.println("**Stopping before baseMap is loaded");
              }
		
            baseMapLoaded = true;
          }

        loadBaseMap();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load base hash/map in Loader: " + rx);
      }

    if (debug)
      {
	System.err.println("Done with thread in loader.");
      }
  }

  /**
   *
   * Clear out all the information in the loader, and spawn
   * a new loader thread to download new information from
   * the server.
   *
   */

  public void clear()
  {
    cleanUp();

    if (debug)
      {
	System.err.println("Starting to load the loader again");
      }

    isShutdown = false;

    // start up a new thread

    Thread t = new Thread(this);
    t.setPriority(Thread.NORM_PRIORITY);
    t.start();
  }

  /**
   * Clear out all information in the loader
   */

  public void cleanUp()
  {
    if (debug)
      {
	System.err.println("** Loader cleanUp()");
      }

    isShutdown = true;

    synchronized (this) 
      {
	// setting isShutdown to true should cause the loader run
	// method to quickly drop out and set the loader flags to
	// true, so we wait until all of the boolean loaded flags are
	// set

	while (!(baseNamesLoaded && baseListLoaded && baseMapLoaded && !templateLoading))
	  {
	    if (debug)
	      {
		System.err.println("Loader waiting for previous method to stop.");
	      }

	    try
	      {
		this.wait();
	      }
	    catch (InterruptedException x)
	      {
		throw new RuntimeException("Interrupted while waiting for previous loader to finish. " + x);
	      }
	  }
      }

    baseNamesLoaded = false;
    baseListLoaded = false;
    baseMapLoaded = false;

    if (baseList != null)
      {
	baseList.setSize(0);
	baseList = null;
      }

    // many of these hashes have values that are themselves hashes,
    // but we won't worry about things enough to iterate over the
    // interior hashes and clear them out.  GC will do that for us
    // anyway, and it would take us some effort to do it affirmatively

    if (baseNames != null)
      {
	baseNames.clear();
	baseNames = null;
      }

    if (baseMap != null)
      {
	baseMap.clear();
	baseMap = null;
      }

    if (templateHash != null)
      {
	templateHash.clear();
	templateHash = null;
      }

    if (templateNameHash != null)
      {
	templateNameHash.clear();
	templateNameHash = null;
      }
  }

  /**
   * Returns the type name for a given object.
   *
   * If the loader thread hasn't yet downloaded that information, this
   * method will block until the information is available.
   */

  public String getObjectType(Invid objId)
  {
    return this.getObjectType(objId.getType());
  }

  /**
   * Returns the type name for a given object type number.
   *
   * If the loader thread hasn't yet downloaded that information, this
   * method will block until the information is available.
   */

  public String getObjectType(short typeId)
  {
    try
      {
	Hashtable baseMap = getBaseMap(); // block
	BaseDump base = (BaseDump) baseMap.get(Short.valueOf(typeId));

	return base.getName();
      }
    catch (NullPointerException ex)
      {
	return "<unknown>";
      }
  }

  /**
   * Returns a Vector of {@link arlut.csd.ganymede.common.BaseDump BaseDump} objects,
   * providing a local cache of {@link arlut.csd.ganymede.rmi.Base Base}
   * references that the client consults during operations.
   *
   * If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.
   */

  public Vector getBaseList()
  {
    if (!baseListLoaded && !isShutdown)
      {
	synchronized (this)
	  {
	    while (!baseListLoaded && !isShutdown)
	      {
		if (debug)
		  {
		    System.err.println("Dang, have to wait to get the base list");
		  }
		
		try
		  {
		    this.wait();
		  }
		catch (InterruptedException x)
		  {
		    throw new RuntimeException("Interrupted while waiting for base list to load: " + x);
		  }
	      }
	  }
      }

    if (debug)
      {
	if (baseList == null)
	  {
	    System.err.println("baseList is null");
	  }
	else
	  {
	    System.err.println("returning baseList");
	  }
      }

    return baseList;
  }

  /**
   * Returns a hash mapping {@link arlut.csd.ganymede.common.BaseDump BaseDump}
   * references to their title.
   *
   * If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.
   */

  public Hashtable getBaseNames()
  {
    if (!baseNamesLoaded && !isShutdown)
      {
	synchronized (this)
	  {
	    // we have to check baseNamesLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseNamesLoaded && !isShutdown)
	      {
		if (debug)
		  {
		    System.err.println("Dang, have to wait to get the base names list");
		  }

		try
		  {
		    this.wait();
		  }
		catch (InterruptedException x)
		  {
		    throw new RuntimeException("Interrupted while waiting for base names to load: " + x);
		  }
	      }
	  }
      }
    
    if (debug)
      {
	if (baseNames == null)
	  {
	    System.err.println("baseNames is null");
	  }
	else
	  {
	    System.err.println("returning baseNames");
	  }
      }

    return baseNames;
  }

  /**
   * Returns a hash mapping Short {@link arlut.csd.ganymede.rmi.Base Base} id's to
   * {@link arlut.csd.ganymede.common.BaseDump BaseDump} objects.
   *
   * If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.
   */

  public Hashtable getBaseMap()
  {
    if (!baseMapLoaded && !isShutdown)
      {
	synchronized (this)
	  {
	    // we have to check baseMapLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseMapLoaded && !isShutdown)
	      {
		if (debug)
		  {
		    System.err.println("Loader: waiting for base map");
		  }

		try
		  {
		    this.wait();
		  }
		catch (InterruptedException x)
		  {
		    throw new RuntimeException("Interrupted while waiting for base hash to load: " + x);
		  }
	      }
	  }
      }

    if (debug)
      {
	if (baseMap == null)
	  {
	    System.err.println("baseMap is null");
	  }
	else
	  {
	    System.err.println("returning baseMap");
	  }
      }

    return baseMap;
  }

  /**
   * Returns a hashtable mapping {@link arlut.csd.ganymede.common.BaseDump BaseDump}
   * references to their object type id in Short form.  This is
   * a holdover from a time when the client didn't create local copies
   * of the server's Base references.
   *
   * If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.
   */

  public Hashtable getBaseToShort()
  {
    // baseToShort is loaded in the loadBaseMap function, so we can just
    // check to see if the baseMapLoaded is true.

    if (!baseMapLoaded && !isShutdown)
      {
	synchronized (this)
	  {
	    // we have to check baseMapLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseMapLoaded && !isShutdown)
	      {
		if (debug)
		  {
		    System.err.println("Loader: waiting for base hash");
		  }

		try
		  {
		    this.wait();
		  }
		catch (InterruptedException x)
		  {
		    throw new RuntimeException("Interrupted while waiting for base hash to load: " + x);
		  }
	      }
	  }
      }

    if (debug)
      {
	if (baseToShort == null)
	  {
	    System.err.println("baseToShort is null");
	  }
	else
	  {
	    System.err.println("returning baseToShort");
	  }
      }

    return baseToShort;
  }

  /**
   * Returns a hashtable mapping base names to their object type id
   * in Short form.  This is used by the XML client to quickly map
   * object type names to the numeric type id.
   *
   * If this thread hasn't yet downloaded that information, this method will
   * block until the information is available. 
   */

  public Hashtable getNameToShort()
  {
    // baseToShort is loaded in the loadBaseMap function, so we can just
    // check to see if the baseMapLoaded is true.

    if (!baseMapLoaded && !isShutdown)
      {
	synchronized (this)
	  {
	    // we have to check baseMapLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseMapLoaded && !isShutdown)
	      {
		if (debug)
		  {
		    System.err.println("Loader: waiting for base hash");
		  }

		try
		  {
		    this.wait();
		  }
		catch (InterruptedException x)
		  {
		    throw new RuntimeException("Interrupted while waiting for base hash to load: " + x);
		  }
	      }
	  }
      }

    if (false)
      {
	if (nameShorts == null)
	  {
	    System.err.println("nameShorts is null");
	  }
	else
	  {
	    System.err.println("returning nameShorts");
	  }
      }

    return nameShorts;
  }

  /**
   * Returns a {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * for a field specified by object type id and field name.
   */

  public FieldTemplate getFieldTemplate(short objectid, String fieldname)
  {
    return getFieldTemplate(Short.valueOf(objectid), fieldname);
  }

  /**
   * Returns a {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * for a field specified by object type id and field name.
   */

  public FieldTemplate getFieldTemplate(Short objectid, String fieldname)
  {
    if (templateNameHash == null)
      {
	templateNameHash = new Hashtable();
      }

    Hashtable nameHash = (Hashtable) templateNameHash.get(objectid);

    if (nameHash == null)
      {
	getTemplateVector(objectid);

	nameHash = (Hashtable) templateNameHash.get(objectid);

	if (nameHash == null)
	  {
	    return null;
	  }
      }

    return (FieldTemplate) nameHash.get(fieldname);
  }

  /**
   * Returns a vector of 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s.
   *
   * @param id Object type id to retrieve field information for.
   */

  public Vector getTemplateVector(short id)
  {
    return getTemplateVector(Short.valueOf(id));
  }

  /**
   * Returns a vector of 
   * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}'s
   * listing fields and field informaton for the object type identified by 
   * id.
   *
   * @param id The id number of the object type to be returned the base id.
   */

  public synchronized Vector getTemplateVector(Short id)
  {
    Vector result = null;

    /* -- */

    if (isShutdown)
      {
	if (debug)
	  {
	    System.err.println("Loader.getTemplateVector() -- isShutdown is true");
	  }

	return null;
      }

    try
      {
	templateLoading = true;

	if (templateHash == null)
	  {
	    templateHash = new Hashtable();
	  }
	
	if (templateHash.containsKey(id))
	  {
	    result = (Vector) templateHash.get(id);
	  }
	else
	  {
	    try
	      {
		result = session.getFieldTemplateVector(id.shortValue());
		templateHash.put(id, result);
		constructTemplateNameHash(id, result);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get field templates: " + rx);
	      }
	  }
	
	return result;
      }
    finally
      {
	templateLoading = false;
      }
  }

  /* -- Private methods  --  */

  private void constructTemplateNameHash(Short objectId, Vector fieldTemplates)
  {
    Hashtable nameHash = new Hashtable();

    for (int i = 0; i < fieldTemplates.size(); i++)
      {
	FieldTemplate x = (FieldTemplate) fieldTemplates.elementAt(i);
	nameHash.put(x.getName(), x);
      }

    if (templateNameHash == null)
      {
	templateNameHash = new Hashtable();
      }

    templateNameHash.put(objectId, nameHash);
  }

  /** 
   * loadBaseList loads a sorted Vector of types from the server.
   */

  private synchronized void loadBaseList() throws RemoteException
  {
    baseList = session.getBaseList().getBaseList();

    if (debug)
      {
	System.err.println("Finished loading base list");

	if (baseList == null)
	  {
	    System.err.println("****** BaseList is null after loading!!!! *****");
	  }
	else
	  {
	    System.err.println("*** BaseList is not null.");
	  }
      }

    (new VecQuickSort(baseList, null)).sort();

    baseListLoaded = true;
    notifyAll();
  }

  /**
   * loadBaseNames constructs a hashtable mapping Base references
   * to base names.  This is intended to serve as a local cache
   * to avoid having to do round-trip calls to the server just
   * to get a Base reference's name.
   */

  private synchronized void loadBaseNames() throws RemoteException
  {
    baseNames = new Hashtable();
    
    Base b;
    Vector list = getBaseList();

    for (int i = 0; i < list.size(); i++)
      {
	b = (Base)list.elementAt(i);
	baseNames.put(b, b.getName());
      }
      
    if (debug)
      {
	System.err.println("Finished loading base list");
      }

    baseNamesLoaded = true;
    notifyAll();
  }

  /**
   * loadBaseMap() generates baseMap, a mapping of Short's to
   * the corresponding remote base reference.
   */

  private synchronized void loadBaseMap() throws RemoteException
  {
    Base base;
    int size;
    Vector myBaseList;

    /* -- */

    myBaseList = getBaseList();
    size = myBaseList.size();

    baseMap = new Hashtable(size);
    baseToShort = new Hashtable(size);
    nameShorts = new Hashtable(size);

    for (int i = 0; i < size; i++)
      {
	base = (Base) myBaseList.elementAt(i);
	Short id = Short.valueOf(base.getTypeID());

	baseMap.put(id, base);
	baseToShort.put(base, id);
	nameShorts.put(base.getName(), id);
      }

    baseMapLoaded = true;
    notifyAll();
  }
}
