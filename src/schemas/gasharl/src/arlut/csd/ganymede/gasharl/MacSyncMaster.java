/*

   MacSyncMaster.java

   Provides delta sync channel augmentation for the Mac Sync Channel.

   Created: 23 February 2010

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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

import java.util.ArrayList;
import java.util.List;

import arlut.csd.ganymede.common.*;
import arlut.csd.ganymede.server.*;

/*------------------------------------------------------------------------------
									   class
								      SyncMaster

------------------------------------------------------------------------------*/

/**
 *  Provides delta sync channel augmentation for the Mac Sync Channel.
 */

public class MacSyncMaster implements SyncMaster {

  private DBSession session;

  /* -- */

  public MacSyncMaster()
  {
  }

  /**
   * The augment() method optionally adds DBObject and DBField
   * identifiers to the FieldBook book parameter if the SyncMaster
   * decides that the additional DBObject/DBFields need to be written
   * to a delta sync channel in response to the changes made to obj.
   */

  public synchronized void augment(FieldBook book, DBEditObject obj)
  {
    this.session = obj.getSession();

    try
      {
	switch (obj.getTypeID())
	  {
	  case SchemaConstants.UserBase:
	    includeUser(book, obj);
	    return;

	  case volumeSchema.BASE:
	    includeVolume(book, obj);
	    return;

	  case systemSchema.BASE:
	    includeSystem(book, obj);
	    return;
	  }
      }
    finally
      {
	this.session = null;
      }
  }

  /**
   * If a user is being modified, we need to make sure we link in
   * the edit-in-place object definition for their auto.home.default
   * map entry, the volume object, and the system object, so that
   * the external mac sync channel scripting can resolve the nfs
   * home directory.
   */

  private void includeUser(FieldBook book, DBObject user)
  {
    if (!(user instanceof DBEditObject))
      {
	// if we're putting in a user as a non-edited context object
	// for a host or volume redefinition, force the necessary
	// fields to make sense of that.

	includeUserFields(book, user);
      }

    List<Invid> mapEntries = (List<Invid>) user.getFieldValuesLocal(userSchema.VOLUMES);

    for (Invid invid: mapEntries)
      {
	book.add(invid, mapEntrySchema.MAP);
	book.add(invid, mapEntrySchema.VOLUME);

	DBObject automounterMap = session.viewDBObject(invid);

	Invid volumeInvid = (Invid) automounterMap.getFieldValueLocal(mapEntrySchema.VOLUME);

	DBObject volumeObject = session.viewDBObject(volumeInvid);

	Invid hostInvid = (Invid) volumeObject.getFieldValueLocal(volumeSchema.HOST);

	book.add(volumeInvid, volumeSchema.HOST);
	book.add(volumeInvid, volumeSchema.PATH);
	book.add(hostInvid, systemSchema.SYSTEMNAME);
      }

    return;
  }

  /**
   * If a volume is being modified, we need to scan to see if any
   * users have that volume as a home directory in automounter, and
   * if so we need to include those user objects and linked volume
   * and system objects.
   */

  private void includeVolume(FieldBook book, DBObject volume)
  {
    try
      {
	GanymedeSession gSession = session.getGSession();

	String label = volume.getLabel();

	String queryString = "select object from 'User' where 'Directory Volume'->('NFS Volume'->('Volume Label' == '" + label + "'))";
	Query query = new GanyQueryTransmuter().transmuteQueryString(queryString);

	List<Result> results = gSession.internalQuery(query);
	
	for (Result result: results)
	  {
	    includeUser(book, session.viewDBObject(result.getInvid()));
	  }
      }
    catch (GanyParseException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * If a system is being modified such that its host name is being
   * changed, we need to scan to see if any volumes are linked to
   * that system, and if any users have their home directories on
   * that volume, we need to include those user objects
   */

  private void includeSystem(FieldBook book, DBObject system)
  {
    try
      {
	GanymedeSession gSession = session.getGSession();

	String label = system.getLabel();

	String queryString = "select object from 'User' where 'Directory Volume'->('NFS Volume'->('Host' == '" + label + "'))";
	Query query = new GanyQueryTransmuter().transmuteQueryString(queryString);

	List<Result> results = gSession.internalQuery(query);
	
	for (Result result: results)
	  {
	    includeUser(book, session.viewDBObject(result.getInvid()));
	  }
      }
    catch (GanyParseException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * This method is called when includeVolume() or includeSystem()
   * triggers the inclusion of a user.
   */

  private void includeUserFields(FieldBook book, DBObject user)
  {
    Invid userInvid = user.getInvid();

    if (book.has(userInvid))
      {
	// the user is already in the field book, we won't add
	// anything

	return;
      }

    book.add(userInvid, userSchema.USERNAME);
    book.add(userInvid, userSchema.UID);
    book.add(userInvid, userSchema.GUID);
    book.add(userInvid, userSchema.VOLUMES);
  }
}
