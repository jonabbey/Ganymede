/*

   Client.java

   Server-side interface for the Client object.

   Created: 28 May 1996
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2000/02/16 11:31:59 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                          Client

------------------------------------------------------------------------------*/

/**
 * <p>Interface that must be implemented by client code that connects to the
 * ganymede server.  The {@link arlut.csd.ganymede.GanymedeServer GanymedeServer}'s
 * {@link arlut.csd.ganymede.GanymedeServer#login(arlut.csd.ganymede.Client) login method}
 * uses this interface to authenticate a client, and to force the client to shutdown
 * if the user is forced off or the server goes down.</p>
 */

public interface Client extends Remote {

  /**
   * <p>Allows the server to retrieve the username.</p>
   */

  String getName() throws RemoteException;

  /**
   * <p>Allows the server to retrieve the password.</p>
   */

  String getPassword() throws RemoteException;

  /**
   * <p>Allows the server to force us off when it goes down.</p>
   */

  void forceDisconnect(String reason) throws RemoteException;

  /**
   * <p>Allows the server to send an asynchronous message to the
   * client..  Used by the server to tell the client when a build
   * is/is not being performed on the server.</P> 
   *
   * <p>The only messageType defined so far is 1, build update.</p>
   */

  void sendMessage(int messageType, String status) throws RemoteException;
}
