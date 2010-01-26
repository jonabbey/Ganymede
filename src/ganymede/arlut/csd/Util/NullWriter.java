/*

   NullWriter.java

   Created: 2 February 2009


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

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      NullWriter

------------------------------------------------------------------------------*/

/**
 * This class provides a 'no-op' Writer subclass.
 *
 * It is intended to perform the equivalent of writing to /dev/null on
 * Unix systems, for software components that are designed to write to
 * a Writer but which may also perform desired side-effects at the
 * same time.
 *
 * By using NullWriter, such a component can be run once against the
 * NullWriter to capture the side effects, then again at a later point
 * against a functioning Writer to perform the actual I/O.
 */

public class NullWriter extends java.io.Writer {

  public NullWriter()
  {
  }

  public void close()
  {
  }

  public void flush()
  {
  }

  public void write (char[] cbuf, int off, int len)
  {
  }
}
