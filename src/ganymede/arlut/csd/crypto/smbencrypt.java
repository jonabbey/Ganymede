/*
   smbencrypt.java

   Created: 15 March 2001

   Java Port By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Based on smbencrypt.c and smbdes.c in Samba

   Unix SMB/Netbios implementation.
   Version 1.9.

   a partial implementation of DES designed for use in the 
   SMB authentication protocol

   Copyright (C) Andrew Tridgell 1998
   
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

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
   The University of Texas at Austin

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

package arlut.csd.crypto;

import java.security.MessageDigest;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      smbencrypt

------------------------------------------------------------------------------*/

/**
 * <p>This Java class implements the two cryptographic hash methods used
 * by SMB clients.  In particular, the LANMANHash() and NTUNICODEHash()
 * static methods in this class perform the two hashes required for Samba's
 * encrypted password entries.  As such, this class and the other classes
 * in the broadly named arlut.csd.crypto package provide all the support
 * functions necessary to allow Ganymede to manage Samba's password file.</p>
 *
 * <p>The following notes are from the original Samba source code for the
 * smbdes.c module, which this module is adapted from.</p>
 *
 * <p>--------------------------------------------------</p>
 *
 * <p>Unix SMB/Netbios implementation.<br>
 * Version 1.9.</p>
 *
 * <p>a partial implementation of DES designed for use in the 
 * SMB authentication protocol</p>
 *
 * <p>Copyright (C) Andrew Tridgell 1998</p>
 *
 * <p>--------------------------------------------------</p>
 *
 * <p>NOTES:</p>
 *
 * <p>This code makes no attempt to be fast! In fact, it is a very
 * slow implementation.</p>
 *
 * <p>This code is NOT a complete DES implementation. It implements only
 * the minimum necessary for SMB authentication, as used by all SMB
 * products (including every copy of Microsoft Windows95 ever sold)</p>
 *
 * <p>In particular, it can only do a unchained forward DES pass. This
 * means it is not possible to use this code for encryption/decryption
 * of data, instead it is only useful as a "hash" algorithm.</p>
 *
 * <p>There is no entry point into this code that allows normal DES operation.</p>
 *
 * <p>I believe this means that this code does not come under ITAR
 * regulations but this is NOT a legal opinion. If you are concerned
 * about the applicability of ITAR regulations to this code then you
 * should confirm it for yourself (and maybe let me know if you come
 * up with a different answer to the one above)</p>
 */

public class smbencrypt {

  static char hexdigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  //[56]
  static byte perm1[] = {57, 49, 41, 33, 25, 17,  9,
                         1, 58, 50, 42, 34, 26, 18,
                         10,  2, 59, 51, 43, 35, 27,
                         19, 11,  3, 60, 52, 44, 36,
                         63, 55, 47, 39, 31, 23, 15,
                         7, 62, 54, 46, 38, 30, 22,
                         14,  6, 61, 53, 45, 37, 29,
                         21, 13,  5, 28, 20, 12,  4};
  
  //[48]
  static byte perm2[] = {14, 17, 11, 24,  1,  5,
                         3, 28, 15,  6, 21, 10,
                         23, 19, 12,  4, 26,  8,
                         16,  7, 27, 20, 13,  2,
                         41, 52, 31, 37, 47, 55,
                         30, 40, 51, 45, 33, 48,
                         44, 49, 39, 56, 34, 53,
                         46, 42, 50, 36, 29, 32};

  //[64]
  static byte perm3[] = {58, 50, 42, 34, 26, 18, 10,  2,
                         60, 52, 44, 36, 28, 20, 12,  4,
                         62, 54, 46, 38, 30, 22, 14,  6,
                         64, 56, 48, 40, 32, 24, 16,  8,
                         57, 49, 41, 33, 25, 17,  9,  1,
                         59, 51, 43, 35, 27, 19, 11,  3,
                         61, 53, 45, 37, 29, 21, 13,  5,
                         63, 55, 47, 39, 31, 23, 15,  7};
  
  //[48]
  static byte perm4[] = {   32,  1,  2,  3,  4,  5,
                            4,  5,  6,  7,  8,  9,
                            8,  9, 10, 11, 12, 13,
                            12, 13, 14, 15, 16, 17,
                            16, 17, 18, 19, 20, 21,
                            20, 21, 22, 23, 24, 25,
                            24, 25, 26, 27, 28, 29,
                            28, 29, 30, 31, 32,  1};
  
  //[32]
  static byte perm5[] = {      16,  7, 20, 21,
                               29, 12, 28, 17,
                               1, 15, 23, 26,
                               5, 18, 31, 10,
                               2,  8, 24, 14,
                               32, 27,  3,  9,
                               19, 13, 30,  6,
                               22, 11,  4, 25};
  
  //[64]
  static byte perm6[] ={ 40,  8, 48, 16, 56, 24, 64, 32,
                         39,  7, 47, 15, 55, 23, 63, 31,
                         38,  6, 46, 14, 54, 22, 62, 30,
                         37,  5, 45, 13, 53, 21, 61, 29,
                         36,  4, 44, 12, 52, 20, 60, 28,
                         35,  3, 43, 11, 51, 19, 59, 27,
                         34,  2, 42, 10, 50, 18, 58, 26,
                         33,  1, 41,  9, 49, 17, 57, 25};
  
  
  //[16]
  static byte sc[] = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};
  
  //[8][4][16]
  static byte sbox[][][] = {
    {{14,  4, 13,  1,  2, 15, 11,  8,  3, 10,  6, 12,  5,  9,  0,  7},
     {0, 15,  7,  4, 14,  2, 13,  1, 10,  6, 12, 11,  9,  5,  3,  8},
     {4,  1, 14,  8, 13,  6,  2, 11, 15, 12,  9,  7,  3, 10,  5,  0},
     {15, 12,  8,  2,  4,  9,  1,  7,  5, 11,  3, 14, 10,  0,  6, 13}},
    
    {{15,  1,  8, 14,  6, 11,  3,  4,  9,  7,  2, 13, 12,  0,  5, 10},
     {3, 13,  4,  7, 15,  2,  8, 14, 12,  0,  1, 10,  6,  9, 11,  5},
     {0, 14,  7, 11, 10,  4, 13,  1,  5,  8, 12,  6,  9,  3,  2, 15},
     {13,  8, 10,  1,  3, 15,  4,  2, 11,  6,  7, 12,  0,  5, 14,  9}},
    
    {{10,  0,  9, 14,  6,  3, 15,  5,  1, 13, 12,  7, 11,  4,  2,  8},
     {13,  7,  0,  9,  3,  4,  6, 10,  2,  8,  5, 14, 12, 11, 15,  1},
     {13,  6,  4,  9,  8, 15,  3,  0, 11,  1,  2, 12,  5, 10, 14,  7},
     {1, 10, 13,  0,  6,  9,  8,  7,  4, 15, 14,  3, 11,  5,  2, 12}},
    
    {{7, 13, 14,  3,  0,  6,  9, 10,  1,  2,  8,  5, 11, 12,  4, 15},
     {13,  8, 11,  5,  6, 15,  0,  3,  4,  7,  2, 12,  1, 10, 14,  9},
     {10,  6,  9,  0, 12, 11,  7, 13, 15,  1,  3, 14,  5,  2,  8,  4},
     {3, 15,  0,  6, 10,  1, 13,  8,  9,  4,  5, 11, 12,  7,  2, 14}},
    
    {{2, 12,  4,  1,  7, 10, 11,  6,  8,  5,  3, 15, 13,  0, 14,  9},
     {14, 11,  2, 12,  4,  7, 13,  1,  5,  0, 15, 10,  3,  9,  8,  6},
     {4,  2,  1, 11, 10, 13,  7,  8, 15,  9, 12,  5,  6,  3,  0, 14},
     {11,  8, 12,  7,  1, 14,  2, 13,  6, 15,  0,  9, 10,  4,  5,  3}},
    
    {{12,  1, 10, 15,  9,  2,  6,  8,  0, 13,  3,  4, 14,  7,  5, 11},
     {10, 15,  4,  2,  7, 12,  9,  5,  6,  1, 13, 14,  0, 11,  3,  8},
     {9, 14, 15,  5,  2,  8, 12,  3,  7,  0,  4, 10,  1, 13, 11,  6},
     {4,  3,  2, 12,  9,  5, 15, 10, 11, 14,  1,  7,  6,  0,  8, 13}},
    
    {{4, 11,  2, 14, 15,  0,  8, 13,  3, 12,  9,  7,  5, 10,  6,  1},
     {13,  0, 11,  7,  4,  9,  1, 10, 14,  3,  5, 12,  2, 15,  8,  6},
     {1,  4, 11, 13, 12,  3,  7, 14, 10, 15,  6,  8,  0,  5,  9,  2},
     {6, 11, 13,  8,  1,  4, 10,  7,  9,  5,  0, 15, 14,  2,  3, 12}},
    
    {{13,  2,  8,  4,  6, 15, 11,  1, 10,  9,  3, 14,  5,  0, 12,  7},
     {1, 15, 13,  8, 10,  3,  7,  4, 12,  5,  6, 11,  0, 14,  9,  2},
     {7, 11,  4,  1,  9, 12, 14,  2,  0,  6, 10, 13, 15,  3,  5,  8},
     {2,  1, 14,  7,  4, 10,  8, 13, 15, 12,  9,  0,  3,  5,  6, 11}}};

  /**
   * convert an encoded unsigned byte value into a int
   * with the unsigned value.
   */
  
  static private final int bytes2u(byte b)
  {
    return (int) b & 0xff;
  }

  /**
   * generates a two character hexadecimal string from
   * an unsigned-encoded byte
   */
  
  private static String byteToHex(byte b)
  {
    char cary[] = new char[2];

    cary[0] = hexdigits[bytes2u(b) / 16];
    cary[1] = hexdigits[bytes2u(b) % 16];

    return new String(cary);
  }

  static void permute(byte[] out, byte[] in, byte[] p, int n)
  {
    for (int i=0; i<n; i++)
      {
        out[i] = in[p[i]-1];
      }
  }

  static void lshift(byte[] d, int count, int n)
  {
    byte out[] = new byte[64];
    
    /* -- */
    
    for (int i=0; i<n; i++)
      {
        out[i] = d[(i+count)%n];
      }
    
    for (int i=0; i<n; i++)
      {
        d[i] = out[i];
      }
  }
  
  static void concat(byte[] out, byte[] in1, byte[] in2, int l1, int l2)
  {
    int i = 0;
    int j = 0;
    
    /* -- */
    
    while (l1-- > 0)
      {
        out[i] = in1[i];
        
        i++;
      }
    
    while (l2-- > 0)
      {
        out[i] = in2[j];
        
        i++;
        j++;
      }
  }

  static void xor(byte[] out, byte[] in1, byte[] in2, int n)
  {
    for (int i=0; i<n; i++)
      {
        out[i] = (byte) (in1[i] ^ in2[i]);
      }
  }

  static void dohash(byte[] out, byte[] in, byte[] key, boolean forw)
  {
    int i, j, k;
    byte pk1[] = new byte[56];
    byte c[] = new byte[28];
    byte d[] = new byte[28];
    byte cd[] = new byte[56];
    byte ki[][] = new byte[16][48];
    byte pd1[] = new byte[64];
    byte l[] = new byte[32];
    byte r[] = new byte[32];
    byte rl[] = new byte[64];
    
    /* -- */
    
    permute(pk1, key, perm1, 56);
    
    for (i=0; i<28; i++)
      {
        c[i] = pk1[i];
      }
    
    for (i=0; i<28; i++)
      {
        d[i] = pk1[i+28];
      }
    
    for (i=0; i<16; i++) 
      {
        lshift(c, sc[i], 28);
        lshift(d, sc[i], 28);
        
        concat(cd, c, d, 28, 28); 
        permute(ki[i], cd, perm2, 48); 
      }

    permute(pd1, in, perm3, 64);
    
    for (j=0; j<32; j++) 
      {
        l[j] = pd1[j];
        r[j] = pd1[j+32];
      }
    
    for (i=0; i<16; i++) 
      {
        byte er[] = new byte[48];
        byte erk[] = new byte[48];
        byte b[][] = new byte[8][6];
        byte cb[] = new byte[32];
        byte pcb[] = new byte[32];
        byte r2[] = new byte[32];

        permute(er, r, perm4, 48);

        xor(erk, er, ki[forw ? i : 15 - i], 48);

        for (j=0; j<8; j++)
          {
            for (k=0; k<6; k++)
              {
                b[j][k] = erk[j*6 + k];
              }
          }

        for (j=0; j<8; j++) 
          {
            int m, n;

            m = (b[j][0]<<1) | b[j][5];
          
            n = (b[j][1]<<3) | (b[j][2]<<2) | (b[j][3]<<1) | b[j][4]; 
          
            for (k=0; k<4; k++)
              {
                b[j][k] = (byte) ((sbox[j][m][n] & (1<<(3-k))) != 0 ?1:0);
              }
          }

        for (j=0; j<8; j++)
          {
            for (k=0; k<4; k++)
              {
                cb[j*4+k] = b[j][k];
              }
          }

        permute(pcb, cb, perm5, 32);

        xor(r2, l, pcb, 32);

        for (j=0; j<32; j++)
          {
            l[j] = r[j];
          }

        for (j=0; j<32; j++)
          {
            r[j] = r2[j];
          }
      }
  
    concat(rl, r, l, 32, 32);
    
    permute(out, rl, perm6, 64);
  }

  /**
   * <p>This function reads from str and writes into key</p>
   */
  
  static void str_to_key(byte[] str, int str_offset, byte[] key)
  {
    int i;
    
    /* -- */
    
    key[0] = (byte) (str[str_offset]>>>1);
    key[1] = (byte) (((str[str_offset]&0x01)<<6) | (str[str_offset+1]>>>2));
    key[2] = (byte) (((str[str_offset+1]&0x03)<<5) | (str[str_offset+2]>>>3));
    key[3] = (byte) (((str[str_offset+2]&0x07)<<4) | (str[str_offset+3]>>>4));
    key[4] = (byte) (((str[str_offset+3]&0x0F)<<3) | (str[str_offset+4]>>>5));
    key[5] = (byte) (((str[str_offset+4]&0x1F)<<2) | (str[str_offset+5]>>>6));
    key[6] = (byte) (((str[str_offset+5]&0x3F)<<1) | (str[str_offset+6]>>>7));
    key[7] = (byte) (str[str_offset+6]&0x7F);
    
    for (i=0; i<8; i++)
      {
        key[i] = (byte) (key[i]<<1);
      }
  }

  /**
   * <p>This method actually performs the SMB hash algorithm.</p>
   *
   * @param out An 8 element byte array to hold the results of the
   * hash function
   *
   * @param in An 8 element byte array to hold the known pattern
   * that we are hashing
   *
   * @param key An 8 element byte array that holds information
   * from the secret that we are using to hash the appropriate
   * known pattern.
   */

  static void smbhash(byte[] out, byte[] in, byte[] key)
  {
    smbhash(out, 0, in, 0, key, 0, true);
  }

  /**
   * <p>This method actually performs the SMB hash algorithm.</p>
   *
   * @param out An 8 element byte array to hold the results of the
   * hash function
   *
   * @param in An 8 element byte array to hold the known pattern
   * that we are hashing
   *
   * @param key An 8 element byte array that holds information
   * from the secret that we are using to hash the appropriate
   * known pattern.
   *
   * @param forw If true, we do a forward XOR operation.  If false,
   * we reverse it.
   */

  static void smbhash(byte[] out, byte[] in, byte[] key, boolean forw)
  {
    smbhash(out, 0, in, 0, key, 0, forw);
  }

  /**
   * <p>This method actually performs the SMB hash algorithm.</p>
   *
   * @param out An 8 element byte array to hold the results of the
   * hash function
   *
   * @param in An 8 element byte array to hold the known pattern
   * that we are hashing
   *
   * @param key An 8 element byte array that holds information
   * from the secret that we are using to hash the appropriate
   * known pattern.
   */
  
  static void smbhash(byte[] out, int out_offset, byte[] in, int in_offset, byte[] key, int key_offset)
  {
    smbhash(out, out_offset, in, in_offset, key, key_offset, true);
  }

  /**
   * <p>This method actually performs the SMB hash algorithm.</p>
   *
   * @param out An 8 element byte array to hold the results of the
   * hash function
   *
   * @param in An 8 element byte array to hold the known pattern
   * that we are hashing
   *
   * @param key An 8 element byte array that holds information
   * from the secret that we are using to hash the appropriate
   * known pattern.
   *
   * @param forw If true, we do a forward XOR operation.  If false,
   * we reverse it.
   */

  static void smbhash(byte[] out, int out_offset, byte[] in, int in_offset, byte[] key, int key_offset, boolean forw)
  {
    int i;
    byte outb[] = new byte[64];
    byte inb[] = new byte[64];
    byte keyb[] = new byte[64];
    byte key2[] = new byte[8];
    
    /* -- */
    
    str_to_key(key, key_offset, key2);
    
    for (i=0; i<64; i++) 
      {
        inb[i] = (byte) ((in[in_offset + i/8] & (1<<(7-(i%8)))) != 0 ? 1 : 0);
        keyb[i] = (byte) ((key2[i/8] & (1<<(7-(i%8)))) != 0 ? 1 : 0);
        outb[i] = 0;
      }
    
    dohash(outb, inb, keyb, forw);
    
    for (i=0; i<8; i++) 
      {
        out[out_offset + i] = 0;
      }
    
    for (i=0; i<64; i++)
      {
        if (outb[i] != 0)
          {
            out[out_offset + i/8] |= (1<<(7-(i%8)));
          }
      }
  }

  /**
   * <p>This method actually performs the standard LANMAN DES hashing, using
   * the 14 byte password array p14 as the hashing key and the magic
   * string 'KGS!@#$%' as the data to be hashed.</p>
   */
  
  static void E_P16(byte[] p14, byte[] p16)
  {
    byte sp8[] = {0x4b, 0x47, 0x53, 0x21, 0x40, 0x23, 0x24, 0x25}; // KGS!@#$%
    
    /* -- */
    
    smbhash(p16, sp8, p14);
    smbhash(p16, 8, sp8, 0, p14, 7);
  }
  
  static void E_P24(byte[] p21, byte[] c8, byte[] p24)
  {
    smbhash(p24, c8, p21);
    smbhash(p24, 8, c8, 0, p21, 7);
    smbhash(p24, 16, c8, 0, p21, 14);
  }
  
  static void D_P16(byte[] p14, byte[] in, byte[] out)
  {
    smbhash(out, in, p14, false);
    smbhash(out, 8, in, 8, p14, 7, false);
  }

  static void E_old_pw_hash( byte[] p14, byte[] in, byte[] out)
  {
    smbhash(out, in, p14);
    smbhash(out, 8, in, 8, p14, 7);
  }

  public static void cred_hash1(byte[] out,byte[] in,byte[] key)
  {
    byte buf[] = new byte[8];
    
    /* -- */
    
    smbhash(buf, in, key);
    smbhash(out, 0, buf, 0, key, 9);
  }

  public static void cred_hash2(byte[] out,byte[] in,byte[] key)
  {
    byte buf[] = new byte[8];
    byte key2[] = new byte[8];
    
    /* -- */
    
    smbhash(buf, in, key);
    key2[0] = key[7];
    smbhash(out, buf, key2);
  }
  
  public static void cred_hash3(byte[] out,byte[] in,byte[] key, boolean forw)
  {
    byte key2[] = new byte[8];
    
    /* -- */
    
    smbhash(out, in, key, forw);
    key2[0] = key[7];
    smbhash(out, 8, in, 8, key2, 0, forw);
  }
  
  /**
   * <p>This function does the password hashing used by the NT SAM database</p>
   */
  
  public static void SamOEMhash(byte[] data, byte[] key, boolean bigbuf)
  {
    byte s_box[] = new byte[256];
    int index_i = 0;            // was unsigned char
    int index_j = 0;            // was unsigned char
    int j = 0;                  // was unsigned char
    int ind;
    
    /* -- */
    
    for (ind = 0; ind < 256; ind++)
      {
        s_box[ind] = (byte) ind;
      }
    
    for (ind = 0; ind < 256; ind++)
      {
        byte tc;
        
        j += (bytes2u(s_box[ind]) + bytes2u(key[ind%16]));

        // the original C code depended on j overflowing and wrapping,
        // we have to do it manually since we are using a larger type

        j &= 0xff;
      
        tc = s_box[ind];
        s_box[ind] = s_box[j];
        s_box[j] = tc;
      }

    for (ind = 0; ind < (bigbuf ? 516 : 16); ind++)
      {
        byte tc;
        int t;
      
        index_i++; 
        index_i &= 0xff;

        index_j += s_box[index_i];
        index_j &= 0xff;
      
        tc = s_box[index_i];
        s_box[index_i] = s_box[index_j];
        s_box[index_j] = tc;
      
        t = bytes2u(s_box[index_i]) + bytes2u(s_box[index_j]);
        data[ind] = (byte) (data[ind] ^ s_box[t]);
      }
  }

  /**
   * <p>This method generates a LANMAN-compatible DES hashing of the
   * input password string, as used in the Samba encrypted password
   * file.</p>
   *
   * <p>Only the first fourteen characters of the password are used,
   * and they are converted to uppercase before hashing.</p>
   */

  public static String LANMANHash(String password)
  {
    byte input[] = new byte[14];
    byte output[] = new byte[16];

    if (password.length() > 14)
      {
        password = password.substring(0,14);
      }

    char c_ary[] = password.toUpperCase().toCharArray();

    int i;

    for (i = 0; i < c_ary.length && i < 14; i++)
      {
        input[i] = (byte) c_ary[i];
      }

    E_P16(input, output);

    StringBuilder result = new StringBuilder();

    for (i = 0; i < 16; i++)
      {
        result.append(byteToHex(output[i]));
      }

    return result.toString();
  }

  /**
   * <p>This method generates a SMB-compatible MD4 hashing of the
   * input password string in little-endian Unicode format, as used in
   * the Samba encrypted password file.</p>
   *
   * <p>This method hashes the first 128 characters of the password,
   * with full Unicode range and case preservation.</p>
   */

  public static String NTUNICODEHash(String password)
  {
    if (password.length() > 128)
      {
        password = password.substring(0, 128);
      }

    char c_ary[] = password.toCharArray();

    // we need to simulate NT's little endian Unicode
    // representation before we pass this to md4

    byte wpwd[] = new byte[c_ary.length * 2];

    for (int i = 0; i < c_ary.length; i++)
      {
        char c = c_ary[i];

        wpwd[2*i] = (byte) (c & 0x00ff);
        wpwd[2*i+1] = (byte) (c & 0xff00);
      }

    md4 m = new md4(wpwd);

    m.calc();

    return m.toString();
  }

  /**
   * <p>test rig.</p>
   */

  public static void main(String argv[])
  {
    if (argv.length != 1)
      {
        System.err.println("Error, must provide a password value to hash");
        System.exit(1);
      }

    System.err.println(LANMANHash(argv[0]));
    System.err.println(NTUNICODEHash(argv[0]));
    System.exit(0);
  }
}
