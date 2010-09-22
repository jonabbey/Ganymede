/*

   exchangeStoreSchema.java

   An interface defining constants for representing an Exchange Store
   in our Active Directory cluster.
   
   Created: 22 September 2010

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

package arlut.csd.ganymede.gasharl;


/*------------------------------------------------------------------------------
                                                                       interface
                                                             exchangeStoreSchema

------------------------------------------------------------------------------*/

/**
 * An interface defining constants for representing an Exchange Store
 * in our Active Directory cluster.
 */

public interface exchangeStoreSchema {

  // field id's for the Exchange Store object.  These should match the
  // current specs in the Ganymede schema file precisely.  If you
  // change the schema for the Exchange Store, you'll want to change
  // this file to match.

  final static short BASE=273;

  final static short MAILSTORENAME=256;
  final static short EXCHANGEMDB=257;
}
