/*

   userSchema.java

   An interface defining constants to be used by the user code.
   
   Created: 12 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
  final static short PASSWORD=101;
  final static short UID=256;
  final static short HOMEGROUP=265;
  final static short GROUPLIST=264;
  final static short DIVISION=258;
  final static short OFFICEPHONE=260;
  final static short FULLNAME=257;
  final static short ROOM=259;
  final static short HOMEPHONE=261;
  final static short LOGINSHELL=263;
  final static short HOMEDIR=262;
  final static short PERSONAE=102;
  final static short NETGROUPS=266;
  final static short VOLUMES=271;
  final static short ALIASES=267;
  final static short SIGNATURE=268;
  final static short EMAILTARGET=269;
}
