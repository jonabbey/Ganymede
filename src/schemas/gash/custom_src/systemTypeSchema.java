/*

   systemTypeSchema.java

   An interface defining constants to be used by the system type code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                systemTypeSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the system type code.
 *
 */

public interface systemTypeSchema {

  // field id's for the system type object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the system type, you'll want to change
  // this file to match.

  final static short SYSTEMTYPE=256;
  final static short STARTIP=257;
  final static short STOPIP=258;
  final static short USERREQ=259;
}
