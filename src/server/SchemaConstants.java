/*

   SchemaConstants.java

   This file defines known constants for pre-defined Schema object
   types and fields.
   
   Created: 21 July 1997
   Version: $Revision: 1.3 $ %D%
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
  final static short NotesField = 3; // string field for notes for this object
  final static short CreationDateField = 4; // date that this object was created
  final static short CreatorField = 5; // string describing the creator of this object
  final static short ModificationDateField = 6;	// date that this object was last modified
  final static short ModifierField = 7;	// string describing the administrator who last modified this object

  /* all embedded objects have these fields */

  final static short ContainerField = 0; // Invid pointer to object 'containing' us.

  /* administrators have a defined set of fields */

  final static short AdminBase = 0; // an administrator account

  final static short AdminGroupField = 100; // boolean, true if this is an admin group 
  final static short AdminNameField = 101; // name of this admin account, null if associated with a user
  final static short AdminPasswordField = 102; // password for this admin account
  final static short AdminMembersField = 103; // members of this admin account if an admin group
  final static short AdminGroupsField = 104; // list of admin groups that this admin is a member of
  final static short AdminAssocUser = 105; // if associated with a user, here's the reference.
  final static short AdminPrivs = 106; // vector of permission invids for this admin account
  final static short AdminObjectsOwned = 107; // list of objects owned by this admin account

  /* permission */

  final static short PermBase = 1;

  final static short PermName = 100;
  final static short PermMatrix = 101;

  /* users have a defined set of fields */

  final static short UserBase = 2;

  final static short UserUserName = 100; // username string
  final static short UserPassword = 101; // password
  final static short UserAdminRole = 102; // pointer to associated admin account, if any

}
