/*

   networkSchema.java

   An interface defining constants to be used by the network code.
   
   Created: 16 May 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                   networkSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the network code.
 *
 */

public interface networkSchema {

  final static short NETNUMBER=256;
  final static short BROADCAST=257;
  final static short NETMASK=258;
  final static short NAMESERVER=259;
  final static short DEFAULTROUTER=260;
  final static short NAME=261;
  final static short IPV6OK=262;
  final static short INTERFACES=263;
  final static short ROOMS=264;
}
