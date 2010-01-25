/*

   ObjectStatus.java

   Hackishly enumerated type for DBEditObject status
   
   Created: 15 April 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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

package arlut.csd.ganymede.common;

/**
 * Interface constants used to enumerate possible editing states for
 * a {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}.
 */

public interface ObjectStatus {

  /**
   * Status code for an object in the DBStore that has been checked out for editing.
   */
  static final byte EDITING = 1;

  /**
   * Status code for a newly created object.
   */
  static final byte CREATING = 2;

  /**
   * Status code for a previously existing object that is to be deleted
   */
  static final byte DELETING = 3;

  /**
   * Status code for a newly created object that is to be dropped
   */
  static final byte DROPPING = 4;

}
