/*

   timeOutTask.java

   This task allows a Directory Droid administrator to trigger a
   server-side garbage collection cycle through the Directory Droid
   Administration Console.

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


/*------------------------------------------------------------------------------
                                                                           class
                                                                     timeOutTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a periodic sweep of idle users.  Registered with
 * the {@link arlut.csd.ddroid.server.GanymedeScheduler GanymedeScheduler} by
 * {@link arlut.csd.ddroid.server.Ganymede#registerTasks() registerTasks()}, to
 * run every minute.</p>
 *
 * <p>This task implements {@link arlut.csd.ddroid.server.silentTask silentTask}
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
