/*

   CircleBuffer.java

   Created: 3 December 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/12/04 03:07:26 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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

  /* -- */

  public CircleBuffer(int size)
  {
    buf = new Object[size];
    firstSlot = 0;
    nextSlot = 0;
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

    buf[nextSlot] = item;

    nextSlot++;

    if (nextSlot == buf.length)
      {
	nextSlot = 0;
      }
  }

  public synchronized String getContents()
  {
    int count = 0;
    int i = firstSlot;

    if (buf[i] == null)
      {
	return "";
      }

    StringBuffer sb = new StringBuffer();

    sb.append(count++);
    sb.append(":");
    sb.append(buf[i].toString());
    sb.append("\n");

    i++; 

    if (i == buf.length)
      {
	i = 0;
      }

    while (i != nextSlot)
      {
	sb.append(count++);
	sb.append(":");
	sb.append(buf[i].toString());
	sb.append("\n");
      }

    return sb.toString();
  }
}
