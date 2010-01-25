/*

   dumpAndArchiveTask.java

   Runnable class to do a journal sync and zip archive creation of our
   data store.  Issued by the GanymedeScheduler

   Created: 20 May 2004


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

import java.io.IOException;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                              dumpAndArchiveTask

------------------------------------------------------------------------------*/

/**
 * Runnable class to do a journal sync.  Issued by the 
 * {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.
 */

class dumpAndArchiveTask implements Runnable {

  private static TranslationService ts = null;

  public dumpAndArchiveTask()
  {
    if (dumpAndArchiveTask.ts == null)
      {
	dumpAndArchiveTask.ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.dumpAndArchiveTask");
      }
  }

  public void run()
  {
    boolean started = false;
    boolean completed = false;

    /* -- */

    try
      {
        String error = GanymedeServer.lSemaphore.increment();
	    
        if (error != null)
          {
            Ganymede.debug(ts.l("semaphore_disabled", error));
            return;
          }

        try
          {
            started = true;
            Ganymede.debug(ts.l("running"));

            try
              {
                Ganymede.db.dump(Ganymede.dbFilename, true, true);
              }
            catch (IOException ex)
              {
                Ganymede.debug(ts.l("dump_error", ex.getMessage()));
              }
            catch (InterruptedException ex)
              {
                Ganymede.debug(ts.l("dump_interrupted_error", ex.getMessage()));
              }

            completed = true;
          }
        finally
          {
            // we'll go through here if our task was stopped
            // note that the DBStore dump code will handle
            // thread death ok.

            if (started && !completed)
              {
                Ganymede.debug(ts.l("forced_stop"));
              }

            GanymedeServer.lSemaphore.decrement();
          }
      }
    finally
      {
	Ganymede.debug(ts.l("completed"));
      }
  }
}
