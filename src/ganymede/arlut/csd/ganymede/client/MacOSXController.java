/*
   MacOSXController.java

   Ganymede Macintosh event handler

   Created: 16 October 2009

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
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

package arlut.csd.ganymede.client;

/*------------------------------------------------------------------------------
                                                                           class
								MacOSXController

------------------------------------------------------------------------------*/

/**
 * Controller class to handle actions initiated on a Mac from the
 * Applications menu.
 */

public class MacOSXController {

  private static gclient client = null;

  public MacOSXController(gclient client)
  {
    this.client = client;
  }

  public void handleAbout()
  {
    client.showAboutMessage();
  }

  public void handleQuit()
  {
    if (client.OKToProceed())
      {
	client.logout(true);
      }
  }
}
