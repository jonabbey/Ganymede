/*

   emailRedirectSchema.java

   An interface defining constants to be used by the email redirect code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                             emailRedirectSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the email redirect code.
 *
 */

public interface emailRedirectSchema {

  // field id's for the email redirect object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the email redirect, you'll want to change
  // this file to match.

  final static short NAME=256;
  final static short TARGETS=257;
  final static short ALIASES=258;
}
