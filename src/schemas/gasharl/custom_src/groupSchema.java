/*

   groupSchema.java

   An interface defining constants to be used by the group code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                     groupSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the group code.
 *
 */

public interface groupSchema {

  // field id's for the group object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the group, you'll want to change
  // this file to match.

  final static short GROUPNAME=256;
  final static short PASSWORD=257;
  final static short GID=258;
  final static short DESCRIPTION=259;
  final static short CONTRACT=260;
  final static short USERS=261;
  final static short HOMEUSERS=262;
}
