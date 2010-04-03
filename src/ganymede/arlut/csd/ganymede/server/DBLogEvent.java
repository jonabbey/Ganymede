/*

   DBLogEvent.java

   This class stores a complete record of a single sub-transactional event,
   to be emitted to the DBLog log file, or sent to a set of users via
   email..
   
   Created: 31 October 1997

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 DBLogEvent.java

------------------------------------------------------------------------------*/

/**
 * This class stores a complete record of a single sub-transactional event,
 * to be emitted to the DBLog log file, or sent to a set of users via
 * email..
 */

public class DBLogEvent {

  Date time;
  String transactionID;
  String eventClassToken;
  String description;
  String subject = null;
  String adminName;
  Invid admin;

  private List<Invid> objects;
  private List<String> notifyList = Collections.emptyList();
  
  /**
   * Will be true if this log event has already had its email
   * recipient list expanded in keeping with systemic logging rules.
   */

  boolean augmented = false;

  /* -- */

  public DBLogEvent()
  {
  }

  /**
   * Constructor to be used for a mailout event.  This constructor is
   * used when an email message should be synthesized during the
   * course of a transaction, and transmitted if and only if the
   * transaction is successfully committed.
   *
   * @param addresses A List of Strings listing email addresses to send notification
   * of this event to.
   * @param subject a short string specifying a DBObject
   * record describing the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A List of Invids of objects involved in this event.
   */

  public DBLogEvent(List<String> addresses, String subject, String description,
		    Invid admin, String adminName,
		    List<Invid> objects)
  {
    this("mailout", description, admin, adminName, objects, addresses);

    this.subject = subject;
  }

  /**
   * Constructor
   *
   * @param eventClassToken a short string specifying a DBObject
   * record describing the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A List of Invids of objects involved in this event.
   * @param notifyVec A List of Strings listing email addresses to send notification
   * of this event to.
   */

  public DBLogEvent(String eventClassToken, String description,
		    Invid admin, String adminName,
		    List<Invid> objects, List<String> notifyList)
  {
    this.eventClassToken = eventClassToken;
    this.description = description;
    this.admin = admin;
    this.adminName = adminName;
    this.objects = objects;

    setMailTargets(notifyList);
  }

  public void setTransactionID(String transID)
  {
    this.transactionID = transID;
  }

  public void setLogTime(Date time)
  {
    this.time = time;
  }

  public void setLogTime(long millis)
  {
    this.time = new Date(millis);
  }

  /**
   * Sets the List of Invids affected by this DBLogEvent.
   */

  public void setInvids(Collection<Invid> invids)
  {
    this.objects = new ArrayList<Invid>(invids);
  }

  /**
   * This method is used by DBLog to set the list of email targets
   * that this event will need to be mailed to.
   */

  public void setMailTargets(Collection<String> mailTargets)
  {
    if (mailTargets != null)
      {
	this.notifyList = new ArrayList<String>(mailTargets);
      }
    else
      {
	this.notifyList = Collections.emptyList();
      }
  }

  /**
   * Return the List of Invids affected by this DBLogEvent.
   */

  public List<Invid> getInvids()
  {
    return objects;
  }

  /**
   * Return the List of email recipients that this DBLogEvent should
   * be directed to.
   */

  public List<String> getMailTargets()
  {
    return this.notifyList;
  }

  /**
   * Returns a comma-separated String of addresses that need to
   * receive notification of this log event.
   */

  public String getToString()
  {
    StringBuffer buffer = new StringBuffer();

    for (int i = 0; i < notifyList.size(); i++)
      {
	if (i > 0)
	  {
	    buffer.append(",");
	  }

	buffer.append(notifyList.get(i));
      }

    return buffer.toString();
  }

  public String toString()
  {
    StringWriter writer = new StringWriter();
    PrintWriter pWriter = new PrintWriter(writer);
    DBLogFileController controller = new DBLogFileController(pWriter);

    try
      {
	controller.writeEvent(this);
      }
    finally
      {
	controller.close();
      }

    return writer.toString();
  }
}
