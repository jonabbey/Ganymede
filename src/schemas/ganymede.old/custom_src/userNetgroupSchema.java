/*

   userNetgroupSchema.java

   An interface defining constants to be used by the user netgroup code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                              userNetgroupSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user netgroup code.
 *
 */

public interface userNetgroupSchema {

  // field id's for the user netgroup object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the user netgroup, you'll want to change
  // this file to match.

  final static short NETGROUPNAME=256;
  final static short USERS=257;
  final static short MEMBERGROUPS=258;
  final static short OWNERNETGROUPS=259;
}
