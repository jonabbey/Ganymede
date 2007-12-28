 /*
   Sha256Crypt.java

   Created: 18 December 2007
   Release: $Name:  $
   Version: $Revision: 1.00 $
   Last Mod Date: $Date: 2007/12/18 06:02:34 $

   Java Port By: James Ratcliff, falazar@arlut.utexas.edu

   Algorithm taken from:
     SHA256-based Unix crypt implementation.
     Released into the Public Domain by Ulrich Drepper <drepper@redhat.com>.
     http://people.redhat.com/drepper/SHA-crypt.txt

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2008
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.crypto;

import java.security.MessageDigest;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     Sha256Crypt

------------------------------------------------------------------------------*/

/**
 * This class defines a method, {@link
 * Sha256Crypt#Sha256_crypt(java.lang.String, java.lang.String)
 * Sha256_crypt()}, which takes a password and a salt string and
 * generates a Sha256 encrypted password entry.
 *
 * Algorithm taken from:
 *   SHA256-based Unix crypt implementation.
 *   Released into the Public Domain by Ulrich Drepper <drepper@redhat.com>.
 *   http://people.redhat.com/drepper/SHA-crypt.txt
 */

public final class Sha256Crypt
{
  static private final String sha256_salt_prefix = "$5$";
  static private final String sha256_rounds_prefix = "rounds=";
  static private final int SALT_LEN_MAX = 16;
  static private final int ROUNDS_DEFAULT = 5000;
  static private final int ROUNDS_MIN = 1000;
  static private final int ROUNDS_MAX = 999999999;
  static private final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  static private MessageDigest getSHA256()
  {
    try
      {
	return MessageDigest.getInstance("SHA-256");
      }
    catch (java.security.NoSuchAlgorithmException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * This method actually generates an
   * Sha256 crypted password hash from a plaintext password and a
   * salt.
   *
   * The resulting string will be in the form '$5$&lt;salt&gt;$&lt;hashed mess&gt;
   *
   * @param password Plaintext password
   *
   * @return A Sha256 Unix Crypt encrypted password field.
   */

  public static final String Sha256_crypt(String keyStr, String saltStr)
  {
    MessageDigest ctx = getSHA256();
    MessageDigest alt_ctx = getSHA256();
    byte[] alt_result;
    byte[] temp_result;
    byte[] p_bytes = null;
    byte[] s_bytes = null;
    int cnt, cnt2;
    int rounds = ROUNDS_DEFAULT;  // Default number of rounds.
    boolean rounds_custom = false;
    StringBuffer buffer;

    /* -- */

    if (saltStr.startsWith(sha256_salt_prefix))
      {
	saltStr = saltStr.substring(sha256_salt_prefix.length());
      }

    if (saltStr.startsWith(sha256_rounds_prefix))
      {
	String num = saltStr.substring(sha256_rounds_prefix.length(), saltStr.indexOf('$'));
	int srounds = Integer.valueOf(num).intValue();
	saltStr = saltStr.substring(saltStr.indexOf('$')+1);
	rounds = Math.max(ROUNDS_MIN, Math.min(srounds, ROUNDS_MAX));
	rounds_custom = true;
      }

    if (saltStr.length() > SALT_LEN_MAX)
      {
	saltStr = saltStr.substring(0, SALT_LEN_MAX);
      }

    byte[] key = keyStr.getBytes();
    byte[] salt = saltStr.getBytes();

    ctx.reset();
    ctx.update(key, 0, key.length);
    ctx.update(salt, 0, salt.length);

    alt_ctx.reset();
    alt_ctx.update(key, 0, key.length);
    alt_ctx.update(salt, 0, salt.length);
    alt_ctx.update(key, 0, key.length);

    alt_result = alt_ctx.digest();

    for (cnt = key.length; cnt > 32; cnt -= 32)
      {
	ctx.update(alt_result, 0, 32);
      }

    ctx.update(alt_result, 0, cnt);

    for (cnt = key.length; cnt > 0; cnt >>= 1)
      {
	if ((cnt & 1) != 0)
	  {
	    ctx.update(alt_result, 0, 32);
	  }
	else
	  {
	    ctx.update(key, 0, key.length);
	  }
      }

    alt_result = ctx.digest();

    alt_ctx.reset();

    for (cnt = 0; cnt < key.length; ++cnt)
      {
	alt_ctx.update(key, 0, key.length);
      }

    temp_result = alt_ctx.digest();

    p_bytes = new byte[key.length];

    for (cnt2 = 0, cnt = p_bytes.length; cnt >= 32; cnt -= 32)
      {
	System.arraycopy(temp_result, 0, p_bytes, cnt2, 32);
	cnt2 += 32;
      }

    System.arraycopy(temp_result, 0, p_bytes, cnt2, cnt);

    alt_ctx.reset();

    for (cnt = 0; cnt < 16 + (alt_result[0]&0xFF); ++cnt)
      {
	alt_ctx.update(salt, 0, salt.length);
      }

    temp_result = alt_ctx.digest();

    s_bytes = new byte[salt.length];

    for (cnt2 = 0, cnt = s_bytes.length; cnt >= 32; cnt -= 32)
      {
	System.arraycopy(temp_result, 0, s_bytes, cnt2, 32);
	cnt2 += 32;
      }

    System.arraycopy(temp_result, 0, s_bytes, cnt2, cnt);

    /* Repeatedly run the collected hash value through SHA256 to burn
       CPU cycles.  */

    for (cnt = 0; cnt < rounds; ++cnt)
      {
	ctx.reset();

	if ((cnt & 1) != 0)
	  {
	    ctx.update(p_bytes, 0, key.length);
	  }
	else
	  {
	    ctx.update (alt_result, 0, 32);
	  }

	if (cnt % 3 != 0)
	  {
	    ctx.update(s_bytes, 0, salt.length);
	  }

	if (cnt % 7 != 0)
	  {
	    ctx.update(p_bytes, 0, key.length);
	  }

	if ((cnt & 1) != 0)
	  {
	    ctx.update(alt_result, 0, 32);
	  }
	else
	  {
	    ctx.update(p_bytes, 0, key.length);
	  }

	alt_result = ctx.digest();
      }

    buffer = new StringBuffer(sha256_salt_prefix);

    if (rounds_custom)
      {
	buffer.append(sha256_rounds_prefix);
	buffer.append(rounds);
	buffer.append("$");
      }

    buffer.append(saltStr);
    buffer.append("$");

    buffer.append(b64_from_24bit (alt_result[0],  alt_result[10], alt_result[20], 4));
    buffer.append(b64_from_24bit (alt_result[21], alt_result[1],  alt_result[11], 4));
    buffer.append(b64_from_24bit (alt_result[12], alt_result[22], alt_result[2],  4));
    buffer.append(b64_from_24bit (alt_result[3],  alt_result[13], alt_result[23], 4));
    buffer.append(b64_from_24bit (alt_result[24], alt_result[4],  alt_result[14], 4));
    buffer.append(b64_from_24bit (alt_result[15], alt_result[25], alt_result[5],  4));
    buffer.append(b64_from_24bit (alt_result[6],  alt_result[16], alt_result[26], 4));
    buffer.append(b64_from_24bit (alt_result[27], alt_result[7],  alt_result[17], 4));
    buffer.append(b64_from_24bit (alt_result[18], alt_result[28], alt_result[8],  4));
    buffer.append(b64_from_24bit (alt_result[9],  alt_result[19], alt_result[29], 4));
    buffer.append(b64_from_24bit ((byte)0x00,     alt_result[31],  alt_result[30], 3));

    /* Clear the buffer for the intermediate result so that people
       attaching to processes or reading core dumps cannot get any
       information. */

    ctx.reset();

    return buffer.toString();
  }

  private static final String b64_from_24bit(byte B2, byte B1, byte B0, int size)
  {
    int v = ((((int) B2) & 0xFF) << 16) | ((((int) B1) & 0xFF) << 8) | ((int)B0 & 0xff);

    StringBuffer result = new StringBuffer();

    while (--size >= 0)
      {
	result.append(itoa64.charAt((int) (v & 0x3f)));
	v >>>= 6;
      }

    return result.toString();
  }

  /**
   * This method tests a plaintext password against a md5Crypt'ed hash and returns
   * true if the password matches the hash.
   *
   * This method will work properly whether the hashtext was crypted
   * using the default FreeBSD md5Crypt algorithm or the Apache
   * md5Crypt variant.
   *
   * @param plaintextPass The plaintext password text to test.
   * @param md5CryptText The Apache or FreeBSD-md5Crypted hash used to authenticate the plaintextPass.
   */

  static public final boolean verifyPassword(String plaintextPass, String salt, String sha256CryptText)
  {
    if (sha256CryptText.startsWith("$5$"))
      {
        return sha256CryptText.equals(Sha256_crypt(plaintextPass, salt));
      }
    else
      {
        throw new RuntimeException("Bad sha256CryptText");
      }
  }

  /**
   * Validate our implementation using test data from Ulrich Drepper's
   * C implementation.
   */

  private static void selfTest()
  {
    String msgs[] =
      {
	"$5$saltstring", "Hello world!", "$5$saltstring$5B8vYYiY.CVt1RlTTf8KbXBH3hsxY/GNooZaBBGWEc5",
	"$5$rounds=10000$saltstringsaltstring", "Hello world!", "$5$rounds=10000$saltstringsaltst$3xv.VbSHBb41AL9AvLeujZkZRBAwqFMz2.opqey6IcA",
	"$5$rounds=5000$toolongsaltstring", "This is just a test", "$5$rounds=5000$toolongsaltstrin$Un/5jzAHMgOGZ5.mWJpuVolil07guHPvOW8mGRcvxa5",
	"$5$rounds=1400$anotherlongsaltstring", "a very much longer text to encrypt.  This one even stretches over morethan one line.", "$5$rounds=1400$anotherlongsalts$Rx.j8H.h8HjEDGomFU8bDkXm3XIUnzyxf12oP84Bnq1",
	"$5$rounds=77777$short", "we have a short salt string but not a short password", "$5$rounds=77777$short$JiO1O3ZpDAxGJeaDIuqCoEFysAe1mZNJRs3pw0KQRd/",
	"$5$rounds=123456$asaltof16chars..", "a short string", "$5$rounds=123456$asaltof16chars..$gP3VQ/6X7UUEW3HkBn2w1/Ptq2jxPyzV/cZKmF/wJvD",
	"$5$rounds=10$roundstoolow", "the minimum number is still observed", "$5$rounds=1000$roundstoolow$yfvwcWrQ8l/K0DAWyuPMDNHpIVlTQebY9l/gL972bIC"
      };

    System.out.println("Starting Sha256Crypt tests now...");
    String result;

    String salt = "$5$saltstring";
    String msg = "Hello world!";
    String res = "$5$saltstring$5B8vYYiY.CVt1RlTTf8KbXBH3hsxY/GNooZaBBGWEc5";

    result = Sha256_crypt(msg, salt);

    System.out.println("result is:"+result);
    System.out.println("should be:"+res);

    if (result.equals(res))
      {
	System.out.println("Passed well");
      }
    else
      {
	System.out.println("Failed Badly");
      }

    for (int t=0; t<7; t++)
      {
	result = Sha256_crypt(msgs[t*3+1], msgs[t*3]);

	System.out.println("test " + t + " result is:" + result);
	System.out.println("test " + t + " should be:" + msgs[t*3+2]);

	if (result.equals(msgs[t*3+2]))
	  {
	    System.out.println("Passed well");
	  }
	else
	  {
	    System.out.println("Failed Badly");
	  }
      }
  }

  /**
   * Test rig
   */

  public static void main(String arg[])
  {
    selfTest();
  }
}
