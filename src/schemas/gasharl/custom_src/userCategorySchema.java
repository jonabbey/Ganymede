/*

   userCategorySchema.java

   An interface defining constants to be used by the user code.
   
   Created: 7 August 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                              userCategorySchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface userCategorySchema {

  // field id's for the user object.  These should match the
  // current specs in the Ganymede schema file precisely.  If
  // you change the schema for the user, you'll want to change
  // this file to match.

  final static short TITLE=256;
  final static short SUMMARY=263;
  final static short DESCRIPTION=261;
  final static short APPROVALREQ=259;
  final static short APPROVALLIST=260;
  final static short EXPIRE=257;
  final static short LIMIT=258;
  final static short SSREQUIRED=262;
}
