/*

   systemNetgroupSchema.java

   An interface defining constants to be used by the system netgroup code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                            systemNetgroupSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the system netgroup code.
 *
 */

public interface systemNetgroupSchema {

  // field id's for the system netgroup object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the system netgroup, you'll want to change
  // this file to match.

  final static short NETGROUPNAME=256;
  final static short SYSTEMS=257;
  final static short MEMBERGROUPS=258;
  final static short OWNERNETGROUPS=259;
}
