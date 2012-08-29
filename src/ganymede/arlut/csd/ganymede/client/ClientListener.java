/*

   ClientListener.java

   An interface to complement the ClientBase class.  This interface
   must be implemented by any code that creates uses ClientBase to
   talk to the Ganymede server.
   
   Created: 31 March 1998

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2011
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;


/*------------------------------------------------------------------------------
                                                                       interface
                                                                  ClientListener

------------------------------------------------------------------------------*/

/** 
 * <p>An interface to complement the {@link
 * arlut.csd.ganymede.client.ClientBase ClientBase} class.  This
 * interface must be implemented by any code that creates uses
 * ClientBase to talk to the Ganymede server.</p>
 *
 * @see arlut.csd.ganymede.client.ClientEvent
 */

public interface ClientListener {

  /**
   * <p>Called when the server forces a disconnect.</p>
   *
   * <p>Call getMessage() on the passed
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} e
   * to get the reason for the disconnect.</p>
   */

  public void disconnected(ClientEvent e);

  /**
   * <p>Called when the ClientBase needs to report something
   * to the client.  The client is expected to then put
   * up a dialog or do whatever else is appropriate.</p>
   *
   * <p>Call getMessage() on the passed
   * {@link arlut.csd.ganymede.client.ClientEvent ClientEvent} e
   * to get the reason for the disconnect.</p>
   */

  public void messageReceived(ClientEvent e);
}
