/*

   ipSchema.java

   An interface defining constants to be used by the ip code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                        ipSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the ip code.
 *
 */

public interface ipSchema {

  // field id's for the ip object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the ip, you'll want to change
  // this file to match.

  final static short DNSRECORDS=256;
  final static short ADDRESS=257;
  final static short IPNET=258;
}
