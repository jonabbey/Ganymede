/*

   MD5Crypt.java

   Created: 3 November 1999
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/11/04 02:17:06 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

/*------------------------------------------------------------------------------
                                                                           class
                                                                        MD5Crypt

------------------------------------------------------------------------------*/

/**
 * <P>An Invid is an immutable object id (an INVariant ID) in the
 * Ganymede system.  All objects created in the database have a unique
 * and permanent Invid that identify the object's type and identity.  Because
 * of these properties, the Invid can be used as a persistent object pointer
 * type.</P>
 *
 * <P>Invid's are used extensively in the server to track pointer
 * relationships between objects.  Invid's are also used by the client to identify
 * objects to be viewed, edited, deleted, etc.  Basically whenever any code
 * in Ganymede deals with a reference to an object, it is done through the use
 * of Invid's.</P>
 *
 * @see arlut.csd.ganymede.InvidDBField
 * @see arlut.csd.ganymede.Session
 */

import MD5;

public final class MD5Crypt {

  static public void main(String argv[])
  {
    if (argv.length != 2)
      {
	System.err.println("Usage: MD5Crypt password salt");
	System.exit(1);
      }

    String password = argv[0];
    String salt = argv[1];

    System.err.println(MD5Crypt.crypt(password, salt));
    
    System.exit(0);
  }

  static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  static final String to64(long v, int size)
  {
    StringBuffer result = new StringBuffer();

    for (int i = 0; i < size; i++)
      {
	result.append(itoa64.charAt((int) (v & 0x3f)));
	v>>=6;
      }

    return result.toString();
  }

  static final String crypt(String password, String salt)
  {
    /* This string is magic for this algorithm.  Having it this way,
     * we can get get better later on */

    String magic = "$1$";
    byte finalState[];
    MD5 ctx, ctx1;
    long l;

    /* -- */

    /* Refine the Salt first */

    /* If it starts with the magic string, then skip that */
    
    if (salt.startsWith(magic))
      {
	salt = salt.substring(magic.length());
      }

    /* It stops at the first '$', max 8 chars */

    if (salt.indexOf('$') != -1)
      {
	salt = salt.substring(0, salt.indexOf('$'));
      }

    if (salt.length() > 8)
      {
	salt = salt.substring(0, 8);
      }

    ctx = new MD5();

    ctx.Update(password);    // The password first, since that is what is most unknown
    ctx.Update(magic);    // Then our magic string
    ctx.Update(salt);    // Then the raw salt

    /* Then just as many characters of the MD5(pw,salt,pw) */

    ctx1 = new MD5();

    ctx1.Update(password);
    ctx1.Update(salt);
    ctx1.Update(password);
    finalState = ctx1.Final();

    for (int pl = password.length(); pl > 0; pl -= 16)
      {
	ctx.Update(finalState, pl > 16? 16 : pl);
      }

    /* Then something really weird... */

    for (int j = 0, i = password.length(); i != 0; i >>=1)
      {
	if ((i & 1) != 0)
	  {
	    ctx.Update(finalState, j, 1);
	  }
	else
	  {
	    ctx.Update(password.getBytes(), j, 1);
	  }
      }

    finalState = ctx.Final();

    /*
     * and now, just to make sure things don't run too fast
     * On a 60 Mhz Pentium this takes 34 msec, so you would
     * need 30 seconds to build a 1000 entry dictionary...
     *
     * (The above timings from the C version)
     */

    for (int i = 0; i < 1000; i++)
      {
	ctx1 = new MD5();

	if ((i & 1) != 0)
	  {
	    ctx1.Update(password);
	  }
	else
	  {
	    ctx1.Update(finalState, 16);
	  }

	if ((i % 3) != 0)
	  {
	    ctx1.Update(salt);
	  }

	if ((i % 7) != 0)
	  {
	    ctx1.Update(password);
	  }

	if ((i & 1) != 0)
	  {
	    ctx1.Update(finalState, 16);
	  }
	else
	  {
	    ctx1.Update(password);
	  }

	finalState = ctx1.Final();
      }

    /* Now make the output string */

    StringBuffer result = new StringBuffer();

    result.append(magic);
    result.append(salt);
    result.append("$");

    l = (finalState[0] << 16) | (finalState[6] << 8) | finalState[12];
    result.append(to64(l, 4));

    l = (finalState[1] << 16) | (finalState[7] << 8) | finalState[13];
    result.append(to64(l, 4));

    l = (finalState[2] << 16) | (finalState[8] << 8) | finalState[14];
    result.append(to64(l, 4));

    l = (finalState[3] << 16) | (finalState[9] << 8) | finalState[15];
    result.append(to64(l, 4));

    l = (finalState[4] << 16) | (finalState[10] << 8) | finalState[5];
    result.append(to64(l, 4));

    l = finalState[11];
    result.append(to64(l, 2));

    return result.toString();
  }
}
