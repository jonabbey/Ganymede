/*

   Loader.java

   A convenient initialization thread, does start up stuff for
   the client.
   
   Created: 1 October 1997
   Release: $Name:  $
   Version: $Revision: 1.21 $
   Last Mod Date: $Date: 2000/05/30 05:53:36 $
   Module By: Michael Mulvaney

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

import java.util.*;
import java.rmi.*;

import arlut.csd.ganymede.*;
import arlut.csd.Util.VecQuickSort;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Loader

------------------------------------------------------------------------------*/

/**
 * Client-side thread class for loading object and field type definitions from
 * the server in the background during the client's start-up.
 *
 * @version $Revision: 1.21 $ $Date: 2000/05/30 05:53:36 $ $Name:  $
 * @author Mike Mulvaney
 */

public class Loader extends Thread {

  private boolean debug = false;

  private Hashtable 
    baseMap,
    baseNames,
    baseHash,
    baseToShort,
    nameShorts;

  private Vector
    baseList;
 
  private boolean
    keepGoing = true,
    baseNamesLoaded = false,
    baseListLoaded = false,
    baseMapLoaded = false,
    templateLoading = false,
    baseHashLoaded = false;

  private Session
    session;

  /**
   * <p>Hash mapping Short object type id's to Vectors of
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}'s,
   * used by the client to quickly look up information about fields 
   * in order to populate 
   * {@link arlut.csd.ganymede.client.containerPanel containerPanel}'s.</p>
   *
   * <p>This hash is used by
   * {@link arlut.csd.ganymede.client.gclient#getTemplateVector(java.lang.Short) getTemplateVector}.</p>
   */

  private Hashtable templateHash;

  /**
   * <p>Hash mapping Short object type id's to a Hash that
   * maps field name to {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * object</p>
   */

  private Hashtable templateNameHash;

  /* -- */

  public Loader(Session session, boolean debug)
  {
    if (debug)
      {
	System.out.println("Initializing Loader");
      }

    this.debug = debug;

    this.session = session;

    if (session == null)
      {
	throw new NullPointerException("Null session paramater in Loader constructor");
      }
  }

  public synchronized void run()
  {
    if (debug)
      {
	System.out.println("Starting thread in loader");
      }

    try
      {
	if (keepGoing)
	  {
	    loadBaseList();
	  }
	else
	  {
	    System.out.println("**Stopping before baseList is loaded");

	    // Ok, it's not really loaded, but this basically means that it is finished.

	    baseListLoaded = true;
	    notifyAll();
	  }

	if (keepGoing)
	  {
	    loadBaseNames();
	  }
	else
	  {
	    System.out.println("**Stopping before baseNames are loaded");

	    baseNamesLoaded = true;
	    this.notifyAll();
	  }

	if (keepGoing)
	  {
	    loadBaseMap();
	  }
	else
	  {
	    System.out.println("**Stopping before baseMap is loaded");

	    baseMapLoaded = true;
	    this.notifyAll();
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load base hash/map in Loader: " + rx);
      }

    if (debug)
      {
	System.out.println("Done with thread in loader.");
      }

    this.notifyAll();
  }

  /**
   *
   * Clear out all the information in the loader.
   *
   */

  public void clear()
  {
    // First stop loading stuff.

    keepGoing = false;
    
    if (debug)
      {
	if (!(baseNamesLoaded && baseListLoaded && baseMapLoaded))
	  {
	    System.out.println("***There are not all finished.");
	  }
	else
	  {
	    System.out.println("***All the hashes are clear.");
	  }
      }

    synchronized (this) 
      {
	// setting keepGoing to false should cause the loader run
	// method to quickly drop out and set the loader flags to
	// true, so we wait until all of the boolean loaded flags are
	// set

	while (!(baseNamesLoaded && baseListLoaded && baseMapLoaded && !templateLoading))
	  {
	    System.out.println("Loader waiting for previous method to stop.");

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

    if (debug)
      {
	System.out.println("Clearing the loader");
      }

    // Set everything to false, so it will reload them all.

    baseNamesLoaded = false;
    baseListLoaded = false;
    baseMapLoaded = false;
    
    baseList = null;
    baseNames = null;
    baseMap = null;
    baseList = null;
    templateHash = null;
    templateNameHash = null;

    if (debug)
      {
	System.out.println("Starting to load the loader again");
      }

    keepGoing = true;

    // start up a new thread

    Thread t = new Thread(this);
    t.start();
  }

  /**
   * <p>Returns the type name for a given object.</p>
   *
   * <p>If the loader thread hasn't yet downloaded that information, this
   * method will block until the information is available.</p>
   */

  public String getObjectType(Invid objId)
  {
    try
      {
	Hashtable baseMap = getBaseMap(); // block
	BaseDump base = (BaseDump) baseMap.get(new Short(objId.getType()));

	return base.getName();
      }
    catch (NullPointerException ex)
      {
	return "<unknown>";
      }
  }

  /**
   * <p>Returns a Vector of {@link arlut.csd.ganymede.BaseDump BaseDump} objects,
   * providing a local cache of {@link arlut.csd.ganymede.Base Base}
   * references that the client consults during operations.</p>
   *
   * <p>If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.</p>
   */

  public Vector getBaseList()
  {
    if (!baseListLoaded || !keepGoing)
      {
	synchronized (this)
	  {
	    // we have to check baseListLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseListLoaded || !keepGoing)
	      {
		if (debug)
		  {
		    System.out.println("Dang, have to wait to get the base list");
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
	    System.out.println("baseList is null");
	  }
	else
	  {
	    System.out.println("returning baseList");
	  }
      }

    return baseList;
  }

  /**
   * <p>Returns a hash mapping {@link arlut.csd.ganymede.BaseDump BaseDump}
   * references to their title.</p>
   *
   * <p>If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.</p>
   */

  public Hashtable getBaseNames()
  {
    if (!baseNamesLoaded || !keepGoing)
      {
	synchronized (this)
	  {
	    // we have to check baseNamesLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseNamesLoaded || !keepGoing)
	      {
		if (debug)
		  {
		    System.out.println("Dang, have to wait to get the base names list");
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
	    System.out.println("baseNames is null");
	  }
	else
	  {
	    System.out.println("returning baseNames");
	  }
      }

    return baseNames;
  }

  /**
   * <p>Returns a hash mapping Short {@link arlut.csd.ganymede.Base Base} id's to
   * {@link arlut.csd.ganymede.BaseDump BaseDump} objects.</p>
   *
   * <p>If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.</p>
   */

  public Hashtable getBaseMap()
  {
    if (!baseMapLoaded || !keepGoing)
      {
	synchronized (this)
	  {
	    // we have to check baseMapLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseMapLoaded || !keepGoing)
	      {
		System.out.println("Loader: waiting for base map");

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
	    System.out.println("baseMap is null");
	  }
	else
	  {
	    System.out.println("returning baseMap");
	  }
      }

    return baseMap;
  }

  /**
   * <p>Returns a hashtable mapping {@link arlut.csd.ganymede.BaseDump BaseDump}
   * references to their object type id in Short form.  This is
   * a holdover from a time when the client didn't create local copies
   * of the server's Base references.</p>
   *
   * <p>If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.</p>
   */

  public Hashtable getBaseToShort()
  {
    // baseToShort is loaded in the loadBaseMap function, so we can just
    // check to see if the baseMapLoaded is true.

    if (!baseMapLoaded || !keepGoing)
      {
	synchronized (this)
	  {
	    // we have to check baseMapLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseMapLoaded || !keepGoing)
	      {
		if (debug)
		  {
		    System.out.println("Loader: waiting for base hash");
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
	    System.out.println("baseToShort is null");
	  }
	else
	  {
	    System.out.println("returning baseToShort");
	  }
      }

    return baseToShort;
  }

  /**
   * <p>Returns a hashtable mapping base names to their object type id
   * in Short form.  This is used by the XML client to quickly map
   * object type names to the numeric type id.</p>
   *
   * <p>If this thread hasn't yet downloaded that information, this method will
   * block until the information is available.</p> 
   */

  public Hashtable getNameToShort()
  {
    // baseToShort is loaded in the loadBaseMap function, so we can just
    // check to see if the baseMapLoaded is true.

    if (!baseMapLoaded || !keepGoing)
      {
	synchronized (this)
	  {
	    // we have to check baseMapLoaded inside the
	    // synchronization loop or else we can get deadlocked

	    while (!baseMapLoaded || !keepGoing)
	      {
		if (debug)
		  {
		    System.out.println("Loader: waiting for base hash");
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
	    System.out.println("nameShorts is null");
	  }
	else
	  {
	    System.out.println("returning nameShorts");
	  }
      }

    return nameShorts;
  }

  /**
   * <P>Returns a {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * for a field specified by object type id and field name.</P>
   */

  public FieldTemplate getFieldTemplate(short objectid, String fieldname)
  {
    return getFieldTemplate(new Short(objectid), fieldname);
  }

  /**
   * <P>Returns a {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * for a field specified by object type id and field name.</P>
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
   * <p>Returns a vector of 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}'s.</p>
   *
   * @param id Object type id to retrieve field information for.
   */

  public Vector getTemplateVector(short id)
  {
    return getTemplateVector(new Short(id));
  }

  /**
   * <p>Returns a vector of 
   * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}'s
   * listing fields and field informaton for the object type identified by 
   * id.</p>
   *
   * @param id The id number of the object type to be returned the base id.
   */

  public synchronized Vector getTemplateVector(Short id)
  {
    Vector result = null;

    /* -- */

    if (!keepGoing)
      {
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

  /* -- Private methods  --  */

  /** 
   * loadBaseList gets the list of types from the server.  This is
   * used in loadBaseHash to get the rest of the BaseHash, but is
   * also used in a few places in the client where the whole list
   * of fields in the baseHash isn't needed.
   */

  private synchronized void loadBaseList() throws RemoteException
  {
    baseList = session.getBaseList().getBaseList();

    if (debug)
      {
	System.out.println("Finished loading base list");

	if (baseList == null)
	  {
	    System.out.println("****** BaseList is null after loading!!!! *****");
	  }
	else
	  {
	    System.out.println("*** BaseList is not null.");
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
	System.out.println("Finished loading base list");
      }

    baseNamesLoaded = true;
    notifyAll();
  }

  /**
   * loadBaseHash is used to prepare a hash table mapping Bases to
   * Vector's of BaseField.. this is used to allow different pieces
   * of client-side code to get access to the Base/BaseField information,
   * which changes infrequently (not at all?) while the client is
   * connected.. the perm_editor panel created by the windowPanel class
   * benefits from this, as does buildTree() below. 
   */

  private synchronized void loadBaseHash() throws RemoteException
  {
    Base base;
    Vector typesV;

    /* -- */

    typesV = getBaseList();

    if (typesV == null)
      {
	throw new RuntimeException("typesV is null in Loader!");
      }

    if (baseHash != null)
      {
	baseHash.clear();
      }
    else
      {
	baseHash = new Hashtable(typesV.size());
      }
    
    for (int i = 0; i < typesV.size(); i++)
      {
	base = (Base) typesV.elementAt(i);

	if (base != null)
	  {
	    baseHash.put(base, base.getFields());

	    if (debug)
	      {
		System.out.println("Putting another base on the old baseHash");
	      }
	  }
	else
	  {
	    System.out.println("Base was null");
	  }
      }

    baseHashLoaded = true;
    notifyAll();
  }

  /**
   * loadBaseMap() generates baseMap, a mapping of Short's to
   * the corresponding remote base reference.
   */

  private synchronized void loadBaseMap() throws RemoteException
  {
    Base base;
    Enumeration enum;
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
	Short id = new Short(base.getTypeID());

	baseMap.put(id, base);
	baseToShort.put(base, id);
	nameShorts.put(base.getName(), id);
      }

    baseMapLoaded = true;
    notifyAll();
  }
}
