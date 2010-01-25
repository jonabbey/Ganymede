/*
   FoxtrotAdapter.java

   Adapter which can apply the foxtrot library to handle certain long
   running tasks in a way which will not block the GUI thread.

   Created: 10 April 2009


   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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

package arlut.csd.ganymede.client;

import foxtrot.Task;
import foxtrot.Worker;

/*---------------------------------------------------------------------
                                                                  class 
                                                         FoxtrotAdapter

---------------------------------------------------------------------*/

/**
 * This class takes care of dispatching tasks to Foxtrot (to allow the
 * AWT/Swing event queue to continue to be processed while the task
 * executes) if the "enable.foxtrot" property/parameter is not set to
 * false.
 *
 * Certain Swing look and feel modules are not compatible with the use
 * of foxtrot, and in such cases, it may be necessary to set
 * enable.foxtrot to false.
 */

class FoxtrotAdapter {

  static Boolean useFoxtrot = null;

  public static Object post(Task task)
  {
    if (useFoxtrot == null)
      {
	useFoxtrot = Boolean.valueOf(glogin.getConfigBoolean("enable.foxtrot", true));
      }

    if (useFoxtrot)
      {
	try
	  {
	    return foxtrot.Worker.post(task);
	  }
	catch (java.security.AccessControlException ex)
	  {
	    try
	      {
		return task.run();
	      }
	    catch (Exception ex2)
	      {
		gclient.client.processExceptionRethrow(ex2);
		return null;
	      }
	  }
	catch (Throwable ex)
	  {
	    gclient.client.processExceptionRethrow(ex);
	    return null;
	  }
      }
    else
      {
	try
	  {
	    return task.run();
	  }
	catch (Throwable ex)
	  {
	    gclient.client.processExceptionRethrow(ex);
	    return null;
	  }
      }
  }
}
