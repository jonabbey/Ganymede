/*

   IPwrap.java

   This is a wrapper class for the Byte array IP value representation
   that allows IP addresses to be processed by the Ganymede namespace
   hash system.
   
   Created: 20 May 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2010
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

package arlut.csd.ganymede.server;


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

  Byte[] address;

  /* -- */

  public IPwrap(Byte[] address)
  {
    this.address = address;
  }

  public IPwrap(String addressStr)
  {
    if (addressStr.indexOf(':') == -1)
      {
	this.address = IPDBField.genIPV4bytes(addressStr);
      }
    else
      {
	this.address = IPDBField.genIPV6bytes(addressStr);
      }
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

    if (value instanceof IPwrap)
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

  /**
   * <p>Getter method.  This method creates a copy of the address
   * before returning it, to avoid the calling function messing with
   * the internally editable array representation.</p>
   */

  public Byte[] getAddress()
  {
    Byte[] result = new Byte[this.address.length];

    System.arraycopy(this.address, 0, result, 0, this.address.length);

    return result;
  }

  public String toString()
  {
    return IPDBField.genIPString(address);
  }
}
