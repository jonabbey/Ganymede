/*

   CircleBuffer.java

   Created: 3 December 2000

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
                                                                    CircleBuffer

------------------------------------------------------------------------------*/

/**
 * <P>This class defines a fixed size circular buffer that can be used to
 * keep references to the last <i>n</i> objects added to the buffer.</P>
 *
 * <P>This class is intended to be used as a debugging tool, so there are
 * no methods to dequeue items, just to add items and to display the last <i>n</i>
 * items submitted.</P>
 */

public class CircleBuffer {

  private Object[] buf;
  private int firstSlot;
  private int nextSlot;
  private int contents = 0;

  /* -- */

  public CircleBuffer(int size)
  {
    buf = new Object[size];
    firstSlot = 0;
    nextSlot = 0;
  }

  /**
   * Returns the number of elements loaded into this CircleBuffer.
   */

  public synchronized int getSize()
  {
    return contents;
  }

  public synchronized void add(Object item)
  {
    if (item == null)
      {
	throw new IllegalArgumentException("no null's allowed");
      }

    if (buf[firstSlot] != null && nextSlot == firstSlot)
      {
	firstSlot++;

	if (firstSlot == buf.length)
	  {
	    firstSlot = 0;
	  }
      }

    buf[nextSlot++] = item;

    if (contents < buf.length)
      {
	contents++;
      }

    if (nextSlot == buf.length)
      {
	nextSlot = 0;
      }
  }

  public synchronized String getContents()
  {
    int count = 0;
    int i = firstSlot;

    // once the buffer is filled to capacity, firstSlot will be equal
    // to nextSlot.. the way we distinguish that case from the empty
    // buffer case is by seeing if firstSlot contains null

    if (buf[i] == null)
      {
	return "";
      }

    // okay, we know there is at least one item in the buffer.. create
    // a StringBuilder and add information for it to the StringBuilder

    StringBuilder sb = new StringBuilder();

    sb.append(count++);
    sb.append(":");
    sb.append(buf[i].toString());
    sb.append("\n");

    i++; 

    if (i == buf.length)
      {
	i = 0;
      }

    // at this point, we have moved i to the slot following
    // firstSlot.. until 
    
    while (i != nextSlot)
      {
	sb.append(count++);
	sb.append(":");
	sb.append(buf[i].toString());
	sb.append("\n");

	i++;

	if (i == buf.length)
	  {
	    i = 0;
	  }
      }

    return sb.toString();
  }
}
