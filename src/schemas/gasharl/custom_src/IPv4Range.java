/*
   GASH 2

   IPv4Range.java

   Created: 4 April 2001
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2001/04/06 00:24:12 $
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

import arlut.csd.Util.StringUtils;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPv4Range

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to store and manipulate a definition for a sequence
 * of IPv4 addresses, with support for conversion to/from a textual format,
 * for generating an enumeration of addresses in the IPv4Range, and more.</p>
 *
 * <p>This class supports continuous and discontinuous IP range specification
 * using a string that comprises multiple lines, each of which has four
 * dot-separated octets which may either be simple numeric values, or
 * a range of values, with the start and stop point for the range separated
 * by a dash and surrounded by brackets.  The iteration across the range
 * implemented by the getElements() Enumeration will vary across the least
 * significant byte of ranged stanzas first.</p>
 *
 * <p>In other words, the following examples would be legal:</p>
 *
 * <pre>10.2.[12-21].[1-253]
 *      10.5.11.[1-100]</pre>
 *
 * <p>which would start at 10.2.12.1, then 10.2.12.2, up to 10.2.12.253, then
 * to 10.2.13.1, on up to 10.2.21.253, and then to 10.5.11.1 and finishing
 * up with 10.5.11.100.</p>
 *
 * <p>and</p>
 *
 * <pre>129.116.[224-226].[253-1]</pre>
 *
 * <p>which will start at 129.116.224.253, then 129.116.224.252, and ending
 * up at 129.116.226.1</p>
 */

public class IPv4Range {

  private byte[][][] range = null;

  /* -- */

  public IPv4Range()
  {
  }
  
  public IPv4Range(String initValue)
  {
    this.setRange(initValue);
  }

  public synchronized byte[][][] getByteArray()
  {
    if (range == null)
      {
	return null;
      }

    byte[][][] _range = new byte[range.length][4][2];

    for (int i = 0; i < range.length; i++)
      {
	for (int j = 0; j < 4; j++)
	  {
	    for (int k = 0; k < 2; k++)
	      {
		_range[i][j][k] = range[i][j][k];
	      }
	  }
      }

    return _range;
  }

  public synchronized void setRange(String value)
  {
    String lines[] = StringUtils.split(value, "\n");

    byte[][][] _range = new byte[lines.length][4][2];

    for (int i = 0; i < lines.length; i++)
      {
	String octets[] = StringUtils.split(lines[i], ".");

	if (octets.length != 4)
	  {
	    throw new IllegalArgumentException("Error, line '" + lines[i] +
					       "' does not contain four dot-separated byte specifiers");
	  }

	for (int j = 0; j < octets.length; j++)
	  {
	    if (!StringUtils.containsOnly(octets[j], "0123456789[-]"))
	      {
		throw new IllegalArgumentException("Error, line '" + lines[i] +
						   "' contains invalid characters in byte " + j);
	      }

	    if (octets[j].indexOf("[") == -1 && octets[j].indexOf("]") == -1 && octets[j].indexOf("-") == -1)
	      {
		// simple fixed value

		int val = Integer.parseInt(octets[j]);

		if (val > 255)
		  {
		    throw new IllegalArgumentException("Error, line '" + lines[i] + "' byte " + j + " is out of range.");
		  }

		_range[i][j][0] = u2s(val);
		_range[i][j][1] = _range[i][j][0];
	      }
	    else
	      {
		// assume a bracketed byte range

		if (!(octets[j].charAt(0) == '[' && octets[j].charAt(octets[j].length()-1) == ']'))
		  {
		    throw new IllegalArgumentException("Error, line '" + lines[i] +
						       "' contains a formatting error in byte " + j);
		  }

		String strippedString = octets[j].substring(1, octets[j].length()-1);

		String endpoints[] = StringUtils.split(strippedString, "-");

		if (endpoints.length != 2)
		  {
		    throw new IllegalArgumentException("Error, line '" + lines[i] + 
						       "' contains a formatting error in byte " + j);
		  }

		for (int k = 0; k < endpoints.length; k++)
		  {
		    int val = Integer.parseInt(endpoints[k]);

		    if (val > 255)
		      {
			throw new IllegalArgumentException("Error, line '" + lines[i] + 
							   "' contains a range error in byte " + j);
		      }

		    _range[i][j][k] = u2s(val);
		  }
	      }
	  }
      }

    if (!sanityCheck(_range))
      {
	throw new IllegalArgumentException("Error, overlapping entries");
      }

    range = _range;
  }

  public synchronized String toString()
  {
    StringBuffer result = new StringBuffer();
    boolean started;

    /* -- */

    for (int i = 0; i < range.length; i++)
      {
	if (i > 0)
	  {
	    result.append("\n");
	  }

	for (int j = 0; j < 4; j++)
	  {
	    if (j > 0)
	      {
		result.append(".");
	      }

	    if (range[i][j][0] == range[i][j][1])
	      {
		result.append(s2u(range[i][j][0]));
	      }
	    else
	      {
		result.append("[");
		result.append(s2u(range[i][j][0]));
		result.append("-");
		result.append(s2u(range[i][j][1]));
		result.append("]");
	      }
	  }
      }

    return result.toString();
  }

  public Enumeration getElements()
  {
    return new IPv4RangeEnumerator(this);
  }

  private boolean sanityCheck(byte _range[][][])
  {
    for (int i = 0; i < _range.length; i++)
      {
	for (int j = i+1; j < _range.length; j++)
	  {
	    if (byteOverlap(_range[i][0], _range[j][0]) &&
		byteOverlap(_range[i][1], _range[j][1]) &&
		byteOverlap(_range[i][2], _range[j][2]) &&
		byteOverlap(_range[i][3], _range[j][3]))
	      {
		return false;
	      }
	  }
      }
    
    return true;
  }

  private boolean byteOverlap(byte pair1[], byte pair2[])
  {
    if (pair1.length != 2)
      {
	throw new IllegalArgumentException("pair1 length is wrong: " + pair1.length);
      }

    if (pair2.length != 2)
      {
	throw new IllegalArgumentException("pair2 length is wrong: " + pair2.length);
      }

    if (pair1[0] == pair1[1])
      {
	if ((pair2[0] <= pair1[0] && pair2[1] >= pair1[0]) ||
	    (pair2[0] >= pair1[0] && pair2[1] <= pair1[0]))
	  {
	    return true;
	  }

	return false;
      }

    int firstLow = 0;
    int firstHigh = 0;

    if (pair1[0] < pair1[1])
      {
	firstLow = pair1[0];
	firstHigh = pair1[1];
      }
    else
      {
	firstLow = pair1[1];
	firstHigh = pair1[0];
      }

    int secondLow = 0;
    int secondHigh = 0;

    if (pair2[0] < pair2[1])
      {
	secondLow = pair2[0];
	secondHigh = pair2[1];
      }
    else
      {
	secondLow = pair2[1];
	secondHigh = pair2[0];
      }

    if (secondLow <= firstLow && secondHigh >= firstLow)
      {
	return true;
      }

    if (secondLow <= firstHigh && secondHigh >= firstHigh)
      {
	return true;
      }

    if (secondLow >= firstLow && secondHigh <= firstHigh)
      {
	return true;
      }

    return false;
  }

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

  public final static int s2u(byte b)
  {
    return (int) (b + 128);
  }

  public static void main(String argv[])
  {
    String x = "10.8.[100-95].[1-25]\n10.7.[12-15].[99-70]\n129.116.224.14";

    IPv4Range range = new IPv4Range(x);

    Enumeration enum = range.getElements();

    while (enum.hasMoreElements())
      {
	System.out.println(arlut.csd.ganymede.IPDBField.genIPV4string((Byte[]) enum.nextElement()));
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                             IPv4RangeEnumerator

------------------------------------------------------------------------------*/

/**
 * <P>This class handles enumerating across a range of IP addresses specified
 * in an {@arlut.csd.ganymede.custom.IPv4Range IPv4Range} object.  An
 * IPv4RangeEnumerator is created as a snapshot of the state of an IPv4Range object,
 * and may be iterated over even if the source IPv4Range object is modified
 * after it has been created.</P>
 */

final class IPv4RangeEnumerator implements Enumeration {

  private byte[][][] range = null;

  private int line = 0;

  private int index[] = new int[4];

  /* -- */

  IPv4RangeEnumerator(IPv4Range v4Range) {
    range = v4Range.getByteArray();

    for (int i = 0; i < 4; i++)
      {
	index[i] = 0;
      }
  }

  public boolean hasMoreElements() {
    return line < range.length;
  }
  
  public synchronized Object nextElement() 
  {
    Byte[] address;

    /* -- */

    if (line >= range.length)
      {
	throw new NoSuchElementException("IPv4RangeEnumerator");
      }

    // create the next address to return

    address = new Byte[4];

    for (int i = 0; i < 4; i++)
      {
	address[i] = new Byte((byte) (range[line][i][0] + index[i]));
      }

    // now update our range index variables to find the set of
    // offsets for the next address in our sequence

    boolean incremented = false;

    for (int i = 3; !incremented && i >= 0; i--)
      {
	if (range[line][i][1] > range[line][i][0])
	  {
	    index[i]++;

	    if (index[i] > (range[line][i][1] - range[line][i][0]))
	      {
		for (int j = i; j < 4; j++)
		  {
		    index[j] = 0;
		  }
	      }
	    else
	      {
		incremented = true;
	      }
	  }
	else if (range[line][i][1] < range[line][i][0])
	  {
	    index[i]--;

	    if (index[i] < (range[line][i][1] - range[line][i][0]))
	      {
		for (int j = i; j < 4; j++)
		  {
		    index[j] = 0;
		  }
	      }
	    else
	      {
		incremented = true;
	      }
	  }
      }

    if (!incremented)
      {
	line++;
      }

    return address;
  }

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

  public final static int s2u(byte b)
  {
    return (int) (b + 128);
  }
}
