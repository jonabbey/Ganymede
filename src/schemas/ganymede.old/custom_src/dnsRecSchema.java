/*

   dnsRecSchema.java

   An interface defining constants to be used by the dns code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    dnsRecSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the dns code.
 *
 */

public interface dnsRecSchema {

  // field id's for the dns object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the dns, you'll want to change
  // this file to match.

  final static short DNSDOMAIN=256;
  final static short NAME=257;
  final static short ALIASES=258;
}
