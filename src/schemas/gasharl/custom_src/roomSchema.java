/*

   roomSchema.java

   An interface defining constants to be used by the room code.
   
   Created: 15 May 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      roomSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the room code.
 *
 */

public interface roomSchema {

  // field id's for the room object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the room, you'll want to change
  // this file to match.

  final static short ROOMNUMBER=256;
  final static short SYSTEMS=257;
  final static short NETWORKS=258;
}
