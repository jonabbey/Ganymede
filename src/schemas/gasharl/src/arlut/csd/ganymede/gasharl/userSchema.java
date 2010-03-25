/*

   userSchema.java

   An interface defining constants to be used by the user code.
   
   Created: 12 March 1998

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

  final static short BASE=3;

  final static short USERNAME=100;
  final static short UID=256;
  final static short GUID=277;
  final static short PASSWORD=101;
  final static short CATEGORY=273;
  final static short FULLNAME=257;
  final static short BADGE=276;
  final static short SOCIALSECURITY=274; // removed
  final static short DIVISION=258;
  final static short ROOM=259;
  final static short OFFICEPHONE=260;
  final static short HOMEPHONE=261;
  final static short GROUPLIST=264;
  final static short HOMEGROUP=265;
  final static short LOGINSHELL=263;
  final static short HOMEDIR=262;
  final static short PERSONAE=102;
  final static short NETGROUPS=266;
  final static short VOLUMES=271;
  final static short ALIASES=267;
  final static short SIGNATURE=268;
  final static short EMAILTARGET=269;
  final static short PASSWORDCHANGETIME=275;
  final static short ALLOWEXTERNAL=278;
  final static short MAILUSER=279;
  final static short MAILPASSWORD=280;
  final static short MAILEXPDATE=281;
  final static short OLDMAILUSER=282;
  final static short OLDMAILPASSWORD=283;
}
