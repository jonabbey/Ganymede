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

public enum TaskType {

  // Java Enum classes don't allow us to declare static fields at the
  // top of the declaration.
  //
  // The following comment line is required for 'ant validate' to work
  // in the Ganymede build system, even though we don't declare ts
  // until the end of the enum.
  //
  // TranslationService.getTranslationService("arlut.csd.ganymede.common.TaskType");

  SCHEDULED()
    {
      @Override public String toString()
	{
	  return ts.l("scheduledTask"); // "Scheduled Task"
	}
    },

  MANUAL()
    {
      @Override public String toString()
	{
	  return ts.l("manualTask"); // "On Demand Task"
	}
    },

  BUILDER()
    {
      @Override public String toString()
	{
	  return ts.l("builderTask"); // "Ganymede Builder Task"
	}
    },

  UNSCHEDULEDBUILDER()
    {
      @Override public String toString()
	{
	  return ts.l("unscheduledBuilderTask"); // "Unscheduled Ganymede Builder Task"
	}
    },

  SYNCINCREMENTAL()
    {
      @Override public String toString()
	{
	  return ts.l("incrementalSync"); // "Incremental Sync Channel"
	}
    },

  SYNCFULLSTATE()
    {
      @Override public String toString()
	{
	  return ts.l("fullstateSync"); // "Full State Sync Channel"
	}
    },

  SYNCMANUAL()
    {
      @Override public String toString()
	{
	  return ts.l("manualSync"); // "Manual Sync Channel"
	}
    };

  TaskType()
  {
  }

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.TaskType");
}