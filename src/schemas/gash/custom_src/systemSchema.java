/*

   systemSchema.java

   An interface defining constants to be used by the system code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    systemSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the system code.
 *
 */

public interface systemSchema {

  // field id's for the system object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the system, you'll want to change
  // this file to match.

  final static short OS=256;
  final static short MANUFACTURER=257;
  final static short MODEL=258;
  final static short INTERFACES=260;
  final static short SYSTEMNAME=261;
  final static short SYSTEMALIASES=262;
  final static short DNSDOMAIN=263;
  final static short ROOM=264;
  final static short NETGROUPS=265;
  final static short SYSTEMTYPE=266;
  final static short PRIMARYUSER=267;
  final static short VOLUMES=268;
}
