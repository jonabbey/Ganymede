/*

   mapSchema.java

   An interface defining constants to be used by the automounter map code.
   
   Created: 15 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       mapSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface mapSchema {

  // field id's for the automounter map object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the automounter map, you'll want to change
  // this file to match.

  final static short MAPNAME=256;
  final static short MAPENTRIES=257;
}
