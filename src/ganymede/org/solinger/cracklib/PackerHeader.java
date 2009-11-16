/*
   PackerHeader.java

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

public class PackerHeader {

  int magic;
  int numWords;
  short blockLen;
  short pad;

  public PackerHeader()
  {
  }

  public PackerHeader(int magic, int numWords, short blockLen, short pad)
  {
    this.magic = magic;
    this.numWords = numWords;
    this.blockLen = blockLen;
    this.pad = pad;
  }

  public int getMagic()
  {
    return magic;
  }

  public void setMagic(int magic)
  {
    this.magic = magic;
  }

  public int getNumWords()
  {
    return numWords;
  }

  public void setNumWords(int num)
  {
    this.numWords = num;
  }

  public short getBlockLen()
  {
    return blockLen;
  }

  public void setBlockLen(short len)
  {
    this.blockLen = len;
  }

  public short getPad()
  {
    return pad;
  }

  public void setPad(short pad)
  {
    this.pad = pad;
  }

  public static final int sizeOf()
  {
    return 12;
  }

  public static PackerHeader parse(RandomAccessFile raf) throws IOException
  {
    byte[] b = new byte[sizeOf()];
    raf.readFully(b);
    return parse(b);
  }

  public static PackerHeader parse(byte[] b)
  {
    byte[] b1 = new byte[4]; // magic, numWords
    byte[] b2 = new byte[2]; // blocklen, pad

    System.arraycopy(b,0,b1,0,4);
    int magic = Util.getIntLE(b1);
    System.arraycopy(b,4,b1,0,4);
    int numWords = Util.getIntLE(b1);
    System.arraycopy(b,8,b2,0,2);
    short blockLen = Util.getShortLE(b2);
    System.arraycopy(b,10,b2,0,2);
    short pad = Util.getShortLE(b2);
    return new PackerHeader(magic,numWords,blockLen,pad);
  }

  public byte[] getBytes()
  {
    byte[] b = new byte[sizeOf()];
    System.arraycopy(Util.getBytesLE(magic),0,b,0,4);
    System.arraycopy(Util.getBytesLE(numWords),0,b,4,4);
    System.arraycopy(Util.getBytesLE(blockLen),0,b,8,2);
    System.arraycopy(Util.getBytesLE(pad),0,b,10,2);
    return b;
  }

  public String toString()
  {
    return "magic=0x"+Integer.toHexString(magic)+",numWords="+numWords+
      ",blockLen="+blockLen+",pad="+pad;
  }
}
