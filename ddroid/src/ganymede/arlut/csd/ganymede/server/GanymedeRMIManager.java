/*

   GanymedeRMIManager.java

   The GanymedeRMIManager class is responsible for publishing Ganymede
   server objects for accessibility through RMI.  All decisions
   regarding encryption and the like are handled in this class.
   
   Created: 15 November 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.ganymede.common.RMISSLClientSocketFactory;
import arlut.csd.ganymede.common.RMISSLServerSocketFactory;
import arlut.csd.ganymede.rmi.Server;
import arlut.csd.ganymede.rmi.adminSession;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                              GanymedeRMIManager

------------------------------------------------------------------------------*/

/**
 * <p>The GanymedeRMIManager class is responsible for publishing Ganymede
 * server objects for accessibility through RMI.  All decisions
 * regarding encryption and the like are handled in this class.</p>
 */

public class GanymedeRMIManager {

  private boolean useSSL = false;
  private RMISSLServerSocketFactory ssf = null;
  private RMISSLClientSocketFactory csf = null;
  private int port = 0;

  /**
   * <p>Constructor for the GanymedeRMIManager.  If the defaultPort is
   * 0, the port number for exported objects will be chosen at random
   * by the RMI runtime.  Any other value will force all objects to be
   * published on a constant port, useful for specifying single port access
   * through a firewall.</p>
   *
   * <p>If useSSL is true, all objects exported by this GanymedeRMIManager
   * will be using the arlut.csd.ganymede.common.RMISSLServerSocketFactory
   * and arlut.csd.ganymede.common.RMISSLClientSocketFactory socket
   * factories.</p>
   */

  public GanymedeRMIManager(int defaultPort, boolean useSSL)
  {
    this.port = defaultPort;

    if (useSSL)
      {
	this.useSSL = true;

	// let's keep our SSL sockets open for 2 minutes even in the face
	// of idle connections, as opposed to the 15 second default.

	System.getProperties().setProperty("sun.rmi.transport.connectionTimeout", "120000");
	this.csf = new RMISSLClientSocketFactory();
	this.ssf = new RMISSLServerSocketFactory();
      }
  }

  /**
   * <p>Exports the referenced Remote interface-implementing object
   * for remote access through RMI.  Returns true on success, or false
   * if the underlying exportObject() call threw a RemoteException.</p>
   */

  public boolean publishObject(Remote obj) throws RemoteException
  {
    if (useSSL)
      {
	UnicastRemoteObject.exportObject(obj, this.port, this.csf, this.ssf);
      }
    else
      {
	UnicastRemoteObject.exportObject(obj, this.port);
      }

    return true;
  }

  /**
   * <p>Removes the Remote obj reference from remote accessiblity.  Once this
   * call returns, the obj will not receive any more remote RMI calls.  If
   * force is true, the object will be unpublished even if there are pending
   * calls on it.</p>
   *
   * <p>Returns true on successful unpublish, or false if there was
   * some problem.</p>
   */

  public boolean unpublishObject(Remote obj, boolean force) throws NoSuchObjectException
  {
    return UnicastRemoteObject.unexportObject(obj, force);
  }
}
