/*

   Loader.java

   A convenient initialization thread, does start up stuff for
   the client.
   
   Created: 1 October 1997
   Release: $Name:  $
   Version: $Revision: 1.13 $
   Last Mod Date: $Date: 1999/01/22 18:04:10 $
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Loader

------------------------------------------------------------------------------*/

public class Loader extends Thread {

  private boolean debug = true;

  private Hashtable 
    baseMap,
    baseNames,
    baseHash,
    baseToShort;

  private Vector
    baseList;
 
  private boolean
    keepGoing = true,
    baseNamesLoaded = false,
    baseListLoaded = false,
    baseMapLoaded = false,
    baseHashLoaded = false;

  private Session
    session;

  /* -- */

  public Loader(Session session, boolean debug)
  {
    if (debug)
      {
	System.out.println("Initializing Loader");
      }

    this.debug = debug;

    this.session = session;
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
	if(!( baseNamesLoaded && baseListLoaded && baseMapLoaded))
	  {
	    System.out.println("***There are not all finished.");
	  }
	else
	  {
	    System.out.println("***All the hashes are clear.");
	  }
      }
     

    while (! ( baseNamesLoaded && baseListLoaded && baseMapLoaded))
      {
	System.out.println("Loader waiting for previous method to stop.");

	synchronized (this) 
	  {
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

    if (debug)
      {
	System.out.println("Starting to load the loader again");
      }

    keepGoing = true;

    Thread t = new Thread(this);
    t.start();
  }

  public Vector getBaseList()
  {
    while (! baseListLoaded)
      {
	if (debug)
	  {
	    System.out.println("Dang, have to wait to get the base list");
	  }

	synchronized (this)
	  {
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

  public Hashtable getBaseNames()
  {
    while (! baseNamesLoaded)
      {
	if (debug)
	  {
	    System.out.println("Dang, have to wait to get the base names list");
	  }

	synchronized (this)
	  {
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

  public Hashtable getBaseMap()
  {
    while (! baseMapLoaded)
      {
	System.out.println("Loader: waiting for base map");

	synchronized (this)
	  {
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

  public Hashtable getBaseToShort()
  {
    // baseToShort is loaded in the loadBaseMap function, so we can just
    // check to see if the baseMapLoaded is true.

    while (! baseMapLoaded)
      {
	if (debug)
	  {
	    System.out.println("Loader: waiting for base hash");
	  }

	synchronized (this)
	  {
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

  /* -- Private methods  --  */

  /** 
   *
   * loadBaseList gets the list of types from the server.  This is
   * used in loadBaseHash to get the rest of the BaseHash, but is
   * also used in a few places in the client where the whole list
   * of fields in the baseHash isn't needed.
   *
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

    baseListLoaded = true;
    notifyAll();
  }

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
   *
   * loadBaseHash is used to prepare a hash table mapping Bases to
   * Vector's of BaseField.. this is used to allow different pieces
   * of client-side code to get access to the Base/BaseField information,
   * which changes infrequently (not at all?) while the client is
   * connected.. the perm_editor panel created by the windowPanel class
   * benefits from this, as does buildTree() below. 
   *
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
   *
   * loadBaseMap() generates baseMap, a mapping of Short's to
   * the corresponding remote base reference.
   *
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

    for (int i = 0; i < size; i++)
      {
	base = (Base) myBaseList.elementAt(i);
	Short id = new Short(base.getTypeID());

	baseMap.put(id, base);
	baseToShort.put(base, id);
      }

    baseMapLoaded = true;
    notifyAll();
  }
}
