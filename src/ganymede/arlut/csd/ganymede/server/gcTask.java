/*

   gcTask.java

   This task allows a Ganymede administrator to trigger a
   server-side garbage collection cycle through the Ganymede
   Administration Console.

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

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          gcTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a synchronous garbage collection run.  Issued
 * by the {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.</p>
 *
 * <p>I'm not sure that there is any point to having a synchronous garbage
 * collection task.. the idea was that we could schedule a full gc when
 * the server was likely not to be busy so as to keep things trim for when
 * the server was busy, but the main() entry point isn't yet scheduling this
 * for a particularly good time.</p>
 */

class gcTask implements Runnable {

  /**
   * <p>Localization resouce bundle for this class.</p>
   */

  private static TranslationService ts = null;

  public gcTask()
  {
    if (ts == null)
      {
        ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.gcTask");
      }
  }

  public void run()
  {
    Ganymede.debug(ts.l("running"));
    System.gc();
    Ganymede.debug(ts.l("completed"));
    GanymedeAdmin.updateMemState(Runtime.getRuntime().freeMemory(),
                                 Runtime.getRuntime().totalMemory());
  }
}
