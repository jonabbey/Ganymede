/*

   ServiceFailedException.java
 
   Created: 24 May 2010

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

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

package arlut.csd.ganymede.server;


/*------------------------------------------------------------------------------
                                                                           class
                                                          ServiceFailedException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific RuntimeException that is designed to be
 * thrown by GanymedeBuilderTask subclasses to communicate build
 * status information to the admin console.</p>
 */

public class ServiceFailedException extends RuntimeException {

  public ServiceFailedException()
  {
    super();
  }

  public ServiceFailedException(String s)
  {
    super(s);
  }
}
