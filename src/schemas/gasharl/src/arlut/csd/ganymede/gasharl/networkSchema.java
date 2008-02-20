/*

   networkSchema.java

   An interface defining constants to be used by the network code.
   
   Created: 16 May 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2008
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.gasharl;


/*------------------------------------------------------------------------------
                                                                       interface
                                                                   networkSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the network code.
 *
 */

public interface networkSchema {
  final static short NETNUMBER=256;
  final static short BROADCAST=257;
  final static short NETMASK=258;
  final static short NAMESERVER=259;
  final static short DEFAULTROUTER=260;
  final static short NAME=261;
  final static short IPV6OK=262;
  final static short INTERFACES=263;
  final static short ROOMS=264;
  final static short ALLOCRANGE=265;
  final static short PUBLICNETWORK=266;
  final static short DHCP=267;
  final static short MACREQUIRED=268;
}
