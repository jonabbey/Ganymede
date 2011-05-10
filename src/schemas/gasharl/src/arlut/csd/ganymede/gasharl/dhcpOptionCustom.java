/*

   dhcpOptionCustom.java

   This file is a management class for DHCP Option objects in Ganymede.
   
   Created: 8 October 2007

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2011
   The University of Texas at Austin

   Contact information

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.GanyParseException;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.BooleanDBField;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.IPDBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.InvidDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                dhcpOptionCustom

------------------------------------------------------------------------------*/

public class dhcpOptionCustom extends DBEditObject implements SchemaConstants, dhcpOptionSchema {

  private final static boolean debug = false;

  /**
   * This List of Strings represent the options innately recognized by
   * our DHCP server without having to declare a custom option.
   */

  private static List<String> supportedOptions = Arrays.asList
    (
     "all-subnets-local",
     "arp-cache-timeout",
     "boot-size",
     "bootfile-name",
     "broadcast-address",
     "cookie-servers",
     "default-ip-ttl",
     "default-tcp-ttl",
     "dhcp-client-identifier",
     "dhcp-lease-time",
     "dhcp-max-message-size",
     "dhcp-message",
     "dhcp-message-type",
     "dhcp-option-overload",
     "dhcp-parameter-request-list",
     "dhcp-rebinding-time",
     "dhcp-renewal-time",
     "dhcp-requested-address",
     "dhcp-server-identifier",
     "domain-name",
     "domain-name-servers",
     "extensions-path",
     "ien116-name-servers",
     "interface-mtu",
     "ip-forwarding",
     "irc-server",
     "log-servers",
     "lpr-servers",
     "mask-supplier",
     "max-dgram-reassembly",
     "merit-dump",
     "mobile-ip-home-agent",
     "netbios-name-servers",
     "netbios-node-type",
     "nis-domain",
     "nis-servers",
     "nntp-server",
     "non-local-source-routing",
     "ntp-servers",
     "perform-mask-discovery",
     "policy-filter",
     "pop-server",
     "resource-location-servers",
     "root-path",
     "router-discovery",
     "router-solicitation-address",
     "routers",
     "smtp-server",
     "static-routes",
     "subnet-mask",
     "subnet-selection",
     "swap-server",
     "tcp-keepalive-garbage",
     "tcp-keepalive-interval",
     "tftp-server-name",
     "time-offset",
     "time-servers",
     "trailer-encapsulation",
     "uap-servers",
     "user-class",
     "vendor-class-identifier",
     "vendor-encapsulated-options",
     "www-server",
     "x-display-manager"
     );

  /**
   * This List of Strings represent the options innately recognized by
   * our DHCP server as naked keywords without having to use the
   * option token.
   */

  private static List<String> builtInOptions = Arrays.asList
    (
     "default-lease-time",
     "filename",
     "max-lease-time",
     "next-server"
     );

  private static QueryResult result = new QueryResult(true);

  static
  {
    result.addRow("flag");
    result.addRow("uint8");
    result.addRow("int8");
    result.addRow("uint16");
    result.addRow("int16");
    result.addRow("uint32");
    result.addRow("int32");
    result.addRow("string");
    result.addRow("text");
    result.addRow("ip-address");
    result.addRow("array of ip-address");
    result.setNonEditable();
  }

  private static Pattern flagRegex = Pattern.compile("^true|false$", Pattern.CASE_INSENSITIVE);
  private static Pattern hostNameRegex = Pattern.compile("^[a-zA-Z\\d]+[a-zA-Z\\d\\-]*$", Pattern.CASE_INSENSITIVE);
  private static Pattern arlDomainNameRegex = Pattern.compile("^([a-zA-Z\\d]+[a-zA-Z\\d\\-]*)\\.arlut\\.utexas\\.edu$", Pattern.CASE_INSENSITIVE);

  private static long maxUByte = 255;
  private static long maxUShort = 65535;
  private static long maxUInt = 4294967295L;

  /**
   *
   * Customization Constructor
   *
   */

  public dhcpOptionCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public dhcpOptionCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public dhcpOptionCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.  If this DBEditObject is an embedded type, it will
   * have been linked into its parent object before this method
   * is called.</p>
   *
   * <p>This method is responsible for filling in any default
   * values that can be calculated from the
   * {@link arlut.csd.ganymede.server.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.server.DBSession#createDBObject(short, arlut.csd.ganymede.common.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure code, the
   * calling method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.</p>
   *
   * <p>If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.</p>
   *
   * <p>This method should be overridden in subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal initializeNewObject()
  {
    // we don't want to do any of this initialization during
    // bulk-loading.

    if (!getGSession().enableOversight)
      {
	return null;
      }

    BooleanDBField custom = (BooleanDBField) getField(dhcpOptionSchema.CUSTOMOPTION);

    return custom.setValueLocal(Boolean.TRUE);
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case dhcpOptionSchema.OPTIONNAME:
      case dhcpOptionSchema.OPTIONTYPE:
        return true;

      case dhcpOptionSchema.CUSTOMCODE:
	return object.isSet(dhcpOptionSchema.CUSTOMOPTION);
      }

    return false;
  }

  /**
   * <p>Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of 
   * {@link arlut.csd.ganymede.server.DBField DBField} will
   * wind up calling up to here to let us override the normal visibility
   * process.</p>
   *
   * <p>Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.</p>
   *
   * <p>If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    if (field.getID() == dhcpOptionSchema.CUSTOMOPTION)
      {
        return !field.getOwner().isSet(dhcpOptionSchema.BUILTIN);
      }

    if (field.getID() == dhcpOptionSchema.CUSTOMCODE || field.getID() == dhcpOptionSchema.FORCESEND)
      {
        return !field.getOwner().isSet(dhcpOptionSchema.BUILTIN) &&
          field.getOwner().isSet(dhcpOptionSchema.CUSTOMOPTION);
      }

    return super.canSeeField(session, field);
  }

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == volumeSchema.ENTRIES)
      {
	return null;
      }

    return super.obtainChoicesKey(field);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses,
   * particularly if you have a StringDBField that you want to force
   * to pick from the list of choices provided by your DBEditObject
   * subclass' obtainChoiceList() method.</p>
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == dhcpOptionSchema.OPTIONTYPE)
      {
	return true;
      }

    return super.mustChoose(field);
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields.</p>
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() == dhcpOptionSchema.OPTIONTYPE)
      {
        return result;
      }

    return super.obtainChoiceList(field);
  }

  /**
   * <p>This method is used to control whether or not it is acceptable to
   * make a link to the given field in this 
   * {@link arlut.csd.ganymede.server.DBObject DBObject} type when the
   * user only has editing access for the source
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} and not
   * the target.</p>
   *
   * <p>See {@link arlut.csd.ganymede.server.DBEditObject#anonymousLinkOK(arlut.csd.ganymede.server.DBObject,short,
   * arlut.csd.ganymede.server.DBObject,short,arlut.csd.ganymede.server.GanymedeSession)
   * anonymousLinkOK(obj,short,obj,short,GanymedeSession)} for details on
   * anonymousLinkOK() method chaining.</p>
   *
   * <p>Note that the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)
   * choiceListHasExceptions()} method will call this version of anonymousLinkOK()
   * with a null targetObject, to determine that the client should not
   * use its cache for an InvidDBField's choices.  Any overriding done
   * of this method must be able to handle a null targetObject, or else
   * an exception will be thrown inappropriately.</p>
   *
   * <p>The only reason to consult targetObject in any case is to
   * allow or disallow anonymous object linking to a field based on
   * the current state of the target object.  If you are just writing
   * generic anonymous linking rules for a field in this object type,
   * targetObject won't concern you anyway.  If you do care about the
   * targetObject's state, though, you have to be prepared to handle
   * a null valued targetObject.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param targetObject The object that the link is to be created in (may be null)
   * @param targetFieldID The field that the link is to be created in
   */

  public boolean anonymousLinkOK(DBObject targetObject, short targetFieldID)
  {
    return true;                // anybody can link to us, we don't care.
  }

  /**
   * <p>Customization method to verify overall consistency of
   * a DBObject.  This method is intended to be overridden
   * in DBEditObject subclasses, and will be called by
   * {@link arlut.csd.ganymede.server.DBEditObject#commitPhase1() commitPhase1()}
   * to verify the readiness of this object for commit.  The
   * DBObject passed to this method will be a DBEditObject,
   * complete with that object's GanymedeSession reference
   * if this method is called during transaction commit, and
   * that session reference may be used by the verifying code if
   * the code needs to access the database.</p>
   *
   * <p>This method is for custom checks specific to custom DBEditObject
   * subclasses.  Standard checking for missing fields for which
   * fieldRequired() returns true is done by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_checkObjectMissingFields(arlut.csd.ganymede.server.DBEditObject)}
   * during {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_handlePhase1()}.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal consistencyCheck(DBObject object)
  {
    String name = (String) object.getFieldValueLocal(dhcpOptionSchema.OPTIONNAME);
    boolean builtIn = object.isSet(dhcpOptionSchema.BUILTIN);

    if (builtIn && !builtInOptions.contains(name))
      {
	return Ganymede.createErrorDialog("Option name " + name + " is not a supported built-in keyword in our DHCP server.");
      }

    boolean custom = object.isSet(dhcpOptionSchema.CUSTOMOPTION);

    if (!custom && !supportedOptions.contains(name) && !builtInOptions.contains(name))
      {
	return Ganymede.createErrorDialog("Option name " + name + " is not a recognized standard option supported by our DHCP server.  You will need to set this as a custom option with a numeric custom option code.");
      }

    return null;
  }

  /**
   * <p>This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.server.GanymediatorWizard wizards} when a field's
   * value is being changed.</p>
   *
   * <p>Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.</p>
   *
   * <p>In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.</p>
   *
   * <p>If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean) 
   * enableOversight(false)},
   * this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.</p>
   *
   * <p>This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.</p>
   *
   * <p>This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.</p>
   *
   * <p>The operation parameter will be a small integer, and should hold one of the
   * following values:</p>
   *
   * <dl>
   * <dt>1 - SETVAL</dt>
   * <dd>This operation is used whenever a simple scalar field is having
   * it's value set.  param1 will be the value being placed into the field.</dd>
   * <dt>2 - SETELEMENT</dt>
   * <dd>This operation is used whenever a value in a vector field is being
   * set.  param1 will be an Integer holding the element index, and
   * param2 will be the value being set.</dd>
   * <dt>3 - ADDELEMENT</dt>
   * <dd>This operation is used whenever a value is being added to the
   * end of a vector field.  param1 will be the value being added.</dd>
   * <dt>4 - DELELEMENT</dt>
   * <dd>This operation is used whenever a value in a vector field is being
   * deleted.  param1 will be an Integer holding the element index.</dd>
   * <dt>5 - ADDELEMENTS</dt>
   * <dd>This operation is used whenever a set of elements is being
   * added to a vector field en masse.  param1 will be a Vector containing
   * the values that are being added.</dd>
   * <dt>6 - DELELEMENTS</dt>
   * <dd>This operation is used whenever a set of elements is being
   * deleted from a vector field en masse.  param1 will be a Vector containing
   * the values that are being deleted.</dd>
   * <dt>7 - SETPASSPLAIN</dt>
   * <dd>This operation is used when a password field is having its password
   * set using a plaintext source.  param1 will be a String containing the
   * submitted password, or null if the password is being cleared.</dd>
   * <dt>8 - SETPASSCRYPT</dt>
   * <dd>This operation is used when a password field is having its password
   * set using a UNIX crypt() hashed source.  param1 will be a String containing the
   * submitted hashed password, or null if the password is being cleared.</dd>
   * <dt>9 - SETPASSMD5</dt>
   * <dd>This operation is used when a password field is having its password
   * set using an md5Ccrypt() hashed source.  param1 will be a String containing the
   * submitted hashed password, or null if the password is being cleared.</dd>
   * <dt>10 - SETPASSWINHASHES</dt>
   * <dd>This operation is used when a password field is having its password
   * set using Windows style password hashes.  param1 will be the password in
   * LANMAN hash form, param2 will be the password in NT Unicode MD4 hash
   * form.  Either or both of param1 and param2 may be null.</dd>
   * <dt>11 - SETPASSSSHA</dt>
   * <dd>This operation is used when a password field is having its
   * password set using the OpenLDAP-style SSHA password hash.  param1
   * will be the password in SSHA form, or null if the password is
   * being cleared.  param2 will be null.</dd>
   * </dl>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @return null if the operation is approved without comment, or a
   * ReturnVal object indicating success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing
   * flag that will indicate to the field code whether or not the
   * operation should continue to completion using the field's
   * standard logic.  <b>It is very important that wizardHook return a
   * new ReturnVal(true, true) if the wizardHook wishes to simply
   * specify rescan information while having the field perform its
   * standard operation.</b> wizardHook() may return new
   * ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true
   * for the respond() method in GanymediatorWizard subclasses.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    // if the BUILTIN check box is toggled, we'll need to refresh the
    // visibility of the CUSTOMOPTION and CUSTOMCODE fields.

    if (operation == DBEditObject.SETVAL)
      {
        if (field.getID() == dhcpOptionSchema.BUILTIN)
          {
            if (Boolean.TRUE.equals(param1))
              {
                ReturnVal innerRetVal = null;
                DBEditObject parent = (DBEditObject) field.getOwner();

		String name = (String) parent.getFieldValueLocal(dhcpOptionSchema.OPTIONNAME);

		if (name != null && !builtInOptions.contains(name))
		  {
		    return Ganymede.createErrorDialog("Option name " + name + " not supported as a 'built-in' option type in dhcpd server.");
		  }

                innerRetVal = parent.setFieldValueLocal(dhcpOptionSchema.CUSTOMOPTION, Boolean.FALSE);

                if (!ReturnVal.didSucceed(innerRetVal))
                  {
                    return innerRetVal;
                  }

                innerRetVal = parent.setFieldValueLocal(dhcpOptionSchema.CUSTOMOPTION, null);

                if (!ReturnVal.didSucceed(innerRetVal))
                  {
                    return innerRetVal;
                  }
              }

            ReturnVal result = new ReturnVal(true, true);
            result.addRescanField(field.getOwner().getInvid(), dhcpOptionSchema.CUSTOMOPTION);
            result.addRescanField(field.getOwner().getInvid(), dhcpOptionSchema.CUSTOMCODE);
            result.addRescanField(field.getOwner().getInvid(), dhcpOptionSchema.FORCESEND);

            return result;
          }

        // if the CUSTOMOPTION check box is toggled, we'll need to refresh
        // the visibility of the CUSTOMCODE and FORCESEND fields.

        if (field.getID() == dhcpOptionSchema.CUSTOMOPTION)
          {
            ReturnVal result = new ReturnVal(true, true);
            result.addRescanField(field.getOwner().getInvid(), dhcpOptionSchema.CUSTOMCODE);
            result.addRescanField(field.getOwner().getInvid(), dhcpOptionSchema.FORCESEND);

            return result;
          }
      }

    return super.wizardHook(field, operation, param1, param2);
  }

  /**
   * <p>This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.</p>
   *
   * <p>Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    if (field.getID() == dhcpOptionSchema.OPTIONNAME)
      {
        String valueStr = (String) value;

	if (valueStr == null)
	  {
	    return null;
	  }

        if (valueStr.startsWith("dhcp.") ||
            valueStr.startsWith("server.") ||
            valueStr.startsWith("agent.") ||
            valueStr.startsWith("nwip.") ||
            valueStr.startsWith("fqdn."))
          {
            return Ganymede.createErrorDialog("We do not currently support the use of the built-in option spaces.");
          }
      }

    return super.verifyNewValue(field,value);
  }

  /**
   * <p>This method is used to verify the value being set on a
   * dhcpEntryCustom object that points to this dhcpOptionCustom
   * object.</p>
   *
   * <p>We'll validate the value against the type restriction set in this
   * dhcpOptionCustom object.</p>
   *
   * <p>Returns null on approved value without modification, a ReturnVal
   * with an encoded error if the value wasn't acceptable and could
   * not be canonicalized, or a ReturnVal encoding success with a
   * transformed value if the input could be canonicalized.</p>
   *
   * @param object A reference to the DBObject representing the DHCP
   * option type that this value is being verified as suitable for.
   * @param value The value that is being set in the dhcp entry's
   * value field.
   */

  public static ReturnVal verifyAcceptableValue(DBObject object, String value)
  {
    if (value == null)
      {
        return null;
      }

    String currentType = (String) object.getFieldValueLocal(dhcpOptionSchema.OPTIONTYPE);

    if (currentType == null)
      {
        return null;           // we have no type, go ahead and pass it through for now
      }

    if (currentType.equals("flag"))
      {
        if (!flagRegex.matcher(value).matches())
          {
            return Ganymede.createErrorDialog("Unacceptable value",
                                              "The only acceptable values for this field are 'true' and 'false'.");
          }
        else
          {
            if (value.equals(value.toLowerCase()))
              {
                return null;
              }

            ReturnVal retVal = new ReturnVal(true, true);
            retVal.setTransformedValueObject(value.toLowerCase());

            return retVal;
          }
      }
    else if (currentType.equals("uint8") ||
             currentType.equals("int8") ||
             currentType.equals("uint16") ||
             currentType.equals("int16") ||
             currentType.equals("uint32") ||
             currentType.equals("int32"))
      {
        long longValue = -1;

        try
          {
            longValue = Long.parseLong(value);
          }
        catch (NumberFormatException ex)
          {
            return Ganymede.createErrorDialog("Unacceptable value",
                                              "This dhcp option requires a numeric parameter.");
          }

        if ((currentType.equals("uint8") && (longValue < 0 || longValue > maxUByte)) ||
            (currentType.equals("int8") && (longValue < Byte.MIN_VALUE || longValue > Byte.MAX_VALUE)) ||
            (currentType.equals("uint16") && (longValue < 0 || longValue > maxUShort)) ||
            (currentType.equals("int16") && (longValue < Short.MIN_VALUE || longValue > Short.MAX_VALUE)) ||
            (currentType.equals("uint32") && (longValue < 0 || longValue > maxUInt)) ||
            (currentType.equals("int32") && (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE)))
          {
            return Ganymede.createErrorDialog("Unacceptable value",
                                              "This value is out of range for this dhcp option.");
          }

        String canonicalizedValue = Long.toString(longValue);

        if (!canonicalizedValue.equals(value))
          {
            ReturnVal retVal = new ReturnVal(true, true);
            retVal.setTransformedValueObject(canonicalizedValue);

            return retVal;
          }

        return null;
      }
    else if (currentType.equals("string"))
      {
      }
    else if (currentType.equals("text"))
      {
      }
    else if (currentType.equals("ip-address"))
      {
	return validateIP(object, value);
      }
    else if (currentType.equals("array of ip-address"))
      {
	StringBuilder resultBuilder = new StringBuilder();
	String pieces[] = value.split(",\\s*");

	boolean first_pass = true;

	for (String piece: pieces)
	  {
	    ReturnVal retVal = validateIP(object, piece);

	    if (!retVal.didSucceed())
	      {
		return retVal;
	      }

	    if (first_pass)
	      {
		first_pass = false;
	      }
	    else
	      {
		resultBuilder.append(", ");
	      }

	    resultBuilder.append((String) retVal.getTransformedValueObject());
	  }

	ReturnVal retVal = new ReturnVal(true, true);
	retVal.setTransformedValueObject(resultBuilder.toString());

	return retVal;
      }
    else
      {
      }

    return null;
  }

  private static ReturnVal validateIP(DBObject object, String ipOrHost)
  {
    Byte[] parsedAddress = null;

    try
      {
	parsedAddress = IPDBField.genIPV4bytes(ipOrHost);
      }
    catch (IllegalArgumentException ex)
      {
	String hostname = null;

	if (hostNameRegex.matcher(ipOrHost).matches())
	  {
	    hostname = ipOrHost;
	  }
	else
	  {
	    Matcher m = arlDomainNameRegex.matcher(ipOrHost);

	    if (m.matches())
	      {
		hostname = m.group(1);
	      }
	  }

	if (hostname == null)
	  {
	    return Ganymede.createErrorDialog("Unacceptable value",
					      "This dhcp option type requires an IP address");
	  }
	else
	  {
	    String query = "select object from 'Embedded System Interface' where " +
	      "'Containing Object'->('System Name' ==_ci '" + hostname + "' or 'Aliases' ==_ci '" + hostname + "') or " +
	      "'Name' ==_ci '" + hostname + "' or 'Aliases' ==_ci '" + hostname + "'";

	    QueryResult results = null;

	    try
	      {
		if (object.getGSession() == null)
		  {
		    results = Ganymede.internalSession.query(query);
		  }
		else
		  {
		    results = object.getGSession().query(query);
		  }
	      }
	    catch (NotLoggedInException exa)
	      {
		return Ganymede.createErrorDialog("Internal error",
						  exa.getMessage());
	      }
	    catch (GanyParseException exb)
	      {
		return Ganymede.createErrorDialog("Internal error",
						  exb.getMessage());
	      }

	    if (results.size() != 1)
	      {
		return Ganymede.createErrorDialog("Unacceptable error",
						  "This dhcp option type requires an IP address.\n\n" +
						  "Couldn't recognize hostname " + ipOrHost);
	      }

	    try
	      {
		Invid interfaceInvid = results.getInvid(0);
		DBObject interfaceObject = object.lookupInvid(interfaceInvid);
		DBField ipField = (DBField) interfaceObject.getField(interfaceSchema.ADDRESS);

		ReturnVal retVal = new ReturnVal(true, true);
		retVal.setTransformedValueObject(ipField.getEncodingString());

		return retVal;
	      }
	    catch (NullPointerException ex2)
	      {
		return Ganymede.createErrorDialog("Internal error",
						  ex2.getMessage());
	      }
	  }
      }

    ReturnVal retVal = new ReturnVal(true, true);
    retVal.setTransformedValueObject(IPDBField.genIPV4string(parsedAddress));

    return retVal;
  }
}
