/*
 * $Header: /home/broccol/ganymede/cvsroot/ganymede/src/md5/MD5TestSuite.java,v 1.2 2002/03/16 01:46:39 broccol Exp $
 *
 * MD5 test suite in Java JDK Beta-2
 * written Santeri Paavolainen, Helsinki Finland 1996
 * (c) Santeri Paavolainen, Helsinki Finland 1996
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *
 * This Java class has been derived from the RSA Data Security, Inc. MD5 
 * Message-Digest Algorithm and/or its reference implementation. 
 *
 *
 * See http://www.cs.hut.fi/~santtu/java/ for more information on the
 * MD5 class and other classes. 
 *
 * This is runnable Java application mimicing functionality of the
 * md5driver.c as in RFC1312 RSA reference MD5 implementation. Only
 * notable exception is the -s command-line option requiring its string
 * argument in the next argument place instead of directly following the
 * option.
 *
 * $Log: MD5TestSuite.java,v $
 * Revision 1.2  2002/03/16 01:46:39  broccol
 * Moved the MD5 classes into the md5 package to make 1.4 javac happy
 *
 * Revision 1.1  1999/08/05 22:07:04  broccol
 * Added support for the MD5 classes.
 *
 * Revision 1.2  1996/04/15 07:28:09  santtu
 * Added GPL statemets, and RSA derivate stametemetsnnts.
 *
 * Revision 1.1  1996/01/07 20:51:59  santtu
 * Initial revision
 *
 */

package md5;

import MD5InputStream;
import java.io.*;

class MD5TestSuite {
  private static void printHex (byte b) {
    if (((int) b & 0xff) < 0x10)
      System.out.print("0");
    
    System.out.print(Long.toString(((int) b) & 0xff, 16));
  }

  private static void teststring (String s, String real) {
    byte 	hash[];
    MD5 	md5 = new MD5();
    int 	i;
    String	hex;

    System.out.print("MD5(\"" + s + "\") = ");

    md5.Init();
    md5.Update(s);
    hex = md5.asHex();

    if (real == null || hex.equals(real))
      System.out.println(hex);
    else
      System.out.println(hex + " should be " + real);
  }

  public static void testsuite () {	
    teststring("", "d41d8cd98f00b204e9800998ecf8427e");
    teststring("a", "0cc175b9c0f1b6a831c399e269772661");
    teststring("abc", "900150983cd24fb0d6963f7d28e17f72");
    teststring("message digest", "f96b697d7cb7938d525a2f31aaf161d0");
    teststring("abcdefghijklmnopqrstuvwxyz", "c3fcd3d76192e4007dfb496cca67e13b");
    teststring("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", "d174ab98d277d9f5a5611c2c9f419d9f");
    teststring("12345678901234567890123456789012345678901234567890123456789012345678901234567890", "57edf4a22be3c955ac49da2e2107b67a");
  }

  public static void teststream (String file, InputStream in) {
    MD5InputStream	md5in;
    byte		buf[] = new byte[512];
    int			l;

    md5in = new MD5InputStream(in);
    do {
      try {
	l = md5in.read();
      } catch (IOException e) {
	l = -1;
      };
    } while (l > 0);

    if (file != null)
      System.out.print(file + ": ");

    System.out.println(MD5.asHex(md5in.hash()));
  }

  public static void testfile (String file) {
    FileInputStream	in;

    try {
      in = new FileInputStream(file);
      teststream(file, in);
    } catch (FileNotFoundException e) {
      System.out.println("File \"" + file + "\" not found");
    }
  }

  public static void testtimetrial () {
    int		test_block_len = 1000,
      test_block_count = 1000;
    byte	block[] = new byte[test_block_len],
      hash[];
    int		i;
    long	start, end;
    MD5		md5;
    
    System.out.print("MD5 time trial. Digesting " +
		     test_block_count + " " +
		     test_block_len + "-byte blocks ...");
    
    
    for (i = 0; i < block.length; i++) {
      block[i] = (byte) (i & 0xff);
    }

    start = System.currentTimeMillis();

    md5 = new MD5();

    for (i = 0; i < test_block_count; i++)
      md5.Update(block, test_block_len);

    hash = md5.Final();

    end = System.currentTimeMillis();

    System.out.print(" done\nDigest = " +
		     md5.asHex() + 
		     "\nTime = " +
		     ((end - start) / 1000) + " seconds\n");
    
    System.out.print("Speed = " +
		     (test_block_len * test_block_count) / ((end - start) / 1000) +
		     " bytes/second\n");
  }

  public static void main (String args[]) {	
    int			l, i;
    String		file = "MD5.java";

    for (i = 0; i < args.length; i++) {
      if (args[i].equals("-t")) {
	testtimetrial();
      } else if (args[i].equals("-x")) {
	testsuite();
      } else if (args[i].equals("-s")) {
	if ((i + 1) < args.length) {
	  teststring(args[i + 1], null);

	  i++;
	}
      } else {
	testfile(args[i]);
      }
    }

    if (args.length == 0)
      teststream(null, System.in);
  }
}
