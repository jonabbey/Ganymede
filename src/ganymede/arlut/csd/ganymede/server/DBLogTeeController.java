/*

   DBLogTeeController.java

   This controller class allows events to be logged to multiple
   controllers.

   Created: 27 August 2014

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                              DBLogTeeController

------------------------------------------------------------------------------*/

/**
 * <p>This controller class allows multiplexing of {@link
 * arlut.csd.ganymede.server.DBLogEvent DBLogEvents} to multiple
 * controllers for the {@link arlut.csd.ganymede.server.DBLog DBLog}
 * class.</p>
 *
 * <p>The first DBLogController in the DBLogTeeController's internal
 * list will be the one selected for retrieving historical events.
 * Events logged with writeEvent will be written to all controllers
 * attached to the DBLogTeeController.</p>
 */

public class DBLogTeeController implements DBLogController {

  final private List<DBLogController> controllers;

  /* -- */

  public DBLogTeeController()
  {
    this.controllers = new ArrayList<DBLogController>();
  }

  public synchronized void addController(DBLogController controller)
  {
    this.controllers.add(controller);
  }

  /**
   * <p>Removes a controller from this DBLogTeeController.  The
   * controller will be flushed and closed before being removed.</p>
   */

  public synchronized void removeController(DBLogController controller)
  {
    if (!controllers.contains(controller))
      {
        throw new IllegalArgumentException("No such controller");
      }

    try
      {
        controller.flushAndSync();
        controller.close();
      }
    finally
      {
        this.controllers.remove(controller);
      }
  }

  /**
   * <p>This method writes the given event to the persistent storage managed by
   * this controller.</p>
   *
   * @param event The DBLogEvent to be recorded
   */

  public synchronized void writeEvent(DBLogEvent event)
  {
    for (DBLogController controller: this.controllers)
      {
        try
          {
            controller.writeEvent(event);
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
          }
      }
  }

  /**
   * <P>This method is used to scan the persistent log storage for log
   * events that match invid and that have occurred since
   * sinceTime.</P>
   *
   * @param invid If not null, retrieveHistory() will only return events involving
   * this object invid.
   *
   * @param sinceTime if not null, retrieveHistory() will only return events
   * occuring on or after the time specified in this Date object.
   *
   * @param beforeTime if not null, retrieveHistory() will only return events
   * occuring on or before the time specified in this Date object.
   *
   * @param keyOnAdmin if true, rather than returning a string containing events
   * that involved &lt;invid&gt;, retrieveHistory() will return a string containing events
   * performed on behalf of the administrator with invid &lt;invid&gt;.
   *
   * @param fullTransactions if true, the buffer returned will include all events in any
   * transactions that involve the given invid.  if false, only those events in a transaction
   * directly affecting the given invid will be returned.
   *
   * @param getLoginEvents if true, this method will return only login
   * and logout events.  if false, this method will return no login
   * and logout events.
   *
   * @return A human-readable multiline string containing a list of history events
   */

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime,
                                                   Date beforeTime, boolean keyOnAdmin,
                                                   boolean fullTransactions, boolean getLoginEvents)
  {
    DBLogController firstController = this.controllers.get(0);

    return firstController.retrieveHistory(invid, sinceTime,
                                           beforeTime, keyOnAdmin,
                                           fullTransactions, getLoginEvents);
  }

  /**
   * <p>This method flushes the log and syncs it to disk.  May be
   * no-op for some controllers.</p>
   */

  public synchronized void flushAndSync()
  {
    for (DBLogController controller: this.controllers)
      {
        try
          {
            controller.flushAndSync();
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
          }
      }
  }

  /**
   * <p>Shuts down this controller, freeing up any resources used by
   * this controller.</p>
   *
   * <p>All controllers added to this DBLogTeeController are removed
   * after they are closed.</p>
   */

  public synchronized void close()
  {
    for (DBLogController controller: this.controllers)
      {
        try
          {
            controller.close();
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
          }
      }

    this.controllers.clear();
  }
}
