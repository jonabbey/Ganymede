/*

   AdminEntry.java

   A serializable object, holding the contents of a row in an
   admin console's table.
   
   Created: 3 February 1997
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2002/01/26 04:49:27 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
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

/**
 * <p>Serializable data carrier object used by the Ganymede
 * server to report a row of user status to the Ganymede admin console.</p>
 */

public class AdminEntry implements java.io.Serializable {

  static final long serialVersionUID = -2534608083606361951L;

  // ---

  String
    username,
    personaName,
    hostname,
    status,
    connecttime,
    event;
  
  int
    objectsCheckedOut;

  /* -- */

  public AdminEntry(String username, String personaName,
		    String hostname, String status,
		    String connecttime, String event,
		    int objectsCheckedOut)
  {
    this.username = username;
    this.personaName = personaName;
    this.hostname = hostname;
    this.status = status;
    this.connecttime = connecttime;
    this.event = event;
    this.objectsCheckedOut = objectsCheckedOut;
  }
}
