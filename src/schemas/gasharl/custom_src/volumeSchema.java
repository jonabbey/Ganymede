/*

   volumeSchema.java

   An interface defining constants to be used by the volume code.
   
   Created: 23 April 1998
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
 * An interface defining constants to be used by the volume code.
 *
 */

public interface volumeSchema {

  // field id's for the volume object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the volume, you'll want to change
  // this file to match.

  final static short LABEL=256;
  final static short HOST=257;
  final static short PATH=258;
  final static short ENTRIES=259;
  final static short MOUNTOPTIONS=260;
}
