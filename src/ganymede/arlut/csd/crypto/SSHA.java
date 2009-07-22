/*

   SSHA.java

   This class implements support for Netscape's SSHA algorithm.  It is
   capable of taking a plaintext password, hashing it with SSHA and
   generating an RFC 2307-friendly LDAP password string of the form
   {SSHA}Base64(<hash><salt>).  Contrariwise, it is also capable of
   taking an SHA or SSHA encoded password and verifying a plaintext
   password against it.

   See http://www.openldap.org/faq/data/cache/347.html for more
   details.

   Created: 28 April 2004

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
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

import java.io.IOException;
import java.security.MessageDigest;

/*------------------------------------------------------------------------------
                                                                           class
                                                                            SSHA

------------------------------------------------------------------------------*/

/**
 * <p>This class implements support for Netscape's SSHA algorithm.  It
 * is capable of taking a plaintext password, hashing it with SSHA and
 * generating an RFC 2307-friendly LDAP password string of the form
 * {SSHA}Base64(&lt;hash&gt;&lt;salt&gt;).  Contrariwise, it is also
 * capable of taking an SHA or SSHA encoded password and verifying a
 * plaintext password against it.</p>
 *
 * <p>See http://www.openldap.org/faq/data/cache/347.html for more
 * details.</p>
 */

public final class SSHA {

  private final static boolean debug = false;

  // ---

  static private MessageDigest getSHA1()
  {
    try
      {
	return MessageDigest.getInstance("SHA-1");
      }
    catch (java.security.NoSuchAlgorithmException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * <p>This method takes a plaintext string and generates an SSHA
   * hash out of it.  If &lt;salt&gt; is not null, it will be
   * used, otherwise a random salt will be generated.</p>
   *
   * <p>The string returned will be a Base64 encoding of the
   * concatenation of the SHA-1 hash of (salt concatenated to plaintext)
   * and the salt.</p>
   */

  static public String getSSHAHash(String plaintext, String salt)
  {
    byte saltBytes[];
    MessageDigest hasher = getSHA1();

    /* -- */

    if (salt == null)
      {
	saltBytes = genRandomSalt();
      }
    else
      {
	saltBytes = salt.getBytes();
      }

    hasher.reset();
    hasher.update(plaintext.getBytes());
    hasher.update(saltBytes);
    byte digestBytes[] = hasher.digest();
    byte outBytes[] = new byte[saltBytes.length + 20]; // SHA1 hash is 20 bytes long

    assert(digestBytes.length == 20);

    System.arraycopy(digestBytes, 0, outBytes, 0, digestBytes.length);
    System.arraycopy(saltBytes, 0, outBytes, digestBytes.length, saltBytes.length);

    return Base64.encodeBytes(outBytes);
  }

  /**
   * <p>This method takes a plaintext string and generates an SSHA
   * hash out of it.  If &lt;salt&gt; is not null, it will be
   * used, otherwise a random salt will be generated.</p>
   *
   * <p>The string returned will be {SSHA} followed by the Base64
   * encoding of the concatenation of the SHA-1 hash of (salt
   * concatenated to plaintext) and the salt.</p>
   */

  static public String getLDAPSSHAHash(String plaintext, String salt)
  {
    return "{SSHA}" + getSSHAHash(plaintext, salt);
  }

  /**
   * <p>This method takes a Base64 encoded SHA/SSHA hashText (either
   * with or without the preceding {SHA}|{SSHA}) and attempts to
   * verify that the given plaintext matches it.  Returns true on
   * successful match.</p>
   */

  static public boolean matchSHAHash(String hashText, String plaintext)
  {
    byte[] hashBytes;
    byte[] plainBytes;
    byte[] saltBytes = null;
    MessageDigest hasher = getSHA1();

    /* -- */

    if (hashText.indexOf("{SSHA}") != -1)
      {
	hashText = hashText.substring(6);
      }
    else if (hashText.indexOf("{SHA}") != -1)
      {
	hashText = hashText.substring(5);
      }

    if (debug)
      {
	System.err.println("Wha!  " + hashText);
      }

    try
      {
	hashBytes = Base64.decode(hashText);
      }
    catch (IOException ex)
      {
	return false;
      }

    if (hashBytes.length > 20)
      {
	saltBytes = new byte[hashBytes.length - 20];

	for (int i = 20; i < hashBytes.length; i++)
	  {
	    saltBytes[i-20] = hashBytes[i];
	  }

	if (debug)
	  {
	    System.err.println("Salt is " + new String(saltBytes));
	  }
      }
    
    if (saltBytes != null)
      {
	byte[] inBytes = plaintext.getBytes();
	plainBytes = new byte[inBytes.length + saltBytes.length];

	for (int i = 0; i < inBytes.length; i++)
	  {
	    plainBytes[i] = inBytes[i];
	  }

	for (int i = 0; i < saltBytes.length; i++)
	  {
	    plainBytes[i+inBytes.length] = saltBytes[i];
	  }
      }
    else
      {
	plainBytes = plaintext.getBytes();
      }

    if (debug)
      {
	System.err.println("Match text is " + new String(plainBytes));
      }

    // okay, now we should have in plainBytes the input to the SHA
    // algorithm that would have been used to generate the hashText if
    // we indeed have a match.  Let's just check.

    hasher.reset();
    hasher.update(plainBytes);
    byte matchBytes[] = hasher.digest();

    assert(matchBytes.length == 20);

    for (int i = 0; i < matchBytes.length; i++)
      {
	if (matchBytes[i] != hashBytes[i])
	  {
	    if (debug)
	      {
		System.err.println("Char mismatch [" + i + "]");
	      }

	    return false;
	  }
      }

    return true;
  }

  /**
   * <p>This method generates a short, random sequence of salt bytes
   * for use in generating a Netscape SSHA hash.</p>
   */

  static public byte[] genRandomSalt()
  {
    /* note that we don't need to worry about using a fancy-schmancy
       Java 1.2 SecureRandom here.. the hash is directly included in
       the SSHA hashtext, so it's not like we can hide it.. the only
       purpose of the salt is to discourage extensive pre-generation
       of hash dictionaries.. as long the odds of any two hashtexts
       sharing a salt are adequately low, we don't really care about
       whether the salt we use at any given time was completely
       unpredictable. */

    java.util.Random randgen = new java.util.Random();
    byte[] saltBytes = new byte[4];

    /* -- */

    randgen.nextBytes(saltBytes);

    return saltBytes;
  }

  /**
   * Unit test rig
   */

  public static void main(String args[])
  {
    String hashText = SSHA.getLDAPSSHAHash("secret", null);
    
    if (SSHA.matchSHAHash(hashText, "secret"))
      {
	System.out.println("Good match on " + hashText);
      }
    else
      {
	System.out.println("Bad match on " + hashText);
      }

    /*
      example unsalted hashText for 'abc' from 

      http://developer.netscape.com/docs/technote/ldap/pass_sha.html
    */

    hashText = "{SHA}qZk+NkcGgWq6PiVxeFDCbJzQ2J0=";

    if (SSHA.matchSHAHash(hashText, "abc"))
      {
	System.out.println("Good match on " + hashText);
      }
    else
      {
	System.out.println("Bad match on " + hashText);
      }

    /*
      example unsalted hashText for
      'abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq' from 

      http://developer.netscape.com/docs/technote/ldap/pass_sha.html
    */

    hashText = "{SHA}hJg+RBw70m66rkqh+VEp5eVGcPE=";

    if (SSHA.matchSHAHash(hashText,
			  "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"))
      {
	System.out.println("Good match on " + hashText);
      }
    else
      {
	System.out.println("Bad match on " + hashText);
      }
  }
}
