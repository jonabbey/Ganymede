/*

   Invid.java

   Non-remote object;  used as local on client and server,
   passed as value object.

   Invid's are intended to be immutable once created.

   Data type for invid objects;

   Created: 11 April 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Invid

------------------------------------------------------------------------------*/

/**
 * <p>An Invid is an immutable object id (an INVariant ID) in the
 * Ganymede system.  All objects created in the database have a unique
 * and permanent Invid that identify the object's type and identity.
 * Because of these properties, the Invid can be used as a persistent
 * object pointer type.</p>
 *
 * <p>Invid's are used extensively in the server to track pointer
 * relationships between objects.  Invid's are also used by the client
 * to identify objects to be viewed, edited, deleted, etc.  Basically
 * whenever any code in Ganymede deals with a reference to an object,
 * it is done through the use of Invid's.</p>
 *
 * @see arlut.csd.ganymede.server.InvidDBField
 * @see arlut.csd.ganymede.rmi.Session
 */

public final class Invid implements java.io.Serializable {

  static final long serialVersionUID = 5357151693275369893L;
  static private InvidAllocator allocator = null;
  static private int counter = 0;
  static private int reuseCounter = 0;

  static final public void printCount()
  {
    System.err.println("I've seen " + counter + " invids created, and seen " + reuseCounter + " reuses of interned invids.");
  }

  static final public int getCount()
  {
    return counter;
  }

  /**
   * <p>This method can be used to prep the Invid class with an {@link
   * arlut.csd.ganymede.common.InvidAllocator} that will return a
   * possibly pre-existing Invid object, given a short/int
   * combination.</p>
   *
   * <p>The purpose of this allocator is to allow the Ganymede server to
   * re-use previously created Invids in the server to minimize memory
   * usage, in a fashion similar to the Java language's
   * java.lang.String.intern() scheme.</p>
   */

  static final public void setAllocator(InvidAllocator newAllocator)
  {
    Invid.allocator = newAllocator;
  }

  /**
   * Receive Factory method for Invid's.  Can do caching/object reuse if
   * an {@link arlut.csd.ganymede.common.InvidAllocator} has been set.
   */

  static final public Invid createInvid(short type, int num)
  {
    if (allocator == null)
      {
        counter++;
        return new Invid(type, num);
      }
    else
      {
        Invid newInvid = new Invid(type, num);
        Invid internedInvid = newInvid.intern();

        if (newInvid == internedInvid)
          {
            counter++;          // we're the initial creation of this invid
          }
        else
          {
            reuseCounter++;
          }

        return internedInvid;
      }
  }

  /**
   * Receive Factory method for Invid's.  Can do caching/object reuse
   * if an {@link arlut.csd.ganymede.common.InvidAllocator} has been
   * set.
   */

  static final public Invid createInvid(DataInput in) throws IOException
  {
    return createInvid(in.readShort(), in.readInt());
  }

  /**
   * Factory method for Invid's.  String should be a pair of colon
   * separated numbers, in the form 5:134 where the first number is
   * the short type and the second is the int object number. Can do
   * efficient memory re-use if an {@link
   * arlut.csd.ganymede.common.InvidAllocator} has been set.
   */

  static final public Invid createInvid(String string)
  {
    String first = string.substring(0, string.indexOf(':'));
    String last = string.substring(string.indexOf(':')+1);

    try
      {
        return createInvid(Short.valueOf(first).shortValue(),
                           Integer.valueOf(last).intValue());
      }
    catch (NumberFormatException ex)
      {
        throw new IllegalArgumentException("bad string format " + ex);
      }
  }

  /**
   * Returns true if the Invid class is configured to use a pooled
   * allocator.
   */

  static final public boolean hasAllocator()
  {
    return allocator != null;
  }

  // ---

  private final short type;
  private final int num;
  private transient boolean interned = false;

  // constructor

  /**
   * Private constructor.  Use the static createInvid methods to
   * create Invids, please.
   */

  private Invid(short type, int num)
  {
    this.type = type;
    this.num = num;
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
    // we'll mix type and num.. since we allocate invids in
    // incrementing order starting at 0, we'll shove the type numbers,
    // (which also start at 0 and go up) to the left of our 32 bit
    // hash so that when we hash invids of differing type together
    // they won't all collide at the low end of the range

    return (type << 23) ^ num;
  }

  /**
   * <p>As with java.lang.String, Invids are immutable objects that we
   * may be able to usefully pool for object re-use.  The result of an
   * intern method is a single immutable Invid that will be reused by
   * all other Invid's that point to the same object in the Ganymede
   * server (if you're calling intern() on the server, that is.)</p>
   *
   * <p>If an Invid Allocator has not been set with setAllocator(),
   * intern() will do nothing, and will return this.</p>
   *
   * <p>Note that there only two ways to create an Invid.. to use the
   * public static createInvid factory methods, in which case the
   * Invids you get will already be interned, or through
   * deserialization, in which case the JVM creates the Invid from the
   * serialized representation without going through the Invid class
   * constructors or factory methods.</p>
   *
   * <p>The intern method is to provide for the latter case, so that
   * Ganymede code which receives Invids from serialization streams
   * can look up a pooled reference pointing to the content of the
   * Invid and use that for in-heap storage, rather than a possibly
   * redundant copy.</p>
   */

  public Invid intern()
  {
    if (interned || allocator == null)
      {
        return this;
      }
    else
      {
        Invid result = allocator.findInvid(this);

        if (result == null)
          {
            counter++;
            allocator.storeInvid(this);
            this.interned = true;
            return this;
          }
        else
          {
            reuseCounter++;
            return result;
          }
      }
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

  public boolean isInterned()
  {
    return this.interned;
  }

  /**
   * <p>Method to write this Invid out to a stream.</p>
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
