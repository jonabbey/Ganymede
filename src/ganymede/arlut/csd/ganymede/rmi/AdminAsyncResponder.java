/*

   AdminAsyncResponder.java

   Server-side interface for the serverAdminAsyncResponder object.

   Created: 5 September 2003

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import arlut.csd.ganymede.common.adminAsyncMessage;

/*------------------------------------------------------------------------------
                                                                       interface
                                                             AdminAsyncResponder

------------------------------------------------------------------------------*/

/**
 * <p>Remote Interface exported by the {@link
 * arlut.csd.ganymede.server.serverAdminAsyncResponder} object.  The console
 * can call methods on this remote interface to query the server for
 * new asynchronous notification events.</p>
 */

public interface AdminAsyncResponder extends Remote {

  /**
   * <p>Returns true if we are open for communications.</p>
   */

  public boolean isAlive() throws RemoteException;

  /**
   * <p>Blocks waiting for the next group of asynchronous message from the
   * server.  Returns null if isAlive() is false.</p>
   */

  public adminAsyncMessage[] getNextMsgs() throws RemoteException;
}
