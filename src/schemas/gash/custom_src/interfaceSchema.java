/*

   interfaceSchema.java

   An interface defining constants to be used by the interface code.
   
   Created: 23 April 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    interfaceSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the interface code.
 *
 */

public interface interfaceSchema {

  // field id's for the interface object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the interface, you'll want to change
  // this file to match.

  final static short ETHERNETINFO=256;
  final static short NAME=259;
  final static short ALIASES=260;
  final static short IPNET=261;
  final static short ADDRESS=258;
}
