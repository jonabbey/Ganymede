/*
   GASH 2

   DBCheckPoint.java

   The GANYMEDE object storage system.

   Created: 15 January 1999

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.Invid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBCheckPoint

------------------------------------------------------------------------------*/

/**
 * <p>DBCheckPoint is a class designed to allow server-side code that
 * needs to attempt a multi-step operation that might not successfully
 * complete to be able to undo all changes made without having to
 * abort the entire transaction.</p>
 *
 * <p>In other words, a DBCheckPoint is basically a transaction within
 * a transaction.</p>
 */

final class DBCheckPoint {

  static final boolean debug = false;

  // ---

  List<DBLogEvent> logEvents = null;
  List<DBCheckPointObj> objects = null;
  Set<Invid> invidDeleteLocks = null;

  /* -- */

  DBCheckPoint(List<DBLogEvent> logEvents, DBEditObject[] transObjects, DBSession session)
  {
    DBEditObject obj;

    /* -- */

    // assume that log events are not going to change once recorded,
    // so we can make do with a shallow copy.

    this.logEvents = new ArrayList<DBLogEvent>(logEvents);

    objects = new ArrayList<DBCheckPointObj>(transObjects.length);

    for (int i = 0; i < transObjects.length; i++)
      {
        obj = transObjects[i];

        if (debug)
          {
            System.err.println("DBCheckPoint: add " + obj.getLabel() +
                               " (" + obj.getInvid().toString() + ")");
          }

        objects.add(new DBCheckPointObj(obj));
      }

    invidDeleteLocks = DBDeletionManager.getSessionCheckpoint(session);
  }
}

