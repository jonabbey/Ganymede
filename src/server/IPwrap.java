/*

   IPwrap.java

   This is a wrapper class for the Byte array IP value representation
   that allows IP addresses to be processed by the Ganymede namespace
   hash system.
   
   Created: 20 May 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
