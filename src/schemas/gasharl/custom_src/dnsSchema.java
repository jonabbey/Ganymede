/*

   dnsSchema.java

   An interface defining constants to be used by the dns domain code.
   
   Created: 21 May 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       dnsSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the network code.
 *
 */

public interface dnsSchema {

  final static short SYSTEMS=256;
  final static short DOMAINNAME=257;
  final static short MX=259;
}
