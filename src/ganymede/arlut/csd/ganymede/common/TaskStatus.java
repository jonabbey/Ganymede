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

public enum TaskStatus {

  // Java Enum classes don't allow us to declare static fields at the
  // top of the declaration.
  //
  // The following comment line is required for 'ant validate' to work
  // in the Ganymede build system, even though we don't declare ts
  // until the end of the enum.
  //
  // TranslationService.getTranslationService("arlut.csd.ganymede.common.TaskStatus");

  /**
   * OK used for scheduled, manual, builder, unscheduled builder,
   * syncfullstate, syncmanual TaskTypes.
   *
   * Indicates an uneventful condition, with everything as it should
   * be.
   */

  OK()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Good"
	  return ts.l("ok");
	}
    },

  /**
   * EMPTYQUEUE only used for SYNCINCREMENTAL.  Has the connotation
   * of OK, but specifically indicates that the queue is empty for
   * an incremental sync channel.
   */

  EMPTYQUEUE()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Good, Queue is empty"
	  return ts.l("emptyQueue");
	}
    },

  /**
   * NONEMPTYQUEUE only used for SYNCINCREMENTAL.  Has the
   * connotation of OK, but specifically indicates that the queue is
   * not empty for an incremental sync channel.
   */

  NONEMPTYQUEUE()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Good, Queue size is {0,number,#}"
	  return ts.l("nonEmptyQueue", queueSize);
	}
    },

  /**
   * STUCKQUEUE only used for SYNCINCREMENTAL.  Has the connotation
   * of SERVICEERROR, SERVICEFAIL, or FAIL, but specifically
   * indicates that the queue is not being successfully serviced,
   * meaning that the lowest numbered transaction in the queue
   * remained in the queue after the service was last run.
   */
    
  STUCKQUEUE()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Stuck, Queue size is {0,number,#}. {1}"
	  return ts.l("stuckQueue", queueSize, condition);
	}
    },

  /**
   * SERVICEERROR is used for BUILDER, UNSCHEDULEDBUILDER,
   * SYNCFULLSTATE, SYNCMANUAL, and indicates that the service
   * program associated with this task could not be successfully
   * started, due to a missing service program or a permissions
   * error.
   */

  SERVICEERROR()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Service program could not be run: {0}"
	  return ts.l("serviceError", condition);
	}
    },

  /**
   * SERVICEFAIL is used for BUILDER, UNSCHEDULEDBUILDER,
   * SYNCFULLSTATE, SYNCMANUAL, and indicates that the service
   * program associated with this task was able to be started, but
   * exited with a failure code.
   */

  SERVICEFAIL()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Service program failure: {0}"
	  return ts.l("serviceFail", condition);
	}
    },

  /**
   * FAIL can be used for any task type, and indicates some kind of
   * unexpected exception was thrown.
   */

  FAIL()
    {
      @Override public String getMessage(int queueSize, String condition)
	{
	  // "Error: {0}"
	  return ts.l("fail", condition);
	}
    };

  TaskStatus()
    {
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

  abstract public String getMessage(int queueSize, String condition);

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.TaskStatus");
}
