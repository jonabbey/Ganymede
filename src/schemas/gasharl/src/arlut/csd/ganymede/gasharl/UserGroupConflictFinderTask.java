/*

   UserGroupConflictFinderTask.java

   This task is a simple one-shot intended to detect and report on
   conflicts between the group and username name spaces, so that we
   can mitigate them.
   
   Created: 19 March 2008

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

import java.rmi.RemoteException;
import java.util.Vector;

import org.doomdark.uuid.EthernetAddress;
import org.doomdark.uuid.UUIDGenerator;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBNameSpace;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeServer;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                     UserGroupConflictFinderTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is a simple one-shot intended to detect and report on
 * conflicts between the group and username name spaces, so that we
 * can mitigate them.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class UserGroupConflictFinderTask implements Runnable {

  static final boolean debug = true;

  /* -- */

  GanymedeSession mySession = null;
  Thread currentThread = null;

  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    boolean transactionOpen = false;

    /* -- */

    Ganymede.debug("UserGroupConflictFinder Task: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring UserGroupConflictFinder task - semaphore disabled: " + error);
	return;
      }

    try
      {
        DBNameSpace users = Ganymede.db.getNameSpace("username");
        DBNameSpace groups = Ganymede.db.getNameSpace("groupname");

        users.findConflicts(groups);  // this will generate debug statements
      }
    catch (Throwable ex)
      {
	Ganymede.debug("Caught " + ex.getMessage());
      }
  }
}

