/*

   DBLogController.java

   This controller interface handles the recording and retrieval of
   events from persistent storage.
   
   Created: 18 February 2003
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2003/02/19 04:28:38 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 DBLogController

------------------------------------------------------------------------------*/

/**
 * <p>This controller interface manages the recording and retrieval of
 * {@link arlut.csd.ganymede.DBLogEvent DBLogEvents} for the {@link
 * arlut.csd.ganymede.DBLog DBLog} class.</p>
 *
 * @version $Revision: 1.1 $ $Date: 2003/02/19 04:28:38 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface DBLogController {

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
   * @param keyOnAdmin if true, rather than returning a string containing events
   * that involved &lt;invid&gt;, retrieveHistory() will return a string containing events
   * performed on behalf of the administrator with invid &lt;invid&gt;.
   *
   * @param fullTransactions if true, the buffer returned will include all events in any
   * transactions that involve the given invid.  if false, only those events in a transaction
   * directly affecting the given invid will be returned.
   *
   * @return A human-readable multiline string containing a list of history events
   */

  public StringBuffer retrieveHistory(Invid invid, Date sinceTime, boolean keyOnAdmin, boolean fullTransactions);
}
