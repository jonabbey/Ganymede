/*
   Util.java

   This is a reformatted copy of Justin Chapweske's Artistic Licensed
   Java port of Alec Muffett's cracklib, circa version 0.5.

   All changes to this file relative to that located in the original
   archive from

   http://sourceforge.net/projects/solinger/

   are for the purpose of keeping with the code formatting rules we
   use in the Ganymede project, and to implement Ganymede-compatible
   localization support for the message strings.

*/

package org.solinger.cracklib;

import java.io.*;

public class Util {

  public static final short getShortLE(byte[] b)
  {
    return (short) (((b[1] & 0xff) << 8) | (b[0] & 0xff));
  }

  public static final int getIntLE(byte[] b)
  {
    return (int) (((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff));
  }

  public static final byte[] getBytesLE(int i)
  {
    byte[] b = new byte[4];
    b[0] = (byte) (i & 0xff);
    b[1] = (byte) ((i >> 8) & 0xff);
    b[2] = (byte) ((i >> 16) & 0xff);
    b[3] = (byte) ((i >> 24) & 0xff);
    return b;
  }

  public static final byte[] getBytesLE(short s)
  {
    byte[] b = new byte[2];
    b[0] = (byte) (s & 0xff);
    b[1] = (byte) ((s >> 8) & 0xff);
    return b;
  }

  public static final int readFullish(RandomAccessFile raf, byte[] b,
				      int off, int len) throws IOException
  {
    int i = 0;
    int j = 0;

    while ((j=raf.read(b,off+i,len-i)) != -1 && i != len)
      {
	i += j;
      }

    return i;
  }
}
