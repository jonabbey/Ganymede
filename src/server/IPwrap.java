/*

   IPwrap.java

   This is a wrapper class for the Byte array IP value representation
   that allows IP addresses to be processed by the Ganymede namespace
   hash system.
   
   Created: 20 May 1998
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/22 18:05:47 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          IPwrap

------------------------------------------------------------------------------*/

/**
 *
 * This is a wrapper class for the Byte array IP value representation
 * that allows IP addresses to be processed by the Ganymede namespace
 * hash system.
 *
 */

public class IPwrap {
  
  /**
   *
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.
   *
   */

  public final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  /**
   *
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   *
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }

  // ---

  Byte[] address;

  /* -- */

  public IPwrap(Byte[] address)
  {
    this.address = address;
  }

  /**
   *
   * This method generates the hash key for this object for use in
   * a Hashtable.
   *
   */

  public int hashCode()
  {
    if ((address == null) || (address.length == 0))
      {
	return 0;
      }

    int result = 0;

    try
      {
	for (int i = 0; i < address.length; i++)
	  {
	    result += address[i].intValue();
	  }
      }
    catch (ArithmeticException ex)
      {
	return result;
      }

    return result;
  }

  /**
   *
   * Equality test.  This IPwrap can be compared to either
   * another IPwrap object or to an array of Bytes.
   *
   */

  public boolean equals(Object value)
  {
    Byte[] foreignBytes;

    /* -- */

    if (value == null)
      {
	return false;
      }

    if (value instanceof Byte[])
      {
	foreignBytes = (Byte[]) value;
      }
    else if (value instanceof IPwrap)
      {
	foreignBytes = ((IPwrap) value).address;
      }
    else
      {
	return false;
      }

    if (foreignBytes.length != address.length)
      {
	return false;
      }

    for (int i = 0; i < address.length; i++)
      {
	try
	  {
	    if (!foreignBytes[i].equals(address[i]))
	      {
		return false;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    return false;
	  }
      }

    return true;
  }
}
