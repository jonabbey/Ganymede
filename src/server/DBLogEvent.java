/*

   DBLogEvent.java

   This class stores a complete record of a single sub-transactional event,
   to be emitted to the DBLog log file, or sent to a set of users via
   email..
   
   Created: 31 October 1997
   Release: $Name:  $
   Version: $Revision: 1.21 $
   Last Mod Date: $Date: 2003/02/27 00:01:49 $
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

import java.io.*;
import java.util.*;
import arlut.csd.Util.*;

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
  Vector objects;
  Vector notifyVect;
  String notifyList;

  boolean augmented = false;

  /* -- */

  public DBLogEvent()
  {
  }

  /**
   *
   * Constructor to be used for a mailout event.
   *
   * @param addresses A vector of Strings listing email addresses to send notification
   * of this event to.
   * @param eventClassToken a short string specifying a DBObject
   * record describing the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A vector of invids of objects involved in this event.
   */

  public DBLogEvent(Vector addresses, String subject, String description,
		    Invid admin, String adminName,
		    Vector objects)
  {
    this("mailout", description, admin, adminName, objects, addresses);

    this.subject = subject;
  }

  /**
   *
   * @param eventClassToken a short string specifying a DBObject
   * record describing the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A vector of invids of objects involved in this event.
   * @param notifyVec A vector of Strings listing email addresses to send notification
   * of this event to.
   * 
   */

  public DBLogEvent(String eventClassToken, String description,
		    Invid admin, String adminName,
		    Vector objects, Vector notifyVec)
  {
    this.eventClassToken = eventClassToken;
    this.description = description;
    this.admin = admin;
    this.adminName = adminName;
    this.objects = objects;

    setMailTargets(notifyVec);
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
   *
   * This method is used by DBLog to set the list of email targets
   * that this event will need to be mailed to.
   * 
   */

  public void setMailTargets(Vector mailTargets)
  {
    this.notifyVect = mailTargets;

    if (mailTargets == null)
      {
	notifyList = "";
	return;
      }

    // we want to set the notifyList String as well as the
    // notifyVect vector..

    StringBuffer buf = new StringBuffer();
    
    for (int i = 0; i < notifyVect.size(); i++)
      {
	if (i > 0)
	  {
	    buf.append(", ");
	  }
	    
	buf.append((String) notifyVect.elementAt(i));
      }
    
    this.notifyList = buf.toString();
  }

  /**
   *
   * Debug rig.. this will scan a log file and attempt to parse lines out of it
   *
   */

  static public void main(String argv[])
  {
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
