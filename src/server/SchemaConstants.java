/*

   SchemaConstants.java

   This file defines known constants for pre-defined Schema object
   types, fields, and certain pre-defined objects that the server
   depends on for administering default permissions.
   
   Created: 21 July 1997
   Release: $Name:  $
   Version: $Revision: 1.27 $
   Last Mod Date: $Date: 2000/11/23 02:35:53 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 SchemaConstants

------------------------------------------------------------------------------*/

/**
 * <p>This file defines known constants for pre-defined Schema object
 * types, fields, and certain pre-defined objects that the server
 * depends on for administering default permissions.</p>
 *
 * <p>
 * <h3>Fixed object types used for the server's operations</h3>
 * <table border>
 * <tr>
 * <th>Object Name</th>
 * <th>Object Type ID</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>Owner Group</td>
 * <td>0</td>
 * <td>An association between a collection of admin personae and the collection of
 * objects they own.</td>
 * </tr>
 * <tr>
 * <td>Admin Persona</td>
 * <td>1</td>
 * <td>A record of privileges and owner groups that can be associated
 * with a user.</td>
 * </tr>
 * <tr>
 * <td>Role</td>
 * <td>2</td>
 * <td>A record of permissions by object and field type.  Associated with admin personae.</td>
 * </tr>
 * <tr>
 * <td>User</td>
 * <td>3</td>
 * <td>A person who can log in to Ganymede.</td>
 * </tr>
 * <tr>
 * <td>System Event</td>
 * <td>4</td>
 * <td>A type of general system event that can be logged and for which mail can be sent to a list of addresses
 * when it occurs.</td>
 * </tr>
 * <tr>
 * <td>Task</td>
 * <td>5</td>
 * <td>A schedulable work routine.</td>
 * </tr>
 * <tr>
 * <td>Object Event</td>
 * <td>6</td>
 * <td>A kind of event that can occur in relation to a specific database object, which can be logged
 * and for which mail can be sent to a list of addresses.</td>
 * </tr>
 * </table>
 * </p>
 *
 * <p>
 * <h3>Common fields present in all DBObjects</h3>
 * <table border>
 * <tr>
 * <th>Field Name</th>
 * <th>Field ID</th>
 * <th>Field Type</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>OwnerListField</td>
 * <td>0</td>
 * <td>{@link arlut.csd.ganymede.InvidDBField InvidDBField}</td>
 * <td>List of owner groups with authority over this object</td>
 * </tr>
 * <tr>
 * <td>ExpirationField</td>
 * <td>1</td>
 * <td>{@link arlut.csd.ganymede.DateDBField DateDBField}</td>
 * <td>date that this object will expire</td>
 * </tr>
 * <tr>
 * <td>RemovalField</td>
 * <td>2</td>
 * <td>{@link arlut.csd.ganymede.DateDBField DateDBField}</td>
 * <td>date that this object will be removed</td>
 * </tr>
 * <tr>
 * <td>NotesField</td>
 * <td>3</td>
 * <td>{@link arlut.csd.ganymede.StringDBField StringDBField}</td>
 * <td>string field for notes for this object</td>
 * </tr>
 * <tr>
 * <td>CreationDateField</td>
 * <td>4</td>
 * <td>{@link arlut.csd.ganymede.DateDBField DateDBField}</td>
 * <td>date that this object was created</td>
 * </tr>
 * <tr>
 * <td>CreatorField</td>
 * <td>5</td>
 * <td>{@link arlut.csd.ganymede.StringDBField StringDBField}</td>
 * <td>string describing the creator of this object</td>
 * </tr>
 * <tr>
 * <td>ModificationDateField</td>
 * <td>6</td>
 * <td>{@link arlut.csd.ganymede.DateDBField DateDBField}</td>
 * <td>date that this object was last modified</td>
 * </tr>
 * <tr>
 * <td>ModifierField</td>
 * <td>7</td>
 * <td>{@link arlut.csd.ganymede.StringDBField StringDBField}</td>
 * <td>string describing the administrator who last modified this object</td>
 * </tr>
 * <tr>
 * <td>BackLinksField</td>
 * <td>8</td>
 * <td>{@link arlut.csd.ganymede.InvidDBField InvidDBField}</td>
 * <td>any miscellaneous pointers pointing to us are kept track of in
 * this field if we don't have an explicit back-link field set</td>
 * </tr>
 * </table>
 * </p>
 *
 * <p>This file contains lots of other constants for fields in the 7 mandatory
 * object types, but I'm tired of typing, so I'm not going to try to document
 * them in the javadoc class header right now.</p>
 */

public interface SchemaConstants {

  //
  //  /* all non-embedded objects have these fields */
  //

  /** list of owner groups with authority over this object */
  final static short OwnerListField = 0; 

  /** date that this object will expire */
  final static short ExpirationField = 1;

  /** date that this object will be removed. */
  final static short RemovalField = 2; 

  /** string field for notes for this object */
  final static short NotesField = 3; 

  /** date that this object was created */
  final static short CreationDateField = 4;

  /** string describing the creator of this object */
  final static short CreatorField = 5; 

  /** date that this object was last modified */
  final static short ModificationDateField = 6;

  /** string describing the administrator who last modified this object */
  final static short ModifierField = 7;	

  /**
   * any miscellaneous pointers pointing to us are kept track of in
   * this field if we don't have an explicit back-link field 
   */
  final static short BackLinksField = 8; 

  /**
   * in case we add more system fields to objects
   */

  final static short FinalSystemField = 8;
  //
  //  /* all embedded objects have these fields */
  //

  final static short ContainerField = 0; // Invid pointer to object 'containing' us.

  //
  //  /* ownerGroup base */
  //

  /** all objects are owned by objects of type OwnerBase */
  final static short OwnerBase = 0; 

  /** name of this owner set */
  final static short OwnerNameField = 100; 

  /** what admin:role entities have privileges in this OwnerBase? */
  final static short OwnerMembersField = 101;

  /** what objects does this owner set own? */
  final static short OwnerObjectsOwned = 102; 

  /**
   * If true, all mail sent in care of this owner group will be
   * distributed to all admins on the list automatically
   */
  final static short OwnerCcAdmins = 104; 

  /** what external email addresses should be notified if objects owned change? */
  final static short OwnerExternalMail = 105;

  /** the following is a fixed object id's */
  final static short OwnerSupergash = 1;

  //
  //  /* Admin Personae */
  //

  /** an administrator privilege record */
  final static short PersonaBase = 1; 

  /** what is this descriptive tag for this persona, not including the user name? */
  final static short PersonaNameField = 100; 

  /** password for this admin account */
  final static short PersonaPasswordField = 101; 

  /** list of owner groups that this admin is a member of */
  final static short PersonaGroupsField = 102; 

  /** if associated with a user, here's the reference. */
  final static short PersonaAssocUser = 103; 

  /** vector of permission invids for this admin account */
  final static short PersonaPrivs = 104; 

  /** boolean, does this role have access to the admin console? */
  final static short PersonaAdminConsole = 105; 

  /** boolean, does this role have *full* access to the admin console? */
  final static short PersonaAdminPower = 106; 

  /** 
   * string, the mail address for this administrator, if not implicitly
   * extracted from their user name.
   */
  final static short PersonaMailAddr = 107; 

  /**
   * string, a hidden label field.  Not usually directly editable, derived from
   * PersonaNameField and PersonaAssocUser.
   */
  final static short PersonaLabelField = 108;


  /** Object number for a pre-defined object used by the server */
  final static short PersonaSupergashObj = 1;

  /** Object number for a pre-defined object used by the server */
  final static short PersonaMonitorObj = 2;

  //
  // /* Role */
  //

  /** 
   * this base contains a set of permission bits constraining the
   * admin personae's power 
   */

  final static short RoleBase = 2; 

  /** The name of this Role (String) */
  final static short RoleName = 100;

  /** permissions applying to objects owned by admin personae */
  final static short RoleMatrix = 101; 

  /** what admin personae are using this role? */
  final static short RolePersonae = 102; 

  /** permissions applying to all objects not owned by the relevant personae */
  final static short RoleDefaultMatrix = 103; 

  /** can this role be passed on to admins created by admins owning this role? */
  final static short RoleDelegatable = 104;

  /** Object number for a pre-defined object used by the server */
  final static short RoleDefaultObj = 1;

  //
  // /* User */
  //

  /* users have a defined set of fields */
  final static short UserBase = 3;

  /** username string */
  final static short UserUserName = 100;

  /** password */
  final static short UserPassword = 101;

  /** pointer to zero or more associated admin personae */
  final static short UserAdminPersonae = 102; 

  //
  //  /* mail/log event classes */
  //

  final static short EventBase = 4;

  /** single-word token for this event class (String field) */
  final static short EventToken = 100;

  /**
   * Short name for this event class, suitable for an email message
   * title (String field) 
   */
  final static short EventName = 101;
  
  /**
   * fuller description of this event class, suitable for an email
   * body (String field)
   */
  final static short EventDescription = 102; 

  /** if true, events of this type should be mailed (Boolean field) */
  final static short EventMailBoolean = 103; 

  /**
   * if true, the admin performing the action will get a copy of any
   * mail (Boolean field) 
   */
  final static short EventMailToSelf = 105;

  /**
   * if true, the owner groups owning the objects referenced in this
   * event will get a copy of any mail (Boolean field) 
   */
  final static short EventMailOwners = 106;

  /**
   * A list of external email addresses to send mail for this event to.
   *
   * That is, addresses in raw string form, rather than as an invid reference
   * to a user or email list recorded in Ganymede. (String field)
   */
  final static short EventExternalMail = 107;

  //
  //  /* mail/log event classes */
  //

  final static short ObjectEventBase = 6;

  /**
   * single-word token for this event class (String field)
   */
  final static short ObjectEventToken = 100;

  /**
   * Short name for this event class, suitable for an email message
   * title (String field) 
   */
  final static short ObjectEventName = 101;

  /**
   * fuller description of this event class, suitable for an email
   * body (String field) 
   */
  final static short ObjectEventDescription = 102; 

  /**
   * if true, the admin performing the action will get a copy of any
   * mail (Boolean field) 
   */
  final static short ObjectEventMailToSelf = 105;

  /**
   * the name of the object type that this event category applies to 
   */
  final static short ObjectEventObjectName = 106;

  /**
   * if true, the owner groups owning objects affected by this event
   * will get a copy of the mail 
   */
  final static short ObjectEventMailOwners = 107;

  /**
   * the short id of the object type that this event category applies
   * to 
   */
  final static short ObjectEventObjectType = 108;

  /**
   * A list of external email addresses to send mail for this event to.
   *
   * That is, addresses in raw string form, rather than as an invid reference
   * to a user or email list recorded in Ganymede. (String field)
   */
  final static short ObjectEventExternalMail = 109;

  //  
  //  /* builder classes */
  //

  /**
   * Records of task classes we want to attach to the server
   */
  final static short TaskBase = 5;

  /**
   * name of this task (i.e., DNSBuilder, NISBuilder)
   */
  final static short TaskName = 100; 

  /**
   * what is the fully qualified classname for this builder task?
   */
  final static short TaskClass = 101;

  /**
   * boolean: should this task be scheduled for execution when a transaction is committed
   */
  final static short TaskRunOnCommit = 102;

  /**
   * boolean: should this task be scheduled for execution at regular intevals?
   */
  final static short TaskRunPeriodically = 103;

  /**
   * string: what is the inteval unit for periodic execution
   */
  final static short TaskPeriodUnit = 104;

  /**
   * numeric: how many period units between task execution?
   */
  final static short TaskPeriodCount = 105;

  /**
   * date: anchor point used to determine time of day, week, etc.
   */
  final static short TaskPeriodAnchor = 106;

  /**
   * string: vector of option strings that can be interpreted by
   * a given builder task
   */
  final static short TaskOptionStrings = 107;

  /**
   * what's the last base we currently have defined as a mandatory base? 
   */
  final static short FinalBase = ObjectEventBase;
}
