/*

   syncChannelCustom.java

   This file is a management class for sync channel definitions in Ganymede.
   
   Created: 2 February 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                               syncChannelCustom

------------------------------------------------------------------------------*/

/**
 * <p>This class is a {@link arlut.csd.ganymede.server.DBEditObject}
 * subclass for handling fields in the Ganymede server's sync channel
 * object type.  It contains special logic for handling the set up and
 * configuration of {@link arlut.csd.ganymede.server.SyncRunners} in
 * the Ganymede server.</p>
 */

public class syncChannelCustom extends DBEditObject implements SchemaConstants {

  /**
   *
   * Customization Constructor
   *
   */

  public syncChannelCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public syncChannelCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public syncChannelCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case SchemaConstants.SyncChannelName:
      case SchemaConstants.SyncChannelDirectory:
	return true;
      }

    // We'll allow SyncChannelServicer, SyncChannelFields, and
    // SyncChannelPlaintextOK to be false or undefined

    return false;
  }

  /**
   *
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.<br><br>
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invokation should be placed here,
   * rather than in commitPhase1().<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   */

  public void commitPhase2()
  {
    String origName = null, channelName;

    /* -- */

    if (original != null)
      {
	origName = (String) original.getFieldValueLocal(SchemaConstants.SyncChannelName);
      }

    channelName = (String) getFieldValueLocal(SchemaConstants.SyncChannelName);

    switch (getStatus())
      {
      case DROPPING:
	return;

      case DELETING:
	Ganymede.unregisterSyncChannel(origName);
	break;

      case EDITING:
	if (!origName.equals(channelName))
	  {
	    // we changed our channel name.. find the old channel
	    // runner, unregister it by that name, reconfigure it, and
	    // re-register

	    SyncRunner oldChannel = Ganymede.getSyncChannel(origName);
	    Ganymede.unregisterSyncChannel(origName);
	    oldChannel.updateInfo(this);
	    Ganymede.registerSyncChannel(oldChannel);
	  }
	else
	  {
	    // no name change, go ahead and reconfigure it on the
	    // fly.. the SyncRunner class is designed to be safe to
	    // run updateInfo at any time.

	    SyncRunner channel = Ganymede.getSyncChannel(origName);
	    channel.updateInfo(this);
	  }
	break;

      case CREATING:
	Ganymede.registerSyncChannel(new SyncRunner(this));
      }
  }
}
