/*

   Server.java

   Client side remote interface.

   Remote interface definition for the Ganymede Server Object.  The
   Ganymede Server object provides the interface that clients use to
   log in to the Ganymede Server.

   Created: 1 April 1996
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2003/09/05 21:09:40 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

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

package arlut.csd.ganymede;

import java.rmi.*;

// Server is our remote access interface 

public interface Server extends Remote {

  /** 
   * <p>Client login method.  Establishes a {@link
   * arlut.csd.ganymede.GanymedeSession GanymedeSession} object in the
   * server for the client, and returns a serializable {@link
   * arlut.csd.ganymede.ReturnVal ReturnVal} object which will contain
   * a {@link arlut.csd.ganymede.Session Session} remote reference
   * for the client to use, if login was successful.</p>
   *
   * <p>If login is not successful, the ReturnVal object will encode
   * a failure condition, along with a dialog explaining the problem.</p>
   *
   * <p>The GanymedeSession object contains all of the server's
   * knowledge about a given client's status., and is tracked by
   * the GanymedeServer object for statistics and for the admin
   * console's monitoring support.</P>
   */

  public ReturnVal login(String username, String password) throws RemoteException;

  /** 
   * <p>XML Client login method.  Establishes a {@link
   * arlut.csd.ganymede.GanymedeXMLSession GanymedeXMLSession} object
   * in the server for the client, and returns a serializable {@link
   * arlut.csd.ganymede.ReturnVal ReturnVal} object which will contain
   * a {@link arlut.csd.ganymede.XMLSession XMLSession} remote reference
   * for the client to use, if login was successful.</p>
   *
   * <p>If login is not successful, the ReturnVal object will encode
   * a failure condition, along with a dialog explaining the problem.</p>
   *
   * <p>The GanymedeXMLSession object in turn contains a
   * GanymedeSession object, which contains all of the server's
   * knowledge about a given client's status., and is tracked by the
   * GanymedeServer object for statistics and for the admin console's
   * monitoring support.</P>
   */
  
  public ReturnVal xmlLogin(String username, String password) throws RemoteException;

  /**
   * <p>This method is used to process a Ganymede admin console
   * attachment.  The returned {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * object will contain an {@link arlut.csd.ganymede.adminSession adminSession}
   * remote reference, accessible with the
   *{@link arlut.csd.ganymede.ReturnVal#getAdminSession() getAdminSession()}
   * method if the admin connect was successful.</p>
   *
   * <p>If the admin console connect was not successful, the reason why it failed
   * will be encoded in a dialog definition contained in ReturnVal.</p>
   */

  public ReturnVal admin(String username, String password) throws RemoteException;

  /**
   * <p>Simple RMI test method.. this method is here so that the
   * {@link arlut.csd.ganymede.client.ClientBase ClientBase} class
   * can test to see whether it has truly gotten a valid RMI reference
   * to the server.</p>
   */

  boolean up() throws RemoteException;
}
