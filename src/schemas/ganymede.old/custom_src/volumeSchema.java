/*

   volumeSchema.java

   An interface defining constants to be used by the automounter map code.
   
   Created: 15 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    volumeSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface volumeSchema {

  // field id's for the automounter map object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the automounter map, you'll want to change
  // this file to match.

  final static short VOLNAME=256;
  final static short HOST=257;
  final static short PATH=258;
  final static short MAPENTRIES=259;
  final static short OPTIONS=260;
}
