/*

   GanymedeConfig.java

   This module defines a class with useful constants.  To change
   these constants, edit this file and re-build Ganymede.
   
   Created: 3 July 1997
   Release: $Name:  $
   Version: $Revision: 1.9 $
   Last Mod Date: $Date: 1999/01/22 18:05:44 $
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

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GanymedeConfig

------------------------------------------------------------------------------*/

public class GanymedeConfig {
  static final String schemaPath = "db/ganymede.schema"; // what name to dump the schema to?
  static final String journal = "db/journal"; // what's our journal file?
}
