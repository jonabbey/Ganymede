/*
   booleanSempahore.java

   Handy, simple synchronized flag class

   Created: 29 March 2001
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2001/03/29 06:40:56 $
   Release: $Name:  $

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                booleanSempahore

------------------------------------------------------------------------------*/

/**
 * <p>Simple, synchronized boolean flag class.</p>
 *
 * <p>This class is useful for providing a reliable boolean flag that can
 * be examined by separate threads without worry over funky memory model behavior
 * on multiprocessor systems, etc.</p>
 */

public class booleanSempahore {

  private boolean state;

  public booleanSempahore(boolean initialState)
  {
    this.state = initialState;
  }

  public synchronized boolean isSet()
  {
    return state;
  }

  public synchronized void set(boolean b)
  {
    this.state = b;
  }
}
