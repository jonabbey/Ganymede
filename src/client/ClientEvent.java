/*

   ClientEvent.java

   An event object to pass information from the ClientBase class to
   users thereof. Currently, this only has support for a message
   String, but it is here in case we need some other types in there.
   
   Created: 31 March 1998
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 2000/05/30 05:53:36 $
   Module By: Michael Mulvaney

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

package arlut.csd.ganymede.client;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     ClientEvent

------------------------------------------------------------------------------*/

/**
 * <P>An event object to pass information from the {@link
 * arlut.csd.ganymede.client.ClientBase ClientBase} class to users
 * thereof. Currently, this only has support for a message String and
 * integer message type, but it is here in case we need some other
 * types in there.</P>
 */

public class ClientEvent implements arlut.csd.ganymede.ClientMessage {

  int type;
  private String message;

  /* -- */

  public ClientEvent(String message)
  {
    this.message = message;
    this.type = ERROR;		// this used to be the only kind
  }

  public ClientEvent(int type, String message)
  {
    this.type = type;
    this.message = message;
  }

  /**
   *
   * Returns the message type for this event.
   *
   */

  public int getType()
  {
    return type;
  }

  /**
   *
   * Returns the message for this event.
   *
   */

  public String getMessage()
  {
    return message;
  }

  public String toString()
  {
    return "ClientEvent: " + message;
  }
}
