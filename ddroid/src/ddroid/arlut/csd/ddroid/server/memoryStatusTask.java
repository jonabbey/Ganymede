/*

   memoryStatusTask.java

   This task is executed by the Directory Droid scheduler periodically
   to update the server's memory statistics on any attached
   administration consoles.

   Created: 20 May 2004

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ddroid.server;

import java.util.Date;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                memoryStatusTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to update the memory status fields in the admin
 * console.  Registered with the {@link
 * arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler} by {@link
 * arlut.csd.ddroid.server.Ganymede#registerTasks() registerTasks()}, to
 * run every minute.</p> 
 *
 * <p>This task implements {@link arlut.csd.ddroid.server.silentTask silentTask}
 * in order to signal the GanymedeScheduler not to print anything to the console
 * when the task is run.</p>
 */

class memoryStatusTask implements Runnable, silentTask {

  /**
   * <p>The debug flag in memoryStatusTask is used to control
   * whether memoryStatusTask will log memory usage to Ganymede's
   * standard error log.  This is useful for tracking memory usage patterns.</p>
   */

  static final boolean debug = true;

  /**
   * <p>The period value is used to set how often the memory
   * statistics will be logged to Ganymede's standard error
   * log.  This period value is counted in terms of the number
   * of runs of the memoryStatusTask.  By default, memoryStatusTask
   * is run once a minute from the Directory Droid scheduler, so the
   * period count is minutes.</p>
   */

  static final int period = 15;

  /**
   * <p>Localization resouce bundle for this class.</p>
   */

  private static TranslationService ts = null;

  static int count = 0;

  // ---

  public memoryStatusTask()
  {
    if (ts == null)
      {
	ts = TranslationService.getTranslationService("arlut.csd.ddroid.server.memoryStatusTask");
      }
  }

  public void run()
  {
    Runtime rt = Runtime.getRuntime();

    /* -- */

    if (debug)
      {
	if (count == period)
	  {
	    count = 0;
	  }
	
	if (count == 0)
	  {
	    Ganymede.debug(ts.l("status_dump",
				new Date(),
				new Long(rt.totalMemory() - rt.freeMemory()),
				new Long(rt.freeMemory()),
				new Long(rt.totalMemory())));
	  }
      }

    GanymedeAdmin.updateMemState(rt.freeMemory(), rt.totalMemory());

    if (debug)
      {
	count++;
      }
  }
}
