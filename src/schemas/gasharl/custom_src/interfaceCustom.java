/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.
   
   Created: 15 October 1997
   Release: $Name:  $
   Version: $Revision: 1.34 $
   Last Mod Date: $Date: 2000/10/09 05:51:47 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
  Invid myNet = null;
  boolean inFinalizeAddrChange = false;

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
    getParentObj();
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
	    result.append(":");
	  }
	
	result.append(ipfield.getValueString(true));
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

    sysObj = getParentObj();
    Vector ipNetVec = sysObj.getAvailableNets();

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

    if (deleting)
      {
	return null;
      }

    if (field.getID() == interfaceSchema.IPNET)
      {
	// we don't want to mess with the available-network
	// management code if we are doing bulk-loading.

	if (!gSession.enableOversight || !gSession.enableWizards)
	  {
	    return null;
	  }

	// we're changing the IP net.. make sure our parent is ok with us
	// taking the new net.
	
	sysObj = getParentObj();

	// if the net is being set to a net that matches what's already
	// in the address field, we'll go ahead and ok it

	Byte[] address = (Byte[]) getFieldValueLocal(interfaceSchema.ADDRESS);

	if (address != null && matchNet(address, (Invid) value))
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.finalizeSetValue(): approving ipnet change");
	      }
	    
	    return null;
	  }
	
	// free the old net for others to use.. note that at this point, 
	// field.getValueLocal() holds the field's old value and value is the
	// proposed new value

	if (debug)
	  {
	    System.err.println("interfaceCustom.finalizeSetValue(): about to check net stuff");
	  }
	
	if (!sysObj.freeNet((Invid) field.getValueLocal()))
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.finalizeSetValue(): couldn't free old net num");
	      }
	
	    return Ganymede.createErrorDialog("schema error",
					      "interfaceCustom.finalizeSetValue(): couldn't free old net num");
	  }
	else if (!sysObj.allocNet((Invid) value))
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.finalizeSetValue(): couldn't alloc new net num");
	      }
	
	    sysObj.allocNet((Invid) field.getValueLocal()); // take it back

	    return Ganymede.createErrorDialog("schema error",
					      "interfaceCustom.finalizeSetValue(): couldn't alloc new net num");
	  }

	// if this net change was initiated by an approved ADDRESS change,
	// we're not going to try to second-guess their address choice.

	if (inFinalizeAddrChange)
	  {
	    return null;
	  }

	// set our address.. the wizardHook will have instructed the
	// client to rescan the address field.

	if (debug)
	  {
	    System.err.println("interfaceCustom.finalizeSetValue(): getting address");
	  }

	IPDBField ipfield = (IPDBField) getField(interfaceSchema.ADDRESS);
	address = getParentObj().getAddress((Invid) value);
	
	if (address != null)
	  {
	    // this will work or not, the client's rescan will show the
	    // result regardless

	    if (debug)
	      {
		System.err.print("interfaceCustom.finalizeSetValue(): setting address to ");

		for (int j = 0; j < address.length; j++)
		  {
		    if (j > 0)
		      {
			System.err.print(".");
		      }
		    
		    System.err.print(s2u(address[j].byteValue()));
		  }
		
		System.err.println();
	      }

	    ipfield.setValueLocal(address);
	  }
	else
	  {
	    System.err.println("interfaceCustom.finalizeSetValue(): null address from parent"); 
	  }
      }

    if (field.getID() == interfaceSchema.ADDRESS)
      {
	// don't get fancy if we're bulk loading

	if (!gSession.enableOversight || !gSession.enableWizards)
	  {
	    return null;
	  }

	if (myNet != null)
	  {
	    // we need to turn off the wizardHook's intercession
	    // as we correct the IPNET.. we use the inFinalizeAddrChange
	    // object variable to accomplish this.

	    inFinalizeAddrChange = true;
	    ReturnVal retVal = setFieldValue(interfaceSchema.IPNET, myNet);
	    inFinalizeAddrChange = false;

	    if (retVal != null && !retVal.didSucceed())
	      {
		if (debug)
		  {
		    System.err.println("interfaceCustom.finalizeSetValue(): failed to set ip net");
		  }

		return Ganymede.createErrorDialog("schema error",
						  "interfaceCustom.finalizeSetValue(): failed to set ip net");
	      }

	    // ok, we've got a valid address change request.. let the system object
	    // know that it should remember *this* address for the matching
	    // net.

	    sysObj = getParentObj();
	    sysObj.setAddress((Byte[]) value, myNet);

	    // ok, we tried it

	    myNet = null;
	  }
      }

    return null;
  }

  /**
   * This method is the hook that DBEditObject subclasses use to interpose
   * wizards when a field's value is being changed.<br><br>
   *
   * Whenever a field is changed in this object, this method will be
   * called with details about the change. This method can refuse to
   * perform the operation, it can make changes to other objects in
   * the database in response to the requested operation, or it can
   * choose to allow the operation to continue as requested.<br><br>
   *
   * In the latter two cases, the wizardHook code may specify a list
   * of fields and/or objects that the client may need to update in
   * order to maintain a consistent view of the database.<br><br>
   *
   * If server-local code has called
   * GanymedeSession.enableOversight(false), this method will never be
   * called.  This mode of operation is intended only for initial
   * bulk-loading of the database.<br><br>
   *
   * This method may also be bypassed when server-side code uses
   * setValueLocal() and the like to make changes in the database.<br><br>
   *
   * This method is called before the finalize*() methods.. the finalize*()
   * methods is where last minute cascading changes should be performed..
   * the finalize*() methods have no power to set object/field rescan
   * or return dialogs to the client, however.. in cases where such
   * is necessary, a custom plug-in class must have wizardHook() and
   * finalize*() configured to work together to both provide proper field
   * rescan notification and to check the operation being performed and
   * make any changes necessary to other fields and/or objects.<br><br>
   *
   * Note as well that wizardHook() is called before the namespace checking
   * for the proposed value is performed, while the finalize*() methods are
   * called after the namespace checking.
   *
   * @return a ReturnVal object indicated success or failure, objects and
   * fields to be rescanned by the client, and a doNormalProcessing flag
   * that will indicate to the field code whether or not the operation
   * should continue to completion using the field's standard logic.
   * <b>It is very important that wizardHook return a new ReturnVal(true, true)
   * if the wizardHook wishes to simply specify rescan information while
   * having the field perform its standard operation.</b>  wizardHook() may
   * return new ReturnVal(true, false) if the wizardHook performs the operation
   * (or a logically related operation) itself.  The same holds true for the
   * respond() method in GanymediatorWizard subclasses.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if (!gSession.enableWizards)
      {
	return null;
      }

    // we don't want to play our address/ipnet games if we're being deleted.

    if (deleting)
      {
	return null;
      }

    if (field.getID() == interfaceSchema.IPNET && operation == SETVAL)
      {
	// we don't want to mess with the IPNET change if it was initiated
	// by the client changing the IP address

	if (inFinalizeAddrChange)
	  {
	    return null;
	  }

	// Ok, we want to create a ReturnVal which will cause the
	// field.setValue() call which triggered us to continue normal
	// processing, and return our list of rescan preferences to
	// the client.

	ReturnVal result = new ReturnVal(true, true);
	result.addRescanField(this.getInvid(), interfaceSchema.ADDRESS);

	// Now scan through our siblings and let the client know that
	// they need to refresh their IPNET fields so that they won't
	// have the net that *we* just chose show up as options.

	Vector entries = getSiblingInvids();

	for (int i = 0; i < entries.size(); i++)
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.wizardHook(): adding object " + 
				   entries.elementAt(i) + " for rescan");
	      }
	    result.addRescanField((Invid) entries.elementAt(i), interfaceSchema.IPNET);
	  }

	if (debug)
	  {
	    System.err.println("interfaceCustom.wizardHook(): requesting ADDRESS field rescan");
	  }

	return result;
      }

    if (field.getID() == interfaceSchema.ADDRESS && operation == SETVAL)
      {
	if (!findNet((Byte[]) param1))
	  {
	    return Ganymede.createErrorDialog("Can't change IP net",
					      "The IP address you have selected does not match any network number " +
					      "available in this room.  Either this room does not have that network " +
					      "listed, or another interface on this system is already allocated on " +
					      "that network.");
	  }

	// findNet prepped finalizeSetValue() to set the IPNET for us
	// to myNet.. tell the client to rescan the IPNET field so it
	// sees the change

	ReturnVal result = new ReturnVal(true, true);
	result.addRescanField(this.getInvid(), interfaceSchema.IPNET);

	if (debug)
	  {
	    System.err.println("interfaceCustom.wizardHook(): asking for IPNET rescan in response to net change");
	  }
	
	return result;
      }

    return null;
  }

  /**
   *
   * This private helper method tries to find an IP net linked to the room that this
   * system is in.
   *
   */

  private boolean findNet(Byte[] address)
  {
    if (!matchNet(address, (Invid) getFieldValueLocal(interfaceSchema.IPNET)))
      {
	// need to find a net that matches the new address, if we can.

	if (debug)
	  {
	    System.err.println("interfaceCustom.findNet(): going to have to try to find a new net");
	  }

	Vector ipNetVec = sysObj.getAvailableNets();
	Invid netInvid;
	boolean found = false;
	ReturnVal retVal = null;
	String label;

	for (int i = 0; i < ipNetVec.size(); i++)
	  {
	    netInvid = ((ObjectHandle) ipNetVec.elementAt(i)).getInvid();
	    label = getGSession().viewObjectLabel(netInvid);

	    if (debug)
	      {
		System.err.println("interfaceCustom.findNet(): testing network " + label);
	      }

	    if (matchNet(address, netInvid))
	      {
		if (debug)
		  {
		    System.err.println("interfaceCustom.findNet(): found a network: " + label);
		  }

		found = true;

		// save this net for finalizeSetValue() to use
		myNet = netInvid;
		break;
	      }
	  }
	
	return found;
      }

    return true;
  }

  /**
   *
   * This private helper method compares the address given with the
   * IP network referenced by netInvid, returning true if the
   * address specified fits with the network referenced by netInvid.<br><br>
   *
   * This code, like the rest of the GASH network schema code, currently
   * assumes that all IP networks are 'Class C', where the first 3 octets
   * of an IPv4 address are the net number and the last octet is the
   * host id.
   *
   */

  private boolean matchNet(Byte[] address, Invid netInvid)
  {
    try
      {
	DBObject netObj = getSession().viewDBObject(netInvid);
	Byte[] netNum = (Byte[]) netObj.getFieldValueLocal(networkSchema.NETNUMBER);
	
	if (debug)
	  {
	    System.err.println("interfaceCustom.matchNet():");

	    System.err.println("\taddress: " + IPDBField.genIPV4string(address));
	    System.err.println("\tnet num: " + IPDBField.genIPV4string(netNum));
	  }

	for (int i = 0; i < (netNum.length-1); i++)
	  {
	    if (!netNum[i].equals(address[i]))
	      {
		if (debug)
		  {
		    System.err.println("interfaceCustom.matchNet(): failure to match octet " + i);
		  }

		return false;
	      }
	  }
      }
    catch (NullPointerException ex)
      {
	if (debug)
	  {
	    ex.printStackTrace();
	    Ganymede.debug("interfaceCustom.matchNet: NullPointer " + ex.getMessage());
	  }
	return false;
      }
    
    return true;
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
    DBSession session;
    DBObject sysObj;
    Invid sysInvid = (Invid) object.getFieldValueLocal(SchemaConstants.ContainerField);

    if (object instanceof DBEditObject)
      {
	sysObj = ((DBEditObject) object).getSession().viewDBObject(sysInvid);
      }
    else
      {
	sysObj = Ganymede.internalSession.getSession().viewDBObject(sysInvid);
      }

    try
      {
	result = (Vector) sysObj.getFieldValuesLocal(systemSchema.INTERFACES).clone();
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
