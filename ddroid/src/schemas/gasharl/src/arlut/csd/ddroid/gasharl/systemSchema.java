/*

   systemSchema.java

   An interface defining constants to be used by the system code.
   
   Created: 23 April 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
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

package arlut.csd.ddroid.gasharl;

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;
import arlut.csd.ddroid.server.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    systemSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the system code.
 *
 */

public interface systemSchema {

  // field id's for the system object.  These should match the
  // current specs in the Directory Droid schema file precisely.  If
  // you change the schema for the system, you'll want to change
  // this file to match.

  final static short OS=256;
  final static short MANUFACTURER=257;
  final static short MODEL=258;
  final static short INTERFACES=260;
  final static short SYSTEMNAME=261;
  final static short SYSTEMALIASES=262;
  final static short ROOM=264;
  final static short NETGROUPS=265;
  final static short SYSTEMTYPE=266;
  final static short PRIMARYUSER=267;
  final static short VOLUMES=268;
}
