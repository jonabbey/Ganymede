/*

   TaskType.java

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
                                                                           class
                                                                        TaskType

------------------------------------------------------------------------------*/

/**
 * Data carrier used for conveying the type of a Task in the
 * Ganymede scheduler.
 *
 * Note that we don't use a Java 5 enum because they don't work so hot
 * in the presence of serialization / RMI usage.
 */

public class TaskType implements java.io.Serializable {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.TaskType");

  static public final int SCHEDULED = 0;
  static public final int MANUAL = 1;
  static public final int BUILDER = 2;
  static public final int UNSCHEDULEDBUILDER = 3;
  static public final int SYNCINCREMENTAL = 4;
  static public final int SYNCFULLSTATE = 5;
  static public final int SYNCMANUAL = 6;
  static public final int LAST = 6;

  static private final TaskType scheduled = new TaskType(SCHEDULED);
  static private final TaskType manual = new TaskType(MANUAL);
  static private final TaskType builder = new TaskType(BUILDER);
  static private final TaskType unscheduled = new TaskType(UNSCHEDULEDBUILDER);
  static private final TaskType syncincremental = new TaskType(SYNCINCREMENTAL);
  static private final TaskType syncfullstate = new TaskType(SYNCFULLSTATE);
  static private final TaskType last = new TaskType(SYNCMANUAL);

  static public TaskType get(int type)
  {
    switch (type)
    {
    case SCHEDULED:
      return scheduled;

    case MANUAL:
      return manual;

    case BUILDER:
      return builder;

    case UNSCHEDULEDBUILDER:
      return unscheduled;

    case SYNCINCREMENTAL:
      return syncincremental;

    case SYNCFULLSTATE:
      return syncfullstate;

    case SYNCMANUAL:
      return last;
    }

    throw new IllegalArgumentException();
  }

  private int type;

  private TaskType(int type)
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

  public String toString()
  {
    switch (type)
      {
      case SCHEDULED:
	return ts.l("scheduledTask"); // "Scheduled Task"

      case MANUAL:
	return ts.l("manualTask"); // "On Demand Task"

      case BUILDER:
	return ts.l("builderTask"); // "Ganymede Builder Task"

      case UNSCHEDULEDBUILDER:
	return ts.l("unscheduledBuilderTask"); // "Unscheduled Ganymede Builder Task"

      case SYNCINCREMENTAL:
	return ts.l("incrementalSync"); // "Incremental Sync Channel"

      case SYNCFULLSTATE:
	return ts.l("fullstateSync"); // "Full State Sync Channel"

      case SYNCMANUAL:
	return ts.l("manualSync"); // "Manual Sync Channel"
      }

    throw new IllegalArgumentException();
  }
}
