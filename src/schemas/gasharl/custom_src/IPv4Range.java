/*
   GASH 2

   IPv4Range.java

   Created: 4 April 2001
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2001/04/04 22:39:23 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.custom;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPv4Range

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to store and manipulate a definition for a sequence
 * of IPv4 addresses, with support for conversion to/from a textual format,
 * for generating an enumeration of addresses in the IPv4Range, and more.</p>
 */

public class IPv4Range {

  Vector stanzas = null;

  /* -- */

  public IPv4Range()
  {
  }
  
  public IPv4Range(String initValue)
  {
  }

  public synchronized void setRange(String value)
  {
    this.stanzas = new Vector();

    
  }
}
