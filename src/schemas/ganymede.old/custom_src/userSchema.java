/*

   userSchema.java

   An interface defining constants to be used by the user code.
   
   Created: 12 March 1998
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:04:35 $
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
                                                                      userSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface userSchema {

  // field id's for the user object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the user, you'll want to change
  // this file to match.

  final static short USERNAME=100;
  final static short PERSON=272;
  final static short PRIVILEGED=273;
  final static short PURPOSE=274;
  final static short PERSONAE=102;
  final static short PASSWORD=101;
  final static short ALIASES=267;
  final static short SIGNATURE=268;
  final static short EMAILTARGET=269;
  final static short UNIX=275;
  final static short UNIXENABLED=276;
  final static short UID=256;
  final static short LOGINSHELL=263;
  final static short HOMEGROUP=265;
  final static short GROUPLIST=264;
  final static short NETGROUPS=266;
  final static short HOMEDIR=262;
  final static short VOLUMES=271;
}
