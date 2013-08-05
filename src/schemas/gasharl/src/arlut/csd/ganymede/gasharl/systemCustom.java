/*

   systemCustom.java

   This file is a management class for system objects in Ganymede.

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

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBNameSpace;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.IPDBField;
import arlut.csd.ganymede.server.InvidDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    systemCustom

------------------------------------------------------------------------------*/

public class systemCustom extends DBEditObject implements SchemaConstants {

  static final boolean debug = false;

  // must pre-alloc for sync in updateSystemTypeChoiceList()

  static QueryResult systemTypes = new QueryResult();
  static Date systemTypesStamp = null;

  /**
   * <p>This method returns true if the given address fits in the
   * network object pointed to by netInvid, or false otherwise.</p>
   */

  public final static boolean checkMatchingNet(DBSession session, Invid netInvid, Byte[] address)
  {
    IPv4Range range;

    /* -- */

    if (netInvid == null)
      {
        return false;
      }

    DBObject netObj;

    if (session != null)
      {
        netObj = session.viewDBObject(netInvid);
      }
    else
      {
        netObj = Ganymede.db.getObject(netInvid);
      }

    String rangeString = (String) netObj.getFieldValueLocal(networkSchema.ALLOCRANGE);

    if (rangeString != null && !rangeString.equals(""))
      {
        range = new IPv4Range(rangeString);
      }
    else
      {
        Byte[] netNum = (Byte[]) netObj.getFieldValueLocal(networkSchema.NETNUMBER);

        if (netNum == null)
          {
            System.err.println("systemCustom.checkMatchingNet() found network " +
                               "object with no range string and no netNum");

            return false;
          }

        range = new IPv4Range(netNum);
      }

    return range.matches(address);
  }

  // ---

  /**
   * vector of ip network Object Handles that the user should be presented
   * as choices for individual interfaces
   */

  Vector<ObjectHandle> netsToChooseFrom = new Vector<ObjectHandle>();

  /**
   * <p>Vector of IP addresses in Byte array form.  If the user
   * switches the network of an interface, we push the old network on
   * a stack in ipAddresses so that we can pop it back off if another
   * interface is moved onto that network.</p>
   */

  Vector<Byte[]> ipAddresses = new Vector<Byte[]>();

  /**
   * <p>If this system is associated with a system type that
   * specifies a fourth-octet IP address search pattern,
   * we'll record the starting point for that seek here, or
   * -1 if we don't have one..</p>
   */

  int startSearchRange = -1;

  /**
   * <p>If this system is associated with a system type that
   * specifies a fourth-octet IP address search pattern,
   * we'll record the stopping point for that seek here, or
   * -1 if we don't have one..</p>
   */

  int stopSearchRange = -1;

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

    if (getGSession().enableOversight)
      {
        initializeNets(false);
      }
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
        initializeNets(false);
      }
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
   */

  @Override public ReturnVal initializeNewObject()
  {
    // If we are being created in an interactive context,
    // create the first interface

    if (getGSession().enableOversight && getGSession().enableWizards)
      {
        InvidDBField invField = (InvidDBField) getField(systemSchema.INTERFACES);

        // we shouldn't throw a null pointer here, as we should always have the
        // INTERFACES field available

        try
          {
            return invField.createNewEmbedded(true);
          }
        catch (NotLoggedInException ex)
          {
            return Ganymede.loginError(ex);
          }
        catch (GanyPermissionsException ex)
          {
            return Ganymede.createErrorDialog(this.getGSession(),
                                              "permissions",
                                              "permissions error initializing main system interface. " + ex);
          }
      }

    return null;
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.</p>
   *
   * @param session The DBSession that the new object is to be created in
   * @param origObj The object we are cloning
   * @param local If true, fields that have choice lists will not be checked against
   * those choice lists and read permissions for each field will not be consulted.
   * The canCloneField() method will still be consulted, however.
   *
   * @return A standard ReturnVal status object.  May be null on success, or
   * else may carry a dialog with information on problems and a success flag.
   */

  @Override public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    try
      {
        boolean problem = false;
        ReturnVal tmpVal;
        StringBuilder resultBuf = new StringBuilder();
        ReturnVal retVal = super.cloneFromObject(session, origObj, local);

        if (retVal != null)
          {
            if (!retVal.didSucceed())
              {
                return retVal;
              }

            if (retVal.getDialog() != null)
              {
                resultBuf.append("\n\n");
                resultBuf.append(retVal.getDialog().getText());

                problem = true;
              }
          }

        // and clone the embedded objects.
        //
        // Remember, systemCustom.initializeNewObject() will create a
        // single embedded interface as part of the normal system
        // creation process.  We'll put this (single) automatically
        // created embedded object into the newOnes vector, then
        // create any new embedded interfaces necessary when cloning a
        // multiple interface system.

        InvidDBField newInterfaces = (InvidDBField) getField(systemSchema.INTERFACES);
        InvidDBField oldInterfaces = (InvidDBField) origObj.getField(systemSchema.INTERFACES);

        Vector<Invid> newOnes = (Vector<Invid>) newInterfaces.getValuesLocal();
        Vector<Invid> oldOnes = (Vector<Invid>) oldInterfaces.getValuesLocal();

        DBObject origVolume;
        DBEditObject workingVolume;
        int i;

        for (i = 0; i < newOnes.size(); i++)
          {
            workingVolume = (DBEditObject) session.editDBObject(newOnes.get(i));
            origVolume = session.viewDBObject(oldOnes.get(i));
            tmpVal = workingVolume.cloneFromObject(session, origVolume, local);

            if (tmpVal != null && tmpVal.getDialog() != null)
              {
                resultBuf.append("\n\n");
                resultBuf.append(tmpVal.getDialog().getText());

                problem = true;
              }

            // clear the ethernet MAC address field in the interface,
            // since we don't want to duplicate MAC addresses when we
            // clone a system.

            tmpVal = workingVolume.setFieldValueLocal(interfaceSchema.ETHERNETINFO, null);

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
                try
                  {
                    tmpVal = newInterfaces.createNewEmbedded(local);
                  }
                catch (GanyPermissionsException ex)
                  {
                    tmpVal = Ganymede.createErrorDialog(session.getGSession(),
                                                        "permissions",
                                                        "permissions failure creating embedded interface " + ex);
                  }

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
                origVolume = session.viewDBObject(oldOnes.get(i));
                tmpVal = workingVolume.cloneFromObject(session, origVolume, local);

                if (tmpVal != null && tmpVal.getDialog() != null)
                  {
                    resultBuf.append("\n\n");
                    resultBuf.append(tmpVal.getDialog().getText());

                    problem = true;
                  }

                // clear the ethernet MAC address field in the interface,
                // since we don't want to duplicate MAC addresses when we
                // clone a system.

                tmpVal = workingVolume.setFieldValueLocal(interfaceSchema.ETHERNETINFO, null);

                if (tmpVal != null && tmpVal.getDialog() != null)
                  {
                    resultBuf.append("\n\n");
                    resultBuf.append(tmpVal.getDialog().getText());

                    problem = true;
                  }
              }
          }

        // and we also need to clone the DHCPOPTIONS field

        if (origObj.isDefined(systemSchema.DHCPOPTIONS))
          {
            InvidDBField newOptions = (InvidDBField) getField(systemSchema.DHCPOPTIONS);
            InvidDBField oldOptions = (InvidDBField) origObj.getField(systemSchema.DHCPOPTIONS);

            oldOnes = (Vector<Invid>) oldOptions.getValuesLocal();

            DBObject origOption;
            DBEditObject workingOption;

            for (i = 0; i < oldOnes.size(); i++)
              {
                try
                  {
                    tmpVal = newOptions.createNewEmbedded(local);
                  }
                catch (GanyPermissionsException ex)
                  {
                    tmpVal = Ganymede.createErrorDialog(session.getGSession(),
                                                        "permissions",
                                                        "permissions failure creating embedded option " + ex);
                  }

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

                workingOption = (DBEditObject) session.editDBObject(newInvid);
                origOption = session.viewDBObject(oldOnes.get(i));
                tmpVal = workingOption.cloneFromObject(session, origOption, local);

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
            retVal.setDialog(new JDialogBuff("Possible Clone Problems", resultBuf.toString(),
                                             "Ok", null, "ok.gif"));
          }

        return retVal;
      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }
  }

  /**
   * <p>Returns a vector of ObjectHandle objects describing the I.P. nets
   * available for this system to be connected to.</p>
   *
   * <p>Used by the interfaceCustom object to provide a list of network
   * choices.</p>
   */

  public Vector<ObjectHandle> getAvailableNets()
  {
    if (debug)
      {
        System.err.println("systemCustom: returning netsToChooseFrom");

        for (int i = 0; i < netsToChooseFrom.size(); i++)
          {
            ObjectHandle handle = netsToChooseFrom.get(i);

            System.err.println(i + ": " + handle.getLabel());
          }
      }

    return netsToChooseFrom;
  }

  /**
   * <p>This method scans through the networks available to the user editing
   * this object and returns an Invid for a network definition object, if
   * one can be found which matches the address in question.</p>
   *
   * <p>If no matching network is available, null is returned.</p>
   *
   * <p>Called from {@link arlut.csd.ganymede.gasharl.interfaceCustom
   * interfaceCustom}.</p>
   */

  public Invid findMatchingNet(Byte[] address)
  {
    for (int i = 0; i < netsToChooseFrom.size(); i++)
      {
        ObjectHandle handle = netsToChooseFrom.get(i);

        Invid netInvid = handle.getInvid();

        if (checkMatchingNet(getDBSession(), netInvid, address))
          {
            return netInvid;
          }
      }

    return null;
  }

  /**
   * <p>Called when an interface has its network changed, or when an
   * address on an interface is changed within a network.
   * saveAddress() records the address as one previously connected
   * with this system.  Future getAddress() calls on netInvid's with
   * overlapping ranges will return addresses saved with saveAddress()
   * before scanning for new addresses from the network's
   * IPv4Range.</p>
   */

  public synchronized boolean saveAddress(Byte[] address)
  {
    VectorUtils.unionAdd(ipAddresses, address);

    return true;                // we're probably freeing a net from an old room
  }

  /**
   * <p>Checks out a network for use by an interface in the current room.</p>
   *
   * <p>Used by the interfaceCustom object to provide a list of network
   * choices.</p>
   */

  public synchronized Byte[] getAddress(Invid netInvid)
  {
    return getIPAddress(netInvid, startSearchRange, stopSearchRange);
  }

  /**
   *
   * private helper method to initialize our network choices
   * that our interface code uses.  This method will load our
   * netsToChooseFrom vector with a list of object handles suitable
   * for use as network choices for our embedded interfaces, and
   * will determine the start and stop range from the specified
   * system type, if any.
   *
   */

  private void initializeNets(boolean onlyDoSystemType)
  {
    try
      {
        if (debug)
          {
            System.err.println("systemCustom.initializeNets()");
          }

        if (!onlyDoSystemType)
          {
            // what embedded interfaces do we have right now?

            Vector<Invid> interfaces = (Vector<Invid>) getFieldValuesLocal(systemSchema.INTERFACES);

            // what networks are available to us?

            Query netQuery = new Query(networkSchema.BASE);

            QueryResult netsEditable = editset.getDBSession().getGSession().query(netQuery);

            netsToChooseFrom = netsEditable.getHandles();

            // add any nets that are already connected to interfaces

            for (Invid interfaceInvid: interfaces)
              {
                DBObject interfaceObj = (DBObject) getDBSession().viewDBObject(interfaceInvid);
                Invid netInvid = (Invid) interfaceObj.getFieldValueLocal(interfaceSchema.IPNET);

                if (netInvid == null)
                  {
                    Ganymede.debug("Missing netinvid in object " + this.toString());
                    continue;
                  }

                DBObject netObj = (DBObject) getDBSession().viewDBObject(netInvid);
                String netLabel = netObj.getLabel();

                // okay, is this network already in our choice list?

                boolean found = false;

                for (ObjectHandle handle: netsToChooseFrom)
                  {
                    if (handle.getInvid().equals(netInvid))
                      {
                        found = true;
                        break;
                      }
                  }

                if (!found)
                  {
                    netsToChooseFrom.addElement(new ObjectHandle(netLabel, netInvid, false, false, false, true));
                  }
              }
          }

        // see if we have an attached system type which modifies our IP
        // search pattern

        Invid systemTypeInvid = (Invid) getFieldValueLocal(systemSchema.SYSTEMTYPE);

        if (systemTypeInvid != null)
          {
            DBObject systemTypeInfo = getDBSession().viewDBObject(systemTypeInvid);

            if (systemTypeInfo != null)
              {
                Boolean rangeRequired = (Boolean) systemTypeInfo.getFieldValueLocal(systemTypeSchema.USERANGE);

                if (rangeRequired != null && rangeRequired.booleanValue())
                  {
                    startSearchRange = ((Integer) systemTypeInfo.getFieldValueLocal(systemTypeSchema.STARTIP)).intValue();
                    stopSearchRange = ((Integer) systemTypeInfo.getFieldValueLocal(systemTypeSchema.STOPIP)).intValue();

                    if (debug)
                      {
                        System.err.println("systemCustom.initializeNets(): found start and stop for this type: " +
                                           startSearchRange + "->" + stopSearchRange);
                      }
                  }
              }
          }
      }
    catch (NotLoggedInException ex)
      {
        throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   * <p>This method updates the cached and static list of system type
   * choices in the event that the System Type object base has changed
   * since the cache was last updated.</p>
   */

  void updateSystemTypeChoiceList() throws NotLoggedInException
  {
    synchronized (systemTypes)
      {
        DBObjectBase base = Ganymede.db.getObjectBase((short) 272);

        // just go ahead and throw the null pointer if we didn't get our base.

        if (systemTypesStamp == null || systemTypesStamp.before(base.getTimeStamp()))
          {
            if (debug)
              {
                System.err.println("userCustom - updateSystemTypeChoiceList()");
              }

            Query query = new Query((short) 272, null, false);

            query.setFiltered(false); // don't care if we own the system types

            // internalQuery doesn't care if the query has its filtered bit set

            systemTypes = editset.getDBSession().getGSession().query(query);

            if (systemTypesStamp == null)
              {
                systemTypesStamp = new Date();
              }
            else
              {
                systemTypesStamp.setTime(System.currentTimeMillis());
              }
          }
      }
  }

  /**
   * <p>Allocates a free I.P. address for the given network object.  This
   * is done using the {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace}
   * attached to the interface address value field.  getIPAddress() will
   * seek through the Class-C host range looking for an IP address that
   * is not yet taken.  The direction of host id scanning depends on the
   * system category attached to this object.</p>
   *
   * <p>Note that this private helper method should only be called from
   * within synchronization on this object.</p>
   *
   * @param start The octet value to start seeking from, or -1 if not
   * used
   *
   * @param stop The octet value to stop seeking at, or -1 if not
   * used
   *
   * @return An IP address if one could be allocated, null otherwise
   */

  private Byte[] getIPAddress(Invid netInvid, int start, int stop)
  {
    // the namespace being used to manage the IP address space

    DBNameSpace namespace = Ganymede.db.getNameSpace("IPspace");
    Enumeration en = null;
    IPv4Range range;

    /* -- */

    if (namespace == null)
      {
        System.err.println("systemCustom.getIPAddress(): couldn't get IP namespace");
        return null;
      }

    DBObject netObj = getDBSession().viewDBObject(netInvid);

    String rangeString = (String) netObj.getFieldValueLocal(networkSchema.ALLOCRANGE);

    if (rangeString != null && !rangeString.equals(""))
      {
        range = new IPv4Range(rangeString);

        if (debug)
          {
            System.err.println("systemCustom.getIPAddress(): created range from rangeString: " + range);
          }
      }
    else
      {
        Byte[] netNum = (Byte[]) netObj.getFieldValueLocal(networkSchema.NETNUMBER);

        if (netNum == null)
          {
            System.err.println("systemCustom.getIPAddress(): no range or netnum in network object");
            return null;
          }

        range = new IPv4Range(netNum);

        if (debug)
          {
            System.err.println("systemCustom.getIPAddress(): created range from net number: " + IPDBField.genIPString(netNum) + ": " + range);
          }
      }

    // look for pre-existing address

    for (int i = 0; i < ipAddresses.size(); i++)
      {
        Byte[] address = ipAddresses.get(i);

        if (range.matches(address, start, stop))
          {
            ipAddresses.remove(i);
            return address;
          }
      }

    en = range.getElements(start, stop);

    boolean found = false;
    Byte[] address = null;

    while (!found && en.hasMoreElements())
      {
        address = (Byte[]) en.nextElement();

        if (debug)
          {
            System.err.println("systemCustom checking " + IPDBField.genIPString(address));
          }

        if (namespace.reserve(editset, address))
          {
            found = true;
          }
      }

    if (!found)
      {
        return null;
      }

    if (debug)
      {
        System.err.println("systemCustom.getIPAddress(): returning " + IPDBField.genIPString(address));
      }

    return address;
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

  @Override public boolean grantOwnership(GanymedeSession gSession, DBObject object)
  {
    Invid userInvid = (Invid) object.getFieldValueLocal(systemSchema.PRIMARYUSER);

    if (userInvid != null &&
        userInvid.equals(gSession.getPermManager().getUserInvid()))
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

  @Override public boolean fieldRequired(DBObject object, short fieldid)
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
        if (getStatus() != CREATING)
          {
            return false;
          }

        try
          {
            Invid systemType = (Invid) object.getFieldValueLocal(systemSchema.SYSTEMTYPE);

            // we're PSEUDOSTATIC, so we need to get ahold of the
            // internal session so we can look up objects

            DBObject typeObject = internalSession().getDBSession().viewDBObject(systemType);

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

  @Override public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == systemSchema.VOLUMES)
      {
        return null;            // no choices for volumes
      }

    if (field.getID() != systemSchema.SYSTEMTYPE)       // system type field
      {
        return super.obtainChoicesKey(field);
      }

    DBObjectBase base = Ganymede.db.getObjectBase((short) 272); // system types

    // we put a time stamp on here so the client
    // will know to call obtainChoiceList() afresh if the
    // system types base has been modified

    return "System Type:" + base.getTimeStamp();
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

  @Override public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() == systemSchema.VOLUMES)
      {
        return null;            // no choices for volumes
      }

    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
        updateSystemTypeChoiceList();
        return systemTypes;
      }

    return super.obtainChoiceList(field);
  }

  /**
   * <p>This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()</p>
   */

  @Override public boolean mustChoose(DBField field)
  {
    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
        return true;
      }

    if (field.getID() == systemSchema.PRIMARYUSER)
      {
        return false;           // allow the primary user to be set to <none>
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

  @Override public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // we only want to do the checks/work in this method if we aren't
    // in bulk load mode.

    if (!gSession.enableOversight || !gSession.enableWizards)
      {
        return null;
      }

    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
        // need to update the ip addresses pre-allocated for this system

        if (debug)
          {
            System.err.println("systemCustom: system type changed to " +
                               getDBSession().getObjectLabel((Invid) value));
          }

        initializeNets(true);
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

  @Override public ReturnVal finalizeAddElement(DBField field, Object value)
  {
    if (field.getID() == systemSchema.INTERFACES)
      {
        Vector<Invid> interfaces = (Vector<Invid>) getFieldValuesLocal(systemSchema.INTERFACES);

        if (interfaces.size() == 0)
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

        for (Invid invid: interfaces)
          {
            result.addRescanField(invid, interfaceSchema.NAME);
            result.addRescanField(invid, interfaceSchema.ALIASES);
          }

        return result;
      }

    return null;
  }

  /**
   * <p>This method allows the DBEditObject to have executive approval of
   * any vector delete operation, and to take any special actions in
   * reaction to the delete.. if this method returns null or a success
   * code in its ReturnVal, the {@link arlut.csd.ganymede.server.DBField DBField}
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

  @Override public ReturnVal finalizeDeleteElement(DBField field, int index)
  {
    if (field.getID() == systemSchema.INTERFACES)
      {
        Vector<Invid> interfaces = (Vector<Invid>) getFieldValuesLocal(systemSchema.INTERFACES);

        // if the interface that we are deleting is holding a network
        // allocation, we need to free that

        interfaceCustom delInterface = (interfaceCustom)
          getDBSession().editDBObject(interfaces.get(index));

        Invid oldNet = (Invid) delInterface.getFieldValueLocal(interfaceSchema.IPNET);
        Byte[] address = (Byte[]) delInterface.getFieldValueLocal(interfaceSchema.ADDRESS);

        if (oldNet != null && address != null)
          {
            saveAddress(address);
          }

        // if we have less than or more than 2 interfaces, we don't
        // care about hiding or revealing fields in the remaining
        // interface

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
          getDBSession().editDBObject(interfaces.get(indexToChange));

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
            String alias = (String) sourceField.getElementLocal(0);
            sourceField.deleteElementLocal(0);
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

        result.addRescanField(interfaces.get(index), interfaceSchema.NAME);
        result.addRescanField(interfaces.get(index), interfaceSchema.ALIASES);

        // finalizeDeleteElement() may add things to the SYSTEMALIASES field.

        result.addRescanField(this.getInvid(), systemSchema.SYSTEMALIASES);
        return result;
      }

    return null;
  }
}
