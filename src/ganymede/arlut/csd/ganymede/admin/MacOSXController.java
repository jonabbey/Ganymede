/*
   MacOSXController.java

   Ganymede Admin console Macintosh event handler

   Created: 9 January 2010

   Module By: Jonathan Abbey

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

package arlut.csd.ganymede.admin;

/*------------------------------------------------------------------------------
                                                                           class
								MacOSXController

------------------------------------------------------------------------------*/

/**
 * Controller class to handle actions initiated on a Mac from the
 * Applications menu.
 *
 * This class (and the methods in it) have to be public so that the
 * com.apple.eawt.Application class can call these methods.
 */

public class MacOSXController {

  private static GASHAdminFrame frame = null;

  public MacOSXController(GASHAdminFrame frame)
  {
    this.frame = frame;
  }

  public void handleAbout()
  {
    frame.showAboutMessage();
  }

  public void handleQuit()
  {
    frame.disconnect();
  }
}
