/*

   Loader.java

   A convenient initialization thread, does start up stuff for
   the client.
   
   Created: 1 October 1997
   Version: $Revision: 1.8 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

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

  private final boolean debug = false;

  private Hashtable 
    baseMap,
    baseNames,
    baseHash,
    baseHashNoBuiltIns,
    baseToShort;

  private Vector
    baseList;
 
  private boolean
    baseNamesLoaded = false,
    baseListLoaded = false,
    baseMapLoaded = false,
    baseHashLoaded = false,
    baseHashNoBuiltInsLoaded = false;

  private Session
    session;

  /* -- */

  public Loader(Session session)
  {
    if (debug)
      {
	System.out.println("Initializing Loader");
      }

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
	loadBaseList();
	loadBaseNames();
	loadBaseHash();
	loadBaseHashNoBuiltIns();
	loadBaseMap();
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
   * Clear out all the information in the loader.
   *
   */
  public void clear()
  {

    if (debug)
      {
	System.out.println("Clearing the loader");
      }

    // Set everything to false, so it will reload them all.
    baseNamesLoaded = false;
    baseListLoaded = false;
    baseMapLoaded = false;
    baseHashLoaded = false;
    
    baseList = null;
    baseNames = null;
    baseHash = null;
    baseMap = null;
    baseList = null;

    if (debug)
      {
	System.out.println("Starting to load the loader again");
      }

    Thread t = new Thread(this);
    t.start();

  }

  public Vector getBaseList()
  {
    while (! baseListLoaded)
      {
	System.out.println("Dang, have to wait to get the base list");
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
	System.out.println("Dang, have to wait to get the base names list");

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
  public Hashtable getBaseHash()
  {
    while (! baseHashLoaded)
      {
	System.out.println("Loader: waiting for base hash");

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
	if (baseHash == null)
	  {
	    System.out.println("baseHash is null");
	  }
	else
	  {
	    System.out.println("returning baseHash");
	  }
      }

    return baseHash;
    
  }

  public Hashtable getBaseHashNoBuiltIns()
  {
    while (! baseHashNoBuiltInsLoaded)
      {
	System.out.println("Loader: waiting for base hash no built ins");

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
	if (baseHashNoBuiltIns == null)
	  {
	    System.out.println("baseHashNoBuiltIns is null");
	  }
	else
	  {
	    System.out.println("returning baseHashNoBuiltIns");
	  }
      }

    return baseHashNoBuiltIns;
    
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
	System.out.println("Loader: waiting for base hash");

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
    
    baseList = session.getTypes();

    if (debug)
      {
	System.out.println("Finished loading base list");
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
	    baseHash.put(base, base.getFields(true));
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


  private synchronized void loadBaseHashNoBuiltIns() throws RemoteException
  {
    Base base;
    Vector typesV;

    /* -- */

    typesV = getBaseList();

    if (typesV == null)
      {
	throw new RuntimeException("typesV is null in Loader!");
      }

    if (baseHashNoBuiltIns != null)
      {
	baseHashNoBuiltIns.clear();
      }
    else
      {
	baseHashNoBuiltIns = new Hashtable(typesV.size());
      }
    
    for (int i = 0; i < typesV.size(); i++)
      {
	base = (Base) typesV.elementAt(i);
	if (base != null)
	  {
	    baseHashNoBuiltIns.put(base, base.getFields(false));
	    if (debug)
	      {
		System.out.println("Putting another base on the old baseHashNoBuiltIns");
	      }
	  }
	else
	  {
	    System.out.println("Base was null");
	  }
      }

    baseHashNoBuiltInsLoaded = true;
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

    /* -- */

    baseMap = new Hashtable(baseHash.size());
    baseToShort = new Hashtable(baseHash.size());

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	base = (Base) enum.nextElement();
	Short id = new Short(base.getTypeID());

	baseMap.put(id, base);
	baseToShort.put(base, id);
      }

    baseMapLoaded = true;
    notifyAll();
  }


}
