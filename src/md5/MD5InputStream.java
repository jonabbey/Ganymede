/* 
 * $Header: /home/broccol/ganymede/cvsroot/ganymede/src/md5/MD5InputStream.java,v 1.2 2002/03/16 01:46:39 broccol Exp $
 *
 * MD5InputStream, a subclass of FilterInputStream implementing MD5
 * functionality on a stream.
 *
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
 * 
 * See http://www.cs.hut.fi/~santtu/java/ for more information on this
 * and the MD5 class.  
 *
 * $Log: MD5InputStream.java,v $
 * Revision 1.2  2002/03/16 01:46:39  broccol
 * Moved the MD5 classes into the md5 package to make 1.4 javac happy
 *
 * Revision 1.1  1999/08/05 22:07:03  broccol
 * Added support for the MD5 classes.
 *
 * Revision 1.3  1996/04/15 07:28:09  santtu
 * Added GPL statemets, and RSA derivate stametemetsnnts.
 *
 * Revision 1.2  1996/01/09 10:20:44  santtu
 * Changed read() method to use offset too invoking md5.Update()
 *
 * Revision 1.1  1996/01/07 20:51:59  santtu
 * Initial revision
 *
 */

package md5;

import java.io.*;

/**
 * MD5InputStream is a subclass of FilterInputStream adding MD5
 * hashing of the read input.
 *
 * @version	$Revision: 1.2 $
 * @author	Santeri Paavolainen <santtu@cs.hut.fi>
 */

public class MD5InputStream extends FilterInputStream {
  /**
   * MD5 context
   */
  private MD5	md5;
  
  /**
   * Creates a MD5InputStream
   * @param in	The input stream
   */
  public MD5InputStream (InputStream in) {
    super(in);

    md5 = new MD5();
  }

  /**
   * Read a byte of data. 
   * @see java.io.FilterInputStream
   */
  public int read() throws IOException {
    int c;

    if ((c = in.read()) == -1)
      return c;

    if ((c & ~0xff) != 0) {
      System.out.println("MD5InputStream.read() got character with (c & ~0xff) != 0)!");
    } else {
      md5.Update((byte) c);
    }

    return c;
  }

  /**
   * Reads into an array of bytes.
   *
   * @see java.lang.FilterInputStream
   */
  public int read (byte bytes[], int offset, int length) throws IOException {
    int	r;
    
    if ((r = in.read(bytes, offset, length)) == -1)
      return r;

    md5.Update(bytes, offset, r);

    return r;
  }

  /**
   * Returns array of bytes representing hash of the stream as
   * finalized for the current state. 
   * @see MD5.Final()
   */

  public byte [] hash () {
    return md5.Final();
  }
}
