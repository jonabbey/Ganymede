/*

   Loader.java

   A convenient initialization thread, does start up stuff for
   the client.
   
   Created: 1 October 1997
   Version: $Revision: 1.6 $ %D%
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
    baseToShort;

  private Vector
    baseList;
 
  private boolean
    baseNamesLoaded = false,
    baseListLoaded = false,
    baseMapLoaded = false,
    baseHashLoaded = false;

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

  public Vector getBaseList()
  {
    while (! baseListLoaded)
      {
	synchronized (this)
	  {
	    System.out.println("Dang, have to wait to get the base list");
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
	synchronized (this)
	  {
	    System.out.println("Dang, have to wait to get the base names list");
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

  public synchronized Hashtable getBaseHash()
  {
    while (! baseHashLoaded)
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

  public synchronized Hashtable getBaseMap()
  {
    while (! baseMapLoaded)
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
  public synchronized Hashtable getBaseToShort()
  {
    // baseToShort is loaded in the loadBaseMap function, so we can just
    // check to see if the baseMapLoaded is true.
    while (! baseMapLoaded)
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
	baseHash.put(base, base.getFields());
	if (debug)
	  {
	    System.out.println("Putting another base on the old baseHash");
	  }
      }

    baseHashLoaded = true;
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
  }


}
