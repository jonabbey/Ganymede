/*

   NotLoggedInException.java
 
   Created: 11 March 2003
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2003/03/11 23:22:56 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
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
                                                            NotLoggedInException

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede-specific RemoteException subclass that can be
 * thrown by the server if a method is called on a GanymedeSession
 * after that session has terminated.</p>
 */

public class NotLoggedInException extends java.rmi.RemoteException {

  public NotLoggedInException()
  {
    super();
  }

  public NotLoggedInException(String s)
  {
    super(s);
  }
}
