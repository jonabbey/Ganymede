/*

   SchemaConstants.java

   This file defines known constants for pre-defined Schema object
   types and fields.
   
   Created: 21 July 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 SchemaConstants

------------------------------------------------------------------------------*/

/**
 *
 * This file defines known constants for pre-defined Schema object
 * types and fields.
 *
 */

public interface SchemaConstants {

  /* all non-embedded objects have these fields */

  final static short OwnerListField = 0; // list of admin accounts with authority over this object
  final static short ExpirationField = 1; // date that this object will expire
  final static short RemovalField = 2; // date that this object will be removed.

  /* all embedded objects have these fields */

  final static short ContainerField = 0; // Invid pointer to object 'containing' us.

  /* administrators have a defined set of fields */

  final static short AdminBase = 0; // an administrator account

  final static short AdminGroupField = 3; // boolean, true if this is an admin group 
  final static short AdminNameField = 4; // name of this admin account, null if associated with a user
  final static short AdminPasswordField = 5; // password for this admin account
  final static short AdminMembersField = 6; // members of this admin account if an admin group
  final static short AdminGroupsField = 7; // list of admin groups that this admin is a member of
  final static short AdminAssocUser = 8; // if associated with a user, here's the reference.
  final static short AdminPrivs = 9; // vector of permission invids for this admin account
  final static short AdminObjectsOwned = 10; // list of objects owned by this admin account

  /* permission */

  final static short PermBase = 1;

  final static short PermName = 3;
  final static short PermMatrix = 4;

  /* users have a defined set of fields */

  final static short UserBase = 2;

  final static short UserUserName = 3; // username string
  final static short UserPassword = 4; // password
  final static short UserAdminRole = 5;	// pointer to associated admin account, if any
}
