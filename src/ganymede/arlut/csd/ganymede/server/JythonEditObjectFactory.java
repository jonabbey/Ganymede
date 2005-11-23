/*

  JythonEditObjectFactory.java

  This class acts as a factory for Jython-based DBEditObjects that are
  loaded from an external location (disk, HTTP, FTP, etc).

   Created: 5 August 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Deepak Giridharagopal <deepak@arlut.utexas.edu>

   -----------------------------------------------------------------------
      
   Ganymede Directory Management System

   Copyright (C) 1996-2005
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import arlut.csd.ganymede.common.Invid;

/**
 * <p>
 * This class is a factory for Jython-based DBEditObject subclasses. It will
 * attempt to load the source for these subclasses from an external location
 * that is specified in the options of the given 
 * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}.
 * </p>
 * 
 * <p>
 * It implements the 3 required factory methods that are analogues of the 3
 * constructors for all {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
 * constructors.
 * </p>
 */
public class JythonEditObjectFactory {

  /**
   * The Jython interpreter that's responsible for loading the external source file
   * and instantiating a Java class from it.
   */
  static PythonInterpreter interp = null;

  /**
   * <p>
   * Factory version of the {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * customization constructor.
   * </p>
   * 
   * @param base the object base the newly created DBEditObject should be attached to
   */
  
  public static DBEditObject factory(DBObjectBase base)
  {
    return factory(base, null, null, null);
  }

  /**
   * <p>
   * Factory version of the {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * new object constructor.
   * </p>
   * 
   * @param base the object base the newly created DBEditObject should be attached to
   * @param invid the Invid to associate with this object
   * @param editset the transaction to associate the object with
   */
  
  public static DBEditObject factory(DBObjectBase base, Invid invid, DBEditSet editset)
  {
    return factory(base, invid, editset, null);
  }
  
  /**
   * <p>
   * Factory version of the {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * check-out constructor.
   * </p>
   * 
   * @param original the object to be "checked out"
   * @param editset the transaction to associate the object with
   */
  
  public static DBEditObject factory(DBObject original, DBEditSet editset)
  {
    return factory(null, null, editset, original);
  }

  /**
   * <p>
   * This is the <i>workhorse</i> factory method that the constructors all end up
   * calling in the end. This function will simply pass all of the possible 
   * constructor parameters to the Jython-based
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}, whose constructor
   * is smart enough to determine which superclass constructor to call.
   * </p>
   * 
   * <p>
   * This should never be called directly...use one of the other 3 public factory
   * methods instead.
   * </p>
   */
  
  private static DBEditObject factory(DBObjectBase base, Invid invid, DBEditSet editset, DBObject original)
  {
    if (interp == null)
      {
        Ganymede.debug("Initializing interpreter");
        initializeInterpreter();
      }
    
    String uri = null;
    
    /* If we've been handed a DBObjectBase, use it to find the URI (which
     * is held in the DBObjectBase's options string). If we don't have one,
     * the we must have been called from the copy constructor and we should
     * have a non-null DBObject. We can get its parent DBObjectBase and then
     * get the options string from there.
     */
    if (base != null)
      {
        uri = base.getClassOptionString();
      }
    else if (original != null)
      {
        uri = original.getBase().getClassOptionString();
      }
    
    return loadJythonClass(uri, base, invid, editset, original);
  }
  
  /**
   * <p>
   * Sets up the Jython interpreter and ensures that it can find any requisite
   * external libraries
   * </p> 
   */
  
  private synchronized static void initializeInterpreter()
  {
    PySystemState.initialize();
    interp = new PythonInterpreter(null, new PySystemState());
    
    try
      {
        /* Import the additional Jython library routines */
        interp.exec("import sys");
        interp.exec("sys.path.append( sys.prefix + '" + System.getProperty("file.separator") + "' + 'jython-lib.jar' )");
      }
    catch (PyException pex)
      {
        throw new RuntimeException(pex.toString());
      }
  }
  
  /**
   * <p>
   * This method takes the given URI and transforms the Jython text it points to into
   * a Java object. All of the other arguments as simply passed to the Jython text's 
   * constructor.
   * </p>
   *
   * <p>
   * This will only load the Jython text once...after that it's cached. To unload
   * the uri's class definition, use the unloadURI method.
   * </p>
   * 
   * @return a newly constructed DBEditObject subclass
   */
  
  private synchronized static DBEditObject loadJythonClass(String uri, DBObjectBase base, Invid invid, DBEditSet editset, DBObject original)
  {
    if (uri == null || uri.equals(""))
      {
        Ganymede.debug("No URI was passed in!");
        return null;
      }
    try
      {
        Ganymede.debug("Invoking Jython loader");
        interp.exec("from JythonEditObjectBootstrapper import *");
        
        /* We'll go ahead an pass in all of the args we've got, null or not. The
         * Jython class' constructor is smart enough to figure out which superclass
         * constructor to call based on which of the arguments are null. */
         
        interp.set("uri", uri);
        interp.set("base", base);
        interp.set("invid", invid);
        interp.set("editset", editset);
        interp.set("original", original);
        interp.exec("obj = get_jythonEditObject(uri, base, invid, editset, original)");
        
        return (DBEditObject) interp.get("obj", DBEditObject.class);
      }
    catch (PyException pex)
      {
        Ganymede.debug(pex.toString());
        throw new GanymedeManagementException(pex.toString());
      }
  }
  
  /**
   * <p>
   * Removes the class definition associated with the given URI from the
   * class cache. This method should only be used to designate that the
   * next time the given URI is loaded, you should bypass the cache.
   * </p>
   * 
   * @param uri the uri we want to reset. If this is <b>null</b>, then
   * <b>ALL</b> of the class definitions in the cache will be unloaded
   */
  
  public synchronized static void unloadURI(String uri)
  {
    if (interp == null)
      {
        return;
      }
    
    interp.set("uri_to_unload", uri);
    interp.exec("unload_class(uri_to_unload)");
  }

  /**
   * <p>
   * Similar to {@link arlut.csd.ganymede.server.JythonEditObjectFactory#unloadURI(java.lang.String) unloadURI},
   * except this method will unload <b>ALL</b> of the class definitions
   * in the cache.
   * </p>
   */
  
  public synchronized static void unloadAllURIs()
  {
    unloadURI(null);
  }
  
}
