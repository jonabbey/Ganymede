/*

   Invid.java

   Non-remote object;  used as local on client and server,
   passed as value object.

   Invid's are intended to be immutable once created.

   Data type for invid objects;
   
   Created: 11 April 1996
   Release: $Name:  $
   Version: $Revision: 1.19 $
   Last Mod Date: $Date: 1999/01/22 18:05:47 $
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

package arlut.csd.ganymede;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Invid

------------------------------------------------------------------------------*/

/**
 *
 * An Invid is an immutable object id (an INVariant ID) in the
 * Ganymede system.  All objects created in the database have a unique
 * and permanent Invid that identify the object's type and identity.  Because
 * of these properties, the Invid can be used as a persistent object pointer
 * type.<br><br>
 *
 * Invid's are used extensively in the server to track pointer
 * relationships between objects.  Invid's are also used by the client to identify
 * objects to be viewed, edited, deleted, etc.  Basically whenever any code
 * in Ganymede deals with a reference to an object, it is done through the use
 * of Invid's.
 *
 * @see arlut.csd.ganymede.InvidDBField
 * @see arlut.csd.ganymede.Session
 *  
 */

public final class Invid implements java.io.Serializable {

  static final long serialVersionUID = 5357151693275369893L;

  // ---

  private short type;
  private int num;

  // constructors

  public Invid(short type, int num) 
  {
    this.type = type;
    this.num = num;
  }

  /**
   *
   * Receive constructor
   *
   */

  public Invid(DataInput in) throws IOException
  {
    type = in.readShort();
    num = in.readInt();
  }

  /**
   *
   * This is the string constructor.. string should be
   * a pair of colon separated numbers, in the form
   *
   * 5:134 where the first number is the short type
   * and the second is the int object number
   *
   */

  public Invid(String string)
  {
    String first = string.substring(0, string.indexOf(':'));
    String last = string.substring(string.indexOf(':')+1);

    try
      {
	this.type = Short.valueOf(first).shortValue();
	this.num = Integer.valueOf(last).intValue();
      }
    catch (NumberFormatException ex)
      {
	throw new IllegalArgumentException("bad string format " + ex);
      }
  }

  // equals

  public boolean equals(Object obj)
  {
    if (obj == null)
      {
	return false;
      }

    if (obj instanceof Invid)
      {
	return equals((Invid) obj);
      }
    else
      {
	return false;
      }
  }

  public boolean equals(Invid invid)
  {
    // in case some one casts the param

    if (invid == null)
      {
	return false;
      }

    if ((invid.type == type) &&
	(invid.num == num))
      {
	return true;
      }

    return false;
  }

  // hashcode

  public int hashCode()
  {

    return num;			// simplistic, different types of invid's with
				// same number will hash to same bucket, but
				// this is probably ok for our uses, where we
				// will generally not have multiple types of
				// invid's in a particular hash.
  }

  // pull the values

  public short getType() 
  {
    return type;
  }

  public int getNum() 
  {
    return num;
  }

  /**
   *
   * Method to write this Invid out to a stream. 
   *
   */

  public void emit(DataOutput out) throws IOException
  {
    out.writeShort(type);
    out.writeInt(num);
  }

  public String toString()
  {
    return type + ":" + num;
  }
}
