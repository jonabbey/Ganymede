/*

   InvidAllocator.java

   This interface is used by the Invid class to provide an Invid when
   given a short type number and an int object number.  By using an
   InvidAllocator, the server will be able to reuse previously created
   Invid's, much as the JVM can reuse interned strings.
   
   Created: 6 January 2005

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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

package arlut.csd.ganymede.common;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  InvidAllocator

------------------------------------------------------------------------------*/

/**
 * <p>This interface is used by the Invid class to provide an Invid when
 * given a short type number and an int object number.  By using an
 * InvidAllocator, the server will be able to reuse previously created
 * Invid's, much as the JVM can reuse interned strings.</p>
 */

public interface InvidAllocator {

  /**
   * <p>This method takes the identifying elements of an Invid to be found, and searches
   * to find a suitable Invid object to return.  If one cannot found, null should be
   * returned, in which case {@link arlut.csd.ganymede.common.Invid#createInvid(short,int) createInvid}
   * will synthesize and return a new one.
   */

  public Invid findInvid(Invid matchInvid);

  /**
   * <p>This method takes the invid given and places in whatever
   * storage mechanism is appropriate, if any, for findInvid() to
   * later draw upon.</p>
   */

  public void storeInvid(Invid newInvid);
}
