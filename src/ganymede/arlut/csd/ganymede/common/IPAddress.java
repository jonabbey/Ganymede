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
import java.util.List;
import java.util.Random;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPAddress

------------------------------------------------------------------------------*/

/**
 * This is an immutable wrapper class for the Byte array IP value
 * representation that allows IP addresses to be safely stored and
 * processed in the Ganymede system.
 */

public final class IPAddress implements Cloneable, java.io.Serializable {

  static final boolean debug = false;
  private static String IPv4allowedChars = "1234567890.";
  private static String IPv6allowedChars = "1234567890.abcdefABCDEF:";

  // ---

  private final Byte[] address;

  /* -- */

  public IPAddress(Byte[] address)
  {
    if (address == null)
      {
        throw new NullPointerException();
      }

    if (address.length !=4 && address.length != 16)
      {
        throw new IllegalArgumentException();
      }

    this.address = (Byte[]) address.clone();
  }

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
   * Equality test.  This IPAddress can be compared to either
   * another IPAddress object or to an array of Bytes.
   */

  @Override public boolean equals(Object value)
  {
    Byte[] foreignBytes;

    /* -- */

    if (value == null)
      {
        return false;
      }

    if (value instanceof IPAddress)
      {
        foreignBytes = ((IPAddress) value).address;
      }
    else
      {
        return false;
      }

    if (foreignBytes.length != this.address.length)
      {
        return false;
      }

    for (int i = 0; i < this.address.length; i++)
      {
        try
          {
            if (!foreignBytes[i].equals(this.address[i]))
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

  @Override public Object clone()
  {
    return new IPAddress(this.getBytes());
  }

  /**
   * <p>Getter method.  This method creates a copy of the address
   * before returning it, to avoid the calling function messing with
   * the internally editable array representation.</p>
   */

  public Byte[] getBytes()
  {
    Byte[] result = new Byte[this.address.length];

    System.arraycopy(this.address, 0, result, 0, this.address.length);

    return result;
  }

  public byte getOctet(int index)
  {
    if (index < 0 || index > this.address.length)
      {
        throw new ArrayIndexOutOfBoundsException();
      }

    return this.address[index].byteValue();
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

  public String toString()
  {
    return IPAddress.genIPString(this.address);
  }

  public void emit(DataOutput out) throws IOException
  {
    out.writeByte(this.address.length);

    for (int i = 0; i < this.address.length; i++)
      {
        out.writeByte(this.address[i].byteValue());
      }
  }

  /*------------------------------------------------------------


                            Static Methods


    ------------------------------------------------------------*/

  public static IPAddress readIPAddr(DataInput in) throws IOException
  {
    Byte[] bytes = new Byte[in.readByte()];

    for (int j = 0; j < bytes.length; j++)
      {
        bytes[j] = Byte.valueOf(in.readByte());
      }

    return new IPAddress(bytes);
  }

  /**
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.
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
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
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
   * <p>Returns an IPv4 or IPv6 string, based on the length of the
   * octets parameter.</p>
   */

  public static String genIPString(Byte[] octets)
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
        throw new IllegalArgumentException();
      }
  }

  /**
   * <p>This method takes an IPv4 string in standard format and
   * generates an array of 4 bytes that the Ganymede server can
   * accept.</p>
   */

  public static Byte[] genIPV4bytes(String input)
  {
    Byte[] result = new Byte[4];
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
        result[i] = Byte.valueOf(u2s(0));
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
        result[i] = Byte.valueOf(u2s(Integer.parseInt(octets.get(i))));
      }

    return result;
  }

  /**
   * <p>This method generates a standard string representation of an
   * IPv4 address from an array of 4 octets.</p>
   */

  public static String genIPV4string(Byte[] octets)
  {
    StringBuilder result = new StringBuilder();
    Short absoctets[];

    /* -- */

    if (octets.length != 4)
      {
        throw new IllegalArgumentException("bad number of octets.");
      }

    absoctets = new Short[octets.length];

    for (int i = 0; i < octets.length; i++)
      {
        absoctets[i] = Short.valueOf((short) (octets[i].shortValue() + 128)); // don't want negative values
      }

    result.append(absoctets[0].toString());
    result.append(".");
    result.append(absoctets[1].toString());
    result.append(".");
    result.append(absoctets[2].toString());
    result.append(".");
    result.append(absoctets[3].toString());

    return result.toString();
  }

  /**
   * <p>This method takes an IPv6 string in any of the standard RFC
   * 1884 formats or a standard IPv4 string and generates an array of
   * 16 bytes that the Ganymede server can accept as an IPv6
   * address.</p>
   */

  public static Byte[] genIPV6bytes(String input)
  {
    Byte[] result = new Byte[16];
    Byte[] ipv4bytes = null;
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
        result[i] = Byte.valueOf(u2s(0));
      }

    // trim the input

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

        result[10] = Byte.valueOf(u2s(255));
        result[11] = Byte.valueOf(u2s(255));
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

    if (debug)
      {
        System.err.println("tailBytes = " + tailBytes);
        System.err.println("tailOffset = " + tailOffset);
      }

    //

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

        result[i * 2] = Byte.valueOf(u2s(Integer.parseInt(tmp.substring(0, 2), 16)));
        result[(i * 2) + 1] = Byte.valueOf(u2s(Integer.parseInt(tmp.substring(2, 4), 16)));

        if (debug)
          {
            System.err.println("Byte " + (i*2) + " = " + s2u(result[i*2].byteValue()));
            System.err.println("Byte " + ((i*2)+1) + " = " + s2u(result[(i*2)+1].byteValue()));
          }
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

        result[tailOffset + (x * 2)] = Byte.valueOf(u2s(Integer.parseInt(tmp.substring(0, 2), 16)));
        result[tailOffset + (x * 2) + 1] = Byte.valueOf(u2s(Integer.parseInt(tmp.substring(2, 4), 16)));

        if (debug)
          {
            System.err.println("Byte " + (tailOffset + (x*2)) + " = " + s2u(result[(tailOffset + (x*2))].byteValue()));
            System.err.println("Byte " + (tailOffset + ((x*2)+1)) + " = " + s2u(result[tailOffset + (x*2) + 1].byteValue()));
          }

      }

    return result;
  }

  /**
   * <p>This method takes an array of 4 or 16 bytes and generates an
   * optimal RFC 1884 string encoding suitable for display.</p>
   */

  public static String genIPV6string(Byte[] octets)
  {
    StringBuilder result = new StringBuilder();
    int[] stanzas;
    String[] stanzaStrings;
    int i, j;
    int loCompress, hiCompress;
    Short absoctets[];

    /* -- */

    absoctets = new Short[octets.length];

    for (i = 0; i < octets.length; i++)
      {
        absoctets[i] = Short.valueOf((short) (octets[i].shortValue() + 128)); // don't want negative values

        //      System.err.println("Converting byte " + octets[i].intValue() + " to abs " + absoctets[i].intValue());
      }

    if (absoctets.length == 4)
      {
        // okay, here's the easy one..

        result.append("::FFFF:"); // this is IPV6's compatibility mode

        result.append(absoctets[0].toString());
        result.append(".");
        result.append(absoctets[1].toString());
        result.append(".");
        result.append(absoctets[2].toString());
        result.append(".");
        result.append(absoctets[3].toString());

        return result.toString();
      }

    if (absoctets.length != 16)
      {
        throw new IllegalArgumentException("bad number of octets.");
      }

    // now for the challenge..

    stanzas = new int[8];

    for (i = 0; i < 8; i++)
      {
        stanzas[i] = (absoctets[i*2].intValue()*256) + absoctets[(i*2) + 1].intValue();
      }

    stanzaStrings = new String[8];

    // generate hex strings for each 16 bit sequence

    for (i = 0; i < 8; i++)
      {
        stanzaStrings[i] = Integer.toString(stanzas[i], 16); // generate the 4 hex digits

        //      System.err.println("Hex for " + stanzas[i] + " is " + stanzaStrings[i]);
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

    if ((loCompress == 0) && (hiCompress == 5))
      {
        return "::" + absoctets[12].toString() + "." + absoctets[13].toString() + "." +
          absoctets[14].toString() + "." + absoctets[15].toString();
      }
    else if ((loCompress == 0) && (hiCompress == 4) &&
             (absoctets[10].shortValue() == 255) && (absoctets[11].shortValue() == 255))
      {
        return "::FFFF:" + absoctets[12].toString() + "." + absoctets[13].toString() + "." +
          absoctets[14].toString() + "." + absoctets[15].toString();
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

    return result.toString().toUpperCase();
  }

  /**
   * Test rig
   */

  public static void main(String argv[])
  {
    Byte[] octets;
    Random rand = new Random();

    /* -- */

    octets = new Byte[16];

    for (int i = 0; i < 16; i++)
      {
        octets[i] = Byte.valueOf((byte) -128);
      }

    System.out.println("All zero v6 string: " + genIPV6string(octets));

    octets[15] = Byte.valueOf((byte) -127);

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
        octets[i] = Byte.valueOf((byte) -128);
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
        octets[i] = Byte.valueOf((byte) -128);
      }

    rand.nextBytes(randbytes);

    for (int i = 12; i < 16; i++)
      {
        octets[i] = Byte.valueOf(randbytes[i]);
      }

    System.out.println("IPv4 compatible string (A): " + genIPV6string(octets));

    for (int i = 0; i < 10; i++)
      {
        octets[i] = Byte.valueOf((byte) -128);
      }

    octets[10] = Byte.valueOf((byte) 127);
    octets[11] = Byte.valueOf((byte) 127);

    rand.nextBytes(randbytes);

    for (int i = 12; i < 16; i++)
      {
        octets[i] = Byte.valueOf(randbytes[i]);
      }

    System.out.println("IPv4 compatible string (B): " + genIPV6string(octets));
  }
}
