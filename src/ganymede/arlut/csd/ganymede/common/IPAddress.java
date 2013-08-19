/*

   IPAddress.java

   This is a wrapper class for the Byte array IP value representation
   that allows IP addresses to be stored and processed in the Ganymede
   system.

   Created: 20 May 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPAddress

------------------------------------------------------------------------------*/

/**
 * <p>This is an immutable, serializable IPv4 or IPv6 address.</p>
 */

public final class IPAddress implements Cloneable, java.io.Serializable {

  static final long serialVersionUID = -2432213571055741805L;

  private static String IPv4allowedChars = "1234567890.";
  private static String IPv6allowedChars = "1234567890.abcdefABCDEF:";

  // ---

  /**
   * <p>Note that Java doesn't have unsigned bytes, so we have to deal
   * with the bytes in address in terms of signed arithmetic, with
   * values between -128 and 127.</p>
   *
   * <p>While Java uses 2's complement for holding signed numeric
   * values, we are not, for historical reasons, using the bits in
   * these bytes as if they were unsigned (i.e., storing a value that
   * would be held in an unsigned char in C using the 2's complement
   * interpretation of that bit pattern), as you might expect in
   * C.</p>
   *
   * <p>Instead, they are equal to a value of 0-255, minus 128.  So 0
   * is -128, 100 is -28, etc., up to 255 is 127.</p>
   *
   * <p>This makes it impossible to just mask these values against
   * 0xff to get the unsigned int value.  Fixing this would break
   * Ganymede's on-disk database and journal format.</p>
   *
   * <p>On the other hand, the u2s() encoded bytes can be safely
   * compared for greater than or less than, and subtracting them from
   * each other will result in the proper distance between them.</p>
   *
   * <p>For compatibility, be sure to use s2u() to do the conversion
   * to the unsigned value, or you can just add 128 to do it
   * yourself.</p>
   */

  private final byte[] address;

  /**
   * <p>We'll cache the string representation of our IPAddress for
   * performance.</p>
   *
   * <p>(Generating the string for IPv4 addresses is fast, but
   * generating the string for IPv6 addresses is less so.)</p>
   */

  private transient String text;

  /* -- */

  /**
   * <p>Primitive byte array constructor</p>
   *
   * <p>Because bytes are signed in Java, the bytes submitted for the
   * octets of this address will range from -128 to 127.  You can use
   * IPAddress.u2s() to convert ints in the range 0 to 255 to the
   * range used by bytes in this class.</p>
   *
   * <p>The address parameter must be of length 4 for an IPv4 address,
   * and 16 for an IPv6 address.</p>
   */

  public IPAddress(byte[] address)
  {
    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException("Wrong number of bytes for an IP address: " + address.length);
      }

    this.address = Arrays.copyOf(address, address.length);
  }

  /**
   * <p>java.lang.Byte array constructor</p>
   *
   * <p>Because bytes are signed in Java, the bytes submitted for the
   * octets of this address will range from -128 to 127.  You can use
   * IPAddress.u2s() to convert ints in the range 0 to 255 to the
   * range used by bytes in this class, or just subtract 128.</p>
   *
   * <p>The address parameter must be of length 4 for an IPv4 address,
   * and 16 for an IPv6 address.</p>
   */

  public IPAddress(Byte[] address)
  {
    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException("Wrong number of bytes for an IP address: " + address.length);
      }

    this.address = new byte[address.length];

    for (int i = 0; i < address.length; i++)
      {
        this.address[i] = address[i].byteValue();
      }
  }

  /**
   * <p>Primitive short array constructor</p>
   *
   * <p>shorts in the address array must all be between 0 and 255, or
   * an IllegalArgumentException will be thrown.</p>
   *
   * <p>The address parameter must be of length 4 for an IPv4 address,
   * and 16 for an IPv6 address.</p>
   */

  public IPAddress(short[] address)
  {
    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException("Wrong number of octets for an IP address: " + address.length);
      }

    this.address = u2s(address);
  }

  /**
   * <p>Short object array constructor</p>
   *
   * <p>Shorts in the address array must all be between 0 and 255, or
   * an IllegalArgumentException will be thrown.</p>
   *
   * <p>The address parameter must be of length 4 for an IPv4 address,
   * and 16 for an IPv6 address.</p>
   */

  public IPAddress(Short[] address)
  {
    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException("Wrong number of octets for an IP address: " + address.length);
      }

    this.address = new byte[address.length];

    for (int i = 0; i < address.length; i++)
      {
        this.address[i] = u2s(address[i].intValue());
      }
  }

  /**
   * <p>Primitive int array constructor</p>
   *
   * <p>ints in the address array must all be between 0 and 255, or an
   * IllegalArgumentException will be thrown.</p>
   *
   * <p>The address parameter must be of length 4 for an IPv4 address,
   * and 16 for an IPv6 address.</p>
   */

  public IPAddress(int[] address)
  {
    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException("Wrong number of octets for an IP address: " + address.length);
      }

    this.address = u2s(address);
  }

  /**
   * <p>Integer object array constructor</p>
   *
   * <p>Integers in the address array must all be between 0 and 255,
   * or an IllegalArgumentException will be thrown.</p>
   *
   * <p>The address parameter must be of length 4 for an IPv4 address,
   * and 16 for an IPv6 address.</p>
   */

  public IPAddress(Integer[] address)
  {
    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException("Wrong number of octets for an IP address: " + address.length);
      }

    this.address = new byte[address.length];

    for (int i = 0; i < address.length; i++)
      {
        this.address[i] = u2s(address[i].intValue());
      }
  }

  /**
   * <p>String constructor, supports IPv4 dotted decimal and RFC 4291
   * style IPv6 encodings.</p>
   */

  public IPAddress(String addressStr)
  {
    if (addressStr.indexOf(':') == -1)
      {
        this.address = IPAddress.genIPV4bytes(addressStr);
      }
    else
      {
        this.address = IPAddress.genIPV6bytes(addressStr);
      }
  }

  /**
   * This method generates the hash key for this object for use in
   * a Hashtable.
   */

  @Override public int hashCode()
  {
    return Arrays.hashCode(this.address);
  }

  /**
   * Equality test.  This IPAddress can be compared to either
   * another IPAddress object or to an array of Bytes.
   */

  @Override public boolean equals(Object value)
  {
    if (!(value instanceof IPAddress))
      {
        return false;
      }

    return Arrays.equals(this.address, ((IPAddress) value).address);
  }

  @Override public Object clone()
  {
    return new IPAddress(this.address);
  }

  /**
   * <p>Returns a copy of the array of primitive bytes in this
   * address.</p>
   *
   * <p>Because bytes are signed in Java, the range of of the bytes
   * are from -128 to 127.  You can use IPAddress.s2u() to convert the
   * individual bytes to shorts in the range 0-255, or you can use
   * getOctets() to get the bytes in this IPAddress in int form.</p>
   *
   * <p>Or you can just add 128 to each signed value to get a short or
   * int ranged value from 0-255.</p>
   */

  public byte[] getBytes()
  {
    return Arrays.copyOf(this.address, this.address.length);
  }

  /**
   * <p>Gets an individual byte from this IPAddress.</p>
   *
   * <p>Because bytes are signed in Java, the range of of the returned
   * byte is from -128 to 127.  You can use IPAddress.s2u() to convert
   * the byte returned to a 0-255 int value, or you can just add 128
   * to the returned value.</p>
   */

  public byte getByte(int index)
  {
    return this.address[index];
  }

  /**
   * <p>Returns an array of ints containing the octets in this address.</p>
   *
   * <p>Each int will be in the range 0-255.</p>
   */

  public int[] getOctets()
  {
    int[] result = new int[this.address.length];

    for (int i = 0; i < this.address.length; i++)
      {
        result[i] = s2u(this.address[i]);
      }

    return result;
  }

  public boolean isIPv4()
  {
    return this.address.length == 4;
  }

  public boolean isIPv6()
  {
    return this.address.length == 16;
  }

  public int length()
  {
    return this.address.length;
  }

  @Override public String toString()
  {
    if (text != null)
      {
        return text;
      }

    synchronized (this.address)
      {
        if (text == null)
          {
            text = IPAddress.genIPString(this.address);
          }
      }

    return text;
  }

  /**
   * <p>Writes this IPAddress to out in a form compatible with the
   * ganymede.db and/or journal file serializations.</p>
   */

  public void emit(DataOutput out) throws IOException
  {
    out.writeByte(this.address.length);
    out.write(this.address);
  }

  /*------------------------------------------------------------


                            Static Methods


    ------------------------------------------------------------*/

  /**
   * <p>Reads an IPAddress from in using the Ganymede database and/or
   * journal file serialization.</p>
   */

  public static IPAddress readIPAddr(DataInput in) throws IOException
  {
    byte[] bytes = new byte[in.readByte()];
    in.readFully(bytes);

    return new IPAddress(bytes);
  }

  /**
   * <p>This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.</p>
   *
   * <p>Read u2s as 'unsigned to signed'.</p>
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
   * <p>This method maps an array of primitive int values between 0
   * and 255 to an array of primitive byte values between -128 and 127
   * inclusive, as is used in the internal byte array in
   * IPAddress.</p>
   *
   * <p>Read u2s as 'unsigned to signed'.</p>
   */

  public final static byte[] u2s(int[] octets)
  {
    byte[] result = new byte[octets.length];

    for (int i = 0; i < octets.length; i++)
      {
        if ((octets[i] < 0) || (octets[i] > 255))
          {
            throw new IllegalArgumentException("Out of range: " + octets[i]);
          }

        result[i] = (byte) (octets[i] - 128);
      }

    return result;
  }

  /**
   * <p>This method maps an array of primitive short values between 0
   * and 255 to an array of primitive byte values between -128 and 127
   * inclusive, as is used in the internal byte array in
   * IPAddress.</p>
   *
   * <p>Read u2s as 'unsigned to signed'.</p>
   */

  public final static byte[] u2s(short[] octets)
  {
    byte[] result = new byte[octets.length];

    for (int i = 0; i < octets.length; i++)
      {
        if ((octets[i] < 0) || (octets[i] > 255))
          {
            throw new IllegalArgumentException("Out of range: " + octets[i]);
          }

        result[i] = (byte) (octets[i] - 128);
      }

    return result;
  }

  /**
   * <p>This method maps a u2s-encoded signed byte value (-128 to 127)
   * to a positive primitive short value between 0 and 255
   * inclusive.</p>
   *
   * <p>Read s2u as 'signed to unsigned'.</p>
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }

  /**
   * <p>This method maps a u2s-encoded signed array of primitive byte
   * values (-128 to 127) to an array of positive primitive short
   * value between 0 and 255 inclusive.</p>
   *
   * <p>Read s2u as 'signed to unsigned'.</p>
   */

  public final static short[] s2u(byte[] b)
  {
    short[] result = new short[b.length];

    for (int i = 0; i < b.length; i++)
      {
        result[i] = (short) (b[i] + 128);
      }

    return result;
  }

  /**
   * <p>Returns an IPv4 or IPv6 string, based on the length of the
   * octets parameter.</p>
   */

  public static String genIPString(int[] octets)
  {
    return genIPString(u2s(octets));
  }

  /**
   * <p>Returns an IPv4 or IPv6 string, based on the length of the
   * octets parameter.</p>
   */

  public static String genIPString(Byte[] octets)
  {
    return genIPString(unwrap(octets));
  }

  /**
   * <p>Returns an IPv4 or IPv6 string, based on the length of the
   * octets parameter.</p>
   */

  public static String genIPString(byte[] octets)
  {
    if (octets == null)
      {
        return null;
      }

    if (octets.length == 4)
      {
        return IPAddress.genIPV4string(octets);
      }
    else if (octets.length == 16)
      {
        return IPAddress.genIPV6string(octets);
      }
    else
      {
        throw new IllegalArgumentException("Wrong number of octets for an IP address: " + octets.length);
      }
  }

  /**
   * <p>This method takes an IPv4 string in standard format and
   * generates an array of 4 bytes that the Ganymede server can
   * accept.</p>
   */

  public static byte[] genIPV4bytes(String input)
  {
    byte[] result = new byte[4];
    List<String> octets = new ArrayList<String>();
    char[] cAry;
    int length = 0;
    int dotCount = 0;
    StringBuilder temp = new StringBuilder();

    /* -- */

    if (input == null)
      {
        throw new IllegalArgumentException("null input");
      }

    /*
      The string will be of the form 255.255.255.255,
      with each dot separated element being a 8 bit octet
      in decimal form.  Trailing bytes may be excluded, in
      which case the bytes will be left as 0.
      */

    // initialize the result array

    for (int i = 0; i < 4; i++)
      {
        result[i] = u2s(0);
      }

    input = input.trim();

    if (input.equals(""))
      {
        return result;
      }

    cAry = input.toCharArray();

    for (int i = 0; i < cAry.length; i++)
      {
        if (!isAllowedV4(cAry[i]))
          {
            throw new IllegalArgumentException("bad char in input: " + input);
          }

        if (cAry[i] == '.')
          {
            dotCount++;
          }
      }

    if (dotCount > 3)
      {
        throw new IllegalArgumentException("too many dots for an IPv4 address: " + input);
      }

    while (length < cAry.length)
      {
        temp.setLength(0);

        while ((length < cAry.length) && (cAry[length] != '.'))
          {
            temp.append(cAry[length++]);
          }

        length++;               // skip the .

        octets.add(temp.toString());
      }

    for (int i = 0; i < octets.size(); i++)
      {
        result[i] = u2s(Integer.parseInt(octets.get(i)));
      }

    return result;
  }

  /**
   * <p>This method generates a standard string representation of an
   * IPv4 address from an array of 4 octets.</p>
   */

  public static String genIPV4string(Byte[] octets)
  {
    return genIPV4string(unwrap(octets));
  }

  /**
   * <p>This method generates a standard string representation of an
   * IPv4 address from an array of 4 octets.</p>
   */

  public static String genIPV4string(byte[] octets)
  {
    if (octets.length != 4)
      {
        throw new IllegalArgumentException("bad number of octets.");
      }

    StringBuilder result = new StringBuilder();

    result.append(Integer.toString(octets[0] + 128));
    result.append(".");
    result.append(Integer.toString(octets[1] + 128));
    result.append(".");
    result.append(Integer.toString(octets[2] + 128));
    result.append(".");
    result.append(Integer.toString(octets[3] + 128));

    return result.toString();
  }

  /**
   * <p>This method takes an IPv6 string in any of the standard RFC
   * 4291 formats or a standard IPv4 string and generates an array of
   * 16 bytes that the Ganymede server can accept as an IPv6
   * address.</p>
   */

  public static byte[] genIPV6bytes(String input)
  {
    byte[] result = new byte[16];
    byte[] ipv4bytes = null;
    List<String> segments = new ArrayList<String>();
    char[] cAry;

    int
      length = 0,               // how far into the input have we processed?
      dotCount = 0,             // how many dots for the IPv4 portion?
      colonCount = 0,           // how many colons for the IPv6 portion?
      doublecolon = 0,          // how many double colons?
      tailBytes = 0,            // how many trailing bytes do we have?
      v4v6boundary = 0;         // what is the index of the last char of the v6 portion?

    StringBuilder temp = new StringBuilder();

    /* -- */

    if (input == null)
      {
        throw new IllegalArgumentException("null input");
      }

    /*
      The string may be in one of 3 principle forms.

      The first is that of a standard IPv4 address, in which
      case genIPV6bytes will generate an IPv6 compatible
      address with the assistance of the genIPV4bytes method.

      The second is that of a standard IPv6 address, with 8
      16 bit segments in hex form, :'s between the segments.
      A pair of :'s in immediate succession indicates a range
      of 0 segments have been ommitted.  Since only one
      pair of adjacent :'s may appear in a valid IPv6 address,
      it is possible by looking at the other segments specified
      to determine the extent of the collapsed segments and
      properly insert 0 bytes to fill the collapsed region.

      The third is that of a mixed IPv6/IPv4 address.  In
      this form, the last 4 bytes of the address may be
      specified in IPv4 dotted decimal form.
      */

    // initialize the result array

    for (int i = 0; i < 16; i++)
      {
        result[i] = u2s(0);
      }

    input = input.trim();

    v4v6boundary = input.length();

    if (input.equals("") || input.equals("::"))
      {
        return result;
      }

    cAry = input.toCharArray();

    for (int i = 0; i < cAry.length; i++)
      {
        if (!isAllowedV6(cAry[i]))
          {
            throw new IllegalArgumentException("bad char in input: " + input);
          }

        if (cAry[i] == '.')
          {
            dotCount++;
          }

        if (cAry[i] == ':')
          {
            colonCount++;

            if (i > 0 && (cAry[i-1] == ':'))
              {
                doublecolon++;
              }
          }
      }

    if (dotCount > 3)
      {
        throw new IllegalArgumentException("too many dots for a mixed IPv4/IPv6 address: " + input);
      }

    if (colonCount > 7)
      {
        throw new IllegalArgumentException("too many colons for an IPv6 address: " + input);
      }

    if (doublecolon > 1)
      {
        throw new IllegalArgumentException("error: more than one double-colon.  Invalid IPv6 address: " + input);
      }

    if (dotCount > 0 && colonCount > 6)
      {
        throw new IllegalArgumentException("invalid mixed IPv4/IPv6 address: " + input);
      }

    if ((colonCount == 0) && (dotCount != 0))
      {
        // we've got an IPv4 address where we would like an IPv6 address.  Convert it.

        ipv4bytes = genIPV4bytes(input);

        result[10] = u2s(255);
        result[11] = u2s(255);
        result[12] = ipv4bytes[0];
        result[13] = ipv4bytes[1];
        result[14] = ipv4bytes[2];
        result[15] = ipv4bytes[3];

        return result;
      }

    if (dotCount > 0)
      {
        // we've got a mixed address.. get the v4 bytes from the end.

        v4v6boundary = input.lastIndexOf(':');

        try
          {
            ipv4bytes = genIPV4bytes(input.substring(v4v6boundary + 1));
          }
        catch (Exception ex)
          {
            throw new IllegalArgumentException("couldn't do mixed IPv4 parsing: " + ex);
          }

        result[12] = ipv4bytes[0];
        result[13] = ipv4bytes[1];
        result[14] = ipv4bytes[2];
        result[15] = ipv4bytes[3];

        tailBytes = 4;
      }

    // note that the v4v6boundary will be the length of the input
    // string if we are processing a pure v6 address

    while (length < v4v6boundary)
      {
        temp.setLength(0);

        while ((length < v4v6boundary) && (cAry[length] != ':'))
          {
            temp.append(cAry[length++]);
          }

        length++;               // skip the :

        segments.add(temp.toString());
      }

    // okay, we now have a vector of segment strings, with (possibly)
    // an empty string where we had a pair of colons in succession.  Find
    // out if we have a colon pair, and determine the region that it
    // is compressing.

    String tmp;
    boolean beforeRegion = true;
    int compressBoundary = 0;

    for (int i = 0; i < segments.size(); i++)
      {
        tmp = segments.get(i);

        if (tmp.equals(""))
          {
            beforeRegion = false;

            compressBoundary = i;
          }
        else if (!beforeRegion)
          {
            tailBytes += 2;
          }
      }

    // compressBoundary won't be 0 if we had a leading ::,
    // because we would have 2 empties in a row.

    if (compressBoundary == 0)
      {
        compressBoundary = segments.size();
      }
    else if (compressBoundary == 1)
      {
        // if the :: is leading the string, we want to get rid
        // of the extra empty string

        tmp = segments.get(0);

        if (tmp.equals(""))
          {
            segments.remove(0);
            compressBoundary--;
          }
      }

    int tailOffset = 16 - tailBytes;

    for (int i = 0; i < compressBoundary; i++)
      {
        tmp = segments.get(i);

        if (tmp.length() < 4)
          {
            int l = tmp.length();

            for (int j = 4; j > l; j--)
              {
                tmp = "0" + tmp;
              }
          }

        if (tmp.equals(""))
          {
            throw new Error("logic error");
          }

        result[i * 2] = u2s(Integer.parseInt(tmp.substring(0, 2), 16));
        result[(i * 2) + 1] = u2s(Integer.parseInt(tmp.substring(2, 4), 16));
      }

    int x;

    for (int i = compressBoundary+1; i < segments.size(); i++)
      {
        x = i - compressBoundary - 1;
        tmp = segments.get(i);

        if (tmp.length() < 4)
          {
            int l = tmp.length();

            for (int j = 4; j > l; j--)
              {
                tmp = "0" + tmp;
              }
          }

        if (tmp.equals(""))
          {
            throw new Error("logic error");
          }

        result[tailOffset + (x * 2)] = u2s(Integer.parseInt(tmp.substring(0, 2), 16));
        result[tailOffset + (x * 2) + 1] = u2s(Integer.parseInt(tmp.substring(2, 4), 16));
      }

    return result;
  }

  /**
   * <p>This method takes an array of 4 or 16 Byte objects and
   * generates an optimal RFC 5952 string encoding suitable for
   * display.</p>
   */

  public static String genIPV6string(Byte[] octets)
  {
    return genIPV6string(unwrap(octets));
  }

  /**
   * <p>This method takes an array of 4 or 16 primitive bytes and
   * generates an optimal RFC 5952 string encoding suitable for
   * display.</p>
   */

  public static String genIPV6string(byte[] octets)
  {
    StringBuilder result = new StringBuilder();
    int[] stanzas;
    String[] stanzaStrings;
    int i, j;
    int loCompress, hiCompress;
    short absoctets[];

    /* -- */

    if (octets.length == 4)
      {
        result.append("::ffff:"); // this is IPV6's compatibility mode
        result.append(genIPV4string(octets));

        return result.toString();
      }

    if (octets.length != 16)
      {
        throw new IllegalArgumentException("bad number of octets.");
      }

    absoctets = s2u(octets);

    // now for the challenge..

    stanzas = new int[8];

    for (i = 0; i < 8; i++)
      {
        stanzas[i] = absoctets[i*2] * 256 + absoctets[(i*2) + 1];
      }

    stanzaStrings = new String[8];

    // generate hex strings for each 16 bit sequence

    for (i = 0; i < 8; i++)
      {
        stanzaStrings[i] = Integer.toString(stanzas[i], 16);
      }

    // okay, we've got 8 stanzas.. now we have to determine
    // if we want to collapse any part of it to ::

    loCompress = hiCompress = -1;

    i = 0;

    while (i < 8)
      {
        if (i < 7 && (stanzas[i] == 0) && (stanzas[i+1] == 0))
          {
            int localLo, localHi;

            localLo = i;

            for (j = i; (j<8) && (stanzas[j] == 0); j++)
              {
                // just counting up
              }

            localHi = j-1;

            if ((localHi - localLo) > (hiCompress - loCompress))
              {
                hiCompress = localHi;
                loCompress = localLo;

                i = localHi;    // continue our outer loop after this block
              }
          }

        i++;
      }

    // System.err.println("loCompress = " + loCompress);
    // System.err.println("hiCompress = " + hiCompress);

    // okay, we've calculated our compression block, if any..
    // let's also check to see if we want to represent the
    // last 4 bytes in dotted decimal IPv4 form

    if (loCompress == 0 && hiCompress == 5)
      {
        // RFC 4291 IPv4-compatible

        return "::" +
          Short.toString(absoctets[12]) + "." +
          Short.toString(absoctets[13]) + "." +
          Short.toString(absoctets[14]) + "." +
          Short.toString(absoctets[15]);
      }
    else if (loCompress == 0 &&
             hiCompress == 4 &&
             absoctets[10] == 255 &&
             absoctets[11] == 255)
      {
        // RFC 4291 IPv4-mapped

        return "::ffff:" +
          Short.toString(absoctets[12]) + "." +
          Short.toString(absoctets[13]) + "." +
          Short.toString(absoctets[14]) + "." +
          Short.toString(absoctets[15]);
      }
    else if (loCompress == 0 &&
             hiCompress == 3 &&
             absoctets[8] == 255 &&
             absoctets[9] == 255 &&
             absoctets[10] == 0 &&
             absoctets[11] == 0)
      {
        // RFC 2765 IPv4-translated

        return "::ffff:0:" +
          Short.toString(absoctets[12]) + "." +
          Short.toString(absoctets[13]) + "." +
          Short.toString(absoctets[14]) + "." +
          Short.toString(absoctets[15]);
      }

    // nope, we're gonna go all the way in IPv6 form..

    if (loCompress != hiCompress)
      {
        // we've got a compressed area

        i = 0;

        while (i < loCompress)
          {
            if (i > 0)
              {
                result.append(":");
              }

            result.append(stanzaStrings[i++]);
          }

        result.append("::");

        j = hiCompress + 1;

        while (j < 8)
          {
            if (j > (hiCompress+1))
              {
                result.append(":");
              }

            result.append(stanzaStrings[j++]);
          }
      }
    else
      {
        // no compressed area

        for (i = 0; i < 8; i++)
          {
            if (i > 0)
              {
                result.append(":");
              }

            result.append(stanzaStrings[i]);
          }
      }

    return result.toString();
  }

  /**
   * Determines whether a given character is valid or invalid for an
   * IPDBField
   *
   * @param ch the character which is being tested for its validity
   */

  private static final boolean isAllowedV4(char ch)
  {
    return IPv4allowedChars.indexOf(ch) != -1;
  }

  /**
   * Determines whether a given character is valid or invalid for an
   * IPDBField
   *
   * @param ch the character which is being tested for its validity
   */

  private static final boolean isAllowedV6(char ch)
  {
    return IPv6allowedChars.indexOf(ch) != -1;
  }

  /**
   * Convenience function to copy an array of primitive byte to an
   * array of Byte objects.
   */

  private static final Byte[] wrap(byte[] octets)
  {
    Byte[] results = new Byte[octets.length];

    for (int i = 0; i < results.length; i++)
      {
        results[i] = Byte.valueOf(octets[i]);
      }

    return results;
  }

  /**
   * Convenience function to copy an array of Byte objects to an
   * array of primitive bytes.
   */

  private static final byte[] unwrap(Byte[] octets)
  {
    byte[] results = new byte[octets.length];

    for (int i = 0; i < results.length; i++)
      {
        results[i] = octets[i].byteValue();
      }

    return results;
  }

  /**
   * Test rig
   */

  public static void main(String argv[])
  {
    Random rand = new Random();
    int[][] testOctets = {{192,168,0,1},
                          {127,0,0,1},
                          {0x20,0x01,0xdd,0xdd,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00},
                          {0x20,0x01,0x0d,0xb8,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01},
                          {0x20,0x01,0x0d,0xb8,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x02,0x00,0x01},
                          {0x20,0x01,0x0d,0xb8,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01},
                          {0x20,0x01,0x0d,0xb8,0x85,0xa3,0x00,0x00,0x00,0x00,0x8a,0x2e,0x03,0x70,0x73,0x34},
                          {0x20,0x01,0x0d,0xb8,0x85,0xa3,0x00,0x00,0xaa,0xaa,0x8a,0x2e,0x03,0x70,0x73,0x34},
                          {0x20,0x01,0x0d,0xb8,0x85,0xa3,0xaa,0xbb,0xcc,0xdd,0x8a,0x2e,0x03,0x70,0x73,0x34},
                          {0x20,0x01,0x0d,0xb8,0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x01},
                          {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x03,0x70,0x73,0x34},
                          {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xff,0xff,0x03,0x70,0x73,0x34},
                          {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xff,0xff,0x00,0x00,0x03,0x70,0x73,0x34},
                          {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01},
                          {0x10,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00},
                          {0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00},
                          {0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x0f},
                          {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}};

    String[] testStrings = {"10",
                            "10.1",
                            "10.1.4",
                            "192.168.0.1",
                            "127.0.0.1",
                            "::ffff:3.112.115.52",
                            "::",
                            "::1",
                            "::192.168.0.1",
                            "cafe:babe::1",
                            "CAFE:babe::01",
                            "cafe:babe:0000:0000:0000:0000:0000:0001",
                            "f::dead:beef",
                            "f2f2:7474:2e2e:0:3030:0:b2b2:0",
                            "F2F2:7474:2E2E:0000:3030:00:B2B2:0000"};

    /* -- */

    for (int i = 0; i < testOctets.length; i++)
      {
        IPAddress addr = new IPAddress(testOctets[i]);

        String first = addr.toString();
        String second = new IPAddress(first).toString();

        if (addr.isIPv4())
          {
            System.out.println("IPv4 (from octets): " + first);
            System.out.println("IPv4 (roundtrip  ): " + second);
          }
        else
          {
            System.out.println("IPv6 (from octets): " + first);
            System.out.println("IPv6 (roundtrip  ): " + second);
          }

        if (!first.equals(second))
          {
            System.out.println("FAILURE");
          }

        if (!first.equals((new IPAddress(addr.getOctets())).toString()))
          {
            System.out.println("FAILURE!");
          }
      }

    for (int i = 0; i < testStrings.length; i++)
      {
        IPAddress addr = new IPAddress(testStrings[i]);
        IPAddress readdr = new IPAddress(addr.getBytes());

        if (addr.isIPv4())
          {
            System.out.println("IPv4 (from string): " + addr);
            System.out.println("IPv4 (roundtrip  ): " + readdr);
          }
        else
          {
            System.out.println("IPv6 (from string): " + addr);
            System.out.println("IPv6 (roundtrip  ): " + readdr);
          }

        if (!addr.equals(readdr))
          {
            System.out.println("FAILURE");
          }

        if (!addr.toString().equals((new IPAddress(addr.getOctets())).toString()))
          {
            System.out.println("FAILURE!");
          }
      }


    Byte[] octets = new Byte[16];

    for (int i = 0; i < 16; i++)
      {
        octets[i] = Byte.valueOf(u2s(0));
      }

    System.out.println("All zero v6 string: " + genIPV6string(octets));

    octets[15] = Byte.valueOf(u2s(1));

    System.out.println("Trailing 1 string: " + genIPV6string(octets));

    byte[] randbytes = new byte[16];

    rand.nextBytes(randbytes);

    for (int i = 4; i < 16; i++)
      {
        octets[i] = Byte.valueOf(randbytes[i]);
      }

    System.out.println("4 Leading zero rand string: " + genIPV6string(octets));

    for (int i = 0; i < 16; i++)
      {
        octets[i] = Byte.valueOf(u2s(0));
      }

    rand.nextBytes(randbytes);

    for (int i = 0; i < 8; i++)
      {
        if (rand.nextInt() > 0)
          {
            octets[i*2] = Byte.valueOf(randbytes[i]);
            octets[(i*2)+1] = Byte.valueOf(randbytes[i]);

            System.out.print("**");
          }
        else
          {
            System.out.print("00");
          }
      }

    System.out.println();

    System.out.println("Random compression block string: " + genIPV6string(octets));

    for (int i = 0; i < 12; i++)
      {
        octets[i] = Byte.valueOf(u2s(0));
      }

    rand.nextBytes(randbytes);

    for (int i = 12; i < 16; i++)
      {
        octets[i] = Byte.valueOf(randbytes[i]);
      }

    System.out.println("IPv4 compatible string (A): " + genIPV6string(octets));

    for (int i = 0; i < 10; i++)
      {
        octets[i] = Byte.valueOf(u2s(0));
      }

    octets[10] = Byte.valueOf(u2s(255));
    octets[11] = Byte.valueOf(u2s(255));

    rand.nextBytes(randbytes);

    for (int i = 12; i < 16; i++)
      {
        octets[i] = Byte.valueOf(randbytes[i]);
      }

    System.out.println("IPv4 compatible string (B): " + genIPV6string(octets));
  }
}
