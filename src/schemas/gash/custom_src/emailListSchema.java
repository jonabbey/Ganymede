/*

   emailListSchema.java

   An interface defining constants to be used by the email list code.
   
   Created: 23 April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 emailListSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the email list code.
 *
 */

public interface emailListSchema {

  // field id's for the email list object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the email list, you'll want to change
  // this file to match.

  final static short LISTNAME=256;
  final static short MEMBERS=257;
  final static short EXTERNALTARGETS=258;
}
