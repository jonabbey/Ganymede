/*

   timeOutTask.java

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


/*------------------------------------------------------------------------------
                                                                           class
                                                                     timeOutTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a periodic sweep of idle users.  Registered with
 * the {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler} by
 * {@link arlut.csd.ganymede.server.Ganymede#registerTasks() registerTasks()}, to
 * run every minute.</p>
 *
 * <p>This task implements {@link arlut.csd.ganymede.server.silentTask silentTask}
 * in order to signal the GanymedeScheduler not to print anything to the console
 * when the task is run.</p>
 */

class timeOutTask implements Runnable, silentTask {

  public timeOutTask()
  {
  }

  public void run()
  {
    Ganymede.server.clearIdleSessions();
  }
}
