/*

   dhcpSubnetSchema.java

   An interface defining constants to be used by the DHCP Option code.

   Created: 1 August 2013

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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
                                                                dhcpSubnetSchema

------------------------------------------------------------------------------*/

/**
 * An interface defining constants to be used by the DHCP Schema code.
 */

public interface dhcpSubnetSchema {

  final static short BASE=284;

  final static short NAME=256;
  final static short NETWORK_NUMBER=257;
  final static short NETWORK_MASK=258;
  final static short OPTIONS=259;
  final static short ALLOW_REGISTERED_GUESTS=260;
  final static short GUEST_RANGE=261;
  final static short GUEST_OPTIONS=262;
}
