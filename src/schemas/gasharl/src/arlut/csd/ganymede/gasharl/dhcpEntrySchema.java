/*

   dhcpEntrySchema.java

   An interface defining constants to be used by the dhcp entry code.
   
   Created: 10 October 2007
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2007
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
                                                                 dhcpEntrySchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface dhcpEntrySchema {

  // field id's for an option entry in the DHCP Group or system
  // objects.  These should match the current specs in the Ganymede
  // schema file precisely.  If you change the schema for the DHCP
  // Entry object, you'll want to change this file to match.

  final static short LABEL=256;
  final static short TYPE=257;
  final static short VALUE=258;

  // suggestion.. add a hidden vector of system links, to represent a
  // hidden relationship to a system in the ganymede database, so that
  // we can deal with it being removed automatically.. ?
}
