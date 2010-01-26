/*

   Queue.java

   Convenient subclass of java.util.Vector that can act as a simple
   FIFO queue.
   
   Created: 2 June 2005


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Directory Management System
 
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
                                                                           Queue

------------------------------------------------------------------------------*/

/**
 * Simple subclass of java.util.Vector that implements a FIFO queue.
 */

public class Queue extends java.util.Vector {

  public Queue()
  {
  }

  public Queue(int initialCapacity)
  {
    super(initialCapacity);
  }

  public Queue(int initialCapacity, int capacityIncrement)
  {
    super(initialCapacity, capacityIncrement);
  }

  public void enqueue(Object item)
  {
    insertElementAt(item, 0);
  }

  public synchronized Object dequeue()
  {
    int len = size();

    if (len == 0)
      {
	return null;
      }

    Object result = elementAt(len - 1);
    removeElementAt(len - 1);

    return result;
  }
}
