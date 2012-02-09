/*

   RFC2307ADSyncMaster.java

   A class used to inject additional context information into the ARL
   ADSync Channel XML file for the benefit of the servicing program.

   Created: 2 February 2012

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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

import java.util.Vector;

import arlut.csd.ganymede.common.FieldBook;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryDeRefNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.GanymedeSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                             RFC2307ADSyncMaster

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to add some additional context to
 * synchronization files when synchronizing to an Active Directory sync channel
 * with RFC2307 schema extensions.</p>
 */

public class RFC2307ADSyncMaster implements arlut.csd.ganymede.server.SyncMaster {

  /**
   * The augment() method optionally adds DBObject and DBField
   * identifiers to the FieldBook book parameter if the SyncMaster
   * decides that the additional DBObject/DBFields need to be written
   * to a delta sync channel in response to the changes made to obj.
   */

  public void augment(FieldBook book, DBEditObject eObj)
  {
    // whenever we write out a delta record for a user object that
    // includes a change to the homegroup field, we need to make sure
    // we include the target group and its GID field so that our sync
    // software can put the GID of that group into the posixAccount
    // object's rfc2307.bis gidNumber attribute.

    if (eObj.getTypeID() == SchemaConstants.UserBase)
      {
        if (book.has(eObj.getInvid(), userSchema.HOMEGROUP))
          {
            Invid group = (Invid) eObj.getFieldValueLocal(userSchema.HOMEGROUP);

	    // If the edited version of the eObj does not have a valid
	    // homegroup target, we will put the pre-transaction home
	    // group target object and GID into the sync file,
	    // otherwise we'll just put the new one.

            if (group == null)
              {
                group = (Invid) eObj.getOriginal().getFieldValueLocal(userSchema.HOMEGROUP);
              }

            book.add(group, groupSchema.GID);
          }

	if (book.has(eObj.getInvid(), userSchema.PORTALPIN))
	  {
	    book.add(eObj.getInvid(), userSchema.PASSWORD);
	  }
      }

    // likewise, whenever we write out a group whose gid is changing,
    // make sure we include all user objects and homegroup fields that
    // pointed to the group being changed.

    if (eObj.getTypeID() == groupSchema.BASE)
      {
	if (book.has(eObj.getInvid(), groupSchema.GID))
	  {
	    // we're changing our group id.. make sure we include sync
	    // information for all user objects that have this group as
	    // their old or new homegroup target.

	    GanymedeSession querySession = eObj.getGSession();

	    QueryDataNode matchingGroupNode = new QueryDataNode(QueryDataNode.INVIDVAL, QueryDataNode.EQUALS, eObj.getInvid());
	    QueryDeRefNode matchingUsers = new QueryDeRefNode(userSchema.HOMEGROUP, matchingGroupNode);

	    Vector<Result> results = querySession.internalQuery(new Query(SchemaConstants.UserBase, matchingUsers, false));

	    for (Result result: results)
	      {
		book.add(result.getInvid(), userSchema.HOMEGROUP);
	      }
	  }
      }
  }
}
