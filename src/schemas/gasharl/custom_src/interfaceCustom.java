/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.
   
   Created: 15 October 1997
   Release: $Name:  $
   Version: $Revision: 1.42 $
   Last Mod Date: $Date: 2002/08/21 07:07:47 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import gnu.regexp.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 interfaceCustom

------------------------------------------------------------------------------*/

public class interfaceCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  static gnu.regexp.RE regexp = null;
  // ---

  systemCustom sysObj = null;
  boolean inFinalizeAddrChange = false;
  boolean inFinalizeNetChange = false;

  /* -- */

  /**
   *
   * Customization Constructor
   *
   */

  public interfaceCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public interfaceCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public interfaceCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>This method should be defined to return true in DBEditObject subclasses
   * which provide a getLabelHook() method.</p>
   *
   * <p>If this method is not redefined to return true in any subclasses which
   * define a getLabelHook() method, then searches on objects of this type
   * may not properly reflect the desired label.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean useLabelHook()
  {
    return true;
  }

  /**
   *
   * Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type
   *
   */

  public String getLabelHook(DBObject object)
  {
    StringBuffer result = new StringBuffer();
    boolean openIP = false;
    boolean openMAC = false;

    /* -- */

    String name = (String) object.getFieldValueLocal(interfaceSchema.NAME);

    if (name != null)
      {
	result.append(name);
      }

    IPDBField ipfield = (IPDBField) object.getField(interfaceSchema.ADDRESS);

    if (ipfield != null)
      {
	if (result.length() != 0)
	  {
	    result.append(" ");
	  }

	result.append("[");
	result.append(ipfield.getValueString());

	openIP = true;
      }

    String macAddress = (String) object.getFieldValueLocal(interfaceSchema.ETHERNETINFO);

    if (macAddress != null && macAddress.length() != 0)
      {
	if (result.length() != 0)
	  {
	    result.append(" ");
	  }

	if (openIP)
	  {
	    result.append(" - ");
	  }
	else
	  {
	    result.append("[");
	  }

	result.append(macAddress);

	openMAC = true;
      }

    if (openIP || openMAC)
      {
	result.append("]");
      }

    if (result.length() == 0)
      {
	return null;
      }
    else
      {
	return result.toString();
      }
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == interfaceSchema.IPNET)
      {
	return null;		// no caching net choices, thankyouverymuch
      }
    
    return super.obtainChoicesKey(field);
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.<br><br>
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() != interfaceSchema.IPNET)
      {
	return super.obtainChoiceList(field);
      }

    QueryResult result = new QueryResult();

    // get the vector of currently available nets from our containing
    // System

    Vector ipNetVec = getParentObj().getAvailableNets();

    if (ipNetVec != null)
      {
	for (int i = 0; i < ipNetVec.size(); i++)
	  {
	    result.addRow((ObjectHandle) ipNetVec.elementAt(i));
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
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
   */

  public boolean mustChoose(DBField field)
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
   *
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.<br><br>
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.<br><br>
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   * 
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
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

	Vector siblings = iObj.getSiblingInvids();

	if (siblings.size() == 0)
	  {
	    return false;
	  }
	else
	  {
	    return true;
	  }
      }

    return super.canSeeField(session, field);
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case interfaceSchema.NAME:

	// the name is required if and only if the parent
	// object has more than one interface
	
	Vector siblings = getSiblingInvids(object);
	
	if (siblings.size() == 0)
	  {
	    return false;
	  }
	else
	  {
	    return true;
	  }

      case interfaceSchema.ADDRESS:
      case interfaceSchema.ETHERNETINFO:
      case interfaceSchema.IPNET:
	return true;
      }

    return false;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval of
   * any scalar set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its value.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.<br><br>
   *
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // if this embedded interface is being removed, we won't try to get
    // fancy with the address/ipnet stuff.

    if (isDeleting())
      {
	return null;
      }

    // we don't want to mess with the available-network
    // management code if we are doing bulk-loading.
    
    if (!gSession.enableOversight || !gSession.enableWizards)
      {
	return null;
      }

    if (field.getID() == interfaceSchema.IPNET)
      {
	// if this net change was initiated by an approved ADDRESS change,
	// we're not going to try to second-guess their address choice.
	
	if (inFinalizeAddrChange)
	  {
	    return null;
	  }

	// if the net is being set to a net that matches what's already
	// in the address field for some reason, we'll go ahead and ok it
	
	Byte[] address = (Byte[]) getFieldValueLocal(interfaceSchema.ADDRESS);

	if (address != null && getParentObj().checkMatchingNet((Invid) value, address))
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.finalizeSetValue(): approving ipnet change");
	      }
	    
	    return null;
	  }
	
	// okay, we didn't match, tell the system object to remember the
	// address that was formerly associated with the old network value

	if (field.getValueLocal() != null)
	  {
	    getParentObj().saveAddress(address);
	  }

	if (value == null)
	  {
	    IPDBField ipfield = (IPDBField) getField(interfaceSchema.ADDRESS);

	    inFinalizeNetChange = true;
	    ipfield.setValueLocal(null);
	    inFinalizeNetChange = false;

	    ReturnVal retVal = new ReturnVal(true, true);
	    retVal.addRescanField(this.getInvid(), interfaceSchema.ADDRESS);
	    
	    return retVal;
	  }

	// now find a new address for this object based on the network we
	// are being asked to change to.

	address = getParentObj().getAddress((Invid) value);

	if (address == null)
	  {
	    String label = getGSession().viewObjectLabel((Invid) value);

	    return Ganymede.createErrorDialog("Network Full",
					      "There are no more addresses available in the " +
					      getGSession().viewObjectLabel((Invid) value) +
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

	ReturnVal retVal = new ReturnVal(true, true);
	retVal.addRescanField(this.getInvid(), interfaceSchema.ADDRESS);

	return retVal;
      }

    if (field.getID() == interfaceSchema.ADDRESS)
      {
	// if the address is being set in response to a network change,
	// don't bounce back and set the network again

	if (inFinalizeNetChange)
	  {
	    return null;
	  }

	Invid netInvid = (Invid) getFieldValueLocal(interfaceSchema.IPNET);
	Byte[] address = (Byte[]) value;

	if (getParentObj().checkMatchingNet(netInvid, address))
	  {
	    // fine, no change to the network required

	    return null;
	  }

	// we need to find a new network to match, and to set that
	// into our network field

	netInvid = getParentObj().findMatchingNet((Byte[]) value);

	if (netInvid == null)
	  {
	    return Ganymede.createErrorDialog("Unacceptable IP address",
					      "IP address " + IPDBField.genIPString(address) +
					      " does not match any network available to you.");
	  }

	// we need to fix up the IP Network link to point to the
	// network that matches the new address.  We set
	// inFinalizeAddrChange to let the recursive call to
	// finalizeSetValue() spawned by setFieldValue() know not to
	// try and choose a new IP address before we get a chance to
	// return and okay the IP address change we are processing.
	
	inFinalizeAddrChange = true;
	ReturnVal retVal = setFieldValue(interfaceSchema.IPNET, netInvid);
	inFinalizeAddrChange = false;
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return Ganymede.createErrorDialog("schema error",
					      "interfaceCustom.finalizeSetValue(): failed to set ip net");
	  }
	
	retVal = new ReturnVal(true, true);
	retVal.addRescanField(this.getInvid(), interfaceSchema.IPNET);
	
	return retVal;
      }

    return null;
  }

  private systemCustom getParentObj()
  {
    if (sysObj == null)
      {    
	Invid sysInvid = (Invid) getFieldValueLocal(SchemaConstants.ContainerField);

	// we *have* to use editDBObject() here because we need access to the custom
	// object.. it makes no sense for us to be pulled out for editing without
	// our parent also being edited.

	if (sysInvid != null)
	  {
	    sysObj = (systemCustom) getSession().editDBObject(sysInvid);
	  }
      }

    return sysObj;
  }

  /**
   *
   * This private method returns a vector of invids, being a list of
   * other interfaces defined in the system we are defined in.
   * 
   */

  private Vector getSiblingInvids()
  {
    return getSiblingInvids(this);
  }

  /**
   *
   * This private method returns a vector of invids, being a list of
   * other interfaces defined in the system we are defined in.
   *
   * <b>*PSEUDOSTATIC*</b>
   * 
   */

  private Vector getSiblingInvids(DBObject object)
  {
    Vector result;
    DBObject parentObj;
    Invid sysInvid;

    /* -- */

    // we can't use getParentObj() because that only works in an editing
    // context.  The checkRequiredFields() call may be called from a task
    // that wants to just sweep through the database looking for incomplete
    // objects, so we arrange to find a DBObject reference to parentObj
    // so that we can get access to the list of our siblings.

    sysInvid = (Invid) object.getFieldValueLocal(SchemaConstants.ContainerField);

    if (object instanceof DBEditObject)
      {
	parentObj = ((DBEditObject) object).getSession().viewDBObject(sysInvid);
      }
    else
      {
	parentObj = Ganymede.internalSession.getSession().viewDBObject(sysInvid);
      }

    try
      {
	result = (Vector) parentObj.getFieldValuesLocal(systemSchema.INTERFACES).clone();
      }
    catch (NullPointerException ex)
      {
	return new Vector();
      }
    
    // we are not our own sibling.

    result.removeElement(object.getInvid());

    if (debug)
      {
	System.err.println("interfaceCustom.getSiblingInvids(): " + object.getInvid() +
			   " has return value: " + result);
      }

    return result;
  }

  /**
   *
   * This method provides a hook that can be used to check any values
   * to be set in any field in this object.  Subclasses of
   * DBEditObject should override this method, implementing basically
   * a large switch statement to check for any given field whether the
   * submitted value is acceptable given the current state of the
   * object.<br><br>
   *
   * Question: what synchronization issues are going to be needed
   * between DBEditObject and DBField to insure that we can have
   * a reliable verifyNewValue method here?
   * 
   */

  public ReturnVal verifyNewValue(DBField field, Object value)
  {
    if (field.getID() == interfaceSchema.ETHERNETINFO)
      {
	// no worries about thread synchronization here, since
	// equality and assignment are both atomic operators

	String etherString = (String) value;

	if ((etherString == null) || (etherString.equals("")))
	  {
	    return null;
	  }

	if (regexp == null)
	  {
	    try
	      {
		String hexdigit = "[abcdef0123456789]";
		String separator = ":";

		regexp = new gnu.regexp.RE("^" + hexdigit + hexdigit + "?" + separator +
					   hexdigit + hexdigit + "?" + separator +
					   hexdigit + hexdigit + "?" + separator +
					   hexdigit + hexdigit + "?" + separator +
					   hexdigit + hexdigit + "?" + separator +
					   hexdigit + hexdigit + "?$",
					   gnu.regexp.RE.REG_ICASE);
	      }
	    catch (gnu.regexp.REException ex)
	      {
		throw new RuntimeException("Error, interface custom code can't initialize regular expression" + 
					   ex.getMessage());
	      }
	  }

	gnu.regexp.REMatch match = regexp.getMatch(etherString);

	if (match == null)
	  {
	    return Ganymede.createErrorDialog("Bad Ethernet Address",
					      "You entered an invalid ethernet address (" + etherString +
					      ")\n\nEthernet addresses should be in the form of 6 :" +
					      " separated hex bytes.\n\nExample:\n01:a2:cc:04:12:2d\n");
	  }
	else
	  {
	    return null;
	  }
      }

    return super.verifyNewValue(field, value);
  }

  /**
   *
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.
   *
   */

  public final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  /**
   *
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   *
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }
}
