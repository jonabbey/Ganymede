/*

   SchemaConstants.java

   This file defines known constants for pre-defined Schema object
   types and fields.
   
   Created: 21 July 1997
   Version: $Revision: 1.4 $ %D%
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

  /* owner base */

  final static short OwnerBase = 0; // all objects are owned by objects of type OwnerBase

  final static short OwnerNameField = 100; // name of this owner set
  final static short OwnerMembersField = 101; // what admin:role entities have privileges in this OwnerBase?
  final static short OwnerObjectsOwned = 102; // what objects does this owner set own?

  /* administrator roles have a defined set of fields */

  final static short PersonaBase = 1; // an administrator privilege record

  final static short PersonaNameField = 100; // what is this persona called?
  final static short PersonaPasswordField = 101; // password for this admin account
  final static short PersonaGroupsField = 102; // list of admin groups that this admin is a member of
  final static short PersonaAssocUser = 103; // if associated with a user, here's the reference.
  final static short PersonaPrivs = 104; // vector of permission invids for this admin account
  final static short PersonaAdminConsole = 105; // boolean, does this role have access to the admin console?
  final static short PersonaAdminPower = 106; // boolean, does this role have *full* access to the admin console?

  /* permission */

  final static short PermBase = 2; // this base contains a set of permission bits constraining the admin personae's power

  final static short PermName = 100;
  final static short PermMatrix = 101;
  final static short PermPersonae = 102; // what admin personae are using this priv matrix?

  /* users have a defined set of fields */

  final static short UserBase = 3;

  final static short UserUserName = 100; // username string
  final static short UserPassword = 101; // password
  final static short UserAdminPersonae = 102; // pointer to zero or more associated admin personae

}
