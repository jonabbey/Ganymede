/*

   systemCustom.java

   This file is a management class for system objects in Ganymede.
   
   Created: 15 October 1997
   Release: $Name:  $
   Version: $Revision: 1.28 $
   Last Mod Date: $Date: 1999/10/29 18:43:35 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
import arlut.csd.JDialog.JDialogBuff;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    systemCustom

------------------------------------------------------------------------------*/

public class systemCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;
  static QueryResult shellChoices = new QueryResult();
  static Date shellChoiceStamp = null;

  // ---

  /**
   * vector of ip network Object Handles in current room
   */

  Vector netsInRoom = new Vector();

  /**
   * vector of ip network Object Handles free in current room
   */

  Vector freeNets = new Vector();

  /**
   * <p>map of IPNet Invids to addresses.  This hashtable maps invids of
   * network DBObjects to IP addresses that this system has allocated for
   * each network.  If a system has multiple interfaces and we take an
   * interface from one net to another, and then move it back, this
   * hashtable will let us give it back the same address that it had
   * before when it was associated with this net.</p>
   */

  Hashtable ipAddresses = new Hashtable();

  /**
   *
   * Customization Constructor
   *
   */

  public systemCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public systemCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public systemCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);

    if (getGSession().enableOversight)
      {
	initializeNets((Invid) getFieldValueLocal(systemSchema.ROOM));
      }
  }

  /**
   * <p>Initializes a newly created DBEditObject.</p>
   *
   * <p>When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.  If this DBEditObject is an embedded type, it will
   * have been linked into its parent object before this method
   * is called.</p>
   *
   * <p>This method is responsible for filling in any default
   * values that can be calculated from the 
   * {@link arlut.csd.ganymede.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.</p>
   *
   * <p>If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.DBSession#createDBObject(short, arlut.csd.ganymede.Invid, java.util.Vector)
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
   */

  public ReturnVal initializeNewObject()
  {
    // If we are being created in an interactive context, 
    // create the first interface

    if (getGSession().enableOversight)
      {
	InvidDBField invField = (InvidDBField) getField(systemSchema.INTERFACES);

	// we shouldn't throw a null pointer here, as we should always have the
	// INTERFACES field available

	return invField.createNewEmbedded(true);
      }

    return null;
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.</p>
   */

  public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    boolean problem = false;
    ReturnVal tmpVal;
    StringBuffer resultBuf = new StringBuffer();
    ReturnVal retVal = super.cloneFromObject(session, origObj, local);

    if (retVal != null && retVal.getDialog() != null)
      {
	resultBuf.append("\n\n");
	resultBuf.append(retVal.getDialog().getText());
	
	problem = true;
      }

    // and clone the embedded objects

    InvidDBField newInterfaces = (InvidDBField) getField(systemSchema.INTERFACES);
    InvidDBField oldInterfaces = (InvidDBField) origObj.getField(systemSchema.INTERFACES);

    Vector newOnes;
    Vector oldOnes;

    if (local)
      {
	newOnes = (Vector) newInterfaces.getValuesLocal().clone();
	oldOnes = (Vector) oldInterfaces.getValuesLocal().clone();
      }
    else
      {
	newOnes = newInterfaces.getValues();
	oldOnes = oldInterfaces.getValues();
      }

    DBObject origVolume;
    DBEditObject workingVolume;
    int i;

    for (i = 0; i < newOnes.size(); i++)
      {
	workingVolume = (DBEditObject) session.editDBObject((Invid) newOnes.elementAt(i));
	origVolume = session.viewDBObject((Invid) oldOnes.elementAt(i));
	tmpVal = workingVolume.cloneFromObject(session, origVolume, local);

	if (tmpVal != null && tmpVal.getDialog() != null)
	  {
	    resultBuf.append("\n\n");
	    resultBuf.append(tmpVal.getDialog().getText());
	    
	    problem = true;
	  }
      }

    Invid newInvid;

    if (i < oldOnes.size())
      {
	for (; i < oldOnes.size(); i++)
	  {
	    tmpVal = newInterfaces.createNewEmbedded(local);

	    if (!tmpVal.didSucceed())
	      {
		if (tmpVal != null && tmpVal.getDialog() != null)
		  {
		    resultBuf.append("\n\n");
		    resultBuf.append(tmpVal.getDialog().getText());
		    
		    problem = true;
		  }
		continue;
	      }

	    newInvid = tmpVal.getInvid();

	    workingVolume = (DBEditObject) session.editDBObject(newInvid);
	    origVolume = session.viewDBObject((Invid) oldOnes.elementAt(i));
	    tmpVal = workingVolume.cloneFromObject(session, origVolume, local);

	    if (tmpVal != null && tmpVal.getDialog() != null)
	      {
		resultBuf.append("\n\n");
		resultBuf.append(tmpVal.getDialog().getText());
	    
		problem = true;
	      }
	  }
      }

    retVal = new ReturnVal(true, !problem);

    if (problem)
      {
	retVal.setDialog(new JDialogBuff("Possible Clone Problem", resultBuf.toString(),
					 "Ok", null, "ok.gif"));
      }
    
    return retVal;
  }

  /**
   * <p>Returns a vector of ObjectHandle objects describing the I.P. nets
   * available for this system to be connected to.</p>
   *
   * <p>Used by the interfaceCustom object to provide a list of network
   * choices.</p>
   */

  public Vector getAvailableNets()
  {
    if (debug)
      {
	System.err.println("systemCustom: returning freeNets");

	for (int i = 0; i < freeNets.size(); i++)
	  {
	    ObjectHandle handle = (ObjectHandle) freeNets.elementAt(i);
	    
	    System.err.println(i + ": " + handle.getLabel());
	  }
      }

    return freeNets;
  }

  /**
   * This method returns an IPv4 address for an embedded interface
   * based on the network invid passed in.
   */

  public Byte[] getAddress(Invid netInvid)
  {
    if (debug)
      {
	System.err.println("systemCustom: returning address for net " + getGSession().viewObjectLabel(netInvid));
      }

    return (Byte[]) ipAddresses.get(netInvid);
  }

  /**
   * This method allows an embedded interfaceCustom object to change
   * this systemCustom's notion of what the preferred address is for a
   * particular available IP network.
   */

  public void setAddress(Byte[] address, Invid netInvid)
  {
    if (debug)
      {
	System.err.println("systemCustom.setAddress(): setting address for net " + 
			   getGSession().viewObjectLabel(netInvid));
      }

    ipAddresses.put(netInvid, address);
  }

  /**
   * <p>Marks a network in the current room that was previously used as
   * available for an interface attached to this system to be
   * connected to.</p>
   *
   * <p>Used by the interfaceCustom object to provide a list of network
   * choices.</p>
   */

  public synchronized boolean freeNet(Invid netInvid)
  {
    ObjectHandle handle;
    boolean found = false;

    /* -- */

    if (netInvid == null)
      {
	System.err.println("systemCustom.freeNet(): trying to free null");
	return true;
      }

    String label = getGSession().viewObjectLabel(netInvid);

    if (debug)
      {
	System.err.println("systemCustom.freeNet(): attempting to free " + label);
      }

    // do we already have this net in our free list?

    for (int i = 0; i < freeNets.size(); i++)
      {
	handle = (ObjectHandle) freeNets.elementAt(i);
	
	if (handle.getInvid().equals(netInvid))
	  {
	    found = true;
	  }
      }

    if (found)
      {
	if (debug)
	  {
	    System.err.println("systemCustom.freeNet(): " + label + " is already freed");
	  }

	return true;
      }

    for (int i = 0; i < netsInRoom.size(); i++)
      {
	handle = (ObjectHandle) netsInRoom.elementAt(i);
	
	if (handle.getInvid().equals(netInvid))
	  {
	    freeNets.addElement(handle);

	    if (debug)
	      {
		System.err.println("systemCustom.freeNet(" + handle.getLabel() + ")");
	      }

	    return true;
	  }
      }

    return false;
  }

  /**
   * <p>Checks out a network for use by an interface in the current room.</p>
   *
   * <p>Used by the interfaceCustom object to provide a list of network
   * choices.</p>
   */

  public synchronized boolean allocNet(Invid netInvid)
  {
    ObjectHandle handle;

    /* -- */
    
    for (int i = 0; i < freeNets.size(); i++)
      {
	handle = (ObjectHandle) freeNets.elementAt(i);
	
	if (handle.getInvid().equals(netInvid))
	  {
	    if (debug)
	      {
		System.err.println("systemCustom.allocNet(" + handle.getLabel() + ")");
	      }

	    freeNets.removeElementAt(i);
	    return true;
	  }
      }

    return false;
  }

  /**
   *
   * private helper method to initialize our network choices
   * that our interface code uses.  This method will load our
   * netsInRoom vector with a list of object handles suitable
   * for use as network choices for our embedded interfaces,
   * will reset our freeNets vector with a list of object
   * handles that are available for new interfaces, or to
   * change an existing interface to, and builds the ipAddresses
   * hash to give us a quick way of picking an ip address for
   * a given subnet.
   *
   */

  private void initializeNets(Invid roomInvid)
  {
    DBObject interfaceObj;
    Invid netInvid;
    String label;
    ObjectHandle handle;
    boolean usingNet = false;
    Hashtable localAddresses = new Hashtable();
    Byte[] address;

    /* -- */

    if (debug)
      {
	System.err.println("systemCustom.initializeNets(" + 
			   getGSession().viewObjectLabel(roomInvid)+")");
      }

    if (netsInRoom == null)
      {
	netsInRoom = new Vector();
      }
    else
      {
	netsInRoom.removeAllElements();
      }

    if (freeNets == null)
      {
	freeNets = new Vector();
      }
    else
      {
	freeNets.removeAllElements();
      }

    if (roomInvid == null)
      {
	return;
      }

    // what embedded interfaces do we have right now?

    Vector interfaces = getFieldValuesLocal(systemSchema.INTERFACES);

    // get the room information

    DBObject roomObj = getSession().viewDBObject(roomInvid);
    Vector nets = roomObj.getFieldValuesLocal(roomSchema.NETWORKS);
    
    for (int i = 0; i < nets.size(); i++)
      {
	netInvid = (Invid) nets.elementAt(i);
	label = getGSession().viewObjectLabel(netInvid);
	handle = new ObjectHandle(label, netInvid, false, false, false, true);

	if (debug)
	  {
	    System.err.println("systemCustom.initializeNets(): processing net " + label);
	  }

	netsInRoom.addElement(handle);

	// find out what nets are new with this new room invid and which we
	// were already using

	if (!ipAddresses.containsKey(netInvid))
	  {
	    // ok, we don't have an address for this net stored yet.. see whether
	    // an interface hooked up to us is using this net.. if so, see if
	    // it has an address and keep that if so.

	    usingNet = false;

	    if (interfaces != null)
	      {
		for (int j = 0; j < interfaces.size(); j++)
		  {
		    interfaceObj = getSession().viewDBObject((Invid) interfaces.elementAt(j));

		    // interfaceObj damn well shouldn't be null

		    Invid netInvid2 = (Invid) interfaceObj.getFieldValueLocal(interfaceSchema.IPNET);

		    if (netInvid2 != null && netInvid2.equals(netInvid))
		      {
			address = (Byte[]) interfaceObj.getFieldValueLocal(interfaceSchema.ADDRESS);

			if (address != null)
			  {
			    usingNet = true;

			    // remember this address for this net
			
			    ipAddresses.put(netInvid, address);
			    break;
			  }
		      }
		  }
	      }

	    // if we didn't find an interface using this net, we'll need to generate
	    // a new address for this network.

	    if (!usingNet)
	      {
		localAddresses.put(netInvid, netInvid);
	      }
	  }
      }

    // okay, now localAddresses has a map for the nets that we were
    // not previously on.
    //
    // now we need to get an IP address on each net.. if any of the
    // nets are full, we'll remove that network from netsInRoom to
    // mark that net as not being usable for a new address

    Enumeration enum = localAddresses.keys();

    while (enum.hasMoreElements())
      {
	netInvid = (Invid) enum.nextElement();

	if (debug)
	  {
	    System.err.println("systemCustom.initializeNets() trying to find an address on " + 
			       getGSession().viewObjectLabel(netInvid));
	  }

	address = getIPAddress(netInvid);

	if (address != null)
	  {
	    // we've allocated a new address for a net that
	    // we don't yet have an address for.. store
	    // the address we allocated

	    ipAddresses.put(netInvid, address);
	  }
	else
	  {
	    // we couldn't get an address for netInvid.. take the net
	    // out of our netsInRoom vector.

	    if (debug)
	      {
		System.err.println("systemCustom.initializeNets(): net " + 
				   getGSession().viewObjectLabel(netInvid) + 
				   " can't be used for allocation, we couldn't find an address on it.");
	      }

	    for (int i = 0; i < netsInRoom.size(); i++)
	      {
		handle = (ObjectHandle) netsInRoom.elementAt(i);

		if (handle.getInvid().equals(netInvid))
		  {
		    netsInRoom.removeElementAt(i);
		    break;
		  }
	      }
	  }
      }

    // okay, we have ipAddresses loaded with addresses available for
    // this system in this room.  we need to go through the nets that
    // we have registered and for those addresses that are both
    // available in this room and not already taken, we need to
    // add handles to them to the freeNets vector which we
    // cleared upon entering initializeNets().

    enum = ipAddresses.keys();

    while (enum.hasMoreElements())
      {
	netInvid = (Invid) enum.nextElement();

	// is this netInvid really available in our current room?

	for (int i = 0; i < netsInRoom.size(); i++)
	  {
	    handle = (ObjectHandle) netsInRoom.elementAt(i);
		
	    if (handle.getInvid().equals(netInvid))
	      {
		if (debug)
		  {
		    System.err.println("systemCustom.initializeNets(): net " + 
				       handle.getLabel() + " is available for allocation");
		  }

		usingNet = false;
		
		for (int j = 0; j < interfaces.size(); j++)
		  {
		    interfaceObj = getSession().viewDBObject((Invid) interfaces.elementAt(j));

		    Invid netInvid2 = (Invid) interfaceObj.getFieldValueLocal(interfaceSchema.IPNET);

		    if (netInvid2 != null && netInvid2.equals(netInvid))
		      {
			usingNet = true;
		      }
		  }

		if (!usingNet)
		  {
		    freeNets.addElement(handle);
		    break;
		  }
	      }
	  }
      }
  }

  /**
   * <p>Allocates a free I.P. address for the given network object.  This
   * is done using the {@link arlut.csd.ganymede.DBNameSpace DBNameSpace}
   * attached to the interface address value field.  getIPAddress() will
   * seek through the Class-C host range looking for an IP address that
   * is not yet taken.  The direction of host id scanning depends on the
   * system category attached to this object.</p>
   *
   * @return An IP address if one could be allocated, null otherwise
   */

  private Byte[] getIPAddress(Invid netInvid)
  {
    // the namespace being used to manage the IP address space

    DBNameSpace namespace = Ganymede.db.getNameSpace("IPspace");

    // default IP host-byte scan pattern 

    int start = 1;
    int stop = 254;

    /* -- */

    if (namespace == null)
      {
	System.err.println("systemCustom.getIPAddress(): couldn't get IP namespace");
	return null;
      }

    DBObject netObj = getSession().viewDBObject(netInvid);
    Byte[] netNum = (Byte[]) netObj.getFieldValueLocal(networkSchema.NETNUMBER);
    
    if (netNum.length != 4)
      {
	Ganymede.debug("Error, " + netObj.getLabel() + 
		       " has an improper network number for the GASH schema.");
	return null;
      }

    Byte[] address = new Byte[4];

    for (int i = 0; i < netNum.length; i++)
      {
	address[i] = netNum[i];
      }

    // ok, we've got our net prefix.. try to find an open slot..

    // first see if we have an attached system type which modifies our IP
    // search pattern

    try
      {
	Invid systemTypeInvid = (Invid) getFieldValueLocal(systemSchema.SYSTEMTYPE);
	DBObject systemTypeInfo = getSession().viewDBObject(systemTypeInvid);
	
	if (systemTypeInfo != null)
	  {
	    start = ((Integer) systemTypeInfo.getFieldValueLocal(systemTypeSchema.STARTIP)).intValue();
	    stop = ((Integer) systemTypeInfo.getFieldValueLocal(systemTypeSchema.STOPIP)).intValue();

	    if (debug)
	      {
		System.err.println("systemCustom.getIPAddress(): found start and stop for this type: " + 
				   start + "->" + stop);
	      }
	  }
      }
    catch (NullPointerException ex)
      {
	System.err.println("systemCustom.getIPAddress(): null pointer exception trying to get system type info");
      }

    int i = start;
    address[3] = new Byte(u2s(i));

    // find an unused ip address on this net

    if (start > stop)
      {
	while (i > stop && !namespace.reserve(editset, address, true))
	  {
	    address[3] = new Byte(u2s(--i));
	  }
      }
    else
      {
	while (i < stop && !namespace.reserve(editset, address, true))
	  {
	    address[3] = new Byte(u2s(++i));
	  }
      }

    // see if we really did wind up with an acceptable address

    if (!namespace.reserve(editset, address, true))
      {
	return null;
      }
    else
      {
	if (debug)
	  {
	    System.err.print("systemCustom.getIPAddress(): returning ");
	    
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

	return address;
      }
  }

  /**
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value, and is used to down shift
   * values from the 0-255 that can only be held in a short or
   * larger to a signed byte for storage.
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
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }

  /**
   * <p>Hook to allow subclasses to grant ownership privileges to a given
   * object.  If this method returns true on a given object, the Ganymede
   * Permissions system will provide access to the object as owned with
   * whatever permissions apply to objects owned by the persona active
   * in gSession.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    Invid userInvid = (Invid) object.getFieldValueLocal(systemSchema.PRIMARYUSER);

    if (userInvid != null &&
	userInvid.equals(gSession.getUserInvid()))
      {
	return true;
      }

    return false;
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   *
   * <p>Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case systemSchema.SYSTEMNAME:
      case systemSchema.INTERFACES:
      case systemSchema.SYSTEMTYPE:
      case systemSchema.ROOM:
	return true;
      }

    // Whether or not the associated user field is required depends on
    // the system type.

    if (fieldid == systemSchema.PRIMARYUSER)
      {
	try
	  {
	    Invid systemType = (Invid) object.getFieldValueLocal(systemSchema.SYSTEMTYPE);

	    // we're PSEUDOSTATIC, so we need to get ahold of the
	    // internal session so we can look up objects
	    
	    DBObject typeObject = internalSession().getSession().viewDBObject(systemType);

	    Boolean userRequired = (Boolean) typeObject.getFieldValueLocal(systemTypeSchema.USERREQ);

	    return userRequired.booleanValue();
	  }
	catch (NullPointerException ex)
	  {
	    // if we can't get the system type reference, assume that
	    // we aren't gonna require it.. the user will still be
	    // prompted to set a system type, and once they go back
	    // and do that and try to re-commit, they'll hit us again
	    // and we can make the proper determination at that point.

	    return false;
	  }
      }

    return false;
  }

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>If there is no caching key, this method will return null.
   */

  public Object obtainChoicesKey(DBField field)
  {
    DBObjectBase base = Ganymede.db.getObjectBase((short) 272);	// system types

    /* -- */

    if (field.getID() == systemSchema.VOLUMES)
      {
	return null;		// no choices for volumes
      }
    
    if (field.getID() != systemSchema.SYSTEMTYPE)	// system type field
      {
	return super.obtainChoicesKey(field);
      }
    else
      {
	// we put a time stamp on here so the client
	// will know to call obtainChoiceList() afresh if the
	// system types base has been modified

	return "System Type:" + base.getTimeStamp();
      }
  }

  /**
   * <p>This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.</p>
   *
   * <p>This method will provide a reasonable default for targetted
   * invid fields.</p>
   *
   * <p>NOTE: This method does not need to be synchronized.  Making this
   * synchronized can lead to DBEditObject/DBSession nested monitor
   * deadlocks.</p>
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() == systemSchema.VOLUMES)
      {
	return null;		// no choices for volumes
      }

    if (field.getID() != systemSchema.SYSTEMTYPE) // system type field
      {
	return super.obtainChoiceList(field);
      }

    Query query = new Query((short) 272, null, false); // list all system types

    query.setFiltered(false);	// don't care if we own the system types

    return editset.getSession().getGSession().query(query);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
	return true;
      }
    
    if (field.getID() == systemSchema.PRIMARYUSER)
      {
	return false;		// allow the primary user to be set to <none>
      }

    return super.mustChoose(field);
  }
  
  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any scalar set operation, and to take any special actions in
   * reaction to the set.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its value.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.</p>
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // we only want to do the checks/work in this method if we aren't
    // in bulk load mode.

    if (!gSession.enableOversight)
      {
	return null;
      }

    if (field.getID() == systemSchema.ROOM)
      {
	if (debug)
	  {
	    System.err.println("systemCustom: room changed to " + 
			       getGSession().viewObjectLabel((Invid) value));
	  }

	// rework our structures to reflect the networks available due to our
	// change in room
	
	initializeNets((Invid) value);

	// we need to generate a returnval that will cause all our
	// interfaces' ipnet fields to be rescanned.

	Vector interfaces = getFieldValuesLocal(systemSchema.INTERFACES);

	if (interfaces == null)
	  {
	    return null;
	  }

	// create the ReturnVal that we are actually going to
	// return.. the second true tells the code that called us to
	// go ahead and proceed normally, but to include the ReturnVal
	// information that we are returning when the results finally
	// go back to the client.

	ReturnVal result = new ReturnVal(true, true);

	// Have all of the interface objects under us refresh their IPNET
	// field to go along with the change in room.

	for (int i = 0; i < interfaces.size(); i++)
	  {
	    result.addRescanField((Invid) interfaces.elementAt(i), interfaceSchema.IPNET);
	  }

	return result;
      }

    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
	// need to update the ip addresses pre-allocated for this system

	if (debug)
	  {
	    System.err.println("systemCustom: system type changed to " + 
			       getGSession().viewObjectLabel((Invid) value));
	  }
	
	initializeNets((Invid) getFieldValueLocal(systemSchema.ROOM));
      }

    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector add operation, and to take any special actions in
   * reaction to the add.. if this method returns null or a success
   * code in its ReturnVal, the DBField that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.</p>
   */

  public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    if (field.getID() == systemSchema.INTERFACES)
      {
	Vector interfaces = getFieldValuesLocal(systemSchema.INTERFACES);

	if (interfaces == null)
	  {
	    return null;
	  }

	// create the ReturnVal that we are actually going to
	// return.. the second true tells the code that called us to
	// go ahead and proceed normally, but to include the ReturnVal
	// information that we are returning when the results finally
	// go back to the client.
	    
	ReturnVal result = new ReturnVal(true, true);

	// Have all of the interface objects under us refresh
	// their IPNET field to go along with the changes
	// resulting from the extra interface
	    
	for (int i = 0; i < interfaces.size(); i++)
	  {
	    result.addRescanField((Invid) interfaces.elementAt(i), 
				  interfaceSchema.NAME);

	    result.addRescanField((Invid) interfaces.elementAt(i), 
				  interfaceSchema.ALIASES);
	  }
	    
	return result;
      }

    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the {@link arlut.csd.ganymede.DBField DBField}
   * that called us will proceed to
   * make the change to its vector.  If this method returns a
   * non-success code in its ReturnVal, the DBField that called us
   * will not make the change, and the field will be left
   * unchanged.</p>
   *
   * <p>The DBField that called us will take care of all possible checks
   * on the operation (including vector bounds, etc.).  Under normal
   * circumstances, we won't need to do anything here.</p>
   */

  public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    if (field.getID() == systemSchema.INTERFACES)
      {
	Vector interfaces = getFieldValuesLocal(systemSchema.INTERFACES);

	// if we have more than 2 interfaces, we don't care

	if (interfaces.size() != 2)
	  {
	    return null;
	  }

	// we want to clear the name field of the remaining interface, and concatenate
	// any aliases defined on it to the system alias list instead.

	int indexToChange;

	if (index == 1)
	  {
	    indexToChange = 0;
	  }
	else if (index == 0)
	  {
	    indexToChange = 1;
	  }
	else
	  {
	    throw new ArrayIndexOutOfBoundsException("can't delete an index out of range");
	  }

	interfaceCustom io = (interfaceCustom) 
	  getSession().editDBObject((Invid) interfaces.elementAt(indexToChange));
	    
	ReturnVal retVal = io.setFieldValueLocal(interfaceSchema.NAME, null);
	    
	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
	
	// we want to rip all the aliases out of the interface alias field
	// and add them to our system aliases field.. we know there's no
	// overlap because they are both in the same namespace.
	
	DBField aliasesField = (DBField) getField(systemSchema.SYSTEMALIASES);
	DBField sourceField = (DBField) io.getField(interfaceSchema.ALIASES);
	
	while (sourceField.size() > 0)
	  {
	    String alias = (String) sourceField.getElement(0);
	    sourceField.deleteElement(0);
	    aliasesField.addElementLocal(alias);
	  }

	ReturnVal result = new ReturnVal(true, true);

	// We want to rescan the remaining interface, whichever that might be
	
	if (index == 1)
	  {
	    index = 0;
	  }
	else if (index == 0)
	  {
	    index = 1;
	  }

	result.addRescanField((Invid) interfaces.elementAt(index), 
			      interfaceSchema.NAME);
	result.addRescanField((Invid) interfaces.elementAt(index), 
			      interfaceSchema.ALIASES);
	    
	// finalizeDeleteElement() may add things to the SYSTEMALIASES field.
	
	result.addRescanField(this.getInvid(), systemSchema.SYSTEMALIASES);
	return result;
      }

    return null;
  }

  /**
   * <p>This method is the hook that DBEditObject subclasses use to interpose
   * {@link arlut.csd.ganymede.GanymediatorWizard wizards} when a field's
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
   * {@link arlut.csd.ganymede.GanymedeSession#enableOversight(boolean) 
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
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    return null;
  }
}
