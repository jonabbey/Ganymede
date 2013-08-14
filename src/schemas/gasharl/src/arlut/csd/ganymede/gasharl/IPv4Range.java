/*
   GASH 2

   IPv4Range.java

   Created: 4 April 2001

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.gasharl;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import arlut.csd.ganymede.common.IPAddress;
import arlut.csd.Util.StringUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPv4Range

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to store and manipulate a definition for a
 * sequence of IPv4 addresses, with support for conversion to/from a
 * textual format, for generating an enumeration of addresses in the
 * IPv4Range, and more.</p>
 *
 * <p>This class supports continuous and discontinuous IP range
 * specification using a string that comprises multiple lines, each of
 * which has four dot-separated octets which may either be simple
 * numeric values, or a range of values, with the start and stop point
 * for the range separated by a dash and surrounded by brackets.  The
 * iteration across the range implemented by the getElements()
 * Enumeration will vary across the least significant byte of ranged
 * stanzas first.</p>
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

  /**
   * <p>An array of arrays of byte arrays.</p>
   *
   * <p>The individual signed Java bytes are encoded using the
   * IPAddress u2s() method.</p>
   */

  private byte[][][] range = null;

  /* -- */

  public IPv4Range()
  {
  }

  public IPv4Range(byte[][][] initRange)
  {
    range = new byte[initRange.length][4][2];

    for (int i = 0; i < initRange.length; i++)
      {
        for (int j = 0; j < 4; j++)
          {
            for (int k = 0; k < 2; k++)
              {
                range[i][j][k] = initRange[i][j][k];
              }
          }
      }
  }

  public IPv4Range(byte[][] initRange)
  {
    range = new byte[1][4][2];

    for (int i = 0; i < 4; i++)
      {
        for (int j = 0; j < 2; j++)
          {
            range[0][i][j] = initRange[i][j];
          }
      }
  }

  public IPv4Range(IPAddress networkNumber)
  {
    if (!networkNumber.isIPv4())
      {
        throw new IllegalArgumentException("Bad number of bytes");
      }

    byte[] netBytes = networkNumber.getBytes();

    range = new byte[1][4][2];

    boolean hostPortion = true;

    for (int i = 3; i >= 0; i--)
      {
        range[0][i][0] = netBytes[i];
        range[0][i][1] = netBytes[i];

        if (hostPortion)
          {
            if (range[0][i][0] == IPAddress.u2s(0))
              {
                range[0][i][0] = IPAddress.u2s(1);
                range[0][i][1] = IPAddress.u2s(254);
              }
            else
              {
                hostPortion = false;
              }
          }
      }
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

  /**
   * <p>This method is used to load an IP address allocation specifier
   * into this IPv4Range.</p>
   *
   * <p>See the Javadocs for this class for the details on the
   * allowed formats for the initialization string.</p>
   */

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

                _range[i][j][0] = IPAddress.u2s(val);
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

                    _range[i][j][k] = IPAddress.u2s(val);
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

  /**
   * <p>This method returns the total number of IPv4 addresses that
   * this IPv4Range contains.</p>
   */

  public synchronized int getSize()
  {
    if (range == null)
      {
        return 0;
      }

    int count = 0;

    for (int i = 0; i < range.length; i++)
      {
        int stanzaCount = 1;

        for (int j = 0; j < 4; j++)
          {
            if (range[i][j][0] < range[i][j][1])
              {
                stanzaCount *= ((range[i][j][1] - range[i][j][0]) + 1);
              }
            else if (range[i][j][0] > range[i][j][1])
              {
                stanzaCount *= ((range[i][j][0] - range[i][j][1]) + 1);
              }
          }

        count += stanzaCount;
      }

    return count;
  }

  public synchronized String toString()
  {
    StringBuilder result = new StringBuilder();

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
                result.append(IPAddress.s2u(range[i][j][0]));
              }
            else
              {
                result.append("[");
                result.append(IPAddress.s2u(range[i][j][0]));
                result.append("-");
                result.append(IPAddress.s2u(range[i][j][1]));
                result.append("]");
              }
          }
      }

    return result.toString();
  }

  /**
   * <p>This method returns an enumeration that will iterate over all
   * IPv4 addresses specified by this IPv4Range, in order.</p>
   *
   * <p>The returned Enumeration uses a snapshot of the state of this
   * IPv4Range, so changes made to this IPv4Range's state after
   * obtaining the Enumeration will have no effect on the addresses
   * generated by the Enumeration.</p>
   *
   * <p>The nextElement() method in the returned Enumeration return a
   * IPAddress.</p>
   */

  public Enumeration getElements()
  {
    return new IPv4RangeEnumerator(this);
  }

  /**
   * <p>This method returns an enumeration that will use defined value
   * for the starting and stopping points in ranging across the fourth
   * octet of the IPv4 addresses specified by this IPv4Range.  Any
   * addresses that would ordinarily be present in this IPv4Range will
   * be skipped if they do not fall between the start and stop values,
   * and the order in which the fourth byte will be ranged will depend
   * on whether start or stop is greater.  The enumeration returned
   * will not generate any values that would not be generated by the
   * no parameter getElements() call with no external start and stop
   * values specified.</p>
   *
   * <p>The returned Enumeration uses a snapshot of the state of this
   * IPv4Range, so changes made to this IPv4Range's state after
   * obtaining the Enumeration will have no effect on the addresses
   * generated by the Enumeration.</p>
   *
   * <p>The nextElement() method in the returned Enumeration return an
   * IPAddress.</p>
   *
   * <p>If start and stop are both -1, a non-constrained Enumeration
   * will be return.  If one but not both of start and stop are
   * -1, an error will result.</p>
   */

  public Enumeration getElements(int start, int stop)
  {
    if (start == -1 && stop == -1)
      {
        return new IPv4RangeEnumerator(this);
      }

    return new IPv4RangeEnumerator(this, start, stop);
  }

  /**
   * <p>This method returns an enumeration that will iterate over all
   * discretely defined subranges of this IPv4Range, in order.</p>
   *
   * <p>In other words, if the String used to initialize this
   * IPv4Range consisted of multiple lines, the enumeration returned
   * by this method will return a sequences of IPv4Range objects,
   * one for each line of the original String.</p>
   *
   * <p>The returned Enumeration uses a snapshot of the state of this
   * IPv4Range, so changes made to this IPv4Range's state after
   * obtaining the Enumeration will have no effect on the IPv4Range's
   * generated by the returned Enumeration.</p>
   *
   * <p>The nextElement() method in the returned Enumeration returns IPv4Range
   * objects which each contain the equivalent of a single line's worth of
   * range information, relative to the string specification form used to
   * construct IPv4Range's.</p>
   */

  public Enumeration getSubRanges()
  {
    return new IPv4SubRangeEnumerator(this);
  }

  /**
   * <p>This method returns an enumeration that will iterate over all
   * Class C sized (three octet prefix) network numbers available
   * from this IPv4Range, in order.</p>
   *
   * <p>The returned Enumeration uses a snapshot of the state of this
   * IPv4Range, so changes made to this IPv4Range's state after
   * obtaining the Enumeration will have no effect on the addresses
   * generated by the Enumeration.</p>
   *
   * <p>The nextElement() method in the returned Enumeration returns
   * an IPAddress.</p>
   *
   * <p>The fourth and final octet of each Byte array returned by nextElement()
   * will contain a value of -128, representing 0 for the last octet.</p>
   */

  public Enumeration getClassCEnum()
  {
    return new IPv4ClassCEnumerator(this);
  }

  /**
   * <p>This method returns true if this IPv4Range and the
   * IPv4Range _range contain any overlaps in specified address
   * space.</p>
   */

  public synchronized boolean overlapsRange(IPv4Range _range)
  {
    synchronized (_range)
      {
        for (int i = 0; i < range.length; i++)
          {
            for (int j = 0; j < _range.range.length; j++)
              {
                if (byteOverlap(range[i][0], _range.range[j][0]) &&
                    byteOverlap(range[i][1], _range.range[j][1]) &&
                    byteOverlap(range[i][2], _range.range[j][2]) &&
                    byteOverlap(range[i][3], _range.range[j][3]))
                  {
                    return true;
                  }
              }
          }
      }

    return false;
  }

  /**
   * <p>This method returns true if address is a member of
   * the collection of IPv4 addresses represented by this
   * IPv4Range object, false otherwise.</p>
   */

  public boolean matches(String address)
  {
    return matches(IPAddress.genIPV4bytes(address));
  }

  /**
   * <p>This method returns true if address is a member of
   * the collection of IPv4 addresses represented by this
   * IPv4Range object, false otherwise.</p>
   *
   * <p>If start and stop are not equal to -1, matches will
   * only return true if the last octet of the address in
   * question is between start and stop.  This is an additional
   * restriction on top of that specified in this IPv4Range
   * object.</p>
   */

  public boolean matches(String address, int start, int stop)
  {
    return matches(IPAddress.genIPV4bytes(address), start, stop);
  }

  /**
   * <p>This method returns true if address is a member of
   * the collection of IPv4 addresses represented by this
   * IPv4Range object, false otherwise.</p>
   */

  public boolean matches(IPAddress address)
  {
    return matches(address.getBytes(), -1, -1);
  }

  /**
   * <p>This method returns true if address is a member of
   * the collection of IPv4 addresses represented by this
   * IPv4Range object, false otherwise.</p>
   */

  public boolean matches(byte[] address)
  {
    return matches(address, -1, -1);
  }

  /**
   * <p>This method returns true if address is a member of
   * the collection of IPv4 addresses represented by this
   * IPv4Range object, false otherwise.</p>
   *
   * <p>If start and stop are not equal to -1, matches will
   * only return true if the last octet of the address in
   * question is between start and stop.  This is an additional
   * restriction on top of that specified in this IPv4Range
   * object.</p>
   */

  public boolean matches(IPAddress address, int start, int stop)
  {
    return matches(address.getBytes(), start, stop);
  }

  /**
   * <p>This method returns true if address is a member of
   * the collection of IPv4 addresses represented by this
   * IPv4Range object, false otherwise.</p>
   *
   * <p>If start and stop are not equal to -1, matches will
   * only return true if the last octet of the address in
   * question is between start and stop.  This is an additional
   * restriction on top of that specified in this IPv4Range
   * object.</p>
   */

  public synchronized boolean matches(byte[] address, int start, int stop)
  {
    if (address.length != 4)
      {
        throw new IllegalArgumentException("bad number of bytes in address");
      }

    boolean found = false;

    for (int i = 0; !found && i < range.length; i++)
      {
        if (stanzaMatch(range[i], address, start, stop))
          {
            found = true;
          }
      }

    return found;
  }

  /**
   * <p>This method compares a given 4 signed byte address against
   * a specific stanza and returns true if the address could be generated
   * by the stanza array passed in as _range.</p>
   *
   * <p>If start and stop are not each equal to -1, stanzaMatch will
   * only return true if the last octet of the address in
   * question is between start and stop.  This is an additional
   * restriction on top of that specified in the _range parameter.</p>
   */

  private boolean stanzaMatch(byte _range[][], byte address[], int start, int stop)
  {
    for (int i = 0; i < 4; i++)
      {
        if ((address[i] < _range[i][0] && address[i] < _range[i][1]) ||
            (address[i] > _range[i][0] && address[i] > _range[i][1]))
          {
            return false;
          }
      }

    if (start != -1 || stop != -1)
      {
        if (start > stop)
          {
            if (address[4] < stop || address[4] > start)
              {
                return false;
              }
          }
        else
          {
            if (address[4] < start || address[4] > stop)
              {
                return false;
              }
          }
      }

    return true;
  }

  /**
   * <p>This method compares a set of stanzas of a multi-line
   * IPv4Range speficiation, and returns true if any of the
   * ranges overlap at all.  We want to avoid stanza overlap
   * so that we don't generate a single address at more than one
   * point in the getElements() enumeration.</p>
   */

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

  /**
   * <p>This method returns true if there is any overlap between
   * the range of values specified in pair1 and the range of
   * values specified in pair2.</p>
   */

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
}

/*------------------------------------------------------------------------------
                                                                           class
                                                             IPv4RangeEnumerator

------------------------------------------------------------------------------*/

/**
 * <p>This class handles enumerating across a range of IP addresses
 * specified in an {@arlut.csd.ganymede.gasharl.IPv4Range IPv4Range}
 * object.  An IPv4RangeEnumerator is created as a snapshot of the
 * state of an IPv4Range object, and may be iterated over even if the
 * source IPv4Range object is modified after it has been created.</p>
 */

final class IPv4RangeEnumerator implements Enumeration {

  private byte[][][] range = null;

  private int line = 0;

  private int index[] = new int[4];

  /* -- */

  IPv4RangeEnumerator(IPv4Range v4Range, int start, int stop)
  {
    if (start < 0 || start > 255)
      {
        throw new IllegalArgumentException("start out of range:" + start);
      }

    if (stop < 0 || stop > 255)
      {
        throw new IllegalArgumentException("stop out of range:" + stop);
      }

    byte[][][] tmpRange = v4Range.getByteArray();
    int rowCount = 0;

    for (int i = 0; i < tmpRange.length; i++)
      {
        int firstIndex = IPAddress.s2u(tmpRange[i][3][0]);
        int lastIndex = IPAddress.s2u(tmpRange[i][3][1]);

        if (firstIndex < lastIndex)
          {
            if ((start < firstIndex && stop < firstIndex) ||
                (start > lastIndex && stop > lastIndex))
              {
                tmpRange[i] = null;

                continue;
              }

            if (start > firstIndex && start < lastIndex)
              {
                tmpRange[i][3][0] = IPAddress.u2s(start);
              }

            if (stop < lastIndex && stop > firstIndex)
              {
                tmpRange[i][3][1] = IPAddress.u2s(stop);
              }

            rowCount++;
          }
        else if (firstIndex > lastIndex)
          {
            if ((start > firstIndex && stop > firstIndex) ||
                (start < lastIndex && stop < lastIndex))
              {
                tmpRange[i] = null;

                continue;
              }

            if (start < firstIndex && start > lastIndex)
              {
                tmpRange[i][3][0] = IPAddress.u2s(start);
              }

            if (stop > lastIndex && stop < firstIndex)
              {
                tmpRange[i][3][1] = IPAddress.u2s(stop);
              }

            rowCount++;
          }
        else if (firstIndex == lastIndex)
          {
            if ((start < firstIndex && stop < firstIndex) ||
                (start > firstIndex && stop > firstIndex))
              {
                tmpRange[i] = null;

                continue;
              }

            rowCount++;
          }
      }

    range = new byte[rowCount][4][2];

    for (int i = 0, sourceIndex = 0; i < rowCount; i++, sourceIndex++)
      {
        while (tmpRange[sourceIndex] == null)
          {
            sourceIndex++;
          }

        for (int j = 0; j < 4; j++)
          {
            range[i][j][0] = tmpRange[sourceIndex][j][0];
            range[i][j][1] = tmpRange[sourceIndex][j][1];
          }
      }
  }

  IPv4RangeEnumerator(IPv4Range v4Range)
  {
    range = v4Range.getByteArray();

    for (int i = 0; i < 4; i++)
      {
        index[i] = 0;
      }
  }

  public boolean hasMoreElements()
  {
    return line < range.length;
  }

  public synchronized Object nextElement()
  {
    if (line >= range.length)
      {
        throw new NoSuchElementException("IPv4RangeEnumerator");
      }

    // create the next address to return

    byte[] bytes = new byte[4];

    for (int i = 0; i < 4; i++)
      {
        bytes[i] = (byte) (range[line][i][0] + index[i]);
      }

    IPAddress address = new IPAddress(bytes);

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
}

/*------------------------------------------------------------------------------
                                                                           class
                                                          IPv4SubRangeEnumerator

------------------------------------------------------------------------------*/

/**
 * <p>This class handles enumerating across a set of IPv4Range subsets
 * specified in an {@arlut.csd.ganymede.gasharl.IPv4Range IPv4Range}
 * object.  An IPv4SubRangeEnumerator is created as a snapshot of the
 * state of an IPv4Range object, and may be iterated over even if the
 * source IPv4Range object is modified after it has been created.</p>
 *
 * <p>The nextElement() method in IPv4SubRangeEnumerator returns
 * IPv4Range objects which each contain the equivalent of a single
 * line's worth of range information, relative to the string
 * specification form used to construct IPv4Range's.</p>
 */

final class IPv4SubRangeEnumerator implements Enumeration {

  private byte[][][] range = null;

  private int line = 0;

  /* -- */

  IPv4SubRangeEnumerator(IPv4Range v4Range)
  {
    range = v4Range.getByteArray();
  }

  public boolean hasMoreElements()
  {
    return line < range.length;
  }

  public synchronized Object nextElement()
  {
    if (line >= range.length)
      {
        throw new NoSuchElementException("IPv4SubRangeEnumerator");
      }

    return new IPv4Range(range[line++]);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                            IPv4ClassCEnumerator

------------------------------------------------------------------------------*/

/**
 * <p>This class handles enumerating across a set of Class C sized (three octet prefix)
 * subsets specified in an {@arlut.csd.ganymede.gasharl.IPv4Range IPv4Range} object.  An
 * IPv4ClassCEnumerator is created as a snapshot of the state of an IPv4Range object,
 * and may be iterated over even if the source IPv4Range object is modified
 * after it has been created.</p>
 *
 * <p>The nextElement() method in IPv4ClassCEnumerator returns Byte[]
 * arrays which contain four elements, one for each of the four octets
 * of an IPv4 address.  The elements hold values in the range -128 to 127,
 * which are equivalent to the 0 to 255 range for IPv4 octets.  These
 * numbers can be converted to the appropriate number for IPv4 by adding
 * 128 to each element.</p>
 *
 * <p>The fourth and final octet of each Byte array returned by nextElement()
 * will contain a value of -128, representing 0 for the last octet.</p>
 */

final class IPv4ClassCEnumerator implements Enumeration {

  private byte[][][] range = null;

  private int line = 0;

  private int index[] = new int[3];

  /* -- */

  IPv4ClassCEnumerator(IPv4Range v4Range)
  {
    range = v4Range.getByteArray();

    for (int i = 0; i < 3; i++)
      {
        index[i] = 0;
      }
  }

  public boolean hasMoreElements()
  {
    return line < range.length;
  }

  public synchronized Object nextElement()
  {
    if (line >= range.length)
      {
        throw new NoSuchElementException("IPv4ClassCEnumerator");
      }

    // create the next Class C-sized subnet to return

    Byte[] bytes = new Byte[4];

    for (int i = 0; i < 3; i++)
      {
        bytes[i] = new Byte((byte) (range[line][i][0] + index[i]));
      }

    bytes[3] = new Byte((byte) -128);

    IPAddress address = new IPAddress(bytes);

    // now update our range index variables to find the set of
    // offsets for the next address in our sequence

    boolean incremented = false;

    for (int i = 2; !incremented && i >= 0; i--)
      {
        if (range[line][i][1] > range[line][i][0])
          {
            index[i]++;

            if (index[i] > (range[line][i][1] - range[line][i][0]))
              {
                for (int j = i; j < 3; j++)
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
                for (int j = i; j < 3; j++)
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
}
