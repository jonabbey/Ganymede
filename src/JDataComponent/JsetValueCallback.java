/*

   JsetValueCallback.java

   Created: 18 June 1996
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:04:00 $
   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

package arlut.csd.JDataComponent;

/*------------------------------------------------------------------------------
                                                                       interface
                                                               JsetValueCallback

------------------------------------------------------------------------------*/

/**
 *  This interface is used to allow callback to be done from the
 *  components in the package to the container which contains them.
 *  The conatiner that is to contain the components needs to provide
 *  implementations for the methods defined in this interface.
 */

public interface JsetValueCallback
{
  public boolean setValuePerformed(JValueObject v) throws java.rmi.RemoteException;
}









