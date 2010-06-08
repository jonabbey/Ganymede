/*

   TaskStatus.java

   Created: 7 June 2010

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

package arlut.csd.ganymede.common;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           enum
                                                                      TaskStatus

------------------------------------------------------------------------------*/

/**
 * Data carrier used for conveying the status of a Task in the
 * Ganymede scheduler.
 *
 * Note that we don't use a Java 5 enum because they don't work so hot
 * in the presence of serialization / RMI usage.
 */

public class TaskStatus implements java.io.Serializable {

  static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.TaskStatus");

  /**
   * OK used for scheduled, manual, builder, unscheduled builder,
   * syncfullstate, syncmanual TaskTypes.
   *
   * Indicates an uneventful condition, with everything as it should
   * be.
   */

  static public final int OK = 0;

  /**
   * EMPTYQUEUE only used for SYNCINCREMENTAL.  Has the connotation
   * of OK, but specifically indicates that the queue is empty for
   * an incremental sync channel.
   */

  static public final int EMPTYQUEUE = 1;

  /**
   * NONEMPTYQUEUE only used for SYNCINCREMENTAL.  Has the
   * connotation of OK, but specifically indicates that the queue is
   * not empty for an incremental sync channel.
   */

  static public final int NONEMPTYQUEUE = 2;

  /**
   * STUCKQUEUE only used for SYNCINCREMENTAL.  Has the connotation
   * of SERVICEERROR, SERVICEFAIL, or FAIL, but specifically
   * indicates that the queue is not being successfully serviced,
   * meaning that the lowest numbered transaction in the queue
   * remained in the queue after the service was last run.
   */

  static public final int STUCKQUEUE = 3;

  /**
   * SERVICEERROR is used for BUILDER, UNSCHEDULEDBUILDER,
   * SYNCFULLSTATE, SYNCMANUAL, and indicates that the service
   * program associated with this task could not be successfully
   * started, due to a missing service program or a permissions
   * error.
   */

  static public final int SERVICEERROR = 4;

  /**
   * SERVICEFAIL is used for BUILDER, UNSCHEDULEDBUILDER,
   * SYNCFULLSTATE, SYNCMANUAL, and indicates that the service
   * program associated with this task was able to be started, but
   * exited with a failure code.
   */

  static public final int SERVICEFAIL = 5;

  /**
   * FAIL can be used for any task type, and indicates some kind of
   * unexpected exception was thrown.
   */

  static public final int FAIL = 6;
  static public final int LAST = 6;

  static private final TaskStatus ok = new TaskStatus(OK);
  static private final TaskStatus emptyq = new TaskStatus(EMPTYQUEUE);
  static private final TaskStatus nonemptyq = new TaskStatus(NONEMPTYQUEUE);
  static private final TaskStatus stuckq = new TaskStatus(STUCKQUEUE);
  static private final TaskStatus serviceerror = new TaskStatus(SERVICEERROR);
  static private final TaskStatus servicefail = new TaskStatus(SERVICEFAIL);


  static public TaskStatus get(int type)
  {
    switch (type)
    {
    case OK:
      return ok;

    case EMPTYQUEUE:
      return emptyq;

    case NONEMPTYQUEUE:
      return nonemptyq;

    case STUCKQUEUE:
      return stuckq;

    case SERVICEERROR:
      return serviceerror;

    case SERVICEFAIL:
      return servicefail;
    }

    throw new IllegalArgumentException();
  }

  // --

  private int type;

  private TaskStatus(int type)
  {
    if (type < 0 || type > LAST)
      {
	throw new IllegalArgumentException();
      }

    this.type = type;
  }

  public int getID()
  {
    return type;
  }

  public String getMessage()
  {
    return getMessage(0, "");
  }

  public String getMessage(int queueSize)
  {
    return getMessage(queueSize, "");
  }

  public String getMessage(String condition)
  {
    return getMessage(0, condition);
  }

  public String getMessage(int queueSize, String condition)
  {
    switch (type)
      {
      case OK:
	// "Good"
	return ts.l("ok");

      case EMPTYQUEUE:
	// "Good, Queue is empty"
	return ts.l("emptyQueue");

      case NONEMPTYQUEUE:
	// "Good, Queue size is {0,number,#}"
	return ts.l("nonEmptyQueue", queueSize);

      case STUCKQUEUE:
	// "Stuck, Queue size is {0,number,#}. {1}"
	return ts.l("stuckQueue", queueSize, condition);

      case SERVICEERROR:
	// "Service program could not be run: {0}"
	return ts.l("serviceError", condition);

      case SERVICEFAIL:
	// "Service program failure: {0}"
	return ts.l("serviceFail", condition);

      case FAIL:
	  // "Error: {0}"
	  return ts.l("fail", condition);
      }

    throw new IllegalArgumentException();
  }
}
