/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.

   Created: 15 October 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.util.Vector;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.IPAddress;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.IPDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 interfaceCustom

------------------------------------------------------------------------------*/

public class interfaceCustom extends DBEditObject implements SchemaConstants {

  static final boolean debug = false;

  // ---

  systemCustom sysObj = null;
  boolean inFinalizeAddrChange = false;
  boolean inFinalizeNetChange = false;

  /* -- */

  /**
   * Customization Constructor
   */

  public interfaceCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   * Create new object constructor
   */

  public interfaceCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   */

  public interfaceCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>This method provides a pre-commit hook that runs after the
   * user has hit commit but before the system has established write
   * locks for the commit.</p>
   *
   * <p>The intended purpose of this hook is to allow objects that
   * dynamically maintain hidden label fields to update those fields
   * from the contents of the object's other fields at commit
   * time.</p>
   *
   * <p>This method runs in a checkpointed context.  If this method
   * fails in any operation, you should return a ReturnVal with a
   * failure dialog encoded, and the transaction's commit will be
   * blocked and a dialog explaining the problem will be presented to
   * the user.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal preCommitHook()
  {
    if (this.getStatus() == ObjectStatus.DELETING ||
        this.getStatus() == ObjectStatus.DROPPING)
      {
        return null;
      }

    // if we changed networks so as not to require a MAC address for
    // this interface, go ahead and null it out as part of our
    // pre-commit activities.

    if (this.isDefined(interfaceSchema.IPNET) && !fieldRequired(this, interfaceSchema.ETHERNETINFO))
      {
        ReturnVal retVal = this.setFieldValueLocal(interfaceSchema.ETHERNETINFO, null);

        if (!ReturnVal.didSucceed(retVal))
          {
            return retVal;      // in case we failed for some reason
          }

        // change the hidden label to reflect the now empty MAC
        // address

        return updateHiddenLabelMACADDR(null);
      }

    return null;
  }

  /**
   * Update the hidden label with a proposed new interface name.
   *
   * @param interfaceName The new name being given to this interface.
   * May safely be null.
   *
   * @return A ReturnVal indicating success or failure in setting the
   * label.  If the label was changed successfully, a directive is
   * encoded into the ReturnVal to cause the system containing this
   * interface to refresh the label of this interface.
   */

  private ReturnVal updateHiddenLabelNAME(String interfaceName)
  {
    IPDBField ipfield = (IPDBField) this.getField(interfaceSchema.ADDRESS);
    String IPAddress = null;

    if (ipfield != null)
      {
        IPAddress = ipfield.getValueString();
      }

    // we'll only include our MAC address if the network we're
    // associated with requires it.

    String MACAddress = null;

    Invid netInvid = (Invid) this.getFieldValueLocal(interfaceSchema.IPNET);
    DBObject networkObj = this.lookupInvid(netInvid);

    if (networkObj == null || networkObj.isSet(networkSchema.MACREQUIRED))
      {
        MACAddress = (String) this.getFieldValueLocal(interfaceSchema.ETHERNETINFO);
      }

    ReturnVal retVal = this.setFieldValueLocal(interfaceSchema.HIDDENLABEL,
                                               genLabel(interfaceName,
                                                        IPAddress,
                                                        MACAddress));

    if (ReturnVal.didSucceed(retVal))
      {
        return ReturnVal.success().requestRefresh(getParentSysObj().getInvid(), systemSchema.INTERFACES);
      }

    return retVal;
  }

  /**
   * Update the hidden label with a proposed new IP Address.
   *
   * @param addr The new IP Address being given to this interface.
   * May safely be null.
   *
   * @return A ReturnVal indicating success or failure in setting the
   * label.  If the label was changed successfully, a directive is
   * encoded into the ReturnVal to cause the system containing this
   * interface to refresh the label of this interface.
   */

  private ReturnVal updateHiddenLabelIPADDR(IPAddress addr)
  {
    String interfaceName = (String) this.getFieldValueLocal(interfaceSchema.NAME);
    String IPAddress = null;

    if (addr != null)
      {
        IPAddress = addr.toString();
      }

    String MACAddress = null;

    Invid netInvid = getParentSysObj().findMatchingNet(addr);
    DBObject networkObj = this.lookupInvid(netInvid);

    if (networkObj == null || networkObj.isSet(networkSchema.MACREQUIRED))
      {
        MACAddress = (String) this.getFieldValueLocal(interfaceSchema.ETHERNETINFO);
      }

    ReturnVal retVal = this.setFieldValueLocal(interfaceSchema.HIDDENLABEL,
                                               genLabel(interfaceName,
                                                        IPAddress,
                                                        MACAddress));

    if (ReturnVal.didSucceed(retVal))
      {
        return ReturnVal.success().requestRefresh(getParentSysObj().getInvid(), systemSchema.INTERFACES);
      }

    return retVal;
  }

  /**
   * Update the hidden label with a proposed new MAC Address
   *
   * @param MACAddress The new MAC Address being given to this
   * interface.  May safely be null.
   *
   * @return A ReturnVal indicating success or failure in setting the
   * label.  If the label was changed successfully, a directive is
   * encoded into the ReturnVal to cause the system containing this
   * interface to refresh the label of this interface.
   */

  private ReturnVal updateHiddenLabelMACADDR(String MACAddress)
  {
    String interfaceName = (String) this.getFieldValueLocal(interfaceSchema.NAME);
    IPDBField ipfield = (IPDBField) this.getField(interfaceSchema.ADDRESS);
    String IPAddress = null;

    if (ipfield != null)
      {
        IPAddress = ipfield.getValueString();
      }

    // we'll only include our MAC address if the network we're
    // associated with requires it.

    Invid netInvid = (Invid) this.getFieldValueLocal(interfaceSchema.IPNET);
    DBObject networkObj = this.lookupInvid(netInvid);

    if (networkObj != null && !networkObj.isSet(networkSchema.MACREQUIRED))
      {
        MACAddress = null;
      }

    ReturnVal retVal = this.setFieldValueLocal(interfaceSchema.HIDDENLABEL,
                                               genLabel(interfaceName,
                                                        IPAddress,
                                                        MACAddress));

    if (ReturnVal.didSucceed(retVal))
      {
        return ReturnVal.success().requestRefresh(getParentSysObj().getInvid(), systemSchema.INTERFACES);
      }

    return retVal;
  }

  private final String genLabel(String interfaceName, String ipString, String macAddress)
  {
    StringBuilder result = new StringBuilder();
    boolean openIP = false;
    boolean openMAC = false;

    /* -- */

    if (interfaceName != null)
      {
        result.append(interfaceName);
      }

    if (ipString != null && !ipString.equals(""))
      {
        if (result.length() != 0)
          {
            result.append(" ");
          }

        result.append("[");
        result.append(ipString);
        openIP = true;
      }

    if (macAddress != null && !macAddress.trim().equals(""))
      {
        if (result.length() != 0)
          {
            result.append(" ");
          }

        if (!openIP)
          {
            result.append("[");
          }
        else
          {
            result.append("- ");
          }

        result.append(macAddress);

        openMAC = true;
      }

    if (openIP || openMAC)
      {
        result.append("]");
      }

    return result.toString();
  }

  /**
   * <p>This method returns a key that can be used by the client to
   * cache the value returned by choices().  If the client already has
   * the key cached on the client side, it can provide the choice list
   * from its cache rather than calling choices() on this object
   * again.</p>
   *
   * <p>The default logic in this method is designed to cause the
   * client to cache choice lists for invid fields in the 'all objects
   * of invid target type' cache bucket.  If your InvidDBField needs
   * to provide a restricted subset of objects of the targeted type as
   * the choice list, you'll need to override this method to either
   * return null (to turn off choice list caching), or generate some
   * kind of unique key that won't collide with the Short objects used
   * to represent the default object list caches.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.DBEditObject#choiceListHasExceptions(arlut.csd.ganymede.server.DBField)}
   * hook, which controls whether or not the default logic will
   * encourage the client to cache a given InvidDBField's choice
   * list.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  @Override public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == interfaceSchema.IPNET)
      {
        return null;            // no caching net choices, thankyouverymuch
      }

    return super.obtainChoicesKey(field);
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields, filtered by the GanymedeSession's
   * visibilityFilterInvids list.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  @Override public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() != interfaceSchema.IPNET)
      {
        return super.obtainChoiceList(field);
      }

    QueryResult result = new QueryResult();

    // get the vector of currently available nets from our containing
    // System

    Vector<ObjectHandle> ipNetVec = getParentSysObj().getAvailableNets();

    if (ipNetVec != null)
      {
        for (ObjectHandle handle: ipNetVec)
          {
            result.addRow(handle);
          }
      }

    if (debug)
      {
        System.err.println("interfaceCustom: net choice for invid " + getInvid() + ":\n" +
                           result.getBuffer());
      }

    return result;
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

  @Override public boolean mustChoose(DBField field)
  {
    // Don't force the IPNET field to be chosen from the choices()
    // list, since the custom finalizeSetValue logic in this class
    // takes care of that for us, and because the custom code in
    // this class modifies the choices in finalizeSetValue before
    // InvidDBField.setValue() calls verifyNewValue(), which would
    // normally check out the value selected against the results
    // of choices().

    if (field.getID() == interfaceSchema.IPNET)
      {
        return false;
      }

    return super.mustChoose(field);
  }

  /**
   * <p>If this DBEditObject is managing an embedded object, the
   * getEmbeddedObjectLabel() can be overridden to display a synthetic
   * label in the context of viewing or editing the containing object,
   * and when doing queries on the containing type.</p>
   *
   * <p>The getLabel() method will not consult this hook, however, and
   * embedded objects will be represented with their unique label
   * field when processed in an XML context.</p>
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  @Override public String getEmbeddedObjectDisplayLabelHook(DBObject object)
  {
    DBObject parent = object.getParentObj();

    try
      {
        DBObject typeObj = object.lookupInvid((Invid)parent.getFieldValueLocal(systemSchema.SYSTEMTYPE), false);

        if (typeObj.getFieldValueLocal(systemTypeSchema.SYSTEMTYPE).equals("IP Telephone"))
          {
            DBObject userObj = object.lookupInvid((Invid)parent.getFieldValueLocal(systemSchema.PRIMARYUSER), false);

            return String.valueOf(object.getLabel()) + " (" + String.valueOf(parent.getLabel()) + " - " + String.valueOf(userObj.getLabel()) + ")";
          }
      }
    catch (NullPointerException ex)
      {
      }

    return String.valueOf(object.getLabel()) + " (" + String.valueOf(parent.getLabel()) + ")";
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

  @Override public boolean canSeeField(DBSession session, DBField field)
  {
    // don't show off our hidden label for direct editing or viewing

    if (field.getID() == interfaceSchema.HIDDENLABEL)
      {
        return false;
      }

    // For the rest of our fields that we are concerned with, the
    // session will be null if we are being checked outside of an
    // editable context.  If we are not being edited, we don't
    // care.. if the fields are there, they can see them.

    if (!(field.getOwner() instanceof interfaceCustom) && (session == null))
      {
        return true;
      }

    // if we only have a single interface in this system, we don't
    // want the name field to be visible

    if ((field.getID() == interfaceSchema.NAME) ||
        (field.getID() == interfaceSchema.ALIASES))
      {
        interfaceCustom iObj;
        DBObject owner = field.getOwner();

        if (owner instanceof interfaceCustom)
          {
            iObj = (interfaceCustom) owner;
          }
        else
          {
            iObj = (interfaceCustom) session.editDBObject(owner.getInvid());
          }

        Vector<Invid> siblings = iObj.getSiblingInvids();

        if (siblings.size() == 0)
          {
            return false;
          }
        else
          {
            return true;
          }
      }

    if (field.getID() == interfaceSchema.ETHERNETINFO)
      {
        DBObject owner = field.getOwner();
        DBObject networkObj = owner.lookupInvid((Invid)owner.getFieldValueLocal(interfaceSchema.IPNET));

        if (networkObj != null && !networkObj.isSet(networkSchema.MACREQUIRED))
          {
            return false;       // we don't need to show the ethernet field if no MAC address is required
          }
      }

    return super.canSeeField(session, field);
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p>Note as well that the designated label field for objects are
   * always required, whatever this method returns, and that this
   * requirement holds without regard to the GanymedeSession's
   * enableOversight value.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  @Override public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case interfaceSchema.NAME:

        // the name is required if and only if the parent
        // object has more than one interface

        Vector<Invid> siblings = getSiblingInvids(object);

        if (siblings.size() == 0)
          {
            return false;
          }
        else
          {
            return true;
          }

      case interfaceSchema.ADDRESS:
      case interfaceSchema.IPNET:
        return true;

      case interfaceSchema.ETHERNETINFO:
        DBObject networkObj = object.lookupInvid((Invid)object.getFieldValueLocal(interfaceSchema.IPNET));

        // If networkObj is null, the DBEditSet will trigger on the
        // missing/unset IPNET field, so we don't need to worry about
        // that here.  We don't want to throw an exception, though.

        if (networkObj != null)
          {
            return networkObj.isSet(networkSchema.MACREQUIRED);
          }
      }

    return false;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions in
   * reaction to the set.  When a scalar field has its value set, it
   * will call its owners finalizeSetValue() method, passing itself as
   * the &lt;field&gt; parameter, and passing the new value to be
   * approved as the &lt;value&gt; parameter.  A Ganymede customizer
   * who creates custom subclasses of the DBEditObject class can
   * override the finalizeSetValue() method and write his own logic
   * to examine any change and either approve or reject the change.</p>
   *
   * <p>A custom finalizeSetValue() method will typically need to
   * examine the field parameter to see which field is being changed,
   * and then do the appropriate checking based on the value
   * parameter.  The finalizeSetValue() method can call the normal
   * this.getFieldValueLocal() type calls to examine the current state
   * of the object, if such information is necessary to make
   * appropriate decisions.</p>
   *
   * <p>If finalizeSetValue() returns null or a ReturnVal object with
   * a positive success value, the DBField that called us is
   * guaranteed to proceed to make the change to its value.  If this
   * method returns a non-success code in its ReturnVal, as with the
   * result of a call to Ganymede.createErrorDialog(), the DBField
   * that called us will not make the change, and the field will be
   * left unchanged.  Any error dialog returned from finalizeSetValue()
   * will be passed to the user.</p>
   *
   * <p>The DBField that called us will take care of all standard
   * checks on the operation (including a call to our own
   * verifyNewValue() method before calling this method.  Under normal
   * circumstances, we won't need to do anything here.
   * finalizeSetValue() is useful when you need to do unusually
   * involved checks, and for when you want a chance to trigger other
   * changes in response to a particular field's value being
   * changed.</p>
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  @Override public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // if this embedded interface is being removed, we won't try to get
    // fancy with the address/ipnet stuff.

    if (isDeleting())
      {
        return null;
      }

    if (field.getID() == interfaceSchema.IPNET)
      {
        // if this net change was initiated by an approved ADDRESS change,
        // we're not going to try to second-guess their address choice.

        if (inFinalizeAddrChange)
          {
            ReturnVal retVal = ReturnVal.success();
            retVal.addRescanField(this.getInvid(), interfaceSchema.ETHERNETINFO);

            return retVal;
          }

        // if the net is being set to a net that matches what's already
        // in the address field for some reason, we'll go ahead and ok it

        IPAddress address = (IPAddress) getFieldValueLocal(interfaceSchema.ADDRESS);

        if (address != null && systemCustom.checkMatchingNet(getDBSession(), (Invid) value, address))
          {
            if (debug)
              {
                System.err.println("interfaceCustom.finalizeSetValue(): approving ipnet change");
              }

            // some IPNETs don't require MAC addresses

            ReturnVal retVal = ReturnVal.success();
            retVal.addRescanField(this.getInvid(), interfaceSchema.ETHERNETINFO);

            return retVal;
          }

        // okay, we didn't match, tell the system object to remember the
        // address that was formerly associated with the old network value

        if (field.getValueLocal() != null)
          {
            getParentSysObj().saveAddress(address);
          }

        if (value == null)
          {
            IPDBField ipfield = (IPDBField) getField(interfaceSchema.ADDRESS);

            inFinalizeNetChange = true;
            ipfield.setValueLocal(null);
            inFinalizeNetChange = false;

            ReturnVal retVal = ReturnVal.success();
            retVal.addRescanField(this.getInvid(), interfaceSchema.ADDRESS);
            retVal.addRescanField(this.getInvid(), interfaceSchema.ETHERNETINFO);

            return retVal.merge(updateHiddenLabelIPADDR(null));
          }

        // now find a new address for this object based on the network we
        // are being asked to change to.

        address = getParentSysObj().getAddress((Invid) value);

        if (address == null)
          {
            return Ganymede.createErrorDialog(this.getGSession(),
                                              "Network Full",
                                              "There are no more addresses available in the " +
                                              getDBSession().getObjectLabel((Invid) value) +
                                              " network.");
          }

        // we've got a new IP address, go ahead and set it

        IPDBField ipfield = (IPDBField) getField(interfaceSchema.ADDRESS);

        // set the inFinalizeNetChange variable around the call to
        // setValueLocal() so that the recursive call to finalizeSetValue()
        // doesn't waste time trying to find a network to match the
        // new address before we complete the network change

        inFinalizeNetChange = true;
        ipfield.setValueLocal(address);
        inFinalizeNetChange = false;

        // and tell the client to rescan the address field to update
        // the display

        ReturnVal retVal = ReturnVal.success();
        retVal.addRescanField(this.getInvid(), interfaceSchema.ADDRESS);

        // and tell the client to rescan the ethernet info field as
        // well, in case we don't need it any more

        retVal.addRescanField(this.getInvid(), interfaceSchema.ETHERNETINFO);

        return retVal.merge(updateHiddenLabelIPADDR(address));
      }

    if (field.getID() == interfaceSchema.ADDRESS)
      {
        Invid netInvid = (Invid) getFieldValueLocal(interfaceSchema.IPNET);
        IPAddress address = (IPAddress) value;

        // if the address is being set in response to a network change,
        // don't bounce back and set the network again

        if (inFinalizeNetChange)
          {
            return null;
          }

        if (systemCustom.checkMatchingNet(getDBSession(), netInvid, address))
          {
            // fine, no change to the network required

            return updateHiddenLabelIPADDR(address);
          }

        // we need to find a new network to match, and to set that
        // into our network field

        netInvid = getParentSysObj().findMatchingNet(address);

        if (netInvid == null)
          {
            return Ganymede.createErrorDialog(this.getGSession(),
                                              "Unacceptable IP address",
                                              "IP address " + address +
                                              " does not match any network available to you.");
          }

        // we need to fix up the IP Network link to point to the
        // network that matches the new address.  We set
        // inFinalizeAddrChange to let the recursive call to
        // finalizeSetValue() spawned by setFieldValue() know not to
        // try and choose a new IP address before we get a chance to
        // return and okay the IP address change we are processing.

        try
          {
            inFinalizeAddrChange = true;
            ReturnVal retVal = this.setFieldValue(interfaceSchema.IPNET, netInvid);
            inFinalizeAddrChange = false;

            if (retVal != null && !retVal.didSucceed())
              {
                return Ganymede.createErrorDialog(this.getGSession(),
                                                  "schema error",
                                                  "interfaceCustom.finalizeSetValue(): failed to set ip net");
              }

            retVal = ReturnVal.success();
            retVal.addRescanField(this.getInvid(), interfaceSchema.IPNET);

            return retVal.merge(updateHiddenLabelIPADDR(address));
          }
        catch (GanyPermissionsException ex)
          {
            return Ganymede.createErrorDialog(this.getGSession(),
                                              "permissions", "permissions error setting network " + ex);
          }
      }

    // we also need to update the hidden label if the MAC address or
    // interface name was changed

    switch (field.getID())
      {
      case interfaceSchema.ETHERNETINFO:
        return updateHiddenLabelMACADDR((String) value);

      case interfaceSchema.NAME:
        return updateHiddenLabelNAME((String) value);
      }

    return null;
  }

  private systemCustom getParentSysObj()
  {
    if (sysObj == null)
      {
        Invid sysInvid = (Invid) getFieldValueLocal(SchemaConstants.ContainerField);

        // we *have* to use editDBObject() here because we need access to the custom
        // object.. it makes no sense for us to be pulled out for editing without
        // our parent also being edited.

        if (sysInvid != null)
          {
            sysObj = (systemCustom) getDBSession().editDBObject(sysInvid);
          }
      }

    return sysObj;
  }

  /**
   * <p>This private method returns a vector of invids, being a list
   * of other interfaces defined in the system we are defined in.</p>
   */

  private Vector<Invid> getSiblingInvids()
  {
    return getSiblingInvids(this);
  }

  /**
   * <p>This private method returns a vector of invids, being a list of
   * other interfaces defined in the system we are defined in.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  private Vector<Invid> getSiblingInvids(DBObject object)
  {
    // we can't use getParentSysObj() because that only works in an editing
    // context.  The checkRequiredFields() call may be called from a task
    // that wants to just sweep through the database looking for incomplete
    // objects, so we arrange to find a DBObject reference to parentObj
    // so that we can get access to the list of our siblings.

    DBObject parentObj = object.getParentObj();

    Vector<Invid> result = (Vector<Invid>) parentObj.getFieldValuesLocal(systemSchema.INTERFACES);

    // we are not our own sibling.

    result.remove(object.getInvid());

    if (debug)
      {
        System.err.println("interfaceCustom.getSiblingInvids(): " + object.getInvid() +
                           " has return value: " + result);
      }

    return result;
  }

  /**
   * <p>Provides a hook that can be used to approve, disapprove,
   * and/or transform any values to be set in any field in this
   * object.</p>
   *
   * <p>verifyNewValue can be used to canonicalize a
   * submitted value.  The verifyNewValue method may call
   * {@link arlut.csd.ganymede.common.ReturnVal#setTransformedValueObject(java.lang.Object, arlut.csd.ganymede.common.Invid, short) setTransformedValue()}
   * on the ReturnVal returned in order to substitute a new value for
   * the provided value prior to any other processing on the server.</p>
   *
   * <p>This method is called before any NameSpace checking is done, before the
   * {@link arlut.csd.ganymede.server.DBEditObject#wizardHook(arlut.csd.ganymede.server.DBField,int,java.lang.Object,java.lang.Object) wizardHook()}
   * method, and before the appropriate
   * {@link arlut.csd.ganymede.server.DBEditObject#finalizeSetValue(arlut.csd.ganymede.server.DBField, Object) finalizeSetValue()},
   * {@link arlut.csd.ganymede.server.DBEditObject#finalizeSetElement(arlut.csd.ganymede.server.DBField, int, Object) finalizeSetElement()},
   * {@link arlut.csd.ganymede.server.DBEditObject#finalizeAddElement(arlut.csd.ganymede.server.DBField, java.lang.Object) finalizeAddElement()},
   * or {@link arlut.csd.ganymede.server.DBEditObject#finalizeAddElements(arlut.csd.ganymede.server.DBField, java.util.Vector) finalizeAddElements()}
   * method is called.</p>
   *
   * @param field The DBField contained within this object whose value
   * is being changed
   * @param value The value that is being proposed to go into field.
   *
   * @return A ReturnVal indicating success or failure.  May be simply
   * 'null' to indicate success if no feedback need be provided.  If
   * {@link arlut.csd.ganymede.common.ReturnVal#hasTransformedValue() hasTransformedValue()}
   * returns true when callled on the returned ReturnVal, the value
   * returned by {@link arlut.csd.ganymede.common.ReturnVal#getTransformedValueObject() getTransformedValueObject()}
   * will be used for all further processing in the server, and will
   * be the value actually saved in the DBStore.
   */

  @Override public ReturnVal verifyNewValue(DBField field, Object value)
  {
    if (field.getID() == interfaceSchema.ETHERNETINFO)
      {
        // no worries about thread synchronization here, since
        // equality and assignment are both atomic operators

        String etherString = (String) value;
        String transformedString;

        if ((etherString == null) || (etherString.equals("")))
          {
            return null; // okay by us!
          }

        try
          {
            transformedString = verifyAndTransformEthernetInfo(etherString);
          }
        catch (MACAddressException ex)
          {
            return Ganymede.createErrorDialog(this.getGSession(),
                                              "Bad Ethernet Address",
                                              "You entered an invalid ethernet address (" + etherString +
                                              ")\n\nEthernet addresses should be in the form of 6 colon-separated" +
                                              " hexadecimal numbers.\n\nExample:\n01:a2:cc:04:12:2d\n");
          }

        if (transformedString.equals(etherString))
          {
            return super.verifyNewValue(field, value); // no change, so no problem
          }

        // tell the client that we'd like it to take the string that
        // they gave us and replace it with the reformatted one we
        // crafted.

        ReturnVal result = ReturnVal.success();

        result.setTransformedValueObject(transformedString, this.getInvid(), field.getID());

        return result;
      }

    return super.verifyNewValue(field, value);
  }

  /**
   * This method verifies and canonicalizes an ethernet info input
   * from the client.  If the input is a valid MAC address, or can be
   * turned into a valid MAC address, the MAC address is returned.  If
   * the input cannot be made into a properly formatted MAC address, a
   * MACAddressException will be thrown instead.
   */

  private String verifyAndTransformEthernetInfo(String input) throws MACAddressException
  {
    String transform1;

    /* -- */

    if (input == null)
      {
        return null;
      }

    input = input.trim();

    if (input.equals("0"))
      {
        return "00:00:00:00:00:00";
      }

    char [] ary = input.toCharArray();

    int digit_count = 0;

    for (int i = 0; i < ary.length; i++)
      {
        if (Character.digit(ary[i], 16) != -1)
          {
            digit_count++;
          }
      }

    if (digit_count == 12)
      {
        // yay, we've got precisely enough hex digits for an ethernet
        // address, whatever the separators may or may not be. Go
        // through and extract them and generate a new string.

        StringBuilder result = new StringBuilder();

        digit_count = 0;

        for (int i = 0; i < ary.length; i++)
          {
            if (Character.digit(ary[i], 16) != -1)
              {
                if (digit_count > 0 && digit_count % 2 == 0)
                  {
                    result.append(":");
                  }

                result.append(ary[i]);

                digit_count++;
              }
          }

        return result.toString().toLowerCase();
      }

    // we'll try to deal with missing leading zeros on hex bytes, but
    // we still need to have between 6 and 12 hex digits

    if (digit_count < 6 || digit_count > 12)
      {
        throw new MACAddressException();
      }

    // now, even though we have less than 12 hex digits, we may still have
    // a valid input, as the user may have skipped leading zeros on
    // bytes.  in order for us to make sense of such a state, we'll
    // need to have some separators.  we'll accept seperators of
    // spaces, dashes, colons, and periods.

    String[] pieces = input.split(":|\\.|\\-|\\s");

    if (pieces.length != 6)
      {
        throw new MACAddressException();
      }

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < pieces.length; i++)
      {
        if (pieces[i].length() > 2)
          {
            throw new MACAddressException();
          }

        if (pieces[i].length() == 1 && Character.digit(pieces[i].charAt(0), 16) != -1)
          {
            if (result.length() != 0)
              {
                result.append(":");
              }

            result.append("0");
            result.append(pieces[i].charAt(0));
            continue;
          }

        if (pieces[i].length() == 2 && Character.digit(pieces[i].charAt(0), 16) != -1 && Character.digit(pieces[i].charAt(1), 16) != -1)
          {
            if (result.length() != 0)
              {
                result.append(":");
              }

            result.append(pieces[i]);
            continue;
          }

        throw new MACAddressException();
      }

    return result.toString().toLowerCase();
  }

  /**
   * <p>Customization method to verify overall consistency of a
   * DBObject.  This method is intended to be overridden in
   * DBEditObject subclasses, and will be called by {@link
   * arlut.csd.ganymede.server.DBEditObject#commitPhase1()
   * commitPhase1()} to verify the readiness of this object for
   * commit.  The DBObject passed to this method will be a
   * DBEditObject, complete with that object's GanymedeSession
   * reference if this method is called during transaction commit, and
   * that session reference may be used by the verifying code if the
   * code needs to access the database.</p>
   *
   * <p>This method is for custom checks specific to custom
   * DBEditObject subclasses.  Standard checking for missing fields
   * for which fieldRequired() returns true is done by {@link
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

  @Override public ReturnVal consistencyCheck(DBObject object)
  {
    IPAddress address = (IPAddress) object.getFieldValueLocal(interfaceSchema.ADDRESS);
    Invid netInvid = (Invid) object.getFieldValueLocal(interfaceSchema.IPNET);

    if (address != null && !systemCustom.checkMatchingNet(getDBSession(), netInvid, address))
      {
        return Ganymede.createErrorDialog(object.getGSession(),
                                          "Bad IP Address",
                                          "Error, I.P. number/network mismatch in " + object.toString());
      }

    return null;
  }
}

/**
 * Context-specific exception for handling parse errors for submitted
 * Ethernet Info values.
 */

class MACAddressException extends RuntimeException {

  public MACAddressException()
  {
  }
}
