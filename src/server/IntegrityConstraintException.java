/*

   IntegrityConstraintException.java
 
   Created: 8 October 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/10/09 05:51:52 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

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

/*------------------------------------------------------------------------------
                                                                           class
                                                    IntegrityConstraintException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific RuntimeException that can be thrown by the
 * server if a piece of the server's code encounters a condition that violates
 * the fundamental integrity constraint guarantees of the Ganymede database.</p>
 */

public class IntegrityConstraintException extends RuntimeException {

  public IntegrityConstraintException()
  {
    super();
  }

  public IntegrityConstraintException(String s)
  {
    super(s);
  }
}
