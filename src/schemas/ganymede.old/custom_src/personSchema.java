/*

   personSchema.java

   An interface defining constants to be used by the user code.
   
   Created: 25 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    personSchema

------------------------------------------------------------------------------*/

/**
 *
 * An interface defining constants to be used by the user code.
 *
 */

public interface personSchema {

  // field id's for the person object.  These should match the current
  // specs in the Ganymede schema file precisely.  If you change the
  // schema for the person, you'll want to change this file to match.

  final static short LASTNAME=257;
  final static short FIRSTNAME=256;
  final static short EMPLOYEETYPE=265;
  final static short BADGE=267;
  final static short DIVISION=261;
  final static short ROOM=260;
  final static short HOMEPAGE=268;
  final static short OFFICEPHONE=258;
  final static short HOMEPHONE=259;
  final static short CELLPHONE=263;
  final static short FAX=266;
  final static short PAGER=262;
  final static short ACCOUNTS=264;
}
