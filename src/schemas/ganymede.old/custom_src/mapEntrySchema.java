/*

   mapEntrySchema.java

   An interface defining constants to be used by the automounter map code.
   
   Created: 15 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                  mapEntrySchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface mapEntrySchema {

  // field id's for the automounter map object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the automounter map, you'll want to change
  // this file to match.

  final static short CONTAININGUSER = 0;
  final static short MAP=256;
  final static short VOLUME=257;
}
