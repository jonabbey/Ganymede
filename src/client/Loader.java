/*

   Loader.java

   A convenient initialization thread, does start up stuff for
   the client.
   
   Created: 1 October 1997
   Version: $Revision: 1.1 $ %D%
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
    baseHash;

  private boolean
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

  /* -- Private methods  --  */

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

    typesV = session.getTypes();

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

    baseMap = new Hashtable();

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	base = (Base) enum.nextElement();

	baseMap.put(new Short(base.getTypeID()), base);
      }

    baseMapLoaded = true;
  }


}
