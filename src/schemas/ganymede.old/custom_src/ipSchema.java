/*

   ipSchema.java

   An interface defining constants to be used by the ip code.
   
   Created: 23 April 1998
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/22 18:04:29 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                        ipSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the ip code.
 *
 */

public interface ipSchema {

  // field id's for the ip object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the ip, you'll want to change
  // this file to match.

  final static short DNSRECORDS=256;
  final static short ADDRESS=257;
  final static short IPNET=258;
}
