/*

   DBLogController.java

   This controller interface handles the recording and retrieval of
   events from persistent storage.

   Created: 18 February 2003

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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

import java.util.Date;

import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 DBLogController

------------------------------------------------------------------------------*/

/**
 * <p>This controller interface manages the recording and retrieval of
 * {@link arlut.csd.ganymede.server.DBLogEvent DBLogEvents} for the {@link
 * arlut.csd.ganymede.server.DBLog DBLog} class.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface DBLogController extends java.io.Closeable {

  /**
   * <p>This method writes the given event to the persistent storage managed by
   * this controller.</p>
   *
   * @param event The DBLogEvent to be recorded
   */

  public void writeEvent(DBLogEvent event);

  /**
   * <P>This method is used to scan the persistent log storage for log events that match
   * invid and that have occurred since sinceTime.</P>
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

  public StringBuffer retrieveHistory(Invid invid, Date sinceTime, Date beforeTime, boolean keyOnAdmin, boolean fullTransactions, boolean getLoginEvents);

  /**
   * <p>This method flushes the log and syncs it to disk.  May be
   * no-op for some controllers.</p>
   */

  public void flushAndSync();

  /**
   * <p>This method shuts down this controller, freeing up any resources used by this
   * controller.</p>
   */

  public void close();
}
